package com.example.zazili.presentation.saga

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.progress.GameProgress
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiStarIcon
import com.example.zazili.core.ui.ZaziSpriteAnimado
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.core.ui.ZaziState
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.domain.saga.EstadoSaga
import com.example.zazili.domain.saga.MundoSaga
import com.example.zazili.domain.saga.NodoSaga
import com.example.zazili.domain.saga.RutaSaga
import com.example.zazili.presentation.modulos.comun.IconoModulo
import com.example.zazili.presentation.modulos.comun.nombreCortoDeModulo
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.MintGreen
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import kotlin.math.sin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
//  GEOMETRÍA DEL CAMINO
//  Todo el mapa comparte estas medidas: la fila compuesta y el Canvas dibujan
//  con el mismo sistema de coordenadas, así los nodos siempre caen sobre la
//  línea punteada, sin importar el tamaño de pantalla.
// ---------------------------------------------------------------------------

internal val MARGEN_SUPERIOR = 6.dp
internal val ALTO_BLOQUE_CABECERA = 164.dp
internal val ALTO_FILA_NODO = 192.dp
internal val NODO_Y_EN_FILA = 68.dp
internal val TAMANO_NODO = 76.dp

/** Amplitud del serpenteo, como fracción del recorrido horizontal libre. */
internal const val AMPLITUD_SENO = 0.62f

/** Frecuencia de la onda: un "S" continuo a lo largo de toda la saga. */
internal const val FRECUENCIA_SENO = 0.8

/** Un reto ya colocado en la ruta plana de la saga. */
internal data class NodoEnRuta(
    val mundo: MundoSaga,
    val mundoIndice: Int,
    val nodo: NodoSaga,
    val indiceGlobal: Int
)

/** Posición final de un nodo: desplazamiento X respecto al centro y centro Y. */
internal data class NodoColocado(
    val item: NodoEnRuta,
    val desplazamientoX: Dp,
    val yCentro: Dp
)

/** Aplana los mundos en una sola ruta, conservando el orden del currículo. */
internal fun construirRuta(mundos: List<MundoSaga> = RutaSaga.mundos): List<NodoEnRuta> {
    val salida = ArrayList<NodoEnRuta>(mundos.sumOf { it.nodos.size })
    var global = 0
    mundos.forEachIndexed { indiceMundo, mundo ->
        mundo.nodos.forEach { nodo ->
            salida += NodoEnRuta(
                mundo = mundo,
                mundoIndice = indiceMundo,
                nodo = nodo,
                indiceGlobal = global
            )
            global++
        }
    }
    return salida
}

/**
 * Número con el que [com.example.zazili.core.progress.UserProgressManager]
 * guarda el desbloqueo de cada nodo.
 *
 * Todos los currículos terminan su id en el número de lección (`mate_07`,
 * `emociones_42`). Tecnología guarda por bloques de cinco, y sus nodos ya se
 * llaman por bloque (`piramide_2`, `tecnologia_bloque_2`), así que el número
 * final del id ES la llave de desbloqueo en los cinco módulos.
 */
internal fun numeroDeLeccion(nodoId: String): Int? =
    nodoId.trim().substringAfterLast('_').toIntOrNull()

/**
 * Desplazamiento horizontal del nodo según una onda senoidal:
 * `sin(indice * frecuencia) * amplitud`. El nodo nunca se sale del ancho útil.
 */
internal fun desplazamientoOndulado(indice: Int, anchoMapa: Dp): Dp {
    val recorrido = ((anchoMapa - TAMANO_NODO) / 2f).coerceAtLeast(0.dp)
    val onda = sin(indice * FRECUENCIA_SENO).toFloat()
    return recorrido * AMPLITUD_SENO * onda
}

/** Tramo curvo entre dos centros, con tangentes verticales en ambos extremos. */
internal fun caminoOndulado(desde: Offset, hasta: Offset): Path {
    val medioY = (desde.y + hasta.y) / 2f
    return Path().apply {
        moveTo(desde.x, desde.y)
        cubicTo(desde.x, medioY, hasta.x, medioY, hasta.x, hasta.y)
    }
}

