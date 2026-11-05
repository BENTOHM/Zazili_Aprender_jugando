package com.example.zazili.presentation.onboarding

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiCenoteIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiHeartIcon
import com.example.zazili.core.ui.KawaiiMoonIcon
import com.example.zazili.core.ui.KawaiiNahualIcon
import com.example.zazili.core.ui.KawaiiSeedIcon
import com.example.zazili.core.ui.KawaiiSparkIcon
import com.example.zazili.core.ui.KawaiiSunIcon
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.usuario.EDAD_MAXIMA
import com.example.zazili.core.usuario.EDAD_MINIMA
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliCreamSoft
import com.example.zazili.ui.theme.ZaziliWhite

@Immutable
private data class AvatarOpcion(
    val id: String,
    val nombre: String,
    val color: Color
)

private val AVATARES = listOf(
    AvatarOpcion("chispa", "Chispa", AmarilloVibrante),
    AvatarOpcion("nahual", "Nahual", MagicalPurple),
    AvatarOpcion("cenote", "Cenote", AzulCielo),
    AvatarOpcion("sol", "Sol", AmarilloVibrante),
    AvatarOpcion("luna", "Luna", LavandaMedia),
    AvatarOpcion("semilla", "Semilla", MintGreen)
)

private val EDADES = (EDAD_MINIMA..EDAD_MAXIMA).toList()
private const val MAX_NOMBRE = 16

/**
 * Primer paso del onboarding: nombre, edad y compi de aventura.
 *
 * Todo es local y sin reloj. El botón 3D solo se activa cuando las tres
 * respuestas están completas; mientras DataStore escribe, las piezas quedan
 * bloqueadas para evitar dobles envíos.
 */
