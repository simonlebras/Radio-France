package com.simonlebras.radiofrance.playback

import android.support.v4.media.session.MediaSessionCompat

interface Playback {
    var currentRadioId: String?

    var playbackState: Int

    var callback: Callback?

    val isConnected: Boolean
        get() = true

    val isPlaying: Boolean

    fun start() {
    }

    fun play(item: MediaSessionCompat.QueueItem)

    fun pause()

    fun stop(notify: Boolean)

    interface Callback {
        fun onPlaybackStateChanged(playbackState: Int)

        fun onError(error: String)

        fun setCurrentRadioId(id: String)
    }
}
