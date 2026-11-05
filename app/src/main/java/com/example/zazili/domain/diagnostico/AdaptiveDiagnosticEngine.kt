package com.example.zazili.domain.diagnostico

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.domain.arena.ARCHIVO_METRICAS
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliCreamSoft
import com.example.zazili.ui.theme.ZaziliWhite
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.dataStorePerfilAdaptativo: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_perfil_adaptativo"
)

/** Los cuatro vectores que evalúa el diagnóstico adaptativo. */
enum class VectorDiagnostico(val etiqueta: String) {
    EDAD("Edad y grado"),
    LOGICA_MATEMATICA("Lógica matemática"),
    COMPRENSION_LECTORA("Comprensión lectora"),
    VELOCIDAD_COGNITIVA("Velocidad y memoria")
}

/**
 * Perfil global que leen los minijuegos y las lecciones.
 *
 * [multiplicadorVelocidad] > 1 acorta el reloj de cada ronda; < 1 lo alarga.
 * [complejidad] mueve el escalón base de un juego de 1 a 5. Ambos se guardan
 * en DataStore, así que sobreviven reinicios y ningún motor tiene que volver a
 * pedir el diagnóstico.
 */
@Immutable
data class DifficultyProfile(
    val multiplicadorVelocidad: Float = 1f,
    val complejidad: Int = 2,
    val edadSugerida: Int = 8,
    val apoyoAudio: Boolean = true,
    val aciertoEsperado: Float = 0.55f,
    val calibrado: Boolean = false
) {
    /** Alias estable que consumen los motores de minijuegos. */
    val SPEED_MULTIPLIER: Float get() = multiplicadorVelocidad

    /** Alias estable que consumen los motores de dificultad. */
    val COMPLEXITY_LEVEL: Int get() = complejidad

    /**
     * Convierte los segundos base de una ronda al reloj efectivo del niño.
     * Nunca deja una ronda por debajo de 3 s ni la estira al doble.
     */
    fun segundosDeTemporizador(baseSegundos: Int): Int {
        val seguro = multiplicadorVelocidad.coerceIn(MIN_MULTIPLICADOR, MAX_MULTIPLICADOR)
        return (baseSegundos / seguro).roundToInt()
            .coerceIn(MIN_TEMPORIZADOR_SEGUNDOS, (baseSegundos * 2).coerceAtLeast(6))
    }

    fun milisegundosDeTemporizador(baseMs: Long): Long =
        segundosDeTemporizador((baseMs / 1_000L).toInt()).toLong() * 1_000L

    /** Nivel 1..5 sugerido para un juego que arranca en [base]. */
    fun nivelPara(base: Int): Int =
        (base + complejidad - 2).coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO)

    /** Cantidad de opciones visible según la carga cognitiva. */
    fun opcionesPara(base: Int): Int =
        (2 + complejidad / 2).coerceIn(2, base.coerceIn(2, 6))

    /** Frecuencia de aparición para juegos con enemigos o distractores. */
    fun milisegundosEntreApariciones(baseMs: Long): Long =
        (baseMs / multiplicadorVelocidad.coerceIn(MIN_MULTIPLICADOR, MAX_MULTIPLICADOR))
            .toLong()
            .coerceIn(350L, baseMs * 3)

    companion object {
        const val MIN_MULTIPLICADOR = 0.75f
        const val MAX_MULTIPLICADOR = 1.25f
        private const val MIN_TEMPORIZADOR_SEGUNDOS = 3
    }
}

/** Una pregunta del diagnóstico. Las informativas no cuentan para la nota. */
@Immutable
data class PreguntaAdaptativa(
    val id: String,
    val vector: VectorDiagnostico,
    val enunciado: String,
    val opciones: List<String>,
    val indiceCorrecto: Int,
    val textoApoyo: String? = null,
    val esInformativa: Boolean = false
)

@Immutable
data class RespuestaAdaptativa(
    val preguntaId: String,
    val vector: VectorDiagnostico,
    val indiceElegido: Int,
    val correcta: Boolean,
    val tiempoMs: Long
)

