package com.example.zazili.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.StoryTimeScreen
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.core.usuario.EDAD_MAXIMA
import com.example.zazili.core.usuario.EDAD_MINIMA
import com.example.zazili.domain.diagnostico.ResultadoDiagnostico
import com.example.zazili.domain.arena.ArenaCortexRepository
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.arena.ArenaCortexScreen
import com.example.zazili.presentation.arena.ArenaTechGameEngine
import com.example.zazili.presentation.arena.AplastaElBugTechScreen
import com.example.zazili.presentation.arena.CyberSwipeScreen
import com.example.zazili.presentation.arena.RutaDelAlgoritmoTechScreen
import com.example.zazili.presentation.arena.TechGame
import com.example.zazili.presentation.ajustes.AjustesScreen
import com.example.zazili.presentation.arena.TlacuachePoetaScreen
import com.example.zazili.presentation.avatar.ChildAvatarShopScreen
import com.example.zazili.presentation.avatar.ProfileAndWardrobeScreen
import com.example.zazili.presentation.chat.ZaziChatScreen
import com.example.zazili.presentation.dashboard.AvisoBloqueoJuego
import com.example.zazili.presentation.dashboard.MainDashboardScreen
import com.example.zazili.presentation.logros.AchievementsScreen
import com.example.zazili.presentation.dashboard.MainViewModel
import com.example.zazili.presentation.lecciones.LanguageLessonScreen
import com.example.zazili.presentation.lecciones.LessonEngine
import com.example.zazili.presentation.lecciones.LessonScreen
import com.example.zazili.presentation.lecciones.MentalHealthLessonScreen
import com.example.zazili.presentation.lecciones.ScienceLessonScreen
import com.example.zazili.presentation.nido.NidoPadresScreen
import com.example.zazili.presentation.onboarding.AvatarCreationScreen
import com.example.zazili.presentation.onboarding.DiagnosticGameScreen
import com.example.zazili.presentation.onboarding.EstadoOnboarding
import com.example.zazili.presentation.onboarding.OnboardingFlowManager
import com.example.zazili.presentation.tienda.Tienditas3BScreen
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoProfundo
import kotlinx.coroutines.launch

/** Rutas de la arquitectura maestra. Las de aprendizaje viven en [Destinos]. */
object RutasZazili {
    const val BIENVENIDA = "onboarding"
    const val CREAR_AVATAR = "onboarding/avatar"
    const val EXAMEN = "onboarding/examen"
    const val TABLERO = "tablero"
    const val ARMARIO = "perfil"

    /** Plática con Zazi (Gemini). Única entrada de texto de la app. */
    const val CHAT = "chat"

    /** Tienda exclusiva de Zazi: comida, cuidados y accesorios de ave. */
    const val TIENDITAS_3B = "tienditas_3b"

    /** Tienda exclusiva del niño: ropa, peinados y máscaras pixel. */
    const val BOUTIQUE_PIXEL = "child_avatar_shop"

    /** Galería completa de logros (trofeo del Top Bar). */
    const val LOGROS = "achievements_gallery"

    /** Ajustes básicos: tema, música, efectos y Modo Zen. */
    const val AJUSTES = "ajustes"

    /** Biblioteca de cuentos históricos con lectura en voz alta. */
    const val CUENTOS = "cuentacuentos"

    const val ARENA = "arena"

    /**
     * Rutas exactas de la barra inferior. Son las que usa [BottomNavItem] tal
     * cual, sin prefijos ni argumentos. La pestaña 4 es Emociones y la 5 es
     * Tecnología; sus ids internos siguen invertidos para no perder progreso.
     */
    const val MAPA_PESTANA_MATEMATICAS = "map/1"
    const val MAPA_PESTANA_LENGUAJE = "map/2"
    const val MAPA_PESTANA_CIENCIAS = "map/3"
    const val MAPA_PESTANA_EMOCIONES = "map/4"
    const val MAPA_PESTANA_TECNOLOGIA = "map/5"

    /**
     * Arena Cortex de la barra inferior: abre el MISMO menú de 19 juegos
     * (los 16 originales + los 3 de tecnología) con la barra a la vista.
     */
    const val ARENA_CORTEX = "arena_cortex"

