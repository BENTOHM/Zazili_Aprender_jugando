package com.example.zazili.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.UniversoBrillante
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziTinta
import com.example.zazili.ui.theme.ZaziliWhite

// ============================================================================
//  Iconos 2.0: piezas sueltas para la casita, la saga y los medidores.
//  Todos se dibujan con Canvas; nada de vectores genéricos.
// ============================================================================

private val CaraTinta = UniversoProfundo
private val MejillaIcono = Color(0xFFF5A8BC)

@Composable
private fun IconoBase(
    size: Dp,
    modifier: Modifier,
    dibujo: androidx.compose.ui.graphics.drawscope.DrawScope.(Float, Float, Float) -> Unit
) {
    Canvas(modifier = modifier.size(size)) {
        val s = minOf(this.size.width, this.size.height)
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        dibujo(s, cx, cy)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.ojitos(
    cx: Float,
    cy: Float,
    s: Float,
    dx: Float = 0.09f,
    radio: Float = 0.035f
) {
    listOf(-1f, 1f).forEach { lado ->
        drawCircle(CaraTinta, radio * s, Offset(cx + lado * dx * s, cy))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.sonrisita(
    cx: Float,
    cy: Float,
    s: Float,
    ancho: Float = 0.06f
) {
    val boca = Path().apply {
        moveTo(cx - ancho * s, cy)
        quadraticTo(cx, cy + ancho * 0.7f * s, cx + ancho * s, cy)
    }
    drawPath(
        boca,
        CaraTinta,
        style = Stroke(width = 0.014f * s, cap = StrokeCap.Round)
    )
}

@Composable
fun KawaiiCoinIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        drawCircle(AmarilloOscuro, 0.47f * s, Offset(cx, cy + 0.02f * s))
        drawCircle(AmarilloVibrante, 0.43f * s, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.35f), 0.30f * s, Offset(cx, cy), style = Stroke(width = 0.035f * s))
        val destello = Path().apply {
            moveTo(cx, cy - 0.25f * s)
            quadraticTo(cx + 0.06f * s, cy - 0.02f * s, cx + 0.24f * s, cy)
            quadraticTo(cx + 0.06f * s, cy + 0.02f * s, cx, cy + 0.25f * s)
            quadraticTo(cx - 0.06f * s, cy + 0.02f * s, cx - 0.24f * s, cy)
            quadraticTo(cx - 0.06f * s, cy - 0.02f * s, cx, cy - 0.25f * s)
            close()
        }
        drawPath(destello, ZaziliWhite)
        drawCircle(Color.White.copy(alpha = 0.6f), 0.055f * s, Offset(cx - 0.24f * s, cy - 0.27f * s))
    }
}

@Composable
fun KawaiiHeartIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        val corazon = Path().apply {
            moveTo(cx, cy + 0.38f * s)
            cubicTo(cx - 0.50f * s, cy + 0.08f * s, cx - 0.45f * s, cy - 0.38f * s, cx - 0.16f * s, cy - 0.36f * s)
            cubicTo(cx - 0.05f * s, cy - 0.35f * s, cx, cy - 0.25f * s, cx, cy - 0.18f * s)
            cubicTo(cx, cy - 0.25f * s, cx + 0.05f * s, cy - 0.35f * s, cx + 0.16f * s, cy - 0.36f * s)
            cubicTo(cx + 0.45f * s, cy - 0.38f * s, cx + 0.50f * s, cy + 0.08f * s, cx, cy + 0.38f * s)
            close()
        }
        drawPath(corazon, Color(0xFFF26D8D))
        drawPath(
            corazon,
            Color(0xFFC94A6B).copy(alpha = 0.55f),
            style = Stroke(width = 0.035f * s, join = StrokeJoin.Round)
        )
        drawOval(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(cx - 0.30f * s, cy - 0.24f * s),
            size = Size(0.14f * s, 0.09f * s)
        )
        ojitos(cx, cy + 0.02f * s, s, dx = 0.10f, radio = 0.026f)
        sonrisita(cx, cy + 0.14f * s, s, ancho = 0.045f)
    }
}

