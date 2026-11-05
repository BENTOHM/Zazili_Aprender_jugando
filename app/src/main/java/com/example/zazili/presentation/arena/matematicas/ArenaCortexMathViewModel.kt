package com.example.zazili.presentation.arena.matematicas

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.ZaziliApplication
import com.example.zazili.domain.arena.FaseAprendizaje
import com.example.zazili.domain.arena.NivelAndamiaje
import com.example.zazili.domain.arena.ParametrosDificultad
import com.example.zazili.domain.arena.ZaziDifficultyEngine
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado compartido de los cuatro minijuegos de Matemáticas de la Arena Cortex.
 *
 * El [ArenaCortexMathViewModel] es el único dueño de la dificultad, del marcador
 * y del reto que se está pintando. Las pantallas no sortean nada, no deciden
 * nada y no guardan progreso propio: solo dibujan [SesionMath] y avisan lo que
 * hizo el niño (elegir una opción, soltar una pieza, completar una zona).
 *
 * Reglas de diseño:
 *  - **Sin relojes.** Ningún contador baja ni expulsa al niño. El DDA mide el
 *    tiempo de reacción para saber cuándo escalar, jamás para castigar.
 *  - **Sin derrota.** Una sesión siempre termina con al menos una estrella; si
 *    el resultado fue bajo, los textos invitan a otra ronda.
 *  - **Andamiaje invisible.** Los fallos suben [NivelAndamiaje] a través de
 *    [ZaziDifficultyEngine]: menos opciones, pista fija, texto biónico y
 *    resaltados. La pantalla lo único que hace es pintar lo que diga la sesión.
 *  - **Línea base del diagnóstico.** El primer nivel de cada juego sale de
 *    `PerfilUsuario.nivelMatematicas`; después manda el avance real del DDA.
 */

/** Rondas de una sesión de Matemáticas: cortita, para no cansar. */
const val RONDAS_SESION_MATE = 6

const val NIVEL_MINIMO_MATE = 1
const val NIVEL_MAXIMO_MATE = 5

private const val OPCIONES_MINIMAS = 2
private const val OPCIONES_MAXIMAS = 4

private const val PUNTOS_BASE = 10
private const val PUNTOS_POR_NIVEL = 2
private const val PUNTOS_POR_RACHA = 2

private const val MS_RETROALIMENTACION = 1_150L
private const val MS_AVISO = 1_700L
private const val ESPERA_MAXIMA_BASE_MS = 1_200L

// ---------------------------------------------------------------------------
//  CATÁLOGO
// ---------------------------------------------------------------------------

/** Los cuatro minijuegos de Matemáticas; el id coincide con `Minijuego`. */
@Immutable
enum class JuegoMatematico(
    val id: String,
    val titulo: String,
    val emoji: String
) {
    CALCULO_MENTAL(
        id = "calculo_mental",
        titulo = "Cálculo Mental",
        emoji = "🧮"
    ),
    FRACCIONES_FESTIVAS(
        id = "fracciones_festivas",
        titulo = "Fracciones Festivas",
        emoji = "🍩"
    ),
    GEOMETRIA_ANCESTRAL(
        id = "geometria_ancestral",
        titulo = "Geometría Ancestral",
        emoji = "🔺"
    ),
    ESTADISTICA_ALEBRIJE(
        id = "estadistica_alebrije",
        titulo = "Estadística Alebrije",
        emoji = "📊"
    );

    companion object {
        /**
         * Traduce el id de una ficha de la Arena a su juego de Matemáticas.
         * Devuelve null para las otras tres materias, que sí se abren desde
         * la navegación padre.
         */
        fun desdeId(id: String): JuegoMatematico? =
            entries.firstOrNull { it.id == id }
    }
}

/**
 * Pantalla visible dentro de la pestaña Arena Cortex.
 *
 * La navegación es estado, no una pila de rutas: el menú pide un juego y el
 * router pinta exactamente ese composable. Así, volver atrás o rotar la
 * pantalla nunca abre un minijuego distinto al que se tocó.
 */
@Immutable
sealed interface DestinoArena {

    /** Menú con los 16 minijuegos. */
    data object Hub : DestinoArena

    /** Uno de los cuatro juegos de Matemáticas, ya dentro de la Arena. */
    data class Matematicas(val juego: JuegoMatematico) : DestinoArena
}

/** Figuras que se pueden dibujar y arrastrar en Geometría Ancestral. */
@Immutable
enum class FormaGeometrica(val etiqueta: String, val lados: Int) {
    TRIANGULO("Triángulo", 3),
    TRAPECIO("Trapecio", 4),
    HEXAGONO("Hexágono", 6),
    ROMBO("Rombo", 4),
    CIRCULO("Círculo", 0)
}

// ---------------------------------------------------------------------------
//  RETOS
// ---------------------------------------------------------------------------

/**
 * Cuerpo de un reto matemático. No sabe nada de Compose: la pantalla decide
 * cómo dibujarlo y el ViewModel decide si la respuesta es correcta.
 */
@Immutable
sealed interface RetoMath {

    val consigna: String
    val pista: String

