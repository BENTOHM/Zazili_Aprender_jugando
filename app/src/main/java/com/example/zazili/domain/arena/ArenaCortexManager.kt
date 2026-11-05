package com.example.zazili.domain.arena

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import com.example.zazili.core.usuario.UserManager
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.minijuegos.ModuloMinijuego
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Punto de integración de la Arena Cortex.
 *
 * Junta las tres piezas en una sola sesión jugable:
 *  - [ProceduralContentEngine] arma el reto del momento,
 *  - [ZaziDifficultyEngine] decide nivel, N-Back y andamiaje,
 *  - [DeepGamePuzzleManager] decide qué regla atómica se juega y cuándo se
 *    comunica, verifica o re-explica.
 *
 * La sesión nunca resta vidas ni muestra "Game Over": un fallo devuelve el
 * **mismo** reto con más apoyo, y el segundo fallo cierra la ronda para seguir
 * adelante. Al terminar, se guardan marca personal y minutos jugados, y si hay
 * puente se avisa al motor histórico de estrellas para que el resto de la app
 * siga viendo el mismo avance.
 */

/** Rondas por sesión de Arena. */
const val RONDAS_SESION = 8

/** Puntos por acierto, multiplicados por el nivel cognitivo. */
const val PUNTOS_BASE = 10

/** Puntos extra por cada acierto de la racha. */
const val PUNTOS_RACHA = 2

/** Segundos estimados por reto para la bitácora de minutos jugados. */
const val SEGUNDOS_POR_RETO = 25

/** Semilla que separa una partida de la siguiente. */
private const val SEMILLA_POR_PARTIDA = 1_000L

/** Archivo de marcas personales de la Arena. */
const val ARCHIVO_PARTIDAS = "zazili_arena_metricas"

private const val MENSAJE_PRIMER_FALLO =
    "Casi. El mismo reto, ahora con más apoyo."

private const val MENSAJE_SEGUNDO_FALLO =
    "Cerramos esta ronda y seguimos. Nadie se queda sin jugar."

private const val MENSAJE_CIERRE =
    "Terminaste la ronda de la Arena. Lo que practicaste se queda contigo."

private const val MENSAJE_SALIDA =
    "Cerramos por hoy. Vuelve cuando quieras, sin prisa."

private const val MENSAJE_CERRADA =
    "Esta sesión ya cerró. Abre otra para seguir jugando."

/** Fotografía completa de una sesión en curso. */
@Immutable
data class EstadoJuegoArena(
    val juegoId: String,
    val titulo: String,
    val modulo: ModuloMinijuego,
    val reto: RetoArena,
    val ronda: Int,
    val totalRondas: Int,
    val rondasCerradas: Int,
    val aciertos: Int,
    val errores: Int,
    val puntos: Int,
    val racha: Int,
    val reintentos: Int,
    val enReintento: Boolean,
    val terminado: Boolean,
    val mensajeNino: String,
    val nivel: Int,
    val fase: FaseAprendizaje,
    val andamiaje: NivelAndamiaje,
    val reglas: List<ReglaActiva>,
    val reglaActiva: String?,
    val verificaciones: Int
) {
    val progreso: Float
        get() = if (totalRondas <= 0) {
            0f
        } else {
            (rondasCerradas.toFloat() / totalRondas).coerceIn(0f, 1f)
        }

    val estrellas: Int get() = estrellasDe(aciertos, totalRondas)

    val reglasInteriorizadas: Int get() = reglas.count { it.interiorizada }

    val complejidad: Int get() = reglas.size
}

/** Lo que devuelve cada respuesta, ya con la foto nueva del estado. */
@Immutable
data class ResultadoTurno(
    val correcto: Boolean,
    val eco: String?,
    val puntosGanados: Int,
    val mensajeNino: String,
    val reglaNueva: ReglaAtomica?,
    val reglaInteriorizada: ReglaAtomica?,
    val reglaReforzada: ReglaAtomica?,
    val andamiaje: NivelAndamiaje,
    val estado: EstadoJuegoArena
) {
    val sesionTerminada: Boolean get() = estado.terminado
}

