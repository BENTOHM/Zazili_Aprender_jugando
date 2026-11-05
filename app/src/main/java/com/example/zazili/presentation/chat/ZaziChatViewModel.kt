package com.example.zazili.presentation.chat

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.core.ai.ZaziChatEngine
import com.example.zazili.core.narrator.ZaziOnnxVoiceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Quién escribió el mensaje. */
enum class AutorMensaje { NINO, ZAZI }

/** Un mensaje ya resuelto, listo para pintarse en la lista. */
data class MensajeChat(
    val id: Long,
    val autor: AutorMensaje,
    val texto: String
)

/**
 * Estado completo del chat. [puedeEnviar] concentra las reglas de envío:
 * nada vacío, nada mientras Zazi piensa y nada que pase del límite de 100.
 */
data class ZaziChatUiState(
    val mensajes: List<MensajeChat> = emptyList(),
    val borrador: String = "",
    val cargando: Boolean = false,
    val vozActivada: Boolean = true
) {
    val puedeEnviar: Boolean
        get() = !cargando && borrador.isNotBlank()
}

/**
 * Estado del chat con Zazi.
 *
 * No conoce Gemini: solo llama a [ZaziChatEngine.responder] dentro de un
 * timeout y traduce sus fallos a un aviso breve. El borrador se recorta a
 * [MAX_CARACTERES] incluso si el niño pega texto largo, y el envío se bloquea
 * en cuanto arranca una respuesta para que dos toques rápidos no dupliquen
 * mensajes.
 */
class ZaziChatViewModel(application: Application) : AndroidViewModel(application) {

    private val conectividad: ConnectivityManager? =
        application.getSystemService(ConnectivityManager::class.java)

    private var siguienteId = 1L

    private val _estado = MutableStateFlow(
        ZaziChatUiState(
            mensajes = listOf(
                MensajeChat(
                    id = 0L,
                    autor = AutorMensaje.ZAZI,
                    texto = "¡Hola! Soy Zazi. ¿Qué quieres aprender hoy? Escríbeme algo cortito."
                )
            )
        )
    )
    val estado: StateFlow<ZaziChatUiState> = _estado.asStateFlow()

    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    init {
        // El modelo ya se inicializa en ZaziliApplication; esta llamada es
        // idempotente y cubre el arranque directo del chat en pruebas.
        ZaziOnnxVoiceEngine.initialize(application)
    }

    fun escribir(texto: String) {
        _estado.update { actual -> actual.copy(borrador = texto.take(MAX_CARACTERES)) }
    }

    /** Enciende o apaga la voz neural de las próximas respuestas. */
    fun alternarVoz() {
        val activa = !_estado.value.vozActivada
        _estado.update { it.copy(vozActivada = activa) }
        if (!activa) ZaziOnnxVoiceEngine.stop()
    }

    fun enviar() {
        val actual = _estado.value
        if (!actual.puedeEnviar) return

        val texto = actual.borrador.trim()
        if (texto.isEmpty()) return

        // Sin red no tiene caso esperar el timeout de 20 s. El borrador se
        // conserva para que el niño pueda reintentar cuando vuelva la señal.
        if (!hayRed()) {
            _aviso.value = AVISO_SIN_CONEXION
            return
        }

        // Un turno nuevo interrumpe la frase anterior de Zazi.
        ZaziOnnxVoiceEngine.stop()

        val mensajeNino = MensajeChat(siguienteId++, AutorMensaje.NINO, texto)
        _estado.update {
            it.copy(
                mensajes = it.mensajes + mensajeNino,
                borrador = "",
                cargando = true
            )
        }

        viewModelScope.launch {
            val respuesta: String? = try {
                withTimeout(TIEMPO_MAXIMO_MS) { ZaziChatEngine.responder(texto) }
            } catch (tiempoAgotado: TimeoutCancellationException) {
                null
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("ZaziGeminiDebug", "Gemini API Failure", error)
                null
            }

            val fallo = respuesta == null ||
                respuesta == ZaziChatEngine.MENSAJE_SIN_CONEXION ||
                respuesta == ZaziChatEngine.MENSAJE_SIN_LLAVE

            if (fallo) {
                _estado.update { it.copy(cargando = false) }
                _aviso.value = AVISO_SIN_CONEXION
            } else {
                val mensajeZazi = MensajeChat(siguienteId++, AutorMensaje.ZAZI, respuesta)
                _estado.update {
                    it.copy(mensajes = it.mensajes + mensajeZazi, cargando = false)
                }
                if (_estado.value.vozActivada) {
                    ZaziOnnxVoiceEngine.speak(respuesta)
                }
            }
        }
    }

    fun limpiarAviso() {
        _aviso.value = null
    }

    override fun onCleared() {
        ZaziOnnxVoiceEngine.stop()
        super.onCleared()
    }

    private fun hayRed(): Boolean {
        val gestor = conectividad ?: return false
        val redActiva = gestor.activeNetwork ?: return false
        val capacidades = gestor.getNetworkCapabilities(redActiva) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        const val MAX_CARACTERES = 100
        const val AVISO_SIN_CONEXION = "Zazi está dormido o sin conexión. ¡Intenta más tarde!"
        private const val TIEMPO_MAXIMO_MS = 20_000L
    }
}
