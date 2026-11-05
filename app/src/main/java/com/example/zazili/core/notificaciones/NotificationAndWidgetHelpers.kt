package com.example.zazili.core.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.zazili.MainActivity
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/** Canal único de las cartas de Zazi (racha, recordatorios y aviso de prueba). */
const val CANAL_CARTAS_ZAZI = "canal_racha_zazili"

/**
 * Disparo de avisos locales y creación del canal compartido.
 *
 * El permiso POST_NOTIFICATIONS (Android 13+) se pide en
 * [NotificationRationaleDialog.kt]; aquí solo se verifica antes de publicar y
 * se degrada con calma: nunca lanza excepción y devuelve `false` si Android no
 * deja avisar.
 */
object NotificacionesZazili {

    const val ID_AVISO_PRUEBA = 2027

    /** En Android 12 y anteriores no existe el permiso: siempre está concedido. */
    fun permisoConcedido(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /** Canal de importancia ALTA: el aviso debe salir con burbuja y sonido. */
    fun asegurarCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val canal = NotificationChannel(
                CANAL_CARTAS_ZAZI,
                "Cartas de Zazi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios chistosos para cuidar tu racha de juego."
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(canal)
        }.onFailure { Log.w(ETIQUETA, "No se pudo crear el canal de avisos", it) }
    }

    /**
     * Lanza un aviso local inmediato. Devuelve true solo si Android lo aceptó.
     * Sirve tanto al worker diario como al botón "Probar aviso" de la hoja.
     */
    fun mostrar(
        context: Context,
        titulo: String,
        mensaje: String,
        idNotificacion: Int
    ): Boolean {
        if (!permisoConcedido(context)) return false
        return runCatching {
            asegurarCanal(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendiente = PendingIntent.getActivity(
                context,
                idNotificacion,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notificacion = NotificationCompat.Builder(context, CANAL_CARTAS_ZAZI)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
                .setContentIntent(pendiente)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build()
            NotificationManagerCompat.from(context).notify(idNotificacion, notificacion)
            true
        }.onFailure { Log.w(ETIQUETA, "No se pudo mostrar el aviso", it) }
            .getOrDefault(false)
    }

    /** Aviso pasivo-agresivo de prueba: verifica de golpe permiso + canal. */
    fun notificarPrueba(context: Context): Boolean = mostrar(
        context,
        "👀 Zazi te está juzgando…",
        "¡Ven a jugar! Tu racha no se va a cuidar sola, ¿eh?",
        ID_AVISO_PRUEBA
    )

    private const val ETIQUETA = "NotificacionesZazili"
}

/**
 * Fila reutilizable que muestra el estado del permiso con un botón de acción.
 * La usa la hoja de racha para encender las cartas y para probar el aviso.
 */
@Composable
fun FilaEstadoAvisos(
    permisoConcedido: Boolean,
    onSolicitar: () -> Unit,
    onProbar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = if (permisoConcedido) "💌" else "🔕", fontSize = 22.sp)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (permisoConcedido) "Cartas de Zazi encendidas" else "Cartas de Zazi apagadas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = UniversoProfundo
            )
            Text(
                text = if (permisoConcedido) {
                    "Zazi ya puede recordarte tu racha a las 5 PM."
                } else {
                    "Zazi quiere avisarte cuando tu racha esté en peligro."
                },
                style = MaterialTheme.typography.bodySmall,
                color = UniversoProfundo.copy(alpha = 0.66f)
            )
        }
        Spacer(Modifier.width(8.dp))
        ZaziButton(
            text = if (permisoConcedido) "Probar" else "Activar",
            onClick = if (permisoConcedido) onProbar else onSolicitar,
            modifier = Modifier.size(width = 96.dp, height = 44.dp),
            containerColor = if (permisoConcedido) AmarilloSuave else VerdeDuo,
            contentColor = if (permisoConcedido) UniversoProfundo else ZaziliWhite,
            cornerRadius = 13.dp,
            baseHeight = 4.dp,
            fontSize = 13.sp
        )
    }
}
