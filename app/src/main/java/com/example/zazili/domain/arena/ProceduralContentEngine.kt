package com.example.zazili.domain.arena

import androidx.compose.runtime.Immutable
import com.example.zazili.domain.minijuegos.Minijuego
import kotlin.random.Random

/**
 * Motor de contenido procedural de la Arena Cortex.
 *
 * No hay niveles escritos a mano: cada reto se arma en el momento combinando
 * tres corpus (fauna mexicana, mitología mesoamericana y cultura pop) con las
 * plantillas de la mecánica del juego y los parámetros que entrega
 * [ZaziDifficultyEngine]. Un reto ocupa unos cientos de bytes y se descarta al
 * responder, así que la memoria no crece con las horas de juego.
 *
 * Todo el texto visible está en español mexicano y evita marcas registradas:
 * la cultura pop se describe con objetos y escenas, no con logotipos.
 */

// ---------------------------------------------------------------------------
//  CORPUS CULTURAL
// ---------------------------------------------------------------------------

@Immutable
enum class FuenteCultural(val etiqueta: String) {
    FAUNA_MEXICANA("Fauna mexicana"),
    MITOLOGIA("Mitología mesoamericana"),
    CULTURA_POP("Cultura pop contemporánea")
}

/**
 * Una pieza del corpus. [rasgo] se usa como complemento de oración ("El jaguar
 * es …"), [detalle] como dato curioso y [nivelTrofico] solo tiene sentido en la
 * fauna: 0 productor, 1 herbívoro, 2 carnívoro, 3 descomponedor.
 */
@Immutable
data class EntradaCultural(
    val id: String,
    val nombre: String,
    val plural: String,
    val emoji: String,
    val fuente: FuenteCultural,
    val rasgo: String,
    val detalle: String,
    val nivelTrofico: Int = -1
)

private fun fauna(
    id: String,
    nombre: String,
    plural: String,
    emoji: String,
    rasgo: String,
    detalle: String,
    trofico: Int
) = EntradaCultural(id, nombre, plural, emoji, FuenteCultural.FAUNA_MEXICANA, rasgo, detalle, trofico)

private fun mito(id: String, nombre: String, emoji: String, rasgo: String, detalle: String) =
    EntradaCultural(id, nombre, nombre, emoji, FuenteCultural.MITOLOGIA, rasgo, detalle)

private fun pop(id: String, nombre: String, plural: String, emoji: String, rasgo: String, detalle: String) =
    EntradaCultural(id, nombre, plural, emoji, FuenteCultural.CULTURA_POP, rasgo, detalle)

private val FAUNA: List<EntradaCultural> = listOf(
    fauna("jaguar", "jaguar", "jaguares", "🐆", "el felino sagrado de la selva maya", "Sus manchas forman un dibujo único, como una huella digital.", 2),
    fauna("colibri", "colibrí", "colibríes", "🐦", "el ave que puede volar hacia atrás", "Bate las alas unas ochenta veces por segundo.", 1),
    fauna("ajolote", "ajolote", "ajolotes", "🦎", "el anfibio que regenera su cuerpo", "Puede recuperar una pata y hasta parte del corazón.", 2),
    fauna("tlacuache", "tlacuache", "tlacuaches", "🦝", "el marsupial que se hace el dormido", "Es el único marsupial de Norteamérica.", 2),
    fauna("cacomixtle", "cacomixtle", "cacomixtles", "🦡", "el trepador de cola anillada", "Baja de los árboles de cabeza, como un pequeño acróbata.", 1),
    fauna("teporingo", "teporingo", "teporingos", "🐰", "el conejo de los volcanes", "Vive a más de tres mil metros y casi nadie lo ha visto.", 1),
    fauna("vaquita", "vaquita marina", "vaquitas marinas", "🐬", "la marsopa más pequeña del mundo", "Solo vive en el norte del Golfo de California.", 2),
    fauna("lobo", "lobo mexicano", "lobos mexicanos", "🐺", "el lobo gris más pequeño", "Su manada cría a los cachorros entre todos.", 2),
    fauna("aguila", "águila real", "águilas reales", "🦅", "el ave nacional de México", "Aparece en la bandera devorando una serpiente.", 2),
    fauna("guacamaya", "guacamaya roja", "guacamayas rojas", "🦜", "la gritona de la selva", "Vuela en pareja y no se separa ni para dormir.", 1),
    fauna("quetzal", "quetzal", "quetzales", "🦚", "el ave de plumas verdes que no vive en cautiverio", "Su cola mide hasta un metro y se llama cola de quetzal.", 1),
    fauna("ocelote", "ocelote", "ocelotes", "🐈", "el felino de manchas alargadas", "Nada muy bien y caza cerca del agua.", 2),
    fauna("puma", "puma", "pumas", "🐅", "el felino que no ruge, ronronea", "Puede saltar tres metros de alto desde parado.", 2),
    fauna("armadillo", "armadillo", "armadillos", "🦔", "el que carga su armadura", "Se enrolla como una pelota cuando siente peligro.", 2),
    fauna("monarca", "mariposa monarca", "mariposas monarca", "🦋", "la viajera que cruza el continente", "Recorre miles de kilómetros y regresa al mismo árbol.", 1),
    fauna("tortuga", "tortuga laúd", "tortugas laúd", "🐢", "la tortuga marina más grande", "Su caparazón es suave, como cuero.", 2),
    fauna("iguana", "iguana verde", "iguanas verdes", "🦎", "la que toma el sol sin prisa", "Puede quedarse quieta horas para no gastar energía.", 1),
    fauna("melipona", "abeja melipona", "abejas meliponas", "🐝", "la abeja sin aguijón de los mayas", "Su miel se usa desde hace más de mil años.", 1),
    fauna("murcielago", "murciélago magueyero", "murciélagos magueyeros", "🦇", "el que poliniza el maguey de noche", "Sin él no habría tequila ni mezcal.", 1),
    fauna("xolo", "xoloitzcuintle", "xoloitzcuintles", "🐕", "el perro sin pelo que guía a los ancestros", "Es una de las razas más antiguas del continente.", 2),
    fauna("maguey", "maguey", "magueyes", "🪴", "la planta que guarda agua en sus hojas", "Sus pencas pueden medir dos metros.", 0),
    fauna("maiz", "maíz", "maíces", "🌽", "el grano que sostiene la milpa", "En México existen más de sesenta razas de maíz.", 0),
    fauna("nopal", "nopal", "nopales", "🌵", "el cactus de las tunas", "Sus pencas tiernas se comen en ensalada.", 0),
    fauna("cempasuchil", "cempasúchil", "cempasúchiles", "🌼", "la flor de los muertos", "Su olor guía a las almas en noviembre.", 0),
    fauna("cacao", "cacao", "cacaos", "🌱", "el árbol del chocolate", "Los mayas lo bebían espumoso y amargo.", 0)
)

