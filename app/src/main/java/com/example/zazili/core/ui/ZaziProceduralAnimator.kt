package com.example.zazili.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.VerdeDuo
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.isActive

/**
 * Zazi viva a partir de seis láminas estáticas.
 *
 * Cada pose recibe una animación procedural distinta, siempre con
 * [FilterQuality.None] para que el pixel art no se difumine:
 *
 *  - VUELO_IDLE y BASE: flotación vertical y un latido rápido de
 *    squash & stretch (dos pulsos por ciclo).
 *  - PENSANDO: inclinación de cabeza de lado a lado.
 *  - DURMIENDO: respiración lenta y partículas "Zzz" dibujadas en Canvas.
 *  - TRISTE: caída con física de resorte, pesada y blanda.
 *  - CELEBRACION: rebote con resorte, balanceo y confeti en Canvas.
 *
 * Con [activo] en `false` o con el Modo Zen encendido no se crea ninguna
 * animación infinita: la pose queda quieta y sin partículas, para respetar
 * la accesibilidad y ahorrar batería.
 */
@Composable
fun ZaziProceduralAnimator(
    estado: ZaziState,
    modifier: Modifier = Modifier,
    tamano: Dp = 168.dp,
    activo: Boolean = true,
    descripcion: String = estado.descripcion
) {
    val zen = collectZenMode()
    val imagen = recordarZaziLimpio(estado)
    val animar = activo && !zen

    Box(
        modifier = modifier
            .size(tamano)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        if (imagen == null) return@Box

        when (estado) {
            ZaziState.PENSANDO -> CuerpoPensando(imagen, animar)
            ZaziState.DURMIENDO -> {
                CuerpoDurmiendo(imagen, animar)
                if (animar) LienzoZzz(Modifier.matchParentSize())
            }

            ZaziState.TRISTE -> CuerpoTriste(imagen, animar)
            ZaziState.CELEBRACION -> {
                CuerpoCelebracion(imagen, animar)
                if (animar) LienzoConfeti(Modifier.matchParentSize())
            }

            ZaziState.VUELO_IDLE,
            ZaziState.BASE -> CuerpoVuelo(imagen, animar)
        }
    }
}

/**
 * Reemplazo directo de `PixelArtZazi` para todas las pantallas.
 *
 * Conserva la misma firma —tamaño opcional, descripción para lectores de
 * pantalla y toque para acariciar a Zazi— pero dibuja las láminas reales ya
 * limpiadas por [ZaziGraphicCleaner] y las anima con
 * [ZaziProceduralAnimator]. Al cambiar de emoción hay un fundido breve de 230
 * ms (0 ms en Modo Zen) para que la pose anterior no desaparezca de golpe.
 */
@Composable
fun ZaziSpriteAnimado(
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
            .semantics(mergeDescendants = true) {
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
        label = "estadoSpriteZazi"
    ) { actual ->
        ZaziProceduralAnimator(
            estado = actual,
            modifier = Modifier.fillMaxSize(),
            tamano = size ?: 168.dp,
            activo = animar,
            descripcion = texto
        )
    }
}

/** Lámina limpia dentro del contenedor, sin filtrado bilineal. */
@Composable
private fun ImagenZazi(imagen: ImageBitmap, modifier: Modifier = Modifier) {
    Image(
        bitmap = imagen,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Flotación y latido. La flotación mueve 5 dp hacia arriba y abajo; el latido
 * usa keyframes para dos pulsos cortos dentro de un ciclo de 760 ms, con el
 * eje Y encogiendo mientras el X se ensancha (volumen aparente constante).
 */
@Composable
private fun CuerpoVuelo(imagen: ImageBitmap, animar: Boolean) {
    if (!animar) {
        ImagenZazi(imagen)
        return
    }
    val transicion = rememberInfiniteTransition(label = "vueloZazi")
    val flotacion by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flotacionZazi"
    )
    val latido by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 760
                0f at 0
                1f at 120 using FastOutSlowInEasing
                0.25f at 240
                0.85f at 360
                0f at 760
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "latidoZazi"
    )

    ImagenZazi(
        imagen = imagen,
        modifier = Modifier.graphicsLayer {
            translationY = (flotacion - 0.5f) * 10.dp.toPx()
            scaleX = 1f + 0.035f * latido
            scaleY = 1f - 0.028f * latido + (flotacion - 0.5f) * 0.012f
        }
    )
}

/** Inclinación pendular de la cabeza, de -7° a 7°. */
@Composable
private fun CuerpoPensando(imagen: ImageBitmap, animar: Boolean) {
    if (!animar) {
        ImagenZazi(imagen)
        return
    }
    val inclinacion by rememberInfiniteTransition(label = "pensandoZazi")
        .animateFloat(
            initialValue = -7f,
            targetValue = 7f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "inclinacionZazi"
        )

    ImagenZazi(
        imagen = imagen,
        modifier = Modifier.graphicsLayer { rotationZ = inclinacion }
    )
}

/** Respiración profunda y lenta; las "Zzz" flotan encima desde el Canvas. */
@Composable
private fun CuerpoDurmiendo(imagen: ImageBitmap, animar: Boolean) {
    if (!animar) {
        ImagenZazi(imagen)
        return
    }
    val respiracion by rememberInfiniteTransition(label = "suenoZazi")
        .animateFloat(
            initialValue = 0.975f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "respiracionZazi"
        )

    ImagenZazi(
        imagen = imagen,
        modifier = Modifier.graphicsLayer {
            scaleX = respiracion
            scaleY = respiracion
            translationY = (1f - respiracion) * 34.dp.toPx()
        }
    )
}

