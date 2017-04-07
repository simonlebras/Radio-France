package fr.simonlebras.radiofrance.ui.browser.di.modules

import dagger.Binds
import dagger.Module
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManagerImpl

@Module
abstract class RadioBrowserModule {
    @Binds
    @ActivityScope
    abstract fun bindRadioManager(radioManager: RadioManagerImpl): RadioManager
}
