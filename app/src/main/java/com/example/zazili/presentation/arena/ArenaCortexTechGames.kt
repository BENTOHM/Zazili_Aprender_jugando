package com.example.zazili.presentation.arena

import android.app.Application
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziSpriteAnimado
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Hub de la Arena Cortex de tecnología rápida: tres minijuegos veloces,
 * sin castigos, con recompensas de Zazoles y feedback inmediato pensados para
 * mentes de 6 a 12 años con TDAH.
 *
 * Cada juego tiene su propio `AndroidViewModel` (estado `StateFlow`, sin
 * lambdas suspendidas en Compose) y su pantalla. Los timers viven en
 * `LaunchedEffect` y mueren al salir de composición; nada corre en segundo
 * plano salvo la corrutina de ejecución de la ruta, cancelada en `onCleared`.
 */
private enum class DestinoTech(val id: String) {
    HUB("hub"),
    APLASTA("aplasta"),
    RUTA("ruta"),
    FILTRO("filtro")
}

@Composable
fun ArenaCortexTechGamesScreen(
    onSalir: () -> Unit,
    onAbrirClasicos: () -> Unit,
    modifier: Modifier = Modifier
) {
    var destinoGuardado by rememberSaveable { mutableStateOf(DestinoTech.HUB.id) }
    val destino = remember(destinoGuardado) {
        DestinoTech.entries.firstOrNull { it.id == destinoGuardado } ?: DestinoTech.HUB
    }

    // El atrás del sistema regresa al hub antes de salir de la Arena.
    BackHandler(enabled = destino != DestinoTech.HUB) {
        GlobalAudioManager.playPop()
        destinoGuardado = DestinoTech.HUB.id
    }

    val economia = economiaTech()
    val zazoles by economia.zazoles.collectAsStateWithLifecycle()

    when (destino) {
        DestinoTech.HUB -> HubTechJuegos(
            zazoles = zazoles,
            onSalir = onSalir,
            onAbrirClasicos = onAbrirClasicos,
            onAbrirJuego = { juego ->
                GlobalAudioManager.playPop()
                destinoGuardado = juego.id
            },
            modifier = modifier
        )

        DestinoTech.APLASTA -> AplastaElBugScreen(
            onSalir = { destinoGuardado = DestinoTech.HUB.id },
            modifier = modifier
        )

        DestinoTech.RUTA -> RutaDelAlgoritmoScreen(
            onSalir = { destinoGuardado = DestinoTech.HUB.id },
            modifier = modifier
        )

        DestinoTech.FILTRO -> FiltroCiberSeguroScreen(
            onSalir = { destinoGuardado = DestinoTech.HUB.id },
            modifier = modifier
        )
    }
}

private data class FichaJuegoTech(
    val destino: DestinoTech,
    val emoji: String,
    val titulo: String,
    val gancho: String,
    val detalle: String,
    val color: Color
)

private val FICHAS_TECH = listOf(
    FichaJuegoTech(
        destino = DestinoTech.APLASTA,
        emoji = "🐞",
        titulo = "Aplasta el Bug",
        gancho = "Reflejos de colibrí en 30 segundos",
        detalle = "Los bugs se asoman 1 segundo. ¡Tócalos todos antes de que se escondan!",
        color = AmarilloSuave
    ),
    FichaJuegoTech(
        destino = DestinoTech.RUTA,
        emoji = "🧭",
        titulo = "Ruta del Algoritmo",
        gancho = "Programa a Zazi con flechas",
        detalle = "Arrastra flechas para guiar a Zazi hasta la batería sin chocar.",
        color = AzulCielo
    ),
    FichaJuegoTech(
        destino = DestinoTech.FILTRO,
        emoji = "🛡️",
        titulo = "Filtro Ciber-Seguro",
        gancho = "Desliza y decide en un segundo",
        detalle = "Los mensajes llegan volando: bloquea los engaños y acepta lo seguro.",
        color = MintGreen
    )
)

@Composable
private fun HubTechJuegos(
    zazoles: Int,
    onSalir: () -> Unit,
    onAbrirClasicos: () -> Unit,
    onAbrirJuego: (DestinoTech) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "hub_encabezado") {
            EncabezadoJuego(
                titulo = "Arena Cortex",
                descripcion = "Tecnología rápida",
                onSalir = onSalir,
                contenidoDerecha = { ChipZazoles(zazoles) }
            )
        }

        item(key = "hub_zazi") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZaziSpriteAnimado(
                    size = 88.dp,
                    estado = ZaziState.BASE,
                    descripcion = "Zazi saluda desde la Arena"
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "¡Hola! ¿Qué entrenamiento jugamos hoy?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        item(key = "hub_seccion") {
            Text(
                text = "Tecnología rápida",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(items = FICHAS_TECH, key = { it.titulo }) { ficha ->
            TarjetaJuegoTech(ficha = ficha, onClick = { onAbrirJuego(ficha.destino) })
        }

        item(key = "hub_clasicos") {
            TarjetaArenaClasica(onClick = {
                GlobalAudioManager.playPop()
                onAbrirClasicos()
            })
        }
    }
}

@Composable
private fun TarjetaJuegoTech(ficha: FichaJuegoTech, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ficha.color,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Abrir ${ficha.titulo}") { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ZaziliWhite.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Text(ficha.emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ficha.titulo,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = UniversoProfundo
                )
                Text(
                    text = ficha.gancho,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = UniversoProfundo.copy(alpha = 0.72f)
                )
                Text(
                    text = ficha.detalle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = UniversoProfundo.copy(alpha = 0.58f)
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(color = VerdeDuoOscuro, shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = "JUGAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ZaziliWhite,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaArenaClasica(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = LavandaClara,
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Abrir la Arena clásica") { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MagicalPurple.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 28.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Arena clásica",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = UniversoProfundo
                )
                Text(
                    text = "Los 16 minijuegos de matemáticas, lenguaje, ciencias y emociones.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = UniversoProfundo.copy(alpha = 0.58f)
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(color = MagicalPurple, shape = RoundedCornerShape(12.dp)) {
                Text(
                    text = "ABRIR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ZaziliWhite,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PIEZAS COMPARTIDAS
// ---------------------------------------------------------------------------

@Composable
private fun economiaTech() =
    (LocalContext.current.applicationContext as ZaziliApplication).container.economiaZazoles

@Composable
private fun EncabezadoJuego(
    titulo: String,
    descripcion: String,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    contenidoDerecha: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotonRegreso(onSalir = onSalir)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = descripcion,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f)
            )
        }
        contenidoDerecha?.invoke()
    }
}

