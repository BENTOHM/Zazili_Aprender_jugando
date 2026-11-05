package com.example.zazili.core.notificaciones

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.example.zazili.core.racha.StreakWidgetReceiver

/**
 * Fijado del widget de racha desde dentro de la app.
 *
 * REGLA DEL BUG CORREGIDO: `requestPinAppWidget` falla en silencio cuando el
 * `ComponentName` no apunta EXACTAMENTE al receptor declarado en el manifiesto.
 * Aquí el receptor es [StreakWidgetReceiver] (paquete
 * `com.example.zazili.core.racha`), el mismo que aparece en
 * `AndroidManifest_Snippet.xml`. Si el nombre no coincide, Android rechaza la
 * solicitud sin lanzar excepción.
 *
 * El Toast "Abriendo sistema..." se dispara justo antes de la API para
 * confirmar que el clic del botón sí está llegando aquí.
 */
object WidgetPinHelper {

    enum class ResultadoFijado {
        /** Android mostró su ventana nativa de confirmación. */
        MOSTRADO,

        /** El widget ya estaba en la pantalla de inicio. */
        YA_EN_INICIO,

        /** El lanzador o la versión de Android no ofrecen fijado directo. */
        NO_SOPORTADO,

        /** Algo falló al pedir la ventana. */
        ERROR
    }

    private const val ETIQUETA = "WidgetPinHelper"

    /**
     * Componente del proveedor Glance. Debe ser el MISMO nombre que el
     * `<receiver android:name=".core.racha.StreakWidgetReceiver">` del
     * manifiesto (ver AndroidManifest_Snippet.xml).
     */
    private fun proveedor(context: Context): ComponentName =
        ComponentName(context.applicationContext, StreakWidgetReceiver::class.java)

    fun puedeFijar(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            runCatching {
                AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
            }.getOrDefault(false)

    fun yaEstaEnInicio(context: Context): Boolean = runCatching {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(proveedor(context))
            .isNotEmpty()
    }.getOrDefault(false)

    fun solicitarFijado(context: Context): ResultadoFijado {
        if (yaEstaEnInicio(context)) return ResultadoFijado.YA_EN_INICIO

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(
                context.applicationContext,
                "Tu Android es anterior a Android 8. Mantén presionada tu pantalla de inicio y elige \"Widgets\" para añadir a Zazi.",
                Toast.LENGTH_LONG
            ).show()
            return ResultadoFijado.NO_SOPORTADO
        }

        return runCatching {
            if (!puedeFijar(context)) {
                Toast.makeText(
                    context.applicationContext,
                    "Tu lanzador no deja fijar widgets desde aquí. Mantén presionada tu pantalla de inicio y busca \"Zazili\".",
                    Toast.LENGTH_LONG
                ).show()
                ResultadoFijado.NO_SOPORTADO
            } else {
                // Confirmación táctil: si este Toast aparece, el clic llegó.
                Toast.makeText(
                    context.applicationContext,
                    "Abriendo sistema...",
                    Toast.LENGTH_SHORT
                ).show()

                val enviado = AppWidgetManager.getInstance(context.applicationContext)
                    .requestPinAppWidget(proveedor(context), null, null)

                if (enviado) {
                    ResultadoFijado.MOSTRADO
                } else {
                    Toast.makeText(
                        context.applicationContext,
                        "Android rechazó la solicitud. Revisa que el widget esté declarado en el manifiesto.",
                        Toast.LENGTH_LONG
                    ).show()
                    ResultadoFijado.ERROR
                }
            }
        }.getOrElse {
            Log.w(ETIQUETA, "No se pudo pedir el fijado del widget", it)
            Toast.makeText(
                context.applicationContext,
                "No pudimos abrir la ventana del widget. Inténtalo desde tu pantalla de inicio.",
                Toast.LENGTH_LONG
            ).show()
            ResultadoFijado.ERROR
        }
    }
}
