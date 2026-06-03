package com.lloppy.candycrash.screens.levels.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lloppy.candycrash.screens.levels.game.Gem
import com.lloppy.candycrash.screens.levels.game.GemColor
import com.lloppy.candycrash.screens.levels.game.isComplete
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.iterator
import kotlin.random.Random

enum class GameStatus { Playing, Won, Lost }

/** Совпавший шарик, который сейчас «лопается» (анимация очистки). */
data class ClearFx(val row: Int, val col: Int, val gem: Gem)

data class GameUiState(
    /** Поле для отображения: null означает пустую клетку (во время анимации). */
    val board: List<List<Gem?>> = emptyList(),
    val score: Int = 0,
    val movesLeft: Int = 0,
    val target: Int = 0,
    val selected: com.lloppy.candycrash.screens.levels.game.Pos? = null,
    val status: GameStatus = GameStatus.Playing,
    val earnedStars: Int = 0,
    /** Идёт анимация хода — ввод заблокирован. */
    val busy: Boolean = false,
    /** Шарики, которые сейчас лопаются (для анимации «поп»). */
    val clearing: List<ClearFx> = emptyList(),
    /** Цель уровня. */
    val objective: com.lloppy.candycrash.screens.levels.game.Objective = _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Objective.Score,
    /** Сколько шариков каждого цвета уже собрано (для цели "собрать"). */
    val collected: Map<GemColor, Int> = emptyMap(),
)

