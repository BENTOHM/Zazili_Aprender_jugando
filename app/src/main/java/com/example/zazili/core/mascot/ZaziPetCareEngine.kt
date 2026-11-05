package com.example.zazili.core.mascot

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.zazili.core.ui.ZaziState
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val Context.dataStoreCuidadoZazi: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_petcare"
)

/**
 * Cambio neto que un alimento, un baño o un juguete produce en las cuatro
 * vitales. Todos los valores son puntos porcentuales y pueden ser negativos.
 */
@Immutable
data class EfectosCuidado(
    val pancita: Int = 0,
    val felicidad: Int = 0,
    val energia: Int = 0,
    val higiene: Int = 0
) {
    /** Resumen para el texto flotante: "+20 Pancita · +6 Felicidad". */
    fun resumen(): String {
        val partes = buildList {
            if (pancita != 0) add("${pancita.conSigno()} Pancita")
            if (felicidad != 0) add("${felicidad.conSigno()} Felicidad")
            if (energia != 0) add("${energia.conSigno()} Energía")
            if (higiene != 0) add("${higiene.conSigno()} Higiene")
        }
        return if (partes.isEmpty()) "¡Qué rico!" else partes.joinToString(" · ")
    }
}

private fun Int.conSigno(): String = if (this > 0) "+$this" else "$this"

/**
 * Motivos por los que Zazi no puede abrir una lección o un minijuego.
 * Las tres compuertas son estrictas y se evalúan en este orden: sueño,
 * salud y energía. El mensaje está pensado para leerse en voz alta.
 */
enum class BloqueoJuego(val mensaje: String) {
    SUENO("¡Shhh! Zazi está durmiendo. Despiértalo para jugar."),
    SALUD("Zazi no se siente bien. ¡Aliméntalo o báñalo antes de jugar!"),
    ENERGIA("Zazi está agotado. Déjalo descansar un ratito.")
}

/** Inventario de bienvenida: alcanza para el primer día sin gastar Zazoles. */
private val CONSUMIBLES_BIENVENIDA: Map<String, Int> = mapOf(
    "agua_limon_chia" to 2,
    "bano_burbujas_lavanda" to 1,
    "mini_pinata" to 1
)

// Ritmo del Tamagotchi. Vive fuera de la clase para que las funciones puras
// de decaimiento y las propiedades de ZaziVitals puedan leerlo.
private const val UMBRAL_SUCIEDAD = 35f
private const val UMBRAL_TICK_HORAS = 1f / 60f
private const val MILISEGUNDOS_POR_HORA = 3_600_000f

// Rebalanceo 1.5x para Pancita e Higiene: piden 2-3 visitas al día sin
// castigar. Felicidad conserva su ritmo y la energía ya no decae por tiempo.
private const val DECAIMIENTO_PANCITA = 6.0f
private const val DECAIMIENTO_FELICIDAD = 2.5f
private const val DECAIMIENTO_HIGIENE = 2.7f

// Sistema 30/30: jugar 30 minutos continuos vacía la pila; descansar 30
// minutos reales (sin jugar) la llena. Dormir la llena al instante.
private const val ENERGIA_POR_MINUTO_JUEGO = 100.0f / 30.0f
private const val RECUPERACION_ENERGIA_HORA = 200.0f
private const val MAX_MINUTOS_SESION_JUEGO = 45.0f

private const val FACTOR_SUENO_PANCITA = 0.55f
private const val FACTOR_SUENO_FELICIDAD = 0.60f
private const val FACTOR_SUENO_HIGIENE = 0.45f

private const val PISO_PANCITA = 0f
private const val PISO_FELICIDAD = 0f
private const val PISO_HIGIENE = 0f

private const val ENERGIA_SUENO = 10f

/**
 * Las cuatro vitales de Zazi, siempre en el rango 0..100.
 *
 * No existe la muerte ni el castigo: los pisos dejan a Zazi con hambre o
 * sueñito, nunca en un estado alarmante. [suciedad] es un valor derivado que
 * la vista usa para dibujar manchitas y líneas de olor.
 */
