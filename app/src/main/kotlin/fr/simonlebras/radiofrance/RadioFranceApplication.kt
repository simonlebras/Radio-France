package fr.simonlebras.radiofrance

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.app.Service
import android.os.Build.VERSION_CODES.M
import android.os.StrictMode
import com.facebook.stetho.Stetho
import com.facebook.stetho.timber.StethoTree
import com.squareup.leakcanary.LeakCanary
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasDispatchingActivityInjector
import dagger.android.HasDispatchingServiceInjector
import fr.simonlebras.radiofrance.di.components.ApplicationComponent
import fr.simonlebras.radiofrance.di.components.DaggerApplicationComponent
import fr.simonlebras.radiofrance.di.modules.ApplicationModule
import fr.simonlebras.radiofrance.utils.DebugUtils
import fr.simonlebras.radiofrance.utils.VersionUtils.supportsSdkVersion
import timber.log.Timber
import javax.inject.Inject

class RadioFranceApplication : Application(),
        HasDispatchingActivityInjector,
        HasDispatchingServiceInjector {
    val component: ApplicationComponent by lazy(LazyThreadSafetyMode.NONE) {
        DaggerApplicationComponent.builder()
                .applicationModule(ApplicationModule(this))
                .build()
    }

    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    @TargetApi(M)
    override fun onCreate() {
        super.onCreate()

        if (!setupLeakCanary()) {
            return
        }

        DebugUtils.executeInDebugMode {
            setupTimber()

            setupStetho()

            setupStrictMode()
        }

        component.injectMembers(this)
    }

    override fun activityInjector() = activityInjector

    override fun serviceInjector() = serviceInjector

    private fun setupLeakCanary(): Boolean {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return false
        }

        LeakCanary.install(this)
        return true
    }

    private fun setupTimber() {
        Timber.plant(Timber.DebugTree())
        Timber.plant(StethoTree())
    }

    private fun setupStetho() {
        Stetho.initializeWithDefaults(this)
    }

    @TargetApi(M)
    private fun setupStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()

        supportsSdkVersion(M, {
            threadPolicyBuilder.detectResourceMismatches()
            vmPolicyBuilder.detectCleartextNetwork()
        })

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