    /**
     * Rutas de los tres minijuegos anexados. Son destinos sin argumentos:
     * cada uno arranca en su nivel de tecnología correspondiente.
     */
    const val JUEGO_APLASTA_BUG = "route_whack_a_bug"
    const val JUEGO_RUTA_ALGORITMO = "route_algorithm"
    const val JUEGO_CIBER_SEGURO = "route_cyber_swipe"

    /** Argumento numérico del módulo; el 4 corresponde a Tecnología. */
    const val ARG_MODULE_ID = "moduleId"

    /**
     * Lección pedida por número de módulo. El `nodo` es opcional: sin él, el
     * motor de Tecnología arranca en el primer bloque sin completar.
     */
    const val LECCION_MODULO =
        "leccion_modulo/{" + ARG_MODULE_ID + "}?nodo={" + Destinos.ARG_NODO + "}"

    fun leccionModulo(moduleId: Int, nodoId: String? = null): String =
        "leccion_modulo/$moduleId" + if (nodoId.isNullOrBlank()) "" else "?nodo=$nodoId"

    /**
     * Mapa de un módulo pedido por número (1 Matemáticas … 5 Emociones). Es la
     * entrada del Tablero: el mundo se abre en su mapa, nunca en una lección.
     */
    const val MODULO_MAPA = "module_map_route/{" + ARG_MODULE_ID + "}"

    fun moduloMapa(moduleId: Int): String = "module_map_route/$moduleId"
}

/**
 * Arquitectura de navegación maestra de Zazili 2.0.
 *
 * Dos grafos que nunca conviven:
 *
 * 1. **Bienvenida** (`onboarding`): creador de avatar → "El Vuelo de Prueba".
 *    Solo existe mientras `is_first_launch` sea `true`.
 * 2. **Principal**: tablero, armario, tienda, mapas de aprendizaje, lecciones,
 *    Arena Cortex y Nido.
 *
 * El cambio entre grafos lo provoca [OnboardingFlowManager] al cerrar el
 * diagnóstico. En ese instante este composable cambia de rama y el `NavHost` de
 * bienvenida se destruye con TODA su pila: no hay gesto, botón del sistema ni
 * ruta que devuelva al niño a la creación de avatar. Dentro de la bienvenida,
 * la transición al examen usa `popUpTo(CREAR_AVATAR) { inclusive = true }` como
 * segunda barrera.
 */
@Composable
fun ZaziliNavGraph(
    darkTheme: Boolean,
    onToggleTema: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val onboarding = remember(contexto) { OnboardingFlowManager.obtener(contexto) }
    val estado by onboarding.estado.collectAsStateWithLifecycle()

    when {
        estado.cargando -> PantallaCargaZazili(modifier = modifier)
        estado.primeraVez -> GrafoBienvenida(
            onboarding = onboarding,
            estado = estado,
            modifier = modifier
        )

        else -> GrafoPrincipal(
            darkTheme = darkTheme,
            onToggleTema = onToggleTema,
            modifier = modifier
        )
    }
}

// ---------------------------------------------------------------------------
//  GRAFO 1: BIENVENIDA (AVATAR → EXAMEN)
// ---------------------------------------------------------------------------

