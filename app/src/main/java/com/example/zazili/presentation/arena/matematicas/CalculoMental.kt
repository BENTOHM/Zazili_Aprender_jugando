package com.example.zazili.presentation.arena.matematicas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.BotonAltavoz
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val PALETA_OPCIONES = listOf(MintGreen, AzulCielo, AmarilloSuave, LavandaClara)

/**
 * Cálculo Mental de la Arena Cortex.
 *
 * La ecuación se pinta con [BionicText] (anclas visuales para dislexia) y se
 * responde tocando botones 3D flotantes: no hay teclado, no hay campo de texto
 * y no hay reloj. El [ArenaCortexMathViewModel] decide la operación, el número
 * de opciones y el andamiaje; esta pantalla solo dibuja.
 */
@Composable
fun CalculoMentalScreen(
    onSalir: () -> Unit,
    onResultado: (gano: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArenaCortexMathViewModel = viewModel()
) {
    val sesion by viewModel.calculoMental.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    var reportado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.preparar(JuegoMatematico.CALCULO_MENTAL)
    }

    LaunchedEffect(sesion.terminado) {
        if (sesion.terminado && !reportado) {
            reportado = true
            onResultado(sesion.esVictoria)
        }
    }

    if (sesion.terminado) {
        FinalCalculoMental(
            sesion = sesion,
            zen = zen,
            onOtraRonda = {
                GlobalAudioManager.playPop()
                viewModel.reiniciar(JuegoMatematico.CALCULO_MENTAL)
            },
            onSalir = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = modifier
        )
        return
    }

    val reto = sesion.reto as? RetoMath.Calculo

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CabeceraCalculo(sesion = sesion, onSalir = onSalir)

        Spacer(Modifier.height(10.dp))

        BarraAvanceCalculo(sesion = sesion)

        Spacer(Modifier.height(14.dp))

        if (reto == null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Preparando tu reto…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        var seleccion by rememberSaveable(sesion.ronda) { mutableIntStateOf(-1) }
        var verPista by rememberSaveable(sesion.ronda) { mutableStateOf(false) }

        AnimatedContent(
            targetState = reto,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 240)) +
                    slideInVertically(tween(durationMillis = 240)) { alto -> alto / 8 })
                    .togetherWith(fadeOut(tween(durationMillis = 150)))
            },
            label = "tableroCalculo"
        ) { actual ->
            val semilla = actual.hashCode() * 31 + sesion.ronda * 131 + sesion.aciertos
            val escena = remember(actual) { WackyMathContentFactory.escenaCalculo(semilla) }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TarjetaEcuacion(
                    reto = actual,
                    sesion = sesion,
                    zen = zen,
                    escena = escena
                )

                Spacer(Modifier.height(14.dp))

                PizarraOpciones(
                    reto = actual,
                    resultado = sesion.resultado,
                    seleccion = seleccion,
                    mutador = escena.mutador,
                    zen = zen,
                    onElegir = { indice ->
                        if (sesion.resultado == null) {
                            GlobalAudioManager.playPop()
                            seleccion = indice
                            viewModel.responder(JuegoMatematico.CALCULO_MENTAL, indice)
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        BandaRespuesta(
            titulo = sesion.resultado?.titulo,
            detalle = sesion.resultado?.detalle ?: sesion.aviso,
            acierto = sesion.resultado?.acierto
        )

        Spacer(Modifier.height(6.dp))

        PistaCalculo(
            pista = reto.pista,
            visible = verPista || sesion.andamiaje.pistaSiempreVisible,
            onAlternar = {
                GlobalAudioManager.playPop()
                verPista = !verPista
            }
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Sin reloj: puedes contar con los dedos y repetir la operación.",
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
        )

        if (sesion.hayApoyo) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Apoyo activo: ${sesion.andamiaje.etiqueta}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MagicalPurple,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  CABECERA Y AVANCE
// ---------------------------------------------------------------------------

@Composable
private fun CabeceraCalculo(
    sesion: SesionMath,
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
                text = "Cálculo Mental",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Nivel ${sesion.nivel} · ${sesion.fase.etiqueta}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSystemInDarkTheme()) MintGreen else MagicalPurple
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "${sesion.rondasContestadas}/${sesion.totalRondas} · ${sesion.aciertos} ★",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BarraAvanceCalculo(sesion: SesionMath) {
    val fraccion by animateFloatAsState(
        targetValue = sesion.progreso.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280),
        label = "avanceCalculo"
    )
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🧮 Tu ronda",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${sesion.puntos} puntos",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = VerdeDuo
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
                    .background(VerdeDuo)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  ECUACIÓN
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaEcuacion(
    reto: RetoMath.Calculo,
    sesion: SesionMath,
    zen: Boolean,
    escena: EscenaCalculo
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, VerdeDuo.copy(alpha = 0.34f)),
        shadowElevation = if (zen) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MintGreen.copy(alpha = 0.42f)
                ) {
                    Text(
                        text = "SIN RELOJ",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = UniversoProfundo
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LavandaClara
                ) {
                    Text(
                        text = "${escena.mutador.emoji} ${escena.mutador.etiqueta}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Ecuación: ${reto.consigna}"
                    }
            ) {
                BionicText(
                    text = reto.consigna,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 44.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = LavandaClara.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${escena.emoji} ${escena.frase}",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UniversoProfundo,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            BotonAltavoz(
                texto = reto.consigna,
                tamano = 34.dp,
                colorFondo = MintGreen.copy(alpha = 0.40f),
                descripcion = "Escuchar la operación"
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  OPCIONES FLOTANTES
// ---------------------------------------------------------------------------

@Composable
private fun PizarraOpciones(
    reto: RetoMath.Calculo,
    resultado: ResultadoMath?,
    seleccion: Int,
    mutador: MutadorCalculo,
    zen: Boolean,
    onElegir: (Int) -> Unit
) {
    val bloqueado = resultado != null
    val tiempo = remember { mutableFloatStateOf(0f) }

    // Un solo reloj de frames para los cuatro botones. Al bloquearse la
    // respuesta o encenderse el Modo Zen, los botones se quedan quietos.
    LaunchedEffect(mutador.movimiento, bloqueado, zen) {
        val animado = !bloqueado && !zen &&
            mutador.movimiento != MovimientoBotones.QUIETO
        if (!animado) {
            tiempo.floatValue = 0f
            return@LaunchedEffect
        }
        var arranque = 0L
        while (true) {
            withFrameNanos { ahora ->
                if (arranque == 0L) arranque = ahora
                tiempo.floatValue = (ahora - arranque) / 1_000_000_000f
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = "Toca la respuesta",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (mutador.movimiento == MovimientoBotones.QUIETO) {
                    "Sin reloj"
                } else {
                    "¡Síguelos con la mirada!"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }

        reto.opciones.chunked(2).forEachIndexed { fila, textos ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                textos.forEachIndexed { columna, texto ->
                    val indice = fila * 2 + columna
                    val esCorrecta = bloqueado && indice == reto.indiceCorrecto
                    val esFallada = bloqueado && indice == seleccion && !esCorrecta
                    val colorBase = PALETA_OPCIONES[indice % PALETA_OPCIONES.size]
                    val contenedor = when {
                        esCorrecta -> VerdeDuo
                        esFallada -> CoralAviso
                        bloqueado -> MaterialTheme.colorScheme.surfaceVariant
                        else -> colorBase
                    }
                    val contenido = when {
                        esCorrecta -> ZaziliWhite
                        esFallada -> UniversoProfundo
                        bloqueado -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f)
                        else -> contenidoDe(colorBase)
                    }

                    ZaziButton(
                        text = texto,
                        onClick = { onElegir(indice) },
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .graphicsLayer {
                                val t = tiempo.floatValue
                                val fase = indice * 1.7f
                                val amplitud = mutador.amplitudDp.dp.toPx()
                                when (mutador.movimiento) {
                                    MovimientoBotones.ORBITA -> {
                                        translationX = cos(t * 1.7f + fase) * amplitud
                                        translationY = sin(t * 1.15f + fase) * amplitud * 0.55f
                                    }

                                    MovimientoBotones.COHETE -> {
                                        translationX = sin(t * 2.1f + fase) * amplitud
                                        translationY = -abs(sin(t * 1.3f + fase)) * amplitud
                                    }

                                    MovimientoBotones.RESPIRA -> {
                                        val escala = 1f + 0.05f * sin(t * 1.8f + fase)
                                        scaleX = escala
                                        scaleY = escala
                                    }

                                    MovimientoBotones.QUIETO -> Unit
                                }
                            },
                        containerColor = contenedor,
                        contentColor = contenido,
                        cornerRadius = 24.dp,
                        baseHeight = 7.dp,
                        enabled = !bloqueado,
                        fontSize = 26.sp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (esCorrecta) {
                                Text(
                                    text = "✓ ",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = contenido
                                )
                            }
                            Text(
                                text = texto,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = contenido
                            )
                        }
                    }
                }
                if (textos.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  FEEDBACK Y PISTA
// ---------------------------------------------------------------------------

@Composable
private fun BandaRespuesta(
    titulo: String?,
    detalle: String?,
    acierto: Boolean?
) {
    AnimatedVisibility(
        visible = detalle != null,
        enter = fadeIn() + androidx.compose.animation.scaleIn(),
        exit = fadeOut()
    ) {
        val color = when (acierto) {
            true -> MintGreen
            false -> AmarilloSuave
            null -> MaterialTheme.colorScheme.surfaceVariant
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                }
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (titulo != null) {
                    Text(
                        text = titulo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo
                    )
                }
                Text(
                    text = detalle.orEmpty(),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UniversoProfundo.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun PistaCalculo(
    pista: String,
    visible: Boolean,
    onAlternar: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            onClick = onAlternar,
            modifier = Modifier.semantics {
                contentDescription = if (visible) "Ocultar pista" else "Ver pista"
            }
        ) {
            KawaiiSparkIcon(size = 17.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (visible) "Ocultar pista" else "Ver pista",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }

        AnimatedVisibility(visible = visible) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AmarilloSuave.copy(alpha = 0.60f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pista: $pista",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = UniversoProfundo
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  FINAL
// ---------------------------------------------------------------------------

@Composable
private fun FinalCalculoMental(
    sesion: SesionMath,
    zen: Boolean,
    onOtraRonda: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        KawaiiZazi(
            size = 116.dp,
            animar = !zen,
            descripcion = "Zazi celebra el final de tu ronda de cálculo mental"
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "¡Ronda terminada!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${sesion.aciertos} de ${sesion.totalRondas} aciertos · ${sesion.puntos} puntos",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { indice ->
                Text(
                    text = if (indice < sesion.estrellas) "★" else "☆",
                    fontSize = 38.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = if (indice < sesion.estrellas) {
                        AmarilloSuave
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MintGreen.copy(alpha = 0.36f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (sesion.esVictoria) {
                    "¡Tus cuentas están bien firmes! El DDA ya anotó tu avance."
                } else {
                    "Cada intento cuenta. En otra ronda vas con más apoyo y menos prisa."
                },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        ZaziButton(
            text = "Otra ronda",
            onClick = onOtraRonda,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            containerColor = VerdeDuo,
            contentColor = ZaziliWhite,
            cornerRadius = 20.dp,
            baseHeight = 6.dp,
            fontSize = 19.sp
        )

        Spacer(Modifier.height(12.dp))

        ZaziButton(
            text = "Volver a la Arena",
            onClick = onSalir,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 18.dp,
            baseHeight = 5.dp,
            fontSize = 16.sp
        )
    }
}
