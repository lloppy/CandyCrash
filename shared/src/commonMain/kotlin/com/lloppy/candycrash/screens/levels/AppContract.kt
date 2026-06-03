package com.lloppy.candycrash.screens.levels

data class AppState(
    val darkTheme: Boolean = false,
)

sealed interface AppAction {
    data object ToggleTheme : AppAction
}
