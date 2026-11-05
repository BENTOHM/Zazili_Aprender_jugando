package com.example.zazili.presentation.lecciones

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.mascot.ZaziPetEngine
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo

/**
 * Pantalla de victoria compartida por todos los mapas de lecciones.
 *
 * El botón verde «¡Siguiente Lección!» es el camino principal y respira con un
 * pulso suave para invitar a tocarlo; en Modo Zen el pulso se apaga y el botón
 * queda quieto. «Repetir» usa amarillo y «Volver al mapa» usa lavanda para que
 * las tres acciones se distingan de un vistazo, con áreas táctiles grandes.
 *
 * En la última lección del mapa solo queda «Volver al mapa»: no hay siguiente
 * que ofrecer y prometerlo sería confuso para el niño.
 */
@Composable
internal fun PantallaCompletada(
    estrellas: Int,
    onRepetir: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    puedeSiguiente: Boolean = false,
    esUltimaLeccion: Boolean = false,
    onSiguiente: () -> Unit = {}
) {
    val zen = collectZenMode()
    val fraseVictoria = remember(estrellas) { ZaziPetEngine.fraseDeVictoria(estrellas) }
    val transicion = rememberInfiniteTransition(label = "pulsoSiguiente")
    val pulso by transicion.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaSiguiente"
    )
    val escalaSiguiente = if (zen) 1f else pulso

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        KawaiiZazi(
            size = 152.dp,
            descripcion = "Zazi celebra la lección completada"
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "¡Lección completada!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.semantics {
                contentDescription = "$estrellas de 3 estrellas"
            }
        ) {
            repeat(3) { indice ->
                KawaiiStarIcon(
                    size = 42.dp,
                    color = if (indice < estrellas) {
                        AmarilloVibrante
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = fraseVictoria,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))

        if (esUltimaLeccion) {
            Text(
                text = "¡Completaste todo este mapa! Zazi te espera para repasar cuando quieras.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            ZaziButton(
                text = "Volver al mapa",
                onClick = onSalir,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                containerColor = VerdeDuo,
                contentColor = contenidoDe(VerdeDuo),
                cornerRadius = 22.dp,
                baseHeight = 6.dp,
                fontSize = 19.sp
            )
        } else {
            if (puedeSiguiente) {
                ZaziButton(
                    text = "¡Siguiente Lección!",
                    onClick = onSiguiente,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = escalaSiguiente
                            scaleY = escalaSiguiente
                        }
                        .height(66.dp),
                    containerColor = VerdeDuo,
                    contentColor = contenidoDe(VerdeDuo),
                    cornerRadius = 22.dp,
                    baseHeight = 7.dp,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(14.dp))
            }

            ZaziButton(
                text = "Repetir",
                onClick = onRepetir,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                containerColor = AmarilloVibrante,
                contentColor = UniversoProfundo,
                cornerRadius = 20.dp,
                baseHeight = 5.dp,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(12.dp))
            ZaziButton(
                text = "Volver al mapa",
                onClick = onSalir,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                containerColor = LavandaClara,
                contentColor = UniversoProfundo,
                cornerRadius = 20.dp,
                baseHeight = 5.dp,
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "El mapa te espera cuando quieras. Sin prisa.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}
