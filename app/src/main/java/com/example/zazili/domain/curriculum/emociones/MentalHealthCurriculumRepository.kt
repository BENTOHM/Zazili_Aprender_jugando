package com.example.zazili.domain.curriculum.emociones

import androidx.compose.runtime.Immutable
import com.example.zazili.domain.emociones.EmocionTormenta
import com.example.zazili.domain.emociones.MecanicaOva
import com.example.zazili.domain.emociones.OvaExercise
import com.example.zazili.domain.emociones.OvaOpcion
import com.example.zazili.domain.emociones.TipoRespuestaOva

/**
 * Un capítulo de la ruta socioemocional. [jefe] es la metáfora que cierra el
 * tramo y [dichoDelJefe] le habla al niño sin regaños.
 */
@Immutable
data class CapituloEmocional(
    val numero: Int,
    val titulo: String,
    val lema: String,
    val jefe: String,
    val dichoDelJefe: String
)

/**
 * Una lección socioemocional. Sus [preguntas] son escenarios O.V.A. y su
 * [teoria] acompaña antes del primer reto.
 */
@Immutable
data class LeccionEmocional(
    val id: String,
    val numero: Int,
    val capitulo: Int,
    val titulo: String,
    val subtitulo: String,
    val tier: Int,
    val icono: String,
    val conceptos: List<String>,
    val teoria: List<String>,
    val preguntas: List<OvaExercise>,
    val esJefeDeCapitulo: Boolean = false
)

/**
 * Currículo de salud mental de Zazili 2.0: 50 lecciones, 5 capítulos y 150
 * escenarios O.V.A. No evalúa datos: evalúa cómo acompaña el niño una emoción.
 *
 * El nodo 50 cierra la ruta con el aviso "¡Continuará...!".
 */
object MentalHealthCurriculumRepository {

    const val ULTIMO_NODO_ID = "emociones_50"

    val capitulos: List<CapituloEmocional> = listOf(
        CapituloEmocional(
            numero = 1,
            titulo = "Alfabetización Emocional",
            lema = "Ponerle nombre a lo que siento",
            jefe = "La Nube del Estrés",
            dichoDelJefe = "Vengo a nublarte el día… a menos que me mires y me nombres."
        ),
        CapituloEmocional(
            numero = 2,
            titulo = "Control de Impulsos",
            lema = "Primero la pausa, luego la acción",
            jefe = "El Volcán de los Impulsos",
            dichoDelJefe = "Rujo fuerte, pero tú puedes aprender a leer mi lava."
        ),
        CapituloEmocional(
            numero = 3,
            titulo = "Habilidades Sociales",
            lema = "Escuchar también es abrazar",
            jefe = "La Sombra Solitaria",
            dichoDelJefe = "Cuando me hablas, me hago pequeñita. ¿Te atreves?"
        ),
        CapituloEmocional(
            numero = 4,
            titulo = "Autoestima y TDAH",
            lema = "Mi cerebro tiene turbo y también corazón",
            jefe = "El Espejo Torcido",
            dichoDelJefe = "Te muestro tus defectos aumentados. ¿Me crees o me miras con cariño?"
        ),
        CapituloEmocional(
            numero = 5,
            titulo = "Ética y Decisiones",
            lema = "Cada decisión deja una huella",
            jefe = "El Alebrije de las Decisiones",
            dichoDelJefe = "Elige con cuidado: hasta una mariposa puede mover un huracán."
        )
    )

    val lecciones: List<LeccionEmocional> = construirLecciones()

    val totalLecciones: Int
        get() = lecciones.size

    fun leccionPorId(id: String?): LeccionEmocional? {
        if (id.isNullOrBlank()) return null
        return lecciones.firstOrNull { it.id == id }
    }

    fun leccionesDe(capitulo: Int): List<LeccionEmocional> =
        lecciones.filter { it.capitulo == capitulo }

    fun capituloPorNumero(numero: Int): CapituloEmocional? =
        capitulos.firstOrNull { it.numero == numero }

    fun idDe(numero: Int): String = "emociones_" + numero.toString().padStart(2, '0')

    init {
        require(lecciones.size == 50) { "El currículo emocional debe tener 50 lecciones" }
        require(lecciones.map { it.numero } == (1..50).toList()) {
            "Las lecciones emocionales deben estar numeradas del 1 al 50"
        }
        require(lecciones.all { it.preguntas.size >= 2 }) {
            "Cada lección emocional necesita al menos dos escenarios"
        }
        require(
            lecciones.all { leccion ->
                leccion.preguntas.all { reto ->
                    reto.opciones.count { it.tipo == TipoRespuestaOva.VALIDACION } == 1
                }
            }
        ) { "Cada escenario debe tener exactamente una respuesta que valida" }
    }

    private fun construirLecciones(): List<LeccionEmocional> =
        capitulo1() + capitulo2() + capitulo3() + capitulo4() + capitulo5()

    // =======================================================================
    //  CAPÍTULO 1 · ALFABETIZACIÓN EMOCIONAL
    // =======================================================================

