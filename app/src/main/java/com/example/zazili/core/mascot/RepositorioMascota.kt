package com.example.zazili.core.mascot

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistencia y estado reactivo de la mascota. Recalcula el decaimiento
 * offline cada vez que el proceso arranca (o al entrar a la casita), igual
 * que un Tamagotchi real: el tiempo sin abrir la app también pasa para Zazi.
 */
class RepositorioMascota(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure {
        Log.w(TAG, "Estado de la mascota no disponible; se usa uno nuevo", it)
    }.getOrNull()

    private val _estado = MutableStateFlow(cargar())
    val estado: StateFlow<EstadoVital> = _estado.asStateFlow()

    init {
        sincronizarDecaimiento()
    }

    /** Recalcula las métricas contra el reloj actual y persiste el resultado. */
    @Synchronized
    fun sincronizarDecaimiento() {
        val ahora = System.currentTimeMillis()
        val recalculado = MotorMascota.aplicarDecaimiento(_estado.value, ahora)
        _estado.value = recalculado
        persistir(recalculado)
    }

    @Synchronized
    fun alimentar(): EstadoVital = interactuar {
        MotorMascota.alimentar(it).copy(ultimaComida = System.currentTimeMillis())
    }

    @Synchronized
    fun jugar(): EstadoVital = interactuar { MotorMascota.jugar(it) }

    @Synchronized
    fun hacerMimo(): EstadoVital = interactuar { MotorMascota.hacerMimo(it) }

    @Synchronized
    fun despertar(): EstadoVital = interactuar { MotorMascota.despertar(it) }

    /** Lo llaman las lecciones al completarse: energía + comida. */
    @Synchronized
    fun registrarLeccionCompletada(): EstadoVital =
        interactuar { MotorMascota.registrarLeccion(it) }

    /** Lo llaman los minijuegos al completarse: felicidad. */
    @Synchronized
    fun registrarMinijuegoCompletado(): EstadoVital =
        interactuar { MotorMascota.registrarMinijuego(it) }

    private fun interactuar(transform: (EstadoVital) -> EstadoVital): EstadoVital {
        val ahora = System.currentTimeMillis()
        val base = MotorMascota.aplicarDecaimiento(_estado.value, ahora)
        // Cualquier interacción limpia las telarañas: cuidar a Zazi ordena la casita.
        val resultado = transform(base).copy(
            ultimaInteraccion = ahora,
            nivelTelaranas = 0
        )
        _estado.value = resultado
        persistir(resultado)
        return resultado
    }

    private fun cargar(): EstadoVital {
        val ahora = System.currentTimeMillis()
        val p = prefs ?: return EstadoVital(ultimaInteraccion = ahora, ultimaComida = ahora)
        return runCatching {
            EstadoVital(
                pancita = p.getFloat(KEY_PANCITA, 0.8f),
                felicidad = p.getFloat(KEY_FELICIDAD, 0.85f),
                energia = p.getFloat(KEY_ENERGIA, 0.9f),
                dormida = p.getBoolean(KEY_DORMIDA, false),
                nivelTelaranas = p.getInt(KEY_TELARANAS, 0),
                ultimaInteraccion = p.getLong(KEY_ULTIMA_INTERACCION, ahora),
                ultimaComida = p.getLong(KEY_ULTIMA_COMIDA, ahora)
            )
        }.getOrDefault(EstadoVital(ultimaInteraccion = ahora, ultimaComida = ahora))
    }

    private fun persistir(estado: EstadoVital) {
        runCatching {
            prefs?.edit()
                ?.putFloat(KEY_PANCITA, estado.pancita)
                ?.putFloat(KEY_FELICIDAD, estado.felicidad)
                ?.putFloat(KEY_ENERGIA, estado.energia)
                ?.putBoolean(KEY_DORMIDA, estado.dormida)
                ?.putInt(KEY_TELARANAS, estado.nivelTelaranas)
                ?.putLong(KEY_ULTIMA_INTERACCION, estado.ultimaInteraccion)
                ?.putLong(KEY_ULTIMA_COMIDA, estado.ultimaComida)
                ?.apply()
        }.onFailure {
            Log.w(TAG, "No se pudo guardar el estado de la mascota", it)
        }
    }

    companion object {
        private const val TAG = "RepositorioMascota"
        private const val PREFS_NAME = "zazili_mascota"
        private const val KEY_PANCITA = "pancita"
        private const val KEY_FELICIDAD = "felicidad"
        private const val KEY_ENERGIA = "energia"
        private const val KEY_DORMIDA = "dormida"
        private const val KEY_TELARANAS = "telaranas"
        private const val KEY_ULTIMA_INTERACCION = "ultima_interaccion"
        private const val KEY_ULTIMA_COMIDA = "ultima_comida"
    }
}
