package com.example.zazili.presentation.lecciones

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.logros.AccionesLogro
import com.example.zazili.core.logros.AchievementDispatcher
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.progress.UserProgressManager
import com.example.zazili.domain.ciencias.CapaFiltro
import com.example.zazili.domain.ciencias.ElementoCientifico
import com.example.zazili.domain.ciencias.ORDEN_FILTRO
import com.example.zazili.domain.curriculum.LessonNode
import com.example.zazili.domain.curriculum.MathCurriculumRepository
import com.example.zazili.domain.curriculum.ciencias.LessonNode as LeccionCiencia
import com.example.zazili.domain.curriculum.ciencias.MecanicaCiencia
import com.example.zazili.domain.curriculum.ciencias.ScienceCurriculumRepository
import com.example.zazili.domain.curriculum.emociones.MentalHealthCurriculumRepository
import com.example.zazili.domain.curriculum.lengua.LanguageCurriculumRepository
import com.example.zazili.domain.curriculum.lengua.LessonNode as LeccionLengua
import com.example.zazili.domain.curriculum.lengua.MecanicaLengua
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.modulos.ModuloZazili
import java.util.ArrayDeque
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Contrato mínimo que la pantalla de lección sabe renderizar.
 *
 * Las opciones y la respuesta correcta viven en el contrato, no en cada
 * pantalla: así la hoja de retroalimentación y el motor de cola funcionan
 * igual para cualquiera de los 16 minijuegos.
 */
@Immutable
sealed interface LessonExercise {
    val id: String
    val titulo: String
    val consigna: String
    val pista: String
    val opciones: List<String>
    val indiceCorrecto: Int
    val dificultad: Int
}

/**
 * Reto del currículo de Matemáticas (50 lecciones). [conceptosDificiles] pinta
 * las palabras con subrayado punteado y [guiaFoco] es el apoyo visual que se
 * compra con 10 Zazoles.
 */
@Immutable
data class MathExercise(
    override val id: String,
    override val titulo: String,
    override val consigna: String,
    override val pista: String,
    val enunciado: String,
    override val opciones: List<String>,
    override val indiceCorrecto: Int,
    override val dificultad: Int,
    val conceptosDificiles: List<String> = emptyList(),
    val guiaFoco: List<String> = emptyList(),
    val icono: String = "🍎",
    /** Etiqueta de la tarjeta del reto; Tecnología reusa esta misma pantalla. */
    val etiquetaReto: String = "RETO DE MATEMÁTICAS"
) : LessonExercise

/**
 * Reto del currículo de Lengua (50 lecciones). [mecanica] decide qué minijuego
 * lo pinta: Atrapa-Sonidos para conciencia fonológica o el reto de opción
 * múltiple para sílabas, ortografía, gramática, lectura y pensamiento crítico.
 * [audioObjetivo] es el sonido que el altavoz pronuncia en Atrapa-Sonidos.
 */
@Immutable
data class LanguageExercise(
    override val id: String,
    override val titulo: String,
    override val consigna: String,
    override val pista: String,
    val enunciado: String,
    override val opciones: List<String>,
    override val indiceCorrecto: Int,
    override val dificultad: Int,
    val mecanica: MecanicaLengua,
    val audioObjetivo: String? = null,
    val conceptosDificiles: List<String> = emptyList(),
    val icono: String = "📖"
) : LessonExercise

@Immutable
data class CalculoMentalExercise(
    override val id: String,
    val enunciado: String,
    override val opciones: List<String>,
    override val indiceCorrecto: Int,
    override val dificultad: Int = 1,
    val gruposVisuales: List<Int> = emptyList(),
    override val titulo: String = "Cálculo Mental",
    override val consigna: String = "Elige el resultado correcto. Sin prisa.",
    override val pista: String = "Puedes contar con los dedos o con los tacos."
) : LessonExercise

/**
 * Micro-ronda de Ciencias. [mecanica] elige la pantalla que la pinta: opciones,
 * filtro mágico o arenero de reacciones. [minijuegoId] conserva el enlace con
 * el gimnasio de minijuegos; [elementos], [resultadoEsperado] y [capasCorrectas]
 * alimentan al arenero sin que la UI invente materiales.
 */
@Immutable
data class ScienceExercise(
    override val id: String,
    val minijuegoId: String,
    override val titulo: String,
    override val consigna: String,
    override val pista: String,
    val enunciado: String,
    override val opciones: List<String>,
    override val indiceCorrecto: Int,
    override val dificultad: Int,
    val visuales: List<String> = emptyList(),
    val mecanica: MecanicaCiencia = MecanicaCiencia.OPCIONES,
    val elementos: List<ElementoCientifico> = emptyList(),
    val resultadoEsperado: ElementoCientifico? = null,
    val capasCorrectas: List<CapaFiltro> = emptyList()
) : LessonExercise

@Immutable
sealed interface LessonFeedback {
    val ejercicio: LessonExercise

    data class Acierto(override val ejercicio: LessonExercise) : LessonFeedback

    data class Error(
        override val ejercicio: LessonExercise,
        val seleccion: Int
    ) : LessonFeedback
}

/**
 * Estado seguro para la pantalla. [currentExercise] es nullable a propósito:
 * así la UI decide entre cargar, estudiar la teoría, mostrar el reto o cerrar.
 * [mostrarTeoria] cubre la cápsula inicial; [pistaVisible] y [focoUsado] viven
 * aquí para que una recomposición jamás borre el apoyo que el niño encendió.
 */
@Immutable
data class LessonState(
    val isLoading: Boolean = true,
    val currentExercise: LessonExercise? = null,
    val hearts: Int = LessonViewModel.VIDAS_INICIALES,
    val progress: Float = 0f,
    val totalExercises: Int = 0,
    val solvedExercises: Int = 0,
    val feedback: LessonFeedback? = null,
    val isBreathing: Boolean = false,
    val isFinished: Boolean = false,
    val stars: Int = 0,
    val roundToken: Int = 0,
    val teoria: TheoryContent? = null,
    val mostrarTeoria: Boolean = false,
    val pistaVisible: Boolean = false,
    val focoUsado: Boolean = false,
    val guiaFoco: List<String> = emptyList(),
    val focoAviso: String? = null,
    val audioApoyo: Boolean = false,
    /** Nodo que se abrirá al tocar «¡Siguiente Lección!»; `null` si no hay. */
    val siguienteNodoId: String? = null,
    /** En la última lección del mapa se oculta «¡Siguiente Lección!». */
    val esUltimaLeccion: Boolean = false
)

