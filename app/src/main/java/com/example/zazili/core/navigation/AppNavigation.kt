package com.example.zazili.core.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.accessibility.LocalZenMode
import com.example.zazili.core.ajustes.AjustesManager
import com.example.zazili.core.ajustes.ModoTema
import com.example.zazili.core.ajustes.aTemaPreferido
import com.example.zazili.core.audio.AudioEngine
import com.example.zazili.core.logros.Achievement
import com.example.zazili.core.logros.AchievementEngine
import com.example.zazili.core.logros.AchievementUnlockedBanner
import com.example.zazili.core.logros.DURACION_BANNER_LOGRO_MS
import com.example.zazili.core.logros.DURACION_SALIDA_BANNER_LOGRO_MS
import com.example.zazili.core.tiempo.recordarFaseDelDia
import com.example.zazili.core.ui.LevelUpCelebration
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.arena.TlacuachePoetaScreen
import com.example.zazili.presentation.hogar.HomeScreen
import com.example.zazili.presentation.lecciones.LanguageLessonScreen
import com.example.zazili.presentation.lecciones.LessonEngine
import com.example.zazili.presentation.lecciones.LessonScreen
import com.example.zazili.presentation.lecciones.MentalHealthLessonScreen
import com.example.zazili.presentation.lecciones.ScienceLessonScreen
import com.example.zazili.presentation.nido.NidoPadresScreen
import com.example.zazili.ui.theme.ZaziliTheme
import kotlinx.coroutines.delay

/**
 * Raíz de la app 2.0. Aplica el tema (claro/oscuro con conmutador en la
 * casita), publica el Modo Zen y entrega el control a la arquitectura maestra
 * ([ZaziliNavGraph]): compuerta de primera vez (avatar → diagnóstico) y, ya
 * calibrado el perfil, el tablero nuevo con armario y tienda.
 */
