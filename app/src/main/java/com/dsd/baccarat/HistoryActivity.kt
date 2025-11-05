package com.dsd.baccarat

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dsd.baccarat.model.DefaultViewModel.Companion.KEY_GAME_ID
import com.dsd.baccarat.model.HistoryViewModel
import com.dsd.baccarat.ui.compose.Screen
import com.dsd.baccarat.ui.theme.ReopenAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Create by Shengda 2025/11/4 20:05
 */
@AndroidEntryPoint
class HistoryActivity : ComponentActivity() {
    private val viewModel: HistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val gameID = intent.getStringExtra(KEY_GAME_ID) ?: ""
        viewModel.loadHistory(gameID)

        enableEdgeToEdge()
        setContent {
            ReopenAndroidTheme {
                Screen(viewModel, true)
            }
        }
    }
}