package com.example.zazili.domain.saga

import com.example.zazili.domain.modulos.ModuloZazili

/**
 * Un reto breve de la Saga. Cada nodo es una sola pregunta sin cronómetro:
 * la recompensa es avanzar, nunca castigar el error. El contenido completo de
 * las lecciones llega en sus rondas; este primer set permite recorrer los seis
 * mundos de Zazili 2.0 de punta a punta.
 */
data class NodoSaga(
    val id: String,
    val nombre: String,
    val enunciado: String,
    val opciones: List<String>,
    val indiceCorrecto: Int,
    val pista: String,
    val esJefe: Boolean = false
)

/**
 * Un mundo de la Saga con su jefe y sus retos. [nombreCapitulo] solo lo usan
 * los capítulos curriculares de Matemáticas para rotular su tramo del mapa.
 */
data class MundoSaga(
    val modulo: ModuloZazili,
    val jefe: String,
    val dichoDelJefe: String,
    val nodos: List<NodoSaga>,
    val nombreCapitulo: String? = null
)

/** Estado de avance de la Saga (puro, sin Android). */
data class EstadoSaga(
    val completados: Set<String> = emptySet(),
    val estrellas: Map<String, Int> = emptyMap()
) {
    fun completado(id: String): Boolean = id in completados

    fun estrellasDe(id: String): Int = estrellas[id] ?: 0

    fun completadosEn(mundo: MundoSaga): Int = mundo.nodos.count { completado(it.id) }

    fun mundoCompletado(mundo: MundoSaga): Boolean = mundo.nodos.all { completado(it.id) }

    fun totalCompletados(): Int = completados.size
}

/**
 * Camino vertical de Zazili 2.0. Cinco mundos de aprendizaje y, al final,
 * el Nido de los Grandes. Los nodos se desbloquean en orden; el primero de
 * cada mundo siempre está abierto para que el niño elija por dónde empezar.
 */
object RutaSaga {

