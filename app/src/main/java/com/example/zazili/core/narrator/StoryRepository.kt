package com.example.zazili.core.narrator

import androidx.annotation.RawRes
import com.example.zazili.R

/**
 * Un cuento del Cuentacuentos.
 *
 * [bgmResId] es la pista de fondo que acompaña la lectura. Solo se asignan
 * pistas musicales de `res/raw`; nunca efectos de interfaz ni sonidos de
 * animales.
 */
data class HistoriaCuentacuentos(
    val id: String,
    val titulo: String,
    val origen: String,
    val emoji: String,
    val resumen: String,
    val parrafos: List<String>,
    val datoCurioso: String,
    val moraleja: String,
    val pregunta: String,
    val respuesta: String,
    @param:RawRes val bgmResId: Int
) {
    /** Texto plano que consume el motor de voz, con pausas naturales. */
    fun textoParaLeer(): String = buildString {
        append(titulo)
        append(". ")
        parrafos.forEach { parrafo ->
            append(parrafo)
            append(' ')
        }
        append(datoCurioso)
        append(' ')
        append(moraleja)
    }
}

/**
 * Treinta historias para leer en familia: seis de México, seis de España, seis
 * de Perú, seis de República Dominicana y seis de Brasil.
 *
 * Se mezclan historia real sin temas controversiales, leyendas tradicionales,
 * curiosidades culturales y relatos populares. Los textos evitan símbolos
 * raros para que el motor de voz los pronuncie con claridad.
 */
object StoryRepository {

