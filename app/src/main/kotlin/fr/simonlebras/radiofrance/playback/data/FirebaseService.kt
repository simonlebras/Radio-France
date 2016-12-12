package fr.simonlebras.radiofrance.playback.data

import fr.simonlebras.radiofrance.models.Radio
import io.reactivex.Observable
import retrofit2.http.GET

interface FirebaseService {
    @GET("radios.json")
    fun getRadios(): Observable<List<Radio>>
}
