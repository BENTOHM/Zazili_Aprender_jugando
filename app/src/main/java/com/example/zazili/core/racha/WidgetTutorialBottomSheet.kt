package com.example.zazili.core.racha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.logros.AccionesLogro
import com.example.zazili.core.logros.AchievementDispatcher
import com.example.zazili.core.notificaciones.WidgetPinHelper
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tutorial visual para colocar el widget de racha en la pantalla de inicio.
 *
 * El fijado nativo (`requestPinAppWidget`) falla en silencio en varias capas
 * de personalización (Xiaomi, Samsung, etc.), así que aquí el niño ve dos
 * pasos grandes, un botón verde enorme y —si el teléfono no coopera— las
 * instrucciones manuales cambian en pantalla sin mostrar un error crudo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetTutorialBottomSheet(
    visible: Boolean,
    onCerrar: () -> Unit,
    onInstalado: () -> Unit = {}
) {
    if (!visible) return
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    // Si el lanzador ya dice que no soporta el fijado, el tutorial arranca en
    // modo manual: el niño nunca ve un botón que sabemos que no funciona.
    val soportaFijado = remember(contexto) { WidgetPinHelper.puedeFijar(contexto) }
    var mostrarManual by rememberSaveable { mutableStateOf(!soportaFijado) }
    var instalado by rememberSaveable { mutableStateOf(false) }
    var esperando by rememberSaveable { mutableStateOf(false) }
    var aviso by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * Pide la ventana nativa. Si el lanzador no la ofrece (o Android rechaza)
     * se cambia al modo manual en lugar de dejar al niño con un botón muerto.
     */
    fun intentarFijado() {
        if (esperando || instalado) return
        aviso = null
        when (WidgetPinHelper.solicitarFijado(contexto)) {
            WidgetPinHelper.ResultadoFijado.MOSTRADO -> {
                esperando = true
                aviso = "Revisa tu teléfono y toca «Añadir»."
                alcance.launch {
                    delay(2200)
                    if (WidgetPinHelper.yaEstaEnInicio(contexto)) {
                        instalado = true
                        esperando = false
                        aviso = null
                        AchievementDispatcher.notifyAction(
                            contexto,
                            AccionesLogro.WIDGET_AGREGADO
                        )
                        onInstalado()
                    } else {
                        esperando = false
                        aviso = "Si la ventana no apareció, puedes hacerlo a mano."
                    }
                }
            }

            WidgetPinHelper.ResultadoFijado.YA_EN_INICIO -> {
                instalado = true
                esperando = false
                aviso = null
                AchievementDispatcher.notifyAction(contexto, AccionesLogro.WIDGET_AGREGADO)
                onInstalado()
            }

            WidgetPinHelper.ResultadoFijado.NO_SOPORTADO,
            WidgetPinHelper.ResultadoFijado.ERROR -> {
                mostrarManual = true
                esperando = false
            }
        }
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
            Text(
                text = "📲",
                fontSize = 30.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (instalado) {
                    "¡Zazi ya vive en tu inicio!"
                } else {
                    "¡Pon a Zazi en tu pantalla de inicio!"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (instalado) {
                    "Tu flama y tu racha estarán siempre a la vista."
                } else {
                    "Son dos pasos sencillos. Yo te acompaño."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            TelefonoConFlama()
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Así se verá en tu pantalla",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(16.dp))

            if (instalado) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MintGreen,
                    border = BorderStroke(2.dp, VerdeDuo),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Text(text = "✅", fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Listo!",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = UniversoProfundo
                            )
                            Text(
                                text = "Busca a Zazi en tu pantalla de inicio y presiona su flama cuando quieras ver tu racha.",
                                style = MaterialTheme.typography.bodySmall,
                                color = UniversoProfundo.copy(alpha = 0.78f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                ZaziButton(
                    text = "Cerrar",
                    onClick = onCerrar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    containerColor = VerdeDuo,
                    contentColor = ZaziliWhite,
                    cornerRadius = 18.dp,
                    baseHeight = 5.dp,
                    fontSize = 16.sp
                )
            } else if (!soportaFijado) {
                InstruccionesManuales()
            } else {
                PasoWidget(
                    numero = 1,
                    texto = "Paso 1: Toca el botón verde de abajo."
                )
                PasoWidget(
                    numero = 2,
                    texto = "Paso 2: Cuando tu teléfono te pregunte, presiona «Añadir»."
                )

                Spacer(Modifier.height(18.dp))

                ZaziButton(
                    text = if (esperando) {
                        "Esperando a tu teléfono…"
                    } else {
                        "¡Intentar poner el Widget!"
                    },
                    onClick = { intentarFijado() },
                    enabled = !esperando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp),
                    containerColor = VerdeDuo,
                    contentColor = ZaziliWhite,
                    cornerRadius = 20.dp,
                    baseHeight = 6.dp,
                    fontSize = 16.sp
                )

                if (!mostrarManual) {
                    TextButton(onClick = { mostrarManual = true }) {
                        Text(
                            text = "¿No apareció la ventana? Muéstrame cómo",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }

                if (mostrarManual) {
                    Spacer(Modifier.height(10.dp))
                    InstruccionesManuales()
                }
            }

            if (aviso != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = UniversoProfundo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aviso.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ZaziliWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            if (!instalado) {
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = onCerrar) {
                    Text(
                        text = "Ahora no",
                        fontWeight = FontWeight.SemiBold,
                        color = UniversoProfundo.copy(alpha = 0.66f)
                    )
                }
            }
        }
    }
}

/** Mini teléfono con la flama: se entiende sin leer una sola palabra. */
@Composable
private fun TelefonoConFlama() {
    Box(
        modifier = Modifier
            .size(width = 104.dp, height = 146.dp)
            .background(LavandaClara, RoundedCornerShape(24.dp))
            .border(4.dp, UniversoProfundo, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconoFlamaPixel(activa = true, size = 48.dp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ZAZI",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                color = UniversoProfundo
            )
        }
        Text(
            text = "✨",
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
        )
        Text(
            text = "✨",
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 10.dp)
        )
    }
}

@Composable
private fun PasoWidget(numero: Int, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(AmarilloVibrante, CircleShape)
                .border(2.dp, UniversoProfundo, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$numero",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = UniversoProfundo
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Fallback neuro-amable: instrucciones claras para cualquier lanzador. */
@Composable
private fun InstruccionesManuales() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AmarilloSuave,
        border = BorderStroke(2.dp, CoralAviso),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                text = "¡Uy! Tu teléfono es especial.",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = UniversoProfundo
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Para poner a Zazi, sal a tu pantalla principal, mantén presionado un espacio vacío, elige «Widgets» y busca a Zazi.",
                style = MaterialTheme.typography.bodyMedium,
                color = UniversoProfundo.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🧩", fontSize = 18.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Si te pierdes, pide ayuda a un adulto. No pasa nada.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = UniversoProfundo.copy(alpha = 0.7f)
                )
            }
        }
    }
}
