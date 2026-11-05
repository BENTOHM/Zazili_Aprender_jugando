package com.example.zazili.core.navigation

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.ajustes.AjustesManager
import com.example.zazili.core.audio.AudioEngine
import com.example.zazili.core.racha.RachaEngine
import com.example.zazili.core.tiempo.TimeOfDayEngine
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.presentation.onboarding.OnboardingFlowManager
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Salida del splash; [ZaziliApp] la comparte con la entrada del contenido real. */
const val DURACION_SALIDA_SPLASH_MS = 600

/** Respiro mínimo para que el despertar de Zazi no sea un parpadeo. */
private const val DURACION_MINIMA_SPLASH_MS = 1_250L

/** Tope de espera por cada DataStore: el niño nunca queda atorado aquí. */
private const val ESPERA_MAXIMA_CARGA_MS = 2_000L

/**
 * Pantalla de despertar de Zazili, inspirada en la entrada de Duolingo.
 *
 * Fondo morado profundo de la marca, Zazi respirando en el centro y una carga
 * silenciosa: perfil/onboarding, SoundPool, racha y fase del día se leen en
 * [Dispatchers.IO]. [onListo] se dispara una sola vez, cuando todo terminó y ya
 * pasó el respiro mínimo; [ZaziliApp] es quien desliza esta capa hacia arriba.
 */
@Composable
fun ZaziliSplashScreen(
    onListo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val avisoListo by rememberUpdatedState(onListo)

    // Respiración infinita: 1.0 → 1.12 → 1.0 con un tween suave y sin fin.
    var expandido by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_400L)
            expandido = !expandido
        }
    }
    val escala by animateFloatAsState(
        targetValue = if (expandido) 1.12f else 1f,
        animationSpec = tween(durationMillis = 1_400, easing = FastOutSlowInEasing),
        label = "respiracionZazi"
    )

    LaunchedEffect(Unit) {
        val inicio = SystemClock.elapsedRealtime()
        withContext(Dispatchers.IO) {
            runCatching {
                withTimeoutOrNull(ESPERA_MAXIMA_CARGA_MS) {
                    OnboardingFlowManager.obtener(contexto)
                        .estado
                        .first { !it.cargando }
                }
            }
            runCatching { AudioEngine.initialize(contexto) }
            runCatching {
                val racha = RachaEngine.obtener(contexto)
                withTimeoutOrNull(ESPERA_MAXIMA_CARGA_MS) {
                    racha.estado.first { it.listo }
                }
                racha.garantizarBonoInicial()
                racha.programarRecordatorios()
            }
            runCatching { TimeOfDayEngine.faseActual() }
            runCatching {
                withTimeoutOrNull(ESPERA_MAXIMA_CARGA_MS) {
                    AjustesManager.obtener(contexto).modoTema.first()
                }
            }
        }
        val transcurrido = SystemClock.elapsedRealtime() - inicio
        delay((DURACION_MINIMA_SPLASH_MS - transcurrido).coerceAtLeast(0L))
        avisoListo()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UniversoProfundo,
                        UniversoMedio,
                        MagicalPurple
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(232.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AmarilloSuave.copy(alpha = 0.30f),
                                    MagicalPurple.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                ZaziSpriteAnimado(
                    modifier = Modifier.scale(escala),
                    size = 168.dp,
                    estado = ZaziState.VUELO_IDLE,
                    animar = true,
                    descripcion = "Zazi despierta y te está esperando"
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Zazili",
                color = ZaziliWhite,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Preparando tu casita…",
                color = LavandaMedia,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
