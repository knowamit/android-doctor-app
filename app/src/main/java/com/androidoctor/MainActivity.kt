package com.androidoctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.androidoctor.ui.navigation.DoctorNavGraph
import com.androidoctor.ui.theme.AndroidDoctorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidDoctorTheme {
                val navController = rememberNavController()
                DoctorNavGraph(navController = navController)
            }
        }
    }
}
