package com.jetbrains.kmpapp.game

import kotlin.math.abs
import kotlin.random.Random

/** Позиция клетки на поле. */
data class Pos(val row: Int, val col: Int)

/**
 * Результат хода: последовательность кадров для анимации + итог.
 *
 * Поле представлено как `List<List<Gem?>>`, где `null` — пустая клетка:
 * либо заблокированная формой уровня, либо временно опустевшая во время каскада.
 */
data class MoveResult(
    val frames: List<List<List<Gem?>>>,
    val finalBoard: List<List<Gem?>>,
    val gainedScore: Int,
)

private data class Analysis(
    val matched: Set<Pos>,
    val spawns: Map<Pos, Gem>,
)

/**
 * Логика "Три в ряд" в стиле Candy Crush: спецэлементы, комбинации, каскады.
 * Учитывает форму поля (заблокированные клетки) из [Level.mask].
 */
object Match3Engine {

    private const val CLEAR_POINTS = 10
    private const val CASCADE_BONUS = 20
    private const val MAX_CASCADES = 100

    private var nextId = 1L
    private fun newId(): Long = nextId++

    private fun randomGem(colors: Int, random: Random): Gem =
        Gem(GemColor.entries[random.nextInt(colors)], id = newId())

    /** Совпадают по цвету (цветные бомбы и пустые клетки не участвуют в линиях). */
    private fun colorMatch(a: Gem?, b: Gem?): Boolean =
        a != null && b != null &&
            a.special != Special.COLOR_BOMB && b.special != Special.COLOR_BOMB &&
            a.color == b.color

    // ---------------------------------------------------------------------
    // Создание поля
    // ---------------------------------------------------------------------

    /** Заполняет играбельные клетки без готовых совпадений; пустые — null. */
    fun createBoard(level: Level, random: Random): List<List<Gem?>> {
        val grid = MutableList(level.rows) { MutableList<Gem?>(level.cols) { null } }
        for (r in 0 until level.rows) {
            for (c in 0 until level.cols) {
                if (!level.playable(r, c)) continue
                var gem: Gem
                do {
                    gem = randomGem(level.colors, random)
                } while (
                    (c >= 2 && grid[r][c - 1]?.color == gem.color && grid[r][c - 2]?.color == gem.color) ||
                    (r >= 2 && grid[r - 1][c]?.color == gem.color && grid[r - 2][c]?.color == gem.color)
                )
                grid[r][c] = gem
            }
        }
        return grid.map { it.toList() }
    }

    fun areAdjacent(a: Pos, b: Pos): Boolean =
        abs(a.row - b.row) + abs(a.col - b.col) == 1

    fun swapped(board: List<List<Gem?>>, a: Pos, b: Pos): List<List<Gem?>> {
        val grid = board.map { it.toMutableList() }
        val tmp = grid[a.row][a.col]
        grid[a.row][a.col] = grid[b.row][b.col]
        grid[b.row][b.col] = tmp
        return grid.map { it.toList() }
    }

    // ---------------------------------------------------------------------
    // Совпадения и спецэлементы
    // ---------------------------------------------------------------------

    fun findMatches(board: List<List<Gem?>>): Set<Pos> {
        if (board.isEmpty()) return emptySet()
        val rows = board.size
        val cols = board[0].size
        val matched = mutableSetOf<Pos>()
        for (r in 0 until rows) {
            var start = 0
            for (c in 1..cols) {
                if (c < cols && colorMatch(board[r][c], board[r][start])) continue
                if (c - start >= 3) for (k in start until c) matched.add(Pos(r, k))
                start = c
            }
        }
        for (c in 0 until cols) {
            var start = 0
            for (r in 1..rows) {
                if (r < rows && colorMatch(board[r][c], board[start][c])) continue
                if (r - start >= 3) for (k in start until r) matched.add(Pos(k, c))
                start = r
            }
        }
        return matched
    }

