package com.example.zazili.domain.tienda

import androidx.compose.runtime.Immutable
import com.example.zazili.core.mascot.EfectosCuidado

/** Las cuatro pestañas de la tienda, en el orden en que se muestran. */
enum class PestanaTienda(val etiqueta: String, val emoji: String) {
    COMIDA("Comida", "🍎"),
    CUIDADO("Cuidado", "🛁"),
    ROPA("Ropa", "🎩"),
    CASITA("Mi Casita", "🏡")
}

/** Qué hace un artículo consumible cuando cae sobre Zazi. */
enum class TipoConsumible { COMIDA, HIGIENE, JUGUETE }

/** Ranuras del armario. Una prenda ocupa exactamente una. */
enum class RanuraRopa(val clave: String, val etiqueta: String, val emoji: String) {
    CABEZA("cabeza", "Gorros y cabezas", "👑"),
    CARA("cara", "Caras y lentes", "🕶️"),
    CUERPO("cuerpo", "Ropa y cuerpecito", "👕")
}

/**
 * Fondos procedurales de la casita. Los colores viven como ARGB para que el
 * dominio no dependa de Compose; [ZaziCompanionView] los convierte en
 * degradados y dibuja el motivo con Canvas.
 */
enum class EscenaFondo(
    val colorInicio: Long,
    val colorFin: Long,
    val colorSuelo: Long,
    val motivo: String,
    val conEstrellas: Boolean
) {
    UNIVERSO(0xFF2B0B3F, 0xFF4C1A66, 0xFF7B4B94, "✨", true),
    CHAPULTEPEC(0xFF9BD9C8, 0xFF5DBB8F, 0xFF3F8B5E, "🌳", false),
    RUINAS(0xFF26143F, 0xFF6B3FA0, 0xFFA8E6CF, "🔺", true),
    GALAXIA(0xFF120B2E, 0xFF3B1E6E, 0xFF7B4B94, "🌌", true),
    KAWAII(0xFFFFE3F1, 0xFFF7B8C6, 0xFFC9B0E0, "🎀", false),
    AURORA(0xFF132447, 0xFF2E6B6B, 0xFFA8E6CF, "🌠", true),
    CEMPASUCHIL(0xFF3A1A2E, 0xFFB55A2A, 0xFFFFC800, "🌼", true),
    PLAYA(0xFFAEE3F7, 0xFF6FD3F5, 0xFFF7E6B0, "🐚", false),
    VOLCAN(0xFF3B2430, 0xFF8A3A2E, 0xFFF08A6C, "🌋", false),
    BIBLIOTECA(0xFF3A2A1F, 0xFF70513A, 0xFFD9A400, "📚", false),
    CHINAMPA(0xFFBFE8D8, 0xFF7BC9A6, 0xFF58CC02, "🌺", false),
    ARCADE(0xFF1A1030, 0xFF3B1E6E, 0xFFFF2E88, "👾", true),
    ALEBRIJES(0xFF2B0B3F, 0xFF7B4B94, 0xFFFFC800, "🐉", true),
    ALGODON(0xFFFFF3FA, 0xFFC9E8FF, 0xFFF7B8C6, "☁️", false),
    NEBULOSA(0xFF1B0F35, 0xFF5B2A86, 0xFFAEE3F7, "💫", true),
    LLUVIA(0xFF141A3A, 0xFF2E3F7A, 0xFFAEE3F7, "🌠", true),
    CARAMELO(0xFFFFE0D6, 0xFFF7A8B8, 0xFFFFC800, "🍭", false),
    MARIPOSAS(0xFFE6F7D9, 0xFFA8E6CF, 0xFFF7B8C6, "🦋", false),
    MONTANA(0xFFB9DDF0, 0xFF7BA8C9, 0xFF4C1A66, "⛰️", false),    CRISTAL(0xFF14243F, 0xFF2E6B8A, 0xFFAEE3F7, "💎", true),
    ATARDECER(0xFFFFC28A, 0xFFF08A6C, 0xFF7B4B94, "🌅", false),
    ISLA(0xFFAEE3F7, 0xFF58CC02, 0xFFFFC800, "🏝️", false),
    RELOJ(0xFFFFE9A8, 0xFFD9A400, 0xFF7B4B94, "🕰️", false)
}

/** Un alimento, artículo de higiene o juguete que se arrastra hacia Zazi. */
@Immutable
data class ConsumibleTienda(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: Int,
    val emoji: String,
    val tipo: TipoConsumible,
    val grupo: String,
    val efectos: EfectosCuidado
)

/** Una prenda del armario. */
@Immutable
data class RopaTienda(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: Int,
    val emoji: String,
    val ranura: RanuraRopa
)

/** Un fondo para la casita de Zazi. */
@Immutable
data class FondoTienda(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: Int,
    val emoji: String,
    val escena: EscenaFondo
)

/** Bloque visual de la lista: título, emoji y artículos. */
@Immutable
data class GrupoConsumibles(
    val titulo: String,
    val emoji: String,
    val items: List<ConsumibleTienda>
)

/**
 * Catálogo completo de Tienditas 3B.
 *
 * Los precios respetan las bandas de diseño: nutrición 5–15, antojitos 20–40,
 * golosinas mágicas 50–80, higiene y juguetes 30–100, ropa 120–320 y fondos
 * desde 200 Zazoles. Las golosinas mágicas reparan las cuatro vitales.
 *
 * Todo es inmutable y sin recursos externos: nombre, emoji y efectos bastan
 * para que la tienda funcione aunque el dispositivo no tenga conexión.
 */
object CatalogoTienditas3B {

