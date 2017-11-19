package fr.simonlebras.radiofrance.ui.browser

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.View.VISIBLE
import com.google.android.gms.cast.framework.*
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import dagger.Lazy
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_radio_browser.*
import kotlinx.android.synthetic.main.partial_toolbar.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RadioBrowserActivity : BaseActivity<RadioBrowserPresenter>(),
        HasSupportFragmentInjector,
        RadioBrowserPresenter.View {
    companion object {
        const val REQUEST_CODE_NOTIFICATION = 100
        const val REQUEST_CODE_SESSION = 101

        const val BUNDLE_QUERY = "BUNDLE_QUERY"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var presenterProvider: Lazy<RadioBrowserPresenter>

    private lateinit var radioListFragment: RadioListFragment
    private lateinit var miniPlayerFragment: MiniPlayerFragment

    private var mediaRouteMenuItem: MenuItem? = null

    private lateinit var castContext: CastContext
    private val castStateListener = CastStateListener {
        if (it != CastState.NO_DEVICES_AVAILABLE) {
            showCastOverlay()
        }
    }
    private var introductoryOverlay: IntroductoryOverlay? = null
    private val handler = Handler()
    private val handlerCallbacks = Runnable {
        introductoryOverlay = IntroductoryOverlay.Builder(this, mediaRouteMenuItem)
                .setTitleText(R.string.touch_to_cast)
                .setSingleTime()
                .setOnOverlayDismissedListener {
                    introductoryOverlay = null
                }
                .build()

        introductoryOverlay!!.show()
    }

    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_radio_browser)
        setSupportActionBar(toolbar)

        radioListFragment = supportFragmentManager.findFragmentById(R.id.fragment_radio_browser) as RadioListFragment
        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.fragment_mini_player) as MiniPlayerFragment

        query = savedInstanceState?.getString(BUNDLE_QUERY, null)

        castContext = CastContext.getSharedInstance(this)

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onPostResume() {
        super.onPostResume()

        changeMiniPlayerVisibility()
    }

    override fun onResume() {
        super.onResume()

        castContext.addCastStateListener(castStateListener)
    }

    override fun onPause() {
        castContext.removeCastStateListener(castStateListener)

        handler.removeCallbacks(handlerCallbacks)

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(BUNDLE_QUERY, query)

        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.activity_radio_browser, menu)

        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.action_media_route)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        if (!query.isNullOrBlank()) {
            searchItem.expandActionView()
            searchView.setQuery(query, false)
            searchView.clearFocus()
        }

        compositeDisposable.add(searchView.queryTextChanges()
                .skipInitialValue()
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .debounce(200, TimeUnit.MILLISECONDS)
                .map(CharSequence::toString)
                .map(String::trim)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    query = it

                    radioListFragment.searchRadios(it)
                }))

        return true
    }

    override fun restorePresenter() {
        presenter = presenterManager[uuid] as? RadioBrowserPresenter ?: presenterProvider.get()
        presenterManager[uuid] = presenter
    }

    override fun supportFragmentInjector() = fragmentInjector

    override fun onConnected(mediaController: MediaControllerCompat) {
        MediaControllerCompat.setMediaController(this, mediaController)

        updateToolbarTitle(mediaController.metadata?.description?.title?.toString())

        miniPlayerFragment.onConnected()
        changeMiniPlayerVisibility()

        presenter.subscribeToPlaybackUpdates()
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        updateToolbarTitle(metadata.description?.title?.toString())
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

    private fun updateToolbarTitle(title: String?) {
        toolbar.title = title ?: getString(R.string.label_radios)
    }

    private fun showCastOverlay() {
        introductoryOverlay?.remove()

        if (mediaRouteMenuItem?.isVisible == true) {
            handler.post(handlerCallbacks)
        }
    }
}
