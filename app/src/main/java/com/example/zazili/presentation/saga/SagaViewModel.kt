package com.example.zazili.presentation.saga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.domain.saga.EstadoSaga
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado del mapa vertical de la Saga. Solo lee; completar retos ocurre en
 * cada módulo y se refleja aquí gracias a los flujos compartidos del
 * contenedor.
 */
class SagaViewModel(application: Application) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container

    val estado: StateFlow<EstadoSaga> = contenedor.progresoSaga.estado
    val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles
}
