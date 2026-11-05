package com.example.zazili.core.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.zazili.core.accessibility.collectZenMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ---------------------------------------------------------------------------
//  PALETA PIXEL (image_32dab9.png)
// ---------------------------------------------------------------------------

private val MentaZazi = Color(0xFF86DCB8)
private val RosaZazi = Color(0xFFFFAFD1)
private val AzulZazi = Color(0xFF91C8FF)
private val MoradoZazi = Color(0xFFC9A7FF)
private val CremaZazi = Color(0xFFFFF6EC)
private val BlancoZazi = Color(0xFFFFFFFF)
private val TintaZazi = Color(0xFF000000)
private val OroZazi = Color(0xFFFFD966)
private val MarronRama = Color(0xFF7A4E2D)
private val MarronSombra = Color(0xFF57381F)

private val ARGB_MENTA = MentaZazi.toArgb()
private val ARGB_ROSA = RosaZazi.toArgb()
private val ARGB_AZUL = AzulZazi.toArgb()
private val ARGB_MORADO = MoradoZazi.toArgb()
private val ARGB_CREMA = CremaZazi.toArgb()
private val ARGB_BLANCO = BlancoZazi.toArgb()
private val ARGB_TINTA = TintaZazi.toArgb()
private val ARGB_ORO = OroZazi.toArgb()
private val ARGB_MARRON = MarronRama.toArgb()
private val ARGB_MARRON_SOMBRA = MarronSombra.toArgb()

private const val ANCHO_SPRITE = 64
private const val ALTO_SPRITE = 56
private const val MARCOS_ALA = 3
private const val SEGMENTOS_ARCO = 8
private const val OPACO = 0xFF000000.toInt()

private enum class VistaSprite { PERFIL, FRONTAL }

private enum class VarianteSprite { BASE, VUELO, FIESTA, PENSANDO, PIJAMA, TRISTE }

/**
 * Zazi en pixel-art, rasterizada con una rejilla explícita de 64 x 56 celdas.
 *
 * Cada pieza (cola, ala, cuerpo, cabeza, gorro) se dibuja en una capa aparte y
 * se estampa con un contorno negro de 1 px: así el borde interior entre el ala
 * y el cuerpo también queda marcado, como en la hoja de diseño. Las alas llevan
 * tres franjas fijas —rosa, azul y morado— y la cola es un abanico con
 * separadores de 1 px.
 *
 * El sprite se construye UNA vez por pose y se dibuja con [FilterQuality.None],
 * así que cada celda cae en píxeles enteros y nunca se suaviza. Toda la
 * animación (aleteo, flotación, partículas) se resuelve en la fase de dibujo:
 * la pantalla NO se recompone por frame.
 */
