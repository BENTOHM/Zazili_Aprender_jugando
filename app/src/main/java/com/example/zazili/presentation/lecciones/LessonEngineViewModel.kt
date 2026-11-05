package com.example.zazili.presentation.lecciones

import androidx.compose.runtime.Immutable
import com.example.zazili.domain.curriculum.tecnologia.Lesson
import com.example.zazili.domain.curriculum.tecnologia.TechnologyModuleRepository
import com.example.zazili.domain.modulos.ModuloZazili
import kotlin.random.Random

/**
 * Una opción con su veredicto ya resuelto.
 *
 * La UI nunca compara letras ni posiciones: pinta la lista tal como llega y,
 * cuando el niño toca una tarjeta, consulta [isCorrect].
 */
@Immutable
data class AnswerOption(
    val text: String,
    val isCorrect: Boolean
)

/**
 * Sesión de Tecnología lista para el motor de lecciones.
 *
 * [bloque] es el grupo de cinco lecciones que se juega de una sentada y es el
 * identificador con el que se guarda el avance en la saga. [teoria] solo llega
 * en el primer bloque de cada capítulo para no repetir la misma cápsula.
 */
@Immutable
data class SesionTecnologia(
    val bloque: Int,
    val ejercicios: List<LessonExercise>,
    val teoria: TheoryContent?
)

/**
 * Motor de opciones de las lecciones de Lenguaje y Ciencias.
 *
 * El currículo guarda la respuesta como un índice. Al cargar cada reto,
 * [LessonEngine.barajar] convierte las opciones en [AnswerOption], las mezcla
 * con `.shuffled()` y reescribe el índice del ejercicio para que siga apuntando
 * al mismo texto. El barajado ocurre una sola vez, antes de entrar a la cola
 * del [LessonViewModel]; así una recomposición nunca cambia las casillas a
 * media pregunta y la respuesta tampoco se queda clavada en la letra A.
 *
 * Tecnología comparte exactamente la misma tubería: el módulo 4 se traduce a
 * [ModuloZazili.PIRAMIDE_CODIGO], sus 50 lecciones se agrupan en bloques de
 * cinco y cada una se convierte en un [MathExercise] que la pantalla de
 * opciones ya sabe pintar.
 */
object LessonEngine {

    /** Número de módulo que usa la ruta de navegación para Tecnología. */
    const val MODULO_TECNOLOGIA_ID = 4

    /** Identificadores numéricos estables que usa la navegación por módulo. */
    const val MODULO_MATEMATICAS_ID = 1
    const val MODULO_LENGUAJE_ID = 2
    const val MODULO_CIENCIAS_ID = 3
    const val MODULO_EMOCIONES_ID = 5

    /** Cada sesión cubre cinco lecciones del currículo de Tecnología. */
    const val LECCIONES_POR_BLOQUE = 5

    const val BLOQUES_TECNOLOGIA =
        TechnologyModuleRepository.TOTAL_LECCIONES / LECCIONES_POR_BLOQUE

    /**
     * Traduce el argumento `moduleId` de la ruta al módulo real. Cualquier
     * número desconocido devuelve `null` y la navegación regresa a un destino
     * válido en lugar de dejar la pantalla en blanco.
     */
    fun moduloDeId(moduleId: Int?): ModuloZazili? = when (moduleId) {
        MODULO_MATEMATICAS_ID -> ModuloZazili.TIANGUIS_NUMERICO
        MODULO_LENGUAJE_ID -> ModuloZazili.CUEVA_LEYENDAS
        MODULO_CIENCIAS_ID -> ModuloZazili.LABORATORIO_CENOTE
        MODULO_TECNOLOGIA_ID -> ModuloZazili.PIRAMIDE_CODIGO
        MODULO_EMOCIONES_ID -> ModuloZazili.ARBOL_ABRAZOS
        else -> null
    }

