package com.example.zazili.presentation.avatar

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.avatar.AvatarItem
import com.example.zazili.domain.avatar.CapaAvatar
import com.example.zazili.domain.avatar.EquipamientoAvatar
import kotlin.math.floor

/**
 * Retrato pixel art del niño, armado capa por capa con Z-index estricto.
 *
 * RULE 1 — cero arte vectorial: este archivo no dibuja nada del retrato
 * humano. Cada pieza es un PNG que vive en `res/drawable-nodpi` y se pinta
 * con [Image] + [FilterQuality.None]. No hay `Canvas`, `Path`, `Paint` ni
 * `ImageVector` aquí; los píxeles quedan duros y nítidos a cualquier tamaño,
 * igual que el compañero Zazi.
 *
 * MANDATO ESTÉTICO — chibi moderno de 16 bits: el lienzo lógico es de 64×64.
 * Al escalar se busca un múltiplo entero (2×, 3×, 4×…) para que cada píxel
 * del arte caiga sobre una rejilla exacta de píxeles de pantalla; solo en
 * miniaturas menores a 128 dp se usa un factor fraccionario, siempre con
 * `FilterQuality.None` para no interpolar color. Así conviven cabeza grande
 * (chibi) y texturas finas de la ropa (charro, máscaras) sin barrido bilineal.
 *
 * ORDEN DE Z ESTRICTO, de atrás hacia adelante (jamás se reordena):
 *
 *  1. Fondo pixel ([CapaRetrato.FONDO])
 *  2. Cabello trasero: melenas largas ([CapaRetrato.PELO_ATRAS])
 *  3. Accesorio de espalda: alas, capas, mochilas ([CapaRetrato.ESPALDA])
 *  4. Cuerpo y tono de piel ([CapaRetrato.CUERPO])
 *  5. Ropa inferior: pantalón, falda, pollera ([CapaRetrato.ROPA_INFERIOR])
 *  6. Ropa superior: camiseta, blusa, armadura ([CapaRetrato.ROPA_SUPERIOR])
 *  7. Ojos ([CapaRetrato.OJOS]) y boca ([CapaRetrato.BOCA])
 *  8. Cabello frontal: flequillo y mechones ([CapaRetrato.PELO_FRENTE])
 *  9. Accesorios de rostro: lentes, máscaras, maquillaje ([CapaRetrato.ROSTRO])
 * 10. Sombreros, coronas y tocados ([CapaRetrato.CABEZA])
 *
 * Los sprites se buscan con el nombre `avatar_<categoria>_<resourceId>_<capa>.png`.
 * Si un PNG no existe, la capa se omite en silencio y se registra una sola
 * advertencia: el avatar nunca se rompe ni cae a dibujo procedural.
 */
@Composable
fun ChildAvatarRenderer(
    equipados: List<String>,
    modifier: Modifier = Modifier,
    mostrarFondo: Boolean = true,
    animar: Boolean = true,
    descripcion: String? = null,
    tonoPiel: Color? = null
) {
    val equipamiento = remember(equipados) {
        AvatarContentFactory.equipamientoDesdeIds(equipados)
    }
    ChildAvatarRenderer(
        equipamiento = equipamiento,
        modifier = modifier,
        mostrarFondo = mostrarFondo,
        animar = animar,
        descripcion = descripcion,
        tonoPiel = tonoPiel
    )
}

/**
 * Variante que recibe el equipo ya resuelto.
 *
 * [tonoPiel] es el tinte dinámico opcional del cuerpo: se multiplica sobre el
 * PNG de [CapaRetrato.CUERPO] con [BlendMode.Modulate], así un mismo sprite
 * sirve para varias pieles sin duplicar archivos. Con `null` (por defecto) el
 * cuerpo conserva los colores originales del PNG.
 *
 * El vaivén de respiración es sutil (±1.5 dp) para dar vida sin marear; el
 * Modo Zen lo apaga por completo.
 */