/**
 * Calcula la posición de todos los nodos. Cada mundo abre con su cabecera y
 * después vienen sus retos, igual que en la composición de la pantalla.
 */
internal fun colocarRuta(
    anchoMapa: Dp,
    ruta: List<NodoEnRuta> = construirRuta()
): List<NodoColocado> {
    var y = MARGEN_SUPERIOR
    var mundoAnterior = -1
    return ruta.map { item ->
        if (item.mundoIndice != mundoAnterior) {
            mundoAnterior = item.mundoIndice
            y += ALTO_BLOQUE_CABECERA
        }
        val colocado = NodoColocado(
            item = item,
            desplazamientoX = desplazamientoOndulado(item.indiceGlobal, anchoMapa),
            yCentro = y + NODO_Y_EN_FILA + TAMANO_NODO / 2f
        )
        y += ALTO_FILA_NODO
        colocado
    }
}

/** Altura total del lienzo del camino (sin contar el pie de la pantalla). */
internal fun altoDelCamino(mundos: List<MundoSaga> = RutaSaga.mundos): Dp {
    val filas = mundos.sumOf { it.nodos.size }
    return MARGEN_SUPERIOR +
        ALTO_BLOQUE_CABECERA * mundos.size +
        ALTO_FILA_NODO * filas
}

/**
 * Dibuja el camino punteado detrás de los nodos. Los tramos que salen de un
 * reto completado van en verde; los pendientes, en crema/gris.
 */
internal fun DrawScope.dibujarCaminoOndulado(
    colocados: List<NodoColocado>,
    segmentosCompletados: List<Boolean>,
    colorPendiente: Color,
    colorCompletado: Color,
    haloCompletado: Color
) {
    if (colocados.size < 2) return
    val centros = colocados.map { colocado ->
        Offset(
            x = size.width / 2f + colocado.desplazamientoX.toPx(),
            y = colocado.yCentro.toPx()
        )
    }

    val pendiente = Path()
    val completado = Path()
    for (indice in 1 until centros.size) {
        val tramo = caminoOndulado(centros[indice - 1], centros[indice])
        if (segmentosCompletados.getOrElse(indice - 1) { false }) {
            completado.addPath(tramo)
        } else {
            pendiente.addPath(tramo)
        }
    }

    val grosor = 12.dp.toPx()
    val guiones = PathEffect.dashPathEffect(
        intervals = floatArrayOf(16.dp.toPx(), 14.dp.toPx()),
        phase = 0f
    )

    drawPath(
        path = pendiente,
        color = colorPendiente,
        style = Stroke(width = grosor, cap = StrokeCap.Round, pathEffect = guiones)
    )
    drawPath(
        path = completado,
        color = haloCompletado,
        style = Stroke(width = grosor * 2.1f, cap = StrokeCap.Round, pathEffect = guiones)
    )
    drawPath(
        path = completado,
        color = colorCompletado,
        style = Stroke(width = grosor, cap = StrokeCap.Round, pathEffect = guiones)
    )
}

/**
 * Lo que el mapa necesita de un ViewModel para pintarse. Matemáticas y Lengua
 * comparten exactamente el mismo lienzo; solo cambia de dónde salen los mundos
 * y qué nivel adaptativo se aplica al arrancar.
 */
interface FuenteMapaSaga {
    val estado: StateFlow<EstadoSaga>
    val progreso: StateFlow<GameProgress>
    val zazoles: StateFlow<Int>
    fun mundosDe(modulo: ModuloZazili): List<MundoSaga>
}

/**
 * El mapa de un módulo: un camino vertical que serpentea por sus propios nodos.
 *
 * Cada pestaña monta su propia instancia con su mundo filtrado, de modo que el
 * scroll y el avance de Matemáticas no se mezclan con los de Ciencias. Usa un
 * lienzo grande con scroll (en lugar de LazyColumn) para que la línea punteada
 * y los nodos compartan el mismo sistema de coordenadas. El reto actual es el
 * primer nodo sin completar; Zazi flota sobre él y el mapa se desplaza solo
 * hasta ahí. Los nodos bloqueados responden con humor, nunca con un error rojo.
 */
