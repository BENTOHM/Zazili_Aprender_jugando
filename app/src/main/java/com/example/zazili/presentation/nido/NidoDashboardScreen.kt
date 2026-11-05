package com.example.zazili.presentation.nido

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.zazili.core.accessibility.collectZenMode
import com.example.zazili.core.accessibility.zenMutedBackground
import com.example.zazili.core.accessibility.zenVariantFor
import com.example.zazili.core.audio.GlobalAudioManager
import com.example.zazili.core.ui.KawaiiArrowIcon
import com.example.zazili.core.ui.KawaiiCheckIcon
import com.example.zazili.core.ui.KawaiiLockIcon
import com.example.zazili.core.ui.ZaziButton
import com.example.zazili.domain.nido.BienestarReporte
import com.example.zazili.domain.nido.CategoriaAprendizaje
import com.example.zazili.domain.nido.ChildProfileReport
import com.example.zazili.domain.nido.MetricaAprendizaje
import com.example.zazili.domain.nido.NidoAnalyticsEngine
import com.example.zazili.domain.nido.PerfilGeneral
import com.example.zazili.presentation.avatar.ChildAvatarRenderer
import com.example.zazili.presentation.modulos.comun.IconoModulo
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoMedio
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reporte profesional del Nido para familias, docentes y psicólogos.
 *
 * La entrada está protegida por una multiplicación sencilla: el niño no debe
 * leer sus propias métricas emocionales por accidente. Una vez adentro, todo
 * el panel observa un único [ChildProfileReport] con
 * `collectAsStateWithLifecycle()`, así que se actualiza mientras el niño juega.
 */
@Composable
fun NidoDashboardScreen(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    var adultoVerificado by rememberSaveable { mutableStateOf(false) }
    val contexto = LocalContext.current
    val motor = remember(contexto) { NidoAnalyticsEngine.obtener(contexto) }
    val reporte by motor.reporte.collectAsStateWithLifecycle()

    if (!adultoVerificado) {
        PantallaBloqueoNido(onSalir = onSalir, modifier = modifier)
        AdultGateDialog(
            visible = true,
            onDesbloquear = { adultoVerificado = true },
            onCancelar = {
                GlobalAudioManager.playPop()
                onSalir()
            }
        )
        return
    }

    ContenidoDashboard(
        reporte = reporte,
        onSalir = onSalir,
        modifier = modifier
    )
}