/**
 * Estado vivo del diagnóstico.
 *
 * [indicePregunta] es la única fuente de verdad del avance: cada pulsación de
 * "Siguiente" publica un estado NUEVO con `indicePregunta + 1`. Compose
 * colecta este flujo con `collectAsStateWithLifecycle()`, por eso el botón ya
 * no puede quedarse congelado (el bug clásico de mutar un `var` interno que
 * nunca emite).
 */
@Immutable
data class EstadoDiagnosticoAdaptativo(
    val preguntas: List<PreguntaAdaptativa> = emptyList(),
    val indicePregunta: Int = 0,
    val seleccion: Int? = null,
    val respuestas: List<RespuestaAdaptativa> = emptyList(),
    val perfil: DifficultyProfile? = null
) {
    val currentQuestionIndex: Int get() = indicePregunta
    val preguntaActual: PreguntaAdaptativa? get() = preguntas.getOrNull(indicePregunta)
    val esUltima: Boolean get() = indicePregunta >= preguntas.lastIndex
    val puedeAvanzar: Boolean get() = seleccion != null && perfil == null
    val avance: Float
        get() = if (preguntas.isEmpty()) 0f
        else ((indicePregunta + 1).toFloat() / preguntas.size).coerceIn(0f, 1f)
}

/**
 * Máquina de estados pura del diagnóstico adaptativo.
 *
 * Toda transición usa [MutableStateFlow.update], así que dos toques seguidos
 * nunca leen el índice viejo: el primero avanza y el segundo ya ve la pregunta
 * siguiente (o queda ignorado si no hay selección). No conoce Android ni
 * Compose y recibe el reloj por inyección para medir la velocidad cognitiva
 * sin depender de los frames.
 */
