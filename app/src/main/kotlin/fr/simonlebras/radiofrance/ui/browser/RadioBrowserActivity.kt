package fr.simonlebras.radiofrance.ui.browser

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.gms.cast.framework.*
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import dagger.Lazy
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasDispatchingSupportFragmentInjector
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import fr.simonlebras.radiofrance.ui.preferences.PreferencesActivity
import fr.simonlebras.radiofrance.ui.preferences.PreferencesFragment.Companion.PREFERENCE_KEY_LIST_TYPE
import fr.simonlebras.radiofrance.ui.preferences.PreferencesFragment.Companion.PREFERENCE_VALUE_LIST_TYPE_GRID
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RadioBrowserActivity : BaseActivity<RadioBrowserPresenter>(),
        HasDispatchingSupportFragmentInjector,
        RadioBrowserPresenter.View {
    companion object {
        const val REQUEST_CODE_NOTIFICATION = 100
        const val REQUEST_CODE_SESSION = 101

        const val REQUEST_CODE_PREFERENCES = 100

        const val BUNDLE_QUERY = "BUNDLE_QUERY"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var presenterProvider: Lazy<RadioBrowserPresenter>
    @Inject lateinit var sharedPreferences: SharedPreferences

    @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
    @BindView(R.id.container_mini_player) lateinit var containerMiniPlayer: View

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

    private lateinit var listType: String
    private var listTypeChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_radio_browser)
        ButterKnife.bind(this)
        setSupportActionBar(toolbar)

        listType = sharedPreferences.getString(PREFERENCE_KEY_LIST_TYPE, PREFERENCE_VALUE_LIST_TYPE_GRID)

        if (savedInstanceState == null) {
            replaceRadioListFragment(listType, false)
        } else {
            radioListFragment = supportFragmentManager.findFragmentByTag(RadioListFragment.TAG) as RadioListFragment

            query = savedInstanceState.getString(BUNDLE_QUERY, null)
        }

        miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.fragment_mini_player) as MiniPlayerFragment

        castContext = CastContext.getSharedInstance(this)

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onPostResume() {
        super.onPostResume()

        changeMiniPlayerVisibility()

        if (listTypeChanged) {
            listTypeChanged = false

            replaceRadioListFragment(listType, true)
        }
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
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_preferences -> {
                val intent = Intent(this, PreferencesActivity::class.java)
                startActivityForResult(intent, REQUEST_CODE_PREFERENCES)

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PREFERENCES) {
            val newListType = sharedPreferences.getString(PREFERENCE_KEY_LIST_TYPE, PREFERENCE_VALUE_LIST_TYPE_GRID)

            if (newListType != listType) {
                listType = newListType

                listTypeChanged = true
            }

            return
        }

        super.onActivityResult(requestCode, resultCode, data)
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

        when (mediaController.playbackState.state) {
            STATE_ERROR, STATE_NONE, STATE_STOPPED -> return false
            else -> return true
        }
    }

    private fun showMiniPlayer() {
        containerMiniPlayer.visibility = VISIBLE

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

    private fun replaceRadioListFragment(listType: String, animate: Boolean) {
        radioListFragment = RadioListFragment.newInstance(listType)

        val transaction = supportFragmentManager.beginTransaction()

        if (animate) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        transaction.replace(R.id.container_radio_list, radioListFragment, RadioListFragment.TAG)
                .commitNow()

        if (!query.isNullOrBlank()) {
            radioListFragment.searchRadios(query!!)
        }
    }
}
