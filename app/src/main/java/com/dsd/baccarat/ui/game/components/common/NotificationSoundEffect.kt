package com.dsd.baccarat.ui.game.components.common

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * 提示音播放组件
 *
 * @param soundEffectFlow 声音效果事件流
 */
@Composable
fun NotificationSoundEffect(
    soundEffectFlow: SharedFlow<com.dsd.baccarat.ui.game.state.GameSideEffect>
) {
    val toneGenerator = remember {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    LaunchedEffect(soundEffectFlow) {
        soundEffectFlow.collectLatest { sideEffect ->
            if (sideEffect is com.dsd.baccarat.ui.game.state.GameSideEffect.PlaySound) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 10000)
            }
        }
    }
}
