package com.example.zazili.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenMutedSurface
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.audio.TemaMusical
import com.example.zazili.core.logros.AccionesLogro
import com.example.zazili.core.logros.AchievementDispatcher
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.NightSurface
import com.example.zazili.ui.theme.NightSurfaceHigh
import com.example.zazili.ui.theme.NightTextPrimary
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.ZaziliPrimaryText
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Selector de música de fondo: un botón 3D flotante con nota musical que abre
 * una hoja inferior limpia con los cinco estilos. La elección se guarda sola y
 * la pista cambia al instante (con desvanecido dentro de [GlobalAudioManager]).
 *
 * Colócalo donde quieras: no necesita parámetros de estado ni callbacks.
 */
@Composable
fun MusicThemeButton(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    size: Dp = 52.dp
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val zen = collectZenMode()

    val cara = when {
        zen -> zenMutedSurface(darkTheme)
        darkTheme -> NightSurfaceHigh
        else -> MagicalPurple
    }
    val icono = if (zen || darkTheme) NightTextPrimary else ZaziliWhite

    // Vaivén tierno e infinito: la nota respira mientras nadie la toca.
    val transicion = rememberInfiniteTransition(label = "notaFlotante")
    val flotar by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "notaFlotanteValor"
    )

    ZaziButton(
        text = "",
        onClick = {
            GlobalAudioManager.playPop()
            abierto = true
        },
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Elegir la música del bosque" },
        containerColor = cara,
        contentColor = icono,
        cornerRadius = size / 2,
        baseHeight = 5.dp
    ) {
        Box(
            modifier = Modifier.graphicsLayer { translationY = -2.dp.toPx() * flotar },
            contentAlignment = Alignment.Center
        ) {
            KawaiiNoteIcon(size = size * 0.58f)
        }
    }

    if (abierto) {
        MusicSelectorDialog(
            onDismiss = { abierto = false },
            darkTheme = darkTheme
        )
    }
}

/**
 * Hoja inferior con los cinco estilos de música. Cada opción reproduce su
 * pista al tocarla, así el niño escucha el cambio antes de decidir; la hoja
 * sigue abierta hasta que pulse "Listo".
 */
