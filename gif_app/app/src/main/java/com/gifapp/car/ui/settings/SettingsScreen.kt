package com.gifapp.car.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gifapp.car.download.StorageManager
import com.gifapp.car.integration.SelectedGifStore
import com.gifapp.car.ui.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val session by vm.session.collectAsStateWithLifecycle()
    val storageReady by vm.storageReady.collectAsStateWithLifecycle()
    val selectedGif by vm.selectedGif.collectAsStateWithLifecycle()
    val writeSettingsReady by vm.writeSettingsReady.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var server by rememberSaveable { mutableStateOf(session.serverUrl) }
    var apiKey by rememberSaveable { mutableStateOf(session.apiKey) }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.refreshStorage() }
    val writePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refreshStorage() }
    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.refreshIntegration() }

    fun requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = StorageManager.allFilesAccessIntent(context)
            try {
                if (intent != null) allFilesLauncher.launch(intent)
                else allFilesLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            } catch (e: Exception) {
                allFilesLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        SettingCard(title = "服务器连接") {
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("服务器地址") },
                placeholder = { Text("http://你的VPS地址:9099") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("访问密钥") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { vm.updateConnection(server, apiKey) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Rounded.Save, null, Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("保存并连接", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                "密钥需与后端 .env 的 CLIENT_API_KEY 一致。车机端无需登录，打开即自动加载全部文件。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingCard(title = "存储") {
            InfoRow("下载目录", "/sdcard/wear_gif")
            InfoRow("权限状态", if (storageReady) "已就绪" else "未授权")
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    requestStorage()
                    vm.refreshStorage()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (storageReady) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (storageReady) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(
                    if (storageReady) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                    null, Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (storageReady) "重新检查 / 创建目录" else "授予存储权限",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (!storageReady) {
                Text(
                    "Android 11+ 需在系统设置中为本应用开启「所有文件访问」权限。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingCard(title = "应用联动") {
            InfoRow("属性键", SelectedGifStore.KEY)
            InfoRow("当前值", selectedGif.ifBlank { "未选择" })
            InfoRow("授权状态", if (writeSettingsReady) "已授权" else "未授权")
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    try {
                        writeSettingsLauncher.launch(SelectedGifStore.writeSettingsIntent(context))
                    } catch (e: Exception) {
                        writeSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                    }
                    vm.refreshIntegration()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (writeSettingsReady) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.primary,
                    contentColor = if (writeSettingsReady) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(
                    if (writeSettingsReady) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                    null, Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (writeSettingsReady) "重新检查授权" else "授予「修改系统设置」",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(
                "在「本地下载」点击「使用」后，会把该 GIF 的绝对路径写入系统属性 wear_gif_selected。" +
                    "其他应用用 ContentObserver 监听 Settings.System.getUriFor(\"wear_gif_selected\") 即可实时读取并加载。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "车载 GIF / MP4 · v1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}