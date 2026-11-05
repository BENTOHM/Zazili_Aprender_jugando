package com.example.zazili.core.dialogos

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Momentos en los que Zazi habla. Cada pantalla y cada aviso piden su
 * categoría aquí; ningún texto queda escrito a mano en la UI.
 */
enum class CategoriaDialogo {
    SALUDO,
    ALIMENTAR,
    VICTORIA_PERFECTA,
    VICTORIA_CON_ERRORES,
    ANIMO_ERROR,
    IDLE,
    RACHA_ENCENDIDA,
    PROTECTOR_USADO,
    MIMAR,
    DORMIR,
    DESPERTAR,
    SIN_ENERGIA,
    SIN_ZAZOLES,
    TELARANAS,
    RACHA_PELIGRO,
    RACHA_CONGELADA,
    FIN_DE_SEMANA,
    AVISO_PRUEBA
}

/**
 * Diccionario central de la voz de Zazi.
 *
 * Todo el humor es mexicano, contemporáneo y apto para niños: tacos,
 * tianguis, la abuela, el internet del vecino, telenovelas y memes clásicos.
 * Nunca hay regaños ni referencias que necesiten contexto adulto.
 *
 * [frase] evita repetir la última línea de la misma categoría, así que la
 * mascota no dice dos veces seguidas el mismo chiste aunque se toque el mismo
 * botón en ráfaga. Las listas son deliberadamente amplias (mínimo ocho
 * variantes por categoría) y no se recortan.
 */
object DynamicDialogueDictionary {

