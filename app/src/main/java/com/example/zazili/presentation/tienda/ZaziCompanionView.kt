package com.example.zazili.presentation.tienda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.mascot.ZaziVitals
import com.example.zazili.core.tiempo.DayPhase
import com.example.zazili.core.tiempo.recordarFaseDelDia
import com.example.zazili.core.ui.CelebrationKonfetti
import com.example.zazili.domain.tienda.EscenaFondo
import com.example.zazili.domain.tienda.FondoTienda
import com.example.zazili.domain.tienda.RopaTienda
import com.example.zazili.presentation.hogar.FondoHabitacionZazi
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.ZaziliCreamSoft
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Zazi dentro de su casita: escenario procedural, accesorios, manchitas de
 * suciedad, líneas de olor, texto flotante y confeti.
 *
 * La vista NO decide nada del estado: recibe [vitals] y la ropa puesta, y
 * reporta su rectángulo global con [onZonaCambiada] para que la pantalla haga
 * la prueba de caída del arrastre. Todas las animaciones decorativas se
 * detienen en modo Zen; el texto y el estado siempre se anuncian.
 */
@Composable
fun ZaziCompanionView(
    vitals: ZaziVitals,
    fondo: FondoTienda?,
    ropa: List<RopaTienda>,
    modifier: Modifier = Modifier,
    faseDelDia: DayPhase = recordarFaseDelDia(),
    mensajeFlotante: String? = null,
    tokenFeedback: Int = 0,
    confetiTrigger: Int = 0,
    resaltado: Boolean = false,
    celebrando: Boolean = false,
    pensando: Boolean = false,
    onTocarZazi: () -> Unit = {},
    onAlternarSueno: (() -> Unit)? = null,
    onZonaCambiada: (Rect) -> Unit = {}
) {
    val zen = collectZenMode()
    val escena = fondo?.escena ?: EscenaFondo.UNIVERSO
    val forma = RoundedCornerShape(28.dp)
    var mostrarMensaje by remember { mutableStateOf(false) }

    LaunchedEffect(tokenFeedback) {
        if (tokenFeedback <= 0 || mensajeFlotante.isNullOrBlank()) return@LaunchedEffect
        mostrarMensaje = true
        delay(1600)
        mostrarMensaje = false
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordenadas -> onZonaCambiada(coordenadas.boundsInRoot()) }
            .clip(forma)
            .background(ZaziliCreamSoft)
            .border(
                width = if (resaltado) 4.dp else 2.dp,
                color = if (resaltado) AmarilloVibrante else LavandaMedia.copy(alpha = 0.55f),
                shape = forma
            )
            .semantics(mergeDescendants = false) {
                stateDescription = "Pancita ${vitals.pancitaPct} por ciento, " +
                    "felicidad ${vitals.felicidadPct} por ciento, " +
                    "energía ${vitals.energiaPct} por ciento, " +
                    "higiene ${vitals.higienePct} por ciento" +
                    if (vitals.dormida) ", Zazi está dormida" else ""
            }
    ) {
        // Capa 1: lámina real de la hora de Zazi, confinada a este cuarto.
        FondoHabitacionZazi(
            faseDelDia = faseDelDia,
            zen = zen,
            modifier = Modifier.matchParentSize()
        )

        // Capa 2: si el niño compró y equipó un fondo, cubre la lámina.
        if (fondo != null) {
            EscenarioCasita(escena = escena, modifier = Modifier.matchParentSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = vitals.fraseEstado(),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.86f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = UniversoProfundo,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier.size(168.dp),
                contentAlignment = Alignment.Center
            ) {
                ZaziPetRenderer(
                    ropa = ropa,
                    estado = vitals.estadoVisual(celebrando = celebrando, pensando = pensando),
                    tamano = 168.dp,
                    descripcion = if (vitals.dormida) {
                        "Zazi está dormida; toca el botón del sol para despertarla"
                    } else {
                        "Zazi, tu colibrí. Tócala para hacerle cariño"
                    },
                    onTap = onTocarZazi
                )

                if (vitals.suciedad > 0.04f) {
                    if (zen) {
                        LienzoSuciedad(suciedad = vitals.suciedad, fase = 0.35f)
                    } else {
                        LienzoSuciedadAnimada(suciedad = vitals.suciedad)
                    }
                }
            }
        }

        if (resaltado) {
            Text(
                text = "Suelta aquí",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp)
                    .clip(CircleShape)
                    .background(AmarilloVibrante)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
        }

        AnimatedVisibility(
            visible = mostrarMensaje && !mensajeFlotante.isNullOrBlank(),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-60).dp),
            enter = fadeIn(tween(160)) + slideInVertically(tween(240)) { alto -> alto / 3 },
            exit = fadeOut(tween(260)) + slideOutVertically(tween(320)) { alto -> -alto / 2 }
        ) {
            Text(
                text = mensajeFlotante.orEmpty(),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MagicalPurple,
                fontWeight = FontWeight.Black
            )
        }

        onAlternarSueno?.let { alternar ->
            val accion = if (vitals.dormida) "Despertar a Zazi" else "Arropar a Zazi"
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (vitals.dormida) AmarilloVibrante else LavandaMedia)
                    .clickable(onClick = alternar)
                    .semantics {
                        role = Role.Button
                        contentDescription = accion
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (vitals.dormida) "☀️" else "🌙",
                    fontSize = 22.sp
                )
            }
        }

        CelebrationKonfetti(
            trigger = confetiTrigger,
            modifier = Modifier.matchParentSize()
        )
    }
}

// ---------------------------------------------------------------------------
//  Escenario procedural
// ---------------------------------------------------------------------------