@Composable
fun KawaiiBoltIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        val rayo = Path().apply {
            moveTo(cx + 0.06f * s, cy - 0.46f * s)
            lineTo(cx - 0.28f * s, cy + 0.05f * s)
            lineTo(cx - 0.02f * s, cy + 0.05f * s)
            lineTo(cx - 0.12f * s, cy + 0.46f * s)
            lineTo(cx + 0.30f * s, cy - 0.10f * s)
            lineTo(cx + 0.04f * s, cy - 0.10f * s)
            close()
        }
        drawPath(
            rayo,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color(0xFFFFE066), AmarilloVibrante),
                startY = cy - 0.46f * s,
                endY = cy + 0.46f * s
            )
        )
        drawPath(
            rayo,
            AmarilloOscuro,
            style = Stroke(width = 0.035f * s, join = StrokeJoin.Round)
        )
        drawCircle(Color.White.copy(alpha = 0.55f), 0.045f * s, Offset(cx - 0.055f * s, cy - 0.16f * s))
    }
}

@Composable
fun KawaiiTacoIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        drawOval(
            color = Color(0xFF9B5B2B).copy(alpha = 0.30f),
            topLeft = Offset(cx - 0.42f * s, cy + 0.28f * s),
            size = Size(0.84f * s, 0.14f * s)
        )
        val tortilla = Path().apply {
            moveTo(cx - 0.46f * s, cy + 0.24f * s)
            cubicTo(cx - 0.48f * s, cy - 0.22f * s, cx + 0.48f * s, cy - 0.22f * s, cx + 0.46f * s, cy + 0.24f * s)
            quadraticTo(cx, cy + 0.38f * s, cx - 0.46f * s, cy + 0.24f * s)
            close()
        }
        drawPath(tortilla, Color(0xFFF7CE73))
        drawPath(
            tortilla,
            Color(0xFFC98F34),
            style = Stroke(width = 0.035f * s, join = StrokeJoin.Round)
        )
        // Relleno asomado.
        val lechuga = Path().apply {
            moveTo(cx - 0.36f * s, cy - 0.02f * s)
            cubicTo(cx - 0.28f * s, cy - 0.24f * s, cx + 0.28f * s, cy - 0.24f * s, cx + 0.36f * s, cy - 0.02f * s)
            lineTo(cx + 0.26f * s, cy + 0.08f * s)
            lineTo(cx - 0.26f * s, cy + 0.08f * s)
            close()
        }
        drawPath(lechuga, VerdeDuo)
        drawLine(
            color = Color(0xFFF06A5A),
            start = Offset(cx - 0.20f * s, cy - 0.02f * s),
            end = Offset(cx + 0.18f * s, cy - 0.01f * s),
            strokeWidth = 0.055f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFFE08A),
            start = Offset(cx - 0.10f * s, cy + 0.04f * s),
            end = Offset(cx + 0.08f * s, cy + 0.035f * s),
            strokeWidth = 0.035f * s,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun KawaiiCenoteIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        val gota = Path().apply {
            moveTo(cx, cy - 0.46f * s)
            cubicTo(cx + 0.10f * s, cy - 0.22f * s, cx + 0.38f * s, cy + 0.02f * s, cx + 0.38f * s, cy + 0.18f * s)
            cubicTo(cx + 0.38f * s, cy + 0.38f * s, cx + 0.20f * s, cy + 0.48f * s, cx, cy + 0.48f * s)
            cubicTo(cx - 0.20f * s, cy + 0.48f * s, cx - 0.38f * s, cy + 0.38f * s, cx - 0.38f * s, cy + 0.18f * s)
            cubicTo(cx - 0.38f * s, cy + 0.02f * s, cx - 0.10f * s, cy - 0.22f * s, cx, cy - 0.46f * s)
            close()
        }
        drawPath(
            gota,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(AzulCielo, Color(0xFF4FB6E8)),
                startY = cy - 0.46f * s,
                endY = cy + 0.48f * s
            )
        )
        drawPath(
            gota,
            Color(0xFF2D87B8).copy(alpha = 0.55f),
            style = Stroke(width = 0.032f * s, join = StrokeJoin.Round)
        )
        drawOval(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(cx - 0.20f * s, cy - 0.16f * s),
            size = Size(0.11f * s, 0.16f * s)
        )
        ojitos(cx, cy + 0.08f * s, s, dx = 0.11f, radio = 0.028f)
        sonrisita(cx, cy + 0.19f * s, s, ancho = 0.045f)
    }
}

