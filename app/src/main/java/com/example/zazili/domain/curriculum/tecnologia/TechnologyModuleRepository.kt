package com.example.zazili.domain.curriculum.tecnologia

import androidx.compose.runtime.Immutable

/**
 * Una opción de respuesta con su veredicto ya resuelto.
 *
 * El motor de lecciones mezcla la lista antes de mostrarla y conserva este
 * campo intacto, así que la respuesta correcta nunca depende de la posición.
 */
@Immutable
data class AnswerOption(
    val text: String,
    val isCorrect: Boolean
)

/**
 * Una lección de Tecnología: una pregunta y cuatro opciones, una sola cierta.
 *
 * [id] va de 1 a 50 en el orden del mapa de la Pirámide del Código. La
 * validación en el constructor atrapa cualquier dato incompleto en desarrollo
 * antes de que llegue a la pantalla del niño.
 */
@Immutable
data class Lesson(
    val id: Int,
    val question: String,
    val options: List<AnswerOption>
) {
    init {
        require(id in 1..TechnologyModuleRepository.TOTAL_LECCIONES) {
            "La lección $id está fuera del rango 1..${TechnologyModuleRepository.TOTAL_LECCIONES}."
        }
        require(options.size == 4) {
            "La lección $id necesita exactamente cuatro opciones."
        }
        require(options.count { it.isCorrect } == 1) {
            "La lección $id necesita una sola respuesta correcta."
        }
        require(options.map { it.text }.distinct().size == options.size) {
            "La lección $id tiene opciones repetidas."
        }
    }
}

/**
 * Un capítulo de Tecnología: agrupa diez lecciones alrededor de un mismo tema.
 *
 * [jefe] y [dichoDelJefe] alimentan la cabecera del mapa de la saga; [lessons]
 * se juega en bloques de cinco, así que cada capítulo ocupa dos bloques del
 * camino.
 */
@Immutable
data class Chapter(
    val id: Int,
    val title: String,
    val description: String,
    val jefe: String,
    val dichoDelJefe: String,
    val lessons: List<Lesson>
) {
    init {
        require(id >= 1) { "El capítulo $title necesita un identificador positivo." }
        require(title.isNotBlank()) { "El capítulo $id necesita un título." }
        require(lessons.isNotEmpty()) { "El capítulo $title necesita lecciones." }
    }
}

/**
 * Currículo completo del módulo "Introducción a la Tecnología y Programación".
 *
 * 50 lecciones repartidas en cinco capítulos de diez:
 *
 *  - [capituloHardware] (1–10): partes físicas, dispositivos y cuidados.
 *  - [capituloSoftware] (11–20): programas, sistema operativo, apps y archivos.
 *  - [capituloInternet] (21–30): contraseñas, privacidad, ciberacoso y huella
 *    digital.
 *  - [capituloLogica] (31–40): algoritmos, secuencias, bugs, bucles,
 *    condicionales y variables.
 *  - [capituloProgramacion] (41–50): eventos, instrucciones, pruebas y el
 *    proyecto final de un juego.
 *
 * Todo el texto está en español mexicano, pensado para primaria y secundaria.
 * Las opciones nunca se reordenan aquí: el motor de lecciones baraja y decide
 * cuál tarjeta mostrar primero.
 */
object TechnologyModuleRepository {

    const val TOTAL_LECCIONES = 50
    const val TOTAL_CAPITULOS = 5
    const val LECCIONES_POR_CAPITULO = 10
    const val NOMBRE_MODULO = "Introducción a la Tecnología y Programación"

    // -----------------------------------------------------------------------
    //  CAPÍTULO 1 — HARDWARE (1–10)
    // -----------------------------------------------------------------------

