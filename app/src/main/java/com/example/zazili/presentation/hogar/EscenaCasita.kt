package com.example.zazili.presentation.hogar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.mascot.EstadoVital
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.estadoZaziPara
import com.example.zazili.domain.economia.IdsArticulo
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.UniversoBrillante
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliCreamSoft
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * La casita de Zazi en 2.0: cielo de universo, luna dormilona, casa ilustrada
 * con luz cálida, telarañas cuando hay ausencia y decoraciones compradas.
 * Zazi se puede tocar para mimarla o despertarla. Todos los destellos se
 * apagan en Modo Zen.
 */
@Composable
fun EscenaCasita(
    estado: EstadoVital,
    frase: String,
    equipados: Set<String>,
    evento: EventoAfecto,
    onTocarZazi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    val transicion = rememberInfiniteTransition(label = "casitaViva")
    val centelleo by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "centelleoCasita"
    )
    val brillo = if (zen) 0.5f else centelleo
    val toqueActual by rememberUpdatedState(onTocarZazi)

    // Un mimo, una comida o un juego disparan la celebración pixel; al poco
    // rato Zazi vuelve a su pose de vuelo para no quedar estimulada de más.
    var celebrando by remember { mutableStateOf(false) }
    LaunchedEffect(evento.id) {
        if (evento.id == 0L) {
            celebrando = false
            return@LaunchedEffect
        }
        celebrando = true
        delay(2_400L)
        celebrando = false
    }
    val estadoMascota = estadoZaziPara(estado, celebrando = celebrando)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(UniversoProfundo, Color(0xFF3A1052), UniversoMedio)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            dibujarCielo(brillo, zen)
            dibujarCasa(estado, equipados, brillo, zen)
        }

        BurbujaFrase(
            frase = frase,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .fillMaxWidth(0.68f)
        )

        ZaziSpriteAnimado(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp, end = 8.dp),
            size = 170.dp,
            estado = estadoMascota,
            descripcion = if (estado.dormida) {
                "Despertar a Zazi con un cariñito"
            } else {
                "Hacerle cariños a Zazi"
            },
            onTap = toqueActual
        )

        ParticulasAfecto(
            evento = evento,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun BurbujaFrase(frase: String, modifier: Modifier = Modifier) {
    val zen = collectZenMode()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ZaziliCreamSoft,
            shadowElevation = 8.dp,
            modifier = Modifier.animateContentSize()
        ) {
            AnimatedContent(
                targetState = frase,
                transitionSpec = {
                    val duracion = if (zen) 0 else 240
                    (fadeIn(tween(duracion)) + slideInVertically(tween(duracion)) { it / 5 })
                        .togetherWith(fadeOut(tween(if (zen) 0 else 140)))
                },
                label = "fraseZazi"
            ) { texto ->
                Text(
                    text = texto,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    color = UniversoProfundo,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Canvas(modifier = Modifier.size(width = 26.dp, height = 12.dp)) {
            val colita = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(colita, ZaziliCreamSoft)
        }
    }
}

@Composable
private fun ParticulasAfecto(evento: EventoAfecto, modifier: Modifier = Modifier) {
    val zen = collectZenMode()
    val avance = remember { Animatable(0f) }

    LaunchedEffect(evento.id) {
        if (evento.id == 0L) return@LaunchedEffect
        avance.snapTo(0f)
        avance.animateTo(1f, tween(durationMillis = 1250, easing = LinearOutSlowInEasing))
    }

    if (zen || evento.id == 0L) return
    val progreso = avance.value
    if (progreso <= 0f || progreso >= 1f) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val posiciones = listOf(
            0.36f to 0.70f,
            0.50f to 0.73f,
            0.64f to 0.69f,
            0.43f to 0.66f,
            0.58f to 0.72f
        )
        posiciones.forEachIndexed { indice, (bx, by) ->
            val x = w * bx + sin((progreso * 5f + indice) * 1.7f) * w * 0.035f
            val y = h * by - h * 0.32f * progreso - indice * 6f
            val alfa = (1f - progreso).coerceIn(0f, 1f)
            val tam = w * 0.032f * (1f - progreso * 0.25f)
            when (evento.tipo) {
                TipoAfecto.MIMO -> corazoncito(
                    Offset(x, y),
                    tam,
                    RosaSuave.copy(alpha = alfa)
                )
                TipoAfecto.COMIDA -> {
                    drawOval(
                        color = AmarilloVibrante.copy(alpha = alfa),
                        topLeft = Offset(x - tam, y - tam * 0.7f),
                        size = Size(tam * 2f, tam * 1.4f)
                    )
                    drawCircle(
                        color = AmarilloOscuro.copy(alpha = alfa),
                        radius = tam * 0.28f,
                        center = Offset(x - tam * 0.3f, y)
                    )
                }
                TipoAfecto.JUEGO -> {
                    estrellaCanvas(Offset(x, y), tam, AmarilloVibrante.copy(alpha = alfa))
                    estrellaCanvas(
                        Offset(x + w * 0.035f, y + h * 0.012f),
                        tam * 0.55f,
                        MintGreen.copy(alpha = alfa)
                    )
                }
            }
        }
    }
}

private fun DrawScope.corazoncito(centro: Offset, tam: Float, color: Color) {
    val corazon = Path().apply {
        moveTo(centro.x, centro.y + tam)
        cubicTo(
            centro.x - tam * 1.4f, centro.y + tam * 0.3f,
            centro.x - tam * 1.2f, centro.y - tam,
            centro.x - tam * 0.4f, centro.y - tam * 0.9f
        )
        cubicTo(
            centro.x - tam * 0.1f, centro.y - tam * 0.85f,
            centro.x, centro.y - tam * 0.65f,
            centro.x, centro.y - tam * 0.45f
        )
        cubicTo(
            centro.x, centro.y - tam * 0.65f,
            centro.x + tam * 0.1f, centro.y - tam * 0.85f,
            centro.x + tam * 0.4f, centro.y - tam * 0.9f
        )
        cubicTo(
            centro.x + tam * 1.2f, centro.y - tam,
            centro.x + tam * 1.4f, centro.y + tam * 0.3f,
            centro.x, centro.y + tam
        )
        close()
    }
    drawPath(corazon, color)
}

private fun DrawScope.estrellaCanvas(centro: Offset, radio: Float, color: Color) {
    val curva = radio * 0.24f
    val camino = Path().apply {
        moveTo(centro.x, centro.y - radio)
        quadraticTo(centro.x + curva, centro.y - curva, centro.x + radio, centro.y)
        quadraticTo(centro.x + curva, centro.y + curva, centro.x, centro.y + radio)
        quadraticTo(centro.x - curva, centro.y + curva, centro.x - radio, centro.y)
        quadraticTo(centro.x - curva, centro.y - curva, centro.x, centro.y - radio)
        close()
    }
    drawPath(camino, color)
}

// ---------------------------------------------------------------------------
//  Cielo, luna, nubes y estrellas.
// ---------------------------------------------------------------------------

private val estrellasFijas = listOf(
    0.07f to 0.10f, 0.17f to 0.26f, 0.26f to 0.08f, 0.34f to 0.17f,
    0.41f to 0.07f, 0.49f to 0.22f, 0.57f to 0.10f, 0.64f to 0.20f,
    0.71f to 0.06f, 0.79f to 0.23f, 0.90f to 0.09f, 0.95f to 0.27f,
    0.05f to 0.35f, 0.15f to 0.46f, 0.23f to 0.36f, 0.90f to 0.42f,
    0.96f to 0.55f, 0.06f to 0.58f, 0.12f to 0.68f, 0.92f to 0.68f
)

private fun DrawScope.dibujarCielo(brillo: Float, zen: Boolean) {
    val w = size.width
    val h = size.height

    // Estrellitas.
    estrellasFijas.forEachIndexed { indice, (fx, fy) ->
        val pulso = 0.35f + 0.65f * ((brillo + indice * 0.137f) % 1f)
        val radio = w * (0.0035f + (indice % 3) * 0.0022f)
        val color = if (indice % 5 == 0) {
            AmarilloSuave
        } else {
            LavandaClara
        }
        if (indice % 7 == 0) {
            estrellaCanvas(
                Offset(w * fx, h * fy),
                radio * 3.2f,
                color.copy(alpha = 0.45f + pulso * 0.4f)
            )
        } else {
            drawCircle(
                color = color.copy(alpha = 0.35f + pulso * 0.5f),
                radius = radio,
                center = Offset(w * fx, h * fy)
            )
        }
    }

    // Luna dormilona, esquina superior derecha.
    val luna = Offset(w * 0.875f, h * 0.17f)
    val r = w * 0.062f
    drawCircle(LavandaClara.copy(alpha = 0.05f), r * 2.1f, luna)
    drawCircle(LavandaClara.copy(alpha = 0.08f), r * 1.55f, luna)
    drawCircle(AmarilloSuave, r, luna)
    drawCircle(Color(0xFFEAC98C).copy(alpha = 0.35f), r * 0.22f, Offset(luna.x - r * 0.35f, luna.y + r * 0.28f))
    drawCircle(Color(0xFFEAC98C).copy(alpha = 0.25f), r * 0.14f, Offset(luna.x + r * 0.30f, luna.y - r * 0.25f))
    val ojoLuna = Path().apply {
        moveTo(luna.x - r * 0.42f, luna.y)
        quadraticTo(luna.x - r * 0.22f, luna.y + r * 0.14f, luna.x - r * 0.04f, luna.y)
        moveTo(luna.x + r * 0.18f, luna.y)
        quadraticTo(luna.x + r * 0.38f, luna.y + r * 0.14f, luna.x + r * 0.56f, luna.y)
    }
    drawPath(
        ojoLuna,
        UniversoProfundo.copy(alpha = 0.55f),
        style = Stroke(width = r * 0.075f, cap = StrokeCap.Round)
    )
    val sonrisa = Path().apply {
        moveTo(luna.x - r * 0.22f, luna.y + r * 0.28f)
        quadraticTo(luna.x, luna.y + r * 0.42f, luna.x + r * 0.24f, luna.y + r * 0.24f)
    }
    drawPath(
        sonrisa,
        UniversoProfundo.copy(alpha = 0.5f),
        style = Stroke(width = r * 0.065f, cap = StrokeCap.Round)
    )
    drawCircle(RosaSuave.copy(alpha = 0.4f), r * 0.11f, Offset(luna.x - r * 0.60f, luna.y + r * 0.28f))
    drawCircle(RosaSuave.copy(alpha = 0.4f), r * 0.11f, Offset(luna.x + r * 0.48f, luna.y + r * 0.28f))

    if (zen) return

    // Nubecitas suaves.
    val nube = Path().apply {
        moveTo(w * 0.03f, h * 0.38f)
        cubicTo(w * 0.05f, h * 0.31f, w * 0.16f, h * 0.30f, w * 0.18f, h * 0.36f)
        cubicTo(w * 0.24f, h * 0.33f, w * 0.28f, h * 0.42f, w * 0.22f, h * 0.44f)
        lineTo(w * 0.07f, h * 0.44f)
        cubicTo(w * 0.02f, h * 0.44f, w * 0.01f, h * 0.40f, w * 0.03f, h * 0.38f)
        close()
    }
    drawPath(nube, LavandaClara.copy(alpha = 0.10f))

    val nubeDerecha = Path().apply {
        moveTo(w * 0.68f, h * 0.40f)
        cubicTo(w * 0.71f, h * 0.33f, w * 0.82f, h * 0.32f, w * 0.85f, h * 0.38f)
        cubicTo(w * 0.92f, h * 0.35f, w * 0.96f, h * 0.44f, w * 0.89f, h * 0.46f)
        lineTo(w * 0.72f, h * 0.46f)
        cubicTo(w * 0.67f, h * 0.46f, w * 0.66f, h * 0.42f, w * 0.68f, h * 0.40f)
        close()
    }
    drawPath(nubeDerecha, LavandaClara.copy(alpha = 0.08f))
}

// ---------------------------------------------------------------------------
//  Casa, huerto y decoraciones.
// ---------------------------------------------------------------------------

private fun DrawScope.dibujarCasa(
    estado: EstadoVital,
    equipados: Set<String>,
    brillo: Float,
    zen: Boolean
) {
    val w = size.width
    val h = size.height

    // Colinas del fondo.
    val colinaLejana = Path().apply {
        moveTo(0f, h * 0.70f)
        quadraticTo(w * 0.24f, h * 0.58f, w * 0.52f, h * 0.67f)
        quadraticTo(w * 0.78f, h * 0.75f, w, h * 0.64f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        colinaLejana,
        Brush.verticalGradient(
            colors = listOf(Color(0xFF2A6A4A), Color(0xFF1E4F39)),
            startY = h * 0.60f,
            endY = h
        )
    )

    val colinaMedia = Path().apply {
        moveTo(0f, h * 0.78f)
        quadraticTo(w * 0.20f, h * 0.69f, w * 0.46f, h * 0.77f)
        quadraticTo(w * 0.74f, h * 0.86f, w, h * 0.75f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        colinaMedia,
        Brush.verticalGradient(
            colors = listOf(Color(0xFF3C8C5A), Color(0xFF2C6E47)),
            startY = h * 0.72f,
            endY = h
        )
    )
    drawPath(
        Path().apply {
            moveTo(0f, h * 0.78f)
            quadraticTo(w * 0.20f, h * 0.69f, w * 0.46f, h * 0.77f)
            quadraticTo(w * 0.74f, h * 0.86f, w, h * 0.75f)
        },
        MintGreen.copy(alpha = 0.28f),
        style = Stroke(width = w * 0.008f, cap = StrokeCap.Round)
    )

    val papalote = IdsArticulo.PAPALOTE in equipados
    val cuartoGamer = IdsArticulo.CUARTO_GAMER in equipados
    val trajinera = IdsArticulo.TRAJINERA in equipados
    val chihuahua = IdsArticulo.CHIHUAHUA in equipados
    val cacomixtle = IdsArticulo.CACOMIXTLE in equipados

    // Trajinera en su canalito.
    if (trajinera) {
        drawOval(
            color = AzulCielo.copy(alpha = 0.20f),
            topLeft = Offset(w * 0.02f, h * 0.80f),
            size = Size(w * 0.25f, h * 0.09f)
        )
        drawOval(
            color = AzulCielo.copy(alpha = 0.35f),
            topLeft = Offset(w * 0.05f, h * 0.82f),
            size = Size(w * 0.16f, h * 0.035f)
        )
        val casco = Path().apply {
            moveTo(w * 0.06f, h * 0.805f)
            quadraticTo(w * 0.12f, h * 0.86f, w * 0.22f, h * 0.81f)
            quadraticTo(w * 0.17f, h * 0.785f, w * 0.06f, h * 0.805f)
            close()
        }
        drawPath(casco, UniversoBrillante)
        drawPath(casco, AmarilloVibrante, style = Stroke(width = w * 0.008f))
        val arco = Path().apply {
            moveTo(w * 0.095f, h * 0.80f)
            quadraticTo(w * 0.14f, h * 0.72f, w * 0.185f, h * 0.80f)
        }
        drawPath(arco, MintGreen, style = Stroke(width = w * 0.012f, cap = StrokeCap.Round))
        listOf(0.11f, 0.14f, 0.17f).forEachIndexed { i, fx ->
            drawCircle(
                color = listOf(RosaSuave, AmarilloVibrante, AzulCielo)[i],
                radius = w * 0.009f,
                center = Offset(w * (fx + 0.01f), h * (0.765f + i * 0.008f))
            )
        }
    }

    // Cuerpo de la casa.
    val cuerpoCasa = Path().apply {
        moveTo(w * 0.30f, h * 0.46f)
        lineTo(w * 0.70f, h * 0.46f)
        lineTo(w * 0.70f, h * 0.79f)
        quadraticTo(w * 0.70f, h * 0.81f, w * 0.68f, h * 0.81f)
        lineTo(w * 0.32f, h * 0.81f)
        quadraticTo(w * 0.30f, h * 0.81f, w * 0.30f, h * 0.79f)
        close()
    }
    drawPath(
        cuerpoCasa,
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF4DF), Color(0xFFF3DCB9)),
            startY = h * 0.46f,
            endY = h * 0.81f
        )
    )
    drawPath(
        cuerpoCasa,
        UniversoProfundo.copy(alpha = 0.25f),
        style = Stroke(width = w * 0.006f, join = StrokeJoin.Round)
    )

    // Lado derecho con profundidad.
    val costado = Path().apply {
        moveTo(w * 0.70f, h * 0.46f)
        lineTo(w * 0.78f, h * 0.50f)
        lineTo(w * 0.78f, h * 0.78f)
        lineTo(w * 0.70f, h * 0.81f)
        close()
    }
    drawPath(costado, Color(0xFFD6B98F))
    drawPath(
        costado,
        UniversoProfundo.copy(alpha = 0.16f),
        style = Stroke(width = w * 0.005f)
    )

    // Techo morado con textura.
    val techo = Path().apply {
        moveTo(w * 0.22f, h * 0.48f)
        quadraticTo(w * 0.33f, h * 0.31f, w * 0.50f, h * 0.235f)
        quadraticTo(w * 0.67f, h * 0.31f, w * 0.78f, h * 0.48f)
        lineTo(w * 0.73f, h * 0.51f)
        quadraticTo(w * 0.61f, h * 0.36f, w * 0.50f, h * 0.30f)
        quadraticTo(w * 0.39f, h * 0.36f, w * 0.27f, h * 0.51f)
        close()
    }
    drawPath(
        techo,
        Brush.verticalGradient(
            colors = listOf(UniversoBrillante, Color(0xFF5E2E7C), UniversoMedio),
            startY = h * 0.23f,
            endY = h * 0.51f
        )
    )
    clipPath(techo) {
        repeat(4) { fila ->
            val fy = h * (0.30f + fila * 0.045f)
            drawLine(
                color = LavandaMedia.copy(alpha = 0.20f),
                start = Offset(w * 0.25f, fy),
                end = Offset(w * 0.75f, fy),
                strokeWidth = w * 0.006f
            )
            var fx = w * 0.27f
            while (fx < w * 0.73f) {
                drawArc(
                    color = LavandaMedia.copy(alpha = 0.12f),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(fx, fy - w * 0.02f),
                    size = Size(w * 0.045f, w * 0.045f),
                    style = Stroke(width = w * 0.005f)
                )
                fx += w * 0.055f
            }
        }
    }
    drawPath(
        techo,
        MintGreen.copy(alpha = 0.65f),
        style = Stroke(width = w * 0.010f, join = StrokeJoin.Round)
    )
    // Remate superior.
    drawLine(
        color = LavandaClara.copy(alpha = 0.5f),
        start = Offset(w * 0.27f, h * 0.505f),
        end = Offset(w * 0.39f, h * 0.365f),
        strokeWidth = w * 0.006f,
        cap = StrokeCap.Round
    )

    // Chimenea y humo.
    drawRoundRect(
        color = Color(0xFF5A2B73),
        topLeft = Offset(w * 0.615f, h * 0.27f),
        size = Size(w * 0.075f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.012f)
    )
    drawRoundRect(
        color = LavandaMedia.copy(alpha = 0.55f),
        topLeft = Offset(w * 0.607f, h * 0.255f),
        size = Size(w * 0.092f, h * 0.035f),
        cornerRadius = CornerRadius(w * 0.010f)
    )
    if (!zen) {
        repeat(3) { i ->
            val progreso = (brillo + i * 0.33f) % 1f
            drawCircle(
                color = LavandaClara.copy(alpha = 0.18f * (1f - progreso)),
                radius = w * (0.020f + progreso * 0.035f),
                center = Offset(
                    w * (0.652f + progreso * 0.02f),
                    h * (0.245f - progreso * 0.10f)
                )
            )
        }
    }

    // Luces de fiesta bajo el alero.
    val luces = listOf(
        Triple(0.31f, 0.515f, AmarilloVibrante),
        Triple(0.395f, 0.535f, MintGreen),
        Triple(0.48f, 0.545f, RosaSuave),
        Triple(0.565f, 0.54f, AzulCielo),
        Triple(0.65f, 0.525f, AmarilloVibrante),
        Triple(0.72f, 0.505f, MintGreen)
    )
    val cable = Path().apply {
        moveTo(w * 0.30f, h * 0.505f)
        quadraticTo(w * 0.51f, h * 0.565f, w * 0.73f, h * 0.50f)
    }
    drawPath(
        cable,
        UniversoProfundo.copy(alpha = 0.35f),
        style = Stroke(width = w * 0.005f, cap = StrokeCap.Round)
    )
    luces.forEachIndexed { i, (fx, fy, color) ->
        val pulso = 0.65f + 0.35f * ((brillo + i * 0.19f) % 1f)
        drawCircle(color.copy(alpha = 0.18f), w * 0.022f, Offset(w * fx, h * fy))
        drawCircle(color.copy(alpha = pulso), w * 0.010f, Offset(w * fx, h * fy))
    }

    // Puerta con carita.
    val puerta = Path().apply {
        moveTo(w * 0.43f, h * 0.795f)
        lineTo(w * 0.43f, h * 0.655f)
        quadraticTo(w * 0.50f, h * 0.565f, w * 0.57f, h * 0.655f)
        lineTo(w * 0.57f, h * 0.795f)
        close()
    }
    drawPath(
        puerta,
        Brush.verticalGradient(
            colors = listOf(Color(0xFF7B4B94), Color(0xFF4C1A66)),
            startY = h * 0.57f,
            endY = h * 0.80f
        )
    )
    drawPath(
        puerta,
        MintGreen.copy(alpha = 0.55f),
        style = Stroke(width = w * 0.009f, join = StrokeJoin.Round)
    )
    drawCircle(AmarilloVibrante, w * 0.011f, Offset(w * 0.545f, h * 0.705f))
    drawCircle(UniversoProfundo.copy(alpha = 0.85f), w * 0.006f, Offset(w * 0.482f, h * 0.658f))
    drawCircle(UniversoProfundo.copy(alpha = 0.85f), w * 0.006f, Offset(w * 0.518f, h * 0.658f))
    val sonrisaPuerta = Path().apply {
        moveTo(w * 0.488f, h * 0.680f)
        quadraticTo(w * 0.500f, h * 0.692f, w * 0.512f, h * 0.680f)
    }
    drawPath(
        sonrisaPuerta,
        UniversoProfundo.copy(alpha = 0.7f),
        style = Stroke(width = w * 0.005f, cap = StrokeCap.Round)
    )
    drawRoundRect(
        color = MintGreen.copy(alpha = 0.35f),
        topLeft = Offset(w * 0.415f, h * 0.80f),
        size = Size(w * 0.17f, h * 0.018f),
        cornerRadius = CornerRadius(w * 0.009f)
    )

    // Ventanas cálidas.
    listOf(0.385f, 0.615f).forEach { fx ->
        drawCircle(AmarilloSuave.copy(alpha = 0.12f), w * 0.070f, Offset(w * fx, h * 0.60f))
        drawCircle(AmarilloSuave.copy(alpha = 0.20f), w * 0.052f, Offset(w * fx, h * 0.60f))
        drawCircle(AmarilloVibrante, w * 0.043f, Offset(w * fx, h * 0.60f))
        drawCircle(UniversoProfundo.copy(alpha = 0.45f), w * 0.043f, Offset(w * fx, h * 0.60f), style = Stroke(width = w * 0.007f))
        drawLine(
            color = UniversoProfundo.copy(alpha = 0.45f),
            start = Offset(w * fx, h * 0.557f),
            end = Offset(w * fx, h * 0.643f),
            strokeWidth = w * 0.006f
        )
        drawLine(
            color = UniversoProfundo.copy(alpha = 0.45f),
            start = Offset(w * (fx - 0.043f), h * 0.60f),
            end = Offset(w * (fx + 0.043f), h * 0.60f),
            strokeWidth = w * 0.006f
        )
        // Macetitas con cempasúchil.
        drawRoundRect(
            color = Color(0xFFB86A3C),
            topLeft = Offset(w * (fx - 0.038f), h * 0.648f),
            size = Size(w * 0.076f, h * 0.030f),
            cornerRadius = CornerRadius(w * 0.010f)
        )
        listOf(-0.022f, 0f, 0.022f).forEachIndexed { i, dx ->
            drawLine(
                color = VerdeDuo,
                start = Offset(w * (fx + dx), h * 0.648f),
                end = Offset(w * (fx + dx * 0.7f), h * 0.625f),
                strokeWidth = w * 0.006f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = if (i == 1) AmarilloVibrante else Color(0xFFF28A2E),
                radius = w * 0.008f,
                center = Offset(w * (fx + dx * 0.7f), h * 0.622f)
            )
        }
    }

    if (cuartoGamer) {
        drawLine(
            color = AzulCielo.copy(alpha = 0.35f),
            start = Offset(w * 0.31f, h * 0.755f),
            end = Offset(w * 0.69f, h * 0.755f),
            strokeWidth = w * 0.022f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = AzulCielo,
            start = Offset(w * 0.31f, h * 0.755f),
            end = Offset(w * 0.69f, h * 0.755f),
            strokeWidth = w * 0.006f,
            cap = StrokeCap.Round
        )
        drawCircle(RosaSuave, w * 0.012f, Offset(w * 0.31f, h * 0.755f))
        drawCircle(AzulCielo, w * 0.012f, Offset(w * 0.69f, h * 0.755f))
    }

    if (papalote) {
        val puntaCasa = Offset(w * 0.72f, h * 0.44f)
        val cometa = Offset(w * 0.84f, h * 0.33f)
        drawLine(
            color = LavandaClara.copy(alpha = 0.45f),
            start = puntaCasa,
            end = cometa,
            strokeWidth = w * 0.004f
        )
        val rombo = Path().apply {
            moveTo(cometa.x, cometa.y - h * 0.05f)
            lineTo(cometa.x + w * 0.045f, cometa.y)
            lineTo(cometa.x, cometa.y + h * 0.055f)
            lineTo(cometa.x - w * 0.04f, cometa.y)
            close()
        }
        drawPath(rombo, RosaSuave)
        drawPath(rombo, AmarilloVibrante, style = Stroke(width = w * 0.006f, join = StrokeJoin.Round))
        drawLine(
            color = AmarilloVibrante,
            start = Offset(cometa.x, cometa.y - h * 0.05f),
            end = Offset(cometa.x, cometa.y + h * 0.055f),
            strokeWidth = w * 0.004f
        )
        drawLine(
            color = AmarilloVibrante,
            start = Offset(cometa.x - w * 0.04f, cometa.y),
            end = Offset(cometa.x + w * 0.045f, cometa.y),
            strokeWidth = w * 0.004f
        )
        val cola = Path().apply {
            moveTo(cometa.x, cometa.y + h * 0.055f)
            quadraticTo(cometa.x - w * 0.04f, cometa.y + h * 0.10f, cometa.x + w * 0.01f, cometa.y + h * 0.13f)
            quadraticTo(cometa.x + w * 0.05f, cometa.y + h * 0.15f, cometa.x + w * 0.03f, cometa.y + h * 0.19f)
        }
        drawPath(cola, MintGreen, style = Stroke(width = w * 0.006f, cap = StrokeCap.Round))
    }

    if (chihuahua) {
        val perrito = Offset(w * 0.615f, h * 0.745f)
        drawCircle(Color(0xFFD9A86C), w * 0.038f, perrito)
        val orejaIzq = Path().apply {
            moveTo(perrito.x - w * 0.030f, perrito.y - w * 0.028f)
            lineTo(perrito.x - w * 0.052f, perrito.y - w * 0.075f)
            lineTo(perrito.x - w * 0.008f, perrito.y - w * 0.045f)
            close()
        }
        val orejaDer = Path().apply {
            moveTo(perrito.x + w * 0.030f, perrito.y - w * 0.028f)
            lineTo(perrito.x + w * 0.052f, perrito.y - w * 0.075f)
            lineTo(perrito.x + w * 0.008f, perrito.y - w * 0.045f)
            close()
        }
        drawPath(orejaIzq, Color(0xFFD9A86C))
        drawPath(orejaDer, Color(0xFFD9A86C))
        drawCircle(Color(0xFF6B3F22), w * 0.005f, Offset(perrito.x - w * 0.012f, perrito.y - w * 0.004f))
        drawCircle(Color(0xFF6B3F22), w * 0.005f, Offset(perrito.x + w * 0.012f, perrito.y - w * 0.004f))
        drawCircle(UniversoProfundo, w * 0.006f, Offset(perrito.x, perrito.y + w * 0.010f))
    }

    if (cacomixtle) {
        val cola = Path().apply {
            moveTo(w * 0.345f, h * 0.80f)
            cubicTo(w * 0.315f, h * 0.72f, w * 0.345f, h * 0.66f, w * 0.375f, h * 0.70f)
            cubicTo(w * 0.395f, h * 0.73f, w * 0.375f, h * 0.76f, w * 0.36f, h * 0.80f)
            close()
        }
        drawPath(cola, Color(0xFF8A6A4A))
        repeat(3) { i ->
            drawCircle(
                color = Color(0xFF4B3422).copy(alpha = 0.75f),
                radius = w * 0.008f,
                center = Offset(w * (0.345f + i * 0.010f), h * (0.744f - i * 0.012f))
            )
        }
    }

    // Telarañas: la casa extraña a Zazi, pero sin drama.
    repeat(estado.nivelTelaranas.coerceIn(0, 3)) { i ->
        val centros = listOf(
            Triple(0.315f, 0.475f, 0.055f),
            Triple(0.685f, 0.475f, 0.050f),
            Triple(0.34f, 0.28f, 0.045f)
        )
        val (fx, fy, radio) = centros[i]
        telarana(
            centro = Offset(w * fx, h * fy),
            radio = w * radio,
            alpha = 0.35f
        )
    }
}

private fun DrawScope.telarana(centro: Offset, radio: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha)
    listOf(0f, 45f, 90f, 135f).forEach { grados ->
        val rad = Math.toRadians(grados.toDouble())
        drawLine(
            color = color,
            start = centro,
            end = Offset(
                centro.x + cos(rad).toFloat() * radio,
                centro.y + sin(rad).toFloat() * radio
            ),
            strokeWidth = radio * 0.035f,
            cap = StrokeCap.Round
        )
    }
    listOf(0.38f, 0.70f, 1f).forEach { factor ->
        val r = radio * factor
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centro.x - r, centro.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = radio * 0.028f)
        )
    }
}
