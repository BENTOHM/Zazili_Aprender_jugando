package com.example.zazili.presentation.saga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.progress.UserProgressManager
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.lecciones.LessonEngine

/**
 * Mapa de un módulo pedido por número (`module_map_route/{moduleId}`).
 *
 * Es el único punto de entrada del Tablero y de la barra inferior hacia los
 * caminos de aprendizaje. El número se traduce con [LessonEngine.moduloDeId],
 * la misma llave que usa el avance persistido, así la ruta y el desbloqueo
 * nunca se desincronizan.
 *
 * El máximo desbloqueado se lee UNA sola vez aquí, con la llave explícita del
 * módulo activo, y se entrega al mapa como un dato más. De ahí en adelante la
 * regla es universal para los cinco módulos: `leccion <= maximo` está abierta.
 * El mapa no reescribe destinos: cada nodo abre la lección que le toca.
 *
 * Un número desconocido no deja la pantalla en blanco: se regresa al destino
 * anterior.
 */
@Composable
fun ModuleMapScreen(
    moduleId: Int,
    onVolver: () -> Unit,
    onAbrirNodo: (ModuloZazili, String?) -> Unit,
    onIrANido: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modulo = LessonEngine.moduloDeId(moduleId)
    if (modulo == null) {
        LaunchedEffect(moduleId) { onVolver() }
        return
    }

    val contexto = LocalContext.current
    val progresoUsuario = remember(contexto) { UserProgressManager.obtener(contexto) }
    // DataStore empuja cada cambio: al volver de una lección el nodo siguiente
    // se enciende solo, sin reiniciar la pantalla ni el mapa. El flujo se
    // recuerda por módulo para no reiniciar la colección en cada recomposición.
    val flujoDesbloqueo = remember(progresoUsuario, moduleId) {
        progresoUsuario.getUnlockedLesson(moduleId)
    }
    val highestUnlocked by flujoDesbloqueo
        .collectAsStateWithLifecycle(initialValue = UserProgressManager.LECCION_INICIAL)

    when (modulo) {
        ModuloZazili.CUEVA_LEYENDAS -> LanguageSagaMapScreen(
            desbloqueoMaximo = highestUnlocked,
            onVolver = onVolver,
            onAbrirModulo = onAbrirNodo,
            onIrANido = onIrANido,
            modifier = modifier
        )

        ModuloZazili.ARBOL_ABRAZOS -> MentalHealthSagaMapScreen(
            desbloqueoMaximo = highestUnlocked,
            onVolver = onVolver,
            onAbrirModulo = onAbrirNodo,
            onIrANido = onIrANido,
            modifier = modifier
        )

        ModuloZazili.LABORATORIO_CENOTE -> ScienceSagaMapScreen(
            desbloqueoMaximo = highestUnlocked,
            onVolver = onVolver,
            onAbrirModulo = onAbrirNodo,
            onIrANido = onIrANido,
            modifier = modifier
        )

        else -> SagaMapScreen(
            modulo = modulo,
            desbloqueoMaximo = highestUnlocked,
            onVolver = onVolver,
            onAbrirModulo = onAbrirNodo,
            onIrANido = onIrANido,
            modifier = modifier
        )
    }
}