/**
 * Caída con resorte: entra desde la pose recta y se vence hacia abajo con un
 * rebote casi nulo (amortiguación alta) para que se sienta pesada, no juguetona.
 */
@Composable
private fun CuerpoTriste(imagen: ImageBitmap, animar: Boolean) {
    if (!animar) {
        ImagenZazi(imagen)
        return
    }
    val caida = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        caida.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.68f,
                stiffness = 95f
            )
        )
    }
    val peso = caida.value

    ImagenZazi(
        imagen = imagen,
        modifier = Modifier.graphicsLayer {
            translationY = peso * 9.dp.toPx()
            scaleY = 1f - 0.10f * peso
            scaleX = 1f + 0.04f * peso
            rotationZ = -2f * peso
        }
    )
}

/**
 * Rebote errático: dos resortes encadenados (subida elástica, bajada blanda)
 * se repiten mientras la pose siga activa. El balanceo cambia de sentido cada
 * 460 ms para que el movimiento nunca se vuelva predecible.
 */
@Composable
private fun CuerpoCelebracion(imagen: ImageBitmap, animar: Boolean) {
    if (!animar) {
        ImagenZazi(imagen)
        return
    }
    val salto = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            salto.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.33f,
                    stiffness = 330f
                )
            )
            salto.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = 240f
                )
            )
        }
    }
    val balanceo by rememberInfiniteTransition(label = "celebracionZazi")
        .animateFloat(
            initialValue = -4f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 460, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "balanceoZazi"
        )
    val impulso = salto.value

    ImagenZazi(
        imagen = imagen,
        modifier = Modifier.graphicsLayer {
            translationY = -impulso * 10.dp.toPx()
            scaleX = 1f + 0.05f * impulso
            scaleY = 1f + 0.04f * impulso
            rotationZ = balanceo * impulso
        }
    )
}

/**
 * Tres "Z" procedurales que suben desde el hombro derecho, se mecen y se
 * desvanecen. Se dibujan con líneas (no con texto) para conservar el trazo
 * cuadrado del pixel art y no depender de una fuente.
 */
@Composable
private fun LienzoZzz(modifier: Modifier = Modifier) {
    val avance by rememberInfiniteTransition(label = "zzzZazi")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "avanceZzz"
        )

    Canvas(modifier) {
        repeat(3) { indice ->
            val fase = (avance + indice * 0.31f) % 1f
            val subida = sin((fase * PI).toFloat())
            val mecido = sin((fase * 2f * PI).toFloat() + indice * 1.7f)
            val centro = Offset(
                x = size.width * (0.62f + 0.05f * mecido),
                y = size.height * (0.54f - 0.40f * fase)
            )
            val tamano = size.minDimension * (0.05f + 0.032f * fase)
            val alfa = subida.coerceIn(0f, 1f) * 0.85f
            dibujarZ(
                esquina = Offset(centro.x - tamano / 2f, centro.y - tamano / 2f),
                tamano = tamano,
                color = MagicalPurple.copy(alpha = alfa)
            )
        }
    }
}

private fun DrawScope.dibujarZ(esquina: Offset, tamano: Float, color: Color) {
    if (tamano <= 0f || color.alpha <= 0.01f) return
    val grosor = (tamano * 0.18f).coerceAtLeast(1.5f)
    drawLine(
        color = color,
        start = Offset(esquina.x + tamano * 0.10f, esquina.y),
        end = Offset(esquina.x + tamano * 0.90f, esquina.y),
        strokeWidth = grosor,
        cap = StrokeCap.Square
    )
    drawLine(
        color = color,
        start = Offset(esquina.x + tamano * 0.90f, esquina.y),
        end = Offset(esquina.x + tamano * 0.10f, esquina.y + tamano),
        strokeWidth = grosor,
        cap = StrokeCap.Square
    )
    drawLine(
        color = color,
        start = Offset(esquina.x + tamano * 0.10f, esquina.y + tamano),
        end = Offset(esquina.x + tamano * 0.90f, esquina.y + tamano),
        strokeWidth = grosor,
        cap = StrokeCap.Square
    )
}

/**
 * Confeti procedural: 18 rectángulos de la paleta caen con vaivén y giro. Las
 * posiciones salen de una función determinista, así que no cambian de un
 * fotograma a otro ni necesitan un `Random` en composición.
 */
@Composable
private fun LienzoConfeti(modifier: Modifier = Modifier) {
    val avance by rememberInfiniteTransition(label = "confetiZazi")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "avanceConfeti"
        )

    Canvas(modifier) {
        repeat(18) { indice ->
            val fase = (avance + indice * 0.093f) % 1f
            val vaiven = sin((fase * 2f * PI * 1.6f).toFloat() + indice)
            val centro = Offset(
                x = size.width * (0.12f + 0.76f * ruido(indice)) +
                    vaiven * size.width * 0.035f,
                y = size.height * (-0.08f + fase * 1.16f)
            )
            val ancho = size.minDimension * (0.012f + 0.010f * ruido(indice + 7))
            val alto = ancho * 1.8f
            val color = PALETA_CONFETI[indice % PALETA_CONFETI.size].copy(alpha = 0.9f)

            rotate(degrees = fase * 900f + indice * 37f, pivot = centro) {
                drawRect(
                    color = color,
                    topLeft = Offset(centro.x - ancho / 2f, centro.y - alto / 2f),
                    size = Size(ancho, alto)
                )
            }
        }
    }
}

private val PALETA_CONFETI = listOf(
    MagicalPurple,
    VerdeDuo,
    AmarilloVibrante,
    RosaSuave,
    AzulCielo,
    MintGreen
)

private fun ruido(indice: Int): Float = ((indice * 37) % 101) / 100f
