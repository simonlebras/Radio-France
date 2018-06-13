package com.simonlebras.radiofrance.ui.browser.list

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.data.model.Radio
import com.simonlebras.radiofrance.di.scopes.FragmentScope
import com.simonlebras.radiofrance.ui.base.BasePresenter
import com.simonlebras.radiofrance.ui.base.BaseView
import com.simonlebras.radiofrance.ui.browser.manager.RadioManager
import com.simonlebras.radiofrance.utils.AppSchedulers
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

@FragmentScope
class RadioListPresenter @Inject constructor(
        private val radioManager: RadioManager,
        private val appSchedulers: AppSchedulers
) : BasePresenter<RadioListPresenter.View>() {
    private var refreshSubject = PublishSubject.create<Boolean>()
    private var searchSubject = BehaviorSubject.create<String>()

    fun connect() {
        compositeDisposable.add(radioManager.connection
                                        .subscribe {
                                            view?.onConnected(it)
                                        })
    }

    fun subscribeToRefreshAndSearchEvents() {
        compositeDisposable.add(Observable
                                        .combineLatest(refreshSubject, searchSubject.startWith(""), BiFunction { _: Boolean, search: String -> search })
                                        .switchMap {
                                            val query = it
                                            radioManager.radios
                                                    .onErrorResumeNext(Observable.just(emptyList()))
                                                    .flatMap {
                                                        Observable.fromIterable(it)
                                                                .filter {
                                                                    it.name.toLowerCase()
                                                                            .contains(query.toLowerCase())
                                                                }
                                                                .toList()
                                                                .map {
                                                                    Pair(query, it)
                                                                }
                                                                .toObservable()
                                                    }
                                        }
                                        .observeOn(appSchedulers.main)
                                        .subscribe {
                                            val query = it.first
                                            val radios = it.second

                                            if (radios.isEmpty()) {
                                                if (query.isBlank()) {
                                                    view?.showRefreshError()
                                                } else {
                                                    view?.showSearchError()
                                                }

                                                return@subscribe
                                            }

                                            view?.showRadios(radios)
                                        })
    }

    fun subscribeToPlaybackUpdates() {
        compositeDisposable.add(radioManager.playbackUpdates
                                        .subscribe {
                                            if (it is MediaMetadataCompat) {
                                                view?.onMetadataChanged(it)
                                            } else if (it is PlaybackStateCompat) {
                                                view?.onPlaybackStateChanged(it)
                                            }
                                        }
        )
    }

    fun refresh() {
        refreshSubject.onNext(true)
    }

    fun searchRadios(query: String) {
        searchSubject.onNext(query)
    }

    fun play(id: String) {
        radioManager.play(id)
    }

    interface View : BaseView {
        fun onConnected(mediaController: MediaControllerCompat)

        fun showRadios(radios: List<Radio>)

        fun showRefreshError()

        fun showSearchError()

        fun onMetadataChanged(metadata: MediaMetadataCompat)

        fun onPlaybackStateChanged(playbackState: PlaybackStateCompat)
    }
}
