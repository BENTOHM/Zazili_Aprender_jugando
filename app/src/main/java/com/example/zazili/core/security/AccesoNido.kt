package com.example.zazili.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PIN del Nido de los Grandes.
 *
 * No se guarda el PIN, solo un hash SHA-256 con sal aleatoria. Sirve como
 * barrera amable para que el panel de adultos no quede al alcance de un toque;
 * no sustituye el cifrado de disco del sistema operativo. Si el
 * almacenamiento falla, el panel no se abre por accidente.
 */
class AccesoNido(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure {
        Log.w(TAG, "El PIN del Nido no está disponible", it)
    }.getOrNull()

    private val _configurado = MutableStateFlow(!prefs?.getString(KEY_HASH, null).isNullOrBlank())
    val configurado: StateFlow<Boolean> = _configurado.asStateFlow()

    fun pinValido(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

    /** Crea el PIN la primera vez. Devuelve false si ya existe o no es válido. */
    @Synchronized
    fun configurarPin(pin: String): Boolean {
        if (_configurado.value || !pinValido(pin)) return false
        val storage = prefs ?: return false
        return runCatching {
            val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
            storage.edit()
                .putString(KEY_SAL, sal.toHex())
                .putString(KEY_HASH, hashear(sal, pin))
                .apply()
            _configurado.value = true
            true
        }.onFailure {
            Log.w(TAG, "No se pudo guardar el PIN del Nido", it)
        }.getOrDefault(false)
    }

    /** Compara el PIN en tiempo constante contra el hash guardado. */
    fun verificarPin(pin: String): Boolean {
        if (!pinValido(pin)) return false
        val storage = prefs ?: return false
        val salHex = storage.getString(KEY_SAL, null) ?: return false
        val hashGuardado = storage.getString(KEY_HASH, null) ?: return false
        val sal = salHex.fromHex() ?: return false
        val calculado = hashear(sal, pin)
        return MessageDigest.isEqual(
            hashGuardado.toByteArray(Charsets.UTF_8),
            calculado.toByteArray(Charsets.UTF_8)
        )
    }

    private fun hashear(sal: ByteArray, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sal)
        digest.update("zazili-nido".toByteArray(Charsets.UTF_8))
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun String.fromHex(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            ByteArray(length / 2) { indice ->
                substring(indice * 2, indice * 2 + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }

    private companion object {
        const val TAG = "AccesoNido"
        const val PREFS_NAME = "zazili_nido"
        const val KEY_SAL = "pin_sal"
        const val KEY_HASH = "pin_hash"
    }
}
