package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import fr.simonlebras.radiofrance.di.components.BaseComponent
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserPresenter
import fr.simonlebras.radiofrance.ui.browser.di.modules.MiniPlayerModule
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserModule
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioListModule

@Subcomponent(modules = arrayOf(RadioBrowserModule::class))
@ActivityScope
interface RadioBrowserComponent : BaseComponent<RadioBrowserActivity> {
    fun radioBrowserPresenter(): RadioBrowserPresenter

    fun plus(module: RadioListModule): RadioListComponent

    fun plus(module: MiniPlayerModule): MiniPlayerComponent
}
