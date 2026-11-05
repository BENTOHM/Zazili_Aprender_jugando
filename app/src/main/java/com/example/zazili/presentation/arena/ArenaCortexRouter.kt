package com.example.zazili.presentation.arena

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.logros.AccionesLogro
import com.example.zazili.core.logros.AchievementDispatcher
import com.example.zazili.domain.arena.DynamicDifficultyEngine
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.arena.matematicas.ArenaCortexMathHost
import com.example.zazili.presentation.arena.matematicas.ArenaCortexMathViewModel
import com.example.zazili.presentation.arena.matematicas.DestinoArena

/**
 * Enrutador único de la pestaña Arena Cortex.
 *
 * El destino vive en [ArenaCortexMathViewModel] (un `StateFlow`), no en una
 * pila de rutas ni en `remember` local. Eso garantiza que:
 *
 *  - tocar "Fracciones Festivas" abra [com.example.zazili.presentation.arena.matematicas.FraccionesFestivasScreen]
 *    y no la pantalla genérica de lecciones;
 *  - rotar la pantalla o cambiar de pestaña conserve el minijuego abierto;
 *  - el botón atrás cierre el juego y regrese al menú antes de salir de la
 *    Arena.
 *
 * Los otros 12 minijuegos siguen usando [onPlayGame]: su contenido todavía
 * se monta desde la navegación padre.
 */
@Composable
fun ArenaCortexRouter(
    onPlayGame: (ModuloZazili, String) -> Unit,
    onVolverAlHogar: () -> Unit,
    modifier: Modifier = Modifier,
    motor: DynamicDifficultyEngine? = null,
    viewModel: ArenaCortexMathViewModel = viewModel(),
    onAbrirJuegoTech: (String) -> Unit = {}
) {
    val contexto = LocalContext.current
    val motorArena = remember(contexto, motor) {
        motor ?: (contexto.applicationContext as ZaziliApplication)
            .container
            .dynamicDifficultyEngine
    }
    val destino by viewModel.destino.collectAsStateWithLifecycle()

    // El atrás del sistema cierra el juego, nunca la app ni la pestaña.
    BackHandler(enabled = destino is DestinoArena.Matematicas) {
        GlobalAudioManager.playPop()
        viewModel.cerrarJuego()
    }

    when (val actual = destino) {
        DestinoArena.Hub -> MenuArenaCortex(
            onAbrirJuegoMatematico = { juego ->
                // El menú ya suena al tocar la tarjeta: aquí solo se enruta.
                viewModel.abrirJuego(juego)
            },
            onPlayGame = onPlayGame,
            onVolverAlHogar = onVolverAlHogar,
            modifier = modifier,
            motor = motorArena,
            onAbrirJuegoTech = onAbrirJuegoTech
        )

        is DestinoArena.Matematicas -> ArenaCortexMathHost(
            juego = actual.juego,
            onSalir = viewModel::cerrarJuego,
            onResultado = { gano ->
                // La sesión terminó: la racha de la Arena (3 victorias suben
                // un nivel) se guarda fuera del ámbito de Compose para que
                // salir de inmediato no pierda el resultado.
                motorArena.registrarResultadoEnSegundoPlano(actual.juego.id, gano)
                // Toda partida de la Arena reporta al despachador global, así
                // los logros de minijuegos se evalúan al instante.
                AchievementDispatcher.notifyAction(
                    contexto,
                    AccionesLogro.MINIJUEGO_COMPLETADO
                )
            },
            // El menú ya pinta su propio inset; las pantallas del minijuego
            // llenan la pestaña y necesitan apartarse de la barra de estado.
            modifier = modifier.statusBarsPadding(),
            viewModel = viewModel
        )
    }
}
