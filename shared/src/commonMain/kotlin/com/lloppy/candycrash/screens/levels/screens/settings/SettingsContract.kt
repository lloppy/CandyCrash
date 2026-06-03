package com.lloppy.candycrash.screens.levels.screens.settings

import com.lloppy.candycrash.screens.levels.mvi.UiAction
import com.lloppy.candycrash.screens.levels.mvi.UiState

data class SettingsState(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
) : UiState

sealed interface SettingsAction : UiAction {
    data class SetSound(val enabled: Boolean) : SettingsAction
    data class SetVibration(val enabled: Boolean) : SettingsAction
}
