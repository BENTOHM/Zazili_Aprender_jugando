package com.example.zazili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.zazili.core.accessibility.collectZenMode
import java.util.concurrent.TimeUnit
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

/**
 * Overlay de celebración con Konfetti. Cada vez que [trigger] cambia a un
 * valor mayor que el anterior se lanza una nueva lluvia de partículas.
 */
@Composable
fun CelebrationKonfetti(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    // Modo Zen global: sin partículas, en ningún minijuego.
    if (collectZenMode()) return

    val parties = remember(trigger) {
        if (trigger <= 0) emptyList() else celebrationParties()
    }
    if (parties.isNotEmpty()) {
        KonfettiView(modifier = modifier, parties = parties)
    }
}

private fun celebrationParties(): List<Party> = listOf(
    Party(
        speed = 0f,
        maxSpeed = 34f,
        damping = 0.9f,
        spread = 360,
        colors = listOf(
            Color(0xFFE8D48A).toArgb(),
            Color(0xFFCDE6C5).toArgb(),
            Color(0xFFE1D0F3).toArgb(),
            Color(0xFFBCE3F5).toArgb(),
            Color(0xFFF4B8C1).toArgb()
        ),
        position = Position.Relative(0.5, 0.4),
        timeToLive = 2600L,
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(140)
    ),
    Party(
        speed = 8f,
        maxSpeed = 28f,
        damping = 0.85f,
        spread = 360,
        colors = listOf(
            Color(0xFFF8C471).toArgb(),
            Color(0xFF7BE0A3).toArgb(),
            Color(0xFF6FD3F5).toArgb()
        ),
        position = Position.Relative(0.5, 0.62),
        timeToLive = 2000L,
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(80)
    )
)
