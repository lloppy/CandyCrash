package com.lloppy.candycrash.screens.levels.screens.game

import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.Gem
import com.lloppy.candycrash.screens.levels.game.GemColor
import com.lloppy.candycrash.screens.levels.game.GameProgressRepository
import com.lloppy.candycrash.screens.levels.game.Level
import com.lloppy.candycrash.screens.levels.game.Levels
import com.lloppy.candycrash.screens.levels.game.Match3Engine
import com.lloppy.candycrash.screens.levels.game.MoveResult
import com.lloppy.candycrash.screens.levels.game.Objective
import com.lloppy.candycrash.screens.levels.game.Pos
import com.lloppy.candycrash.screens.levels.game.isComplete
import com.lloppy.candycrash.screens.levels.mvi.MviViewModel
import com.lloppy.candycrash.screens.levels.mvi.NoEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(
    val levelId: Int,
    private val progress: GameProgressRepository,
) : MviViewModel<GameState, GameIntent, NoEffect>(GameState()) {

    private val level: Level = Levels.byId(levelId)
    private val random = Random.Default
    private var solidBoard: List<List<Gem?>> = emptyList()

    init {
        startLevel()
    }

    override fun onIntent(intent: GameIntent) = when (intent) {
        is GameIntent.CellClicked -> onCellClicked(intent.pos)
        is GameIntent.Swiped -> applyMove(intent.from, intent.to)
        GameIntent.Restart -> startLevel()
    }

    private fun startLevel() {
        var board = Match3Engine.createBoard(level, random)
        while (!Match3Engine.hasAvailableMove(board)) {
            board = Match3Engine.createBoard(level, random)
        }
        solidBoard = board
        updateState {
            GameState(
                board = board,
                shape = level.mask,
                movesLeft = level.moves,
                target = level.passScore,
                star1 = level.star1,
                star2 = level.star2,
                star3 = level.star3,
                objective = level.objective,
            )
        }
    }

    private fun onCellClicked(pos: Pos) {
        val s = currentState
        if (s.busy || s.status != GameStatus.Playing) return
        val selected = s.selected
        when {
            selected == null -> updateState { copy(selected = pos) }
            selected == pos -> updateState { copy(selected = null) }
            !Match3Engine.areAdjacent(selected, pos) -> updateState { copy(selected = pos) }
            else -> applyMove(selected, pos)
        }
    }

    private fun applyMove(a: Pos, b: Pos) {
        val s = currentState
        if (s.busy || s.status != GameStatus.Playing) return
        if (!Match3Engine.areAdjacent(a, b)) return

        val result = Match3Engine.tryMove(solidBoard, a, b, level, random)
        val swappedBoard = Match3Engine.swapped(solidBoard, a, b)
        updateState { copy(board = swappedBoard, selected = null, busy = true) }

        viewModelScope.launch {
            delay(SWAP_MS)
            if (result == null) {
                updateState { copy(board = solidBoard) }
                delay(SWAP_MS)
                updateState { copy(busy = false) }
                return@launch
            }
            runCascade(result, swappedBoard)
        }
    }

    private suspend fun runCascade(result: MoveResult, swappedBoard: List<List<Gem?>>) {
        var prev = swappedBoard
        val steps = result.frames.size / 2
        for (k in 0 until steps) {
            val cleared = result.frames[2 * k]
            val collapsed = result.frames[2 * k + 1]

            val fx = buildList {
                for (r in prev.indices) for (c in prev[r].indices) {
                    val g = prev[r][c]
                    if (g != null && cleared[r][c] == null) add(ClearFx(r, c, g))
                }
            }
            val acts = result.activations.getOrNull(k).orEmpty()

            updateState { copy(board = cleared, clearing = fx, effects = acts) }
            delay(if (acts.isNotEmpty()) maxOf(POP_MS, EFFECT_MS) else POP_MS)

            val fallCells = maxFallCells(cleared, collapsed)
            updateState { copy(board = collapsed, clearing = emptyList(), effects = emptyList()) }
            delay(fallDelay(fallCells))

            prev = collapsed
        }

        var board = result.finalBoard
        if (!Match3Engine.hasAvailableMove(board)) {
            board = reshuffle(board)
            updateState { copy(board = board) }
        }
        solidBoard = board

        val newScore = currentState.score + result.gainedScore
        val newMoves = currentState.movesLeft - 1
        val newCollected = mergeCounts(currentState.collected, result.clearedByColor)
        val objectiveMet = level.objective.isComplete(newScore, level.star1, newCollected)

        val newStatus = when (level.objective) {
            Objective.Score ->
                if (newMoves > 0) GameStatus.Playing
                else if (objectiveMet) GameStatus.Won else GameStatus.Lost
            is Objective.Collect ->
                if (objectiveMet) GameStatus.Won
                else if (newMoves > 0) GameStatus.Playing
                else GameStatus.Lost
        }
        val stars = if (newStatus == GameStatus.Won) starsFor(newScore) else 0

        updateState {
            copy(
                board = board,
                score = newScore,
                movesLeft = newMoves,
                status = newStatus,
                earnedStars = stars,
                busy = false,
                clearing = emptyList(),
                collected = newCollected,
            )
        }
        if (newStatus == GameStatus.Won) progress.onLevelCompleted(level.id, stars)
    }

    private fun mergeCounts(a: Map<GemColor, Int>, b: Map<GemColor, Int>): Map<GemColor, Int> {
        if (b.isEmpty()) return a
        val out = HashMap(a)
        for ((k, v) in b) out[k] = (out[k] ?: 0) + v
        return out
    }

    private fun maxFallCells(prev: List<List<Gem?>>, cur: List<List<Gem?>>): Int {
        val prevRow = HashMap<Long, Int>()
        for (r in prev.indices) for (c in prev[r].indices) prev[r][c]?.let { prevRow[it.id] = r }
        var maxd = 0
        for (c in 0 until level.cols) {
            var newInCol = 0
            for (r in 0 until level.rows) {
                val g = cur[r][c] ?: continue
                val pr = prevRow[g.id]
                if (pr != null) { if (r - pr > maxd) maxd = r - pr } else newInCol++
            }
            if (newInCol > maxd) maxd = newInCol
        }
        return maxd
    }

    private fun fallDelay(cells: Int): Long =
        (cells * MS_PER_CELL).coerceIn(MIN_MOVE_MS, MAX_MOVE_MS) + SETTLE_MS

    private fun reshuffle(board: List<List<Gem?>>): List<List<Gem?>> {
        val positions = ArrayList<Pos>()
        val gems = ArrayList<Gem>()
        for (r in board.indices) for (c in board[r].indices) {
            board[r][c]?.let { positions.add(Pos(r, c)); gems.add(it) }
        }
        repeat(50) {
            gems.shuffle(random)
            val grid = board.map { it.toMutableList() }
            positions.forEachIndexed { i, p -> grid[p.row][p.col] = gems[i] }
            val candidate = grid.map { it.toList() }
            if (Match3Engine.findMatches(candidate).isEmpty() && Match3Engine.hasAvailableMove(candidate)) {
                return candidate
            }
        }
        return board
    }

    private fun starsFor(score: Int): Int = when {
        score >= level.star3 -> 3
        score >= level.star2 -> 2
        else -> 1
    }

    private companion object {
        const val SWAP_MS = 110L
        const val POP_MS = 140L
        const val EFFECT_MS = 320L
        const val MS_PER_CELL = 55L
        const val MIN_MOVE_MS = 170L
        const val MAX_MOVE_MS = 480L
        const val SETTLE_MS = 40L
    }
}
