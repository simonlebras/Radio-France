package com.simonlebras.radiofrance.ui.browser.manager

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.data.models.Resource
import com.simonlebras.radiofrance.playback.RadioPlaybackService
import com.simonlebras.radiofrance.ui.browser.mappers.RadioMapper
import com.simonlebras.radiofrance.utils.AppSchedulers
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class RadioManagerImpl @Inject constructor(
        private val context: Context,
        private val appSchedulers: AppSchedulers
) : RadioManager {
    private val compositeDisposable = CompositeDisposable()

    private lateinit var mediaBrowser: MediaBrowserCompat

    private lateinit var mediaController: MediaControllerCompat

    private val playbackUpdates by lazy(LazyThreadSafetyMode.NONE) {
        Observable
                .create<Any> { emitter ->
                    val callback = object : MediaControllerCompat.Callback() {
                        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                            if (!emitter.isDisposed && state != null) {
                                emitter.onNext(state)
                            }
                        }

                        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                            if (!emitter.isDisposed && metadata != null) {
                                emitter.onNext(metadata)
                            }
                        }
                    }

                    emitter.setCancellable { mediaController.unregisterCallback(callback) }

                    mediaController.playbackState?.let {
                        emitter.onNext(it)
                    }

                    mediaController.metadata?.let {
                        emitter.onNext(it)
                    }

                    mediaController.registerCallback(callback)
                }
                .publish()
                .autoConnect(2) {
                    compositeDisposable.add(it)
                }
    }

    override fun connect(): Single<MediaControllerCompat> {
        return Single.create<MediaControllerCompat> {
            mediaBrowser = MediaBrowserCompat(context, ComponentName(context, RadioPlaybackService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    if (!it.isDisposed) {
                        mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken)

                        it.onSuccess(mediaController)
                    }
                }
            }, null)

            mediaBrowser.connect()
        }
    }

    override fun playbackStateUpdates(): Observable<PlaybackStateCompat> {
        return playbackUpdates.ofType(PlaybackStateCompat::class.java)
    }

    override fun metadataUpdates(): Observable<MediaMetadataCompat> {
        return playbackUpdates.ofType(MediaMetadataCompat::class.java)
    }

    override fun loadRadios(): Single<Resource<List<Radio>>> {
        return Single
                .create<List<MediaBrowserCompat.MediaItem>> {
                    val root = mediaBrowser.root

                    mediaBrowser.unsubscribe(root)
                    mediaBrowser.subscribe(root, object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
                            mediaBrowser.unsubscribe(root)

                            if (!it.isDisposed) {
                                it.onSuccess(children)
                            }
                        }

                        override fun onError(parentId: String) {
                            if (!it.isDisposed) {
                                it.onError(SubscriptionException(parentId))
                            }
                        }
                    })

                    it.setCancellable {
                        if (mediaBrowser.isConnected) {
                            mediaBrowser.unsubscribe(root)
                        }
                    }
                }
                .observeOn(appSchedulers.computation)
                .map {
                    RadioMapper().transform(it)
                }
                .map {
                    Resource.success(it)
                }
    }

    override fun playFromId(id: String) {
        mediaController.transportControls.playFromMediaId(id, null)
    }

    override fun togglePlayPause() {
        when (mediaController.playbackState.state) {
            PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_NONE -> mediaController.transportControls.play()
            PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING -> mediaController.transportControls.pause()
        }
    }

    override fun skipToPrevious() {
        mediaController.transportControls.skipToPrevious()
    }

    override fun skipToNext() {
        mediaController.transportControls.skipToNext()
    }

    override fun clear() {
        compositeDisposable.dispose()

        if (this::mediaBrowser.isInitialized && mediaBrowser.isConnected) {
            mediaBrowser.disconnect()
        }
    }
}
