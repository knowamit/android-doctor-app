package com.androidoctor.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.androidoctor.ads.ConsentViewModel
import com.androidoctor.ads.findActivity
import com.androidoctor.ui.screens.battery.BatteryScreen
import com.androidoctor.ui.screens.benchmark.BenchmarkScreen
import com.androidoctor.ui.screens.debloat.DebloatScreen
import com.androidoctor.ui.screens.diagnose.DiagnoseScreen
import com.androidoctor.ui.screens.fix.FixScreen
import com.androidoctor.ui.screens.fixhistory.FixHistoryScreen
import com.androidoctor.ui.screens.home.HomeScreen
import com.androidoctor.ui.screens.onboarding.OnboardingScreen
import com.androidoctor.ui.screens.onboarding.RootViewModel
import com.androidoctor.ui.screens.performance.PerformanceScreen
import com.androidoctor.ui.screens.result.OptimizeResultScreen
import com.androidoctor.ui.screens.result.ResultViewModel
import com.androidoctor.ui.screens.shizuku.ShizukuSetupScreen
import com.androidoctor.ui.screens.storage.StorageScreen
import com.androidoctor.ui.screens.tools.ToolItem
import com.androidoctor.ui.screens.tools.ToolListScreen
import com.androidoctor.ui.screens.trends.TrendsScreen
import com.androidoctor.ui.screens.usage.HeavyAppsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TOOLS = "tools"
    const val SHIZUKU = "shizuku"
    const val MORE = "more"
    const val RESULT = "result"
    const val STORAGE = "storage"
    const val PERFORMANCE = "performance"
    const val DIAGNOSE = "diagnose"
    const val FIX = "fix"
    const val BENCHMARK = "benchmark"
    const val BATTERY = "battery"
    const val TRENDS = "trends"
    const val BLOATWARE = "bloatware"
    const val HEAVY_APPS = "heavy_apps"
    const val FIX_HISTORY = "fix_history"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.TOOLS, "Tools", Icons.Filled.Build),
    Tab(Routes.SHIZUKU, "Shizuku", Icons.Filled.Usb),
    Tab(Routes.MORE, "More", Icons.Filled.MoreHoriz),
)

@Composable
fun DoctorNavGraph(navController: NavHostController) {
    val root: RootViewModel = hiltViewModel()
    val done by root.onboardingDone.collectAsState()
    // Decide the start destination once. If it followed `done`, finishing onboarding would
    // rebuild the graph and reset the back stack.
    var start by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(done) {
        val d = done
        if (start == null && d != null) start = if (d) Routes.HOME else Routes.ONBOARDING
    }
    val startDestination = start ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBar = tabs.any { it.route == current }

    fun go(route: String) = navController.navigate(route)
    fun back() { navController.popBackStack() }
    fun tab(route: String) = navController.navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = current == t.route,
                            onClick = { tab(t.route) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onFinish = {
                    root.finish()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOptimized = { go(Routes.RESULT) },
                    onStorage = { go(Routes.STORAGE) },
                    onPerformance = { go(Routes.PERFORMANCE) },
                    onApps = { go(Routes.BLOATWARE) },
                    onBattery = { go(Routes.BATTERY) },
                    onAdvanced = { tab(Routes.SHIZUKU) },
                    onSettings = { tab(Routes.MORE) },
                )
            }
            composable(Routes.TOOLS) {
                ToolListScreen(
                    "Tools",
                    listOf(
                        ToolItem("Full Diagnosis", "Check every part of your phone", Icons.Filled.BugReport) { go(Routes.DIAGNOSE) },
                        ToolItem("Fix and Optimize", "Pick exactly what to change", Icons.Filled.Tune) { go(Routes.FIX) },
                        ToolItem("Benchmark", "Measure CPU and storage speed", Icons.Filled.Speed) { go(Routes.BENCHMARK) },
                        ToolItem("Heavy Apps", "Who uses the most resources", Icons.Filled.Memory) { go(Routes.HEAVY_APPS) },
                        ToolItem("Health Trends", "Score over time", Icons.Filled.AutoGraph) { go(Routes.TRENDS) },
                    ),
                )
            }
            composable(Routes.SHIZUKU) { ShizukuSetupScreen(onBack = { tab(Routes.HOME) }) }
            composable(Routes.MORE) {
                val consent = hiltViewModel<ConsentViewModel>().consent
                val privacyRequired by consent.privacyOptionsRequired.collectAsState()
                val ctx = LocalContext.current
                ToolListScreen(
                    "More",
                    buildList {
                        add(ToolItem("Fix History", "Review and undo changes", Icons.Filled.History) { go(Routes.FIX_HISTORY) })
                        add(ToolItem("Battery", "Usage and health", Icons.Filled.BatteryChargingFull) { go(Routes.BATTERY) })
                        if (privacyRequired) {
                            add(ToolItem("Privacy options", "Change your ad consent choices", Icons.Filled.PrivacyTip) {
                                ctx.findActivity()?.let { consent.showPrivacyOptions(it) }
                            })
                        }
                    },
                )
            }
            composable(Routes.RESULT) {
                val vm: ResultViewModel = hiltViewModel()
                val r = vm.result
                if (r == null) {
                    LaunchedEffect(Unit) { navController.popBackStack(Routes.HOME, false) }
                } else {
                    val ctx = LocalContext.current
                    val toHome = { navController.popBackStack(Routes.HOME, false); Unit }
                    OptimizeResultScreen(
                        result = r,
                        onDone = toHome,
                        // Only this button can show an ad: system back and the top bar never do.
                        onBackToHome = { vm.showAdThen(ctx.findActivity(), toHome) },
                        onHistory = { go(Routes.FIX_HISTORY) },
                    )
                }
            }
            composable(Routes.STORAGE) { StorageScreen(onBack = ::back, onNeedShizuku = { tab(Routes.SHIZUKU) }) }
            composable(Routes.PERFORMANCE) { PerformanceScreen(onBack = ::back, onNeedShizuku = { tab(Routes.SHIZUKU) }) }
            composable(Routes.DIAGNOSE) { DiagnoseScreen(::back) }
            composable(Routes.FIX) { FixScreen(::back, { tab(Routes.SHIZUKU) }, { go(Routes.FIX_HISTORY) }) }
            composable(Routes.FIX_HISTORY) { FixHistoryScreen(::back) }
            composable(Routes.BENCHMARK) { BenchmarkScreen(::back) }
            composable(Routes.BATTERY) { BatteryScreen(onBack = ::back, onShizukuSetup = { tab(Routes.SHIZUKU) }) }
            composable(Routes.TRENDS) { TrendsScreen(::back) }
            composable(Routes.BLOATWARE) { DebloatScreen(onBack = ::back, onNeedShizuku = { tab(Routes.SHIZUKU) }) }
            composable(Routes.HEAVY_APPS) { HeavyAppsScreen(::back) }
        }
    }
}
