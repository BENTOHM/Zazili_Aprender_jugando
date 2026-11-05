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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.NarradorNativo
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.progress.UserProgressManager
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.curriculum.emociones.MentalHealthCurriculumRepository
import com.example.zazili.domain.emociones.ConsecuenciaOva
import com.example.zazili.domain.emociones.EmocionTormenta
import com.example.zazili.domain.emociones.HerramientaAuxilio
import com.example.zazili.domain.emociones.MecanicaOva
import com.example.zazili.domain.emociones.OVAEngine
import com.example.zazili.domain.emociones.OvaExercise
import com.example.zazili.domain.emociones.ResultadoOva
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lección socioemocional de Zazili 2.0.
 *
 * El motor O.V.A. nunca resta un corazón al primer tropiezo: primero abre una
 * [ConsequenceBottomSheet] que explica por qué esa respuesta no acompañó, y el
 * reto vuelve a la cola. El corazón se descuenta al cerrar la hoja, y solo
 * después de [MentalHealthLessonViewModel.INTENTOS_PARA_CORAZON] intentos.
 *
 * Al terminar el nodo 50 se abre el aviso "¡Continuará...!".
 */
@Composable
fun MentalHealthLessonScreen(
    modulo: ModuloZazili,
    nodoId: String?,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aplicacion = LocalContext.current.applicationContext as Application
    val viewModel: MentalHealthLessonViewModel = viewModel(
        key = "emociones_${nodoId.orEmpty()}",
        factory = remember(modulo, nodoId, aplicacion) {
            MentalHealthLessonViewModel.factory(aplicacion, nodoId)
        }
    )
    val estado by viewModel.state.collectAsStateWithLifecycle()
    val zen = collectZenMode()

    val esFinalDeSaga = nodoId == MentalHealthCurriculumRepository.ULTIMO_NODO_ID ||
        estado.esUltimaLeccion
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
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.50f)
            )
        )
    }

    val progresoAnimado by animateFloatAsState(
        targetValue = estado.progress,
        animationSpec = tween(
            durationMillis = if (zen) 0 else 420,
            easing = FastOutSlowInEasing
        ),
        label = "progresoEmociones"
    )

    BackHandler(
        enabled = estado.mostrarTeoria ||
            estado.consecuencia != null ||
            estado.exito != null
    ) {
        if (estado.mostrarTeoria) viewModel.cerrarTeoria()
    }

    val retoActual = estado.currentExercise
    LaunchedEffect(
        retoActual?.id,
        estado.roundToken,
        estado.audioApoyo,
        estado.mostrarTeoria,
        estado.consecuencia,
        estado.exito
    ) {
        val reto = retoActual
        val bloqueado = estado.mostrarTeoria ||
            estado.consecuencia != null ||
            estado.exito != null
        if (bloqueado) {
            NarradorNativo.detener()
            return@LaunchedEffect
        }
        if (
            reto != null &&
            estado.audioApoyo &&
            reto.mecanica != MecanicaOva.MEDIDOR_TORMENTAS
        ) {
            NarradorNativo.hablar("${reto.escenario}. ${reto.observacion}")
        }
    }
    DisposableEffect(Unit) {
        onDispose { NarradorNativo.detener() }
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
                when {
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

                    retoActual == null -> EmocionSinReto(
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

                    else -> key(retoActual.id, estado.roundToken) {
                        if (retoActual.mecanica == MecanicaOva.MEDIDOR_TORMENTAS) {
                            MedidorTormentasScreen(
                                exercise = retoActual,
                                onElegirHerramienta = viewModel::elegirHerramienta,
                                activo = !estado.mostrarTeoria &&
                                    estado.consecuencia == null &&
                                    estado.exito == null,
                                audioApoyo = estado.audioApoyo,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            RetoOvaOpciones(
                                ejercicio = retoActual,
                                titulo = estado.titulo,
                                subtitulo = estado.subtitulo,
                                audioApoyo = estado.audioApoyo && !estado.mostrarTeoria,
                                bloqueado = estado.consecuencia != null ||
                                    estado.exito != null,
                                pistaVisible = estado.pistaVisible,
                                onAlternarPista = viewModel::alternarPista,
                                onElegir = viewModel::elegirOpcion,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        val consecuencia = estado.consecuencia
        if (consecuencia != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(UniversoProfundo.copy(alpha = 0.42f))
                    .pointerInput(Unit) { detectTapGestures { } }
            )
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                ConsequenceBottomSheet(
                    consecuencia = consecuencia,
                    onEntendido = {
                        GlobalAudioManager.playPop()
                        viewModel.cerrarConsecuencia()
                    }
                )
            }
        }

        val exito = estado.exito
        if (exito != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(UniversoProfundo.copy(alpha = 0.38f))
                    .pointerInput(Unit) { detectTapGestures { } }
            )
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                HojaExitoOva(
                    exito = exito,
                    onContinuar = {
                        GlobalAudioManager.playPop()
                        viewModel.continuarExito()
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
            DialogoContinuaraEmociones(
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
//  RETO O.V.A. DE OPCIÓN MÚLTIPLE
// ---------------------------------------------------------------------------

@Composable
private fun RetoOvaOpciones(
    ejercicio: OvaExercise,
    titulo: String,
    subtitulo: String,
    audioApoyo: Boolean,
    bloqueado: Boolean,
    pistaVisible: Boolean,
    onAlternarPista: () -> Unit,
    onElegir: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    var seleccion by remember(ejercicio.id) { mutableIntStateOf(-1) }
    var enviado by remember(ejercicio.id) { mutableStateOf(false) }
    val habilitado = !bloqueado && !enviado

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = "OBSERVAR · VALIDAR · ACTUAR",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp,
            color = MagicalPurple
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = titulo,
            fontSize = 20.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitulo,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))

        FasesOva()
        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (zen) MaterialTheme.colorScheme.surfaceVariant else SoftCream,
            shadowElevation = if (zen) 0.dp else 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                Text(
                    text = "OBSERVA SU CUERPO Y SU CARA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.3.sp,
                    color = MagicalPurple
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.Top) {
                    BionicText(
                        text = ejercicio.observacion,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo
                    )
                    if (audioApoyo) {
                        Spacer(Modifier.width(7.dp))
                        BotonAltavoz(
                            texto = ejercicio.observacion,
                            tamano = 30.dp,
                            colorFondo = LavandaClara
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (zen) {
                MaterialTheme.colorScheme.surface
            } else {
                LavandaClara.copy(alpha = 0.85f)
            },
            shadowElevation = if (zen) 0.dp else 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                Text(
                    text = "ESCUCHA EL MOMENTO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.3.sp,
                    color = MagicalPurple
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.Top) {
                    BionicText(
                        text = ejercicio.escenario,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = UniversoProfundo
                    )
                    if (audioApoyo) {
                        Spacer(Modifier.width(7.dp))
                        BotonAltavoz(
                            texto = "${ejercicio.escenario}. ${ejercicio.observacion}",
                            tamano = 32.dp,
                            colorFondo = ZaziliWhite.copy(alpha = 0.72f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (pistaVisible) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
            },
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = habilitado, role = Role.Button) {
                    GlobalAudioManager.playPop()
                    onAlternarPista()
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
                color = if (zen) MaterialTheme.colorScheme.surfaceVariant else MintGreen.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                ) {
                    BionicText(
                        text = ejercicio.pista,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo
                    )
                    if (audioApoyo) {
                        Spacer(Modifier.width(7.dp))
                        BotonAltavoz(
                            texto = ejercicio.pista,
                            tamano = 28.dp,
                            colorFondo = ZaziliWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(13.dp))
        Text(
            text = "¿Qué respuesta acompaña?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(9.dp))

        ejercicio.opciones.forEachIndexed { indice, opcion ->
            val elegida = indice == seleccion
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (elegida) {
                    LavandaClara
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = 2.dp,
                    color = if (elegida) MagicalPurple else Color.Transparent
                ),
                shadowElevation = if (zen) 0.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = habilitado, role = Role.Button) {
                        GlobalAudioManager.playPop()
                        seleccion = indice
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (elegida) {
                                    MagicalPurple
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letraOva(indice),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (elegida) {
                                ZaziliWhite
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    BionicText(
                        text = opcion.texto,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (audioApoyo) {
                        Spacer(Modifier.width(7.dp))
                        BotonAltavoz(
                            texto = opcion.texto,
                            tamano = 28.dp,
                            colorFondo = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(4.dp))

        ZaziButton(
            text = "Comprobar",
            onClick = {
                if (!habilitado || seleccion < 0) return@ZaziButton
                enviado = true
                onElegir(seleccion)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            containerColor = VerdeDuo,
            contentColor = contenidoDe(VerdeDuo),
            cornerRadius = 20.dp,
            baseHeight = 5.dp,
            enabled = habilitado && seleccion >= 0,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sin prisa. Validar también se practica.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun FasesOva() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FaseOvaChip(
            texto = "1 · Observar",
            fondo = MintGreen,
            color = UniversoProfundo,
            modifier = Modifier.weight(1f)
        )
        FaseOvaChip(
            texto = "2 · Validar",
            fondo = MagicalPurple,
            color = ZaziliWhite,
            modifier = Modifier.weight(1f)
        )
        FaseOvaChip(
            texto = "3 · Actuar",
            fondo = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FaseOvaChip(
    texto: String,
    fondo: Color,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = fondo,
        modifier = modifier
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
//  HOJAS DE RETROALIMENTACIÓN
// ---------------------------------------------------------------------------

@Composable
private fun ConsequenceBottomSheet(
    consecuencia: ConsecuenciaOva,
    onEntendido: () -> Unit
) {
    val zen = collectZenMode()
    val fondo = if (zen) MaterialTheme.colorScheme.surface else LavandaClara
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
                .verticalScroll(rememberScrollState())
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
                        .background(RosaSuave.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌧️", fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = consecuencia.titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Black,
                        lineHeight = 25.sp
                    )
                    Text(
                        text = "Nadie nace sabiendo acompañar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UniversoProfundo.copy(alpha = 0.66f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            BionicText(
                text = consecuencia.mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = UniversoProfundo.copy(alpha = 0.88f)
            )

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZaziliWhite.copy(alpha = 0.58f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "LO QUE SÍ ACOMPAÑA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MagicalPurple
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "«${consecuencia.validacionModelo}»",
                        style = MaterialTheme.typography.titleSmall,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZaziliWhite.copy(alpha = 0.42f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "UN PASO CONCRETO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = MagicalPurple
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = consecuencia.accionModelo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (consecuencia.corazonPerdido) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Este corazón se va a descansar. Puedes recuperarlo respirando con Zazi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UniversoProfundo.copy(alpha = 0.76f),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(16.dp))

            ZaziButton(
                text = "Entendido, sigo",
                onClick = onEntendido,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                containerColor = MagicalPurple,
                contentColor = ZaziliWhite,
                cornerRadius = 20.dp,
                baseHeight = 5.dp,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "El reto te espera otra vez, sin regaños.",
                style = MaterialTheme.typography.labelSmall,
                color = UniversoProfundo.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HojaExitoOva(
    exito: ExitoOva,
    onContinuar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = VerdeDuo,
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
                    KawaiiCheckIcon(size = 27.dp, color = UniversoProfundo)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡Qué chido!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Nombraste lo que siente y lo acompañaste.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo.copy(alpha = 0.78f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZaziliWhite.copy(alpha = 0.52f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "LO QUE DIJISTE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = VerdeDuoOscuro
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "«${exito.validacion}»",
                        style = MaterialTheme.typography.titleSmall,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZaziliWhite.copy(alpha = 0.40f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = "PASO CONCRETO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = VerdeDuoOscuro
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = exito.accion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.SemiBold
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
                contentColor = VerdeDuoOscuro,
                shadowColor = UniversoProfundo.copy(alpha = 0.28f),
                cornerRadius = 20.dp,
                baseHeight = 5.dp,
                fontSize = 17.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  AVISO DE FIN DE SAGA
// ---------------------------------------------------------------------------

@Composable
private fun DialogoContinuaraEmociones(
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
                    text = "Acompañaste las 50 emociones del Árbol de los Abrazos. " +
                        "Zazi está tejiendo nuevas historias sobre calma, amistad y valentía. " +
                        "Mientras tanto, puedes repetir cualquier reto y practicar tus herramientas.",
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
private fun EmocionSinReto(
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
            descripcion = "Zazi te espera con otra ronda de emociones"
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

private fun letraOva(indice: Int): String =
    ('A' + indice.coerceIn(0, 2)).toString()

// ---------------------------------------------------------------------------
//  MOTOR DE LECCIÓN EMOCIONAL (O.V.A.)
// ---------------------------------------------------------------------------

@Immutable
data class ExitoOva(
    val validacion: String,
    val accion: String
)

@Immutable
data class MentalHealthLessonState(
    val isLoading: Boolean = true,
    val currentExercise: OvaExercise? = null,
    val hearts: Int = LessonViewModel.VIDAS_INICIALES,
    val progress: Float = 0f,
    val totalExercises: Int = 0,
    val solvedExercises: Int = 0,
    val consecuencia: ConsecuenciaOva? = null,
    val exito: ExitoOva? = null,
    val isBreathing: Boolean = false,
    val isFinished: Boolean = false,
    val stars: Int = 0,
    val roundToken: Int = 0,
    val teoria: TheoryContent? = null,
    val mostrarTeoria: Boolean = false,
    val pistaVisible: Boolean = false,
    val audioApoyo: Boolean = false,
    val titulo: String = "El Árbol de los Abrazos",
    val subtitulo: String = "Emociones",
    /** Nodo que se abrirá al tocar «¡Siguiente Lección!»; `null` si no hay. */
    val siguienteNodoId: String? = null,
    /** En la última lección del mapa se oculta «¡Siguiente Lección!». */
    val esUltimaLeccion: Boolean = false
)

@Immutable
private data class ContenidoEmocional(
    val ejercicios: List<OvaExercise>,
    val teoria: TheoryContent?,
    val titulo: String,
    val subtitulo: String
)

/**
 * Destino de la pantalla de victoria emocional. [numeroActual] es el escalón del
 * módulo que se acaba de completar y [esUltima] apaga «¡Siguiente Lección!».
 */
@Immutable
private data class PlanEmocional(
    val numeroActual: Int,
    val nodoIdSiguiente: String?,
    val esUltima: Boolean
)

/**
 * Cola de escenarios O.V.A. con corazones tardíos.
 *
 * Un tropiezo abre la consecuencia y devuelve el reto al final de la cola sin
 * tocar los corazones. Al cerrar la hoja, si el reto acumuló tres tropiezos,
 * el corazón se descuenta y, si llega a cero, Zazi guía una respiración.
 */
class MentalHealthLessonViewModel(
    application: Application,
    private var nodoId: String?
) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container
    private val progresoUsuario = UserProgressManager.obtener(application)
    private val cola = ArrayDeque<OvaExercise>()
    private val motor = OVAEngine()
    private val fallosPorReto = mutableMapOf<String, Int>()

    private var total = 0
    private var resueltos = 0
    private var errores = 0
    private var audioApoyo = false
    private var trabajo: Job? = null
    private var generacion = 0

    private val _state = MutableStateFlow(MentalHealthLessonState())
    val state: StateFlow<MentalHealthLessonState> = _state.asStateFlow()

    init {
        configurarApoyoAudio()
        cargar()
    }

    private fun configurarApoyoAudio() {
        viewModelScope.launch {
            val perfil = contenedor.userManager.perfil
                .filterNotNull()
                .firstOrNull()
                ?: return@launch
            if (perfil.apoyoAudio) {
                StoryNarratorEngine.setHabilitado(true)
                audioApoyo = true
                _state.update { it.copy(audioApoyo = true) }
            }
        }
    }

    @Synchronized
    fun elegirOpcion(indice: Int) {
        val actual = _state.value
        if (!puedeJugar(actual)) return
        val reto = actual.currentExercise ?: return
        if (indice !in reto.opciones.indices) return
        motor.observar()
        resolver(motor.validar(indice), reto)
    }

    @Synchronized
    fun elegirHerramienta(herramienta: HerramientaAuxilio) {
        val actual = _state.value
        if (!puedeJugar(actual)) return
        val reto = actual.currentExercise ?: return
        motor.observar()
        resolver(motor.actuar(herramienta), reto)
    }

    @Synchronized
    fun continuarExito() {
        val actual = _state.value
        if (actual.exito == null || actual.isBreathing || actual.isFinished) return
        if (actual.solvedExercises >= actual.totalExercises || cola.isEmpty()) {
            finalizar()
        } else {
            avanzar()
        }
    }

    /**
     * Cierra la explicación suave. El corazón se descuenta AQUÍ, nunca al
     * elegir la respuesta; después el reto regresa al final de la cola.
     */
    @Synchronized
    fun cerrarConsecuencia() {
        val actual = _state.value
        val consecuencia = actual.consecuencia ?: return
        val reto = actual.currentExercise ?: return

        val corazones = if (consecuencia.corazonPerdido) {
            (actual.hearts - 1).coerceAtLeast(0)
        } else {
            actual.hearts
        }

        if (cola.isNotEmpty()) {
            cola.removeFirst()
            cola.addLast(reto)
        }

        if (corazones <= 0) {
            _state.update {
                it.copy(
                    consecuencia = null,
                    hearts = 0,
                    isBreathing = true,
                    pistaVisible = false,
                    roundToken = it.roundToken + 1
                )
            }
            return
        }

        val siguiente = cola.firstOrNull()
        motor.iniciar(siguiente)
        _state.update {
            it.copy(
                consecuencia = null,
                hearts = corazones,
                currentExercise = siguiente,
                pistaVisible = false,
                roundToken = it.roundToken + 1
            )
        }
    }

    @Synchronized
    fun completarRespiracion() {
        val actual = _state.value
        if (!actual.isBreathing) return
        val siguiente = cola.firstOrNull()
        motor.iniciar(siguiente)
        _state.update {
            it.copy(
                isBreathing = false,
                hearts = (it.hearts + 1).coerceAtMost(LessonViewModel.VIDAS_INICIALES),
                currentExercise = siguiente,
                consecuencia = null,
                exito = null,
                pistaVisible = false,
                roundToken = it.roundToken + 1
            )
        }
    }

    @Synchronized
    fun alternarPista() {
        val actual = _state.value
        if (!puedeJugar(actual)) return
        _state.update { it.copy(pistaVisible = !it.pistaVisible) }
    }

    @Synchronized
    fun cerrarPista() {
        _state.update {
            if (it.pistaVisible) it.copy(pistaVisible = false) else it
        }
    }

    @Synchronized
    fun cerrarTeoria() {
        _state.update {
            if (it.mostrarTeoria) {
                it.copy(mostrarTeoria = false, roundToken = it.roundToken + 1)
            } else {
                it
            }
        }
    }

    fun reiniciar() {
        trabajo?.cancel()
        cargar()
    }

    /**
     * Abre la lección emocional que sigue sin salir de la pantalla. El
     * desbloqueo ya quedó en DataStore al completar la anterior; aquí solo se
     * cambia el nodo y se reconstruye la cola O.V.A.
     */
    @Synchronized
    fun siguienteLeccion() {
        val actual = _state.value
        if (!actual.isFinished) return
        val siguiente = actual.siguienteNodoId ?: return
        nodoId = siguiente
        cargar()
    }

    private fun resolver(resultado: ResultadoOva, reto: OvaExercise) {
        if (resultado.correcto) {
            GlobalAudioManager.playSuccess()
            if (cola.isNotEmpty()) cola.removeFirst()
            resueltos++
            fallosPorReto.remove(reto.id)
            _state.update { estado ->
                estado.copy(
                    solvedExercises = resueltos,
                    progress = if (estado.totalExercises == 0) {
                        0f
                    } else {
                        resueltos.toFloat() / estado.totalExercises
                    },
                    exito = ExitoOva(
                        validacion = reto.validacion,
                        accion = reto.accionModelo
                    ),
                    consecuencia = null,
                    pistaVisible = false
                )
            }
            return
        }

        GlobalAudioManager.playMiss()
        errores++
        val fallos = (fallosPorReto[reto.id] ?: 0) + 1
        fallosPorReto[reto.id] = fallos
        val corazonPerdido = fallos >= INTENTOS_PARA_CORAZON
        if (corazonPerdido) fallosPorReto.remove(reto.id)
        val consecuencia = resultado.consecuencia
            ?.copy(corazonPerdido = corazonPerdido)
            ?: OVAEngine.consecuencia(reto, resultado.tipo, corazonPerdido)
        _state.update {
            it.copy(
                consecuencia = consecuencia,
                exito = null,
                pistaVisible = false
            )
        }
    }

    private fun avanzar() {
        if (cola.isEmpty()) {
            finalizar()
            return
        }
        val siguiente = cola.firstOrNull()
        motor.iniciar(siguiente)
        _state.update {
            it.copy(
                currentExercise = siguiente,
                exito = null,
                consecuencia = null,
                pistaVisible = false,
                roundToken = it.roundToken + 1
            )
        }
    }

    private fun finalizar() {
        val estrellas = when {
            errores == 0 -> 3
            errores <= 2 -> 2
            else -> 1
        }
        val leccion = MentalHealthCurriculumRepository.leccionPorId(nodoId)
        if (leccion != null) {
            contenedor.progresoSaga.completar(leccion.id, estrellas)
            // Chequeo duro del módulo 5: terminar la lección N abre la N+1 en
            // DataStore, sin preguntar si el currículo tiene un nodo siguiente.
            // El guardado va aparte y nunca retrasa las estrellas.
            progresoUsuario.unlockNextLessonEnSegundoPlano(
                LessonEngine.MODULO_EMOCIONES_ID,
                leccion.numero + 1
            )
        }

        val planEmocional = planDeSiguiente()

        _state.update {
            it.copy(
                currentExercise = null,
                consecuencia = null,
                exito = null,
                isBreathing = false,
                isFinished = true,
                progress = 1f,
                stars = estrellas,
                pistaVisible = false,
                siguienteNodoId = planEmocional?.nodoIdSiguiente,
                esUltimaLeccion = planEmocional?.esUltima ?: false,
                roundToken = it.roundToken + 1
            )
        }
    }

    /**
     * La siguiente lección se resuelve por [LeccionEmocional.numero], nunca por
     * texto del id: un nodo de gimnasio no tiene número y por eso no ofrece
     * siguiente.
     */
    private fun planDeSiguiente(): PlanEmocional? {
        val actual = MentalHealthCurriculumRepository.leccionPorId(nodoId) ?: return null
        val siguiente = MentalHealthCurriculumRepository.lecciones
            .firstOrNull { it.numero == actual.numero + 1 }
        return PlanEmocional(
            numeroActual = actual.numero,
            nodoIdSiguiente = siguiente?.id,
            esUltima = MentalHealthCurriculumRepository.lecciones
                .none { it.numero > actual.numero }
        )
    }

    private fun cargar() {
        trabajo?.cancel()
        val token = ++generacion
        cola.clear()
        total = 0
        resueltos = 0
        errores = 0
        fallosPorReto.clear()
        _state.value = MentalHealthLessonState(
            isLoading = true,
            hearts = LessonViewModel.VIDAS_INICIALES,
            roundToken = _state.value.roundToken + 1,
            audioApoyo = audioApoyo
        )

        trabajo = viewModelScope.launch {
            val contenido = withContext(Dispatchers.Default) {
                runCatching { construirContenido() }
                    .getOrElse {
                        val respaldo = MentalHealthCurriculumRepository.lecciones.firstOrNull()
                        ContenidoEmocional(
                            ejercicios = respaldo?.preguntas.orEmpty(),
                            teoria = null,
                            titulo = respaldo?.titulo ?: "El Árbol de los Abrazos",
                            subtitulo = "Emociones"
                        )
                    }
            }
            if (token != generacion) return@launch

            cola.addAll(contenido.ejercicios)
            total = cola.size
            motor.iniciar(cola.firstOrNull())
            _state.value = MentalHealthLessonState(
                isLoading = false,
                currentExercise = cola.firstOrNull(),
                hearts = LessonViewModel.VIDAS_INICIALES,
                progress = 0f,
                totalExercises = total,
                solvedExercises = 0,
                teoria = contenido.teoria,
                mostrarTeoria = contenido.teoria != null,
                roundToken = _state.value.roundToken + 1,
                audioApoyo = audioApoyo,
                titulo = contenido.titulo,
                subtitulo = contenido.subtitulo
            )
        }
    }

    private fun puedeJugar(estado: MentalHealthLessonState): Boolean =
        !estado.isLoading &&
            !estado.isFinished &&
            !estado.isBreathing &&
            !estado.mostrarTeoria &&
            estado.consecuencia == null &&
            estado.exito == null &&
            estado.currentExercise != null

    /**
     * Un nodo del mapa usa su lección de tres escenarios y su cápsula de
     * teoría. Los minijuegos de la Arena Cortex toman prestados escenarios
     * según su tema; medidor_tormentas usa los retos de arrastre.
     */
    private fun construirContenido(): ContenidoEmocional {
        val juego = Minijuego.porId(nodoId.orEmpty())
            ?.takeIf { it.modulo.moduloZazili == ModuloZazili.ARBOL_ABRAZOS }
        if (juego != null) {
            return ContenidoEmocional(
                ejercicios = construirGimnasio(juego),
                teoria = null,
                titulo = juego.titulo,
                subtitulo = "Arena Cortex · ${juego.mecanica}"
            )
        }

        val leccion = MentalHealthCurriculumRepository.leccionPorId(nodoId)
            ?: MentalHealthCurriculumRepository.lecciones.firstOrNull()
            ?: return ContenidoEmocional(
                ejercicios = emptyList(),
                teoria = null,
                titulo = "El Árbol de los Abrazos",
                subtitulo = "Emociones"
            )
        val capitulo = MentalHealthCurriculumRepository.capituloPorNumero(leccion.capitulo)
        return ContenidoEmocional(
            ejercicios = leccion.preguntas,
            teoria = TheoryContent(
                id = leccion.id,
                titulo = leccion.titulo,
                subtitulo = "Capítulo ${leccion.capitulo} · ${leccion.subtitulo}",
                icono = leccion.icono,
                bullets = leccion.teoria
            ),
            titulo = leccion.titulo,
            subtitulo = capitulo?.titulo ?: "Emociones"
        )
    }

    private fun construirGimnasio(juego: Minijuego): List<OvaExercise> {
        val todos = MentalHealthCurriculumRepository.lecciones.flatMap { it.preguntas }
        val filtrados = when (juego.id) {
            Minijuego.MedidorTormentas.id ->
                todos.filter { it.mecanica == MecanicaOva.MEDIDOR_TORMENTAS }

            Minijuego.CaminoJaguar.id ->
                MentalHealthCurriculumRepository.leccionesDe(5).flatMap { it.preguntas }

            Minijuego.EspejoFortalezas.id ->
                MentalHealthCurriculumRepository.leccionesDe(4).flatMap { it.preguntas }

            Minijuego.SombraMiedo.id ->
                MentalHealthCurriculumRepository.leccionesDe(1).flatMap { it.preguntas }

            else -> todos
        }
        val banco = filtrados.ifEmpty { todos }

        // El prototipo de arrastre debe mostrar la nube roja del enojo y la
        // azul de la tristeza, además de una tercera emoción distinta.
        if (juego.id == Minijuego.MedidorTormentas.id) {
            val preferidos = listOf(
                EmocionTormenta.ENOJO,
                EmocionTormenta.TRISTEZA,
                EmocionTormenta.ANSIEDAD
            ).mapNotNull { emocion ->
                banco.firstOrNull { reto -> reto.emocion == emocion }
            }
            if (preferidos.size == RETOS_GYM) return preferidos
        }

        return when {
            banco.size <= RETOS_GYM -> banco
            else -> listOf(banco.first(), banco[banco.size / 2], banco.last())
        }
    }

    companion object {

        /** Tres tropiezos en el mismo reto antes de descontar un corazón. */
        const val INTENTOS_PARA_CORAZON = 3

        private const val RETOS_GYM = 3

        fun factory(
            application: Application,
            nodoId: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MentalHealthLessonViewModel(application, nodoId)
            }
        }
    }
}
