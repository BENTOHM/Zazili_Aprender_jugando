package com.example.zazili.core.navigation

import com.example.zazili.domain.modulos.ModuloZazili

/**
 * Rutas de Zazili 2.0: casita, cuatro pestañas de aprendizaje, lección y
 * Nido de adultos. La saga global ya no existe: cada pestaña tiene su propio
 * mapa y sus propias lecciones. El diagnóstico vive solo en la compuerta de
 * primera vez y no tiene ruta aquí.
 */
object Destinos {
    const val ARG_MODULO = "modulo"
    const val ARG_NODO = "nodo"

    const val HOGAR = "hogar"
    const val APRENDER = "aprender?modulo={" + ARG_MODULO + "}"
    const val NIDO = "nido"

    const val LECCION = "leccion/{" + ARG_MODULO + "}?nodo={" + ARG_NODO + "}"

    // Rutas de las cinco pestañas. La barra inferior y el NavHost de
    // MainNavigation comparten estas constantes: un typo es imposible.
    const val TAB_MATEMATICAS = "tab/matematicas"
    const val TAB_LENGUAJE = "tab/lenguaje"
    const val TAB_EMOCIONES = "tab/emociones"
    const val TAB_CIENCIAS = "tab/ciencias"
    const val TAB_TECNOLOGIA = "tab/tecnologia"
    const val TAB_ARENA_CORTEX = "tab/arena_cortex"

    val RUTAS_APRENDIZAJE: Set<String> = setOf(
        TAB_MATEMATICAS,
        TAB_LENGUAJE,
        TAB_EMOCIONES,
        TAB_CIENCIAS,
        TAB_TECNOLOGIA,
        TAB_ARENA_CORTEX
    )

    /** Ruta de la pestaña que corresponde a una materia. */
    fun tabDeModulo(modulo: ModuloZazili): String = when (modulo) {
        ModuloZazili.TIANGUIS_NUMERICO -> TAB_MATEMATICAS
        ModuloZazili.CUEVA_LEYENDAS -> TAB_LENGUAJE
        ModuloZazili.ARBOL_ABRAZOS -> TAB_EMOCIONES
        ModuloZazili.LABORATORIO_CENOTE -> TAB_CIENCIAS
        ModuloZazili.PIRAMIDE_CODIGO -> TAB_TECNOLOGIA
        else -> TAB_MATEMATICAS
    }

    /** Abre la barra de pestañas, opcionalmente en el mundo sugerido. */
    fun aprender(modulo: ModuloZazili? = null): String =
        if (modulo == null) "aprender" else "aprender?modulo=${modulo.name}"

    fun leccion(modulo: ModuloZazili, nodoId: String? = null): String {
        val base = "leccion/${modulo.name}"
        return if (nodoId.isNullOrBlank()) base else "$base?nodo=$nodoId"
    }

    fun moduloDesdeRuta(ruta: String?): ModuloZazili? =
        ModuloZazili.entries.firstOrNull { it.name == ruta }
}
