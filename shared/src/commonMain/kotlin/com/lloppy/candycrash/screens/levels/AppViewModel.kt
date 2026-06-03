package com.lloppy.candycrash.screens.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class AppViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        settings.darkTheme
            .onEach { dark -> _state.update { it.copy(darkTheme = dark) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: AppAction) = when (action) {
        AppAction.ToggleTheme -> settings.toggleDarkTheme()
    }
}
