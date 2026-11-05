package com.example.zazili.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.ui.KawaiiCenoteIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCloudStressIcon
import com.example.zazili.core.ui.KawaiiHomeIcon
import com.example.zazili.core.ui.KawaiiSeedIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.KawaiiTreeIcon
import com.example.zazili.core.ui.KawaiiVolumeUpIcon
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.diagnostico.DiagnosticEngine
import com.example.zazili.domain.diagnostico.EstadoDiagnostico
import com.example.zazili.domain.diagnostico.FaseDiagnostico
import com.example.zazili.domain.diagnostico.ResultadoDiagnostico
import com.example.zazili.domain.diagnostico.TOTAL_SEMILLAS
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliCreamSoft
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TEXTO_MAPA =
    "Desde la casita morada, camina siete pasos hacia el cenote azul. " +
        "Junto a la ceiba hay una piedra con tres semillas. " +
        "Gira a la izquierda y sigue a las luciérnagas hasta el puente de madera. " +
        "Cruza el puente, sube la loma y busca el árbol de los abrazos. " +
        "Zazi te espera en la rama más alta, donde el cielo se pinta de amarillo."

/**
 * "El Vuelo de Prueba": tres juegos que el niño vive como aventura.
 *
 * La pantalla solo presenta y captura eventos; toda la telemetría y la
 * decisión de la línea base viven en [DiagnosticEngine]. El resultado se
 * guarda fuera (OnboardingNavGraph) y esta pantalla jamás vuelve a mostrarse
 * una vez cerrada la primera compuerta.
 */
@Composable
fun DiagnosticGameScreen(
    nombre: String,
    edad: Int,
    onCompletar: (ResultadoDiagnostico) -> Unit,
    modifier: Modifier = Modifier
) {
    val fabrica = remember(edad) {
        viewModelFactory {
            initializer { DiagnosticViewModel(edad) }
        }
    }
    val diagnosticoViewModel: DiagnosticViewModel = viewModel(factory = fabrica)
    val estado by diagnosticoViewModel.estado.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val completarActual by rememberUpdatedState(onCompletar)

    DisposableEffect(Unit) {
        onDispose { StoryNarratorEngine.detener() }
    }

    LaunchedEffect(estado.fase) {
        scroll.animateScrollTo(0)
    }

    LaunchedEffect(estado.fase, estado.surcoCompleto) {
        if (estado.fase == FaseDiagnostico.SEMILLAS && estado.surcoCompleto) {
            delay(950L)
            diagnosticoViewModel.terminarSemillas()
        }
    }

    LaunchedEffect(estado.resultado) {
        val listo = estado.resultado ?: return@LaunchedEffect
        delay(if (listo.abandono) 1_000L else 1_700L)
        completarActual(listo)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            EncabezadoDiagnostico(nombre = nombre, fase = estado.fase)

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(6.dp))
                when (estado.fase) {
                    FaseDiagnostico.LECTURA -> FaseLectura(
                        audioTocado = estado.audioTocado,
                        onEscuchar = {
                            diagnosticoViewModel.tocarAltavoz()
                            StoryNarratorEngine.narrar(TEXTO_MAPA)
                        },
                        onContinuar = {
                            StoryNarratorEngine.detener()
                            GlobalAudioManager.playPop()
                            diagnosticoViewModel.terminarLectura()
                        }
                    )

                    FaseDiagnostico.SEMILLAS -> FaseSemillas(
                        estado = estado,
                        onSoltar = { cantidad -> diagnosticoViewModel.soltarSemillas(cantidad) }
                    )

                    FaseDiagnostico.VUELO -> FaseVuelo(
                        estado = estado,
                        onDespegar = { GlobalAudioManager.playPop() },
                        onFallo = { diagnosticoViewModel.fallarVuelo() },
                        onReintentar = {
                            GlobalAudioManager.playPop()
                            diagnosticoViewModel.reintentar()
                        },
                        onAbandonar = { diagnosticoViewModel.abandonarVuelo() }
                    )
                }
                Spacer(Modifier.height(22.dp))
            }
        }

        val resultado = estado.resultado
        if (resultado != null) {
            BackHandler { }
            OverlayCierre(resultado = resultado)
        }
    }
}

