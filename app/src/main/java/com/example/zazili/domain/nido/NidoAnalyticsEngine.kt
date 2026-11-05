package com.example.zazili.domain.nido

import android.content.Context
import androidx.compose.runtime.Immutable
import com.example.zazili.ZaziliApplication
import com.example.zazili.core.mascot.ZaziCareState
import com.example.zazili.core.mascot.ZaziPetCareEngine
import com.example.zazili.core.progress.LevelProgressionEngine
import com.example.zazili.core.progress.ProgresoNivel
import com.example.zazili.core.usuario.PerfilUsuario
import com.example.zazili.domain.arena.EstadoArena
import com.example.zazili.domain.diagnostico.DifficultyProfile
import com.example.zazili.domain.diagnostico.PerfilAdaptativoStore
import com.example.zazili.domain.diagnostico.PerfilFrustracion
import com.example.zazili.domain.economia.EstadoEconomia
import com.example.zazili.domain.economia.ZazolEconomyManager
import com.example.zazili.domain.minijuegos.Minijuego
import com.example.zazili.domain.model.SessionLog
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.EstadoSaga
import com.example.zazili.domain.saga.RutaSaga
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Las cuatro áreas que resume el reporte del Nido. La Pirámide del Código se
 * agrupa con Ciencias porque mide razonamiento lógico y secuencias.
 */
enum class CategoriaAprendizaje(
    val etiqueta: String,
    val modulo: ModuloZazili
) {
    MATEMATICAS("Matemáticas", ModuloZazili.TIANGUIS_NUMERICO),
    LECTURA("Lectura", ModuloZazili.CUEVA_LEYENDAS),
    EMOCIONES("Inteligencia emocional", ModuloZazili.ARBOL_ABRAZOS),
    CIENCIAS("Ciencias y lógica", ModuloZazili.LABORATORIO_CENOTE);

    companion object {
        fun de(modulo: ModuloZazili): CategoriaAprendizaje? = when (modulo) {
            ModuloZazili.TIANGUIS_NUMERICO -> MATEMATICAS
            ModuloZazili.CUEVA_LEYENDAS -> LECTURA
            ModuloZazili.ARBOL_ABRAZOS -> EMOCIONES
            ModuloZazili.LABORATORIO_CENOTE -> CIENCIAS
            ModuloZazili.PIRAMIDE_CODIGO -> CIENCIAS
            ModuloZazili.NIDO_ADULTOS -> null
        }
    }
}

/** Foto pedagógica de una materia, ya normalizada para pintarse en barras. */
@Immutable
data class MetricaAprendizaje(
    val categoria: CategoriaAprendizaje,
    val puntaje: Float = 0f,
    val precision: Float = 0f,
    val tasaReintento: Float = 0f,
    val nivelPromedio: Int = 1,
    val intentos: Int = 0,
    val partidas: Int = 0,
    val nodosCompletados: Int = 0,
    val nodosTotales: Int = 0,
    val conDatos: Boolean = false,
    val fortaleza: Boolean = false,
    val areaOportunidad: Boolean = false
) {
    val puntajePct: Int get() = puntaje.aPorcentaje()
    val precisionPct: Int get() = precision.aPorcentaje()
    val reintentoPct: Int get() = tasaReintento.aPorcentaje()
    val nodosPct: Int
        get() = if (nodosTotales <= 0) 0
        else ((nodosCompletados.toFloat() / nodosTotales) * 100f).roundToInt().coerceIn(0, 100)
}

@Immutable
data class DificultadReporte(
    val complejidad: Int = 2,
    val multiplicadorVelocidad: Float = 1f,
    val apoyoAudio: Boolean = true,
    val calibrada: Boolean = false,
    val nivelArena: Int = 1,
    val perfilFrustracion: String = "Sin calibrar"
) {
    val velocidadPct: Int get() = (multiplicadorVelocidad * 100f).roundToInt()
}

@Immutable
data class PerfilGeneral(
    val nombre: String = "Pequeño explorador",
    val edad: Int = 0,
    val grado: String = "Sin registrar",
    val avatarEquipado: List<String> = emptyList(),
    val nivel: Int = 1,
    val xpEnNivel: Int = 0,
    val xpNecesaria: Int = 100,
    val xpTotal: Int = 0,
    val zazoles: Int = 0,
    val dificultad: DificultadReporte = DificultadReporte()
) {
    val avanceXp: Float
        get() = if (xpNecesaria <= 0) 0f
        else (xpEnNivel.toFloat() / xpNecesaria).coerceIn(0f, 1f)
}

