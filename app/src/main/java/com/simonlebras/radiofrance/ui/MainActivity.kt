package com.simonlebras.radiofrance.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.support.v7.app.AppCompatDelegate
import android.view.View.VISIBLE
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import com.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.android.synthetic.main.activity_radio_browser.*
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {
    companion object {
        const val REQUEST_CODE_NOTIFICATION = 100
        const val REQUEST_CODE_SESSION = 101

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var radioListFragment: RadioListFragment
    private lateinit var miniPlayerFragment: MiniPlayerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_radio_browser)

        radioListFragment = supportFragmentManager.findFragmentById(R.id.fragment_radio_browser) as RadioListFragment
        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.fragment_mini_player) as MiniPlayerFragment

        val viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(MainViewModel::class.java)

        viewModel.connect()

        viewModel.connection.observe(this, Observer {
            MediaControllerCompat.setMediaController(this, it)

            toggleMiniPlayerVisibility()
        })

        viewModel.playbackState.observe(this, Observer {
            toggleMiniPlayerVisibility()
        })
    }

    private fun toggleMiniPlayerVisibility() {
        if (shouldShowMiniPlayer()) {
            showMiniPlayer()
        } else {
            hideMiniPlayer()
        }
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
