package com.lloppy.candycrash.screens.levels.screens.levels

import com.lloppy.candycrash.screens.levels.game.Level

data class LevelsState(
    val highestUnlocked: Int = 1,
    val stars: Map<Int, Int> = emptyMap(),
    val infoLevel: Level? = null,
)

sealed interface LevelsAction {
    data class NodeClicked(val level: Level) : LevelsAction
    data object DismissInfo : LevelsAction
    data class PlayClicked(val levelId: Int) : LevelsAction
}

sealed interface LevelsEvent {
    data class NavigateToGame(val levelId: Int) : LevelsEvent
}
