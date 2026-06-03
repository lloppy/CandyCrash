package com.jetbrains.kmpapp.game

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Настройки приложения, сохраняются между запусками через [Settings].
 */
class SettingsRepository(
    private val settings: Settings,
) {

    private val _soundEnabled = MutableStateFlow(settings.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(settings.getBoolean(KEY_VIBRATION, true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _darkTheme = MutableStateFlow(settings.getBoolean(KEY_DARK, false))
    /** true — тёмная (космическая) тема. По умолчанию светлая. */
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        settings.putBoolean(KEY_SOUND, enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        settings.putBoolean(KEY_VIBRATION, enabled)
    }

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        settings.putBoolean(KEY_DARK, enabled)
    }

    fun toggleDarkTheme() = setDarkTheme(!_darkTheme.value)

    private companion object {
        const val KEY_SOUND = "settings.sound"
        const val KEY_VIBRATION = "settings.vibration"
        const val KEY_DARK = "settings.darkTheme"
    }
}
