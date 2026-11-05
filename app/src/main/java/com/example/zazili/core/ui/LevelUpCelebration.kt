package com.example.zazili.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.ZaziliApplication
import com.example.zazili.ui.theme.ShadowMemoryGreen
import com.example.zazili.ui.theme.ZaziliCreamSoft
import com.example.zazili.ui.theme.ZaziliPrimaryText

/**
 * Overlay global de mérito: cuando [com.example.zazili.core.progress.GameProgressManager]
 * registra una subida de nivel, muestra el diálogo "¡Subiste de Nivel!" con
 * lluvia de Konfetti. Se invoca una sola vez, en la raíz de la navegación.
 */
@Composable
fun LevelUpCelebration(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember(context) {
        (context.applicationContext as ZaziliApplication).container.gameProgressManager
    }
    val events by manager.levelUpEvents.collectAsStateWithLifecycle()
    val progress by manager.progress.collectAsStateWithLifecycle()

    if (events <= 0) return

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x59000000))
            .pointerInput(Unit) { detectTapGestures { /* bloquea toques de fondo */ } },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(durationMillis = 180)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = ZaziliCreamSoft,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(30.dp),
                        clip = false,
                        ambientColor = ShadowMemoryGreen,
                        spotColor = ShadowMemoryGreen
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    ZaziSpriteAnimado(
                        modifier = Modifier.size(112.dp),
                        estado = ZaziState.CELEBRACION
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "¡Subiste de Nivel!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ZaziliPrimaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Ahora eres nivel ${progress.level}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ZaziliPrimaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "¡Tu esfuerzo hace brillar el bosque!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZaziliPrimaryText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    ZaziButton(
                        text = "¡Seguir jugando!",
                        onClick = manager::consumeLevelUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )
                }
            }
        }

        CelebrationKonfetti(trigger = events, modifier = Modifier.fillMaxSize())
    }
}
