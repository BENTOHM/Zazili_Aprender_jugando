package com.example.zazili.presentation.saga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.domain.curriculum.ciencias.LessonNode as LeccionCiencia
import com.example.zazili.domain.curriculum.ciencias.PreguntaCiencia
import com.example.zazili.domain.curriculum.ciencias.ScienceCurriculumRepository
import com.example.zazili.domain.diagnostico.EstrategiaSemillas
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.EstadoSaga
import com.example.zazili.domain.saga.MundoSaga
import com.example.zazili.domain.saga.NodoSaga
import com.example.zazili.domain.saga.RutaSaga
import com.example.zazili.presentation.lecciones.LessonEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Mapa adaptativo de Ciencias (El Laboratorio del Cenote).
 *
 * El vuelo de prueba no guarda un nivel científico aparte: lo deduce de las dos
 * fases que sí midió. `nivelMatematicas` aporta la base y la estrategia con las
 * semillas dice cuánto razona por grupos; la edad solo afina un escalón. El
 * resultado es un `logicLevel` de 1 a 5:
 *
 *  1 → abre el nodo 1.
 *  2 → completa 1–10 y abre el 11.
 *  3 → completa 1–20 y abre el 21.
 *  4 → completa 1–30 y abre el 31.
 *  5 → completa 1–40 y abre el 41.
 *
 * La siembra es por lotes, idempotente y nunca borra estrellas ya ganadas. Si
 * el diagnóstico pidió apoyo de audio, el narrador queda encendido.
 *
 * Robustez: este ViewModel es la primera pieza que toca la pestaña de Ciencias,
 * así que ninguna lectura de DataStore ni siembra de progreso puede tumbar la
 * app. Ante cualquier fallo se queda en el nivel 1 y el mapa se pinta igual.
 */
