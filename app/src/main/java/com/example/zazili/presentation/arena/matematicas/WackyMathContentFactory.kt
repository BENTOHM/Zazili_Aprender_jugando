package com.example.zazili.presentation.arena.matematicas

import androidx.compose.runtime.Immutable
import kotlin.random.Random

/**
 * Fábrica procedural de la Arena Matemática.
 *
 * Combina sujetos, objetos, lugares, postres y ruinas mexicanas con ciencia
 * ficción absurda. Cada ronda recibe una semilla distinta (ronda + reto), así
 * que dos partidas nunca cuentan la misma historia. La fábrica también decide
 * el **mutador** de la ronda: botones que bailan, postres que giran, viento de
 * nahual o una barra tapada por un alebrije.
 *
 * Es puro Kotlin: no toca Compose, ViewModel ni navegación. Recibe una semilla
 * y devuelve estructuras inmutables listas para pintar.
 */

// ---------------------------------------------------------------------------
//  CÁLCULO MENTAL
// ---------------------------------------------------------------------------

/** Cómo se mueven los botones de respuesta. */
enum class MovimientoBotones { QUIETO, ORBITA, COHETE, RESPIRA }

@Immutable
data class MutadorCalculo(
    val movimiento: MovimientoBotones,
    val amplitudDp: Float,
    val etiqueta: String,
    val emoji: String
)

@Immutable
data class EscenaCalculo(
    val frase: String,
    val emoji: String,
    val mutador: MutadorCalculo
)

// ---------------------------------------------------------------------------
//  FRACCIONES FESTIVAS
// ---------------------------------------------------------------------------

/** Forma física del postre: anillo, pastel redondo o churro alargado. */
enum class FormaPostre { ANILLO, PASTEL, CHURRO }

@Immutable
data class TemaFraccion(
    val nombre: String,
    val emoji: String,
    val forma: FormaPostre,
    val giro: Float,
    val etiquetaMutador: String
)

// ---------------------------------------------------------------------------
//  GEOMETRÍA ANCESTRAL
// ---------------------------------------------------------------------------

@Immutable
data class Viento(
    val fuerza: Float,
    val etiqueta: String
)

@Immutable
data class TemaGeometria(
    val nombre: String,
    val emoji: String,
    val viento: Viento
)

// ---------------------------------------------------------------------------
//  ESTADÍSTICA ALEBRIJE
// ---------------------------------------------------------------------------

/** Con qué se dibuja cada voto de la gráfica. */
enum class EstiloGrafica { LUCHADORES, CHILES, ALFENIQUES }

@Immutable
data class TemaEstadistica(
    val estilo: EstiloGrafica,
    val etiquetaMutador: String,
    val emojiMutador: String,
    val indiceOculto: Int,
    val total: Int
) {
    val hayBarraOculta: Boolean get() = indiceOculto >= 0
}

// ---------------------------------------------------------------------------
//  FÁBRICA
// ---------------------------------------------------------------------------

object WackyMathContentFactory {

    private data class Sujeto(val texto: String, val emoji: String)
    private data class Objeto(val texto: String, val emoji: String)
    private data class Postre(val nombre: String, val emoji: String, val forma: FormaPostre)
    private data class Ruina(val nombre: String, val emoji: String)

    // ------------------------- datasets -------------------------

    private val SUJETOS = listOf(
        Sujeto("los ajolotes cibernéticos", "🦎"),
        Sujeto("los tlacuaches con rayo láser", "🦝"),
        Sujeto("las abuelitas ninjas", "👵"),
        Sujeto("los luchadores espaciales", "🤼"),
        Sujeto("los mariachis marcianos", "🎺"),
        Sujeto("los perros xoloitzcuintles astronautas", "🐕"),
        Sujeto("los nahuales de neón", "🐺"),
        Sujeto("los aluxes con patineta", "👽"),
        Sujeto("los chupacabras taqueros", "🌮"),
        Sujeto("las monjas mayas del futuro", "🧘"),
        Sujeto("los zombis de cempasúchil", "💀"),
        Sujeto("los concheros galácticos", "💫"),
        Sujeto("las catrinas hacker", "💃"),
        Sujeto("los eloteros interdimensionales", "🌽"),
        Sujeto("los alebrijes contadores", "🐉"),
        Sujeto("las sirenas de Chapultepec", "🧜"),
        Sujeto("los volcanes con tenis", "🌋"),
        Sujeto("los colibríes ninja", "🐦")
    )

