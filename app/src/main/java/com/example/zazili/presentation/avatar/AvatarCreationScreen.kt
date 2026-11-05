package com.example.zazili.presentation.avatar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.avatar.AvatarCategory
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.avatar.AvatarItem
import com.example.zazili.domain.economia.ZazolEconomyManager
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Creador y armario pixel del niño.
 *
 * La vista previa usa [ChildAvatarRenderer]: puras capas de pixel art de 64×64
 * escaladas con `ContentScale.Fit` y `FilterQuality.None`, así que no hay
 * bilineal ni bordes borrosos. Todo el marco es retro y chunky: bordes
 * gruesos, sombras duras y nombres en monoespaciada.
 *
 * El cambio de ropa es optimista: la pieza se pinta al instante y DataStore
 * confirma en segundo plano. Las piezas bloqueadas muestran su precio y
 * llevan a la Tienda de Zazi; las compradas se ponen o se quitan desde aquí.
 */
@Composable
fun AvatarCreationScreen(
    onSalir: () -> Unit,
    onAbrirTienda: () -> Unit,
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

    var capaLocal by remember { mutableStateOf<Map<AvatarCategory, String?>>(emptyMap()) }
    var cambiando by remember { mutableStateOf(false) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }
    var avisoTriste by rememberSaveable { mutableStateOf(false) }

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

    val catalogo = remember(categoria) { AvatarContentFactory.porCategoria(categoria) }
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
    val lista = remember(desbloqueadas, bloqueadas) { desbloqueadas + bloqueadas }

    LaunchedEffect(aviso) {
        val actual = aviso ?: return@LaunchedEffect
        delay(3200)
        if (aviso == actual) aviso = null
    }

    val alternar: (AvatarItem) -> Unit = { item ->
        if (!cambiando) {
            if (!AvatarContentFactory.estaDesbloqueado(item, estado.desbloqueados)) {
                GlobalAudioManager.playPop()
                avisoTriste = false
                aviso = "${item.name} te espera en la Tienda de Zazi."
                onAbrirTienda()
            } else {
                val puesto = equipamiento.de(item.category)?.id == item.id
                capaLocal = capaLocal + (item.category to if (puesto) null else item.id)
                cambiando = true
                alcance.launch {
                    runCatching {
                        if (puesto) {
                            gestor.desequipar(item.category)
                        } else {
                            gestor.equipar(item.category, item.id)
                        }
                    }.onSuccess {
                        avisoTriste = false
                        if (puesto) {
                            GlobalAudioManager.playPop()
                            aviso = "Quitaste ${item.name}."
                        } else {
                            GlobalAudioManager.playSuccess()
                            aviso = "¡${item.name} ya está puesto!"
                        }
                    }.onFailure {
                        capaLocal = capaLocal - item.category
                        GlobalAudioManager.playMiss()
                        avisoTriste = true
                        aviso = "No pudimos cambiar tu look. ¿Lo intentamos otra vez?"
                    }
                    cambiando = false
                }
            }
        }
    }

    val quitarRanura: () -> Unit = {
        val puesta = equipamiento.de(categoria)
        if (puesta != null && !cambiando) {
            capaLocal = capaLocal + (categoria to null)
            cambiando = true
            alcance.launch {
                runCatching { gestor.desequipar(categoria) }
                    .onSuccess {
                        GlobalAudioManager.playPop()
                        avisoTriste = false
                        aviso = "Quitaste ${puesta.name}."
                    }
                    .onFailure {
                        capaLocal = capaLocal - categoria
                        GlobalAudioManager.playMiss()
                        avisoTriste = true
                        aviso = "No pudimos quitar esa pieza. ¿Lo intentamos otra vez?"
                    }
                cambiando = false
            }
        }
    }

    val fondo = if (zen) zenMutedBackground(oscuro) else SoftCream
    val resumen = remember(equipamiento) {
        equipamiento.articulos.joinToString(" · ") { it.name }
    }

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
            BarraCreadorPixel(
                zazoles = estado.zazoles,
                onSalir = {
                    GlobalAudioManager.playPop()
                    onSalir()
                }
            )

            AnimatedVisibility(visible = cambiando || !estado.listo) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MagicalPurple,
                    trackColor = LavandaClara.copy(alpha = 0.55f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VistaPreviaPixel(
                    equipamiento = equipamiento,
                    resumen = resumen,
                    zen = zen
                )
                FilaRanuras(equipamiento = equipamiento)
            }

            PestanasPixel(
                seleccionada = categoria,
                habilitadas = !cambiando,
                onElegir = { elegida ->
                    GlobalAudioManager.playPop()
                    categoriaClave = elegida.clave
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "RANURA: ${categoria.etiqueta.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = UniversoProfundo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${desbloqueadas.size}/${catalogo.size}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = UniversoProfundo.copy(alpha = 0.55f)
                )
                if (equipamiento.de(categoria) != null) {
                    Spacer(Modifier.width(10.dp))
                    QuitarChip(
                        habilitado = !cambiando,
                        onClick = quitarRanura
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(items = lista, key = { item -> item.id }) { item ->
                    val desbloqueada = AvatarContentFactory
                        .estaDesbloqueado(item, estado.desbloqueados)
                    val equipado = desbloqueada &&
                        equipamiento.de(item.category)?.id == item.id
                    CeldaPixel(
                        item = item,
                        equipamiento = equipamiento,
                        desbloqueada = desbloqueada,
                        equipado = equipado,
                        habilitada = !cambiando,
                        onClick = { alternar(item) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                ZaziButton(
                    text = "¡Listo!",
                    onClick = {
                        GlobalAudioManager.playSuccess()
                        onSalir()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    containerColor = VerdeDuo,
                    contentColor = ZaziliWhite,
                    cornerRadius = 12.dp,
                    baseHeight = 7.dp,
                    fontSize = 18.sp
                )
            }
        }

        AnimatedVisibility(
            visible = aviso != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 84.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            enter = slideInVertically { alto -> alto } + fadeIn(),
            exit = slideOutVertically { alto -> alto } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (avisoTriste) CoralAviso.copy(alpha = 0.96f) else MintGreen,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = UniversoProfundo
                ),
                shadowElevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (avisoTriste) {
                        KawaiiLockIcon(size = 18.dp, color = UniversoProfundo)
                    } else {
                        KawaiiCheckIcon(size = 18.dp, color = UniversoProfundo)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = aviso.orEmpty(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = UniversoProfundo,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
private fun BarraCreadorPixel(
    zazoles: Int,
    onSalir: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        ZaziButton(
            text = "‹",
            onClick = onSalir,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Volver al menú" },
            containerColor = ZaziliWhite,
            contentColor = UniversoProfundo,
            cornerRadius = 8.dp,
            baseHeight = 4.dp,
            fontSize = 26.sp
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CREADOR PIXEL",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = UniversoProfundo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Viste a tu Zazi píxel por píxel",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = UniversoProfundo.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AmarilloSuave,
            border = androidx.compose.foundation.BorderStroke(2.dp, UniversoProfundo)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .semantics { contentDescription = "Tienes $zazoles Zazoles" }
            ) {
                KawaiiCoinIcon(size = 18.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$zazoles",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = UniversoProfundo,
                    maxLines = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  VISTA PREVIA
// ---------------------------------------------------------------------------

@Composable
private fun VistaPreviaPixel(
    equipamiento: com.example.zazili.domain.avatar.EquipamientoAvatar,
    resumen: String,
    zen: Boolean
) {
    val forma = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(272.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 6.dp)
                .clip(forma)
                .background(UniversoProfundo)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(ZaziliWhite)
                .border(3.dp, UniversoProfundo, forma)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val lado = 14.dp.toPx()
                var fila = 0
                var y = 0f
                while (y < size.height) {
                    var columna = 0
                    var x = 0f
                    while (x < size.width) {
                        if ((fila + columna) % 2 == 0) {
                            drawRect(
                                color = Color(0xFFEFE6FA),
                                topLeft = Offset(x, y),
                                size = Size(lado, lado)
                            )
                        }
                        x += lado
                        columna++
                    }
                    y += lado
                    fila++
                }
            }

            ChildAvatarRenderer(
                equipamiento = equipamiento,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 10.dp),
                mostrarFondo = false,
                animar = !zen,
                descripcion = if (resumen.isBlank()) {
                    "Vista previa de tu Zazi pixel, sin accesorios"
                } else {
                    "Vista previa de tu Zazi pixel con $resumen"
                }
            )

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = UniversoProfundo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "VISTA PREVIA 64×64",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = ZaziliWhite,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            if (zen) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MagicalPurple,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "MODO ZEN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = ZaziliWhite,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaRanuras(equipamiento: com.example.zazili.domain.avatar.EquipamientoAvatar) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AvatarCategory.entries.forEach { categoria ->
            val puesta = equipamiento.de(categoria) != null
            val forma = RoundedCornerShape(5.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(forma)
                    .background(if (puesta) MintGreen else ZaziliWhite)
                    .border(
                        width = if (puesta) 2.dp else 1.dp,
                        color = if (puesta) UniversoProfundo else UniversoProfundo.copy(alpha = 0.30f),
                        shape = forma
                    )
                    .semantics {
                        contentDescription = if (puesta) {
                            "${categoria.etiqueta}: ocupada"
                        } else {
                            "${categoria.etiqueta}: vacía"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoria.emoji,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PESTAÑAS
// ---------------------------------------------------------------------------

@Composable
private fun PestanasPixel(
    seleccionada: AvatarCategory,
    habilitadas: Boolean,
    onElegir: (AvatarCategory) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        AvatarCategory.entries.forEach { categoria ->
            PestanaPixel(
                categoria = categoria,
                activa = categoria == seleccionada,
                habilitada = habilitadas,
                onClick = { onElegir(categoria) }
            )
        }
    }
}

@Composable
private fun PestanaPixel(
    categoria: AvatarCategory,
    activa: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada && habilitada) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "presionPestanaPixel"
    )
    val forma = RoundedCornerShape(7.dp)

    Box(modifier = Modifier.graphicsLayer { scaleX = escala; scaleY = escala }) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(forma)
                .background(UniversoProfundo.copy(alpha = 0.85f))
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(forma)
                .background(if (activa) MagicalPurple else ZaziliWhite)
                .border(
                    width = if (activa) 3.dp else 2.dp,
                    color = UniversoProfundo,
                    shape = forma
                )
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    enabled = habilitada,
                    role = Role.Button,
                    onClickLabel = "Ver ${categoria.etiqueta}",
                    onClick = onClick
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(text = categoria.emoji, fontSize = 13.sp)
            Spacer(Modifier.width(5.dp))
            Text(
                text = categoria.etiqueta.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.6.sp,
                color = if (activa) ZaziliWhite else UniversoProfundo,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuitarChip(
    habilitado: Boolean,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .clip(forma)
            .background(ZaziliWhite)
            .border(2.dp, UniversoProfundo, forma)
            .clickable(
                enabled = habilitado,
                role = Role.Button,
                onClickLabel = "Quitar la pieza de esta ranura",
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "QUITAR",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            color = UniversoProfundo
        )
    }
}

// ---------------------------------------------------------------------------
//  CELDAS DEL ARMARIO
// ---------------------------------------------------------------------------

@Composable
private fun CeldaPixel(
    item: AvatarItem,
    equipamiento: com.example.zazili.domain.avatar.EquipamientoAvatar,
    desbloqueada: Boolean,
    equipado: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    val vista = remember(equipamiento, item) { equipamiento.con(item) }
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada && habilitada) 0.955f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "presionCeldaPixel"
    )

    val forma = RoundedCornerShape(9.dp)
    val fondo = when {
        equipado -> MintGreen.copy(alpha = 0.55f)
        !desbloqueada -> AmarilloSuave.copy(alpha = 0.50f)
        else -> ZaziliWhite
    }
    val borde = when {
        equipado -> VerdeDuo
        desbloqueada -> UniversoProfundo
        else -> UniversoProfundo.copy(alpha = 0.42f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(206.dp)
            .graphicsLayer { scaleX = escala; scaleY = escala }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .clip(forma)
                .background(UniversoProfundo.copy(alpha = 0.22f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(fondo)
                .border(if (equipado) 3.dp else 2.dp, borde, forma)
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    enabled = habilitada,
                    role = Role.Button,
                    onClickLabel = when {
                        equipado -> "Quitar ${item.name}"
                        desbloqueada -> "Poner ${item.name}"
                        else -> "Ver ${item.name} en la Tienda de Zazi"
                    },
                    onClick = onClick
                )
                .semantics {
                    contentDescription = when {
                        equipado -> "${item.name}, puesto. Toca para quitarlo."
                        desbloqueada -> "${item.name}, en tu armario. Toca para ponerlo."
                        else -> "${item.name}, bloqueado. Cuesta ${item.price} Zazoles."
                    }
                }
                .padding(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ChildAvatarRenderer(
                    equipamiento = vista,
                    modifier = Modifier.fillMaxSize(),
                    mostrarFondo = false,
                    animar = false,
                    descripcion = "Así se ve ${item.name}"
                )
                if (!desbloqueada) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ZaziliWhite.copy(alpha = 0.92f))
                            .border(1.dp, UniversoProfundo.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(3.dp)
                    ) {
                        KawaiiLockIcon(size = 12.dp, color = UniversoProfundo.copy(alpha = 0.7f))
                    }
                }
                if (equipado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(VerdeDuo)
                            .padding(3.dp)
                    ) {
                        KawaiiCheckIcon(size = 12.dp, color = ZaziliWhite)
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            Text(
                text = item.name.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp,
                color = if (desbloqueada) {
                    UniversoProfundo
                } else {
                    UniversoProfundo.copy(alpha = 0.65f)
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            EtiquetaPixel(
                item = item,
                desbloqueada = desbloqueada,
                equipado = equipado
            )
        }
    }
}

@Composable
private fun EtiquetaPixel(
    item: AvatarItem,
    desbloqueada: Boolean,
    equipado: Boolean
) {
    val forma = RoundedCornerShape(4.dp)
    val color = when {
        equipado -> VerdeDuo
        !desbloqueada && item.price > 0 -> AmarilloVibrante
        !desbloqueada -> AmarilloSuave
        else -> LavandaClara
    }
    val texto = when {
        equipado -> "PUESTO"
        !desbloqueada && item.price > 0 -> "${item.price}"
        !desbloqueada -> "GRATIS"
        else -> "TUYO"
    }
    val colorTexto = if (equipado) ZaziliWhite else UniversoProfundo

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(forma)
            .background(color)
            .border(1.dp, UniversoProfundo.copy(alpha = 0.35f), forma)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        if (equipado) {
            KawaiiCheckIcon(size = 10.dp, color = ZaziliWhite)
            Spacer(Modifier.width(3.dp))
        } else if (!desbloqueada && item.price > 0) {
            KawaiiCoinIcon(size = 11.dp)
            Spacer(Modifier.width(3.dp))
        }
        Text(
            text = texto,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            color = colorTexto,
            maxLines = 1
        )
    }
}
