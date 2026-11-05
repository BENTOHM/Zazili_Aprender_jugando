package com.example.zazili.presentation.minijuegos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.presentation.lecciones.LessonEngine
import com.example.zazili.presentation.lecciones.ScienceExercise
import com.example.zazili.presentation.modulos.comun.contenidoDe
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

// ============================================================================
//  STUBS JUGABLES DEL LABORATORIO DEL CENOTE
//  Cada pantalla comparte el mismo motor de selección/comprobar, pero tiene un
//  panel visual propio. Son ligeras a propósito: Box + Text, sin Canvas ni
//  animaciones costosas, para probar el enrutado sin lag.
// ============================================================================

@Composable
fun AlquimiaCenoteScreen(
    exercise: ScienceExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    MarcoCiencia(
        exercise = exercise,
        titulo = "Alquimia del Cenote",
        emoji = "🧪",
        color = CoralAviso,
        onSubmitAnswer = onSubmitAnswer,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilaProceso(
                emojis = exercise.visuales.ifEmpty { listOf("🧊", "☀️", "💧") },
                color = AzulCielo
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaVisual(texto = "🔥 Calor", color = CoralAviso)
                EtiquetaVisual(texto = "🧊 Frío", color = AzulCielo)
            }
        }
    }
}

@Composable
fun CadenasAlimenticiasScreen(
    exercise: ScienceExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    MarcoCiencia(
        exercise = exercise,
        titulo = "Cadenas Alimenticias",
        emoji = "🌿",
        color = VerdeDuo,
        onSubmitAnswer = onSubmitAnswer,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilaProceso(
                emojis = exercise.visuales.ifEmpty { listOf("☀️", "🌱", "🐇") },
                color = VerdeDuo,
                anillo = true
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "La flecha muestra quién alimenta a quién.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FiltroMagicoScreen(
    exercise: ScienceExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    MarcoCiencia(
        exercise = exercise,
        titulo = "El Filtro Mágico",
        emoji = "💧",
        color = AzulCielo,
        onSubmitAnswer = onSubmitAnswer,
        modifier = modifier
    ) {
        PilaDeCapas(
            capas = exercise.visuales.ifEmpty { listOf("Grava", "Arena", "Algodón") }
        )
    }
}

@Composable
fun MonstruoBasuraScreen(
    exercise: ScienceExercise,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    MarcoCiencia(
        exercise = exercise,
        titulo = "El Monstruo de Basura",
        emoji = "🗑️",
        color = AmarilloSuave,
        onSubmitAnswer = onSubmitAnswer,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(LavandaClara),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👾", fontSize = 28.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "¡Atrapa y clasifica!",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(AmarilloSuave),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exercise.visuales.firstOrNull() ?: "🗑️",
                    fontSize = 32.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                EtiquetaVisual("🌱 Orgánico", VerdeDuo, Modifier.weight(1f))
                EtiquetaVisual("♻️ Inorgánico", AzulCielo, Modifier.weight(1f))
                EtiquetaVisual("☠️ Tóxico", CoralAviso, Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  MARCO COMPARTIDO: ENUNCIADO, OPCIONES, PISTA Y COMPROBAR
// ---------------------------------------------------------------------------

@Composable
private fun MarcoCiencia(
    exercise: ScienceExercise,
    titulo: String,
    emoji: String,
    color: Color,
    onSubmitAnswer: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    visual: @Composable () -> Unit
) {
    var seleccion by rememberSaveable(exercise.id) { mutableIntStateOf(-1) }
    var enviado by rememberSaveable(exercise.id) { mutableStateOf(false) }
    // El motor ya mezcló las opciones; aquí solo se vinculan con su
    // AnswerOption para comprobar isCorrect al momento de enviar.
    val opciones = remember(exercise.id, exercise.opciones, exercise.indiceCorrecto) {
        LessonEngine.opcionesDe(exercise)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(color.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = exercise.consigna,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = color.copy(alpha = 0.22f)
            ) {
                Text(
                    text = "Nivel ${exercise.dificultad}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(26.dp),
            color = color.copy(alpha = 0.14f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 132.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                visual()
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = exercise.enunciado,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Elige una respuesta. Puedes cambiarla antes de comprobar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
        )

        Spacer(Modifier.height(12.dp))

        opciones.forEachIndexed { indice, opcion ->
            OpcionCiencia(
                texto = opcion.text,
                seleccionada = seleccion == indice,
                color = color,
                habilitada = !enviado,
                onClick = { seleccion = indice }
            )
            Spacer(Modifier.height(10.dp))
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Pista: ${exercise.pista}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        ZaziButton(
            text = "Comprobar",
            onClick = {
                enviado = true
                val acierto = opciones.getOrNull(seleccion)?.isCorrect == true
                onSubmitAnswer(seleccion, acierto)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            containerColor = VerdeDuo,
            contentColor = contenidoDe(VerdeDuo),
            cornerRadius = 20.dp,
            baseHeight = 5.dp,
            enabled = seleccion >= 0 && !enviado,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Sin prisa. Este experimento no tiene reloj.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OpcionCiencia(
    texto: String,
    seleccionada: Boolean,
    color: Color,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    val fondo = if (seleccionada) color else MaterialTheme.colorScheme.surface
    val tinta = if (seleccionada) {
        contenidoDe(color)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    ZaziButton(
        text = texto,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        containerColor = fondo,
        contentColor = tinta,
        cornerRadius = 18.dp,
        baseHeight = 4.dp,
        enabled = habilitada,
        fontSize = 15.sp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        if (seleccionada) {
                            tinta
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
                        }
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = texto,
                color = tinta,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  PIEZAS VISUALES LIGERAS
// ---------------------------------------------------------------------------

@Composable
private fun FilaProceso(
    emojis: List<String>,
    color: Color,
    anillo: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        emojis.take(3).forEachIndexed { indice, emoji ->
            if (indice > 0) {
                Text(
                    text = "→",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        if (anillo) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            color.copy(alpha = 0.30f)
                        }
                    )
                    .then(
                        if (anillo) {
                            Modifier.border(3.dp, color, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }
        }
    }
}

@Composable
private fun PilaDeCapas(capas: List<String>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        capas.take(3).forEachIndexed { indice, capa ->
            val fondo = when (indice) {
                0 -> LavandaMedia
                1 -> AmarilloSuave
                else -> ZaziliWhite
            }
            val emoji = when (indice) {
                0 -> "🪨"
                1 -> "🏖️"
                else -> "🧵"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(fondo)
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(text = emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = capa,
                    style = MaterialTheme.typography.bodySmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                val etiqueta = when (indice) {
                    0 -> "arriba"
                    capas.lastIndex -> "abajo"
                    else -> ""
                }
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = UniversoProfundo.copy(alpha = 0.62f)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "El agua cae y deja atrás la tierra.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EtiquetaVisual(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.22f),
        modifier = modifier
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
