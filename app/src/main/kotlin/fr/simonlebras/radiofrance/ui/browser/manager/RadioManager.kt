package fr.simonlebras.radiofrance.ui.browser.manager

import fr.simonlebras.radiofrance.models.Radio
import io.reactivex.Observable
import io.reactivex.Single

interface RadioManager {
    var cache: List<Radio>?
    fun connect(): Observable<MediaControllerCompatWrapper>
    fun getRadios(): Single<List<Radio>>
    fun reset()
}
