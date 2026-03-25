package com.example.amimetadataproxy

import android.content.Context

object AppPrefs {
    private const val PREFS = "ami_proxy_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SPOTIFY_ONLY = "spotify_only"
    private const val KEY_LAST_TITLE = "last_title"
    private const val KEY_LAST_ARTIST = "last_artist"
    private const val KEY_LAST_SOURCE = "last_source"
    private const val KEY_LAST_AUDIO_DEVICE = "last_audio_device"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun isSpotifyOnly(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SPOTIFY_ONLY, true)

    fun setSpotifyOnly(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SPOTIFY_ONLY, value).apply()
    }

    fun saveLastTrack(context: Context, title: String?, artist: String?, source: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_TITLE, title)
            .putString(KEY_LAST_ARTIST, artist)
            .putString(KEY_LAST_SOURCE, source)
            .apply()
    }

    fun getLastTitle(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_TITLE, null)

    fun getLastArtist(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_ARTIST, null)

    fun getLastSource(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_SOURCE, null)

    fun saveLastAudioDevice(context: Context, value: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST_AUDIO_DEVICE, value).apply()
    }

    fun getLastAudioDevice(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_AUDIO_DEVICE, null)
}