/** Cierre de sesión: lo que ve la pantalla final y lo que queda guardado. */
@Immutable
data class ResumenSesion(
    val juegoId: String,
    val rondas: Int,
    val aciertos: Int,
    val errores: Int,
    val reintentos: Int,
    val puntos: Int,
    val estrellas: Int,
    val tiempoPromedioMs: Long,
    val nivel: Int,
    val andamiaje: NivelAndamiaje,
    val reglasInteriorizadas: Int,
    val mensaje: String
) {
    val aprobado: Boolean get() = estrellas >= 2
}

/** Historial acumulado de un minijuego. */
@Immutable
data class ProgresoArena(
    val juegoId: String,
    val partidas: Int,
    val mejorPuntos: Int,
    val mejorEstrellas: Int,
    val aciertosTotales: Int,
    val segundosJugados: Long,
    val nivel: Int,
    val reglasInteriorizadas: Int
) {
    val minutosJugados: Long get() = segundosJugados / 60
}

/** Total del módulo, para la portada de la Arena. */
@Immutable
data class PanoramaArena(
    val juegos: Int,
    val combinaciones: Long,
    val minutosEstimados: Long,
    val partidasJugadas: Int,
    val minutosJugados: Long,
    val nivelPromedio: Float
)

internal fun estrellasDe(aciertos: Int, totalRondas: Int): Int = when {
    totalRondas <= 0 -> 0
    aciertos >= totalRondas -> 3
    aciertos * 10 >= totalRondas * 7 -> 2
    aciertos * 2 >= totalRondas -> 1
    else -> 0
}

/**
 * Una sesión de un minijuego. Mantiene su propio estado reactivo y delega cada
 * decisión a los motores; no conoce Compose ni pinta nada.
 */
