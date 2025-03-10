package com.devpush.morty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.devpush.morty.ui.theme.MortyTheme
import com.devpush.network.KtorClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.devpush.morty.screens.CharacterDetailsScreen
import com.devpush.morty.ui.theme.RickPrimary
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.devpush.morty.screens.HomeScreen
import com.devpush.morty.ui.theme.RickAction
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.vectorResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.devpush.morty.screens.AllEpisodesScreen
import com.devpush.morty.screens.CharacterEpisodeScreen
import com.devpush.morty.screens.SaveScreen
import com.devpush.morty.viewmodels.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ktorClient: KtorClient

    private val viewModel by viewModels<SplashViewModel>()

    sealed class NavDestination(val title: String, val route: String, val icon: ImageVector) {
        object Home :
            NavDestination(title = "Home", route = "home_screen",
                icon = Icons.Filled.Home)

        object Episodes :
            NavDestination(title = "Episodes", route = "episodes", icon = Icons.Filled.PlayArrow)

        object Save :
            NavDestination(title = "Save", route = "save", icon = Icons.Filled.Star)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Morty)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        installSplashScreen()
            .apply {
                setKeepOnScreenCondition {
                    viewModel.isLoading.value
                }
            }

        setContent {
            val navController = rememberNavController()
            val items = listOf(
                NavDestination.Home, NavDestination.Episodes, NavDestination.Save
            )
            var selectedIndex by remember { mutableIntStateOf(0) }

            MortyTheme {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = RickPrimary
                        ) {
                            items.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(imageVector = screen.icon, contentDescription = null)
                                    },
                                    label = { Text(screen.title) },
                                    selected = index == selectedIndex,
                                    onClick = {
                                        selectedIndex = index
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            // Avoid multiple copies of the same destination when
                                            // reselecting the same item
                                            launchSingleTop = true
                                            // Restore state when reselecting a previously selected item
                                            restoreState = true
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = RickAction,
                                        selectedTextColor = RickAction,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavigationHost(
                        navController = navController,
                        ktorClient = ktorClient,
                        innerPadding = innerPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationHost(
    navController: NavHostController,
    ktorClient: KtorClient,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = "home_screen",
        modifier = Modifier
            .background(color = RickPrimary)
            .padding(innerPadding)
    ) {
        composable(route = "home_screen") {
            HomeScreen(
                onCharacterSelected = { characterId ->
                    navController.navigate("character_details/$characterId")
                }
            )
        }

        composable(
            route = "character_details/{characterId}",
            arguments = listOf(navArgument("characterId") {
                type = NavType.IntType
            })
        ) { backStackEntry ->
            val characterId: Int =
                backStackEntry.arguments?.getInt("characterId") ?: -1
            CharacterDetailsScreen(
                characterId = characterId,
                onEpisodeClicked = { navController.navigate("character_episodes/$it") },
                onBackClicked = { navController.navigateUp() }
            )
        }

        composable(
            route = "character_episodes/{characterId}",
            arguments = listOf(navArgument("characterId") {
                type = NavType.IntType
            })
        ) { backStackEntry ->
            val characterId: Int =
                backStackEntry.arguments?.getInt("characterId") ?: -1
            CharacterEpisodeScreen(
                characterId = characterId,
                ktorClient = ktorClient,
                onBackClicked = { navController.navigateUp() }
            )
        }

        composable(route = MainActivity.NavDestination.Episodes.route) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                AllEpisodesScreen()
            }
        }

        composable(route = MainActivity.NavDestination.Save.route) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                SaveScreen()
            }
        }
    }
}
