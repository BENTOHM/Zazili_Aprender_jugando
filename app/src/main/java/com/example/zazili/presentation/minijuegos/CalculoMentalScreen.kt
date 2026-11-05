package com.example.zazili.presentation.minijuegos

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiTacoIcon
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.presentation.lecciones.CalculoMentalExercise
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Minijuego de cálculo mental embebido en la lección.
 *
 * No tiene barra, corazones ni resultados propios: la pantalla de lección es
 * dueña de esos elementos. Aquí solo se elige una opción, se comprueba y se
 * entrega `(indice, esCorrecta)` al motor de la lección.
 */
@Composable
fun CalculoMentalScreen(
    exercise: CalculoMentalExercise,
    onSubmitAnswer: (indice: Int, esCorrecta: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    var seleccion by rememberSaveable(exercise.id) { mutableIntStateOf(-1) }
    var enviado by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var pistaVisible by rememberSaveable(exercise.id) { mutableStateOf(false) }
    val haySeleccion = seleccion in exercise.opciones.indices

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            FilaZaziCalculo(consigna = exercise.consigna, zen = zen)
            Spacer(Modifier.height(12.dp))
            TarjetaEcuacion(exercise = exercise, zen = zen)
            Spacer(Modifier.height(4.dp))
            BotonPista(
                visible = pistaVisible,
                onClick = {
                    GlobalAudioManager.playPop()
                    pistaVisible = !pistaVisible
                }
            )
            AnimatedVisibility(visible = pistaVisible) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AmarilloSuave.copy(alpha = 0.62f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pista: ${exercise.pista}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = UniversoProfundo
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OpcionesCalculo(
                exercise = exercise,
                seleccion = seleccion,
                enviado = enviado,
                onElegir = { indice ->
                    if (!enviado) {
                        GlobalAudioManager.playPop()
                        seleccion = indice
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }

        PanelComprobar(
            haySeleccion = haySeleccion,
            enviado = enviado,
            onComprobar = {
                val elegido = seleccion
                if (elegido in exercise.opciones.indices && !enviado) {
                    enviado = true
                    onSubmitAnswer(elegido, elegido == exercise.indiceCorrecto)
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
//  ZAZI Y SU CONSIGNA
// ---------------------------------------------------------------------------

@Composable
private fun FilaZaziCalculo(
    consigna: String,
    zen: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        KawaiiZazi(
            size = 58.dp,
            animar = !zen,
            descripcion = "Zazi te acompaña en el cálculo mental"
        )
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (zen) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            },
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "Zazi dice",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = consigna,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  TARJETA DE LA ECUACIÓN
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaEcuacion(
    exercise: CalculoMentalExercise,
    zen: Boolean
) {
    val tamanoEcuacion = when {
        exercise.enunciado.length > 18 -> 28.sp
        exercise.enunciado.length > 13 -> 32.sp
        else -> 38.sp
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (zen) 0.dp else 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Nivel ${exercise.dificultad}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = exercise.titulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Bold
                )
            }

            if (!zen && exercise.gruposVisuales.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FilaTacos(grupos = exercise.gruposVisuales)
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = exercise.enunciado,
                fontSize = tamanoEcuacion,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Ecuación: ${exercise.enunciado}"
                    }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Sin prisa. Puedes contar con los dedos.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilaTacos(grupos: List<Int>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grupos.forEachIndexed { indice, cantidad ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(cantidad.coerceAtMost(10)) {
                    KawaiiTacoIcon(size = 22.dp)
                }
            }
            if (indice < grupos.lastIndex) {
                Text(
                    text = "+",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PISTA
// ---------------------------------------------------------------------------

@Composable
private fun BotonPista(
    visible: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics {
            contentDescription = if (visible) "Ocultar pista" else "Ver pista"
        }
    ) {
        KawaiiSparkIcon(size = 17.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (visible) "Ocultar pista" else "Ver pista",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ---------------------------------------------------------------------------
//  OPCIONES
// ---------------------------------------------------------------------------

@Composable
private fun OpcionesCalculo(
    exercise: CalculoMentalExercise,
    seleccion: Int,
    enviado: Boolean,
    onElegir: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        exercise.opciones.chunked(2).forEachIndexed { fila, textos ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                textos.forEachIndexed { columna, texto ->
                    val indice = fila * 2 + columna
                    val esCorrecta = enviado && indice == exercise.indiceCorrecto
                    val esElegidaMala = enviado &&
                        seleccion == indice &&
                        indice != exercise.indiceCorrecto
                    val seleccionada = !enviado && seleccion == indice

                    val contenedor = when {
                        esCorrecta -> VerdeDuo
                        esElegidaMala -> CoralAviso
                        seleccionada -> MintGreen
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val contenido = when {
                        esCorrecta -> ZaziliWhite
                        esElegidaMala -> UniversoProfundo
                        seleccionada -> UniversoProfundo
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    ZaziButton(
                        text = texto,
                        onClick = { onElegir(indice) },
                        modifier = Modifier
                            .weight(1f)
                            .height(66.dp),
                        containerColor = contenedor,
                        contentColor = contenido,
                        cornerRadius = 20.dp,
                        baseHeight = 5.dp,
                        enabled = !enviado,
                        fontSize = 22.sp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (esCorrecta) {
                                KawaiiCheckIcon(size = 19.dp, color = contenido)
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = texto,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = contenido
                            )
                        }
                    }
                }
                if (textos.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  PANEL INFERIOR
// ---------------------------------------------------------------------------

@Composable
private fun PanelComprobar(
    haySeleccion: Boolean,
    enviado: Boolean,
    onComprobar: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = when {
                    enviado -> "Revisa la respuesta con Zazi."
                    haySeleccion -> "¿Va? Toca Comprobar."
                    else -> "Elige una respuesta. Sin prisa."
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            val activo = haySeleccion && !enviado
            ZaziButton(
                text = "Comprobar",
                onClick = onComprobar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                containerColor = if (activo) {
                    VerdeDuo
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (activo) {
                    ZaziliWhite
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
                cornerRadius = 20.dp,
                baseHeight = 6.dp,
                enabled = activo,
                fontSize = 19.sp
            )
        }
    }
}
