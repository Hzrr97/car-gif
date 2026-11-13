package com.gifapp.car.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/client/files")
    suspend fun listFiles(
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int
    ): FileListResponse

    @GET("api/client/files/{id}/download")
    suspend fun downloadUrl(@Path("id") id: Long): DownloadUrlResponse
}