class ScienceSagaViewModel(application: Application) :
    AndroidViewModel(application), FuenteMapaSaga {

    private val contenedor = (application as ZaziliApplication).container

    override val estado: StateFlow<EstadoSaga> = contenedor.progresoSaga.estado
    override val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    override val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles

    private val _logicLevel = MutableStateFlow(NIVEL_MINIMO)
    val logicLevel: StateFlow<Int> = _logicLevel.asStateFlow()

    private val _audioApoyo = MutableStateFlow(false)
    val audioApoyo: StateFlow<Boolean> = _audioApoyo.asStateFlow()

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil.asStateFlow()

    private val mundosCientificos: List<MundoSaga> = runCatching {
        construirMundosCientificos()
    }.getOrElse {
        listOf(mundoCientificoDeRespaldo())
    }

    private var nivelSembrado = 0

    init {
        viewModelScope.launch {
            try {
                contenedor.userManager.perfil
                    // Si DataStore falla (archivo corrupto, IO), el mapa debe
                    // abrir igual: se conserva el nivel 1 por defecto.
                    .catch { _ -> _logicLevel.value = NIVEL_MINIMO }
                    .filterNotNull()
                    .collect { perfil ->
                        aplicarPerfilSeguro(perfil)
                    }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Throwable) {
                _logicLevel.value = NIVEL_MINIMO
            }
        }
    }

    /**
     * Toda la reacción al perfil vive aquí y nunca propaga excepciones: un
     * dato raro del diagnóstico no puede cerrar la pestaña de Ciencias.
     */
    private fun aplicarPerfilSeguro(perfil: PerfilUsuario) {
        _perfil.value = perfil
        _audioApoyo.value = perfil.apoyoAudio
        if (perfil.apoyoAudio) {
            runCatching { StoryNarratorEngine.setHabilitado(true) }
        }

        val nivel = runCatching { calcularLogicLevel(perfil) }
            .getOrDefault(NIVEL_MINIMO)
            .coerceIn(NIVEL_MINIMO, NIVEL_MAXIMO)
        _logicLevel.value = nivel

        if (nivel <= nivelSembrado) return
        runCatching { aplicarNivelInicial(nivel) }
        nivelSembrado = nivel
    }

    override fun mundosDe(modulo: ModuloZazili): List<MundoSaga> {
        if (modulo == ModuloZazili.LABORATORIO_CENOTE) {
            return if (mundosCientificos.isEmpty()) {
                listOf(mundoCientificoDeRespaldo())
            } else {
                mundosCientificos
            }
        }
        return listOfNotNull(RutaSaga.mundoDe(modulo))
            .ifEmpty {
                RutaSaga.mundos.firstOrNull()?.let { listOf(it) }
                    ?: listOf(mundoCientificoDeRespaldo())
            }
    }

    /**
     * Siembra el tramo que el diagnóstico ya domina. Es idempotente: si el niño
     * avanzó por su cuenta, solo rellena lo que falte.
     */
    private fun aplicarNivelInicial(logicLevel: Int) {
        val total = nodosSembrados(logicLevel)
        if (total <= 0) return
        val ids = ScienceCurriculumRepository.lecciones
            .take(total)
            .map { it.id }
        if (ids.isEmpty()) return
        contenedor.progresoSaga.completarVarios(
            nodoIds = ids,
            estrellas = ESTRELLAS_ADAPTATIVAS
        )
    }

    private fun construirMundosCientificos(): List<MundoSaga> {
        val mundos = ScienceCurriculumRepository.capitulos.mapNotNull { capitulo ->
            val lecciones = ScienceCurriculumRepository.leccionesDe(capitulo)
            if (lecciones.isEmpty()) return@mapNotNull null
            MundoSaga(
                modulo = ModuloZazili.LABORATORIO_CENOTE,
                jefe = capitulo.jefe,
                dichoDelJefe = capitulo.dichoDelJefe,
                nodos = lecciones.map(::aNodoSaga),
                nombreCapitulo = capitulo.titulo
            )
        }
        return mundos.ifEmpty { listOf(mundoCientificoDeRespaldo()) }
    }

    private fun aNodoSaga(leccion: LeccionCiencia): NodoSaga {
        val reto = leccion.preguntas.firstOrNull() ?: preguntaDeRespaldo()
        val base = reto.opciones.ifEmpty { listOf("Sí", "No") }
        // El texto del currículo no se toca: solo se barajan las opciones y se
        // remapea el índice para que la respuesta correcta no sea siempre "A".
        val opciones = LessonEngine.opcionesBarajadas(
            opciones = base,
            indiceCorrecto = reto.indiceCorrecto
        )
        return NodoSaga(
            id = leccion.id,
            nombre = leccion.titulo,
            enunciado = reto.enunciado,
            opciones = opciones.map { it.text },
            indiceCorrecto = opciones.indexOfFirst { it.isCorrect }.coerceAtLeast(0),
            pista = reto.pista,
            esJefe = leccion.esJefeDeCapitulo
        )
    }

    private fun mundoCientificoDeRespaldo(): MundoSaga {
        val reto = preguntaDeRespaldo()
        val opciones = LessonEngine.opcionesBarajadas(
            opciones = reto.opciones,
            indiceCorrecto = reto.indiceCorrecto
        )
        return MundoSaga(
            modulo = ModuloZazili.LABORATORIO_CENOTE,
            jefe = "El Guardián del Cenote",
            dichoDelJefe = "El agua limpia no es magia: es orden, cuidado y trabajo en equipo.",
            nodos = listOf(
                NodoSaga(
                    id = "ciencias_respaldo_1",
                    nombre = "El primer experimento",
                    enunciado = reto.enunciado,
                    opciones = opciones.map { it.text },
                    indiceCorrecto = opciones.indexOfFirst { it.isCorrect }.coerceAtLeast(0),
                    pista = reto.pista,
                    esJefe = false
                )
            ),
            nombreCapitulo = "Laboratorio del Cenote"
        )
    }

    private fun preguntaDeRespaldo(): PreguntaCiencia = PreguntaCiencia(
        enunciado = "¿Qué necesita una planta para crecer sana?",
        opciones = listOf("Luz, agua y tierra", "Solo dulces", "Solo viento", "Nada"),
        indiceCorrecto = 0,
        pista = "Las plantas toman agua y luz para hacer su alimento."
    )

    companion object {

        private const val NIVEL_MINIMO = 1
        private const val NIVEL_MAXIMO = 5

        /** Los tramos que el diagnóstico salta cuentan como dominio pleno. */
        private const val ESTRELLAS_ADAPTATIVAS = 3

        /** Cuántas lecciones se dan por completadas en cada nivel. */
        fun nodosSembrados(logicLevel: Int): Int = when (logicLevel) {
            2 -> 10
            3 -> 20
            4 -> 30
            5 -> 40
            else -> 0
        }

        /**
         * Traduce la línea base del vuelo de prueba a un nivel científico.
         *
         * Base: 1..5 del diagnóstico de matemáticas. Las semillas agrupadas en
         * decenas suman 2; la agrupación parcial, 1. La edad ajusta un escalón:
         * los mayores de 9 suben uno y los de 6 años o menos bajan uno. Nunca
         * sale de 1..5.
         */
        fun calcularLogicLevel(perfil: PerfilUsuario): Int {
            val base = perfil.nivelMatematicas.coerceIn(NIVEL_MINIMO, NIVEL_MAXIMO)
            val bonus = when (perfil.estrategiaSemillas) {
                EstrategiaSemillas.DECENAS -> 2
                EstrategiaSemillas.AGRUPACION_PARCIAL -> 1
                EstrategiaSemillas.CONTEO_UNITARIO -> 0
                null -> 0
            }
            val edadAjuste = when {
                perfil.edad >= 9 && base + bonus < NIVEL_MAXIMO -> 1
                perfil.edad in 1..6 && base + bonus > NIVEL_MINIMO -> -1
                else -> 0
            }
            return (base + bonus + edadAjuste).coerceIn(NIVEL_MINIMO, NIVEL_MAXIMO)
        }
    }
}