private val MITOS: List<EntradaCultural> = listOf(
    mito("quetzalcoatl", "Quetzalcóatl", "🐍", "la serpiente emplumada que trajo el maíz", "Enseñó a sembrar y a medir el tiempo."),
    mito("tezcatlipoca", "Tezcatlipoca", "🌑", "el espejo humeante que todo lo ve", "Su espejo muestra lo que uno no quiere mirar."),
    mito("tlaloc", "Tláloc", "⛈️", "el señor de la lluvia", "Sus ojos eran dos círculos de agua."),
    mito("huitzilopochtli", "Huitzilopochtli", "☀️", "el colibrí del sur que guio al pueblo mexica", "Le hablaba a su pueblo con el zumbido de un colibrí."),
    mito("coatlicue", "Coatlicue", "🗿", "la de falda de serpientes", "Sin ella, el sol no habría nacido."),
    mito("mictlantecuhtli", "Mictlantecuhtli", "💀", "el señor del lugar de los muertos", "Guardaba los huesos para que otros pudieran vivir."),
    mito("xochiquetzal", "Xochiquetzal", "🌸", "la de la flor y el canto", "Protegía a quienes tejían y bordaban."),
    mito("ehecatl", "Ehécatl", "🌬️", "el viento que abre camino", "Soplaba para mover las nubes hasta el campo."),
    mito("chalchiuhtlicue", "Chalchiuhtlicue", "💧", "la de la falda de jade", "Cuidaba ríos, lagos y cenotes."),
    mito("xolotl", "Xólotl", "🐕", "el que se negó a morir", "Se escondió en el maíz y por eso es perro y gemelo."),
    mito("itzpapalotl", "Itzpapálotl", "🦋", "la mariposa de obsidiana", "Sus alas eran cuchillos que brillaban de noche."),
    mito("cipactli", "Cipactli", "🐊", "el caimán que sostiene la tierra", "De su cuerpo se formó el cielo y el suelo."),
    mito("ahuizotl", "Ahuizotl", "🐾", "el que atrapa con la cola", "Vivía en el agua y cuidaba que nadie la ensuciara."),
    mito("alux", "alux", "👺", "el duende maya de la milpa", "Si le dejas ofrenda, cuida tu siembra."),
    mito("chaneque", "chaneque", "🧚", "el pequeño guardián del monte", "Se ríe como un pájaro para perder a los curiosos."),
    mito("nahual", "nahual", "🐺", "la persona que puede volverse animal", "Su forma cambia según lo que necesita proteger."),
    mito("llorona", "La Llorona", "🕯️", "la que llora junto al río", "Su historia se cuenta para que los niños no se alejen de noche."),
    mito("codice", "el códice", "📜", "el libro pintado que guarda la memoria", "Sus dibujos cuentan lo que las palabras no alcanzan.")
)

private val POP: List<EntradaCultural> = listOf(
    pop("taco", "taco al pastor", "tacos al pastor", "🌮", "el que gira en el trompo", "Se sirve con piña, cilantro y cebolla."),
    pop("lucha", "lucha libre", "luchas libres", "🤼", "la máscara que no se quita", "La llave más famosa se llama quebradora."),
    pop("mariachi", "mariachi", "mariachis", "🎺", "la música que llega de madrugada", "Su traje lleva botones de plata."),
    pop("pinata", "piñata", "piñatas", "🪅", "la que se rompe a palos", "Tiene siete picos y cada uno es un pecado."),
    pop("papelpicado", "papel picado", "papeles picados", "🎏", "el que tiembla con el viento", "Se corta con cincel y se cuelga en el tianguis."),
    pop("tianguis", "tianguis", "tianguis", "🛒", "el mercado que se pone cada semana", "Se arma desde las cuatro de la mañana."),
    pop("combi", "combi", "combis", "🚐", "la que grita la parada", "El chofer anuncia las calles a gritos."),
    pop("metro", "metro", "metros", "🚇", "el que conecta la ciudad", "Sus estaciones tienen escudos con animales."),
    pop("elote", "elote", "elotes", "🌽", "el que se come parado", "Con mayonesa, queso y chile se llama esquite."),
    pop("rosca", "rosca de reyes", "roscas de reyes", "🍩", "la que esconde un muñequito", "Quien encuentra el muñeco invita los tamales."),
    pop("atole", "atole", "atoles", "☕", "el que calienta las manos", "De guayaba, de piloncillo o de champurrado."),
    pop("alebrije", "alebrije", "alebrijes", "🐉", "el animal que no existe", "Se talla en madera de copal y se pinta a mano."),
    pop("molcajete", "molcajete", "molcajetes", "🥣", "el que muele el chile", "Es de piedra volcánica y pesa como un ladrillo."),
    pop("rebozo", "rebozo", "rebozos", "🧣", "el que se usa de mil formas", "Carga al bebé, abriga y sirve de bolsa."),
    pop("sonidero", "sonidero", "sonideros", "🔊", "el que prende la banqueta", "Su voz se oye a tres cuadras."),
    pop("arcade", "arcade", "arcades", "🕹️", "la máquina de fichas", "Se jugaba de pie y con los amigos mirando."),
    pop("portatil", "consola portátil", "consolas portátiles", "🎮", "la que cabe en la mochila", "Necesita batería y buenos pulgares."),
    pop("tamal", "tamal", "tamales", "🫔", "el que se envuelve en hoja", "Hay de rajas, de mole y de dulce."),
    pop("bici", "bicicleta pública", "bicicletas públicas", "🚲", "la que se renta por minuto", "Tiene canasta y un candado con código.")
)

/** Los tres corpus juntos: 62 piezas, siempre disponible en memoria (≈ 12 KB). */
internal val CORPUS_CULTURAL: List<EntradaCultural> = FAUNA + MITOS + POP

// ---------------------------------------------------------------------------
//  MODELO DE RETO
// ---------------------------------------------------------------------------

@Immutable
enum class FamiliaReto(val nombre: String) {
    NUMERICA("Números"),
    LEXICA("Palabras"),
    CLASIFICACION("Clasificar"),
    ORDEN("Ordenar"),
    RITMO("Ritmo"),
    DECISION("Decisión"),
    ESPACIAL("Espacio")
}

/**
 * Cuerpo de un reto. La pantalla del minijuego elige cómo dibujarlo; el
 * contenido no sabe nada de Compose.
 *
 * Regla de oro: una sola respuesta correcta por reto. Las opciones abiertas
 * (dilemas) también traen [Dilema.indiceCorrecto] porque el juego premia la
 * opción que cuida a los personajes, no cualquier elección.
 */
@Immutable
sealed interface ContenidoReto {

