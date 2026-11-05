package com.example.zazili

import com.example.zazili.core.mascot.BloqueoJuego
import com.example.zazili.core.mascot.ZaziVitals
import com.example.zazili.core.mascot.consumirJuego
import com.example.zazili.core.mascot.decaerZazi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Congela la economía del cuidado: compuertas de Rule 2 y calibración 30/30.
 * Las cuentas son puras para que ningún cambio de UI pueda moverlas sin avisar.
 */
class PetCareBalanceTest {

    private val ahora = 1_800_000_000_000L

    @Test
    fun zazi_despierta_y_sana_puede_jugar() {
        assertNull(ZaziVitals().bloqueoParaJugar())
    }

    @Test
    fun dormir_bloquea_antes_que_cualquier_otra_vital() {
        val vitals = ZaziVitals(
            dormida = true,
            pancita = 0f,
            felicidad = 0f,
            higiene = 0f,
            energia = 0f
        )
        assertEquals(BloqueoJuego.SUENO, vitals.bloqueoParaJugar())
    }

    @Test
    fun cualquier_vital_de_salud_en_cero_bloquea() {
        assertEquals(
            BloqueoJuego.SALUD,
            ZaziVitals(pancita = 0f).bloqueoParaJugar()
        )
        assertEquals(
            BloqueoJuego.SALUD,
            ZaziVitals(felicidad = 0f).bloqueoParaJugar()
        )
        assertEquals(
            BloqueoJuego.SALUD,
            ZaziVitals(higiene = 0f).bloqueoParaJugar()
        )
    }

    @Test
    fun energia_en_cero_bloquea() {
        assertEquals(
            BloqueoJuego.ENERGIA,
            ZaziVitals(energia = 0f).bloqueoParaJugar()
        )
    }

    @Test
    fun treinta_minutos_de_juego_agotan_la_pila() {
        val resultado = ZaziVitals(energia = 100f).consumirJuego(30f)
        assertEquals(0f, resultado.energia, 0.001f)
    }

    @Test
    fun quince_minutos_de_juego_gastan_la_mitad() {
        val resultado = ZaziVitals(energia = 100f).consumirJuego(15f)
        assertEquals(50f, resultado.energia, 0.001f)
    }

    @Test
    fun treinta_minutos_de_descanso_recargan_al_maximo() {
        val vitals = ZaziVitals(energia = 0f, ultimaActualizacion = ahora)
        val resultado = decaerZazi(vitals, ahora + 30 * 60_000L)
        assertEquals(100f, resultado.energia, 0.001f)
    }

    @Test
    fun dormir_recarga_la_energia_al_instante() {
        val vitals = ZaziVitals(energia = 0f, dormida = true, ultimaActualizacion = ahora)
        val resultado = decaerZazi(vitals, ahora + 60_000L)
        assertEquals(100f, resultado.energia, 0.001f)
    }

    @Test
    fun pancita_e_higiene_decaen_una_vez_y_media_mas_rapido() {
        val vitals = ZaziVitals(
            pancita = 78f,
            felicidad = 84f,
            higiene = 92f,
            energia = 88f,
            ultimaActualizacion = ahora
        )
        val resultado = decaerZazi(vitals, ahora + 60 * 60_000L)
        assertEquals(72f, resultado.pancita, 0.01f)
        assertEquals(81.5f, resultado.felicidad, 0.01f)
        assertEquals(89.3f, resultado.higiene, 0.01f)
    }
}
