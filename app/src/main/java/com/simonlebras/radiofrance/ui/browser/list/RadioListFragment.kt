package com.simonlebras.radiofrance.ui.browser.list

import android.app.SearchManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.view.*
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.FixedPreloadSizeProvider
import com.google.android.gms.cast.framework.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.databinding.FragmentRadioListBinding
import com.simonlebras.radiofrance.ui.MainViewModel
import com.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import com.simonlebras.radiofrance.ui.utils.observeK
import com.simonlebras.radiofrance.ui.utils.scan
import com.simonlebras.radiofrance.ui.utils.withViewModel
import com.simonlebras.radiofrance.utils.AppContexts
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.drop
import kotlinx.coroutines.experimental.withContext
import javax.inject.Inject

private const val BUNDLE_QUERY = "BUNDLE_QUERY"

class RadioListFragment : DaggerFragment() {
    companion object {
        fun newInstance() = RadioListFragment()
    }

    @Inject
    lateinit var appContexts: AppContexts

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentRadioListBinding

    private lateinit var miniPlayerFragment: MiniPlayerFragment

    lateinit var viewModel: MainViewModel

    private lateinit var adapter: RadioListAdapter

    private lateinit var searchView: SearchView
    private var query: String? = null

    private var menuSet = false

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

    private val parentJob = Job()

    private lateinit var updateActor: SendChannel<List<Radio>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            castContext = CastContext.getSharedInstance(requireContext())
        }

        query = savedInstanceState?.getString(BUNDLE_QUERY, null)

        updateActor = actor(context = appContexts.computation, parent = parentJob) {
            channel
                    .scan(Pair<List<Radio>, DiffUtil.DiffResult?>(emptyList(), null)) { pair, next ->
                        Pair(
                                next,
                                DiffUtil.calculateDiff(DiffUtilCallback(pair.first, next))
                        )
                    }
                    .drop(1)
                    .consumeEach {
                        val (radios, diffResult) = it

                        withContext(appContexts.main) {
                            binding.radios = radios

                            if (radios.isEmpty()) {
                                adapter.radios = radios
                                adapter.notifyDataSetChanged()
                            } else {
                                adapter.radios = radios

                                diffResult!!.dispatchUpdatesTo(adapter)

                                binding.recyclerView.scrollToPosition(0)
                            }
                        }
                    }
        }
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
            viewModel.retryLoadRadios()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        miniPlayerFragment =
                childFragmentManager.findFragmentById(R.id.fragment_mini_player) as MiniPlayerFragment

        viewModel = requireActivity().withViewModel(viewModelFactory) {
            connect()

            connection.observeK(viewLifecycleOwner) {
                loadRadios()
            }

            playbackState.observeK(viewLifecycleOwner) {
                onPlaybackStateChanged(it!!)
                toggleMiniPlayerVisibility(it)
            }

            metadata.observeK(viewLifecycleOwner) {
                onMetadataChanged(it!!)
            }

            status.observeK(viewLifecycleOwner) {
                binding.status = it
            }

            radios.observeK(viewLifecycleOwner) {
                updateActor.offer(it!!)

                if (!menuSet) {
                    menuSet = true

                    setHasOptionsMenu(true)
                }
            }
        }
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
        val searchManager = requireContext().getSystemService<SearchManager>()!!
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

        if (query != null) {
            searchItem.expandActionView()
            searchView.setQuery(query, false)
            searchView.clearFocus()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String): Boolean {
                viewModel.searchRadios(query)

                return true
            }

            override fun onQueryTextSubmit(query: String) = false
        })
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
                if (!menuSet || searchView.isIconified) null else searchView.query.toString()
        )

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        parentJob.cancel()

        super.onDestroy()
    }

    private fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        if (playbackState.state == STATE_ERROR) {
            Snackbar.make(view!!, playbackState.errorMessage, Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    private fun onMetadataChanged(metadata: MediaMetadataCompat) {
        val title = metadata.description?.title?.toString() ?: getString(R.string.label_radios)
        binding.partialToolbar.toolbar.title = title
    }

    private fun showCastOverlay() {
        introductoryOverlay?.remove()

        if (mediaRouteMenuItem?.isVisible == true) {
            handler.post(handlerCallbacks)
        }
    }

    private fun toggleMiniPlayerVisibility(playbackState: PlaybackStateCompat) {
        if (shouldShowMiniPlayer(playbackState)) {
            showMiniPlayer()
        } else {
            hideMiniPlayer()
        }
    }

    private fun shouldShowMiniPlayer(playbackState: PlaybackStateCompat) =
            playbackState.state != PlaybackStateCompat.STATE_NONE

    private fun showMiniPlayer() {
        binding.containerMiniPlayer.visibility = VISIBLE

        childFragmentManager.beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_bottom,
                        R.anim.slide_out_bottom,
                        R.anim.slide_in_top,
                        R.anim.slide_out_top
                )
                .show(miniPlayerFragment)
                .commitNowAllowingStateLoss()
    }

    private fun hideMiniPlayer() {
        childFragmentManager.beginTransaction()
                .hide(miniPlayerFragment)
                .commitNowAllowingStateLoss()
    }
}
