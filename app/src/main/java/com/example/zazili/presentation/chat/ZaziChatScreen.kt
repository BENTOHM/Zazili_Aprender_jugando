package com.example.zazili.presentation.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.R
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenMutedSurface
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.PI
import kotlin.math.sin

/**
 * Chat infantil con Zazi.
 *
 * El niño escribe frases cortas (máximo 100 caracteres, con contador) y Zazi
 * contesta en burbujas blancas con borde suave. Mientras Zazi piensa se ven
 * tres puntitos que botan, y si la red o la llave fallan aparece un snackbar
 * amable en vez de un error técnico.
 *
 * El teclado se esconde al enviar y el foco se suelta, para que la pantalla
 * vuelva a quedar quieta. En modo zen el fondo y la barra de escritura bajan
 * de contraste sin perder legibilidad.
 */
@Composable
fun ZaziChatScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ZaziChatViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()

    val zen = collectZenMode()
    val fondo = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }
    val superficieBarra = if (zen) {
        zenMutedSurface(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.surface
    }

    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lista = rememberLazyListState()

    val enviarMensaje: () -> Unit = {
        if (estado.puedeEnviar) {
            viewModel.enviar()
            foco.clearFocus()
            teclado?.hide()
        }
    }

    LaunchedEffect(aviso) {
        val texto = aviso ?: return@LaunchedEffect
        foco.clearFocus()
        teclado?.hide()
        snackbarHostState.showSnackbar(
            message = texto,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
        viewModel.limpiarAviso()
    }

    LaunchedEffect(estado.mensajes.size, estado.cargando) {
        val total = estado.mensajes.size + if (estado.cargando) 1 else 0
        if (total > 0) {
            lista.animateScrollToItem(total - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = fondo,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
                snackbar = { datos ->
                    Snackbar(
                        snackbarData = datos,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = UniversoProfundo,
                        contentColor = ZaziliWhite
                    )
                }
            )
        }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
        ) {
            CabeceraChat(
                onVolver = onVolver,
                vozActivada = estado.vozActivada,
                onAlternarVoz = viewModel::alternarVoz,
                colorTitulo = MaterialTheme.colorScheme.onBackground
            )

            LazyColumn(
                state = lista,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = estado.mensajes, key = { it.id }) { mensaje ->
                    BurbujaMensaje(mensaje = mensaje)
                }
                if (estado.cargando) {
                    item(key = "zazi-escribiendo") {
                        IndicadorEscribiendo()
                    }
                }
            }

            BarraEscritura(
                estado = estado,
                onEscribir = viewModel::escribir,
                onEnviar = enviarMensaje,
                colorFondo = superficieBarra
            )
        }
    }
}

@Composable
private fun CabeceraChat(
    onVolver: () -> Unit,
    vozActivada: Boolean,
    onAlternarVoz: () -> Unit,
    colorTitulo: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZaziButton(
            text = "Volver",
            onClick = {
                GlobalAudioManager.playPop()
                onVolver()
            },
            modifier = Modifier.size(44.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 22.dp,
            baseHeight = 4.dp
        ) {
            KawaiiArrowIcon(
                modifier = Modifier.graphicsLayer { rotationZ = 180f },
                size = 18.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Chat con Zazi",
                style = MaterialTheme.typography.headlineSmall,
                color = colorTitulo,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Respuestas cortitas, con calma",
                style = MaterialTheme.typography.bodySmall,
                color = colorTitulo.copy(alpha = 0.62f)
            )
        }
        Spacer(Modifier.width(10.dp))
        ZaziButton(
            text = if (vozActivada) "Silenciar la voz de Zazi" else "Activar la voz de Zazi",
            onClick = {
                GlobalAudioManager.playPop()
                onAlternarVoz()
            },
            modifier = Modifier.size(44.dp),
            containerColor = if (vozActivada) LavandaClara else MaterialTheme.colorScheme.surface,
            contentColor = UniversoProfundo,
            cornerRadius = 22.dp,
            baseHeight = 4.dp
        ) {
            Icon(
                imageVector = if (vozActivada) {
                    Icons.AutoMirrored.Rounded.VolumeUp
                } else {
                    Icons.AutoMirrored.Rounded.VolumeOff
                },
                contentDescription = null,
                tint = if (vozActivada) {
                    UniversoProfundo
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        AvatarZazi(tamano = 42.dp)
    }
}

@Composable
private fun BurbujaMensaje(mensaje: MensajeChat) {
    val esNino = mensaje.autor == AutorMensaje.NINO
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (esNino) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (!esNino) {
                AvatarZazi(tamano = 36.dp)
                Spacer(Modifier.width(8.dp))
            }
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = if (esNino) 6.dp else 20.dp,
                    bottomStart = if (esNino) 20.dp else 6.dp
                ),
                color = if (esNino) AmarilloSuave else ZaziliWhite,
                border = if (esNino) null else BorderStroke(1.dp, LavandaMedia.copy(alpha = 0.55f)),
                shadowElevation = if (esNino) 0.dp else 1.dp
            ) {
                Text(
                    text = mensaje.texto,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun IndicadorEscribiendo() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarZazi(tamano = 36.dp)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomEnd = 20.dp,
                bottomStart = 6.dp
            ),
            color = ZaziliWhite,
            border = BorderStroke(1.dp, LavandaMedia.copy(alpha = 0.55f)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics { contentDescription = "Zazi está pensando" },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { indice ->
                    PuntoRebote(indice = indice)
                }
            }
        }
    }
}

@Composable
private fun PuntoRebote(indice: Int) {
    val transicion = rememberInfiniteTransition(label = "puntosZazi")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(indice * 130)
        ),
        label = "puntoZazi$indice"
    )
    val salto = sin(fase * 2f * PI.toFloat()).coerceAtLeast(0f) * 5f
    Box(
        modifier = Modifier
            .graphicsLayer { translationY = -salto.dp.toPx() }
            .size(8.dp)
            .clip(CircleShape)
            .background(MagicalPurple.copy(alpha = 0.85f))
    )
}

@Composable
private fun AvatarZazi(tamano: Dp = 36.dp) {
    Surface(
        modifier = Modifier.size(tamano),
        shape = CircleShape,
        color = LavandaClara,
        border = BorderStroke(1.dp, LavandaMedia.copy(alpha = 0.8f))
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Zazi",
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(5.dp)
        )
    }
}

@Composable
private fun BarraEscritura(
    estado: ZaziChatUiState,
    onEscribir: (String) -> Unit,
    onEnviar: () -> Unit,
    colorFondo: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colorFondo,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = estado.borrador,
                onValueChange = onEscribir,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(text = "Escribe algo cortito…")
                },
                shape = RoundedCornerShape(20.dp),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onEnviar() }),
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${estado.borrador.length}/${ZaziChatViewModel.MAX_CARACTERES}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (estado.borrador.length >= 90) {
                                CoralAviso
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                }
            )

            ZaziButton(
                text = "Enviar mensaje",
                onClick = onEnviar,
                modifier = Modifier.size(width = 92.dp, height = 58.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                enabled = estado.puedeEnviar,
                cornerRadius = 18.dp,
                baseHeight = 5.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    KawaiiArrowIcon(
                        size = 18.dp,
                        color = ZaziliWhite.copy(alpha = if (estado.puedeEnviar) 1f else 0.6f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Enviar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZaziliWhite.copy(alpha = if (estado.puedeEnviar) 1f else 0.6f)
                    )
                }
            }
        }
    }
}
