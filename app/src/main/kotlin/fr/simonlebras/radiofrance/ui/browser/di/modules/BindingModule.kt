package fr.simonlebras.radiofrance.ui.browser.di.modules

import android.support.v4.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.support.FragmentKey
import dagger.multibindings.IntoMap
import fr.simonlebras.radiofrance.ui.browser.di.components.MiniPlayerComponent
import fr.simonlebras.radiofrance.ui.browser.di.components.RadioListComponent
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import fr.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment

@Module(subcomponents = arrayOf(RadioListComponent::class, MiniPlayerComponent::class))
abstract class BindingModule {
    @Binds
    @IntoMap
    @FragmentKey(RadioListFragment::class)
    abstract fun bindRadioListFragmentInjectorFactory(builder: RadioListComponent.Builder): AndroidInjector.Factory<out Fragment>

    @Binds
    @IntoMap
    @FragmentKey(MiniPlayerFragment::class)
    abstract fun bindMiniPlayerFragmentInjectorFactory(builder: MiniPlayerComponent.Builder): AndroidInjector.Factory<out Fragment>
}