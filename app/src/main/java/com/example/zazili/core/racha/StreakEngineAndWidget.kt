package com.example.zazili.core.racha

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment as AlineacionGlance
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight as PesoGlance
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.zazili.core.logros.AchievementEngine
import com.example.zazili.core.logros.EventoLogro
import com.example.zazili.core.mascot.EventoZazi
import com.example.zazili.core.mascot.ZaziPetEngine
import com.example.zazili.core.notificaciones.NotificacionesZazili
import com.example.zazili.domain.economia.ZazolEconomyManager
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.UniversoProfundo
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStoreRacha: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_racha"
)

/** Precio de un Protector de Racha extra en la Boutique de la Racha. */
const val PRECIO_PROTECTOR_RACHA = 75

/** Hora local en que se dispara el recordatorio diario (formato 24 h). */
private const val HORA_RECORDATORIO = 17

/** Número de Protectores de Racha gratis con los que se inicia la aventura. */
private const val PROTECTORES_INICIALES = 2

/** Resultado de intentar comprar un Protector de Racha. */
sealed interface ResultadoProtector {
    data object Exito : ResultadoProtector
    data class SinFondos(val faltan: Int) : ResultadoProtector
    data object Error : ResultadoProtector
}

/**
 * Los cuatro estados visuales del widget de racha. Cada uno tiene su propia
 * imagen en `drawable-nodpi` (el usuario puede reemplazarlas sin tocar código):
 *
 *  - [INACTIVA] `widget_racha_inactiva`: aún no juega hoy (mañana/tarde).
 *  - [ACTIVA] `widget_racha_activa`: ya hubo lección o minijuego hoy.
 *  - [PELIGRO] `widget_racha_peligro`: pasaron las 5 PM y no ha jugado.
 *  - [CONGELADA] `widget_racha_congelada`: un protector salvó la racha hoy.
 */
enum class EstadoWidgetRacha(
    val nombreDrawable: String,
    val emojiRespaldo: String,
    val etiqueta: String,
    val descripcion: String
) {
    INACTIVA(
        "widget_racha_inactiva",
        "🕯️",
        "¡A jugar!",
        "Tu racha te espera. Juega hoy para encenderla."
    ),
    ACTIVA(
        "widget_racha_activa",
        "🔥",
        "Racha viva",
        "¡Racha encendida! Ya jugaste hoy."
    ),
    PELIGRO(
        "widget_racha_peligro",
        "🥺",
        "¡En peligro!",
        "Pasaron las 5 de la tarde y todavía no juegas. ¡Tu racha está en peligro!"
    ),
    CONGELADA(
        "widget_racha_congelada",
        "🧊",
        "Protegida",
        "Un protector de racha salvó tu progreso. ¡Sigue jugando!"
    )
}

/**
 * Estado observable de la racha diaria estilo Duolingo.
 *
 * @property currentStreak días seguidos jugando (sin contar hoy hasta jugar).
 * @property longestStreak récord histórico de días seguidos.
 * @property freezes Protectores de Racha disponibles; uno se consume cuando
 *   se pierde un día y evita que la racha se reinicie.
 * @property jugoHoy true si ya hubo una lección o un minijuego hoy.
 * @property rachaViva true si la racha sigue viva (jugó hoy o ayer, o tiene
 *   protectores que la sostienen).
 * @property fechasRecientes fechas recientes con actividad ("aaaa-MM-dd"),
 *   usadas por el calendario semanal de la hoja y por el widget.
 * @property estadoWidget estado de las cuatro láminas del widget.
 */
data class EstadoRacha(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val freezes: Int = 0,
    val jugoHoy: Boolean = false,
    val rachaViva: Boolean = false,
    val fechasRecientes: Set<String> = emptySet(),
    val estadoWidget: EstadoWidgetRacha = EstadoWidgetRacha.INACTIVA,
    val listo: Boolean = false
)

