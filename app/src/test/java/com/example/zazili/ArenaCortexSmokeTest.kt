package com.example.zazili

import com.example.zazili.domain.arena.ArenaCortexRepository
import com.example.zazili.domain.minijuegos.Minijuego
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Blindaje del GDD de la Arena Cortex: los 16 ids del hub deben ser
 * exactamente los del catálogo [Minijuego], con cuatro carruseles de cuatro
 * juegos y cinco consignas por juego. Evita que un typo de id deje una
 * tarjeta huérfana o reviente la inicialización del repositorio.
 */
class ArenaCortexSmokeTest {

    @Test
    fun elGddCubreLos16MinijuegosDelCatalogo() {
        val fichas = ArenaCortexRepository.fichas

        assertEquals(16, fichas.size)
        assertEquals(16, fichas.map { it.id }.toSet().size)
        assertEquals(
            Minijuego.todos.map { it.id }.toSet(),
            fichas.map { it.id }.toSet()
        )

        val secciones = ArenaCortexRepository.secciones
        assertEquals(4, secciones.size)
        secciones.forEach { seccion ->
            assertEquals(4, seccion.fichas.size)
            assertTrue(seccion.alias.isNotBlank())
        }

        fichas.forEach { ficha ->
            assertTrue(ficha.titulo.isNotBlank())
            assertTrue(ficha.gancho.isNotBlank())
            assertTrue(ficha.descripcion.isNotBlank())
            assertTrue(ficha.control.isNotBlank())
            assertTrue(ficha.escalaDificultad.isNotBlank())
            assertEquals(5, ficha.niveles.size)
        }

        assertEquals(1, fichas.count { it.esJefe })
        assertEquals(
            Minijuego.TlacuachePoeta.id,
            fichas.first { it.esJefe }.id
        )
    }

    @Test
    fun cadaJuegoConservaSuModuloYSuRuta() {
        ArenaCortexRepository.fichas.forEach { ficha ->
            assertEquals(ficha.juego.modulo, ficha.modulo)
            assertEquals("juego/${ficha.id}", ficha.juego.ruta)
        }
    }

    @Test
    fun elCodiceAnimadoReemplazaAAtrapaSonidos() {
        val ids = ArenaCortexRepository.fichas.map { it.id }

        assertTrue(Minijuego.CodiceAnimado.id in ids)
        assertFalse("atrapa_sonidos" in ids)
        assertEquals(4, ArenaCortexRepository.fichasDe(Minijuego.CodiceAnimado.modulo).size)
    }
}
