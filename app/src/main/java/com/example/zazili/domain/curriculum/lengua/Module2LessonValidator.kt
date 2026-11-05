package com.example.zazili.domain.curriculum.lengua

import kotlin.random.Random

/**
 * Auditoría y mezcla de respuestas del Módulo 2 (Lenguaje y Literatura).
 *
 * [LanguageCurriculumRepository] llama a [mezclarLecciones] una sola vez por
 * proceso. Así la respuesta correcta ya no se queda clavada en la casilla A:
 * cada pregunta estrena un orden de opciones y su [PreguntaLengua.indiceCorrecto]
 * se mueve junto con el texto correcto. Como la mezcla ocurre al construir el
 * repositorio y no en cada recomposición, la pantalla ve listas inmutables y el
 * orden nunca cambia a media pregunta.
 *
 * La posición de la respuesta se reparte con una bolsa estratificada por número
 * de opciones: en cada grupo de preguntas del mismo tamaño, A, B, C y D reciben
 * casi exactamente la misma cantidad. Después los distractores se barajan, de
 * modo que el orden también cambia entre combinaciones.
 */
object Module2LessonValidator {

    const val TOTAL_LECCIONES_ESPERADAS = 50
    const val PREGUNTAS_POR_LECCION = 3
    const val LECCIONES_POR_CAPITULO = 10

    /** Una incoherencia detectada por la auditoría. */
    data class Hallazgo(val leccionId: String, val mensaje: String)

    /** Resultado completo de revisar el módulo. */
    data class Auditoria(
        val totalLecciones: Int,
        val totalPreguntas: Int,
        val hallazgos: List<Hallazgo>,
        val conteoPorPosicion: Map<Int, Int>
    ) {
        val esValida: Boolean
            get() = hallazgos.isEmpty()

        fun resumen(): String = buildString {
            append("Módulo 2: $totalLecciones lecciones y $totalPreguntas preguntas. ")
            append("Respuestas por casilla: ")
            append(
                conteoPorPosicion.toSortedMap().entries.joinToString { (posicion, total) ->
                    "${letraDe(posicion)}=$total"
                }
            )
            if (esValida) {
                append(". Sin hallazgos.")
            } else {
                append(". ${hallazgos.size} hallazgos: ")
                append(hallazgos.joinToString { "${it.leccionId}: ${it.mensaje}" })
            }
        }
    }

    /** Convierte una posición 0..3 en la letra que ve el niño. */
    fun letraDe(posicion: Int): Char = ('A' + posicion.coerceIn(0, 25))

    /**
     * Revisa estructura, contenido y equilibrio de respuestas.
     *
     * [revisarEquilibrio] debe quedarse en `true` para el dataset ya mezclado;
     * se apaga solo cuando se quiere auditar el contenido crudo antes de barajar.
     */
    fun auditar(lecciones: List<LessonNode>, revisarEquilibrio: Boolean = true): Auditoria {
        val hallazgos = mutableListOf<Hallazgo>()
        val ids = mutableSetOf<String>()

        if (lecciones.size != TOTAL_LECCIONES_ESPERADAS) {
            hallazgos += Hallazgo(
                leccionId = "modulo_2",
                mensaje = "Se esperaban $TOTAL_LECCIONES_ESPERADAS lecciones y hay ${lecciones.size}."
            )
        }

        lecciones.forEach { leccion ->
            if (!ids.add(leccion.id)) {
                hallazgos += Hallazgo(leccion.id, "El id está repetido.")
            }
            val idEsperado = "lengua_" + leccion.numero.toString().padStart(2, '0')
            if (leccion.id != idEsperado) {
                hallazgos += Hallazgo(
                    leccion.id,
                    "El id no coincide con su número ($idEsperado)."
                )
            }
            if (leccion.numero !in 1..TOTAL_LECCIONES_ESPERADAS) {
                hallazgos += Hallazgo(leccion.id, "El número está fuera de 1..50.")
            }
            if (leccion.titulo.isBlank()) {
                hallazgos += Hallazgo(leccion.id, "El título está vacío.")
            }
            if (leccion.teoria.isEmpty()) {
                hallazgos += Hallazgo(leccion.id, "La teoría está vacía.")
            }
            if (leccion.preguntas.size != PREGUNTAS_POR_LECCION) {
                hallazgos += Hallazgo(
                    leccion.id,
                    "Se esperaban $PREGUNTAS_POR_LECCION retos y hay ${leccion.preguntas.size}."
                )
            }
            leccion.preguntas.forEachIndexed { indice, pregunta ->
                revisarPregunta(leccion.id, indice, pregunta, hallazgos)
            }
        }

        CapituloLengua.entries.forEach { capitulo ->
            val total = lecciones.count { it.capitulo == capitulo }
            if (total != LECCIONES_POR_CAPITULO) {
                hallazgos += Hallazgo(
                    leccionId = "capitulo_${capitulo.numero}",
                    mensaje = "«${capitulo.titulo}» tiene $total lecciones en lugar de $LECCIONES_POR_CAPITULO."
                )
            }
        }

        if (revisarEquilibrio) {
            hallazgos += revisarEquilibrio(lecciones)
        }

        val conteo = lecciones
            .flatMap { it.preguntas }
            .groupingBy { it.indiceCorrecto }
            .eachCount()
            .toSortedMap()

        return Auditoria(
            totalLecciones = lecciones.size,
            totalPreguntas = lecciones.sumOf { it.preguntas.size },
            hallazgos = hallazgos,
            conteoPorPosicion = conteo
        )
    }