@Immutable
data class ZaziVitals(
    val pancita: Float = 78f,
    val felicidad: Float = 84f,
    val energia: Float = 88f,
    val higiene: Float = 92f,
    val dormida: Boolean = false,
    val ultimaActualizacion: Long = System.currentTimeMillis()
) {
    val pancitaPct: Int get() = pancita.aPorcentaje()
    val felicidadPct: Int get() = felicidad.aPorcentaje()
    val energiaPct: Int get() = energia.aPorcentaje()
    val higienePct: Int get() = higiene.aPorcentaje()

    /** 0 = limpia, 1 = necesita un baño urgente. */
    val suciedad: Float
        get() = ((UMBRAL_SUCIEDAD - higiene) / UMBRAL_SUCIEDAD).coerceIn(0f, 1f)

    val pancitaBaja: Boolean get() = pancita < 30f
    val necesitaDormir: Boolean get() = energia <= ENERGIA_SUENO

    /**
     * Compuerta única de Rule 2. Devuelve null cuando Zazi puede jugar.
     * Un cero visible en cualquiera de las tres vitales de salud bloquea.
     */
    fun bloqueoParaJugar(): BloqueoJuego? = when {
        dormida -> BloqueoJuego.SUENO
        pancita <= 0.5f || felicidad <= 0.5f || higiene <= 0.5f -> BloqueoJuego.SALUD
        energia <= 0.5f -> BloqueoJuego.ENERGIA
        else -> null
    }

    /** Pose visual coherente con el motor de sprites y con el estado de sueño. */
    fun estadoVisual(celebrando: Boolean = false, pensando: Boolean = false): ZaziState =
        when {
            dormida || energia <= 8f -> ZaziState.DURMIENDO
            celebrando -> ZaziState.CELEBRACION
            pensando -> ZaziState.PENSANDO
            pancita < 30f || felicidad < 25f -> ZaziState.TRISTE
            else -> ZaziState.VUELO_IDLE
        }

    fun fraseEstado(): String = when {
        dormida -> "Zazi sueña con mariposas y flores."
        pancita < 30f && higiene < UMBRAL_SUCIEDAD ->
            "A Zazi le ruge la pancita y quiere un baño."
        pancita < 30f -> "A Zazi le ruge la pancita."
        higiene < UMBRAL_SUCIEDAD -> "Zazi necesita un baño de burbujas."
        energia < 25f -> "Zazi tiene sueñito. ¿La arropas?"
        felicidad < 35f -> "Zazi extraña tus cariñitos."
        felicidad > 90f -> "¡Zazi está feliz como colibrí en primavera!"
        else -> "Zazi está contenta de estar contigo."
    }
}

/** Suma los efectos y respeta los topes de cada vital. */
fun ZaziVitals.conEfectos(efectos: EfectosCuidado): ZaziVitals = copy(
    pancita = (pancita + efectos.pancita).coerceIn(0f, 100f),
    felicidad = (felicidad + efectos.felicidad).coerceIn(0f, 100f),
    energia = (energia + efectos.energia).coerceIn(0f, 100f),
    higiene = (higiene + efectos.higiene).coerceIn(0f, 100f)
)

/**
 * Aplica el paso del tiempo real a las cuatro vitales.
 *
 * - Despierta y sin jugar: la energía se recarga sola (0 → 100 en 30 minutos).
 * - Jugando ([enJuego] = true): la recarga pasiva se pausa; el consumo se
 *   liquida al cerrar la sesión con [consumirJuego].
 * - Dormida: la energía queda llena al instante y el resto de las vitales baja
 *   muy despacio. Zazi no se despierta sola ni se acuesta por sí misma: el
 *   sueño siempre lo decide el niño.
 * - Pancita, felicidad e higiene pueden llegar a 0; ese cero activa la
 *   compuerta de salud, nunca una muerte ni un castigo.
 */
