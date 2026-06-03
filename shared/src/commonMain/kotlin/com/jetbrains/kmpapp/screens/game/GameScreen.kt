package com.jetbrains.kmpapp.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jetbrains.kmpapp.game.Gem
import com.jetbrains.kmpapp.game.GemColor
import com.jetbrains.kmpapp.game.Levels
import com.jetbrains.kmpapp.game.Objective
import com.jetbrains.kmpapp.game.Pos
import com.jetbrains.kmpapp.game.SettingsRepository
import com.jetbrains.kmpapp.theme.LocalIsDarkTheme
import com.jetbrains.kmpapp.ui.GameBackground
import com.jetbrains.kmpapp.ui.GameDialog
import com.jetbrains.kmpapp.ui.GameTitle
import com.jetbrains.kmpapp.ui.GemVisual
import com.jetbrains.kmpapp.ui.GlossyCard
import com.jetbrains.kmpapp.ui.RoundIconButton
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val Gold = Color(0xFFFFC107)

// Падение с постоянной скоростью (мс/клетку), с порогами. Синхронно с GameViewModel.
private const val MS_PER_CELL = 55
private const val MIN_MOVE_MS = 170
private const val MAX_MOVE_MS = 480
private const val POP_MS = 140

private fun moveDuration(distCells: Float): Int =
    (distCells * MS_PER_CELL).roundToInt().coerceIn(MIN_MOVE_MS, MAX_MOVE_MS)

@Composable
fun GameScreen(
    levelId: Int,
    onBack: () -> Unit,
    onNextLevel: (Int) -> Unit,
) {
    val viewModel = koinViewModel<GameViewModel> { parametersOf(levelId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val settings = koinInject<SettingsRepository>()
    val sound by settings.soundEnabled.collectAsStateWithLifecycle()
    var showPause by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        GameBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        ) {
            // верхняя панель
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    GameTitle(text = "Уровень $levelId", fontSize = 24.sp)
                }
                RoundIconButton(onClick = { showPause = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Меню")
                }
            }

            Spacer(Modifier.height(16.dp))

            StatsPanel(
                score = state.score,
                movesLeft = state.movesLeft,
                objective = state.objective,
                collected = state.collected,
                star1 = viewModel.star1,
                star2 = viewModel.star2,
                star3 = viewModel.star3,
            )

            Spacer(Modifier.height(20.dp))

            BoardFrame {
                GameBoard(
                    board = state.board,
                    shape = viewModel.shape,
                    selected = state.selected,
                    clearing = state.clearing,
                    enabled = !state.busy && state.status == GameStatus.Playing,
                    onCellTap = viewModel::onCellClick,
                    onSwipe = viewModel::applyMove,
                )
            }
        }
    }

    if (state.status != GameStatus.Playing) {
        ResultDialog(
            won = state.status == GameStatus.Won,
            score = state.score,
            stars = state.earnedStars,
            hasNext = levelId < Levels.COUNT,
            onRetry = viewModel::restart,
            onNext = { onNextLevel(levelId + 1) },
            onExit = onBack,
        )
    }

    if (showPause) {
        PauseDialog(
            sound = sound,
            onToggleSound = settings::setSoundEnabled,
            onResume = { showPause = false },
            onRestart = { showPause = false; viewModel.restart() },
            onExit = onBack,
        )
    }
}

@Composable
private fun PauseDialog(
    sound: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    GameDialog(onDismiss = onResume) {
        GameTitle(text = "Пауза", fontSize = 28.sp)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Звук",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = sound, onCheckedChange = onToggleSound)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) { Text("Продолжить") }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("Заново") }
        Spacer(Modifier.height(2.dp))
        TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Выход в меню") }
    }
}

// ---------------------------------------------------------------------
// Панель счёта/ходов со звёздным прогрессом
// ---------------------------------------------------------------------

