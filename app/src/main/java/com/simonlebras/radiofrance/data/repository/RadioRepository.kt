package com.simonlebras.radiofrance.data.repository

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import io.reactivex.Observable

interface RadioRepository {
    var queue: List<MediaSessionCompat.QueueItem>

    var metadata: Map<String, MediaMetadataCompat>

    val radios: Observable<List<MediaMetadataCompat>>

    fun reset()
}
