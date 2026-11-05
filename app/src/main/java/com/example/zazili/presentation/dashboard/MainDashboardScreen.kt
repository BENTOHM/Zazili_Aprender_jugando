package com.example.zazili.presentation.dashboard

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.audio.AudioEngine
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.logros.AchievementEngine
import com.example.zazili.core.logros.EventoLogro
import com.example.zazili.core.logros.ObservadorLogros
import com.example.zazili.core.mascot.ZaziVitals
import com.example.zazili.core.notificaciones.PedirPermisoNotificacionesAlEntrar
import com.example.zazili.core.progress.LevelProgressionEngine
import com.example.zazili.core.racha.EstadoRacha
import com.example.zazili.core.racha.RachaEngine
import com.example.zazili.core.racha.StreakWidgetReceiver
import com.example.zazili.core.racha.StreakBottomSheet
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiBoltIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiGearIcon
import com.example.zazili.core.ui.KawaiiHeartIcon
import com.example.zazili.core.ui.KawaiiNestIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.KawaiiTacoIcon
import com.example.zazili.core.ui.ThemeToggleButton
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.avatar.EquipamientoAvatar
import com.example.zazili.domain.diagnostico.DificultadAdaptativa
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.tienda.CatalogoTienditas3B
import com.example.zazili.domain.tienda.FondoTienda
import com.example.zazili.domain.tienda.RopaTienda
import com.example.zazili.presentation.avatar.ChildAvatarRenderer
import com.example.zazili.presentation.logros.IconoTrofeoPixel
import com.example.zazili.presentation.modulos.comun.IconoModulo
import com.example.zazili.presentation.recompensas.BuzonResumenRecompensas
import com.example.zazili.presentation.recompensas.RewardSummaryDialog
import com.example.zazili.presentation.tienda.ZaziCompanionView
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.NightBackground
import com.example.zazili.ui.theme.NightSurface
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.roundToInt

/** Un mundo elegible en el centro de "Jugar y Aprender". */
private data class MundoAprender(
    val titulo: String,
    val lema: String,
    val modulo: ModuloZazili?,
    val color: Color,
    val esArena: Boolean = false
)

private val MUNDOS_APRENDER = listOf(
    MundoAprender(
        titulo = "Matemáticas",
        lema = "Números y retos",
        modulo = ModuloZazili.TIANGUIS_NUMERICO,
        color = VerdeDuo
    ),
    MundoAprender(
        titulo = "Lengua",
        lema = "Literatura y lectura",
        modulo = ModuloZazili.CUEVA_LEYENDAS,
        color = LavandaMedia
    ),
    MundoAprender(
        titulo = "Ciencias",
        lema = "Naturales y sociales",
        modulo = ModuloZazili.LABORATORIO_CENOTE,
        color = AzulCielo
    ),
    MundoAprender(
        titulo = "Tecnología",
        lema = "Introducción a TI",
        modulo = ModuloZazili.PIRAMIDE_CODIGO,
        color = AmarilloVibrante
    ),
    MundoAprender(
        titulo = "Emociones",
        lema = "El Árbol de los Abrazos",
        modulo = ModuloZazili.ARBOL_ABRAZOS,
        color = RosaSuave
    ),
    MundoAprender(
        titulo = "Arena Cortex",
        lema = "Minijuegos y agilidad mental",
        modulo = null,
        color = MagicalPurple,
        esArena = true
    )
)

