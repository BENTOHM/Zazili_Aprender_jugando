package com.example.zazili.presentation.arena.matematicas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.UniversoProfundo
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val PALETA_FIGURAS = listOf(AzulCielo, RosaSuave, AmarilloSuave, LavandaClara)
private const val FIGURAS_POR_FILA = 3

/**
 * Geometría Ancestral — mecánica de encaje por silueta.
 *
 * [WackyMathContentFactory] elige la ruina de la ronda (pirámide invertida de
 * Marte, máscara de Tláloc mecatrónica, observatorio de obsidiana flotante…) y
 * el viento del Nahual. La ruina se dibuja en un Canvas con huecos (zonas) en
 * coordenadas fraccionarias. El niño arrastra una figura de la bandeja; mientras
 * la mueve, la zona que está debajo se enciende:
 *  - **verde** si la figura encaja por forma,
 *  - **dorado** si todavía no.
 *
 * Con viento, un bucle de [withFrameNanos] empuja la figura en horizontal
 * mientras el dedo presiona. El desvío entra en la zona de soltado, así el niño
 * debe corregir el rumbo; en Modo Zen el viento se detiene.
 *
 * La zona activa y el soltado se calculan con `boundsInRoot()` del Canvas: la
 * colisión jamás depende de la resolución ni de la densidad de la pantalla.
 * Si no encaja, la figura regresa con un muelle y el DDA enciende el andamiaje
 * en silencio. Sin reloj, sin "Game Over".
 */
