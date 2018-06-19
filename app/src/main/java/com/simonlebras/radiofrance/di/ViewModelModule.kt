package com.simonlebras.radiofrance.di

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.simonlebras.radiofrance.ui.MainViewModel
import com.simonlebras.radiofrance.ui.utils.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module(includes = [MainActivityModule::class])
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    abstract fun bindViewModelProviderFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}
