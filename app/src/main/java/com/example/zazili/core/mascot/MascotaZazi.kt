package com.example.zazili.core.mascot

import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Estado vital de Zazi (Tamagotchi orgánico). Todas las métricas viven en
 * 0..1. DECISIÓN DE DISEÑO (TDAH): los mínimos NUNCA llegan a 0 — Zazi no
 * muere ni sufre castigos; solo se duerme con su piyama y la casita junta
 * telarañas. El abandono se comunica con humor, nunca con miedo.
 */
data class EstadoVital(
    val pancita: Float = 0.8f,
    val felicidad: Float = 0.85f,
    val energia: Float = 0.9f,
    val dormida: Boolean = false,
    val nivelTelaranas: Int = 0,
    val ultimaInteraccion: Long = System.currentTimeMillis(),
    val ultimaComida: Long = System.currentTimeMillis()
) {
    val contenta: Boolean
        get() = !dormida && pancita > 0.35f && felicidad > 0.35f && energia > 0.25f
}

/**
 * Motor puro de la mascota: decaimiento offline por horas transcurridas y
 * efectos de cada acción. Sin Android, sin I/O — 100 % testeable.
 */
object MotorMascota {

    // Decaimiento por hora de inactividad.
    const val DECAIMIENTO_PANCITA_HORA = 0.055f
    const val DECAIMIENTO_FELICIDAD_HORA = 0.03f
    const val DECAIMIENTO_ENERGIA_HORA = 0.07f

    // Sueño: al llegar al umbral Zazi se acuesta; dormida recupera energía.
    const val UMBRAL_DORMIR = 0.15f
    const val RECUPERACION_DORMIDA_HORA = 0.16f

    // Pisos vitales (nunca 0: anti-ansiedad).
    const val MINIMO_VITAL = 0.05f
    const val MINIMO_FELICIDAD = 0.10f

    // Telarañas: 1 por cada 2 días sin entrar, máximo 3.
    const val DIAS_POR_TELARANA = 2
    const val MAX_TELARANAS = 3

    // Efectos de acciones.
    const val GANANCIA_COMIDA = 0.30f
    const val GANANCIA_JUEGO_FELICIDAD = 0.25f
    const val COSTO_JUEGO_ENERGIA = 0.15f
    const val GANANCIA_MIMO = 0.08f
    const val GANANCIA_LECCION_ENERGIA = 0.20f
    const val GANANCIA_LECCION_PANCITA = 0.10f
    const val GANANCIA_MINIJUEGO_FELICIDAD = 0.15f

    const val ENERGIA_MINIMA_PARA_JUGAR = 0.20f

    /**
     * Recalcula el estado a partir de las horas transcurridas desde la última
     * interacción. Zazi despierta solo cuando el niño la toca.
     */
    fun aplicarDecaimiento(estado: EstadoVital, ahoraMillis: Long): EstadoVital {
        val horas = TimeUnit.MILLISECONDS.toHours(
            (ahoraMillis - estado.ultimaInteraccion).coerceAtLeast(0L)
        ).toFloat()

        val pancita = (estado.pancita - horas * DECAIMIENTO_PANCITA_HORA)
            .coerceIn(MINIMO_VITAL, 1f)
        val felicidad = (estado.felicidad - horas * DECAIMIENTO_FELICIDAD_HORA)
            .coerceIn(MINIMO_FELICIDAD, 1f)

        var energia = estado.energia
        var dormida = estado.dormida
        if (dormida) {
            energia += horas * RECUPERACION_DORMIDA_HORA
        } else if (energia <= UMBRAL_DORMIR || horas * DECAIMIENTO_ENERGIA_HORA >= energia - UMBRAL_DORMIR) {
            // Se agotó en algún punto del intervalo: lo que sobró de horas lo
            // pasa durmiendo y recuperando.
            val horasParaDormir = ((energia - UMBRAL_DORMIR) / DECAIMIENTO_ENERGIA_HORA)
                .coerceAtLeast(0f)
            energia = UMBRAL_DORMIR + (horas - horasParaDormir) * RECUPERACION_DORMIDA_HORA
            dormida = true
        } else {
            energia -= horas * DECAIMIENTO_ENERGIA_HORA
        }
        energia = energia.coerceIn(MINIMO_VITAL, 1f)

        val diasInactividad = TimeUnit.MILLISECONDS.toDays(
            (ahoraMillis - estado.ultimaInteraccion).coerceAtLeast(0L)
        )
        val telaranas = (diasInactividad / DIAS_POR_TELARANA).toInt()
            .coerceIn(0, MAX_TELARANAS)

        return estado.copy(
            pancita = pancita,
            felicidad = felicidad,
            energia = energia,
            dormida = dormida,
            nivelTelaranas = telaranas
        )
    }

