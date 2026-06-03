package com.lloppy.candycrash.screens.levels.di

import com.lloppy.candycrash.screens.levels.screens.levels.LevelsViewModel
import com.lloppy.candycrash.screens.levels.screens.settings.SettingsViewModel
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    // Постоянное хранилище (Android SharedPreferences / iOS NSUserDefaults)
    single<Settings> { Settings() }
    // Репозитории живут на всё время приложения
    single { _root_ide_package_.com.lloppy.candycrash.screens.levels.game.GameProgressRepository(get()) }
    single { _root_ide_package_.com.lloppy.candycrash.screens.levels.game.SettingsRepository(get()) }
}

val viewModelModule = module {
    factoryOf(::LevelsViewModel)
    factoryOf(::SettingsViewModel)
    // GameViewModel принимает levelId как параметр навигации
    factory { params ->
        _root_ide_package_.com.lloppy.candycrash.screens.levels.screens.game.GameViewModel(
            levelId = params.get(),
            progress = get()
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
