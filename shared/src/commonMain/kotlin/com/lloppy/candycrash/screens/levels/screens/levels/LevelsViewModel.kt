package com.lloppy.candycrash.screens.levels.screens.levels

import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.GameProgressRepository
import com.lloppy.candycrash.screens.levels.mvi.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LevelsViewModel(
    progress: GameProgressRepository,
) : MviViewModel<LevelsState, LevelsAction, LevelsEvent>(LevelsState()) {

    init {
        combine(progress.highestUnlocked, progress.stars) { highest, stars ->
            highest to stars
        }.onEach { (highest, stars) ->
            updateState { copy(highestUnlocked = highest, stars = stars) }
        }.launchIn(viewModelScope)
    }

    override fun onAction(action: LevelsAction) = when (action) {
        is LevelsAction.NodeClicked -> updateState { copy(infoLevel = action.level) }
        LevelsAction.DismissInfo -> updateState { copy(infoLevel = null) }
        is LevelsAction.PlayClicked -> {
            updateState { copy(infoLevel = null) }
            emitEvent(LevelsEvent.NavigateToGame(action.levelId))
        }
    }
}
