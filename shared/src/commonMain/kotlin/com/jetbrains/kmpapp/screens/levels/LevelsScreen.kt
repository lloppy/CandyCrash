package com.jetbrains.kmpapp.screens.levels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jetbrains.kmpapp.game.Levels
import com.jetbrains.kmpapp.ui.GameBackground
import com.jetbrains.kmpapp.ui.GlossyCard
import com.jetbrains.kmpapp.ui.RoundIconButton
import org.koin.compose.viewmodel.koinViewModel

private val Gold = Color(0xFFFFC107)

@Composable
fun LevelsScreen(
    onLevelClick: (levelId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<LevelsViewModel>()
    val highestUnlocked by viewModel.highestUnlocked.collectAsStateWithLifecycle()
    val stars by viewModel.stars.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        GameBackground()

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    text = "Выбор уровня",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.size(46.dp))
            }

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
            ) {
                items(Levels.all, key = { it.id }) { level ->
                    val unlocked = level.id <= highestUnlocked
                    LevelTile(
                        number = level.id,
                        unlocked = unlocked,
                        stars = stars[level.id] ?: 0,
                        onClick = { if (unlocked) onLevelClick(level.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelTile(
    number: Int,
    unlocked: Boolean,
    stars: Int,
    onClick: () -> Unit,
) {
    GlossyCard(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = unlocked, onClick = onClick),
        cornerRadius = 18.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (unlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = number.toString(),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row {
                        repeat(3) { i ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (i < stars) Gold else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Закрыто",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}