/**
 * Tablero principal de Zazili 2.0.
 *
 * Jerarquía para un niño con TDAH:
 *  1. Cabecera mínima: saldo de Zazoles + botón-bolsa de la tienda de Zazi.
 *  2. Héroe: avatar del niño con su botón de Boutique Pixel y Zazi en su
 *     casita con su propio botón de Tienditas 3B. Dos zonas, dos tiendas.
 *  3. Acción central: "¡A Jugar y Aprender!" abre la hoja de los 6 mundos.
 *  4. Avance en una tarjeta delgada; adultos y ajustes al pie.
 *
 * Todo bloque declara altura explícita dentro del `LazyColumn`, así que la
 * mascota, el avatar y el botón principal nunca se recortan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onAbrirTienda: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onAbrirBoutique: () -> Unit,
    onIrAAprender: () -> Unit,
    onIrAArena: () -> Unit,
    onIrANido: () -> Unit,
    onAbrirAjustes: () -> Unit = {},
    darkTheme: Boolean,
    onToggleTema: () -> Unit,
    onAbrirLogros: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
    onAbrirModulo: (ModuloZazili) -> Unit = { onIrAAprender() },
    onAbrirCuentos: () -> Unit = {},
    onAbrirChat: () -> Unit = {}
) {
    val cuidado by viewModel.cuidado.collectAsStateWithLifecycle()
    val perfil by viewModel.perfil.collectAsStateWithLifecycle()
    val bolsa by viewModel.bolsa.collectAsStateWithLifecycle()

    val contexto = LocalContext.current
    val motorNivel = remember { LevelProgressionEngine.obtener(contexto) }
    val nivel by motorNivel.progreso.collectAsStateWithLifecycle()
    val buzonRecompensas = remember { BuzonResumenRecompensas.obtener(contexto) }
    val resumenRecompensa by buzonRecompensas.pendiente.collectAsStateWithLifecycle()
    val motorRacha = remember { RachaEngine.obtener(contexto) }
    val motorLogros = remember { AchievementEngine.obtener(contexto) }
    val racha by motorRacha.estado.collectAsStateWithLifecycle()
    var hojaRachaAbierta by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DificultadAdaptativa.instalar(contexto)
        motorRacha.garantizarBonoInicial()
        motorRacha.programarRecordatorios()
        val widgets = runCatching {
            AppWidgetManager.getInstance(contexto)
                .getAppWidgetIds(ComponentName(contexto, StreakWidgetReceiver::class.java))
        }.getOrDefault(intArrayOf())
        if (widgets.isNotEmpty()) {
            motorLogros.notificar(EventoLogro.WIDGET_AGREGADO, widgets.size)
        }
    }

    val zen = collectZenMode()
    val equipamiento = remember(bolsa.equipamiento) {
        AvatarContentFactory.equipamientoDesde(bolsa.equipamiento)
    }
    val nombre = perfil?.nombre?.trim().orEmpty().ifBlank { "Amiguito" }
    val fondoCasita = remember(cuidado.fondoEquipado) {
        CatalogoTienditas3B.fondoPorId(cuidado.fondoEquipado)
    }
    val ropaPuesta = remember(cuidado.ropaEquipada) {
        cuidado.ropaEquipada.values.mapNotNull(CatalogoTienditas3B::ropaPorId)
    }

    val saldoAnimado by animateIntAsState(
        targetValue = bolsa.zazoles,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "saldoZazolesTablero"
    )

    val latido = rememberInfiniteTransition(label = "latidoJugar")
    val pulso by latido.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsoJugar"
    )
    val escalaCta = if (zen) 1f else 1f + 0.014f * pulso

    var hubAbierto by rememberSaveable { mutableStateOf(false) }
    val hojaMundos = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Cielo degradado: crema→lavanda de día, noche profunda→superficie de
    // noche. El Modo Zen lo aplana para no estimular la vista.
    val fondo: Brush = when {
        zen -> Brush.verticalGradient(
            listOf(zenMutedBackground(darkTheme), zenMutedBackground(darkTheme))
        )

        darkTheme -> Brush.verticalGradient(listOf(NightBackground, NightSurface))
        else -> Brush.verticalGradient(listOf(SoftCream, LavandaClara.copy(alpha = 0.8f)))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            BarraUtilidades(
                nivel = nivel.currentLevel,
                zazoles = bolsa.zazoles,
                saldoAnimado = saldoAnimado,
                zen = zen,
                racha = racha,
                onAbrirRacha = {
                    GlobalAudioManager.playPop()
                    hojaRachaAbierta = true
                },
                onAbrirLogros = {
                    GlobalAudioManager.playPop()
                    onAbrirLogros()
                },
                onAbrirTienda = {
                    GlobalAudioManager.playPop()
                    onAbrirTienda()
                }
            )
        }

        item {
            HeroeZazi(
                vitals = cuidado.vitals,
                fondo = fondoCasita,
                ropa = ropaPuesta,
                equipamiento = equipamiento,
                nombre = nombre,
                zen = zen,
                darkTheme = darkTheme,
                onAbrirPerfil = {
                    GlobalAudioManager.playPop()
                    onAbrirPerfil()
                },
                onAbrirTienda = {
                    GlobalAudioManager.playPop()
                    onAbrirTienda()
                },
                onAbrirBoutique = {
                    GlobalAudioManager.playPop()
                    onAbrirBoutique()
                },
                onTocarZazi = {
                    GlobalAudioManager.playPop()
                    viewModel.acariciar()
                },
                onAlternarSueno = {
                    GlobalAudioManager.playPop()
                    viewModel.alternarSueno()
                },
                onAbrirChat = {
                    AudioEngine.playPop()
                    onAbrirChat()
                }
            )
        }

        item {
            AccionPrincipal(
                escala = escalaCta,
                onClick = {
                    GlobalAudioManager.playPop()
                    hubAbierto = true
                }
            )
        }

        item {
            TarjetaAvance(
                nivel = nivel.currentLevel,
                avance = nivel.avance,
                xpActual = nivel.currentXP,
                xpNecesaria = nivel.xpToNextLevel,
                onClick = {
                    GlobalAudioManager.playPop()
                    onIrAAprender()
                }
            )
        }

        item {
            PieTablero(
                darkTheme = darkTheme,
                onToggleTema = onToggleTema,
                onIrANido = {
                    GlobalAudioManager.playPop()
                    onIrANido()
                },
                onAbrirAjustes = {
                    GlobalAudioManager.playPop()
                    onAbrirAjustes()
                }
            )
        }

        item {
            Text(
                text = "Tu casita vive en este dispositivo. Zazi solo se conecta cuando platican.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
        }
    }

    if (hubAbierto) {
        ModalBottomSheet(
            onDismissRequest = { hubAbierto = false },
            sheetState = hojaMundos,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            HojaMundos(
                onElegir = { mundo ->
                    GlobalAudioManager.playPop()
                    hubAbierto = false
                    // Todo mundo abre su mapa; nunca una lección directa.
                    if (mundo.esArena) onIrAArena() else mundo.modulo?.let(onAbrirModulo)
                },
                onAbrirCuentos = {
                    GlobalAudioManager.playPop()
                    hubAbierto = false
                    onAbrirCuentos()
                }
            )
        }
    }

    StreakBottomSheet(
        visible = hojaRachaAbierta,
        estado = racha,
        saldoZazoles = bolsa.zazoles,
        onComprarProtector = { motorRacha.comprarProtector() },
        onCerrar = { hojaRachaAbierta = false }
    )

    RewardSummaryDialog(
        resumen = resumenRecompensa,
        onContinuar = { buzonRecompensas.consumir() }
    )

    ObservadorLogros()

    // Android 13+: si Zazi no puede mandar cartas, se pide permiso con calma.
    PedirPermisoNotificacionesAlEntrar()
}

/**
 * Puerta estática a la plática con Zazi.
 *
 * Vive dentro de la zona de cuidado, justo debajo de las barras de la
 * mascota: se siente como una acción más de atenderla y ya no flota encima
 * del contenido. Lavanda de bajo contraste para no competir con
 * "¡A Jugar y Aprender!", que sigue siendo la acción dominante.
 */
