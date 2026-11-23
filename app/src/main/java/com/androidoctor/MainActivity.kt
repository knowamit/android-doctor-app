package com.androidoctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.androidoctor.ui.navigation.DoctorNavGraph
import com.androidoctor.ui.theme.AndroidDoctorTheme
import com.androidoctor.ads.ConsentManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var consentManager: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consentManager.gather(this)
        setContent {
            AndroidDoctorTheme {
                val navController = rememberNavController()
                DoctorNavGraph(navController = navController)
            }
        }
    }
}
