package com.example.zazili.domain.arena

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.core.usuario.UserManager
import com.example.zazili.domain.diagnostico.EstrategiaSemillas
import com.example.zazili.domain.diagnostico.PerfilFrustracion
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.minijuegos.ModuloMinijuego
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.dataStoreArena: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_arena_cortex"
)

/** Victorias consecutivas necesarias para subir un escalón. */
const val VICTORIAS_PARA_SUBIR: Int = 3

/** Derrotas consecutivas que devuelven un escalón de aire. */
const val DERROTAS_PARA_BAJAR: Int = 2

/**
 * Progreso adaptativo de UN minijuego.
 *
 * [nivel] siempre vive entre 1 y 5. Las rachas se reinician al cruzar un
 * umbral para que el siguiente cambio exija otra racha completa: así el niño
 * no rebota entre niveles por un solo acierto o error.
 */
@Immutable
data class ProgresoDificultad(
    val juegoId: String,
    val nivel: Int = Minijuego.NIVEL_MINIMO,
    val rachaVictorias: Int = 0,
    val rachaDerrotas: Int = 0,
    val partidasJugadas: Int = 0
) {
    val estrellas: Int
        get() = nivel.coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO)

    val victoriasParaSubir: Int
        get() = (VICTORIAS_PARA_SUBIR - rachaVictorias).coerceAtLeast(0)

    val derrotasParaBajar: Int
        get() = (DERROTAS_PARA_BAJAR - rachaDerrotas).coerceAtLeast(0)
}

/** Fotografía completa de la Arena: los 16 juegos siempre presentes. */
@Immutable
data class EstadoArena(
    val progresos: Map<String, ProgresoDificultad> = emptyMap()
) {
    fun progresoDe(juegoId: String): ProgresoDificultad =
        progresos[juegoId] ?: ProgresoDificultad(juegoId)

    fun nivelDe(juegoId: String): Int = progresoDe(juegoId).estrellas

    val nivelPromedio: Int
        get() {
            if (progresos.isEmpty()) return Minijuego.NIVEL_MINIMO
            val suma = progresos.values.sumOf { it.estrellas }
            return (suma / progresos.size).coerceIn(
                Minijuego.NIVEL_MINIMO,
                Minijuego.NIVEL_MAXIMO
            )
        }
}

/**
 * El cerebro adaptativo de la Arena Cortex.
 *
 * Lee la línea base del vuelo de prueba (FTUE) de [UserManager] una sola vez y
 * la traduce al nivel inicial de cada juego según su materia. Después, cada
 * resultado actualiza rachas y nivel en un DataStore propio, con escrituras
 * atómicas y sin bloquear el hilo principal.
 *
 * Reglas del flujo (zona de desarrollo próximo):
 *  - 3 victorias seguidas en el mismo juego suben un nivel (máximo 5).
 *  - 2 derrotas seguidas bajan un nivel (mínimo 1).
 *
 * [estado] es reactivo: cualquier pantalla que lo colecte ve el cambio de
 * estrellas sin volver a consultar el disco.
 */