    private val OBJETOS = listOf(
        Objeto("tacos al pastor cuánticos", "🌮"),
        Objeto("meteoritos de mole", "☄️"),
        Objeto("tamales de neón", "🫔"),
        Objeto("churros de antimateria", "🥖"),
        Objeto("elotes de otro planeta", "🌽"),
        Objeto("chiles en nogada radiactivos", "🌶️"),
        Objeto("piñatas de plutonio", "🪅"),
        Objeto("conchas voladoras", "🥐"),
        Objeto("aguacates ninja", "🥑"),
        Objeto("tortillas de holograma", "🫓"),
        Objeto("chicles de agujero negro", "🍬"),
        Objeto("marquesitas de Saturno", "🍪"),
        Objeto("flanes de gravedad cero", "🍮"),
        Objeto("aguas frescas de cometa", "🥤"),
        Objeto("quesadillas sin queso (¡traición!)", "🧀"),
        Objeto("pastelillos de plasma", "🧁")
    )

    private val LUGARES = listOf(
        "en el mercado de Xochimilco",
        "en la pirámide de Marte",
        "en el cenote de la cuarta dimensión",
        "en la taquería interestelar",
        "en el Metro de la CDMX a las 3 a.m.",
        "en la fiesta de la Luna",
        "en el puente de los alebrijes",
        "en el taller de Tepito galáctico",
        "en la esquina de la Vía Láctea",
        "en el taller de Doña Chelo, pero en 3025",
        "en el estadio de los volcanes",
        "en la feria de San Marcos de Neptuno",
        "en el tianguis de los sueños",
        "en la estación espacial de Tepoztlán"
    )

    private val VERBOS = listOf(
        "juntan",
        "esconden",
        "reparten",
        "lanzan",
        "coleccionan",
        "intercambian",
        "cuentan",
        "apilan"
    )

    private val MUTADORES_CALCULO = listOf(
        MutadorCalculo(MovimientoBotones.QUIETO, 0f, "Botones quietecitos", "🧘"),
        MutadorCalculo(MovimientoBotones.QUIETO, 0f, "Botones tranquilotes", "🍃"),
        MutadorCalculo(MovimientoBotones.ORBITA, 9f, "Botones bailarines", "💃"),
        MutadorCalculo(MovimientoBotones.ORBITA, 11f, "Botones de fiesta", "🪅"),
        MutadorCalculo(MovimientoBotones.COHETE, 8f, "Botones cohete", "🚀"),
        MutadorCalculo(MovimientoBotones.COHETE, 10f, "Botones saltarines", "🦘"),
        MutadorCalculo(MovimientoBotones.RESPIRA, 0f, "Botones que respiran", "🫧")
    )

    private val POSTRES = listOf(
        Postre("Churro de 10 metros", "🥖", FormaPostre.CHURRO),
        Postre("Rosca de Reyes radiactiva", "🍩", FormaPostre.ANILLO),
        Postre("Concha voladora", "🥐", FormaPostre.PASTEL),
        Postre("Pan de muerto con propulsión", "🍞", FormaPostre.PASTEL),
        Postre("Gelatina de agujero negro", "🍮", FormaPostre.ANILLO),
        Postre("Oreja de elefante cósmico", "🥠", FormaPostre.PASTEL),
        Postre("Empanada de meteorito", "🥟", FormaPostre.PASTEL),
        Postre("Tamal de neón", "🫔", FormaPostre.CHURRO),
        Postre("Buñuelo de Saturno", "🍩", FormaPostre.ANILLO),
        Postre("Polvorón de antimateria", "🍪", FormaPostre.PASTEL),
        Postre("Churro de otro planeta", "🥖", FormaPostre.CHURRO),
        Postre("Rosca de Saturno", "🍩", FormaPostre.ANILLO)
    )

    private val GIROS = listOf(0f, 0f, 16f, 22f, -18f, 30f)

    private val RUINAS = listOf(
        Ruina("Pirámide Invertida de Marte", "🔺"),
        Ruina("Máscara de Tláloc mecatrónica", "🤖"),
        Ruina("Templo de la Luna con WiFi", "🛕"),
        Ruina("Palacio de los Alebrijes", "🏛️"),
        Ruina("Observatorio de obsidiana flotante", "🔭"),
        Ruina("Glifo del Jaguar holográfico", "🐆"),
        Ruina("Juego de pelota de gravedad cero", "🏟️"),
        Ruina("Altar de cempasúchil galáctico", "💀")
    )

