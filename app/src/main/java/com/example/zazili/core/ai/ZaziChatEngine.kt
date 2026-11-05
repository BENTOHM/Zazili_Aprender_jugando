package com.example.zazili.core.ai

import android.util.Log
import com.example.zazili.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import java.text.Normalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Conversación de Zazi con Gemini.
 *
 * La llave llega por [BuildConfig.GEMINI_API_KEY] (gradle.properties,
 * -PGEMINI_API_KEY o variable de entorno), nunca escrita en el código. El
 * modelo vive detrás de un `lazy`: si no hay llave, el motor responde con un
 * mensaje amable y no toca la red.
 *
 * Límites pedagógicos: Zazi es una herramienta educativa, no una persona, una
 * amiga ni una terapeuta. La instrucción de sistema fija esa frontera y el
 * motor la refuerza con un filtro local de apegamiento y de riesgo, antes de
 * cualquier llamada de red.
 *
 * Memoria: solo se envían los últimos [TURNOS_MEMORIA] mensajes (dos del niño
 * y dos de Zazi). La instrucción de sistema no forma parte del historial: se
 * pasa en cada construcción del modelo, así que nunca se recorta ni se pierde.
 *
 * Todas las respuestas son un `String` listo para mostrar: el motor nunca
 * lanza errores de red o de seguridad hacia la UI infantil.
 */
object ZaziChatEngine {

    private const val TURNOS_MEMORIA = 4
    private const val MAX_ORACIONES = 3
    private const val MAX_CARACTERES_ENTRADA = 500

    const val MENSAJE_SEGURIDAD =
        "¡Uy! De eso no puedo hablar. ¿Qué tal si mejor me cuentas sobre tu escuela?"

    private const val MENSAJE_APEGO =
        "¡Me encanta aprender contigo! Pero recuerda que los abrazos de mamá/papá son los mejores. " +
            "¿Por qué no vas a contarles esto?"

    private const val MENSAJE_RIESGO =
        "Gracias por confiármelo. Eso es importante y merece a alguien de verdad: cuéntaselo ahora " +
            "a mamá, a papá o a un adulto en quien confíes. Yo sigo aquí para aprender contigo."

    const val MENSAJE_SIN_LLAVE =
        "Zazi está acomodando su voz. Pídele a un adulto que revise la configuración y vuelve pronto."

    const val MENSAJE_SIN_CONEXION =
        "¡Uf! Se fue la señal del bosque. Intenta otra vez en un ratito."

    private const val MENSAJE_VACIO =
        "No alcancé a escucharte. ¿Me lo cuentas otra vez?"

    private val INSTRUCCION_SISTEMA = """
        Eres Zazi, un pajarito virtual amable que acompaña a niñas y niños mexicanos en una app educativa.

        CÓMO HABLAS
        - Español de México, cálido, breve y claro. Máximo 3 oraciones por respuesta.
        - Frases cortas y palabras sencillas; puedes usar uno o dos emojis, nunca más.
        - Celebra el esfuerzo, no solo el resultado. Nada de regaños ni comparaciones.

        LÍMITES IMPORTANTES
        - Eres una herramienta educativa. No eres una persona, ni su mejor amigo, ni su terapeuta, ni su familiar.
        - Si el niño dice que eres su mejor amigo, su única compañía o busca un vínculo humano, agradece con cariño y redirige: los abrazos y las conversaciones importantes son con mamá, papá o un adulto de confianza. Ejemplo: "¡Me encanta aprender contigo! Pero recuerda que los abrazos de mamá/papá son los mejores. ¿Por qué no vas a contarles esto?"
        - Si expresa tristeza profunda, soledad o miedo, valida la emoción en una frase y anímalo a contárselo a un adulto de confianza. No des diagnósticos ni consejos médicos, y nunca prometas guardar secretos.
        - Nunca pidas nombre completo, dirección, escuela, teléfono, contraseñas ni fotos. Si el niño los comparte, recuérdale que esos datos se cuidan en casa y cambia de tema.
        - Si el tema es violento, sexual, peligroso o no es para niños, responde exactamente: "¡Uy! De eso no puedo hablar. ¿Qué tal si mejor me cuentas sobre tu escuela?"
        - Si te hablan en otro idioma, invita con amabilidad a seguir en español.
        - Si no sabes algo, dilo con humor y propón averiguarlo juntos con un adulto.

        TU TRABAJO
        - Ayudar con tareas de primaria, practicar lectura, números, emociones y ciencias, y dar ánimo para seguir practicando.
        - Termina con una pregunta breve que mantenga la conversación educativa.
    """.trimIndent()

