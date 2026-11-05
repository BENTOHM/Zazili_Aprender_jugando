package com.example.zazili.core.narrator

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.Locale
import kotlin.math.abs

/**
 * Motor de voz de Zazili con el modelo neural local (Piper VITS en ONNX).
 *
 * Carga `assets/modelo_voz.onnx` y `assets/config_voz.json`, convierte el
 * texto español a fonemas IPA (espeak es-419) y sintetiza audio a 22 050 Hz
 * sin red. Corre sobre `Dispatchers.IO` para que la inferencia nunca bloquee
 * el hilo principal.
 *
 * No existe respaldo con la voz del sistema: si el modelo falta, está dañado
 * o el hardware no puede correrlo, la voz queda en silencio en lugar de sonar
 * con una voz que no es la de Zazi.
 *
 * Vive fuera de Compose, se inicializa una sola vez desde
 * `ZaziliApplication.onCreate()` y nunca retiene una Activity.
 */
object NeuralVoiceEngine {

    private const val TAG = "ZaziliVozNeural"
    private const val ASSET_MODELO = "modelo_voz.onnx"
    private const val ASSET_CONFIG = "config_voz.json"
    private const val MAX_IDS = 900
    private const val MAX_CARACTERES_FRASE = 600
    private const val PAUSA_FRASES_MS = 140L

    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var initialized = false

    @Volatile
    private var neuralDisponible = false

    @Volatile
    private var generacion = 0L

    private var appContext: Context? = null
    private var trabajo: Job? = null

    private var entorno: OrtEnvironment? = null
    private var sesion: OrtSession? = null
    private var config: VozConfig? = null

    private var audioTrack: AudioTrack? = null

    private val _habilitado = MutableStateFlow(true)
    val habilitado: StateFlow<Boolean> = _habilitado.asStateFlow()

    // -----------------------------------------------------------------------
    //  Arranque.
    // -----------------------------------------------------------------------

