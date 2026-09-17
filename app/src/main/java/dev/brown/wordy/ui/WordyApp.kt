package dev.brown.wordy.ui

import androidx.activity.compose.BackHandler
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
import dev.brown.wordy.ui.browse.BrowseTab
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
        var browseTab by remember { mutableStateOf<BrowseTab?>(null) }

        Scaffold(bottomBar = {
            if (showBar) NavigationBar {
                NavigationBarItem(selected = dest?.hasRoute(HomeRoute::class) == true, onClick = { nav.navigateTab(HomeRoute) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("홈") })
                NavigationBarItem(selected = dest?.hasRoute(BrowseRoute::class) == true, onClick = { browseTab = null; nav.navigateTab(BrowseRoute) }, icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) }, label = { Text("단어장") })
                NavigationBarItem(selected = dest?.hasRoute(StatsRoute::class) == true, onClick = { nav.navigateTab(StatsRoute) }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("통계") })
                NavigationBarItem(selected = dest?.hasRoute(SettingsRoute::class) == true, onClick = { nav.navigateTab(SettingsRoute) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("설정") })
            }
        }) { padding ->
            NavHost(nav, startDestination = if (done) HomeRoute else OnboardingRoute, modifier = Modifier.padding(padding)) {
                composable<OnboardingRoute> { OnboardingScreen(onDone = { nav.navigate(HomeRoute) { popUpTo(OnboardingRoute) { inclusive = true } } }) }
                composable<HomeRoute> {
                    HomeScreen(
                        onStartSession = { extra -> nav.navigate(SessionRoute(extra)) },
                        onOpenCore = { browseTab = BrowseTab.Core; nav.navigateTab(BrowseRoute) },
                        onOpenLevel = { lvl -> browseTab = BrowseTab.OfLevel(lvl); nav.navigateTab(BrowseRoute) },
                    )
                }
                composable<BrowseRoute> { TabBack(nav); BrowseScreen(initialTab = browseTab, onOpenUnit = { id, title -> nav.navigate(UnitRoute(id, title)) }, onOpenWord = { nav.navigate(WordRoute(it)) }) }
                composable<StatsRoute> { TabBack(nav); StatsScreen(onWord = { nav.navigate(WordRoute(it)) }) }
                composable<SettingsRoute> { TabBack(nav); SettingsScreen() }
                composable<SessionRoute> { entry -> SessionScreen(extraNew = entry.toRoute<SessionRoute>().extraNew, onExit = { nav.popBackStack() }, onOpenWord = { nav.navigate(WordRoute(it)) }) }
                composable<UnitRoute> { entry -> val r = entry.toRoute<UnitRoute>(); UnitScreen(r.unitId, r.title, onBack = { nav.popBackStack() }, onOpenWord = { nav.navigate(WordRoute(it)) }) }
                composable<WordRoute> { entry -> WordDetailScreen(entry.toRoute<WordRoute>().wordId, onBack = { nav.popBackStack() }, onPractice = { nav.navigate(PracticeRoute(it)) }) }
                composable<PracticeRoute> { entry -> PracticeScreen(entry.toRoute<PracticeRoute>().wordId, onExit = { nav.popBackStack() }) }
            }
        }
    }
}

/**
 * 하단 탭 이동. 백스택에 탭이 쌓이지 않도록 매번 그래프 루트까지 비운다.
 *
 * 대상을 HomeRoute 나 startDestination 으로 잡으면 안 된다. 둘 다 "스택에 그것이
 * 남아 있을 때만" 동작하는데, 홈에서 단어장으로 가는 순간 홈이 스택에서 빠지므로
 * 그 뒤의 탭 이동은 아무것도 비우지 못하고 계속 쌓인다.
 * (실제로 [홈] → [단어장] → [단어장, 설정] → [단어장, 설정, 홈] 으로 늘어났다.)
 *
 * graph.id 는 항상 스택 맨 아래에 있으므로 어느 탭에서 눌러도 확실히 비워진다.
 */
private fun androidx.navigation.NavHostController.navigateTab(route: Any) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

/**
 * 홈이 아닌 탭에서 백 키를 누르면 홈으로 보낸다. 홈에서 한 번 더 누르면 앱이 닫힌다.
 *
 * NavHost **안쪽**(각 탭 화면 안)에 둬야 한다. 바깥에 두면 NavHost 가 등록한
 * 자체 백 처리기가 먼저 가져가 홈이 아니라 직전에 들른 탭으로 돌아간다.
 */
@Composable
private fun TabBack(nav: androidx.navigation.NavHostController) {
    BackHandler { nav.navigateTab(HomeRoute) }
}