@Composable
fun PixelArtZazi(
    modifier: Modifier = Modifier,
    estado: ZaziState = ZaziState.VUELO_IDLE,
    animar: Boolean = true,
    size: Dp? = null,
    descripcion: String? = null,
    onTap: (() -> Unit)? = null
) {
    val zen = collectZenMode()
    val texto = descripcion ?: estado.descripcion
    val toqueActual by rememberUpdatedState(onTap)

    val medida = if (size != null) Modifier.size(size) else Modifier
    val interaccion = if (onTap != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures { toqueActual?.invoke() }
        }
    } else {
        Modifier
    }

    AnimatedContent(
        targetState = estado,
        modifier = modifier
            .then(medida)
            .then(interaccion)
            .semantics {
                if (texto.isNotBlank()) contentDescription = texto
                if (onTap != null) {
                    role = Role.Button
                    onClick(label = "Hacerle cariño a Zazi") {
                        toqueActual?.invoke()
                        true
                    }
                }
            },
        transitionSpec = {
            val duracion = if (zen) 0 else 230
            (
                fadeIn(tween(duracion)) +
                    scaleIn(
                        initialScale = if (targetState == ZaziState.CELEBRACION) {
                            0.82f
                        } else {
                            0.94f
                        },
                        animationSpec = tween(duracion)
                    )
                ).togetherWith(fadeOut(tween(if (zen) 0 else 140)))
        },
        label = "estadoPixelZazi"
    ) { actual ->
        LienzoPixelZazi(
            estado = actual,
            activo = animar && !zen,
            zen = zen,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun LienzoPixelZazi(
    estado: ZaziState,
    activo: Boolean,
    zen: Boolean,
    modifier: Modifier
) {
    val reloj = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(activo, estado) {
        if (!activo) {
            reloj.floatValue = 0f
            return@LaunchedEffect
        }
        val inicio = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { ahora ->
                reloj.floatValue = (ahora - inicio) / 1_000_000_000f
            }
        }
    }

    var parpadeo by remember { mutableStateOf(false) }
    LaunchedEffect(activo, estado) {
        parpadeo = false
        val parpadea = estado == ZaziState.VUELO_IDLE || estado == ZaziState.PENSANDO
        if (!activo || !parpadea) return@LaunchedEffect
        while (isActive) {
            delay(Random.nextLong(2_800L, 5_400L))
            parpadeo = true
            delay(120L)
            parpadeo = false
        }
    }

    val sprites = remember(estado) { spritesDe(estado) }

    Canvas(
        modifier = modifier.graphicsLayer {
            val t = reloj.floatValue
            if (zen) {
                translationX = 0f
                translationY = 0f
                rotationZ = 0f
                scaleX = 1f
                scaleY = 1f
            } else {
                when (estado) {
                    ZaziState.VUELO_IDLE -> {
                        translationY = sin(t * 2.8f) * 2.6.dp.toPx()
                        translationX = sin(t * 1.7f) * 1.2.dp.toPx()
                    }
                    ZaziState.CELEBRACION -> {
                        val salto = abs(sin(t * 6.6f))
                        translationY = -salto * 7.dp.toPx()
                        scaleX = 1f - salto * 0.020f
                        scaleY = 1f + salto * 0.035f
                    }
                    ZaziState.PENSANDO -> {
                        rotationZ = sin(t * 2.2f) * 5f
                        translationY = -1.dp.toPx()
                    }
                    ZaziState.DURMIENDO -> {
                        val respira = 1f + sin(t * 1.5f) * 0.012f
                        scaleX = respira
                        scaleY = respira
                        translationY = 1.5.dp.toPx()
                    }
                    ZaziState.TRISTE -> {
                        translationY = 2.dp.toPx()
                        rotationZ = -2f + sin(t * 1.1f)
                    }
                    ZaziState.BASE -> {
                        val respira = 1f + sin(t * 1.8f) * 0.008f
                        scaleX = respira
                        scaleY = respira
                    }
                }
            }
        }
    ) {
        val t = reloj.floatValue
        if (size.minDimension <= 0f) return@Canvas

        val celda = floor(
            min(size.width / ANCHO_SPRITE, size.height / ALTO_SPRITE)
        ).coerceAtLeast(1f)
        val ancho = celda * ANCHO_SPRITE
        val alto = celda * ALTO_SPRITE
        val origen = Offset(
            x = (size.width - ancho) / 2f,
            y = (size.height - alto) / 2f
        )

        drawImage(
            image = sprites[marcoDe(estado, t)],
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(ANCHO_SPRITE, ALTO_SPRITE),
            dstOffset = IntOffset(origen.x.roundToInt(), origen.y.roundToInt()),
            dstSize = IntSize(ancho.roundToInt(), alto.roundToInt()),
            filterQuality = FilterQuality.None
        )

        if (parpadeo && estado.vista() == VistaSprite.PERFIL) {
            dibujarParpadeo(estado, origen, celda)
        }
        if (!zen) {
            dibujarParticulasEstado(
                estado = estado,
                t = t,
                origen = origen,
                celda = celda,
                ancho = ancho,
                alto = alto
            )
        }
    }
}

private fun marcoDe(estado: ZaziState, t: Float): Int = when (estado) {
    ZaziState.VUELO_IDLE -> (t * 10f).toInt().mod(MARCOS_ALA)
    ZaziState.CELEBRACION -> (t * 11f).toInt().mod(MARCOS_ALA)
    ZaziState.PENSANDO, ZaziState.DURMIENDO, ZaziState.TRISTE, ZaziState.BASE -> 0
}

private fun ZaziState.vista(): VistaSprite = when (this) {
    ZaziState.CELEBRACION, ZaziState.BASE -> VistaSprite.FRONTAL
    else -> VistaSprite.PERFIL
}

private fun spritesDe(estado: ZaziState): List<ImageBitmap> = when (estado) {
    ZaziState.VUELO_IDLE -> CacheSpritesZazi.obtener(VarianteSprite.VUELO)
    ZaziState.CELEBRACION -> CacheSpritesZazi.obtener(VarianteSprite.FIESTA)
    ZaziState.PENSANDO -> CacheSpritesZazi.obtener(VarianteSprite.PENSANDO)
    ZaziState.DURMIENDO -> CacheSpritesZazi.obtener(VarianteSprite.PIJAMA)
    ZaziState.TRISTE -> CacheSpritesZazi.obtener(VarianteSprite.TRISTE)
    ZaziState.BASE -> CacheSpritesZazi.obtener(VarianteSprite.BASE)
}

/**
 * Cada pose se rasteriza una sola vez (14 KB por bitmap). Las poses quietas
 * comparten el mismo bitmap entre sus tres marcos; solo el vuelo y la fiesta
 * construyen tres siluetas de ala distintas.
 */
private object CacheSpritesZazi {
    private val cache = HashMap<VarianteSprite, List<ImageBitmap>>()

    fun obtener(variante: VarianteSprite): List<ImageBitmap> =
        cache.getOrPut(variante) {
            when (variante) {
                VarianteSprite.BASE -> repetir(construirBase())
                VarianteSprite.VUELO ->
                    List(MARCOS_ALA) { marco -> construirVuelo(marco) }
                VarianteSprite.FIESTA ->
                    List(MARCOS_ALA) { marco -> construirFiesta(marco) }
                VarianteSprite.PENSANDO -> repetir(construirPensando())
                VarianteSprite.PIJAMA -> repetir(construirDurmiendo())
                VarianteSprite.TRISTE -> repetir(construirTriste())
            }
        }

    private fun repetir(imagen: ImageBitmap): List<ImageBitmap> =
        List(MARCOS_ALA) { imagen }
}

// ---------------------------------------------------------------------------
//  CONSTRUCCIÓN DE SPRITES
// ---------------------------------------------------------------------------

private val COLA_MORADA = intArrayOf(
    ARGB_MORADO,
    ARGB_AZUL,
    ARGB_MORADO,
    ARGB_AZUL,
    ARGB_MORADO
)

private val COLA_VUELO = intArrayOf(ARGB_MORADO, ARGB_AZUL, ARGB_MORADO, ARGB_AZUL)

private val COLA_CORTA = intArrayOf(ARGB_MORADO, ARGB_AZUL, ARGB_MORADO)

private val FRANJAS_ALA = intArrayOf(ARGB_ROSA, ARGB_AZUL, ARGB_MORADO)

/** BASE: frontal, alas extendidas, cola en abanico. */
private fun construirBase(): ImageBitmap {
    val l = LienzoPixeles()
    l.cola(32f, 37.5f, 48f, 132f, floatArrayOf(11f, 14f, 16f, 14f, 11f), COLA_MORADA)
    l.sellar()
    l.ala(40f, 32f, -74f, -8f, 25f, 10f, 17.5f)
    l.sellar()
    l.ala(24f, 32f, 188f, 254f, 25f, 10f, 17.5f)
    l.sellar()

    l.elipse(32f, 33f, 8.5f, 8f, ARGB_MENTA)
    l.elipse(32f, 36f, 5.5f, 4.5f, ARGB_CREMA)
    l.elipse(32f, 29.5f, 5.5f, 4f, ARGB_ROSA)
    l.sellar()

    l.elipse(32f, 17f, 10f, 9.5f, ARGB_MENTA)
    l.elipse(22.5f, 22.5f, 2.2f, 1.8f, ARGB_ROSA)
    l.elipse(41.5f, 22.5f, 2.2f, 1.8f, ARGB_ROSA)
    l.ojoFrontal(27.5f, 18f)
    l.ojoFrontal(36.5f, 18f)
    l.picoFrontal(abierto = false)
    l.sellar()

    l.patas(30, 34, 42, 44)
    l.sellar()
    return l.imagen()
}

/** VUELO_IDLE: tres cuartos, aleteo en tres posiciones, líneas de velocidad. */
private fun construirVuelo(marco: Int): ImageBitmap {
    val l = LienzoPixeles()
    l.cola(31f, 42f, 40f, 110f, floatArrayOf(9f, 12f, 13f, 11f), COLA_VUELO)
    l.sellar()

    val (desde, hasta) = when (marco) {
        0 -> -86f to -20f
        1 -> -72f to -6f
        else -> -56f to 10f
    }
    l.ala(32f, 26f, desde, hasta, 24f, 9f, 16.5f)
    l.sellar()

    l.elipse(31f, 34f, 7f, 8f, ARGB_MENTA)
    l.elipse(29f, 37f, 4f, 3.5f, ARGB_CREMA)
    l.elipse(27f, 30f, 3.5f, 4f, ARGB_ROSA)
    l.sellar()

    l.elipse(24f, 22f, 10f, 9f, ARGB_MENTA)
    l.elipse(16.5f, 26.5f, 2.2f, 1.8f, ARGB_ROSA)
    l.ojoPerfil(18.5f, 22f)
    l.picoPerfilIzquierda(puntaX = 7f, y = 23f, finX = 15f)
    l.sellar()

    l.patas(29, 33, 43, 44)
    l.sellar()

    l.trazo(2f, 36f, 10f, 36f, ARGB_AZUL)
    l.trazo(1f, 40f, 8f, 40f, ARGB_AZUL)
    l.trazo(4f, 44f, 12f, 44f, ARGB_AZUL)
    l.trazo(8f, 47f, 14f, 47f, ARGB_AZUL)
    l.destello(52f, 9f, ARGB_CREMA)
    l.destello(47f, 5f, ARGB_ORO)
    return l.imagen()
}

/** CELEBRACIÓN: frontal, ojos de estrella, pico abierto, alas arriba. */
private fun construirFiesta(marco: Int): ImageBitmap {
    val l = LienzoPixeles()
    l.cola(32f, 38f, 56f, 124f, floatArrayOf(10f, 12f, 13f, 12f, 10f), COLA_MORADA)
    l.sellar()

    val (desde, hasta) = when (marco) {
        0 -> -102f to -40f
        1 -> -110f to -50f
        else -> -94f to -30f
    }
    l.ala(38f, 29f, desde, hasta, 23f, 8f, 15f)
    l.sellar()
    l.ala(26f, 29f, 180f - hasta, 180f - desde, 23f, 8f, 15f)
    l.sellar()

    l.elipse(32f, 33f, 8.5f, 8f, ARGB_MENTA)
    l.elipse(32f, 36f, 5.5f, 4.5f, ARGB_CREMA)
    l.elipse(32f, 29.5f, 5.5f, 4f, ARGB_ROSA)
    l.sellar()

    l.elipse(32f, 17f, 10f, 9.5f, ARGB_MENTA)
    l.elipse(22.5f, 22.5f, 2.2f, 1.8f, ARGB_ROSA)
    l.elipse(41.5f, 22.5f, 2.2f, 1.8f, ARGB_ROSA)
    l.estrellaOjo(27.5f, 18f)
    l.estrellaOjo(36.5f, 18f)
    l.picoFrontal(abierto = true)
    l.sellar()

    l.patas(29, 35, 42, 43)
    l.sellar()
    return l.imagen()
}

/** PENSANDO: posado, ala tocando el pico, signo de interrogación. */
private fun construirPensando(): ImageBitmap {
    val l = LienzoPixeles()
    l.cola(32f, 42f, 150f, 200f, floatArrayOf(8f, 10f, 9f), COLA_CORTA)
    l.sellar()

    l.elipse(32f, 37f, 8f, 8.5f, ARGB_MENTA)
    l.elipse(32f, 41f, 5f, 4f, ARGB_CREMA)
    l.elipse(33f, 34f, 5f, 3.5f, ARGB_ROSA)
    l.sellar()

    l.elipse(24f, 27f, 9f, 8.5f, ARGB_MENTA)
    l.elipse(30f, 31f, 3f, 2f, ARGB_ROSA)
    l.ojoPerfil(19f, 27f)
    l.picoPerfilIzquierda(puntaX = 6f, y = 28f, finX = 15f)
    l.sellar()

    l.ala(28f, 34f, 195f, 225f, 14f, 5f, 9.5f)
    l.sellar()

    l.patas(28, 34, 44, 45)
    l.sellar()
    return l.imagen()
}

/** DURMIENDO: rama, gorro a rayas, ojo cerrado, cuerpo en piyama. */
private fun construirDurmiendo(): ImageBitmap {
    val l = LienzoPixeles()
    l.rama()
    l.sellar()
    l.cola(35f, 37f, 145f, 200f, floatArrayOf(8f, 10f, 9f), COLA_CORTA)
    l.sellar()

    l.elipse(40f, 36f, 8f, 8f, ARGB_MORADO)
    l.franjasPijama(40f, 36f, 8f, 8f, intArrayOf(33, 36, 39, 42))
    l.elipse(39f, 41f, 4.5f, 3f, ARGB_CREMA)
    l.sellar()

    l.elipse(34f, 27f, 9f, 8.5f, ARGB_MENTA)
    l.elipse(39f, 31f, 3f, 2f, ARGB_ROSA)
    l.linea(29f, 27f, 33f, 27f, ARGB_TINTA)
    l.poner(28, 28, ARGB_TINTA)
    l.picoPerfilDerecha(internoX = 43f, y = 28f, puntaX = 51f)
    l.sellar()

    l.elipse(34f, 18f, 8.5f, 5f, ARGB_MORADO)
    l.rectangulo(26, 22, 42, 23, ARGB_CREMA)
    l.linea(28f, 21f, 31f, 15f, ARGB_CREMA)
    l.linea(33f, 22f, 37f, 15f, ARGB_CREMA)
    l.poligono(floatArrayOf(38f, 17f, 46f, 14f, 42f, 21f), ARGB_MORADO)
    l.linea(39f, 18f, 44f, 16f, ARGB_CREMA)
    l.elipse(48f, 14f, 2.5f, 2.5f, ARGB_BLANCO)
    l.sellar()

    l.ala(42f, 32f, 30f, 80f, 11f, 4.5f, 7.5f)
    l.sellar()
    l.patas(36, 42, 44, 45)
    l.sellar()
    return l.imagen()
}

/** TRISTE: alas caídas, cabeza baja, ceja y gota azul. */
private fun construirTriste(): ImageBitmap {
    val l = LienzoPixeles()
    l.cola(35f, 42f, 140f, 200f, floatArrayOf(8f, 10f, 9f), COLA_CORTA)
    l.sellar()

    l.elipse(36f, 36f, 8f, 9f, ARGB_MENTA)
    l.elipse(37f, 42f, 4.5f, 3.5f, ARGB_CREMA)
    l.elipse(33f, 38f, 3.5f, 4f, ARGB_ROSA)
    l.sellar()

    l.elipse(31f, 25f, 9f, 8.5f, ARGB_MENTA)
    l.elipse(26f, 29f, 3f, 2f, ARGB_ROSA)
    l.linea(21f, 21f, 25f, 22f, ARGB_TINTA)
    l.linea(21f, 24f, 24f, 25f, ARGB_TINTA)
    l.poner(23, 26, ARGB_TINTA)
    l.poner(24, 26, ARGB_TINTA)
    l.picoPerfilIzquierda(puntaX = 20f, y = 27f, finX = 28f)
    l.sellar()

    l.ala(39f, 32f, 60f, 105f, 16f, 6f, 11f)
    l.sellar()
    l.patas(34, 40, 45, 46)
    l.sellar()
    return l.imagen()
}

// ---------------------------------------------------------------------------
//  RASGOS COMPARTIDOS
// ---------------------------------------------------------------------------

private fun LienzoPixeles.ala(
    baseX: Float,
    baseY: Float,
    desde: Float,
    hasta: Float,
    radioMax: Float,
    corte1: Float,
    corte2: Float,
    colores: IntArray = FRANJAS_ALA
) {
    sector(baseX, baseY, desde, hasta, 0f, corte1, colores[0])
    sector(baseX, baseY, desde, hasta, corte1, corte2, colores[1])
    sector(baseX, baseY, desde, hasta, corte2, radioMax, colores[2])
    separadorArco(baseX, baseY, desde, hasta, corte1)
    separadorArco(baseX, baseY, desde, hasta, corte2)
}

private fun LienzoPixeles.separadorArco(
    baseX: Float,
    baseY: Float,
    desde: Float,
    hasta: Float,
    radio: Float
) {
    val pasos = max(2, ((hasta - desde) / 3f).roundToInt())
    for (k in 0..pasos) {
        val grados = desde + (hasta - desde) * k / pasos
        val rad = grados * PI.toFloat() / 180f
        poner(
            (baseX + cos(rad) * radio).roundToInt(),
            (baseY + sin(rad) * radio).roundToInt(),
            ARGB_TINTA
        )
    }
}

/** Cola en abanico: un sector por pluma, separados por 1 px negro. */
private fun LienzoPixeles.cola(
    baseX: Float,
    baseY: Float,
    desde: Float,
    hasta: Float,
    radios: FloatArray,
    colores: IntArray
) {
    val n = colores.size
    val paso = (hasta - desde) / n
    for (i in 0 until n) {
        sector(
            baseX,
            baseY,
            desde + paso * i,
            desde + paso * (i + 1),
            0f,
            radios[i % radios.size],
            colores[i]
        )
    }
    for (i in 1 until n) {
        val rad = (desde + paso * i) * PI.toFloat() / 180f
        val radio = radios[(i - 1) % radios.size]
        linea(
            baseX,
            baseY,
            baseX + cos(rad) * radio,
            baseY + sin(rad) * radio,
            ARGB_TINTA
        )
    }
}

private fun LienzoPixeles.ojoFrontal(cx: Float, cy: Float) {
    val x0 = (cx - 2.5f).roundToInt()
    val y0 = (cy - 3f).roundToInt()
    for (dy in 0..5) {
        for (dx in 0..4) {
            val esquina = (dy == 0 || dy == 5) && (dx == 0 || dx == 4)
            if (!esquina) poner(x0 + dx, y0 + dy, ARGB_TINTA)
        }
    }
    poner(x0 + 1, y0 + 1, ARGB_BLANCO)
    poner(x0 + 2, y0 + 1, ARGB_BLANCO)
    poner(x0 + 1, y0 + 2, ARGB_BLANCO)
}

private fun LienzoPixeles.ojoPerfil(cx: Float, cy: Float) {
    val x0 = (cx - 2f).roundToInt()
    val y0 = (cy - 2.5f).roundToInt()
    for (dy in 0..4) {
        for (dx in 0..3) {
            val esquina = (dy == 0 || dy == 4) && (dx == 0 || dx == 3)
            if (!esquina) poner(x0 + dx, y0 + dy, ARGB_TINTA)
        }
    }
    poner(x0 + 1, y0 + 1, ARGB_BLANCO)
    poner(x0 + 1, y0 + 2, ARGB_BLANCO)
}

private fun LienzoPixeles.picoFrontal(abierto: Boolean) {
    if (!abierto) {
        poligono(floatArrayOf(29.5f, 20.5f, 34.5f, 20.5f, 32f, 26f), ARGB_TINTA)
    } else {
        poligono(floatArrayOf(29f, 19f, 35f, 19f, 32f, 23f), ARGB_TINTA)
        poligono(floatArrayOf(30f, 27f, 34f, 27f, 32f, 24f), ARGB_TINTA)
        poner(31, 24, ARGB_ROSA)
        poner(32, 24, ARGB_ROSA)
    }
}

private fun LienzoPixeles.picoPerfilIzquierda(puntaX: Float, y: Float, finX: Float) {
    poligono(
        floatArrayOf(puntaX, y - 1.5f, finX, y - 2.5f, finX, y + 1.5f),
        ARGB_TINTA
    )
    poner(puntaX.roundToInt() + 1, y.roundToInt() + 2, ARGB_TINTA)
}

private fun LienzoPixeles.picoPerfilDerecha(internoX: Float, y: Float, puntaX: Float) {
    poligono(
        floatArrayOf(puntaX, y - 1.5f, internoX, y - 2.5f, internoX, y + 1.5f),
        ARGB_TINTA
    )
    poner(puntaX.roundToInt() - 1, y.roundToInt() + 2, ARGB_TINTA)
}

private fun LienzoPixeles.patas(xA: Int, xB: Int, y0: Int, y1: Int) {
    rectangulo(xA, y0, xA, y1, ARGB_TINTA)
    rectangulo(xB, y0, xB, y1, ARGB_TINTA)
    poner(xA - 1, y1 + 1, ARGB_TINTA)
    poner(xA + 1, y1 + 1, ARGB_TINTA)
    poner(xB - 1, y1 + 1, ARGB_TINTA)
    poner(xB + 1, y1 + 1, ARGB_TINTA)
}

private val CONTORNO_ESTRELLA = arrayOf("..X..", ".XXX.", "XXXXX", ".XXX.", "..X..")
private val ORO_ESTRELLA = arrayOf(".X.", "XXX", ".X.")

private fun LienzoPixeles.estrellaOjo(cx: Float, cy: Float) {
    val ix = cx.roundToInt()
    val iy = cy.roundToInt()
    CONTORNO_ESTRELLA.forEachIndexed { dy, fila ->
        fila.forEachIndexed { dx, celda ->
            if (celda == 'X') poner(ix - 2 + dx, iy - 2 + dy, ARGB_TINTA)
        }
    }
    ORO_ESTRELLA.forEachIndexed { dy, fila ->
        fila.forEachIndexed { dx, celda ->
            if (celda == 'X') poner(ix - 1 + dx, iy - 1 + dy, ARGB_ORO)
        }
    }
}

private fun LienzoPixeles.franjasPijama(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    filas: IntArray
) {
    for (fila in filas) {
        for (x in 0 until ANCHO_SPRITE) {
            if (dentroElipse(x, fila, cx, cy, rx, ry)) poner(x, fila, ARGB_CREMA)
        }
    }
}

private fun LienzoPixeles.rama() {
    rectangulo(2, 47, 61, 49, ARGB_MARRON)
    rectangulo(4, 50, 59, 50, ARGB_MARRON_SOMBRA)
    rectangulo(18, 46, 21, 50, ARGB_MARRON_SOMBRA)
    linea(55f, 47f, 61f, 42f, ARGB_MARRON)
    linea(56f, 47f, 61f, 43f, ARGB_MARRON_SOMBRA)
}

private fun LienzoPixeles.destello(x: Float, y: Float, color: Int) {
    val ix = x.roundToInt()
    val iy = y.roundToInt()
    pintar(ix, iy, color)
    pintar(ix - 1, iy, color)
    pintar(ix + 1, iy, color)
    pintar(ix, iy - 1, color)
    pintar(ix, iy + 1, color)
}

// ---------------------------------------------------------------------------
//  LIENZO DE PÍXELES
// ---------------------------------------------------------------------------

/**
 * Pinta sobre una capa transparente y la estampa sobre el sprite con un
 * contorno negro de 1 px. Al sellar cada pieza por separado, el borde entre la
 * cabeza y el cuerpo (o entre el ala y el cuerpo) también queda marcado.
 */
private class LienzoPixeles {
    private val px = IntArray(ANCHO_SPRITE * ALTO_SPRITE)
    private var capa = IntArray(ANCHO_SPRITE * ALTO_SPRITE)

    fun poner(x: Int, y: Int, color: Int) {
        if (x in 0 until ANCHO_SPRITE && y in 0 until ALTO_SPRITE) {
            capa[y * ANCHO_SPRITE + x] = color and 0xFFFFFF
        }
    }

    /** Pinta directo en el sprite, sin contorno: destellos y líneas de aire. */
    fun pintar(x: Int, y: Int, color: Int) {
        if (x in 0 until ANCHO_SPRITE && y in 0 until ALTO_SPRITE) {
            px[y * ANCHO_SPRITE + x] = color
        }
    }

    fun sellar() {
        for (y in 0 until ALTO_SPRITE) {
            for (x in 0 until ANCHO_SPRITE) {
                val indice = y * ANCHO_SPRITE + x
                if (capa[indice] != 0) continue
                val vecino =
                    (x > 0 && capa[indice - 1] != 0) ||
                        (x < ANCHO_SPRITE - 1 && capa[indice + 1] != 0) ||
                        (y > 0 && capa[indice - ANCHO_SPRITE] != 0) ||
                        (y < ALTO_SPRITE - 1 && capa[indice + ANCHO_SPRITE] != 0)
                if (vecino) px[indice] = ARGB_TINTA
            }
        }
        for (indice in px.indices) {
            if (capa[indice] != 0) px[indice] = OPACO or capa[indice]
        }
        capa.fill(0)
    }

    fun rectangulo(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        for (y in y0..y1) {
            for (x in x0..x1) poner(x, y, color)
        }
    }

    fun elipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Int) {
        val desdeX = floor(cx - rx).toInt()
        val hastaX = ceil(cx + rx).toInt()
        val desdeY = floor(cy - ry).toInt()
        val hastaY = ceil(cy + ry).toInt()
        for (y in desdeY..hastaY) {
            for (x in desdeX..hastaX) {
                if (dentroElipse(x, y, cx, cy, rx, ry)) poner(x, y, color)
            }
        }
    }

    fun linea(x0: Float, y0: Float, x1: Float, y1: Float, color: Int) {
        val dx = x1 - x0
        val dy = y1 - y0
        val pasos = max(abs(dx), abs(dy)).roundToInt().coerceAtLeast(1)
        for (i in 0..pasos) {
            val t = i / pasos.toFloat()
            poner(
                floor(x0 + dx * t + 0.5f).toInt(),
                floor(y0 + dy * t + 0.5f).toInt(),
                color
            )
        }
    }

    fun trazo(x0: Float, y0: Float, x1: Float, y1: Float, color: Int) {
        val dx = x1 - x0
        val dy = y1 - y0
        val pasos = max(abs(dx), abs(dy)).roundToInt().coerceAtLeast(1)
        for (i in 0..pasos) {
            val t = i / pasos.toFloat()
            pintar(
                floor(x0 + dx * t + 0.5f).toInt(),
                floor(y0 + dy * t + 0.5f).toInt(),
                color
            )
        }
    }

    /** Sector de corona: franja de ala entre dos radios. */
    fun sector(
        baseX: Float,
        baseY: Float,
        desde: Float,
        hasta: Float,
        radioInterior: Float,
        radioExterior: Float,
        color: Int
    ) {
        val puntos = FloatArray((SEGMENTOS_ARCO + 1) * 4)
        var p = 0
        for (k in 0..SEGMENTOS_ARCO) {
            val grados = desde + (hasta - desde) * k / SEGMENTOS_ARCO
            val rad = grados * PI.toFloat() / 180f
            puntos[p++] = baseX + cos(rad) * radioExterior
            puntos[p++] = baseY + sin(rad) * radioExterior
        }
        for (k in SEGMENTOS_ARCO downTo 0) {
            val grados = desde + (hasta - desde) * k / SEGMENTOS_ARCO
            val rad = grados * PI.toFloat() / 180f
            puntos[p++] = baseX + cos(rad) * radioInterior
            puntos[p++] = baseY + sin(rad) * radioInterior
        }
        poligono(puntos, color)
    }

    fun poligono(puntos: FloatArray, color: Int) {
        val n = puntos.size / 2
        if (n < 3) return
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until n) {
            val y = puntos[i * 2 + 1]
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val desdeY = floor(minY).toInt().coerceAtLeast(0)
        val hastaY = ceil(maxY).toInt().coerceAtMost(ALTO_SPRITE - 1)
        val cortes = FloatArray(n)
        for (y in desdeY..hastaY) {
            val sy = y + 0.5f
            var c = 0
            for (i in 0 until n) {
                val j = (i + 1) % n
                val x1 = puntos[i * 2]
                val y1 = puntos[i * 2 + 1]
                val x2 = puntos[j * 2]
                val y2 = puntos[j * 2 + 1]
                if ((y1 <= sy && sy < y2) || (y2 <= sy && sy < y1)) {
                    cortes[c++] = x1 + (sy - y1) * (x2 - x1) / (y2 - y1)
                }
            }
            if (c >= 2) {
                cortes.sort(0, c)
                var k = 0
                while (k + 1 < c) {
                    val desdeX = ceil(cortes[k]).toInt()
                    val hastaX = floor(cortes[k + 1]).toInt()
                    for (x in desdeX..hastaX) poner(x, y, color)
                    k += 2
                }
            }
        }
    }

    fun imagen(): ImageBitmap {
        val bitmap = Bitmap.createBitmap(
            ANCHO_SPRITE,
            ALTO_SPRITE,
            Bitmap.Config.ARGB_8888
        )
        bitmap.setPixels(px, 0, ANCHO_SPRITE, 0, 0, ANCHO_SPRITE, ALTO_SPRITE)
        return bitmap.asImageBitmap()
    }
}

