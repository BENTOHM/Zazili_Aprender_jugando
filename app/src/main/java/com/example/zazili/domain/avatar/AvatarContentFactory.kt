package com.example.zazili.domain.avatar

import androidx.compose.runtime.Immutable

/**
 * Orden de pintado del avatar pixel. Cada valor trae su Z-index explícito para
 * que el renderizador ordene las capas sin depender del orden del catálogo.
 *
 * El cabello largo se parte en dos capas: [PELO_ATRAS] se pinta antes del
 * cuerpo y [PELO_FRENTE] después de la ropa, para que la melena caiga por
 * detrás de los hombros y el flequillo quede sobre la cara.
 */
enum class CapaAvatar(val z: Int) {
    PELO_ATRAS(10),
    ESPALDA(20),
    CUERPO(30),
    ROPA(40),
    PELO_FRENTE(50),
    RASGOS(60),
    ROSTRO(70),
    CABEZA(80),
    EXTERIOR(90)
}

/** Ranuras del armario, en el orden en que se muestran en la tienda pixel. */
enum class AvatarCategory(
    val clave: String,
    val etiqueta: String,
    val emoji: String,
    val capaPorDefecto: CapaAvatar
) {
    PIEL("piel", "Piel", "🎨", CapaAvatar.CUERPO),
    OJOS("ojos", "Ojos", "👀", CapaAvatar.RASGOS),
    BOCA("boca", "Boca", "😊", CapaAvatar.RASGOS),
    ROPA("ropa", "Ropa", "👕", CapaAvatar.ROPA),
    CABEZA("cabeza", "Cabezas", "🎩", CapaAvatar.CABEZA),
    PELO("pelo", "Peinados", "💇", CapaAvatar.PELO_FRENTE),
    ROSTRO("rostro", "Rostro", "🕶️", CapaAvatar.ROSTRO),
    ESPALDA("espalda", "Espalda", "🪽", CapaAvatar.ESPALDA)
}

/**
 * Prenda, peinado o accesorio del armario pixel.
 *
 * [resourceId] es la clave de dibujo que consume `ChildAvatarRenderer`: no es
 * un id de Android, sino el nombre de la variante (escaramuza, kuromi,
 * alas_hada…). A partir de él se calcula [spriteDrawable], el nombre base del
 * PNG de 64×64 píxeles que vive en `app/src/main/res/drawable-nodpi/`.
 *
 * Todos los sprites humanos son archivos PNG reales; no existe dibujo vectorial
 * ni generación procedural en tiempo de ejecución. Los sprites se buscan por
 * capa con el sufijo de la pasada de dibujo:
 * `avatar_<categoria>_<resourceId>_<capa>.png` (por ejemplo,
 * `avatar_pelo_melena_suave_pelo_atras.png`). Si un PNG no está instalado, esa
 * capa simplemente no se pinta: el juego sigue funcionando sin arte vectorial
 * de respaldo.
 *
 * [capas] declara en qué pasadas participa la pieza. Los peinados largos usan
 * [CapaAvatar.PELO_ATRAS] + [CapaAvatar.PELO_FRENTE]; las alas y capas viven
 * estrictamente en [CapaAvatar.ESPALDA] para no recortarse con el cuerpo.
 *
 * Los artículos con `price = 0` nacen equipados y nunca se cobran.
 */
@Immutable
data class AvatarItem(
    val id: String,
    val name: String,
    val category: AvatarCategory,
    val price: Int,
    val isUnlockedByDefault: Boolean,
    val resourceId: String,
    val descripcion: String = "",
    val capas: Set<CapaAvatar> = setOf(category.capaPorDefecto)
) {
    init {
        require(price >= 0) { "El precio de $id no puede ser negativo." }
        require(capas.isNotEmpty()) { "El artículo $id necesita al menos una capa de dibujo." }
    }

    val zInferior: Int get() = capas.minOf { it.z }
    val zSuperior: Int get() = capas.maxOf { it.z }
    val capasOrdenadas: List<CapaAvatar> get() = capas.sortedBy { it.z }

    fun ocupa(capa: CapaAvatar): Boolean = capa in capas

    /** Nombre base del PNG pixel en `drawable-nodpi`, sin sufijo de capa. */
    val spriteDrawable: String
        get() = "avatar_${category.clave}_$resourceId"

    /** Ruta lógica completa del sprite base, útil para documentación y pruebas. */
    val spriteRuta: String
        get() = "drawable-nodpi/$spriteDrawable.png"
}

