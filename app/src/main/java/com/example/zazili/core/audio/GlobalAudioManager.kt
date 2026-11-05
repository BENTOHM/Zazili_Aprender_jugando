package com.example.zazili.core.audio

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.zazili.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Los cinco estilos de música de fondo que el niño puede elegir. Cada uno
 * mapea EXACTAMENTE un recurso de `res/raw` y lleva su etiqueta en español
 * para la hoja de selección.
 */
enum class TemaMusical(
    val id: String,
    val resId: Int,
    val etiqueta: String,
    val descripcion: String
) {
    ORIGINAL(
        id = "original",
        resId = R.raw.bgm_original,
        etiqueta = "Melodía Original",
        descripcion = "La canción de siempre del bosque."
    ),
    OCHO_BITS(
        id = "8bits",
        resId = R.raw.bgm_8bits,
        etiqueta = "Estilo 8-Bits (Retro)",
        descripcion = "Suena a maquinita de videojuego."
    ),
    ANIMADA(
        id = "animada",
        resId = R.raw.bgm_animada,
        etiqueta = "Ritmo Animado",
        descripcion = "Rápida y llena de energía."
    ),
    NINOS(
        id = "ninos",
        resId = R.raw.bgm_ninos,
        etiqueta = "Modo Infantil (Tierno)",
        descripcion = "Alegre, suavecita y juguetona."
    ),
    ALTERNATIVA(
        id = "alternativa",
        resId = R.raw.bgm_alternativa,
        etiqueta = "Versión Alternativa",
        descripcion = "Acústica y relajada para respirar."
    );

    companion object {
        /** Los cinco estilos en el orden en que se muestran al niño. */
        val todos: List<TemaMusical> = entries.toList()

        /** Resuelve el identificador guardado; ante cualquier duda, la original. */
        fun porId(id: String?): TemaMusical =
            todos.firstOrNull { it.id == id } ?: ORIGINAL
    }
}

/**
 * Fachada de audio de Zazili.
 *
 * Conserva la API que ya usan todas las pantallas, pero el trabajo pesado vive
 * en dos motores especializados:
 *
 *  - [AudioEngine]: [android.media.SoundPool] de cinco flujos, precarga
 *    asíncrona y sonidos que solo suenan después de `onLoadComplete`.
 *  - [BgmEngine]: un único [android.media.MediaPlayer], preparación en
 *    [kotlinx.coroutines.Dispatchers.IO], foco de audio y máquina de estados a
 *    prueba de reproductores liberados.
 *
 * Aquí solo se guardan preferencias y se reenvían las órdenes. Ningún
 * composable crea, retiene ni libera audio.
 */
object GlobalAudioManager {

    private const val TAG = "ZaziliAudio"
    private const val PREFS_NAME = "zazili_settings"
    private const val KEY_BGM_VOLUME = "bgm_volume"
    private const val KEY_SFX_VOLUME = "sfx_volume"
    private const val KEY_TEMA_BGM = "bgm_tema"
    private const val KEY_BGM_RES = "bgm_res"
    private const val DEFAULT_BGM_VOLUME = 0.5f
    private const val DEFAULT_SFX_VOLUME = 0.8f

    @Volatile
    private var initialized = false

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    @Volatile
    private var pistaElegidaRes = 0

    private val _bgmVolume = MutableStateFlow(DEFAULT_BGM_VOLUME)
    val bgmVolume: StateFlow<Float> = _bgmVolume.asStateFlow()

    private val _sfxVolume = MutableStateFlow(DEFAULT_SFX_VOLUME)
    val sfxVolume: StateFlow<Float> = _sfxVolume.asStateFlow()

    private val _temaBgm = MutableStateFlow(TemaMusical.ORIGINAL)
    val temaBgm: StateFlow<TemaMusical> = _temaBgm.asStateFlow()

    /** Resolución de la pista que está sonando (0 = ninguna). */
    val pistaActualRes: StateFlow<Int>
        get() = BgmEngine.pistaActualRes

    /**
     * Inicializa los motores una sola vez. Debe llamarse desde
     * `ZaziliApplication.onCreate()` y desde `MainActivity.onCreate()` para
     * poder reconstruir el audio si la app volvió después de un cierre real.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        appContext = app
        prefs = try {
            app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudieron abrir los ajustes de audio; se usarán valores por defecto", t)
            null
        }

        _bgmVolume.value = leerVolumen(KEY_BGM_VOLUME, DEFAULT_BGM_VOLUME)
        _sfxVolume.value = leerVolumen(KEY_SFX_VOLUME, DEFAULT_SFX_VOLUME)
        _temaBgm.value = leerTema()

        AudioEngine.initialize(app)
        AudioEngine.setVolumen(_sfxVolume.value)

        BgmEngine.initialize(app)
        BgmEngine.setVolume(_bgmVolume.value)

        pistaElegidaRes = leerPistaPersistida() ?: _temaBgm.value.resId
        BgmEngine.setPistaPrincipal(pistaElegidaRes)
        initialized = true
        Log.i(TAG, "Audio listo con el estilo ${_temaBgm.value.id}")
    }

    private fun asegurarInicializacion(context: Context) {
        if (!initialized) initialize(context)
    }

    // -----------------------------------------------------------------------
    //  Efectos cortos (delegados a AudioEngine)
    // -----------------------------------------------------------------------

    fun playPop() = AudioEngine.playPop()

    fun playSuccess() = AudioEngine.playSuccess()

    fun playMiss() = AudioEngine.playMiss()

    fun playDrum() = AudioEngine.playDrum()

    /** Voz del animalito del coro: 0 = rana, 1 = pájaro, 2 = mono. */
    fun playAnimal(index: Int) = AudioEngine.playAnimal(index)

