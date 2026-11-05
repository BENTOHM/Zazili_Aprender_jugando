package com.example.zazili.core.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.saga.ModuleMapScreen

/**
 * Contenedor de un mapa de módulo con la barra inferior de seis destinos.
 *
 * El mapa se pinta con [ModuleMapScreen], que lee el máximo desbloqueado del
 * módulo activo desde DataStore. La barra recibe el `NavController` completo y
 * navega por su cuenta; este Scaffold no reconstruye rutas ni guarda estado
 * paralelo. Las lecciones viven fuera de esta pantalla: al abrirlas, la barra
 * desaparece y toda la superficie es del motor de la lección.
 */
@Composable
fun MainNavigation(
    moduleId: Int,
    navController: NavHostController,
    onAbrirNodo: (ModuloZazili, String?) -> Unit,
    onIrANido: () -> Unit,
    onVolverAlHogar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ModuleNavigationBar(
                navController = navController,
                moduloRespaldo = moduleId
            )
        }
    ) { relleno ->
        ModuleMapScreen(
            moduleId = moduleId,
            onVolver = onVolverAlHogar,
            onAbrirNodo = onAbrirNodo,
            onIrANido = onIrANido,
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
        )
    }
}