/** Conjunto de piezas puestas, una por ranura. `null` deja la ranura vacía. */
@Immutable
data class EquipamientoAvatar(
    val piel: AvatarItem? = null,
    val ojos: AvatarItem? = null,
    val boca: AvatarItem? = null,
    val ropa: AvatarItem? = null,
    val cabeza: AvatarItem? = null,
    val pelo: AvatarItem? = null,
    val rostro: AvatarItem? = null,
    val espalda: AvatarItem? = null
) {
    fun de(categoria: AvatarCategory): AvatarItem? = when (categoria) {
        AvatarCategory.PIEL -> piel
        AvatarCategory.OJOS -> ojos
        AvatarCategory.BOCA -> boca
        AvatarCategory.ROPA -> ropa
        AvatarCategory.CABEZA -> cabeza
        AvatarCategory.PELO -> pelo
        AvatarCategory.ROSTRO -> rostro
        AvatarCategory.ESPALDA -> espalda
    }

    fun con(item: AvatarItem?): EquipamientoAvatar = when (item?.category) {
        AvatarCategory.PIEL -> copy(piel = item)
        AvatarCategory.OJOS -> copy(ojos = item)
        AvatarCategory.BOCA -> copy(boca = item)
        AvatarCategory.ROPA -> copy(ropa = item)
        AvatarCategory.CABEZA -> copy(cabeza = item)
        AvatarCategory.PELO -> copy(pelo = item)
        AvatarCategory.ROSTRO -> copy(rostro = item)
        AvatarCategory.ESPALDA -> copy(espalda = item)
        null -> this
    }

    fun sin(categoria: AvatarCategory): EquipamientoAvatar = when (categoria) {
        AvatarCategory.PIEL -> copy(piel = null)
        AvatarCategory.OJOS -> copy(ojos = null)
        AvatarCategory.BOCA -> copy(boca = null)
        AvatarCategory.ROPA -> copy(ropa = null)
        AvatarCategory.CABEZA -> copy(cabeza = null)
        AvatarCategory.PELO -> copy(pelo = null)
        AvatarCategory.ROSTRO -> copy(rostro = null)
        AvatarCategory.ESPALDA -> copy(espalda = null)
    }

    val articulos: List<AvatarItem>
        get() = listOfNotNull(piel, ojos, boca, ropa, cabeza, pelo, rostro, espalda)
}

/**
 * Catálogo completo del armario pixel de Zazi.
 *
 * Todo el catálogo vive en memoria: es puro Kotlin, sin base de datos. Mezcla
 * estética kawaii, cultura mexicana (escaramuza, huipil, Catrina, cempasúchil,
 * Frida) y homenajes pop (Snoopy, Kuromi, Hello Kitty, héroe arácnido) con la
 * misma equidad de género: hay peinados largos, cortos, rizados, trenzados,
 * rapados y con picos; ropa femenina, masculina y neutra; y accesorios para
 * todos los gustos.
 *
 * Cada pieza es un PNG 2D pixel art de 64×64 píxeles, dibujado sin suavizado
 * (nearest-neighbor) y alineado sobre la misma rejilla fija. El renderizador
 * carga los archivos de `res/drawable-nodpi` y los pinta con
 * `FilterQuality.None`; no hay rutas, vectores ni Canvas para el avatar humano.
 *
 * Escalón económico:
 *  - Básicos gratis o 10–50 Zazoles.
 *  - Tesoros culturales a 150 Zazoles.
 *  - Homenajes pop premium de 300 a 500 Zazoles.
 */
object AvatarContentFactory {

    private val PELO_ATRAS_Y_FRENTE = setOf(CapaAvatar.PELO_ATRAS, CapaAvatar.PELO_FRENTE)
    private val SOLO_PELO_FRENTE = setOf(CapaAvatar.PELO_FRENTE)

