package com.example.zazili.presentation.hogar

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.mascot.EstadoVital
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiBoltIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiHeartIcon
import com.example.zazili.core.ui.KawaiiNestIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.KawaiiTacoIcon
import com.example.zazili.core.ui.MusicThemeButton
import com.example.zazili.core.ui.ThemeToggleButton
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.economia.IdsArticulo
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliCreamSoft
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * La casita de Zazi 2.0: hero ilustrado, medidores claros, acciones 3D y una
 * entrada directa a los cuatro mundos de aprendizaje. Sin menús escondidos ni
 * letras chiquitas: el niño ve su mascota, su nivel y qué puede hacer en menos
 * de tres segundos.
 */
@Composable
fun HomeScreen(
    onIrASaga: () -> Unit,
    onIrAlNido: () -> Unit,
    darkTheme: Boolean,
    onToggleTema: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val progreso by viewModel.progreso.collectAsStateWithLifecycle()
    val zazoles by viewModel.zazoles.collectAsStateWithLifecycle()
    val equipados by viewModel.equipados.collectAsStateWithLifecycle()
    val frase by viewModel.frase.collectAsStateWithLifecycle()
    val evento by viewModel.evento.collectAsStateWithLifecycle()

    val zen = collectZenMode()
    val fondo = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }

    LaunchedEffect(Unit) { viewModel.saludar() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Encabezado(
                darkTheme = darkTheme,
                onToggleTema = onToggleTema
            )

            Spacer(Modifier.height(16.dp))
            EscenaCasita(
                estado = estado,
                frase = frase,
                equipados = equipados,
                evento = evento,
                onTocarZazi = {
                    GlobalAudioManager.playPop()
                    viewModel.mimo()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(348.dp)
            )

            Spacer(Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TarjetaNivel(
                    progreso = progreso,
                    modifier = Modifier.weight(1.25f)
                )
                TarjetaZazoles(
                    zazoles = zazoles,
                    modifier = Modifier.weight(0.75f)
                )
            }

            Spacer(Modifier.height(14.dp))
            TarjetaMedidores(estado = estado)

            Spacer(Modifier.height(20.dp))
            Text(
                text = "¿Qué hacemos hoy?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BotonAccion(
                    texto = "Alimentar",
                    icono = { KawaiiTacoIcon(size = 28.dp) },
                    contenedor = AmarilloVibrante,
                    contenido = UniversoProfundo,
                    onClick = {
                        GlobalAudioManager.playPop()
                        viewModel.alimentar()
                    },
                    modifier = Modifier.weight(1f)
                )
                BotonAccion(
                    texto = "Jugar",
                    icono = { KawaiiSparkIcon(size = 28.dp) },
                    contenedor = VerdeDuo,
                    contenido = ZaziliWhite,
                    onClick = {
                        GlobalAudioManager.playPop()
                        viewModel.jugar()
                    },
                    modifier = Modifier.weight(1f)
                )
                BotonAccion(
                    texto = "Mimo",
                    icono = { KawaiiHeartIcon(size = 28.dp) },
                    contenedor = RosaSuave,
                    contenido = UniversoProfundo,
                    onClick = {
                        GlobalAudioManager.playPop()
                        viewModel.mimo()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))
            ZaziButton(
                text = "¡Vamos a aprender!",
                onClick = {
                    GlobalAudioManager.playPop()
                    onIrASaga()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                cornerRadius = 26.dp,
                baseHeight = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    KawaiiStarIcon(size = 30.dp, color = AmarilloVibrante)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "¡Vamos a aprender!",
                        color = ZaziliWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(10.dp))
                    KawaiiArrowIcon(size = 24.dp, color = ZaziliWhite)
                }
            }

            Spacer(Modifier.height(12.dp))
            ZaziButton(
                text = "El Nido",
                onClick = {
                    GlobalAudioManager.playPop()
                    onIrAlNido()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                cornerRadius = 20.dp,
                baseHeight = 5.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(LavandaClara),
                        contentAlignment = Alignment.Center
                    ) {
                        KawaiiNestIcon(size = 20.dp)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "El Nido",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Zazi nunca se enoja. Si hoy descansas, ella también.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun Encabezado(
    darkTheme: Boolean,
    onToggleTema: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "¡Órale!",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Zazi te estaba esperando, sin prisa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
            )
        }
        ThemeToggleButton(
            darkTheme = darkTheme,
            onToggle = onToggleTema
        )
        Spacer(Modifier.width(10.dp))
        MusicThemeButton(
            darkTheme = darkTheme,
            size = 52.dp
        )
    }
}

