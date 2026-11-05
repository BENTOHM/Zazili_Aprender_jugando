package com.example.zazili.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
//  KawaiiIcons — iconos dibujados a mano con Canvas/Path (estilo chibi-flat).
//  Nada de Icons.Default: cada trazo es propio, redondeado y con carita.
//  Paleta: Morado Mágico #7B4B94 · Menta Pastel #A8E6CF · Crema #FCF9F2.
// ============================================================================

private val IconPurple = Color(0xFF7B4B94)
private val IconPurpleDeep = Color(0xFF5E3A73)
private val IconPurpleSoft = Color(0xFFC9B6E4)
private val IconMint = Color(0xFFA8E6CF)
private val IconMintDeep = Color(0xFF8BD1B6)
private val IconCream = Color(0xFFFCF9F2)
private val IconFace = Color(0xFF3B2F4F)
private val IconBlush = Color(0xFFF5A8BC)

@Composable
private fun KawaiiIconCanvas(
    modifier: Modifier,
    size: Dp,
    draw: DrawScope.(Float, Float, Float) -> Unit
) {
    Canvas(modifier = modifier.size(size)) {
        val s = minOf(this.size.width, this.size.height)
        draw(s, this.size.width / 2f, this.size.height / 2f)
    }
}

// ---------------------------------------------------------------------------
//  Carita kawaii compartida.
// ---------------------------------------------------------------------------

private fun DrawScope.kawaiiEyes(
    cx: Float,
    cy: Float,
    s: Float,
    dx: Float,
    dy: Float,
    eyeR: Float,
    pupilR: Float
) {
    listOf(-1f, 1f).forEach { side ->
        val ex = cx + side * dx * s
        val ey = cy + dy * s
        drawCircle(Color.White, eyeR * s, Offset(ex, ey))
        drawCircle(IconFace, pupilR * s, Offset(ex, ey))
        drawCircle(Color.White, 0.23f * pupilR * s, Offset(ex - 0.45f * pupilR * s, ey - 0.5f * pupilR * s))
    }
}

private fun DrawScope.kawaiiSmile(
    cx: Float,
    cy: Float,
    s: Float,
    halfW: Float,
    depth: Float,
    width: Float = 0.024f
) {
    val path = Path().apply {
        moveTo(cx - halfW * s, cy)
        quadraticBezierTo(cx, cy + depth * s, cx + halfW * s, cy)
    }
    drawPath(path, IconFace, style = Stroke(width = width * s, cap = StrokeCap.Round))
}

private fun DrawScope.kawaiiBlush(cx: Float, cy: Float, s: Float, dx: Float, dy: Float) {
    listOf(-1f, 1f).forEach { side ->
        drawCircle(IconBlush.copy(alpha = 0.5f), 0.045f * s, Offset(cx + side * dx * s, cy + dy * s))
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val r = radius
    val k = 0.20f * r
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticBezierTo(center.x + k, center.y - k, center.x + r, center.y)
        quadraticBezierTo(center.x + k, center.y + k, center.x, center.y + r)
        quadraticBezierTo(center.x - k, center.y + k, center.x - r, center.y)
        quadraticBezierTo(center.x - k, center.y - k, center.x, center.y - r)
        close()
    }
    drawPath(path, color)
}

// ---------------------------------------------------------------------------
//  Iconos de navegación y módulos.
// ---------------------------------------------------------------------------

/** Casita kawaii: tejado menta y puerta con carita. */
@Composable
fun KawaiiHomeIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        val roof = Path().apply {
            moveTo(cx - 0.46f * s, cy - 0.06f * s)
            lineTo(cx, cy - 0.46f * s)
            lineTo(cx + 0.46f * s, cy - 0.06f * s)
            close()
        }
        drawPath(roof, IconMint)
        drawPath(roof, IconPurpleDeep, style = Stroke(width = 0.05f * s, join = StrokeJoin.Round, cap = StrokeCap.Round))

        drawRoundRect(
            color = IconPurple,
            topLeft = Offset(cx - 0.33f * s, cy - 0.10f * s),
            size = Size(0.66f * s, 0.52f * s),
            cornerRadius = CornerRadius(0.10f * s)
        )
        drawRoundRect(
            color = IconCream,
            topLeft = Offset(cx - 0.15f * s, cy + 0.04f * s),
            size = Size(0.30f * s, 0.38f * s),
            cornerRadius = CornerRadius(0.08f * s)
        )
        kawaiiEyes(cx, cy + 0.15f * s, s, dx = 0.075f, dy = 0f, eyeR = 0.052f, pupilR = 0.032f)
        kawaiiSmile(cx, cy + 0.25f * s, s, halfW = 0.05f, depth = 0.04f, width = 0.02f)
        drawCircle(IconPurple, 0.02f * s, Offset(cx + 0.10f * s, cy + 0.31f * s))
    }
}

