package com.example.zazili.presentation.arena

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.navigation.RutasZazili
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.arena.ArenaCortexRepository
import com.example.zazili.domain.arena.DynamicDifficultyEngine
import com.example.zazili.domain.arena.FichaArena
import com.example.zazili.domain.arena.ProgresoDificultad
import com.example.zazili.domain.arena.SeccionArena
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.minijuegos.ModuloMinijuego
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.presentation.arena.matematicas.JuegoMatematico
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Punto de entrada de la pestaña Arena Cortex.
 *
 * Delega en [ArenaCortexRouter], que alterna entre este menú y el minijuego
 * exacto que se tocó. La firma se conserva para que la navegación padre siga
 * abriendo la Arena igual que antes.
 *
 * [onAbrirJuegoTech] recibe la ruta de uno de los tres minijuegos de
 * tecnología que se anexan al final del menú. Los 16 juegos originales siguen
 * saliendo por [onPlayGame], sin cambios.
 */
@Composable
fun ArenaCortexScreen(
    onPlayGame: (ModuloZazili, String) -> Unit,
    onVolverAlHogar: () -> Unit,
    modifier: Modifier = Modifier,
    motor: DynamicDifficultyEngine? = null,
    onAbrirJuegoTech: (String) -> Unit = {}
) {
    ArenaCortexRouter(
        onPlayGame = onPlayGame,
        onVolverAlHogar = onVolverAlHogar,
        modifier = modifier,
        motor = motor,
        onAbrirJuegoTech = onAbrirJuegoTech
    )
}

/**
 * Menú de la Arena Cortex: los 16 minijuegos originales en cuatro carruseles
 * por materia y, al final, los tres retos de tecnología.
 *
 * Cada tarjeta lee su nivel actual del [DynamicDifficultyEngine] y lo pinta
 * como 1 a 5 estrellas. Al tocar "¡Jugar!" se resuelve el id contra
 * [JuegoMatematico]: los cuatro juegos de Matemáticas se abren dentro de la
 * pestaña con su composable propio; las otras materias siguen saliendo por
 * [onPlayGame]. No hay reloj de hub ni castigos por explorar.
 */
@Composable
internal fun MenuArenaCortex(
    onAbrirJuegoMatematico: (JuegoMatematico) -> Unit,
    onPlayGame: (ModuloZazili, String) -> Unit,
    onVolverAlHogar: () -> Unit,
    modifier: Modifier = Modifier,
    motor: DynamicDifficultyEngine? = null,
    onAbrirJuegoTech: (String) -> Unit = {}
) {
    val contexto = LocalContext.current
    val motorArena = remember(contexto, motor) {
        motor ?: (contexto.applicationContext as ZaziliApplication)
            .container
            .dynamicDifficultyEngine
    }
    val estadoArena by motorArena.estado.collectAsStateWithLifecycle()
    val secciones = remember { ArenaCortexRepository.secciones }
    val estadoLista = rememberLazyListState()

    LazyColumn(
        state = estadoLista,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "arena_encabezado", contentType = "encabezado") {
            EncabezadoArena(
                nivelPromedio = estadoArena.nivelPromedio,
                onVolverAlHogar = {
                    GlobalAudioManager.playPop()
                    onVolverAlHogar()
                }
            )
        }

        item(key = "arena_aviso", contentType = "aviso") {
            AvisoArena()
        }

        secciones.forEach { seccion ->
            item(
                key = "arena_seccion_${seccion.modulo.titulo}",
                contentType = "seccion"
            ) {
                CabeceraSeccionArena(seccion = seccion)
            }

            item(
                key = "arena_carrusel_${seccion.modulo.titulo}",
                contentType = "carrusel"
            ) {
                CarruselJuegos(
                    seccion = seccion,
                    progresos = estadoArena.progresos,
                    onJugar = { ficha ->
                        GlobalAudioManager.playPop()
                        // Los cuatro juegos de Matemáticas abren su pantalla
                        // dentro de la Arena; el resto mantiene la ruta padre.
                        val juego = JuegoMatematico.desdeId(ficha.id)
                        if (juego != null) {
                            onAbrirJuegoMatematico(juego)
                        } else {
                            onPlayGame(ficha.modulo.moduloZazili, ficha.id)
                        }
                    }
                )
            }
        }

        // Los tres minijuegos de tecnología van al final del menú, después de
        // los cuatro carruseles originales. Se tocan una sola vez al entrar.
        item(key = "arena_tech_cabecera", contentType = "seccion") {
            CabeceraTech()
        }

        item(key = "arena_tech_carrusel", contentType = "carrusel") {
            CarruselTech(onAbrir = onAbrirJuegoTech)
        }

        item(key = "arena_pie", contentType = "pie") {
            PieArena()
        }
    }
}