@Composable
private fun EscenarioCasita(escena: EscenaFondo, modifier: Modifier = Modifier) {
    val estrellas = remember(escena) {
        val azar = Random(escena.ordinal * 7_919 + 13)
        List(22) {
            Triple(azar.nextFloat(), azar.nextFloat() * 0.72f, 0.6f + azar.nextFloat() * 1.6f)
        }
    }
    val acento = Color(escena.colorSuelo)

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(escena.colorInicio), Color(escena.colorFin))
            )
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (escena.conEstrellas) {
                estrellas.forEach { (x, y, radio) ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f + y * 0.6f),
                        radius = radio.dp.toPx(),
                        center = Offset(x * size.width, y * size.height)
                    )
                }
            }

            when (escena) {
                EscenaFondo.GALAXIA, EscenaFondo.UNIVERSO, EscenaFondo.NEBULOSA -> {
                    drawCircle(
                        color = acento.copy(alpha = 0.30f),
                        radius = size.minDimension * 0.16f,
                        center = Offset(size.width * 0.82f, size.height * 0.24f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.20f),
                        radius = size.minDimension * 0.22f,
                        center = Offset(size.width * 0.18f, size.height * 0.34f)
                    )
                }

                EscenaFondo.PLAYA, EscenaFondo.ISLA -> {
                    drawCircle(
                        color = AmarilloVibrante.copy(alpha = 0.55f),
                        radius = size.minDimension * 0.14f,
                        center = Offset(size.width * 0.78f, size.height * 0.20f)
                    )
                }

                EscenaFondo.VOLCAN, EscenaFondo.MONTANA, EscenaFondo.CRISTAL -> {
                    val pico = Path().apply {
                        moveTo(size.width * 0.02f, size.height * 0.78f)
                        lineTo(size.width * 0.30f, size.height * 0.30f)
                        lineTo(size.width * 0.55f, size.height * 0.78f)
                        close()
                    }
                    drawPath(pico, color = acento.copy(alpha = 0.35f))
                    val picoSecundario = Path().apply {
                        moveTo(size.width * 0.42f, size.height * 0.80f)
                        lineTo(size.width * 0.66f, size.height * 0.42f)
                        lineTo(size.width * 0.96f, size.height * 0.80f)
                        close()
                    }
                    drawPath(picoSecundario, color = acento.copy(alpha = 0.22f))
                }

                EscenaFondo.BIBLIOTECA -> {
                    repeat(3) { indice ->
                        val ancho = size.width * 0.16f
                        drawRect(
                            color = acento.copy(alpha = 0.30f),
                            topLeft = Offset(
                                size.width * (0.08f + indice * 0.32f),
                                size.height * 0.22f
                            ),
                            size = Size(ancho, size.height * 0.46f)
                        )
                    }
                }

                EscenaFondo.ARCADE -> {
                    repeat(5) { fila ->
                        val y = size.height * (0.56f + fila * 0.06f)
                        drawLine(
                            color = acento.copy(alpha = 0.18f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                else -> Unit
            }

            drawOval(
                color = acento.copy(alpha = 0.42f),
                topLeft = Offset(-size.width * 0.2f, size.height * 0.76f),
                size = Size(size.width * 1.4f, size.height * 0.42f)
            )
        }

        Text(
            text = escena.motivo,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            fontSize = 20.sp
        )
        Text(
            text = escena.motivo,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .offset(y = (-30).dp),
            fontSize = 16.sp
        )
        Text(
            text = escena.motivo,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 6.dp),
            fontSize = 18.sp
        )
    }
}

// ---------------------------------------------------------------------------
//  Suciedad procedural
// ---------------------------------------------------------------------------

@Composable
private fun LienzoSuciedadAnimada(suciedad: Float) {
    val transicion = rememberInfiniteTransition(label = "olorZazi")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "faseOlorZazi"
    )
    LienzoSuciedad(suciedad = suciedad, fase = fase)
}

@Composable
private fun LienzoSuciedad(suciedad: Float, fase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val alphaMancha = (suciedad * 0.65f).coerceIn(0f, 0.65f)
        val manchas = listOf(
            Offset(0.34f, 0.58f), Offset(0.62f, 0.50f),
            Offset(0.48f, 0.72f), Offset(0.70f, 0.66f)
        )
        manchas.forEachIndexed { indice, relativa ->
            val radio = size.minDimension * (0.045f + indice * 0.008f)
            drawCircle(
                color = Color(0xFF6B4A2F).copy(alpha = alphaMancha),
                radius = radio,
                center = Offset(relativa.x * size.width, relativa.y * size.height)
            )
            drawCircle(
                color = Color(0xFF8A6642).copy(alpha = alphaMancha * 0.7f),
                radius = radio * 0.45f,
                center = Offset(
                    relativa.x * size.width + radio * 0.5f,
                    relativa.y * size.height - radio * 0.4f
                )
            )
        }

        if (alphaMancha <= 0.02f) return@Canvas
        val colorOlor = Color(0xFF7E8F4A).copy(alpha = (0.25f + suciedad * 0.4f))
        repeat(3) { indice ->
            val baseX = size.width * (0.30f + indice * 0.17f)
            val baseY = size.height * 0.12f
            var x = baseX
            var y = baseY
            repeat(4) { tramo ->
                val desfase = sin(fase * 2.0 * PI + indice + tramo * 0.7).toFloat()
                val siguienteX = baseX + desfase * size.width * 0.035f
                val siguienteY = y + size.height * 0.065f
                drawLine(
                    color = colorOlor,
                    start = Offset(x, y),
                    end = Offset(siguienteX, siguienteY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                x = siguienteX
                y = siguienteY
            }
        }
    }
}
