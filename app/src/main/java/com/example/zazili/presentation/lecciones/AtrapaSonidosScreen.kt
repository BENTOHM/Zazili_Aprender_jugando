package com.example.zazili.presentation.lecciones

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * "Atrapa-Sonidos": conciencia fonológica con burbujas flotantes.
 *
 * El altavoz pronuncia un sonido o sílaba (por ejemplo, "Ma") y el niño elige
 * la burbuja que suena igual, tocándola o arrastrando a Zazi hasta ella. Un
 * acierto avanza la cola de la lección; un error resta un corazón y el reto
 * vuelve al final de la cola, siempre desde [LessonViewModel].
 *
 * Física del arrastre (patrón obligatorio de Zazili): UN [Animatable] de
 * [Offset] para Zazi, rectángulos globales para cada burbuja, prueba de
 * intersección al soltar y regreso con resorte al punto de reposo.
 */
@Composable
fun AtrapaSonidosScreen(
    exercise: LanguageExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activo: Boolean = true
) {
    val zen = collectZenMode()
    // Las opciones ya llegan barajadas desde el motor; se vinculan a su
    // AnswerOption para que la burbuja tocada se evalúe por isCorrect y no
    // por la letra o la posición que le tocó.
    val opciones = remember(exercise.id, exercise.opciones, exercise.indiceCorrecto) {
        LessonEngine.opcionesDe(exercise).take(4)
    }
    val objetivo = remember(exercise.id, exercise.audioObjetivo) {
        exercise.audioObjetivo?.takeIf { it.isNotBlank() } ?: exercise.enunciado
    }
    val enviarActual by rememberUpdatedState(onSubmitAnswer)

    var enviado by remember(exercise.id) { mutableStateOf(false) }
    var flotandoArriba by remember { mutableStateOf(false) }
    val puedeElegir = activo && !enviado

    val alcance = rememberCoroutineScope()
    val arrastre = remember(exercise.id) {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    var arrastrando by remember { mutableStateOf(false) }
    var centroReposo by remember { mutableStateOf(Offset.Zero) }
    val zonas = remember(exercise.id, opciones.size) {
        Array(opciones.size) { Rect.Zero }
    }

    fun elegir(indice: Int) {
        if (!puedeElegir) return
        val elegida = opciones.getOrNull(indice) ?: return
        enviado = true
        GlobalAudioManager.playPop()
        enviarActual(indice, elegida.isCorrect)
    }

    // Una sola corrutina mueve todas las burbujas: jamás una por burbuja.
    LaunchedEffect(exercise.id, activo, zen) {
        if (!activo || zen) {
            flotandoArriba = false
            return@LaunchedEffect
        }
        while (isActive) {
            delay(1450)
            flotandoArriba = !flotandoArriba
        }
    }

    // El sonido se reproduce solo al abrir el reto, siempre que la lección no
    // esté tapada por la cápsula de teoría o por la hoja de retroalimentación.
    LaunchedEffect(exercise.id, activo, objetivo) {
        if (!activo) return@LaunchedEffect
        delay(320)
        NarradorNativo.hablar(objetivo)
    }
    DisposableEffect(Unit) {
        onDispose { NarradorNativo.detener() }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "CONCIENCIA FONOLÓGICA",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            color = MagicalPurple
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = exercise.titulo,
            fontSize = 20.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
            shadowElevation = if (zen) 0.dp else 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "ESCUCHA Y ATRAPA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = MagicalPurple
                )
                Spacer(Modifier.height(6.dp))
                BionicText(
                    text = exercise.enunciado,
                    style = MaterialTheme.typography.titleMedium,
                    color = UniversoProfundo
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BotonAltavoz(
                        texto = objetivo,
                        tamano = 58.dp,
                        colorFondo = AmarilloVibrante
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Toca el altavoz para oírlo otra vez",
                        style = MaterialTheme.typography.bodySmall,
                        color = UniversoProfundo.copy(alpha = 0.68f)
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "¿Cuál suena igual?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            opciones.forEachIndexed { indice, opcion ->
                BurbujaSonido(
                    texto = opcion.text,
                    indice = indice,
                    fase = if (flotandoArriba) 1f else -1f,
                    habilitada = puedeElegir,
                    zen = zen,
                    modifier = Modifier.weight(1f),
                    onRect = { rect -> zonas[indice] = rect },
                    onClick = { elegir(indice) }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Toca una burbuja o arrastra a Zazi hasta ella.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .onGloballyPositioned { coordenadas ->
                    if (!arrastrando) centroReposo = coordenadas.boundsInRoot().center
                }
                .graphicsLayer {
                    translationX = arrastre.value.x
                    translationY = arrastre.value.y
                }
                .pointerInput(exercise.id, puedeElegir) {
                    if (!puedeElegir) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            arrastrando = true
                            GlobalAudioManager.playPop()
                        },
                        onDragEnd = {
                            val centroSoltado = centroReposo + arrastre.value
                            val indice = zonas.indexOfFirst { zona ->
                                zona.contains(centroSoltado)
                            }
                            if (indice >= 0) {
                                elegir(indice)
                            } else {
                                GlobalAudioManager.playMiss()
                            }
                            alcance.launch {
                                arrastre.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                arrastrando = false
                            }
                        },
                        onDragCancel = {
                            alcance.launch {
                                arrastre.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                arrastrando = false
                            }
                        },
                        onDrag = { cambio, cantidad ->
                            cambio.consume()
                            alcance.launch {
                                arrastre.snapTo(arrastre.value + cantidad)
                            }
                        }
                    )
                }
                .semantics {
                    contentDescription =
                        "Zazi. Arrástrala hasta la burbuja con la sílaba correcta."
                }
        ) {
            KawaiiZazi(
                size = 84.dp,
                animar = !zen,
                descripcion = "Zazi"
            )
        }
    }
}

@Composable
private fun BurbujaSonido(
    texto: String,
    indice: Int,
    fase: Float,
    habilitada: Boolean,
    zen: Boolean,
    modifier: Modifier = Modifier,
    onRect: (Rect) -> Unit,
    onClick: () -> Unit
) {
    val flotacion by animateFloatAsState(
        targetValue = fase,
        animationSpec = tween(
            durationMillis = if (zen) 0 else 1300 + indice * 120,
            easing = FastOutSlowInEasing
        ),
        label = "flotacionBurbuja$indice"
    )
    val desplazamientoY = if (zen) 0.dp else (flotacion * (7 + indice * 2)).dp
    val relleno = when (indice % 4) {
        0 -> LavandaMedia
        1 -> MintGreen
        2 -> AmarilloSuave
        else -> RosaSuave
    }

    Surface(
        shape = CircleShape,
        color = relleno,
        shadowElevation = if (zen) 0.dp else 7.dp,
        modifier = modifier
            .aspectRatio(1f)
            .offset(y = desplazamientoY)
            .onGloballyPositioned { onRect(it.boundsInRoot()) }
            .clip(CircleShape)
            .clickable(enabled = habilitada, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = "Burbuja con la sílaba $texto"
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ZaziliWhite.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Text(
                text = texto,
                fontSize = 21.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Black,
                color = UniversoProfundo,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
