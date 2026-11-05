package com.example.zazili.presentation.logros

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.logros.Achievement
import com.example.zazili.core.logros.AchievementEngine
import com.example.zazili.core.logros.CatalogoLogros
import com.example.zazili.core.logros.ObservadorLogros
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/** Fondos pastel rotativos para las tarjetas ganadas. */
private val FONDOS_LOGRO = listOf(
    LavandaClara,
    MintGreen,
    AzulCielo,
    RosaSuave,
    AmarilloSuave,
    LavandaMedia.copy(alpha = 0.55f)
)

/**
 * Galería completa de los 25 logros.
 *
 * Observa `AchievementEngine.estado` con `collectAsStateWithLifecycle`, así que
 * si el niño gana un logro mientras navega, la tarjeta se enciende al instante
 * y [ObservadorLogros] lanza el confeti encima de la galería.
 *
 * Ganados: color pastel, borde dorado brillante, sombra cálida y palomita.
 * Bloqueados: gris lavanda, candado y descripción recortada con una pista.
 */
@Composable
fun AchievementsScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val motor = remember(contexto) { AchievementEngine.obtener(contexto) }
    val estado by motor.estado.collectAsStateWithLifecycle()
    val zen = collectZenMode()

    val logros = remember(estado.desbloqueados) {
        CatalogoLogros.todos
            .map { it.copy(isUnlocked = it.id in estado.desbloqueados) }
            .sortedByDescending { it.isUnlocked }
    }
    val total = CatalogoLogros.todos.size
    val ganados = logros.count { it.isUnlocked }
    val fondo = if (zen) {
        zenMutedBackground(isSystemInDarkTheme())
    } else {
        MaterialTheme.colorScheme.background
    }

    BackHandler(onBack = onSalir)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            BarraLogros(onSalir = onSalir)

            TarjetaProgreso(
                ganados = ganados,
                total = total,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 10.dp,
                    bottom = 28.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(logros, key = { _, logro -> logro.id }) { indice, logro ->
                    TarjetaLogro(
                        logro = logro,
                        colorFondo = FONDOS_LOGRO[indice % FONDOS_LOGRO.size]
                    )
                }
            }
        }

        ObservadorLogros()
    }
}

// ---------------------------------------------------------------------------
//  BARRA SUPERIOR
// ---------------------------------------------------------------------------

@Composable
private fun BarraLogros(onSalir: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        ZaziButton(
            text = "‹",
            onClick = onSalir,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Volver al tablero" },
            containerColor = ZaziliWhite,
            contentColor = UniversoProfundo,
            cornerRadius = 8.dp,
            baseHeight = 4.dp,
            fontSize = 26.sp
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoTrofeoPixel(size = 20.dp, color = AmarilloOscuro)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "MIS LOGROS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Tu colección de trofeos",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AmarilloSuave,
            border = androidx.compose.foundation.BorderStroke(2.dp, AmarilloVibrante)
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                IconoTrofeoPixel(size = 26.dp, color = AmarilloOscuro)
            }
        }
    }
}

