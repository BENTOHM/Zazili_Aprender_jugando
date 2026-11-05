package com.example.zazili.domain.ciencias

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Materiales del arenero científico. Cada uno declara cómo se llama en la UI y
 * su emoji para que las pantallas no inventen etiquetas.
 */
enum class ElementoCientifico(
    val etiqueta: String,
    val emoji: String
) {
    AGUA(etiqueta = "Agua", emoji = "💧"),
    CALOR(etiqueta = "Calor", emoji = "🔥"),
    FRIO(etiqueta = "Frío", emoji = "❄️"),
    HIELO(etiqueta = "Hielo", emoji = "🧊"),
    VAPOR(etiqueta = "Vapor", emoji = "☁️"),
    SEMILLA(etiqueta = "Semilla", emoji = "🌰"),
    TIERRA(etiqueta = "Tierra", emoji = "🟤"),
    IMAN(etiqueta = "Imán", emoji = "🧲"),
    METAL(etiqueta = "Metal", emoji = "🔩"),
    METAL_ATRAIDO(etiqueta = "Metal atraído", emoji = "🧲🔩"),
    PLANTULA(etiqueta = "Plantita", emoji = "🌱"),
    BICARBONATO(etiqueta = "Bicarbonato", emoji = "🧂"),
    VINAGRE(etiqueta = "Vinagre", emoji = "🍶"),
    BURBUJAS(etiqueta = "Burbujas de gas", emoji = "🫧"),
    PLASTICO(etiqueta = "Plástico", emoji = "🧴"),
    LODO(etiqueta = "Lodo", emoji = "🟫"),
    ROCA(etiqueta = "Roca", emoji = "🪨"),
    LUZ(etiqueta = "Luz", emoji = "☀️"),
    OXIGENO(etiqueta = "Oxígeno", emoji = "💨")
}

/**
 * Regla de causa y efecto del arenero. Es simétrica: da igual cuál elemento se
 * arrastre primero, la reacción ocurre cuando los dos se encuentran.
 */
@Immutable
data class ReactionRule(
    val id: String,
    val elemento1: ElementoCientifico,
    val elemento2: ElementoCientifico,
    val yields: ElementoCientifico,
    val titulo: String,
    val explicacion: String,
    val particulas: Int = 14
) {
    fun coincide(a: ElementoCientifico, b: ElementoCientifico): Boolean =
        (a == elemento1 && b == elemento2) || (a == elemento2 && b == elemento1)
}

/** Capas del filtro, en el orden en que deben apilarse de arriba a abajo. */
enum class CapaFiltro(
    val etiqueta: String,
    val emoji: String,
    val descripcion: String
) {
    GRAVA(
        etiqueta = "Grava",
        emoji = "🪨",
        descripcion = "Atrapa las piedritas más grandes."
    ),
    ARENA(
        etiqueta = "Arena",
        emoji = "🏖️",
        descripcion = "Retiene la tierra que quedó."
    ),
    CARBON(
        etiqueta = "Carbón",
        emoji = "⬛",
        descripcion = "Absorbe olores y algunas sustancias."
    ),
    ALGODON(
        etiqueta = "Algodón",
        emoji = "🧵",
        descripcion = "Es el colador final para lo más fino."
    )
}

/** Orden correcto del Filtro Mágico: de lo más grande a lo más fino. */
val ORDEN_FILTRO: List<CapaFiltro> = listOf(
    CapaFiltro.GRAVA,
    CapaFiltro.ARENA,
    CapaFiltro.CARBON,
    CapaFiltro.ALGODON
)

@Immutable
data class EstadoSandbox(
    val elementosEnMesa: List<ElementoCientifico> = emptyList(),
    val ultimaReaccion: ReactionRule? = null,
    val reacciones: List<String> = emptyList(),
    val pasos: Int = 0
)

@Immutable
data class ResultadoSandbox(
    val regla: ReactionRule? = null,
    val producto: ElementoCientifico? = null,
    val titulo: String,
    val mensaje: String,
    val particulas: Int = 0
) {
    val huboReaccion: Boolean get() = regla != null
}

@Immutable
data class ResultadoFiltro(
    val correcto: Boolean,
    val titulo: String,
    val mensaje: String,
    val aguaLimpia: Boolean
)

/**
 * Motor puro del arenero científico.
 *
 * No conoce Compose ni Android: recibe materiales, busca una [ReactionRule] y
 * publica un [EstadoSandbox] inmutable. La pantalla solo dibuja el resultado y
 * las partículas. El mismo motor valida el orden de las capas del Filtro Mágico
 * para que el minijuego y los retos de la ruta compartan una sola verdad.
 */
class ScienceSandboxEngine {

    private val _estado = MutableStateFlow(EstadoSandbox())
    val estado: StateFlow<EstadoSandbox> = _estado.asStateFlow()

    @Synchronized
    fun colocar(elemento: ElementoCientifico) {
        _estado.update { actual ->
            if (actual.elementosEnMesa.contains(elemento)) {
                actual
            } else {
                actual.copy(
                    elementosEnMesa = actual.elementosEnMesa + elemento,
                    pasos = actual.pasos + 1
                )
            }
        }
    }

