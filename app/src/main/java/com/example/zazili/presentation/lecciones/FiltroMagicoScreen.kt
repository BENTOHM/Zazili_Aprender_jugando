package com.example.zazili.presentation.lecciones

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.ciencias.CapaFiltro
import com.example.zazili.domain.ciencias.ORDEN_FILTRO
import com.example.zazili.domain.ciencias.ResultadoFiltro
import com.example.zazili.domain.ciencias.ScienceSandboxEngine
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AzulCielo
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
 * "El Filtro Mágico": prototipo jugable del arenero científico.
 *
 * Zazi sostiene un embudo de cristal. Abajo esperan cuatro capas: algodón,
 * arena, grava y carbón. El niño las arrastra al embudo en el orden que crea
 * correcto; si se equivoca puede quitar la última. Cuando están las cuatro
 * aparece el botón "Filtrar Agua": una animación de Canvas vierte el agua sucia
 * y el vaso de abajo revela si salió clara o turbia.
 *
 * Física de arrastre de Zazili: UN [Animatable] de [Offset] por ficha, centros
 * medidos en coordenadas globales, prueba de intersección contra el embudo al
 * soltar y regreso con resorte a [Offset.Zero]. El motor de dominio decide si
 * el orden es correcto; la pantalla solo lo dibuja.
 */
