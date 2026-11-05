@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.zazili.presentation.lecciones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.NarradorNativo
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiVolumeUpIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pantalla de un reto de Matemáticas.
 *
 * Tres apoyos, pensados para TDAH y dislexia:
 *  1. Palabras clave con subrayado punteado: al tocarlas brota una pista gratis.
 *  2. Foco: gasta 10 Zazoles y muestra la guía visual + el inicio de la
 *     respuesta correcta. Se enciende una sola vez por reto.
 *  3. Voz: si el perfil pide apoyo de audio, la consigna y el reto se narran
 *     solos, y hay un botón para repetirlos.
 *
 * La pantalla solo reporta la elección; el ViewModel decide si acierta, mueve
 * la cola y administra los corazones.
 */
@Composable
fun ExerciseScreen(
    exercise: MathExercise,
    zazoles: Int,
    focoUsado: Boolean,
    guiaFoco: List<String>,
    focoAviso: String?,
    pistaVisible: Boolean,
    bloqueado: Boolean,
    audioApoyo: Boolean,
    onMostrarPista: () -> Unit,
    onCerrarPista: () -> Unit,
    onUsarFoco: () -> Unit,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    var seleccion by rememberSaveable(exercise.id) { mutableIntStateOf(-1) }

    val textoParaNarrar = remember(exercise.id) {
        "${exercise.consigna}. ${exercise.enunciado}"
    }
    LaunchedEffect(exercise.id, audioApoyo) {
        if (audioApoyo) NarradorNativo.hablar(textoParaNarrar)
    }
    DisposableEffect(Unit) {
        onDispose { NarradorNativo.detener() }
    }

    val respuestaCorrecta = exercise.opciones.getOrNull(exercise.indiceCorrecto).orEmpty()
    val pistaParcial = remember(exercise.id, respuestaCorrecta) {
        val letras = (respuestaCorrecta.length * 0.4f).roundToInt()
            .coerceIn(1, respuestaCorrecta.length.coerceAtLeast(1))
        respuestaCorrecta.take(letras)
    }
    val pistaIncompleta = pistaParcial.length < respuestaCorrecta.length

    val colorTexto = MaterialTheme.colorScheme.onBackground
    val fondoPista = if (zen) MaterialTheme.colorScheme.surfaceVariant else AmarilloSuave
    val textoTip = if (zen) MaterialTheme.colorScheme.onSurfaceVariant else UniversoProfundo
    val fondoChip = if (zen) MaterialTheme.colorScheme.surfaceVariant else LavandaClara
    val colorChip = if (zen) MaterialTheme.colorScheme.onSurfaceVariant else UniversoProfundo

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (zen) MaterialTheme.colorScheme.surfaceVariant else LavandaClara),
                contentAlignment = Alignment.Center
            ) {
                Text(text = exercise.icono, fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorTexto,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Nivel ${exercise.dificultad} · $zazoles Zazoles",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorTexto.copy(alpha = 0.58f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            BotonFoco(
                focoUsado = focoUsado,
                habilitado = !bloqueado,
                zen = zen,
                onClick = onUsarFoco
            )
        }

        if (exercise.conceptosDificiles.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Palabras clave · toca una punteada para una pista gratis",
                style = MaterialTheme.typography.labelSmall,
                color = colorTexto.copy(alpha = 0.55f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exercise.conceptosDificiles.forEach { concepto ->
                    Text(
                        text = concepto,
                        style = MaterialTheme.typography.labelLarge,
                        color = colorChip,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(fondoChip.copy(alpha = 0.70f))
                            .clickable(enabled = !bloqueado) {
                                GlobalAudioManager.playPop()
                                if (pistaVisible) onCerrarPista() else onMostrarPista()
                            }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .punteadoInferior(
                                color = if (zen) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MagicalPurple
                                }
                            )
                    )
                }
            }

            AnimatedVisibility(
                visible = pistaVisible,
                enter = fadeIn(tween(160)) + expandVertically(tween(200)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(160))
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = fondoPista,
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (zen) {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            } else {
                                AmarilloOscuro.copy(alpha = 0.55f)
                            }
                        ),
                        shadowElevation = if (zen) 0.dp else 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCerrarPista() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            KawaiiFocoIcon(
                                tamano = 22.dp,
                                color = if (zen) textoTip else UniversoProfundo
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "¡Pista!",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = textoTip,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.height(2.dp))
                                BionicText(
                                    text = exercise.pista,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textoTip
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Toca para cerrar",
                                style = MaterialTheme.typography.labelSmall,
                                color = textoTip.copy(alpha = 0.52f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 2.dp,
                color = if (zen) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                }
            ),
            shadowElevation = if (zen) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.etiquetaReto,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (zen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MagicalPurple
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.weight(1f))
                    if (audioApoyo) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(role = Role.Button) {
                                    GlobalAudioManager.playPop()
                                    NarradorNativo.hablar(textoParaNarrar)
                                }
                                .semantics {
                                    contentDescription = "Escuchar el reto otra vez"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            KawaiiVolumeUpIcon(size = 19.dp)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = exercise.consigna,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorTexto.copy(alpha = 0.55f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                BionicText(
                    text = exercise.enunciado,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorTexto
                )
            }
        }

        if (focoUsado) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (zen) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MintGreen.copy(alpha = 0.34f)
                },
                border = BorderStroke(
                    width = 2.dp,
                    color = if (zen) {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    } else {
                        MintGreen.copy(alpha = 0.9f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KawaiiFocoIcon(
                            tamano = 21.dp,
                            color = if (zen) textoTip else UniversoProfundo
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Foco encendido",
                            style = MaterialTheme.typography.titleSmall,
                            color = textoTip,
                            fontWeight = FontWeight.Black
                        )
                    }
                    if (guiaFoco.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            guiaFoco.forEach { guia ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (zen) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        ZaziliWhite.copy(alpha = 0.72f)
                                    }
                                ) {
                                    Text(
                                        text = guia,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = textoTip,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 9.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "La respuesta empieza con «$pistaParcial" +
                            if (pistaIncompleta) "…»." else "».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textoTip,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (focoAviso != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (zen) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    CoralAviso.copy(alpha = 0.18f)
                },
                border = BorderStroke(
                    width = 1.5.dp,
                    color = CoralAviso.copy(alpha = if (zen) 0.35f else 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                BionicText(
                    text = focoAviso,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (zen) MaterialTheme.colorScheme.onSurfaceVariant else UniversoProfundo,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        exercise.opciones.forEachIndexed { indice, opcion ->
            OpcionRespuesta(
                letra = ('A'.code + indice).toChar().toString(),
                texto = opcion,
                seleccionada = seleccion == indice,
                habilitada = !bloqueado,
                zen = zen,
                onClick = {
                    GlobalAudioManager.playPop()
                    seleccion = indice
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        ZaziButton(
            text = "Comprobar",
            onClick = {
                if (seleccion in exercise.opciones.indices) {
                    onSubmitAnswer(
                        seleccion,
                        seleccion == exercise.indiceCorrecto
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            containerColor = VerdeDuo,
            contentColor = ZaziliWhite,
            cornerRadius = 20.dp,
            baseHeight = 6.dp,
            enabled = seleccion >= 0 && !bloqueado,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Sin prisa. Puedes usar una pista gratis o encender el Foco.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = colorTexto.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  FOCO                                                                      */
/* ------------------------------------------------------------------------- */

@Composable
private fun BotonFoco(
    focoUsado: Boolean,
    habilitado: Boolean,
    zen: Boolean,
    onClick: () -> Unit
) {
    val colorFondo = when {
        focoUsado || zen -> MaterialTheme.colorScheme.surfaceVariant
        else -> AmarilloSuave
    }
    val colorContenido = when {
        focoUsado || zen -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> UniversoProfundo
    }
    ZaziButton(
        text = if (focoUsado) "Foco ya encendido" else "Foco por 10 Zazoles",
        onClick = onClick,
        modifier = Modifier
            .width(112.dp)
            .height(60.dp),
        containerColor = colorFondo,
        contentColor = colorContenido,
        cornerRadius = 18.dp,
        baseHeight = 4.dp,
        enabled = habilitado && !focoUsado
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KawaiiFocoIcon(tamano = 17.dp, color = colorContenido)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Foco",
                    color = colorContenido,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(1.dp))
            if (focoUsado) {
                Text(
                    text = "Encendido",
                    color = colorContenido.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "10",
                        color = colorContenido,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.width(2.dp))
                    KawaiiCoinIcon(size = 11.dp)
                }
            }
        }
    }
}

@Composable
private fun KawaiiFocoIcon(
    tamano: Dp = 20.dp,
    color: Color = UniversoProfundo,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(tamano)) {
        val centro = Offset(size.width / 2f, size.height * 0.42f)
        val radio = size.minDimension * 0.21f
        repeat(5) { indice ->
            val angulo = -PI + indice * (PI / 4.0)
            val inicioX = centro.x + (cos(angulo) * radio * 1.62f).toFloat()
            val inicioY = centro.y + (sin(angulo) * radio * 1.62f).toFloat()
            val finX = centro.x + (cos(angulo) * radio * 2.02f).toFloat()
            val finY = centro.y + (sin(angulo) * radio * 2.02f).toFloat()
            drawLine(
                color = color,
                start = Offset(inicioX, inicioY),
                end = Offset(finX, finY),
                strokeWidth = size.minDimension * 0.065f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(
            color = color,
            radius = radio,
            center = centro
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(centro.x - radio * 0.44f, centro.y + radio * 0.72f),
            size = Size(radio * 0.88f, radio * 0.62f),
            cornerRadius = CornerRadius(radio * 0.2f)
        )
        drawLine(
            color = ZaziliWhite.copy(alpha = 0.88f),
            start = Offset(centro.x, centro.y - radio * 0.42f),
            end = Offset(centro.x, centro.y + radio * 0.22f),
            strokeWidth = radio * 0.18f,
            cap = StrokeCap.Round
        )
    }
}

/* ------------------------------------------------------------------------- */
/*  OPCIONES                                                                  */
/* ------------------------------------------------------------------------- */

@Composable
private fun OpcionRespuesta(
    letra: String,
    texto: String,
    seleccionada: Boolean,
    habilitada: Boolean,
    zen: Boolean,
    onClick: () -> Unit
) {
    val escala by animateFloatAsState(
        targetValue = if (seleccionada) 1.015f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escalaOpcion"
    )
    val colorFondo = if (seleccionada) {
        MagicalPurple
    } else {
        MaterialTheme.colorScheme.surface
    }
    val colorTexto = if (seleccionada) ZaziliWhite else MaterialTheme.colorScheme.onSurface
    val colorLetra = if (seleccionada) {
        AmarilloVibrante
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val colorTextoLetra = if (seleccionada) {
        UniversoProfundo
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val colorBorde = when {
        seleccionada -> AmarilloVibrante
        zen -> MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colorFondo,
        border = BorderStroke(width = if (seleccionada) 2.5.dp else 2.dp, color = colorBorde),
        shadowElevation = if (seleccionada && !zen) 5.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .selectable(
                selected = seleccionada,
                enabled = habilitada,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colorLetra),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letra,
                    color = colorTextoLetra,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.titleMedium,
                color = colorTexto,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  SUBRAYADO PUNTEADO                                                        */
/* ------------------------------------------------------------------------- */

private fun Modifier.punteadoInferior(color: Color): Modifier = drawBehind {
    val grosor = 2.dp.toPx()
    val y = size.height - grosor
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = grosor,
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
            phase = 0f
        )
    )
}
