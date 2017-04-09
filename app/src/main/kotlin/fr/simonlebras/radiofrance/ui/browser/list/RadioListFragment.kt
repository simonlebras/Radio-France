package fr.simonlebras.radiofrance.ui.browser.list

import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import dagger.Lazy
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.base.BaseFragment
import fr.simonlebras.radiofrance.ui.preferences.PreferencesFragment.Companion.PREFERENCE_VALUE_LIST_TYPE_GRID
import fr.simonlebras.radiofrance.ui.preferences.PreferencesFragment.Companion.PREFERENCE_VALUE_LIST_TYPE_LIST
import javax.inject.Inject

class RadioListFragment : BaseFragment<RadioListPresenter>(), RadioListPresenter.View {
    companion object {
        val TAG: String = RadioListFragment::class.java.simpleName

        const val ARGUMENT_LIST_TYPE = "ARGUMENT_LIST_TYPE"

        fun newInstance(listType: String): RadioListFragment {
            if (listType != PREFERENCE_VALUE_LIST_TYPE_GRID && listType != PREFERENCE_VALUE_LIST_TYPE_LIST) {
                throw IllegalArgumentException("Illegal list type")
            }

            val arguments = Bundle()
            arguments.putString(ARGUMENT_LIST_TYPE, listType)

            val fragment = RadioListFragment()
            fragment.arguments = arguments

            return fragment
        }
    }

    @Inject lateinit var presenterProvider: Lazy<RadioListPresenter>

    @BindView(R.id.progress_bar) lateinit var progressBar: ProgressBar
    @BindView(R.id.recycler_view) lateinit var recyclerView: RecyclerView
    @BindView(R.id.empty_view) lateinit var emptyView: View
    @BindView(R.id.button_list_refresh) lateinit var buttonListRefresh: Button
    @BindView(R.id.text_no_result) lateinit var textNoResult: TextView

    private lateinit var unbinder: Unbinder

    private lateinit var adapter: RadioListAdapter
    private lateinit var listType: String

    private var snackBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listType = arguments?.getString(ARGUMENT_LIST_TYPE) as? String ?: PREFERENCE_VALUE_LIST_TYPE_GRID
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_radio_list, container, false)

        unbinder = ButterKnife.bind(this, view)

        adapter = RadioListAdapter(this, listType)

        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.setHasFixedSize(true)

        val resources = resources
        if (listType == PREFERENCE_VALUE_LIST_TYPE_GRID) {
            recyclerView.setPadding(0, 0, 0, 0)

            val columnCount = resources.getInteger(R.integer.grid_column_count)
            val columnSpace = resources.getDimensionPixelSize(R.dimen.grid_column_space)

            recyclerView.layoutManager = GridLayoutManager(context, columnCount)

            recyclerView.addItemDecoration(SpaceItemDecoration(columnSpace))
        } else {
            recyclerView.layoutManager = LinearLayoutManager(context)

            val width = resources.getDimensionPixelSize(R.dimen.list_divider_width).toFloat()
            val decoration = DividerItemDecoration(ContextCompat.getColor(context, R.color.colorDivider), width)
            recyclerView.addItemDecoration(decoration)
        }

        buttonListRefresh.setOnClickListener {
            snackBar?.dismiss()

            showProgressBar()

            presenter.refresh()
        }

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onDestroyView() {
        unbinder.unbind()

        super.onDestroyView()
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
        val diffResult = DiffUtil.calculateDiff(RadioListDiffCallback(adapter.radios, radios))

        showRecyclerView()

        adapter.radios = radios
        diffResult.dispatchUpdatesTo(adapter)

        recyclerView.scrollToPosition(0)
    }

    override fun showRefreshError() {
        showEmptyView()

        showRetryAction()
    }

    override fun showSearchError() {
        showNoResultView()

        adapter.radios = emptyList()
        adapter.notifyDataSetChanged()
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
        progressBar.visibility = VISIBLE
        recyclerView.visibility = GONE
        emptyView.visibility = GONE
        textNoResult.visibility = GONE
    }

    private fun showRecyclerView() {
        if (recyclerView.visibility != VISIBLE) {
            progressBar.visibility = GONE
            recyclerView.visibility = VISIBLE
            emptyView.visibility = GONE
            textNoResult.visibility = GONE
        }
    }

    private fun showEmptyView() {
        progressBar.visibility = GONE
        recyclerView.visibility = GONE
        emptyView.visibility = VISIBLE
        textNoResult.visibility = GONE
    }

    private fun showNoResultView() {
        progressBar.visibility = GONE
        recyclerView.visibility = GONE
        emptyView.visibility = GONE
        textNoResult.visibility = VISIBLE
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
