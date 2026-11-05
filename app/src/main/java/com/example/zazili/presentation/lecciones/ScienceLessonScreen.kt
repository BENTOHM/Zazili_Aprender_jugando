package com.example.zazili.presentation.lecciones

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.ciencias.ElementoCientifico
import com.example.zazili.domain.ciencias.ResultadoSandbox
import com.example.zazili.domain.ciencias.ScienceSandboxEngine
import com.example.zazili.domain.curriculum.ciencias.MecanicaCiencia
import com.example.zazili.domain.curriculum.ciencias.ScienceCurriculumRepository
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.modulos.comun.contenidoDe
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
 * Lección de Ciencias (El Laboratorio del Cenote).
 *
 * Es el mismo motor de cola de [LessonViewModel] que Matemáticas y Lengua
 * (corazones, respiración 4-2-4, teoría y estrellas), pero enruta cada reto
 * según su [MecanicaCiencia]:
 *  - [MecanicaCiencia.OPCIONES] → tarjetas de opción múltiple.
 *  - [MecanicaCiencia.REACCION] → arenero de causa y efecto con arrastre.
 *  - [MecanicaCiencia.FILTRO_MAGICO] → el prototipo del Filtro Mágico.
 *
 * El nodo 50, "El Guardián del Cenote", cierra el currículo con el diálogo
 * "¡Continuará...!".
 */