    /** Cálculo Mental: una ecuación y de 2 a 4 botones flotantes. */
    data class Calculo(
        override val consigna: String,
        override val pista: String,
        val opciones: List<String>,
        val indiceCorrecto: Int
    ) : RetoMath

    /** Fracciones Festivas: piezas que se arrastran hasta completar el postre. */
    data class Fraccion(
        val titulo: String,
        val nombreConArticulo: String,
        val emoji: String,
        val denominador: Int,
        val porcionesLlenas: Int,
        val faltan: Int,
        val piezas: List<PiezaFraccion>,
        override val consigna: String,
        override val pista: String
    ) : RetoMath

    /** Geometría Ancestral: silueta con zonas y piezas para reconstruirla. */
    data class Geometria(
        val titulo: String,
        val nombreConArticulo: String,
        val emoji: String,
        val zonas: List<ZonaGeometria>,
        val piezas: List<PiezaGeometria>,
        override val consigna: String,
        override val pista: String
    ) : RetoMath

    /** Estadística Alebrije: gráfica de barras y una pregunta de interpretación. */
    data class Estadistica(
        val titulo: String,
        val pregunta: String,
        val barras: List<BarraDato>,
        val opciones: List<String>,
        val indiceCorrecto: Int,
        val muestraValores: Boolean,
        override val consigna: String,
        override val pista: String
    ) : RetoMath
}

/** Un pedazo de postre. [denominador] es el tamaño real de la rebanada. */
@Immutable
data class PiezaFraccion(
    val denominador: Int,
    val tono: Int
)

/**
 * Zona de la silueta en coordenadas fraccionarias (0..1) del lienzo. Al
 * dibujarse con el tamaño real del Canvas nunca se sale de la pantalla.
 */
@Immutable
data class ZonaGeometria(
    val forma: FormaGeometrica,
    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float
)

/** Pieza de la bandeja. Varias piezas iguales pueden ir a cualquier zona igual. */
@Immutable
data class PiezaGeometria(
    val forma: FormaGeometrica,
    val tono: Int
)

/** Una barra de la gráfica de los alebrijes. */
@Immutable
data class BarraDato(
    val etiqueta: String,
    val emoji: String,
    val valor: Int,
    val tono: Int
)

/** Retroalimentación inmediata de una ronda. Nunca regaña. */
@Immutable
data class ResultadoMath(
    val acierto: Boolean,
    val titulo: String,
    val detalle: String
)

// ---------------------------------------------------------------------------
//  SESIÓN
// ---------------------------------------------------------------------------

/**
 * Fotografía de un minijuego de Matemáticas, lista para pintar.
 *
 * [piezasColocadas] guarda los índices de piezas ya usadas (fracciones y
 * geometría) y [zonasLlenas] los índices de zonas resueltas (geometría).
 * Ambos son inmutables para que Compose pueda saltarse recomposiciones.
 */
@Immutable
data class SesionMath(
    val juego: JuegoMatematico,
    val reto: RetoMath? = null,
    val nivel: Int = NIVEL_MINIMO_MATE,
    val andamiaje: NivelAndamiaje = NivelAndamiaje.NINGUNO,
    val fase: FaseAprendizaje = FaseAprendizaje.CALIBRACION,
    val mensaje: String = "Vamos conociéndonos. Responde a tu ritmo.",
    val opciones: Int = 4,
    val ronda: Int = 0,
    val totalRondas: Int = RONDAS_SESION_MATE,
    val aciertos: Int = 0,
    val errores: Int = 0,
    val puntos: Int = 0,
    val racha: Int = 0,
    val piezasColocadas: Set<Int> = emptySet(),
    val zonasLlenas: Set<Int> = emptySet(),
    val aviso: String? = null,
    val resultado: ResultadoMath? = null,
    val terminado: Boolean = false,
    val estrellas: Int = 0,
    val iniciadoEnMs: Long = 0L
) {
    val textoBionico: Boolean get() = andamiaje.textoBionico

    val resaltar: Boolean get() = andamiaje.resaltados

    val hayApoyo: Boolean get() = andamiaje.hayApoyo

    val rondasContestadas: Int
        get() = (ronda + if (terminado || resultado != null) 1 else 0)
            .coerceAtMost(totalRondas)

    val progreso: Float
        get() = if (totalRondas <= 0) 0f else rondasContestadas.toFloat() / totalRondas
}

/** Sesión ganada: sesenta por ciento de aciertos o más. Nunca hay derrota. */
val SesionMath.esVictoria: Boolean
    get() = totalRondas > 0 && aciertos * 5 >= totalRondas * 3

// ---------------------------------------------------------------------------
//  VIEWMODEL
// ---------------------------------------------------------------------------

/**
 * Motor central de la Arena Matemática. Sobrevive a la rotación, comparte el
 * mismo [ZaziDifficultyEngine] entre los cuatro juegos y entrega a cada
 * pantalla un único [StateFlow] con su sesión.
 */