    private val leccionesHardware: List<Lesson> = listOf(
        Lesson(
            id = 1,
            question = "¿Qué es el hardware de una computadora?",
            options = listOf(
                AnswerOption("Las partes físicas que puedes tocar", isCorrect = true),
                AnswerOption("Los programas que abres en la pantalla", isCorrect = false),
                AnswerOption("Las fotos guardadas en la nube", isCorrect = false),
                AnswerOption("Las contraseñas de tus juegos", isCorrect = false)
            )
        ),
        Lesson(
            id = 2,
            question = "¿Para qué sirve el monitor?",
            options = listOf(
                AnswerOption("Para ver lo que la computadora muestra", isCorrect = true),
                AnswerOption("Para escribir letras y números", isCorrect = false),
                AnswerOption("Para imprimir hojas", isCorrect = false),
                AnswerOption("Para conectar el internet", isCorrect = false)
            )
        ),
        Lesson(
            id = 3,
            question = "¿Cuál de estos es un ejemplo de hardware?",
            options = listOf(
                AnswerOption("El teclado", isCorrect = true),
                AnswerOption("Una app de dibujo", isCorrect = false),
                AnswerOption("Un video de YouTube", isCorrect = false),
                AnswerOption("El sistema operativo", isCorrect = false)
            )
        ),
        Lesson(
            id = 4,
            question = "¿Para qué sirve el teclado?",
            options = listOf(
                AnswerOption("Para escribir letras, números y símbolos", isCorrect = true),
                AnswerOption("Para ver imágenes", isCorrect = false),
                AnswerOption("Para escuchar música", isCorrect = false),
                AnswerOption("Para imprimir hojas", isCorrect = false)
            )
        ),
        Lesson(
            id = 5,
            question = "¿Qué parte mueves con la mano para señalar en la pantalla?",
            options = listOf(
                AnswerOption("El ratón", isCorrect = true),
                AnswerOption("La CPU", isCorrect = false),
                AnswerOption("El monitor", isCorrect = false),
                AnswerOption("La bocina", isCorrect = false)
            )
        ),
        Lesson(
            id = 6,
            question = "La CPU se conoce como el cerebro de la computadora porque…",
            options = listOf(
                AnswerOption("hace los cálculos y da las órdenes", isCorrect = true),
                AnswerOption("guarda la tarea en hojas", isCorrect = false),
                AnswerOption("ilumina la pantalla", isCorrect = false),
                AnswerOption("conecta el wifi", isCorrect = false)
            )
        ),
        Lesson(
            id = 7,
            question = "¿Para qué sirve una memoria USB?",
            options = listOf(
                AnswerOption("Para guardar y llevar archivos", isCorrect = true),
                AnswerOption("Para que la computadora hable", isCorrect = false),
                AnswerOption("Para tomar fotos", isCorrect = false),
                AnswerOption("Para escribir más rápido", isCorrect = false)
            )
        ),
        Lesson(
            id = 8,
            question = "¿Qué dispositivo sirve para escuchar el sonido de la computadora?",
            options = listOf(
                AnswerOption("Las bocinas o audífonos", isCorrect = true),
                AnswerOption("El teclado", isCorrect = false),
                AnswerOption("La impresora", isCorrect = false),
                AnswerOption("El ratón", isCorrect = false)
            )
        ),
        Lesson(
            id = 9,
            question = "La cámara web sirve para…",
            options = listOf(
                AnswerOption("verte y hablar en videollamadas", isCorrect = true),
                AnswerOption("imprimir fotos", isCorrect = false),
                AnswerOption("guardar la batería", isCorrect = false),
                AnswerOption("mover el cursor", isCorrect = false)
            )
        ),
        Lesson(
            id = 10,
            question = "¿Qué dispositivo sirve para pasar documentos de la pantalla al papel?",
            options = listOf(
                AnswerOption("La impresora", isCorrect = true),
                AnswerOption("El monitor", isCorrect = false),
                AnswerOption("La memoria USB", isCorrect = false),
                AnswerOption("Las bocinas", isCorrect = false)
            )
        )
    )

    // -----------------------------------------------------------------------
    //  CAPÍTULO 2 — SOFTWARE (11–20)
    // -----------------------------------------------------------------------

