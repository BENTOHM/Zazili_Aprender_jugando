package com.example.zazili.presentation.hogar

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.zazili.R
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.tiempo.DayPhase
import com.example.zazili.core.tiempo.recordarFaseDelDia

/**
 * Lámina del cuarto de Zazi para cada momento del día. Las cuatro viven en
 * `drawable-nodpi` y el artista puede reemplazarlas sin tocar código.
 */
@get:DrawableRes
val DayPhase.recursoFondo: Int
    get() = when (this) {
        DayPhase.AMANECER -> R.drawable.fondo_amanecer
        DayPhase.DIA -> R.drawable.fondo_dia
        DayPhase.TARDE -> R.drawable.fondo_tarde
        DayPhase.NOCHE -> R.drawable.fondo_noche
    }

/**
 * Contenedor de pantalla completa del tablero: pinta la lámina del cuarto
 * según la hora y dibuja el contenido encima.
 */
@Composable
fun ZaziRoomScreen(
    faseDelDia: DayPhase = recordarFaseDelDia(),
    modifier: Modifier = Modifier,
    contenido: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        FondoHabitacionZazi(
            faseDelDia = faseDelDia,
            modifier = Modifier.matchParentSize()
        )
        Box(modifier = Modifier.matchParentSize(), content = contenido)
    }
}

/**
 * Fondo del cuarto: fundido suave de 900 ms entre las cuatro láminas. En Modo
 * Zen se apaga a un color plano para no estimular la vista.
 */
@Composable
fun FondoHabitacionZazi(
    faseDelDia: DayPhase,
    modifier: Modifier = Modifier,
    zen: Boolean = collectZenMode()
) {
    if (zen) {
        val fondoZen = zenMutedBackground(
            MaterialTheme.colorScheme.background.luminance() < 0.5f
        )
        Box(modifier = modifier.fillMaxSize().background(fondoZen))
        return
    }

    Crossfade(
        targetState = faseDelDia,
        modifier = modifier,
        animationSpec = tween(durationMillis = 900),
        label = "fondoCuartoZazi"
    ) { fase ->
        Image(
            painter = painterResource(id = fase.recursoFondo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
