package com.example.zazili.domain.arena

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable

/**
 * Reglas atómicas de la Arena Cortex.
 *
 * Cada minijuego nace con **una** regla raíz que el niño puede decir en voz
 * alta y cumplir siempre. Encima se derivan dos restricciones de pantalla:
 * primero se esconde la pista, después se aprieta el reloj o se suma una
 * opción. Ninguna derivación contradice la raíz ni la desactiva: siempre se
 * sigue jugando con la misma idea, solo con menos red.
 *
 * El sistema nunca castiga con un "Game Over":
 *  - una regla nueva se **comunica** con un reto que enseña y a la vez evalúa,
 *  - se **verifica** durante [RONDAS_POR_REGLA] rondas,
 *  - si el niño falla dos veces, la regla se **re-explica** con otro ejemplo y
 *    la verificación vuelve a empezar.
 *
 * La complejidad sube solo cuando hace falta una decisión nueva, nunca por
 * llenar la pantalla: como máximo conviven [MAX_REGLAS_SIMULTANEAS] reglas.
 */

const val MAX_REGLAS_SIMULTANEAS = 3

/** Rondas de experimentación que sellan una regla. */
const val RONDAS_POR_REGLA = 3

/** Fallos que disparan la re-explicación de la regla en juego. */
const val FALLOS_PARA_REFORZAR = 2

private const val ARCHIVO_REGLAS = "zazili_arena_reglas"

/**
 * Efecto mecánico de una regla. Son las únicas tres palancas que el generador
 * procedural sabe aplicar; si una regla no cambia la pantalla, es [NINGUNO].
 */
@Immutable
enum class AjusteRegla(val etiqueta: String) {
    NINGUNO("La regla se juega sin cambiar la pantalla"),
    SIN_PISTA("La pista queda oculta"),
    MAS_OPCIONES("Aparece una opción extra"),
    MENOS_TIEMPO("El reloj corre más rápido")
}

@Immutable
enum class FaseRegla(val etiqueta: String) {
    ANUNCIO("Recién comunicada"),
    VERIFICACION("Verificando"),
    INTERIORIZADA("Interiorizada")
}

/**
 * Regla inmutable. [orden] 0 es la raíz; [dependeDe] apunta a la regla de la
 * que se deriva, de modo que la cadena nunca puede saltarse la raíz.
 */
@Immutable
data class ReglaAtomica(
    val id: String,
    val juegoId: String,
    val orden: Int,
    val enunciado: String,
    val ajuste: AjusteRegla,
    val ejemplo: String,
    val dependeDe: String?
) {
    val esRaiz: Boolean get() = orden == 0

    val permitePista: Boolean get() = ajuste != AjusteRegla.SIN_PISTA
}

@Immutable
data class ReglaActiva(
    val regla: ReglaAtomica,
    val fase: FaseRegla
) {
    val interiorizada: Boolean get() = fase == FaseRegla.INTERIORIZADA
}

/** Estado de profundidad de un minijuego. */
@Immutable
data class EstadoProfundo(
    val juegoId: String,
    val raiz: ReglaAtomica,
    val activas: List<ReglaActiva>,
    val verificaciones: Int,
    val reglasInteriorizadas: Int,
    val mensajeNino: String
) {
    val complejidad: Int get() = activas.size

    /** La regla que se está jugando ahora (la última que se sumó). */
    val enJuego: ReglaActiva? get() = activas.lastOrNull()

    val dominioCompleto: Boolean get() = reglasInteriorizadas >= MAX_REGLAS_SIMULTANEAS

    val reglasRestantes: Int
        get() = (MAX_REGLAS_SIMULTANEAS - reglasInteriorizadas).coerceAtLeast(0)

    val ajustes: Set<AjusteRegla>
        get() = activas.map { it.regla.ajuste }.filter { it != AjusteRegla.NINGUNO }.toSet()

    val verificacionesRestantes: Int
        get() = (RONDAS_POR_REGLA - verificaciones).coerceAtLeast(0)
}

/** Lo que cambió tras una respuesta: qué se comunicó, selló o re-explicó. */
@Immutable
data class ResultadoProfundo(
    val estado: EstadoProfundo,
    val reglaNueva: ReglaAtomica? = null,
    val reglaInteriorizada: ReglaAtomica? = null,
    val reglaReforzada: ReglaAtomica? = null,
    val mensajeNino: String
)

