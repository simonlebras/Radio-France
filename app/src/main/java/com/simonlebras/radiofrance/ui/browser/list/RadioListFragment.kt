package com.simonlebras.radiofrance.ui.browser.list

import android.app.SearchManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.google.android.gms.cast.framework.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.data.model.Radio
import com.simonlebras.radiofrance.ui.base.BaseActivity
import com.simonlebras.radiofrance.ui.base.BaseFragment
import dagger.Lazy
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_radio_list.*
import kotlinx.android.synthetic.main.partial_toolbar.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class RadioListFragment : BaseFragment<RadioListPresenter>(), RadioListPresenter.View {
    companion object {
        val TAG: String = RadioListFragment::class.java.simpleName

        const val BUNDLE_QUERY = "BUNDLE_QUERY"

        fun newInstance() = RadioListFragment()
    }

    @Inject
    lateinit var presenterProvider: Lazy<RadioListPresenter>

    val preloadSizeProvider = ViewPreloadSizeProvider<Radio>()

    private lateinit var adapter: RadioListAdapter

    private var snackBar: Snackbar? = null

    private var query: String? = null

    private var mediaRouteMenuItem: MenuItem? = null

    private var castContext: CastContext? = null
    private val castStateListener = CastStateListener {
        if (it != CastState.NO_DEVICES_AVAILABLE) {
            showCastOverlay()
        }
    }
    private var introductoryOverlay: IntroductoryOverlay? = null
    private val handler = Handler()
    private val handlerCallbacks = Runnable {
        introductoryOverlay = IntroductoryOverlay.Builder(activity, mediaRouteMenuItem)
                .setTitleText(R.string.touch_to_cast)
                .setSingleTime()
                .setOnOverlayDismissedListener {
                    introductoryOverlay = null
                }
                .build()

        introductoryOverlay!!.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            castContext = CastContext.getSharedInstance(context!!)
        }

        setHasOptionsMenu(true)

        query = savedInstanceState?.getString(BUNDLE_QUERY, null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_radio_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        adapter = RadioListAdapter(this)

        recycler_view.let {
            it.adapter = adapter
            it.itemAnimator = DefaultItemAnimator()
            it.setHasFixedSize(true)

            it.layoutManager = LinearLayoutManager(context)

            val preloader = RecyclerViewPreloader(this, adapter, preloadSizeProvider, 4)
            it.addOnScrollListener(preloader)

            val width = resources.getDimensionPixelSize(R.dimen.list_divider_width)
                    .toFloat()
            val decoration = DividerItemDecoration(ContextCompat.getColor(context!!, R.color.colorDivider), width)
            it.addItemDecoration(decoration)
        }

        button_list_refresh.setOnClickListener {
            snackBar?.dismiss()

            showProgressBar()

            presenter.refresh()
        }

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.activity_radio_browser, menu)

        if (castContext != null) {
            mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(context!!.applicationContext, menu, R.id.action_media_route)
        }

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        val searchManager = ContextCompat.getSystemService(context!!, SearchManager::class.java)!!
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))

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

                                                       searchRadios(it)
                                                   }))
    }

    override fun onResume() {
        super.onResume()

        castContext?.addCastStateListener(castStateListener)
    }

    override fun onPause() {
        castContext?.removeCastStateListener(castStateListener)

        handler.removeCallbacks(handlerCallbacks)

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(BUNDLE_QUERY, query)

        super.onSaveInstanceState(outState)
    }

    override fun restorePresenter() {
        val presenterManager = (activity as BaseActivity<*>).presenterManager
        presenter = presenterManager[uuid] as? RadioListPresenter ?: presenterProvider.get()
        presenterManager[uuid] = presenter
    }

    override fun onConnected(mediaController: MediaControllerCompat) {
        updateToolbarTitle(mediaController.metadata?.description?.title?.toString())

        presenter.subscribeToPlaybackUpdates()

        presenter.subscribeToRefreshAndSearchEvents()
        presenter.refresh()
    }

    override fun showRadios(radios: List<Radio>) {
        showRecyclerView()

        adapter.submitList(radios)

        recycler_view.scrollToPosition(0)
    }

    override fun showRefreshError() {
        showEmptyView()

        showRetryAction()
    }

    override fun showSearchError() {
        showNoResultView()

        adapter.submitList(emptyList())
    }

    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        if (playbackState.state == STATE_ERROR) {
            Snackbar.make(view!!, playbackState.errorMessage, Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        updateToolbarTitle(metadata.description?.title?.toString())
    }

    fun searchRadios(query: String) {
        presenter.searchRadios(query)
    }

    fun onRadioSelected(id: String) {
        presenter.play(id)
    }

    private fun showProgressBar() {
        progress_bar.visibility = VISIBLE
        recycler_view.visibility = GONE
        empty_view.visibility = GONE
        text_no_result.visibility = GONE
    }

    private fun showRecyclerView() {
        if (recycler_view.visibility != VISIBLE) {
            progress_bar.visibility = GONE
            recycler_view.visibility = VISIBLE
            empty_view.visibility = GONE
            text_no_result.visibility = GONE
        }
    }

    private fun showEmptyView() {
        progress_bar.visibility = GONE
        recycler_view.visibility = GONE
        empty_view.visibility = VISIBLE
        text_no_result.visibility = GONE
    }

    private fun showNoResultView() {
        progress_bar.visibility = GONE
        recycler_view.visibility = GONE
        empty_view.visibility = GONE
        text_no_result.visibility = VISIBLE
    }

    private fun showRetryAction() {
        snackBar = Snackbar.make(view!!, R.string.error_service_unavailable, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry) {
                    showProgressBar()

                    presenter.refresh()
                }

        snackBar!!.show()
    }

    private fun showCastOverlay() {
        introductoryOverlay?.remove()

        if (mediaRouteMenuItem?.isVisible == true) {
            handler.post(handlerCallbacks)
        }
    }

    private fun updateToolbarTitle(title: String?) {
        toolbar.title = title ?: getString(R.string.label_radios)
    }
}