@Composable
private fun BotonPlaticarConZazi(
    zen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZaziButton(
        text = "Platicar con Zazi",
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .semantics { contentDescription = "Platicar con Zazi" },
        containerColor = if (zen) LavandaMedia else LavandaClara,
        contentColor = UniversoProfundo,
        cornerRadius = 20.dp,
        baseHeight = 5.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ChatBubble,
                contentDescription = null,
                tint = UniversoProfundo,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Platicar con Zazi",
                color = UniversoProfundo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Botón de trofeo del Top Bar: abre la galería de logros. */
@Composable
private fun BotonTrofeoLogros(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AmarilloSuave,
        border = androidx.compose.foundation.BorderStroke(2.dp, AmarilloVibrante),
        modifier = modifier
            .clickable(role = Role.Button, onClickLabel = "Abrir mis logros") { onClick() }
            .semantics { contentDescription = "Abrir la galería de logros" }
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconoTrofeoPixel(size = 24.dp, color = UniversoProfundo)
        }
    }
}

// ---------------------------------------------------------------------------
//  1. CABECERA: SALDO + TIENDA
// ---------------------------------------------------------------------------

@Composable
private fun BarraUtilidades(
    nivel: Int,
    zazoles: Int,
    saldoAnimado: Int,
    zen: Boolean,
    racha: EstadoRacha,
    onAbrirRacha: () -> Unit,
    onAbrirLogros: () -> Unit,
    onAbrirTienda: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mi casita",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                KawaiiStarIcon(size = 11.dp, color = AmarilloVibrante)
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "Nivel $nivel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        BotonTrofeoLogros(
            onClick = onAbrirLogros,
            modifier = Modifier.size(44.dp)
        )

        Spacer(Modifier.width(6.dp))

        BotonFlamaRacha(
            estado = racha,
            onClick = onAbrirRacha,
            modifier = Modifier.heightIn(min = 46.dp)
        )

        Spacer(Modifier.width(6.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (zen) {
                AmarilloSuave.copy(alpha = 0.28f)
            } else {
                AmarilloSuave.copy(alpha = 0.85f)
            },
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Abrir la Tienda de Zazi"
                ) { onAbrirTienda() }
                .semantics {
                    contentDescription =
                        "Tienes $zazoles Zazoles. Toca para abrir la Tienda de Zazi."
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
            ) {
                KawaiiCoinIcon(size = 20.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$saldoAnimado",
                    style = MaterialTheme.typography.titleSmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.width(6.dp))

        ZaziButton(
            text = "Tienda de Zazi",
            onClick = onAbrirTienda,
            modifier = Modifier
                .size(46.dp)
                .semantics { contentDescription = "Abrir la Tienda de Zazi" },
            containerColor = MagicalPurple,
            contentColor = ZaziliWhite,
            cornerRadius = 16.dp,
            baseHeight = 4.dp
        ) {
            IconoTienda(size = 22.dp, color = ZaziliWhite)
        }
    }
}