    private fun components(board: List<List<Gem?>>, matched: Set<Pos>): List<Set<Pos>> {
        val seen = mutableSetOf<Pos>()
        val result = mutableListOf<Set<Pos>>()
        for (p in matched) {
            if (p in seen) continue
            val color = board[p.row][p.col]!!.color
            val comp = mutableSetOf<Pos>()
            val stack = ArrayDeque<Pos>()
            stack.addLast(p)
            seen.add(p)
            while (stack.isNotEmpty()) {
                val cur = stack.removeLast()
                comp.add(cur)
                for (n in listOf(
                    Pos(cur.row - 1, cur.col), Pos(cur.row + 1, cur.col),
                    Pos(cur.row, cur.col - 1), Pos(cur.row, cur.col + 1),
                )) {
                    if (n in matched && n !in seen && board[n.row][n.col]?.color == color) {
                        seen.add(n)
                        stack.addLast(n)
                    }
                }
            }
            result.add(comp)
        }
        return result
    }

    private fun maxHorizontal(comp: Set<Pos>): Int =
        comp.groupBy { it.row }.maxOf { (_, cells) -> longestConsecutive(cells.map { it.col }) }

    private fun maxVertical(comp: Set<Pos>): Int =
        comp.groupBy { it.col }.maxOf { (_, cells) -> longestConsecutive(cells.map { it.row }) }

