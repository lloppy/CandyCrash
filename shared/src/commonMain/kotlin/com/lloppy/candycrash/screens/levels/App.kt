package com.lloppy.candycrash.screens.levels

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object HomeDestination

@Serializable
object LevelsDestination

@Serializable
object SettingsDestination

@Serializable
data class GameDestination(val levelId: Int)

@Composable
fun App() {
    val settings = koinInject<com.lloppy.candycrash.screens.levels.game.SettingsRepository>()
    val darkTheme by settings.darkTheme.collectAsStateWithLifecycle()

    _root_ide_package_.com.lloppy.candycrash.screens.levels.theme.GameTheme(darkTheme = darkTheme) {
        Surface {
            val navController: NavHostController = rememberNavController()
            NavHost(navController = navController, startDestination = HomeDestination) {
                composable<HomeDestination> {
                    _root_ide_package_.com.lloppy.candycrash.screens.levels.screens.home.HomeScreen(
                        darkTheme = darkTheme,
                        onToggleTheme = { settings.toggleDarkTheme() },
                        onPlay = { navController.navigate(LevelsDestination) },
                        onSettings = { navController.navigate(SettingsDestination) },
                    )
                }
                composable<LevelsDestination> {
                    LevelsScreen(
                        onLevelClick = { levelId -> navController.navigate(GameDestination(levelId)) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<SettingsDestination> {
                    _root_ide_package_.com.lloppy.candycrash.screens.levels.screens.settings.SettingsScreen(
                        onBack = { navController.popBackStack() })
                }
                composable<GameDestination> { backStackEntry ->
                    val levelId = backStackEntry.toRoute<GameDestination>().levelId
                    _root_ide_package_.com.lloppy.candycrash.screens.levels.screens.game.GameScreen(
                        levelId = levelId,
                        onBack = { navController.popBackStack() },
                        onNextLevel = { nextId ->
                            // заменяем текущий игровой экран следующим уровнем
                            navController.navigate(GameDestination(nextId)) {
                                popUpTo<GameDestination> { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}
