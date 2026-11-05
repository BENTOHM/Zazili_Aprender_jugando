package com.example.zazili.core.logros

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.zazili.core.racha.RachaEngine
import com.example.zazili.core.ui.CelebrationKonfetti
import com.example.zazili.domain.avatar.AvatarContentFactory
import com.example.zazili.domain.economia.ZazolEconomyManager
import java.io.IOException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.dataStoreLogros: DataStore<Preferences> by preferencesDataStore(
    name = "zazili_logros"
)

/**
 * Eventos que las pantallas reportan al motor. Cada evento lleva un [dato]
 * numérico opcional: monto gastado, días de racha, número de nivel, etc.
 */
enum class EventoLogro(val datoPorDefecto: Int = 1) {
    /** Se completó cualquier actividad pagada (lección o minijuego). */
    ACTIVIDAD_COMPLETADA,

    /** Se jugó un minijuego (cualquiera). */
    MINIJUEGO_JUGADO,

    /** Se completó una lección del camino de algún módulo. */
    LECCION_COMPLETADA,

    /** Se visitó un mundo concreto (dato = id interno del módulo). */
    MUNDO_VISITADO,

    /** Se gastaron Zazoles (dato = monto gastado). */
    GASTO,

    /** Se ganaron Zazoles (dato = monto ganado). */
    GANANCIA,

    /** Se compró una pieza del avatar (dato = precio pagado). */
    COMPRA_AVATAR,

    /** Se le hizo cariño a Zazi. */
    CARRINO_ZAZI,

    /** Se alimentó a Zazi. */
    ALIMENTO_ZAZI,

    /** Bug aplastado en Aplasta el Bug. */
    BUG_APLASTADO,

    /** Nivel superado en Ruta del Algoritmo (dato = nivel). */
    NIVEL_ALGORITMO,

    /** Escenario respondido en Filtro Ciber-Seguro. */
    ESCENARIO_CIBER,

    /** Se compró un Protector de Racha. */
    PROTECTOR_COMPRADO,

    /** Un protector salvó la racha de un día perdido. */
    PROTECTOR_USADO,

    /** El widget de racha vive en la pantalla de inicio. */
    WIDGET_AGREGADO,

    /** Se cambió la música de fondo. */
    MUSICA_CAMBIADA,

    /** Se equipó una pieza del armario (solo reevalúa logros de colección). */
    EQUIPAR_AVATAR,

    /** Racha diaria alcanzada (dato = días seguidos). */
    RACHA_DIAS,

    /** Niveles completados de un módulo (dato = cantidad acumulada). */
    NIVELES_MODULO
}

/**
 * Nombres estables de las acciones globales que las pantallas reportan con
 * [AchievementEngine.notifyAction]. Se usan constantes para que un error de
 * dedo en un `String` no vuelva a desconectar un logro en silencio.
 */
object AccionesLogro {
    const val LECCION_COMPLETADA = "LESSON_COMPLETE"

    /**
     * Ganar un minijuego es la única otra acción que enciende la racha.
     * El nombre sigue el contrato `MINIGAME_WON`; [MINIJUEGO_COMPLETADO_LEGADO]
     * se acepta solo para no romper pantallas o datos viejos.
     */
    const val MINIJUEGO_COMPLETADO = "MINIGAME_WON"
    const val MINIJUEGO_COMPLETADO_LEGADO = "MINIGAME_COMPLETE"
    const val EQUIPAR_PIEZA = "EQUIP_ITEM"
    const val CAMBIAR_MUSICA = "CHANGE_MUSIC"
    const val COMPRA_REALIZADA = "PURCHASE"
    const val WIDGET_AGREGADO = "WIDGET_ADDED"
    const val DIA_DE_RACHA = "STREAK_DAY"
    const val PROTECTOR_USADO = "PROTECTOR_USED"
    const val PROTECTOR_COMPRADO = "PROTECTOR_BOUGHT"
}

/**
 * Puerta global de eventos: cualquier pantalla que tenga un `Context` puede
 * reportar una acción sin cargar el motor ni recordar el enum. El motor se
 * obtiene como singleton y evalúa el catálogo al instante.
 */
