package com.example.zazili.presentation.tienda

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.mascot.EventoZazi
import com.example.zazili.core.mascot.ZaziPetCareEngine
import com.example.zazili.core.mascot.ZaziPetEngine
import com.example.zazili.core.mascot.ZaziVitals
import com.example.zazili.domain.economia.ResultadoCompra
import com.example.zazili.domain.economia.ZazolEconomyManager
import com.example.zazili.domain.tienda.CatalogoTienditas3B
import com.example.zazili.domain.tienda.ConsumibleTienda
import com.example.zazili.domain.tienda.FondoTienda
import com.example.zazili.domain.tienda.GrupoConsumibles
import com.example.zazili.domain.tienda.PestanaTienda
import com.example.zazili.domain.tienda.RanuraRopa
import com.example.zazili.domain.tienda.RopaTienda
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DoradoBrillo = Color(0xFFFFF8DE)
private val DoradoSuave = Color(0xFFFFE9A8)
private val LilaBrillo = Color(0xFFF7F1FF)
private val LilaMedio = Color(0xFFE9DDFB)

/**
 * Tienditas 3B: comida, cuidado, ropa y fondos para Zazi.
 *
 * El saldo y el estado del motor se observan con `collectAsStateWithLifecycle`:
 * cualquier compra, cambio de ropa o comida se refleja al instante en la
 * cabecera, las tarjetas y [ZaziCompanionView]. Las acciones que dependen del
 * inventario vuelven a leer `estado.value` para no decidir con datos viejos.
 * La comida y el cuidado se entregan arrastrándolos sobre la casita; TalkBack
 * tiene la acción "Dar a Zazi" como alternativa motriz.
 */