@Composable
private fun TarjetaNivel(progreso: GameProgress, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AmarilloSuave),
                contentAlignment = Alignment.Center
            ) {
                KawaiiStarIcon(size = 25.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Nivel ${progreso.level}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${progreso.xpIntoLevel}/${progreso.xpForNextLevel} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                BarraProgreso(
                    fraccion = progreso.levelProgress,
                    color = MintGreen,
                    altura = 7.dp
                )
            }
        }
    }
}

@Composable
private fun TarjetaZazoles(zazoles: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            KawaiiCoinIcon(size = 34.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "$zazoles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "zazoles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun TarjetaMedidores(estado: EstadoVital) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Así anda Zazi",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorEstado(estado).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = resumenEstado(estado),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorEstado(estado),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            MedidorVital(
                etiqueta = "Pancita",
                descripcion = descripcionPancita(estado),
                fraccion = estado.pancita,
                color = AmarilloVibrante,
                icono = { KawaiiTacoIcon(size = 24.dp) }
            )
            Spacer(Modifier.height(13.dp))
            MedidorVital(
                etiqueta = "Felicidad",
                descripcion = descripcionFelicidad(estado),
                fraccion = estado.felicidad,
                color = Color(0xFFF26D8D),
                icono = { KawaiiHeartIcon(size = 24.dp) }
            )
            Spacer(Modifier.height(13.dp))
            MedidorVital(
                etiqueta = "Energía",
                descripcion = descripcionEnergia(estado),
                fraccion = estado.energia,
                color = Color(0xFF4FB6E8),
                icono = { KawaiiBoltIcon(size = 24.dp) }
            )
        }
    }
}

@Composable
private fun MedidorVital(
    etiqueta: String,
    descripcion: String,
    fraccion: Float,
    color: Color,
    icono: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            icono()
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
            Spacer(Modifier.height(5.dp))
            BarraProgreso(
                fraccion = fraccion,
                color = color,
                altura = 10.dp
            )
        }
    }
}

@Composable
private fun BarraProgreso(
    fraccion: Float,
    color: Color,
    altura: Dp
) {
    val animada by animateFloatAsState(
        targetValue = fraccion.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "barraVital"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(altura)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animada)
                .height(altura)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
private fun BotonAccion(
    texto: String,
    icono: @Composable () -> Unit,
    contenedor: Color,
    contenido: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ZaziButton(
        text = texto,
        onClick = onClick,
        modifier = modifier.height(96.dp),
        containerColor = contenedor,
        contentColor = contenido,
        cornerRadius = 24.dp,
        baseHeight = 6.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ZaziliCreamSoft.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                icono()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = texto,
                color = contenido,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

private fun resumenEstado(estado: EstadoVital): String = when {
    estado.dormida -> "Modo siesta"
    estado.pancita < 0.30f -> "Pide semillas"
    estado.energia < 0.30f -> "Modo avión"
    estado.felicidad > 0.75f -> "¡Contentísima!"
    else -> "Tranquila"
}

private fun colorEstado(estado: EstadoVital): Color = when {
    estado.dormida -> LavandaMedia
    estado.pancita < 0.30f -> AmarilloVibrante
    estado.energia < 0.30f -> AzulCielo
    estado.felicidad > 0.75f -> VerdeDuo
    else -> LavandaMedia
}

private fun descripcionPancita(estado: EstadoVital): String = when {
    estado.pancita < 0.30f -> "pide semillas"
    estado.pancita < 0.60f -> "con hambre"
    else -> "bien llena"
}

private fun descripcionFelicidad(estado: EstadoVital): String = when {
    estado.felicidad < 0.30f -> "necesita mimos"
    estado.felicidad < 0.60f -> "tranquila"
    else -> "contentísima"
}

private fun descripcionEnergia(estado: EstadoVital): String = when {
    estado.dormida -> "recargando"
    estado.energia < 0.30f -> "modo avión"
    estado.energia < 0.60f -> "con pilas"
    else -> "a tope"
}
