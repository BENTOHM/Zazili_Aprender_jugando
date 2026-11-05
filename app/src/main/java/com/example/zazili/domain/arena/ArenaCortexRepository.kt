package com.example.zazili.domain.arena

import androidx.compose.runtime.Immutable
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.minijuegos.ModuloMinijuego

/**
 * Ficha de diseño (GDD) de un minijuego dentro de la Arena Cortex.
 *
 * [juego] aporta id, título y las cinco consignas oficiales del catálogo
 * [Minijuego]; los demás campos describen cómo se siente el juego en pantalla
 * y cómo escala, para que el hub y los prototipos no inventen copy.
 */
@Immutable
data class FichaArena(
    val juego: Minijuego,
    val emoji: String,
    val gancho: String,
    val descripcion: String,
    val control: String,
    val escalaDificultad: String
) {
    val id: String get() = juego.id
    val titulo: String get() = juego.titulo
    val modulo: ModuloMinijuego get() = juego.modulo
    val niveles: List<String> get() = juego.niveles.map { it.consigna }
    val esJefe: Boolean get() = juego.esJefe
}

/** Una carretilla de la Arena: los cuatro juegos de una materia. */
@Immutable
data class SeccionArena(
    val modulo: ModuloMinijuego,
    val emoji: String,
    val alias: String,
    val fichas: List<FichaArena>
) {
    val totalJuegos: Int get() = fichas.size
}

/**
 * Tipo de juego que vive en un nivel de la Arena Cortex.
 *
 * Los 16 primeros niveles conservan uno a uno los minijuegos originales del
 * catálogo [Minijuego.todos]. Del nivel 17 en adelante entran los tres juegos
 * de tecnología y la secuencia rota sin fin: 17 bugs, 18 algoritmos, 19 filtro,
 * 20 bugs…
 *
 * Los nombres `LEGACY_GAME_N` son estables: la navegación y el mapa los usan
 * como llave, nunca como posición de una lista mutable.
 */
enum class MinigameType {
    LEGACY_GAME_1,
    LEGACY_GAME_2,
    LEGACY_GAME_3,
    LEGACY_GAME_4,
    LEGACY_GAME_5,
    LEGACY_GAME_6,
    LEGACY_GAME_7,
    LEGACY_GAME_8,
    LEGACY_GAME_9,
    LEGACY_GAME_10,
    LEGACY_GAME_11,
    LEGACY_GAME_12,
    LEGACY_GAME_13,
    LEGACY_GAME_14,
    LEGACY_GAME_15,
    LEGACY_GAME_16,
    WHACK_A_BUG,
    ALGORITHM_PATH,
    CYBER_SWIPE
}

/** Un nivel de la Arena ya resuelto: qué juego le toca y con qué ficha. */
@Immutable
data class NivelArena(
    val lessonId: Int,
    val tipo: MinigameType,
    val juegoLegado: Minijuego?
) {
    val esLegado: Boolean get() = juegoLegado != null
}

/**
 * Repositorio único de la Arena Cortex.
 *
 * Es la fuente de verdad del Game Design Document de los 16 minijuegos. No
 * conoce Compose ni navegación: solo entrega fichas inmutables y agrupaciones
 * por materia. Los identificadores coinciden EXACTAMENTE con [Minijuego.todos]
 * para que el motor adaptativo y el hub hablen el mismo idioma.
 */
object ArenaCortexRepository {

    /**
     * Id interno del módulo que hospeda la Arena Cortex. Es el 5 del currículo
     * (Emociones) y se nombra aquí para que navegación y mapa no repitan
     * números mágicos.
     */
    const val MODULO_ARENA_CORTEX = 5

    /** Los niveles 1 a 16 son los minijuegos originales. */
    const val NIVELES_LEGADOS = 16

    /** Primer nivel ocupado por los juegos nuevos de tecnología. */
    const val PRIMER_NIVEL_TECH = NIVELES_LEGADOS + 1

    /** Cada cuántos niveles vuelve a empezar la secuencia de tecnología. */
    const val NIVELES_POR_CICLO_TECH = 3

