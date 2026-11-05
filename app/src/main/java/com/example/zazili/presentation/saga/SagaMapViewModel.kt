package com.example.zazili.presentation.saga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.domain.curriculum.LessonNode
import com.example.zazili.domain.curriculum.MathCurriculumRepository
import com.example.zazili.domain.curriculum.tecnologia.Chapter
import com.example.zazili.domain.curriculum.tecnologia.Lesson
import com.example.zazili.domain.curriculum.tecnologia.TechnologyModuleRepository
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.EstadoSaga
import com.example.zazili.domain.saga.MundoSaga
import com.example.zazili.domain.saga.NodoSaga
import com.example.zazili.domain.saga.RutaSaga
import com.example.zazili.presentation.lecciones.LessonEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Mapa adaptativo de la Saga.
 *
 * Lee el nivel de matemáticas calibrado en DataStore y coloca al niño en el
 * reto que le corresponde: nivel 1 abre el nodo 1; nivel 2 da por completados
 * los nodos 1–10 y abre el 11; nivel 3 salta al 21; nivel 4 al 31; nivel 5 al
 * 41. Los saltos solo se aplican una vez por instalación: la comprobación es
 * idempotente y nunca borra avance real.
 *
 * Si el diagnóstico marcó `needsAudioSupport`, el narrador se enciende de
 * forma global para que todas las lecciones y cápsulas de teoría se lean.
 */
class SagaMapViewModel(application: Application) :
    AndroidViewModel(application), FuenteMapaSaga {

    private val contenedor = (application as ZaziliApplication).container

    override val estado: StateFlow<EstadoSaga> = contenedor.progresoSaga.estado
    override val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    override val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles

    private val _audioApoyo = MutableStateFlow(false)
    val audioApoyo: StateFlow<Boolean> = _audioApoyo.asStateFlow()

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil.asStateFlow()

    /**
     * Primer nodo de Matemáticas sin completar. Es la fuente única del estado
     * `CURRENT` del mapa: no se guarda aparte, así jamás se desincroniza del
     * avance real.
     */
    val nodoActual: StateFlow<String?> = estado
        .map { actual ->
            MathCurriculumRepository.lecciones.firstOrNull {
                !actual.completado(it.id)
            }?.id ?: MathCurriculumRepository.lecciones.lastOrNull()?.id
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MathCurriculumRepository.lecciones.firstOrNull()?.id
        )

    private val mundosMatematicas: List<MundoSaga> = construirMundosMatematicas()
    private val mundosTecnologia: List<MundoSaga> = construirMundosTecnologia()

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
                    aplicarNivelInicial(perfil.nivelMatematicas)
                }
        }
    }

    /**
     * Entrega los mundos que pinta el mapa. Matemáticas usa los 5 capítulos
     * del currículo de 50 lecciones; los demás módulos conservan su mundo
     * original.
     */
    override fun mundosDe(modulo: ModuloZazili): List<MundoSaga> {
        if (modulo == ModuloZazili.TIANGUIS_NUMERICO) return mundosMatematicas
        if (modulo == ModuloZazili.PIRAMIDE_CODIGO) return mundosTecnologia
        return listOfNotNull(RutaSaga.mundoDe(modulo))
            .ifEmpty { listOf(RutaSaga.mundos.first()) }
    }

    /**
     * Completa los nodos que el diagnóstico ya domina. Es idempotente: si el
     * niño ya avanzó, solo rellena los que falten y nunca quita estrellas.
     */
    private fun aplicarNivelInicial(nivel: Int) {
        val total = nodosQueCorrespondenAlNivel(nivel)
        if (total <= 0) return
        val nodosDelNivel = MathCurriculumRepository.lecciones
            .take(total)
            .map { it.id }
        contenedor.progresoSaga.completarVarios(
            nodoIds = nodosDelNivel,
            estrellas = ESTRELLAS_ADAPTATIVAS
        )
    }

    private fun nodosQueCorrespondenAlNivel(nivel: Int): Int = when (nivel) {
        2 -> 10
        3 -> 20
        4 -> 30
        5 -> 40
        else -> 0
    }

    private fun construirMundosMatematicas(): List<MundoSaga> =
        MathCurriculumRepository.capitulos.map { capitulo ->
            MundoSaga(
                modulo = ModuloZazili.TIANGUIS_NUMERICO,
                jefe = capitulo.jefe,
                dichoDelJefe = capitulo.dichoDelJefe,
                nodos = MathCurriculumRepository.leccionesDe(capitulo).map(::aNodoSaga),
                nombreCapitulo = capitulo.titulo
            )
        }

    private fun aNodoSaga(leccion: LessonNode): NodoSaga {
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

    /**
     * Tecnología se juega en bloques de cinco lecciones. Cada capítulo de diez
     * lecciones ocupa dos nodos del camino y el segundo nodo es el reto del
     * jefe. Los ids son los mismos que usan el motor de lecciones y el avance
     * persistido (`tecnologia_bloque_N`), así que mapa, lección y desbloqueo
     * nunca se desincronizan.
     */
    private fun construirMundosTecnologia(): List<MundoSaga> {
        var bloqueGlobal = 1
        return TechnologyModuleRepository.capitulos.map { capitulo ->
            val bloques = capitulo.lessons.chunked(LessonEngine.LECCIONES_POR_BLOQUE)
            val nodos = bloques.mapIndexed { indice, lecciones ->
                aNodoSagaTecnologia(
                    capitulo = capitulo,
                    lecciones = lecciones,
                    bloque = bloqueGlobal + indice,
                    esJefe = indice == bloques.lastIndex
                )
            }
            bloqueGlobal += bloques.size
            MundoSaga(
                modulo = ModuloZazili.PIRAMIDE_CODIGO,
                jefe = capitulo.jefe,
                dichoDelJefe = capitulo.dichoDelJefe,
                nodos = nodos,
                nombreCapitulo = capitulo.title
            )
        }
    }

    private fun aNodoSagaTecnologia(
        capitulo: Chapter,
        lecciones: List<Lesson>,
        bloque: Int,
        esJefe: Boolean
    ): NodoSaga {
        val reto = lecciones.first()
        val indiceCorrecto = reto.options
            .indexOfFirst { it.isCorrect }
            .coerceAtLeast(0)
        return NodoSaga(
            id = LessonEngine.nodoDeBloque(bloque),
            nombre = if (esJefe) {
                "El reto de ${capitulo.jefe}"
            } else {
                "${capitulo.title}: primeros pasos"
            },
            enunciado = reto.question,
            opciones = reto.options.map { it.text },
            indiceCorrecto = indiceCorrecto,
            pista = "Repasa lo aprendido en ${capitulo.title} antes de responder.",
            esJefe = esJefe
        )
    }

    companion object {
        /** Los nodos que el diagnóstico salta cuentan como dominio pleno. */
        private const val ESTRELLAS_ADAPTATIVAS = 3
    }
}
