package com.example.zazili.presentation.nido

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.ZenModeToggle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.mascot.EstadoVital
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiHeartIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.core.ui.estadoZaziPara
import com.example.zazili.domain.model.SessionLog
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.EstadoSaga
import com.example.zazili.domain.saga.MundoSaga
import com.example.zazili.domain.saga.NodoSaga
import com.example.zazili.domain.saga.RutaSaga
import com.example.zazili.presentation.modulos.comun.IconoModulo
import com.example.zazili.presentation.modulos.comun.colorDeModulo
import com.example.zazili.presentation.modulos.comun.fondoSuaveModulo
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

private val EtiquetasHabilidades = listOf("Números", "Letras", "Emociones", "Ciencia", "Código")

private data class MomentoDificil(
    val modulo: ModuloZazili,
    val nodo: NodoSaga,
    val intentos: Int
)

/**
 * El Nido de los Grandes: PIN amable, radar de habilidades, avance por mundo
 * y registro local de intentos que necesitaron otra vuelta. La lectura está
 * pensada para tutores: frases cortas, cero culpa y recomendaciones de cinco
 * minutos.
 */
@Composable
fun NidoPadresScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NidoViewModel = viewModel()
) {
    val configurado by viewModel.configurado.collectAsStateWithLifecycle()
    val desbloqueado by viewModel.desbloqueado.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()

    if (!desbloqueado) {
        PantallaPin(
            configurado = configurado,
            mensaje = mensaje,
            onVolver = onVolver,
            onLimpiarMensaje = viewModel::limpiarMensaje,
            onConfigurar = viewModel::configurarPin,
            onDesbloquear = viewModel::desbloquear,
            modifier = modifier
        )
        return
    }

    val estado by viewModel.estadoSaga.collectAsStateWithLifecycle()
    val progreso by viewModel.progreso.collectAsStateWithLifecycle()
    val zazoles by viewModel.zazoles.collectAsStateWithLifecycle()
    val mascota by viewModel.mascota.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    PanelNido(
        estado = estado,
        progreso = progreso,
        zazoles = zazoles,
        mascota = mascota,
        logs = logs,
        onVolver = onVolver,
        onCerrarNido = {
            GlobalAudioManager.playPop()
            viewModel.cerrarNido()
        },
        modifier = modifier
    )
}

