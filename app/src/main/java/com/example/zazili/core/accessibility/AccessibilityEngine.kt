package com.example.zazili.core.accessibility

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.ZaziOnnxVoiceEngine
import com.example.zazili.core.ui.KawaiiMoonIcon
import com.example.zazili.core.ui.KawaiiVolumeUpIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---------------------------------------------------------------------------
//  MODO ZEN (motor DUA): estado global, fuera de la composición.
// ---------------------------------------------------------------------------

/**
 * Estado global del Modo Zen, persistido en SharedPreferences y expuesto como
 * [StateFlow]. Vive en [com.example.zazili.di.AppContainer] y se publica a la
 * jerarquía de Compose mediante [LocalZenMode].
 *
 * Cuando está activo:
 *  - se silencian las animaciones ambientales/secundarias (flotaciones,
 *    brillos, confeti repetitivo) en las pantallas que lo consulten;
 *  - el fondo pasa a un tono oscuro, apagado y de bajo contraste.
 *
 * Pensado para niños con sensibilidad sensorial (TEA/TPHA).
 */
class ZenModeManager(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure {
        Log.w(TAG, "Modo Zen no disponible; se usará desactivado", it)
    }.getOrNull()

    private val _enabled = MutableStateFlow(load())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        runCatching { prefs?.edit()?.putBoolean(KEY_ENABLED, value)?.apply() }
            .onFailure { Log.w(TAG, "No se pudo guardar el Modo Zen", it) }
    }

    fun toggle() = setEnabled(!enabled.value)

    private fun load(): Boolean =
        runCatching { prefs?.getBoolean(KEY_ENABLED, false) ?: false }.getOrDefault(false)

    private companion object {
        const val TAG = "ZaziliZen"
        const val PREFS_NAME = "zazili_zen"
        const val KEY_ENABLED = "zen_enabled"
    }
}

/** Canal de Compose para el Modo Zen; nulo = la pantalla ignora el modo. */
val LocalZenMode = staticCompositionLocalOf<ZenModeManager?> { null }

/** Lee (y observa) el Modo Zen actual; por defecto es falso. */
@Composable
fun collectZenMode(): Boolean =
    LocalZenMode.current?.enabled?.collectAsStateWithLifecycle()?.value ?: false

/** Fondo "zen": oscuro, apagado y de bajo contraste, para cada tema. */
fun zenMutedBackground(isDark: Boolean): Color =
    if (isDark) Color(0xFF101318) else Color(0xFFE9E4F0)

/** Superficie "zen" ligeramente más clara que el fondo, misma regla. */
fun zenMutedSurface(isDark: Boolean): Color =
    if (isDark) Color(0xFF181C24) else Color(0xFFDFD9EA)

/** Elije la variante zen según la luminancia del tema actual. */
fun zenVariantFor(background: Color): Boolean = background.luminance() < 0.5f

// ---------------------------------------------------------------------------
//  INTERRUPTOR DEL MODO ZEN.
// ---------------------------------------------------------------------------

/**
 * Tarjeta con el interruptor global del Modo Zen. Colócala en cualquier
 * pantalla: escribe directamente sobre el estado persistido.
 */
@Composable
fun ZenModeToggle(modifier: Modifier = Modifier) {
    val manager = LocalZenMode.current ?: return
    val enabled by manager.enabled.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(24.dp)

    Surface(
        shape = shape,
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            KawaiiMoonIcon(size = 34.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Modo Zen",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Menos movimiento y colores suaves para calmar los sentidos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    manager.setEnabled(it)
                    GlobalAudioManager.playPop()
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  BIONIC TEXT (motor DUA): anclas visuales para lectores con dislexia/TPHA.
// ---------------------------------------------------------------------------

/** Proporción de cada palabra que se resalta en negrita extra. */
private const val BIONIC_BOLD_FRACTION = 0.4f

/**
 * Texto biónico: la primera parte de CADA palabra se dibuja con
 * [FontWeight.ExtraBold] y el resto con peso normal. Esos tramos oscuros
 * funcionan como anclas visuales que estabilizan la mirada y reducen el
 * esfuerzo de lectura (dislexia, TDAH).
 */
@Composable
fun BionicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified
) {
    val annotated = remember(text) { buildBionicAnnotated(text) }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color
    )
}

/** Construye el [AnnotatedString] biónico conservando espacios y signos. */
fun buildBionicAnnotated(text: String): AnnotatedString = buildAnnotatedString {
    val wordRegex = Regex("\\S+")
    var cursor = 0
    for (match in wordRegex.findAll(text)) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        val word = match.value
        val boldChars = (
            kotlin.math.ceil(word.length * BIONIC_BOLD_FRACTION).toInt()
                .coerceAtLeast(1)
            ).coerceAtMost(word.length)
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
            append(word.substring(0, boldChars))
        }
        append(word.substring(boldChars))
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}

// ---------------------------------------------------------------------------
//  NARRADOR DE APOYO (DUA): lectura en voz alta bajo demanda.
// ---------------------------------------------------------------------------

/**
 * Puerta de entrada del altavoz de apoyo sobre la voz neural local.
 *
 * Se usa SOLO para el botón de altavoz: el niño pide escuchar un enunciado o
 * una idea de teoría cuando lo necesita. La síntesis real vive en
 * [ZaziOnnxVoiceEngine] (Piper/ONNX), la misma que narra cuentos, lecciones y
 * el chat, así que nunca conviven dos motores ni dos voces distintas.
 *
 * No hay motor nativo ni inicialización propia: [inicializar] solo registra el
 * contexto de aplicación y [detener] se engancha al ciclo de vida de cada
 * pantalla para que el audio no siga sonando al salir.
 */
object NarradorNativo {

    /** Registra el contexto de aplicación. Idempotente. */
    fun inicializar(context: Context) {
        ZaziOnnxVoiceEngine.initialize(context.applicationContext)
    }

    /** Lee [texto] de inmediato, interrumpiendo cualquier lectura anterior. */
    fun hablar(texto: String) {
        ZaziOnnxVoiceEngine.speak(texto)
    }

    /** Corta la lectura actual y descarta cualquier texto en espera. */
    fun detener() {
        ZaziOnnxVoiceEngine.stop()
    }

    /** Corta la voz al cerrar la app; la sesión ONNX se libera aparte. */
    fun liberar() {
        ZaziOnnxVoiceEngine.stop()
    }
}

// ---------------------------------------------------------------------------
//  BOTÓN DE ALTAVOZ (motor DUA): lectura bajo demanda junto a cada texto.
// ---------------------------------------------------------------------------

/**
 * Altavoz pequeño y pulsable que lee [texto] con la voz neural local.
 *
 * Obligatorio junto a los bloques de teoría y a los enunciados de ejercicio
 * cuando el diagnóstico marcó `needsAudioSupport`, para que el niño nunca
 * dependa de leer solo. El toque también devuelve un "pop" de la economía
 * sonora global, igual que el resto de la interfaz.
 */
@Composable
fun BotonAltavoz(
    texto: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 32.dp,
    colorFondo: Color = MaterialTheme.colorScheme.primaryContainer,
    descripcion: String = "Escuchar el texto en voz alta"
) {
    val textoActual by rememberUpdatedState(texto)
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(colorFondo)
            .clickable(role = Role.Button, onClickLabel = descripcion) {
                GlobalAudioManager.playPop()
                NarradorNativo.hablar(textoActual)
            }
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        KawaiiVolumeUpIcon(size = tamano * 0.66f)
    }
}
