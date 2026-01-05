package com.dsd.baccarat

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.dsd.baccarat.ui.theme.ReopenAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity 基类
 *
 * 封装了所有 Activity 的共同逻辑：
 * - 横屏显示
 * - Edge-to-Edge 显示
 * - 主题设置
 *
 * 子类只需实现 [Content] 方法提供具体内容
 */
abstract class BaseActivity : ComponentActivity() {

    /**
     * 子类实现此方法提供 Activity 的具体内容
     */
    @Composable
    protected abstract fun Content()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 启用 Edge-to-Edge
        enableEdgeToEdge()

        // 设置内容
        setContent {
            ReopenAndroidTheme {
                Content()
            }
        }
    }
}
