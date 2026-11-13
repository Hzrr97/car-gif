package com.gifapp.car.ui.local

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gifapp.car.ui.MainViewModel
import com.gifapp.car.ui.components.EmptyState
import com.gifapp.car.ui.components.VideoPlayer
import com.gifapp.car.ui.components.formatSize
import java.io.File

@Composable
fun LocalScreen(vm: MainViewModel) {
    val localFiles by vm.localFiles.collectAsStateWithLifecycle()
    val storageReady by vm.storageReady.collectAsStateWithLifecycle()
    val selectedGif by vm.selectedGif.collectAsStateWithLifecycle()
    var preview by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) { vm.loadLocalFiles() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("本地下载", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "/sdcard/wear_gif · ${localFiles.size} 个文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { vm.loadLocalFiles() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Refresh, "刷新", modifier = Modifier.size(32.dp))
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                !storageReady -> EmptyState(
                    Icons.Rounded.FolderOff, "无法访问存储", "请在「设置」中授予存储权限"
                )
                localFiles.isEmpty() -> EmptyState(
                    Icons.Rounded.DownloadDone, "还没有下载", "从云端文件下载后会出现在这里"
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 240.dp),
                    contentPadding = PaddingValues(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(localFiles, key = { it.absolutePath }) { f ->
                        LocalCard(
                            file = f,
                            selected = f.absolutePath == selectedGif,
                            onOpen = { preview = f },
                            onUse = { vm.useFile(f) },
                            onDelete = { vm.deleteLocal(f) }
                        )
                    }
                }
            }
        }
    }

    preview?.let { f ->
        LocalPreviewDialog(
            file = f,
            selected = f.absolutePath == selectedGif,
            onDismiss = { preview = null },
            onUse = { vm.useFile(f) },
            onDelete = { vm.deleteLocal(f); preview = null }
        )
    }
}

@Composable
private fun LocalCard(
    file: File,
    selected: Boolean,
    onOpen: () -> Unit,
    onUse: () -> Unit,
    onDelete: () -> Unit
) {
    val isGif = file.extension.equals("gif", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF0A0C10))) {
                AsyncImage(
                    model = file,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!isGif) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.Center).size(56.dp)
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                ) {
                    Text(
                        text = if (isGif) "GIF" else "MP4",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatSize(file.length()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onUse,
                        enabled = !selected,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            if (selected) "使用中" else "使用",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onOpen, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Rounded.PlayArrow, "播放", modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Rounded.Delete, "删除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalPreviewDialog(
    file: File,
    selected: Boolean,
    onDismiss: () -> Unit,
    onUse: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize(0.92f)
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(56.dp)) {
                        Icon(
                            Icons.Rounded.Delete, "删除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.Close, "关闭", modifier = Modifier.size(28.dp))
                    }
                }
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0A0C10)),
                    contentAlignment = Alignment.Center
                ) {
                    if (file.extension.equals("gif", ignoreCase = true)) {
                        AsyncImage(
                            model = file,
                            contentDescription = file.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VideoPlayer(uri = Uri.fromFile(file), modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onUse,
                    enabled = !selected,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    if (selected) {
                        Icon(Icons.Rounded.CheckCircle, null, Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (selected) "使用中" else "使用",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}