@Composable
private fun PantallaPin(
    configurado: Boolean,
    mensaje: String?,
    onVolver: () -> Unit,
    onLimpiarMensaje: () -> Unit,
    onConfigurar: (String, String) -> Boolean,
    onDesbloquear: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var primerPin by remember { mutableStateOf("") }
    var confirmando by remember(configurado) { mutableStateOf(false) }

    LaunchedEffect(pin) {
        if (pin.length < 4) return@LaunchedEffect
        val capturado = pin
        pin = ""
        if (configurado) {
            onDesbloquear(capturado)
        } else if (!confirmando) {
            primerPin = capturado
            confirmando = true
        } else {
            val guardado = onConfigurar(primerPin, capturado)
            if (!guardado) {
                primerPin = ""
                confirmando = false
            }
        }
    }

    val titulo = when {
        configurado -> "Ingresa tu PIN"
        confirmando -> "Repite el PIN"
        else -> "Crea tu PIN"
    }
    val subtitulo = when {
        configurado -> "Cuatro números para abrir el panel de adultos."
        confirmando -> "Una vez más para confirmar que no se te escape."
        else -> "Cuatro números que solo tú recuerdes. Nada de 1234, ¿va?"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                ZaziButton(
                    text = "Volver a la casita",
                    onClick = {
                        GlobalAudioManager.playPop()
                        onVolver()
                    },
                    modifier = Modifier.size(46.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    cornerRadius = 23.dp,
                    baseHeight = 4.dp
                ) {
                    KawaiiArrowIcon(
                        modifier = Modifier.graphicsLayer { rotationZ = 180f },
                        size = 18.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = MagicalPurple) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        KawaiiLockIcon(size = 15.dp, color = ZaziliWhite)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Solo adultos",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZaziliWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            ZaziSpriteAnimado(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(136.dp),
                estado = ZaziState.VUELO_IDLE
            )
            Text(
                text = "El Nido de los Grandes",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Un espacio tranquilo para ver cómo va el vuelo y cómo acompañarlo.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MagicalPurple.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        KawaiiLockIcon(size = 28.dp, color = MagicalPurple)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        textAlign = TextAlign.Center
                    )
                    if (!configurado) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (confirmando) "Paso 2 de 2" else "Paso 1 de 2",
                            style = MaterialTheme.typography.labelSmall,
                            color = MagicalPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PuntosPin(cantidad = pin.length)
                    if (mensaje != null) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AmarilloSuave
                        ) {
                            Text(
                                text = mensaje,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = UniversoProfundo,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TecladoPin(
                        onDigito = { digito ->
                            if (pin.length < 4) {
                                onLimpiarMensaje()
                                pin += digito
                            }
                        },
                        onBorrar = {
                            if (pin.isNotEmpty()) {
                                pin = pin.dropLast(1)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "El PIN se guarda cifrado en este dispositivo. Si lo olvidas, la app no puede recuperarlo.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun PuntosPin(cantidad: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { indice ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (indice < cantidad) {
                            MagicalPurple
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}

@Composable
private fun TecladoPin(
    onDigito: (Char) -> Unit,
    onBorrar: () -> Unit
) {
    val teclas = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "vacio", "0", "borrar"
    )
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        teclas.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                fila.forEach { tecla ->
                    when (tecla) {
                        "vacio" -> Spacer(
                            Modifier.size(width = 78.dp, height = 56.dp)
                        )
                        "borrar" -> ZaziButton(
                            text = "Borrar el último número",
                            onClick = onBorrar,
                            modifier = Modifier.size(width = 78.dp, height = 56.dp),
                            containerColor = LavandaClara,
                            contentColor = UniversoProfundo,
                            cornerRadius = 18.dp,
                            baseHeight = 4.dp
                        ) {
                            Text(
                                text = "Borrar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = UniversoProfundo
                            )
                        }
                        else -> ZaziButton(
                            text = "Número $tecla",
                            onClick = { onDigito(tecla.first()) },
                            modifier = Modifier.size(width = 78.dp, height = 56.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cornerRadius = 18.dp,
                            baseHeight = 4.dp
                        ) {
                            Text(
                                text = tecla,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelNido(
    estado: EstadoSaga,
    progreso: GameProgress,
    zazoles: Int,
    mascota: EstadoVital,
    logs: List<SessionLog>,
    onVolver: () -> Unit,
    onCerrarNido: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    val fondo = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }

    val nodosPorId = remember {
        RutaSaga.mundos
            .flatMap { mundo -> mundo.nodos.map { nodo -> nodo.id to (mundo.modulo to nodo) } }
            .toMap()
    }
    val fallos = remember(logs, nodosPorId) {
        logs.asSequence()
            .filter { !it.succeeded }
            .groupBy { it.activityId }
            .mapNotNull { (id, intentos) ->
                val par = nodosPorId[id] ?: return@mapNotNull null
                MomentoDificil(
                    modulo = par.first,
                    nodo = par.second,
                    intentos = intentos.size
                )
            }
            .sortedByDescending { it.intentos }
            .take(3)
    }
    val mundoSugerido = RutaSaga.mundos.minByOrNull { mundo ->
        mundo.nodos.sumOf { estado.estrellasDe(it.id) }
    } ?: RutaSaga.mundos.first()
    val siguienteSugerido = RutaSaga.siguienteNodo(mundoSugerido, estado)
    val recomendacion = when {
        fallos.isNotEmpty() -> {
            val primero = fallos.first()
            "Acompañen «${primero.nodo.nombre}» de ${primero.modulo.titulo}. " +
                "Léanlo juntos y pidan la pista: cinco minutos alcanzan."
        }
        siguienteSugerido != null ->
            "Hoy pueden probar «${siguienteSugerido.nombre}» en ${mundoSugerido.modulo.titulo}. " +
                "Una pregunta, un respiro y se cierra."
        else ->
            "La Saga está completa. Repasen el mundo que más les guste; practicar sin premio también cuenta."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraNido(onVolver = onVolver)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                HeroNido(
                    estado = estado,
                    progreso = progreso,
                    zazoles = zazoles,
                    mascota = mascota
                )
                Spacer(Modifier.height(16.dp))
                SeccionRadar(estado = estado)
                Spacer(Modifier.height(14.dp))
                SeccionAvance(estado = estado)
                Spacer(Modifier.height(14.dp))
                SeccionRecomendacion(
                    recomendacion = recomendacion,
                    mundo = mundoSugerido
                )
                Spacer(Modifier.height(14.dp))
                SeccionFrustracion(fallos = fallos)
                Spacer(Modifier.height(16.dp))
                ZenModeToggle()
                Spacer(Modifier.height(16.dp))
                ZaziButton(
                    text = "Bloquear el Nido",
                    onClick = onCerrarNido,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    containerColor = MagicalPurple,
                    contentColor = ZaziliWhite,
                    cornerRadius = 20.dp,
                    baseHeight = 5.dp,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Todo se queda en este dispositivo: sin cuentas, sin nubes y sin anuncios.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun BarraNido(onVolver: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ZaziButton(
            text = "Volver a la casita",
            onClick = {
                GlobalAudioManager.playPop()
                onVolver()
            },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 24.dp,
            baseHeight = 4.dp
        ) {
            KawaiiArrowIcon(
                modifier = Modifier.graphicsLayer { rotationZ = 180f },
                size = 20.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "El Nido",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Panel de papás, mamás y maestros",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MagicalPurple.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            KawaiiLockIcon(size = 22.dp, color = MagicalPurple)
        }
    }
}

@Composable
private fun HeroNido(
    estado: EstadoSaga,
    progreso: GameProgress,
    zazoles: Int,
    mascota: EstadoVital
) {
    val estrellas = estado.estrellas.values.sum()
    val mensajeMascota = when {
        mascota.dormida -> "Zazi está dormida en su piyama. Descansar también es parte del plan."
        mascota.pancita < 0.30f -> "Zazi pide semillas. Un mimo en la casita la pone contenta."
        mascota.energia < 0.30f -> "Zazi anda en modo avión; hoy conviene una sesión corta."
        else -> "Zazi está contenta y lista para acompañar el vuelo de hoy."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(UniversoProfundo, UniversoMedio, MagicalPurple)
                )
            )
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HOLA, PAPÁ, MAMÁ O MAESTRO",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmarilloSuave,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Así va el vuelo.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ZaziliWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = mensajeMascota,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZaziliWhite.copy(alpha = 0.78f)
                    )
                }
                ZaziSpriteAnimado(
                    modifier = Modifier.size(84.dp),
                    estado = estadoZaziPara(mascota)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipHeroe {
                    KawaiiStarIcon(size = 15.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Nivel ${progreso.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZaziliWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                ChipHeroe {
                    KawaiiStarIcon(size = 15.dp, color = AmarilloVibrante)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$estrellas/${RutaSaga.totalEstrellasPosibles()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZaziliWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                ChipHeroe {
                    KawaiiCoinIcon(size = 16.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$zazoles",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZaziliWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipHeroe(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = ZaziliWhite.copy(alpha = 0.13f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SeccionRadar(estado: EstadoSaga) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mapa de habilidades",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cada punta crece con las estrellas de su mundo. No mide notas ni velocidad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Spacer(Modifier.height(8.dp))
            RadarHabilidades(
                estado = estado,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                RutaSaga.mundos.forEachIndexed { indice, mundo ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colorDeModulo(mundo.modulo))
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = EtiquetasHabilidades.getOrElse(indice) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarHabilidades(
    estado: EstadoSaga,
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val ejeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val relleno = MagicalPurple.copy(alpha = 0.20f)
    val trazo = MagicalPurple.copy(alpha = 0.85f)
    val etiquetaColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val colores = RutaSaga.mundos.map { colorDeModulo(it.modulo) }
    val valores = RutaSaga.mundos.map { mundo ->
        val maximo = mundo.nodos.size * 3
        if (maximo == 0) 0f else {
            (mundo.nodos.sumOf { estado.estrellasDe(it.id) }.toFloat() / maximo).coerceIn(0f, 1f)
        }
    }

    Canvas(modifier = modifier) {
        val centro = Offset(size.width / 2f, size.height / 2f)
        val radio = (minOf(size.width, size.height) / 2f - 34.dp.toPx()).coerceAtLeast(10f)

        fun punto(indice: Int, valor: Float): Offset {
            val angulo = -Math.PI / 2.0 + indice * 2.0 * Math.PI / 5.0
            return Offset(
                x = centro.x + (radio * valor * kotlin.math.cos(angulo)).toFloat(),
                y = centro.y + (radio * valor * kotlin.math.sin(angulo)).toFloat()
            )
        }

        for (nivel in 1..4) {
            val valor = nivel / 4f
            val path = Path()
            repeat(5) { indice ->
                val p = punto(indice, valor)
                if (indice == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(
                path = path,
                color = gridColor,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        repeat(5) { indice ->
            drawLine(
                color = ejeColor,
                start = centro,
                end = punto(indice, 1f),
                strokeWidth = 1.dp.toPx()
            )
        }

        val datos = Path()
        val puntosDatos = valores.mapIndexed { indice, valor ->
            val p = punto(indice, valor)
            if (indice == 0) datos.moveTo(p.x, p.y) else datos.lineTo(p.x, p.y)
            p
        }
        datos.close()
        drawPath(datos, color = relleno)
        drawPath(datos, color = trazo, style = Stroke(width = 2.5.dp.toPx()))
        puntosDatos.forEachIndexed { indice, p ->
            drawCircle(
                color = colores.getOrElse(indice) { MagicalPurple },
                radius = 4.5.dp.toPx(),
                center = p
            )
            drawCircle(
                color = ZaziliWhite.copy(alpha = 0.85f),
                radius = 1.5.dp.toPx(),
                center = p
            )
        }

        val paint = Paint().apply {
            isAntiAlias = true
            color = etiquetaColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 11.dp.toPx()
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        EtiquetasHabilidades.forEachIndexed { indice, etiqueta ->
            val p = punto(indice, 1.24f)
            val desplazamiento = if (p.y < centro.y) 0f else paint.textSize * 0.8f
            drawContext.canvas.nativeCanvas.drawText(
                etiqueta,
                p.x,
                p.y + desplazamiento,
                paint
            )
        }
    }
}

@Composable
private fun SeccionAvance(estado: EstadoSaga) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Avance por mundo",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Retos completados y estrellas ganadas con constancia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Spacer(Modifier.height(12.dp))
            RutaSaga.mundos.forEachIndexed { indice, mundo ->
                val completados = estado.completadosEn(mundo)
                val estrellas = mundo.nodos.sumOf { estado.estrellasDe(it.id) }
                val color = colorDeModulo(mundo.modulo)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(fondoSuaveModulo(mundo.modulo)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconoModulo(mundo.modulo, 23.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = mundo.modulo.titulo,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$completados/${mundo.nodos.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            KawaiiStarIcon(size = 11.dp)
                            Text(
                                text = "$estrellas/${mundo.nodos.size * 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        BarraAdulto(
                            fraccion = if (mundo.nodos.isEmpty()) 0f else {
                                completados.toFloat() / mundo.nodos.size
                            },
                            color = color
                        )
                    }
                }
                if (indice < RutaSaga.mundos.lastIndex) {
                    Spacer(Modifier.height(11.dp))
                }
            }
        }
    }
}

@Composable
private fun BarraAdulto(fraccion: Float, color: Color) {
    val animada by animateFloatAsState(
        targetValue = fraccion.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "barraNido"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animada)
                .height(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun SeccionRecomendacion(
    recomendacion: String,
    mundo: MundoSaga
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AmarilloSuave),
                    contentAlignment = Alignment.Center
                ) {
                    KawaiiStarIcon(size = 22.dp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Para acompañar hoy",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = mundo.modulo.titulo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = recomendacion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MintGreen.copy(alpha = 0.28f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                ) {
                    KawaiiHeartIcon(size = 18.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "Tip de calma: inhalen 4, sostengan 2 y exhalen 4 antes de empezar.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionFrustracion(fallos: List<MomentoDificil>) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Momentos que pidieron otra vuelta",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Registro local de intentos. No es un boletín: es una brújula para acompañar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Spacer(Modifier.height(10.dp))
            if (fallos.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MintGreen.copy(alpha = 0.22f))
                        .padding(12.dp)
                ) {
                    KawaiiHeartIcon(size = 24.dp)
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Aún no hay intentos fallidos registrados. Si aparecen, aquí verán dónde acompañar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            } else {
                fallos.forEachIndexed { indice, fallo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AmarilloSuave),
                            contentAlignment = Alignment.Center
                        ) {
                            KawaiiStarIcon(size = 18.dp)
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fallo.nodo.nombre,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = fallo.modulo.titulo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (fallo.intentos == 1) "1 intento" else "${fallo.intentos} intentos",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (indice < fallos.lastIndex) {
                        Spacer(Modifier.height(9.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Si un reto se repite, cambien de actividad y vuelvan mañana. Zazi no lleva prisa.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}
