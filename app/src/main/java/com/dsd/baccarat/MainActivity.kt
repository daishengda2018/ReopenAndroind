package com.dsd.baccarat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsd.baccarat.data.BppcDisplayItem
import com.dsd.baccarat.data.InputViewModel
import com.dsd.baccarat.ui.page.UI
import com.dsd.baccarat.ui.theme.ReopenAndroidTheme

class MainActivity : ComponentActivity() {
    private val viewModel: InputViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val listState = rememberLazyListState()
            val items = viewModel.bppcTableStateFlow.collectAsStateWithLifecycle().value

            // 【优化3】优化自动滚动逻辑
            LaunchedEffect(items.size) { // 仅在列表大小变化时触发
                val lastRealIndex = items.indexOfLast { it is BppcDisplayItem.Real }
                if (lastRealIndex != -1) {
                    // 使用 animateScrollToItem 获得更平滑的滚动动画效果
                    listState.animateScrollToItem(items.lastIndex)
                }
            }

            ReopenAndroidTheme {
                UI(viewModel, listState)
            }
        }
    }
}