@Composable
fun GeometriaAncestralScreen(
    onSalir: () -> Unit,
    onResultado: (gano: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArenaCortexMathViewModel = viewModel()
) {
    val sesion by viewModel.geometriaAncestral.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    var reportado by rememberSaveable { mutableStateOf(false) }
    var lienzoRuina by remember { mutableStateOf(Rect.Zero) }
    var zonaActiva by remember { mutableIntStateOf(-1) }
    var formaArrastrada by remember { mutableStateOf<FormaGeometrica?>(null) }

    LaunchedEffect(Unit) {
        viewModel.preparar(JuegoMatematico.GEOMETRIA_ANCESTRAL)
    }

    LaunchedEffect(sesion.ronda, sesion.terminado) {
        zonaActiva = -1
        formaArrastrada = null
    }

    LaunchedEffect(sesion.terminado) {
        if (sesion.terminado && !reportado) {
            reportado = true
            onResultado(sesion.esVictoria)
        }
    }

    if (sesion.terminado) {
        FinalGeometria(
            sesion = sesion,
            zen = zen,
            onOtraRonda = {
                GlobalAudioManager.playPop()
                viewModel.reiniciar(JuegoMatematico.GEOMETRIA_ANCESTRAL)
            },
            onSalir = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = modifier
        )
        return
    }

    val reto = sesion.reto as? RetoMath.Geometria

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CabeceraGeometria(sesion = sesion, onSalir = onSalir)

        Spacer(Modifier.height(8.dp))

        BarraRondaGeometria(sesion = sesion)

        Spacer(Modifier.height(10.dp))

        if (reto == null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Trazando la ruina…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        AnimatedContent(
            targetState = reto,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 240)) +
                    slideInVertically(tween(durationMillis = 240)) { alto -> alto / 10 })
                    .togetherWith(fadeOut(tween(durationMillis = 150)))
            },
            label = "tableroGeometria"
        ) { actual ->
            val semilla = actual.hashCode() * 31 + sesion.ronda * 131 + sesion.aciertos
            val tema = remember(actual) { WackyMathContentFactory.temaGeometria(semilla) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RuinaSilueta(
                    reto = actual,
                    tema = tema,
                    sesion = sesion,
                    zen = zen,
                    zonaActiva = zonaActiva,
                    formaArrastrada = formaArrastrada,
                    onBounds = { if (actual == reto) lienzoRuina = it }
                )

                Spacer(Modifier.height(10.dp))

                BandejaFiguras(
                    reto = actual,
                    tema = tema,
                    sesion = sesion,
                    zen = zen,
                    lienzoRuina = lienzoRuina,
                    onSoltarPieza = { indicePieza, indiceZona ->
                        if (indiceZona != null) {
                            val pieza = actual.piezas.getOrNull(indicePieza)
                            val zona = actual.zonas.getOrNull(indiceZona)
                            if (pieza != null && zona != null) {
                                if (pieza.forma == zona.forma) {
                                    GlobalAudioManager.playSuccess()
                                } else {
                                    GlobalAudioManager.playMiss()
                                }
                                viewModel.colocarEnZona(
                                    JuegoMatematico.GEOMETRIA_ANCESTRAL,
                                    indicePieza,
                                    indiceZona
                                )
                            }
                        }
                    },
                    onZonaBajo = { indiceZona -> zonaActiva = indiceZona },
                    onArrastreInicio = { forma -> formaArrastrada = forma },
                    onArrastreFin = {
                        zonaActiva = -1
                        formaArrastrada = null
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        BandaSilueta(
            titulo = sesion.resultado?.titulo,
            detalle = sesion.resultado?.detalle ?: sesion.aviso,
            acierto = sesion.resultado?.acierto
        )

        Spacer(Modifier.height(4.dp))

        if (sesion.andamiaje.pistaSiempreVisible) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AzulCielo.copy(alpha = 0.48f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pista: ${reto.pista}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    color = UniversoProfundo
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        Text(
            text = "Arrastra una figura sobre la silueta. El hueco se enciende cuando encaja. " +
                "Si el Nahual sopla, corrige el rumbo.",
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
        )
    }
}

// ---------------------------------------------------------------------------
//  CABECERA Y AVANCE
// ---------------------------------------------------------------------------

@Composable
private fun CabeceraGeometria(
    sesion: SesionMath,
    onSalir: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
                text = "Geometría Ancestral",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Nivel ${sesion.nivel} · ${sesion.fase.etiqueta}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSystemInDarkTheme()) MintGreen else MagicalPurple
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "${sesion.rondasContestadas}/${sesion.totalRondas} · ${sesion.aciertos} ★",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BarraRondaGeometria(sesion: SesionMath) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🔺 Tu ronda",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${sesion.puntos} puntos",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AmarilloOscuro
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(sesion.progreso.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(AzulCielo)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  SILUETA Y ZONAS
// ---------------------------------------------------------------------------

@Composable
private fun RuinaSilueta(
    reto: RetoMath.Geometria,
    tema: TemaGeometria,
    sesion: SesionMath,
    zen: Boolean,
    zonaActiva: Int,
    formaArrastrada: FormaGeometrica?,
    onBounds: (Rect) -> Unit
) {
    val contorno = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f)
    val arena = MaterialTheme.colorScheme.surfaceVariant
    val superficie = MaterialTheme.colorScheme.surface
    val escombro = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = superficie,
        border = BorderStroke(
            if (sesion.hayApoyo) 2.5.dp else 1.dp,
            if (sesion.hayApoyo) AzulCielo.copy(alpha = 0.85f) else contorno.copy(alpha = 0.28f)
        ),
        shadowElevation = if (zen) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = tema.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = tema.nombre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${reto.zonas.size - sesion.zonasLlenas.size} huecos por llenar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                    )
                }
                BotonAltavoz(
                    texto = "${tema.nombre}. ${reto.consigna} ${reto.pista}",
                    tamano = 30.dp,
                    colorFondo = AzulCielo.copy(alpha = 0.55f),
                    descripcion = "Escuchar la consigna de ${tema.nombre}"
                )
            }

            if (tema.viento.fuerza != 0f) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LavandaClara.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "🌬️ ${tema.viento.etiqueta}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagicalPurple
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(268.dp)
                    .onGloballyPositioned { coordenadas ->
                        onBounds(coordenadas.boundsInRoot())
                    }
                    .semantics {
                        contentDescription = "Silueta de ${reto.nombreConArticulo} con " +
                            "${reto.zonas.size} huecos para figuras."
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lienzo = Rect(Offset.Zero, size)

                    drawRect(color = arena.copy(alpha = 0.38f))

                    // Sol de obsidiana sobre la ruina.
                    drawCircle(
                        color = AmarilloVibrante.copy(alpha = 0.22f),
                        radius = size.minDimension * 0.09f,
                        center = Offset(size.width * 0.86f, size.height * 0.13f)
                    )

                    // Suelo de tepetate.
                    drawRect(
                        color = escombro,
                        topLeft = Offset(0f, size.height * 0.93f),
                        size = Size(size.width, size.height * 0.07f)
                    )

                    reto.zonas.forEachIndexed { indice, zona ->
                        val rect = rectDeZona(lienzo, zona)
                        if (rect.width <= 0f || rect.height <= 0f) return@forEachIndexed

                        val camino = caminoDe(rect, zona.forma)
                        val llena = indice in sesion.zonasLlenas
                        val hover = !llena && indice == zonaActiva
                        val acierta = hover && formaArrastrada == zona.forma
                        val pendiente = !llena && sesion.resaltar && !hover

                        // Halo: primero el del DDA, después el del dedo.
                        if (pendiente) {
                            drawPath(
                                path = camino,
                                color = AmarilloVibrante.copy(alpha = 0.50f),
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        if (hover) {
                            drawPath(
                                path = camino,
                                color = if (acierta) {
                                    MintGreen.copy(alpha = 0.80f)
                                } else {
                                    AmarilloVibrante.copy(alpha = 0.70f)
                                },
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        drawPath(
                            path = camino,
                            color = when {
                                llena -> colorDeForma(zona.forma).copy(alpha = 0.92f)
                                hover && acierta -> MintGreen.copy(alpha = 0.32f)
                                hover -> AmarilloSuave.copy(alpha = 0.42f)
                                else -> superficie.copy(alpha = 0.60f)
                            }
                        )

                        if (!llena) {
                            // Rayado de silueta: se recorta a la figura.
                            clipPath(camino) {
                                val paso = 13.dp.toPx()
                                var x = -size.height
                                while (x < size.width + size.height) {
                                    drawLine(
                                        color = contorno.copy(alpha = 0.09f),
                                        start = Offset(x, 0f),
                                        end = Offset(x + size.height, size.height),
                                        strokeWidth = 1.4.dp.toPx()
                                    )
                                    x += paso
                                }
                            }
                        }

                        drawPath(
                            path = camino,
                            color = if (llena) contorno else contorno.copy(alpha = 0.70f),
                            style = Stroke(
                                width = if (llena) 2.5.dp.toPx() else 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = if (llena) {
                                    null
                                } else {
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(9.dp.toPx(), 7.dp.toPx())
                                    )
                                }
                            )
                        )

                        if (llena) {
                            val cx = rect.center.x
                            val cy = rect.center.y
                            val r = minOf(rect.width, rect.height) * 0.13f
                            drawLine(
                                color = UniversoProfundo.copy(alpha = 0.55f),
                                start = Offset(cx - r, cy),
                                end = Offset(cx - r * 0.2f, cy + r * 0.7f),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = UniversoProfundo.copy(alpha = 0.55f),
                                start = Offset(cx - r * 0.2f, cy + r * 0.7f),
                                end = Offset(cx + r, cy - r * 0.7f),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  BANDEJA DE FIGURAS
// ---------------------------------------------------------------------------

@Composable
private fun BandejaFiguras(
    reto: RetoMath.Geometria,
    tema: TemaGeometria,
    sesion: SesionMath,
    zen: Boolean,
    lienzoRuina: Rect,
    onSoltarPieza: (Int, Int?) -> Unit,
    onZonaBajo: (Int) -> Unit,
    onArrastreInicio: (FormaGeometrica) -> Unit,
    onArrastreFin: () -> Unit
) {
    val piezas = reto.piezas
    val alcance = rememberCoroutineScope()
    val arrastres = remember(reto) {
        List(piezas.size) { Animatable(Offset.Zero, Offset.VectorConverter) }
    }
    val centros = remember(reto) { mutableStateMapOf<Int, Offset>() }
    var arrastrandoIndice by remember(reto) { mutableIntStateOf(-1) }

    // Viento del Nahual: desplaza el dibujo y la zona de soltado del arrastre
    // mientras el dedo presiona. Se lee dentro de graphicsLayer(), así cada
    // frame solo invalida la capa y no recompone la bandeja.
    val desvio = remember(reto) { mutableFloatStateOf(0f) }
    var vientoArrastrando by remember(reto) { mutableStateOf(false) }
    val vientoFuerza = tema.viento.fuerza

    LaunchedEffect(vientoArrastrando, zen, vientoFuerza) {
        if (!vientoArrastrando || zen || vientoFuerza == 0f) {
            desvio.floatValue = 0f
            return@LaunchedEffect
        }
        var anterior = 0L
        while (true) {
            withFrameNanos { ahora ->
                if (anterior != 0L) {
                    val dt = ((ahora - anterior) / 1_000_000_000f).coerceAtMost(0.05f)
                    desvio.floatValue = (desvio.floatValue + vientoFuerza * 1.6f * dt)
                        .coerceIn(-90f, 90f)
                }
                anterior = ahora
            }
        }
    }

    fun centroVisible(indice: Int): Offset =
        (centros[indice] ?: Offset.Zero) + arrastres[indice].value +
            Offset(desvio.floatValue, 0f)

    val habilitado = sesion.resultado == null
    val resaltar = sesion.resaltar
    val formasPendientes = remember(reto, sesion.zonasLlenas) {
        reto.zonas
            .filterIndexed { indice, _ -> indice !in sesion.zonasLlenas }
            .map { it.forma }
            .toSet()
    }

    val soltarActual by rememberUpdatedState(onSoltarPieza)
    val zonaBajoActual by rememberUpdatedState(onZonaBajo)
    val finActual by rememberUpdatedState(onArrastreFin)
    val inicioActual by rememberUpdatedState(onArrastreInicio)
    val lienzoActual by rememberUpdatedState(lienzoRuina)
    val llenasActual by rememberUpdatedState(sesion.zonasLlenas)

    fun zonaBajo(centro: Offset): Int? {
        val lienzo = lienzoActual
        if (lienzo == Rect.Zero || !lienzo.contains(centro)) return null
        return reto.zonas.indices.firstOrNull { indice ->
            indice !in llenasActual &&
                rectDeZona(lienzo, reto.zonas[indice]).contains(centro)
        }
    }

    fun regresar(indice: Int) {
        alcance.launch {
            arrastres[indice].animateTo(
                targetValue = Offset.Zero,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            if (arrastrandoIndice == indice) arrastrandoIndice = -1
        }
    }

    fun soltar(indice: Int) {
        val centro = centroVisible(indice)
        val indiceZona = zonaBajo(centro)
        vientoArrastrando = false
        finActual()

        if (indiceZona == null) {
            regresar(indice)
            return
        }

        val pieza = reto.piezas.getOrNull(indice)
        val zona = reto.zonas.getOrNull(indiceZona)
        soltarActual(indice, indiceZona)

        val lienzo = lienzoActual
        if (pieza == null || zona == null || pieza.forma != zona.forma ||
            lienzo == Rect.Zero
        ) {
            regresar(indice)
            return
        }

        // Encaje real: la figura viaja al centro del hueco y se queda ahí.
        val destino = rectDeZona(lienzo, zona).center - (centros[indice] ?: Offset.Zero)
        alcance.launch {
            arrastres[indice].animateTo(destino, tween(durationMillis = 200))
            if (arrastrandoIndice == indice) arrastrandoIndice = -1
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "FIGURAS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = MagicalPurple
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (tema.viento.fuerza != 0f) {
                        "El Nahual empuja tus figuras"
                    } else {
                        "Cuenta sus lados"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                )
            }

            Spacer(Modifier.height(8.dp))

            piezas.indices.chunked(FIGURAS_POR_FILA).forEach { fila ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fila.forEach { indice ->
                        val pieza = piezas[indice]
                        val colocada = indice in sesion.piezasColocadas
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(96.dp)
                                .onGloballyPositioned { coordenadas ->
                                    if (arrastrandoIndice != indice) {
                                        centros[indice] = coordenadas.boundsInRoot().center
                                    }
                                }
                        ) {
                            FiguraArrastrable(
                                pieza = pieza,
                                habilitada = habilitado && !colocada,
                                colocada = colocada,
                                desplazamiento = arrastres[indice].value,
                                desvio = { desvio.floatValue },
                                resaltada = resaltar && habilitado && !colocada &&
                                    pieza.forma in formasPendientes,
                                onArrastreInicio = {
                                    arrastrandoIndice = indice
                                    desvio.floatValue = 0f
                                    vientoArrastrando = true
                                    inicioActual(pieza.forma)
                                },
                                onArrastre = { cantidad ->
                                    val nuevo = arrastres[indice].value + cantidad
                                    alcance.launch { arrastres[indice].snapTo(nuevo) }
                                    val centro = (centros[indice] ?: Offset.Zero) + nuevo +
                                        Offset(desvio.floatValue, 0f)
                                    zonaBajoActual(zonaBajo(centro) ?: -1)
                                },
                                onSoltar = { soltar(indice) },
                                onCancelar = {
                                    vientoArrastrando = false
                                    finActual()
                                    regresar(indice)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    repeat(FIGURAS_POR_FILA - fila.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FiguraArrastrable(
    pieza: PiezaGeometria,
    habilitada: Boolean,
    colocada: Boolean,
    desplazamiento: Offset,
    desvio: () -> Float,
    resaltada: Boolean,
    onArrastreInicio: () -> Unit,
    onArrastre: (Offset) -> Unit,
    onSoltar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = PALETA_FIGURAS[pieza.tono % PALETA_FIGURAS.size]
    val fondo = if (colocada) MaterialTheme.colorScheme.surfaceVariant else color
    val tinta = if (colocada) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    } else {
        contenidoDe(color)
    }

    val soltarActual by rememberUpdatedState(onSoltar)
    val cancelarActual by rememberUpdatedState(onCancelar)
    val inicioActual by rememberUpdatedState(onArrastreInicio)
    val arrastreActual by rememberUpdatedState(onArrastre)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = fondo,
        border = if (resaltada) BorderStroke(2.5.dp, AmarilloVibrante) else null,
        shadowElevation = if (colocada) 0.dp else 5.dp,
        modifier = modifier
            .graphicsLayer {
                translationX = desplazamiento.x + desvio()
                translationY = desplazamiento.y
                val activa = desplazamiento != Offset.Zero
                val escala = if (activa) 1.09f else 1f
                scaleX = escala
                scaleY = escala
                alpha = if (colocada) 0.40f else 1f
            }
            .pointerInput(pieza, habilitada) {
                if (!habilitada) return@pointerInput
                detectDragGestures(
                    onDragStart = { inicioActual() },
                    onDragEnd = { soltarActual() },
                    onDragCancel = { cancelarActual() },
                    onDrag = { cambio, cantidad ->
                        cambio.consume()
                        arrastreActual(cantidad)
                    }
                )
            }
            .semantics {
                contentDescription = "Figura: ${pieza.forma.etiqueta}. " +
                    "Arrástrala al hueco que tenga la misma forma."
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 6.dp)
        ) {
            Canvas(modifier = Modifier.size(42.dp)) {
                val marco = Rect(
                    left = 1.dp.toPx(),
                    top = 1.dp.toPx(),
                    right = size.width - 1.dp.toPx(),
                    bottom = size.height - 1.dp.toPx()
                )
                if (marco.width <= 0f || marco.height <= 0f) return@Canvas
                val camino = caminoDe(marco, pieza.forma)
                drawPath(path = camino, color = tinta.copy(alpha = 0.20f))
                drawPath(
                    path = camino,
                    color = tinta,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = pieza.forma.etiqueta,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                color = tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  FEEDBACK Y FINAL
// ---------------------------------------------------------------------------

@Composable
private fun BandaSilueta(
    titulo: String?,
    detalle: String?,
    acierto: Boolean?
) {
    AnimatedVisibility(
        visible = detalle != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val color = when (acierto) {
            true -> MintGreen
            false -> AmarilloSuave
            null -> MaterialTheme.colorScheme.surfaceVariant
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                if (titulo != null) {
                    Text(
                        text = titulo,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo
                    )
                }
                Text(
                    text = detalle.orEmpty(),
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UniversoProfundo.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun FinalGeometria(
    sesion: SesionMath,
    zen: Boolean,
    onOtraRonda: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        KawaiiZazi(
            size = 116.dp,
            animar = !zen,
            descripcion = "Zazi celebra el final de tu ronda de geometría"
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "¡La ruina volvió a brillar!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${sesion.aciertos} de ${sesion.totalRondas} ruinas reconstruidas · " +
                "${sesion.puntos} puntos",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { indice ->
                Text(
                    text = if (indice < sesion.estrellas) "★" else "☆",
                    fontSize = 38.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = if (indice < sesion.estrellas) {
                        AmarilloVibrante
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AzulCielo.copy(alpha = 0.48f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (sesion.esVictoria) {
                    "Reconociste cada figura por sus lados. La próxima ruina trae más piezas."
                } else {
                    "Cada pieza colocada cuenta. En otra ronda verás brillar los huecos que siguen."
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        ZaziButton(
            text = "Otra ronda",
            onClick = onOtraRonda,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            containerColor = AzulCielo,
            contentColor = UniversoProfundo,
            cornerRadius = 20.dp,
            baseHeight = 6.dp,
            fontSize = 19.sp
        )

        Spacer(Modifier.height(12.dp))

        ZaziButton(
            text = "Volver a la Arena",
            onClick = onSalir,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 18.dp,
            baseHeight = 5.dp,
            fontSize = 16.sp
        )
    }
}

// ---------------------------------------------------------------------------
//  GEOMETRÍA PURA
// ---------------------------------------------------------------------------

/**
 * Rectángulo real de una zona a partir del Canvas medido con `boundsInRoot()`.
 * Las zonas viven en fracciones 0..1, así la colisión es idéntica en cualquier
 * pantalla, orientación o densidad.
 */
private fun rectDeZona(lienzo: Rect, zona: ZonaGeometria): Rect = Rect(
    left = lienzo.left + zona.x * lienzo.width,
    top = lienzo.top + zona.y * lienzo.height,
    right = lienzo.left + (zona.x + zona.ancho) * lienzo.width,
    bottom = lienzo.top + (zona.y + zona.alto) * lienzo.height
)

/** Camino de cada figura inscrito en [rect]. */
private fun caminoDe(rect: Rect, forma: FormaGeometrica): Path {
    val path = Path()
    if (rect.width <= 0f || rect.height <= 0f) return path

    when (forma) {
        FormaGeometrica.TRIANGULO -> {
            path.moveTo(rect.center.x, rect.top)
            path.lineTo(rect.right, rect.bottom)
            path.lineTo(rect.left, rect.bottom)
            path.close()
        }

        FormaGeometrica.TRAPECIO -> {
            val sangria = rect.width * 0.22f
            path.moveTo(rect.left + sangria, rect.top)
            path.lineTo(rect.right - sangria, rect.top)
            path.lineTo(rect.right, rect.bottom)
            path.lineTo(rect.left, rect.bottom)
            path.close()
        }

        FormaGeometrica.HEXAGONO -> {
            val radioX = rect.width / 2f
            val radioY = rect.height / 2f
            repeat(6) { lado ->
                val angulo = (-90.0 + lado * 60.0) * PI / 180.0
                val x = rect.center.x + (radioX * cos(angulo)).toFloat()
                val y = rect.center.y + (radioY * sin(angulo)).toFloat()
                if (lado == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }

        FormaGeometrica.ROMBO -> {
            path.moveTo(rect.center.x, rect.top)
            path.lineTo(rect.right, rect.center.y)
            path.lineTo(rect.center.x, rect.bottom)
            path.lineTo(rect.left, rect.center.y)
            path.close()
        }

        FormaGeometrica.CIRCULO -> path.addOval(rect)
    }
    return path
}

private fun colorDeForma(forma: FormaGeometrica) = when (forma) {
    FormaGeometrica.TRIANGULO -> AzulCielo
    FormaGeometrica.TRAPECIO -> RosaSuave
    FormaGeometrica.HEXAGONO -> AmarilloSuave
    FormaGeometrica.ROMBO -> LavandaClara
    FormaGeometrica.CIRCULO -> MintGreen
}
