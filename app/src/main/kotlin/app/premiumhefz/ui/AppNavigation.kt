package app.premiumhefz.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.premiumhefz.R
import app.premiumhefz.domain.PageCoordinates
import app.premiumhefz.ui.bookmarks.BookmarksScreen
import app.premiumhefz.ui.browse.BrowseScreen
import app.premiumhefz.ui.home.HomeScreen
import app.premiumhefz.ui.reader.ReaderScreen
import app.premiumhefz.ui.settings.SettingsScreen
import app.premiumhefz.ui.setup.SetupScreen
import app.premiumhefz.ui.theme.brand

object Routes {
    const val HOME = "home"
    const val BROWSE = "browse?tab={tab}"
    const val BOOKMARKS = "bookmarks"
    const val SETTINGS = "settings"
    const val SETUP = "setup"
    const val READER = "reader/{page}"

    fun browse(tab: Int) = "browse?tab=$tab"
    fun reader(page: Int) = "reader/$page"
}

private class TabItem(
    val pattern: String,
    val target: String,
    val label: Int,
    val icon: @Composable () -> Unit
)

private fun NavHostController.navigateToTab(target: String) {
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavigation(
    showWelcome: Boolean = false,
    onWelcomeStarted: () -> Unit = {}
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    val tabs = listOf(
        TabItem(Routes.HOME, Routes.HOME, R.string.nav_home) { Icon(Icons.Filled.Home, contentDescription = null) },
        TabItem(Routes.BROWSE, Routes.browse(0), R.string.nav_browse) {
            Icon(painterResource(R.drawable.ic_book), contentDescription = null)
        },
        TabItem(Routes.BOOKMARKS, Routes.BOOKMARKS, R.string.nav_bookmarks) {
            Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null)
        },
        TabItem(Routes.SETTINGS, Routes.SETTINGS, R.string.nav_settings) { Icon(Icons.Filled.Settings, contentDescription = null) }
    )
    val showBar = tabs.any { it.pattern == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.pattern,
                            onClick = { nav.navigateToTab(tab.target) },
                            icon = tab.icon,
                            label = { Text(stringResource(tab.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.brand.gold,
                                selectedTextColor = MaterialTheme.brand.gold,
                                indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onContinue = { nav.navigate(Routes.reader(it)) },
                    onOpenBrowse = { nav.navigate(Routes.browse(it)) },
                    onOpenBookmarks = { nav.navigateToTab(Routes.BOOKMARKS) },
                    onImportPages = { nav.navigate(Routes.SETUP) },
                    showWelcome = showWelcome,
                    onWelcomeStarted = onWelcomeStarted
                )
            }
            composable(
                route = Routes.BROWSE,
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
                BrowseScreen(
                    initialTab = backStackEntry.arguments?.getInt("tab") ?: 0,
                    onOpenPage = { nav.navigate(Routes.reader(it)) }
                )
            }
            composable(Routes.BOOKMARKS) { BookmarksScreen(onOpenPage = { nav.navigate(Routes.reader(it)) }) }
            composable(Routes.SETTINGS) { SettingsScreen(onOpenSetup = { nav.navigate(Routes.SETUP) }) }
            composable(Routes.SETUP) { SetupScreen(onBack = { nav.popBackStack() }) }
            composable(
                route = Routes.READER,
                arguments = listOf(navArgument("page") { type = NavType.IntType })
            ) { backStackEntry ->
                ReaderScreen(
                    startPage = backStackEntry.arguments?.getInt("page") ?: PageCoordinates.FIRST_PAGE,
                    onBack = { nav.popBackStack() },
                    onImportPages = { nav.navigate(Routes.SETUP) }
                )
            }
        }
    }
}