    private val leccionesSoftware: List<Lesson> = listOf(
        Lesson(
            id = 11,
            question = "¿Qué es el software?",
            options = listOf(
                AnswerOption("Los programas e instrucciones que usa la computadora", isCorrect = true),
                AnswerOption("El metal y el plástico de la computadora", isCorrect = false),
                AnswerOption("El cable del cargador", isCorrect = false),
                AnswerOption("La silla del escritorio", isCorrect = false)
            )
        ),
        Lesson(
            id = 12,
            question = "¿Cuál de estos es un ejemplo de software?",
            options = listOf(
                AnswerOption("La app de dibujo", isCorrect = true),
                AnswerOption("El ratón", isCorrect = false),
                AnswerOption("La pantalla", isCorrect = false),
                AnswerOption("El teclado", isCorrect = false)
            )
        ),
        Lesson(
            id = 13,
            question = "¿Qué hace el sistema operativo?",
            options = listOf(
                AnswerOption("Administra los programas y archivos de la computadora", isCorrect = true),
                AnswerOption("Imprime las tareas", isCorrect = false),
                AnswerOption("Carga la batería", isCorrect = false),
                AnswerOption("Limpia la pantalla", isCorrect = false)
            )
        ),
        Lesson(
            id = 14,
            question = "¿Qué pasa cuando actualizas una app?",
            options = listOf(
                AnswerOption("Se corrige y mejora el software", isCorrect = true),
                AnswerOption("Cambias el teclado físico", isCorrect = false),
                AnswerOption("Se limpia el monitor", isCorrect = false),
                AnswerOption("Se cambia la CPU", isCorrect = false)
            )
        ),
        Lesson(
            id = 15,
            question = "¿Cuál es la diferencia principal entre hardware y software?",
            options = listOf(
                AnswerOption("El hardware se toca; el software son programas", isCorrect = true),
                AnswerOption("El software se toca; el hardware no", isCorrect = false),
                AnswerOption("Los dos son cables", isCorrect = false),
                AnswerOption("Los dos son aplicaciones", isCorrect = false)
            )
        ),
        Lesson(
            id = 16,
            question = "¿Qué es un videojuego instalado en la computadora?",
            options = listOf(
                AnswerOption("Software", isCorrect = true),
                AnswerOption("Hardware", isCorrect = false),
                AnswerOption("Un cable de corriente", isCorrect = false),
                AnswerOption("Una impresora", isCorrect = false)
            )
        ),
        Lesson(
            id = 17,
            question = "¿Dónde se guardan tus fotos y archivos dentro de la computadora?",
            options = listOf(
                AnswerOption("En el almacenamiento o memoria", isCorrect = true),
                AnswerOption("En el monitor", isCorrect = false),
                AnswerOption("En el teclado", isCorrect = false),
                AnswerOption("En la cámara web", isCorrect = false)
            )
        ),
        Lesson(
            id = 18,
            question = "¿Qué es una aplicación o app?",
            options = listOf(
                AnswerOption("Un programa que sirve para una tarea concreta", isCorrect = true),
                AnswerOption("Una pieza física de la computadora", isCorrect = false),
                AnswerOption("Un cable de corriente", isCorrect = false),
                AnswerOption("Una bocina", isCorrect = false)
            )
        ),
        Lesson(
            id = 19,
            question = "¿Qué significa guardar un archivo en la nube?",
            options = listOf(
                AnswerOption("Guardarlo en un servicio de internet", isCorrect = true),
                AnswerOption("Guardarlo dentro del monitor", isCorrect = false),
                AnswerOption("Guardarlo en el teclado", isCorrect = false),
                AnswerOption("Borrarlo para siempre", isCorrect = false)
            )
        ),
        Lesson(
            id = 20,
            question = "Una app se queda trabada y no responde. ¿Qué haces?",
            options = listOf(
                AnswerOption("Cerrarla, abrirla otra vez y pedir ayuda a un adulto", isCorrect = true),
                AnswerOption("Golpear el monitor", isCorrect = false),
                AnswerOption("Desconectar la computadora de golpe", isCorrect = false),
                AnswerOption("Borrar todos tus archivos", isCorrect = false)
            )
        )
    )

    // -----------------------------------------------------------------------
    //  CAPÍTULO 3 — INTERNET Y SEGURIDAD (21–30)
    // -----------------------------------------------------------------------