    val todos: List<AvatarItem> = listOf(
        // ----------------------------- PIEL -----------------------------
        AvatarItem("piel_canela", "Piel de Canela", AvatarCategory.PIEL, 0, true, "canela", "Cálida como el sol de mediodía."),
        AvatarItem("piel_miel", "Piel de Miel", AvatarCategory.PIEL, 0, true, "miel", "Dulce tono de panal recién hecho."),
        AvatarItem("piel_cacao", "Piel de Cacao", AvatarCategory.PIEL, 0, true, "cacao", "Profunda como el chocolate de Oaxaca."),
        AvatarItem("piel_marfil", "Piel de Marfil", AvatarCategory.PIEL, 40, false, "marfil", "Clara y suave como la porcelana."),
        AvatarItem("piel_durazno", "Piel de Durazno", AvatarCategory.PIEL, 45, false, "durazno", "Rosadita como el atardecer."),
        AvatarItem("piel_bronce", "Piel de Bronce", AvatarCategory.PIEL, 60, false, "bronce", "Brilla con la luz de la tarde."),
        AvatarItem("piel_obsidiana", "Piel de Obsidiana", AvatarCategory.PIEL, 75, false, "obsidiana", "Elegante y misteriosa como el volcán."),
        AvatarItem("piel_cempasuchil", "Piel de Cempasúchil", AvatarCategory.PIEL, 90, false, "cempasuchil", "Naranja de flor de muerto."),
        AvatarItem("piel_ajolote", "Piel de Ajolote", AvatarCategory.PIEL, 120, false, "ajolote", "Rosa y suavecita como las branquias."),
        AvatarItem("piel_jade", "Piel de Jade Maya", AvatarCategory.PIEL, 140, false, "jade", "Verde piedra sagrada."),
        AvatarItem("piel_zombie", "Piel de Zombie", AvatarCategory.PIEL, 150, false, "zombie", "Verde, con costuras y todo."),
        AvatarItem("piel_alebrije", "Piel de Alebrije", AvatarCategory.PIEL, 180, false, "alebrije", "Mágica y llena de colores."),
        AvatarItem("piel_robot", "Piel de Robot Metálico", AvatarCategory.PIEL, 220, false, "metal", "Acero pulido con luces LED."),
        AvatarItem("piel_galactica", "Piel Galáctica", AvatarCategory.PIEL, 260, false, "galactica", "Con constelaciones en la mejilla."),

        // ----------------------------- OJOS -----------------------------
        AvatarItem("ojos_felices", "Ojitos Felices", AvatarCategory.OJOS, 0, true, "felices", "Sonríen incluso cuando duermen."),
        AvatarItem("ojos_curiosos", "Ojitos Curiosos", AvatarCategory.OJOS, 0, true, "curiosos", "Todo lo quieren ver de cerca."),
        AvatarItem("ojos_dormilones", "Ojitos Dormilones", AvatarCategory.OJOS, 0, true, "dormilones", "Cerraditos, soñando con tacos."),
        AvatarItem("ojos_pestanas_rubor", "Ojos con Pestañas y Rubor Kawaii", AvatarCategory.OJOS, 50, false, "pestanas_rubor", "Pestañas de mariposa y mejillas sonrojadas."),
        AvatarItem("ojos_gato", "Ojos de Gato", AvatarCategory.OJOS, 60, false, "gato", "Con pupila de felino curioso."),
        AvatarItem("ojos_mariposa", "Ojos de Mariposa", AvatarCategory.OJOS, 80, false, "mariposa", "Pestañas que revolotean."),
        AvatarItem("ojos_alebrije", "Ojos de Alebrije", AvatarCategory.OJOS, 90, false, "alebrije", "Remolinos de colores."),
        AvatarItem("ojos_corazon", "Ojos de Corazón", AvatarCategory.OJOS, 100, false, "corazon", "Enamorados de los zazoles."),
        AvatarItem("ojos_luchador", "Ojos de Luchador", AvatarCategory.OJOS, 110, false, "luchador", "Decididos para la revancha."),
        AvatarItem("ojos_estrella", "Ojos de Estrella", AvatarCategory.OJOS, 120, false, "estrella", "Brillan de pura emoción."),
        AvatarItem("ojos_jade", "Ojos de Jade", AvatarCategory.OJOS, 130, false, "jade", "Verdes como el cenote."),
        AvatarItem("ojos_robot", "Ojos de Robot", AvatarCategory.OJOS, 160, false, "robot", "LEDs que escanean la tarea."),
        AvatarItem("ojos_fuego", "Ojos de Fuego", AvatarCategory.OJOS, 170, false, "fuego", "Con llamitas en la mirada."),

        // ----------------------------- BOCA -----------------------------
        AvatarItem("boca_sol", "Sonrisa de Sol", AvatarCategory.BOCA, 0, true, "sol", "De oreja a oreja."),
        AvatarItem("boca_luna", "Sonrisa de Luna", AvatarCategory.BOCA, 0, true, "luna", "Medio círculo de felicidad."),
        AvatarItem("boca_picara", "Sonrisa Pícara", AvatarCategory.BOCA, 0, true, "picara", "Tiene un plan muy bueno."),
        AvatarItem("boca_brackets", "Sonrisa con Brackets de Colores", AvatarCategory.BOCA, 40, false, "brackets", "Combinables con el moño del día."),
        AvatarItem("boca_bigotes", "Bigotes de Charro", AvatarCategory.BOCA, 50, false, "bigotes", "Respeto de rancho."),
        AvatarItem("boca_mariachi", "Sonrisa de Mariachi", AvatarCategory.BOCA, 55, false, "mariachi", "Con diente de oro."),
        AvatarItem("boca_ajolote", "Silbido de Ajolote", AvatarCategory.BOCA, 70, false, "silbido", "Sopla burbujitas."),
        AvatarItem("boca_zombi", "Sonrisa de Zombi", AvatarCategory.BOCA, 95, false, "zombi", "Cosida con hilo de plata."),
        AvatarItem("boca_nahual", "Colmillos de Nahual", AvatarCategory.BOCA, 100, false, "colmillos", "Muerde miedos, no amigos."),
        AvatarItem("boca_alebrije", "Boca de Alebrije", AvatarCategory.BOCA, 110, false, "alebrije", "Ondulada y juguetona."),
        AvatarItem("boca_catrina", "Labios de Catrina", AvatarCategory.BOCA, 120, false, "catrina", "Bordados con cempasúchil."),
        AvatarItem("boca_robot", "Boca de Robot", AvatarCategory.BOCA, 140, false, "robot", "Bocina con ritmo eléctrico."),
        AvatarItem("boca_galactica", "Sonrisa Galáctica", AvatarCategory.BOCA, 150, false, "galactica", "Con chispitas de estrella."),

        // ----------------------------- ROPA -----------------------------
        AvatarItem("ropa_playera_zazili", "Playera de Zazili", AvatarCategory.ROPA, 0, true, "playera", "El uniforme oficial de la aventura."),
        AvatarItem("ropa_sudadera_amarilla", "Sudadera Amarilla", AvatarCategory.ROPA, 0, true, "sudadera", "Abrigadita para las rachas."),
        AvatarItem("ropa_camisa_cuadros", "Camisa de Cuadros", AvatarCategory.ROPA, 0, true, "cuadros", "De leñador urbano."),
        AvatarItem("ropa_sudadera_oversize", "Sudadera Oversize Pastel", AvatarCategory.ROPA, 40, false, "sudadera_oversize", "Cómoda para las tardes de series."),
        AvatarItem("ropa_overol_girasoles", "Overol de Mezclilla con Girasoles", AvatarCategory.ROPA, 45, false, "overol_girasoles", "Bolsita bordada de girasoles."),
        AvatarItem("ropa_playera_ajolote", "Playera de Ajolote", AvatarCategory.ROPA, 50, false, "ajolote", "Con ajolote estampado."),
        AvatarItem("ropa_playera_taqueria", "Playera de Taquería", AvatarCategory.ROPA, 60, false, "taqueria", "Dos de pastor, por favor."),
        AvatarItem("ropa_sudadera_alebrije", "Sudadera de Alebrije", AvatarCategory.ROPA, 90, false, "alebrije", "Suave y llena de color."),
        AvatarItem("ropa_vestido_mariposa", "Vestido de Mariposa", AvatarCategory.ROPA, 100, false, "vestido_mariposa", "Mangas que parecen alas."),
        AvatarItem("ropa_lucha", "Camiseta de Lucha Libre", AvatarCategory.ROPA, 140, false, "lucha", "Máscara plateada incluida en el pecho."),
        AvatarItem("ropa_escaramuza", "Vestido de Escaramuza Charra", AvatarCategory.ROPA, 150, false, "escaramuza", "Vuelos de falda y listones al galope."),
        AvatarItem("ropa_huipil", "Huipil Bordado Oaxaqueño", AvatarCategory.ROPA, 150, false, "huipil", "Flores bordadas a mano en el Istmo."),
        AvatarItem("ropa_quinceanera_neon", "Vestido de Quinceañera Neón", AvatarCategory.ROPA, 150, false, "quinceanera_neon", "Brillo de fiesta y vuelo de vals."),
        AvatarItem("ropa_superheroe", "Playera de Superhéroe", AvatarCategory.ROPA, 160, false, "superheroe", "Con rayo en el pecho."),
        AvatarItem("ropa_overol", "Overol de Minero", AvatarCategory.ROPA, 170, false, "overol", "Para picar piedra cúbica."),
        AvatarItem("ropa_falda_kpop", "Falda Plisada Estilo K-Pop", AvatarCategory.ROPA, 200, false, "falda_kpop", "Pliegues que giran al bailar."),
        AvatarItem("ropa_capoeira", "Traje de Capoeira", AvatarCategory.ROPA, 200, false, "capoeira", "Pantalón blanco y faja verde."),
        AvatarItem("ropa_cyberpunk", "Chaqueta Cyberpunk", AvatarCategory.ROPA, 210, false, "cyberpunk", "Neón en las costuras."),
        AvatarItem("ropa_poncho", "Poncho Andino", AvatarCategory.ROPA, 220, false, "poncho", "Rayas de colores de la sierra."),
        AvatarItem("ropa_mago", "Túnica de Mago Escolar", AvatarCategory.ROPA, 240, false, "mago", "Con estrellas plateadas."),
        AvatarItem("ropa_mariachi", "Traje de Mariachi", AvatarCategory.ROPA, 250, false, "mariachi", "Botones de plata y moño rojo."),
        AvatarItem("ropa_diablada", "Túnica de Diablada", AvatarCategory.ROPA, 260, false, "diablada", "Lentejuelas y flecos para bailar."),
        AvatarItem("ropa_flamenca", "Traje de Flamenca", AvatarCategory.ROPA, 280, false, "flamenca", "Vestido de volantes con lunares."),
        AvatarItem("ropa_charro", "Traje de Charro", AvatarCategory.ROPA, 300, false, "charro", "Bordado de oro de gala."),
        AvatarItem("ropa_astronauta", "Traje de Astronauta", AvatarCategory.ROPA, 330, false, "astronauta", "Listo para Marte, pero con lunch."),
        AvatarItem("ropa_playera_snoopy", "Playera de Snoopy", AvatarCategory.ROPA, 350, false, "snoopy", "Beagle volador estampado en el pecho."),
        AvatarItem("ropa_guerrero", "Traje de Guerrero Águila", AvatarCategory.ROPA, 350, false, "guerrero", "Plumas y placa dorada."),
        AvatarItem("ropa_playera_dinosaurio", "Playera de Dinosaurio", AvatarCategory.ROPA, 45, false, "playera_dinosaurio", "Un dino feroz… y muy tierno."),
        AvatarItem("ropa_pijama_estrellas", "Pijama de Estrellas", AvatarCategory.ROPA, 45, false, "pijama_estrellas", "Para soñar despierto en pijama."),
        AvatarItem("ropa_overol_plomero", "Overol Rojo de Plomero", AvatarCategory.ROPA, 50, false, "overol_plomero", "Listo para rescatar la tubería."),
        AvatarItem("ropa_sudadera_dinosaurio", "Sudadera de Dinosaurio", AvatarCategory.ROPA, 55, false, "sudadera_dinosaurio", "Con capucha de dientes suaves."),
        AvatarItem("ropa_seleccion_futbol", "Playera de la Selección de Fútbol", AvatarCategory.ROPA, 60, false, "seleccion_futbol", "Verde, blanca y con escudo al pecho."),
        AvatarItem("ropa_chamarra_beisbolista", "Chamarra de Beisbolista", AvatarCategory.ROPA, 65, false, "chamarra_beisbolista", "Mangas azules para el home run."),
        AvatarItem("ropa_uniforme_taekwondo", "Uniforme de Taekwondo", AvatarCategory.ROPA, 120, false, "uniforme_taekwondo", "Dobok blanco con cinta negra."),
        AvatarItem("ropa_armadura_caballero", "Armadura de Caballero de 8-bits", AvatarCategory.ROPA, 220, false, "armadura_caballero", "Placas de acero con brillo dorado."),

        // ----------------------------- CABEZAS --------------------------
        // SIN TOCADO POR DEFECTO: ninguna pieza de esta ranura se equipa sola.
        // El avatar nace con la cabeza descubierta y el niño elige su gorro.
        AvatarItem("cabeza_coleta", "Coleta Rebelde", AvatarCategory.CABEZA, 0, true, "coleta", "Moño alto con mucha actitud."),
        AvatarItem("cabeza_mono_gigante", "Moño Gigante de Listón", AvatarCategory.CABEZA, 20, false, "mono_gigante", "Más grande que la tarea."),
        AvatarItem("cabeza_diadema", "Diadema de Estrellas", AvatarCategory.CABEZA, 40, false, "diadema", "Estrellitas alrededor de la cabeza."),
        AvatarItem("cabeza_diadema_perlas", "Diadema de Perlas", AvatarCategory.CABEZA, 50, false, "diadema_perlas", "Brillito elegante para cualquier día."),
        AvatarItem("cabeza_flores", "Corona de Flores de Cempasúchil", AvatarCategory.CABEZA, 150, false, "flores", "Pétalos anaranjados que huelen a fiesta."),
        AvatarItem("cabeza_sandia", "Cabeza de Sandía", AvatarCategory.CABEZA, 140, false, "sandia", "Fresquita y con semillas."),
        AvatarItem("cabeza_calabaza", "Cabeza de Calabaza", AvatarCategory.CABEZA, 150, false, "calabaza", "Con tallo y todo."),
        AvatarItem("cabeza_capucha", "Gorro de Luchador", AvatarCategory.CABEZA, 160, false, "capucha", "Capucha de encordado."),
        AvatarItem("cabeza_chullo", "Chullo Peruano", AvatarCategory.CABEZA, 190, false, "chullo", "Orejeras con pompones andinos."),
        AvatarItem("cabeza_birrete", "Birrete de Graduación", AvatarCategory.CABEZA, 200, false, "birrete", "Borla dorada de logros."),
        AvatarItem("cabeza_cuernos", "Cabeza de Alebrije", AvatarCategory.CABEZA, 240, false, "cuernos", "Cuernitos de fantasía."),
        AvatarItem("cabeza_casco", "Cabeza de Robot", AvatarCategory.CABEZA, 260, false, "casco", "Casco metálico con antena."),
        AvatarItem("cabeza_mariachi", "Sombrero de Mariachi", AvatarCategory.CABEZA, 280, false, "sombrero_mariachi", "Ala ancha con cordón de plata."),
        AvatarItem("cabeza_corona_jade", "Corona de Jade", AvatarCategory.CABEZA, 300, false, "corona_jade", "Puntas de piedra verde."),
        AvatarItem("cabeza_charro", "Sombrero de Charro", AvatarCategory.CABEZA, 300, false, "sombrero_charro", "Galoneado de oro, bien plantado."),
        AvatarItem("cabeza_cubica", "Cabeza Cúbica de Minero", AvatarCategory.CABEZA, 350, false, "cubica", "Cuadrada y pixelada."),
        AvatarItem("cabeza_penacho_fuego", "Penacho de Fuego", AvatarCategory.CABEZA, 360, false, "penacho_fuego", "Llamaradas en la cabeza."),
        AvatarItem("cabeza_hello_kitty", "Diadema de Hello Kitty", AvatarCategory.CABEZA, 400, false, "hello_kitty", "Moño rojo y carita tierna."),
        AvatarItem("cabeza_penacho", "Penacho Azteca", AvatarCategory.CABEZA, 500, false, "penacho", "Plumas de quetzal y oro."),
        AvatarItem("cabeza_gorra_reversa", "Gorra Hacia Atrás", AvatarCategory.CABEZA, 30, false, "gorra_reversa", "Visera al revés, actitud al frente."),
        AvatarItem("cabeza_audifonos_retro", "Audífonos Retro", AvatarCategory.CABEZA, 50, false, "audifonos_retro", "Música ochentera para estudiar."),
        AvatarItem("cabeza_casco_caballero", "Casco de Caballero 8-bits", AvatarCategory.CABEZA, 180, false, "casco_caballero", "Yelmo con visera y penacho rojo."),

        // ----------------------------- PELO -----------------------------
        AvatarItem("pelo_melena_suave", "Melena Suave", AvatarCategory.PELO, 0, true, "melena_suave", "Lacia, brillante y lista para todo.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_media_cola", "Media Cola con Listón", AvatarCategory.PELO, 20, false, "media_cola", "Un listón sostiene la coleta del día.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_chongos_espaciales", "Chongos Espaciales", AvatarCategory.PELO, 25, false, "chongos", "Dos chonguitos que parecen planetas.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_bob_pastel", "Corte Bob Color Pastel", AvatarCategory.PELO, 30, false, "bob_pastel", "Recto, suave y con reflejos de algodón de azúcar.", SOLO_PELO_FRENTE),
        AvatarItem("pelo_rizos_nube", "Rizos de Nube", AvatarCategory.PELO, 40, false, "rizos_nube", "Esponjaditos como nube de lluvia.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_largo_espigado", "Cabello Largo Espigado", AvatarCategory.PELO, 45, false, "largo_espigado", "Largo y con puntas de tijera.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_ondas_largas", "Cabello Largo con Ondas", AvatarCategory.PELO, 50, false, "ondas_largas", "Ondas de mar que bailan al caminar.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_afro_estrellas", "Afro de Estrellitas", AvatarCategory.PELO, 50, false, "afro_estrellas", "Volumen enorme con brillitos prendidos.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_trenza_corona", "Trenza Corona con Perlas", AvatarCategory.PELO, 60, false, "trenza_corona", "Trenza alrededor de la cabeza, como princesa.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_trenzas_listones", "Trenzas con Listones Tricolores", AvatarCategory.PELO, 150, false, "trenzas_listones", "Dos trenzas con listones verde, blanco y rojo.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_frida", "Trenza Estilo Frida Kahlo", AvatarCategory.PELO, 150, false, "trenza_frida", "Trenza corona con flores y mucho color.", PELO_ATRAS_Y_FRENTE),
        AvatarItem("pelo_desordenado_anime", "Cabello Desordenado Estilo Anime", AvatarCategory.PELO, 0, true, "desordenado_anime", "Picos rebeldes que no piden permiso.", SOLO_PELO_FRENTE),
        AvatarItem("pelo_hongo_retro", "Corte de Hongo Retro", AvatarCategory.PELO, 25, false, "hongo_retro", "Flequillo redondo de los noventa.", SOLO_PELO_FRENTE),
        AvatarItem("pelo_librito_90s", "Peinado de Librito 90s", AvatarCategory.PELO, 30, false, "librito_90s", "Raya en medio y mechones de revista.", SOLO_PELO_FRENTE),
        AvatarItem("pelo_rizos_cortos", "Rizos Cortos Neutros", AvatarCategory.PELO, 30, false, "rizos_cortos", "Rizos apretados, fáciles de peinar.", SOLO_PELO_FRENTE),
        AvatarItem("pelo_rapado_lineas", "Rapado con Líneas", AvatarCategory.PELO, 35, false, "rapado_lineas", "Tres líneas a navaja, bien perfiladas.", SOLO_PELO_FRENTE),
        AvatarItem("pelo_mohawk_menta", "Mohawk Menta", AvatarCategory.PELO, 45, false, "mohawk_menta", "Cresta verde que apunta al cielo.", SOLO_PELO_FRENTE),
        AvatarItem("pelo_picos_sayayin", "Picos de Super Sayayin", AvatarCategory.PELO, 100, false, "picos_sayayin", "Cabello dorado que desafía la gravedad.", SOLO_PELO_FRENTE),

        // ----------------------------- ROSTRO ---------------------------
        AvatarItem("rostro_lentes", "Lentes Redondos", AvatarCategory.ROSTRO, 0, true, "lentes", "De intelectual inquieto."),
        AvatarItem("rostro_pecas", "Pecas de Sol", AvatarCategory.ROSTRO, 0, true, "pecas", "Pintitas de sol en las mejillas."),
        AvatarItem("rostro_brillo_hada", "Brillo de Hada", AvatarCategory.ROSTRO, 30, false, "brillo_hada", "Purpurina en mejillas y nariz.", setOf(CapaAvatar.RASGOS)),
        AvatarItem("rostro_pecas_estrellitas", "Pecas de Estrellitas", AvatarCategory.ROSTRO, 35, false, "pecas_estrellitas", "Constelaciones diminutas en las mejillas.", setOf(CapaAvatar.RASGOS)),
        AvatarItem("rostro_bigotito", "Bigotito de Charro", AvatarCategory.ROSTRO, 40, false, "bigotito", "Fino y respetable."),
        AvatarItem("rostro_gafas_nerd", "Gafas de Nerd", AvatarCategory.ROSTRO, 50, false, "gafas_nerd", "Para estudiar con estilo."),
        AvatarItem("rostro_sol", "Anteojos de Sol", AvatarCategory.ROSTRO, 60, false, "sol", "Muy cool para el recreo."),
        AvatarItem("rostro_lentes_corazon", "Lentes de Corazón", AvatarCategory.ROSTRO, 60, false, "lentes_corazon", "Todo se ve con amor."),
        AvatarItem("rostro_aretes_concha", "Aretes de Pan Dulce", AvatarCategory.ROSTRO, 80, false, "aretes_concha", "Un par de conchas con azúcar."),
        AvatarItem("rostro_catrina", "Pintura de Catrina", AvatarCategory.ROSTRO, 100, false, "catrina", "Calaverita de azúcar."),
        AvatarItem("rostro_antifaz", "Antifaz de Luchador", AvatarCategory.ROSTRO, 110, false, "antifaz", "A la arena, sin miedo."),
        AvatarItem("rostro_maquillaje_catrina", "Maquillaje de Catrina Cute", AvatarCategory.ROSTRO, 150, false, "maquillaje_catrina", "Florecitas y puntitos de azúcar.", setOf(CapaAvatar.RASGOS)),
        AvatarItem("rostro_calavera", "Máscara de Calavera", AvatarCategory.ROSTRO, 150, false, "calavera", "Blanca y con dientes."),
        AvatarItem("rostro_cyberpunk", "Gafas Cyberpunk", AvatarCategory.ROSTRO, 170, false, "cyberpunk", "Visor de neón turquesa."),
        AvatarItem("rostro_rayos", "Gafas de Rayos X", AvatarCategory.ROSTRO, 190, false, "rayos", "Ven los huesos… y los dulces."),
        AvatarItem("rostro_robot", "Máscara de Robot", AvatarCategory.ROSTRO, 210, false, "robot", "Placa de metal con sensores."),
        AvatarItem("rostro_alebrije", "Máscara de Alebrije", AvatarCategory.ROSTRO, 230, false, "alebrije", "Geometrías de colores."),
        AvatarItem("rostro_diablada", "Máscara de Diablada", AvatarCategory.ROSTRO, 280, false, "diablada", "Cuernos y colmillos de fiesta."),
        AvatarItem("rostro_aracnido", "Máscara de Héroe Arácnido", AvatarCategory.ROSTRO, 300, false, "aracnido", "Telaraña roja y lentes grandes."),
        AvatarItem("rostro_kuromi", "Máscara de Kuromi", AvatarCategory.ROSTRO, 400, false, "kuromi", "Capucha negra con calaverita rosa."),
        AvatarItem("rostro_parche_pirata", "Parche Pirata", AvatarCategory.ROSTRO, 40, false, "parche_pirata", "Un ojo tapado y mil aventuras."),
        AvatarItem("rostro_gafas_thug", "Gafas de Sol de Píxeles (Thug Life)", AvatarCategory.ROSTRO, 80, false, "gafas_thug", "Lentes cuadrados con onda callejera."),

        // ----------------------------- ESPALDA --------------------------
        AvatarItem("espalda_mochila", "Mochila de Zazili", AvatarCategory.ESPALDA, 0, true, "mochila", "Lleva el lunch y las ideas."),
        AvatarItem("espalda_mochila_taqueria", "Mochila de Taquería", AvatarCategory.ESPALDA, 90, false, "mochila_taqueria", "Siempre trae salsa."),
        AvatarItem("espalda_mochila_ajolote", "Mochila de Ajolote", AvatarCategory.ESPALDA, 110, false, "mochila_ajolote", "Con branquias de peluche."),
        AvatarItem("espalda_mochila_peluche", "Mochila de Peluche", AvatarCategory.ESPALDA, 120, false, "mochila_peluche", "Suavecita, con orejitas."),
        AvatarItem("espalda_alas_mariposa", "Alas de Mariposa", AvatarCategory.ESPALDA, 120, false, "alas_mariposa", "Vuelan despacito."),
        AvatarItem("espalda_mochila_arcoiris", "Mochila Arcoíris", AvatarCategory.ESPALDA, 140, false, "mochila_arcoiris", "Con arco y nubes bordadas."),
        AvatarItem("espalda_capa_flores", "Capa de Cempasúchil", AvatarCategory.ESPALDA, 160, false, "capa_flores", "Cae como pétalos al viento."),
        AvatarItem("espalda_capa_mariachi", "Capa de Mariachi", AvatarCategory.ESPALDA, 180, false, "capa_mariachi", "Para el último bolero."),
        AvatarItem("espalda_capa_heroe", "Capa de Superhéroe", AvatarCategory.ESPALDA, 200, false, "capa_heroe", "Ondea aunque no haya viento."),
        AvatarItem("espalda_mochila_astro", "Mochila de Astronauta", AvatarCategory.ESPALDA, 220, false, "mochila_astro", "Oxígeno para soñar alto."),
        AvatarItem("espalda_capa_mago", "Capa de Mago Escolar", AvatarCategory.ESPALDA, 250, false, "capa_mago", "Estrellada, para los hechizos."),
        AvatarItem("espalda_alas_alebrije", "Alas de Alebrije", AvatarCategory.ESPALDA, 280, false, "alas_alebrije", "Plumas de mil colores."),
        AvatarItem("espalda_alas_robot", "Alas de Robot", AvatarCategory.ESPALDA, 300, false, "alas_robot", "Turbinas de luz azul."),
        AvatarItem("espalda_alas_quetzal", "Alas de Quetzal", AvatarCategory.ESPALDA, 350, false, "alas_quetzal", "Cola larga de plumas verdes."),
        AvatarItem("espalda_alas_hada", "Alas de Hada Mágicas", AvatarCategory.ESPALDA, 450, false, "alas_hada", "Translúcidas, con polvo de estrellas.", setOf(CapaAvatar.ESPALDA)),
        AvatarItem("espalda_capa_vampiro", "Capa de Vampiro", AvatarCategory.ESPALDA, 150, false, "capa_vampiro", "Cuello alto y forro color sangre.")
    )

