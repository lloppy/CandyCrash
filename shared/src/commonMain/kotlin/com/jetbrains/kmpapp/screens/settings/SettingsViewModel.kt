package com.jetbrains.kmpapp.screens.settings

import androidx.lifecycle.ViewModel
import com.jetbrains.kmpapp.game.SettingsRepository

class SettingsViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    val soundEnabled = settings.soundEnabled
    val vibrationEnabled = settings.vibrationEnabled

    fun setSound(enabled: Boolean) = settings.setSoundEnabled(enabled)
    fun setVibration(enabled: Boolean) = settings.setVibrationEnabled(enabled)
}
