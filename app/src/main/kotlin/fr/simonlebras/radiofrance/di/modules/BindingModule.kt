package fr.simonlebras.radiofrance.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.RadioPlaybackService
import fr.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import fr.simonlebras.radiofrance.ui.browser.di.modules.BindingModule
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserModule

@Module
abstract class BindingModule {
    @ContributesAndroidInjector(modules = arrayOf(RadioPlaybackModule::class))
    @ServiceScope
    abstract fun contributeRadioPlaybackServiceInjector(): RadioPlaybackService

    @ContributesAndroidInjector(modules = arrayOf(RadioBrowserModule::class, BindingModule::class))
    @ActivityScope
    abstract fun contributeRadioBrowserActivityInjector(): RadioBrowserActivity
}