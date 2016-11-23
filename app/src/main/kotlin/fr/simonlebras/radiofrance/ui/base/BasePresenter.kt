package fr.simonlebras.radiofrance.ui.base

import io.reactivex.disposables.CompositeDisposable

abstract class BasePresenter<V : BaseView>() {
    protected var view: V? = null
    protected var compositeDisposable = CompositeDisposable()

    fun onAttachView(v: V) {
        view = v
    }

    fun onDetachView() {
        this.view = null
    }

    open fun onDestroy() {
        compositeDisposable.clear()
    }
}
