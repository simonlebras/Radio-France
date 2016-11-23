package fr.simonlebras.radiofrance.playback.di.components

import dagger.Subcomponent
import fr.simonlebras.radiofrance.di.components.BaseComponent
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.RadioPlaybackService
import fr.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule

@Subcomponent(modules = arrayOf(RadioPlaybackModule::class))
@ServiceScope
interface RadioPlaybackComponent : BaseComponent<RadioPlaybackService>
