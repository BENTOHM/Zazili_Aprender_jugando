package com.example.zazili

import com.example.zazili.presentation.lecciones.AccionCodice
import com.example.zazili.presentation.lecciones.PLANTILLAS_CODICE
import com.example.zazili.presentation.lecciones.MISIONES_CODICE
import com.example.zazili.presentation.lecciones.plantillaPara
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Blindaje del banco del Códice Animado: la pantalla abre el hueco buscando la
 * palabra correcta DENTRO de la oración, así que una misión con la palabra
 * fuera del texto rompería la mecánica. También se fija la fauna mexicana que
 * dispara una celebración distinta por animal.
 */
class CodiceAnimadoSmokeTest {

    @Test
    fun cadaMisionEsJugableYSinRespuestasRepetidas() {
        MISIONES_CODICE.forEach { mision ->
            assertTrue(
                "La oración debe contener la palabra correcta: ${mision.enunciado}",
                mision.enunciado.contains(mision.correcta, ignoreCase = true)
            )
            assertTrue(mision.enunciado.isNotBlank())
            assertTrue(mision.correcta.isNotBlank())
            assertTrue(mision.pista.isNotBlank())
            assertEquals(2, mision.distractores.size)
            assertTrue(
                "Los distractores no pueden repetir la respuesta",
                mision.distractores.none {
                    it.equals(mision.correcta, ignoreCase = true)
                }
            )
            assertTrue(mision.nivel in 1..5)
        }
    }

    @Test
    fun hayCuatroMisionesPorNivel() {
        (1..5).forEach { nivel ->
            assertEquals(
                "El nivel $nivel necesita cuatro misiones",
                4,
                MISIONES_CODICE.count { it.nivel == nivel }
            )
        }
    }

    @Test
    fun laPrimeraMisionEsElJaguarCorriendo() {
        val primera = MISIONES_CODICE.first()

        assertEquals(1, primera.nivel)
        assertEquals("El jaguar corre por la selva.", primera.enunciado)
        assertEquals("jaguar", primera.correcta)
        assertEquals(setOf("piedra", "nube"), primera.distractores.toSet())
    }

    @Test
    fun laFaunaTieneCuatroAccionesDistintas() {
        assertEquals(4, PLANTILLAS_CODICE.size)
        assertEquals(
            setOf(
                AccionCodice.CORRER,
                AccionCodice.REVOLOTEAR,
                AccionCodice.ARRASTRAR,
                AccionCodice.SALTAR
            ),
            PLANTILLAS_CODICE.map { it.accion }.toSet()
        )
        PLANTILLAS_CODICE.forEach { plantilla ->
            assertTrue(plantilla.emoji.isNotBlank())
            assertNotNull(
                plantillaPara(
                    "${plantilla.textoAntes}${plantilla.palabraCorrecta}${plantilla.textoDespues}",
                    plantilla.palabraCorrecta
                )
            )
        }
    }

    @Test
    fun laFaunaDelNivelUnoEsJugable() {
        val fauna = MISIONES_CODICE
            .filter { it.nivel == 1 }
            .map { it.correcta }
            .toSet()

        assertEquals(
            setOf("jaguar", "colibrí", "tortuga", "mono"),
            fauna
        )
    }
}
