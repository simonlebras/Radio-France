package fr.simonlebras.radiofrance.ui.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import fr.simonlebras.radiofrance.di.components.BaseComponent
import io.reactivex.disposables.CompositeDisposable
import java.util.*

abstract class BaseActivity<T : BasePresenter<out BaseView>> : AppCompatActivity(), BaseFragment.BaseListener {
    companion object {
        const val BUNDLE_UUID = "BUNDLE_UUID"
    }

    lateinit var presenterManager: PresenterManager

    abstract val component: BaseComponent<*>

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

    override fun provideComponent() = component

    override fun providePresenterManager() = presenterManager

    abstract protected fun restorePresenter()

    private fun restorePresenterManager() {
        presenterManager = lastCustomNonConfigurationInstance ?: PresenterManager()
    }
}
