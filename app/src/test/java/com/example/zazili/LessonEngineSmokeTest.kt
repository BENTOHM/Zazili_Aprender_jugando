package com.example.zazili

import com.example.zazili.presentation.lecciones.CalculoMentalExercise
import com.example.zazili.presentation.lecciones.LessonEngine
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonEngineSmokeTest {

    private fun ejercicio() = CalculoMentalExercise(
        id = "prueba_1",
        enunciado = "¿Cuánto es 2 + 2?",
        opciones = listOf("4", "3", "5", "6"),
        indiceCorrecto = 0
    )

    @Test
    fun barajar_conserva_textos_y_marca_una_sola_correcta() {
        val original = ejercicio()
        repeat(40) { semilla ->
            val barajado = LessonEngine.barajar(original, Random(semilla))
            assertEquals(original.opciones.sorted(), barajado.opciones.sorted())
            assertEquals(1, barajado.opciones.count { it == "4" })
            val correctas = LessonEngine.opcionesDe(barajado).filter { it.isCorrect }
            assertEquals(1, correctas.size)
            assertEquals("4", correctas.single().text)
            assertEquals("4", barajado.opciones[barajado.indiceCorrecto])
        }
    }

    @Test
    fun barajar_reparte_la_respuesta_correcta_entre_todas_las_posiciones() {
        val original = ejercicio()
        val posiciones = (0 until 200)
            .map { semilla ->
                val barajado = LessonEngine.barajar(original, Random(semilla))
                barajado.indiceCorrecto
            }
            .toSet()
        assertEquals(setOf(0, 1, 2, 3), posiciones)
    }

    @Test
    fun opcionesDe_marca_solo_el_indice_indicado() {
        val opciones = LessonEngine.opcionesDe(ejercicio())
        assertTrue(opciones[0].isCorrect)
        assertFalse(opciones[1].isCorrect)
        assertFalse(opciones[2].isCorrect)
        assertFalse(opciones[3].isCorrect)
    }

    @Test
    fun barajar_no_toca_ejercicios_incompletos() {
        val sinOpciones = ejercicio().copy(opciones = emptyList(), indiceCorrecto = 0)
        assertEquals(sinOpciones, LessonEngine.barajar(sinOpciones, Random(3)))

        val indiceFuera = ejercicio().copy(indiceCorrecto = 9)
        assertEquals(indiceFuera, LessonEngine.barajar(indiceFuera, Random(3)))
    }

    @Test
    fun opcionesBarajadas_ignora_un_indice_invalido_sin_marcar_correcta() {
        val opciones = LessonEngine.opcionesBarajadas(
            opciones = listOf("uno", "dos"),
            indiceCorrecto = 5,
            random = Random(1)
        )
        assertEquals(2, opciones.size)
        assertTrue(opciones.none { it.isCorrect })
    }
}
