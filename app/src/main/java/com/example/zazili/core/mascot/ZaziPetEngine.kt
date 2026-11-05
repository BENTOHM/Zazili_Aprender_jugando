package com.example.zazili.core.mascot

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Momentos en los que Zazi suelta una frase. Cada pantalla pide el suyo y el
 * motor elige una línea al azar de su repertorio, evitando repetir la última
 * que salió para que el niño no escuche dos veces seguidas el mismo chiste.
 */
enum class EventoZazi {
    SALUDO,
    ALIMENTAR,
    NIVEL_GANADO,
    RACHA_ENCENDIDA,
    PROTECTOR_USADO,
    MIMAR,
    DORMIR,
    DESPERTAR,
    SIN_ENERGIA,
    SIN_ZAZOLES,
    TELARANAS
}

/**
 * Voz de Zazi: humor mexicano contemporáneo para niños, sin regaños y sin
 * referencias que requieran contexto adulto. Los chistes juegan con tacos,
 * tianguis, la abuela, el internet del vecino, las telenovelas y los memes
 * clásicos de la cultura pop, siempre con cariño.
 *
 * El catálogo es deliberadamente amplio: cada evento tiene mínimo ocho
 * variantes para que la mascota no se sienta repetitiva en una sesión larga.
 * No se recorta ninguna lista.
 */
object ZaziPetEngine {

    private val ultimaFrase = ConcurrentHashMap<EventoZazi, String>()

    /**
     * Devuelve una frase nueva para [evento]. Si el azar elige la misma que
     * salió la vez anterior, se prueba otra; con listas de 8+ variantes el
     * bucle termina de inmediato.
     */
    fun frase(evento: EventoZazi): String {
        val opciones = frasesDe(evento)
        if (opciones.isEmpty()) return ""
        if (opciones.size == 1) return opciones.first()
        val anterior = ultimaFrase[evento]
        var elegida = opciones[Random.nextInt(opciones.size)]
        var intentos = 0
        while (elegida == anterior && intentos < 6) {
            elegida = opciones[Random.nextInt(opciones.size)]
            intentos++
        }
        ultimaFrase[evento] = elegida
        return elegida
    }

    /** Frase de victoria con un guiño extra cuando fueron tres estrellas. */
    fun fraseDeVictoria(estrellas: Int): String =
        if (estrellas >= 3) frasePerfecta() else frase(EventoZazi.NIVEL_GANADO)

    /** Frase de bienvenida que respeta si Zazi está dormidita. */
    fun fraseDeSaludo(dormida: Boolean): String =
        if (dormida) frase(EventoZazi.DORMIR) else frase(EventoZazi.SALUDO)

    private fun frasesDe(evento: EventoZazi): List<String> = when (evento) {
        EventoZazi.SALUDO -> SALUDO
        EventoZazi.ALIMENTAR -> ALIMENTAR
        EventoZazi.NIVEL_GANADO -> NIVEL_GANADO
        EventoZazi.RACHA_ENCENDIDA -> RACHA_ENCENDIDA
        EventoZazi.PROTECTOR_USADO -> PROTECTOR_USADO
        EventoZazi.MIMAR -> MIMAR
        EventoZazi.DORMIR -> DORMIR
        EventoZazi.DESPERTAR -> DESPERTAR
        EventoZazi.SIN_ENERGIA -> SIN_ENERGIA
        EventoZazi.SIN_ZAZOLES -> SIN_ZAZOLES
        EventoZazi.TELARANAS -> TELARANAS
    }

    // -----------------------------------------------------------------------
    //  COMPIDITA: SALUDO
    // -----------------------------------------------------------------------

    private val SALUDO = listOf(
        "¡Órale, llegaste! Te estaba esperando como tamal a la hora del almuerzo.",
        "¡Qué milagrito! Pensé que te habías ido de vacaciones a la casa de la abuela.",
        "¡Eeeh, mi persona favorita! Bueno, después de los tacos, pero igual.",
        "¡Llegaste! Zazi enciende el boiler del cariño.",
        "¡Buenas! Aquí reportándose la mascota más chida del barrio digital.",
        "¡Ya llegó la estrella! Y sí, estoy hablando de ti, no de mí.",
        "Me estaba aburriendo más que anuncio de refresco de dos horas.",
        "¡Qué gusto! Ya me había quedado así como cuando se cae el internet.",
        "¡Hola, hola! ¿Vienes a aprender o a escuchar mis chistes malos?",
        "¡A darle! El día está bonito y las ideas andan de fiesta."
    )

