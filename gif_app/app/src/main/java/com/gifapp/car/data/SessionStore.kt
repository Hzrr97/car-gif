package com.gifapp.car.data

import android.content.Context

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("gifcar_session", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    companion object {
        const val KEY_SERVER = "server_url"
        const val KEY_API_KEY = "api_key"
        const val DEFAULT_API_KEY = "2466ad5cfde0f946a2ff2007a637a59858f95bb71232206c"
    }
}