@Composable
private fun PantallaBloqueoNido(
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(UniversoProfundo, UniversoMedio, MagicalPurple)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(ZaziliWhite.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                KawaiiLockIcon(size = 46.dp, color = AmarilloSuave)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "El Nido está protegido",
                style = MaterialTheme.typography.headlineMedium,
                color = ZaziliWhite,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Este reporte contiene métricas de aprendizaje y bienestar " +
                    "emocional. Solo un adulto debe leerlo.",
                style = MaterialTheme.typography.bodyMedium,
                color = ZaziliWhite.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            ZaziButton(
                text = "Volver a la casita",
                onClick = {
                    GlobalAudioManager.playPop()
                    onSalir()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                containerColor = ZaziliWhite,
                contentColor = UniversoProfundo,
                cornerRadius = 20.dp,
                baseHeight = 5.dp,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ContenidoDashboard(
    reporte: ChildProfileReport,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zen = collectZenMode()
    val fondo = if (zen) {
        zenMutedBackground(zenVariantFor(MaterialTheme.colorScheme.background))
    } else {
        MaterialTheme.colorScheme.background
    }
    val hora = remember(reporte.generadoEn) {
        if (reporte.generadoEn <= 0L) {
            "Sincronizando…"
        } else {
            SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-MX"))
                .format(Date(reporte.generadoEn))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(fondo)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BarraDashboard(hora = hora, onSalir = onSalir)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(2.dp))
                SeccionPerfilGeneral(
                    perfil = reporte.perfil,
                    animarAvatar = !zen
                )
                SeccionBienestar(bienestar = reporte.bienestar)
                SeccionAprendizaje(metricas = reporte.metricas)
                SeccionReportePsicologico(reporte = reporte)
                PieLegal()
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun BarraDashboard(
    hora: String,
    onSalir: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ZaziButton(
            text = "Volver a la casita",
            onClick = {
                GlobalAudioManager.playPop()
                onSalir()
            },
            modifier = Modifier.size(46.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            cornerRadius = 23.dp,
            baseHeight = 4.dp
        ) {
            KawaiiArrowIcon(
                modifier = Modifier.graphicsLayer { rotationZ = 180f },
                size = 18.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "El Nido",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Reporte general · $hora",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = VerdeDuo.copy(alpha = 0.16f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(VerdeDuo)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "En vivo",
                    style = MaterialTheme.typography.labelSmall,
                    color = VerdeDuo,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TarjetaSeccion(
    titulo: String,
    subtitulo: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = 22.dp)
                        .clip(CircleShape)
                        .background(MagicalPurple)
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SeccionPerfilGeneral(
    perfil: PerfilGeneral,
    animarAvatar: Boolean
) {
    TarjetaSeccion(
        titulo = "1 · Perfil general",
        subtitulo = "Identidad, nivel y dificultad adaptativa"
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LavandaClara.copy(alpha = 0.55f)
            ) {
                ChildAvatarRenderer(
                    equipados = perfil.avatarEquipado,
                    modifier = Modifier.size(118.dp),
                    descripcion = "Avatar de ${perfil.nombre}",
                    animar = animarAvatar
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = perfil.nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${perfil.edad} años · ${perfil.grado}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipEtiqueta("Nivel ${perfil.nivel}", MagicalPurple)
                    ChipEtiqueta("${perfil.zazoles} zazoles", AmarilloVibrante)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Dificultad adaptativa ${perfil.dificultad.complejidad}/5",
                    style = MaterialTheme.typography.labelMedium,
                    color = MagicalPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Progreso de experiencia",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        val avanceXp by animateFloatAsState(
            targetValue = perfil.avanceXp.coerceIn(0f, 1f),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "xpNido"
        )
        LinearProgressIndicator(
            progress = { avanceXp },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = MagicalPurple,
            trackColor = MagicalPurple.copy(alpha = 0.14f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${perfil.xpEnNivel} / ${perfil.xpNecesaria} XP en este nivel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatoClave(
                etiqueta = "Dificultad",
                valor = "${perfil.dificultad.complejidad}/5",
                color = MagicalPurple,
                modifier = Modifier.weight(1f)
            )
            DatoClave(
                etiqueta = "Arena",
                valor = "${perfil.dificultad.nivelArena}/5",
                color = VerdeDuo,
                modifier = Modifier.weight(1f)
            )
            DatoClave(
                etiqueta = "Ritmo",
                valor = "${perfil.dificultad.velocidadPct}%",
                color = CoralAviso,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipEtiqueta(
                texto = if (perfil.dificultad.apoyoAudio) "Audio activo" else "Audio apagado",
                color = if (perfil.dificultad.apoyoAudio) VerdeDuo else CoralAviso
            )
            ChipEtiqueta(
                texto = if (perfil.dificultad.calibrada) "Calibración completa" else "Calibración pendiente",
                color = if (perfil.dificultad.calibrada) VerdeDuo else AmarilloVibrante
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Perfil de frustración: ${perfil.dificultad.perfilFrustracion}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DatoClave(
    etiqueta: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun ChipEtiqueta(
    texto: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SeccionBienestar(bienestar: BienestarReporte) {
    TarjetaSeccion(
        titulo = "2 · Estado de Zazi",
        subtitulo = "Bienestar y responsabilidades del cuidado"
    ) {
        Text(
            text = bienestar.mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(14.dp))
        BarraVital(
            emoji = "⚡",
            etiqueta = "Energía",
            valor = bienestar.energia,
            color = AmarilloVibrante
        )
        Spacer(Modifier.height(11.dp))
        BarraVital(
            emoji = "🍎",
            etiqueta = "Alimentación",
            valor = bienestar.alimentacion,
            color = CoralAviso
        )
        Spacer(Modifier.height(11.dp))
        BarraVital(
            emoji = "🫧",
            etiqueta = "Higiene",
            valor = bienestar.higiene,
            color = AzulCielo
        )
        Spacer(Modifier.height(11.dp))
        BarraVital(
            emoji = "💖",
            etiqueta = "Felicidad",
            valor = bienestar.felicidad,
            color = VerdeDuo
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Las barras se recalculan con el paso del tiempo real, aunque " +
                "la app esté cerrada.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun BarraVital(
    emoji: String,
    etiqueta: String,
    valor: Int,
    color: Color
) {
    val animado by animateFloatAsState(
        targetValue = (valor / 100f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "vital$etiqueta"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = etiqueta,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$valor%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.16f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animado)
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun SeccionAprendizaje(metricas: List<MetricaAprendizaje>) {
    TarjetaSeccion(
        titulo = "3 · Métricas de aprendizaje",
        subtitulo = "Precisión, dificultad y constancia por materia"
    ) {
        metricas.forEachIndexed { indice, metrica ->
            FilaMetrica(metrica)
            if (indice < metricas.lastIndex) {
                Spacer(Modifier.height(15.dp))
            }
        }
        Spacer(Modifier.height(13.dp))
        Text(
            text = "Fortaleza: más de 80% de aciertos. Área de oportunidad: menos de " +
                "50% de aciertos o muchos reintentos.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun FilaMetrica(metrica: MetricaAprendizaje) {
    val color = colorDeCategoria(metrica.categoria)
    val avance by animateFloatAsState(
        targetValue = metrica.puntaje.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "metrica${metrica.categoria.name}"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                IconoModulo(metrica.categoria.modulo, 20.dp)
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metrica.categoria.etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (!metrica.conDatos) {
                        "Sin actividad registrada"
                    } else {
                        "${metrica.precisionPct}% de aciertos · Nivel " +
                            "${metrica.nivelPromedio}/5"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.width(8.dp))
            EstadoMetrica(metrica)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { avance },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = color,
            trackColor = color.copy(alpha = 0.14f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Saga ${metrica.nodosCompletados}/${metrica.nodosTotales} · " +
                "Reintentos ${metrica.reintentoPct}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun EstadoMetrica(metrica: MetricaAprendizaje) {
    when {
        metrica.fortaleza -> ChipEtiqueta("Fortaleza", VerdeDuo)
        metrica.areaOportunidad -> ChipEtiqueta("A acompañar", CoralAviso)
        !metrica.conDatos -> ChipEtiqueta(
            texto = "Sin datos",
            color = MaterialTheme.colorScheme.outline
        )
        else -> ChipEtiqueta("En progreso", MagicalPurple)
    }
}

private fun colorDeCategoria(categoria: CategoriaAprendizaje): Color = when (categoria) {
    CategoriaAprendizaje.MATEMATICAS -> VerdeDuo
    CategoriaAprendizaje.LECTURA -> MagicalPurple
    CategoriaAprendizaje.EMOCIONES -> CoralAviso
    CategoriaAprendizaje.CIENCIAS -> AmarilloVibrante
}

@Composable
private fun SeccionReportePsicologico(reporte: ChildProfileReport) {
    TarjetaSeccion(
        titulo = "4 · Reporte psicológico",
        subtitulo = "Lectura pedagógica, nunca un diagnóstico clínico"
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MagicalPurple.copy(alpha = 0.10f)
        ) {
            Text(
                text = reporte.resumen,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 21.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        SubtituloReporte("Fortalezas detectadas", VerdeDuo)
        Spacer(Modifier.height(8.dp))
        if (reporte.fortalezas.isEmpty()) {
            Text(
                text = "Todavía no hay una materia por encima del 80% de aciertos. " +
                    "La constancia traerá la primera fortaleza.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                lineHeight = 19.sp
            )
        } else {
            reporte.fortalezas.forEach { fortaleza ->
                ListaHallazgo(texto = fortaleza, color = VerdeDuo)
            }
        }
        Spacer(Modifier.height(16.dp))
        SubtituloReporte("Áreas de mejora", CoralAviso)
        Spacer(Modifier.height(8.dp))
        if (reporte.areasOportunidad.isEmpty()) {
            Text(
                text = "No aparecen áreas de riesgo en los datos actuales. Repasen " +
                    "cualquier materia sin premio: practicar también cuenta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                lineHeight = 19.sp
            )
        } else {
            reporte.areasOportunidad.forEach { area ->
                ListaHallazgo(texto = area, color = CoralAviso)
            }
        }
        Spacer(Modifier.height(16.dp))
        SubtituloReporte("Recomendaciones para casa y aula", MagicalPurple)
        Spacer(Modifier.height(8.dp))
        reporte.recomendaciones.forEachIndexed { indice, recomendacion ->
            FilaRecomendacion(numero = indice + 1, texto = recomendacion)
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AmarilloSuave.copy(alpha = 0.35f)
        ) {
            Text(
                text = "Este reporte es una brújula pedagógica. No sustituye una " +
                    "evaluación profesional ni debe usarse para etiquetar al niño.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SubtituloReporte(titulo: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ListaHallazgo(texto: String, color: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            KawaiiCheckIcon(size = 12.dp, color = color)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = texto,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun FilaRecomendacion(numero: Int, texto: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MagicalPurple.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$numero",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            text = texto,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun PieLegal() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Reporte generado en este dispositivo. Sin cuentas, sin nube " +
                "y sin anuncios.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "El Nido no etiqueta al niño: describe señales para acompañarlo.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}
