package com.example.zazili.core.usuario

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.zazili.domain.diagnostico.EstrategiaSemillas
import com.example.zazili.domain.diagnostico.PerfilFrustracion
import com.example.zazili.domain.diagnostico.ResultadoDiagnostico
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStoreUsuario: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_usuario"
)

/** Perfil local del niño. `primeraVez` es la compuerta del onboarding. */
@Immutable
data class PerfilUsuario(
    val primeraVez: Boolean = true,
    val nombre: String = "",
    val edad: Int = 0,
    val avatarId: String = "",
    val nivelMatematicas: Int = 0,
    val nivelLenguaje: Int = 0,
    val apoyoAudio: Boolean = false,
    val perfilFrustracion: PerfilFrustracion? = null,
    val latenciaReintentoMs: Long = -1L,
    val estrategiaSemillas: EstrategiaSemillas? = null
)

/**
 * Fuente de verdad del niño, 100 % local.
 *
 * DataStore reemplaza a SharedPreferences para el perfil y la calibración:
 * lecturas reactivas, escrituras atómicas y tolerancia a archivos corruptos
 * (un `IOException` se traduce en el perfil por defecto, nunca en un crash).
 * No hay cuentas, nube ni identificadores personales fuera del dispositivo.
 */
class UserManager(context: Context) {

    private val dataStore = context.applicationContext.dataStoreUsuario

    /**
     * El `null` inicial de la UI significa "leyendo DataStore"; en cuanto hay
     * la primera emisión siempre llega un [PerfilUsuario] (nunca null).
     */
    val perfil: Flow<PerfilUsuario?> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias ->
            PerfilUsuario(
                primeraVez = preferencias[Claves.PRIMERA_VEZ] ?: true,
                nombre = preferencias[Claves.NOMBRE].orEmpty(),
                edad = preferencias[Claves.EDAD] ?: 0,
                avatarId = preferencias[Claves.AVATAR].orEmpty(),
                nivelMatematicas = preferencias[Claves.NIVEL_MATEMATICAS] ?: 0,
                nivelLenguaje = preferencias[Claves.NIVEL_LENGUAJE] ?: 0,
                apoyoAudio = preferencias[Claves.APOYO_AUDIO] ?: false,
                perfilFrustracion = preferencias[Claves.PERFIL_FRUSTRACION]
                    ?.let { guardado ->
                        runCatching { PerfilFrustracion.valueOf(guardado) }.getOrNull()
                    },
                latenciaReintentoMs = preferencias[Claves.LATENCIA_REINTENTO] ?: -1L,
                estrategiaSemillas = preferencias[Claves.ESTRATEGIA_SEMILLAS]
                    ?.let { guardada ->
                        runCatching { EstrategiaSemillas.valueOf(guardada) }.getOrNull()
                    }
            )
        }

    /** Guarda nombre, edad y avatar sin cerrar todavía la primera compuerta. */
    suspend fun guardarPerfil(nombre: String, edad: Int, avatarId: String) {
        val nombreLimpio = nombre.trim().take(MAX_NOMBRE)
        dataStore.edit { preferencias ->
            preferencias[Claves.NOMBRE] = nombreLimpio
            preferencias[Claves.EDAD] = edad.coerceIn(EDAD_MINIMA, EDAD_MAXIMA)
            preferencias[Claves.AVATAR] = avatarId
        }
    }

    /**
     * Cierra el onboarding en UNA sola transacción: línea base + bandera de
     * primera vez. Si algo falla, la bandera no cambia y el niño no queda a
     * medias entre el diagnóstico y la casita.
     */
    suspend fun completarOnboarding(resultado: ResultadoDiagnostico) {
        dataStore.edit { preferencias ->
            preferencias[Claves.NIVEL_MATEMATICAS] = resultado.nivelMatematicas.coerceIn(1, 5)
            preferencias[Claves.NIVEL_LENGUAJE] = resultado.nivelLenguaje.coerceIn(1, 5)
            preferencias[Claves.APOYO_AUDIO] = resultado.apoyoAudio
            preferencias[Claves.PERFIL_FRUSTRACION] = resultado.perfilFrustracion.name
            preferencias[Claves.LATENCIA_REINTENTO] = resultado.latenciaReintentoMs
            preferencias[Claves.ESTRATEGIA_SEMILLAS] = resultado.estrategiaSemillas.name
            preferencias[Claves.CALIBRADO_EN] = System.currentTimeMillis()
            preferencias[Claves.PRIMERA_VEZ] = false
        }
    }

    private object Claves {
        val PRIMERA_VEZ = booleanPreferencesKey("is_first_launch")
        val NOMBRE = stringPreferencesKey("user_name")
        val EDAD = intPreferencesKey("user_age")
        val AVATAR = stringPreferencesKey("avatar_id")
        val NIVEL_MATEMATICAS = intPreferencesKey("baseline_math_level")
        val NIVEL_LENGUAJE = intPreferencesKey("baseline_language_level")
        val APOYO_AUDIO = booleanPreferencesKey("needs_audio_support")
        val PERFIL_FRUSTRACION = stringPreferencesKey("frustration_profile")
        val LATENCIA_REINTENTO = longPreferencesKey("retry_latency_ms")
        val ESTRATEGIA_SEMILLAS = stringPreferencesKey("seed_strategy")
        val CALIBRADO_EN = longPreferencesKey("calibrated_at")
    }
}

const val EDAD_MINIMA = 6
const val EDAD_MAXIMA = 10
private const val MAX_NOMBRE = 16
