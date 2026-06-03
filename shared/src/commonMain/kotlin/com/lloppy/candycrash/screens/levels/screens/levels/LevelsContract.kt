package com.lloppy.candycrash.screens.levels.screens.levels

import com.lloppy.candycrash.screens.levels.game.Level
import com.lloppy.candycrash.screens.levels.mvi.UiAction
import com.lloppy.candycrash.screens.levels.mvi.UiEvent
import com.lloppy.candycrash.screens.levels.mvi.UiState

data class LevelsState(
    val highestUnlocked: Int = 1,
    val stars: Map<Int, Int> = emptyMap(),
    val infoLevel: Level? = null,
) : UiState

sealed interface LevelsAction : UiAction {
    data class NodeClicked(val level: Level) : LevelsAction
    data object DismissInfo : LevelsAction
    data class PlayClicked(val levelId: Int) : LevelsAction
}

sealed interface LevelsEvent : UiEvent {
    data class NavigateToGame(val levelId: Int) : LevelsEvent
}
