package com.example.zazili.presentation.lecciones

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.NarradorNativo
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.BotonFlotanteTTS
import com.example.zazili.core.narrator.TTSManager
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.curriculum.lengua.LanguageCurriculumRepository
import com.example.zazili.domain.curriculum.lengua.MecanicaLengua
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Lección de Lengua de Zazili 2.0.
 *
 * Comparte el motor de cola, corazones, teoría y respiración con Matemáticas
 * ([LessonViewModel]); esta pantalla solo decide qué minijuego pinta cada reto:
 *  - [MecanicaLengua.ATRAPA_SONIDOS] → [AtrapaSonidosScreen].
 *  - El resto → reto de opción múltiple con texto biónico y altavoz nativo.
 *
 * Al terminar el nodo 50 de la saga se abre el aviso "¡Continuará...!".
 */
@Composable
fun LanguageLessonScreen(
    modulo: ModuloZazili,
    nodoId: String?,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aplicacion = LocalContext.current.applicationContext as Application
    val viewModel: LessonViewModel = viewModel(
        key = "lengua_${nodoId.orEmpty()}",
        factory = remember(modulo, nodoId, aplicacion) {
            LessonViewModel.factory(aplicacion, modulo, nodoId)
        }
    )
    val estado by viewModel.state.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    val contexto = LocalContext.current
    val ttsActivo by TTSManager.activo.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        TTSManager.inicializar(contexto)
    }

    val esFinalDeSaga = nodoId == LanguageCurriculumRepository.ULTIMO_NODO_ID
    var dialogoFinalVisto by rememberSaveable(nodoId) { mutableStateOf(false) }

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
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            )
        )
    }

    val progresoAnimado by animateFloatAsState(
        targetValue = estado.progress,
        animationSpec = tween(
            durationMillis = if (zen) 0 else 420,
            easing = FastOutSlowInEasing
        ),
        label = "progresoLengua"
    )

    BackHandler(
        enabled = estado.feedback != null || estado.isBreathing || estado.mostrarTeoria
    ) {
        if (estado.mostrarTeoria && !estado.isBreathing) {
            viewModel.cerrarTeoria()
        }
    }

    // Lectura automática de los retos escritos (no de Atrapa-Sonidos ni del
    // Códice Animado: ahí el juego ya maneja su propio audio y no conviene
    // reventar la respuesta correcta antes de que el niño elija).
    //
    // Dos caminos, nunca simultáneos: el interruptor flotante 🔊 usa
    // [TTSManager] y deja la voz fija; si está apagado, se respeta el apoyo
    // de audio de la lección con [NarradorNativo].
    val ejercicioActual = estado.currentExercise as? LanguageExercise
    LaunchedEffect(
        ejercicioActual?.id,
        estado.roundToken,
        estado.audioApoyo,
        estado.mostrarTeoria,
        estado.feedback == null,
        ttsActivo
    ) {
        val reto = ejercicioActual
        if (estado.mostrarTeoria || estado.feedback != null) {
            NarradorNativo.detener()
            TTSManager.detener()
            return@LaunchedEffect
        }
        if (
            reto != null &&
            (estado.audioApoyo || ttsActivo) &&
            reto.mecanica != MecanicaLengua.ATRAPA_SONIDOS &&
            reto.mecanica != MecanicaLengua.CODICE_ANIMADO
        ) {
            val texto = "${reto.enunciado}. ${reto.consigna}"
            if (ttsActivo) {
                TTSManager.leer(texto)
            } else {
                NarradorNativo.hablar(texto)
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            NarradorNativo.detener()
            TTSManager.detener()
        }
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
                                dialogoFinalVisto = false
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
                                dialogoFinalVisto = false
                                viewModel.siguienteLeccion()
                            }
                        )

                        else -> LenguaSinReto(
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

                    is LanguageExercise -> key(
                        ejercicio.id,
                        estado.roundToken
                    ) {
                        if (ejercicio.mecanica == MecanicaLengua.ATRAPA_SONIDOS) {
                            AtrapaSonidosScreen(
                                exercise = ejercicio,
                                onSubmitAnswer = viewModel::enviarRespuesta,
                                activo = !estado.mostrarTeoria && estado.feedback == null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (ejercicio.mecanica == MecanicaLengua.CODICE_ANIMADO) {
                            CodiceAnimadoScreen(
                                exercise = ejercicio,
                                onSubmitAnswer = { acierto ->
                                    // El ViewModel valida contra `indiceCorrecto`;
                                    // la pantalla ya decidió si el toque fue
                                    // correcto y aquí solo se conserva el índice
                                    // para que la hoja muestre la respuesta real.
                                    viewModel.enviarRespuesta(
                                        ejercicio.indiceCorrecto,
                                        acierto
                                    )
                                },
                                activo = !estado.mostrarTeoria && estado.feedback == null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            RetoLenguaOpciones(
                                ejercicio = ejercicio,
                                audioApoyo = estado.audioApoyo && !estado.mostrarTeoria,
                                onSubmitAnswer = viewModel::enviarRespuesta,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Este reto llegó de otra materia. Vuelve al mapa de Lengua.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        BotonFlotanteTTS(
            activo = ttsActivo,
            onAlternar = {
                GlobalAudioManager.playPop()
                val encendido = TTSManager.alternar(contexto)
                val reto = estado.currentExercise as? LanguageExercise
                if (
                    encendido &&
                    reto != null &&
                    reto.mecanica != MecanicaLengua.ATRAPA_SONIDOS &&
                    reto.mecanica != MecanicaLengua.CODICE_ANIMADO
                ) {
                    TTSManager.leer("${reto.enunciado}. ${reto.consigna}")
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = 14.dp)
        )

        val feedback = estado.feedback
        if (feedback != null) {
            // El premio del Códice Animado vive en el contenido: si el velo
            // fuera tan oscuro como el resto, el jaguar bailarín quedaría
            // apagado justo en el momento más importante.
            val premioCodice = feedback is LessonFeedback.Acierto &&
                (feedback.ejercicio as? LanguageExercise)?.mecanica ==
                MecanicaLengua.CODICE_ANIMADO
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        UniversoProfundo.copy(
                            alpha = if (premioCodice) 0.12f else 0.42f
                        )
                    )
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

        if (estado.isFinished && esFinalDeSaga && !dialogoFinalVisto) {
            DialogoContinuara(
                onVolverAlMapa = {
                    GlobalAudioManager.playPop()
                    dialogoFinalVisto = true
                    onSalir()
                },
                onSeguirAqui = {
                    GlobalAudioManager.playPop()
                    dialogoFinalVisto = true
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  RETO DE OPCIÓN MÚLTIPLE (sílabas, ortografía, gramática, lectura, crítico)
// ---------------------------------------------------------------------------

@Composable
private fun RetoLenguaOpciones(
    ejercicio: LanguageExercise,
    audioApoyo: Boolean,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    var seleccion by remember(ejercicio.id) { mutableIntStateOf(-1) }
    var enviado by remember(ejercicio.id) { mutableStateOf(false) }
    var pistaVisible by remember(ejercicio.id) { mutableStateOf(false) }
    val opciones = remember(ejercicio.id, ejercicio.opciones, ejercicio.indiceCorrecto) {
        LessonEngine.opcionesDe(ejercicio)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = etiquetaMecanica(ejercicio.mecanica),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            color = MagicalPurple
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = ejercicio.titulo,
            fontSize = 20.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
            shadowElevation = if (zen) 0.dp else 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
                Text(
                    text = ejercicio.consigna,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = MagicalPurple
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    BionicText(
                        text = ejercicio.enunciado,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = UniversoProfundo
                    )
                    if (audioApoyo) {
                        Spacer(Modifier.width(8.dp))
                        BotonAltavoz(
                            texto = "${ejercicio.enunciado}. ${ejercicio.consigna}",
                            tamano = 34.dp,
                            colorFondo = LavandaClara
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (pistaVisible) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
            },
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = !enviado, role = Role.Button) {
                    GlobalAudioManager.playPop()
                    pistaVisible = !pistaVisible
                }
                .padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (pistaVisible) "Ocultar pista" else "Ver pista gratis",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (pistaVisible) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (zen) MaterialTheme.colorScheme.surfaceVariant else LavandaClara,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
                ) {
                    BionicText(
                        text = ejercicio.pista,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo
                    )
                    if (audioApoyo) {
                        Spacer(Modifier.width(8.dp))
                        BotonAltavoz(
                            texto = ejercicio.pista,
                            tamano = 30.dp,
                            colorFondo = ZaziliWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        opciones.forEachIndexed { indice, opcion ->
            val elegida = indice == seleccion
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    elegida -> LavandaClara
                    zen -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 2.dp,
                    color = if (elegida) MagicalPurple else ColorTransparente
                ),
                shadowElevation = if (zen) 0.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = !enviado, role = Role.Button) {
                        GlobalAudioManager.playPop()
                        seleccion = indice
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (elegida) MagicalPurple else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letraDe(indice),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (elegida) ZaziliWhite else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    BionicText(
                        text = opcion.text,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
        }

        Spacer(Modifier.height(6.dp))

        ZaziButton(
            text = "Comprobar",
            onClick = {
                if (seleccion < 0 || enviado) return@ZaziButton
                enviado = true
                val acierto = opciones.getOrNull(seleccion)?.isCorrect == true
                onSubmitAnswer(seleccion, acierto)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            containerColor = VerdeDuo,
            contentColor = contenidoDe(VerdeDuo),
            cornerRadius = 20.dp,
            baseHeight = 5.dp,
            enabled = seleccion >= 0 && !enviado,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sin prisa. Puedes leerlo otra vez antes de elegir.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
    }
}

// ---------------------------------------------------------------------------
//  AVISO DE FIN DE SAGA
// ---------------------------------------------------------------------------

@Composable
private fun DialogoContinuara(
    onVolverAlMapa: () -> Unit,
    onSeguirAqui: () -> Unit
) {
    Dialog(
        onDismissRequest = onSeguirAqui,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                KawaiiStarIcon(size = 54.dp, color = AmarilloVibrante)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "¡Continuará...!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MagicalPurple,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                BionicText(
                    text = "Ya dominaste los 50 retos de la Cueva de Leyendas. Zazi está escribiendo el siguiente capítulo con más leyendas, más sílabas y más risas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )
                Spacer(Modifier.height(18.dp))
                ZaziButton(
                    text = "Volver al mapa",
                    onClick = onVolverAlMapa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    containerColor = VerdeDuo,
                    contentColor = contenidoDe(VerdeDuo),
                    cornerRadius = 18.dp,
                    baseHeight = 5.dp,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(9.dp))
                ZaziButton(
                    text = "Seguir aquí",
                    onClick = onSeguirAqui,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    cornerRadius = 18.dp,
                    baseHeight = 4.dp,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  ESTADO SIN RETO
// ---------------------------------------------------------------------------

@Composable
private fun LenguaSinReto(
    onReintentar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        KawaiiZazi(
            size = 126.dp,
            descripcion = "Zazi te espera con otra ronda de Lengua"
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Esta lección se quedó sin retos.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "No pasa nada: Zazi puede prepararte otra ronda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        ZaziButton(
            text = "Reintentar",
            onClick = onReintentar,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            containerColor = VerdeDuo,
            contentColor = contenidoDe(VerdeDuo),
            cornerRadius = 20.dp,
            baseHeight = 5.dp,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(9.dp))
        ZaziButton(
            text = "Volver al mapa",
            onClick = onSalir,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 20.dp,
            baseHeight = 4.dp,
            fontSize = 16.sp
        )
    }
}

// ---------------------------------------------------------------------------
//  UTILIDADES
// ---------------------------------------------------------------------------

private val ColorTransparente = androidx.compose.ui.graphics.Color.Transparent

private fun etiquetaMecanica(mecanica: MecanicaLengua): String = when (mecanica) {
    MecanicaLengua.ATRAPA_SONIDOS -> "CONCIENCIA FONOLÓGICA"
    MecanicaLengua.CODICE_ANIMADO -> "CÓDICE MÁGICO"
    MecanicaLengua.SILABAS -> "SÍLABAS"
    MecanicaLengua.ORTOGRAFIA -> "ORTOGRAFÍA"
    MecanicaLengua.GRAMATICA -> "GRAMÁTICA"
    MecanicaLengua.LECTURA -> "COMPRENSIÓN LECTORA"
    MecanicaLengua.CRITICO -> "PENSAMIENTO CRÍTICO"
}

private fun letraDe(indice: Int): String =
    ('A' + indice.coerceIn(0, 3)).toString()
