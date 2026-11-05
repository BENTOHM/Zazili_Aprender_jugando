package com.example.zazili.presentation.arena

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.logros.AccionesLogro
import com.example.zazili.core.logros.AchievementDispatcher
import com.example.zazili.core.logros.AchievementEngine
import com.example.zazili.core.logros.EventoLogro
import com.example.zazili.core.progress.UserProgressManager
import com.example.zazili.domain.arena.ArenaCortexRepository
import com.example.zazili.domain.arena.MinigameType
import com.example.zazili.domain.economia.ZazolEconomyManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Los tres minijuegos veloces de tecnología. */
enum class TechGame(val titulo: String, val subtitulo: String) {
    APLASTA_BUG("Aplasta el Bug", "Reflejos contra los bichos del sistema"),
    RUTA_ALGORITMO("Ruta del Algoritmo", "Programa el camino de Zazi a la batería"),
    FILTRO_CIBER("Filtro Ciber-Seguro", "Desliza y decide qué mensaje es seguro");

    companion object {
        /**
         * El nivel no se decide con aritmética: [ArenaCortexRepository] es la
         * única tabla que sabe si la lección es un juego legado o uno de los
         * tres nuevos. Los niveles legados nunca llegan aquí, pero si llegaran
         * el motor cae en el filtro ciber-seguro en lugar de reventar.
         */
        fun paraLeccion(lessonId: Int): TechGame =
            when (ArenaCortexRepository.tipoDeNivel(lessonId)) {
                MinigameType.WHACK_A_BUG -> APLASTA_BUG
                MinigameType.ALGORITHM_PATH -> RUTA_ALGORITMO
                MinigameType.CYBER_SWIPE,
                MinigameType.LEGACY_GAME_1,
                MinigameType.LEGACY_GAME_2,
                MinigameType.LEGACY_GAME_3,
                MinigameType.LEGACY_GAME_4,
                MinigameType.LEGACY_GAME_5,
                MinigameType.LEGACY_GAME_6,
                MinigameType.LEGACY_GAME_7,
                MinigameType.LEGACY_GAME_8,
                MinigameType.LEGACY_GAME_9,
                MinigameType.LEGACY_GAME_10,
                MinigameType.LEGACY_GAME_11,
                MinigameType.LEGACY_GAME_12,
                MinigameType.LEGACY_GAME_13,
                MinigameType.LEGACY_GAME_14,
                MinigameType.LEGACY_GAME_15,
                MinigameType.LEGACY_GAME_16 -> FILTRO_CIBER
            }
    }
}

// ---------------------------------------------------------------------------
//  JUEGO 1: APLASTA EL BUG
// ---------------------------------------------------------------------------

/** Un bicho (o escudo) vivo dentro de una celda del tablero. */
@Immutable
data class BichoVivo(
    val id: Long,
    val indice: Int,
    val esAntivirus: Boolean
)

@Immutable
data class EstadoAplastaBugTech(
    val puntaje: Int = 0,
    val bugsAplastados: Int = 0,
    val antivirusTocados: Int = 0,
    val tamano: Int = 3,
    val celdas: List<BichoVivo?> = List(9) { null },
    val terminado: Boolean = false
) {
    val progreso: Float
        get() = (puntaje.toFloat() / ArenaTechGameEngine.META_APLASTA).coerceIn(0f, 1f)

    val enFrenesi: Boolean
        get() = puntaje >= ArenaTechGameEngine.UMBRAL_FRENESI
}

sealed interface EventoAplastaBug {
    data class Acierto(val indice: Int, val puntos: Int) : EventoAplastaBug
    data class Error(val indice: Int) : EventoAplastaBug
}

// ---------------------------------------------------------------------------
//  JUEGO 2: RUTA DEL ALGORITMO
// ---------------------------------------------------------------------------

@Immutable
data class Punto(val fila: Int, val col: Int)

enum class Direccion(
    val titulo: String,
    val emoji: String,
    val deltaFila: Int,
    val deltaCol: Int
) {
    ARRIBA("Arriba", "⬆️", -1, 0),
    ABAJO("Abajo", "⬇️", 1, 0),
    IZQUIERDA("Izquierda", "⬅️", 0, -1),
    DERECHA("Derecha", "➡️", 0, 1)
}

enum class TipoCeldaRuta { LIBRE, MURO, TRAMPA, INICIO, META }