fun decaerZazi(
    vitals: ZaziVitals,
    ahoraMillis: Long,
    enJuego: Boolean = false
): ZaziVitals {
    val transcurrido = (ahoraMillis - vitals.ultimaActualizacion).coerceAtLeast(0L)
    val horas = transcurrido / MILISEGUNDOS_POR_HORA
    if (horas < UMBRAL_TICK_HORAS) return vitals

    val proporcionDormida = if (vitals.dormida) 1f else 0f
    val factorPancita = (1f - proporcionDormida) + proporcionDormida * FACTOR_SUENO_PANCITA
    val factorFelicidad = (1f - proporcionDormida) + proporcionDormida * FACTOR_SUENO_FELICIDAD
    val factorHigiene = (1f - proporcionDormida) + proporcionDormida * FACTOR_SUENO_HIGIENE

    val energia = when {
        vitals.dormida -> 100f
        enJuego -> vitals.energia
        else -> (vitals.energia + horas * RECUPERACION_ENERGIA_HORA).coerceAtMost(100f)
    }

    return vitals.copy(
        pancita = (vitals.pancita - horas * DECAIMIENTO_PANCITA * factorPancita)
            .coerceAtLeast(PISO_PANCITA),
        felicidad = (vitals.felicidad - horas * DECAIMIENTO_FELICIDAD * factorFelicidad)
            .coerceAtLeast(PISO_FELICIDAD),
        energia = energia,
        higiene = (vitals.higiene - horas * DECAIMIENTO_HIGIENE * factorHigiene)
            .coerceAtLeast(PISO_HIGIENE),
        dormida = vitals.dormida,
        ultimaActualizacion = ahoraMillis
    )
}

/**
 * Consumo puro de una sesión de juego. La calibración es exacta: 30 minutos
 * continuos restan 100 puntos. El tope de 45 minutos evita castigar una sesión
 * que quedó abierta en segundo plano.
 */
fun ZaziVitals.consumirJuego(minutos: Float): ZaziVitals {
    if (minutos <= 0f) return this
    val costo = minutos.coerceAtMost(MAX_MINUTOS_SESION_JUEGO) * ENERGIA_POR_MINUTO_JUEGO
    return copy(energia = (energia - costo).coerceAtLeast(0f))
}

/**
 * Estado completo del sistema de cuidado: vitales + inventario + armario.
 * Es inmutable para que Compose nunca vea una lista a medio escribir.
 */
@Immutable
data class ZaziCareState(
    val vitals: ZaziVitals = ZaziVitals(),
    val consumibles: Map<String, Int> = CONSUMIBLES_BIENVENIDA,
    val ropaComprada: Set<String> = emptySet(),
    val fondosComprados: Set<String> = emptySet(),
    val ropaEquipada: Map<String, String> = emptyMap(),
    val fondoEquipado: String? = null,
    val listo: Boolean = false
)

/**
 * Motor del Tamagotchi de Zazili 2.0.
 *
 * Singleton de proceso respaldado por DataStore (`zazili_petcare`). Toda
 * escritura es atómica dentro de un mismo `edit`: el paso del tiempo, el
 * consumo del inventario y los efectos ocurren en una sola transacción, así
 * que dos pantallas no pueden duplicar comida ni perder vitales.
 *
 * La UI solo observa [estado] y llama a las acciones `suspend`. Ningún método
 * lanza excepciones: ante un fallo de disco, se registra y el estado previo
 * se conserva.
 */
class ZaziPetCareEngine private constructor(context: Context) {

    private val dataStore = context.applicationContext.dataStoreCuidadoZazi
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Mientras hay una lección o un minijuego abierto, el tick no recarga
     * energía: el descanso pasivo solo corre fuera del juego. Es memoria de
     * proceso a propósito; un cierre inesperado reinicia la sesión sin castigo.
     */
    @Volatile
    private var juegoActivo = false

