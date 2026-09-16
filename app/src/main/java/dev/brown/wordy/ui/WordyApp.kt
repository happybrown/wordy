package dev.brown.wordy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.brown.wordy.di.AppContainer
import dev.brown.wordy.domain.model.Level
import dev.brown.wordy.ui.browse.BrowseScreen
import dev.brown.wordy.ui.browse.UnitScreen
import dev.brown.wordy.ui.components.LoadingBox
import dev.brown.wordy.ui.components.LocalAppContainer
import dev.brown.wordy.ui.home.HomeScreen
import dev.brown.wordy.ui.navigation.BrowseRoute
import dev.brown.wordy.ui.navigation.HomeRoute
import dev.brown.wordy.ui.navigation.OnboardingRoute
import dev.brown.wordy.ui.navigation.PracticeRoute
import dev.brown.wordy.ui.practice.PracticeScreen
import dev.brown.wordy.ui.navigation.SessionRoute
import dev.brown.wordy.ui.navigation.SettingsRoute
import dev.brown.wordy.ui.navigation.StatsRoute
import dev.brown.wordy.ui.navigation.UnitRoute
import dev.brown.wordy.ui.navigation.WordRoute
import dev.brown.wordy.ui.onboarding.OnboardingScreen
import dev.brown.wordy.ui.session.SessionScreen
import dev.brown.wordy.ui.settings.SettingsScreen
import dev.brown.wordy.ui.stats.StatsScreen
import dev.brown.wordy.ui.word.WordDetailScreen
import kotlinx.coroutines.flow.first

@Composable
fun WordyApp(container: AppContainer) {
    CompositionLocalProvider(LocalAppContainer provides container) {
        var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(Unit) { onboardingDone = container.settings.settings.first().onboardingDone }
        val done = onboardingDone
        if (done == null) { LoadingBox(); return@CompositionLocalProvider }
        val nav = rememberNavController()
        val backStack by nav.currentBackStackEntryAsState()
        val dest = backStack?.destination
        val showBar = dest?.hasRoute(HomeRoute::class) == true || dest?.hasRoute(BrowseRoute::class) == true ||
            dest?.hasRoute(StatsRoute::class) == true || dest?.hasRoute(SettingsRoute::class) == true
        var browseLevel by remember { mutableStateOf<Level?>(null) }

        Scaffold(bottomBar = {
            if (showBar) NavigationBar {
                NavigationBarItem(selected = dest?.hasRoute(HomeRoute::class) == true, onClick = { nav.navigateTab(HomeRoute) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("홈") })
                NavigationBarItem(selected = dest?.hasRoute(BrowseRoute::class) == true, onClick = { browseLevel = null; nav.navigateTab(BrowseRoute) }, icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) }, label = { Text("단어장") })
                NavigationBarItem(selected = dest?.hasRoute(StatsRoute::class) == true, onClick = { nav.navigateTab(StatsRoute) }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("통계") })
                NavigationBarItem(selected = dest?.hasRoute(SettingsRoute::class) == true, onClick = { nav.navigateTab(SettingsRoute) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("설정") })
            }
        }) { padding ->
            NavHost(nav, startDestination = if (done) HomeRoute else OnboardingRoute, modifier = Modifier.padding(padding)) {
                composable<OnboardingRoute> { OnboardingScreen(onDone = { nav.navigate(HomeRoute) { popUpTo(OnboardingRoute) { inclusive = true } } }) }
                composable<HomeRoute> {
                    HomeScreen(
                        onStartSession = { extra -> nav.navigate(SessionRoute(extra)) },
                        onOpenCore = { browseLevel = null; nav.navigateTab(BrowseRoute) },
                        onOpenLevel = { lvl -> browseLevel = lvl; nav.navigateTab(BrowseRoute) },
                    )
                }
                composable<BrowseRoute> { BrowseScreen(initialLevel = browseLevel, onOpenUnit = { id, title -> nav.navigate(UnitRoute(id, title)) }, onOpenWord = { nav.navigate(WordRoute(it)) }) }
                composable<StatsRoute> { StatsScreen(onWord = { nav.navigate(WordRoute(it)) }) }
                composable<SettingsRoute> { SettingsScreen() }
                composable<SessionRoute> { entry -> SessionScreen(extraNew = entry.toRoute<SessionRoute>().extraNew, onExit = { nav.popBackStack() }, onOpenWord = { nav.navigate(WordRoute(it)) }) }
                composable<UnitRoute> { entry -> val r = entry.toRoute<UnitRoute>(); UnitScreen(r.unitId, r.title, onBack = { nav.popBackStack() }, onOpenWord = { nav.navigate(WordRoute(it)) }) }
                composable<WordRoute> { entry -> WordDetailScreen(entry.toRoute<WordRoute>().wordId, onBack = { nav.popBackStack() }, onPractice = { nav.navigate(PracticeRoute(it)) }) }
                composable<PracticeRoute> { entry -> PracticeScreen(entry.toRoute<PracticeRoute>().wordId, onExit = { nav.popBackStack() }) }
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateTab(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
