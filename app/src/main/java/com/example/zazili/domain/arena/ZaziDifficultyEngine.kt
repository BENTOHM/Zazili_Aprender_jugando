package com.example.zazili.domain.arena

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.example.zazili.domain.minijuegos.MinigameEngine
import com.example.zazili.domain.minijuegos.Minijuego
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DDA de la Arena Cortex: un autómata por minijuego que mueve dos palancas.
 *
 *  1. **Carga cognitiva** ([ParametrosDificultad]): nivel 1..5, número de
 *     opciones, N-Back (1 → 2 → 3) y el salto de aritmética entera a fracciones
 *     y decimales.
 *  2. **Andamiaje invisible** ([NivelAndamiaje]): en cuanto el niño encadena
 *     fallos o reintenta, la misma pantalla baja variables y suma apoyos
 *     visuales (pista fija, texto biónico, palabras resaltadas). Nunca aparece
 *     un "Game Over": solo cambia el tamaño del escalón.
 *
 * Las decisiones se toman con una media móvil exponencial (EMA) de acierto y
 * de tiempo de reacción, no con el acumulado histórico: así un mal comienzo no
 * condena la sesión y una racha buena no dispara el nivel de golpe. Cada
 * transición reinicia la EMA a un valor neutro para que el siguiente cambio
 * tenga que ganarse otra vez.
 *
 * Todo se persiste en `SharedPreferences` (archivo [ARCHIVO_METRICAS]); con
 * `context` nulo el motor funciona en memoria, que es lo que usan las pruebas.
 */

const val ARCHIVO_METRICAS = "zazili_arena_dda"

/** Respuestas mínimas antes de mover cualquier palanca. */
const val RESPUESTAS_MINIMAS = 5

/** Éxito reciente que habilita subir carga cognitiva o soltar apoyo. */
const val UMBRAL_ESCALADA = 0.85f

/** Éxito reciente por debajo del cual se protege al niño con andamiaje. */
const val UMBRAL_PROTECCION = 0.55f

/** Reintentos por respuesta a partir de los cuales se considera baja tolerancia. */
const val UMBRAL_TOLERANCIA = 0.40f

/** Fallos consecutivos que encienden el andamiaje. */
const val FALLOS_PARA_APOYO = 2

/** Aciertos consecutivos que devuelven un escalón de apoyo. */
const val ACIERTOS_PARA_SOLTAR = 4

/** Aciertos consecutivos que habilitan subir un nivel. */
const val ACIERTOS_PARA_ESCALAR = 3

/** Peso de la respuesta nueva en la media móvil (0..1). */
const val SUAVIZADO_EMA = 0.35f

/** Fracción del reloj de ronda que se considera tiempo de reacción cómodo. */
private const val FRACCION_TIEMPO_COMODO = 700L

/**
 * Escalera de apoyos. El [peso] permite comparar niveles sin recordar el orden
 * y el [aireExtraSegundos] alimenta el reloj de ronda efectivo.
 */
@Immutable
enum class NivelAndamiaje(
    val etiqueta: String,
    val peso: Int,
    val aireExtraSegundos: Int,
    val opcionesMenos: Int,
    val pistaSiempreVisible: Boolean,
    val textoBionico: Boolean,
    val resaltados: Boolean
) {
    NINGUNO(
        etiqueta = "Sin apoyo",
        peso = 0,
        aireExtraSegundos = 0,
        opcionesMenos = 0,
        pistaSiempreVisible = false,
        textoBionico = false,
        resaltados = false
    ),
    PISTA(
        etiqueta = "Pista siempre a la vista",
        peso = 1,
        aireExtraSegundos = 4,
        opcionesMenos = 0,
        pistaSiempreVisible = true,
        textoBionico = false,
        resaltados = false
    ),
    RESALTADO(
        etiqueta = "Texto biónico y palabras clave resaltadas",
        peso = 2,
        aireExtraSegundos = 8,
        opcionesMenos = 1,
        pistaSiempreVisible = true,
        textoBionico = true,
        resaltados = true
    ),
    FRACCIONADO(
        etiqueta = "Menos variables en pantalla",
        peso = 3,
        aireExtraSegundos = 12,
        opcionesMenos = 2,
        pistaSiempreVisible = true,
        textoBionico = true,
        resaltados = true
    );

    val hayApoyo: Boolean get() = this != NINGUNO

    fun siguiente(): NivelAndamiaje = entries.getOrElse(ordinal + 1) { this }

    fun anterior(): NivelAndamiaje = entries.getOrElse(ordinal - 1) { this }
}

