package com.example.zazili.presentation.onboarding

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.usuario.EDAD_MAXIMA
import com.example.zazili.core.usuario.EDAD_MINIMA
import com.example.zazili.core.usuario.UserManager
import com.example.zazili.domain.avatar.AvatarCategory
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.avatar.AvatarItem
import com.example.zazili.domain.diagnostico.ResultadoDiagnostico
import com.example.zazili.domain.economia.ZazolEconomyManager
import com.example.zazili.presentation.avatar.ChildAvatarRenderer
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliCreamSoft
import com.example.zazili.ui.theme.ZaziliWhite
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

private val Context.dataStoreOnboarding: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_onboarding"
)

private val CLAVE_AVATAR_INICIAL = booleanPreferencesKey("avatar_inicial_creado")
private val CLAVE_BONO_INICIAL = booleanPreferencesKey("bono_inicial_entregado")

/** Resultado de intentar guardar el avatar del primer arranque. */
sealed interface ResultadoCreacionAvatar {
    data object Exito : ResultadoCreacionAvatar
    data class Invalido(val motivo: String) : ResultadoCreacionAvatar
    data object ErrorAlGuardar : ResultadoCreacionAvatar
}

/**
 * Estado de la compuerta de primera vez.
 *
 * `primeraVez` manda el flujo: mientras sea `true` el niño pasa por avatar y
 * "El Vuelo de Prueba"; en el instante en que [UserManager] cierra el
 * onboarding, el estado cambia y la navegación raíz abandona por completo el
 * grafo de bienvenida. [avatarCreado] evita repetir la creación si la app se
 * cerró entre el avatar y el diagnóstico.
 */
@Immutable
data class EstadoOnboarding(
    val cargando: Boolean = true,
    val primeraVez: Boolean = true,
    val avatarCreado: Boolean = false,
    val bonoEntregado: Boolean = false,
    val nombre: String = "",
    val edad: Int = 0,
    val equipamientoInicial: Map<AvatarCategory, String> = emptyMap()
) {
    val necesitaCrearAvatar: Boolean get() = primeraVez && !avatarCreado
    val necesitaDiagnostico: Boolean get() = primeraVez && avatarCreado
}

/**
 * Lógica de la primera vez: perfil, avatar gratis y bono de bienvenida.
 *
 * Todo es local. La selección inicial solo acepta piezas gratis del catálogo
 * ([AvatarItem.isUnlockedByDefault] o precio cero); los artículos de compra se
 * abren después en la Tienda de Zazi. El bono de [BONO_INICIAL] Zazoles se
 * entrega UNA sola vez y queda marcado en DataStore.
 */
class OnboardingFlowManager private constructor(context: Context) {