class AdaptiveDiagnosticEngine(
    private val edadInicial: Int = 8,
    private val reloj: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val _estado = MutableStateFlow(
        EstadoDiagnosticoAdaptativo(preguntas = bancoDePreguntas(edadInicial))
    )
    val estado: StateFlow<EstadoDiagnosticoAdaptativo> = _estado.asStateFlow()

    private var inicioPreguntaMs = reloj()
    private var tiempoRespuestaMs = 0L

    /** Selección sin avanzar; apto para doble toque accidental. */
    fun responder(indiceOpcion: Int) {
        val ahora = reloj()
        var aceptada = false
        _estado.update { actual ->
            val pregunta = actual.preguntaActual
            if (actual.perfil != null || pregunta == null || indiceOpcion !in pregunta.opciones.indices) {
                actual
            } else {
                aceptada = true
                actual.copy(seleccion = indiceOpcion)
            }
        }
        if (aceptada) {
            tiempoRespuestaMs = (ahora - inicioPreguntaMs).coerceAtLeast(0L)
        }
    }

    /**
     * Avanza una pregunta. Aquí vive el arreglo del botón congelado: el estado
     * se REEMPLAZA con un índice nuevo dentro de [update], de modo que
     * `collectAsStateWithLifecycle()` recibe una emisión en el acto.
     */
    fun siguiente() {
        _estado.update { actual ->
            if (actual.perfil != null) return@update actual
            val pregunta = actual.preguntaActual ?: return@update actual
            val elegido = actual.seleccion ?: return@update actual
            val respuesta = RespuestaAdaptativa(
                preguntaId = pregunta.id,
                vector = pregunta.vector,
                indiceElegido = elegido,
                correcta = !pregunta.esInformativa && elegido == pregunta.indiceCorrecto,
                tiempoMs = tiempoRespuestaMs
            )
            val respuestas = actual.respuestas
                .filterNot { it.preguntaId == pregunta.id } + respuesta
            if (actual.indicePregunta >= actual.preguntas.lastIndex) {
                actual.copy(
                    respuestas = respuestas,
                    perfil = calcularPerfil(respuestas)
                )
            } else {
                actual.copy(
                    respuestas = respuestas,
                    indicePregunta = actual.indicePregunta + 1,
                    seleccion = null
                )
            }
        }
        tiempoRespuestaMs = 0L
        inicioPreguntaMs = reloj()
    }

    fun reiniciar() {
        _estado.value = EstadoDiagnosticoAdaptativo(
            preguntas = bancoDePreguntas(edadInicial)
        )
        tiempoRespuestaMs = 0L
        inicioPreguntaMs = reloj()
    }

    private fun calcularPerfil(respuestas: List<RespuestaAdaptativa>): DifficultyProfile {
        val edadElegida = respuestas
            .firstOrNull { it.vector == VectorDiagnostico.EDAD }
            ?.let { EDADES_POR_OPCION.getOrElse(it.indiceElegido) { edadInicial } }
            ?: edadInicial
        val edad = edadElegida.coerceIn(EDAD_MINIMA_PERFIL, EDAD_MAXIMA_PERFIL)

        val juzgables = respuestas.filterNot { it.vector == VectorDiagnostico.EDAD }
        val total = juzgables.size
        val aciertos = juzgables.count { it.correcta }
        val precision = if (total == 0) 0.5f else aciertos.toFloat() / total
        val promedioMs = if (total == 0) 0L else juzgables.sumOf { it.tiempoMs } / total
        val esperadoMs = (9_000L - (edad - 6) * 500L).coerceIn(4_500L, 9_000L)

        val velocidad = when {
            precision >= 0.85f && promedioMs in 1L..esperadoMs -> 1.25f
            precision >= 0.65f -> 1.05f
            precision >= 0.40f -> 0.90f
            else -> 0.75f
        }.coerceIn(DifficultyProfile.MIN_MULTIPLICADOR, DifficultyProfile.MAX_MULTIPLICADOR)

        val ajusteEdad = when {
            edad <= 7 -> -1
            edad >= 10 -> 1
            else -> 0
        }
        val complejidad = (1 + (precision * 2f).roundToInt() + ajusteEdad)
            .coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO)

        return DifficultyProfile(
            multiplicadorVelocidad = velocidad,
            complejidad = complejidad,
            edadSugerida = edad,
            apoyoAudio = precision < 0.6f || promedioMs > esperadoMs * 3 / 2,
            aciertoEsperado = (0.45f + complejidad * 0.06f).coerceIn(0.45f, 0.8f),
            calibrado = true
        )
    }

    companion object {
        private const val EDAD_MINIMA_PERFIL = 6
        private const val EDAD_MAXIMA_PERFIL = 12

        fun persistir(context: Context, perfil: DifficultyProfile) {
            PerfilAdaptativoStore.obtener(context).guardarEnSegundoPlano(perfil)
        }

        fun perfilGuardado(context: Context): StateFlow<DifficultyProfile> =
            PerfilAdaptativoStore.obtener(context).perfil
    }
}

/** Orden de edades que corresponde a las opciones de la primera pregunta. */
private val EDADES_POR_OPCION = listOf(6, 8, 9, 10)

/**
 * Banco dinámico de cuatro preguntas: una por vector. La franja etaria del
 * perfil ajusta tanto la aritmética como la longitud del texto y el patrón.
 */
private fun bancoDePreguntas(edad: Int): List<PreguntaAdaptativa> {
    val franja = when {
        edad <= 7 -> 1
        edad == 8 -> 2
        edad == 9 -> 3
        else -> 4
    }
    return listOf(
        PreguntaAdaptativa(
            id = "edad_grado",
            vector = VectorDiagnostico.EDAD,
            enunciado = "¿Qué edad tienes?",
            textoApoyo = "Así Zazi elige retos del tamaño justo para ti.",
            opciones = listOf("6 o 7 años", "8 años", "9 años", "10 años o más"),
            indiceCorrecto = 0,
            esInformativa = true
        ),
        preguntaMatematica(franja),
        preguntaLectura(franja),
        preguntaVelocidad(franja)
    )
}

