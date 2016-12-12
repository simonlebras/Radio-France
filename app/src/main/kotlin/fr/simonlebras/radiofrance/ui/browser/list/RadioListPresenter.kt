package fr.simonlebras.radiofrance.ui.browser.list

import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.ui.base.BasePresenter
import fr.simonlebras.radiofrance.ui.base.BaseView
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

@FragmentScope
class RadioListPresenter @Inject constructor(val radioManager: RadioManager) : BasePresenter<RadioListPresenter.View>() {
    private var refreshSubject = PublishSubject.create<Boolean>()
    private var searchSubject = PublishSubject.create<String>()

    override fun onDetachView() {
        compositeDisposable.clear()

        super.onDetachView()
    }

    fun connect() {
        compositeDisposable.add(radioManager.connection
                .subscribe {
                    subscribeToRefreshEvents()
                    refresh()
                })
    }

    fun refresh() {
        refreshSubject.onNext(true)
    }

    fun searchRadios(query: String) {
        searchSubject.onNext(query)
    }

    fun play(id: String) {
        radioManager.play(id)
    }

    private fun subscribeToRefreshEvents() {
        compositeDisposable.add(refreshSubject
                .switchMap {
                    radioManager.radios
                            .onErrorResumeNext(Observable.create {
                                it.onNext(emptyList())
                            })
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (it.isEmpty()) {
                        view?.showRefreshError()
                        return@subscribe
                    }

                    subscribeToSearchEvents()

                    val query = view?.currentQuery ?: ""
                    if (!query.isNullOrEmpty()) {
                        searchRadios(query)
                        return@subscribe
                    }

                    view?.updateRadios(it)
                })
    }

    private fun subscribeToSearchEvents() {
        compositeDisposable.add(searchSubject
                .switchMap {
                    val query = it
                    radioManager.radios
                            .subscribeOn(Schedulers.computation())
                            .map {
                                it.filter {
                                    it.name.toUpperCase().contains(query.toUpperCase())
                                }
                            }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view?.updateRadios(it)
                })
    }

    interface View : BaseView {
        val isSearching: Boolean

        val currentQuery: String

        fun updateRadios(radios: List<Radio>)

        fun showRefreshError()
    }
}
