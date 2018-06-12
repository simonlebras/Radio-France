package com.simonlebras.radiofrance.di.components

import android.content.Context
import com.simonlebras.radiofrance.RadioFranceApplication
import com.simonlebras.radiofrance.di.modules.ApplicationModule
import com.simonlebras.radiofrance.di.modules.BindingModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Component(modules = [
    ApplicationModule::class,
    BindingModule::class,
    AndroidSupportInjectionModule::class
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
