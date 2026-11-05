package com.example.zazili.presentation.modulos

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.MundoSaga
import com.example.zazili.domain.saga.NodoSaga
import com.example.zazili.domain.saga.RutaSaga
import com.example.zazili.presentation.modulos.comun.IconoJefe
import com.example.zazili.presentation.modulos.comun.IconoModulo
import com.example.zazili.presentation.modulos.comun.colorDeModulo
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.presentation.modulos.comun.fondoSuaveModulo
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.launch

/**
 * Hub de un mundo de la Saga: portada, jefe, retos y el diálogo de pregunta.
 * Cada reto se resuelve sin cronómetro y con pista disponible desde el primer
 * intento. El botón de volver queda siempre a la mano.
 */
@Composable
fun ModuloHubScreen(
    modulo: ModuloZazili,
    nodoInicial: String?,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModuloHubViewModel = viewModel()
) {
    val mundo = RutaSaga.mundoDe(modulo)
    if (mundo == null) {
        ModuloNoDisponible(onVolver = onVolver, modifier = modifier)
        return
    }

    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val progreso by viewModel.progreso.collectAsStateWithLifecycle()
    val zazoles by viewModel.zazoles.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    val fondo = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var nodoAbierto by remember { mutableStateOf<NodoSaga?>(null) }
    var autoAbierto by rememberSaveable(nodoInicial) { mutableStateOf(false) }

    fun avisar(texto: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texto)
        }
    }

    LaunchedEffect(nodoInicial, estado, autoAbierto) {
        if (autoAbierto || nodoInicial.isNullOrBlank()) return@LaunchedEffect
        val nodo = mundo.nodos.firstOrNull { it.id == nodoInicial } ?: return@LaunchedEffect
        autoAbierto = true
        if (RutaSaga.desbloqueado(mundo, nodo.id, estado)) {
            nodoAbierto = nodo
        } else {
            avisar("Ese reto todavía duerme. Empieza por el primero de este mundo, ¿va?")
        }
    }

    val completados = estado.completadosEn(mundo)
    val estrellasMundo = mundo.nodos.sumOf { estado.estrellasDe(it.id) }
    val colorModulo = colorDeModulo(modulo)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraMundo(
                mundo = mundo,
                estrellas = estrellasMundo,
                maxEstrellas = mundo.nodos.size * 3,
                onVolver = onVolver
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                PortadaMundo(
                    mundo = mundo,
                    completados = completados,
                    estrellas = estrellasMundo,
                    nivel = progreso.level,
                    zazoles = zazoles
                )

                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Los retos del mundo",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "$completados/${mundo.nodos.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorModulo,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Una pregunta a la vez. Si fallas, te damos una pista y otra oportunidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
                )
                Spacer(Modifier.height(12.dp))

                mundo.nodos.forEachIndexed { indice, nodo ->
                    val completado = estado.completado(nodo.id)
                    val desbloqueado = RutaSaga.desbloqueado(mundo, nodo.id, estado)
                    val esSiguiente = desbloqueado && !completado
                    TarjetaReto(
                        mundo = mundo,
                        nodo = nodo,
                        numero = indice + 1,
                        completado = completado,
                        desbloqueado = desbloqueado,
                        esSiguiente = esSiguiente,
                        estrellas = estado.estrellasDe(nodo.id),
                        onClick = {
                            if (desbloqueado) {
                                GlobalAudioManager.playPop()
                                nodoAbierto = nodo
                            } else {
                                val anterior = mundo.nodos.getOrNull(indice - 1)
                                GlobalAudioManager.playPop()
                                avisar(
                                    if (anterior != null) {
                                        "Primero termina «${anterior.nombre}». Paso a pasito."
                                    } else {
                                        "Este reto abre en un ratito."
                                    }
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (completados >= mundo.nodos.size) {
                        "¡Mundo completado! Puedes repasar cuando quieras; las estrellas ya son tuyas."
                    } else if (nodoAbierto == null) {
                        "Toca un reto para empezar. No hay reloj ni penalización."
                    } else {
                        "Zazi está orgullosa de que lo intentes."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) { data ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = data.visuals.message,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    nodoAbierto?.let { nodo ->
        DialogoReto(
            mundo = mundo,
            nodo = nodo,
            onCerrar = { nodoAbierto = null },
            alResponder = { indice ->
                viewModel.responder(modulo, nodo, indice)
            }
        )
    }
}

@Composable
private fun BarraMundo(
    mundo: MundoSaga,
    estrellas: Int,
    maxEstrellas: Int,
    onVolver: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ZaziButton(
            text = "Volver a la Saga",
            onClick = {
                GlobalAudioManager.playPop()
                onVolver()
            },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 24.dp,
            baseHeight = 4.dp
        ) {
            KawaiiArrowIcon(
                modifier = Modifier.graphicsLayer { rotationZ = 180f },
                size = 20.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "MUNDO ${RutaSaga.mundos.indexOf(mundo) + 1} DE ${RutaSaga.mundos.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = mundo.modulo.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(shape = RoundedCornerShape(50), color = AmarilloSuave) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                KawaiiStarIcon(size = 16.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$estrellas/$maxEstrellas",
                    style = MaterialTheme.typography.labelLarge,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PortadaMundo(
    mundo: MundoSaga,
    completados: Int,
    estrellas: Int,
    nivel: Int,
    zazoles: Int
) {
    val color = colorDeModulo(mundo.modulo)
    val contenido = contenidoDe(color)
    val fraccion by animateFloatAsState(
        targetValue = completados.toFloat() / mundo.nodos.size,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "avanceMundo"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 260.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
        )
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    IconoModulo(mundo.modulo, 42.dp)
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MUNDO DE APRENDIZAJE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = mundo.modulo.titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = mundo.modulo.lema,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    IconoJefe(mundo.modulo, 28.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jefe final: ${mundo.jefe}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "«${mundo.dichoDelJefe}»",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$completados de ${mundo.nodos.size} retos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                KawaiiStarIcon(size = 14.dp)
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "$estrellas/${mundo.nodos.size * 3}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraccion)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaSuave {
                    KawaiiStarIcon(size = 15.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Nivel $nivel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                EtiquetaSuave {
                    KawaiiCoinIcon(size = 16.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$zazoles zazoles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconoModulo(mundo.modulo, 22.dp)
                }
            }
        }
    }
}

@Composable
private fun EtiquetaSuave(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun TarjetaReto(
    mundo: MundoSaga,
    nodo: NodoSaga,
    numero: Int,
    completado: Boolean,
    desbloqueado: Boolean,
    esSiguiente: Boolean,
    estrellas: Int,
    onClick: () -> Unit
) {
    val color = colorDeModulo(mundo.modulo)
    val contenido = contenidoDe(color)
    val fondo = if (esSiguiente) {
        fondoSuaveModulo(mundo.modulo)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = fondo,
        shadowElevation = if (esSiguiente) 3.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            completado -> VerdeDuo
                            desbloqueado -> AmarilloVibrante
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    completado -> KawaiiCheckIcon(size = 22.dp)
                    !desbloqueado -> KawaiiLockIcon(
                        size = 20.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    else -> Text(
                        text = "$numero",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = UniversoProfundo
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (nodo.esJefe) "RETO $numero · JEFE FINAL" else "RETO $numero",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (esSiguiente) color else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = nodo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (completado) {
                        repeat(3) { indice ->
                            KawaiiStarIcon(
                                size = 13.dp,
                                color = if (indice < estrellas) AmarilloVibrante else LavandaClara
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Completado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    } else if (esSiguiente) {
                        Text(
                            text = "¡Te toca!",
                            style = MaterialTheme.typography.labelSmall,
                            color = VerdeDuo,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Se abre después",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            ZaziButton(
                text = when {
                    completado -> "Repasar ${nodo.nombre}"
                    desbloqueado -> "Empezar ${nodo.nombre}"
                    else -> "Reto bloqueado: ${nodo.nombre}"
                },
                onClick = onClick,
                modifier = Modifier
                    .width(92.dp)
                    .height(46.dp),
                containerColor = when {
                    completado -> VerdeDuo
                    desbloqueado -> color
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = when {
                    completado -> ZaziliWhite
                    desbloqueado -> contenido
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                },
                cornerRadius = 17.dp,
                baseHeight = 4.dp,
                fontSize = 12.sp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!desbloqueado) {
                        KawaiiLockIcon(
                            size = 16.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = when {
                            completado -> "Repasar"
                            desbloqueado -> "Empezar"
                            else -> "Luego"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            completado -> ZaziliWhite
                            desbloqueado -> contenido
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoReto(
    mundo: MundoSaga,
    nodo: NodoSaga,
    onCerrar: () -> Unit,
    alResponder: (Int) -> ResultadoReto
) {
    val colorModulo = colorDeModulo(mundo.modulo)
    var elegida by remember(nodo.id) { mutableStateOf<Int?>(null) }
    var resultado by remember(nodo.id) { mutableStateOf<ResultadoReto?>(null) }
    var pistaVisible by remember(nodo.id) { mutableStateOf(false) }
    val exitoso = resultado?.correcto == true

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorModulo.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (nodo.esJefe) "JEFE FINAL" else "RETO DEL MUNDO",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorModulo,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    ZaziButton(
                        text = "Cerrar el reto",
                        onClick = onCerrar,
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cornerRadius = 20.dp,
                        baseHeight = 3.dp
                    ) {
                        Text(
                            text = "×",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZaziSpriteAnimado(
                        modifier = Modifier.size(76.dp),
                        estado = if (exitoso) {
                            ZaziState.CELEBRACION
                        } else {
                            ZaziState.VUELO_IDLE
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (exitoso) {
                            VerdeDuo.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when {
                                resultado == null -> "Tómate tu tiempo. Aquí nadie corre."
                                exitoso && resultado?.primeraVez == true ->
                                    "¡Chido! Zazi dio un brinco de gusto."
                                exitoso -> "¡Otra vez! Ya traías la respuesta."
                                else -> "Casi le atinas. Los errores son pistas, no regaños."
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = nodo.nombre,
                    style = MaterialTheme.typography.labelLarge,
                    color = colorModulo,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = nodo.enunciado,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 27.sp
                )

                Spacer(Modifier.height(14.dp))
                nodo.opciones.forEachIndexed { indice, opcion ->
                    val esCorrecta = indice == nodo.indiceCorrecto
                    val elegidaIncorrecta = resultado?.correcto == false && elegida == indice
                    val fondoCarta = when {
                        exitoso && esCorrecta -> VerdeDuo
                        elegidaIncorrecta -> AmarilloSuave
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val contenidoCarta = when {
                        exitoso && esCorrecta -> ZaziliWhite
                        elegidaIncorrecta -> UniversoProfundo
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    ZaziButton(
                        text = opcion,
                        onClick = {
                            if (!exitoso) {
                                val nuevo = alResponder(indice)
                                elegida = indice
                                resultado = nuevo
                                if (nuevo.correcto) {
                                    GlobalAudioManager.playSuccess()
                                } else {
                                    GlobalAudioManager.playPop()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp),
                        containerColor = fondoCarta,
                        contentColor = contenidoCarta,
                        cornerRadius = 18.dp,
                        baseHeight = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(contenidoCarta.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (exitoso && esCorrecta) {
                                    KawaiiCheckIcon(size = 16.dp, color = ZaziliWhite)
                                } else {
                                    Text(
                                        text = listOf("A", "B", "C", "D").getOrElse(indice) { "?" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = contenidoCarta
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = opcion,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contenidoCarta,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (!exitoso) {
                    Spacer(Modifier.height(4.dp))
                    ZaziButton(
                        text = if (pistaVisible) "Ocultar la pista" else "¿Me das una pista?",
                        onClick = {
                            GlobalAudioManager.playPop()
                            pistaVisible = !pistaVisible
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        containerColor = if (pistaVisible) {
                            AmarilloVibrante
                        } else {
                            AmarilloSuave
                        },
                        contentColor = UniversoProfundo,
                        cornerRadius = 16.dp,
                        baseHeight = 3.dp,
                        fontSize = 13.sp
                    )
                    if (pistaVisible) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AmarilloSuave.copy(alpha = 0.55f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                KawaiiStarIcon(size = 22.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = nodo.pista,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UniversoProfundo,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                if (resultado != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (exitoso) {
                            VerdeDuo.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            if (exitoso) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(3) { indice ->
                                        KawaiiStarIcon(
                                            size = 32.dp,
                                            color = if (indice < (resultado?.estrellas ?: 0)) {
                                                AmarilloVibrante
                                            } else {
                                                LavandaClara
                                            }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (resultado?.primeraVez == true) {
                                        "¡Reto completado!"
                                    } else {
                                        "¡Práctica completada!"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if ((resultado?.xpGanado ?: 0) > 0) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        EtiquetaSuave {
                                            KawaiiStarIcon(size = 16.dp)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "+${resultado?.xpGanado} XP",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        EtiquetaSuave {
                                            KawaiiCoinIcon(size = 17.dp)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "+${resultado?.zazolesGanados} zazoles",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                ZaziButton(
                                    text = "¡Seguir!",
                                    onClick = onCerrar,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    containerColor = VerdeDuo,
                                    contentColor = ZaziliWhite,
                                    cornerRadius = 20.dp,
                                    baseHeight = 5.dp,
                                    fontSize = 16.sp
                                )
                            } else {
                                Text(
                                    text = "Puedes intentarlo otra vez. La pista de arriba no cuesta nada.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Sin cronómetro y sin castigos. Si te cansas, cierras y regresas después.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ModuloNoDisponible(onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ZaziSpriteAnimado(
                modifier = Modifier.size(110.dp),
                estado = ZaziState.PENSANDO
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Este mundo vive en El Nido de los Grandes.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            ZaziButton(
                text = "Volver",
                onClick = onVolver,
                modifier = Modifier
                    .width(180.dp)
                    .height(52.dp)
            )
        }
    }
}
