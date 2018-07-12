package com.simonlebras.radiofrance.playback

import android.support.v4.media.session.MediaSessionCompat

interface Playback {
    val isPlaying: Boolean

    fun play(item: MediaSessionCompat.QueueItem)

    fun pause()

    fun stop()

    fun release()

    interface Callback {
        fun onPlaybackStateChanged(state: Int)

        fun onError(error: String)
    }
}