/**
 * Administra la cadena de reglas de los 16 juegos.
 *
 * Es determinista y sin efectos sobre el contenido: recibe aciertos y fallos,
 * devuelve fases. La persistencia es opcional (`context` nulo = en memoria),
 * así que las pruebas pueden ejercitar el autómata sin Android.
 */
class DeepGamePuzzleManager(context: Context? = null) {

    private val preferencias: SharedPreferences? = context
        ?.applicationContext
        ?.getSharedPreferences(ARCHIVO_REGLAS, Context.MODE_PRIVATE)

    // Sin disco (pruebas, previews) el autómata conserva su avance aquí: las
    // reglas no dependen de Android para funcionar.
    private val memoria = mutableMapOf<String, ProgresoReglas>()

    fun reglasDe(juegoId: String): List<ReglaAtomica> =
        checkNotNull(CATALOGO_REGLAS[juegoId]) {
            "El minijuego $juegoId no tiene reglas atómicas"
        }

    fun estado(juegoId: String): EstadoProfundo = synchronized(this) {
        construir(reglasDe(juegoId), leer(juegoId))
    }

    /** Reglas, con su fase, listas para que el generador las aplique. */
    fun activas(juegoId: String): List<ReglaActiva> = estado(juegoId).activas

    /**
     * Primera llamada de la sesión: comunica la regla raíz antes de jugar.
     * Si ya se anunció, devuelve el estado sin cambios.
     */
    fun apertura(juegoId: String): ResultadoProfundo = synchronized(this) {
        val progreso = leer(juegoId)
        val previo = construir(reglasDe(juegoId), progreso)
        val enJuego = previo.enJuego ?: return@synchronized ResultadoProfundo(
            estado = previo,
            mensajeNino = MENSAJE_DOMINIO
        )
        if (enJuego.fase != FaseRegla.ANUNCIO) {
            return@synchronized ResultadoProfundo(
                estado = previo,
                mensajeNino = MENSAJE_DOMINIO
            )
        }
        guardar(juegoId, progreso.copy(anunciada = true, verificaciones = 0))
        val estado = estado(juegoId)
        ResultadoProfundo(
            estado = estado,
            reglaNueva = enJuego.regla,
            mensajeNino = "Regla de hoy: «${enJuego.regla.enunciado}» " +
                "Mira: ${enJuego.regla.ejemplo}"
        )
    }

    fun registrarAcierto(juegoId: String): ResultadoProfundo =
        aplicar(juegoId, correcto = true)

    fun registrarFallo(juegoId: String): ResultadoProfundo =
        aplicar(juegoId, correcto = false)

    fun reiniciar(juegoId: String) = synchronized(this) {
        memoria.remove(juegoId)
        preferencias?.edit()
            ?.remove(clave(juegoId, SELLADAS))
            ?.remove(clave(juegoId, ANUNCIADA))
            ?.remove(clave(juegoId, VERIFICACIONES))
            ?.remove(clave(juegoId, FALLOS))
            ?.apply()
        Unit
    }

    // ------------------------- autómata -------------------------

