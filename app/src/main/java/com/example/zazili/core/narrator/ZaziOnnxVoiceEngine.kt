package com.example.zazili.core.narrator

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Fachada global de la voz de Zazi.
 *
 * Reutiliza la única sesión Piper/ONNX del proceso ([NeuralVoiceEngine]), así
 * que no duplica el modelo de 61 MB ni abre un segundo motor. [speak] es no
 * bloqueante: la síntesis y la reproducción corren fuera del hilo principal,
 * sobre `Dispatchers.IO`, heredado de [NeuralVoiceEngine].
 *
 * Este camino es estrictamente neural: si el modelo todavía está cargando, el
 * texto queda en espera unos segundos y se pronuncia en cuanto la sesión está
 * lista; si el modelo falló en este dispositivo, la espera termina en silencio.
 * Nunca se cae a la voz del sistema, porque esa voz no es la de Zazi.
 */
object ZaziOnnxVoiceEngine {

    private const val ESPERA_MAXIMA_MS = 8_000L
    private const val PASO_ESPERA_MS = 150L

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val generacion = AtomicLong(0)

    @Volatile
    private var inicializado = false

    /** Registra el contexto de aplicación una sola vez (idempotente). */
    @Synchronized
    fun initialize(context: Context) {
        if (inicializado) return
        inicializado = true
        NeuralVoiceEngine.initialize(context.applicationContext)
    }

    /**
     * Sintetiza y reproduce [texto] con el modelo local. Corta cualquier
     * narración anterior antes de empezar. Si la sesión aún se está cargando,
     * el texto espera unos segundos; un [stop] o un [speak] nuevo lo descarta.
     */
    fun speak(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        val gen = generacion.incrementAndGet()
        if (NeuralVoiceEngine.usaVozNeural()) {
            NeuralVoiceEngine.speakHumanized(limpio)
            return
        }
        ambito.launch { esperarMotorYHablar(limpio, gen) }
    }

    /** Interrumpe la voz en curso y descarta cualquier texto en espera. */
    fun stop() {
        generacion.incrementAndGet()
        NeuralVoiceEngine.detener()
    }

    /** `true` cuando la sesión ONNX quedó operativa en este proceso. */
    fun disponible(): Boolean = NeuralVoiceEngine.usaVozNeural()

    /** Libera la sesión al cerrar la app (terminal para este proceso). */
    fun liberar() {
        generacion.incrementAndGet()
        NeuralVoiceEngine.liberar()
    }

    private suspend fun esperarMotorYHablar(texto: String, gen: Long) {
        var esperado = 0L
        while (esperado < ESPERA_MAXIMA_MS) {
            if (generacion.get() != gen) return
            delay(PASO_ESPERA_MS)
            esperado += PASO_ESPERA_MS
            if (NeuralVoiceEngine.usaVozNeural()) {
                if (generacion.get() == gen) NeuralVoiceEngine.speakHumanized(texto)
                return
            }
        }
    }
}
