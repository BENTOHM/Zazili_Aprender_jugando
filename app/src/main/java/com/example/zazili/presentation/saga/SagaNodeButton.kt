package com.example.zazili.presentation.saga

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.ui.theme.AmarilloOscuro
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite

/** Los estados visuales de un reto en el mapa. */
enum class EstadoNodoMapa {
    COMPLETADO,
    ACTUAL,
    DISPONIBLE,
    BLOQUEADO
}

/**
 * Nodo-botón 3D de la Saga. La cara superior se hunde hasta su base al
 * presionar, como un botón de goma. El nodo ACTUAL respira con un pulso
 * suave (se apaga en Modo Zen) para guiar la mirada sin saturar.
 *
 * @param numero número global del reto, visible mientras está activo.
 * @param descripcion texto que lee TalkBack; describe estado y nombre.
 */
@Composable
fun SagaNodeButton(
    estado: EstadoNodoMapa,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    numero: Int? = null,
    size: Dp = 76.dp,
    animarPulso: Boolean = true,
    descripcion: String = "Reto de la saga"
) {
    val profundidad = size * 0.13f
    val accionActual by rememberUpdatedState(onClick)
    var presionado by remember { mutableStateOf(false) }

    val presion by animateFloatAsState(
        targetValue = if (presionado && estado != EstadoNodoMapa.BLOQUEADO) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.42f,
            stiffness = 900f
        ),
        label = "presionNodoSaga"
    )

    val escalaPulso = if (estado == EstadoNodoMapa.ACTUAL && animarPulso) {
        val transicion = rememberInfiniteTransition(label = "pulsoNodoSaga")
        transicion.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "escalaNodoSaga"
        ).value
    } else {
        1f
    }

    val cara = when (estado) {
        EstadoNodoMapa.COMPLETADO -> AmarilloVibrante
        EstadoNodoMapa.ACTUAL -> VerdeDuo
        EstadoNodoMapa.DISPONIBLE -> MintGreen
        EstadoNodoMapa.BLOQUEADO -> UniversoProfundo.copy(alpha = 0.42f)
    }
    val base = when (estado) {
        EstadoNodoMapa.COMPLETADO -> AmarilloOscuro
        EstadoNodoMapa.ACTUAL -> VerdeDuoOscuro
        EstadoNodoMapa.DISPONIBLE -> VerdeDuoOscuro
        EstadoNodoMapa.BLOQUEADO -> UniversoProfundo.copy(alpha = 0.28f)
    }
    val colorContenido = when (estado) {
        EstadoNodoMapa.COMPLETADO, EstadoNodoMapa.ACTUAL -> ZaziliWhite
        EstadoNodoMapa.DISPONIBLE -> UniversoProfundo
        EstadoNodoMapa.BLOQUEADO -> ZaziliWhite.copy(alpha = 0.72f)
    }

    Box(
        modifier = modifier
            .size(width = size, height = size + profundidad)
            .graphicsLayer {
                scaleX = escalaPulso
                scaleY = escalaPulso
                transformOrigin = TransformOrigin.Center
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = descripcion
                onClick(label = "Abrir reto") {
                    accionActual()
                    true
                }
            }
            .pointerInput(estado) {
                detectTapGestures(
                    onPress = {
                        if (estado != EstadoNodoMapa.BLOQUEADO) {
                            presionado = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                presionado = false
                            }
                        }
                    },
                    onTap = { accionActual() }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radio = size.toPx() / 2f
            val profundidadPx = profundidad.toPx()
            val centroSombra = Offset(
                x = this.size.width / 2f,
                y = this.size.height / 2f + profundidadPx / 2f
            )
            val centroCara = Offset(
                x = centroSombra.x,
                y = centroSombra.y - profundidadPx + profundidadPx * presion
            )

            drawCircle(color = base, radius = radio, center = centroSombra)
            drawCircle(color = cara, radius = radio, center = centroCara)
            drawCircle(
                color = ZaziliWhite.copy(
                    alpha = if (estado == EstadoNodoMapa.BLOQUEADO) 0.14f else 0.22f
                ),
                radius = radio * 0.82f,
                center = centroCara - Offset(0f, radio * 0.08f),
                style = Stroke(width = radio * 0.075f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-profundidad / 2f) + profundidad * presion)
                .size(size * 0.5f),
            contentAlignment = Alignment.Center
        ) {
            when (estado) {
                EstadoNodoMapa.COMPLETADO -> KawaiiStarIcon(
                    size = size * 0.42f,
                    color = colorContenido
                )

                EstadoNodoMapa.ACTUAL,
                EstadoNodoMapa.DISPONIBLE -> if (numero != null) {
                    Text(
                        text = numero.toString(),
                        color = colorContenido,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    KawaiiStarIcon(size = size * 0.42f, color = colorContenido)
                }

                EstadoNodoMapa.BLOQUEADO -> KawaiiLockIcon(
                    size = size * 0.38f,
                    color = colorContenido
                )
            }
        }
    }
}
