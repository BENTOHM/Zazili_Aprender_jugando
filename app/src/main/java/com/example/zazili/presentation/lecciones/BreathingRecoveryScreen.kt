package com.example.zazili.presentation.lecciones

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.BionicText
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay

private enum class FaseRespiracion(
    val nombre: String,
    val indicacion: String,
    val duracionMs: Long,
    val escala: Float,
    val brillo: Float
) {
    INHALA(
        nombre = "Inhala",
        indicacion = "Toma aire por la nariz, despacito.",
        duracionMs = 4_000L,
        escala = 1.38f,
        brillo = 1f
    ),
    SOSTEN(
        nombre = "Sostén",
        indicacion = "Guarda el aire tantito.",
        duracionMs = 2_000L,
        escala = 1.38f,
        brillo = 0.85f
    ),
    EXHALA(
        nombre = "Exhala",
        indicacion = "Suelta el aire por la boca.",
        duracionMs = 4_000L,
        escala = 0.78f,
        brillo = 0.32f
    )
}

/**
 * Recuperación afectiva cuando se acaban los corazones.
 *
 * No hay derrota ni botón de salto: Zazi pide ayuda para respirar y la pantalla
 * guía UN ciclo de 4 segundos inhalando, 2 sosteniendo y 4 exhalando. Al cerrar
 * el ciclo, [onRecuperado] devuelve un corazón y la lección continúa donde iba.
 *
 * La expansión y la contracción usan [animateFloatAsState] con la duración de
 * cada fase; el velo bloquea cualquier toque para que el descanso sea real.
 */
@Composable
fun BreathingRecoveryScreen(
    onRecuperado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    val recuperar by rememberUpdatedState(onRecuperado)

    var fase by remember { mutableStateOf(FaseRespiracion.INHALA) }
    var arrancado by remember { mutableStateOf(false) }

    val escala by animateFloatAsState(
        targetValue = if (arrancado) fase.escala else 0.72f,
        animationSpec = tween(
            durationMillis = if (arrancado) fase.duracionMs.toInt() else 4_000,
            easing = if (fase == FaseRespiracion.EXHALA) {
                LinearEasing
            } else {
                FastOutSlowInEasing
            }
        ),
        label = "escalaRespiracion"
    )
    val brillo by animateFloatAsState(
        targetValue = if (arrancado) fase.brillo else 0.12f,
        animationSpec = tween(
            durationMillis = if (arrancado) fase.duracionMs.toInt() else 4_000,
            easing = FastOutSlowInEasing
        ),
        label = "brilloRespiracion"
    )

    LaunchedEffect(Unit) {
        arrancado = true
        delay(FaseRespiracion.INHALA.duracionMs)
        fase = FaseRespiracion.SOSTEN
        delay(FaseRespiracion.SOSTEN.duracionMs)
        fase = FaseRespiracion.EXHALA
        delay(FaseRespiracion.EXHALA.duracionMs)
        recuperar()
    }

    BackHandler(enabled = true) {
        // Respirar no se puede saltar: la lección espera este ciclo.
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(UniversoProfundo, UniversoMedio, Color(0xFF3A1450))
                )
            )
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 22.dp)
        ) {
            KawaiiZazi(
                size = 112.dp,
                animar = !zen,
                descripcion = "Zazi respira contigo"
            )
            Spacer(Modifier.height(6.dp))
            BionicText(
                text = "¡Uf, mi cerebro va muy rápido! Ayúdame a respirar.",
                style = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                color = ZaziliWhite
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sigue el círculo. No hay prisa y no hay regaño.",
                style = MaterialTheme.typography.bodySmall,
                color = MintGreen.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(286.dp)
                    .semantics {
                        contentDescription = "${fase.nombre}. ${fase.indicacion}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centro = center
                    val radioBase = size.minDimension * 0.46f
                    val radio = radioBase * escala
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MintGreen.copy(alpha = 0.20f + 0.30f * brillo),
                                MagicalPurple.copy(alpha = 0.10f + 0.20f * brillo),
                                Color.Transparent
                            ),
                            center = centro,
                            radius = radio * 1.18f
                        ),
                        radius = radio * 1.18f,
                        center = centro
                    )
                    drawCircle(
                        color = MintGreen.copy(alpha = 0.28f + 0.42f * brillo),
                        radius = radio,
                        center = centro
                    )
                    drawCircle(
                        color = ZaziliWhite.copy(alpha = 0.45f + 0.35f * brillo),
                        radius = radio * 0.82f,
                        center = centro,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = ZaziliWhite.copy(alpha = 0.16f),
                        radius = radioBase * 1.06f,
                        center = centro,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = fase.nombre,
                        color = ZaziliWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${fase.duracionMs / 1_000} segundos",
                        color = AmarilloSuave,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = fase.indicacion,
                style = MaterialTheme.typography.titleMedium,
                color = ZaziliWhite.copy(alpha = 0.92f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FaseRespiracion.entries.forEach { paso ->
                    val activa = paso == fase
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (activa) {
                            MintGreen
                        } else {
                            ZaziliWhite.copy(alpha = 0.10f)
                        }
                    ) {
                        Text(
                            text = paso.nombre,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (activa) UniversoProfundo else ZaziliWhite.copy(alpha = 0.7f),
                            fontWeight = if (activa) FontWeight.Black else FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZaziliWhite.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Al terminar el ciclo, Zazi te regala un corazón y seguimos juntos.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = ZaziliWhite.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(UniversoProfundo.copy(alpha = 0.34f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Inhala 4 · Sostén 2 · Exhala 4",
                    style = MaterialTheme.typography.labelSmall,
                    color = MintGreen.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
