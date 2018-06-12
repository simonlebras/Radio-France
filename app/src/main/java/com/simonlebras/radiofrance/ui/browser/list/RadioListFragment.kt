package com.simonlebras.radiofrance.ui.browser.list

import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.models.Radio
import com.simonlebras.radiofrance.ui.base.BaseActivity
import com.simonlebras.radiofrance.ui.base.BaseFragment
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_radio_list.*
import kotlinx.android.synthetic.main.fragment_radio_list.view.*
import javax.inject.Inject


class RadioListFragment : BaseFragment<RadioListPresenter>(), RadioListPresenter.View {
    companion object {
        val TAG: String = RadioListFragment::class.java.simpleName

        fun newInstance() = RadioListFragment()
    }

    @Inject
    lateinit var presenterProvider: Lazy<RadioListPresenter>

    val preloadSizeProvider = ViewPreloadSizeProvider<Radio>()

    private lateinit var adapter: RadioListAdapter

    private var snackBar: Snackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_radio_list, container, false)

        adapter = RadioListAdapter(this)

        view.recycler_view.let {
            it.adapter = adapter
            it.itemAnimator = DefaultItemAnimator()
            it.setHasFixedSize(true)

            it.layoutManager = LinearLayoutManager(context)

            val preloader = RecyclerViewPreloader(this, adapter, preloadSizeProvider, 4)
            it.addOnScrollListener(preloader)
        }

        val width = resources.getDimensionPixelSize(R.dimen.list_divider_width)
                .toFloat()
        val decoration = DividerItemDecoration(ContextCompat.getColor(context!!, R.color.colorDivider), width)
        view.recycler_view.addItemDecoration(decoration)

        view.button_list_refresh.setOnClickListener {
            snackBar?.dismiss()

            showProgressBar()

            presenter.refresh()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun restorePresenter() {
        val presenterManager = (activity as BaseActivity<*>).presenterManager
        presenter = presenterManager[uuid] as? RadioListPresenter ?: presenterProvider.get()
        presenterManager[uuid] = presenter
    }

    override fun onConnected() {
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
            Snackbar.make(view as CoordinatorLayout, playbackState.errorMessage, Snackbar.LENGTH_LONG)
                    .show()
        }
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
        snackBar = Snackbar.make(view as CoordinatorLayout, R.string.error_service_unavailable, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry) {
                    showProgressBar()

                    presenter.refresh()
                }

        snackBar!!.show()
    }
}
