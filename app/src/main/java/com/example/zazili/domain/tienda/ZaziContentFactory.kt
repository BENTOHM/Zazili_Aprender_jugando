package com.example.zazili.domain.tienda

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Capa de dibujo de un cosmético de Zazi. [orden] ES el Z-index del renderer.
 */
enum class CapaCosmetica(val orden: Int, val etiqueta: String) {
    ROPA(0, "Ropa"),
    ACCESORIO(10, "Accesorio"),
    SOMBRERO(20, "Sombrero")
}

/**
 * Una prenda de Zazi anclada a su cuerpo.
 *
 * Los offsets son Dp fijos medidos en la caja de diseño de
 * [ZaziContentFactory.LADO_CAJA_DP] (200 dp), con el sprite base de 168 dp
 * centrado dentro. Dp fijos, no porcentajes: el arte es rígido y el sombrero
 * no debe "flotar" si la tarjeta crece o se encoge.
 *
 * - [offsetX] positivo mueve a la derecha del centro.
 * - [offsetY] negativo sube; positivo baja.
 */
@Immutable
data class ZaziCosmeticItem(
    val id: String,
    val nombre: String,
    val emoji: String,
    val ranura: RanuraRopa,
    val capa: CapaCosmetica,
    val offsetX: Dp,
    val offsetY: Dp,
    val scale: Float = 1f
)

/**
 * Fábrica única del armario de Zazi.
 *
 * No duplica el inventario de Tienditas 3B: cada [RopaTienda] se convierte en
 * un [ZaziCosmeticItem] con coordenadas REALES (nunca 0.dp por defecto). Las
 * piezas nuevas sin anclaje propio caen en su anclaje de ranura, así que
 * agregar ropa a la tienda jamás deja un accesorio flotando.
 *
 * Sistema de coordenadas (caja de 200 × 200 dp con el sprite de 168 dp
 * centrado, centro = 100 dp):
 *
 *  - Cabeza (gorros, coronas):   y ≈ -50 dp … -60 dp (arriba del centro).
 *  - Ojos / pico (lentes, bigote): y ≈ -12 dp … -26 dp.
 *  - Pancita (camisas, tutús):   y ≈ +22 dp … +28 dp.
 *
 * Escalas pensadas para una mota de 96 dp: los sombreros anchos llegan a
 * 0.68 y los detalles pequeños bajan a 0.26.
 */
object ZaziContentFactory {

    /** Lado de la caja raíz del renderer. Deja 16 dp de aire por lado. */
    const val LADO_CAJA_DP: Float = 200f

    /** Tamaño del sprite base (colibrí) dentro de la caja de diseño. */
    const val TAMANO_BASE_DP: Float = 168f

    /** Lado de la mota de cada cosmético en el espacio de diseño. */
    const val LADO_MOTA_DP: Float = 96f

    private data class Anclaje(val x: Dp, val y: Dp, val escala: Float)

    /** Anclaje de respaldo por ranura: ninguna prenda puede quedar suelta. */
    private val anclajePorRanura: Map<RanuraRopa, Anclaje> = mapOf(
        RanuraRopa.CABEZA to Anclaje(0.dp, (-50).dp, 0.56f),
        RanuraRopa.CARA to Anclaje(0.dp, (-24).dp, 0.40f),
        RanuraRopa.CUERPO to Anclaje(0.dp, 24.dp, 0.64f)
    )

