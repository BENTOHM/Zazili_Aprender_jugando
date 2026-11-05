package com.example.zazili

import com.example.zazili.domain.curriculum.tecnologia.TechnologyModuleRepository
import com.example.zazili.presentation.lecciones.LessonEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechCurriculumSmokeTest {

    @Test
    fun elCurriculoTieneCincuentaLeccionesCompletas() {
        val lecciones = TechnologyModuleRepository.lecciones
        assertEquals(50, lecciones.size)
        assertEquals((1..50).toList(), lecciones.map { it.id })
        assertEquals(50, lecciones.map { it.question }.distinct().size)
        assertTrue(lecciones.all { it.options.size == 4 })
        assertTrue(lecciones.all { leccion -> leccion.options.count { it.isCorrect } == 1 })
        assertTrue(lecciones.all { leccion -> leccion.options.map { it.text }.distinct().size == 4 })
    }

    @Test
    fun losCincoCapitulosCubrenTodoElMapa() {
        val capitulos = TechnologyModuleRepository.capitulos
        assertEquals(5, capitulos.size)
        assertTrue(capitulos.all { it.lessons.size == 10 })
        assertEquals(
            listOf(
                "Hardware",
                "Software",
                "Internet y Seguridad",
                "Lógica y Pensamiento Computacional",
                "Programación y Código"
            ),
            capitulos.map { it.title }
        )
        assertEquals("Hardware", TechnologyModuleRepository.tituloDeCapituloDe(1))
        assertEquals("Software", TechnologyModuleRepository.tituloDeCapituloDe(11))
        assertEquals("Internet y Seguridad", TechnologyModuleRepository.tituloDeCapituloDe(21))
        assertEquals(
            "Lógica y Pensamiento Computacional",
            TechnologyModuleRepository.tituloDeCapituloDe(31)
        )
        assertEquals("Programación y Código", TechnologyModuleRepository.tituloDeCapituloDe(50))
    }

    @Test
    fun lasBusquedasPorIdRespetanElRango() {
        assertNotNull(TechnologyModuleRepository.porId(1))
        assertNotNull(TechnologyModuleRepository.porId(50))
        assertNull(TechnologyModuleRepository.porId(0))
        assertNull(TechnologyModuleRepository.porId(51))
    }

    @Test
    fun elMotorAgrupaLasCincuentaLeccionesEnDiezBloques() {
        assertEquals(10, LessonEngine.BLOQUES_TECNOLOGIA)
        val bloquesConTeoria = mutableListOf<Int>()
        (1..LessonEngine.BLOQUES_TECNOLOGIA).forEach { bloque ->
            val sesion = LessonEngine.sesionDeTecnologia(
                nodoId = LessonEngine.nodoDeBloque(bloque),
                completados = emptySet()
            )
            assertEquals(bloque, sesion.bloque)
            assertEquals(LessonEngine.LECCIONES_POR_BLOQUE, sesion.ejercicios.size)
            assertEquals(
                sesion.ejercicios.size,
                sesion.ejercicios.map { it.id }.distinct().size
            )
            if (sesion.teoria != null) bloquesConTeoria += bloque
        }
        assertEquals(listOf(1, 3, 5, 7, 9), bloquesConTeoria)
    }
}
