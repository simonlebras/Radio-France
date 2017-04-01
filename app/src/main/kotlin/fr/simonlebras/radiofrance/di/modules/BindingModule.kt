package fr.simonlebras.radiofrance.di.modules

import android.app.Activity
import android.app.Service
import dagger.Binds
import dagger.Module
import dagger.android.ActivityKey
import dagger.android.AndroidInjector
import dagger.android.ServiceKey
import dagger.multibindings.IntoMap
import fr.simonlebras.radiofrance.playback.RadioPlaybackService
import fr.simonlebras.radiofrance.playback.di.components.RadioPlaybackComponent
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioBrowserComponent

@Module(subcomponents = arrayOf(RadioPlaybackComponent::class, RadioBrowserComponent::class))
abstract class BindingModule {
    @Binds
    @IntoMap
    @ServiceKey(RadioPlaybackService::class)
    abstract fun bindRadioPlaybackServiceInjectorFactory(builder: RadioPlaybackComponent.Builder): AndroidInjector.Factory<out Service>

    @Binds
    @IntoMap
    @ActivityKey(RadioBrowserActivity::class)
    abstract fun bindRadioBrowserActivityInjectorFactory(builder: RadioBrowserComponent.Builder): AndroidInjector.Factory<out Activity>
}