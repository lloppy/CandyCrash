package com.lloppy.candycrash.screens.levels

import com.lloppy.candycrash.screens.levels.mvi.UiAction
import com.lloppy.candycrash.screens.levels.mvi.UiState

data class AppState(
    val darkTheme: Boolean = false,
) : UiState

sealed interface AppAction : UiAction {
    data object ToggleTheme : AppAction
}
