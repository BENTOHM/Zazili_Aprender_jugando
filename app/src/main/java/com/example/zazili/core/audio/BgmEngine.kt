package com.example.zazili.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RawRes
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Motor de música de fondo de Zazili.
 *
 *  - Una sola instancia de [MediaPlayer] para todo el proceso. La creación y
 *    el `prepare()` corren en [Dispatchers.IO]; el cambio de pista nunca
 *    bloquea el hilo de interfaz.
 *  - Cada cambio lleva un token de generación: una preparación vieja jamás
 *    puede pisar la pista nueva ni dejar un reproductor huérfano.
 *  - [AudioManager.OnAudioFocusChangeListener]: pérdida permanente pausa y
 *    libera, pérdida temporal pausa, duck baja al diez por ciento y la
 *    recuperación del foco restaura el volumen y reanuda.
 *  - `start`, `pause`, `stop` y `release` viven dentro de try-catch: un
 *    reproductor liberado nunca provoca `IllegalStateException`.
 *
 * El ciclo de vida de la app lo dispara `MainActivity` a través de
 * [GlobalAudioManager]; este motor no se registra por su cuenta.
 */
object BgmEngine {

    private const val TAG = "ZaziliBgm"
    private const val PASOS_APARICION = 8
    private const val MS_APARICION = 26L
    private const val FACTOR_DUCK = 0.10f

    private val bloqueo = Any()
    private val manejador = Handler(Looper.getMainLooper())
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tokenPista = AtomicInteger(0)

    @Volatile
    private var inicializado = false

    @Volatile
    private var liberado = false

    @Volatile
    private var appContext: Context? = null

    private var audioManager: AudioManager? = null
    private var solicitudFoco: AudioFocusRequest? = null

    private var reproductor: MediaPlayer? = null

    @Volatile
    private var preparado = false

    @Volatile
    private var pistaDeseada = 0

    @Volatile
    private var pistaPrincipal = 0

    @Volatile
    private var volumen = 0.5f

    @Volatile
    private var duck = false

    @Volatile
    private var pausadoPorVisibilidad = false

    @Volatile
    private var pausadoPorFoco = false

    private val _pistaActualRes = MutableStateFlow(0)
    val pistaActualRes: StateFlow<Int> = _pistaActualRes.asStateFlow()

    private val oyenteFoco = AudioManager.OnAudioFocusChangeListener { cambio ->
        manejarCambioDeFoco(cambio)
    }

    /** Prepara el audio y el foco una sola vez. Idempotente. */
    @Synchronized
    fun initialize(context: Context) {
        if (inicializado) return
        val app = context.applicationContext
        appContext = app
        liberado = false
        pausadoPorVisibilidad = false
        pausadoPorFoco = false
        duck = false

        audioManager = try {
            app.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (t: Throwable) {
            Log.w(TAG, "AudioManager no disponible; la música seguirá sin foco", t)
            null
        }

        solicitudFoco = try {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(oyenteFoco, manejador)
                .setWillPauseWhenDucked(false)
                .build()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo preparar la solicitud de foco de audio", t)
            null
        }

        inicializado = true
    }

    /** Guarda la melodía elegida por el niño para [resumeMainTheme]. */
    fun setPistaPrincipal(@RawRes resId: Int) {
        pistaPrincipal = resId
        if (pistaDeseada == 0) pistaDeseada = resId
    }

    /** Pista puntual de un cuento o pantalla especial. No se persiste. */
    fun playSpecificTrack(context: Context, @RawRes resId: Int) {
        playTrack(context, resId)
    }

    /** Regresa a la melodía elegida por el niño. */
    fun resumeMainTheme(context: Context) {
        val principal = pistaPrincipal
        if (principal != 0) playTrack(context, principal)
    }

    /**
     * Cambia a [resId]. Si la pista ya está lista, solo se reanuda; si es
     * distinta, se prepara en segundo plano y se instala al terminar.
     */
    fun playTrack(context: Context, @RawRes resId: Int) {
        if (resId == 0) return
        initialize(context)
        if (liberado) return

        var token = 0
        synchronized(bloqueo) {
            if (liberado) return
            val actual = reproductor
            if (resId == pistaDeseada && actual != null && preparado) {
                if (!pausadoPorVisibilidad && !pausadoPorFoco) {
                    pedirFoco()
                    reproducirSeguro(actual)
                }
                return
            }
            token = tokenPista.incrementAndGet()
            pistaDeseada = resId
            if (pausadoPorVisibilidad) return
        }

        if (!pausadoPorFoco) pedirFoco()

        val app = context.applicationContext
        alcance.launch {
            val nuevo = crearReproductor(app, resId)
            if (nuevo == null) return@launch
            if (token != tokenPista.get() || liberado) {
                liberarReproductor(nuevo)
                return@launch
            }
            withContext(Dispatchers.Main) {
                instalarPista(nuevo, resId, token)
            }
        }
    }

    /** Crea y prepara el reproductor fuera del hilo de interfaz. */
    private fun crearReproductor(context: Context, @RawRes resId: Int): MediaPlayer? {
        val creado = try {
            MediaPlayer()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo crear el reproductor para la pista $resId", t)
            return null
        }
        return try {
            creado.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            creado.setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/$resId")
            )
            creado.isLooping = true
            creado.setVolume(0f, 0f)
            creado.prepare()
            creado
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo preparar la pista BGM $resId", t)
            liberarReproductor(creado)
            null
        }
    }