/** Nido: huevo menta con carita sobre un bol de ramitas morado. */
@Composable
fun KawaiiNestIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        drawOval(
            color = IconMint,
            topLeft = Offset(cx - 0.20f * s, cy - 0.46f * s),
            size = Size(0.40f * s, 0.56f * s)
        )
        kawaiiEyes(cx, cy - 0.24f * s, s, dx = 0.075f, dy = 0f, eyeR = 0.052f, pupilR = 0.032f)
        kawaiiSmile(cx, cy - 0.14f * s, s, halfW = 0.05f, depth = 0.04f, width = 0.02f)
        kawaiiBlush(cx, cy - 0.17f * s, s, dx = 0.115f, dy = 0.01f)

        val bowl = Path().apply {
            moveTo(cx - 0.47f * s, cy + 0.04f * s)
            quadraticBezierTo(cx, cy + 0.64f * s, cx + 0.47f * s, cy + 0.04f * s)
            close()
        }
        drawPath(bowl, IconPurple)
        drawLine(
            color = IconMintDeep,
            start = Offset(cx - 0.34f * s, cy + 0.18f * s),
            end = Offset(cx + 0.24f * s, cy + 0.28f * s),
            strokeWidth = 0.035f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = IconMintDeep,
            start = Offset(cx + 0.34f * s, cy + 0.16f * s),
            end = Offset(cx - 0.16f * s, cy + 0.32f * s),
            strokeWidth = 0.035f * s,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = IconPurpleDeep,
            topLeft = Offset(cx - 0.48f * s, cy - 0.01f * s),
            size = Size(0.96f * s, 0.13f * s),
            cornerRadius = CornerRadius(0.065f * s)
        )
    }
}

/** Hoja menta con venas y carita (Valle). */
@Composable
fun KawaiiLeafIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        val leaf = Path().apply {
            moveTo(cx, cy + 0.42f * s)
            cubicTo(cx - 0.50f * s, cy + 0.16f * s, cx - 0.40f * s, cy - 0.40f * s, cx, cy - 0.44f * s)
            cubicTo(cx + 0.40f * s, cy - 0.40f * s, cx + 0.50f * s, cy + 0.16f * s, cx, cy + 0.42f * s)
            close()
        }
        drawPath(leaf, IconMint)
        drawPath(leaf, IconPurpleDeep, style = Stroke(width = 0.045f * s, join = StrokeJoin.Round))
        drawLine(
            color = IconPurpleDeep,
            start = Offset(cx, cy - 0.30f * s),
            end = Offset(cx, cy + 0.30f * s),
            strokeWidth = 0.032f * s,
            cap = StrokeCap.Round
        )
        drawLine(
            color = IconPurpleDeep,
            start = Offset(cx, cy + 0.38f * s),
            end = Offset(cx, cy + 0.52f * s),
            strokeWidth = 0.05f * s,
            cap = StrokeCap.Round
        )
        kawaiiEyes(cx, cy - 0.06f * s, s, dx = 0.13f, dy = 0f, eyeR = 0.07f, pupilR = 0.042f)
        kawaiiSmile(cx, cy + 0.06f * s, s, halfW = 0.07f, depth = 0.05f, width = 0.022f)
        kawaiiBlush(cx, cy + 0.10f * s, s, dx = 0.22f, dy = 0f)
    }
}

