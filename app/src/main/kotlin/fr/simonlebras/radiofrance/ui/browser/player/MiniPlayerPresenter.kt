package fr.simonlebras.radiofrance.ui.browser.player

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.ui.base.BasePresenter
import fr.simonlebras.radiofrance.ui.base.BaseView
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import javax.inject.Inject

@FragmentScope
class MiniPlayerPresenter @Inject constructor(val radioManager: RadioManager) : BasePresenter<MiniPlayerPresenter.View>() {
    override fun onDetachView() {
        compositeDisposable.clear()

        super.onDetachView()
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
        fun onMetadataChanged(metadata: MediaMetadataCompat?)

        fun onPlaybackStateChanged(playbackState: PlaybackStateCompat?)
    }
}