private fun dentroElipse(
    x: Int,
    y: Int,
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float
): Boolean {
    val dx = (x + 0.5f - cx) / rx
    val dy = (y + 0.5f - cy) / ry
    return dx * dx + dy * dy <= 1f
}

// ---------------------------------------------------------------------------
//  PARTÍCULAS Y DETALLES DE ESTADO
// ---------------------------------------------------------------------------

private fun DrawScope.dibujarParpadeo(
    estado: ZaziState,
    origen: Offset,
    celda: Float
) {
    val (x, y) = if (estado == ZaziState.PENSANDO) 17f to 25f else 17f to 20f
    drawRect(
        color = MentaZazi,
        topLeft = Offset(origen.x + x * celda, origen.y + y * celda),
        size = Size(4f * celda, 5f * celda)
    )
    drawRect(
        color = TintaZazi,
        topLeft = Offset(origen.x + x * celda, origen.y + (y + 2f) * celda),
        size = Size(4f * celda, celda)
    )
}

private fun DrawScope.dibujarParticulasEstado(
    estado: ZaziState,
    t: Float,
    origen: Offset,
    celda: Float,
    ancho: Float,
    alto: Float
) {
    when (estado) {
        ZaziState.CELEBRACION -> dibujarFestejo(t, origen, celda, ancho, alto)
        ZaziState.DURMIENDO -> dibujarZzz(t, origen, celda)
        ZaziState.PENSANDO -> dibujarPensamiento(t, origen, celda)
        ZaziState.TRISTE -> dibujarLagrima(t, origen, celda)
        ZaziState.VUELO_IDLE, ZaziState.BASE -> Unit
    }
}

