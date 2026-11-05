package com.example.zazili.presentation.vuelo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.domain.model.SessionLog
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.NodoSaga
import com.example.zazili.domain.saga.RutaSaga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Un reto del vuelo diagnóstico: el primer nodo de cada mundo. */
data class PreguntaVuelo(
    val modulo: ModuloZazili,
    val nodo: NodoSaga
)

/**
 * Estado del Vuelo de Prueba. No hay nota ni cronómetro: solo cinco preguntas
 * para saber por dónde empezar. Cada respuesta se registra como intento para
 * que el Nido pueda acompañar sin adivinar.
 */
data class EstadoVuelo(
    val indice: Int = 0,
    val elegida: Int? = null,
    val respuestaCorrecta: Boolean? = null,
    val aciertos: Int = 0,
    val respuestas: List<Boolean> = emptyList(),
    val fallos: Set<ModuloZazili> = emptySet(),
    val terminado: Boolean = false
)

class VueloViewModel(application: Application) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container

    val preguntas: List<PreguntaVuelo> = RutaSaga.mundos.map { mundo ->
        PreguntaVuelo(modulo = mundo.modulo, nodo = mundo.nodos.first())
    }

    private val _estado = MutableStateFlow(EstadoVuelo())
    val estado: StateFlow<EstadoVuelo> = _estado.asStateFlow()

    fun responder(opcion: Int) {
        val actual = _estado.value
        if (actual.terminado || actual.respuestaCorrecta != null) return
        val pregunta = preguntas.getOrNull(actual.indice) ?: return

        val correcta = opcion == pregunta.nodo.indiceCorrecto
        _estado.value = actual.copy(
            elegida = opcion,
            respuestaCorrecta = correcta,
            aciertos = actual.aciertos + if (correcta) 1 else 0,
            respuestas = actual.respuestas + correcta,
            fallos = if (correcta) actual.fallos else actual.fallos + pregunta.modulo
        )

        viewModelScope.launch {
            runCatching {
                contenedor.progressRepository.recordAttempt(
                    SessionLog(
                        moduleRoute = "VUELO_DE_PRUEBA",
                        activityId = pregunta.nodo.id,
                        succeeded = correcta,
                        attemptCount = 1,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun siguiente() {
        val actual = _estado.value
        if (actual.respuestaCorrecta == null) return
        _estado.value = if (actual.indice >= preguntas.lastIndex) {
            actual.copy(terminado = true)
        } else {
            actual.copy(
                indice = actual.indice + 1,
                elegida = null,
                respuestaCorrecta = null
            )
        }
    }

    fun reiniciar() {
        _estado.value = EstadoVuelo()
    }

    /** Mundo por el que conviene empezar según el vuelo y el avance actual. */
    fun moduloSugerido(): ModuloZazili {
        _estado.value.fallos.firstOrNull()?.let { return it }
        return RutaSaga.mundoActual(contenedor.progresoSaga.estado.value).modulo
    }
}