/**
 * Motor de lección de Zazili 2.0.
 *
 * Reglas de juego:
 *  - 5 corazones. Un error resta uno y manda el reto al final de la cola.
 *  - Al llegar a 0 corazones NO hay derrota: [completarRespiracion] devuelve
 *    un corazón después de un ciclo de respiración 4-2-4.
 *  - El Foco cuesta exactamente 10 Zazoles, una vez por reto.
 *  - La pista de las palabras punteadas es gratis y no gasta nada.
 *  - Al terminar, el avance se guarda en la Saga para abrir el siguiente nodo.
 */
class LessonViewModel(
    application: Application,
    private val modulo: ModuloZazili,
    private var nodoId: String?
) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container
    private val progresoUsuario = UserProgressManager.obtener(application)
    private val moduleId: Int? = LessonEngine.idDeModulo(modulo)

    val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles

    private val cola = ArrayDeque<LessonExercise>()
    private var totalEjercicios = 0
    private var errores = 0
    private var audioApoyo = false
    private var trabajoDeCarga: Job? = null
    private var generacion = 0
    private var bloqueTecnologia: Int? = null

    private val _state = MutableStateFlow(LessonState(isLoading = true))
    val state: StateFlow<LessonState> = _state.asStateFlow()

    init {
        configurarApoyoAudio()
        cargarLeccion()
    }

    /**
     * Si el diagnóstico detectó necesidad de audio, el narrador global queda
     * encendido y esta lección se lee sola. No se apaga si el perfil no lo
     * pidió: puede haber quedado encendido a mano en Ajustes.
     */
    private fun configurarApoyoAudio() {
        viewModelScope.launch {
            val perfil = try {
                contenedor.userManager.perfil
                    .filterNotNull()
                    .firstOrNull()
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Throwable) {
                null
            } ?: return@launch
            if (perfil.apoyoAudio) {
                runCatching { StoryNarratorEngine.setHabilitado(true) }
                audioApoyo = true
                _state.update { it.copy(audioApoyo = true) }
            }
        }
    }

    @Synchronized
    fun enviarRespuesta(seleccion: Int, esCorrecta: Boolean) {
        val actual = _state.value
        if (actual.isLoading || actual.isFinished || actual.isBreathing) return
        if (actual.mostrarTeoria || actual.feedback != null) return
        val ejercicio = actual.currentExercise ?: return
        if (seleccion !in ejercicio.opciones.indices) return

        val acierto = esCorrecta && seleccion == ejercicio.indiceCorrecto
        if (acierto) {
            GlobalAudioManager.playSuccess()
            if (cola.isNotEmpty()) cola.removeFirst()
            _state.update { estado ->
                val resueltos = estado.solvedExercises + 1
                estado.copy(
                    solvedExercises = resueltos,
                    progress = progresoDe(resueltos, estado.totalExercises),
                    feedback = LessonFeedback.Acierto(ejercicio),
                    pistaVisible = false,
                    focoAviso = null
                )
            }
        } else {
            GlobalAudioManager.playMiss()
            errores++
            if (cola.isNotEmpty()) {
                cola.removeFirst()
                cola.addLast(ejercicio)
            }
            _state.update {
                it.copy(
                    hearts = (it.hearts - 1).coerceAtLeast(0),
                    feedback = LessonFeedback.Error(ejercicio, seleccion),
                    pistaVisible = false,
                    focoAviso = null
                )
            }
        }
    }

    @Synchronized
    fun continuar() {
        val actual = _state.value
        if (actual.isBreathing || actual.isFinished || actual.isLoading) return
        if (actual.mostrarTeoria) return
        val retroalimentacion = actual.feedback ?: return

        when (retroalimentacion) {
            is LessonFeedback.Acierto -> {
                if (actual.solvedExercises >= actual.totalExercises || cola.isEmpty()) {
                    finalizar()
                } else {
                    avanzar()
                }
            }

            is LessonFeedback.Error -> {
                if (actual.hearts <= 0) {
                    iniciarRespiracion()
                } else {
                    avanzar()
                }
            }
        }
    }

    /**
     * La pantalla de respiración avisa al terminar su ciclo 4-2-4: aquí se
     * devuelve UN corazón y se retoma la cola exactamente donde quedó.
     */
    @Synchronized
    fun completarRespiracion() {
        val actual = _state.value
        if (!actual.isBreathing) return
        _state.update {
            it.copy(
                isBreathing = false,
                hearts = (it.hearts + 1).coerceAtMost(VIDAS_INICIALES),
                currentExercise = cola.firstOrNull(),
                feedback = null,
                pistaVisible = false,
                focoUsado = false,
                guiaFoco = emptyList(),
                focoAviso = null,
                roundToken = it.roundToken + 1
            )
        }
    }

    @Synchronized
    fun alternarPista() {
        val actual = _state.value
        if (actual.isLoading || actual.isFinished || actual.isBreathing) return
        if (actual.mostrarTeoria || actual.feedback != null) return
        _state.update { it.copy(pistaVisible = !it.pistaVisible) }
    }

    @Synchronized
    fun cerrarPista() {
        _state.update {
            if (it.pistaVisible) it.copy(pistaVisible = false) else it
        }
    }

    @Synchronized
    fun cerrarTeoria() {
        _state.update {
            if (it.mostrarTeoria) {
                it.copy(mostrarTeoria = false, roundToken = it.roundToken + 1)
            } else {
                it
            }
        }
    }

    /**
     * Enciende el Foco: cobra 10 Zazoles y revela la guía visual más el inicio
     * de la respuesta. Si no alcanza el saldo, no cobra nada y deja un aviso
     * amable que recuerda la pista gratuita.
     */
    @Synchronized
    fun usarFoco() {
        val actual = _state.value
        if (actual.isLoading || actual.isFinished || actual.isBreathing) return
        if (actual.mostrarTeoria || actual.feedback != null) return
        val ejercicio = actual.currentExercise as? MathExercise ?: return

        if (actual.focoUsado) {
            _state.update {
                it.copy(focoAviso = "Este reto ya tiene el Foco encendido. Puedes usarlo en el siguiente.")
            }
            return
        }

        if (contenedor.economiaZazoles.gastar(COSTO_FOCO)) {
            GlobalAudioManager.playPop()
            _state.update {
                it.copy(
                    focoUsado = true,
                    guiaFoco = ejercicio.guiaFoco,
                    pistaVisible = false,
                    focoAviso = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    focoAviso = "Te faltan Zazoles para el Foco. La pista de las palabras punteadas es gratis."
                )
            }
        }
    }

    fun reiniciar() {
        trabajoDeCarga?.cancel()
        cargarLeccion()
    }

    /**
     * Abre la lección que sigue sin salir de la pantalla: cambia el nodo actual
     * y reconstruye la cola desde el currículo. El desbloqueo ya quedó guardado
     * en DataStore al completar la lección anterior, así que aquí solo se
     * reinicia el estado de juego.
     */
    @Synchronized
    fun siguienteLeccion() {
        val actual = _state.value
        if (!actual.isFinished) return
        val siguiente = actual.siguienteNodoId ?: return
        nodoId = siguiente
        cargarLeccion()
    }

    private fun cargarLeccion() {
        trabajoDeCarga?.cancel()
        val token = ++generacion
        cola.clear()
        totalEjercicios = 0
        errores = 0
        bloqueTecnologia = null
        _state.value = LessonState(
            isLoading = true,
            hearts = VIDAS_INICIALES,
            roundToken = _state.value.roundToken + 1,
            audioApoyo = audioApoyo
        )

        trabajoDeCarga = viewModelScope.launch {
            val contenido = withContext(Dispatchers.Default) {
                runCatching { construirContenido() }
                    .getOrElse {
                        val respaldo = when (modulo) {
                            ModuloZazili.CUEVA_LEYENDAS ->
                                LanguageCurriculumRepository.lecciones.firstOrNull()
                                    ?.let(::construirLengua)
                                    ?: construirCalculoMental()

                            ModuloZazili.LABORATORIO_CENOTE ->
                                ejerciciosDeRespaldoCiencia()

                            else -> construirCalculoMental()
                        }
                        ContenidoLeccion(respaldo, null)
                    }
            }
            if (token != generacion) return@launch

            bloqueTecnologia = contenido.bloqueTecnologia

            // Las opciones se mezclan UNA vez, antes de entrar a la cola: el
            // niño ve casillas nuevas en cada partida y la respuesta correcta
            // sigue al texto aunque cambie de letra. Un reto que regresa por
            // error conserva exactamente el mismo orden.
            cola.addAll(contenido.ejercicios.map { LessonEngine.barajar(it) })
            // Torniquete de seguridad: Ciencias nunca arranca con la cola
            // vacía. Si el currículo no dio retos, entran tres filtros válidos.
            if (cola.isEmpty() && modulo == ModuloZazili.LABORATORIO_CENOTE) {
                cola.addAll(ejerciciosDeRespaldoCiencia().map { LessonEngine.barajar(it) })
            }
            totalEjercicios = cola.size
            _state.value = LessonState(
                isLoading = false,
                currentExercise = cola.firstOrNull(),
                hearts = VIDAS_INICIALES,
                progress = 0f,
                totalExercises = totalEjercicios,
                solvedExercises = 0,
                teoria = contenido.teoria,
                mostrarTeoria = contenido.teoria != null,
                roundToken = _state.value.roundToken + 1,
                audioApoyo = audioApoyo
            )
        }
    }

    private fun avanzar() {
        if (cola.isEmpty()) {
            finalizar()
            return
        }
        _state.update {
            it.copy(
                currentExercise = cola.firstOrNull(),
                feedback = null,
                pistaVisible = false,
                focoUsado = false,
                guiaFoco = emptyList(),
                focoAviso = null,
                roundToken = it.roundToken + 1
            )
        }
    }

    private fun iniciarRespiracion() {
        _state.update {
            it.copy(
                isBreathing = true,
                feedback = null,
                pistaVisible = false,
                focoUsado = false,
                guiaFoco = emptyList(),
                focoAviso = null,
                roundToken = it.roundToken + 1
            )
        }
    }

    private fun finalizar() {
        val estrellas = when {
            errores == 0 -> 3
            errores <= 2 -> 2
            else -> 1
        }
        // Una partida de la Arena desde una lección también mueve su racha
        // adaptativa. Dos estrellas o más significan que el niño sostuvo el
        // reto; con menos, el motor baja la dificultad sin castigar.
        if (modulo == ModuloZazili.CUEVA_LEYENDAS &&
            nodoId == Minijuego.CodiceAnimado.id
        ) {
            runCatching {
                contenedor.dynamicDifficultyEngine
                    .registrarResultadoEnSegundoPlano(
                        juegoId = Minijuego.CodiceAnimado.id,
                        gano = estrellas >= 2
                    )
            }
        }
        // El cierre de la lección nunca depende del disco: si guardar el
        // avance falla, la pantalla de estrellas se muestra de todas formas.
        runCatching {
            val leccion = MathCurriculumRepository.leccionPorId(nodoId)
            if (modulo == ModuloZazili.TIANGUIS_NUMERICO && leccion != null) {
                contenedor.progresoSaga.completar(leccion.id, estrellas)
            }
            val leccionLengua = LanguageCurriculumRepository.leccionPorId(nodoId)
            if (modulo == ModuloZazili.CUEVA_LEYENDAS && leccionLengua != null) {
                contenedor.progresoSaga.completar(leccionLengua.id, estrellas)
            }
            val leccionCiencia = ScienceCurriculumRepository.leccionPorId(nodoId)
            if (modulo == ModuloZazili.LABORATORIO_CENOTE && leccionCiencia != null) {
                contenedor.progresoSaga.completar(leccionCiencia.id, estrellas)
            }
            // Tecnología entra por la misma puerta que Matemáticas: el Tablero
            // observa la saga y paga Zazoles, energía y felicidad una sola vez.
            val bloque = bloqueTecnologia
            if (modulo == ModuloZazili.PIRAMIDE_CODIGO && bloque != null) {
                contenedor.progresoSaga.completar(LessonEngine.nodoDeBloque(bloque), estrellas)
            }
        }

        // Desbloqueo persistente universal: al terminar la lección N se abre la
        // N+1 del mismo módulo. El módulo 5 se resuelve con SU currículo para
        // que el chequeo no dependa de que exista un nodo siguiente. Se lanza
        // aparte para que el guardado nunca retrase las estrellas.
        val planSiguiente = planDeSiguiente()
        val idModulo = moduleId
        val numeroActual = if (idModulo == LessonEngine.MODULO_EMOCIONES_ID) {
            MentalHealthCurriculumRepository.leccionPorId(nodoId)?.numero
        } else {
            planSiguiente?.numeroActual
        }
        val siguienteNumero = numeroActual?.plus(1)
        if (idModulo != null && siguienteNumero != null) {
            progresoUsuario.unlockNextLessonEnSegundoPlano(idModulo, siguienteNumero)
        }

        // Despachador global: la lección terminada también cuenta como
        // actividad completada y dispara la evaluación inmediata de logros.
        AchievementDispatcher.notifyAction(
            getApplication<Application>(),
            AccionesLogro.LECCION_COMPLETADA
        )
        // Un nodo que apunta a un minijuego del gimnasio es, además, una
        // partida jugada: así "Primeros Pasos" no queda como logro fantasma.
        if (Minijuego.porId(nodoId.orEmpty()) != null) {
            AchievementDispatcher.notifyAction(
                getApplication<Application>(),
                AccionesLogro.MINIJUEGO_COMPLETADO
            )
        }

        _state.update {
            it.copy(
                currentExercise = null,
                feedback = null,
                isBreathing = false,
                isFinished = true,
                progress = 1f,
                stars = estrellas,
                pistaVisible = false,
                guiaFoco = emptyList(),
                focoAviso = null,
                siguienteNodoId = planSiguiente?.nodoIdSiguiente,
                esUltimaLeccion = planSiguiente?.esUltima ?: false,
                roundToken = it.roundToken + 1
            )
        }
    }

    /**
     * Calcula qué lección sigue y si esta era la última del mapa. El número de
     * la lección sale del currículo, no del texto del id: así un nodo con
     * nombre raro nunca abre un destino inválido.
     */
    private fun planDeSiguiente(): PlanLeccionSiguiente? = when (modulo) {
        ModuloZazili.TIANGUIS_NUMERICO -> planEnLecciones(
            MathCurriculumRepository.lecciones.map { it.id to it.numero }
        )

        ModuloZazili.CUEVA_LEYENDAS -> planEnLecciones(
            LanguageCurriculumRepository.lecciones.map { it.id to it.numero }
        )

        ModuloZazili.LABORATORIO_CENOTE -> planEnLecciones(
            ScienceCurriculumRepository.lecciones.map { it.id to it.numero }
        )

        ModuloZazili.PIRAMIDE_CODIGO -> {
            val bloque = bloqueTecnologia
            if (bloque == null) {
                null
            } else {
                PlanLeccionSiguiente(
                    numeroActual = bloque,
                    nodoIdSiguiente = if (bloque < LessonEngine.BLOQUES_TECNOLOGIA) {
                        LessonEngine.nodoDeBloque(bloque + 1)
                    } else {
                        null
                    },
                    esUltima = bloque >= LessonEngine.BLOQUES_TECNOLOGIA
                )
            }
        }

        else -> null
    }

    private fun planEnLecciones(lecciones: List<Pair<String, Int>>): PlanLeccionSiguiente? {
        val actual = lecciones.firstOrNull { it.first == nodoId } ?: return null
        val siguiente = lecciones.firstOrNull { it.second == actual.second + 1 }
        return PlanLeccionSiguiente(
            numeroActual = actual.second,
            nodoIdSiguiente = siguiente?.first,
            esUltima = lecciones.none { it.second > actual.second }
        )
    }

    /**
     * Un nodo de Matemáticas se abre con su lección de 3 retos y su cápsula de
     * teoría; uno de Lengua, con sus retos y su propia cápsula. Los minijuegos
     * de Ciencias conservan sus micro-rondas; cualquier otro nodo cae en Cálculo
     * Mental para no dejar la cola vacía.
     */
    private fun construirContenido(): ContenidoLeccion {
        if (modulo == ModuloZazili.TIANGUIS_NUMERICO) {
            val leccion = MathCurriculumRepository.leccionPorId(nodoId)
            if (leccion != null) {
                return ContenidoLeccion(
                    ejercicios = construirMatematicas(leccion),
                    teoria = TheoryContent(
                        id = leccion.id,
                        titulo = leccion.titulo,
                        subtitulo = "Capítulo ${leccion.capitulo.numero} · ${leccion.subtitulo}",
                        icono = leccion.icono,
                        bullets = leccion.teoria
                    )
                )
            }
        }

        if (modulo == ModuloZazili.CUEVA_LEYENDAS) {
            val leccion = LanguageCurriculumRepository.leccionPorId(nodoId)
            if (leccion != null) {
                return ContenidoLeccion(
                    ejercicios = construirLengua(leccion),
                    teoria = TheoryContent(
                        id = leccion.id,
                        titulo = leccion.titulo,
                        subtitulo = "Capítulo ${leccion.capitulo.numero} · ${leccion.subtitulo}",
                        icono = leccion.icono,
                        bullets = leccion.teoria
                    )
                )
            }
            val juegoLengua = Minijuego.porId(nodoId.orEmpty())
                ?.takeIf { it.modulo.moduloZazili == modulo }
            if (juegoLengua != null) {
                val retos = construirLenguaGym(juegoLengua)
                if (retos.isNotEmpty()) return ContenidoLeccion(retos, teoria = null)
            }
            val respaldo = LanguageCurriculumRepository.lecciones.firstOrNull()
            if (respaldo != null) {
                return ContenidoLeccion(
                    ejercicios = construirLengua(respaldo),
                    teoria = TheoryContent(
                        id = respaldo.id,
                        titulo = respaldo.titulo,
                        subtitulo = "Capítulo ${respaldo.capitulo.numero} · ${respaldo.subtitulo}",
                        icono = respaldo.icono,
                        bullets = respaldo.teoria
                    )
                )
            }
        }

        if (modulo == ModuloZazili.LABORATORIO_CENOTE) {
            val leccion = ScienceCurriculumRepository.leccionPorId(nodoId)
            if (leccion != null) {
                val retos = construirCiencias(leccion)
                return ContenidoLeccion(
                    ejercicios = retos.ifEmpty { ejerciciosDeRespaldoCiencia() },
                    teoria = TheoryContent(
                        id = leccion.id,
                        titulo = leccion.titulo,
                        subtitulo = "Capítulo ${leccion.capitulo.numero} · ${leccion.subtitulo}",
                        icono = leccion.icono,
                        bullets = leccion.teoria
                    )
                )
            }
            val juegoCiencia = Minijuego.porId(nodoId.orEmpty())
                ?.takeIf { it.modulo.moduloZazili == modulo }
            val retosGym = juegoCiencia?.let(::construirCiencia).orEmpty()
            return ContenidoLeccion(
                ejercicios = retosGym.ifEmpty { ejerciciosDeRespaldoCiencia() },
                teoria = null
            )
        }

        if (modulo == ModuloZazili.PIRAMIDE_CODIGO) {
            val sesion = LessonEngine.sesionDeTecnologia(
                nodoId = nodoId,
                completados = contenedor.progresoSaga.estado.value.completados
            )
            return ContenidoLeccion(
                ejercicios = sesion.ejercicios,
                teoria = sesion.teoria,
                bloqueTecnologia = sesion.bloque
            )
        }

        val juego = Minijuego.porId(nodoId.orEmpty())
            ?.takeIf { it.modulo.moduloZazili == modulo }
        val ciencias = juego?.let(::construirCiencia).orEmpty()
        return ContenidoLeccion(
            ejercicios = ciencias.ifEmpty { construirCalculoMental() },
            teoria = null
        )
    }

    private fun construirCiencias(leccion: LeccionCiencia): List<LessonExercise> =
        leccion.preguntas.mapIndexed { indice, pregunta ->
            ScienceExercise(
                id = "${leccion.id}_reto_${indice + 1}",
                minijuegoId = when (pregunta.mecanica) {
                    MecanicaCiencia.FILTRO_MAGICO -> Minijuego.FiltroMagico.id
                    MecanicaCiencia.REACCION -> Minijuego.AlquimiaCenote.id
                    MecanicaCiencia.OPCIONES -> MINIJUEGO_OPCIONES_CIENCIA
                },
                titulo = leccion.titulo,
                consigna = "Reto ${indice + 1} de ${leccion.preguntas.size} · Nivel ${leccion.tier}",
                pista = pregunta.pista,
                enunciado = pregunta.enunciado,
                opciones = pregunta.opciones,
                indiceCorrecto = pregunta.indiceCorrecto,
                dificultad = leccion.tier,
                visuales = pregunta.visuales,
                mecanica = pregunta.mecanica,
                elementos = pregunta.elementos,
                resultadoEsperado = pregunta.resultadoEsperado,
                capasCorrectas = pregunta.capasCorrectas
            )
        }

    /**
     * Torniquete de Ciencias: tres filtros completos y jugables. Se usan solo
     * si el currículo o el minijuego pedido no producen retos, de modo que la
     * cola nunca quede vacía ni la pantalla reciba una opción fuera de rango.
     */
    private fun ejerciciosDeRespaldoCiencia(): List<LessonExercise> {
        val semilla = nodoId.orEmpty().ifBlank { "ciencias_libre" }
        val capas = ORDEN_FILTRO.ifEmpty {
            listOf(CapaFiltro.GRAVA, CapaFiltro.ARENA, CapaFiltro.CARBON, CapaFiltro.ALGODON)
        }
        val opciones = listOf(
            "Grava → Arena → Carbón → Algodón",
            "Algodón → Arena → Carbón → Grava",
            "Arena → Carbón → Algodón → Grava",
            "Carbón → Algodón → Arena → Grava"
        )
        val pistas = listOf(
            "La grava atrapa lo grande y el algodón detiene lo más fino.",
            "Piensa en un colador: primero lo grande y al final lo pequeño.",
            "El carbón limpia olores y el algodón cierra el paso a lo fino."
        )
        val enunciados = listOf(
            "Arrastra las capas al embudo: arriba lo más grande, abajo lo más fino.",
            "El agua sucia pasa por las capas. ¿Qué orden la deja más limpia?",
            "Arma el filtro para que el agua del cenote salga clara."
        )
        return enunciados.mapIndexed { indice, enunciado ->
            ScienceExercise(
                id = "${semilla}_respaldo_${indice + 1}",
                minijuegoId = Minijuego.FiltroMagico.id,
                titulo = "Filtro Mágico",
                consigna = "Reto de respaldo ${indice + 1} · Nivel 1",
                pista = pistas[indice],
                enunciado = enunciado,
                opciones = opciones,
                indiceCorrecto = 0,
                dificultad = 1,
                visuales = capas.map { it.emoji },
                mecanica = MecanicaCiencia.FILTRO_MAGICO,
                capasCorrectas = capas
            )
        }
    }

    private fun construirLengua(leccion: LeccionLengua): List<LessonExercise> =
        leccion.preguntas.mapIndexed { indice, pregunta ->
            LanguageExercise(
                id = "${leccion.id}_reto_${indice + 1}",
                titulo = leccion.titulo,
                consigna = "Reto ${indice + 1} de ${leccion.preguntas.size} · Nivel ${leccion.tier}",
                pista = pregunta.pista,
                enunciado = pregunta.enunciado,
                opciones = pregunta.opciones,
                indiceCorrecto = pregunta.indiceCorrecto,
                dificultad = leccion.tier,
                mecanica = pregunta.mecanica,
                audioObjetivo = pregunta.audioObjetivo,
                conceptosDificiles = leccion.conceptos,
                icono = leccion.icono
            )
        }

    private fun construirLenguaGym(juego: Minijuego): List<LessonExercise> {
        if (juego.id == Minijuego.CodiceAnimado.id) {
            return construirCodiceAnimado(juego)
        }

        val mecanica = when (juego.id) {
            Minijuego.PuenteOraciones.id -> MecanicaLengua.GRAMATICA
            Minijuego.Cuentacuentos.id -> MecanicaLengua.LECTURA
            Minijuego.TlacuachePoeta.id -> MecanicaLengua.CRITICO
            else -> MecanicaLengua.ATRAPA_SONIDOS
        }
        val banco = LanguageCurriculumRepository.lecciones.flatMap { leccion ->
            leccion.preguntas
                .filter { it.mecanica == mecanica }
                .map { leccion to it }
        }
        val elegidas = when {
            banco.isEmpty() -> emptyList()
            banco.size <= RETOS_GYM -> banco
            else -> listOf(banco.first(), banco[banco.size / 2], banco.last())
        }
        return elegidas.mapIndexed { indice, (leccion, pregunta) ->
            LanguageExercise(
                id = "${juego.id}_reto_${indice + 1}",
                titulo = juego.titulo,
                consigna = "Reto ${indice + 1} de ${elegidas.size} · ${juego.titulo}",
                pista = pregunta.pista,
                enunciado = pregunta.enunciado,
                opciones = pregunta.opciones,
                indiceCorrecto = pregunta.indiceCorrecto,
                dificultad = leccion.tier,
                mecanica = pregunta.mecanica,
                audioObjetivo = pregunta.audioObjetivo,
                conceptosDificiles = leccion.conceptos,
                icono = leccion.icono
            )
        }
    }

    /**
     * Códice Animado: tres oraciones elegidas alrededor del nivel adaptativo
     * del juego. La palabra correcta vive DENTRO de la oración, así la pantalla
     * abre el hueco sin inventar texto y la hoja de retroalimentación muestra
     * exactamente la misma respuesta que el niño tocó.
     */
    private fun construirCodiceAnimado(juego: Minijuego): List<LessonExercise> {
        val nivel = runCatching {
            contenedor.dynamicDifficultyEngine.nivelActual(juego.id)
        }.getOrDefault(Minijuego.NIVEL_MINIMO)
            .coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO)

        // Ventana de dos niveles: una sola partida nunca salta de "jaguar" a
        // "biodiversidad".
        val desde = (nivel - 1).coerceAtLeast(Minijuego.NIVEL_MINIMO)
        val hasta = (nivel + 1).coerceAtMost(Minijuego.NIVEL_MAXIMO)
        val ventana = MISIONES_CODICE.filter { mision -> mision.nivel in desde..hasta }
        val elegidas = when {
            ventana.isEmpty() -> MISIONES_CODICE.take(RETOS_CODICE)
            ventana.size <= RETOS_CODICE -> ventana
            else -> listOf(
                ventana.first(),
                ventana[ventana.size / 3],
                ventana[ventana.size * 2 / 3],
                ventana.last()
            ).distinct()
        }

        return elegidas.mapIndexed { indice, mision ->
            val opciones = (mision.distractores + mision.correcta)
                .shuffled(Random(mision.enunciado.hashCode() + indice))
            LanguageExercise(
                id = "${juego.id}_reto_${indice + 1}",
                titulo = juego.titulo,
                consigna = "Reto ${indice + 1} de ${elegidas.size} · Completa la oración del códice.",
                pista = mision.pista,
                enunciado = mision.enunciado,
                opciones = opciones,
                indiceCorrecto = opciones.indexOf(mision.correcta),
                dificultad = mision.nivel,
                mecanica = MecanicaLengua.CODICE_ANIMADO,
                conceptosDificiles = listOf(mision.correcta),
                icono = "📜"
            )
        }
    }

    private fun construirMatematicas(leccion: LessonNode): List<LessonExercise> =
        leccion.preguntas.mapIndexed { indice, pregunta ->
            MathExercise(
                id = "${leccion.id}_reto_${indice + 1}",
                titulo = leccion.titulo,
                consigna = "Reto ${indice + 1} de ${leccion.preguntas.size} · Nivel ${leccion.tier}",
                pista = pregunta.pista,
                enunciado = pregunta.enunciado,
                opciones = pregunta.opciones,
                indiceCorrecto = pregunta.indiceCorrecto,
                dificultad = leccion.tier,
                conceptosDificiles = leccion.conceptos,
                guiaFoco = leccion.guiaFoco,
                icono = leccion.icono
            )
        }

    /**
     * Cuatro ejercicios válidos desde el primer frame. La lección nunca arranca
     * con una cola vacía, aunque el nodo pedido no exista en la ruta.
     */
    private fun construirCalculoMental(): List<LessonExercise> {
        val semilla = "${modulo.name.lowercase()}_${nodoId.orEmpty().ifBlank { "libre" }}"
        return listOf(
            CalculoMentalExercise(
                id = "${semilla}_calculo_1",
                enunciado = "3 + 4 = ?",
                opciones = listOf("6", "7", "8", "9"),
                indiceCorrecto = 1,
                dificultad = 1,
                gruposVisuales = listOf(3, 4),
                consigna = "Nivel 1 · Suma con calma y elige el resultado.",
                pista = "Cuenta con los tacos: primero 3, luego 4 más."
            ),
            CalculoMentalExercise(
                id = "${semilla}_calculo_2",
                enunciado = "8 − 5 = ?",
                opciones = listOf("2", "3", "4", "5"),
                indiceCorrecto = 1,
                dificultad = 1,
                consigna = "Nivel 1 · Quita y cuenta lo que queda.",
                pista = "Quita 5 de 8 y cuenta los que sobran."
            ),
            CalculoMentalExercise(
                id = "${semilla}_calculo_3",
                enunciado = "6 × 3 = ?",
                opciones = listOf("15", "16", "18", "21"),
                indiceCorrecto = 2,
                dificultad = 2,
                consigna = "Nivel 2 · Multiplica sin miedo.",
                pista = "Suma 6 tres veces: 6 + 6 + 6."
            ),
            CalculoMentalExercise(
                id = "${semilla}_calculo_4",
                enunciado = "1.5 + 2.4 = ?",
                opciones = listOf("3.9", "3.8", "4.9", "3.5"),
                indiceCorrecto = 0,
                dificultad = 3,
                consigna = "Nivel 3 · Suma con punto decimal.",
                pista = "Suma primero las décimas: 5 + 4 = 9."
            )
        )
    }

    /** Tres micro-rondas por juego de Ciencias, con dificultad 1 → 3. */
    private fun construirCiencia(juego: Minijuego): List<LessonExercise> {
        val prefijo = "${modulo.name.lowercase()}_${juego.id}"
        return semillasDe(juego.id).mapIndexed { indice, semilla ->
            val nivel = indice + 1
            ScienceExercise(
                id = "${prefijo}_$nivel",
                minijuegoId = juego.id,
                titulo = juego.titulo,
                consigna = "Nivel $nivel · ${juego.nivel(nivel).consigna}",
                pista = semilla.pista,
                enunciado = semilla.enunciado,
                opciones = semilla.opciones,
                indiceCorrecto = semilla.correcta,
                dificultad = nivel,
                visuales = semilla.visuales
            )
        }
    }

    private fun semillasDe(minijuegoId: String): List<SemillaCiencia> = when (minijuegoId) {
        Minijuego.AlquimiaCenote.id -> listOf(
            SemillaCiencia(
                enunciado = "Pones un hielo bajo el sol. ¿En qué se transforma?",
                opciones = listOf("Sólido", "Líquido", "Gas", "Polvo"),
                correcta = 1,
                pista = "El calor derrite el hielo y lo vuelve agua.",
                visuales = listOf("🧊", "☀️", "💧")
            ),
            SemillaCiencia(
                enunciado = "El agua de la olla hierve y sube vapor. ¿Qué cambio ocurre?",
                opciones = listOf("Se evapora", "Se congela", "Se hace piedra", "Se endulza"),
                correcta = 0,
                pista = "El calor convierte el líquido en gas.",
                visuales = listOf("🔥", "💧", "☁️")
            ),
            SemillaCiencia(
                enunciado = "El vapor toca la tapa fría y se vuelve gotitas. ¿Cómo se llama?",
                opciones = listOf("Evaporación", "Condensación", "Fusión", "Combustión"),
                correcta = 1,
                pista = "El frío junta el vapor y lo regresa a líquido.",
                visuales = listOf("☁️", "🧊", "💧")
            )
        )

        Minijuego.CadenasAlimenticias.id -> listOf(
            SemillaCiencia(
                enunciado = "¿Quién fabrica su propio alimento con la luz del sol?",
                opciones = listOf("El conejo", "La planta", "El hongo", "El jaguar"),
                correcta = 1,
                pista = "Las plantas son productoras: usan luz, agua y aire.",
                visuales = listOf("☀️", "🌱")
            ),
            SemillaCiencia(
                enunciado = "El conejo come plantas. ¿Qué lugar ocupa en la cadena?",
                opciones = listOf("Productor", "Herbívoro", "Depredador tope", "Descomponedor"),
                correcta = 1,
                pista = "Comer plantas sin cazar a otros animales te hace herbívoro.",
                visuales = listOf("🌱", "🐇")
            ),
            SemillaCiencia(
                enunciado = "¿Quién descompone la hoja seca y devuelve nutrientes a la tierra?",
                opciones = listOf("La mariposa", "El venado", "El hongo", "El pez"),
                correcta = 2,
                pista = "Los hongos son los descomponedores del bosque.",
                visuales = listOf("🍂", "🍄", "🌎")
            )
        )

        Minijuego.FiltroMagico.id -> listOf(
            SemillaCiencia(
                enunciado = "¿Qué capa atrapa primero las piedritas más grandes?",
                opciones = listOf("Algodón", "Arena fina", "Grava", "Carbón"),
                correcta = 2,
                pista = "La grava va arriba: sus huecos dejan pasar el agua y retienen lo grande.",
                visuales = listOf("Grava", "Arena fina", "Algodón")
            ),
            SemillaCiencia(
                enunciado = "¿Qué capa va hasta abajo para detener lo más fino?",
                opciones = listOf("Grava", "Algodón", "Arena gruesa", "Tapas"),
                correcta = 1,
                pista = "El algodón es el colador final del filtro.",
                visuales = listOf("Arena gruesa", "Arena fina", "Algodón")
            ),
            SemillaCiencia(
                enunciado = "¿Cuál es el orden correcto de arriba a abajo?",
                opciones = listOf(
                    "Grava, arena, algodón",
                    "Algodón, arena, grava",
                    "Arena, grava, algodón",
                    "Grava, algodón, arena"
                ),
                correcta = 0,
                pista = "Primero se atrapa lo grande y al final queda lo más fino.",
                visuales = listOf("Grava", "Arena", "Algodón")
            )
        )

        Minijuego.MonstruoBasura.id -> listOf(
            SemillaCiencia(
                enunciado = "¿En qué bote va la cáscara de plátano?",
                opciones = listOf("Orgánico", "Inorgánico", "Tóxico", "Vidrio"),
                correcta = 0,
                pista = "Los restos de comida se convierten en composta.",
                visuales = listOf("🍌")
            ),
            SemillaCiencia(
                enunciado = "¿En qué bote va la botella de plástico?",
                opciones = listOf("Orgánico", "Inorgánico", "Tóxico", "Composta"),
                correcta = 1,
                pista = "El plástico no se deshace solo: va al bote inorgánico.",
                visuales = listOf("🧴")
            ),
            SemillaCiencia(
                enunciado = "¿En qué bote va una pila usada?",
                opciones = listOf("Orgánico", "Inorgánico", "Tóxico", "Juguetes"),
                correcta = 2,
                pista = "Las pilas tienen metales peligrosos: van al contenedor tóxico.",
                visuales = listOf("🔋")
            )
        )

        else -> emptyList()
    }

    private fun progresoDe(resueltos: Int, total: Int): Float =
        if (total <= 0) 0f else (resueltos.toFloat() / total).coerceIn(0f, 1f)

    companion object {
        const val VIDAS_INICIALES = 5
        const val COSTO_FOCO = 10
        private const val RETOS_GYM = 3
        private const val RETOS_CODICE = 4
        const val MINIJUEGO_OPCIONES_CIENCIA = "opciones_ciencia"

        fun factory(
            application: Application,
            modulo: ModuloZazili,
            nodoId: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LessonViewModel(application, modulo, nodoId)
            }
        }
    }
}