private fun preguntaMatematica(franja: Int): PreguntaAdaptativa = when (franja) {
    1 -> PreguntaAdaptativa(
        id = "mate_1",
        vector = VectorDiagnostico.LOGICA_MATEMATICA,
        enunciado = "Zazi junta 4 flores y luego encuentra 3 más. ¿Cuántas flores tiene?",
        opciones = listOf("6", "7", "8"),
        indiceCorrecto = 1
    )

    2 -> PreguntaAdaptativa(
        id = "mate_2",
        vector = VectorDiagnostico.LOGICA_MATEMATICA,
        enunciado = "En el cenote hay 9 peces. Se van 4. ¿Cuántos quedan?",
        opciones = listOf("4", "5", "6"),
        indiceCorrecto = 1
    )

    3 -> PreguntaAdaptativa(
        id = "mate_3",
        vector = VectorDiagnostico.LOGICA_MATEMATICA,
        enunciado = "Cada caja trae 5 semillas. ¿Cuántas semillas hay en 3 cajas?",
        opciones = listOf("10", "15", "20"),
        indiceCorrecto = 1
    )

    else -> PreguntaAdaptativa(
        id = "mate_4",
        vector = VectorDiagnostico.LOGICA_MATEMATICA,
        enunciado = "El mapa tiene 12 pasos y Zazi ya dio 7. ¿Cuántos pasos le faltan?",
        opciones = listOf("4", "5", "6"),
        indiceCorrecto = 1
    )
}

private fun preguntaLectura(franja: Int): PreguntaAdaptativa = when (franja) {
    1 -> PreguntaAdaptativa(
        id = "lectura_1",
        vector = VectorDiagnostico.COMPRENSION_LECTORA,
        enunciado = "¿Qué lleva Lula para alumbrar el camino?",
        textoApoyo = "Lula la luciérnaga lleva una lámpara pequeñita. " +
            "Cuando anochece, ilumina el sendero del bosque.",
        opciones = listOf("Una lámpara", "Una canasta", "Un sombrero"),
        indiceCorrecto = 0
    )

    2 -> PreguntaAdaptativa(
        id = "lectura_2",
        vector = VectorDiagnostico.COMPRENSION_LECTORA,
        enunciado = "¿Dónde se escondió la semilla?",
        textoApoyo = "La semilla rodó por la loma y se escondió junto a la ceiba. " +
            "Zazi la busca moviendo las hojas con cuidado.",
        opciones = listOf("Junto a la ceiba", "Dentro del cenote", "Sobre el puente"),
        indiceCorrecto = 0
    )

    3 -> PreguntaAdaptativa(
        id = "lectura_3",
        vector = VectorDiagnostico.COMPRENSION_LECTORA,
        enunciado = "¿Por qué Zazi camina despacio?",
        textoApoyo = "El piso del cenote está mojado. Zazi camina despacio para no resbalar " +
            "y así llega limpia a la fiesta del pueblo.",
        opciones = listOf("Porque está cansada", "Para no resbalar", "Porque hace frío"),
        indiceCorrecto = 1
    )

    else -> PreguntaAdaptativa(
        id = "lectura_4",
        vector = VectorDiagnostico.COMPRENSION_LECTORA,
        enunciado = "¿Qué aprende el niño del cuento?",
        textoApoyo = "El tlacuache poeta escribía un verso cada noche. " +
            "Al principio no le gustaban sus palabras, pero siguió practicando. " +
            "Con el tiempo, todo el bosque quiso escuchar sus poemas.",
        opciones = listOf(
            "Que practicar ayuda a mejorar",
            "Que escribir es aburrido",
            "Que hay que copiar a otros"
        ),
        indiceCorrecto = 0
    )
}

