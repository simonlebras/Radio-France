package com.simonlebras.radiofrance.di

import android.content.Context
import com.simonlebras.radiofrance.RadioFranceApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Component(modules = [
    AndroidSupportInjectionModule::class,
    ApplicationModule::class,
    ViewModelModule::class
])
@Singleton
interface ApplicationComponent : AndroidInjector<RadioFranceApplication> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): ApplicationComponent
    }
}
