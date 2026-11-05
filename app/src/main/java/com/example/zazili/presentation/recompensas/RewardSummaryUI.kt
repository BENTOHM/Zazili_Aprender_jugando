package com.example.zazili.presentation.recompensas

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.progress.LevelProgressionEngine
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.domain.economia.EventoRecompensa
import com.example.zazili.domain.economia.RecompensaZazoles
import com.example.zazili.domain.economia.ZazolEconomyManager
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliCreamSoft
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Fotografía de una sesión terminada, lista para celebrar. */
@Immutable
data class ResumenRecompensa(
    val zazoles: Int,
    val xpGanada: Int,
    val nivelAnterior: Int,
    val nivelNuevo: Int,
    val avanceAnterior: Float,
    val avanceNuevo: Float,
    val motivo: String
) {
    val subioDeNivel: Boolean get() = nivelNuevo > nivelAnterior
}

/**
 * Buzón global de recompensas.
 *
 * Escucha la economía una sola vez por proceso: cualquier lección o minijuego
 * que pague Zazoles (vía `ZazolEconomyManager.notificarActividad`) deja aquí
 * un resumen. También convierte ese pago en XP con [LevelProgressionEngine],
 * de modo que la barra del Tablero deja de estar congelada incluso si la
 * pantalla que completó la actividad ya no está compuesta.
 *
 * La UI llama a [consumir] al pulsar "Continuar"; el resumen sobrevive a la
 * navegación porque vive fuera de Compose.
 */
class BuzonResumenRecompensas private constructor(context: Context) {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val economia = ZazolEconomyManager.obtener(context)
    private val niveles = LevelProgressionEngine.obtener(context)
    private val candado = Any()

    private val _pendiente = MutableStateFlow<ResumenRecompensa?>(null)
    val pendiente: StateFlow<ResumenRecompensa?> = _pendiente.asStateFlow()

    private var parcial = Parcial()
    private var publicacion: Job? = null

    init {
        alcance.launch {
            economia.recompensas.collect { evento -> registrar(evento) }
        }
    }

    /**
     * Acumula Zazoles y XP. Los eventos de una misma sesión (lección + jefe,
     * por ejemplo) se funden en un solo resumen tras una ventana corta.
     */
    private fun registrar(evento: RecompensaZazoles) {
        val xp = xpPara(evento.evento)
        val antes = niveles.progreso.value
        val subida = niveles.otorgarXp(xp, evento.evento.etiqueta)

        synchronized(candado) {
            val previo = parcial
            val vacio = previo.zazoles == 0 && previo.xp == 0
            parcial = Parcial(
                zazoles = previo.zazoles + evento.evento.zazoles,
                xp = previo.xp + xp,
                nivelAnterior = if (vacio) subida.nivelAnterior else previo.nivelAnterior,
                nivelNuevo = subida.nivelNuevo,
                avanceAnterior = if (vacio || subida.subioDeNivel) 0f else antes.avance,
                motivo = evento.evento.etiqueta
            )
            publicacion?.cancel()
            publicacion = alcance.launch {
                delay(VENTANA_DE_AGRUPACION_MS)
                publicar()
            }
        }
    }

    private fun publicar() {
        val foto = synchronized(candado) { parcial }
        if (foto.zazoles <= 0 && foto.xp <= 0) return
        _pendiente.value = ResumenRecompensa(
            zazoles = foto.zazoles,
            xpGanada = foto.xp,
            nivelAnterior = foto.nivelAnterior,
            nivelNuevo = foto.nivelNuevo,
            avanceAnterior = foto.avanceAnterior.coerceIn(0f, 1f),
            avanceNuevo = niveles.progreso.value.avance.coerceIn(0f, 1f),
            motivo = foto.motivo
        )
    }

    /** Cierra el resumen actual. La siguiente sesión empieza de cero. */
    fun consumir() {
        synchronized(candado) {
            parcial = Parcial()
            publicacion?.cancel()
            publicacion = null
        }
        _pendiente.value = null
    }

    fun registrarManual(resumen: ResumenRecompensa) {
        _pendiente.value = resumen
    }

    private data class Parcial(
        val zazoles: Int = 0,
        val xp: Int = 0,
        val nivelAnterior: Int = 0,
        val nivelNuevo: Int = 0,
        val avanceAnterior: Float = 0f,
        val motivo: String = ""
    )