private fun preguntaVelocidad(franja: Int): PreguntaAdaptativa = when (franja) {
    1 -> PreguntaAdaptativa(
        id = "patron_1",
        vector = VectorDiagnostico.VELOCIDAD_COGNITIVA,
        enunciado = "¿Qué número sigue?  1, 2, 3, 4, …",
        opciones = listOf("5", "6", "7"),
        indiceCorrecto = 0
    )

    2 -> PreguntaAdaptativa(
        id = "patron_2",
        vector = VectorDiagnostico.VELOCIDAD_COGNITIVA,
        enunciado = "¿Qué número sigue?  2, 4, 6, 8, …",
        opciones = listOf("9", "10", "12"),
        indiceCorrecto = 1
    )

    3 -> PreguntaAdaptativa(
        id = "patron_3",
        vector = VectorDiagnostico.VELOCIDAD_COGNITIVA,
        enunciado = "¿Qué número sigue?  3, 6, 9, 12, …",
        opciones = listOf("14", "15", "16"),
        indiceCorrecto = 1
    )

    else -> PreguntaAdaptativa(
        id = "patron_4",
        vector = VectorDiagnostico.VELOCIDAD_COGNITIVA,
        enunciado = "¿Qué número sigue?  1, 4, 9, 16, …",
        opciones = listOf("20", "24", "25"),
        indiceCorrecto = 2
    )
}

/**
 * Persistencia reactiva del [DifficultyProfile] y puente hacia los motores.
 *
 * Además de su DataStore propio, siembra el arranque de la Arena en
 * `SharedPreferences` (`zazili_arena_dda`) con las llaves que
 * [com.example.zazili.domain.arena.ZaziDifficultyEngine] ya lee. Así los
 * minijuegos ajustan reloj de ronda, opciones y andamiaje desde la primera
 * partida, sin tocar ninguna pantalla.
 */
class PerfilAdaptativoStore private constructor(context: Context) {