@Composable
private fun BotonRegreso(onSalir: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                CircleShape
            )
            .clickable(onClickLabel = "Volver") {
                GlobalAudioManager.playPop()
                onSalir()
            },
        contentAlignment = Alignment.Center
    ) {
        KawaiiArrowIcon(
            modifier = Modifier.rotate(180f),
            size = 20.dp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChipZazoles(zazoles: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(AmarilloSuave)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⭐", fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$zazoles",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = UniversoProfundo
        )
    }
}

@Composable
private fun DatoTech(
    emoji: String,
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            text = valor,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = etiqueta,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
        )
    }
}

// ---------------------------------------------------------------------------
//  JUEGO 1: APLASTA EL BUG (WHACK-A-BUG)
// ---------------------------------------------------------------------------

private const val CELDAS_APLASTA = 9
private const val SEGUNDOS_PARTIDA_APLASTA = 30
private const val PUNTOS_BUG = 10
private const val CASTIGO_SISTEMA = 20
private const val PREFERENCIAS_ARENA_TECH = "arena_tech_records"
private const val LLAVE_RECORD_APLASTA = "record_aplasta_bug"

enum class FaseAplastaBug { LISTO, JUGANDO, TERMINADO }

enum class TipoCeldaBug { VACIA, BUG, SISTEMA_SEGURO }

data class CeldaBug(
    val sello: Long = 0L,
    val tipo: TipoCeldaBug = TipoCeldaBug.VACIA
)

data class EstadoAplastaBug(
    val fase: FaseAplastaBug = FaseAplastaBug.LISTO,
    val celdas: List<CeldaBug> = List(CELDAS_APLASTA) { CeldaBug() },
    val puntos: Int = 0,
    val bugsAplastados: Int = 0,
    val sistemasTocados: Int = 0,
    val segundosRestantes: Int = SEGUNDOS_PARTIDA_APLASTA,
    val record: Int = 0,
    val ronda: Int = 0
)

/**
 * Motor de Aplasta el Bug.
 *
 * Los timers viven en `LaunchedEffect` de la pantalla: un lazo brota un bicho
 * cada 620 ms y otro descuenta el reloj cada segundo. Cada celda lleva un
 * `sello` único y caduca exactamente 1 segundo después de nacer.
 *
 * Aplastar un bug otorga 1 Zazol real; tocar el escudo del sistema resta
 * puntos (no Zazoles, porque la bolsa nunca puede quedar negativa).
 */
class AplastaElBugViewModel(application: Application) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container
    private val economia = contenedor.economiaZazoles
    private val azar = Random(System.nanoTime())
    private val preferencias = application.getSharedPreferences(
        PREFERENCIAS_ARENA_TECH,
        Context.MODE_PRIVATE
    )

    private var siguienteSello = 1L

    private val _estado = MutableStateFlow(
        EstadoAplastaBug(record = preferencias.getInt(LLAVE_RECORD_APLASTA, 0))
    )
    val estado: StateFlow<EstadoAplastaBug> = _estado.asStateFlow()

    fun iniciarPartida() {
        _estado.update {
            EstadoAplastaBug(
                fase = FaseAplastaBug.JUGANDO,
                record = it.record,
                ronda = it.ronda + 1
            )
        }
    }

    fun brotarBug() {
        val actual = _estado.value
        if (actual.fase != FaseAplastaBug.JUGANDO) return
        val libres = actual.celdas.indices.filter {
            actual.celdas[it].tipo == TipoCeldaBug.VACIA
        }
        if (libres.isEmpty()) return
        val indice = libres[azar.nextInt(libres.size)]
        val tipo = if (azar.nextInt(100) < 78) {
            TipoCeldaBug.BUG
        } else {
            TipoCeldaBug.SISTEMA_SEGURO
        }
        val sello = siguienteSello++
        _estado.update { s ->
            if (s.fase != FaseAplastaBug.JUGANDO) {
                s
            } else {
                s.copy(
                    celdas = s.celdas.toMutableList().also {
                        it[indice] = CeldaBug(sello = sello, tipo = tipo)
                    }
                )
            }
        }
    }

    fun caducar(indice: Int, sello: Long) {
        _estado.update { s ->
            val celda = s.celdas.getOrNull(indice)
            if (celda == null || celda.sello != sello || s.fase != FaseAplastaBug.JUGANDO) {
                s
            } else {
                s.copy(celdas = s.celdas.vaciando(indice))
            }
        }
    }

    fun aplastar(indice: Int) {
        val actual = _estado.value
        if (actual.fase != FaseAplastaBug.JUGANDO) return
        val celda = actual.celdas.getOrNull(indice) ?: return
        if (celda.tipo == TipoCeldaBug.VACIA) return
        if (celda.tipo == TipoCeldaBug.BUG) {
            economia.ganar(1)
            GlobalAudioManager.playPop()
            _estado.update { s ->
                s.copy(
                    celdas = s.celdas.vaciando(indice),
                    puntos = s.puntos + PUNTOS_BUG,
                    bugsAplastados = s.bugsAplastados + 1
                )
            }
        } else {
            GlobalAudioManager.playMiss()
            _estado.update { s ->
                s.copy(
                    celdas = s.celdas.vaciando(indice),
                    puntos = (s.puntos - CASTIGO_SISTEMA).coerceAtLeast(0),
                    sistemasTocados = s.sistemasTocados + 1
                )
            }
        }
    }

    fun descontarSegundo() {
        val actual = _estado.value
        if (actual.fase != FaseAplastaBug.JUGANDO) return
        val restante = actual.segundosRestantes - 1
        if (restante <= 0) terminar() else _estado.update { it.copy(segundosRestantes = restante) }
    }

    private fun terminar() {
        _estado.update { s ->
            val record = maxOf(s.record, s.puntos)
            preferencias
                .edit()
                .putInt(LLAVE_RECORD_APLASTA, record)
                .apply()
            s.copy(
                fase = FaseAplastaBug.TERMINADO,
                segundosRestantes = 0,
                celdas = List(CELDAS_APLASTA) { CeldaBug() },
                record = record
            )
        }
    }

    private fun List<CeldaBug>.vaciando(indice: Int): List<CeldaBug> =
        toMutableList().also { it[indice] = CeldaBug() }
}

