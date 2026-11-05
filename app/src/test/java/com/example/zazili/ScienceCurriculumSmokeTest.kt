package com.example.zazili

import com.example.zazili.domain.curriculum.ciencias.ScienceCurriculumRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScienceCurriculumSmokeTest {

    @Test
    fun elCurriculoSeConstruyeSinErrores() {
        val lecciones = ScienceCurriculumRepository.lecciones
        assertEquals(50, lecciones.size)
        assertEquals((1..50).toList(), lecciones.map { it.numero })
        assertEquals(50, lecciones.map { it.titulo }.distinct().size)
        assertTrue(lecciones.all { it.preguntas.size == 3 })
        assertEquals(5, lecciones.groupBy { it.capitulo }.size)
        assertTrue(
            lecciones.groupBy { it.capitulo }.values.all { it.size == 10 }
        )
        assertTrue(ScienceCurriculumRepository.leccionPorId("ciencias_50") != null)
    }
}