    private val contexto = context.applicationContext
    private val dataStore = contexto.dataStorePerfilAdaptativo
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val perfil: StateFlow<DifficultyProfile> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs.aPerfil() }
        .stateIn(
            scope = alcance,
            started = SharingStarted.Eagerly,
            initialValue = DifficultyProfile()
        )

    suspend fun guardar(perfil: DifficultyProfile) {
        val seguro = perfil.copy(
            multiplicadorVelocidad = perfil.multiplicadorVelocidad.coerceIn(
                DifficultyProfile.MIN_MULTIPLICADOR,
                DifficultyProfile.MAX_MULTIPLICADOR
            ),
            complejidad = perfil.complejidad.coerceIn(
                Minijuego.NIVEL_MINIMO,
                Minijuego.NIVEL_MAXIMO
            )
        )
        try {
            dataStore.edit { prefs ->
                prefs[CLAVE_VELOCIDAD] = seguro.multiplicadorVelocidad
                prefs[CLAVE_COMPLEJIDAD] = seguro.complejidad
                prefs[CLAVE_EDAD] = seguro.edadSugerida
                prefs[CLAVE_APOYO_AUDIO] = seguro.apoyoAudio
                prefs[CLAVE_ACIERTO_ESPERADO] = seguro.aciertoEsperado
                prefs[CLAVE_CALIBRADO] = seguro.calibrado
                prefs[CLAVE_ACTUALIZADO] = System.currentTimeMillis()
            }
        } catch (error: IOException) {
            Log.w(TAG, "No se pudo guardar el perfil adaptativo", error)
        }
        if (seguro.calibrado) sembrarArranqueArena(seguro)
    }

    fun guardarEnSegundoPlano(perfil: DifficultyProfile) {
        alcance.launch {
            runCatching { guardar(perfil) }
                .onFailure { Log.w(TAG, "Perfil adaptativo sin guardar", it) }
        }
    }

    /** Los minijuegos pueden leer este valor sin colectar nada. */
    fun multiplicadorVelocidad(): Float = perfil.value.multiplicadorVelocidad

    fun segundosDeTemporizador(baseSegundos: Int): Int =
        perfil.value.segundosDeTemporizador(baseSegundos)

    fun nivelAdaptado(nivelBase: Int): Int = perfil.value.nivelPara(nivelBase)

    private fun sembrarArranqueArena(perfil: DifficultyProfile) {
        val prefs = runCatching {
            contexto.getSharedPreferences(ARCHIVO_METRICAS, Context.MODE_PRIVATE)
        }.getOrNull() ?: return
        val editor = prefs.edit()
        val nivelInicial = perfil.complejidad.coerceIn(
            Minijuego.NIVEL_MINIMO,
            Minijuego.NIVEL_MAXIMO
        )
        // Se toca `Minijuego.todos` como única puerta al catálogo: nunca un
        // objeto anidado antes (evita el <clinit> reentrante).
        Minijuego.todos.forEach { juego ->
            val claveNivel = "dda_${juego.id}_nivel"
            if (!prefs.contains(claveNivel)) {
                editor.putInt(claveNivel, nivelInicial)
            }
            val claveApoyo = "dda_${juego.id}_apoyo"
            if (perfil.apoyoAudio && !prefs.contains(claveApoyo)) {
                // 1 = PISTA en NivelAndamiaje.
                editor.putInt(claveApoyo, 1)
            }
        }
        editor.apply()
    }

    private fun Preferences.aPerfil(): DifficultyProfile = DifficultyProfile(
        multiplicadorVelocidad = (this[CLAVE_VELOCIDAD] ?: 1f)
            .coerceIn(DifficultyProfile.MIN_MULTIPLICADOR, DifficultyProfile.MAX_MULTIPLICADOR),
        complejidad = (this[CLAVE_COMPLEJIDAD] ?: 2)
            .coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO),
        edadSugerida = this[CLAVE_EDAD] ?: 8,
        apoyoAudio = this[CLAVE_APOYO_AUDIO] ?: true,
        aciertoEsperado = this[CLAVE_ACIERTO_ESPERADO] ?: 0.55f,
        calibrado = this[CLAVE_CALIBRADO] ?: false
    )

    companion object {
        private const val TAG = "PerfilAdaptativo"
        private val CLAVE_VELOCIDAD = floatPreferencesKey("spe_proporcional")
        private val CLAVE_COMPLEJIDAD = intPreferencesKey("nivel_complejidad")
        private val CLAVE_EDAD = intPreferencesKey("edad_detectada")
        private val CLAVE_APOYO_AUDIO = booleanPreferencesKey("apoyo_audio")
        private val CLAVE_ACIERTO_ESPERADO = floatPreferencesKey("acierto_esperado")
        private val CLAVE_CALIBRADO = booleanPreferencesKey("calibrado")
        private val CLAVE_ACTUALIZADO = longPreferencesKey("actualizado_en")

        @Volatile
        private var instancia: PerfilAdaptativoStore? = null

        fun obtener(context: Context): PerfilAdaptativoStore =
            instancia ?: synchronized(this) {
                instancia ?: PerfilAdaptativoStore(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}

/**
 * Punto de acceso global para pantallas y motores que no pueden inyectar el
 * repositorio. Instalarlo una vez (el Tablero lo hace) deja los valores
 * disponibles durante todo el proceso.
 */
object DificultadAdaptativa {

    @Volatile
    private var almacen: PerfilAdaptativoStore? = null

    fun instalar(context: Context): PerfilAdaptativoStore {
        val store = PerfilAdaptativoStore.obtener(context)
        almacen = store
        return store
    }

    fun perfilActual(): DifficultyProfile = almacen?.perfil?.value ?: DifficultyProfile()

    fun multiplicadorVelocidad(): Float = perfilActual().multiplicadorVelocidad

    fun segundosDeTemporizador(baseSegundos: Int): Int =
        perfilActual().segundosDeTemporizador(baseSegundos)

    fun milisegundosDeTemporizador(baseMs: Long): Long =
        perfilActual().milisegundosDeTemporizador(baseMs)

    fun nivelAdaptado(nivelBase: Int): Int = perfilActual().nivelPara(nivelBase)

    fun opcionesVisibles(base: Int): Int = perfilActual().opcionesPara(base)

    fun milisegundosEntreApariciones(baseMs: Long): Long =
        perfilActual().milisegundosEntreApariciones(baseMs)
}

/**
 * Flujo visual del diagnóstico (drop-in para el onboarding).
 *
 * La pantalla solo pinta [estado] y llama a [DiagnosticoAdaptativoViewModel];
 * la decisión de avanzar y el cálculo del perfil viven en el motor. El botón
 * queda deshabilitado hasta que hay una selección y, al pulsarlo, el estado
 * emite una pregunta nueva sin bloqueos de hilo.
 */
@Composable
fun AdaptiveDiagnosticFlow(
    nombre: String,
    edad: Int,
    onCompletar: (DifficultyProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val fabrica = remember(edad) {
        viewModelFactory {
            initializer { DiagnosticoAdaptativoViewModel(edad) }
        }
    }
    val diagnostico: DiagnosticoAdaptativoViewModel = viewModel(factory = fabrica)
    val estado by diagnostico.estado.collectAsStateWithLifecycle()
    val completarActual by rememberUpdatedState(onCompletar)
    val contexto = LocalContext.current
    val scroll = rememberScrollState()
    val perfil = estado.perfil

    LaunchedEffect(perfil) {
        val listo = perfil ?: return@LaunchedEffect
        runCatching { PerfilAdaptativoStore.obtener(contexto).guardar(listo) }
        DificultadAdaptativa.instalar(contexto)
        completarActual(listo)
    }

    LaunchedEffect(estado.indicePregunta) {
        scroll.animateScrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scroll)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        EncabezadoAdaptativo(nombre = nombre, avance = estado.avance)

        if (perfil != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MagicalPurple)
            }
            return@Column
        }

        val pregunta = estado.preguntaActual ?: return@Column

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = pregunta.vector.etiqueta.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MagicalPurple,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
            Text(
                text = pregunta.enunciado,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black
            )
            pregunta.textoApoyo?.let { apoyo ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ZaziliCreamSoft,
                    border = BorderStroke(1.dp, LavandaMedia.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = apoyo,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = UniversoProfundo,
                        lineHeight = 24.sp
                    )
                }
            }

            pregunta.opciones.forEachIndexed { indice, texto ->
                val elegida = estado.seleccion == indice
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (elegida) AmarilloSuave else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (elegida) AmarilloVibrante else LavandaMedia.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) {
                            GlobalAudioManager.playPop()
                            diagnostico.responder(indice)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = texto,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (elegida) FontWeight.Black else FontWeight.SemiBold
                        )
                        if (elegida) {
                            Spacer(Modifier.width(8.dp))
                            KawaiiCheckIcon(size = 22.dp, color = VerdeDuo)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            ZaziButton(
                text = if (estado.esUltima) "Ver mi perfil" else "Siguiente",
                onClick = {
                    GlobalAudioManager.playPop()
                    diagnostico.siguiente()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                enabled = estado.puedeAvanzar,
                cornerRadius = 22.dp,
                baseHeight = 6.dp,
                fontSize = 18.sp
            )
            Text(
                text = "Toca una respuesta para continuar. No hay reloj.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EncabezadoAdaptativo(nombre: String, avance: Float) {
    val saludo = if (nombre.isBlank()) "¡Hola!" else "¡Hola, ${nombre.trim()}!"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(MagicalPurple, UniversoProfundo)))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EL VUELO DE PRUEBA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintGreen,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = saludo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZaziliWhite,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Cuatro retos cortos. Sin calificaciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZaziliWhite.copy(alpha = 0.78f)
                )
            }
            Spacer(Modifier.width(8.dp))
            ZaziSpriteAnimado(
                size = 70.dp,
                estado = ZaziState.VUELO_IDLE,
                animar = true,
                descripcion = "Zazi te acompaña en el vuelo"
            )
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { avance.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = AmarilloVibrante,
            trackColor = ZaziliWhite.copy(alpha = 0.22f)
        )
    }
}

private class DiagnosticoAdaptativoViewModel(edad: Int) : ViewModel() {
    private val motor = AdaptiveDiagnosticEngine(edadInicial = edad)
    val estado = motor.estado

    fun responder(opcion: Int) = motor.responder(opcion)
    fun siguiente() = motor.siguiente()
}