    private fun revisarPregunta(
        leccionId: String,
        indice: Int,
        pregunta: PreguntaLengua,
        hallazgos: MutableList<Hallazgo>
    ) {
        val ref = "$leccionId · reto ${indice + 1}"
        if (pregunta.enunciado.isBlank()) {
            hallazgos += Hallazgo(leccionId, "$ref sin enunciado.")
        }
        if (pregunta.pista.isBlank()) {
            hallazgos += Hallazgo(leccionId, "$ref sin pista.")
        }
        if (pregunta.opciones.size !in 2..4) {
            hallazgos += Hallazgo(
                leccionId,
                "$ref tiene ${pregunta.opciones.size} opciones; se esperan de 2 a 4."
            )
        }
        if (pregunta.opciones.any { it.isBlank() }) {
            hallazgos += Hallazgo(leccionId, "$ref tiene una opción vacía.")
        }
        if (pregunta.opciones.distinct().size != pregunta.opciones.size) {
            hallazgos += Hallazgo(leccionId, "$ref repite opciones.")
        }
        if (pregunta.indiceCorrecto !in pregunta.opciones.indices) {
            hallazgos += Hallazgo(
                leccionId,
                "$ref apunta a la casilla ${pregunta.indiceCorrecto} y solo existen ${pregunta.opciones.indices}."
            )
        }
        if (pregunta.mecanica == MecanicaLengua.ATRAPA_SONIDOS &&
            pregunta.audioObjetivo.isNullOrBlank()
        ) {
            hallazgos += Hallazgo(leccionId, "$ref necesita audio y no lo tiene.")
        }
    }

    /**
     * Verifica que, dentro de cada tamaño de lista, las posiciones de la
     * respuesta correcta estén repartidas con diferencia máxima de uno.
     */
    private fun revisarEquilibrio(lecciones: List<LessonNode>): List<Hallazgo> {
        val hallazgos = mutableListOf<Hallazgo>()
        val porTamano = lecciones
            .flatMap { it.preguntas }
            .groupBy { it.opciones.size }

        porTamano.forEach { (tamano, preguntas) ->
            val conteo = preguntas.groupingBy { it.indiceCorrecto }.eachCount()
            val minimo = (0 until tamano).minOf { conteo[it] ?: 0 }
            val maximo = (0 until tamano).maxOf { conteo[it] ?: 0 }
            if (maximo - minimo > 1) {
                val detalle = (0 until tamano).joinToString { posicion ->
                    "${letraDe(posicion)}=${conteo[posicion] ?: 0}"
                }
                hallazgos += Hallazgo(
                    leccionId = "modulo_2",
                    mensaje = "Respuestas desbalanceadas con $tamano opciones ($detalle)."
                )
            }
        }
        return hallazgos
    }

    /**
     * Devuelve una copia de las lecciones con las opciones mezcladas y el
     * índice correcto recalculado. No modifica la lista original.
     */
    fun mezclarLecciones(
        lecciones: List<LessonNode>,
        random: Random = Random.Default
    ): List<LessonNode> {
        val bolsas = crearBolsas(lecciones, random)
        return lecciones.map { leccion ->
            leccion.copy(
                preguntas = leccion.preguntas.map { pregunta ->
                    val destino = bolsas.getValue(pregunta.opciones.size).removeFirst()
                    mezclarPregunta(pregunta, destino, random)
                }
            )
        }
    }

    private fun crearBolsas(
        lecciones: List<LessonNode>,
        random: Random
    ): Map<Int, ArrayDeque<Int>> {
        val totales = lecciones
            .flatMap { it.preguntas }
            .groupingBy { it.opciones.size }
            .eachCount()

        return totales.mapValues { (tamano, total) ->
            val posiciones = ArrayList<Int>(total)
            while (posiciones.size < total) {
                val vuelta = (0 until tamano).shuffled(random)
                posiciones += vuelta.take(total - posiciones.size)
            }
            ArrayDeque(posiciones.shuffled(random))
        }
    }

    private fun mezclarPregunta(
        pregunta: PreguntaLengua,
        destino: Int,
        random: Random
    ): PreguntaLengua {
        if (pregunta.indiceCorrecto !in pregunta.opciones.indices) {
            return pregunta
        }
        val correcta = pregunta.opciones[pregunta.indiceCorrecto]
        val distractores = pregunta.opciones
            .filterIndexed { indice, _ -> indice != pregunta.indiceCorrecto }
            .shuffled(random)
        val posicion = destino.coerceIn(0, distractores.size)
        val nuevas = distractores.toMutableList().apply { add(posicion, correcta) }
        return pregunta.copy(opciones = nuevas, indiceCorrecto = posicion)
    }
}
