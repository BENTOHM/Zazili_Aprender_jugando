package com.example.zazili.domain.curriculum.ciencias

import androidx.compose.runtime.Immutable
import com.example.zazili.domain.ciencias.CapaFiltro
import com.example.zazili.domain.ciencias.ElementoCientifico
import com.example.zazili.domain.ciencias.ORDEN_FILTRO

/** Cómo se juega un reto de Ciencias. */
enum class MecanicaCiencia {
    OPCIONES,
    FILTRO_MAGICO,
    REACCION
}

/**
 * Un reto corto del Laboratorio del Cenote. Los retos de [MecanicaCiencia.REACCION]
 * traen los materiales que se pueden arrastrar y el producto que debe aparecer;
 * los de [MecanicaCiencia.FILTRO_MAGICO] traen la pila correcta de capas.
 */
@Immutable
data class PreguntaCiencia(
    val enunciado: String,
    val opciones: List<String>,
    val indiceCorrecto: Int,
    val pista: String,
    val mecanica: MecanicaCiencia = MecanicaCiencia.OPCIONES,
    val visuales: List<String> = emptyList(),
    val elementos: List<ElementoCientifico> = emptyList(),
    val resultadoEsperado: ElementoCientifico? = null,
    val capasCorrectas: List<CapaFiltro> = emptyList()
) {
    init {
        require(opciones.size >= 2) { "Cada reto de Ciencias necesita al menos dos opciones" }
        require(indiceCorrecto in opciones.indices) { "La respuesta correcta está fuera de rango" }
        require(mecanica != MecanicaCiencia.REACCION || elementos.size >= 2) {
            "Un reto de reacción necesita al menos dos materiales"
        }
        require(mecanica != MecanicaCiencia.REACCION || resultadoEsperado != null) {
            "Un reto de reacción necesita un producto esperado"
        }
        require(mecanica != MecanicaCiencia.FILTRO_MAGICO || capasCorrectas.isNotEmpty()) {
            "Un reto de filtro necesita capas"
        }
    }
}

/** Los cinco capítulos del currículo de Ciencias. */
enum class CapituloCiencia(
    val numero: Int,
    val titulo: String,
    val lema: String,
    val jefe: String,
    val dichoDelJefe: String
) {
    BIODIVERSIDAD(
        numero = 1,
        titulo = "Biodiversidad",
        lema = "Flora, fauna y tesoros que solo viven en México.",
        jefe = "La Reina del Maguey",
        dichoDelJefe = "Mi jardín tiene ajolotes, jaguares y monarcas. ¿Los conoces a todos?"
    ),
    CIENCIAS_TIERRA(
        numero = 2,
        titulo = "Ciencias de la Tierra",
        lema = "El agua viaja, cambia de estado y esculpe el mundo.",
        jefe = "El Tlaloque del Agua",
        dichoDelJefe = "Yo cuido las nubes y los cenotes. ¡A ver si entiendes mi ciclo!"
    ),
    ECOSISTEMAS(
        numero = 3,
        titulo = "Ecosistemas",
        lema = "Redes de vida donde todos tienen un lugar.",
        jefe = "El Jaguar de las Sombras",
        dichoDelJefe = "En mi selva nada sobra: cada hoja, cada presa y cada hongo cuenta."
    ),
    FISICA_QUIMICA(
        numero = 4,
        titulo = "Física y Química",
        lema = "Fuerzas, imanes y reacciones que puedes probar.",
        jefe = "La Alebrije Magnética",
        dichoDelJefe = "Mis cuernos atraen tornillos y mis burbujas explican la química."
    ),
    SUSTENTABILIDAD(
        numero = 5,
        titulo = "Sustentabilidad",
        lema = "Decisiones pequeñas que cuidan el planeta entero.",
        jefe = "El Guardián del Cenote",
        dichoDelJefe = "El agua limpia no es magia: es orden, cuidado y trabajo en equipo."
    )
}

/** Una lección completa del mapa de Ciencias. */
@Immutable
data class LessonNode(
    val id: String,
    val numero: Int,
    val capitulo: CapituloCiencia,
    val titulo: String,
    val subtitulo: String,
    val tier: Int,
    val icono: String,
    val conceptos: List<String>,
    val teoria: List<String>,
    val preguntas: List<PreguntaCiencia>
) {
    val esJefeDeCapitulo: Boolean
        get() = numero % 10 == 0

    init {
        require(tier in 1..5) { "La dificultad de $id debe estar entre 1 y 5" }
        require(preguntas.size == 3) { "La lección $id necesita tres retos" }
    }
}

/**
 * Currículo de Ciencias de Zazili 2.0: 5 capítulos, 50 lecciones y tres retos
 * por lección. Mezcla biodiversidad mexicana, ciencias de la Tierra, ecología,
 * física, química y sustentabilidad. La lección 50 cierra con "¡Continuará...!".
 */
object ScienceCurriculumRepository {

    const val ULTIMO_NODO_ID = "ciencias_50"

    val capitulos: List<CapituloCiencia> = CapituloCiencia.entries

    // Debe inicializarse antes que `lecciones`: `filtro()` lo usa como valor
    // por defecto y, en Kotlin, los miembros del object se inicializan en
    // orden de declaración. Si queda abajo, el primer reto de filtro recibe
    // una lista nula y el mapa de Ciencias truena al abrirse.
    private val OPCIONES_FILTRO = listOf(
        "Grava → Arena → Carbón → Algodón",
        "Algodón → Arena → Carbón → Grava",
        "Arena → Grava → Algodón → Carbón",
        "Carbón → Grava → Arena → Algodón"
    )

    val lecciones: List<LessonNode> = construirLecciones()

    val totalLecciones: Int
        get() = lecciones.size

    fun leccionPorId(id: String?): LessonNode? =
        if (id.isNullOrBlank()) null else lecciones.firstOrNull { it.id == id }

    fun leccionesDe(capitulo: CapituloCiencia): List<LessonNode> =
        lecciones.filter { it.capitulo == capitulo }

    fun capituloPorNumero(numero: Int): CapituloCiencia? =
        capitulos.firstOrNull { it.numero == numero }

    fun idDe(numero: Int): String = "ciencias_" + numero.toString().padStart(2, '0')

    private fun p(
        enunciado: String,
        opciones: List<String>,
        correcta: Int,
        pista: String,
        visuales: List<String> = emptyList()
    ): PreguntaCiencia = PreguntaCiencia(
        enunciado = enunciado,
        opciones = opciones,
        indiceCorrecto = correcta,
        pista = pista,
        visuales = visuales
    )

    private fun reaccion(
        enunciado: String,
        opciones: List<String>,
        correcta: Int,
        pista: String,
        elementos: List<ElementoCientifico>,
        resultado: ElementoCientifico
    ): PreguntaCiencia = PreguntaCiencia(
        enunciado = enunciado,
        opciones = opciones,
        indiceCorrecto = correcta,
        pista = pista,
        mecanica = MecanicaCiencia.REACCION,
        visuales = elementos.map { it.emoji },
        elementos = elementos,
        resultadoEsperado = resultado
    )

    private fun filtro(
        enunciado: String,
        pista: String,
        opciones: List<String> = OPCIONES_FILTRO,
        correcta: Int = 0,
        capas: List<CapaFiltro> = ORDEN_FILTRO
    ): PreguntaCiencia = PreguntaCiencia(
        enunciado = enunciado,
        opciones = opciones,
        indiceCorrecto = correcta,
        pista = pista,
        mecanica = MecanicaCiencia.FILTRO_MAGICO,
        visuales = capas.map { it.emoji },
        capasCorrectas = capas
    )

    private fun leccion(
        numero: Int,
        titulo: String,
        subtitulo: String,
        icono: String,
        conceptos: List<String>,
        teoria: List<String>,
        preguntas: List<PreguntaCiencia>
    ): LessonNode {
        val capitulo = capitulos[(numero - 1) / 10]
        return LessonNode(
            id = idDe(numero),
            numero = numero,
            capitulo = capitulo,
            titulo = titulo,
            subtitulo = subtitulo,
            tier = capitulo.numero,
            icono = icono,
            conceptos = conceptos,
            teoria = teoria,
            preguntas = preguntas
        )
    }

