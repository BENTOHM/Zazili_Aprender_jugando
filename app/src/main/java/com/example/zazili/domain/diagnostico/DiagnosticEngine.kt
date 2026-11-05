package com.example.zazili.domain.diagnostico

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Las tres estaciones invisibles de "El Vuelo de Prueba". */
enum class FaseDiagnostico { LECTURA, SEMILLAS, VUELO }

/** Cómo resolvió el niño las 12 semillas. */
enum class EstrategiaSemillas { CONTEO_UNITARIO, AGRUPACION_PARCIAL, DECENAS }

/** Respuesta conductual ante el fracaso inevitable del primer vuelo. */
enum class PerfilFrustracion { IMPULSIVO, RESILIENTE, BAJA_TOLERANCIA }

@Immutable
data class EstadoDiagnostico(
    val fase: FaseDiagnostico = FaseDiagnostico.LECTURA,
    val semillasEnSurco: Int = 0,
    val movimientosSemillas: Int = 0,
    val usoBolsaDiez: Boolean = false,
    val usoBolsaDos: Boolean = false,
    val audioTocado: Boolean = false,
    val audioATiempo: Boolean = false,
    val falloDeVuelo: Boolean = false,
    val resultado: ResultadoDiagnostico? = null
) {
    val semillasRestantes: Int get() = (TOTAL_SEMILLAS - semillasEnSurco).coerceAtLeast(0)
    val surcoCompleto: Boolean get() = semillasEnSurco >= TOTAL_SEMILLAS
}

/** Línea base local que alimentará lecciones, pistas y ayudas. */
@Immutable
data class ResultadoDiagnostico(
    val nivelMatematicas: Int,
    val nivelLenguaje: Int,
    val apoyoAudio: Boolean,
    val perfilFrustracion: PerfilFrustracion,
    val latenciaReintentoMs: Long,
    val estrategiaSemillas: EstrategiaSemillas,
    val movimientosSemillas: Int,
    val tiempoSemillasMs: Long,
    val tiempoLecturaMs: Long,
    val abandono: Boolean
)

/**
 * Motor puro de telemetría del onboarding.
 *
 * No conoce Compose ni DataStore: recibe eventos desde la pantalla y publica
 * un [EstadoDiagnostico] inmutable. El reloj se inyecta para poder medir la
 * ventana de 5 s del altavoz y la latencia de reintento sin depender del
 * ciclo de frames (una composición lenta nunca altera la medición).
 */
