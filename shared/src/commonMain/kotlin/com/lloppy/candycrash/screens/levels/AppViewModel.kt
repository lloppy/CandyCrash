package com.lloppy.candycrash.screens.levels

import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.SettingsRepository
import com.lloppy.candycrash.screens.levels.mvi.MviViewModel
import com.lloppy.candycrash.screens.levels.mvi.NoEvent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AppViewModel(
    private val settings: SettingsRepository,
) : MviViewModel<AppState, AppAction, NoEvent>(AppState()) {

    init {
        settings.darkTheme
            .onEach { dark -> updateState { copy(darkTheme = dark) } }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: AppAction) = when (action) {
        AppAction.ToggleTheme -> settings.toggleDarkTheme()
    }
}