    val consigna: String
    val pista: String
    val familia: FamiliaReto

    data class OpcionMultiple(
        override val consigna: String,
        override val pista: String,
        override val familia: FamiliaReto,
        val enunciado: String,
        val opciones: List<String>,
        val indiceCorrecto: Int,
        val codigo: String = ""
    ) : ContenidoReto

    data class Ordenar(
        override val consigna: String,
        override val pista: String,
        val piezas: List<String>,
        val ordenCorrecto: List<Int>
    ) : ContenidoReto {
        override val familia: FamiliaReto get() = FamiliaReto.ORDEN
    }

    data class Clasificar(
        override val consigna: String,
        override val pista: String,
        val elemento: String,
        val categorias: List<String>,
        val indiceCorrecto: Int
    ) : ContenidoReto {
        override val familia: FamiliaReto get() = FamiliaReto.CLASIFICACION
    }

    data class Ritmo(
        override val consigna: String,
        override val pista: String,
        val patron: List<String>,
        val nBack: Int,
        val ventanaMs: Long
    ) : ContenidoReto {
        override val familia: FamiliaReto get() = FamiliaReto.RITMO
    }

    data class Dilema(
        override val consigna: String,
        override val pista: String,
        val situacion: String,
        val opciones: List<String>,
        val indiceCorrecto: Int,
        val eco: String
    ) : ContenidoReto {
        override val familia: FamiliaReto get() = FamiliaReto.DECISION
    }

    data class Espacio(
        override val consigna: String,
        override val pista: String,
        val piezas: List<String>,
        val ranuras: Int,
        val ordenCorrecto: List<Int>
    ) : ContenidoReto {
        override val familia: FamiliaReto get() = FamiliaReto.ESPACIAL
    }
}

/** Índice correcto del reto, para que cualquier pantalla evalúe igual. */
fun ContenidoReto.indiceCorrecto(): Int? = when (this) {
    is ContenidoReto.OpcionMultiple -> indiceCorrecto
    is ContenidoReto.Clasificar -> indiceCorrecto
    is ContenidoReto.Dilema -> indiceCorrecto
    else -> null
}

fun ContenidoReto.evaluar(indice: Int?): Boolean =
    indice != null && indiceCorrecto() == indice

/** Reto listo para pintar, con sus banderas de accesibilidad ya resueltas. */
@Immutable
data class RetoArena(
    val juegoId: String,
    val numero: Int,
    val nivel: Int,
    val semilla: Long,
    val contenido: ContenidoReto,
    val fuente: FuenteCultural,
    val entrada: EntradaCultural,
    val reglaActiva: String?,
    val textoBionico: Boolean,
    val resaltados: List<String>,
    val pistaDisponible: Boolean,
    val segundosSugeridos: Int
)

/** Cuántas combinaciones distintas puede producir un juego (para el GDD). */
@Immutable
data class Diversidad(
    val combinaciones: Long,
    val minutosEstimados: Long
)

private data class Generado(
    val contenido: ContenidoReto,
    val entrada: EntradaCultural
)

private data class ParOpciones(
    val opciones: List<String>,
    val indiceCorrecto: Int
)

@Immutable
private data class ParLexico(val palabra: String, val sinonimo: String, val antonimo: String)

private val PARES_LEXICOS: List<ParLexico> = listOf(
    ParLexico("rápido", "veloz", "lento"),
    ParLexico("grande", "enorme", "chiquito"),
    ParLexico("alegre", "contento", "triste"),
    ParLexico("valiente", "decidido", "miedoso"),
    ParLexico("callado", "silencioso", "ruidoso"),
    ParLexico("antiguo", "viejo", "nuevo"),
    ParLexico("brillante", "luminoso", "opaco"),
    ParLexico("difícil", "complicado", "sencillo"),
    ParLexico("amable", "gentil", "grosero"),
    ParLexico("fuerte", "robusto", "débil"),
    ParLexico("limpio", "aseado", "sucio"),
    ParLexico("sabroso", "delicioso", "insípido"),
    ParLexico("curioso", "preguntón", "indiferente"),
    ParLexico("tranquilo", "calmado", "inquieto")
)

private val FORTALEZAS: List<Pair<String, String>> = listOf(
    "distraído" to "tu atención salta a lo que te interesa",
    "inquieto" to "tu cuerpo necesita moverse para pensar",
    "tímido" to "tomas tu tiempo para confiar",
    "terco" to "defiendes lo que crees justo",
    "sensible" to "percibes lo que otros no alcanzan a ver",
    "lento" to "vas con cuidado y casi no te equivocas",
    "soñador" to "tu imaginación encuentra caminos nuevos",
    "platicador" to "conectas con la gente",
    "desordenado" to "tu cabeza ve conexiones raras y buenas",
    "impulsivo" to "actúas rápido cuando hay que actuar"
)

private data class EscenarioEmocional(val situacion: String, val herramienta: String, val eco: String)

private val ESCENARIOS_EMOCIONALES: List<EscenarioEmocional> = listOf(
    EscenarioEmocional("El alebrije perdió su partido y avienta el balón.", "Escuchar", "Se sentó, habló y el enojo bajó poquito."),
    EscenarioEmocional("La alebrije tiembla antes de pasar al pizarrón.", "Respirar", "Contó cuatro adentro y dos afuera; le tembló menos la voz."),
    EscenarioEmocional("El alebrije llora escondido detrás del árbol.", "Abrazar", "No estuvo solo; el llanto se volvió palabras."),
    EscenarioEmocional("Le quitaron su turno en la fila y grita.", "Respirar", "Primero el aire, luego la pregunta: ¿qué pasó?"),
    EscenarioEmocional("El alebrije se ríe de otro y el otro se encoge.", "Escuchar", "Escuchó al lastimado y pidió disculpas sin excusas."),
    EscenarioEmocional("La alebrije se queda muda cuando la regañan.", "Escuchar", "Alguien se sentó a su lado sin interrogar."),
    EscenarioEmocional("El alebrije rompe su dibujo y lo avienta.", "Respirar", "Respiró, recogió el papel y lo intentó otra vez."),
    EscenarioEmocional("Su amigo se muda y el alebrije no quiere hablar.", "Abrazar", "Un abrazo largo dijo lo que las palabras no."),
    EscenarioEmocional("El alebrije llega tarde y todos lo miran.", "Escuchar", "Le preguntaron qué le pasó antes de juzgar."),
    EscenarioEmocional("La alebrije siente celos de su hermano.", "Escuchar", "Nombró los celos y dejó de pelear consigo misma.")
)

private data class DilemaJaguar(val situacion: String, val opciones: List<String>, val correcta: Int, val eco: String)