class ArenaCortexMathViewModel(application: Application) :
    AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container

    private val azar = Random(System.nanoTime())

    /** Línea base del diagnóstico, en cuanto DataStore la entrega. */
    private val _lineaBase = MutableStateFlow(0)
    val lineaBase: StateFlow<Int> = _lineaBase.asStateFlow()

    private val nivelBase = MutableStateFlow(NIVEL_MINIMO_MATE)

    private val baseLista = CompletableDeferred<Unit>()

    /**
     * DDA real de la Arena: persiste nivel y andamiaje por juego. El nivel de
     * arranque de cada juego es la línea base del diagnóstico.
     */
    private val motor = ZaziDifficultyEngine(application) {
        nivelBase.value.coerceIn(NIVEL_MINIMO_MATE, NIVEL_MAXIMO_MATE)
    }

    private val _sesiones = MutableStateFlow(
        JuegoMatematico.entries.associateWith { SesionMath(juego = it) }
    )

    /** Todas las sesiones; útil para un panel o para depurar. */
    val sesiones: StateFlow<Map<JuegoMatematico, SesionMath>> = _sesiones.asStateFlow()

    val calculoMental: StateFlow<SesionMath> = flujo(JuegoMatematico.CALCULO_MENTAL)
    val fraccionesFestivas: StateFlow<SesionMath> = flujo(JuegoMatematico.FRACCIONES_FESTIVAS)
    val geometriaAncestral: StateFlow<SesionMath> = flujo(JuegoMatematico.GEOMETRIA_ANCESTRAL)
    val estadisticaAlebrije: StateFlow<SesionMath> = flujo(JuegoMatematico.ESTADISTICA_ALEBRIJE)

    /**
     * Destino actual de la pestaña Arena Cortex. Vive aquí y no en el menú
     * para que sobreviva a la rotación y para que el router tenga una sola
     * fuente de verdad sobre qué minijuego está abierto.
     */
    private val _destino = MutableStateFlow<DestinoArena>(DestinoArena.Hub)
    val destino: StateFlow<DestinoArena> = _destino.asStateFlow()

    private val marcas: MutableMap<JuegoMatematico, Int> =
        JuegoMatematico.entries.associateWith { 0 }.toMutableMap()

    init {
        viewModelScope.launch {
            contenedor.userManager.perfil
                .filterNotNull()
                .collect { perfil ->
                    _lineaBase.value = perfil.nivelMatematicas
                    val base = perfil.nivelMatematicas
                        .coerceIn(NIVEL_MINIMO_MATE, NIVEL_MAXIMO_MATE)
                    nivelBase.value = base
                    if (!baseLista.isCompleted) baseLista.complete(Unit)
                }
        }
        // Si el perfil tarda o viene corrupto, la Arena abre igual con nivel 1.
        viewModelScope.launch {
            delay(ESPERA_MAXIMA_BASE_MS)
            if (!baseLista.isCompleted) baseLista.complete(Unit)
        }
    }

    /** Estado propio de un juego; cada minijuego tiene su sesión independiente. */
    fun flujo(juego: JuegoMatematico): StateFlow<SesionMath> =
        _sesiones
            .map { mapa -> mapa[juego] ?: SesionMath(juego = juego) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SesionMath(juego = juego)
            )

    // ------------------------- navegación -------------------------

    /**
     * Abre EXACTAMENTE el juego que se tocó en el menú.
     *
     * Si la sesión anterior quedó a medias, la retoma en la misma ronda; si ya
     * había terminado, arranca una ronda nueva sin tocar el nivel del DDA.
     */
    fun abrirJuego(juego: JuegoMatematico) {
        _destino.value = DestinoArena.Matematicas(juego)
        if (_sesiones.value[juego]?.terminado == true) {
            reiniciar(juego)
        } else {
            preparar(juego)
        }
    }

    /** Regresa al menú de la Arena sin tocar niveles, rachas ni puntos. */
    fun cerrarJuego() {
        _destino.value = DestinoArena.Hub
    }

    // ------------------------- ciclo de la sesión -------------------------

    /**
     * Prepara el juego al entrar a su pantalla. Si la sesión sigue viva (por
     * ejemplo tras una rotación) la conserva; si ya terminó, deja ver el
     * resultado y espera a que el niño pida otra ronda.
     */
    fun preparar(juego: JuegoMatematico) {
        if (_sesiones.value[juego]?.reto != null) return
        viewModelScope.launch {
            baseLista.await()
            if (_sesiones.value[juego]?.reto == null) iniciarSesion(juego)
        }
    }

    /** "Otra ronda": sesión nueva sin tocar el nivel ganado en el DDA. */
    fun reiniciar(juego: JuegoMatematico) {
        viewModelScope.launch {
            baseLista.await()
            iniciarSesion(juego)
        }
    }

    private fun iniciarSesion(juego: JuegoMatematico) {
        val dda = motor.estado(juego.id)
        siguienteMarca(juego)
        val reto = generarReto(juego, dda.parametros)
        publicar(
            juego,
            SesionMath(
                juego = juego,
                reto = reto,
                nivel = dda.nivel,
                andamiaje = dda.parametros.andamiaje,
                fase = dda.fase,
                mensaje = dda.mensajeNino,
                opciones = dda.parametros.opciones.coerceIn(OPCIONES_MINIMAS, OPCIONES_MAXIMAS),
                iniciadoEnMs = ahora()
            )
        )
    }

    /** Respuesta de un reto de opción múltiple (cálculo y estadística). */
    fun responder(juego: JuegoMatematico, indice: Int) {
        val sesion = _sesiones.value[juego] ?: return
        if (sesion.terminado || sesion.resultado != null) return
        val correcto = when (val reto = sesion.reto) {
            is RetoMath.Calculo -> reto.indiceCorrecto == indice
            is RetoMath.Estadistica -> reto.indiceCorrecto == indice
            else -> return
        }
        cerrarRonda(juego, correcto, ahora() - sesion.iniciadoEnMs)
    }

    /** Suelta una pieza de fracción sobre el postre. */
    fun colocarPiezaFraccion(juego: JuegoMatematico, indicePieza: Int) {
        val sesion = _sesiones.value[juego] ?: return
        if (sesion.terminado || sesion.resultado != null) return
        val reto = sesion.reto as? RetoMath.Fraccion ?: return
        if (indicePieza in sesion.piezasColocadas) return
        val pieza = reto.piezas.getOrNull(indicePieza) ?: return

        if (pieza.denominador != reto.denominador) {
            anotarDesliz(
                juego,
                "Ese pedazo mide 1/${pieza.denominador}. Necesitas pedazos de 1/${reto.denominador}."
            )
            return
        }

        val colocadas = sesion.piezasColocadas + indicePieza
        publicar(juego, sesion.copy(piezasColocadas = colocadas, aviso = null))
        if (colocadas.size >= reto.faltan) {
            cerrarRonda(
                juego = juego,
                correcto = true,
                reaccionMs = ahora() - sesion.iniciadoEnMs
            )
        }
    }

    /** Suelta una pieza de geometría dentro de una zona de la silueta. */
    fun colocarEnZona(
        juego: JuegoMatematico,
        indicePieza: Int,
        indiceZona: Int
    ) {
        val sesion = _sesiones.value[juego] ?: return
        if (sesion.terminado || sesion.resultado != null) return
        val reto = sesion.reto as? RetoMath.Geometria ?: return
        if (indicePieza in sesion.piezasColocadas) return
        if (indiceZona in sesion.zonasLlenas) return
        val pieza = reto.piezas.getOrNull(indicePieza) ?: return
        val zona = reto.zonas.getOrNull(indiceZona) ?: return

        if (pieza.forma != zona.forma) {
            anotarDesliz(
                juego,
                "Esa figura no encaja en el hueco. Cuenta sus lados con calma."
            )
            return
        }

        val llenas = sesion.zonasLlenas + indiceZona
        val usadas = sesion.piezasColocadas + indicePieza
        publicar(
            juego,
            sesion.copy(
                zonasLlenas = llenas,
                piezasColocadas = usadas,
                aviso = null
            )
        )
        if (llenas.size >= reto.zonas.size) {
            cerrarRonda(
                juego = juego,
                correcto = true,
                reaccionMs = ahora() - sesion.iniciadoEnMs
            )
        }
    }

    // ------------------------- cierre de ronda -------------------------

    private fun cerrarRonda(
        juego: JuegoMatematico,
        correcto: Boolean,
        reaccionMs: Long
    ) {
        val actual = _sesiones.value[juego] ?: return
        if (actual.resultado != null || actual.terminado) return

        val dda = if (correcto) {
            motor.registrarAcierto(juego.id, reaccionMs)
        } else {
            motor.registrarFallo(juego.id, reaccionMs)
        }
        val racha = if (correcto) actual.racha + 1 else 0
        val puntos = if (correcto) {
            actual.puntos + PUNTOS_BASE + dda.nivel * PUNTOS_POR_NIVEL +
                racha * PUNTOS_POR_RACHA
        } else {
            actual.puntos
        }
        val marca = siguienteMarca(juego)

        val actualizado = actual.copy(
            nivel = dda.nivel,
            andamiaje = dda.parametros.andamiaje,
            fase = dda.fase,
            mensaje = dda.mensajeNino,
            opciones = dda.parametros.opciones.coerceIn(OPCIONES_MINIMAS, OPCIONES_MAXIMAS),
            aciertos = actual.aciertos + if (correcto) 1 else 0,
            errores = actual.errores + if (correcto) 0 else 1,
            puntos = puntos,
            racha = racha,
            aviso = null,
            resultado = ResultadoMath(
                acierto = correcto,
                titulo = if (correcto) "¡Muy bien!" else "Casi, casi",
                detalle = detalleDe(actual.reto, correcto)
            )
        )
        publicar(juego, actualizado)

        viewModelScope.launch {
            delay(MS_RETROALIMENTACION)
            if (marcas[juego] == marca) avanzar(juego)
        }
    }

    private fun avanzar(juego: JuegoMatematico) {
        val sesion = _sesiones.value[juego] ?: return
        if (sesion.resultado == null) return

        val siguienteRonda = sesion.ronda + 1
        if (siguienteRonda >= sesion.totalRondas) {
            siguienteMarca(juego)
            publicar(
                juego,
                sesion.copy(
                    resultado = null,
                    terminado = true,
                    estrellas = estrellasPara(sesion.aciertos, sesion.totalRondas)
                )
            )
            return
        }

        val dda = motor.estado(juego.id)
        siguienteMarca(juego)
        publicar(
            juego,
            sesion.copy(
                reto = generarReto(juego, dda.parametros),
                ronda = siguienteRonda,
                nivel = dda.nivel,
                andamiaje = dda.parametros.andamiaje,
                fase = dda.fase,
                mensaje = dda.mensajeNino,
                opciones = dda.parametros.opciones.coerceIn(
                    OPCIONES_MINIMAS,
                    OPCIONES_MAXIMAS
                ),
                piezasColocadas = emptySet(),
                zonasLlenas = emptySet(),
                aviso = null,
                resultado = null,
                iniciadoEnMs = ahora()
            )
        )
    }

    /**
     * Desliz en un juego de arrastrar y soltar: no cierra la ronda ni quita
     * nada. Solo le avisa al DDA que hace falta un escalón más chico y muestra
     * un consejo amable que se borra solo.
     */
    private fun anotarDesliz(juego: JuegoMatematico, detalle: String) {
        val sesion = _sesiones.value[juego] ?: return
        if (sesion.terminado || sesion.resultado != null) return

        val dda = motor.registrarFallo(juego.id, ahora() - sesion.iniciadoEnMs)
        publicar(
            juego,
            sesion.copy(
                nivel = dda.nivel,
                andamiaje = dda.parametros.andamiaje,
                fase = dda.fase,
                mensaje = dda.mensajeNino,
                opciones = dda.parametros.opciones.coerceIn(
                    OPCIONES_MINIMAS,
                    OPCIONES_MAXIMAS
                ),
                errores = sesion.errores + 1,
                racha = 0,
                aviso = detalle
            )
        )
        limpiarAviso(juego, detalle)
    }

    private fun limpiarAviso(juego: JuegoMatematico, texto: String) {
        viewModelScope.launch {
            delay(MS_AVISO)
            val actual = _sesiones.value[juego] ?: return@launch
            if (actual.aviso == texto && actual.resultado == null) {
                publicar(juego, actual.copy(aviso = null))
            }
        }
    }

    // ------------------------- utilería -------------------------

    private fun publicar(juego: JuegoMatematico, sesion: SesionMath) {
        _sesiones.update { actuales -> actuales + (juego to sesion) }
    }

    private fun siguienteMarca(juego: JuegoMatematico): Int {
        val nueva = (marcas[juego] ?: 0) + 1
        marcas[juego] = nueva
        return nueva
    }

    private fun ahora(): Long = System.currentTimeMillis()

    private fun estrellasPara(aciertos: Int, total: Int): Int = when {
        total <= 0 -> 1
        aciertos >= total -> 3
        aciertos * 5 >= total * 3 -> 2
        else -> 1
    }

    private fun detalleDe(reto: RetoMath?, correcto: Boolean): String = when (reto) {
        is RetoMath.Calculo -> {
            val correcta = reto.opciones.getOrElse(reto.indiceCorrecto) { "" }
            if (correcto) {
                "Sí: ${reto.consigna.replace("?", correcta)}"
            } else {
                "La respuesta era $correcta. ${reto.consigna.replace("?", correcta)}"
            }
        }

        is RetoMath.Estadistica -> {
            val correcta = reto.opciones.getOrElse(reto.indiceCorrecto) { "" }
            if (correcto) "¡Exacto! Es $correcta." else "Casi. La respuesta era $correcta."
        }

        is RetoMath.Fraccion -> if (correcto) {
            "¡Completaste ${reto.nombreConArticulo}!"
        } else {
            "Todavía faltaban pedazos, pero ya casi."
        }

        is RetoMath.Geometria -> if (correcto) {
            "¡Reconstruiste ${reto.nombreConArticulo}!"
        } else {
            "Faltaban piezas; la silueta sigue esperando."
        }

        null -> if (correcto) "¡Muy bien!" else "Casi, casi."
    }

    // -----------------------------------------------------------------------
    //  GENERADORES PROCEDURALES (uno por minijuego)
    // -----------------------------------------------------------------------

    private fun generarReto(
        juego: JuegoMatematico,
        parametros: ParametrosDificultad
    ): RetoMath = when (juego) {
        JuegoMatematico.CALCULO_MENTAL -> generarCalculo(parametros)
        JuegoMatematico.FRACCIONES_FESTIVAS -> generarFraccion(parametros)
        JuegoMatematico.GEOMETRIA_ANCESTRAL -> generarGeometria(parametros)
        JuegoMatematico.ESTADISTICA_ALEBRIJE -> generarEstadistica(parametros)
    }

    // ------------------------- cálculo mental -------------------------

    private fun generarCalculo(parametros: ParametrosDificultad): RetoMath.Calculo {
        val nivel = parametros.nivel.coerceIn(NIVEL_MINIMO_MATE, NIVEL_MAXIMO_MATE)
        val operacion = when (nivel) {
            1 -> Operacion.SUMA
            2 -> if (azar.nextBoolean()) Operacion.SUMA else Operacion.RESTA
            3 -> if (azar.nextBoolean()) Operacion.MULTIPLICACION else Operacion.RESTA
            4 -> if (azar.nextBoolean()) Operacion.MULTIPLICACION else Operacion.SUMA
            else -> when (azar.nextInt(3)) {
                0 -> Operacion.SUMA
                1 -> Operacion.RESTA
                else -> Operacion.MULTIPLICACION
            }
        }

        val a: Int
        val b: Int
        val respuesta: Int
        when (operacion) {
            Operacion.SUMA -> {
                val tope = when (nivel) {
                    1 -> 5
                    2 -> 30
                    3 -> 60
                    4 -> 120
                    else -> 400
                }
                a = azar.nextInt(1, tope)
                b = azar.nextInt(1, tope)
                respuesta = a + b
            }

            Operacion.RESTA -> {
                val tope = when (nivel) {
                    2 -> 40
                    3 -> 80
                    4 -> 150
                    else -> 600
                }
                a = azar.nextInt(tope / 2, tope)
                b = azar.nextInt(1, a)
                respuesta = a - b
            }

            Operacion.MULTIPLICACION -> {
                val tope = when (nivel) {
                    3 -> 6
                    4 -> 9
                    else -> 12
                }
                a = azar.nextInt(2, tope + 1)
                b = azar.nextInt(2, tope + 1)
                respuesta = a * b
            }
        }

        val opciones = opcionesNumericas(respuesta, parametros.opciones, operacion)
        val ecuacion = "$a ${operacion.simbolo} $b = ?"
        return RetoMath.Calculo(
            consigna = ecuacion,
            pista = operacion.pista(a, b, nivel),
            opciones = opciones,
            indiceCorrecto = opciones.indexOf(respuesta.toString())
        )
    }

    private fun opcionesNumericas(
        respuesta: Int,
        cantidad: Int,
        operacion: Operacion
    ): List<String> {
        val total = cantidad.coerceIn(OPCIONES_MINIMAS, OPCIONES_MAXIMAS)
        val valores = linkedSetOf(respuesta)
        val candidatos = mutableListOf(
            respuesta + 1,
            respuesta - 1,
            respuesta + 2,
            respuesta - 2,
            respuesta + 10,
            respuesta - 10,
            if (operacion == Operacion.MULTIPLICACION) respuesta / 2 else respuesta + 3
        )
        candidatos.shuffle(azar)
        for (candidato in candidatos) {
            if (valores.size >= total) break
            if (candidato >= 0 && candidato != respuesta) valores.add(candidato)
        }
        var relleno = respuesta + 3
        while (valores.size < total) {
            if (relleno != respuesta) valores.add(relleno)
            relleno += 1
        }
        return valores.map { it.toString() }.shuffled(azar)
    }

    // ------------------------- fracciones festivas -------------------------

    private fun generarFraccion(parametros: ParametrosDificultad): RetoMath.Fraccion {
        val nivel = parametros.nivel.coerceIn(NIVEL_MINIMO_MATE, NIVEL_MAXIMO_MATE)
        val denominador = when (nivel) {
            1 -> 2
            2 -> 4
            3 -> if (azar.nextBoolean()) 4 else 6
            4 -> 8
            else -> if (azar.nextBoolean()) 8 else 6
        }
        val faltan = azar.nextInt(1, (denominador / 2) + 1)
        val llenas = (denominador - faltan).coerceAtLeast(1)
        val postre = Postre.entries[azar.nextInt(Postre.entries.size)]

        val piezas = mutableListOf<PiezaFraccion>()
        repeat(faltan) {
            piezas.add(PiezaFraccion(denominador = denominador, tono = azar.nextInt(3)))
        }
        if (nivel >= 2) {
            val otraMedida = if (denominador >= 4 && azar.nextBoolean()) {
                denominador / 2
            } else {
                (denominador * 2).coerceAtMost(8)
            }
            val distractores = if (nivel >= 4) 2 else 1
            repeat(distractores) {
                piezas.add(PiezaFraccion(denominador = otraMedida, tono = 2))
            }
        }

        return RetoMath.Fraccion(
            titulo = postre.titulo,
            nombreConArticulo = postre.nombreConArticulo,
            emoji = postre.emoji,
            denominador = denominador,
            porcionesLlenas = llenas,
            faltan = faltan,
            piezas = piezas.shuffled(azar),
            consigna = "Completa ${postre.nombreConArticulo}: faltan $faltan " +
                if (faltan == 1) "pedazo de 1/$denominador." else "pedazos de 1/$denominador.",
            pista = "${postre.titulo} está partida en $denominador partes iguales. " +
                "Suelta los pedazos que midan 1/$denominador."
        )
    }

    // ------------------------- geometría ancestral -------------------------

    private fun generarGeometria(parametros: ParametrosDificultad): RetoMath.Geometria {
        val nivel = parametros.nivel.coerceIn(NIVEL_MINIMO_MATE, NIVEL_MAXIMO_MATE)
        val ruina = when {
            nivel <= 2 -> Ruina.PIRAMIDE
            nivel <= 4 -> Ruina.GLIFO
            else -> Ruina.TEMPLO
        }
        val zonas = ruina.zonas
        val piezas = zonas.map { PiezaGeometria(forma = it.forma, tono = azar.nextInt(3)) }
            .toMutableList()

        if (nivel >= 3) {
            val formasUsadas = zonas.map { it.forma }.toSet()
            val sobrante = FormaGeometrica.entries
                .filter { it !in formasUsadas }
                .shuffled(azar)
                .firstOrNull()
            if (sobrante != null) {
                piezas.add(PiezaGeometria(forma = sobrante, tono = 2))
            }
        }

        return RetoMath.Geometria(
            titulo = ruina.titulo,
            nombreConArticulo = ruina.nombreConArticulo,
            emoji = "🔺",
            zonas = zonas,
            piezas = piezas.shuffled(azar),
            consigna = "Reconstruye ${ruina.nombreConArticulo}: arrastra cada figura a su hueco.",
            pista = "Cuenta los lados: el triángulo tiene 3, el trapecio 4 y el hexágono 6."
        )
    }

    // ------------------------- estadística alebrije -------------------------

    private fun generarEstadistica(
        parametros: ParametrosDificultad
    ): RetoMath.Estadistica {
        val nivel = parametros.nivel.coerceIn(NIVEL_MINIMO_MATE, NIVEL_MAXIMO_MATE)
        val frutas = FRUTAS.shuffled(azar).take(4)
        val valores = (1..9).shuffled(azar).take(4)
        val barras = frutas.mapIndexed { indice, fruta ->
            BarraDato(
                etiqueta = fruta.etiqueta,
                emoji = fruta.emoji,
                valor = valores[indice],
                tono = indice
            )
        }

        var indiceMax = 0
        var indiceMin = 0
        barras.forEachIndexed { indice, barra ->
            if (barra.valor > barras[indiceMax].valor) indiceMax = indice
            if (barra.valor < barras[indiceMin].valor) indiceMin = indice
        }

        val pregunta: String
        val opciones: List<String>
        val indiceCorrecto: Int
        val pista: String

        when (nivel) {
            1 -> {
                pregunta = "¿Qué fruta les gusta más a los alebrijes?"
                opciones = barras.map { it.etiqueta }.shuffled(azar)
                indiceCorrecto = opciones.indexOf(barras[indiceMax].etiqueta)
                pista = "La barra más alta es la que más alebrijes eligieron."
            }

            2 -> {
                pregunta = "¿Qué fruta les gusta menos a los alebrijes?"
                opciones = barras.map { it.etiqueta }.shuffled(azar)
                indiceCorrecto = opciones.indexOf(barras[indiceMin].etiqueta)
                pista = "La barra más baja es la que menos alebrijes eligieron."
            }

            3 -> {
                val elegida = barras[azar.nextInt(barras.size)]
                pregunta = "¿Cuántos alebrijes eligieron «${elegida.etiqueta}»?"
                opciones = opcionesNumericas(
                    respuesta = elegida.valor,
                    cantidad = parametros.opciones,
                    operacion = Operacion.SUMA
                )
                indiceCorrecto = opciones.indexOf(elegida.valor.toString())
                pista = "Busca la barra de «${elegida.etiqueta}» y lee cuánto mide."
            }

            4 -> {
                val mayor = barras[indiceMax]
                val menor = barras[indiceMin]
                val diferencia = mayor.valor - menor.valor
                pregunta = "¿Cuántos alebrijes más eligieron «${mayor.etiqueta}» " +
                    "que «${menor.etiqueta}»?"
                opciones = opcionesNumericas(
                    respuesta = diferencia,
                    cantidad = parametros.opciones,
                    operacion = Operacion.RESTA
                )
                indiceCorrecto = opciones.indexOf(diferencia.toString())
                pista = "Resta la barra chiquita a la barra grande."
            }

            else -> {
                val total = barras.sumOf { it.valor }
                pregunta = "¿Cuántos alebrijes respondieron en total?"
                opciones = opcionesNumericas(
                    respuesta = total,
                    cantidad = parametros.opciones,
                    operacion = Operacion.SUMA
                )
                indiceCorrecto = opciones.indexOf(total.toString())
                pista = "Suma los números de las cuatro barras."
            }
        }

        return RetoMath.Estadistica(
            titulo = "Frutas favoritas de los Alebrijes",
            pregunta = pregunta,
            barras = barras,
            opciones = opciones,
            indiceCorrecto = indiceCorrecto,
            muestraValores = nivel >= 3,
            consigna = pregunta,
            pista = pista
        )
    }

    // -----------------------------------------------------------------------
    //  CATÁLOGOS INTERNOS
    // -----------------------------------------------------------------------

    private enum class Operacion(val simbolo: String) {
        SUMA("+"),
        RESTA("−"),
        MULTIPLICACION("×");

        fun pista(a: Int, b: Int, nivel: Int): String = when (this) {
            SUMA -> if (nivel <= 1) {
                "Junta $a tacos con $b tacos y cuéntalos todos."
            } else {
                "Suma primero las decenas y luego las unidades."
            }

            RESTA -> "Quita $b de $a. Puedes ir contando hacia atrás."

            MULTIPLICACION -> "Piensa en $a grupos de $b y ve sumándolos."
        }
    }

    private enum class Postre(
        val titulo: String,
        val nombreConArticulo: String,
        val emoji: String
    ) {
        ROSCA(
            titulo = "Rosca de Reyes",
            nombreConArticulo = "la Rosca de Reyes",
            emoji = "🍩"
        ),
        PAN_MUERTO(
            titulo = "Pan de Muerto",
            nombreConArticulo = "el Pan de Muerto",
            emoji = "🍞"
        )
    }

    private enum class Ruina(
        val titulo: String,
        val nombreConArticulo: String,
        val zonas: List<ZonaGeometria>
    ) {
        PIRAMIDE(
            titulo = "Pirámide del Sol",
            nombreConArticulo = "la Pirámide del Sol",
            zonas = listOf(
                ZonaGeometria(FormaGeometrica.TRIANGULO, 0.37f, 0.03f, 0.26f, 0.28f),
                ZonaGeometria(FormaGeometrica.TRAPECIO, 0.26f, 0.34f, 0.48f, 0.26f),
                ZonaGeometria(FormaGeometrica.TRAPECIO, 0.14f, 0.63f, 0.72f, 0.30f)
            )
        ),
        GLIFO(
            titulo = "Glifo del Jaguar",
            nombreConArticulo = "el Glifo del Jaguar",
            zonas = listOf(
                ZonaGeometria(FormaGeometrica.TRIANGULO, 0.06f, 0.36f, 0.24f, 0.24f),
                ZonaGeometria(FormaGeometrica.HEXAGONO, 0.36f, 0.28f, 0.28f, 0.30f),
                ZonaGeometria(FormaGeometrica.TRIANGULO, 0.70f, 0.36f, 0.24f, 0.24f),
                ZonaGeometria(FormaGeometrica.TRAPECIO, 0.22f, 0.66f, 0.56f, 0.28f)
            )
        ),
        TEMPLO(
            titulo = "Templo de la Luna",
            nombreConArticulo = "el Templo de la Luna",
            zonas = listOf(
                ZonaGeometria(FormaGeometrica.TRIANGULO, 0.38f, 0.02f, 0.24f, 0.24f),
                ZonaGeometria(FormaGeometrica.HEXAGONO, 0.33f, 0.30f, 0.34f, 0.26f),
                ZonaGeometria(FormaGeometrica.ROMBO, 0.04f, 0.60f, 0.28f, 0.30f),
                ZonaGeometria(FormaGeometrica.TRAPECIO, 0.40f, 0.66f, 0.56f, 0.26f)
            )
        )
    }

    private data class Fruta(val etiqueta: String, val emoji: String)

    private companion object {
        val FRUTAS: List<Fruta> = listOf(
            Fruta("manzana", "🍎"),
            Fruta("plátano", "🍌"),
            Fruta("fresa", "🍓"),
            Fruta("sandía", "🍉"),
            Fruta("naranja", "🍊"),
            Fruta("mango", "🥭"),
            Fruta("uvas", "🍇")
        )
    }
}

