package com.example.zazili.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.zazili.core.ajustes.TemaPreferido
import com.example.zazili.core.tiempo.DayPhase
import com.example.zazili.core.tiempo.recordarFaseDelDia

private val EsquemaClaro = lightColorScheme(
    primary = MagicalPurple,
    onPrimary = ZaziliWhite,
    primaryContainer = LavandaClara,
    onPrimaryContainer = UniversoProfundo,

    secondary = VerdeDuo,
    onSecondary = ZaziliWhite,
    secondaryContainer = VerdeDuoClaro.copy(alpha = 0.35f),
    onSecondaryContainer = Color(0xFF173D00),

    tertiary = AmarilloVibrante,
    onTertiary = UniversoProfundo,
    tertiaryContainer = AmarilloSuave,
    onTertiaryContainer = Color(0xFF3A2A00),

    background = SoftCream,
    onBackground = Color(0xFF2E1B3A),

    surface = ZaziliWhite,
    onSurface = Color(0xFF2E1B3A),
    surfaceVariant = LavandaClara,
    onSurfaceVariant = Color(0xFF5A4A68),

    outline = LavandaMedia,
    outlineVariant = LavandaClara,

    error = CoralAviso,
    onError = ZaziliWhite
)

private val EsquemaOscuro = darkColorScheme(
    primary = AmarilloVibrante,
    onPrimary = UniversoProfundo,
    primaryContainer = UniversoMedio,
    onPrimaryContainer = NightTextPrimary,

    secondary = VerdeDuoClaro,
    onSecondary = UniversoProfundo,
    secondaryContainer = Color(0xFF254516),
    onSecondaryContainer = NightTextPrimary,

    tertiary = LavandaMedia,
    onTertiary = UniversoProfundo,
    tertiaryContainer = NightSurfaceHigh,
    onTertiaryContainer = NightTextPrimary,

    background = NightBackground,
    onBackground = NightTextPrimary,

    surface = NightSurface,
    onSurface = NightTextPrimary,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightTextSecondary,

    outline = NightTextSecondary.copy(alpha = 0.45f),
    outlineVariant = NightTextSecondary.copy(alpha = 0.18f),

    error = NightError,
    onError = UniversoProfundo
)

/**
 * Tema único de Zazili. Primero manda lo que eligió el niño en
 * [TemaPreferido]: Día fija claro, Noche fija oscuro. Solo en Automático se
 * consulta el reloj de Zazi ([DayPhase]): amanecer/día claros, tarde/noche
 * oscuros. El modo oscuro del sistema Android nunca participa.
 */
@Composable
fun ZaziliTheme(
    faseDelDia: DayPhase = recordarFaseDelDia(),
    preferencia: TemaPreferido = TemaPreferido.AUTO,
    content: @Composable () -> Unit
) {
    val oscuro = when (preferencia) {
        TemaPreferido.CLARO -> false
        TemaPreferido.OSCURO -> true
        TemaPreferido.AUTO -> faseDelDia.esOscura
    }
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = ZaziliTypography,
        content = content
    )
}
