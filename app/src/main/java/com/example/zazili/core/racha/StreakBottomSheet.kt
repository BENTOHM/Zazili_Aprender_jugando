package com.example.zazili.core.racha

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.notificaciones.FilaEstadoAvisos
import com.example.zazili.core.notificaciones.NotificacionesZazili
import com.example.zazili.core.notificaciones.WidgetPinHelper
import com.example.zazili.core.notificaciones.solicitarPermisoNotificaciones
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.ZaziliWhite
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hoja inferior estilo Duolingo: calendario de los últimos 7 días, racha
 * actual, récord, tienda de Protectores, cartas de Zazi (permiso Android 13+)
 * y fijado del widget de inicio desde la propia app.
 *
 * Es la misma hoja de siempre, ahora con dos puertas nuevas hacia el sistema:
 * "Probar aviso" valida permiso + canal + WorkManager de un solo toque, y
 * "Añadir Zazi a mi pantalla de inicio" abre el tutorial paso a paso
 * [WidgetTutorialBottomSheet], que sí sabe qué hacer si el lanzador no deja
 * fijar widgets desde la app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakBottomSheet(
    visible: Boolean,
    estado: EstadoRacha,
    saldoZazoles: Int,
    onComprarProtector: suspend () -> ResultadoProtector,
    onCerrar: () -> Unit
) {
    if (!visible) return
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    var comprando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var permisoAvisos by remember {
        mutableStateOf(NotificacionesZazili.permisoConcedido(contexto))
    }
    var widgetInstalado by remember {
        mutableStateOf(WidgetPinHelper.yaEstaEnInicio(contexto))
    }
    var tutorialWidget by rememberSaveable { mutableStateOf(false) }

    val solicitarPermiso = solicitarPermisoNotificaciones { concedido ->
        permisoAvisos = concedido
        aviso = if (concedido) {
            val enviado = NotificacionesZazili.notificarPrueba(contexto)
            if (enviado) {
                "¡Listo! Revisa tu barra de notificaciones."
            } else {
                "Android no dejó mostrar el aviso de prueba."
            }
        } else {
            "Sin permiso no puedo mandarte cartas. Puedes activarlo cuando quieras."
        }
    }

    fun probarAviso() {
        if (!NotificacionesZazili.permisoConcedido(contexto)) {
            aviso = "Primero dame permiso para mandarte cartas."
            solicitarPermiso()
            return
        }
        val enviado = NotificacionesZazili.notificarPrueba(contexto)
        aviso = if (enviado) {
            "¡Aviso enviado! Revisa tu barra de notificaciones."
        } else {
            "No pudimos avisar. Revisa los permisos de Zazili."
        }
    }

    fun fijarWidget() {
        tutorialWidget = true
    }

    LaunchedEffect(aviso) {
        val actual = aviso ?: return@LaunchedEffect
        delay(3200)
        if (aviso == actual) aviso = null
    }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                IconoFlamaPixel(activa = estado.jugoHoy, size = 72.dp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (estado.jugoHoy) {
                    "¡Racha de ${estado.currentStreak} ${if (estado.currentStreak == 1) "día" else "días"}!"
                } else {
                    "Tu flama está descansando"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (estado.jugoHoy) {
                    "Hoy ya jugaste. Zazi está muy orgulloso de ti."
                } else {
                    "Juega cualquier minijuego hoy y la flama vuelve a arder."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            CalendarioSemanalRacha(fechas = estado.fechasRecientes)

            Spacer(Modifier.height(18.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TarjetaDatoRacha(
                    titulo = "Racha actual",
                    valor = "${estado.currentStreak}",
                    unidad = if (estado.currentStreak == 1) "día" else "días",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                TarjetaDatoRacha(
                    titulo = "Récord",
                    valor = "${estado.longestStreak}",
                    unidad = if (estado.longestStreak == 1) "día" else "días",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                TarjetaDatoRacha(
                    titulo = "Protectores",
                    valor = "${estado.freezes}",
                    unidad = if (estado.freezes == 1) "disponible" else "disponibles",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            TarjetaWidgetInicio(
                instalado = widgetInstalado,
                onFijar = { fijarWidget() }
            )

            Spacer(Modifier.height(12.dp))

            TarjetaCartasDeZazi(
                permisoConcedido = permisoAvisos,
                onSolicitar = {
                    aviso = "Pidiendo permiso a Android…"
                    solicitarPermiso()
                },
                onProbar = { probarAviso() }
            )

            Spacer(Modifier.height(12.dp))

            TarjetaProtector(
                comprando = comprando,
                onComprar = {
                    if (!comprando) {
                        comprando = true
                        alcance.launch {
                            when (onComprarProtector()) {
                                ResultadoProtector.Exito ->
                                    aviso = "¡Protector guardado! Tu racha duerme tranquila."

                                is ResultadoProtector.SinFondos ->
                                    aviso = "Te faltan ${PRECIO_PROTECTOR_RACHA - saldoZazoles} Zazoles. ¡A jugar se ha dicho!"

                                ResultadoProtector.Error ->
                                    aviso = "No pudimos guardarlo. Intenta otra vez."
                            }
                            comprando = false
                        }
                    }
                }
            )

            AnimatedVisibility(
                visible = aviso != null,
                enter = slideInVertically { alto -> alto } + fadeIn(),
                exit = slideOutVertically { alto -> alto } + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = UniversoProfundo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = aviso.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ZaziliWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    WidgetTutorialBottomSheet(
        visible = tutorialWidget,
        onCerrar = { tutorialWidget = false },
        onInstalado = {
            widgetInstalado = true
            aviso = "¡Zazi ya vive en tu pantalla de inicio!"
        }
    )
}

// ---------------------------------------------------------------------------
//  TARJETA: WIDGET EN LA PANTALLA DE INICIO
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaWidgetInicio(
    instalado: Boolean,
    onFijar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = UniversoProfundo
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (instalado) "🏠" else "📲", fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zazi en tu inicio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = ZaziliWhite
                    )
                    Text(
                        text = "Tu flama y tu racha, siempre a la vista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LavandaClara
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ZaziButton(
                text = if (instalado) {
                    "✓ Ya está en tu pantalla de inicio"
                } else {
                    "Añadir Zazi a mi pantalla de inicio"
                },
                onClick = onFijar,
                enabled = !instalado,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                containerColor = AmarilloVibrante,
                contentColor = UniversoProfundo,
                cornerRadius = 14.dp,
                baseHeight = 4.dp,
                fontSize = 13.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  TARJETA: CARTAS DE ZAZI (PERMISO + AVISO DE PRUEBA)
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaCartasDeZazi(
    permisoConcedido: Boolean,
    onSolicitar: () -> Unit,
    onProbar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AmarilloSuave
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            FilaEstadoAvisos(
                permisoConcedido = permisoConcedido,
                onSolicitar = onSolicitar,
                onProbar = onProbar
            )
            Spacer(Modifier.height(10.dp))
            ZaziButton(
                text = "Enviar aviso de prueba",
                onClick = onProbar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                containerColor = MagicalPurple,
                contentColor = ZaziliWhite,
                cornerRadius = 13.dp,
                baseHeight = 4.dp,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (permisoConcedido) {
                    "👀 Zazi te está juzgando… ¡Ven a jugar!"
                } else {
                    "Activa las cartas para que Zazi pueda recordarte tu racha a las 5 PM."
                },
                style = MaterialTheme.typography.bodySmall,
                color = UniversoProfundo.copy(alpha = 0.66f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  TARJETA: PROTECTOR DE RACHA
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaProtector(
    comprando: Boolean,
    onComprar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = LavandaClara
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Protector de Racha",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = UniversoProfundo
                )
                Text(
                    text = "Si un día no puedes jugar, él cuida tu racha por ti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UniversoProfundo.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KawaiiCoinIcon(size = 16.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$PRECIO_PROTECTOR_RACHA Zazoles",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = UniversoProfundo
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            ZaziButton(
                text = "Comprar",
                onClick = onComprar,
                enabled = !comprando,
                modifier = Modifier.height(46.dp),
                containerColor = MagicalPurple,
                contentColor = ZaziliWhite,
                cornerRadius = 14.dp,
                baseHeight = 4.dp,
                fontSize = 13.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  CALENDARIO Y DATOS
// ---------------------------------------------------------------------------

/** Fila de los últimos 7 días: círculo verde con palomita si hubo juego. */
@Composable
private fun CalendarioSemanalRacha(fechas: Set<String>) {
    val hoy = LocalDate.now()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for (retroceso in 6 downTo 0) {
            val fecha = hoy.minusDays(retroceso.toLong())
            val jugado = fecha.toString() in fechas
            val esHoy = retroceso == 0
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (fecha.dayOfWeek.value) {
                        1 -> "L"
                        2 -> "M"
                        3 -> "X"
                        4 -> "J"
                        5 -> "V"
                        6 -> "S"
                        else -> "D"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = when {
                                jugado -> Color(0xFF58CC02)
                                esHoy -> AmarilloSuave
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                            },
                            shape = CircleShape
                        )
                        .then(
                            if (esHoy) {
                                Modifier.border(2.dp, AmarilloVibrante, CircleShape)
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (jugado) "✓" else "${fecha.dayOfMonth}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (jugado) ZaziliWhite else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaDatoRacha(
    titulo: String,
    valor: String,
    unidad: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Text(
                text = titulo.uppercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = UniversoProfundo
            )
            Text(
                text = unidad,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}
