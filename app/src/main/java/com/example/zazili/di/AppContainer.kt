package com.example.zazili.di

import android.content.Context
import androidx.room.Room
import com.example.zazili.core.accessibility.ZenModeManager
import com.example.zazili.core.mascot.RepositorioMascota
import com.example.zazili.core.progress.GameProgressManager
import com.example.zazili.core.progress.ProgresoSagaStore
import com.example.zazili.core.security.AccesoNido
import com.example.zazili.core.usuario.UserManager
import com.example.zazili.data.local.ZaziliDatabase
import com.example.zazili.data.repository.ProgressRepositoryImpl
import com.example.zazili.domain.arena.DynamicDifficultyEngine
import com.example.zazili.domain.economia.EconomiaZazoles
import com.example.zazili.domain.repository.ProgressRepository

/**
 * Contenedor manual de dependencias de Zazili 2.0.
 *
 * Solo utilidades de núcleo: base de datos segura (Room), audio global,
 * accesibilidad, perfil del niño, mascota, economía, progreso y el PIN del
 * Nido. Los módulos educativos nuevos consumen estas piezas; ninguna pantalla
 * vieja sobrevive.
 */
class AppContainer(context: Context) {

    private val database: ZaziliDatabase = Room.databaseBuilder(
        context.applicationContext,
        ZaziliDatabase::class.java,
        "zazili.db"
    ).build()

    val progressRepository: ProgressRepository =
        ProgressRepositoryImpl(database.sessionLogDao())

    val gameProgressManager: GameProgressManager = GameProgressManager(context)

    // Onboarding, perfil y línea base calibrada (DataStore, solo local).
    val userManager: UserManager = UserManager(context)

    // Arena Cortex: nivel 1..5 por minijuego y rachas de victorias/derrotas.
    val dynamicDifficultyEngine: DynamicDifficultyEngine =
        DynamicDifficultyEngine(context, userManager)

    val zenMode: ZenModeManager = ZenModeManager(context)

    // Tamagotchi orgánico + economía de Zazoles.
    val repositorioMascota: RepositorioMascota = RepositorioMascota(context)

    val economiaZazoles: EconomiaZazoles = EconomiaZazoles(context)

    // Saga 2.0 y acceso de adultos.
    val progresoSaga: ProgresoSagaStore = ProgresoSagaStore(context)

    val accesoNido: AccesoNido = AccesoNido(context)
}