class SesionArena internal constructor(
    private val manager: ArenaCortexManager,
    val juego: Minijuego,
    val totalRondas: Int
) {

    val juegoId: String = juego.id

    private val semilla: Long = manager.semillaDePartida(juegoId)

    private var dificultad: EstadoDificultad = manager.dificultad.estado(juegoId)
    private var profundo: EstadoProfundo = manager.profundidad.estado(juegoId)
    private var ronda: Int = 1
    private var rondasCerradas: Int = 0
    private var aciertos: Int = 0
    private var errores: Int = 0
    private var puntos: Int = 0
    private var racha: Int = 0
    private var reintentos: Int = 0
    private var respuestas: Int = 0
    private var tiempoTotalMs: Long = 0L
    private var enReintento: Boolean = false
    private var terminado: Boolean = false
    private var mensaje: String = dificultad.mensajeNino
    private var inicioRondaMs: Long = manager.ahora()
    private var resumen: ResumenSesion? = null
    private var reto: RetoArena = manager.generarReto(juego, ronda, semilla)

    private val _estado = MutableStateFlow(construir())

    val estado: StateFlow<EstadoJuegoArena> = _estado.asStateFlow()

    /** Evalúa la opción tocada. Un fallo no cierra la ronda: la amplía. */
    fun responder(indice: Int?): ResultadoTurno {
        if (terminado) return ResultadoTurno(
            correcto = false,
            eco = null,
            puntosGanados = 0,
            mensajeNino = MENSAJE_CERRADA,
            reglaNueva = null,
            reglaInteriorizada = null,
            reglaReforzada = null,
            andamiaje = dificultad.parametros.andamiaje,
            estado = _estado.value
        )
        val reaccion = manager.ahora() - inicioRondaMs
        return if (reto.contenido.evaluar(indice)) {
            acertar(reaccion)
        } else {
            fallar(reaccion)
        }
    }

    /** La pista del reto, o null si una regla atómica la escondió. */
    fun pedirPista(): String? =
        if (terminado || !reto.pistaDisponible) null else reto.contenido.pista

    /**
     * Mismo reto, más apoyo. Cuenta como reintento para el DDA y nunca resta
     * puntos: pedir ayuda es parte de jugar.
     */
    fun reintentar(): EstadoJuegoArena {
        if (terminado) return _estado.value
        dificultad = manager.dificultad.registrarReintento(juegoId)
        reintentos += 1
        reto = manager.generarReto(juego, ronda, semilla)
        inicioRondaMs = manager.ahora()
        mensaje = "Seguimos con el mismo reto, ahora con más apoyo."
        publicar()
        return _estado.value
    }

    /** Cierra la sesión (aunque falten rondas) y devuelve el resumen. */
    fun cerrar(): ResumenSesion {
        if (!terminado) {
            terminado = true
            mensaje = MENSAJE_SALIDA
            publicar()
        }
        return cerrarSesion()
    }

    // ------------------------- turnos -------------------------

    private fun acertar(reaccionMs: Long): ResultadoTurno {
        val previo = reto
        dificultad = manager.dificultad.registrarAcierto(juegoId, reaccionMs)
        val reglas = manager.profundidad.registrarAcierto(juegoId)
        profundo = reglas.estado

        registrarTiempo(reaccionMs)
        val ganados = PUNTOS_BASE * dificultad.nivel + (racha + 1) * PUNTOS_RACHA
        puntos += ganados
        aciertos += 1
        racha += 1
        rondasCerradas += 1
        enReintento = false
        mensaje = mensajeDeRegla(reglas) ?: dificultad.mensajeNino

        avanzarRonda(oUltima = rondasCerradas >= totalRondas)
        return ResultadoTurno(
            correcto = true,
            eco = ecoDe(previo),
            puntosGanados = ganados,
            mensajeNino = mensaje,
            reglaNueva = reglas.reglaNueva,
            reglaInteriorizada = reglas.reglaInteriorizada,
            reglaReforzada = reglas.reglaReforzada,
            andamiaje = dificultad.parametros.andamiaje,
            estado = _estado.value
        )
    }

    private fun fallar(reaccionMs: Long): ResultadoTurno {
        dificultad = manager.dificultad.registrarFallo(juegoId, reaccionMs)
        val reglas = manager.profundidad.registrarFallo(juegoId)
        profundo = reglas.estado

        registrarTiempo(reaccionMs)
        errores += 1
        racha = 0

        if (!enReintento) {
            enReintento = true
            reto = manager.generarReto(juego, ronda, semilla)
            inicioRondaMs = manager.ahora()
            mensaje = mensajeDeRegla(reglas) ?: MENSAJE_PRIMER_FALLO
            publicar()
            return ResultadoTurno(
                correcto = false,
                eco = null,
                puntosGanados = 0,
                mensajeNino = mensaje,
                reglaNueva = reglas.reglaNueva,
                reglaInteriorizada = reglas.reglaInteriorizada,
                reglaReforzada = reglas.reglaReforzada,
                andamiaje = dificultad.parametros.andamiaje,
                estado = _estado.value
            )
        }

        enReintento = false
        rondasCerradas += 1
        mensaje = mensajeDeRegla(reglas) ?: MENSAJE_SEGUNDO_FALLO
        avanzarRonda(oUltima = rondasCerradas >= totalRondas)
        return ResultadoTurno(
            correcto = false,
            eco = null,
            puntosGanados = 0,
            mensajeNino = mensaje,
            reglaNueva = reglas.reglaNueva,
            reglaInteriorizada = reglas.reglaInteriorizada,
            reglaReforzada = reglas.reglaReforzada,
            andamiaje = dificultad.parametros.andamiaje,
            estado = _estado.value
        )
    }

    private fun avanzarRonda(oUltima: Boolean) {
        if (oUltima) {
            rondasCerradas = totalRondas
            terminado = true
            mensaje = MENSAJE_CIERRE
            publicar()
            cerrarSesion()
            return
        }
        ronda += 1
        reto = manager.generarReto(juego, ronda, semilla)
        inicioRondaMs = manager.ahora()
        publicar()
    }

    private fun mensajeDeRegla(resultado: ResultadoProfundo): String? =
        if (resultado.reglaNueva != null ||
            resultado.reglaInteriorizada != null ||
            resultado.reglaReforzada != null
        ) {
            resultado.mensajeNino
        } else {
            null
        }

    private fun registrarTiempo(reaccionMs: Long) {
        respuestas += 1
        if (reaccionMs > 0L) tiempoTotalMs += reaccionMs
    }

    private fun ecoDe(reto: RetoArena): String? =
        (reto.contenido as? ContenidoReto.Dilema)?.eco

    private fun publicar() {
        _estado.value = construir()
    }

    private fun construir(): EstadoJuegoArena = EstadoJuegoArena(
        juegoId = juegoId,
        titulo = juego.titulo,
        modulo = juego.modulo,
        reto = reto,
        ronda = ronda,
        totalRondas = totalRondas,
        rondasCerradas = rondasCerradas,
        aciertos = aciertos,
        errores = errores,
        puntos = puntos,
        racha = racha,
        reintentos = reintentos,
        enReintento = enReintento,
        terminado = terminado,
        mensajeNino = mensaje,
        nivel = dificultad.nivel,
        fase = dificultad.fase,
        andamiaje = dificultad.parametros.andamiaje,
        reglas = profundo.activas,
        reglaActiva = reto.reglaActiva,
        verificaciones = profundo.verificaciones
    )

    private fun cerrarSesion(): ResumenSesion {
        resumen?.let { return it }
        val cierre = ResumenSesion(
            juegoId = juegoId,
            rondas = rondasCerradas,
            aciertos = aciertos,
            errores = errores,
            reintentos = reintentos,
            puntos = puntos,
            estrellas = estrellasDe(aciertos, totalRondas),
            tiempoPromedioMs = if (respuestas <= 0) 0L else tiempoTotalMs / respuestas,
            nivel = dificultad.nivel,
            andamiaje = dificultad.parametros.andamiaje,
            reglasInteriorizadas = profundo.reglasInteriorizadas,
            mensaje = mensaje.ifBlank { MENSAJE_CIERRE }
        )
        manager.finalizarPartida(cierre)
        resumen = cierre
        return cierre
    }
}

