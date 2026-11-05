package com.example.zazili.presentation.minijuegos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCenoteIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.recursoKawaiiZazi
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.minijuegos.ModuloMinijuego
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.VerdeDuo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------
//  MODELO INMUTABLE
// ---------------------------------------------------------------------------

/**
 * Una tarjeta del Laboratorio del Cenote. Todos sus campos son `val` y la
 * anotación [Immutable] permite que Compose omita recomposiciones cuando la
 * tarjeta no cambió; la lista de niveles se construye una sola vez fuera del
 * hilo principal.
 */
@Immutable
data class ScienceGameCard(
    val id: String,
    val posicion: Int,
    val titulo: String,
    val concepto: String,
    val descripcion: String,
    val emoji: String,
    val color: Color,
    val niveles: List<String>
)

@Immutable
data class ScienceUiState(
    val isLoading: Boolean = true,
    val cards: List<ScienceGameCard> = emptyList(),
    val totalNiveles: Int = 0
)

/**
 * Construye el catálogo de Ciencias en [Dispatchers.Default]. La pantalla nunca
 * ordena, filtra ni arma listas: solo recibe estado inmutable listo para pintar.
 */
@Stable
class ScienceModuleViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScienceUiState())
    val state: StateFlow<ScienceUiState> = _state.asStateFlow()

    private var trabajo: Job? = null

    init {
        cargar()
    }

    fun recargar() {
        cargar()
    }

    private fun cargar() {
        trabajo?.cancel()
        _state.value = ScienceUiState(isLoading = true)
        trabajo = viewModelScope.launch {
            val tarjetas = withContext(Dispatchers.Default) {
                runCatching { construirTarjetas() }.getOrDefault(emptyList())
            }
            _state.value = ScienceUiState(
                isLoading = false,
                cards = tarjetas,
                totalNiveles = tarjetas.sumOf { it.niveles.size }
            )
        }
    }

    override fun onCleared() {
        trabajo?.cancel()
        trabajo = null
        super.onCleared()
    }

    private fun construirTarjetas(): List<ScienceGameCard> =
        Minijuego.porModulo(ModuloMinijuego.Ciencias).mapIndexed { indice, juego ->
            val meta = metaDe(juego)
            ScienceGameCard(
                id = juego.id,
                posicion = indice + 1,
                titulo = meta.titulo,
                concepto = meta.concepto,
                descripcion = meta.descripcion,
                emoji = meta.emoji,
                color = meta.color,
                niveles = juego.niveles.map { it.consigna }
            )
        }

    private fun metaDe(juego: Minijuego): MetaCientifica = when (juego.id) {
        Minijuego.AlquimiaCenote.id -> MetaCientifica(
            titulo = "Alquimia del Cenote",
            concepto = "Estados de la Materia",
            descripcion = "Mezcla hielo, calor y vapor para descubrir cómo cambia el agua.",
            emoji = "🧪",
            color = CoralAviso
        )

        Minijuego.CadenasAlimenticias.id -> MetaCientifica(
            titulo = "Cadenas Alimenticias",
            concepto = "Ecosistemas",
            descripcion = "Conecta al sol, las plantas y los animales en su red de comida.",
            emoji = "🌿",
            color = VerdeDuo
        )

        Minijuego.FiltroMagico.id -> MetaCientifica(
            titulo = "El Filtro Mágico",
            concepto = "Causa y Efecto",
            descripcion = "Apila grava, arena y algodón para dejar el agua limpiecita.",
            emoji = "💧",
            color = AzulCielo
        )

        Minijuego.MonstruoBasura.id -> MetaCientifica(
            titulo = "El Monstruo de Basura",
            concepto = "Clasificación",
            descripcion = "Atrapa la basura que cae y sepárala en su bote correcto.",
            emoji = "🗑️",
            color = AmarilloVibrante
        )

        else -> MetaCientifica(
            titulo = juego.titulo,
            concepto = "Ciencias",
            descripcion = "Un experimento del Laboratorio del Cenote.",
            emoji = "🔬",
            color = MintGreen
        )
    }

    private data class MetaCientifica(
        val titulo: String,
        val concepto: String,
        val descripcion: String,
        val emoji: String,
        val color: Color
    )
}

// ---------------------------------------------------------------------------
//  PANTALLA
// ---------------------------------------------------------------------------

/**
 * Hub del módulo de Ciencias. Usa una [LazyColumn] con `key` y `contentType`
 * para reciclar tarjetas sin recomponer las que no cambiaron. El botón flotante
 * de regreso al inicio nace de un [derivedStateOf] sobre el scroll, no de una
 * variable que se actualice en cada frame.
 */