@Immutable
data class BienestarReporte(
    val energia: Int = 88,
    val alimentacion: Int = 78,
    val higiene: Int = 92,
    val felicidad: Int = 84,
    val dormida: Boolean = false,
    val mensaje: String = "Zazi está bien cuidada y la rutina va al día."
)

/** Reporte único que consume el panel de adultos. */
@Immutable
data class ChildProfileReport(
    val listo: Boolean = false,
    val perfil: PerfilGeneral = PerfilGeneral(),
    val bienestar: BienestarReporte = BienestarReporte(),
    val metricas: List<MetricaAprendizaje> =
        CategoriaAprendizaje.entries.map { MetricaAprendizaje(it) },
    val fortalezas: List<String> = emptyList(),
    val areasOportunidad: List<String> = emptyList(),
    val recomendaciones: List<String> = emptyList(),
    val resumen: String = "Esperando la primera lectura de datos…",
    val nodosCompletados: Int = 0,
    val totalNodos: Int = RutaSaga.totalNodos(),
    val estrellas: Int = 0,
    val estrellasPosibles: Int = RutaSaga.totalEstrellasPosibles(),
    val generadoEn: Long = 0L
)

/**
 * Cerebro del Nido: cruza diagnóstico adaptativo, progreso, economía, cuidado
 * de Zazi, Arena Cortex, Saga y registro de intentos para producir un
 * [ChildProfileReport] reactivo.
 *
 * Todo se calcula en un solo `StateFlow` con `combine`, así que cualquier
 * cambio (una lección, un minijuego, un tick del Tamagotchi) refresca el
 * reporte sin que la UI vuelva a consultar el disco. No hay calificaciones ni
 * diagnósticos clínicos: solo señales para acompañar.
 */
class NidoAnalyticsEngine private constructor(context: Context) {

    private val contenedor = (context.applicationContext as ZaziliApplication).container
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val perfilAdaptativo: StateFlow<DifficultyProfile> =
        PerfilAdaptativoStore.obtener(context).perfil
    private val progresoNivel: StateFlow<ProgresoNivel> =
        LevelProgressionEngine.obtener(context).progreso
    private val economia: StateFlow<EstadoEconomia> =
        ZazolEconomyManager.obtener(context).estado
    private val cuidado: StateFlow<ZaziCareState> =
        ZaziPetCareEngine.obtener(context).estado

    private val categoriaPorJuego: Map<String, CategoriaAprendizaje> =
        Minijuego.todos.mapNotNull { juego ->
            CategoriaAprendizaje.de(juego.modulo.moduloZazili)?.let { juego.id to it }
        }.toMap()

    private val categoriaPorNodo: Map<String, CategoriaAprendizaje> =
        RutaSaga.mundos.flatMap { mundo ->
            val categoria = CategoriaAprendizaje.de(mundo.modulo)
                ?: return@flatMap emptyList<Pair<String, CategoriaAprendizaje>>()
            mundo.nodos.map { nodo -> nodo.id to categoria }
        }.toMap()

    private val base: Flow<BaseReporte> = combine(
        contenedor.userManager.perfil.catch { emit(null) },
        perfilAdaptativo,
        progresoNivel,
        economia,
        cuidado
    ) { perfil, dificultad, progreso, bolsa, estadoCuidado ->
        BaseReporte(perfil, dificultad, progreso, bolsa, estadoCuidado)
    }

    private val aprendizaje: Flow<AprendizajeReporte> = combine(
        contenedor.dynamicDifficultyEngine.estado,
        contenedor.progresoSaga.estado,
        contenedor.progressRepository.observeLogs().catch { emit(emptyList()) }
    ) { arena, saga, registros ->
        AprendizajeReporte(arena, saga, registros)
    }

    /** Reporte vivo; la UI lo colecta con `collectAsStateWithLifecycle()`. */
    val reporte: StateFlow<ChildProfileReport> = combine(base, aprendizaje) { datosBase, datosAprendizaje ->
        construirReporte(datosBase, datosAprendizaje)
    }.stateIn(
        scope = alcance,
        started = SharingStarted.Eagerly,
        initialValue = ChildProfileReport()
    )

