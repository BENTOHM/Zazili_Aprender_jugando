package com.example.zazili.core.logros

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay

/** Tiempo exacto que el trofeo permanece en pantalla antes de irse. */
const val DURACION_BANNER_LOGRO_MS = 3_500L

/** Margen extra para que termine la animación de salida sin cortes. */
const val DURACION_SALIDA_BANNER_LOGRO_MS = 300L

/**
 * Aviso global de logro desbloqueado.
 *
 * Una tarjeta dorada cae desde el borde superior, se queda 3.5 segundos y
 * sube de vuelta. El padre decide cuándo mostrarla pasándole un logro nuevo:
 * cada logro recibe su propio ciclo de vida, así que dos desbloqueos seguidos
 * nunca comparten temporizador.
 *
 * El texto se anuncia con `liveRegion` para que el lector de pantalla lo lea
 * sin robar el foco al niño.
 */
@Composable
fun AchievementUnlockedBanner(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    var visible by remember(achievement.id) { mutableStateOf(false) }

    LaunchedEffect(achievement.id) {
        visible = true
        delay(DURACION_BANNER_LOGRO_MS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        enter = slideInVertically(
            initialOffsetY = { alto -> -alto },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
        exit = slideOutVertically(
            targetOffsetY = { alto -> -alto },
            animationSpec = tween(durationMillis = 280)
        ) + fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        TarjetaTrofeo(achievement = achievement)
    }
}

@Composable
private fun TarjetaTrofeo(achievement: Achievement) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = BorderStroke(3.dp, ZaziliWhite)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(AmarilloSuave, AmarilloVibrante)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics(mergeDescendants = true) {}
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .background(UniversoProfundo, CircleShape)
                    .border(2.dp, AmarilloSuave, CircleShape)
            ) {
                Text(text = "🏆", fontSize = 28.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¡Logro Desbloqueado!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = UniversoProfundo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = UniversoProfundo,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (achievement.rewardZazoles > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KawaiiCoinIcon(size = 16.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "+${achievement.rewardZazoles} Zazoles",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = UniversoMedio
                        )
                    }
                }
            }
        }
    }
}
