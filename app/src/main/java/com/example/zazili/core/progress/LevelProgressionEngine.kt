package com.example.zazili.core.progress

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Progreso de mérito reactivo.
 *
 * La escalera crece con el nivel: 100 XP para el 1 → 2, 200 para el 2 → 3,
 * 300 para el 3 → 4, etc. [xpEnNivel] siempre queda por debajo de
 * [xpNecesaria]; el resto de la experiencia ya se convirtió en niveles.
 */
@Immutable
data class ProgresoNivel(
    val nivel: Int = 1,
    val xpEnNivel: Int = 0,
    val xpNecesaria: Int = LevelProgressionEngine.umbralPara(1),
    val xpTotal: Int = 0
) {
    val avance: Float
        get() = if (xpNecesaria <= 0) 0f
        else (xpEnNivel.toFloat() / xpNecesaria).coerceIn(0f, 1f)

    val xpRestante: Int get() = (xpNecesaria - xpEnNivel).coerceAtLeast(0)

    /** Alias legibles para quien ya piensa en inglés. */
    val currentLevel: Int get() = nivel
    val currentXP: Int get() = xpEnNivel
    val xpToNextLevel: Int get() = xpNecesaria
}

/** Aviso de experiencia para que la UI celebre sin volver a calcular nada. */
@Immutable
data class SubidaXp(
    val ganada: Int,
    val nivelAnterior: Int,
    val nivelNuevo: Int,
    val motivo: String = ""
) {
    val subioDeNivel: Boolean get() = nivelNuevo > nivelAnterior
}

/**
 * Administrador único de XP y niveles del proceso.
 *
 * Por qué existe además de [GameProgressManager]: aquel guarda en
 * `SharedPreferences` y expone un `StateFlow`, pero cualquier lector que
 * aparezca después del arranque puede quedarse con una foto vieja. Este motor
 * registra un listener sobre el archivo legado (`total_xp`) y publica cada
 * cambio en SU PROPIO flujo, así el Tablero siempre se redibuja aunque la
 * experiencia la haya otorgado otra pantalla.
 *
 * El Tablero solo necesita:
 * ```
 * val progreso by LevelProgressionEngine.obtener(context).progreso
 *     .collectAsStateWithLifecycle()
 * ```
 */