    // -----------------------------------------------------------------------
    //  A COMER: ALIMENTAR
    // -----------------------------------------------------------------------

    private val ALIMENTAR = listOf(
        "¡Un manjar digno de los dioses!",
        "¡Panza llena, Zazi contento!",
        "¡Ñam! Está tan rico que hasta mis plumas dijeron «¡otra!».",
        "Esto sabe a domingo con la familia: rico y sin tarea.",
        "¡Provecho, dicen los duendes! Yo digo: ¡ñomi, ñomi!",
        "¡Ay, mi pancita! Ahora sí puedo ver la telenovela en paz.",
        "Sabes servir como la doña de la esquina. ¡Puro diez!",
        "¡Cuidado, que me como hasta el plato!",
        "Mi estómago aplaudió. Y no trae manos, imagínate la magia.",
        "¡Eso! Con esto puedo correr más rápido que el camión de la ruta."
    )

    // -----------------------------------------------------------------------
    //  VICTORIA: NIVEL GANADO
    // -----------------------------------------------------------------------

    private val NIVEL_GANADO = listOf(
        "¡Esto es cine! 🍿",
        "¡Poder ilimitado!",
        "Eres más rápido que el internet del vecino.",
        "¡Bravo! Ni en la final de la liga se armó tanto drama.",
        "¡Eso, campeón! Mañana salen tus highlights en la tele de la abuela.",
        "¡Se sintió como gol de último minuto en el Azteca!",
        "¡Wow! Yo aplico esa misma energía para levantarme del sillón.",
        "¡Aplausos de pie! Bueno, de ala, pero aplausos.",
        "¡Le diste en el blanco como a piñata en posada!",
        "¡Nivel desbloqueado y orgullo también!",
        "¡Qué cráneo! Tienes más memoria que la abuela con los cumpleaños.",
        "¡Te luciste! Hasta los alebrijes se voltearon a ver."
    )

    private val PERFECTA = listOf(
        "¡Perfecto! Ni la panadería te queda tan perfecto a las 7 de la mañana.",
        "¡Tres estrellas! Ya puedes pedir plaza en la NASA con tacos incluidos.",
        "¡Sin un solo error! Eso fue más limpio que barrer con el patio regado.",
        "¡Récord mundial! Silencio en la arena… ¡el público enloquece!",
        "¡Qué nivel! Hasta el profe se quedó de a seis.",
        "¡Impecable! Ni la abuela borda tan bien.",
        "¡Perfecto absoluto! Te mereces las tres estrellas y un pan dulce."
    )

    private fun frasePerfecta(): String = PERFECTA[Random.nextInt(PERFECTA.size)]

    // -----------------------------------------------------------------------
    //  RACHA
    // -----------------------------------------------------------------------

    private val RACHA_ENCENDIDA = listOf(
        "¡Se encendió la flama! Ni en la cocina de la abuela hay tanto fuego.",
        "¡Racha viva! Ya vamos por otra vuelta al carrusel.",
        "¡Fuego, fuego! Cuidado, Zazi, que te vas a achicharrar de orgullo.",
        "¡Un día más! Esto ya parece maratón de series un fin de semana.",
        "¡Racha encendida! Como la tele de casa ajena un sábado.",
        "¡Esa llama no se apaga ni con regaderazo!",
        "¡Sigue así y el calendario se va a cansar de nosotros!",
        "¡Otra velita encendida! Ya casi hacemos pastel."
    )

