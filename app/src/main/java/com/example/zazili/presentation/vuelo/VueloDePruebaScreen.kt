package com.example.zazili.presentation.vuelo

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
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.RutaSaga
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

/**
 * El Vuelo de Prueba: diagnóstico invisible en forma de juego.
 *
 * Cinco preguntas cortas, una por mundo, sin reloj ni calificación visible.
 * Al final solo sugiere por dónde empezar; el registro queda en el Nido para
 * que un adulto pueda acompañar sin interrogar al niño.
 */
@Composable
fun VueloDePruebaScreen(
    onVolver: () -> Unit,
    onIrASaga: () -> Unit,
    onIrAModulo: (ModuloZazili) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VueloViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    val fondoBase = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }
    val fondo = if (zen) {
        Brush.verticalGradient(listOf(fondoBase, fondoBase))
    } else {
        Brush.verticalGradient(
            listOf(
                fondoBase,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            )
        )
    }

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
            BarraVuelo(onVolver = onVolver)
            Spacer(Modifier.height(8.dp))

            if (estado.terminado) {
                ResultadoVuelo(
                    estado = estado,
                    preguntas = viewModel.preguntas,
                    moduloSugerido = viewModel.moduloSugerido(),
                    onIrAModulo = onIrAModulo,
                    onIrASaga = onIrASaga,
                    onRepetir = viewModel::reiniciar
                )
            } else {
                HeroVuelo()
                Spacer(Modifier.height(14.dp))
                ProgresoVuelo(
                    indice = estado.indice,
                    total = viewModel.preguntas.size
                )
                Spacer(Modifier.height(12.dp))
                PreguntaVueloCard(
                    estado = estado,
                    pregunta = viewModel.preguntas[estado.indice],
                    numero = estado.indice + 1,
                    total = viewModel.preguntas.size,
                    alResponder = { opcion ->
                        viewModel.responder(opcion)
                        if (opcion == viewModel.preguntas[estado.indice].nodo.indiceCorrecto) {
                            GlobalAudioManager.playSuccess()
                        } else {
                            GlobalAudioManager.playPop()
                        }
                    },
                    alSiguiente = viewModel::siguiente
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Guardamos tus respuestas solo para recomendarte por dónde empezar. Nunca como calificación.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun BarraVuelo(onVolver: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        ZaziButton(
            text = "Volver a la casita",
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
                text = "El Vuelo de Prueba",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Cinco mundos, cero nota",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = AmarilloSuave
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                KawaiiSparkIcon(size = 15.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Sin nota",
                    style = MaterialTheme.typography.labelMedium,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HeroVuelo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            ZaziSpriteAnimado(
                modifier = Modifier.size(96.dp),
                estado = ZaziState.VUELO_IDLE
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Un paseo de 5 preguntas.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 29.sp
            )
            Text(
                text = "Sin reloj ni calificación. Solo queremos ver por dónde te gusta volar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun ProgresoVuelo(indice: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { posicion ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            posicion < indice -> VerdeDuo
                            posicion == indice -> AmarilloVibrante
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}

@Composable
private fun PreguntaVueloCard(
    estado: EstadoVuelo,
    pregunta: PreguntaVuelo,
    numero: Int,
    total: Int,
    alResponder: (Int) -> Unit,
    alSiguiente: () -> Unit
) {
    val nodo = pregunta.nodo
    val color = colorDeModulo(pregunta.modulo)
    val contenido = contenidoDe(color)
    val revelado = estado.respuestaCorrecta != null
    val acerto = estado.respuestaCorrecta == true

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = fondoSuaveModulo(pregunta.modulo)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        IconoModulo(pregunta.modulo, 18.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = pregunta.modulo.titulo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "$numero de $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = nodo.nombre,
                style = MaterialTheme.typography.labelLarge,
                color = color,
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
                val elegidaIncorrecta = revelado && estado.elegida == indice && !esCorrecta
                val fondoOpcion = when {
                    revelado && esCorrecta -> VerdeDuo
                    elegidaIncorrecta -> AmarilloSuave
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contenidoOpcion = when {
                    revelado && esCorrecta -> ZaziliWhite
                    elegidaIncorrecta -> UniversoProfundo
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                ZaziButton(
                    text = opcion,
                    onClick = {
                        if (!revelado) alResponder(indice)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    containerColor = fondoOpcion,
                    contentColor = contenidoOpcion,
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
                                .size(27.dp)
                                .clip(CircleShape)
                                .background(contenidoOpcion.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (revelado && esCorrecta) {
                                KawaiiCheckIcon(size = 15.dp, color = ZaziliWhite)
                            } else {
                                Text(
                                    text = listOf("A", "B", "C", "D").getOrElse(indice) { "?" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = contenidoOpcion
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
                            color = contenidoOpcion,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
            }

            if (!revelado) {
                ZaziButton(
                    text = "Todavía no lo sé",
                    onClick = { alResponder(-1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    cornerRadius = 16.dp,
                    baseHeight = 3.dp,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Decir «no sé» también ayuda. No hay respuesta que enoje a Zazi.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (acerto) {
                        VerdeDuo.copy(alpha = 0.14f)
                    } else {
                        AmarilloSuave.copy(alpha = 0.62f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        if (acerto) {
                            KawaiiCheckIcon(size = 24.dp, color = VerdeDuo)
                        } else {
                            KawaiiStarIcon(size = 24.dp)
                        }
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text(
                                text = if (acerto) {
                                    "¡Chido! Ese mundo ya te guiña un ojo."
                                } else {
                                    "Sin pena. Así sabemos por dónde empezar."
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (acerto) {
                                    "Pista para otra vez: ${nodo.pista}"
                                } else {
                                    "La respuesta era: ${nodo.opciones[nodo.indiceCorrecto]}. ${nodo.pista}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                ZaziButton(
                    text = if (numero >= total) "Ver mi vuelo" else "Siguiente pregunta",
                    onClick = alSiguiente,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    containerColor = if (acerto) VerdeDuo else color,
                    contentColor = if (acerto) ZaziliWhite else contenido,
                    cornerRadius = 20.dp,
                    baseHeight = 5.dp,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ResultadoVuelo(
    estado: EstadoVuelo,
    preguntas: List<PreguntaVuelo>,
    moduloSugerido: ModuloZazili,
    onIrAModulo: (ModuloZazili) -> Unit,
    onIrASaga: () -> Unit,
    onRepetir: () -> Unit
) {
    val mundo = RutaSaga.mundoDe(moduloSugerido)
    val color = colorDeModulo(moduloSugerido)
    val contenido = contenidoDe(color)
    val total = preguntas.size.coerceAtLeast(1)
    val mensaje = when {
        estado.aciertos >= total ->
            "¡Volaste parejo! Todos los mundos te dijeron «hola»."
        estado.aciertos >= total - 2 ->
            "Muy buen vuelo. Ya sabemos dónde brillas y dónde conviene acompañar."
        estado.aciertos > 0 ->
            "El vuelo de hoy nos dio pistas. Sin prisa: esto no es una nota."
        else ->
            "Gracias por intentarlo. El vuelo sirve para saber por dónde empezar, no para calificar."
    }

    Column {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZaziSpriteAnimado(
                        modifier = Modifier.size(96.dp),
                        estado = ZaziState.CELEBRACION
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¡Volaste 5 preguntas!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AmarilloSuave
                        ) {
                            Text(
                                text = "${estado.aciertos} de $total con respuesta chida",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = UniversoProfundo,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                )

                Spacer(Modifier.height(14.dp))
                preguntas.forEachIndexed { indice, pregunta ->
                    val acerto = estado.respuestas.getOrNull(indice) == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (acerto) VerdeDuo.copy(alpha = 0.18f) else AmarilloSuave
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (acerto) {
                                KawaiiCheckIcon(size = 16.dp, color = VerdeDuo)
                            } else {
                                KawaiiStarIcon(size = 16.dp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pregunta.modulo.titulo,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pregunta.nodo.nombre,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (acerto) "¡Lo sabías!" else "Para practicar",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (acerto) VerdeDuo else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (indice < preguntas.lastIndex) {
                        Spacer(Modifier.height(9.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = fondoSuaveModulo(moduloSugerido),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        IconoModulo(moduloSugerido, 30.dp)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Para empezar por aquí",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = moduloSugerido.titulo,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = mundo?.modulo?.lema ?: "Un mundo para volar a tu ritmo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ZaziButton(
                    text = "¡Vamos a ese mundo!",
                    onClick = {
                        GlobalAudioManager.playPop()
                        onIrAModulo(moduloSugerido)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    containerColor = color,
                    contentColor = contenido,
                    cornerRadius = 20.dp,
                    baseHeight = 5.dp,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZaziButton(
                text = "Volver a la Saga",
                onClick = {
                    GlobalAudioManager.playPop()
                    onIrASaga()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                cornerRadius = 18.dp,
                baseHeight = 4.dp,
                fontSize = 13.sp
            )
            ZaziButton(
                text = "Repetir el vuelo",
                onClick = {
                    GlobalAudioManager.playPop()
                    onRepetir()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                containerColor = LavandaClara,
                contentColor = UniversoProfundo,
                cornerRadius = 18.dp,
                baseHeight = 4.dp,
                fontSize = 13.sp
            )
        }
    }
}
