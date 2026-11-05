package com.example.zazili.domain.minijuegos

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.example.zazili.domain.modulos.ModuloZazili

/**
 * Los cuatro módulos jugables de Zazili 2.0. Cada uno se pinta en su propia
 * pestaña de la barra inferior y comparte el mismo motor de progreso.
 */
@Immutable
sealed class ModuloMinijuego(
    val titulo: String,
    val escenario: String,
    val lema: String,
    val moduloZazili: ModuloZazili
) {
    data object Matematicas : ModuloMinijuego(
        titulo = "Matemáticas",
        escenario = "El Tianguis Numérico",
        lema = "Cuentas con tacos, elotes y roscas",
        moduloZazili = ModuloZazili.TIANGUIS_NUMERICO
    )

    data object Lenguaje : ModuloMinijuego(
        titulo = "Lenguaje",
        escenario = "La Biblioteca del Nahual",
        lema = "Sílabas, cuentos y un tlacuache rapero",
        moduloZazili = ModuloZazili.CUEVA_LEYENDAS
    )

    data object Emociones : ModuloMinijuego(
        titulo = "Emociones",
        escenario = "El Jardín de los Espejos",
        lema = "Sentir, acompañar y decidir con calma",
        moduloZazili = ModuloZazili.ARBOL_ABRAZOS
    )

    data object Ciencias : ModuloMinijuego(
        titulo = "Ciencias",
        escenario = "El Laboratorio del Cenote",
        lema = "Agua, materia y ecosistemas",
        moduloZazili = ModuloZazili.LABORATORIO_CENOTE
    )

    companion object {
        val todos: List<ModuloMinijuego> = listOf(
            Matematicas,
            Lenguaje,
            Emociones,
            Ciencias
        )

        fun para(moduloZazili: ModuloZazili): ModuloMinijuego? =
            todos.firstOrNull { it.moduloZazili == moduloZazili }
    }
}

/** Una consigna concreta para un nivel de dificultad. */
@Immutable
data class NivelMinijuego(
    val nivel: Int,
    val consigna: String
)

private fun nivelesDe(vararg consignas: String): List<NivelMinijuego> =
    consignas.mapIndexed { indice, texto -> NivelMinijuego(indice + 1, texto) }

/**
 * Catálogo cerrado de los 16 minijuegos. Cada juego declara su módulo, su
 * mecánica y las cinco consignas de dificultad; el motor de lecciones puede
 * encolar micro-rondas de cualquiera de ellos sin conocer su implementación.
 */