    /**
     * Una entrada por tipo legado, en el mismo orden de [Minijuego.todos]:
     * Matemáticas 1–4, Lenguaje 5–8, Emociones 9–12, Ciencias 13–16.
     */
    private val TIPOS_LEGADO: List<MinigameType> = listOf(
        MinigameType.LEGACY_GAME_1,
        MinigameType.LEGACY_GAME_2,
        MinigameType.LEGACY_GAME_3,
        MinigameType.LEGACY_GAME_4,
        MinigameType.LEGACY_GAME_5,
        MinigameType.LEGACY_GAME_6,
        MinigameType.LEGACY_GAME_7,
        MinigameType.LEGACY_GAME_8,
        MinigameType.LEGACY_GAME_9,
        MinigameType.LEGACY_GAME_10,
        MinigameType.LEGACY_GAME_11,
        MinigameType.LEGACY_GAME_12,
        MinigameType.LEGACY_GAME_13,
        MinigameType.LEGACY_GAME_14,
        MinigameType.LEGACY_GAME_15,
        MinigameType.LEGACY_GAME_16
    )

    private val TIPOS_TECH: List<MinigameType> = listOf(
        MinigameType.WHACK_A_BUG,
        MinigameType.ALGORITHM_PATH,
        MinigameType.CYBER_SWIPE
    )

    private data class DetalleArena(
        val emoji: String,
        val gancho: String,
        val descripcion: String,
        val control: String,
        val escalaDificultad: String
    )

    /**
     * Fuerza la inicialización del catálogo ANTES de tocar cualquier
     * `Minijuego.*`. Sin esta línea, la primera referencia sería
     * `Minijuego.CalculoMental`, cuyo `<clinit>` inicializa la superclase
     * `Minijuego`; ahí mismo `todos` lee `CalculoMental.INSTANCE` cuando aún
     * vale `null` y revienta con NPE. Entrar por `Minijuego.todos` primero
     * hace que la JVM inicialice cada objeto hijo de forma segura.
     */
    private val CATALOGO: List<Minijuego> = Minijuego.todos