class LevelProgressionEngine private constructor(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(ARCHIVO_NIVEL, Context.MODE_PRIVATE)
    }.onFailure {
        Log.w(TAG, "Progreso de nivel no disponible; se usará uno nuevo", it)
    }.getOrNull()

    private val prefsLegado: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(ARCHIVO_LEGADO, Context.MODE_PRIVATE)
    }.getOrNull()

    private var ultimoLegadoXp = 0

    private val _progreso = MutableStateFlow(cargarInicial())
    val progreso: StateFlow<ProgresoNivel> = _progreso.asStateFlow()

    private val _eventos = MutableSharedFlow<SubidaXp>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventos: SharedFlow<SubidaXp> = _eventos.asSharedFlow()

    private val escucha: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, clave ->
            if (clave == CLAVE_LEGADO_XP) importarLegadoPendiente()
        }

    init {
        if (prefs?.contains(CLAVE_NIVEL) != true) guardar(_progreso.value)
        ultimoLegadoXp = prefsLegado?.getInt(CLAVE_LEGADO_XP, 0) ?: 0
        runCatching { prefsLegado?.registerOnSharedPreferenceChangeListener(escucha) }
    }

    /**
     * Suma experiencia y liquida todas las subidas de nivel que quepan.
     * Devuelve el delta exacto para que la pantalla de recompensa lo muestre.
     *
     * La operación es atómica (synchronized) y persiste con `apply()`, así que
     * puede llamarse desde el hilo que sea sin congelar la UI.
     */
    @Synchronized
    fun otorgarXp(cantidad: Int, motivo: String = "Sesión completada"): SubidaXp {
        if (cantidad <= 0) {
            val actual = _progreso.value
            return SubidaXp(0, actual.nivel, actual.nivel, motivo)
        }
        val antes = _progreso.value
        var nivel = antes.nivel
        var xp = antes.xpEnNivel + cantidad
        val total = antes.xpTotal + cantidad

        while (nivel < NIVEL_MAXIMO && xp >= umbralPara(nivel)) {
            xp -= umbralPara(nivel)
            nivel += 1
        }
        if (nivel >= NIVEL_MAXIMO) {
            xp = xp.coerceIn(0, umbralPara(NIVEL_MAXIMO))
        }

        val nuevo = ProgresoNivel(
            nivel = nivel,
            xpEnNivel = xp,
            xpNecesaria = umbralPara(nivel),
            xpTotal = total
        )
        _progreso.value = nuevo
        guardar(nuevo)
        val evento = SubidaXp(
            ganada = cantidad,
            nivelAnterior = antes.nivel,
            nivelNuevo = nivel,
            motivo = motivo
        )
        _eventos.tryEmit(evento)
        return evento
    }

    /** Variante sin espera para pantallas que no necesitan el delta. */
    fun reiniciar() {
        val limpio = ProgresoNivel(
            nivel = 1,
            xpEnNivel = 0,
            xpNecesaria = umbralPara(1),
            xpTotal = 0
        )
        _progreso.value = limpio
        guardar(limpio)
    }

    /**
     * La experiencia que otorgue el motor legado también mueve esta escalera:
     * se compara contra la última lectura conocida para pagar solo el delta.
     */
    @Synchronized
    private fun importarLegadoPendiente() {
        val prefs = prefsLegado ?: return
        val total = prefs.getInt(CLAVE_LEGADO_XP, 0)
        val delta = total - ultimoLegadoXp
        ultimoLegadoXp = total
        if (delta > 0) otorgarXp(delta, motivo = "Progreso de lección")
    }

    private fun cargarInicial(): ProgresoNivel {
        val guardadoNivel = prefs?.getInt(CLAVE_NIVEL, 0) ?: 0
        if (guardadoNivel in 1..NIVEL_MAXIMO) {
            val nivel = guardadoNivel
            return ProgresoNivel(
                nivel = nivel,
                xpEnNivel = (prefs?.getInt(CLAVE_XP_NIVEL, 0) ?: 0).coerceAtLeast(0),
                xpNecesaria = umbralPara(nivel),
                xpTotal = (prefs?.getInt(CLAVE_XP_TOTAL, 0) ?: 0).coerceAtLeast(0)
            )
        }

        // Primera vez con este motor: se migra el total legado sin perder XP.
        val legado = (prefsLegado?.getInt(CLAVE_LEGADO_XP, 0) ?: 0).coerceAtLeast(0)
        ultimoLegadoXp = legado
        return desdeTotal(legado)
    }

    private fun desdeTotal(totalXp: Int): ProgresoNivel {
        var nivel = 1
        var restante = totalXp.coerceAtLeast(0)
        while (nivel < NIVEL_MAXIMO && restante >= umbralPara(nivel)) {
            restante -= umbralPara(nivel)
            nivel += 1
        }
        return ProgresoNivel(
            nivel = nivel,
            xpEnNivel = restante,
            xpNecesaria = umbralPara(nivel),
            xpTotal = totalXp.coerceAtLeast(0)
        )
    }

    private fun guardar(progreso: ProgresoNivel) {
        runCatching {
            prefs?.edit()
                ?.putInt(CLAVE_NIVEL, progreso.nivel)
                ?.putInt(CLAVE_XP_NIVEL, progreso.xpEnNivel)
                ?.putInt(CLAVE_XP_TOTAL, progreso.xpTotal)
                ?.apply()
        }.onFailure {
            Log.w(TAG, "No se pudo guardar el nivel; se conserva en memoria", it)
        }
    }

    companion object {
        private const val TAG = "ProgresoNivel"
        private const val ARCHIVO_NIVEL = "zazili_progreso_nivel"
        private const val ARCHIVO_LEGADO = "zazili_progress"
        private const val CLAVE_NIVEL = "nivel_actual"
        private const val CLAVE_XP_NIVEL = "xp_en_nivel"
        private const val CLAVE_XP_TOTAL = "xp_total"
        private const val CLAVE_LEGADO_XP = "total_xp"

        /** Curva `nivel * 100`: 100, 200, 300… */
        const val XP_POR_NIVEL = 100
        const val NIVEL_MAXIMO = 99

        fun umbralPara(nivel: Int): Int =
            (nivel.coerceAtLeast(1) * XP_POR_NIVEL).coerceAtMost(9_900)

        @Volatile
        private var instancia: LevelProgressionEngine? = null

        fun obtener(context: Context): LevelProgressionEngine =
            instancia ?: synchronized(this) {
                instancia ?: LevelProgressionEngine(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}