// ---------------------------------------------------------------------------
//  TECNOLOGÍA: LOS TRES JUEGOS ANEXADOS
//  Se dibujan después de los cuatro carruseles originales. Ninguna tarjeta,
//  consulta de progreso ni clic de los 16 juegos existentes cambia.
// ---------------------------------------------------------------------------

/** Ficha propia de los juegos de tecnología; no vive en el catálogo de 16. */
private data class FichaTech(
    val titulo: String,
    val emoji: String,
    val gancho: String,
    val descripcion: String,
    val ruta: String,
    val acento: Color
)

private val FICHAS_TECH: List<FichaTech> = listOf(
    FichaTech(
        titulo = "Aplasta el Bug",
        emoji = "🐞",
        gancho = "Reflejos contra el sistema",
        descripcion = "Aplasta los bichos y esquiva los antivirus. El tablero se acelera mientras más aciertas.",
        ruta = RutasZazili.JUEGO_APLASTA_BUG,
        acento = VerdeDuo
    ),
    FichaTech(
        titulo = "Ruta del Algoritmo",
        emoji = "🧭",
        gancho = "Programa el camino de Zazi",
        descripcion = "Arrastra flechas para armar la secuencia y lleva a Zazi hasta la batería sin tocar firewalls.",
        ruta = RutasZazili.JUEGO_RUTA_ALGORITMO,
        acento = AzulCielo
    ),
    FichaTech(
        titulo = "Filtro Ciber-Seguro",
        emoji = "🛡️",
        gancho = "Desliza y decide",
        descripcion = "Bloquea los mensajes peligrosos y acepta los seguros. Cada error te explica el porqué.",
        ruta = RutasZazili.JUEGO_CIBER_SEGURO,
        acento = MagicalPurple
    )
)

@Composable
private fun CabeceraTech() {
    val oscuro = isSystemInDarkTheme()
    val acento = MagicalPurple
    val fondo = acento.copy(alpha = if (oscuro) 0.24f else 0.15f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(fondo)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(acento),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💻", fontSize = 22.sp)
        }

        Spacer(Modifier.width(11.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Tecnología rápida",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Reflejos, lógica y ciberseguridad",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        ) {
            Text(
                text = "3 juegos",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun CarruselTech(onAbrir: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = FICHAS_TECH,
            key = { ficha -> ficha.ruta },
            contentType = { "arena_tech_tarjeta" }
        ) { ficha ->
            TarjetaTech(
                ficha = ficha,
                onJugar = { onAbrir(ficha.ruta) }
            )
        }
    }
}

@Composable
private fun TarjetaTech(
    ficha: FichaTech,
    onJugar: () -> Unit
) {
    val oscuro = isSystemInDarkTheme()
    val tinte = ficha.acento.copy(alpha = if (oscuro) 0.20f else 0.10f)

    Surface(
        modifier = Modifier.width(266.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.5.dp,
            color = ficha.acento.copy(alpha = if (oscuro) 0.44f else 0.34f)
        ),
        shadowElevation = if (oscuro) 0.dp else 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tinte)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ficha.acento),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ficha.emoji, fontSize = 23.sp)
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = ficha.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ficha.gancho,
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AmarilloVibrante
                ) {
                    Text(
                        text = "NUEVO",
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = UniversoProfundo
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = ficha.descripcion,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tecnología",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Dificultad adaptativa",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ficha.acento
                )
            }

            Spacer(Modifier.height(12.dp))

            ZaziButton(
                text = "¡Jugar!",
                onClick = onJugar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                containerColor = ficha.acento,
                contentColor = contenidoDe(ficha.acento),
                cornerRadius = 16.dp,
                baseHeight = 4.dp,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ---------------------------------------------------------------------------
//  ENCABEZADO
// ---------------------------------------------------------------------------

@Composable
private fun EncabezadoArena(
    nivelPromedio: Int,
    onVolverAlHogar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(UniversoProfundo, MagicalPurple, UniversoMedio)
                )
            )
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ZaziButton(
                    text = "Volver a la casita",
                    onClick = onVolverAlHogar,
                    modifier = Modifier.size(44.dp),
                    containerColor = ZaziliWhite.copy(alpha = 0.16f),
                    contentColor = ZaziliWhite,
                    cornerRadius = 22.dp,
                    baseHeight = 3.dp
                ) {
                    KawaiiArrowIcon(
                        modifier = Modifier.graphicsLayer { rotationZ = 180f },
                        size = 18.dp,
                        color = ZaziliWhite
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Arena Cortex",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ZaziliWhite,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "¡Bienvenido a la Arena Cortex!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ZaziliWhite.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "⭐ $nivelPromedio/5",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = ZaziliWhite
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Aquí entrenas el cerebro jugando. Tres victorias seguidas suben un nivel; dos tropiezos te dan aire. ¡Ponte trucha!",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 17.sp,
                        color = ZaziliWhite.copy(alpha = 0.88f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PastillaArena("🎮 19 juegos")
                        PastillaArena("🧠 4 cerebros")
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PastillaArena("⭐ Niveles 1 a 5")
                        PastillaArena("🌱 A tu ritmo")
                    }
                }

                Spacer(Modifier.width(4.dp))

                ZaziSpriteAnimado(
                    size = 104.dp,
                    estado = ZaziState.PENSANDO,
                    descripcion = "Zazi te invita a la Arena Cortex"
                )
            }
        }
    }
}