/** Montaña morada con cima de nieve menta (Cumbres). */
@Composable
fun KawaiiMountainIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        val back = Path().apply {
            moveTo(cx - 0.46f * s, cy + 0.42f * s)
            lineTo(cx - 0.06f * s, cy - 0.30f * s)
            lineTo(cx + 0.34f * s, cy + 0.42f * s)
            close()
        }
        drawPath(back, IconPurpleSoft)

        val front = Path().apply {
            moveTo(cx - 0.30f * s, cy + 0.42f * s)
            lineTo(cx + 0.16f * s, cy - 0.44f * s)
            lineTo(cx + 0.54f * s, cy + 0.42f * s)
            close()
        }
        drawPath(front, IconPurple)

        val snow = Path().apply {
            moveTo(cx + 0.16f * s, cy - 0.44f * s)
            lineTo(cx + 0.03f * s, cy - 0.18f * s)
            quadraticBezierTo(cx + 0.16f * s, cy - 0.12f * s, cx + 0.29f * s, cy - 0.18f * s)
            close()
        }
        drawPath(snow, IconMint)

        kawaiiEyes(cx + 0.16f * s, cy + 0.10f * s, s, dx = 0.10f, dy = 0f, eyeR = 0.065f, pupilR = 0.038f)
        kawaiiSmile(cx + 0.16f * s, cy + 0.22f * s, s, halfW = 0.06f, depth = 0.045f, width = 0.022f)
        kawaiiBlush(cx + 0.16f * s, cy + 0.16f * s, s, dx = 0.17f, dy = 0.02f)
    }
}

/** Árbol de copa menta con carita (Raíces). */
@Composable
fun KawaiiTreeIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        drawRoundRect(
            color = IconPurpleDeep,
            topLeft = Offset(cx - 0.07f * s, cy + 0.02f * s),
            size = Size(0.14f * s, 0.44f * s),
            cornerRadius = CornerRadius(0.05f * s)
        )
        drawRoundRect(
            color = IconPurpleDeep,
            topLeft = Offset(cx - 0.20f * s, cy + 0.36f * s),
            size = Size(0.40f * s, 0.09f * s),
            cornerRadius = CornerRadius(0.045f * s)
        )
        drawCircle(IconMintDeep, 0.20f * s, Offset(cx - 0.26f * s, cy - 0.02f * s))
        drawCircle(IconMintDeep, 0.20f * s, Offset(cx + 0.26f * s, cy - 0.02f * s))
        drawCircle(IconMint, 0.28f * s, Offset(cx, cy - 0.20f * s))
        kawaiiEyes(cx, cy - 0.22f * s, s, dx = 0.11f, dy = 0f, eyeR = 0.07f, pupilR = 0.042f)
        kawaiiSmile(cx, cy - 0.10f * s, s, halfW = 0.065f, depth = 0.05f, width = 0.022f)
        kawaiiBlush(cx, cy - 0.16f * s, s, dx = 0.185f, dy = 0.02f)
    }
}

/** Libro abierto con la carita repartida entre las dos páginas. */
@Composable
fun KawaiiBookIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        val leftPage = Path().apply {
            moveTo(cx, cy - 0.16f * s)
            quadraticBezierTo(cx - 0.20f * s, cy - 0.30f * s, cx - 0.46f * s, cy - 0.22f * s)
            lineTo(cx - 0.46f * s, cy + 0.26f * s)
            quadraticBezierTo(cx - 0.20f * s, cy + 0.20f * s, cx, cy + 0.32f * s)
            close()
        }
        drawPath(leftPage, IconMint)
        drawPath(leftPage, IconPurpleDeep, style = Stroke(width = 0.042f * s, join = StrokeJoin.Round))

        val rightPage = Path().apply {
            moveTo(cx, cy - 0.16f * s)
            quadraticBezierTo(cx + 0.20f * s, cy - 0.30f * s, cx + 0.46f * s, cy - 0.22f * s)
            lineTo(cx + 0.46f * s, cy + 0.26f * s)
            quadraticBezierTo(cx + 0.20f * s, cy + 0.20f * s, cx, cy + 0.32f * s)
            close()
        }
        drawPath(rightPage, IconCream)
        drawPath(rightPage, IconPurpleDeep, style = Stroke(width = 0.042f * s, join = StrokeJoin.Round))

        drawLine(
            color = IconPurple,
            start = Offset(cx, cy - 0.16f * s),
            end = Offset(cx, cy + 0.32f * s),
            strokeWidth = 0.05f * s,
            cap = StrokeCap.Round
        )
        kawaiiEyes(cx, cy - 0.02f * s, s, dx = 0.20f, dy = 0f, eyeR = 0.075f, pupilR = 0.045f)
        kawaiiSmile(cx, cy + 0.09f * s, s, halfW = 0.10f, depth = 0.06f, width = 0.024f)
        kawaiiBlush(cx, cy + 0.07f * s, s, dx = 0.32f, dy = 0.02f)
    }
}

