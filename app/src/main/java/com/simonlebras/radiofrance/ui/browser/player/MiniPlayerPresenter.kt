package com.simonlebras.radiofrance.ui.browser.player

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.di.scopes.FragmentScope
import com.simonlebras.radiofrance.ui.base.BasePresenter
import com.simonlebras.radiofrance.ui.base.BaseView
import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import javax.inject.Inject

@FragmentScope
class MiniPlayerPresenter @Inject constructor(private val radioManager: RadioManager) : BasePresenter<MiniPlayerPresenter.View>() {
    fun subscribeToPlaybackUpdates() {
        compositeDisposable.add(radioManager.playbackUpdates
                .subscribe {
                    if (it is MediaMetadataCompat) {
                        view?.onMetadataChanged(it)
                    } else if (it is PlaybackStateCompat) {
                        view?.onPlaybackStateChanged(it)
                    }
                }
        )
    }

    fun play() {
        radioManager.play()
    }

    fun pause() {
        radioManager.pause()
    }

    fun skipToPrevious() {
        radioManager.skipToPrevious()
    }

    fun skipToNext() {
        radioManager.skipToNext()
    }

    interface View : BaseView {
        fun onMetadataChanged(metadata: MediaMetadataCompat)

        fun onPlaybackStateChanged(playbackState: PlaybackStateCompat)
    }
}
