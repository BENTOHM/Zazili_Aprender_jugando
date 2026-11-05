package com.example.zazili.presentation.modulos.comun

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zazili.core.ui.KawaiiBookIcon
import com.example.zazili.core.ui.KawaiiCenoteIcon
import com.example.zazili.core.ui.KawaiiCloudStressIcon
import com.example.zazili.core.ui.KawaiiCoinIcon
import com.example.zazili.core.ui.KawaiiGlitchIcon
import com.example.zazili.core.ui.KawaiiNahualIcon
import com.example.zazili.core.ui.KawaiiNestIcon
import com.example.zazili.core.ui.KawaiiPyramidIcon
import com.example.zazili.core.ui.KawaiiTacoIcon
import com.example.zazili.core.ui.KawaiiTrashMonsterIcon
import com.example.zazili.core.ui.KawaiiTreeIcon
import com.example.zazili.domain.modulos.ModuloZazili
import com.example.zazili.ui.theme.AmarilloVibrante
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.MagicalPurple
import com.example.zazili.ui.theme.UniversoBrillante
import com.example.zazili.ui.theme.UniversoProfundo
import com.example.zazili.ui.theme.VerdeDuo
import com.example.zazili.ui.theme.ZaziliWhite

/** Identidad visual compartida por la Saga y los hubs de módulo. */
fun colorDeModulo(modulo: ModuloZazili): Color = when (modulo) {
    ModuloZazili.TIANGUIS_NUMERICO -> AmarilloVibrante
    ModuloZazili.CUEVA_LEYENDAS -> LavandaMedia
    ModuloZazili.ARBOL_ABRAZOS -> VerdeDuo
    ModuloZazili.LABORATORIO_CENOTE -> AzulCielo
    ModuloZazili.PIRAMIDE_CODIGO -> UniversoBrillante
    ModuloZazili.NIDO_ADULTOS -> MagicalPurple
}

/** Nombre breve para la barra inferior y las pestañas de la app. */
fun nombreCortoDeModulo(modulo: ModuloZazili): String = when (modulo) {
    ModuloZazili.TIANGUIS_NUMERICO -> "Matemáticas"
    ModuloZazili.CUEVA_LEYENDAS -> "Lenguaje"
    ModuloZazili.ARBOL_ABRAZOS -> "Emociones"
    ModuloZazili.LABORATORIO_CENOTE -> "Ciencias"
    ModuloZazili.PIRAMIDE_CODIGO -> "Código"
    ModuloZazili.NIDO_ADULTOS -> "Nido"
}

/** Contraste seguro sobre el color del módulo. */
fun contenidoDe(color: Color): Color =
    if (color.luminance() > 0.55f) UniversoProfundo else ZaziliWhite

@Composable
fun IconoModulo(modulo: ModuloZazili, size: Dp = 30.dp) {
    when (modulo) {
        ModuloZazili.TIANGUIS_NUMERICO -> KawaiiTacoIcon(size = size)
        ModuloZazili.CUEVA_LEYENDAS -> KawaiiBookIcon(size = size)
        ModuloZazili.ARBOL_ABRAZOS -> KawaiiTreeIcon(size = size)
        ModuloZazili.LABORATORIO_CENOTE -> KawaiiCenoteIcon(size = size)
        ModuloZazili.PIRAMIDE_CODIGO -> KawaiiPyramidIcon(size = size)
        ModuloZazili.NIDO_ADULTOS -> KawaiiNestIcon(size = size)
    }
}

/** El jefe de cada mundo, dibujado como personaje kawaii. */
@Composable
fun IconoJefe(modulo: ModuloZazili, size: Dp = 26.dp) {
    when (modulo) {
        ModuloZazili.TIANGUIS_NUMERICO -> KawaiiCoinIcon(size = size)
        ModuloZazili.CUEVA_LEYENDAS -> KawaiiNahualIcon(size = size)
        ModuloZazili.ARBOL_ABRAZOS -> KawaiiCloudStressIcon(size = size)
        ModuloZazili.LABORATORIO_CENOTE -> KawaiiTrashMonsterIcon(size = size)
        ModuloZazili.PIRAMIDE_CODIGO -> KawaiiGlitchIcon(size = size)
        ModuloZazili.NIDO_ADULTOS -> KawaiiNestIcon(size = size)
    }
}

/** Fondo tenue del módulo, distinto en tema claro y oscuro. */
@Composable
fun fondoSuaveModulo(modulo: ModuloZazili): Color {
    val base = colorDeModulo(modulo)
    return base.copy(alpha = if (contenidoDe(base) == UniversoProfundo) 0.16f else 0.22f)
}