    private fun construirReporte(
        base: BaseReporte,
        aprendizaje: AprendizajeReporte
    ): ChildProfileReport {
        val perfil = base.perfilUsuario
        val dificultad = base.dificultad
        val economia = base.economia
        val progreso = base.progresoNivel
        val bienestar = construirBienestar(base.cuidado)
        val metricas = calcularMetricas(aprendizaje, dificultad)

        val fortalezas = metricas.filter { it.fortaleza }.map { metrica ->
            "${metrica.categoria.etiqueta}: ${metrica.precisionPct}% de aciertos " +
                "en ${metrica.intentos} intentos."
        }
        val areas = metricas.filter { it.areaOportunidad }.map { metrica ->
            buildString {
                append("${metrica.categoria.etiqueta}: ")
                append("${metrica.precisionPct}% de aciertos")
                if (metrica.reintentoPct > 0) {
                    append(" y ${metrica.reintentoPct}% de los intentos se repitieron")
                }
                append('.')
            }
        }
        val recomendaciones = generarRecomendaciones(metricas, bienestar, fortalezas)

        val idsValidos = RutaSaga.mundos
            .flatMap { mundo -> mundo.nodos.map { it.id } }
            .toSet()
        val nodosCompletados = aprendizaje.saga.completados.count { it in idsValidos }
        val estrellas = aprendizaje.saga.estrellas
            .filterKeys { it in idsValidos }
            .values
            .sum()

        val edad = perfil?.edad?.takeIf { it > 0 } ?: dificultad.edadSugerida

        return ChildProfileReport(
            listo = true,
            perfil = PerfilGeneral(
                nombre = perfil?.nombre?.takeIf { it.isNotBlank() } ?: "Pequeño explorador",
                edad = edad,
                grado = gradoDe(edad),
                avatarEquipado = economia.equipamiento.values.toList(),
                nivel = progreso.nivel,
                xpEnNivel = progreso.xpEnNivel,
                xpNecesaria = progreso.xpNecesaria,
                xpTotal = progreso.xpTotal,
                zazoles = economia.zazoles,
                dificultad = DificultadReporte(
                    complejidad = dificultad.complejidad,
                    multiplicadorVelocidad = dificultad.multiplicadorVelocidad,
                    apoyoAudio = dificultad.apoyoAudio,
                    calibrada = dificultad.calibrado,
                    nivelArena = aprendizaje.arena.nivelPromedio,
                    perfilFrustracion = nombreFrustracion(perfil?.perfilFrustracion)
                )
            ),
            bienestar = bienestar,
            metricas = metricas,
            fortalezas = fortalezas,
            areasOportunidad = areas,
            recomendaciones = recomendaciones,
            resumen = resumenDe(metricas, fortalezas.size, areas.size),
            nodosCompletados = nodosCompletados,
            totalNodos = idsValidos.size,
            estrellas = estrellas,
            estrellasPosibles = idsValidos.size * 3,
            generadoEn = System.currentTimeMillis()
        )
    }

    private fun calcularMetricas(
        aprendizaje: AprendizajeReporte,
        dificultad: DifficultyProfile
    ): List<MetricaAprendizaje> = CategoriaAprendizaje.entries.map { categoria ->
        val juegos = Minijuego.todos.filter { categoriaPorJuego[it.id] == categoria }
        val idsJuegos = juegos.mapTo(HashSet()) { it.id }
        val progresos = aprendizaje.arena.progresos.values
            .filter { it.juegoId in idsJuegos }
        val jugados = progresos.filter { it.partidasJugadas > 0 }
        val partidas = jugados.sumOf { it.partidasJugadas }
        val nivelPromedio = if (jugados.isEmpty()) {
            dificultad.complejidad
        } else {
            (jugados.sumOf { it.estrellas }.toFloat() / jugados.size)
                .roundToInt()
                .coerceIn(1, 5)
        }

        val nodos = RutaSaga.mundos
            .filter { CategoriaAprendizaje.de(it.modulo) == categoria }
            .flatMap { it.nodos }
        val completados = nodos.count { aprendizaje.saga.completado(it.id) }

        val registros = aprendizaje.logs.filter { categoriaDe(it) == categoria }
        val intentos = registros.size
        val aciertos = registros.count { it.succeeded }
        val precision = if (intentos == 0) 0f else aciertos.toFloat() / intentos
        val reintentos = registros.count { it.attemptCount > 1 }
        val tasaReintento = if (intentos == 0) 0f else reintentos.toFloat() / intentos

        val conDatos = intentos > 0 || partidas > 0 || completados > 0
        val nivelBase = ((nivelPromedio - 1) / 4f).coerceIn(0f, 1f)
        val sagaBase = if (nodos.isEmpty()) 0f else completados.toFloat() / nodos.size

        val puntaje = when {
            intentos > 0 -> precision * 0.60f + nivelBase * 0.20f +
                sagaBase * 0.20f - tasaReintento * 0.10f
            partidas > 0 -> nivelBase * 0.65f + sagaBase * 0.35f
            completados > 0 -> nivelBase * 0.30f + sagaBase * 0.70f
            else -> 0f
        }.coerceIn(0f, 1f)

        val fortaleza = intentos >= 3 && precision > 0.80f
        val areaOportunidad = !fortaleza && intentos >= 2 &&
            (precision < 0.50f || (precision < 0.75f && tasaReintento >= 0.35f))

        MetricaAprendizaje(
            categoria = categoria,
            puntaje = puntaje,
            precision = precision,
            tasaReintento = tasaReintento,
            nivelPromedio = nivelPromedio,
            intentos = intentos,
            partidas = partidas,
            nodosCompletados = completados,
            nodosTotales = nodos.size,
            conDatos = conDatos,
            fortaleza = fortaleza,
            areaOportunidad = areaOportunidad
        )
    }

