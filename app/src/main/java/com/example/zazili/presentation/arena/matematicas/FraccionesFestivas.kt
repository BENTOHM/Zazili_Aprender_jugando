package com.example.zazili.presentation.arena.matematicas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val PALETA_REBANADAS = listOf(AmarilloVibrante, RosaSuave, CoralAviso)
private const val REBANADAS_POR_FILA = 4

/**
 * Fracciones Festivas — mecánica de arrastre real.
 *
 * [WackyMathContentFactory] elige cada ronda un postre absurdo (churro de 10
 * metros, rosca radiactiva, concha voladora…) y su forma física:
 *  - **ANILLO:** rebanadas de rosca (arcos con grosor, no pastel).
 *  - **PASTEL:** pan circular partido en rebanadas.
 *  - **CHURRO:** barra segmentada de 1/N rebanadas.
 *
 * Algunas rondas giran sus zonas de soltado despacio ([Animatable] con
 * [LinearEasing]): el niño debe apuntar a un blanco en movimiento, pero la
 * colisión sigue siendo el tablero medido con `boundsInRoot()`.
 *
 * El niño arrastra una rebanada de la bandeja hasta el postre. El motor de
 * gestos usa `detectDragGestures` + [Animatable] sobre [Offset]; si la rebanada
 * no mide 1/N, vuelve con un muelle y el DDA sube el apoyo. Nunca hay
 * "Game Over" ni reloj.
 */
