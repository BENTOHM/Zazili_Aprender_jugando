package com.example.zazili.presentation.avatar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.CelebrationKonfetti
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.avatar.AvatarCategory
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.avatar.AvatarItem
import com.example.zazili.domain.avatar.EquipamientoAvatar
import com.example.zazili.domain.economia.ResultadoCompra
import com.example.zazili.domain.economia.ZazolEconomyManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tienditas 3B.
 *
 * Arriba vive el retrato en vivo y el saldo de Zazoles; abajo, la cuadrícula
 * filtrada por pestañas. Tocar un artículo libre lo pone o lo quita al
 * instante. Tocar uno bloqueado abre la confirmación de compra: si el saldo
 * alcanza, cae konfeti y la prenda se equipa sola; si no, Zazi avisa con
 * cariño que hay que ir a jugar.
 */
@Composable
fun AvatarShopScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val gestor = remember(contexto) { ZazolEconomyManager.obtener(contexto) }
    val estado by gestor.estado.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    val oscuro = isSystemInDarkTheme()
    val alcance = rememberCoroutineScope()

    var categoriaClave by rememberSaveable { mutableStateOf(AvatarCategory.ROPA.clave) }
    val categoria = AvatarCategory.entries.firstOrNull { it.clave == categoriaClave }
        ?: AvatarCategory.ROPA

    var objetivoId by rememberSaveable { mutableStateOf<String?>(null) }
    val objetivo = remember(objetivoId) { objetivoId?.let(AvatarContentFactory::porId) }

    var mensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var mensajeTriste by rememberSaveable { mutableStateOf(false) }
    var konfetti by rememberSaveable { mutableIntStateOf(0) }
    var comprando by remember { mutableStateOf(false) }

    val equipamiento = remember(estado.equipamiento) {
        AvatarContentFactory.equipamientoDesde(estado.equipamiento)
    }
    val resumen = remember(equipamiento) {
        equipamiento.articulos.joinToString(" · ") { it.name }
    }
    val lista = remember(categoria) { AvatarContentFactory.porCategoria(categoria) }

    LaunchedEffect(mensaje) {
        val actual = mensaje ?: return@LaunchedEffect
        delay(4800)
        if (mensaje == actual) mensaje = null
    }

    val intentarComprar: (AvatarItem) -> Unit = { articulo ->
        if (!comprando) {
            alcance.launch {
                comprando = true
                when (val resultado = gestor.unlockItem(articulo.id, articulo.price)) {
                    is ResultadoCompra.Exito -> {
                        gestor.equipar(articulo.category, articulo.id)
                        GlobalAudioManager.playSuccess()
                        konfetti += 1
                        mensajeTriste = false
                        mensaje = "¡${articulo.name} ya es tuyo! Se ve increíble."
                    }

                    is ResultadoCompra.SinFondos -> {
                        GlobalAudioManager.playMiss()
                        mensajeTriste = true
                        mensaje = "¡Uy! Te faltan Zazoles. ¡A jugar se ha dicho!"
                    }

                    ResultadoCompra.YaDesbloqueado -> {
                        gestor.equipar(articulo.category, articulo.id)
                    }

                    ResultadoCompra.NoExiste -> Unit
                }
                objetivoId = null
                comprando = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (zen) zenMutedBackground(oscuro) else MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            BarraSuperiorArmario(
                zazoles = estado.zazoles,
                zen = zen,
                onSalir = onSalir
            )

            if (!estado.listo) {
                LinearProgressIndicator(
                    color = MintGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TarjetaVistaPrevia(
                    equipamiento = equipamiento,
                    resumen = resumen,
                    zen = zen
                )
            }

            PestanasCategoria(
                seleccionada = categoria,
                onElegir = { elegida ->
                    GlobalAudioManager.playPop()
                    categoriaClave = elegida.clave
                }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(items = lista, key = { it.id }) { articulo ->
                    val desbloqueado = AvatarContentFactory.estaDesbloqueado(
                        articulo,
                        estado.desbloqueados
                    )
                    val equipado = equipamiento.de(articulo.category)?.id == articulo.id
                    CeldaArticulo(
                        item = articulo,
                        equipamiento = equipamiento,
                        desbloqueado = desbloqueado,
                        equipado = equipado,
                        onTocar = {
                            GlobalAudioManager.playPop()
                            when {
                                !desbloqueado -> objetivoId = articulo.id
                                equipado -> alcance.launch {
                                    gestor.desequipar(articulo.category)
                                }

                                else -> alcance.launch {
                                    gestor.equipar(articulo.category, articulo.id)
                                }
                            }
                        }
                    )
                }
            }
        }

        CelebrationKonfetti(
            trigger = konfetti,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
        )

        AnimatedVisibility(
            visible = mensaje != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            enter = slideInVertically { alto -> alto } + fadeIn(),
            exit = slideOutVertically { alto -> alto } + fadeOut()
        ) {
            BannerZazi(
                mensaje = mensaje.orEmpty(),
                triste = mensajeTriste,
                zen = zen
            )
        }
    }

    if (objetivo != null) {
        DialogoCompra(
            item = objetivo,
            saldo = estado.zazoles,
            comprando = comprando,
            onConfirmar = { intentarComprar(objetivo) },
            onCancelar = { if (!comprando) objetivoId = null }
        )
    }
}

// ---------------------------------------------------------------------------
//  BARRA SUPERIOR
// ---------------------------------------------------------------------------

@Composable
private fun BarraSuperiorArmario(
    zazoles: Int,
    zen: Boolean,
    onSalir: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        ZaziButton(
            text = "Salir",
            onClick = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = Modifier.size(44.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 22.dp,
            baseHeight = 3.dp
        ) {
            KawaiiArrowIcon(
                modifier = Modifier.graphicsLayer { rotationZ = 180f },
                size = 18.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.width(11.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Tienditas 3B",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Viste a tu Zazi con sus tesoros",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        MonedaZazoles(zazoles = zazoles, zen = zen)
    }
}

@Composable
private fun MonedaZazoles(zazoles: Int, zen: Boolean) {
    val saldo by animateIntAsState(
        targetValue = zazoles,
        animationSpec = tween(durationMillis = 620),
        label = "saldoZazoles"
    )
    val transicion = rememberInfiniteTransition(label = "brilloMoneda")
    val brillo = if (!zen) {
        transicion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing)
            ),
            label = "brillo"
        )
    } else {
        null
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, AmarilloOscuro.copy(alpha = 0.45f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .semantics {
                    contentDescription = "Tienes $saldo Zazoles"
                }
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                val radio = size.minDimension / 2f
                val centro = Offset(size.width / 2f, size.height / 2f)
                drawCircle(AmarilloVibrante, radio, centro)
                drawCircle(
                    color = AmarilloOscuro,
                    radius = radio,
                    center = centro,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = ZaziliWhite.copy(alpha = 0.65f),
                    radius = radio * 0.22f,
                    center = Offset(size.width * 0.34f, size.height * 0.30f)
                )

                val valorBrillo = brillo?.value ?: -1f
                if (valorBrillo >= 0f) {
                    val recorte = Path().apply { addOval(Rect(centro, radio * 0.92f)) }
                    clipPath(recorte) {
                        rotate(degrees = -26f, pivot = centro) {
                            val x = -size.width + valorBrillo * size.width * 2.6f
                            drawRoundRect(
                                color = ZaziliWhite.copy(alpha = 0.45f),
                                topLeft = Offset(x, -size.height),
                                size = Size(size.width * 0.26f, size.height * 3f),
                                cornerRadius = CornerRadius(size.width * 0.13f)
                            )
                        }
                    }
                }

                val zeta = Path().apply {
                    moveTo(centro.x - radio * 0.34f, centro.y - radio * 0.40f)
                    lineTo(centro.x + radio * 0.34f, centro.y - radio * 0.40f)
                    lineTo(centro.x - radio * 0.34f, centro.y + radio * 0.40f)
                    lineTo(centro.x + radio * 0.34f, centro.y + radio * 0.40f)
                }
                drawPath(
                    path = zeta,
                    color = UniversoProfundo,
                    style = Stroke(
                        width = radio * 0.16f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            Spacer(Modifier.width(6.dp))

            Text(
                text = "$saldo",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  VISTA PREVIA
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaVistaPrevia(
    equipamiento: EquipamientoAvatar,
    resumen: String,
    zen: Boolean
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (zen) 1.dp else 2.dp,
            if (zen) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            else LavandaClara
        ),
        shadowElevation = if (zen) 0.dp else 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Tu Zazi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = resumen.ifBlank { "Toca una prenda para vestirlo." },
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BotonAltavoz(
                    texto = if (resumen.isBlank()) {
                        "Tu Zazi no lleva accesorios"
                    } else {
                        "Tu Zazi lleva $resumen"
                    },
                    tamano = 32.dp,
                    colorFondo = LavandaClara,
                    descripcion = "Escuchar cómo va vestido tu Zazi"
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp)
            ) {
                ChildAvatarRenderer(
                    equipamiento = equipamiento,
                    modifier = Modifier.fillMaxSize(),
                    mostrarFondo = true,
                    animar = !zen
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PESTAÑAS
// ---------------------------------------------------------------------------

@Composable
private fun PestanasCategoria(
    seleccionada: AvatarCategory,
    onElegir: (AvatarCategory) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        AvatarCategory.entries.forEach { categoria ->
            val activa = categoria == seleccionada
            ZaziButton(
                text = categoria.etiqueta,
                onClick = { onElegir(categoria) },
                modifier = Modifier
                    .height(44.dp)
                    .semantics {
                        stateDescription = if (activa) "Pestaña elegida" else "Pestaña disponible"
                    },
                containerColor = if (activa) {
                    MagicalPurple
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (activa) {
                    ZaziliWhite
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                cornerRadius = 20.dp,
                baseHeight = 4.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = categoria.emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = categoria.etiqueta,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (activa) ZaziliWhite else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  CELDA DE ARTÍCULO
// ---------------------------------------------------------------------------

@Composable
private fun CeldaArticulo(
    item: AvatarItem,
    equipamiento: EquipamientoAvatar,
    desbloqueado: Boolean,
    equipado: Boolean,
    onTocar: () -> Unit
) {
    val vista = remember(equipamiento, item) { equipamiento.con(item) }
    val forma = RoundedCornerShape(22.dp)
    val borde = when {
        equipado -> BorderStroke(2.5.dp, MagicalPurple)
        desbloqueado -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
        else -> BorderStroke(1.5.dp, AmarilloOscuro.copy(alpha = 0.55f))
    }
    val fondo = if (desbloqueado) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val estadoAccesible = when {
        equipado -> "Puesto"
        desbloqueado -> "Desbloqueado"
        else -> "Cuesta ${item.price} Zazoles"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .clip(forma)
                .background(
                    if (desbloqueado) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                    } else {
                        AmarilloOscuro.copy(alpha = 0.20f)
                    }
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(fondo)
                .border(borde, forma)
                .clickable(
                    onClickLabel = when {
                        equipado -> "Quitar ${item.name}"
                        desbloqueado -> "Usar ${item.name}"
                        else -> "Comprar ${item.name}"
                    },
                    role = Role.Button,
                    onClick = onTocar
                )
                .semantics {
                    contentDescription = "${item.name}. $estadoAccesible"
                }
                .padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(102.dp)
                ) {
                    ChildAvatarRenderer(
                        equipamiento = vista,
                        modifier = Modifier.fillMaxSize(),
                        mostrarFondo = false,
                        animar = false,
                        descripcion = "Así se ve ${item.name}"
                    )
                    if (!desbloqueado) {
                        InsigniaMoneda(
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    if (equipado) {
                        InsigniaPuesta(
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.name,
                    fontSize = 12.5.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.weight(1f))

                PieCelda(
                    item = item,
                    desbloqueado = desbloqueado,
                    equipado = equipado
                )
            }
        }
    }
}

@Composable
private fun PieCelda(item: AvatarItem, desbloqueado: Boolean, equipado: Boolean) {
    when {
        equipado -> Etiqueta(
            color = VerdeDuo,
            texto = "¡Puesto!",
            mostrarPalomita = true
        )

        !desbloqueado && item.price > 0 -> Etiqueta(
            color = AmarilloSuave,
            texto = "${item.price}",
            mostrarMoneda = true,
            textoOscuro = true
        )

        !desbloqueado -> Etiqueta(
            color = AmarilloSuave,
            texto = "Gratis",
            textoOscuro = true
        )

        else -> Etiqueta(
            color = LavandaClara,
            texto = "Usar",
            textoOscuro = true
        )
    }
}

@Composable
private fun Etiqueta(
    color: androidx.compose.ui.graphics.Color,
    texto: String,
    mostrarMoneda: Boolean = false,
    mostrarPalomita: Boolean = false,
    textoOscuro: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            if (mostrarPalomita) {
                KawaiiCheckIcon(size = 12.dp, color = ZaziliWhite)
                Spacer(Modifier.width(3.dp))
            }
            if (mostrarMoneda) {
                KawaiiCoinIcon(size = 15.dp)
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = texto,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (textoOscuro) UniversoProfundo else ZaziliWhite
            )
        }
    }
}

@Composable
private fun InsigniaMoneda(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(23.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AmarilloOscuro.copy(alpha = 0.45f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            KawaiiLockIcon(size = 13.dp, color = UniversoProfundo)
        }
    }
}

@Composable
private fun InsigniaPuesta(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(23.dp),
        shape = CircleShape,
        color = VerdeDuo
    ) {
        Box(contentAlignment = Alignment.Center) {
            KawaiiCheckIcon(size = 13.dp, color = ZaziliWhite)
        }
    }
}

// ---------------------------------------------------------------------------
//  DIÁLOGO DE COMPRA
// ---------------------------------------------------------------------------

@Composable
private fun DialogoCompra(
    item: AvatarItem,
    saldo: Int,
    comprando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "¿Comprar «${item.name}»?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KawaiiCoinIcon(size = 24.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (item.price > 0) "${item.price} Zazoles" else "Gratis",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = AmarilloOscuro
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.descripcion.ifBlank { "Un tesoro para tu Zazi." },
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Tu saldo: $saldo Zazoles",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
            }
        },
        confirmButton = {
            ZaziButton(
                text = "¡Comprar!",
                onClick = onConfirmar,
                enabled = !comprando,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .height(46.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                cornerRadius = 18.dp,
                baseHeight = 5.dp,
                fontSize = 15.sp
            )
        },
        dismissButton = {
            ZaziButton(
                text = "Mejor luego",
                onClick = onCancelar,
                enabled = !comprando,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .height(46.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                cornerRadius = 18.dp,
                baseHeight = 5.dp,
                fontSize = 14.sp
            )
        }
    )
}

// ---------------------------------------------------------------------------
//  BANNER DE ZAZI
// ---------------------------------------------------------------------------

@Composable
private fun BannerZazi(mensaje: String, triste: Boolean, zen: Boolean) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (triste) CoralAviso.copy(alpha = 0.96f) else MintGreen.copy(alpha = 0.98f),
        border = BorderStroke(1.5.dp, UniversoProfundo.copy(alpha = 0.14f)),
        shadowElevation = if (zen) 0.dp else 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            KawaiiZazi(size = 54.dp, animar = !zen)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (triste) "Zazi dice:" else "¡Tesoro desbloqueado!",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = UniversoProfundo.copy(alpha = 0.62f)
                )
                Text(
                    text = mensaje,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = UniversoProfundo
                )
            }
        }
    }
}
