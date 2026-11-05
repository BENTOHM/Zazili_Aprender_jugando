package com.example.zazili.presentation.modulos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.progress.GameProgressManager
import com.example.zazili.domain.model.SessionLog
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.EstadoSaga
import com.example.zazili.domain.saga.NodoSaga
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Lo que devuelve un intento: nunca regaña, siempre dice qué sigue. */
data class ResultadoReto(
    val correcto: Boolean,
    val primeraVez: Boolean,
    val estrellas: Int,
    val xpGanado: Int,
    val zazolesGanados: Int,
    val intentos: Int
)

/**
 * Lógica de un mundo: progreso, recompensas y registro de intentos.
 *
 * Las estrellas se ganan por constancia, no por velocidad: 3 al primer
 * intento, 2 al segundo y 1 del tercero en adelante. Repasar un reto ya
 * completado jamás quita estrellas ni duplica premios.
 */
class ModuloHubViewModel(application: Application) : AndroidViewModel(application) {

    private val contenedor = (application as ZaziliApplication).container

    val estado: StateFlow<EstadoSaga> = contenedor.progresoSaga.estado
    val progreso: StateFlow<GameProgress> = contenedor.gameProgressManager.progress
    val zazoles: StateFlow<Int> = contenedor.economiaZazoles.zazoles

    private val intentosPorNodo = mutableMapOf<String, Int>()

    fun responder(modulo: ModuloZazili, nodo: NodoSaga, indiceElegido: Int): ResultadoReto {
        val intento = (intentosPorNodo[nodo.id] ?: 0) + 1
        intentosPorNodo[nodo.id] = intento

        val correcto = indiceElegido == nodo.indiceCorrecto
        registrarIntento(modulo, nodo, correcto, intento)

        if (!correcto) {
            return ResultadoReto(
                correcto = false,
                primeraVez = false,
                estrellas = 0,
                xpGanado = 0,
                zazolesGanados = 0,
                intentos = intento
            )
        }

        val estrellas = when {
            intento <= 1 -> 3
            intento == 2 -> 2
            else -> 1
        }
        val primeraVez = contenedor.progresoSaga.completar(nodo.id, estrellas)
        // El marcador nunca baja: si ya tenía 3 estrellas, sigue mostrando 3.
        val estrellasVisibles = maxOf(
            estrellas,
            contenedor.progresoSaga.estado.value.estrellasDe(nodo.id)
        )

        if (primeraVez) {
            contenedor.gameProgressManager.awardXp(GameProgressManager.XP_PER_MINIGAME)
            contenedor.economiaZazoles.ganar(RECOMPENSA_ZAZOLES)
            contenedor.repositorioMascota.registrarLeccionCompletada()
        }

        return ResultadoReto(
            correcto = true,
            primeraVez = primeraVez,
            estrellas = estrellasVisibles,
            xpGanado = if (primeraVez) GameProgressManager.XP_PER_MINIGAME else 0,
            zazolesGanados = if (primeraVez) RECOMPENSA_ZAZOLES else 0,
            intentos = intento
        )
    }

    private fun registrarIntento(
        modulo: ModuloZazili,
        nodo: NodoSaga,
        exito: Boolean,
        intento: Int
    ) {
        viewModelScope.launch {
            runCatching {
                contenedor.progressRepository.recordAttempt(
                    SessionLog(
                        moduleRoute = modulo.name,
                        activityId = nodo.id,
                        succeeded = exito,
                        attemptCount = intento,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    companion object {
        const val RECOMPENSA_ZAZOLES = 15
    }
}
