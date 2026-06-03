package com.lloppy.candycrash.screens.levels.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private class Particle(
    val x0: Float,
    val y0: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val w: Float,
    val h: Float,
    val spin: Float,
)

private val confettiColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFDD835),
    Color(0xFF8E24AA), Color(0xFFFB8C00), Color(0xFFFF4FA3), Color(0xFF00E5FF),
    Color(0xFFFFFFFF),
)

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier, count: Int = 120) {
    val particles = remember {
        List(count) { i ->
            val left = i % 2 == 0
            val dirX = if (left) 1f else -1f
            Particle(
                x0 = if (left) 0.04f else 0.96f,
                y0 = 1.03f,
                vx = dirX * (0.15f + Random.nextFloat() * 0.75f),
                vy = -(0.9f + Random.nextFloat() * 0.75f),
                color = confettiColors[Random.nextInt(confettiColors.size)],
                w = 0.012f + Random.nextFloat() * 0.018f,
                h = (0.012f + Random.nextFloat() * 0.018f) * (0.5f + Random.nextFloat()),
                spin = (Random.nextFloat() - 0.5f) * 5f,
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2400, easing = LinearEasing)) }

    Canvas(modifier.fillMaxSize()) {
        val tt = t.value
        if (tt >= 1f) return@Canvas
        val g = 1.8f
        particles.forEach { p ->
            val alpha = ((1f - tt) / 0.3f).coerceIn(0f, 1f)
            if (alpha <= 0.01f) return@forEach
            val x = (p.x0 + p.vx * tt) * size.width
            val y = (p.y0 + p.vy * tt + 0.5f * g * tt * tt) * size.height
            if (y > size.height * 1.1f) return@forEach
            val pw = p.w * size.minDimension
            val ph = p.h * size.minDimension
            rotate(degrees = p.spin * tt * 540f, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - pw / 2f, y - ph / 2f),
                    size = Size(pw, ph),
                    cornerRadius = CornerRadius(pw * 0.3f),
                )
            }
        }
    }
}
