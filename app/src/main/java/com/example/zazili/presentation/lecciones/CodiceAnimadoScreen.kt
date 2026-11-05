package com.example.zazili.presentation.lecciones

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.NarradorNativo
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * Códice Animado: la oración del códice pierde una palabra y el niño la
 * devuelve tocando o arrastrando una tarjeta.
 *
 * Contrato con el motor de lección: [onSubmitAnswer] entrega ÚNICAMENTE si el
 * toque fue correcto. El ViewModel conserva la cola, los corazones y el
 * guardado; esta pantalla no muta estado ajeno ni llama dos veces.
 *
 * Física obligatoria de arrastre de Zazili: un [Animatable] de [Offset] por
 * tarjeta, rectángulo global del hueco, prueba de intersección al soltar y
 * regreso con resorte a [Offset.Zero].
 */
@Composable
fun CodiceAnimadoScreen(
    exercise: LanguageExercise,
    onSubmitAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activo: Boolean = true
) {
    val zen = collectZenMode()
    val responder by rememberUpdatedState(onSubmitAnswer)
    val alcance = rememberCoroutineScope()

    val palabraCorrecta = remember(
        exercise.id,
        exercise.opciones,
        exercise.indiceCorrecto
    ) {
        exercise.opciones.getOrNull(exercise.indiceCorrecto).orEmpty()
    }
    val opciones = remember(
        exercise.id,
        exercise.opciones,
        exercise.indiceCorrecto
    ) {
        val base = exercise.opciones.distinct().take(3)
        when {
            palabraCorrecta.isBlank() -> base
            base.any { it.equals(palabraCorrecta, ignoreCase = true) } -> base
            else -> (base.take(2) + palabraCorrecta)
        }
    }
    val plantilla = remember(exercise.id, exercise.enunciado, palabraCorrecta) {
        plantillaPara(exercise.enunciado, palabraCorrecta)
    }
    val reto = remember(
        exercise.id,
        exercise.enunciado,
        palabraCorrecta,
        plantilla
    ) {
        construirReto(exercise.enunciado, palabraCorrecta, plantilla)
    }

    var enviado by remember(exercise.id) { mutableStateOf(false) }
    var acierto by remember(exercise.id) { mutableStateOf(false) }
    var indiceError by remember(exercise.id) { mutableIntStateOf(-1) }
    var rectanguloOracion by remember(exercise.id) { mutableStateOf(Rect.Zero) }
    var arrastrandoTarjeta by remember(exercise.id) { mutableStateOf(false) }
    val sacudida = remember(exercise.id) { Animatable(0f) }
    val habilitado = activo && !enviado

    // Una respuesta por reto. El error espera a que termine la sacudida para
    // que el niño vea la pista naranja antes de que aparezca la hoja.
    fun elegir(indice: Int) {
        // `enviado` se lee en el momento del toque: dos gestos del mismo frame
        // (clic + fin de arrastre) no pueden reportar la respuesta dos veces.
        if (!activo || enviado || indice !in opciones.indices) return
        val elegida = opciones[indice]
        val esCorrecta = palabraCorrecta.isNotBlank() &&
            elegida.equals(palabraCorrecta, ignoreCase = true)

        enviado = true
        GlobalAudioManager.playPop()

        if (esCorrecta) {
            acierto = true
            responder(true)
        } else {
            indiceError = indice
            alcance.launch {
                sacudida.snapTo(0f)
                sacudida.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 360,
                        easing = LinearEasing
                    )
                )
                responder(false)
            }
        }
    }

    LaunchedEffect(enviado) {
        if (enviado) NarradorNativo.detener()
    }
    DisposableEffect(Unit) {
        onDispose { NarradorNativo.detener() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "CÓDICE MÁGICO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = MagicalPurple
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Códice Animado",
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))

            OracionCodice(
                reto = reto,
                acierto = acierto,
                arrastrando = arrastrandoTarjeta,
                zen = zen,
                onRect = { rectanguloOracion = it }
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "¿Qué palabra completa la oración?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Toca una tarjeta o arrástrala hasta el hueco.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                opciones.forEachIndexed { indice, palabra ->
                    TarjetaPalabraCodice(
                        palabra = palabra,
                        indice = indice,
                        habilitada = habilitado,
                        zen = zen,
                        esCorrecta = acierto &&
                            palabra.equals(palabraCorrecta, ignoreCase = true),
                        esError = indice == indiceError,
                        objetivo = rectanguloOracion,
                        sacudida = sacudida,
                        onArrastrando = { arrastrandoTarjeta = it },
                        onElegir = ::elegir
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                BotonAltavoz(
                    texto = "Completa la oración. ${reto.textoParaEscuchar}",
                    tamano = 34.dp,
                    colorFondo = AmarilloSuave,
                    descripcion = "Escuchar la oración incompleta"
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Escucha la oración si la necesitas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
                )
            }
        }

        if (acierto) {
            FestejoCodice(
                accion = plantilla?.accion ?: AccionCodice.GENERICO,
                emoji = plantilla?.emoji ?: "🌟",
                descripcion = plantilla?.fraseFestejo
                    ?: "¡Muy bien! La oración quedó completa.",
                zen = zen,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  ORACIÓN CON HUECO
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
//  PLANTILLAS DE FAUNA MEXICANA
// ---------------------------------------------------------------------------

internal enum class AccionCodice { CORRER, REVOLOTEAR, ARRASTRAR, SALTAR, GENERICO }

/**
 * Una oración jugable del códice. [textoAntes] y [textoDespues] rodean la
 * palabra que el niño debe recuperar; [emoji] y [accion] controlan la
 * celebración propia de cada animal.
 */
@Immutable
internal data class PlantillaCodice(
    val textoAntes: String,
    val palabraCorrecta: String,
    val textoDespues: String,
    val emoji: String,
    val accion: AccionCodice,
    val fraseFestejo: String
)

internal val PLANTILLAS_CODICE: List<PlantillaCodice> = listOf(
    PlantillaCodice(
        textoAntes = "El ",
        palabraCorrecta = "jaguar",
        textoDespues = " corre por la selva.",
        emoji = "🐆",
        accion = AccionCodice.CORRER,
        fraseFestejo = "¡Muy bien! El jaguar corre por la selva."
    ),
    PlantillaCodice(
        textoAntes = "El ",
        palabraCorrecta = "colibrí",
        textoDespues = " vuela entre flores.",
        emoji = "🐦",
        accion = AccionCodice.REVOLOTEAR,
        fraseFestejo = "¡Muy bien! El colibrí vuela entre flores."
    ),
    PlantillaCodice(
        textoAntes = "La ",
        palabraCorrecta = "tortuga",
        textoDespues = " camina en la arena.",
        emoji = "🐢",
        accion = AccionCodice.ARRASTRAR,
        fraseFestejo = "¡Muy bien! La tortuga camina en la arena."
    ),
    PlantillaCodice(
        textoAntes = "El ",
        palabraCorrecta = "mono",
        textoDespues = " salta en los árboles.",
        emoji = "🐒",
        accion = AccionCodice.SALTAR,
        fraseFestejo = "¡Muy bien! El mono salta en los árboles."
    )
)

private val CARACTERES_FUERA_DE_ORACION = Regex("[^a-záéíóúüñ ]")
private val ESPACIOS_MULTIPLES = Regex("\\s+")

internal fun plantillaPara(enunciado: String, palabra: String): PlantillaCodice? {
    val limpio = normalizarOracion(enunciado)
    return PLANTILLAS_CODICE.firstOrNull { candidata ->
        normalizarOracion(
            "${candidata.textoAntes}${candidata.palabraCorrecta}${candidata.textoDespues}"
        ) == limpio
    } ?: PLANTILLAS_CODICE.firstOrNull {
        it.palabraCorrecta.equals(palabra, ignoreCase = true)
    }
}

private fun normalizarOracion(texto: String): String = texto
    .lowercase()
    .replace(CARACTERES_FUERA_DE_ORACION, " ")
    .replace(ESPACIOS_MULTIPLES, " ")
    .trim()

@Immutable
private data class RetoCodice(
    val antes: String,
    val despues: String,
    val palabra: String,
    val textoParaEscuchar: String
)

/**
 * Parte la oración en el lugar EXACTO de la palabra correcta. Si el currículo
 * cambiara y la palabra no estuviera dentro (no debería pasar), el hueco se
 * coloca al final en lugar de inventar una oración distinta.
 */
private fun construirReto(
    enunciado: String,
    palabra: String,
    plantilla: PlantillaCodice?
): RetoCodice {
    val limpio = enunciado.trim()
    if (palabra.isBlank()) {
        if (plantilla != null) return retoDePlantilla(plantilla)
        val antes = if (limpio.isEmpty()) "En el códice dice: " else "$limpio "
        return RetoCodice(
            antes = antes,
            despues = "",
            palabra = "",
            textoParaEscuchar = "Completa la oración del códice."
        )
    }

    val indice = limpio.indexOf(palabra, ignoreCase = true)
    if (indice < 0) {
        if (plantilla != null) return retoDePlantilla(plantilla)
        val antes = if (limpio.isEmpty()) "En el códice dice: " else "$limpio "
        return RetoCodice(
            antes = antes,
            despues = "",
            palabra = palabra,
            textoParaEscuchar = "Completa la oración del códice."
        )
    }

    val antes = limpio.substring(0, indice)
    val despues = limpio.substring(indice + palabra.length)
    return RetoCodice(
        antes = antes,
        despues = despues,
        palabra = palabra,
        textoParaEscuchar = "${antes.trim()}… ${despues.trim()}"
    )
}

private fun retoDePlantilla(plantilla: PlantillaCodice): RetoCodice = RetoCodice(
    antes = plantilla.textoAntes,
    despues = plantilla.textoDespues,
    palabra = plantilla.palabraCorrecta,
    textoParaEscuchar = "${plantilla.textoAntes.trim()}… ${plantilla.textoDespues.trim()}"
)

@Composable
private fun OracionCodice(
    reto: RetoCodice,
    acierto: Boolean,
    arrastrando: Boolean,
    zen: Boolean,
    onRect: (Rect) -> Unit
) {
    val colorHueco = if (acierto) VerdeDuo.copy(alpha = 0.20f) else LavandaClara
    val colorBorde by animateColorAsState(
        targetValue = when {
            acierto -> VerdeDuo
            arrastrando -> MagicalPurple
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 180),
        label = "bordeOracionCodice"
    )
    val escala by animateFloatAsState(
        targetValue = when {
            acierto -> 1.03f
            arrastrando -> 1.015f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escalaOracionCodice"
    )

    val oracion = remember(reto, acierto, colorHueco) {
        buildAnnotatedString {
            append(reto.antes)
            withStyle(
                SpanStyle(
                    background = colorHueco,
                    color = if (acierto) VerdeDuoOscuro else MagicalPurple,
                    fontWeight = FontWeight.Black
                )
            ) {
                if (acierto) {
                    if (reto.palabra.isBlank()) append(" ✓ ")
                    else append(" ${reto.palabra} ")
                } else {
                    append(" [ ____ ] ")
                }
            }
            append(reto.despues)
        }
    }
    val descripcion = if (acierto) {
        "${reto.antes} ${reto.palabra} ${reto.despues}".trim()
    } else {
        "${reto.antes} hueco. ${reto.despues}".trim()
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
        border = BorderStroke(2.dp, colorBorde),
        shadowElevation = if (zen) 0.dp else 5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .onGloballyPositioned { onRect(it.boundsInRoot()) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = AmarilloSuave,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "📜", fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "EL CÓDICE DICE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                    color = MagicalPurple
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = oracion,
                fontSize = 25.sp,
                lineHeight = 33.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (zen) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    UniversoProfundo
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = descripcion }
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  TARJETAS DE PALABRA: TOQUE O ARRASTRE
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaPalabraCodice(
    palabra: String,
    indice: Int,
    habilitada: Boolean,
    zen: Boolean,
    esCorrecta: Boolean,
    esError: Boolean,
    objetivo: Rect,
    sacudida: Animatable<Float, AnimationVector1D>,
    onArrastrando: (Boolean) -> Unit,
    onElegir: (Int) -> Unit
) {
    val alcance = rememberCoroutineScope()
    val desplazamiento = remember {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    var centro by remember { mutableStateOf(Offset.Zero) }
    var arrastrando by remember { mutableStateOf(false) }

    val colorBorde by animateColorAsState(
        targetValue = when {
            esCorrecta -> VerdeDuo
            esError -> CoralAviso
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 160),
        label = "bordePalabra$indice"
    )
    val colorFondo by animateColorAsState(
        targetValue = when {
            esCorrecta -> VerdeDuo.copy(alpha = 0.16f)
            esError -> CoralAviso.copy(alpha = 0.12f)
            zen -> MaterialTheme.colorScheme.surfaceVariant
            else -> SoftCream
        },
        animationSpec = tween(durationMillis = 160),
        label = "fondoPalabra$indice"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colorFondo,
        border = BorderStroke(2.5.dp, colorBorde),
        shadowElevation = if (zen || arrastrando) 0.dp else 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .onGloballyPositioned { centro = it.boundsInRoot().center }
            .graphicsLayer {
                // La sacudida se evalúa solo en la tarjeta equivocada y vive en
                // la fase de dibujo: ninguna tarjeta se recompone por frame.
                val onda = if (esError) sacudida.value else 0f
                val sacudidaPx = if (esError) {
                    sin(onda * PI.toFloat() * 6f) * (1f - onda) * 15.dp.toPx()
                } else {
                    0f
                }
                translationX = desplazamiento.value.x + sacudidaPx
                translationY = desplazamiento.value.y
                rotationZ = if (esError) {
                    0f
                } else {
                    (desplazamiento.value.x / 80.dp.toPx()).coerceIn(-7f, 7f)
                }
                scaleX = if (arrastrando) 1.035f else 1f
                scaleY = if (arrastrando) 1.035f else 1f
            }
            .pointerInput(habilitada, indice) {
                if (!habilitada) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        arrastrando = true
                        onArrastrando(true)
                        GlobalAudioManager.playPop()
                    },
                    onDragEnd = {
                        val soltado = centro + desplazamiento.value
                        val cayoEnElHueco = objetivo != Rect.Zero &&
                            objetivo.contains(soltado)
                        if (cayoEnElHueco) onElegir(indice)
                        alcance.launch {
                            desplazamiento.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            arrastrando = false
                            onArrastrando(false)
                        }
                    },
                    onDragCancel = {
                        alcance.launch {
                            desplazamiento.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            arrastrando = false
                            onArrastrando(false)
                        }
                    },
                    onDrag = { cambio, cantidad ->
                        cambio.consume()
                        alcance.launch {
                            desplazamiento.snapTo(desplazamiento.value + cantidad)
                        }
                    }
                )
            }
            .clickable(
                enabled = habilitada,
                role = Role.Button,
                onClickLabel = "Elegir $palabra"
            ) {
                onElegir(indice)
            }
            .semantics {
                role = Role.Button
                contentDescription =
                    "Palabra $palabra. Toca para completar la oración o arrástrala al hueco."
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (esCorrecta) VerdeDuo else LavandaClara,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (esCorrecta) "✓" else "✋",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (esCorrecta) ZaziliWhite else MagicalPurple
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = palabra,
                fontSize = 21.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    esError -> CoralAviso
                    esCorrecta -> MaterialTheme.colorScheme.onSurface
                    zen -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> UniversoProfundo
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "☰",
                fontSize = 17.sp,
                color = UniversoProfundo.copy(alpha = 0.34f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  RECOMPENSA: CADA ANIMAL TIENE SU PROPIO MOVIMIENTO
// ---------------------------------------------------------------------------

/**
 * Se monta SOLO cuando hubo acierto. El progreso vive en un [Animatable] y se
 * lee dentro de [graphicsLayer] y del [Canvas]: la secuencia corre en la fase
 * de dibujo, sin recomponer la pantalla. En Modo Zen no hay partículas ni
 * desplazamientos, solo el animal quieto.
 */
@Composable
private fun FestejoCodice(
    accion: AccionCodice,
    emoji: String,
    descripcion: String,
    zen: Boolean,
    modifier: Modifier = Modifier
) {
    val avance = remember(accion) { Animatable(0f) }

    LaunchedEffect(accion, zen) {
        avance.snapTo(0f)
        if (zen) {
            avance.snapTo(1f)
            return@LaunchedEffect
        }
        avance.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = duracionFestejo(accion),
                easing = LinearEasing
            )
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = descripcion
            },
        contentAlignment = Alignment.Center
    ) {
        val anchoPx = constraints.maxWidth.toFloat()
        val altoPx = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (zen) return@Canvas
            val progreso = avance.value
            if (progreso <= 0f) return@Canvas
            val base = Offset(size.width / 2f, size.height / 2f)
            val desplazamiento = desplazamientoFestejo(
                accion,
                progreso,
                size.width,
                size.height
            )
            val centro = Offset(
                base.x + desplazamiento.x,
                base.y + desplazamiento.y
            )
            if (accion == AccionCodice.SALTAR) dibujarLianasFestejo(progreso)
            drawCircle(
                color = AmarilloSuave.copy(alpha = 0.16f),
                radius = 118.dp.toPx(),
                center = centro
            )
            when (accion) {
                AccionCodice.CORRER -> dibujarPolvo(progreso, centro)
                AccionCodice.REVOLOTEAR ->
                    dibujarDestellosVuelo(progreso, base, size.width, size.height)
                AccionCodice.ARRASTRAR ->
                    dibujarHuellas(progreso, base, size.width, size.height)
                AccionCodice.SALTAR -> dibujarHojasSalto(progreso, centro)
                AccionCodice.GENERICO -> dibujarDestellosCentro(progreso, centro)
            }
        }

        Text(
            text = emoji,
            fontSize = 116.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clearAndSetSemantics { }
                .graphicsLayer {
                    val progreso = avance.value
                    val desplazamiento = desplazamientoFestejo(
                        accion,
                        progreso,
                        anchoPx,
                        altoPx
                    )
                    translationX = if (zen) 0f else desplazamiento.x
                    translationY = if (zen) 0f else desplazamiento.y
                    rotationZ = if (zen) 0f else rotacionFestejo(accion, progreso)
                    val escala = if (zen) 1f else escalaFestejo(accion, progreso)
                    scaleX = escala
                    scaleY = escala
                    alpha = if (zen) 1f else (progreso / 0.08f).coerceIn(0f, 1f)
                }
        )

        Text(
            text = "¡El códice brilla!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = VerdeDuoOscuro,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .clearAndSetSemantics { }
                .graphicsLayer {
                    alpha = if (zen) {
                        1f
                    } else {
                        ((avance.value - 0.30f) / 0.22f).coerceIn(0f, 1f)
                    }
                }
        )
    }
}

private fun duracionFestejo(accion: AccionCodice): Int = when (accion) {
    AccionCodice.CORRER -> 1_150
    AccionCodice.REVOLOTEAR -> 1_500
    AccionCodice.ARRASTRAR -> 2_100
    AccionCodice.SALTAR -> 1_650
    AccionCodice.GENERICO -> 1_100
}

/**
 * Trayectoria en píxeles para cada animal. Todas empiezan y terminan cerca del
 * centro para que la celebración no deje al animal fuera de cuadro.
 */
private fun desplazamientoFestejo(
    accion: AccionCodice,
    progreso: Float,
    ancho: Float,
    alto: Float
): Offset {
    val t = progreso.coerceIn(0f, 1f)
    return when (accion) {
        AccionCodice.CORRER -> {
            val carrera = (t / 0.78f).coerceAtMost(1f)
            Offset(
                x = (carrera - 1f) * ancho * 0.42f,
                y = -abs(sin(carrera * PI.toFloat() * 5f)) * alto * 0.030f
            )
        }
        AccionCodice.REVOLOTEAR -> {
            val giro = t * 2f * PI.toFloat()
            Offset(
                x = sin(giro) * ancho * 0.20f,
                y = -(1f - cos(giro)) * alto * 0.14f +
                    sin(t * 7f * PI.toFloat()) * alto * 0.010f
            )
        }
        AccionCodice.ARRASTRAR -> Offset(
            x = (t - 1f) * ancho * 0.30f,
            y = -abs(sin(t * PI.toFloat() * 4f)) * alto * 0.018f
        )
        AccionCodice.SALTAR -> {
            val salto = sin(t * 3f * PI.toFloat())
            Offset(
                x = salto * ancho * 0.10f,
                y = -abs(salto) * alto * 0.24f
            )
        }
        AccionCodice.GENERICO -> Offset(
            x = 0f,
            y = -abs(sin(t * 2f * PI.toFloat())) * alto * 0.05f * (1f - t)
        )
    }
}

private fun rotacionFestejo(accion: AccionCodice, progreso: Float): Float =
    when (accion) {
        AccionCodice.CORRER -> sin(progreso * PI.toFloat() * 10f) * 3f * (1f - progreso)
        AccionCodice.REVOLOTEAR -> sin(progreso * 2f * PI.toFloat()) * 11f
        AccionCodice.ARRASTRAR -> sin(progreso * PI.toFloat() * 4f) * 2.5f
        AccionCodice.SALTAR -> sin(progreso * PI.toFloat() * 6f) * 7f
        AccionCodice.GENERICO -> sin(progreso * PI.toFloat() * 4f) * 4f
    }

private fun escalaFestejo(accion: AccionCodice, progreso: Float): Float =
    when (accion) {
        AccionCodice.ARRASTRAR -> 1f + sin(progreso * PI.toFloat() * 4f) * 0.012f
        AccionCodice.SALTAR -> 1f + abs(sin(progreso * PI.toFloat() * 3f)) * 0.035f
        else -> 1f
    }

private fun DrawScope.dibujarPolvo(progreso: Float, centro: Offset) {
    if (progreso >= 0.92f) return
    repeat(7) { indice ->
        val alfa = (1f - progreso) * 0.55f * (1f - indice / 8f)
        if (alfa <= 0.01f) return@repeat
        val patada = sin(progreso * 12f + indice) * 5.dp.toPx()
        drawCircle(
            color = Color(0xFFE8D9B8).copy(alpha = alfa),
            radius = (5f + (indice % 3) * 2.2f).dp.toPx() * (0.6f + progreso * 0.5f),
            center = Offset(
                x = centro.x - 22.dp.toPx() - indice * 13.dp.toPx() + patada,
                y = centro.y + 54.dp.toPx() - (indice % 3) * 5.dp.toPx()
            )
        )
    }
}

private fun DrawScope.dibujarDestellosVuelo(
    progreso: Float,
    base: Offset,
    ancho: Float,
    alto: Float
) {
    repeat(8) { indice ->
        val anterior = progreso - indice * 0.035f
        if (anterior <= 0f) return@repeat
        val posicion = desplazamientoFestejo(
            AccionCodice.REVOLOTEAR,
            anterior,
            ancho,
            alto
        )
        val alfa = (1f - indice / 9f) * 0.8f * (1f - progreso * 0.3f)
        if (alfa <= 0.01f) return@repeat
        dibujarDestelloPixel(
            centro = Offset(base.x + posicion.x, base.y + posicion.y),
            radio = (3.5f + (indice % 3) * 1.8f).dp.toPx(),
            color = when (indice % 3) {
                0 -> AmarilloSuave
                1 -> LavandaClara
                else -> Color(0xFF80DEEA)
            }.copy(alpha = alfa)
        )
    }
}

private fun DrawScope.dibujarHuellas(
    progreso: Float,
    base: Offset,
    ancho: Float,
    alto: Float
) {
    val pasos = 14
    repeat(pasos) { indice ->
        val momento = (indice + 1) / pasos.toFloat()
        if (momento > progreso) return@repeat
        val posicion = desplazamientoFestejo(
            AccionCodice.ARRASTRAR,
            momento,
            ancho,
            alto
        )
        val centro = Offset(
            x = base.x + posicion.x + 34.dp.toPx(),
            y = base.y + 56.dp.toPx() +
                if (indice % 2 == 0) 4.dp.toPx() else -4.dp.toPx()
        )
        drawOval(
            color = Color(0xFFD9C08A).copy(alpha = 0.55f),
            topLeft = Offset(centro.x - 5.dp.toPx(), centro.y - 3.dp.toPx()),
            size = Size(10.dp.toPx(), 6.dp.toPx())
        )
        repeat(3) { dedo ->
            drawCircle(
                color = Color(0xFFC9AE76).copy(alpha = 0.5f),
                radius = 1.1.dp.toPx(),
                center = Offset(
                    centro.x - 3.dp.toPx() + dedo * 3.dp.toPx(),
                    centro.y - 4.dp.toPx()
                )
            )
        }
    }
}

private fun DrawScope.dibujarLianasFestejo(progreso: Float) {
    val ancho = size.width
    val alto = size.height
    repeat(3) { indice ->
        val baseX = ancho * (0.22f + indice * 0.28f)
        val vaiven = sin(progreso * 6f + indice) * ancho * 0.012f
        val liana = Path().apply {
            moveTo(baseX, 0f)
            quadraticTo(
                baseX + vaiven,
                alto * 0.28f,
                baseX + vaiven * 1.4f,
                alto * 0.58f
            )
        }
        drawPath(
            path = liana,
            color = Color(0xFF4F9B5E).copy(alpha = 0.55f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        repeat(2) { hoja ->
            val fy = alto * (0.16f + hoja * 0.18f)
            val fx = baseX + vaiven * (hoja + 1) * 0.4f
            drawOval(
                color = Color(0xFF7ED08A).copy(alpha = 0.6f),
                topLeft = Offset(fx - 8.dp.toPx(), fy),
                size = Size(16.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

private fun DrawScope.dibujarHojasSalto(progreso: Float, centro: Offset) {
    repeat(5) { indice ->
        val fase = (progreso * 2.2f + indice / 5f) % 1f
        val alfa = (1f - fase) * 0.55f
        if (alfa <= 0.01f) return@repeat
        val x = centro.x + sin(fase * PI.toFloat() * 2f + indice) * 42.dp.toPx()
        val y = centro.y - 40.dp.toPx() + fase * 34.dp.toPx()
        drawOval(
            color = Color(0xFF7ED08A).copy(alpha = alfa),
            topLeft = Offset(x, y),
            size = Size(7.dp.toPx(), 4.dp.toPx())
        )
    }
}

private fun DrawScope.dibujarDestellosCentro(progreso: Float, centro: Offset) {
    repeat(6) { indice ->
        val angulo = indice * PI.toFloat() / 3f + progreso * PI.toFloat()
        val radio = (22f + progreso * 34f + (indice % 2) * 12f).dp.toPx()
        val alfa = (1f - progreso) * 0.8f
        if (alfa <= 0.01f) return@repeat
        dibujarDestelloPixel(
            centro = Offset(
                centro.x + cos(angulo) * radio,
                centro.y + sin(angulo) * radio * 0.6f
            ),
            radio = (3.5f + (indice % 3) * 1.6f).dp.toPx(),
            color = AmarilloSuave.copy(alpha = alfa)
        )
    }
}

private fun DrawScope.dibujarDestelloPixel(
    centro: Offset,
    radio: Float,
    color: Color
) {
    drawRect(
        color = color,
        topLeft = Offset(centro.x - radio * 0.4f, centro.y - radio * 1.6f),
        size = Size(radio * 0.8f, radio * 3.2f)
    )
    drawRect(
        color = color,
        topLeft = Offset(centro.x - radio * 1.6f, centro.y - radio * 0.4f),
        size = Size(radio * 3.2f, radio * 0.8f)
    )
}
