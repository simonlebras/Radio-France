package com.simonlebras.radiofrance.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import com.simonlebras.radiofrance.di.scopes.ActivityScope
import com.simonlebras.radiofrance.di.scopes.ServiceScope
import com.simonlebras.radiofrance.playback.RadioPlaybackService
import com.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule
import com.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import com.simonlebras.radiofrance.ui.browser.di.modules.BindingModule
import com.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserModule

@Module
abstract class BindingModule {
    @ContributesAndroidInjector(modules = arrayOf(RadioPlaybackModule::class))
    @ServiceScope
    abstract fun contributeRadioPlaybackServiceInjector(): RadioPlaybackService

    @ContributesAndroidInjector(modules = arrayOf(RadioBrowserModule::class, BindingModule::class))
    @ActivityScope
    abstract fun contributeRadioBrowserActivityInjector(): RadioBrowserActivity
}