    // -----------------------------------------------------------------------
    //  COMIDA · Nutrición Básica (5–15 Zazoles)
    // -----------------------------------------------------------------------

    private val nutricionBasica = listOf(
        ConsumibleTienda(
            id = "agua_limon_chia",
            nombre = "Agua de Limón con Chía",
            descripcion = "Fresquita y ligera. Ideal para empezar el día.",
            precio = 5,
            emoji = "🍋",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 12, felicidad = 3, higiene = 1)
        ),
        ConsumibleTienda(
            id = "semillas_girasol",
            nombre = "Semillas de Girasol",
            descripcion = "Crujientitas, como confeti que se puede comer.",
            precio = 6,
            emoji = "🌻",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 13, felicidad = 4)
        ),
        ConsumibleTienda(
            id = "nectar_orquidea",
            nombre = "Néctar de Orquídea",
            descripcion = "El jugo favorito de las colibríes elegantes.",
            precio = 8,
            emoji = "🌸",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 14, felicidad = 5, energia = 2)
        ),
        ConsumibleTienda(
            id = "trocitos_mango",
            nombre = "Trocitos de Mango",
            descripcion = "Dulces, jugosos y con un color de sol.",
            precio = 9,
            emoji = "🥭",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 16, felicidad = 5)
        ),
        ConsumibleTienda(
            id = "tortilla_maiz",
            nombre = "Tortilla de Maíz",
            descripcion = "Recién hecha, con su olorcito a abuela.",
            precio = 5,
            emoji = "🌽",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 14, felicidad = 3)
        ),
        ConsumibleTienda(
            id = "frijolitos_charros",
            nombre = "Frijolitos Charros",
            descripcion = "Una cucharita de energía mexicana.",
            precio = 7,
            emoji = "🫘",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 17, felicidad = 3, energia = 1)
        ),
        ConsumibleTienda(
            id = "calabacitas_vapor",
            nombre = "Calabacitas al Vapor",
            descripcion = "Verdes y suavecitas, con un toque de elote.",
            precio = 8,
            emoji = "🥒",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 15, felicidad = 3, higiene = 1)
        ),
        ConsumibleTienda(
            id = "platano_dominico",
            nombre = "Plátano Dominico",
            descripcion = "Curvado como una sonrisa amarilla.",
            precio = 6,
            emoji = "🍌",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 14, energia = 2)
        ),
        ConsumibleTienda(
            id = "avena_canela",
            nombre = "Avena con Canela",
            descripcion = "Calientita en su tazón de estrellas.",
            precio = 9,
            emoji = "🥣",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 18, felicidad = 4)
        ),
        ConsumibleTienda(
            id = "huevito_cocido",
            nombre = "Huevito Cocido",
            descripcion = "Pequeñito, tibio y lleno de fuerza.",
            precio = 10,
            emoji = "🥚",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 19, energia = 3)
        ),
        ConsumibleTienda(
            id = "queso_fresco",
            nombre = "Queso Fresco en Cubitos",
            descripcion = "Blanco y saludadito; combina con todo.",
            precio = 11,
            emoji = "🧀",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 20, felicidad = 4)
        ),
        ConsumibleTienda(
            id = "sandia_fresca",
            nombre = "Sandía Fresca",
            descripcion = "Una rebanada de verano en forma de luna.",
            precio = 7,
            emoji = "🍉",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 14, felicidad = 4, higiene = 1)
        ),
        ConsumibleTienda(
            id = "nopalitos_limon",
            nombre = "Nopalitos con Limón",
            descripcion = "Verdes, frescos y muy mexicanos.",
            precio = 8,
            emoji = "🌵",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 15, felicidad = 2)
        ),
        ConsumibleTienda(
            id = "sopa_verduras",
            nombre = "Sopa de Verduras",
            descripcion = "Con fideos pequeñitos que parecen letras.",
            precio = 12,
            emoji = "🥕",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 22, felicidad = 4, higiene = 2)
        ),
        ConsumibleTienda(
            id = "arroz_con_leche",
            nombre = "Arroz con Leche",
            descripcion = "Cremoso, con canela y un suspiro de vainilla.",
            precio = 13,
            emoji = "🥛",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 21, felicidad = 7)
        ),
        ConsumibleTienda(
            id = "tlacoyito_queso",
            nombre = "Tlacoyito de Queso",
            descripcion = "Pequeñito, calientito y con salsita verde.",
            precio = 15,
            emoji = "🫓",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 24, felicidad = 6)
        ),
        ConsumibleTienda(
            id = "pera_jugosa",
            nombre = "Pera Jugosa",
            descripcion = "Un bocado dulce que chorretea felicidad.",
            precio = 8,
            emoji = "🍐",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 14, felicidad = 4, higiene = 1)
        ),
        ConsumibleTienda(
            id = "zanahoria_baby",
            nombre = "Zanahorias Baby",
            descripcion = "Crujientes como palitos de duende.",
            precio = 6,
            emoji = "🥕",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 12, felicidad = 2)
        ),
        ConsumibleTienda(
            id = "tamalito_dulce",
            nombre = "Tamalito Dulce",
            descripcion = "Envuelto en hoja, con pasitas escondidas.",
            precio = 14,
            emoji = "🫔",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 23, felicidad = 7)
        ),
        ConsumibleTienda(
            id = "atole_avena",
            nombre = "Atole de Avena",
            descripcion = "Un abrazo tibio en vasito de barro.",
            precio = 12,
            emoji = "🍵",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_NUTRICION,
            efectos = EfectosCuidado(pancita = 18, felicidad = 6, energia = 2)
        )
    )

    // -----------------------------------------------------------------------
    //  COMIDA · Antojitos Mexicanos y Latinos (20–40 Zazoles)
    // -----------------------------------------------------------------------

    private val antojitos = listOf(
        ConsumibleTienda(
            id = "mini_taco_pastor",
            nombre = "Mini-Taco al Pastor",
            descripcion = "Con piñita, cebollita y salsa de la feliz.",
            precio = 26,
            emoji = "🌮",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 28, felicidad = 11, higiene = -2)
        ),
        ConsumibleTienda(
            id = "esquite_espacial",
            nombre = "Esquite Espacial",
            descripcion = "Elote flotante con mayonesa de nebulosa.",
            precio = 24,
            emoji = "🌽",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 24, felicidad = 10, higiene = -3)
        ),
        ConsumibleTienda(
            id = "alegria_amaranto",
            nombre = "Alegría de Amaranto",
            descripcion = "Se deshace en la boca y en el corazón.",
            precio = 22,
            emoji = "🍯",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 20, felicidad = 12)
        ),
        ConsumibleTienda(
            id = "churro_cajeta",
            nombre = "Churro con Cajeta",
            descripcion = "Crujiente por fuera, dulce por dentro.",
            precio = 28,
            emoji = "🥖",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 25, felicidad = 13, higiene = -2)
        ),
        ConsumibleTienda(
            id = "arepita_queso",
            nombre = "Arepita de Queso",
            descripcion = "Redondita, dorada y con queso que se estira.",
            precio = 30,
            emoji = "🫓",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 29, felicidad = 10)
        ),
        ConsumibleTienda(
            id = "tlacoyo_chicharron",
            nombre = "Tlacoyo con Chicharrón",
            descripcion = "Con nopales y salsa que hace cosquillas.",
            precio = 32,
            emoji = "🌶️",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 30, felicidad = 12, higiene = -2)
        ),
        ConsumibleTienda(
            id = "elote_asadito",
            nombre = "Elote Asadito",
            descripcion = "Con limón, chile y un palito para sostenerlo.",
            precio = 27,
            emoji = "🌽",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 26, felicidad = 11, higiene = -3)
        ),
        ConsumibleTienda(
            id = "quesadilla_flor_calabaza",
            nombre = "Quesadilla de Flor de Calabaza",
            descripcion = "Amarillita, como una tarde de campo.",
            precio = 34,
            emoji = "🌼",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 31, felicidad = 12)
        ),
        ConsumibleTienda(
            id = "tostada_tinga",
            nombre = "Tostada de Tinga",
            descripcion = "Crujiente, rojita y con aguacate encima.",
            precio = 31,
            emoji = "🥑",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 29, felicidad = 12, higiene = -2)
        ),
        ConsumibleTienda(
            id = "paleta_mango_chamoy",
            nombre = "Paleta de Mango con Chamoy",
            descripcion = "Dulce, ácida y con polvito rojo de emoción.",
            precio = 23,
            emoji = "🍭",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 18, felicidad = 13, higiene = -4)
        ),
        ConsumibleTienda(
            id = "bolis_limon",
            nombre = "Bolis de Limón",
            descripcion = "Frío, agrio y perfecto para día de sol.",
            precio = 20,
            emoji = "🧊",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 16, felicidad = 12, higiene = -3)
        ),
        ConsumibleTienda(
            id = "pan_dulce_concha",
            nombre = "Pan Dulce Concha",
            descripcion = "Con su costra de azúcar en forma de ola.",
            precio = 25,
            emoji = "🥐",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 22, felicidad = 14, higiene = -2)
        ),
        ConsumibleTienda(
            id = "flautitas_pollo",
            nombre = "Flautitas de Pollo",
            descripcion = "Dos tubitos dorados con guacamole.",
            precio = 29,
            emoji = "🌯",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 28, felicidad = 11, higiene = -2)
        ),
        ConsumibleTienda(
            id = "sopes_con_papas",
            nombre = "Sopes con Papitas",
            descripcion = "Con col, crema y salsa verde al gusto.",
            precio = 33,
            emoji = "🫓",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 30, felicidad = 12, higiene = -2)
        ),
        ConsumibleTienda(
            id = "gelatina_arcoiris",
            nombre = "Gelatina Arcoíris",
            descripcion = "Tiembla de colores y se ríe con Zazi.",
            precio = 21,
            emoji = "🌈",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 17, felicidad = 13)
        ),
        ConsumibleTienda(
            id = "bunuelo_piloncillo",
            nombre = "Buñuelo de Piloncillo",
            descripcion = "Doradito, con azúcar y un crujido de fiesta.",
            precio = 36,
            emoji = "🍩",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 27, felicidad = 15, higiene = -3)
        ),
        ConsumibleTienda(
            id = "empanada_platanito",
            nombre = "Empanada de Platanito",
            descripcion = "Con relleno de cajeta que se escapa despacito.",
            precio = 30,
            emoji = "🥟",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 26, felicidad = 13)
        ),
        ConsumibleTienda(
            id = "martes_tamales",
            nombre = "Martes de Tamales",
            descripcion = "Tamal verde con su atole de la esquina.",
            precio = 38,
            emoji = "🫔",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 34, felicidad = 14, energia = 3)
        ),
        ConsumibleTienda(
            id = "pozole_pasajero",
            nombre = "Pozole Pasajero",
            descripcion = "Un plato lleno de orégano, rábanos y cuentos.",
            precio = 40,
            emoji = "🍲",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 36, felicidad = 15, energia = 4)
        ),
        ConsumibleTienda(
            id = "mole_magico",
            nombre = "Mole Mágico",
            descripcion = "Con tantos ingredientes que parece hechizo.",
            precio = 39,
            emoji = "🍛",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 34, felicidad = 16)
        ),
        ConsumibleTienda(
            id = "torta_aguacate",
            nombre = "Torta de Aguacate",
            descripcion = "Grandota, verde y con muchas historias.",
            precio = 35,
            emoji = "🥪",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 32, felicidad = 10)
        ),
        ConsumibleTienda(
            id = "cocada_coco",
            nombre = "Cocada de Coco",
            descripcion = "Blanca como nube y dulce como domingo.",
            precio = 24,
            emoji = "🥥",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 19, felicidad = 12)
        ),
        ConsumibleTienda(
            id = "churros_guayaba",
            nombre = "Churros de Guayaba",
            descripcion = "Rellenos de una mermeladita rosita.",
            precio = 28,
            emoji = "🍢",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 25, felicidad = 14, higiene = -2)
        ),
        ConsumibleTienda(
            id = "fresas_con_crema",
            nombre = "Fresas con Crema",
            descripcion = "Un postre rojo con bigote de nube.",
            precio = 37,
            emoji = "🍓",
            tipo = TipoConsumible.COMIDA,
            grupo = GRUPO_ANTOJITOS,
            efectos = EfectosCuidado(pancita = 26, felicidad = 16, higiene = -1)
        )
    )

    // -----------------------------------------------------------------------
    //  COMIDA · Golosinas Mágicas (50–80 Zazoles, reparan las cuatro vitales)
    // -----------------------------------------------------------------------

    private fun golosina(
        id: String,
        nombre: String,
        descripcion: String,
        precio: Int,
        emoji: String,
        potencia: Int = 25
    ) = ConsumibleTienda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        emoji = emoji,
        tipo = TipoConsumible.COMIDA,
        grupo = GRUPO_GOLOSINAS,
        efectos = EfectosCuidado(
            pancita = potencia,
            felicidad = potencia,
            energia = potencia,
            higiene = potencia
        )
    )

    private val golosinas = listOf(
        golosina(
            "gomita_ajolote_neon",
            "Gomita de Ajolote Neón",
            "Brilla en la oscuridad y sabe a sandía radiante.",
            55,
            "🦎",
            28
        ),
        golosina(
            "helado_cuantico_vainilla",
            "Helado Cuántico de Vainilla",
            "Está frío y caliente al mismo tiempo. No preguntes.",
            60,
            "🍨",
            30
        ),
        golosina(
            "pocion_concentracion_mora",
            "Poción de Concentración (Sabor Mora)",
            "Burbujas moradas que ordenan las ideas.",
            65,
            "🧪",
            30
        ),
        golosina(
            "dona_estelar",
            "Dona Estelar",
            "Con chispitas que titilan como constelaciones.",
            52,
            "🍩",
            26
        ),
        golosina(
            "chocolate_abuela",
            "Chocolate de la Abuela",
            "Receta secreta con canela y mucho cariño.",
            58,
            "🍫",
            28
        ),
        golosina(
            "piruleta_arcoiris",
            "Piruleta Arcoíris",
            "Cada capa sabe a un color distinto.",
            54,
            "🍭",
            27
        ),
        golosina(
            "malteada_nebulosa",
            "Malteada Nebulosa",
            "Espesa, morada y con estrellitas de azúcar.",
            70,
            "🥤",
            30
        ),
        golosina(
            "pastelito_luna",
            "Pastelito de Luna",
            "Relleno de crema de luna llena.",
            75,
            "🌙",
            32
        ),
        golosina(
            "pan_de_muerto_glaseado",
            "Pan de Muerto Glaseado",
            "Con azúcar y ajonjolí; se comparte con cariño.",
            57,
            "🍞",
            28
        ),
        golosina(
            "miel_de_hada",
            "Miel de Hada",
            "Una gotita endulza todo el día.",
            50,
            "🍯",
            25
        ),
        golosina(
            "galleta_suerte",
            "Galleta de la Suerte",
            "Adentro trae un mensaje que siempre dice 'tú puedes'.",
            62,
            "🥠",
            28
        ),
        golosina(
            "crepa_espacial",
            "Crepa Espacial",
            "Rellena de nutella de otro planeta.",
            68,
            "🥞",
            30
        ),
        golosina(
            "algodon_nube",
            "Algodón de Nube",
            "Se deshace como una nube rosita.",
            56,
            "☁️",
            27
        ),
        golosina(
            "mazapan_estrella",
            "Mazapán Estrella",
            "Se desmorona y hace cosquillas en los dedos.",
            51,
            "⭐",
            25
        ),
        golosina(
            "beso_merengue",
            "Beso de Merengue",
            "Blanco, crujiente y con sabor a abrazo.",
            53,
            "💋",
            26
        ),
        golosina(
            "flan_brillante",
            "Flan Brillante",
            "Con caramelo que refleja luces de colores.",
            66,
            "🍮",
            30
        ),
        golosina(
            "bombon_cosmos",
            "Bombón Cósmico",
            "El dulce más raro de la galaxia. Cura todo.",
            80,
            "🌌",
            35
        ),
        golosina(
            "gomitas_monstruo",
            "Gomitas de Monstruito",
            "Muerden suavecito y luego se dejan comer.",
            59,
            "👾",
            28
        ),
        golosina(
            "chicle_galaxia",
            "Chicle Galaxia",
            "Hace burbujas llenas de puntitos brillantes.",
            54,
            "🫧",
            26
        ),
        golosina(
            "nieve_luciernaga",
            "Nieve de Luciérnaga",
            "Se ilumina mientras se derrite despacito.",
            60,
            "✨",
            29
        )
    )

    // -----------------------------------------------------------------------
    //  CUIDADO · Higiene (30–55 Zazoles)
    // -----------------------------------------------------------------------

    private fun higiene(
        id: String,
        nombre: String,
        descripcion: String,
        precio: Int,
        emoji: String,
        limpieza: Int,
        felicidad: Int = 6
    ) = ConsumibleTienda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        emoji = emoji,
        tipo = TipoConsumible.HIGIENE,
        grupo = GRUPO_HIGIENE,
        efectos = EfectosCuidado(higiene = limpieza, felicidad = felicidad)
    )

    private val articulosHigiene = listOf(
        higiene(
            "jabonsito_avena",
            "Jabonsito de Avena",
            "Hace espuma tibia y huele a campo.",
            30,
            "🧼",
            28,
            6
        ),
        higiene(
            "cepillo_dientes_menta",
            "Cepillo de Dientes de Menta",
            "Con cerditas de colores y pasta chiclosa.",
            32,
            "🪥",
            25,
            7
        ),
        higiene(
            "pasta_fresa",
            "Pasta de Fresa",
            "Rosa, dulce y con brillitos comestibles.",
            34,
            "🍓",
            22,
            8
        ),
        higiene(
            "toalla_seca_rapido",
            "Toalla Seca-Rápido",
            "Suavecita como nube y súper absorbente.",
            36,
            "🧻",
            26,
            6
        ),
        higiene(
            "bano_burbujas_lavanda",
            "Baño de Burbujas de Lavanda",
            "Una tina llena de burbujas que huelen a calma.",
            55,
            "🛁",
            55,
            14
        ),
        higiene(
            "esponja_nube",
            "Esponja de Nube",
            "Blandita, esponjosa y con forma de cúmulo.",
            45,
            "☁️",
            34,
            9
        ),
        higiene(
            "champu_bebe",
            "Champú de Bebé",
            "No pica en los ojos y huele a manzanilla.",
            38,
            "🧴",
            30,
            8
        ),
        higiene(
            "cepillo_pelo_unicornio",
            "Cepillo de Pelo Unicornio",
            "Desenreda sin jalones y deja brillo mágico.",
            40,
            "🦄",
            22,
            10
        ),
        higiene(
            "perfumito_vainilla",
            "Perfumito de Vainilla",
            "Dos rociaditas y Zazi huele a postre.",
            48,
            "🌸",
            24,
            12
        ),
        higiene(
            "cortaugas_kawaii",
            "Cortauñas Kawaii",
            "Con carita feliz para que no dé miedo.",
            35,
            "💅",
            18,
            7
        )
    )

    // -----------------------------------------------------------------------
    //  CUIDADO · Juguetes (33–100 Zazoles)
    // -----------------------------------------------------------------------

    private fun juguete(
        id: String,
        nombre: String,
        descripcion: String,
        precio: Int,
        emoji: String,
        felicidad: Int,
        energia: Int = -2
    ) = ConsumibleTienda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        emoji = emoji,
        tipo = TipoConsumible.JUGUETE,
        grupo = GRUPO_JUGUETES,
        efectos = EfectosCuidado(felicidad = felicidad, energia = energia)
    )

    private val juguetes = listOf(
        juguete(
            "pelota_estambre_galactica",
            "Pelota de Estambre Galáctica",
            "Rebota y deja un hilito de estrellas.",
            70,
            "🧶",
            26,
            -3
        ),
        juguete(
            "sonaja_maracas",
            "Sonaja de Maracas",
            "¡Chiqui-chiqui! Zazi baila en cuanto la oye.",
            60,
            "🪇",
            20,
            -2
        ),
        juguete(
            "mini_pinata",
            "Mini-Piñata Interactiva",
            "Llena de confeti y de sorpresas pequeñitas.",
            80,
            "🪅",
            30,
            -4
        ),
        juguete(
            "balero_magico",
            "Balero Mágico",
            "Casi siempre cae, pero siempre da risa.",
            50,
            "🪀",
            18,
            -2
        ),
        juguete(
            "trompo_arcoiris",
            "Trompo Arcoíris",
            "Gira y pinta círculos de colores en el aire.",
            55,
            "🌈",
            22,
            -2
        ),
        juguete(
            "yoyo_luminoso",
            "Yoyó Luminoso",
            "Sube y baja dejando una línea de luz.",
            45,
            "🪩",
            19,
            -2
        ),
        juguete(
            "osito_de_peluche",
            "Osito de Peluche",
            "Suavecito para abrazar cuando hay nubes grises.",
            90,
            "🧸",
            34,
            -1
        ),
        juguete(
            "globos_de_colores",
            "Globos de Colores",
            "Flotan y hacen fiesta aunque sea martes.",
            40,
            "🎈",
            17,
            -2
        ),
        juguete(
            "cometa_estrellas",
            "Cometa de Estrellas",
            "Vuela alto y dibuja ochos en el cielo.",
            65,
            "🪁",
            24,
            -3
        ),
        juguete(
            "carrusel_musical",
            "Carrusel Musical",
            "Da vueltas despacito con una melodía de cuna.",
            100,
            "🎠",
            36,
            -1
        ),
        juguete(
            "cubos_apilables",
            "Cubos Apilables",
            "Torres que se caen y se vuelven a armar.",
            58,
            "🧱",
            21,
            -2
        ),
        juguete(
            "matraca_sonriente",
            "Matraca Sonriente",
            "Hace clac-clac y tiene una carita feliz.",
            33,
            "🎵",
            15,
            -1
        ),
        juguete(
            "peluche_ajolote",
            "Peluche de Ajolote",
            "Morado, blandito y con branquias de felpa.",
            95,
            "🦎",
            35,
            -1
        ),
        juguete(
            "rompecabezas_luna",
            "Rompecabezas de Luna",
            "Veinte piezas que forman una luna sonriente.",
            77,
            "🌙",
            27,
            -2
        )
    )

    // -----------------------------------------------------------------------
    //  ROPA · Gorros y cabezas (120–320 Zazoles)
    // -----------------------------------------------------------------------

    private fun cabeza(
        id: String,
        nombre: String,
        descripcion: String,
        precio: Int,
        emoji: String
    ) = RopaTienda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        emoji = emoji,
        ranura = RanuraRopa.CABEZA
    )

    private val gorros = listOf(
        cabeza(
            "sombrero_vueltiao",
            "Sombrero Vueltiao (Colombia)",
            "Tejido de caña flecha con cintas de colores.",
            250,
            "👒"
        ),
        cabeza(
            "chullo_peru",
            "Chullo (Perú)",
            "Con orejeras y borlas que bailan al volar.",
            220,
            "🧶"
        ),
        cabeza(
            "gorro_ajolote",
            "Gorro de Ajolote",
            "Con branquias de felpa morada.",
            180,
            "🦎"
        ),
        cabeza(
            "casco_daft_punk",
            "Casco de Daft Punk",
            "Plateado y brillante, con visor de estrellas.",
            300,
            "🪖"
        ),
        cabeza(
            "corona_cempasuchil",
            "Corona de Cempasúchil",
            "Pétalos anaranjados que huelen a fiesta.",
            240,
            "🌼"
        ),
        cabeza(
            "gorro_dormir_pompom",
            "Gorro de Dormir con Pompones",
            "Largo, suavecito y perfecto para las siestas.",
            150,
            "🧢"
        ),
        cabeza(
            "sombrero_mago_8bits",
            "Sombrero de Mago de 8-bits",
            "Puntiagudo, con estrellita pixelada.",
            200,
            "🧙"
        ),
        cabeza(
            "corona_estrellas",
            "Corona de Estrellas",
            "Titila cuando Zazi está feliz.",
            260,
            "👑"
        ),
        cabeza(
            "diadema_margaritas",
            "Diadema de Margaritas",
            "Florecitas blancas con centro amarillo.",
            120,
            "🌼"
        ),
        cabeza(
            "gorro_santa",
            "Gorro de Santa",
            "Rojo con pompón blanco y espíritu de diciembre.",
            140,
            "🎅"
        ),
        cabeza(
            "beret_artista",
            "Beret de Artista",
            "Para pintar sueños con acuarela.",
            160,
            "🎨"
        ),
        cabeza(
            "cucurucho_helado",
            "Gorro de Cucurucho de Helado",
            "Con bolita de nieve de fresa encima.",
            190,
            "🍦"
        ),
        cabeza(
            "antenas_marciano",
            "Antenas de Marciano",
            "Con bolitas que se iluminan al pensar.",
            170,
            "👽"
        ),
        cabeza(
            "sombrero_charro",
            "Sombrero Charro",
            "Ala ancha, barbiquejo y mucho orgullo.",
            280,
            "🤠"
        ),
        cabeza(
            "cachitos_diablito",
            "Cachitos de Diablito",
            "Rojos y pequeñitos; solo para jugar.",
            130,
            "😈"
        )
    )

    // -----------------------------------------------------------------------
    //  ROPA · Caras y lentes (85–210 Zazoles)
    // -----------------------------------------------------------------------

    private fun cara(
        id: String,
        nombre: String,
        descripcion: String,
        precio: Int,
        emoji: String
    ) = RopaTienda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        emoji = emoji,
        ranura = RanuraRopa.CARA
    )

    private val caras = listOf(
        cara(
            "lentes_corazon",
            "Lentes de Corazón",
            "Todo se ve color de cariño.",
            130,
            "😍"
        ),
        cara(
            "gafas_cyberpunk",
            "Gafas Cyberpunk",
            "Con luces neón que recorren el visor.",
            210,
            "🕶️"
        ),
        cara(
            "antifaz_el_santo",
            "Antifaz de Luchador (El Santo)",
            "Plateado y valiente, como el héroe de la arena.",
            180,
            "🤼"
        ),
        cara(
            "pecas_estrellitas",
            "Pecas de Estrellitas",
            "Diez puntitos brillantes sobre las mejillas.",
            100,
            "⭐"
        ),
        cara(
            "lentes_nerd_cinta",
            "Lentes de Nerd con Cinta",
            "Reparados con cinta adhesiva y mucho estilo.",
            120,
            "🤓"
        ),
        cara(
            "monoculo_elegante",
            "Monóculo Elegante",
            "Para mirar las nubes con clase.",
            160,
            "🧐"
        ),
        cara(
            "lentes_sol_playa",
            "Lentes de Sol de Playa",
            "Con reflejo de atardecer en los cristales.",
            140,
            "🌅"
        ),
        cara(
            "gafas_3d",
            "Gafas 3D",
            "Una lente roja y otra azul; todo se ve doble.",
            110,
            "🎬"
        ),
        cara(
            "bigote_falso",
            "Bigote Falso",
            "Se pega con un poquito de saliva y mucha risa.",
            90,
            "🥸"
        ),
        cara(
            "labios_brillantes",
            "Labios Brillantes",
            "Pintalabios de fresa con destellos.",
            85,
            "💄"
        ),
        cara(
            "lentes_abu",
            "Lentes de Abu",
            "Dorados, grandes y llenos de historias.",
            150,
            "👵"
        ),
        cara(
            "parche_pirata",
            "Parche de Pirata",
            "Negro con calaverita bordada.",
            115,
            "🏴‍☠️"
        ),
        cara(
            "pintura_mejillas",
            "Pintura de Mejillas",
            "Tres rayitas de colores en cada lado.",
            95,
            "🎨"
        ),
        cara(
            "pestanas_neon",
            "Pestañas Neón",
            "Verdes y moradas, se ven desde lejos.",
            125,
            "💚"
        ),
        cara(
            "gafas_mariposa",
            "Gafas de Mariposa",
            "Alas de cristal que cambian de color.",
            155,
            "🦋"
        )
    )

    // -----------------------------------------------------------------------
    //  ROPA · Ropa y cuerpecito (160–320 Zazoles)
    // -----------------------------------------------------------------------

    private fun cuerpo(
        id: String,
        nombre: String,
        descripcion: String,
        precio: Int,
        emoji: String
    ) = RopaTienda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        emoji = emoji,
        ranura = RanuraRopa.CUERPO
    )

    private val prendas = listOf(
        cuerpo(
            "guayabera_blanca",
            "Guayabera Blanca",
            "Con bolsillos y alforcitas para el calor.",
            260,
            "👔"
        ),
        cuerpo(
            "poncho_andino",
            "Poncho de Lana Andino",
            "Rayas de colores y flequitos suaves.",
            240,
            "🧣"
        ),
        cuerpo(
            "chamarra_cuero",
            "Chamarra de Cuero Rockera",
            "Con estrellas y un aire de concierto.",
            300,
            "🧥"
        ),
        cuerpo(
            "tutu_ballet",
            "Tutú de Ballet Arcoíris",
            "Falditas de tul que flotan al volar.",
            220,
            "🩰"
        ),
        cuerpo(
            "traje_astronauta",
            "Traje de Astronauta",
            "Blanco, con parches de planetas y una burbuja.",
            320,
            "👨‍🚀"
        ),
        cuerpo(
            "playera_seleccion",
            "Playera de la Selección de Futbol",
            "Verde con número y muchas porras.",
            250,
            "⚽"
        ),
        cuerpo(
            "sueter_abuelita",
            "Suéter de Abuelita",
            "Tejido a mano, con botones de madera.",
            180,
            "🧶"
        ),
        cuerpo(
            "vestido_flores",
            "Vestido de Flores",
            "Florecitas bordadas y vuelo de campana.",
            230,
            "🌺"
        ),
        cuerpo(
            "capa_superheroe",
            "Capa de Superhéroe",
            "Ondea al viento y da valor.",
            280,
            "🦸"
        ),
        cuerpo(
            "impermeable_pato",
            "Impermeable de Patito",
            "Amarillo, con capucha y botitas.",
            190,
            "🦆"
        ),
        cuerpo(
            "pijama_estrellitas",
            "Pijama de Estrellitas",
            "Fresca, suave y llena de constelaciones.",
            170,
            "🌠"
        ),
        cuerpo(
            "delantal_cocinerito",
            "Delantal de Cocinerito",
            "Con cuchara de madera en el bolsillo.",
            160,
            "👨‍🍳"
        ),
        cuerpo(
            "chaleco_chilango",
            "Chaleco de Chilango",
            "Con botones y una actitud de barrio.",
            210,
            "🚇"
        ),
        cuerpo(
            "kimonito_sakura",
            "Kimonito de Sakura",
            "Rosa con flores de cerezo bordadas.",
            200,
            "🌸"
        ),
        cuerpo(
            "overall_jardinero",
            "Overall de Jardinero",
            "Con bolsas para semillas y una palita.",
            185,
            "🌱"
        )
    )

    // -----------------------------------------------------------------------
    //  MI CASITA · Fondos (200+ Zazoles)
    // -----------------------------------------------------------------------

    private fun fondo(
        id: String,
        nombre: String,
        descripcion: String,
        precio: Int,
        emoji: String,
        escena: EscenaFondo
    ) = FondoTienda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        precio = precio,
        emoji = emoji,
        escena = escena
    )

    private val listaFondos = listOf(
        fondo(
            "bosque_chapultepec",
            "Bosque de Chapultepec",
            "Árboles enormes y ardillas que saludan.",
            200,
            "🌳",
            EscenaFondo.CHAPULTEPEC
        ),
        fondo(
            "ruinas_mayas_neon",
            "Ruinas Mayas Neón",
            "Pirámides con luces de colores y luciérnagas.",
            260,
            "🔺",
            EscenaFondo.RUINAS
        ),
        fondo(
            "espacio_via_lactea",
            "Espacio Exterior (Vía Láctea)",
            "Un cielo lleno de estrellas y planetas amigos.",
            320,
            "🌌",
            EscenaFondo.GALAXIA
        ),
        fondo(
            "cuarto_kawaii_pastel",
            "Cuarto Kawaii Pastel",
            "Cortinas rosas, cojines y una lámpara de nube.",
            220,
            "🎀",
            EscenaFondo.KAWAII
        ),
        fondo(
            "aurora_boreal",
            "Aurora Boreal",
            "Cortinas de luz verde que bailan despacito.",
            340,
            "🌠",
            EscenaFondo.AURORA
        ),
        fondo(
            "cempasuchil_noche",
            "Cempasúchil de Noche",
            "Pétalos anaranjados que guían a los recuerdos.",
            280,
            "🌼",
            EscenaFondo.CEMPASUCHIL
        ),
        fondo(
            "playa_esmeralda",
            "Playa Esmeralda",
            "Arena tibia, olas suaves y conchitas.",
            240,
            "🐚",
            EscenaFondo.PLAYA
        ),
        fondo(
            "volcan_popocatepetl",
            "Volcán Popocatépetl",
            "Humo suavecito y nieve en la punta.",
            300,
            "🌋",
            EscenaFondo.VOLCAN
        ),
        fondo(
            "biblioteca_encantada",
            "Biblioteca Encantada",
            "Libros que flotan y cuentan cuentos solos.",
            230,
            "📚",
            EscenaFondo.BIBLIOTECA
        ),
        fondo(
            "chinampa_flores",
            "Chinampa de Flores",
            "Canales, trajineras y flores de todos colores.",
            250,
            "🌺",
            EscenaFondo.CHINAMPA
        ),
        fondo(
            "arcade_retro",
            "Arcade Retro",
            "Maquinitas con luces y música de 8-bits.",
            270,
            "👾",
            EscenaFondo.ARCADE
        ),
        fondo(
            "alebrijes_sueno",
            "Alebrijes de Sueño",
            "Animales fantásticos de colores imposibles.",
            360,
            "🐉",
            EscenaFondo.ALEBRIJES
        ),
        fondo(
            "nube_de_algodon",
            "Nube de Algodón",
            "Suavecita, rosita y flotando en el cielo.",
            310,
            "☁️",
            EscenaFondo.ALGODON
        ),
        fondo(
            "nebulosa_helado",
            "Nebulosa de Helado",
            "Gases morados con olor a vainilla.",
            330,
            "💫",
            EscenaFondo.NEBULOSA
        ),
        fondo(
            "lluvia_estrellas",
            "Lluvia de Estrellas",
            "Pide un deseo cada vez que cae una.",
            350,
            "🌠",
            EscenaFondo.LLUVIA
        ),
        fondo(
            "casa_caramelo",
            "Casa de Caramelo",
            "Paredes de galleta y techo de chocolate.",
            380,
            "🍭",
            EscenaFondo.CARAMELO
        ),
        fondo(
            "jardin_mariposas",
            "Jardín de Mariposas",
            "Monarcas que se posan en las flores.",
            290,
            "🦋",
            EscenaFondo.MARIPOSAS
        ),
        fondo(
            "cima_montana",
            "Cima de Montaña",
            "Aire fresco y nubes al alcance de la mano.",
            210,
            "⛰️",
            EscenaFondo.MONTANA
        ),
        fondo(
            "cueva_cristal",
            "Cueva de Cristal",
            "Geodas que tintinean cuando Zazi vuela.",
            400,
            "💎",
            EscenaFondo.CRISTAL
        ),
        fondo(
            "atardecer_verde",
            "Atardecer Verde",
            "El sol se despide pintando el cielo de limón.",
            205,
            "🌅",
            EscenaFondo.ATARDECER
        ),
        fondo(
            "isla_zazoles",
            "Isla de Zazoles",
            "Una isla donde crecen monedas doradas.",
            420,
            "🏝️",
            EscenaFondo.ISLA
        ),
        fondo(
            "reloj_pastel",
            "Reloj de Pastel",
            "El tiempo pasa entre capas de crema.",
            390,
            "🕰️",
            EscenaFondo.RELOJ
        )
    )

    // -----------------------------------------------------------------------
    //  Consultas
    // -----------------------------------------------------------------------

    private const val GRUPO_NUTRICION = "Nutrición Básica"
    private const val GRUPO_ANTOJITOS = "Antojitos Mexicanos y Latinos"
    private const val GRUPO_GOLOSINAS = "Golosinas Mágicas"
    private const val GRUPO_HIGIENE = "Higiene"
    private const val GRUPO_JUGUETES = "Juguetes"

    val consumibles: List<ConsumibleTienda> =
        nutricionBasica + antojitos + golosinas + articulosHigiene + juguetes

    val ropa: List<RopaTienda> = gorros + caras + prendas

    val fondos: List<FondoTienda> = listaFondos

    private val mapaConsumibles: Map<String, ConsumibleTienda> =
        consumibles.associateBy { it.id }

    private val mapaRopa: Map<String, RopaTienda> = ropa.associateBy { it.id }

    private val mapaFondos: Map<String, FondoTienda> = fondos.associateBy { it.id }

    fun consumiblePorId(id: String?): ConsumibleTienda? = id?.let { mapaConsumibles[it] }

    fun ropaPorId(id: String?): RopaTienda? = id?.let { mapaRopa[it] }

    fun fondoPorId(id: String?): FondoTienda? = id?.let { mapaFondos[it] }

    /**
     * Artículos agrupados para la pestaña indicada. Ropa y casita se resuelven
     * en la pantalla porque usan tarjetas distintas.
     */
    fun consumiblesDe(pestana: PestanaTienda): List<GrupoConsumibles> = when (pestana) {
        PestanaTienda.COMIDA -> listOf(
            GrupoConsumibles(GRUPO_NUTRICION, "🍎", nutricionBasica),
            GrupoConsumibles(GRUPO_ANTOJITOS, "🌮", antojitos),
            GrupoConsumibles(GRUPO_GOLOSINAS, "🪄", golosinas)
        )

        PestanaTienda.CUIDADO -> listOf(
            GrupoConsumibles(GRUPO_HIGIENE, "🛁", articulosHigiene),
            GrupoConsumibles(GRUPO_JUGUETES, "🧸", juguetes)
        )

        PestanaTienda.ROPA, PestanaTienda.CASITA -> emptyList()
    }

    fun ropaDe(ranura: RanuraRopa): List<RopaTienda> = ropa.filter { it.ranura == ranura }
}
