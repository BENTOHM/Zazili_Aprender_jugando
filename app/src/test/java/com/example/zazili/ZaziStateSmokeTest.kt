package com.example.zazili

import com.example.zazili.core.mascot.EstadoVital
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.core.ui.estadoZaziPara
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La mascota pixel-art tiene seis láminas y cada una debe ser alcanzable sin
 * ambigüedad: la celebración manda sobre el Tamagotchi, el cansancio usa la
 * misma pose de sueño con piyama y BASE queda para las bienvenidas.
 */
class ZaziStateSmokeTest {

    @Test
    fun cadaEventoEligeUnaPoseDistinta() {
        assertEquals(
            ZaziState.CELEBRACION,
            estadoZaziPara(EstadoVital(pancita = 0.1f), celebrando = true)
        )
        assertEquals(
            ZaziState.DURMIENDO,
            estadoZaziPara(EstadoVital(dormida = true))
        )
        assertEquals(
            ZaziState.PENSANDO,
            estadoZaziPara(EstadoVital(), pensando = true)
        )
        assertEquals(
            ZaziState.TRISTE,
            estadoZaziPara(EstadoVital(pancita = 0.1f))
        )
        assertEquals(
            ZaziState.TRISTE,
            estadoZaziPara(EstadoVital(felicidad = 0.1f))
        )
        assertEquals(
            ZaziState.DURMIENDO,
            estadoZaziPara(EstadoVital(energia = 0.1f))
        )
        assertEquals(ZaziState.VUELO_IDLE, estadoZaziPara(EstadoVital()))
    }

    @Test
    fun todasLasPosesSeEscuchanEnEspanol() {
        ZaziState.entries.forEach { estado ->
            assertTrue(estado.descripcion.isNotBlank())
        }
        assertEquals(6, ZaziState.entries.size)
        assertEquals(ZaziState.BASE, ZaziState.entries.last())
    }
}
