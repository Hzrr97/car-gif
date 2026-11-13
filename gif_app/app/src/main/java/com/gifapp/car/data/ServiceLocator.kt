package com.gifapp.car.data

import android.content.Context
import android.net.Uri
import com.gifapp.car.download.Downloader
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ServiceLocator(context: Context) {
    val session = SessionStore(context)

    private val hostInterceptor = Interceptor { chain ->
        val req = chain.request()
        val server = session.serverUrl
        if (server.isBlank()) return@Interceptor chain.proceed(req)
        val uri = Uri.parse(server)
        val host = uri.host ?: return@Interceptor chain.proceed(req)
        val scheme = uri.scheme ?: "http"
        val port = if (uri.port != -1) uri.port else if (scheme == "https") 443 else 80
        val newUrl = req.url.newBuilder()
            .scheme(scheme)
            .host(host)
            .port(port)
            .build()
        chain.proceed(req.newBuilder().url(newUrl).build())
    }

    // 车机客户端用共享密钥鉴权（X-API-Key），不再需要用户登录
    private val apiKeyInterceptor = Interceptor { chain ->
        val key = session.apiKey
        val req = if (key.isNotEmpty()) {
            chain.request().newBuilder().header("X-API-Key", key).build()
        } else {
            chain.request()
        }
        chain.proceed(req)
    }

    private val apiClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(hostInterceptor)
        .addInterceptor(apiKeyInterceptor)
        .build()

    // 下载直连 R2 预签名 URL，绝不能套用上面的 host/key 拦截器
    private val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("http://placeholder.local/")
        .client(apiClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
    val repository = FileRepository(api)
    val downloader = Downloader(downloadClient)
}