    /**
     * Coordenadas por pieza. Todas las prendas del catálogo de Tienditas 3B
     * tienen entrada propia; los valores se midieron sobre la caja de 200 dp.
     */
    private val anclajesPropios: Map<String, Anclaje> = mapOf(
        // ---------------------------- Cabeza ----------------------------
        // El ala del sombrero descansa sobre la coronilla sin tapar los ojos.
        "sombrero_vueltiao" to Anclaje(0.dp, (-50).dp, 0.62f),
        "chullo_peru" to Anclaje(0.dp, (-52).dp, 0.56f),
        "gorro_ajolote" to Anclaje(0.dp, (-50).dp, 0.58f),
        "casco_daft_punk" to Anclaje(0.dp, (-50).dp, 0.68f),
        "corona_cempasuchil" to Anclaje(0.dp, (-54).dp, 0.50f),
        "gorro_dormir_pompom" to Anclaje(0.dp, (-56).dp, 0.54f),
        "sombrero_mago_8bits" to Anclaje(0.dp, (-54).dp, 0.64f),
        "corona_estrellas" to Anclaje(0.dp, (-50).dp, 0.48f),
        "diadema_margaritas" to Anclaje(0.dp, (-42).dp, 0.44f),
        "gorro_santa" to Anclaje(0.dp, (-54).dp, 0.56f),
        "beret_artista" to Anclaje((-10).dp, (-46).dp, 0.48f),
        "cucurucho_helado" to Anclaje(0.dp, (-58).dp, 0.50f),
        "antenas_marciano" to Anclaje(0.dp, (-60).dp, 0.44f),
        "sombrero_charro" to Anclaje(0.dp, (-50).dp, 0.66f),
        "cachitos_diablito" to Anclaje(0.dp, (-38).dp, 0.30f),

        // ----------------------------- Cara -----------------------------
        // Anteojos centrados en la línea de los ojos; boquita y mejillas más abajo.
        "lentes_corazon" to Anclaje(0.dp, (-24).dp, 0.40f),
        "gafas_cyberpunk" to Anclaje(0.dp, (-24).dp, 0.46f),
        "antifaz_el_santo" to Anclaje(0.dp, (-24).dp, 0.44f),
        "pecas_estrellitas" to Anclaje(0.dp, (-16).dp, 0.34f),
        "lentes_nerd_cinta" to Anclaje(0.dp, (-24).dp, 0.40f),
        "monoculo_elegante" to Anclaje(14.dp, (-24).dp, 0.28f),
        "lentes_sol_playa" to Anclaje(0.dp, (-25).dp, 0.42f),
        "gafas_3d" to Anclaje(0.dp, (-24).dp, 0.42f),
        "bigote_falso" to Anclaje(0.dp, (-13).dp, 0.30f),
        "labios_brillantes" to Anclaje(0.dp, (-13).dp, 0.28f),
        "lentes_abu" to Anclaje(0.dp, (-23).dp, 0.38f),
        "parche_pirata" to Anclaje((-12).dp, (-24).dp, 0.26f),
        "pintura_mejillas" to Anclaje(0.dp, (-12).dp, 0.42f),
        "pestanas_neon" to Anclaje(0.dp, (-26).dp, 0.30f),
        "gafas_mariposa" to Anclaje(0.dp, (-24).dp, 0.46f),

        // --------------------------- Cuerpecito --------------------------
        // La ropa se centra en la pancita, nunca al pie del recuadro.
        "guayabera_blanca" to Anclaje(0.dp, 24.dp, 0.64f),
        "poncho_andino" to Anclaje(0.dp, 22.dp, 0.72f),
        "chamarra_cuero" to Anclaje(0.dp, 24.dp, 0.64f),
        "tutu_ballet" to Anclaje(0.dp, 28.dp, 0.66f),
        "traje_astronauta" to Anclaje(0.dp, 22.dp, 0.76f),
        "playera_seleccion" to Anclaje(0.dp, 24.dp, 0.62f),
        "sueter_abuelita" to Anclaje(0.dp, 22.dp, 0.66f),
        "vestido_flores" to Anclaje(0.dp, 26.dp, 0.68f),
        "capa_superheroe" to Anclaje(0.dp, 24.dp, 0.74f),
        "impermeable_pato" to Anclaje(0.dp, 22.dp, 0.68f),
        "pijama_estrellitas" to Anclaje(0.dp, 24.dp, 0.64f),
        "delantal_cocinerito" to Anclaje(0.dp, 26.dp, 0.60f),
        "chaleco_chilango" to Anclaje(0.dp, 24.dp, 0.64f),
        "kimonito_sakura" to Anclaje(0.dp, 24.dp, 0.68f),
        "overall_jardinero" to Anclaje(0.dp, 24.dp, 0.64f)
    )

    private fun capaDe(ranura: RanuraRopa): CapaCosmetica = when (ranura) {
        RanuraRopa.CUERPO -> CapaCosmetica.ROPA
        RanuraRopa.CARA -> CapaCosmetica.ACCESORIO
        RanuraRopa.CABEZA -> CapaCosmetica.SOMBRERO
    }

    /** Convierte una prenda de la tienda en cosmético con su anclaje resuelto. */
    fun crear(prenda: RopaTienda): ZaziCosmeticItem {
        val anclaje = anclajesPropios[prenda.id] ?: anclajePorRanura.getValue(prenda.ranura)
        return ZaziCosmeticItem(
            id = prenda.id,
            nombre = prenda.nombre,
            emoji = prenda.emoji,
            ranura = prenda.ranura,
            capa = capaDe(prenda.ranura),
            offsetX = anclaje.x,
            offsetY = anclaje.y,
            scale = anclaje.escala
        )
    }

    /** Todo el armario de Zazi ya anclado. */
    val todos: List<ZaziCosmeticItem> by lazy {
        CatalogoTienditas3B.ropa.map(::crear)
    }

    private val indice: Map<String, ZaziCosmeticItem> by lazy {
        todos.associateBy { it.id }
    }

    fun porId(id: String?): ZaziCosmeticItem? = id?.let(indice::get)

    /**
     * Prendas puestas listas para dibujar, en orden estricto de capa:
     * cuerpo → accesorios → sombreros. Un id huérfano se ignora.
     */
    fun capasDe(ropa: List<RopaTienda>): List<ZaziCosmeticItem> =
        ropa.mapNotNull { porId(it.id) }.enOrdenDeDibujo()

    /** Variante para mapas de equipamiento (`id` por ranura). */
    fun capasDeIds(ids: Collection<String>): List<ZaziCosmeticItem> =
        ids.mapNotNull(::porId).enOrdenDeDibujo()

    private fun List<ZaziCosmeticItem>.enOrdenDeDibujo(): List<ZaziCosmeticItem> =
        sortedWith(compareBy({ it.capa.orden }, { it.ranura.ordinal }))
}
