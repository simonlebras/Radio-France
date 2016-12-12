package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import fr.simonlebras.radiofrance.di.components.BaseComponent
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.ui.browser.di.modules.MiniPlayerModule
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerPresenter

@Subcomponent(modules = arrayOf(MiniPlayerModule::class))
@FragmentScope
interface MiniPlayerComponent : BaseComponent<MiniPlayerFragment> {
    fun miniPlayerPresenter(): MiniPlayerPresenter
}
