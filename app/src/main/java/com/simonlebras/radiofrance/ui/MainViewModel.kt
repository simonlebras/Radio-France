package com.simonlebras.radiofrance.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.data.models.Resource
import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import com.simonlebras.radiofrance.utils.AppSchedulers
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val radioManager: RadioManager,
    private val appSchedulers: AppSchedulers
) : ViewModel() {
    val connection = MutableLiveData<MediaControllerCompat>()

    val playbackState = MutableLiveData<PlaybackStateCompat>()
    val metadata = MutableLiveData<MediaMetadataCompat>()

    val radios = MutableLiveData<Resource<List<Radio>>>()

    private val compositeDisposable = CompositeDisposable()

    private var connectionStarted = false

    private var radioLoadingStarted = false

    private val retryProcessor = PublishProcessor.create<Boolean>()

    private val searchSubject = BehaviorSubject.createDefault("")

    override fun onCleared() {
        compositeDisposable.dispose()

        radioManager.clear()

        super.onCleared()
    }

    fun connect() {
        if (!connectionStarted) {
            connectionStarted = true

            val disposable = radioManager.connect()
                .subscribeWith(object : DisposableSingleObserver<MediaControllerCompat>() {
                    override fun onSuccess(mediaController: MediaControllerCompat) {
                        connection.value = mediaController

                        subscribePlaybackUpdates()
                    }

                    override fun onError(e: Throwable) {}
                })

            compositeDisposable.add(disposable)
        }
    }

    private fun subscribePlaybackUpdates() {
        val playbackDisposable = radioManager.playbackStateUpdates()
            .subscribeWith(object : DisposableObserver<PlaybackStateCompat>() {
                override fun onComplete() {}

                override fun onNext(state: PlaybackStateCompat) {
                    playbackState.value = state
                }

                override fun onError(e: Throwable) {}
            })

        compositeDisposable.add(playbackDisposable)

        val metadataDisposable = radioManager.metadataUpdates()
            .subscribeWith(object : DisposableObserver<MediaMetadataCompat>() {
                override fun onComplete() {}

                override fun onNext(metadata: MediaMetadataCompat) {
                    this@MainViewModel.metadata.value = metadata
                }

                override fun onError(e: Throwable) {}
            })

        compositeDisposable.add(metadataDisposable)
    }

    fun loadRadios() {
        if (!radioLoadingStarted) {
            radioLoadingStarted = true

            val disposable = Observables
                .combineLatest(
                    radioManager.loadRadios()
                        .subscribeOn(appSchedulers.network)
                        .doOnSubscribe { radios.postValue(Resource.loading(null)) }
                        .doOnError { radios.postValue(Resource.error(it.message, null)) }
                        .retryWhen { retryProcessor }
                        .toObservable(),
                    searchSubject
                ) { resource, query ->
                    Pair(resource, query)
                }
                .switchMapSingle {
                    val (resource, query) = it

                    Observable.fromIterable(resource.data!!)
                        .filter {
                            query.isEmpty() || it.name.contains(query, true)
                        }
                        .toList()
                        .map {
                            Resource.success(it)
                        }

                }
                .subscribeWith(object : DisposableObserver<Resource<List<Radio>>>() {
                    override fun onComplete() {}

                    override fun onNext(resource: Resource<List<Radio>>) {
                        radios.postValue(resource)
                    }

                    override fun onError(e: Throwable) {}
                })

            compositeDisposable.add(disposable)
        }
    }

    fun searchRadios(query: String) {
        searchSubject.onNext(query)
    }

    fun retryLoadRadios() {
        retryProcessor.offer(true)
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
