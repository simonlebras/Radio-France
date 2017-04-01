package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import dagger.android.AndroidInjector
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import fr.simonlebras.radiofrance.ui.browser.di.modules.BindingModule
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserModule

@Subcomponent(modules = arrayOf(RadioBrowserModule::class, BindingModule::class))
@ActivityScope
interface RadioBrowserComponent : AndroidInjector<RadioBrowserActivity> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RadioBrowserActivity>()
}