private fun DrawScope.dibujarFestejo(
    t: Float,
    origen: Offset,
    celda: Float,
    ancho: Float,
    alto: Float
) {
    val ciclo = (t / 1.6f) % 1f
    repeat(8) { indice ->
        val fase = (ciclo + indice / 8f) % 1f
        val alfa = (1f - fase) * 0.85f
        if (alfa <= 0.01f) return@repeat
        val x = origen.x +
            ancho * (0.14f + (indice % 4) * 0.24f) +
            sin(fase * 2f * PI.toFloat() + indice) * celda * 1.4f
        val y = origen.y + alto * (0.46f - fase * 0.42f)
        val tam = celda * (0.9f + (indice % 3) * 0.22f) * (0.75f + fase * 0.35f)

        when (indice.mod(3)) {
            0 -> dibujarCorazonPixel(Offset(x, y), tam, RosaZazi.copy(alpha = alfa))
            1 -> dibujarEstrellaPixel(Offset(x, y), tam, OroZazi.copy(alpha = alfa))
            else -> dibujarEstrellaPixel(
                Offset(x, y),
                tam,
                if (indice.mod(2) == 0) MoradoZazi.copy(alpha = alfa) else AzulZazi.copy(alpha = alfa)
            )
        }
    }
}

private fun DrawScope.dibujarCorazonPixel(centro: Offset, u: Float, color: Color) {
    fun bloque(dx: Float, dy: Float, anchoBloque: Float, altoBloque: Float) {
        drawRect(
            color = color,
            topLeft = Offset(centro.x + dx * u, centro.y + dy * u),
            size = Size(anchoBloque * u, altoBloque * u)
        )
    }
    bloque(-2.5f, -2.5f, 2f, 2f)
    bloque(0.5f, -2.5f, 2f, 2f)
    bloque(-2.5f, -0.5f, 5f, 2.5f)
    bloque(-1.5f, 2f, 3f, 1f)
    bloque(-0.5f, 3f, 1f, 1f)
}

