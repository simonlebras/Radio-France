package fr.simonlebras.radiofrance.ui.browser.di.modules

import dagger.Module
import dagger.android.ContributesAndroidInjector
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment

@Module
abstract class BindingModule {
    @ContributesAndroidInjector
    @FragmentScope
    abstract fun contributeRadioListFragmentInjector(): RadioListFragment

    @ContributesAndroidInjector
    @FragmentScope
    abstract fun contributeMiniPlayerFragmentInjector(): MiniPlayerFragment
}