    private val leccionesInternet: List<Lesson> = listOf(
        Lesson(
            id = 21,
            question = "¿Quién debe conocer tu contraseña?",
            options = listOf(
                AnswerOption("Solo tú y un adulto de confianza", isCorrect = true),
                AnswerOption("Todos tus compañeros", isCorrect = false),
                AnswerOption("Cualquier persona en un juego", isCorrect = false),
                AnswerOption("Tus seguidores en redes", isCorrect = false)
            )
        ),
        Lesson(
            id = 22,
            question = "¿Cuál de estas contraseñas es la más segura?",
            options = listOf(
                AnswerOption("Luna8Taco!2026", isCorrect = true),
                AnswerOption("1234", isCorrect = false),
                AnswerOption("tupassword", isCorrect = false),
                AnswerOption("tu nombre", isCorrect = false)
            )
        ),
        Lesson(
            id = 23,
            question = "Un amigo en línea te pide tu contraseña. ¿Qué haces?",
            options = listOf(
                AnswerOption("No la compartes y avisas a un adulto", isCorrect = true),
                AnswerOption("Se la das porque es tu amigo", isCorrect = false),
                AnswerOption("La publicas en el chat", isCorrect = false),
                AnswerOption("La cambias por un regalo", isCorrect = false)
            )
        ),
        Lesson(
            id = 24,
            question = "¿Cuál de estos datos personales NO debes publicar?",
            options = listOf(
                AnswerOption("Tu dirección de casa", isCorrect = true),
                AnswerOption("Tu color favorito", isCorrect = false),
                AnswerOption("Tu animal favorito", isCorrect = false),
                AnswerOption("Tu comida favorita", isCorrect = false)
            )
        ),
        Lesson(
            id = 25,
            question = "Un desconocido en un juego te invita a hablar en privado. ¿Qué haces?",
            options = listOf(
                AnswerOption("Rechazas y le cuentas a un adulto", isCorrect = true),
                AnswerOption("Aceptas para ganar monedas", isCorrect = false),
                AnswerOption("Le das tu edad y tu escuela", isCorrect = false),
                AnswerOption("Le mandas una foto", isCorrect = false)
            )
        ),
        Lesson(
            id = 26,
            question = "Un sitio te regala monedas si escribes tu contraseña. ¿Qué haces?",
            options = listOf(
                AnswerOption("No la escribes: es una trampa", isCorrect = true),
                AnswerOption("La escribes rápido", isCorrect = false),
                AnswerOption("La compartes con tus amigos", isCorrect = false),
                AnswerOption("Creas otra cuenta y la das", isCorrect = false)
            )
        ),
        Lesson(
            id = 27,
            question = "¿Qué es el ciberacoso?",
            options = listOf(
                AnswerOption("Molestar o insultar a alguien por internet", isCorrect = true),
                AnswerOption("Jugar en línea con amigos", isCorrect = false),
                AnswerOption("Aprender a programar", isCorrect = false),
                AnswerOption("Ver videos educativos", isCorrect = false)
            )
        ),
        Lesson(
            id = 28,
            question = "¿Qué es la huella digital?",
            options = listOf(
                AnswerOption("El rastro que dejan tus publicaciones y búsquedas", isCorrect = true),
                AnswerOption("Tu huella del dedo escaneada", isCorrect = false),
                AnswerOption("La marca de tu mouse", isCorrect = false),
                AnswerOption("Un dibujo en la pantalla", isCorrect = false)
            )
        ),
        Lesson(
            id = 29,
            question = "¿Qué señal indica que un enlace puede ser peligroso?",
            options = listOf(
                AnswerOption("Pide tus datos y ofrece un premio increíble", isCorrect = true),
                AnswerOption("Es de una página oficial de tu escuela", isCorrect = false),
                AnswerOption("Lo mandó tu maestro", isCorrect = false),
                AnswerOption("Es un video educativo conocido", isCorrect = false)
            )
        ),
        Lesson(
            id = 30,
            question = "Si ves algo en internet que te asusta o te hace sentir mal…",
            options = listOf(
                AnswerOption("se lo cuentas a un adulto de confianza", isCorrect = true),
                AnswerOption("te lo guardas para ti", isCorrect = false),
                AnswerOption("lo compartes con tus amigos", isCorrect = false),
                AnswerOption("sigues viendo más", isCorrect = false)
            )
        )
    )

    // -----------------------------------------------------------------------
    //  CAPÍTULO 4 — LÓGICA Y PENSAMIENTO COMPUTACIONAL (31–40)
    // -----------------------------------------------------------------------

