package com.example.zazili

import com.example.zazili.domain.curriculum.lengua.LanguageCurriculumRepository
import com.example.zazili.domain.curriculum.lengua.Module2LessonValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class Module2LessonValidatorSmokeTest {

    private val lecciones = LanguageCurriculumRepository.lecciones

    @Test
    fun laAuditoriaDelModulo2EstaLimpia() {
        val auditoria = Module2LessonValidator.auditar(lecciones)
        assertTrue(auditoria.resumen(), auditoria.esValida)
        assertEquals(50, auditoria.totalLecciones)
        assertEquals(150, auditoria.totalPreguntas)
        assertEquals(5, lecciones.count { it.esJefeDeCapitulo })
    }

    @Test
    fun lasRespuestasNoSeSesganHaciaUnaLetra() {
        val porTamano = lecciones
            .flatMap { it.preguntas }
            .groupBy { it.opciones.size }

        porTamano.forEach { (tamano, preguntas) ->
            val conteo = preguntas.groupingBy { it.indiceCorrecto }.eachCount()
            val minimo = (0 until tamano).minOf { conteo[it] ?: 0 }
            val maximo = (0 until tamano).maxOf { conteo[it] ?: 0 }
            assertTrue(
                "Con $tamano opciones el reparto fue $conteo",
                maximo - minimo <= 1
            )
        }

        val total = lecciones.sumOf { it.preguntas.size }
        val enPrimeraCasilla = lecciones
            .flatMap { it.preguntas }
            .count { it.indiceCorrecto == 0 }
        assertTrue(
            "Todavía hay demasiadas respuestas en A: $enPrimeraCasilla de $total",
            enPrimeraCasilla < total / 2
        )
    }

    @Test
    fun laRespuestaCorrectaSigueAlTextoCorrecto() {
        assertEquals("a", correcta("lengua_01", 0))
        assertEquals("blu", correcta("lengua_11", 0))
        assertEquals("corre", correcta("lengua_21", 0))
        assertEquals("Guanajuato.", correcta("lengua_32", 2))
        assertEquals("Acompañan a los guerreros.", correcta("lengua_39", 2))
        assertEquals(
            "El libro ladró y el perro leyó.",
            correcta("lengua_43", 2)
        )
        assertEquals("Un libro.", correcta("lengua_46", 0))
        assertEquals("Metáfora.", correcta("lengua_50", 0))
    }

    @Test
    fun todasLasPreguntasTienenEstructuraValida() {
        lecciones.forEach { leccion ->
            leccion.preguntas.forEach { pregunta ->
                assertTrue(pregunta.opciones.size in 2..4)
                assertTrue(pregunta.opciones.all { it.isNotBlank() })
                assertEquals(
                    pregunta.opciones.size,
                    pregunta.opciones.distinct().size
                )
                assertTrue(pregunta.indiceCorrecto in pregunta.opciones.indices)
                assertTrue(pregunta.enunciado.isNotBlank())
                assertTrue(pregunta.pista.isNotBlank())
            }
        }
    }

    @Test
    fun mezclarConservaOpcionesYRespuestaCorrecta() {
        val mezcladas = Module2LessonValidator.mezclarLecciones(lecciones, Random(7))
        assertEquals(lecciones.size, mezcladas.size)

        var huboCambioDeOrden = false
        lecciones.zip(mezcladas).forEach { (original, mezclada) ->
            assertEquals(original.id, mezclada.id)
            original.preguntas.zip(mezclada.preguntas).forEach { (antes, despues) ->
                assertEquals(antes.opciones.toSet(), despues.opciones.toSet())
                assertEquals(
                    antes.opciones[antes.indiceCorrecto],
                    despues.opciones[despues.indiceCorrecto]
                )
                if (antes.opciones != despues.opciones) huboCambioDeOrden = true
            }
        }
        assertTrue(huboCambioDeOrden)
    }

    @Test
    fun elOrdenSeQuedaFijoDuranteLaSesion() {
        assertSame(
            LanguageCurriculumRepository.lecciones,
            LanguageCurriculumRepository.lecciones
        )
    }

    private fun correcta(leccionId: String, reto: Int): String {
        val leccion = requireNotNull(LanguageCurriculumRepository.leccionPorId(leccionId))
        val pregunta = leccion.preguntas[reto]
        return pregunta.opciones[pregunta.indiceCorrecto]
    }
}
