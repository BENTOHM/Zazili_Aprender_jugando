package com.example.zazili.core.progress

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStoreProgresoUsuario: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_progreso_usuario"
)

/**
 * Desbloqueo de lecciones por módulo.
 *
 * [desbloqueadas] guarda, para cada `moduleId`, el número de lección más alto
 * que el niño ya puede abrir. Todo módulo arranca con la lección 1 disponible.
 */
@Immutable
data class EstadoProgresoUsuario(
    val desbloqueadas: Map<Int, Int> = emptyMap(),
    val listo: Boolean = false
) {
    fun maximoDesbloqueado(moduleId: Int): Int =
        (desbloqueadas[moduleId] ?: UserProgressManager.LECCION_INICIAL)
            .coerceAtLeast(UserProgressManager.LECCION_INICIAL)

    fun estaDesbloqueada(moduleId: Int, leccionId: Int): Boolean =
        leccionId in UserProgressManager.LECCION_INICIAL..maximoDesbloqueado(moduleId)
}

/**
 * Rastreador persistente del avance lineal de cada módulo.
 *
 * Cada módulo tiene UNA llave explícita e inmutable en DataStore
 * (`unlocked_lesson_mod_1` … `unlocked_lesson_mod_5`). El módulo 5 usa
 * [MODULE_5_KEY]; ninguna ruta escribe con un nombre calculado, así que un
 * cambio de catálogo jamás puede mover el desbloqueo de un niño.
 *
 * Los módulos se identifican con el mismo número que usan las rutas de
 * navegación (1 Matemáticas, 2 Lengua, 3 Ciencias, 4 Tecnología,
 * 5 Emociones/Arena Cortex).
 */
class UserProgressManager private constructor(context: Context) {

    private val dataStore = context.applicationContext.dataStoreProgresoUsuario
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val estado: StateFlow<EstadoProgresoUsuario> = dataStore.data
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
            initialValue = EstadoProgresoUsuario()
        )

    /**
     * Flujo del máximo desbloqueado de un módulo. El mapa lo colecciona con
     * `collectAsStateWithLifecycle(initialValue = 1)` para encender los nodos
     * en cuanto DataStore confirma el nuevo valor.
     */
    fun getUnlockedLesson(moduleId: Int): Flow<Int> =
        estado.map { it.maximoDesbloqueado(moduleId) }.distinctUntilChanged()

    fun maximoDesbloqueado(moduleId: Int): Int = estado.value.maximoDesbloqueado(moduleId)

    fun estaDesbloqueada(moduleId: Int, leccionId: Int): Boolean =
        estado.value.estaDesbloqueada(moduleId, leccionId)

    /**
     * Sube el máximo desbloqueado del módulo si [nextId] lo supera.
     *
     * @return true si el valor cambió y se guardó.
     */
    suspend fun unlockNextLesson(moduleId: Int, nextId: Int): Boolean =
        desbloquear(moduleId, nextId)

    /**
     * Sube el máximo desbloqueado del módulo si [leccionId] lo supera.
     *
     * @return true si el valor cambió y se guardó.
     */
    suspend fun desbloquear(moduleId: Int, leccionId: Int): Boolean {
        if (moduleId !in MODULOS_CONOCIDOS || leccionId <= 0) return false
        var cambio = false
        try {
            dataStore.edit { prefs ->
                val clave = claveDe(moduleId)
                val actual = (prefs[clave] ?: LECCION_INICIAL).coerceAtLeast(LECCION_INICIAL)
                val nuevo = maxOf(actual, leccionId)
                if (nuevo != actual) {
                    prefs[clave] = nuevo
                    cambio = true
                }
            }
        } catch (error: IOException) {
            Log.w(TAG, "No se pudo desbloquear la lección $leccionId del módulo $moduleId", error)
            return false
        }
        return cambio
    }

    /**
     * Variante sin `suspend` para el cierre de una lección: el avance se
     * guarda en segundo plano sin retrasar la pantalla de victoria.
     */
    fun unlockNextLessonEnSegundoPlano(moduleId: Int, nextId: Int) {
        alcance.launch {
            runCatching { unlockNextLesson(moduleId, nextId) }
                .onFailure {
                    Log.w(TAG, "No se pudo guardar el desbloqueo del módulo $moduleId", it)
                }
        }
    }

    private fun Preferences.aEstado(listo: Boolean): EstadoProgresoUsuario {
        val desbloqueadas = MODULOS_CONOCIDOS.mapNotNull { moduleId ->
            // La llave explícita manda. El nombre anterior se sigue leyendo
            // para que un niño que ya avanzó no pierda su mapa al actualizar.
            val nuevo = this[claveDe(moduleId)]
            val legado = this[CLAVES_LEGADO.getValue(moduleId)]
            val guardado = listOfNotNull(nuevo, legado).maxOrNull() ?: return@mapNotNull null
            moduleId to guardado.coerceAtLeast(LECCION_INICIAL)
        }.toMap()
        return EstadoProgresoUsuario(desbloqueadas = desbloqueadas, listo = listo)
    }

    companion object {
        private const val TAG = "ProgresoUsuario"
        const val LECCION_INICIAL = 1

        private val MODULOS_CONOCIDOS = 1..5

        /** Llave explícita del módulo 5 (Arena Cortex / Emociones). */
        val MODULE_5_KEY: Preferences.Key<Int> = intPreferencesKey("unlocked_lesson_mod_5")

        private val CLAVES: Map<Int, Preferences.Key<Int>> = mapOf(
            1 to intPreferencesKey("unlocked_lesson_mod_1"),
            2 to intPreferencesKey("unlocked_lesson_mod_2"),
            3 to intPreferencesKey("unlocked_lesson_mod_3"),
            4 to intPreferencesKey("unlocked_lesson_mod_4"),
            5 to MODULE_5_KEY
        )

        private val CLAVES_LEGADO: Map<Int, Preferences.Key<Int>> =
            MODULOS_CONOCIDOS.associateWith { moduleId ->
                intPreferencesKey("modulo_${moduleId}_desbloqueada")
            }

        private fun claveDe(moduleId: Int): Preferences.Key<Int> =
            CLAVES.getValue(moduleId)

        @Volatile
        private var instancia: UserProgressManager? = null

        fun obtener(context: Context): UserProgressManager =
            instancia ?: synchronized(this) {
                instancia ?: UserProgressManager(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}
