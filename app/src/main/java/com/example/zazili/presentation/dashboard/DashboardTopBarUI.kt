package com.example.zazili.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zazili.core.racha.EstadoRacha
import com.example.zazili.core.racha.EstadoWidgetRacha
import com.example.zazili.core.racha.IconoFlamaPixel
import com.example.zazili.ui.theme.AmarilloSuave
import com.example.zazili.ui.theme.AzulCielo
import com.example.zazili.ui.theme.CoralAviso
import com.example.zazili.ui.theme.LavandaClara
import com.example.zazili.ui.theme.LavandaMedia
import com.example.zazili.ui.theme.RosaSuave
import com.example.zazili.ui.theme.UniversoProfundo

/**
 * Píldora de racha del Top Bar: flama + número en una sola fila centrada.
 *
 * El bug anterior venía de un `Modifier.size(46.dp)` que recortaba un contenido
 * más ancho que su caja. Aquí la superficie se ajusta al contenido
 * (`heightIn(min = 44.dp)` garantiza el blanco táctil) y la fila usa
 * [Alignment.CenterVertically] con padding propio, así el número nunca se
 * corta aunque llegue a tres cifras.
 *
 * Los cuatro estados del motor de racha se reflejan en el fondo y el borde:
 * lavanda (inactiva), amarillo (activa), rosa (peligro) y azul hielo
 * (congelada). El número siempre se pinta en negro profundo para mantener el
 * contraste AA sobre cualquiera de esos fondos.
 */
@Composable
fun BotonFlamaRacha(
    estado: EstadoRacha,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo: Color = when (estado.estadoWidget) {
        EstadoWidgetRacha.INACTIVA -> LavandaClara
        EstadoWidgetRacha.ACTIVA -> AmarilloSuave
        EstadoWidgetRacha.PELIGRO -> RosaSuave
        EstadoWidgetRacha.CONGELADA -> AzulCielo
    }
    val borde: Color = when (estado.estadoWidget) {
        EstadoWidgetRacha.INACTIVA -> LavandaMedia
        EstadoWidgetRacha.ACTIVA -> AmarilloSuave
        EstadoWidgetRacha.PELIGRO -> CoralAviso
        EstadoWidgetRacha.CONGELADA -> AzulCielo
    }
    val dias = estado.currentStreak
    val descripcion = buildString {
        append(if (dias == 1) "Racha de 1 día. " else "Racha de $dias días. ")
        append(estado.estadoWidget.descripcion)
        append(" Toca para abrir tu racha.")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = fondo,
        border = BorderStroke(2.dp, borde),
        modifier = modifier
            .heightIn(min = 44.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = "Abrir mi racha"
            ) { onClick() }
            .semantics { contentDescription = descripcion }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            IconoFlamaPixel(
                activa = estado.jugoHoy,
                size = 24.dp
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "$dias",
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Black,
                color = UniversoProfundo,
                maxLines = 1,
                softWrap = false
            )
            if (estado.estadoWidget == EstadoWidgetRacha.CONGELADA) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "❄",
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = UniversoProfundo,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Etiqueta corta del estado para lectores de pantalla y hojas informativas.
 * Vive junto al botón para que ambos usen exactamente la misma semántica.
 */
internal fun etiquetaRacha(estado: EstadoRacha): String =
    when (estado.estadoWidget) {
        EstadoWidgetRacha.INACTIVA -> "Lista para encender"
        EstadoWidgetRacha.ACTIVA -> "Racha viva"
        EstadoWidgetRacha.PELIGRO -> "¡En peligro!"
        EstadoWidgetRacha.CONGELADA -> "Protegida con hielo"
    }