class GameViewModel(
    val levelId: Int,
    private val progress: com.lloppy.candycrash.screens.levels.game.GameProgressRepository,
) : ViewModel() {

    private val level: com.lloppy.candycrash.screens.levels.game.Level = _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Levels.byId(levelId)
    private val random = Random.Default

    /** Форма поля (играбельные клетки) — для отрисовки подложки. */
    val shape: List<List<Boolean>> = level.mask

    // пороги звёзд (для UI прогресс-бара)
    val star1: Int = level.star1
    val star2: Int = level.star2
    val star3: Int = level.star3

    /** Цель уровня — для UI. */
    val objective: com.lloppy.candycrash.screens.levels.game.Objective = level.objective

    /** Текущее "устойчивое" поле между ходами (null = пустая/заблокированная клетка). */
    private var solidBoard: List<List<Gem?>> = emptyList()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        restart()
    }

    fun restart() {
        var board = _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.createBoard(level, random)
        while (!_root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.hasAvailableMove(board)) {
            board = _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.createBoard(level, random)
        }
        solidBoard = board
        _uiState.value = GameUiState(
            board = board,
            score = 0,
            movesLeft = level.moves,
            target = level.passScore,
            status = GameStatus.Playing,
            objective = level.objective,
            collected = emptyMap(),
        )
    }

    /** Тап по клетке: выбор и обмен с соседом (альтернатива свайпу). */
    fun onCellClick(pos: com.lloppy.candycrash.screens.levels.game.Pos) {
        val state = _uiState.value
        if (state.busy || state.status != GameStatus.Playing) return

        val selected = state.selected
        when {
            selected == null -> _uiState.update { it.copy(selected = pos) }
            selected == pos -> _uiState.update { it.copy(selected = null) }
            !_root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.areAdjacent(selected, pos) -> _uiState.update { it.copy(selected = pos) }
            else -> applyMove(selected, pos)
        }
    }

    /**
     * Выполняет обмен [a] <-> [b]. Анимация: обмен → "поп" совпавших → падение,
     * затем каскады. Недопустимый ход — отскок. Используется тапом и свайпом.
     */
    fun applyMove(a: com.lloppy.candycrash.screens.levels.game.Pos, b: com.lloppy.candycrash.screens.levels.game.Pos) {
        val state = _uiState.value
        if (state.busy || state.status != GameStatus.Playing) return
        if (!_root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.areAdjacent(a, b)) return

        val result = _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.tryMove(solidBoard, a, b, level, random)
        val swappedBoard = _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.swapped(solidBoard, a, b)
        // фиксируем обмен в поле СИНХРОННО — чтобы гашение смещения от пальца и
        // анимация обмена стартовали одновременно, без рывка на отпускании.
        _uiState.update { it.copy(board = swappedBoard, selected = null, busy = true) }
        viewModelScope.launch {
            delay(SWAP_MS)

            if (result == null) {
                // недопустимый ход — вернуть назад (отскок)
                _uiState.update { it.copy(board = solidBoard) }
                delay(SWAP_MS)
                _uiState.update { it.copy(busy = false) }
                return@launch
            }
            runCascade(result, swappedBoard)
        }
    }

    private suspend fun runCascade(result: com.lloppy.candycrash.screens.levels.game.MoveResult, swappedBoard: List<List<Gem?>>) {
        var prev = swappedBoard
        val steps = result.frames.size / 2
        for (k in 0 until steps) {
            val cleared = result.frames[2 * k]      // дыры + созданные спецэлементы
            val collapsed = result.frames[2 * k + 1] // после падения и досыпки

            // что лопнуло: было в prev, стало пусто в cleared
            val fx = buildList {
                for (r in prev.indices) for (c in prev[r].indices) {
                    val g = prev[r][c]
                    if (g != null && cleared[r][c] == null) add(ClearFx(r, c, g))
                }
            }

            // фаза "поп" — совпавшие сжимаются
            _uiState.update { it.copy(board = cleared, clearing = fx) }
            delay(POP_MS)

            // фаза падения — уцелевшие съезжают, новые сыпятся сверху
            val fallCells = maxFallCells(cleared, collapsed)
            _uiState.update { it.copy(board = collapsed, clearing = emptyList()) }
            delay(fallDelay(fallCells))

            prev = collapsed
        }

        var board = result.finalBoard
        if (!_root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.hasAvailableMove(board)) {
            board = reshuffle(board)
            _uiState.update { it.copy(board = board) }
        }
        solidBoard = board

        val newScore = _uiState.value.score + result.gainedScore
        val newMoves = _uiState.value.movesLeft - 1
        val newCollected = mergeCounts(_uiState.value.collected, result.clearedByColor)
        val objectiveMet = level.objective.isComplete(newScore, level.star1, newCollected)

        // Score: играем до конца ходов. Collect: победа сразу при выполнении цели.
        val newStatus = when (level.objective) {
            _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Objective.Score ->
                if (newMoves > 0) GameStatus.Playing
                else if (objectiveMet) GameStatus.Won else GameStatus.Lost
            is com.lloppy.candycrash.screens.levels.game.Objective.Collect ->
                if (objectiveMet) GameStatus.Won
                else if (newMoves > 0) GameStatus.Playing
                else GameStatus.Lost
        }
        val stars = if (newStatus == GameStatus.Won) starsFor(newScore) else 0

        _uiState.update {
            it.copy(
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

    /** Макс. расстояние падения (в клетках) между двумя кадрами. */
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

    /** Перемешивает шарики (только играбельные клетки), пока не появится ход. */
    private fun reshuffle(board: List<List<Gem?>>): List<List<Gem?>> {
        val positions = ArrayList<com.lloppy.candycrash.screens.levels.game.Pos>()
        val gems = ArrayList<Gem>()
        for (r in board.indices) for (c in board[r].indices) {
            board[r][c]?.let { positions.add(
                _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Pos(
                    r,
                    c
                )
            ); gems.add(it) }
        }
        repeat(50) {
            gems.shuffle(random)
            val grid = board.map { it.toMutableList() }
            positions.forEachIndexed { i, p -> grid[p.row][p.col] = gems[i] }
            val candidate = grid.map { it.toList() }
            if (_root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.findMatches(candidate).isEmpty() &&
                _root_ide_package_.com.lloppy.candycrash.screens.levels.game.Match3Engine.hasAvailableMove(candidate)
            ) return candidate
        }
        return board
    }

    private fun starsFor(score: Int): Int = when {
        score >= level.star3 -> 3
        score >= level.star2 -> 2
        else -> 1
    }

    companion object {
        // Тайминги синхронны с GameScreen (MS_PER_CELL/MIN/MAX совпадают).
        private const val SWAP_MS = 110L
        private const val POP_MS = 140L
        private const val MS_PER_CELL = 55L
        private const val MIN_MOVE_MS = 170L
        private const val MAX_MOVE_MS = 480L
        private const val SETTLE_MS = 40L
    }
}