    /**
     * Ruta inversa, para que el Tablero construya la misma URL que lee el grafo
     * y para que el progreso se guarde con una llave estable por módulo.
     */
    fun idDeModulo(modulo: ModuloZazili): Int? = when (modulo) {
        ModuloZazili.TIANGUIS_NUMERICO -> MODULO_MATEMATICAS_ID
        ModuloZazili.CUEVA_LEYENDAS -> MODULO_LENGUAJE_ID
        ModuloZazili.LABORATORIO_CENOTE -> MODULO_CIENCIAS_ID
        ModuloZazili.PIRAMIDE_CODIGO -> MODULO_TECNOLOGIA_ID
        ModuloZazili.ARBOL_ABRAZOS -> MODULO_EMOCIONES_ID
        ModuloZazili.NIDO_ADULTOS -> null
    }

    /** Identificador estable del avance de un bloque en la saga. */
    fun nodoDeBloque(bloque: Int): String =
        "tecnologia_bloque_${bloque.coerceIn(1, BLOQUES_TECNOLOGIA)}"

    /**
     * Lee el nodo pedido sin romperse con enlaces viejos.
     *
     * Acepta `tecnologia_bloque_3`, `tecnologia_13` (lección suelta) y los
     * históricos `piramide_1`…`piramide_4` que venían del mapa anterior.
     */
    fun bloqueDeNodo(nodoId: String?): Int? {
        val limpio = nodoId?.trim().orEmpty()
        if (limpio.isEmpty()) return null
        val numero = limpio.substringAfterLast('_').toIntOrNull() ?: return null
        return when {
            limpio.startsWith("tecnologia_bloque_") ->
                numero.takeIf { it in 1..BLOQUES_TECNOLOGIA }

            limpio.startsWith("tecnologia_") ->
                numero.takeIf { it in 1..TechnologyModuleRepository.TOTAL_LECCIONES }
                    ?.let { (it - 1) / LECCIONES_POR_BLOQUE + 1 }

            limpio.startsWith("piramide_") ->
                numero.takeIf { it in 1..BLOQUES_TECNOLOGIA }

            else -> null
        }
    }

    /**
     * Arma la siguiente sesión de Tecnología.
     *
     * Si la ruta pide un bloque concreto se respeta; si no, arranca en el
     * primer bloque sin completar y, cuando ya no queda ninguno, repite el
     * último para que la pantalla nunca se quede vacía.
     */
    fun sesionDeTecnologia(
        nodoId: String?,
        completados: Set<String>
    ): SesionTecnologia {
        val bloque = bloqueDeNodo(nodoId)
            ?: (1..BLOQUES_TECNOLOGIA).firstOrNull { nodoDeBloque(it) !in completados }
            ?: BLOQUES_TECNOLOGIA
        return SesionTecnologia(
            bloque = bloque,
            ejercicios = ejerciciosDeTecnologia(bloque),
            teoria = teoriaDeBloque(bloque)
        )
    }

    /**
     * Convierte las opciones crudas en [AnswerOption] y las mezcla.
     *
     * Si el índice correcto llegara fuera de rango (dato corrupto), todas las
     * opciones quedan como incorrectas y la lección muestra retroalimentación
     * en lugar de tumbar la app.
     */
    fun opcionesBarajadas(
        opciones: List<String>,
        indiceCorrecto: Int,
        random: Random = Random.Default
    ): List<AnswerOption> {
        if (opciones.isEmpty()) return emptyList()
        val correcto = indiceCorrecto.takeIf { it in opciones.indices }
        return opciones
            .mapIndexed { indice, texto ->
                AnswerOption(text = texto, isCorrect = indice == correcto)
            }
            .shuffled(random)
    }

    /**
     * Envuelve las opciones de un ejercicio que ya pasó por [barajar].
     *
     * No vuelve a mezclar: el orden que ve el niño es el que quedó guardado en
     * la cola de la lección.
     */
    fun opcionesDe(ejercicio: LessonExercise): List<AnswerOption> =
        ejercicio.opciones.mapIndexed { indice, texto ->
            AnswerOption(text = texto, isCorrect = indice == ejercicio.indiceCorrecto)
        }

