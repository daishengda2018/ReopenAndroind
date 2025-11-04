package com.dsd.baccarat

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import java.time.LocalDate

/**
 * Create by Shengda 2025/11/4 20:05
 */
class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        intent.get("date", LocalDate::class)
    }
}