    private val leccionesLogica: List<Lesson> = listOf(
        Lesson(
            id = 31,
            question = "Un algoritmo es como…",
            options = listOf(
                AnswerOption("una receta: pasos ordenados para lograr algo", isCorrect = true),
                AnswerOption("un dibujo sin orden", isCorrect = false),
                AnswerOption("una canción sin letra", isCorrect = false),
                AnswerOption("un juego sin reglas", isCorrect = false)
            )
        ),
        Lesson(
            id = 32,
            question = "¿Qué es una secuencia en programación?",
            options = listOf(
                AnswerOption("Una lista de pasos en un orden", isCorrect = true),
                AnswerOption("Una nube de datos", isCorrect = false),
                AnswerOption("Un error del teclado", isCorrect = false),
                AnswerOption("Una pantalla grande", isCorrect = false)
            )
        ),
        Lesson(
            id = 33,
            question = "¿Cuál es el orden correcto para lavarte los dientes?",
            options = listOf(
                AnswerOption("Moja el cepillo, pon pasta, cepilla y enjuaga", isCorrect = true),
                AnswerOption("Enjuaga, cepilla, pon pasta y moja", isCorrect = false),
                AnswerOption("Pon pasta, enjuaga, moja y cepilla", isCorrect = false),
                AnswerOption("Cepilla en seco, guarda, moja y pon pasta", isCorrect = false)
            )
        ),
        Lesson(
            id = 34,
            question = "En programación, un bug es…",
            options = listOf(
                AnswerOption("un error que hace que el programa falle", isCorrect = true),
                AnswerOption("un insecto que vive en la computadora", isCorrect = false),
                AnswerOption("un tipo de teclado", isCorrect = false),
                AnswerOption("una contraseña segura", isCorrect = false)
            )
        ),
        Lesson(
            id = 35,
            question = "¿Qué significa depurar un programa?",
            options = listOf(
                AnswerOption("Encontrar y corregir sus errores", isCorrect = true),
                AnswerOption("Borrarlo completo", isCorrect = false),
                AnswerOption("Cambiarle el color", isCorrect = false),
                AnswerOption("Apagar la computadora", isCorrect = false)
            )
        ),
        Lesson(
            id = 36,
            question = "Un bucle o loop sirve para…",
            options = listOf(
                AnswerOption("repetir instrucciones varias veces", isCorrect = true),
                AnswerOption("guardar contraseñas", isCorrect = false),
                AnswerOption("apagar el monitor", isCorrect = false),
                AnswerOption("cambiar el idioma", isCorrect = false)
            )
        ),
        Lesson(
            id = 37,
            question = "¿Qué pasa en un bucle infinito?",
            options = listOf(
                AnswerOption("El programa repite sin parar y nunca termina", isCorrect = true),
                AnswerOption("El programa se vuelve más rápido", isCorrect = false),
                AnswerOption("La computadora se apaga sola", isCorrect = false),
                AnswerOption("El programa se guarda solo", isCorrect = false)
            )
        ),
        Lesson(
            id = 38,
            question = "¿Qué hace un condicional?",
            options = listOf(
                AnswerOption("Decide qué camino seguir según una condición", isCorrect = true),
                AnswerOption("Repite siempre lo mismo", isCorrect = false),
                AnswerOption("Guarda fotos", isCorrect = false),
                AnswerOption("Imprime documentos", isCorrect = false)
            )
        ),
        Lesson(
            id = 39,
            question = "¿Qué es una variable?",
            options = listOf(
                AnswerOption("Un espacio con nombre que guarda un valor", isCorrect = true),
                AnswerOption("Una marca en la pantalla", isCorrect = false),
                AnswerOption("Un cable de la computadora", isCorrect = false),
                AnswerOption("Un tipo de contraseña", isCorrect = false)
            )
        ),
        Lesson(
            id = 40,
            question = "Para preparar agua de limón, ¿cuál es el orden correcto?",
            options = listOf(
                AnswerOption("Lavar los limones, exprimirlos, endulzar y servir", isCorrect = true),
                AnswerOption("Servir, endulzar, lavar y exprimir", isCorrect = false),
                AnswerOption("Exprimir, servir, lavar y endulzar", isCorrect = false),
                AnswerOption("Endulzar, servir, exprimir y lavar", isCorrect = false)
            )
        )
    )

