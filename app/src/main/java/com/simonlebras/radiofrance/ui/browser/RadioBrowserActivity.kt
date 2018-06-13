package com.simonlebras.radiofrance.ui.browser

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.support.v7.app.AppCompatDelegate
import android.view.View.VISIBLE
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.ui.base.BaseActivity
import com.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import com.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import dagger.Lazy
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_radio_browser.*
import javax.inject.Inject

class RadioBrowserActivity : BaseActivity<RadioBrowserPresenter>(),
        HasSupportFragmentInjector,
        RadioBrowserPresenter.View {
    companion object {
        const val REQUEST_CODE_NOTIFICATION = 100
        const val REQUEST_CODE_SESSION = 101

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var presenterProvider: Lazy<RadioBrowserPresenter>

    private lateinit var radioListFragment: RadioListFragment
    private lateinit var miniPlayerFragment: MiniPlayerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_radio_browser)

        radioListFragment = supportFragmentManager.findFragmentById(R.id.fragment_radio_browser) as RadioListFragment
        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.fragment_mini_player) as MiniPlayerFragment

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onPostResume() {
        super.onPostResume()

        changeMiniPlayerVisibility()
    }

    override fun restorePresenter() {
        presenter = presenterManager[uuid] as? RadioBrowserPresenter ?: presenterProvider.get()
        presenterManager[uuid] = presenter
    }

    override fun supportFragmentInjector() = fragmentInjector

    override fun onConnected(mediaController: MediaControllerCompat) {
        MediaControllerCompat.setMediaController(this, mediaController)

        miniPlayerFragment.onConnected()
        changeMiniPlayerVisibility()

        presenter.subscribeToPlaybackUpdates()
    }

    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        changeMiniPlayerVisibility()
    }

    private fun changeMiniPlayerVisibility() {
        if (shouldShowMiniPlayer()) {
            showMiniPlayer()
            return
        }

        hideMiniPlayer()
    }

    private fun shouldShowMiniPlayer(): Boolean {
        val mediaController = MediaControllerCompat.getMediaController(this)
        if ((mediaController == null) || (mediaController.metadata == null) || (mediaController.playbackState == null)) {
            return false
        }

        return when (mediaController.playbackState.state) {
            STATE_ERROR, STATE_NONE, STATE_STOPPED -> false
            else -> true
        }
    }

    private fun showMiniPlayer() {
        container_mini_player.visibility = VISIBLE

        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_top, R.anim.slide_out_top)
                .show(miniPlayerFragment)
                .commitNowAllowingStateLoss()
    }

    private fun hideMiniPlayer() {
        supportFragmentManager.beginTransaction()
                .hide(miniPlayerFragment)
                .commitNowAllowingStateLoss()
    }
}
