package com.lloppy.candycrash.screens.levels.game

import kotlin.math.abs

data class Level(
    val id: Int,
    val rows: Int,
    val cols: Int,
    val colors: Int,
    val moves: Int,
    val star1: Int,
    val star2: Int,
    val star3: Int,
    val mask: List<List<Boolean>>,
    val objective: Objective = Objective.Score,
) {
    val passScore: Int get() = star1

    fun playable(r: Int, c: Int): Boolean =
        r in mask.indices && c in mask[r].indices && mask[r][c]

    val playableCount: Int = mask.sumOf { row -> row.count { it } }
}

object Levels {
    const val COUNT = 10
    private const val N = 8

    val all: List<Level> = (1..COUNT).map { i ->
        val mask = shapeFor(i)
        val playable = mask.sumOf { row -> row.count { it } }
        val moves = (25 - (i - 1)).coerceAtLeast(12)
        val colors = (4 + (i - 1) / 3).coerceAtMost(5)
        val pf = playable.toDouble() / (N * N)
        val objective = objectiveFor(i)
        if (objective is Objective.Collect) {
            check(objective.targets.keys.all { it.ordinal < colors }) {
                "Level $i: цель использует цвет вне диапазона colors=$colors"
            }
        }
        Level(
            id = i,
            rows = N,
            cols = N,
            colors = colors,
            moves = moves,
            star1 = roundTo((moves * 26 * pf).toInt(), 50).coerceAtLeast(150),
            star2 = roundTo((moves * 42 * pf).toInt(), 50).coerceAtLeast(250),
            star3 = roundTo((moves * 58 * pf).toInt(), 50).coerceAtLeast(350),
            mask = mask,
            objective = objective,
        )
    }

    fun byId(id: Int): Level = all.first { it.id == id }

    private fun roundTo(value: Int, step: Int) = ((value + step / 2) / step) * step

    private fun objectiveFor(level: Int): Objective = when (level) {
        1 -> Objective.Collect(linkedMapOf(GemColor.Red to 14))
        2 -> Objective.Collect(linkedMapOf(GemColor.Blue to 16))
        3 -> Objective.Collect(linkedMapOf(GemColor.Red to 20))
        4 -> Objective.Collect(linkedMapOf(GemColor.Green to 16, GemColor.Yellow to 16))
        5 -> Objective.Collect(linkedMapOf(GemColor.Blue to 16, GemColor.Green to 16))
        6 -> Objective.Collect(linkedMapOf(GemColor.Purple to 18))
        7 -> Objective.Collect(linkedMapOf(GemColor.Yellow to 22))
        8 -> Objective.Collect(linkedMapOf(GemColor.Red to 16, GemColor.Purple to 16))
        9 -> Objective.Collect(linkedMapOf(GemColor.Yellow to 16, GemColor.Purple to 16))
        else -> Objective.Collect(linkedMapOf(GemColor.Green to 12, GemColor.Blue to 12, GemColor.Yellow to 12))
    }

    private fun shapeFor(level: Int): List<List<Boolean>> {
        val center = (N - 1) / 2.0

        fun diamond(r: Int, c: Int, k: Int) =
            abs(r - center) + abs(c - center) <= k + 0.001

        fun circle(r: Int, c: Int, rad: Double): Boolean {
            val dr = r - center
            val dc = c - center
            return dr * dr + dc * dc <= rad * rad
        }

        fun corner(r: Int, c: Int, k: Int) =
            (r + c < k) || ((N - 1 - r) + c < k) ||
                (r + (N - 1 - c) < k) || ((N - 1 - r) + (N - 1 - c) < k)

        fun plus(r: Int, c: Int, lo: Int, hi: Int) = (c in lo..hi) || (r in lo..hi)

        val pred: (Int, Int) -> Boolean = when (level) {
            1 -> { _, _ -> true }
            2 -> { r, c -> !corner(r, c, 2) }
            3 -> { r, c -> diamond(r, c, 4) }
            4 -> { r, c -> plus(r, c, 2, 5) }
            5 -> { r, c -> circle(r, c, 4.2) }
            6 -> { r, c -> diamond(r, c, 5) }
            7 -> { r, c -> circle(r, c, 3.6) }
            8 -> { r, c -> plus(r, c, 3, 4) }
            9 -> { r, c -> diamond(r, c, 3) }
            else -> { r, c -> diamond(r, c, 4) || plus(r, c, 2, 5) }
        }
        return List(N) { r -> List(N) { c -> pred(r, c) } }
    }
}
