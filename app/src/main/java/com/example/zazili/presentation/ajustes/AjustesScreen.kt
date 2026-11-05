package com.example.zazili.presentation.ajustes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.ZenModeToggle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.ajustes.AjustesManager
import com.example.zazili.core.ajustes.ModoTema
import com.example.zazili.core.audio.BibliotecaAudio
import com.example.zazili.core.audio.EfectoZazili
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.audio.PistaZazili
import com.example.zazili.core.logros.AccionesLogro
import com.example.zazili.core.logros.AchievementDispatcher
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiGearIcon
import com.example.zazili.core.ui.KawaiiHeartIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.KawaiiNoteIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiSunIcon
import com.example.zazili.core.ui.KawaiiVolumeDownIcon
import com.example.zazili.core.ui.KawaiiVolumeUpIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.NightBackground
import com.example.zazili.ui.theme.NightSurface
import com.example.zazili.ui.theme.NightTextPrimary
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Ajustes básicos de Zazili.
 *
 * Todo lo que un niño (o su adulto) necesita tocar sin perderse: apariencia,
 * música de fondo, efectos, Modo Zen y la promesa de privacidad. La música se
 * elige de la biblioteca viva de `res/raw` (`bgm_*`) y los efectos extra de
 * `sfx_*`, así que los archivos nuevos aparecen aquí sin tocar código.
 *
 * La pantalla respeta el Modo Zen: fondos planos y sin animaciones de latido.
 */
