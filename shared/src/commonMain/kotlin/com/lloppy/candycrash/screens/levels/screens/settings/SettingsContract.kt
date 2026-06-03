package com.lloppy.candycrash.screens.levels.screens.settings

data class SettingsState(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

sealed interface SettingsAction {
    data class SetSound(val enabled: Boolean) : SettingsAction
    data class SetVibration(val enabled: Boolean) : SettingsAction
}
