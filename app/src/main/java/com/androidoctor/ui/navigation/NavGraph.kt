package com.androidoctor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.androidoctor.ui.screens.battery.BatteryScreen
import com.androidoctor.ui.screens.benchmark.BenchmarkScreen
import com.androidoctor.ui.screens.diagnose.DiagnoseScreen
import com.androidoctor.ui.screens.fix.FixScreen
import com.androidoctor.ui.screens.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val DIAGNOSE = "diagnose"
    const val FIX = "fix"
    const val BENCHMARK = "benchmark"
    const val BATTERY = "battery"
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
            )
        }
        composable(Routes.DIAGNOSE) {
            DiagnoseScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FIX) {
            FixScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BENCHMARK) {
            BenchmarkScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BATTERY) {
            BatteryScreen(onBack = { navController.popBackStack() })
        }
    }
}
