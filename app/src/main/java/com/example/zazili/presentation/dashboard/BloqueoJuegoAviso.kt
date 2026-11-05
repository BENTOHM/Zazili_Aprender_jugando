package com.example.zazili.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zazili.core.mascot.BloqueoJuego
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay

private const val DURACION_AVISO_BLOQUEO_MS = 4_000L

/**
 * Aviso flotante de la compuerta de juego. No altera el layout de ninguna
 * pantalla: cae encima del contenido, explica el bloqueo con calma y se retira
 * solo o al tocar "Va".
 */
@Composable
fun AvisoBloqueoJuego(
    bloqueo: BloqueoJuego?,
    onDescartar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Conserva el último texto mientras la tarjeta se desvanece.
    var ultimoMensaje by remember { mutableStateOf("") }
    LaunchedEffect(bloqueo) {
        val actual = bloqueo ?: return@LaunchedEffect
        ultimoMensaje = actual.mensaje
        delay(DURACION_AVISO_BLOQUEO_MS)
        onDescartar()
    }

    AnimatedVisibility(
        visible = bloqueo != null,
        enter = fadeIn(tween(durationMillis = 180)) +
            slideInVertically(tween(durationMillis = 240)) { alto -> alto / 2 },
        exit = fadeOut(tween(durationMillis = 160)) +
            slideOutVertically(tween(durationMillis = 200)) { alto -> alto / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = UniversoProfundo,
            contentColor = ZaziliWhite,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = 18.dp,
                    top = 14.dp,
                    end = 8.dp,
                    bottom = 14.dp
                )
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zazi te necesita",
                        color = AmarilloSuave,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ultimoMensaje,
                        color = ZaziliWhite,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(onClick = onDescartar) {
                    Text(
                        text = "Va",
                        color = AmarilloVibrante,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
