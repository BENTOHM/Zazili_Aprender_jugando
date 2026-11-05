package com.example.zazili.domain.modulos

/**
 * Los 6 módulos educativos de Zazili 2.0. Cada uno tendrá ruta de Lecciones
 * (teoría interactiva estilo Duolingo) y zona de Minijuegos con jefe final.
 * [fraseConstruccion] es el aviso con humor de cuarta pared mientras llega el
 * contenido de su ronda.
 */
enum class ModuloZazili(
    val titulo: String,
    val lema: String,
    val fraseConstruccion: String,
    val esPanelAdultos: Boolean = false
) {
    TIANGUIS_NUMERICO(
        titulo = "El Tianguis Numérico",
        lema = "Matemáticas con tacos, elotes y roscas",
        fraseConstruccion = "Los costales siguen atorados en la caseta… digo, en la aduana del saber."
    ),
    CUEVA_LEYENDAS(
        titulo = "La Cueva de las Leyendas",
        lema = "Sílabas, lectura y leyendas mexicanas",
        fraseConstruccion = "La Llorona está barriendo la cueva. Abre en cuanto seque el piso."
    ),
    ARBOL_ABRAZOS(
        titulo = "El Árbol de los Abrazos",
        lema = "Emociones, empatía y calma",
        fraseConstruccion = "El árbol está abrazando a alguien más. Espera tu turno, hay abrazos para todos."
    ),
    LABORATORIO_CENOTE(
        titulo = "El Laboratorio del Cenote",
        lema = "Agua, materia y ecosistemas",
        fraseConstruccion = "El cenote está en mantenimiento. Hasta los cenotes se lavan la cara."
    ),
    PIRAMIDE_CODIGO(
        titulo = "La Pirámide del Código",
        lema = "Lógica, loops y seguridad digital",
        fraseConstruccion = "El Gran Glitch pintó la pirámide de gris. Ya mandamos por thinner."
    ),
    NIDO_ADULTOS(
        titulo = "El Nido de los Grandes",
        lema = "Panel para papás y maestros",
        fraseConstruccion = "El Nido abre con PIN. El conserje apenas está llegando, dénle chance.",
        esPanelAdultos = true
    )
}
