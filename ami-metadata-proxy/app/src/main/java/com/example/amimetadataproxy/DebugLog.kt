package com.example.amimetadataproxy

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val PREFS = "ami_proxy_prefs"
    private const val KEY_LOG = "debug_log"
    private const val MAX_LINES = 80
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun add(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_LOG, "") ?: ""
        val lines = (current.lines().filter { it.isNotBlank() } + "${sdf.format(Date())}  $message")
            .takeLast(MAX_LINES)
        prefs.edit().putString(KEY_LOG, lines.joinToString("\n")).apply()
    }

    fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LOG, "Nog geen debugregels")
            ?: "Nog geen debugregels"

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LOG).apply()
    }
}
