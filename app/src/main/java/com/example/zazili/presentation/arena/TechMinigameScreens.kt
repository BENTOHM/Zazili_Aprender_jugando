package com.example.zazili.presentation.arena

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.PixelArtZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
//  JUEGO 1: APLASTA EL BUG
// ---------------------------------------------------------------------------

/**
 * Whack-a-mole digital. Los bichos nacen y caducan solos; la ventana de vida
 * baja con el puntaje (1200 → 800 → 500 ms) y los antivirus aparecen desde
 * los 50 puntos. Un error sacude el tablero y pinta el borde de rojo.
 */
@Composable
fun AplastaElBugTechScreen(
    motor: ArenaTechGameEngine,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by motor.estadoAplastaBug.collectAsStateWithLifecycle()
    val progreso by motor.progresoTech.collectAsStateWithLifecycle()

    val sacudida = remember { Animatable(0f) }
    val destelloRojo = remember { Animatable(0f) }
    val alcance = rememberCoroutineScope()
    var celdaAcierto by remember { mutableStateOf<Int?>(null) }
    var celdaError by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(motor) {
        motor.iniciarAplastaBug()
        motor.eventosAplastaBug.collect { evento ->
            when (evento) {
                is EventoAplastaBug.Acierto -> {
                    celdaAcierto = evento.indice
                    alcance.launch {
                        delay(640L)
                        if (celdaAcierto == evento.indice) celdaAcierto = null
                    }
                }

                is EventoAplastaBug.Error -> {
                    celdaError = evento.indice
                    alcance.launch {
                        delay(640L)
                        if (celdaError == evento.indice) celdaError = null
                    }
                    alcance.launch { sacudir(sacudida) }
                    alcance.launch {
                        destelloRojo.animateTo(0.38f, tween(90))
                        destelloRojo.animateTo(0f, tween(280))
                    }
                }
            }
        }
    }

    DisposableEffect(motor) {
        onDispose { motor.detenerAplastaBug() }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EncabezadoTech(
                titulo = "Aplasta el Bug",
                subtitulo = "Toca los bichos, evita los escudos",
                chip = if (estado.enFrenesi) "¡FRENESÍ!" else "Nivel ${
                    (motor.lessonId - 1) / 3 + 1
                }",
                onSalir = onSalir
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoTech("🎯", "${estado.puntaje}", "Puntos", Modifier.weight(1f))
                DatoTech("🐞", "${estado.bugsAplastados}", "Bugs", Modifier.weight(1f))
                DatoTech("🛡️", "${estado.antivirusTocados}", "Errores", Modifier.weight(1f))
            }

            LinearProgressIndicator(
                progress = { estado.progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (estado.enFrenesi) CoralAviso else VerdeDuo,
                trackColor = LavandaClara
            )

            Text(
                text = "Llena la barra para ganar. ¡Cada bicho vale 10 Zazoles!",
                style = MaterialTheme.typography.bodySmall,
                color = UniversoProfundo.copy(alpha = 0.72f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            TableroBichos(
                estado = estado,
                celdaAcierto = celdaAcierto,
                celdaError = celdaError,
                sacudida = sacudida.value,
                destelloRojo = destelloRojo.value,
                onAplastar = motor::aplastarCelda,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        if (progreso.victoria) {
            PantallaVictoriaTech(
                titulo = "Aplasta el Bug",
                estado = progreso,
                onSalir = onSalir
            )
        }
    }
}

@Composable
private fun TableroBichos(
    estado: EstadoAplastaBugTech,
    celdaAcierto: Int?,
    celdaError: Int?,
    sacudida: Float,
    destelloRojo: Float,
    onAplastar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val separacion = 8.dp
        val ladoPorAncho =
            (maxWidth - separacion * (estado.tamano - 1)) / estado.tamano
        val ladoPorAlto =
            (maxHeight - separacion * (estado.tamano - 1)) / estado.tamano
        val lado = minOf(ladoPorAncho, ladoPorAlto).coerceAtLeast(40.dp)
        val temblorX = (sin(sacudida * PI * 5).toFloat() * 16f).roundToInt()

        Column(
            Modifier.offset { IntOffset(temblorX, 0) },
            verticalArrangement = Arrangement.spacedBy(separacion),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(estado.tamano) { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(separacion)) {
                    repeat(estado.tamano) { columna ->
                        val indice = fila * estado.tamano + columna
                        CeldaBicho(
                            bicho = estado.celdas.getOrNull(indice),
                            indice = indice,
                            lado = lado,
                            mostrarAcierto = celdaAcierto == indice,
                            mostrarError = celdaError == indice,
                            onAplastar = { onAplastar(indice) }
                        )
                    }
                }
            }
        }

        if (destelloRojo > 0.01f) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CoralAviso.copy(alpha = destelloRojo))
            )
        }
    }
}