object AchievementDispatcher {
    fun notifyAction(context: Context, actionType: String, dato: Int = 1) {
        AchievementEngine.obtener(context).notifyAction(actionType, dato)
    }
}

/**
 * Un logro desbloqueable. [isUnlocked] se resuelve contra el set persistido;
 * [rewardItemId] referencia un id del [AvatarContentFactory] que se regala al
 * desbloquear (null si solo paga Zazoles).
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val rewardZazoles: Int,
    val rewardItemId: String? = null,
    val isUnlocked: Boolean = false
)

/**
 * Catálogo completo de logros. NO se recorta: cada entrada tiene título,
 * descripción y recompensa en español mexicano. Las condiciones se evalúan en
 * [AchievementEngine.evaluar] contra los contadores persistidos.
 */
object CatalogoLogros {
    val todos: List<Achievement> = listOf(
        // ------------------------- PRIMEROS PASOS -------------------------
        Achievement(
            "primeros_pasos", "Primeros Pasos",
            "Juega tu primer minijuego. El comienzo de algo enorme.", 50
        ),
        Achievement(
            "tarea_terminada", "Tarea Terminada",
            "Completa tu primera lección del camino.", 25
        ),
        Achievement(
            "primer_zazol", "Primer Zazol",
            "Gana tu primer Zazol brillante.", 10
        ),

        // ------------------------- CONSTANCIA -------------------------
        Achievement(
            "aprendiz_constante", "Aprendiz Constante",
            "Completa 10 lecciones en total.", 60
        ),
        Achievement(
            "maraton_mental", "Maratón Mental",
            "Juega 50 minijuegos en total.", 100
        ),
        Achievement(
            "curioso_nato", "Curioso Nato",
            "Visita los 6 mundos de Zazili al menos una vez.", 80
        ),
        Achievement(
            "cerebro_galactico", "Cerebro Galáctico",
            "Completa los 50 niveles de cualquier módulo. Cerebro de otro planeta.", 200,
            rewardItemId = "cabeza_penacho"
        ),

        // ------------------------- RACHA -------------------------
        Achievement(
            "fiebre_aprender_3", "Fiebre de Aprender I",
            "Mantén una racha de 3 días seguidos.", 30
        ),
        Achievement(
            "fiebre_aprender_7", "Fiebre de Aprender II",
            "Mantén una racha de 7 días seguidos. ¡Una semana entera!", 70
        ),
        Achievement(
            "fiebre_aprender_30", "Fiebre de Aprender III",
            "Mantén una racha de 30 días. Eres leyenda viva.", 300
        ),
        Achievement(
            "racha_inmortal", "Racha Inmortal",
            "Un Protector de Racha te salvó de perder tu racha.", 45
        ),
        Achievement(
            "protector_listo", "Protector Listo",
            "Compra tu primer Protector de Racha.", 15
        ),

        // ------------------------- ECONOMÍA -------------------------
        Achievement(
            "ahorrador_experto", "Ahorrador Experto",
            "Junta 1,000 Zazoles en tu bolsa.", 100
        ),
        Achievement(
            "tiburon_zazoles", "Tiburón de los Zazoles",
            "Acumula 5,000 Zazoles. Nadie negocia mejor que tú.", 250
        ),
        Achievement(
            "gran_gasto", "Gran Gasto",
            "Gasta 500 Zazoles en total entre todas las tiendas.", 60
        ),

        // ------------------------- BOUTIQUE PIXEL -------------------------
        Achievement(
            "fashionista_novato", "Fashionista Novato",
            "Compra tu primera pieza en la Boutique Pixel.", 20
        ),
        Achievement(
            "vestidor_pro", "Vestidor Pro",
            "Desbloquea 10 piezas del armario pixel.", 90
        ),
        Achievement(
            "leyenda_estilo", "Leyenda del Estilo",
            "Desbloquea una pieza Legendaria de la Boutique Pixel.", 120
        ),
        Achievement(
            "fan_kuromi", "Fan de Kuromi",
            "Desbloquea la Máscara de Kuromi. Traviesa y tierna.", 50
        ),

        // ------------------------- CASITA Y ZAZI -------------------------
        Achievement(
            "mano_magica", "Mano Mágica",
            "Hazle 20 cariños a Zazi.", 30
        ),
        Achievement(
            "chef_zazoles", "Chef de Zazoles",
            "Alimenta a Zazi 10 veces. Pancita feliz.", 40
        ),

        // ------------------------- ARENA CORTEX -------------------------
        Achievement(
            "cazabugs", "Cazabugs",
            "Aplasta 25 bugs en Aplasta el Bug.", 40
        ),
        Achievement(
            "maestro_algoritmo", "Maestro del Algoritmo",
            "Supera los 3 niveles de Ruta del Algoritmo.", 80
        ),
        Achievement(
            "guardia_ciberseguro", "Guardia Ciber-Seguro",
            "Responde 12 escenarios del Filtro Ciber-Seguro.", 50
        ),

        // ------------------------- PERSONALIZACIÓN -------------------------
        Achievement(
            "decorador_experto", "Decorador Experto",
            "Añade el widget de racha a tu pantalla de inicio.", 100
        ),
        Achievement(
            "dj_zazili", "DJ Zazili",
            "Cambia la música de fondo de la app.", 20
        )
    )