@Composable
fun SagaMapScreen(
    modulo: ModuloZazili,
    desbloqueoMaximo: Int,
    onVolver: () -> Unit,
    onAbrirModulo: (ModuloZazili, String?) -> Unit,
    onIrANido: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SagaMapViewModel = viewModel()
) {
    ContenidoMapaSaga(
        modulo = modulo,
        desbloqueoMaximo = desbloqueoMaximo,
        fuente = viewModel,
        onVolver = onVolver,
        onAbrirModulo = onAbrirModulo,
        onIrANido = onIrANido,
        modifier = modifier
    )
}

/**
 * Mapa de Lengua con arranque adaptativo propio: lee `nivelLenguaje` del
 * diagnóstico y coloca al niño en el reto que le toca sin tocar el avance de
 * Matemáticas, porque comparten el mismo [FuenteMapaSaga] pero cada pestaña
 * conserva su ViewModel y su nivel.
 */
@Composable
fun LanguageSagaMapScreen(
    desbloqueoMaximo: Int,
    onVolver: () -> Unit,
    onAbrirModulo: (ModuloZazili, String?) -> Unit,
    onIrANido: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LanguageSagaViewModel = viewModel()
) {
    ContenidoMapaSaga(
        modulo = ModuloZazili.CUEVA_LEYENDAS,
        desbloqueoMaximo = desbloqueoMaximo,
        fuente = viewModel,
        onVolver = onVolver,
        onAbrirModulo = onAbrirModulo,
        onIrANido = onIrANido,
        modifier = modifier
    )
}

/**
 * Mapa de Emociones (El Árbol de los Abrazos). Monta los cinco capítulos
 * socioemocionales y coloca al niño en el tramo que su tolerancia a la
 * frustración ya domina.
 */
@Composable
fun MentalHealthSagaMapScreen(
    desbloqueoMaximo: Int,
    onVolver: () -> Unit,
    onAbrirModulo: (ModuloZazili, String?) -> Unit,
    onIrANido: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MentalHealthSagaViewModel = viewModel()
) {
    ContenidoMapaSaga(
        modulo = ModuloZazili.ARBOL_ABRAZOS,
        desbloqueoMaximo = desbloqueoMaximo,
        fuente = viewModel,
        onVolver = onVolver,
        onAbrirModulo = onAbrirModulo,
        onIrANido = onIrANido,
        modifier = modifier
    )
}

/**
 * Mapa de Ciencias (El Laboratorio del Cenote). Monta los cinco capítulos
 * científicos y coloca al niño en el tramo que su nivel lógico ya domina,
 * calculado con la línea base del vuelo de prueba.
 */
@Composable
fun ScienceSagaMapScreen(
    desbloqueoMaximo: Int,
    onVolver: () -> Unit,
    onAbrirModulo: (ModuloZazili, String?) -> Unit,
    onIrANido: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScienceSagaViewModel = viewModel()
) {
    ContenidoMapaSaga(
        modulo = ModuloZazili.LABORATORIO_CENOTE,
        desbloqueoMaximo = desbloqueoMaximo,
        fuente = viewModel,
        onVolver = onVolver,
        onAbrirModulo = onAbrirModulo,
        onIrANido = onIrANido,
        modifier = modifier
    )
}

