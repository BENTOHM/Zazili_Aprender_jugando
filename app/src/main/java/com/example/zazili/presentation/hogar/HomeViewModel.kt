package com.example.zazili.presentation.hogar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.mascot.EstadoVital
import com.example.zazili.core.mascot.FrasesZazi
import com.example.zazili.core.mascot.MotorMascota
import com.example.zazili.core.progress.GameProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pequeña explosión de afecto que la casita dibuja encima de Zazi. */
data class EventoAfecto(
    val id: Long = 0L,
    val tipo: TipoAfecto = TipoAfecto.MIMO
)

enum class TipoAfecto { MIMO, COMIDA, JUEGO }

/**
 * Estado de la casita: mascota, XP, zazoles y equipamiento. Las acciones
 * nunca regañan; si Zazi no puede jugar, contesta con humor y una alternativa.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container

    val estado: StateFlow<EstadoVital> = contenedor.repositorioMascota.estado
    val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles
    val equipados: StateFlow<Set<String>> = contenedor.economiaZazoles.equipados

    private val _frase = MutableStateFlow(FrasesZazi.para(estado.value))
    val frase: StateFlow<String> = _frase.asStateFlow()

    private val _evento = MutableStateFlow(EventoAfecto())
    val evento: StateFlow<EventoAfecto> = _evento.asStateFlow()

    private var contadorEventos = 0L

    /** Rota la frase cuando el niño vuelve a entrar a la casita. */
    fun saludar() {
        _frase.value = FrasesZazi.para(estado.value)
    }

    fun alimentar() {
        contenedor.repositorioMascota.alimentar()
        _frase.value = FrasesZazi.alComer()
        emitir(TipoAfecto.COMIDA)
    }

    fun jugar() {
        val actual = estado.value
        if (actual.dormida || actual.energia < MotorMascota.ENERGIA_MINIMA_PARA_JUGAR) {
            _frase.value = FrasesZazi.sinEnergia()
            return
        }
        contenedor.repositorioMascota.jugar()
        _frase.value = FrasesZazi.alJugar()
        emitir(TipoAfecto.JUEGO)
    }

    fun mimo() {
        if (estado.value.dormida) {
            contenedor.repositorioMascota.despertar()
            _frase.value = FrasesZazi.alDespertar()
        } else {
            contenedor.repositorioMascota.hacerMimo()
            _frase.value = FrasesZazi.alMimo()
        }
        emitir(TipoAfecto.MIMO)
    }

    private fun emitir(tipo: TipoAfecto) {
        contadorEventos += 1
        _evento.value = EventoAfecto(id = contadorEventos, tipo = tipo)
    }
}
