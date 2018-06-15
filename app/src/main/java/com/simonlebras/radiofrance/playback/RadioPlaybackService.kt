package com.simonlebras.radiofrance.playback

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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.data.repository.RadioRepository
import com.simonlebras.radiofrance.playback.mappers.MediaItemMapper
import dagger.Lazy
import dagger.android.AndroidInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import javax.inject.Inject


/**
 * Class providing radio browsing and playback.
 */
class RadioPlaybackService : MediaBrowserServiceCompat(), PlaybackManager.Callback, QueueManager.Listener {
    companion object {
        val TAG: String = RadioPlaybackService::class.java.simpleName

        const val ACTION_CMD = "${BuildConfig.APPLICATION_ID}.ACTION_CMD"

        const val EXTRAS_CMD_NAME = "${BuildConfig.APPLICATION_ID}.EXTRAS_CMD_NAME"
        const val EXTRA_CONNECTED_CAST = "${BuildConfig.APPLICATION_ID}.EXTRAS_CAST_NAME"

        const val CMD_PAUSE = "CMD_PAUSE"
        const val CMD_STOP_CASTING = "CMD_STOP_CASTING"
    }

    @Inject
    lateinit var mediaSession: MediaSessionCompat
    @Inject
    lateinit var radioRepository: RadioRepository
    @Inject
    lateinit var playbackManager: PlaybackManager
    @Inject
    lateinit var radioNotificationManager: RadioNotificationManager
    @Inject
    lateinit var castSessionManagerProvider: Lazy<SessionManager>
    @Inject
    lateinit var castSessionManagerListenerProvider: Lazy<CastSessionManagerListener>

    private val compositeDisposable = CompositeDisposable()
    private lateinit var root: String

    private var castSessionManager: SessionManager? = null
    private var castSessionManagerListener: CastSessionManagerListener? = null

    override fun onCreate() {
        AndroidInjection.inject(this)

        super.onCreate()

        // Load the radios as soon as possible
        compositeDisposable.add(radioRepository.radios
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

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            castSessionManager = castSessionManagerProvider.get()
            castSessionManagerListener = castSessionManagerListenerProvider.get()
            castSessionManager!!.addSessionManagerListener(castSessionManagerListener!!, CastSession::class.java)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val command = intent.getStringExtra(EXTRAS_CMD_NAME)
            if (ACTION_CMD == intent.action) {
                if (CMD_PAUSE == command) {
                    playbackManager.pause()
                } else if (CMD_STOP_CASTING == command) {
                    castSessionManager!!.endCurrentSession(true)
                }
            } else {
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        playbackManager.stop(null)

        radioNotificationManager.reset()

        castSessionManager?.removeSessionManagerListener(castSessionManagerListener!!, CastSession::class.java)

        compositeDisposable.clear()

        radioRepository.reset()

        mediaSession.release()

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        stopSelf()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot =
            MediaBrowserServiceCompat.BrowserRoot(root, null)

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()

        compositeDisposable.add(radioRepository.radios
                                        .firstOrError()
                                        .map {
                                            MediaItemMapper().transform(it)
                                        }
                                        .subscribeWith(object : DisposableSingleObserver<List<MediaBrowserCompat.MediaItem>>() {
                                            override fun onSuccess(mediaItems: List<MediaBrowserCompat.MediaItem>) {
                                                result.sendResult(mediaItems)
                                            }

                                            override fun onError(e: Throwable) {
                                                result.sendError(null)
                                            }
                                        }))
    }

    override fun onPlaybackStart() {
        if (!mediaSession.isActive) {
            mediaSession.isActive = true
        }

        startService(Intent(this, RadioPlaybackService::class.java))
    }

    override fun onPlaybackStop() {
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
