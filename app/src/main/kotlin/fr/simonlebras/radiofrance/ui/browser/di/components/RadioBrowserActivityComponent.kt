package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import fr.simonlebras.radiofrance.di.components.BaseComponent
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.browser.activity.RadioBrowserActivity
import fr.simonlebras.radiofrance.ui.browser.activity.RadioBrowserActivityPresenter
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserActivityModule
import fr.simonlebras.radiofrance.ui.browser.di.modules.RadioBrowserFragmentModule

@Subcomponent(modules = arrayOf(RadioBrowserActivityModule::class))
@ActivityScope
interface RadioBrowserActivityComponent : BaseComponent<RadioBrowserActivity> {
    fun radioBrowserPresenter(): RadioBrowserActivityPresenter

    fun plus(module: RadioBrowserFragmentModule): RadioBrowserFragmentComponent
}