private val DILEMAS_JAGUAR: List<DilemaJaguar> = listOf(
    DilemaJaguar(
        "Encuentras la mochila de un compañero con su lunch adentro. Tienes hambre.",
        listOf("La dejo donde está y aviso", "Me como el lunch y no digo nada", "La escondo para hacerle una broma"),
        0,
        "Tres escenas después, el compañero te presta su lápiz sin que se lo pidas."
    ),
    DilemaJaguar(
        "Un amigo te cuenta un secreto y otro amigo insiste en saberlo.",
        listOf("Le cuento para que no se enoje", "Le digo que no me toca contarlo", "Invento otra cosa para distraerlo"),
        1,
        "El secreto sigue guardado y tu amigo confía más en ti."
    ),
    DilemaJaguar(
        "Ves que se burlan de alguien en el recreo.",
        listOf("Me río para no quedar fuera", "Me voy y no me meto", "Me acerco y me pongo al lado"),
        2,
        "La persona burlada ya no se queda sola y contigo se puede hablar."
    ),
    DilemaJaguar(
        "Rompiste un vidrio con la pelota y nadie vio.",
        listOf("Digo la verdad aunque me regañen", "Lo dejo y corro", "Le echo la culpa al viento"),
        0,
        "Te regañan, pero al día siguiente te prestan la pelota otra vez."
    ),
    DilemaJaguar(
        "Tu equipo pierde y quieres dejar de jugar.",
        listOf("Me salgo sin decir nada", "Pido un minuto y sigo", "Grito que fue culpa de todos"),
        1,
        "El último minuto alcanza y el equipo aprende a pedir pausas."
    ),
    DilemaJaguar(
        "Ves a un niño nuevo comiendo solo.",
        listOf("Me siento con él", "Lo miro de lejos", "Le digo que se vaya a otra mesa"),
        0,
        "El niño nuevo te enseña un juego que nadie conocía."
    ),
    DilemaJaguar(
        "Encuentras dinero en el piso del salón.",
        listOf("Me lo guardo", "Pregunto de quién es", "Lo dejo en el piso"),
        1,
        "Era del de la tienda; te regala un dulce por devolverlo."
    ),
    DilemaJaguar(
        "Tu hermana usa tu cuaderno sin pedirlo.",
        listOf("Le rompo su hoja", "Le digo cómo me siento", "Me callo y me enojo todo el día"),
        1,
        "Al día siguiente te pide permiso, y tú también a ella."
    ),
    DilemaJaguar(
        "Un compañero copia tu tarea.",
        listOf("Le digo que no y le ofrezco explicarle", "Le doy la tarea", "Lo acuso con el maestro"),
        0,
        "Aprende el ejercicio y al siguiente examen le va mejor solo."
    ),
    DilemaJaguar(
        "Ganas un juego y tu amigo se queda callado.",
        listOf("Le presumo otra vez", "Le pregunto si quiere jugar otra", "Le digo que él juega mal"),
        1,
        "Juegan otra y esta vez los dos se ríen."
    )
)

private val BOTES_BASURA: List<String> = listOf("Orgánico", "Inorgánico", "Tóxico")

private val OBJETOS_BASURA: List<Pair<String, Int>> = listOf(
    "cáscara de plátano" to 0,
    "bolsa de plástico" to 1,
    "pila de control" to 2,
    "popote" to 1,
    "sobras de comida" to 0,
    "foco ahorrador" to 2,
    "botella de vidrio" to 1,
    "hoja seca" to 0,
    "aceite de cocina usado" to 2,
    "servilleta con grasa" to 0,
    "unicel" to 1,
    "lata de refresco" to 1,
    "medicamento caducado" to 2,
    "cáscara de huevo" to 0
)

private val CAPAS_FILTRO: List<String> = listOf(
    "grava gruesa",
    "arena",
    "carbón activado",
    "algodón",
    "arena fina",
    "malla metálica"
)

// ---------------------------------------------------------------------------
//  MOTOR
// ---------------------------------------------------------------------------

/**
 * Genera retos bajo demanda. Es sin estado: la semilla manda, así que el mismo
 * número de reto y los mismos parámetros producen siempre el mismo desafío
 * (repetible, testeable y sin sorpresas al recomponer la pantalla).
 */
class ProceduralContentEngine(private val semillaBase: Long = 2026L) {

    fun generar(
        juegoId: String,
        parametros: ParametrosDificultad,
        reglas: List<ReglaActiva> = emptyList(),
        numero: Int = 1,
        semillaExtra: Long = 0L
    ): RetoArena {
        val ajustes = reglas.map { it.regla.ajuste }.toSet()
        val semilla = semillaBase * 31L +
            juegoId.hashCode().toLong() * 17L +
            numero.toLong() * 131L +
            parametros.nivel.toLong() * 7L +
            semillaExtra
        val azar = Random(semilla)
        val generado = generarContenido(juegoId, parametros, ajustes, azar)
        val bionico = parametros.andamiaje.peso >= NivelAndamiaje.RESALTADO.peso
        val regla = reglas.lastOrNull()?.regla?.enunciado
        return RetoArena(
            juegoId = juegoId,
            numero = numero,
            nivel = parametros.nivel,
            semilla = semilla,
            contenido = generado.contenido,
            fuente = generado.entrada.fuente,
            entrada = generado.entrada,
            reglaActiva = regla,
            textoBionico = bionico,
            resaltados = if (bionico) listOf(generado.entrada.nombre) else emptyList(),
            // El apoyo invisible gana a la restricción: si el DDA ya encendió
            // el andamiaje, la pista vuelve aunque una regla atómica la ocultara.
            pistaDisponible = parametros.andamiaje.pistaSiempreVisible ||
                AjusteRegla.SIN_PISTA !in ajustes,
            segundosSugeridos = segundosEfectivos(parametros, ajustes)
        )
    }

    /** Variedad declarada para el GDD: combinaciones y minutos de juego. */
    fun diversidad(juegoId: String): Diversidad {
        val piezas = CORPUS_CULTURAL.size.toLong()
        val plantillas = plantillasDe(juegoId).toLong()
        val combinaciones = piezas * plantillas * Minijuego.NIVEL_MAXIMO * 12L
        // Un reto consume ~25 s entre lectura, respuesta y celebración.
        val minutos = (combinaciones * 25L) / 60L
        return Diversidad(combinaciones, minutos)
    }

    fun diversidadTotal(): Diversidad {
        val juegos = CATALOGO_ARENA.size.toLong()
        val combinaciones = CORPUS_CULTURAL.size.toLong() * 24L * Minijuego.NIVEL_MAXIMO * 12L * juegos
        return Diversidad(combinaciones, (combinaciones * 25L) / 60L)
    }

    // ------------------------- despacho -------------------------

