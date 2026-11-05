package com.example.zazili.presentation.dashboard

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.mascot.BloqueoJuego
import com.example.zazili.core.mascot.ZaziCareState
import com.example.zazili.core.mascot.ZaziPetCareEngine
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.domain.economia.EstadoEconomia
import com.example.zazili.domain.economia.EventoRecompensa
import com.example.zazili.domain.economia.ZazolEconomyManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado global del niño y de Zazi.
 *
 * El [ZaziPetCareEngine] sigue siendo un singleton de proceso: este
 * ViewModel solo lo expone y sobrevive a los cambios de configuración. Al
 * declararse una sola vez fuera del `NavHost`, el Tablero y cualquier pantalla
 * que reciba esta instancia observan exactamente los mismos valores.
 *
 * Las lecciones y la Arena abren una sesión de actividad. Mientras esa sesión
 * vive, este ViewModel escucha el avance de las sagas y las partidas de la
 * Arena para aplicar los efectos de cuidado y pagar los Zazoles UNA sola vez
 * por logro. El arranque adaptativo de los mapas queda fuera de la sesión, así
 * que completar nodos de golpe nunca castiga a Zazi.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container
    private val motorCuidado = ZaziPetCareEngine.obtener(application)
    private val economia = ZazolEconomyManager.obtener(application)

    val cuidado: StateFlow<ZaziCareState> = motorCuidado.estado
    val bolsa: StateFlow<EstadoEconomia> = economia.estado
    val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    val perfil: StateFlow<PerfilUsuario?> = contenedor.userManager.perfil.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private var sesionesActivas = 0
    private var nodosRecompensados: Set<String> = emptySet()
    private var partidasConocidas: Map<String, Int> = emptyMap()
    private var observadorLecciones: Job? = null
    private var observadorArena: Job? = null

    private val _bloqueo = MutableStateFlow<BloqueoJuego?>(null)

    /** Aviso vivo de la compuerta; la UI lo muestra y lo descarta. */
    val bloqueo: StateFlow<BloqueoJuego?> = _bloqueo.asStateFlow()

    // Reloj de la sesión de juego real (lección o minijuego abierto).
    private var sesionesDeJuego = 0
    private var inicioSesionJuegoMs = 0L

    /** Caricia directa desde el Tablero. No despierta a Zazi. */
    fun acariciar() {
        viewModelScope.launch { motorCuidado.acariciar() }
    }

    /** Arropa a Zazi o la despierta según su estado actual. */
    fun alternarSueno() {
        viewModelScope.launch {
            if (motorCuidado.estado.value.vitals.dormida) {
                motorCuidado.despertar()
            } else {
                motorCuidado.dormir()
            }
        }
    }

    /**
     * Compuerta de Rule 2. Evalúa sueño, salud y energía antes de abrir una
     * lección o un minijuego. Si hay bloqueo solo publica el aviso; si no,
     * ejecuta la navegación en el acto.
     */
    fun intentarAbrirActividad(accion: () -> Unit) {
        val motivo = motorCuidado.bloqueoParaJugar()
        _bloqueo.value = motivo
        if (motivo == null) accion()
    }

    /** Cierra el aviso de bloqueo sin cambiar el estado de Zazi. */
    fun descartarBloqueo() {
        _bloqueo.value = null
    }

    /**
     * Abre el cronómetro de juego real. Solo las pantallas de lección y de
     * minijuego lo llaman; el hub de la Arena no cuenta como tiempo jugado.
     */
    fun abrirSesionDeJuego() {
        if (sesionesDeJuego == 0) {
            inicioSesionJuegoMs = SystemClock.elapsedRealtime()
            motorCuidado.marcarJuegoActivo(true)
        }
        sesionesDeJuego += 1
    }

    /** Cierra el cronómetro y liquida la energía consumida una sola vez. */
    fun cerrarSesionDeJuego() {
        if (sesionesDeJuego <= 0) return
        sesionesDeJuego -= 1
        if (sesionesDeJuego > 0) return
        val minutos = (SystemClock.elapsedRealtime() - inicioSesionJuegoMs) / 60_000f
        inicioSesionJuegoMs = 0L
        motorCuidado.marcarJuegoActivo(false)
        viewModelScope.launch { motorCuidado.consumirTiempoDeJuego(minutos) }
    }

    /**
     * Abre una sesión de actividad. Las transiciones del `NavHost` pueden
     * solapar dos destinos, por eso se usa un contador y no un booleano.
     */
    fun abrirSesionActividad() {
        sesionesActivas += 1
        if (sesionesActivas > 1) return

        nodosRecompensados = contenedor.progresoSaga.estado.value.completados
        partidasConocidas = partidasJugadas()
        observadorLecciones = viewModelScope.launch {
            contenedor.progresoSaga.estado.collect { estado ->
                val nuevos = estado.completados - nodosRecompensados
                if (nuevos.isEmpty()) return@collect
                nodosRecompensados = estado.completados
                nuevos.forEach(::alCompletarLeccion)
            }
        }
        observadorArena = viewModelScope.launch {
            contenedor.dynamicDifficultyEngine.estado.collect { estado ->
                val actuales = estado.progresos.mapValues { it.value.partidasJugadas }
                val completadas = actuales.entries.sumOf { (juegoId, total) ->
                    (total - (partidasConocidas[juegoId] ?: 0)).coerceAtLeast(0)
                }
                partidasConocidas = actuales
                repeat(completadas.coerceAtMost(MAX_MINIJUEGOS_POR_EMISION)) {
                    alCompletarMinijuego()
                }
            }
        }
    }

    /** Cierra la sesión; al llegar a cero se detienen los observadores. */
    fun cerrarSesionActividad() {
        if (sesionesActivas > 0) sesionesActivas -= 1
        if (sesionesActivas > 0) return
        observadorLecciones?.cancel()
        observadorLecciones = null
        observadorArena?.cancel()
        observadorArena = null
    }

    /**
     * Punto único de recompensa por lección: gasta energía de Zazi y paga los
     * Zazoles del evento. Las repeticiones no vuelven a pagar porque el nodo
     * ya forma parte de [nodosRecompensados].
     */
    fun alCompletarLeccion(nodoId: String) {
        if (nodoId.isBlank()) return
        viewModelScope.launch { motorCuidado.registrarEstudio() }
        economia.notificarActividad(EventoRecompensa.LECCION_COMPLETADA)
    }

    /** Punto único de recompensa por partida de la Arena. */
    fun alCompletarMinijuego() {
        viewModelScope.launch { motorCuidado.registrarMinijuego() }
        economia.notificarActividad(EventoRecompensa.MINIJUEGO_COMPLETADO)
    }

    private fun partidasJugadas(): Map<String, Int> =
        contenedor.dynamicDifficultyEngine.estado.value.progresos.mapValues {
            it.value.partidasJugadas
        }

    private companion object {
        const val MAX_MINIJUEGOS_POR_EMISION = 4
    }
}
