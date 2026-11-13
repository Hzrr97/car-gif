package com.gifapp.car.data

class FileRepository(private val api: ApiService) {
    suspend fun files(page: Int, pageSize: Int = PAGE_SIZE): FileListResponse =
        api.listFiles(page, pageSize)

    suspend fun freshDownloadUrl(id: Long): String = api.downloadUrl(id).url

    companion object {
        const val PAGE_SIZE = 40
    }
}