    private fun generarContenido(
        juegoId: String,
        p: ParametrosDificultad,
        ajustes: Set<AjusteRegla>,
        azar: Random
    ): Generado = when (juegoId) {
        "calculo_mental" -> calculoMental(p, azar, ajustes)
        "fracciones_festivas" -> fraccionesFestivas(p, azar, ajustes)
        "geometria_ancestral" -> geometriaAncestral(p, azar)
        "estadistica_alebrije" -> estadisticaAlebrije(p, azar, ajustes)
        "codice_animado" -> codiceAnimado(p, azar, ajustes)
        "puente_oraciones" -> puenteOraciones(p, azar)
        "cuentacuentos" -> cuentacuentos(p, azar)
        "tlacuache_poeta" -> tlacuachePoeta(p, azar, ajustes)
        "medidor_tormentas" -> medidorTormentas(p, azar, ajustes)
        "camino_jaguar" -> caminoJaguar(p, azar, ajustes)
        "espejo_fortalezas" -> espejoFortalezas(p, azar, ajustes)
        "sombra_miedo" -> sombraMiedo(p, azar)
        "alquimia_cenote" -> alquimiaCenote(p, azar)
        "cadenas_alimenticias" -> cadenasAlimenticias(p, azar, ajustes)
        "filtro_magico" -> filtroMagico(p, azar)
        "monstruo_basura" -> monstruoBasura(p, azar)
        else -> generico(p, azar, ajustes)
    }

    // ------------------------- matemáticas -------------------------

    private fun calculoMental(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val entrada = FAUNA.random(azar)
        val cuantas = opcionesEfectivas(p, ajustes)
        return when {
            p.incluyeDecimales -> {
                val a = azar.nextInt(15, 90)
                val b = azar.nextInt(15, 60)
                val total = a + b
                val par = opcionesDe(
                    texto(total),
                    listOf(texto(total + 10), texto(total - 10), texto(total + 1), texto(total - 1)),
                    cuantas,
                    azar
                )
                Generado(
                    ContenidoReto.OpcionMultiple(
                        consigna = "Suma con pesos y centavos.",
                        pista = "Acomoda las comas antes de sumar.",
                        familia = FamiliaReto.NUMERICA,
                        enunciado = "En el tianguis pagas ${texto(a)} pesos por ${entrada.plural} y ${texto(b)} por el mandado. ¿Cuánto llevas gastado?",
                        opciones = par.opciones,
                        indiceCorrecto = par.indiceCorrecto
                    ),
                    entrada
                )
            }

            p.incluyeFracciones -> {
                val d = listOf(2, 3, 4, 6).random(azar)
                val n = azar.nextInt(1, d)
                val correcta = "$n/$d"
                val par = opcionesDe(
                    correcta,
                    listOf("${d - n}/$d", "$n/${d + 1}", "$d/$n", "${n + 1}/$d"),
                    cuantas,
                    azar
                )
                Generado(
                    ContenidoReto.OpcionMultiple(
                        consigna = "Reparte la rosca en partes iguales.",
                        pista = "El número de abajo dice en cuántos pedazos se partió.",
                        familia = FamiliaReto.NUMERICA,
                        enunciado = "Partes una rosca de reyes en $d pedazos iguales y tomas $n. ¿Qué fracción de la rosca te toca?",
                        opciones = par.opciones,
                        indiceCorrecto = par.indiceCorrecto
                    ),
                    entrada
                )
            }

            p.nivel <= 1 -> {
                val a = azar.nextInt(1, 8)
                val b = azar.nextInt(1, 8)
                val total = a + b
                val par = opcionesDe(total.toString(), distractoresNumericos(total, cuantas, azar), cuantas, azar)
                Generado(
                    ContenidoReto.OpcionMultiple(
                        consigna = "Cuenta los ${entrada.plural}.",
                        pista = "Suma primero los que ya tenías.",
                        familia = FamiliaReto.NUMERICA,
                        enunciado = "Ves $a ${entrada.plural} y luego llegan $b más. ¿Cuántos hay?",
                        opciones = par.opciones,
                        indiceCorrecto = par.indiceCorrecto
                    ),
                    entrada
                )
            }

            p.nivel == 2 -> {
                val a = azar.nextInt(11, 60)
                val b = azar.nextInt(2, 20)
                val suma = azar.nextBoolean()
                val total = if (suma) a + b else a - b
                val par = opcionesDe(total.toString(), distractoresNumericos(total, cuantas, azar), cuantas, azar)
                Generado(
                    ContenidoReto.OpcionMultiple(
                        consigna = if (suma) "Suma de dos cifras." else "Resta de dos cifras.",
                        pista = "Separa decenas y unidades.",
                        familia = FamiliaReto.NUMERICA,
                        enunciado = if (suma) {
                            "El tianguis tenía $a ${entrada.plural} y llegaron $b más. ¿Cuántos hay ahora?"
                        } else {
                            "Había $a ${entrada.plural} y se vendieron $b. ¿Cuántos quedan?"
                        },
                        opciones = par.opciones,
                        indiceCorrecto = par.indiceCorrecto
                    ),
                    entrada
                )
            }

            p.nivel == 3 -> {
                val a = azar.nextInt(2, 7)
                val b = azar.nextInt(2, 10)
                val total = a * b
                val par = opcionesDe(total.toString(), distractoresNumericos(total, cuantas, azar), cuantas, azar)
                Generado(
                    ContenidoReto.OpcionMultiple(
                        consigna = "Multiplica a la carrera.",
                        pista = "Puedes sumar el mismo número varias veces.",
                        familia = FamiliaReto.NUMERICA,
                        enunciado = "Hay $b cajas con $a ${entrada.plural} cada una. ¿Cuántos ${entrada.plural} hay en total?",
                        opciones = par.opciones,
                        indiceCorrecto = par.indiceCorrecto
                    ),
                    entrada
                )
            }

            else -> {
                val a = azar.nextInt(120, 900)
                val b = azar.nextInt(11, 99)
                val total = a - b
                val par = opcionesDe(total.toString(), distractoresNumericos(total, cuantas, azar), cuantas, azar)
                Generado(
                    ContenidoReto.OpcionMultiple(
                        consigna = "Resta grande, sin miedo.",
                        pista = "Quita primero las centenas completas.",
                        familia = FamiliaReto.NUMERICA,
                        enunciado = "El censo contó $a ${entrada.plural}; después se fueron $b. ¿Cuántos quedaron?",
                        opciones = par.opciones,
                        indiceCorrecto = par.indiceCorrecto
                    ),
                    entrada
                )
            }
        }
    }

