package com.example.zazili.presentation.arena.matematicas

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

private val PALETA_BARRAS = listOf(MintGreen, AzulCielo, AmarilloSuave, RosaSuave, LavandaClara)
private const val ALTO_GRAFICA_DP = 196

/**
 * Estadística Alebrije — lectura de datos sobre un Canvas propio.
 *
 * [WackyMathContentFactory] decide el estilo del pictograma (luchadores
 * apilados, chiles en nogada o alfeñiques) y, a veces, esconde una barra con el
 * ala de un alebrije. En ese caso el panel muestra el total de votos y el niño
 * deduce el dato que falta; la barra tapada nunca es la que responde la
 * pregunta. La gráfica se dibuja con [androidx.compose.foundation.Canvas]:
 * trama punteada, eje base, un pictograma por voto y, desde el nivel 3 del DDA,
 * el valor numérico sobre cada columna. Todas las medidas son fracciones del
 * tamaño del Canvas, así que se adapta a cualquier pantalla.
 *
 * La pregunta cambia con el nivel: la fruta más votada, la menos votada, leer
 * una barra, la diferencia entre dos barras o el total. Se responde con botones
 * 3D apilados; la opción tocada se tiñe de verde o de coral. Sin teclado y sin
 * reloj.
 */
@Composable
fun EstadisticaAlebrijeScreen(
    onSalir: () -> Unit,
    onResultado: (gano: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArenaCortexMathViewModel = viewModel()
) {
    val sesion by viewModel.estadisticaAlebrije.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    var reportado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.preparar(JuegoMatematico.ESTADISTICA_ALEBRIJE)
    }

    LaunchedEffect(sesion.terminado) {
        if (sesion.terminado && !reportado) {
            reportado = true
            onResultado(sesion.esVictoria)
        }
    }

    if (sesion.terminado) {
        FinalEstadistica(
            sesion = sesion,
            zen = zen,
            onOtraRonda = {
                GlobalAudioManager.playPop()
                viewModel.reiniciar(JuegoMatematico.ESTADISTICA_ALEBRIJE)
            },
            onSalir = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = modifier
        )
        return
    }

    val reto = sesion.reto as? RetoMath.Estadistica

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CabeceraEstadistica(sesion = sesion, onSalir = onSalir)

        Spacer(Modifier.height(8.dp))

        BarraRondaEstadistica(sesion = sesion)

        Spacer(Modifier.height(10.dp))

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
                        text = "Contando votos de los alebrijes…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        var seleccion by rememberSaveable(sesion.ronda) { mutableIntStateOf(-1) }

        AnimatedContent(
            targetState = reto,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 240)) +
                    slideInVertically(tween(durationMillis = 240)) { alto -> alto / 10 })
                    .togetherWith(fadeOut(tween(durationMillis = 150)))
            },
            label = "tableroEstadistica"
        ) { actual ->
            val semilla = actual.hashCode() * 31 + sesion.ronda * 131 + sesion.aciertos
            val tema = remember(actual) {
                WackyMathContentFactory.temaEstadistica(
                    semilla = semilla,
                    barras = actual.barras,
                    pregunta = actual.pregunta
                )
            }

            PanelBarras(reto = actual, tema = tema, sesion = sesion, zen = zen)
        }

        Spacer(Modifier.height(12.dp))

        TarjetaPregunta(reto = reto, sesion = sesion)

        Spacer(Modifier.height(12.dp))

        BotonesAlebrije(
            reto = reto,
            resultado = sesion.resultado,
            seleccion = seleccion,
            onElegir = { indice ->
                if (sesion.resultado == null) {
                    GlobalAudioManager.playPop()
                    seleccion = indice
                    viewModel.responder(JuegoMatematico.ESTADISTICA_ALEBRIJE, indice)
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        BandaDatos(
            titulo = sesion.resultado?.titulo,
            detalle = sesion.resultado?.detalle ?: sesion.aviso,
            acierto = sesion.resultado?.acierto
        )

        Spacer(Modifier.height(4.dp))

        if (sesion.andamiaje.pistaSiempreVisible) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AmarilloSuave.copy(alpha = 0.60f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pista: ${reto.pista}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    color = UniversoProfundo
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        Text(
            text = if (reto.muestraValores) {
                "Los números sobre las barras ya están a la vista. Lee con calma."
            } else {
                "Compara la altura de los pictogramas. Si un alebrije tapa una barra, " +
                    "dedúcela con el total."
            },
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
        )
    }
}

// ---------------------------------------------------------------------------
//  CABECERA Y AVANCE
// ---------------------------------------------------------------------------

@Composable
private fun CabeceraEstadistica(
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
                text = "Estadística Alebrije",
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
private fun BarraRondaEstadistica(sesion: SesionMath) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "📊 Tu ronda",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${sesion.puntos} puntos",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AmarilloOscuro
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
                    .fillMaxWidth(sesion.progreso.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MagicalPurple)
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  GRÁFICA DE BARRAS EN CANVAS
// ---------------------------------------------------------------------------

@Composable
private fun PanelBarras(
    reto: RetoMath.Estadistica,
    tema: TemaEstadistica,
    sesion: SesionMath,
    zen: Boolean
) {
    val maximo = remember(reto) { reto.barras.maxOf { it.valor }.coerceAtLeast(1) }
    val destacada = remember(reto, sesion.resaltar) {
        if (sesion.resaltar) reto.opciones.getOrNull(reto.indiceCorrecto) else null
    }
    val colorValor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f).toArgb()
    val tintaEje = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.34f)
    val trama = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
    val pincel = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
    }

    var crecer by remember(reto) { mutableStateOf(zen) }
    LaunchedEffect(reto, zen) {
        if (!zen) crecer = true
    }
    val crecimiento by animateFloatAsState(
        targetValue = if (crecer) 1f else 0f,
        animationSpec = tween(durationMillis = 460),
        label = "crecimientoBarras"
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (sesion.hayApoyo) 2.5.dp else 1.dp,
            if (sesion.hayApoyo) MagicalPurple.copy(alpha = 0.55f) else
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f)
        ),
        shadowElevation = if (zen) 0.dp else 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "📊", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = reto.titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tema.emojiMutador} ${tema.etiquetaMutador}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BotonAltavoz(
                    texto = "${reto.pregunta} ${reto.pista}",
                    tamano = 30.dp,
                    colorFondo = LavandaClara,
                    descripcion = "Escuchar la pregunta de la gráfica"
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ALTO_GRAFICA_DP.dp)
                    .semantics {
                        val detalle = reto.barras.mapIndexed { indice, barra ->
                            if (indice == tema.indiceOculto) {
                                "${barra.etiqueta}: barra tapada por el alebrije"
                            } else {
                                "${barra.etiqueta}: ${barra.valor} votos"
                            }
                        }.joinToString(separator = ". ")
                        val extra = if (tema.hayBarraOculta) {
                            " Total de votos: ${tema.total}."
                        } else {
                            ""
                        }
                        contentDescription = "Gráfica de barras. $detalle.$extra"
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cuantas = reto.barras.size
                    if (cuantas == 0) return@Canvas

                    // Trama horizontal y eje base.
                    repeat(4) { nivel ->
                        val y = size.height * nivel / 4f
                        drawLine(
                            color = trama,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(7.dp.toPx(), 7.dp.toPx())
                            )
                        )
                    }
                    drawLine(
                        color = tintaEje,
                        start = Offset(0f, size.height - 1.dp.toPx()),
                        end = Offset(size.width, size.height - 1.dp.toPx()),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    val anchoColumna = size.width / cuantas
                    val anchoBarra = anchoColumna * 0.58f
                    val altoTope = size.height * 0.70f
                    val base = size.height - 3.dp.toPx()
                    val radio = CornerRadius(11.dp.toPx(), 11.dp.toPx())
                    val glifo = when (tema.estilo) {
                        EstiloGrafica.LUCHADORES -> "🤼"
                        EstiloGrafica.CHILES -> "🌶️"
                        EstiloGrafica.ALFENIQUES -> "🍬"
                    }
                    val unidad = altoTope / maximo
                    val tamanoGlifo = (unidad * 1.05f).coerceAtLeast(8.sp.toPx())
                    val alaOculta = if (tema.hayBarraOculta) tema.indiceOculto else -1

                    reto.barras.forEachIndexed { indice, barra ->
                        val color = PALETA_BARRAS[barra.tono % PALETA_BARRAS.size]
                        val fraccion = barra.valor.toFloat() / maximo
                        val alto = (altoTope * fraccion * crecimiento)
                            .coerceAtLeast(2.dp.toPx())
                        val izquierda = indice * anchoColumna +
                            (anchoColumna - anchoBarra) / 2f
                        val arriba = base - alto
                        val centroColumna = izquierda + anchoBarra / 2f
                        val encendida = destacada != null && barra.etiqueta == destacada

                        if (indice == alaOculta) {
                            // El ala del alebrije tapa votos y altura: el niño
                            // deduce la barra con el total de la feria.
                            val topeAla = base - altoTope * 0.58f
                            val altoAla = base - topeAla
                            val ala = Path().apply {
                                moveTo(centroColumna, topeAla)
                                quadraticTo(
                                    izquierda - anchoBarra * 0.55f,
                                    topeAla + altoAla * 0.30f,
                                    izquierda - anchoBarra * 0.10f,
                                    base
                                )
                                lineTo(izquierda + anchoBarra * 1.10f, base)
                                quadraticTo(
                                    izquierda + anchoBarra * 1.55f,
                                    topeAla + altoAla * 0.30f,
                                    centroColumna,
                                    topeAla
                                )
                                close()
                            }
                            drawPath(path = ala, color = MagicalPurple)
                            drawPath(
                                path = ala,
                                color = LavandaClara,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            pincel.color = ZaziliWhite.toArgb()
                            pincel.alpha = 255
                            pincel.textSize = 24.sp.toPx()
                            drawContext.canvas.nativeCanvas.drawText(
                                "?",
                                centroColumna,
                                base - altoAla * 0.42f,
                                pincel
                            )
                            pincel.textSize = 12.sp.toPx()
                            drawContext.canvas.nativeCanvas.drawText(
                                "🪽",
                                centroColumna,
                                topeAla + altoAla * 0.30f,
                                pincel
                            )
                            return@forEachIndexed
                        }

                        // Riel tenue de la columna: mantiene el color de la barra
                        // aunque los votos se dibujen con pictogramas.
                        drawRoundRect(
                            color = color.copy(alpha = 0.14f),
                            topLeft = Offset(izquierda, base - altoTope),
                            size = Size(anchoBarra, altoTope),
                            cornerRadius = radio
                        )

                        if (encendida) {
                            drawRoundRect(
                                color = AmarilloVibrante.copy(alpha = 0.34f),
                                topLeft = Offset(
                                    izquierda - 4.dp.toPx(),
                                    arriba - 4.dp.toPx()
                                ),
                                size = Size(
                                    anchoBarra + 8.dp.toPx(),
                                    alto + 4.dp.toPx()
                                ),
                                cornerRadius = radio
                            )
                            drawRoundRect(
                                color = AmarilloVibrante,
                                topLeft = Offset(
                                    izquierda - 2.dp.toPx(),
                                    arriba - 2.dp.toPx()
                                ),
                                size = Size(
                                    anchoBarra + 4.dp.toPx(),
                                    alto + 2.dp.toPx()
                                ),
                                cornerRadius = radio,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }

                        // Pictogramas apilados: un luchador, un chile o un
                        // alfeñique por cada voto.
                        pincel.color = colorValor
                        pincel.textSize = tamanoGlifo
                        pincel.alpha = (crecimiento.coerceIn(0f, 1f) * 255).toInt()
                        repeat(barra.valor) { voto ->
                            drawContext.canvas.nativeCanvas.drawText(
                                glifo,
                                centroColumna,
                                base - (voto + 0.5f) * unidad + tamanoGlifo * 0.34f,
                                pincel
                            )
                        }
                        pincel.alpha = 255

                        if (reto.muestraValores) {
                            pincel.color = colorValor
                            pincel.textSize = 12.sp.toPx()
                            drawContext.canvas.nativeCanvas.drawText(
                                barra.valor.toString(),
                                centroColumna,
                                (arriba - 6.dp.toPx()).coerceAtLeast(11.sp.toPx()),
                                pincel
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                reto.barras.forEach { barra ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = barra.emoji, fontSize = 17.sp, lineHeight = 19.sp)
                        Text(
                            text = barra.etiqueta,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (tema.hayBarraOculta) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = LavandaClara
                ) {
                    Text(
                        text = "🪽 Total de votos en la feria: ${tema.total}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = UniversoProfundo
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PREGUNTA Y BOTONES 3D
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaPregunta(
    reto: RetoMath.Estadistica,
    sesion: SesionMath
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MagicalPurple.copy(alpha = 0.24f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Text(
                text = "LEE LA GRÁFICA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                color = MagicalPurple
            )
            Spacer(Modifier.height(6.dp))
            BionicText(
                text = reto.pregunta,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sesion.hayApoyo) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Apoyo activo: ${sesion.andamiaje.etiqueta}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MagicalPurple
                )
            }
        }
    }
}

@Composable
private fun BotonesAlebrije(
    reto: RetoMath.Estadistica,
    resultado: ResultadoMath?,
    seleccion: Int,
    onElegir: (Int) -> Unit
) {
    val bloqueado = resultado != null
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        reto.opciones.forEachIndexed { indice, texto ->
            val colorBase = PALETA_BARRAS[indice % PALETA_BARRAS.size]
            val emoji = reto.barras.firstOrNull { it.etiqueta == texto }?.emoji
            val esCorrecta = bloqueado && indice == reto.indiceCorrecto
            val esFallada = bloqueado && indice == seleccion && !esCorrecta
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
            val tamanoTexto = if (texto.length <= 3) 24.sp else 17.sp

            ZaziButton(
                text = texto,
                onClick = { onElegir(indice) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                containerColor = contenedor,
                contentColor = contenido,
                cornerRadius = 20.dp,
                baseHeight = 6.dp,
                enabled = !bloqueado,
                fontSize = tamanoTexto
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (emoji != null && !bloqueado) {
                        Text(text = emoji, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = texto,
                        fontSize = tamanoTexto,
                        fontWeight = FontWeight.Black,
                        color = contenido,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (esCorrecta) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "✓",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = contenido
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  FEEDBACK Y FINAL
// ---------------------------------------------------------------------------

@Composable
private fun BandaDatos(
    titulo: String?,
    detalle: String?,
    acierto: Boolean?
) {
    AnimatedVisibility(
        visible = detalle != null,
        enter = fadeIn(),
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
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                if (titulo != null) {
                    Text(
                        text = titulo,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo
                    )
                }
                Text(
                    text = detalle.orEmpty(),
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UniversoProfundo.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@Composable
private fun FinalEstadistica(
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
            descripcion = "Zazi celebra el final de tu ronda de estadística"
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "¡La feria de datos cerró!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "${sesion.aciertos} de ${sesion.totalRondas} gráficas leídas · " +
                "${sesion.puntos} puntos",
            fontSize = 13.5.sp,
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
                        AmarilloVibrante
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AmarilloSuave.copy(alpha = 0.60f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (sesion.esVictoria) {
                    "Leíste las barras con calma y precisión. La próxima feria trae más datos."
                } else {
                    "Cada gráfica te enseñó algo. En otra ronda verás los números sobre las barras."
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
            containerColor = MagicalPurple,
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
