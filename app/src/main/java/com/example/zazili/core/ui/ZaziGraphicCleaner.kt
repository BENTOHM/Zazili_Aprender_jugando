package com.example.zazili.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.zazili.R
import java.util.Arrays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Zona de una lámina que sí debe verse, expresada en fracciones 0..1.
 *
 * [zonaBorrada] elimina la etiqueta inferior de la pose DURMIENDO sin cortar
 * la cola: la cola cuelga a la misma altura que la primera línea de texto.
 * [manchaEsquina] borra un residuo rectangular pegado a la esquina inferior
 * derecha (la lámina BASE trae una cabeza duplicada de la mascota).
 */
@Immutable
data class RecorteZazi(
    val arriba: Float,
    val abajo: Float,
    val izquierda: Float = 0f,
    val derecha: Float = 0f,
    val manchaEsquina: EsquinaSucia? = null,
    val zonaBorrada: ZonaBorrada? = null
)

/** Rectángulo que se pinta de transparente, en fracciones 0..1. */
@Immutable
data class ZonaBorrada(
    val desdeX: Float,
    val desdeY: Float,
    val hastaX: Float = 1f,
    val hastaY: Float = 1f
)

/** Esquina que se pinta de transparente, también en fracciones 0..1. */
@Immutable
data class EsquinaSucia(
    val desdeX: Float,
    val desdeY: Float
)

/** Ventana en píxeles que queda después de recortar una lámina. */
@Immutable
data class VentanaZazi(
    val x: Int,
    val y: Int,
    val ancho: Int,
    val alto: Int
) {
    companion object {
        private const val LIMITE = 0.45f

        fun de(recorte: RecorteZazi, anchoTotal: Int, altoTotal: Int): VentanaZazi {
            val x0 = (recorte.izquierda.coerceIn(0f, LIMITE) * anchoTotal).toInt()
            val y0 = (recorte.arriba.coerceIn(0f, LIMITE) * altoTotal).toInt()
            val x1 = ((1f - recorte.derecha.coerceIn(0f, LIMITE)) * anchoTotal)
                .toInt()
                .coerceIn(x0 + 1, anchoTotal)
            val y1 = ((1f - recorte.abajo.coerceIn(0f, LIMITE)) * altoTotal)
                .toInt()
                .coerceIn(y0 + 1, altoTotal)
            return VentanaZazi(x = x0, y = y0, ancho = x1 - x0, alto = y1 - y0)
        }
    }
}

/**
 * Limpieza en tiempo de ejecución de las seis láminas de Zazi.
 *
 * Las láminas originales son capturas con fondo blanco opaco y una etiqueta
 * de texto pegada arriba o abajo ("BASE", "VUELO IDLE (Home)", "PENSANDO",
 * etc.). Este objeto hace tres cosas, una sola vez por pose y en un hilo de
 * fondo:
 *
 *  1. Recorta la franja de la etiqueta con [RecorteZazi], en píxeles enteros.
 *  2. Borra el fondo con un chroma key real: relleno por inundación desde las
 *     cuatro orillas. Solo se vuelve transparente el blanco CONECTADO al
 *     borde, así que los brillos blancos de los ojos o de las estrellas, que
 *     quedan encerrados por el contorno, se conservan.
 *  3. Elimina el residuo de la esquina de la pose BASE.
 *
 * Una [androidx.compose.ui.graphics.ColorFilter] con `ColorMatrix` no sirve
 * aquí: una matriz lineal no puede separar el blanco puro del crema pastel de
 * la pancita ni del rosa claro del pecho, y dejaría agujeros en el cuerpo. El
 * relleno por inundación sí distingue "fondo" de "color claro interior".
 *
 * El resultado se guarda en un [LruCache] para que cambiar de emoción no
 * vuelva a decodificar ni a recorrer un millón de píxeles.
 */
object ZaziGraphicCleaner {

