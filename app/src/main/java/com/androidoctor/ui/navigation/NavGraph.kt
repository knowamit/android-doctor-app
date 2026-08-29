package com.androidoctor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.androidoctor.ui.screens.battery.BatteryScreen
import com.androidoctor.ui.screens.benchmark.BenchmarkScreen
import com.androidoctor.ui.screens.bloatware.BloatwareScreen
import com.androidoctor.ui.screens.diagnose.DiagnoseScreen
import com.androidoctor.ui.screens.fix.FixScreen
import com.androidoctor.ui.screens.fixhistory.FixHistoryScreen
import com.androidoctor.ui.screens.home.HomeScreen
import com.androidoctor.ui.screens.shizuku.ShizukuSetupScreen
import com.androidoctor.ui.screens.trends.TrendsScreen
import com.androidoctor.ui.screens.usage.HeavyAppsScreen

object Routes {
    const val HOME = "home"
    const val DIAGNOSE = "diagnose"
    const val FIX = "fix"
    const val BENCHMARK = "benchmark"
    const val BATTERY = "battery"
    const val TRENDS = "trends"
    const val BLOATWARE = "bloatware"
    const val SHIZUKU = "shizuku"
    const val HEAVY_APPS = "heavy_apps"
    const val FIX_HISTORY = "fix_history"
}

@Composable
fun DoctorNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onDiagnose = { navController.navigate(Routes.DIAGNOSE) },
                onFix = { navController.navigate(Routes.FIX) },
                onBenchmark = { navController.navigate(Routes.BENCHMARK) },
                onBattery = { navController.navigate(Routes.BATTERY) },
                onTrends = { navController.navigate(Routes.TRENDS) },
                onBloatware = { navController.navigate(Routes.BLOATWARE) },
                onHeavyApps = { navController.navigate(Routes.HEAVY_APPS) },
            )
        }
        composable(Routes.DIAGNOSE) {
            DiagnoseScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FIX) {
            FixScreen(
                onBack = { navController.popBackStack() },
                onShizukuSetup = { navController.navigate(Routes.SHIZUKU) },
                onHistory = { navController.navigate(Routes.FIX_HISTORY) },
            )
        }
        composable(Routes.FIX_HISTORY) {
            FixHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BENCHMARK) {
            BenchmarkScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BATTERY) {
            BatteryScreen(
                onBack = { navController.popBackStack() },
                onShizukuSetup = { navController.navigate(Routes.SHIZUKU) },
            )
        }
        composable(Routes.TRENDS) {
            TrendsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BLOATWARE) {
            BloatwareScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SHIZUKU) {
            ShizukuSetupScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HEAVY_APPS) {
            HeavyAppsScreen(onBack = { navController.popBackStack() })
        }
    }
}
