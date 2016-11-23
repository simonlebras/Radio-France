package fr.simonlebras.radiofrance.ui.browser.di.modules

import dagger.Module
import fr.simonlebras.radiofrance.di.modules.FragmentModule
import fr.simonlebras.radiofrance.ui.browser.fragment.RadioBrowserFragment

@Module
class RadioBrowserFragmentModule(fragment: RadioBrowserFragment) : FragmentModule<RadioBrowserFragment>(fragment)
