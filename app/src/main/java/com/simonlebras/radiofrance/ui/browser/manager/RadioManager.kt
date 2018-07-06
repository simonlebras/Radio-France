package com.simonlebras.radiofrance.ui.browser.manager

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.data.models.Resource
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface RadioManager {
    fun connect(): Deferred<Boolean>

    fun playbackStateUpdates(): ReceiveChannel<PlaybackStateCompat>

    fun metadataUpdates(): ReceiveChannel<MediaMetadataCompat>

    fun loadRadios(): Deferred<Resource<List<Radio>>>

    fun playFromId(id: String)

    fun togglePlayPause()

    fun skipToPrevious()

    fun skipToNext()

    fun clear()
}
