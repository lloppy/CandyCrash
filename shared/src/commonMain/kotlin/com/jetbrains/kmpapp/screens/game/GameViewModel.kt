package com.jetbrains.kmpapp.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.game.Gem
import com.jetbrains.kmpapp.game.GameProgressRepository
import com.jetbrains.kmpapp.game.Level
import com.jetbrains.kmpapp.game.Levels
import com.jetbrains.kmpapp.game.Match3Engine
import com.jetbrains.kmpapp.game.MoveResult
import com.jetbrains.kmpapp.game.Pos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameStatus { Playing, Won, Lost }

data class GameUiState(
    /** Поле для отображения: null означает пустую клетку (во время анимации). */
    val board: List<List<Gem?>> = emptyList(),
    val score: Int = 0,
    val movesLeft: Int = 0,
    val target: Int = 0,
    val selected: Pos? = null,
    val status: GameStatus = GameStatus.Playing,
    val earnedStars: Int = 0,
    /** Идёт анимация хода — ввод заблокирован. */
    val busy: Boolean = false,
)

class GameViewModel(
    val levelId: Int,
    private val progress: GameProgressRepository,
) : ViewModel() {

    private val level: Level = Levels.byId(levelId)
    private val random = Random.Default

    /** Форма поля (играбельные клетки) — для отрисовки подложки. */
    val shape: List<List<Boolean>> = level.mask

    // пороги звёзд (для UI прогресс-бара)
    val star1: Int = level.star1
    val star2: Int = level.star2
    val star3: Int = level.star3

    /** Текущее "устойчивое" поле между ходами (null = пустая/заблокированная клетка). */
    private var solidBoard: List<List<Gem?>> = emptyList()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        restart()
    }

    fun restart() {
        var board = Match3Engine.createBoard(level, random)
        while (!Match3Engine.hasAvailableMove(board)) {
            board = Match3Engine.createBoard(level, random)
        }
        solidBoard = board
        _uiState.value = GameUiState(
            board = board,
            score = 0,
            movesLeft = level.moves,
            target = level.passScore,
            status = GameStatus.Playing,
        )
    }

    /** Тап по клетке: выбор и обмен с соседом (альтернатива свайпу). */
    fun onCellClick(pos: Pos) {
        val state = _uiState.value
        if (state.busy || state.status != GameStatus.Playing) return

        val selected = state.selected
        when {
            selected == null -> _uiState.update { it.copy(selected = pos) }
            selected == pos -> _uiState.update { it.copy(selected = null) }
            !Match3Engine.areAdjacent(selected, pos) -> _uiState.update { it.copy(selected = pos) }
            else -> applyMove(selected, pos)
        }
    }

    /**
     * Выполняет обмен [a] <-> [b]. UI анимирует перемещение шариков по смене
     * состояния поля: сначала показываем обмен, затем — каскад или "отскок".
     * Используется тапом и свайпом.
     */
    fun applyMove(a: Pos, b: Pos) {
        val state = _uiState.value
        if (state.busy || state.status != GameStatus.Playing) return
        if (!Match3Engine.areAdjacent(a, b)) return

        val result = Match3Engine.tryMove(solidBoard, a, b, level, random)
        viewModelScope.launch {
            // показываем обмен (шарики скользят навстречу)
            _uiState.update {
                it.copy(board = Match3Engine.swapped(solidBoard, a, b), selected = null, busy = true)
            }
            delay(SWAP_MS)

            if (result == null) {
                // недопустимый ход — возвращаем шарики на место (отскок)
                _uiState.update { it.copy(board = solidBoard) }
                delay(SWAP_MS)
                _uiState.update { it.copy(busy = false) }
                return@launch
            }
            runCascade(result)
        }
    }

    private suspend fun runCascade(result: MoveResult) {
        // Кадры идут парами: [с дырами, после падения]. Показываем только осевшие
        // (нечётные), чтобы очистка и падение шли одним плавным движением.
        // Задержка = реальному времени падения (макс. расстояние × скорость) + осадка.
        result.frames.forEachIndexed { i, frame ->
            if (i % 2 == 1) {
                val cells = maxFallCells(_uiState.value.board, frame)
                _uiState.update { it.copy(board = frame) }
                delay(moveDelay(cells))
            }
        }

        // на случай "мёртвого" поля — перемешиваем
        var board = result.finalBoard
        if (!Match3Engine.hasAvailableMove(board)) {
            board = reshuffle(board)
            _uiState.update { it.copy(board = board) }
        }
        solidBoard = board

        val newScore = _uiState.value.score + result.gainedScore
        val newMoves = _uiState.value.movesLeft - 1
        // уровень идёт до конца ходов; звёзды считаются по итоговому счёту
        val newStatus = when {
            newMoves > 0 -> GameStatus.Playing
            newScore >= level.star1 -> GameStatus.Won
            else -> GameStatus.Lost
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
            )
        }
        if (newStatus == GameStatus.Won) progress.onLevelCompleted(level.id, stars)
    }

    /** Перемешивает шарики (только играбельные клетки), пока не появится ход. */
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
            if (Match3Engine.findMatches(candidate).isEmpty() &&
                Match3Engine.hasAvailableMove(candidate)
            ) return candidate
        }
        return board
    }

    private fun starsFor(score: Int): Int = when {
        score >= level.star3 -> 3
        score >= level.star2 -> 2
        else -> 1
    }

    /** Максимальное расстояние падения (в клетках) между двумя кадрами поля. */
    private fun maxFallCells(prev: List<List<Gem?>>, cur: List<List<Gem?>>): Int {
        val prevRow = HashMap<Long, Int>()
        for (r in prev.indices) for (c in prev[r].indices) prev[r][c]?.let { prevRow[it.id] = r }
        var maxd = 0
        for (c in 0 until level.cols) {
            var newInCol = 0
            for (r in 0 until level.rows) {
                val g = cur[r][c] ?: continue
                val pr = prevRow[g.id]
                if (pr != null) {
                    if (r - pr > maxd) maxd = r - pr
                } else {
                    newInCol++ // новый шарик падает сверху (стопкой высотой newInCol)
                }
            }
            if (newInCol > maxd) maxd = newInCol
        }
        return maxd
    }

    private fun moveDelay(cells: Int): Long =
        (cells * MS_PER_CELL).coerceIn(MIN_MOVE_MS, MAX_MOVE_MS) + SETTLE_MS

    companion object {
        // Тайминги синхронны с GameScreen (постоянная скорость падения).
        private const val SWAP_MS = 150L
        private const val MS_PER_CELL = 55L
        private const val MIN_MOVE_MS = 120L
        private const val MAX_MOVE_MS = 480L
        private const val SETTLE_MS = 60L
    }
}