/**
 * Motor de racha diaria + widget Glance + recordatorios WorkManager.
 *
 * Persistencia en DataStore (`zazili_racha`): racha actual, récord,
 * protectores, última fecha jugada, fecha en que se usó un protector y las
 * fechas recientes. La primera vez que arranca regala [PROTECTORES_INICIALES].
 *
 * REGLA DE ACTIVACIÓN: [registrarJuegoHoy] solo debe invocarse desde
 * [AchievementEngine.notifyAction] cuando la acción sea `LESSON_COMPLETE` o
 * `MINIGAME_WON`. Abrir la app, tocar a Zazi o pasear por el menú NO enciende
 * la racha: solo completar una lección o ganar un minijuego.
 */
class RachaEngine private constructor(context: Context) {

    private val app = context.applicationContext
    private val dataStore = app.dataStoreRacha
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _avisos = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val avisos: SharedFlow<String> = _avisos.asSharedFlow()

    val estado: StateFlow<EstadoRacha> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            val hoy = LocalDate.now().toString()
            val ayer = LocalDate.now().minusDays(1).toString()
            val ultima = prefs[CLAVE_ULTIMA_FECHA]
            val racha = prefs[CLAVE_RACHA_ACTUAL] ?: 0
            val protectores = prefs[CLAVE_PROTECTORES] ?: 0
            val jugoHoy = ultima == hoy
            val congeladaHoy = prefs[CLAVE_FECHA_PROTECTOR] == hoy
            EstadoRacha(
                currentStreak = racha,
                longestStreak = prefs[CLAVE_RACHA_MAXIMA] ?: 0,
                freezes = protectores,
                jugoHoy = jugoHoy,
                rachaViva = racha > 0 && (jugoHoy || ultima == ayer || protectores > 0),
                fechasRecientes = prefs[CLAVE_FECHAS] ?: emptySet(),
                estadoWidget = calcularEstadoWidget(
                    jugoHoy = jugoHoy,
                    congeladaHoy = congeladaHoy,
                    horaLocal = LocalTime.now().hour
                ),
                listo = true
            )
        }
        .stateIn(alcance, kotlinx.coroutines.flow.SharingStarted.Eagerly, EstadoRacha())

    /** Garantiza el regalo inicial de protectores. Se llama desde la UI. */
    suspend fun garantizarBonoInicial() {
        try {
            dataStore.edit { prefs ->
                if (prefs[CLAVE_BONO_INICIAL] != true) {
                    prefs[CLAVE_BONO_INICIAL] = true
                    prefs[CLAVE_PROTECTORES] =
                        (prefs[CLAVE_PROTECTORES] ?: 0) + PROTECTORES_INICIALES
                }
            }
        } catch (error: IOException) {
            Log.w(ETIQUETA, "No se pudo entregar el bono de protectores", error)
        }
    }

    /**
     * Marca la actividad de hoy. Devuelve true si hoy se encendió la flama por
     * primera vez (para celebrar).
     *
     * Solo lo llama [AchievementEngine.notifyAction] tras una lección
     * completada o un minijuego ganado. Es idempotente por día.
     */
    suspend fun registrarJuegoHoy(): Boolean {
        var encendioHoy = false
        var congeloHoy = false
        try {
            dataStore.edit { prefs ->
                val hoy = LocalDate.now()
                val hoyTexto = hoy.toString()
                val ultimaTexto = prefs[CLAVE_ULTIMA_FECHA]

                if (ultimaTexto == hoyTexto) {
                    encendioHoy = false
                } else {
                    encendioHoy = true
                    val rachaAnterior = prefs[CLAVE_RACHA_ACTUAL] ?: 0
                    val ayerTexto = hoy.minusDays(1).toString()
                    val rachaNueva = when {
                        ultimaTexto == null -> 1
                        ultimaTexto == ayerTexto -> rachaAnterior + 1
                        else -> {
                            // Día(s) saltados: un protector salva la racha.
                            val protectores = prefs[CLAVE_PROTECTORES] ?: 0
                            if (protectores > 0) {
                                prefs[CLAVE_PROTECTORES] = protectores - 1
                                prefs[CLAVE_PROTECTOR_USADO] = true
                                prefs[CLAVE_FECHA_PROTECTOR] = hoyTexto
                                congeloHoy = true
                                rachaAnterior.coerceAtLeast(1)
                            } else {
                                1
                            }
                        }
                    }
                    prefs[CLAVE_RACHA_ACTUAL] = rachaNueva
                    val maxima = prefs[CLAVE_RACHA_MAXIMA] ?: 0
                    if (rachaNueva > maxima) prefs[CLAVE_RACHA_MAXIMA] = rachaNueva
                    prefs[CLAVE_ULTIMA_FECHA] = hoyTexto
                    val fechas = (prefs[CLAVE_FECHAS] ?: emptySet()).toMutableSet()
                    fechas += hoyTexto
                    // Conserva solo las últimas 3 semanas.
                    val limite = hoy.minusDays(21).toString()
                    prefs[CLAVE_FECHAS] = fechas.filterTo(HashSet()) { it >= limite }
                }
            }
        } catch (error: IOException) {
            Log.w(ETIQUETA, "No se pudo registrar la actividad de hoy", error)
            return false
        }

        val actual = estado.value
        runCatching {
            AchievementEngine.obtener(app).notificar(EventoLogro.RACHA_DIAS, actual.currentStreak)
            if (dataStore.data.first()[CLAVE_PROTECTOR_USADO] == true) {
                AchievementEngine.obtener(app).notificar(EventoLogro.PROTECTOR_USADO)
                dataStore.edit { it.remove(CLAVE_PROTECTOR_USADO) }
            }
        }.onFailure { Log.w(ETIQUETA, "No se pudo avisar a los logros", it) }

        if (congeloHoy) {
            RecordatoriosZazili.notificarCongelada(app)
            _avisos.tryEmit(ZaziPetEngine.frase(EventoZazi.PROTECTOR_USADO))
        }

        runCatching { RachaWidget().updateAll(app) }
        return encendioHoy
    }

    /** Compra un Protector de Racha con Zazoles; el saldo nunca queda negativo. */
    suspend fun comprarProtector(): ResultadoProtector {
        val economia = ZazolEconomyManager.obtener(app)
        if (!economia.deductZazoles(PRECIO_PROTECTOR_RACHA)) {
            val saldo = economia.estado.value.zazoles
            return ResultadoProtector.SinFondos(faltan = PRECIO_PROTECTOR_RACHA - saldo)
        }
        return try {
            dataStore.edit { prefs ->
                prefs[CLAVE_PROTECTORES] = (prefs[CLAVE_PROTECTORES] ?: 0) + 1
            }
            AchievementEngine.obtener(app).notificar(EventoLogro.PROTECTOR_COMPRADO)
            ResultadoProtector.Exito
        } catch (error: IOException) {
            Log.w(ETIQUETA, "No se pudo guardar el protector comprado", error)
            economia.addZazoles(PRECIO_PROTECTOR_RACHA)
            ResultadoProtector.Error
        }
    }

    /**
     * Programa el recordatorio diario de las 5 PM con WorkManager. Si ya hay
     * una cadencia encolada, la sustituye con la versión nueva (UPDATE).
     */
    fun programarRecordatorios() {
        runCatching {
            val ahora = LocalDateTime.now()
            var objetivo = ahora.withHour(HORA_RECORDATORIO).withMinute(0).withSecond(0)
            if (!objetivo.isAfter(ahora)) objetivo = objetivo.plusDays(1)
            val demoraMs = ChronoUnit.MILLIS.between(ahora, objetivo)

            val solicitud = PeriodicWorkRequestBuilder<RecordatorioRachaWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(demoraMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(app).enqueueUniquePeriodicWork(
                NOMBRE_TRABAJO,
                ExistingPeriodicWorkPolicy.UPDATE,
                solicitud
            )
        }.onFailure { Log.w(ETIQUETA, "No se pudieron programar los recordatorios", it) }
    }

    companion object {
        private const val ETIQUETA = "RachaZazili"
        private const val NOMBRE_TRABAJO = "recordatorio_racha_diario"

        private val CLAVE_RACHA_ACTUAL = intPreferencesKey("racha_actual")
        private val CLAVE_RACHA_MAXIMA = intPreferencesKey("racha_maxima")
        private val CLAVE_PROTECTORES = intPreferencesKey("protectores_racha")
        private val CLAVE_ULTIMA_FECHA = stringPreferencesKey("ultima_fecha_juego")
        private val CLAVE_FECHA_PROTECTOR = stringPreferencesKey("fecha_protector_usado")
        private val CLAVE_FECHAS = stringSetPreferencesKey("fechas_juego")
        private val CLAVE_BONO_INICIAL = booleanPreferencesKey("bono_protectores_listo")
        private val CLAVE_PROTECTOR_USADO = booleanPreferencesKey("protector_usado_pendiente")

        @Volatile
        private var instancia: RachaEngine? = null

        fun obtener(context: Context): RachaEngine =
            instancia ?: synchronized(this) {
                instancia ?: RachaEngine(context.applicationContext).also { instancia = it }
            }

        /**
         * Regla única de los cuatro estados. Se calcula al leer la racha y al
         * refrescar el widget, así que el worker de las 5 PM lo ve al día.
         */
        private fun calcularEstadoWidget(
            jugoHoy: Boolean,
            congeladaHoy: Boolean,
            horaLocal: Int
        ): EstadoWidgetRacha = when {
            congeladaHoy -> EstadoWidgetRacha.CONGELADA
            jugoHoy -> EstadoWidgetRacha.ACTIVA
            horaLocal >= HORA_RECORDATORIO -> EstadoWidgetRacha.PELIGRO
            else -> EstadoWidgetRacha.INACTIVA
        }
    }
}

