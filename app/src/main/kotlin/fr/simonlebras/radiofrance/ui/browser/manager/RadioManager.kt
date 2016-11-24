package fr.simonlebras.radiofrance.ui.browser.manager

import fr.simonlebras.radiofrance.models.Radio
import io.reactivex.Observable
import io.reactivex.Single

interface RadioManager {
    val connection: Observable<MediaControllerCompatWrapper>

    val radios: Single<List<Radio>>

    var cache: List<Radio>?

    fun reset()
}