    private val mapa: Map<String, AvatarItem> = todos.associateBy { it.id }

    fun porId(id: String): AvatarItem? = mapa[id]

    fun porCategoria(categoria: AvatarCategory): List<AvatarItem> =
        todos.filter { it.category == categoria }

    /** Piezas que participan en una pasada de dibujo, ordenadas por su Z-index. */
    fun porCapa(capa: CapaAvatar): List<AvatarItem> =
        todos.filter { it.ocupa(capa) }.sortedBy { it.zInferior }

    /**
     * Piezas con las que arranca el avatar. La ranura [AvatarCategory.CABEZA]
     * queda deliberadamente vacía: el bug del "sombrero verde" venía de
     * equipar la primera gorra gratis sin que el niño la eligiera. Ahora toda
     * cabeza es una decisión explícita.
     */
    fun predeterminados(): List<AvatarItem> =
        AvatarCategory.entries
            .filter { it != AvatarCategory.CABEZA }
            .mapNotNull { categoria ->
                porCategoria(categoria).firstOrNull { it.isUnlockedByDefault }
            }

    fun predeterminado(categoria: AvatarCategory): AvatarItem? =
        if (categoria == AvatarCategory.CABEZA) {
            null
        } else {
            porCategoria(categoria).firstOrNull { it.isUnlockedByDefault }
        }

