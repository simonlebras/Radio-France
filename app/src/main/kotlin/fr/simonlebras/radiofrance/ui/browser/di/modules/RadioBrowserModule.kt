package fr.simonlebras.radiofrance.ui.browser.di.modules

import com.google.android.gms.cast.framework.CastContext
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManagerImpl

@Module
class RadioBrowserModule {
    @Provides
    @ActivityScope
    fun provideRadioManager(radioManager: RadioManagerImpl): RadioManager = radioManager

    @Provides
    @ActivityScope
    fun provideCastContext(activity: RadioBrowserActivity): CastContext = CastContext.getSharedInstance(activity)
}
