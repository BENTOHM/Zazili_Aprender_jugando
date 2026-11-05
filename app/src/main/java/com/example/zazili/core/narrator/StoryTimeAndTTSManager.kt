package com.example.zazili.core.narrator

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiVolumeDownIcon
import com.example.zazili.core.ui.KawaiiVolumeUpIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.presentation.cuentos.StoryDetailScreen
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.VerdeDuo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---------------------------------------------------------------------------
//  LECTURA AUTOMÁTICA CON INTERRUPTOR PERSISTIDO
// ---------------------------------------------------------------------------

/**
 * Lectura en voz alta de cuentos y lecciones con la voz neural de Zazi.
 *
 * Recuerda si el niño dejó el altavoz encendido: el interruptor vive en
 * SharedPreferences y sobrevive a reinicios, así una lección o un cuento se
 * leen solos sin volver a tocar el ícono. Toda la síntesis la hace
 * [ZaziOnnxVoiceEngine] (Piper/ONNX): no existe voz del sistema ni un segundo
 * motor que pueda pronunciar la misma frase al mismo tiempo.
 */
object TTSManager {

    private const val TAG = "ZaziliTTSManager"
    private const val PREFS = "zazili_tts"
    private const val CLAVE_ACTIVO = "lectura_automatica"

    private val _activo = MutableStateFlow(false)

    /** true cuando la lectura automática está encendida. */
    val activo: StateFlow<Boolean> = _activo.asStateFlow()

    private var prefs: SharedPreferences? = null

    @Volatile
    private var inicializado = false

    /** Recupera el interruptor guardado y enlaza la voz neural. Idempotente. */
    @Synchronized
    fun inicializar(context: Context) {
        if (inicializado) return
        inicializado = true
        val aplicacion = context.applicationContext
        prefs = runCatching {
            aplicacion.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }.onFailure {
            Log.w(TAG, "No se pudo leer el interruptor de voz; queda apagado", it)
        }.getOrNull()
        _activo.value = prefs?.getBoolean(CLAVE_ACTIVO, false) ?: false
        ZaziOnnxVoiceEngine.initialize(aplicacion)
    }

    /**
     * Enciende o apaga la lectura automática. Devuelve el estado nuevo para que
     * la UI pueda animar el ícono sin releer el flujo.
     */
    fun alternar(context: Context): Boolean {
        inicializar(context)
        val nuevo = !_activo.value
        establecerActivo(context, nuevo)
        return nuevo
    }

    fun establecerActivo(context: Context, valor: Boolean) {
        inicializar(context)
        _activo.value = valor
        runCatching {
            prefs?.edit()?.putBoolean(CLAVE_ACTIVO, valor)?.apply()
        }.onFailure { Log.w(TAG, "No se pudo guardar el interruptor de voz", it) }
        if (!valor) detener()
    }

    /**
     * Pronuncia [texto] de inmediato si el interruptor está encendido. La voz
     * neural interrumpe la lectura anterior y, si la sesión aún se está
     * cargando, el texto queda en espera unos segundos.
     */
    fun leer(texto: String) {
        if (!_activo.value) return
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        if (!inicializado) {
            Log.w(TAG, "TTSManager sin inicializar; se ignora la lectura")
            return
        }
        ZaziOnnxVoiceEngine.speak(limpio)
    }

    /** Corta la lectura actual y descarta cualquier texto en espera. */
    fun detener() {
        ZaziOnnxVoiceEngine.stop()
    }

    /** Corta la voz al cerrar la pantalla; la sesión ONNX se comparte. */
    fun liberar() {
        ZaziOnnxVoiceEngine.stop()
    }
}

/**
 * Botón flotante redondo para el interruptor de voz. El ícono cambia de
 * altavoz con ondas (encendido) a altavoz con una sola onda (apagado) y el
 * fondo verde confirma visualmente que la lectura está activa.
 */
