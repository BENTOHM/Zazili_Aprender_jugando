package com.example.zazili.core.tiempo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

/**
 * Las cuatro caras del día de Zazi. El reloj de la casita manda: el tema de la
 * app y el fondo de su cuarto salen de aquí, nunca del modo oscuro de Android.
 *
 * - [AMANECER]: 06:00 a 11:59, tema claro.
 * - [DIA]: 12:00 a 17:59, tema claro.
 * - [TARDE]: 18:00 a 19:59, tema oscuro (hora dorada).
 * - [NOCHE]: 20:00 a 05:59, tema oscuro.
 */
enum class DayPhase(
    val etiqueta: String,
    val emoji: String,
    val nombreFondo: String,
    val esOscura: Boolean
) {
    AMANECER("Amanecer", "🌅", "fondo_amanecer", esOscura = false),
    DIA("Día", "☀️", "fondo_dia", esOscura = false),
    TARDE("Tarde", "🌇", "fondo_tarde", esOscura = true),
    NOCHE("Noche", "🌙", "fondo_noche", esOscura = true);

    companion object {
        /** Reparte la hora del reloj en la fase correspondiente. */
        fun desdeHora(hora: LocalTime): DayPhase = when (hora.hour) {
            in 6..11 -> AMANECER
            in 12..17 -> DIA
            in 18..19 -> TARDE
            else -> NOCHE
        }
    }
}

/**
 * Reloj de la app. No guarda estado ni abre hilos propios: solo traduce la hora
 * local a [DayPhase] y calcula cuánto falta para el siguiente minuto exacto.
 */
object TimeOfDayEngine {

    fun faseActual(reloj: LocalTime = LocalTime.now()): DayPhase =
        DayPhase.desdeHora(reloj)

    /**
     * Milisegundos hasta el próximo minuto en punto. Se suma un respiro mínimo
     * para no despertar justo en el borde y volver a calcular la misma fase.
     */
    fun milisegundosAlSiguienteMinuto(ahora: LocalDateTime = LocalDateTime.now()): Long {
        val siguiente = ahora.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        return Duration.between(ahora, siguiente).toMillis().coerceAtLeast(1_000L) + 50L
    }
}

/**
 * Fase vigente como estado de Compose.
 *
 * Se refresca cada minuto y solo mientras la app está visible
 * (`repeatOnLifecycle(STARTED)`); al pasar a segundo plano la corrutina se
 * cancela y no queda ningún temporizador despierto. `produceState` cancela
 * todo al salir de la composición, así que no hay fugas de memoria.
 */
@Composable
fun recordarFaseDelDia(): DayPhase {
    val duenoCiclo = LocalLifecycleOwner.current
    val fase by produceState(
        initialValue = TimeOfDayEngine.faseActual(),
        key1 = duenoCiclo
    ) {
        duenoCiclo.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val ahora = LocalDateTime.now()
                val nueva = TimeOfDayEngine.faseActual(ahora.toLocalTime())
                if (nueva != value) value = nueva
                delay(TimeOfDayEngine.milisegundosAlSiguienteMinuto(ahora))
            }
        }
    }
    return fase
}