    private fun fraccionesFestivas(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val entrada = POP.firstOrNull { it.id == "pinata" } ?: CORPUS_CULTURAL.random(azar)
        val cuantas = opcionesEfectivas(p, ajustes)
        val d = when (p.nivel) {
            1 -> 2
            2 -> listOf(3, 4).random(azar)
            3 -> listOf(4, 6, 8).random(azar)
            4 -> listOf(6, 8, 12).random(azar)
            else -> listOf(8, 12).random(azar)
        }
        val n = azar.nextInt(1, d)
        val correcta = "$n/$d"
        val par = opcionesDe(
            correcta,
            listOf("${d - n}/$d", "$n/${d + 1}", "$d/$n", "${n + 1}/$d", "1/${d + n}"),
            cuantas,
            azar
        )
        val enunciado = if (p.nivel >= 4) {
            "Cortas la piñata en $d partes y luego juntas $n. Si ya tenías ${n}/${d}, ¿qué fracción del total llevas?"
        } else {
            "La piñata se reparte en $d partes iguales y te tocan $n. ¿Qué fracción te toca?"
        }
        return Generado(
            ContenidoReto.OpcionMultiple(
                consigna = "Ni un pedazo de más.",
                pista = "Arriba van los pedazos que tomas; abajo, en cuántos se partió.",
                familia = FamiliaReto.NUMERICA,
                enunciado = enunciado,
                opciones = par.opciones,
                indiceCorrecto = par.indiceCorrecto
            ),
            entrada
        )
    }

    private fun geometriaAncestral(p: ParametrosDificultad, azar: Random): Generado {
        val entrada = MITOS.first { it.id == "codice" }
        val ranuras = (2 + p.nivel).coerceAtMost(5)
        val figuras = listOf("triángulo", "cuadrado", "trapecio", "rombo", "pentágono")
            .shuffled(azar)
            .take(ranuras)
        val (piezas, orden) = ordenarPasos(figuras, azar)
        return Generado(
            ContenidoReto.Espacio(
                consigna = "Acomoda el tangram sin dejar huecos.",
                pista = "Empieza por la pieza de abajo: sostiene a las demás.",
                piezas = piezas,
                ranuras = ranuras,
                ordenCorrecto = orden
            ),
            entrada
        )
    }

    private fun estadisticaAlebrije(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val entrada = FAUNA.filter { it.nivelTrofico > 0 }.random(azar)
        val otra = FAUNA.filter { it.id != entrada.id && it.nivelTrofico > 0 }.random(azar)
        val categorias = listOf("Seguro", "Probable", "Imposible")
        val a = azar.nextInt(0, 6)
        val b = azar.nextInt(0, 6)
        val correcta = when {
            b == 0 -> 0
            a == 0 -> 2
            else -> 1
        }
        val par = opcionesDe(
            categorias[correcta],
            categorias.filterIndexed { indice, _ -> indice != correcta },
            opcionesEfectivas(p, ajustes).coerceAtMost(3),
            azar
        )
        return Generado(
            ContenidoReto.OpcionMultiple(
                consigna = "Atrapa la fruta y arma la gráfica.",
                pista = "Si no hay ninguno, es imposible; si solo hay de ese, es seguro.",
                familia = FamiliaReto.CLASIFICACION,
                enunciado = "En la canasta hay $a ${entrada.plural} y $b ${otra.plural}. Sin mirar sacas uno. ¿Qué tan posible es que sea ${entrada.nombre}?",
                opciones = par.opciones,
                indiceCorrecto = par.indiceCorrecto
            ),
            entrada
        )
    }

    // ------------------------- lenguaje -------------------------

    private fun codiceAnimado(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val entrada = CORPUS_CULTURAL.random(azar)
        val cuantas = opcionesEfectivas(p, ajustes)
        val otras = CORPUS_CULTURAL.filter { it.id != entrada.id }.shuffled(azar).map { it.nombre }
        val par = opcionesDe(entrada.nombre, otras, cuantas, azar)
        val cuerpo = if (p.nivel >= 3) entrada.detalle else entrada.rasgo
        return Generado(
            ContenidoReto.OpcionMultiple(
                consigna = "Completa la oración del códice.",
                pista = "Lee toda la oración antes de elegir.",
                familia = FamiliaReto.LEXICA,
                enunciado = "«___ es $cuerpo».",
                opciones = par.opciones,
                indiceCorrecto = par.indiceCorrecto
            ),
            entrada
        )
    }

    private fun puenteOraciones(p: ParametrosDificultad, azar: Random): Generado {
        val entrada = FAUNA.random(azar)
        val palabras = when (p.nivel) {
            1 -> listOf("El ${entrada.nombre}", "vive", "en", "México")
            2 -> listOf("El ${entrada.nombre}", "cruza", "el", "cenote", "muy", "callado")
            3 -> listOf("Ayer", "el ${entrada.nombre}", "cruzó", "el", "cenote", "con", "cuidado")
            4 -> listOf("Cuando", "el ${entrada.nombre}", "cruzó", "el", "cenote", "los", "aluxes", "se", "rieron")
            else -> listOf("Aunque", "el ${entrada.nombre}", "cruzó", "el", "cenote", "los", "aluxes", "nunca", "lo", "vieron")
        }
        val (piezas, orden) = ordenarPasos(palabras, azar)
        return Generado(
            ContenidoReto.Ordenar(
                consigna = "Tiende el puente palabra por palabra.",
                pista = "Primero quién, luego qué hizo.",
                piezas = piezas,
                ordenCorrecto = orden
            ),
            entrada
        )
    }

    private fun cuentacuentos(p: ParametrosDificultad, azar: Random): Generado {
        val entrada = MITOS.random(azar)
        val testigo = FAUNA.random(azar)
        val ofrenda = listOf("maíz", "cacao", "agua", "flores").random(azar)
        val pasos = when (p.nivel) {
            1 -> listOf(
                "De noche, ${entrada.nombre} salió del cenote",
                "encontró a un ${testigo.nombre}",
                "y los dos volvieron al pueblo"
            )
            2 -> listOf(
                "De noche, ${entrada.nombre} salió del cenote",
                "encontró a un ${testigo.nombre}",
                "le compartió su $ofrenda",
                "y los dos volvieron al pueblo"
            )
            else -> listOf(
                "El pueblo pidió lluvia y nadie sabía qué hacer",
                "${entrada.nombre} salió del cenote al anochecer",
                "encontró a un ${testigo.nombre} que temblaba",
                "le compartió su $ofrenda sin pedir nada",
                "el ${testigo.nombre} lo guio hasta la milpa",
                "y al amanecer cayó la primera gota"
            )
        }
        val (piezas, orden) = ordenarPasos(pasos, azar)
        return Generado(
            ContenidoReto.Ordenar(
                consigna = "Ordena el relato con la lupa.",
                pista = "Busca la pista de tiempo: noche, después, amanecer.",
                piezas = piezas,
                ordenCorrecto = orden
            ),
            entrada
        )
    }

