package fr.simonlebras.radiofrance.ui.browser.activity

import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.base.BasePresenter
import fr.simonlebras.radiofrance.ui.base.BaseView
import fr.simonlebras.radiofrance.ui.browser.manager.MediaControllerCompatWrapper
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import javax.inject.Inject

@ActivityScope
class RadioBrowserActivityPresenter @Inject constructor(val radioManager: RadioManager) : BasePresenter<RadioBrowserActivityPresenter.View>() {
    override fun onDestroy() {
        radioManager.reset()

        super.onDestroy()
    }

    fun connect() {
        compositeDisposable.add(radioManager.connect()
                .subscribe {
                    view?.setMediaController(it)
                })
    }

    interface View : BaseView {
        fun setMediaController(mediaControllerWrapper: MediaControllerCompatWrapper)
    }
}
