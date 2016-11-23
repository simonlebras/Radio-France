package fr.simonlebras.radiofrance.playback

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaSessionCompat
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.RadioFranceApplication
import fr.simonlebras.radiofrance.playback.data.RadioProvider
import fr.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule
import fr.simonlebras.radiofrance.playback.mappers.MediaItemMapper
import fr.simonlebras.radiofrance.utils.LogUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Class providing radio browsing and playback.
 */
class RadioPlaybackService : MediaBrowserServiceCompat() {
    companion object {
        val TAG = LogUtils.makeLogTag(RadioPlaybackService::class.java.simpleName)
        private const val TIMEOUT = 5L // in seconds
    }

    @Inject lateinit var mediaSession: MediaSessionCompat
    @Inject lateinit var radioProvider: RadioProvider
    @Inject lateinit var mapper: MediaItemMapper

    private val component by lazy(LazyThreadSafetyMode.NONE) {
        (application as RadioFranceApplication).component
                .plus(RadioPlaybackModule(this))
    }
    private val compositeDisposable = CompositeDisposable()
    private lateinit var root: String

    override fun onCreate() {
        super.onCreate()

        component.inject(this)

        // Load the radios as soon as possible
        radioProvider.getRadios()

        root = getString(R.string.app_name)

        sessionToken = mediaSession.sessionToken
    }

    override fun onDestroy() {
        compositeDisposable.clear()

        mediaSession.release()

        radioProvider.reset()

        super.onDestroy()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return MediaBrowserServiceCompat.BrowserRoot(root, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()

        compositeDisposable.add(radioProvider.getRadios()
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .firstOrError()
                .map {
                    mapper.transform(it)
                }
                .subscribeWith(object : DisposableSingleObserver<List<MediaBrowserCompat.MediaItem>>() {
                    override fun onSuccess(mediaItems: List<MediaBrowserCompat.MediaItem>) {
                        result.sendResult(mediaItems)
                    }

                    override fun onError(e: Throwable) {
                        result.sendResult(null)
                    }
                }))
    }
}
