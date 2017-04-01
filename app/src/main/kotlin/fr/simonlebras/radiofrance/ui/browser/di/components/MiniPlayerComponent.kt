package fr.simonlebras.radiofrance.ui.browser.di.components

import dagger.Subcomponent
import dagger.android.AndroidInjector
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.ui.browser.di.modules.MiniPlayerModule
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment

@Subcomponent(modules = arrayOf(MiniPlayerModule::class))
@FragmentScope
interface MiniPlayerComponent : AndroidInjector<MiniPlayerFragment> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<MiniPlayerFragment>()
}