/**
 * Parámetros listos para [ProceduralContentEngine.generar]. La pantalla no
 * decide nada: pide estos parámetros y el contenido sale ya adaptado.
 */
@Immutable
data class ParametrosDificultad(
    val nivel: Int = Minijuego.NIVEL_MINIMO,
    val opciones: Int = 4,
    val segundosPorRonda: Int = 22,
    val nBack: Int = 1,
    val incluyeFracciones: Boolean = false,
    val incluyeDecimales: Boolean = false,
    val andamiaje: NivelAndamiaje = NivelAndamiaje.NINGUNO
) {
    /** N-Back es memoria de trabajo: nunca se pide más de 3 atrás. */
    val cargaDeMemoria: String get() = if (nBack <= 1) "1-Back" else "$nBack-Back"
}

/** Señales crudas del niño, tal como las registra una sesión. */
@Immutable
data class MetricasJuego(
    val respuestas: Int = 0,
    val aciertos: Int = 0,
    val tiempoTotalMs: Long = 0L,
    val reintentos: Int = 0,
    val fallosSeguidos: Int = 0,
    val aciertosSeguidos: Int = 0,
    val tasaExitoReciente: Float = -1f,
    val tiempoRecienteMs: Long = -1L
) {
    val tasaExito: Float
        get() = if (respuestas <= 0) 0f else aciertos.toFloat() / respuestas

    val tiempoPromedioMs: Long
        get() = if (respuestas <= 0) 0L else tiempoTotalMs / respuestas

    /**
     * 1 = ninguna ronda necesitó reintento; 0 = cada ronda necesitó uno.
     * Es el termómetro de frustración que usa el DDA para proteger antes.
     */
    val toleranciaFrustracion: Float
        get() = if (respuestas <= 0) {
            0.5f
        } else {
            (1f - reintentos.toFloat() / respuestas).coerceIn(0f, 1f)
        }

    val muestraSuficiente: Boolean get() = respuestas >= RESPUESTAS_MINIMAS
}

/** Fase del autómata; explica en una palabra qué está haciendo el sistema. */
@Immutable
enum class FaseAprendizaje(val etiqueta: String, val mensaje: String) {
    CALIBRACION(
        etiqueta = "Calibrando",
        mensaje = "Vamos conociéndonos. Responde a tu ritmo."
    ),
    FLUJO(
        etiqueta = "En flujo",
        mensaje = "Vas bien. Sigue así."
    ),
    RETO(
        etiqueta = "Reto justo",
        mensaje = "Se puso más interesante: tú puedes."
    ),
    CONSOLIDACION(
        etiqueta = "Consolidando",
        mensaje = "Casi lo tienes. Con un intento más quito el apoyo."
    ),
    ANDAMIAJE(
        etiqueta = "Andamiaje invisible",
        mensaje = "Te acompaño: menos opciones y más tiempo."
    )
}

/** Apoyos concretos que la pantalla debe pintar. */
@Immutable
data class AjusteAndamiaje(
    val nivel: NivelAndamiaje,
    val opcionesMenos: Int,
    val aireExtraSegundos: Int,
    val pistaSiempreVisible: Boolean,
    val textoBionico: Boolean,
    val resaltarPalabrasClave: Boolean
) {
    val activo: Boolean get() = nivel.hayApoyo

    companion object {
        fun desde(nivel: NivelAndamiaje): AjusteAndamiaje = AjusteAndamiaje(
            nivel = nivel,
            opcionesMenos = nivel.opcionesMenos,
            aireExtraSegundos = nivel.aireExtraSegundos,
            pistaSiempreVisible = nivel.pistaSiempreVisible,
            textoBionico = nivel.textoBionico,
            resaltarPalabrasClave = nivel.resaltados
        )
    }
}

