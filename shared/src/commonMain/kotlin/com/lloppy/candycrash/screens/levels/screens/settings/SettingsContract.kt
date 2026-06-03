package com.lloppy.candycrash.screens.levels.screens.settings

import com.lloppy.candycrash.screens.levels.mvi.UiIntent
import com.lloppy.candycrash.screens.levels.mvi.UiState

data class SettingsState(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data class SetSound(val enabled: Boolean) : SettingsIntent
    data class SetVibration(val enabled: Boolean) : SettingsIntent
}
