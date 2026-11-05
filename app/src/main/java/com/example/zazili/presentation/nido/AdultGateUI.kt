package com.example.zazili.presentation.nido

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.random.Random

/**
 * Compuerta amable para adultos.
 *
 * No es autenticación real: una multiplicación de dos cifras basta para que un
 * niño pequeño no abra por accidente las métricas emocionales del Nido. Cada
 * error cambia la cuenta, así que adivinar no es un atajo.
 */
@Composable
fun AdultGateDialog(
    visible: Boolean,
    onDesbloquear: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    var factorA by rememberSaveable { mutableIntStateOf(Random.nextInt(6, 10)) }
    var factorB by rememberSaveable { mutableIntStateOf(Random.nextInt(6, 10)) }
    var respuesta by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val foco = remember { FocusRequester() }

    val verificar: () -> Unit = {
        val esperado = factorA * factorB
        if (respuesta.trim().toIntOrNull() == esperado) {
            GlobalAudioManager.playSuccess()
            onDesbloquear()
        } else {
            GlobalAudioManager.playMiss()
            error = "Casi. Resuelve esta nueva cuenta."
            respuesta = ""
            factorA = Random.nextInt(6, 10)
            factorB = Random.nextInt(6, 10)
            runCatching { foco.requestFocus() }
        }
    }

    LaunchedEffect(visible) {
        if (visible) runCatching { foco.requestFocus() }
    }

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MagicalPurple.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    KawaiiLockIcon(size = 32.dp, color = MagicalPurple)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Acceso para adultos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Para entrar al reporte, resuelve esta multiplicación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$factorA × $factorB = ?",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = respuesta,
                    onValueChange = { texto ->
                        if (texto.length <= 3 && texto.all { it.isDigit() }) {
                            respuesta = texto
                            error = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(foco),
                    label = { Text("Tu respuesta") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { verificar() }),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZaziButton(
                        text = "Cancelar",
                        onClick = onCancelar,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cornerRadius = 18.dp,
                        baseHeight = 4.dp,
                        fontSize = 15.sp
                    )
                    ZaziButton(
                        text = "Entrar",
                        onClick = verificar,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        containerColor = VerdeDuo,
                        contentColor = ZaziliWhite,
                        cornerRadius = 18.dp,
                        baseHeight = 5.dp,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Esta compuerta evita que el niño abra las métricas emocionales por accidente.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
