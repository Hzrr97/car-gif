package com.gifapp.car.ui.detail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gifapp.car.ui.DownloadUi
import com.gifapp.car.ui.MainViewModel
import com.gifapp.car.ui.components.DownloadButton
import com.gifapp.car.ui.components.VideoPlayer
import com.gifapp.car.ui.components.formatSize

@Composable
fun DetailScreen(vm: MainViewModel, fileId: Long, onBack: () -> Unit) {
    val state by vm.files.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val localFiles by vm.localFiles.collectAsStateWithLifecycle()
    val file = state.items.firstOrNull { it.id == fileId }

    if (file == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(56.dp))
        }
        return
    }

    val downloaded = localFiles.any { it.name == file.name } || downloads[file.id] is DownloadUi.Success

    Row(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)
    ) {
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A0C10)),
            contentAlignment = Alignment.Center
        ) {
            if (file.isGif) {
                AsyncImage(
                    model = file.url,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                VideoPlayer(uri = Uri.parse(file.url), modifier = Modifier.fillMaxSize())
            }
        }

        Spacer(Modifier.width(24.dp))

        Column(Modifier.width(360.dp).fillMaxHeight()) {
            IconButton(onClick = onBack, modifier = Modifier.size(56.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = file.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(20.dp))
            InfoRow("类型", if (file.isGif) "GIF 动图" else "MP4 视频")
            InfoRow("大小", formatSize(file.size))
            InfoRow("上传时间", file.created_at.replace("T", " ").removeSuffix("Z"))
            Spacer(Modifier.height(28.dp))
            DownloadButton(
                state = downloads[file.id],
                downloaded = downloaded,
                onClick = { vm.download(file) }
            )
            if (downloaded) {
                Text(
                    text = "已保存到 /sdcard/wear_gif/${file.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}