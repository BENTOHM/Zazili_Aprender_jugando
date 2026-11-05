package com.example.zazili.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.zazili.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Motor de efectos cortos de Zazili.
 *
 * Un único [SoundPool] con atributos de juego, cinco flujos simultáneos y
 * precarga asíncrona. Ningún sonido suena antes de que `onLoadComplete`
 * confirme que su muestra está lista: el primer toque de cada efecto se
 * guarda como pendiente y se dispara en cuanto termina la carga, así nunca se
 * pierde el primer pop y nunca se toca un identificador inválido.
 *
 * Toda la API degrada a silencio: ninguna llamada lanza excepciones.
 */
object AudioEngine {

    private const val TAG = "ZaziliSfx"
    private const val MAX_STREAMS = 5
    private const val VOLUMEN_MINIMO = 0.01f

    @Volatile
    private var soundPool: SoundPool? = null

    @Volatile
    private var liberado = false

    @Volatile
    private var volumen = 0.8f

    private val cargados = ConcurrentHashMap.newKeySet<Int>()
    private val pendientes = ConcurrentHashMap.newKeySet<Int>()
    private val efectosExtra = ConcurrentHashMap<Int, Int>()

    @Volatile
    private var popId = 0

    @Volatile
    private var successId = 0

    @Volatile
    private var missId = 0

    @Volatile
    private var drumId = 0

    @Volatile
    private var animalIds: IntArray = IntArray(0)

    /**
     * Crea el [SoundPool] y precarga los efectos base una sola vez. Después de
     * [release] puede volver a llamarse y reconstruye todo.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (soundPool != null) return
        liberado = false
        val app = context.applicationContext
        val pool = try {
            SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
        } catch (t: Throwable) {
            Log.w(TAG, "SoundPool no disponible; la app seguirá sin efectos", t)
            null
        } ?: return

        soundPool = pool
        pool.setOnLoadCompleteListener { origen, sampleId, estado ->
            if (origen !== soundPool) return@setOnLoadCompleteListener
            if (estado == 0) {
                cargados.add(sampleId)
                if (pendientes.remove(sampleId)) reproducirMuestra(sampleId)
            } else {
                pendientes.remove(sampleId)
                Log.w(TAG, "El efecto $sampleId no se pudo cargar (estado $estado)")
            }
        }

        popId = cargar(pool, app, "ui_pop", R.raw.ui_pop)
        successId = cargar(pool, app, "ui_success", R.raw.ui_success)
        missId = cargar(pool, app, "ui_miss", R.raw.ui_miss)
        drumId = cargar(pool, app, "drum_antiguo", R.raw.drum_antiguo)
        animalIds = intArrayOf(
            cargar(pool, app, "frog", R.raw.frog),
            cargar(pool, app, "bird", R.raw.bird),
            cargar(pool, app, "monkey", R.raw.monkey)
        )
    }

    private fun cargar(pool: SoundPool, context: Context, nombre: String, resId: Int): Int =
        try {
            pool.load(context, resId, 1)
        } catch (t: Throwable) {
            Log.w(TAG, "El efecto '$nombre' no está disponible; se omite", t)
            0
        }

    fun playPop() = reproducirMuestra(popId)

    fun playSuccess() = reproducirMuestra(successId)

    fun playMiss() = reproducirMuestra(missId)

    fun playDrum() = reproducirMuestra(drumId)

    /** Voz del animalito del coro: 0 = rana, 1 = pájaro, 2 = mono. */
    fun playAnimal(index: Int) {
        val ids = animalIds
        if (ids.isEmpty()) return
        val seguro = ((index % ids.size) + ids.size) % ids.size
        reproducirMuestra(ids[seguro])
    }

    /**
     * Reproduce un `res/raw/sfx_*` descubierto por el usuario. La primera vez
     * se carga y se guarda como pendiente; las siguientes sale del caché.
     */
    fun reproducirEfectoExtra(context: Context, resId: Int) {
        if (resId == 0) return
        initialize(context)
        if (liberado) return

        val existente = efectosExtra[resId]
        if (existente != null) {
            reproducirMuestra(existente)
            return
        }

        val pool = soundPool ?: return
        val sampleId = cargar(pool, context.applicationContext, "extra_$resId", resId)
        if (sampleId == 0) return
        efectosExtra[resId] = sampleId
        reproducirMuestra(sampleId)
    }

    private fun reproducirMuestra(sampleId: Int) {
        if (sampleId == 0 || liberado) return
        val pool = soundPool ?: return
        val valor = volumen
        if (valor <= VOLUMEN_MINIMO) return

        if (!cargados.contains(sampleId)) {
            // Un solo pendiente por muestra: el primer toque no se pierde y
            // una ráfaga de toques no dispara una ráfaga de sonidos.
            pendientes.add(sampleId)
            return
        }

        try {
            pool.play(sampleId, valor, valor, 1, 0, 1f)
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo reproducir el efecto $sampleId", t)
        }
    }

    /** Volumen de los efectos nuevos; los flujos ya activos conservan el suyo. */
    fun setVolumen(valor: Float) {
        volumen = valor.coerceIn(0f, 1f)
    }

    /**
     * Libera el [SoundPool] para el cierre de la app. Las llamadas siguientes
     * no suenan y [initialize] puede reconstruirlo si hace falta.
     */
    @Synchronized
    fun release() {
        liberado = true
        val pool = soundPool
        soundPool = null
        popId = 0
        successId = 0
        missId = 0
        drumId = 0
        animalIds = IntArray(0)
        cargados.clear()
        pendientes.clear()
        efectosExtra.clear()
        try {
            pool?.release()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo liberar el SoundPool", t)
        }
    }
}
