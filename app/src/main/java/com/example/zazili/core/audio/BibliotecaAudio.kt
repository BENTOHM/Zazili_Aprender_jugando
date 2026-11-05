package com.example.zazili.core.audio

import com.example.zazili.R

/**
 * Una pista de fondo detectada en `res/raw`.
 *
 * [id] es el nombre del recurso (`bgm_lluvia`), [titulo] la etiqueta bonita
 * que ve el niño y [resId] el identificador que consume
 * [GlobalAudioManager.seleccionarPista].
 */
data class PistaZazili(
    val id: String,
    val titulo: String,
    val resId: Int
)

/** Un efecto corto detectado en `res/raw/sfx_*`. */
data class EfectoZazili(
    val id: String,
    val titulo: String,
    val resId: Int
)

/**
 * Biblioteca viva de audio.
 *
 * Escanea los recursos de `res/raw` por convención de nombre, así que el
 * usuario puede soltar archivos nuevos sin tocar una línea de código:
 *
 *  - `bgm_<nombre>.mp3|ogg|wav` → aparece en Ajustes → Música.
 *  - `sfx_<nombre>.mp3|ogg|wav` → aparece en Ajustes → Efectos extra.
 *
 * Los nombres deben ir en minúsculas y sin acentos ni espacios. El orden de
 * las pistas conocidas es curado; las nuevas se acomodan alfabéticamente al
 * final para no descolocar la selección de siempre.
 */
object BibliotecaAudio {

    private val titulosCurados = mapOf(
        "bgm_original" to "Melodía Original",
        "bgm_zazili" to "Canción de Zazili",
        "bgm_ninos" to "Modo Infantil",
        "bgm_animada" to "Ritmo Animado",
        "bgm_8bits" to "8-Bits Retro",
        "bgm_alternativa" to "Versión Alternativa",
        "bgm_ajolote" to "El Ajolote",
        "bgm_alebrijes" to "Alebrijes",
        "bgm_cempasuchil" to "Cempasúchil",
        "bgm_dragon" to "Dragón",
        "bgm_duendes" to "Duendes",
        "bgm_gato_relojero" to "Gato Relojero",
        "bgm_raton_perez" to "Ratón Pérez"
    )

    private val ordenCurado = listOf(
        "bgm_original",
        "bgm_zazili",
        "bgm_ninos",
        "bgm_animada",
        "bgm_8bits",
        "bgm_alternativa",
        "bgm_ajolote",
        "bgm_alebrijes",
        "bgm_cempasuchil",
        "bgm_dragon",
        "bgm_duendes",
        "bgm_gato_relojero",
        "bgm_raton_perez"
    )

    /** Todas las pistas `bgm_*`, primero las curadas y luego las nuevas. */
    fun pistas(): List<PistaZazili> {
        val prioridad = ordenCurado.withIndex().associate { (indice, nombre) ->
            nombre to indice
        }
        return recursosRaw
            .filter { (nombre, _) -> nombre.startsWith(PREFIJO_MUSICA) }
            .map { (nombre, resId) ->
                PistaZazili(
                    id = nombre,
                    titulo = titulosCurados[nombre] ?: embellecer(nombre, PREFIJO_MUSICA),
                    resId = resId
                )
            }
            .sortedWith(
                compareBy<PistaZazili>(
                    { prioridad[it.id] ?: Int.MAX_VALUE },
                    { it.titulo }
                )
            )
    }

    /** Efectos `sfx_*` agregados por el usuario; vacío si no hay ninguno. */
    fun efectos(): List<EfectoZazili> =
        recursosRaw
            .filter { (nombre, _) -> nombre.startsWith(PREFIJO_EFECTOS) }
            .map { (nombre, resId) ->
                EfectoZazili(
                    id = nombre,
                    titulo = titulosCurados[nombre] ?: embellecer(nombre, PREFIJO_EFECTOS),
                    resId = resId
                )
            }
            .sortedBy { it.titulo }

    /**
     * Los campos de `R.raw` son enteros estáticos generados en compilación.
     * Se leen una sola vez por proceso y se memorizan.
     */
    private val recursosRaw: List<Pair<String, Int>> by lazy {
        R.raw::class.java.fields.mapNotNull { campo ->
            if (campo.type != Int::class.javaPrimitiveType) return@mapNotNull null
            runCatching { campo.name to campo.getInt(null) }.getOrNull()
        }
    }

    private fun embellecer(nombre: String, prefijo: String): String =
        nombre.removePrefix(prefijo)
            .split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { palabra ->
                palabra.replaceFirstChar { letra -> letra.uppercase() }
            }

    private const val PREFIJO_MUSICA = "bgm_"
    private const val PREFIJO_EFECTOS = "sfx_"
}