    fun estaDesbloqueado(item: AvatarItem, desbloqueados: Set<String>): Boolean =
        item.isUnlockedByDefault || item.id in desbloqueados

    /** Traduce la selección persistida (ranura → id) a un equipo renderizable. */
    fun equipamientoDesde(seleccion: Map<AvatarCategory, String>): EquipamientoAvatar {
        var equipo = EquipamientoAvatar()
        AvatarCategory.entries.forEach { categoria ->
            val item = seleccion[categoria]?.let(::porId)
            if (item != null && item.category == categoria) {
                equipo = equipo.con(item)
            }
        }
        return equipo
    }

    /**
     * Variante del renderizador pixel: acepta la lista de ids equipados sin
     * importar el orden. Si dos piezas comparten ranura, gana la última.
     */
    fun equipamientoDesdeIds(ids: Collection<String>): EquipamientoAvatar {
        var equipo = EquipamientoAvatar()
        ids.forEach { id ->
            porId(id)?.let { item -> equipo = equipo.con(item) }
        }
        return equipo
    }

    /** Equipo por defecto, usado mientras DataStore entrega la primera lectura. */
    fun equipamientoPredeterminado(): EquipamientoAvatar =
        predeterminados().fold(EquipamientoAvatar()) { equipo, item -> equipo.con(item) }

    /** Nombre del sprite pixel de una pieza, sin extensión ni sufijo de capa. */
    fun spriteDe(item: AvatarItem): String = item.spriteDrawable
}
