package com.example.zazili.presentation.comun

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlinx.coroutines.delay

/**
 * Fuente única de las frases que Zazi dice después de cada respuesta.
 *
 * Sonaban repetitivas cuando cada pantalla escribía su propio texto; aquí
 * viven todas juntas para que cualquier reto de la app use el mismo tono:
 * humor ligero, español mexicano y una pizca de broma pasivo-agresiva que
 * jamás insulta al niño. Las frases se eligen con [random] en el momento de
 * mostrarlas, así que dos respuestas seguidas nunca se sienten iguales.
 */
object InGameFeedbackDictionary {

    /** Refuerzos para una respuesta correcta. Se rotan al azar. */
    val respuestasCorrectas: List<String> = listOf(
        "¡Cine puro! 🍿",
        "Ni Einstein lo haría tan rápido.",
        "¡Andas volando, fiera!",
        "100% de capacidad cerebral desbloqueada.",
        "Zazi está orgulloso (y un poco asustado de tu inteligencia).",
        "¡Pum! Le atinaste sin sudar.",
        "Tu cerebro acaba de hacer un backflip.",
        "Eso merece unos taquitos virtuales. 🌮",
        "Zazi aplaude con las patitas.",
        "Nivel: genio incomprendido.",
        "¿Seguro que no leíste las respuestas? 👀",
        "¡Qué nivel, carnal!",
        "Le diste en el clavo, sin martillo.",
        "Tu cerebrito está en modo turbo.",
        "¡Esa respuesta fue una obra de arte! 🎨",
        "Ya puedes presumir en el recreo.",
        "Zazi quiere chocar esos cinco. 🖐️",
        "Perfecto. Sospechosamente perfecto. 🤨"
    )

    /** Comentarios para una respuesta incorrecta, con cariño y algo de broma. */
    val respuestasIncorrectas: List<String> = listOf(
        "Ups... ¿el internet de tu cerebro tuvo lag? 📶",
        "Zazi cerró los ojos para no ver eso. Intenta de nuevo.",
        "¿Adivinando? Se vale, pero concéntrate. 🧐",
        "Casi... en un universo paralelo, esa era la respuesta.",
        "Respira. Parpadea. Vuelve a leer.",
        "Tu respuesta se fue de vacaciones. 🏖️",
        "Zazi no dice nada, pero su cara lo dice todo. 😐",
        "Eso fue valiente... pero no correcto.",
        "Le pegaste al poste, no a la portería. ⚽",
        "Hasta Zazi suspiró. Pero cree en ti.",
        "Mmm... ni cerca, pero se agradece el intento.",
        "Tu cerebro pidió un café antes de responder. ☕",
        "Esa respuesta salió del universo equivocado. 🪐",
        "¡Buen intento! Mal resultado.",
        "Zazi finge que no vio nada. 🤫",
        "No era esa, pero qué bonita seguridad.",
        "Pausa dramática... y otra oportunidad.",
        "El botón correcto te está esperando. Búscalo bien. 🔍"
    )

    /** Frases extra cuando el niño encadena varios aciertos. */
    val refuerzosRacha: List<String> = listOf(
        "¡Racha en llamas! 🔥",
        "Vas imparable, campeón.",
        "Zazi está viendo magia pura.",
        "Tres seguidas, ¡qué oso tan chido!",
        "Racha nivel leyenda. 🏆"
    )

    fun acierto(): String = respuestasCorrectas.random()

    fun error(): String = respuestasIncorrectas.random()

    fun refuerzoRacha(): String = refuerzosRacha.random()
}

/**
 * Aviso breve que baja desde arriba tras un acierto. Si no recibe [mensaje],
 * pide una frase nueva al diccionario con [InGameFeedbackDictionary.acierto],
 * de modo que cada aparición se siente distinta. Se oculta solo a los 2.2 s
 * para no tapar la actividad.
 */
@Composable
fun LessonSnackbar(
    visible: Boolean,
    esCorrecto: Boolean,
    modifier: Modifier = Modifier,
    mensaje: String? = null,
    onOcultar: () -> Unit = {}
) {
    val textoMostrado = remember(visible, mensaje, esCorrecto) {
        mensaje ?: if (esCorrecto) {
            InGameFeedbackDictionary.acierto()
        } else {
            InGameFeedbackDictionary.error()
        }
    }

    LaunchedEffect(visible, mensaje) {
        if (visible) {
            delay(2_200L)
            onOcultar()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (esCorrecto) VerdeDuo else CoralAviso,
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (esCorrecto) "🎉" else "🙈",
                    fontSize = 26.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = textoMostrado,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esCorrecto) ZaziliWhite else UniversoProfundo,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Hoja inferior para las respuestas que necesitan explicación.
 *
 * A diferencia de un simple "incorrecto", aquí el niño lee qué efecto tuvo su
 * elección y qué puede hacer en su lugar. Es la versión accesible de un
 * `BottomSheet`: sin gestos obligatorios, con botón grande y texto claro.
 */
@Composable
fun FeedbackBottomSheet(
    visible: Boolean,
    esCorrecto: Boolean,
    titulo: String,
    mensaje: String,
    textoBoton: String,
    onBoton: () -> Unit,
    modifier: Modifier = Modifier,
    mensajeDinamico: String? = null,
    onDescartar: (() -> Unit)? = null
) {
    val frase = remember(visible, mensajeDinamico, esCorrecto) {
        mensajeDinamico ?: if (esCorrecto) {
            InGameFeedbackDictionary.acierto()
        } else {
            InGameFeedbackDictionary.error()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(UniversoProfundo.copy(alpha = 0.46f))
                    .pointerInput(onDescartar) {
                        detectTapGestures { onDescartar?.invoke() }
                    }
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .padding(top = 10.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LavandaMedia)
                    )
                    Text(
                        text = if (esCorrecto) "🌟" else "💡",
                        fontSize = 44.sp
                    )
                    Text(
                        text = titulo,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = frase,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (esCorrecto) {
                            VerdeDuo.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = mensaje,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (esCorrecto) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                    ZaziButton(
                        text = textoBoton,
                        onClick = onBoton,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (esCorrecto) VerdeDuo else UniversoProfundo,
                        contentColor = ZaziliWhite
                    )
                }
            }
        }
    }
}
