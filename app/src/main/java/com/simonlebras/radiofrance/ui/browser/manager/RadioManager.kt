package com.simonlebras.radiofrance.ui.browser.manager

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.data.models.Resource
import io.reactivex.Observable
import io.reactivex.Single

interface RadioManager {
    fun connect(): Observable<MediaControllerCompat>

    fun playbackStateUpdates(): Observable<PlaybackStateCompat>

    fun metadataUpdates(): Observable<MediaMetadataCompat>

    fun loadRadios(): Single<Resource<List<Radio>>>

    fun playFromId(id: String)

    fun togglePlayPause()

    fun skipToPrevious()

    fun skipToNext()

    fun clear()
}