    val estado: StateFlow<ZaziCareState> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs -> prefs.aEstadoCuidado(listo = true) }
        .stateIn(
            scope = alcance,
            started = SharingStarted.Eagerly,
            initialValue = ZaziCareState()
        )

    init {
        alcance.launch { sincronizar() }
        alcance.launch {
            while (isActive) {
                delay(INTERVALO_TICK_MS)
                sincronizar()
            }
        }
    }

    fun cantidadDe(itemId: String): Int = estado.value.consumibles[itemId] ?: 0

    fun ropaPuesta(ranura: String): String? = estado.value.ropaEquipada[ranura]

    fun estaPuestoFondo(fondoId: String): Boolean = estado.value.fondoEquipado == fondoId

    /** Compuerta de Rule 2 para cualquier pantalla que quiera abrir actividad. */
    fun bloqueoParaJugar(): BloqueoJuego? = estado.value.vitals.bloqueoParaJugar()

    /** Marca si hay una sesión de juego abierta; suspende la recarga pasiva. */
    fun marcarJuegoActivo(activo: Boolean) {
        juegoActivo = activo
    }

    /**
     * Liquida el tiempo real jugado. 30 minutos continuos equivalen a 100
     * puntos de energía; la sesión se acota a 45 minutos por seguridad.
     */
    suspend fun consumirTiempoDeJuego(minutos: Float) {
        if (minutos <= 0f) return
        mutarVitals { vitals -> vitals.consumirJuego(minutos) }
    }

    /** Aplica el tiempo transcurrido desde la última visita. */
    suspend fun sincronizar() {
        mutarConPrefs { _, vitals -> vitals }
    }

    /** Hacer cariño: sube la felicidad sin despertar a Zazi. */
    suspend fun acariciar() {
        mutarVitals { vitals ->
            vitals.copy(felicidad = (vitals.felicidad + CARICIA_FELICIDAD).coerceAtMost(100f))
        }
    }

    /** Arropa a Zazi: el sueño recarga la energía al instante. */
    suspend fun dormir() {
        mutarVitals { vitals -> vitals.copy(dormida = true, energia = 100f) }
    }

    /** Despierta a Zazi con un mínimo de energía para que no se recueste al instante. */
    suspend fun despertar() {
        mutarVitals { vitals ->
            vitals.copy(
                dormida = false,
                energia = vitals.energia.coerceAtLeast(ENERGIA_MINIMA_DESPERTAR)
            )
        }
    }

    /**
     * Consume una unidad del inventario y aplica sus efectos en una sola
     * transacción. Devuelve false si ya no hay existencias.
     */
    suspend fun usarConsumible(itemId: String, efectos: EfectosCuidado): Boolean {
        var consumido = false
        try {
            dataStore.edit { prefs ->
                val ahora = System.currentTimeMillis()
                prefs.prepararSiEsNueva(ahora)
                val inventario = prefs.consumiblesMapa().toMutableMap()
                val cantidad = inventario[itemId] ?: 0
                if (cantidad <= 0) return@edit
                if (cantidad == 1) {
                    inventario.remove(itemId)
                } else {
                    inventario[itemId] = cantidad - 1
                }
                prefs[CLAVE_CONSUMIBLES] = inventario.aClaves()
                val actuales = decaerZazi(prefs.aVitals(), ahora, enJuego = juegoActivo)
                prefs.escribirVitals(actuales.conEfectos(efectos))
                consumido = true
            }
        } catch (error: IOException) {
            Log.w(TAG, "No se pudo usar el artículo $itemId", error)
        }
        return consumido
    }

    suspend fun comprarConsumible(itemId: String) {
        mutarConPrefs { prefs, vitals ->
            val inventario = prefs.consumiblesMapa().toMutableMap()
            inventario[itemId] = (inventario[itemId] ?: 0) + 1
            prefs[CLAVE_CONSUMIBLES] = inventario.aClaves()
            vitals
        }
    }

    /** Registra una prenda comprada, la pone y premia con felicidad. */
    suspend fun registrarCompraRopa(ropaId: String, ranura: String) {
        mutarConPrefs { prefs, vitals ->
            prefs[CLAVE_ROPA_COMPRADA] = (prefs[CLAVE_ROPA_COMPRADA] ?: emptySet()) + ropaId
            val equipo = prefs.ropaEquipadaMapa().toMutableMap()
            equipo[ranura] = ropaId
            prefs[CLAVE_ROPA_EQUIPADA] = equipo.aClavesEquipo()
            vitals.copy(felicidad = (vitals.felicidad + FELICIDAD_ESTRENO).coerceAtMost(100f))
        }
    }

    suspend fun equiparRopa(ropaId: String, ranura: String) {
        mutarConPrefs { prefs, vitals ->
            val compradas = prefs[CLAVE_ROPA_COMPRADA] ?: emptySet()
            if (ropaId !in compradas) return@mutarConPrefs vitals
            val equipo = prefs.ropaEquipadaMapa().toMutableMap()
            if (equipo[ranura] == ropaId) return@mutarConPrefs vitals
            equipo[ranura] = ropaId
            prefs[CLAVE_ROPA_EQUIPADA] = equipo.aClavesEquipo()
            vitals.copy(felicidad = (vitals.felicidad + FELICIDAD_ESTRENO).coerceAtMost(100f))
        }
    }

    suspend fun quitarRopa(ranura: String) {
        mutarConPrefs { prefs, vitals ->
            val equipo = prefs.ropaEquipadaMapa().toMutableMap()
            if (equipo.remove(ranura) != null) {
                prefs[CLAVE_ROPA_EQUIPADA] = equipo.aClavesEquipo()
            }
            vitals
        }
    }

    /** Registra un fondo comprado, lo coloca y premia con felicidad. */
    suspend fun registrarCompraFondo(fondoId: String) {
        mutarConPrefs { prefs, vitals ->
            prefs[CLAVE_FONDOS_COMPRADOS] = (prefs[CLAVE_FONDOS_COMPRADOS] ?: emptySet()) + fondoId
            prefs[CLAVE_FONDO_EQUIPADO] = fondoId
            vitals.copy(felicidad = (vitals.felicidad + FELICIDAD_ESTRENO).coerceAtMost(100f))
        }
    }

    suspend fun equiparFondo(fondoId: String) {
        mutarConPrefs { prefs, vitals ->
            val comprados = prefs[CLAVE_FONDOS_COMPRADOS] ?: emptySet()
            if (fondoId !in comprados) return@mutarConPrefs vitals
            if (prefs[CLAVE_FONDO_EQUIPADO] == fondoId) return@mutarConPrefs vitals
            prefs[CLAVE_FONDO_EQUIPADO] = fondoId
            vitals.copy(felicidad = (vitals.felicidad + FELICIDAD_ESTRENO).coerceAtMost(100f))
        }
    }

    suspend fun quitarFondo() {
        mutarConPrefs { prefs, vitals ->
            prefs.remove(CLAVE_FONDO_EQUIPADO)
            vitals
        }
    }

    /**
     * Terminar una lección deja a Zazi orgullosa. La energía NO se cobra aquí:
     * el consumo se liquida por tiempo real de juego en [consumirTiempoDeJuego],
     * así una lección larga cuesta más que una corta y nunca se cobra doble.
     */
    suspend fun registrarEstudio() {
        mutarVitals { vitals ->
            vitals.copy(felicidad = (vitals.felicidad + FELICIDAD_ESTUDIO).coerceAtMost(100f))
        }
    }

    /** Jugar un minijuego sube la felicidad; la energía sale del tiempo jugado. */
    suspend fun registrarMinijuego() {
        mutarVitals { vitals ->
            vitals.copy(felicidad = (vitals.felicidad + FELICIDAD_MINIJUEGO).coerceAtMost(100f))
        }
    }

    private suspend fun mutarVitals(transform: (ZaziVitals) -> ZaziVitals) {
        mutarConPrefs { _, vitals -> transform(vitals) }
    }

    private suspend fun mutarConPrefs(
        transform: (MutablePreferences, ZaziVitals) -> ZaziVitals
    ) {
        try {
            dataStore.edit { prefs ->
                val ahora = System.currentTimeMillis()
                prefs.prepararSiEsNueva(ahora)
                val actuales = decaerZazi(prefs.aVitals(), ahora, enJuego = juegoActivo)
                prefs.escribirVitals(transform(prefs, actuales))
            }
        } catch (error: IOException) {
            Log.w(TAG, "No se pudo actualizar el cuidado de Zazi", error)
        }
    }

    private fun Preferences.aEstadoCuidado(listo: Boolean): ZaziCareState = ZaziCareState(
        vitals = aVitals(),
        consumibles = consumiblesMapa(),
        ropaComprada = this[CLAVE_ROPA_COMPRADA] ?: emptySet(),
        fondosComprados = this[CLAVE_FONDOS_COMPRADOS] ?: emptySet(),
        ropaEquipada = ropaEquipadaMapa(),
        fondoEquipado = this[CLAVE_FONDO_EQUIPADO],
        listo = listo
    )

    private fun Preferences.aVitals(): ZaziVitals = ZaziVitals(
        pancita = (this[CLAVE_PANCITA] ?: 78f).coerceIn(0f, 100f),
        felicidad = (this[CLAVE_FELICIDAD] ?: 84f).coerceIn(0f, 100f),
        energia = (this[CLAVE_ENERGIA] ?: 88f).coerceIn(0f, 100f),
        higiene = (this[CLAVE_HIGIENE] ?: 92f).coerceIn(0f, 100f),
        dormida = this[CLAVE_DORMIDA] ?: false,
        ultimaActualizacion = this[CLAVE_ULTIMA_ACTUALIZACION]
            ?: System.currentTimeMillis()
    )

    private fun MutablePreferences.escribirVitals(vitals: ZaziVitals) {
        this[CLAVE_PANCITA] = vitals.pancita
        this[CLAVE_FELICIDAD] = vitals.felicidad
        this[CLAVE_ENERGIA] = vitals.energia
        this[CLAVE_HIGIENE] = vitals.higiene
        this[CLAVE_DORMIDA] = vitals.dormida
        this[CLAVE_ULTIMA_ACTUALIZACION] = vitals.ultimaActualizacion
    }

    /**
     * Primera lectura de una instalación: siembra vitales e inventario de
     * bienvenida dentro del mismo edit para que ninguna acción posterior
     * pueda pisar los valores por defecto.
     */
    private fun MutablePreferences.prepararSiEsNueva(ahora: Long) {
        if (this[CLAVE_ULTIMA_ACTUALIZACION] != null) return
        escribirVitals(ZaziVitals(ultimaActualizacion = ahora))
        this[CLAVE_CONSUMIBLES] = CONSUMIBLES_BIENVENIDA.aClaves()
        this[CLAVE_ROPA_COMPRADA] = emptySet()
        this[CLAVE_FONDOS_COMPRADOS] = emptySet()
        this[CLAVE_ROPA_EQUIPADA] = emptySet()
    }

    private fun Preferences.consumiblesMapa(): Map<String, Int> {
        val guardado = this[CLAVE_CONSUMIBLES]
            ?: return CONSUMIBLES_BIENVENIDA
        return guardado.mapNotNull { entrada ->
            val partes = entrada.split(SEPARADOR, limit = 2)
            if (partes.size != 2) return@mapNotNull null
            val cantidad = partes[1].toIntOrNull() ?: return@mapNotNull null
            if (partes[0].isBlank() || cantidad <= 0) return@mapNotNull null
            partes[0] to cantidad
        }.toMap()
    }

    private fun Map<String, Int>.aClaves(): Set<String> =
        entries.filter { it.value > 0 }.map { (id, cantidad) -> "$id$SEPARADOR$cantidad" }.toSet()

    private fun Preferences.ropaEquipadaMapa(): Map<String, String> {
        val guardado = this[CLAVE_ROPA_EQUIPADA] ?: return emptyMap()
        return guardado.mapNotNull { entrada ->
            val partes = entrada.split(SEPARADOR, limit = 2)
            if (partes.size != 2) return@mapNotNull null
            partes[0] to partes[1]
        }.toMap()
    }

    private fun Map<String, String>.aClavesEquipo(): Set<String> =
        entries.map { (ranura, ropaId) -> "$ranura$SEPARADOR$ropaId" }.toSet()

    companion object {
        private const val TAG = "ZaziPetCare"
        private const val SEPARADOR = '='
        private const val INTERVALO_TICK_MS = 60_000L

        private const val ENERGIA_MINIMA_DESPERTAR = 22f

        private const val CARICIA_FELICIDAD = 6f
        private const val FELICIDAD_ESTRENO = 4f
        private const val FELICIDAD_ESTUDIO = 3f
        private const val FELICIDAD_MINIJUEGO = 8f

        private val CLAVE_PANCITA = floatPreferencesKey("pancita")
        private val CLAVE_FELICIDAD = floatPreferencesKey("felicidad")
        private val CLAVE_ENERGIA = floatPreferencesKey("energia")
        private val CLAVE_HIGIENE = floatPreferencesKey("higiene")
        private val CLAVE_DORMIDA = booleanPreferencesKey("dormida")
        private val CLAVE_ULTIMA_ACTUALIZACION = longPreferencesKey("ultima_actualizacion")
        private val CLAVE_CONSUMIBLES = stringSetPreferencesKey("consumibles")
        private val CLAVE_ROPA_COMPRADA = stringSetPreferencesKey("ropa_comprada")
        private val CLAVE_FONDOS_COMPRADOS = stringSetPreferencesKey("fondos_comprados")
        private val CLAVE_ROPA_EQUIPADA = stringSetPreferencesKey("ropa_equipada")
        private val CLAVE_FONDO_EQUIPADO = stringPreferencesKey("fondo_equipado")

        @Volatile
        private var instancia: ZaziPetCareEngine? = null

        fun obtener(context: Context): ZaziPetCareEngine =
            instancia ?: synchronized(this) {
                instancia ?: ZaziPetCareEngine(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}

private fun Float.aPorcentaje(): Int = roundToInt().coerceIn(0, 100)