class DynamicDifficultyEngine(
    context: Context,
    userManager: UserManager
) {

    private val dataStore: DataStore<Preferences> =
        context.applicationContext.dataStoreArena

    private val perfilActual = MutableStateFlow<PerfilUsuario?>(null)

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Serializa los clics de resultados: dos respuestas simultáneas no pueden
    // leer la misma racha vieja ni pisarse la escritura.
    private val candado = Mutex()

    init {
        ambito.launch {
            userManager.perfil
                .catch { emit(null) }
                .collect { perfil -> perfilActual.value = perfil }
        }
    }

    private val preferencias: Flow<Preferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val estado: StateFlow<EstadoArena> = combine(
        preferencias,
        perfilActual
    ) { guardadas, perfil ->
        construirEstado(guardadas, perfil)
    }.stateIn(
        scope = ambito,
        started = SharingStarted.Eagerly,
        initialValue = construirEstado(emptyPreferences(), null)
    )

    fun nivelActual(juegoId: String): Int = estado.value.nivelDe(juegoId)

    /**
     * Variante para pantallas: la escritura vive en el ámbito del motor, no en
     * el de Compose. Así, salir justo al terminar la partida no cancela el
     * guardado de la racha.
     */
    fun registrarResultadoEnSegundoPlano(juegoId: String, gano: Boolean) {
        ambito.launch {
            runCatching { registrarResultado(juegoId, gano) }
        }
    }

    /**
     * Registra una partida terminada. `gano` significa que el niño alcanzó el
     * umbral de la sesión (no cada respuesta suelta), para que las rachas
     * midan sesiones completas y no micro-aciertos.
     */
    suspend fun registrarResultado(
        juegoId: String,
        gano: Boolean
    ): ProgresoDificultad = candado.withLock {
        val juego = Minijuego.porId(juegoId)
            ?: return@withLock ProgresoDificultad(juegoId)

        var resultado = estado.value.progresoDe(juego.id)
        val escritura = runCatching {
            dataStore.edit { guardadas ->
                val previo = leerProgreso(guardadas, juego, perfilActual.value)
                resultado = siguienteProgreso(previo, gano)
                guardadas[Claves.nivel(juego.id)] = resultado.nivel
                guardadas[Claves.rachaVictorias(juego.id)] = resultado.rachaVictorias
                guardadas[Claves.rachaDerrotas(juego.id)] = resultado.rachaDerrotas
                guardadas[Claves.partidas(juego.id)] = resultado.partidasJugadas
            }
        }
        escritura.exceptionOrNull()?.let { error ->
            // La cancelación de la pantalla no es un fallo de disco: debe
            // propagarse para que el ámbito del llamador se cierre limpio.
            if (error is CancellationException) throw error
        }
        if (escritura.isFailure) {
            // El disco no respondió: la partida no se pierde de la vista, pero
            // tampoco se inventa un nivel que no quedó guardado.
            resultado = estado.value.progresoDe(juego.id)
        }
        resultado
    }

    /**
     * Aplica la transición de rachas sobre un progreso ya leído. Es una
     * función pura para que la regla viva en un solo lugar.
     */
    private fun siguienteProgreso(
        previo: ProgresoDificultad,
        gano: Boolean
    ): ProgresoDificultad {
        var nivel = previo.estrellas
        var victorias: Int
        var derrotas: Int

        if (gano) {
            victorias = previo.rachaVictorias + 1
            derrotas = 0
            if (victorias >= VICTORIAS_PARA_SUBIR) {
                nivel = (nivel + 1).coerceAtMost(Minijuego.NIVEL_MAXIMO)
                victorias = 0
            }
        } else {
            victorias = 0
            derrotas = previo.rachaDerrotas + 1
            if (derrotas >= DERROTAS_PARA_BAJAR) {
                nivel = (nivel - 1).coerceAtLeast(Minijuego.NIVEL_MINIMO)
                derrotas = 0
            }
        }

        return previo.copy(
            nivel = nivel,
            rachaVictorias = victorias,
            rachaDerrotas = derrotas,
            partidasJugadas = previo.partidasJugadas + 1
        )
    }

    private fun leerProgreso(
        guardadas: Preferences,
        juego: Minijuego,
        perfil: PerfilUsuario?
    ): ProgresoDificultad = ProgresoDificultad(
        juegoId = juego.id,
        nivel = (
            guardadas[Claves.nivel(juego.id)]
                ?: nivelBase(juego, perfil)
            ).coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO),
        rachaVictorias = (guardadas[Claves.rachaVictorias(juego.id)] ?: 0)
            .coerceAtLeast(0),
        rachaDerrotas = (guardadas[Claves.rachaDerrotas(juego.id)] ?: 0)
            .coerceAtLeast(0),
        partidasJugadas = (guardadas[Claves.partidas(juego.id)] ?: 0)
            .coerceAtLeast(0)
    )

    private fun construirEstado(
        guardadas: Preferences,
        perfil: PerfilUsuario?
    ): EstadoArena {
        val progresos = LinkedHashMap<String, ProgresoDificultad>(
            Minijuego.todos.size
        )
        Minijuego.todos.forEach { juego ->
            progresos[juego.id] = leerProgreso(guardadas, juego, perfil)
        }
        return EstadoArena(progresos = progresos)
    }

    /**
     * Línea base FTUE por materia:
     *  - Matemáticas y Lenguaje usan su nivel calibrado.
     *  - Emociones usa el perfil de frustración (resiliente arranca más alto).
     *  - Ciencias hereda el mismo cálculo del mapa científico: base de
     *    matemáticas + estrategia de semillas + ajuste por edad.
     */
    private fun nivelBase(
        juego: Minijuego,
        perfil: PerfilUsuario?
    ): Int {
        if (perfil == null) return Minijuego.NIVEL_MINIMO

        val base = when (juego.modulo) {
            ModuloMinijuego.Matematicas -> perfil.nivelMatematicas
            ModuloMinijuego.Lenguaje -> perfil.nivelLenguaje
            ModuloMinijuego.Emociones -> when (perfil.perfilFrustracion) {
                PerfilFrustracion.RESILIENTE -> 3
                PerfilFrustracion.IMPULSIVO -> 2
                PerfilFrustracion.BAJA_TOLERANCIA -> 1
                null -> Minijuego.NIVEL_MINIMO
            }

            ModuloMinijuego.Ciencias -> {
                val baseCiencia = perfil.nivelMatematicas.coerceIn(
                    Minijuego.NIVEL_MINIMO,
                    Minijuego.NIVEL_MAXIMO
                )
                val bonus = when (perfil.estrategiaSemillas) {
                    EstrategiaSemillas.DECENAS -> 2
                    EstrategiaSemillas.AGRUPACION_PARCIAL -> 1
                    EstrategiaSemillas.CONTEO_UNITARIO, null -> 0
                }
                val ajusteEdad = when {
                    perfil.edad >= 9 -> 1
                    perfil.edad in 1..6 -> -1
                    else -> 0
                }
                baseCiencia + bonus + ajusteEdad
            }
        }

        return base.coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO)
    }

    private object Claves {
        fun nivel(juegoId: String) = intPreferencesKey("nivel_$juegoId")
        fun rachaVictorias(juegoId: String) =
            intPreferencesKey("racha_victorias_$juegoId")

        fun rachaDerrotas(juegoId: String) =
            intPreferencesKey("racha_derrotas_$juegoId")

        fun partidas(juegoId: String) = intPreferencesKey("partidas_$juegoId")
    }
}
