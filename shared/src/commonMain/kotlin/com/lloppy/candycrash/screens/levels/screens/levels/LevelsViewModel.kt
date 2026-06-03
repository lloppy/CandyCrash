package com.lloppy.candycrash.screens.levels.screens.levels

import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.GameProgressRepository
import com.lloppy.candycrash.screens.levels.mvi.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LevelsViewModel(
    progress: GameProgressRepository,
) : MviViewModel<LevelsState, LevelsIntent, LevelsEffect>(LevelsState()) {

    init {
        combine(progress.highestUnlocked, progress.stars) { highest, stars ->
            highest to stars
        }.onEach { (highest, stars) ->
            updateState { copy(highestUnlocked = highest, stars = stars) }
        }.launchIn(viewModelScope)
    }

    override fun onIntent(intent: LevelsIntent) = when (intent) {
        is LevelsIntent.NodeClicked -> updateState { copy(infoLevel = intent.level) }
        LevelsIntent.DismissInfo -> updateState { copy(infoLevel = null) }
        is LevelsIntent.PlayClicked -> {
            updateState { copy(infoLevel = null) }
            emitEffect(LevelsEffect.NavigateToGame(intent.levelId))
        }
    }
}
