package com.example.zazili.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiBoltIcon
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.lecciones.LessonEngine
import com.example.zazili.presentation.modulos.comun.IconoModulo

/**
 * Destino de la barra inferior de Zazili 2.0.
 *
 * Cada elemento lleva su propia [route] para que el `onClick` sea idéntico en
 * las seis pestañas: la barra nunca decide rutas por su cuenta. [icon] es un
 * código estable de icono, no un `drawable`: los módulos se dibujan con Canvas
 * y la Arena usa el rayo kawaii. El valor coincide con el id interno del
 * módulo (Matemáticas 1, Lenguaje 2, Ciencias 3, Tecnología 4, Emociones 5) y
 * `0` identifica a la Arena Cortex.
 *
 * Los números de ruta son los de la barra pública: `map/4` es Emociones y
 * `map/5` es Tecnología, aunque sus llaves internas de progreso sigan siendo
 * 5 y 4. La traducción vive en [moduloInternoDePestana] para no romper datos
 * guardados.
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: Int
) {
    data object Matematicas : BottomNavItem(
        route = RutasZazili.MAPA_PESTANA_MATEMATICAS,
        title = "Matemáticas",
        icon = LessonEngine.MODULO_MATEMATICAS_ID
    )

    data object Lenguaje : BottomNavItem(
        route = RutasZazili.MAPA_PESTANA_LENGUAJE,
        title = "Lenguaje",
        icon = LessonEngine.MODULO_LENGUAJE_ID
    )

    data object Ciencias : BottomNavItem(
        route = RutasZazili.MAPA_PESTANA_CIENCIAS,
        title = "Ciencias",
        icon = LessonEngine.MODULO_CIENCIAS_ID
    )

    data object Emociones : BottomNavItem(
        route = RutasZazili.MAPA_PESTANA_EMOCIONES,
        title = "Emociones",
        icon = LessonEngine.MODULO_EMOCIONES_ID
    )

    data object Tecnologia : BottomNavItem(
        route = RutasZazili.MAPA_PESTANA_TECNOLOGIA,
        title = "Tecnología",
        icon = LessonEngine.MODULO_TECNOLOGIA_ID
    )

    data object ArenaCortex : BottomNavItem(
        route = RutasZazili.ARENA_CORTEX,
        title = "Arena Cortex",
        icon = ICONO_ARENA
    )

    companion object {
        const val ICONO_ARENA = 0

        /**
         * Se construye bajo demanda (getter) para que el `<clinit>` de la
         * clase sellada nunca lea `INSTANCE` de un objeto hijo a medio iniciar.
         */
        val todos: List<BottomNavItem>
            get() = listOf(
                Matematicas,
                Lenguaje,
                Ciencias,
                Emociones,
                Tecnologia,
                ArenaCortex
            )
    }
}

/**
 * Traduce el número público de pestaña al id interno del módulo.
 *
 * Emociones conserva la llave 5 y Tecnología la llave 4: sus avances ya están
 * guardados con esos números. Solo cambia la ruta visible de la barra.
 */
internal fun moduloInternoDePestana(pestana: Int): Int = when (pestana) {
    1 -> LessonEngine.MODULO_MATEMATICAS_ID
    2 -> LessonEngine.MODULO_LENGUAJE_ID
    3 -> LessonEngine.MODULO_CIENCIAS_ID
    4 -> LessonEngine.MODULO_EMOCIONES_ID
    5 -> LessonEngine.MODULO_TECNOLOGIA_ID
    else -> LessonEngine.MODULO_MATEMATICAS_ID
}

/** Id interno que corresponde a una ruta `map/N`, o `null` si no es de mapa. */
private fun idInternoDeRuta(ruta: String?): Int? = when (ruta) {
    RutasZazili.MAPA_PESTANA_MATEMATICAS -> LessonEngine.MODULO_MATEMATICAS_ID
    RutasZazili.MAPA_PESTANA_LENGUAJE -> LessonEngine.MODULO_LENGUAJE_ID
    RutasZazili.MAPA_PESTANA_CIENCIAS -> LessonEngine.MODULO_CIENCIAS_ID
    RutasZazili.MAPA_PESTANA_EMOCIONES -> LessonEngine.MODULO_EMOCIONES_ID
    RutasZazili.MAPA_PESTANA_TECNOLOGIA -> LessonEngine.MODULO_TECNOLOGIA_ID
    else -> null
}

/**
 * Barra inferior estricta de seis destinos.
 *
 * Todas las pestañas ejecutan EXACTAMENTE el mismo `navigate(item.route)` con
 * `popUpTo` al inicio del grafo, `launchSingleTop` y `restoreState`. No hay
 * ramas por pestaña: si una ruta falta en [ZaziliNavGraph], la barra falla en
 * vez de fallar en silencio.
 *
 * El resaltado se deduce del `back stack` (rutas nuevas y la ruta legada
 * `module_map_route/{moduleId}`), con [moduloRespaldo] como respaldo para el
 * primer frame.
 */
@Composable
fun ModuleNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    moduloRespaldo: Int? = null
) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    val moduloActivo = remember(entradaActual, rutaActual) {
        if (rutaActual == RutasZazili.MODULO_MAPA) {
            entradaActual?.arguments?.getInt(RutasZazili.ARG_MODULE_ID)
        } else {
            idInternoDeRuta(rutaActual)
        }
    } ?: moduloRespaldo

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            BottomNavItem.todos.forEach { item ->
                val seleccionada = if (item == BottomNavItem.ArenaCortex) {
                    rutaActual == RutasZazili.ARENA_CORTEX ||
                        rutaActual == RutasZazili.ARENA
                } else {
                    moduloActivo == item.icon
                }

                NavigationBarItem(
                    selected = seleccionada,
                    onClick = {
                        GlobalAudioManager.playPop()
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { IconoPestana(codigo = item.icon) },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = if (seleccionada) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.SemiBold
                            },
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun IconoPestana(codigo: Int) {
    val modulo = when (codigo) {
        LessonEngine.MODULO_MATEMATICAS_ID -> ModuloZazili.TIANGUIS_NUMERICO
        LessonEngine.MODULO_LENGUAJE_ID -> ModuloZazili.CUEVA_LEYENDAS
        LessonEngine.MODULO_CIENCIAS_ID -> ModuloZazili.LABORATORIO_CENOTE
        LessonEngine.MODULO_TECNOLOGIA_ID -> ModuloZazili.PIRAMIDE_CODIGO
        LessonEngine.MODULO_EMOCIONES_ID -> ModuloZazili.ARBOL_ABRAZOS
        else -> null
    }
    if (modulo == null) {
        KawaiiBoltIcon(size = 24.dp)
    } else {
        IconoModulo(modulo, 24.dp)
    }
}
