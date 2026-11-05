package com.example.zazili

import android.app.Application
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.narrator.StoryNarratorEngine
import com.example.zazili.core.narrator.ZaziOnnxVoiceEngine
import com.example.zazili.di.AppContainer

class ZaziliApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // El audio se instancia EXACTAMENTE una vez por proceso, fuera de
        // Compose; su ciclo de vida queda atado al de la app.
        GlobalAudioManager.initialize(this)
        // Narración con voz neural local (Piper/ONNX): una sola sesión para
        // cuentos, lecciones, juegos de lenguaje y chat. Sin voz del sistema.
        StoryNarratorEngine.initialize(this)
        // Voz global de Zazi para el chat y los altavoces de apoyo (DUA).
        ZaziOnnxVoiceEngine.initialize(this)
    }
}
