package com.example.zazili.presentation.cuentos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.audio.BgmEngine
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.BotonFlotanteTTS
import com.example.zazili.core.narrator.HistoriaCuentacuentos
import com.example.zazili.core.narrator.TTSManager
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.SoftCream
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/**
 * Lectura de una historia del Cuentacuentos.
 *
 * Al entrar, la música de fondo cambia a la pista asignada a la historia sin
 * tocar la preferencia guardada del niño. Al salir, [DisposableEffect]
 * devuelve la melodía principal y corta la voz para que ninguna lectura siga
 * sonando en la lista.
 *
 * El botón flotante de voz queda en la esquina superior derecha, encima del
 * encabezado, y el contenido empieza debajo de él: nunca tapa el texto.
 */
@Composable
fun StoryDetailScreen(
    historia: HistoriaCuentacuentos,
    ttsActivo: Boolean,
    onAlternarTts: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    var mostrarRespuesta by rememberSaveable(historia.id) { mutableStateOf(false) }

    LaunchedEffect(historia.id) {
        BgmEngine.playSpecificTrack(contexto, historia.bgmResId)
    }

    LaunchedEffect(historia.id, ttsActivo) {
        if (ttsActivo) {
            TTSManager.leer(historia.textoParaLeer())
        } else {
            TTSManager.detener()
        }
    }

    DisposableEffect(historia.id) {
        onDispose {
            TTSManager.detener()
            BgmEngine.resumeMainTheme(contexto)
        }
    }

    val salir: () -> Unit = {
        GlobalAudioManager.playPop()
        onVolver()
    }

    BackHandler(onBack = salir)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoCuento(
                historia = historia,
                onVolver = salir
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp)
            ) {
                Text(
                    text = historia.emoji,
                    fontSize = 44.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = historia.titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = historia.origen,
                    style = MaterialTheme.typography.labelMedium,
                    color = MagicalPurple,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))

                historia.parrafos.forEach { parrafo ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = parrafo,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AmarilloSuave,
                    border = BorderStroke(2.dp, AmarilloVibrante),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = "DATO CURIOSO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            color = UniversoProfundo.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = historia.datoCurioso,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniversoProfundo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RosaSuave.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text(
                            text = "LO QUE APRENDIMOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            color = UniversoProfundo.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = historia.moraleja,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniversoProfundo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Para platicar en familia",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = historia.pregunta,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))

                ZaziButton(
                    text = if (mostrarRespuesta) "Ocultar respuesta" else "Ver respuesta",
                    onClick = {
                        GlobalAudioManager.playPop()
                        mostrarRespuesta = !mostrarRespuesta
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    containerColor = if (mostrarRespuesta) LavandaMedia else VerdeDuo,
                    contentColor = if (mostrarRespuesta) UniversoProfundo else ZaziliWhite,
                    cornerRadius = 20.dp,
                    baseHeight = 5.dp,
                    fontSize = 17.sp
                )

                if (mostrarRespuesta) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SoftCream,
                        border = BorderStroke(2.dp, VerdeDuo.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = historia.respuesta,
                            style = MaterialTheme.typography.bodyLarge,
                            color = UniversoProfundo,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Cuento terminado. Puedes volver a leerlo cuando quieras.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        BotonFlotanteTTS(
            activo = ttsActivo,
            onAlternar = {
                GlobalAudioManager.playPop()
                onAlternarTts()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp)
        )
    }
}

@Composable
private fun EncabezadoCuento(
    historia: HistoriaCuentacuentos,
    onVolver: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 72.dp, top = 8.dp, bottom = 8.dp)
    ) {
        ZaziButton(
            text = "‹",
            onClick = onVolver,
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Volver a la lista de cuentos" },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 12.dp,
            baseHeight = 4.dp,
            fontSize = 26.sp
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cuentacuentos",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = historia.origen,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
