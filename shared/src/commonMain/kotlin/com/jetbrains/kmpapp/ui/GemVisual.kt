package com.jetbrains.kmpapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.jetbrains.kmpapp.game.Gem
import com.jetbrains.kmpapp.game.GemColor
import com.jetbrains.kmpapp.game.Special
import com.jetbrains.kmpapp.theme.LocalIsDarkTheme
import kmp_app_template.shared.generated.resources.Res
import kmp_app_template.shared.generated.resources.gem_blue
import kmp_app_template.shared.generated.resources.gem_cyan
import kmp_app_template.shared.generated.resources.gem_green
import kmp_app_template.shared.generated.resources.gem_orange
import kmp_app_template.shared.generated.resources.gem_purple
import kmp_app_template.shared.generated.resources.gem_red
import kmp_app_template.shared.generated.resources.gem_yellow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val rainbow = GemColor.entries.map { it.color }

private fun jewelRes(color: GemColor): DrawableResource = when (color) {
    GemColor.Red -> Res.drawable.gem_red
    GemColor.Blue -> Res.drawable.gem_blue
    GemColor.Green -> Res.drawable.gem_green
    GemColor.Yellow -> Res.drawable.gem_yellow
    GemColor.Purple -> Res.drawable.gem_purple
    GemColor.Orange -> Res.drawable.gem_orange
    GemColor.Cyan -> Res.drawable.gem_cyan
}

/**
 * Рисует один шарик:
 *  - светлая тема — глянцевый гранёный самоцвет (SVG);
 *  - тёмная тема — светящаяся "планета".
 * Цветная бомба всегда радужная; спецэлементы помечаются белым.
 */
@Composable
fun GemVisual(
    gem: Gem,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val dark = LocalIsDarkTheme.current
    Box(modifier) {
        when {
            gem.special == Special.COLOR_BOMB ->
                Canvas(Modifier.fillMaxSize()) { drawColorBomb() }
            dark ->
                Canvas(Modifier.fillMaxSize()) { drawPlanet(gem.color.color) }
            else ->
                Image(
                    painter = painterResource(jewelRes(gem.color)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
        }
        // спецэлементы и подсветка выбора — поверх
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawSpecialMark(gem.special, center, radius)
            if (selected) {
                drawCircle(Color.White, radius = radius, center = center, style = Stroke(width = radius * 0.16f))
            }
        }
    }
}

private fun DrawScope.drawPlanet(base: Color) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(base.copy(alpha = 0.25f), radius = radius, center = center)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(lerp(base, Color.White, 0.5f), base, lerp(base, Color.Black, 0.45f)),
            center = Offset(center.x - radius * 0.32f, center.y - radius * 0.32f),
            radius = radius * 1.3f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = radius * 0.16f,
        center = Offset(center.x - radius * 0.38f, center.y - radius * 0.38f),
    )
}

private fun DrawScope.drawColorBomb() {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(brush = Brush.sweepGradient(rainbow + rainbow.first(), center), radius = radius, center = center)
    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = radius * 0.22f, center = center)
}

private fun DrawScope.drawSpecialMark(special: Special, center: Offset, radius: Float) {
    val white = Color.White.copy(alpha = 0.92f)
    when (special) {
        Special.ROCKET_H -> {
            val bar = radius * 0.16f
            listOf(-0.42f, 0f, 0.42f).forEach { f ->
                drawRect(
                    color = white,
                    topLeft = Offset(center.x - radius * 0.8f, center.y + f * radius - bar / 2f),
                    size = Size(radius * 1.6f, bar),
                )
            }
        }
        Special.ROCKET_V -> {
            val bar = radius * 0.16f
            listOf(-0.42f, 0f, 0.42f).forEach { f ->
                drawRect(
                    color = white,
                    topLeft = Offset(center.x + f * radius - bar / 2f, center.y - radius * 0.8f),
                    size = Size(bar, radius * 1.6f),
                )
            }
        }
        Special.BOMB -> {
            drawCircle(white, radius = radius * 0.92f, center = center, style = Stroke(width = radius * 0.18f))
            drawCircle(white, radius = radius * 0.28f, center = center)
        }
        else -> {}
    }
}
