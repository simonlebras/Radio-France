package fr.simonlebras.radiofrance.ui.browser.di.modules

import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.modules.ActivityModule
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.ui.browser.activity.RadioBrowserActivity
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManager
import fr.simonlebras.radiofrance.ui.browser.manager.RadioManagerImpl

@Module
class RadioBrowserActivityModule(activity: RadioBrowserActivity) : ActivityModule<RadioBrowserActivity>(activity) {
    @Provides
    @ActivityScope
    fun provideRadioManager(radioManager: RadioManagerImpl): RadioManager = radioManager
}
