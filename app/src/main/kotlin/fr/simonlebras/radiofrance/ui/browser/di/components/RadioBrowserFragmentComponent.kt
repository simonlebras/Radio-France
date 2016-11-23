package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import fr.simonlebras.radiofrance.di.components.BaseComponent
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserFragmentModule
import fr.simonlebras.radiofrance.ui.browser.fragment.RadioBrowserFragment
import fr.simonlebras.radiofrance.ui.browser.fragment.RadioBrowserFragmentPresenter

@Subcomponent(modules = arrayOf(RadioBrowserFragmentModule::class))
@FragmentScope
interface RadioBrowserFragmentComponent : BaseComponent<RadioBrowserFragment> {
    fun radioBrowserPresenter(): RadioBrowserFragmentPresenter
}
