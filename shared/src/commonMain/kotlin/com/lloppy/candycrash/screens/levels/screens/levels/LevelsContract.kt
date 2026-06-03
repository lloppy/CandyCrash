package com.lloppy.candycrash.screens.levels.screens.levels

import com.lloppy.candycrash.screens.levels.game.Level
import com.lloppy.candycrash.screens.levels.mvi.UiEffect
import com.lloppy.candycrash.screens.levels.mvi.UiIntent
import com.lloppy.candycrash.screens.levels.mvi.UiState

data class LevelsState(
    val highestUnlocked: Int = 1,
    val stars: Map<Int, Int> = emptyMap(),
    val infoLevel: Level? = null,
) : UiState

sealed interface LevelsIntent : UiIntent {
    data class NodeClicked(val level: Level) : LevelsIntent
    data object DismissInfo : LevelsIntent
    data class PlayClicked(val levelId: Int) : LevelsIntent
}

sealed interface LevelsEffect : UiEffect {
    data class NavigateToGame(val levelId: Int) : LevelsEffect
}
