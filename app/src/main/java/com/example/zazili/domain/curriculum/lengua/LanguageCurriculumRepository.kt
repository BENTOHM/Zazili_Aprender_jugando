package com.example.zazili.domain.curriculum.lengua

import androidx.compose.runtime.Immutable

/**
 * Currículo de Lenguaje y Literatura de Zazili 2.0.
 *
 * Cinco capítulos de diez lecciones cada uno: conciencia fonológica, sílabas
 * trabadas y ortografía, gramática, comprensión lectora con leyendas
 * mexicanas y pensamiento crítico. Cada lección trae tres retos cortos, teoría
 * en dos ideas y dificultad declarada del 1 al 5.
 */
@Immutable
data class PreguntaLengua(
    val enunciado: String,
    val opciones: List<String>,
    val indiceCorrecto: Int,
    val pista: String,
    val mecanica: MecanicaLengua,
    val audioObjetivo: String? = null
)

/** Cómo se juega cada reto dentro de la lección de Lenguaje. */
enum class MecanicaLengua {
    ATRAPA_SONIDOS,
    CODICE_ANIMADO,
    SILABAS,
    ORTOGRAFIA,
    GRAMATICA,
    LECTURA,
    CRITICO
}

/** Una lección completa del mapa de Lenguaje. */
@Immutable
data class LessonNode(
    val id: String,
    val numero: Int,
    val capitulo: CapituloLengua,
    val titulo: String,
    val subtitulo: String,
    val tier: Int,
    val icono: String,
    val conceptos: List<String>,
    val teoria: List<String>,
    val preguntas: List<PreguntaLengua>
) {
    val esJefeDeCapitulo: Boolean
        get() = numero % 10 == 0

    init {
        require(tier in 1..5) { "La dificultad de $id debe estar entre 1 y 5" }
        require(preguntas.isNotEmpty()) { "La lección $id necesita retos" }
        require(
            preguntas.all { it.indiceCorrecto in it.opciones.indices }
        ) { "La lección $id tiene una respuesta correcta fuera de rango" }
    }
}

/** Los cinco capítulos y su jefe de cierre. */
enum class CapituloLengua(
    val numero: Int,
    val titulo: String,
    val lema: String,
    val jefe: String,
    val dichoDelJefe: String
) {
    CONCIENCIA_FONOLOGICA(
        numero = 1,
        titulo = "Conciencia Fonológica",
        lema = "Vocales, sílabas y rimas para escuchar mejor.",
        jefe = "El Nahual Trabalenguas",
        dichoDelJefe = "Si me entiendes, me ganas. Si no… ¡otra vez, sin pena!"
    ),
    SILABAS_TRABADAS_ORTOGRAFIA(
        numero = 2,
        titulo = "Sílabas Trabadas y Ortografía",
        lema = "Bl, br, tl, tr… y cada letra en su lugar.",
        jefe = "El Mago de la Ortografía",
        dichoDelJefe = "Una letra cambia todo: «vaca» no es «baca». ¡A ver esa vista!"
    ),
    SINTAXIS_GRAMATICA(
        numero = 3,
        titulo = "Sintaxis y Gramática",
        lema = "Verbos, adjetivos y oraciones bien armadas.",
        jefe = "La Reina de la Gramática",
        dichoDelJefe = "Las palabras son piezas: si las acomodas bien, todo encaja."
    ),
    COMPRENSION_LECTORA(
        numero = 4,
        titulo = "Comprensión Lectora",
        lema = "Leyendas mexicanas para leer entre líneas.",
        jefe = "El Guardián de las Leyendas",
        dichoDelJefe = "No basta leer: hay que entender qué pasó y por qué."
    ),
    PENSAMIENTO_CRITICO(
        numero = 5,
        titulo = "Pensamiento Crítico",
        lema = "Metáforas, acertijos y preguntas poderosas.",
        jefe = "El Búho de las Preguntas",
        dichoDelJefe = "La respuesta más chida empieza con un «¿por qué?»."
    )
}

object LanguageCurriculumRepository {

    val capitulos: List<CapituloLengua> = CapituloLengua.entries

    /**
     * Las 50 lecciones con las respuestas ya mezcladas.
     *
     * La mezcla se hace una vez por proceso: las opciones y su
     * [PreguntaLengua.indiceCorrecto] quedan estables durante toda la sesión,
     * así que ninguna recomposición puede cambiarle las casillas al niño.
     */
    val lecciones: List<LessonNode> =
        Module2LessonValidator.mezclarLecciones(construirLecciones())

    val totalLecciones: Int
        get() = lecciones.size

    /** El nodo que dispara el diálogo "¡Continuará...!". */
    val ULTIMO_NODO_ID: String = "lengua_50"

    fun leccionPorId(id: String?): LessonNode? =
        if (id.isNullOrBlank()) null else lecciones.firstOrNull { it.id == id }

    fun leccionesDe(capitulo: CapituloLengua): List<LessonNode> =
        lecciones.filter { it.capitulo == capitulo }

    fun capituloPorNumero(numero: Int): CapituloLengua? =
        capitulos.firstOrNull { it.numero == numero }

    private fun idDe(numero: Int): String =
        "lengua_" + numero.toString().padStart(2, '0')

    private fun p(
        enunciado: String,
        opciones: List<String>,
        correcta: Int,
        pista: String,
        mecanica: MecanicaLengua,
        audio: String? = null
    ): PreguntaLengua = PreguntaLengua(
        enunciado = enunciado,
        opciones = opciones,
        indiceCorrecto = correcta,
        pista = pista,
        mecanica = mecanica,
        audioObjetivo = audio
    )

    private fun fonologia(
        numero: Int,
        titulo: String,
        subtitulo: String,
        tier: Int,
        icono: String,
        conceptos: List<String>,
        teoria: List<String>,
        preguntas: List<PreguntaLengua>
    ): LessonNode = LessonNode(
        id = idDe(numero),
        numero = numero,
        capitulo = CapituloLengua.CONCIENCIA_FONOLOGICA,
        titulo = titulo,
        subtitulo = subtitulo,
        tier = tier,
        icono = icono,
        conceptos = conceptos,
        teoria = teoria,
        preguntas = preguntas
    )

    private fun ortografia(
        numero: Int,
        titulo: String,
        subtitulo: String,
        tier: Int,
        icono: String,
        conceptos: List<String>,
        teoria: List<String>,
        preguntas: List<PreguntaLengua>
    ): LessonNode = LessonNode(
        id = idDe(numero),
        numero = numero,
        capitulo = CapituloLengua.SILABAS_TRABADAS_ORTOGRAFIA,
        titulo = titulo,
        subtitulo = subtitulo,
        tier = tier,
        icono = icono,
        conceptos = conceptos,
        teoria = teoria,
        preguntas = preguntas
    )

    private fun gramatica(
        numero: Int,
        titulo: String,
        subtitulo: String,
        tier: Int,
        icono: String,
        conceptos: List<String>,
        teoria: List<String>,
        preguntas: List<PreguntaLengua>
    ): LessonNode = LessonNode(
        id = idDe(numero),
        numero = numero,
        capitulo = CapituloLengua.SINTAXIS_GRAMATICA,
        titulo = titulo,
        subtitulo = subtitulo,
        tier = tier,
        icono = icono,
        conceptos = conceptos,
        teoria = teoria,
        preguntas = preguntas
    )

    private fun lectura(
        numero: Int,
        titulo: String,
        subtitulo: String,
        tier: Int,
        icono: String,
        conceptos: List<String>,
        teoria: List<String>,
        preguntas: List<PreguntaLengua>
    ): LessonNode = LessonNode(
        id = idDe(numero),
        numero = numero,
        capitulo = CapituloLengua.COMPRENSION_LECTORA,
        titulo = titulo,
        subtitulo = subtitulo,
        tier = tier,
        icono = icono,
        conceptos = conceptos,
        teoria = teoria,
        preguntas = preguntas
    )

