package com.example.zazili.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.zazili.ui.theme.NightSurfaceHigh
import com.example.zazili.ui.theme.ZaziliWhite

/** Botón Sol/Luna con rotación animada: recorrido Automático → Día → Noche. */
@Composable
fun ThemeToggleButton(
    darkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (darkTheme) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "themeRotation"
    )
    val background by animateColorAsState(
        targetValue = if (darkTheme) NightSurfaceHigh else ZaziliWhite,
        animationSpec = tween(300),
        label = "toggleBackground"
    )
    val descripcion = if (darkTheme) {
        "Apariencia: noche. Toca para cambiar"
    } else {
        "Apariencia: día. Toca para cambiar"
    }

    ZaziButton(
        text = descripcion,
        onClick = onToggle,
        modifier = modifier
            .size(52.dp)
            .semantics { contentDescription = descripcion },
        containerColor = background,
        contentColor = if (darkTheme) ZaziliWhite else NightSurfaceHigh,
        cornerRadius = 26.dp,
        baseHeight = 4.dp
    ) {
        Box(
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
            contentAlignment = Alignment.Center
        ) {
            if (darkTheme) {
                KawaiiMoonIcon(size = 28.dp)
            } else {
                KawaiiSunIcon(size = 28.dp)
            }
        }
    }
}