@Immutable
private data class ContenidoLeccion(
    val ejercicios: List<LessonExercise>,
    val teoria: TheoryContent?,
    val bloqueTecnologia: Int? = null
)

/**
 * Destino que se ofrece en la pantalla de victoria. [numeroActual] es el escalón
 * del módulo que se acaba de completar y [esUltima] apaga «¡Siguiente Lección!».
 */
@Immutable
private data class PlanLeccionSiguiente(
    val numeroActual: Int,
    val nodoIdSiguiente: String?,
    val esUltima: Boolean
)

@Immutable
private data class SemillaCiencia(
    val enunciado: String,
    val opciones: List<String>,
    val correcta: Int,
    val pista: String,
    val visuales: List<String>
)

/**
 * Banco del Códice Animado. Cada oración contiene su palabra correcta tal cual
 * para que el hueco se abra en el lugar exacto; los distractores son plausibles
 * pero nunca sinónimos. Cuatro misiones por nivel, de lo cotidiano a lo
 * científico.
 */
@Immutable
internal data class MisionCodice(
    val nivel: Int,
    val enunciado: String,
    val correcta: String,
    val distractores: List<String>,
    val pista: String
)

internal val MISIONES_CODICE: List<MisionCodice> = listOf(
    // ------------------------------ NIVEL 1 ------------------------------
    MisionCodice(
        nivel = 1,
        enunciado = "El jaguar corre por la selva.",
        correcta = "jaguar",
        distractores = listOf("piedra", "nube"),
        pista = "Es un animal con manchas que corre rápido entre los árboles."
    ),
    MisionCodice(
        nivel = 1,
        enunciado = "El colibrí vuela entre flores.",
        correcta = "colibrí",
        distractores = listOf("tigre", "piedra"),
        pista = "Es un pájaro pequeñito que vuela muy rápido."
    ),
    MisionCodice(
        nivel = 1,
        enunciado = "La tortuga camina en la arena.",
        correcta = "tortuga",
        distractores = listOf("silla", "nube"),
        pista = "Es un animal lento que lleva su caparazón."
    ),
    MisionCodice(
        nivel = 1,
        enunciado = "El mono salta en los árboles.",
        correcta = "mono",
        distractores = listOf("plato", "luna"),
        pista = "Es un animal travieso que se cuelga de las ramas."
    ),
    // ------------------------------ NIVEL 2 ------------------------------
    MisionCodice(
        nivel = 2,
        enunciado = "El ajolote nada en el agua limpia del canal.",
        correcta = "ajolote",
        distractores = listOf("jaguar", "nube"),
        pista = "Es un animalito de agua con branquias de plumita."
    ),
    MisionCodice(
        nivel = 2,
        enunciado = "El maíz crece en la milpa.",
        correcta = "maíz",
        distractores = listOf("oro", "vidrio"),
        pista = "Es el grano con el que se hacen las tortillas."
    ),
    MisionCodice(
        nivel = 2,
        enunciado = "La mariposa monarca vuela hacia el bosque.",
        correcta = "mariposa",
        distractores = listOf("ballena", "piedra"),
        pista = "Tiene alas de colores y viaja en millones."
    ),
    MisionCodice(
        nivel = 2,
        enunciado = "El venado corre entre los árboles.",
        correcta = "venado",
        distractores = listOf("plato", "nube"),
        pista = "Es un animal de patas largas que salta."
    ),
    // ------------------------------ NIVEL 3 ------------------------------
    MisionCodice(
        nivel = 3,
        enunciado = "El tlacuache desliza palabras con mucha gracia.",
        correcta = "tlacuache",
        distractores = listOf("piedra", "silla"),
        pista = "Es el poeta marsupial de la Arena Cortex."
    ),
    MisionCodice(
        nivel = 3,
        enunciado = "La leyenda explica por qué el cielo se pinta de naranja.",
        correcta = "leyenda",
        distractores = listOf("receta", "piedra"),
        pista = "Es un relato antiguo que mezcla magia y verdad."
    ),
    MisionCodice(
        nivel = 3,
        enunciado = "El cenote guarda agua fresca entre las rocas.",
        correcta = "cenote",
        distractores = listOf("desierto", "nube"),
        pista = "Es una poza profunda de agua en la selva."
    ),
    MisionCodice(
        nivel = 3,
        enunciado = "El campesino siembra semillas antes de la lluvia.",
        correcta = "siembra",
        distractores = listOf("piedra", "nube"),
        pista = "Es la acción de poner semillas en la tierra."
    ),
    // ------------------------------ NIVEL 4 ------------------------------
    MisionCodice(
        nivel = 4,
        enunciado = "El sacerdote maya dibuja el códice con tinta roja.",
        correcta = "códice",
        distractores = listOf("plato", "nube"),
        pista = "Es el libro pintado de los pueblos antiguos."
    ),
    MisionCodice(
        nivel = 4,
        enunciado = "La metamorfosis transforma a la oruga en mariposa.",
        correcta = "metamorfosis",
        distractores = listOf("piedra", "nube"),
        pista = "Es el cambio completo del cuerpo de un animal."
    ),
    MisionCodice(
        nivel = 4,
        enunciado = "El jaguar acecha en silencio entre los árboles.",
        correcta = "acecha",
        distractores = listOf("canta", "nada"),
        pista = "Es observar escondido, listo para saltar."
    ),
    MisionCodice(
        nivel = 4,
        enunciado = "La milpa alimenta a la familia durante todo el año.",
        correcta = "milpa",
        distractores = listOf("nube", "piedra"),
        pista = "Es el terreno donde se siembra maíz, frijol y calabaza."
    ),
    // ------------------------------ NIVEL 5 ------------------------------
    MisionCodice(
        nivel = 5,
        enunciado = "La biodiversidad mantiene el equilibrio de la selva.",
        correcta = "biodiversidad",
        distractores = listOf("piedra", "nube"),
        pista = "Es la variedad de seres vivos de un lugar."
    ),
    MisionCodice(
        nivel = 5,
        enunciado = "El mito narra el origen del fuego.",
        correcta = "mito",
        distractores = listOf("plato", "zapato"),
        pista = "Es un relato sagrado que explica el mundo."
    ),
    MisionCodice(
        nivel = 5,
        enunciado = "El tlacuache es un marsupial de hábitos nocturnos.",
        correcta = "marsupial",
        distractores = listOf("reptil", "piedra"),
        pista = "Es un mamífero que carga a sus crías en una bolsa."
    ),
    MisionCodice(
        nivel = 5,
        enunciado = "La chinampa permite cultivar sobre el agua.",
        correcta = "chinampa",
        distractores = listOf("nube", "piedra"),
        pista = "Es una isla de tierra y plantas hecha en los canales."
    )
)
