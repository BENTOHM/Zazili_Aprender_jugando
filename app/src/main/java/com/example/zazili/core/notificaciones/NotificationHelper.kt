package com.example.zazili.core.notificaciones

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.zazili.MainActivity
import com.example.zazili.R
import com.example.zazili.core.dialogos.CategoriaDialogo
import com.example.zazili.core.dialogos.DynamicDialogueDictionary
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Constructor de cartas ricas de Zazi.
 *
 * Cada aviso elige una frase nueva del [DynamicDialogueDictionary] y adjunta
 * la lámina correspondiente: alerta cuando la racha corre peligro, escudo
 * cuando un protector la salvó y ánimo de fin de semana con imagen grande.
 *
 * Reutiliza el canal y el permiso de [NotificacionesZazili] para no duplicar
 * canales en el sistema. Si Android niega el permiso, devuelve `false` y la
 * app sigue sin interrumpir al niño.
 */
object NotificationHelper {

    const val ID_AVISO_PELIGRO = 2030
    const val ID_AVISO_CONGELADA = 2031
    const val ID_AVISO_FIN_DE_SEMANA = 2032
    const val ID_AVISO_PRUEBA = 2033

    private const val ETIQUETA = "AvisosZazi"
    private const val LADO_ICONO = 256
    private const val COLOR_ACENTO = 0xFFFFC800.toInt()
    private const val NOMBRE_TRABAJO_FIN_DE_SEMANA = "aviso_fin_de_semana_zazi"
    private const val HORA_FIN_DE_SEMANA = 10

    private enum class EstiloAviso { ICONO_GRANDE, IMAGEN_GRANDE }

    /** Aviso de las 5 PM: la racha se apaga si el niño no entra a jugar. */
    fun notificarRachaEnPeligro(context: Context): Boolean = publicar(
        context = context,
        titulo = "¡Zazi está a punto de llorar!",
        mensaje = DynamicDialogueDictionary.frase(CategoriaDialogo.RACHA_PELIGRO),
        idNotificacion = ID_AVISO_PELIGRO,
        imagenRes = R.drawable.noti_zazi_alerta,
        estilo = EstiloAviso.ICONO_GRANDE
    )

    /** Aviso de medianoche: un Protector de Racha ya salvó el día. */
    fun notificarRachaCongelada(context: Context): Boolean = publicar(
        context = context,
        titulo = "¡Racha protegida!",
        mensaje = DynamicDialogueDictionary.frase(CategoriaDialogo.RACHA_CONGELADA),
        idNotificacion = ID_AVISO_CONGELADA,
        imagenRes = R.drawable.noti_zazi_congelado,
        estilo = EstiloAviso.ICONO_GRANDE
    )

    /**
     * Sábado de aventura: usa imagen grande ([NotificationCompat.BigPictureStyle])
     * porque el mensaje invita a explorar contenido nuevo, no a apagar una
     * emergencia.
     */
    fun notificarFinDeSemana(context: Context): Boolean = publicar(
        context = context,
        titulo = "¡Fin de semana de récords!",
        mensaje = DynamicDialogueDictionary.frase(CategoriaDialogo.FIN_DE_SEMANA),
        idNotificacion = ID_AVISO_FIN_DE_SEMANA,
        imagenRes = R.drawable.noti_zazi_feliz,
        estilo = EstiloAviso.IMAGEN_GRANDE
    )

    /** Botón "Probar aviso": verifica permiso, canal y frase al azar. */
    fun notificarPrueba(context: Context): Boolean = publicar(
        context = context,
        titulo = "👀 Zazi te está juzgando…",
        mensaje = DynamicDialogueDictionary.frase(CategoriaDialogo.AVISO_PRUEBA),
        idNotificacion = ID_AVISO_PRUEBA,
        imagenRes = R.drawable.noti_zazi_feliz,
        estilo = EstiloAviso.ICONO_GRANDE
    )