@Immutable
sealed class Minijuego(
    val id: String,
    val titulo: String,
    val modulo: ModuloMinijuego,
    val mecanica: String,
    val niveles: List<NivelMinijuego>,
    val esJefe: Boolean = false
) {
    val ruta: String get() = "juego/$id"

    fun nivel(nivel: Int): NivelMinijuego =
        niveles.firstOrNull { it.nivel == nivel } ?: niveles.first()

    // ------------------------- MATEMÁTICAS -------------------------

    data object CalculoMental : Minijuego(
        id = "calculo_mental",
        titulo = "Cálculo Mental",
        modulo = ModuloMinijuego.Matematicas,
        mecanica = "Cuentas rápidas contra un reloj suave",
        niveles = nivelesDe(
            "Sumas visuales del 1 al 10",
            "Sumas y restas de dos cifras",
            "Multiplicaciones y divisiones",
            "Miles y operaciones grandes",
            "Decimales y operaciones mezcladas"
        )
    )

    data object FraccionesFestivas : Minijuego(
        id = "fracciones_festivas",
        titulo = "Fracciones Festivas",
        modulo = ModuloMinijuego.Matematicas,
        mecanica = "Parte piñatas y roscas con arrastrar y soltar",
        niveles = nivelesDe(
            "Mitades y cuartos",
            "Tercios y sextos",
            "Fracciones equivalentes",
            "Sumar fracciones con el mismo denominador",
            "Sumar fracciones con distinto denominador"
        )
    )

    data object GeometriaAncestral : Minijuego(
        id = "geometria_ancestral",
        titulo = "Geometría Ancestral",
        modulo = ModuloMinijuego.Matematicas,
        mecanica = "Acomoda piezas de tangram en el lienzo",
        niveles = nivelesDe(
            "Reconocer figuras básicas",
            "Construir figuras complejas",
            "Contar perímetros",
            "Calcular áreas",
            "Armar cuerpos en 3D"
        )
    )

    data object EstadisticaAlebrije : Minijuego(
        id = "estadistica_alebrije",
        titulo = "Estadística Alebrije",
        modulo = ModuloMinijuego.Matematicas,
        mecanica = "Atrapa datos y clasifícalos en su bote",
        niveles = nivelesDe(
            "Seguro, probable o imposible",
            "Probabilidad con fracciones sencillas",
            "Leer una gráfica de barras",
            "Media, mediana y moda",
            "Comparar datos y sacar conclusiones"
        )
    )

    // ------------------------- LENGUAJE -------------------------

    data object CodiceAnimado : Minijuego(
        id = "codice_animado",
        titulo = "Códice Animado",
        modulo = ModuloMinijuego.Lenguaje,
        mecanica = "Completa la historia y despierta al jaguar",
        niveles = nivelesDe(
            "Sustantivos de todos los días",
            "Animales y palabras del campo",
            "Verbos y personajes de leyenda",
            "Vocabulario del códice y la naturaleza",
            "Palabras precisas y científicas"
        )
    )

    data object PuenteOraciones : Minijuego(
        id = "puente_oraciones",
        titulo = "Puente de Oraciones",
        modulo = ModuloMinijuego.Lenguaje,
        mecanica = "Ordena palabras para cruzar el río",
        niveles = nivelesDe(
            "Sujeto y verbo",
            "Agregar adjetivos",
            "Agregar adverbios",
            "Tiempos verbales complejos",
            "Conjunciones y oraciones largas"
        )
    )

    data object Cuentacuentos : Minijuego(
        id = "cuentacuentos",
        titulo = "Cuentacuentos",
        modulo = ModuloMinijuego.Lenguaje,
        mecanica = "Lee y elige lo que pasa después",
        niveles = nivelesDe(
            "Recordar lo que pasó",
            "Ordenar los sucesos",
            "Inferir cómo se siente cada personaje",
            "Predecir el final",
            "Descubrir metáforas"
        )
    )

    data object TlacuachePoeta : Minijuego(
        id = "tlacuache_poeta",
        titulo = "Tlacuache Poeta",
        modulo = ModuloMinijuego.Lenguaje,
        mecanica = "Rima, sinónimo o antónimo a toda velocidad",
        esJefe = true,
        niveles = nivelesDe(
            "Rimas sencillas",
            "Sinónimos",
            "Antónimos",
            "Cambio de regla a media batalla",
            "Vocabulario difícil sin perder el ritmo"
        )
    )

    // ------------------------- EMOCIONES -------------------------

    data object MedidorTormentas : Minijuego(
        id = "medidor_tormentas",
        titulo = "Medidor de Tormentas",
        modulo = ModuloMinijuego.Emociones,
        mecanica = "Elige el primer auxilio psicológico correcto",
        niveles = nivelesDe(
            "Reconocer enojo y tristeza",
            "Elegir la herramienta adecuada",
            "Celos y ansiedad",
            "Leer el contexto social",
            "Acompañar emociones difíciles"
        )
    )

    data object CaminoJaguar : Minijuego(
        id = "camino_jaguar",
        titulo = "El Camino del Jaguar",
        modulo = ModuloMinijuego.Emociones,
        mecanica = "Decisiones con consecuencia y eco",
        niveles = nivelesDe(
            "Reglas claras de convivencia",
            "Compartir y empatía",
            "Presión de los amigos",
            "Verdades piadosas",
            "Dilemas morales con matices"
        )
    )

    data object EspejoFortalezas : Minijuego(
        id = "espejo_fortalezas",
        titulo = "El Espejo de Fortalezas",
        modulo = ModuloMinijuego.Emociones,
        mecanica = "Convierte una etiqueta en superpoder",
        niveles = nivelesDe(
            "Conocerme mejor",
            "Ver el otro lado de una etiqueta",
            "Reformular lo que digo de mí",
            "Reconocer fortalezas en equipo",
            "Reframe avanzado con orgullo"
        )
    )

    data object SombraMiedo : Minijuego(
        id = "sombra_miedo",
        titulo = "La Sombra del Miedo",
        modulo = ModuloMinijuego.Emociones,
        mecanica = "Pasos valientes para achicar al monstruo",
        niveles = nivelesDe(
            "Un miedo pequeñito",
            "Primeros pasos con respiración",
            "Un miedo mediano",
            "Exposición gradual",
            "Enfrentar el miedo grande con plan"
        )
    )

    // ------------------------- CIENCIAS -------------------------

    data object AlquimiaCenote : Minijuego(
        id = "alquimia_cenote",
        titulo = "Alquimia del Cenote",
        modulo = ModuloMinijuego.Ciencias,
        mecanica = "Calor y frío para cambiar el estado del agua",
        niveles = nivelesDe(
            "Sólido, líquido y gas",
            "Evaporación y condensación",
            "Mezclas y temperatura",
            "Cambios encadenados",
            "Reacciones físicas de varios pasos"
        )
    )

    data object CadenasAlimenticias : Minijuego(
        id = "cadenas_alimenticias",
        titulo = "Cadenas Alimenticias",
        modulo = ModuloMinijuego.Ciencias,
        mecanica = "Conecta seres vivos en una red trófica",
        niveles = nivelesDe(
            "Sol, planta y herbívoro",
            "Dos consumidores en cadena",
            "Una red con varios caminos",
            "Depredadores tope",
            "Descomponedores y redes complejas"
        )
    )

    data object FiltroMagico : Minijuego(
        id = "filtro_magico",
        titulo = "Filtro Mágico",
        modulo = ModuloMinijuego.Ciencias,
        mecanica = "Apila capas para limpiar el agua",
        niveles = nivelesDe(
            "Tres capas básicas",
            "Cuatro capas y más tierra",
            "Elegir el orden correcto",
            "Cinco capas con arena fina",
            "Seis capas con contaminantes químicos"
        )
    )

    data object MonstruoBasura : Minijuego(
        id = "monstruo_basura",
        titulo = "Monstruo Basura",
        modulo = ModuloMinijuego.Ciencias,
        mecanica = "Clasifica basura que cae, rapidito",
        niveles = nivelesDe(
            "Tres botes sin prisa",
            "Más velocidad",
            "Aparecen distractores",
            "Todo vuela rápido",
            "Caos tóxico a toda máquina"
        )
    )

    companion object {
        const val NIVEL_MINIMO = 1
        const val NIVEL_MAXIMO = 5

        val todos: List<Minijuego> = listOf(
            CalculoMental,
            FraccionesFestivas,
            GeometriaAncestral,
            EstadisticaAlebrije,
            CodiceAnimado,
            PuenteOraciones,
            Cuentacuentos,
            TlacuachePoeta,
            MedidorTormentas,
            CaminoJaguar,
            EspejoFortalezas,
            SombraMiedo,
            AlquimiaCenote,
            CadenasAlimenticias,
            FiltroMagico,
            MonstruoBasura
        )

        fun porId(id: String): Minijuego? = todos.firstOrNull { it.id == id }

        fun porModulo(modulo: ModuloMinijuego): List<Minijuego> =
            todos.filter { it.modulo == modulo }

        fun porModuloZazili(modulo: ModuloZazili): List<Minijuego> =
            ModuloMinijuego.para(modulo)?.let(::porModulo).orEmpty()

        fun jefeDe(modulo: ModuloMinijuego): Minijuego? =
            todos.firstOrNull { it.modulo == modulo && it.esJefe }
    }
}

