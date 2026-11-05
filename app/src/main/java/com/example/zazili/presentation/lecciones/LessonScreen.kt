package com.example.zazili.presentation.lecciones

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiHeartIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.minijuegos.AlquimiaCenoteScreen
import com.example.zazili.presentation.minijuegos.CadenasAlimenticiasScreen
import com.example.zazili.presentation.minijuegos.CalculoMentalScreen
import com.example.zazili.presentation.minijuegos.FiltroMagicoScreen
import com.example.zazili.presentation.minijuegos.MonstruoBasuraScreen
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.presentation.modulos.comun.nombreCortoDeModulo
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Lección de Zazili 2.0. La barra superior (progreso y corazones) y la hoja de
 * retroalimentación envuelven el área central, donde un `when` decide qué
 * minijuego pintar. Sin ejercicios no hay crash: siempre hay un estado visible.
 */
@Composable
fun LessonScreen(
    modulo: ModuloZazili,
    nodoId: String?,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aplicacion = LocalContext.current.applicationContext as Application
    val viewModel: LessonViewModel = viewModel(
        key = "leccion_${modulo.name}_${nodoId.orEmpty()}",
        factory = remember(modulo, nodoId, aplicacion) {
            LessonViewModel.factory(aplicacion, modulo, nodoId)
        }
    )
    val estado by viewModel.state.collectAsStateWithLifecycle()
    val zazoles by viewModel.zazoles.collectAsStateWithLifecycle()
    val zen = collectZenMode()

    val fondoBase = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }
    val fondo = if (zen) {
        Brush.verticalGradient(listOf(fondoBase, fondoBase))
    } else {
        Brush.verticalGradient(
            listOf(
                fondoBase,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            )
        )
    }

    val progresoAnimado by animateFloatAsState(
        targetValue = estado.progress,
        animationSpec = tween(
            durationMillis = if (zen) 0 else 420,
            easing = FastOutSlowInEasing
        ),
        label = "progresoLeccion"
    )

    BackHandler(
        enabled = estado.feedback != null || estado.isBreathing || estado.mostrarTeoria
    ) {
        if (estado.mostrarTeoria && !estado.isBreathing) {
            viewModel.cerrarTeoria()
        }
    }

    // Apoyo de audio global: los minijuegos de Ciencias y el Cálculo Mental
    // también se leen solos. Matemáticas narra dentro de ExerciseScreen para
    // no duplicar la voz.
    val ejercicioActual = estado.currentExercise
    LaunchedEffect(ejercicioActual?.id, estado.audioApoyo, estado.mostrarTeoria) {
        val texto = when (ejercicioActual) {
            is CalculoMentalExercise -> "${ejercicioActual.consigna}. ${ejercicioActual.enunciado}"
            is ScienceExercise -> "${ejercicioActual.consigna}. ${ejercicioActual.enunciado}"
            else -> null
        }
        if (texto != null && estado.audioApoyo && !estado.mostrarTeoria) {
            StoryNarratorEngine.narrar(texto)
        }
    }
    DisposableEffect(Unit) {
        onDispose { StoryNarratorEngine.detener() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraLeccion(
                progreso = progresoAnimado,
                corazones = estado.hearts,
                modulo = modulo,
                zen = zen,
                onSalir = {
                    GlobalAudioManager.playPop()
                    onSalir()
                }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val ejercicio = estado.currentExercise) {
                    null -> when {
                        estado.isLoading -> PantallaCargando(
                            modifier = Modifier.fillMaxSize()
                        )

                        estado.isFinished -> PantallaCompletada(
                            estrellas = estado.stars,
                            onRepetir = {
                                GlobalAudioManager.playPop()
                                viewModel.reiniciar()
                            },
                            onSalir = {
                                GlobalAudioManager.playPop()
                                onSalir()
                            },
                            modifier = Modifier.fillMaxSize(),
                            puedeSiguiente = estado.siguienteNodoId != null,
                            esUltimaLeccion = estado.esUltimaLeccion,
                            onSiguiente = {
                                GlobalAudioManager.playPop()
                                viewModel.siguienteLeccion()
                            }
                        )

                        else -> PantallaSinEjercicio(
                            onReintentar = {
                                GlobalAudioManager.playPop()
                                viewModel.reiniciar()
                            },
                            onSalir = {
                                GlobalAudioManager.playPop()
                                onSalir()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is MathExercise -> key(
                        ejercicio.id,
                        estado.roundToken
                    ) {
                        ExerciseScreen(
                            exercise = ejercicio,
                            zazoles = zazoles,
                            focoUsado = estado.focoUsado,
                            guiaFoco = estado.guiaFoco,
                            focoAviso = estado.focoAviso,
                            pistaVisible = estado.pistaVisible,
                            bloqueado = estado.feedback != null,
                            audioApoyo = estado.audioApoyo && !estado.mostrarTeoria,
                            onMostrarPista = viewModel::alternarPista,
                            onCerrarPista = viewModel::cerrarPista,
                            onUsarFoco = viewModel::usarFoco,
                            onSubmitAnswer = viewModel::enviarRespuesta,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is CalculoMentalExercise -> key(
                        ejercicio.id,
                        estado.roundToken
                    ) {
                        CalculoMentalScreen(
                            exercise = ejercicio,
                            onSubmitAnswer = { indice, acierto ->
                                viewModel.enviarRespuesta(indice, acierto)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is ScienceExercise -> key(
                        ejercicio.id,
                        estado.roundToken
                    ) {
                        when (ejercicio.minijuegoId) {
                            Minijuego.AlquimiaCenote.id -> AlquimiaCenoteScreen(
                                exercise = ejercicio,
                                onSubmitAnswer = { indice, acierto ->
                                    viewModel.enviarRespuesta(indice, acierto)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            Minijuego.CadenasAlimenticias.id -> CadenasAlimenticiasScreen(
                                exercise = ejercicio,
                                onSubmitAnswer = { indice, acierto ->
                                    viewModel.enviarRespuesta(indice, acierto)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            Minijuego.FiltroMagico.id -> FiltroMagicoScreen(
                                exercise = ejercicio,
                                onSubmitAnswer = { indice, acierto ->
                                    viewModel.enviarRespuesta(indice, acierto)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            Minijuego.MonstruoBasura.id -> MonstruoBasuraScreen(
                                exercise = ejercicio,
                                onSubmitAnswer = { indice, acierto ->
                                    viewModel.enviarRespuesta(indice, acierto)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            else -> Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Este experimento todavía no está listo. Vuelve al laboratorio.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is LanguageExercise -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Este reto es de Lengua. Vuelve al mapa de la Cueva de Leyendas.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        val feedback = estado.feedback
        if (feedback != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(UniversoProfundo.copy(alpha = 0.42f))
                    .pointerInput(Unit) { detectTapGestures { } }
            )
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                HojaRetroalimentacion(
                    feedback = feedback,
                    corazones = estado.hearts,
                    onContinuar = {
                        GlobalAudioManager.playPop()
                        viewModel.continuar()
                    }
                )
            }
        }

        val teoria = estado.teoria
        if (estado.mostrarTeoria && teoria != null) {
            TheoryBottomSheet(
                content = teoria,
                audioApoyo = estado.audioApoyo,
                onEntendido = viewModel::cerrarTeoria
            )
        }

        if (estado.isBreathing) {
            BreathingRecoveryScreen(
                onRecuperado = viewModel::completarRespiracion
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  BARRA SUPERIOR
// ---------------------------------------------------------------------------

@Composable
internal fun BarraLeccion(
    progreso: Float,
    corazones: Int,
    modulo: ModuloZazili,
    zen: Boolean,
    onSalir: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)
    ) {
        ZaziButton(
            text = "Salir de la lección",
            onClick = onSalir,
            modifier = Modifier.size(44.dp),
            containerColor = MaterialTheme.colorScheme.surface,
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

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombreCortoDeModulo(modulo),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = VerdeDuo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(Modifier.width(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.semantics {
                contentDescription = "$corazones de ${LessonViewModel.VIDAS_INICIALES} corazones"
            }
        ) {
            repeat(LessonViewModel.VIDAS_INICIALES) { indice ->
                val alfa by animateFloatAsState(
                    targetValue = if (indice < corazones) 1f else 0.20f,
                    animationSpec = tween(durationMillis = if (zen) 0 else 260),
                    label = "corazonLeccion$indice"
                )
                KawaiiHeartIcon(
                    modifier = Modifier.alpha(alfa),
                    size = 15.dp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  ESTADOS SIN MINIJUEGO
// ---------------------------------------------------------------------------

@Composable
internal fun PantallaCargando(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(24.dp)
    ) {
        KawaiiZazi(
            size = 128.dp,
            descripcion = "Zazi prepara la lección"
        )
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(color = VerdeDuo)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Zazi está preparando tu lección...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PantallaSinEjercicio(
    onReintentar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        KawaiiZazi(
            size = 132.dp,
            descripcion = "Zazi te espera con otra ronda"
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Esta lección se quedó sin ejercicios.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "No pasa nada: Zazi puede prepararte otra ronda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        ZaziButton(
            text = "Reintentar",
            onClick = onReintentar,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            containerColor = VerdeDuo,
            contentColor = contenidoDe(VerdeDuo),
            cornerRadius = 20.dp,
            baseHeight = 5.dp,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(10.dp))
        ZaziButton(
            text = "Volver al mapa",
            onClick = onSalir,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 20.dp,
            baseHeight = 4.dp,
            fontSize = 17.sp
        )
    }
}

// ---------------------------------------------------------------------------
//  HOJA DE RETROALIMENTACIÓN
// ---------------------------------------------------------------------------

@Composable
internal fun HojaRetroalimentacion(
    feedback: LessonFeedback,
    corazones: Int,
    onContinuar: () -> Unit
) {
    val acerto = feedback is LessonFeedback.Acierto
    val correcta = feedback.ejercicio.opciones
        .getOrNull(feedback.ejercicio.indiceCorrecto)
        .orEmpty()
    val fondo = if (acerto) VerdeDuo else CoralAviso

    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = fondo,
        shadowElevation = 18.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(46.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(UniversoProfundo.copy(alpha = 0.22f))
            )
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(UniversoProfundo.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (acerto) {
                        KawaiiCheckIcon(size = 27.dp, color = UniversoProfundo)
                    } else {
                        KawaiiStarIcon(size = 27.dp, color = UniversoProfundo)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (acerto) "¡Súper chido!" else "¡Uy, casi!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (acerto) {
                            "«$correcta» es la respuesta. ¡Muy bien!"
                        } else {
                            "No pasa nada: los errores también enseñan."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo.copy(alpha = 0.78f)
                    )
                }
            }

            if (!acerto) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ZaziliWhite.copy(alpha = 0.42f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "La respuesta correcta es:",
                            style = MaterialTheme.typography.labelSmall,
                            color = UniversoProfundo.copy(alpha = 0.70f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "«$correcta»",
                            style = MaterialTheme.typography.titleMedium,
                            color = UniversoProfundo,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                if (corazones <= 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Ya no quedan corazones. Al continuar, Zazi te invita a respirar y te regala uno.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UniversoProfundo.copy(alpha = 0.78f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ZaziButton(
                text = "Continuar",
                onClick = onContinuar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                containerColor = ZaziliWhite,
                contentColor = if (acerto) VerdeDuoOscuro else UniversoProfundo,
                shadowColor = UniversoProfundo.copy(alpha = 0.28f),
                cornerRadius = 20.dp,
                baseHeight = 5.dp,
                fontSize = 17.sp
            )
        }
    }
}