    private fun aplicar(juegoId: String, correcto: Boolean): ResultadoProfundo =
        synchronized(this) {
            val reglas = reglasDe(juegoId)
            val progreso = leer(juegoId)
            val previo = construir(reglas, progreso)
            if (previo.dominioCompleto) {
                return@synchronized ResultadoProfundo(
                    estado = previo,
                    mensajeNino = MENSAJE_DOMINIO
                )
            }

            val regla = reglas[progreso.selladas]

            if (!correcto) {
                val fallos = progreso.fallos + 1
                if (fallos >= FALLOS_PARA_REFORZAR) {
                    // Se re-explica la misma regla con otro ejemplo y se
                    // reinicia la verificación: aprender cuesta, pero no duele.
                    guardar(
                        juegoId,
                        progreso.copy(
                            anunciada = false,
                            verificaciones = 0,
                            fallos = 0
                        )
                    )
                    return@synchronized ResultadoProfundo(
                        estado = estado(juegoId),
                        reglaReforzada = regla,
                        mensajeNino = "Volvemos a la regla: «${regla.enunciado}» " +
                            "Piensa en esto: ${regla.ejemplo}"
                    )
                }
                guardar(
                    juegoId,
                    progreso.copy(
                        anunciada = true,
                        verificaciones = 0,
                        fallos = fallos
                    )
                )
                return@synchronized ResultadoProfundo(
                    estado = estado(juegoId),
                    mensajeNino = "Casi. Recuerda: «${regla.enunciado}»"
                )
            }

            val logradas = progreso.verificaciones + 1
            if (logradas < RONDAS_POR_REGLA) {
                guardar(
                    juegoId,
                    progreso.copy(
                        anunciada = true,
                        verificaciones = logradas,
                        fallos = 0
                    )
                )
                return@synchronized ResultadoProfundo(
                    estado = estado(juegoId),
                    mensajeNino = "Bien. Te falta ${RONDAS_POR_REGLA - logradas} " +
                        "para que esta regla sea tuya."
                )
            }

            // Regla sellada: se comunica la siguiente, si existe.
            val nuevas = progreso.selladas + 1
            val siguiente = reglas.getOrNull(nuevas)
            guardar(
                juegoId,
                ProgresoReglas(
                    selladas = nuevas,
                    anunciada = siguiente == null,
                    verificaciones = 0
                )
            )
            val estado = estado(juegoId)
            return@synchronized ResultadoProfundo(
                estado = estado,
                reglaNueva = siguiente,
                reglaInteriorizada = regla,
                mensajeNino = if (siguiente == null) {
                    "Las tres reglas son tuyas. Ahora el reto se mueve más rápido."
                } else {
                    "Regla nueva: «${siguiente.enunciado}» " +
                        "Mira: ${siguiente.ejemplo}"
                }
            )
        }

    private fun construir(
        reglas: List<ReglaAtomica>,
        progreso: ProgresoReglas
    ): EstadoProfundo {
        val selladas = progreso.selladas.coerceIn(0, MAX_REGLAS_SIMULTANEAS)
        val anunciada = progreso.anunciada || selladas >= MAX_REGLAS_SIMULTANEAS
        val activas = buildList {
            reglas.take(selladas).forEach { add(ReglaActiva(it, FaseRegla.INTERIORIZADA)) }
            reglas.getOrNull(selladas)?.let { regla ->
                add(
                    ReglaActiva(
                        regla = regla,
                        fase = if (anunciada) {
                            FaseRegla.VERIFICACION
                        } else {
                            FaseRegla.ANUNCIO
                        }
                    )
                )
            }
        }
        val enJuego = activas.last()
        val mensaje = when {
            selladas >= MAX_REGLAS_SIMULTANEAS -> MENSAJE_DOMINIO
            enJuego.fase == FaseRegla.ANUNCIO ->
                "Regla nueva: «${enJuego.regla.enunciado}»"
            else -> "En juego: «${enJuego.regla.enunciado}»"
        }
        return EstadoProfundo(
            juegoId = enJuego.regla.juegoId,
            raiz = reglas.first(),
            activas = activas,
            verificaciones = progreso.verificaciones.coerceIn(0, RONDAS_POR_REGLA),
            reglasInteriorizadas = selladas,
            mensajeNino = mensaje
        )
    }

    // ------------------------- persistencia -------------------------

    private fun leer(juegoId: String): ProgresoReglas {
        val p = preferencias ?: return (memoria[juegoId] ?: ProgresoReglas()).normalizado()
        return ProgresoReglas(
            selladas = p.getInt(clave(juegoId, SELLADAS), 0),
            anunciada = p.getBoolean(clave(juegoId, ANUNCIADA), false),
            verificaciones = p.getInt(clave(juegoId, VERIFICACIONES), 0),
            fallos = p.getInt(clave(juegoId, FALLOS), 0)
        ).normalizado()
    }

