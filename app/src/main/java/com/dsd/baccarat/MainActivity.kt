package com.dsd.baccarat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                val bppcItems = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value
                val counter = viewModel.bpCounterStateFlow.collectAsStateWithLifecycle().value
                Scaffold { innerPadding ->
                    Row(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxHeight()
                            .fillMaxWidth()
                    ) {
                        BppcTableAndChart(bppcItems, counter)
                        InputButtons(viewModel)
                    }
                }
            }
        }
    }
}
