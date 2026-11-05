package com.example.zazili.presentation.tienda

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.mascot.ZaziPetCareEngine
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.domain.tienda.CapaCosmetica
import com.example.zazili.domain.tienda.RopaTienda
import com.example.zazili.domain.tienda.ZaziContentFactory
import com.example.zazili.domain.tienda.ZaziCosmeticItem

/**
 * Zazi con su ropa perfectamente anclada, sin recortes.
 *
 * Tres decisiones resuelven el bug de la "ropa flotante":
 *
 *  1. La caja raíz mide [ZaziContentFactory.LADO_CAJA_DP] (200 dp) y usa
 *     [requiredSize], no [size]. El padre mide 168 dp, así que `size(200.dp)`
 *     habría sido coaccionado a 168 y el ala del sombrero quedaba fuera del
 *     área dibujable. `requiredSize` ignora las restricciones entrantes: los
 *     16 dp de aire por lado existen SIEMPRE y nada se recorta.
 *  2. Cada cosmético se dibuja en un Box propio con el orden exacto
 *     `size(...)` → `offset(...)` → `scale(...)`. El offset se aplica sobre el
 *     layout (no sobre el dibujo), así que el gorro sube de verdad y no se
 *     queda pegado al borde superior.
 *  3. El sprite va primero y las capas se pintan después, ordenadas por
 *     [CapaCosmetica.orden]: cuerpo → accesorios → sombreros. El Z-index es el
 *     orden de los hijos, sin `zIndex` implícito ni saltos por tamaño.
 *
 * El armario se lee del [ZaziPetCareEngine] con `collectAsStateWithLifecycle`:
 * en cuanto el niño se pone algo en Tienditas 3B, este composable repinta sin
 * que la pantalla tenga que avisarle. Mientras DataStore aún no responde
 * (`listo == false`) se usa la lista [ropa] que entrega el llamador.
 */
@Composable
fun ZaziPetRenderer(
    ropa: List<RopaTienda> = emptyList(),
    modifier: Modifier = Modifier,
    estado: ZaziState = ZaziState.VUELO_IDLE,
    tamano: Dp = ZaziContentFactory.TAMANO_BASE_DP.dp,
    activo: Boolean = true,
    descripcion: String? = null,
    onTap: (() -> Unit)? = null
) {
    val contexto = LocalContext.current
    val motor = remember(contexto) { ZaziPetCareEngine.obtener(contexto) }
    val cuidado by motor.estado.collectAsStateWithLifecycle()

    val capas = remember(ropa, cuidado.listo, cuidado.ropaEquipada) {
        if (cuidado.listo) {
            ZaziContentFactory.capasDeIds(cuidado.ropaEquipada.values)
        } else {
            ZaziContentFactory.capasDe(ropa)
        }
    }
    val escalaVista = (tamano.value / ZaziContentFactory.TAMANO_BASE_DP)
        .coerceIn(0.6f, 1.6f)
    val descripcionBase = descripcion ?: estado.descripcion
    val descripcionCompleta = remember(descripcionBase, capas) {
        if (capas.isEmpty()) {
            descripcionBase
        } else {
            "$descripcionBase Lleva puesta: ${capas.joinToString { it.nombre }}."
        }
    }

    Box(
        modifier = modifier.requiredSize(ZaziContentFactory.LADO_CAJA_DP.dp),
        contentAlignment = Alignment.Center
    ) {
        ZaziSpriteAnimado(
            estado = estado,
            size = tamano,
            animar = activo,
            descripcion = descripcionCompleta,
            onTap = onTap
        )

        capas.forEach { item ->
            PiezaCosmetica(item = item, escalaVista = escalaVista)
        }
    }
}

/**
 * Una capa del armario. El orden del modificador es contractual:
 * primero el tamaño de la mota, luego el anclaje y al final la escala óptica.
 * Invertir tamaño y offset desplazaría la pieza media mota.
 */
@Composable
private fun PiezaCosmetica(item: ZaziCosmeticItem, escalaVista: Float) {
    val contexto = LocalContext.current
    val escalaFuente = LocalDensity.current.fontScale.coerceAtLeast(0.5f)
    val imagen = remember(item.id) { SpritesCosmeticos.obtener(contexto, item.id) }
    val lado = ZaziContentFactory.LADO_MOTA_DP.dp * escalaVista
    val ladoFuente = (lado.value * 0.90f / escalaFuente).sp
    val desplazamientoX = item.offsetX * escalaVista
    val desplazamientoY = item.offsetY * escalaVista

    Box(
        modifier = Modifier
            .size(lado)
            .offset(x = desplazamientoX, y = desplazamientoY)
            .scale(item.scale),
        contentAlignment = Alignment.Center
    ) {
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None
            )
        } else {
            Text(
                text = item.emoji,
                fontSize = ladoFuente,
                lineHeight = ladoFuente,
                textAlign = TextAlign.Center,
                softWrap = false
            )
        }
    }
}

/**
 * Busca un PNG propio del cosmético. El catálogo actual solo trae emoji, así
 * que la ausencia del recurso es lo normal y no se registra en el log; el
 * resultado se memoriza para no consultar el identificador en cada cuadro.
 */
private object SpritesCosmeticos {

    private val cache = mutableMapOf<String, ImageBitmap?>()
    private val opciones = BitmapFactory.Options().apply { inScaled = false }

    fun obtener(contexto: Context, id: String): ImageBitmap? {
        if (cache.containsKey(id)) return cache[id]
        val recursos = contexto.resources
        val recurso = recursos.getIdentifier(
            "zazi_cosmetico_$id",
            "drawable",
            contexto.packageName
        )
        val imagen = if (recurso == 0) {
            null
        } else {
            runCatching {
                BitmapFactory.decodeResource(recursos, recurso, opciones)?.asImageBitmap()
            }.getOrNull()
        }
        cache[id] = imagen
        return imagen
    }
}
