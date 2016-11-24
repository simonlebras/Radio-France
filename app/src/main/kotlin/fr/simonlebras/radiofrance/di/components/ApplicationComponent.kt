package fr.simonlebras.radiofrance.di.components

import dagger.Component
import fr.simonlebras.radiofrance.RadioFranceApplication
import fr.simonlebras.radiofrance.di.modules.ApplicationModule
import fr.simonlebras.radiofrance.playback.di.components.RadioPlaybackComponent
import fr.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioBrowserActivityComponent
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserActivityModule
import javax.inject.Singleton

@Component(modules = arrayOf(ApplicationModule::class))
@Singleton
interface ApplicationComponent : BaseComponent<RadioFranceApplication> {
    fun plus(module: RadioPlaybackModule): RadioPlaybackComponent

    fun plus(module: RadioBrowserActivityModule): RadioBrowserActivityComponent
}
