package fr.simonlebras.radiofrance.ui.base

import io.reactivex.disposables.CompositeDisposable

abstract class BasePresenter<V : BaseView> {
    protected var view: V? = null

    protected lateinit var compositeDisposable: CompositeDisposable

    fun onAttachView(v: V) {
        compositeDisposable = CompositeDisposable()

        view = v
    }

    open fun onDetachView() {
        compositeDisposable.clear()

        view = null
    }

    open fun onDestroy() {
    }
}
