package com.example.zazili.presentation.arena

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TOTAL_RONDAS = 8
private const val ACIERTOS_PARA_GANAR = 5
private const val DISTANCIA_PARA_SOLTAR_DP = 92

/**
 * Tlacuache Poeta: el jefe de Lenguaje convertido en juego de swipe.
 *
 * El niño ve una palabra ancla y una tarjeta con otra palabra. Desliza a la
 * derecha si son sinónimos y a la izquierda si son antónimos. La tarjeta sigue
 * el dedo con [Animatable], regresa con un muelle si no cruzó el umbral y sale
 * volando cuando la decisión es clara.
 *
 * Al terminar, entrega `(gano)` UNA sola vez a la Arena Cortex para que el
 * motor adaptativo mueva la racha del juego. No hay pantalla de derrota: si el
 * resultado es bajo, el tlacuache invita a otra ronda.
 */
@Composable
fun TlacuachePoetaScreen(
    difficultyLevel: Int,
    onSalir: () -> Unit,
    onResultado: (gano: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val nivel = difficultyLevel.coerceIn(
        Minijuego.NIVEL_MINIMO,
        Minijuego.NIVEL_MAXIMO
    )
    val zen = collectZenMode()
    val segundosRonda = (13 - nivel).coerceAtLeast(8)
    val acento = MagicalPurple

    var semilla by rememberSaveable { mutableIntStateOf(Random.nextInt()) }
    val mazo = remember(nivel, semilla) {
        MazoTlacuache.paraNivel(nivel)
            .shuffled(Random(semilla))
            .take(TOTAL_RONDAS)
    }

    var indiceRonda by rememberSaveable { mutableIntStateOf(0) }
    var aciertos by rememberSaveable { mutableIntStateOf(0) }
    var rondasContestadas by rememberSaveable { mutableIntStateOf(0) }
    var terminado by rememberSaveable { mutableStateOf(false) }
    var reporteEnviado by rememberSaveable { mutableStateOf(false) }
    var bloqueado by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<FeedbackTlacuache?>(null) }
    var restante by rememberSaveable { mutableIntStateOf(segundosRonda) }

    val alcance = remember {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    val ambito = rememberCoroutineScope()
    val densidad = LocalDensity.current
    val umbralPx = with(densidad) { DISTANCIA_PARA_SOLTAR_DP.dp.toPx() }
    val salidaPx = with(densidad) { 900.dp.toPx() }
    val reportar by rememberUpdatedState(onResultado)

    val rondaActual = mazo.getOrElse(indiceRonda) { mazo.first() }

    fun evaluar(direccion: Int) {
        val ronda = mazo.getOrNull(indiceRonda) ?: return
        val acerto = when (direccion) {
            1 -> ronda.esSinonimo
            -1 -> !ronda.esSinonimo
            else -> false
        }

        // Máscara anti-duplicado: un giro de pantalla no vuelve a sumar la
        // misma ronda ni rompe el total de aciertos.
        val mascara = 1 shl indiceRonda
        if ((rondasContestadas and mascara) == 0) {
            rondasContestadas = rondasContestadas or mascara
            if (acerto) aciertos += 1
        }

        if (acerto) GlobalAudioManager.playSuccess() else GlobalAudioManager.playMiss()

        feedback = FeedbackTlacuache(
            acierto = acerto,
            titulo = when {
                acerto -> "¡Exacto!"
                direccion == 0 -> "Se acabó el tiempo"
                else -> "Casi, casi"
            },
            detalle = construirDetalle(ronda)
        )
    }

    fun resolver(direccion: Int, animarSalida: Boolean) {
        if (bloqueado || terminado) return
        bloqueado = true
        ambito.launch {
            if (animarSalida && direccion != 0) {
                alcance.animateTo(
                    targetValue = Offset(
                        x = if (direccion > 0) salidaPx else -salidaPx,
                        y = 0f
                    ),
                    animationSpec = tween(durationMillis = 200)
                )
            } else {
                alcance.animateTo(
                    targetValue = Offset.Zero,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            evaluar(direccion)
        }
    }

    fun reiniciarPartida() {
        semilla = Random.nextInt()
        indiceRonda = 0
        aciertos = 0
        rondasContestadas = 0
        terminado = false
        reporteEnviado = false
        bloqueado = false
        feedback = null
        restante = segundosRonda
        ambito.launch { alcance.snapTo(Offset.Zero) }
    }

    // Reloj suave: se detiene mientras hay feedback o se está resolviendo una
    // tarjeta. En Modo Zen no existe el reloj, solo la decisión.
    LaunchedEffect(indiceRonda, terminado, bloqueado, zen, segundosRonda) {
        if (terminado || bloqueado || zen) return@LaunchedEffect
        restante = segundosRonda
        while (restante > 0 && !bloqueado && !terminado) {
            delay(1_000)
            restante = (restante - 1).coerceAtLeast(0)
        }
        if (restante <= 0 && !bloqueado && !terminado) {
            resolver(direccion = 0, animarSalida = false)
        }
    }

    // Deja ver el feedback y luego avanza. La última ronda cierra la sesión.
    LaunchedEffect(feedback) {
        if (feedback == null) return@LaunchedEffect
        delay(1_050)
        if (indiceRonda >= TOTAL_RONDAS - 1) {
            terminado = true
            feedback = null
        } else {
            indiceRonda += 1
            alcance.snapTo(Offset.Zero)
            bloqueado = false
            feedback = null
            restante = segundosRonda
        }
    }

    LaunchedEffect(terminado) {
        if (terminado && !reporteEnviado) {
            reporteEnviado = true
            reportar(aciertos >= ACIERTOS_PARA_GANAR)
        }
    }

    if (terminado) {
        PantallaFinalTlacuache(
            aciertos = aciertos,
            onReiniciar = ::reiniciarPartida,
            onSalir = onSalir,
            modifier = modifier
        )
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CabeceraTlacuache(
            nivel = nivel,
            ronda = indiceRonda,
            aciertos = aciertos,
            onSalir = onSalir
        )

        Spacer(Modifier.height(10.dp))

        BarraTiempoTlacuache(
            restante = restante,
            total = segundosRonda,
            zen = zen
        )

        Spacer(Modifier.height(12.dp))

        PanelAncla(
            ancla = rondaActual.ancla,
            nivel = nivel,
            acento = acento
        )

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp),
            contentAlignment = Alignment.Center
        ) {
            TarjetaSwipe(
                ronda = rondaActual,
                acento = acento,
                desplazamiento = alcance,
                bloqueado = bloqueado,
                umbralPx = umbralPx,
                onSoltar = { direccion ->
                    if (direccion == 0) {
                        ambito.launch {
                            alcance.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    } else {
                        resolver(direccion = direccion, animarSalida = true)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.94f)
            )
        }

        Spacer(Modifier.height(8.dp))

        ZonaFeedback(feedback = feedback)

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ZaziButton(
                text = "← Antónimo",
                onClick = { resolver(direccion = -1, animarSalida = true) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                containerColor = CoralAviso,
                contentColor = contenidoDe(CoralAviso),
                cornerRadius = 18.dp,
                baseHeight = 5.dp,
                enabled = !bloqueado,
                fontSize = 14.sp
            )
            ZaziButton(
                text = "Sinónimo →",
                onClick = { resolver(direccion = 1, animarSalida = true) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                containerColor = VerdeDuo,
                contentColor = contenidoDe(VerdeDuo),
                cornerRadius = 18.dp,
                baseHeight = 5.dp,
                enabled = !bloqueado,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "También puedes usar los botones si arrastrar se te complica.",
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.50f)
        )
    }
}

// ---------------------------------------------------------------------------
//  CABECERA, RELOJ Y ANCLA
// ---------------------------------------------------------------------------

@Composable
private fun CabeceraTlacuache(
    nivel: Int,
    ronda: Int,
    aciertos: Int,
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
                text = "Tlacuache Poeta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Nivel $nivel · ¡Ponte trucha!",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSystemInDarkTheme()) MintGreen else MagicalPurple
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MagicalPurple.copy(alpha = 0.14f)
        ) {
            Text(
                text = "${ronda + 1}/$TOTAL_RONDAS · $aciertos ★",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BarraTiempoTlacuache(
    restante: Int,
    total: Int,
    zen: Boolean
) {
    val fraccion by animateFloatAsState(
        targetValue = if (zen) {
            1f
        } else {
            (restante.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        },
        animationSpec = tween(durationMillis = 280),
        label = "relojTlacuache"
    )
    val colorBarra = when {
        zen -> MintGreen
        restante <= 3 -> CoralAviso
        else -> VerdeDuo
    }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (zen) "🧘 Modo Zen: sin reloj" else "⏱️ $restante segundos",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (zen) "Sin prisa" else "¡Rapidito!",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorBarra
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
                    .background(colorBarra)
            )
        }
    }
}

@Composable
private fun PanelAncla(
    ancla: String,
    nivel: Int,
    acento: Color
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = acento.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, acento.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PALABRA ANCLA",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = acento
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = acento.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "Nivel $nivel",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(6.dp))
                BotonAltavoz(
                    texto = ancla,
                    tamano = 30.dp,
                    colorFondo = acento.copy(alpha = 0.18f),
                    descripcion = "Escuchar la palabra $ancla"
                )
            }

            Spacer(Modifier.height(4.dp))

            BionicText(
                text = ancla,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "¿La tarjeta se parece o se opone?",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  TARJETA ARRASTRABLE
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaSwipe(
    ronda: RondaTlacuache,
    acento: Color,
    desplazamiento: Animatable<Offset, AnimationVector2D>,
    bloqueado: Boolean,
    umbralPx: Float,
    onSoltar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val oscuro = isSystemInDarkTheme()
    val ambito = rememberCoroutineScope()
    val soltarActual by rememberUpdatedState(onSoltar)
    val tinteArriba = acento.copy(alpha = if (oscuro) 0.30f else 0.16f)
    val tinteAbajo = if (oscuro) MaterialTheme.colorScheme.surface else SoftCream

    Surface(
        modifier = modifier
            .graphicsLayer {
                translationX = desplazamiento.value.x
                rotationZ = desplazamiento.value.x / 52f
            }
            .pointerInput(bloqueado, ronda.palabra) {
                if (bloqueado) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { cambio, arrastre ->
                        cambio.consume()
                        ambito.launch {
                            desplazamiento.snapTo(
                                Offset(desplazamiento.value.x + arrastre, 0f)
                            )
                        }
                    },
                    onDragEnd = {
                        val x = desplazamiento.value.x
                        when {
                            x > umbralPx -> soltarActual(1)
                            x < -umbralPx -> soltarActual(-1)
                            else -> soltarActual(0)
                        }
                    },
                    onDragCancel = { soltarActual(0) }
                )
            }
            .semantics {
                contentDescription =
                    "Tarjeta ${ronda.palabra}. Desliza a la derecha si es sinónimo de ${ronda.ancla}, o a la izquierda si es antónimo."
            },
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 2.dp,
            color = acento.copy(alpha = if (oscuro) 0.50f else 0.34f)
        ),
        shadowElevation = if (oscuro) 0.dp else 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(tinteArriba, tinteAbajo)))
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                EtiquetaArrastre(
                    texto = "ANTÓNIMO",
                    color = CoralAviso,
                    modifier = Modifier.graphicsLayer {
                        alpha = ((-desplazamiento.value.x - 26f) / 140f)
                            .coerceIn(0f, 1f)
                    }
                )
                EtiquetaArrastre(
                    texto = "SINÓNIMO",
                    color = VerdeDuo,
                    modifier = Modifier.graphicsLayer {
                        alpha = ((desplazamiento.value.x - 26f) / 140f)
                            .coerceIn(0f, 1f)
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp)
            ) {
                Text(
                    text = "DESLIZA LA TARJETA",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = ronda.palabra,
                    fontSize = 31.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )

                Spacer(Modifier.height(6.dp))

                BotonAltavoz(
                    texto = ronda.palabra,
                    tamano = 32.dp,
                    colorFondo = acento.copy(alpha = 0.16f),
                    descripcion = "Escuchar la palabra ${ronda.palabra}"
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Derecha: sinónimo · Izquierda: antónimo",
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                )
            }
        }
    }
}