// ---------------------------------------------------------------------------
//  RUTEO DE LOS CUATRO MINIJUEGOS
// ---------------------------------------------------------------------------

/**
 * Punto único de entrada a la Arena Matemática.
 *
 * Cada [JuegoMatematico] abre SU composable y su propia sesión; no hay una
 * pantalla comodín. El host recibe el juego desde el mapa de la Arena y deja
 * que el [ArenaCortexMathViewModel] conserve el estado de cada minijuego por
 * separado:
 *
 *  - [JuegoMatematico.CALCULO_MENTAL] escoge operación y rango numérico.
 *  - [JuegoMatematico.FRACCIONES_FESTIVAS] escoge denominador y rebanadas.
 *  - [JuegoMatematico.GEOMETRIA_ANCESTRAL] escoge ruina, zonas y piezas extra.
 *  - [JuegoMatematico.ESTADISTICA_ALEBRIJE] escoge tipo de pregunta y si los
 *    valores se muestran sobre las barras.
 *
 * El mismo [ZaziDifficultyEngine] persiste un nivel y un andamiaje por id de
 * juego, así que subir de nivel en Cálculo Mental no altera Fracciones.
 */
@Composable
fun ArenaCortexMathHost(
    juego: JuegoMatematico,
    onSalir: () -> Unit,
    onResultado: (gano: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArenaCortexMathViewModel = viewModel()
) {
    when (juego) {
        JuegoMatematico.CALCULO_MENTAL -> CalculoMentalScreen(
            onSalir = onSalir,
            onResultado = onResultado,
            modifier = modifier,
            viewModel = viewModel
        )

        JuegoMatematico.FRACCIONES_FESTIVAS -> FraccionesFestivasScreen(
            onSalir = onSalir,
            onResultado = onResultado,
            modifier = modifier,
            viewModel = viewModel
        )

        JuegoMatematico.GEOMETRIA_ANCESTRAL -> GeometriaAncestralScreen(
            onSalir = onSalir,
            onResultado = onResultado,
            modifier = modifier,
            viewModel = viewModel
        )

        JuegoMatematico.ESTADISTICA_ALEBRIJE -> EstadisticaAlebrijeScreen(
            onSalir = onSalir,
            onResultado = onResultado,
            modifier = modifier,
            viewModel = viewModel
        )
    }
}