    /**
     * Devuelve una copia del ejercicio con las opciones mezcladas y el índice
     * correcto recalculado. El objeto original no se modifica.
     */
    fun barajar(
        ejercicio: LessonExercise,
        random: Random = Random.Default
    ): LessonExercise {
        if (ejercicio.opciones.size < 2) return ejercicio
        if (ejercicio.indiceCorrecto !in ejercicio.opciones.indices) return ejercicio

        val barajadas = opcionesBarajadas(
            opciones = ejercicio.opciones,
            indiceCorrecto = ejercicio.indiceCorrecto,
            random = random
        )
        val textos = barajadas.map { it.text }
        val indice = barajadas.indexOfFirst { it.isCorrect }.coerceAtLeast(0)

        return when (ejercicio) {
            is MathExercise -> ejercicio.copy(opciones = textos, indiceCorrecto = indice)
            is LanguageExercise -> ejercicio.copy(opciones = textos, indiceCorrecto = indice)
            is ScienceExercise -> ejercicio.copy(opciones = textos, indiceCorrecto = indice)
            is CalculoMentalExercise -> ejercicio.copy(opciones = textos, indiceCorrecto = indice)
        }
    }

    // -----------------------------------------------------------------------
    //  TECNOLOGÍA (MÓDULO 4)
    // -----------------------------------------------------------------------

    private fun ejerciciosDeTecnologia(bloque: Int): List<LessonExercise> {
        val inicio = (bloque - 1) * LECCIONES_POR_BLOQUE
        return (0 until LECCIONES_POR_BLOQUE).mapNotNull { posicion ->
            val leccion = TechnologyModuleRepository.porId(inicio + posicion + 1)
                ?: return@mapNotNull null
            ejercicioDeTecnologia(leccion, reto = posicion + 1)
        }
    }

    /**
     * Un [Lesson] del currículo de Tecnología dentro del contrato que ya pinta
     * [ExerciseScreen]. La respuesta correcta viaja como índice de texto, así
     * que [barajar] puede reordenar sin perderla.
     */
    private fun ejercicioDeTecnologia(leccion: Lesson, reto: Int): MathExercise {
        val capitulo = TechnologyModuleRepository.tituloDeCapituloDe(leccion.id)
        val indiceCorrecto = leccion.options
            .indexOfFirst { it.isCorrect }
            .takeIf { it in leccion.options.indices }
            ?: 0
        val nivel = capituloDeLeccion(leccion.id)
        return MathExercise(
            id = "tecnologia_${leccion.id}",
            titulo = "Tecnología · $capitulo",
            consigna = "Reto $reto de $LECCIONES_POR_BLOQUE · $capitulo",
            pista = pistaDeCapitulo(nivel),
            enunciado = leccion.question,
            opciones = leccion.options.map { it.text },
            indiceCorrecto = indiceCorrecto,
            dificultad = nivel,
            conceptosDificiles = palabrasClaveDeCapitulo(nivel),
            guiaFoco = guiaFocoDeCapitulo(nivel),
            icono = iconoDeCapitulo(nivel),
            etiquetaReto = "RETO DE TECNOLOGÍA"
        )
    }

    private fun capituloDeLeccion(id: Int): Int =
        ((id - 1) / TechnologyModuleRepository.LECCIONES_POR_CAPITULO + 1)
            .coerceIn(1, TechnologyModuleRepository.TOTAL_CAPITULOS)

    private fun pistaDeCapitulo(capitulo: Int): String = when (capitulo) {
        1 -> "El hardware se toca: monitor, teclado, ratón y CPU. Cada pieza tiene una función."
        2 -> "El software son programas e instrucciones: apps, sistema operativo y archivos."
        3 -> "En internet cuida tus datos: no compartas contraseñas ni información personal con extraños."
        4 -> "Un algoritmo es como una receta: pasos en orden. Los bucles repiten y los condicionales deciden."
        else -> "Un programa es un conjunto de instrucciones. Un evento dispara código y las pruebas encuentran errores."
    }

    private fun palabrasClaveDeCapitulo(capitulo: Int): List<String> = when (capitulo) {
        1 -> listOf("hardware", "dispositivo", "CPU")
        2 -> listOf("software", "app", "sistema operativo")
        3 -> listOf("contraseña", "privacidad", "huella digital")
        4 -> listOf("algoritmo", "secuencia", "bucle")
        else -> listOf("programa", "evento", "prueba")
    }

