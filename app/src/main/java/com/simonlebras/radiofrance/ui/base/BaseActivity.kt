package com.simonlebras.radiofrance.ui.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import dagger.android.AndroidInjection
import io.reactivex.disposables.CompositeDisposable
import java.util.*

abstract class BaseActivity<T : BasePresenter<out BaseView>> : AppCompatActivity(), BaseFragment.BaseCallback {
    private companion object {
        const val BUNDLE_UUID = "BUNDLE_UUID"
    }

    override lateinit var presenterManager: PresenterManager

    protected lateinit var presenter: T
    protected lateinit var uuid: UUID

    protected val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        restorePresenterManager()

        uuid = savedInstanceState?.get(BUNDLE_UUID) as? UUID ?: UUID.randomUUID()
        restorePresenter()

        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(BUNDLE_UUID, uuid)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        compositeDisposable.clear()

        presenter.onDetachView()

        if (!isChangingConfigurations) {
            presenter.onDestroy()
            presenterManager.remove(uuid)
        }

        super.onDestroy()
    }

    override fun onRetainCustomNonConfigurationInstance() = presenterManager

    override fun getLastCustomNonConfigurationInstance() = super.getLastCustomNonConfigurationInstance() as? PresenterManager

    abstract protected fun restorePresenter()

    private fun restorePresenterManager() {
        presenterManager = lastCustomNonConfigurationInstance ?: PresenterManager()
    }
}