@Composable
fun ProfileCreationScreen(
    nombreInicial: String,
    edadInicial: Int,
    avatarInicial: String,
    guardando: Boolean,
    errorAlGuardar: Boolean,
    onContinuar: (nombre: String, edad: Int, avatarId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nombre by rememberSaveable { mutableStateOf(nombreInicial.take(MAX_NOMBRE)) }
    var edad by rememberSaveable {
        mutableIntStateOf(edadInicial.takeIf { it in EDAD_MINIMA..EDAD_MAXIMA } ?: 0)
    }
    var avatarId by rememberSaveable {
        mutableStateOf(avatarInicial.takeIf { id -> AVATARES.any { it.id == id } }.orEmpty())
    }
    val foco = LocalFocusManager.current

    val nombreLimpio = nombre.trim()
    val puedeEnviar = nombreLimpio.isNotEmpty() &&
        edad in EDAD_MINIMA..EDAD_MAXIMA &&
        avatarId.isNotEmpty() &&
        !guardando
    val falta = when {
        nombreLimpio.isEmpty() -> "Escribe tu nombre para seguir."
        edad !in EDAD_MINIMA..EDAD_MAXIMA -> "Elige cuántos años tienes."
        avatarId.isEmpty() -> "Elige a tu compi de aventura."
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        HeroBienvenida()

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SeccionTarjeta(
                titulo = "Tu nombre",
                ayuda = "Así te va a llamar Zazi. Puede ser tu apodo."
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nuevo -> nombre = nuevo.take(MAX_NOMBRE) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !guardando,
                    singleLine = true,
                    label = { Text("Nombre") },
                    placeholder = { Text("¿Cómo te llamas?") },
                    leadingIcon = { KawaiiHeartIcon(size = 26.dp) },
                    shape = RoundedCornerShape(18.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaziliWhite,
                        unfocusedContainerColor = ZaziliWhite,
                        disabledContainerColor = ZaziliWhite.copy(alpha = 0.7f),
                        focusedBorderColor = MagicalPurple,
                        unfocusedBorderColor = LavandaMedia,
                        disabledBorderColor = LavandaMedia.copy(alpha = 0.5f),
                        cursorColor = MagicalPurple,
                        focusedTextColor = UniversoProfundo,
                        unfocusedTextColor = UniversoProfundo,
                        disabledTextColor = UniversoProfundo.copy(alpha = 0.6f),
                        focusedLabelColor = MagicalPurple,
                        unfocusedLabelColor = UniversoProfundo.copy(alpha = 0.6f),
                        focusedPlaceholderColor = UniversoProfundo.copy(alpha = 0.45f),
                        unfocusedPlaceholderColor = UniversoProfundo.copy(alpha = 0.45f)
                    )
                )
            }

            SeccionTarjeta(
                titulo = "Tu edad",
                ayuda = "Toca tu edad. Sin prisa, Zazi no cuenta los segundos."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EDADES.forEach { opcion ->
                        val elegida = edad == opcion
                        ZaziButton(
                            text = "$opcion",
                            onClick = {
                                GlobalAudioManager.playPop()
                                edad = opcion
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            containerColor = if (elegida) VerdeDuo else LavandaClara,
                            contentColor = if (elegida) ZaziliWhite else UniversoProfundo,
                            cornerRadius = 16.dp,
                            baseHeight = 5.dp,
                            enabled = !guardando,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            SeccionTarjeta(
                titulo = "Tu compi de aventura",
                ayuda = "Elige el que más te guste. Puedes cambiarlo después."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AVATARES.chunked(3).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            fila.forEach { avatar ->
                                CeldaAvatar(
                                    avatar = avatar,
                                    seleccionado = avatarId == avatar.id,
                                    habilitada = !guardando,
                                    onClick = {
                                        GlobalAudioManager.playPop()
                                        avatarId = avatar.id
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - fila.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            ZaziButton(
                text = if (guardando) "Guardando…" else "¡Órale, vamos a jugar!",
                onClick = {
                    foco.clearFocus()
                    GlobalAudioManager.playPop()
                    onContinuar(nombreLimpio, edad, avatarId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                cornerRadius = 26.dp,
                baseHeight = 8.dp,
                enabled = puedeEnviar,
                fontSize = 19.sp
            )

            if (falta.isNotEmpty() && !guardando) {
                Text(
                    text = falta,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniversoProfundo.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center
                )
            }

            if (errorAlGuardar) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CoralAviso.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pudimos guardar tu perfil. ¿Lo intentamos otra vez?",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Text(
                text = "Todo se queda en este dispositivo. Sin cuentas y sin internet.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun HeroBienvenida() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(UniversoProfundo, MagicalPurple)
                )
            )
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PASO 1 DE 2",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintGreen,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "¡Órale,\nvamos a jugar!",
                    style = MaterialTheme.typography.displaySmall,
                    color = ZaziliWhite,
                    fontWeight = FontWeight.Black,
                    lineHeight = 38.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Primero tú. Luego, tres juegos rapiditos con Zazi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZaziliWhite.copy(alpha = 0.84f),
                    lineHeight = 19.sp
                )
            }
            Spacer(Modifier.width(6.dp))
            ZaziSpriteAnimado(
                size = 118.dp,
                estado = ZaziState.BASE,
                animar = true,
                descripcion = "Zazi te da la bienvenida con las alas abiertas"
            )
        }
    }
}

@Composable
private fun SeccionTarjeta(
    titulo: String,
    ayuda: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = ZaziliCreamSoft,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = UniversoProfundo,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = ayuda,
                style = MaterialTheme.typography.bodySmall,
                color = UniversoProfundo.copy(alpha = 0.62f)
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun CeldaAvatar(
    avatar: AvatarOpcion,
    seleccionado: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tamanoIcono: Dp = 40.dp
) {
    val escala by animateFloatAsState(
        targetValue = if (seleccionado) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escalaAvatar"
    )
    val forma = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(forma)
            .background(
                if (seleccionado) {
                    avatar.color.copy(alpha = 0.26f)
                } else {
                    LavandaClara.copy(alpha = 0.55f)
                }
            )
            .border(
                width = if (seleccionado) 3.dp else 1.dp,
                color = if (seleccionado) MagicalPurple else LavandaMedia.copy(alpha = 0.55f),
                shape = forma
            )
            .selectable(
                selected = seleccionado,
                enabled = habilitada,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 11.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(avatar.color.copy(alpha = 0.30f)),
            contentAlignment = Alignment.Center
        ) {
            IconoAvatar(avatar.id, tamanoIcono)
            if (seleccionado) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MagicalPurple),
                    contentAlignment = Alignment.Center
                ) {
                    KawaiiCheckIcon(size = 13.dp, color = ZaziliWhite)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = avatar.nombre,
            style = MaterialTheme.typography.labelLarge,
            color = UniversoProfundo,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun IconoAvatar(id: String, size: Dp) {
    when (id) {
        "chispa" -> KawaiiSparkIcon(size = size)
        "nahual" -> KawaiiNahualIcon(size = size)
        "cenote" -> KawaiiCenoteIcon(size = size)
        "sol" -> KawaiiSunIcon(size = size)
        "luna" -> KawaiiMoonIcon(size = size)
        else -> KawaiiSeedIcon(size = size)
    }
}