private fun DrawScope.dibujarEstrellaPixel(centro: Offset, u: Float, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(centro.x - 0.5f * u, centro.y - 2.5f * u),
        size = Size(u, 5f * u)
    )
    drawRect(
        color = color,
        topLeft = Offset(centro.x - 2.5f * u, centro.y - 0.5f * u),
        size = Size(5f * u, u)
    )
    drawRect(
        color = color.copy(alpha = color.alpha * 0.7f),
        topLeft = Offset(centro.x - 1.5f * u, centro.y - 1.5f * u),
        size = Size(3f * u, 3f * u)
    )
}

private fun DrawScope.dibujarZzz(t: Float, origen: Offset, celda: Float) {
    val base = Offset(origen.x + 49f * celda, origen.y + 12f * celda)
    repeat(3) { indice ->
        val fase = ((t / 3.2f) + indice / 3f) % 1f
        val alfa = (1f - fase) * 0.78f
        if (alfa <= 0.01f) return@repeat
        val centro = Offset(
            x = base.x + fase * celda * 3.6f + indice * celda * 1.4f,
            y = base.y - fase * celda * 5.5f - indice * celda * 1.8f
        )
        dibujarLetraZ(
            centro = centro,
            u = celda * (0.8f + fase * 0.7f),
            color = MoradoZazi.copy(alpha = alfa)
        )
    }
}