@Composable
fun ScienceLessonScreen(
    modulo: ModuloZazili,
    nodoId: String?,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aplicacion = LocalContext.current.applicationContext as Application
    val viewModel: LessonViewModel = viewModel(
        key = "leccion_ciencias_${nodoId.orEmpty()}",
        factory = remember(modulo, nodoId, aplicacion) {
            LessonViewModel.factory(aplicacion, modulo, nodoId)
        }
    )
    val estado by viewModel.state.collectAsStateWithLifecycle()
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
                AzulCielo.copy(alpha = 0.34f)
            )
        )
    }

    val progresoAnimado by animateFloatAsState(
        targetValue = estado.progress,
        animationSpec = tween(
            durationMillis = if (zen) 0 else 420,
            easing = FastOutSlowInEasing
        ),
        label = "progresoCiencias"
    )

    var dialogoFinal by remember(nodoId) { mutableStateOf(false) }
    LaunchedEffect(estado.isFinished, nodoId) {
        if (estado.isFinished && nodoId == ScienceCurriculumRepository.ULTIMO_NODO_ID) {
            dialogoFinal = true
        }
    }

    BackHandler(
        enabled = estado.feedback != null || estado.isBreathing || estado.mostrarTeoria
    ) {
        if (estado.mostrarTeoria && !estado.isBreathing) {
            viewModel.cerrarTeoria()
        }
    }

    val ejercicioActual = estado.currentExercise as? ScienceExercise
    LaunchedEffect(ejercicioActual?.id, estado.audioApoyo, estado.mostrarTeoria) {
        val ejercicio = ejercicioActual ?: return@LaunchedEffect
        if (estado.audioApoyo && !estado.mostrarTeoria) {
            StoryNarratorEngine.narrar("${ejercicio.enunciado}. ${ejercicio.pista}")
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

                        else -> PantallaSinEjercicioCiencia(
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

                    is ScienceExercise -> key(
                        ejercicio.id,
                        estado.roundToken
                    ) {
                        val opcionesValidas = ejercicio.opciones.isNotEmpty() &&
                            ejercicio.indiceCorrecto in ejercicio.opciones.indices
                        if (!opcionesValidas) {
                            // Ningún reto puede tumbar la lección: si el
                            // contenido llegara incompleto, se ofrece otra
                            // ronda en lugar de indexar una lista vacía.
                            PantallaSinEjercicioCiencia(
                                onReintentar = {
                                    GlobalAudioManager.playPop()
                                    viewModel.reiniciar()
                                },
                                onSalir = onSalir,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            when (ejercicio.mecanica) {
                                MecanicaCiencia.FILTRO_MAGICO -> FiltroMagicoScreen(
                                    exercise = ejercicio,
                                    onSubmitAnswer = { indice, acierto ->
                                        viewModel.enviarRespuesta(indice, acierto)
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    activo = estado.feedback == null
                                )

                                MecanicaCiencia.REACCION -> if (ejercicio.elementos.size >= 2) {
                                    SandboxReaccionScreen(
                                        exercise = ejercicio,
                                        onSubmitAnswer = { indice, acierto ->
                                            viewModel.enviarRespuesta(indice, acierto)
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        activo = estado.feedback == null
                                    )
                                } else {
                                    OpcionesCienciaScreen(
                                        exercise = ejercicio,
                                        onSubmitAnswer = { indice, acierto ->
                                            viewModel.enviarRespuesta(indice, acierto)
                                        },
                                        bloqueado = estado.feedback != null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                MecanicaCiencia.OPCIONES -> OpcionesCienciaScreen(
                                    exercise = ejercicio,
                                    onSubmitAnswer = { indice, acierto ->
                                        viewModel.enviarRespuesta(indice, acierto)
                                    },
                                    bloqueado = estado.feedback != null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Este reto pertenece a otra materia. Vuelve a su mapa.",
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

        if (dialogoFinal) {
            DialogoContinuara(
                onVolverAlMapa = {
                    dialogoFinal = false
                    onSalir()
                },
                onSeguirExplorando = { dialogoFinal = false }
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  RETO DE OPCIONES
// ---------------------------------------------------------------------------

@Composable
private fun OpcionesCienciaScreen(
    exercise: ScienceExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    bloqueado: Boolean,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    val enviarActual by rememberUpdatedState(onSubmitAnswer)
    val opciones = remember(exercise.id, exercise.opciones, exercise.indiceCorrecto) {
        LessonEngine.opcionesDe(exercise)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "LABORATORIO DEL CENOTE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            color = MagicalPurple
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = exercise.titulo,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
            shadowElevation = if (zen) 0.dp else 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BionicText(
                    text = exercise.enunciado,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = UniversoProfundo
                )
                Spacer(Modifier.width(8.dp))
                BotonAltavoz(
                    texto = "${exercise.enunciado}. ${exercise.pista}",
                    tamano = 34.dp,
                    colorFondo = LavandaClara
                )
            }
        }

        if (exercise.visuales.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            "Materiales del reto: ${exercise.visuales.joinToString(", ")}"
                    }
            ) {
                exercise.visuales.forEach { visual ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (zen) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            ZaziliWhite.copy(alpha = 0.72f)
                        }
                    ) {
                        Text(
                            text = visual,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            opciones.forEachIndexed { indice, opcion ->
                OpcionCiencia(
                    indice = indice,
                    texto = opcion.text,
                    bloqueada = bloqueado,
                    zen = zen,
                    onClick = {
                        if (!bloqueado) {
                            GlobalAudioManager.playPop()
                            enviarActual(indice, opcion.isCorrect)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (zen) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                AmarilloSuave.copy(alpha = 0.72f)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "PISTA DE ZAZI · ${exercise.pista}",
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = UniversoProfundo.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun OpcionCiencia(
    indice: Int,
    texto: String,
    bloqueada: Boolean,
    zen: Boolean,
    onClick: () -> Unit
) {
    val letra = ('A' + indice).toString()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
        shadowElevation = if (zen || bloqueada) 0.dp else 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(
                enabled = !bloqueada,
                role = Role.Button,
                onClickLabel = "Elegir la respuesta $letra",
                onClick = onClick
            )
            .semantics {
                contentDescription = "Respuesta $letra: $texto"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(LavandaClara),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = letra,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = MagicalPurple
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = texto,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  ARENERO DE REACCIONES
// ---------------------------------------------------------------------------

@Composable
private fun SandboxReaccionScreen(
    exercise: ScienceExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activo: Boolean = true
) {
    val zen = collectZenMode()
    val enviarActual by rememberUpdatedState(onSubmitAnswer)
    val elementos = remember(exercise.id) { exercise.elementos.distinct() }
    val esperado = exercise.resultadoEsperado
    val motor = remember(exercise.id) { ScienceSandboxEngine() }
    val alcance = rememberCoroutineScope()

    var primero by remember(exercise.id) {
        mutableStateOf<ElementoCientifico?>(null)
    }
    var resultado by remember(exercise.id) { mutableStateOf<ResultadoSandbox?>(null) }
    var enviado by remember(exercise.id) { mutableStateOf(false) }
    var arrastrandoIndice by remember(exercise.id) { mutableIntStateOf(-1) }
    var zonaMesa by remember(exercise.id) { mutableStateOf(Rect.Zero) }
    var zonaPrimero by remember(exercise.id) { mutableStateOf(Rect.Zero) }

    val chispas = remember(exercise.id) { Animatable(0f) }
    val arrastres = remember(exercise.id) {
        List(elementos.size) { Animatable(Offset.Zero, Offset.VectorConverter) }
    }
    val centros = remember(exercise.id) { Array(elementos.size) { Offset.Zero } }

    val puedeTocar = activo && !enviado

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

    fun combinar(elemento: ElementoCientifico) {
        val base = primero ?: return
        if (!puedeTocar) return
        val salida = motor.combinar(base, elemento)
        resultado = salida
        val acierto = salida.huboReaccion &&
            (esperado == null || salida.producto == esperado)

        if (acierto) {
            enviado = true
            GlobalAudioManager.playSuccess()
            alcance.launch {
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
                delay(900)
                enviarActual(exercise.indiceCorrecto, true)
            }
        } else if (salida.huboReaccion) {
            enviado = true
            GlobalAudioManager.playMiss()
            alcance.launch {
                delay(700)
                val indiceErrado =
                    (exercise.indiceCorrecto + 1) % exercise.opciones.size
                enviarActual(indiceErrado, false)
            }
        } else {
            GlobalAudioManager.playMiss()
            alcance.launch {
                delay(1200)
                if (!enviado) {
                    primero = null
                    resultado = null
                }
            }
        }
    }

    fun elegir(indice: Int) {
        if (!puedeTocar) return
        val elemento = elementos.getOrNull(indice) ?: return
        if (elemento == primero) return
        if (primero == null) {
            primero = elemento
            GlobalAudioManager.playPop()
        } else {
            combinar(elemento)
        }
        regresar(indice)
    }

    fun soltar(indice: Int) {
        val elemento = elementos.getOrNull(indice) ?: return
        if (!puedeTocar || elemento == primero) {
            regresar(indice)
            return
        }
        val centroSoltado = centros[indice] + arrastres[indice].value
        val enMesa = zonaMesa != Rect.Zero && zonaMesa.contains(centroSoltado)
        val sobrePrimero = zonaPrimero != Rect.Zero && zonaPrimero.contains(centroSoltado)

        when {
            primero == null && enMesa -> {
                primero = elemento
                GlobalAudioManager.playPop()
            }

            primero != null && (enMesa || sobrePrimero) -> combinar(elemento)
            else -> GlobalAudioManager.playMiss()
        }
        regresar(indice)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "ARENERO CIENTÍFICO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            color = MagicalPurple
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "¿Qué pasa si los juntas?",
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
                            AmarilloSuave.copy(alpha = 0.52f),
                            SoftCream,
                            AzulCielo.copy(alpha = 0.46f)
                        )
                    )
                )
        ) {
            Text(
                text = "MESA DE MEZCLA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = UniversoProfundo.copy(alpha = 0.56f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 12.dp)
            )

            KawaiiZazi(
                size = 54.dp,
                descripcion = "Zazi observa la reacción",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 8.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(226.dp, 214.dp)
                    .onGloballyPositioned { zonaMesa = it.boundsInRoot() }
                    .semantics {
                        contentDescription =
                            "Mesa de mezcla. Suelta aquí el primer material y luego " +
                                "arrastra el segundo sobre él."
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val ancho = size.width
                    val alto = size.height
                    drawCircle(
                        color = ZaziliWhite.copy(alpha = 0.56f),
                        radius = ancho * 0.40f,
                        center = Offset(ancho * 0.5f, alto * 0.54f)
                    )
                    drawOval(
                        color = LavandaMedia.copy(alpha = 0.34f),
                        topLeft = Offset(ancho * 0.10f, alto * 0.68f),
                        size = Size(ancho * 0.80f, alto * 0.22f)
                    )
                    drawCircle(
                        color = MintGreen.copy(alpha = 0.20f),
                        radius = ancho * 0.46f,
                        center = Offset(ancho * 0.08f, alto * 1.04f)
                    )
                }

                if (primero == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(text = "🧪", fontSize = 38.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ARRASTRA UN MATERIAL AQUÍ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = UniversoProfundo.copy(alpha = 0.54f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val elementoPrimero = primero
                    if (elementoPrimero != null) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = colorElemento(elementoPrimero),
                            shadowElevation = if (zen) 0.dp else 7.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(118.dp, 114.dp)
                                .onGloballyPositioned { zonaPrimero = it.boundsInRoot() }
                                .semantics {
                                    contentDescription =
                                        "${elementoPrimero.etiqueta} en la mesa. " +
                                            "Arrastra otro material sobre él."
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = resultado?.producto?.emoji
                                        ?: elementoPrimero.emoji,
                                    fontSize = 38.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = resultado?.producto?.etiqueta
                                        ?: elementoPrimero.etiqueta,
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = UniversoProfundo,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            ParticulasReaccion(
                progreso = chispas.value,
                modifier = Modifier.fillMaxSize()
            )

            val salida = resultado
            if (salida != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (salida.huboReaccion) {
                        ZaziliWhite.copy(alpha = 0.92f)
                    } else {
                        AmarilloSuave.copy(alpha = 0.94f)
                    },
                    shadowElevation = if (zen) 0.dp else 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = salida.titulo,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (salida.huboReaccion) MagicalPurple else UniversoProfundo
                        )
                        Text(
                            text = salida.mensaje,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = UniversoProfundo.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Arrastra un material sobre otro. También puedes tocarlos.",
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
                        text = "MATERIALES",
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
                    elementos.forEachIndexed { indice, elemento ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(90.dp)
                                .onGloballyPositioned { coordenadas ->
                                    if (arrastrandoIndice != indice) {
                                        centros[indice] = coordenadas.boundsInRoot().center
                                    }
                                }
                        ) {
                            FichaElemento(
                                elemento = elemento,
                                desplazamiento = arrastres[indice].value,
                                arrastrando = arrastrandoIndice == indice,
                                habilitada = puedeTocar && elemento != primero,
                                zen = zen,
                                modifier = Modifier.fillMaxSize(),
                                onTap = { elegir(indice) },
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

@Composable
private fun FichaElemento(
    elemento: ElementoCientifico,
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
        color = if (zen) MaterialTheme.colorScheme.surface else colorElemento(elemento),
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
            .pointerInput(elemento, habilitada) {
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
                    "${elemento.etiqueta}. Arrástralo a la mesa o tócalo."
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Text(text = elemento.emoji, fontSize = 25.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                text = elemento.etiqueta,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ParticulasReaccion(
    progreso: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (progreso <= 0f) return@Canvas
        val avance = progreso.coerceIn(0f, 1f)
        val alfa = (1f - avance).coerceIn(0f, 1f)
        val centro = Offset(size.width * 0.5f, size.height * 0.55f)
        val radio = size.minDimension * (0.10f + 0.30f * avance)
        repeat(16) { indice ->
            val angulo = (PI * 2.0 * indice / 16.0).toFloat()
            drawCircle(
                color = if (indice % 2 == 0) AmarilloVibrante else MintGreen,
                radius = 3.dp.toPx() * (1f - avance * 0.35f),
                center = Offset(
                    x = centro.x + cos(angulo) * radio,
                    y = centro.y + sin(angulo) * radio * 0.68f
                ),
                alpha = alfa
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  DIÁLOGO FINAL
// ---------------------------------------------------------------------------

@Composable
private fun DialogoContinuara(
    onVolverAlMapa: () -> Unit,
    onSeguirExplorando: () -> Unit
) {
    Dialog(onDismissRequest = { }) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = SoftCream,
            shadowElevation = 18.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
            ) {
                KawaiiZazi(
                    size = 118.dp,
                    descripcion = "Zazi celebra el final del capítulo de Ciencias"
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "¡Continuará...!",
                    fontSize = 27.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = MagicalPurple,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "El Guardián del Cenote te dio su bendición. " +
                        "Zazi ya guardó en su morral las semillas, las nubes y los imanes " +
                        "para la siguiente aventura.",
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UniversoProfundo.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                ZaziButton(
                    text = "Volver al mapa",
                    onClick = onVolverAlMapa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    containerColor = VerdeDuo,
                    contentColor = contenidoDe(VerdeDuo),
                    cornerRadius = 20.dp,
                    baseHeight = 5.dp,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Seguir explorando",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(role = Role.Button, onClick = onSeguirExplorando)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MagicalPurple
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  ESTADO SIN EJERCICIO
// ---------------------------------------------------------------------------

@Composable
private fun PantallaSinEjercicioCiencia(
    onReintentar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        KawaiiZazi(
            size = 132.dp,
            descripcion = "Zazi te espera con otro experimento"
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Este experimento se quedó sin materiales.",
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
//  PALETA DE MATERIALES
// ---------------------------------------------------------------------------

private fun colorElemento(elemento: ElementoCientifico): Color = when (elemento) {
    ElementoCientifico.AGUA -> AzulCielo
    ElementoCientifico.CALOR -> Color(0xFFFFD9C9)
    ElementoCientifico.FRIO -> LavandaClara
    ElementoCientifico.HIELO -> Color(0xFFD6F0FA)
    ElementoCientifico.VAPOR -> Color(0xFFEDE8F4)
    ElementoCientifico.SEMILLA -> AmarilloSuave
    ElementoCientifico.TIERRA -> Color(0xFFE3CDB4)
    ElementoCientifico.IMAN -> LavandaMedia
    ElementoCientifico.METAL -> Color(0xFFD9D3CC)
    ElementoCientifico.METAL_ATRAIDO -> LavandaMedia
    ElementoCientifico.PLANTULA -> MintGreen
    ElementoCientifico.BICARBONATO -> ZaziliWhite
    ElementoCientifico.VINAGRE -> Color(0xFFF4E9C9)
    ElementoCientifico.BURBUJAS -> Color(0xFFD8F2F7)
    ElementoCientifico.PLASTICO -> RosaSuave
    ElementoCientifico.LODO -> Color(0xFFD8C3A5)
    ElementoCientifico.ROCA -> Color(0xFFD5D0C8)
    ElementoCientifico.LUZ -> AmarilloSuave
    ElementoCientifico.OXIGENO -> MintGreen
}
