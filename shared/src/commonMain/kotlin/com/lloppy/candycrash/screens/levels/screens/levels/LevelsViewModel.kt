package com.lloppy.candycrash.screens.levels.screens.levels

import androidx.lifecycle.ViewModel

class LevelsViewModel(
    progress: com.lloppy.candycrash.screens.levels.game.GameProgressRepository,
) : ViewModel() {
    val highestUnlocked = progress.highestUnlocked
    val stars = progress.stars
}