@Composable
fun AjustesScreen(
    onVolver: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val zen = collectZenMode()
    val ajustes = remember(contexto) { AjustesManager.obtener(contexto) }
    val modoTema by ajustes.modoTema.collectAsStateWithLifecycle()
    val pistaActual by GlobalAudioManager.pistaActualRes.collectAsStateWithLifecycle()
    val volumenMusica by GlobalAudioManager.bgmVolume.collectAsStateWithLifecycle()
    val volumenEfectos by GlobalAudioManager.sfxVolume.collectAsStateWithLifecycle()

    val pistas = remember { BibliotecaAudio.pistas() }
    val efectos = remember { BibliotecaAudio.efectos() }

    val fondo = when {
        zen -> Brush.verticalGradient(
            listOf(zenMutedBackground(darkTheme), zenMutedBackground(darkTheme))
        )

        darkTheme -> Brush.verticalGradient(listOf(NightBackground, NightSurface))
        else -> Brush.verticalGradient(listOf(SoftCream, LavandaClara.copy(alpha = 0.85f)))
    }

    BackHandler(onBack = onVolver)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp)
    ) {
        BarraAjustes(onVolver = onVolver)

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            TarjetaAjuste {
                Encabezado(
                    icono = { KawaiiSunIcon(size = 20.dp) },
                    titulo = "APARIENCIA",
                    subtitulo = "¿Cómo quieres ver tu casita?"
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModoTema.entries.forEach { candidato ->
                        OpcionTema(
                            modo = candidato,
                            elegido = candidato == modoTema,
                            darkTheme = darkTheme,
                            onClick = {
                                if (candidato != modoTema) {
                                    GlobalAudioManager.playPop()
                                    ajustes.establecerModo(candidato)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Automático sigue al teléfono. También puedes cambiarlo " +
                        "con el sol o la luna del tablero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 11.sp
                )
            }

            TarjetaAjuste {
                Encabezado(
                    icono = { KawaiiNoteIcon(size = 20.dp) },
                    titulo = "MÚSICA DEL BOSQUE",
                    subtitulo = "Toca una pista y suena al instante"
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp)
                ) {
                    items(pistas, key = { it.id }) { pista ->
                        FichaPista(
                            pista = pista,
                            sonando = pista.resId == pistaActual,
                            darkTheme = darkTheme,
                            onClick = {
                                GlobalAudioManager.seleccionarPista(contexto, pista.resId)
                                AchievementDispatcher.notifyAction(
                                    contexto,
                                    AccionesLogro.CAMBIAR_MUSICA
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                DeslizadorVolumen(
                    titulo = "Volumen de la música",
                    valor = volumenMusica,
                    onCambiar = GlobalAudioManager::setBgmVolume,
                    descripcion = "Volumen de la música de fondo"
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "¿Tienes más música? Los archivos que empiecen con bgm_ en " +
                        "res/raw aparecen aquí solitos al compilar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }

            TarjetaAjuste {
                Encabezado(
                    icono = { KawaiiVolumeUpIcon(size = 20.dp) },
                    titulo = "EFECTOS",
                    subtitulo = "Los soniditos de tus logros y toques"
                )
                Spacer(Modifier.height(14.dp))
                DeslizadorVolumen(
                    titulo = "Volumen de los efectos",
                    valor = volumenEfectos,
                    onCambiar = GlobalAudioManager::setSfxVolume,
                    descripcion = "Volumen de los efectos de sonido"
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ZaziButton(
                        text = "Pop",
                        onClick = { GlobalAudioManager.playPop() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .semantics { contentDescription = "Probar el efecto Pop" },
                        containerColor = MintGreen,
                        contentColor = UniversoProfundo,
                        cornerRadius = 16.dp,
                        baseHeight = 4.dp,
                        fontSize = 13.sp
                    )
                    ZaziButton(
                        text = "¡Sí!",
                        onClick = { GlobalAudioManager.playSuccess() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .semantics { contentDescription = "Probar el efecto de acierto" },
                        containerColor = VerdeDuo,
                        contentColor = ZaziliWhite,
                        cornerRadius = 16.dp,
                        baseHeight = 4.dp,
                        fontSize = 13.sp
                    )
                    ZaziButton(
                        text = "Ups",
                        onClick = { GlobalAudioManager.playMiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .semantics { contentDescription = "Probar el efecto de error" },
                        containerColor = CoralAviso,
                        contentColor = ZaziliWhite,
                        cornerRadius = 16.dp,
                        baseHeight = 4.dp,
                        fontSize = 13.sp
                    )
                }

                if (efectos.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Sonidos extra",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 1.dp)
                    ) {
                        items(efectos, key = { it.id }) { efecto ->
                            FichaEfecto(
                                efecto = efecto,
                                darkTheme = darkTheme,
                                onClick = {
                                    GlobalAudioManager.reproducirEfectoExtra(
                                        contexto,
                                        efecto.resId
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Aún no hay sonidos extra. Los archivos que empiecen " +
                            "con sfx_ en res/raw aparecen aquí al compilar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }

            Column {
                Encabezado(
                    icono = { KawaiiHeartIcon(size = 20.dp) },
                    titulo = "CALMA",
                    subtitulo = "Baja el ruido y los brillos de la app"
                )
                Spacer(Modifier.height(10.dp))
                ZenModeToggle()
            }

            TarjetaAjuste {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        KawaiiLockIcon(size = 22.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Todo se queda en tu dispositivo",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Sin cuentas, sin internet y sin anuncios. " +
                                "Tu música, tu tema y tu progreso viven aquí.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Text(
                text = "Zazili 2.0 · Hecha con cariño para ti",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  CABECERA
// ---------------------------------------------------------------------------

@Composable
private fun BarraAjustes(onVolver: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        ZaziButton(
            text = "‹",
            onClick = {
                GlobalAudioManager.playPop()
                onVolver()
            },
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Volver al tablero" },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 8.dp,
            baseHeight = 4.dp,
            fontSize = 26.sp
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AJUSTES",
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
                text = "Tu casita, a tu manera",
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
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
        ) {
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                KawaiiGearIcon(size = 26.dp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  BLOQUES COMPARTIDOS
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaAjuste(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun Encabezado(
    icono: @Composable () -> Unit,
    titulo: String,
    subtitulo: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            icono()
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  TEMA
// ---------------------------------------------------------------------------

@Composable
private fun OpcionTema(
    modo: ModoTema,
    elegido: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val escala by animateFloatAsState(
        targetValue = if (elegido) 1f else 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escalaTema${modo.id}"
    )

    val cara = when {
        elegido -> MaterialTheme.colorScheme.primaryContainer
        darkTheme -> NightSurface
        else -> ZaziliWhite
    }
    val tinta = when {
        elegido -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    ZaziButton(
        text = modo.etiqueta,
        onClick = onClick,
        modifier = modifier
            .height(100.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .semantics {
                contentDescription = if (elegido) {
                    "Tema ${modo.etiqueta}. Elegido."
                } else {
                    "Elegir el tema ${modo.etiqueta}"
                }
            },
        containerColor = cara,
        contentColor = tinta,
        cornerRadius = 20.dp,
        baseHeight = 5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(text = modo.emoji, fontSize = 24.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = modo.etiqueta,
                    color = tinta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = modo.detalle,
                    color = tinta.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (elegido) {
                KawaiiCheckIcon(
                    size = 16.dp,
                    color = tinta,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PISTAS Y EFECTOS
// ---------------------------------------------------------------------------

@Composable
private fun FichaPista(
    pista: PistaZazili,
    sonando: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val cara = when {
        sonando -> MintGreen
        darkTheme -> NightSurface
        else -> ZaziliWhite
    }
    val tinta = when {
        sonando -> UniversoProfundo
        darkTheme -> NightTextPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    ZaziButton(
        text = pista.titulo,
        onClick = onClick,
        modifier = Modifier
            .width(178.dp)
            .height(68.dp)
            .semantics {
                contentDescription = if (sonando) {
                    "${pista.titulo}. Sonando ahora."
                } else {
                    "Escuchar ${pista.titulo}"
                }
            },
        containerColor = cara,
        contentColor = tinta,
        cornerRadius = 18.dp,
        baseHeight = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tinta.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                KawaiiNoteIcon(size = 22.dp)
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pista.titulo,
                    color = tinta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (sonando) "Sonando" else "Tocar",
                    color = tinta.copy(alpha = 0.68f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            if (sonando) {
                KawaiiCheckIcon(size = 16.dp, color = tinta)
            }
        }
    }
}

@Composable
private fun FichaEfecto(
    efecto: EfectoZazili,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val cara = if (darkTheme) NightSurface else ZaziliWhite
    val tinta = if (darkTheme) NightTextPrimary else MaterialTheme.colorScheme.onSurface

    ZaziButton(
        text = efecto.titulo,
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(60.dp)
            .semantics { contentDescription = "Probar el sonido ${efecto.titulo}" },
        containerColor = cara,
        contentColor = tinta,
        cornerRadius = 16.dp,
        baseHeight = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(tinta.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                KawaiiSparkIcon(size = 18.dp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = efecto.titulo,
                color = tinta,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  VOLUMEN
// ---------------------------------------------------------------------------

@Composable
private fun DeslizadorVolumen(
    titulo: String,
    valor: Float,
    onCambiar: (Float) -> Unit,
    descripcion: String
) {
    val porcentaje = (valor.coerceIn(0f, 1f) * 100f).toInt()

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$porcentaje%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            KawaiiVolumeDownIcon(size = 22.dp)
            Slider(
                value = valor,
                onValueChange = onCambiar,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .semantics { contentDescription = descripcion },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
            KawaiiVolumeUpIcon(size = 22.dp)
        }
    }
}