    // Debe declararse antes de `fichas`: en un object de Kotlin los miembros
    // se inicializan en orden de aparición.
    private val DETALLES: Map<String, DetalleArena> = mapOf(
        // ------------------------- MATEMÁTICAS -------------------------
        Minijuego.CalculoMental.id to DetalleArena(
            emoji = "🧮",
            gancho = "Meteoros con ecuaciones",
            descripcion = "Resuelve la operación antes de que el meteoro toque el suelo. El teclado numérico rebota con cada toque.",
            control = "Toca los números del tablero 3D.",
            escalaDificultad = "La velocidad de caída y el tamaño de las cifras suben contigo."
        ),
        Minijuego.FraccionesFestivas.id to DetalleArena(
            emoji = "🪅",
            gancho = "Piñatas y roscas en fracciones",
            descripcion = "Corta la piñata o la rosca para repartir 1/2, 1/3 o 1/4 exactos. Ni un pedazo de más.",
            control = "Desliza el cuchillo de madera sobre el postre.",
            escalaDificultad = "Los cortes se vuelven más finos y aparecen equivalencias."
        ),
        Minijuego.GeometriaAncestral.id to DetalleArena(
            emoji = "🏛️",
            gancho = "Tangram maya",
            descripcion = "Acomoda las piezas del tangram para llenar la silueta del templo sin dejar huecos.",
            control = "Arrastra y gira cada pieza sobre el lienzo.",
            escalaDificultad = "Las siluetas suman vértices, perímetros y cuerpos en 3D."
        ),
        Minijuego.EstadisticaAlebrije.id to DetalleArena(
            emoji = "📊",
            gancho = "Atrapa frutas, arma la gráfica",
            descripcion = "Atrapa la fruta que pide la meta y colócala en una de las tres canastas para completar la gráfica de barras.",
            control = "Desliza la fruta hacia la canasta correcta.",
            escalaDificultad = "La meta cambia y las categorías se multiplican."
        ),

        // ------------------------- LENGUAJE -------------------------
        Minijuego.CodiceAnimado.id to DetalleArena(
            emoji = "📜",
            gancho = "Completa la historia y haz correr a la fauna",
            descripcion = "Lee la oración incompleta del códice y elige la palabra que la completa. Si aciertas, el jaguar corre, el colibrí revolotea, la tortuga camina o el mono salta.",
            control = "Toca la palabra o arrástrala hasta el hueco.",
            escalaDificultad = "De palabras de todos los días a vocabulario de leyenda y ciencia."
        ),
        Minijuego.PuenteOraciones.id to DetalleArena(
            emoji = "🌉",
            gancho = "Palabras que sostienen puentes",
            descripcion = "Arrastra las palabras en orden para tender el puente y cruzar el río sin caerte.",
            control = "Arrastra cada tabla de palabra a su lugar.",
            escalaDificultad = "Las oraciones crecen con adjetivos, adverbios y conjunciones."
        ),
        Minijuego.Cuentacuentos.id to DetalleArena(
            emoji = "📖",
            gancho = "Mitos con lupa",
            descripcion = "Lee un mito mexicano en texto biónico y usa la lupa para descubrir la palabra que esconde la pista.",
            control = "Desliza la lupa sobre el texto y toca la palabra.",
            escalaDificultad = "De recordar a inferir, predecir y encontrar metáforas."
        ),
        Minijuego.TlacuachePoeta.id to DetalleArena(
            emoji = "🎤",
            gancho = "Desliza sinónimos y antónimos",
            descripcion = "El tlacuache rapea una palabra y tú deslizas la tarjeta: derecha si es sinónimo, izquierda si es antónimo.",
            control = "Arrastra la tarjeta o usa los botones de dirección.",
            escalaDificultad = "El vocabulario se vuelve más fino y el reloj más corto."
        ),

        // ------------------------- EMOCIONES -------------------------
        Minijuego.MedidorTormentas.id to DetalleArena(
            emoji = "⛈️",
            gancho = "Primeros auxilios emocionales",
            descripcion = "Arrastra Escuchar, Respirar o Abrazar al alebrije que trae la tormenta y ayúdalo a calmarse.",
            control = "Arrastra la herramienta correcta hacia el alebrije.",
            escalaDificultad = "De reconocer enojo a acompañar emociones complejas."
        ),
        Minijuego.CaminoJaguar.id to DetalleArena(
            emoji = "🐆",
            gancho = "Decisiones con efecto mariposa",
            descripcion = "Elige el camino del jaguar y mira cómo tu decisión cambia el bosque varias escenas después.",
            control = "Toca una de las veredas de la encrucijada.",
            escalaDificultad = "Los dilemas suman matices y consecuencias lejanas."
        ),
        Minijuego.EspejoFortalezas.id to DetalleArena(
            emoji = "🪞",
            gancho = "Etiquetas convertidas en superpoderes",
            descripcion = "Une una etiqueta negativa con la reformulación que la convierte en una fortaleza real.",
            control = "Arrastra la etiqueta hacia su reflejo.",
            escalaDificultad = "De conocerte a reformular con orgullo."
        ),
        Minijuego.SombraMiedo.id to DetalleArena(
            emoji = "🌑",
            gancho = "Ritmo de pasos valientes",
            descripcion = "Toca los pasos valientes al ritmo para achicar la sombra del miedo y llegar a la puerta.",
            control = "Toca las notas al compás.",
            escalaDificultad = "El ritmo acelera y los pasos piden más valentía."
        ),

        // ------------------------- CIENCIAS -------------------------
        Minijuego.AlquimiaCenote.id to DetalleArena(
            emoji = "🧪",
            gancho = "Tuberías y estados del agua",
            descripcion = "Conecta tubos de calor y frío para llevar el agua del cenote a sólido, líquido o gas.",
            control = "Arrastra los codos de tubería al tablero.",
            escalaDificultad = "Los circuitos encadenan más cambios de estado."
        ),
        Minijuego.CadenasAlimenticias.id to DetalleArena(
            emoji = "🌿",
            gancho = "Red trófica sin cruces",
            descripcion = "Dibuja líneas que conecten quién come a quién sin cruzar las líneas que ya están en el tablero.",
            control = "Arrastra el dedo de un ser vivo a otro.",
            escalaDificultad = "De una cadena simple a redes con depredadores y descomponedores."
        ),
        Minijuego.FiltroMagico.id to DetalleArena(
            emoji = "💧",
            gancho = "Capas que limpian el cenote",
            descripcion = "Apila grava, arena, carbón y algodón en el orden que pide la pista de contaminación.",
            control = "Arrastra las capas a la torre del filtro.",
            escalaDificultad = "Más capas, arena fina y contaminantes químicos."
        ),
        Minijuego.MonstruoBasura.id to DetalleArena(
            emoji = "🗑️",
            gancho = "Banda transportadora",
            descripcion = "Desliza cada objeto a Orgánico, Inorgánico o Tóxico antes de que caiga en la banda.",
            control = "Desliza la basura hacia el bote correcto.",
            escalaDificultad = "La banda acelera y aparecen distractores."
        )
    )

