package com.jetbrains.kmpapp.game

/**
 * Цель уровня.
 *  - [Score] — пройти, набрав счёт >= star1 (за отведённые ходы).
 *  - [Collect] — собрать заданное число шариков указанных цветов.
 * Звёзды всегда считаются по очкам (star1/star2/star3) независимо от типа цели.
 */
sealed interface Objective {

    data object Score : Objective

    /** [targets] — сколько шариков каждого цвета нужно собрать (порядок сохраняется для HUD). */
    data class Collect(val targets: Map<GemColor, Int>) : Objective
}

/** Выполнена ли цель при текущем счёте и собранных шариках. */
fun Objective.isComplete(
    score: Int,
    star1: Int,
    collected: Map<GemColor, Int>,
): Boolean = when (this) {
    Objective.Score -> score >= star1
    is Objective.Collect -> targets.all { (color, need) -> (collected[color] ?: 0) >= need }
}
