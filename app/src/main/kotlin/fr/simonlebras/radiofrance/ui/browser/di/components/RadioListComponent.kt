package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import fr.simonlebras.radiofrance.di.components.BaseComponent
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioListModule
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import fr.simonlebras.radiofrance.ui.browser.list.RadioListPresenter

@Subcomponent(modules = arrayOf(RadioListModule::class))
@FragmentScope
interface RadioListComponent : BaseComponent<RadioListFragment> {
    fun radioListPresenter(): RadioListPresenter
}
