package com.jetbrains.kmpapp.screens.levels

import androidx.lifecycle.ViewModel
import com.jetbrains.kmpapp.game.GameProgressRepository

class LevelsViewModel(
    progress: GameProgressRepository,
) : ViewModel() {
    val highestUnlocked = progress.highestUnlocked
    val stars = progress.stars
}
