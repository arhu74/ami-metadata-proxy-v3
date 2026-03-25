package com.example.amimetadataproxy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationCompat

class MetadataProxyNotificationListener : NotificationListenerService(), MediaSessionManager.OnActiveSessionsChangedListener {

    private lateinit var mediaSession: MediaSession
    private lateinit var sessionManager: MediaSessionManager
    private var sourceController: MediaController? = null
    private var sourceCallback: MediaController.Callback? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1001, buildNotification("Proxy start op"))

        mediaSession = MediaSession(this, "AMI-Metadata-Proxy").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackToLocal(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { sourceController?.transportControls?.play() }
                override fun onPause() { sourceController?.transportControls?.pause() }
                override fun onSkipToNext() { sourceController?.transportControls?.skipToNext() }
                override fun onSkipToPrevious() { sourceController?.transportControls?.skipToPrevious() }
                override fun onStop() { sourceController?.transportControls?.stop() }
            })
            isActive = true
        }

        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        DebugLog.add(this, "Notification listener created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            sessionManager.addOnActiveSessionsChangedListener(this, null)
            DebugLog.add(this, "Notification listener connected")
            onActiveSessionsChanged(sessionManager.getActiveSessions(null))
        } catch (e: SecurityException) {
            DebugLog.add(this, "SecurityException: meldingstoegang nog niet volledig actief")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        cleanupSourceCallback()
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(this) }
        DebugLog.add(this, "Notification listener disconnected")
    }

    override fun onDestroy() {
        cleanupSourceCallback()
        mediaSession.release()
        DebugLog.add(this, "Service destroyed")
        super.onDestroy()
    }

    override fun onActiveSessionsChanged(controllers: MutableList<MediaController>?) {
        val list = controllers.orEmpty().filterNot { it.packageName == packageName }
        DebugLog.add(this, "Actieve sessies: ${list.joinToString { it.packageName }}")
        attachToController(selectCandidate(list))
    }

    private fun selectCandidate(controllers: List<MediaController>): MediaController? {
        if (!AppPrefs.isEnabled(this)) return null

        val spotifyOnly = AppPrefs.isSpotifyOnly(this)
        val filtered = if (spotifyOnly) {
            controllers.filter { it.packageName == SPOTIFY_PACKAGE }
        } else {
            controllers
        }

        val preferredStateOrder = listOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_PAUSED
        )

        for (state in preferredStateOrder) {
            val hit = filtered.firstOrNull { it.playbackState?.state == state }
            if (hit != null) {
                DebugLog.add(this, "Gekozen bron: ${hit.packageName} / state=$state")
                return hit
            }
        }

        val fallback = filtered.firstOrNull() ?: if (spotifyOnly) null else controllers.firstOrNull()
        if (fallback != null) {
            DebugLog.add(this, "Fallback bron: ${fallback.packageName}")
        } else {
            DebugLog.add(this, "Geen bruikbare bron gevonden")
        }
        return fallback
    }

    private fun attachToController(controller: MediaController?) {
        if (controller?.sessionToken == sourceController?.sessionToken) return

        cleanupSourceCallback()
        sourceController = controller

        if (controller == null) {
            mediaSession.setMetadata(null)
            mediaSession.setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_NONE, 0L, 0f)
                    .setActions(defaultActions())
                    .build()
            )
            updateForegroundText("Geen actieve bron")
            AppPrefs.saveLastTrack(this, null, null, null)
            return
        }

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                publishSwappedMetadata(metadata)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                mediaSession.setPlaybackState(copyPlaybackState(state))
                DebugLog.add(this@MetadataProxyNotificationListener, "Playback state gewijzigd naar ${state?.state}")
            }

            override fun onSessionDestroyed() {
                DebugLog.add(this@MetadataProxyNotificationListener, "Bronsessie vernietigd")
                attachToController(null)
            }
        }
        sourceCallback = callback
        controller.registerCallback(callback)

        publishSwappedMetadata(controller.metadata)
        mediaSession.setPlaybackState(copyPlaybackState(controller.playbackState))
        updateForegroundText("Proxy actief voor ${controller.packageName}")
    }

    private fun publishSwappedMetadata(input: MediaMetadata?) {
        if (input == null) {
            mediaSession.setMetadata(null)
            DebugLog.add(this, "Geen metadata ontvangen")
            return
        }

        val title = input.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = input.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val album = input.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val albumArtist = input.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        val displayTitle = input.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val displaySubtitle = input.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val duration = input.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val art = input.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: input.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: input.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val effectiveTitle = title ?: displayTitle
        val effectiveArtist = artist ?: displaySubtitle ?: albumArtist ?: album
        val replacementAlbum = effectiveArtist ?: album

        val builder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, effectiveTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, effectiveArtist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, replacementAlbum)
            .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, effectiveArtist)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, effectiveTitle)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, effectiveArtist)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, replacementAlbum)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)

        if (art != null) {
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
            builder.putBitmap(MediaMetadata.METADATA_KEY_ART, art)
            builder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, art)
        }

        mediaSession.setMetadata(builder.build())
        AppPrefs.saveLastTrack(this, effectiveTitle, effectiveArtist, sourceController?.packageName)
        DebugLog.add(
            this,
            "Metadata gepusht: title='${effectiveTitle ?: "?"}', artist='${effectiveArtist ?: "?"}', album->'${replacementAlbum ?: "?"}'"
        )
        updateForegroundText("${effectiveTitle ?: "Onbekende track"} — ${effectiveArtist ?: "Onbekende artiest"}")
    }

    private fun copyPlaybackState(state: PlaybackState?): PlaybackState {
        if (state == null) {
            return PlaybackState.Builder()
                .setActions(defaultActions())
                .setState(PlaybackState.STATE_NONE, 0L, 0f)
                .build()
        }

        return PlaybackState.Builder()
            .setActions(state.actions or defaultActions())
            .setBufferedPosition(state.bufferedPosition)
            .setState(state.state, state.position, state.playbackSpeed)
            .build()
    }

    private fun defaultActions(): Long {
        return PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_PLAY_PAUSE
    }

    private fun cleanupSourceCallback() {
        sourceCallback?.let { callback ->
            sourceController?.unregisterCallback(callback)
        }
        sourceCallback = null
        sourceController = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AMI Metadata Proxy",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AMI Metadata Proxy")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun updateForegroundText(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, buildNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "ami_proxy"
        private const val SPOTIFY_PACKAGE = "com.spotify.music"
    }
}