/** Pieza de puzle morada con pestañas y destello menta. */
@Composable
fun KawaiiPuzzleIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        drawCircle(IconPurple, 0.16f * s, Offset(cx - 0.06f * s, cy - 0.20f * s))
        drawCircle(IconPurple, 0.16f * s, Offset(cx + 0.26f * s, cy + 0.14f * s))
        drawRoundRect(
            color = IconPurple,
            topLeft = Offset(cx - 0.36f * s, cy - 0.16f * s),
            size = Size(0.60f * s, 0.60f * s),
            cornerRadius = CornerRadius(0.13f * s)
        )
        kawaiiEyes(cx - 0.06f * s, cy + 0.08f * s, s, dx = 0.105f, dy = 0f, eyeR = 0.07f, pupilR = 0.042f)
        kawaiiSmile(cx - 0.06f * s, cy + 0.20f * s, s, halfW = 0.065f, depth = 0.05f, width = 0.022f)
        kawaiiBlush(cx - 0.06f * s, cy + 0.14f * s, s, dx = 0.175f, dy = 0.02f)
        drawSparkle(Offset(cx + 0.38f * s, cy - 0.34f * s), 0.11f * s, IconMint)
    }
}

// ---------------------------------------------------------------------------
//  Iconos de los minijuegos del Nido.
// ---------------------------------------------------------------------------

/** Semilla con brote: minijuego "Semillas Bailarinas". */
@Composable
fun KawaiiSeedIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        drawOval(
            color = IconPurpleDeep,
            topLeft = Offset(cx - 0.40f * s, cy + 0.30f * s),
            size = Size(0.80f * s, 0.22f * s)
        )
        drawLine(
            color = IconPurpleDeep,
            start = Offset(cx, cy - 0.02f * s),
            end = Offset(cx, cy - 0.36f * s),
            strokeWidth = 0.045f * s,
            cap = StrokeCap.Round
        )
        drawOval(
            color = IconMint,
            topLeft = Offset(cx - 0.22f * s, cy - 0.42f * s),
            size = Size(0.19f * s, 0.13f * s)
        )
        drawOval(
            color = IconMint,
            topLeft = Offset(cx + 0.03f * s, cy - 0.50f * s),
            size = Size(0.19f * s, 0.13f * s)
        )
        drawOval(
            color = IconMintDeep,
            topLeft = Offset(cx - 0.19f * s, cy - 0.06f * s),
            size = Size(0.38f * s, 0.44f * s)
        )
        drawOval(
            color = IconPurpleDeep,
            topLeft = Offset(cx - 0.19f * s, cy - 0.06f * s),
            size = Size(0.38f * s, 0.44f * s),
            style = Stroke(width = 0.035f * s)
        )
        kawaiiEyes(cx, cy + 0.10f * s, s, dx = 0.085f, dy = 0f, eyeR = 0.058f, pupilR = 0.035f)
        kawaiiSmile(cx, cy + 0.20f * s, s, halfW = 0.05f, depth = 0.04f, width = 0.02f)
        kawaiiBlush(cx, cy + 0.16f * s, s, dx = 0.135f, dy = 0.02f)
    }
}

/** Chispa luminosa: minijuego "Escondite de Luciérnagas". */
@Composable
fun KawaiiSparkIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        drawSparkle(Offset(cx, cy), 0.48f * s, IconMintDeep)
        drawSparkle(Offset(cx, cy), 0.34f * s, IconMint)
        drawCircle(IconPurple, 0.20f * s, Offset(cx, cy))
        listOf(-1f, 1f).forEach { side ->
            drawCircle(Color.White, 0.05f * s, Offset(cx + side * 0.075f * s, cy - 0.02f * s))
            drawCircle(IconFace, 0.028f * s, Offset(cx + side * 0.075f * s, cy - 0.02f * s))
        }
        kawaiiSmile(cx, cy + 0.06f * s, s, halfW = 0.06f, depth = 0.045f, width = 0.02f)
        drawSparkle(Offset(cx - 0.36f * s, cy - 0.34f * s), 0.10f * s, IconPurpleSoft)
        drawSparkle(Offset(cx + 0.36f * s, cy + 0.34f * s), 0.08f * s, IconPurpleSoft)
    }
}

