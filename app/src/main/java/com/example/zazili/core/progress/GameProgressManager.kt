package com.example.zazili.core.progress

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Progreso de mérito del jugador (XP y nivel). +10 XP por minijuego superado,
 * 50 XP por nivel. Vive en [com.example.zazili.di.AppContainer] y persiste en
 * SharedPreferences para sobrevivir reinicios de la app.
 */
data class GameProgress(
    val xp: Int = 0,
    val level: Int = 1,
    val xpIntoLevel: Int = 0,
    val xpForNextLevel: Int = GameProgressManager.XP_PER_LEVEL
) {
    val levelProgress: Float
        get() = (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

class GameProgressManager(context: Context) {

    // Lectura defensiva: una prefs corrupta o con tipos cambiados jamás
    // debe impedir el arranque de la app.
    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure {
        Log.w(TAG, "Progreso no disponible; se usará un progreso nuevo", it)
    }.getOrNull()

    private val _progress = MutableStateFlow(load())
    val progress: StateFlow<GameProgress> = _progress.asStateFlow()

    /**
     * Contador de subidas de nivel pendientes de celebrar. La UI escucha este
     * flujo, muestra el diálogo "¡Subiste de Nivel!" y después llama a
     * [consumeLevelUp].
     */
    private val _levelUpEvents = MutableStateFlow(0)
    val levelUpEvents: StateFlow<Int> = _levelUpEvents.asStateFlow()

    /** Suma experiencia; otorga un evento de celebración si sube de nivel. */
    @Synchronized
    fun awardXp(amount: Int = XP_PER_MINIGAME) {
        if (amount <= 0) return
        val current = _progress.value
        val totalXp = current.xp + amount
        val newLevel = 1 + totalXp / XP_PER_LEVEL
        _progress.value = derive(totalXp)
        runCatching { prefs?.edit()?.putInt(KEY_TOTAL_XP, totalXp)?.apply() }
        if (newLevel > current.level) {
            _levelUpEvents.value = _levelUpEvents.value + 1
        }
    }

    fun consumeLevelUp() {
        _levelUpEvents.value = 0
    }

    private fun load(): GameProgress =
        derive(
            runCatching { prefs?.getInt(KEY_TOTAL_XP, 0) ?: 0 }
                .getOrDefault(0)
        )

    private fun derive(totalXp: Int): GameProgress {
        val safeXp = totalXp.coerceAtLeast(0)
        return GameProgress(
            xp = safeXp,
            level = 1 + safeXp / XP_PER_LEVEL,
            xpIntoLevel = safeXp % XP_PER_LEVEL
        )
    }

    companion object {
        const val XP_PER_MINIGAME = 10
        const val XP_PER_LEVEL = 50

        private const val TAG = "ZaziliProgress"
        private const val PREFS_NAME = "zazili_progress"
        private const val KEY_TOTAL_XP = "total_xp"
    }
}