@Composable
fun AplastaElBugScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    vistaModelo: AplastaElBugViewModel = viewModel()
) {
    val estado by vistaModelo.estado.collectAsStateWithLifecycle()
    val economia = economiaTech()
    val zazoles by economia.zazoles.collectAsStateWithLifecycle()

    // Reloj rápido: un bicho nuevo cada 620 ms.
    LaunchedEffect(estado.fase, estado.ronda) {
        if (estado.fase != FaseAplastaBug.JUGANDO) return@LaunchedEffect
        while (isActive && vistaModelo.estado.value.fase == FaseAplastaBug.JUGANDO) {
            delay(620L)
            vistaModelo.brotarBug()
        }
    }

    // Cuenta regresiva de 30 segundos.
    LaunchedEffect(estado.fase, estado.ronda) {
        if (estado.fase != FaseAplastaBug.JUGANDO) return@LaunchedEffect
        while (isActive && vistaModelo.estado.value.fase == FaseAplastaBug.JUGANDO) {
            delay(1000L)
            vistaModelo.descontarSegundo()
        }
    }

    LaunchedEffect(estado.fase) {
        if (estado.fase == FaseAplastaBug.TERMINADO) {
            GlobalAudioManager.playSuccess()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EncabezadoJuego(
                titulo = "Aplasta el Bug",
                descripcion = "Reflejos de colibrí digital",
                onSalir = onSalir,
                contenidoDerecha = { ChipZazoles(zazoles) }
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoTech("⏱", "${estado.segundosRestantes}s", "Tiempo", Modifier.weight(1f))
                DatoTech("🐞", "${estado.bugsAplastados}", "Bugs", Modifier.weight(1f))
                DatoTech("🎯", "${estado.puntos}", "Puntos", Modifier.weight(1f))
            }

            TableroAplasta(
                estado = estado,
                onAplastar = vistaModelo::aplastar,
                onCaducar = vistaModelo::caducar,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Text(
                text = "Toca los bichos 🐞 antes de que se escondan. ¡No toques el escudo 🛡️!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (estado.fase != FaseAplastaBug.JUGANDO) {
            PanelAplasta(
                estado = estado,
                onJugar = vistaModelo::iniciarPartida,
                onSalir = onSalir,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TableroAplasta(
    estado: EstadoAplastaBug,
    onAplastar: (Int) -> Unit,
    onCaducar: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                2.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(28.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { fila ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { col ->
                    val indice = fila * 3 + col
                    CeldaAplasta(
                        celda = estado.celdas[indice],
                        indice = indice,
                        habilitada = estado.fase == FaseAplastaBug.JUGANDO,
                        onAplastar = onAplastar,
                        onCaducar = onCaducar,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun CeldaAplasta(
    celda: CeldaBug,
    indice: Int,
    habilitada: Boolean,
    onAplastar: (Int) -> Unit,
    onCaducar: (Int, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val oscuro = isSystemInDarkTheme()
    val fondo = if (oscuro) UniversoMedio else LavandaClara

    // Cada bicho vive exactamente 1 segundo; el efecto se reinicia con su
    // sello, así que nunca caduca a un hermano recién nacido.
    LaunchedEffect(celda.sello) {
        if (celda.sello != 0L) {
            delay(1000L)
            onCaducar(indice, celda.sello)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fondo)
            .border(
                2.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                RoundedCornerShape(20.dp)
            )
            .then(
                if (habilitada && celda.tipo != TipoCeldaBug.VACIA) {
                    Modifier.clickable { onAplastar(indice) }
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = when (celda.tipo) {
                    TipoCeldaBug.BUG ->
                        "Bug en la celda ${indice + 1}. Tócalo para aplastarlo."
                    TipoCeldaBug.SISTEMA_SEGURO ->
                        "Escudo de sistema seguro en la celda ${indice + 1}. No lo toques."
                    TipoCeldaBug.VACIA -> "Celda ${indice + 1} vacía."
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = celda.tipo,
            transitionSpec = {
                (scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f)
                ) + fadeIn(tween(90))) togetherWith
                    (scaleOut(
                        targetScale = 0.6f,
                        animationSpec = tween(110)
                    ) + fadeOut(tween(110)))
            },
            label = "aparicionBug",
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { tipo ->
            when (tipo) {
                TipoCeldaBug.BUG -> Text("🐞", fontSize = 34.sp)
                TipoCeldaBug.SISTEMA_SEGURO -> Text("🛡️", fontSize = 30.sp)
                TipoCeldaBug.VACIA -> Spacer(Modifier.size(1.dp))
            }
        }
    }
}

@Composable
private fun PanelAplasta(
    estado: EstadoAplastaBug,
    onJugar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val terminado = estado.fase == FaseAplastaBug.TERMINADO
    Box(
        modifier.background(UniversoProfundo.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZaziSpriteAnimado(
                    size = 108.dp,
                    estado = if (terminado) ZaziState.CELEBRACION else ZaziState.VUELO_IDLE,
                    descripcion = if (terminado) {
                        "Zazi celebra tu puntaje"
                    } else {
                        "Zazi se prepara para jugar"
                    }
                )
                Text(
                    text = if (terminado) "¡Ronda terminada!" else "Aplasta el Bug",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (terminado) {
                        "Aplastaste ${estado.bugsAplastados} bugs y lograste ${estado.puntos} puntos."
                    } else {
                        "Los bugs se asoman por solo 1 segundo. " +
                            "¡Atrápalos todos en 30 segundos y evita los escudos!"
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                if (terminado) {
                    Text(
                        text = "🏆 Récord personal: ${estado.record}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = VerdeDuoOscuro
                    )
                }
                ZaziButton(
                    text = if (terminado) "Jugar otra vez" else "¡Jugar!",
                    onClick = onJugar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    containerColor = VerdeDuo
                )
                TextButton(onClick = onSalir) {
                    Text(
                        text = "Volver a los juegos",
                        color = MagicalPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  JUEGO 2: RUTA DEL ALGORITMO
// ---------------------------------------------------------------------------

private const val LADO_RUTA = 5

enum class ComandoRuta(
    val etiqueta: String,
    val flecha: String,
    val dx: Int,
    val dy: Int
) {
    ARRIBA("Arriba", "▲", 0, -1),
    ABAJO("Abajo", "▼", 0, 1),
    IZQUIERDA("Izquierda", "◀", -1, 0),
    DERECHA("Derecha", "▶", 1, 0)
}

data class NivelRuta(
    val inicio: IntOffset,
    val bateria: IntOffset,
    val muros: Set<IntOffset>,
    val maxComandos: Int = 8
)

/**
 * Tres rutas con solución de máximo 8 comandos. Los muros empujan al niño a
 * pensar en secuencias reales, no solo en ir derecho.
 */
private val NIVELES_RUTA = listOf(
    NivelRuta(
        inicio = IntOffset(0, 0),
        bateria = IntOffset(4, 4),
        muros = setOf(
            IntOffset(1, 1),
            IntOffset(2, 1),
            IntOffset(2, 2),
            IntOffset(3, 3),
            IntOffset(1, 3)
        )
    ),
    NivelRuta(
        inicio = IntOffset(0, 4),
        bateria = IntOffset(4, 0),
        muros = setOf(
            IntOffset(1, 3),
            IntOffset(2, 3),
            IntOffset(3, 2),
            IntOffset(1, 1),
            IntOffset(2, 1)
        )
    ),
    NivelRuta(
        inicio = IntOffset(2, 4),
        bateria = IntOffset(2, 0),
        muros = setOf(
            IntOffset(1, 3),
            IntOffset(3, 3),
            IntOffset(0, 2),
            IntOffset(4, 2),
            IntOffset(1, 1),
            IntOffset(3, 1)
        )
    )
)

enum class ResultadoRuta { EN_CURSO, GANADA, CHOQUE, SIN_BATERIA }

data class EstadoRutaAlgoritmo(
    val indiceNivel: Int = 0,
    val comandos: List<ComandoRuta> = emptyList(),
    val posicion: IntOffset = NIVELES_RUTA.first().inicio,
    val pasoActual: Int = 0,
    val ejecutando: Boolean = false,
    val resultado: ResultadoRuta = ResultadoRuta.EN_CURSO
)

/**
 * Motor de Ruta del Algoritmo.
 *
 * El niño compone una lista de comandos (arrastrando o tocando bloques) y al
 * tocar "Ejecutar" la corrutina del ViewModel recorre la secuencia con pasos
 * de 420 ms, animando la posición de Zazi en pantalla. Un choque o salirse
 * del tablero detiene la corrutina con resultado [ResultadoRuta.CHOQUE].
 * Llegar a la batería otorga 3 Zazoles, una sola vez por ejecución.
 */
class RutaDelAlgoritmoViewModel(application: Application) : AndroidViewModel(application) {

    private val economia = (application as ZaziliApplication).container.economiaZazoles

    private val _estado = MutableStateFlow(EstadoRutaAlgoritmo())
    val estado: StateFlow<EstadoRutaAlgoritmo> = _estado.asStateFlow()

    private var trabajo: Job? = null

    private fun nivelActual(): NivelRuta = NIVELES_RUTA[_estado.value.indiceNivel]

    fun agregarComando(comando: ComandoRuta) {
        val actual = _estado.value
        if (actual.ejecutando || actual.resultado != ResultadoRuta.EN_CURSO) return
        if (actual.comandos.size >= nivelActual().maxComandos) {
            GlobalAudioManager.playMiss()
            return
        }
        GlobalAudioManager.playPop()
        _estado.update { it.copy(comandos = it.comandos + comando) }
    }

    fun quitarComando(indice: Int) {
        val actual = _estado.value
        if (actual.ejecutando || actual.resultado != ResultadoRuta.EN_CURSO) return
        _estado.update { s ->
            if (indice in s.comandos.indices) {
                s.copy(
                    comandos = s.comandos.toMutableList().also { it.removeAt(indice) }
                )
            } else {
                s
            }
        }
    }

    fun limpiar() {
        if (_estado.value.ejecutando) return
        val nivel = nivelActual()
        _estado.update {
            it.copy(
                comandos = emptyList(),
                resultado = ResultadoRuta.EN_CURSO,
                posicion = nivel.inicio,
                pasoActual = 0
            )
        }
    }

    fun ejecutar() {
        val inicial = _estado.value
        if (inicial.ejecutando || inicial.comandos.isEmpty()) return
        if (inicial.resultado != ResultadoRuta.EN_CURSO) return
        val nivel = nivelActual()
        val comandos = inicial.comandos

        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            _estado.update {
                it.copy(
                    ejecutando = true,
                    resultado = ResultadoRuta.EN_CURSO,
                    posicion = nivel.inicio,
                    pasoActual = 0
                )
            }
            delay(200)
            var pos = nivel.inicio
            var choco = false
            for ((indice, comando) in comandos.withIndex()) {
                val destino = IntOffset(pos.x + comando.dx, pos.y + comando.dy)
                choco = destino.x !in 0 until LADO_RUTA ||
                    destino.y !in 0 until LADO_RUTA ||
                    destino in nivel.muros
                if (choco) break
                pos = destino
                _estado.update { it.copy(posicion = pos, pasoActual = indice + 1) }
                delay(420)
            }
            if (choco) {
                _estado.update {
                    it.copy(ejecutando = false, resultado = ResultadoRuta.CHOQUE)
                }
            } else if (pos == nivel.bateria) {
                economia.ganar(3)
                _estado.update {
                    it.copy(ejecutando = false, resultado = ResultadoRuta.GANADA)
                }
            } else {
                _estado.update {
                    it.copy(ejecutando = false, resultado = ResultadoRuta.SIN_BATERIA)
                }
            }
        }
    }

    fun reiniciarNivel() {
        trabajo?.cancel()
        _estado.value = EstadoRutaAlgoritmo(indiceNivel = _estado.value.indiceNivel)
    }

    fun siguienteNivel() {
        trabajo?.cancel()
        _estado.value = EstadoRutaAlgoritmo(
            indiceNivel = (_estado.value.indiceNivel + 1) % NIVELES_RUTA.size
        )
    }

    override fun onCleared() {
        trabajo?.cancel()
        super.onCleared()
    }
}

@Composable
fun RutaDelAlgoritmoScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    vistaModelo: RutaDelAlgoritmoViewModel = viewModel()
) {
    val estado by vistaModelo.estado.collectAsStateWithLifecycle()
    val nivel = NIVELES_RUTA[estado.indiceNivel]
    val economia = economiaTech()
    val zazoles by economia.zazoles.collectAsStateWithLifecycle()

    var zonaFila by remember { mutableStateOf(Rect.Zero) }
    var arrastrando by remember { mutableStateOf(false) }

    LaunchedEffect(estado.resultado) {
        when (estado.resultado) {
            ResultadoRuta.GANADA -> GlobalAudioManager.playSuccess()
            ResultadoRuta.CHOQUE, ResultadoRuta.SIN_BATERIA ->
                GlobalAudioManager.playMiss()
            ResultadoRuta.EN_CURSO -> Unit
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EncabezadoJuego(
                titulo = "Ruta del Algoritmo",
                descripcion = "Ruta ${estado.indiceNivel + 1} de ${NIVELES_RUTA.size}",
                onSalir = onSalir,
                contenidoDerecha = { ChipZazoles(zazoles) }
            )

            Text(
                text = "Arrastra flechas para guiar a Zazi hasta la batería 🔋 " +
                    "(máximo ${nivel.maxComandos} comandos).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val lado = minOf(maxWidth, maxHeight)
                TableroRuta(
                    estado = estado,
                    nivel = nivel,
                    modifier = Modifier.size(lado)
                )
            }

            FilaComandos(
                comandos = estado.comandos,
                maxComandos = nivel.maxComandos,
                ejecutando = estado.ejecutando,
                pasoActual = estado.pasoActual,
                resaltada = arrastrando,
                onZonaMedida = { zonaFila = it },
                onQuitar = vistaModelo::quitarComando,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ComandoRuta.entries.forEach { comando ->
                    BloqueComando(
                        comando = comando,
                        habilitado = !estado.ejecutando &&
                            estado.resultado == ResultadoRuta.EN_CURSO &&
                            estado.comandos.size < nivel.maxComandos,
                        zonaDestino = { zonaFila },
                        onInsertar = vistaModelo::agregarComando,
                        onArrastreCambia = { arrastrando = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ZaziButton(
                text = if (estado.ejecutando) "Ejecutando…" else "Ejecutar ruta",
                onClick = vistaModelo::ejecutar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                containerColor = VerdeDuo,
                enabled = !estado.ejecutando &&
                    estado.comandos.isNotEmpty() &&
                    estado.resultado == ResultadoRuta.EN_CURSO
            )
            TextButton(
                onClick = vistaModelo::limpiar,
                enabled = !estado.ejecutando && estado.comandos.isNotEmpty(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Borrar comandos",
                    color = MagicalPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (estado.resultado != ResultadoRuta.EN_CURSO) {
            PanelResultadoRuta(
                resultado = estado.resultado,
                esUltimaRuta = estado.indiceNivel == NIVELES_RUTA.lastIndex,
                onRepetir = vistaModelo::reiniciarNivel,
                onSiguiente = vistaModelo::siguienteNivel,
                onSalir = onSalir,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TableroRuta(
    estado: EstadoRutaAlgoritmo,
    nivel: NivelRuta,
    modifier: Modifier = Modifier
) {
    val oscuro = isSystemInDarkTheme()
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(if (oscuro) UniversoMedio else LavandaClara)
            .border(
                2.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                RoundedCornerShape(26.dp)
            )
    ) {
        val celda = maxWidth / LADO_RUTA
        Column(Modifier.fillMaxSize()) {
            repeat(LADO_RUTA) { fila ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    repeat(LADO_RUTA) { col ->
                        val coord = IntOffset(col, fila)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colorDeCeldaRuta(coord, nivel, oscuro)),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                coord in nivel.muros -> Text("🧱", fontSize = 18.sp)
                                coord == nivel.bateria -> Text("🔋", fontSize = 22.sp)
                                coord == nivel.inicio -> Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MagicalPurple.copy(alpha = 0.45f))
                                )
                            }
                        }
                    }
                }
            }
        }

        val xZazi by animateDpAsState(
            targetValue = celda * estado.posicion.x,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "zaziX"
        )
        val yZazi by animateDpAsState(
            targetValue = celda * estado.posicion.y,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "zaziY"
        )

        Box(
            Modifier
                .offset(x = xZazi, y = yZazi)
                .size(celda)
                .padding(5.dp)
        ) {
            ZaziSpriteAnimado(
                modifier = Modifier.fillMaxSize(),
                estado = if (estado.resultado == ResultadoRuta.GANADA) {
                    ZaziState.CELEBRACION
                } else {
                    ZaziState.VUELO_IDLE
                },
                descripcion = "Zazi en el tablero"
            )
        }
    }
}

private fun colorDeCeldaRuta(coord: IntOffset, nivel: NivelRuta, oscuro: Boolean): Color =
    when {
        coord in nivel.muros -> CoralAviso.copy(alpha = if (oscuro) 0.35f else 0.28f)
        coord == nivel.bateria -> AmarilloSuave
        coord == nivel.inicio -> MintGreen.copy(alpha = if (oscuro) 0.35f else 0.55f)
        else -> if (oscuro) UniversoMedio else ZaziliWhite
    }

private fun colorDeComando(comando: ComandoRuta): Color = when (comando) {
    ComandoRuta.ARRIBA -> AzulCielo
    ComandoRuta.ABAJO -> AmarilloSuave
    ComandoRuta.IZQUIERDA -> RosaSuave
    ComandoRuta.DERECHA -> MintGreen
}

@Composable
private fun FilaComandos(
    comandos: List<ComandoRuta>,
    maxComandos: Int,
    ejecutando: Boolean,
    pasoActual: Int,
    resaltada: Boolean,
    onZonaMedida: (Rect) -> Unit,
    onQuitar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            2.dp,
            if (resaltada) VerdeDuo else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .onGloballyPositioned { onZonaMedida(it.boundsInRoot()) }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (comandos.isEmpty()) {
                Text(
                    text = "Arrastra las flechas aquí",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(start = 6.dp)
                )
            } else {
                comandos.forEachIndexed { indice, comando ->
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                colorDeComando(comando)
                                    .copy(alpha = if (indice < pasoActual) 0.45f else 1f)
                            )
                            .then(
                                if (!ejecutando) {
                                    Modifier.clickable { onQuitar(indice) }
                                } else {
                                    Modifier
                                }
                            )
                            .semantics {
                                contentDescription =
                                    "Comando ${indice + 1}: ${comando.etiqueta}. " +
                                        "Toca para quitarlo."
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = comando.flecha,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = UniversoProfundo
                        )
                    }
                }
            }
            Text(
                text = "${comandos.size}/$maxComandos",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Bloque de comando arrastrable. Usa el patrón obligatorio del proyecto:
 * `Animatable(Offset.Zero)` + rectángulos globales + regreso con muelle.
 * Soltar sobre la fila inserta el comando; cualquier otro punto devuelve el
 * bloque a su lugar con un muelle. Tocar el bloque también lo inserta.
 */
@Composable
private fun BloqueComando(
    comando: ComandoRuta,
    habilitado: Boolean,
    zonaDestino: () -> Rect,
    onInsertar: (ComandoRuta) -> Unit,
    onArrastreCambia: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val ambito = rememberCoroutineScope()
    val desplazamiento = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var limites by remember { mutableStateOf(Rect.Zero) }
    val habilitadoActual by rememberUpdatedState(habilitado)
    val zonaActual by rememberUpdatedState(zonaDestino)
    val insertarActual by rememberUpdatedState(onInsertar)
    val arrastreActual by rememberUpdatedState(onArrastreCambia)

    Box(
        modifier = modifier
            .height(64.dp)
            .onGloballyPositioned { limites = it.boundsInRoot() }
            .graphicsLayer {
                translationX = desplazamiento.value.x
                translationY = desplazamiento.value.y
            }
            .clip(RoundedCornerShape(18.dp))
            .background(colorDeComando(comando))
            .border(
                2.dp,
                UniversoProfundo.copy(alpha = 0.12f),
                RoundedCornerShape(18.dp)
            )
            .clickable(enabled = habilitado) { insertarActual(comando) }
            .pointerInput(comando, habilitado) {
                if (!habilitadoActual) return@pointerInput
                detectDragGestures(
                    onDragStart = { arrastreActual(true) },
                    onDragEnd = {
                        val movido = Rect(
                            left = limites.left + desplazamiento.value.x,
                            top = limites.top + desplazamiento.value.y,
                            right = limites.right + desplazamiento.value.x,
                            bottom = limites.bottom + desplazamiento.value.y
                        )
                        if (movido.overlaps(zonaActual())) {
                            insertarActual(comando)
                        } else {
                            GlobalAudioManager.playMiss()
                        }
                        arrastreActual(false)
                        ambito.launch {
                            desplazamiento.animateTo(
                                Offset.Zero,
                                spring(dampingRatio = 0.55f, stiffness = 550f)
                            )
                        }
                    },
                    onDragCancel = {
                        arrastreActual(false)
                        ambito.launch {
                            desplazamiento.animateTo(Offset.Zero, spring())
                        }
                    },
                    onDrag = { cambio, arrastre ->
                        cambio.consume()
                        ambito.launch {
                            desplazamiento.snapTo(
                                desplazamiento.value + Offset(arrastre.x, arrastre.y)
                            )
                        }
                    }
                )
            }
            .semantics {
                contentDescription =
                    "Comando ${comando.etiqueta}. " +
                        "Arrástralo a la fila o tócalo para agregarlo."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = comando.flecha,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = UniversoProfundo
            )
            Text(
                text = comando.etiqueta,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = UniversoProfundo.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun PanelResultadoRuta(
    resultado: ResultadoRuta,
    esUltimaRuta: Boolean,
    onRepetir: () -> Unit,
    onSiguiente: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.background(UniversoProfundo.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZaziSpriteAnimado(
                    size = 104.dp,
                    estado = if (resultado == ResultadoRuta.GANADA) {
                        ZaziState.CELEBRACION
                    } else {
                        ZaziState.PENSANDO
                    },
                    descripcion = "Zazi evalúa tu ruta"
                )
                Text(
                    text = when (resultado) {
                        ResultadoRuta.GANADA -> "¡Llegaste a la batería!"
                        ResultadoRuta.CHOQUE -> "¡Pum! Un muro en el camino"
                        else -> "Casi lo logras"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when (resultado) {
                        ResultadoRuta.GANADA ->
                            "Tu algoritmo funcionó. " +
                                "¡Zazi recuperó la energía y ganó 3 Zazoles!"
                        ResultadoRuta.CHOQUE ->
                            "Un comando chocó contra un muro o salió del " +
                                "tablero. Revisa la ruta e inténtalo de nuevo."
                        else ->
                            "Zazi no llegó a la batería. " +
                                "Agrega o quita flechas e inténtalo otra vez."
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                ZaziButton(
                    text = when (resultado) {
                        ResultadoRuta.GANADA ->
                            if (esUltimaRuta) "Jugar desde la primera ruta" else "Siguiente ruta"
                        else -> "Intentar otra vez"
                    },
                    onClick = if (resultado == ResultadoRuta.GANADA) onSiguiente else onRepetir,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    containerColor = VerdeDuo
                )
                TextButton(onClick = onSalir) {
                    Text(
                        text = "Volver a los juegos",
                        color = MagicalPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  JUEGO 3: FILTRO CIBER-SEGURO (SWIPE GAME)
// ---------------------------------------------------------------------------

data class EscenarioCiber(
    val id: Int,
    val remitente: String,
    val titulo: String,
    val detalle: String,
    val emoji: String,
    val esSeguro: Boolean
)

/**
 * Doce mensajes mexicanos, mezcla de seguros y engaños reales: phishing,
 * "cuento del tío", datos personales y cuentos de premios. Los seguros son
 * mensajes cotidianos de Zazi, la escuela y el sistema.
 */
private val ESCENARIOS_CYBER = listOf(
    EscenarioCiber(1, "Zazi", "Mensaje de Zazi", "¡Hola! Te espero en la Arena Cortex para jugar juntos.", "💌", true),
    EscenarioCiber(2, "Escuela", "Recordatorio de tarea", "Tu maestra envió la tarea de hoy por la aplicación oficial de la escuela.", "📚", true),
    EscenarioCiber(3, "Sistema", "Actualización de seguridad", "La tablet encontró una actualización oficial. Pide a un adulto que la instale.", "⬆️", true),
    EscenarioCiber(4, "Nido", "Recado de casa", "El Nido te recuerda lavarte las manos antes de comer.", "🏠", true),
    EscenarioCiber(5, "Biblioteca", "Cuento nuevo", "Hay un cuento nuevo en la Cueva de las Leyendas. ¡Pásate a leerlo!", "📖", true),
    EscenarioCiber(6, "Desconocido", "¡Ganaste 10,000 Zazoles!", "Escribe la contraseña de tu mamá para recibir el premio ahora mismo.", "🎉", false),
    EscenarioCiber(7, "Desconocido", "Soy tu amigo nuevo", "Mándame una foto tuya y dime en qué escuela estudias.", "📷", false),
    EscenarioCiber(8, "Correo raro", "Tu cuenta se cierra hoy", "Toca este enlace con letras raras o perderás todos tus juegos.", "⚠️", false),
    EscenarioCiber(9, "Sorteo", "Tablet gratis", "¡Toca el enlace para ganar una tablet! Solo necesitamos tus datos.", "📱", false),
    EscenarioCiber(10, "Desconocido", "Regalo sorpresa", "Dame tu dirección para enviarte un regalo. No le digas a nadie.", "🧸", false),
    EscenarioCiber(11, "Anuncio", "Juego fuera de la tienda", "Descarga este juego desde un enlace que no es la tienda oficial.", "🕹️", false),
    EscenarioCiber(12, "Banco de Zazoles", "Paga para desbloquear", "Pide la tarjeta de un adulto para desbloquear premios secretos.", "💳", false)
)

data class RetroalimentacionCiber(val correcto: Boolean, val texto: String)

data class EstadoFiltroCiber(
    val mazo: List<EscenarioCiber>,
    val indice: Int = 0,
    val aciertos: Int = 0,
    val fallos: Int = 0,
    val racha: Int = 0,
    val mejorRacha: Int = 0,
    val puntos: Int = 0,
    val retroalimentacion: RetroalimentacionCiber? = null,
    val terminado: Boolean = false,
    val ronda: Int = 0
) {
    val cartaActual: EscenarioCiber? get() = mazo.getOrNull(indice)
    val total: Int get() = mazo.size
    val progreso: Float get() = if (total == 0) 0f else indice.toFloat() / total
}

/**
 * Motor de Filtro Ciber-Seguro. Deslizar a la derecha acepta y a la
 * izquierda bloquea; acierto = 10 puntos + 1 Zazol. El mazo se baraja en cada
 * ronda, así que memorizar el orden no sirve: hay que leer.
 */
class FiltroCiberSeguroViewModel(application: Application) : AndroidViewModel(application) {

    private val economia = (application as ZaziliApplication).container.economiaZazoles
    private val azar = Random(System.nanoTime())

    private val _estado = MutableStateFlow(EstadoFiltroCiber(mazo = barajar()))
    val estado: StateFlow<EstadoFiltroCiber> = _estado.asStateFlow()

    private fun barajar(): List<EscenarioCiber> = ESCENARIOS_CYBER.shuffled(azar)

    fun decidir(aceptar: Boolean) {
        val actual = _estado.value
        if (actual.terminado) return
        val carta = actual.cartaActual ?: return
        val correcto = carta.esSeguro == aceptar
        if (correcto) {
            economia.ganar(1)
            GlobalAudioManager.playSuccess()
        } else {
            GlobalAudioManager.playMiss()
        }
        val siguiente = actual.indice + 1
        val rachaNueva = if (correcto) actual.racha + 1 else 0
        _estado.value = actual.copy(
            indice = siguiente,
            aciertos = actual.aciertos + if (correcto) 1 else 0,
            fallos = actual.fallos + if (correcto) 0 else 1,
            racha = rachaNueva,
            mejorRacha = maxOf(actual.mejorRacha, rachaNueva),
            puntos = actual.puntos + if (correcto) 10 else 0,
            retroalimentacion = RetroalimentacionCiber(
                correcto = correcto,
                texto = when {
                    correcto && aceptar -> "¡Bien! \"${carta.titulo}\" sí era seguro."
                    correcto -> "¡Bien! \"${carta.titulo}\" era un engaño y lo bloqueaste."
                    aceptar -> "¡Cuidado! \"${carta.titulo}\" era un engaño."
                    else -> "\"${carta.titulo}\" era seguro. Podías aceptarlo sin problema."
                }
            ),
            terminado = siguiente >= actual.total
        )
    }

    fun reiniciar() {
        _estado.value = EstadoFiltroCiber(
            mazo = barajar(),
            ronda = _estado.value.ronda + 1
        )
    }
}

@Composable
fun FiltroCiberSeguroScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    vistaModelo: FiltroCiberSeguroViewModel = viewModel()
) {
    val estado by vistaModelo.estado.collectAsStateWithLifecycle()
    val economia = economiaTech()
    val zazoles by economia.zazoles.collectAsStateWithLifecycle()

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EncabezadoJuego(
                titulo = "Filtro Ciber-Seguro",
                descripcion = "Aciertos ${estado.aciertos} · Racha ${estado.racha}",
                onSalir = onSalir,
                contenidoDerecha = { ChipZazoles(zazoles) }
            )

            LinearProgressIndicator(
                progress = { estado.progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = VerdeDuo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            val retroalimentacion = estado.retroalimentacion
            AnimatedVisibility(
                visible = retroalimentacion != null,
                enter = fadeIn(tween(140)) + expandVertically(tween(160)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(120))
            ) {
                if (retroalimentacion != null) {
                    BannerRetroalimentacion(
                        retroalimentacion = retroalimentacion,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val carta = estado.cartaActual
                if (carta != null && !estado.terminado) {
                    key(carta.id) {
                        TarjetaEscenario(
                            escenario = carta,
                            onDecidir = vistaModelo::decidir,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Desliza para bloquear",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoralAviso
                )
                Text(
                    text = "Desliza para aceptar →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VerdeDuoOscuro
                )
            }
        }

        if (estado.terminado) {
            PanelResultadoFiltro(
                estado = estado,
                onReiniciar = vistaModelo::reiniciar,
                onSalir = onSalir,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun BannerRetroalimentacion(
    retroalimentacion: RetroalimentacionCiber,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (retroalimentacion.correcto) {
                    MintGreen.copy(alpha = 0.85f)
                } else {
                    RosaSuave.copy(alpha = 0.9f)
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (retroalimentacion.correcto) "✅" else "💡", fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = retroalimentacion.texto,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            color = UniversoProfundo
        )
    }
}

@Composable
private fun TarjetaEscenario(
    escenario: EscenarioCiber,
    onDecidir: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val ambito = rememberCoroutineScope()
    val desplazamiento = remember(escenario.id) {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    val decidirActual by rememberUpdatedState(onDecidir)
    var decidido by remember(escenario.id) { mutableStateOf(false) }
    val densidad = LocalDensity.current
    val umbralPx = with(densidad) { 70.dp.toPx() }
    val salidaPx = with(densidad) { 560.dp.toPx() }

    Surface(
        modifier = modifier
            .graphicsLayer {
                translationX = desplazamiento.value.x
                rotationZ = desplazamiento.value.x / 48f
            }
            .pointerInput(escenario.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { cambio, arrastre ->
                        cambio.consume()
                        ambito.launch {
                            desplazamiento.snapTo(
                                Offset(
                                    (desplazamiento.value.x + arrastre).coerceIn(-1100f, 1100f),
                                    0f
                                )
                            )
                        }
                    },
                    onDragEnd = {
                        val x = desplazamiento.value.x
                        if (!decidido) {
                            when {
                                x > umbralPx -> {
                                    decidido = true
                                    ambito.launch {
                                        desplazamiento.animateTo(
                                            Offset(salidaPx, 0f),
                                            tween(210)
                                        )
                                        decidirActual(true)
                                    }
                                }
                                x < -umbralPx -> {
                                    decidido = true
                                    ambito.launch {
                                        desplazamiento.animateTo(
                                            Offset(-salidaPx, 0f),
                                            tween(210)
                                        )
                                        decidirActual(false)
                                    }
                                }
                                else -> ambito.launch {
                                    desplazamiento.animateTo(
                                        Offset.Zero,
                                        spring(dampingRatio = 0.55f, stiffness = 600f)
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        ambito.launch { desplazamiento.animateTo(Offset.Zero, spring()) }
                    }
                )
            }
            .semantics {
                contentDescription = "Mensaje de ${escenario.remitente}: ${escenario.titulo}. " +
                    "Desliza a la izquierda para bloquear o a la derecha para aceptar."
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        ),
        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 6.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(escenario.emoji, fontSize = 52.sp)
                Spacer(Modifier.height(8.dp))
                Surface(color = LavandaClara, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "De: ${escenario.remitente}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = UniversoProfundo,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = escenario.titulo,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = escenario.detalle,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
            EtiquetaSwipe(
                texto = "BLOQUEAR",
                color = CoralAviso,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .graphicsLayer {
                        alpha = ((-desplazamiento.value.x - 20f) / 90f).coerceIn(0f, 1f)
                    }
            )
            EtiquetaSwipe(
                texto = "ACEPTAR",
                color = VerdeDuo,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .graphicsLayer {
                        alpha = ((desplazamiento.value.x - 20f) / 90f).coerceIn(0f, 1f)
                    }
            )
        }
    }
}

@Composable
private fun EtiquetaSwipe(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(2.dp, color),
        modifier = modifier
    ) {
        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PanelResultadoFiltro(
    estado: EstadoFiltroCiber,
    onReiniciar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.background(UniversoProfundo.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ZaziSpriteAnimado(
                    size = 104.dp,
                    estado = ZaziState.CELEBRACION,
                    descripcion = "Zazi celebra tu filtro ciber-seguro"
                )
                Text(
                    text = "¡Filtro completado!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Aciertos: ${estado.aciertos} de ${estado.total}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MagicalPurple
                )
                Text(
                    text = "Mejor racha: ${estado.mejorRacha} · ${estado.puntos} puntos",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                ZaziButton(
                    text = "Jugar otra vez",
                    onClick = onReiniciar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    containerColor = VerdeDuo
                )
                TextButton(onClick = onSalir) {
                    Text(
                        text = "Volver a los juegos",
                        color = MagicalPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
