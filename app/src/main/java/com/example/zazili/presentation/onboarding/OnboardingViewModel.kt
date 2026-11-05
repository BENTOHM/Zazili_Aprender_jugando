package com.example.zazili.presentation.onboarding

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zazili.core.usuario.EDAD_MAXIMA
import com.example.zazili.core.usuario.EDAD_MINIMA
import com.example.zazili.domain.avatar.AvatarCategory
import com.example.zazili.domain.avatar.AvatarContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del primer paso del onboarding.
 *
 * [puedeContinuar] es la única fuente de verdad del botón "¡Listo, a jugar!":
 * solo pide nombre y edad. El look ya viene completo con piezas gratis, así que
 * una ranura vacía nunca puede dejar al niño atorado en la compuerta.
 */
@Immutable
data class OnboardingUiState(
    val nombre: String = "",
    val edad: Int = 0,
    val seleccion: Map<AvatarCategory, String> = emptyMap(),
    val guardando: Boolean = false,
    val error: String? = null
) {
    val nombreLimpio: String get() = nombre.trim()
    val edadValida: Boolean get() = edad in EDAD_MINIMA..EDAD_MAXIMA
    val puedeContinuar: Boolean
        get() = nombreLimpio.isNotEmpty() && edadValida && !guardando
}

/** Payload del evento que dispara la navegación al examen. */
data class OnboardingGuardado(
    val nombre: String,
    val edad: Int,
    val seleccion: Map<AvatarCategory, String>
)

/**
 * Estado y persistencia del avatar inicial.
 *
 * La escritura ocurre en [Dispatchers.IO] dentro de [viewModelScope] y el
 * resultado viaja como evento de un solo uso: la pantalla no navega hasta que
 * DataStore confirmó el perfil, el equipo y el bono de bienvenida.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val onboarding = OnboardingFlowManager.obtener(application)

    private val _estado = MutableStateFlow(OnboardingUiState())
    val estado: StateFlow<OnboardingUiState> = _estado.asStateFlow()

    private val _guardados = Channel<OnboardingGuardado>(Channel.BUFFERED)
    val guardados = _guardados.receiveAsFlow()

    private var sembrado = false

    fun sembrar(
        nombreInicial: String,
        edadInicial: Int,
        equipamientoInicial: Map<AvatarCategory, String>
    ) {
        if (sembrado) return
        sembrado = true
        _estado.update { actual ->
            actual.copy(
                nombre = actual.nombre.ifBlank {
                    nombreInicial.trim().take(MAX_NOMBRE_ONBOARDING)
                },
                edad = actual.edad.takeIf { it in EDAD_MINIMA..EDAD_MAXIMA }
                    ?: edadInicial.takeIf { it in EDAD_MINIMA..EDAD_MAXIMA }
                    ?: 0,
                seleccion = completarSeleccion(equipamientoInicial)
            )
        }
    }

    fun escribirNombre(texto: String) {
        _estado.update {
            it.copy(
                nombre = texto.take(MAX_NOMBRE_ONBOARDING),
                error = null
            )
        }
    }

    fun elegirEdad(edad: Int) {
        if (edad !in EDAD_MINIMA..EDAD_MAXIMA) return
        _estado.update { it.copy(edad = edad, error = null) }
    }

    fun elegirItem(categoria: AvatarCategory, itemId: String) {
        val item = AvatarContentFactory.porId(itemId) ?: return
        if (item.category != categoria) return
        if (!item.isUnlockedByDefault && item.price > 0) return
        _estado.update {
            it.copy(seleccion = it.seleccion + (categoria to itemId), error = null)
        }
    }

    fun guardar() {
        val actual = _estado.value
        if (!actual.puedeContinuar) return
        val seleccion = completarSeleccion(actual.seleccion)
        _estado.update { it.copy(guardando = true, error = null, seleccion = seleccion) }

        viewModelScope.launch(Dispatchers.IO) {
            when (
                val resultado = onboarding.guardarAvatarInicial(
                    nombre = actual.nombreLimpio,
                    edad = actual.edad,
                    seleccion = seleccion
                )
            ) {
                ResultadoCreacionAvatar.Exito -> {
                    _estado.update { it.copy(guardando = false) }
                    _guardados.send(
                        OnboardingGuardado(
                            nombre = actual.nombreLimpio,
                            edad = actual.edad,
                            seleccion = seleccion
                        )
                    )
                }

                is ResultadoCreacionAvatar.Invalido ->
                    _estado.update {
                        it.copy(guardando = false, error = resultado.motivo)
                    }

                ResultadoCreacionAvatar.ErrorAlGuardar ->
                    _estado.update {
                        it.copy(
                            guardando = false,
                            error = "No pudimos guardar tu avatar. ¿Lo intentamos otra vez?"
                        )
                    }
            }
        }
    }

    /**
     * Completa las ranuras con la primera pieza gratis de cada categoría.
     *
     * La cabeza no viene en [AvatarContentFactory.predeterminados] a propósito,
     * así que aquí se elige la coleta gratis solo si el niño no escogió nada:
     * el botón no depende del look, pero el avatar guardado queda completo.
     */
    private fun completarSeleccion(
        base: Map<AvatarCategory, String>
    ): Map<AvatarCategory, String> {
        val predeterminados = AvatarContentFactory.predeterminados()
            .associate { it.category to it.id }
        var seleccion = predeterminados + base
        AvatarCategory.entries.forEach { categoria ->
            val item = seleccion[categoria]?.let(AvatarContentFactory::porId)
            val sirve = item != null &&
                item.category == categoria &&
                (item.isUnlockedByDefault || item.price == 0)
            if (!sirve) {
                AvatarContentFactory.porCategoria(categoria)
                    .firstOrNull { it.isUnlockedByDefault || it.price == 0 }
                    ?.let { gratis -> seleccion = seleccion + (categoria to gratis.id) }
            }
        }
        return seleccion
    }
}

private const val MAX_NOMBRE_ONBOARDING = 16
