package com.example.zazili.presentation.lecciones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.NarradorNativo
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Contenido de una cápsula de teoría: 2 o 3 ideas cortas, un icono kawaii y el
 * nombre de la lección. Es inmutable para que la hoja jamás recomponga de más.
 */
@Immutable
data class TheoryContent(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val icono: String,
    val bullets: List<String>
)

/**
 * Micro-teoría estilo Duolingo antes de cada lección.
 *
 * La cápsula se monta sobre un velo que bloquea toques accidentales: solo se
 * cierra con el botón "¡Entendido!". Si el diagnóstico marcó apoyo de audio,
 * cada idea lleva su altavoz para leerla con el TTS nativo cuando el niño
 * quiera; la lectura nunca arranca sola y así el TDAH no se satura de voces.
 */
@Composable
fun TheoryBottomSheet(
    content: TheoryContent,
    audioApoyo: Boolean,
    onEntendido: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    val oscuro = isSystemInDarkTheme()
    val accionActual by rememberUpdatedState(onEntendido)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    DisposableEffect(Unit) {
        onDispose { NarradorNativo.detener() }
    }

    val fondoHoja = if (zen) MaterialTheme.colorScheme.surface else SoftCream
    val colorTexto = if (zen) MaterialTheme.colorScheme.onSurface else UniversoProfundo
    val colorEtiqueta = if (zen) MaterialTheme.colorScheme.primary else MagicalPurple
    val fondoBullet = if (zen) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        LavandaClara
    }
    val colorNumero = if (zen) MaterialTheme.colorScheme.primary else MagicalPurple

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (oscuro && zen) {
                    Color(0xFF0B0712).copy(alpha = 0.86f)
                } else {
                    UniversoProfundo.copy(alpha = 0.58f)
                }
            )
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { alto -> alto },
                animationSpec = tween(durationMillis = if (zen) 0 else 340)
            ) + fadeIn(animationSpec = tween(durationMillis = if (zen) 0 else 200)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = fondoHoja,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { paneTitle = "Cápsula de teoría de ${content.titulo}" }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp)
                        .padding(top = 12.dp, bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(colorTexto.copy(alpha = 0.18f))
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(colorNumero),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = content.icono,
                                fontSize = 34.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CÁPSULA DE TEORÍA",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorEtiqueta,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.6.sp
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = content.titulo,
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorTexto,
                                fontWeight = FontWeight.Black,
                                lineHeight = 26.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = content.subtitulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorTexto.copy(alpha = 0.62f),
                                maxLines = 2
                            )
                        }
                        if (audioApoyo) {
                            Spacer(Modifier.width(8.dp))
                            BotonAltavoz(
                                texto = "${content.titulo}. ${content.subtitulo}",
                                tamano = 32.dp,
                                colorFondo = colorNumero.copy(alpha = 0.16f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    content.bullets.take(3).forEachIndexed { indice, bullet ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = fondoBullet,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(colorNumero),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (indice + 1).toString(),
                                        color = ZaziliWhite,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                BionicText(
                                    text = bullet,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorTexto
                                )
                                if (audioApoyo) {
                                    Spacer(Modifier.width(6.dp))
                                    BotonAltavoz(
                                        texto = bullet,
                                        tamano = 30.dp,
                                        colorFondo = colorNumero.copy(alpha = 0.16f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    ZaziButton(
                        text = "¡Entendido!",
                        onClick = {
                            GlobalAudioManager.playPop()
                            accionActual()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        containerColor = VerdeDuo,
                        contentColor = ZaziliWhite,
                        cornerRadius = 20.dp,
                        baseHeight = 6.dp,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Leer tantito antes de jugar hace la diferencia.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorTexto.copy(alpha = 0.48f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
