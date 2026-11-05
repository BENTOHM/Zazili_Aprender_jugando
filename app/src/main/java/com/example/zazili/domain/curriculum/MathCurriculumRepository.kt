package com.example.zazili.domain.curriculum

import androidx.compose.runtime.Immutable

/**
 * Currículo de Matemáticas de Zazili 2.0: 5 capítulos, 50 lecciones y tres
 * retos cortos por lección. El contenido sigue el sentido de los programas
 * mexicanos de primaria (sentido numérico, operaciones, fracciones, geometría
 * y estadística) y está escrito para leerse sin prisa.
 *
 * La UI nunca arma ni ordena estas listas: [MathCurriculumRepository] entrega
 * todo listo y con dificultad declarada del 1 al 5.
 */
@Immutable
data class PreguntaMate(
    val enunciado: String,
    val opciones: List<String>,
    val indiceCorrecto: Int,
    val pista: String
)

/** Una lección completa del mapa de Matemáticas. */
@Immutable
data class LessonNode(
    val id: String,
    val numero: Int,
    val capitulo: CapituloMate,
    val titulo: String,
    val subtitulo: String,
    val tier: Int,
    val icono: String,
    val conceptos: List<String>,
    val teoria: List<String>,
    val guiaFoco: List<String>,
    val preguntas: List<PreguntaMate>
) {
    val esJefeDeCapitulo: Boolean
        get() = numero % 10 == 0

    init {
        require(tier in 1..5) { "La dificultad de $id debe estar entre 1 y 5" }
        require(preguntas.isNotEmpty()) { "La lección $id necesita retos" }
        require(
            preguntas.all { it.indiceCorrecto in it.opciones.indices }
        ) { "La lección $id tiene una respuesta correcta fuera de rango" }
    }
}

/** Los cinco capítulos, con su jefe y su dicho. */
enum class CapituloMate(
    val numero: Int,
    val titulo: String,
    val lema: String,
    val jefe: String,
    val dichoDelJefe: String
) {
    SENTIDO_NUMERICO(
        numero = 1,
        titulo = "Sentido Numérico y Sumas",
        lema = "Cuenta, ordena y suma como en el tianguis.",
        jefe = "El Señor de los Costales",
        dichoDelJefe = "Aquí se cuenta con frijoles, no con calculadora. ¡Échale ganas!"
    ),
    MULTIPLICACION_DIVISION_DINERO(
        numero = 2,
        titulo = "Multiplicación, División y Dinero",
        lema = "Grupos iguales, repartos justos y cambio exacto.",
        jefe = "La Reina de las Tablas",
        dichoDelJefe = "Multiplicar es sumar sin flojera. ¡Demuéstrame que sabes!"
    ),
    FRACCIONES_DECIMALES(
        numero = 3,
        titulo = "Fracciones y Decimales",
        lema = "Rebanadas de taco, pesos con centavos y puntos que valen.",
        jefe = "El Guardián del Punto Decimal",
        dichoDelJefe = "Un punto bien puesto vale mucho. ¡No me lo muevas!"
    ),
    GEOMETRIA_MEDICION(
        numero = 4,
        titulo = "Geometría y Medición",
        lema = "Lados, ángulos, relojes y metros por todas partes.",
        jefe = "El Arquitecto de Tenochtitlan",
        dichoDelJefe = "Mide dos veces y construye una vez. ¡A ver esos perímetros!"
    ),
    ESTADISTICA_LOGICA(
        numero = 5,
        titulo = "Estadística y Lógica",
        lema = "Gráficas, patrones y acertijos para mentes brillantes.",
        jefe = "El Gran Tianguero",
        dichoDelJefe = "Los números también cuentan historias. ¿Puedes leer la mía?"
    )
}

object MathCurriculumRepository {

    val capitulos: List<CapituloMate> = CapituloMate.entries

    val lecciones: List<LessonNode> = construirLecciones()

    val totalLecciones: Int
        get() = lecciones.size

    fun leccionPorId(id: String?): LessonNode? =
        if (id.isNullOrBlank()) null else lecciones.firstOrNull { it.id == id }

    fun leccionesDe(capitulo: CapituloMate): List<LessonNode> =
        lecciones.filter { it.capitulo == capitulo }

    fun capituloPorNumero(numero: Int): CapituloMate? =
        capitulos.firstOrNull { it.numero == numero }

    private fun p(
        enunciado: String,
        opciones: List<String>,
        correcta: Int,
        pista: String
    ): PreguntaMate = PreguntaMate(
        enunciado = enunciado,
        opciones = opciones,
        indiceCorrecto = correcta,
        pista = pista
    )

