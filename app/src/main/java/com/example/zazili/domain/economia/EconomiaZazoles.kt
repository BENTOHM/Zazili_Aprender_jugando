package com.example.zazili.domain.economia

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tipo de artículo de La Tiendita. */
enum class TipoArticulo { ROPA, DECORACION, MASCOTA }

/** Artículo vendible en La Tiendita (la UI del catálogo llega en su ronda). */
data class ArticuloTiendita(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: Int,
    val tipo: TipoArticulo
)

/** Identificadores estables de los artículos (persistidos en prefs). */
object IdsArticulo {
    const val MASCARA_SANTO = "mascara_santo"
    const val SOMBRERO_MARIACHI = "sombrero_mariachi"
    const val PIYAMA_AJOLOTE = "piyama_ajolote"
    const val TRAJINERA = "trajinera_xochimilco"
    const val CUARTO_GAMER = "cuarto_gamer_neon"
    const val PAPALOTE = "papalote_coyoacan"
    const val CHIHUAHUA = "chihuahua_chispita"
    const val CACOMIXTLE = "cacomixtle_don_bigotes"
}

/** Catálogo completo de La Tiendita. */
object CatalogoTiendita {
    val articulos: List<ArticuloTiendita> = listOf(
        // Ropa para Zazi.
        ArticuloTiendita(
            id = IdsArticulo.MASCARA_SANTO,
            nombre = "Máscara del Santo",
            descripcion = "Lucha libre contra las multiplicaciones.",
            precio = 80,
            tipo = TipoArticulo.ROPA
        ),
        ArticuloTiendita(
            id = IdsArticulo.SOMBRERO_MARIACHI,
            nombre = "Sombrero de Mariachi",
            descripcion = "Cielito lindo, pero para las mentes brillantes.",
            precio = 60,
            tipo = TipoArticulo.ROPA
        ),
        ArticuloTiendita(
            id = IdsArticulo.PIYAMA_AJOLOTE,
            nombre = "Piyama de Ajolote",
            descripcion = "Para soñar en chinampa, estilo Xochimilco.",
            precio = 45,
            tipo = TipoArticulo.ROPA
        ),
        // Decoración de la casita.
        ArticuloTiendita(
            id = IdsArticulo.TRAJINERA,
            nombre = "Trajinera de Xochimilco",
            descripcion = "Con flores incluidas y todo el canal romántico.",
            precio = 120,
            tipo = TipoArticulo.DECORACION
        ),
        ArticuloTiendita(
            id = IdsArticulo.CUARTO_GAMER,
            nombre = "Cuarto Gamer Neón",
            descripcion = "Paredes neón para las rachas épicas.",
            precio = 150,
            tipo = TipoArticulo.DECORACION
        ),
        ArticuloTiendita(
            id = IdsArticulo.PAPALOTE,
            nombre = "Papalote de Coyoacán",
            descripcion = "Vuela más alto que tus calificaciones futuras.",
            precio = 40,
            tipo = TipoArticulo.DECORACION
        ),
        // Mascotas para el avatar del niño.
        ArticuloTiendita(
            id = IdsArticulo.CHIHUAHUA,
            nombre = "Chispita la Chihuahua",
            descripcion = "Pequeña, ruidosa y leal como ninguna.",
            precio = 70,
            tipo = TipoArticulo.MASCOTA
        ),
        ArticuloTiendita(
            id = IdsArticulo.CACOMIXTLE,
            nombre = "Don Bigotes el Cacomixtle",
            descripcion = "Vigila la casa de noche. Se echaba una siesta.",
            precio = 90,
            tipo = TipoArticulo.MASCOTA
        )
    )

    fun porId(id: String): ArticuloTiendita? = articulos.firstOrNull { it.id == id }
}

/**
 * Economía dual: XP (mérito/nivel, vive en GameProgressManager) + Zazoles
 * (semillas brillantes de moneda corriente). Bolsa inicial de bienvenida:
 * 100 zazoles para que alimentar a Zazi funcione desde el primer día.
 */
class EconomiaZazoles(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure {
        Log.w(TAG, "Economía no disponible; se usa una bolsa nueva", it)
    }.getOrNull()

    private val _zazoles = MutableStateFlow(cargarZazoles())
    val zazoles: StateFlow<Int> = _zazoles.asStateFlow()

    private val _comprados = MutableStateFlow(cargarIds(KEY_COMPRADOS))
    val comprados: StateFlow<Set<String>> = _comprados.asStateFlow()

    private val _equipados = MutableStateFlow(cargarIds(KEY_EQUIPADOS))
    val equipados: StateFlow<Set<String>> = _equipados.asStateFlow()

    @Synchronized
    fun ganar(cantidad: Int) {
        if (cantidad <= 0) return
        _zazoles.value += cantidad
        persistir()
    }

    /** Gasta zazoles; devuelve false si no alcanza (nunca queda en negativo). */
    @Synchronized
    fun gastar(cantidad: Int): Boolean {
        if (cantidad <= 0) return true
        if (_zazoles.value < cantidad) return false
        _zazoles.value -= cantidad
        persistir()
        return true
    }

    /** Compra un artículo y lo equipa automáticamente. */
    @Synchronized
    fun comprar(articulo: ArticuloTiendita): Boolean {
        if (_comprados.value.contains(articulo.id)) return false
        if (!gastar(articulo.precio)) return false
        _comprados.value = _comprados.value + articulo.id
        _equipados.value = _equipados.value + articulo.id
        persistir()
        return true
    }

    /** Equipa/desequipa un artículo ya comprado. */
    @Synchronized
    fun alternarEquipado(id: String): Boolean {
        if (!_comprados.value.contains(id)) return false
        _equipados.value = if (_equipados.value.contains(id)) {
            _equipados.value - id
        } else {
            _equipados.value + id
        }
        persistir()
        return true
    }

    fun equipado(id: String): Boolean = _equipados.value.contains(id)

    private fun cargarZazoles(): Int =
        runCatching { prefs?.getInt(KEY_ZAZOLES, BOLSA_BIENVENIDA) ?: BOLSA_BIENVENIDA }
            .getOrDefault(BOLSA_BIENVENIDA)

    private fun cargarIds(clave: String): Set<String> =
        runCatching { prefs?.getStringSet(clave, emptySet()) ?: emptySet() }
            .getOrDefault(emptySet())

    private fun persistir() {
        runCatching {
            prefs?.edit()
                ?.putInt(KEY_ZAZOLES, _zazoles.value)
                ?.putStringSet(KEY_COMPRADOS, _comprados.value)
                ?.putStringSet(KEY_EQUIPADOS, _equipados.value)
                ?.apply()
        }.onFailure {
            Log.w(TAG, "No se pudo guardar la economía", it)
        }
    }

    companion object {
        private const val TAG = "EconomiaZazoles"
        private const val PREFS_NAME = "zazili_economia"
        private const val KEY_ZAZOLES = "zazoles"
        private const val KEY_COMPRADOS = "comprados"
        private const val KEY_EQUIPADOS = "equipados"
        private const val BOLSA_BIENVENIDA = 100
    }
}
