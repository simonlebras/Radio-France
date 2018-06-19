package com.simonlebras.radiofrance.ui.browser.list

import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.FixedPreloadSizeProvider
import com.google.android.gms.cast.framework.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.data.models.Status
import com.simonlebras.radiofrance.databinding.FragmentRadioListBinding
import com.simonlebras.radiofrance.ui.MainViewModel
import com.simonlebras.radiofrance.utils.AppSchedulers
import dagger.android.support.DaggerFragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.DisposableSubscriber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RadioListFragment : DaggerFragment() {
    companion object {
        const val BUNDLE_QUERY = "BUNDLE_QUERY"
    }

    @Inject
    lateinit var appSchedulers: AppSchedulers

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentRadioListBinding

    lateinit var viewModel: MainViewModel

    private lateinit var adapter: RadioListAdapter

    private val compositeDisposable = CompositeDisposable()

    private val radioSubject = PublishProcessor.create<List<Radio>>()

    private lateinit var searchView: SearchView
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
            castContext = CastContext.getSharedInstance(requireContext())
        }

        setHasOptionsMenu(true)

        query = savedInstanceState?.getString(BUNDLE_QUERY, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRadioListBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).setSupportActionBar(binding.partialToolbar.toolbar)

        adapter = RadioListAdapter(this)

        binding.recyclerView.let {
            it.adapter = adapter
            it.itemAnimator = DefaultItemAnimator()
            it.setHasFixedSize(true)

            it.layoutManager = LinearLayoutManager(context)

            val imageSize = resources.getDimensionPixelSize(R.dimen.list_item_image_size)
            val preloadSizeProvider = FixedPreloadSizeProvider<Radio>(imageSize, imageSize)
            val preloader = RecyclerViewPreloader(this, adapter, preloadSizeProvider, 5)
            it.addOnScrollListener(preloader)

            val width = resources.getDimensionPixelSize(R.dimen.list_divider_width)
                .toFloat()
            val decoration = DividerItemDecoration(
                ContextCompat.getColor(requireContext(), R.color.colorDivider),
                width
            )
            it.addItemDecoration(decoration)
        }

        binding.buttonListRefresh.setOnClickListener {
            showProgressBar()

            viewModel.retryLoadRadios()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
            .get(MainViewModel::class.java)

        viewModel.connect()

        viewModel.connection.observe(this, Observer {
            viewModel.loadRadios()
        })

        viewModel.playbackState.observe(
            viewLifecycleOwner,
            Observer { onPlaybackStateChanged(it!!) })
        viewModel.metadata.observe(viewLifecycleOwner, Observer { onMetadataChanged(it!!) })

        val initialValue = Pair<List<Radio>, DiffUtil.DiffResult?>(emptyList(), null)
        val disposable = radioSubject
            .scan(initialValue) { pair, next ->
                Pair(
                    next,
                    DiffUtil.calculateDiff(DiffUtilCallback(pair.first, next))
                )
            }
            .skip(1)
            .subscribeOn(appSchedulers.computation)
            .observeOn(appSchedulers.main)
            .subscribeWith(object :
                               DisposableSubscriber<Pair<List<Radio>, DiffUtil.DiffResult?>>() {
                override fun onComplete() {}

                override fun onNext(pair: Pair<List<Radio>, DiffUtil.DiffResult?>) {
                    val (radios, diffResult) = pair

                    if (radios.isEmpty()) {
                        showNoResultView()
                    } else {
                        showRecyclerView()
                    }

                    adapter.radios = radios
                    diffResult!!.dispatchUpdatesTo(adapter)

                    binding.recyclerView.scrollToPosition(0)
                }

                override fun onError(e: Throwable) {}
            })

        compositeDisposable.add(disposable)

        viewModel.radios.observe(viewLifecycleOwner, Observer {
            when (it!!.status) {
                Status.LOADING -> showProgressBar()
                Status.SUCCESS -> radioSubject.offer(it.data)
                Status.ERROR -> showEmptyView()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.fragment_radio_list, menu)

        if (castContext != null) {
            mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
                requireContext().applicationContext,
                menu,
                R.id.action_media_route
            )
        }

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView
        val searchManager =
            ContextCompat.getSystemService(requireContext(), SearchManager::class.java)!!
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

        if (query != null) {
            searchItem.expandActionView()
            searchView.setQuery(query, false)
            searchView.clearFocus()
        }

        val disposable = searchView.queryTextChanges()
            .skipInitialValue()
            .throttleLast(100, TimeUnit.MILLISECONDS, appSchedulers.computation)
            .debounce(200, TimeUnit.MILLISECONDS, appSchedulers.computation)
            .map(CharSequence::toString)
            .map(String::trim)
            .distinctUntilChanged()
            .observeOn(appSchedulers.main)
            .subscribeWith(object : DisposableObserver<String>() {
                override fun onComplete() {
                }

                override fun onNext(query: String) {
                    viewModel.searchRadios(query)
                }

                override fun onError(e: Throwable) {
                }
            })

        compositeDisposable.add(disposable)
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
        outState.putString(
            BUNDLE_QUERY,
            if (searchView.isIconified) null else searchView.query.toString()
        )

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        compositeDisposable.clear()

        super.onDestroyView()
    }

    private fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        if (playbackState.state == STATE_ERROR) {
            Snackbar.make(view!!, playbackState.errorMessage, Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun onMetadataChanged(metadata: MediaMetadataCompat) {
        updateToolbarTitle(metadata.description?.title?.toString())
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = VISIBLE
        binding.recyclerView.visibility = GONE
        binding.emptyView.visibility = GONE
        binding.textNoResult.visibility = GONE
    }

    private fun showRecyclerView() {
        if (binding.recyclerView.visibility != VISIBLE) {
            binding.progressBar.visibility = GONE
            binding.recyclerView.visibility = VISIBLE
            binding.emptyView.visibility = GONE
            binding.textNoResult.visibility = GONE
        }
    }

    private fun showEmptyView() {
        binding.progressBar.visibility = GONE
        binding.recyclerView.visibility = GONE
        binding.emptyView.visibility = VISIBLE
        binding.textNoResult.visibility = GONE
    }

    private fun showNoResultView() {
        binding.progressBar.visibility = GONE
        binding.recyclerView.visibility = GONE
        binding.emptyView.visibility = GONE
        binding.textNoResult.visibility = VISIBLE
    }

    private fun showCastOverlay() {
        introductoryOverlay?.remove()

        if (mediaRouteMenuItem?.isVisible == true) {
            handler.post(handlerCallbacks)
        }
    }

    private fun updateToolbarTitle(title: String?) {
        binding.partialToolbar.toolbar.title = title ?: getString(R.string.label_radios)
    }
}
