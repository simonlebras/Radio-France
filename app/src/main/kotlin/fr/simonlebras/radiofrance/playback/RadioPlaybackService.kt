package fr.simonlebras.radiofrance.playback

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import fr.simonlebras.radiofrance.BuildConfig
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
class RadioPlaybackService : MediaBrowserServiceCompat(), PlaybackManager.Callback, QueueManager.Listener {
    companion object {
        val TAG = LogUtils.makeLogTag(RadioPlaybackService::class.java.simpleName)

        const val ACTION_CMD = "${BuildConfig.APPLICATION_ID}.ACTION_CMD"

        const val EXTRAS_CMD_NAME = "${BuildConfig.APPLICATION_ID}.EXTRAS_CMD_NAME"
        const val EXTRA_CONNECTED_CAST = "${BuildConfig.APPLICATION_ID}.EXTRAS_CAST_NAME"

        const val CMD_PAUSE = "CMD_PAUSE"
        const val CMD_STOP_CASTING = "CMD_STOP_CASTING"

        private const val STOP_DELAY = 30000L // in milliseconds

        private const val TIMEOUT = 10L // in seconds
    }

    @Inject lateinit var mediaSession: MediaSessionCompat
    @Inject lateinit var radioProvider: RadioProvider
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var radioNotificationManager: RadioNotificationManager
    @Inject lateinit var delayedStopHandler: DelayedStopHandler
    @Inject lateinit var castSessionManager: SessionManager
    @Inject lateinit var castSessionManagerListener: CastSessionManagerListener

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
        compositeDisposable.add(radioProvider.radios
                .firstOrError()
                .subscribeWith(object : DisposableSingleObserver<List<MediaMetadataCompat>>() {
                    override fun onSuccess(value: List<MediaMetadataCompat>) {
                    }

                    override fun onError(e: Throwable) {
                    }
                }))

        playbackManager.callback = this

        playbackManager.queueManager.listener = this

        root = getString(R.string.app_name)

        sessionToken = mediaSession.sessionToken

        radioNotificationManager.updateSessionToken()

        playbackManager.updatePlaybackState(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val command = intent.getStringExtra(EXTRAS_CMD_NAME)
            if (ACTION_CMD == intent.action) {
                if (CMD_PAUSE == command) {
                    playbackManager.pause()
                } else if (CMD_STOP_CASTING == command) {
                    castSessionManager.endCurrentSession(true)
                }
            } else {
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
        }

        delayedStopHandler.removeCallbacksAndMessages(null)
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        playbackManager.stop(null)

        radioNotificationManager.reset()

        castSessionManager.removeSessionManagerListener(castSessionManagerListener, CastSession::class.java)

        compositeDisposable.clear()

        radioProvider.reset()

        delayedStopHandler.removeCallbacksAndMessages(null)

        mediaSession.release()

        super.onDestroy()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return MediaBrowserServiceCompat.BrowserRoot(root, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()

        compositeDisposable.add(radioProvider.radios
                .timeout(TIMEOUT, TimeUnit.SECONDS)
                .firstOrError()
                .map {
                    MediaItemMapper.transform(it)
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

    override fun onPlaybackStart() {
        if (!mediaSession.isActive) {
            mediaSession.isActive = true
        }

        delayedStopHandler.removeCallbacksAndMessages(null)

        startService(Intent(this, RadioPlaybackService::class.java))
    }

    override fun onPlaybackStop() {
        delayedStopHandler.removeCallbacksAndMessages(null)
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY)

        stopForeground(true)
    }

    override fun onNotificationRequired() {
        radioNotificationManager.startNotification()
    }

    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        mediaSession.setPlaybackState(playbackState)
    }

    override fun onQueueUpdated(title: String, queue: List<MediaSessionCompat.QueueItem>) {
        mediaSession.setQueueTitle(title)
        mediaSession.setQueue(queue)
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        mediaSession.setMetadata(metadata)
    }

    override fun onMetadataRetrieveError() {
        playbackManager.updatePlaybackState(getString(R.string.error_radio_unavailable))
    }
}