private fun DrawScope.dibujarLetraZ(centro: Offset, u: Float, color: Color) {
    val grosor = max(1f, u * 0.6f)
    drawRect(
        color,
        Offset(centro.x, centro.y),
        Size(3f * u, grosor)
    )
    drawRect(
        color,
        Offset(centro.x, centro.y + 3f * u),
        Size(3f * u, grosor)
    )
    for (paso in 0..2) {
        drawRect(
            color,
            Offset(centro.x + (2 - paso) * u, centro.y + paso * u),
            Size(u, u)
        )
    }
}

private fun DrawScope.dibujarPensamiento(t: Float, origen: Offset, celda: Float) {
    val flotar = sin(t * 1.7f) * celda * 0.55f
    val centro = Offset(
        x = origen.x + 41f * celda,
        y = origen.y + 10f * celda + flotar
    )
    val pulso = 0.85f + 0.15f * sin(t * 2.2f)
    dibujarInterrogacion(centro, celda * 1.15f, TintaZazi.copy(alpha = pulso))
}

private fun DrawScope.dibujarInterrogacion(centro: Offset, u: Float, color: Color) {
    val grosor = max(1f, u * 0.7f)
    drawRect(color, Offset(centro.x - 1.5f * u, centro.y - 2.8f * u), Size(3f * u, grosor))
    drawRect(color, Offset(centro.x + 1.2f * u, centro.y - 2.5f * u), Size(grosor, 1.6f * u))
    drawRect(color, Offset(centro.x - 1.5f * u, centro.y - 1.2f * u), Size(grosor, 1f * u))
    drawRect(color, Offset(centro.x - 0.5f * u, centro.y - 0.2f * u), Size(grosor, 1.4f * u))
    drawRect(color, Offset(centro.x - 0.5f * u, centro.y + 2f * u), Size(grosor, 1.1f * u))
}

private fun DrawScope.dibujarLagrima(t: Float, origen: Offset, celda: Float) {
    val ciclo = (t / 2.6f) % 1f
    val alfa = (1f - ciclo).coerceIn(0f, 1f) * 0.85f
    if (alfa <= 0.01f) return
    val x = origen.x + 22f * celda
    val y = origen.y + (27f + ciclo * 5f) * celda
    val gota = Path().apply {
        moveTo(x, y - 2.2f * celda)
        quadraticTo(x + 1.8f * celda, y + 0.6f * celda, x, y + 1.7f * celda)
        quadraticTo(x - 1.8f * celda, y + 0.6f * celda, x, y - 2.2f * celda)
        close()
    }
    drawPath(gota, AzulZazi.copy(alpha = alfa))
    drawCircle(
        color = BlancoZazi.copy(alpha = alfa * 0.7f),
        radius = 0.4f * celda,
        center = Offset(x - 0.4f * celda, y)
    )
}
