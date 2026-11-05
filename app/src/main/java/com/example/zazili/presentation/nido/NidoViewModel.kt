package com.example.zazili.presentation.nido

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.mascot.EstadoVital
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.domain.model.SessionLog
import com.example.zazili.domain.saga.EstadoSaga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Panel de adultos. El PIN se valida contra el hash con sal de [AccesoNido];
 * la sesión desbloqueada vive solo en memoria y se cierra al salir de la app.
 */
class NidoViewModel(application: Application) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container

    val estadoSaga: StateFlow<EstadoSaga> = contenedor.progresoSaga.estado
    val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles
    val mascota: StateFlow<EstadoVital> = contenedor.repositorioMascota.estado
    val configurado: StateFlow<Boolean> = contenedor.accesoNido.configurado

    val logs: StateFlow<List<SessionLog>> = contenedor.progressRepository.observeLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _desbloqueado = MutableStateFlow(false)
    val desbloqueado: StateFlow<Boolean> = _desbloqueado.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    fun configurarPin(pin: String, confirmacion: String): Boolean {
        val error = when {
            !contenedor.accesoNido.pinValido(pin) ->
                "El PIN debe tener exactamente 4 números."
            pin != confirmacion ->
                "Los dos PIN no coinciden. Vamos otra vez, sin prisa."
            contenedor.accesoNido.configurado.value ->
                "Este Nido ya tiene un PIN."
            else -> null
        }
        if (error != null) {
            _mensaje.value = error
            return false
        }
        val guardado = contenedor.accesoNido.configurarPin(pin)
        if (!guardado) {
            _mensaje.value = "No pudimos guardar el PIN. Inténtalo de nuevo."
            return false
        }
        _desbloqueado.value = true
        _mensaje.value = null
        return true
    }

    fun desbloquear(pin: String): Boolean {
        val valido = contenedor.accesoNido.verificarPin(pin)
        if (valido) {
            _desbloqueado.value = true
            _mensaje.value = null
        } else {
            _mensaje.value = "Ese PIN no es. Respira y prueba otra vez."
        }
        return valido
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    fun cerrarNido() {
        _desbloqueado.value = false
        _mensaje.value = null
    }
}
