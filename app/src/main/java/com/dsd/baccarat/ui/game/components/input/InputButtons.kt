package com.dsd.baccarat.ui.game.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.InputType
import com.dsd.baccarat.data.TimerStatus
import com.dsd.baccarat.data.room.entity.GameSessionEntity
import com.dsd.baccarat.ui.game.state.GameUiEvent
import com.dsd.baccarat.ui.game.state.GameUiState

/**
 * 输入按钮组组件
 *
 * @param uiState UI 状态
 * @param onEvent 事件处理回调
 * @param modifier 修饰符
 */
@Composable
fun InputButtons(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            val currentBetType = uiState.currentBetType
            val isInputB = currentBetType == InputType.B
            val isInputP = currentBetType == InputType.P

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.isBetBEnabled,
                onClick = { onEvent(GameUiEvent.BetB) }
            ) {
                Text(text = if (isInputB) "押 B 中" else "押 B")
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.isBetPEnabled,
                onClick = { onEvent(GameUiEvent.BetP) }
            ) {
                Text(text = if (isInputP) "押 P 中" else "押 P")
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.isInputEnabled,
                onClick = { onEvent(GameUiEvent.RemoveLastBet) }
            ) {
                Text(text = "撤销")
            }
        }

        Column(Modifier.weight(1f)) {
            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.isInputEnabled,
                onClick = { onEvent(GameUiEvent.OpenB) }
            ) {
                Text(text = "开 B")
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.isInputEnabled,
                onClick = { onEvent(GameUiEvent.OpenP) }
            ) {
                Text(text = "开 P")
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.isInputEnabled,
                onClick = { onEvent(GameUiEvent.RemoveLastOpen) }
            ) {
                Text(text = "撤销")
            }
        }

        Spacer(Modifier.weight(0.5f))

        Column(Modifier.weight(1f)) {
            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.timerState.canStart && !uiState.isOnlyShowNewGame,
                onClick = { onEvent(GameUiEvent.StartTimer) }
            ) {
                Text(
                    text = when (uiState.timerState.status) {
                        TimerStatus.Running -> "正在运行"
                        TimerStatus.Paused -> "重新开始"
                        else -> "开始记时"
                    }
                )
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.timerState.canPauseOrResume && !uiState.isOnlyShowNewGame,
                onClick = { onEvent(GameUiEvent.PauseOrResumeTimer) }
            ) {
                Text(
                    text = when (uiState.timerState.status) {
                        TimerStatus.Running -> "暂停记时"
                        TimerStatus.Paused -> "继续记时"
                        else -> "未开始"
                    }
                )
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = uiState.timerState.canStop && !uiState.isOnlyShowNewGame,
                onClick = { onEvent(GameUiEvent.StopTimer) }
            ) {
                Text(text = "结束记时")
            }
        }

        Column(Modifier.weight(1f)) {
            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                onClick = { onEvent(GameUiEvent.ShowDateSelectDialog) }
            ) {
                Text(text = "历史")
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                enabled = !uiState.isOnlyShowNewGame,
                onClick = { onEvent(GameUiEvent.SaveGame) }
            ) {
                Text(text = "保存")
            }

            Button(
                modifier = Modifier.padding(3.dp).fillMaxWidth().weight(1f),
                onClick = { onEvent(GameUiEvent.NewGame) }
            ) {
                Text(text = "新牌")
            }
        }
    }
}

/**
 * 日期选择对话框
 */
@Composable
fun DateSelectDialog(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit
) {
    if (!uiState.dateSelectionState.isDialogVisible) return

    AlertDialog(
        onDismissRequest = { onEvent(GameUiEvent.DismissDialog) },
        title = { Text("选择日期") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.dateSelectionState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.dateSelectionState.availableDates.isEmpty()) {
                    Text(
                        text = "暂无数据",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.dateSelectionState.availableDates) { date ->
                            val isSelected = (date == uiState.dateSelectionState.selectedDate)
                            GameSessionItem(
                                gameSession = date,
                                isSelected = isSelected,
                                onClick = { onEvent(GameUiEvent.SelectDate(date)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(GameUiEvent.ConfirmDateSelection) }) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GameUiEvent.DismissDialog) }) {
                Text("取消")
            }
        }
    )
}

/**
 * 游戏会话项
 */
@Composable
private fun GameSessionItem(
    gameSession: GameSessionEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val detailDateFormatter = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd hh:mm", java.util.Locale.CHINESE)
    }

    Text(
        text = "${detailDateFormatter.format(gameSession.startTime)} ~ ${detailDateFormatter.format(gameSession.endTime)}",
        fontSize = 16.sp,
        color = if (isSelected) Color.Blue else Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .width(100.dp)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    )
}
