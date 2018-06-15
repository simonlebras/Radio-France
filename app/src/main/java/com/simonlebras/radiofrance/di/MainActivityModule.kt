package com.simonlebras.radiofrance.di

import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import com.simonlebras.radiofrance.ui.browser.manager.RadioManagerImpl
import dagger.Binds
import dagger.Module

@Module
abstract class MainActivityModule {
    @Binds
    abstract fun bindRadioManager(radioManager: RadioManagerImpl): RadioManager
}