@Immutable
data class MapaAlgoritmo(
    val tamano: Int,
    val inicio: Punto,
    val meta: Punto,
    val muros: Set<Punto>,
    val trampas: Set<Punto>
) {
    fun dentro(punto: Punto): Boolean =
        punto.fila in 0 until tamano && punto.col in 0 until tamano

    fun tipoDe(punto: Punto): TipoCeldaRuta = when {
        !dentro(punto) || punto in muros -> TipoCeldaRuta.MURO
        punto in trampas -> TipoCeldaRuta.TRAMPA
        punto == meta -> TipoCeldaRuta.META
        punto == inicio -> TipoCeldaRuta.INICIO
        else -> TipoCeldaRuta.LIBRE
    }

    val maxPasos: Int
        get() = when (tamano) {
            3 -> 8
            4 -> 12
            else -> 16
        }
}

@Immutable
data class EstadoRutaTech(
    val mapa: MapaAlgoritmo,
    val secuencia: List<Direccion> = emptyList(),
    val zazi: Punto = mapa.inicio,
    val ejecutando: Boolean = false,
    val mensaje: String? = null,
    val choque: Boolean = false,
    val terminado: Boolean = false
)

// ---------------------------------------------------------------------------
//  JUEGO 3: FILTRO CIBER-SEGURO
// ---------------------------------------------------------------------------

/**
 * Una tarjeta del filtro. [esSegura] es el veredicto educativo, no una
 * etiqueta visible: el niño debe deducirlo antes de deslizar.
 */
@Immutable
data class TarjetaCiber(
    val id: String,
    val emisor: String,
    val mensaje: String,
    val esSegura: Boolean,
    val explicacion: String,
    val nivel: Int
)

@Immutable
data class EstadoFiltroTech(
    val tarjetaActual: TarjetaCiber? = null,
    val cartasResueltas: Int = 0,
    val aciertos: Int = 0,
    val errores: Int = 0,
    val racha: Int = 0,
    val total: Int = ArenaTechGameEngine.CARTAS_POR_RONDA,
    val aviso: TarjetaCiber? = null,
    val pausado: Boolean = false,
    val terminado: Boolean = false
) {
    val progreso: Float
        get() = if (total <= 0) 0f else (cartasResueltas.toFloat() / total).coerceIn(0f, 1f)
}

/** Resultado de completar cualquier minijuego. */
@Immutable
data class EstadoProgresoTech(
    val victoria: Boolean = false,
    val guardando: Boolean = false,
    val recompensaOtorgada: Boolean = false,
    val zazolesGanados: Int = 0
)

/**
 * Un solo motor para los tres minijuegos de tecnología.
 *
 * Recibe la lección del mapa y decide el juego con [TechGame.paraLeccion]. La
 * dificultad sale del número de lección: 1-3 tableros 3x3, 4-7 tableros 4x4
 * con firewalls y 8+ tableros 5x5 con trampas. Al ganar se guardan los Zazoles
 * y el desbloqueo ANTES de avisar a la UI; así el botón de regreso nunca
 * pierde la recompensa por una salida anticipada.
 */
