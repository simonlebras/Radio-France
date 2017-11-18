package fr.simonlebras.radiofrance.ui.base

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import java.util.*

abstract class BaseFragment<T : BasePresenter<out BaseView>> : Fragment() {
    private companion object {
        const val BUNDLE_UUID = "BUNDLE_UUID"
    }

    private var baseCallback: BaseCallback? = null

    protected lateinit var presenter: T
    protected lateinit var uuid: UUID

    private val compositeDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)

        super.onAttach(context)

        baseCallback = context as BaseCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uuid = savedInstanceState?.getSerializable(BUNDLE_UUID) as? UUID ?: UUID.randomUUID()
        restorePresenter()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(BUNDLE_UUID, uuid)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        compositeDisposable.clear()

        presenter.onDetachView()

        super.onDestroyView()
    }

    override fun onDestroy() {
        if (!activity!!.isChangingConfigurations) {
            presenter.onDestroy()
            baseCallback!!.presenterManager.remove(uuid)
        }

        super.onDestroy()
    }

    override fun onDetach() {
        baseCallback = null

        super.onDetach()
    }

    abstract protected fun restorePresenter()

    interface BaseCallback {
        val presenterManager: PresenterManager
    }
}