    companion object {
        private const val VENTANA_DE_AGRUPACION_MS = 450L

        private fun xpPara(evento: EventoRecompensa): Int = when (evento) {
            EventoRecompensa.JEFE_DERROTADO -> 50
            EventoRecompensa.RESCATE_EN_NIDO -> 30
            EventoRecompensa.RACHA_DIARIA -> 15
            EventoRecompensa.LECCION_COMPLETADA,
            EventoRecompensa.ARENA_CORTEX -> 20
            EventoRecompensa.MINIJUEGO_COMPLETADO,
            EventoRecompensa.ACTIVIDAD_COMPLETADA -> 15
        }

        @Volatile
        private var instancia: BuzonResumenRecompensas? = null

        fun obtener(context: Context): BuzonResumenRecompensas =
            instancia ?: synchronized(this) {
                instancia ?: BuzonResumenRecompensas(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}

/**
 * Celebración post-sesión.
 *
 * Se muestra como capa completa sobre el Tablero (o cualquier pantalla que la
 * hospede) para que terminar una lección nunca "rebote" sin recompensa. El
 * botón "Continuar" solo cierra la capa: el niño ya está en su casita.
 */
@Composable
fun RewardSummaryDialog(
    resumen: ResumenRecompensa?,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrado by remember { mutableStateOf(resumen) }
    LaunchedEffect(resumen) {
        if (resumen != null) {
            mostrado = resumen
            GlobalAudioManager.playSuccess()
        }
    }
    val visible = resumen != null
    val actual = resumen ?: mostrado ?: return

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.86f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.92f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UniversoProfundo.copy(alpha = 0.72f))
                .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = ZaziliCreamSoft,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    ZaziSpriteAnimado(
                        size = 104.dp,
                        estado = ZaziState.CELEBRACION,
                        animar = true,
                        descripcion = "Zazi celebra contigo"
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "¡Misión cumplida!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = actual.motivo,
                        style = MaterialTheme.typography.labelMedium,
                        color = UniversoProfundo.copy(alpha = 0.62f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(14.dp))
                    FilaZazoles(zazoles = actual.zazoles)

                    if (actual.xpGanada > 0) {
                        Spacer(Modifier.height(10.dp))
                        FilaXp(
                            xpGanada = actual.xpGanada,
                            nivelNuevo = actual.nivelNuevo,
                            subioDeNivel = actual.subioDeNivel
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    BarraXpAnimada(resumen = actual)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Nivel ${actual.nivelNuevo} · te faltan " +
                            "${(100 - (actual.avanceNuevo * 100).toInt()).coerceAtLeast(0)}% " +
                            "para el siguiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = UniversoProfundo.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))
                    ZaziButton(
                        text = "Continuar",
                        onClick = {
                            GlobalAudioManager.playPop()
                            onContinuar()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        containerColor = VerdeDuo,
                        contentColor = ZaziliWhite,
                        cornerRadius = 22.dp,
                        baseHeight = 6.dp,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaZazoles(zazoles: Int) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AmarilloSuave,
        border = BorderStroke(1.5.dp, AmarilloVibrante),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
        ) {
            KawaiiCoinIcon(size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "+$zazoles Zazoles",
                style = MaterialTheme.typography.titleLarge,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun FilaXp(xpGanada: Int, nivelNuevo: Int, subioDeNivel: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (subioDeNivel) {
            KawaiiSparkIcon(size = 22.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = if (subioDeNivel) {
                "¡Subiste al nivel $nivelNuevo!  +$xpGanada XP"
            } else {
                "+$xpGanada XP"
            },
            style = MaterialTheme.typography.titleSmall,
            color = MagicalPurple,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BarraXpAnimada(resumen: ResumenRecompensa) {
    var objetivo by remember(resumen) {
        mutableStateOf(resumen.avanceAnterior.coerceIn(0f, 1f))
    }
    LaunchedEffect(resumen) {
        delay(220L)
        objetivo = resumen.avanceNuevo.coerceIn(0f, 1f)
    }
    val avance by animateFloatAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "xpRecompensa"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .background(LavandaMedia.copy(alpha = 0.35f), CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(avance)
                .height(14.dp)
                .background(
                    Brush.horizontalGradient(listOf(MintGreen, VerdeDuo)),
                    CircleShape
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                KawaiiStarIcon(
                    size = 18.dp,
                    modifier = Modifier.padding(end = 3.dp)
                )
            }
        }
    }
}

/** Alias explícito para pantallas que prefieran el nombre de pantalla. */
@Composable
fun RewardSummaryScreen(
    resumen: ResumenRecompensa?,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier
) {
    RewardSummaryDialog(resumen = resumen, onContinuar = onContinuar, modifier = modifier)
}