// ---------------------------------------------------------------------------
//  WORKER DE RECORDATORIOS
// ---------------------------------------------------------------------------

/**
 * Revisión diaria de las 5 PM: si el niño aún no juega, Zazi manda el aviso de
 * peligro exacto y el widget se repinta con su lámina correspondiente.
 */
class RecordatorioRachaWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val engine = RachaEngine.obtener(applicationContext)
        engine.garantizarBonoInicial()
        val estado = engine.estado.first()
        if (estado.estadoWidget == EstadoWidgetRacha.PELIGRO) {
            RecordatoriosZazili.notificarPeligro(applicationContext)
        }
        RachaWidget().updateAll(applicationContext)
        Result.success()
    }.getOrElse {
        Log.w("RecordatorioRacha", "Reintento del recordatorio", it)
        Result.retry()
    }
}

/** Textos y disparos de los avisos de racha. */
object RecordatoriosZazili {

    private const val ID_AVISO_PELIGRO = 2026
    private const val ID_AVISO_CONGELADA = 2028

    private const val TITULO_PELIGRO = "¡Zazi está a punto de llorar!"
    private const val MENSAJE_PELIGRO =
        "¡Zazi está a punto de llorar! Tu racha está en peligro. 🥺"

    private const val TITULO_CONGELADA = "¡Racha protegida!"
    private const val MENSAJE_CONGELADA =
        "¡Uf! Un protector de racha salvó tu progreso de quedar en cero."