    private fun capitulo1(): List<LeccionEmocional> = listOf(
        leccion(
            numero = 1,
            titulo = "El mapa de mi cuerpo",
            subtitulo = "Tu cuerpo avisa antes que las palabras.",
            icono = "🫀",
            conceptos = listOf("cuerpo", "emoción", "señal"),
            teoria = listOf(
                "Las emociones se sienten en el cuerpo: panza apretada, manos temblorosas, pecho caliente.",
                "Nombrar lo que sientes ayuda a que tu cerebro baje la alarma."
            ),
            preguntas = listOf(
                ova(
                    escenario = "A Zazi le tiemblan las manos y siente la panza apretada antes de entrar a clase.",
                    validar = "Veo que tu cuerpo está avisando que algo te preocupa. ¿Quieres que respiremos juntos?",
                    toxica = "¡No pasa nada, échale ganas y ya!",
                    tercera = "Te jalo a entrar ahora mismo, sin hablar.",
                    accion = "Respirar juntos 4-2-4 y entrar cuando se sienta listo.",
                    pista = "El cuerpo avisa antes que las palabras. Mira manos, panza y hombros.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Manos temblorosas, panza apretada y hombros subidos.",
                    mecanica = MecanicaOva.MEDIDOR_TORMENTAS
                ),
                ova(
                    escenario = "El alebrije dice que le duele la cabeza y no quiere jugar.",
                    validar = "¿Te duele la cabeza? Te acompaño a un lugar tranquilo y te escucho.",
                    toxica = "¡Pero si era tu juego favorito, no te enojes!",
                    tercera = "¡Pues yo juego sin ti!",
                    accion = "Ir juntos a un lugar tranquilo y preguntar qué necesita.",
                    pista = "Un dolor de cabeza también puede ser una emoción que pide pausa.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Se toca la cabeza, tiene los hombros duros y evita el juego.",
                    mecanica = MecanicaOva.MEDIDOR_TORMENTAS
                ),
                ova(
                    escenario = "Tus piernas brincan y tu sonrisa no cabe antes de tu cumpleaños.",
                    validar = "¡Tu cuerpo está celebrando! Cuéntame qué te emociona tanto.",
                    toxica = "Cálmate, no es para tanto.",
                    tercera = "¡Deja de brincar de una vez!",
                    accion = "Compartir la alegría con una persona de confianza.",
                    pista = "La alegría también se siente en el cuerpo: piernas, sonrisa y pecho ligero.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Piernas que brincan, sonrisa grande y voz con más energía.",
                    mecanica = MecanicaOva.MEDIDOR_TORMENTAS
                )
            )
        ),
        leccion(
            numero = 2,
            titulo = "Cara de… ¿qué siento?",
            subtitulo = "Leer caras sin adivinar de más.",
            icono = "🙂",
            conceptos = listOf("cara", "expresión", "contexto"),
            teoria = listOf(
                "Las cejas, la boca y los ojos cuentan una historia; el contexto la completa.",
                "Preguntar «¿cómo estás?» es más seguro que suponer."
            ),
            preguntas = listOf(
                ova(
                    escenario = "El búho tiene las cejas juntas y los puños apretados.",
                    validar = "Parece que algo te enojó. ¿Quieres contarme qué pasó?",
                    toxica = "Sonríe, se te ve feo enojado.",
                    tercera = "¡Ya cállate y deja de hacer berrinche!",
                    accion = "Preguntar sin suponer y escuchar la respuesta.",
                    pista = "Cejas juntas y puños cerrados suelen avisar enojo.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Cejas juntas, puños cerrados y respiración corta."
                ),
                ova(
                    escenario = "Tu amigo está con los hombros caídos y mira al piso.",
                    validar = "Te veo calladito. Estoy cerca si quieres hablar.",
                    toxica = "¡Anímate, hay niños con problemas peores!",
                    tercera = "¿Por qué lloras si no pasa nada?",
                    accion = "Ofrecer compañía y esperar sin presionar.",
                    pista = "Hombros caídos y mirada baja pueden acompañar tristeza.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Hombros caídos, mirada al piso y voz bajita.",
                    tipoTercera = TipoRespuestaOva.INVALIDACION
                ),
                ova(
                    escenario = "Una compañera se tapa la boca y mira a todos lados.",
                    validar = "No pasa nada, a todos nos pasa. ¿Quieres que te acompañe?",
                    toxica = "¡Ay, qué pena contigo!",
                    tercera = "Jajaja, se puso roja.",
                    accion = "Ofrecer compañía sin hacer burla.",
                    pista = "Taparse la boca y mirar alrededor puede ser vergüenza.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Se tapa la boca, mira a todos lados y se encoge."
                )
            )
        ),
        leccion(
            numero = 3,
            titulo = "La panza con mariposas",
            subtitulo = "Cuando la preocupación se adelanta.",
            icono = "🦋",
            conceptos = listOf("ansiedad", "mariposas", "respiración"),
            teoria = listOf(
                "La ansiedad es una alarma que se adelanta a lo que todavía no pasa.",
                "Respirar lento le dice al cuerpo: «estoy aquí, puedo con un paso a la vez»."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Antes de exponer, sientes mariposas en la panza.",
                    validar = "Es normal sentir mariposas. Respira conmigo y di tu primera frase.",
                    toxica = "¡Piensa en positivo y ya!",
                    tercera = "Mejor no expones hoy y te escondes.",
                    accion = "Respiración de globo y plan de una frase a la vez.",
                    pista = "Las mariposas son el cuerpo preparándose, no una orden de huir.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mariposas en la panza, manos inquietas y mirada al frente."
                ),
                ova(
                    escenario = "No puede dormir porque mañana hay examen.",
                    validar = "Tu cabeza está ensayando mil escenarios. Vamos a soltar uno por uno.",
                    toxica = "¡Duérmete ya, no es para tanto!",
                    tercera = "Quédate despierto viendo videos hasta tarde.",
                    accion = "Respirar 4-2-4 y escribir la preocupación más grande.",
                    pista = "Nombrar la preocupación le quita fuerza a la rueda mental.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Da vueltas en la cama, suspira y mira el techo."
                ),
                ova(
                    escenario = "Le duele la panza cada domingo en la noche.",
                    validar = "El domingo te trae nervios. ¿Qué te ayudaría a sentirte más listo?",
                    toxica = "Inventas dolencias para no ir.",
                    tercera = "Pues no vayas a la escuela nunca.",
                    accion = "Preparar la mochila el sábado y elegir ropa cómoda.",
                    pista = "El cuerpo a veces habla con la panza cuando hay preocupación.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Se agarra la panza, se encoge y mira el calendario.",
                    tipoTercera = TipoRespuestaOva.INVALIDACION
                )
            )
        ),
        leccion(
            numero = 4,
            titulo = "Termómetro del enojo",
            subtitulo = "Del rojo al verde sin explotar.",
            icono = "🌡️",
            conceptos = listOf("enojo", "temperatura", "pausa"),
            teoria = listOf(
                "El enojo sube como un termómetro: tibio, caliente, rojo.",
                "Bajar la temperatura a tiempo evita decir o hacer algo que lastime."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Sientes calor en la cara y ganas de gritar porque perdiste el juego.",
                    validar = "Tu enojo está en rojo. Vamos a bajar el termómetro respirando.",
                    toxica = "¡Es solo un juego, no te enojes!",
                    tercera = "Aventar el control con fuerza.",
                    accion = "Pausa, un poco de agua y regresar cuando estés en verde.",
                    pista = "Calor en la cara y ganas de gritar son señales de termómetro rojo.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Cara caliente, puños apretados y voz que sube."
                ),
                ova(
                    escenario = "Tu hermanito rompió tu dibujo.",
                    validar = "Estás enojado y tu dibujo importaba. Podemos guardar los pedazos y hablar con él.",
                    toxica = "No llores por un papel.",
                    tercera = "Rómpeme su juguete para que aprenda.",
                    accion = "Nombrar el enojo y pedir una reparación.",
                    pista = "El enojo puede decir «esto me importaba».",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Mira los pedazos, aprieta la mandíbula y respira fuerte."
                ),
                ova(
                    escenario = "La maestra te dijo que esperaras y sientes fuego en el pecho.",
                    validar = "Esperar te cuesta y tu cuerpo lo grita. Respira y cuenta hasta diez.",
                    toxica = "¡Sé paciente, los niños buenos esperan!",
                    tercera = "Hablar encima de ella sin esperar.",
                    accion = "Contar hasta diez y levantar la mano.",
                    pista = "El fuego en el pecho es el enojo pidiendo pausa.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Pecho caliente, pie inquieto y ganas de interrumpir."
                )
            )
        ),
        leccion(
            numero = 5,
            titulo = "El semáforo del cuerpo",
            subtitulo = "Alto, respiro, elijo.",
            icono = "🚦",
            conceptos = listOf("semáforo", "autocontrol", "elección"),
            teoria = listOf(
                "Rojo means alto y respira; amarillo means pausa y piensa; verde means elige y actúa.",
                "Tu cuerpo cambia de color antes de que tú te des cuenta."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Puños cerrados y respiración rápida: tu semáforo está en rojo.",
                    validar = "Tu semáforo está rojo. Primero paramos y respiramos.",
                    toxica = "¡No estás enojado, se te ve bien!",
                    tercera = "Seguir discutiendo hasta ganar.",
                    accion = "Alto, respiro y elijo qué hacer después.",
                    pista = "Rojo no es castigo: es una señal para detenerte.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Puños cerrados, respiración rápida y cuerpo tenso."
                ),
                ova(
                    escenario = "Ojos pesados y bostezos: tu semáforo está en amarillo.",
                    validar = "Estás cansado. ¿Tomamos una pausa corta y seguimos después?",
                    toxica = "¡Eres un flojo, échale ganas!",
                    tercera = "Tomar otra soda para aguantar.",
                    accion = "Pausa corta, agua y volver a intentarlo.",
                    pista = "Amarillo avisa que necesitas pausa antes de seguir.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Ojos pesados, bostezos y movimientos lentos."
                ),
                ova(
                    escenario = "Sonrisa, hombros suaves: tu semáforo está en verde.",
                    validar = "Estás en verde. Es buen momento para intentar algo nuevo.",
                    toxica = "¡Aprovecha y haz todo de una vez!",
                    tercera = "Ignorar el cansancio y seguir sin parar.",
                    accion = "Elegir un reto y celebrar el paso dado.",
                    pista = "Verde es buen momento para elegir con calma.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Sonrisa, hombros suaves y respiración tranquila."
                )
            )
        ),
        leccion(
            numero = 6,
            titulo = "Nombres para la tristeza",
            subtitulo = "Extrañar también es querer.",
            icono = "🌧️",
            conceptos = listOf("tristeza", "extrañar", "compañía"),
            teoria = listOf(
                "La tristeza no se apura ni se esconde: se acompaña.",
                "Ponerle nombre a lo que perdimos ayuda a que duela un poco menos."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Se le rompió su papalote favorito.",
                    validar = "Perder algo que quieres da tristeza. ¿Quieres que lo arreglemos o solo que me quede?",
                    toxica = "¡No llores, hay más papalotes!",
                    tercera = "Comprarle otro sin escuchar lo que siente.",
                    accion = "Acompañar y decidir juntos qué hacer con el papalote.",
                    pista = "Preguntar si quiere solución o compañía respeta su tristeza.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira los pedazos, tiene la boca hacia abajo y suspira.",
                    mecanica = MecanicaOva.MEDIDOR_TORMENTAS
                ),
                ova(
                    escenario = "Extraña a su abuela que vive lejos.",
                    validar = "Extrañar también es una forma de querer. Puedes contarme una memoria bonita.",
                    toxica = "¡Ya supéralo, no es para tanto!",
                    tercera = "No la veías tanto, así que no importa.",
                    accion = "Hacer un dibujo o una carta para ella.",
                    pista = "Las memorias bonitas acompañan la tristeza sin borrarla.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira una foto, se queda callado y abraza su suéter.",
                    tipoTercera = TipoRespuestaOva.INVALIDACION
                ),
                ova(
                    escenario = "Nadie quiso jugar con él en el recreo.",
                    validar = "Sentirse solo duele. ¿Quieres que busquemos juntos a alguien?",
                    toxica = "¡Juega solo, es súper divertido!",
                    tercera = "Obligar a los demás a que jueguen contigo.",
                    accion = "Pedir turno en un juego con una frase amable.",
                    pista = "La soledad también es una emoción que pide compañía.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Se quedó en una banca, mira a los demás y juega con sus manos."
                )
            )
        ),
        leccion(
            numero = 7,
            titulo = "Alegría que se contagia",
            subtitulo = "Celebrar sin opacar a nadie.",
            icono = "☀️",
            conceptos = listOf("alegría", "celebrar", "compartir"),
            teoria = listOf(
                "La alegría crece cuando se comparte y se cuenta con detalle.",
                "Celebrar también incluye cuidar cómo se sienten los demás."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Ganó una carrera y salta de gusto.",
                    validar = "¡Se te nota la alegría! ¿Cómo quieres celebrarlo?",
                    toxica = "No te creas tanto, fue suerte.",
                    tercera = "Presumir frente a los que perdieron.",
                    accion = "Celebrar sin burlarse de quienes perdieron.",
                    pista = "Compartir la alegría no necesita comparar ni presumir.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Salta, sonríe y busca con quién compartir."
                ),
                ova(
                    escenario = "Su amigo está feliz por su dibujo.",
                    validar = "Me encanta que estés feliz. Cuéntame qué te gustó de tu dibujo.",
                    toxica = "No es para tanto, cualquiera dibuja.",
                    tercera = "Hablar de tu dibujo sin escuchar el suyo.",
                    accion = "Preguntar y escuchar antes de compartir lo propio.",
                    pista = "Una pregunta sobre lo que disfrutó alarga su alegría.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Le brillan los ojos, muestra el dibujo y habla rápido."
                ),
                ova(
                    escenario = "Le dio risa algo y no puede parar.",
                    validar = "Tu risa es contagiosa. Vamos a bajar la energía para no molestar a otros.",
                    toxica = "¡Cállate ya, no es tan gracioso!",
                    tercera = "Seguir riéndote en el momento inadecuado.",
                    accion = "Respirar y guardar el chiste para después.",
                    pista = "Bajar la risa no la apaga: la guarda para el momento justo.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Ríe fuerte, se tapa la boca y no puede respirar bien."
                )
            )
        ),
        leccion(
            numero = 8,
            titulo = "El miedo que avisa",
            subtitulo = "Escuchar al miedo sin obedecerle todo.",
            icono = "🌙",
            conceptos = listOf("miedo", "seguridad", "paso pequeño"),
            teoria = listOf(
                "El miedo es un vigilante: avisa de un peligro real o imaginado.",
                "Se puede escuchar y dar un paso pequeño con apoyo."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Un perro grande ladra cerca y te quedas quieto.",
                    validar = "Tu miedo te avisa que no conoces a ese perro. Preguntemos al dueño antes de acercarte.",
                    toxica = "¡Los perros no muerden, no tengas miedo!",
                    tercera = "Correr gritando sin mirar.",
                    accion = "Pedir permiso al dueño y observar desde lejos.",
                    pista = "El miedo pide información; no siempre pide huir.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Se queda quieto, mira al perro y aprieta los labios."
                ),
                ova(
                    escenario = "Hay oscuridad en su cuarto y no quiere entrar.",
                    validar = "El miedo en la oscuridad es normal. Podemos dejar una luz pequeña.",
                    toxica = "¡Ya eres grande, no seas bebé!",
                    tercera = "Burla y cerrar la puerta.",
                    accion = "Dejar una luz y abrazar un objeto de calma.",
                    pista = "Una luz pequeña le da información al vigilante interno.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Se asoma, no pasa del marco y busca a un adulto con la mirada."
                ),
                ova(
                    escenario = "Le da miedo equivocarse en la tarea.",
                    validar = "Equivocarse da miedo y también enseña. ¿Quieres intentarlo conmigo?",
                    toxica = "¡Tú puedes con todo, no falles!",
                    tercera = "Evitar la tarea para siempre.",
                    accion = "Intentar un paso pequeño y revisarlo juntos.",
                    pista = "El miedo a fallar se achica con un paso pequeño.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Aprieta el lápiz, borra varias veces y mira la hoja en blanco."
                )
            )
        ),
        leccion(
            numero = 9,
            titulo = "Celos y envidia, sin regaños",
            subtitulo = "Decir «te extrañé» en vez de explotar.",
            icono = "💚",
            conceptos = listOf("celos", "envidia", "pedir"),
            teoria = listOf(
                "Los celos y la envidia avisan que quieres un lugar o algo que valoras.",
                "Nombrar el deseo y pedir con palabras evita lastimar."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Su amigo está jugando con otro niño.",
                    validar = "Los celos avisan que quieres tu lugar. Puedes decir: «te extrañé».",
                    toxica = "¡No seas celoso, comparte y ya!",
                    tercera = "Romper la amistad sin hablar.",
                    accion = "Decir con palabras lo que extrañas y pedir un turno.",
                    pista = "Los celos se pueden convertir en una frase honesta.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira desde lejos, cruza los brazos y no se acerca."
                ),
                ova(
                    escenario = "Un compañero trae el juguete que tanto quería.",
                    validar = "Desear lo que otro tiene es normal. Puedes pedir turno cuando termine.",
                    toxica = "¡No debes sentir envidia, eso está mal!",
                    tercera = "Quitárselo cuando se descuide.",
                    accion = "Pedir turno y esperar con una actividad mientras.",
                    pista = "La envidia se calma cuando el deseo tiene un plan.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Se acerca al juguete, aprieta los labios y mira al compañero."
                ),
                ova(
                    escenario = "Su hermanita recibe más atención de mamá.",
                    validar = "A veces parece que hay menos abrazos para ti. Te abrazo y hablamos con mamá.",
                    toxica = "¡No seas payaso para llamar la atención!",
                    tercera = "Portarse mal para que lo miren.",
                    accion = "Pedir atención de forma clara: «mamá, ¿me abrazas?».",
                    pista = "Pedir atención con palabras es más efectivo que portarse mal.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Se queda en un rincón, mira el abrazo y se muerde la uña."
                )
            )
        ),
        leccion(
            numero = 10,
            titulo = "La Nube del Estrés",
            subtitulo = "Nombrar, respirar y pedir apoyo.",
            icono = "🌩️",
            conceptos = listOf("estrés", "pausa", "apoyo"),
            teoria = listOf(
                "El estrés es una señal de que algo se siente grande o demasiado junto.",
                "No tienes que poder solo: pausa, respira y pide apoyo."
            ),
            preguntas = listOf(
                ova(
                    escenario = "La nube del estrés se acerca y todo se siente demasiado.",
                    validar = "Hola, nube. Estoy sintiendo mucho. Voy paso a paso y pido ayuda.",
                    toxica = "¡No sientas nada, solo sonríe!",
                    tercera = "Explotar con todos.",
                    accion = "Nombrar lo que siento, respirar y pedir apoyo.",
                    pista = "Nombrar la nube la hace un poco más pequeña.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Todo se siente rápido, el pecho se aprieta y hay ganas de llorar.",
                    mecanica = MecanicaOva.MEDIDOR_TORMENTAS
                ),
                ova(
                    escenario = "La nube trae preocupaciones de mañana.",
                    validar = "Vamos a separar lo que sí puedo hacer hoy de lo que todavía no toca.",
                    toxica = "¡No pienses en eso para que se vaya!",
                    tercera = "Quedarte despierto pensando toda la noche.",
                    accion = "Escribir una acción pequeña para hoy.",
                    pista = "Una acción pequeña le da suelo a la preocupación.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira el reloj, da vueltas y repite «¿y si…?».",
                    mecanica = MecanicaOva.MEDIDOR_TORMENTAS
                ),
                ova(
                    escenario = "El estrés no es un enemigo: es una señal.",
                    validar = "Mi cuerpo me pide pausa y apoyo. Puedo escucharlo sin pelear.",
                    toxica = "¡Debo poder solo, pedir ayuda es de débiles!",
                    tercera = "Ignorar la señal y seguir hasta agotarse.",
                    accion = "Hacer una pausa y avisar a alguien de confianza.",
                    pista = "Una señal se atiende; no se pelea ni se ignora.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Respiración corta, hombros arriba y cansancio acumulado.",
                    mecanica = MecanicaOva.MEDIDOR_TORMENTAS
                )
            ),
            esJefe = true
        )
    )

    // =======================================================================
    //  CAPÍTULO 2 · CONTROL DE IMPULSOS
    // =======================================================================

    private fun capitulo2(): List<LeccionEmocional> = listOf(
        leccion(
            numero = 11,
            titulo = "El botón de pausa",
            subtitulo = "Antes de actuar, respira.",
            icono = "⏸️",
            conceptos = listOf("pausa", "impulso", "turno"),
            teoria = listOf(
                "Un impulso es una orden rapidita del cuerpo; una pausa es tu turno de elegir.",
                "Respirar una vez antes de responder cambia la historia."
            ),
            preguntas = listOf(
                ova(
                    escenario = "El alebrije quiere interrumpir una conversación importante.",
                    validar = "Quiero decir algo importante. Respiro y espero mi turno.",
                    toxica = "¡Habla ya, tu idea es la mejor!",
                    tercera = "Interrumpir a gritos.",
                    accion = "Respirar y esperar el espacio para hablar.",
                    pista = "La pausa no borra lo que quieres decir; lo ordena.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Levanta la mano a medias, respira rápido y mira la conversación."
                ),
                ova(
                    escenario = "En clase, su mano quiere agarrar el celular a escondidas.",
                    validar = "Mi mano quiere el celular. Lo guardo y elijo esperar.",
                    toxica = "¡Solo un ratito no hace daño!",
                    tercera = "Agarrarlo a escondidas.",
                    accion = "Guardar el celular lejos y esperar el momento acordado.",
                    pista = "Alejar el objeto hace más fácil la pausa.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira el celular, se muerde el labio y mueve la pierna."
                ),
                ova(
                    escenario = "Siente fuego en el pecho porque algo le pareció injusto.",
                    validar = "Tengo fuego en el pecho. Pausa y luego hablo con la maestra.",
                    toxica = "¡Los niños no se enojan!",
                    tercera = "Gritar sin escuchar.",
                    accion = "Pausa, respiración y denunciar con calma.",
                    pista = "El fuego es una señal, no una instrucción para gritar.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Pecho caliente, puños apretados y voz que quiere subir."
                )
            )
        ),
        leccion(
            numero = 12,
            titulo = "Detente, piensa, actúa",
            subtitulo = "Tres pasos que cambian todo.",
            icono = "🛑",
            conceptos = listOf("detente", "piensa", "actúa"),
            teoria = listOf(
                "Detente: no respondas todavía. Piensa: ¿qué está pasando? Actúa: elige un paso que cuide.",
                "El orden importa: primero el cuerpo, luego la decisión."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Alguien te empuja en la fila.",
                    validar = "Detente. Miro si fue accidente. Actúo: pido que se fije.",
                    toxica = "¡Seguro lo hizo a propósito!",
                    tercera = "Empujar de vuelta.",
                    accion = "Detente, piensa y pide que se fije.",
                    pista = "Preguntar antes de acusar evita una pelea.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Se tambalea, aprieta los dientes y mira al otro."
                ),
                ova(
                    escenario = "Ves un secreto abierto en el celular de un amigo.",
                    validar = "Detente. No es mío. Actúo: no lo abro.",
                    toxica = "¡Solo mira, no pasa nada!",
                    tercera = "Ver todo y contarlo.",
                    accion = "Cerrar el celular y avisar que estaba abierto.",
                    pista = "La privacidad también se cuida con pausas.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "La pantalla está prendida y la curiosidad empuja la mano."
                ),
                ova(
                    escenario = "Quieres comprar algo con el dinero de tu alcancía.",
                    validar = "Detente. Pienso si lo necesito. Actúo: espero un día.",
                    toxica = "¡Date un gusto, tú lo mereces!",
                    tercera = "Gastar todo de una vez.",
                    accion = "Esperar un día y decidir con calma.",
                    pista = "Esperar un día convierte el impulso en decisión.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Mira el dinero, lo cuenta y lo aprieta."
                )
            )
        ),
        leccion(
            numero = 13,
            titulo = "Mis detonantes",
            subtitulo = "Lo que enciende mi alarma.",
            icono = "⚡",
            conceptos = listOf("detonante", "ruido", "hambre"),
            teoria = listOf(
                "Un detonante es lo que enciende una emoción fuerte: ruido, hambre, cansancio o una injusticia.",
                "Conocerlos te permite prepararte antes de que explote la alarma."
            ),
            preguntas = listOf(
                ova(
                    escenario = "El ruido de las sirenas lo hace cubrirse los oídos.",
                    validar = "El ruido te dispara. Podemos usar audífonos o un lugar tranquilo.",
                    toxica = "¡Aguanta, exagerado!",
                    tercera = "Gritar para tapar el ruido.",
                    accion = "Buscar pausa sonora con audífonos o un lugar quieto.",
                    pista = "El ruido fuerte puede ser un detonante del cuerpo.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Se cubre los oídos, cierra los ojos y respira corto."
                ),
                ova(
                    escenario = "Alguien no respeta su turno en el juego.",
                    validar = "Cuando no respetan tu turno, tu enojo se enciende. Levantamos la mano juntos.",
                    toxica = "¡No importa el turno, es solo un juego!",
                    tercera = "Quitar el turno a empujones.",
                    accion = "Pedir turno con voz firme y buscar a un adulto si sigue.",
                    pista = "Nombrar la injusticia ayuda a no convertirla en golpe.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Aprieta el puño, mira al que se coló y respira fuerte."
                ),
                ova(
                    escenario = "Tiene hambre y sueño, y todo se siente enorme.",
                    validar = "Cuando tienes hambre, todo se siente más grande. Comamos algo.",
                    toxica = "¡Solo es flojera!",
                    tercera = "Explotar con quien esté cerca.",
                    accion = "Comer algo y descansar un momento.",
                    pista = "Hambre y cansancio bajan la paciencia.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Se frota los ojos, se agarra la panza y responde cortante."
                )
            )
        ),
        leccion(
            numero = 14,
            titulo = "La voz que grita por dentro",
            subtitulo = "No todo pensamiento es verdad.",
            icono = "🗯️",
            conceptos = listOf("pensamiento", "verdad", "reformular"),
            teoria = listOf(
                "La voz interior puede exagerar cuando hay cansancio o enojo.",
                "Preguntar «¿esto es un hecho o un pensamiento?» abre una puerta."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Ante una tarea difícil, su voz dice: «no puedo».",
                    validar = "Esa voz es un pensamiento, no una verdad. ¿Y si lo intentas un minuto?",
                    toxica = "¡Tú puedes con todo, no falles!",
                    tercera = "Rendirte y romper la hoja.",
                    accion = "Intentar un minuto y luego decidir.",
                    pista = "Un minuto es más pequeño que «todo».",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Mira la hoja, aprieta el lápiz y suspira."
                ),
                ova(
                    escenario = "Después de una discusión, piensa: «todos me odian».",
                    validar = "Ese pensamiento duele. ¿Hay pruebas o es el enojo hablando?",
                    toxica = "¡Claro que te quieren, no seas dramático!",
                    tercera = "Aislarte para siempre.",
                    accion = "Preguntar a una persona segura cómo se siente con vos.",
                    pista = "Buscar pruebas convierte la exageración en información.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Se queda callado, mira el piso y se abraza las rodillas."
                ),
                ova(
                    escenario = "Quiere decir una frase fea que le salió del enojo.",
                    validar = "Esa frase quiere salir. La cambio por una que no lastime.",
                    toxica = "¡Di lo que sientas, sin filtro!",
                    tercera = "Decirla tal cual.",
                    accion = "Reformular la frase antes de hablar.",
                    pista = "El filtro no borra lo que sientes: elige cómo lo dices.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Abre la boca para hablar, aprieta la mandíbula y mira fijo."
                )
            )
        ),
        leccion(
            numero = 15,
            titulo = "Manos que no golpean",
            subtitulo = "Descargar sin lastimar.",
            icono = "🤲",
            conceptos = listOf("cuerpo", "descarga", "reparar"),
            teoria = listOf(
                "El enojo da energía y esa energía puede salir de forma segura.",
                "Si lastimaste, reparar también es parte de aprender."
            ),
            preguntas = listOf(
                ova(
                    escenario = "El enojo le da ganas de aventar una silla.",
                    validar = "Tu cuerpo quiere descargar. Vamos a apretar un cojín o correr en lugar seguro.",
                    toxica = "¡Los niños buenos no se enojan!",
                    tercera = "Aventar la silla.",
                    accion = "Descargar con un cojín o correr en un lugar seguro.",
                    pista = "La descarga segura cuida a todos, incluido tú.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Mira la silla, aprieta los puños y respira fuerte."
                ),
                ova(
                    escenario = "Quiere patear porque perdió un juego.",
                    validar = "El enojo está en tus piernas. Pisotear fuerte en el piso y respirar.",
                    toxica = "¡Eso es de bebés!",
                    tercera = "Patear a alguien.",
                    accion = "Pisotear el piso y respirar hasta bajar.",
                    pista = "Pisotear el piso descarga sin lastimar.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Levanta el pie, aprieta la quijada y mira al rival."
                ),
                ova(
                    escenario = "Ya golpeó a alguien y ahora siente vergüenza.",
                    validar = "Lastimar a alguien no está bien. Podemos reparar: disculpa y juntos con un adulto.",
                    toxica = "¡No fue tu culpa, no pasa nada!",
                    tercera = "Negarlo todo.",
                    accion = "Disculparse y reparar con ayuda de un adulto.",
                    pista = "Reparar es distinto a castigarte: es aprender.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Mira al piso, esconde las manos y no habla."
                )
            )
        ),
        leccion(
            numero = 16,
            titulo = "Esperar mi turno",
            subtitulo = "El turno también es respeto.",
            icono = "⏳",
            conceptos = listOf("espera", "turno", "respeto"),
            teoria = listOf(
                "Esperar es una habilidad que se entrena con apoyos visuales y conteos.",
                "Respetar el turno cuida el juego y a las personas."
            ),
            preguntas = listOf(
                ova(
                    escenario = "En la fila del recreo, quiere llegar primero.",
                    validar = "Las ganas de llegar primero son grandes. Contamos hasta que toque.",
                    toxica = "¡Tú mereces ir primero!",
                    tercera = "Colarse.",
                    accion = "Contar y esperar el turno.",
                    pista = "Contar le da algo que hacer a la espera.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Se mueve en la fila, mira la puerta y aprieta los labios."
                ),
                ova(
                    escenario = "Sus papás están hablando y él quiere decir algo.",
                    validar = "Esperar se siente largo. Puedo dibujar mientras.",
                    toxica = "¡Interrumpe si es importante!",
                    tercera = "Gritar hasta que lo escuchen.",
                    accion = "Dibujar o anotar y esperar la pausa.",
                    pista = "Una actividad pequeña hace más corta la espera.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Abre la boca, mira a sus papás y se jala la manga."
                ),
                ova(
                    escenario = "En el juego de mesa, pierde su turno.",
                    validar = "Perder el turno da coraje. Respiro y espero mi siguiente jugada.",
                    toxica = "¡No importa el turno!",
                    tercera = "Mover dos veces.",
                    accion = "Respetar el turno y planear la siguiente jugada.",
                    pista = "El enojo por el turno se calma con un plan.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Aprieta la ficha, mira el tablero y respira fuerte."
                )
            )
        ),
        leccion(
            numero = 17,
            titulo = "El enojo que se enfría",
            subtitulo = "Volver a hablar cuando baje la temperatura.",
            icono = "❄️",
            conceptos = listOf("enfriar", "tiempo", "reparar"),
            teoria = listOf(
                "El enojo necesita tiempo para enfriarse; no se apaga con un «ya supéralo».",
                "Volver a hablar en primera persona ayuda a reparar."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Después de una discusión, el enojo sigue caliente.",
                    validar = "El enojo sigue caliente. Voy a caminar un poco antes de hablar.",
                    toxica = "¡Ya se te pasó, no estés triste!",
                    tercera = "Seguir discutiendo hasta ganar.",
                    accion = "Caminar un poco y volver cuando esté más tranquilo.",
                    pista = "El tiempo de enfriamiento no es abandono: es cuidado.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Habla rápido, tiene la cara caliente y los hombros duros."
                ),
                ova(
                    escenario = "Su cara sigue caliente después del conflicto.",
                    validar = "Tu cuerpo va a tardar en enfriarse. Agua fría y respiración.",
                    toxica = "¡Ya supéralo!",
                    tercera = "Gritar más para descargar.",
                    accion = "Agua fría, respiración y esperar.",
                    pista = "El cuerpo se enfría más rápido con agua y respiración.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Se toca la cara, respira corto y no puede quedarse quieto."
                ),
                ova(
                    escenario = "Ya puede volver a hablar con su amigo.",
                    validar = "Estoy más tranquilo. Puedo decir: «me enojé cuando pasó esto».",
                    toxica = "¡Olvídalo y sé feliz!",
                    tercera = "Reprochar todo lo que pasó.",
                    accion = "Hablar en primera persona y escuchar la otra versión.",
                    pista = "«Me enojé cuando…» abre la conversación sin atacar.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Respira más lento, mira a su amigo y baja los hombros."
                )
            )
        ),
        leccion(
            numero = 18,
            titulo = "Cuando quiero interrumpir",
            subtitulo = "Mi voz también puede esperar.",
            icono = "✋",
            conceptos = listOf("escuchar", "interrumpir", "turno"),
            teoria = listOf(
                "Interrumpir no es malo: solo necesita un turno y una señal.",
                "Anotar la idea ayuda a que no se escape mientras esperas."
            ),
            preguntas = listOf(
                ova(
                    escenario = "La maestra está contando una historia y él tiene una idea.",
                    validar = "Tengo algo que decir. Lo anoto y espero.",
                    toxica = "¡Tu idea es la mejor, dilo ya!",
                    tercera = "Interrumpir y contar todo.",
                    accion = "Anotar la idea y esperar el final.",
                    pista = "Anotar guarda la idea sin perder el turno.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Levanta la mano, se mueve y mira a la maestra."
                ),
                ova(
                    escenario = "Su amigo cuenta algo triste y él quiere contar su historia.",
                    validar = "Quiero contar mi historia, pero primero escucho la suya.",
                    toxica = "¡A mí me pasó peor!",
                    tercera = "Hablar encima.",
                    accion = "Escuchar y después compartir lo propio.",
                    pista = "Escuchar primero no borra tu historia.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Abre la boca para hablar, se detiene y mira a su amigo."
                ),
                ova(
                    escenario = "En el juego en línea todos hablan al mismo tiempo.",
                    validar = "Espero mi turno en la conversación.",
                    toxica = "¡Habla ya, tu voz importa más!",
                    tercera = "Gritar al micrófono.",
                    accion = "Esperar el turno y hablar con volumen amable.",
                    pista = "El turno también existe en las pantallas.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Aprieta el micrófono, respira y espera."
                )
            )
        ),
        leccion(
            numero = 19,
            titulo = "Recaídas sin castigo",
            subtitulo = "Aprender también es volver a intentar.",
            icono = "🌱",
            conceptos = listOf("recaída", "aprendizaje", "reparación"),
            teoria = listOf(
                "Una recaída no borra lo aprendido: muestra dónde necesitas más apoyo.",
                "Reparar y ajustar el plan vale más que castigarte."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Volvió a gritar aunque había practicado la pausa.",
                    validar = "Ya gritaste. Podemos reparar y probar otra vez.",
                    toxica = "¡Tú nunca gritas, eres perfecto!",
                    tercera = "Decirte que eres un fracaso.",
                    accion = "Disculparse y ajustar el plan de pausa.",
                    pista = "Una recaída es información, no una etiqueta.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Evita la mirada, se tapa la boca y suspira.",
                    tipoTercera = TipoRespuestaOva.INVALIDACION
                ),
                ova(
                    escenario = "Se le olvidó respirar antes de responder.",
                    validar = "Se te olvidó. La próxima ponemos un recordatorio.",
                    toxica = "¡La respiración lo arregla todo, no pasa nada!",
                    tercera = "Rendirte y no intentarlo más.",
                    accion = "Poner un recordatorio visual para la próxima.",
                    pista = "Un recordatorio apoya la memoria, no la regaña.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Se encoge de hombros, mira la pared y dice «otra vez»."
                ),
                ova(
                    escenario = "Después de una recaída, pidió ayuda.",
                    validar = "Pedir ayuda después también cuenta. ¿Qué te funcionó?",
                    toxica = "¡Lo lograste solito, no necesitaste a nadie!",
                    tercera = "No reconocer el apoyo.",
                    accion = "Anotar qué funcionó y agradecer el apoyo.",
                    pista = "Reconocer el apoyo refuerza el siguiente intento.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Respira más tranquilo, mira a quien lo ayudó y sonríe poquito."
                )
            )
        ),
        leccion(
            numero = 20,
            titulo = "El Volcán de los Impulsos",
            subtitulo = "Leer la lava antes de explotar.",
            icono = "🌋",
            conceptos = listOf("impulso", "distancia", "reparación"),
            teoria = listOf(
                "El volcán avisa con humo: cuerpo caliente, voz alta y ganas de actuar ya.",
                "Alejarse un momento y respirar es una decisión valiente."
            ),
            preguntas = listOf(
                ova(
                    escenario = "El volcán ruge y el cuerpo quiere explotar.",
                    validar = "Siento el volcán. Me alejo, respiro y elijo.",
                    toxica = "¡Los volcanes no existen, no sientas nada!",
                    tercera = "Explotar con todos.",
                    accion = "Alejarse, respirar y elegir una acción segura.",
                    pista = "El humo del volcán es la señal para alejarte.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Cara roja, voz alta y puños cerrados."
                ),
                ova(
                    escenario = "La lava quiere salir en forma de golpe.",
                    validar = "Puedo descargar con palabras, no con golpes.",
                    toxica = "¡Aguanta como héroe!",
                    tercera = "Golpear lo que esté cerca.",
                    accion = "Decir lo que sientes y descargar con un cojín.",
                    pista = "Las palabras también descargan lava.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Respira fuerte, aprieta los puños y mira fijo."
                ),
                ova(
                    escenario = "Después de la erupción, ve el desorden que dejó.",
                    validar = "Limpiar lo que rompí y hablar con quien lastimé.",
                    toxica = "¡Ya pasó, no importa!",
                    tercera = "Evadir y no hablar del tema.",
                    accion = "Limpiar, disculparse y reparar.",
                    pista = "Reparar cierra el ciclo del volcán.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Mira el desorden, se queda quieto y baja la cabeza."
                )
            ),
            esJefe = true
        )
    )

    // =======================================================================
    //  CAPÍTULO 3 · HABILIDADES SOCIALES
    // =======================================================================

    private fun capitulo3(): List<LeccionEmocional> = listOf(
        leccion(
            numero = 21,
            titulo = "Ponerme en tus zapatos",
            subtitulo = "Imaginar el mundo desde otro cuerpo.",
            icono = "👟",
            conceptos = listOf("empatía", "perspectiva", "cuidado"),
            teoria = listOf(
                "La empatía es imaginar cómo se siente otra persona sin decidir por ella.",
                "Preguntar y ofrecer es más empático que suponer."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Un compañero no trae lunch y mira a los demás comer.",
                    validar = "Quizá tiene hambre y pena. Puedo preguntar si quiere compartir.",
                    toxica = "¡Seguro no tiene hambre!",
                    tercera = "Comer enfrente sin decir nada.",
                    accion = "Ofrecer sin humillar y preguntar qué necesita.",
                    pista = "Ofrecer en voz baja cuida la dignidad del otro.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira la comida, se agarra la panza y se queda callado."
                ),
                ova(
                    escenario = "Su amigo perdió el partido y está callado.",
                    validar = "Está triste. No necesita consejos, necesita compañía.",
                    toxica = "¡Es solo un partido, anímate!",
                    tercera = "Burlarte de él.",
                    accion = "Quedarte cerca y preguntar si quiere hablar.",
                    pista = "A veces la compañía abraza más que un consejo.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Tiene la cabeza baja, los hombros caídos y no celebra."
                ),
                ova(
                    escenario = "Llega un alumno nuevo y no habla con nadie.",
                    validar = "Puede sentirse solo o nervioso. Me presento y le muestro dónde está todo.",
                    toxica = "¡Seguro es antipático!",
                    tercera = "Ignorarlo.",
                    accion = "Presentarte y mostrarle un lugar seguro.",
                    pista = "Una bienvenida pequeña puede cambiar el día de alguien.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira a todos lados, juega con su mochila y no se acerca."
                )
            )
        ),
        leccion(
            numero = 22,
            titulo = "Escuchar con todo el cuerpo",
            subtitulo = "Ojos, oídos y paciencia.",
            icono = "👂",
            conceptos = listOf("escucha", "cuerpo", "secreto"),
            teoria = listOf(
                "Escuchar con todo el cuerpo es dejar el celular, mirar y no interrumpir.",
                "Si un secreto hace daño, buscar a un adulto es cuidar."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Tu amiga te cuenta algo importante.",
                    validar = "Dejo el celular, la miro y no interrumpo.",
                    toxica = "¡Mientras escucho, juego un ratito!",
                    tercera = "Interrumpir para contar lo mío.",
                    accion = "Escuchar completa y hacer una pregunta.",
                    pista = "El celular abajo es una forma de abrazo.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Tu amiga habla bajito y te busca con la mirada."
                ),
                ova(
                    escenario = "Tu hermano quiere contarte su día.",
                    validar = "Me volteo y pregunto «¿y luego?».",
                    toxica = "¡Ya sé lo que vas a decir!",
                    tercera = "Cambiar de tema.",
                    accion = "Voltearte y preguntar por los detalles.",
                    pista = "«¿Y luego?» invita a seguir contando.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Tu hermano habla rápido y mueve las manos."
                ),
                ova(
                    escenario = "Alguien te pide guardar un secreto que te preocupa.",
                    validar = "Escucho y pregunto si es un secreto que necesita ayuda de un adulto.",
                    toxica = "¡Promete no decir nada nunca!",
                    tercera = "Contarlo para que todos se enteren.",
                    accion = "Buscar a un adulto de confianza si hay peligro.",
                    pista = "Un secreto que hace daño necesita ayuda segura.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Te habla al oído, mira alrededor y aprieta tus manos."
                )
            )
        ),
        leccion(
            numero = 23,
            titulo = "Preguntas que abrazan",
            subtitulo = "Preguntar antes de suponer.",
            icono = "💬",
            conceptos = listOf("preguntar", "opciones", "escucha"),
            teoria = listOf(
                "Una pregunta abierta invita a hablar; una suposición cierra la puerta.",
                "Ofrecer compañía o espacio respeta lo que la otra persona necesita."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Tu amigo está callado y con la mirada perdida.",
                    validar = "¿Cómo estás hoy? ¿Quieres compañía o espacio?",
                    toxica = "¡Anímate, sonríe y ya!",
                    tercera = "Adivinar y decidir por él.",
                    accion = "Preguntar y respetar su respuesta.",
                    pista = "Ofrecer opciones respeta su forma de estar.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira a la nada, responde cortito y no sonríe."
                ),
                ova(
                    escenario = "Una compañera está enojada y golpea la mesa.",
                    validar = "¿Quieres contarme qué pasó?",
                    toxica = "¡Se te va a pasar, no te enojes!",
                    tercera = "Decirle que está mal estar enojada.",
                    accion = "Preguntar y escuchar sin juzgar.",
                    pista = "Preguntar por lo que pasó abre la conversación.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Golpea la mesa, respira fuerte y evita la mirada."
                ),
                ova(
                    escenario = "Tu amigo está feliz y quiere contarlo.",
                    validar = "¿Qué te hizo sonreír? Cuéntame.",
                    toxica = "¡Ya cállate de felicidad!",
                    tercera = "Opacar su momento con lo tuyo.",
                    accion = "Escuchar y celebrar con él.",
                    pista = "Preguntar por la alegría la hace durar más.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Sonríe, habla rápido y busca tus ojos."
                )
            )
        ),
        leccion(
            numero = 24,
            titulo = "Decir lo que siento sin lastimar",
            subtitulo = "Mensajes en primera persona.",
            icono = "🗨️",
            conceptos = listOf("asertividad", "límite", "yo siento"),
            teoria = listOf(
                "«Me siento… cuando…» dice lo que necesitas sin atacar.",
                "Poner un límite también es cuidar la relación."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Tu amigo tomó tu lápiz sin preguntar.",
                    validar = "Me molesta cuando tomas mis cosas sin preguntar. ¿Me lo devuelves?",
                    toxica = "¡No importa, llévatelo!",
                    tercera = "Gritar.",
                    accion = "Decir lo que sientes y pedir el lápiz de vuelta.",
                    pista = "Empezar con «me molesta» evita el ataque.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Tu amigo tiene tu lápiz y tú aprietas la mano."
                ),
                ova(
                    escenario = "No te invitaron a un juego.",
                    validar = "Me sentí triste cuando no me invitaron. ¿Puedo jugar?",
                    toxica = "¡No quería jugar, para nada!",
                    tercera = "Vengarte y no invitarlos nunca.",
                    accion = "Expresar la tristeza y preguntar si puedes unirte.",
                    pista = "Decir la verdad de lo que sentiste abre la puerta.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Miras desde lejos, te quedas quieto y bajas la mirada."
                ),
                ova(
                    escenario = "Necesitas estar a solas un momento.",
                    validar = "Necesito un momento a solas. Te busco después.",
                    toxica = "¡Estoy bien, no me pasa nada!",
                    tercera = "Empujar para salir.",
                    accion = "Pedir el espacio y regresar cuando estés listo.",
                    pista = "Pedir espacio con un «te busco después» cuida el vínculo.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Hay mucho ruido, te tapas los oídos y respiras corto."
                )
            )
        ),
        leccion(
            numero = 25,
            titulo = "Pedir ayuda también es valentía",
            subtitulo = "Nadie aprende ni se cuida solo.",
            icono = "🆘",
            conceptos = listOf("ayuda", "valentía", "adulto seguro"),
            teoria = listOf(
                "Pedir ayuda es una habilidad, no una falla.",
                "Para un secreto peligroso, un adulto de confianza es la ruta segura."
            ),
            preguntas = listOf(
                ova(
                    escenario = "No entiendes la tarea y todos parecen avanzar.",
                    validar = "Puedo levantar la mano y decir que no entiendo.",
                    toxica = "¡Tú puedes solo, no molestes!",
                    tercera = "Copiar la respuesta.",
                    accion = "Levantar la mano y pedir una explicación.",
                    pista = "Decir «no entiendo» es el primer paso para entender.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Miras la hoja, borras y miras a los demás."
                ),
                ova(
                    escenario = "Te sientes mal y no sabes por qué.",
                    validar = "Puedo decirle a un adulto de confianza que no sé qué me pasa.",
                    toxica = "¡Nadie necesita saber lo que sientes!",
                    tercera = "Aislarte y no hablar con nadie.",
                    accion = "Buscar a un adulto de confianza y describir lo que sientes.",
                    pista = "No necesitas tener la respuesta para pedir ayuda.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Estás callado, con la panza apretada y evitas a todos."
                ),
                ova(
                    escenario = "Un amigo te cuenta que alguien lo está lastimando.",
                    validar = "Busco a un adulto ahora. No es chisme, es cuidado.",
                    toxica = "¡No te metas, seguro exagera!",
                    tercera = "Grabar y compartir el secreto.",
                    accion = "Avisar a un adulto de confianza de inmediato.",
                    pista = "Cuidar a un amigo puede requerir ayuda de un adulto.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Tu amigo habla bajito, tiembla y te pide no decir nada."
                )
            )
        ),
        leccion(
            numero = 26,
            titulo = "El amigo que se equivocó",
            subtitulo = "Límites y reparación sin tirar la amistad.",
            icono = "🤝",
            conceptos = listOf("límite", "perdón", "reparación"),
            teoria = listOf(
                "Un límite dice qué necesitas para seguir cerca.",
                "Perdonar no es olvidar: es elegir qué sigue después."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Tu amigo te lastimó con una broma.",
                    validar = "Me dolió lo que hizo. Quiero hablar para entender y decidir.",
                    toxica = "¡Perdona y olvida ya, no fue para tanto!",
                    tercera = "Dejar de hablarle sin explicar.",
                    accion = "Hablar del dolor y decidir juntos cómo seguir.",
                    pista = "El perdón también puede incluir una conversación.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Te quedas callado, miras a tu amigo y aprietas la boca."
                ),
                ova(
                    escenario = "Tu amigo rompió una regla que habían acordado.",
                    validar = "Necesito poner un límite y escuchar su versión.",
                    toxica = "¡Tú también eres culpable!",
                    tercera = "Castigarlo sin escucharlo.",
                    accion = "Poner el límite y escuchar qué pasó.",
                    pista = "Un límite claro y una escucha abierta pueden convivir.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Se cruza de brazos, respira fuerte y mira la regla escrita."
                ),
                ova(
                    escenario = "Tu amigo se disculpa por lo que pasó.",
                    validar = "Gracias por disculparte. Me ayudaría que la próxima preguntes.",
                    toxica = "¡Con un abrazo se arregla todo!",
                    tercera = "No aceptar nunca la disculpa.",
                    accion = "Aceptar la disculpa y pedir un cambio concreto.",
                    pista = "Una disculpa se completa con un cambio.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Tu amigo baja la cabeza, habla despacio y espera tu respuesta."
                )
            )
        ),
        leccion(
            numero = 27,
            titulo = "Compartir sin perder",
            subtitulo = "Compartir también tiene turnos.",
            icono = "🧺",
            conceptos = listOf("compartir", "turno", "límite"),
            teoria = listOf(
                "Compartir no significa quedarte sin nada: significa elegir un turno.",
                "Decir «ahora no» también es una forma de cuidar lo tuyo."
            ),
            preguntas = listOf(
                ova(
                    escenario = "No quieres prestar tu juguete en este momento.",
                    validar = "Puedo decir: «ahora no, lo estoy usando; al rato te lo presto».",
                    toxica = "¡Si no compartes, eres malo!",
                    tercera = "Esconderlo para que no lo vea.",
                    accion = "Ofrecer un turno claro y cumplirlo.",
                    pista = "Un turno claro cuida tu juego y la amistad.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Aprietas el juguete, miras al otro y dudas."
                ),
                ova(
                    escenario = "Tienes dos magdalenas y tu compañero no trae lunch.",
                    validar = "Puedo compartir una y quedarme otra. Compartir no es quedarme sin nada.",
                    toxica = "¡Dalo todo, tú no importas!",
                    tercera = "Comerte las dos.",
                    accion = "Compartir una y conservar la otra.",
                    pista = "Compartir también incluye cuidarte.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Miras tus magdalenas, miras a tu compañero y sonríes poquito."
                ),
                ova(
                    escenario = "Tu amigo no quiere prestarte su juego ahora.",
                    validar = "No quiere ahora. Puedo respetar y pedir otra cosa.",
                    toxica = "¡No es tu amigo si no comparte!",
                    tercera = "Quitárselo cuando se distraiga.",
                    accion = "Respetar el turno y buscar otra actividad.",
                    pista = "Un no ahora no significa un no para siempre.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Miras el juego, cruzas los brazos y suspiras."
                )
            )
        ),
        leccion(
            numero = 28,
            titulo = "Defender sin pelear",
            subtitulo = "Valentía con palabras y adultos.",
            icono = "🛡️",
            conceptos = listOf("defender", "límite", "adulto"),
            teoria = listOf(
                "Defender es poner un límite claro, no devolver el golpe.",
                "Pedir ayuda a un adulto también es defender."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Alguien molesta a un compañero en el recreo.",
                    validar = "Lo que haces no está bien. Voy por un adulto.",
                    toxica = "¡No es tu problema, no te metas!",
                    tercera = "Golpear al que molesta.",
                    accion = "Poner un límite y buscar a un adulto.",
                    pista = "Defender no requiere pelear.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Tu compañero se encoge y el otro se ríe."
                ),
                ova(
                    escenario = "Te están molestando a ti.",
                    validar = "No me gusta lo que dices. Me voy y busco ayuda.",
                    toxica = "¡Aguanta como un héroe!",
                    tercera = "Insultar de vuelta.",
                    accion = "Poner el límite, alejarte y buscar apoyo.",
                    pista = "Alejarte no es cobardía: es cuidarte.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Aprietas los puños, sientes calor en la cara y quieres responder."
                ),
                ova(
                    escenario = "Tu amigo quiere vengarse de alguien.",
                    validar = "Entiendo tu enojo. Vengarnos lastima más. ¿Qué necesita tu cuerpo ahora?",
                    toxica = "¡Véngate, te lo mereces!",
                    tercera = "Ayudarlo con la venganza.",
                    accion = "Pausa, respiración y buscar a un adulto juntos.",
                    pista = "La venganza enciende otro volcán.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Tu amigo habla rápido, aprieta los puños y mira al otro lado."
                )
            )
        ),
        leccion(
            numero = 29,
            titulo = "El arte de disculparse",
            subtitulo = "Perdón sin excusas y con reparación.",
            icono = "🙏",
            conceptos = listOf("disculpa", "reparación", "cambio"),
            teoria = listOf(
                "Una disculpa completa dice qué pasó, reconoce el daño y ofrece un cambio.",
                "Pedir perdón no te hace menos: te hace más valiente."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Gritaste a tu amigo sin razón.",
                    validar = "Perdón por gritarte. No fue justo. ¿Qué necesitas?",
                    toxica = "¡No pasa nada, no te enojes!",
                    tercera = "No disculparte y esperar a que se le pase.",
                    accion = "Disculparte sin excusas y preguntar qué necesita.",
                    pista = "El perdón que escucha también repara.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Tu amigo está callado y tú miras hacia otro lado."
                ),
                ova(
                    escenario = "Rompiste algo que no era tuyo.",
                    validar = "Rompí tu cosa. Voy a ayudar a repararla.",
                    toxica = "¡No fue tan importante!",
                    tercera = "Culpar a otro.",
                    accion = "Reconocer, ofrecer reparar y cumplir.",
                    pista = "La reparación hace que el perdón tenga manos.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Miras los pedazos, te metes las manos a los bolsillos y suspiras."
                ),
                ova(
                    escenario = "Te disculpaste, pero volvió a pasar.",
                    validar = "Me disculpé y volvió a pasar. Necesito cambiar mi acción.",
                    toxica = "¡Ya dije perdón, con eso basta!",
                    tercera = "Seguir haciendo lo mismo.",
                    accion = "Hacer un plan concreto para cambiar la acción.",
                    pista = "Una disculpa repetida pide un cambio, no más palabras.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Tu amigo te mira con cansancio y tú bajas la cabeza."
                )
            )
        ),
        leccion(
            numero = 30,
            titulo = "La Sombra Solitaria",
            subtitulo = "La conexión también se practica.",
            icono = "🌑",
            conceptos = listOf("soledad", "conexión", "pedir"),
            teoria = listOf(
                "La soledad puede aparecer aunque estés rodeado de gente.",
                "Una frase pequeña o un mensaje pueden abrir la puerta a la conexión."
            ),
            preguntas = listOf(
                ova(
                    escenario = "La sombra susurra: «nadie te quiere».",
                    validar = "La sombra habla fuerte. Busco a alguien seguro y le cuento.",
                    toxica = "¡Solo sonríe y ya, seguro te quieren!",
                    tercera = "Aislarte y no hablar con nadie.",
                    accion = "Buscar a una persona segura y contarle lo que sientes.",
                    pista = "La sombra se achica cuando la compartes.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Te quedas en un rincón, miras a los demás y te abrazas."
                ),
                ova(
                    escenario = "La sombra te invita a no hablar durante el recreo.",
                    validar = "Puedo decir una frase pequeña para empezar.",
                    toxica = "¡Habla mucho y ya, nadie muerde!",
                    tercera = "Quedarte callado por siempre.",
                    accion = "Decir un saludo pequeño y ver qué pasa.",
                    pista = "Una frase pequeña es más fácil que hablar mucho.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Miras a un grupo, abres la boca y la cierras."
                ),
                ova(
                    escenario = "La sombra se encoge después de jugar con alguien.",
                    validar = "La soledad se achica cuando comparto. Gracias por quedarte.",
                    toxica = "¡Ya no sientas nada, ya estás bien!",
                    tercera = "Negar que la soledad existe.",
                    accion = "Agradecer la compañía y mantener la conexión.",
                    pista = "Agradecer refuerza el vínculo que acabas de crear.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Sonríes poquito, respiras más tranquilo y te quedas cerca."
                )
            ),
            esJefe = true
        )
    )

    // =======================================================================
    //  CAPÍTULO 4 · AUTOESTIMA Y TDAH
    // =======================================================================

    private fun capitulo4(): List<LeccionEmocional> = listOf(
        leccion(
            numero = 31,
            titulo = "Mi voz interior",
            subtitulo = "Habla contigo como hablarías con un amigo.",
            icono = "🗯️",
            conceptos = listOf("voz interior", "reformular", "amabilidad"),
            teoria = listOf(
                "La voz interior se aprende; también se puede entrenar para ser más amable.",
                "Cambiar «soy tonto» por «me equivoqué y puedo aprender» abre una puerta."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Después de un error, se dice: «soy tonto».",
                    validar = "Ese pensamiento pesa. ¿Puedo decir: «me equivoqué, puedo aprender»?",
                    toxica = "¡Eres el mejor en todo, no falles!",
                    tercera = "Repetirte tonto.",
                    accion = "Cambiar la frase por una que incluya aprendizaje.",
                    pista = "La voz interior puede ser entrenada como un músculo.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Se queda mirando la hoja, aprieta el lápiz y suspira."
                ),
                ova(
                    escenario = "Ante algo nuevo, su voz dice: «no puedo».",
                    validar = "Todavía no puedo. Vamos por un paso.",
                    toxica = "¡Sí puedes con todo, no hay excusas!",
                    tercera = "Rendirte antes de intentarlo.",
                    accion = "Añadir «todavía» y probar un paso pequeño.",
                    pista = "«Todavía» deja la puerta abierta.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Mira el reto, se encoge de hombros y se aleja."
                ),
                ova(
                    escenario = "Su voz dice: «siempre fallo».",
                    validar = "Fallé esta vez. No es siempre. ¿Qué salió bien?",
                    toxica = "¡Nunca fallas, eres perfecto!",
                    tercera = "Exageras.",
                    accion = "Buscar un hecho que muestre lo que sí salió bien.",
                    pista = "Un hecho pequeño desarma un «siempre».",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Baja la mirada, repite la frase y se abraza.",
                    tipoTercera = TipoRespuestaOva.INVALIDACION
                )
            )
        ),
        leccion(
            numero = 32,
            titulo = "La etiqueta no me define",
            subtitulo = "Distraído, inquieto, intenso… y mucho más.",
            icono = "🏷️",
            conceptos = listOf("etiqueta", "TDAH", "fortaleza"),
            teoria = listOf(
                "Una etiqueta describe una parte, no a la persona completa.",
                "Con las herramientas correctas, la energía y la creatividad también son fortalezas."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Alguien te llama «distraído».",
                    validar = "Tengo un cerebro con turbo. Puedo usar pausas y listas.",
                    toxica = "¡Eres perfecto, no tienes nada!",
                    tercera = "Aceptar que eres tonto.",
                    accion = "Elegir una herramienta de foco y usarla.",
                    pista = "Una etiqueta no borra tus herramientas.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Te quedas mirando el pizarrón y pierdes el hilo."
                ),
                ova(
                    escenario = "Alguien te llama «inquieto».",
                    validar = "Mi cuerpo necesita moverse. Busco una forma segura de moverme.",
                    toxica = "¡Quédate quieto y sonríe!",
                    tercera = "Moverse sin respetar a los demás.",
                    accion = "Tomar una pausa activa y volver.",
                    pista = "Moverte con permiso también es moverte.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Mueves la pierna, te levantas a medias y suspiras."
                ),
                ova(
                    escenario = "Alguien te llama «intenso».",
                    validar = "Siento fuerte y eso también me ayuda a crear. Aprendo a dosificar.",
                    toxica = "¡Nunca sientas tanto!",
                    tercera = "Apagar todo lo que sientes.",
                    accion = "Elegir un momento y una dosis para tu energía.",
                    pista = "Dosificar no es apagar.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Hablas rápido, gesticulas y te emocionas."
                )
            )
        ),
        leccion(
            numero = 33,
            titulo = "Error = pista",
            subtitulo = "Equivocarse muestra el siguiente paso.",
            icono = "🔍",
            conceptos = listOf("error", "aprendizaje", "pista"),
            teoria = listOf(
                "El error no es lo contrario del aprendizaje: es parte de él.",
                "Preguntar «¿qué me dice este error?» convierte la vergüenza en información."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Se equivocó en un problema de matemáticas.",
                    validar = "Este error me dice dónde practicar.",
                    toxica = "¡Los errores no existen, todo está bien!",
                    tercera = "Romper la hoja.",
                    accion = "Revisar el error y practicar esa parte.",
                    pista = "El error señala el lugar exacto para practicar.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Mira la calificación, tacha la hoja y se hunde en la silla."
                ),
                ova(
                    escenario = "Perdió un juego.",
                    validar = "Perdí. Puedo pedir otra ronda y probar una estrategia.",
                    toxica = "¡Tú nunca pierdes!",
                    tercera = "Culpar al juego.",
                    accion = "Pedir otra ronda y cambiar una estrategia.",
                    pista = "Perder es una pista para probar distinto.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Deja el control, cruza los brazos y mira el marcador."
                ),
                ova(
                    escenario = "Se equivocó con un amigo y lo lastimó.",
                    validar = "Me equivoqué con mi amigo. Puedo disculparme y aprender.",
                    toxica = "¡Tú siempre tienes razón!",
                    tercera = "Negar que pasó.",
                    accion = "Disculparte y preguntar qué necesita.",
                    pista = "Reconocer el error abre la reparación.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Tu amigo está callado y tú miras hacia otro lado."
                )
            )
        ),
        leccion(
            numero = 34,
            titulo = "Todavía no",
            subtitulo = "El poder de una palabra pequeña.",
            icono = "🌱",
            conceptos = listOf("todavía", "práctica", "paciencia"),
            teoria = listOf(
                "«Todavía no» convierte un fracaso en un paso pendiente.",
                "La práctica necesita tiempo y un paso a la vez."
            ),
            preguntas = listOf(
                ova(
                    escenario = "No le sale un nudo en el cordón.",
                    validar = "Todavía no me sale. Practico un paso más.",
                    toxica = "¡Deberías saberlo ya!",
                    tercera = "Tirar el cordón.",
                    accion = "Practicar un paso y pedir ayuda si hace falta.",
                    pista = "Un paso más es suficiente por hoy.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Mira el nudo, se frustra y aprieta los puños."
                ),
                ova(
                    escenario = "No entiende una lectura.",
                    validar = "Todavía no lo entiendo. Leo otra vez y pido ayuda.",
                    toxica = "¡Eres lento, deberías entender ya!",
                    tercera = "Cerrar el libro y abandonar.",
                    accion = "Releer una parte y pedir una explicación.",
                    pista = "Releer es una estrategia, no un castigo.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Mira la página, se rasca la cabeza y suspira."
                ),
                ova(
                    escenario = "No hace amigos rápido.",
                    validar = "Todavía estoy conociendo. Una conversación a la vez.",
                    toxica = "¡Todos te aman ya, seguro!",
                    tercera = "Forzar a alguien a ser tu amigo.",
                    accion = "Saludar a una persona y ver qué pasa.",
                    pista = "La amistad también tiene tiempos.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira a un grupo, se queda cerca y no se acerca."
                )
            )
        ),
        leccion(
            numero = 35,
            titulo = "Mi cerebro con turbo",
            subtitulo = "Muchas ideas, una a la vez.",
            icono = "🚀",
            conceptos = listOf("TDAH", "foco", "memoria"),
            teoria = listOf(
                "Un cerebro con turbo puede tener muchas ideas y olvidar pasos.",
                "Anotar, usar alarmas y hacer una cosa a la vez son apoyos, no trampas."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Tiene mil ideas a la vez y no sabe por dónde empezar.",
                    validar = "Mi cerebro trae muchas ideas. Las anoto y elijo una.",
                    toxica = "¡Piensa en todo ya!",
                    tercera = "Hacer todo a medias.",
                    accion = "Anotar las ideas y elegir una para hoy.",
                    pista = "Anotar vacía un poco la cabeza.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira a todos lados, habla rápido y cambia de tarea."
                ),
                ova(
                    escenario = "No puede quedarse quieto en la silla.",
                    validar = "Mi cuerpo pide movimiento. Puedo estirarme y volver.",
                    toxica = "¡Siéntate y no te muevas!",
                    tercera = "Correr por todo el salón.",
                    accion = "Estirarte con permiso y regresar a la tarea.",
                    pista = "Una pausa activa puede ayudar al foco.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Se mece en la silla, mueve los pies y mira la puerta."
                ),
                ova(
                    escenario = "Olvidó la tarea en casa.",
                    validar = "Se me olvidó. Puedo usar una lista o alarma.",
                    toxica = "¡Eres un desastre!",
                    tercera = "Echar la culpa a la maestra.",
                    accion = "Crear una lista o alarma para la próxima.",
                    pista = "Un recordatorio apoya la memoria.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Abre la mochila, busca y se queda quieto."
                )
            )
        ),
        leccion(
            numero = 36,
            titulo = "Compararme sin lastimarme",
            subtitulo = "Mi ritmo también es válido.",
            icono = "⚖️",
            conceptos = listOf("comparación", "ritmo", "valor"),
            teoria = listOf(
                "Compararte puede dar información o lastimarte; depende de qué haces con ella.",
                "Tu valor no se mide por la velocidad de otro."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Un compañero termina la tarea mucho antes.",
                    validar = "Cada cerebro tiene su ritmo. Yo voy a mi paso.",
                    toxica = "¡Deberías ser como él!",
                    tercera = "Copiarle para terminar igual.",
                    accion = "Seguir tu plan y revisar tu propio avance.",
                    pista = "El ritmo de otro no marca tu valor.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira la hoja del otro, mira la tuya y suspira."
                ),
                ova(
                    escenario = "Un amigo tiene más seguidores en redes.",
                    validar = "Los números no miden mi valor. Elijo lo que me gusta.",
                    toxica = "¡No importan los demás, tú eres único!",
                    tercera = "Publicar cosas por presión.",
                    accion = "Cerrar la app y hacer algo que disfrutes.",
                    pista = "Los números son un dato, no una medida de quién eres.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira el contador, lo revisa otra vez y aprieta el celular."
                ),
                ova(
                    escenario = "Te comparas con tu hermano.",
                    validar = "Somos diferentes y ambos valemos. ¿Qué es mío?",
                    toxica = "¡Sé como tu hermano!",
                    tercera = "Rendirte porque él es mejor.",
                    accion = "Nombrar una fortaleza propia.",
                    pista = "Cada quien tiene una fortaleza distinta.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira las fotos familiares y baja la mirada."
                )
            )
        ),
        leccion(
            numero = 37,
            titulo = "Celebrar mis pasos",
            subtitulo = "Lo pequeño también cuenta.",
            icono = "🎉",
            conceptos = listOf("logro", "esfuerzo", "celebración"),
            teoria = listOf(
                "Celebrar el esfuerzo sostiene la motivación mejor que exigir perfección.",
                "Un paso pequeño merece reconocimiento."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Terminó una tarea que le costó mucho.",
                    validar = "¡Lo lograste! Reconoce el esfuerzo.",
                    toxica = "¡Era fácil, cualquiera puede!",
                    tercera = "Ignorar el logro.",
                    accion = "Celebrar el esfuerzo y anotar qué funcionó.",
                    pista = "El esfuerzo cuenta aunque el resultado no sea perfecto.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Sonríe, suelta el lápiz y mira la tarea terminada."
                ),
                ova(
                    escenario = "Se animó a pedir ayuda.",
                    validar = "Pedir ayuda fue valiente. Anótalo.",
                    toxica = "¡No fue para tanto!",
                    tercera = "Minimizar lo que hiciste.",
                    accion = "Reconocer el paso y agradecer el apoyo.",
                    pista = "Pedir ayuda es una acción valiente.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Levantó la mano, habló bajito y ahora respira."
                ),
                ova(
                    escenario = "Mejoró un poquito en algo que practicaba.",
                    validar = "Un poco también cuenta. ¿Qué sigue?",
                    toxica = "¡Aún no eres el mejor, sigue hasta lograrlo!",
                    tercera = "Exigirte más sin descanso.",
                    accion = "Reconocer la mejora y elegir el siguiente paso.",
                    pista = "La mejora pequeña también es progreso.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Compara su práctica anterior, sonríe y sigue."
                )
            )
        ),
        leccion(
            numero = 38,
            titulo = "Pedir pausa sin culpa",
            subtitulo = "Descansar también es parte del plan.",
            icono = "🛋️",
            conceptos = listOf("pausa", "saturación", "autocuidado"),
            teoria = listOf(
                "Una pausa pedida a tiempo evita una explosión o un agotamiento.",
                "Descansar no es abandonar: es volver con más recursos."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Necesita silencio porque hay mucho ruido.",
                    validar = "Necesito una pausa. La pido y vuelvo.",
                    toxica = "¡Aguanta, no seas débil!",
                    tercera = "Explotar con todos.",
                    accion = "Pedir la pausa y regresar cuando estés listo.",
                    pista = "Pedir pausa antes de explotar es autocuidado.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Se tapa los oídos, respira corto y mira la puerta."
                ),
                ova(
                    escenario = "Está saturado por tantos estímulos.",
                    validar = "Mi cuerpo está lleno de estímulos. Busco un lugar tranquilo.",
                    toxica = "¡Esto es fácil, no te agotes!",
                    tercera = "Quedarte y sufrir en silencio.",
                    accion = "Buscar un lugar tranquilo y respirar.",
                    pista = "La saturación no se resuelve con más ruido.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira a todos, se mueve inquieto y respira rápido."
                ),
                ova(
                    escenario = "Necesita una pausa en medio de un juego.",
                    validar = "Puedo pedir un minuto y regresar. La pausa no es abandono.",
                    toxica = "¡Si paras, pierdes!",
                    tercera = "Seguir jugando sin disfrutar.",
                    accion = "Pedir un minuto y volver al juego.",
                    pista = "Una pausa corta puede devolverte el disfrute.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Se frota la cara, mira el juego y bosteza."
                )
            )
        ),
        leccion(
            numero = 39,
            titulo = "Mi fuerza es mi forma",
            subtitulo = "No hay un solo molde de talento.",
            icono = "🎨",
            conceptos = listOf("fortaleza", "talento", "entorno"),
            teoria = listOf(
                "Cada persona aprende y brilla de una forma distinta.",
                "Ajustar el entorno puede cambiar por completo la experiencia."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Le gusta dibujar, pero no el deporte.",
                    validar = "Mi fuerza está en mi creatividad. La practico y la comparto.",
                    toxica = "¡Debes ser bueno en todo!",
                    tercera = "Dejar de dibujar.",
                    accion = "Practicar tu creatividad y compartirla.",
                    pista = "No necesitas ser bueno en todo para valer.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Mira su dibujo, lo colorea y sonríe."
                ),
                ova(
                    escenario = "Aprende mejor con cierto ruido de fondo.",
                    validar = "Aprendo mejor con ciertos sonidos. Uso audífonos o un lugar quieto.",
                    toxica = "¡Todos aprenden igual!",
                    tercera = "Copiar la forma de otros.",
                    accion = "Ajustar tu entorno para aprender mejor.",
                    pista = "Cambiar el entorno no es hacer trampa.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Se distrae con cada sonido y mira la ventana."
                ),
                ova(
                    escenario = "Tiene mucha energía y no sabe dónde usarla.",
                    validar = "Mi energía puede ayudar en equipo. Busco un rol activo.",
                    toxica = "¡Eres demasiado, contrólate!",
                    tercera = "Molestar a otros con tu energía.",
                    accion = "Elegir un rol activo que ayude al equipo.",
                    pista = "La energía encuentra su lugar cuando tiene un rol.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Salta, se mueve y busca algo que hacer."
                )
            )
        ),
        leccion(
            numero = 40,
            titulo = "El Espejo Torcido",
            subtitulo = "Mirarme con cariño y con hechos.",
            icono = "🪞",
            conceptos = listOf("autoestima", "hechos", "cariño"),
            teoria = listOf(
                "El espejo torcido exagera los defectos y esconde las fortalezas.",
                "Los hechos y el cariño propio enderezan el espejo."
            ),
            preguntas = listOf(
                ova(
                    escenario = "El espejo torcido solo muestra defectos.",
                    validar = "El espejo torcido exagera. Busco cinco cosas ciertas de mí.",
                    toxica = "¡Eres perfecto, no tienes defectos!",
                    tercera = "Creerle al espejo.",
                    accion = "Escribir cinco cosas ciertas y buenas de ti.",
                    pista = "Los hechos enderezan la imagen.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Mira su reflejo, se toca la cara y se encoge."
                ),
                ova(
                    escenario = "El espejo te compara con todos.",
                    validar = "Puedo mirar mi propio camino.",
                    toxica = "¡Sé como todos los demás!",
                    tercera = "Cambiar todo para parecerte.",
                    accion = "Elegir un valor propio y actuar desde él.",
                    pista = "Tu camino no necesita copiar otro.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Mira a los demás, mira su reflejo y suspira."
                ),
                ova(
                    escenario = "El espejo se endereza y te ves con cariño.",
                    validar = "Me veo con más cariño. Puedo seguir practicando.",
                    toxica = "¡Ya eres perfecto, no cambies!",
                    tercera = "Exageras.",
                    accion = "Sostener el cariño propio con prácticas pequeñas.",
                    pista = "El cariño propio también se practica.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Sonríe a su reflejo, respira tranquilo y se queda ahí.",
                    tipoTercera = TipoRespuestaOva.INVALIDACION
                )
            ),
            esJefe = true
        )
    )

    // =======================================================================
    //  CAPÍTULO 5 · ÉTICA Y DECISIONES
    // =======================================================================

    private fun capitulo5(): List<LeccionEmocional> = listOf(
        leccion(
            numero = 41,
            titulo = "La mariposa y el huracán",
            subtitulo = "Las acciones pequeñas tienen eco.",
            icono = "🦋",
            conceptos = listOf("efecto mariposa", "eco", "cuidado"),
            teoria = listOf(
                "Una acción pequeña puede crecer y afectar a muchas personas.",
                "Elegir con cuidado no es exagerar: es reconocer el eco."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Tira una bolsita de basura al patio.",
                    validar = "Una acción pequeña tiene eco. La guardo y la tiro donde va.",
                    toxica = "¡Una bolsita no importa!",
                    tercera = "Tirar más basura.",
                    accion = "Guardar la basura y tirarla en su lugar.",
                    pista = "El eco de una acción puede ser más grande de lo que parece.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Mira la bolsita, mira el patio y duda."
                ),
                ova(
                    escenario = "Ayudas a un compañero con su mochila.",
                    validar = "Un gesto pequeño puede cambiar su día.",
                    toxica = "¡No cambia nada, no sirve!",
                    tercera = "Ignorarlo.",
                    accion = "Ayudar y sonreír.",
                    pista = "Los gestos pequeños también dejan eco.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Tu compañero carga mucho y se le cae una libreta."
                ),
                ova(
                    escenario = "Repites un chisme que escuchaste.",
                    validar = "Ese chisme puede crecer como huracán. Paro y aviso.",
                    toxica = "¡Solo fue un chiste, no pasa nada!",
                    tercera = "Contarlo a más personas.",
                    accion = "Cortar el chisme y avisar que no quieres participar.",
                    pista = "Un chisme crece cada vez que se repite.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Te ríes, miras a los demás y bajas la voz."
                )
            )
        ),
        leccion(
            numero = 42,
            titulo = "Huellas digitales",
            subtitulo = "Lo que publicas también cuida o lastima.",
            icono = "📱",
            conceptos = listOf("huella digital", "permiso", "seguridad"),
            teoria = listOf(
                "Lo que se publica puede quedarse mucho tiempo y llegar lejos.",
                "Permiso, captura y adulto de confianza son tres herramientas digitales."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Quieres publicar una foto de tu amigo sin avisar.",
                    validar = "Pido permiso antes de publicar. Su huella también importa.",
                    toxica = "¡Todos lo hacen!",
                    tercera = "Publicar sin preguntar.",
                    accion = "Pedir permiso y decidir juntos.",
                    pista = "Una foto también tiene dueño.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Tienes la foto lista y el dedo sobre publicar."
                ),
                ova(
                    escenario = "Alguien te escribe algo feo en línea.",
                    validar = "No respondo en caliente. Guardo captura y aviso a un adulto.",
                    toxica = "¡Respóndele igual!",
                    tercera = "Contestar con un insulto.",
                    accion = "Guardar captura, no responder y avisar a un adulto.",
                    pista = "Responder en caliente alimenta el fuego.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Lees el mensaje, aprietas el celular y respiras fuerte."
                ),
                ova(
                    escenario = "Ves un reto peligroso con muchos likes.",
                    validar = "No lo hago. Mi seguridad vale más que los likes.",
                    toxica = "¡Todos lo logran, tú también puedes!",
                    tercera = "Intentarlo para no quedar mal.",
                    accion = "Decir no y salir del reto.",
                    pista = "Los likes no cuidan tu cuerpo.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Miras el video, miras a tus amigos y dudas."
                )
            )
        ),
        leccion(
            numero = 43,
            titulo = "Verdad o mentira piadosa",
            subtitulo = "Honestidad con cuidado.",
            icono = "🤥",
            conceptos = listOf("honestidad", "amabilidad", "reparación"),
            teoria = listOf(
                "La honestidad no necesita ser cruel; puede ser clara y amable.",
                "Reconocer un error abre la puerta a repararlo."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Rompiste un jarrón y nadie te vio.",
                    validar = "Lo rompí. Quiero ayudar a repararlo.",
                    toxica = "¡Nadie se dará cuenta, no digas nada!",
                    tercera = "Echarle la culpa a otro.",
                    accion = "Decir la verdad y ayudar a reparar.",
                    pista = "La verdad también se puede decir con calma.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Miras los pedazos, miras la puerta y respiras."
                ),
                ova(
                    escenario = "Tu amigo te pide opinión de su dibujo.",
                    validar = "Me gusta cómo pintaste el sol. ¿Quieres que hablemos de la parte que puedes mejorar?",
                    toxica = "¡Es perfecto!",
                    tercera = "Decir que es feo.",
                    accion = "Decir algo cierto y ofrecer una mejora con cariño.",
                    pista = "La honestidad amable dice una verdad y ofrece apoyo.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Tu amigo te muestra el dibujo con orgullo."
                ),
                ova(
                    escenario = "Cometiste un error en un juego y perdieron.",
                    validar = "Fui yo. Puedo pedir otra oportunidad.",
                    toxica = "¡Fue el juego, tú no fallaste!",
                    tercera = "Negar que participaste.",
                    accion = "Asumir tu parte y pedir otra oportunidad.",
                    pista = "Asumir el error también es jugar en equipo.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "El equipo se queda callado y tú miras el piso."
                )
            )
        ),
        leccion(
            numero = 44,
            titulo = "El grupo y yo",
            subtitulo = "Pertenecer sin dejar de ser yo.",
            icono = "👥",
            conceptos = listOf("grupo", "presión", "pertenencia"),
            teoria = listOf(
                "Pertenecer es importante; pertenecer lastimando a otros no es amistad.",
                "Puedes elegir un grupo donde te traten con respeto."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Tus amigos te presionan para molestar a alguien.",
                    validar = "No quiero lastimar. Me voy y busco otra compañía.",
                    toxica = "¡Solo es diversión, no pasa nada!",
                    tercera = "Molestar para encajar.",
                    accion = "Decir no, alejarte y elegir otra compañía.",
                    pista = "Decir no también es pertenecerte.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "El grupo se ríe y te mira para que participes."
                ),
                ova(
                    escenario = "Todos se ríen de un compañero.",
                    validar = "No me río. Puedo decir «no me late» y acercarme al compañero.",
                    toxica = "¡Es solo un chiste!",
                    tercera = "Reírte para no quedar fuera.",
                    accion = "Romper la burla y acercarte al compañero.",
                    pista = "No reírte también es una decisión valiente.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Tu compañero está callado y el grupo ríe."
                ),
                ova(
                    escenario = "Quieres pertenecer a un grupo nuevo.",
                    validar = "Puedo ser yo y buscar un grupo donde me traten bien.",
                    toxica = "¡Haz lo que sea para encajar!",
                    tercera = "Cambiar todo para parecerte a ellos.",
                    accion = "Observar cómo se tratan y elegir con cuidado.",
                    pista = "La pertenencia sana incluye respeto.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Miras al grupo, te acomodas la ropa y dudas."
                )
            )
        ),
        leccion(
            numero = 45,
            titulo = "Pantallas con corazón",
            subtitulo = "Usar la tecnología sin que te use.",
            icono = "🌐",
            conceptos = listOf("pantallas", "privacidad", "límites"),
            teoria = listOf(
                "Las pantallas pueden conectar, aprender y también saturar.",
                "Poner límites y pedir ayuda con lo que ves es cuidarte."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Quiere ver un video más antes de dormir.",
                    validar = "Apago pantallas y aviso a mi familia si algo me preocupa.",
                    toxica = "¡Solo un video más, no pasa nada!",
                    tercera = "Seguir hasta muy tarde.",
                    accion = "Apagar pantallas y hacer una rutina de sueño.",
                    pista = "Un límite de pantallas también es autocuidado.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Mira la hora, bosteza y sigue deslizando."
                ),
                ova(
                    escenario = "Alguien comparte algo privado en un grupo.",
                    validar = "No lo reenvío. Pregunto si está bien y aviso a un adulto.",
                    toxica = "¡Solo lo comparto con uno, no pasa nada!",
                    tercera = "Reenviarlo a todos.",
                    accion = "No reenviar, preguntar y avisar a un adulto.",
                    pista = "La privacidad de otros también se cuida.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "Ves la foto, miras el grupo y dudas."
                ),
                ova(
                    escenario = "Sientes envidia al ver las redes de otros.",
                    validar = "Las redes muestran pedacitos. Cierro y hago algo que me guste.",
                    toxica = "¡No sientas envidia, eso está mal!",
                    tercera = "Comentar algo feo.",
                    accion = "Cerrar la app y hacer algo que disfrutes.",
                    pista = "Las redes muestran una parte, no la vida completa.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Miras fotos, suspiras y te comparas."
                )
            )
        ),
        leccion(
            numero = 46,
            titulo = "Mi huella en el planeta",
            subtitulo = "Cuidar el mundo también es cuidarnos.",
            icono = "🌎",
            conceptos = listOf("planeta", "cuidado", "comunidad"),
            teoria = listOf(
                "El planeta se cuida con acciones pequeñas y repetidas.",
                "Cuidar el lugar donde vives también cuida a quienes viven contigo."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Sales de un cuarto y la luz queda prendida.",
                    validar = "Un gesto pequeño cuida el planeta.",
                    toxica = "¡No sirve de nada, total!",
                    tercera = "Dejar todo prendido.",
                    accion = "Apagar la luz al salir.",
                    pista = "Los gestos pequeños se suman.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Miras el foco, miras la puerta y sigues."
                ),
                ova(
                    escenario = "Ves basura tirada en el patio de la escuela.",
                    validar = "Puedo recoger una parte y pedir ayuda.",
                    toxica = "¡Alguien más lo hará!",
                    tercera = "Ignorarla.",
                    accion = "Recoger una parte y pedir apoyo.",
                    pista = "Una parte también cuenta.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Miras la basura, miras a los demás y dudas."
                ),
                ova(
                    escenario = "Quieres comprar algo de moda rápida.",
                    validar = "Pienso si lo necesito y cuánto durará.",
                    toxica = "¡Compra, tú lo mereces!",
                    tercera = "Comprar sin pensar.",
                    accion = "Esperar y decidir si lo necesitas de verdad.",
                    pista = "Preguntar «¿lo necesito?» cuida tu dinero y el planeta.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "Miras la tienda, miras el precio y dudas."
                )
            )
        ),
        leccion(
            numero = 47,
            titulo = "Decisiones con eco",
            subtitulo = "Lo que elijo también toca a otros.",
            icono = "🔔",
            conceptos = listOf("decisión", "eco", "inclusión"),
            teoria = listOf(
                "Cada decisión tiene un eco en otras personas, aunque no lo veas.",
                "Incluir, respetar y no copiar son decisiones con buen eco."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Puedes copiar la tarea de un compañero.",
                    validar = "Copiar no me enseña. Pido ayuda y hago lo mío.",
                    toxica = "¡Nadie te va a descubrir!",
                    tercera = "Copiar sin pensar.",
                    accion = "Pedir ayuda y hacer tu propio intento.",
                    pista = "Copiar apaga tu aprendizaje.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Miras la hoja del compañero, miras la tuya y dudas."
                ),
                ova(
                    escenario = "Ves a alguien solo en el recreo.",
                    validar = "Lo invito a jugar. Mi decisión tiene eco.",
                    toxica = "¡No es tu problema, no te metas!",
                    tercera = "Ignorarlo.",
                    accion = "Invitarlo a jugar y presentarte.",
                    pista = "Una invitación puede cambiar un recreo completo.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "Miras a la persona sola, miras tu grupo y dudas."
                ),
                ova(
                    escenario = "Tienes prisa y hay una fila larga.",
                    validar = "Respeto la fila. Mi tiempo no vale más que el de otros.",
                    toxica = "¡Tú eres especial, pasa primero!",
                    tercera = "Colarse.",
                    accion = "Esperar tu turno o pedir permiso si es urgente.",
                    pista = "Respetar la fila cuida el trato justo.",
                    emocion = EmocionTormenta.ANSIEDAD,
                    observacion = "Miras la fila, miras el reloj y mueves el pie."
                )
            )
        ),
        leccion(
            numero = 48,
            titulo = "Justicia y compasión",
            subtitulo = "La regla y la persona importan.",
            icono = "⚖️",
            conceptos = listOf("justicia", "compasión", "escucha"),
            teoria = listOf(
                "La justicia escucha a todas las partes antes de decidir.",
                "La compasión no borra la regla: busca una reparación justa."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Dos amigos pelean y cada uno tiene su versión.",
                    validar = "Escucho a ambos y busco una solución justa.",
                    toxica = "¡Qué se arreglen solos, no es tu problema!",
                    tercera = "Echarle la culpa a uno sin escuchar.",
                    accion = "Escuchar a ambos y proponer una reparación.",
                    pista = "La justicia necesita escuchar antes de decidir.",
                    emocion = EmocionTormenta.ENOJO,
                    observacion = "Los dos hablan al mismo tiempo y se miran con enojo."
                ),
                ova(
                    escenario = "Alguien rompió una regla porque tenía hambre.",
                    validar = "La regla importa y la persona también. Busco ayuda para ambos.",
                    toxica = "¡Que lo castiguen sin escuchar nada!",
                    tercera = "Castigarlo muy fuerte.",
                    accion = "Buscar apoyo para la persona y reparar la regla.",
                    pista = "La compasión también busca que se repare el daño.",
                    emocion = EmocionTormenta.TRISTEZA,
                    observacion = "La persona mira la comida, baja la cabeza y tiembla."
                ),
                ova(
                    escenario = "Tú te equivocaste en una decisión del grupo.",
                    validar = "Acepto mi parte y reparo.",
                    toxica = "¡No fue tu culpa, no te preocupes!",
                    tercera = "Negar que participaste.",
                    accion = "Reconocer tu parte y proponer una reparación.",
                    pista = "Aceptar la parte propia también es justicia.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "El grupo te mira y tú bajas la cabeza."
                )
            )
        ),
        leccion(
            numero = 49,
            titulo = "El valor de decir «no»",
            subtitulo = "Un no amable también cuida.",
            icono = "🚪",
            conceptos = listOf("límite", "no", "consentimiento"),
            teoria = listOf(
                "Decir no puede ser amable, claro y firme.",
                "Tu cuerpo y tus cosas también tienen límites."
            ),
            preguntas = listOf(
                ova(
                    escenario = "Te piden prestado algo que necesitas ahora.",
                    validar = "Ahora no puedo prestarlo. Puedo ofrecer otra cosa.",
                    toxica = "¡Si dices no, no te quieren!",
                    tercera = "Decir sí y enojarte después.",
                    accion = "Decir no con calma y ofrecer una alternativa.",
                    pista = "Un no claro evita un sí resentido.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Aprietas tus cosas, miras al otro y dudas."
                ),
                ova(
                    escenario = "Te piden un abrazo y no quieres.",
                    validar = "Si no quiero, puedo decir: «no quiero abrazos ahora».",
                    toxica = "¡Los abrazos siempre se aceptan!",
                    tercera = "Empujar para quitártelo.",
                    accion = "Decir no con respeto y ofrecer otra forma de cariño.",
                    pista = "Tu cuerpo decide quién se acerca.",
                    emocion = EmocionTormenta.VERGUENZA,
                    observacion = "La persona abre los brazos y tú te echas para atrás."
                ),
                ova(
                    escenario = "Te piden guardar un secreto que te hace daño.",
                    validar = "No lo guardo si hace daño. Aviso a un adulto.",
                    toxica = "¡Los amigos no se acusan!",
                    tercera = "Guardarlo para siempre.",
                    accion = "Avisar a un adulto de confianza.",
                    pista = "Un secreto que lastima necesita ayuda.",
                    emocion = EmocionTormenta.MIEDO,
                    observacion = "Te hablan al oído, miran alrededor y te aprietan el brazo."
                )
            )
        ),
        leccion(
            numero = 50,
            titulo = "El Alebrije de las Decisiones",
            subtitulo = "Llevar las herramientas a la vida real.",
            icono = "🦋",
            conceptos = listOf("decisiones", "cuidado", "cierre"),
            teoria = listOf(
                "Cada decisión puede cuidar o lastimar; elegir con atención es un superpoder.",
                "Terminar la ruta no es el final: es el inicio de practicar cada día."
            ),
            preguntas = listOf(
                ova(
                    escenario = "El alebrije te pide elegir entre una risa compartida y una burla.",
                    validar = "Elijo la risa que no lastima.",
                    toxica = "¡Todo es broma, no pasa nada!",
                    tercera = "Burlarte para que los demás rían.",
                    accion = "Elegir la risa que comparte y no humilla.",
                    pista = "La risa que lastima no es juego.",
                    emocion = EmocionTormenta.ALEGRIA,
                    observacion = "El alebrije señala dos caminos con gestos distintos."
                ),
                ova(
                    escenario = "El alebrije te muestra dos caminos: el fácil y el que cuida.",
                    validar = "Elijo el que cuida a otros y a mí.",
                    toxica = "¡Elige el más fácil, total nadie ve!",
                    tercera = "El que da más likes.",
                    accion = "Elegir el camino que cuida a todos, incluido tú.",
                    pista = "El camino fácil no siempre es el que cuida.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "Los dos caminos brillan distinto y el alebrije espera."
                ),
                ova(
                    escenario = "El alebrije te dice adiós y te pide llevar lo aprendido.",
                    validar = "Puedo llevar estas herramientas a mi vida.",
                    toxica = "¡Ya eres perfecto, no necesitas practicar!",
                    tercera = "No aprendiste nada.",
                    accion = "Practicar cada día una herramienta de calma.",
                    pista = "Lo aprendido crece cuando se practica.",
                    emocion = EmocionTormenta.CALMA,
                    observacion = "El alebrije sonríe, la nube se aclara y te extiende la mano.",
                    tipoTercera = TipoRespuestaOva.INVALIDACION
                )
            ),
            esJefe = true
        )
    )

    // -----------------------------------------------------------------------
    //  Ayudantes de construcción
    // -----------------------------------------------------------------------

    private fun leccion(
        numero: Int,
        titulo: String,
        subtitulo: String,
        icono: String,
        conceptos: List<String>,
        teoria: List<String>,
        preguntas: List<OvaExercise>,
        esJefe: Boolean = false
    ): LeccionEmocional {
        val id = idDe(numero)
        val capitulo = ((numero - 1) / 10) + 1
        return LeccionEmocional(
            id = id,
            numero = numero,
            capitulo = capitulo,
            titulo = titulo,
            subtitulo = subtitulo,
            tier = capitulo,
            icono = icono,
            conceptos = conceptos,
            teoria = teoria,
            preguntas = preguntas.mapIndexed { indice, reto ->
                reto.copy(id = "${id}_reto_${indice + 1}")
            },
            esJefeDeCapitulo = esJefe
        )
    }

    private fun ova(
        escenario: String,
        validar: String,
        toxica: String,
        tercera: String,
        accion: String,
        pista: String,
        emocion: EmocionTormenta,
        observacion: String = "",
        tipoTercera: TipoRespuestaOva = TipoRespuestaOva.IMPULSIVA,
        mecanica: MecanicaOva = MecanicaOva.OPCIONES,
        concepto: String = "validación"
    ): OvaExercise {
        val base = listOf(
            OvaOpcion(validar, TipoRespuestaOva.VALIDACION),
            OvaOpcion(toxica, TipoRespuestaOva.POSITIVIDAD_TOXICA),
            OvaOpcion(tercera, tipoTercera)
        )
        val giro = (escenario.hashCode() and Int.MAX_VALUE) % base.size
        val opciones = List(base.size) { indice -> base[(indice + giro) % base.size] }
        return OvaExercise(
            escenario = escenario,
            observacion = observacion.ifBlank {
                "Mira su cara, su cuerpo y lo que acaba de pasar."
            },
            opciones = opciones,
            accionModelo = accion,
            pista = pista,
            emocion = emocion,
            mecanica = mecanica,
            concepto = concepto
        )
    }
}
