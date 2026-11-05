package com.example.zazili.domain.economia

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.zazili.domain.avatar.AvatarCategory
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.avatar.AvatarItem
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStoreEconomiaGlobal: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_economia_global"
)

/** Actividades que pagan Zazoles. Una sola fuente de verdad para los montos. */
enum class EventoRecompensa(val zazoles: Int, val etiqueta: String) {
    ACTIVIDAD_COMPLETADA(10, "Actividad completada"),
    LECCION_COMPLETADA(10, "Lección completada"),
    MINIJUEGO_COMPLETADO(10, "Minijuego completado"),
    ARENA_CORTEX(10, "Arena Cortex superada"),
    RESCATE_EN_NIDO(15, "Rescate en el Nido"),
    RACHA_DIARIA(25, "Racha del día intacta"),
    JEFE_DERROTADO(50, "¡Jefe derrotado!")
}

/** Aviso que la UI puede mostrar como recompensa flotante. */
@Immutable
data class RecompensaZazoles(
    val evento: EventoRecompensa,
    val total: Int
)

/** Resultado de intentar desbloquear (comprar) un artículo. */
sealed interface ResultadoCompra {
    data class Exito(val itemId: String, val saldoRestante: Int) : ResultadoCompra
    data class SinFondos(val faltan: Int) : ResultadoCompra
    data object YaDesbloqueado : ResultadoCompra
    data object NoExiste : ResultadoCompra
}

/**
 * Estado observable de la bolsa y el armario. `listo = false` mientras
 * DataStore entrega la primera lectura; la UI ya puede pintar los valores
 * iniciales sin parpadeos.
 */
@Immutable
data class EstadoEconomia(
    val zazoles: Int = ZazolEconomyManager.BOLSA_BIENVENIDA,
    val desbloqueados: Set<String> = emptySet(),
    val equipamiento: Map<AvatarCategory, String> =
        AvatarContentFactory.predeterminados().associate { it.category to it.id },
    val listo: Boolean = false
) {
    fun pieza(categoria: AvatarCategory): AvatarItem? =
        equipamiento[categoria]?.let(AvatarContentFactory::porId)
}

/**
 * Motor global de Zazoles.
 *
 * Es un singleton local respaldado por DataStore. Cualquier actividad de los
 * cuatro módulos o de la Arena Cortex puede llamar a:
 *
 * ```
 * ZazolEconomyManager.obtener(context).notificarActividad(EventoRecompensa.ACTIVIDAD_COMPLETADA)
 * ```
 *
 * Las operaciones `suspend` son atómicas: leen y escriben dentro del mismo
 * `dataStore.edit`, así que dos pantallas no pueden duplicar ni perder saldo.
 * No hay nube, cuentas ni compras reales: todo vive en el dispositivo.
 */
class ZazolEconomyManager private constructor(context: Context) {

    private val dataStore = context.applicationContext.dataStoreEconomiaGlobal
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _recompensas = MutableSharedFlow<RecompensaZazoles>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val recompensas: SharedFlow<RecompensaZazoles> = _recompensas.asSharedFlow()

    val estado: StateFlow<EstadoEconomia> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { prefs -> prefs.aEstado(listo = true) }
        .stateIn(
            scope = alcance,
            started = SharingStarted.Eagerly,
            initialValue = EstadoEconomia()
        )

    fun hasEnough(cantidad: Int): Boolean =
        cantidad <= 0 || estado.value.zazoles >= cantidad

    fun estaDesbloqueado(item: AvatarItem): Boolean =
        AvatarContentFactory.estaDesbloqueado(item, estado.value.desbloqueados)

    fun estaEquipado(categoria: AvatarCategory, itemId: String): Boolean =
        estado.value.equipamiento[categoria] == itemId

    /** Suma Zazoles y devuelve el saldo final. Cantidades negativas se ignoran. */
    suspend fun addZazoles(cantidad: Int): Int {
        val suma = cantidad.coerceIn(0, MAXIMO_POR_OPERACION)
        if (suma == 0) return estado.value.zazoles
        return try {
            val prefs = dataStore.edit { actuales ->
                val saldo = (actuales[CLAVE_ZAZOLES] ?: BOLSA_BIENVENIDA).coerceAtLeast(0)
                actuales[CLAVE_ZAZOLES] = (saldo + suma).coerceAtMost(SALDO_MAXIMO)
            }
            prefs[CLAVE_ZAZOLES] ?: estado.value.zazoles
        } catch (error: IOException) {
            Log.w(TAG, "No se pudieron sumar Zazoles", error)
            estado.value.zazoles
        }
    }

    /** Resta Zazoles; devuelve false si no alcanza. El saldo nunca es negativo. */
    suspend fun deductZazoles(cantidad: Int): Boolean {
        if (cantidad <= 0) return true
        var alcanzo = false
        try {
            dataStore.edit { actuales ->
                val saldo = (actuales[CLAVE_ZAZOLES] ?: BOLSA_BIENVENIDA).coerceAtLeast(0)
                if (saldo >= cantidad) {
                    actuales[CLAVE_ZAZOLES] = saldo - cantidad
                    alcanzo = true
                } else {
                    alcanzo = false
                }
            }
        } catch (error: IOException) {
            Log.w(TAG, "No se pudieron gastar Zazoles", error)
            return false
        }
        return alcanzo
    }

