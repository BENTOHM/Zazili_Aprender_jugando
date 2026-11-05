package com.example.zazili.core.notificaciones

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Diálogo de justificación ("rationale") del permiso POST_NOTIFICATIONS.
 *
 * REGLA DEL BUG CORREGIDO: el `confirmButton` es OBLIGATORIO en `AlertDialog`.
 * Antes usábamos un botón a medida (ZaziButton) dentro del slot y el slot se
 * quedaba sin altura útil en algunas versiones de Material3, así que solo se
 * veía "Después". Ahora el botón confirmador es un `Button` de Material3
 * explícito, con relleno y altura fija, y su `onClick` dispara DIRECTO el
 * lanzador del sistema.
 */
@Composable
fun DialogoCartasDeZazi(
    visible: Boolean,
    onCerrar: () -> Unit,
    onResultado: (Boolean) -> Unit = {}
) {
    if (!visible) return

    val contexto = LocalContext.current
    val resultadoActual = rememberUpdatedState(onResultado)

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) NotificacionesZazili.asegurarCanal(contexto)
        resultadoActual.value(concedido)
        onCerrar()
    }

    fun aceptar() {
        val yaConcedido = NotificacionesZazili.permisoConcedido(contexto)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || yaConcedido) {
            resultadoActual.value(true)
            onCerrar()
        } else {
            // El lanzador real del sistema. Sin esto, Android 13 nunca
            // concedería el permiso y el worker quedaría en silencio.
            lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Text(text = "💌", fontSize = 36.sp)
        },
        title = {
            Text(
                text = "¡Zazi necesita enviarte cartas!",
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "Son recordatorios cortitos para que tu racha no se apague y Zazi no se ponga tristón.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Solo se encienden si tú dices que sí. Puedes apagarlas cuando quieras.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { aceptar() },
                modifier = Modifier
                    .height(50.dp)
                    .semantics {
                        contentDescription = "Aceptar y activar las cartas de Zazi"
                    },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VerdeDuo,
                    contentColor = ZaziliWhite
                )
            ) {
                Text(
                    text = "¡Aceptar!",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(
                    text = "Después",
                    fontWeight = FontWeight.SemiBold,
                    color = UniversoProfundo.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    )
}

/**
 * Devuelve una función que pide POST_NOTIFICATIONS con el flujo correcto de
 * Compose. En Android 12 o anterior no se lanza nada: el permiso no existe y
 * se reporta concedido para no bloquear la app.
 */
@Composable
fun solicitarPermisoNotificaciones(
    onResultado: (Boolean) -> Unit = {}
): () -> Unit {
    val contexto = LocalContext.current
    val resultadoActual = rememberUpdatedState(onResultado)

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) NotificacionesZazili.asegurarCanal(contexto)
        resultadoActual.value(concedido)
    }

    return remember(contexto, lanzador) {
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                NotificacionesZazili.permisoConcedido(contexto)
            ) {
                resultadoActual.value(true)
            } else {
                lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * Trigger del Tablero: al abrir la app, si falta el permiso en Android 13+,
 * aparece una sola vez el diálogo. "¡Aceptar!" abre el permiso del sistema;
 * "Después" solo esconde el diálogo durante esta visita.
 */
@Composable
fun PedirPermisoNotificacionesAlEntrar() {
    val contexto = LocalContext.current
    var visible by rememberSaveable {
        mutableStateOf(!NotificacionesZazili.permisoConcedido(contexto))
    }

    DialogoCartasDeZazi(
        visible = visible,
        onCerrar = { visible = false },
        onResultado = { }
    )
}
