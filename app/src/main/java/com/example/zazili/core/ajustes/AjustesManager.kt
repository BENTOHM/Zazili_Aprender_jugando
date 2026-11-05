package com.example.zazili.core.ajustes

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Preferencia de apariencia que ve el niño en Ajustes.
 *
 * [SISTEMA] es en realidad "Automático": sigue la hora de Zazi
 * (amanecer/día claros, tarde/noche oscuros). [CLARO] y [OSCURO] fijan el
 * tema a toda hora.
 */
@Immutable
enum class ModoTema(
    val id: String,
    val etiqueta: String,
    val detalle: String,
    val emoji: String
) {
    SISTEMA("THEME_AUTO", "Automático", "Sigue la hora de Zazi", "🌗"),
    CLARO("THEME_FORCE_LIGHT", "Día", "Claro a toda hora", "☀️"),
    OSCURO("THEME_FORCE_DARK", "Noche", "Oscuro a toda hora", "🌙");

    companion object {
        fun porId(id: String?): ModoTema =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SISTEMA
    }
}

/**
 * Fachada de ajustes ligeros para la pantalla del niño. La persistencia real
 * vive en [ThemePreferencesManager] (DataStore); aquí solo se traduce entre
 * ambas representaciones para no tocar la UI existente.
 */
class AjustesManager private constructor(context: Context) {

    private val preferencias = ThemePreferencesManager.obtener(context)
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _modoTema: StateFlow<ModoTema> = preferencias.tema
        .map { tema -> tema.aModoTema() }
        .stateIn(
            scope = alcance,
            started = SharingStarted.Eagerly,
            initialValue = preferencias.tema.value.aModoTema()
        )

    val modoTema: StateFlow<ModoTema> get() = _modoTema

    fun establecerModo(modo: ModoTema) {
        preferencias.establecer(modo.aTemaPreferido())
    }

    /**
     * Botón de apariencia del tablero: recorre Automático → Día → Noche. El
     * niño no necesita permiso de nadie para cambiar cómo se ve Zazili.
     */
    fun ciclarTema() {
        preferencias.alternar()
    }

    companion object {
        @Volatile
        private var instancia: AjustesManager? = null

        fun obtener(context: Context): AjustesManager =
            instancia ?: synchronized(this) {
                instancia ?: AjustesManager(context.applicationContext).also { instancia = it }
            }
    }
}

internal fun TemaPreferido.aModoTema(): ModoTema = when (this) {
    TemaPreferido.AUTO -> ModoTema.SISTEMA
    TemaPreferido.CLARO -> ModoTema.CLARO
    TemaPreferido.OSCURO -> ModoTema.OSCURO
}

internal fun ModoTema.aTemaPreferido(): TemaPreferido = when (this) {
    ModoTema.SISTEMA -> TemaPreferido.AUTO
    ModoTema.CLARO -> TemaPreferido.CLARO
    ModoTema.OSCURO -> TemaPreferido.OSCURO
}
