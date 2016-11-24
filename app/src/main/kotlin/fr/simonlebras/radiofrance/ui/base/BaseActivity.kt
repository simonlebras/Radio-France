package fr.simonlebras.radiofrance.ui.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import fr.simonlebras.radiofrance.di.components.BaseComponent
import io.reactivex.disposables.CompositeDisposable
import java.util.*

abstract class BaseActivity<T : BasePresenter<out BaseView>> : AppCompatActivity(), BaseFragment.BaseListener {
    private companion object {
        private const val BUNDLE_UUID = "BUNDLE_UUID"
    }

    override abstract val component: BaseComponent<*>

    override lateinit var presenterManager: PresenterManager

    protected val compositeDisposable = CompositeDisposable()
    protected lateinit var presenter: T
    protected lateinit var uuid: UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uuid = savedInstanceState?.get(BUNDLE_UUID) as? UUID ?: UUID.randomUUID()
        restorePresenterManager()
        restorePresenter()
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