@Composable
fun Tienditas3BScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val motor = remember(contexto) { ZaziPetCareEngine.obtener(contexto) }
    val economia = remember(contexto) { ZazolEconomyManager.obtener(contexto) }
    val cuidado by motor.estado.collectAsStateWithLifecycle()
    val bolsa by economia.estado.collectAsStateWithLifecycle()
    val alcance = rememberCoroutineScope()

    var pestana by rememberSaveable { mutableStateOf(PestanaTienda.COMIDA) }
    var zonaZazi by remember { mutableStateOf(Rect.Zero) }
    var arrastre by remember { mutableStateOf<ArrastreActivo?>(null) }
    var confeti by remember { mutableIntStateOf(0) }
    var mensajeFlotante by remember { mutableStateOf<String?>(null) }
    var tokenFeedback by remember { mutableIntStateOf(0) }
    var celebrando by remember { mutableStateOf(false) }
    var compraPendiente by remember { mutableStateOf<CompraPendiente?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var tokenAviso by remember { mutableIntStateOf(0) }

    val vitales = cuidado.vitals
    val densidad = LocalDensity.current
    val ladoFantasmaPx = with(densidad) { 76.dp.toPx() }

    fun avisar(texto: String) {
        aviso = texto
        tokenAviso++
    }

    LaunchedEffect(tokenAviso) {
        val actual = tokenAviso
        if (actual == 0) return@LaunchedEffect
        delay(2800)
        if (tokenAviso == actual) aviso = null
    }

    fun soltarSobreZazi(item: ConsumibleTienda, centro: Offset) {
        if (!zonaZazi.contains(centro)) return
        if (motor.estado.value.vitals.dormida) {
            GlobalAudioManager.playMiss()
            avisar("Zazi está dormidita. Toca el botón del sol para despertarla.")
            return
        }
        alcance.launch {
            val entregado = motor.usarConsumible(item.id, item.efectos)
            if (!entregado) {
                GlobalAudioManager.playMiss()
                avisar("Ya no te queda ${item.nombre}. Compra más en la tienda.")
                return@launch
            }
            GlobalAudioManager.playSuccess()
            confeti++
            mensajeFlotante = ZaziPetEngine.frase(EventoZazi.ALIMENTAR)
            tokenFeedback++
            celebrando = true
            delay(1900)
            celebrando = false
        }
    }

    fun comprar(pendiente: CompraPendiente) {
        compraPendiente = null
        alcance.launch {
            when (pendiente) {
                is CompraPendiente.Consumible -> {
                    if (!economia.deductZazoles(pendiente.precio)) {
                        GlobalAudioManager.playMiss()
                        avisar("Te faltan Zazoles. Completa una actividad y vuelve.")
                        return@launch
                    }
                    motor.comprarConsumible(pendiente.item.id)
                    GlobalAudioManager.playPop()
                    avisar("¡Ya tienes ${pendiente.nombre}! Arrástralo hasta Zazi.")
                }

                is CompraPendiente.Prenda -> {
                    when (val resultado = economia.unlockItem(pendiente.item.id, pendiente.precio)) {
                        is ResultadoCompra.Exito,
                        ResultadoCompra.YaDesbloqueado -> {
                            motor.registrarCompraRopa(
                                pendiente.item.id,
                                pendiente.item.ranura.clave
                            )
                            GlobalAudioManager.playSuccess()
                            avisar("¡${pendiente.nombre} se ve increíble en Zazi!")
                        }

                        is ResultadoCompra.SinFondos -> {
                            GlobalAudioManager.playMiss()
                            avisar("Te faltan ${resultado.faltan} Zazoles para esa prenda.")
                        }

                        ResultadoCompra.NoExiste -> {
                            GlobalAudioManager.playMiss()
                            avisar("No pudimos completar la compra. Inténtalo otra vez.")
                        }
                    }
                }

                is CompraPendiente.Fondo -> {
                    when (val resultado = economia.unlockItem(pendiente.item.id, pendiente.precio)) {
                        is ResultadoCompra.Exito,
                        ResultadoCompra.YaDesbloqueado -> {
                            motor.registrarCompraFondo(pendiente.item.id)
                            GlobalAudioManager.playSuccess()
                            avisar("¡La casita ahora tiene ${pendiente.nombre}!")
                        }

                        is ResultadoCompra.SinFondos -> {
                            GlobalAudioManager.playMiss()
                            avisar("Te faltan ${resultado.faltan} Zazoles para ese fondo.")
                        }

                        ResultadoCompra.NoExiste -> {
                            GlobalAudioManager.playMiss()
                            avisar("No pudimos completar la compra. Inténtalo otra vez.")
                        }
                    }
                }
            }
        }
    }

    fun accionPrenda(item: RopaTienda) {
        val actual = motor.estado.value
        val desbloqueada = item.id in actual.ropaComprada ||
            item.id in economia.estado.value.desbloqueados
        if (!desbloqueada) {
            GlobalAudioManager.playPop()
            compraPendiente = CompraPendiente.Prenda(item)
            return
        }
        val puesta = actual.ropaEquipada[item.ranura.clave] == item.id
        alcance.launch {
            when {
                puesta -> motor.quitarRopa(item.ranura.clave)
                item.id in actual.ropaComprada ->
                    motor.equiparRopa(item.id, item.ranura.clave)
                else -> motor.registrarCompraRopa(item.id, item.ranura.clave)
            }
            GlobalAudioManager.playPop()
        }
    }

    fun accionFondo(item: FondoTienda) {
        val actual = motor.estado.value
        val desbloqueado = item.id in actual.fondosComprados ||
            item.id in economia.estado.value.desbloqueados
        if (!desbloqueado) {
            GlobalAudioManager.playPop()
            compraPendiente = CompraPendiente.Fondo(item)
            return
        }
        val puesto = actual.fondoEquipado == item.id
        alcance.launch {
            when {
                puesto -> motor.quitarFondo()
                item.id in actual.fondosComprados -> motor.equiparFondo(item.id)
                else -> motor.registrarCompraFondo(item.id)
            }
            GlobalAudioManager.playPop()
        }
    }

    val ropaPuesta = cuidado.ropaEquipada.values
        .mapNotNull { CatalogoTienditas3B.ropaPorId(it) }
    val fondoPuesto = CatalogoTienditas3B.fondoPorId(cuidado.fondoEquipado)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 14.dp)
        ) {
            EncabezadoTienda(
                saldo = bolsa.zazoles,
                onSalir = {
                    GlobalAudioManager.playPop()
                    onSalir()
                }
            )

            ZaziCompanionView(
                vitals = vitales,
                fondo = fondoPuesto,
                ropa = ropaPuesta,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(252.dp),
                mensajeFlotante = mensajeFlotante,
                tokenFeedback = tokenFeedback,
                confetiTrigger = confeti,
                resaltado = arrastre != null,
                celebrando = celebrando,
                onTocarZazi = {
                    alcance.launch { motor.acariciar() }
                    GlobalAudioManager.playPop()
                },
                onAlternarSueno = {
                    val dormida = motor.estado.value.vitals.dormida
                    alcance.launch {
                        if (dormida) motor.despertar() else motor.dormir()
                    }
                    GlobalAudioManager.playPop()
                },
                onZonaCambiada = { zonaZazi = it }
            )

            Spacer(modifier = Modifier.height(8.dp))
            FilaVitales(vitals = vitales, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
            FilaPestanas(
                seleccionada = pestana,
                onSeleccionar = { nueva ->
                    pestana = nueva
                    GlobalAudioManager.playPop()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (pestana) {
                PestanaTienda.COMIDA, PestanaTienda.CUIDADO -> ListaConsumibles(
                    grupos = CatalogoTienditas3B.consumiblesDe(pestana),
                    inventario = cuidado.consumibles,
                    saldo = bolsa.zazoles,
                    onComprar = { item ->
                        GlobalAudioManager.playPop()
                        compraPendiente = CompraPendiente.Consumible(item)
                    },
                    onArrastreInicio = { item, centro ->
                        arrastre = ArrastreActivo(item, centro)
                    },
                    onArrastreMover = { item, centro ->
                        arrastre = ArrastreActivo(item, centro)
                    },
                    onArrastreCancelado = { arrastre = null },
                    onSoltar = { item, centro ->
                        arrastre = null
                        soltarSobreZazi(item, centro)
                    },
                    onDar = { item ->
                        soltarSobreZazi(
                            item,
                            Offset(zonaZazi.center.x, zonaZazi.center.y)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                PestanaTienda.ROPA -> ListaRopa(
                    compradas = cuidado.ropaComprada,
                    desbloqueadas = bolsa.desbloqueados,
                    equipadas = cuidado.ropaEquipada,
                    saldo = bolsa.zazoles,
                    onAccion = { item -> accionPrenda(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                PestanaTienda.CASITA -> ListaFondos(
                    comprados = cuidado.fondosComprados,
                    desbloqueados = bolsa.desbloqueados,
                    equipado = cuidado.fondoEquipado,
                    saldo = bolsa.zazoles,
                    onAccion = { item -> accionFondo(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        arrastre?.let { activo ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (activo.centro.x - ladoFantasmaPx / 2f).roundToInt(),
                            (activo.centro.y - ladoFantasmaPx / 2f).roundToInt()
                        )
                    }
                    .size(76.dp)
                    .zIndex(20f)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(ZaziliWhite)
                    .border(3.dp, AmarilloVibrante, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = activo.consumible.emoji, fontSize = 30.sp)
                    Text(
                        text = "¡A Zazi!",
                        fontSize = 9.sp,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = aviso != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(top = 64.dp, start = 18.dp, end = 18.dp)
                .zIndex(30f),
            enter = fadeIn(tween(140)) + slideInVertically(tween(200)) { alto -> -alto / 2 },
            exit = fadeOut(tween(240))
        ) {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(UniversoProfundo.copy(alpha = 0.96f))
                    .border(
                        1.5.dp,
                        AmarilloVibrante.copy(alpha = 0.65f),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = aviso.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ZaziliWhite,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        compraPendiente?.let { pendiente ->
            PurchaseConfirmDialog(
                nombre = pendiente.nombre,
                descripcion = pendiente.descripcion,
                emoji = pendiente.emoji,
                precio = pendiente.precio,
                saldo = bolsa.zazoles,
                onDescartar = {
                    compraPendiente = null
                    GlobalAudioManager.playPop()
                },
                onConfirmar = { comprar(pendiente) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  Cabecera
// ---------------------------------------------------------------------------

@Composable
private fun EncabezadoTienda(
    saldo: Int,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(ZaziliWhite)
                .border(2.dp, LavandaMedia, CircleShape)
                .clickable(onClickLabel = "Volver al menú", role = Role.Button, onClick = onSalir)
                .semantics { contentDescription = "Volver al menú" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "‹",
                modifier = Modifier.padding(bottom = 2.dp),
                fontSize = 28.sp,
                color = UniversoProfundo,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tienditas 3B",
                style = MaterialTheme.typography.headlineSmall,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Todo para consentir a Zazi",
                style = MaterialTheme.typography.labelSmall,
                color = UniversoMedio
            )
        }

        Row(
            modifier = Modifier
                .shadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(listOf(AmarilloSuave, DoradoSuave))
                )
                .border(2.dp, AmarilloVibrante, CircleShape)
                .padding(horizontal = 11.dp, vertical = 6.dp)
                .semantics {
                    contentDescription = "Tienes $saldo Zazoles"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🪙", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$saldo",
                style = MaterialTheme.typography.titleSmall,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  Vitales
// ---------------------------------------------------------------------------

@Composable
private fun FilaVitales(vitals: ZaziVitals, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BarraVital(
            emoji = "🍓",
            nombre = "Pancita",
            valor = vitals.pancita / 100f,
            porcentaje = vitals.pancitaPct,
            color = CoralAviso,
            modifier = Modifier.weight(1f)
        )
        BarraVital(
            emoji = "😊",
            nombre = "Felicidad",
            valor = vitals.felicidad / 100f,
            porcentaje = vitals.felicidadPct,
            color = AmarilloVibrante,
            modifier = Modifier.weight(1f)
        )
        BarraVital(
            emoji = "⚡",
            nombre = "Energía",
            valor = vitals.energia / 100f,
            porcentaje = vitals.energiaPct,
            color = AzulCielo,
            modifier = Modifier.weight(1f)
        )
        BarraVital(
            emoji = "🫧",
            nombre = "Higiene",
            valor = vitals.higiene / 100f,
            porcentaje = vitals.higienePct,
            color = MintGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BarraVital(
    emoji: String,
    nombre: String,
    valor: Float,
    porcentaje: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progreso by animateFloatAsState(
        targetValue = valor.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 460),
        label = "vital$nombre"
    )
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$nombre $porcentaje por ciento"
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = nombre,
                fontSize = 9.sp,
                color = UniversoProfundo,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(CircleShape)
                .background(ZaziliWhite.copy(alpha = 0.85f))
                .border(1.dp, UniversoProfundo.copy(alpha = 0.12f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progreso)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = "$porcentaje%",
            fontSize = 8.sp,
            color = UniversoMedio,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------------------------------------------------------------------
//  Pestañas
// ---------------------------------------------------------------------------

@Composable
private fun FilaPestanas(
    seleccionada: PestanaTienda,
    onSeleccionar: (PestanaTienda) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PestanaTienda.entries.forEach { pestana ->
            PestanaChip(
                pestana = pestana,
                activa = pestana == seleccionada,
                onSeleccionar = { onSeleccionar(pestana) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PestanaChip(
    pestana: PestanaTienda,
    activa: Boolean,
    onSeleccionar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = when {
            presionada -> 0.93f
            activa -> 1.04f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escalaPestana"
    )
    val forma = RoundedCornerShape(15.dp)
    val fondo = if (activa) {
        Brush.verticalGradient(listOf(MagicalPurple, UniversoProfundo))
    } else {
        Brush.verticalGradient(listOf(ZaziliWhite, LilaMedio.copy(alpha = 0.7f)))
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .shadow(if (activa) 8.dp else 2.dp, forma)
            .clip(forma)
            .background(fondo)
            .border(
                width = if (activa) 2.dp else 1.5.dp,
                color = if (activa) AmarilloVibrante else LavandaMedia.copy(alpha = 0.55f),
                shape = forma
            )
            .clickable(
                interactionSource = interaccion,
                indication = null,
                onClickLabel = pestana.etiqueta,
                role = Role.Button,
                onClick = onSeleccionar
            )
            .padding(vertical = 8.dp)
            .semantics {
                selected = activa
                contentDescription = pestana.etiqueta
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = pestana.emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = pestana.etiqueta,
            fontSize = 10.sp,
            color = if (activa) ZaziliWhite else UniversoProfundo,
            fontWeight = if (activa) FontWeight.Black else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
//  Marco compartido: dorado = de la tienda, lila = ya es tuyo
// ---------------------------------------------------------------------------

@Composable
private fun MarcoTarjeta(
    propia: Boolean,
    modifier: Modifier = Modifier,
    destacada: Boolean = false,
    bloqueada: Boolean = false,
    presionadaExterna: Boolean = false,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
    contenido: @Composable BoxScope.() -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionadaInterna by interaccion.collectIsPressedAsState()
    val presionada = presionadaInterna || presionadaExterna
    val escala by animateFloatAsState(
        targetValue = if (presionada) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escalaTarjeta"
    )
    val forma = RoundedCornerShape(16.dp)
    val fondo = if (propia) {
        Brush.linearGradient(listOf(LilaBrillo, LavandaClara, LilaMedio))
    } else {
        Brush.linearGradient(listOf(DoradoBrillo, DoradoSuave, AmarilloSuave))
    }
    val borde = when {
        destacada -> AmarilloVibrante
        propia -> MagicalPurple.copy(alpha = 0.55f)
        else -> AmarilloVibrante.copy(alpha = 0.85f)
    }
    val elevacion = when {
        destacada -> 10.dp
        propia -> 4.dp
        else -> 5.dp
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .shadow(elevacion, forma)
            .clip(forma)
            .background(fondo)
            .border(if (destacada) 3.dp else 1.5.dp, borde, forma)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaccion,
                        indication = null,
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = { onClick() }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        contenido()

        if (bloqueada) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(forma)
                    .background(UniversoProfundo.copy(alpha = 0.34f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(ZaziliWhite.copy(alpha = 0.94f))
                        .border(2.dp, AmarilloVibrante, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(text = "🔒", fontSize = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun EtiquetaPrecio(precio: Int) {
    Box(
        modifier = Modifier
            .shadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(AmarilloVibrante, Color(0xFFFFE066), AmarilloVibrante)
                )
            )
            .border(1.dp, AmarilloOscuro.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .semantics { contentDescription = "Cuesta $precio Zazoles" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🪙", fontSize = 11.sp)
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$precio",
                fontSize = 13.sp,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun EtiquetaEquipada() {
    Box(
        modifier = Modifier
            .shadow(9.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(listOf(AmarilloVibrante, Color(0xFFFFE066)))
            )
            .border(1.5.dp, ZaziliWhite.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .semantics { contentDescription = "Equipado por Zazi" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "✨", fontSize = 10.sp)
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "Equipado",
                fontSize = 10.sp,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun EtiquetaAccion(texto: String, color: Color) {
    Box(
        modifier = Modifier
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, ZaziliWhite.copy(alpha = 0.7f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(
            text = texto,
            fontSize = 10.sp,
            color = ZaziliWhite,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun NotaLeyenda(texto: String) {
    Text(
        text = texto,
        fontSize = 9.sp,
        color = UniversoMedio,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    )
}

// ---------------------------------------------------------------------------
//  Lista de consumibles (arrastrables)
// ---------------------------------------------------------------------------

@Composable
private fun ListaConsumibles(
    grupos: List<GrupoConsumibles>,
    inventario: Map<String, Int>,
    saldo: Int,
    onComprar: (ConsumibleTienda) -> Unit,
    onArrastreInicio: (ConsumibleTienda, Offset) -> Unit,
    onArrastreMover: (ConsumibleTienda, Offset) -> Unit,
    onArrastreCancelado: () -> Unit,
    onSoltar: (ConsumibleTienda, Offset) -> Unit,
    onDar: (ConsumibleTienda) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 26.dp)
    ) {
        item(key = "leyenda_consumibles") {
            NotaLeyenda("✨ Dorado: por comprar · 💜 Lila: ya es tuyo. Arrastra lo tuyo hasta Zazi.")
        }
        grupos.forEach { grupo ->
            item(key = "titulo_${grupo.titulo}") {
                Text(
                    text = "${grupo.emoji} ${grupo.titulo}",
                    style = MaterialTheme.typography.titleSmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(items = grupo.items, key = { it.id }) { item ->
                TarjetaConsumible(
                    item = item,
                    cantidad = inventario[item.id] ?: 0,
                    saldo = saldo,
                    onComprar = { onComprar(item) },
                    onArrastreInicio = onArrastreInicio,
                    onArrastreMover = onArrastreMover,
                    onArrastreCancelado = onArrastreCancelado,
                    onSoltar = onSoltar,
                    onDar = { onDar(item) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaConsumible(
    item: ConsumibleTienda,
    cantidad: Int,
    saldo: Int,
    onComprar: () -> Unit,
    onArrastreInicio: (ConsumibleTienda, Offset) -> Unit,
    onArrastreMover: (ConsumibleTienda, Offset) -> Unit,
    onArrastreCancelado: () -> Unit,
    onSoltar: (ConsumibleTienda, Offset) -> Unit,
    onDar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val desplazamiento = remember(item.id) {
        Animatable(Offset.Zero, Offset.VectorConverter)
    }
    val alcance = rememberCoroutineScope()
    var centroBase by remember(item.id) { mutableStateOf<Offset?>(null) }
    var arrastrando by remember(item.id) { mutableStateOf(false) }

    val baseActual by rememberUpdatedState(centroBase)
    val inicioActual by rememberUpdatedState(onArrastreInicio)
    val moverActual by rememberUpdatedState(onArrastreMover)
    val soltarActual by rememberUpdatedState(onSoltar)
    val cancelarActual by rememberUpdatedState(onArrastreCancelado)
    val comprarActual by rememberUpdatedState(onComprar)
    val darActual by rememberUpdatedState(onDar)

    val tiene = cantidad > 0
    val bloqueada = !tiene && saldo < item.precio
    val faltan = (item.precio - saldo).coerceAtLeast(0)

    MarcoTarjeta(
        propia = tiene,
        bloqueada = bloqueada,
        presionadaExterna = arrastrando,
        onClickLabel = if (tiene) null else "Comprar ${item.nombre}",
        onClick = if (tiene) null else ({ comprarActual() }),
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordenadas ->
                centroBase = coordenadas.boundsInRoot().center
            }
            .graphicsLayer {
                translationX = desplazamiento.value.x
                translationY = desplazamiento.value.y
                alpha = if (arrastrando) 0.26f else 1f
            }
            .pointerInput(item.id, cantidad) {
                if (cantidad <= 0) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        val base = baseActual
                        if (base != null) {
                            arrastrando = true
                            inicioActual(item, base)
                        }
                    },
                    onDragCancel = {
                        arrastrando = false
                        cancelarActual()
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
                        val base = baseActual
                        if (base != null) {
                            soltarActual(item, base + desplazamiento.value)
                        } else {
                            cancelarActual()
                        }
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
                    onDrag = { cambio, avance ->
                        cambio.consume()
                        val base = baseActual
                        if (base != null) {
                            moverActual(item, base + desplazamiento.value + avance)
                        }
                        alcance.launch {
                            desplazamiento.snapTo(desplazamiento.value + avance)
                        }
                    }
                )
            }
            .semantics(mergeDescendants = true) {
                if (tiene) {
                    contentDescription = "${item.nombre}. Tienes $cantidad. " +
                        "Arrástralo hasta Zazi para dárselo."
                    customActions = listOf(
                        CustomAccessibilityAction(label = "Dar a Zazi") {
                            darActual()
                            true
                        }
                    )
                } else {
                    role = Role.Button
                    contentDescription = buildString {
                        append("${item.nombre}. Cuesta ${item.precio} Zazoles. ")
                        if (bloqueada) {
                            append("Te faltan $faltan Zazoles. ")
                        }
                        append("Toca para comprar.")
                    }
                }
            }
    ) {
        ContenidoConsumible(item = item, cantidad = cantidad)
    }
}

@Composable
private fun ContenidoConsumible(item: ConsumibleTienda, cantidad: Int) {
    val tiene = cantidad > 0
    Row(
        modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    if (tiene) {
                        Brush.verticalGradient(listOf(LavandaClara, LilaMedio))
                    } else {
                        Brush.verticalGradient(listOf(DoradoBrillo, DoradoSuave))
                    }
                )
                .border(
                    1.5.dp,
                    if (tiene) MagicalPurple.copy(alpha = 0.45f) else AmarilloVibrante,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.emoji, fontSize = 25.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = UniversoProfundo,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.descripcion,
                style = MaterialTheme.typography.labelSmall,
                color = UniversoMedio,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (tiene) {
                    "Tienes $cantidad · Arrástralo hasta Zazi"
                } else {
                    "Toca para comprarlo"
                },
                fontSize = 9.sp,
                color = if (tiene) MagicalPurple else AmarilloOscuro,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        if (tiene) {
            Box(
                modifier = Modifier
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(MagicalPurple, UniversoMedio)))
                    .border(1.5.dp, ZaziliWhite.copy(alpha = 0.8f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Tienes $cantidad" }
            ) {
                Text(
                    text = "x$cantidad",
                    fontSize = 14.sp,
                    color = ZaziliWhite,
                    fontWeight = FontWeight.Black
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                EtiquetaPrecio(precio = item.precio)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Zazoles",
                    fontSize = 8.sp,
                    color = AmarilloOscuro,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Armario
// ---------------------------------------------------------------------------

@Composable
private fun ListaRopa(
    compradas: Set<String>,
    desbloqueadas: Set<String>,
    equipadas: Map<String, String>,
    saldo: Int,
    onAccion: (RopaTienda) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 26.dp)
    ) {
        item(key = "leyenda_ropa") {
            NotaLeyenda("✨ Dorado: de la tienda · 💜 Lila: ya está en tu armario.")
        }
        RanuraRopa.entries.forEach { ranura ->
            item(key = "rana_${ranura.clave}") {
                Text(
                    text = "${ranura.emoji} ${ranura.etiqueta}",
                    style = MaterialTheme.typography.titleSmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
            items(
                items = CatalogoTienditas3B.ropaDe(ranura),
                key = { it.id }
            ) { prenda ->
                val comprada = prenda.id in compradas || prenda.id in desbloqueadas
                val puesta = equipadas[prenda.ranura.clave] == prenda.id
                TarjetaRopa(
                    prenda = prenda,
                    comprada = comprada,
                    puesta = puesta,
                    saldo = saldo,
                    onAccion = { onAccion(prenda) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaRopa(
    prenda: RopaTienda,
    comprada: Boolean,
    puesta: Boolean,
    saldo: Int,
    onAccion: () -> Unit
) {
    val bloqueada = !comprada && saldo < prenda.precio
    val faltan = (prenda.precio - saldo).coerceAtLeast(0)

    MarcoTarjeta(
        propia = comprada,
        destacada = puesta,
        bloqueada = bloqueada,
        onClickLabel = when {
            puesta -> "Quitar ${prenda.nombre}"
            comprada -> "Poner ${prenda.nombre}"
            else -> "Comprar ${prenda.nombre}"
        },
        onClick = onAccion,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = when {
                    puesta -> "${prenda.nombre}. Equipado. Toca para quitarlo."
                    comprada -> "${prenda.nombre}. En tu armario. Toca para ponerlo."
                    else -> buildString {
                        append("${prenda.nombre}. Cuesta ${prenda.precio} Zazoles. ")
                        if (bloqueada) append("Te faltan $faltan Zazoles. ")
                        append("Toca para comprar.")
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (comprada) {
                            Brush.verticalGradient(listOf(LavandaClara, LilaMedio))
                        } else {
                            Brush.verticalGradient(listOf(DoradoBrillo, DoradoSuave))
                        }
                    )
                    .border(
                        1.5.dp,
                        if (puesta) AmarilloVibrante else {
                            if (comprada) MagicalPurple.copy(alpha = 0.45f)
                            else AmarilloVibrante
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = prenda.emoji, fontSize = 25.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prenda.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = prenda.descripcion,
                    style = MaterialTheme.typography.labelSmall,
                    color = UniversoMedio,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        puesta -> "Zazi lo trae puesto"
                        comprada -> "Listo para usar"
                        else -> "Toca para comprarlo"
                    },
                    fontSize = 9.sp,
                    color = when {
                        puesta -> AmarilloOscuro
                        comprada -> VerdeDuoOscuro
                        else -> AmarilloOscuro
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                when {
                    puesta -> EtiquetaEquipada()
                    comprada -> EtiquetaAccion(texto = "Poner", color = VerdeDuo)
                    else -> {
                        EtiquetaPrecio(precio = prenda.precio)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zazoles",
                            fontSize = 8.sp,
                            color = AmarilloOscuro,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Mi Casita
// ---------------------------------------------------------------------------

@Composable
private fun ListaFondos(
    comprados: Set<String>,
    desbloqueados: Set<String>,
    equipado: String?,
    saldo: Int,
    onAccion: (FondoTienda) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 26.dp)
    ) {
        item(key = "leyenda_fondos") {
            NotaLeyenda("✨ Dorado: de la tienda · 💜 Lila: ya decora la casita.")
        }
        item(key = "titulo_fondos") {
            Text(
                text = "🏡 Fondos de la Casita",
                style = MaterialTheme.typography.titleSmall,
                color = UniversoProfundo,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        }
        items(items = CatalogoTienditas3B.fondos, key = { it.id }) { fondo ->
            val comprado = fondo.id in comprados || fondo.id in desbloqueados
            val puesto = equipado == fondo.id
            TarjetaFondo(
                fondo = fondo,
                comprado = comprado,
                puesto = puesto,
                saldo = saldo,
                onAccion = { onAccion(fondo) }
            )
        }
    }
}

@Composable
private fun TarjetaFondo(
    fondo: FondoTienda,
    comprado: Boolean,
    puesto: Boolean,
    saldo: Int,
    onAccion: () -> Unit
) {
    val bloqueada = !comprado && saldo < fondo.precio
    val faltan = (fondo.precio - saldo).coerceAtLeast(0)

    MarcoTarjeta(
        propia = comprado,
        destacada = puesto,
        bloqueada = bloqueada,
        onClickLabel = when {
            puesto -> "Quitar ${fondo.nombre}"
            comprado -> "Poner ${fondo.nombre}"
            else -> "Comprar ${fondo.nombre}"
        },
        onClick = onAccion,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = when {
                    puesto -> "${fondo.nombre}. Fondo puesto. Toca para quitarlo."
                    comprado -> "${fondo.nombre}. En tu colección. Toca para ponerlo."
                    else -> buildString {
                        append("${fondo.nombre}. Cuesta ${fondo.precio} Zazoles. ")
                        if (bloqueada) append("Te faltan $faltan Zazoles. ")
                        append("Toca para comprar.")
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 58.dp, height = 46.dp)
                    .shadow(3.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(fondo.escena.colorInicio),
                                Color(fondo.escena.colorFin)
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        if (puesto) AmarilloVibrante else UniversoProfundo.copy(alpha = 0.18f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = fondo.emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fondo.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fondo.descripcion,
                    style = MaterialTheme.typography.labelSmall,
                    color = UniversoMedio,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        puesto -> "Decorando la casita"
                        comprado -> "Listo para poner"
                        else -> "Toca para comprarlo"
                    },
                    fontSize = 9.sp,
                    color = when {
                        puesto -> AmarilloOscuro
                        comprado -> VerdeDuoOscuro
                        else -> AmarilloOscuro
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.End) {
                when {
                    puesto -> EtiquetaEquipada()
                    comprado -> EtiquetaAccion(texto = "Poner", color = VerdeDuo)
                    else -> {
                        EtiquetaPrecio(precio = fondo.precio)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Zazoles",
                            fontSize = 8.sp,
                            color = AmarilloOscuro,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Modelos de UI
// ---------------------------------------------------------------------------

private data class ArrastreActivo(
    val consumible: ConsumibleTienda,
    val centro: Offset
)

private sealed interface CompraPendiente {
    val nombre: String
    val precio: Int
    val emoji: String
    val descripcion: String

    data class Consumible(val item: ConsumibleTienda) : CompraPendiente {
        override val nombre: String get() = item.nombre
        override val precio: Int get() = item.precio
        override val emoji: String get() = item.emoji
        override val descripcion: String get() = item.descripcion
    }

    data class Prenda(val item: RopaTienda) : CompraPendiente {
        override val nombre: String get() = item.nombre
        override val precio: Int get() = item.precio
        override val emoji: String get() = item.emoji
        override val descripcion: String get() = item.descripcion
    }

    data class Fondo(val item: FondoTienda) : CompraPendiente {
        override val nombre: String get() = item.nombre
        override val precio: Int get() = item.precio
        override val emoji: String get() = item.emoji
        override val descripcion: String get() = item.descripcion
    }
}
