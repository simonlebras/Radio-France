package com.simonlebras.radiofrance.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.simonlebras.radiofrance.ui.MainViewModel
import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import com.simonlebras.radiofrance.ui.browser.manager.RadioManagerImpl
import com.simonlebras.radiofrance.ui.utils.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindRadioManager(radioManager: RadioManagerImpl): RadioManager

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    abstract fun bindViewModelProviderFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
