package fr.simonlebras.radiofrance.di.components

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import fr.simonlebras.radiofrance.RadioFranceApplication
import fr.simonlebras.radiofrance.di.modules.ApplicationModule
import fr.simonlebras.radiofrance.di.modules.BindingModule
import okhttp3.OkHttpClient
import javax.inject.Singleton


@Component(modules = arrayOf(ApplicationModule::class, BindingModule::class, AndroidSupportInjectionModule::class))
@Singleton
interface ApplicationComponent : AndroidInjector<RadioFranceApplication> {
    fun okHttpClient(): OkHttpClient

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): ApplicationComponent
    }
}
