package com.lloppy.candycrash.screens.levels

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.lloppy.candycrash.screens.levels.game.Gem
import com.lloppy.candycrash.screens.levels.game.Level
import com.lloppy.candycrash.screens.levels.game.Levels
import com.lloppy.candycrash.screens.levels.game.Objective
import com.lloppy.candycrash.screens.levels.screens.levels.LevelsAction
import com.lloppy.candycrash.screens.levels.screens.levels.LevelsEvent
import com.lloppy.candycrash.screens.levels.screens.levels.LevelsViewModel
import com.lloppy.candycrash.screens.levels.ui.GameBackground
import com.lloppy.candycrash.screens.levels.ui.GameDialog
import com.lloppy.candycrash.screens.levels.ui.GameTitle
import com.lloppy.candycrash.screens.levels.ui.GemVisual
import com.lloppy.candycrash.screens.levels.ui.GlossyCard
import com.lloppy.candycrash.screens.levels.ui.Gold
import com.lloppy.candycrash.screens.levels.ui.RoundIconButton
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.sin

private const val NODE = 78
private val ROW_HEIGHT = 132.dp

private fun xFrac(i: Int): Float = 0.5f + 0.30f * sin(i * (PI / 2)).toFloat()

@Composable
fun LevelsScreen(
    onLevelClick: (levelId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<LevelsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LevelsEvent.NavigateToGame -> onLevelClick(event.levelId)
            }
        }
    }

    LaunchedEffect(state.highestUnlocked) {
        val targetPx = with(density) { (ROW_HEIGHT * (state.highestUnlocked - 1)).toPx() }.toInt()
        scroll.animateScrollTo(targetPx.coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize()) {
        GameBackground()

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    GameTitle(text = "Карта уровней", fontSize = 26.sp)
                }
                Spacer(Modifier.size(46.dp))
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll),
            ) {
                LevelMap(
                    highestUnlocked = state.highestUnlocked,
                    starsByLevel = state.stars,
                    onNodeClick = { lvl -> viewModel.onAction(LevelsAction.NodeClicked(lvl)) },
                )
            }
        }
    }

    state.infoLevel?.let { lvl ->
        LevelInfoDialog(
            level = lvl,
            bestStars = state.stars[lvl.id] ?: 0,
            onPlay = { viewModel.onAction(LevelsAction.PlayClicked(lvl.id)) },
            onDismiss = { viewModel.onAction(LevelsAction.DismissInfo) },
        )
    }
}

@Composable
private fun LevelMap(
    highestUnlocked: Int,
    starsByLevel: Map<Int, Int>,
    onNodeClick: (Level) -> Unit,
) {
    val count = Levels.COUNT
    val pathColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT * count),
    ) {
        val w = maxWidth
        val nodeSize = NODE.dp

        Canvas(Modifier.fillMaxSize()) {
            val path = Path()
            fun cx(i: Int) = size.width * xFrac(i)
            fun cy(i: Int) = size.height / count * (i + 0.5f)
            path.moveTo(cx(0), cy(0))
            for (i in 1 until count) {
                val midY = (cy(i - 1) + cy(i)) / 2f
                path.cubicTo(cx(i - 1), midY, cx(i), midY, cx(i), cy(i))
            }
            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = 10.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 26f)),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }

        for (i in 0 until count) {
            val level = Levels.all[i]
            val unlocked = level.id <= highestUnlocked
            val centerX = w * xFrac(i)
            val centerY = ROW_HEIGHT * i + ROW_HEIGHT / 2
            Box(
                Modifier
                    .offset(x = centerX - nodeSize / 2, y = centerY - nodeSize / 2)
                    .size(nodeSize),
            ) {
                LevelNode(
                    number = level.id,
                    unlocked = unlocked,
                    isCurrent = level.id == highestUnlocked,
                    stars = starsByLevel[level.id] ?: 0,
                    onClick = { if (unlocked) onNodeClick(level) },
                )
            }
        }
    }
}

@Composable
private fun LevelNode(
    number: Int,
    unlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    onClick: () -> Unit,
) {

    val pulse = if (isCurrent) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(
            initialValue = 1f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulseScale",
        ).value
    } else 1f

    Box(Modifier.fillMaxSize().scale(pulse), contentAlignment = Alignment.Center) {
        GlossyCard(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (unlocked) 1f else 0.5f)
                .clickable(enabled = unlocked, onClick = onClick),
            cornerRadius = (NODE / 2).dp,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (unlocked) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = number.toString(),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row {
                            repeat(3) { s ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (s < stars) Gold else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Закрыто",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelInfoDialog(
    level: Level,
    bestStars: Int,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    GameDialog(onDismiss = onDismiss) {
        GameTitle(text = "Уровень ${level.id}", fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { s ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (s < bestStars) Gold else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Цель",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        when (val obj = level.objective) {
            Objective.Score -> Text(
                text = "Набрать ${level.star1} очков",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            is Objective.Collect -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((color, need) in obj.targets) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GemVisual(gem = Gem(color), modifier = Modifier.size(30.dp))
                        Text(
                            text = " ×$need",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Text(
            text = "★ ${level.star1}   ★★ ${level.star2}   ★★★ ${level.star3}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Играть", fontSize = 18.sp)
        }
    }
}
