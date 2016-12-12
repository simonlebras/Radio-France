package fr.simonlebras.radiofrance.di.components

import dagger.Component
import fr.simonlebras.radiofrance.RadioFranceApplication
import fr.simonlebras.radiofrance.di.modules.ApplicationModule
import fr.simonlebras.radiofrance.playback.di.components.RadioPlaybackComponent
import fr.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioBrowserComponent
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserModule
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Component(modules = arrayOf(ApplicationModule::class))
@Singleton
interface ApplicationComponent : BaseComponent<RadioFranceApplication> {
    fun okHttpClient(): OkHttpClient

    fun plus(module: RadioPlaybackModule): RadioPlaybackComponent

    fun plus(module: RadioBrowserModule): RadioBrowserComponent
}
