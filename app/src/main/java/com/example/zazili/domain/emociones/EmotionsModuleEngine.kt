package com.example.zazili.domain.emociones

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Una respuesta posible dentro de un reto de "¿Tú qué harías?".
 *
 * [emoji] es el ancla visual: los niños neurodivergentes decodifican la
 * intención de la opción antes de leerla. [explanation] nunca regaña; nombra
 * el efecto de la elección y ofrece el camino amable.
 */
@Immutable
data class EmotionOption(
    val text: String,
    val emoji: String,
    val isCorrect: Boolean,
    val explanation: String
)

/** Una situación social concreta y las respuestas que el niño puede elegir. */
@Immutable
data class EmotionScenario(
    val situation: String,
    val options: List<EmotionOption>
) {
    val indiceCorrecto: Int
        get() = options.indexOfFirst { it.isCorrect }

    val opcionCorrecta: EmotionOption?
        get() = options.getOrNull(indiceCorrecto)
}

/** Lo que el motor devuelve al tocar una opción. */
@Immutable
data class ResultadoEmocion(
    val correcta: Boolean,
    val explicacion: String,
    val opcion: EmotionOption
)

/**
 * Estado visible del módulo. [opcionElegida] se queda puesta hasta que el
 * niño reintenta o avanza, para que la tarjeta conserve su color y la
 * explicación no desaparezca sola.
 */
@Immutable
data class EstadoEmociones(
    val indiceEscenario: Int = 0,
    val opcionElegida: Int? = null,
    val aciertos: Int = 0,
    val intentos: Int = 0,
    val terminado: Boolean = false
)

/**
 * Motor de los 10 retos "¿Tú qué harías?".
 *
 * Es puro a propósito: no conoce Compose ni persistencia. La pantalla
 * observa [estado] y el motor solo cambia de escenario cuando el niño acertó
 * y pidió continuar. Un error no lo deja atrapado: [descartarSeleccion]
 * devuelve la tarjeta a su estado neutral para intentarlo otra vez.
 */
class EmotionsModuleEngine(
    val escenarios: List<EmotionScenario> = bancoDeEscenarios()
) {

    init {
        require(escenarios.isNotEmpty()) { "El módulo de emociones necesita escenarios." }
        require(escenarios.all { escenario -> escenario.options.count { it.isCorrect } == 1 }) {
            "Cada escenario debe tener exactamente una respuesta que ayuda."
        }
    }

    private val _estado = MutableStateFlow(EstadoEmociones())
    val estado: StateFlow<EstadoEmociones> = _estado.asStateFlow()

    val total: Int get() = escenarios.size

    fun escenarioActual(): EmotionScenario =
        escenarios[_estado.value.indiceEscenario.coerceIn(0, escenarios.lastIndex)]

    @Synchronized
    fun seleccionar(indice: Int): ResultadoEmocion? {
        val actual = _estado.value
        if (actual.terminado || actual.opcionElegida != null) return null
        val opcion = escenarioActual().options.getOrNull(indice) ?: return null
        _estado.update { previo ->
            if (previo.terminado || previo.opcionElegida != null) {
                previo
            } else {
                previo.copy(
                    opcionElegida = indice,
                    intentos = previo.intentos + 1,
                    aciertos = if (opcion.isCorrect) previo.aciertos + 1 else previo.aciertos
                )
            }
        }
        return ResultadoEmocion(
            correcta = opcion.isCorrect,
            explicacion = opcion.explanation,
            opcion = opcion
        )
    }

    /** Devuelve la tarjeta al estado neutral para volver a intentarlo. */
    @Synchronized
    fun descartarSeleccion() {
        _estado.update { actual ->
            if (actual.terminado) actual else actual.copy(opcionElegida = null)
        }
    }

    /** Avanza solo después de una respuesta correcta. */
    @Synchronized
    fun siguiente() {
        _estado.update { actual ->
            if (actual.terminado) return@update actual
            val siguiente = actual.indiceEscenario + 1
            if (siguiente >= escenarios.size) {
                actual.copy(terminado = true, opcionElegida = null)
            } else {
                actual.copy(indiceEscenario = siguiente, opcionElegida = null)
            }
        }
    }

    @Synchronized
    fun reiniciar() {
        _estado.value = EstadoEmociones()
    }
}

