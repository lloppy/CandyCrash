package com.lloppy.candycrash.screens.levels.screens.settings

import androidx.lifecycle.ViewModel

class SettingsViewModel(
    private val settings: com.lloppy.candycrash.screens.levels.game.SettingsRepository,
) : ViewModel() {
    val soundEnabled = settings.soundEnabled
    val vibrationEnabled = settings.vibrationEnabled

    fun setSound(enabled: Boolean) = settings.setSoundEnabled(enabled)
    fun setVibration(enabled: Boolean) = settings.setVibrationEnabled(enabled)
}
