package com.example.zazili.presentation.arena

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Filtro Ciber-Seguro: tarjetas estilo Tinder.
 *
 * Izquierda = bloquear (peligro), derecha = aceptar (seguro). La rotación sale
 * del desplazamiento horizontal y los sellos BLOQUEAR/ACEPTAR se encienden
 * conforme el niño arrastra. Un veredicto incorrecto pausa el mazo y abre una
 * hoja educativa antes de continuar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberSwipeScreen(
    motor: ArenaTechGameEngine,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by motor.estadoFiltro.collectAsStateWithLifecycle()
    val progreso by motor.progresoTech.collectAsStateWithLifecycle()

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            EncabezadoCiber(
                titulo = "Filtro Ciber-Seguro",
                subtitulo = "Izquierda = bloquear · Derecha = aceptar",
                chip = "Nivel ${motor.lessonId}",
                onSalir = onSalir
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoCiber("✅", "${estado.aciertos}", "Aciertos", Modifier.weight(1f))
                DatoCiber("🔥", "${estado.racha}", "Racha", Modifier.weight(1f))
                DatoCiber(
                    "🃏",
                    "${estado.cartasResueltas}/${estado.total}",
                    "Tarjetas",
                    Modifier.weight(1f)
                )
            }

            LinearProgressIndicator(
                progress = { estado.progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = VerdeDuo,
                trackColor = LavandaClara
            )

            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val densidad = LocalDensity.current
                val anchoPx = with(densidad) { maxWidth.toPx() }
                val umbralPx = anchoPx * 0.26f

                PilaTarjetas(
                    carta = estado.tarjetaActual,
                    pausado = estado.pausado || estado.terminado,
                    anchoPx = anchoPx,
                    umbralPx = umbralPx,
                    onDecidir = motor::responderCiber,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZaziButton(
                    text = "⬅️ Bloquear",
                    onClick = { motor.responderCiber(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    containerColor = CoralAviso,
                    contentColor = ZaziliWhite,
                    enabled = !estado.pausado && !estado.terminado
                )
                ZaziButton(
                    text = "Aceptar ✅",
                    onClick = { motor.responderCiber(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    containerColor = VerdeDuo,
                    contentColor = ZaziliWhite,
                    enabled = !estado.pausado && !estado.terminado
                )
            }
        }

        if (progreso.victoria) {
            PantallaVictoriaTech(
                titulo = "Filtro Ciber-Seguro",
                estado = progreso,
                onSalir = onSalir
            )
        }
    }

    val aviso = estado.aviso
    if (aviso != null) {
        val titulo = if (aviso.esSegura) {
            "¡Uy! Ese mensaje sí era seguro"
        } else {
            "¡Cuidado! Ese era un virus"
        }
        val colorAviso = if (aviso.esSegura) MintGreen else RosaSuave
        ModalBottomSheet(
            onDismissRequest = motor::cerrarAvisoCiber,
            containerColor = SoftCream
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (aviso.esSegura) "🛡️" else "🚨",
                    fontSize = 42.sp
                )
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (aviso.esSegura) VerdeDuoOscuro else CoralAviso,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorAviso,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = "De: ${aviso.emisor}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = UniversoProfundo
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "«${aviso.mensaje}»",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniversoProfundo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    text = aviso.explicacion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniversoProfundo.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                ZaziButton(
                    text = "Entendido, sigo aprendiendo",
                    onClick = motor::cerrarAvisoCiber,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    containerColor = VerdeDuo,
                    contentColor = ZaziliWhite
                )
            }
        }
    }
}

@Composable
private fun PilaTarjetas(
    carta: TarjetaCiber?,
    pausado: Boolean,
    anchoPx: Float,
    umbralPx: Float,
    onDecidir: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 24.dp)
                .graphicsLayer {
                    translationY = 22.dp.toPx()
                    scaleX = 0.95f
                    scaleY = 0.95f
                },
            shape = RoundedCornerShape(28.dp),
            color = LavandaClara
        ) {}

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 12.dp)
                .graphicsLayer {
                    translationY = 10.dp.toPx()
                    scaleX = 0.975f
                    scaleY = 0.975f
                },
            shape = RoundedCornerShape(28.dp),
            color = AzulCielo
        ) {}

        if (carta != null) {
            key(carta.id) {
                TarjetaDeslizable(
                    carta = carta,
                    pausado = pausado,
                    anchoPx = anchoPx,
                    umbralPx = umbralPx,
                    onDecidir = onDecidir,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else if (!pausado) {
            Text(
                text = "Preparando tarjetas…",
                style = MaterialTheme.typography.titleMedium,
                color = UniversoProfundo,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TarjetaDeslizable(
    carta: TarjetaCiber,
    pausado: Boolean,
    anchoPx: Float,
    umbralPx: Float,
    onDecidir: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val alcance = rememberCoroutineScope()
    val desplazamiento = remember { Animatable(0f) }
    val decisionActual by rememberUpdatedState(onDecidir)
    val anchoActual by rememberUpdatedState(anchoPx)
    val umbralActual by rememberUpdatedState(umbralPx)

    val gradiente = when (carta.nivel) {
        1 -> Brush.verticalGradient(listOf(AzulCielo, SoftCream))
        2 -> Brush.verticalGradient(listOf(RosaSuave, SoftCream))
        else -> Brush.verticalGradient(listOf(AmarilloSuave, SoftCream))
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = desplazamiento.value
                translationY = abs(desplazamiento.value) * 0.06f
                rotationZ = (desplazamiento.value / anchoActual.coerceAtLeast(1f))
                    .coerceIn(-1f, 1f) * 12f
            }
            .pointerInput(carta.id, pausado, anchoActual, umbralActual) {
                if (pausado) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        val valor = desplazamiento.value
                        alcance.launch {
                            if (abs(valor) > umbralActual) {
                                val destino = if (valor > 0f) {
                                    anchoActual * 1.7f
                                } else {
                                    -anchoActual * 1.7f
                                }
                                desplazamiento.animateTo(destino, tween(230))
                                decisionActual(valor > 0f)
                                desplazamiento.snapTo(0f)
                            } else {
                                desplazamiento.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        alcance.launch {
                            desplazamiento.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDrag = { cambio, arrastre ->
                        cambio.consume()
                        alcance.launch { desplazamiento.snapTo(desplazamiento.value + arrastre.x) }
                    }
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .background(gradiente)
            .border(
                width = 3.dp,
                color = MagicalPurple.copy(alpha = 0.18f),
                shape = RoundedCornerShape(28.dp)
            )
            .semantics {
                contentDescription =
                    "Mensaje de ${carta.emisor}. Desliza a la izquierda para bloquear o a la derecha para aceptar."
            }
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = ZaziliWhite.copy(alpha = 0.78f)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📩", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = carta.emisor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = UniversoProfundo
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MagicalPurple.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "NIVEL ${carta.nivel}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MagicalPurple
                )
            }

            Text(
                text = "«${carta.mensaje}»",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Text(
                text = "Desliza para decidir",
                style = MaterialTheme.typography.bodySmall,
                color = UniversoProfundo.copy(alpha = 0.55f),
                fontWeight = FontWeight.SemiBold
            )
        }

        val avance = (desplazamiento.value / umbralActual.coerceAtLeast(1f)).coerceIn(-1f, 1f)
        SelloDecision(
            texto = "BLOQUEAR",
            color = CoralAviso,
            alpha = (-avance).coerceIn(0f, 1f),
            rotacion = -12f,
            modifier = Modifier.align(Alignment.TopStart)
        )
        SelloDecision(
            texto = "ACEPTAR",
            color = VerdeDuoOscuro,
            alpha = avance.coerceIn(0f, 1f),
            rotacion = 12f,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Composable
private fun SelloDecision(
    texto: String,
    color: Color,
    alpha: Float,
    rotacion: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.01f) return
    Surface(
        modifier = modifier
            .padding(18.dp)
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = rotacion
                scaleX = 0.9f + alpha * 0.1f
                scaleY = 0.9f + alpha * 0.1f
            },
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(3.dp, color)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun EncabezadoCiber(
    titulo: String,
    subtitulo: String,
    chip: String,
    onSalir: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = {
                GlobalAudioManager.playPop()
                onSalir()
            }
        ) {
            Text(
                text = "← Salir",
                color = MagicalPurple,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = UniversoProfundo.copy(alpha = 0.66f),
                textAlign = TextAlign.Center
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = AmarilloSuave
        ) {
            Text(
                text = chip,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DatoCiber(
    emoji: String,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = LavandaClara
    ) {
        Column(
            Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$emoji $valor",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo
            )
            Text(
                text = etiqueta,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = UniversoProfundo.copy(alpha = 0.65f)
            )
        }
    }
}