    /**
     * Canal mínimo para considerar un píxel "blanco de fondo". Se bajó a 225
     * tras medir el halo claro que dejaba 236 entre el contorno negro y el
     * morado; los brillos interiores siguen a salvo porque el relleno solo
     * avanza desde las orillas.
     */
    private const val UMBRAL_BLANCO = 225

    /** Diferencia máxima entre canales para seguir considerándolo neutro. */
    private const val TOLERANCIA_NEUTRA = 20

    private val cache = object : LruCache<Int, ImageBitmap>(12 * 1024) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int =
            value.width * value.height * 4 / 1024
    }

    /** Lámina de la pose. */
    @DrawableRes
    fun recursoDe(estado: ZaziState): Int = when (estado) {
        ZaziState.BASE -> R.drawable.zazi_base
        ZaziState.VUELO_IDLE -> R.drawable.zazi_vuelo_idle
        ZaziState.PENSANDO -> R.drawable.zazi_pensando
        ZaziState.DURMIENDO -> R.drawable.zazi_durmiendo
        ZaziState.TRISTE -> R.drawable.zazi_triste
        ZaziState.CELEBRACION -> R.drawable.zazi_celebracion
    }

    /**
     * Franja útil de cada lámina. Las fracciones se midieron sobre las filas
     * con tinta: justo debajo del título (BASE, VUELO IDLE) o justo arriba de
     * la etiqueta inferior (PENSANDO, DURMIENDO, TRISTE, CELEBRACIÓN).
     */
    fun recorteDe(estado: ZaziState): RecorteZazi = when (estado) {
        ZaziState.BASE -> RecorteZazi(
            arriba = 0.085f,
            abajo = 0f,
            manchaEsquina = EsquinaSucia(desdeX = 0.84f, desdeY = 0.80f)
        )

        ZaziState.VUELO_IDLE -> RecorteZazi(arriba = 0.15f, abajo = 0f)
        ZaziState.PENSANDO -> RecorteZazi(arriba = 0f, abajo = 0.17f)

        // La cola baja hasta la altura de "DURMIENDO", así que el recorte
        // rectangular se queda en 0.12 y el texto se borra por zona.
        ZaziState.DURMIENDO -> RecorteZazi(
            arriba = 0f,
            abajo = 0.12f,
            zonaBorrada = ZonaBorrada(
                desdeX = 0.28f,
                desdeY = 0.90f
            )
        )

        ZaziState.TRISTE -> RecorteZazi(arriba = 0f, abajo = 0.16f)
        ZaziState.CELEBRACION -> RecorteZazi(arriba = 0.02f, abajo = 0.18f)
    }

    /**
     * Devuelve la lámina recortada y con fondo transparente, o `null` si el
     * recurso no se pudo decodificar. Seguro para llamarse desde cualquier
     * hilo; la caché está sincronizada.
     */
    fun limpiar(contexto: Context, estado: ZaziState): ImageBitmap? {
        val recurso = recursoDe(estado)
        cache.get(recurso)?.let { return it }

        return runCatching {
            val opciones = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val fuente = BitmapFactory.decodeResource(contexto.resources, recurso, opciones)
                ?: return@runCatching null

            try {
                val recorte = recorteDe(estado)
                val ventana = VentanaZazi.de(recorte, fuente.width, fuente.height)
                val pixeles = IntArray(ventana.ancho * ventana.alto)
                fuente.getPixels(
                    pixeles,
                    0,
                    ventana.ancho,
                    ventana.x,
                    ventana.y,
                    ventana.ancho,
                    ventana.alto
                )

                borrarRectangulo(
                    pixeles = pixeles,
                    ancho = ventana.ancho,
                    alto = ventana.alto,
                    desdeX = recorte.manchaEsquina?.desdeX,
                    desdeY = recorte.manchaEsquina?.desdeY,
                    hastaX = recorte.manchaEsquina?.let { 1f },
                    hastaY = recorte.manchaEsquina?.let { 1f }
                )
                borrarRectangulo(
                    pixeles = pixeles,
                    ancho = ventana.ancho,
                    alto = ventana.alto,
                    desdeX = recorte.zonaBorrada?.desdeX,
                    desdeY = recorte.zonaBorrada?.desdeY,
                    hastaX = recorte.zonaBorrada?.hastaX,
                    hastaY = recorte.zonaBorrada?.hastaY
                )
                borrarFondoDesdeOrillas(pixeles, ventana.ancho, ventana.alto)

                val limpio = Bitmap.createBitmap(
                    pixeles,
                    ventana.ancho,
                    ventana.alto,
                    Bitmap.Config.ARGB_8888
                )
                limpio.asImageBitmap().also { cache.put(recurso, it) }
            } finally {
                fuente.recycle()
            }
        }.getOrNull()
    }

    private fun borrarRectangulo(
        pixeles: IntArray,
        ancho: Int,
        alto: Int,
        desdeX: Float?,
        desdeY: Float?,
        hastaX: Float?,
        hastaY: Float?
    ) {
        if (desdeX == null || desdeY == null || hastaX == null || hastaY == null) return
        val x0 = (desdeX.coerceIn(0f, 1f) * ancho).toInt().coerceIn(0, ancho)
        val y0 = (desdeY.coerceIn(0f, 1f) * alto).toInt().coerceIn(0, alto)
        val x1 = (hastaX.coerceIn(0f, 1f) * ancho).toInt().coerceIn(x0, ancho)
        val y1 = (hastaY.coerceIn(0f, 1f) * alto).toInt().coerceIn(y0, alto)
        for (y in y0 until y1) {
            val inicio = y * ancho
            Arrays.fill(pixeles, inicio + x0, inicio + x1, 0)
        }
    }

    /**
     * Relleno por inundación iterativo (sin recursión, sin riesgo de
     * desbordar la pila) desde las cuatro orillas. La pila reutiliza un
     * [IntArray] del tamaño de la imagen y nunca guarda un píxel dos veces.
     */
    private fun borrarFondoDesdeOrillas(pixeles: IntArray, ancho: Int, alto: Int) {
        val marcado = BooleanArray(pixeles.size)
        val pila = IntArray(pixeles.size)
        var tope = 0

        fun apilar(indice: Int) {
            if (!marcado[indice] && esFondo(pixeles[indice])) {
                marcado[indice] = true
                pila[tope++] = indice
            }
        }

        for (x in 0 until ancho) {
            apilar(x)
            apilar((alto - 1) * ancho + x)
        }
        for (y in 0 until alto) {
            apilar(y * ancho)
            apilar(y * ancho + ancho - 1)
        }

        while (tope > 0) {
            val indice = pila[--tope]
            val x = indice % ancho
            val y = indice / ancho
            if (x > 0) apilar(indice - 1)
            if (x < ancho - 1) apilar(indice + 1)
            if (y > 0) apilar(indice - ancho)
            if (y < alto - 1) apilar(indice + ancho)
        }

        for (indice in pixeles.indices) {
            if (marcado[indice]) pixeles[indice] = 0
        }
    }

    private fun esFondo(color: Int): Boolean {
        val alfa = color ushr 24
        if (alfa < 16) return true
        val rojo = (color shr 16) and 0xFF
        val verde = (color shr 8) and 0xFF
        val azul = color and 0xFF
        val menor = minOf(rojo, verde, azul)
        val mayor = maxOf(rojo, verde, azul)
        return menor >= UMBRAL_BLANCO && (mayor - menor) <= TOLERANCIA_NEUTRA
    }
}

/**
 * Carga la pose limpia sin bloquear el hilo principal. Mientras termina el
 * primer procesamiento devuelve `null`; la pose anterior se conserva si la
 * caché ya la tenía, así que el cambio de emoción no parpadea.
 */
@Composable
fun recordarZaziLimpio(estado: ZaziState): ImageBitmap? {
    val contexto = LocalContext.current.applicationContext
    val imagen by produceState<ImageBitmap?>(initialValue = null, estado) {
        value = withContext(Dispatchers.Default) {
            ZaziGraphicCleaner.limpiar(contexto, estado)
        }
    }
    return imagen
}