@Composable
private fun PastillaArena(texto: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = ZaziliWhite.copy(alpha = 0.14f)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ZaziliWhite
        )
    }
}

@Composable
private fun AvisoArena() {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(text = "✨", fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Desliza cada carrusel y toca ¡Jugar! Las estrellas muestran tu nivel de hoy: se mueven solas con cada partida.",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f)
        )
    }
}

// ---------------------------------------------------------------------------
//  SECCIÓN Y CARRUSEL
// ---------------------------------------------------------------------------

@Composable
private fun CabeceraSeccionArena(seccion: SeccionArena) {
    val oscuro = isSystemInDarkTheme()
    val acento = colorDeSeccion(seccion.modulo)
    val fondo = acento.copy(alpha = if (oscuro) 0.24f else 0.15f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(fondo)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(acento),
            contentAlignment = Alignment.Center
        ) {
            Text(text = seccion.emoji, fontSize = 22.sp)
        }

        Spacer(Modifier.width(11.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = seccion.modulo.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = seccion.alias,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        ) {
            Text(
                text = "${seccion.totalJuegos} juegos",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun CarruselJuegos(
    seccion: SeccionArena,
    progresos: Map<String, ProgresoDificultad>,
    onJugar: (FichaArena) -> Unit
) {
    val acento = colorDeSeccion(seccion.modulo)

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = seccion.fichas,
            key = { ficha -> ficha.id },
            contentType = { "arena_tarjeta" }
        ) { ficha ->
            val progreso = progresos[ficha.id] ?: ProgresoDificultad(ficha.id)
            TarjetaArena(
                ficha = ficha,
                progreso = progreso,
                acento = acento,
                onJugar = { onJugar(ficha) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  TARJETA DE JUEGO
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaArena(
    ficha: FichaArena,
    progreso: ProgresoDificultad,
    acento: Color,
    onJugar: () -> Unit
) {
    val oscuro = isSystemInDarkTheme()
    val tinte = acento.copy(alpha = if (oscuro) 0.20f else 0.10f)
    val nivel = progreso.estrellas

    Surface(
        modifier = Modifier.width(266.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.5.dp,
            color = acento.copy(alpha = if (oscuro) 0.44f else 0.34f)
        ),
        shadowElevation = if (oscuro) 0.dp else 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tinte)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(acento),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = ficha.emoji, fontSize = 23.sp)
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = ficha.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ficha.gancho,
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (ficha.esJefe) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AmarilloVibrante
                    ) {
                        Text(
                            text = "JEFE",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = UniversoProfundo
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = ficha.descripcion,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilaEstrellas(nivel = nivel, color = acento)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Nivel $nivel de 5",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
            }

            Spacer(Modifier.height(12.dp))

            ZaziButton(
                text = "¡Jugar!",
                onClick = onJugar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                containerColor = acento,
                contentColor = contenidoDe(acento),
                cornerRadius = 16.dp,
                baseHeight = 4.dp,
                fontSize = 16.sp
            )

            // La base 3D del botón sobresale 4 dp: este aire evita que la
            // tarjeta recorte el relieve.
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FilaEstrellas(nivel: Int, color: Color) {
    val seguro = nivel.coerceIn(Minijuego.NIVEL_MINIMO, Minijuego.NIVEL_MAXIMO)

    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.semantics {
            contentDescription = "Nivel $seguro de 5 estrellas"
        }
    ) {
        repeat(Minijuego.NIVEL_MAXIMO) { indice ->
            Text(
                text = if (indice < seguro) "★" else "☆",
                fontSize = 17.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (indice < seguro) {
                    color
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                }
            )
        }
    }
}

@Composable
private fun PieArena() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Cada juego se adapta a ti: ni demasiado fácil, ni demasiado difícil.",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.60f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Si algo se siente pesado, puedes salir y volver cuando quieras.",
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
            textAlign = TextAlign.Center
        )
    }
}

private fun colorDeSeccion(modulo: ModuloMinijuego): Color = when (modulo) {
    ModuloMinijuego.Matematicas -> VerdeDuo
    ModuloMinijuego.Lenguaje -> MagicalPurple
    ModuloMinijuego.Emociones -> AmarilloVibrante
    ModuloMinijuego.Ciencias -> AzulCielo
}
