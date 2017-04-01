package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import dagger.android.AndroidInjector
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioListModule
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment

@Subcomponent(modules = arrayOf(RadioListModule::class))
@FragmentScope
interface RadioListComponent : AndroidInjector<RadioListFragment> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RadioListFragment>()
}