    private val aplicacion = context.applicationContext
    private val dataStore = aplicacion.dataStoreOnboarding
    private val usuario: UserManager =
        (aplicacion as? ZaziliApplication)?.container?.userManager ?: UserManager(aplicacion)
    private val economia = ZazolEconomyManager.obtener(aplicacion)
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val estado: StateFlow<EstadoOnboarding> = combine(
        usuario.perfil,
        dataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        },
        economia.estado
    ) { perfil, preferencias, bolsa ->
        if (perfil == null) {
            EstadoOnboarding()
        } else {
            EstadoOnboarding(
                cargando = false,
                primeraVez = perfil.primeraVez,
                avatarCreado = preferencias[CLAVE_AVATAR_INICIAL] ?: false,
                bonoEntregado = preferencias[CLAVE_BONO_INICIAL] ?: false,
                nombre = perfil.nombre,
                edad = perfil.edad,
                equipamientoInicial = bolsa.equipamiento
            )
        }
    }.stateIn(
        scope = alcance,
        started = SharingStarted.Eagerly,
        initialValue = EstadoOnboarding()
    )

    /**
     * Valida, guarda perfil + avatar y entrega el bono inicial.
     *
     * La comprobación de piezas gratis ocurre antes de tocar DataStore: si la
     * selección trae algo de pago (o le falta una ranura), no se escribe nada.
     */
    suspend fun guardarAvatarInicial(
        nombre: String,
        edad: Int,
        seleccion: Map<AvatarCategory, String>
    ): ResultadoCreacionAvatar {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isEmpty()) {
            return ResultadoCreacionAvatar.Invalido("Escribe tu nombre para seguir.")
        }
        if (edad !in EDAD_MINIMA..EDAD_MAXIMA) {
            return ResultadoCreacionAvatar.Invalido("Elige cuántos años tienes.")
        }
        val completa = AvatarCategory.entries.all { categoria ->
            val item = seleccion[categoria]?.let(AvatarContentFactory::porId)
            item != null &&
                item.category == categoria &&
                (item.isUnlockedByDefault || item.price == 0)
        }
        if (!completa) {
            return ResultadoCreacionAvatar.Invalido(
                "Elige un look gratis para cada parte de tu avatar."
            )
        }

        return try {
            val avatarId = AvatarCategory.entries.joinToString("&") { categoria ->
                "${categoria.clave}=${seleccion.getValue(categoria)}"
            }
            usuario.guardarPerfil(nombreLimpio, edad, avatarId)

            AvatarCategory.entries.forEach { categoria ->
                economia.equipar(categoria, seleccion.getValue(categoria))
            }

            val bonoEntregado = dataStore.data.first()[CLAVE_BONO_INICIAL] ?: false
            if (!bonoEntregado) {
                economia.addZazoles(BONO_INICIAL)
                dataStore.edit { preferencias ->
                    preferencias[CLAVE_BONO_INICIAL] = true
                }
            }

            dataStore.edit { preferencias ->
                preferencias[CLAVE_AVATAR_INICIAL] = true
            }
            ResultadoCreacionAvatar.Exito
        } catch (error: IOException) {
            Log.w(TAG, "No se pudo guardar el avatar inicial", error)
            ResultadoCreacionAvatar.ErrorAlGuardar
        }
    }

    /**
     * Cierra la compuerta con el resultado del diagnóstico. Si falla, la
     * bandera de primera vez no cambia y la UI puede reintentar.
     */
    suspend fun completarDiagnostico(resultado: ResultadoDiagnostico): Boolean =
        try {
            usuario.completarOnboarding(resultado)
            true
        } catch (error: IOException) {
            Log.w(TAG, "No se pudo cerrar el onboarding", error)
            false
        }

    companion object {
        const val BONO_INICIAL = 50
        private const val TAG = "OnboardingFlowManager"

        @Volatile
        private var instancia: OnboardingFlowManager? = null

        fun obtener(context: Context): OnboardingFlowManager =
            instancia ?: synchronized(this) {
                instancia ?: OnboardingFlowManager(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}

/**
 * Paso 1 del onboarding: nombre, edad y avatar con piezas gratis.
 *
 * El nombre y la edad son lo único que bloquea el botón: el look se completa
 * solo con la primera pieza gratis de cada ranura, así que una categoría sin
 * elegir no puede dejar al niño atorado. La vista previa usa el mismo
 * [ChildAvatarRenderer] que la casita y la tienda, y la escritura ocurre en
 * [OnboardingViewModel] antes de avisar que ya se puede navegar.
 */
@Composable
fun AvatarCreationScreen(
    nombreInicial: String,
    edadInicial: Int,
    equipamientoInicial: Map<AvatarCategory, String>,
    onContinuar: (nombre: String, edad: Int, seleccion: Map<AvatarCategory, String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sembrar(nombreInicial, edadInicial, equipamientoInicial)
        viewModel.guardados.collect { guardado ->
            onContinuar(guardado.nombre, guardado.edad, guardado.seleccion)
        }
    }

    var categoriaClave by rememberSaveable { mutableStateOf(AvatarCategory.PIEL.clave) }
    val categoria = AvatarCategory.entries.firstOrNull { it.clave == categoriaClave }
        ?: AvatarCategory.PIEL

    val equipamiento = remember(estado.seleccion) {
        AvatarContentFactory.equipamientoDesde(estado.seleccion)
    }
    val opciones = remember(categoria) {
        AvatarContentFactory.porCategoria(categoria).filter { item ->
            item.isUnlockedByDefault || item.price == 0
        }
    }
    val foco = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        HeroCreacion(equipamiento = equipamiento)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            SeccionCreacion(
                titulo = "Tu nombre",
                ayuda = "Así te va a llamar Zazi. Puede ser tu apodo."
            ) {
                OutlinedTextField(
                    value = estado.nombre,
                    onValueChange = viewModel::escribirNombre,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !estado.guardando,
                    singleLine = true,
                    label = { Text("Nombre") },
                    placeholder = { Text("¿Cómo te llamas?") },
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

            SeccionCreacion(
                titulo = "Tu edad",
                ayuda = "Toca tu edad. Sin prisa, Zazi no cuenta los segundos."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (EDAD_MINIMA..EDAD_MAXIMA).forEach { opcion ->
                        val elegida = estado.edad == opcion
                        ZaziButton(
                            text = "$opcion",
                            onClick = {
                                GlobalAudioManager.playPop()
                                viewModel.elegirEdad(opcion)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            containerColor = if (elegida) VerdeDuo else LavandaClara,
                            contentColor = if (elegida) ZaziliWhite else UniversoProfundo,
                            cornerRadius = 16.dp,
                            baseHeight = 5.dp,
                            enabled = !estado.guardando,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            SeccionCreacion(
                titulo = "Tu look gratis",
                ayuda = "Elige una pieza de cada parte. Todo esto ya viene en tu mochila."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AvatarCategory.entries.forEach { ranura ->
                            val activa = ranura == categoria
                            ZaziButton(
                                text = ranura.etiqueta,
                                onClick = {
                                    GlobalAudioManager.playPop()
                                    categoriaClave = ranura.clave
                                },
                                modifier = Modifier.height(42.dp),
                                containerColor = if (activa) MagicalPurple else LavandaClara,
                                contentColor = if (activa) ZaziliWhite else UniversoProfundo,
                                cornerRadius = 18.dp,
                                baseHeight = 4.dp,
                                enabled = !estado.guardando,
                                fontSize = 12.sp
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = ranura.emoji, fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = ranura.etiqueta,
                                        color = if (activa) ZaziliWhite else UniversoProfundo,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(items = opciones, key = { it.id }) { item ->
                            val elegido = estado.seleccion[categoria] == item.id
                            CeldaLookGratis(
                                item = item,
                                elegido = elegido,
                                habilitada = !estado.guardando,
                                onClick = {
                                    GlobalAudioManager.playPop()
                                    viewModel.elegirItem(categoria, item.id)
                                    foco.clearFocus()
                                }
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = AmarilloVibrante.copy(alpha = 0.20f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    KawaiiCoinIcon(size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "+50 Zazoles de regalo",
                            style = MaterialTheme.typography.titleMedium,
                            color = UniversoProfundo,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Por crear tu avatar. Después puedes comprar más en la Tienda de Zazi.",
                            style = MaterialTheme.typography.labelSmall,
                            color = UniversoProfundo.copy(alpha = 0.68f)
                        )
                    }
                }
            }

            val mensajeError = estado.error
            if (mensajeError != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CoralAviso.copy(alpha = 0.18f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mensajeError,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniversoProfundo,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            ZaziButton(
                text = if (estado.guardando) "Guardando…" else "¡Listo, a jugar!",
                onClick = {
                    foco.clearFocus()
                    GlobalAudioManager.playPop()
                    viewModel.guardar()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                containerColor = VerdeDuo,
                contentColor = ZaziliWhite,
                cornerRadius = 26.dp,
                baseHeight = 8.dp,
                enabled = estado.puedeContinuar,
                fontSize = 19.sp
            )

            Text(
                text = "Todo se queda en este dispositivo. Sin cuentas y sin internet.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroCreacion(equipamiento: com.example.zazili.domain.avatar.EquipamientoAvatar) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Brush.linearGradient(colors = listOf(UniversoProfundo, MagicalPurple)))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PASO 1 DE 2 · TU AVATAR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintGreen,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "¡Crea tu\nZazi!",
                    style = MaterialTheme.typography.displaySmall,
                    color = ZaziliWhite,
                    fontWeight = FontWeight.Black,
                    lineHeight = 38.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Elige nombre, edad y tu look gratis. El examen va después.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZaziliWhite.copy(alpha = 0.84f),
                    lineHeight = 19.sp
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 118.dp, height = 150.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(ZaziliWhite.copy(alpha = 0.14f))
            ) {
                ChildAvatarRenderer(
                    equipamiento = equipamiento,
                    modifier = Modifier.fillMaxSize(),
                    mostrarFondo = false,
                    animar = true,
                    descripcion = "Vista previa de tu nuevo avatar"
                )
            }
        }
    }
}

@Composable
private fun SeccionCreacion(
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
private fun CeldaLookGratis(
    item: AvatarItem,
    elegido: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(106.dp)
            .clip(forma)
            .background(if (elegido) MintGreen.copy(alpha = 0.35f) else ZaziliWhite)
            .border(
                width = if (elegido) 2.5.dp else 1.dp,
                color = if (elegido) VerdeDuo else LavandaMedia.copy(alpha = 0.6f),
                shape = forma
            )
            .selectable(
                selected = elegido,
                enabled = habilitada,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (elegido) VerdeDuo else LavandaClara),
            contentAlignment = Alignment.Center
        ) {
            if (elegido) {
                KawaiiCheckIcon(size = 18.dp, color = ZaziliWhite)
            } else {
                Text(text = item.category.emoji, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            color = UniversoProfundo,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (elegido) "Puesto" else "Gratis",
            style = MaterialTheme.typography.labelSmall,
            color = if (elegido) VerdeDuo else UniversoProfundo.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold
        )
    }
}