class DiagnosticEngine(
    private val edad: Int,
    private val reloj: () -> Long = { SystemClock.elapsedRealtime() }
) {

    private val inicioLecturaMs = reloj()
    private var finLecturaMs = 0L
    private var inicioSemillasMs = 0L
    private var finSemillasMs = 0L
    private var falloVueloMs = 0L

    private val _estado = MutableStateFlow(EstadoDiagnostico())
    val estado: StateFlow<EstadoDiagnostico> = _estado.asStateFlow()

    /** Primer toque al altavoz: solo cuenta si ocurre dentro de los 5 s. */
    @Synchronized
    fun tocarAltavoz() {
        val actual = _estado.value
        if (actual.fase != FaseDiagnostico.LECTURA || actual.audioTocado) return
        val aTiempo = (reloj() - inicioLecturaMs) <= VENTANA_APOYO_AUDIO_MS
        _estado.value = actual.copy(audioTocado = true, audioATiempo = aTiempo)
    }

    @Synchronized
    fun terminarLectura() {
        val actual = _estado.value
        if (actual.fase != FaseDiagnostico.LECTURA) return
        finLecturaMs = reloj()
        inicioSemillasMs = finLecturaMs
        _estado.value = actual.copy(fase = FaseDiagnostico.SEMILLAS)
    }

    /**
     * Acepta una semilla suelta (1) o una bolsa (2/10). Devuelve `false` si la
     * entrega no cabe o la fase ya cambió, para que la UI pueda regresar la
     * pieza a su sitio sin corromper el conteo.
     */
    @Synchronized
    fun soltarSemillas(cantidad: Int): Boolean {
        if (cantidad != 1 && cantidad != SEMILLAS_BOLSA_CHICA && cantidad != SEMILLAS_BOLSA_GRANDE) {
            return false
        }
        val actual = _estado.value
        if (actual.fase != FaseDiagnostico.SEMILLAS) return false
        if (cantidad > actual.semillasRestantes) return false
        _estado.value = actual.copy(
            semillasEnSurco = actual.semillasEnSurco + cantidad,
            movimientosSemillas = actual.movimientosSemillas + 1,
            usoBolsaDiez = actual.usoBolsaDiez || cantidad == SEMILLAS_BOLSA_GRANDE,
            usoBolsaDos = actual.usoBolsaDos || cantidad == SEMILLAS_BOLSA_CHICA
        )
        return true
    }

    @Synchronized
    fun terminarSemillas() {
        val actual = _estado.value
        if (actual.fase != FaseDiagnostico.SEMILLAS || !actual.surcoCompleto) return
        finSemillasMs = reloj()
        _estado.value = actual.copy(fase = FaseDiagnostico.VUELO)
    }

    /** Se llama cuando la ráfaga ya es visible y aparece el botón de reintento. */
    @Synchronized
    fun fallarVuelo() {
        val actual = _estado.value
        if (actual.fase != FaseDiagnostico.VUELO || actual.falloDeVuelo) return
        if (actual.resultado != null) return
        falloVueloMs = reloj()
        _estado.value = actual.copy(falloDeVuelo = true)
    }

    @Synchronized
    fun reintentar(): ResultadoDiagnostico? {
        val actual = _estado.value
        if (actual.resultado != null) return actual.resultado
        if (!actual.falloDeVuelo || falloVueloMs <= 0L) return null
        val latencia = (reloj() - falloVueloMs).coerceAtLeast(0L)
        val perfil = when {
            latencia < LATENCIA_IMPULSIVO_MS -> PerfilFrustracion.IMPULSIVO
            latencia <= LATENCIA_RESILIENTE_MS -> PerfilFrustracion.RESILIENTE
            else -> PerfilFrustracion.BAJA_TOLERANCIA
        }
        return publicarResultado(actual, perfil, latencia, abandono = false)
    }

    /**
     * Salida digna: "Ahora no" también es una respuesta válida. Se registra
     * como baja tolerancia y jamás deja al niño atrapado en la pantalla.
     */
    @Synchronized
    fun abandonarVuelo(): ResultadoDiagnostico? {
        val actual = _estado.value
        if (actual.resultado != null) return actual.resultado
        if (!actual.falloDeVuelo) return null
        val latencia = if (falloVueloMs > 0L) {
            (reloj() - falloVueloMs).coerceAtLeast(0L)
        } else {
            0L
        }
        return publicarResultado(
            actual = actual,
            perfil = PerfilFrustracion.BAJA_TOLERANCIA,
            latencia = latencia,
            abandono = true
        )
    }

    private fun publicarResultado(
        actual: EstadoDiagnostico,
        perfil: PerfilFrustracion,
        latencia: Long,
        abandono: Boolean
    ): ResultadoDiagnostico {
        val estrategia = when {
            actual.usoBolsaDiez -> EstrategiaSemillas.DECENAS
            actual.usoBolsaDos -> EstrategiaSemillas.AGRUPACION_PARCIAL
            else -> EstrategiaSemillas.CONTEO_UNITARIO
        }
        val nivelMatematicas = when (estrategia) {
            EstrategiaSemillas.DECENAS -> 3
            EstrategiaSemillas.AGRUPACION_PARCIAL -> 2
            EstrategiaSemillas.CONTEO_UNITARIO -> 1
        }
        val nivelLenguaje = when {
            actual.audioATiempo -> 1
            edad >= 10 -> 5
            edad == 9 -> 4
            edad == 8 -> 3
            else -> 2
        }
        val resultado = ResultadoDiagnostico(
            nivelMatematicas = nivelMatematicas,
            nivelLenguaje = nivelLenguaje,
            apoyoAudio = actual.audioATiempo,
            perfilFrustracion = perfil,
            latenciaReintentoMs = latencia,
            estrategiaSemillas = estrategia,
            movimientosSemillas = actual.movimientosSemillas,
            tiempoSemillasMs = (finSemillasMs - inicioSemillasMs).coerceAtLeast(0L),
            tiempoLecturaMs = (finLecturaMs - inicioLecturaMs).coerceAtLeast(0L),
            abandono = abandono
        )
        _estado.value = actual.copy(resultado = resultado)
        return resultado
    }
}

const val TOTAL_SEMILLAS = 12
const val SEMILLAS_BOLSA_GRANDE = 10
const val SEMILLAS_BOLSA_CHICA = 2

/** Ventana para considerar que el niño pidió apoyo de audio. */
const val VENTANA_APOYO_AUDIO_MS = 5_000L

/** Menos de 2.5 s sugiere impulso; más de 15 s, baja tolerancia a la espera. */
const val LATENCIA_IMPULSIVO_MS = 2_500L
const val LATENCIA_RESILIENTE_MS = 15_000L
