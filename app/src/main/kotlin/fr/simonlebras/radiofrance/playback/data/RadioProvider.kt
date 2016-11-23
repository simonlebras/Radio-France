package fr.simonlebras.radiofrance.playback.data

import android.support.v4.media.MediaMetadataCompat
import io.reactivex.Flowable

interface RadioProvider {
    fun getRadios(): Flowable<List<MediaMetadataCompat>>
    fun reset()
}