    /** Instala la pista nueva en el hilo principal, sin dejar huecos audibles. */
    private fun instalarPista(nuevo: MediaPlayer, resId: Int, token: Int) {
        var anterior: MediaPlayer? = null
        var puedeSonar = false
        synchronized(bloqueo) {
            if (liberado || token != tokenPista.get()) {
                liberarReproductor(nuevo)
                return
            }
            anterior = reproductor
            reproductor = nuevo
            preparado = true
            _pistaActualRes.value = resId
            puedeSonar = !pausadoPorVisibilidad && !pausadoPorFoco
            if (puedeSonar) iniciarSeguro(nuevo)
        }
        if (puedeSonar) aparecer(nuevo, token)
        liberarReproductor(anterior)
    }

    /** Pausa la pista actual sin destruirla. */
    fun pauseBgm() {
        if (!inicializado || liberado) return
        pausadoPorVisibilidad = true
        synchronized(bloqueo) {
            pausarActual()
        }
    }

    /** Reanuda la pista deseada al volver a primer plano. */
    fun startBgm() {
        if (!inicializado || liberado) return
        pausadoPorVisibilidad = false
        if (pausadoPorFoco) return

        var actual: MediaPlayer? = null
        var listo = false
        synchronized(bloqueo) {
            actual = reproductor
            listo = preparado
        }
        val vivo = actual
        if (vivo != null && listo) {
            pedirFoco()
            reproducirSeguro(vivo)
            return
        }

        val app = appContext ?: return
        val destino = if (pistaDeseada != 0) pistaDeseada else pistaPrincipal
        if (destino != 0) playTrack(app, destino)
    }

    /** Volumen persistido del niño, entre 0 y 1. */
    fun setVolume(valor: Float) {
        volumen = valor.coerceIn(0f, 1f)
        aplicarVolumenActual()
    }

    private fun volumenEfectivo(): Float = volumen * if (duck) FACTOR_DUCK else 1f