@Composable
fun FraccionesFestivasScreen(
    onSalir: () -> Unit,
    onResultado: (gano: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArenaCortexMathViewModel = viewModel()
) {
    val sesion by viewModel.fraccionesFestivas.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    var reportado by rememberSaveable { mutableStateOf(false) }
    var zonaPostre by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(Unit) {
        viewModel.preparar(JuegoMatematico.FRACCIONES_FESTIVAS)
    }

    LaunchedEffect(sesion.terminado) {
        if (sesion.terminado && !reportado) {
            reportado = true
            onResultado(sesion.esVictoria)
        }
    }

    if (sesion.terminado) {
        FinalFracciones(
            sesion = sesion,
            zen = zen,
            onOtraRonda = {
                GlobalAudioManager.playPop()
                viewModel.reiniciar(JuegoMatematico.FRACCIONES_FESTIVAS)
            },
            onSalir = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = modifier
        )
        return
    }

    val reto = sesion.reto as? RetoMath.Fraccion

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CabeceraFracciones(sesion = sesion, onSalir = onSalir)

        Spacer(Modifier.height(8.dp))

        BarraRondaFracciones(sesion = sesion)

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
                        text = "Preparando el postre…",
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
            label = "tableroFracciones"
        ) { actual ->
            val semilla = actual.hashCode() * 31 + sesion.ronda * 131 + sesion.aciertos
            val tema = remember(actual) { WackyMathContentFactory.temaFraccion(semilla) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MesaPostre(
                    reto = actual,
                    tema = tema,
                    sesion = sesion,
                    zen = zen,
                    onBounds = { if (actual == reto) zonaPostre = it }
                )

                Spacer(Modifier.height(10.dp))

                BandejaRebanadas(
                    reto = actual,
                    tema = tema,
                    sesion = sesion,
                    zonaPostre = zonaPostre,
                    onSoltarRebanada = { indice ->
                        val pieza = actual.piezas.getOrNull(indice)
                        if (pieza != null) {
                            if (pieza.denominador == actual.denominador) {
                                GlobalAudioManager.playSuccess()
                            } else {
                                GlobalAudioManager.playMiss()
                            }
                            viewModel.colocarPiezaFraccion(
                                JuegoMatematico.FRACCIONES_FESTIVAS,
                                indice
                            )
                        }
                    },
                    onArrastreInicio = { GlobalAudioManager.playPop() }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        BandaFiesta(
            titulo = sesion.resultado?.titulo,
            detalle = sesion.resultado?.detalle ?: sesion.aviso,
            acierto = sesion.resultado?.acierto
        )

        Spacer(Modifier.height(4.dp))

        if (sesion.andamiaje.pistaSiempreVisible) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AmarilloSuave.copy(alpha = 0.60f),
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
            text = "Arrastra una rebanada hasta el postre. Si no embona, regresa con un brinquito.",
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
private fun CabeceraFracciones(
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
                text = "Fracciones Festivas",
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
private fun BarraRondaFracciones(sesion: SesionMath) {
    val fraccion by animateFloatAsState(
        targetValue = sesion.progreso.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280),
        label = "avanceFracciones"
    )
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🍩 Tu ronda",
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
                    .fillMaxWidth(fraccion)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(RosaSuave)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  POSTRE: ZONA DE SOLTADO
// ---------------------------------------------------------------------------

@Composable
private fun MesaPostre(
    reto: RetoMath.Fraccion,
    tema: TemaFraccion,
    sesion: SesionMath,
    zen: Boolean,
    onBounds: (Rect) -> Unit
) {
    val colocadas = sesion.piezasColocadas.size
    val completas = (reto.porcionesLlenas + colocadas).coerceAtMost(reto.denominador)
    val forma = tema.forma
    val relleno = when (forma) {
        FormaPostre.ANILLO -> RosaSuave
        FormaPostre.PASTEL -> AmarilloVibrante
        FormaPostre.CHURRO -> AmarilloOscuro
    }
    val hueco = MaterialTheme.colorScheme.surfaceVariant
    val borde = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.16f)
    val brillo = sesion.hayApoyo

    // Zonas de soltado que giran despacio: solo cambia el dibujo, el soltado
    // se sigue aceptando en todo el tablero medido con boundsInRoot().
    val giroAnim = remember(reto) { Animatable(0f) }
    val girando = !zen && tema.giro != 0f
    LaunchedEffect(girando, tema.giro) {
        if (!girando) {
            giroAnim.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            val destino = giroAnim.value + tema.giro
            giroAnim.animateTo(
                targetValue = destino,
                animationSpec = tween(durationMillis = 11_000, easing = LinearEasing)
            )
            giroAnim.snapTo((destino % 360f + 360f) % 360f)
        }
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (brillo) 2.5.dp else 1.dp,
            if (brillo) RosaSuave.copy(alpha = 0.90f) else borde
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
                        text = "${(reto.faltan - colocadas).coerceAtLeast(0)} " +
                            "rebanada(s) de 1/${reto.denominador} por colocar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                    )
                }
                BotonAltavoz(
                    texto = "${tema.nombre}. ${reto.consigna} ${reto.pista}",
                    tamano = 30.dp,
                    colorFondo = RosaSuave.copy(alpha = 0.55f),
                    descripcion = "Escuchar la consigna de ${tema.nombre}"
                )
            }

            if (tema.giro != 0f) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LavandaClara.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = "🌀 ${tema.etiquetaMutador}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagicalPurple
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(198.dp)
                    .onGloballyPositioned { coordenadas ->
                        onBounds(coordenadas.boundsInRoot())
                    }
                    .semantics {
                        contentDescription = "${tema.nombre} partido en " +
                            "${reto.denominador} rebanadas. Ya tiene $completas. " +
                            "Suelta aquí las rebanadas que falten."
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centro = Offset(size.width / 2f, size.height / 2f)
                    val barrido = 360f / reto.denominador

                    rotate(degrees = giroAnim.value, pivot = centro) {
                        when (forma) {
                            FormaPostre.ANILLO -> {
                                val externo = size.minDimension * 0.45f
                                val interno = externo * 0.48f
                                val medio = (externo + interno) / 2f
                                val grosor = externo - interno

                                if (brillo) {
                                    drawCircle(
                                        color = RosaSuave.copy(alpha = 0.32f),
                                        radius = externo + 8.dp.toPx(),
                                        center = centro,
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                }

                                repeat(reto.denominador) { indice ->
                                    val llena = indice < completas
                                    drawArc(
                                        color = if (llena) relleno else hueco,
                                        startAngle = -90f + indice * barrido,
                                        sweepAngle = barrido - 2.6f,
                                        useCenter = false,
                                        topLeft = Offset(centro.x - medio, centro.y - medio),
                                        size = Size(medio * 2f, medio * 2f),
                                        style = Stroke(width = grosor, cap = StrokeCap.Butt)
                                    )
                                }

                                // Ate y fruta seca solo en las rebanadas ya servidas.
                                repeat(reto.denominador) { indice ->
                                    if (indice >= completas) return@repeat
                                    val angulo =
                                        (-90.0 + indice * barrido + barrido / 2.0) * PI / 180.0
                                    val x = centro.x + medio * cos(angulo).toFloat()
                                    val y = centro.y + medio * sin(angulo).toFloat()
                                    drawCircle(
                                        color = if (indice % 2 == 0) VerdeDuo else RosaSuave,
                                        radius = 3.4.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }

                                drawCircle(
                                    color = borde,
                                    radius = externo,
                                    center = centro,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                drawCircle(
                                    color = borde,
                                    radius = interno,
                                    center = centro,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            FormaPostre.PASTEL -> {
                                val diametro = size.minDimension * 0.80f
                                val radio = diametro / 2f

                                if (brillo) {
                                    drawCircle(
                                        color = AmarilloVibrante.copy(alpha = 0.30f),
                                        radius = radio + 8.dp.toPx(),
                                        center = centro,
                                        style = Stroke(width = 8.dp.toPx())
                                    )
                                }

                                repeat(reto.denominador) { indice ->
                                    val llena = indice < completas
                                    drawArc(
                                        color = if (llena) relleno else hueco,
                                        startAngle = -90f + indice * barrido,
                                        sweepAngle = barrido - 2.5f,
                                        useCenter = true,
                                        topLeft = Offset(centro.x - radio, centro.y - radio),
                                        size = Size(diametro, diametro)
                                    )
                                }

                                // Azúcar glass en las rebanadas ya servidas.
                                repeat(reto.denominador) { indice ->
                                    if (indice >= completas) return@repeat
                                    val angulo =
                                        (-90.0 + indice * barrido + barrido / 2.0) * PI / 180.0
                                    val x = centro.x + (radio * 0.62f) * cos(angulo).toFloat()
                                    val y = centro.y + (radio * 0.62f) * sin(angulo).toFloat()
                                    drawCircle(
                                        color = ZaziliWhite.copy(alpha = 0.85f),
                                        radius = 3.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }

                                drawCircle(
                                    color = borde,
                                    radius = radio,
                                    center = centro,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )
                            }

                            FormaPostre.CHURRO -> {
                                val anchoBarra = size.width * 0.82f
                                val altoBarra = size.height * 0.22f
                                val izquierda = centro.x - anchoBarra / 2f
                                val arriba = centro.y - altoBarra / 2f
                                val radioBarra = CornerRadius(altoBarra / 2f, altoBarra / 2f)
                                val separacion = 2.5.dp.toPx()
                                val anchoSegmento = anchoBarra / reto.denominador

                                if (brillo) {
                                    drawRoundRect(
                                        color = AmarilloVibrante.copy(alpha = 0.30f),
                                        topLeft = Offset(
                                            izquierda - 5.dp.toPx(),
                                            arriba - 5.dp.toPx()
                                        ),
                                        size = Size(
                                            anchoBarra + 10.dp.toPx(),
                                            altoBarra + 10.dp.toPx()
                                        ),
                                        cornerRadius = radioBarra,
                                        style = Stroke(width = 6.dp.toPx())
                                    )
                                }

                                repeat(reto.denominador) { indice ->
                                    val llena = indice < completas
                                    val xSegmento = izquierda + indice * anchoSegmento
                                    drawRoundRect(
                                        color = if (llena) relleno else hueco,
                                        topLeft = Offset(
                                            xSegmento + separacion / 2f,
                                            arriba
                                        ),
                                        size = Size(
                                            (anchoSegmento - separacion).coerceAtLeast(1f),
                                            altoBarra
                                        ),
                                        cornerRadius = CornerRadius(
                                            altoBarra * 0.30f,
                                            altoBarra * 0.30f
                                        )
                                    )
                                    if (llena) {
                                        // Azúcar y canela del churro recién hecho.
                                        drawCircle(
                                            color = ZaziliWhite.copy(alpha = 0.88f),
                                            radius = 2.4.dp.toPx(),
                                            center = Offset(
                                                xSegmento + anchoSegmento * 0.5f,
                                                arriba + altoBarra * 0.28f
                                            )
                                        )
                                        drawCircle(
                                            color = UniversoProfundo.copy(alpha = 0.26f),
                                            radius = 1.8.dp.toPx(),
                                            center = Offset(
                                                xSegmento + anchoSegmento * 0.72f,
                                                arriba + altoBarra * 0.70f
                                            )
                                        )
                                    }
                                }

                                drawRoundRect(
                                    color = borde,
                                    topLeft = Offset(izquierda, arriba),
                                    size = Size(anchoBarra, altoBarra),
                                    cornerRadius = radioBarra,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                val enChurro = forma == FormaPostre.CHURRO
                Surface(
                    shape = if (enChurro) RoundedCornerShape(50) else CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = if (enChurro) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .size(width = 130.dp, height = 38.dp)
                    } else {
                        Modifier.size(if (forma == FormaPostre.ANILLO) 66.dp else 76.dp)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (enChurro) {
                            Text(
                                text = "$completas/${reto.denominador} rebanadas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$completas/${reto.denominador}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (completas >= reto.denominador) {
                                        "¡completo!"
                                    } else {
                                        "rebanadas"
                                    },
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  BANDEJA DE REBANADAS
// ---------------------------------------------------------------------------

@Composable
private fun BandejaRebanadas(
    reto: RetoMath.Fraccion,
    tema: TemaFraccion,
    sesion: SesionMath,
    zonaPostre: Rect,
    onSoltarRebanada: (Int) -> Unit,
    onArrastreInicio: () -> Unit
) {
    val piezas = reto.piezas
    val forma = tema.forma
    val alcance = rememberCoroutineScope()
    val arrastres = remember(reto) {
        List(piezas.size) { Animatable(Offset.Zero, Offset.VectorConverter) }
    }
    val centros = remember(reto) { mutableStateMapOf<Int, Offset>() }
    var arrastrandoIndice by remember(reto) { mutableIntStateOf(-1) }

    val habilitado = sesion.resultado == null
    val resaltar = sesion.andamiaje.resaltados
    val soltarActual by rememberUpdatedState(onSoltarRebanada)
    val zonaActual by rememberUpdatedState(zonaPostre)

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
        val centro = (centros[indice] ?: Offset.Zero) + arrastres[indice].value
        val dentro = zonaActual != Rect.Zero && zonaActual.contains(centro)
        if (dentro) soltarActual(indice)
        regresar(indice)
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
                    text = "REBANADAS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = MagicalPurple
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (tema.giro != 0f) {
                        "¡Gira despacio! Apunta con calma"
                    } else {
                        "Toca y arrastra al postre"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                )
            }

            Spacer(Modifier.height(8.dp))

            piezas.indices.chunked(REBANADAS_POR_FILA).forEach { fila ->
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
                                .height(92.dp)
                                .onGloballyPositioned { coordenadas ->
                                    if (arrastrandoIndice != indice) {
                                        centros[indice] = coordenadas.boundsInRoot().center
                                    }
                                }
                        ) {
                            RebanadaArrastrable(
                                pieza = pieza,
                                forma = forma,
                                habilitada = habilitado && !colocada,
                                colocada = colocada,
                                desplazamiento = arrastres[indice].value,
                                resaltada = resaltar && habilitado && !colocada &&
                                    pieza.denominador == reto.denominador,
                                onArrastreInicio = {
                                    arrastrandoIndice = indice
                                    onArrastreInicio()
                                },
                                onArrastre = { cantidad ->
                                    alcance.launch {
                                        arrastres[indice].snapTo(
                                            arrastres[indice].value + cantidad
                                        )
                                    }
                                },
                                onSoltar = { soltar(indice) },
                                onCancelar = { regresar(indice) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    repeat(REBANADAS_POR_FILA - fila.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RebanadaArrastrable(
    pieza: PiezaFraccion,
    forma: FormaPostre,
    habilitada: Boolean,
    colocada: Boolean,
    desplazamiento: Offset,
    resaltada: Boolean,
    onArrastreInicio: () -> Unit,
    onArrastre: (Offset) -> Unit,
    onSoltar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = PALETA_REBANADAS[pieza.tono % PALETA_REBANADAS.size]
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
                translationX = desplazamiento.x
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
                contentDescription =
                    "Rebanada de 1/${pieza.denominador}. Arrástrala al postre si completa el entero."
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 6.dp)
        ) {
            Canvas(modifier = Modifier.size(40.dp)) {
                val centro = Offset(size.width / 2f, size.height / 2f)
                val barrido = 360f / pieza.denominador
                when (forma) {
                    FormaPostre.ANILLO -> {
                        val externo = size.minDimension * 0.46f
                        val interno = externo * 0.48f
                        val medio = (externo + interno) / 2f
                        drawArc(
                            color = tinta.copy(alpha = 0.18f),
                            startAngle = -90f,
                            sweepAngle = 360f - 6f,
                            useCenter = false,
                            topLeft = Offset(centro.x - medio, centro.y - medio),
                            size = Size(medio * 2f, medio * 2f),
                            style = Stroke(
                                width = externo - interno,
                                cap = StrokeCap.Butt
                            )
                        )
                        drawArc(
                            color = tinta,
                            startAngle = -90f,
                            sweepAngle = barrido - 3f,
                            useCenter = false,
                            topLeft = Offset(centro.x - medio, centro.y - medio),
                            size = Size(medio * 2f, medio * 2f),
                            style = Stroke(
                                width = externo - interno,
                                cap = StrokeCap.Butt
                            )
                        )
                    }

                    FormaPostre.PASTEL -> {
                        val diametro = size.minDimension * 0.94f
                        val radio = diametro / 2f
                        repeat(pieza.denominador) { indice ->
                            drawArc(
                                color = tinta.copy(alpha = 0.18f),
                                startAngle = -90f + indice * barrido,
                                sweepAngle = barrido - 2f,
                                useCenter = true,
                                topLeft = Offset(centro.x - radio, centro.y - radio),
                                size = Size(diametro, diametro)
                            )
                        }
                        drawArc(
                            color = tinta,
                            startAngle = -90f,
                            sweepAngle = barrido - 2f,
                            useCenter = true,
                            topLeft = Offset(centro.x - radio, centro.y - radio),
                            size = Size(diametro, diametro)
                        )
                    }

                    FormaPostre.CHURRO -> {
                        val ancho = size.width * 0.96f
                        val alto = size.height * 0.34f
                        val izquierda = centro.x - ancho / 2f
                        val arriba = centro.y - alto / 2f
                        val paso = ancho / pieza.denominador
                        repeat(pieza.denominador) { indice ->
                            drawRoundRect(
                                color = if (indice == 0) tinta else tinta.copy(alpha = 0.18f),
                                topLeft = Offset(
                                    izquierda + indice * paso + 1.dp.toPx(),
                                    arriba
                                ),
                                size = Size(
                                    (paso - 2.dp.toPx()).coerceAtLeast(1f),
                                    alto
                                ),
                                cornerRadius = CornerRadius(alto * 0.30f, alto * 0.30f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (colocada) "servida" else "1/${pieza.denominador}",
                fontSize = if (colocada) 10.sp else 13.sp,
                fontWeight = FontWeight.Black,
                color = tinta,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  FEEDBACK Y FINAL
// ---------------------------------------------------------------------------

@Composable
private fun BandaFiesta(
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
private fun FinalFracciones(
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
            descripcion = "Zazi celebra el final de tu ronda de fracciones"
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "¡La mesa quedó servida!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${sesion.aciertos} de ${sesion.totalRondas} postres completos · " +
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
            color = RosaSuave.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (sesion.esVictoria) {
                    "Repartiste cada postre en rebanadas iguales. La próxima fiesta trae pedazos nuevos."
                } else {
                    "Completaste varios postres. En otra ronda te acompaño con rebanadas más fáciles."
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
            containerColor = RosaSuave,
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
