package com.lloppy.candycrash.screens.levels.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lloppy.candycrash.screens.levels.game.Gem
import com.lloppy.candycrash.screens.levels.game.GemColor
import com.lloppy.candycrash.screens.levels.ui.GameBackground
import com.lloppy.candycrash.screens.levels.ui.GameTitle
import com.lloppy.candycrash.screens.levels.ui.GemVisual
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        GameBackground()

        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(8.dp),
        ) {
            ThemeToggle(
                dark = darkTheme,
                onClick = onToggleTheme,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GemColor.entries.take(5).forEach { color ->
                    GemVisual(
                        gem = Gem(
                            color
                        ), modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            GameTitle(
                text = "Три в ряд",
                fontSize = 46.sp
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Играть", fontSize = 20.sp)
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Настройки", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun ThemeToggle(
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gold = Color(0xFFFFD700)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(28.dp)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            if (dark) {

                drawCircle(gold, radius = r * 0.5f, center = center)
                for (i in 0 until 8) {
                    val a = i * (PI / 4)
                    val dx = cos(a).toFloat()
                    val dy = sin(a).toFloat()
                    drawLine(
                        color = gold,
                        start = Offset(center.x + dx * r * 0.65f, center.y + dy * r * 0.65f),
                        end = Offset(center.x + dx * r * 0.95f, center.y + dy * r * 0.95f),
                        strokeWidth = r * 0.14f,
                    )
                }
            } else {

                val outer = Path().apply { addOval(Rect(center, r * 0.8f)) }
                val cut = Path().apply {
                    addOval(Rect(Offset(center.x + r * 0.45f, center.y - r * 0.12f), r * 0.78f))
                }
                val moon = Path().apply { op(outer, cut, PathOperation.Difference) }
                drawPath(moon, gold)
            }
        }
    }
}
