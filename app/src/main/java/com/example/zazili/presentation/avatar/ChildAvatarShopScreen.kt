package com.example.zazili.presentation.avatar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.logros.AccionesLogro
import com.example.zazili.core.logros.AchievementEngine
import com.example.zazili.core.logros.EventoLogro
import com.example.zazili.core.logros.ObservadorLogros
import com.example.zazili.core.racha.EstadoRacha
import com.example.zazili.core.racha.RachaEngine
import com.example.zazili.core.racha.StreakBottomSheet
import com.example.zazili.core.racha.StreakWidgetReceiver
import com.example.zazili.presentation.dashboard.BotonFlamaRacha
import com.example.zazili.core.ui.CelebrationKonfetti
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiLockIcon
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
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Desde este precio una pieza entra a la pestaña Premium. */
private const val PRECIO_PREMIUM = 200

/**
 * Rareza derivada del escalón de precio: el catálogo no lleva el campo y no
 * se modifica; la tienda lo infiere.
 *
 *  - Común = gris lavanda suave
 *  - Raro = azul pastel
 *  - Épico = morado
 *  - Legendario = dorado con brillo animado
 *
 * Los fondos usan tonos pastel de la paleta de marca para bajar el ruido
 * visual: el color fuerte se reserva para el borde, la insignia y el brillo.
 */
private enum class RarezaPixel(
    val etiqueta: String,
    val emoji: String,
    val color: Color,
    val fondoTarjeta: Color
) {
    COMUN("Común", "⚪", LavandaMedia, LavandaClara.copy(alpha = 0.55f)),
    RARO("Raro", "🔵", AzulCielo, AzulCielo.copy(alpha = 0.26f)),
    EPICO("Épico", "🟣", MagicalPurple, MagicalPurple.copy(alpha = 0.14f)),
    LEGENDARIO("Legendario", "🟡", AmarilloVibrante, AmarilloSuave.copy(alpha = 0.62f));

    companion object {
        fun de(item: AvatarItem): RarezaPixel = when {
            item.price >= 400 -> LEGENDARIO
            item.price >= 200 -> EPICO
            item.price >= 100 -> RARO
            else -> COMUN
        }
    }
}

/** Pestañas de la boutique del niño. No comparten catálogo con Zazi. */
private enum class PestanaBoutique(
    val etiqueta: String,
    val emoji: String,
    val categorias: List<AvatarCategory>
) {
    ROPA("Ropa", "👕", listOf(AvatarCategory.ROPA)),
    PEINADOS(
        "Peinados / Caras",
        "💇",
        listOf(
            AvatarCategory.PELO,
            AvatarCategory.CABEZA,
            AvatarCategory.PIEL,
            AvatarCategory.OJOS,
            AvatarCategory.BOCA
        )
    ),
    ACCESORIOS(
        "Accesorios",
        "🕶️",
        listOf(AvatarCategory.ROSTRO, AvatarCategory.ESPALDA)
    ),
    PREMIUM("Premium", "👑", emptyList())
}

/**
 * Boutique Pixel: tienda exclusiva de la ropa, peinados y accesorios del niño.
 *
 * Es hermana de Tienditas 3B, pero no comparte inventario: aquí solo se vende
 * lo que sale de [AvatarContentFactory] (Kuromi, Charro, Astronauta, Snoopy y
 * todo el catálogo original intacto; no se borró ni una pieza). Ambas gastan
 * los mismos Zazoles.
 *
 * Rediseño "premium pixel art":
 *  - Parrilla masonry de 2 columnas: cada tarjeta crece según su contenido y
 *    lleva sombra de bloque, borde grueso de 3 dp y esquinas suaves.
 *  - Rareza visible en el borde, la insignia y el tinte pastel del fondo;
 *    las legendarias respiran con un brillo dorado lento (se apaga en Zen).
 *  - Rebote elástico al presionar cada tarjeta, sin animaciones estridentes.
 *  - Flama de racha en la barra superior: abre la hoja de racha con
 *    calendario semanal y tienda de Protectores.
 *  - Cada compra reporta al [AchievementEngine]; el [ObservadorLogros]
 *    superpuesto celebra cada logro con confeti y aviso dorado.
 *
 * La vista previa escucha `estado` con `collectAsStateWithLifecycle()` y
 * superpone un equipo optimista, así que el muñeco cambia en el mismo instante
 * en que se confirma la compra, sin esperar a DataStore.
 */