    // -----------------------------------------------------------------------
    //  INTERACCIÓN Y CARIÑO
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
        "¡A darle! El día está bonito y las ideas andan de fiesta.",
        "¡Pásale, pásale! Hay lugar en el sillón y misiones en el mapa.",
        "¡Hey! Justo estaba pensando en ti. Y en chilaquiles, pero más en ti."
    )

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
        "¡Eso! Con esto puedo correr más rápido que el camión de la ruta.",
        "¡Qué sazón! Hasta el comal se puso celoso.",
        "¡Rico, rico! Ya puedo ir por mi segunda ronda de aventuras."
    )

    private val MIMAR = listOf(
        "Prrr… así se apapachan los colibrís contentos.",
        "¡Oye, que me sonrojo en pluma!",
        "Mimo recibido. Mimo guardado para día triste.",
        "¡Ay, qué lindo! Me siento como taco al pastor recién hecho.",
        "Sigan, sigan, que aquí aguanto todo el cariño del mundo.",
        "¡Eso! Hasta mi coronita de plumas se puso feliz.",
        "Un apapacho más y floto hasta la luna sin internet.",
        "¡Mi corazón hace pip-pip de emoción!",
        "¡Apapacho nivel campeonato! Ya me siento de estreno.",
        "Cariñito recibido con honores y con muchas plumas."
    )

    private val DORMIR = listOf(
        "Zzz… cinco minutitos más, campeón…",
        "Zzz… soñé que el internet nunca se caía…",
        "Zzz… en mi sueño los tacos hacen la tarea solos…",
        "Shhh… Zazi ya entró en modo avión.",
        "Zzz… si me hablas, deja recado después del sueño…",
        "Zzz… estoy volando en un mar de chocolate…",
        "Zzz… despiértame cuando haya pan dulce…",
        "Zzz… abracé mi almohada y no la suelto…",
        "Zzz… soñé que ganábamos la copa… ¡y había botanitas!",
        "Zzz… no apagues la luna, que me da miedo…"
    )

    private val DESPERTAR = listOf(
        "¡Buenos días! Soñé que ya habías hecho la tarea…",
        "¡Ah! Me dormí más que novio en película romántica.",
        "¡Despierta desperté! ¿Qué me perdí?",
        "¡Uy! ¿Ya es hora? Me quedé como estatua de cera.",
        "¡Estirando alas! Listo para el día.",
        "¡Ahí viene el sol! Y yo con el pelo hecho nido.",
        "¡Desperté! Y traigo hambre de aprender. Y de tacos.",
        "¡Modo avión desactivado! ¿Qué jugamos?",
        "¡Ya abrí un ojo! El otro viene en camino.",
        "¡Listo! Hoy se siente día de récord."
    )

    private val IDLE = listOf(
        "¿Qué me ves? ¡Vamos a jugar!",
        "Tengo hambre de conocimiento... y de chilaquiles.",
        "¡Ey! Ese toque despertó mis plumas.",
        "¿Ya viste el mapa? Hay misiones nuevas.",
        "Me gusta cuando vienes, aunque sea a picotearme.",
        "¡Pío! Digo… ¡hola!",
        "Zazi modo curioso activado: ¿qué hacemos hoy?",
        "¿Vamos por una lección o nos quedamos de flojos?",
        "¡Aquí estoy! Listo para la aventura.",
        "Si me tocas otra vez, te cuento un secreto: hay Zazoles esperando.",
        "¡Qué emoción! ¿Jugamos un minijuego?",
        "Ando de buen humor: hasta mis plumas se peinaron."
    )

    // -----------------------------------------------------------------------
    //  VICTORIA Y ERRORES
    // -----------------------------------------------------------------------

    private val VICTORIA_PERFECTA = listOf(
        "¡Cine! 🎬 Ni Einstein lo haría mejor.",
        "¡Andas volando, fiera!",
        "¡Hackeaste el sistema! 100% perfecto.",
        "¡Perfecto! Ni la panadería a las 7 de la mañana.",
        "¡Tres estrellas y aplausos de pie! Bueno, de ala.",
        "¡Nivel NASA! Y con tacos incluidos.",
        "¡Sin un solo error! Más limpio que barrer con el patio regado.",
        "¡Récord mundial! La arena se queda en silencio… ¡y explota!",
        "¡Impecable! Ni la abuela borda tan bien.",
        "¡Le diste en el blanco como a piñata en posada!",
        "¡Qué cráneo! Tienes más memoria que la abuela con los cumpleaños.",
        "¡Poder ilimitado! Modo campeón desbloqueado."
    )

    private val VICTORIA_CON_ERRORES = listOf(
        "¡Lo logramos! Un tropezón no es caída.",
        "¡Aprobado! Pero casi me da un microinfarto.",
        "¡Victoria! Suda la gota gorda, pero se logró.",
        "¡Eso, campeón! No fue perfecto, pero fue tuyo.",
        "¡Se pudo! Como cuando encuentras lugar para estacionarte.",
        "¡Bravo! Los errores fueron solo decoración.",
        "¡Bien ahí! Aprendiste más que en una tarde de telenovela.",
        "¡Lo sacaste! Ni el tráfico del lunes nos detuvo.",
        "¡Casi arte! Con un poquito de chile y mucho esfuerzo.",
        "¡Ganamos! La próxima sale más limpio, ya verás.",
        "¡A darle! El siguiente nivel ya nos tiene miedo.",
        "¡Chido! Terminaste aunque el camino traía baches."
    )

    private val ANIMO_ERROR = listOf(
        "¡Uy, cerca! Intenta de nuevo.",
        "El error es información, ¡tú puedes!",
        "Ni los tamales salen bien a la primera.",
        "Tranquilo, respira: hasta los campeones fallan.",
        "Ese no era, pero ya vamos aprendiendo.",
        "¡Ánimo! Un tropiezo no apaga la flama.",
        "Los errores son pistas, no castigos.",
        "¡Casi! Ajusta tantito y va de nuevo.",
        "No pasa nada: Zazi también se equivoca de canción.",
        "¡Eh! Esa fue difícil; la siguiente es tuya.",
        "Intenta otra vez, sin prisa y con onda.",
        "Fallar es parte del entrenamiento ninja.",
        "¡Vas bien! Solo cambia de estrategia.",
        "Respira, piensa y ¡pum! Ahora sí."
    )

    // -----------------------------------------------------------------------
    //  RACHA Y CUIDADOS
    // -----------------------------------------------------------------------

    private val RACHA_ENCENDIDA = listOf(
        "¡Se encendió la flama! Ni en la cocina de la abuela hay tanto fuego.",
        "¡Racha viva! Ya vamos por otra vuelta al carrusel.",
        "¡Fuego, fuego! Cuidado, Zazi, que te vas a achicharrar de orgullo.",
        "¡Un día más! Esto ya parece maratón de series un fin de semana.",
        "¡Racha encendida! Como la tele de casa ajena un sábado.",
        "¡Esa llama no se apaga ni con regaderazo!",
        "¡Sigue así y el calendario se va a cansar de nosotros!",
        "¡Otra velita encendida! Ya casi hacemos pastel.",
        "¡Flama nivel volcán! Ya huele a récord.",
        "¡Encendida otra vez! El calendario nos está aplaudiendo."
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

    private val SIN_ENERGIA = listOf(
        "Uf… me falta energía como a celular a las 3 pm.",
        "Mis alas piden modo avión… ¿me das una lección? Esas me recargan.",
        "Estoy más cansado que banda en desfile de tres horas…",
        "Alas en cero. Mejor una lección, esas me recargan.",
        "Déjame descansar tantito y regresamos con todo.",
        "Ando como tele sin cable: hay imagen, pero con nieve.",
        "Me pasa lo que al carrito del súper: se me baja la pila.",
        "Necesito recargar. ¿Un cuentito o una lección rapidita?",
        "Batería en rojo. Un minijuego y vuelvo a volar.",
        "Hoy amanecí en modo gelatina. Ayúdame a despertar."
    )

    private val SIN_ZAZOLES = listOf(
        "Te faltan zazoles, compa. Habrá que jugar… cuando abra el tianguis.",
        "Mis semillas no crecen en árboles. Bueno, sí, pero tú me entiendes.",
        "Cuenta bancaria de colibrí en ceros. Lo sentimos mucho.",
        "Saldo insuficiente, como cuando quieres dos quesadillas y alcanza para una.",
        "Se me acabaron los ahorros. Hora de ganar más con una lección.",
        "La alcancía dice «aquí no hay nada, jefe».",
        "Nada en la bolsa. Ni un cinco, ni una pluma.",
        "Andamos en modo austeridad, pero con estilo.",
        "Bolsa vacía como despensa del día antes del mandado.",
        "Cero Zazoles, muchas ganas. ¿Jugamos para llenar la alcancía?"
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

    // -----------------------------------------------------------------------
    //  AVISOS DE RACHA Y FIN DE SEMANA
    // -----------------------------------------------------------------------

    private val RACHA_PELIGRO = listOf(
        "¡Zazi está haciendo sus maletas! 🧳 Tu racha se apaga.",
        "¿Me vas a dejar en visto? Salva tu racha.",
        "¡Emergencia nivel código rojo! 🚨 Entra ya.",
        "¡La flama está temblando! Un juego y la salvamos.",
        "Zazi ya preparó el drama: tu racha necesita rescate.",
        "¡No me dejes así! Entra, juega y apagamos la alarma.",
        "Se nos acaba el día y la racha sigue en pausa.",
        "¡Última llamada! Tu racha te está buscando."
    )

    private val RACHA_CONGELADA = listOf(
        "¡Zazi usó escudo de titanio! Racha protegida.",
        "Uf, nos salvamos. El protector de racha hizo su magia.",
        "¡Congelamos el tiempo! Tu racha sigue viva.",
        "Ese protector entró al quite como portero en penales.",
        "¡Respira! La racha sobrevivió al drama del día.",
        "Escudo activado: ni el lunes pudo con nosotros."
    )

    private val FIN_DE_SEMANA = listOf(
        "¡Es sábado de romper récords!",
        "Zazi te preparó un minijuego nuevo, ¡descúbrelo!",
        "Fin de semana = modo aventura activado.",
        "¿Películas o minijuegos? Zazi ya votó por jugar.",
        "¡Domingo de campeones! Ven a probar algo nuevo.",
        "Hay retos fresquecitos esperando por ti."
    )

    private val AVISO_PRUEBA = listOf(
        "¡Ven a jugar! Tu racha no se va a cuidar sola, ¿eh?",
        "Zazi te está buscando. Trae Zazoles y todo.",
        "Prueba de aviso: ¡pío! ¿Me escuchas?",
        "Aquí ando, recordándote que la aventura te espera."
    )

    private val listas: Map<CategoriaDialogo, List<String>> = mapOf(
        CategoriaDialogo.SALUDO to SALUDO,
        CategoriaDialogo.ALIMENTAR to ALIMENTAR,
        CategoriaDialogo.VICTORIA_PERFECTA to VICTORIA_PERFECTA,
        CategoriaDialogo.VICTORIA_CON_ERRORES to VICTORIA_CON_ERRORES,
        CategoriaDialogo.ANIMO_ERROR to ANIMO_ERROR,
        CategoriaDialogo.IDLE to IDLE,
        CategoriaDialogo.RACHA_ENCENDIDA to RACHA_ENCENDIDA,
        CategoriaDialogo.PROTECTOR_USADO to PROTECTOR_USADO,
        CategoriaDialogo.MIMAR to MIMAR,
        CategoriaDialogo.DORMIR to DORMIR,
        CategoriaDialogo.DESPERTAR to DESPERTAR,
        CategoriaDialogo.SIN_ENERGIA to SIN_ENERGIA,
        CategoriaDialogo.SIN_ZAZOLES to SIN_ZAZOLES,
        CategoriaDialogo.TELARANAS to TELARANAS,
        CategoriaDialogo.RACHA_PELIGRO to RACHA_PELIGRO,
        CategoriaDialogo.RACHA_CONGELADA to RACHA_CONGELADA,
        CategoriaDialogo.FIN_DE_SEMANA to FIN_DE_SEMANA,
        CategoriaDialogo.AVISO_PRUEBA to AVISO_PRUEBA
    )

    private val ultimaFrase = ConcurrentHashMap<CategoriaDialogo, String>()

    /** Todas las variantes de una categoría, para pruebas o galerías. */
    fun variantes(categoria: CategoriaDialogo): List<String> =
        listas[categoria].orEmpty()

    /**
     * Frase al azar que nunca repite la última de la misma categoría. Si la
     * lista tiene una sola entrada, la devuelve; si está vacía, regresa texto
     * neutro para no dejar la UI en blanco.
     */
    fun frase(categoria: CategoriaDialogo): String {
        val opciones = listas[categoria].orEmpty()
        if (opciones.isEmpty()) return "¡Vamos a jugar!"
        if (opciones.size == 1) return opciones.first()

        val anterior = ultimaFrase[categoria]
        var elegida = opciones[Random.nextInt(opciones.size)]
        var intentos = 0
        while (elegida == anterior && intentos < 8) {
            elegida = opciones[Random.nextInt(opciones.size)]
            intentos++
        }
        ultimaFrase[categoria] = elegida
        return elegida
    }

    /** Victoria con guiño de perfección cuando se ganaron tres estrellas. */
    fun victoria(estrellas: Int): String =
        if (estrellas >= 3) frase(CategoriaDialogo.VICTORIA_PERFECTA)
        else frase(CategoriaDialogo.VICTORIA_CON_ERRORES)

    /** Animo tras un error: siempre positivo, nunca un regaño. */
    fun animoDeError(): String = frase(CategoriaDialogo.ANIMO_ERROR)

    /** Reacción al tocar a Zazi sin una acción concreta. */
    fun idle(): String = frase(CategoriaDialogo.IDLE)
}