/** Nota musical con carita: minijuego "Coro del Bosque". */
@Composable
fun KawaiiNoteIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        // Nota pequeña menta al fondo.
        drawLine(
            color = IconMintDeep,
            start = Offset(cx + 0.40f * s, cy + 0.18f * s),
            end = Offset(cx + 0.40f * s, cy - 0.30f * s),
            strokeWidth = 0.05f * s,
            cap = StrokeCap.Round
        )
        drawOval(
            color = IconMintDeep,
            topLeft = Offset(cx + 0.18f * s, cy + 0.10f * s),
            size = Size(0.26f * s, 0.20f * s)
        )

        // Nota grande morada.
        drawLine(
            color = IconPurple,
            start = Offset(cx - 0.02f * s, cy + 0.20f * s),
            end = Offset(cx - 0.02f * s, cy - 0.42f * s),
            strokeWidth = 0.065f * s,
            cap = StrokeCap.Round
        )
        val flag = Path().apply {
            moveTo(cx - 0.02f * s, cy - 0.42f * s)
            quadraticBezierTo(cx + 0.28f * s, cy - 0.36f * s, cx + 0.24f * s, cy - 0.14f * s)
            quadraticBezierTo(cx + 0.18f * s, cy - 0.30f * s, cx - 0.02f * s, cy - 0.26f * s)
            close()
        }
        drawPath(flag, IconPurple)
        drawOval(
            color = IconPurple,
            topLeft = Offset(cx - 0.32f * s, cy + 0.10f * s),
            size = Size(0.38f * s, 0.30f * s)
        )
        kawaiiEyes(cx - 0.13f * s, cy + 0.22f * s, s, dx = 0.075f, dy = 0f, eyeR = 0.05f, pupilR = 0.03f)
        kawaiiSmile(cx - 0.13f * s, cy + 0.31f * s, s, halfW = 0.045f, depth = 0.035f, width = 0.018f)
    }
}

/** Ráfaga de viento con nubecita: minijuego "Respira con Zazili". */
@Composable
fun KawaiiBreezeIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        val windPaths = listOf(
            -0.38f to 0.20f,
            -0.20f to 0.30f,
            -0.02f to 0.24f
        )
        windPaths.forEach { (wy, len) ->
            val path = Path().apply {
                moveTo(cx - 0.44f * s, cy + wy * s)
                cubicTo(
                    cx - 0.12f * s, cy + (wy - 0.07f) * s,
                    cx + 0.06f * s, cy + (wy + 0.07f) * s,
                    cx + len * s, cy + wy * s
                )
            }
            drawPath(path, IconPurple, style = Stroke(width = 0.055f * s, cap = StrokeCap.Round))
        }
        drawCircle(IconMintDeep, 0.16f * s, Offset(cx - 0.22f * s, cy + 0.18f * s))
        drawCircle(IconMint, 0.20f * s, Offset(cx - 0.02f * s, cy + 0.12f * s))
        drawCircle(IconMintDeep, 0.14f * s, Offset(cx + 0.16f * s, cy + 0.20f * s))
        drawRoundRect(
            color = IconMint,
            topLeft = Offset(cx - 0.26f * s, cy + 0.22f * s),
            size = Size(0.48f * s, 0.18f * s),
            cornerRadius = CornerRadius(0.09f * s)
        )
        kawaiiEyes(cx - 0.04f * s, cy + 0.16f * s, s, dx = 0.08f, dy = 0f, eyeR = 0.05f, pupilR = 0.03f)
        kawaiiSmile(cx - 0.04f * s, cy + 0.25f * s, s, halfW = 0.045f, depth = 0.035f, width = 0.018f)
    }
}

// ---------------------------------------------------------------------------
//  Iconos de la pantalla de Ajustes.
// ---------------------------------------------------------------------------

private fun DrawScope.drawKawaiiSpeaker(cx: Float, cy: Float, s: Float) {
    drawRoundRect(
        color = IconPurple,
        topLeft = Offset(cx - 0.40f * s, cy - 0.13f * s),
        size = Size(0.26f * s, 0.26f * s),
        cornerRadius = CornerRadius(0.06f * s)
    )
    val cone = Path().apply {
        moveTo(cx - 0.14f * s, cy - 0.13f * s)
        lineTo(cx + 0.08f * s, cy - 0.34f * s)
        lineTo(cx + 0.08f * s, cy + 0.34f * s)
        lineTo(cx - 0.14f * s, cy + 0.13f * s)
        close()
    }
    drawPath(cone, IconPurple)
    kawaiiEyes(cx - 0.27f * s, cy - 0.02f * s, s, dx = 0.055f, dy = 0f, eyeR = 0.038f, pupilR = 0.022f)
    kawaiiSmile(cx - 0.27f * s, cy + 0.05f * s, s, halfW = 0.035f, depth = 0.025f, width = 0.015f)
}

