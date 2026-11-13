package com.gifapp.car.ui.files

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gifapp.car.data.RemoteFile
import com.gifapp.car.ui.DownloadUi
import com.gifapp.car.ui.MainViewModel
import com.gifapp.car.ui.components.DownloadButton
import com.gifapp.car.ui.components.EmptyState
import com.gifapp.car.ui.components.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val state by vm.files.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val storageReady by vm.storageReady.collectAsStateWithLifecycle()
    val localFiles by vm.localFiles.collectAsStateWithLifecycle()
    val localNames = remember(localFiles) { localFiles.map { it.name }.toSet() }
    val gridState = rememberLazyGridState()

    // 回到本页（切回标签 / 应用回前台）时自动刷新一次
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.items.isNotEmpty() && last >= state.items.size - 6 &&
                !state.loading && !state.endReached
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) vm.loadFiles() }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("云端文件", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "共 ${state.total} 个 · 下载到 /sdcard/wear_gif",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { vm.refresh() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.Refresh, "刷新", modifier = Modifier.size(32.dp))
            }
        }

        if (!storageReady) {
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        "尚未授予存储权限，无法下载。请前往「设置」开启。",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.weight(1f)
        ) {
            when {
                state.items.isEmpty() && state.loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center).size(56.dp))
                }
                state.items.isEmpty() && state.error != null -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { vm.refresh() }) { Text("重试", style = MaterialTheme.typography.labelLarge) }
                    }
                }
                state.items.isEmpty() -> {
                    EmptyState(Icons.Rounded.CloudOff, "暂无文件", "在网页端上传后，这里会显示")
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 240.dp),
                        state = gridState,
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items, key = { it.id }) { file ->
                            val downloaded = localNames.contains(file.name) ||
                                downloads[file.id] is DownloadUi.Success
                            FileCard(
                                file = file,
                                download = downloads[file.id],
                                downloaded = downloaded,
                                onOpen = { onOpen(file.id) },
                                onDownload = { vm.download(file) }
                            )
                        }
                        if (state.loading && !state.refreshing) {
                            item {
                                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileCard(
    file: RemoteFile,
    download: DownloadUi?,
    downloaded: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF0A0C10))
            ) {
                AsyncImage(
                    model = file.url,
                    contentDescription = file.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                ) {
                    Text(
                        text = if (file.isGif) "GIF" else "MP4",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                if (downloaded) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "已下载",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(30.dp)
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
                    text = formatSize(file.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(14.dp))
                DownloadButton(state = download, downloaded = downloaded, onClick = onDownload)
            }
        }
    }
}