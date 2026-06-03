package com.lloppy.candycrash.screens.levels.game

import androidx.compose.ui.graphics.Color

/** Базовый цвет шарика. */
enum class GemColor(val color: Color) {
    Red(Color(0xFFE53935)),
    Blue(Color(0xFF1E88E5)),
    Green(Color(0xFF43A047)),
    Yellow(Color(0xFFFDD835)),
    Purple(Color(0xFF8E24AA)),
    Orange(Color(0xFFFB8C00)),
    Cyan(Color(0xFF00ACC1));

    companion object {
        val MAX_COLORS = entries.size
    }
}

/**
 * Тип спецэлемента (бонуса), создаётся при совпадении 4+ шариков.
 *
 * - [ROCKET_H] / [ROCKET_V] — "ракета" (полосатая конфета): чистит строку/столбец.
 * - [BOMB] — "бомба" (обёрнутая конфета): взрыв 3×3.
 * - [COLOR_BOMB] — "звезда" (цветная бомба): убирает все шарики одного цвета.
 */
enum class Special { NONE, ROCKET_H, ROCKET_V, BOMB, COLOR_BOMB }

/**
 * Один шарик на поле: цвет + возможный бонус.
 * [id] — стабильный идентификатор для анимации перемещения/падения
 * (по нему UI отслеживает один и тот же шарик между кадрами).
 */
data class Gem(
    val color: GemColor,
    val special: Special = Special.NONE,
    val id: Long = 0,
) {
    val isColorBomb: Boolean get() = special == Special.COLOR_BOMB
    val isSpecial: Boolean get() = special != Special.NONE
}
