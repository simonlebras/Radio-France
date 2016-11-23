package fr.simonlebras.radiofrance

import android.annotation.TargetApi
import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.squareup.leakcanary.LeakCanary
import fr.simonlebras.radiofrance.di.components.ApplicationComponent
import fr.simonlebras.radiofrance.di.components.DaggerApplicationComponent
import fr.simonlebras.radiofrance.di.modules.ApplicationModule
import fr.simonlebras.radiofrance.utils.DebugUtils
import fr.simonlebras.radiofrance.utils.VersionUtils
import timber.log.Timber

class RadioFranceApplication : Application() {
    val component: ApplicationComponent by lazy(LazyThreadSafetyMode.NONE) {
        DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
    }

    override fun onCreate() {
        super.onCreate()

        if (!setupLeakCanary()) {
            return
        }

        DebugUtils.executeInDebugMode {
            setupTimber()

            setupStrictMode()
        }

        component.inject(this)
    }

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

    @TargetApi(Build.VERSION_CODES.M)
    private fun setupStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())

        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()

        VersionUtils.supportsSdkVersion(Build.VERSION_CODES.JELLY_BEAN_MR2, {
            vmPolicyBuilder.detectFileUriExposure()
        })

        VersionUtils.supportsSdkVersion(Build.VERSION_CODES.M, {
            vmPolicyBuilder.detectCleartextNetwork()
        })

        vmPolicyBuilder.detectActivityLeaks()
                .detectLeakedClosableObjects()
                .penaltyLog()

        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
