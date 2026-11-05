package com.example.zazili.presentation.emociones

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiZazi
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.emociones.EmotionOption
import com.example.zazili.domain.emociones.EmotionScenario
import com.example.zazili.domain.emociones.EmotionsModuleEngine
import com.example.zazili.presentation.comun.FeedbackBottomSheet
import com.example.zazili.presentation.comun.InGameFeedbackDictionary
import com.example.zazili.presentation.comun.LessonSnackbar
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.VerdeDuoOscuro
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Pantalla completa de "¿Tú qué harías?".
 *
 * Diseñada para niños con ADHD y autismo: una situación por pantalla, opciones
 * verticales enormes, emojis de 38 sp como ancla visual y explicación amable
 * cuando la respuesta no ayuda. El niño puede reintentar sin castigo; solo
 * avanza después de encontrar la opción que acompaña.
 */
@Composable
fun EmotionScenarioScreen(
    modifier: Modifier = Modifier,
    motor: EmotionsModuleEngine = remember { EmotionsModuleEngine() },
    onSalir: () -> Unit = {},
    onCompletar: (aciertos: Int, total: Int) -> Unit = { _, _ -> }
) {
    val estado by motor.estado.collectAsStateWithLifecycle()
    val escenario = motor.escenarioActual()
    val zen = collectZenMode()
    val completarActual by rememberUpdatedState(onCompletar)

    var mensajeAcierto by remember { mutableStateOf<String?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var resultadoNotificado by rememberSaveable { mutableStateOf(false) }

    val opcionElegida = estado.opcionElegida?.let { escenario.options.getOrNull(it) }
    val seleccionCorrecta = opcionElegida?.isCorrect == true
    val seleccionIncorrecta = opcionElegida != null && !seleccionCorrecta

    LaunchedEffect(estado.terminado) {
        if (estado.terminado && !resultadoNotificado) {
            resultadoNotificado = true
            completarActual(estado.aciertos, motor.total)
        }
    }

    BackHandler(onBack = onSalir)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            EncabezadoEmociones(
                indice = estado.indiceEscenario,
                total = motor.total,
                terminado = estado.terminado,
                zen = zen,
                onSalir = onSalir
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = {
                    if (estado.terminado) {
                        1f
                    } else {
                        estado.indiceEscenario.toFloat() / motor.total.toFloat()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = VerdeDuo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (estado.terminado) {
                PantallaFinal(
                    modifier = Modifier.weight(1f),
                    aciertos = estado.aciertos,
                    total = motor.total,
                    zen = zen,
                    onRepetir = {
                        resultadoNotificado = false
                        mensajeAcierto = null
                        mensajeError = null
                        motor.reiniciar()
                    },
                    onSalir = onSalir
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    TarjetaSituacion(escenario)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Elige la opción que más ayuda:",
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(12.dp))
                    escenario.options.forEachIndexed { indice, opcion ->
                        OpcionEmocionalCard(
                            opcion = opcion,
                            seleccionada = estado.opcionElegida == indice,
                            habilitada = estado.opcionElegida == null,
                            onClick = {
                                val resultado = motor.seleccionar(indice)
                                when {
                                    resultado == null -> Unit
                                    resultado.correcta -> {
                                        GlobalAudioManager.playSuccess()
                                        mensajeAcierto = InGameFeedbackDictionary.acierto()
                                    }
                                    else -> {
                                        GlobalAudioManager.playMiss()
                                        mensajeError = InGameFeedbackDictionary.error()
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    if (seleccionCorrecta) {
                        ExplicacionCorrecta(opcionElegida?.explanation.orEmpty())
                        Spacer(Modifier.height(16.dp))
                        ZaziButton(
                            text = if (estado.indiceEscenario >= motor.total - 1) {
                                "Ver mi resultado"
                            } else {
                                "Siguiente reto"
                            },
                            onClick = {
                                mensajeAcierto = null
                                mensajeError = null
                                motor.siguiente()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }

        LessonSnackbar(
            visible = mensajeAcierto != null && !estado.terminado,
            esCorrecto = true,
            mensaje = mensajeAcierto,
            onOcultar = { mensajeAcierto = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )

        FeedbackBottomSheet(
            visible = seleccionIncorrecta && !estado.terminado,
            esCorrecto = false,
            titulo = "Otra mirada",
            mensaje = opcionElegida?.explanation.orEmpty(),
            mensajeDinamico = mensajeError,
            textoBoton = "Intentar otra vez",
            onBoton = {
                mensajeError = null
                motor.descartarSeleccion()
            },
            onDescartar = {
                mensajeError = null
                motor.descartarSeleccion()
            }
        )
    }
}

@Composable
private fun EncabezadoEmociones(
    indice: Int,
    total: Int,
    terminado: Boolean,
    zen: Boolean,
    onSalir: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Salir de la actividad"
                ) { onSalir() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = "Salir de ¿Tú qué harías?"
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "¿Tú qué harías?",
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (terminado) "Resultado final" else "Reto ${indice + 1} de $total",
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }
        KawaiiZazi(
            size = 56.dp,
            animar = !zen,
            descripcion = "Zazi te acompaña en este reto"
        )
    }
}

@Composable
private fun TarjetaSituacion(escenario: EmotionScenario) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(3.dp, LavandaMedia.copy(alpha = 0.85f)),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🧩", fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "LA SITUACIÓN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = escenario.situation,
                fontSize = 26.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OpcionEmocionalCard(
    opcion: EmotionOption,
    seleccionada: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    val colorFondo by animateColorAsState(
        targetValue = when {
            seleccionada && opcion.isCorrect -> VerdeDuo
            seleccionada -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 220),
        label = "fondoOpcionEmocion"
    )
    val colorBorde by animateColorAsState(
        targetValue = when {
            seleccionada && opcion.isCorrect -> VerdeDuoOscuro
            seleccionada -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 220),
        label = "bordeOpcionEmocion"
    )
    val colorTexto = when {
        seleccionada && opcion.isCorrect -> ZaziliWhite
        seleccionada -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val visibilidad = if (habilitada || seleccionada) 1f else 0.5f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                enabled = habilitada,
                role = Role.Button,
                onClickLabel = "Elegir ${opcion.text}"
            ) { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = colorFondo,
        border = BorderStroke(3.dp, colorBorde),
        shadowElevation = if (seleccionada) 2.dp else 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .alpha(visibilidad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = opcion.emoji,
                fontSize = 38.sp,
                lineHeight = 42.sp
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = opcion.text,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorTexto,
                modifier = Modifier.weight(1f)
            )
            if (seleccionada) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (opcion.isCorrect) "✓" else "✕",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto
                )
            }
        }
    }
}

@Composable
private fun ExplicacionCorrecta(explicacion: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VerdeDuo.copy(alpha = 0.14f),
        border = BorderStroke(2.dp, VerdeDuo)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(text = "✅", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "¿Por qué ayuda?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = explicacion,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PantallaFinal(
    modifier: Modifier = Modifier,
    aciertos: Int,
    total: Int,
    zen: Boolean,
    onRepetir: () -> Unit,
    onSalir: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(20.dp))
        KawaiiZazi(size = 132.dp, animar = !zen)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "¡Terminaste!",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Acertaste $aciertos de $total retos.",
            fontSize = 19.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Cada intento te enseña algo nuevo.",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
        Spacer(Modifier.height(28.dp))
        ZaziButton(
            text = "Jugar otra vez",
            onClick = onRepetir,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        ZaziButton(
            text = "Volver",
            onClick = onSalir,
            modifier = Modifier.fillMaxWidth(),
            containerColor = LavandaMedia,
            contentColor = UniversoProfundo
        )
        Spacer(Modifier.height(28.dp))
    }
}