    @Suppress("LongMethod")
    private fun construirLecciones(): List<LessonNode> = listOf(
        // ===================================================================
        //  CAPÍTULO 1 · SENTIDO NUMÉRICO Y SUMAS
        // ===================================================================
        LessonNode(
            id = "mate_01",
            numero = 1,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "El mandado de doña Chelo",
            subtitulo = "Cuenta de 10 en 10 y compara cantidades.",
            tier = 1,
            icono = "🧺",
            conceptos = listOf("decenas", "unidades", "mayor"),
            teoria = listOf(
                "Contar de 10 en 10 es juntar decenas: 10, 20, 30…",
                "Para comparar dos números, mira primero las decenas y luego las unidades."
            ),
            guiaFoco = listOf("10", "20", "30", "🔟"),
            preguntas = listOf(
                p(
                    "¿Qué número sigue? 10, 20, 30, ___",
                    listOf("35", "40", "50", "31"),
                    1,
                    "Cuenta de 10 en 10: después de 30 viene 40."
                ),
                p(
                    "Doña Chelo tiene 24 jitomates y 3 pepinos. ¿Cuántos vegetales hay?",
                    listOf("26", "27", "28", "21"),
                    1,
                    "Suma unidades y decenas: 24 + 3 = 27."
                ),
                p(
                    "¿Cuál número es mayor: 58 u 85?",
                    listOf("58", "85", "Son iguales", "No se puede saber"),
                    1,
                    "85 tiene 8 decenas y 58 tiene 5. ¡85 es mayor!"
                )
            )
        ),
        LessonNode(
            id = "mate_02",
            numero = 2,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "Sumas de tianguis",
            subtitulo = "Junta dos grupos y encuentra el total.",
            tier = 1,
            icono = "➕",
            conceptos = listOf("sumar", "unidades"),
            teoria = listOf(
                "Sumar es juntar dos grupos en uno solo.",
                "Primero suma las unidades y después las decenas."
            ),
            guiaFoco = listOf("🍅", "➕", "🍅", "🟰"),
            preguntas = listOf(
                p(
                    "4 + 5 = ?",
                    listOf("8", "9", "10", "11"),
                    1,
                    "Empieza en 4 y cuenta 5 dedos más: llegarás a 9."
                ),
                p(
                    "7 + 6 = ?",
                    listOf("12", "13", "14", "15"),
                    1,
                    "7 + 3 = 10, y todavía faltan 3: el total es 13."
                ),
                p(
                    "Doña Chelo vendió 8 elotes y luego 9. ¿Cuántos fueron?",
                    listOf("16", "17", "18", "19"),
                    1,
                    "8 + 9 es como 8 + 8 + 1: el total es 17."
                )
            )
        ),
        LessonNode(
            id = "mate_03",
            numero = 3,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "Decenas que se juntan",
            subtitulo = "Suma grupos de diez sin contar uno por uno.",
            tier = 1,
            icono = "🔟",
            conceptos = listOf("decenas", "sumar"),
            teoria = listOf(
                "30 es lo mismo que 3 decenas.",
                "Al sumar decenas, puedes contar de 10 en 10."
            ),
            guiaFoco = listOf("3 decenas", "➕", "4 decenas", "🟰"),
            preguntas = listOf(
                p(
                    "30 + 40 = ?",
                    listOf("60", "70", "80", "100"),
                    1,
                    "3 decenas más 4 decenas son 7 decenas: 70."
                ),
                p(
                    "20 + 50 + 10 = ?",
                    listOf("70", "80", "90", "100"),
                    1,
                    "Cuenta de 10 en 10: 20, 70, 80. El total es 80."
                ),
                p(
                    "Un costal tiene 40 frijoles y otro 30. ¿Cuántos son?",
                    listOf("60", "70", "80", "90"),
                    1,
                    "40 + 30 = 70. Son 7 decenas de frijoles."
                )
            )
        ),
        LessonNode(
            id = "mate_04",
            numero = 4,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "El cajón de los valores",
            subtitulo = "Unidades, decenas y centenas: cada quien en su cajón.",
            tier = 2,
            icono = "📦",
            conceptos = listOf("centenas", "decenas", "unidades"),
            teoria = listOf(
                "En 472, el 4 vale 400, el 7 vale 70 y el 2 vale 2.",
                "El lugar de cada número cambia su valor."
            ),
            guiaFoco = listOf("300", "70", "2", "📦"),
            preguntas = listOf(
                p(
                    "En 472, ¿qué valor tiene el 7?",
                    listOf("7 unidades", "7 decenas", "7 centenas", "7 miles"),
                    1,
                    "El 7 está en el segundo lugar desde la derecha: vale 70."
                ),
                p(
                    "¿Cómo se descompone 305?",
                    listOf("3 + 0 + 5", "300 + 50", "300 + 5", "30 + 5"),
                    2,
                    "3 centenas valen 300, 0 decenas y 5 unidades."
                ),
                p(
                    "¿Qué número tiene 6 centenas, 2 decenas y 0 unidades?",
                    listOf("620", "602", "260", "206"),
                    0,
                    "600 + 20 + 0 = 620. ¡Cada cifra en su cajón!"
                )
            )
        ),
        LessonNode(
            id = "mate_05",
            numero = 5,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "Números romanos I, V, X",
            subtitulo = "Aprende los tres símbolos básicos del mercado antiguo.",
            tier = 2,
            icono = "🏛️",
            conceptos = listOf("romano", "número"),
            teoria = listOf(
                "I vale 1, V vale 5 y X vale 10.",
                "Un símbolo menor antes de uno mayor se resta: IV es 4."
            ),
            guiaFoco = listOf("I", "V", "X", "🏛️"),
            preguntas = listOf(
                p(
                    "¿Qué número es VII?",
                    listOf("5", "6", "7", "8"),
                    2,
                    "V vale 5 y II vale 2: 5 + 2 = 7."
                ),
                p(
                    "¿Cómo se escribe 9 en romano?",
                    listOf("VIIII", "IX", "XI", "IV"),
                    1,
                    "10 menos 1 se escribe IX."
                ),
                p(
                    "¿Qué número es XIV?",
                    listOf("14", "16", "19", "4"),
                    0,
                    "X vale 10, IV vale 4: 10 + 4 = 14."
                )
            )
        ),
        LessonNode(
            id = "mate_06",
            numero = 6,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "Romanos hasta el C",
            subtitulo = "Resta y suma símbolos romanos como un experto.",
            tier = 3,
            icono = "🏺",
            conceptos = listOf("romano", "resta"),
            teoria = listOf(
                "L vale 50 y C vale 100.",
                "Cuando un símbolo pequeño va antes de uno grande, se resta."
            ),
            guiaFoco = listOf("XL", "XC", "C", "🏺"),
            preguntas = listOf(
                p(
                    "¿Qué número es XL?",
                    listOf("40", "60", "90", "15"),
                    0,
                    "L vale 50 y X vale 10: 50 − 10 = 40."
                ),
                p(
                    "¿Cómo se escribe 90?",
                    listOf("XC", "CX", "LXXXX", "C"),
                    0,
                    "C vale 100 y X vale 10: 100 − 10 = 90, o sea XC."
                ),
                p(
                    "¿Qué número es XCIV?",
                    listOf("94", "104", "96", "64"),
                    0,
                    "XC vale 90 e IV vale 4: 90 + 4 = 94."
                )
            )
        ),
        LessonNode(
            id = "mate_07",
            numero = 7,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "Pares, impares y saltos",
            subtitulo = "Descubre el ritmo de los números.",
            tier = 1,
            icono = "🦘",
            conceptos = listOf("par", "impar"),
            teoria = listOf(
                "Los pares terminan en 0, 2, 4, 6 u 8.",
                "Contar de 5 en 5 es como dar saltos de canguro."
            ),
            guiaFoco = listOf("2", "4", "6", "8", "🦘"),
            preguntas = listOf(
                p(
                    "¿Cuál de estos números es par?",
                    listOf("13", "27", "38", "41"),
                    2,
                    "38 termina en 8, así que es par."
                ),
                p(
                    "¿Qué número sigue al contar de 5 en 5? 5, 10, 15, ___",
                    listOf("18", "20", "25", "30"),
                    1,
                    "Después de 15, un salto de 5 llega a 20."
                ),
                p(
                    "¿Cuál de estos números es impar?",
                    listOf("24", "36", "48", "57"),
                    3,
                    "57 termina en 7: no es par, es impar."
                )
            )
        ),
        LessonNode(
            id = "mate_08",
            numero = 8,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "Redondeo al diez más cercano",
            subtitulo = "Aproxima cantidades para calcular más rápido.",
            tier = 2,
            icono = "🎯",
            conceptos = listOf("redondear", "decena"),
            teoria = listOf(
                "Redondear es elegir la decena más cercana.",
                "Si las unidades son 5 o más, sube a la siguiente decena."
            ),
            guiaFoco = listOf("30", "35", "40", "🎯"),
            preguntas = listOf(
                p(
                    "¿A qué decena se acerca más 38?",
                    listOf("30", "40", "50", "100"),
                    1,
                    "38 está a 2 de 40 y a 8 de 30: se redondea a 40."
                ),
                p(
                    "¿A qué decena se acerca más 72?",
                    listOf("60", "70", "80", "90"),
                    1,
                    "72 está a 2 de 70 y a 8 de 80: se redondea a 70."
                ),
                p(
                    "¿Cuál número redondea a 50?",
                    listOf("41", "44", "54", "59"),
                    2,
                    "54 está más cerca de 50 que de 60. Los demás no."
                )
            )
        ),
        LessonNode(
            id = "mate_09",
            numero = 9,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "Sumas con acarreo",
            subtitulo = "Cuando las unidades pasan de diez, lleva una decena.",
            tier = 2,
            icono = "🎒",
            conceptos = listOf("acarreo", "decenas", "unidades"),
            teoria = listOf(
                "Si al sumar unidades obtienes 10 o más, llevas una decena.",
                "El acarreo se suma con las decenas, no se pierde."
            ),
            guiaFoco = listOf("27", "15", "42", "🎒"),
            preguntas = listOf(
                p(
                    "27 + 15 = ?",
                    listOf("32", "41", "42", "43"),
                    2,
                    "7 + 5 = 12: escribe 2 y lleva 1 a las decenas. 2 + 1 + 1 = 4."
                ),
                p(
                    "48 + 26 = ?",
                    listOf("64", "72", "74", "84"),
                    2,
                    "8 + 6 = 14: escribe 4 y lleva 1. 4 + 2 + 1 = 7 decenas."
                ),
                p(
                    "La tienda vendió 39 tacos y 45 quesadillas. ¿Cuántas?",
                    listOf("74", "83", "84", "85"),
                    2,
                    "9 + 5 = 14: escribe 4 y lleva 1. 3 + 4 + 1 = 8 decenas."
                )
            )
        ),
        LessonNode(
            id = "mate_10",
            numero = 10,
            capitulo = CapituloMate.SENTIDO_NUMERICO,
            titulo = "La resta del cambio",
            subtitulo = "Resta y comprueba que tu cuenta cuadre.",
            tier = 3,
            icono = "💵",
            conceptos = listOf("resta", "comprobar"),
            teoria = listOf(
                "Restar es quitar una cantidad de otra.",
                "Puedes comprobar una resta sumando el resultado."
            ),
            guiaFoco = listOf("50", "−23", "27", "💵"),
            preguntas = listOf(
                p(
                    "50 − 23 = ?",
                    listOf("27", "28", "33", "37"),
                    0,
                    "23 + 27 = 50, así que 50 − 23 = 27."
                ),
                p(
                    "Doña Chelo tenía 82 pesos y gastó 35. ¿Cuánto le queda?",
                    listOf("47", "48", "53", "57"),
                    0,
                    "82 − 35 = 47. Puedes comprobarlo con 47 + 35 = 82."
                ),
                p(
                    "¿Qué resta comprueba 64 + 26 = 90?",
                    listOf(
                        "90 − 64 = 26",
                        "64 − 26 = 38",
                        "90 + 26 = 116",
                        "26 − 64 = 0"
                    ),
                    0,
                    "Si 64 + 26 = 90, al quitar 64 a 90 vuelve a quedar 26."
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 2 · MULTIPLICACIÓN, DIVISIÓN Y DINERO
        // ===================================================================
        LessonNode(
            id = "mate_11",
            numero = 11,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Grupos que se multiplican",
            subtitulo = "Multiplicar es sumar grupos iguales.",
            tier = 1,
            icono = "✖️",
            conceptos = listOf("multiplicar", "grupos"),
            teoria = listOf(
                "3 × 4 significa 3 grupos de 4: 4 + 4 + 4.",
                "El signo × se lee «por»."
            ),
            guiaFoco = listOf("🍽️", "🌮🌮🌮", "✖️", "🟰"),
            preguntas = listOf(
                p(
                    "¿Cuánto es 3 × 4?",
                    listOf("7", "12", "14", "16"),
                    1,
                    "Suma 4 tres veces: 4 + 4 + 4 = 12."
                ),
                p(
                    "¿Qué suma representa 5 × 2?",
                    listOf("5 + 2", "5 + 5", "2 + 2 + 2", "5 + 5 + 5"),
                    1,
                    "5 × 2 son dos grupos de 5: 5 + 5."
                ),
                p(
                    "Hay 4 platos con 3 tacos cada uno. ¿Cuántos tacos?",
                    listOf("7", "10", "12", "14"),
                    2,
                    "4 grupos de 3: 3 + 3 + 3 + 3 = 12."
                )
            )
        ),
        LessonNode(
            id = "mate_12",
            numero = 12,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Tablas del 2, 5 y 10",
            subtitulo = "Las tablas más amigables del tianguis.",
            tier = 1,
            icono = "🖐️",
            conceptos = listOf("tabla", "multiplicar"),
            teoria = listOf(
                "La tabla del 2 es contar de 2 en 2.",
                "La del 5 termina en 0 o en 5; la del 10 siempre termina en 0."
            ),
            guiaFoco = listOf("2", "5", "10", "🖐️"),
            preguntas = listOf(
                p(
                    "2 × 7 = ?",
                    listOf("12", "14", "16", "17"),
                    1,
                    "Cuenta de 2 en 2 siete veces: llegarás a 14."
                ),
                p(
                    "5 × 6 = ?",
                    listOf("25", "30", "35", "40"),
                    1,
                    "Cuenta de 5 en 5 seis veces: 5, 10, 15, 20, 25, 30."
                ),
                p(
                    "10 × 8 = ?",
                    listOf("18", "70", "80", "90"),
                    2,
                    "Multiplicar por 10 es agregar un cero: 80."
                )
            )
        ),
        LessonNode(
            id = "mate_13",
            numero = 13,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Tablas del 3 y 4",
            subtitulo = "Dobla grupos y suma uno más.",
            tier = 2,
            icono = "🍫",
            conceptos = listOf("tabla", "multiplicar"),
            teoria = listOf(
                "3 × 6 es lo mismo que 6 + 6 + 6.",
                "4 × 7 es el doble de 2 × 7."
            ),
            guiaFoco = listOf("3", "4", "🍫", "🍫"),
            preguntas = listOf(
                p(
                    "3 × 6 = ?",
                    listOf("15", "18", "21", "24"),
                    1,
                    "6 + 6 + 6 = 18."
                ),
                p(
                    "4 × 7 = ?",
                    listOf("24", "26", "28", "32"),
                    2,
                    "4 × 7 es el doble de 14: 28."
                ),
                p(
                    "Compraste 4 bolsas con 8 canicas. ¿Cuántas canicas?",
                    listOf("12", "24", "32", "36"),
                    2,
                    "4 grupos de 8: 8 × 4 = 32."
                )
            )
        ),
        LessonNode(
            id = "mate_14",
            numero = 14,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Tablas del 6 y 7",
            subtitulo = "Dos tablas que parecen difíciles, pero no lo son.",
            tier = 2,
            icono = "🧮",
            conceptos = listOf("tabla", "multiplicar"),
            teoria = listOf(
                "6 × 6 se puede ver como 6 grupos de 6.",
                "7 × 8 es 7 grupos de 8; también es 8 grupos de 7."
            ),
            guiaFoco = listOf("6", "7", "🧮", "🟰"),
            preguntas = listOf(
                p(
                    "6 × 6 = ?",
                    listOf("30", "32", "36", "42"),
                    2,
                    "6 + 6 + 6 + 6 + 6 + 6 = 36."
                ),
                p(
                    "7 × 8 = ?",
                    listOf("48", "54", "56", "64"),
                    2,
                    "7 × 8 = 8 × 7 = 56."
                ),
                p(
                    "6 cajas con 9 huevos. ¿Cuántos huevos?",
                    listOf("45", "54", "56", "63"),
                    1,
                    "6 × 9 = 6 × 10 − 6 = 54."
                )
            )
        ),
        LessonNode(
            id = "mate_15",
            numero = 15,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Tablas del 8 y 9",
            subtitulo = "Trucos para las dos tablas más grandes.",
            tier = 3,
            icono = "🎲",
            conceptos = listOf("tabla", "multiplicar"),
            teoria = listOf(
                "8 × 7 es el doble de 4 × 7.",
                "En la tabla del 9, las cifras del resultado suman 9."
            ),
            guiaFoco = listOf("8", "9", "🎲", "🟰"),
            preguntas = listOf(
                p(
                    "8 × 7 = ?",
                    listOf("48", "54", "56", "64"),
                    2,
                    "8 × 7 = 56. Puedes verlo como 4 × 7 = 28 y su doble."
                ),
                p(
                    "9 × 9 = ?",
                    listOf("72", "81", "89", "90"),
                    1,
                    "9 × 9 = 9 × 10 − 9 = 81."
                ),
                p(
                    "Hay 8 mesas con 6 sillas. ¿Cuántas sillas?",
                    listOf("42", "46", "48", "54"),
                    2,
                    "8 × 6 = 48. Ocho grupos de seis sillas."
                )
            )
        ),
        LessonNode(
            id = "mate_16",
            numero = 16,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Repartir en partes iguales",
            subtitulo = "La división es un reparto justo.",
            tier = 2,
            icono = "🍽️",
            conceptos = listOf("dividir", "repartir"),
            teoria = listOf(
                "Dividir es repartir en grupos iguales.",
                "El resultado de una división se llama cociente."
            ),
            guiaFoco = listOf("🌮🌮🌮", "3 amigos", "÷", "🍽️"),
            preguntas = listOf(
                p(
                    "12 tacos entre 3 amigos. ¿Cuántos cada uno?",
                    listOf("3", "4", "5", "6"),
                    1,
                    "12 ÷ 3 = 4. Cada amigo recibe 4 tacos."
                ),
                p(
                    "20 dulces en 5 bolsas. ¿Cuántos por bolsa?",
                    listOf("4", "5", "6", "10"),
                    0,
                    "20 ÷ 5 = 4 dulces por bolsa."
                ),
                p(
                    "¿Cuál división da 6?",
                    listOf("18 ÷ 3", "24 ÷ 6", "30 ÷ 6", "12 ÷ 4"),
                    0,
                    "18 ÷ 3 = 6. Las otras dan 4, 5 y 3."
                )
            )
        ),
        LessonNode(
            id = "mate_17",
            numero = 17,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "División con residuo",
            subtitulo = "A veces sobra algo y también cuenta.",
            tier = 3,
            icono = "🍬",
            conceptos = listOf("residuo", "dividir"),
            teoria = listOf(
                "El residuo es lo que sobra cuando no se puede repartir completo.",
                "Multiplica el cociente por el divisor y suma el residuo para comprobar."
            ),
            guiaFoco = listOf("13", "÷ 4", "3 y sobra 1", "🍬"),
            preguntas = listOf(
                p(
                    "¿Cuánto sobra en 13 ÷ 4?",
                    listOf("1", "2", "3", "4"),
                    0,
                    "4 × 3 = 12, y 13 − 12 = 1. Sobra 1."
                ),
                p(
                    "14 ÷ 4 = ?",
                    listOf("2 y sobran 6", "3 y sobran 2", "3 y sobran 4", "4 y sobran 2"),
                    1,
                    "4 × 3 = 12; 14 − 12 = 2. Son 3 y sobran 2."
                ),
                p(
                    "17 dulces entre 5 niños. ¿Cuántos sobran?",
                    listOf("1", "2", "3", "4"),
                    1,
                    "5 × 3 = 15; 17 − 15 = 2. Sobran 2 dulces."
                )
            )
        ),
        LessonNode(
            id = "mate_18",
            numero = 18,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Dos pasos, una respuesta",
            subtitulo = "Primero una operación, luego la otra.",
            tier = 3,
            icono = "👣",
            conceptos = listOf("pasos", "multiplicar"),
            teoria = listOf(
                "Algunos problemas necesitan dos operaciones.",
                "Lee con calma y decide qué hacer primero."
            ),
            guiaFoco = listOf("1️⃣", "2️⃣", "👣", "🟰"),
            preguntas = listOf(
                p(
                    "Compras 3 tacos de \$10 y un agua de \$8. ¿Cuánto pagas?",
                    listOf("\$30", "\$38", "\$40", "\$48"),
                    1,
                    "Primero 3 × \$10 = \$30; luego \$30 + \$8 = \$38."
                ),
                p(
                    "Hay 4 cajas con 5 lápices y se pierden 3. ¿Cuántos quedan?",
                    listOf("17", "18", "20", "23"),
                    0,
                    "4 × 5 = 20 lápices; 20 − 3 = 17."
                ),
                p(
                    "Doña Chelo tenía 50 pesos, compró 2 elotes de \$10. ¿Cuánto le queda?",
                    listOf("\$20", "\$30", "\$40", "\$50"),
                    1,
                    "2 × \$10 = \$20; \$50 − \$20 = \$30."
                )
            )
        ),
        LessonNode(
            id = "mate_19",
            numero = 19,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "Monedas y billetes mexicanos",
            subtitulo = "Cuenta tu dinero como un tianguero experto.",
            tier = 2,
            icono = "🪙",
            conceptos = listOf("monedas", "billetes", "pesos"),
            teoria = listOf(
                "Las monedas de \$5 y \$10 se suman como grupos.",
                "Un billete de \$20 vale lo mismo que dos de \$10."
            ),
            guiaFoco = listOf("🪙 \$5", "🪙 \$5", "🪙 \$2", "🟰"),
            preguntas = listOf(
                p(
                    "¿Cuánto son dos monedas de \$5 y una de \$2?",
                    listOf("\$7", "\$10", "\$12", "\$15"),
                    2,
                    "\$5 + \$5 + \$2 = \$12."
                ),
                p(
                    "¿Cuánto son un billete de \$20 y dos de \$10?",
                    listOf("\$30", "\$40", "\$50", "\$60"),
                    1,
                    "\$20 + \$10 + \$10 = \$40."
                ),
                p(
                    "¿Con qué monedas juntas \$10?",
                    listOf(
                        "Dos de \$5",
                        "Tres de \$5",
                        "Una de \$2",
                        "Una de \$10 y una de \$5"
                    ),
                    0,
                    "Dos monedas de \$5 hacen \$10."
                )
            )
        ),
        LessonNode(
            id = "mate_20",
            numero = 20,
            capitulo = CapituloMate.MULTIPLICACION_DIVISION_DINERO,
            titulo = "El cambio del tianguis",
            subtitulo = "Compra, paga y calcula lo que te devuelven.",
            tier = 4,
            icono = "🛍️",
            conceptos = listOf("cambio", "pesos"),
            teoria = listOf(
                "El cambio es lo que te devuelven después de pagar.",
                "Cambio = lo que pagas − lo que cuesta."
            ),
            guiaFoco = listOf("\$50", "− \$35", "\$15", "🛍️"),
            preguntas = listOf(
                p(
                    "Pagas \$50 por algo de \$35. ¿Cuánto cambio?",
                    listOf("\$15", "\$20", "\$25", "\$35"),
                    0,
                    "\$50 − \$35 = \$15."
                ),
                p(
                    "Dos kilos de jitomate a \$18 el kilo. Pagas con \$50. ¿Cuánto cambio?",
                    listOf("\$12", "\$14", "\$16", "\$18"),
                    1,
                    "2 × \$18 = \$36; \$50 − \$36 = \$14."
                ),
                p(
                    "Tienes \$100 y compras 3 quesadillas de \$25. ¿Cuánto queda?",
                    listOf("\$25", "\$50", "\$75", "\$100"),
                    0,
                    "3 × \$25 = \$75; \$100 − \$75 = \$25."
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 3 · FRACCIONES Y DECIMALES
        // ===================================================================
        LessonNode(
            id = "mate_21",
            numero = 21,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Mitades y cuartos de taco",
            subtitulo = "Partes iguales de una comida compartida.",
            tier = 2,
            icono = "🌮",
            conceptos = listOf("mitad", "cuartos", "fracción"),
            teoria = listOf(
                "Una mitad es 1 de 2 partes iguales.",
                "Un cuarto es 1 de 4 partes iguales."
            ),
            guiaFoco = listOf("🍕", "½", "¼", "🌮"),
            preguntas = listOf(
                p(
                    "¿Cuánto es la mitad de 12?",
                    listOf("4", "6", "8", "24"),
                    1,
                    "12 ÷ 2 = 6."
                ),
                p(
                    "Un taco partido en 4. ¿Qué fracción es cada pedazo?",
                    listOf("1/2", "1/3", "1/4", "4/1"),
                    2,
                    "Una de cuatro partes iguales se escribe 1/4."
                ),
                p(
                    "Si comes 2 de 4 pedazos, ¿qué fracción comiste?",
                    listOf("1/4", "1/2", "3/4", "4/4"),
                    1,
                    "2 de 4 es lo mismo que la mitad: 1/2."
                )
            )
        ),
        LessonNode(
            id = "mate_22",
            numero = 22,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Fracción de una colección",
            subtitulo = "Toma una parte de un grupo de objetos.",
            tier = 2,
            icono = "🫘",
            conceptos = listOf("fracción", "colección"),
            teoria = listOf(
                "Para sacar 1/2 de una colección, divide entre 2.",
                "Para sacar 1/4, divide entre 4."
            ),
            guiaFoco = listOf("🫘🫘🫘🫘", "¼", "🫘", "🟰"),
            preguntas = listOf(
                p(
                    "¿Cuánto es 1/2 de 10?",
                    listOf("2", "5", "8", "10"),
                    1,
                    "10 ÷ 2 = 5."
                ),
                p(
                    "¿Cuánto es 1/4 de 8?",
                    listOf("2", "4", "6", "8"),
                    0,
                    "8 ÷ 4 = 2."
                ),
                p(
                    "En una bolsa hay 12 canicas. Tomas 1/3. ¿Cuántas tomas?",
                    listOf("3", "4", "6", "9"),
                    1,
                    "12 ÷ 3 = 4 canicas."
                )
            )
        ),
        LessonNode(
            id = "mate_23",
            numero = 23,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Fracciones equivalentes",
            subtitulo = "Dos nombres para la misma rebanada.",
            tier = 3,
            icono = "🧩",
            conceptos = listOf("equivalente", "fracción"),
            teoria = listOf(
                "Fracciones equivalentes valen lo mismo aunque se escriban distinto.",
                "Si multiplicas arriba y abajo por el mismo número, la fracción no cambia."
            ),
            guiaFoco = listOf("1/2", "🟰", "2/4", "🧩"),
            preguntas = listOf(
                p(
                    "¿Cuál fracción es equivalente a 1/2?",
                    listOf("2/3", "2/4", "3/4", "4/5"),
                    1,
                    "2/4 es la mitad: si divides 2 y 4 entre 2, vuelve a 1/2."
                ),
                p(
                    "¿Cuál fracción es equivalente a 2/4?",
                    listOf("1/2", "1/3", "3/4", "2/3"),
                    0,
                    "2/4 se simplifica a 1/2."
                ),
                p(
                    "Completa: 3/6 = ?/2",
                    listOf("1", "2", "3", "4"),
                    0,
                    "3/6 es la mitad, y la mitad de 2 es 1: 1/2."
                )
            )
        ),
        LessonNode(
            id = "mate_24",
            numero = 24,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "¿Cuál fracción es mayor?",
            subtitulo = "Compara rebanadas con el mismo denominador.",
            tier = 3,
            icono = "⚖️",
            conceptos = listOf("mayor", "fracción"),
            teoria = listOf(
                "Si el denominador es igual, gana la fracción con el numerador más grande.",
                "3/4 es mayor que 1/4 porque tiene más rebanadas."
            ),
            guiaFoco = listOf("3/4", "vs", "1/4", "⚖️"),
            preguntas = listOf(
                p(
                    "¿Qué es mayor: 3/4 o 1/4?",
                    listOf("3/4", "1/4", "Son iguales", "No se puede"),
                    0,
                    "Con el mismo denominador, 3 rebanadas son más que 1."
                ),
                p(
                    "¿Qué es mayor: 2/5 o 4/5?",
                    listOf("2/5", "4/5", "Iguales", "Falta información"),
                    1,
                    "4/5 tiene más rebanadas que 2/5."
                ),
                p(
                    "Ordena de menor a mayor: 1/2, 1/4, 3/4",
                    listOf(
                        "1/4, 1/2, 3/4",
                        "1/2, 1/4, 3/4",
                        "3/4, 1/2, 1/4",
                        "1/4, 3/4, 1/2"
                    ),
                    0,
                    "1/4 es la más chica; luego 1/2; y 3/4 es la más grande."
                )
            )
        ),
        LessonNode(
            id = "mate_25",
            numero = 25,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Sumar rebanadas iguales",
            subtitulo = "Junta fracciones con el mismo denominador.",
            tier = 3,
            icono = "🍕",
            conceptos = listOf("fracción", "denominador"),
            teoria = listOf(
                "Si las fracciones tienen el mismo denominador, suma los numeradores.",
                "El denominador se queda igual."
            ),
            guiaFoco = listOf("1/4", "➕", "1/4", "🟰 2/4"),
            preguntas = listOf(
                p(
                    "1/4 + 1/4 = ?",
                    listOf("1/8", "2/8", "3/4", "2/4"),
                    3,
                    "1 + 1 = 2; el denominador sigue siendo 4: 2/4."
                ),
                p(
                    "2/5 + 1/5 = ?",
                    listOf("3/5", "3/10", "2/10", "1/5"),
                    0,
                    "2 + 1 = 3; el denominador se queda en 5: 3/5."
                ),
                p(
                    "Comiste 1/8 de pizza y luego 3/8. ¿Cuánto comiste?",
                    listOf("4/8", "3/8", "1/8", "4/16"),
                    0,
                    "1 + 3 = 4; el denominador es 8: 4/8."
                )
            )
        ),
        LessonNode(
            id = "mate_26",
            numero = 26,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Décimas y centésimas",
            subtitulo = "Del 1/10 al 0.01 sin perderte.",
            tier = 2,
            icono = "🎟️",
            conceptos = listOf("décimas", "centésimas", "decimal"),
            teoria = listOf(
                "Una décima se escribe 0.1; una centésima, 0.01.",
                "0.25 son 25 centésimas, o sea un cuarto."
            ),
            guiaFoco = listOf("0.1", "0.01", "0.25", "🎟️"),
            preguntas = listOf(
                p(
                    "¿Cómo se escribe 3/10 en decimal?",
                    listOf("0.3", "3.0", "0.03", "30"),
                    0,
                    "3 décimas es 0.3."
                ),
                p(
                    "¿Cómo se escribe 0.25 en fracción?",
                    listOf("1/2", "1/4", "2/5", "1/5"),
                    1,
                    "0.25 es 25 centésimas, que simplificadas son 1/4."
                ),
                p(
                    "¿Qué número es mayor: 0.7 u 0.07?",
                    listOf("0.7", "0.07", "Iguales", "Falta información"),
                    0,
                    "0.7 son 7 décimas y 0.07 son 7 centésimas: 0.7 es mayor."
                )
            )
        ),
        LessonNode(
            id = "mate_27",
            numero = 27,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Punto decimal en acción",
            subtitulo = "Suma y resta con punto decimal.",
            tier = 3,
            icono = "🪄",
            conceptos = listOf("decimal", "décimas"),
            teoria = listOf(
                "Acomoda los puntos decimales uno debajo del otro.",
                "Suma o resta como siempre y baja el punto."
            ),
            guiaFoco = listOf("1.5", "➕", "2.3", "🟰 3.8"),
            preguntas = listOf(
                p(
                    "1.5 + 2.3 = ?",
                    listOf("3.8", "3.9", "4.8", "3.7"),
                    0,
                    "5 décimas + 3 décimas = 8 décimas: 3.8."
                ),
                p(
                    "4.6 − 1.2 = ?",
                    listOf("3.2", "3.4", "4.4", "2.6"),
                    1,
                    "6 − 2 = 4 décimas y 4 − 1 = 3 enteros: 3.4."
                ),
                p(
                    "0.5 + 0.25 = ?",
                    listOf("0.30", "0.55", "0.75", "1.0"),
                    2,
                    "0.50 + 0.25 = 0.75."
                )
            )
        ),
        LessonNode(
            id = "mate_28",
            numero = 28,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Pesos con centavos",
            subtitulo = "El dinero también usa punto decimal.",
            tier = 3,
            icono = "💰",
            conceptos = listOf("centavos", "pesos", "decimal"),
            teoria = listOf(
                "Un peso se escribe \$1.00; cincuenta centavos, \$0.50.",
                "Al sumar dinero, alinea los puntos decimales."
            ),
            guiaFoco = listOf("\$12.50", "➕", "\$7.50", "🟰 \$20.00"),
            preguntas = listOf(
                p(
                    "¿Cuánto es \$12.50 + \$7.50?",
                    listOf("\$18.00", "\$19.00", "\$20.00", "\$21.00"),
                    2,
                    "\$12.50 + \$7.50 = \$20.00."
                ),
                p(
                    "Pagas \$100 por \$38.75. ¿Cuánto cambio?",
                    listOf("\$61.25", "\$62.25", "\$61.75", "\$62.75"),
                    0,
                    "\$100.00 − \$38.75 = \$61.25."
                ),
                p(
                    "Dos jugos de \$9.50. ¿Total?",
                    listOf("\$17.00", "\$18.00", "\$19.00", "\$20.00"),
                    2,
                    "2 × \$9.50 = \$19.00."
                )
            )
        ),
        LessonNode(
            id = "mate_29",
            numero = 29,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "La recta de las fracciones",
            subtitulo = "Ubica fracciones y decimales entre 0 y 1.",
            tier = 4,
            icono = "📏",
            conceptos = listOf("fracción", "recta", "decimal"),
            teoria = listOf(
                "En la recta, 0 está a la izquierda y 1 a la derecha.",
                "1/2, 0.5 y 2/4 caen exactamente en el mismo punto."
            ),
            guiaFoco = listOf("0", "0.5", "1", "📏"),
            preguntas = listOf(
                p(
                    "¿Qué fracción está a la mitad entre 0 y 1?",
                    listOf("1/4", "1/3", "1/2", "2/1"),
                    2,
                    "La mitad de la recta entre 0 y 1 es 1/2."
                ),
                p(
                    "¿Qué número está entre 0.2 y 0.4?",
                    listOf("0.1", "0.3", "0.5", "0.6"),
                    1,
                    "0.3 está justo en medio de 0.2 y 0.4."
                ),
                p(
                    "¿Cuál fracción está más cerca de 1?",
                    listOf("1/5", "2/5", "4/5", "1/10"),
                    2,
                    "4/5 está a solo 1/5 de distancia de 1."
                )
            )
        ),
        LessonNode(
            id = "mate_30",
            numero = 30,
            capitulo = CapituloMate.FRACCIONES_DECIMALES,
            titulo = "Porcentajes de fiesta",
            subtitulo = "50%, 25% y 10% con piñata incluida.",
            tier = 4,
            icono = "🎉",
            conceptos = listOf("porcentaje", "mitad"),
            teoria = listOf(
                "50% es la mitad de una cantidad.",
                "25% es la cuarta parte; 10% se obtiene dividiendo entre 10."
            ),
            guiaFoco = listOf("50%", "25%", "10%", "🎉"),
            preguntas = listOf(
                p(
                    "¿Cuánto es 50% de 40?",
                    listOf("10", "20", "30", "40"),
                    1,
                    "50% es la mitad de 40: 20."
                ),
                p(
                    "¿Cuánto es 25% de 80?",
                    listOf("20", "25", "30", "40"),
                    0,
                    "25% es la cuarta parte: 80 ÷ 4 = 20."
                ),
                p(
                    "¿Cuánto es 10% de 90?",
                    listOf("9", "10", "19", "90"),
                    0,
                    "10% es dividir entre 10: 90 ÷ 10 = 9."
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 4 · GEOMETRÍA Y MEDICIÓN
        // ===================================================================
        LessonNode(
            id = "mate_31",
            numero = 31,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Figuras de todos lados",
            subtitulo = "Reconoce figuras por sus lados.",
            tier = 1,
            icono = "🔷",
            conceptos = listOf("lados", "figura"),
            teoria = listOf(
                "El triángulo tiene 3 lados; el cuadrado, 4.",
                "El hexágono tiene 6 lados y el círculo no tiene lados rectos."
            ),
            guiaFoco = listOf("🔺", "⬛", "⬡", "🔷"),
            preguntas = listOf(
                p(
                    "¿Cuántos lados tiene un triángulo?",
                    listOf("2", "3", "4", "5"),
                    1,
                    "El triángulo se llama así por sus 3 lados."
                ),
                p(
                    "¿Cuántos lados tiene un cuadrado?",
                    listOf("3", "4", "5", "6"),
                    1,
                    "El cuadrado tiene 4 lados iguales."
                ),
                p(
                    "¿Qué figura tiene 6 lados?",
                    listOf("Triángulo", "Cuadrado", "Hexágono", "Círculo"),
                    2,
                    "Hexa significa seis: el hexágono tiene 6 lados."
                )
            )
        ),
        LessonNode(
            id = "mate_32",
            numero = 32,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Ángulos de la esquina",
            subtitulo = "Agudo, recto y obtuso: tres formas de abrirse.",
            tier = 2,
            icono = "📐",
            conceptos = listOf("ángulo", "recto", "agudo", "obtuso"),
            teoria = listOf(
                "El ángulo recto mide 90°, como la esquina de una puerta.",
                "El agudo mide menos de 90° y el obtuso más de 90°."
            ),
            guiaFoco = listOf("45°", "90°", "120°", "📐"),
            preguntas = listOf(
                p(
                    "Un ángulo recto mide…",
                    listOf("45°", "90°", "180°", "360°"),
                    1,
                    "El ángulo recto forma una escuadra perfecta: 90°."
                ),
                p(
                    "Un ángulo menor de 90° es…",
                    listOf("Recto", "Agudo", "Obtuso", "Llano"),
                    1,
                    "Si mide menos de 90°, es agudo."
                ),
                p(
                    "Un ángulo de 120° es…",
                    listOf("Agudo", "Recto", "Obtuso", "Completo"),
                    2,
                    "120° es más abierto que 90°, así que es obtuso."
                )
            )
        ),
        LessonNode(
            id = "mate_33",
            numero = 33,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "La orilla: perímetro",
            subtitulo = "Recorre el borde de una figura.",
            tier = 2,
            icono = "🚧",
            conceptos = listOf("perímetro", "lados"),
            teoria = listOf(
                "El perímetro es la suma de todos los lados.",
                "Se mide en centímetros, metros o la unidad que uses."
            ),
            guiaFoco = listOf("4 cm", "4 cm", "4 cm", "4 cm"),
            preguntas = listOf(
                p(
                    "Un cuadrado de 4 cm por lado. ¿Perímetro?",
                    listOf("8 cm", "12 cm", "16 cm", "20 cm"),
                    2,
                    "4 + 4 + 4 + 4 = 16 cm."
                ),
                p(
                    "Un rectángulo de 5 cm y 3 cm. ¿Perímetro?",
                    listOf("8 cm", "15 cm", "16 cm", "30 cm"),
                    2,
                    "5 + 3 + 5 + 3 = 16 cm."
                ),
                p(
                    "Un triángulo con lados 3, 4 y 5 cm. ¿Perímetro?",
                    listOf("7 cm", "9 cm", "12 cm", "20 cm"),
                    2,
                    "3 + 4 + 5 = 12 cm."
                )
            )
        ),
        LessonNode(
            id = "mate_34",
            numero = 34,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Área con cuadritos",
            subtitulo = "Cuenta los cuadritos que cubren una figura.",
            tier = 3,
            icono = "🟩",
            conceptos = listOf("área", "cuadritos"),
            teoria = listOf(
                "El área es la superficie que cubre una figura.",
                "Puedes contarla con cuadritos de 1 × 1."
            ),
            guiaFoco = listOf("🟩🟩🟩", "🟩🟩🟩", "🟩🟩🟩", "9"),
            preguntas = listOf(
                p(
                    "Un rectángulo de 3 × 4 cuadritos. ¿Área?",
                    listOf("7", "10", "12", "14"),
                    2,
                    "3 filas de 4 cuadritos son 12."
                ),
                p(
                    "Una figura cubre 5 filas de 3 cuadritos. ¿Área?",
                    listOf("8", "10", "15", "20"),
                    2,
                    "5 × 3 = 15 cuadritos."
                ),
                p(
                    "Un cuadrado de 4 × 4. ¿Área?",
                    listOf("8", "12", "16", "20"),
                    2,
                    "4 filas de 4 son 16 cuadritos."
                )
            )
        ),
        LessonNode(
            id = "mate_35",
            numero = 35,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Área de rectángulos",
            subtitulo = "Largo por ancho: la fórmula mágica.",
            tier = 3,
            icono = "🪟",
            conceptos = listOf("área", "rectángulo"),
            teoria = listOf(
                "Área del rectángulo = largo × ancho.",
                "El área se mide en unidades cuadradas, como cm²."
            ),
            guiaFoco = listOf("6 cm", "× 3 cm", "18 cm²", "🪟"),
            preguntas = listOf(
                p(
                    "Área de un rectángulo de 6 cm × 3 cm",
                    listOf("9 cm²", "18 cm²", "20 cm²", "36 cm²"),
                    1,
                    "6 × 3 = 18 cm²."
                ),
                p(
                    "Área de un cuadrado de 7 cm de lado",
                    listOf("14 cm²", "21 cm²", "28 cm²", "49 cm²"),
                    3,
                    "7 × 7 = 49 cm²."
                ),
                p(
                    "Un rectángulo tiene área 24 cm² y mide 4 cm de ancho. ¿Largo?",
                    listOf("4 cm", "6 cm", "8 cm", "20 cm"),
                    1,
                    "4 × 6 = 24, así que el largo es 6 cm."
                )
            )
        ),
        LessonNode(
            id = "mate_36",
            numero = 36,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Simetría de papel picado",
            subtitulo = "Dobla, recorta y encuentra el eje.",
            tier = 2,
            icono = "✂️",
            conceptos = listOf("simetría", "eje"),
            teoria = listOf(
                "Una figura es simétrica si al doblarla sus mitades coinciden.",
                "La línea del doblez es el eje de simetría."
            ),
            guiaFoco = listOf("🦋", "✂️", "↔️", "✨"),
            preguntas = listOf(
                p(
                    "¿Cuántos ejes de simetría tiene un cuadrado?",
                    listOf("1", "2", "4", "0"),
                    2,
                    "Puedes doblarlo en horizontal, vertical y por las dos diagonales: 4."
                ),
                p(
                    "Si doblas un corazón a la mitad, las dos partes son…",
                    listOf("Diferentes", "Simétricas", "Más grandes", "Rectas"),
                    1,
                    "Las dos mitades coinciden: son simétricas."
                ),
                p(
                    "¿Qué letra es simétrica de forma vertical?",
                    listOf("F", "G", "A", "P"),
                    2,
                    "La A tiene un eje vertical que la parte en dos mitades iguales."
                )
            )
        ),
        LessonNode(
            id = "mate_37",
            numero = 37,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Figuras compuestas",
            subtitulo = "Suma áreas de figuras que se juntan.",
            tier = 4,
            icono = "🧱",
            conceptos = listOf("área", "compuesta"),
            teoria = listOf(
                "Una figura compuesta se puede partir en rectángulos.",
                "Suma el área de cada parte; si hay un hueco, réstalo."
            ),
            guiaFoco = listOf("4×3", "➕", "2×3", "🧱"),
            preguntas = listOf(
                p(
                    "Un rectángulo de 4×3 y otro de 2×3 juntos. ¿Área total?",
                    listOf("12", "15", "18", "24"),
                    2,
                    "4×3 = 12 y 2×3 = 6; 12 + 6 = 18."
                ),
                p(
                    "Un cuadrado de 5×5 con un hueco de 2×2. ¿Área visible?",
                    listOf("21", "23", "25", "29"),
                    0,
                    "25 − 4 = 21."
                ),
                p(
                    "Dos triángulos iguales forman un cuadrado de 6×6. ¿Área de cada triángulo?",
                    listOf("12", "18", "24", "36"),
                    1,
                    "El cuadrado mide 36; cada triángulo es la mitad: 18."
                )
            )
        ),
        LessonNode(
            id = "mate_38",
            numero = 38,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "El reloj de Zazi",
            subtitulo = "Lee la hora y cuenta las horas.",
            tier = 2,
            icono = "🕖",
            conceptos = listOf("reloj", "horas"),
            teoria = listOf(
                "La manecilla corta marca la hora; la larga, los minutos.",
                "Una hora tiene 60 minutos."
            ),
            guiaFoco = listOf("🕖", "3:00", "4:00", "60 min"),
            preguntas = listOf(
                p(
                    "Si son las 3:00, ¿cuántos minutos faltan para las 4:00?",
                    listOf("30", "45", "60", "90"),
                    2,
                    "Una hora completa son 60 minutos."
                ),
                p(
                    "El reloj marca 7:30. ¿Qué hora es en palabras?",
                    listOf("Siete y cuarto", "Siete y media", "Ocho menos cuarto", "Ocho en punto"),
                    1,
                    "30 minutos son media hora: siete y media."
                ),
                p(
                    "¿Cuántas horas hay entre las 9:00 y las 12:00?",
                    listOf("2", "3", "4", "5"),
                    1,
                    "De 9 a 10, de 10 a 11 y de 11 a 12: 3 horas."
                )
            )
        ),
        LessonNode(
            id = "mate_39",
            numero = 39,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Cuánto dura el recreo",
            subtitulo = "Suma minutos a la hora de inicio.",
            tier = 3,
            icono = "⏱️",
            conceptos = listOf("minutos", "horas"),
            teoria = listOf(
                "Para sumar minutos, avanza desde la hora de inicio.",
                "Si pasas de 60 minutos, avanza una hora."
            ),
            guiaFoco = listOf("10:15", "➕ 30 min", "10:45", "⏱️"),
            preguntas = listOf(
                p(
                    "El recreo empieza 10:15 y dura 30 min. ¿Termina?",
                    listOf("10:30", "10:45", "11:00", "11:15"),
                    1,
                    "15 + 30 = 45 minutos: termina a las 10:45."
                ),
                p(
                    "Una película empieza 6:40 y dura 50 min. ¿Termina?",
                    listOf("7:20", "7:30", "7:40", "8:00"),
                    1,
                    "40 + 50 = 90 minutos: una hora y media. Termina a las 7:30."
                ),
                p(
                    "Si son las 8:45 y faltan 15 min para salir, ¿a qué hora sales?",
                    listOf("8:50", "8:55", "9:00", "9:15"),
                    2,
                    "45 + 15 = 60 minutos, o sea una hora: sales a las 9:00."
                )
            )
        ),
        LessonNode(
            id = "mate_40",
            numero = 40,
            capitulo = CapituloMate.GEOMETRIA_MEDICION,
            titulo = "Centímetros, metros y kilos",
            subtitulo = "Elige la unidad correcta para medir.",
            tier = 3,
            icono = "📏",
            conceptos = listOf("centímetros", "metros", "kilos"),
            teoria = listOf(
                "1 metro tiene 100 centímetros.",
                "Los kilos miden masa; los litros, líquidos."
            ),
            guiaFoco = listOf("1 m", "🟰", "100 cm", "📏"),
            preguntas = listOf(
                p(
                    "¿Qué unidad usarías para medir un lápiz?",
                    listOf("Metros", "Centímetros", "Kilos", "Litros"),
                    1,
                    "Un lápiz es pequeñito: centímetros."
                ),
                p(
                    "¿Cuántos centímetros hay en 2 metros?",
                    listOf("20", "100", "200", "2000"),
                    2,
                    "2 × 100 = 200 centímetros."
                ),
                p(
                    "¿Qué pesa más: 1 kg de plumas o 1 kg de hierro?",
                    listOf("Las plumas", "El hierro", "Pesan igual", "No se puede saber"),
                    2,
                    "Los dos pesan 1 kilogramo: pesan igual."
                )
            )
        ),

        // ===================================================================
        //  CAPÍTULO 5 · ESTADÍSTICA Y LÓGICA
        // ===================================================================
        LessonNode(
            id = "mate_41",
            numero = 41,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Pictogramas del tianguis",
            subtitulo = "Cada dibujo vale una cantidad.",
            tier = 2,
            icono = "🖼️",
            conceptos = listOf("pictograma", "clave"),
            teoria = listOf(
                "En un pictograma, un dibujo representa una cantidad.",
                "La clave dice cuánto vale cada dibujo."
            ),
            guiaFoco = listOf("🍎", "🟰 2 frutas", "🍎🍎🍎", "🖼️"),
            preguntas = listOf(
                p(
                    "Un pictograma muestra 1 manzana = 2 frutas. Si hay 3 manzanas, ¿cuántas frutas?",
                    listOf("3", "5", "6", "8"),
                    2,
                    "3 × 2 = 6 frutas."
                ),
                p(
                    "1 sol = 5 visitas. Hay 4 soles. ¿Cuántas visitas?",
                    listOf("9", "15", "20", "25"),
                    2,
                    "4 × 5 = 20 visitas."
                ),
                p(
                    "1 taco = 10 ventas. Hay 2 tacos y medio. ¿Cuántas ventas?",
                    listOf("15", "20", "25", "30"),
                    2,
                    "2.5 × 10 = 25 ventas."
                )
            )
        ),
        LessonNode(
            id = "mate_42",
            numero = 42,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Tablas de conteo",
            subtitulo = "Organiza datos en filas y columnas.",
            tier = 2,
            icono = "🗂️",
            conceptos = listOf("conteo", "tabla"),
            teoria = listOf(
                "Una tabla de conteo organiza cuántas veces aparece algo.",
                "Sumar los datos te da el total."
            ),
            guiaFoco = listOf("Lun 4", "Mar 6", "Mié 5", "🗂️"),
            preguntas = listOf(
                p(
                    "Conteo: lunes 4, martes 6, miércoles 5. ¿Total?",
                    listOf("13", "14", "15", "16"),
                    2,
                    "4 + 6 + 5 = 15."
                ),
                p(
                    "¿Qué día tuvo más? (lunes 4, martes 6, miércoles 5)",
                    listOf("Lunes", "Martes", "Miércoles", "Empate"),
                    1,
                    "6 es mayor que 4 y 5: el martes."
                ),
                p(
                    "Si el jueves hubo 7 y el total de lunes a jueves es 22, ¿cuántos hubo lunes a miércoles?",
                    listOf("13", "14", "15", "22"),
                    2,
                    "22 − 7 = 15."
                )
            )
        ),
        LessonNode(
            id = "mate_43",
            numero = 43,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Gráficas de barras",
            subtitulo = "Compara datos a simple vista.",
            tier = 3,
            icono = "📊",
            conceptos = listOf("gráfica", "barra"),
            teoria = listOf(
                "La altura de cada barra muestra la cantidad.",
                "Para comparar, mira qué barra es más alta."
            ),
            guiaFoco = listOf("🌮 ▮▮▮▮▮▮", "🫔 ▮▮▮▮", "📊", "2"),
            preguntas = listOf(
                p(
                    "La barra de tacos mide 6 y la de tamales 4. ¿Cuántos más tacos?",
                    listOf("2", "4", "6", "10"),
                    0,
                    "6 − 4 = 2."
                ),
                p(
                    "Si la barra de 5 se duplica, ¿cuánto mide?",
                    listOf("5", "10", "15", "25"),
                    1,
                    "5 × 2 = 10."
                ),
                p(
                    "La barra más alta mide 9 y la más baja 3. ¿Diferencia?",
                    listOf("3", "6", "9", "12"),
                    1,
                    "9 − 3 = 6."
                )
            )
        ),
        LessonNode(
            id = "mate_44",
            numero = 44,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "El promedio del equipo",
            subtitulo = "Reparte los puntos en partes iguales.",
            tier = 4,
            icono = "🏅",
            conceptos = listOf("promedio", "equipo"),
            teoria = listOf(
                "El promedio se obtiene sumando los datos y dividiendo entre cuántos son.",
                "Sirve para comparar equipos de forma justa."
            ),
            guiaFoco = listOf("6", "8", "10", "🟰 8"),
            preguntas = listOf(
                p(
                    "Puntajes 6, 8 y 10. ¿Promedio?",
                    listOf("6", "7", "8", "9"),
                    2,
                    "6 + 8 + 10 = 24; 24 ÷ 3 = 8."
                ),
                p(
                    "Puntajes 5, 5 y 8. ¿Promedio?",
                    listOf("5", "6", "7", "8"),
                    1,
                    "5 + 5 + 8 = 18; 18 ÷ 3 = 6."
                ),
                p(
                    "Tres equipos: 10, 20 y 30 puntos. ¿Promedio?",
                    listOf("15", "20", "25", "30"),
                    1,
                    "10 + 20 + 30 = 60; 60 ÷ 3 = 20."
                )
            )
        ),
        LessonNode(
            id = "mate_45",
            numero = 45,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Seguro, posible, imposible",
            subtitulo = "Habla de lo que puede pasar.",
            tier = 1,
            icono = "🎲",
            conceptos = listOf("seguro", "posible", "imposible"),
            teoria = listOf(
                "Seguro es que siempre pasa; imposible, que nunca pasa.",
                "Posible es que puede pasar, pero no siempre."
            ),
            guiaFoco = listOf("🌞", "🎲", "🚫", "🎲"),
            preguntas = listOf(
                p(
                    "Sacar un número del 1 al 6 con un dado es…",
                    listOf("Imposible", "Posible", "Seguro", "Poco probable"),
                    1,
                    "Puede salir cualquiera del 1 al 6: es posible."
                ),
                p(
                    "Que salga el sol mañana es…",
                    listOf("Imposible", "Posible", "Seguro", "Nunca"),
                    2,
                    "Todos los días sale el sol: es seguro."
                ),
                p(
                    "Sacar un 7 con un dado de 6 caras es…",
                    listOf("Seguro", "Posible", "Imposible", "Probable"),
                    2,
                    "El dado solo llega al 6: sacar 7 es imposible."
                )
            )
        ),
        LessonNode(
            id = "mate_46",
            numero = 46,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Dados y monedas",
            subtitulo = "Cuenta los resultados posibles.",
            tier = 3,
            icono = "🪙",
            conceptos = listOf("probable", "moneda", "dado"),
            teoria = listOf(
                "Un dado de 6 caras tiene 6 resultados posibles.",
                "Una moneda tiene 2 resultados: cara o cruz."
            ),
            guiaFoco = listOf("🎲", "1-6", "🪙", "cara/cruz"),
            preguntas = listOf(
                p(
                    "¿Cuántos resultados posibles tiene un dado de 6 caras?",
                    listOf("2", "4", "6", "12"),
                    2,
                    "Los resultados son 1, 2, 3, 4, 5 y 6: seis."
                ),
                p(
                    "¿Qué es más probable al lanzar una moneda?",
                    listOf("Cara", "Cruz", "Las dos igual", "Ninguna"),
                    2,
                    "Cara y cruz tienen la misma probabilidad."
                ),
                p(
                    "Sacas un 5 en un dado. ¿Cuántos resultados son menores que 5?",
                    listOf("3", "4", "5", "6"),
                    1,
                    "Menores que 5: 1, 2, 3 y 4. Son cuatro."
                )
            )
        ),
        LessonNode(
            id = "mate_47",
            numero = 47,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Patrones que saltan",
            subtitulo = "Descubre la regla y sigue la serie.",
            tier = 2,
            icono = "🦘",
            conceptos = listOf("patrón", "saltos"),
            teoria = listOf(
                "Un patrón numérico sigue una regla que se repite.",
                "Observa cuánto cambia de un número al siguiente."
            ),
            guiaFoco = listOf("2", "4", "6", "➕2"),
            preguntas = listOf(
                p(
                    "2, 4, 6, 8, ___",
                    listOf("9", "10", "12", "14"),
                    1,
                    "La regla es sumar 2: después de 8 viene 10."
                ),
                p(
                    "3, 6, 9, 12, ___",
                    listOf("13", "14", "15", "18"),
                    2,
                    "La regla es sumar 3: 12 + 3 = 15."
                ),
                p(
                    "1, 4, 7, 10, ___",
                    listOf("11", "12", "13", "14"),
                    2,
                    "La regla es sumar 3: 10 + 3 = 13."
                )
            )
        ),
        LessonNode(
            id = "mate_48",
            numero = 48,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Patrones de figuras",
            subtitulo = "Las formas también bailan en orden.",
            tier = 3,
            icono = "🔁",
            conceptos = listOf("patrón", "figura"),
            teoria = listOf(
                "Los patrones también pueden ser de figuras o colores.",
                "Encuentra el bloque que se repite y continúa."
            ),
            guiaFoco = listOf("🔺🔺⬛", "🔺🔺⬛", "🔁", "🔺"),
            preguntas = listOf(
                p(
                    "Círculo, cuadrado, círculo, cuadrado, ___",
                    listOf("Círculo", "Cuadrado", "Triángulo", "Rectángulo"),
                    0,
                    "El patrón alterna: después de cuadrado toca círculo."
                ),
                p(
                    "🔺, 🔺, ⬛, 🔺, 🔺, ⬛, ___",
                    listOf("🔺", "⬛", "🔵", "⭐"),
                    0,
                    "El bloque es 🔺🔺⬛: después del segundo bloque toca 🔺."
                ),
                p(
                    "1, 2, 4, 8, ___",
                    listOf("10", "12", "16", "18"),
                    2,
                    "Cada número es el doble del anterior: 8 × 2 = 16."
                )
            )
        ),
        LessonNode(
            id = "mate_49",
            numero = 49,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "Acertijos de lógica",
            subtitulo = "Piensa, descarta y encuentra la respuesta.",
            tier = 4,
            icono = "🕵️",
            conceptos = listOf("lógica", "mayor", "menor"),
            teoria = listOf(
                "La lógica usa pistas para descartar opciones.",
                "Lee todas las pistas antes de responder."
            ),
            guiaFoco = listOf("🟢", "🟣", "🟡", "🕵️"),
            preguntas = listOf(
                p(
                    "Zazi tiene 3 colores: verde, morado y amarillo. Si no es verde ni amarillo, ¿de qué color es?",
                    listOf("Verde", "Morado", "Amarillo", "Azul"),
                    1,
                    "Si descartas verde y amarillo, queda morado."
                ),
                p(
                    "Ana es mayor que Beto; Beto es mayor que Carla. ¿Quién es el menor?",
                    listOf("Ana", "Beto", "Carla", "No se sabe"),
                    2,
                    "Carla es menor que Beto, y Beto es menor que Ana: Carla es la menor."
                ),
                p(
                    "Todos los tacos tienen tortilla. Este plato tiene un taco. ¿Qué tiene?",
                    listOf("Tortilla", "Queso", "Salsa", "Nada"),
                    0,
                    "Si todos los tacos tienen tortilla, este taco también."
                )
            )
        ),
        LessonNode(
            id = "mate_50",
            numero = 50,
            capitulo = CapituloMate.ESTADISTICA_LOGICA,
            titulo = "El gran reto del Tianguis",
            subtitulo = "Todo lo aprendido en una sola compra.",
            tier = 5,
            icono = "🏆",
            conceptos = listOf("reto", "multiplicar", "cambio"),
            teoria = listOf(
                "Lee el problema en dos pasos: primero calcula, luego decide.",
                "Puedes usar multiplicación, resta y cambio en el mismo reto."
            ),
            guiaFoco = listOf("4×5", "➕3", "100−71", "🏆"),
            preguntas = listOf(
                p(
                    "Doña Chelo vende 4 bolsas con 5 elotes cada una. Luego vende 3 elotes más. ¿Cuántos vendió?",
                    listOf("15", "20", "23", "27"),
                    2,
                    "4 × 5 = 20, y 20 + 3 = 23 elotes."
                ),
                p(
                    "Tienes \$100. Compras 2 kg de jitomate a \$18 y un queso de \$35. ¿Cuánto queda?",
                    listOf("\$29", "\$31", "\$35", "\$47"),
                    0,
                    "2 × \$18 = \$36; \$36 + \$35 = \$71; \$100 − \$71 = \$29."
                ),
                p(
                    "Una caja tiene 3 filas de 4 galletas. Te comes 2. ¿Cuántas quedan?",
                    listOf("10", "12", "14", "6"),
                    0,
                    "3 × 4 = 12; 12 − 2 = 10."
                )
            )
        )
    )
}
