package com.lloppy.candycrash.screens.levels.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import com.lloppy.candycrash.screens.levels.game.Gem
import com.lloppy.candycrash.screens.levels.game.GemColor
import com.lloppy.candycrash.screens.levels.game.Special
import kmp_app_template.shared.generated.resources.Res
import kmp_app_template.shared.generated.resources.gem_overlay_blue
import kmp_app_template.shared.generated.resources.gem_overlay_cyan
import kmp_app_template.shared.generated.resources.gem_overlay_green
import kmp_app_template.shared.generated.resources.gem_overlay_orange
import kmp_app_template.shared.generated.resources.gem_overlay_purple
import kmp_app_template.shared.generated.resources.gem_overlay_red
import kmp_app_template.shared.generated.resources.gem_overlay_yellow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val rainbow = GemColor.entries.map { it.color }

private const val GEM_IMAGE_FRACTION = 0.90f

private const val GEM_TINT_ALPHA = 0.01f

private fun overlayRes(color: GemColor): DrawableResource = when (color) {
    GemColor.Red -> Res.drawable.gem_overlay_red
    GemColor.Blue -> Res.drawable.gem_overlay_blue
    GemColor.Green -> Res.drawable.gem_overlay_green
    GemColor.Yellow -> Res.drawable.gem_overlay_yellow
    GemColor.Purple -> Res.drawable.gem_overlay_purple
    GemColor.Orange -> Res.drawable.gem_overlay_orange
    GemColor.Cyan -> Res.drawable.gem_overlay_cyan
}

@Composable
fun GemVisual(
    gem: Gem,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (gem.special == Special.COLOR_BOMB) drawColorBomb() else drawPlanet(gem.color.color)
        }
        if (gem.special != Special.COLOR_BOMB) {
            Image(
                painter = painterResource(overlayRes(gem.color)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                    gem.color.color.copy(alpha = GEM_TINT_ALPHA),
                    BlendMode.SrcAtop,
                ),
                modifier = Modifier.fillMaxSize(GEM_IMAGE_FRACTION).clip(CircleShape),
            )
        }
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
