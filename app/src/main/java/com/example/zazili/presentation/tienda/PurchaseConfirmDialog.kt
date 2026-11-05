package com.example.zazili.presentation.tienda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

private val GrisSuave = Color(0xFFE9E4EF)
private val DoradoDialogo = Color(0xFFFFE9A8)

/**
 * Confirmación de compra de Tienditas 3B.
 *
 * Se usa un [Dialog] de ancho libre con un [Surface] centrado para que la fila
 * de botones ("Mejor no" / "¡Comprar!") siempre quede dentro de la ventana,
 * incluso con letra grande o barras del sistema. El contenido superior puede
 * desplazarse, pero los botones viven fuera del área desplazable y siempre
 * quedan visibles al fondo. El botón verde se desactiva si falta saldo y el
 * aviso explica cuántos Zazoles faltan.
 */
@Composable
fun PurchaseConfirmDialog(
    nombre: String,
    descripcion: String,
    emoji: String,
    precio: Int,
    saldo: Int,
    onDescartar: () -> Unit,
    onConfirmar: () -> Unit
) {
    val alcanza = saldo >= precio
    val faltan = (precio - saldo).coerceAtLeast(0)

    Dialog(
        onDismissRequest = onDescartar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(26.dp),
                color = SoftCream,
                tonalElevation = 0.dp,
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .shadow(6.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFFFF8DE), DoradoDialogo)
                                        )
                                    )
                                    .border(2.dp, AmarilloVibrante, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 30.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "¿Comprar $nombre por $precio Zazoles?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = UniversoProfundo,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = descripcion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UniversoMedio,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFFF8DE), AmarilloSuave)
                                    )
                                )
                                .border(
                                    1.5.dp,
                                    AmarilloVibrante.copy(alpha = 0.85f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Precio",
                                    fontSize = 10.sp,
                                    color = UniversoMedio,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🪙", fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$precio Zazoles",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AmarilloOscuro,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Tienes",
                                    fontSize = 10.sp,
                                    color = UniversoMedio,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$saldo Zazoles",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (alcanza) UniversoProfundo else CoralAviso,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        if (!alcanza) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CoralAviso.copy(alpha = 0.13f))
                                    .border(
                                        1.dp,
                                        CoralAviso.copy(alpha = 0.45f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Te faltan $faltan Zazoles. " +
                                        "Completa una actividad para ganarlos.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralAviso,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ZaziButton(
                            text = "Mejor no",
                            onClick = onDescartar,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            containerColor = GrisSuave,
                            contentColor = UniversoProfundo,
                            cornerRadius = 16.dp,
                            baseHeight = 5.dp,
                            fontSize = 15.sp
                        )
                        ZaziButton(
                            text = "¡Comprar!",
                            onClick = onConfirmar,
                            enabled = alcanza,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            containerColor = VerdeDuo,
                            contentColor = ZaziliWhite,
                            cornerRadius = 16.dp,
                            baseHeight = 5.dp,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