@Composable
fun KawaiiPyramidIcon(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        drawOval(
            color = Color(0xFF7B4B94).copy(alpha = 0.22f),
            topLeft = Offset(cx - 0.44f * s, cy + 0.37f * s),
            size = Size(0.88f * s, 0.12f * s)
        )
        repeat(3) { i ->
            val ancho = (0.36f - i * 0.08f) * s
            val alto = 0.19f * s
            val top = cy + 0.34f * s - (i + 1) * 0.17f * s
            val color = listOf(UniversoBrillante, Color(0xFF9B6BB5), LavandaMedia)[i]
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - ancho, top),
                size = Size(ancho * 2f, alto),
                cornerRadius = CornerRadius(0.035f * s)
            )
            drawRoundRect(
                color = UniversoProfundo.copy(alpha = 0.22f),
                topLeft = Offset(cx - ancho, top),
                size = Size(ancho * 2f, alto),
                cornerRadius = CornerRadius(0.035f * s),
                style = Stroke(width = 0.022f * s)
            )
            repeat(3 - i) { j ->
                drawCircle(
                    color = MintGreen.copy(alpha = 0.75f),
                    radius = 0.012f * s,
                    center = Offset(cx - ancho + 0.055f * s + j * 0.07f * s, top + alto * 0.5f)
                )
            }
        }
        drawCircle(MintGreen, 0.065f * s, Offset(cx, cy - 0.25f * s))
        drawCircle(MintGreen, 0.03f * s, Offset(cx, cy - 0.25f * s), style = Stroke(width = 0.018f * s))
        drawLine(
            color = MintGreen,
            start = Offset(cx, cy - 0.32f * s),
            end = Offset(cx, cy - 0.12f * s),
            strokeWidth = 0.018f * s,
            cap = StrokeCap.Round
        )
        drawCircle(Color.White.copy(alpha = 0.65f), 0.018f * s, Offset(cx - 0.02f * s, cy - 0.27f * s))
    }
}

@Composable
fun KawaiiLockIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = LavandaMedia
) {
    IconoBase(size, modifier) { s, cx, cy ->
        val arco = Path().apply {
            moveTo(cx - 0.20f * s, cy - 0.04f * s)
            lineTo(cx - 0.20f * s, cy - 0.20f * s)
            cubicTo(cx - 0.20f * s, cy - 0.46f * s, cx + 0.20f * s, cy - 0.46f * s, cx + 0.20f * s, cy - 0.20f * s)
            lineTo(cx + 0.20f * s, cy - 0.04f * s)
        }
        drawPath(
            arco,
            color.copy(alpha = 0.75f),
            style = Stroke(width = 0.075f * s, cap = StrokeCap.Round)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(cx - 0.30f * s, cy - 0.08f * s),
            size = Size(0.60f * s, 0.48f * s),
            cornerRadius = CornerRadius(0.11f * s)
        )
        drawCircle(ZaziTinta.copy(alpha = 0.7f), 0.075f * s, Offset(cx, cy + 0.10f * s))
        drawRoundRect(
            color = ZaziTinta.copy(alpha = 0.7f),
            topLeft = Offset(cx - 0.025f * s, cy + 0.10f * s),
            size = Size(0.05f * s, 0.13f * s),
            cornerRadius = CornerRadius(0.02f * s)
        )
        drawCircle(Color.White.copy(alpha = 0.35f), 0.028f * s, Offset(cx + 0.15f * s, cy + 0.02f * s))
    }
}

@Composable
fun KawaiiStarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    color: Color = AmarilloVibrante
) {
    IconoBase(size, modifier) { s, cx, cy ->
        val exterior = 0.48f * s
        val interior = 0.22f * s
        val estrella = Path()
        repeat(10) { i ->
            val angulo = Math.toRadians((i * 36 - 90).toDouble())
            val radio = if (i % 2 == 0) exterior else interior
            val px = cx + (radio * kotlin.math.cos(angulo)).toFloat()
            val py = cy + (radio * kotlin.math.sin(angulo)).toFloat()
            if (i == 0) estrella.moveTo(px, py) else estrella.lineTo(px, py)
        }
        estrella.close()
        drawPath(estrella, color)
        drawPath(
            estrella,
            AmarilloOscuro.copy(alpha = if (color == AmarilloVibrante) 0.8f else 0.35f),
            style = Stroke(width = 0.03f * s, join = StrokeJoin.Round)
        )
        drawCircle(Color.White.copy(alpha = 0.55f), 0.045f * s, Offset(cx - 0.08f * s, cy - 0.10f * s))
    }
}