    /** Inicializa el motor una sola vez con el contexto de aplicación. */
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val app = context.applicationContext
        appContext = app
        ambito.launch {
            val cargada = runCatching {
                withContext(Dispatchers.IO) { cargarNeural(app) }
            }.onFailure {
                Log.w(TAG, "La voz neural no se pudo cargar; Zazi quedará en silencio", it)
            }.getOrDefault(false)
            neuralDisponible = cargada
            if (cargada) {
                Log.i(TAG, "Voz neural local lista (${config?.sampleRate ?: 22050} Hz)")
            } else {
                Log.i(TAG, "Voz neural no disponible; no se usará una voz del sistema")
            }
        }
    }

    // -----------------------------------------------------------------------
    //  API pública.
    // -----------------------------------------------------------------------

    /**
     * Narra un texto con micro-pausas entre frases usando solo la voz neural.
     * Si el modelo no está listo o falla, la narración se detiene en silencio.
     */
    fun speakHumanized(texto: String) {
        if (!_habilitado.value) return
        val contenido = texto.trim()
        detenerReproduccion()
        if (contenido.isEmpty() || !neuralDisponible) return
        val gen = generacion

        trabajo = ambito.launch {
            try {
                val frases = dividirEnFrases(contenido)
                for ((indice, frase) in frases.withIndex()) {
                    if (!vigente(gen)) return@launch
                    val audio = sintetizar(frase)
                    if (audio == null) {
                        desactivarNeural()
                        return@launch
                    }
                    reproducirAudio(audio, gen)
                    if (indice < frases.lastIndex && vigente(gen)) delay(PAUSA_FRASES_MS)
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (t: Throwable) {
                Log.w(TAG, "Fallo de la voz neural; la narración se detiene", t)
                desactivarNeural()
            }
        }
    }

    /** Alias de compatibilidad con las pantallas que ya narran. */
    fun narrar(texto: String) = speakHumanized(texto)

    fun detener() = detenerReproduccion()

    fun setHabilitado(valor: Boolean) {
        _habilitado.value = valor
        if (!valor) detener()
    }

    fun alternar() = setHabilitado(!habilitado.value)

    /** `true` si la voz neural ONNX quedó operativa en este proceso. */
    fun usaVozNeural(): Boolean = neuralDisponible

    /** Diagnóstico en español para los ajustes. */
    fun motorActivo(): String = when {
        neuralDisponible -> "Voz neural local"
        else -> "Voz neural no disponible"
    }

    fun vozDisponible(): Boolean = neuralDisponible

    /** Libera la sesión ONNX. Terminal para este proceso. */
    @Synchronized
    fun liberar() {
        detenerReproduccion()
        trabajo = null
        runCatching { sesion?.close() }
        sesion = null
        config = null
        neuralDisponible = false
        appContext = null
    }

    // -----------------------------------------------------------------------
    //  Carga del modelo neural (todo blindado; nunca propaga errores).
    // -----------------------------------------------------------------------

    private fun cargarNeural(app: Context): Boolean {
        val cfg = leerConfig(app) ?: run {
            Log.w(TAG, "config_voz.json ausente o inválido")
            return false
        }
        val modelo = copiarModelo(app) ?: run {
            Log.w(TAG, "modelo_voz.onnx ausente o ilegible")
            return false
        }
        val entornoLocal = OrtEnvironment.getEnvironment()
        var opciones: OrtSession.SessionOptions? = null
        var sesionLocal: OrtSession? = null
        try {
            opciones = OrtSession.SessionOptions().apply {
                runCatching {
                    setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(1, 4))
                }
                runCatching {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                }
            }
            sesionLocal = entornoLocal.createSession(modelo.absolutePath, opciones)
            val nombres = sesionLocal.inputNames
            if (!nombres.containsAll(listOf("input", "input_lengths", "scales"))) {
                Log.w(TAG, "El modelo ONNX no expone las entradas esperadas: $nombres")
                runCatching { sesionLocal.close() }
                return false
            }
            entorno = entornoLocal
            sesion = sesionLocal
            config = cfg.copy(requiereSid = "sid" in nombres)
            return true
        } catch (t: Throwable) {
            runCatching { sesionLocal?.close() }
            throw t
        } finally {
            runCatching { opciones?.close() }
        }
    }

    private fun leerConfig(app: Context): VozConfig? = runCatching {
        val crudo = app.assets.open(ASSET_CONFIG).bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val raiz = JSONObject(crudo)
        val audio = raiz.optJSONObject("audio")
        val sampleRate = audio?.optInt("sample_rate", 22050) ?: 22050
        val inferencia = raiz.optJSONObject("inference")
        val noiseScale = inferencia?.optDouble("noise_scale", 0.667)?.toFloat() ?: 0.667f
        val lengthScale = inferencia?.optDouble("length_scale", 1.0)?.toFloat() ?: 1.0f
        val noiseW = inferencia?.optDouble("noise_w", 0.8)?.toFloat() ?: 0.8f
        val mapaJson = raiz.optJSONObject("phoneme_id_map") ?: return null

        val mapa = HashMap<String, IntArray>(mapaJson.length())
        val claves = mapaJson.keys()
        while (claves.hasNext()) {
            val clave = claves.next()
            val arreglo = mapaJson.optJSONArray(clave) ?: continue
            val ids = IntArray(arreglo.length()) { indice -> arreglo.optInt(indice, -1) }
                .filter { it >= 0 }
                .toIntArray()
            if (ids.isNotEmpty() && clave.isNotEmpty()) mapa[clave] = ids
        }
        if (mapa.isEmpty()) return null

        VozConfig(
            sampleRate = sampleRate,
            noiseScale = noiseScale,
            lengthScale = lengthScale,
            noiseW = noiseW,
            mapaIds = mapa,
            bos = mapa["^"] ?: intArrayOf(1),
            eos = mapa["$"] ?: intArrayOf(2),
            pad = mapa["_"] ?: intArrayOf(0),
            speakerId = 0L,
            requiereSid = raiz.optInt("num_speakers", 1) > 1
        )
    }.onFailure {
        Log.w(TAG, "No se pudo leer la configuración de la voz neural", it)
    }.getOrNull()

    /**
     * Copia el modelo desde `assets/` a `filesDir/voz` (ONNX Runtime necesita
     * una ruta real). Reutiliza la copia si la longitud coincide con la del
     * asset; escribe primero un temporal para no dejar un archivo truncado.
     */
    private fun copiarModelo(app: Context): File? {
        val carpeta = File(app.filesDir, "voz")
        if (!carpeta.exists() && !carpeta.mkdirs()) return null
        val destino = File(carpeta, ASSET_MODELO)
        val longitudAsset = runCatching {
            app.assets.openFd(ASSET_MODELO).use { it.length }
        }.getOrDefault(-1L)
        if (destino.exists() && destino.length() > 1024L &&
            (longitudAsset <= 0L || destino.length() == longitudAsset)
        ) {
            return destino
        }
        val temporal = File(carpeta, "$ASSET_MODELO.tmp")
        app.assets.open(ASSET_MODELO, AssetManager.ACCESS_STREAMING).use { entrada ->
            temporal.outputStream().use { salida -> entrada.copyTo(salida, 1 shl 16) }
        }
        if (temporal.length() <= 1024L) {
            temporal.delete()
            return null
        }
        if (destino.exists()) destino.delete()
        if (!temporal.renameTo(destino)) {
            temporal.copyTo(destino, overwrite = true)
            temporal.delete()
        }
        return destino
    }

    // -----------------------------------------------------------------------
    //  Inferencia ONNX.
    // -----------------------------------------------------------------------

    private fun sintetizar(frase: String): FloatArray? {
        val entornoLocal = entorno ?: return null
        val sesionLocal = sesion ?: return null
        val cfg = config ?: return null
        if (!neuralDisponible || frase.length > MAX_CARACTERES_FRASE) return null

        val ids = cfg.aIds(frase)
        if (ids.size < 4 || ids.size > MAX_IDS) {
            Log.w(TAG, "Frase fuera de rango para la voz neural (${ids.size} tokens)")
            return null
        }

        val tensores = ArrayList<OnnxTensor>(4)
        try {
            val tensorIds = OnnxTensor.createTensor(
                entornoLocal,
                LongBuffer.wrap(ids),
                longArrayOf(1, ids.size.toLong())
            )
            tensores.add(tensorIds)
            val tensorLongitudes = OnnxTensor.createTensor(
                entornoLocal,
                LongBuffer.wrap(longArrayOf(ids.size.toLong())),
                longArrayOf(1)
            )
            tensores.add(tensorLongitudes)
            val tensorEscalas = OnnxTensor.createTensor(
                entornoLocal,
                FloatBuffer.wrap(floatArrayOf(cfg.noiseScale, cfg.lengthScale, cfg.noiseW)),
                longArrayOf(3)
            )
            tensores.add(tensorEscalas)

            val entradas = HashMap<String, OnnxTensor>(4)
            entradas["input"] = tensorIds
            entradas["input_lengths"] = tensorLongitudes
            entradas["scales"] = tensorEscalas
            if (cfg.requiereSid) {
                val tensorSid = OnnxTensor.createTensor(
                    entornoLocal,
                    LongBuffer.wrap(longArrayOf(cfg.speakerId)),
                    longArrayOf(1)
                )
                tensores.add(tensorSid)
                entradas["sid"] = tensorSid
            }

            sesionLocal.run(entradas).use { resultado ->
                val tensorSalida = resultado.get(0) as? OnnxTensor ?: return null
                val buffer = tensorSalida.floatBuffer
                val audio = FloatArray(buffer.remaining())
                buffer.get(audio)
                return audio.takeIf { esAudioValido(it, cfg.sampleRate) }
            }
        } finally {
            tensores.forEach { tensor -> runCatching { tensor.close() } }
        }
    }

    private fun esAudioValido(audio: FloatArray, sampleRate: Int): Boolean {
        if (audio.size < sampleRate / 4) return false
        var pico = 0f
        for (muestra in audio) {
            val magnitud = abs(muestra)
            if (magnitud > pico) pico = magnitud
        }
        return pico.isFinite() && pico >= 0.01f
    }

    // -----------------------------------------------------------------------
    //  Reproducción del audio sintetizado.
    // -----------------------------------------------------------------------

    private suspend fun reproducirAudio(audio: FloatArray, gen: Long) {
        val cfg = config ?: return
        val frecuencia = cfg.sampleRate
        val pcm = ShortArray(audio.size) { indice ->
            (audio[indice].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        val bufferMinimo = AudioTrack.getMinBufferSize(
            frecuencia,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferMinimo <= 0) return

        val pista = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(frecuencia)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(bufferMinimo, pcm.size * 2))
                .build()
        } catch (t: Throwable) {
            Log.w(TAG, "No se pudo crear la salida de audio neural", t)
            return
        }

        audioTrack = pista
        try {
            pista.play()
            var desplazamiento = 0
            val bloque = maxOf(bufferMinimo, 2048)
            while (desplazamiento < pcm.size) {
                if (!vigente(gen)) return
                val escritos = pista.write(pcm, desplazamiento, minOf(bloque, pcm.size - desplazamiento))
                if (escritos <= 0) return
                desplazamiento += escritos
            }
            val duracionMs = pcm.size * 1000L / frecuencia
            var transcurrido = 0L
            while (transcurrido < duracionMs && vigente(gen)) {
                delay(50L)
                transcurrido += 50L
            }
        } finally {
            runCatching { pista.stop() }
            runCatching { pista.release() }
            if (audioTrack === pista) audioTrack = null
        }
    }

    // -----------------------------------------------------------------------
    //  Utilidades internas.
    // -----------------------------------------------------------------------

    private fun detenerReproduccion() {
        generacion++
        trabajo?.cancel()
        trabajo = null
        val pista = audioTrack
        audioTrack = null
        if (pista != null) {
            runCatching { pista.pause() }
            runCatching { pista.flush() }
            runCatching { pista.release() }
        }
    }

    private fun vigente(gen: Long): Boolean = _habilitado.value && generacion == gen

    private fun desactivarNeural() {
        if (neuralDisponible) {
            neuralDisponible = false
            Log.w(TAG, "Voz neural desactivada en este proceso; se usará la voz nativa")
        }
    }

    private fun dividirEnFrases(texto: String): List<String> =
        texto.split(Regex("(?<=[.!?…])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(texto) }

    // -----------------------------------------------------------------------
    //  Configuración Piper.
    // -----------------------------------------------------------------------

    private data class VozConfig(
        val sampleRate: Int,
        val noiseScale: Float,
        val lengthScale: Float,
        val noiseW: Float,
        val mapaIds: Map<String, IntArray>,
        val bos: IntArray,
        val eos: IntArray,
        val pad: IntArray,
        val speakerId: Long,
        val requiereSid: Boolean
    ) {
        /** Texto → IDs de fonemas con BOS, PAD y EOS como en Piper. */
        fun aIds(texto: String): LongArray {
            val fonemas = fonemizarTexto(texto)
            val ids = ArrayList<Long>(fonemas.size * 2 + bos.size + eos.size + 2)
            bos.forEach { ids.add(it.toLong()) }
            for (fonema in fonemas) {
                val mapeo = mapaIds[fonema] ?: continue
                for (id in mapeo) {
                    ids.add(id.toLong())
                    pad.forEach { ids.add(it.toLong()) }
                }
            }
            eos.forEach { ids.add(it.toLong()) }
            return ids.toLongArray()
        }
    }

    private data class Vocal(
        val indice: Int,
        val fuerte: Boolean,
        val acentuada: Boolean
    )

    private val SIN_ESTRES = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas",
        "de", "del", "que", "y", "o", "a", "en", "con", "por",
        "para", "al", "se", "su", "mi", "tu", "me", "te", "le",
        "lo", "es", "son", "no", "si", "ya"
    )

    private val UNIDADES = arrayOf(
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete",
        "ocho", "nueve", "diez", "once", "doce", "trece", "catorce",
        "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve",
        "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro",
        "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve"
    )

    private val DECENAS = arrayOf(
        "", "", "", "treinta", "cuarenta", "cincuenta",
        "sesenta", "setenta", "ochenta", "noventa"
    )

    private val CENTENAS = arrayOf(
        "", "ciento", "doscientos", "trescientos", "cuatrocientos",
        "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"
    )

    private fun numeroAPalabras(numero: Long): String {
        if (numero < 0) return "menos ${numeroAPalabras(-numero)}"
        if (numero < 30) return UNIDADES[numero.toInt()]
        if (numero < 100) {
            val decena = (numero / 10).toInt()
            val unidad = (numero % 10).toInt()
            return if (unidad == 0) DECENAS[decena] else "${DECENAS[decena]} y ${UNIDADES[unidad]}"
        }
        if (numero < 1000) {
            if (numero == 100L) return "cien"
            val centena = (numero / 100).toInt()
            val resto = numero % 100
            return if (resto == 0L) CENTENAS[centena]
            else "${CENTENAS[centena]} ${numeroAPalabras(resto)}"
        }
        if (numero < 1_000_000) {
            val miles = numero / 1000
            val resto = numero % 1000
            val prefijo = if (miles == 1L) "mil" else "${numeroAPalabras(miles)} mil"
            return if (resto == 0L) prefijo else "$prefijo ${numeroAPalabras(resto)}"
        }
        return numero.toString().map { digito -> UNIDADES[digito.digitToInt()] }
            .joinToString(" ")
    }

    private fun fonemizarTexto(texto: String): List<String> {
        val resultado = ArrayList<String>(texto.length)
        val coincidencias = Regex("[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+|\\d+|[.,;:!?…]")
            .findAll(texto)
        for (coincidencia in coincidencias) {
            val token = coincidencia.value
            when {
                token.length == 1 && token[0] in ".,;:!?…" -> {
                    if (resultado.isNotEmpty() && resultado.last() != " ") resultado.add(" ")
                    resultado.add(if (token == "…") "." else token)
                    resultado.add(" ")
                }
                token.all { it.isDigit() } -> {
                    val palabras = numeroAPalabras(token.toLongOrNull() ?: 0L)
                    for ((indice, palabra) in palabras.split(" ").withIndex()) {
                        if (indice > 0) agregarEspacio(resultado)
                        resultado.addAll(fonemasPalabra(palabra))
                    }
                    agregarEspacio(resultado)
                }
                else -> {
                    resultado.addAll(fonemasPalabra(token))
                    agregarEspacio(resultado)
                }
            }
        }
        while (resultado.lastOrNull() == " ") resultado.removeAt(resultado.lastIndex)
        return resultado
    }

    private fun agregarEspacio(lista: MutableList<String>) {
        if (lista.isNotEmpty() && lista.last() != " ") lista.add(" ")
    }

    /**
     * Aproximación de grafemas españoles a fonemas IPA de espeak (es-419)
     * contra el mapa del modelo. No pretende ser un G2P perfecto: si el
     * resultado no genera audio válido, el motor cae a la voz nativa.
     */
    private fun fonemasPalabra(original: String): List<String> {
        val minusculas = original.lowercase(Locale.forLanguageTag("es-MX"))
        val letras = StringBuilder()
        val acentos = HashSet<Int>()
        val dieresis = HashSet<Int>()
        for (caracter in minusculas) {
            var base: Char
            var acentuada = false
            var dieres = false
            when (caracter) {
                'á', 'à', 'â', 'ä' -> { base = 'a'; acentuada = true }
                'é', 'è', 'ê', 'ë' -> { base = 'e'; acentuada = true }
                'í', 'ì', 'î', 'ï' -> { base = 'i'; acentuada = true }
                'ó', 'ò', 'ô', 'ö' -> { base = 'o'; acentuada = true }
                'ú', 'ù', 'û' -> { base = 'u'; acentuada = true }
                'ü' -> { base = 'u'; dieres = true }
                in 'a'..'z' -> base = caracter
                else -> continue
            }
            val indice = letras.length
            letras.append(base)
            if (acentuada) acentos.add(indice)
            if (dieres) dieresis.add(indice)
        }

        val palabra = letras.toString()
        if (palabra.isEmpty()) return emptyList()

        val salida = ArrayList<String>(palabra.length + 4)
        val vocales = ArrayList<Vocal>(palabra.length)
        var i = 0
        while (i < palabra.length) {
            val caracter = palabra[i]
            val siguiente = palabra.getOrNull(i + 1)
            val despues = palabra.getOrNull(i + 2)
            when (caracter) {
                'a', 'e', 'i', 'o', 'u' -> {
                    salida.add(caracter.toString())
                    vocales.add(Vocal(salida.lastIndex, caracter in "aeo", i in acentos))
                    i++
                }
                'c' -> when {
                    siguiente == 'h' -> { salida.add("t"); salida.add("ʃ"); i += 2 }
                    siguiente == 'e' || siguiente == 'i' -> { salida.add("s"); i++ }
                    else -> { salida.add("k"); i++ }
                }
                'l' -> {
                    if (siguiente == 'l') { salida.add("ʝ"); i += 2 }
                    else { salida.add("l"); i++ }
                }
                'r' -> {
                    if (siguiente == 'r') { salida.add("r"); i += 2 }
                    else {
                        val previa = palabra.getOrNull(i - 1)
                        val fuerte = i == 0 || previa == 'n' || previa == 'l' || previa == 's'
                        salida.add(if (fuerte) "r" else "ɾ")
                        i++
                    }
                }
                'g' -> when {
                    siguiente == 'u' && (despues == 'e' || despues == 'i') -> {
                        salida.add("ɡ")
                        if ((i + 1) in dieresis) salida.add("w")
                        i += 2
                    }
                    siguiente == 'e' || siguiente == 'i' -> { salida.add("x"); i++ }
                    else -> { salida.add("ɡ"); i++ }
                }
                'q' -> {
                    salida.add("k")
                    i += if (siguiente == 'u') 2 else 1
                }
                'h' -> i++
                'x' -> { salida.add("k"); salida.add("s"); i++ }
                'y' -> {
                    val vocalSiguiente = siguiente != null && siguiente in "aeiou"
                    if (i < palabra.lastIndex && vocalSiguiente) salida.add("ʝ")
                    else salida.add("i")
                    i++
                }
                'z' -> { salida.add("s"); i++ }
                'v' -> { salida.add("b"); i++ }
                'ñ' -> { salida.add("ɲ"); i++ }
                'j' -> { salida.add("x"); i++ }
                'k' -> { salida.add("k"); i++ }
                'w' -> { salida.add("w"); i++ }
                'b', 'd', 'f', 'm', 'n', 'p', 's', 't' -> {
                    salida.add(caracter.toString())
                    i++
                }
                else -> i++
            }
        }
        if (salida.isEmpty()) return emptyList()

        val nucleos = ArrayList<MutableList<Vocal>>()
        for (vocal in vocales) {
            val ultimo = nucleos.lastOrNull()
            if (ultimo != null && vocal.indice == ultimo.last().indice + 1) {
                val anterior = ultimo.last()
                if ((vocal.fuerte && anterior.fuerte) || vocal.acentuada || anterior.acentuada) {
                    nucleos.add(mutableListOf(vocal))
                } else {
                    ultimo.add(vocal)
                }
            } else {
                nucleos.add(mutableListOf(vocal))
            }
        }

        if (nucleos.isNotEmpty() && palabra.length >= 4 && palabra !in SIN_ESTRES) {
            val acentuado = nucleos.indexOfFirst { nucleo -> nucleo.any { it.acentuada } }
            val indiceNucleo = if (acentuado >= 0) {
                acentuado
            } else {
                val ultima = palabra.last()
                val llana = ultima in "aeiou" || ultima == 'n' || ultima == 's'
                if (llana) (nucleos.size - 2).coerceAtLeast(0) else nucleos.size - 1
            }
            val posicion = nucleos.getOrNull(indiceNucleo)?.firstOrNull()?.indice
            if (posicion != null && posicion <= salida.size) salida.add(posicion, "ˈ")
        }
        return salida
    }
}