/** Tienda kawaii: toldo con ondas, cuerpo y puerta. Se reconoce a 22dp. */
@Composable
private fun IconoTienda(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension

        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.10f, s * 0.13f),
            size = Size(s * 0.80f, s * 0.20f),
            cornerRadius = CornerRadius(s * 0.07f)
        )
        listOf(0.26f, 0.50f, 0.74f).forEach { centro ->
            drawCircle(
                color = color,
                radius = s * 0.09f,
                center = Offset(s * centro, s * 0.33f)
            )
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.20f, s * 0.36f),
            size = Size(s * 0.60f, s * 0.52f),
            cornerRadius = CornerRadius(s * 0.05f),
            style = Stroke(width = s * 0.07f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(s * 0.42f, s * 0.58f),
            size = Size(s * 0.16f, s * 0.30f),
            cornerRadius = CornerRadius(s * 0.03f)
        )
    }
}

/** Percha de ropa: gancho, hombros y barra. Distingue la Boutique Pixel. */
@Composable
private fun IconoPercha(size: Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.minDimension
        val trazo = Stroke(width = s * 0.10f)
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
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
//  2. HÉROE: ZAZI + AVATAR + MICRO-BADGES
// ---------------------------------------------------------------------------

@Composable
private fun HeroeZazi(
    vitals: ZaziVitals,
    fondo: FondoTienda?,
    ropa: List<RopaTienda>,
    equipamiento: EquipamientoAvatar,
    nombre: String,
    zen: Boolean,
    darkTheme: Boolean,
    onAbrirPerfil: () -> Unit,
    onAbrirBoutique: () -> Unit,
    onAbrirTienda: () -> Unit,
    onTocarZazi: () -> Unit,
    onAlternarSueno: () -> Unit,
    onAbrirChat: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TU RINCÓN",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = if (darkTheme) LavandaMedia else MagicalPurple
        )

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val formaAvatar = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(formaAvatar)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 3.dp,
                        color = if (darkTheme) LavandaMedia else MagicalPurple,
                        shape = formaAvatar
                    )
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Abrir mi armario"
                    ) { onAbrirPerfil() }
                    .semantics {
                        contentDescription =
                            "Mi avatar de $nombre. Toca para abrir mi armario."
                    },
                contentAlignment = Alignment.Center
            ) {
                ChildAvatarRenderer(
                    equipamiento = equipamiento,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    mostrarFondo = false,
                    animar = !zen,
                    descripcion = "Mi avatar pixel con la ropa puesta"
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¡Hola, $nombre!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Viste tu avatar. Zazi te espera abajo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            ZaziButton(
                text = "Boutique Pixel",
                onClick = onAbrirBoutique,
                modifier = Modifier
                    .size(46.dp)
                    .semantics {
                        contentDescription = "Abrir la Boutique Pixel"
                    },
                containerColor = AmarilloVibrante,
                contentColor = UniversoProfundo,
                cornerRadius = 10.dp,
                baseHeight = 4.dp
            ) {
                IconoPercha(size = 22.dp, color = UniversoProfundo)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "LA CASITA DE ZAZI",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = if (darkTheme) LavandaMedia else MagicalPurple,
                modifier = Modifier.weight(1f)
            )
            ZaziButton(
                text = "Tienditas 3B",
                onClick = onAbrirTienda,
                modifier = Modifier
                    .height(42.dp)
                    .semantics { contentDescription = "Abrir Tienditas 3B" },
                containerColor = LavandaMedia,
                contentColor = UniversoProfundo,
                cornerRadius = 10.dp,
                baseHeight = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp)
                ) {
                    IconoTienda(size = 18.dp, color = UniversoProfundo)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tienditas 3B",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        ZaziCompanionView(
            vitals = vitals,
            fondo = fondo,
            ropa = ropa,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            onTocarZazi = onTocarZazi,
            onAlternarSueno = onAlternarSueno
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BadgeEstado(
                etiqueta = "Pancita",
                valor = vitals.pancita / 100f,
                color = AmarilloVibrante,
                zen = zen,
                onClick = onAbrirTienda,
                modifier = Modifier.weight(1f),
                icono = { KawaiiTacoIcon(size = 15.dp) }
            )
            BadgeEstado(
                etiqueta = "Felicidad",
                valor = vitals.felicidad / 100f,
                color = RosaSuave,
                zen = zen,
                onClick = onTocarZazi,
                modifier = Modifier.weight(1f),
                icono = { KawaiiHeartIcon(size = 15.dp) }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BadgeEstado(
                etiqueta = "Energía",
                valor = vitals.energia / 100f,
                color = AzulCielo,
                zen = zen,
                onClick = onAlternarSueno,
                modifier = Modifier.weight(1f),
                icono = { KawaiiBoltIcon(size = 15.dp) }
            )
            BadgeEstado(
                etiqueta = "Higiene",
                valor = vitals.higiene / 100f,
                color = MintGreen,
                zen = zen,
                onClick = onAbrirTienda,
                modifier = Modifier.weight(1f),
                icono = { KawaiiSparkIcon(size = 15.dp) }
            )
        }

        Spacer(Modifier.height(12.dp))

        BotonPlaticarConZazi(
            zen = zen,
            onClick = onAbrirChat
        )
    }
}