@Composable
private fun TarjetaProgreso(
    ganados: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val avance = if (total == 0) 0f else ganados.toFloat() / total.toFloat()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LavandaClara,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AmarilloSuave),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏆", fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TU COLECCIÓN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp,
                        color = MagicalPurple
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$ganados de $total",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { avance.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = AmarilloVibrante,
                    trackColor = ZaziliWhite.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  TARJETA DE LOGRO
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaLogro(
    logro: Achievement,
    colorFondo: Color
) {
    val desbloqueado = logro.isUnlocked
    val forma = RoundedCornerShape(20.dp)

    val borde = if (desbloqueado) {
        Modifier
            .shadow(
                elevation = 10.dp,
                shape = forma,
                clip = false,
                ambientColor = AmarilloVibrante,
                spotColor = AmarilloVibrante
            )
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(
                    listOf(AmarilloVibrante, ZaziliWhite, AmarilloVibrante)
                ),
                shape = forma
            )
    } else {
        Modifier.border(
            width = 2.dp,
            color = LavandaMedia.copy(alpha = 0.4f),
            shape = forma
        )
    }

    val descripcion = if (desbloqueado) {
        logro.description
    } else {
        logro.description.take(38).trimEnd().ifEmpty { "Un secreto por descubrir" } + "…"
    }

    Surface(
        shape = forma,
        color = if (desbloqueado) colorFondo else SoftCream.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
            .then(borde)
            .clickable(
                role = Role.Button,
                onClickLabel = if (desbloqueado) "Logro ganado" else "Logro bloqueado"
            ) { }
            .semantics {
                contentDescription = if (desbloqueado) {
                    "Logro ganado: ${logro.title}. ${logro.description} Recompensa: ${logro.rewardZazoles} Zazoles."
                } else {
                    "Logro bloqueado: ${logro.title}. Sigue jugando para descubrirlo."
                }
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (desbloqueado) {
                                ZaziliWhite.copy(alpha = 0.55f)
                            } else {
                                LavandaMedia.copy(alpha = 0.16f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emojiDeLogro(logro.id),
                        fontSize = 22.sp,
                        modifier = Modifier.then(
                            if (desbloqueado) Modifier else Modifier.graphicsLayerAlpha(0.32f)
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                if (desbloqueado) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(VerdeDuo),
                        contentAlignment = Alignment.Center
                    ) {
                        KawaiiCheckIcon(size = 18.dp, color = ZaziliWhite)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(LavandaMedia.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        KawaiiLockIcon(size = 16.dp, color = UniversoProfundo.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = logro.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = if (desbloqueado) UniversoProfundo else UniversoProfundo.copy(alpha = 0.45f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (desbloqueado) {
                    UniversoProfundo.copy(alpha = 0.72f)
                } else {
                    UniversoProfundo.copy(alpha = 0.3f)
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!desbloqueado) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sigue jugando para descubrirlo",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = LavandaMedia,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                KawaiiCoinIcon(size = 16.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "+${logro.rewardZazoles}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (desbloqueado) {
                        AmarilloOscuro
                    } else {
                        UniversoProfundo.copy(alpha = 0.28f)
                    },
                    maxLines = 1
                )
                if (logro.rewardItemId != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "🎁",
                        fontSize = 13.sp,
                        modifier = Modifier.then(
                            if (desbloqueado) Modifier else Modifier.graphicsLayerAlpha(0.3f)
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (desbloqueado) "¡GANADO!" else "BLOQUEADO",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = if (desbloqueado) VerdeDuo else UniversoProfundo.copy(alpha = 0.32f),
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
        }
    }
}

/** Atenúa un emoji sin tocar el resto de la tarjeta. */
private fun Modifier.graphicsLayerAlpha(alfa: Float): Modifier =
    this.then(
        Modifier.graphicsLayer {
            alpha = alfa
        }
    )

/** Emoji temático por logro: ayuda a reconocer la categoría de un vistazo. */
private fun emojiDeLogro(id: String): String = when {
    id.startsWith("fiebre") || id.startsWith("racha") || id.startsWith("protector") -> "🔥"
    id.contains("zazol") || id.contains("ahorr") || id.contains("tiburon") ||
        id.contains("gasto") -> "🪙"
    id.contains("fashion") || id.contains("vestidor") || id.contains("estilo") ||
        id.contains("kuromi") -> "👗"
    id.contains("mano") -> "🤗"
    id.contains("chef") || id.contains("alimento") -> "🌮"
    id.contains("bug") -> "🪲"
    id.contains("algoritmo") -> "🧠"
    id.contains("ciber") -> "🛡️"
    id.contains("decorador") -> "📲"
    id.contains("dj") -> "🎵"
    id.contains("galactico") -> "🌌"
    id.contains("curioso") || id.contains("mundo") -> "🌍"
    id.contains("primeros") -> "🎮"
    id.contains("tarea") || id.contains("aprendiz") || id.contains("maraton") -> "📚"
    else -> "🏆"
}

// ---------------------------------------------------------------------------
//  ÍCONO DE TROFEO (TAMBIÉN LO USA EL TABLERO)
// ---------------------------------------------------------------------------

/**
 * Trofeo kawaii dibujado en Canvas: copa, asas redondas, pie y base.
 * Sirve como ícono de UI (vector permitido) en la barra del tablero y en la
 * cabecera de la galería.
 */
@Composable
fun IconoTrofeoPixel(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val trazo = Stroke(width = s * 0.07f)

        drawArc(
            color = color,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(s * 0.04f, s * 0.14f),
            size = Size(s * 0.34f, s * 0.36f),
            style = trazo
        )
        drawArc(
            color = color,
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(s * 0.62f, s * 0.14f),
            size = Size(s * 0.34f, s * 0.36f),
            style = trazo
        )

        val copa = Path().apply {
            moveTo(s * 0.24f, s * 0.13f)
            lineTo(s * 0.76f, s * 0.13f)
            lineTo(s * 0.70f, s * 0.50f)
            cubicTo(s * 0.66f, s * 0.64f, s * 0.34f, s * 0.64f, s * 0.30f, s * 0.50f)
            close()
        }
        drawPath(copa, color)

        drawRoundRect(
            color = ZaziliWhite.copy(alpha = 0.35f),
            topLeft = Offset(s * 0.32f, s * 0.19f),
            size = Size(s * 0.09f, s * 0.24f),
            cornerRadius = CornerRadius(s * 0.045f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.45f, s * 0.62f),
            size = Size(s * 0.10f, s * 0.15f),
            cornerRadius = CornerRadius(s * 0.03f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.30f, s * 0.77f),
            size = Size(s * 0.40f, s * 0.10f),
            cornerRadius = CornerRadius(s * 0.05f)
        )
    }
}