    // -----------------------------------------------------------------------
    //  CAPÍTULO 5 — PROGRAMACIÓN Y CÓDIGO (41–50)
    // -----------------------------------------------------------------------

    private val leccionesProgramacion: List<Lesson> = listOf(
        Lesson(
            id = 41,
            question = "¿Qué es un evento en un programa?",
            options = listOf(
                AnswerOption("Una acción, como un clic, que dispara código", isCorrect = true),
                AnswerOption("Un error que apaga la pantalla", isCorrect = false),
                AnswerOption("Un archivo de música", isCorrect = false),
                AnswerOption("Una pieza física de la computadora", isCorrect = false)
            )
        ),
        Lesson(
            id = 42,
            question = "¿Qué es un programa?",
            options = listOf(
                AnswerOption("Un conjunto de instrucciones para la computadora", isCorrect = true),
                AnswerOption("Un cable que conecta la energía", isCorrect = false),
                AnswerOption("Una bocina grande", isCorrect = false),
                AnswerOption("Una foto del escritorio", isCorrect = false)
            )
        ),
        Lesson(
            id = 43,
            question = "La instrucción «si pasa esto, entonces haz aquello» es…",
            options = listOf(
                AnswerOption("un condicional", isCorrect = true),
                AnswerOption("una variable", isCorrect = false),
                AnswerOption("un archivo", isCorrect = false),
                AnswerOption("una impresora", isCorrect = false)
            )
        ),
        Lesson(
            id = 44,
            question = "¿Para qué sirve un diagrama de flujo?",
            options = listOf(
                AnswerOption("Para dibujar los pasos y decisiones de un proceso", isCorrect = true),
                AnswerOption("Para guardar contraseñas", isCorrect = false),
                AnswerOption("Para imprimir en color", isCorrect = false),
                AnswerOption("Para escuchar música", isCorrect = false)
            )
        ),
        Lesson(
            id = 45,
            question = "Un programa corre sin errores, pero da una respuesta equivocada. ¿Qué falla?",
            options = listOf(
                AnswerOption("Tiene un error de lógica", isCorrect = true),
                AnswerOption("Le falta un monitor", isCorrect = false),
                AnswerOption("No tiene teclado", isCorrect = false),
                AnswerOption("Se acabó la batería", isCorrect = false)
            )
        ),
        Lesson(
            id = 46,
            question = "¿Qué haces primero para resolver un problema con código?",
            options = listOf(
                AnswerOption("Entender bien el problema y planear los pasos", isCorrect = true),
                AnswerOption("Escribir código al azar", isCorrect = false),
                AnswerOption("Apagar la computadora", isCorrect = false),
                AnswerOption("Borrar todo el proyecto", isCorrect = false)
            )
        ),
        Lesson(
            id = 47,
            question = "El pato dice «cuac» tres veces. ¿Qué usas para repetirlo sin escribirlo tres veces?",
            options = listOf(
                AnswerOption("Un bucle", isCorrect = true),
                AnswerOption("Una suma", isCorrect = false),
                AnswerOption("Un mapa", isCorrect = false),
                AnswerOption("Una impresora", isCorrect = false)
            )
        ),
        Lesson(
            id = 48,
            question = "¿Qué significa probar un programa?",
            options = listOf(
                AnswerOption("Revisar que haga lo que debe hacer en cada caso", isCorrect = true),
                AnswerOption("Cambiarle el color a la pantalla", isCorrect = false),
                AnswerOption("Guardarlo sin mirarlo", isCorrect = false),
                AnswerOption("Apagar la computadora", isCorrect = false)
            )
        ),
        Lesson(
            id = 49,
            question = "¿Por qué un robot necesita instrucciones muy precisas?",
            options = listOf(
                AnswerOption("Porque no adivina lo que queremos hacer", isCorrect = true),
                AnswerOption("Porque siempre tiene sueño", isCorrect = false),
                AnswerOption("Porque no tiene batería", isCorrect = false),
                AnswerOption("Porque no puede moverse", isCorrect = false)
            )
        ),
        Lesson(
            id = 50,
            question = "Para crear un juego, ¿cuál es el mejor orden?",
            options = listOf(
                AnswerOption("Diseñar, programar, probar y corregir", isCorrect = true),
                AnswerOption("Corregir, borrar, programar y diseñar", isCorrect = false),
                AnswerOption("Probar, diseñar, borrar y corregir", isCorrect = false),
                AnswerOption("Programar, olvidar, probar y borrar", isCorrect = false)
            )
        )
    )

