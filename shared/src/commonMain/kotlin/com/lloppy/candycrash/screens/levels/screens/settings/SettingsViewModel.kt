package com.lloppy.candycrash.screens.levels.screens.settings

import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.SettingsRepository
import com.lloppy.candycrash.screens.levels.mvi.MviViewModel
import com.lloppy.candycrash.screens.levels.mvi.NoEffect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsViewModel(
    private val settings: SettingsRepository,
) : MviViewModel<SettingsState, SettingsIntent, NoEffect>(SettingsState()) {

    init {
        combine(settings.soundEnabled, settings.vibrationEnabled) { sound, vibration ->
            SettingsState(soundEnabled = sound, vibrationEnabled = vibration)
        }.onEach { next ->
            updateState { next }
        }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) = when (intent) {
        is SettingsIntent.SetSound -> settings.setSoundEnabled(intent.enabled)
        is SettingsIntent.SetVibration -> settings.setVibrationEnabled(intent.enabled)
    }
}
