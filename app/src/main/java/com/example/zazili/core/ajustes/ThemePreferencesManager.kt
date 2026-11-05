package com.example.zazili.core.ajustes

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private val Context.dataStoreTema: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_tema_preferido"
)

/**
 * Apariencia elegida por el niño. Son tres estados y el pequeño manda:
 *
 * - [AUTO]: sigue la hora real de Zazi (amanecer/día claros, tarde/noche
 *   oscuros).
 * - [CLARO]: día a toda hora, sin importar el reloj.
 * - [OSCURO]: noche a toda hora, sin importar el reloj.
 */
enum class TemaPreferido(
    val clave: String,
    val etiqueta: String,
    val detalle: String,
    val emoji: String
) {
    AUTO("THEME_AUTO", "Automático", "Sigue la hora de Zazi", "🌗"),
    CLARO("THEME_FORCE_LIGHT", "Día", "Claro a toda hora", "☀️"),
    OSCURO("THEME_FORCE_DARK", "Noche", "Oscuro a toda hora", "🌙");

    /** Siguiente parada del botón de apariencia: Automático → Día → Noche. */
    fun siguiente(): TemaPreferido = when (this) {
        AUTO -> CLARO
        CLARO -> OSCURO
        OSCURO -> AUTO
    }

    companion object {
        fun porClave(clave: String?): TemaPreferido =
            entries.firstOrNull { it.clave == clave } ?: AUTO
    }
}

/**
 * Guarda la preferencia de tema en DataStore y la publica como [StateFlow]
 * para que el tema raíz reaccione al instante, sin releer disco.
 */
class ThemePreferencesManager private constructor(context: Context) {

    private val almacen = context.applicationContext.dataStoreTema
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tema = MutableStateFlow(TemaPreferido.AUTO)
    val tema: StateFlow<TemaPreferido> = _tema.asStateFlow()

    init {
        alcance.launch {
            almacen.data
                .catch { error ->
                    if (error is IOException) {
                        Log.w(TAG, "No se pudo leer el tema guardado", error)
                        emit(emptyPreferences())
                    } else {
                        throw error
                    }
                }
                .collect { preferencias ->
                    _tema.value = TemaPreferido.porClave(preferencias[CLAVE_TEMA])
                }
        }
    }

    fun establecer(tema: TemaPreferido) {
        _tema.value = tema
        alcance.launch {
            runCatching {
                almacen.edit { preferencias ->
                    preferencias[CLAVE_TEMA] = tema.clave
                }
            }.onFailure { Log.w(TAG, "No se pudo guardar el tema", it) }
        }
    }

    /** Botón del niño: recorre Automático → Día → Noche. */
    fun alternar(): TemaPreferido {
        val nuevo = tema.value.siguiente()
        establecer(nuevo)
        return nuevo
    }

    companion object {
        private const val TAG = "ZaziliTema"
        private val CLAVE_TEMA = stringPreferencesKey("tema_preferido")

        @Volatile
        private var instancia: ThemePreferencesManager? = null

        fun obtener(context: Context): ThemePreferencesManager =
            instancia ?: synchronized(this) {
                instancia ?: ThemePreferencesManager(context.applicationContext)
                    .also { instancia = it }
            }
    }
}
