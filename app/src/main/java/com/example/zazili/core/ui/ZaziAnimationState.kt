package com.example.zazili.core.ui

import com.example.zazili.core.mascot.EstadoVital

/**
 * Pose visual de Zazi, la colibrí pixel-art.
 *
 * Cada estado define UNA intención emocional y un nivel de movimiento, salvo
 * [BASE], que es la lámina frontal de presentación (alas abiertas y cola en
 * abanico). El motor de [ZaziSpriteAnimado] nunca mezcla dos poses:
 * [androidx.compose.animation.AnimatedContent] hace el cruce y el lienzo se
 * detiene cuando la mascota no está activa.
 */
enum class ZaziState(val descripcion: String) {
    VUELO_IDLE("Zazi, la colibrí, vuela contenta"),
    CELEBRACION("¡Zazi celebra contigo!"),
    PENSANDO("Zazi está pensando"),
    DURMIENDO("Zazi duerme con su piyama"),
    TRISTE("Zazi está triste y te extraña"),
    BASE("Zazi, la colibrí, con las alas abiertas")
}

/**
 * Traduce el Tamagotchi a una pose sin castigos ni dramatismo: la pancita baja
 * se comunica con ternura, nunca con hambre extrema. [ZaziState.BASE] no sale
 * de aquí: es la pose de presentación que eligen las pantallas de bienvenida.
 */
fun estadoZaziPara(
    estado: EstadoVital,
    celebrando: Boolean = false,
    pensando: Boolean = false
): ZaziState = when {
    celebrando -> ZaziState.CELEBRACION
    estado.dormida -> ZaziState.DURMIENDO
    pensando -> ZaziState.PENSANDO
    estado.pancita < UMBRAL_TRISTE || estado.felicidad < UMBRAL_TRISTE ->
        ZaziState.TRISTE
    estado.energia < UMBRAL_SUEÑO -> ZaziState.DURMIENDO
    else -> ZaziState.VUELO_IDLE
}

private const val UMBRAL_TRISTE = 0.30f
private const val UMBRAL_SUEÑO = 0.20f
