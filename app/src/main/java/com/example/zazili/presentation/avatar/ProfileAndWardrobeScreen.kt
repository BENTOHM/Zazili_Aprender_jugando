package com.example.zazili.presentation.avatar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.avatar.AvatarCategory
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.avatar.AvatarItem
import com.example.zazili.domain.avatar.EquipamientoAvatar
import com.example.zazili.domain.economia.ZazolEconomyManager
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Perfil y armario pixel del niño.
 *
 * El retrato de arriba es el mismo [ChildAvatarRenderer] del tablero y de la
 * Boutique Pixel: PNG de 64×64 píxeles, capas ordenadas por Z y
 * `FilterQuality.None`, sin una sola geometría vectorial.
 *
 * ReDiseño "premium pixel art":
 *  - El inventario se parte en dos bloques legibles: "En tu armario" (tuyas)
 *    y "Por desbloquear" (bloqueadas, con su precio y acceso a la tienda).
 *  - Tarjetas con sombra de bloque, borde grueso y rebote elástico al tocar.
 *  - Retrato en vivo enmarcado como vitrina, con el mismo aviso de estado
 *    "Guardado / Cargando" que la tienda.
 *
 * Todo cambio se pinta primero en una capa optimista y viaja a
 * [ZazolEconomyManager], que persiste el equipo en DataStore. Como el armario
 * escucha el flujo con `collectAsStateWithLifecycle`, la pieza queda puesta en
 * el mismo fotograma y las demás pantallas se enteran sin refrescar nada.
 */