    val todas: List<HistoriaCuentacuentos> = listOf(

        // -------------------------------------------------------------------
        //  MÉXICO
        // -------------------------------------------------------------------

        HistoriaCuentacuentos(
            id = "cabezas_olmecas",
            titulo = "El misterio de las cabezas olmecas",
            origen = "Tabasco y Veracruz, México",
            emoji = "🗿",
            resumen = "Gigantes de piedra que llevan miles de años mirando al trópico.",
            parrafos = listOf(
                "Hace más de dos mil años, en las tierras cálidas del sur de México, " +
                    "vivió la cultura olmeca. Fue una de las primeras grandes " +
                    "civilizaciones de Mesoamérica y dejó un tesoro muy peculiar: " +
                    "cabezas de piedra del tamaño de un automóvil.",
                "Los escultores olmecas tomaron enormes bloques de basalto, una roca " +
                    "volcánica muy dura, y tallaron rostros con narices anchas, labios " +
                    "gruesos y cascos ajustados. Algunas cabezas miden más de dos metros " +
                    "y pesan varias toneladas.",
                "El misterio es cómo las movieron. El basalto se encuentra en la sierra " +
                    "de los Tuxtlas, lejos de los sitios donde fueron colocadas las " +
                    "cabezas. Sin ruedas ni animales de carga, los olmecas las " +
                    "trasladaron con troncos, cuerdas y muchos brazos trabajando juntos.",
                "Hoy se pueden visitar en museos de Tabasco y Veracruz. Cada rostro es " +
                    "distinto, como si fueran retratos de gobernantes o de personas muy " +
                    "importantes para su comunidad."
            ),
            datoCurioso = "La cabeza olmeca más grande que se conoce mide unos 2.5 metros " +
                "de alto y pesa alrededor de 25 toneladas.",
            moraleja = "Cuando un pueblo trabaja en equipo, puede mover montañas, " +
                "literalmente.",
            pregunta = "¿De qué material están hechas las cabezas olmecas?",
            respuesta = "De basalto, una roca volcánica muy dura.",
            bgmResId = R.raw.bgm_original
        ),

        HistoriaCuentacuentos(
            id = "ajolote_xochimilco",
            titulo = "El ajolote que sonríe en Xochimilco",
            origen = "Ciudad de México, México",
            emoji = "🦎",
            resumen = "Un pequeño dragón mexicano que puede volver a crecer.",
            parrafos = listOf(
                "En los canales de Xochimilco, al sur de la Ciudad de México, vive un " +
                    "animalito muy especial: el ajolote. Tiene una carita que parece " +
                    "sonreír, branquias rosadas como plumas y una cola que lo ayuda a " +
                    "nadar despacio.",
                "Hace muchos años, los antiguos mexicas admiraban al ajolote. Lo " +
                    "llamaban axolotl, una palabra náhuatl relacionada con el agua y con " +
                    "un ser que se transforma. Lo veían como un animal valiente y " +
                    "curioso.",
                "El ajolote tiene un poder increíble: puede regenerar partes de su " +
                    "cuerpo. Si pierde una pata o un pedacito de su cola, le vuelve a " +
                    "crecer. Por eso los científicos lo estudian con mucho respeto.",
                "Hoy el ajolote está en peligro porque su hogar se ha contaminado. En " +
                    "Xochimilco, muchas familias cuidan las chinampas y limpian el agua " +
                    "para que este pequeño dragón mexicano siga sonriendo bajo las " +
                    "flores."
            ),
            datoCurioso = "Los ajolotes respiran por la piel y por las branquias. Pueden " +
                "vivir toda su vida bajo el agua sin salir a la superficie.",
            moraleja = "Cuidar el agua es cuidar a los animales que la necesitan.",
            pregunta = "¿Qué parte del cuerpo puede volver a crecer el ajolote?",
            respuesta = "Puede regenerar sus patas y partes de su cola.",
            bgmResId = R.raw.bgm_ajolote
        ),

        HistoriaCuentacuentos(
            id = "colibri_mensajero",
            titulo = "El colibrí que llevaba mensajes",
            origen = "Puebla, México",
            emoji = "🐦",
            resumen = "Una leyenda sobre las palabras amables que viajan lejos.",
            parrafos = listOf(
                "Cuenta una leyenda de los pueblos de Puebla que el colibrí no siempre " +
                    "fue tan pequeño. Antes era un ave veloz y brillante que llevaba " +
                    "mensajes entre las montañas.",
                "Un día, el colibrí vio a una niña que extrañaba a su abuela, que vivía " +
                    "muy lejos. La niña le pidió que llevara un mensaje de amor hasta su " +
                    "casa. El colibrí voló sin descansar, cruzó ríos y bosques.",
                "Cuando llegó, la abuela escuchó el mensaje y sonrió. Como " +
                    "agradecimiento, le regaló al colibrí un polvo de colores. Desde " +
                    "entonces, sus plumas brillan como gotas de luz.",
                "Dicen que cuando un colibrí se acerca a tu ventana, trae un saludo de " +
                    "alguien que te quiere. Por eso hay que recibirlo con calma y no " +
                    "espantarlo."
            ),
            datoCurioso = "El colibrí puede batir sus alas más de cincuenta veces por " +
                "segundo y volar hacia atrás.",
            moraleja = "Las palabras amables pueden viajar muy lejos y alegrar un " +
                "corazón.",
            pregunta = "¿Qué mensaje llevó el colibrí?",
            respuesta = "Llevó un mensaje de amor de una niña a su abuela.",
            bgmResId = R.raw.bgm_ninos
        ),

        HistoriaCuentacuentos(
            id = "cempasuchil_camino",
            titulo = "La flor que guía el camino",
            origen = "Estado de México y Morelos, México",
            emoji = "🌼",
            resumen = "El cempasúchil y su papel en el Día de Muertos.",
            parrafos = listOf(
                "Cada otoño, en muchas casas de México aparecen flores anaranjadas que " +
                    "llenan el aire de un olor dulce. Son cempasúchiles, la flor de los " +
                    "muertos.",
                "Su nombre viene del náhuatl cempoalxóchitl, que quiere decir flor de " +
                    "veinte pétalos. Los antiguos mexicas ya la usaban en sus ceremonias " +
                    "para recordar a sus seres queridos.",
                "Durante el Día de Muertos, las familias hacen caminos de pétalos desde " +
                    "la puerta hasta la ofrenda. También ponen fotografías, comida y " +
                    "velas. La idea es recordar con cariño a quienes ya no están.",
                "El cempasúchil necesita mucha luz y agua para crecer. En varios pueblos, " +
                    "los campesinos lo siembran en agosto y lo cosechan justo antes de la " +
                    "celebración."
            ),
            datoCurioso = "El cempasúchil también se usa para pintar telas y para hacer " +
                "remedios naturales.",
            moraleja = "Recordar a quienes queremos es una forma de mantenerlos cerca.",
            pregunta = "¿Qué significa el nombre cempoalxóchitl?",
            respuesta = "Significa flor de veinte pétalos.",
            bgmResId = R.raw.bgm_cempasuchil
        ),

        HistoriaCuentacuentos(
            id = "alebrijes_pedro_linares",
            titulo = "Los alebrijes de Pedro Linares",
            origen = "Ciudad de México, México",
            emoji = "🎨",
            resumen = "Cómo un sueño lleno de animales extraños se volvió arte.",
            parrafos = listOf(
                "En la Ciudad de México, hace muchos años, vivía un artesano llamado " +
                    "Pedro Linares. Hacía máscaras y figuras de cartón para las fiestas " +
                    "del barrio.",
                "Un día se enfermó y soñó con animales extraños: un burro con alas, un " +
                    "gallo con cuernos y un perro con cola de dragón. Todos gritaban una " +
                    "palabra inventada: alebrije.",
                "Cuando se recuperó, Pedro comenzó a dar forma a esos animales con " +
                    "cartón, papel y pintura. Los llenó de colores y patrones. La gente " +
                    "quedó asombrada y pronto le pidieron muchos más.",
                "Hoy los alebrijes son famosos en todo el mundo. Se hacen de madera en " +
                    "Oaxaca y de cartón en la Ciudad de México. Cada uno es único, como " +
                    "si hubiera salido de un sueño."
            ),
            datoCurioso = "Los alebrijes de cartón se construyen con tiras de papel y " +
                "engrudo, capa por capa.",
            moraleja = "Los sueños y la imaginación pueden convertirse en arte.",
            pregunta = "¿Qué palabra gritaban los animales del sueño de Pedro Linares?",
            respuesta = "Gritaban la palabra alebrije.",
            bgmResId = R.raw.bgm_alebrijes
        ),

        HistoriaCuentacuentos(
            id = "juego_pelota_mesoamerica",
            titulo = "El juego de pelota de los antiguos mexicanos",
            origen = "Veracruz y Yucatán, México",
            emoji = "⚽",
            resumen = "Un deporte de hace tres mil años que usaba una pelota de hule.",
            parrafos = listOf(
                "Hace más de tres mil años, en Mesoamérica se jugaba un deporte muy " +
                    "distinto al futbol. Se llamaba juego de pelota y se practicaba en " +
                    "canchas con forma de letra I.",
                "La pelota era de hule, un material que los pueblos de México " +
                    "descubrieron antes que nadie en el mundo. Pesaba más que una pelota " +
                    "actual y botaba muy alto.",
                "Los jugadores se golpeaban la pelota con las caderas, los codos y las " +
                    "rodillas. No podían usar las manos ni los pies. Tenían que " +
                    "mantenerla en movimiento y pasarla por unos aros de piedra.",
                "El juego tenía un significado importante: representaba el movimiento " +
                    "del sol y las estrellas. Hoy podemos ver canchas antiguas en " +
                    "lugares como Chichén Itzá y El Tajín."
            ),
            datoCurioso = "Los aros de piedra de las canchas eran tan pequeños que meter " +
                "la pelota era un logro enorme.",
            moraleja = "Los juegos también cuentan la historia de un pueblo.",
            pregunta = "¿Con qué partes del cuerpo se golpeaba la pelota?",
            respuesta = "Con las caderas, los codos y las rodillas.",
            bgmResId = R.raw.bgm_animada
        ),

        // -------------------------------------------------------------------
        //  ESPAÑA
        // -------------------------------------------------------------------

        HistoriaCuentacuentos(
            id = "reloj_puerta_del_sol",
            titulo = "La construcción del reloj de la Puerta del Sol",
            origen = "Madrid, España",
            emoji = "🕰️",
            resumen = "El reloj que da la bienvenida al año nuevo en Madrid.",
            parrafos = listOf(
                "En el corazón de Madrid hay una plaza muy famosa llamada Puerta del " +
                    "Sol. Allí se levanta la Real Casa de Correos, un edificio de " +
                    "fachada blanca con un reloj muy especial en lo más alto.",
                "Durante mucho tiempo, ese reloj no existía. En 1866, el relojero " +
                    "suizo José Rodríguez de Losada recibió el encargo de construir uno " +
                    "para que toda la ciudad pudiera ver la hora exacta. Trabajó con " +
                    "paciencia, engranaje por engranaje, hasta terminarlo ese mismo año.",
                "Cuando el reloj se inauguró, la gente se reunió en la plaza para verlo " +
                    "funcionar. Desde entonces, cada 31 de diciembre, miles de personas " +
                    "se juntan en la Puerta del Sol para escuchar sus campanadas.",
                "La tradición dice que hay que comer doce uvas al ritmo de las doce " +
                    "campanadas: una uva por cada mes del año nuevo. Quien termina las " +
                    "doce a tiempo pide un deseo para el año que empieza."
            ),
            datoCurioso = "El reloj de la Puerta del Sol se atrasa o se adelanta tan poco " +
                "que los relojeros lo revisan con muchísimo cuidado varias veces al año.",
            moraleja = "La paciencia y el trabajo cuidadoso hacen que las cosas duren " +
                "mucho, mucho tiempo.",
            pregunta = "¿Qué se come en la Puerta del Sol al ritmo de las campanadas?",
            respuesta = "Doce uvas, una por cada mes del año nuevo.",
            bgmResId = R.raw.bgm_gato_relojero
        ),

        HistoriaCuentacuentos(
            id = "acueducto_segovia",
            titulo = "El acueducto de Segovia, piedra sobre piedra",
            origen = "Segovia, España",
            emoji = "🏛️",
            resumen = "Un puente de gigantes que llevó agua a una ciudad durante siglos.",
            parrafos = listOf(
                "En la ciudad de Segovia, en España, hay un acueducto enorme que cruza " +
                    "la plaza como si fuera un puente de gigantes. Tiene dos pisos de " +
                    "arcos y mide más de ochocientos metros.",
                "Lo construyeron los romanos hace casi dos mil años para llevar agua " +
                    "desde las montañas hasta la ciudad. El agua corría por un canal en " +
                    "la parte de arriba.",
                "Lo más asombroso es que sus piedras se sostienen sin cemento. Cada " +
                    "bloque está colocado con tanta precisión que solo se usa una fina " +
                    "capa de unión.",
                "Con el paso de los siglos, el acueducto siguió en pie. Hoy es uno de " +
                    "los monumentos más queridos de España y un recordatorio de lo que " +
                    "se puede lograr con paciencia y buen trabajo."
            ),
            datoCurioso = "El acueducto de Segovia funcionó como sistema de agua hasta el " +
                "siglo veinte.",
            moraleja = "El trabajo bien hecho puede durar muchísimos años.",
            pregunta = "¿Qué pueblo construyó el acueducto de Segovia?",
            respuesta = "Lo construyeron los romanos.",
            bgmResId = R.raw.bgm_original
        ),

        HistoriaCuentacuentos(
            id = "sant_jordi_rosa",
            titulo = "La leyenda del dragón y la rosa",
            origen = "Cataluña, España",
            emoji = "🌹",
            resumen = "San Jorge, un dragón y el origen del Día del Libro.",
            parrafos = listOf(
                "Cuenta una leyenda de Cataluña que hace mucho tiempo un dragón vivía " +
                    "cerca de un pueblo. El dragón asustaba a la gente y pedía regalos " +
                    "para dejarla tranquila.",
                "Un valiente caballero llamado Jordi decidió enfrentarlo. Con su lanza y " +
                    "mucho valor, se acercó a la cueva del dragón. Después de una larga " +
                    "batalla, el dragón cayó.",
                "De la tierra donde cayó brotó un rosal lleno de flores rojas. Jordi " +
                    "cortó una rosa y se la llevó a la princesa, como símbolo de paz y " +
                    "amistad.",
                "Por eso, cada 23 de abril, en muchas partes de España se regalan rosas " +
                    "y libros. Es un día para celebrar la lectura, la valentía y los " +
                    "buenos gestos."
            ),
            datoCurioso = "El 23 de abril se celebra el Día del Libro porque es el día en " +
                "que murieron dos grandes escritores: Cervantes y Shakespeare.",
            moraleja = "El valor verdadero sirve para proteger y compartir.",
            pregunta = "¿Qué brotó en el lugar donde cayó el dragón?",
            respuesta = "Brotó un rosal lleno de flores rojas.",
            bgmResId = R.raw.bgm_dragon
        ),

        HistoriaCuentacuentos(
            id = "molinos_mancha",
            titulo = "Los molinos de viento de La Mancha",
            origen = "Castilla-La Mancha, España",
            emoji = "🌾",
            resumen = "Gigantes de aspas blancas y el sueño de Don Quijote.",
            parrafos = listOf(
                "En las llanuras de Castilla-La Mancha se levantan molinos de viento " +
                    "con aspas blancas. Antes servían para moler trigo y convertir el " +
                    "grano en harina.",
                "Hace más de cuatrocientos años, el escritor Miguel de Cervantes imaginó " +
                    "a un personaje llamado Don Quijote. En su historia, Don Quijote " +
                    "confunde los molinos con gigantes y decide pelear contra ellos.",
                "Esa escena se volvió famosa en todo el mundo. Desde entonces, los " +
                    "molinos de La Mancha son un símbolo de la imaginación y de las " +
                    "aventuras que nacen de los libros.",
                "Hoy muchos molinos se conservan como museos. Los visitantes suben a " +
                    "mirar las aspas y aprenden cómo funcionaba la maquinaria de madera."
            ),
            datoCurioso = "El libro de Don Quijote es una de las obras más traducidas del " +
                "mundo.",
            moraleja = "La imaginación puede convertir una llanura en un mundo de " +
                "aventuras.",
            pregunta = "¿Con qué confundió Don Quijote a los molinos?",
            respuesta = "Los confundió con gigantes.",
            bgmResId = R.raw.bgm_alternativa
        ),

        HistoriaCuentacuentos(
            id = "fallas_valencia",
            titulo = "Las Fallas de Valencia",
            origen = "Valencia, España",
            emoji = "🎆",
            resumen = "Figuras gigantes que se construyen durante meses y arden una noche.",
            parrafos = listOf(
                "Cada mes de marzo, la ciudad de Valencia se llena de figuras gigantes " +
                    "hechas de madera, cartón y pintura. Son las fallas, y pueden medir " +
                    "varios metros de alto.",
                "Los vecinos trabajan durante meses para construirlas. Cada falla cuenta " +
                    "una historia con humor y mucha creatividad. También hay música, " +
                    "desfiles y puestos de comida en las calles.",
                "Al final de la fiesta, las fallas se queman en una noche llamada la Nit " +
                    "de la Cremà. No es una despedida triste: es una manera de cerrar el " +
                    "ciclo y comenzar de nuevo.",
                "Aunque se quemen, las fallas siempre vuelven al año siguiente. Por eso " +
                    "la fiesta es un símbolo de la creatividad que se renueva."
            ),
            datoCurioso = "Algunas fallas son tan grandes que necesitan grúas para " +
                "colocarlas en la plaza.",
            moraleja = "Crear algo con esfuerzo también es una forma de celebrar.",
            pregunta = "¿Qué pasa con las fallas al final de la fiesta?",
            respuesta = "Se queman en la noche de la Cremà.",
            bgmResId = R.raw.bgm_animada
        ),

        HistoriaCuentacuentos(
            id = "alhambra_agua",
            titulo = "El agua que canta en la Alhambra",
            origen = "Granada, España",
            emoji = "⛲",
            resumen = "Un palacio donde el agua, la piedra y la geometría conversan.",
            parrafos = listOf(
                "En la ciudad de Granada hay un palacio llamado la Alhambra. Fue " +
                    "construido por los reyes nazaríes y es famoso por sus patios, sus " +
                    "arcos y sus jardines.",
                "Lo más especial de la Alhambra es el agua. Corre por fuentes, canales y " +
                    "estanques. Los constructores la usaban para refrescar el aire y " +
                    "para crear sonidos suaves.",
                "En el patio de los Arrayanes hay un estanque largo que refleja el " +
                    "cielo. En el patio de los Leones, el agua sale de una fuente " +
                    "rodeada de doce leones de piedra.",
                "Los artistas que decoraron la Alhambra llenaron las paredes de figuras " +
                    "geométricas y palabras. Cada rincón invita a detenerse y mirar con " +
                    "calma."
            ),
            datoCurioso = "La palabra Alhambra viene del árabe al-hamra, que significa la " +
                "roja, por el color de sus muros al atardecer.",
            moraleja = "Los lugares tranquilos nos enseñan a mirar con atención.",
            pregunta = "¿Cuántos leones de piedra rodean la fuente del patio?",
            respuesta = "Doce leones.",
            bgmResId = R.raw.bgm_duendes
        ),

        // -------------------------------------------------------------------
        //  PERÚ
        // -------------------------------------------------------------------

        HistoriaCuentacuentos(
            id = "puentes_incas_qeswachaca",
            titulo = "Los increíbles puentes incas de Q'eswachaca",
            origen = "Cusco, Perú",
            emoji = "🌉",
            resumen = "Un puente de hierba que se teje nuevo cada año sobre un río sagrado.",
            parrafos = listOf(
                "En los Andes peruanos, sobre el río Apurímac, existe un puente " +
                    "colgante que no está hecho de acero ni de cemento: está tejido " +
                    "con hierba. Se llama Q'eswachaca y tiene raíces en la época de " +
                    "los incas.",
                "El puente mide alrededor de treinta metros y cuelga entre dos orillas " +
                    "rocosas. Sus cables se trenzan con ichu, una planta de la punta de " +
                    "los Andes, y el piso también se teje con fibras resistentes.",
                "Cada mes de junio, las comunidades quechuas de la zona se reúnen para " +
                    "construir un puente nuevo. Cortan la hierba, la trenzan en cuerdas " +
                    "largas y, con mucho cuidado, retiran el puente viejo y tienden el " +
                    "nuevo mientras los vecinos sostienen las cuerdas desde ambas orillas.",
                "Esta tradición pasa de generación en generación: los abuelos enseñan a " +
                    "los niños a trenzar y a trabajar en equipo. En 2013, la UNESCO " +
                    "reconoció este conocimiento como patrimonio cultural inmaterial de " +
                    "la humanidad."
            ),
            datoCurioso = "Durante la reconstrucción, los constructores trabajan en el " +
                "aire sostenidos por cuerdas, como equilibristas sobre el río.",
            moraleja = "Compartir lo que sabemos con los demás mantiene vivas las " +
                "tradiciones más valiosas.",
            pregunta = "¿Cada cuándo se construye un puente nuevo en Q'eswachaca?",
            respuesta = "Cada año, en el mes de junio.",
            bgmResId = R.raw.bgm_original
        ),

        HistoriaCuentacuentos(
            id = "machu_picchu_nubes",
            titulo = "Machu Picchu, la ciudad entre las nubes",
            origen = "Cusco, Perú",
            emoji = "🏞️",
            resumen = "Una ciudad inca de piedra que aprendió a vivir con la montaña.",
            parrafos = listOf(
                "En lo alto de los Andes peruanos, entre montañas verdes y nubes, se " +
                    "encuentra Machu Picchu. Fue construida por los incas hace más de " +
                    "quinientos años.",
                "Sus casas, templos y terrazas están hechas de piedra. Los incas " +
                    "acomodaron cada bloque con gran cuidado para que resistiera lluvias " +
                    "y temblores.",
                "Machu Picchu también tenía un sistema de agua. Un canal llevaba el " +
                    "agua desde un manantial hasta las fuentes de la ciudad.",
                "Hoy es uno de los sitios arqueológicos más visitados del mundo. Los " +
                    "visitantes caminan despacio para no dañar las piedras y para " +
                    "escuchar el sonido del viento en las montañas."
            ),
            datoCurioso = "Machu Picchu significa montaña vieja en quechua.",
            moraleja = "Respetar los lugares antiguos ayuda a que sigan contando su " +
                "historia.",
            pregunta = "¿Qué significa Machu Picchu en quechua?",
            respuesta = "Montaña vieja.",
            bgmResId = R.raw.bgm_duendes
        ),

        HistoriaCuentacuentos(
            id = "lineas_nazca",
            titulo = "Las líneas de Nazca, dibujos en el desierto",
            origen = "Ica, Perú",
            emoji = "🌀",
            resumen = "Animales gigantes dibujados en la arena hace más de dos mil años.",
            parrafos = listOf(
                "En el desierto de Nazca, al sur de Perú, hay dibujos enormes hechos en " +
                    "el suelo. Algunos parecen animales: un colibrí, un mono, un perro y " +
                    "una araña.",
                "Las líneas se hicieron hace más de dos mil años. Para crearlas, los " +
                    "antiguos nazcas retiraron las piedras oscuras y dejaron al " +
                    "descubierto la arena clara.",
                "Los dibujos son tan grandes que no se ven bien desde el suelo. Por eso " +
                    "se descubrieron cuando aparecieron las primeras avionetas que " +
                    "volaban sobre el desierto.",
                "Nadie sabe con certeza para qué se hicieron. Algunos piensan que eran " +
                    "caminos para ceremonias, otros creen que señalaban el paso del agua " +
                    "o de las estrellas."
            ),
            datoCurioso = "El colibrí de Nazca mide unos noventa metros de largo.",
            moraleja = "Las preguntas sin respuesta también nos invitan a investigar.",
            pregunta = "¿Por qué los dibujos de Nazca se ven mejor desde el aire?",
            respuesta = "Porque son enormes y desde el suelo cuesta ver su forma " +
                "completa.",
            bgmResId = R.raw.bgm_alternativa
        ),

        HistoriaCuentacuentos(
            id = "condor_zorro",
            titulo = "El cóndor y el zorro",
            origen = "Ayacucho, Perú",
            emoji = "🦊",
            resumen = "Un relato andino sobre la curiosidad y los buenos consejos.",
            parrafos = listOf(
                "En los Andes se cuenta la historia de un zorro muy curioso que quería " +
                    "volar como el cóndor. Un día vio al cóndor posado en una roca y le " +
                    "pidió que le enseñara.",
                "El cóndor aceptó. Le dijo al zorro que se sujetara fuerte de sus plumas " +
                    "y que cerrara los ojos cuando subieran. El zorro prometió obedecer.",
                "Ya en el cielo, el zorro abrió los ojos y vio los campos, las casas y " +
                    "los ríos desde arriba. Se emocionó tanto que soltó una pata para " +
                    "señalar el paisaje.",
                "Por suerte, el cóndor voló bajo y lo dejó en una loma suave. El zorro " +
                    "aprendió que la curiosidad es buena, pero también hay que seguir " +
                    "las instrucciones."
            ),
            datoCurioso = "El cóndor andino puede volar más de cinco mil metros de " +
                "altura.",
            moraleja = "Escuchar los consejos ayuda a disfrutar las aventuras sin " +
                "peligro.",
            pregunta = "¿Qué prometió el zorro al cóndor?",
            respuesta = "Prometió cerrar los ojos y sujetarse fuerte.",
            bgmResId = R.raw.bgm_ninos
        ),

        HistoriaCuentacuentos(
            id = "papas_andinas",
            titulo = "Las papas que nacieron en los Andes",
            origen = "Puno, Perú",
            emoji = "🥔",
            resumen = "Miles de variedades de papa cultivadas entre montañas y frío.",
            parrafos = listOf(
                "La papa es uno de los alimentos más importantes del mundo, y nació en " +
                    "los Andes. Los pueblos del Perú antiguo la cultivaron hace miles de " +
                    "años.",
                "En las montañas hay frío, viento y poca lluvia. Aun así, los " +
                    "agricultores aprendieron a sembrar papa a distintas alturas y a " +
                    "guardar las mejores semillas.",
                "Existen cientos de variedades de papa andina. Algunas son moradas, " +
                    "otras amarillas o rojas. Tienen nombres como papa nativa, huayro o " +
                    "peruanita.",
                "Hoy la papa se cultiva en muchos países. Pero su historia comenzó en " +
                    "las alturas de los Andes, donde las familias campesinas siguen " +
                    "cuidando las semillas de sus abuelos."
            ),
            datoCurioso = "En Perú existen más de tres mil variedades de papa.",
            moraleja = "Guardar las semillas es guardar la historia de un pueblo.",
            pregunta = "¿En qué región nació la papa?",
            respuesta = "Nació en los Andes.",
            bgmResId = R.raw.bgm_animada
        ),

        HistoriaCuentacuentos(
            id = "puente_suspiros",
            titulo = "El puente de los suspiros",
            origen = "Barranco, Lima, Perú",
            emoji = "🌉",
            resumen = "Una leyenda de espera y reencuentro en un puente de madera.",
            parrafos = listOf(
                "En el barrio de Barranco, en Lima, hay un puente pequeño que cruza una " +
                    "calle. Se llama el puente de los suspiros.",
                "Cuenta la leyenda que hace mucho tiempo vivía allí una joven que se " +
                    "enamoró de un pescador. Se veían en secreto cerca del puente.",
                "Un día, el pescador tuvo que irse al mar por mucho tiempo. La joven lo " +
                    "esperó en el puente, mirando el horizonte. Dicen que suspiraba al " +
                    "pensar en su regreso.",
                "Cuando el pescador volvió, se encontraron en el mismo lugar. Desde " +
                    "entonces, la gente dice que quien cruza el puente con calma y pide " +
                    "un deseo ve cumplidos sus anhelos."
            ),
            datoCurioso = "El puente de los suspiros mide unos ocho metros y está hecho " +
                "de madera.",
            moraleja = "La esperanza y la paciencia también son formas de amor.",
            pregunta = "¿Dónde se encuentra el puente de los suspiros?",
            respuesta = "En el barrio de Barranco, en Lima.",
            bgmResId = R.raw.bgm_gato_relojero
        ),

        // -------------------------------------------------------------------
        //  REPÚBLICA DOMINICANA
        // -------------------------------------------------------------------

        HistoriaCuentacuentos(
            id = "merengue_dominicano",
            titulo = "El merengue y el tambor dominicano",
            origen = "Santo Domingo, República Dominicana",
            emoji = "🥁",
            resumen = "La música que reúne a los pueblos del Caribe.",
            parrafos = listOf(
                "En la República Dominicana, el merengue es mucho más que un baile: es " +
                    "una forma de reunir a la gente. Su música alegre se escucha en " +
                    "fiestas, calles y casas.",
                "El merengue tiene raíces africanas y europeas. Se toca con tambora, " +
                    "güira y acordeón. La tambora marca el ritmo y la güira suena como " +
                    "un rasquido brillante.",
                "Al bailar, las parejas se mueven con pasos cortos y cintura suave. " +
                    "Antes se bailaba en espacios grandes; hoy se baila en cualquier " +
                    "rincón donde haya música.",
                "Cada año, en Santo Domingo y en otros pueblos, se celebran festivales " +
                    "de merengue. La música pasa de generación en generación, de abuelos " +
                    "a niños."
            ),
            datoCurioso = "La tambora es un tambor de dos parches que se toca con una " +
                "baqueta y con la mano.",
            moraleja = "La música une a las personas y guarda la memoria de un pueblo.",
            pregunta = "¿Qué instrumentos forman el conjunto típico de merengue?",
            respuesta = "La tambora, la güira y el acordeón.",
            bgmResId = R.raw.bgm_animada
        ),

        HistoriaCuentacuentos(
            id = "cacao_dominicano",
            titulo = "El cacao que viajó por el mundo",
            origen = "Samaná y Monte Plata, República Dominicana",
            emoji = "🍫",
            resumen = "Un grano pequeño que conectó culturas y sabores.",
            parrafos = listOf(
                "El cacao es el fruto del árbol del cacao. Con sus semillas se hace el " +
                    "chocolate. En la República Dominicana se cultiva desde hace siglos.",
                "Los pueblos originarios de América ya preparaban una bebida con cacao " +
                    "molido, agua y especias. La llamaban xocolatl. Era una bebida " +
                    "especial, servida en ceremonias.",
                "Con la llegada de los europeos, el cacao viajó a otros continentes. Se " +
                    "volvió famoso en España, Francia y luego en todo el mundo.",
                "Hoy, en zonas como Samaná, los agricultores siembran cacao bajo la " +
                    "sombra de otros árboles. Así el bosque se mantiene fresco y los " +
                    "animales tienen hogar."
            ),
            datoCurioso = "El cacao dominicano es conocido por su sabor intenso y se usa " +
                "en chocolates de alta calidad.",
            moraleja = "Un pequeño grano puede conectar culturas de todo el planeta.",
            pregunta = "¿Cómo se llamaba la bebida de cacao de los pueblos originarios?",
            respuesta = "Se llamaba xocolatl.",
            bgmResId = R.raw.theme_playful_bgm
        ),

        HistoriaCuentacuentos(
            id = "faro_colon",
            titulo = "El Faro a Colón",
            origen = "Santo Domingo Este, República Dominicana",
            emoji = "🗼",
            resumen = "Un monumento en forma de cruz que ilumina la noche caribeña.",
            parrafos = listOf(
                "En Santo Domingo, la capital de la República Dominicana, hay un " +
                    "monumento muy grande llamado Faro a Colón. Tiene forma de cruz y " +
                    "se ilumina de noche con haces de luz.",
                "Se construyó para recordar el encuentro entre América y Europa. Fue " +
                    "inaugurado en 1992, quinientos años después de la llegada de " +
                    "Cristóbal Colón a las islas del Caribe.",
                "El monumento mide unos doscientos metros de largo. En su interior hay " +
                    "salas con objetos de distintos países y una cripta con un monumento " +
                    "dedicado a Colón.",
                "Desde lejos, sus luces se ven como una estrella en el cielo. Es uno de " +
                    "los lugares más visitados de Santo Domingo."
            ),
            datoCurioso = "Cuando el faro se ilumina completo, sus haces de luz pueden " +
                "verse a varios kilómetros de distancia.",
            moraleja = "Los monumentos nos ayudan a recordar y conversar sobre el pasado.",
            pregunta = "¿Qué forma tiene el Faro a Colón?",
            respuesta = "Tiene forma de cruz.",
            bgmResId = R.raw.bgm_original
        ),

        HistoriaCuentacuentos(
            id = "tres_ojos",
            titulo = "Las cuevas de los Tres Ojos",
            origen = "Santo Domingo, República Dominicana",
            emoji = "💧",
            resumen = "Lagunas escondidas bajo la roca, con agua fresca y cristalina.",
            parrafos = listOf(
                "Cerca de Santo Domingo hay un parque con cuevas y lagunas de agua " +
                    "cristalina. Se llama los Tres Ojos porque tres de sus lagunas se " +
                    "miran desde arriba como ojos abiertos.",
                "Las cuevas se formaron hace miles de años, cuando el agua disolvió la " +
                    "roca caliza. Con el tiempo se crearon pasillos, columnas y techos " +
                    "con forma de bóveda.",
                "El agua de las lagunas es fresca y muy clara. En algunas se pueden ver " +
                    "pequeños peces y plantas. Los guías cuentan leyendas de tesoros y de " +
                    "antiguos habitantes.",
                "Hoy el parque es un lugar para caminar, escuchar el eco y aprender " +
                    "sobre la naturaleza. Hay que cuidar las rocas y no dejar basura para " +
                    "que las cuevas sigan vivas."
            ),
            datoCurioso = "En una de las lagunas se filmaron escenas de películas " +
                "antiguas de aventuras.",
            moraleja = "Los lugares naturales también son un tesoro que debemos cuidar.",
            pregunta = "¿Por qué se llaman las cuevas de los Tres Ojos?",
            respuesta = "Porque tres de sus lagunas parecen ojos vistos desde arriba.",
            bgmResId = R.raw.bgm_duendes
        ),

        HistoriaCuentacuentos(
            id = "guloyas",
            titulo = "Los guloyas de San Pedro de Macorís",
            origen = "San Pedro de Macorís, República Dominicana",
            emoji = "🎭",
            resumen = "Bailarines de trajes brillantes que mantienen viva una tradición.",
            parrafos = listOf(
                "En San Pedro de Macorís, cada diciembre sale a las calles un grupo de " +
                    "bailarines con trajes llenos de colores. Son los guloyas.",
                "Los guloyas usan máscaras, capas y sombreros con espejos. Bailan al " +
                    "ritmo de tambores y cantan historias que mezclan tradiciones del " +
                    "Caribe y de otras islas.",
                "La tradición llegó con familias que vinieron de las islas vecinas hace " +
                    "más de cien años. Con el tiempo se volvió parte de la cultura " +
                    "dominicana.",
                "En 2008, la UNESCO reconoció a los guloyas como patrimonio cultural " +
                    "inmaterial. Hoy los niños aprenden los bailes para que la fiesta no " +
                    "se pierda."
            ),
            datoCurioso = "Los trajes de guloya pueden tener cientos de espejos y cintas " +
                "que brillan al bailar.",
            moraleja = "Las tradiciones se mantienen vivas cuando los niños aprenden de " +
                "los mayores.",
            pregunta = "¿En qué mes salen los guloyas a bailar?",
            respuesta = "En diciembre.",
            bgmResId = R.raw.bgm_8bits
        ),

        HistoriaCuentacuentos(
            id = "cigua_palmera",
            titulo = "La cigua palmera, ave nacional",
            origen = "República Dominicana",
            emoji = "🌴",
            resumen = "Un canto suave que vive en las palmas y en las monedas del país.",
            parrafos = listOf(
                "La cigua palmera es el ave nacional de la República Dominicana. Es " +
                    "pequeña, de color gris y tiene un canto suave que se escucha en " +
                    "bosques y palmares.",
                "Vive en las palmas y en los árboles altos. Se alimenta de insectos y " +
                    "frutos. Su nido es una pequeña copa hecha con ramitas y fibras.",
                "La cigua palmera aparece en monedas, sellos y canciones. Para muchos " +
                    "dominicanos, su canto es un sonido de casa.",
                "Sin embargo, su hogar se reduce cuando se cortan los árboles. Proteger " +
                    "los bosques y las palmas ayuda a que la cigua siga cantando."
            ),
            datoCurioso = "La cigua palmera solo vive en la isla de La Española.",
            moraleja = "Proteger el hogar de un ave es proteger el nuestro también.",
            pregunta = "¿Cuál es el ave nacional de la República Dominicana?",
            respuesta = "La cigua palmera.",
            bgmResId = R.raw.bgm_ninos
        ),

        // -------------------------------------------------------------------
        //  BRASIL
        // -------------------------------------------------------------------

        HistoriaCuentacuentos(
            id = "rio_amazonas",
            titulo = "El río Amazonas, el gigante de agua dulce",
            origen = "Amazonas, Brasil",
            emoji = "🌊",
            resumen = "El río más caudaloso del mundo y los pueblos que lo cuidan.",
            parrafos = listOf(
                "El río Amazonas es el río más caudaloso del mundo. Nace en los Andes y " +
                    "cruza América del Sur hasta llegar al océano Atlántico.",
                "Durante su camino recoge agua de cientos de afluentes. En algunos " +
                    "lugares es tan ancho que no se ve la otra orilla.",
                "En sus aguas viven delfines rosados, manatíes y miles de especies de " +
                    "peces. En sus orillas crecen árboles enormes y plantas que no " +
                    "existen en otros lugares.",
                "Los pueblos de la Amazonía han vivido allí por miles de años. Conocen " +
                    "los ciclos del río y saben cuándo sube y cuándo baja el agua."
            ),
            datoCurioso = "El Amazonas aporta cerca de una quinta parte del agua dulce " +
                "que llega a los océanos.",
            moraleja = "Los ríos grandes empiezan con muchas gotas pequeñas.",
            pregunta = "¿Dónde nace el río Amazonas?",
            respuesta = "Nace en los Andes.",
            bgmResId = R.raw.bgm_alternativa
        ),

        HistoriaCuentacuentos(
            id = "leyenda_guarana",
            titulo = "La leyenda del guaraná",
            origen = "Amazonas, Brasil",
            emoji = "🌱",
            resumen = "El regalo de un niño que se convirtió en una planta llena de energía.",
            parrafos = listOf(
                "Cuenta una leyenda del pueblo sateré-mawé que en un bosque de la " +
                    "Amazonía vivía un niño muy querido por su tribu. Era curioso y " +
                    "jugaba con los animales del lugar.",
                "Un día, el niño fue a buscar frutas y no regresó. La tribu lo buscó " +
                    "durante días. Al final, encontraron un arbusto con frutos rojos y " +
                    "semillas negras.",
                "La tribu entendió que el niño había dejado un regalo: una planta que " +
                    "daba energía y ayudaba en los días de cansancio. La llamaron " +
                    "guaraná.",
                "Desde entonces, el guaraná se usa para hacer bebidas y se comparte con " +
                    "respeto. La leyenda recuerda que los regalos de la naturaleza deben " +
                    "cuidarse."
            ),
            datoCurioso = "El guaraná tiene semillas con cafeína natural y es un cultivo " +
                "importante en la Amazonía.",
            moraleja = "La naturaleza nos da regalos que debemos usar con cuidado.",
            pregunta = "¿Qué planta apareció como regalo en la leyenda?",
            respuesta = "El guaraná.",
            bgmResId = R.raw.bgm_ninos
        ),

        HistoriaCuentacuentos(
            id = "carnaval_rio",
            titulo = "El carnaval de Río de Janeiro",
            origen = "Río de Janeiro, Brasil",
            emoji = "🎉",
            resumen = "Escuelas de samba, tambores y una alegría que se prepara todo el año.",
            parrafos = listOf(
                "El carnaval de Río de Janeiro es una de las fiestas más grandes del " +
                    "mundo. Se celebra antes de la cuaresma, con desfiles, música y " +
                    "disfraces.",
                "Las escuelas de samba preparan durante meses sus carros alegóricos y " +
                    "sus canciones. Cada escuela elige un tema y lo cuenta con " +
                    "bailarines, tambores y colores.",
                "El desfile se realiza en el sambódromo, una avenida con gradas para " +
                    "miles de personas. Los jueces evalúan la música, la coreografía y " +
                    "la creatividad.",
                "Además del desfile, en las calles hay fiestas llamadas blocos. La gente " +
                    "baila, canta y comparte. El carnaval es una explosión de alegría y " +
                    "trabajo en equipo."
            ),
            datoCurioso = "Una escuela de samba puede tener más de tres mil integrantes.",
            moraleja = "Trabajar en equipo puede crear algo espectacular.",
            pregunta = "¿Cómo se llaman las fiestas de calle del carnaval?",
            respuesta = "Se llaman blocos.",
            bgmResId = R.raw.bgm_animada
        ),

        HistoriaCuentacuentos(
            id = "capoeira_roda",
            titulo = "La capoeira y la roda",
            origen = "Salvador de Bahía, Brasil",
            emoji = "🤸",
            resumen = "Baile, música y equilibrio dentro de un círculo de amigos.",
            parrafos = listOf(
                "La capoeira es una mezcla de baile, música y lucha que nació en Brasil. " +
                    "Se practica en un círculo llamado roda.",
                "En la roda, los participantes forman un círculo y tocan instrumentos " +
                    "como el berimbau. Dos personas entran al centro y se mueven con " +
                    "giros, patadas suaves y equilibrio.",
                "La capoeira tiene raíces africanas y fue creada por comunidades que " +
                    "necesitaban expresarse con el cuerpo y la música. Con el tiempo se " +
                    "volvió un arte muy querido.",
                "Hoy se practica en escuelas de todo el mundo. Los maestros enseñan que " +
                    "la capoeira también es respeto, disciplina y alegría."
            ),
            datoCurioso = "El berimbau es un instrumento de cuerda con forma de arco que " +
                "marca el ritmo de la roda.",
            moraleja = "El movimiento y la música pueden unir a las personas.",
            pregunta = "¿Cómo se llama el círculo donde se practica la capoeira?",
            respuesta = "Se llama roda.",
            bgmResId = R.raw.theme_playful_bgm
        ),

        HistoriaCuentacuentos(
            id = "brasilia_avion",
            titulo = "Brasilia, la ciudad con forma de avión",
            origen = "Brasilia, Brasil",
            emoji = "🏙️",
            resumen = "Una capital construida desde cero en menos de cuatro años.",
            parrafos = listOf(
                "Brasilia es la capital de Brasil, pero no siempre lo fue. Antes, la " +
                    "capital era Río de Janeiro. En 1960, el gobierno decidió construir " +
                    "una ciudad nueva en el centro del país.",
                "El arquitecto Óscar Niemeyer y el urbanista Lúcio Costa diseñaron la " +
                    "ciudad con forma de avión. Las alas son barrios residenciales y el " +
                    "centro es la plaza de los Tres Poderes.",
                "Construir Brasilia fue un reto enorme. Miles de trabajadores llegaron " +
                    "de todo el país para levantar edificios blancos y avenidas anchas en " +
                    "pocos años.",
                "Hoy Brasilia es patrimonio mundial de la UNESCO. Su diseño muestra cómo " +
                    "la arquitectura puede cambiar la forma de vivir de una ciudad."
            ),
            datoCurioso = "Brasilia fue construida en menos de cuatro años.",
            moraleja = "Las ideas grandes necesitan trabajo en equipo para hacerse " +
                "realidad.",
            pregunta = "¿Qué forma tiene el plano de Brasilia?",
            respuesta = "Tiene forma de avión.",
            bgmResId = R.raw.bgm_8bits
        ),

        HistoriaCuentacuentos(
            id = "cristo_redentor",
            titulo = "El Cristo Redentor del Corcovado",
            origen = "Río de Janeiro, Brasil",
            emoji = "⛰️",
            resumen = "Una estatua de brazos abiertos sobre la ciudad y el mar.",
            parrafos = listOf(
                "En la cima del cerro Corcovado, en Río de Janeiro, se levanta una " +
                    "estatua enorme de brazos abiertos: el Cristo Redentor.",
                "La estatua mide unos treinta metros y está hecha de hormigón y piedra " +
                    "jabón. Fue terminada en 1931 después de varios años de trabajo.",
                "Para llevarla hasta la cima, los obreros usaron un pequeño tren y " +
                    "subieron materiales pieza por pieza. El lugar es tan alto que a " +
                    "veces las nubes lo cubren.",
                "Hoy el Cristo Redentor es uno de los símbolos de Brasil. Desde sus pies " +
                    "se ve la ciudad, las playas y el mar. Miles de personas lo visitan " +
                    "cada año."
            ),
            datoCurioso = "El Cristo Redentor fue elegido en 2007 como una de las nuevas " +
                "siete maravillas del mundo.",
            moraleja = "La paciencia y la cooperación permiten alcanzar metas muy altas.",
            pregunta = "¿En qué cerro se encuentra el Cristo Redentor?",
            respuesta = "En el cerro Corcovado.",
            bgmResId = R.raw.bgm_original
        )
    )

    private val mapa: Map<String, HistoriaCuentacuentos> = todas.associateBy { it.id }

    fun porId(id: String): HistoriaCuentacuentos? = mapa[id]
}