/**
 * Fachada de la Arena Cortex.
 *
 * Las pantallas crean una [SesionArena] por juego y observan su `estado`. Todo
 * lo demás (contenido, dificultad, reglas, marcas) vive aquí.
 */
class ArenaCortexManager(
    internal val dificultad: ZaziDifficultyEngine,
    internal val profundidad: DeepGamePuzzleManager,
    internal val procedural: ProceduralContentEngine = ProceduralContentEngine(),
    private val partidas: SharedPreferences? = null,
    private val puenteProgreso: ((String, Boolean) -> Unit)? = null,
    private val relojMs: () -> Long = { System.nanoTime() / 1_000_000L }
) {

    /** Entrar por `todos` respeta el orden de inicialización de [Minijuego]. */
    private val catalogo: List<Minijuego> = Minijuego.todos

    fun juegos(): List<Minijuego> = catalogo

    fun iniciarSesion(juegoId: String, totalRondas: Int = RONDAS_SESION): SesionArena {
        require(totalRondas > 0) { "Una sesión necesita al menos una ronda" }
        val juego = Minijuego.porId(juegoId)
            ?: error("Minijuego desconocido en la Arena: $juegoId")
        return SesionArena(this, juego, totalRondas)
    }

    fun reglasDe(juegoId: String): List<ReglaAtomica> = profundidad.reglasDe(juegoId)

    fun diversidad(juegoId: String): Diversidad = procedural.diversidad(juegoId)

    fun estadoDificultad(juegoId: String): EstadoDificultad = dificultad.estado(juegoId)

    fun estadoProfundo(juegoId: String): EstadoProfundo = profundidad.estado(juegoId)

    fun progresoDe(juegoId: String): ProgresoArena = ProgresoArena(
        juegoId = juegoId,
        partidas = leerInt("jugadas_$juegoId"),
        mejorPuntos = leerInt("mejor_puntos_$juegoId"),
        mejorEstrellas = leerInt("mejor_estrellas_$juegoId"),
        aciertosTotales = leerInt("aciertos_$juegoId"),
        segundosJugados = leerLong("segundos_$juegoId"),
        nivel = dificultad.estado(juegoId).nivel,
        reglasInteriorizadas = profundidad.estado(juegoId).reglasInteriorizadas
    )

    fun panorama(): PanoramaArena {
        val diversidad = procedural.diversidadTotal()
        val jugadas = catalogo.sumOf { leerInt("jugadas_${it.id}") }
        val segundos = catalogo.sumOf { leerLong("segundos_${it.id}") }
        val niveles = catalogo.map { dificultad.estado(it.id).nivel }
        return PanoramaArena(
            juegos = catalogo.size,
            combinaciones = diversidad.combinaciones,
            minutosEstimados = diversidad.minutosEstimados,
            partidasJugadas = jugadas,
            minutosJugados = segundos / 60,
            nivelPromedio = if (niveles.isEmpty()) {
                0f
            } else {
                niveles.average().toFloat()
            }
        )
    }

    /** Borra el avance de un juego; se usa desde Ajustes, no en medio de una sesión. */
    fun reiniciarJuego(juegoId: String) {
        dificultad.reiniciar(juegoId)
        profundidad.reiniciar(juegoId)
        val editor = partidas?.edit() ?: return
        editor.remove("jugadas_$juegoId")
        editor.remove("mejor_puntos_$juegoId")
        editor.remove("mejor_estrellas_$juegoId")
        editor.remove("aciertos_$juegoId")
        editor.remove("segundos_$juegoId")
        editor.apply()
    }

    // ------------------------- puente con las sesiones -------------------------

    internal fun generarReto(juego: Minijuego, numero: Int, semilla: Long): RetoArena =
        procedural.generar(
            juegoId = juego.id,
            parametros = dificultad.parametros(juego.id),
            reglas = profundidad.activas(juego.id),
            numero = numero,
            semillaExtra = semilla
        )

    internal fun semillaDePartida(juegoId: String): Long =
        (leerInt("jugadas_$juegoId") + 1L) * SEMILLA_POR_PARTIDA

    internal fun ahora(): Long = relojMs()

    internal fun finalizarPartida(cierre: ResumenSesion) {
        val juego = cierre.juegoId
        val editor = partidas?.edit()
        if (editor != null) {
            editor.putInt("jugadas_$juego", leerInt("jugadas_$juego") + 1)
            editor.putInt(
                "mejor_puntos_$juego",
                maxOf(leerInt("mejor_puntos_$juego"), cierre.puntos)
            )
            editor.putInt(
                "mejor_estrellas_$juego",
                maxOf(leerInt("mejor_estrellas_$juego"), cierre.estrellas)
            )
            editor.putInt("aciertos_$juego", leerInt("aciertos_$juego") + cierre.aciertos)
            editor.putLong(
                "segundos_$juego",
                leerLong("segundos_$juego") + cierre.rondas.toLong() * SEGUNDOS_POR_RETO
            )
            editor.apply()
        }
        puenteProgreso?.invoke(juego, cierre.aprobado)
    }

    private fun leerInt(clave: String): Int = partidas?.getInt(clave, 0) ?: 0

    private fun leerLong(clave: String): Long = partidas?.getLong(clave, 0L) ?: 0L

    companion object {
        @Volatile
        private var instancia: ArenaCortexManager? = null

        /**
         * Instancia única de la app. Se auto-conecta al motor histórico de
         * estrellas para que [com.example.zazili.presentation.arena.ArenaCortexScreen]
         * y la lección sigan leyendo el mismo avance.
         */
        fun obtener(context: Context): ArenaCortexManager {
            val app = context.applicationContext
            return instancia ?: synchronized(this) {
                instancia ?: construir(app).also { instancia = it }
            }
        }

        /** Sin Android ni disco: ideal para pruebas unitarias. */
        fun paraPruebas(): ArenaCortexManager = ArenaCortexManager(
            dificultad = ZaziDifficultyEngine(),
            profundidad = DeepGamePuzzleManager(),
            procedural = ProceduralContentEngine()
        )

        private fun construir(context: Context): ArenaCortexManager {
            val historico = DynamicDifficultyEngine(context, UserManager(context))
            return ArenaCortexManager(
                dificultad = ZaziDifficultyEngine(context) { juegoId ->
                    historico.estado.value.nivelDe(juegoId)
                },
                profundidad = DeepGamePuzzleManager(context),
                partidas = context.getSharedPreferences(
                    ARCHIVO_PARTIDAS,
                    Context.MODE_PRIVATE
                ),
                puenteProgreso = { juegoId, gano ->
                    historico.registrarResultadoEnSegundoPlano(juegoId, gano)
                }
            )
        }
    }
}