    @Synchronized
    fun combinar(
        primero: ElementoCientifico,
        segundo: ElementoCientifico
    ): ResultadoSandbox {
        if (primero == segundo) {
            _estado.update { it.copy(pasos = it.pasos + 1) }
            return ResultadoSandbox(
                titulo = "Es el mismo material",
                mensaje = "Prueba con dos materiales distintos para ver qué pasa."
            )
        }

        val regla = ReglasCiencia.buscar(primero, segundo)
        _estado.update { actual ->
            actual.copy(
                ultimaReaccion = regla,
                reacciones = if (regla == null) {
                    actual.reacciones
                } else {
                    actual.reacciones + regla.id
                },
                pasos = actual.pasos + 1
            )
        }

        if (regla == null) {
            return ResultadoSandbox(
                titulo = "No pasa nada nuevo",
                mensaje = "Estos materiales no reaccionan entre sí. Prueba otra pareja."
            )
        }

        return ResultadoSandbox(
            regla = regla,
            producto = regla.yields,
            titulo = regla.titulo,
            mensaje = regla.explicacion,
            particulas = regla.particulas
        )
    }

    /**
     * Valida una pila completa del filtro. El orden esperado siempre es
     * grava, arena, carbón y algodón: de lo grande a lo fino.
     */
    @Synchronized
    fun validarFiltro(orden: List<CapaFiltro>): ResultadoFiltro {
        val completo = orden.size == ORDEN_FILTRO.size
        val correcto = completo && orden == ORDEN_FILTRO
        return when {
            correcto -> ResultadoFiltro(
                correcto = true,
                titulo = "¡Agua limpia!",
                mensaje = "La grava atrapó lo grande, la arena y el carbón retuvieron lo fino y el algodón fue el último colador.",
                aguaLimpia = true
            )

            !completo -> ResultadoFiltro(
                correcto = false,
                titulo = "Falta una capa",
                mensaje = "El filtro necesita sus cuatro capas para funcionar.",
                aguaLimpia = false
            )

            else -> ResultadoFiltro(
                correcto = false,
                titulo = "El agua sigue turbia",
                mensaje = "Las capas quedaron en otro orden. Arriba va lo más grande y abajo lo más fino.",
                aguaLimpia = false
            )
        }
    }

    @Synchronized
    fun reiniciar() {
        _estado.value = EstadoSandbox()
    }
}

/** Catálogo único de reacciones. La UI nunca decide qué se forma. */
object ReglasCiencia {

    val reglas: List<ReactionRule> = listOf(
        ReactionRule(
            id = "evaporacion",
            elemento1 = ElementoCientifico.AGUA,
            elemento2 = ElementoCientifico.CALOR,
            yields = ElementoCientifico.VAPOR,
            titulo = "Evaporación",
            explicacion = "El calor le dio energía al agua y sus gotitas subieron como vapor."
        ),
        ReactionRule(
            id = "condensacion",
            elemento1 = ElementoCientifico.VAPOR,
            elemento2 = ElementoCientifico.FRIO,
            yields = ElementoCientifico.AGUA,
            titulo = "Condensación",
            explicacion = "El vapor se enfrió y volvió a juntarse en gotitas de agua."
        ),
        ReactionRule(
            id = "congelacion",
            elemento1 = ElementoCientifico.AGUA,
            elemento2 = ElementoCientifico.FRIO,
            yields = ElementoCientifico.HIELO,
            titulo = "Congelación",
            explicacion = "Con mucho frío, el agua se ordena y se convierte en hielo."
        ),
        ReactionRule(
            id = "derretimiento",
            elemento1 = ElementoCientifico.HIELO,
            elemento2 = ElementoCientifico.CALOR,
            yields = ElementoCientifico.AGUA,
            titulo = "Derretimiento",
            explicacion = "El calor movió las moléculas del hielo y lo regresó a agua líquida."
        ),
        ReactionRule(
            id = "germinacion",
            elemento1 = ElementoCientifico.SEMILLA,
            elemento2 = ElementoCientifico.AGUA,
            yields = ElementoCientifico.PLANTULA,
            titulo = "Germinación",
            explicacion = "La semilla despertó con el agua y empezó a sacar su primera hojita."
        ),
        ReactionRule(
            id = "magnetismo",
            elemento1 = ElementoCientifico.IMAN,
            elemento2 = ElementoCientifico.METAL,
            yields = ElementoCientifico.METAL_ATRAIDO,
            titulo = "Magnetismo",
            explicacion = "El imán atrae al metal sin necesidad de tocarlo."
        ),
        ReactionRule(
            id = "mezcla_tierra",
            elemento1 = ElementoCientifico.AGUA,
            elemento2 = ElementoCientifico.TIERRA,
            yields = ElementoCientifico.LODO,
            titulo = "Mezcla",
            explicacion = "La tierra se mezcló con el agua y formó lodo."
        ),
        ReactionRule(
            id = "burbujas",
            elemento1 = ElementoCientifico.BICARBONATO,
            elemento2 = ElementoCientifico.VINAGRE,
            yields = ElementoCientifico.BURBUJAS,
            titulo = "Reacción con gas",
            explicacion = "El bicarbonato y el vinagre reaccionaron y soltaron burbujas de dióxido de carbono."
        ),
        ReactionRule(
            id = "fotosintesis",
            elemento1 = ElementoCientifico.PLANTULA,
            elemento2 = ElementoCientifico.LUZ,
            yields = ElementoCientifico.OXIGENO,
            titulo = "Fotosíntesis",
            explicacion = "La plantita usó la luz para fabricar su alimento y soltó oxígeno."
        ),
        ReactionRule(
            id = "calor_roca",
            elemento1 = ElementoCientifico.ROCA,
            elemento2 = ElementoCientifico.CALOR,
            yields = ElementoCientifico.ROCA,
            titulo = "La roca se calienta",
            explicacion = "La roca se calentó, pero sigue siendo roca. El calor no la cambió de material."
        )
    )

    fun buscar(a: ElementoCientifico, b: ElementoCientifico): ReactionRule? =
        reglas.firstOrNull { it.coincide(a, b) }
}