@Composable
private fun CeldaBicho(
    bicho: BichoVivo?,
    indice: Int,
    lado: androidx.compose.ui.unit.Dp,
    mostrarAcierto: Boolean,
    mostrarError: Boolean,
    onAplastar: () -> Unit
) {
    val fondo = when {
        bicho?.esAntivirus == true -> AzulCielo
        bicho != null -> RosaSuave
        else -> LavandaClara.copy(alpha = 0.55f)
    }
    val borde = when {
        mostrarError -> CoralAviso
        bicho != null -> MagicalPurple.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val descripcion = when {
        bicho == null -> "Celda vacía $indice"
        bicho.esAntivirus -> "Antivirus, no lo toques"
        else -> "Bug, aplástalo"
    }

    Box(
        modifier = Modifier
            .size(lado)
            .clip(RoundedCornerShape(24.dp))
            .background(fondo)
            .border(3.dp, borde, RoundedCornerShape(24.dp))
            .clickable(enabled = bicho != null) {
                onAplastar()
            }
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        if (bicho != null) {
            Text(
                text = if (bicho.esAntivirus) "🛡️" else "🐞",
                fontSize = 40.sp,
                textAlign = TextAlign.Center
            )
        }

        AnimatedVisibility(
            visible = mostrarAcierto,
            enter = fadeIn(tween(80)) + scaleIn(initialScale = 0.5f),
            exit = fadeOut(tween(260)),
            modifier = Modifier.offset(y = (-lado.value * 0.42f).dp)
        ) {
            Text(
                text = "+10",
                color = VerdeDuoOscuro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        }
    }
}

private suspend fun sacudir(animatable: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>) {
    animatable.snapTo(0f)
    animatable.animateTo(1f, tween(320))
    animatable.snapTo(0f)
}

// ---------------------------------------------------------------------------
//  JUEGO 2: RUTA DEL ALGORITMO
// ---------------------------------------------------------------------------

/**
 * Rompecabezas de programación por bloques. El niño arrastra (o toca) flechas
 * hacia la bandeja de secuencia y después presiona «¡Ejecutar!». Cada paso se
 * anima con 300 ms de separación; los firewalls y las trampas reinician la
 * carrera con un mensaje amable.
 */
@Composable
fun RutaDelAlgoritmoTechScreen(
    motor: ArenaTechGameEngine,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by motor.estadoRuta.collectAsStateWithLifecycle()
    val progreso by motor.progresoTech.collectAsStateWithLifecycle()
    var rectanguloBandeja by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EncabezadoTech(
                titulo = "Ruta del Algoritmo",
                subtitulo = "Lleva a Zazi hasta la batería",
                chip = "Nivel ${motor.lessonId}",
                onSalir = onSalir
            )

            MapaRuta(
                estado = estado,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            val mensaje = estado.mensaje
            if (mensaje != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (estado.choque) RosaSuave else MintGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mensaje,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = UniversoProfundo,
                        textAlign = TextAlign.Center
                    )
                }
            }

            BandejaSecuencia(
                estado = estado,
                onMedir = { rectanguloBandeja = it },
                onQuitarUltima = motor::quitarUltimaDireccion,
                onLimpiar = motor::limpiarSecuencia
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Direccion.entries.forEach { direccion ->
                    BloqueDireccionArrastrable(
                        direccion = direccion,
                        habilitado = !estado.ejecutando && !estado.terminado,
                        rectanguloDestino = rectanguloBandeja,
                        onSoltar = { motor.agregarDireccion(direccion) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ZaziButton(
                text = if (estado.ejecutando) "Ejecutando…" else "¡Ejecutar!",
                onClick = motor::ejecutarRuta,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                enabled = estado.secuencia.isNotEmpty() &&
                    !estado.ejecutando &&
                    !estado.terminado
            )

            TextButton(
                onClick = {
                    GlobalAudioManager.playPop()
                    motor.reiniciarRuta()
                },
                enabled = !estado.ejecutando && !estado.terminado,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Nuevo mapa",
                    color = MagicalPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (progreso.victoria) {
            PantallaVictoriaTech(
                titulo = "Ruta del Algoritmo",
                estado = progreso,
                onSalir = onSalir
            )
        }
    }
}

@Composable
private fun MapaRuta(
    estado: EstadoRutaTech,
    modifier: Modifier = Modifier
) {
    val mapa = estado.mapa
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val separacion = 6.dp
        val ladoPorAncho = (maxWidth - separacion * (mapa.tamano - 1)) / mapa.tamano
        val ladoPorAlto = (maxHeight - separacion * (mapa.tamano - 1)) / mapa.tamano
        val lado = minOf(ladoPorAncho, ladoPorAlto).coerceAtLeast(36.dp)
        val paso = lado + separacion

        val x by animateDpAsState(
            targetValue = paso * estado.zazi.col.toFloat(),
            animationSpec = tween(260),
            label = "zaziColumna"
        )
        val y by animateDpAsState(
            targetValue = paso * estado.zazi.fila.toFloat(),
            animationSpec = tween(260),
            label = "zaziFila"
        )

        Box(
            Modifier.size(paso * mapa.tamano.toFloat()),
            contentAlignment = Alignment.TopStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(separacion)) {
                repeat(mapa.tamano) { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(separacion)) {
                        repeat(mapa.tamano) { columna ->
                            CeldaMapa(
                                tipo = mapa.tipoDe(Punto(fila, columna)),
                                lado = lado
                            )
                        }
                    }
                }
            }

            PixelArtZazi(
                modifier = Modifier.offset(x = x, y = y),
                estado = if (estado.ejecutando) ZaziState.VUELO_IDLE else ZaziState.BASE,
                animar = true,
                size = lado * 0.92f,
                descripcion = "Zazi en la ruta"
            )
        }
    }
}

@Composable
private fun CeldaMapa(tipo: TipoCeldaRuta, lado: androidx.compose.ui.unit.Dp) {
    val fondo = when (tipo) {
        TipoCeldaRuta.MURO -> UniversoMedio.copy(alpha = 0.9f)
        TipoCeldaRuta.TRAMPA -> AmarilloSuave
        TipoCeldaRuta.META -> MintGreen
        TipoCeldaRuta.INICIO -> LavandaClara
        TipoCeldaRuta.LIBRE -> SoftCream
    }
    val icono = when (tipo) {
        TipoCeldaRuta.MURO -> "🧱"
        TipoCeldaRuta.TRAMPA -> "⚠️"
        TipoCeldaRuta.META -> "🔋"
        TipoCeldaRuta.INICIO -> "🏁"
        TipoCeldaRuta.LIBRE -> ""
    }
    Box(
        modifier = Modifier
            .size(lado)
            .clip(RoundedCornerShape(14.dp))
            .background(fondo)
            .border(
                width = 2.dp,
                color = MagicalPurple.copy(alpha = 0.16f),
                shape = RoundedCornerShape(14.dp)
            )
            .semantics { contentDescription = descripcionCelda(tipo) },
        contentAlignment = Alignment.Center
    ) {
        if (icono.isNotEmpty()) {
            Text(text = icono, fontSize = 20.sp)
        }
    }
}

private fun descripcionCelda(tipo: TipoCeldaRuta): String = when (tipo) {
    TipoCeldaRuta.MURO -> "Firewall, no se puede pasar"
    TipoCeldaRuta.TRAMPA -> "Trampa, evítala"
    TipoCeldaRuta.META -> "Batería, la meta"
    TipoCeldaRuta.INICIO -> "Inicio"
    TipoCeldaRuta.LIBRE -> "Camino libre"
}

@Composable
private fun BandejaSecuencia(
    estado: EstadoRutaTech,
    onMedir: (Rect) -> Unit,
    onQuitarUltima: () -> Unit,
    onLimpiar: () -> Unit
) {
    val maximo = estado.mapa.maxPasos
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onMedir(it.boundsInRoot()) },
        shape = RoundedCornerShape(20.dp),
        color = LavandaClara
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tu secuencia",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = UniversoProfundo
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${estado.secuencia.size}/$maximo",
                    style = MaterialTheme.typography.labelMedium,
                    color = UniversoProfundo.copy(alpha = 0.65f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onQuitarUltima, enabled = estado.secuencia.isNotEmpty()) {
                    Text("Borrar", fontSize = 11.sp)
                }
                TextButton(onClick = onLimpiar, enabled = estado.secuencia.isNotEmpty()) {
                    Text("Limpiar", fontSize = 11.sp)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (estado.secuencia.isEmpty()) {
                    Text(
                        text = "Arrastra flechas aquí o tócalas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = UniversoProfundo.copy(alpha = 0.6f)
                    )
                } else {
                    estado.secuencia.forEachIndexed { indice, direccion ->
                        Surface(
                            shape = CircleShape,
                            color = ZaziliWhite,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${indice + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MagicalPurple
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(text = direccion.emoji, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BloqueDireccionArrastrable(
    direccion: Direccion,
    habilitado: Boolean,
    rectanguloDestino: Rect,
    onSoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alcance = rememberCoroutineScope()
    val desplazamiento = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var rectanguloBoton by remember { mutableStateOf(Rect.Zero) }
    val destinoActual by rememberUpdatedState(rectanguloDestino)
    val accionActual by rememberUpdatedState(onSoltar)

    Surface(
        modifier = modifier
            // Se mide ANTES del desplazamiento para que el rectángulo guardado
            // sea el punto de partida, no la posición animada del gesto.
            .onGloballyPositioned { rectanguloBoton = it.boundsInRoot() }
            .offset {
                IntOffset(
                    desplazamiento.value.x.roundToInt(),
                    desplazamiento.value.y.roundToInt()
                )
            }
            .pointerInput(habilitado) {
                if (!habilitado) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        val centro = rectanguloBoton.center + desplazamiento.value
                        val solto = destinoActual.contains(centro)
                        alcance.launch {
                            if (solto) {
                                accionActual()
                                desplazamiento.snapTo(Offset.Zero)
                            } else {
                                desplazamiento.animateTo(
                                    targetValue = Offset.Zero,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
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
                        }
                    },
                    onDrag = { cambio, arrastre ->
                        cambio.consume()
                        alcance.launch {
                            desplazamiento.snapTo(desplazamiento.value + arrastre)
                        }
                    }
                )
            }
            .clickable(enabled = habilitado) {
                GlobalAudioManager.playPop()
                accionActual()
            },
        shape = RoundedCornerShape(18.dp),
        color = if (habilitado) SoftCream else SoftCream.copy(alpha = 0.55f),
        shadowElevation = 4.dp
    ) {
        Column(
            Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = direccion.emoji, fontSize = 20.sp)
            Text(
                text = direccion.titulo,
                fontSize = 9.sp,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  PIEZAS COMPARTIDAS
// ---------------------------------------------------------------------------

@Composable
fun PantallaVictoriaTech(
    titulo: String,
    estado: EstadoProgresoTech,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .background(UniversoProfundo.copy(alpha = 0.93f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = SoftCream,
            shadowElevation = 10.dp
        ) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¡Nivel completado!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MagicalPurple,
                    textAlign = TextAlign.Center
                )
                PixelArtZazi(
                    size = 118.dp,
                    estado = ZaziState.CELEBRACION,
                    animar = true,
                    descripcion = "Zazi celebra el nivel completado"
                )
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = UniversoProfundo,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AmarilloSuave
                ) {
                    Text(
                        text = "+${estado.zazolesGanados} Zazoles",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = UniversoProfundo
                    )
                }
                Text(
                    text = if (estado.guardando) {
                        "Guardando tu premio…"
                    } else {
                        "Tu siguiente nivel ya está abierto. ¡Vamos!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = UniversoProfundo.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
                ZaziButton(
                    text = if (estado.guardando) "Guardando…" else "¡Volver al mapa!",
                    onClick = onSalir,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    containerColor = VerdeDuo,
                    contentColor = ZaziliWhite,
                    enabled = !estado.guardando
                )
            }
        }
    }
}

@Composable
private fun EncabezadoTech(
    titulo: String,
    subtitulo: String,
    chip: String,
    onSalir: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = {
                GlobalAudioManager.playPop()
                onSalir()
            }
        ) {
            Text(
                text = "← Salir",
                color = MagicalPurple,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = UniversoProfundo.copy(alpha = 0.66f),
                textAlign = TextAlign.Center
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = AmarilloSuave
        ) {
            Text(
                text = chip,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DatoTech(
    emoji: String,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = LavandaClara
    ) {
        Column(
            Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$emoji $valor", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = UniversoProfundo)
            Text(
                text = etiqueta,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = UniversoProfundo.copy(alpha = 0.65f)
            )
        }
    }
}