/**
 * Los 10 retos del módulo.
 *
 * Regla de claridad: cada situación es UNA oración literal, sin metáforas.
 * Regla de visuales: cada opción mide entre 2 y 6 palabras y trae emoji.
 * Regla de feedback: toda opción —correcta o no— explica su efecto.
 */
fun bancoDeEscenarios(): List<EmotionScenario> = listOf(
    EmotionScenario(
        situation = "Un compañero tira su helado por accidente.",
        options = listOf(
            EmotionOption(
                text = "Reírme de él",
                emoji = "😂",
                isCorrect = false,
                explanation = "Si te ríes, se sentirá más triste. Mejor ayúdalo."
            ),
            EmotionOption(
                text = "Compartir el mío",
                emoji = "🤝",
                isCorrect = true,
                explanation = "Compartir demuestra empatía y lo hace sentir acompañado."
            ),
            EmotionOption(
                text = "Irme sin decir nada",
                emoji = "🚶",
                isCorrect = false,
                explanation = "Si te vas, se quedará solo con su tristeza."
            ),
            EmotionOption(
                text = "Grabarlo y subirlo",
                emoji = "📱",
                isCorrect = false,
                explanation = "Grabar a alguien vulnerable puede lastimarlo. Su momento no es un chiste."
            ),
            EmotionOption(
                text = "Decirle que fue su culpa",
                emoji = "😠",
                isCorrect = false,
                explanation = "Fue un accidente. Culparlo no lo ayuda a sentirse mejor."
            )
        )
    ),
    EmotionScenario(
        situation = "Tu amiga llora porque perdió su juego favorito.",
        options = listOf(
            EmotionOption(
                text = "Preguntarle qué necesita",
                emoji = "🤗",
                isCorrect = true,
                explanation = "Preguntar sin suponer le da permiso de pedir lo que necesita."
            ),
            EmotionOption(
                text = "Decirle que no es para tanto",
                emoji = "😐",
                isCorrect = false,
                explanation = "Minimizar su tristeza la hará sentir incomprendida."
            ),
            EmotionOption(
                text = "Salir corriendo",
                emoji = "🏃",
                isCorrect = false,
                explanation = "Irte puede sentirse como abandono. Quédate cerca."
            ),
            EmotionOption(
                text = "Decirle que llora como bebé",
                emoji = "😂",
                isCorrect = false,
                explanation = "Burlarte de su llanto le dará vergüenza de sentir."
            ),
            EmotionOption(
                text = "Darle su juego de vuelta",
                emoji = "🎁",
                isCorrect = false,
                explanation = "El juego no regresa solo. Primero acompaña su enojo."
            )
        )
    ),
    EmotionScenario(
        situation = "Un niño nuevo come solo en la escuela.",
        options = listOf(
            EmotionOption(
                text = "Ir a sentarme con él",
                emoji = "🙋",
                isCorrect = true,
                explanation = "Tu compañía le dice que es bienvenido."
            ),
            EmotionOption(
                text = "Mirarlo desde lejos",
                emoji = "👀",
                isCorrect = false,
                explanation = "Mirar sin acercarte puede hacerlo sentir observado."
            ),
            EmotionOption(
                text = "Decirle que se vaya",
                emoji = "😐",
                isCorrect = false,
                explanation = "Pedirle que se vaya lo aísla más."
            ),
            EmotionOption(
                text = "Seguir con lo mío",
                emoji = "📚",
                isCorrect = false,
                explanation = "Ignorarlo pierde una oportunidad de hacerlo sentir incluido."
            ),
            EmotionOption(
                text = "Reírme de su lunch",
                emoji = "😆",
                isCorrect = false,
                explanation = "Burlarte de su comida puede lastimarlo."
            )
        )
    ),
    EmotionScenario(
        situation = "Tu hermano rompió tu dibujo sin querer.",
        options = listOf(
            EmotionOption(
                text = "Respirar antes de hablar",
                emoji = "🧘",
                isCorrect = true,
                explanation = "Respirar te ayuda a responder sin explotar."
            ),
            EmotionOption(
                text = "Gritarle y empujarlo",
                emoji = "😡",
                isCorrect = false,
                explanation = "Gritar y empujar puede lastimarlo y empeorar todo."
            ),
            EmotionOption(
                text = "Esconderme y no decir nada",
                emoji = "😢",
                isCorrect = false,
                explanation = "Guardarte el enojo no lo hace desaparecer."
            ),
            EmotionOption(
                text = "Romper algo suyo",
                emoji = "🗑️",
                isCorrect = false,
                explanation = "Vengarte suma más enojo y más tristeza."
            ),
            EmotionOption(
                text = "Decirle que es tonto",
                emoji = "🗣️",
                isCorrect = false,
                explanation = "Los insultos no arreglan el dibujo ni el enojo."
            )
        )
    ),
    EmotionScenario(
        situation = "Una compañera se equivocó al leer en voz alta.",
        options = listOf(
            EmotionOption(
                text = "Aplaudirle por intentarlo",
                emoji = "👏",
                isCorrect = true,
                explanation = "Reconocer su esfuerzo la anima a seguir practicando."
            ),
            EmotionOption(
                text = "Reírme muy fuerte",
                emoji = "😂",
                isCorrect = false,
                explanation = "La risa puede darle vergüenza y ganas de callarse."
            ),
            EmotionOption(
                text = "Decirle que lee feo",
                emoji = "🙄",
                isCorrect = false,
                explanation = "Una burla puede hacerla sentir incapaz."
            ),
            EmotionOption(
                text = "Contarle a todos",
                emoji = "📢",
                isCorrect = false,
                explanation = "Contarlo sin permiso la expone frente al grupo."
            ),
            EmotionOption(
                text = "Corregirla en voz alta",
                emoji = "😐",
                isCorrect = false,
                explanation = "Corregir frente a todos la puede avergonzar. Mejor en privado."
            )
        )
    ),
    EmotionScenario(
        situation = "Tu amigo está enojado y aprieta los puños.",
        options = listOf(
            EmotionOption(
                text = "Darle espacio y esperar",
                emoji = "🫧",
                isCorrect = true,
                explanation = "El espacio le permite calmarse sin sentirse presionado."
            ),
            EmotionOption(
                text = "Gritarle que se calme",
                emoji = "😠",
                isCorrect = false,
                explanation = "Gritar más enojo puede encenderlo más."
            ),
            EmotionOption(
                text = "Empujarlo para ver qué pasa",
                emoji = "🤜",
                isCorrect = false,
                explanation = "Empujar puede lastimarlo y romper su confianza."
            ),
            EmotionOption(
                text = "Ignorarlo por completo",
                emoji = "🙈",
                isCorrect = false,
                explanation = "Ignorarlo puede hacerlo sentir que a nadie le importa."
            ),
            EmotionOption(
                text = "Ofrecerle un juguete",
                emoji = "🎁",
                isCorrect = false,
                explanation = "Un regalo no reemplaza la escucha en ese momento."
            )
        )
    ),
    EmotionScenario(
        situation = "Zazi se asusta con el ruido de la licuadora.",
        options = listOf(
            EmotionOption(
                text = "Hablarle suave y acercarme",
                emoji = "🤗",
                isCorrect = true,
                explanation = "Tu voz calmada le dice que está a salvo."
            ),
            EmotionOption(
                text = "Reírme de su miedo",
                emoji = "😆",
                isCorrect = false,
                explanation = "Burlarte del miedo lo hace sentir inseguro."
            ),
            EmotionOption(
                text = "Hacer más ruido",
                emoji = "🔊",
                isCorrect = false,
                explanation = "Más ruido aumenta su susto y su estrés."
            ),
            EmotionOption(
                text = "Salir sin mirarlo",
                emoji = "🏃",
                isCorrect = false,
                explanation = "Irte lo deja solo justo cuando tiene miedo."
            ),
            EmotionOption(
                text = "Ofrecerle su juguete",
                emoji = "🧸",
                isCorrect = false,
                explanation = "El juguete puede ayudar después, pero primero necesita calma."
            )
        )
    ),
    EmotionScenario(
        situation = "Un compañero dice que nadie quiere jugar con él.",
        options = listOf(
            EmotionOption(
                text = "Invitarlo a jugar",
                emoji = "🤝",
                isCorrect = true,
                explanation = "Una invitación le demuestra que sí hay lugar para él."
            ),
            EmotionOption(
                text = "Decirle que es broma",
                emoji = "🙅",
                isCorrect = false,
                explanation = "Decirle que exagera minimiza lo que siente."
            ),
            EmotionOption(
                text = "Inventarle un apodo",
                emoji = "😂",
                isCorrect = false,
                explanation = "Un apodo puede lastimarlo más de lo que crees."
            ),
            EmotionOption(
                text = "Subirlo a redes",
                emoji = "📱",
                isCorrect = false,
                explanation = "Subir su tristeza sin permiso lo expone."
            ),
            EmotionOption(
                text = "Alejarme rápido",
                emoji = "🚶",
                isCorrect = false,
                explanation = "Alejarte confirma su idea de estar solo."
            )
        )
    ),
    EmotionScenario(
        situation = "Tu mamá te dice que está cansada.",
        options = listOf(
            EmotionOption(
                text = "Preguntarle si puedo ayudar",
                emoji = "🫂",
                isCorrect = true,
                explanation = "Ofrecer ayuda demuestra que te importa su cansancio."
            ),
            EmotionOption(
                text = "Seguir jugando fuerte",
                emoji = "🎮",
                isCorrect = false,
                explanation = "El ruido y el desorden aumentan su cansancio."
            ),
            EmotionOption(
                text = "Decirle que no es para tanto",
                emoji = "😤",
                isCorrect = false,
                explanation = "Minimizar su cansancio hace que se sienta sola."
            ),
            EmotionOption(
                text = "Pedirle más cosas",
                emoji = "🗣️",
                isCorrect = false,
                explanation = "Pedirle más la carga justo cuando necesita descansar."
            ),
            EmotionOption(
                text = "Decirle que se aguante",
                emoji = "😴",
                isCorrect = false,
                explanation = "Pedirle aguantar ignora lo que siente."
            )
        )
    ),
    EmotionScenario(
        situation = "Un amigo te cuenta que se siente triste hoy.",
        options = listOf(
            EmotionOption(
                text = "Escucharlo sin interrumpir",
                emoji = "👂",
                isCorrect = true,
                explanation = "Escuchar sin interrumpir lo hace sentir comprendido."
            ),
            EmotionOption(
                text = "Decirle que no es cierto",
                emoji = "🙈",
                isCorrect = false,
                explanation = "Negar su tristeza lo hace sentir solo con lo que siente."
            ),
            EmotionOption(
                text = "Ofrecerle un dulce",
                emoji = "🍭",
                isCorrect = false,
                explanation = "Un dulce no reemplaza la conversación que necesita."
            ),
            EmotionOption(
                text = "Cambiar de tema rápido",
                emoji = "🎬",
                isCorrect = false,
                explanation = "Cambiar de tema le dice que su tristeza incomoda."
            ),
            EmotionOption(
                text = "Decirle que es un dramático",
                emoji = "😜",
                isCorrect = false,
                explanation = "Llamarlo dramático lo avergüenza por sentir."
            )
        )
    )
)