@Composable
private fun ContenidoMapaSaga(
    modulo: ModuloZazili,
    desbloqueoMaximo: Int,
    fuente: FuenteMapaSaga,
    onVolver: () -> Unit,
    onAbrirModulo: (ModuloZazili, String?) -> Unit,
    onIrANido: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by fuente.estado.collectAsStateWithLifecycle()
    val progreso by fuente.progreso.collectAsStateWithLifecycle()
    val zazoles by fuente.zazoles.collectAsStateWithLifecycle()
    val zen = collectZenMode()
    val oscuro = isSystemInDarkTheme()
    val fondo = if (zen) zenMutedBackground(oscuro) else MaterialTheme.colorScheme.background

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun avisar(texto: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texto)
        }
    }

    // Matemáticas y Lengua montan sus 5 capítulos curriculares; los demás
    // módulos conservan su mundo único. Cada pestaña tiene su propio avance.
    val mundos = remember(modulo, fuente) { fuente.mundosDe(modulo) }
    val ruta = remember(mundos) { construirRuta(mundos) }

    if (ruta.isEmpty()) {
        MapaSinRetos(
            modulo = modulo,
            onVolver = onVolver,
            modifier = modifier
                .fillMaxSize()
                .background(fondo)
        )
        return
    }

    val numeroModulo = remember(modulo) {
        (RutaSaga.mundos.indexOfFirst { it.modulo == modulo } + 1).coerceAtLeast(1)
    }
    val nodosPorMundo = remember(ruta) { ruta.groupBy { it.mundoIndice } }
    val indiceActual = remember(estado, ruta) {
        ruta.indexOfFirst { !estado.completado(it.nodo.id) }
    }
    val indiceZazi = if (indiceActual >= 0) indiceActual else ruta.lastIndex
    val sagaCompleta = indiceActual < 0 && ruta.isNotEmpty()
    val mapaCurricular = remember(mundos) { mundos.any { it.nombreCapitulo != null } }
    val estrellasGanadas = remember(estado, ruta) {
        ruta.sumOf { estado.estrellasDe(it.nodo.id) }
    }
    val totalEstrellas = ruta.size * 3
    val segmentosCompletados = remember(estado, ruta) {
        ruta.map { estado.completado(it.nodo.id) }
    }

    // El avance adaptativo siembra lecciones completadas sin pasar por
    // DataStore. El techo efectivo nunca queda por debajo de la última
    // completada + 1, así el primer reto pendiente siempre está abierto en
    // cualquiera de los cinco módulos.
    val desbloqueoEfectivo = remember(estado, ruta, desbloqueoMaximo) {
        val ultimaCompletada = ruta
            .filter { estado.completado(it.nodo.id) }
            .mapNotNull { numeroDeLeccion(it.nodo.id) }
            .maxOrNull()
        maxOf(desbloqueoMaximo, (ultimaCompletada ?: 0) + 1)
    }

    val colorCaminoPendiente = when {
        zen -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        oscuro -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color(0xFFDCD3C2)
    }
    val colorCaminoCompletado = if (zen) VerdeDuo.copy(alpha = 0.55f) else VerdeDuo
    val haloCamino = VerdeDuo.copy(alpha = if (zen) 0.08f else 0.16f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraSuperior(
                titulo = nombreCortoDeModulo(modulo),
                descripcion = if (mapaCurricular) {
                    "${ruta.size} retos · ${mundos.size} capítulos · Nivel ${progreso.level} · $zazoles zazoles"
                } else {
                    "Módulo $numeroModulo · Nivel ${progreso.level} · $zazoles zazoles"
                },
                estrellas = estrellasGanadas,
                totalEstrellas = totalEstrellas,
                onVolver = onVolver
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val anchoMapa = maxWidth
                val colocados = remember(ruta, anchoMapa) { colocarRuta(anchoMapa, ruta) }
                val altoMapa = remember(mundos) { altoDelCamino(mundos) }
                val scroll = rememberScrollState()
                val densidad = LocalDensity.current

                LaunchedEffect(colocados, indiceZazi) {
                    if (indiceZazi <= 1) return@LaunchedEffect
                    val actual = colocados.getOrNull(indiceZazi) ?: return@LaunchedEffect
                    val objetivo = with(densidad) {
                        (actual.yCentro - 220.dp).coerceAtLeast(0.dp).roundToPx()
                    }
                    scroll.animateScrollTo(objetivo)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(altoMapa)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            dibujarCaminoOndulado(
                                colocados = colocados,
                                segmentosCompletados = segmentosCompletados,
                                colorPendiente = colorCaminoPendiente,
                                colorCompletado = colorCaminoCompletado,
                                haloCompletado = haloCamino
                            )
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(Modifier.height(MARGEN_SUPERIOR))
                            mundos.forEachIndexed { indiceMundo, mundoDeLaRuta ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ALTO_BLOQUE_CABECERA)
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    CabeceraModulo(
                                        mundo = mundoDeLaRuta,
                                        numero = if (mundoDeLaRuta.nombreCapitulo != null) {
                                            indiceMundo + 1
                                        } else {
                                            (RutaSaga.mundos.indexOfFirst {
                                                it.modulo == mundoDeLaRuta.modulo
                                            } + 1).coerceAtLeast(1)
                                        },
                                        completados = estado.completadosEn(mundoDeLaRuta),
                                        estrellas = mundoDeLaRuta.nodos.sumOf {
                                            estado.estrellasDe(it.id)
                                        }
                                    )
                                }

                                nodosPorMundo[indiceMundo].orEmpty().forEach { item ->
                                    val colocacion = colocados.getOrNull(item.indiceGlobal)
                                    if (colocacion != null) {
                                        val numeroLeccion = numeroDeLeccion(item.nodo.id)
                                        // Regla única para los cinco módulos:
                                        // el nodo se abre si su número no pasa
                                        // del máximo persistido del módulo.
                                        val desbloqueadoPorDatos =
                                            numeroLeccion == null ||
                                                numeroLeccion <= desbloqueoEfectivo
                                        val estadoNodo = when {
                                            estado.completado(item.nodo.id) ->
                                                EstadoNodoMapa.COMPLETADO

                                            !desbloqueadoPorDatos ->
                                                EstadoNodoMapa.BLOQUEADO

                                            item.indiceGlobal == indiceActual ->
                                                EstadoNodoMapa.ACTUAL

                                            else -> EstadoNodoMapa.DISPONIBLE
                                        }
                                        FilaNodo(
                                            colocacion = colocacion,
                                            estadoNodo = estadoNodo,
                                            numero = item.indiceGlobal + 1,
                                            esZazi = item.indiceGlobal == indiceZazi,
                                            animar = !zen,
                                            onClick = {
                                                GlobalAudioManager.playPop()
                                                if (estadoNodo == EstadoNodoMapa.BLOQUEADO) {
                                                    val anterior = ruta
                                                        .getOrNull(item.indiceGlobal - 1)
                                                        ?.nodo
                                                    avisar(
                                                        if (anterior != null) {
                                                            "«${anterior.nombre}» todavía te está esperando. Termínalo y este se abre solito."
                                                        } else {
                                                            "Este reto abre en un ratito. Mientras, puedes repasar uno completado."
                                                        }
                                                    )
                                                } else {
                                                    onAbrirModulo(item.mundo.modulo, item.nodo.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AccesosFinales(
                        sagaCompleta = sagaCompleta,
                        onIrANido = onIrANido
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) { data ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AmarilloSuave,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = data.visuals.message,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BarraSuperior(
    titulo: String,
    descripcion: String,
    estrellas: Int,
    totalEstrellas: Int,
    onVolver: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 18.dp, top = 8.dp, bottom = 6.dp)
    ) {
        ZaziButton(
            text = "Volver a la casita",
            onClick = {
                GlobalAudioManager.playPop()
                onVolver()
            },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 24.dp,
            baseHeight = 4.dp
        ) {
            KawaiiArrowIcon(
                modifier = Modifier.graphicsLayer { rotationZ = 180f },
                size = 20.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = AmarilloSuave
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                KawaiiStarIcon(size = 16.dp, color = UniversoProfundo)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$estrellas/$totalEstrellas",
                    style = MaterialTheme.typography.labelMedium,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun CabeceraModulo(
    mundo: MundoSaga,
    numero: Int,
    completados: Int,
    estrellas: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(UniversoProfundo, MagicalPurple, MintGreen)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ZaziliWhite.copy(alpha = 0.08f),
                radius = size.height * 0.62f,
                center = Offset(size.width * 0.92f, size.height * 0.05f)
            )
            drawCircle(
                color = ZaziliWhite.copy(alpha = 0.06f),
                radius = size.height * 0.34f,
                center = Offset(size.width * 0.08f, size.height * 1.05f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ZaziliWhite.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                IconoModulo(mundo.modulo, 34.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (mundo.nombreCapitulo != null) {
                        "CAPÍTULO $numero"
                    } else {
                        "MÓDULO $numero"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MintGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = mundo.nombreCapitulo ?: mundo.modulo.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = ZaziliWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KawaiiStarIcon(size = 13.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "$estrellas/${mundo.nodos.size * 3}  ·  $completados/${mundo.nodos.size} retos",
                        style = MaterialTheme.typography.labelSmall,
                        color = ZaziliWhite.copy(alpha = 0.86f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaNodo(
    colocacion: NodoColocado,
    estadoNodo: EstadoNodoMapa,
    numero: Int,
    esZazi: Boolean,
    animar: Boolean,
    onClick: () -> Unit
) {
    val nodo = colocacion.item.nodo
    val descripcion = when (estadoNodo) {
        EstadoNodoMapa.COMPLETADO ->
            "«${nodo.nombre}», completado. Toca para repasar."
        EstadoNodoMapa.ACTUAL ->
            "«${nodo.nombre}», tu reto de hoy. Toca para empezar."
        EstadoNodoMapa.DISPONIBLE ->
            "«${nodo.nombre}», desbloqueado. Toca para empezar."
        EstadoNodoMapa.BLOQUEADO ->
            "«${nodo.nombre}», bloqueado. Se abre al terminar el reto anterior."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ALTO_FILA_NODO)
    ) {
        if (esZazi) {
            ZaziFlotante(
                animar = animar,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = colocacion.desplazamientoX)
            )
        }

        SagaNodeButton(
            estado = estadoNodo,
            onClick = onClick,
            numero = numero,
            size = TAMANO_NODO,
            animarPulso = animar,
            descripcion = descripcion,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    x = colocacion.desplazamientoX,
                    y = NODO_Y_EN_FILA
                )
        )

        if (estadoNodo == EstadoNodoMapa.ACTUAL) {
            Surface(
                shape = RoundedCornerShape(50),
                color = AmarilloSuave,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        x = colocacion.desplazamientoX,
                        y = NODO_Y_EN_FILA + TAMANO_NODO + TAMANO_NODO * 0.13f + 4.dp
                    )
            ) {
                Text(
                    text = "¡Te toca!",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = UniversoProfundo,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ZaziFlotante(
    animar: Boolean,
    modifier: Modifier = Modifier
) {
    ZaziSpriteAnimado(
        modifier = modifier.size(width = 68.dp, height = 62.dp),
        estado = ZaziState.VUELO_IDLE,
        animar = animar,
        descripcion = "Zazi te acompaña sobre el reto de hoy"
    )
}

@Composable
private fun MapaSinRetos(
    modulo: ModuloZazili,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(28.dp)
    ) {
        Text(
            text = "El mapa de ${nombreCortoDeModulo(modulo)} está tomando forma.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Zazi está acomodando los retos. Mientras, puedes volver a la casita.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        ZaziButton(
            text = "Volver a la casita",
            onClick = {
                GlobalAudioManager.playPop()
                onVolver()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = MagicalPurple,
            contentColor = ZaziliWhite,
            cornerRadius = 20.dp,
            baseHeight = 5.dp
        )
    }
}

@Composable
private fun AccesosFinales(
    sagaCompleta: Boolean,
    onIrANido: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp, bottom = 28.dp)
    ) {
        Text(
            text = if (sagaCompleta) {
                "¡Este mundo quedó completito!"
            } else {
                "¿Otro caminito hoy?"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (sagaCompleta) {
                "Zazi ya está inflando el papalote para la siguiente aventura."
            } else {
                "El bosque no corre y Zazi tampoco. Un reto hoy, otro mañana."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f)
        )
        Spacer(Modifier.height(16.dp))
        ZaziButton(
            text = "El Nido de los Grandes",
            onClick = {
                GlobalAudioManager.playPop()
                onIrANido()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            containerColor = MagicalPurple,
            contentColor = ZaziliWhite,
            cornerRadius = 20.dp,
            baseHeight = 5.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoModulo(ModuloZazili.NIDO_ADULTOS, 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "El Nido de los Grandes",
                    color = ZaziliWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "El Nido abre solo con el PIN de un adulto.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}
