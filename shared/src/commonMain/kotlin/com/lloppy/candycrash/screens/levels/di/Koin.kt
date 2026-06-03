package com.lloppy.candycrash.screens.levels.di

import com.lloppy.candycrash.screens.levels.AppViewModel
import com.lloppy.candycrash.screens.levels.game.GameProgressRepository
import com.lloppy.candycrash.screens.levels.game.SettingsRepository
import com.lloppy.candycrash.screens.levels.screens.game.GameViewModel
import com.lloppy.candycrash.screens.levels.screens.levels.LevelsViewModel
import com.lloppy.candycrash.screens.levels.screens.settings.SettingsViewModel
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    single<Settings> { Settings() }
    single { GameProgressRepository(get()) }
    single { SettingsRepository(get()) }
}

val viewModelModule = module {
    factoryOf(::AppViewModel)
    factoryOf(::LevelsViewModel)
    factoryOf(::SettingsViewModel)
    factory { params ->
        GameViewModel(
            levelId = params.get(),
            progress = get(),
            settings = get(),
        )
    }
}

fun initKoin() {
    startKoin {
        modules(
            appModule,
            viewModelModule,
        )
    }
}
