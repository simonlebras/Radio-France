package fr.simonlebras.radiofrance.ui.browser.manager

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.playback.RadioPlaybackService
import fr.simonlebras.radiofrance.ui.browser.exceptions.SubscriptionException
import fr.simonlebras.radiofrance.ui.browser.mappers.RadioMapper
import fr.simonlebras.radiofrance.utils.OnErrorRetryCache
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ActivityScope
class RadioManagerImpl @Inject constructor(val context: Context) : RadioManager {
    private companion object {
        private const val TIMEOUT = 10L // in seconds
    }

    override val connection: Observable<MediaControllerCompat> by lazy(LazyThreadSafetyMode.NONE) {
        Observable
                .create<MediaControllerCompat> {
                    mediaBrowser = MediaBrowserCompat(context, ComponentName(context, RadioPlaybackService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                        override fun onConnected() {
                            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken)
                            if (!it.isDisposed) {
                                it.onNext(mediaController)
                            }
                        }
                    }, null)

                    it.setCancellable {
                        mediaBrowser.disconnect()
                    }

                    mediaBrowser.connect()
                }
                .replay(1)
                .autoConnect(1) {
                    compositeDisposable.add(it)
                }
    }

    override val radios: Observable<List<Radio>> by lazy(LazyThreadSafetyMode.NONE) {
        val source = Observable
                .create<List<MediaBrowserCompat.MediaItem>> {
                    val root = mediaBrowser.root
                    mediaBrowser.unsubscribe(root)
                    mediaBrowser.subscribe(root, object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
                            mediaBrowser.unsubscribe(root)
                            if (!it.isDisposed) {
                                if (children.isEmpty()) {
                                    it.onError(SubscriptionException(parentId))
                                    return
                                }

                                it.onNext(children)
                                it.onComplete()
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
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .observeOn(Schedulers.computation())
                .map {
                    RadioMapper.transform(it)
                }

        retryCache = OnErrorRetryCache(source)
        retryCache!!.result
    }

    override val playbackUpdates: Observable<Any> by lazy(LazyThreadSafetyMode.NONE) {
        Observable.create<Any> {
            val callback = object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
                    it.onNext(state)
                }

                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    if (metadata != null) {
                        it.onNext(metadata)
                    }
                }
            }

            it.setCancellable {
                mediaController.unregisterCallback(callback)
            }

            mediaController.registerCallback(callback)
        }
    }

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private val compositeDisposable = CompositeDisposable()
    private var retryCache: OnErrorRetryCache<List<Radio>>? = null

    override fun play() {
        mediaController.transportControls.play()
    }

    override fun play(id: String) {
        mediaController.transportControls.playFromMediaId(id, null)
    }

    override fun pause() {
        mediaController.transportControls.pause()
    }

    override fun skipToPrevious() {
        mediaController.transportControls.skipToPrevious()
    }

    override fun skipToNext() {
        mediaController.transportControls.skipToNext()
    }

    override fun reset() {
        retryCache?.reset()
        compositeDisposable.clear()
    }
}
