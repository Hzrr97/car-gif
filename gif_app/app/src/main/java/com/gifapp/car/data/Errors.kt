package com.gifapp.car.data

import com.google.gson.Gson
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val gson = Gson()

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val raw = try {
            response()?.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        val parsed = try {
            gson.fromJson(raw, ApiError::class.java)?.error
        } catch (e: Exception) {
            null
        }
        parsed ?: "请求失败（${code()}）"
    }
    is UnknownHostException -> "无法连接服务器，请检查地址和网络"
    is ConnectException -> "连接服务器失败，请确认服务已启动"
    is SocketTimeoutException -> "连接超时，请重试"
    else -> message ?: "未知错误"
}

fun normalizeServerUrl(url: String): String {
    var u = url.trim()
    if (u.isEmpty()) return u
    if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
    return u.trimEnd('/')
}