package com.simonlebras.radiofrance.di

import com.simonlebras.radiofrance.playback.RadioPlaybackService
import com.simonlebras.radiofrance.ui.MainActivity
import com.simonlebras.radiofrance.utils.AppContexts
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import javax.inject.Singleton

@Module
abstract class ApplicationModule {
    @ContributesAndroidInjector(modules = [RadioPlaybackModule::class])
    abstract fun contributeRadioPlaybackServiceInjector(): RadioPlaybackService

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeMainActivityInjector(): MainActivity

    @Module
    companion object {
        @JvmStatic
        @Provides
        @Singleton
        fun provideAppContexts() = AppContexts(
            computation = CommonPool,
            network = newFixedThreadPoolContext(3, "network"),
            main = UI
        )
    }
}