    private fun critico(
        numero: Int,
        titulo: String,
        subtitulo: String,
        tier: Int,
        icono: String,
        conceptos: List<String>,
        teoria: List<String>,
        preguntas: List<PreguntaLengua>
    ): LessonNode = LessonNode(
        id = idDe(numero),
        numero = numero,
        capitulo = CapituloLengua.PENSAMIENTO_CRITICO,
        titulo = titulo,
        subtitulo = subtitulo,
        tier = tier,
        icono = icono,
        conceptos = conceptos,
        teoria = teoria,
        preguntas = preguntas
    )

    @Suppress("LongMethod")
    private fun construirLecciones(): List<LessonNode> = listOf(
        // ===================================================================
        //  CAPÍTULO 1 · CONCIENCIA FONOLÓGICA
        // ===================================================================
        fonologia(
            numero = 1,
            titulo = "La vocal que canta",
            subtitulo = "Reconoce a, e, i, o, u por su sonido.",
            tier = 1,
            icono = "🔤",
            conceptos = listOf("vocal", "sonido", "escucha"),
            teoria = listOf(
                "Las vocales son a, e, i, o, u: suenan libres, sin cerrar la boca.",
                "Escucha la vocal y toca la burbuja que tenga la misma letra."
            ),
            preguntas = listOf(
                p(
                    "Escucha con atención y atrapa la vocal que suena.",
                    listOf("a", "e", "i", "o"),
                    0,
                    "Suena abierta, como en «abeja».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "a"
                ),
                p(
                    "Escucha con atención y atrapa la vocal que suena.",
                    listOf("i", "e", "o", "u"),
                    1,
                    "Suena como al inicio de «elefante».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "e"
                ),
                p(
                    "Escucha con atención y atrapa la vocal que suena.",
                    listOf("a", "u", "o", "e"),
                    2,
                    "Suena redonda, como en «oso».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "o"
                )
            )
        ),
        fonologia(
            numero = 2,
            titulo = "El tren de las vocales",
            subtitulo = "Encuentra la vocal inicial de cada palabra.",
            tier = 1,
            icono = "🚂",
            conceptos = listOf("vocal inicial", "palabra", "sonido"),
            teoria = listOf(
                "La primera vocal de una palabra es la que suena al empezar.",
                "Di la palabra despacio y alarga el primer sonido."
            ),
            preguntas = listOf(
                p(
                    "Escucha «abeja» y atrapa su primera vocal.",
                    listOf("a", "e", "i"),
                    0,
                    "Abre la boca al inicio: «a-be-ja».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "abeja"
                ),
                p(
                    "Escucha «elefante» y atrapa su primera vocal.",
                    listOf("a", "e", "o"),
                    1,
                    "Empieza con el sonido de «elote».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "elefante"
                ),
                p(
                    "Escucha «uva» y atrapa su primera vocal.",
                    listOf("i", "o", "u"),
                    2,
                    "Los labios se hacen redonditos al inicio.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "uva"
                )
            )
        ),
        fonologia(
            numero = 3,
            titulo = "Palmadas de sílaba",
            subtitulo = "Separa las palabras en golpes de voz.",
            tier = 1,
            icono = "👏",
            conceptos = listOf("sílaba", "palmada", "ma"),
            teoria = listOf(
                "Una sílaba es cada golpe de voz de una palabra.",
                "Aplaude mientras dices «ma-má»: son dos golpes."
            ),
            preguntas = listOf(
                p(
                    "Escucha «mamá» y atrapa la primera sílaba.",
                    listOf("ma", "pa", "sa", "la"),
                    0,
                    "Los labios se juntan para decir «ma».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "mamá"
                ),
                p(
                    "Escucha «papá» y atrapa la primera sílaba.",
                    listOf("ma", "pa", "sa", "la"),
                    1,
                    "Suena como una palmada con la «p».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "papá"
                ),
                p(
                    "Escucha «sapo» y atrapa la primera sílaba.",
                    listOf("ma", "pa", "sa", "la"),
                    2,
                    "Empieza con el sonido de la «s» de sol.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "sapo"
                )
            )
        ),
        fonologia(
            numero = 4,
            titulo = "La sílaba perdida",
            subtitulo = "Completa palabras con la, le, li, lo, lu.",
            tier = 2,
            icono = "🧩",
            conceptos = listOf("sílaba", "la le li", "completar"),
            teoria = listOf(
                "La sílaba «l» cambia según la vocal que la acompaña: la, le, li, lo, lu.",
                "Escucha la palabra completa y busca la sílaba que le toca."
            ),
            preguntas = listOf(
                p(
                    "Escucha «luna» y atrapa su primera sílaba.",
                    listOf("la", "le", "lu", "li"),
                    2,
                    "Suena con la «u»: lu-na.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "luna"
                ),
                p(
                    "Escucha «limón» y atrapa su primera sílaba.",
                    listOf("la", "le", "li", "lo"),
                    2,
                    "La «i» estira la boca: li-món.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "limón"
                ),
                p(
                    "Escucha «lápiz» y atrapa su primera sílaba.",
                    listOf("la", "le", "li", "lu"),
                    0,
                    "Es la primera sílaba de «lata».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "lápiz"
                )
            )
        ),
        fonologia(
            numero = 5,
            titulo = "Rimas de la abuela",
            subtitulo = "Palabras que terminan con sonidos parecidos.",
            tier = 2,
            icono = "🎵",
            conceptos = listOf("rima", "final", "sonido"),
            teoria = listOf(
                "Dos palabras riman cuando terminan con un sonido muy parecido.",
                "Escucha el final: «sol» y «col» suenan igualito al último."
            ),
            preguntas = listOf(
                p(
                    "Escucha «sol» y atrapa la palabra que rima.",
                    listOf("col", "luna", "estrella", "mesa"),
                    0,
                    "«Sol» y «col» terminan con el mismo sonido.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "sol"
                ),
                p(
                    "Escucha «ratón» y atrapa la palabra que rima.",
                    listOf("camión", "gato", "casa", "perro"),
                    0,
                    "Los dos terminan en «ón».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "ratón"
                ),
                p(
                    "Escucha «estrella» y atrapa la palabra que rima.",
                    listOf("botella", "cielo", "nube", "sol"),
                    0,
                    "«Estrella» y «botella» terminan en «ella».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "estrella"
                )
            )
        ),
        fonologia(
            numero = 6,
            titulo = "El sonido inicial",
            subtitulo = "Descubre con qué sonido empieza cada palabra.",
            tier = 2,
            icono = "🎯",
            conceptos = listOf("inicio", "sílaba", "escucha"),
            teoria = listOf(
                "El sonido inicial es el primer golpe de voz de una palabra.",
                "Tapa los ojos un segundo y escucha solo el principio."
            ),
            preguntas = listOf(
                p(
                    "Escucha «mariposa» y atrapa su primera sílaba.",
                    listOf("ma", "pa", "ra"),
                    0,
                    "Los labios se cierran al empezar: ma-ri-po-sa.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "mariposa"
                ),
                p(
                    "Escucha «tortilla» y atrapa su primera sílaba.",
                    listOf("tar", "tor", "til"),
                    1,
                    "La lengua toca el techo: tor-ti-lla.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "tortilla"
                ),
                p(
                    "Escucha «cenote» y atrapa su primera sílaba.",
                    listOf("ce", "se", "ne"),
                    0,
                    "En México suena con «s», pero se escribe con «c»: ce-no-te.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "cenote"
                )
            )
        ),
        fonologia(
            numero = 7,
            titulo = "El sonido final",
            subtitulo = "Escucha cómo termina cada palabra.",
            tier = 2,
            icono = "🔚",
            conceptos = listOf("final", "sílaba", "escucha"),
            teoria = listOf(
                "El sonido final es el último golpe de voz de una palabra.",
                "Alarga la última parte: «tam-bor» termina en «bor»."
            ),
            preguntas = listOf(
                p(
                    "Escucha «tambor» y atrapa su última sílaba.",
                    listOf("bor", "tam", "dor"),
                    0,
                    "El tambor suena al final: tam-bor.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "tambor"
                ),
                p(
                    "Escucha «maíz» y atrapa su última sílaba.",
                    listOf("ma", "íz", "z"),
                    1,
                    "El golpe final lleva la «i» con acento: ma-íz.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "maíz"
                ),
                p(
                    "Escucha «volcán» y atrapa su última sílaba.",
                    listOf("cán", "vol", "án"),
                    0,
                    "Termina como «canción»: vol-cán.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "volcán"
                )
            )
        ),
        fonologia(
            numero = 8,
            titulo = "Cuenta sílabas con Zazi",
            subtitulo = "Una palmada por cada golpe de voz.",
            tier = 2,
            icono = "✋",
            conceptos = listOf("contar sílabas", "palmada", "ritmo"),
            teoria = listOf(
                "Para contar sílabas, aplaude al mismo tiempo que hablas.",
                "Cada palmada es un golpe de voz: za-pa-to son tres."
            ),
            preguntas = listOf(
                p(
                    "Escucha «zapato» y atrapa cuántas sílabas tiene.",
                    listOf("Dos", "Tres", "Cuatro"),
                    1,
                    "Aplaude: za-pa-to. Tres palmadas.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "zapato"
                ),
                p(
                    "Escucha «luna» y atrapa cuántas sílabas tiene.",
                    listOf("Una", "Dos", "Tres"),
                    1,
                    "Aplaude: lu-na. Dos palmadas.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "luna"
                ),
                p(
                    "Escucha «mariposa» y atrapa cuántas sílabas tiene.",
                    listOf("Dos", "Tres", "Cuatro"),
                    2,
                    "Aplaude: ma-ri-po-sa. Cuatro palmadas.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "mariposa"
                )
            )
        ),
        fonologia(
            numero = 9,
            titulo = "El tambor de las sílabas",
            subtitulo = "Junta los golpes y descubre la palabra.",
            tier = 3,
            icono = "🥁",
            conceptos = listOf("unir sílabas", "palabra", "ritmo"),
            teoria = listOf(
                "Si juntas dos sílabas, formas una palabra: «ca» + «sa» = casa.",
                "Di las sílabas seguidas, sin hacer pausas largas."
            ),
            preguntas = listOf(
                p(
                    "Escucha «ca-sa» y atrapa la palabra que se forma.",
                    listOf("casa", "cama", "saca"),
                    0,
                    "Junta «ca» y «sa»: ca-sa.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "ca-sa"
                ),
                p(
                    "Escucha «pe-rro» y atrapa la palabra que se forma.",
                    listOf("perro", "pera", "cerro"),
                    0,
                    "La «rr» suena fuerte en medio: pe-rro.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "pe-rro"
                ),
                p(
                    "Escucha «ti-je-ra» y atrapa la palabra que se forma.",
                    listOf("tijera", "tetera", "tarea"),
                    0,
                    "Junta ti-je-ra: tijera.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "ti-je-ra"
                )
            )
        ),
        fonologia(
            numero = 10,
            titulo = "El Nahual Trabalenguas",
            subtitulo = "Jefe de capítulo: todo el sonido en un reto.",
            tier = 3,
            icono = "🌙",
            conceptos = listOf("sílaba", "sonido", "reto"),
            teoria = listOf(
                "El Nahual Trabalenguas mezcla sílabas para ver si escuchas con atención.",
                "Respira, escucha dos veces y después elige."
            ),
            preguntas = listOf(
                p(
                    "Escucha «chocolate» y atrapa cuántas sílabas tiene.",
                    listOf("Tres", "Cuatro", "Cinco"),
                    1,
                    "Aplaude: cho-co-la-te. Cuatro palmadas.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "chocolate"
                ),
                p(
                    "Escucha «nahual» y atrapa su primera sílaba.",
                    listOf("na", "hua", "nal"),
                    0,
                    "El nahual empieza con «na», como «naranja».",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "nahual"
                ),
                p(
                    "Escucha «trabalenguas» y atrapa cuántas sílabas tiene.",
                    listOf("Tres", "Cuatro", "Cinco"),
                    1,
                    "Aplaude: tra-ba-len-guas. Cuatro palmadas.",
                    MecanicaLengua.ATRAPA_SONIDOS,
                    audio = "trabalenguas"
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 2 · SÍLABAS TRABADAS Y ORTOGRAFÍA
        // ===================================================================
        ortografia(
            numero = 11,
            titulo = "El dragón de las trabadas",
            subtitulo = "Bl y br se juntan con la lengua.",
            tier = 2,
            icono = "🐉",
            conceptos = listOf("sílaba trabada", "bl", "br"),
            teoria = listOf(
                "Las sílabas trabadas juntan dos consonantes: bla, ble, bli, blo, blu; bra, bre, bri, bro, bru.",
                "Escucha si suena suave la «l» o fuerte la «r»."
            ),
            preguntas = listOf(
                p(
                    "¿Con qué sílaba empieza «blusa»?",
                    listOf("blu", "bru", "plu"),
                    0,
                    "La blusa se pone con «bl»: blu-sa.",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "¿Qué sílaba completa «__avo», que significa valiente?",
                    listOf("bra", "bla", "ba"),
                    0,
                    "Bravo lleva «br», como «brazo».",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "¿Cuál palabra está bien escrita?",
                    listOf("bliblioteca", "biblioteca", "bibloteca"),
                    1,
                    "El orden correcto es b-i-b-l-i-o-t-e-c-a, con «bl».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 12,
            titulo = "La serpiente de la pl",
            subtitulo = "Pl y pr silban en la cueva.",
            tier = 2,
            icono = "🐍",
            conceptos = listOf("pl", "pr", "trabada"),
            teoria = listOf(
                "Pla, ple, pli, plo, plu suenan con «l»; pra, pre, pri, pro, pru suenan con «r».",
                "La «l» es suave y la «r» vibra tantito."
            ),
            preguntas = listOf(
                p(
                    "¿Con qué sílaba empieza «plátano»?",
                    listOf("pla", "pra", "pa"),
                    0,
                    "El plátano empieza como «playa».",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "Completa «__imavera».",
                    listOf("pri", "pli", "pi"),
                    0,
                    "La primavera lleva «pr» de «prima».",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "¿Cuál palabra está bien escrita?",
                    listOf("princesa", "plincesa", "prinsesa"),
                    0,
                    "Se escribe con «pr» y con «c»: prin-ce-sa.",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 13,
            titulo = "El tlacuache de la tl",
            subtitulo = "La tl y la tr tienen su maña.",
            tier = 3,
            icono = "🦝",
            conceptos = listOf("tl", "tr", "trabada"),
            teoria = listOf(
                "Tla, tle, tli, tlo, tlu son sílabas mexicanas muy comunes: tlacuache, tlapalería.",
                "Tra, tre, tri, tro, tru vibran con la «r»: trabajo, triste."
            ),
            preguntas = listOf(
                p(
                    "Completa el animal: «__acuache».",
                    listOf("tla", "tra", "ta"),
                    0,
                    "El tlacuache empieza con «tl».",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "Completa «__igo», el grano dorado.",
                    listOf("tri", "tli", "ti"),
                    0,
                    "Trigo lleva «tr» de «tres».",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "¿Cuál palabra lleva «tl»?",
                    listOf("atlas", "atrás", "arma"),
                    0,
                    "At-las separa la «tl»; «atrás» lleva «tr».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 14,
            titulo = "La cueva de la cl",
            subtitulo = "Cl y cr abren la boca de par en par.",
            tier = 3,
            icono = "🕳️",
            conceptos = listOf("cl", "cr", "trabada"),
            teoria = listOf(
                "Cla, cle, cli, clo, clu suenan con «l»: clavo, clima.",
                "Cra, cre, cri, cro, cru suenan con «r»: crecer, cristal."
            ),
            preguntas = listOf(
                p(
                    "¿Con qué sílaba empieza «clavo»?",
                    listOf("cla", "cra", "ca"),
                    0,
                    "El clavo empieza como «clase».",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "Completa «__eciente», que está creciendo.",
                    listOf("cre", "cle", "ce"),
                    0,
                    "Creciente viene de «crecer», con «cr».",
                    MecanicaLengua.SILABAS
                ),
                p(
                    "¿Cuál palabra lleva «cr»?",
                    listOf("cristal", "clima", "cama"),
                    0,
                    "Cristal empieza con «cr»; clima lleva «cl».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 15,
            titulo = "El sonido de la g",
            subtitulo = "Ga, go, gu suenan fuerte; gue, gui también.",
            tier = 3,
            icono = "🐈",
            conceptos = listOf("ga go gu", "sonido fuerte", "g"),
            teoria = listOf(
                "La «g» suena fuerte con a, o, u: gato, gorra, gusta.",
                "Para el mismo sonido antes de e, i se escribe «gu»: guitarra, guerra.",
                "Pero «gue» y «gui» no pronuncian la «u», salvo con diéresis: pingüino."
            ),
            preguntas = listOf(
                p(
                    "Completa «__to», el felino doméstico.",
                    listOf("ga", "go", "gu"),
                    0,
                    "Gato se escribe con «ga».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__tarra».",
                    listOf("gui", "gi", "güi"),
                    0,
                    "La guitarra lleva «gui»: la «u» no se pronuncia.",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__rra», la prenda para la cabeza.",
                    listOf("go", "gu", "ga"),
                    0,
                    "Gorra se escribe con «go».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 16,
            titulo = "La g y la j",
            subtitulo = "Dos letras que a veces suenan parecido.",
            tier = 3,
            icono = "🦒",
            conceptos = listOf("g", "j", "ortografía"),
            teoria = listOf(
                "La «j» suena como una risa suave: jirafa, juguete.",
                "La «g» con e, i también suena fuerte: gente, gitano. Hay que recordar cuál va en cada palabra."
            ),
            preguntas = listOf(
                p(
                    "Completa «__rafa».",
                    listOf("ji", "gi", "hi"),
                    0,
                    "La jirafa se escribe con «j».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__nte».",
                    listOf("gen", "jen", "guen"),
                    0,
                    "Gente se escribe con «gen».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__guete».",
                    listOf("ju", "gu", "hu"),
                    0,
                    "Juguete empieza con «ju».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 17,
            titulo = "El tesoro de la b y la v",
            subtitulo = "B de burro, v de vaca.",
            tier = 3,
            icono = "💎",
            conceptos = listOf("b", "v", "ortografía"),
            teoria = listOf(
                "La «b» y la «v» suenan casi igual en español, pero se escriben distinto.",
                "Un truco: «vaca» va con v, «burro» va con b."
            ),
            preguntas = listOf(
                p(
                    "Completa «__ca», el animal que da leche.",
                    listOf("va", "ba", "fa"),
                    0,
                    "Vaca empieza con «v» de «verde».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__rro».",
                    listOf("bu", "vu", "du"),
                    0,
                    "Burro empieza con «b» de «boca».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__ntana».",
                    listOf("ven", "ben", "fen"),
                    0,
                    "Ventana lleva «v» de «viento».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 18,
            titulo = "La c, la s y la z",
            subtitulo = "Suenan parecido, pero se escriben distinto.",
            tier = 4,
            icono = "🔠",
            conceptos = listOf("c", "s", "z"),
            teoria = listOf(
                "En México, «ce», «ci» y «za», «zo», «zu» suenan con «s».",
                "Por eso hay que recordar la letra: cena con c, zapato con z, siesta con s."
            ),
            preguntas = listOf(
                p(
                    "«Cena» se escribe con...",
                    listOf("c", "s", "z"),
                    0,
                    "Cena lleva «ce», como «cereza».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__pato».",
                    listOf("za", "sa", "ca"),
                    0,
                    "Zapato lleva «za», como «zorro».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__ne», la película.",
                    listOf("ci", "si", "zi"),
                    0,
                    "Cine se escribe con «ci».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 19,
            titulo = "La ll y la y",
            subtitulo = "Llave con ll, yogur con y.",
            tier = 4,
            icono = "🔑",
            conceptos = listOf("ll", "y", "ortografía"),
            teoria = listOf(
                "La «ll» suena como una «y» suave en casi todo México: llave, lluvia.",
                "La «y» sola aparece al inicio o al final: yogur, rey."
            ),
            preguntas = listOf(
                p(
                    "Completa «__ave».",
                    listOf("lla", "ya", "la"),
                    0,
                    "Llave empieza con «ll».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__gur».",
                    listOf("yo", "llo", "io"),
                    0,
                    "Yogur empieza con «y».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "Completa «__via», el agua que cae.",
                    listOf("llu", "yu", "lu"),
                    0,
                    "Lluvia lleva «llu».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),
        ortografia(
            numero = 20,
            titulo = "El Mago de la Ortografía",
            subtitulo = "Jefe de capítulo: cada letra en su lugar.",
            tier = 4,
            icono = "🧙",
            conceptos = listOf("ortografía", "b/v", "ll/y"),
            teoria = listOf(
                "Para escribir bien, lee la palabra completa y fíjate en las letras tramposas.",
                "Si dudas, piensa en una palabra de la misma familia: «ventana» y «viento»."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál palabra está bien escrita?",
                    listOf("ventana", "bentana", "vantana"),
                    0,
                    "Ventana lleva «v» y termina en «ana».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "¿Cuál lleva «br»?",
                    listOf("brazo", "blusa", "plato"),
                    0,
                    "Brazo empieza con «br», como «bravo».",
                    MecanicaLengua.ORTOGRAFIA
                ),
                p(
                    "¿Cuál pareja está bien escrita?",
                    listOf("llave y yogur", "yave y llogur", "llave y llogur"),
                    0,
                    "Llave con «ll» y yogur con «y».",
                    MecanicaLengua.ORTOGRAFIA
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 3 · SINTAXIS Y GRAMÁTICA
        // ===================================================================
        gramatica(
            numero = 21,
            titulo = "El verbo que hace",
            subtitulo = "Los verbos son acciones.",
            tier = 3,
            icono = "🏃",
            conceptos = listOf("verbo", "acción", "oración"),
            teoria = listOf(
                "El verbo dice qué hace el sujeto: corre, canta, cuida.",
                "Para encontrarlo, pregúntate: ¿qué hace?"
            ),
            preguntas = listOf(
                p(
                    "¿Cuál es el verbo en «Zazi corre por la cueva»?",
                    listOf("Zazi", "corre", "cueva"),
                    1,
                    "Corre es la acción que hace Zazi.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Qué verbo completa «La luna ___ en el cielo»?",
                    listOf("brilla", "luna", "cielo"),
                    0,
                    "Brilla es lo que hace la luna.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál es el verbo en «Los aluxes cuidan el maíz»?",
                    listOf("aluxes", "cuidan", "maíz"),
                    1,
                    "Cuidan es la acción de los aluxes.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 22,
            titulo = "El adjetivo pinta",
            subtitulo = "Los adjetivos describen cómo es algo.",
            tier = 3,
            icono = "🎨",
            conceptos = listOf("adjetivo", "describir", "cómo es"),
            teoria = listOf(
                "El adjetivo dice cómo es una persona, animal o cosa: brillante, oscura, sabroso.",
                "Casi siempre acompaña al sustantivo."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál es el adjetivo en «El nahual tiene ojos brillantes»?",
                    listOf("nahual", "ojos", "brillantes"),
                    2,
                    "Brillantes describe cómo son los ojos.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Qué adjetivo describe «la cueva oscura»?",
                    listOf("cueva", "oscura", "la"),
                    1,
                    "Oscura dice cómo es la cueva.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "Elige el adjetivo para «un taco ___».",
                    listOf("sabroso", "camina", "tortilla"),
                    0,
                    "Sabroso describe al taco.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 23,
            titulo = "El sustantivo nombra",
            subtitulo = "Personas, animales, cosas y lugares.",
            tier = 3,
            icono = "🏷️",
            conceptos = listOf("sustantivo", "nombrar", "lugar"),
            teoria = listOf(
                "El sustantivo nombra algo que existe: Llorona, maíz, cenote, Zazi.",
                "Puede ser persona, animal, cosa o lugar."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál es el sustantivo en «La Llorona camina junto al río»?",
                    listOf("La", "Llorona", "camina"),
                    1,
                    "Llorona es el nombre de la persona de la leyenda.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál es el sustantivo en «El maíz crece»?",
                    listOf("El", "maíz", "crece"),
                    1,
                    "Maíz es la cosa que crece.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Qué palabra es un lugar?",
                    listOf("cenote", "correr", "azul"),
                    0,
                    "El cenote es un lugar.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 24,
            titulo = "El sujeto y el predicado",
            subtitulo = "Quién hace y qué hace.",
            tier = 4,
            icono = "🧱",
            conceptos = listOf("sujeto", "predicado", "oración"),
            teoria = listOf(
                "El sujeto es de quién hablamos; el predicado dice qué hace.",
                "En «Zazi vuela», Zazi es el sujeto y vuela es el predicado."
            ),
            preguntas = listOf(
                p(
                    "En «Zazi vuela sobre la pirámide», ¿quién es el sujeto?",
                    listOf("Zazi", "vuela", "pirámide"),
                    0,
                    "El sujeto es Zazi, de quien hablamos.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "En «Los niños leen leyendas», ¿qué es el predicado?",
                    listOf("Los niños", "leen leyendas", "leyendas"),
                    1,
                    "Leen leyendas dice qué hacen los niños.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál oración tiene sujeto y predicado?",
                    listOf("El ajolote nada.", "Nada el.", "Ajolote."),
                    0,
                    "El ajolote nada tiene quién y qué hace.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 25,
            titulo = "Singular y plural",
            subtitulo = "Uno o varios.",
            tier = 4,
            icono = "1️⃣",
            conceptos = listOf("singular", "plural", "concordancia"),
            teoria = listOf(
                "Singular es uno: el alux, la estrella.",
                "Plural es más de uno: los aluxes, las estrellas."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál está en plural?",
                    listOf("el alux", "los aluxes", "la milpa"),
                    1,
                    "Los aluxes son varios; lleva «los» y «es».",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál está en singular?",
                    listOf("las estrellas", "la estrella", "los ríos"),
                    1,
                    "La estrella es una sola.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cómo se escribe el plural de «lápiz»?",
                    listOf("lápizes", "lápices", "lapices"),
                    1,
                    "Las palabras terminadas en z hacen plural con «ces»: lápices.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 26,
            titulo = "Masculino y femenino",
            subtitulo = "El y la también concuerdan.",
            tier = 4,
            icono = "⚖️",
            conceptos = listOf("masculino", "femenino", "artículo"),
            teoria = listOf(
                "Muchos sustantivos llevan «el» si son masculinos y «la» si son femeninos.",
                "No siempre se adivina por la terminación: hay que escuchar cómo suena bien."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál es femenino?",
                    listOf("el sol", "la luna", "el río"),
                    1,
                    "Decimos «la luna».",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál es masculino?",
                    listOf("la casa", "el maíz", "la milpa"),
                    1,
                    "Decimos «el maíz».",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "Completa: «__ casa».",
                    listOf("El", "La", "Los"),
                    1,
                    "Casa es femenino: la casa.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 27,
            titulo = "Sinónimos gemelos",
            subtitulo = "Dos palabras, un mismo significado.",
            tier = 4,
            icono = "👯",
            conceptos = listOf("sinónimo", "significado", "palabras"),
            teoria = listOf(
                "Los sinónimos significan casi lo mismo: contento y feliz.",
                "Cambian las palabras, pero no la idea."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál es sinónimo de «contento»?",
                    listOf("feliz", "triste", "enojado"),
                    0,
                    "Feliz y contento se sienten parecido.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "Sinónimo de «grande».",
                    listOf("enorme", "chico", "delgado"),
                    0,
                    "Enorme también significa muy grande.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "Sinónimo de «bonito».",
                    listOf("hermoso", "feo", "sucio"),
                    0,
                    "Hermoso y bonito dicen lo mismo.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 28,
            titulo = "Antónimos opuestos",
            subtitulo = "Palabras que dicen lo contrario.",
            tier = 4,
            icono = "↔️",
            conceptos = listOf("antónimo", "opuesto", "contrario"),
            teoria = listOf(
                "Los antónimos significan lo contrario: día y noche.",
                "Si una palabra sube, su antónimo baja."
            ),
            preguntas = listOf(
                p(
                    "Antónimo de «día».",
                    listOf("noche", "sol", "luz"),
                    0,
                    "La noche es lo contrario del día.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "Antónimo de «frío».",
                    listOf("caliente", "hielo", "nieve"),
                    0,
                    "Caliente es lo contrario de frío.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "Antónimo de «subir».",
                    listOf("bajar", "saltar", "correr"),
                    0,
                    "Bajar es lo contrario de subir.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 29,
            titulo = "La oración ordenada",
            subtitulo = "Sujeto, verbo y complemento en su lugar.",
            tier = 5,
            icono = "🧩",
            conceptos = listOf("orden", "concordancia", "oración"),
            teoria = listOf(
                "Una oración clara suele ir: quién + qué hace + dónde o qué.",
                "El verbo debe concordar con el sujeto: «las estrellas brillan», no «brilla»."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál oración concuerda?",
                    listOf("Las estrellas brillan.", "Las estrellas brilla.", "La estrellas brillan."),
                    0,
                    "El plural «estrellas» pide «brillan».",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál oración concuerda?",
                    listOf("El niño canta.", "El niño cantan.", "Los niño canta."),
                    0,
                    "El singular «niño» pide «canta».",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "Ordena: «cueva / en / vive / el / nahual / la».",
                    listOf(
                        "El nahual vive en la cueva.",
                        "Vive el nahual en la cueva la.",
                        "En cueva el nahual vive la."
                    ),
                    0,
                    "Sujeto, verbo y lugar: El nahual vive en la cueva.",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),
        gramatica(
            numero = 30,
            titulo = "La Reina de la Gramática",
            subtitulo = "Jefe de capítulo: oraciones perfectas.",
            tier = 5,
            icono = "👑",
            conceptos = listOf("gramática", "verbo", "antónimo"),
            teoria = listOf(
                "Para armar una oración, revisa el verbo, el sujeto y la concordancia.",
                "Leer en voz alta ayuda a escuchar si algo suena raro."
            ),
            preguntas = listOf(
                p(
                    "¿Cuál palabra es un verbo?",
                    listOf("escribir", "cuaderno", "rápido"),
                    0,
                    "Escribir es una acción.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál pareja son antónimos?",
                    listOf("alto/bajo", "alto/grande", "bajo/pequeño"),
                    0,
                    "Alto y bajo son contrarios.",
                    MecanicaLengua.GRAMATICA
                ),
                p(
                    "¿Cuál oración está bien escrita?",
                    listOf(
                        "Los aluxes cuidan el maíz.",
                        "Los aluxes cuida el maíz.",
                        "Los alux cuidan el maíz."
                    ),
                    0,
                    "El plural «aluxes» pide «cuidan»; el plural correcto es «aluxes».",
                    MecanicaLengua.GRAMATICA
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 4 · COMPRENSIÓN LECTORA
        // ===================================================================
        lectura(
            numero = 31,
            titulo = "La Llorona y la secuencia",
            subtitulo = "Primero, después y al final.",
            tier = 4,
            icono = "💧",
            conceptos = listOf("secuencia", "leyenda", "orden"),
            teoria = listOf(
                "Una historia tiene un orden: primero, después y al final.",
                "Lee con calma y busca las pistas de tiempo."
            ),
            preguntas = listOf(
                p(
                    "La Llorona caminaba junto al río. Primero lloró, luego buscó a sus hijos y al final se perdió entre los árboles. ¿Qué hizo primero?",
                    listOf("Lloró.", "Buscó a sus hijos.", "Se perdió."),
                    0,
                    "El texto dice «primero lloró».",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "Después de llorar, ¿qué hizo la Llorona?",
                    listOf("Buscó a sus hijos.", "Cruzó el mar.", "Durmió en el río."),
                    0,
                    "El texto dice «luego buscó a sus hijos».",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué pasó al final de la historia?",
                    listOf("Se perdió entre los árboles.", "Encontró un tesoro.", "Se fue al mercado."),
                    0,
                    "El texto termina con «al final se perdió entre los árboles».",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 32,
            titulo = "El Callejón del Beso",
            subtitulo = "Una leyenda de amor en Guanajuato.",
            tier = 4,
            icono = "💋",
            conceptos = listOf("idea principal", "leyenda", "Guanajuato"),
            teoria = listOf(
                "La idea principal es de qué habla la historia completa.",
                "Los detalles son datos pequeños que la acompañan."
            ),
            preguntas = listOf(
                p(
                    "En Guanajuato, una joven y su novio se besaron desde balcones vecinos. El padre los sorprendió y la historia se volvió leyenda. ¿Cuál es la idea principal?",
                    listOf(
                        "Una leyenda de amor en Guanajuato.",
                        "Un mercado de frutas.",
                        "Un partido de fútbol."
                    ),
                    0,
                    "La historia habla de un amor que se volvió leyenda.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Desde dónde se besaron?",
                    listOf("Desde balcones vecinos.", "Desde un tren.", "Desde la escuela."),
                    0,
                    "El texto menciona «balcones vecinos».",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿En qué ciudad pasa la leyenda?",
                    listOf("Guanajuato.", "Monterrey.", "Mérida."),
                    0,
                    "El texto empieza diciendo «En Guanajuato».",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 33,
            titulo = "Los Aluxes del maíz",
            subtitulo = "Duendes mayas que cuidan la milpa.",
            tier = 4,
            icono = "🌽",
            conceptos = listOf("detalle", "leyenda maya", "aluxes"),
            teoria = listOf(
                "Los detalles responden quién, qué, dónde y cuándo.",
                "Subrayar mentalmente esos datos ayuda a recordar la historia."
            ),
            preguntas = listOf(
                p(
                    "Los aluxes son duendes mayas que cuidan el maíz y las milpas. Si los respetas, te ayudan; si no, esconden tus cosas. ¿Qué cuidan?",
                    listOf("El maíz y las milpas.", "Los coches.", "Las estrellas."),
                    0,
                    "El texto dice que cuidan el maíz y las milpas.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué pasa si no los respetas?",
                    listOf("Esconden tus cosas.", "Te dan oro.", "Te cantan."),
                    0,
                    "El texto dice que esconden tus cosas.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿De qué cultura son los aluxes?",
                    listOf("Maya.", "Inca.", "China."),
                    0,
                    "El texto dice «duendes mayas».",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 34,
            titulo = "La Xtabay y el árbol",
            subtitulo = "La causa y el efecto de una leyenda.",
            tier = 4,
            icono = "🌳",
            conceptos = listOf("causa", "efecto", "Xtabay"),
            teoria = listOf(
                "La causa es lo que provoca algo; el efecto es lo que pasa después.",
                "Pregúntate «¿por qué?» para encontrar la causa."
            ),
            preguntas = listOf(
                p(
                    "La Xtabay aparece junto a un árbol espinoso. Quien la sigue se pierde en la selva. ¿Por qué se pierde?",
                    listOf("Porque la sigue.", "Porque duerme.", "Porque corre."),
                    0,
                    "La causa de perderse es seguir a la Xtabay.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Dónde aparece la Xtabay?",
                    listOf("Junto a un árbol espinoso.", "En una panadería.", "En el mar."),
                    0,
                    "El texto lo dice al inicio.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué conviene hacer si la ves?",
                    listOf("No seguirla.", "Correr hacia ella.", "Darle un abrazo."),
                    0,
                    "Si seguirla causa perderse, lo mejor es no seguirla.",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 35,
            titulo = "El Chupacabras del rancho",
            subtitulo = "Una leyenda para inferir.",
            tier = 5,
            icono = "🐐",
            conceptos = listOf("inferencia", "leyenda", "rancho"),
            teoria = listOf(
                "Inferir es descubrir algo que el texto no dice con todas sus letras.",
                "Une las pistas con lo que ya sabes para sacar una conclusión."
            ),
            preguntas = listOf(
                p(
                    "En el rancho aparecieron cabras sin sangre y con marcas en el cuello. Los abuelos dijeron: «Fue el Chupacabras». ¿Qué creyeron los abuelos?",
                    listOf("Que fue el Chupacabras.", "Que fue un doctor.", "Que fue la lluvia."),
                    0,
                    "Los abuelos lo dijeron directamente.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué animal apareció lastimado en la historia?",
                    listOf("Cabras.", "Perros.", "Pollos."),
                    0,
                    "El texto menciona cabras.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué es el Chupacabras en esta historia?",
                    listOf("Una criatura de leyenda.", "Un animal de zoológico.", "Un juguete."),
                    0,
                    "Es una criatura de leyenda, no un animal real del zoológico.",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 36,
            titulo = "La Mulata de Córdoba",
            subtitulo = "Ingenio en una leyenda veracruzana.",
            tier = 5,
            icono = "⛵",
            conceptos = listOf("personaje", "rasgo", "leyenda"),
            teoria = listOf(
                "Los rasgos de un personaje se notan en lo que dice y hace.",
                "Pregúntate: ¿qué decisión tomó y qué demuestra eso?"
            ),
            preguntas = listOf(
                p(
                    "La Mulata de Córdoba dibujó un barco en la pared de su celda y escapó en él. ¿Qué rasgo la describe mejor?",
                    listOf("Astuta.", "Miedosa.", "Perezosa."),
                    0,
                    "Ingeniárselas para escapar con un dibujo demuestra astucia.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Dónde estaba la Mulata?",
                    listOf("En una celda.", "En un barco.", "En una escuela."),
                    0,
                    "El texto dice «la pared de su celda».",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué dibujó en la pared?",
                    listOf("Un barco.", "Un maíz.", "Un perro."),
                    0,
                    "Dibujó un barco para escapar.",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 37,
            titulo = "El Charro Negro",
            subtitulo = "Piensa antes de aceptar.",
            tier = 5,
            icono = "🐴",
            conceptos = listOf("moraleja", "leyenda", "decisión"),
            teoria = listOf(
                "La moraleja es la enseñanza que deja una historia.",
                "Suele aparecer al final, cuando ya sabes qué pasó."
            ),
            preguntas = listOf(
                p(
                    "El Charro Negro ofrece dinero a cambio del alma. Quien acepta sin pensar termina arrepentido. ¿Qué enseña la leyenda?",
                    listOf("Pensar antes de aceptar.", "Correr más rápido.", "Nunca contar dinero."),
                    0,
                    "La enseñanza es pensar antes de aceptar una oferta.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué ofrece el Charro Negro?",
                    listOf("Dinero.", "Comida.", "Un caballo de juguete."),
                    0,
                    "El texto dice que ofrece dinero.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué pasa si aceptas sin pensar?",
                    listOf("Terminas arrepentido.", "Ganas un premio.", "Todo se vuelve dulce."),
                    0,
                    "El texto dice que terminas arrepentido.",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 38,
            titulo = "Quetzalcóatl y el maíz",
            subtitulo = "La idea principal de un mito.",
            tier = 5,
            icono = "🐍",
            conceptos = listOf("idea principal", "mito", "maíz"),
            teoria = listOf(
                "La idea principal resume de qué trata todo el texto.",
                "Puede estar al inicio o descubrirse al final."
            ),
            preguntas = listOf(
                p(
                    "Quetzalcóatl encontró el maíz y lo compartió con los humanos para que no pasaran hambre. ¿Cuál es la idea principal?",
                    listOf(
                        "Compartir el maíz con los humanos.",
                        "Esconder el maíz.",
                        "Vender el maíz."
                    ),
                    0,
                    "Todo el mito gira alrededor de compartir el maíz.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué encontró Quetzalcóatl?",
                    listOf("El maíz.", "Un tesoro.", "Una ciudad."),
                    0,
                    "Encontró el maíz.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Por qué lo compartió?",
                    listOf("Para que no pasaran hambre.", "Para jugar.", "Para venderlo."),
                    0,
                    "Lo compartió para que no pasaran hambre.",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 39,
            titulo = "Las Cihuateteo y el camino",
            subtitulo = "Compara lo que hacen los personajes.",
            tier = 5,
            icono = "🌕",
            conceptos = listOf("comparar", "personaje", "leyenda"),
            teoria = listOf(
                "Comparar es mirar en qué se parecen y en qué se diferencian dos cosas.",
                "Fíjate en lo que hace cada personaje."
            ),
            preguntas = listOf(
                p(
                    "Las Cihuateteo son mujeres valientes que acompañan a los guerreros. ¿A quién acompañan?",
                    listOf("A los guerreros.", "A los aluxes.", "A los caballos."),
                    0,
                    "El texto dice que acompañan a los guerreros.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Cómo se les describe?",
                    listOf("Valientes.", "Dormilonas.", "Tímidas."),
                    0,
                    "El texto las llama «mujeres valientes».",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "Según la leyenda, ¿qué hacen las Cihuateteo?",
                    listOf(
                        "Acompañan a los guerreros.",
                        "Venden maíz en el mercado.",
                        "Pintan barcos en las paredes."
                    ),
                    0,
                    "El texto dice que acompañan a los guerreros.",
                    MecanicaLengua.LECTURA
                )
            )
        ),
        lectura(
            numero = 40,
            titulo = "El Guardián de las Leyendas",
            subtitulo = "Jefe de capítulo: lee, recuerda y deduce.",
            tier = 5,
            icono = "📖",
            conceptos = listOf("comprensión", "leyenda", "detalle"),
            teoria = listOf(
                "Para responder, vuelve al texto y busca la pista exacta.",
                "No adivines: compara cada opción con lo que dice la historia."
            ),
            preguntas = listOf(
                p(
                    "La Llorona llora cerca del río; los aluxes cuidan el maíz. ¿Cuál leyenda habla de un río?",
                    listOf("La Llorona.", "Los aluxes.", "El Charro Negro."),
                    0,
                    "La Llorona aparece junto al río.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Cuál leyenda cuida el maíz?",
                    listOf("Los aluxes.", "La Xtabay.", "La Mulata."),
                    0,
                    "Los aluxes cuidan el maíz y las milpas.",
                    MecanicaLengua.LECTURA
                ),
                p(
                    "¿Qué tienen en común todas estas historias?",
                    listOf(
                        "Son leyendas tradicionales.",
                        "Son recetas de cocina.",
                        "Son noticias del clima."
                    ),
                    0,
                    "Todas son leyendas que se cuentan de generación en generación.",
                    MecanicaLengua.LECTURA
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 5 · PENSAMIENTO CRÍTICO
        // ===================================================================
        critico(
            numero = 41,
            titulo = "La metáfora del sol",
            subtitulo = "Decir una cosa con otra muy parecida.",
            tier = 5,
            icono = "☀️",
            conceptos = listOf("metáfora", "comparación", "imagen"),
            teoria = listOf(
                "La metáfora dice que algo ES otra cosa para crear una imagen.",
                "«El sol es una naranja de fuego» no es literal: pinta cómo se ve."
            ),
            preguntas = listOf(
                p(
                    "«El sol es una naranja de fuego». ¿Qué significa?",
                    listOf(
                        "Que el sol es redondo y brillante.",
                        "Que el sol se come.",
                        "Que el sol es una fruta."
                    ),
                    0,
                    "La metáfora pinta al sol como algo redondo y ardiente.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«Sus ojos son dos luceros». ¿Qué compara?",
                    listOf(
                        "Los ojos con las estrellas.",
                        "Los ojos con las nubes.",
                        "Los ojos con los ríos."
                    ),
                    0,
                    "Un lucero es una estrella: compara ojos con estrellas.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál es una metáfora?",
                    listOf(
                        "La luna es un plato de plata.",
                        "La luna brilla.",
                        "La luna está arriba."
                    ),
                    0,
                    "Dice que la luna ES un plato: eso es metáfora.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 42,
            titulo = "El símil del río",
            subtitulo = "Comparar con la palabra «como».",
            tier = 5,
            icono = "💧",
            conceptos = listOf("símil", "como", "comparación"),
            teoria = listOf(
                "El símil compara dos cosas usando «como» o «parece».",
                "«Corre como el viento» es un símil: no dice que sea viento."
            ),
            preguntas = listOf(
                p(
                    "«Corre como el viento». ¿Qué figura usa?",
                    listOf("Símil.", "Metáfora.", "Acertijo."),
                    0,
                    "Usa «como» para comparar: es un símil.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál usa la palabra «como»?",
                    listOf(
                        "Fuerte como un roble.",
                        "El roble es un gigante.",
                        "El roble crece."
                    ),
                    0,
                    "La palabra «como» aparece en la primera opción.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«Sus dientes son como perlas» compara...",
                    listOf(
                        "Los dientes con las perlas.",
                        "Las perlas con el mar.",
                        "Los dientes con el agua."
                    ),
                    0,
                    "Compara los dientes con perlas por lo blancos y brillantes.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 43,
            titulo = "La fábula absurda",
            subtitulo = "Detecta lo que no puede pasar.",
            tier = 5,
            icono = "🤪",
            conceptos = listOf("absurdo", "lógica", "fábula"),
            teoria = listOf(
                "Lo absurdo rompe las reglas del mundo real.",
                "Si un pez sube a un árbol, la historia está jugando con lo imposible."
            ),
            preguntas = listOf(
                p(
                    "«El pez subió al árbol y se puso a cantar». ¿Qué es absurdo?",
                    listOf(
                        "El pez subió al árbol.",
                        "El pez nadó.",
                        "El árbol creció."
                    ),
                    0,
                    "Los peces no suben a los árboles.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál situación es imposible?",
                    listOf("La vaca ladró.", "La vaca comió pasto.", "La vaca caminó."),
                    0,
                    "Las vacas mugen, no ladran.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«El libro ladró y el perro leyó la historia». ¿Qué está mal?",
                    listOf(
                        "El libro ladró y el perro leyó.",
                        "El libro tenía muchas historias.",
                        "El perro caminó por el jardín."
                    ),
                    0,
                    "Los libros no ladran y los perros no leen: todo el enunciado es absurdo.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 44,
            titulo = "El refrán revuelto",
            subtitulo = "La sabiduría popular en pocas palabras.",
            tier = 5,
            icono = "🧓",
            conceptos = listOf("refrán", "significado", "sabiduría"),
            teoria = listOf(
                "Un refrán es una frase corta con una enseñanza.",
                "No siempre se entiende literal: hay que buscar el consejo escondido."
            ),
            preguntas = listOf(
                p(
                    "«Más vale tarde que nunca» significa...",
                    listOf(
                        "Que hacerlo tarde es mejor que no hacerlo.",
                        "Que hay que llegar tarde.",
                        "Que nunca hay que hacerlo."
                    ),
                    0,
                    "El refrán anima a hacerlo, aunque sea tarde.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«No por mucho madrugar amanece más temprano» enseña...",
                    listOf(
                        "Que apurarse no siempre adelanta.",
                        "Que hay que dormir hasta tarde.",
                        "Que el sol se levanta solo."
                    ),
                    0,
                    "Dice que madrugar de más no hace que amanezca antes.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál refrán habla de no rendirse?",
                    listOf(
                        "El que persevera, alcanza.",
                        "A quien madruga, Dios lo ayuda.",
                        "Camarón que se duerme, se lo lleva la corriente."
                    ),
                    0,
                    "Perseverar es seguir intentando hasta lograrlo.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 45,
            titulo = "La noticia imposible",
            subtitulo = "Hechos y opiniones no son lo mismo.",
            tier = 5,
            icono = "📰",
            conceptos = listOf("hecho", "opinión", "noticia"),
            teoria = listOf(
                "Un hecho se puede comprobar; una opinión dice lo que alguien siente o piensa.",
                "«El agua hierve a 100 °C» es un hecho; «el agua es deliciosa» es una opinión."
            ),
            preguntas = listOf(
                p(
                    "«El cenote tiene agua azul». ¿Es hecho u opinión?",
                    listOf("Hecho.", "Opinión.", "Acertijo."),
                    0,
                    "Se puede ver y comprobar que el agua es azul.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«El cenote es el lugar más bonito». ¿Es hecho u opinión?",
                    listOf("Opinión.", "Hecho.", "Refrán."),
                    0,
                    "«El más bonito» depende de cada persona: es opinión.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál es una opinión?",
                    listOf(
                        "Los tacos son deliciosos.",
                        "Los tacos tienen tortilla.",
                        "Los tacos se venden en el tianguis."
                    ),
                    0,
                    "Decir que son deliciosos es un gusto personal.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 46,
            titulo = "El acertijo del mercado",
            subtitulo = "Piensa, descarta y adivina.",
            tier = 5,
            icono = "🧠",
            conceptos = listOf("acertijo", "deducción", "pista"),
            teoria = listOf(
                "Un acertijo da pistas y esconde la respuesta en ellas.",
                "Prueba cada pista y descarta lo que no cumpla todas."
            ),
            preguntas = listOf(
                p(
                    "Tiene hojas y no es árbol, tiene lomo y no es animal. ¿Qué es?",
                    listOf("Un libro.", "Una hoja de árbol.", "Un caballo."),
                    0,
                    "Un libro tiene hojas de papel y lomo, pero no es árbol ni animal.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "Cuanto más grande, menos se ve. ¿Qué es?",
                    listOf("La oscuridad.", "La luna.", "Una casa."),
                    0,
                    "Mientras más oscuro está, menos se ve.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "Tiene dientes y no muerde. ¿Qué es?",
                    listOf("Un peine.", "Un perro.", "Un tigre."),
                    0,
                    "Los dientes del peine no muerden.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 47,
            titulo = "El anuncio tramposo",
            subtitulo = "No todo lo que prometen es verdad.",
            tier = 5,
            icono = "📢",
            conceptos = listOf("anuncio", "exageración", "pensar"),
            teoria = listOf(
                "Los anuncios quieren convencerte; a veces exageran.",
                "Pregúntate si la promesa se puede comprobar."
            ),
            preguntas = listOf(
                p(
                    "«¡El único jabón que te hace invisible!». ¿Qué es exagerado?",
                    listOf(
                        "Que te hace invisible.",
                        "Que es un jabón.",
                        "Que se vende."
                    ),
                    0,
                    "Ningún jabón puede volverte invisible.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Qué pregunta conviene hacer ante un anuncio?",
                    listOf(
                        "¿Es verdad lo que promete?",
                        "¿De qué color es?",
                        "¿Quién lo inventó?"
                    ),
                    0,
                    "Lo importante es comprobar si la promesa es real.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«¡Corre, quedan pocos!» intenta...",
                    listOf(
                        "Apurarte a comprar.",
                        "Enseñarte a correr.",
                        "Contarte un cuento."
                    ),
                    0,
                    "La prisa es una técnica para que compres sin pensar.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 48,
            titulo = "La pregunta poderosa",
            subtitulo = "Preguntar también es pensar.",
            tier = 5,
            icono = "❓",
            conceptos = listOf("pregunta", "causa", "opinión"),
            teoria = listOf(
                "Las buenas preguntas abren la historia: ¿quién?, ¿qué?, ¿por qué?",
                "Hay preguntas de hechos y preguntas de opinión."
            ),
            preguntas = listOf(
                p(
                    "Leíste que un niño encontró una moneda. ¿Cuál pregunta ayuda a entender mejor?",
                    listOf(
                        "¿Dónde la encontró?",
                        "¿De qué color es el cielo?",
                        "¿Cuántos años tienes?"
                    ),
                    0,
                    "Saber dónde la encontró amplía la historia.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál pregunta es de causa?",
                    listOf("¿Por qué ocurrió?", "¿Qué hora es?", "¿Cómo te llamas?"),
                    0,
                    "«Por qué» busca la causa de algo.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál pregunta pide una opinión?",
                    listOf("¿Qué te pareció?", "¿Cuánto mide?", "¿Cuándo pasó?"),
                    0,
                    "«Qué te pareció» pide un punto de vista personal.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 49,
            titulo = "El debate de los alebrijes",
            subtitulo = "Escuchar, argumentar y respetar.",
            tier = 5,
            icono = "🦄",
            conceptos = listOf("debate", "argumento", "respeto"),
            teoria = listOf(
                "Un argumento explica POR QUÉ piensas algo.",
                "En un debate se escucha al otro y se responde con respeto."
            ),
            preguntas = listOf(
                p(
                    "Dos niños discuten si los alebrijes deben estar en museos o parques. ¿Cuál es un buen argumento?",
                    listOf(
                        "En el parque todos pueden verlos.",
                        "Porque sí.",
                        "Porque yo lo digo."
                    ),
                    0,
                    "La primera opción da una razón que se puede discutir.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Qué hace un buen debate?",
                    listOf(
                        "Escuchar y responder con respeto.",
                        "Gritar más fuerte.",
                        "No dejar hablar al otro."
                    ),
                    0,
                    "El respeto y la escucha son la base del debate.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "¿Cuál es una respuesta respetuosa?",
                    listOf(
                        "Entiendo tu idea, pero pienso que...",
                        "Estás mal y ya.",
                        "No me importa lo que digas."
                    ),
                    0,
                    "Reconocer la idea del otro y luego opinar es respetuoso.",
                    MecanicaLengua.CRITICO
                )
            )
        ),
        critico(
            numero = 50,
            titulo = "¡Continuará...!",
            subtitulo = "Gran final: todo el poder de las palabras.",
            tier = 5,
            icono = "✨",
            conceptos = listOf("metáfora", "símil", "absurdo", "final"),
            teoria = listOf(
                "Ya leíste, escuchaste, escribiste y pensaste como un gran lector.",
                "La próxima aventura de Zazi se está escribiendo... ¡y tú serás parte de ella!"
            ),
            preguntas = listOf(
                p(
                    "«La luna es una moneda de plata que rueda por el cielo». ¿Qué figura usa?",
                    listOf("Metáfora.", "Símil.", "Refrán."),
                    0,
                    "Dice que la luna ES una moneda: metáfora.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«El ajolote nada como una sonrisa». ¿Qué es?",
                    listOf("Símil.", "Metáfora.", "Acertijo."),
                    0,
                    "Usa «como» para comparar: símil.",
                    MecanicaLengua.CRITICO
                ),
                p(
                    "«El libro ladró y el perro leyó la historia». ¿Qué es?",
                    listOf("Absurdo.", "Hecho real.", "Noticia."),
                    0,
                    "Los libros no ladran: es una situación absurda.",
                    MecanicaLengua.CRITICO
                )
            )
        )
    )
}
