package com.dsd.baccarat.ui.game.components.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.TimerStatus
import java.util.Locale

/**
 * 当前时间显示组件
 *
 * @param dateStr 日期字符串
 * @param elapsedTime 已经过的时间（秒）
 * @param timerStatus 计时器状态
 * @param showReminder 是否显示休息提醒
 * @param onDismissReminder 关闭提醒回调
 */
@Composable
fun CurrentTimeDisplay(
    dateStr: String,
    elapsedTime: Int,
    timerStatus: TimerStatus,
    showReminder: Boolean,
    onDismissReminder: () -> Unit
) {
    val textStyle = remember {
        TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

    val minutes = elapsedTime / 60
    val seconds = elapsedTime % 60

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(ITEM_SIZE)
    ) {
        Text(
            text = dateStr,
            style = textStyle,
            color = Color.Black
        )

        Spacer(Modifier.width(ITEM_SIZE))

        Text(
            text = String.format(Locale.getDefault(), "计时: %02d:%02d", minutes, seconds),
            style = textStyle,
            color = Color.Black
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = when (timerStatus) {
                TimerStatus.Idle -> "未开始"
                TimerStatus.Running -> "运行中"
                TimerStatus.Paused -> "已暂停"
                TimerStatus.Finished -> "已完成"
            },
            style = textStyle.copy(fontSize = 12.sp),
            color = Color.Gray
        )
    }

    if (showReminder) {
        AlertDialog(
            onDismissRequest = onDismissReminder,
            confirmButton = {
                Button(onClick = onDismissReminder) {
                    Text("确定")
                }
            },
            text = { Text("该休息了") }
        )
    }
}
