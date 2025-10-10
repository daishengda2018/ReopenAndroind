package com.dsd.baccarat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import com.dsd.baccarat.ui.page.HorizontalListDemo
import com.dsd.baccarat.ui.theme.ReopenAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReopenAndroidTheme {
                Scaffold { innerPadding ->
                    HorizontalListDemo(innerPadding)
                }
            }

        }
    }

}
