package com.gifapp.car.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gifapp.car.CarApplication
import com.gifapp.car.data.RemoteFile
import com.gifapp.car.data.normalizeServerUrl
import com.gifapp.car.data.toUserMessage
import com.gifapp.car.download.DownloadState
import com.gifapp.car.download.StorageManager
import com.gifapp.car.integration.SelectedGifStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File

data class SessionUiState(
    val serverUrl: String = "",
    val apiKey: String = ""
)

data class FilesUiState(
    val items: List<RemoteFile> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val page: Int = 1,
    val total: Long = 0,
    val error: String? = null,
    val endReached: Boolean = false
)

sealed class DownloadUi {
    data class Running(val percent: Int) : DownloadUi()
    data class Success(val path: String) : DownloadUi()
    data class Failed(val message: String) : DownloadUi()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val locator = (app as CarApplication).locator
    private val repo = locator.repository
    private val sessionStore = locator.session
    private val downloader = locator.downloader

    private val _session = MutableStateFlow(SessionUiState())
    val session: StateFlow<SessionUiState> = _session.asStateFlow()

    private val _files = MutableStateFlow(FilesUiState())
    val files: StateFlow<FilesUiState> = _files.asStateFlow()

    private val _downloads = MutableStateFlow<Map<Long, DownloadUi>>(emptyMap())
    val downloads: StateFlow<Map<Long, DownloadUi>> = _downloads.asStateFlow()

    private val _localFiles = MutableStateFlow<List<File>>(emptyList())
    val localFiles: StateFlow<List<File>> = _localFiles.asStateFlow()

    private val _storageReady = MutableStateFlow(false)
    val storageReady: StateFlow<Boolean> = _storageReady.asStateFlow()

    private val _selectedGif = MutableStateFlow("")
    val selectedGif: StateFlow<String> = _selectedGif.asStateFlow()

    private val _writeSettingsReady = MutableStateFlow(false)
    val writeSettingsReady: StateFlow<Boolean> = _writeSettingsReady.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    init {
        refreshSession()
    }

    fun refreshSession() {
        _session.value = SessionUiState(sessionStore.serverUrl, sessionStore.apiKey)
        refreshStorage()
        refreshIntegration()
    }

    fun refreshIntegration() {
        val app = getApplication<Application>()
        _writeSettingsReady.value = SelectedGifStore.canWrite(app)
        _selectedGif.value = SelectedGifStore.selectedPath(app)
    }

    fun refreshStorage() {
        val ready = StorageManager.hasPermission(getApplication()) && StorageManager.ensureDir()
        _storageReady.value = ready
        if (ready) loadLocalFiles()
    }

    fun loadFiles(reset: Boolean = false) {
        if (sessionStore.serverUrl.isBlank()) {
            _files.update { it.copy(loading = false, error = "请先在「设置」中填写服务器地址") }
            return
        }
        val current = _files.value
        if (current.loading) return
        if (!reset && current.endReached) return
        val nextPage = if (reset) 1 else current.page + 1
        viewModelScope.launch {
            _files.update { it.copy(loading = true, refreshing = reset, error = null) }
            try {
                val resp = repo.files(nextPage)
                _files.update {
                    val merged = if (reset) resp.items else it.items + resp.items
                    it.copy(
                        items = merged,
                        loading = false,
                        refreshing = false,
                        page = resp.page,
                        total = resp.total,
                        endReached = merged.size >= resp.total
                    )
                }
            } catch (e: Exception) {
                handleKeyError(e)
                _files.update { it.copy(loading = false, refreshing = false, error = e.toUserMessage()) }
            }
        }
    }

    fun refresh() = loadFiles(reset = true)

    fun download(file: RemoteFile) {
        if (!StorageManager.hasPermission(getApplication())) {
            _toast.tryEmit("请先在「设置」中授予存储权限")
            return
        }
        StorageManager.ensureDir()
        if (_downloads.value[file.id] is DownloadUi.Running) return
        viewModelScope.launch {
            _downloads.update { it + (file.id to DownloadUi.Running(0)) }
            try {
                val url = repo.freshDownloadUrl(file.id)
                val target = StorageManager.targetFile(file.name)
                downloader.download(url, target).collect { state ->
                    when (state) {
                        is DownloadState.Progress ->
                            _downloads.update { it + (file.id to DownloadUi.Running(state.percent)) }
                        is DownloadState.Done -> {
                            _downloads.update { it + (file.id to DownloadUi.Success(state.file.absolutePath)) }
                            _toast.tryEmit("已保存到 wear_gif/${state.file.name}")
                            loadLocalFiles()
                        }
                        is DownloadState.Error -> {
                            _downloads.update { it + (file.id to DownloadUi.Failed(state.message)) }
                            _toast.tryEmit(state.message)
                        }
                    }
                }
            } catch (e: Exception) {
                handleKeyError(e)
                _downloads.update { it + (file.id to DownloadUi.Failed(e.toUserMessage())) }
            }
        }
    }

    fun clearDownloadState(id: Long) {
        _downloads.update { it - id }
    }

    fun loadLocalFiles() {
        _localFiles.value = StorageManager.localFiles()
    }

    fun deleteLocal(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            file.delete()
            if (_selectedGif.value == file.absolutePath) {
                SelectedGifStore.select(getApplication(), "")
                _selectedGif.value = ""
            }
            loadLocalFiles()
        }
    }

    /** 点击「使用」：把该文件绝对路径写入 Settings.System 属性，其他应用监听后即可加载。 */
    fun useFile(file: File) {
        val app = getApplication<Application>()
        if (!SelectedGifStore.canWrite(app)) {
            _writeSettingsReady.value = false
            _toast.tryEmit("请先在「设置」中授予「修改系统设置」权限")
            return
        }
        if (SelectedGifStore.select(app, file.absolutePath)) {
            _selectedGif.value = file.absolutePath
            _toast.tryEmit("已设为使用：${file.name}")
        } else {
            _toast.tryEmit("写入失败，请检查「修改系统设置」权限")
        }
    }

    fun updateConnection(serverUrl: String, apiKey: String) {
        sessionStore.serverUrl = normalizeServerUrl(serverUrl)
        sessionStore.apiKey = apiKey.trim()
        refreshSession()
        _toast.tryEmit("已保存，正在连接…")
        loadFiles(reset = true)
    }

    private fun handleKeyError(e: Throwable) {
        if (e is HttpException && (e.code() == 401 || e.code() == 403)) {
            _toast.tryEmit("访问密钥无效，请在「设置」中检查密钥")
        }
    }
}