package fr.simonlebras.radiofrance.ui.base

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import fr.simonlebras.radiofrance.di.components.ComponentProvider
import io.reactivex.disposables.CompositeDisposable
import java.util.*

abstract class BaseFragment<T : BasePresenter<out BaseView>> : Fragment() {
    private companion object {
        private const val BUNDLE_UUID = "BUNDLE_UUID"
    }

    protected val compositeDisposable = CompositeDisposable()
    protected lateinit var presenter: T
    protected lateinit var uuid: UUID
    protected var baseListener: BaseListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        baseListener = context as BaseListener
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        uuid = savedInstanceState?.getSerializable(BUNDLE_UUID) as? UUID ?: UUID.randomUUID()
        restorePresenter()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(BUNDLE_UUID, uuid)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        compositeDisposable.dispose()

        presenter.onDetachView()

        super.onDestroyView()
    }

    override fun onDestroy() {
        if (!activity.isChangingConfigurations) {
            presenter.onDestroy()
            baseListener!!.presenterManager.remove(uuid)
        }

        super.onDestroy()
    }

    override fun onDetach() {
        baseListener = null

        super.onDetach()
    }

    abstract protected fun restorePresenter()

    interface BaseListener : ComponentProvider {
        val presenterManager: PresenterManager
    }
}