@Composable
fun MusicSelectorDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false
) {
    val context = LocalContext.current
    val tema by GlobalAudioManager.temaBgm.collectAsStateWithLifecycle()
    val zen = collectZenMode()

    val fondoHoja = when {
        zen -> zenMutedBackground(darkTheme)
        darkTheme -> NightSurface
        else -> SoftCream
    }
    val titulo = if (darkTheme) NightTextPrimary else ZaziliPrimaryText

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                color = fondoHoja,
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(52.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(titulo.copy(alpha = 0.18f))
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Música del bosque",
                        style = MaterialTheme.typography.headlineSmall,
                        color = titulo
                    )
                    Text(
                        text = "Toca un estilo y suena al instante. Tu elección se guarda solita.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = titulo.copy(alpha = 0.72f)
                    )
                    Spacer(Modifier.height(16.dp))

                    TemaMusical.todos.forEach { candidato ->
                        val elegido = candidato == tema
                        OpcionMusica(
                            tema = candidato,
                            elegido = elegido,
                            darkTheme = darkTheme,
                            zen = zen,
                            onClick = {
                                if (!elegido) {
                                    GlobalAudioManager.playPop()
                                    GlobalAudioManager.seleccionarTema(context, candidato)
                                    AchievementDispatcher.notifyAction(
                                        context,
                                        AccionesLogro.CAMBIAR_MUSICA
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(6.dp))
                    ZaziButton(
                        text = "Listo",
                        onClick = {
                            GlobalAudioManager.playSuccess()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        containerColor = if (zen) zenMutedSurface(darkTheme) else MintGreen,
                        contentColor = if (zen) NightTextPrimary else ZaziliPrimaryText
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** Fila 3D de un estilo: icono amable, etiqueta en español y palomita activa. */
@Composable
private fun OpcionMusica(
    tema: TemaMusical,
    elegido: Boolean,
    darkTheme: Boolean,
    zen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cara = when {
        zen -> zenMutedSurface(darkTheme)
        elegido -> if (darkTheme) NightSurfaceHigh else MagicalPurple
        darkTheme -> NightSurfaceHigh
        else -> ZaziliWhite
    }
    val texto = when {
        zen -> NightTextPrimary
        elegido -> if (darkTheme) NightTextPrimary else ZaziliWhite
        darkTheme -> NightTextPrimary
        else -> ZaziliPrimaryText
    }
    val burbuja = when {
        elegido -> if (darkTheme) NightSurface else MintGreen
        darkTheme -> NightSurface
        else -> MintGreen.copy(alpha = 0.45f)
    }

    ZaziButton(
        text = "",
        onClick = onClick,
        modifier = modifier
            .height(78.dp)
            .semantics {
                contentDescription = if (elegido) {
                    "${tema.etiqueta}. Es la música que suena ahora."
                } else {
                    "Escuchar ${tema.etiqueta}"
                }
            },
        containerColor = cara,
        contentColor = texto,
        cornerRadius = 24.dp,
        baseHeight = 5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(burbuja),
                contentAlignment = Alignment.Center
            ) {
                IconoTemaMusical(tema = tema, size = 32.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tema.etiqueta,
                    style = MaterialTheme.typography.titleMedium,
                    color = texto,
                    maxLines = 1
                )
                Text(
                    text = tema.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = texto.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
            if (elegido) {
                PalomitaKawaii(
                    color = if (darkTheme) MintGreen else MagicalPurple,
                    size = 28.dp
                )
            }
        }
    }
}

/** Icono amable y distinto para cada estilo musical (todo dibujado a mano). */
@Composable
private fun IconoTemaMusical(tema: TemaMusical, size: Dp = 32.dp) {
    when (tema) {
        TemaMusical.ORIGINAL -> KawaiiNoteIcon(size = size)
        TemaMusical.OCHO_BITS -> IconoPixelRetro(size = size)
        TemaMusical.ANIMADA -> IconoRayo(size = size)
        TemaMusical.NINOS -> IconoCorazon(size = size)
        TemaMusical.ALTERNATIVA -> IconoGuitarra(size = size)
    }
}

/** Invadercito de píxeles: el estilo 8-bits. */
@Composable
private fun IconoPixelRetro(size: Dp = 32.dp) {
    val pixel = MagicalPurple
    val brillo = MintGreen
    Canvas(modifier = Modifier.size(size)) {
        val celda = this.size.width / 7f
        val mapa = listOf(
            "..#.#..",
            "..###..",
            ".#####.",
            "##.#.##",
            "#######",
            "#.###.#",
            "#.#.#.#"
        )
        mapa.forEachIndexed { fila, texto ->
            texto.forEachIndexed { columna, c ->
                if (c != '#') return@forEachIndexed
                val color = if (fila == 3 && (columna == 2 || columna == 4)) brillo else pixel
                drawRect(
                    color = color,
                    topLeft = Offset(columna * celda, fila * celda),
                    size = Size(celda * 0.92f, celda * 0.92f)
                )
            }
        }
    }
}

/** Rayito con líneas de velocidad: el ritmo animado. */
@Composable
private fun IconoRayo(size: Dp = 32.dp) {
    val rayo = Color(0xFFF2B84B)
    val linea = MagicalPurple
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cuerpo = Path().apply {
            moveTo(w * 0.58f, h * 0.06f)
            lineTo(w * 0.24f, h * 0.54f)
            lineTo(w * 0.46f, h * 0.54f)
            lineTo(w * 0.34f, h * 0.94f)
            lineTo(w * 0.76f, h * 0.42f)
            lineTo(w * 0.52f, h * 0.42f)
            close()
        }
        drawPath(cuerpo, rayo)
        repeat(2) { i ->
            val y = h * (0.30f + i * 0.24f)
            drawLine(
                color = linea,
                start = Offset(w * 0.02f, y),
                end = Offset(w * 0.16f, y),
                strokeWidth = h * 0.07f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Corazoncito con chispas: el modo infantil tierno. */
@Composable
private fun IconoCorazon(size: Dp = 32.dp) {
    val corazon = Color(0xFFF28FA5)
    val chispa = MagicalPurple
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cuerpo = Path().apply {
            moveTo(w * 0.5f, h * 0.88f)
            cubicTo(w * 0.08f, h * 0.60f, w * 0.14f, h * 0.18f, w * 0.40f, h * 0.20f)
            cubicTo(w * 0.47f, h * 0.21f, w * 0.50f, h * 0.28f, w * 0.50f, h * 0.32f)
            cubicTo(w * 0.50f, h * 0.28f, w * 0.53f, h * 0.21f, w * 0.60f, h * 0.20f)
            cubicTo(w * 0.86f, h * 0.18f, w * 0.92f, h * 0.60f, w * 0.5f, h * 0.88f)
            close()
        }
        drawPath(cuerpo, corazon)
        drawCircle(color = chispa, radius = w * 0.06f, center = Offset(w * 0.16f, h * 0.16f))
        drawCircle(color = chispa, radius = w * 0.045f, center = Offset(w * 0.88f, h * 0.12f))
    }
}

/** Guitarrita acústica: la versión alternativa relajada. */
@Composable
private fun IconoGuitarra(size: Dp = 32.dp) {
    val madera = Color(0xFFD9A46B)
    val oscuro = Color(0xFF8A5F37)
    val cuerda = MagicalPurple
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(
            color = oscuro,
            start = Offset(w * 0.52f, h * 0.42f),
            end = Offset(w * 0.86f, h * 0.10f),
            strokeWidth = w * 0.13f,
            cap = StrokeCap.Round
        )
        drawCircle(color = madera, radius = w * 0.30f, center = Offset(w * 0.42f, h * 0.60f))
        drawCircle(color = madera, radius = w * 0.22f, center = Offset(w * 0.34f, h * 0.46f))
        drawCircle(color = oscuro, radius = w * 0.10f, center = Offset(w * 0.40f, h * 0.60f))
        drawLine(
            color = cuerda,
            start = Offset(w * 0.42f, h * 0.62f),
            end = Offset(w * 0.78f, h * 0.20f),
            strokeWidth = w * 0.035f,
            cap = StrokeCap.Round
        )
    }
}

/** Palomita kawaii que marca el estilo activo. */
@Composable
private fun PalomitaKawaii(color: Color, size: Dp = 28.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(color = color.copy(alpha = 0.18f), radius = w * 0.48f)
        val palomita = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.34f)
        }
        drawPath(
            path = palomita,
            color = color,
            style = Stroke(width = w * 0.13f, cap = StrokeCap.Round)
        )
    }
}