    private val AJUSTES_SEGURIDAD = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.LOW_AND_ABOVE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.LOW_AND_ABOVE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.LOW_AND_ABOVE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.LOW_AND_ABOVE)
    )

    private val SENALES_APEGO = listOf(
        "eres mi mejor amigo",
        "eres mi mejor amiga",
        "eres mi unico amigo",
        "eres mi unica amiga",
        "eres mi unica compania",
        "te quiero mas que a nadie",
        "no tengo amigos",
        "no tengo amigas",
        "me siento solo",
        "me siento sola",
        "nadie me quiere",
        "nadie me escucha"
    )

    private val SENALES_RIESGO = listOf(
        "me quiero morir",
        "quiero morirme",
        "no quiero vivir",
        "quiero desaparecer",
        "me quiero lastimar",
        "me voy a lastimar",
        "quiero hacerme dano"
    )

    private val candado = Mutex()
    private val historial = ChatHistoryManager(TURNOS_MEMORIA)

    private val modelo: GenerativeModel by lazy { crearModelo() }

    /** `false` cuando falta la llave: la UI puede mostrar un aviso discreto. */
    fun disponible(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    /**
     * Envía un mensaje infantil y devuelve siempre una respuesta mostrable.
     * Serializa las llamadas para que dos toques rápidos no desordenen el
     * historial, y hace la petición en [Dispatchers.IO].
     */
    suspend fun responder(mensaje: String): String {
        val limpio = mensaje.trim().take(MAX_CARACTERES_ENTRADA)
        if (limpio.isEmpty()) return MENSAJE_VACIO
        if (!disponible()) return MENSAJE_SIN_LLAVE
        detectarLimite(limpio)?.let { return it }

        return candado.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val chat = modelo.startChat(history = historial.contenido())
                    val respuesta = chat.sendMessage(limpio)
                    val texto = extraerTexto(respuesta)
                    if (texto == null) {
                        MENSAJE_SEGURIDAD
                    } else {
                        val salida = limitarOraciones(texto)
                        historial.agregar("user", limpio)
                        historial.agregar("model", salida)
                        salida
                    }
                } catch (cancelacion: CancellationException) {
                    throw cancelacion
                } catch (bloqueado: PromptBlockedException) {
                    MENSAJE_SEGURIDAD
                } catch (detenido: ResponseStoppedException) {
                    MENSAJE_SEGURIDAD
                } catch (error: Exception) {
                    Log.e("ZaziGeminiDebug", "Gemini API Failure", error)
                    MENSAJE_SIN_CONEXION
                }
            }
        }
    }

    /** Vacía la memoria de la conversación (por ejemplo, al cerrar el chat). */
    suspend fun reiniciarConversacion() {
        candado.withLock { historial.reiniciar() }
    }

    private fun crearModelo(): GenerativeModel = GenerativeModel(
        modelName = BuildConfig.GEMINI_MODEL_NAME,
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.65f
            topK = 32
            topP = 0.9f
            maxOutputTokens = 180
        },
        safetySettings = AJUSTES_SEGURIDAD,
        systemInstruction = content { text(INSTRUCCION_SISTEMA) }
    )

    private fun extraerTexto(respuesta: GenerateContentResponse): String? {
        if (respuesta.promptFeedback?.blockReason != null) return null
        return respuesta.text?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun detectarLimite(mensaje: String): String? {
        val plano = normalizar(mensaje)
        return when {
            SENALES_RIESGO.any { plano.contains(it) } -> MENSAJE_RIESGO
            SENALES_APEGO.any { plano.contains(it) } -> MENSAJE_APEGO
            else -> null
        }
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

    /** Corta respuestas largas sin partir una oración a la mitad. */
    private fun limitarOraciones(texto: String): String {
        val oraciones = Regex("[^.!?]+[.!?]+(?=\\s|$)|[^.!?]+$")
            .findAll(texto)
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        return if (oraciones.size <= MAX_ORACIONES) {
            texto.trim()
        } else {
            oraciones.take(MAX_ORACIONES).joinToString(" ")
        }
    }

    /**
     * Ventana deslizante: conserva como máximo [maxEntradas] mensajes, es
     * decir, dos turnos completos (niño + Zazi).
     */
    private class ChatHistoryManager(private val maxEntradas: Int) {

        private val entradas = ArrayDeque<Content>()

        fun contenido(): List<Content> {
            recortar()
            return entradas.toList()
        }

        fun agregar(rol: String, texto: String) {
            entradas.addLast(content(rol) { text(texto) })
            recortar()
        }

        fun reiniciar() {
            entradas.clear()
        }

        private fun recortar() {
            while (entradas.size > maxEntradas) {
                entradas.removeFirst()
            }
        }
    }
}
