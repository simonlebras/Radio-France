package fr.simonlebras.radiofrance

import android.annotation.TargetApi
import android.app.Activity
import android.app.Service
import android.os.Build.VERSION_CODES.M
import android.os.StrictMode
import android.support.v7.preference.PreferenceManager
import com.squareup.leakcanary.LeakCanary
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import dagger.android.support.DaggerApplication
import fr.simonlebras.radiofrance.di.components.DaggerApplicationComponent
import fr.simonlebras.radiofrance.utils.DebugUtils
import fr.simonlebras.radiofrance.utils.VersionUtils.supportsSdkVersion
import timber.log.Timber
import javax.inject.Inject

class RadioFranceApplication : DaggerApplication(),
        HasActivityInjector,
        HasServiceInjector {

    val component by lazy(LazyThreadSafetyMode.NONE) {
        DaggerApplicationComponent.builder()
                .context(this)
                .build()
    }

    @Inject lateinit var dispatchingActivityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var dispatchingServiceInjector: DispatchingAndroidInjector<Service>

    @TargetApi(M)
    override fun onCreate() {
        super.onCreate()

        if (!setupLeakCanary()) {
            return
        }

        DebugUtils.executeInDebugMode {
            setupTimber()

            setupStrictMode()
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
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

    @TargetApi(M)
    private fun setupStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()

        supportsSdkVersion(M) {
            threadPolicyBuilder.detectResourceMismatches()
            vmPolicyBuilder.detectCleartextNetwork()
        }

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
