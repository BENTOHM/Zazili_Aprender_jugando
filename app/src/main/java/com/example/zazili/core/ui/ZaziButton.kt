package com.example.zazili.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón "3D bouncy" de Zazili 2.0.
 *
 * Visual: una cara sólida sobre una base más oscura, como los botones de
 * Duolingo. Al presionar, la cara baja y aplasta la base; al soltar regresa con
 * un muelle suave. La detección del gesto se hace con [pointerInput] (no con
 * `clickable`) y la profundidad se anima con [animateFloatAsState].
 *
 * Importante: el llamador debe dar tamaño al botón (`height`, `size` o
 * `fillMaxWidth`) para que la base y la cara compartan medidas.
 */
@Composable
fun ZaziButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondary,
    contentColor: Color = MaterialTheme.colorScheme.onSecondary,
    shadowColor: Color = containerColor.darken(0.26f),
    cornerRadius: Dp = 22.dp,
    baseHeight: Dp = 7.dp,
    enabled: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 17.sp,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    var presionado by remember { mutableStateOf(false) }
    val accionActual by rememberUpdatedState(onClick)
    val densidad = LocalDensity.current
    val alturaBasePx = with(densidad) { baseHeight.toPx() }

    val presion by animateFloatAsState(
        targetValue = if (presionado && enabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "presionBoton3D"
    )
    val escala = 1f - presion * 0.018f

    val forma = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier) {
        // Base: el "grosor" del botón.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { translationY = alturaBasePx }
                .clip(forma)
                .background(if (enabled) shadowColor else shadowColor.copy(alpha = 0.55f))
        )

        // Cara: se hunde sobre la base al presionar.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationY = alturaBasePx * presion
                    scaleX = escala
                    scaleY = escala
                }
                .clip(forma)
                .background(if (enabled) containerColor else containerColor.copy(alpha = 0.6f))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        presionado = true
                        try {
                            val soltar = waitForUpOrCancellation()
                            if (soltar != null) accionActual()
                        } finally {
                            presionado = false
                        }
                    }
                }
                .semantics {
                    role = Role.Button
                    if (enabled) {
                        onClick(label = text.ifBlank { "Activar" }) {
                            accionActual()
                            true
                        }
                    } else {
                        disabled()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                content()
            } else {
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun Color.darken(factor: Float): Color = Color(
    red = red * (1f - factor.coerceIn(0f, 0.95f)),
    green = green * (1f - factor.coerceIn(0f, 0.95f)),
    blue = blue * (1f - factor.coerceIn(0f, 0.95f)),
    alpha = alpha
)
