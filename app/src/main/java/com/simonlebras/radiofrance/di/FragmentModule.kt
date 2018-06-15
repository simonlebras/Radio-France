package com.simonlebras.radiofrance.di

import com.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import com.simonlebras.radiofrance.ui.browser.player.MiniPlayerFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FragmentModule {
    @ContributesAndroidInjector
    abstract fun contributeRadioListFragmentInjector(): RadioListFragment

    @ContributesAndroidInjector
    abstract fun contributeMiniPlayerFragmentInjector(): MiniPlayerFragment
}
