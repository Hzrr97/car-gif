package com.gifapp.car.integration

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object SelectedGifStore {
    const val KEY = "wear_gif_selected"

    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

    fun selectedPath(context: Context): String =
        Settings.System.getString(context.contentResolver, KEY) ?: ""

    fun select(context: Context, absolutePath: String): Boolean =
        Settings.System.putString(context.contentResolver, KEY, absolutePath)

    fun writeSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
}