@Composable
private fun BadgeEstado(
    etiqueta: String,
    valor: Float,
    color: Color,
    zen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: @Composable () -> Unit
) {
    val nivel = valor.coerceIn(0f, 1f)
    val progreso by animateFloatAsState(
        targetValue = nivel,
        animationSpec = if (zen) {
            snap()
        } else {
            tween(durationMillis = 650, easing = FastOutSlowInEasing)
        },
        label = "nivel$etiqueta"
    )
    val porcentaje = (nivel * 100f).roundToInt()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.30f),
        modifier = modifier
            .height(56.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = "Cuidar a Zazi. $etiqueta al $porcentaje por ciento"
            ) { onClick() }
            .semantics {
                contentDescription = "$etiqueta de Zazi: $porcentaje por ciento."
            }
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icono()
                Spacer(Modifier.width(5.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$porcentaje%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            BarritaNivel(progreso = progreso, color = color)
        }
    }
}

/** Barrita delgada de 4 dp: pista tenue y relleno pastel animado. */
@Composable
private fun BarritaNivel(progreso: Float, color: Color) {
    val forma = RoundedCornerShape(2.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(forma)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progreso.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(forma)
                .background(color)
        )
    }
}

// ---------------------------------------------------------------------------
//  3. ACCIÓN CENTRAL → HOJA DE LOS 6 MUNDOS
// ---------------------------------------------------------------------------

