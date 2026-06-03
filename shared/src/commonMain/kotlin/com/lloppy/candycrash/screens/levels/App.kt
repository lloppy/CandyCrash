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
import com.lloppy.candycrash.screens.levels.game.SettingsRepository
import com.lloppy.candycrash.screens.levels.screens.game.GameScreen
import com.lloppy.candycrash.screens.levels.screens.home.HomeScreen
import com.lloppy.candycrash.screens.levels.screens.settings.SettingsScreen
import com.lloppy.candycrash.screens.levels.theme.GameTheme
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
    val settings = koinInject<SettingsRepository>()
    val darkTheme by settings.darkTheme.collectAsStateWithLifecycle()

    GameTheme(darkTheme = darkTheme) {
        Surface {
            val navController: NavHostController = rememberNavController()
            NavHost(navController = navController, startDestination = HomeDestination) {
                composable<HomeDestination> {
                    HomeScreen(
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
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable<GameDestination> { backStackEntry ->
                    val levelId = backStackEntry.toRoute<GameDestination>().levelId
                    GameScreen(
                        levelId = levelId,
                        onBack = { navController.popBackStack() },
                        onNextLevel = { nextId ->
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
