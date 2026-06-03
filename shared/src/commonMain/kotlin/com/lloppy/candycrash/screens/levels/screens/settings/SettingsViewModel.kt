package com.lloppy.candycrash.screens.levels.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        combine(settings.soundEnabled, settings.vibrationEnabled) { sound, vibration ->
            SettingsState(soundEnabled = sound, vibrationEnabled = vibration)
        }.onEach { next ->
            _state.value = next
        }.launchIn(viewModelScope)
    }

    fun onAction(action: SettingsAction) = when (action) {
        is SettingsAction.SetSound -> settings.setSoundEnabled(action.enabled)
        is SettingsAction.SetVibration -> settings.setVibrationEnabled(action.enabled)
    }
}