    private val PROTECTOR_USADO = listOf(
        "Estuvo cerca, casi me da un infarto aviar.",
        "¡Uf! El protector entró al quite como portero en penales.",
        "¡Salvada! Me sentí como cuando atrapas el celular antes del piso.",
        "¡Ay Dios! Ese protector fue mi capa de superhéroe.",
        "¡Qué susto! Ya estaba yo viendo la racha irse en el camión.",
        "El protector se sacrificó. Bajemos la cabeza en su honor.",
        "¡Se congeló el tiempo! Ahora sí parece telenovela de ciencia ficción.",
        "¡Casi, casi! Zazi respira profundo y se recoloca las plumas.",
        "Ese fue un salto de esos que ni en videojuego salen.",
        "¡Apenas! Que no se repita, que ya me duele la pancita del susto."
    )

    // -----------------------------------------------------------------------
    //  MIMOS, SUEÑO Y MALESTARES
    // -----------------------------------------------------------------------

    private val MIMAR = listOf(
        "Prrr… así se apapachan los colibrís contentos.",
        "¡Oye, que me sonrojo en pluma!",
        "Mimo recibido. Mimo guardado para día triste.",
        "¡Ay, qué lindo! Me siento como taco al pastor recién hecho.",
        "Sigan, sigan, que aquí aguanto todo el cariño del mundo.",
        "¡Eso! Hasta mi coronita de plumas se puso feliz.",
        "Un apapacho más y floto hasta la luna sin internet.",
        "¡Mi corazón hace pip-pip de emoción!"
    )

    private val DORMIR = listOf(
        "Zzz… cinco minutitos más, campeón…",
        "Zzz… soñé que el internet nunca se caía…",
        "Zzz… en mi sueño los tacos hacen la tarea solos…",
        "Shhh… Zazi ya entró en modo avión.",
        "Zzz… si me hablas, deja recado después del sueño…",
        "Zzz… estoy volando en un mar de chocolate…",
        "Zzz… despiértame cuando haya pan dulce…",
        "Zzz… abracé mi almohada y no la suelto…"
    )

    private val DESPERTAR = listOf(
        "¡Buenos días! Soñé que ya habías hecho la tarea…",
        "¡Ah! Me dormí más que novio en película romántica.",
        "¡Despierta desperté! ¿Qué me perdí?",
        "¡Uy! ¿Ya es hora? Me quedé como estatua de cera.",
        "¡Estirando alas! Listo para el día.",
        "¡Ahí viene el sol! Y yo con el pelo hecho nido.",
        "¡Desperté! Y traigo hambre de aprender. Y de tacos.",
        "¡Modo avión desactivado! ¿Qué jugamos?"
    )

    private val SIN_ENERGIA = listOf(
        "Uf… me falta energía como a celular a las 3 pm.",
        "Mis alas piden modo avión… ¿me das una lección? Esas me recargan.",
        "Estoy más cansado que banda en desfile de tres horas…",
        "Alas en cero. Mejor una lección, esas me recargan.",
        "Déjame descansar tantito y regresamos con todo.",
        "Ando como tele sin cable: hay imagen, pero con nieve.",
        "Me pasa lo que al carrito del súper: se me baja la pila.",
        "Necesito recargar. ¿Un cuentito o una lección rapidita?"
    )

    private val SIN_ZAZOLES = listOf(
        "Te faltan zazoles, compa. Habrá que jugar… cuando abra el tianguis.",
        "Mis semillas no crecen en árboles. Bueno, sí, pero tú me entiendes.",
        "Cuenta bancaria de colibrí en ceros. Lo sentimos mucho.",
        "Saldo insuficiente, como cuando quieres dos quesadillas y alcanza para una.",
        "Se me acabaron los ahorros. Hora de ganar más con una lección.",
        "La alcancía dice «aquí no hay nada, jefe».",
        "Nada en la bolsa. Ni un cinco, ni una pluma.",
        "Andamos en modo austeridad, pero con estilo."
    )

    private val TELARANAS = listOf(
        "¿Ves telarañas? Yo también. Hasta las arañas ya tienen arañas.",
        "La casita anda más abandonada que estadio un martes…",
        "Sacudamos esto antes de que la Llorona quiera rentar.",
        "Hay más telarañas aquí que en castillo de película de miedo.",
        "¡Cuidado! Esa arañita ya puso su sala y su comedor.",
        "Esta casa necesita una barrida nivel campeonato.",
        "Hasta las telarañas hicieron junta vecinal.",
        "Si esto sigue así, cobramos entrada como casa embrujada."
    )
}