    private fun manejarCambioDeFoco(cambio: Int) {
        if (liberado) return
        when (cambio) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausadoPorFoco = true
                synchronized(bloqueo) {
                    pausarActual()
                    liberarReproductor(reproductor)
                    reproductor = null
                    preparado = false
                    _pistaActualRes.value = 0
                }
                Log.i(TAG, "Foco de audio perdido; música pausada y liberada")
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausadoPorFoco = true
                synchronized(bloqueo) {
                    pausarActual()
                }
                Log.i(TAG, "Foco de audio temporalmente perdido; música pausada")
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                duck = true
                aplicarVolumenActual()
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                duck = false
                pausadoPorFoco = false
                if (!pausadoPorVisibilidad) {
                    var actual: MediaPlayer? = null
                    var listo = false
                    synchronized(bloqueo) {
                        actual = reproductor
                        listo = preparado
                    }
                    val vivo = actual
                    if (vivo != null && listo) {
                        reproducirSeguro(vivo)
                    } else {
                        val app = appContext
                        val destino = if (pistaDeseada != 0) pistaDeseada else pistaPrincipal
                        if (app != null && destino != 0) playTrack(app, destino)
                    }
                } else {
                    aplicarVolumenActual()
                }
            }
        }
    }

    private fun pedirFoco(): Boolean {
        val administrador = audioManager ?: return false
        val solicitud = solicitudFoco ?: return false
        return try {
            administrador.requestAudioFocus(solicitud) ==
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo solicitar el foco de audio", t)
            false
        }
    }

    private fun abandonarFoco() {
        val administrador = audioManager ?: return
        val solicitud = solicitudFoco ?: return
        try {
            administrador.abandonAudioFocusRequest(solicitud)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo abandonar el foco de audio", t)
        }
    }

    private fun reproducirSeguro(player: MediaPlayer) {
        try {
            if (liberado || player !== reproductor || !preparado) return
            if (!player.isPlaying) player.start()
            player.setVolume(volumenEfectivo(), volumenEfectivo())
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo reanudar la música de fondo", t)
        }
    }

    private fun iniciarSeguro(player: MediaPlayer) {
        try {
            if (liberado || player !== reproductor) return
            if (!player.isPlaying) player.start()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo iniciar la música de fondo", t)
        }
    }

    private fun pausarActual() {
        val player = reproductor ?: return
        try {
            if (player.isPlaying) player.pause()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo pausar la música de fondo", t)
        }
    }

    private fun aplicarVolumenActual() {
        val player = reproductor ?: return
        if (!preparado) return
        try {
            player.setVolume(volumenEfectivo(), volumenEfectivo())
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo ajustar el volumen de la música", t)
        }
    }

    /** Aparición suave desde cero hasta el volumen efectivo. */
    private fun aparecer(player: MediaPlayer, token: Int) {
        var paso = 0
        val runnable = object : Runnable {
            override fun run() {
                if (liberado || token != tokenPista.get() || player !== reproductor) return
                paso++
                val factor = (paso.toFloat() / PASOS_APARICION).coerceIn(0f, 1f)
                val objetivo = volumenEfectivo() * factor
                try {
                    player.setVolume(objetivo, objetivo)
                } catch (_: Throwable) {
                    return
                }
                if (paso < PASOS_APARICION) manejador.postDelayed(this, MS_APARICION)
            }
        }
        manejador.post(runnable)
    }

    private fun liberarReproductor(player: MediaPlayer?) {
        if (player == null) return
        try {
            if (player.isPlaying) player.stop()
        } catch (_: Throwable) {
        }
        try {
            player.reset()
        } catch (_: Throwable) {
        }
        try {
            player.release()
        } catch (_: Throwable) {
        }
    }

    /**
     * Libera el reproductor y abandona el foco. Reservado para el cierre real
     * de la app; después de llamarlo, [initialize] puede reconstruir el motor.
     */
    @Synchronized
    fun release() {
        liberado = true
        tokenPista.incrementAndGet()
        manejador.removeCallbacksAndMessages(null)
        synchronized(bloqueo) {
            liberarReproductor(reproductor)
            reproductor = null
            preparado = false
            _pistaActualRes.value = 0
        }
        abandonarFoco()
        audioManager = null
        solicitudFoco = null
        appContext = null
        pistaDeseada = 0
        pistaPrincipal = 0
        pausadoPorVisibilidad = false
        pausadoPorFoco = false
        duck = false
        inicializado = false
    }
}
