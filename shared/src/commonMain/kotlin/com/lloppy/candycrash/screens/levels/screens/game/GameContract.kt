package com.lloppy.candycrash.screens.levels.screens.game

import com.lloppy.candycrash.screens.levels.game.ActivationFx
import com.lloppy.candycrash.screens.levels.game.Gem
import com.lloppy.candycrash.screens.levels.game.GemColor
import com.lloppy.candycrash.screens.levels.game.Objective
import com.lloppy.candycrash.screens.levels.game.Pos
import com.lloppy.candycrash.screens.levels.mvi.UiAction
import com.lloppy.candycrash.screens.levels.mvi.UiState

enum class GameStatus { Playing, Won, Lost }

data class ClearFx(val row: Int, val col: Int, val gem: Gem)

data class GameState(
    val board: List<List<Gem?>> = emptyList(),
    val shape: List<List<Boolean>> = emptyList(),
    val score: Int = 0,
    val movesLeft: Int = 0,
    val target: Int = 0,
    val star1: Int = 0,
    val star2: Int = 0,
    val star3: Int = 0,
    val selected: Pos? = null,
    val status: GameStatus = GameStatus.Playing,
    val earnedStars: Int = 0,
    val busy: Boolean = false,
    val clearing: List<ClearFx> = emptyList(),
    val effects: List<ActivationFx> = emptyList(),
    val objective: Objective = Objective.Score,
    val collected: Map<GemColor, Int> = emptyMap(),
    val soundEnabled: Boolean = true,
) : UiState

sealed interface GameAction : UiAction {
    data class CellClicked(val pos: Pos) : GameAction
    data class Swiped(val from: Pos, val to: Pos) : GameAction
    data object Restart : GameAction
    data class ToggleSound(val enabled: Boolean) : GameAction
}