    private fun tlacuachePoeta(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val par = PARES_LEXICOS.random(azar)
        val entrada = CORPUS_CULTURAL.first { it.id == "tlacuache" }
        val cuantas = opcionesEfectivas(p, ajustes).coerceAtMost(4)
        val contenido = if (p.nivel <= 2) {
            val esSinonimo = azar.nextBoolean()
            val correcta = if (esSinonimo) "Sinónimos" else "Antónimos"
            val opciones = listOf("Sinónimos", "Antónimos").shuffled(azar)
            Generado(
                ContenidoReto.OpcionMultiple(
                    consigna = "Derecha si es sinónimo, izquierda si es antónimo.",
                    pista = "Sinónimo dice lo mismo; antónimo dice lo contrario.",
                    familia = FamiliaReto.LEXICA,
                    enunciado = "«${par.palabra}» y «${if (esSinonimo) par.sinonimo else par.antonimo}» son…",
                    opciones = opciones,
                    indiceCorrecto = opciones.indexOf(correcta)
                ),
                entrada
            )
        } else {
            val otros = PARES_LEXICOS.filter { it.palabra != par.palabra }.shuffled(azar)
            val distractores = buildList {
                add(par.sinonimo)
                otros.forEach {
                    add(it.antonimo)
                    add(it.sinonimo)
                }
            }
            val opciones = opcionesDe(par.antonimo, distractores, cuantas, azar)
            Generado(
                ContenidoReto.OpcionMultiple(
                    consigna = "El tlacuache rapea y tú contestas.",
                    pista = "Antónimo es lo contrario, no lo parecido.",
                    familia = FamiliaReto.LEXICA,
                    enunciado = "¿Cuál es el antónimo de «${par.palabra}»?",
                    opciones = opciones.opciones,
                    indiceCorrecto = opciones.indiceCorrecto
                ),
                entrada
            )
        }
        return contenido
    }

    private fun espejoFortalezas(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val (etiqueta, fortaleza) = FORTALEZAS.random(azar)
        val entrada = CORPUS_CULTURAL.first { it.id == "alebrije" }
        val cuantas = opcionesEfectivas(p, ajustes)
        val distractores = buildList {
            FORTALEZAS.filter { it.first != etiqueta }.shuffled(azar).forEach { add(it.second) }
            if (p.nivel >= 4) add("no eres $etiqueta, eso no existe")
        }
        val par = opcionesDe(fortaleza, distractores, cuantas, azar)
        return Generado(
            ContenidoReto.OpcionMultiple(
                consigna = "Convierte la etiqueta en superpoder.",
                pista = "Una fortaleza no niega la etiqueta: la usa a tu favor.",
                familia = FamiliaReto.LEXICA,
                enunciado = "Te dicen «$etiqueta». ¿Cuál reflejo te sirve de verdad?",
                opciones = par.opciones,
                indiceCorrecto = par.indiceCorrecto
            ),
            entrada
        )
    }

    // ------------------------- emociones -------------------------

    private fun medidorTormentas(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val escenario = ESCENARIOS_EMOCIONALES.random(azar)
        val herramientas = listOf("Escuchar", "Respirar", "Abrazar")
        val opciones = opcionesDe(
            escenario.herramienta,
            herramientas.filter { it != escenario.herramienta },
            opcionesEfectivas(p, ajustes).coerceAtMost(3),
            azar
        )
        return Generado(
            ContenidoReto.Dilema(
                consigna = "Elige el primer auxilio emocional.",
                pista = "¿Qué necesita primero: ser escuchado, aire o compañía?",
                situacion = escenario.situacion,
                opciones = opciones.opciones,
                indiceCorrecto = opciones.indiceCorrecto,
                eco = escenario.eco
            ),
            CORPUS_CULTURAL.first { it.id == "alebrije" }
        )
    }

    private fun caminoJaguar(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val dilema = DILEMAS_JAGUAR.random(azar)
        val cuantas = opcionesEfectivas(p, ajustes).coerceIn(2, dilema.opciones.size)
        val indices = dilema.opciones.indices.toList().shuffled(azar).take(cuantas)
        val reordenadas = indices.map { dilema.opciones[it] }
        return Generado(
            ContenidoReto.Dilema(
                consigna = "Elige una vereda; el bosque recuerda.",
                pista = "Piensa en la escena de mañana, no solo en la de hoy.",
                situacion = dilema.situacion,
                opciones = reordenadas,
                indiceCorrecto = reordenadas.indexOf(dilema.opciones[dilema.correcta]),
                eco = dilema.eco
            ),
            CORPUS_CULTURAL.first { it.id == "jaguar" }
        )
    }

    private fun sombraMiedo(p: ParametrosDificultad, azar: Random): Generado {
        val pasos = listOf("izquierda", "derecha", "salto", "paso")
        val largo = (3 + p.nivel).coerceAtMost(8)
        val patron = List(largo) { pasos.random(azar) }
        val ventana = ((p.segundosPorRonda * 1000L) / 6L).coerceIn(600L, 2_600L)
        return Generado(
            ContenidoReto.Ritmo(
                consigna = if (p.nBack <= 1) {
                    "Toca el paso cuando suene el tambor."
                } else {
                    "Toca solo cuando el paso sea igual al de hace ${p.nBack}."
                },
                pista = "Si te adelantas, la sombra crece; si esperas, se achica.",
                patron = patron,
                nBack = p.nBack,
                ventanaMs = ventana
            ),
            CORPUS_CULTURAL.first { it.id == "llorona" }
        )
    }

    // ------------------------- ciencias -------------------------

    private fun alquimiaCenote(p: ParametrosDificultad, azar: Random): Generado {
        val entrada = CORPUS_CULTURAL.first { it.id == "chalchiuhtlicue" }
        val pasos = when (p.nivel) {
            1 -> listOf(
                "El hielo se derrite y se vuelve agua",
                "El agua se calienta y sube como vapor"
            )
            2 -> listOf(
                "El hielo se derrite y se vuelve agua",
                "El agua se calienta y sube como vapor",
                "El vapor toca el vidrio frío y vuelve a ser gotas"
            )
            else -> listOf(
                "El sol calienta el agua del cenote",
                "El agua se evapora y sube como vapor",
                "El vapor se enfría arriba y forma nubes",
                "Las gotas caen otra vez sobre el cenote"
            )
        }
        val (piezas, orden) = ordenarPasos(pasos, azar)
        return Generado(
            ContenidoReto.Ordenar(
                consigna = "Conecta tubos: el agua cambia de estado.",
                pista = "El calor sube y el frío baja.",
                piezas = piezas,
                ordenCorrecto = orden
            ),
            entrada
        )
    }