@Composable
fun ZaziliApp() {
    val context = LocalContext.current
    val aplicacion = context.applicationContext as ZaziliApplication
    val contenedor = remember(aplicacion) { aplicacion.container }
    val ajustes = remember(aplicacion) { AjustesManager.obtener(aplicacion) }
    val temaPreferido by ajustes.modoTema.collectAsStateWithLifecycle()
    // El niño manda: Día y Noche fuerzan su tema; solo Automático consulta el
    // reloj de Zazi. El modo oscuro del sistema Android nunca decide.
    val faseDelDia = recordarFaseDelDia()
    val oscuro = when (temaPreferido) {
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
        ModoTema.SISTEMA -> faseDelDia.esOscura
    }

    // El splash es una capa de arranque: se conserva entre rotaciones para no
    // repetir la carga, pero nunca sobrevive a un proceso nuevo.
    var splashTerminado by rememberSaveable { mutableStateOf(false) }

    val view = LocalView.current
    val actividad = remember(view) { view.context.findActivity() }
    SideEffect {
        val ventana = actividad?.window ?: return@SideEffect
        // El fondo del splash es morado profundo aunque el tema del niño sea
        // claro: mientras está visible, los íconos del sistema van en blanco.
        val iconosOscuros = splashTerminado && !oscuro
        WindowCompat.getInsetsController(ventana, view).apply {
            isAppearanceLightStatusBars = iconosOscuros
            isAppearanceLightNavigationBars = iconosOscuros
        }
    }

    ZaziliTheme(
        faseDelDia = faseDelDia,
        preferencia = temaPreferido.aTemaPreferido()
    ) {
        CompositionLocalProvider(LocalZenMode provides contenedor.zenMode) {
            // Raíz absoluta de la app: el splash cubre el arranque y, debajo,
            // el contenido real ya queda compuesto para aparecer sin saltos.
            // La capa de logros vive aquí, fuera del NavHost, para caer sobre
            // onboarding, tablero, lecciones y tienda por igual.
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = splashTerminado,
                    enter = fadeIn(tween(durationMillis = DURACION_SALIDA_SPLASH_MS)) +
                        scaleIn(
                            animationSpec = tween(durationMillis = DURACION_SALIDA_SPLASH_MS),
                            initialScale = 0.94f
                        ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    ZaziliNavGraph(
                        darkTheme = oscuro,
                        onToggleTema = { ajustes.ciclarTema() }
                    )
                }

                AnimatedVisibility(
                    visible = !splashTerminado,
                    exit = slideOutVertically(
                        targetOffsetY = { alto -> -alto },
                        animationSpec = tween(durationMillis = DURACION_SALIDA_SPLASH_MS)
                    ) + fadeOut(tween(durationMillis = DURACION_SALIDA_SPLASH_MS)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    ZaziliSplashScreen(onListo = { splashTerminado = true })
                }

                HostLogroGlobal(modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

/**
 * Puente entre el motor de logros y la capa visual global.
 *
 * Escucha [AchievementEngine.celebraciones] (eventos de una sola vez), encola
 * los logros en orden y muestra uno por uno durante 3.5 segundos. En cuanto la
 * tarjeta aparece suenan el efecto de éxito y la vibración larga; el siguiente
 * logro espera a que termine la animación de salida para no encimarse.
 */
@Composable
private fun HostLogroGlobal(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val motor = remember(contexto) { AchievementEngine.obtener(contexto) }
    val hapticos = LocalHapticFeedback.current

    val pendientes = remember { mutableStateListOf<Achievement>() }
    var actual by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(motor) {
        motor.celebraciones.collect { logro -> pendientes.add(logro) }
    }

    LaunchedEffect(pendientes.size, actual) {
        if (actual == null && pendientes.isNotEmpty()) {
            actual = pendientes.removeAt(0)
        }
    }

    LaunchedEffect(actual?.id) {
        val logro = actual ?: return@LaunchedEffect
        AudioEngine.playSuccess()
        hapticos.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(DURACION_BANNER_LOGRO_MS + DURACION_SALIDA_BANNER_LOGRO_MS)
        if (actual?.id == logro.id) actual = null
    }

    val logroVisible = actual
    if (logroVisible != null) {
        AchievementUnlockedBanner(
            achievement = logroVisible,
            modifier = modifier
        )
    }
}

/**
 * Grafo principal, ya calibrado el perfil: casita, barra de cinco pestañas
 * (cuatro mapas de lecciones + Arena Cortex), lección y Nido de adultos.
 *
 * Las lecciones viven FUERA de [MainNavigation] para que la barra inferior no
 * distraiga durante una pregunta y para que el motor de lección tenga la
 * pantalla completa. No existe ninguna ruta de regreso al diagnóstico.
 */
@Composable
fun AppNavigation(
    darkTheme: Boolean,
    onToggleTema: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destinos.HOGAR,
            enterTransition = {
                fadeIn(tween(durationMillis = 220)) +
                    slideInHorizontally(tween(durationMillis = 260)) { ancho -> ancho / 14 }
            },
            exitTransition = { fadeOut(tween(durationMillis = 180)) },
            popEnterTransition = { fadeIn(tween(durationMillis = 180)) },
            popExitTransition = {
                fadeOut(tween(durationMillis = 180)) +
                    slideOutHorizontally(tween(durationMillis = 220)) { ancho -> ancho / 14 }
            }
        ) {
            composable(Destinos.HOGAR) {
                HomeScreen(
                    onIrASaga = { navController.navigate(Destinos.aprender()) },
                    onIrAlNido = { navController.navigate(Destinos.NIDO) },
                    darkTheme = darkTheme,
                    onToggleTema = onToggleTema
                )
            }

            composable(
                route = Destinos.APRENDER,
                arguments = listOf(
                    navArgument(Destinos.ARG_MODULO) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entrada ->
                val modulo = Destinos.moduloDesdeRuta(
                    entrada.arguments?.getString(Destinos.ARG_MODULO)
                ) ?: ModuloZazili.TIANGUIS_NUMERICO
                RutaMapaModulo(
                    moduleId = LessonEngine.idDeModulo(modulo)
                        ?: LessonEngine.MODULO_MATEMATICAS_ID,
                    navController = navController
                )
            }

            composable(
                route = RutasZazili.MODULO_MAPA,
                arguments = listOf(
                    navArgument(RutasZazili.ARG_MODULE_ID) {
                        type = NavType.IntType
                        defaultValue = LessonEngine.MODULO_MATEMATICAS_ID
                    }
                )
            ) { entrada ->
                RutaMapaModulo(
                    moduleId = entrada.arguments
                        ?.getInt(RutasZazili.ARG_MODULE_ID)
                        ?: LessonEngine.MODULO_MATEMATICAS_ID,
                    navController = navController
                )
            }

            composable(
                route = Destinos.LECCION,
                arguments = listOf(
                    navArgument(Destinos.ARG_MODULO) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(Destinos.ARG_NODO) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { entrada ->
                val modulo = Destinos.moduloDesdeRuta(
                    entrada.arguments?.getString(Destinos.ARG_MODULO)
                )
                if (modulo == null) {
                    // Ruta corrupta o enlace viejo: se regresa a un destino
                    // real en lugar de dejar la pantalla en blanco.
                    LaunchedEffect(Unit) {
                        if (!navController.popBackStack()) {
                            navController.navigate(Destinos.HOGAR) {
                                launchSingleTop = true
                            }
                        }
                    }
                } else {
                    val nodoId = entrada.arguments
                        ?.getString(Destinos.ARG_NODO)
                        ?.takeIf { it.isNotBlank() }

                    if (modulo == ModuloZazili.CUEVA_LEYENDAS &&
                        nodoId == Minijuego.TlacuachePoeta.id
                    ) {
                        // El jefe de Lenguaje tiene prototipo jugable propio:
                        // swipe con nivel adaptativo y reporte a la Arena.
                        TlacuachePoetaRoute(
                            onSalir = { navController.popBackStack() }
                        )
                    } else {
                        when (modulo) {
                            ModuloZazili.CUEVA_LEYENDAS -> LanguageLessonScreen(
                                modulo = modulo,
                                nodoId = nodoId,
                                onSalir = { navController.popBackStack() }
                            )

                            ModuloZazili.ARBOL_ABRAZOS -> MentalHealthLessonScreen(
                                modulo = modulo,
                                nodoId = nodoId,
                                onSalir = { navController.popBackStack() }
                            )

                            // El Laboratorio del Cenote tiene su propia pantalla:
                            // enruta filtro mágico, arenero de reacciones y
                            // opciones sin pasar por el motor de Matemáticas.
                            ModuloZazili.LABORATORIO_CENOTE -> ScienceLessonScreen(
                                modulo = modulo,
                                nodoId = nodoId,
                                onSalir = { navController.popBackStack() }
                            )

                            else -> LessonScreen(
                                modulo = modulo,
                                nodoId = nodoId,
                                onSalir = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }

            composable(Destinos.NIDO) {
                NidoPadresScreen(onVolver = { navController.popBackStack() })
            }
        }

        LevelUpCelebration()
    }
}

/**
 * Un mapa de módulo con la barra estricta de cinco pestañas, compartido por la
 * ruta heredada `aprender` y por `module_map_route/{moduleId}`.
 */
@Composable
private fun RutaMapaModulo(
    moduleId: Int,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    MainNavigation(
        moduleId = moduleId,
        navController = navController,
        onVolverAlHogar = { navController.popBackStack() },
        onAbrirNodo = { modulo, nodoId ->
            navController.navigate(Destinos.leccion(modulo, nodoId)) {
                launchSingleTop = true
                // La lección siempre se apila sobre su mapa: volver atrás
                // devuelve al nodo que la abrió.
                restoreState = false
            }
        },
        onIrANido = {
            navController.navigate(Destinos.NIDO) { launchSingleTop = true }
        },
        modifier = modifier
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Envoltura de Tlacuache Poeta: resuelve el motor adaptativo del contenedor,
 * le pasa el nivel vigente y devuelve el resultado UNA sola vez por sesión.
 */
@Composable
private fun TlacuachePoetaRoute(onSalir: () -> Unit) {
    val contexto = LocalContext.current
    val motor = remember(contexto) {
        (contexto.applicationContext as ZaziliApplication)
            .container
            .dynamicDifficultyEngine
    }
    val estado by motor.estado.collectAsStateWithLifecycle()
    val juegoId = Minijuego.TlacuachePoeta.id

    TlacuachePoetaScreen(
        difficultyLevel = estado.nivelDe(juegoId),
        onSalir = onSalir,
        onResultado = { gano ->
            motor.registrarResultadoEnSegundoPlano(juegoId, gano)
        }
    )
}
