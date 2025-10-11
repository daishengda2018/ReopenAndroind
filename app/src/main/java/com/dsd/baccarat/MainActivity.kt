package com.dsd.baccarat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dsd.baccarat.data.InputViewModel
import com.dsd.baccarat.ui.page.BppcTable
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
                        BppcTable(innerPadding, viewModel)
                        Button(
                            modifier = Modifier
                                .padding(16.dp) // 边距
                                .size(width = 200.dp, height = 50.dp), // 宽高
                            onClick = { viewModel.openB() }
                        )
                        {
                            Text(text = "Open B")
                        }

                        Button(
                            modifier = Modifier
                                .padding(16.dp) // 边距
                                .size(width = 200.dp, height = 50.dp), // 宽高
                            onClick = { viewModel.openP() }
                        )
                        {
                            Text(text = "Open P")
                        }
                    }
                }
            }
        }
    }
}