    /** Canal alto compartido: lo crea [NotificacionesZazili] una sola vez. */
    fun asegurarCanal(context: Context) {
        NotificacionesZazili.asegurarCanal(context)
    }

    /** Aviso de las 5 PM: la racha corre peligro y Zazi está tristón. */
    fun notificarPeligro(context: Context): Boolean =
        NotificacionesZazili.mostrar(
            context,
            TITULO_PELIGRO,
            MENSAJE_PELIGRO,
            ID_AVISO_PELIGRO
        )

    /** Aviso inmediato cuando se consume un Protector de Racha. */
    fun notificarCongelada(context: Context): Boolean =
        NotificacionesZazili.mostrar(
            context,
            TITULO_CONGELADA,
            MENSAJE_CONGELADA,
            ID_AVISO_CONGELADA
        )
}

// ---------------------------------------------------------------------------
//  WIDGET GLANCE: CUATRO LÁMINAS
// ---------------------------------------------------------------------------

/**
 * Widget de inicio con una lámina por estado. Las imágenes se buscan por
 * nombre en `drawable-nodpi`, así el artista puede soltar sus PNG
 * (`widget_racha_inactiva`, `widget_racha_activa`, `widget_racha_peligro`,
 * `widget_racha_congelada`) sin recompilar código ni tocar el layout. Si una
 * lámina todavía no existe, el widget cae en su emoji y sigue funcionando.
 */
class RachaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val estado = RachaEngine.obtener(context).estado.first()
        val imagen = recursoDrawable(context, estado.estadoWidget.nombreDrawable)
        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(UniversoProfundo)
                        .padding(8.dp),
                    contentAlignment = AlineacionGlance.Center
                ) {
                    Column(
                        horizontalAlignment = AlineacionGlance.CenterHorizontally,
                        verticalAlignment = AlineacionGlance.CenterVertically
                    ) {
                        if (imagen != 0) {
                            Image(
                                provider = ImageProvider(imagen),
                                contentDescription = estado.estadoWidget.descripcion,
                                modifier = GlanceModifier.size(64.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = estado.estadoWidget.emojiRespaldo,
                                style = TextStyle(fontSize = 32.sp)
                            )
                        }
                        Text(
                            text = "${estado.currentStreak}",
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = PesoGlance.Bold,
                                color = ColorProvider(
                                    if (estado.jugoHoy) AmarilloVibrante else LavandaMedia
                                )
                            )
                        )
                        Text(
                            text = estado.estadoWidget.etiqueta,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = ColorProvider(LavandaClara)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun recursoDrawable(contexto: Context, nombre: String): Int =
        contexto.resources.getIdentifier(nombre, "drawable", contexto.packageName)
}

/**
 * Receptor del widget de racha. Se declara en el AndroidManifest con el
 * nombre exacto `.core.racha.StreakWidgetReceiver`; [WidgetPinHelper] usa ese
 * mismo ComponentName para fijarlo desde la app.
 */
class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RachaWidget()
}

// ---------------------------------------------------------------------------
//  FLAMA PIXEL COMPARTIDA (tablero, hoja y tutorial del widget)
// ---------------------------------------------------------------------------

/**
 * Flama dibujada como pixel art real: una rejilla fija de 9×11 celdas pintada
 * con rectángulos duros. No depende de ningún PNG, así que se ve nítida a
 * cualquier tamaño y comparte silueta con la lámina del widget.
 *
 * @param activa enciende el naranja/amarillo de racha viva; apagada usa lavanda
 *   para comunicar "todavía no juegas hoy" sin alarmar.
 */
@Composable
fun IconoFlamaPixel(
    activa: Boolean,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val exterior = if (activa) CoralAviso else LavandaMedia
    val medio = if (activa) AmarilloVibrante else LavandaClara
    val nucleo = if (activa) AmarilloSuave else Color.White.copy(alpha = 0.85f)
    val brasa = if (activa) UniversoProfundo.copy(alpha = 0.18f) else Color.Transparent

    Canvas(modifier = modifier.size(size)) {
        val columnas = FLAMA_REJILLA.first().length
        val filas = FLAMA_REJILLA.size
        val celdaAncho = this.size.width / columnas
        val celdaAlto = this.size.height / filas

        FLAMA_REJILLA.forEachIndexed { fila, renglon ->
            renglon.forEachIndexed { columna, simbolo ->
                val color = when (simbolo) {
                    '1' -> exterior
                    '2' -> medio
                    '3' -> nucleo
                    'b' -> brasa
                    else -> null
                } ?: return@forEachIndexed
                drawRect(
                    color = color,
                    topLeft = Offset(columna * celdaAncho, fila * celdaAlto),
                    size = Size(celdaAncho + 0.6f, celdaAlto + 0.6f)
                )
            }
        }
    }
}

/**
 * Silueta de la flama en pixel art. `1` = borde, `2` = fuego medio,
 * `3` = núcleo brillante y `b` = brasa tenue bajo la llama.
 */
private val FLAMA_REJILLA = listOf(
    "....1....",
    "...111...",
    "...121...",
    "..11211..",
    "..11211..",
    ".1112111.",
    ".1123211.",
    ".1123211.",
    "..12321..",
    "..bbbbb.."
)

