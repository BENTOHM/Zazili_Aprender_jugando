package com.example.zazili.presentation.saga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.domain.curriculum.emociones.LeccionEmocional
import com.example.zazili.domain.curriculum.emociones.MentalHealthCurriculumRepository
import com.example.zazili.domain.diagnostico.PerfilFrustracion
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.EstadoSaga
import com.example.zazili.domain.saga.MundoSaga
import com.example.zazili.domain.saga.NodoSaga
import com.example.zazili.domain.saga.RutaSaga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Mapa adaptativo de Emociones (El Árbol de los Abrazos).
 *
 * El diagnóstico del vuelo mide cuánto tarda el niño en reintentar después de
 * un fracaso. Esa latencia se traduce en un `eqLevel` de 1 a 5:
 *  - Impulsivo o abandono inmediato → 1: abre el nodo 1.
 *  - 2.5 a 5 s → 2: completa 1–10 y abre el 11.
 *  - 5 a 8 s → 3: completa 1–20 y abre el 21.
 *  - 8 a 15 s → 4: completa 1–30 y abre el 31.
 *  - Más de 15 s (persistente) → 5: completa 1–40 y abre el 41.
 *
 * La siembra usa una sola escritura por lotes, es idempotente y nunca borra
 * estrellas ya ganadas. Si el diagnóstico pidió apoyo de audio, el narrador
 * global queda encendido para toda la ruta.
 */
class MentalHealthSagaViewModel(application: Application) :
    AndroidViewModel(application), FuenteMapaSaga {

    private val contenedor = (application as ZaziliApplication).container

    override val estado: StateFlow<EstadoSaga> = contenedor.progresoSaga.estado
    override val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    override val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles

    private val _audioApoyo = MutableStateFlow(false)
    val audioApoyo: StateFlow<Boolean> = _audioApoyo.asStateFlow()

    private val _eqLevel = MutableStateFlow(1)
    val eqLevel: StateFlow<Int> = _eqLevel.asStateFlow()

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil.asStateFlow()

    private val mundosEmocionales: List<MundoSaga> = construirMundosEmocionales()
    private var nivelSembrado = 0

    init {
        viewModelScope.launch {
            contenedor.userManager.perfil
                .filterNotNull()
                .collect { perfil ->
                    _perfil.value = perfil
                    _audioApoyo.value = perfil.apoyoAudio
                    if (perfil.apoyoAudio) {
                        StoryNarratorEngine.setHabilitado(true)
                    }
                    val nivel = calcularEqLevel(perfil)
                    _eqLevel.value = nivel
                    if (nivel > nivelSembrado) {
                        aplicarNivelInicial(nivel)
                        nivelSembrado = nivel
                    }
                }
        }
    }

    override fun mundosDe(modulo: ModuloZazili): List<MundoSaga> {
        if (modulo == ModuloZazili.ARBOL_ABRAZOS) return mundosEmocionales
        return listOfNotNull(RutaSaga.mundoDe(modulo))
            .ifEmpty { listOf(RutaSaga.mundos.first()) }
    }

    /**
     * Siembra los tramos que el diagnóstico ya domina. Es idempotente: si el
     * niño avanzó por su cuenta, solo rellena lo que falte.
     */
    private fun aplicarNivelInicial(eqLevel: Int) {
        val total = when (eqLevel) {
            2 -> 10
            3 -> 20
            4 -> 30
            5 -> 40
            else -> 0
        }
        if (total <= 0) return
        contenedor.progresoSaga.completarVarios(
            nodoIds = MentalHealthCurriculumRepository.lecciones
                .take(total)
                .map { it.id },
            estrellas = ESTRELLAS_ADAPTATIVAS
        )
    }

    private fun construirMundosEmocionales(): List<MundoSaga> =
        MentalHealthCurriculumRepository.capitulos.map { capitulo ->
            MundoSaga(
                modulo = ModuloZazili.ARBOL_ABRAZOS,
                jefe = capitulo.jefe,
                dichoDelJefe = capitulo.dichoDelJefe,
                nodos = MentalHealthCurriculumRepository.leccionesDe(capitulo.numero)
                    .map(::aNodoSaga),
                nombreCapitulo = capitulo.titulo
            )
        }

    private fun aNodoSaga(leccion: LeccionEmocional): NodoSaga {
        val reto = leccion.preguntas.first()
        return NodoSaga(
            id = leccion.id,
            nombre = leccion.titulo,
            enunciado = reto.escenario,
            opciones = reto.opciones.map { it.texto },
            indiceCorrecto = reto.indiceCorrecto,
            pista = reto.pista,
            esJefe = leccion.esJefeDeCapitulo
        )
    }

    companion object {

        /** Los tramos que el diagnóstico salta cuentan como dominio pleno. */
        private const val ESTRELLAS_ADAPTATIVAS = 3

        private const val LATENCIA_IMPULSIVA_MS = 2_500L
        private const val LATENCIA_LENTA_MS = 5_000L
        private const val LATENCIA_RESILIENTE_MS = 8_000L
        private const val LATENCIA_PERSISTENTE_MS = 15_000L

        /**
         * Traduce la tolerancia a la frustración en nivel emocional. Un abandono
         * con latencia negativa o un perfil impulsivo siempre abre el nodo 1.
         */
        fun calcularEqLevel(perfil: PerfilUsuario): Int {
            val latencia = perfil.latenciaReintentoMs
            if (latencia < 0L) return 1
            val porLatencia = when {
                latencia < LATENCIA_IMPULSIVA_MS -> 1
                latencia < LATENCIA_LENTA_MS -> 2
                latencia < LATENCIA_RESILIENTE_MS -> 3
                latencia < LATENCIA_PERSISTENTE_MS -> 4
                else -> 5
            }
            return when (perfil.perfilFrustracion) {
                PerfilFrustracion.IMPULSIVO -> 1
                else -> porLatencia
            }
        }
    }
}