    private fun guardar(juegoId: String, progreso: ProgresoReglas) {
        val p = preferencias
        if (p == null) {
            memoria[juegoId] = progreso
            return
        }
        p.edit()
            .putInt(clave(juegoId, SELLADAS), progreso.selladas)
            .putBoolean(clave(juegoId, ANUNCIADA), progreso.anunciada)
            .putInt(clave(juegoId, VERIFICACIONES), progreso.verificaciones)
            .putInt(clave(juegoId, FALLOS), progreso.fallos)
            .apply()
    }

    private fun clave(juegoId: String, campo: String): String = "pz_${juegoId}_$campo"

    private companion object {
        const val SELLADAS = "selladas"
        const val ANUNCIADA = "anunciada"
        const val VERIFICACIONES = "verificaciones"
        const val FALLOS = "fallos"

        const val MENSAJE_DOMINIO =
            "Las tres reglas están contigo. El reto se mueve más rápido."

        /** 16 juegos × (raíz + 2 derivaciones) = 48 reglas. */
        val CATALOGO_REGLAS: Map<String, List<ReglaAtomica>> by lazy {
            listOf(
                cadena(
                    juegoId = "calculo_mental",
                    raiz = Especificacion(
                        "Di el resultado en voz baja antes de tocar.",
                        AjusteRegla.NINGUNO,
                        "En 3 + 4 murmuras «siete» y luego tocas el siete."
                    ),
                    primera = Especificacion(
                        "Si ya lo dijiste, la pista ya no hace falta.",
                        AjusteRegla.SIN_PISTA,
                        "El cartel del tianguis se apaga: tú ya sabes por dónde empezar."
                    ),
                    segunda = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "Antes tenías veinte segundos; ahora tienes dieciséis."
                    )
                ),
                cadena(
                    juegoId = "fracciones_festivas",
                    raiz = Especificacion(
                        "Primero cuenta en cuántas partes se partió.",
                        AjusteRegla.NINGUNO,
                        "La rosca en seis pedazos: el número de abajo es seis."
                    ),
                    primera = Especificacion(
                        "Sin pista: los pedazos se cuentan solos.",
                        AjusteRegla.SIN_PISTA,
                        "Ya no aparece el dibujo de la rosca partida."
                    ),
                    segunda = Especificacion(
                        "Aparece una fracción más para elegir.",
                        AjusteRegla.MAS_OPCIONES,
                        "Ahora hay cuatro respuestas en la mesa y no tres."
                    )
                ),
                cadena(
                    juegoId = "geometria_ancestral",
                    raiz = Especificacion(
                        "Antes de mover, mira el hueco que queda.",
                        AjusteRegla.NINGUNO,
                        "Gira la pieza con los ojos primero y luego con el dedo."
                    ),
                    primera = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "El tangram pierde cuatro segundos de aire."
                    ),
                    segunda = Especificacion(
                        "La silueta guía se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "Solo queda el marco: tú decides dónde va cada pieza."
                    )
                ),
                cadena(
                    juegoId = "estadistica_alebrije",
                    raiz = Especificacion(
                        "Pregúntate: ¿puede salir siempre, a veces o nunca?",
                        AjusteRegla.NINGUNO,
                        "Si en la canasta solo hay jaguar, sacar jaguar es seguro."
                    ),
                    primera = Especificacion(
                        "Sin pista: la canasta ya no se dibuja.",
                        AjusteRegla.SIN_PISTA,
                        "Cuenta mentalmente los animalitos que quedan."
                    ),
                    segunda = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "Menos tiempo para contar y más para decidir."
                    )
                ),
                cadena(
                    juegoId = "codice_animado",
                    raiz = Especificacion(
                        "Lee la oración completa antes de elegir.",
                        AjusteRegla.NINGUNO,
                        "Espera al punto final: la última palabra cambia la respuesta."
                    ),
                    primera = Especificacion(
                        "Aparece una palabra más para elegir.",
                        AjusteRegla.MAS_OPCIONES,
                        "El códice ofrece cuatro candidatos y no tres."
                    ),
                    segunda = Especificacion(
                        "La pista del jaguar se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "Solo la oración: sin dibujos que delaten la respuesta."
                    )
                ),
                cadena(
                    juegoId = "puente_oraciones",
                    raiz = Especificacion(
                        "Primero quién, luego qué hizo.",
                        AjusteRegla.NINGUNO,
                        "«El colibrí vuela»: el colibrí va antes que vuela."
                    ),
                    primera = Especificacion(
                        "Sin pista: el río no marca el orden.",
                        AjusteRegla.SIN_PISTA,
                        "Las piedras ya no brillan para señalar la primera palabra."
                    ),
                    segunda = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "El puente se recoge antes: ordena con más prisa."
                    )
                ),
                cadena(
                    juegoId = "cuentacuentos",
                    raiz = Especificacion(
                        "Busca la palabra que dice cuándo pasó.",
                        AjusteRegla.NINGUNO,
                        "«De noche» va antes que «al amanecer»."
                    ),
                    primera = Especificacion(
                        "La lupa ya no subraya las pistas de tiempo.",
                        AjusteRegla.SIN_PISTA,
                        "Ahora tú encuentras las palabras «después» y «mientras»."
                    ),
                    segunda = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "El relato se ordena con menos aire entre escena y escena."
                    )
                ),
                cadena(
                    juegoId = "tlacuache_poeta",
                    raiz = Especificacion(
                        "Si suenan parecido, no siempre significan lo mismo.",
                        AjusteRegla.NINGUNO,
                        "«Vela» y «vela» suenan igual y son cosas distintas."
                    ),
                    primera = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "El tlacuache rapea más aprisa y tú contestas al ritmo."
                    ),
                    segunda = Especificacion(
                        "La rima de ayuda se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "Se acaba el subrayado del verso: decide de oído."
                    )
                ),
                cadena(
                    juegoId = "medidor_tormentas",
                    raiz = Especificacion(
                        "Pregúntate qué necesita primero la otra persona.",
                        AjusteRegla.NINGUNO,
                        "Si tiembla, primero aire; si llora, primero compañía."
                    ),
                    primera = Especificacion(
                        "Sin pista: nadie te dice qué emoción es.",
                        AjusteRegla.SIN_PISTA,
                        "Solo ves la escena; tú nombras lo que siente."
                    ),
                    segunda = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "La tormenta avanza: hay menos tiempo para elegir el auxilio."
                    )
                ),
                cadena(
                    juegoId = "camino_jaguar",
                    raiz = Especificacion(
                        "Elige pensando en la escena de mañana.",
                        AjusteRegla.NINGUNO,
                        "Si te comes el lunch, mañana nadie te presta su lápiz."
                    ),
                    primera = Especificacion(
                        "La huella que marca la vereda se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "El jaguar ya no ilumina el camino correcto."
                    ),
                    segunda = Especificacion(
                        "Aparece una vereda más para elegir.",
                        AjusteRegla.MAS_OPCIONES,
                        "Tres caminos en vez de dos, y el bosque recuerda igual."
                    )
                ),
                cadena(
                    juegoId = "espejo_fortalezas",
                    raiz = Especificacion(
                        "Una fortaleza no borra la etiqueta: la usa a tu favor.",
                        AjusteRegla.NINGUNO,
                        "«Distraído» también es «tu atención salta a lo que te interesa»."
                    ),
                    primera = Especificacion(
                        "Aparece un reflejo más para elegir.",
                        AjusteRegla.MAS_OPCIONES,
                        "El espejo muestra cuatro reflejos y solo uno te sirve."
                    ),
                    segunda = Especificacion(
                        "La pista del alebrije se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "Sin la voz que explica: tú eliges qué reflejo te fortalece."
                    )
                ),
                cadena(
                    juegoId = "sombra_miedo",
                    raiz = Especificacion(
                        "Si dudas, espera un paso antes de tocar.",
                        AjusteRegla.NINGUNO,
                        "Cuenta «uno» en tu cabeza y hasta entonces toca."
                    ),
                    primera = Especificacion(
                        "El tambor va más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "La sombra corre: hay menos tiempo entre paso y paso."
                    ),
                    segunda = Especificacion(
                        "El brillo que avisa se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "Ya no se ilumina el paso siguiente; escucha con atención."
                    )
                ),
                cadena(
                    juegoId = "alquimia_cenote",
                    raiz = Especificacion(
                        "El calor sube y el frío baja.",
                        AjusteRegla.NINGUNO,
                        "El vapor sube; las gotas bajan."
                    ),
                    primera = Especificacion(
                        "Sin pista: las flechas del diagrama se apagan.",
                        AjusteRegla.SIN_PISTA,
                        "El orden lo decides tú, sin flechas que lo adelanten."
                    ),
                    segunda = Especificacion(
                        "El agua se mueve más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "Cada estado dura menos: conecta los tubos sin pausa."
                    )
                ),
                cadena(
                    juegoId = "cadenas_alimenticias",
                    raiz = Especificacion(
                        "La flecha va de quien come a quien es comido.",
                        AjusteRegla.NINGUNO,
                        "El jaguar come venado: la flecha sale del jaguar."
                    ),
                    primera = Especificacion(
                        "La pista del nivel trófico se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "Ya no aparece si un animal es planta, herbívoro o carnívoro."
                    ),
                    segunda = Especificacion(
                        "Aparece un ser vivo más para elegir.",
                        AjusteRegla.MAS_OPCIONES,
                        "La red crece: cuatro candidatos y solo uno cierra la cadena."
                    )
                ),
                cadena(
                    juegoId = "filtro_magico",
                    raiz = Especificacion(
                        "Lo grueso va abajo y lo fino arriba.",
                        AjusteRegla.NINGUNO,
                        "La grava sostiene; la arena fina limpia al final."
                    ),
                    primera = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "El agua sube: apila las capas con menos tiempo."
                    ),
                    segunda = Especificacion(
                        "La silueta de la capa siguiente se apaga.",
                        AjusteRegla.SIN_PISTA,
                        "Sin sombra que anticipe: tú decides qué capa toca."
                    )
                ),
                cadena(
                    juegoId = "monstruo_basura",
                    raiz = Especificacion(
                        "Lo que se descompone es orgánico; lo que envenena es tóxico.",
                        AjusteRegla.NINGUNO,
                        "La cáscara vuelve a la tierra; la pila no."
                    ),
                    primera = Especificacion(
                        "El reloj corre un poco más rápido.",
                        AjusteRegla.MENOS_TIEMPO,
                        "La basura cae más rápido: clasifica al vuelo."
                    ),
                    segunda = Especificacion(
                        "Los botes pierden su dibujo.",
                        AjusteRegla.SIN_PISTA,
                        "Solo el color del bote te dice a dónde va cada cosa."
                    )
                )
            ).flatten().groupBy { it.juegoId }
        }

        fun cadena(
            juegoId: String,
            raiz: Especificacion,
            primera: Especificacion,
            segunda: Especificacion
        ): List<ReglaAtomica> = listOf(
            ReglaAtomica(
                id = "${juegoId}_raiz",
                juegoId = juegoId,
                orden = 0,
                enunciado = raiz.enunciado,
                ajuste = raiz.ajuste,
                ejemplo = raiz.ejemplo,
                dependeDe = null
            ),
            ReglaAtomica(
                id = "${juegoId}_r1",
                juegoId = juegoId,
                orden = 1,
                enunciado = primera.enunciado,
                ajuste = primera.ajuste,
                ejemplo = primera.ejemplo,
                dependeDe = "${juegoId}_raiz"
            ),
            ReglaAtomica(
                id = "${juegoId}_r2",
                juegoId = juegoId,
                orden = 2,
                enunciado = segunda.enunciado,
                ajuste = segunda.ajuste,
                ejemplo = segunda.ejemplo,
                dependeDe = "${juegoId}_r1"
            )
        )
    }
}

private data class Especificacion(
    val enunciado: String,
    val ajuste: AjusteRegla,
    val ejemplo: String
)

/** Avance guardado del autómata de reglas. */
private data class ProgresoReglas(
    val selladas: Int = 0,
    val anunciada: Boolean = false,
    val verificaciones: Int = 0,
    val fallos: Int = 0
) {
    /** Un archivo corrupto no puede sacar el índice de la cadena de reglas. */
    fun normalizado(): ProgresoReglas = copy(
        selladas = selladas.coerceIn(0, MAX_REGLAS_SIMULTANEAS),
        verificaciones = verificaciones.coerceIn(0, RONDAS_POR_REGLA),
        fallos = fallos.coerceIn(0, FALLOS_PARA_REFORZAR)
    )
}