    fun alimentar(e: EstadoVital): EstadoVital =
        e.copy(pancita = (e.pancita + GANANCIA_COMIDA).coerceAtMost(1f))

    fun jugar(e: EstadoVital): EstadoVital =
        e.copy(
            felicidad = (e.felicidad + GANANCIA_JUEGO_FELICIDAD).coerceAtMost(1f),
            energia = (e.energia - COSTO_JUEGO_ENERGIA).coerceAtLeast(MINIMO_VITAL),
            dormida = false
        )

    fun hacerMimo(e: EstadoVital): EstadoVital =
        e.copy(felicidad = (e.felicidad + GANANCIA_MIMO).coerceAtMost(1f))

    /** Las lecciones ALIMENTAN: dan energía y comida (refuerzo positivo). */
    fun registrarLeccion(e: EstadoVital): EstadoVital =
        e.copy(
            energia = (e.energia + GANANCIA_LECCION_ENERGIA).coerceAtMost(1f),
            pancita = (e.pancita + GANANCIA_LECCION_PANCITA).coerceAtMost(1f)
        )

    fun registrarMinijuego(e: EstadoVital): EstadoVital =
        e.copy(felicidad = (e.felicidad + GANANCIA_MINIJUEGO_FELICIDAD).coerceAtMost(1f))

    fun despertar(e: EstadoVital): EstadoVital = e.copy(dormida = false)
}

/** Personalidad de Zazi: humor mexicano contemporáneo, nunca regaño. */
object FrasesZazi {

    fun para(estado: EstadoVital): String = when {
        estado.dormida -> azar(
            "Zzz… soñé que le ganaba al Señor de los Costales…",
            "Zzz… cinco minutitos más, campeón…",
            "Zzz… en mi sueño los tacos hacen la tarea solos…"
        )
        estado.nivelTelaranas > 0 -> azar(
            "¿Ves telarañas? Yo también. Hasta las arañas ya tienen arañas.",
            "La casita anda más abandonada que estadio un martes…",
            "Sacudamos esto antes de que la Llorona quiera rentar."
        )
        estado.pancita < 0.3f -> azar(
            "Mi pancita suena como tambora de comparsa. ¿Semillitas?",
            "Tengo un hambre que hasta el agua me sabe a gas…",
            "Si no como pronto, me voy a comer mis palabras."
        )
        estado.felicidad < 0.3f -> azar(
            "Ando más apagado que wifi de azotea… ¿jugamos?",
            "Siento que se me olvidó dónde dejé la alegría. ¿Me ayudas a buscarla?",
            "Necesito jueguitos como tamal necesita hoja."
        )
        estado.energia < 0.3f -> azar(
            "Uf… me falta energía como a celular a las 3 pm.",
            "Mis alas piden modo avión… ¿me das una lección? Esas me recargan."
        )
        else -> azar(
            "¡Eres más chido que paleta de cacahuate!",
            "¡Chido! Dice el bosque que vas rompiendo rachas como piñata.",
            "¿Sabías? El colibrí es el único que puede volar pa'atrás. Como tú al repasar.",
            "¡Puro.colibrí.power! Vamos por más conocimiento.",
            "Hoy me siento tamal de dulce: envuelto en pura buena vibra."
        )
    }

    fun alComer(): String = azar(
        "¡Ñam! Semillas nivel dios.",
        "¡Provecho dicen los duendes! Yo digo: ¡ñomi!",
        "Esto sabe a victoria con chile de la buena."
    )

    fun alJugar(): String = azar(
        "¡Jajaja! Vuela, vuela, colibrí.",
        "¡Eso fue más rápido que camión de la ruta en bajada!",
        "¡Pido otra! Y otra. Y otra."
    )

    fun alMimo(): String = azar(
        "Prrr… así se purran los colibrís contentos.",
        "¡Oye, que me sonrojo en pluma!",
        "Mimo aceptado. Mimo siempre es aceptado."
    )

    fun alDespertar(): String = azar(
        "¡Buenos días! Soñé que ya habías hecho la tarea…",
        "¡Ah! Me dormí más que novio en película romántica.",
        "¡Despierta desperté! ¿Qué me perdí?"
    )

    fun sinZazoles(): String = azar(
        "Te faltan zazoles, compa. Habrá que jugar… cuando abra el tianguis.",
        "Mis semillas no crecen en árboles. Bueno, sí, pero tú me entiendes.",
        "Cuenta bancaria de colibrí en ceros. Lo sentimos mucho."
    )

    fun sinEnergia(): String = azar(
        "Estoy más cansado que banda en desfile de tres horas…",
        "Alas en cero. Mejor una lección, esas me recargan.",
        "Déjame descansar tantito y regresamos con todo."
    )

    private fun azar(vararg frases: String): String =
        frases[Random.nextInt(frases.size)]
}