@Composable
private fun GrafoBienvenida(
    onboarding: OnboardingFlowManager,
    estado: EstadoOnboarding,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val alcance = rememberCoroutineScope()

    var cerrando by remember { mutableStateOf(false) }
    var errorCierre by rememberSaveable { mutableStateOf(false) }
    var pendiente by remember { mutableStateOf<ResultadoDiagnostico?>(null) }

    // El destino inicial se congela: si el niño ya había creado su avatar y
    // cerró la app, entra directo al examen. Recalcularlo cuando la bandera
    // cambia reconstruiría el NavHost y perdería la transición.
    val inicio = remember {
        if (estado.avatarCreado) RutasZazili.EXAMEN else RutasZazili.CREAR_AVATAR
    }

    LaunchedEffect(estado.avatarCreado) {
        if (!estado.avatarCreado) return@LaunchedEffect
        val actual = navController.currentBackStackEntry?.destination?.route
        if (actual == RutasZazili.EXAMEN) return@LaunchedEffect
        navController.navigate(RutasZazili.EXAMEN) {
            popUpTo(RutasZazili.CREAR_AVATAR) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun cerrarConDiagnostico(resultado: ResultadoDiagnostico) {
        if (cerrando) return
        cerrando = true
        errorCierre = false
        pendiente = resultado
        alcance.launch {
            if (onboarding.completarDiagnostico(resultado)) {
                // `primeraVez` pasa a false y este grafo se destruye completo.
                pendiente = null
            } else {
                cerrando = false
                errorCierre = true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = inicio,
            route = RutasZazili.BIENVENIDA,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(durationMillis = 240)) },
            exitTransition = { fadeOut(tween(durationMillis = 180)) },
            popEnterTransition = { fadeIn(tween(durationMillis = 180)) },
            popExitTransition = { fadeOut(tween(durationMillis = 180)) }
        ) {
            composable(RutasZazili.CREAR_AVATAR) {
                AvatarCreationScreen(
                    nombreInicial = estado.nombre,
                    edadInicial = estado.edad,
                    equipamientoInicial = estado.equipamientoInicial,
                    onContinuar = { _, _, _ ->
                        GlobalAudioManager.playSuccess()
                        // El ViewModel ya escribió en DataStore; la bandera
                        // `avatarCreado` también navega por su cuenta. El
                        // chequeo evita empujar el examen dos veces.
                        val actual = navController.currentBackStackEntry?.destination?.route
                        if (actual != RutasZazili.EXAMEN) {
                            navController.navigate(RutasZazili.EXAMEN) {
                                popUpTo(RutasZazili.CREAR_AVATAR) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(RutasZazili.EXAMEN) {
                DiagnosticGameScreen(
                    nombre = estado.nombre,
                    edad = estado.edad.coerceIn(EDAD_MINIMA, EDAD_MAXIMA),
                    onCompletar = ::cerrarConDiagnostico
                )
            }
        }

        val resultadoPendiente = pendiente
        if (errorCierre && resultadoPendiente != null) {
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
                    TextButton(onClick = { cerrarConDiagnostico(resultadoPendiente) }) {
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

// ---------------------------------------------------------------------------
//  GRAFO 2: APP PRINCIPAL
// ---------------------------------------------------------------------------

@Composable
private fun GrafoPrincipal(
    darkTheme: Boolean,
    onToggleTema: () -> Unit,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val bloqueo by mainViewModel.bloqueo.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = RutasZazili.TABLERO,
        modifier = Modifier.fillMaxSize(),
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
        composable(RutasZazili.TABLERO) {
            MainDashboardScreen(
                onAbrirTienda = {
                    navController.navigate(RutasZazili.TIENDITAS_3B) {
                        launchSingleTop = true
                    }
                },
                onAbrirPerfil = {
                    navController.navigate(RutasZazili.ARMARIO) { launchSingleTop = true }
                },
                onAbrirBoutique = {
                    navController.navigate(RutasZazili.BOUTIQUE_PIXEL) {
                        launchSingleTop = true
                    }
                },
                onIrAAprender = {
                    navController.navigate(
                        RutasZazili.moduloMapa(LessonEngine.MODULO_MATEMATICAS_ID)
                    ) { launchSingleTop = true }
                },
                onAbrirModulo = { modulo ->
                    // Todos los mundos abren su mapa; la barra inferior hace el
                    // resto. Un módulo sin número cae en Matemáticas.
                    val moduleId = LessonEngine.idDeModulo(modulo)
                        ?: LessonEngine.MODULO_MATEMATICAS_ID
                    navController.navigate(RutasZazili.moduloMapa(moduleId)) {
                        launchSingleTop = true
                    }
                },
                onIrAArena = {
                    navController.navigate(RutasZazili.ARENA) { launchSingleTop = true }
                },
                onIrANido = {
                    navController.navigate(Destinos.NIDO) { launchSingleTop = true }
                },
                onAbrirAjustes = {
                    navController.navigate(RutasZazili.AJUSTES) { launchSingleTop = true }
                },
                darkTheme = darkTheme,
                onToggleTema = onToggleTema,
                onAbrirLogros = {
                    navController.navigate(RutasZazili.LOGROS) { launchSingleTop = true }
                },
                onAbrirCuentos = {
                    navController.navigate(RutasZazili.CUENTOS) { launchSingleTop = true }
                },
                onAbrirChat = {
                    navController.navigate(RutasZazili.CHAT) { launchSingleTop = true }
                },
                viewModel = mainViewModel
            )
        }

        composable(RutasZazili.CUENTOS) {
            StoryTimeScreen(onSalir = { navController.popBackStack() })
        }

        composable(RutasZazili.AJUSTES) {
            AjustesScreen(
                onVolver = { navController.popBackStack() },
                darkTheme = darkTheme
            )
        }

        composable(RutasZazili.ARMARIO) {
            ProfileAndWardrobeScreen(
                onSalir = { navController.popBackStack() },
                onAbrirTienda = {
                    navController.navigate(RutasZazili.BOUTIQUE_PIXEL) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(RutasZazili.TIENDITAS_3B) {
            Tienditas3BScreen(onSalir = { navController.popBackStack() })
        }

        composable(RutasZazili.BOUTIQUE_PIXEL) {
            ChildAvatarShopScreen(onSalir = { navController.popBackStack() })
        }

        composable(RutasZazili.LOGROS) {
            AchievementsScreen(onSalir = { navController.popBackStack() })
        }

        // Rutas exactas de la barra inferior: "map/1" … "map/5". Se
        // registran una por una para que el clic de cada pestaña SIEMPRE
        // encuentre destino en este grafo.
        composable(RutasZazili.MAPA_PESTANA_MATEMATICAS) {
            ContenidoMapaModulo(
                moduleId = LessonEngine.MODULO_MATEMATICAS_ID,
                navController = navController,
                mainViewModel = mainViewModel
            )
        }

        composable(RutasZazili.MAPA_PESTANA_LENGUAJE) {
            ContenidoMapaModulo(
                moduleId = LessonEngine.MODULO_LENGUAJE_ID,
                navController = navController,
                mainViewModel = mainViewModel
            )
        }

        composable(RutasZazili.MAPA_PESTANA_CIENCIAS) {
            ContenidoMapaModulo(
                moduleId = LessonEngine.MODULO_CIENCIAS_ID,
                navController = navController,
                mainViewModel = mainViewModel
            )
        }

        composable(RutasZazili.MAPA_PESTANA_EMOCIONES) {
            ContenidoMapaModulo(
                moduleId = LessonEngine.MODULO_EMOCIONES_ID,
                navController = navController,
                mainViewModel = mainViewModel
            )
        }

        composable(RutasZazili.MAPA_PESTANA_TECNOLOGIA) {
            ContenidoMapaModulo(
                moduleId = LessonEngine.MODULO_TECNOLOGIA_ID,
                navController = navController,
                mainViewModel = mainViewModel
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
            ContenidoMapaModulo(
                moduleId = entrada.arguments
                    ?.getInt(RutasZazili.ARG_MODULE_ID)
                    ?: LessonEngine.MODULO_MATEMATICAS_ID,
                navController = navController,
                mainViewModel = mainViewModel
            )
        }

        composable(RutasZazili.ARENA) {
            DisposableEffect(mainViewModel) {
                mainViewModel.abrirSesionActividad()
                onDispose { mainViewModel.cerrarSesionActividad() }
            }
            ArenaCortexScreen(
                onPlayGame = { modulo, minijuegoId ->
                    mainViewModel.intentarAbrirActividad {
                        navController.navigate(Destinos.leccion(modulo, minijuegoId)) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                },
                onVolverAlHogar = { navController.popBackStack() },
                onAbrirJuegoTech = { ruta ->
                    mainViewModel.intentarAbrirActividad {
                        navController.navigate(ruta) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            )
        }

        // La pestaña "Arena Cortex" muestra el mismo menú de 19 juegos con la
        // barra inferior visible. No existe un hub paralelo.
        composable(RutasZazili.ARENA_CORTEX) {
            DisposableEffect(mainViewModel) {
                mainViewModel.abrirSesionActividad()
                onDispose { mainViewModel.cerrarSesionActividad() }
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    ModuleNavigationBar(navController = navController)
                }
            ) { relleno ->
                ArenaCortexScreen(
                    onPlayGame = { modulo, minijuegoId ->
                        mainViewModel.intentarAbrirActividad {
                            navController.navigate(Destinos.leccion(modulo, minijuegoId)) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    },
                    onVolverAlHogar = { navController.popBackStack() },
                    onAbrirJuegoTech = { ruta ->
                        mainViewModel.intentarAbrirActividad {
                            navController.navigate(ruta) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(relleno)
                )
            }
        }

        // Los tres minijuegos de tecnología anexados a la Arena. Cada destino
        // arranca en su nivel de tecnología (17, 18 y 19 del currículo).
        composable(RutasZazili.JUEGO_APLASTA_BUG) {
            RutaMinijuegoTech(
                lessonId = ArenaCortexRepository.PRIMER_NIVEL_TECH,
                onSalir = { VolverAlMapaTech(navController) },
                mainViewModel = mainViewModel
            )
        }

        composable(RutasZazili.JUEGO_RUTA_ALGORITMO) {
            RutaMinijuegoTech(
                lessonId = ArenaCortexRepository.PRIMER_NIVEL_TECH + 1,
                onSalir = { VolverAlMapaTech(navController) },
                mainViewModel = mainViewModel
            )
        }

        composable(RutasZazili.JUEGO_CIBER_SEGURO) {
            RutaMinijuegoTech(
                lessonId = ArenaCortexRepository.PRIMER_NIVEL_TECH + 2,
                onSalir = { VolverAlMapaTech(navController) },
                mainViewModel = mainViewModel
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
            DisposableEffect(mainViewModel) {
                mainViewModel.abrirSesionActividad()
                onDispose { mainViewModel.cerrarSesionActividad() }
            }
            val modulo = Destinos.moduloDesdeRuta(
                entrada.arguments?.getString(Destinos.ARG_MODULO)
            )
            val nodoId = entrada.arguments
                ?.getString(Destinos.ARG_NODO)
                ?.takeIf { it.isNotBlank() }
            RutaLeccion(
                modulo = modulo,
                nodoId = nodoId,
                navController = navController,
                mainViewModel = mainViewModel
            )
        }

        composable(
            route = RutasZazili.LECCION_MODULO,
            arguments = listOf(
                navArgument(RutasZazili.ARG_MODULE_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Destinos.ARG_NODO) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entrada ->
            val moduleId = entrada.arguments
                ?.getString(RutasZazili.ARG_MODULE_ID)
                ?.toIntOrNull()
            // El número se valida ANTES de crear el ViewModel: un enlace roto
            // nunca construye una lección fantasma, solo regresa al Tablero.
            val modulo = LessonEngine.moduloDeId(moduleId)
            val nodoId = entrada.arguments
                ?.getString(Destinos.ARG_NODO)
                ?.takeIf { it.isNotBlank() }
            if (modulo == null) {
                LaunchedEffect(moduleId) {
                    if (!navController.popBackStack()) {
                        navController.navigate(RutasZazili.TABLERO) { launchSingleTop = true }
                    }
                }
            } else {
                DisposableEffect(mainViewModel) {
                    mainViewModel.abrirSesionActividad()
                    onDispose { mainViewModel.cerrarSesionActividad() }
                }
                RutaLeccion(
                    modulo = modulo,
                    nodoId = nodoId,
                    navController = navController,
                    mainViewModel = mainViewModel
                )
            }
        }

        // Plática con Zazi: el ViewModel vive en el destino, así que al salir
        // se descarta la conversación y no se mezcla con la siguiente visita.
        composable(RutasZazili.CHAT) {
            ZaziChatScreen(onVolver = { navController.popBackStack() })
        }

        composable(Destinos.NIDO) {
            NidoPadresScreen(onVolver = { navController.popBackStack() })
        }
    }

    AvisoBloqueoJuego(
        bloqueo = bloqueo,
        onDescartar = mainViewModel::descartarBloqueo,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
    )
    }
}

// ---------------------------------------------------------------------------
//  CONTENIDO COMPARTIDO DE LOS MAPAS
// ---------------------------------------------------------------------------

/**
 * Contenido de una ruta `map/N` (y de la ruta legada): el Scaffold de la
 * pestaña con [ModuleNavigationBar] y el mapa del módulo. Las lecciones se
 * abren encima de este destino y, al cerrarlas, el niño vuelve a su mapa.
 */
@Composable
private fun ContenidoMapaModulo(
    moduleId: Int,
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    MainNavigation(
        moduleId = moduleId,
        navController = navController,
        onVolverAlHogar = { navController.popBackStack() },
        onAbrirNodo = { modulo, nodoId ->
            mainViewModel.intentarAbrirActividad {
                navController.navigate(Destinos.leccion(modulo, nodoId)) {
                    launchSingleTop = true
                    restoreState = false
                }
            }
        },
        onIrANido = {
            navController.navigate(Destinos.NIDO) { launchSingleTop = true }
        }
    )
}

// ---------------------------------------------------------------------------
//  RUTEO DE LECCIONES Y MINIJUEGOS
// ---------------------------------------------------------------------------

/**
 * Contenedor de un minijuego de tecnología. El motor se crea con la lección
 * del nodo, así que la dificultad y el desbloqueo siempre corresponden al
 * nivel que el niño acaba de tocar.
 */
@Composable
private fun RutaMinijuegoTech(
    lessonId: Int,
    onSalir: () -> Unit,
    mainViewModel: MainViewModel
) {
    val aplicacion = LocalContext.current.applicationContext as ZaziliApplication
    val motor: ArenaTechGameEngine = viewModel(
        factory = ArenaTechGameEngine.factory(aplicacion, lessonId)
    )

    DisposableEffect(mainViewModel) {
        mainViewModel.abrirSesionActividad()
        mainViewModel.abrirSesionDeJuego()
        onDispose {
            mainViewModel.cerrarSesionDeJuego()
            mainViewModel.cerrarSesionActividad()
        }
    }

    val salir: () -> Unit = {
        GlobalAudioManager.playPop()
        onSalir()
    }

    val relleno = Modifier
        .fillMaxSize()
        .statusBarsPadding()

    when (motor.juego) {
        TechGame.APLASTA_BUG -> AplastaElBugTechScreen(
            motor = motor,
            onSalir = salir,
            modifier = relleno
        )

        TechGame.RUTA_ALGORITMO -> RutaDelAlgoritmoTechScreen(
            motor = motor,
            onSalir = salir,
            modifier = relleno
        )

        TechGame.FILTRO_CIBER -> CyberSwipeScreen(
            motor = motor,
            onSalir = salir,
            modifier = relleno
        )
    }
}

/**
 * Regresa a la pantalla que abrió el minijuego; si la ruta llegó sin pantalla
 * previa en la pila (enlace directo), abre el hub de la Arena Cortex.
 */
private fun VolverAlMapaTech(navController: NavHostController) {
    if (navController.popBackStack()) return
    navController.navigate(RutasZazili.ARENA_CORTEX) {
        launchSingleTop = true
    }
}

@Composable
private fun RutaLeccion(
    modulo: ModuloZazili?,
    nodoId: String?,
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    if (modulo == null) {
        // Ruta corrupta o enlace viejo: se regresa a un destino real en lugar
        // de dejar la pantalla en blanco.
        LaunchedEffect(Unit) {
            if (!navController.popBackStack()) {
                navController.navigate(RutasZazili.TABLERO) { launchSingleTop = true }
            }
        }
        return
    }

    // Cronómetro real de juego: la energía se cobra por tiempo, no por nodo.
    DisposableEffect(mainViewModel) {
        mainViewModel.abrirSesionDeJuego()
        onDispose { mainViewModel.cerrarSesionDeJuego() }
    }

    if (modulo == ModuloZazili.CUEVA_LEYENDAS && nodoId == Minijuego.TlacuachePoeta.id) {
        RutaTlacuachePoeta(onSalir = { navController.popBackStack() })
        return
    }

    val salir: () -> Unit = { navController.popBackStack() }
    when (modulo) {
        ModuloZazili.CUEVA_LEYENDAS -> LanguageLessonScreen(
            modulo = modulo,
            nodoId = nodoId,
            onSalir = salir
        )

        ModuloZazili.ARBOL_ABRAZOS -> MentalHealthLessonScreen(
            modulo = modulo,
            nodoId = nodoId,
            onSalir = salir
        )

        ModuloZazili.LABORATORIO_CENOTE -> ScienceLessonScreen(
            modulo = modulo,
            nodoId = nodoId,
            onSalir = salir
        )

        else -> LessonScreen(
            modulo = modulo,
            nodoId = nodoId,
            onSalir = salir
        )
    }
}

@Composable
private fun RutaTlacuachePoeta(onSalir: () -> Unit) {
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

// ---------------------------------------------------------------------------
//  CARGA
// ---------------------------------------------------------------------------

@Composable
private fun PantallaCargaZazili(modifier: Modifier = Modifier) {
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
                text = "Preparando tu casita…",
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