@Composable
fun KawaiiCheckIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    color: Color = ZaziliWhite
) {
    IconoBase(size, modifier) { s, cx, cy ->
        val palomita = Path().apply {
            moveTo(cx - 0.26f * s, cy + 0.01f * s)
            lineTo(cx - 0.07f * s, cy + 0.20f * s)
            lineTo(cx + 0.29f * s, cy - 0.21f * s)
        }
        drawPath(
            palomita,
            color,
            style = Stroke(width = 0.11f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun KawaiiArrowIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = ZaziliWhite
) {
    IconoBase(size, modifier) { s, cx, cy ->
        val flecha = Path().apply {
            moveTo(cx - 0.30f * s, cy)
            lineTo(cx + 0.26f * s, cy)
            moveTo(cx + 0.04f * s, cy - 0.20f * s)
            lineTo(cx + 0.26f * s, cy)
            lineTo(cx + 0.04f * s, cy + 0.20f * s)
        }
        drawPath(
            flecha,
            color,
            style = Stroke(width = 0.08f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun KawaiiKeyIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = LavandaClara
) {
    IconoBase(size, modifier) { s, cx, cy ->
        drawCircle(color, 0.20f * s, Offset(cx - 0.14f * s, cy - 0.10f * s), style = Stroke(width = 0.10f * s))
        drawCircle(Color.Transparent, 0.06f * s, Offset(cx - 0.14f * s, cy - 0.10f * s))
        drawLine(
            color = color,
            start = Offset(cx - 0.01f * s, cy + 0.03f * s),
            end = Offset(cx + 0.30f * s, cy + 0.33f * s),
            strokeWidth = 0.09f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx + 0.15f * s, cy + 0.17f * s),
            end = Offset(cx + 0.25f * s, cy + 0.07f * s),
            strokeWidth = 0.08f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx + 0.23f * s, cy + 0.25f * s),
            end = Offset(cx + 0.33f * s, cy + 0.15f * s),
            strokeWidth = 0.08f * s,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun KawaiiCloudStressIcon(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        drawCircle(LavandaMedia, 0.20f * s, Offset(cx - 0.20f * s, cy - 0.04f * s))
        drawCircle(LavandaClara, 0.26f * s, Offset(cx, cy - 0.12f * s))
        drawCircle(LavandaMedia, 0.18f * s, Offset(cx + 0.22f * s, cy - 0.02f * s))
        drawRoundRect(
            color = LavandaClara,
            topLeft = Offset(cx - 0.38f * s, cy - 0.10f * s),
            size = Size(0.76f * s, 0.34f * s),
            cornerRadius = CornerRadius(0.16f * s)
        )
        drawCircle(UniversoProfundo.copy(alpha = 0.8f), 0.02f * s, Offset(cx - 0.10f * s, cy + 0.05f * s))
        drawCircle(UniversoProfundo.copy(alpha = 0.8f), 0.02f * s, Offset(cx + 0.10f * s, cy + 0.05f * s))
        val boca = Path().apply {
            moveTo(cx - 0.05f * s, cy + 0.13f * s)
            quadraticTo(cx, cy + 0.10f * s, cx + 0.05f * s, cy + 0.13f * s)
        }
        drawPath(boca, UniversoProfundo.copy(alpha = 0.8f), style = Stroke(width = 0.014f * s, cap = StrokeCap.Round))
        // Gotitas de lluvia suave.
        listOf(-0.18f, 0f, 0.18f).forEachIndexed { i, dx ->
            drawLine(
                color = AzulCielo,
                start = Offset(cx + dx * s, cy + 0.28f * s),
                end = Offset(cx + (dx - 0.03f) * s, cy + 0.40f * s + i * 0.015f * s),
                strokeWidth = 0.035f * s,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun KawaiiTrashMonsterIcon(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        val cuerpo = Path().apply {
            moveTo(cx - 0.34f * s, cy - 0.20f * s)
            cubicTo(cx - 0.42f * s, cy + 0.22f * s, cx - 0.24f * s, cy + 0.44f * s, cx, cy + 0.44f * s)
            cubicTo(cx + 0.24f * s, cy + 0.44f * s, cx + 0.42f * s, cy + 0.22f * s, cx + 0.34f * s, cy - 0.20f * s)
            close()
        }
        drawPath(
            cuerpo,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(Color(0xFF7ECF8A), Color(0xFF3E8E55)),
                startY = cy - 0.32f * s,
                endY = cy + 0.44f * s
            )
        )
        drawCircle(Color.White, 0.10f * s, Offset(cx - 0.14f * s, cy - 0.02f * s))
        drawCircle(Color.White, 0.10f * s, Offset(cx + 0.14f * s, cy - 0.02f * s))
        drawCircle(UniversoProfundo, 0.05f * s, Offset(cx - 0.12f * s, cy))
        drawCircle(UniversoProfundo, 0.05f * s, Offset(cx + 0.16f * s, cy))
        val boca = Path().apply {
            moveTo(cx - 0.14f * s, cy + 0.18f * s)
            quadraticTo(cx, cy + 0.30f * s, cx + 0.14f * s, cy + 0.18f * s)
        }
        drawPath(boca, UniversoProfundo, style = Stroke(width = 0.03f * s, cap = StrokeCap.Round))
        // Basurita pegada.
        drawRoundRect(
            color = AmarilloVibrante,
            topLeft = Offset(cx + 0.20f * s, cy - 0.34f * s),
            size = Size(0.18f * s, 0.12f * s),
            cornerRadius = CornerRadius(0.03f * s)
        )
        drawCircle(Color(0xFFF06A5A), 0.045f * s, Offset(cx - 0.30f * s, cy + 0.20f * s))
    }
}

@Composable
fun KawaiiGlitchIcon(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        drawRoundRect(
            color = UniversoProfundo,
            topLeft = Offset(cx - 0.36f * s, cy - 0.34f * s),
            size = Size(0.72f * s, 0.68f * s),
            cornerRadius = CornerRadius(0.12f * s)
        )
        drawRoundRect(
            color = VerdeDuo,
            topLeft = Offset(cx - 0.30f * s, cy - 0.28f * s),
            size = Size(0.60f * s, 0.44f * s),
            cornerRadius = CornerRadius(0.07f * s)
        )
        drawCircle(UniversoProfundo, 0.07f * s, Offset(cx - 0.12f * s, cy - 0.08f * s))
        drawCircle(UniversoProfundo, 0.07f * s, Offset(cx + 0.12f * s, cy - 0.08f * s))
        val boca = Path().apply {
            moveTo(cx - 0.15f * s, cy + 0.03f * s)
            lineTo(cx - 0.05f * s, cy + 0.09f * s)
            lineTo(cx + 0.04f * s, cy + 0.01f * s)
            lineTo(cx + 0.15f * s, cy + 0.08f * s)
        }
        drawPath(boca, UniversoProfundo, style = Stroke(width = 0.025f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawRoundRect(
            color = RosaSuave,
            topLeft = Offset(cx - 0.26f * s, cy + 0.26f * s),
            size = Size(0.18f * s, 0.08f * s),
            cornerRadius = CornerRadius(0.02f * s)
        )
        drawRoundRect(
            color = AzulCielo,
            topLeft = Offset(cx + 0.08f * s, cy + 0.26f * s),
            size = Size(0.20f * s, 0.08f * s),
            cornerRadius = CornerRadius(0.02f * s)
        )
    }
}

@Composable
fun KawaiiNahualIcon(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    IconoBase(size, modifier) { s, cx, cy ->
        // Orejitas.
        val oreja = Path().apply {
            moveTo(cx - 0.26f * s, cy - 0.28f * s)
            lineTo(cx - 0.32f * s, cy - 0.48f * s)
            lineTo(cx - 0.10f * s, cy - 0.34f * s)
            close()
        }
        val orejaDer = Path().apply {
            moveTo(cx + 0.26f * s, cy - 0.28f * s)
            lineTo(cx + 0.32f * s, cy - 0.48f * s)
            lineTo(cx + 0.10f * s, cy - 0.34f * s)
            close()
        }
        drawPath(oreja, UniversoBrillante)
        drawPath(orejaDer, UniversoBrillante)
        drawCircle(UniversoBrillante, 0.36f * s, Offset(cx, cy))
        drawCircle(LavandaClara, 0.29f * s, Offset(cx, cy))
        drawCircle(UniversoProfundo, 0.075f * s, Offset(cx - 0.13f * s, cy - 0.06f * s))
        drawCircle(UniversoProfundo, 0.075f * s, Offset(cx + 0.13f * s, cy - 0.06f * s))
        drawCircle(ZaziliWhite.copy(alpha = 0.85f), 0.022f * s, Offset(cx - 0.10f * s, cy - 0.09f * s))
        drawCircle(ZaziliWhite.copy(alpha = 0.85f), 0.022f * s, Offset(cx + 0.16f * s, cy - 0.09f * s))
        drawCircle(VerdeDuo, 0.06f * s, Offset(cx, cy + 0.10f * s))
        val bucles = Path().apply {
            moveTo(cx - 0.16f * s, cy + 0.20f * s)
            quadraticTo(cx - 0.08f * s, cy + 0.28f * s, cx, cy + 0.20f * s)
            quadraticTo(cx + 0.08f * s, cy + 0.28f * s, cx + 0.16f * s, cy + 0.20f * s)
        }
        drawPath(bucles, UniversoProfundo, style = Stroke(width = 0.024f * s, cap = StrokeCap.Round))
    }
}