    private fun cadenasAlimenticias(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val plantas = FAUNA.filter { it.nivelTrofico == 0 }
        val herbivoros = FAUNA.filter { it.nivelTrofico == 1 }
        val carnivoros = FAUNA.filter { it.nivelTrofico == 2 }
        val planta = plantas.random(azar)
        val herbivoro = herbivoros.random(azar)
        val cuantas = opcionesEfectivas(p, ajustes)
        val pregunta = if (p.nivel <= 2) {
            "En el cenote, ¿qué come el ${herbivoro.nombre}?"
        } else {
            "En esta red trófica, ¿quién come al ${herbivoro.nombre}?"
        }
        val correcta = if (p.nivel <= 2) planta.nombre else carnivoros.random(azar).nombre
        val distractores = buildList {
            plantas.forEach { if (it.nombre != correcta) add(it.nombre) }
            herbivoros.forEach { if (it.nombre != correcta) add(it.nombre) }
            carnivoros.forEach { if (it.nombre != correcta) add(it.nombre) }
        }
        val par = opcionesDe(correcta, distractores, cuantas, azar)
        return Generado(
            ContenidoReto.OpcionMultiple(
                consigna = "Dibuja la red trófica sin cruzar líneas.",
                pista = "La flecha va de quien come a quien es comido.",
                familia = FamiliaReto.CLASIFICACION,
                enunciado = pregunta,
                opciones = par.opciones,
                indiceCorrecto = par.indiceCorrecto
            ),
            herbivoro
        )
    }

    private fun filtroMagico(p: ParametrosDificultad, azar: Random): Generado {
        val capas = (2 + p.nivel).coerceAtMost(CAPAS_FILTRO.size)
        val elegidas = CAPAS_FILTRO.take(capas)
        val (piezas, orden) = ordenarPasos(elegidas, azar)
        return Generado(
            ContenidoReto.Espacio(
                consigna = "Apila las capas del filtro.",
                pista = "Lo más grueso va abajo; lo más fino, arriba.",
                piezas = piezas,
                ranuras = capas,
                ordenCorrecto = orden
            ),
            CORPUS_CULTURAL.first { it.id == "maguey" }
        )
    }

    private fun monstruoBasura(p: ParametrosDificultad, azar: Random): Generado {
        val (objeto, correcta) = OBJETOS_BASURA.random(azar)
        val entrada = CORPUS_CULTURAL.first { it.id == "nopal" }
        val categorias = if (p.nivel >= 3) BOTES_BASURA else BOTES_BASURA.take(2) + "Tóxico"
        return Generado(
            ContenidoReto.Clasificar(
                consigna = "Atrapa la basura antes de que caiga.",
                pista = "Lo que se descompone es orgánico; lo que envenena es tóxico.",
                elemento = objeto,
                categorias = categorias,
                indiceCorrecto = categorias.indexOf(BOTES_BASURA[correcta])
            ),
            entrada
        )
    }

    private fun generico(
        p: ParametrosDificultad,
        azar: Random,
        ajustes: Set<AjusteRegla>
    ): Generado {
        val entrada = CORPUS_CULTURAL.random(azar)
        val cuantas = opcionesEfectivas(p, ajustes)
        val otras = CORPUS_CULTURAL.filter { it.id != entrada.id }.shuffled(azar).map { it.nombre }
        val par = opcionesDe(entrada.nombre, otras, cuantas, azar)
        return Generado(
            ContenidoReto.OpcionMultiple(
                consigna = "Reto de la Arena.",
                pista = "Lee con calma y descarta.",
                familia = FamiliaReto.LEXICA,
                enunciado = "«___ es ${entrada.rasgo}».",
                opciones = par.opciones,
                indiceCorrecto = par.indiceCorrecto
            ),
            entrada
        )
    }

    // ------------------------- utilidades -------------------------

    private fun opcionesEfectivas(p: ParametrosDificultad, ajustes: Set<AjusteRegla>): Int {
        val extra = if (AjusteRegla.MAS_OPCIONES in ajustes) 1 else 0
        return (p.opciones + extra).coerceIn(2, 6)
    }

    private fun segundosEfectivos(p: ParametrosDificultad, ajustes: Set<AjusteRegla>): Int {
        val castigo = if (AjusteRegla.MENOS_TIEMPO in ajustes) 4 else 0
        val aire = p.andamiaje.aireExtraSegundos
        return (p.segundosPorRonda - castigo + aire).coerceIn(8, 40)
    }

    private fun plantillasDe(juegoId: String): Int = when (juegoId) {
        "calculo_mental" -> 12
        "fracciones_festivas" -> 8
        "geometria_ancestral" -> 6
        "estadistica_alebrije" -> 7
        "codice_animado" -> 10
        "puente_oraciones" -> 9
        "cuentacuentos" -> 8
        "tlacuache_poeta" -> 11
        "medidor_tormentas" -> 6
        "camino_jaguar" -> 7
        "espejo_fortalezas" -> 6
        "sombra_miedo" -> 9
        "alquimia_cenote" -> 7
        "cadenas_alimenticias" -> 8
        "filtro_magico" -> 6
        "monstruo_basura" -> 10
        else -> 6
    }

    private fun opcionesDe(
        correcta: String,
        candidatos: List<String>,
        cuantas: Int,
        azar: Random
    ): ParOpciones {
        val elegidas = LinkedHashSet<String>()
        elegidas.add(correcta)
        candidatos.filter { it.isNotBlank() && it != correcta }
            .shuffled(azar)
            .forEach { if (elegidas.size < cuantas) elegidas.add(it) }
        val lista = elegidas.toList().shuffled(azar)
        return ParOpciones(lista, lista.indexOf(correcta))
    }

    private fun distractoresNumericos(correcto: Int, cuantas: Int, azar: Random): List<String> {
        val salida = LinkedHashSet<String>()
        listOf(1, -1, 2, -2, 3, -3, 10, -10, 5, -5).shuffled(azar).forEach { delta ->
            val valor = correcto + delta
            if (valor >= 0 && valor != correcto && salida.size < cuantas) salida.add(valor.toString())
        }
        var relleno = 1
        while (salida.size < cuantas) {
            salida.add((correcto + relleno * 7).toString())
            relleno++
        }
        return salida.toList()
    }

    private fun ordenarPasos(correctos: List<String>, azar: Random): Pair<List<String>, List<Int>> {
        val mezcla = correctos.indices.toList().shuffled(azar)
        val piezas = mezcla.map { correctos[it] }
        val orden = correctos.indices.map { indice -> mezcla.indexOf(indice) }
        return piezas to orden
    }

    private fun texto(decimas: Int): String = "${decimas / 10},${decimas % 10}"

    private companion object {
        /** Mismo orden de inicialización que [ArenaCortexRepository]: entrar por `todos`. */
        val CATALOGO_ARENA: List<Minijuego> = Minijuego.todos
    }
}
