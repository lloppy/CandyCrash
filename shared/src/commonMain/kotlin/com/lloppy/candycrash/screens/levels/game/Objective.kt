package com.lloppy.candycrash.screens.levels.game

sealed interface Objective {

    data object Score : Objective

    data class Collect(val targets: Map<GemColor, Int>) : Objective
}

fun Objective.isComplete(
    score: Int,
    star1: Int,
    collected: Map<GemColor, Int>,
): Boolean = when (this) {
    Objective.Score -> score >= star1
    is Objective.Collect -> targets.all { (color, need) -> (collected[color] ?: 0) >= need }
}