    private fun guiaFocoDeCapitulo(capitulo: Int): List<String> = when (capitulo) {
        1 -> listOf("Se toca = hardware", "Cada pieza tiene una función")
        2 -> listOf("Programa = instrucciones", "Actualizar = mejorar")
        3 -> listOf("No compartas contraseñas", "Avisa a un adulto")
        4 -> listOf("Orden = secuencia", "Si pasa esto, entonces…")
        else -> listOf("Entiende antes de programar", "Probar = revisar cada caso")
    }

    private fun iconoDeCapitulo(capitulo: Int): String = when (capitulo) {
        1 -> "💻"
        2 -> "📱"
        3 -> "🔒"
        4 -> "🧩"
        else -> "⚙️"
    }

    /**
     * Cápsula de teoría solo al abrir cada capítulo. Cada capítulo ocupa dos
     * bloques de cinco lecciones, así que la cápsula aparece en los bloques
     * impares y el segundo bloque entra directo al reto.
     */
    private fun teoriaDeBloque(bloque: Int): TheoryContent? {
        if (bloque !in 1..BLOQUES_TECNOLOGIA) return null
        val capitulo = capituloDeBloque(bloque)
        val bloquesPorCapitulo =
            TechnologyModuleRepository.LECCIONES_POR_CAPITULO / LECCIONES_POR_BLOQUE
        if ((bloque - 1) % bloquesPorCapitulo != 0) return null
        return TheoryContent(
            id = nodoDeBloque(bloque),
            titulo = "Tecnología · ${TechnologyModuleRepository.tituloDeCapituloDe(leccionDeBloque(bloque))}",
            subtitulo = "Bloque $bloque de $BLOQUES_TECNOLOGIA",
            icono = iconoDeCapitulo(capitulo),
            bullets = teoriaDeCapitulo(capitulo)
        )
    }

    private fun capituloDeBloque(bloque: Int): Int =
        ((bloque - 1) * LECCIONES_POR_BLOQUE / TechnologyModuleRepository.LECCIONES_POR_CAPITULO + 1)
            .coerceIn(1, TechnologyModuleRepository.TOTAL_CAPITULOS)

    private fun leccionDeBloque(bloque: Int): Int =
        ((bloque - 1) * LECCIONES_POR_BLOQUE + 1)
            .coerceIn(1, TechnologyModuleRepository.TOTAL_LECCIONES)

    private fun teoriaDeCapitulo(capitulo: Int): List<String> = when (capitulo) {
        1 -> listOf(
            "El hardware son las partes que puedes tocar: monitor, teclado, ratón y CPU.",
            "Cada pieza tiene una función: ver, escribir, escuchar o guardar.",
            "Cuidar el equipo también es aprender tecnología: límpialo y no lo golpees."
        )

        2 -> listOf(
            "El software son los programas e instrucciones que se ejecutan dentro de la computadora.",
            "El sistema operativo organiza archivos y abre las apps que usas.",
            "Actualizar una app corrige errores y la mejora; guarda tu trabajo antes."
        )

        3 -> listOf(
            "Tu contraseña es secreta: solo tú y un adulto de confianza la conocen.",
            "No compartas tu nombre completo, escuela ni dirección con extraños en línea.",
            "Si algo te asusta o te hace sentir mal en internet, cuéntaselo a un adulto."
        )

        4 -> listOf(
            "Un algoritmo es una lista de pasos en orden, como una receta de cocina.",
            "Un bucle repite instrucciones y un condicional decide con «si pasa esto, haz aquello».",
            "Un bug es un error en el programa: se busca, se corrige y se prueba otra vez."
        )

        else -> listOf(
            "Un programa es un conjunto de instrucciones que la computadora sigue en orden.",
            "Un evento, como un clic, dispara una parte del código.",
            "Probar es revisar cada caso; si algo falla, se corrige y se vuelve a probar."
        )
    }
}
