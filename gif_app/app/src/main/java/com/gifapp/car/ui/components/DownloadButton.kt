package com.gifapp.car.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gifapp.car.ui.DownloadUi

@Composable
fun DownloadButton(
    state: DownloadUi?,
    downloaded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val running = state is DownloadUi.Running
    Column(modifier = modifier) {
        Button(
            onClick = onClick,
            enabled = !running,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (downloaded) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.primary,
                contentColor = if (downloaded) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (running) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = LocalContentColor.current
                )
                Spacer(Modifier.width(10.dp))
                Text("${(state as DownloadUi.Running).percent}%", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(
                    imageVector = if (downloaded) Icons.Rounded.Refresh else Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (downloaded) "重新下载" else "下载",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        if (state is DownloadUi.Failed) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}