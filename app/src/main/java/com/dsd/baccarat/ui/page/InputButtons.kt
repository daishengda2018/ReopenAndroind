package com.dsd.baccarat.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.InputViewModel

@Composable
fun InputButtons(viewModel: InputViewModel) {
    @Composable
    fun RowScope.DefaultModifier(): Modifier = Modifier
        .padding(3.dp)
        .height(40.dp)
        .weight(1f)

    Column(
        modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Row {
            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel.openB() }
            ) {
                Text(text = "开 B")
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel.openP() }
            ) {
                Text(text = "开 P")
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel.openP() }
            ) {
                Text(text = "撤销")
            }
        }

        Row {
            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel.openB() }
            ) {
                Text(text = "押 B", fontSize = 14.sp)
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel.openP() }
            ) {
                Text(text = "押 P")
            }

            Button(
                modifier = DefaultModifier(),
                onClick = { viewModel.openP() }
            ) {
                Text(text = "撤销")
            }
        }
    }
}