/** Fotografía completa del estado adaptativo de un minijuego. */
@Immutable
data class EstadoDificultad(
    val juegoId: String,
    val nivel: Int,
    val fase: FaseAprendizaje,
    val metricas: MetricasJuego,
    val parametros: ParametrosDificultad,
    val ajuste: AjusteAndamiaje,
    val mensajeNino: String
) {
    val enFlujo: Boolean get() = fase == FaseAprendizaje.FLUJO

    val nivelEtiqueta: String get() = "Nivel $nivel de ${Minijuego.NIVEL_MAXIMO}"

    val cargaDeMemoria: String get() = parametros.cargaDeMemoria
}

private enum class CambioDda { NINGUNO, ESCALADA, APOYO_SUBIO, APOYO_BAJO }

private data class Decision(
    val nivel: Int,
    val andamiaje: NivelAndamiaje,
    val cambio: CambioDda
)

/**
 * Autómata de dificultad por minijuego.
 *
 * Es seguro llamarlo desde varios hilos: las transiciones están serializadas y
 * cada una escribe su resultado antes de publicarlo, de modo que dos respuestas
 * simultáneas no pueden leer la misma métrica vieja.
 */
class ZaziDifficultyEngine(
    context: Context? = null,
    private val nivelInicial: (String) -> Int = { Minijuego.NIVEL_MINIMO }
) {

    private val preferencias: SharedPreferences? = context
        ?.applicationContext
        ?.getSharedPreferences(ARCHIVO_METRICAS, Context.MODE_PRIVATE)

    private val _estados = MutableStateFlow<Map<String, EstadoDificultad>>(emptyMap())

    /** Estado de todos los juegos ya abiertos; reactivo para Compose. */
    val estados: StateFlow<Map<String, EstadoDificultad>> = _estados.asStateFlow()

    fun estado(juegoId: String): EstadoDificultad = synchronized(this) {
        _estados.value[juegoId] ?: leer(juegoId).also(::publicar)
    }

    fun parametros(juegoId: String): ParametrosDificultad = estado(juegoId).parametros

    fun nivel(juegoId: String): Int = estado(juegoId).nivel

    /**
     * Acierto. [reaccionMs] es lo que tardó el niño desde que vio el reto; con
     * 0 o negativo el tiempo no cuenta (leer la consigna ya es trabajo).
     */
    fun registrarAcierto(juegoId: String, reaccionMs: Long = 0L): EstadoDificultad =
        registrar(juegoId, correcto = true, reaccionMs = reaccionMs)

    fun registrarFallo(juegoId: String, reaccionMs: Long = 0L): EstadoDificultad =
        registrar(juegoId, correcto = false, reaccionMs = reaccionMs)

    /**
     * Reintento sobre el mismo reto. No resta puntos ni vidas: solo le dice al
     * sistema que este niño necesita el mismo escalón más chico.
     */
    fun registrarReintento(juegoId: String): EstadoDificultad = synchronized(this) {
        val previo = estado(juegoId)
        val metricas = previo.metricas.copy(
            reintentos = previo.metricas.reintentos + 1
        )
        val sube = metricas.toleranciaFrustracion < UMBRAL_TOLERANCIA ||
            metricas.fallosSeguidos >= 1
        val andamiaje = if (sube) previo.parametros.andamiaje.siguiente()
        else previo.parametros.andamiaje
        val cambio = if (andamiaje != previo.parametros.andamiaje) {
            CambioDda.APOYO_SUBIO
        } else {
            CambioDda.NINGUNO
        }
        val nuevo = construir(
            juegoId = juegoId,
            nivel = previo.nivel,
            andamiaje = andamiaje,
            metricas = if (cambio == CambioDda.APOYO_SUBIO) {
                metricas.copy(tasaExitoReciente = 0.5f, tiempoRecienteMs = -1L)
            } else {
                metricas
            },
            fase = fasePara(metricas, andamiaje, cambio)
        )
        publicar(nuevo)
        guardar(nuevo)
        nuevo
    }

    /** Vuelve a calibrar: útil al cambiar de sesión o al empezar de nuevo. */
    fun reiniciar(juegoId: String) = synchronized(this) {
        val claves = clavesDe(juegoId)
        preferencias?.edit()?.apply {
            claves.forEach { remove(it) }
            apply()
        }
        _estados.value = _estados.value - juegoId
    }

    // ------------------------- transición -------------------------

    private fun registrar(
        juegoId: String,
        correcto: Boolean,
        reaccionMs: Long
    ): EstadoDificultad = synchronized(this) {
        val previo = estado(juegoId)
        val metricas = actualizar(previo.metricas, correcto, reaccionMs)
        val decision = decidir(previo, metricas)
        val metricasFinales = if (decision.cambio == CambioDda.NINGUNO) {
            metricas
        } else {
            metricas.copy(
                fallosSeguidos = 0,
                aciertosSeguidos = 0,
                tasaExitoReciente = 0.5f,
                tiempoRecienteMs = -1L
            )
        }
        val nuevo = construir(
            juegoId = juegoId,
            nivel = decision.nivel,
            andamiaje = decision.andamiaje,
            metricas = metricasFinales,
            fase = fasePara(metricas, decision.andamiaje, decision.cambio)
        )
        publicar(nuevo)
        guardar(nuevo)
        nuevo
    }

    private fun actualizar(
        previa: MetricasJuego,
        correcto: Boolean,
        reaccionMs: Long
    ): MetricasJuego {
        val exito = if (correcto) 1f else 0f
        val ema = if (previa.tasaExitoReciente < 0f) {
            exito
        } else {
            previa.tasaExitoReciente +
                SUAVIZADO_EMA * (exito - previa.tasaExitoReciente)
        }
        val tiempo = when {
            reaccionMs <= 0L -> previa.tiempoRecienteMs
            previa.tiempoRecienteMs <= 0L -> reaccionMs
            else -> (
                previa.tiempoRecienteMs +
                    SUAVIZADO_EMA * (reaccionMs - previa.tiempoRecienteMs)
                ).toLong()
        }
        return previa.copy(
            respuestas = previa.respuestas + 1,
            aciertos = previa.aciertos + if (correcto) 1 else 0,
            tiempoTotalMs = if (reaccionMs > 0L) {
                previa.tiempoTotalMs + reaccionMs
            } else {
                previa.tiempoTotalMs
            },
            fallosSeguidos = if (correcto) 0 else previa.fallosSeguidos + 1,
            aciertosSeguidos = if (correcto) previa.aciertosSeguidos + 1 else 0,
            tasaExitoReciente = ema,
            tiempoRecienteMs = tiempo
        )
    }

    /**
     * Dos lecturas por respuesta, en este orden:
     *  - proteger primero (el apoyo se enciende antes que la exigencia),
     *  - exigir después (nivel arriba solo con éxito y tiempos cómodos).
     */
    private fun decidir(previo: EstadoDificultad, metricas: MetricasJuego): Decision {
        val apoyo = previo.parametros.andamiaje
        val nivel = previo.nivel
        val objetivoMs = MinigameEngine.segundosParaNivel(nivel) * FRACCION_TIEMPO_COMODO
        val tiempoComodo = metricas.tiempoRecienteMs <= 0L ||
            metricas.tiempoRecienteMs <= objetivoMs
        val protegido = metricas.fallosSeguidos >= FALLOS_PARA_APOYO ||
            (metricas.tasaExitoReciente >= 0f &&
                metricas.tasaExitoReciente < UMBRAL_PROTECCION &&
                metricas.fallosSeguidos >= 1)
        val pocaTolerancia = metricas.toleranciaFrustracion < UMBRAL_TOLERANCIA &&
            metricas.fallosSeguidos >= 1
        val vaMuyBien = metricas.muestraSuficiente &&
            metricas.tasaExitoReciente >= UMBRAL_ESCALADA &&
            metricas.aciertosSeguidos >= ACIERTOS_PARA_ESCALAR &&
            tiempoComodo

        return when {
            protegido || pocaTolerancia -> Decision(
                nivel = nivel,
                andamiaje = apoyo.siguiente(),
                cambio = if (apoyo.siguiente() != apoyo) {
                    CambioDda.APOYO_SUBIO
                } else {
                    CambioDda.NINGUNO
                }
            )

            vaMuyBien && apoyo.hayApoyo -> Decision(
                nivel = nivel,
                andamiaje = apoyo.anterior(),
                cambio = CambioDda.APOYO_BAJO
            )

            vaMuyBien && nivel < Minijuego.NIVEL_MAXIMO -> Decision(
                nivel = nivel + 1,
                andamiaje = NivelAndamiaje.NINGUNO,
                cambio = CambioDda.ESCALADA
            )

            else -> Decision(nivel, apoyo, CambioDda.NINGUNO)
        }
    }

    private fun fasePara(
        metricas: MetricasJuego,
        andamiaje: NivelAndamiaje,
        cambio: CambioDda
    ): FaseAprendizaje = when {
        cambio == CambioDda.APOYO_SUBIO -> FaseAprendizaje.ANDAMIAJE
        cambio == CambioDda.APOYO_BAJO -> FaseAprendizaje.CONSOLIDACION
        cambio == CambioDda.ESCALADA -> FaseAprendizaje.RETO
        andamiaje.hayApoyo -> if (metricas.tasaExitoReciente < UMBRAL_PROTECCION) {
            FaseAprendizaje.ANDAMIAJE
        } else {
            FaseAprendizaje.CONSOLIDACION
        }
        !metricas.muestraSuficiente -> FaseAprendizaje.CALIBRACION
        else -> FaseAprendizaje.FLUJO
    }

    // ------------------------- construcción -------------------------

    private fun construir(
        juegoId: String,
        nivel: Int,
        andamiaje: NivelAndamiaje,
        metricas: MetricasJuego,
        fase: FaseAprendizaje
    ): EstadoDificultad {
        val nivelSeguro = nivel.coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO)
        val opcionesBase = when (nivelSeguro) {
            1, 2 -> 3
            3, 4 -> 4
            else -> 5
        }
        val parametros = ParametrosDificultad(
            nivel = nivelSeguro,
            opciones = (opcionesBase - andamiaje.opcionesMenos).coerceIn(2, 5),
            segundosPorRonda = MinigameEngine.segundosParaNivel(nivelSeguro) +
                andamiaje.aireExtraSegundos,
            nBack = when (nivelSeguro) {
                1, 2 -> 1
                3, 4 -> 2
                else -> 3
            },
            incluyeFracciones = nivelSeguro >= 3,
            incluyeDecimales = nivelSeguro >= 5,
            andamiaje = andamiaje
        )
        return EstadoDificultad(
            juegoId = juegoId,
            nivel = nivelSeguro,
            fase = fase,
            metricas = metricas,
            parametros = parametros,
            ajuste = AjusteAndamiaje.desde(andamiaje),
            mensajeNino = fase.mensaje
        )
    }

    // ------------------------- persistencia -------------------------

    private fun leer(juegoId: String): EstadoDificultad {
        val p = preferencias
        val nivelGuardado = p?.getInt(clave(juegoId, NIVEL), 0) ?: 0
        val nivel = if (nivelGuardado in Minijuego.NIVEL_MINIMO..Minijuego.NIVEL_MAXIMO) {
            nivelGuardado
        } else {
            runCatching { nivelInicial(juegoId) }.getOrDefault(Minijuego.NIVEL_MINIMO)
        }
        val andamiaje = NivelAndamiaje.entries.getOrElse(
            p?.getInt(clave(juegoId, APOYO), 0) ?: 0
        ) { NivelAndamiaje.NINGUNO }
        val metricas = MetricasJuego(
            respuestas = p?.getInt(clave(juegoId, RESPUESTAS), 0) ?: 0,
            aciertos = p?.getInt(clave(juegoId, ACIERTOS), 0) ?: 0,
            tiempoTotalMs = p?.getLong(clave(juegoId, TIEMPO_TOTAL), 0L) ?: 0L,
            reintentos = p?.getInt(clave(juegoId, REINTENTOS), 0) ?: 0,
            fallosSeguidos = p?.getInt(clave(juegoId, FALLOS_SEGUIDOS), 0) ?: 0,
            aciertosSeguidos = p?.getInt(clave(juegoId, ACIERTOS_SEGUIDOS), 0) ?: 0,
            tasaExitoReciente = p?.getFloat(clave(juegoId, EMA_EXITO), -1f) ?: -1f,
            tiempoRecienteMs = p?.getLong(clave(juegoId, EMA_TIEMPO), -1L) ?: -1L
        )
        return construir(
            juegoId = juegoId,
            nivel = nivel,
            andamiaje = andamiaje,
            metricas = metricas,
            fase = fasePara(metricas, andamiaje, CambioDda.NINGUNO)
        )
    }

    private fun guardar(estado: EstadoDificultad) {
        val p = preferencias ?: return
        p.edit()
            .putInt(clave(estado.juegoId, NIVEL), estado.nivel)
            .putInt(clave(estado.juegoId, APOYO), estado.parametros.andamiaje.ordinal)
            .putInt(clave(estado.juegoId, RESPUESTAS), estado.metricas.respuestas)
            .putInt(clave(estado.juegoId, ACIERTOS), estado.metricas.aciertos)
            .putLong(clave(estado.juegoId, TIEMPO_TOTAL), estado.metricas.tiempoTotalMs)
            .putInt(clave(estado.juegoId, REINTENTOS), estado.metricas.reintentos)
            .putInt(clave(estado.juegoId, FALLOS_SEGUIDOS), estado.metricas.fallosSeguidos)
            .putInt(clave(estado.juegoId, ACIERTOS_SEGUIDOS), estado.metricas.aciertosSeguidos)
            .putFloat(clave(estado.juegoId, EMA_EXITO), estado.metricas.tasaExitoReciente)
            .putLong(clave(estado.juegoId, EMA_TIEMPO), estado.metricas.tiempoRecienteMs)
            .apply()
    }

    private fun publicar(estado: EstadoDificultad) {
        _estados.value = _estados.value + (estado.juegoId to estado)
    }

    private fun clave(juegoId: String, campo: String): String = "dda_${juegoId}_$campo"

    private fun clavesDe(juegoId: String): List<String> = listOf(
        NIVEL,
        APOYO,
        RESPUESTAS,
        ACIERTOS,
        TIEMPO_TOTAL,
        REINTENTOS,
        FALLOS_SEGUIDOS,
        ACIERTOS_SEGUIDOS,
        EMA_EXITO,
        EMA_TIEMPO
    ).map { clave(juegoId, it) }

    private companion object {
        const val NIVEL = "nivel"
        const val APOYO = "apoyo"
        const val RESPUESTAS = "respuestas"
        const val ACIERTOS = "aciertos"
        const val TIEMPO_TOTAL = "tiempo_total"
        const val REINTENTOS = "reintentos"
        const val FALLOS_SEGUIDOS = "fallos_seguidos"
        const val ACIERTOS_SEGUIDOS = "aciertos_seguidos"
        const val EMA_EXITO = "ema_exito"
        const val EMA_TIEMPO = "ema_tiempo"
    }
}
