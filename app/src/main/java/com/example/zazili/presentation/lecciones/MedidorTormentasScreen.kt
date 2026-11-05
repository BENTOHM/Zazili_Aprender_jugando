package com.example.zazili.presentation.lecciones

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.NarradorNativo
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.domain.emociones.EmocionTormenta
import com.example.zazili.domain.emociones.HerramientaAuxilio
import com.example.zazili.domain.emociones.OvaExercise
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Medidor de Tormentas": primeros auxilios psicológicos con arrastre.
 *
 * El alebrije bebé carga una nube cuyo color depende de su emoción (rojo para
 * enojo, azul para tristeza). El niño elige del Kit de Primeros Auxilios una
 * herramienta y la arrastra hasta él. La herramienta correcta suelta chispas
 * suaves; cualquier otra regresa a su lugar sin regaño.
 *
 * Física obligatoria de Zazili: UN [Animatable] de [Offset] por ficha, centros
 * de reposo medidos en coordenadas globales, prueba de intersección contra el
 * rectángulo del alebrije al soltar y regreso con resorte a [Offset.Zero].
 */
@Composable
fun MedidorTormentasScreen(
    exercise: OvaExercise,
    onElegirHerramienta: (HerramientaAuxilio) -> Unit,
    modifier: Modifier = Modifier,
    activo: Boolean = true,
    audioApoyo: Boolean = false
) {
    val zen = collectZenMode()
    val enviarActual by rememberUpdatedState(onElegirHerramienta)
    val herramientas = HerramientaAuxilio.entries

    var enviado by remember(exercise.id) { mutableStateOf(false) }
    var arrastrandoIndice by remember(exercise.id) { mutableIntStateOf(-1) }
    val puedeElegir = activo && !enviado

    val alcance = rememberCoroutineScope()
    val arrastres = remember(exercise.id) {
        List(herramientas.size) { Animatable(Offset.Zero, Offset.VectorConverter) }
    }
    val chispa = remember(exercise.id) { Animatable(0f) }
    val centros = remember(exercise.id) { Array(herramientas.size) { Offset.Zero } }
    var zonaAlebrije by remember(exercise.id) { mutableStateOf(Rect.Zero) }

    val vaiven = rememberInfiniteTransition(label = "vaivenTormenta")
    val flotar by vaiven.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flotarNube"
    )
    val balanceo = if (zen) 0f else flotar

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
        if (!puedeElegir) {
            regresar(indice)
            return
        }
        val herramienta = herramientas[indice]
        val centroSoltado = centros[indice] + arrastres[indice].value
        val dentroDelAlebrije = zonaAlebrije != Rect.Zero &&
            zonaAlebrije.contains(centroSoltado)

        if (dentroDelAlebrije) {
            enviado = true
            if (herramienta == exercise.herramienta) {
                GlobalAudioManager.playSuccess()
                alcance.launch {
                    launch {
                        chispa.snapTo(0f)
                        chispa.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 720,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                    delay(380)
                    enviarActual(herramienta)
                }
            } else {
                GlobalAudioManager.playMiss()
                enviarActual(herramienta)
            }
        } else {
            GlobalAudioManager.playMiss()
        }
        regresar(indice)
    }

    LaunchedEffect(exercise.id, activo, audioApoyo) {
        if (!activo) {
            NarradorNativo.detener()
            return@LaunchedEffect
        }
        if (!audioApoyo) return@LaunchedEffect
        delay(360)
        NarradorNativo.hablar("${exercise.escenario}. ${exercise.observacion}")
    }
    DisposableEffect(Unit) {
        onDispose { NarradorNativo.detener() }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "MEDIDOR DE TORMENTAS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            color = MagicalPurple
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Primeros auxilios para el alebrije",
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
            shadowElevation = if (zen) 0.dp else 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
            ) {
                BionicText(
                    text = exercise.escenario,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniversoProfundo
                )
                Spacer(Modifier.width(8.dp))
                BotonAltavoz(
                    texto = "${exercise.escenario}. ${exercise.observacion}",
                    tamano = 32.dp,
                    colorFondo = LavandaClara
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AzulCielo.copy(alpha = 0.42f),
                            LavandaClara.copy(alpha = 0.78f),
                            SoftCream
                        )
                    )
                )
        ) {
            PaisajeTormenta(modifier = Modifier.fillMaxSize())

            Text(
                text = "NUBE DE ${exercise.emocion.etiqueta.uppercase()}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                color = UniversoProfundo.copy(alpha = 0.58f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 12.dp)
            )

            NubeTormenta(
                emocion = exercise.emocion,
                balanceo = balanceo,
                zen = zen,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 22.dp)
                    .size(160.dp, 140.dp)
                    .semantics {
                        contentDescription =
                            "Nube de ${exercise.emocion.etiqueta} sobre el alebrije"
                    }
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(186.dp, 206.dp)
                    .onGloballyPositioned { zonaAlebrije = it.boundsInRoot() }
                    .semantics {
                        contentDescription =
                            "Alebrije bebé con una nube de ${exercise.emocion.etiqueta}. " +
                                "Suelta aquí la herramienta que lo ayuda."
                    }
            ) {
                AlebrijeBebe(
                    emocion = exercise.emocion,
                    balanceo = balanceo,
                    chispa = chispa.value,
                    zen = zen,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Arrastra la herramienta que ayuda a tu alebrije. También puedes tocarla.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
            shadowElevation = if (zen) 0.dp else 5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "KIT DE PRIMEROS AUXILIOS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp,
                        color = MagicalPurple
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Arrastra o toca",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UniversoProfundo.copy(alpha = 0.52f)
                    )
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    herramientas.forEachIndexed { indice, herramienta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(94.dp)
                                .onGloballyPositioned { coordenadas ->
                                    if (arrastrandoIndice != indice) {
                                        centros[indice] =
                                            coordenadas.boundsInRoot().center
                                    }
                                }
                        ) {
                            FichaAuxilio(
                                herramienta = herramienta,
                                desplazamiento = arrastres[indice].value,
                                arrastrando = arrastrandoIndice == indice,
                                habilitada = puedeElegir,
                                zen = zen,
                                modifier = Modifier.fillMaxSize(),
                                onTap = {
                                    if (!puedeElegir) return@FichaAuxilio
                                    enviado = true
                                    if (herramienta == exercise.herramienta) {
                                        GlobalAudioManager.playSuccess()
                                        alcance.launch {
                                            launch {
                                                chispa.snapTo(0f)
                                                chispa.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(
                                                        durationMillis = 720,
                                                        easing = FastOutSlowInEasing
                                                    )
                                                )
                                            }
                                            delay(380)
                                            enviarActual(herramienta)
                                        }
                                    } else {
                                        GlobalAudioManager.playPop()
                                        GlobalAudioManager.playMiss()
                                        enviarActual(herramienta)
                                    }
                                },
                                onArrastreInicio = {
                                    arrastrandoIndice = indice
                                    GlobalAudioManager.playPop()
                                },
                                onArrastre = { cantidad ->
                                    alcance.launch {
                                        arrastres[indice].snapTo(
                                            arrastres[indice].value + cantidad
                                        )
                                    }
                                },
                                onSoltar = { soltar(indice) },
                                onCancelar = { regresar(indice) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  FICHAS DEL KIT
// ---------------------------------------------------------------------------

@Composable
private fun FichaAuxilio(
    herramienta: HerramientaAuxilio,
    desplazamiento: Offset,
    arrastrando: Boolean,
    habilitada: Boolean,
    zen: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onArrastreInicio: () -> Unit,
    onArrastre: (Offset) -> Unit,
    onSoltar: () -> Unit,
    onCancelar: () -> Unit
) {
    val fondo = colorHerramienta(herramienta)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (zen) MaterialTheme.colorScheme.surface else fondo,
        shadowElevation = if (zen) 0.dp else if (arrastrando) 12.dp else 5.dp,
        modifier = modifier
            .graphicsLayer {
                translationX = desplazamiento.x
                translationY = desplazamiento.y
                val escala = if (arrastrando) 1.07f else 1f
                scaleX = escala
                scaleY = escala
            }
            .clickable(enabled = habilitada, role = Role.Button, onClick = onTap)
            .pointerInput(herramienta, habilitada) {
                if (!habilitada) return@pointerInput
                detectDragGestures(
                    onDragStart = { onArrastreInicio() },
                    onDragEnd = { onSoltar() },
                    onDragCancel = { onCancelar() },
                    onDrag = { cambio, cantidad ->
                        cambio.consume()
                        onArrastre(cantidad)
                    }
                )
            }
            .semantics {
                contentDescription =
                    "${herramienta.etiqueta}. ${herramienta.descripcion}. " +
                        "Arrástrala hasta el alebrije o tócala."
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Text(text = herramienta.emoji, fontSize = 26.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                text = etiquetaCorta(herramienta),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  NUBE, ALEBRIJE Y PAISAJE
// ---------------------------------------------------------------------------

@Composable
private fun NubeTormenta(
    emocion: EmocionTormenta,
    balanceo: Float,
    zen: Boolean,
    modifier: Modifier = Modifier
) {
    val colorNube = colorEmocion(emocion)
    Canvas(modifier = modifier) {
        val ancho = size.width
        val alto = size.height
        val vaiven = if (zen) 0f else balanceo * alto * 0.022f
        val centroY = alto * 0.50f + vaiven
        val radio = ancho * 0.165f

        drawCircle(
            color = colorNube.copy(alpha = 0.18f),
            radius = radio * 2.3f,
            center = Offset(ancho * 0.5f, centroY + radio * 0.5f)
        )
        drawCircle(colorNube, radio, Offset(ancho * 0.31f, centroY))
        drawCircle(colorNube, radio * 1.2f, Offset(ancho * 0.5f, centroY - radio * 0.55f))
        drawCircle(colorNube, radio * 1.02f, Offset(ancho * 0.69f, centroY))
        drawRoundRect(
            color = colorNube,
            topLeft = Offset(ancho * 0.24f, centroY),
            size = Size(ancho * 0.52f, radio * 1.12f),
            cornerRadius = CornerRadius(radio * 0.56f)
        )

        val ojoY = centroY + radio * 0.18f
        val ojoRadio = radio * 0.15f
        drawCircle(UniversoProfundo, ojoRadio, Offset(ancho * 0.41f, ojoY))
        drawCircle(UniversoProfundo, ojoRadio, Offset(ancho * 0.59f, ojoY))

        when (emocion) {
            EmocionTormenta.ENOJO -> {
                drawLine(
                    color = UniversoProfundo,
                    start = Offset(ancho * 0.36f, ojoY - radio * 0.32f),
                    end = Offset(ancho * 0.45f, ojoY - radio * 0.16f),
                    strokeWidth = radio * 0.09f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = UniversoProfundo,
                    start = Offset(ancho * 0.64f, ojoY - radio * 0.32f),
                    end = Offset(ancho * 0.55f, ojoY - radio * 0.16f),
                    strokeWidth = radio * 0.09f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = UniversoProfundo,
                    start = Offset(ancho * 0.45f, centroY + radio * 0.62f),
                    end = Offset(ancho * 0.55f, centroY + radio * 0.62f),
                    strokeWidth = radio * 0.09f,
                    cap = StrokeCap.Round
                )
                val rayo = Path().apply {
                    moveTo(ancho * 0.52f, centroY + radio * 1.05f)
                    lineTo(ancho * 0.45f, centroY + radio * 1.8f)
                    lineTo(ancho * 0.52f, centroY + radio * 1.75f)
                    lineTo(ancho * 0.48f, centroY + radio * 2.45f)
                    lineTo(ancho * 0.61f, centroY + radio * 1.6f)
                    lineTo(ancho * 0.54f, centroY + radio * 1.64f)
                    close()
                }
                drawPath(rayo, AmarilloVibrante)
            }

            EmocionTormenta.TRISTEZA -> {
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.39f, ojoY - radio * 0.14f),
                    size = Size(radio * 0.38f, radio * 0.30f),
                    style = Stroke(width = radio * 0.09f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.57f, ojoY - radio * 0.14f),
                    size = Size(radio * 0.38f, radio * 0.30f),
                    style = Stroke(width = radio * 0.09f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.46f, centroY + radio * 0.46f),
                    size = Size(radio * 0.34f, radio * 0.28f),
                    style = Stroke(width = radio * 0.08f, cap = StrokeCap.Round)
                )
                val lagrima = (balanceo + 1f) / 2f
                drawCircle(
                    color = AzulCielo,
                    radius = radio * 0.10f,
                    center = Offset(
                        ancho * 0.43f,
                        ojoY + radio * (0.32f + 0.42f * lagrima)
                    )
                )
            }

            EmocionTormenta.MIEDO -> {
                drawCircle(
                    color = ZaziliWhite.copy(alpha = 0.72f),
                    radius = ojoRadio * 1.5f,
                    center = Offset(ancho * 0.41f, ojoY)
                )
                drawCircle(
                    color = ZaziliWhite.copy(alpha = 0.72f),
                    radius = ojoRadio * 1.5f,
                    center = Offset(ancho * 0.59f, ojoY)
                )
                drawCircle(UniversoProfundo, ojoRadio * 0.8f, Offset(ancho * 0.41f, ojoY))
                drawCircle(UniversoProfundo, ojoRadio * 0.8f, Offset(ancho * 0.59f, ojoY))
                drawOval(
                    color = UniversoProfundo,
                    topLeft = Offset(ancho * 0.47f, centroY + radio * 0.5f),
                    size = Size(radio * 0.26f, radio * 0.30f)
                )
            }

            EmocionTormenta.ANSIEDAD -> {
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.46f, centroY + radio * 0.36f),
                    size = Size(radio * 0.34f, radio * 0.28f),
                    style = Stroke(width = radio * 0.08f, cap = StrokeCap.Round)
                )
                val giro = (balanceo + 1f) / 2f
                repeat(3) { indice ->
                    val x = ancho * (0.30f + 0.2f * indice)
                    drawArc(
                        color = UniversoProfundo.copy(alpha = 0.55f),
                        startAngle = 0f,
                        sweepAngle = 300f,
                        useCenter = false,
                        topLeft = Offset(
                            x - radio * 0.22f,
                            centroY - radio * (0.9f + 0.3f * giro) - indice * radio * 0.12f
                        ),
                        size = Size(radio * 0.44f, radio * 0.44f),
                        style = Stroke(width = radio * 0.07f, cap = StrokeCap.Round)
                    )
                }
            }

            EmocionTormenta.VERGUENZA -> {
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.45f, centroY + radio * 0.46f),
                    size = Size(radio * 0.34f, radio * 0.26f),
                    style = Stroke(width = radio * 0.08f, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = RosaSuave,
                    radius = radio * 0.14f,
                    center = Offset(ancho * 0.34f, centroY + radio * 0.38f)
                )
                drawCircle(
                    color = RosaSuave,
                    radius = radio * 0.14f,
                    center = Offset(ancho * 0.66f, centroY + radio * 0.38f)
                )
            }

            EmocionTormenta.ALEGRIA -> {
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.46f, centroY + radio * 0.34f),
                    size = Size(radio * 0.34f, radio * 0.30f),
                    style = Stroke(width = radio * 0.08f, cap = StrokeCap.Round)
                )
                dibujarDestellos(
                    centro = Offset(ancho * 0.5f, centroY - radio * 0.1f),
                    radio = radio * 1.9f,
                    tamano = radio * 0.10f,
                    color = AmarilloVibrante
                )
            }

            EmocionTormenta.CALMA -> {
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.39f, ojoY - radio * 0.14f),
                    size = Size(radio * 0.38f, radio * 0.30f),
                    style = Stroke(width = radio * 0.09f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.57f, ojoY - radio * 0.14f),
                    size = Size(radio * 0.38f, radio * 0.30f),
                    style = Stroke(width = radio * 0.09f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = UniversoProfundo,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ancho * 0.46f, centroY + radio * 0.42f),
                    size = Size(radio * 0.34f, radio * 0.22f),
                    style = Stroke(width = radio * 0.07f, cap = StrokeCap.Round)
                )
                dibujarDestellos(
                    centro = Offset(ancho * 0.5f, centroY - radio * 0.1f),
                    radio = radio * 2.0f,
                    tamano = radio * 0.07f,
                    color = ZaziliWhite.copy(alpha = 0.8f)
                )
            }
        }

        if (emocion == EmocionTormenta.TRISTEZA ||
            emocion == EmocionTormenta.ANSIEDAD
        ) {
            val lluvia = if (zen) 0f else ((balanceo + 1f) / 2f) * radio * 0.35f
            repeat(3) { indice ->
                val x = ancho * (0.36f + 0.14f * indice)
                val inicio = centroY + radio * (1.30f + indice * 0.12f) + lluvia
                drawLine(
                    color = AzulCielo.copy(alpha = 0.85f),
                    start = Offset(x, inicio),
                    end = Offset(x, inicio + radio * 0.42f),
                    strokeWidth = radio * 0.08f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun AlebrijeBebe(
    emocion: EmocionTormenta,
    balanceo: Float,
    chispa: Float,
    zen: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val ancho = size.width
        val alto = size.height
        val salto = if (zen) 0f else balanceo * alto * 0.012f
        val centroY = alto * 0.64f + salto
        val cuerpoAncho = ancho * 0.46f
        val cuerpoAlto = alto * 0.32f

        drawOval(
            color = UniversoProfundo.copy(alpha = 0.10f),
            topLeft = Offset(ancho * 0.27f, alto * 0.90f),
            size = Size(ancho * 0.46f, alto * 0.07f)
        )

        val cola = Path().apply {
            moveTo(ancho * 0.71f, centroY)
            quadraticTo(
                ancho * 0.94f,
                centroY - alto * 0.11f,
                ancho * 0.86f,
                centroY - alto * 0.22f
            )
        }
        drawPath(
            path = cola,
            color = MintGreen,
            style = Stroke(width = ancho * 0.05f, cap = StrokeCap.Round)
        )
        drawCircle(
            color = RosaSuave,
            radius = ancho * 0.030f,
            center = Offset(ancho * 0.86f, centroY - alto * 0.22f)
        )

        drawRoundRect(
            color = MintGreen,
            topLeft = Offset(ancho * 0.36f, centroY + cuerpoAlto * 0.42f),
            size = Size(ancho * 0.10f, alto * 0.12f),
            cornerRadius = CornerRadius(ancho * 0.05f)
        )
        drawRoundRect(
            color = MintGreen,
            topLeft = Offset(ancho * 0.54f, centroY + cuerpoAlto * 0.42f),
            size = Size(ancho * 0.10f, alto * 0.12f),
            cornerRadius = CornerRadius(ancho * 0.05f)
        )

        drawOval(
            color = MintGreen,
            topLeft = Offset(ancho * 0.27f, centroY - cuerpoAlto * 0.35f),
            size = Size(cuerpoAncho, cuerpoAlto)
        )
        drawOval(
            color = ZaziliWhite.copy(alpha = 0.55f),
            topLeft = Offset(ancho * 0.34f, centroY - cuerpoAlto * 0.22f),
            size = Size(ancho * 0.16f, cuerpoAlto * 0.34f)
        )
        drawCircle(
            color = MagicalPurple.copy(alpha = 0.30f),
            radius = ancho * 0.030f,
            center = Offset(ancho * 0.61f, centroY + cuerpoAlto * 0.05f)
        )
        drawCircle(
            color = AmarilloVibrante.copy(alpha = 0.55f),
            radius = ancho * 0.024f,
            center = Offset(ancho * 0.38f, centroY + cuerpoAlto * 0.18f)
        )
        drawCircle(
            color = RosaSuave.copy(alpha = 0.6f),
            radius = ancho * 0.022f,
            center = Offset(ancho * 0.66f, centroY + cuerpoAlto * 0.22f)
        )

        val cabezaY = centroY - alto * 0.19f
        val cabezaRadio = ancho * 0.175f
        drawCircle(MintGreen, ancho * 0.068f, Offset(ancho * 0.36f, cabezaY - alto * 0.12f))
        drawCircle(MintGreen, ancho * 0.068f, Offset(ancho * 0.64f, cabezaY - alto * 0.12f))
        drawCircle(MintGreen, cabezaRadio, Offset(ancho * 0.5f, cabezaY))
        drawCircle(AmarilloVibrante, ancho * 0.032f, Offset(ancho * 0.41f, cabezaY - alto * 0.16f))
        drawCircle(RosaSuave, ancho * 0.032f, Offset(ancho * 0.59f, cabezaY - alto * 0.16f))

        drawCircle(ZaziliWhite, ancho * 0.047f, Offset(ancho * 0.45f, cabezaY))
        drawCircle(ZaziliWhite, ancho * 0.047f, Offset(ancho * 0.55f, cabezaY))
        drawCircle(UniversoProfundo, ancho * 0.024f, Offset(ancho * 0.452f, cabezaY + alto * 0.004f))
        drawCircle(UniversoProfundo, ancho * 0.024f, Offset(ancho * 0.552f, cabezaY + alto * 0.004f))
        drawCircle(ZaziliWhite, ancho * 0.008f, Offset(ancho * 0.444f, cabezaY - alto * 0.010f))
        drawCircle(ZaziliWhite, ancho * 0.008f, Offset(ancho * 0.544f, cabezaY - alto * 0.010f))

        drawCircle(RosaSuave.copy(alpha = 0.75f), ancho * 0.030f, Offset(ancho * 0.39f, cabezaY + alto * 0.035f))
        drawCircle(RosaSuave.copy(alpha = 0.75f), ancho * 0.030f, Offset(ancho * 0.61f, cabezaY + alto * 0.035f))

        drawArc(
            color = UniversoProfundo,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(ancho * 0.46f, cabezaY + alto * 0.030f),
            size = Size(ancho * 0.08f, alto * 0.040f),
            style = Stroke(width = ancho * 0.012f, cap = StrokeCap.Round)
        )

        if (emocion == EmocionTormenta.ALEGRIA) {
            drawCircle(
                color = RosaSuave.copy(alpha = 0.5f),
                radius = ancho * 0.035f,
                center = Offset(ancho * 0.31f, centroY - alto * 0.02f)
            )
            drawCircle(
                color = AmarilloVibrante.copy(alpha = 0.45f),
                radius = ancho * 0.030f,
                center = Offset(ancho * 0.70f, centroY - alto * 0.08f)
            )
        }

        if (chispa > 0f) {
            val progreso = chispa.coerceIn(0f, 1f)
            val alfa = (1f - progreso).coerceIn(0f, 1f)
            val centro = Offset(ancho * 0.5f, centroY - alto * 0.10f)
            val radio = ancho * (0.16f + 0.34f * progreso)
            repeat(12) { indice ->
                val angulo = (PI * 2.0 * indice / 12.0).toFloat()
                val punto = Offset(
                    x = centro.x + cos(angulo) * radio,
                    y = centro.y + sin(angulo) * radio * 0.72f
                )
                drawCircle(
                    color = if (indice % 2 == 0) AmarilloVibrante else RosaSuave,
                    radius = ancho * 0.021f * (1f - progreso * 0.35f),
                    center = punto,
                    alpha = alfa
                )
            }
        }
    }
}

@Composable
private fun PaisajeTormenta(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val ancho = size.width
        val alto = size.height
        drawCircle(
            color = MintGreen.copy(alpha = 0.26f),
            radius = ancho * 0.52f,
            center = Offset(ancho * 0.12f, alto * 1.04f)
        )
        drawCircle(
            color = MintGreen.copy(alpha = 0.20f),
            radius = ancho * 0.58f,
            center = Offset(ancho * 0.92f, alto * 1.10f)
        )
        repeat(7) { indice ->
            val x = ancho * (0.12f + 0.13f * indice)
            val y = alto * (0.12f + 0.05f * (indice % 3))
            drawCircle(
                color = ZaziliWhite.copy(alpha = 0.55f),
                radius = ancho * 0.006f,
                center = Offset(x, y)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  UTILIDADES
// ---------------------------------------------------------------------------

private fun DrawScope.dibujarDestellos(
    centro: Offset,
    radio: Float,
    tamano: Float,
    color: Color
) {
    repeat(4) { indice ->
        val angulo = (PI * 2.0 * indice / 4.0 + PI / 4.0).toFloat()
        drawCircle(
            color = color,
            radius = tamano,
            center = Offset(
                centro.x + cos(angulo) * radio,
                centro.y + sin(angulo) * radio * 0.6f
            )
        )
    }
}

private fun colorEmocion(emocion: EmocionTormenta): Color = when (emocion) {
    EmocionTormenta.ENOJO -> Color(0xFFE05A47)
    EmocionTormenta.TRISTEZA -> Color(0xFF5B9BD5)
    EmocionTormenta.MIEDO -> Color(0xFF9B72CF)
    EmocionTormenta.ANSIEDAD -> AmarilloVibrante
    EmocionTormenta.VERGUENZA -> Color(0xFFE88AA3)
    EmocionTormenta.ALEGRIA -> VerdeDuo
    EmocionTormenta.CALMA -> LavandaMedia
}

private fun colorHerramienta(herramienta: HerramientaAuxilio): Color = when (herramienta) {
    HerramientaAuxilio.OIDO -> AzulCielo
    HerramientaAuxilio.BURBUJA -> MintGreen
    HerramientaAuxilio.GLOBO -> AmarilloSuave
    HerramientaAuxilio.ABRAZO -> RosaSuave
}

private fun etiquetaCorta(herramienta: HerramientaAuxilio): String = when (herramienta) {
    HerramientaAuxilio.OIDO -> "Escuchar"
    HerramientaAuxilio.BURBUJA -> "Espacio"
    HerramientaAuxilio.GLOBO -> "Respirar"
    HerramientaAuxilio.ABRAZO -> "Abrazo"
}