@Composable
fun ProfileAndWardrobeScreen(
    onSalir: () -> Unit,
    onAbrirTienda: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val aplicacion = contexto.applicationContext as ZaziliApplication
    val gestor = remember(aplicacion) { ZazolEconomyManager.obtener(aplicacion) }
    val usuario = remember(aplicacion) { aplicacion.container.userManager }

    val estado by gestor.estado.collectAsStateWithLifecycle()
    val perfil by usuario.perfil.collectAsStateWithLifecycle(initialValue = null)
    val progreso by aplicacion.container.gameProgressManager.progress
        .collectAsStateWithLifecycle()

    val zen = collectZenMode()
    val oscuro = isSystemInDarkTheme()
    val alcance = rememberCoroutineScope()

    var categoriaClave by rememberSaveable { mutableStateOf(AvatarCategory.ROPA.clave) }
    val categoria = AvatarCategory.entries
        .firstOrNull { it.clave == categoriaClave } ?: AvatarCategory.ROPA

    var guardando by remember { mutableStateOf(0) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }
    var avisoTriste by rememberSaveable { mutableStateOf(false) }

    // Capa optimista: el píxel cambia antes de que DataStore conteste.
    var capaLocal by remember { mutableStateOf<Map<AvatarCategory, String?>>(emptyMap()) }

    val equipamiento = remember(estado.equipamiento, capaLocal) {
        val base = estado.equipamiento.toMutableMap()
        capaLocal.forEach { (ranura, piezaId) ->
            if (piezaId == null) base.remove(ranura) else base[ranura] = piezaId
        }
        AvatarContentFactory.equipamientoDesde(base)
    }

    LaunchedEffect(estado.equipamiento, capaLocal) {
        if (capaLocal.isEmpty()) return@LaunchedEffect
        val resueltas = capaLocal.keys.filter { ranura ->
            estado.equipamiento[ranura] == capaLocal[ranura]
        }
        if (resueltas.isNotEmpty()) {
            capaLocal = capaLocal - resueltas.toSet()
        }
    }

    val catalogo = remember(categoria) {
        AvatarContentFactory.porCategoria(categoria)
    }
    val desbloqueadas = remember(catalogo, estado.desbloqueados) {
        catalogo.filter { item ->
            AvatarContentFactory.estaDesbloqueado(item, estado.desbloqueados)
        }
    }
    val bloqueadas = remember(catalogo, estado.desbloqueados) {
        catalogo
            .filterNot { item ->
                AvatarContentFactory.estaDesbloqueado(item, estado.desbloqueados)
            }
            .sortedBy { item -> item.price }
    }

    val totalPiezas = remember { AvatarContentFactory.todos.size }
    val totalDesbloqueadas = remember(estado.desbloqueados) {
        AvatarContentFactory.todos.count { item ->
            AvatarContentFactory.estaDesbloqueado(item, estado.desbloqueados)
        }
    }
    val bloqueadasTotal = (totalPiezas - totalDesbloqueadas).coerceAtLeast(0)
    val nombre = perfil?.nombre?.trim().orEmpty().ifBlank { "tu Zazi" }

    LaunchedEffect(aviso) {
        val actual = aviso ?: return@LaunchedEffect
        delay(3200)
        if (aviso == actual) aviso = null
    }

    val alternar: (AvatarItem) -> Unit = { item ->
        if (!AvatarContentFactory.estaDesbloqueado(item, estado.desbloqueados)) {
            GlobalAudioManager.playPop()
            avisoTriste = false
            aviso = "${item.name} te espera en la Boutique Pixel."
            onAbrirTienda()
        } else {
            val puesto = equipamiento.de(item.category)?.id == item.id
            capaLocal = capaLocal + (item.category to if (puesto) null else item.id)
            guardando += 1
            alcance.launch {
                runCatching {
                    if (puesto) {
                        gestor.desequipar(item.category)
                    } else {
                        gestor.equipar(item.category, item.id)
                    }
                }.onSuccess {
                    if (puesto) {
                        GlobalAudioManager.playPop()
                        avisoTriste = false
                        aviso = "Quitaste ${item.name}."
                    } else {
                        GlobalAudioManager.playSuccess()
                        avisoTriste = false
                        aviso = "¡${item.name} ya está puesto!"
                    }
                }.onFailure {
                    capaLocal = capaLocal - item.category
                    GlobalAudioManager.playMiss()
                    avisoTriste = true
                    aviso = "No pudimos cambiar tu look. ¿Lo intentamos otra vez?"
                }
                guardando = (guardando - 1).coerceAtLeast(0)
            }
        }
    }

    val fondo = if (zen) {
        zenMutedBackground(oscuro)
    } else {
        MaterialTheme.colorScheme.background
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraArmario(
                nombre = nombre,
                nivel = progreso.level,
                zazoles = estado.zazoles,
                zen = zen,
                onSalir = onSalir
            )

            AnimatedVisibility(visible = guardando > 0) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MagicalPurple,
                    trackColor = LavandaClara.copy(alpha = 0.55f)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 8.dp,
                    bottom = 26.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .navigationBarsPadding()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    VistaPreviaPerfil(
                        equipamiento = equipamiento,
                        nombre = nombre,
                        nivel = progreso.level,
                        zen = zen,
                        listo = estado.listo
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilaEstadisticas(
                        puestas = equipamiento.articulos.size,
                        desbloqueadas = totalDesbloqueadas,
                        total = totalPiezas,
                        zazoles = estado.zazoles
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    PestanasArmario(
                        seleccionada = categoria,
                        onElegir = { elegida ->
                            GlobalAudioManager.playPop()
                            categoriaClave = elegida.clave
                        }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    CabeceraSeccion(
                        titulo = "EN TU ARMARIO",
                        detalle = "${desbloqueadas.size} de ${catalogo.size}",
                        emoji = categoria.emoji,
                        etiqueta = categoria.etiqueta,
                        color = VerdeDuo,
                        subtitulo = "Toca una pieza para ponerla o quitarla al instante."
                    )
                }

                if (desbloqueadas.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AvisoVacio(
                            texto = "Todavía no tienes piezas de ${categoria.etiqueta.lowercase()}. " +
                                "Las encuentras en la Boutique Pixel."
                        )
                    }
                } else {
                    items(items = desbloqueadas, key = { item -> item.id }) { item ->
                        CeldaArmario(
                            equipamiento = equipamiento,
                            item = item,
                            equipado = equipamiento.de(item.category)?.id == item.id,
                            desbloqueada = true,
                            onClick = { alternar(item) }
                        )
                    }
                }

                if (bloqueadas.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CabeceraSeccion(
                            titulo = "POR DESBLOQUEAR",
                            detalle = "${bloqueadas.size}",
                            emoji = "🔒",
                            etiqueta = "Boutique Pixel",
                            color = LavandaMedia,
                            subtitulo = "Se compran con Zazoles. Toca una para ir a la tienda."
                        )
                    }

                    items(items = bloqueadas, key = { item -> "bloqueada_${item.id}" }) { item ->
                        CeldaArmario(
                            equipamiento = equipamiento,
                            item = item,
                            equipado = false,
                            desbloqueada = false,
                            onClick = { alternar(item) }
                        )
                    }
                }

                if (bloqueadasTotal > 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ZaziButton(
                            text = "Ir a la Boutique Pixel",
                            onClick = {
                                GlobalAudioManager.playPop()
                                onAbrirTienda()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .padding(top = 6.dp),
                            containerColor = AmarilloVibrante,
                            contentColor = UniversoProfundo,
                            cornerRadius = 22.dp,
                            baseHeight = 7.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                KawaiiLockIcon(size = 24.dp, color = UniversoProfundo)
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = if (bloqueadasTotal == 1) {
                                            "1 look te espera en la Boutique"
                                        } else {
                                            "$bloqueadasTotal looks te esperan en la Boutique"
                                        },
                                        color = UniversoProfundo,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Ir a la Boutique Pixel",
                                        color = UniversoProfundo.copy(alpha = 0.70f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                KawaiiArrowIcon(size = 20.dp, color = UniversoProfundo)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = aviso != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            enter = slideInVertically { alto -> alto } + fadeIn(),
            exit = slideOutVertically { alto -> alto } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (avisoTriste) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MintGreen
                },
                border = BorderStroke(2.dp, UniversoProfundo),
                shadowElevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (avisoTriste) {
                        KawaiiLockIcon(size = 20.dp, color = UniversoProfundo)
                    } else {
                        KawaiiStarIcon(size = 20.dp, color = AmarilloVibrante)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = aviso.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  BARRA SUPERIOR
// ---------------------------------------------------------------------------

@Composable
private fun BarraArmario(
    nombre: String,
    nivel: Int,
    zazoles: Int,
    zen: Boolean,
    onSalir: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        ZaziButton(
            text = "Salir",
            onClick = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = Modifier
                .width(72.dp)
                .height(46.dp),
            containerColor = LavandaClara,
            contentColor = UniversoProfundo,
            cornerRadius = 14.dp,
            baseHeight = 4.dp,
            fontSize = 13.sp
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MI ARMARIO PIXEL",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$nombre · Nivel $nivel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (zen) {
                AmarilloSuave.copy(alpha = 0.22f)
            } else {
                AmarilloSuave.copy(alpha = 0.60f)
            },
            border = BorderStroke(1.5.dp, UniversoProfundo.copy(alpha = 0.25f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                KawaiiCoinIcon(size = 20.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$zazoles",
                    style = MaterialTheme.typography.titleSmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  VISTA PREVIA PIXEL
// ---------------------------------------------------------------------------

/**
 * Vitrina del retrato en vivo: marco grueso, sombra de bloque y un marco
 * interior tenue para que el sprite 64×64 destaque sin ruido visual.
 */
@Composable
private fun VistaPreviaPerfil(
    equipamiento: EquipamientoAvatar,
    nombre: String,
    nivel: Int,
    zen: Boolean,
    listo: Boolean
) {
    val forma = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(316.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 7.dp)
                .clip(forma)
                .background(UniversoProfundo)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(
                    Brush.linearGradient(
                        colors = listOf(UniversoProfundo, MagicalPurple, UniversoMedio)
                    )
                )
                .border(
                    width = 3.dp,
                    color = if (zen) LavandaMedia else MintGreen,
                    shape = forma
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RETRATO PIXEL EN VIVO",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MintGreen,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleLarge,
                        color = ZaziliWhite,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Nivel $nivel · 64 PX",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = ZaziliWhite.copy(alpha = 0.70f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (listo) MintGreen else AmarilloVibrante,
                    border = BorderStroke(1.5.dp, UniversoProfundo.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        if (listo) {
                            KawaiiCheckIcon(size = 15.dp, color = UniversoProfundo)
                        } else {
                            KawaiiStarIcon(size = 15.dp, color = UniversoProfundo)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (listo) "Guardado" else "Cargando",
                            fontFamily = FontFamily.Monospace,
                            color = UniversoProfundo,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(212.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AmarilloSuave.copy(alpha = 0.12f))
                        .border(
                            width = 2.dp,
                            color = ZaziliWhite.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ChildAvatarRenderer(
                        equipamiento = equipamiento,
                        modifier = Modifier.fillMaxSize(),
                        mostrarFondo = true,
                        animar = !zen,
                        descripcion = "Vista previa pixel de $nombre con su ropa puesta"
                    )
                }
            }

            Text(
                text = if (zen) {
                    "Modo Zen: el retrato se queda quietecito"
                } else {
                    "Toca una pieza para ponerla o quitarla al instante"
                },
                style = MaterialTheme.typography.labelSmall,
                color = ZaziliWhite.copy(alpha = 0.78f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  ESTADÍSTICAS
// ---------------------------------------------------------------------------

@Composable
private fun FilaEstadisticas(
    puestas: Int,
    desbloqueadas: Int,
    total: Int,
    zazoles: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TarjetaEstadistica(
            etiqueta = "Puestas",
            valor = "$puestas",
            detalle = "de 8 ranuras",
            color = MintGreen,
            modifier = Modifier.weight(1f)
        )
        TarjetaEstadistica(
            etiqueta = "Tu look",
            valor = "$desbloqueadas",
            detalle = "de $total piezas",
            color = LavandaMedia,
            modifier = Modifier.weight(1f)
        )
        TarjetaEstadistica(
            etiqueta = "Zazoles",
            valor = "$zazoles",
            detalle = "para gastar",
            color = AmarilloVibrante,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TarjetaEstadistica(
    etiqueta: String,
    valor: String,
    detalle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, UniversoProfundo.copy(alpha = 0.10f)),
        shadowElevation = 2.dp,
        modifier = modifier.heightIn(min = 78.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = etiqueta,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .fillMaxWidth(0.55f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = detalle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  CATEGORÍAS Y CABECERAS DE SECCIÓN
// ---------------------------------------------------------------------------

@Composable
private fun PestanasArmario(
    seleccionada: AvatarCategory,
    onElegir: (AvatarCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AvatarCategory.entries.forEach { categoria ->
            val activa = categoria == seleccionada
            ZaziButton(
                text = categoria.etiqueta,
                onClick = { onElegir(categoria) },
                modifier = Modifier.height(46.dp),
                containerColor = if (activa) MagicalPurple else LavandaClara,
                contentColor = if (activa) ZaziliWhite else UniversoProfundo,
                cornerRadius = 14.dp,
                baseHeight = 4.dp,
                fontSize = 12.sp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = categoria.emoji, fontSize = 13.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = categoria.etiqueta,
                        color = if (activa) ZaziliWhite else UniversoProfundo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Separador de bloque con contador. Mantiene las dos listas —tuyas y
 * bloqueadas— visualmente distintas sin depender solo del color.
 */
@Composable
private fun CabeceraSeccion(
    titulo: String,
    detalle: String,
    emoji: String,
    etiqueta: String,
    color: Color,
    subtitulo: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.22f))
                .border(1.5.dp, color.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 17.sp)
        }

        Spacer(Modifier.width(9.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = titulo,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = color.copy(alpha = 0.20f)
                ) {
                    Text(
                        text = detalle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "$etiqueta · $subtitulo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AvisoVacio(texto: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, LavandaMedia.copy(alpha = 0.35f))
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

// ---------------------------------------------------------------------------
//  PIEZAS DEL ARMARIO
// ---------------------------------------------------------------------------

/** Acento pastel por escalón de precio: el mismo criterio de la tienda. */
private fun acentoDeRareza(item: AvatarItem): Color = when {
    item.price >= 400 -> AmarilloVibrante
    item.price >= 200 -> MagicalPurple
    item.price >= 100 -> AzulCielo
    else -> LavandaMedia
}

/**
 * Tarjeta de inventario con estética premium pixel: sombra de bloque, borde
 * grueso y rebote elástico. Las piezas tuyas se ven a color; las bloqueadas
 * bajan el contraste y muestran candado y precio.
 */
@Composable
private fun CeldaArmario(
    equipamiento: EquipamientoAvatar,
    item: AvatarItem,
    equipado: Boolean,
    desbloqueada: Boolean,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(18.dp)
    val acento = acentoDeRareza(item)
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "presionCeldaArmario"
    )

    val fondo = when {
        equipado -> MintGreen.copy(alpha = 0.34f)
        desbloqueada -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    }
    val borde = when {
        equipado -> VerdeDuo
        desbloqueada -> LavandaMedia.copy(alpha = 0.55f)
        else -> LavandaMedia.copy(alpha = 0.30f)
    }
    val vistaPrevia = remember(item, equipamiento) { equipamiento.con(item) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 216.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .clip(forma)
                .background(UniversoProfundo.copy(alpha = if (desbloqueada) 0.82f else 0.30f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(fondo)
                .border(width = if (equipado) 3.dp else 1.5.dp, color = borde, shape = forma)
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = when {
                        equipado -> "Quitar ${item.name}"
                        desbloqueada -> "Poner ${item.name}"
                        else -> "Ver ${item.name} en la Boutique Pixel"
                    }
                ) { onClick() }
                .semantics {
                    contentDescription = when {
                        equipado -> "${item.name}, puesto. Toca para quitarlo."
                        desbloqueada -> "${item.name}, en tu armario. Toca para ponerlo."
                        else -> "${item.name}, bloqueado. Cuesta ${item.price} Zazoles. " +
                            "Toca para abrir la Boutique Pixel."
                    }
                }
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            equipado -> VerdeDuo.copy(alpha = 0.14f)
                            desbloqueada -> LavandaClara.copy(alpha = 0.85f)
                            else -> LavandaClara.copy(alpha = 0.40f)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = acento.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                ChildAvatarRenderer(
                    equipamiento = vistaPrevia,
                    modifier = Modifier.fillMaxSize(),
                    mostrarFondo = false,
                    animar = false,
                    descripcion = "${item.name} en tu Zazi pixel"
                )
                if (!desbloqueada) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmarilloVibrante),
                        contentAlignment = Alignment.Center
                    ) {
                        KawaiiLockIcon(size = 16.dp, color = UniversoProfundo)
                    }
                }
                if (equipado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VerdeDuo),
                        contentAlignment = Alignment.Center
                    ) {
                        KawaiiCheckIcon(size = 16.dp, color = ZaziliWhite)
                    }
                }
            }

            Spacer(Modifier.height(7.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.category.emoji,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.category.etiqueta.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                when {
                    equipado -> Text(
                        text = "PUESTO",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = VerdeDuo,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )

                    desbloqueada -> Text(
                        text = if (item.price == 0) "GRATIS" else "TUYO",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MagicalPurple,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )

                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        KawaiiCoinIcon(size = 12.dp)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${item.price}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = UniversoProfundo.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (desbloqueada) 1f else 0.62f
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = when {
                    equipado -> "Toca para quitar"
                    desbloqueada -> "Toca para poner"
                    else -> "En la Boutique Pixel"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (desbloqueada) 0.55f else 0.42f
                ),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
