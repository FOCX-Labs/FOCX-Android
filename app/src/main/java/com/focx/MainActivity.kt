package com.focx

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.focx.presentation.navigation.FocxNavigation
import com.focx.presentation.ui.theme.FocxTheme
import com.focx.presentation.viewmodel.MainViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))

        val activityResultSender = ActivityResultSender(this)

        // Try to load existing wallet connection
        mainViewModel.loadConnection()

        setContent {
            FocxTheme(darkTheme = true) {
                FocxNavigation(activityResultSender = activityResultSender)
            }
        }
    }
}