    private val VIENTOS = listOf(0f, 0f, 26f, 38f, -30f, -46f, 58f, -62f)

    // ------------------------- generadores -------------------------

    /** Escena absurda + mutador de movimiento para Cálculo Mental. */
    fun escenaCalculo(semilla: Int): EscenaCalculo {
        val azar = Random(semilla)
        val sujeto = SUJETOS.random(azar)
        val objeto = OBJETOS.random(azar)
        val lugar = LUGARES.random(azar)
        val verbo = VERBOS.random(azar)
        val frase = "${sujeto.texto.replaceFirstChar { it.uppercaseChar() }} " +
            "$verbo ${objeto.texto} $lugar."
        return EscenaCalculo(
            frase = frase,
            emoji = "${sujeto.emoji}${objeto.emoji}",
            mutador = MUTADORES_CALCULO.random(azar)
        )
    }

    /** Postre absurdo + giro lento de las zonas de soltado. */
    fun temaFraccion(semilla: Int): TemaFraccion {
        val azar = Random(semilla)
        val postre = POSTRES.random(azar)
        val giro = GIROS.random(azar)
        val etiqueta = if (giro == 0f) {
            "Postre quietecito"
        } else {
            "Mutador: el postre gira solo"
        }
        return TemaFraccion(
            nombre = postre.nombre,
            emoji = postre.emoji,
            forma = postre.forma,
            giro = giro,
            etiquetaMutador = etiqueta
        )
    }

    /** Ruina absurda + viento del Nahual para el arrastre. */
    fun temaGeometria(semilla: Int): TemaGeometria {
        val azar = Random(semilla)
        val ruina = RUINAS.random(azar)
        val fuerza = VIENTOS.random(azar)
        val viento = when {
            fuerza == 0f -> Viento(0f, "Aire quieto: el Nahual duerme")
            fuerza > 0f -> Viento(
                fuerza,
                if (fuerza >= 50f) "¡Nahual furioso sopla a la derecha!" else
                    "El Nahual sopla a la derecha"
            )
            else -> Viento(
                fuerza,
                if (fuerza <= -50f) "¡Nahual furioso sopla a la izquierda!" else
                    "El Nahual sopla a la izquierda"
            )
        }
        return TemaGeometria(nombre = ruina.nombre, emoji = ruina.emoji, viento = viento)
    }

    /**
     * Estilo de pictograma y barra oculta para Estadística.
     *
     * La barra tapada nunca es la que responde la pregunta: se excluyen el
     * máximo, el mínimo y cualquier fruta nombrada en el texto. Así el niño
     * puede deducir su valor con la suma total sin que el reto se vuelva
     * imposible.
     */
    fun temaEstadistica(
        semilla: Int,
        barras: List<BarraDato>,
        pregunta: String
    ): TemaEstadistica {
        val azar = Random(semilla)
        val estilo = EstiloGrafica.entries.random(azar)
        val total = barras.sumOf { it.valor }
        val mencion = pregunta.lowercase()

        val excluidos = mutableSetOf<Int>()
        barras.indices.maxByOrNull { barras[it].valor }?.let(excluidos::add)
        barras.indices.minByOrNull { barras[it].valor }?.let(excluidos::add)
        barras.forEachIndexed { indice, barra ->
            if (barra.etiqueta.lowercase() in mencion) excluidos.add(indice)
        }

        val candidato = barras.indices.firstOrNull { it !in excluidos }
        val esconder = candidato != null &&
            !mencion.contains("total") &&
            azar.nextInt(100) < 70
        val indiceOculto = if (esconder) candidato ?: -1 else -1

        val (etiqueta, emoji) = when {
            indiceOculto >= 0 -> "El alebrije tapó una barra: usa el total" to "🪽"
            estilo == EstiloGrafica.LUCHADORES -> "Luchadores apilados" to "🤼"
            estilo == EstiloGrafica.CHILES -> "Chiles en nogada" to "🌶️"
            else -> "Alfeñiques de azúcar" to "🍬"
        }

        return TemaEstadistica(
            estilo = estilo,
            etiquetaMutador = etiqueta,
            emojiMutador = emoji,
            indiceOculto = indiceOculto,
            total = total
        )
    }
}
