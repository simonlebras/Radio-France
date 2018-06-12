package com.simonlebras.radiofrance.ui.browser.manager

import android.support.v4.media.session.MediaControllerCompat
import com.simonlebras.radiofrance.models.Radio
import io.reactivex.Observable

interface RadioManager {
    val connection: Observable<MediaControllerCompat>

    val radios: Observable<List<Radio>>

    val playbackUpdates: Observable<Any>

    fun play()

    fun play(id: String)

    fun pause()

    fun skipToPrevious()

    fun skipToNext()

    fun reset()
}