@Composable
fun FiltroMagicoScreen(
    exercise: ScienceExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activo: Boolean = true
) {
    val zen = collectZenMode()
    val enviarActual by rememberUpdatedState(onSubmitAnswer)

    val capasCorrectas = remember(exercise.id) {
        exercise.capasCorrectas.ifEmpty { ORDEN_FILTRO }
    }
    val bandeja = remember(exercise.id) {
        listOf(
            CapaFiltro.ALGODON,
            CapaFiltro.ARENA,
            CapaFiltro.GRAVA,
            CapaFiltro.CARBON
        )
    }

    val motor = remember(exercise.id) { ScienceSandboxEngine() }
    val alcance = rememberCoroutineScope()

    var capas by remember(exercise.id) { mutableStateOf(emptyList<CapaFiltro>()) }
    var resultado by remember(exercise.id) { mutableStateOf<ResultadoFiltro?>(null) }
    var vertiendo by remember(exercise.id) { mutableStateOf(false) }
    var enviado by remember(exercise.id) { mutableStateOf(false) }
    var arrastrandoIndice by remember(exercise.id) { mutableIntStateOf(-1) }
    var zonaEmbudo by remember(exercise.id) { mutableStateOf(Rect.Zero) }

    val vertido = remember(exercise.id) { Animatable(0f) }
    val chispas = remember(exercise.id) { Animatable(0f) }
    val arrastres = remember(exercise.id) {
        List(bandeja.size) { Animatable(Offset.Zero, Offset.VectorConverter) }
    }
    val centros = remember(exercise.id) { Array(bandeja.size) { Offset.Zero } }

    val puedeTocar = activo && !enviado && !vertiendo
    val completo = capas.size >= capasCorrectas.size
    val mostrarBoton = completo && !vertiendo && !enviado

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

    fun agregar(capa: CapaFiltro) {
        if (!puedeTocar || capa in capas || capas.size >= capasCorrectas.size) return
        capas = capas + capa
        GlobalAudioManager.playPop()
    }

    fun soltar(indice: Int) {
        val capa = bandeja[indice]
        if (!puedeTocar || capa in capas) {
            regresar(indice)
            return
        }
        val centroSoltado = centros[indice] + arrastres[indice].value
        val dentroDelEmbudo = zonaEmbudo != Rect.Zero && zonaEmbudo.contains(centroSoltado)
        if (dentroDelEmbudo) {
            agregar(capa)
        } else {
            GlobalAudioManager.playMiss()
        }
        regresar(indice)
    }

    fun quitarUltima() {
        if (!puedeTocar || capas.isEmpty()) return
        capas = capas.dropLast(1)
        GlobalAudioManager.playPop()
    }

    fun filtrar() {
        if (!puedeTocar || capas.size < capasCorrectas.size) return
        vertiendo = true
        enviado = true
        alcance.launch {
            vertido.snapTo(0f)
            vertido.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1700,
                    easing = FastOutSlowInEasing
                )
            )
            val evaluacion = motor.validarFiltro(capas)
            resultado = evaluacion
            if (evaluacion.correcto) {
                GlobalAudioManager.playSuccess()
                launch {
                    chispas.snapTo(0f)
                    chispas.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 780,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
                delay(360)
                if (exercise.indiceCorrecto in exercise.opciones.indices) {
                    enviarActual(exercise.indiceCorrecto, true)
                }
            } else {
                GlobalAudioManager.playMiss()
                delay(300)
                val totalOpciones = exercise.opciones.size
                val indiceErrado = if (totalOpciones > 0) {
                    (exercise.indiceCorrecto + 1) % totalOpciones
                } else {
                    0
                }
                enviarActual(indiceErrado, false)
            }
            vertiendo = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "FILTRO MÁGICO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            color = MagicalPurple
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "El agua del cenote",
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
                    text = exercise.enunciado,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniversoProfundo
                )
                Spacer(Modifier.width(8.dp))
                BotonAltavoz(
                    texto = "${exercise.enunciado}. ${exercise.pista}",
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
                            AzulCielo.copy(alpha = 0.46f),
                            LavandaClara.copy(alpha = 0.72f),
                            SoftCream
                        )
                    )
                )
        ) {
            Text(
                text = "🪣 AGUA SUCIA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = UniversoProfundo.copy(alpha = 0.58f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 12.dp)
            )

            KawaiiZazi(
                size = 54.dp,
                descripcion = "Zazi te acompaña con el filtro del cenote",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 8.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 252.dp, height = 250.dp)
                    .onGloballyPositioned { zonaEmbudo = it.boundsInRoot() }
                    .semantics {
                        contentDescription =
                            "Embudo del filtro con ${capas.size} de ${capasCorrectas.size} capas. " +
                                "Suelta aquí las capas."
                    }
            ) {
                EmbudoCanvas(
                    resultado = resultado,
                    vertiendo = vertiendo || vertido.value > 0f,
                    vertido = vertido.value,
                    chispas = chispas.value,
                    zen = zen,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp)
                        .width(154.dp)
                ) {
                    if (capas.isEmpty()) {
                        Text(
                            text = "EMBUDO VACÍO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = UniversoProfundo.copy(alpha = 0.46f)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Arrastra una capa aquí",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = UniversoProfundo.copy(alpha = 0.54f),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        capas.forEachIndexed { indice, capa ->
                            FilaCapaApilada(
                                capa = capa,
                                numero = indice + 1,
                                zen = zen
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }

            val textoEstado = when {
                resultado != null -> resultado?.titulo.orEmpty()
                vertiendo -> "El agua está pasando por las capas…"
                capas.isEmpty() -> "El embudo está vacío. Arrastra las capas de abajo."
                capas.size < capasCorrectas.size ->
                    "Faltan ${capasCorrectas.size - capas.size} capas para probar el filtro."
                else -> "¡Todo listo! Toca Filtrar Agua."
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (zen) {
                    MaterialTheme.colorScheme.surface
                } else {
                    ZaziliWhite.copy(alpha = 0.78f)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = textoEstado,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (resultado?.correcto == true) {
                        VerdeDuo
                    } else {
                        UniversoProfundo.copy(alpha = 0.76f)
                    },
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = mostrarBoton,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                ZaziButton(
                    text = "Filtrar Agua",
                    onClick = {
                        GlobalAudioManager.playPop()
                        filtrar()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    containerColor = VerdeDuo,
                    contentColor = contenidoDe(VerdeDuo),
                    cornerRadius = 20.dp,
                    baseHeight = 5.dp,
                    fontSize = 17.sp
                )
            }
        }

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
                        text = "CAPAS DEL FILTRO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp,
                        color = MagicalPurple
                    )
                    Spacer(Modifier.weight(1f))
                    if (capas.isNotEmpty() && puedeTocar) {
                        Text(
                            text = "Quitar la última",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MagicalPurple,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(role = Role.Button) { quitarUltima() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .semantics {
                                    contentDescription =
                                        "Quitar la última capa del embudo"
                                }
                        )
                    } else {
                        Text(
                            text = "Arrastra o toca",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = UniversoProfundo.copy(alpha = 0.52f)
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bandeja.forEachIndexed { indice, capa ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .onGloballyPositioned { coordenadas ->
                                    if (arrastrandoIndice != indice) {
                                        centros[indice] = coordenadas.boundsInRoot().center
                                    }
                                }
                        ) {
                            FichaCapa(
                                capa = capa,
                                desplazamiento = arrastres[indice].value,
                                arrastrando = arrastrandoIndice == indice,
                                habilitada = puedeTocar && capa !in capas,
                                zen = zen,
                                modifier = Modifier.fillMaxSize(),
                                onTap = { agregar(capa) },
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
//  PIEZAS VISUALES
// ---------------------------------------------------------------------------

@Composable
private fun FilaCapaApilada(
    capa: CapaFiltro,
    numero: Int,
    zen: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (zen) MaterialTheme.colorScheme.surface else colorCapa(capa),
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 5.dp)
        ) {
            Text(text = capa.emoji, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = capa.etiqueta,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textoCapa(capa)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$numero",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = textoCapa(capa).copy(alpha = 0.58f)
            )
        }
    }
}

@Composable
private fun FichaCapa(
    capa: CapaFiltro,
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
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (zen) MaterialTheme.colorScheme.surface else colorCapa(capa),
        shadowElevation = when {
            zen || !habilitada -> 0.dp
            arrastrando -> 12.dp
            else -> 5.dp
        },
        modifier = modifier
            .graphicsLayer {
                translationX = desplazamiento.x
                translationY = desplazamiento.y
                val escala = if (arrastrando) 1.07f else 1f
                scaleX = escala
                scaleY = escala
                alpha = if (habilitada) 1f else 0.34f
            }
            .clickable(
                enabled = habilitada,
                role = Role.Button,
                onClick = onTap
            )
            .pointerInput(capa, habilitada) {
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
                    "${capa.etiqueta}. ${capa.descripcion}. " +
                        "Arrástrala al embudo o tócala."
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Text(text = capa.emoji, fontSize = 25.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                text = capa.etiqueta,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textoCapa(capa),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "Arrastra",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = textoCapa(capa).copy(alpha = 0.62f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmbudoCanvas(
    resultado: ResultadoFiltro?,
    vertiendo: Boolean,
    vertido: Float,
    chispas: Float,
    zen: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val ancho = size.width
        val alto = size.height
        val bocaAncho = ancho * 0.80f
        val bocaX = (ancho - bocaAncho) / 2f
        val embudoTop = alto * 0.055f
        val embudoAlto = alto * 0.60f
        val cuelloAncho = ancho * 0.24f
        val cuelloX = (ancho - cuelloAncho) / 2f
        val trazo = 3.dp.toPx()

        val embudo = Path().apply {
            moveTo(bocaX, embudoTop)
            lineTo(bocaX + bocaAncho, embudoTop)
            lineTo(cuelloX + cuelloAncho, embudoTop + embudoAlto)
            lineTo(cuelloX, embudoTop + embudoAlto)
            close()
        }
        drawPath(embudo, AzulCielo.copy(alpha = 0.30f))
        drawPath(
            path = embudo,
            color = UniversoProfundo.copy(alpha = 0.28f),
            style = Stroke(width = trazo)
        )
        drawLine(
            color = ZaziliWhite.copy(alpha = 0.72f),
            start = Offset(bocaX + ancho * 0.06f, embudoTop + alto * 0.045f),
            end = Offset(cuelloX + cuelloAncho * 0.34f, embudoTop + embudoAlto - alto * 0.035f),
            strokeWidth = trazo,
            cap = StrokeCap.Round
        )

        val surtidorY = embudoTop + embudoAlto
        drawRoundRect(
            color = AzulCielo.copy(alpha = 0.72f),
            topLeft = Offset(cuelloX - ancho * 0.018f, surtidorY),
            size = Size(cuelloAncho + ancho * 0.036f, alto * 0.034f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = UniversoProfundo.copy(alpha = 0.24f),
            topLeft = Offset(cuelloX - ancho * 0.018f, surtidorY),
            size = Size(cuelloAncho + ancho * 0.036f, alto * 0.034f),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.4.dp.toPx())
        )

        val vasoAncho = ancho * 0.36f
        val vasoX = (ancho - vasoAncho) / 2f
        val vasoTop = alto * 0.80f
        val vasoAlto = alto * 0.16f
        val colorAgua = when {
            resultado?.aguaLimpia == true -> AzulCielo
            resultado != null -> Color(0xFF8B5E3C)
            else -> Color(0xFF9C6B43)
        }

        if (vertiendo || vertido > 0f) {
            val inicioY = surtidorY + alto * 0.034f
            val finY = vasoTop + alto * 0.02f
            val x = ancho / 2f
            val avance = vertido.coerceIn(0f, 1f)
            val gotaY = inicioY + (finY - inicioY) * avance

            drawLine(
                color = if (resultado?.aguaLimpia == true) {
                    AzulCielo
                } else {
                    Color(0xFF8B5E3C)
                },
                start = Offset(x, inicioY),
                end = Offset(x, gotaY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            repeat(5) { indice ->
                val fase = (avance + indice * 0.21f) % 1f
                val y = inicioY + (finY - inicioY) * fase
                drawCircle(
                    color = if (resultado?.aguaLimpia == true) {
                        AzulCielo.copy(alpha = 0.9f)
                    } else {
                        Color(0xFF8B5E3C).copy(alpha = 0.9f)
                    },
                    radius = 3.dp.toPx(),
                    center = Offset(
                        x = x + (indice - 2f) * 7.dp.toPx(),
                        y = y
                    )
                )
            }
        }

        drawRoundRect(
            color = ZaziliWhite.copy(alpha = 0.58f),
            topLeft = Offset(vasoX, vasoTop),
            size = Size(vasoAncho, vasoAlto),
            cornerRadius = CornerRadius(11.dp.toPx())
        )

        val alturaAgua = vasoAlto * vertido.coerceIn(0f, 1f) * 0.82f
        if (alturaAgua > 0f) {
            drawRoundRect(
                color = colorAgua.copy(alpha = 0.92f),
                topLeft = Offset(
                    vasoX + 2.dp.toPx(),
                    vasoTop + vasoAlto - alturaAgua - 2.dp.toPx()
                ),
                size = Size(vasoAncho - 4.dp.toPx(), alturaAgua),
                cornerRadius = CornerRadius(9.dp.toPx())
            )
        }

        drawRoundRect(
            color = UniversoProfundo.copy(alpha = 0.22f),
            topLeft = Offset(vasoX, vasoTop),
            size = Size(vasoAncho, vasoAlto),
            cornerRadius = CornerRadius(11.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        if (resultado != null) {
            val limpia = resultado.aguaLimpia
            drawCircle(
                color = if (limpia) VerdeDuo else Color(0xFFC0563F),
                radius = 9.dp.toPx(),
                center = Offset(vasoX + vasoAncho - 3.dp.toPx(), vasoTop + 3.dp.toPx())
            )
            drawLine(
                color = ZaziliWhite,
                start = Offset(
                    vasoX + vasoAncho - 7.dp.toPx(),
                    vasoTop + 3.dp.toPx()
                ),
                end = Offset(
                    vasoX + vasoAncho - 3.dp.toPx(),
                    vasoTop + 7.dp.toPx()
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            if (limpia) {
                drawLine(
                    color = ZaziliWhite,
                    start = Offset(
                        vasoX + vasoAncho - 3.dp.toPx(),
                        vasoTop + 7.dp.toPx()
                    ),
                    end = Offset(
                        vasoX + vasoAncho + 2.dp.toPx(),
                        vasoTop - 1.dp.toPx()
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        if (chispas > 0f) {
            val progreso = chispas.coerceIn(0f, 1f)
            val alfa = (1f - progreso).coerceIn(0f, 1f)
            val centro = Offset(ancho / 2f, vasoTop + vasoAlto / 2f)
            val radio = ancho * (0.10f + 0.30f * progreso)
            repeat(12) { indice ->
                val angulo = (Math.PI * 2.0 * indice / 12.0).toFloat()
                val punto = Offset(
                    x = centro.x + kotlin.math.cos(angulo) * radio,
                    y = centro.y + kotlin.math.sin(angulo) * radio * 0.62f
                )
                drawCircle(
                    color = if (indice % 2 == 0) AmarilloSuave else MintGreen,
                    radius = 3.dp.toPx() * (1f - progreso * 0.4f),
                    center = punto,
                    alpha = alfa
                )
            }
        }

        if (zen) {
            drawCircle(
                color = ZaziliWhite.copy(alpha = 0.10f),
                radius = ancho * 0.5f,
                center = Offset(ancho * 0.1f, alto * 0.1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  PALETA DE CAPAS
// ---------------------------------------------------------------------------

private fun colorCapa(capa: CapaFiltro): Color = when (capa) {
    CapaFiltro.GRAVA -> Color(0xFFB9B0A2)
    CapaFiltro.ARENA -> Color(0xFFF2D18B)
    CapaFiltro.CARBON -> Color(0xFF5F5760)
    CapaFiltro.ALGODON -> LavandaClara
}

private fun textoCapa(capa: CapaFiltro): Color = when (capa) {
    CapaFiltro.CARBON -> ZaziliWhite
    else -> UniversoProfundo
}