@Composable
fun ChildAvatarShopScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val gestor = remember(contexto) { ZazolEconomyManager.obtener(contexto) }
    val motorRacha = remember(contexto) { RachaEngine.obtener(contexto) }
    val motorLogros = remember(contexto) { AchievementEngine.obtener(contexto) }
    val estado by gestor.estado.collectAsStateWithLifecycle()
    val racha by motorRacha.estado.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    val oscuro = isSystemInDarkTheme()
    val alcance = rememberCoroutineScope()

    var pestanaClave by rememberSaveable { mutableStateOf(PestanaBoutique.ROPA.name) }
    val pestana = PestanaBoutique.entries.firstOrNull { it.name == pestanaClave }
        ?: PestanaBoutique.ROPA

    var rarezaFiltro by rememberSaveable { mutableStateOf<String?>(null) }
    var hojaRachaAbierta by rememberSaveable { mutableStateOf(false) }

    var equipoLocal by remember { mutableStateOf<Map<AvatarCategory, String?>>(emptyMap()) }
    var objetivoId by rememberSaveable { mutableStateOf<String?>(null) }
    val objetivo = remember(objetivoId) { objetivoId?.let(AvatarContentFactory::porId) }

    var comprando by remember { mutableStateOf(false) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }
    var avisoTriste by rememberSaveable { mutableStateOf(false) }
    var konfetti by rememberSaveable { mutableIntStateOf(0) }

    // Bono inicial de protectores, recordatorio diario y detección del widget.
    LaunchedEffect(Unit) {
        motorRacha.garantizarBonoInicial()
        motorRacha.programarRecordatorios()
        runCatching {
            val ids = android.appwidget.AppWidgetManager.getInstance(contexto)
                .getAppWidgetIds(
                    android.content.ComponentName(contexto, StreakWidgetReceiver::class.java)
                )
            if (ids.isNotEmpty()) {
                motorLogros.notificar(EventoLogro.WIDGET_AGREGADO)
            }
        }
    }

    val equipamiento = remember(estado.equipamiento, equipoLocal) {
        val base = estado.equipamiento.toMutableMap()
        equipoLocal.forEach { (ranura, piezaId) ->
            if (piezaId == null) base.remove(ranura) else base[ranura] = piezaId
        }
        AvatarContentFactory.equipamientoDesde(base)
    }

    LaunchedEffect(estado.equipamiento, equipoLocal) {
        if (equipoLocal.isEmpty()) return@LaunchedEffect
        val resueltas = equipoLocal.keys.filter { ranura ->
            estado.equipamiento[ranura] == equipoLocal[ranura]
        }
        if (resueltas.isNotEmpty()) {
            equipoLocal = equipoLocal - resueltas.toSet()
        }
    }

    LaunchedEffect(aviso) {
        val actual = aviso ?: return@LaunchedEffect
        delay(3400)
        if (aviso == actual) aviso = null
    }

    val catalogo = remember(pestana, rarezaFiltro) {
        val base = if (pestana == PestanaBoutique.PREMIUM) {
            AvatarContentFactory.todos.filter { it.price >= PRECIO_PREMIUM }
        } else {
            pestana.categorias.flatMap(AvatarContentFactory::porCategoria)
        }
        base
            .filter { item ->
                rarezaFiltro == null || RarezaPixel.de(item).name == rarezaFiltro
            }
            .sortedWith(
                compareBy<AvatarItem> { item ->
                    AvatarContentFactory.estaDesbloqueado(item, estado.desbloqueados)
                }
                    .thenBy { it.price }
                    .thenBy { it.name }
            )
    }

    val resumen = remember(equipamiento) {
        equipamiento.articulos.joinToString(" · ") { it.name }
    }

    val comprar: (AvatarItem) -> Unit = { item ->
        if (!comprando) {
            objetivoId = null
            comprando = true
            alcance.launch {
                when (val resultado = gestor.unlockItem(item.id, item.price)) {
                    is ResultadoCompra.Exito -> {
                        equipoLocal = equipoLocal + (item.category to item.id)
                        runCatching { gestor.equipar(item.category, item.id) }
                        GlobalAudioManager.playSuccess()
                        konfetti += 1
                        avisoTriste = false
                        aviso = "¡${item.name} ya es tuyo! Se ve increíble."
                        motorLogros.notificar(EventoLogro.COMPRA_AVATAR, item.price)
                        motorLogros.notificar(EventoLogro.GASTO, item.price)
                        motorLogros.notifyAction(AccionesLogro.EQUIPAR_PIEZA)
                    }

                    is ResultadoCompra.SinFondos -> {
                        GlobalAudioManager.playMiss()
                        avisoTriste = true
                        aviso = "Te faltan ${resultado.faltan} Zazoles. ¡A jugar se ha dicho!"
                    }

                    ResultadoCompra.YaDesbloqueado -> {
                        equipoLocal = equipoLocal + (item.category to item.id)
                        runCatching { gestor.equipar(item.category, item.id) }
                        motorLogros.notifyAction(AccionesLogro.EQUIPAR_PIEZA)
                    }

                    ResultadoCompra.NoExiste -> {
                        GlobalAudioManager.playMiss()
                        avisoTriste = true
                        aviso = "Esa pieza ya no está en la tienda."
                    }
                }
                comprando = false
            }
        }
    }

    val alternar: (AvatarItem) -> Unit = { item ->
        if (!comprando) {
            val desbloqueado = AvatarContentFactory.estaDesbloqueado(
                item,
                estado.desbloqueados
            )
            val puesto = equipamiento.de(item.category)?.id == item.id
            when {
                !desbloqueado -> {
                    GlobalAudioManager.playPop()
                    objetivoId = item.id
                }

                puesto -> {
                    GlobalAudioManager.playPop()
                    equipoLocal = equipoLocal + (item.category to null)
                    alcance.launch {
                        runCatching { gestor.desequipar(item.category) }
                            .onFailure {
                                equipoLocal = equipoLocal - item.category
                                GlobalAudioManager.playMiss()
                                avisoTriste = true
                                aviso = "No pudimos quitarte esa pieza. ¿Lo intentamos otra vez?"
                            }
                    }
                }

                else -> {
                    GlobalAudioManager.playPop()
                    equipoLocal = equipoLocal + (item.category to item.id)
                    alcance.launch {
                        runCatching { gestor.equipar(item.category, item.id) }
                            .onSuccess {
                                motorLogros.notifyAction(AccionesLogro.EQUIPAR_PIEZA)
                            }
                            .onFailure {
                                equipoLocal = equipoLocal - item.category
                                GlobalAudioManager.playMiss()
                                avisoTriste = true
                                aviso = "No pudimos ponerte esa pieza. ¿Lo intentamos otra vez?"
                            }
                    }
                }
            }
        }
    }

    val fondo = if (zen) zenMutedBackground(oscuro) else SoftCream

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
            BarraBoutique(
                zazoles = estado.zazoles,
                racha = racha,
                onAbrirRacha = {
                    GlobalAudioManager.playPop()
                    hojaRachaAbierta = true
                },
                onSalir = {
                    GlobalAudioManager.playPop()
                    onSalir()
                }
            )

            AnimatedVisibility(visible = comprando || !estado.listo) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AmarilloVibrante,
                    trackColor = LavandaClara.copy(alpha = 0.55f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                EscaparatePixel(
                    equipamiento = equipamiento,
                    resumen = resumen,
                    zen = zen
                )
            }

            PestanasBoutique(
                seleccionada = pestana,
                habilitadas = !comprando,
                onElegir = { elegida ->
                    GlobalAudioManager.playPop()
                    pestanaClave = elegida.name
                }
            )

            FiltroRareza(
                seleccionada = rarezaFiltro,
                habilitadas = !comprando,
                onElegir = { elegida ->
                    GlobalAudioManager.playPop()
                    rarezaFiltro = elegida
                }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "PASARELA: ${pestana.etiqueta.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = UniversoProfundo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LavandaClara,
                    border = BorderStroke(1.5.dp, LavandaMedia)
                ) {
                    Text(
                        text = "${catalogo.size} PIEZAS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Masonry real: dos columnas que crecen con el contenido de cada
            // tarjeta, sin filas rígidas ni huecos muertos.
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 2.dp,
                    bottom = 18.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(items = catalogo, key = { item -> item.id }) { item ->
                    val desbloqueado = AvatarContentFactory.estaDesbloqueado(
                        item,
                        estado.desbloqueados
                    )
                    val equipado = desbloqueado &&
                        equipamiento.de(item.category)?.id == item.id
                    TarjetaBoutique(
                        item = item,
                        equipamiento = equipamiento,
                        desbloqueado = desbloqueado,
                        equipado = equipado,
                        habilitada = !comprando,
                        zen = zen,
                        onClick = { alternar(item) }
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

        ObservadorLogros()

        AnimatedVisibility(
            visible = aviso != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            enter = slideInVertically { alto -> alto } + fadeIn(),
            exit = slideOutVertically { alto -> alto } + fadeOut()
        ) {
            AvisoBoutique(
                mensaje = aviso.orEmpty(),
                triste = avisoTriste
            )
        }
    }

    StreakBottomSheet(
        visible = hojaRachaAbierta,
        estado = racha,
        saldoZazoles = estado.zazoles,
        onComprarProtector = { motorRacha.comprarProtector() },
        onCerrar = { hojaRachaAbierta = false }
    )

    if (objetivo != null) {
        DialogoCompraPixel(
            item = objetivo,
            saldo = estado.zazoles,
            comprando = comprando,
            onCancelar = { if (!comprando) objetivoId = null },
            onConfirmar = { comprar(objetivo) }
        )
    }
}

// ---------------------------------------------------------------------------
//  BARRA SUPERIOR
// ---------------------------------------------------------------------------

@Composable
private fun BarraBoutique(
    zazoles: Int,
    racha: EstadoRacha,
    onAbrirRacha: () -> Unit,
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
                .semantics { contentDescription = "Volver al tablero" },
            containerColor = ZaziliWhite,
            contentColor = UniversoProfundo,
            cornerRadius = 10.dp,
            baseHeight = 4.dp,
            fontSize = 26.sp
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoPercha(size = 16.dp, color = AmarilloOscuro)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "BOUTIQUE PIXEL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = UniversoProfundo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Solo moda para ti",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = UniversoProfundo.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        BotonFlamaRacha(
            estado = racha,
            onClick = onAbrirRacha
        )

        Spacer(Modifier.width(8.dp))

        MonedaBoutique(zazoles = zazoles)
    }
}

@Composable
private fun MonedaBoutique(zazoles: Int) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AmarilloSuave,
        border = BorderStroke(2.dp, UniversoProfundo)
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

/** Percha kawaii de 3 trazos: gancho, hombros y base. Se reconoce a 16 dp. */
@Composable
private fun IconoPercha(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension
        val trazo = Stroke(width = s * 0.10f)
        drawPath(
            path = Path().apply {
                moveTo(s * 0.50f, s * 0.30f)
                cubicTo(
                    s * 0.50f, s * 0.10f,
                    s * 0.78f, s * 0.12f,
                    s * 0.74f, s * 0.26f
                )
                lineTo(s * 0.50f, s * 0.36f)
                lineTo(s * 0.18f, s * 0.66f)
                lineTo(s * 0.82f, s * 0.66f)
                close()
            },
            color = color,
            style = trazo
        )
    }
}

