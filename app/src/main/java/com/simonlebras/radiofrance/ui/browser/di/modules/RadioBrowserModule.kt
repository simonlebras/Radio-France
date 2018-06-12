package com.simonlebras.radiofrance.ui.browser.di.modules

import dagger.Binds
import dagger.Module
import com.simonlebras.radiofrance.di.scopes.ActivityScope
import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import com.simonlebras.radiofrance.ui.browser.manager.RadioManagerImpl

@Module
abstract class RadioBrowserModule {
    @Binds
    @ActivityScope
    abstract fun bindRadioManager(radioManager: RadioManagerImpl): RadioManager
}
