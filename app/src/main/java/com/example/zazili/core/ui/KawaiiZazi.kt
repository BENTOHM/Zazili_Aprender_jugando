package com.example.zazili.core.ui

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zazili.R
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.VerdeDuo
import kotlinx.coroutines.delay

/**
 * Mascota de Zazili basada en una imagen real, no en Canvas.
 *
 * Busca `zazi_kawaii_placeholder` en `res/drawable`; mientras el ilustrador no
 * la entregue, cae al ícono de galería del sistema. Solo anima la posición y la
 * escala: flotación suave y respiración lenta, apagadas en Modo Zen.
 */
@Composable
fun KawaiiZazi(
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    animar: Boolean = true,
    descripcion: String = "Zazi, la mascota de Zazili"
) {
    val zen = collectZenMode()
    val activo = animar && !zen
    val densidad = LocalDensity.current

    var flotandoArriba by remember { mutableStateOf(false) }
    var inhalando by remember { mutableStateOf(false) }

    LaunchedEffect(activo) {
        if (!activo) {
            flotandoArriba = false
            return@LaunchedEffect
        }
        flotandoArriba = true
        while (true) {
            delay(1_600)
            flotandoArriba = !flotandoArriba
        }
    }

    LaunchedEffect(activo) {
        if (!activo) {
            inhalando = false
            return@LaunchedEffect
        }
        inhalando = true
        while (true) {
            delay(2_400)
            inhalando = !inhalando
        }
    }

    val flotacion by animateFloatAsState(
        targetValue = if (activo && flotandoArriba) -7f else 0f,
        animationSpec = tween(
            durationMillis = if (activo) 1_600 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "zaziFlotacion"
    )
    val respiracion by animateFloatAsState(
        targetValue = if (activo && inhalando) 1.035f else 0.985f,
        animationSpec = tween(
            durationMillis = if (activo) 2_400 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "zaziRespiracion"
    )

    val desplazamientoPx = with(densidad) { flotacion.dp.toPx() }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MintGreen.copy(alpha = 0.45f),
                            VerdeDuo.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        Image(
            painter = painterResource(id = recursoZazi()),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .graphicsLayer {
                    translationY = desplazamientoPx
                    scaleX = respiracion
                    scaleY = respiracion
                },
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Resuelve el arte de Zazi una sola vez por contexto: el drawable propio si el
 * ilustrador ya lo entregó, o el ícono de galería del sistema como respaldo.
 * Público para que otras pantallas puedan cargarlo con Coil sin duplicar la
 * búsqueda por nombre.
 */
fun recursoKawaiiZazi(contexto: Context): Int {
    val propio = contexto.resources.getIdentifier(
        "zazi_kawaii_placeholder",
        "drawable",
        contexto.packageName
    )
    return if (propio != 0) propio else R.drawable.zazi_tender_placeholder
}

@Composable
private fun recursoZazi(): Int {
    val contexto = LocalContext.current
    return remember(contexto) { recursoKawaiiZazi(contexto) }
}