    @Suppress("LongMethod")
    private fun construirLecciones(): List<LessonNode> = listOf(
        // ===================================================================
        //  CAPÍTULO 1 · BIODIVERSIDAD
        // ===================================================================
        leccion(
            numero = 1,
            titulo = "El ajolote que sonríe",
            subtitulo = "Un tesoro de Xochimilco que conserva sus branquias.",
            icono = "🦎",
            conceptos = listOf("ajolote", "endémico", "regeneración"),
            teoria = listOf(
                "El ajolote es un anfibio mexicano que conserva sus branquias de bebé toda la vida.",
                "Es endémico de Xochimilco: en libertad solo vive en esos canales."
            ),
            preguntas = listOf(
                p(
                    "¿Qué animal es endémico de México y vive en Xochimilco?",
                    listOf("Ajolote", "Panda", "Canguro", "Pingüino"),
                    0,
                    "Su carita sonriente y sus branquias rosadas son inconfundibles.",
                    listOf("🦎", "🌊")
                ),
                p(
                    "El ajolote puede volver a formar una patita si la pierde. ¿Cómo se llama esa habilidad?",
                    listOf("Regeneración", "Migración", "Camuflaje", "Hibernación"),
                    0,
                    "Regenerar es volver a crecer una parte del cuerpo.",
                    listOf("🦎", "✨")
                ),
                p(
                    "¿Qué necesita el ajolote para vivir sanito?",
                    listOf("Agua limpia y plantas", "Arena seca y sol", "Solo piedras", "Aire de la ciudad"),
                    0,
                    "Los canales limpios con vegetación son su casa y su comida.",
                    listOf("💧", "🌿")
                )
            )
        ),
        leccion(
            numero = 2,
            titulo = "La guacamaya que grita",
            subtitulo = "Plumas de colores que se comunican en la selva.",
            icono = "🦜",
            conceptos = listOf("guacamaya", "selva", "semillas"),
            teoria = listOf(
                "Los colores fuertes de la guacamaya le ayudan a reconocerse y comunicarse.",
                "Come frutas y semillas, y dispersa semillas nuevas por la selva."
            ),
            preguntas = listOf(
                p(
                    "¿Para qué le sirven los colores fuertes a la guacamaya?",
                    listOf("Para comunicarse y verse entre la selva", "Para esconderse en la nieve", "Para nadar más rápido", "Para hacer miel"),
                    0,
                    "En la selva verde, sus plumas rojas y azules funcionan como señales.",
                    listOf("🦜", "🎨")
                ),
                p(
                    "¿Qué come principalmente la guacamaya?",
                    listOf("Frutas y semillas", "Piedras", "Peces", "Zapatos"),
                    0,
                    "Su pico fuerte abre frutos y semillas duras.",
                    listOf("🍎", "🌰")
                ),
                p(
                    "La guacamaya vive en la selva. ¿Qué pasa si se tala su árbol?",
                    listOf("Pierde su hogar y su alimento", "Se vuelve pez", "Le salen más plumas", "Nada cambia"),
                    0,
                    "Cada árbol derribado quita casa, comida y refugio.",
                    listOf("🌳", "🪓")
                )
            )
        ),
        leccion(
            numero = 3,
            titulo = "El jaguar de la selva",
            subtitulo = "Manchas que se esconden entre las sombras.",
            icono = "🐆",
            conceptos = listOf("jaguar", "camuflaje", "depredador"),
            teoria = listOf(
                "Las rosetas del jaguar rompen su silueta y lo ayudan a camuflarse.",
                "Es un depredador: caza para comer y regula a otros animales."
            ),
            preguntas = listOf(
                p(
                    "Las manchas del jaguar se llaman rosetas. ¿Para qué le ayudan?",
                    listOf("A camuflarse entre las sombras", "A volar", "A cantar", "A nadar"),
                    0,
                    "Las manchas imitan la luz y la sombra de la selva.",
                    listOf("🐆", "🌿")
                ),
                p(
                    "El jaguar es un depredador. ¿Qué significa?",
                    listOf("Caza para comer", "Come solo pasto", "Duerme todo el año", "Fabrica su comida"),
                    0,
                    "Los depredadores cazan a otros animales para alimentarse.",
                    listOf("🐆", "🍖")
                ),
                p(
                    "¿Cuál es una acción que ayuda al jaguar?",
                    listOf("Proteger la selva y no cazarlo", "Quitarle su territorio", "Vender su piel", "Meterlo a una jaula pequeña"),
                    0,
                    "Conservar su territorio es conservar al jaguar.",
                    listOf("🌳", "🐾")
                )
            )
        ),
        leccion(
            numero = 4,
            titulo = "La mariposa monarca viajera",
            subtitulo = "Un viaje de miles de kilómetros hasta los oyameles.",
            icono = "🦋",
            conceptos = listOf("monarca", "migración", "oyamel"),
            teoria = listOf(
                "Cada año las monarca viajan a los bosques de oyamel del centro de México.",
                "Necesitan flores para comer y árboles para descansar durante el viaje."
            ),
            preguntas = listOf(
                p(
                    "¿A dónde viajan las mariposas monarca cada año?",
                    listOf("A los bosques de oyamel en México", "Al desierto de Sonora", "A la playa", "A la Luna"),
                    0,
                    "Los oyameles les dan abrigo y humedad en invierno.",
                    listOf("🦋", "🌲")
                ),
                p(
                    "¿Qué necesitan las monarca para descansar en su viaje?",
                    listOf("Árboles y flores", "Focos de colores", "Hielo", "Plástico"),
                    0,
                    "Las flores son su comida y los árboles su hotel.",
                    listOf("🌼", "🌳")
                ),
                p(
                    "¿Qué las guía en su viaje?",
                    listOf("El sol y el paisaje", "Un mapa de papel", "El ruido de la ciudad", "Los semáforos"),
                    0,
                    "Se orientan con la posición del sol y con señales del paisaje.",
                    listOf("☀️", "🧭")
                )
            )
        ),
        leccion(
            numero = 5,
            titulo = "El cactus que guarda agua",
            subtitulo = "Una planta que aprende a vivir con poquita lluvia.",
            icono = "🌵",
            conceptos = listOf("cactus", "adaptación", "desierto"),
            teoria = listOf(
                "El cactus guarda agua en su tallo y pierde muy poca por sus espinas.",
                "Sus espinas son hojas transformadas que protegen y reducen la pérdida de agua."
            ),
            preguntas = listOf(
                p(
                    "¿Por qué el cactus puede vivir en el desierto?",
                    listOf("Guarda agua en su tallo", "Tiene branquias", "Come arena", "Duerme bajo el agua"),
                    0,
                    "Su tallo acolchonado funciona como una botella de agua.",
                    listOf("🌵", "💧")
                ),
                p(
                    "Las espinas del cactus son hojas transformadas. ¿Para qué le sirven?",
                    listOf("Para perder menos agua", "Para atrapar peces", "Para volar", "Para hacer sombra"),
                    0,
                    "Menos superficie en las hojas significa menos agua perdida.",
                    listOf("🌵", "☀️")
                ),
                p(
                    "¿Qué pasa si riegas un cactus todos los días con mucha agua?",
                    listOf("Sus raíces pueden pudrirse", "Crece más rápido siempre", "Se vuelve piedra", "Le salen flores de inmediato"),
                    0,
                    "Está adaptado a poca agua; el exceso enferma sus raíces.",
                    listOf("🌵", "🚿")
                )
            )
        ),
        leccion(
            numero = 6,
            titulo = "El maguey y el tequio",
            subtitulo = "Una planta generosa y un trabajo que se hace en equipo.",
            icono = "🌿",
            conceptos = listOf("maguey", "tequio", "comunidad"),
            teoria = listOf(
                "El maguey da aguamiel, fibras y sombra; en México se aprovecha desde hace siglos.",
                "El tequio es trabajo comunitario: entre todos se cuida el pueblo y su naturaleza."
            ),
            preguntas = listOf(
                p(
                    "El maguey se usa en México para hacer...",
                    listOf("Aguamiel y fibras", "Vidrio", "Gasolina", "Plástico"),
                    0,
                    "Del maguey sale aguamiel y también fibra para telas y cuerdas.",
                    listOf("🌿", "🥤")
                ),
                p(
                    "¿Qué es el tequio?",
                    listOf("Trabajo comunitario para el bien de todos", "Un juego de pelota", "Una fruta", "Un tipo de nube"),
                    0,
                    "Tequio es cuando la comunidad se organiza para cuidar algo de todos.",
                    listOf("🤝", "🏘️")
                ),
                p(
                    "¿Cómo se cuida un maguey?",
                    listOf("Se deja crecer sin arrancarlo", "Se riega con aceite", "Se corta cada día", "Se esconde del sol"),
                    0,
                    "El maguey tarda años en madurar; cortarlo antes de tiempo lo pierde.",
                    listOf("🌿", "⏳")
                )
            )
        ),
        leccion(
            numero = 7,
            titulo = "La vaquita marina",
            subtitulo = "La pequeña marsopa que solo vive en el Golfo de California.",
            icono = "🐬",
            conceptos = listOf("vaquita", "endémica", "redes"),
            teoria = listOf(
                "La vaquita marina es la marsopa más pequeña del mundo y es endémica de México.",
                "Su mayor peligro son las redes de pesca en las que queda atrapada."
            ),
            preguntas = listOf(
                p(
                    "¿Dónde vive la vaquita marina?",
                    listOf("En el Golfo de California", "En el Amazonas", "En el Polo Norte", "En un río de Europa"),
                    0,
                    "Es un tesoro del mar del norte de México.",
                    listOf("🐬", "🌊")
                ),
                p(
                    "¿Cuál es la mayor amenaza para la vaquita?",
                    listOf("Quedar atrapada en redes de pesca", "Los huracanes de nieve", "Comer algas", "El canto de las ballenas"),
                    0,
                    "Las redes que no dejan escapar a los peces tampoco dejan escapar a la vaquita.",
                    listOf("🐬", "🕸️")
                ),
                p(
                    "¿Qué ayuda a protegerla?",
                    listOf("Usar redes seguras y respetar la veda", "Más redes para pescar", "Capturarla para un acuario", "Hacer ruido en el mar"),
                    0,
                    "La pesca responsable y las redes que no atrapan marsopas son clave.",
                    listOf("🐬", "🛟")
                )
            )
        ),
        leccion(
            numero = 8,
            titulo = "El teporingo entre volcanes",
            subtitulo = "Un conejo pequeño que vive cerca del Popocatépetl.",
            icono = "🐇",
            conceptos = listOf("teporingo", "volcanes", "pastizal"),
            teoria = listOf(
                "El teporingo es un conejo endémico de las montañas del centro de México.",
                "Vive entre pastizales y se esconde muy bien; su hogar es frágil."
            ),
            preguntas = listOf(
                p(
                    "El teporingo es un conejo que vive...",
                    listOf("En las montañas de los volcanes", "En la playa", "En el mar", "En el desierto de hielo"),
                    0,
                    "Su nombre viene de los tepetates, los cerros de la región.",
                    listOf("🐇", "🌋")
                ),
                p(
                    "¿Por qué es difícil verlo?",
                    listOf("Vive entre pastizales y se esconde", "Es transparente", "Vuela muy alto", "Solo sale de noche en la ciudad"),
                    0,
                    "Su pelaje grisáceo se confunde con el pastizal seco.",
                    listOf("🐇", "🌾")
                ),
                p(
                    "¿Qué lo pone en peligro?",
                    listOf("Perder su pastizal y el fuego", "El exceso de lluvia", "Comer hierba", "Correr"),
                    0,
                    "Cuando se quema o se urbaniza su pastizal, se queda sin casa.",
                    listOf("🐇", "🔥")
                )
            )
        ),
        leccion(
            numero = 9,
            titulo = "La milpa viva",
            subtitulo = "Maíz, frijol y calabaza: una familia que se ayuda.",
            icono = "🌽",
            conceptos = listOf("milpa", "maíz", "suelo"),
            teoria = listOf(
                "La milpa combina maíz, frijol y calabaza para aprovechar mejor el suelo y el agua.",
                "El frijol devuelve nitrógeno a la tierra y la calabaza cubre el suelo."
            ),
            preguntas = listOf(
                p(
                    "La milpa combina maíz, frijol y calabaza. ¿Qué beneficio tiene?",
                    listOf("Aprovecha mejor el suelo y el agua", "Solo crece de noche", "Necesita plástico", "No necesita sol"),
                    0,
                    "Cada planta ayuda a las otras de una forma distinta.",
                    listOf("🌽", "🫘", "🎃")
                ),
                p(
                    "El frijol devuelve nutrientes a la tierra. ¿Con qué ayuda?",
                    listOf("Con nitrógeno", "Con oro", "Con sal", "Con humo"),
                    0,
                    "Sus raíces trabajan con bacterias que fijan nitrógeno.",
                    listOf("🫘", "🌱")
                ),
                p(
                    "¿Qué es una semilla nativa?",
                    listOf("Una variedad que se ha cuidado en México por generaciones", "Una semilla de juguete", "Una piedra", "Una semilla hecha de plástico"),
                    0,
                    "Las semillas nativas guardan la historia y el sabor de cada región.",
                    listOf("🌽", "📜")
                )
            )
        ),
        leccion(
            numero = 10,
            titulo = "La Reina del Maguey",
            subtitulo = "Jefa del capítulo: un paseo por los tesoros de México.",
            icono = "👑",
            conceptos = listOf("endémico", "hábitat", "biodiversidad"),
            teoria = listOf(
                "México es uno de los países con más biodiversidad del mundo.",
                "Proteger un hábitat protege a todas las especies que viven en él."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál de estos animales es endémico de México?",
                    listOf("Ajolote", "León africano", "Panda", "Canguro"),
                    0,
                    "El ajolote solo existe de forma natural en México.",
                    listOf("🦎", "🇲🇽")
                ),
                p(
                    "¿Qué relación hay entre una mariposa monarca y los bosques de oyamel?",
                    listOf("Los bosques son su refugio de invierno", "Ahí construyen casas", "Se alimentan de madera", "No tienen relación"),
                    0,
                    "Los oyameles la protegen del frío y de la lluvia.",
                    listOf("🦋", "🌲")
                ),
                p(
                    "¿Cuál acción protege la biodiversidad mexicana?",
                    listOf("Cuidar hábitats y no traficar especies", "Comprar animales silvestres", "Talar selvas", "Soltar mascotas en el bosque"),
                    0,
                    "Cuidar el hogar de cada especie es la mejor protección.",
                    listOf("🌎", "🤝")
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 2 · CIENCIAS DE LA TIERRA
        // ===================================================================
        leccion(
            numero = 11,
            titulo = "El viaje de una gota",
            subtitulo = "Del mar a la nube y de la nube a la montaña.",
            icono = "💧",
            conceptos = listOf("ciclo del agua", "evaporación", "lluvia"),
            teoria = listOf(
                "El sol calienta el agua y la convierte en vapor: eso es la evaporación.",
                "El vapor se enfría, forma nubes y cae como lluvia: el ciclo vuelve a empezar."
            ),
            preguntas = listOf(
                p(
                    "El sol calienta el agua del mar y la convierte en vapor. ¿Cómo se llama?",
                    listOf("Evaporación", "Condensación", "Congelación", "Filtración"),
                    0,
                    "Evaporar es pasar de líquido a gas.",
                    listOf("☀️", "💧", "☁️")
                ),
                p(
                    "El vapor sube, se enfría y forma nubes. ¿Cómo se llama?",
                    listOf("Condensación", "Evaporación", "Derretimiento", "Combustión"),
                    0,
                    "Condensar es volver a juntar el vapor en gotitas.",
                    listOf("☁️", "💧")
                ),
                p(
                    "Cuando la nube pesa demasiado, el agua cae como...",
                    listOf("Lluvia", "Humo", "Arena", "Plástico"),
                    0,
                    "Las gotitas se juntan hasta que la nube ya no las sostiene.",
                    listOf("🌧️", "🌎")
                )
            )
        ),
        leccion(
            numero = 12,
            titulo = "Nubes que cargan lluvia",
            subtitulo = "Vapor, frío y gotitas que se encuentran.",
            icono = "☁️",
            conceptos = listOf("nube", "vapor", "condensación"),
            teoria = listOf(
                "Una nube se forma cuando el vapor de agua sube y se encuentra con aire frío.",
                "El vapor se convierte en gotitas diminutas que flotan juntas."
            ),
            preguntas = listOf(
                p(
                    "¿Qué necesita una nube para formar gotitas?",
                    listOf("Vapor de agua y frío", "Arena y calor", "Humo y plástico", "Sal y viento seco"),
                    0,
                    "El vapor se enfría y se condensa en gotitas.",
                    listOf("☁️", "❄️")
                ),
                reaccion(
                    enunciado = "Arrastra el AGUA hasta el CALOR y mira qué se forma.",
                    opciones = listOf("Vapor de agua", "Hielo", "Arena", "Humo"),
                    correcta = 0,
                    pista = "El calor hace que el agua líquida se convierta en gas.",
                    elementos = listOf(ElementoCientifico.AGUA, ElementoCientifico.CALOR, ElementoCientifico.HIELO),
                    resultado = ElementoCientifico.VAPOR
                ),
                p(
                    "¿Qué pasa con el vapor cuando toca una superficie fría?",
                    listOf("Se convierte en gotitas", "Se vuelve piedra", "Desaparece para siempre", "Se convierte en fuego"),
                    0,
                    "El frío le quita energía y el vapor se condensa.",
                    listOf("☁️", "🥶", "💧")
                )
            )
        ),
        leccion(
            numero = 13,
            titulo = "El hielo que se derrite",
            subtitulo = "El calor despierta al agua que dormía como hielo.",
            icono = "🧊",
            conceptos = listOf("sólido", "líquido", "derretimiento"),
            teoria = listOf(
                "El hielo es agua en estado sólido; sus moléculas están muy ordenadas.",
                "Cuando recibe calor, el hielo se derrite y vuelve a ser agua líquida."
            ),
            preguntas = listOf(
                reaccion(
                    enunciado = "Arrastra el HIELO hasta el CALOR.",
                    opciones = listOf("Agua líquida", "Más hielo", "Metal", "Arena"),
                    correcta = 0,
                    pista = "El calor derrite el hielo y lo convierte en agua.",
                    elementos = listOf(ElementoCientifico.HIELO, ElementoCientifico.CALOR, ElementoCientifico.IMAN),
                    resultado = ElementoCientifico.AGUA
                ),
                p(
                    "El hielo es agua en estado...",
                    listOf("Sólido", "Líquido", "Gas", "Plasma"),
                    0,
                    "En el hielo las moléculas están quietecitas y ordenadas.",
                    listOf("🧊", "💧")
                ),
                p(
                    "¿Qué pasa si pones agua líquida en el congelador?",
                    listOf("Se convierte en hielo", "Se vuelve vapor", "Se hace arena", "Se convierte en metal"),
                    0,
                    "El frío le quita energía y el agua se congela.",
                    listOf("💧", "❄️", "🧊")
                )
            )
        ),
        leccion(
            numero = 14,
            titulo = "Vapor que sube y baja",
            subtitulo = "La condensación devuelve el agua a la tierra.",
            icono = "♨️",
            conceptos = listOf("vapor", "condensación", "ciclo"),
            teoria = listOf(
                "El vapor es agua en estado gaseoso y no se ve; lo que vemos es el agua condensada.",
                "Al enfriarse, el vapor se convierte en gotitas: así se forman la niebla y el rocío."
            ),
            preguntas = listOf(
                reaccion(
                    enunciado = "Arrastra el VAPOR hasta el FRÍO.",
                    opciones = listOf("Gotitas de agua", "Fuego", "Arena", "Metal"),
                    correcta = 0,
                    pista = "Al enfriarse, el vapor se condensa en gotitas.",
                    elementos = listOf(ElementoCientifico.VAPOR, ElementoCientifico.FRIO, ElementoCientifico.CALOR),
                    resultado = ElementoCientifico.AGUA
                ),
                p(
                    "El vapor de una olla toca la tapa fría y se forman gotitas. ¿Qué cambio ocurre?",
                    listOf("Condensación", "Evaporación", "Fusión", "Magnetismo"),
                    0,
                    "El vapor pasa de gas a líquido al enfriarse.",
                    listOf("🍲", "💧")
                ),
                p(
                    "¿Qué estado tiene el vapor de agua?",
                    listOf("Gas", "Sólido", "Líquido", "Polvo"),
                    0,
                    "El vapor es agua en estado gaseoso.",
                    listOf("☁️", "💨")
                )
            )
        ),
        leccion(
            numero = 15,
            titulo = "El suelo que filtra",
            subtitulo = "Arena, arcilla y el camino del agua bajo tus pies.",
            icono = "🟤",
            conceptos = listOf("suelo", "permeabilidad", "filtración"),
            teoria = listOf(
                "La arena tiene granos grandes y deja pasar el agua rápido.",
                "La arcilla tiene granos pequeños y retiene el agua; por eso puede encharcarse."
            ),
            preguntas = listOf(
                p(
                    "¿Qué tipo de suelo deja pasar el agua más rápido?",
                    listOf("La arena", "La arcilla", "El cemento", "El plástico"),
                    0,
                    "Los granos grandes de la arena dejan espacios para el agua.",
                    listOf("🏖️", "💧")
                ),
                p(
                    "El suelo con mucha arcilla retiene agua. ¿Qué puede pasar?",
                    listOf("Se encharca", "Se vuelve piedra", "Desaparece", "Se hace gas"),
                    0,
                    "Si el agua no puede pasar, se queda arriba formando charcos.",
                    listOf("🟤", "🌧️")
                ),
                p(
                    "¿Para qué sirve el suelo en un ecosistema?",
                    listOf("Sostiene plantas y guarda agua", "Solo para pisarlo", "Para hacer ruido", "Para volar"),
                    0,
                    "El suelo es casa, despensa y esponja de agua para las plantas.",
                    listOf("🌱", "🟤")
                )
            )
        ),
        leccion(
            numero = 16,
            titulo = "Ríos que nacen en la montaña",
            subtitulo = "Una cuenca conecta la cima con el mar.",
            icono = "🏔️",
            conceptos = listOf("río", "cuenca", "montaña"),
            teoria = listOf(
                "Los ríos nacen de manantiales y de la lluvia que escurre por las montañas.",
                "Una cuenca es todo el terreno que drena hacia un mismo río."
            ),
            preguntas = listOf(
                p(
                    "¿De dónde nace un río?",
                    listOf("De manantiales y lluvia en las montañas", "Del mar", "De una nube de humo", "De una fábrica"),
                    0,
                    "El agua de lluvia y los manantiales alimentan su cauce.",
                    listOf("🏔️", "💧")
                ),
                p(
                    "¿Qué es una cuenca?",
                    listOf("El terreno por donde corre el agua hacia un río", "Una nube", "Un tipo de pez", "Una piedra"),
                    0,
                    "Piensa en un tazón: toda el agua escurre hacia el mismo punto.",
                    listOf("🗺️", "💧")
                ),
                p(
                    "Si tiramos basura en la montaña, ¿qué pasa con el río?",
                    listOf("El agua baja contaminada", "Se limpia sola", "Se vuelve dulce", "No pasa nada"),
                    0,
                    "El río arrastra todo lo que encuentra en su cuenca.",
                    listOf("🗑️", "🌊")
                )
            )
        ),
        leccion(
            numero = 17,
            titulo = "El cenote escondido",
            subtitulo = "Agua subterránea que guarda historias de Yucatán.",
            icono = "🕳️",
            conceptos = listOf("cenote", "agua subterránea", "caliza"),
            teoria = listOf(
                "Un cenote es una entrada natural al agua subterránea, común en la península de Yucatán.",
                "El agua llega ahí después de filtrarse por la roca caliza."
            ),
            preguntas = listOf(
                p(
                    "¿Qué es un cenote?",
                    listOf("Una entrada de agua subterránea", "Un volcán de nieve", "Una cueva de hielo", "Un río de lava"),
                    0,
                    "En Yucatán son pozos naturales de agua dulce.",
                    listOf("🕳️", "💧")
                ),
                p(
                    "El agua de un cenote puede venir de...",
                    listOf("La lluvia que se filtra por la roca", "El mar directamente", "Un grifo", "El humo"),
                    0,
                    "La roca caliza tiene grietas por donde pasa la lluvia.",
                    listOf("🌧️", "🪨", "💧")
                ),
                p(
                    "¿Por qué hay que cuidar los cenotes?",
                    listOf("Son hogar de seres vivos y fuente de agua", "Son de plástico", "No tienen agua", "Solo son decoración"),
                    0,
                    "Lo que tiremos en la superficie puede llegar al agua subterránea.",
                    listOf("🕳️", "🐠")
                )
            )
        ),
        leccion(
            numero = 18,
            titulo = "Terremotos y placas",
            subtitulo = "La corteza terrestre también se mueve.",
            icono = "🌎",
            conceptos = listOf("sismo", "placas", "prevención"),
            teoria = listOf(
                "La corteza de la Tierra está partida en placas que se mueven lentamente.",
                "Cuando esas placas se atoran y luego se sueltan, la energía viaja como un sismo."
            ),
            preguntas = listOf(
                p(
                    "¿Qué causa un terremoto?",
                    listOf("El movimiento de las placas de la Tierra", "El viento en las nubes", "La lluvia", "El canto de los pájaros"),
                    0,
                    "Las placas empujan, se atoran y liberan energía.",
                    listOf("🌎", "📳")
                ),
                p(
                    "¿Qué es una placa tectónica?",
                    listOf("Un pedazo grande de la corteza terrestre", "Una nube", "Un tipo de árbol", "Un imán"),
                    0,
                    "La corteza está dividida como un rompecabezas enorme.",
                    listOf("🧩", "🌎")
                ),
                p(
                    "¿Qué se recomienda durante un sismo?",
                    listOf("Mantener la calma y seguir el plan de protección civil", "Correr a la calle sin mirar", "Subir al techo", "Gritar más fuerte"),
                    0,
                    "Seguir las indicaciones y resguardarse evita accidentes.",
                    listOf("🛟", "🤝")
                )
            )
        ),
        leccion(
            numero = 19,
            titulo = "Volcanes con nombre",
            subtitulo = "Montañas que respiran fuego y ceniza.",
            icono = "🌋",
            conceptos = listOf("volcán", "lava", "ceniza"),
            teoria = listOf(
                "Un volcán es una abertura de la Tierra por donde sale magma, ceniza y gases.",
                "El Popocatépetl es un volcán activo de México y se monitorea todos los días."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál es un volcán activo de México?",
                    listOf("Popocatépetl", "Everest", "Kilimanjaro", "Fuji"),
                    0,
                    "Don Goyo está entre Puebla, Morelos y el Estado de México.",
                    listOf("🌋", "🇲🇽")
                ),
                p(
                    "¿Qué sale de un volcán cuando entra en erupción?",
                    listOf("Lava, ceniza y gases", "Agua dulce", "Arena de playa", "Nieve"),
                    0,
                    "El magma sube y sale como lava, ceniza y gases.",
                    listOf("🌋", "🔥")
                ),
                p(
                    "¿Por qué no hay que acercarse a un volcán activo?",
                    listOf("Hay ceniza y gases peligrosos", "Hace mucho frío", "No hay señal", "Se puede volar"),
                    0,
                    "La ceniza y los gases pueden dañar las vías respiratorias.",
                    listOf("🌋", "⚠️")
                )
            )
        ),
        leccion(
            numero = 20,
            titulo = "El Tlaloque del Agua",
            subtitulo = "Jefe del capítulo: el ciclo que nunca se detiene.",
            icono = "🌊",
            conceptos = listOf("ciclo del agua", "estados", "cenote"),
            teoria = listOf(
                "El agua cambia de estado con el calor y el frío, pero no desaparece.",
                "Los cenotes guardan agua subterránea que también forma parte del ciclo."
            ),
            preguntas = listOf(
                p(
                    "¿Qué proceso lleva el agua del mar a las nubes?",
                    listOf("Evaporación", "Filtración", "Congelación", "Magnetismo"),
                    0,
                    "El sol calienta el agua y la convierte en vapor.",
                    listOf("☀️", "💧", "☁️")
                ),
                p(
                    "¿Qué estado del agua es el hielo?",
                    listOf("Sólido", "Líquido", "Gas", "Plasma"),
                    0,
                    "El frío ordena las moléculas y las deja quietas.",
                    listOf("🧊", "❄️")
                ),
                p(
                    "¿Qué debemos hacer para cuidar el agua de un cenote?",
                    listOf("No tirar basura y respetar su entorno", "Lavar coches ahí", "Tirar aceite", "Pescar sin control"),
                    0,
                    "El agua subterránea se contamina con lo que dejamos en la superficie.",
                    listOf("🕳️", "🚯")
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 3 · ECOSISTEMAS
        // ===================================================================
        leccion(
            numero = 21,
            titulo = "¿Quién come a quién?",
            subtitulo = "Una cadena que empieza con el sol y termina en el suelo.",
            icono = "🔗",
            conceptos = listOf("cadena alimenticia", "productor", "descomponedor"),
            teoria = listOf(
                "Las plantas son productoras: fabrican su alimento con luz, agua y aire.",
                "Los descomponedores reciclan nutrientes de los seres que mueren."
            ),
            preguntas = listOf(
                p(
                    "En una cadena alimenticia, la planta es...",
                    listOf("Productora", "Consumidora", "Descomponedora", "Depredadora"),
                    0,
                    "Produce su propio alimento usando la luz del sol.",
                    listOf("🌱", "☀️")
                ),
                p(
                    "El conejo come plantas. ¿Qué es?",
                    listOf("Herbívoro", "Carnívoro", "Productor", "Hongo"),
                    0,
                    "Un herbívoro come plantas.",
                    listOf("🐇", "🌿")
                ),
                p(
                    "¿Quién devuelve nutrientes al suelo?",
                    listOf("Los descomponedores", "Los depredadores tope", "Las nubes", "Los semáforos"),
                    0,
                    "Hongos y bacterias reciclan lo que queda de los seres vivos.",
                    listOf("🍄", "🟤")
                )
            )
        ),
        leccion(
            numero = 22,
            titulo = "La red trófica del manglar",
            subtitulo = "Muchas cadenas que se cruzan en el agua salada.",
            icono = "🌿",
            conceptos = listOf("red trófica", "manglar", "consumidor"),
            teoria = listOf(
                "Una red trófica es un conjunto de cadenas alimenticias conectadas.",
                "En el manglar, las hojas, los cangrejos, los peces y las aves forman una red."
            ),
            preguntas = listOf(
                p(
                    "¿Qué es una red trófica?",
                    listOf("Varias cadenas alimenticias conectadas", "Una red de pesca", "Un árbol", "Una nube"),
                    0,
                    "Si una cadena se cruza con otra, se forma una red.",
                    listOf("🕸️", "🌿")
                ),
                p(
                    "En un manglar, los cangrejos comen hojas y los peces comen cangrejos. ¿Quiénes son consumidores?",
                    listOf("El cangrejo y el pez", "La hoja", "El sol", "El agua"),
                    0,
                    "Los consumidores comen a otros seres vivos.",
                    listOf("🦀", "🐟")
                ),
                p(
                    "¿Qué pasa si desaparecen los manglares?",
                    listOf("Se pierden refugios y crías de muchos animales", "Llueve más dulce", "Los peces vuelan", "Nada cambia"),
                    0,
                    "El manglar es guardería y casa para muchas especies.",
                    listOf("🌿", "🐠")
                )
            )
        ),
        leccion(
            numero = 23,
            titulo = "Camuflaje de hoja",
            subtitulo = "Parecer otra cosa también es una estrategia.",
            icono = "🍃",
            conceptos = listOf("camuflaje", "supervivencia", "color"),
            teoria = listOf(
                "El camuflaje permite esconderse de depredadores o acechar sin ser visto.",
                "Algunos insectos imitan hojas, ramas o cortezas para confundirse con el ambiente."
            ),
            preguntas = listOf(
                p(
                    "¿Para qué sirve el camuflaje?",
                    listOf("Para esconderse de depredadores o acechar", "Para volar", "Para cantar", "Para hacer miel"),
                    0,
                    "Parecerse al entorno da ventaja para sobrevivir.",
                    listOf("🍃", "👀")
                ),
                p(
                    "Un insecto parece una hoja seca. ¿Qué ventaja tiene?",
                    listOf("Se confunde con el ambiente", "Se vuelve más rápido", "Cambia de color cada minuto", "Habla con las plantas"),
                    0,
                    "Si no lo ven, no se lo comen.",
                    listOf("🦗", "🍂")
                ),
                p(
                    "El jaguar tiene manchas que lo ayudan a...",
                    listOf("Confundirse entre sombras", "Nadar", "Volar", "Hacer ruido"),
                    0,
                    "Sus rosetas rompen su silueta entre la vegetación.",
                    listOf("🐆", "🌳")
                )
            )
        ),
        leccion(
            numero = 24,
            titulo = "El color que advierte",
            subtitulo = "Rojo, amarillo y negro también dicen «no me comas».",
            icono = "🎨",
            conceptos = listOf("color de advertencia", "mimetismo", "toxina"),
            teoria = listOf(
                "Los colores brillantes pueden advertir que un animal es venenoso o de mal sabor.",
                "El mimetismo es parecerse a un animal peligroso para espantar depredadores."
            ),
            preguntas = listOf(
                p(
                    "La mariposa monarca es naranja y negra. ¿Qué avisa ese color?",
                    listOf("Que puede ser tóxica", "Que es comestible", "Que tiene frío", "Que es invisible"),
                    0,
                    "Las monarca acumulan toxinas de la planta que comen.",
                    listOf("🦋", "⚠️")
                ),
                p(
                    "¿Cómo se llama el parecido de un animal inofensivo con uno peligroso?",
                    listOf("Mimetismo", "Migración", "Fotosíntesis", "Erosión"),
                    0,
                    "Imitar la señal de peligro también protege.",
                    listOf("🐍", "🎭")
                ),
                p(
                    "Una rana de colores brillantes probablemente...",
                    listOf("Avisa que es venenosa", "Quiere jugar", "Tiene hambre de pasto", "Duerme"),
                    0,
                    "En la naturaleza, los colores fuertes suelen ser una advertencia.",
                    listOf("🐸", "🌈")
                )
            )
        ),
        leccion(
            numero = 25,
            titulo = "Semillas que viajan",
            subtitulo = "Viento, agua y animales reparten la próxima generación.",
            icono = "🌰",
            conceptos = listOf("dispersión", "semilla", "viento"),
            teoria = listOf(
                "Las semillas viajan para encontrar espacio, agua y luz lejos de la planta madre.",
                "El viento, el agua y los animales son sus principales transportes."
            ),
            preguntas = listOf(
                p(
                    "¿Cómo viaja una semilla de diente de león?",
                    listOf("Con el viento", "Con un autobús", "Nadando", "Cantando"),
                    0,
                    "Sus pelitos funcionan como un paracaídas.",
                    listOf("🌬️", "🌼")
                ),
                p(
                    "Un coco flota y llega a otra playa. ¿Qué dispersión es?",
                    listOf("Por agua", "Por viento", "Por animal", "Por fuego"),
                    0,
                    "El coco flota y el mar lo lleva a otra costa.",
                    listOf("🥥", "🌊")
                ),
                p(
                    "¿Por qué las semillas necesitan viajar?",
                    listOf("Para encontrar espacio y no competir con la planta madre", "Para ir de vacaciones", "Para cambiar de color", "Para hacer ruido"),
                    0,
                    "Más lejos hay menos competencia por luz y agua.",
                    listOf("🌱", "🌎")
                )
            )
        ),
        leccion(
            numero = 26,
            titulo = "El bosque de niebla",
            subtitulo = "Un bosque que atrapa agua del aire.",
            icono = "🌫️",
            conceptos = listOf("bosque de niebla", "humedad", "captación"),
            teoria = listOf(
                "El bosque de niebla vive entre nubes y tiene mucha humedad.",
                "Sus árboles captan agua de la niebla y la sueltan poco a poco al suelo."
            ),
            preguntas = listOf(
                p(
                    "¿Qué caracteriza al bosque de niebla?",
                    listOf("Mucha humedad y nubes entre los árboles", "Arena y sol intenso", "Hielo todo el año", "Agua salada"),
                    0,
                    "La niebla se queda entre las copas de los árboles.",
                    listOf("🌫️", "🌳")
                ),
                p(
                    "¿Por qué es importante conservar el bosque de niebla?",
                    listOf("Capta agua para muchas comunidades", "Produce plástico", "No tiene animales", "Solo sirve para leña"),
                    0,
                    "Sus árboles funcionan como torres que recogen agua del aire.",
                    listOf("💧", "🏘️")
                ),
                p(
                    "Un árbol del bosque de niebla tiene raíces que...",
                    listOf("Sostienen el suelo y guardan agua", "Vuelan", "Hacen fuego", "Cantan"),
                    0,
                    "Las raíces sujetan la tierra y ayudan a infiltrar el agua.",
                    listOf("🌳", "🟤")
                )
            )
        ),
        leccion(
            numero = 27,
            titulo = "El desierto que florece",
            subtitulo = "La vida espera la lluvia con paciencia.",
            icono = "🌵",
            conceptos = listOf("desierto", "adaptación", "floración"),
            teoria = listOf(
                "Después de una lluvia, muchas semillas del desierto germinan y el paisaje florece.",
                "Las plantas guardan agua en tallos y raíces para resistir la sequía."
            ),
            preguntas = listOf(
                p(
                    "¿Cuándo florece el desierto?",
                    listOf("Después de las lluvias", "Cada lunes", "Cuando nieva", "Solo de noche"),
                    0,
                    "Las semillas esperan años a que caiga suficiente agua.",
                    listOf("🌧️", "🌼")
                ),
                p(
                    "Las plantas del desierto guardan agua en...",
                    listOf("Tallos y raíces", "Hojas de plástico", "Flores de vidrio", "Nubes"),
                    0,
                    "Sus tallos carnosos y raíces profundas almacenan agua.",
                    listOf("🌵", "💧")
                ),
                p(
                    "¿Qué animal del desierto puede pasar mucho tiempo sin beber?",
                    listOf("El camello", "El delfín", "El pingüino", "El ajolote"),
                    0,
                    "El camello guarda reservas y pierde poca agua.",
                    listOf("🐪", "☀️")
                )
            )
        ),
        leccion(
            numero = 28,
            titulo = "Manglares que abrazan",
            subtitulo = "Raíces que protegen la costa y crían peces.",
            icono = "🌊",
            conceptos = listOf("manglar", "costa", "erosión"),
            teoria = listOf(
                "Las raíces del manglar frenan las olas y protegen la costa de la erosión.",
                "Muchos peces y cangrejos crecen entre sus raíces, a salvo."
            ),
            preguntas = listOf(
                p(
                    "¿Qué protegen los manglares en la costa?",
                    listOf("La costa de la erosión y las olas fuertes", "Las ciudades del viento", "Los volcanes", "Los desiertos"),
                    0,
                    "Sus raíces forman una barrera natural contra las olas.",
                    listOf("🌿", "🌊")
                ),
                p(
                    "¿Qué animal usa el manglar como guardería?",
                    listOf("Peces y cangrejos", "Camellos", "Pingüinos", "Leones"),
                    0,
                    "Las crías encuentran refugio entre las raíces.",
                    listOf("🐟", "🦀")
                ),
                p(
                    "¿Cómo cuidamos un manglar?",
                    listOf("No rellenarlo ni tirar basura", "Construir sobre él", "Cortar sus raíces", "Secarlo"),
                    0,
                    "El manglar necesita agua, suelo y raíces intactas.",
                    listOf("🌿", "🚯")
                )
            )
        ),
        leccion(
            numero = 29,
            titulo = "El jaguar y su territorio",
            subtitulo = "El depredador tope también cuida el equilibrio.",
            icono = "🐾",
            conceptos = listOf("depredador tope", "territorio", "equilibrio"),
            teoria = listOf(
                "Un depredador tope está al final de la cadena y regula a los animales que come.",
                "El jaguar necesita territorios grandes con presas, agua y refugio."
            ),
            preguntas = listOf(
                p(
                    "Un depredador tope es...",
                    listOf("El que está al final de la cadena y regula a otros animales", "El que come plantas", "El que hace fotosíntesis", "El que vive en una jaula"),
                    0,
                    "Su presencia mantiene el equilibrio del ecosistema.",
                    listOf("🐆", "⚖️")
                ),
                p(
                    "¿Qué necesita un jaguar para vivir?",
                    listOf("Territorio grande, presas y agua", "Solo un árbol pequeño", "Plástico", "Ruido de la ciudad"),
                    0,
                    "Sin espacio ni presas no puede sobrevivir.",
                    listOf("🐆", "🌳")
                ),
                p(
                    "Si desaparece el jaguar, la cadena alimenticia...",
                    listOf("Puede desequilibrarse", "Se vuelve más fuerte", "No cambia nunca", "Se hace de plástico"),
                    0,
                    "Al faltar el depredador tope, algunas poblaciones crecen demasiado.",
                    listOf("🐆", "🔗")
                )
            )
        ),
        leccion(
            numero = 30,
            titulo = "El Jaguar de las Sombras",
            subtitulo = "Jefe del capítulo: la red de la vida se sostiene entre todos.",
            icono = "🌑",
            conceptos = listOf("red trófica", "camuflaje", "descomponedor"),
            teoria = listOf(
                "En un ecosistema, productores, consumidores y descomponedores dependen entre sí.",
                "Perder una pieza de la red puede afectar a muchas otras."
            ),
            preguntas = listOf(
                p(
                    "¿Quién produce su propio alimento en un ecosistema?",
                    listOf("Las plantas", "Los jaguares", "Los hongos", "Los peces"),
                    0,
                    "Las plantas usan la luz para fabricar su alimento.",
                    listOf("🌱", "☀️")
                ),
                p(
                    "El camuflaje ayuda a los animales a...",
                    listOf("Esconderse y sobrevivir", "Hacer música", "Contar estrellas", "Nadar más rápido"),
                    0,
                    "Confundirse con el ambiente puede salvarles la vida.",
                    listOf("🐆", "🍃")
                ),
                p(
                    "¿Qué hace un descomponedor?",
                    listOf("Recicla nutrientes de seres muertos", "Caza jaguares", "Produce luz", "Hace nubes"),
                    0,
                    "Hongos y bacterias devuelven nutrientes al suelo.",
                    listOf("🍄", "🟤")
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 4 · FÍSICA Y QUÍMICA
        // ===================================================================
        leccion(
            numero = 31,
            titulo = "La gravedad que jala",
            subtitulo = "La fuerza invisible que te mantiene en el suelo.",
            icono = "🍎",
            conceptos = listOf("gravedad", "fuerza", "órbita"),
            teoria = listOf(
                "La gravedad es una fuerza que atrae los objetos con masa hacia otros objetos con masa.",
                "En la Tierra nos jala hacia el centro; en el espacio mantiene a la Luna en órbita."
            ),
            preguntas = listOf(
                p(
                    "¿Qué hace la gravedad?",
                    listOf("Atrae los objetos hacia la Tierra", "Los hace flotar hacia arriba", "Los convierte en gas", "Los vuelve imanes"),
                    0,
                    "Por eso una piedra suelta cae al suelo.",
                    listOf("🍎", "⬇️")
                ),
                p(
                    "Si sueltas una piedra, ¿qué pasa?",
                    listOf("Cae al suelo", "Se queda flotando", "Sube al cielo", "Se vuelve arena"),
                    0,
                    "La gravedad la jala hacia el centro de la Tierra.",
                    listOf("🪨", "⬇️")
                ),
                p(
                    "¿Por qué la Luna no se cae sobre la Tierra?",
                    listOf("Porque se mueve alrededor y la gravedad la mantiene en órbita", "Porque es de plástico", "Porque no existe", "Porque flota en el agua"),
                    0,
                    "Se mueve de lado mientras la gravedad la jala: eso forma una órbita.",
                    listOf("🌙", "🌎")
                )
            )
        ),
        leccion(
            numero = 32,
            titulo = "El imán que atrae",
            subtitulo = "Una fuerza que actúa a distancia.",
            icono = "🧲",
            conceptos = listOf("imán", "magnetismo", "metal"),
            teoria = listOf(
                "Un imán atrae objetos de hierro, níquel y cobalto.",
                "El magnetismo es una fuerza que puede actuar sin tocar el objeto."
            ),
            preguntas = listOf(
                reaccion(
                    enunciado = "Arrastra el IMÁN hasta el METAL.",
                    opciones = listOf("El metal se pega al imán", "El metal se convierte en agua", "El imán se hace de papel", "No pasa nada"),
                    correcta = 0,
                    pista = "El imán atrae al metal aunque no lo toque.",
                    elementos = listOf(ElementoCientifico.IMAN, ElementoCientifico.METAL, ElementoCientifico.PLASTICO),
                    resultado = ElementoCientifico.METAL_ATRAIDO
                ),
                p(
                    "¿Qué material atrae un imán?",
                    listOf("El hierro", "El plástico", "La madera", "El vidrio"),
                    0,
                    "El hierro es un material magnético.",
                    listOf("🧲", "🔩")
                ),
                p(
                    "¿Qué pasa si acercas dos imanes por el mismo polo?",
                    listOf("Se rechazan", "Se abrazan", "Se derriten", "Se vuelven plantas"),
                    0,
                    "Polos iguales se rechazan; polos opuestos se atraen.",
                    listOf("🧲", "↔️")
                )
            )
        ),
        leccion(
            numero = 33,
            titulo = "Polos que se buscan",
            subtitulo = "Norte, sur y una brújula que siempre apunta.",
            icono = "🧭",
            conceptos = listOf("polos", "brújula", "campo magnético"),
            teoria = listOf(
                "Todo imán tiene un polo norte y un polo sur.",
                "Los polos opuestos se atraen y los polos iguales se rechazan."
            ),
            preguntas = listOf(
                p(
                    "Los polos opuestos de dos imanes...",
                    listOf("Se atraen", "Se rechazan", "Se ignoran", "Se convierten en agua"),
                    0,
                    "Norte y sur se buscan.",
                    listOf("🧲", "➕")
                ),
                p(
                    "Los polos iguales...",
                    listOf("Se rechazan", "Se atraen", "Se derriten", "Cantan"),
                    0,
                    "Dos nortes juntos se empujan.",
                    listOf("🧲", "➖")
                ),
                p(
                    "La Tierra tiene un campo magnético. ¿Para qué sirve la brújula?",
                    listOf("Para orientarse con el norte magnético", "Para hacer fuego", "Para medir la lluvia", "Para cocinar"),
                    0,
                    "La aguja imantada se alinea con el campo de la Tierra.",
                    listOf("🧭", "🌎")
                )
            )
        ),
        leccion(
            numero = 34,
            titulo = "Fuerzas en el columpio",
            subtitulo = "Empujar, jalar y encontrar el equilibrio.",
            icono = "🛝",
            conceptos = listOf("fuerza", "equilibrio", "movimiento"),
            teoria = listOf(
                "Una fuerza es un empujón o un jalón que puede mover, frenar o deformar un objeto.",
                "Cuando dos fuerzas iguales van en sentidos opuestos, el objeto queda en equilibrio."
            ),
            preguntas = listOf(
                p(
                    "Empujar y jalar son...",
                    listOf("Fuerzas", "Nubes", "Reacciones químicas", "Semillas"),
                    0,
                    "Las fuerzas cambian el movimiento de los objetos.",
                    listOf("💪", "➡️")
                ),
                p(
                    "Para que un columpio se mueva, necesitas...",
                    listOf("Una fuerza", "Solo mirarlo", "Agua", "Un imán"),
                    0,
                    "Alguien tiene que empujarlo o jalarlo.",
                    listOf("🛝", "💪")
                ),
                p(
                    "Si dos fuerzas iguales van en sentidos opuestos, el objeto...",
                    listOf("Se queda en equilibrio", "Se mueve al doble", "Desaparece", "Se vuelve gas"),
                    0,
                    "Las fuerzas se cancelan y el objeto no se mueve.",
                    listOf("⚖️", "🪨")
                )
            )
        ),
        leccion(
            numero = 35,
            titulo = "Mezclas de cocina",
            subtitulo = "Azúcar, aceite y arena: juntos pero no revueltos.",
            icono = "🥣",
            conceptos = listOf("mezcla", "disolución", "separación"),
            teoria = listOf(
                "Una mezcla junta dos o más materiales sin cambiar su naturaleza.",
                "Algunas mezclas se separan con filtros, imanes o dejándolas reposar."
            ),
            preguntas = listOf(
                p(
                    "Agua con azúcar es una...",
                    listOf("Mezcla", "Reacción nuclear", "Roca", "Nube"),
                    0,
                    "El azúcar se disuelve, pero sigue siendo azúcar y agua.",
                    listOf("💧", "🍬")
                ),
                p(
                    "Si mezclas agua y aceite, ¿qué pasa?",
                    listOf("Se separan en capas", "Se vuelven piedra", "Se congelan al instante", "Se convierten en gas"),
                    0,
                    "El aceite no se mezcla con el agua y flota.",
                    listOf("💧", "🫒")
                ),
                p(
                    "¿Cómo separarías arena del agua?",
                    listOf("Con un filtro", "Con un imán", "Con una lupa", "Con música"),
                    0,
                    "El filtro atrapa la arena y deja pasar el agua.",
                    listOf("🏖️", "🧵")
                )
            )
        ),
        leccion(
            numero = 36,
            titulo = "Reacciones con burbujas",
            subtitulo = "Cuando dos materiales crean algo nuevo.",
            icono = "🫧",
            conceptos = listOf("reacción química", "gas", "bicarbonato"),
            teoria = listOf(
                "En una reacción química, las sustancias se transforman en otras nuevas.",
                "El bicarbonato con vinagre produce dióxido de carbono: por eso burbujea."
            ),
            preguntas = listOf(
                reaccion(
                    enunciado = "Arrastra el BICARBONATO hasta el VINAGRE.",
                    opciones = listOf("Se forman burbujas de gas", "Se congela", "Se vuelve imán", "Nada"),
                    correcta = 0,
                    pista = "La mezcla suelta un gas y hace burbujas.",
                    elementos = listOf(ElementoCientifico.BICARBONATO, ElementoCientifico.VINAGRE, ElementoCientifico.AGUA),
                    resultado = ElementoCientifico.BURBUJAS
                ),
                p(
                    "En la reacción de bicarbonato con vinagre se produce...",
                    listOf("Dióxido de carbono", "Oro", "Agua salada", "Arena"),
                    0,
                    "El gas que sale es dióxido de carbono.",
                    listOf("🫧", "💨")
                ),
                p(
                    "Una reacción química cambia...",
                    listOf("Las sustancias y forma otras nuevas", "Solo el color de la mesa", "La gravedad", "El nombre del niño"),
                    0,
                    "Los materiales iniciales se transforman en productos distintos.",
                    listOf("🧪", "✨")
                )
            )
        ),
        leccion(
            numero = 37,
            titulo = "El calor que cambia",
            subtitulo = "La energía que viaja de lo caliente a lo frío.",
            icono = "🔥",
            conceptos = listOf("calor", "conducción", "cambio de estado"),
            teoria = listOf(
                "El calor es energía que se transfiere de un cuerpo caliente a uno más frío.",
                "El calor puede cambiar el estado de un material, como derretir hielo."
            ),
            preguntas = listOf(
                reaccion(
                    enunciado = "Arrastra el CALOR hasta el HIELO.",
                    opciones = listOf("El hielo se derrite", "El hielo se hace más grande", "La arena se vuelve imán", "Nada"),
                    correcta = 0,
                    pista = "El calor derrite el hielo y lo convierte en agua.",
                    elementos = listOf(ElementoCientifico.CALOR, ElementoCientifico.HIELO, ElementoCientifico.TIERRA),
                    resultado = ElementoCientifico.AGUA
                ),
                p(
                    "El calor pasa de un objeto...",
                    listOf("Caliente a uno frío", "Frío a uno caliente", "No se mueve", "Solo por los imanes"),
                    0,
                    "La energía térmica fluye del más caliente al más frío.",
                    listOf("🔥", "❄️")
                ),
                p(
                    "¿Qué material conduce mejor el calor?",
                    listOf("El metal", "La madera", "El plástico", "El aire"),
                    0,
                    "Los metales son buenos conductores térmicos.",
                    listOf("🔩", "🔥")
                )
            )
        ),
        leccion(
            numero = 38,
            titulo = "La luz que rebota",
            subtitulo = "Espejos, sombras y colores que llegan a tus ojos.",
            icono = "🪞",
            conceptos = listOf("luz", "reflexión", "sombra"),
            teoria = listOf(
                "La luz viaja en línea recta y rebota en las superficies: eso es la reflexión.",
                "Vemos un objeto cuando la luz rebota en él y llega a nuestros ojos."
            ),
            preguntas = listOf(
                p(
                    "Cuando la luz choca con un espejo...",
                    listOf("Se refleja", "Se convierte en sonido", "Se hace agua", "Desaparece"),
                    0,
                    "El espejo cambia la dirección de la luz.",
                    listOf("🪞", "✨")
                ),
                p(
                    "¿Por qué vemos un objeto?",
                    listOf("Porque la luz rebota en él y llega a nuestros ojos", "Porque el objeto nos habla", "Porque tiene imanes", "Porque es de noche"),
                    0,
                    "Sin luz que llegue a los ojos, no hay imagen.",
                    listOf("👀", "💡")
                ),
                p(
                    "Un espejo cóncavo puede...",
                    listOf("Concentrar la luz", "Hacer llover", "Crear gravedad", "Convertir agua en hielo"),
                    0,
                    "Su forma curva junta los rayos de luz.",
                    listOf("🪞", "🔍")
                )
            )
        ),
        leccion(
            numero = 39,
            titulo = "El sonido que vibra",
            subtitulo = "Todo sonido nace de algo que se mueve.",
            icono = "🥁",
            conceptos = listOf("sonido", "vibración", "ondas"),
            teoria = listOf(
                "El sonido se produce cuando un objeto vibra y empuja el aire.",
                "Las vibraciones viajan como ondas y llegan a nuestros oídos."
            ),
            preguntas = listOf(
                p(
                    "El sonido se produce cuando un objeto...",
                    listOf("Vibra", "Se congela", "Se vuelve imán", "Se moja"),
                    0,
                    "La vibración empuja el aire y crea ondas sonoras.",
                    listOf("🥁", "〰️")
                ),
                p(
                    "El sonido viaja mejor en...",
                    listOf("El agua que en el aire", "El vacío", "La nada", "El silencio absoluto"),
                    0,
                    "Las partículas del agua transmiten la vibración más rápido.",
                    listOf("🌊", "🔊")
                ),
                p(
                    "Si tocas un tambor, sientes...",
                    listOf("Vibraciones", "Frío", "Imanes", "Semillas"),
                    0,
                    "La piel del tambor vibra y produce sonido.",
                    listOf("🥁", "🫨")
                )
            )
        ),
        leccion(
            numero = 40,
            titulo = "La Alebrije Magnética",
            subtitulo = "Jefe del capítulo: fuerzas y reacciones en acción.",
            icono = "🦄",
            conceptos = listOf("magnetismo", "gravedad", "reacción"),
            teoria = listOf(
                "Las fuerzas pueden actuar a distancia, como la gravedad y el magnetismo.",
                "Las reacciones químicas transforman materiales y producen cosas nuevas."
            ),
            preguntas = listOf(
                p(
                    "¿Qué fuerza atrae el metal?",
                    listOf("El magnetismo", "La gravedad", "La fotosíntesis", "La evaporación"),
                    0,
                    "Los imanes atraen metales como el hierro.",
                    listOf("🧲", "🔩")
                ),
                p(
                    "La gravedad hace que los objetos...",
                    listOf("Caigan hacia la Tierra", "Floten sin razón", "Se vuelvan plantas", "Canten"),
                    0,
                    "La Tierra jala los objetos hacia su centro.",
                    listOf("🌎", "⬇️")
                ),
                p(
                    "Mezclar bicarbonato y vinagre produce...",
                    listOf("Una reacción con burbujas", "Hielo", "Un imán", "Una nube"),
                    0,
                    "Se forma dióxido de carbono y aparecen burbujas.",
                    listOf("🧂", "🍶", "🫧")
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 5 · SUSTENTABILIDAD
        // ===================================================================
        leccion(
            numero = 41,
            titulo = "Basura con dos destinos",
            subtitulo = "Orgánico e inorgánico: cada residuo tiene su lugar.",
            icono = "🗑️",
            conceptos = listOf("orgánico", "inorgánico", "separación"),
            teoria = listOf(
                "Los residuos orgánicos vienen de seres vivos y se pueden convertir en composta.",
                "Los inorgánicos, como el plástico y el vidrio, se reciclan o se reutilizan."
            ),
            preguntas = listOf(
                p(
                    "¿A qué bote va la cáscara de plátano?",
                    listOf("Orgánico", "Inorgánico", "Tóxico", "Vidrio"),
                    0,
                    "Los restos de comida se convierten en composta.",
                    listOf("🍌", "🌱")
                ),
                p(
                    "¿A qué bote va una botella de plástico?",
                    listOf("Inorgánico", "Orgánico", "Tóxico", "Composta"),
                    0,
                    "El plástico no se deshace solo: se separa para reciclarlo.",
                    listOf("🧴", "♻️")
                ),
                p(
                    "¿Dónde va una pila usada?",
                    listOf("Tóxico", "Orgánico", "Inorgánico", "Juguetes"),
                    0,
                    "Las pilas tienen metales peligrosos y necesitan un contenedor especial.",
                    listOf("🔋", "☠️")
                )
            )
        ),
        leccion(
            numero = 42,
            titulo = "El plástico que no se va",
            subtitulo = "Un material útil que se queda demasiado tiempo.",
            icono = "🧴",
            conceptos = listOf("plástico", "contaminación", "reutilizar"),
            teoria = listOf(
                "Una botella de plástico puede tardar cientos de años en descomponerse.",
                "En el mar, los animales confunden el plástico con comida."
            ),
            preguntas = listOf(
                p(
                    "¿Cuánto puede tardar una botella de plástico en descomponerse?",
                    listOf("Cientos de años", "Una semana", "Un día", "Una hora"),
                    0,
                    "El plástico es resistente y por eso dura muchísimo.",
                    listOf("🧴", "⏳")
                ),
                p(
                    "¿Qué daño hace el plástico en el mar?",
                    listOf("Los animales lo confunden con comida", "Los ayuda a nadar", "Los vuelve más fuertes", "Nada"),
                    0,
                    "Tortugas y peces pueden comerlo y enfermar.",
                    listOf("🐢", "🥤")
                ),
                p(
                    "¿Cuál es una mejor opción que una bolsa de plástico?",
                    listOf("Una bolsa de tela reutilizable", "Más plástico", "Tirarla al río", "Quemarla"),
                    0,
                    "Una bolsa de tela se usa muchas veces y no contamina igual.",
                    listOf("👜", "🌎")
                )
            )
        ),
        leccion(
            numero = 43,
            titulo = "Las tres R",
            subtitulo = "Reducir, reutilizar y reciclar en ese orden.",
            icono = "♻️",
            conceptos = listOf("reducir", "reutilizar", "reciclar"),
            teoria = listOf(
                "Reducir es consumir menos de lo que no necesitamos.",
                "Reutilizar es darle otro uso a algo; reciclar es transformarlo en un material nuevo."
            ),
            preguntas = listOf(
                p(
                    "Las tres R son...",
                    listOf("Reducir, reutilizar y reciclar", "Rápido, ruidoso y raro", "Río, roca y rana", "Rojo, rosa y rueda"),
                    0,
                    "Primero reducir, luego reutilizar y al final reciclar.",
                    listOf("♻️", "3️⃣")
                ),
                p(
                    "Reutilizar significa...",
                    listOf("Usar otra vez algo antes de tirarlo", "Comprar más", "Romperlo", "Quemarlo"),
                    0,
                    "Un frasco puede convertirse en lapicera.",
                    listOf("🫙", "✏️")
                ),
                p(
                    "Reducir significa...",
                    listOf("Consumir menos de lo que no necesitas", "Usar más bolsas", "Tirar todo", "Comprar doble"),
                    0,
                    "Menos consumo significa menos basura desde el inicio.",
                    listOf("🛒", "⬇️")
                )
            )
        ),
        leccion(
            numero = 44,
            titulo = "El filtro del cenote",
            subtitulo = "Capas que atrapan la suciedad del agua.",
            icono = "⏳",
            conceptos = listOf("filtración", "capas", "agua limpia"),
            teoria = listOf(
                "Un filtro de capas deja pasar el agua y atrapa partículas de distinto tamaño.",
                "Primero va lo más grande y al final lo más fino: grava, arena, carbón y algodón."
            ),
            preguntas = listOf(
                filtro(
                    enunciado = "Arrastra las capas al embudo: arriba lo más grande, abajo lo más fino.",
                    pista = "La grava atrapa lo grande y el algodón detiene lo más fino."
                ),
                filtro(
                    enunciado = "El agua sucia pasa por las capas. ¿Qué orden la deja más limpia?",
                    pista = "Piensa en un colador: primero lo grande y al final lo pequeño.",
                    opciones = listOf(
                        "Grava → Arena → Carbón → Algodón",
                        "Algodón → Carbón → Arena → Grava",
                        "Arena → Algodón → Grava → Carbón",
                        "Carbón → Arena → Algodón → Grava"
                    ),
                    correcta = 0
                ),
                p(
                    "¿Qué capa atrapa las piedritas más grandes?",
                    listOf("Grava", "Algodón", "Arena fina", "Carbón"),
                    0,
                    "La grava tiene huecos grandes y detiene lo primero.",
                    listOf("🪨", "💧")
                )
            )
        ),
        leccion(
            numero = 45,
            titulo = "Agua que se limpia",
            subtitulo = "Filtrar, hervir y cuidar cada gota.",
            icono = "🚰",
            conceptos = listOf("purificación", "carbón", "hervido"),
            teoria = listOf(
                "El carbón activado atrapa olores y algunas sustancias que el agua arrastra.",
                "Hervir el agua mata muchos microorganismos, pero no quita la arena."
            ),
            preguntas = listOf(
                filtro(
                    enunciado = "Arma el filtro para que el agua salga clara.",
                    pista = "Grava, arena, carbón y algodón: de lo grande a lo fino."
                ),
                p(
                    "¿Qué hace el carbón activado en un filtro?",
                    listOf("Atrapa olores y algunas sustancias", "Endulza el agua", "La congela", "La vuelve gasolina"),
                    0,
                    "Su superficie porosa atrapa impurezas.",
                    listOf("⬛", "💧")
                ),
                p(
                    "¿Por qué hervir el agua puede hacerla más segura?",
                    listOf("Mata muchos microorganismos", "Le quita la arena", "La vuelve dulce", "La hace brillar"),
                    0,
                    "El calor del hervor elimina muchos seres microscópicos.",
                    listOf("🔥", "🦠")
                )
            )
        ),
        leccion(
            numero = 46,
            titulo = "Energía del sol",
            subtitulo = "La estrella que nos da luz y electricidad.",
            icono = "☀️",
            conceptos = listOf("solar", "renovable", "electricidad"),
            teoria = listOf(
                "Los paneles solares convierten la luz del sol en electricidad.",
                "La energía solar es renovable porque el sol sigue brillando todos los días."
            ),
            preguntas = listOf(
                p(
                    "Los paneles solares convierten la luz en...",
                    listOf("Electricidad", "Agua", "Arena", "Sonido"),
                    0,
                    "Las celdas del panel transforman la luz en corriente eléctrica.",
                    listOf("☀️", "🔌")
                ),
                p(
                    "¿Por qué la energía solar es renovable?",
                    listOf("El sol sigue brillando y no se agota como el carbón", "Se acaba en un día", "Viene de una pila", "Solo funciona de noche"),
                    0,
                    "El sol es una fuente que se repone cada día.",
                    listOf("☀️", "♾️")
                ),
                p(
                    "¿Qué beneficio trae usar energía solar?",
                    listOf("Menos contaminación", "Más humo", "Más basura", "Más ruido"),
                    0,
                    "Genera electricidad sin quemar combustibles.",
                    listOf("🌎", "💚")
                )
            )
        ),
        leccion(
            numero = 47,
            titulo = "El viento que da luz",
            subtitulo = "Aerogeneradores que bailan con la brisa.",
            icono = "🌬️",
            conceptos = listOf("eólica", "renovable", "aerogenerador"),
            teoria = listOf(
                "Los aerogeneradores usan el viento para mover sus aspas y producir electricidad.",
                "El viento es una energía renovable y no se acaba al usarlo."
            ),
            preguntas = listOf(
                p(
                    "Los aerogeneradores producen electricidad con...",
                    listOf("El viento", "El agua salada", "La arena", "El humo"),
                    0,
                    "El viento empuja las aspas y hace girar un generador.",
                    listOf("🌬️", "⚡")
                ),
                p(
                    "¿Qué es una energía renovable?",
                    listOf("Una que se repone naturalmente", "Una que solo se usa una vez", "Una que sale de una pila", "Una que contamina mucho"),
                    0,
                    "El sol, el viento y el agua se reponen naturalmente.",
                    listOf("♻️", "🌎")
                ),
                p(
                    "¿Cuál de estas es renovable?",
                    listOf("El viento", "El petróleo", "El carbón", "El plástico"),
                    0,
                    "El viento no se agota como los combustibles fósiles.",
                    listOf("🌬️", "✅")
                )
            )
        ),
        leccion(
            numero = 48,
            titulo = "Cuidar el agua en casa",
            subtitulo = "Gotas pequeñas que suman un océano.",
            icono = "🚿",
            conceptos = listOf("ahorro", "agua gris", "fugas"),
            teoria = listOf(
                "Cerrar la llave y reparar fugas ahorra mucha agua cada día.",
                "El agua gris, como la de la regadera, puede reutilizarse para las plantas."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál acción ahorra agua?",
                    listOf("Cerrar la llave mientras te cepillas", "Dejar la llave abierta", "Lavar el coche todos los días", "Tirar agua al piso"),
                    0,
                    "Cada minuto con la llave cerrada ahorra litros.",
                    listOf("🚿", "💧")
                ),
                p(
                    "El agua que usamos y se puede reutilizar para las plantas se llama...",
                    listOf("Agua gris", "Agua de mar", "Agua congelada", "Agua de nube"),
                    0,
                    "El agua gris es la que ya se usó, pero no tiene químicos fuertes.",
                    listOf("🪴", "💧")
                ),
                p(
                    "Una fuga de agua en la llave...",
                    listOf("Desperdicia mucha agua", "No importa", "Limpia el aire", "Hace electricidad"),
                    0,
                    "Una gota por segundo puede desperdiciar miles de litros al año.",
                    listOf("🚰", "⚠️")
                )
            )
        ),
        leccion(
            numero = 49,
            titulo = "La huella de mis decisiones",
            subtitulo = "Consumir con cabeza también es ciencia.",
            icono = "👣",
            conceptos = listOf("consumo", "huella", "reutilizar"),
            teoria = listOf(
                "Cada objeto que compramos y tiramos deja una huella en el planeta.",
                "Llevar botella reutilizable, separar basura y apagar luces reduce esa huella."
            ),
            preguntas = listOf(
                p(
                    "¿Qué decisión cuida más el planeta?",
                    listOf("Llevar tu botella reutilizable", "Comprar botellas cada día", "Tirar basura en el río", "Dejar luces encendidas"),
                    0,
                    "Una botella reutilizable evita cientos de desechos.",
                    listOf("🍶", "🌎")
                ),
                p(
                    "Separar la basura ayuda a...",
                    listOf("Reciclar mejor", "Contaminar más", "Perder tiempo", "Hacer más basura"),
                    0,
                    "Cuando está separada, cada material puede aprovecharse.",
                    listOf("♻️", "🗑️")
                ),
                p(
                    "Si ves basura en el cenote, lo mejor es...",
                    listOf("Recogerla con un adulto y separarla", "Echarla más adentro", "Ignorarla", "Quemarla"),
                    0,
                    "Recogerla con ayuda y separarla evita que llegue al agua.",
                    listOf("🕳️", "🤝")
                )
            )
        ),
        leccion(
            numero = 50,
            titulo = "El Guardián del Cenote",
            subtitulo = "Jefe final: pequeñas decisiones, un planeta entero.",
            icono = "🛡️",
            conceptos = listOf("filtración", "renovable", "consumo"),
            teoria = listOf(
                "La ciencia nos ayuda a entender el agua, la energía y los materiales.",
                "Cuidar el planeta es juntar muchas decisiones pequeñas y constantes."
            ),
            preguntas = listOf(
                filtro(
                    enunciado = "Último reto: arma el filtro que devuelve la claridad al cenote.",
                    pista = "Grava, arena, carbón y algodón. De lo grande a lo fino."
                ),
                reaccion(
                    enunciado = "Arrastra el CALOR hasta el HIELO para derretir el último bloque.",
                    opciones = listOf("Agua líquida", "Más hielo", "Metal", "Plástico"),
                    correcta = 0,
                    pista = "El calor derrite el hielo y lo vuelve agua.",
                    elementos = listOf(ElementoCientifico.CALOR, ElementoCientifico.HIELO, ElementoCientifico.PLASTICO),
                    resultado = ElementoCientifico.AGUA
                ),
                p(
                    "¿Cuál es la mejor forma de cuidar un ecosistema?",
                    listOf("Reducir, reutilizar, reciclar y respetar a los seres vivos", "Tirar basura", "Cazar especies", "Secar el cenote"),
                    0,
                    "El cuidado combina decisiones diarias y respeto por la vida.",
                    listOf("🌎", "♻️", "💚")
                )
            )
        )
    )

    init {
        require(lecciones.size == 50) { "El currículo de Ciencias debe tener 50 lecciones" }
        require(lecciones.map { it.numero }.toSet() == (1..50).toSet()) {
            "Los números de lección deben ir del 1 al 50 sin repetirse"
        }
        require(lecciones.map { it.titulo }.distinct().size == 50) {
            "Cada lección de Ciencias necesita un título distinto"
        }
        require(lecciones.all { it.preguntas.size == 3 }) {
            "Cada lección de Ciencias necesita exactamente tres retos"
        }
    }
}
