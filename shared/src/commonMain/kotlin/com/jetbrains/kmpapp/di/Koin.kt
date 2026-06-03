package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.game.GameProgressRepository
import com.jetbrains.kmpapp.game.SettingsRepository
import com.jetbrains.kmpapp.screens.game.GameViewModel
import com.jetbrains.kmpapp.screens.levels.LevelsViewModel
import com.jetbrains.kmpapp.screens.settings.SettingsViewModel
import com.russhwolf.settings.Settings
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    // Постоянное хранилище (Android SharedPreferences / iOS NSUserDefaults)
    single<Settings> { Settings() }
    // Репозитории живут на всё время приложения
    single { GameProgressRepository(get()) }
    single { SettingsRepository(get()) }
}

val viewModelModule = module {
    factoryOf(::LevelsViewModel)
    factoryOf(::SettingsViewModel)
    // GameViewModel принимает levelId как параметр навигации
    factory { params -> GameViewModel(levelId = params.get(), progress = get()) }
}

fun initKoin() {
    startKoin {
        modules(
            appModule,
            viewModelModule,
        )
    }
}
