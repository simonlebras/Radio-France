package com.simonlebras.radiofrance.di

import com.simonlebras.radiofrance.playback.RadioPlaybackService
import com.simonlebras.radiofrance.ui.MainActivity
import com.simonlebras.radiofrance.utils.AppSchedulers
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
abstract class ApplicationModule {
    @ContributesAndroidInjector(modules = [RadioPlaybackModule::class])
    @ServiceScope
    abstract fun contributeRadioPlaybackServiceInjector(): RadioPlaybackService

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeMainActivityInjector(): MainActivity

    @Module
    companion object {
        @JvmStatic
        @Provides
        @Singleton
        fun provideAppSchedulers() = AppSchedulers(
                Schedulers.computation(),
                Schedulers.from(Executors.newFixedThreadPool(3)),
                AndroidSchedulers.mainThread()
        )
    }
}