    /** Los cinco capítulos del módulo, en el orden del mapa. */
    val capitulos: List<Chapter> = listOf(
        Chapter(
            id = 1,
            title = "Hardware",
            description = "Las partes físicas que puedes tocar y para qué sirve cada una.",
            jefe = "La Reina del Circuito",
            dichoDelJefe = "Mis piezas zumban y brillan. ¿Sabes para qué sirve cada una?",
            lessons = leccionesHardware
        ),
        Chapter(
            id = 2,
            title = "Software",
            description = "Programas, apps, archivos y el sistema que despierta la máquina.",
            jefe = "El Genio del Código",
            dichoDelJefe = "Sin mí, el hardware solo sería metal dormido. ¡Despiértalo conmigo!",
            lessons = leccionesSoftware
        ),
        Chapter(
            id = 3,
            title = "Internet y Seguridad",
            description = "Contraseñas, datos personales y cómo navegar sin peligro.",
            jefe = "El Guardián de la Red",
            dichoDelJefe = "La red es enorme: caminemos juntos y sin regalar tus secretos.",
            lessons = leccionesInternet
        ),
        Chapter(
            id = 4,
            title = "Lógica y Pensamiento Computacional",
            description = "Algoritmos, secuencias, bucles y decisiones ordenadas.",
            jefe = "El Búho Ordenado",
            dichoDelJefe = "Cada problema es un laberinto. ¡Encuentra el orden de los pasos!",
            lessons = leccionesLogica
        ),
        Chapter(
            id = 5,
            title = "Programación y Código",
            description = "Eventos, instrucciones y pruebas para crear tus propios programas.",
            jefe = "El Gran Glitch",
            dichoDelJefe = "Tus pasos están enredados. ¡A ver si puedes ordenarlos!",
            lessons = leccionesProgramacion
        )
    )

    /** Las 50 lecciones en el orden del mapa, de la 1 a la 50. */
    val lecciones: List<Lesson> = capitulos.flatMap { it.lessons }

    init {
        require(capitulos.size == TOTAL_CAPITULOS) {
            "Tecnología necesita exactamente $TOTAL_CAPITULOS capítulos."
        }
        require(capitulos.map { it.id } == (1..TOTAL_CAPITULOS).toList()) {
            "Los capítulos deben ir del 1 al $TOTAL_CAPITULOS en orden."
        }
        require(capitulos.all { it.lessons.size == LECCIONES_POR_CAPITULO }) {
            "Cada capítulo necesita exactamente $LECCIONES_POR_CAPITULO lecciones."
        }
        require(lecciones.size == TOTAL_LECCIONES) {
            "Tecnología necesita exactamente $TOTAL_LECCIONES lecciones."
        }
        require(lecciones.map { it.id } == (1..TOTAL_LECCIONES).toList()) {
            "Las lecciones deben ir de la 1 a la $TOTAL_LECCIONES en orden."
        }
        require(lecciones.map { it.question }.distinct().size == TOTAL_LECCIONES) {
            "Cada lección de Tecnología necesita una pregunta distinta."
        }
    }

    /** La lección pedida o `null` si el número no existe. */
    fun porId(id: Int): Lesson? = lecciones.firstOrNull { it.id == id }

    /** El capítulo al que pertenece una lección, o `null` si no existe. */
    fun capituloDe(id: Int): Chapter? =
        capitulos.firstOrNull { capitulo -> capitulo.lessons.any { it.id == id } }

    /** Título del capítulo de una lección; vacío si el número no existe. */
    fun tituloDeCapituloDe(id: Int): String = capituloDe(id)?.title.orEmpty()

    /** Lecciones de un capítulo pedido por identificador. */
    fun leccionesDe(capituloId: Int): List<Lesson> =
        capitulos.firstOrNull { it.id == capituloId }?.lessons.orEmpty()
}
