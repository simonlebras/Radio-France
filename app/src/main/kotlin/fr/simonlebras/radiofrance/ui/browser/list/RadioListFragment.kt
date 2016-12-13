package fr.simonlebras.radiofrance.ui.browser.list

import android.content.Context
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
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
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioBrowserComponent
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioListComponent
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioListModule
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_radio_list.*
import kotlinx.android.synthetic.main.fragment_radio_list.view.*
import javax.inject.Inject

class RadioListFragment : BaseFragment<RadioListPresenter>(), RadioListPresenter.View {
    override val isSearching: Boolean
        get() = callback?.isSearching ?: false

    override val currentQuery: String
        get() = callback?.currentQuery ?: ""

    @Inject lateinit var adapter: RadioListAdapter

    private val component: RadioListComponent by lazy(LazyThreadSafetyMode.NONE) {
        (baseCallback!!.component as RadioBrowserComponent)
                .plus(RadioListModule(this))
    }
    private var callback: Callback? = null
    private var snackBar: Snackbar? = null
    private val updateSubject = PublishSubject.create <RadioListDiffCallback>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as Callback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_radio_list, container, false)

        component.inject(this)

        view.recycler_view.adapter = adapter
        view.recycler_view.itemAnimator = DefaultItemAnimator()
        view.recycler_view.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(context)
        view.recycler_view.layoutManager = layoutManager

        val width = resources.getDimensionPixelSize(R.dimen.list_divider_width).toFloat()
        val decoration = DividerItemDecoration(ContextCompat.getColor(context, R.color.colorDivider), width)
        view.recycler_view.addItemDecoration(decoration)

        view.button_list_refresh.setOnClickListener {
            snackBar?.dismiss()

            showProgressBar()

            presenter.refresh()
        }

        subscribeToUpdateEvents()

        return view
    }

    override fun onStart() {
        super.onStart()

        presenter.onAttachView(this)
        presenter.connect()
    }

    override fun onDetach() {
        callback = null

        super.onDetach()
    }

    override fun restorePresenter() {
        val presenterManager = (activity as BaseActivity<*>).presenterManager
        presenter = presenterManager[uuid] as? RadioListPresenter ?: component.radioListPresenter()
        presenterManager[uuid] = presenter
    }

    override fun updateRadios(radios: List<Radio>) {
        updateSubject.onNext(RadioListDiffCallback(adapter.radios, radios))
    }

    override fun showRefreshError() {
        showEmptyView()

        showRetryAction()
    }

    fun searchRadios(query: String) {
        presenter.searchRadios(query)
    }

    fun showPlaybackError(error: String) {
        Snackbar.make(view as CoordinatorLayout, error, Snackbar.LENGTH_LONG)
                .show()
    }

    fun onRadioSelected(id: String) {
        presenter.play(id)
    }

    private fun subscribeToUpdateEvents() {
        compositeDisposable.add(updateSubject
                .subscribeOn(Schedulers.computation())
                .switchMap {
                    Observable.just(Pair(it.newList, DiffUtil.calculateDiff(it)))
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val newList = it.first
                    if (newList.isEmpty()) {
                        showNoResultView()

                        adapter.radios = newList
                        adapter.notifyDataSetChanged()
                    } else {
                        showRecyclerView()

                        adapter.radios = newList
                        it.second.dispatchUpdatesTo(adapter)

                        if (isSearching) {
                            recycler_view.scrollToPosition(0)
                        }
                    }
                }))
    }

    private fun showProgressBar() {
        progress_bar.visibility = View.VISIBLE
        recycler_view.visibility = View.GONE
        empty_view.visibility = View.GONE
        text_no_result.visibility = View.GONE
    }

    private fun showRecyclerView() {
        if (recycler_view.visibility != View.VISIBLE) {
            progress_bar.visibility = View.GONE
            recycler_view.visibility = View.VISIBLE
            empty_view.visibility = View.GONE
            text_no_result.visibility = View.GONE
        }
    }

    private fun showEmptyView() {
        progress_bar.visibility = View.GONE
        recycler_view.visibility = View.GONE
        empty_view.visibility = View.VISIBLE
        text_no_result.visibility = View.GONE
    }

    private fun showNoResultView() {
        progress_bar.visibility = View.GONE
        recycler_view.visibility = View.GONE
        empty_view.visibility = View.GONE
        text_no_result.visibility = View.VISIBLE
    }

    private fun showRetryAction() {
        snackBar = Snackbar.make(view as CoordinatorLayout, R.string.error_service_unavailable, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_retry) {
                    showProgressBar()

                    presenter.refresh()
                }

        snackBar!!.show()
    }

    interface Callback {
        val isSearching: Boolean

        val currentQuery: String
    }
}
