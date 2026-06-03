package com.jetbrains.kmpapp.screens.levels

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.jetbrains.kmpapp.game.Gem
import com.jetbrains.kmpapp.game.GemColor
import com.jetbrains.kmpapp.game.Level
import com.jetbrains.kmpapp.game.Levels
import com.jetbrains.kmpapp.game.Objective
import com.jetbrains.kmpapp.ui.GameBackground
import com.jetbrains.kmpapp.ui.GameDialog
import com.jetbrains.kmpapp.ui.GemVisual
import com.jetbrains.kmpapp.ui.Gold
import com.jetbrains.kmpapp.ui.GlossyCard
import com.jetbrains.kmpapp.ui.RoundIconButton
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.sin

private const val NODE = 78          // диаметр узла, dp
private val ROW_HEIGHT = 132.dp      // высота строки одного уровня

/** Доля по горизонтали для узла i (змейка). Единый источник для дорожки и узлов. */
private fun xFrac(i: Int): Float = 0.5f + 0.30f * sin(i * (PI / 2)).toFloat()

@Composable
fun LevelsScreen(
    onLevelClick: (levelId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<LevelsViewModel>()
    val highestUnlocked by viewModel.highestUnlocked.collectAsStateWithLifecycle()
    val stars by viewModel.stars.collectAsStateWithLifecycle()

    var infoLevel by remember { mutableStateOf<Level?>(null) }
    val scroll = rememberScrollState()
    val density = LocalDensity.current

    // авто-скролл к текущему (последнему открытому) уровню
    LaunchedEffect(highestUnlocked) {
        val targetPx = with(density) { (ROW_HEIGHT * (highestUnlocked - 1)).toPx() }.toInt()
        scroll.animateScrollTo(targetPx.coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize()) {
        GameBackground()

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            // шапка-баннер
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    GlossyCard(cornerRadius = 18.dp) {
                        Text(
                            text = "Карта уровней",
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.size(46.dp))
            }

            // прокручиваемая карта-змейка
            Box(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll),
            ) {
                LevelMap(
                    highestUnlocked = highestUnlocked,
                    starsByLevel = stars,
                    onNodeClick = { lvl -> infoLevel = lvl },
                )
            }
        }
    }

    infoLevel?.let { lvl ->
        LevelInfoDialog(
            level = lvl,
            bestStars = stars[lvl.id] ?: 0,
            onPlay = { infoLevel = null; onLevelClick(lvl.id) },
            onDismiss = { infoLevel = null },
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

        // дорожка между центрами узлов (плавная змейка)
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

        // узлы (снизу — первый уровень виден последним; рисуем сверху вниз 1..N)
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
    // пульс текущего узла
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
        Text(
            text = "Уровень ${level.id}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        // лучший результат
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
        // цель
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
        // пороги звёзд по очкам
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