@Composable
private fun StatsPanel(
    score: Int,
    movesLeft: Int,
    objective: Objective,
    collected: Map<GemColor, Int>,
    star1: Int,
    star2: Int,
    star3: Int,
) {
    GlossyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatPill(
                    label = "ХОДЫ",
                    value = movesLeft.toString(),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                when (objective) {
                    Objective.Score -> StatPill(
                        label = "ОЧКИ",
                        value = score.toString(),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    is Objective.Collect -> Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for ((color, need) in objective.targets) {
                            val remaining = (need - (collected[color] ?: 0)).coerceAtLeast(0)
                            CollectChip(color = color, remaining = remaining)
                        }
                    }
                }
            }
            // звёздный прогресс по очкам — показываем всегда (звёзды по очкам)
            Spacer(Modifier.height(14.dp))
            StarProgressBar(score = score, star1 = star1, star2 = star2, star3 = star3)
        }
    }
}

/** Чип цели «собрать»: иконка цвета (под тему) + остаток. */
@Composable
private fun CollectChip(color: GemColor, remaining: Int) {
    val done = remaining == 0
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GemVisual(gem = Gem(color), modifier = Modifier.size(26.dp))
        if (done) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = remaining.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = onContainer.copy(alpha = 0.75f))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = onContainer)
    }
}