private fun DrawScope.drawVolumeWaves(cx: Float, cy: Float, s: Float, count: Int) {
    val radii = listOf(0.20f, 0.28f, 0.36f)
    repeat(count.coerceIn(0, radii.size)) { i ->
        val r = radii[i]
        drawArc(
            color = IconMintDeep,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(cx + 0.02f * s - r * s, cy - r * s),
            size = Size(2f * r * s, 2f * r * s),
            style = Stroke(width = 0.05f * s, cap = StrokeCap.Round)
        )
    }
}

/** Altavoz kawaii con ondas: volumen alto. */
@Composable
fun KawaiiVolumeUpIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        drawKawaiiSpeaker(cx - 0.06f * s, cy, s)
        drawVolumeWaves(cx - 0.06f * s, cy, s, count = 3)
    }
}

/** Altavoz kawaii con una onda: volumen bajo. */
@Composable
fun KawaiiVolumeDownIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        drawKawaiiSpeaker(cx - 0.06f * s, cy, s)
        drawVolumeWaves(cx - 0.06f * s, cy, s, count = 1)
    }
}

/** Engranaje kawaii con dientes redondeados y carita. */
@Composable
fun KawaiiGearIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        repeat(6) { i ->
            rotate(degrees = i * 60f, pivot = Offset(cx, cy)) {
                drawRoundRect(
                    color = IconPurple,
                    topLeft = Offset(cx - 0.075f * s, cy - 0.48f * s),
                    size = Size(0.15f * s, 0.22f * s),
                    cornerRadius = CornerRadius(0.055f * s)
                )
            }
        }
        drawCircle(IconPurple, 0.34f * s, Offset(cx, cy))
        drawCircle(IconMint, 0.24f * s, Offset(cx, cy))
        kawaiiEyes(cx, cy - 0.05f * s, s, dx = 0.095f, dy = 0f, eyeR = 0.06f, pupilR = 0.036f)
        kawaiiSmile(cx, cy + 0.07f * s, s, halfW = 0.055f, depth = 0.045f, width = 0.02f)
    }
}

/** Sol menta con rayos y carita (tema claro). */
@Composable
fun KawaiiSunIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        repeat(8) { i ->
            rotate(degrees = i * 45f, pivot = Offset(cx, cy)) {
                drawLine(
                    color = IconMintDeep,
                    start = Offset(cx, cy - 0.46f * s),
                    end = Offset(cx, cy - 0.32f * s),
                    strokeWidth = 0.075f * s,
                    cap = StrokeCap.Round
                )
            }
        }
        drawCircle(IconMint, 0.27f * s, Offset(cx, cy))
        drawCircle(IconMintDeep, 0.27f * s, Offset(cx, cy), style = Stroke(width = 0.03f * s))
        kawaiiEyes(cx, cy - 0.03f * s, s, dx = 0.09f, dy = 0f, eyeR = 0.055f, pupilR = 0.034f)
        kawaiiSmile(cx, cy + 0.07f * s, s, halfW = 0.05f, depth = 0.04f, width = 0.02f)
    }
}

/** Luna lavanda con carita y estrellita (tema oscuro). */
@Composable
fun KawaiiMoonIcon(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    KawaiiIconCanvas(modifier, size) { s, cx, cy ->
        val outer = Rect(cx - 0.36f * s, cy - 0.36f * s, cx + 0.36f * s, cy + 0.36f * s)
        val inner = Rect(cx - 0.16f * s, cy - 0.30f * s, cx + 0.44f * s, cy + 0.30f * s)
        val moon = Path().apply {
            arcTo(outer, 54f, 252f, true)
            arcTo(inner, 76f, 208f, false)
            close()
        }
        drawPath(moon, IconPurpleSoft)
        kawaiiEyes(cx - 0.22f * s, cy - 0.04f * s, s, dx = 0.065f, dy = 0f, eyeR = 0.042f, pupilR = 0.026f)
        kawaiiSmile(cx - 0.22f * s, cy + 0.05f * s, s, halfW = 0.04f, depth = 0.03f, width = 0.016f)
        drawSparkle(Offset(cx + 0.32f * s, cy - 0.28f * s), 0.09f * s, IconMintDeep)
    }
}