@Composable
fun ScienceModuleScreen(
    onPlayGame: (String) -> Unit,
    modifier: Modifier = Modifier,
    onVolverAlHogar: () -> Unit = {}
) {
    val viewModel: ScienceModuleViewModel = viewModel()
    val estado by viewModel.state.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    val lista = rememberLazyListState()
    val alcance = rememberCoroutineScope()
    val hayQueSubir by remember { derivedStateOf { lista.firstVisibleItemIndex > 1 } }

    val fondoBase = MaterialTheme.colorScheme.background
    val fondo = if (zen) {
        Brush.verticalGradient(listOf(fondoBase, fondoBase))
    } else {
        Brush.verticalGradient(listOf(fondoBase, AzulCielo.copy(alpha = 0.20f)))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        when {
            estado.isLoading -> PantallaCargandoCiencias()

            estado.cards.isEmpty() -> PantallaSinCiencias(
                onReintentar = {
                    GlobalAudioManager.playPop()
                    viewModel.recargar()
                },
                onVolverAlHogar = {
                    GlobalAudioManager.playPop()
                    onVolverAlHogar()
                }
            )

            else -> LazyColumn(
                state = lista,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "encabezado_laboratorio", contentType = "encabezado") {
                    EncabezadoCiencias(
                        totalJuegos = estado.cards.size,
                        totalNiveles = estado.totalNiveles,
                        onVolverAlHogar = {
                            GlobalAudioManager.playPop()
                            onVolverAlHogar()
                        }
                    )
                }

                item(key = "titulo_experimentos", contentType = "titulo") {
                    Text(
                        text = "Elige tu experimento",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                }

                items(
                    items = estado.cards,
                    key = { tarjeta -> tarjeta.id },
                    contentType = { "tarjeta_ciencia" }
                ) { tarjeta ->
                    MinigameCard(
                        card = tarjeta,
                        onPlayGame = onPlayGame
                    )
                }

                item(key = "pie_laboratorio", contentType = "pie") {
                    Text(
                        text = "Puedes repetir cada juego cuantas veces quieras. Sin relojes de castigo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = hayQueSubir,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
        ) {
            ZaziButton(
                text = "Subir al inicio",
                onClick = {
                    GlobalAudioManager.playPop()
                    alcance.launch { lista.animateScrollToItem(0) }
                },
                modifier = Modifier.size(50.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                cornerRadius = 25.dp,
                baseHeight = 4.dp
            ) {
                KawaiiArrowIcon(
                    modifier = Modifier.graphicsLayer { rotationZ = -90f },
                    size = 18.dp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  ENCABEZADO CON COIL
// ---------------------------------------------------------------------------

@Composable
private fun EncabezadoCiencias(
    totalJuegos: Int,
    totalNiveles: Int,
    onVolverAlHogar: () -> Unit
) {
    val contexto = LocalContext.current
    val arte = remember(contexto) { recursoKawaiiZazi(contexto) }
    val tieneArtePropio = remember(contexto) {
        contexto.resources.getIdentifier(
            "zazi_kawaii_placeholder",
            "drawable",
            contexto.packageName
        ) != 0
    }
    val respaldo = painterResource(arte)
    val solicitud = remember(contexto, arte) {
        ImageRequest.Builder(contexto)
            .data(arte)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZaziButton(
                text = "Volver a la casita",
                onClick = onVolverAlHogar,
                modifier = Modifier.size(44.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                cornerRadius = 22.dp,
                baseHeight = 3.dp
            ) {
                KawaiiArrowIcon(
                    modifier = Modifier.graphicsLayer { rotationZ = 180f },
                    size = 18.dp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "El Laboratorio del Cenote",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ciencias naturales · elige tu experimento",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            if (tieneArtePropio) {
                // Coil decodifica fuera del hilo principal y guarda el
                // resultado en la caché de memoria; la UI nunca se bloquea.
                AsyncImage(
                    model = solicitud,
                    contentDescription = "Zazi te da la bienvenida al laboratorio",
                    placeholder = respaldo,
                    error = respaldo,
                    fallback = respaldo,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(84.dp)
                )
            } else {
                // Mientras el ilustrador entrega el arte, el ícono kawaii del
                // cenote evita mostrar el ícono de galería del sistema.
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MintGreen.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center
                ) {
                    KawaiiCenoteIcon(size = 54.dp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResumenPill(texto = "$totalJuegos juegos", color = AzulCielo)
            ResumenPill(texto = "$totalNiveles niveles", color = MintGreen)
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Toca «¡Jugar!» para entrar. Cada juego tiene cinco niveles y puedes repetirlo sin prisa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
        )
    }
}

@Composable
private fun ResumenPill(texto: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.26f)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

// ---------------------------------------------------------------------------
//  TARJETA DE MINIJUEGO
// ---------------------------------------------------------------------------

@Composable
fun MinigameCard(
    card: ScienceGameCard,
    onPlayGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(card.color.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = card.emoji, fontSize = 30.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Juego ${card.posicion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = card.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.concepto,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = card.color.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "N1–N5",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = card.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { indice ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(card.color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${indice + 1}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (indice < 4) Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Nivel 1: ${card.niveles.firstOrNull().orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))

            ZaziButton(
                text = "¡Jugar!",
                onClick = {
                    GlobalAudioManager.playPop()
                    onPlayGame(card.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                containerColor = card.color,
                contentColor = contenidoDe(card.color),
                cornerRadius = 20.dp,
                baseHeight = 5.dp,
                fontSize = 18.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  ESTADOS SEGUROS
// ---------------------------------------------------------------------------

@Composable
private fun PantallaCargandoCiencias() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        CircularProgressIndicator(color = VerdeDuo)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Zazi prepara los experimentos...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.70f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PantallaSinCiencias(
    onReintentar: () -> Unit,
    onVolverAlHogar: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            text = "El laboratorio está cerrando por un momento.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Zazi no encontró los experimentos. No pasa nada: intenta otra vez.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        ZaziButton(
            text = "Reintentar",
            onClick = onReintentar,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            containerColor = VerdeDuo,
            contentColor = contenidoDe(VerdeDuo),
            cornerRadius = 20.dp,
            baseHeight = 5.dp,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(10.dp))
        ZaziButton(
            text = "Volver a la casita",
            onClick = onVolverAlHogar,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 20.dp,
            baseHeight = 4.dp,
            fontSize = 17.sp
        )
    }
}