@Composable
private fun EtiquetaArrastre(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color,
        modifier = modifier
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            color = contenidoDe(color)
        )
    }
}

// ---------------------------------------------------------------------------
//  FEEDBACK Y FINAL
// ---------------------------------------------------------------------------

@Composable
private fun ZonaFeedback(feedback: FeedbackTlacuache?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(tween(160)) + scaleIn(
                animationSpec = tween(180),
                initialScale = 0.92f
            ),
            exit = fadeOut(tween(110))
        ) {
            val actual = feedback
            if (actual != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (actual.acierto) {
                        VerdeDuo.copy(alpha = 0.16f)
                    } else {
                        CoralAviso.copy(alpha = 0.16f)
                    },
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (actual.acierto) "✅" else "💛",
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = actual.titulo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = actual.detalle,
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PantallaFinalTlacuache(
    aciertos: Int,
    onReiniciar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estrellas = when {
        aciertos >= 7 -> 3
        aciertos >= ACIERTOS_PARA_GANAR -> 2
        else -> 1
    }
    val titulo = when (estrellas) {
        3 -> "¡Ponte trucha, poeta!"
        2 -> "¡Buen ritmo!"
        else -> "Sin prisa, otra vez"
    }
    val mensaje = when (estrellas) {
        3 -> "El tlacuache se quitó el sombrero: dominaste sinónimos y antónimos a toda velocidad."
        2 -> "Casi perfecto. El tlacuache te reta a una ronda más para ganar la tercera estrella."
        else -> "Cada ronda entrena tu oído. El tlacuache te espera cuando quieras intentarlo de nuevo."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Text(text = "🦝", fontSize = 54.sp)

        Spacer(Modifier.height(8.dp))

        Text(
            text = "FIN DE LA RONDA",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.8.sp,
            color = MagicalPurple
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { indice ->
                Text(
                    text = if (indice < estrellas) "★" else "☆",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (indice < estrellas) {
                        AmarilloVibrante
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                    }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "$aciertos de $TOTAL_RONDAS aciertos",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
        )

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MagicalPurple.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, MagicalPurple.copy(alpha = 0.20f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = mensaje,
                modifier = Modifier.padding(14.dp),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Recuerda: derecha = sinónimo · izquierda = antónimo.",
            fontSize = 11.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.56f)
        )

        Spacer(Modifier.height(20.dp))

        ZaziButton(
            text = "Otra ronda",
            onClick = {
                GlobalAudioManager.playPop()
                onReiniciar()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            containerColor = VerdeDuo,
            contentColor = contenidoDe(VerdeDuo),
            cornerRadius = 18.dp,
            baseHeight = 5.dp,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(12.dp))

        ZaziButton(
            text = "Volver a la Arena",
            onClick = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = MagicalPurple,
            contentColor = ZaziliWhite,
            cornerRadius = 18.dp,
            baseHeight = 5.dp,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
//  MODELO Y MAZO
// ---------------------------------------------------------------------------

@Immutable
private data class RondaTlacuache(
    val ancla: String,
    val palabra: String,
    val esSinonimo: Boolean
)

@Immutable
private data class FeedbackTlacuache(
    val acierto: Boolean,
    val titulo: String,
    val detalle: String
)

private fun construirDetalle(ronda: RondaTlacuache): String {
    val relacion = if (ronda.esSinonimo) "sinónimo" else "antónimo"
    return "«${ronda.palabra}» es $relacion de «${ronda.ancla}»."
}

/**
 * Mazos por nivel. El vocabulario sube de palabras cotidianas a términos
 * finos; la mecánica (sinónimo a la derecha, antónimo a la izquierda) no
 * cambia, para que la dificultad viva en el lenguaje y no en las reglas.
 */
private object MazoTlacuache {

    fun paraNivel(nivel: Int): List<RondaTlacuache> = when (nivel.coerceIn(1, 5)) {
        1 -> nivelUno
        2 -> nivelDos
        3 -> nivelTres
        4 -> nivelCuatro
        else -> nivelCinco
    }

    private fun ronda(
        ancla: String,
        palabra: String,
        esSinonimo: Boolean
    ) = RondaTlacuache(ancla = ancla, palabra = palabra, esSinonimo = esSinonimo)

    private val nivelUno: List<RondaTlacuache> = listOf(
        ronda("bonito", "hermoso", true),
        ronda("bonito", "feo", false),
        ronda("grande", "enorme", true),
        ronda("grande", "chiquito", false),
        ronda("rápido", "veloz", true),
        ronda("rápido", "lento", false),
        ronda("feliz", "alegre", true),
        ronda("feliz", "triste", false),
        ronda("frío", "helado", true),
        ronda("frío", "caliente", false),
        ronda("nuevo", "reciente", true),
        ronda("nuevo", "viejo", false)
    )

    private val nivelDos: List<RondaTlacuache> = listOf(
        ronda("contento", "dichoso", true),
        ronda("contento", "amargado", false),
        ronda("valiente", "valeroso", true),
        ronda("valiente", "cobarde", false),
        ronda("difícil", "complicado", true),
        ronda("difícil", "sencillo", false),
        ronda("limpio", "aseado", true),
        ronda("limpio", "sucio", false),
        ronda("amable", "gentil", true),
        ronda("amable", "grosero", false),
        ronda("oscuro", "sombrío", true),
        ronda("oscuro", "claro", false)
    )

    private val nivelTres: List<RondaTlacuache> = listOf(
        ronda("duro", "firme", true),
        ronda("duro", "blando", false),
        ronda("ligero", "liviano", true),
        ronda("ligero", "pesado", false),
        ronda("sabroso", "delicioso", true),
        ronda("sabroso", "insípido", false),
        ronda("ruidoso", "bullicioso", true),
        ronda("ruidoso", "silencioso", false),
        ronda("sereno", "tranquilo", true),
        ronda("sereno", "inquieto", false),
        ronda("escaso", "limitado", true),
        ronda("escaso", "abundante", false)
    )

    private val nivelCuatro: List<RondaTlacuache> = listOf(
        ronda("audaz", "atrevido", true),
        ronda("audaz", "temeroso", false),
        ronda("generoso", "dadivoso", true),
        ronda("generoso", "egoísta", false),
        ronda("perspicaz", "astuto", true),
        ronda("perspicaz", "ingenuo", false),
        ronda("cordial", "afable", true),
        ronda("cordial", "hostil", false),
        ronda("robusto", "vigoroso", true),
        ronda("robusto", "frágil", false),
        ronda("veraz", "sincero", true),
        ronda("veraz", "mentiroso", false)
    )

    private val nivelCinco: List<RondaTlacuache> = listOf(
        ronda("enigmático", "misterioso", true),
        ronda("enigmático", "evidente", false),
        ronda("altruista", "solidario", true),
        ronda("altruista", "egoísta", false),
        ronda("meticuloso", "cuidadoso", true),
        ronda("meticuloso", "descuidado", false),
        ronda("locuaz", "hablador", true),
        ronda("locuaz", "callado", false),
        ronda("efímero", "pasajero", true),
        ronda("efímero", "eterno", false),
        ronda("tenaz", "constante", true),
        ronda("tenaz", "rendido", false)
    )
}
