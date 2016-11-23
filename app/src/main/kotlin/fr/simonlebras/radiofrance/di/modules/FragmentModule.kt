package fr.simonlebras.radiofrance.di.modules

import android.support.v4.app.Fragment
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.scopes.FragmentScope

@Module
abstract class FragmentModule<out T : Fragment>(private val fragment: T) {
    @Provides
    @FragmentScope
    fun provideFragment() = fragment
}
