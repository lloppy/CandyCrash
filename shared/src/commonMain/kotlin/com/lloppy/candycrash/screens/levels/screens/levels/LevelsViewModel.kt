package com.lloppy.candycrash.screens.levels.screens.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.GameProgressRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LevelsViewModel(
    progress: GameProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LevelsState())
    val state: StateFlow<LevelsState> = _state.asStateFlow()

    private val _events = Channel<LevelsEvent>(Channel.BUFFERED)
    val events: Flow<LevelsEvent> = _events.receiveAsFlow()

    init {
        combine(progress.highestUnlocked, progress.stars) { highest, stars ->
            highest to stars
        }.onEach { (highest, stars) ->
            _state.update { it.copy(highestUnlocked = highest, stars = stars) }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: LevelsAction) = when (action) {
        is LevelsAction.NodeClicked -> _state.update { it.copy(infoLevel = action.level) }
        LevelsAction.DismissInfo -> _state.update { it.copy(infoLevel = null) }
        is LevelsAction.PlayClicked -> {
            _state.update { it.copy(infoLevel = null) }
            viewModelScope.launch { _events.send(LevelsEvent.NavigateToGame(action.levelId)) }
        }
    }
}