    private fun longestConsecutive(values: List<Int>): Int {
        val sorted = values.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) run++ else run = 1
            if (run > best) best = run
        }
        return best
    }

    private fun intersectionCell(comp: Set<Pos>): Pos? {
        for (p in comp) {
            var h = 1; var k = p.col - 1
            while (Pos(p.row, k) in comp) { h++; k-- }
            k = p.col + 1
            while (Pos(p.row, k) in comp) { h++; k++ }
            var v = 1; var m = p.row - 1
            while (Pos(m, p.col) in comp) { v++; m-- }
            m = p.row + 1
            while (Pos(m, p.col) in comp) { v++; m++ }
            if (h >= 3 && v >= 3) return p
        }
        return null
    }

    private fun analyze(board: List<List<Gem?>>, swapPositions: Set<Pos>): Analysis? {
        val matched = findMatches(board)
        if (matched.isEmpty()) return null

        val spawns = mutableMapOf<Pos, Gem>()
        for (comp in components(board, matched)) {
            if (comp.size < 3) continue
            val color = board[comp.first().row][comp.first().col]!!.color
            val maxH = maxHorizontal(comp)
            val maxV = maxVertical(comp)
            val cross = intersectionCell(comp)

            val special = when {
                maxH >= 5 || maxV >= 5 -> Special.COLOR_BOMB
                cross != null -> Special.BOMB
                maxH == 4 -> Special.ROCKET_H
                maxV == 4 -> Special.ROCKET_V
                else -> Special.NONE
            }
            if (special == Special.NONE) continue

            val spawnPos = comp.firstOrNull { it in swapPositions }
                ?: cross
                ?: comp.sortedWith(compareBy({ it.row }, { it.col }))[comp.size / 2]
            spawns[spawnPos] = Gem(color, special, id = newId())
        }
        return Analysis(matched, spawns)
    }

    // ---------------------------------------------------------------------
    // Активация спецэлементов и цепные реакции
    // ---------------------------------------------------------------------

    private fun mostCommonColor(board: List<List<Gem?>>): GemColor {
        val counts = HashMap<GemColor, Int>()
        for (row in board) for (g in row) {
            if (g != null && g.special != Special.COLOR_BOMB) counts[g.color] = (counts[g.color] ?: 0) + 1
        }
        return counts.maxByOrNull { it.value }?.key ?: GemColor.Red
    }

    private fun inBounds(board: List<List<Gem?>>, r: Int, c: Int) =
        r in board.indices && c in board[0].indices

    /** Расширяет очистку: спецэлементы внутри неё активируются (цепная реакция). */
    private fun expandBlast(board: List<List<Gem?>>, initial: Set<Pos>): Set<Pos> {
        val rows = board.size
        val cols = board[0].size
        val cleared = initial.filterTo(mutableSetOf()) { board[it.row][it.col] != null }
        val queue = ArrayDeque(cleared)
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            val g = board[p.row][p.col] ?: continue
            val add = mutableListOf<Pos>()
            when (g.special) {
                Special.ROCKET_H -> for (c in 0 until cols) if (board[p.row][c] != null) add.add(Pos(p.row, c))
                Special.ROCKET_V -> for (r in 0 until rows) if (board[r][p.col] != null) add.add(Pos(r, p.col))
                Special.BOMB -> for (dr in -1..1) for (dc in -1..1)
                    if (inBounds(board, p.row + dr, p.col + dc) && board[p.row + dr][p.col + dc] != null)
                        add.add(Pos(p.row + dr, p.col + dc))
                Special.COLOR_BOMB -> {
                    val target = mostCommonColor(board)
                    for (r in 0 until rows) for (c in 0 until cols) {
                        val cell = board[r][c]
                        if (cell != null && cell.color == target && cell.special != Special.COLOR_BOMB)
                            add.add(Pos(r, c))
                    }
                }
                Special.NONE -> {}
            }
            for (n in add) if (cleared.add(n)) queue.addLast(n)
        }
        return cleared
    }

    /** Активация при обмене спецэлементов (или цветной бомбы с обычным шариком). */
    private fun swapActivation(board: List<List<Gem?>>, a: Pos, b: Pos): Set<Pos>? {
        val ga = board[a.row][a.col] ?: return null
        val gb = board[b.row][b.col] ?: return null
        val rows = board.size
        val cols = board[0].size
        val center = b

        fun rowCells(r: Int) = (0 until cols).mapNotNull { c -> if (board[r][c] != null) Pos(r, c) else null }
        fun colCells(c: Int) = (0 until rows).mapNotNull { r -> if (board[r][c] != null) Pos(r, c) else null }
        fun area(p: Pos, radius: Int) = buildList {
            for (dr in -radius..radius) for (dc in -radius..radius)
                if (inBounds(board, p.row + dr, p.col + dc) && board[p.row + dr][p.col + dc] != null)
                    add(Pos(p.row + dr, p.col + dc))
        }
        fun allOfColor(color: GemColor) = buildList {
            for (r in 0 until rows) for (c in 0 until cols) {
                val cell = board[r][c]
                if (cell != null && cell.color == color && cell.special != Special.COLOR_BOMB) add(Pos(r, c))
            }
        }

        if (ga.isColorBomb && gb.isColorBomb) {
            return buildSet {
                for (r in 0 until rows) for (c in 0 until cols) if (board[r][c] != null) add(Pos(r, c))
            }
        }
        if (ga.isColorBomb || gb.isColorBomb) {
            val other = if (ga.isColorBomb) gb else ga
            return when (other.special) {
                Special.ROCKET_H, Special.ROCKET_V -> buildSet {
                    add(a); add(b)
                    allOfColor(other.color).forEach { addAll(rowCells(it.row)); addAll(colCells(it.col)) }
                }
                Special.BOMB -> buildSet {
                    add(a); add(b)
                    allOfColor(other.color).forEach { addAll(area(it, 1)) }
                }
                else -> buildSet {
                    add(a); add(b)
                    addAll(allOfColor(other.color))
                }
            }
        }
        val aSpecial = ga.special
        val bSpecial = gb.special
        val aLineBomb = aSpecial == Special.ROCKET_H || aSpecial == Special.ROCKET_V || aSpecial == Special.BOMB
        val bLineBomb = bSpecial == Special.ROCKET_H || bSpecial == Special.ROCKET_V || bSpecial == Special.BOMB
        if (aLineBomb && bLineBomb) {
            val rockets = aSpecial != Special.BOMB && bSpecial != Special.BOMB
            return when {
                rockets -> buildSet { addAll(rowCells(center.row)); addAll(colCells(center.col)) }
                aSpecial == Special.BOMB && bSpecial == Special.BOMB -> area(center, 2).toSet()
                else -> buildSet {
                    for (dr in -1..1) addAll(rowCells((center.row + dr).coerceIn(0, rows - 1)))
                    for (dc in -1..1) addAll(colCells((center.col + dc).coerceIn(0, cols - 1)))
                }
            }
        }
        return null
    }

    // ---------------------------------------------------------------------
    // Очистка, гравитация (по сегментам формы), досыпка
    // ---------------------------------------------------------------------

    private fun applyClear(
        board: List<List<Gem?>>,
        clear: Set<Pos>,
        spawns: Map<Pos, Gem>,
    ): List<List<Gem?>> {
        val grid = board.map { it.toMutableList() }
        for (p in clear) grid[p.row][p.col] = null
        for ((p, gem) in spawns) grid[p.row][p.col] = gem
        return grid.map { it.toList() }
    }

    /**
     * Гравитация с учётом формы: в каждом столбце шарики падают вниз внутри
     * непрерывных сегментов играбельных клеток, сверху досыпаются новые.
     * Заблокированные клетки остаются пустыми и работают как "пол".
     */
    private fun collapseAndRefill(
        board: List<List<Gem?>>,
        level: Level,
        random: Random,
    ): List<List<Gem?>> {
        val rows = level.rows
        val cols = level.cols
        val grid = MutableList(rows) { MutableList<Gem?>(cols) { null } }
        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                if (!level.playable(r, c)) { r++; continue }
                var end = r
                while (end < rows && level.playable(end, c)) end++
                // сегмент строк [r, end)
                val existing = ArrayList<Gem>()
                for (row in r until end) board[row][c]?.let { existing.add(it) }
                val newCount = (end - r) - existing.size
                var idx = 0
                for (row in r until end) {
                    grid[row][c] = if (idx < newCount) randomGem(level.colors, random) else existing[idx - newCount]
                    idx++
                }
                r = end
            }
        }
        return grid.map { it.toList() }
    }

    // ---------------------------------------------------------------------
    // Полный ход
    // ---------------------------------------------------------------------

    fun tryMove(initial: List<List<Gem?>>, a: Pos, b: Pos, level: Level, random: Random): MoveResult? {
        var board = swapped(initial, a, b)

        var clear: Set<Pos>
        var spawns: Map<Pos, Gem>
        val swapAct = swapActivation(board, a, b)
        if (swapAct != null) {
            clear = expandBlast(board, swapAct)
            spawns = emptyMap()
        } else {
            val analysis = analyze(board, setOf(a, b)) ?: return null
            clear = expandBlast(board, analysis.matched)
            spawns = analysis.spawns
        }

        val frames = mutableListOf<List<List<Gem?>>>()
        var gained = 0
        var cascade = 0

        while (cascade < MAX_CASCADES) {
            gained += clear.size * CLEAR_POINTS + cascade * CASCADE_BONUS
            cascade++

            val cleared = applyClear(board, clear, spawns)
            frames.add(cleared)

            board = collapseAndRefill(cleared, level, random)
            frames.add(board)

            val next = analyze(board, emptySet()) ?: break
            clear = expandBlast(board, next.matched)
            spawns = next.spawns
        }

        return MoveResult(frames, board, gained)
    }

    // ---------------------------------------------------------------------
    // Доступность ходов
    // ---------------------------------------------------------------------

    /** Есть ли полезный ход (или цветная бомба, которой всегда можно сходить). */
    fun hasAvailableMove(board: List<List<Gem?>>): Boolean {
        val rows = board.size
        val cols = board[0].size
        for (r in 0 until rows) for (c in 0 until cols) {
            val here = board[r][c] ?: continue
            if (here.isColorBomb) return true
            if (c + 1 < cols && board[r][c + 1] != null) {
                if (here.isSpecial && board[r][c + 1]!!.isSpecial) return true
                if (findMatches(swapped(board, Pos(r, c), Pos(r, c + 1))).isNotEmpty()) return true
            }
            if (r + 1 < rows && board[r + 1][c] != null) {
                if (here.isSpecial && board[r + 1][c]!!.isSpecial) return true
                if (findMatches(swapped(board, Pos(r, c), Pos(r + 1, c))).isNotEmpty()) return true
            }
        }
        return false
    }
}
