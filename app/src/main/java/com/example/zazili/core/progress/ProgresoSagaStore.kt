package com.example.zazili.core.progress

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.zazili.domain.saga.EstadoSaga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistencia del avance de la Saga. Guarda qué nodos ya tienen estrella y
 * cuántas estrellas se ganaron (3 si acertó al primer intento, 2 al segundo,
 * 1 en adelante). Lectura defensiva: una preferencia rota no tumba la app.
 */
class ProgresoSagaStore(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure {
        Log.w(TAG, "Progreso de la saga no disponible; se usará uno nuevo", it)
    }.getOrNull()

    private val _estado = MutableStateFlow(cargar())
    val estado: StateFlow<EstadoSaga> = _estado.asStateFlow()

    /**
     * Marca un nodo como completado. Repasar nunca otorga premios de nuevo,
     * pero sí puede SUBIR las estrellas: el esfuerzo extra siempre cuenta y
     * una calificación jamás baja.
     *
     * @return true si era la primera vez que se completaba.
     */
    @Synchronized
    fun completar(nodoId: String, estrellas: Int = 3): Boolean {
        if (nodoId.isBlank()) return false
        val actual = _estado.value
        val primeraVez = !actual.completado(nodoId)
        val previas = actual.estrellas[nodoId] ?: 0
        val nuevas = maxOf(previas, estrellas.coerceIn(1, 3))
        if (!primeraVez && nuevas == previas) return false
        _estado.value = actual.copy(
            completados = actual.completados + nodoId,
            estrellas = actual.estrellas + (nodoId to nuevas)
        )
        persistir(_estado.value)
        return primeraVez
    }

    /**
     * Marca varios nodos en UNA sola escritura. La usa el arranque adaptativo
     * de Matemáticas: completar 40 nodos nodo por nodo provocaría 40 emisiones
     * y 40 escrituras durante la primera apertura del mapa.
     *
     * @return cuántos nodos se completaron por primera vez.
     */
    @Synchronized
    fun completarVarios(nodoIds: Collection<String>, estrellas: Int = 3): Int {
        val ids = nodoIds
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        if (ids.isEmpty()) return 0

        val actual = _estado.value
        val nuevos = ids.filterNot { actual.completado(it) }
        if (nuevos.isEmpty()) return 0

        val valor = estrellas.coerceIn(1, 3)
        val estrellasActualizadas = actual.estrellas.toMutableMap()
        nuevos.forEach { id ->
            estrellasActualizadas[id] = maxOf(estrellasActualizadas[id] ?: 0, valor)
        }
        _estado.value = actual.copy(
            completados = actual.completados + nuevos,
            estrellas = estrellasActualizadas
        )
        persistir(_estado.value)
        return nuevos.size
    }

    private fun cargar(): EstadoSaga {
        val p = prefs ?: return EstadoSaga()
        return runCatching {
            val completados = p.getStringSet(KEY_COMPLETADOS, emptySet())
                ?.toSet()
                .orEmpty()
            val estrellas = completados.associateWith { id ->
                p.getInt("$PREFIJO_ESTRELLAS$id", 3).coerceIn(1, 3)
            }
            EstadoSaga(completados = completados, estrellas = estrellas)
        }.getOrDefault(EstadoSaga())
    }

    private fun persistir(estado: EstadoSaga) {
        runCatching {
            val editor = prefs?.edit() ?: return
            editor.putStringSet(KEY_COMPLETADOS, estado.completados)
            estado.estrellas.forEach { (id, valor) ->
                editor.putInt("$PREFIJO_ESTRELLAS$id", valor)
            }
            editor.apply()
        }.onFailure {
            Log.w(TAG, "No se pudo guardar el avance de la saga", it)
        }
    }

    companion object {
        private const val TAG = "ProgresoSaga"
        private const val PREFS_NAME = "zazili_saga"
        private const val KEY_COMPLETADOS = "nodos_completados"
        private const val PREFIJO_ESTRELLAS = "estrellas_"
    }
}