@Composable
fun ChildAvatarRenderer(
    equipamiento: EquipamientoAvatar,
    modifier: Modifier = Modifier,
    mostrarFondo: Boolean = true,
    animar: Boolean = true,
    descripcion: String? = null,
    tonoPiel: Color? = null
) {
    val contexto = LocalContext.current
    val zen = collectZenMode()
    val respirando = animar && !zen

    val vaiven = if (respirando) {
        val transicion = rememberInfiniteTransition(label = "vaivenPixel")
        val valor by transicion.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "vaivenPixelAvatar"
        )
        valor
    } else {
        0f
    }

    val capas = remember(equipamiento, mostrarFondo, contexto) {
        construirCapas(contexto, equipamiento, mostrarFondo)
    }
    val textoAccesible = descripcion ?: describirEquipo(equipamiento)

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer {
                translationY = vaiven * 1.5.dp.toPx()
            }
            .semantics(mergeDescendants = true) {
                contentDescription = textoAccesible
            },
        contentAlignment = Alignment.Center
    ) {
        // El lado menor dicta la caja cuadrada del lienzo de 64 dp.
        val ladoDisponible = minOf(maxWidth, maxHeight)
        val escala = escalaPixelPerfecta(ladoDisponible)

        Box(
            modifier = Modifier
                .size(LADO_BASE)
                .scale(escala)
        ) {
            capas.forEach { capa ->
                val filtro = if (capa.capa == CapaRetrato.CUERPO && tonoPiel != null) {
                    ColorFilter.tint(tonoPiel, BlendMode.Modulate)
                } else {
                    null
                }
                Image(
                    bitmap = capa.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    colorFilter = filtro
                )
            }
        }
    }
}

/** Lado lógico del lienzo con el que se dibujó todo el arte del niño. */
private val LADO_BASE: Dp = 64.dp

/**
 * Escala que mejor respeta la rejilla del sprite.
 *
 * Por encima de 2× se redondea hacia abajo a un múltiplo entero: un sprite de
 * 64 px pintado a 192 dp (3×) deja cada píxel del arte sobre 9 píxeles de
 * pantalla, sin franjas intermedias. Por debajo de 2× no hay mucho que
 * preservar, así que se usa el ajuste continuo para que la miniatura no quede
 * diminuta dentro de su tarjeta.
 */
private fun escalaPixelPerfecta(ladoDisponible: Dp): Float {
    if (!ladoDisponible.value.isFinite() || ladoDisponible <= 0.dp) return 1f
    val bruto = ladoDisponible.value / LADO_BASE.value
    return if (bruto >= 2f) floor(bruto) else bruto.coerceAtLeast(0.25f)
}

private fun describirEquipo(equipamiento: EquipamientoAvatar): String {
    val piezas = equipamiento.articulos.joinToString(", ") { it.name }
    return if (piezas.isBlank()) {
        "Tu avatar pixel sin accesorios"
    } else {
        "Tu avatar pixel con $piezas"
    }
}

// ---------------------------------------------------------------------------
//  CAPAS Y CACHÉ DE SPRITES
// ---------------------------------------------------------------------------

/**
 * Pasadas de dibujo con su Z-index definitivo. El número fija el orden de
 * pintado global; los valores dejan hueco para futuras capas sin reordenar
 * las existentes. Este orden es la única fuente de verdad del Z.
 */
private enum class CapaRetrato(val orden: Int, val sufijo: String) {
    FONDO(0, "fondo"),
    PELO_ATRAS(10, "pelo_atras"),
    ESPALDA(20, "espalda"),
    CUERPO(30, "cuerpo"),
    ROPA_INFERIOR(40, "ropa_inferior"),
    ROPA_SUPERIOR(50, "ropa_superior"),
    OJOS(60, "ojos"),
    BOCA(61, "boca"),
    PELO_FRENTE(70, "pelo_frente"),
    ROSTRO(80, "rostro"),
    CABEZA(90, "cabeza")
}

private data class CapaPintada(val capa: CapaRetrato, val bitmap: ImageBitmap)

/**
 * Construye la lista de capas visibles, ya ordenada por Z. El orden es
 * inapelable: fondo → pelo trasero → espalda → cuerpo → ropa → cara → pelo
 * frontal → accesorios de rostro → tocado. Un tocado SIEMPRE tapa al flequillo
 * y una máscara SIEMPRE tapa a los ojos: así lo dicta el mandato de Z.
 */
