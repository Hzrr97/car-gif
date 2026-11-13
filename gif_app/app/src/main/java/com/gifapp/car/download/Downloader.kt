package com.gifapp.car.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed class DownloadState {
    data class Progress(val percent: Int, val bytesRead: Long, val totalBytes: Long) : DownloadState()
    data class Done(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class Downloader(private val client: OkHttpClient) {
    fun download(url: String, target: File): Flow<DownloadState> = flow {
        try {
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, target.name + ".part")
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Error("下载失败（${response.code}）"))
                    return@flow
                }
                val body = response.body
                if (body == null) {
                    emit(DownloadState.Error("服务器返回空内容"))
                    return@flow
                }
                val total = body.contentLength()
                var read = 0L
                var lastPct = -1
                tmp.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n == -1) break
                            out.write(buf, 0, n)
                            read += n
                            val pct = if (total > 0) (read * 100 / total).toInt() else 0
                            if (pct != lastPct) {
                                lastPct = pct
                                emit(DownloadState.Progress(pct, read, total))
                            }
                        }
                    }
                }
                if (target.exists()) target.delete()
                if (!tmp.renameTo(target)) {
                    emit(DownloadState.Error("保存文件失败"))
                    return@flow
                }
            }
            emit(DownloadState.Done(target))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "下载出错"))
        }
    }.flowOn(Dispatchers.IO)
}