class ArenaTechGameEngine(
    application: Application,
    val lessonId: Int
) : AndroidViewModel(application) {

    private val app: Application = application
    private val economia = ZazolEconomyManager.obtener(application)
    private val progresoUsuario = UserProgressManager.obtener(application)
    private val logros = AchievementEngine.obtener(application)

    val juego: TechGame = TechGame.paraLeccion(lessonId)

    // --- Juego 1 ----------------------------------------------------------
    private val tamanoTablero =
        if (lessonId >= LEYENDA_TABLERO_GRANDE) 4 else 3

    private val _estadoAplastaBug = MutableStateFlow(
        EstadoAplastaBugTech(
            tamano = tamanoTablero,
            celdas = List(tamanoTablero * tamanoTablero) { null }
        )
    )
    val estadoAplastaBug: StateFlow<EstadoAplastaBugTech> = _estadoAplastaBug.asStateFlow()

    private val _eventosAplastaBug = MutableSharedFlow<EventoAplastaBug>(
        extraBufferCapacity = 24,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventosAplastaBug: SharedFlow<EventoAplastaBug> = _eventosAplastaBug.asSharedFlow()

    private var trabajoBichos: Job? = null
    private var siguienteBicho = 0L
    private val azarBichos = Random(System.nanoTime())

    // --- Juego 2 ----------------------------------------------------------
    private val _estadoRuta = MutableStateFlow(
        generarMapa(lessonId).let { mapa ->
            EstadoRutaTech(mapa = mapa, zazi = mapa.inicio)
        }
    )
    val estadoRuta: StateFlow<EstadoRutaTech> = _estadoRuta.asStateFlow()

    // --- Juego 3 ----------------------------------------------------------
    private val mazo: List<TarjetaCiber> = generarMazo()
    private val _estadoFiltro = MutableStateFlow(
        EstadoFiltroTech(
            tarjetaActual = mazo.firstOrNull(),
            total = mazo.size
        )
    )
    val estadoFiltro: StateFlow<EstadoFiltroTech> = _estadoFiltro.asStateFlow()

    // --- Progreso compartido ----------------------------------------------
    private val _progresoTech = MutableStateFlow(EstadoProgresoTech())
    val progresoTech: StateFlow<EstadoProgresoTech> = _progresoTech.asStateFlow()

    private var recompensaEnCurso = false

    // -----------------------------------------------------------------------
    //  Juego 1: Aplasta el Bug
    // -----------------------------------------------------------------------

    /** Arranca el nacimiento de bichos. Es idempotente por pantalla. */
    fun iniciarAplastaBug() {
        if (juego != TechGame.APLASTA_BUG) return
        if (trabajoBichos?.isActive == true) return
        if (_estadoAplastaBug.value.terminado) return
        trabajoBichos = viewModelScope.launch {
            while (isActive) {
                val actual = _estadoAplastaBug.value
                if (actual.terminado) break
                val ventana = ventanaBugMs(actual.puntaje)
                val libres = actual.celdas.indices.filter { actual.celdas[it] == null }
                if (libres.isEmpty()) {
                    delay(120L)
                    continue
                }
                val indice = libres[azarBichos.nextInt(libres.size)]
                val antivirus = actual.puntaje >= UMBRAL_ANTIVIRUS &&
                    azarBichos.nextFloat() < PROBABILIDAD_ANTIVIRUS
                val bicho = BichoVivo(
                    id = ++siguienteBicho,
                    indice = indice,
                    esAntivirus = antivirus
                )
                _estadoAplastaBug.update { estado ->
                    if (estado.terminado) {
                        estado
                    } else {
                        estado.copy(
                            celdas = estado.celdas.toMutableList().also { it[indice] = bicho }
                        )
                    }
                }
                launch {
                    delay(ventana)
                    _estadoAplastaBug.update { estado ->
                        if (estado.celdas.getOrNull(indice)?.id != bicho.id) {
                            estado
                        } else {
                            estado.copy(
                                celdas = estado.celdas.toMutableList().also { it[indice] = null }
                            )
                        }
                    }
                }
                delay((ventana * RITMO_NACIMIENTO).toLong().coerceAtLeast(180L))
            }
        }
    }

    fun detenerAplastaBug() {
        trabajoBichos?.cancel()
        trabajoBichos = null
    }

    fun aplastarCelda(indice: Int) {
        if (juego != TechGame.APLASTA_BUG) return
        val actual = _estadoAplastaBug.value
        if (actual.terminado) return
        val bicho = actual.celdas.getOrNull(indice) ?: return
        val sinBicho = actual.celdas.toMutableList().also { it[indice] = null }
        if (bicho.esAntivirus) {
            GlobalAudioManager.playMiss()
            _estadoAplastaBug.update {
                it.copy(
                    celdas = sinBicho,
                    puntaje = (it.puntaje - CASTIGO_ANTIVIRUS).coerceAtLeast(0),
                    antivirusTocados = it.antivirusTocados + 1
                )
            }
            _eventosAplastaBug.tryEmit(EventoAplastaBug.Error(indice))
        } else {
            GlobalAudioManager.playPop()
            logros.notificar(EventoLogro.BUG_APLASTADO)
            _estadoAplastaBug.update {
                it.copy(
                    celdas = sinBicho,
                    puntaje = it.puntaje + PUNTOS_BICHO,
                    bugsAplastados = it.bugsAplastados + 1
                )
            }
            _eventosAplastaBug.tryEmit(EventoAplastaBug.Acierto(indice, PUNTOS_BICHO))
        }

        val nuevoPuntaje = _estadoAplastaBug.value.puntaje
        if (nuevoPuntaje >= META_APLASTA && !_estadoAplastaBug.value.terminado) {
            trabajoBichos?.cancel()
            trabajoBichos = null
            _estadoAplastaBug.update {
                it.copy(
                    terminado = true,
                    celdas = List(it.celdas.size) { null }
                )
            }
            GlobalAudioManager.playSuccess()
            otorgarRecompensa(nuevoPuntaje)
        }
    }

    // -----------------------------------------------------------------------
    //  Juego 2: Ruta del Algoritmo
    // -----------------------------------------------------------------------

    fun agregarDireccion(direccion: Direccion) {
        val actual = _estadoRuta.value
        if (actual.ejecutando || actual.terminado) return
        if (actual.secuencia.size >= actual.mapa.maxPasos) return
        GlobalAudioManager.playPop()
        _estadoRuta.update {
            it.copy(secuencia = it.secuencia + direccion, mensaje = null, choque = false)
        }
    }

    fun quitarUltimaDireccion() {
        val actual = _estadoRuta.value
        if (actual.ejecutando || actual.terminado) return
        _estadoRuta.update { it.copy(secuencia = it.secuencia.dropLast(1)) }
    }

    fun limpiarSecuencia() {
        val actual = _estadoRuta.value
        if (actual.ejecutando || actual.terminado) return
        _estadoRuta.update {
            it.copy(secuencia = emptyList(), mensaje = null, choque = false)
        }
    }

    fun reiniciarRuta() {
        if (_estadoRuta.value.ejecutando) return
        val mapa = generarMapa(lessonId)
        _estadoRuta.value = EstadoRutaTech(mapa = mapa, zazi = mapa.inicio)
    }

    fun ejecutarRuta() {
        val actual = _estadoRuta.value
        if (actual.ejecutando || actual.terminado || actual.secuencia.isEmpty()) return
        _estadoRuta.update {
            it.copy(ejecutando = true, mensaje = null, choque = false, zazi = it.mapa.inicio)
        }
        viewModelScope.launch {
            var posicion = actual.mapa.inicio
            var gano = false
            var fallo: String? = null

            for (paso in actual.secuencia) {
                delay(PASO_EJECUCION_MS)
                val destino = Punto(
                    fila = posicion.fila + paso.deltaFila,
                    col = posicion.col + paso.deltaCol
                )
                when {
                    !actual.mapa.dentro(destino) ||
                        actual.mapa.tipoDe(destino) == TipoCeldaRuta.MURO -> {
                        fallo = "¡Pum! Un firewall bloqueó a Zazi. Ajusta tu ruta."
                        break
                    }

                    actual.mapa.tipoDe(destino) == TipoCeldaRuta.TRAMPA -> {
                        _estadoRuta.update { it.copy(zazi = destino) }
                        fallo = "¡Ups! Una trampa. Prueba otro camino."
                        break
                    }

                    else -> {
                        posicion = destino
                        _estadoRuta.update { it.copy(zazi = destino) }
                        if (destino == actual.mapa.meta) {
                            gano = true
                            break
                        }
                    }
                }
            }

            if (gano) {
                GlobalAudioManager.playSuccess()
                logros.notificar(EventoLogro.NIVEL_ALGORITMO, lessonId)
                _estadoRuta.update {
                    it.copy(
                        ejecutando = false,
                        terminado = true,
                        mensaje = "¡Zazi llegó a la batería!"
                    )
                }
                otorgarRecompensa(recompensaRuta(actual.mapa))
            } else {
                if (fallo != null) GlobalAudioManager.playMiss()
                _estadoRuta.update {
                    it.copy(
                        ejecutando = false,
                        choque = fallo != null,
                        mensaje = fallo ?: "¡Casi! Ajusta tu ruta y vuelve a intentar."
                    )
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Juego 3: Filtro Ciber-Seguro
    // -----------------------------------------------------------------------

    /**
     * Resuelve la tarjeta visible. Si el veredicto es incorrecto, se abre el
     * aviso educativo y la partida se pausa hasta que el niño lo cierre.
     */
    fun responderCiber(aceptar: Boolean) {
        val actual = _estadoFiltro.value
        if (actual.pausado || actual.terminado) return
        val tarjeta = actual.tarjetaActual ?: return
        val acerto = aceptar == tarjeta.esSegura

        if (acerto) {
            GlobalAudioManager.playPop()
            _estadoFiltro.update {
                it.copy(
                    aciertos = it.aciertos + 1,
                    racha = it.racha + 1,
                    errores = it.errores
                )
            }
            avanzarCiber()
        } else {
            GlobalAudioManager.playMiss()
            _estadoFiltro.update {
                it.copy(
                    errores = it.errores + 1,
                    racha = 0,
                    aviso = tarjeta,
                    pausado = true
                )
            }
        }
    }

    /** Cierra el aviso educativo y continúa con la siguiente tarjeta. */
    fun cerrarAvisoCiber() {
        val actual = _estadoFiltro.value
        if (actual.aviso == null) return
        logros.notificar(EventoLogro.ESCENARIO_CIBER)
        _estadoFiltro.update { it.copy(aviso = null, pausado = false) }
        avanzarCiber()
    }

    private fun avanzarCiber() {
        val actual = _estadoFiltro.value
        val resueltas = actual.cartasResueltas + 1
        if (resueltas >= actual.total) {
            GlobalAudioManager.playSuccess()
            _estadoFiltro.update {
                it.copy(
                    cartasResueltas = resueltas,
                    tarjetaActual = null,
                    terminado = true,
                    pausado = false,
                    aviso = null
                )
            }
            otorgarRecompensa(recompensaCiber(actual.aciertos))
        } else {
            _estadoFiltro.update {
                it.copy(
                    cartasResueltas = resueltas,
                    tarjetaActual = mazo.getOrNull(resueltas)
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    //  Progreso, recompensa y desbloqueo
    // -----------------------------------------------------------------------

    /**
     * Suma los Zazoles y desbloquea la siguiente lección del módulo. La
     * victoria solo se publica cuando DataStore ya terminó de escribir.
     */
    private fun otorgarRecompensa(zazolesGanados: Int) {
        if (recompensaEnCurso || _progresoTech.value.recompensaOtorgada) return
        recompensaEnCurso = true
        val premio = zazolesGanados.coerceAtLeast(0)
        _progresoTech.update { it.copy(guardando = true, zazolesGanados = premio) }
        viewModelScope.launch {
            runCatching {
                economia.addZazoles(premio)
                progresoUsuario.unlockNextLesson(MODULO_ARENA_TECH, lessonId + 1)
            }
            AchievementDispatcher.notifyAction(app, AccionesLogro.MINIJUEGO_COMPLETADO)
            _progresoTech.update {
                it.copy(
                    guardando = false,
                    recompensaOtorgada = true,
                    victoria = true,
                    zazolesGanados = premio
                )
            }
            recompensaEnCurso = false
        }
    }

    private fun recompensaRuta(mapa: MapaAlgoritmo): Int = 20 + mapa.tamano * 10

    private fun recompensaCiber(aciertos: Int): Int = 20 + aciertos * 5

    companion object {
        /**
         * Llave interna del módulo que hospeda la Arena Cortex. Sale del
         * repositorio unificado para que mapa, motor y desbloqueo no puedan
         * apuntar a módulos distintos.
         */
        const val MODULO_ARENA_TECH = ArenaCortexRepository.MODULO_ARENA_CORTEX

        const val META_APLASTA = 200
        const val UMBRAL_ANTIVIRUS = 50
        const val UMBRAL_FRENESI = 150
        const val CARTAS_POR_RONDA = 12

        private const val PUNTOS_BICHO = 10
        private const val CASTIGO_ANTIVIRUS = 10
        private const val PROBABILIDAD_ANTIVIRUS = 0.30f
        private const val RITMO_NACIMIENTO = 0.60f
        private const val PASO_EJECUCION_MS = 300L
        private const val LEYENDA_TABLERO_GRANDE = 4
        private const val CARTAS_POR_NIVEL = 4

        /**
         * Ventana en milisegundos que un bicho permanece visible.
         * 0-49 → 1200 ms · 50-149 → 800 ms · 150+ → 500 ms.
         */
        fun ventanaBugMs(puntaje: Int): Long = when {
            puntaje >= UMBRAL_FRENESI -> 500L
            puntaje >= UMBRAL_ANTIVIRUS -> 800L
            else -> 1200L
        }

        /** Número de celdas por lado según la lección. */
        fun tamanoDeMapa(lessonId: Int): Int = when {
            lessonId <= 3 -> 3
            lessonId <= 7 -> 4
            else -> 5
        }

        /**
         * Genera un mapa siempre resoluble: primero traza un camino monótono
         * de la salida a la batería y después coloca firewalls y trampas SOLO
         * fuera de ese camino.
         */
        fun generarMapa(lessonId: Int, azar: Random = Random.Default): MapaAlgoritmo {
            val tamano = tamanoDeMapa(lessonId)
            val inicio = Punto(0, 0)
            val meta = Punto(tamano - 1, tamano - 1)
            val pasos = buildList {
                repeat(tamano - 1) { add(Direccion.DERECHA) }
                repeat(tamano - 1) { add(Direccion.ABAJO) }
            }.shuffled(azar)

            val camino = mutableListOf(inicio)
            var posicion = inicio
            pasos.forEach { paso ->
                posicion = Punto(
                    fila = posicion.fila + paso.deltaFila,
                    col = posicion.col + paso.deltaCol
                )
                camino += posicion
            }

            val ocupados = camino.toSet()
            val libres = buildList {
                for (fila in 0 until tamano) {
                    for (col in 0 until tamano) {
                        val punto = Punto(fila, col)
                        if (punto !in ocupados) add(punto)
                    }
                }
            }.shuffled(azar)

            val totalMuros = when (tamano) {
                3 -> 0
                4 -> 3
                else -> 6
            }
            val totalTrampas = if (tamano >= 5) 2 else 0
            val muros = libres.take(totalMuros).toSet()
            val trampas = libres.drop(totalMuros).take(totalTrampas).toSet()

            return MapaAlgoritmo(
                tamano = tamano,
                inicio = inicio,
                meta = meta,
                muros = muros,
                trampas = trampas
            )
        }

        /** Mazo de una ronda: cuatro tarjetas por nivel, de obvia a engañosa. */
        fun generarMazo(azar: Random = Random.Default): List<TarjetaCiber> {
            val porNivel = TarjetasCiber.todas.groupBy { it.nivel }
            return (1..3).flatMap { nivel ->
                porNivel[nivel].orEmpty().shuffled(azar).take(CARTAS_POR_NIVEL)
            }
        }

        fun factory(
            application: Application,
            lessonId: Int
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ArenaTechGameEngine(application, lessonId)
            }
        }
    }
}

/**
 * Banco de tarjetas del Filtro Ciber-Seguro: 36 mensajes en tres niveles de
 * engaño. Nivel 1 es evidente, nivel 2 imita avisos reales y nivel 3 mezcla
 * mensajes legítimos con trampas casi idénticas.
 */
object TarjetasCiber {

    val todas: List<TarjetaCiber> = listOf(
        // ----- Nivel 1: pistas evidentes -----
        TarjetaCiber(
            id = "t1_01",
            emisor = "Premios Falsos",
            mensaje = "¡Ganaste 1 millón de dólares! Haz clic aquí para reclamarlo.",
            esSegura = false,
            explicacion = "Nadie regala un millón por internet. Los premios reales no te piden dar clic ni tus datos.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_02",
            emisor = "Mamá",
            mensaje = "Hola Zazi, soy tu mamá. ¿Ya comiste?",
            esSegura = true,
            explicacion = "Es un mensaje normal de tu familia: no pide contraseñas, dinero ni que abras enlaces raros.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_03",
            emisor = "Zazoles Gratis",
            mensaje = "¡5000 Zazoles gratis! Solo escribe la contraseña de tu mamá.",
            esSegura = false,
            explicacion = "Las contraseñas son secretas. Ningún juego de Zazili te las pide por mensaje.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_04",
            emisor = "Maestra Lupita",
            mensaje = "Mañana llevamos cartulina al salón. No es tarea, es proyecto.",
            esSegura = true,
            explicacion = "Es un aviso escolar tranquilo, sin enlaces ni datos personales.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_05",
            emisor = "Príncipe de Nigeria",
            mensaje = "Soy un príncipe y necesito tu ayuda para mover mi dinero.",
            esSegura = false,
            explicacion = "Es la estafa clásica del príncipe. Nunca respondas mensajes así.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_06",
            emisor = "Papá",
            mensaje = "Te dejé tu lunch en la mochila. ¡Que tengas buen día!",
            esSegura = true,
            explicacion = "Mensaje familiar y cotidiano. No pide nada raro.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_07",
            emisor = "Sorteo de Tablets",
            mensaje = "¡Felicidades! Ganaste una tablet. Paga $99 de envío.",
            esSegura = false,
            explicacion = "Un premio real nunca te cobra por adelantado. Eso es un truco para sacarte dinero.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_08",
            emisor = "Abuela Rosa",
            mensaje = "¿Me enseñas tu juego nuevo el domingo? Te hago quesadillas.",
            esSegura = true,
            explicacion = "Es una invitación cariñosa de tu abuela, sin enlaces ni contraseñas.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_09",
            emisor = "Actualización Urgente",
            mensaje = "Tu teléfono explotará en 5 minutos. Da clic para arreglarlo.",
            esSegura = false,
            explicacion = "Las amenazas exageradas buscan asustarte para que hagas clic. Tu teléfono no explota por un mensaje.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_10",
            emisor = "Zazi",
            mensaje = "¡Hola! Soy Zazi y hoy vamos a aprender con calma.",
            esSegura = true,
            explicacion = "Es un saludo de Zazi sin pedir datos ni enlaces.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_11",
            emisor = "Banco Falso",
            mensaje = "Tu tarjeta fue bloqueada. Pega tu NIP aquí.",
            esSegura = false,
            explicacion = "Tu banco jamás te pide el NIP por mensaje. Compartirlo es muy peligroso.",
            nivel = 1
        ),
        TarjetaCiber(
            id = "t1_12",
            emisor = "Biblioteca",
            mensaje = "Tu libro de préstamo vence el viernes. Puedes renovarlo en el mostrador.",
            esSegura = true,
            explicacion = "Es un recordatorio normal que te invita a ir en persona, sin enlaces.",
            nivel = 1
        ),

        // ----- Nivel 2: engaños sutiles -----
        TarjetaCiber(
            id = "t2_01",
            emisor = "Netflix",
            mensaje = "Tu cuenta de Netflix va a ser borrada, haz clic aquí.",
            esSegura = false,
            explicacion = "El aviso imita a Netflix, pero metió prisa y un enlace. Entra a la app oficial, nunca desde el mensaje.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_02",
            emisor = "Paquetería",
            mensaje = "Tu paquete llegará hoy entre 3 y 5 pm. No necesitas hacer nada.",
            esSegura = true,
            explicacion = "Avisa la hora y no pide clics ni pagos. Es información normal de entrega.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_03",
            emisor = "Servicio Postal",
            mensaje = "Tienes un paquete retenido. Paga $25 aquí para liberarlo.",
            esSegura = false,
            explicacion = "Las paqueterías reales no cobran por un enlace improvisado. Verifica siempre en su app oficial.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_04",
            emisor = "Escuela",
            mensaje = "Se suspendieron las clases por lluvia. Mañana volvemos.",
            esSegura = true,
            explicacion = "Es un aviso sencillo, sin enlaces ni solicitudes de dinero.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_05",
            emisor = "WhatsApp Meta",
            mensaje = "Verificaste un nuevo dispositivo. Si no fuiste tú, da clic para bloquearlo.",
            esSegura = false,
            explicacion = "El miedo a perder la cuenta es el gancho. Abre WhatsApp directamente y revisa desde ahí.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_06",
            emisor = "Tía Marce",
            mensaje = "Te mando la foto del pastel que hicimos ayer. ¿Te gusta?",
            esSegura = true,
            explicacion = "Es una conversación familiar normal, sin enlaces ni contraseñas.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_07",
            emisor = "iCloud",
            mensaje = "Tu almacenamiento está lleno. Inicia sesión aquí para liberar espacio.",
            esSegura = false,
            explicacion = "Pide tu sesión desde un enlace. Entra a los ajustes oficiales del dispositivo para revisarlo.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_08",
            emisor = "Papelería del barrio",
            mensaje = "Comprobante de tu compra: $45. No compartas este mensaje.",
            esSegura = true,
            explicacion = "Solo informa el monto y te pide no compartirlo. No pide datos.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_09",
            emisor = "DHL Express",
            mensaje = "Tu envío está detenido. Actualiza tus datos bancarios ahora.",
            esSegura = false,
            explicacion = "Ninguna paquetería necesita tus datos bancarios por mensaje. Es una estafa.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_10",
            emisor = "Profe de Educación Física",
            mensaje = "Recuerda traer tenis limpios para la clase del jueves.",
            esSegura = true,
            explicacion = "Es un recordatorio escolar simple, sin enlaces ni datos personales.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_11",
            emisor = "Google",
            mensaje = "Alguien intentó entrar a tu cuenta. Confirma tu contraseña en este enlace.",
            esSegura = false,
            explicacion = "Las empresas serias no piden contraseñas por enlace. Entra a la cuenta por la app oficial.",
            nivel = 2
        ),
        TarjetaCiber(
            id = "t2_12",
            emisor = "Prima Sofía",
            mensaje = "¿Vas a la fiesta del sábado? Yo llevo las gelatinas.",
            esSegura = true,
            explicacion = "Es una invitación familiar cotidiana. No pide clics ni información privada.",
            nivel = 2
        ),

        // ----- Nivel 3: trampas casi idénticas -----
        TarjetaCiber(
            id = "t3_01",
            emisor = "Sistema Zazili",
            mensaje = "Actualización requerida del sistema Zazili. Abre la app y actualiza desde la tienda oficial.",
            esSegura = true,
            explicacion = "Te manda a la tienda oficial, no a un enlace externo. Actualizar desde la tienda es seguro.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_02",
            emisor = "Mods Zazi",
            mensaje = "Descarga este mod para tener Zazoles infinitos. Solo desactiva el antivirus.",
            esSegura = false,
            explicacion = "Pedirte que apagues el antivirus es la gran señal de peligro. No existen Zazoles infinitos.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_03",
            emisor = "Mamá",
            mensaje = "Descargué una app nueva para hacer tareas. Te la quiero enseñar en tu tablet.",
            esSegura = true,
            explicacion = "No hay enlace ni prisa. Te invita a verla juntos en persona.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_04",
            emisor = "Soporte Zazili",
            mensaje = "Para recuperar tu cuenta manda tu contraseña por este chat. Somos del equipo.",
            esSegura = false,
            explicacion = "El soporte verdadero nunca pide tu contraseña. Ese mensaje es un fraude.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_05",
            emisor = "Maestra de Computación",
            mensaje = "Las contraseñas nunca se comparten, ni con la maestra. Hoy platicamos de eso.",
            esSegura = true,
            explicacion = "Refuerza una regla de seguridad real. No pide nada.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_06",
            emisor = "Minecraft Free",
            mensaje = "Servidor oficial de Minecraft gratis. Escribe tu correo y contraseña aquí.",
            esSegura = false,
            explicacion = "Pedir tu contraseña para un servidor gratuito es una trampa. Usa solo sitios oficiales.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_07",
            emisor = "Aviso de la tienda",
            mensaje = "Tienes una actualización pendiente. Entra a la tienda de tu dispositivo para instalarla.",
            esSegura = true,
            explicacion = "No hay enlace sospechoso: te pide entrar a la tienda oficial. Eso es seguro.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_08",
            emisor = "Beca Escolar",
            mensaje = "Ganaste una beca. Deposita $300 para apartar tu lugar.",
            esSegura = false,
            explicacion = "Una beca real no te cobra por adelantado. Es un cobro disfrazado de premio.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_09",
            emisor = "Centro de Ayuda Zazili",
            mensaje = "Si algo se siente raro, dile a un adulto de confianza. Nunca compartas tus datos.",
            esSegura = true,
            explicacion = "Te da un consejo de seguridad y no pide nada. Es un mensaje seguro.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_10",
            emisor = "Amigo Desconocido",
            mensaje = "Hola, soy tu compañero nuevo. Pásame el código de tu casa.",
            esSegura = false,
            explicacion = "Nunca compartas datos de tu casa con desconocidos, aunque digan que son de la escuela.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_11",
            emisor = "Veterinaria",
            mensaje = "La cita de Luna es el lunes a las 10 am. Trae su cartilla.",
            esSegura = true,
            explicacion = "Es un recordatorio de cita con datos concretos y sin enlaces.",
            nivel = 3
        ),
        TarjetaCiber(
            id = "t3_12",
            emisor = "Actualización de Seguridad",
            mensaje = "Actualiza tu sistema desde este enlace externo y escribe tu contraseña.",
            esSegura = false,
            explicacion = "Las actualizaciones se hacen desde ajustes o la tienda oficial, nunca desde un enlace que pide contraseña.",
            nivel = 3
        )
    )
}