/**
 * Reglas compartidas: avanzar ronda, sumar puntos, quitar vidas y terminar con
 * una ronda extra o al quedarse sin vidas. No hay pantallas de derrota: al
 * terminar, cada juego decide cómo invitar a seguir practicando.
 *
 * [Stable] confirma que sus funciones son puras y sus constantes inmutables:
 * ninguna pantalla necesita recomponerse por tener una referencia al motor.
 */
@Stable
object MinigameEngine {
    const val RONDAS_POR_NIVEL = 8
    const val VIDAS_INICIALES = 3

    fun estadoInicial(
        minijuego: Minijuego,
        difficultyLevel: Int = Minijuego.NIVEL_MINIMO,
        totalRondas: Int = RONDAS_POR_NIVEL,
        vidas: Int = VIDAS_INICIALES
    ): MinigameState {
        require(totalRondas > 0) { "Una partida necesita al menos una ronda" }
        require(vidas > 0) { "Una partida necesita al menos una vida" }
        return MinigameState(
            minijuego = minijuego,
            difficultyLevel = difficultyLevel.coerceIn(
                Minijuego.NIVEL_MINIMO,
                Minijuego.NIVEL_MAXIMO
            ),
            totalRondas = totalRondas,
            vidas = vidas
        )
    }

    /** Acierto: sube racha, regala puntos y avanza la ronda. */
    fun registrarAcierto(
        estado: MinigameState,
        segundosRestantes: Int = 0
    ): MinigameState {
        if (estado.terminado) return estado
        val racha = estado.rachaActual + 1
        val bonusDeAire = (segundosRestantes / 2).coerceAtLeast(0)
        val puntos = estado.puntos +
            10 * estado.difficultyLevel +
            racha * 2 +
            bonusDeAire
        return avanzar(
            estado.copy(
                aciertos = estado.aciertos + 1,
                rachaActual = racha,
                mejorRacha = maxOf(estado.mejorRacha, racha),
                puntos = puntos
            )
        )
    }