    /**
     * Compra un artículo: cobra [precio] y lo agrega al inventario en una sola
     * transacción. No lo equipa; el armario decide después.
     */
    suspend fun unlockItem(itemId: String, precio: Int): ResultadoCompra {
        if (itemId.isBlank()) return ResultadoCompra.NoExiste
        val cobro = precio.coerceAtLeast(0)
        var resultado: ResultadoCompra = ResultadoCompra.NoExiste
        try {
            dataStore.edit { actuales ->
                val desbloqueados = actuales[CLAVE_DESBLOQUEADOS] ?: emptySet()
                if (itemId in desbloqueados) {
                    resultado = ResultadoCompra.YaDesbloqueado
                    return@edit
                }
                val saldo = (actuales[CLAVE_ZAZOLES] ?: BOLSA_BIENVENIDA).coerceAtLeast(0)
                if (saldo < cobro) {
                    resultado = ResultadoCompra.SinFondos(faltan = cobro - saldo)
                    return@edit
                }
                actuales[CLAVE_ZAZOLES] = saldo - cobro
                actuales[CLAVE_DESBLOQUEADOS] = desbloqueados + itemId
                resultado = ResultadoCompra.Exito(itemId, saldo - cobro)
            }
        } catch (error: IOException) {
            Log.w(TAG, "No se pudo desbloquear $itemId", error)
            return ResultadoCompra.NoExiste
        }
        return resultado
    }

    /** Variante corta: cobra el precio del catálogo del armario. */
    suspend fun unlockItem(itemId: String): ResultadoCompra =
        unlockItem(itemId, AvatarContentFactory.porId(itemId)?.price ?: 0)

    /** Paga la recompensa del evento y avisa a quien esté escuchando. */
    suspend fun registrarActividad(evento: EventoRecompensa): Int {
        val total = addZazoles(evento.zazoles)
        _recompensas.tryEmit(RecompensaZazoles(evento, total))
        return total
    }

    /** Versión sin `suspend` para pantallas que completan una actividad. */
    fun notificarActividad(evento: EventoRecompensa) {
        alcance.launch {
            runCatching { registrarActividad(evento) }
                .onFailure { Log.w(TAG, "No se pudo registrar ${evento.name}", it) }
        }
    }

    suspend fun equipar(categoria: AvatarCategory, itemId: String) {
        dataStore.edit { actuales ->
            val equipo = actuales.equipamientoEfectivo().toMutableMap()
            equipo[categoria] = itemId
            actuales[CLAVE_EQUIPAMIENTO] = equipo.aClaves()
        }
    }

    suspend fun desequipar(categoria: AvatarCategory) {
        dataStore.edit { actuales ->
            val equipo = actuales.equipamientoEfectivo().toMutableMap()
            equipo.remove(categoria)
            actuales[CLAVE_EQUIPAMIENTO] = equipo.aClaves()
        }
    }

    private fun Preferences.aEstado(listo: Boolean): EstadoEconomia {
        val saldo = (this[CLAVE_ZAZOLES] ?: BOLSA_BIENVENIDA).coerceIn(0, SALDO_MAXIMO)
        return EstadoEconomia(
            zazoles = saldo,
            desbloqueados = this[CLAVE_DESBLOQUEADOS] ?: emptySet(),
            equipamiento = equipamientoEfectivo(),
            listo = listo
        )
    }

    /**
     * Si nunca se guardó una selección, el avatar arranca con sus piezas gratis
     * (sin tocado). Una selección guardada vacía significa "todo desequipado"
     * y se respeta.
     *
     * Cualquier id que ya no exista en el catálogo —como la vieja `cabeza_gorra`
     * del sombrero por defecto— se descarta al leer, así una instalación con
     * datos viejos queda con la cabeza descubierta sin migración manual.
     */
    private fun Preferences.equipamientoEfectivo(): Map<AvatarCategory, String> {
        val guardado = this[CLAVE_EQUIPAMIENTO]
            ?: return AvatarContentFactory.predeterminados().associate { it.category to it.id }
        return guardado.mapNotNull { par ->
            val partes = par.split(SEPARADOR, limit = 2)
            if (partes.size != 2) return@mapNotNull null
            val categoria = AvatarCategory.entries.firstOrNull { it.clave == partes[0] }
                ?: return@mapNotNull null
            val item = AvatarContentFactory.porId(partes[1]) ?: return@mapNotNull null
            if (item.category != categoria) return@mapNotNull null
            categoria to item.id
        }.toMap()
    }

    private fun Map<AvatarCategory, String>.aClaves(): Set<String> =
        entries.map { (categoria, itemId) -> "${categoria.clave}$SEPARADOR$itemId" }.toSet()

    companion object {
        private const val TAG = "ZazolEconomyManager"
        const val BOLSA_BIENVENIDA = 120
        private const val SALDO_MAXIMO = 999_999
        private const val MAXIMO_POR_OPERACION = 100_000
        private const val SEPARADOR = '='

        private val CLAVE_ZAZOLES = intPreferencesKey("zazoles")
        private val CLAVE_DESBLOQUEADOS = stringSetPreferencesKey("desbloqueados")
        private val CLAVE_EQUIPAMIENTO = stringSetPreferencesKey("equipamiento")

        @Volatile
        private var instancia: ZazolEconomyManager? = null

        fun obtener(context: Context): ZazolEconomyManager =
            instancia ?: synchronized(this) {
                instancia ?: ZazolEconomyManager(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}
