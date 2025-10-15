package com.dsd.baccarat.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsd.baccarat.data.InputViewModel

@Composable
fun InputButtons(viewModel: InputViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.5f)
    ) {
        Column {
            Button(
                modifier = Modifier
                    .padding(16.dp) // 边距
                    .size(width = 100.dp, height = 50.dp), // 宽高

                onClick = { viewModel.openB() }
            ) {
                Text(text = "押 B", fontSize = 14.sp)
            }

            Button(
                modifier = Modifier
                    .padding(16.dp) // 边距
                    .size(width = 100.dp, height = 50.dp), // 宽高
                onClick = { viewModel.openP() }
            ) {
                Text(text = "押 P")
            }

            Button(
                modifier = Modifier
                    .padding(16.dp) // 边距
                    .size(width = 100.dp, height = 50.dp), // 宽高
                onClick = { viewModel.openP() }
            ) {
                Text(text = "撤销")
            }
        }
        Column {
            Button(
                modifier = Modifier
                    .padding(16.dp) // 边距
                    .size(width = 100.dp, height = 50.dp), // 宽高

                onClick = { viewModel.openB() }
            ) {
                Text(text = "Open B")
            }

            Button(
                modifier = Modifier
                    .padding(16.dp) // 边距
                    .size(width = 100.dp, height = 50.dp), // 宽高
                onClick = { viewModel.openP() }
            ) {
                Text(text = "Open P")
            }

            Button(
                modifier = Modifier
                    .padding(16.dp) // 边距
                    .size(width = 100.dp, height = 50.dp), // 宽高
                onClick = { viewModel.openP() }
            ) {
                Text(text = "撤销")
            }
        }
    }
}
