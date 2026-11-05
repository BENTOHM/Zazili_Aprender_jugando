package com.example.zazili

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.navigation.ZaziliApp

/**
 * Dueña del ciclo de vida del audio global.
 *
 * Los motores ([com.example.zazili.core.audio.AudioEngine] y
 * [com.example.zazili.core.audio.BgmEngine]) no retienen Activities ni se
 * registran por su cuenta: esta actividad observa el ciclo de vida del
 * PROCESO y ordena pausar al salir a segundo plano, reanudar al volver y
 * liberar todo cuando la app se cierra de verdad.
 *
 * No se libera en cambios de configuración para no cortar la música al girar
 * la pantalla; solo cuando [isFinishing] confirma el cierre real.
 */
class MainActivity : ComponentActivity() {

    private val observadorAudio = object : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            GlobalAudioManager.startBgm()
        }

        override fun onResume(owner: LifecycleOwner) {
            GlobalAudioManager.startBgm()
        }

        override fun onPause(owner: LifecycleOwner) {
            GlobalAudioManager.pauseBgm()
        }

        override fun onStop(owner: LifecycleOwner) {
            GlobalAudioManager.pauseBgm()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalAudioManager.initialize(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(observadorAudio)
        enableEdgeToEdge()
        setContent {
            ZaziliApp()
        }
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(observadorAudio)
        if (isFinishing) {
            GlobalAudioManager.release()
        }
        super.onDestroy()
    }
}