    /** Fallo: corta la racha, gasta una vida y avanza la ronda. */
    fun registrarFallo(estado: MinigameState): MinigameState {
        if (estado.terminado) return estado
        return avanzar(
            estado.copy(
                errores = estado.errores + 1,
                vidas = (estado.vidas - 1).coerceAtLeast(0),
                rachaActual = 0
            )
        )
    }

    fun reiniciar(
        estado: MinigameState,
        difficultyLevel: Int = estado.difficultyLevel,
        vidas: Int = VIDAS_INICIALES
    ): MinigameState = estadoInicial(
        minijuego = estado.minijuego,
        difficultyLevel = difficultyLevel,
        totalRondas = estado.totalRondas,
        vidas = vidas
    )

    fun subirDificultad(estado: MinigameState): MinigameState = reiniciar(
        estado = estado,
        difficultyLevel = (estado.difficultyLevel + 1).coerceAtMost(Minijuego.NIVEL_MAXIMO)
    )

    /** Reloj suave: más aire en los niveles bajos, nunca castigos bruscos. */
    fun segundosParaNivel(nivel: Int): Int = when (nivel.coerceIn(1, 5)) {
        1 -> 22
        2 -> 20
        3 -> 18
        4 -> 16
        else -> 14
    }

    private fun avanzar(estado: MinigameState): MinigameState {
        val siguiente = estado.rondaActual + 1
        val termino = siguiente > estado.totalRondas || estado.vidas <= 0
        return estado.copy(
            rondaActual = siguiente,
            terminado = termino
        )
    }
}
