package com.simonlebras.radiofrance

import android.os.StrictMode
import com.simonlebras.radiofrance.di.components.DaggerApplicationComponent
import com.squareup.leakcanary.LeakCanary
import dagger.android.support.DaggerApplication
import timber.log.Timber

class RadioFranceApplication : DaggerApplication() {
    val component by lazy(LazyThreadSafetyMode.NONE) {
        DaggerApplicationComponent.builder()
                .context(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG && !LeakCanary.isInAnalyzerProcess(this)) {
            LeakCanary.install(this)

            Timber.plant(Timber.DebugTree())

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

    override fun applicationInjector() = component
}
