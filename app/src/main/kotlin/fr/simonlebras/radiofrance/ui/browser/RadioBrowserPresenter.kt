package fr.simonlebras.radiofrance.ui.browser

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.base.BasePresenter
import fr.simonlebras.radiofrance.ui.base.BaseView
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import javax.inject.Inject

@ActivityScope
class RadioBrowserPresenter @Inject constructor(val radioManager: RadioManager) : BasePresenter<RadioBrowserPresenter.View>() {
    override fun onDetachView() {
        compositeDisposable.clear()

        super.onDetachView()
    }

    override fun onDestroy() {
        radioManager.reset()

        super.onDestroy()
    }

    fun connect() {
        compositeDisposable.add(radioManager.connection
                .subscribe {
                    view?.setMediaController(it)
                    view?.onConnected(it)
                    view?.changeMiniPlayerVisibility()
                    subscribeToPlaybackUpdates()
                })
    }

    private fun subscribeToPlaybackUpdates() {
        compositeDisposable.add(radioManager.playbackUpdates
                .subscribe {
                    if (it is MediaMetadataCompat) {
                        view?.updateToolbarTitle(it.description.title?.toString())
                    }

                    view?.changeMiniPlayerVisibility()
                }
        )
    }

    interface View : BaseView {
        fun setMediaController(mediaController: MediaControllerCompat)

        fun onConnected(mediaController: MediaControllerCompat)

        fun changeMiniPlayerVisibility()

        fun updateToolbarTitle(title: String?)
    }
}
