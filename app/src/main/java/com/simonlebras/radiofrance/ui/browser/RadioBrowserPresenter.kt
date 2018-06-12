package com.simonlebras.radiofrance.ui.browser

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.di.scopes.ActivityScope
import com.simonlebras.radiofrance.ui.base.BasePresenter
import com.simonlebras.radiofrance.ui.base.BaseView
import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import javax.inject.Inject

@ActivityScope
class RadioBrowserPresenter @Inject constructor(
        private val radioManager: RadioManager
) : BasePresenter<RadioBrowserPresenter.View>() {
    override fun onDestroy() {
        radioManager.reset()

        super.onDestroy()
    }

    fun connect() {
        compositeDisposable.add(radioManager.connection
                .subscribe {
                    view?.onConnected(it)
                })
    }

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

    interface View : BaseView {
        fun onConnected(mediaController: MediaControllerCompat)

        fun onMetadataChanged(metadata: MediaMetadataCompat)

        fun onPlaybackStateChanged(playbackState: PlaybackStateCompat)
    }
}
