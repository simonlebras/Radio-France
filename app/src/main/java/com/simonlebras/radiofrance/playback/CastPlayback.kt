package com.simonlebras.radiofrance.playback

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import timber.log.Timber

class CastPlayback(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val callback: Playback.Callback
) : RemoteMediaClient.Callback(),
    Playback {
    private val remoteMediaClient = sessionManager.currentCastSession.remoteMediaClient.apply {
        registerCallback(this@CastPlayback)
    }

    private var currentItem: MediaSessionCompat.QueueItem? = null

    override var playbackState = STATE_NONE

    override val isPlaying get() = sessionManager.currentCastSession?.isConnected == true && remoteMediaClient.isPlaying

    override fun play(item: MediaSessionCompat.QueueItem) {
        if (currentItem != item) {
            currentItem = item

            remoteMediaClient.stop()

            with(item.description) {
                val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
                    putString(MediaMetadata.KEY_TITLE, title.toString())
                    putString(MediaMetadata.KEY_SUBTITLE, description.toString())

                    val image =
                        WebImage(Uri.Builder().encodedPath(iconUri.toString()).build())
                    addImage(image)
                }

                val mediaInfo = MediaInfo.Builder(mediaUri.toString())
                    .setContentType(MIME_TYPE_AUDIO_MPEG)
                    .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
                    .setMetadata(mediaMetadata)
                    .build()

                val mediaOptions = MediaLoadOptions.Builder().build()

                remoteMediaClient.load(mediaInfo, mediaOptions)
            }
        } else {
            remoteMediaClient.play()
        }
    }

    override fun pause() {
        remoteMediaClient.pause()
    }

    override fun stop() {
        remoteMediaClient.stop()
    }

    override fun release() {
        remoteMediaClient.unregisterCallback(this)

        stop()
    }

    override fun onStatusUpdated() {
        val status = remoteMediaClient.playerState

        when (status) {
            MediaStatus.PLAYER_STATE_BUFFERING -> {
                playbackState = STATE_BUFFERING

                callback.onPlaybackStateChanged(playbackState)
            }
            MediaStatus.PLAYER_STATE_PLAYING -> {
                playbackState = STATE_PLAYING

                callback.onPlaybackStateChanged(playbackState)
            }
            MediaStatus.PLAYER_STATE_PAUSED -> {
                playbackState = STATE_PAUSED

                callback.onPlaybackStateChanged(playbackState)
            }
            MediaStatus.PLAYER_STATE_IDLE -> {
                when (remoteMediaClient.idleReason) {
                    MediaStatus.IDLE_REASON_CANCELED, MediaStatus.IDLE_REASON_INTERRUPTED -> {
                        playbackState = STATE_STOPPED

                        callback.onPlaybackStateChanged(playbackState)
                    }
                    MediaStatus.IDLE_REASON_ERROR -> {
                        playbackState = STATE_ERROR

                        callback.onError(context.getString(R.string.error_occurred))
                    }
                }
            }
            else -> {
                if (BuildConfig.DEBUG) {
                    Timber.d("State default: %d", status)
                }
            }
        }
    }

    private companion object {
        const val MIME_TYPE_AUDIO_MPEG = "audio/mpeg"
    }
}
