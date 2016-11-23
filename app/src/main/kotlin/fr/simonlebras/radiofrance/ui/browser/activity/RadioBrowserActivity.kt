package fr.simonlebras.radiofrance.ui.browser.activity

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.ViewGroup
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.RadioFranceApplication
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserActivityModule
import fr.simonlebras.radiofrance.ui.browser.fragment.RadioBrowserFragment
import fr.simonlebras.radiofrance.ui.browser.manager.MediaControllerCompatWrapper
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_radio_browser.*
import kotlinx.android.synthetic.main.partial_toolbar.*
import java.util.concurrent.TimeUnit

class RadioBrowserActivity : BaseActivity<RadioBrowserActivityPresenter>(), RadioBrowserActivityPresenter.View, RadioBrowserFragment.Listener {
    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    override val component by lazy(LazyThreadSafetyMode.NONE) {
        (application as RadioFranceApplication).component
                .plus(RadioBrowserActivityModule(this))
    }

    private lateinit var radioBrowseFragment: RadioBrowserFragment
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_radio_browser)
        setSupportActionBar(toolbar)
        radioBrowseFragment = supportFragmentManager.findFragmentById(R.id.fragment_radio_browser) as RadioBrowserFragment

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_radio_browser, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = MenuItemCompat.getActionView(menu.findItem(R.id.action_search)) as SearchView
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        subscribeToSearchView(searchView!!)

        return true
    }

    override fun provideParentView(): ViewGroup = container

    override fun restorePresenter() {
        presenter = presenterManager[uuid] as? RadioBrowserActivityPresenter ?: component.radioBrowserPresenter()
        presenterManager[uuid] = presenter
    }

    override fun setMediaController(mediaControllerWrapper: MediaControllerCompatWrapper) {
        supportMediaController = mediaControllerWrapper.mediaController
    }

    override fun isSearching(): Boolean {
        if (searchView == null) {
            return false
        }

        if (searchView!!.isIconified) {
            return false
        }

        return true
    }

    override fun getCurrentQuery() = searchView?.query?.toString()

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
                    radioBrowseFragment.searchRadios(it)
                }))
    }
}
