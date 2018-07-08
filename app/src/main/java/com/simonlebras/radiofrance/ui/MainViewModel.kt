package com.simonlebras.radiofrance.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.data.models.Resource
import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import com.simonlebras.radiofrance.ui.browser.manager.SubscriptionException
import com.simonlebras.radiofrance.ui.utils.debounce
import com.simonlebras.radiofrance.ui.utils.distinctUntilChanged
import com.simonlebras.radiofrance.ui.utils.switchMap
import com.simonlebras.radiofrance.utils.AppContexts
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val radioManager: RadioManager,
    private val appContexts: AppContexts
) : ViewModel() {
    val connection = MutableLiveData<Boolean>()

    val playbackState = MutableLiveData<PlaybackStateCompat>()
    val metadata = MutableLiveData<MediaMetadataCompat>()

    @Volatile
    lateinit var initialRadios: List<Radio>
    val radios = MutableLiveData<Resource<List<Radio>>>()

    private val parentJob = Job()

    private var connected = false
    private var radioLoaded = false

    private val loadRadiosActor =
        actor<Unit>(context = appContexts.network, parent = parentJob) {
            channel.consumeEach {
                radios.postValue(Resource.loading(null))

                try {
                    val radios = radioManager.loadRadios().await()

                    initialRadios = radios.data!!

                    this@MainViewModel.radios.postValue(radios)
                } catch (e: SubscriptionException) {
                    radios.postValue(Resource.error(e.message, null))
                }
            }
        }

    private val searchActor =
        actor<String>(context = appContexts.computation, parent = parentJob, capacity = CONFLATED) {
            channel
                .debounce(300)
                .map {
                    it.trim()
                }
                .distinctUntilChanged()
                .switchMap { query ->
                    initialRadios.filter {
                        query.isEmpty() || it.name.contains(query, true)
                    }
                }
                .consumeEach {
                    radios.postValue(Resource.success(it))
                }
        }

    override fun onCleared() {
        parentJob.cancel()

        radioManager.clear()

        super.onCleared()
    }

    fun connect() {
        if (!connected) {
            connected = true

            launch(context = appContexts.network, parent = parentJob) {
                connection.postValue(radioManager.connect().await())

                subscribePlaybackUpdates()
            }
        }
    }

    private suspend fun subscribePlaybackUpdates() {
        launch {
            for (e in radioManager.playbackStateUpdates()) {
                playbackState.postValue(e)
            }
        }

        launch {
            for (e in radioManager.metadataUpdates()) {
                metadata.postValue(e)
            }
        }
    }

    fun loadRadios() {
        if (!radioLoaded) {
            radioLoaded = true

            loadRadiosActor.offer(Unit)
        }
    }

    fun retryLoadRadios() {
        loadRadiosActor.offer(Unit)
    }

    fun searchRadios(query: String) {
        searchActor.offer(query)
    }


    fun playFromId(id: String) {
        radioManager.playFromId(id)
    }

    fun togglePlayPause() {
        radioManager.togglePlayPause()
    }

    fun skipToPrevious() {
        radioManager.skipToPrevious()
    }

    fun skipToNext() {
        radioManager.skipToNext()
    }
}