    private fun categoriaDe(registro: SessionLog): CategoriaAprendizaje? {
        if (registro.moduleRoute.equals("VUELO_DE_PRUEBA", ignoreCase = true)) return null
        categoriaPorNodo[registro.activityId]?.let { return it }
        categoriaPorJuego[registro.activityId]?.let { return it }
        categoriaPorJuego[registro.moduleRoute]?.let { return it }
        categoriaPorNodo[registro.moduleRoute]?.let { return it }
        val modulo = runCatching { ModuloZazili.valueOf(registro.moduleRoute) }.getOrNull()
            ?: runCatching { ModuloZazili.valueOf(registro.moduleRoute.uppercase(Locale.ROOT)) }
                .getOrNull()
        return modulo?.let { CategoriaAprendizaje.de(it) }
    }

    private fun construirBienestar(cuidado: ZaziCareState): BienestarReporte {
        val vitales = cuidado.vitals
        val mensaje = when {
            vitales.dormida ->
                "Zazi está durmiendo. El descanso también cuenta como cuidado."
            vitales.energia < 30f ->
                "Le queda poca energía. Una sesión corta y un descanso de treinta minutos la recargan."
            vitales.pancita < 35f ->
                "Tiene hambre. Alimentarla antes de jugar mantiene la rutina y el ánimo."
            vitales.higiene < 35f ->
                "Necesita un baño. El autocuidado también se practica acompañando a Zazi."
            vitales.felicidad < 35f ->
                "Anda bajita de ánimo. Un mimo o un minijuego corto le devuelven la sonrisa."
            else -> "Zazi está bien cuidada y la rutina va al día."
        }
        return BienestarReporte(
            energia = vitales.energiaPct,
            alimentacion = vitales.pancitaPct,
            higiene = vitales.higienePct,
            felicidad = vitales.felicidadPct,
            dormida = vitales.dormida,
            mensaje = mensaje
        )
    }

