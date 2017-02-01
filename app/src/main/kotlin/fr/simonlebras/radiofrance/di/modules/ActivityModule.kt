package fr.simonlebras.radiofrance.di.modules

import android.app.Activity
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.scopes.ActivityScope

@Module
abstract class ActivityModule<out T : Activity>(protected val activity: T) {
    @Provides
    @ActivityScope
    fun provideActivity() = activity
}
