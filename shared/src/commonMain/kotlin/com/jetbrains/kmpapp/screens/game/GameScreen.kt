package com.jetbrains.kmpapp.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import com.jetbrains.kmpapp.game.Levels
import com.jetbrains.kmpapp.game.Pos
import com.jetbrains.kmpapp.game.SettingsRepository
import com.jetbrains.kmpapp.theme.LocalIsDarkTheme
import com.jetbrains.kmpapp.ui.GameBackground
import com.jetbrains.kmpapp.ui.GameDialog
import com.jetbrains.kmpapp.ui.GemVisual
import com.jetbrains.kmpapp.ui.GlossyCard
import com.jetbrains.kmpapp.ui.RoundIconButton
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val Gold = Color(0xFFFFC107)

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
                Text(
                    text = "Уровень $levelId",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                RoundIconButton(onClick = { showPause = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Меню")
                }
            }

            Spacer(Modifier.height(16.dp))

            StatsPanel(
                score = state.score,
                movesLeft = state.movesLeft,
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
        Text(
            text = "Пауза",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
private fun StatsPanel(score: Int, movesLeft: Int, star1: Int, star2: Int, star3: Int) {
    GlossyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatPill(
                    label = "ОЧКИ",
                    value = score.toString(),
                    container = MaterialTheme.colorScheme.primaryContainer,
                    onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "ХОДЫ",
                    value = movesLeft.toString(),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            StarProgressBar(score = score, star1 = star1, star2 = star2, star3 = star3)
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

@Composable
private fun GameBoard(
    board: List<List<Gem?>>,
    shape: List<List<Boolean>>,
    selected: Pos?,
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

        val gestures = Modifier
            .pointerInput(rows, cols, cellPx, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { off -> toCell(off).let { if (playable(it)) onCellTap(it) } }
            }
            .pointerInput(rows, cols, cellPx, enabled) {
                if (!enabled) return@pointerInput
                var start: Pos? = null
                var fired = false
                var acc = Offset.Zero
                detectDragGestures(
                    onDragStart = { off -> start = toCell(off).takeIf { playable(it) }; fired = false; acc = Offset.Zero },
                    onDragEnd = { start = null; fired = false },
                    onDragCancel = { start = null; fired = false },
                ) { change, dragAmount ->
                    change.consume()
                    val s = start
                    if (!fired && s != null) {
                        acc += dragAmount
                        if (acc.getDistance() > cellPx * 0.30f) {
                            fired = true
                            val (dr, dc) = if (abs(acc.x) > abs(acc.y)) {
                                0 to (if (acc.x > 0) 1 else -1)
                            } else {
                                (if (acc.y > 0) 1 else -1) to 0
                            }
                            val target = Pos(s.row + dr, s.col + dc)
                            if (target.row in 0 until rows && target.col in 0 until cols && playable(target)) {
                                onSwipe(s, target)
                            }
                        }
                    }
                }
            }

        Column(gestures) {
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
                            val gem = board[r][c]
                            if (gem != null) {
                                GemVisual(
                                    gem = gem,
                                    selected = selected == Pos(r, c),
                                    modifier = Modifier.fillMaxSize().padding(3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
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
        Text(
            text = if (won) "Уровень пройден!" else "Не получилось",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(14.dp))
        if (won) {
            Row {
                repeat(3) { i ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (i < stars) Gold else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(44.dp),
                    )
                }
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
