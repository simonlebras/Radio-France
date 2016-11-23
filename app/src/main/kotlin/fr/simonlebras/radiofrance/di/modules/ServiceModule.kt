package fr.simonlebras.radiofrance.di.modules

import android.app.Service
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.scopes.ServiceScope

@Module
abstract class ServiceModule<out T : Service>(private val service: T) {
    @Provides
    @ServiceScope
    fun provideService() = service
}