@Composable
fun BotonFlotanteTTS(
    activo: Boolean,
    onAlternar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val descripcion = if (activo) {
        "Desactivar lectura en voz alta"
    } else {
        "Activar lectura en voz alta"
    }
    Surface(
        shape = CircleShape,
        color = if (activo) VerdeDuo else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            2.dp,
            if (activo) VerdeDuo else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        ),
        shadowElevation = 6.dp,
        modifier = modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClickLabel = descripcion) { onAlternar() }
            .semantics { contentDescription = descripcion }
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (activo) {
                KawaiiVolumeUpIcon(size = 24.dp, modifier = Modifier)
            } else {
                KawaiiVolumeDownIcon(size = 24.dp, modifier = Modifier)
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PANTALLA DEL CUENTACUENTOS
// ---------------------------------------------------------------------------

/**
 * Biblioteca de treinta cuentos de cinco países, con lectura en voz alta
 * opcional.
 *
 * La lista vive en esta pantalla; al abrir una tarjeta se muestra
 * [StoryDetailScreen], que se encarga de la música propia de la historia y de
 * devolver la melodía principal al salir. El interruptor flotante 🔊 vive en
 * la esquina superior derecha en ambas vistas.
 */
@Composable
fun StoryTimeScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val zen = collectZenMode()
    val ttsActivo by TTSManager.activo.collectAsStateWithLifecycle()
    var idAbierto by rememberSaveable { mutableStateOf<String?>(null) }
    val historiaAbierta = idAbierto?.let(StoryRepository::porId)

    LaunchedEffect(Unit) {
        TTSManager.inicializar(contexto)
    }
    DisposableEffect(Unit) {
        onDispose { TTSManager.detener() }
    }

    val fondoBase = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }
    val fondo = if (zen) {
        Brush.verticalGradient(listOf(fondoBase, fondoBase))
    } else {
        Brush.verticalGradient(
            listOf(fondoBase, LavandaClara.copy(alpha = 0.55f))
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        if (historiaAbierta == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                BarraCuentacuentos(
                    onVolver = {
                        GlobalAudioManager.playPop()
                        onSalir()
                    }
                )

                Box(modifier = Modifier.weight(1f)) {
                    ListaCuentos(
                        onElegir = { historia ->
                            GlobalAudioManager.playPop()
                            idAbierto = historia.id
                        }
                    )
                }
            }

            BotonFlotanteTTS(
                activo = ttsActivo,
                onAlternar = {
                    GlobalAudioManager.playPop()
                    TTSManager.alternar(contexto)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 16.dp)
            )
        } else {
            StoryDetailScreen(
                historia = historiaAbierta,
                ttsActivo = ttsActivo,
                onAlternarTts = { TTSManager.alternar(contexto) },
                onVolver = { idAbierto = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BarraCuentacuentos(onVolver: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 72.dp, top = 8.dp, bottom = 8.dp)
    ) {
        ZaziButton(
            text = "‹",
            onClick = onVolver,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Volver" },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 12.dp,
            baseHeight = 4.dp,
            fontSize = 26.sp
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cuentacuentos",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = "Treinta historias de cinco países",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ListaCuentos(onElegir: (HistoriaCuentacuentos) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KawaiiZazi(size = 74.dp, descripcion = "Zazi te espera para leer")
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Zazi te cuenta treinta historias de México, España, Perú, " +
                        "República Dominicana y Brasil. Toca una tarjeta y enciende el " +
                        "altavoz si quieres escucharla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(StoryRepository.todas, key = { it.id }) { historia ->
            TarjetaCuento(historia = historia, onClick = { onElegir(historia) })
        }
    }
}

@Composable
private fun TarjetaCuento(
    historia: HistoriaCuentacuentos,
    onClick: () -> Unit
) {
    val colorAcento = when {
        historia.origen.contains("México") -> VerdeDuo
        historia.origen.contains("España") -> AmarilloVibrante
        historia.origen.contains("Perú") -> AzulCielo
        historia.origen.contains("Dominicana") -> RosaSuave
        historia.origen.contains("Brasil") -> MintGreen
        else -> LavandaMedia
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "Leer ${historia.titulo}"
            ) { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(colorAcento.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = historia.emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historia.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = historia.origen,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = historia.resumen,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            KawaiiArrowIcon(
                size = 20.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}
