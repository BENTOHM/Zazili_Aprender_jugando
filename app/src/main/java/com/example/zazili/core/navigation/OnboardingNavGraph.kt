package com.example.zazili.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.core.usuario.EDAD_MAXIMA
import com.example.zazili.core.usuario.EDAD_MINIMA
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.core.usuario.UserManager
import com.example.zazili.domain.diagnostico.ResultadoDiagnostico
import com.example.zazili.presentation.onboarding.DiagnosticGameScreen
import com.example.zazili.presentation.onboarding.ProfileCreationScreen
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoProfundo
import kotlinx.coroutines.launch

private const val RUTA_PERFIL = "onboarding/perfil"
private const val RUTA_DIAGNOSTICO = "onboarding/diagnostico"

/**
 * Compuerta única de la primera vez.
 *
 * El `null` de [UserManager.perfil] significa "DataStore todavía está
 * leyendo": jamás se pinta la casita antes de saber la respuesta. Si la
 * instalación ya está calibrada se monta [AppNavigation] directamente; si no,
 * el niño pasa por perfil y "El Vuelo de Prueba". En el instante en que
 * `is_first_launch` cambia a `false`, este grafo deja de existir, así que no
 * hay botón, gesto ni ruta que pueda regresar al diagnóstico.
 */
@Composable
fun OnboardingNavGraph(
    darkTheme: Boolean,
    onToggleTema: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val aplicacion = context.applicationContext as ZaziliApplication
    val usuario = remember(aplicacion) { aplicacion.container.userManager }
    val perfil by usuario.perfil.collectAsStateWithLifecycle(initialValue = null)

    val actual = perfil
    when {
        actual == null -> PantallaCarga(modifier = modifier)
        !actual.primeraVez -> AppNavigation(
            darkTheme = darkTheme,
            onToggleTema = onToggleTema,
            modifier = modifier
        )
        else -> OnboardingPrimeraVez(
            usuario = usuario,
            perfil = actual,
            modifier = modifier
        )
    }
}

@Composable
private fun OnboardingPrimeraVez(
    usuario: UserManager,
    perfil: PerfilUsuario,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val alcance = rememberCoroutineScope()

    var guardandoPerfil by remember { mutableStateOf(false) }
    var errorPerfil by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf(perfil.nombre) }
    var edad by rememberSaveable { mutableIntStateOf(perfil.edad) }

    var guardandoCierre by remember { mutableStateOf(false) }
    var errorCierre by rememberSaveable { mutableStateOf(false) }
    var resultadoPendiente by remember { mutableStateOf<ResultadoDiagnostico?>(null) }

    fun cerrarOnboarding(resultado: ResultadoDiagnostico) {
        if (guardandoCierre) return
        resultadoPendiente = resultado
        guardandoCierre = true
        errorCierre = false
        alcance.launch {
            runCatching { usuario.completarOnboarding(resultado) }
                .onSuccess { resultadoPendiente = null }
                .onFailure {
                    guardandoCierre = false
                    errorCierre = true
                }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = RUTA_PERFIL,
            enterTransition = { androidx.compose.animation.fadeIn() },
            exitTransition = { androidx.compose.animation.fadeOut() },
            popEnterTransition = { androidx.compose.animation.fadeIn() },
            popExitTransition = { androidx.compose.animation.fadeOut() }
        ) {
            composable(RUTA_PERFIL) {
                ProfileCreationScreen(
                    nombreInicial = nombre,
                    edadInicial = edad,
                    avatarInicial = perfil.avatarId,
                    guardando = guardandoPerfil,
                    errorAlGuardar = errorPerfil,
                    onContinuar = { nombreElegido, edadElegida, avatarId ->
                        if (!guardandoPerfil) {
                            errorPerfil = false
                            guardandoPerfil = true
                            alcance.launch {
                                runCatching {
                                    usuario.guardarPerfil(nombreElegido, edadElegida, avatarId)
                                }.onSuccess {
                                    nombre = nombreElegido.trim()
                                    edad = edadElegida
                                    guardandoPerfil = false
                                    navController.navigate(RUTA_DIAGNOSTICO) {
                                        popUpTo(RUTA_PERFIL) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }.onFailure {
                                    guardandoPerfil = false
                                    errorPerfil = true
                                }
                            }
                        }
                    }
                )
            }

            composable(RUTA_DIAGNOSTICO) {
                DiagnosticGameScreen(
                    nombre = nombre.ifBlank { perfil.nombre },
                    edad = edad.takeIf { it in EDAD_MINIMA..EDAD_MAXIMA } ?: perfil.edad,
                    onCompletar = ::cerrarOnboarding
                )
            }
        }

        val pendiente = resultadoPendiente
        if (errorCierre && pendiente != null) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(
                        text = "No pudimos guardar tu casita",
                        style = MaterialTheme.typography.titleLarge,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Zazi sigue aquí. ¿Lo intentamos otra vez?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    TextButton(onClick = { cerrarOnboarding(pendiente) }) {
                        Text(
                            text = "Intentar otra vez",
                            color = MagicalPurple,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PantallaCarga(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ZaziSpriteAnimado(
                size = 132.dp,
                estado = ZaziState.VUELO_IDLE,
                animar = true,
                descripcion = "Zazi se está despertando"
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Despertando a Zazi…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = MagicalPurple,
                strokeWidth = 3.dp
            )
        }
    }
}