    /**
     * Programa el ánimo de fin de semana cada sábado a las 10:00 locales.
     * Usa trabajo periódico semanal y lo reemplaza si ya existía, para no
     * encolar recordatorios duplicados.
     */
    fun programarAvisoFinDeSemana(context: Context) {
        runCatching {
            val ahora = LocalDateTime.now()
            var objetivo = ahora
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                .withHour(HORA_FIN_DE_SEMANA)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            if (!objetivo.isAfter(ahora)) objetivo = objetivo.plusWeeks(1)
            val demoraMs = Duration.between(ahora, objetivo).toMillis()

            val solicitud = PeriodicWorkRequestBuilder<AvisoFinDeSemanaWorker>(
                7, TimeUnit.DAYS
            )
                .setInitialDelay(demoraMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    NOMBRE_TRABAJO_FIN_DE_SEMANA,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    solicitud
                )
        }.onFailure {
            Log.w(ETIQUETA, "No se pudo programar el aviso de fin de semana", it)
        }
    }

    private fun publicar(
        context: Context,
        titulo: String,
        mensaje: String,
        idNotificacion: Int,
        @DrawableRes imagenRes: Int,
        estilo: EstiloAviso
    ): Boolean {
        if (!NotificacionesZazili.permisoConcedido(context)) return false
        return runCatching {
            NotificacionesZazili.asegurarCanal(context)
            val icono = bitmapDeIcono(context, imagenRes)

            val constructor = NotificationCompat.Builder(context, CANAL_CARTAS_ZAZI)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setContentIntent(pendiente(context, idNotificacion))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setColor(COLOR_ACENTO)

            if (icono != null) {
                constructor.setLargeIcon(icono)
                constructor.setStyle(
                    if (estilo == EstiloAviso.IMAGEN_GRANDE) {
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(icono)
                            .setSummaryText(mensaje)
                    } else {
                        NotificationCompat.BigTextStyle().bigText(mensaje)
                    }
                )
            } else {
                constructor.setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            }

            NotificationManagerCompat.from(context)
                .notify(idNotificacion, constructor.build())
            true
        }.onFailure {
            Log.w(ETIQUETA, "No se pudo publicar el aviso $idNotificacion", it)
        }.getOrDefault(false)
    }

    private fun pendiente(context: Context, idNotificacion: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            idNotificacion,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Decodifica la lámina con muestreo y la recorta a cuadrado de 256 px.
     * El límite evita el `TransactionTooLargeException` del Binder cuando el
     * PNG original es grande; las láminas viven en `drawable-nodpi`.
     */
    private fun bitmapDeIcono(context: Context, @DrawableRes resId: Int): Bitmap? = try {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, medidas)

        var muestra = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestra * 2) >= LADO_ICONO) {
            muestra *= 2
        }

        val original = BitmapFactory.decodeResource(
            context.resources,
            resId,
            BitmapFactory.Options().apply { inSampleSize = muestra }
        )

        if (original == null) {
            null
        } else {
            val lado = minOf(original.width, original.height)
            val cuadrado = if (original.width == lado && original.height == lado) {
                original
            } else {
                Bitmap.createBitmap(
                    original,
                    (original.width - lado) / 2,
                    (original.height - lado) / 2,
                    lado,
                    lado
                )
            }
            val resultado = if (cuadrado.width == LADO_ICONO) {
                cuadrado
            } else {
                Bitmap.createScaledBitmap(cuadrado, LADO_ICONO, LADO_ICONO, true)
            }
            if (resultado !== original) original.recycle()
            if (cuadrado !== original && cuadrado !== resultado) cuadrado.recycle()
            resultado
        }
    } catch (t: Throwable) {
        Log.w(ETIQUETA, "No se pudo preparar la imagen del aviso", t)
        null
    }
}

/**
 * Trabajo semanal que manda el ánimo de fin de semana. Reintenta si WorkManager
 * lo pide; nunca relanza excepciones al sistema.
 */
class AvisoFinDeSemanaWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        NotificationHelper.notificarFinDeSemana(applicationContext)
        Result.success()
    }.getOrElse {
        Log.w("AvisoFinDeSemana", "Reintento del aviso de Zazi", it)
        Result.retry()
    }
}