    val mundos: List<MundoSaga> = listOf(
        MundoSaga(
            modulo = ModuloZazili.TIANGUIS_NUMERICO,
            jefe = "El Señor de los Costales",
            dichoDelJefe = "Aquí se cuenta con frijoles, no con calculadora. ¡Échale ganas!",
            nodos = listOf(
                NodoSaga(
                    id = "tianguis_1",
                    nombre = "El mandado de doña Chelo",
                    enunciado = "Doña Chelo vende 3 elotes a $10 cada uno. ¿Cuánto le pagan?",
                    opciones = listOf("$13", "$30", "$40"),
                    indiceCorrecto = 1,
                    pista = "Tres veces diez es como tres monedas de diez."
                ),
                NodoSaga(
                    id = "tianguis_2",
                    nombre = "Cuenta de jitomates",
                    enunciado = "Había 12 jitomates y se vendieron 5. ¿Cuántos quedan en el cajón?",
                    opciones = listOf("5", "7", "17"),
                    indiceCorrecto = 1,
                    pista = "Si quitas 5 a 12, ¿qué número te queda?"
                ),
                NodoSaga(
                    id = "tianguis_3",
                    nombre = "Precio por taco",
                    enunciado = "4 tacos cuestan $40. ¿Cuánto cuesta cada taco?",
                    opciones = listOf("$4", "$10", "$12"),
                    indiceCorrecto = 1,
                    pista = "Reparte los $40 en 4 partes iguales."
                ),
                NodoSaga(
                    id = "tianguis_4",
                    nombre = "El Señor de los Costales",
                    enunciado = "El Señor de los Costales tiene 5 costales con 6 frijoles cada uno. ¿Cuántos frijoles son en total?",
                    opciones = listOf("11", "30", "56"),
                    indiceCorrecto = 1,
                    pista = "Cinco grupos de seis… puedes contar de seis en seis.",
                    esJefe = true
                )
            )
        ),
        MundoSaga(
            modulo = ModuloZazili.CUEVA_LEYENDAS,
            jefe = "El Nahual Trabalenguas",
            dichoDelJefe = "Si me entiendes, me ganas. Si no… ¡otra vez, sin pena!",
            nodos = listOf(
                NodoSaga(
                    id = "cueva_1",
                    nombre = "La primera sílaba",
                    enunciado = "¿Con qué sílaba empieza la palabra «cueva»?",
                    opciones = listOf("cu", "ca", "co"),
                    indiceCorrecto = 0,
                    pista = "Di despacio: cu-e-va. La primera parte suena «cu»."
                ),
                NodoSaga(
                    id = "cueva_2",
                    nombre = "Rimas de la cueva",
                    enunciado = "¿Cuál palabra rima con «sol»?",
                    opciones = listOf("col", "luna", "estrella"),
                    indiceCorrecto = 0,
                    pista = "Las palabras que riman terminan con un sonido parecido."
                ),
                NodoSaga(
                    id = "cueva_3",
                    nombre = "Sílabas del nahual",
                    enunciado = "¿Cuántas sílabas tiene «nahual»?",
                    opciones = listOf("Una", "Dos", "Tres"),
                    indiceCorrecto = 1,
                    pista = "Aplaude al decirlo: na-hual. Dos palmadas."
                ),
                NodoSaga(
                    id = "cueva_4",
                    nombre = "El Nahual Trabalenguas",
                    enunciado = "«El perro de San Roque no tiene rabo». ¿Cuántas letras «r» hay en total?",
                    opciones = listOf("2", "4", "6"),
                    indiceCorrecto = 1,
                    pista = "Cuenta las de pe-rro, Roque y rabo. Son cuatro."
                )
            )
        ),
        MundoSaga(
            modulo = ModuloZazili.ARBOL_ABRAZOS,
            jefe = "La Nube del Estrés",
            dichoDelJefe = "Vengo a nublarte el día… a menos que respires conmigo.",
            nodos = listOf(
                NodoSaga(
                    id = "arbol_1",
                    nombre = "Mariposas en la panza",
                    enunciado = "Sientes mariposas en la panza antes de exponer. ¿Qué emoción es?",
                    opciones = listOf("Nervios", "Alegría", "Aburrimiento"),
                    indiceCorrecto = 0,
                    pista = "Las mariposas suelen avisar que algo nos emociona o nos pone nerviosos."
                ),
                NodoSaga(
                    id = "arbol_2",
                    nombre = "Un abrazo a tiempo",
                    enunciado = "Tu amigo está triste. ¿Qué es lo más chido que puedes hacer?",
                    opciones = listOf("Escucharlo", "Reírte de él", "Ignorarlo"),
                    indiceCorrecto = 0,
                    pista = "A veces un oído atento abraza más que mil consejos."
                ),
                NodoSaga(
                    id = "arbol_3",
                    nombre = "Respira como globo",
                    enunciado = "¿Cuál respiración ayuda a calmar el cuerpo?",
                    opciones = listOf("Inflar el globo despacio", "Correr sin parar", "Aguantar el aire hasta marearse"),
                    indiceCorrecto = 0,
                    pista = "Inhala contando 4 y exhala contando 4, como si inflaras un globo."
                ),
                NodoSaga(
                    id = "arbol_4",
                    nombre = "La Nube del Estrés",
                    enunciado = "La Nube del Estrés se acerca. ¿Qué le decimos?",
                    opciones = listOf("«Hola, nube. Respiro y sigo»", "«¡Ni me mires!»", "«Me escondo para siempre»"),
                    indiceCorrecto = 0,
                    pista = "Nombrar lo que sientes y respirar le quita fuerza a la nube."
                )
            )
        ),
        MundoSaga(
            modulo = ModuloZazili.LABORATORIO_CENOTE,
            jefe = "El Monstruo de Basura",
            dichoDelJefe = "¡Ja! Al cenote le caben más bolsas. ¿O no, campeón?",
            nodos = listOf(
                NodoSaga(
                    id = "cenote_1",
                    nombre = "La receta de la planta",
                    enunciado = "¿Qué necesita una planta para crecer sana?",
                    opciones = listOf("Agua, luz y aire", "Solo dulces", "Una televisión"),
                    indiceCorrecto = 0,
                    pista = "Las plantas fabrican su comida con luz, agua y aire."
                ),
                NodoSaga(
                    id = "cenote_2",
                    nombre = "Aceite en el agua",
                    enunciado = "¿Qué pasa si el aceite del motor cae al agua del cenote?",
                    opciones = listOf("El agua se contamina", "El agua se endulza", "Se convierte en hielo"),
                    indiceCorrecto = 0,
                    pista = "El aceite flota y no deja respirar a los seres vivos del agua."
                ),
                NodoSaga(
                    id = "cenote_3",
                    nombre = "¿Quién vive aquí?",
                    enunciado = "¿Cuál de estos animales puede vivir en un cenote?",
                    opciones = listOf("Un pez", "Una vaca", "Un cóndor"),
                    indiceCorrecto = 0,
                    pista = "Piensa en el animal que nada bajo el agua."
                ),
                NodoSaga(
                    id = "cenote_4",
                    nombre = "El Monstruo de Basura",
                    enunciado = "El Monstruo tira bolsas al cenote. ¿Qué hacemos?",
                    opciones = listOf("Recogerlas y separar la basura", "Echarle más bolsas", "Hacer como que no pasa nada"),
                    indiceCorrecto = 0,
                    pista = "Recoger, separar y avisar a un adulto deja el cenote feliz."
                )
            )
        ),
        MundoSaga(
            modulo = ModuloZazili.PIRAMIDE_CODIGO,
            jefe = "El Gran Glitch",
            dichoDelJefe = "Tus pasos están enredados. ¡A ver si puedes ordenarlos!",
            nodos = listOf(
                NodoSaga(
                    id = "piramide_1",
                    nombre = "El poder del loop",
                    enunciado = "En código, ¿qué hace un «loop»?",
                    opciones = listOf("Repetir pasos", "Dormir la pantalla", "Borrar todo"),
                    indiceCorrecto = 0,
                    pista = "Un loop repite una instrucción varias veces, como un disco rayado."
                ),
                NodoSaga(
                    id = "piramide_2",
                    nombre = "La llave secreta",
                    enunciado = "¿Cuál contraseña es la más segura?",
                    opciones = listOf("1234", "Taco1234", "MiPerro#2026"),
                    indiceCorrecto = 2,
                    pista = "Mezcla mayúsculas, números y un signo. ¡Y no la compartas!"
                ),
                NodoSaga(
                    id = "piramide_3",
                    nombre = "Cuac, cuac, cuac",
                    enunciado = "El pato dice «cuac» 3 veces. ¿Qué usamos para repetirlo sin escribir tres veces?",
                    opciones = listOf("Un loop", "Una suma", "Un mapa"),
                    indiceCorrecto = 0,
                    pista = "El loop evita repetir el mismo bloque a mano."
                ),
                NodoSaga(
                    id = "piramide_4",
                    nombre = "El Gran Glitch",
                    enunciado = "Ordenaste: 1) despertar, 2)… ¿qué paso sigue en la rutina?",
                    opciones = listOf("Lavarme los dientes", "Dormir otra vez", "Hacer la tarea de mañana"),
                    indiceCorrecto = 0,
                    pista = "Primero despiertas y luego te lavas los dientes. El orden importa."
                )
            )
        )
    )

    fun mundoDe(modulo: ModuloZazili): MundoSaga? =
        mundos.firstOrNull { it.modulo == modulo }

    fun totalNodos(): Int = mundos.sumOf { it.nodos.size }

    fun totalEstrellasPosibles(): Int = totalNodos() * 3

    /** Índice dentro del mundo; -1 si el nodo no pertenece. */
    fun indiceDeNodo(mundo: MundoSaga, nodoId: String): Int =
        mundo.nodos.indexOfFirst { it.id == nodoId }

    /** Un nodo está abierto si es el primero o si el anterior ya se completó. */
    fun desbloqueado(mundo: MundoSaga, nodoId: String, estado: EstadoSaga): Boolean {
        val indice = indiceDeNodo(mundo, nodoId)
        if (indice < 0) return false
        return indice == 0 || estado.completado(mundo.nodos[indice - 1].id)
    }

    fun siguienteNodo(mundo: MundoSaga, estado: EstadoSaga): NodoSaga? =
        mundo.nodos.firstOrNull { !estado.completado(it.id) }

    fun mundoActual(estado: EstadoSaga): MundoSaga =
        mundos.firstOrNull { !estado.mundoCompletado(it) } ?: mundos.last()
}