@Composable
private fun EncabezadoDiagnostico(nombre: String, fase: FaseDiagnostico) {
    val saludo = if (nombre.isBlank()) "¡Hola!" else "¡Hola, ${nombre.trim()}!"
    val paso = fase.ordinal + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.verticalGradient(colors = listOf(MagicalPurple, UniversoProfundo))
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EL VUELO DE PRUEBA",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintGreen,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = saludo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZaziliWhite,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Tres juegos. Sin reloj y sin calificaciones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZaziliWhite.copy(alpha = 0.78f)
                )
            }
            Spacer(Modifier.width(8.dp))
            ZaziSpriteAnimado(
                size = 78.dp,
                estado = ZaziState.VUELO_IDLE,
                animar = true,
                descripcion = "Zazi te acompaña en el vuelo"
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            repeat(3) { indice ->
                val color = when {
                    indice < fase.ordinal -> VerdeDuo
                    indice == fase.ordinal -> AmarilloVibrante
                    else -> ZaziliWhite.copy(alpha = 0.20f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = "Juego $paso de 3 · ${tituloFase(fase)}",
            style = MaterialTheme.typography.labelMedium,
            color = ZaziliWhite.copy(alpha = 0.82f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FaseLectura(
    audioTocado: Boolean,
    onEscuchar: () -> Unit,
    onContinuar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "El mapa de Zazi",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Zazi encontró un mapa para llegar al árbol de los abrazos. Léelo a tu ritmo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            lineHeight = 20.sp
        )

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ZaziliCreamSoft,
            border = BorderStroke(1.dp, AmarilloVibrante.copy(alpha = 0.45f)),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mapa del cenote",
                        style = MaterialTheme.typography.titleMedium,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Pergamino de Zazi",
                        style = MaterialTheme.typography.labelSmall,
                        color = UniversoProfundo.copy(alpha = 0.55f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KawaiiHomeIcon(size = 24.dp)
                    Text("casita", style = MaterialTheme.typography.labelSmall, color = UniversoProfundo.copy(alpha = 0.65f))
                    Text("·", color = UniversoProfundo.copy(alpha = 0.35f))
                    KawaiiCenoteIcon(size = 24.dp)
                    Text("cenote", style = MaterialTheme.typography.labelSmall, color = UniversoProfundo.copy(alpha = 0.65f))
                    Text("·", color = UniversoProfundo.copy(alpha = 0.35f))
                    KawaiiTreeIcon(size = 24.dp)
                    Text("árbol", style = MaterialTheme.typography.labelSmall, color = UniversoProfundo.copy(alpha = 0.65f))
                }

                Spacer(Modifier.height(12.dp))
                ZaziButton(
                    text = "Escuchar el mapa",
                    onClick = onEscuchar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    containerColor = if (audioTocado) VerdeDuo else AmarilloVibrante,
                    contentColor = if (audioTocado) ZaziliWhite else UniversoProfundo,
                    cornerRadius = 18.dp,
                    baseHeight = 5.dp,
                    fontSize = 15.sp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        KawaiiVolumeUpIcon(
                            size = 22.dp,
                            modifier = Modifier
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (audioTocado) "Escuchar otra vez" else "Escuchar el mapa",
                            color = if (audioTocado) ZaziliWhite else UniversoProfundo,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = TEXTO_MAPA,
                    style = MaterialTheme.typography.bodyLarge,
                    color = UniversoProfundo,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Si quieres, Zazi te lo lee con su voz.",
                    style = MaterialTheme.typography.labelSmall,
                    color = UniversoProfundo.copy(alpha = 0.55f)
                )
            }
        }

        ZaziButton(
            text = "¡Ya lo leí, vamos!",
            onClick = onContinuar,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            containerColor = VerdeDuo,
            contentColor = ZaziliWhite,
            cornerRadius = 24.dp,
            baseHeight = 7.dp,
            fontSize = 18.sp
        )
        Text(
            text = "No hay reloj. Puedes releerlo o escucharlo otra vez.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FaseSemillas(
    estado: EstadoDiagnostico,
    onSoltar: (Int) -> Boolean
) {
    var zonaSurco by remember { mutableStateOf<Rect?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Las semillas mágicas",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Lleva las 12 semillas al surco de Zazi. Puedes ir una por una " +
                "o usar las bolsitas si te late más rápido.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            lineHeight = 20.sp
        )

        SurcoSemillas(
            semillasEnSurco = estado.semillasEnSurco,
            completo = estado.surcoCompleto,
            onZonaMedida = { zonaSurco = it }
        )

        if (!estado.surcoCompleto) {
            CanastaSemillas(
                restantes = estado.semillasRestantes,
                zonaSurco = zonaSurco,
                onSoltar = onSoltar
            )
            Text(
                text = "Arrastra una semilla al surco o tócala para mandarla.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                KawaiiStarIcon(size = 30.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "¡Listo! El surco está completo.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SurcoSemillas(
    semillasEnSurco: Int,
    completo: Boolean,
    onZonaMedida: (Rect) -> Unit
) {
    val forma = RoundedCornerShape(26.dp)
    Surface(
        shape = forma,
        color = if (completo) {
            VerdeDuo.copy(alpha = 0.14f)
        } else {
            LavandaClara.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = 2.5.dp,
            color = if (completo) VerdeDuo else MagicalPurple.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordenadas ->
                onZonaMedida(coordenadas.boundsInWindow())
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Surco de Zazi",
                    style = MaterialTheme.typography.titleMedium,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$semillasEnSurco / $TOTAL_SEMILLAS",
                    style = MaterialTheme.typography.titleMedium,
                    color = VerdeDuoOscuro,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(6) { columna ->
                            val indice = fila * 6 + columna
                            val llena = indice < semillasEnSurco
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (llena) VerdeDuo else ZaziliWhite.copy(alpha = 0.72f)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (llena) {
                                            VerdeDuoOscuro.copy(alpha = 0.45f)
                                        } else {
                                            LavandaMedia.copy(alpha = 0.55f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (llena) {
                                    KawaiiCheckIcon(size = 13.dp, color = ZaziliWhite)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text = if (completo) {
                    "¡Las 12 semillas están a salvo!"
                } else {
                    "Suelta aquí tus semillas."
                },
                style = MaterialTheme.typography.labelSmall,
                color = UniversoProfundo.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CanastaSemillas(
    restantes: Int,
    zonaSurco: Rect?,
    onSoltar: (Int) -> Boolean
) {
    val forma = RoundedCornerShape(26.dp)
    val piezas = remember(restantes) { List(restantes) { indice -> indice } }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(forma)
                .background(ZaziliCreamSoft)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, LavandaMedia.copy(alpha = 0.45f), forma)
        )
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tu canasta",
                    style = MaterialTheme.typography.titleMedium,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (restantes == 1) "1 semilla" else "$restantes semillas",
                    style = MaterialTheme.typography.labelMedium,
                    color = UniversoProfundo.copy(alpha = 0.62f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                piezas.chunked(4).forEach { fila ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            fila.forEach { indice ->
                                key(restantes, indice) {
                                    PiezaArrastrable(
                                        cantidad = 1,
                                        descripcion = "Semilla mágica",
                                        zonaSurco = zonaSurco,
                                        onSoltar = onSoltar,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            Color(0xFF8FE04D),
                                                            VerdeDuo
                                                        )
                                                    )
                                                )
                                                .border(
                                                    2.dp,
                                                    VerdeDuoOscuro.copy(alpha = 0.38f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            KawaiiSeedIcon(size = 34.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (restantes >= 2) {
                Spacer(Modifier.height(13.dp))
                Text(
                    text = "O usa una bolsita:",
                    style = MaterialTheme.typography.labelMedium,
                    color = UniversoProfundo.copy(alpha = 0.62f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (restantes >= 10) {
                        key("bolsa", 10) {
                            PiezaArrastrable(
                                cantidad = 10,
                                descripcion = "Bolsa de 10 semillas",
                                zonaSurco = zonaSurco,
                                onSoltar = onSoltar
                            ) {
                                ContenidoBolsa(cantidad = 10)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    if (restantes >= 2) {
                        key("bolsa", 2) {
                            PiezaArrastrable(
                                cantidad = 2,
                                descripcion = "Bolsa de 2 semillas",
                                zonaSurco = zonaSurco,
                                onSoltar = onSoltar
                            ) {
                                ContenidoBolsa(cantidad = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContenidoBolsa(cantidad: Int) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AmarilloSuave,
        border = BorderStroke(1.5.dp, AmarilloVibrante.copy(alpha = 0.55f)),
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
        ) {
            KawaiiSeedIcon(size = 24.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$cantidad",
                style = MaterialTheme.typography.titleLarge,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Pieza con física de arrastre: [Animatable] de desplazamiento, detección por
 * rectángulo global del surco y regreso con muelle a [Offset.Zero]. Un toque
 * directo también la manda al surco (alternativa motriz y para TalkBack).
 */
@Composable
private fun PiezaArrastrable(
    cantidad: Int,
    descripcion: String,
    zonaSurco: Rect?,
    onSoltar: (Int) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val desplazamiento = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val alcance = rememberCoroutineScope()
    val soltarActual by rememberUpdatedState(onSoltar)
    val zonaActual by rememberUpdatedState(zonaSurco)
    var centroBase by remember { mutableStateOf<Offset?>(null) }
    var arrastrando by remember { mutableStateOf(false) }
    val centroActual by rememberUpdatedState(centroBase)

    Box(
        modifier = modifier
            .zIndex(if (arrastrando) 1f else 0f)
            .onGloballyPositioned { coordenadas ->
                centroBase = coordenadas.boundsInWindow().center
            }
            .graphicsLayer {
                translationX = desplazamiento.value.x
                translationY = desplazamiento.value.y
                scaleX = if (arrastrando) 1.08f else 1f
                scaleY = if (arrastrando) 1.08f else 1f
            }
            .semantics(mergeDescendants = true) {
                role = androidx.compose.ui.semantics.Role.Button
                contentDescription = descripcion
                onClick(label = "Mandar al surco") {
                    soltarActual(cantidad)
                    true
                }
            }
            .pointerInput(cantidad) {
                detectTapGestures {
                    if (soltarActual(cantidad)) {
                        GlobalAudioManager.playPop()
                    }
                }
            }
            .pointerInput(cantidad) {
                detectDragGestures(
                    onDragStart = { arrastrando = true },
                    onDragCancel = {
                        arrastrando = false
                        alcance.launch {
                            desplazamiento.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onDragEnd = {
                        arrastrando = false
                        val base = centroActual
                        val zona = zonaActual
                        val destino = if (base != null) base + desplazamiento.value else null
                        val aceptada = zona != null &&
                            destino != null &&
                            zona.contains(destino) &&
                            soltarActual(cantidad)
                        if (aceptada) {
                            GlobalAudioManager.playPop()
                            alcance.launch { desplazamiento.snapTo(Offset.Zero) }
                        } else {
                            alcance.launch {
                                desplazamiento.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    },
                    onDrag = { cambio, avance ->
                        cambio.consume()
                        alcance.launch {
                            desplazamiento.snapTo(desplazamiento.value + avance)
                        }
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
private fun FaseVuelo(
    estado: EstadoDiagnostico,
    onDespegar: () -> Unit,
    onFallo: () -> Unit,
    onReintentar: () -> Unit,
    onAbandonar: () -> Unit
) {
    var despegando by remember { mutableStateOf(false) }
    var despegueId by remember { mutableIntStateOf(0) }

    LaunchedEffect(despegueId) {
        if (despegueId == 0) return@LaunchedEffect
        despegando = true
        delay(1_150L)
        despegando = false
        onFallo()
    }

    val etapa = when {
        estado.falloDeVuelo -> EtapaVuelo.FALLO
        despegando -> EtapaVuelo.RAFAGA
        else -> EtapaVuelo.LISTO
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "El primer vuelo",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Zazi quiere volar contigo. Toca despegar y mira qué pasa.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            lineHeight = 20.sp
        )

        Crossfade(
            targetState = etapa,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "etapaVuelo"
        ) { actual ->
            when (actual) {
                EtapaVuelo.LISTO -> TarjetaDespegue(
                    onDespegar = {
                        if (despegando || estado.falloDeVuelo) return@TarjetaDespegue
                        onDespegar()
                        despegueId++
                    }
                )

                EtapaVuelo.RAFAGA -> TarjetaRafaga()

                EtapaVuelo.FALLO -> TarjetaFallo(
                    onReintentar = onReintentar,
                    onAbandonar = onAbandonar
                )
            }
        }

        Text(
            text = "No hay reloj. Lo importante es intentarlo.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TarjetaDespegue(onDespegar: () -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AzulCielo, LavandaClara, ZaziliCreamSoft)
                    )
                )
        ) {
            KawaiiStarIcon(
                size = 30.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
            )
            KawaiiStarIcon(
                size = 20.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 28.dp, end = 30.dp)
            )
            KawaiiStarIcon(
                size = 16.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 34.dp, end = 18.dp)
            )
            ZaziSpriteAnimado(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 16.dp),
                size = 158.dp,
                estado = ZaziState.VUELO_IDLE,
                animar = true,
                descripcion = "Zazi lista para despegar"
            )
            Text(
                text = "Zazi está lista. ¿Le ayudas a despegar?",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = UniversoProfundo,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(14.dp))
        ZaziButton(
            text = "¡Despegar!",
            onClick = onDespegar,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            containerColor = VerdeDuo,
            contentColor = ZaziliWhite,
            cornerRadius = 24.dp,
            baseHeight = 7.dp,
            fontSize = 19.sp
        )
    }
}

@Composable
private fun TarjetaRafaga() {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = ZaziliCreamSoft,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp)
        ) {
            KawaiiCloudStressIcon(size = 72.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "¡Fsssh!",
                style = MaterialTheme.typography.headlineLarge,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Una ráfaga de viento se cruzó en el camino…",
                style = MaterialTheme.typography.bodyMedium,
                color = UniversoProfundo.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MagicalPurple,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun TarjetaFallo(
    onReintentar: () -> Unit,
    onAbandonar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = ZaziliCreamSoft,
        border = BorderStroke(1.dp, LavandaMedia.copy(alpha = 0.55f)),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            KawaiiCloudStressIcon(size = 66.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "El viento nos ganó esta vez",
                style = MaterialTheme.typography.titleLarge,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "No fue tu culpa. A Zazi también le pasa. ¿Lo intentamos otra vez?",
                style = MaterialTheme.typography.bodyMedium,
                color = UniversoProfundo.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))
            ZaziButton(
                text = "Intentar de nuevo",
                onClick = onReintentar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                cornerRadius = 22.dp,
                baseHeight = 6.dp,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onAbandonar,
                colors = ButtonDefaults.textButtonColors(contentColor = UniversoProfundo)
            ) {
                Text(
                    text = "Ahora no",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "También puedes dejarlo para después.",
                style = MaterialTheme.typography.labelSmall,
                color = UniversoProfundo.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun OverlayCierre(resultado: ResultadoDiagnostico) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UniversoProfundo.copy(alpha = 0.64f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = ZaziliCreamSoft,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                if (resultado.abandono) {
                    KawaiiCloudStressIcon(size = 64.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Está bien. Zazi te espera.",
                        style = MaterialTheme.typography.titleLarge,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Lo dejamos para después, sin prisas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo.copy(alpha = 0.66f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    KawaiiStarIcon(size = 72.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "¡Zazi voló!",
                        style = MaterialTheme.typography.displaySmall,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Gracias por intentarlo otra vez.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = UniversoProfundo.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MagicalPurple,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Preparando tu casita…",
                        style = MaterialTheme.typography.labelMedium,
                        color = UniversoProfundo.copy(alpha = 0.62f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private enum class EtapaVuelo { LISTO, RAFAGA, FALLO }

private fun tituloFase(fase: FaseDiagnostico): String = when (fase) {
    FaseDiagnostico.LECTURA -> "El mapa"
    FaseDiagnostico.SEMILLAS -> "Las semillas"
    FaseDiagnostico.VUELO -> "El vuelo"
}

private class DiagnosticViewModel(edad: Int) : ViewModel() {
    private val motor = DiagnosticEngine(edad)
    val estado = motor.estado

    fun tocarAltavoz() = motor.tocarAltavoz()
    fun terminarLectura() = motor.terminarLectura()
    fun soltarSemillas(cantidad: Int) = motor.soltarSemillas(cantidad)
    fun terminarSemillas() = motor.terminarSemillas()
    fun fallarVuelo() = motor.fallarVuelo()
    fun reintentar() = motor.reintentar()
    fun abandonarVuelo() = motor.abandonarVuelo()

    override fun onCleared() {
        StoryNarratorEngine.detener()
    }
}
