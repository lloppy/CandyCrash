package com.jetbrains.kmpapp.game

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * Хранит прогресс игрока: какие уровни открыты и сколько звёзд заработано.
 * Данные сохраняются между запусками через [Settings]
 * (Android SharedPreferences / iOS NSUserDefaults).
 */
class GameProgressRepository(
    private val settings: Settings,
) {

    private val _highestUnlocked = MutableStateFlow(settings.getInt(KEY_UNLOCKED, 1))
    /** Максимальный открытый уровень (1..Levels.COUNT). */
    val highestUnlocked: StateFlow<Int> = _highestUnlocked.asStateFlow()

    private val _stars = MutableStateFlow(loadStars())
    /** Лучшее количество звёзд (0..3) по каждому пройденному уровню. */
    val stars: StateFlow<Map<Int, Int>> = _stars.asStateFlow()

    fun onLevelCompleted(levelId: Int, earnedStars: Int) {
        _stars.update { current ->
            val best = maxOf(current[levelId] ?: 0, earnedStars)
            (current + (levelId to best)).also { saveStars(it) }
        }
        if (levelId == _highestUnlocked.value && levelId < Levels.COUNT) {
            _highestUnlocked.value = levelId + 1
            settings.putInt(KEY_UNLOCKED, levelId + 1)
        }
    }

    fun isUnlocked(levelId: Int): Boolean = levelId <= _highestUnlocked.value

    /** Полный сброс прогресса. */
    fun reset() {
        settings.remove(KEY_UNLOCKED)
        settings.remove(KEY_STARS)
        _highestUnlocked.value = 1
        _stars.value = emptyMap()
    }

    private fun loadStars(): Map<Int, Int> {
        val raw = settings.getStringOrNull(KEY_STARS) ?: return emptyMap()
        return runCatching { Json.decodeFromString<Map<Int, Int>>(raw) }.getOrDefault(emptyMap())
    }

    private fun saveStars(map: Map<Int, Int>) {
        settings.putString(KEY_STARS, Json.encodeToString(map))
    }

    private companion object {
        const val KEY_UNLOCKED = "progress.highestUnlocked"
        const val KEY_STARS = "progress.stars"
    }
}
