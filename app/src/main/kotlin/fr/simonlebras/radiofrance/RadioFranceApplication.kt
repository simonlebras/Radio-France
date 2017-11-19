package fr.simonlebras.radiofrance

import android.app.Activity
import android.app.Service
import android.os.StrictMode
import com.squareup.leakcanary.LeakCanary
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import dagger.android.support.DaggerApplication
import fr.simonlebras.radiofrance.di.components.DaggerApplicationComponent
import fr.simonlebras.radiofrance.utils.DebugUtils
import timber.log.Timber
import javax.inject.Inject

class RadioFranceApplication : DaggerApplication(), HasActivityInjector, HasServiceInjector {
    val component by lazy(LazyThreadSafetyMode.NONE) {
        DaggerApplicationComponent.builder()
                .context(this)
                .build()
    }

    @Inject lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>

    override fun onCreate() {
        super.onCreate()

        if (!setupLeakCanary()) {
            return
        }

        DebugUtils.executeInDebugMode {
            setupTimber()

            setupStrictMode()
        }
    }

    override fun applicationInjector() = component

    private fun setupLeakCanary(): Boolean {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return false
        }

        LeakCanary.install(this)
        return true
    }

    private fun setupTimber() {
        Timber.plant(Timber.DebugTree())
    }

    private fun setupStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
    }
}