    private val RESPALDO = DetalleArena(
        emoji = "✨",
        gancho = "Juego de la Arena",
        descripcion = "Un reto cortito para entrenar tu cerebro y subir de nivel.",
        control = "Toca para empezar.",
        escalaDificultad = "Cada nivel pide un poquito más."
    )

    val fichas: List<FichaArena> = CATALOGO.map { juego ->
        val detalle = DETALLES[juego.id] ?: RESPALDO
        FichaArena(
            juego = juego,
            emoji = detalle.emoji,
            gancho = detalle.gancho,
            descripcion = detalle.descripcion,
            control = detalle.control,
            escalaDificultad = detalle.escalaDificultad
        )
    }

    val secciones: List<SeccionArena> = listOf(
        SeccionArena(
            modulo = ModuloMinijuego.Matematicas,
            emoji = "🧮",
            alias = "Números que se mueven",
            fichas = fichasDe(ModuloMinijuego.Matematicas)
        ),
        SeccionArena(
            modulo = ModuloMinijuego.Lenguaje,
            emoji = "📚",
            alias = "Palabras que se vuelven juego",
            fichas = fichasDe(ModuloMinijuego.Lenguaje)
        ),
        SeccionArena(
            modulo = ModuloMinijuego.Emociones,
            emoji = "💜",
            alias = "Sentir, decidir y valiente",
            fichas = fichasDe(ModuloMinijuego.Emociones)
        ),
        SeccionArena(
            modulo = ModuloMinijuego.Ciencias,
            emoji = "🔬",
            alias = "El cenote como laboratorio",
            fichas = fichasDe(ModuloMinijuego.Ciencias)
        )
    )

    fun ficha(juegoId: String): FichaArena? =
        fichas.firstOrNull { it.id == juegoId }

    fun fichasDe(modulo: ModuloMinijuego): List<FichaArena> =
        fichas.filter { it.modulo == modulo }

    /**
     * Tipo de juego del nivel pedido. No hay techo: del 17 en adelante la
     * secuencia de tecnología se repite, así el mapa puede crecer sin límite
     * sin que ningún nodo se quede sin destino.
     */
    fun tipoDeNivel(lessonId: Int): MinigameType {
        val nivel = lessonId.coerceAtLeast(1)
        if (nivel <= NIVELES_LEGADOS) {
            return TIPOS_LEGADO[nivel - 1]
        }
        val indice = (nivel - PRIMER_NIVEL_TECH) % NIVELES_POR_CICLO_TECH
        return TIPOS_TECH[indice]
    }

    /**
     * Nivel completo: tipo más la ficha original cuando el nivel es legado.
     * [CATALOGO] se recorre aquí, nunca en el `<clinit>` del objeto, para no
     * adelantarse a la inicialización de [Minijuego.todos].
     */
    fun nivel(lessonId: Int): NivelArena {
        val id = lessonId.coerceAtLeast(1)
        val tipo = tipoDeNivel(id)
        val juego = if (id <= NIVELES_LEGADOS) CATALOGO.getOrNull(id - 1) else null
        return NivelArena(lessonId = id, tipo = tipo, juegoLegado = juego)
    }

    /**
     * Tabla base de la Arena: niveles 1 a 19, donde ya conviven los 16 juegos
     * originales y los tres de tecnología. Es la instantánea que pueden pintar
     * hubs o pruebas; el motor de navegación usa [nivel] para cualquier número.
     */
    val nivelesBase: List<NivelArena>
        get() = (1..(NIVELES_LEGADOS + NIVELES_POR_CICLO_TECH)).map { nivel(it) }
}
