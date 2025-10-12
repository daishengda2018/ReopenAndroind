package com.dsd.baccarat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.dsd.baccarat.data.InputViewModel
import com.dsd.baccarat.ui.page.BppcTableAndChart
import com.dsd.baccarat.ui.page.InputButtons
import com.dsd.baccarat.ui.theme.ReopenAndroidTheme

class MainActivity : ComponentActivity() {
    private val viewModel: InputViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReopenAndroidTheme {
                Scaffold { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        BppcTableAndChart(innerPadding, viewModel)
                        InputButtons(innerPadding, viewModel)
                    }
                }
            }
        }
    }
}