    private val mapa: Map<String, Achievement> = todos.associateBy { it.id }

    fun porId(id: String): Achievement? = mapa[id]
}

/** Contadores vivos del motor, observables por la UI de logros futura. */
data class EstadoLogros(
    val desbloqueados: Set<String> = emptySet(),
    val minijuegos: Int = 0,
    val lecciones: Int = 0,
    val actividades: Int = 0,
    val mundosVisitados: Set<Int> = emptySet(),
    val gastado: Int = 0,
    val ganado: Int = 0,
    val comprasAvatar: Int = 0,
    val carinos: Int = 0,
    val alimentos: Int = 0,
    val bugs: Int = 0,
    val nivelesAlgoritmo: Int = 0,
    val escenariosCiber: Int = 0,
    val nivelesModulo: Int = 0,
    val listo: Boolean = false
) {
    fun achievementDe(id: String): Achievement? =
        CatalogoLogros.porId(id)?.let { it.copy(isUnlocked = id in desbloqueados) }
}

/**
 * Motor global de logros: singleton con DataStore (`zazili_logros`).
 *
 * Las pantallas reportan eventos con [notificar]; el motor actualiza sus
 * contadores, evalúa TODAS las condiciones del catálogo, desbloquea lo nuevo,
 * paga la recompensa (Zazoles y/o pieza de avatar) y emite cada logro recién
 * desbloqueado por [celebraciones]. Ese flujo es la única fuente del aviso
 * global: [com.example.zazili.core.navigation.ZaziliApp] lo escucha en la raíz
 * y muestra [AchievementUnlockedBanner] sobre cualquier pantalla, mientras
 * [ObservadorLogros] se queda solo con el confeti local.
 *
 * Los contadores viven en preferencias, así que ningún logro se pierde aunque
 * el evento llegue con la app recién abierta.
 */
class AchievementEngine private constructor(context: Context) {

    private val app = context.applicationContext
    private val dataStore = app.dataStoreLogros
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Serializa los eventos: un logro se emite una sola vez aunque lleguen
     *  dos acciones simultáneas antes de persistir la bandera. */
    private val candadoEventos = Mutex()

    /**
     * Flujo de una sola vez: cada emisión corresponde a una transición real
     * BLOQUEADO → DESBLOQUEADO. No tiene `replay`: un logro desbloqueado con la
     * app en segundo plano no se repite al volver a la pantalla. Los oyentes
     * (raíz de Compose y confeti) se re-suscriben solos al recomponerse.
     */
    private val _celebraciones = MutableSharedFlow<Achievement>(extraBufferCapacity = 8)
    val celebraciones: SharedFlow<Achievement> = _celebraciones.asSharedFlow()

