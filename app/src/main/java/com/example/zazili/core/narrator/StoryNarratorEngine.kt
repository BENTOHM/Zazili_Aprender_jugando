package com.example.zazili.core.narrator

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Fachada del narrador de Zazili. Toda la síntesis vive en
 * [NeuralVoiceEngine] (Piper VITS local, estrictamente ONNX); esta capa
 * conserva la API histórica para las pantallas de lectura.
 */
object StoryNarratorEngine {

    val habilitado: StateFlow<Boolean>
        get() = NeuralVoiceEngine.habilitado

    fun initialize(context: Context) = NeuralVoiceEngine.initialize(context)

    fun speakHumanized(texto: String) = NeuralVoiceEngine.speakHumanized(texto)

    /** Alias de compatibilidad con las pantallas que ya narran. */
    fun narrar(texto: String) = NeuralVoiceEngine.speakHumanized(texto)

    fun detener() = NeuralVoiceEngine.detener()

    fun setHabilitado(valor: Boolean) = NeuralVoiceEngine.setHabilitado(valor)

    fun alternar() = NeuralVoiceEngine.alternar()

    /** Nombre del motor activo (diagnóstico en español). */
    fun vozActiva(): String = NeuralVoiceEngine.motorActivo()

    fun vozDisponible(): Boolean = NeuralVoiceEngine.vozDisponible()

    fun liberar() = NeuralVoiceEngine.liberar()
}
