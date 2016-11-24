package fr.simonlebras.radiofrance.ui.browser.fragment

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.base.BaseFragment
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioBrowserActivityComponent
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioBrowserFragmentComponent
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserFragmentModule
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_radio_browser.*
import kotlinx.android.synthetic.main.fragment_radio_browser.view.*
import javax.inject.Inject

class RadioBrowserFragment : BaseFragment<RadioBrowserFragmentPresenter>(), RadioBrowserFragmentPresenter.View {
    override val isSearching: Boolean
        get() = listener?.isSearching ?: false

    override val currentQuery: String
        get() = listener?.currentQuery ?: ""

    @Inject lateinit var adapter: RadioBrowserAdapter

    private val component: RadioBrowserFragmentComponent by lazy(LazyThreadSafetyMode.NONE) {
        (baseListener!!.component as RadioBrowserActivityComponent)
                .plus(RadioBrowserFragmentModule(this))
    }
    private var listener: Listener? = null
    private var snackBar: Snackbar? = null
    private val updateSubject = PublishSubject.create <RadioListDiffCallback>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = context as Listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_radio_browser, container, false)

        component.inject(this)

        view.swipe_refresh_layout.setColorSchemeResources(R.color.colorAccent)
        subscribeToSwipeRefreshLayout(view.swipe_refresh_layout)

        view.recycler_view.adapter = adapter
        view.recycler_view.itemAnimator = DefaultItemAnimator()
        view.recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        view.recycler_view.layoutManager = layoutManager

        val width = resources.getDimensionPixelSize(R.dimen.list_divider_width).toFloat()
        val decoration = DividerItemDecoration(ContextCompat.getColor(context, R.color.colorDivider), width)
        view.recycler_view.addItemDecoration(decoration)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        subscribeToUpdateEvents()

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onDetach() {
        listener = null

        super.onDetach()
    }

    override fun restorePresenter() {
        val presenterManager = (activity as BaseActivity<*>).presenterManager
        presenter = presenterManager[uuid] as? RadioBrowserFragmentPresenter ?: component.radioBrowserPresenter()
        presenterManager[uuid] = presenter
    }

    override fun updateRadios(radios: List<Radio>) {
        updateSubject.onNext(RadioListDiffCallback(adapter.radios, radios))
    }

    override fun showRefreshError() {
        swipe_refresh_layout.isRefreshing = false

        if (swipe_refresh_layout.visibility != View.VISIBLE) {
            if (!isSearching) {
                showEmptyView()
            } else {
                showNoResultView()
            }

            showRetryAction(true)
        } else {
            showRetryAction(false)
        }
    }

    fun searchRadios(query: String) {
        snackBar?.dismiss()

        presenter.searchRadios(query)
    }

    private fun subscribeToSwipeRefreshLayout(swipeRefreshLayout: SwipeRefreshLayout) {
        compositeDisposable.add(Observable
                .create<Boolean> {
                    val listener = SwipeRefreshLayout.OnRefreshListener {
                        snackBar?.dismiss()
                        snackBar = null

                        if (!it.isDisposed) {
                            it.onNext(true)
                        }
                    }

                    it.setCancellable {
                        swipeRefreshLayout.setOnRefreshListener(null)
                    }

                    swipeRefreshLayout.setOnRefreshListener(listener)
                }
                .subscribe {
                    presenter.refresh()
                })
    }

    private fun subscribeToUpdateEvents() {
        compositeDisposable.add(updateSubject
                .subscribeOn(Schedulers.computation())
                .switchMap {
                    Observable.just(Pair(it.newList, DiffUtil.calculateDiff(it))
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    swipe_refresh_layout.isRefreshing = false

                    val newList = it.first
                    if (newList.isEmpty()) {
                        if (isSearching) {
                            showNoResultView()

                            updateAdapter(newList, it.second)
                        } else if (swipe_refresh_layout.visibility != View.VISIBLE) {
                            showEmptyView()
                            showRetryAction(true)

                            updateAdapter(newList, it.second)
                        } else {
                            showRetryAction(false)
                        }
                    } else {
                        showSwipeRefreshLayout()

                        updateAdapter(newList, it.second)

                        if (isSearching) {
                            recycler_view.scrollToPosition(0)
                        }
                    }
                }))
    }

    private fun showProgressBar() {
        progress_bar.visibility = View.VISIBLE
        swipe_refresh_layout.visibility = View.GONE
        empty_view.visibility = View.GONE
        no_result_view.visibility = View.GONE
    }

    private fun showSwipeRefreshLayout() {
        progress_bar.visibility = View.GONE
        swipe_refresh_layout.visibility = View.VISIBLE
        empty_view.visibility = View.GONE
        no_result_view.visibility = View.GONE
    }

    private fun showEmptyView() {
        progress_bar.visibility = View.GONE
        swipe_refresh_layout.visibility = View.GONE
        empty_view.visibility = View.VISIBLE
        no_result_view.visibility = View.GONE
    }

    private fun showNoResultView() {
        progress_bar.visibility = View.GONE
        swipe_refresh_layout.visibility = View.GONE
        empty_view.visibility = View.GONE
        no_result_view.visibility = View.VISIBLE
    }

    private fun showRetryAction(permanent: Boolean) {
        val length: Int
        if (permanent) {
            length = Snackbar.LENGTH_INDEFINITE
        } else {
            length = Snackbar.LENGTH_LONG
        }

        snackBar = Snackbar.make(listener!!.parentView, R.string.msg_error_occurred, length)
                .setAction(R.string.action_retry) {
                    if (permanent) {
                        showProgressBar()
                    } else {
                        swipe_refresh_layout.isRefreshing = true
                    }

                    snackBar = null

                    presenter.refresh()
                }

        snackBar!!.show()
    }

    private fun updateAdapter(radios: List<Radio>, diffResult: DiffUtil.DiffResult) {
        adapter.radios = radios
        diffResult.dispatchUpdatesTo(adapter)
    }

    interface Listener {
        val parentView: ViewGroup

        val isSearching: Boolean

        val currentQuery: String
    }
}