    val estado: StateFlow<EstadoLogros> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            EstadoLogros(
                desbloqueados = prefs[CLAVE_DESBLOQUEADOS] ?: emptySet(),
                minijuegos = prefs[CONT_MINIJUEGOS] ?: 0,
                lecciones = prefs[CONT_LECCIONES] ?: 0,
                actividades = prefs[CONT_ACTIVIDADES] ?: 0,
                mundosVisitados = prefs[CONT_MUNDOS].orEmpty().mapNotNull { it.toIntOrNull() }
                    .toSet(),
                gastado = prefs[CONT_GASTADO] ?: 0,
                ganado = prefs[CONT_GANADO] ?: 0,
                comprasAvatar = prefs[CONT_COMPRAS_AVATAR] ?: 0,
                carinos = prefs[CONT_CARINOS] ?: 0,
                alimentos = prefs[CONT_ALIMENTOS] ?: 0,
                bugs = prefs[CONT_BUGS] ?: 0,
                nivelesAlgoritmo = prefs[CONT_NIVEL_ALGORITMO] ?: 0,
                escenariosCiber = prefs[CONT_ESCENARIOS_CIBER] ?: 0,
                nivelesModulo = prefs[CONT_NIVELES_MODULO] ?: 0,
                listo = true
            )
        }
        .stateIn(alcance, kotlinx.coroutines.flow.SharingStarted.Eagerly, EstadoLogros())

    /** Versión sin bloqueo para llamar desde callbacks de Compose. */
    fun notificar(evento: EventoLogro, dato: Int = evento.datoPorDefecto) {
        alcance.launch { runCatching { notificarSuspend(evento, dato) } }
    }

    /**
     * Despachador global por nombre de acción. Traduce una acción de pantalla a
     * uno o varios [EventoLogro], los persiste y evalúa el catálogo completo en
     * el momento; si algo se cumple, [celebraciones] emite el logro para que el
     * confeti y el aviso dorado salgan sin esperar a otra pantalla.
     */
    fun notifyAction(actionType: String, dato: Int = 1) {
        val accion = actionType.trim().uppercase()
        val eventos = eventosDeAccion(accion)
        if (eventos.isEmpty()) {
            Log.w(ETIQUETA, "Acción de logro desconocida: $actionType")
            return
        }
        alcance.launch {
            eventos.forEach { evento ->
                runCatching { notificarSuspend(evento, dato) }
                    .onFailure { Log.w(ETIQUETA, "Falló la acción $actionType", it) }
            }
            // ÚNICA PUERTA DE ENTRADA A LA RACHA: solo una lección completada o
            // un minijuego ganado encienden la flama. Abrir la app, tocar a
            // Zazi o navegar por los menús NO llama a este motor.
            if (
                accion == AccionesLogro.LECCION_COMPLETADA ||
                accion == AccionesLogro.MINIJUEGO_COMPLETADO
            ) {
                runCatching { RachaEngine.obtener(app).registrarJuegoHoy() }
                    .onFailure { Log.w(ETIQUETA, "No se pudo registrar la racha", it) }
            }
        }
    }

    /**
     * Una acción puede mover más de un contador: completar una lección también
     * cuenta como actividad completada y cambiar la música no mueve números,
     * pero reevalúa la condición de DJ Zazili.
     */
    private fun eventosDeAccion(actionType: String): List<EventoLogro> =
        when (actionType.trim().uppercase()) {
            AccionesLogro.LECCION_COMPLETADA ->
                listOf(EventoLogro.LECCION_COMPLETADA, EventoLogro.ACTIVIDAD_COMPLETADA)

            AccionesLogro.MINIJUEGO_COMPLETADO,
            AccionesLogro.MINIJUEGO_COMPLETADO_LEGADO ->
                listOf(EventoLogro.MINIJUEGO_JUGADO, EventoLogro.ACTIVIDAD_COMPLETADA)

            AccionesLogro.EQUIPAR_PIEZA -> listOf(EventoLogro.EQUIPAR_AVATAR)
            AccionesLogro.CAMBIAR_MUSICA -> listOf(EventoLogro.MUSICA_CAMBIADA)
            AccionesLogro.COMPRA_REALIZADA -> listOf(EventoLogro.GASTO)
            AccionesLogro.WIDGET_AGREGADO -> listOf(EventoLogro.WIDGET_AGREGADO)
            AccionesLogro.DIA_DE_RACHA -> listOf(EventoLogro.RACHA_DIAS)
            AccionesLogro.PROTECTOR_USADO -> listOf(EventoLogro.PROTECTOR_USADO)
            AccionesLogro.PROTECTOR_COMPRADO -> listOf(EventoLogro.PROTECTOR_COMPRADO)
            else -> emptyList()
        }

    suspend fun notificarSuspend(evento: EventoLogro, dato: Int = evento.datoPorDefecto) {
        candadoEventos.withLock { procesarEvento(evento, dato) }
    }

    private suspend fun procesarEvento(evento: EventoLogro, dato: Int) {
        var recienDesbloqueados: List<Achievement> = emptyList()
        try {
            dataStore.edit { prefs ->
                when (evento) {
                    EventoLogro.ACTIVIDAD_COMPLETADA ->
                        prefs[CONT_ACTIVIDADES] = (prefs[CONT_ACTIVIDADES] ?: 0) + 1

                    EventoLogro.MINIJUEGO_JUGADO ->
                        prefs[CONT_MINIJUEGOS] = (prefs[CONT_MINIJUEGOS] ?: 0) + 1

                    EventoLogro.LECCION_COMPLETADA ->
                        prefs[CONT_LECCIONES] = (prefs[CONT_LECCIONES] ?: 0) + 1

                    EventoLogro.MUNDO_VISITADO -> {
                        val mundo = dato.toString()
                        val visitados = (prefs[CONT_MUNDOS] ?: emptySet()).toMutableSet()
                        visitados += mundo
                        prefs[CONT_MUNDOS] = visitados
                    }

                    EventoLogro.GASTO ->
                        prefs[CONT_GASTADO] = (prefs[CONT_GASTADO] ?: 0) + dato.coerceAtLeast(0)

                    EventoLogro.GANANCIA ->
                        prefs[CONT_GANADO] = (prefs[CONT_GANADO] ?: 0) + dato.coerceAtLeast(0)

                    EventoLogro.COMPRA_AVATAR ->
                        prefs[CONT_COMPRAS_AVATAR] = (prefs[CONT_COMPRAS_AVATAR] ?: 0) + 1

                    EventoLogro.CARRINO_ZAZI ->
                        prefs[CONT_CARINOS] = (prefs[CONT_CARINOS] ?: 0) + 1

                    EventoLogro.ALIMENTO_ZAZI ->
                        prefs[CONT_ALIMENTOS] = (prefs[CONT_ALIMENTOS] ?: 0) + 1

                    EventoLogro.BUG_APLASTADO ->
                        prefs[CONT_BUGS] = (prefs[CONT_BUGS] ?: 0) + 1

                    EventoLogro.NIVEL_ALGORITMO ->
                        prefs[CONT_NIVEL_ALGORITMO] =
                            maxOf(prefs[CONT_NIVEL_ALGORITMO] ?: 0, dato)

                    EventoLogro.ESCENARIO_CIBER ->
                        prefs[CONT_ESCENARIOS_CIBER] = (prefs[CONT_ESCENARIOS_CIBER] ?: 0) + 1

                    EventoLogro.NIVELES_MODULO ->
                        prefs[CONT_NIVELES_MODULO] =
                            maxOf(prefs[CONT_NIVELES_MODULO] ?: 0, dato)

                    EventoLogro.PROTECTOR_COMPRADO,
                    EventoLogro.PROTECTOR_USADO,
                    EventoLogro.WIDGET_AGREGADO,
                    EventoLogro.MUSICA_CAMBIADA,
                    EventoLogro.EQUIPAR_AVATAR,
                    EventoLogro.RACHA_DIAS -> Unit // Solo condición: no llevan contador.
                }
            }

            recienDesbloqueados = evaluarNuevos(evento, dato)
            if (recienDesbloqueados.isNotEmpty()) {
                dataStore.edit { prefs ->
                    prefs[CLAVE_DESBLOQUEADOS] =
                        (prefs[CLAVE_DESBLOQUEADOS] ?: emptySet()) +
                            recienDesbloqueados.map { it.id }.toSet()
                }
                recienDesbloqueados.forEach { logro ->
                    otorgarRecompensa(logro)
                    _celebraciones.emit(logro)
                }
            }
        } catch (error: IOException) {
            Log.w(ETIQUETA, "No se pudo procesar el evento ${evento.name}", error)
        }
    }

    /**
     * Evalúa las condiciones del catálogo con el estado YA persistido y
     * devuelve los logros que acaban de cumplirse. Las condiciones se calculan
     * desde contadores (no desde banderas de una sola pasada), así que ningún
     * logro se salta aunque lleguen varios eventos seguidos.
     */
    private suspend fun evaluarNuevos(evento: EventoLogro, dato: Int): List<Achievement> {
        val prefs = dataStore.data.first()
        val e = EstadoLogros(
            desbloqueados = prefs[CLAVE_DESBLOQUEADOS] ?: emptySet(),
            minijuegos = prefs[CONT_MINIJUEGOS] ?: 0,
            lecciones = prefs[CONT_LECCIONES] ?: 0,
            actividades = prefs[CONT_ACTIVIDADES] ?: 0,
            mundosVisitados = prefs[CONT_MUNDOS].orEmpty().mapNotNull { it.toIntOrNull() }.toSet(),
            gastado = prefs[CONT_GASTADO] ?: 0,
            ganado = prefs[CONT_GANADO] ?: 0,
            comprasAvatar = prefs[CONT_COMPRAS_AVATAR] ?: 0,
            carinos = prefs[CONT_CARINOS] ?: 0,
            alimentos = prefs[CONT_ALIMENTOS] ?: 0,
            bugs = prefs[CONT_BUGS] ?: 0,
            nivelesAlgoritmo = prefs[CONT_NIVEL_ALGORITMO] ?: 0,
            escenariosCiber = prefs[CONT_ESCENARIOS_CIBER] ?: 0,
            nivelesModulo = prefs[CONT_NIVELES_MODULO] ?: 0
        )

        // Piezas desbloqueadas del armario: alimentan logros de colección.
        val economia = ZazolEconomyManager.obtener(app)
        val piezasDesbloqueadas = economia.estado.first().desbloqueados
        val saldoActual = economia.estado.first().zazoles

        fun cumple(id: String, condicion: Boolean): Boolean =
            condicion && id !in e.desbloqueados

        val desbloqueados = buildList {
            if (cumple("primeros_pasos", e.minijuegos >= 1)) add("primeros_pasos")
            if (cumple("tarea_terminada", e.lecciones >= 1)) add("tarea_terminada")
            if (cumple("primer_zazol", e.ganado >= 1 || saldoActual > 120)) add("primer_zazol")
            if (cumple("aprendiz_constante", e.lecciones >= 10)) add("aprendiz_constante")
            if (cumple("maraton_mental", e.minijuegos >= 50)) add("maraton_mental")
            if (cumple("curioso_nato", e.mundosVisitados.size >= 6)) add("curioso_nato")
            if (cumple("cerebro_galactico", e.nivelesModulo >= 50)) add("cerebro_galactico")
            if (cumple("fiebre_aprender_3", evento == EventoLogro.RACHA_DIAS && dato >= 3)) {
                add("fiebre_aprender_3")
            }
            if (cumple("fiebre_aprender_7", evento == EventoLogro.RACHA_DIAS && dato >= 7)) {
                add("fiebre_aprender_7")
            }
            if (cumple("fiebre_aprender_30", evento == EventoLogro.RACHA_DIAS && dato >= 30)) {
                add("fiebre_aprender_30")
            }
            if (cumple("racha_inmortal", evento == EventoLogro.PROTECTOR_USADO)) {
                add("racha_inmortal")
            }
            if (cumple("protector_listo", evento == EventoLogro.PROTECTOR_COMPRADO)) {
                add("protector_listo")
            }
            if (cumple("ahorrador_experto", saldoActual >= 1000)) add("ahorrador_experto")
            if (cumple("tiburon_zazoles", saldoActual >= 5000)) add("tiburon_zazoles")
            if (cumple("gran_gasto", e.gastado >= 500)) add("gran_gasto")
            if (cumple("fashionista_novato", e.comprasAvatar >= 1)) add("fashionista_novato")
            if (cumple("vestidor_pro", piezasDesbloqueadas.size >= 10)) add("vestidor_pro")
            if (cumple("leyenda_estilo", piezasDesbloqueadas.any {
                    (AvatarContentFactory.porId(it)?.price ?: 0) >= 400
                })) {
                add("leyenda_estilo")
            }
            if (cumple("fan_kuromi", "rostro_kuromi" in piezasDesbloqueadas)) add("fan_kuromi")
            if (cumple("mano_magica", e.carinos >= 20)) add("mano_magica")
            if (cumple("chef_zazoles", e.alimentos >= 10)) add("chef_zazoles")
            if (cumple("cazabugs", e.bugs >= 25)) add("cazabugs")
            if (cumple("maestro_algoritmo", e.nivelesAlgoritmo >= 3)) add("maestro_algoritmo")
            if (cumple("guardia_ciberseguro", e.escenariosCiber >= 12)) add("guardia_ciberseguro")
            if (cumple("decorador_experto", evento == EventoLogro.WIDGET_AGREGADO)) {
                add("decorador_experto")
            }
            if (cumple("dj_zazili", evento == EventoLogro.MUSICA_CAMBIADA)) add("dj_zazili")
        }

        return desbloqueados.mapNotNull(CatalogoLogros::porId)
    }

    /** Paga Zazoles y regala la pieza de avatar asociada al logro. */
    private suspend fun otorgarRecompensa(logro: Achievement) {
        runCatching {
            val economia = ZazolEconomyManager.obtener(app)
            if (logro.rewardZazoles > 0) economia.addZazoles(logro.rewardZazoles)
            logro.rewardItemId?.let { id ->
                economia.unlockItem(id, 0)
            }
        }.onFailure { Log.w(ETIQUETA, "No se pudo entregar la recompensa de ${logro.id}", it) }
    }

    companion object {
        private const val ETIQUETA = "LogrosZazili"

        private val CLAVE_DESBLOQUEADOS = stringSetPreferencesKey("logros_desbloqueados")
        private val CONT_MINIJUEGOS = intPreferencesKey("cont_minijuegos")
        private val CONT_LECCIONES = intPreferencesKey("cont_lecciones")
        private val CONT_ACTIVIDADES = intPreferencesKey("cont_actividades")
        private val CONT_MUNDOS = stringSetPreferencesKey("cont_mundos_visitados")
        private val CONT_GASTADO = intPreferencesKey("cont_gastado")
        private val CONT_GANADO = intPreferencesKey("cont_ganado")
        private val CONT_COMPRAS_AVATAR = intPreferencesKey("cont_compras_avatar")
        private val CONT_CARINOS = intPreferencesKey("cont_carinos")
        private val CONT_ALIMENTOS = intPreferencesKey("cont_alimentos")
        private val CONT_BUGS = intPreferencesKey("cont_bugs")
        private val CONT_NIVEL_ALGORITMO = intPreferencesKey("cont_nivel_algoritmo")
        private val CONT_ESCENARIOS_CIBER = intPreferencesKey("cont_escenarios_ciber")
        private val CONT_NIVELES_MODULO = intPreferencesKey("cont_niveles_modulo")

        @Volatile
        private var instancia: AchievementEngine? = null

        fun obtener(context: Context): AchievementEngine =
            instancia ?: synchronized(this) {
                instancia ?: AchievementEngine(context.applicationContext).also { instancia = it }
            }
    }
}

/**
 * Confeti local de logros: se coloca sobre una pantalla (tablero, boutique,
 * galería) y escucha [AchievementEngine.celebraciones]. El aviso textual, el
 * sonido y la vibración viven una sola vez en la raíz de la app
 * ([AchievementUnlockedBanner]); aquí solo queda la lluvia de papelitos para
 * no mostrar dos banners del mismo logro. En Modo Zen el confeti no sale.
 */
@Composable
fun ObservadorLogros(modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val engine = remember(contexto) { AchievementEngine.obtener(contexto) }

    var disparoConfeti by remember { mutableIntStateOf(0) }

    LaunchedEffect(engine) {
        engine.celebraciones.collect { disparoConfeti += 1 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CelebrationKonfetti(
            trigger = disparoConfeti,
            modifier = Modifier.fillMaxSize()
        )
    }
}