@Composable
private fun AccionPrincipal(
    escala: Float,
    onClick: () -> Unit
) {
    ZaziButton(
        text = "¡A Jugar y Aprender!",
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            },
        containerColor = VerdeDuo,
        contentColor = ZaziliWhite,
        cornerRadius = 30.dp,
        baseHeight = 10.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ZaziliWhite.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                KawaiiBoltIcon(size = 30.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = "¡A Jugar y Aprender!",
                    color = ZaziliWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Elige uno de los 6 mundos",
                    color = ZaziliWhite.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            KawaiiArrowIcon(size = 24.dp, color = ZaziliWhite)
        }
    }
}

@Composable
private fun HojaMundos(
    onElegir: (MundoAprender) -> Unit,
    onAbrirCuentos: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)
    ) {
        Text(
            text = "¿A dónde quieres ir?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Elige un mundo. Zazi va contigo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MUNDOS_APRENDER.chunked(2).forEach { fila ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fila.forEach { mundo ->
                        TarjetaMundo(
                            mundo = mundo,
                            onClick = { onElegir(mundo) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        ZaziButton(
            text = "Cuentacuentos",
            onClick = onAbrirCuentos,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            containerColor = AmarilloSuave,
            contentColor = UniversoProfundo,
            cornerRadius = 22.dp,
            baseHeight = 6.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(text = "📖", fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Cuentacuentos",
                        color = UniversoProfundo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = "Historias reales de España, México y Perú",
                        color = UniversoProfundo.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaMundo(
    mundo: MundoAprender,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contenido = if (mundo.esArena) ZaziliWhite else UniversoProfundo

    ZaziButton(
        text = mundo.titulo,
        onClick = onClick,
        modifier = modifier.height(96.dp),
        containerColor = mundo.color,
        contentColor = contenido,
        cornerRadius = 24.dp,
        baseHeight = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(contenido.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                if (mundo.esArena) {
                    KawaiiBoltIcon(size = 22.dp)
                } else {
                    mundo.modulo?.let { modulo ->
                        IconoModulo(modulo, 24.dp)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mundo.titulo,
                    color = contenido,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mundo.lema,
                    color = contenido.copy(alpha = 0.74f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  4. AVANCE (DELGADO)
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaAvance(
    nivel: Int,
    avance: Float,
    xpActual: Int,
    xpNecesaria: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "Abrir mi camino de aprendizaje"
            ) { onClick() }
            .semantics {
                contentDescription =
                    "Mi avance: nivel $nivel, $xpActual de $xpNecesaria puntos de " +
                        "experiencia. Toca para abrir mi camino de aprendizaje."
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            KawaiiStarIcon(size = 24.dp, color = AmarilloVibrante)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mi avance",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Nivel $nivel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$xpActual / $xpNecesaria XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { avance.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = VerdeDuo,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
            Spacer(Modifier.width(10.dp))
            KawaiiArrowIcon(
                size = 20.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  5. PIE: ADULTOS Y AJUSTES
// ---------------------------------------------------------------------------

@Composable
private fun PieTablero(
    darkTheme: Boolean,
    onToggleTema: () -> Unit,
    onIrANido: () -> Unit,
    onAbrirAjustes: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ZaziButton(
            text = "El Nido de los adultos",
            onClick = onIrANido,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            containerColor = LavandaClara,
            contentColor = UniversoProfundo,
            cornerRadius = 20.dp,
            baseHeight = 5.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                KawaiiNestIcon(size = 20.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "El Nido de los adultos",
                    color = UniversoProfundo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        ThemeToggleButton(
            darkTheme = darkTheme,
            onToggle = onToggleTema,
            modifier = Modifier.size(42.dp)
        )

        ZaziButton(
            text = "Ajustes",
            onClick = onAbrirAjustes,
            modifier = Modifier
                .size(42.dp)
                .semantics { contentDescription = "Abrir los ajustes" },
            containerColor = LavandaMedia,
            contentColor = UniversoProfundo,
            cornerRadius = 21.dp,
            baseHeight = 4.dp
        ) {
            KawaiiGearIcon(size = 24.dp)
        }
    }
}
