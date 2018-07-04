package com.simonlebras.radiofrance.playback

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.session.MediaButtonReceiver
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.data.repository.MediaRepository
import com.simonlebras.radiofrance.utils.AppContexts
import dagger.android.AndroidInjection
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import javax.inject.Inject

class RadioPlaybackService : MediaBrowserServiceCompat() {
    @Inject
    lateinit var appContexts: AppContexts

    @Inject
    lateinit var mediaRepository: MediaRepository

    @Inject
    lateinit var playbackManager: PlaybackManager

    private val parentJob = Job()

    override fun onCreate() {
        AndroidInjection.inject(this)

        super.onCreate()

        // Load the media items as soon as possible
        launch(context = CommonPool, parent = parentJob) {
            try {
                mediaRepository.loadMediaItemsAsync().await()
            } finally {
            }
        }

        sessionToken = playbackManager.mediaSession.sessionToken
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent != null) {
            when (intent.action) {
                LocalPlayback.ACTION_PAUSE -> playbackManager.onPause()
                RadioNotificationManager.ACTION_STOP_CASTING -> playbackManager.stopCasting()
                else -> MediaButtonReceiver.handleIntent(playbackManager.mediaSession, intent)
            }
        }

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        parentJob.cancel()

        playbackManager.release()

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        stopSelf()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot =
        MediaBrowserServiceCompat.BrowserRoot(getString(R.string.app_name), null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()

        launch(appContexts.computation, parent = parentJob) {
            try {
                val mediaItems = mediaRepository.loadMediaItemsAsync().await()

                if (!playbackManager.isInitialized) {
                    playbackManager.initialize(mediaItems)
                }

                result.sendResult(mediaItems)
            } catch (_: Exception) {
                result.sendError(null)
            }
        }
    }
}
