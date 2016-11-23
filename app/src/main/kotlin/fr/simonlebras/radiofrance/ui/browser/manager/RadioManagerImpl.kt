package fr.simonlebras.radiofrance.ui.browser.manager

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.playback.RadioPlaybackService
import fr.simonlebras.radiofrance.ui.browser.exceptions.SubscriptionException
import fr.simonlebras.radiofrance.ui.browser.mappers.RadioMapper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@ActivityScope
class RadioManagerImpl @Inject constructor(val context: Context, val mapper: RadioMapper) : RadioManager {
    override var cache: List<Radio>? = null

    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var mediaController: MediaControllerCompat
    private val compositeDisposable = CompositeDisposable()
    private val connection: Observable<MediaControllerCompatWrapper> by lazy(LazyThreadSafetyMode.NONE) {
        Observable
                .create<MediaControllerCompatWrapper> {
                    mediaBrowser = MediaBrowserCompat(context, ComponentName(context, RadioPlaybackService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
                        override fun onConnected() {
                            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken)
                            if (!it.isDisposed) {
                                it.onNext(MediaControllerCompatWrapper(mediaController))
                            }
                        }
                    }, null)

                    it.setCancellable {
                        mediaBrowser.disconnect()
                    }

                    mediaBrowser.connect()
                }
                .replay(1)
                .autoConnect(1, {
                    compositeDisposable.add(it)
                })
    }

    override fun connect() = connection

    override fun getRadios(): Single<List<Radio>> {
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
                .observeOn(Schedulers.computation())
                .map {
                    mapper.transform(it)
                }
    }

    override fun reset() {
        compositeDisposable.clear()
    }
}
