package com.simonlebras.radiofrance.playback.di.components

import dagger.Subcomponent
import dagger.android.AndroidInjector
import com.simonlebras.radiofrance.di.scopes.ServiceScope
import com.simonlebras.radiofrance.playback.RadioPlaybackService
import com.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule

@Subcomponent(modules = arrayOf(RadioPlaybackModule::class))
@ServiceScope
interface RadioPlaybackComponent : AndroidInjector<RadioPlaybackService> {
    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<RadioPlaybackService>()
}