    /** Efectos `sfx_*` agregados por el usuario. */
    fun reproducirEfectoExtra(context: Context, resId: Int) {
        asegurarInicializacion(context)
        AudioEngine.reproducirEfectoExtra(context, resId)
    }

    // -----------------------------------------------------------------------
    //  Música de fondo (delegada a BgmEngine)
    // -----------------------------------------------------------------------

    /**
     * Reproduce [resId] sin guardarlo como preferencia. Ideal para la música
     * propia de un cuento.
     */
    fun playBgm(context: Context, resId: Int) {
        asegurarInicializacion(context)
        if (resId == 0) return
        BgmEngine.playSpecificTrack(context, resId)
    }

    /**
     * Cambia el estilo de música: lo persiste, lo publica en [temaBgm] y lo
     * reproduce al instante sin bloquear la interfaz.
     */
    fun seleccionarTema(context: Context, tema: TemaMusical) {
        asegurarInicializacion(context)
        _temaBgm.value = tema
        pistaElegidaRes = tema.resId
        try {
            prefs?.edit()
                ?.putString(KEY_TEMA_BGM, tema.id)
                ?.putInt(KEY_BGM_RES, tema.resId)
                ?.apply()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo guardar el estilo de música elegido", t)
        }
        BgmEngine.setPistaPrincipal(tema.resId)
        BgmEngine.playSpecificTrack(context, tema.resId)
    }

    /**
     * Elige cualquier pista de `res/raw/bgm_*`, incluidas las que se agreguen
     * después sin registro: la preferencia se guarda por id de recurso y se
     * valida al arrancar para no quedarse en silencio tras una actualización.
     */
    fun seleccionarPista(context: Context, resId: Int) {
        asegurarInicializacion(context)
        if (resId == 0) return
        pistaElegidaRes = resId
        val estiloConocido = TemaMusical.todos.firstOrNull { it.resId == resId }
        if (estiloConocido != null) _temaBgm.value = estiloConocido
        try {
            val editor = prefs?.edit()?.putInt(KEY_BGM_RES, resId)
            if (estiloConocido != null) {
                editor?.putString(KEY_TEMA_BGM, estiloConocido.id)
            }
            editor?.apply()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo guardar la pista elegida", t)
        }
        BgmEngine.setPistaPrincipal(resId)
        BgmEngine.playSpecificTrack(context, resId)
    }

    /** Vuelve al estilo elegido por el niño (fin de un relato, regreso al menú). */
    fun restaurarBgm(context: Context) {
        asegurarInicializacion(context)
        BgmEngine.resumeMainTheme(context)
    }

    /** Reanuda la música al volver a primer plano. */
    fun startBgm() {
        if (!initialized) return
        BgmEngine.startBgm()
    }

    /** Pausa la música al salir a segundo plano. */
    fun pauseBgm() {
        if (!initialized) return
        BgmEngine.pauseBgm()
    }

    // -----------------------------------------------------------------------
    //  Volumen persistido.
    // -----------------------------------------------------------------------

    fun setBgmVolume(volume: Float) {
        val valor = volume.coerceIn(0f, 1f)
        _bgmVolume.value = valor
        try {
            prefs?.edit()?.putFloat(KEY_BGM_VOLUME, valor)?.apply()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo guardar el volumen de música", t)
        }
        BgmEngine.setVolume(valor)
    }

    fun setSfxVolume(volume: Float) {
        val valor = volume.coerceIn(0f, 1f)
        _sfxVolume.value = valor
        try {
            prefs?.edit()?.putFloat(KEY_SFX_VOLUME, valor)?.apply()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo guardar el volumen de efectos", t)
        }
        AudioEngine.setVolumen(valor)
    }

    // -----------------------------------------------------------------------
    //  Cierre real de la app.
    // -----------------------------------------------------------------------

    /**
     * Libera [AudioEngine] y [BgmEngine]. Después de llamarlo, [initialize]
     * puede reconstruir todo si la app vuelve a abrirse en el mismo proceso.
     */
    @Synchronized
    fun release() {
        AudioEngine.release()
        BgmEngine.release()
        initialized = false
        appContext = null
        prefs = null
        pistaElegidaRes = 0
        Log.i(TAG, "Audio global liberado")
    }

    // -----------------------------------------------------------------------
    //  Preferencias.
    // -----------------------------------------------------------------------

    private fun leerVolumen(key: String, fallback: Float): Float =
        try {
            (prefs?.getFloat(key, fallback) ?: fallback).coerceIn(0f, 1f)
        } catch (t: Throwable) {
            fallback
        }

    private fun leerTema(): TemaMusical =
        try {
            TemaMusical.porId(prefs?.getString(KEY_TEMA_BGM, null))
        } catch (t: Throwable) {
            TemaMusical.ORIGINAL
        }

    /**
     * Lee la pista guardada y comprueba que el recurso siga existiendo. Si la
     * app se actualizó y el archivo desapareció, se vuelve a la melodía
     * original en lugar de arrancar en silencio.
     */
    private fun leerPistaPersistida(): Int? {
        val guardada = try {
            prefs?.getInt(KEY_BGM_RES, 0) ?: 0
        } catch (t: Throwable) {
            0
        }
        if (guardada == 0) return null
        val app = appContext ?: return null
        return try {
            app.resources.getResourceEntryName(guardada)
            guardada
        } catch (t: Throwable) {
            null
        }
    }
}