    private fun generarRecomendaciones(
        metricas: List<MetricaAprendizaje>,
        bienestar: BienestarReporte,
        fortalezas: List<String>
    ): List<String> {
        val consejos = mutableListOf<String>()
        val porCategoria = metricas.associateBy { it.categoria }
        val emociones = porCategoria[CategoriaAprendizaje.EMOCIONES]
        val matematicas = porCategoria[CategoriaAprendizaje.MATEMATICAS]
        val lectura = porCategoria[CategoriaAprendizaje.LECTURA]
        val ciencias = porCategoria[CategoriaAprendizaje.CIENCIAS]
        val hayDatos = metricas.any { it.conDatos }

        if (!hayDatos) {
            consejos += "Todavía hay poca actividad registrada. Una lección corta o un minijuego " +
                "de cinco minutos bastan para que este reporte tome forma."
        }
        if (emociones != null && (
                emociones.areaOportunidad ||
                    (emociones.conDatos && emociones.puntaje < 0.55f)
                )
        ) {
            consejos += "Zazi nota que le cuesta identificar emociones sociales. " +
                "Se recomienda practicar el reconocimiento facial en casa."
        }
        if (matematicas != null && (
                matematicas.areaOportunidad ||
                    (matematicas.conDatos && matematicas.puntaje < 0.55f)
                )
        ) {
            consejos += "Para matemáticas, cuenten frijoles, tapas o monedas de juguete antes " +
                "de escribir números. Diez minutos diarios bastan."
        }
        if (lectura != null && (
                lectura.areaOportunidad ||
                    (lectura.conDatos && lectura.puntaje < 0.55f)
                )
        ) {
            consejos += "Para lectura, lean juntos un cuento corto y cierren con una pregunta " +
                "sencilla: ¿quién era el personaje y qué quería?"
        }
        if (ciencias != null && (
                ciencias.areaOportunidad ||
                    (ciencias.conDatos && ciencias.puntaje < 0.55f)
                )
        ) {
            consejos += "Para ciencias y lógica, prueben un experimento de cocina y pregunten " +
                "qué creen que pasará antes de hacerlo."
        }
        if (bienestar.energia < 30) {
            consejos += "Zazi anda con poca energía. Conviene una sesión corta y un descanso " +
                "de treinta minutos sin pantalla."
        }
        if (bienestar.alimentacion < 35) {
            consejos += "Recuerden darle de comer a Zazi antes de abrir una lección; " +
                "cuidarla también enseña responsabilidad."
        }
        if (bienestar.higiene < 35) {
            consejos += "Un baño de burbujas para Zazi ayuda a cerrar el día y mantiene " +
                "la rutina de autocuidado."
        }
        if (metricas.any { it.areaOportunidad }) {
            consejos += "Cuando un reto pida más de dos intentos, hagan una pausa de respiración " +
                "4-2-4 y vuelvan con una pista."
        }
        if (fortalezas.isNotEmpty()) {
            val materia = fortalezas.first().substringBefore(':').trim()
            consejos += "Refuercen en voz alta lo que ya domina en $materia. " +
                "El reconocimiento específico motiva más que un premio genérico."
        }
        if (consejos.isEmpty()) {
            consejos += "Sigan practicando diez minutos al día. La constancia amable gana " +
                "a las sesiones largas."
        }
        return consejos.distinct().take(5)
    }

    private fun resumenDe(
        metricas: List<MetricaAprendizaje>,
        totalFortalezas: Int,
        totalAreas: Int
    ): String = when {
        metricas.none { it.conDatos } ->
            "Aún no hay suficiente actividad para trazar un perfil. " +
                "Abran una lección corta y este reporte se llenará solo."
        totalFortalezas > 0 && totalAreas == 0 ->
            "El vuelo va firme: ${textoFortalezas(totalFortalezas)} y ninguna área de " +
                "riesgo en los datos actuales."
        totalFortalezas > 0 ->
            "Hay ${textoFortalezas(totalFortalezas)} y ${textoAreas(totalAreas)} donde el " +
                "acompañamiento hará diferencia."
        totalAreas > 0 ->
            "Todavía no se perfila una fortaleza por encima del 80%. El esfuerzo se " +
                "concentra en ${textoAreas(totalAreas)} que piden acompañamiento."
        else ->
            "El desempeño se mantiene estable. Sigan practicando sin prisa para que el " +
                "reporte gane detalle."
    }

    private fun textoFortalezas(total: Int): String =
        if (total == 1) "una fortaleza clara" else "$total fortalezas claras"

    private fun textoAreas(total: Int): String =
        if (total == 1) "un área de oportunidad" else "$total áreas de oportunidad"

    private fun gradoDe(edad: Int): String = when (edad) {
        in 6..7 -> "1.º de primaria"
        8 -> "2.º de primaria"
        9 -> "3.º de primaria"
        10 -> "4.º de primaria"
        else -> "Sin registrar"
    }

    private fun nombreFrustracion(perfil: PerfilFrustracion?): String = when (perfil) {
        PerfilFrustracion.RESILIENTE -> "Resiliente"
        PerfilFrustracion.IMPULSIVO -> "Impulsivo"
        PerfilFrustracion.BAJA_TOLERANCIA -> "Baja tolerancia a la frustración"
        null -> "Sin calibrar"
    }

    private data class BaseReporte(
        val perfilUsuario: PerfilUsuario?,
        val dificultad: DifficultyProfile,
        val progresoNivel: ProgresoNivel,
        val economia: EstadoEconomia,
        val cuidado: ZaziCareState
    )

    private data class AprendizajeReporte(
        val arena: EstadoArena,
        val saga: EstadoSaga,
        val logs: List<SessionLog>
    )

    companion object {
        @Volatile
        private var instancia: NidoAnalyticsEngine? = null

        fun obtener(context: Context): NidoAnalyticsEngine =
            instancia ?: synchronized(this) {
                instancia ?: NidoAnalyticsEngine(context.applicationContext).also {
                    instancia = it
                }
            }
    }
}

private fun Float.aPorcentaje(): Int = (this * 100f).roundToInt().coerceIn(0, 100)
