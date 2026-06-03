package com.lloppy.candycrash.screens.levels.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val xFrac: Float,
    val yFrac: Float,
    val baseAlpha: Float,
    val radius: Float,
    val twinkleSpeed: Float,
    val twinkleOffset: Float,
)

/** Мерцающее звёздное небо для тёмной (космической) темы. */
@Composable
fun StarryBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 120,
) {
    val stars = remember(starCount) {
        List(starCount) {
            val rng = Random(it * 31 + 7)
            Star(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                baseAlpha = rng.nextFloat() * 0.5f + 0.15f,
                radius = rng.nextFloat() * 1.8f + 0.4f,
                twinkleSpeed = rng.nextFloat() * 3000f + 2000f,
                twinkleOffset = rng.nextFloat() * 6.28f,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2832f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "starTime",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            val twinkle = sin(time * (5000f / star.twinkleSpeed) + star.twinkleOffset)
            val alpha = (star.baseAlpha + twinkle * 0.25f).coerceIn(0.03f, 0.9f)
            val r = star.radius * (1f + twinkle * 0.15f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = r,
                center = Offset(star.xFrac * size.width, star.yFrac * size.height),
            )
        }
    }
}
