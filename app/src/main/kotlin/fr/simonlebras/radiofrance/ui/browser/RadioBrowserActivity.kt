package fr.simonlebras.radiofrance.ui.browser

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatDelegate
import android.support.v7.app.MediaRouteButton
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.cast.framework.*
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.RadioFranceApplication
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserModule
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.partial_toolbar.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RadioBrowserActivity : BaseActivity<RadioBrowserPresenter>(),
        RadioBrowserPresenter.View,
        RadioListFragment.Callback,
        MiniPlayerFragment.Callback {
    companion object {
        const val REQUEST_CODE = 1

        private const val SHOW_DELAY = 1000L

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    override val component by lazy(LazyThreadSafetyMode.NONE) {
        (application as RadioFranceApplication).component
                .plus(RadioBrowserModule(this))
    }

    override val isSearching: Boolean
        get() {
            if (searchView == null) {
                return false
            }

            if (searchView!!.isIconified) {
                return false
            }

            return true
        }

    override val currentQuery: String
        get() = searchView?.query?.toString() ?: ""

    @Inject lateinit var castContext: CastContext

    private lateinit var radioListFragment: RadioListFragment
    private lateinit var miniPlayerFragment: MiniPlayerFragment
    private lateinit var mediaRouteMenuItem: MenuItem
    private var searchView: SearchView? = null
    private val handler = Handler()
    private val handlerCallbacks = Runnable {
        if (mediaRouteMenuItem.isVisible) {
            showCastOverlay()
        }
    }
    private val castStateListener = CastStateListener { newState ->
        if (newState != CastState.NO_DEVICES_AVAILABLE) {
            handler.postDelayed(handlerCallbacks, SHOW_DELAY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        component.inject(this)

        setContentView(R.layout.activity_radio_browser)
        setSupportActionBar(toolbar)

        radioListFragment = supportFragmentManager.findFragmentById(R.id.fragment_radio_browser) as RadioListFragment
        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.fragment_mini_player) as MiniPlayerFragment
    }

    override fun onStart() {
        super.onStart()

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onResume() {
        super.onResume()

        castContext.addCastStateListener(castStateListener)
    }

    override fun onPause() {
        handler.removeCallbacks(handlerCallbacks)
        castContext.removeCastStateListener(castStateListener)

        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.activity_radio_browser, menu)

        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.action_media_route)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = MenuItemCompat.getActionView(menu.findItem(R.id.action_search)) as SearchView
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        subscribeToSearchView(searchView!!)

        return true
    }

    override fun restorePresenter() {
        presenter = presenterManager[uuid] as? RadioBrowserPresenter ?: component.radioBrowserPresenter()
        presenterManager[uuid] = presenter
    }

    override fun setMediaController(mediaController: MediaControllerCompat) {
        MediaControllerCompat.setMediaController(this, mediaController)
    }

    override fun onConnected(mediaController: MediaControllerCompat) {
        miniPlayerFragment.onConnected()
        updateToolbarTitle(mediaController.metadata?.description?.title?.toString())
    }

    override fun changeMiniPlayerVisibility() {
        if (shouldShowMiniPlayer()) {
            showMiniPlayer()
            return
        }

        hideMiniPlayer()
    }

    override fun updateToolbarTitle(title: String?) {
        toolbar.title = title ?: getString(R.string.label_radios)
    }

    override fun showPlaybackError(error: String) {
        radioListFragment.showPlaybackError(error)
    }

    private fun subscribeToSearchView(searchView: SearchView) {
        compositeDisposable.add(Flowable
                .create<String>({
                    val listener = object : SearchView.OnQueryTextListener {
                        override fun onQueryTextChange(newText: String): Boolean {
                            if (!it.isCancelled) {
                                it.onNext(newText)
                                return true
                            }
                            return false
                        }

                        override fun onQueryTextSubmit(query: String) = false
                    }

                    it.setCancellable {
                        searchView.setOnQueryTextListener(null)
                    }

                    searchView.setOnQueryTextListener(listener)
                }, BackpressureStrategy.LATEST)
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .debounce(200, TimeUnit.MILLISECONDS)
                .map(String::trim)
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    radioListFragment.searchRadios(it)
                }))
    }

    private fun shouldShowMiniPlayer(): Boolean {
        val mediaController = MediaControllerCompat.getMediaController(this)
        if ((mediaController == null) ||
                (mediaController.metadata == null) ||
                (mediaController.playbackState == null)) {
            return false
        }

        when (mediaController.playbackState.state) {
            PlaybackStateCompat.STATE_ERROR,
            PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.STATE_STOPPED -> return false
            else -> return true
        }
    }

    private fun showMiniPlayer() {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_top, R.anim.slide_out_top)
                .show(miniPlayerFragment)
                .commit()
    }

    private fun hideMiniPlayer() {
        supportFragmentManager.beginTransaction()
                .hide(miniPlayerFragment)
                .commit()
    }

    private fun showCastOverlay() {
        val actionView = MenuItemCompat.getActionView(toolbar.menu.findItem(R.id.action_media_route))
        if (actionView != null && actionView is MediaRouteButton) {
            IntroductoryOverlay.Builder(this, mediaRouteMenuItem)
                    .setTitleText(R.string.touch_to_cast)
                    .setSingleTime()
                    .build()
                    .show()
        }
    }
}
