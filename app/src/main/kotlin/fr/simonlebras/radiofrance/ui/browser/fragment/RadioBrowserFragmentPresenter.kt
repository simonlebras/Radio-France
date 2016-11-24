package fr.simonlebras.radiofrance.ui.browser.fragment

import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.ui.base.BasePresenter
import fr.simonlebras.radiofrance.ui.base.BaseView
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

@FragmentScope
class RadioBrowserFragmentPresenter @Inject constructor(val radioManager: RadioManager) : BasePresenter<RadioBrowserFragmentPresenter.View>() {
    private var refreshSubject = PublishSubject.create<Boolean>()
    private var searchSubject = PublishSubject.create<String>()
    private var searchDisposable: Disposable? = null

    override fun onDetachView() {
        compositeDisposable.clear()

        super.onDetachView()
    }

    fun connect() {
        compositeDisposable.add(radioManager.connection
                .subscribe {
                    subscribeToRefreshEvents()
                    subscribeToSearchEvents()
                    refresh()
                })
    }

    fun refresh() {
        refreshSubject.onNext(true)
    }

    fun searchRadios(query: String) {
        searchSubject.onNext(query)
    }

    private fun subscribeToRefreshEvents() {
        compositeDisposable.add(refreshSubject
                .switchMap {
                    radioManager.radios
                            .toObservable()

                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    val disposable = searchDisposable
                    if (disposable != null) {
                        compositeDisposable.remove(disposable)
                    }

                    if (it.isNotEmpty()) {
                        radioManager.cache = it
                    }

                    subscribeToSearchEvents()
                }
                .subscribeWith(object : DisposableObserver<List<Radio>>() {
                    override fun onNext(value: List<Radio>) {
                        val query = view?.currentQuery ?: ""
                        if (!query.isNullOrEmpty()) {
                            searchRadios(query)
                            return
                        }

                        view?.updateRadios(value)
                    }

                    override fun onError(e: Throwable) {
                        view?.showRefreshError()
                    }

                    override fun onComplete() {
                    }
                }))
    }

    private fun subscribeToSearchEvents() {
        searchDisposable = searchSubject
                .switchMap {
                    val cache = radioManager.cache
                    if (cache != null && cache.isNotEmpty()) {
                        val query = it
                        Observable.just(cache)
                                .subscribeOn(Schedulers.computation())
                                .map {
                                    it.filter {
                                        it.name.toUpperCase().contains(query.toUpperCase())
                                    }
                                }
                    } else {
                        Observable.fromIterable(emptyList<List<Radio>>())
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view?.updateRadios(it)
                }

        compositeDisposable.add(searchDisposable)
    }

    interface View : BaseView {
        val isSearching: Boolean

        val currentQuery: String

        fun updateRadios(radios: List<Radio>)

        fun showRefreshError()
    }
}
