package com.example.zazili.domain.minijuegos

import androidx.compose.runtime.Immutable

/**
 * Estado puro de una partida, compartido por los 16 minijuegos.
 *
 * [Immutable] le promete al compilador de Compose que ninguna propiedad cambia
 * después de construirse: cada transición de [MinigameEngine] devuelve una
 * copia nueva. Así las pantallas pueden saltarse recomposiciones profundas y
 * comparar por igualdad estructural sin miedo a mutaciones escondidas.
 */
@Immutable
data class MinigameState(
    val minijuego: Minijuego,
    val difficultyLevel: Int = Minijuego.NIVEL_MINIMO,
    val rondaActual: Int = 1,
    val totalRondas: Int = MinigameEngine.RONDAS_POR_NIVEL,
    val aciertos: Int = 0,
    val errores: Int = 0,
    val vidas: Int = MinigameEngine.VIDAS_INICIALES,
    val puntos: Int = 0,
    val rachaActual: Int = 0,
    val mejorRacha: Int = 0,
    val terminado: Boolean = false
) {
    val rondasRespondidas: Int
        get() = (rondaActual - 1).coerceIn(0, totalRondas)

    val progreso: Float
        get() = if (totalRondas <= 0) {
            0f
        } else {
            rondasRespondidas.toFloat() / totalRondas
        }

    val porcentajeAcierto: Float
        get() = if (totalRondas <= 0) {
            0f
        } else {
            aciertos.toFloat() / totalRondas
        }

    val estrellas: Int
        get() = when {
            totalRondas > 0 && aciertos >= totalRondas -> 3
            porcentajeAcierto >= 0.70f -> 2
            porcentajeAcierto >= 0.50f -> 1
            else -> 0
        }

    val siguienteNivel: Int?
        get() = if (difficultyLevel < Minijuego.NIVEL_MAXIMO && estrellas >= 2) {
            difficultyLevel + 1
        } else {
            null
        }
}
