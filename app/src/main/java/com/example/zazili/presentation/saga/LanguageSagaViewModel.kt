package com.example.zazili.presentation.saga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.domain.curriculum.lengua.LanguageCurriculumRepository
import com.example.zazili.domain.curriculum.lengua.LessonNode as LeccionLengua
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
 * Mapa adaptativo de Lengua (Cueva de Leyendas).
 *
 * Lee `nivelLenguaje` calibrado en el diagnóstico y coloca al niño en el reto
 * que le corresponde: nivel 1 abre el nodo 1; nivel 2 da por completados los
 * nodos 1–10 y abre el 11; nivel 3 salta al 21; nivel 4 al 31; nivel 5 al 41.
 * La siembra usa una sola escritura por lotes, es idempotente y jamás borra
 * estrellas ya ganadas.
 *
 * Si el diagnóstico marcó `needsAudioSupport`, el narrador global queda
 * encendido y el mapa de lengua ofrece la lectura en voz alta de cada reto.
 */
class LanguageSagaViewModel(application: Application) :
    AndroidViewModel(application), FuenteMapaSaga {

    private val contenedor = (application as ZaziliApplication).container

    override val estado: StateFlow<EstadoSaga> = contenedor.progresoSaga.estado
    override val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    override val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles

    private val _audioApoyo = MutableStateFlow(false)
    val audioApoyo: StateFlow<Boolean> = _audioApoyo.asStateFlow()

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil.asStateFlow()

    private val mundosLengua: List<MundoSaga> = construirMundosLengua()

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
                    aplicarNivelInicial(perfil.nivelLenguaje)
                }
        }
    }

    override fun mundosDe(modulo: ModuloZazili): List<MundoSaga> {
        if (modulo == ModuloZazili.CUEVA_LEYENDAS) return mundosLengua
        return listOfNotNull(RutaSaga.mundoDe(modulo))
            .ifEmpty { listOf(RutaSaga.mundos.first()) }
    }

    /**
     * Siembra los nodos que el diagnóstico ya domina. Idempotente: si el niño
     * avanzó por su cuenta, solo rellena lo que falte.
     */
    private fun aplicarNivelInicial(nivel: Int) {
        val total = when (nivel) {
            2 -> 10
            3 -> 20
            4 -> 30
            5 -> 40
            else -> 0
        }
        if (total <= 0) return
        contenedor.progresoSaga.completarVarios(
            nodoIds = LanguageCurriculumRepository.lecciones
                .take(total)
                .map { it.id },
            estrellas = ESTRELLAS_ADAPTATIVAS
        )
    }

    private fun construirMundosLengua(): List<MundoSaga> =
        LanguageCurriculumRepository.capitulos.map { capitulo ->
            MundoSaga(
                modulo = ModuloZazili.CUEVA_LEYENDAS,
                jefe = capitulo.jefe,
                dichoDelJefe = capitulo.dichoDelJefe,
                nodos = LanguageCurriculumRepository.leccionesDe(capitulo)
                    .map(::aNodoSaga),
                nombreCapitulo = capitulo.titulo
            )
        }

    private fun aNodoSaga(leccion: LeccionLengua): NodoSaga {
        val reto = leccion.preguntas.first()
        return NodoSaga(
            id = leccion.id,
            nombre = leccion.titulo,
            enunciado = reto.enunciado,
            opciones = reto.opciones,
            indiceCorrecto = reto.indiceCorrecto,
            pista = reto.pista,
            esJefe = leccion.esJefeDeCapitulo
        )
    }

    companion object {
        /** Los nodos que el diagnóstico salta cuentan como dominio pleno. */
        private const val ESTRELLAS_ADAPTATIVAS = 3
    }
}