private fun construirCapas(
    contexto: Context,
    equipamiento: EquipamientoAvatar,
    mostrarFondo: Boolean
): List<CapaPintada> {
    val capas = ArrayList<CapaPintada>(11)

    if (mostrarFondo) {
        SpritesPixel.fondo(contexto)?.let {
            capas += CapaPintada(CapaRetrato.FONDO, it)
        }
    }

    equipamiento.pelo?.takeIf { it.ocupa(CapaAvatar.PELO_ATRAS) }?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.PELO_ATRAS)?.let {
            capas += CapaPintada(CapaRetrato.PELO_ATRAS, it)
        }
    }

    equipamiento.espalda?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.ESPALDA)?.let {
            capas += CapaPintada(CapaRetrato.ESPALDA, it)
        }
    }

    SpritesPixel.cuerpo(contexto, equipamiento.piel)?.let {
        capas += CapaPintada(CapaRetrato.CUERPO, it)
    }

    equipamiento.ropa?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.ROPA_INFERIOR)?.let {
            capas += CapaPintada(CapaRetrato.ROPA_INFERIOR, it)
        }
        SpritesPixel.obtener(contexto, item, CapaRetrato.ROPA_SUPERIOR)?.let {
            capas += CapaPintada(CapaRetrato.ROPA_SUPERIOR, it)
        }
    }

    equipamiento.ojos?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.OJOS)?.let {
            capas += CapaPintada(CapaRetrato.OJOS, it)
        }
    }

    equipamiento.boca?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.BOCA)?.let {
            capas += CapaPintada(CapaRetrato.BOCA, it)
        }
    }

    equipamiento.pelo?.takeIf { it.ocupa(CapaAvatar.PELO_FRENTE) }?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.PELO_FRENTE)?.let {
            capas += CapaPintada(CapaRetrato.PELO_FRENTE, it)
        }
    }

    equipamiento.rostro?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.ROSTRO)?.let {
            capas += CapaPintada(CapaRetrato.ROSTRO, it)
        }
    }

    equipamiento.cabeza?.takeIf { it.esTocadoValido() }?.let { item ->
        SpritesPixel.obtener(contexto, item, CapaRetrato.CABEZA)?.let {
            capas += CapaPintada(CapaRetrato.CABEZA, it)
        }
    }

    return capas.sortedBy { it.capa.orden }
}

/**
 * El avatar debe nacer descubierto. Cualquier dato persistido de la vieja
 * `cabeza_gorra` (el "sombrero verde" fantasma) se ignora al pintar, incluso si
 * Android lo conservó en preferencias antes de esta corrección.
 */
private fun AvatarItem.esTocadoValido(): Boolean = id != "cabeza_gorra"

/**
 * Caché de bitmaps decodificados. La llave combina el artículo y la capa,
 * porque un mismo `resourceId` (por ejemplo "alebrije") vive en varias
 * ranuras. Los nombres ausentes se recuerdan para no golpear el gestor de
 * recursos en cada recomposición. Funciona igual con PNG de 64×64 o de
 * 128×128: el nombre es la única clave.
 */
private object SpritesPixel {

    private const val ETIQUETA = "AvatarPixel"

    private val cache = HashMap<String, ImageBitmap?>()
    private val faltantes = HashSet<String>()

    fun fondo(contexto: Context): ImageBitmap? = cargar(contexto, "avatar_fondo")

    fun cuerpo(contexto: Context, piel: AvatarItem?): ImageBitmap? {
        if (piel != null) {
            val propio = cargar(contexto, "${piel.spriteDrawable}_${CapaRetrato.CUERPO.sufijo}")
            if (propio != null) return propio
        }
        return cargar(contexto, "avatar_cuerpo_base")
    }

    fun obtener(contexto: Context, item: AvatarItem, capa: CapaRetrato): ImageBitmap? =
        cargar(contexto, "${item.spriteDrawable}_${capa.sufijo}")

    private fun cargar(contexto: Context, nombre: String): ImageBitmap? {
        synchronized(cache) {
            if (cache.containsKey(nombre)) return cache[nombre]
            if (nombre in faltantes) return null
        }
        val bitmap = decodificar(contexto, nombre)
        synchronized(cache) {
            if (bitmap == null) {
                faltantes += nombre
                Log.w(ETIQUETA, "Falta el sprite pixel drawable-nodpi/$nombre.png")
            }
            cache[nombre] = bitmap
        }
        return bitmap
    }

    private fun decodificar(contexto: Context, nombre: String): ImageBitmap? {
        return try {
            val id = contexto.resources.getIdentifier(nombre, "drawable", contexto.packageName)
            if (id == 0) return null
            BitmapFactory.decodeResource(contexto.resources, id)?.asImageBitmap()
        } catch (error: Exception) {
            Log.w(ETIQUETA, "No se pudo leer drawable-nodpi/$nombre.png", error)
            null
        }
    }
}