// ---------------------------------------------------------------------------
//  ESCAPARATE (VISTA PREVIA)
// ---------------------------------------------------------------------------

/**
 * Vitrina del muñeco: marco grueso de bloque con sombra dura, fondo pastel y
 * la etiqueta de rareza del look actual. El retrato respira salvo en Zen.
 */
@Composable
private fun EscaparatePixel(
    equipamiento: EquipamientoAvatar,
    resumen: String,
    zen: Boolean
) {
    val forma = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(238.dp)
    ) {
        // Sombra de bloque: un rectángulo oscuro desplazado, sin difuminado.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 7.dp)
                .clip(forma)
                .background(UniversoProfundo.copy(alpha = 0.92f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF8DE), AmarilloSuave.copy(alpha = 0.65f))
                    )
                )
                .border(3.dp, UniversoProfundo, forma)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 6.dp)
            ) {
                ChildAvatarRenderer(
                    equipamiento = equipamiento,
                    modifier = Modifier.fillMaxSize(),
                    mostrarFondo = true,
                    animar = !zen,
                    descripcion = if (resumen.isBlank()) {
                        "Vista previa de tu avatar pixel, sin accesorios"
                    } else {
                        "Vista previa de tu avatar pixel con $resumen"
                    }
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = UniversoProfundo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = "ESCAPARATE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                    color = ZaziliWhite,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (zen) MagicalPurple else VerdeDuo,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = if (zen) "ZEN" else "64 PX",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = ZaziliWhite,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PESTAÑAS Y FILTRO DE RAREZA
// ---------------------------------------------------------------------------

@Composable
private fun PestanasBoutique(
    seleccionada: PestanaBoutique,
    habilitadas: Boolean,
    onElegir: (PestanaBoutique) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        PestanaBoutique.entries.forEach { pestana ->
            val activa = pestana == seleccionada
            val interaccion = remember { MutableInteractionSource() }
            val presionada by interaccion.collectIsPressedAsState()
            val escala by animateFloatAsState(
                targetValue = if (presionada && habilitadas) 0.94f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "presionPestanaBoutique"
            )
            val forma = RoundedCornerShape(14.dp)

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
                            enabled = habilitadas,
                            role = Role.Button,
                            onClickLabel = "Ver ${pestana.etiqueta}",
                            onClick = { onElegir(pestana) }
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(text = pestana.emoji, fontSize = 13.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = pestana.etiqueta.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = if (activa) ZaziliWhite else UniversoProfundo,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Chips de rareza: gris, azul, morado, dorado. "Todas" limpia el filtro. */
@Composable
private fun FiltroRareza(
    seleccionada: String?,
    habilitadas: Boolean,
    onElegir: (String?) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(
            text = "RAREZA",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = UniversoProfundo.copy(alpha = 0.55f)
        )
        ChipRareza(
            etiqueta = "Todas",
            color = UniversoProfundo,
            activo = seleccionada == null,
            habilitado = habilitadas
        ) { onElegir(null) }
        RarezaPixel.entries.forEach { rareza ->
            ChipRareza(
                etiqueta = "${rareza.emoji} ${rareza.etiqueta}",
                color = rareza.color,
                activo = seleccionada == rareza.name,
                habilitado = habilitadas
            ) { onElegir(rareza.name) }
        }
    }
}

@Composable
private fun ChipRareza(
    etiqueta: String,
    color: Color,
    activo: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (activo) color else ZaziliWhite,
        border = BorderStroke(
            if (activo) 2.dp else 1.5.dp,
            if (activo) UniversoProfundo else color
        ),
        modifier = Modifier.clickable(
            enabled = habilitado,
            role = Role.Button,
            onClickLabel = "Filtrar por $etiqueta"
        ) { onClick() }
    ) {
        Text(
            text = etiqueta,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (activo) UniversoProfundo else UniversoProfundo.copy(alpha = 0.78f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

// ---------------------------------------------------------------------------
//  TARJETAS DE LA BOUTIQUE
// ---------------------------------------------------------------------------

/**
 * Tarjeta premium con borde grueso, sombra de bloque y rebote elástico.
 *
 * La altura varía de forma determinista según el nombre de la pieza ([altura])
 * para que la parrilla se sienta masonry y no una tabla rígida. Las legendarias
 * suman un brillo dorado lento en el borde; el Modo Zen lo deja fijo.
 */
@Composable
private fun TarjetaBoutique(
    item: AvatarItem,
    equipamiento: EquipamientoAvatar,
    desbloqueado: Boolean,
    equipado: Boolean,
    habilitada: Boolean,
    zen: Boolean,
    onClick: () -> Unit
) {
    val vista = remember(equipamiento, item) { equipamiento.con(item) }
    val rareza = RarezaPixel.de(item)
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada && habilitada) 0.955f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "presionTarjetaBoutique"
    )

    val transicion = rememberInfiniteTransition(label = "brilloLegendario")
    val pulso by transicion.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsoBordeLegendario"
    )
    val brilloLegendario = rareza == RarezaPixel.LEGENDARIO && !zen

    val forma = RoundedCornerShape(18.dp)
    val borde = when {
        equipado -> VerdeDuo
        desbloqueado -> MagicalPurple
        brilloLegendario -> AmarilloVibrante.copy(alpha = pulso)
        else -> rareza.color
    }
    val fondo = when {
        equipado -> MintGreen.copy(alpha = 0.42f)
        desbloqueado -> LavandaClara.copy(alpha = 0.80f)
        else -> rareza.fondoTarjeta
    }
    val altura = 204.dp + ((item.name.length % 3) * 12).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
    ) {
        // Sombra dura de bloque: el "pop" 3D del pixel art.
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 6.dp)
                .clip(forma)
                .background(UniversoProfundo.copy(alpha = 0.86f))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .clip(forma)
                .background(fondo)
                .border(if (equipado) 3.5.dp else 2.5.dp, borde, forma)
                .clickable(
                    interactionSource = interaccion,
                    indication = null,
                    enabled = habilitada,
                    role = Role.Button,
                    onClickLabel = when {
                        equipado -> "Quitar ${item.name}"
                        desbloqueado -> "Poner ${item.name}"
                        else -> "Comprar ${item.name}"
                    },
                    onClick = onClick
                )
                .semantics {
                    contentDescription = when {
                        equipado -> "${item.name}, pieza ${rareza.etiqueta.lowercase()}, puesta. Toca para quitarlo."
                        desbloqueado -> "${item.name}, pieza ${rareza.etiqueta.lowercase()}, tuya. Toca para ponerlo."
                        else -> "${item.name}, pieza ${rareza.etiqueta.lowercase()}, en venta por ${item.price} Zazoles."
                    }
                }
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ZaziliWhite.copy(alpha = 0.90f),
                                rareza.fondoTarjeta.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = UniversoProfundo.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                ChildAvatarRenderer(
                    equipamiento = vista,
                    modifier = Modifier.fillMaxSize(),
                    mostrarFondo = false,
                    animar = false,
                    descripcion = "Así se ve ${item.name}"
                )

                if (!desbloqueado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZaziliWhite.copy(alpha = 0.94f))
                            .border(
                                1.5.dp,
                                UniversoProfundo.copy(alpha = 0.35f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                    ) {
                        KawaiiLockIcon(
                            size = 13.dp,
                            color = UniversoProfundo.copy(alpha = 0.72f)
                        )
                    }
                }

                if (equipado) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VerdeDuo)
                            .padding(4.dp)
                    ) {
                        KawaiiCheckIcon(size = 13.dp, color = ZaziliWhite)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = rareza.color,
                    border = BorderStroke(1.5.dp, UniversoProfundo.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                ) {
                    Text(
                        text = rareza.etiqueta.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = UniversoProfundo,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = item.name.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                color = UniversoProfundo,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            EtiquetaBoutique(
                item = item,
                desbloqueado = desbloqueado,
                equipado = equipado
            )

            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun EtiquetaBoutique(
    item: AvatarItem,
    desbloqueado: Boolean,
    equipado: Boolean
) {
    val colorFondo = when {
        equipado -> VerdeDuo
        desbloqueado -> MagicalPurple
        item.price == 0 -> MintGreen
        else -> AmarilloVibrante
    }
    val colorTexto = if (equipado) ZaziliWhite else UniversoProfundo

    Surface(
        shape = RoundedCornerShape(50),
        color = colorFondo,
        border = BorderStroke(1.5.dp, UniversoProfundo)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            when {
                equipado -> Text(
                    text = "PUESTO",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = colorTexto
                )

                desbloqueado -> Text(
                    text = "TUYO",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = colorTexto
                )

                item.price == 0 -> Text(
                    text = "GRATIS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = colorTexto
                )

                else -> {
                    KawaiiCoinIcon(size = 12.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${item.price}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = colorTexto
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  DIÁLOGO DE COMPRA
// ---------------------------------------------------------------------------

/**
 * Confirmación de compra de la Boutique Pixel.
 *
 * Usa `usePlatformDefaultWidth = false` y un Surface de ancho máximo fijo: la
 * fila "Cancelar" / "Comprar" vive fuera del área desplazable, así que nunca
 * se recorta aunque el niño tenga la letra del sistema al máximo. El borde del
 * diálogo adopta el color de rareza de la pieza.
 */
@Composable
private fun DialogoCompraPixel(
    item: AvatarItem,
    saldo: Int,
    comprando: Boolean,
    onCancelar: () -> Unit,
    onConfirmar: () -> Unit
) {
    val rareza = RarezaPixel.de(item)
    val alcanza = saldo >= item.price
    val faltan = (item.price - saldo).coerceAtLeast(0)

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !comprando,
            dismissOnClickOutside = !comprando
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.widthIn(max = 420.dp)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 7.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(UniversoProfundo)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = ZaziliWhite,
                    tonalElevation = 0.dp,
                    border = BorderStroke(3.dp, rareza.color)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "¿COMPRAR? ${rareza.emoji}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = UniversoProfundo
                            )
                            Spacer(Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    ZaziliWhite,
                                                    rareza.fondoTarjeta
                                                )
                                            )
                                        )
                                        .border(
                                            2.5.dp,
                                            rareza.color,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(3.dp)
                                ) {
                                    ChildAvatarRenderer(
                                        equipamiento = AvatarContentFactory
                                            .equipamientoPredeterminado()
                                            .con(item),
                                        modifier = Modifier.fillMaxSize(),
                                        mostrarFondo = false,
                                        animar = false,
                                        descripcion = "Vista de ${item.name}"
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = UniversoProfundo,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = rareza.etiqueta.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp,
                                        color = rareza.color
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = item.descripcion.ifBlank {
                                            "Una pieza pixel para tu armario."
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = UniversoMedio,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AmarilloSuave.copy(alpha = 0.55f))
                                    .border(
                                        1.5.dp,
                                        AmarilloVibrante.copy(alpha = 0.85f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "PRECIO",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = UniversoMedio
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        KawaiiCoinIcon(size = 16.dp)
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${item.price} Zazoles",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AmarilloOscuro
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "TIENES",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = UniversoMedio
                                    )
                                    Text(
                                        text = "$saldo Zazoles",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (alcanza) UniversoProfundo else CoralAviso
                                    )
                                }
                            }

                            if (!alcanza) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CoralAviso.copy(alpha = 0.13f))
                                        .border(
                                            1.dp,
                                            CoralAviso.copy(alpha = 0.45f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = "Te faltan $faltan Zazoles. " +
                                            "Completa una actividad para ganarlos.",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CoralAviso
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ZaziButton(
                                text = "Cancelar",
                                onClick = onCancelar,
                                enabled = !comprando,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                containerColor = LavandaClara,
                                contentColor = UniversoProfundo,
                                cornerRadius = 16.dp,
                                baseHeight = 5.dp,
                                fontSize = 15.sp
                            )
                            ZaziButton(
                                text = "Comprar",
                                onClick = onConfirmar,
                                enabled = alcanza && !comprando,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                                containerColor = VerdeDuo,
                                contentColor = ZaziliWhite,
                                cornerRadius = 16.dp,
                                baseHeight = 5.dp,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  AVISO FLOTANTE
// ---------------------------------------------------------------------------

@Composable
private fun AvisoBoutique(
    mensaje: String,
    triste: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (triste) CoralAviso.copy(alpha = 0.96f) else MintGreen,
        border = BorderStroke(2.dp, UniversoProfundo),
        shadowElevation = 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (triste) {
                KawaiiLockIcon(size = 18.dp, color = UniversoProfundo)
            } else {
                KawaiiCheckIcon(size = 18.dp, color = UniversoProfundo)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = mensaje,
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