@Composable
private fun StarProgressBar(score: Int, star1: Int, star2: Int, star3: Int) {
    val maxScore = star3.coerceAtLeast(1).toFloat()
    val fraction = (score / maxScore).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction, tween(450), label = "starBar")
    val starSize = 24.dp

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(starSize),
        contentAlignment = Alignment.CenterStart,
    ) {
        val w = maxWidth
        // дорожка
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterStart),
        )
        // заполнение
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFB300), Gold)))
                .align(Alignment.CenterStart),
        )
        // три звезды на порогах
        listOf(star1, star2, star3).forEach { threshold ->
            val pos = (threshold.toFloat() / maxScore).coerceIn(0f, 1f)
            val reached = score >= threshold
            val x = (w * pos - starSize / 2).coerceIn(0.dp, w - starSize)
            Box(
                Modifier
                    .offset(x = x)
                    .size(starSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (reached) Gold else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(starSize - 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Поле в рамке с подложкой-плитками
// ---------------------------------------------------------------------

@Composable
private fun BoardFrame(content: @Composable () -> Unit) {
    val dark = LocalIsDarkTheme.current
    val outer = if (dark) {
        Brush.verticalGradient(listOf(Color(0xFF3A2E66), Color(0xFF1E1840)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF0E6FF), Color(0xFFD9C9F5)))
    }
    val inner = if (dark) Color(0xFF140F2E) else Color(0xFFECE4FA)

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(10.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(outer)
            .padding(10.dp),
    ) {
        // утопленная внутренняя панель
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(inner)
                .padding(6.dp),
        ) { content() }
    }
}

/** Перетаскивание: шарик [aId] едет за пальцем, сосед [bId] — навстречу. */
private data class DragState(
    val aId: Long,
    val bId: Long,
    val a: Pos,
    val b: Pos,
    val horizontal: Boolean,
    val sign: Int,
    val vec: Offset,
)

@Composable
private fun GameBoard(
    board: List<List<Gem?>>,
    shape: List<List<Boolean>>,
    selected: Pos?,
    clearing: List<ClearFx>,
    enabled: Boolean,
    onCellTap: (Pos) -> Unit,
    onSwipe: (Pos, Pos) -> Unit,
) {
    if (board.isEmpty()) return
    val rows = board.size
    val cols = board[0].size
    val tileA = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val tileB = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell = maxWidth / cols
        val cellPx = with(LocalDensity.current) { cell.toPx() }

        fun toCell(off: Offset) = Pos(
            row = (off.y / cellPx).toInt().coerceIn(0, rows - 1),
            col = (off.x / cellPx).toInt().coerceIn(0, cols - 1),
        )
        fun playable(p: Pos) = shape.getOrNull(p.row)?.getOrNull(p.col) == true

        // стартовые строки для НОВЫХ шариков — стопкой над полем (без наложений)
        val prevIds = remember { mutableStateOf(emptySet<Long>()) }
        val startRowById = remember(board) {
            val map = HashMap<Long, Int>()
            val prev = prevIds.value
            for (c in 0 until cols) {
                val newIds = ArrayList<Long>()
                for (r in 0 until rows) {
                    val g = board[r][c] ?: continue
                    if (g.id !in prev) newIds.add(g.id)
                }
                val k = newIds.size
                newIds.forEachIndexed { i, id -> map[id] = i - k }
            }
            map
        }
        LaunchedEffect(board) { prevIds.value = board.flatten().mapNotNull { it?.id }.toSet() }

        val boardState = rememberUpdatedState(board)
        var drag by remember { mutableStateOf<DragState?>(null) }
        fun idAt(p: Pos) = boardState.value.getOrNull(p.row)?.getOrNull(p.col)?.id

        // На отпускании просто фиксируем обмен (синхронно) и снимаем drag —
        // каждый шарик сам плавно доедет до клетки из текущей позиции под пальцем.
        fun finishDrag(commit: Boolean) {
            val d = drag ?: return
            if (commit) onSwipe(d.a, d.b)
            drag = null
        }

        val gestures = Modifier
            .pointerInput(rows, cols, cellPx, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { off -> toCell(off).let { if (playable(it)) onCellTap(it) } }
            }
            .pointerInput(rows, cols, cellPx, enabled) {
                if (!enabled) return@pointerInput
                var start: Pos? = null
                var acc = Offset.Zero
                detectDragGestures(
                    onDragStart = { off -> start = toCell(off).takeIf { playable(it) }; acc = Offset.Zero },
                    onDragEnd = {
                        val d = drag
                        val mag = if (d == null) 0f else if (d.horizontal) abs(d.vec.x) else abs(d.vec.y)
                        finishDrag(commit = mag > cellPx / 2f)
                        start = null
                    },
                    onDragCancel = { finishDrag(commit = false); start = null },
                ) { change, dragAmount ->
                    change.consume()
                    val s = start ?: return@detectDragGestures
                    acc += dragAmount
                    if (drag == null && acc.getDistance() > cellPx * 0.12f) {
                        val horizontal = abs(acc.x) >= abs(acc.y)
                        val sign = if (horizontal) (if (acc.x > 0) 1 else -1) else (if (acc.y > 0) 1 else -1)
                        val target = if (horizontal) Pos(s.row, s.col + sign) else Pos(s.row + sign, s.col)
                        if (target.row in 0 until rows && target.col in 0 until cols && playable(target)) {
                            val aId = idAt(s)
                            val bId = idAt(target)
                            if (aId != null && bId != null) {
                                drag = DragState(aId, bId, s, target, horizontal, sign, Offset.Zero)
                            }
                        }
                    }
                    val d = drag
                    if (d != null && d.a == s) {
                        val along = if (d.horizontal) acc.x else acc.y
                        val mag = (along * d.sign).coerceIn(0f, cellPx)
                        val vec = if (d.horizontal) Offset(d.sign * mag, 0f) else Offset(0f, d.sign * mag)
                        drag = d.copy(vec = vec)
                    }
                }
            }

        Box(
            Modifier
                .fillMaxWidth()
                .height(cell * rows)
                .then(gestures),
        ) {
            // подложка-плитки (форма поля)
            Column {
                for (r in 0 until rows) {
                    Row {
                        for (c in 0 until cols) {
                            Box(Modifier.size(cell)) {
                                if (shape.getOrNull(r)?.getOrNull(c) == true) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if ((r + c) % 2 == 0) tileA else tileB)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // шарики: одна анимируемая позиция; при перетаскивании едут за пальцем
            val d = drag
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val gem = board[r][c] ?: continue
                    val dragging = d != null && (gem.id == d.aId || gem.id == d.bId)
                    val dragVec = when {
                        d != null && gem.id == d.aId -> d.vec
                        d != null && gem.id == d.bId -> Offset(-d.vec.x, -d.vec.y)
                        else -> Offset.Zero
                    }
                    key(gem.id) {
                        AnimatedGem(
                            gem = gem,
                            row = r,
                            col = c,
                            startRow = startRowById[gem.id] ?: r,
                            dragX = dragVec.x,
                            dragY = dragVec.y,
                            dragging = dragging,
                            cell = cell,
                            cellPx = cellPx,
                            selected = selected == Pos(r, c),
                        )
                    }
                }
            }
            // эффект "поп" — совпавшие шарики сжимаются
            for (fx in clearing) {
                key("fx", fx.gem.id) {
                    ClearGem(fx = fx, cell = cell, cellPx = cellPx)
                }
            }
        }
    }
}

/** Шарик, анимирующий позицию по id: новые падают сверху (со [startRow]), уцелевшие съезжают. */
@Composable
private fun AnimatedGem(
    gem: Gem,
    row: Int,
    col: Int,
    startRow: Int,
    dragX: Float,
    dragY: Float,
    dragging: Boolean,
    cell: Dp,
    cellPx: Float,
    selected: Boolean,
) {
    // единая позиция шарика в пикселях; init: новые — над полем (startRow), падают вниз
    val animX = remember { Animatable(col * cellPx) }
    val animY = remember { Animatable(startRow * cellPx) }
    val targetX = col * cellPx + dragX
    val targetY = row * cellPx + dragY
    LaunchedEffect(targetX, targetY, dragging) {
        if (dragging) {
            // под палец — мгновенно
            animX.snapTo(targetX)
            animY.snapTo(targetY)
        } else {
            val dist = max(abs(targetX - animX.value), abs(targetY - animY.value)) / cellPx
            if (dist < 0.01f) {
                animX.snapTo(targetX)
                animY.snapTo(targetY)
            } else {
                val dur = moveDuration(dist)
                coroutineScope {
                    launch { animX.animateTo(targetX, tween(dur)) }
                    launch { animY.animateTo(targetY, tween(dur)) }
                }
            }
        }
    }
    val selScale by animateFloatAsState(if (selected) 1.12f else 1f, label = "selScale")

    Box(
        Modifier
            .offset { IntOffset(animX.value.roundToInt(), animY.value.roundToInt()) }
            .size(cell)
            .zIndex(if (dragging || selected) 1f else 0f),
    ) {
        GemVisual(
            gem = gem,
            selected = selected,
            modifier = Modifier.fillMaxSize().padding(3.dp).scale(selScale),
        )
    }
}

/** Анимация исчезновения совпавшего шарика: быстро сжимается и гаснет. */
@Composable
private fun ClearGem(fx: ClearFx, cell: Dp, cellPx: Float) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) { scale.animateTo(0f, tween(POP_MS)) }
    Box(
        Modifier
            .offset { IntOffset((fx.col * cellPx).roundToInt(), (fx.row * cellPx).roundToInt()) }
            .size(cell)
            .zIndex(2f),
    ) {
        GemVisual(
            gem = fx.gem,
            modifier = Modifier.fillMaxSize().padding(3.dp).scale(scale.value),
        )
    }
}

@Composable
private fun ResultDialog(
    won: Boolean,
    score: Int,
    stars: Int,
    hasNext: Boolean,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
) {
    GameDialog(onDismiss = {}) {
        GameTitle(text = if (won) "Уровень пройден!" else "Не получилось", fontSize = 26.sp)
        Spacer(Modifier.height(14.dp))
        if (won) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i -> ResultStar(earned = i < stars, index = i) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = "Очки: $score",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        if (won && hasNext) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Дальше") }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Ещё раз") }
        } else {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Ещё раз") }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("Выход") }
        }
    }
}

/** Звезда результата: появляется с задержкой и лёгким «отскоком». */
@Composable
private fun ResultStar(earned: Boolean, index: Int) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 160L)
        scale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        tint = if (earned) Gold else MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.size(48.dp).scale(if (earned) scale.value else 1f),
    )
}
