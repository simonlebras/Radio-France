package fr.simonlebras.radiofrance.di.components

import dagger.Component
import dagger.MembersInjector
import fr.simonlebras.radiofrance.RadioFranceApplication
import fr.simonlebras.radiofrance.di.modules.ApplicationModule
import fr.simonlebras.radiofrance.di.modules.BindingModule
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Component(modules = arrayOf(ApplicationModule::class, BindingModule::class))
@Singleton
interface ApplicationComponent : MembersInjector<RadioFranceApplication> {
    fun okHttpClient(): OkHttpClient
}
