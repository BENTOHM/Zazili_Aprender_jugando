package com.example.zazili.domain.emociones

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Las tres paradas del acompañamiento emocional. */
enum class FaseOva { OBSERVAR, VALIDAR, ACTUAR }

/** Cómo responde una opción ante la emoción de otra persona. */
enum class TipoRespuestaOva { VALIDACION, POSITIVIDAD_TOXICA, IMPULSIVA, INVALIDACION }

/** Mecánica con la que se juega un reto emocional. */
enum class MecanicaOva { OPCIONES, MEDIDOR_TORMENTAS }

/**
 * Emoción que trae la nube del alebrije. El color rojo es enojo y el azul es
 * tristeza; las demás emociones reciben un tono propio para no confundirlas.
 */
enum class EmocionTormenta(
    val etiqueta: String,
    val nube: String,
    val necesita: String
) {
    ENOJO("enojo", "una nube roja que truena", "espacio y calma"),
    TRISTEZA("tristeza", "una nube azul que llueve", "escucha y compañía"),
    MIEDO("miedo", "una nube morada que se encoge", "un abrazo consentido"),
    ANSIEDAD("ansiedad", "una nube amarilla que gira", "respiración lenta"),
    VERGUENZA("vergüenza", "una nube rosa que se esconde", "seguridad y abrazo"),
    ALEGRIA("alegría", "una nube verde que brilla", "escucha para compartir"),
    CALMA("calma", "una nube blanca que flota", "respirar juntos")
}

/** Kit de primeros auxilios psicológicos que el niño puede arrastrar. */
enum class HerramientaAuxilio(
    val etiqueta: String,
    val emoji: String,
    val descripcion: String,
    val frase: String
) {
    OIDO(
        etiqueta = "Oído que escucha",
        emoji = "👂",
        descripcion = "Escuchar sin interrumpir",
        frase = "Me quedo cerca y te escucho sin prisa."
    ),
    BURBUJA(
        etiqueta = "Burbuja de espacio",
        emoji = "🫧",
        descripcion = "Dar un espacio tranquilo",
        frase = "Te doy tu espacio y sigo cerca."
    ),
    GLOBO(
        etiqueta = "Globo de respiración",
        emoji = "🎈",
        descripcion = "Respirar juntos 4-2-4",
        frase = "Inflo el globo contigo: inhala 4, sostén 2, exhala 4."
    ),
    ABRAZO(
        etiqueta = "Abrazo de oso",
        emoji = "🤗",
        descripcion = "Un abrazo consentido",
        frase = "Si tú quieres, te abrazo fuerte y con calma."
    )
}

@Immutable
data class OvaOpcion(
    val texto: String,
    val tipo: TipoRespuestaOva
)

/**
 * Un reto socioemocional. No hay datos que memorizar: hay respuestas que
 * acompañan y respuestas que lastiman. [emocion] pinta la nube del Medidor de
 * Tormentas y [accionModelo] cierra el ciclo O.V.A. con un paso concreto.
 */
@Immutable
data class OvaExercise(
    val id: String = "",
    val escenario: String,
    val observacion: String,
    val opciones: List<OvaOpcion>,
    val accionModelo: String,
    val pista: String,
    val emocion: EmocionTormenta = EmocionTormenta.CALMA,
    val mecanica: MecanicaOva = MecanicaOva.OPCIONES,
    val concepto: String = "validación"
) {
    val indiceCorrecto: Int
        get() = opciones.indexOfFirst { it.tipo == TipoRespuestaOva.VALIDACION }

    val validacion: String
        get() = opciones.getOrNull(indiceCorrecto)?.texto.orEmpty()

    val herramienta: HerramientaAuxilio
        get() = OVAEngine.herramientaRecomendada(emocion)
}

/** Lo que el motor aprendió de una respuesta, sin castigar ni humillar. */
@Immutable
data class ResultadoOva(
    val correcto: Boolean,
    val tipo: TipoRespuestaOva,
    val consecuencia: ConsecuenciaOva? = null
)

/**
 * Mensaje suave que explica por qué una respuesta no acompañó. Jamás dice
 * "mal", "error" ni "castigo": nombra el efecto y ofrece la alternativa.
 */
@Immutable
data class ConsecuenciaOva(
    val titulo: String,
    val mensaje: String,
    val validacionModelo: String,
    val accionModelo: String,
    val tipo: TipoRespuestaOva,
    val corazonPerdido: Boolean = false
)

@Immutable
data class EstadoOva(
    val fase: FaseOva = FaseOva.OBSERVAR,
    val ejercicio: OvaExercise? = null,
    val intentos: Int = 0,
    val validado: Boolean = false,
    val herramientaElegida: HerramientaAuxilio? = null
)

/**
 * Motor O.V.A. (Observar, Validar, Actuar).
 *
 * El estado avanza solo cuando el niño observa y elige una respuesta. Una
 * respuesta tóxica o impulsiva NO resta corazón de inmediato: devuelve una
 * [ConsecuenciaOva] para conversar y el reto vuelve a la cola.
 */
class OVAEngine {

    private val _estado = MutableStateFlow(EstadoOva())
    val estado: StateFlow<EstadoOva> = _estado.asStateFlow()

    fun iniciar(ejercicio: OvaExercise?) {
        _estado.value = EstadoOva(ejercicio = ejercicio)
    }

    /** El niño ya miró el cuerpo, la cara y el contexto del personaje. */
    @Synchronized
    fun observar() {
        _estado.update {
            if (it.fase == FaseOva.OBSERVAR) it.copy(fase = FaseOva.VALIDAR) else it
        }
    }

