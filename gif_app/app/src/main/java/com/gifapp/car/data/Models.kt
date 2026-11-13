package com.gifapp.car.data

data class RemoteFile(
    val id: Long,
    val name: String,
    val size: Long,
    val content_type: String,
    val created_at: String,
    val url: String? = null
) {
    val isGif: Boolean get() = content_type == "image/gif" || name.endsWith(".gif", ignoreCase = true)
    val isVideo: Boolean get() = content_type.startsWith("video/") || name.endsWith(".mp4", ignoreCase = true)
}

data class FileListResponse(
    val items: List<RemoteFile>,
    val total: Long,
    val page: Int,
    val page_size: Int
)

data class DownloadUrlResponse(val url: String, val expires_in: Int)

data class ApiError(val error: String?)