    @Synchronized
    fun validar(indice: Int): ResultadoOva {
        val ejercicio = _estado.value.ejercicio
            ?: return ResultadoOva(false, TipoRespuestaOva.INVALIDACION)
        val opcion = ejercicio.opciones.getOrNull(indice)
            ?: return ResultadoOva(false, TipoRespuestaOva.INVALIDACION)
        val correcto = opcion.tipo == TipoRespuestaOva.VALIDACION
        _estado.update { actual ->
            if (correcto) {
                actual.copy(
                    fase = FaseOva.ACTUAR,
                    validado = true,
                    intentos = actual.intentos + 1
                )
            } else {
                actual.copy(intentos = actual.intentos + 1)
            }
        }
        return if (correcto) {
            ResultadoOva(correcto = true, tipo = TipoRespuestaOva.VALIDACION)
        } else {
            ResultadoOva(
                correcto = false,
                tipo = opcion.tipo,
                consecuencia = consecuencia(ejercicio, opcion.tipo)
            )
        }
    }

    /** El niño elige una herramienta del kit de primeros auxilios. */
    @Synchronized
    fun actuar(herramienta: HerramientaAuxilio): ResultadoOva {
        val ejercicio = _estado.value.ejercicio
            ?: return ResultadoOva(false, TipoRespuestaOva.INVALIDACION)
        val correcta = esHerramientaCorrecta(ejercicio, herramienta)
        _estado.update { actual ->
            if (correcta) {
                actual.copy(
                    fase = FaseOva.ACTUAR,
                    validado = true,
                    intentos = actual.intentos + 1,
                    herramientaElegida = herramienta
                )
            } else {
                actual.copy(intentos = actual.intentos + 1)
            }
        }
        return if (correcta) {
            ResultadoOva(correcto = true, tipo = TipoRespuestaOva.VALIDACION)
        } else {
            ResultadoOva(
                correcto = false,
                tipo = TipoRespuestaOva.IMPULSIVA,
                consecuencia = consecuenciaHerramienta(ejercicio, herramienta)
            )
        }
    }

    @Synchronized
    fun reiniciar(ejercicio: OvaExercise?) {
        iniciar(ejercicio)
    }

    companion object {

        fun clasificar(ejercicio: OvaExercise, indice: Int): TipoRespuestaOva =
            ejercicio.opciones.getOrNull(indice)?.tipo ?: TipoRespuestaOva.INVALIDACION

        fun herramientaRecomendada(emocion: EmocionTormenta): HerramientaAuxilio =
            when (emocion) {
                EmocionTormenta.ENOJO -> HerramientaAuxilio.BURBUJA
                EmocionTormenta.TRISTEZA -> HerramientaAuxilio.OIDO
                EmocionTormenta.MIEDO -> HerramientaAuxilio.ABRAZO
                EmocionTormenta.ANSIEDAD -> HerramientaAuxilio.GLOBO
                EmocionTormenta.VERGUENZA -> HerramientaAuxilio.ABRAZO
                EmocionTormenta.ALEGRIA -> HerramientaAuxilio.OIDO
                EmocionTormenta.CALMA -> HerramientaAuxilio.GLOBO
            }

        fun esHerramientaCorrecta(
            ejercicio: OvaExercise,
            herramienta: HerramientaAuxilio
        ): Boolean = herramienta == herramientaRecomendada(ejercicio.emocion)

        fun consecuencia(
            ejercicio: OvaExercise,
            tipo: TipoRespuestaOva,
            corazonPerdido: Boolean = false
        ): ConsecuenciaOva {
            val titulo = when (tipo) {
                TipoRespuestaOva.POSITIVIDAD_TOXICA -> "Las ganas no siempre abrazan"
                TipoRespuestaOva.IMPULSIVA -> "Respira antes de responder"
                TipoRespuestaOva.INVALIDACION -> "Su emoción también cuenta"
                TipoRespuestaOva.VALIDACION -> "¡Qué chido!"
            }
            val mensaje = when (tipo) {
                TipoRespuestaOva.POSITIVIDAD_TOXICA ->
                    "Decir «échale ganas» puede sonar como si su emoción estuviera mal. " +
                        "No pasa nada: validar también se practica."
                TipoRespuestaOva.IMPULSIVA ->
                    "Responder rapidito puede sentirse como un empujón. " +
                        "Detente, mira a la otra persona y después actúa."
                TipoRespuestaOva.INVALIDACION ->
                    "Decir «no es para tanto» borra lo que la otra persona siente. " +
                        "Su emoción es real aunque no la entendamos."
                TipoRespuestaOva.VALIDACION -> "Nombraste lo que siente y lo acompañaste."
            }
            return ConsecuenciaOva(
                titulo = titulo,
                mensaje = mensaje,
                validacionModelo = ejercicio.validacion,
                accionModelo = ejercicio.accionModelo,
                tipo = tipo,
                corazonPerdido = corazonPerdido
            )
        }

        fun consecuenciaHerramienta(
            ejercicio: OvaExercise,
            herramienta: HerramientaAuxilio
        ): ConsecuenciaOva {
            val recomendada = herramientaRecomendada(ejercicio.emocion)
            return ConsecuenciaOva(
                titulo = "La tormenta pide otra cosa",
                mensaje = "${herramienta.etiqueta} no es lo que ${ejercicio.emocion.nube} " +
                    "necesita ahora. ${ejercicio.emocion.necesita} con ${recomendada.etiqueta.lowercase()}: " +
                    "${recomendada.frase}",
                validacionModelo = ejercicio.validacion,
                accionModelo = ejercicio.accionModelo,
                tipo = TipoRespuestaOva.IMPULSIVA
            )
        }

        fun mensajeHerramienta(emocion: EmocionTormenta): String {
            val herramienta = herramientaRecomendada(emocion)
            return "${herramienta.emoji} ${herramienta.etiqueta}: ${herramienta.frase}"
        }
    }
}
