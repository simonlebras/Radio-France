package com.simonlebras.radiofrance.playback

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.core.os.bundleOf
import androidx.media.session.MediaButtonReceiver
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.ui.MainActivity
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import javax.inject.Inject

class PlaybackManager @Inject constructor(
    private val service: RadioPlaybackService
) : MediaSessionCompat.Callback(),
    SessionManagerListener<CastSession>,
    Playback.Callback,
    QueueManager.Callback {
    @Volatile
    var isInitialized = false
    private val mutex = Mutex()

    private var queueManager = QueueManager(emptyList(), this)
    private var castSessionManager: SessionManager? = null

    val mediaSession = MediaSessionCompat(service, service.getString(R.string.app_name)).apply {
        setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        setCallback(this@PlaybackManager)

        // PendingIntent for the media button
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setClass(service, MediaButtonReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(service, 0, mediaButtonIntent, 0)
        setMediaButtonReceiver(pendingIntent)

        setSessionActivity(MainActivity.createSessionIntent(service))

        isActive = true
    }

    private var playback: Playback = LocalPlayback(service, this)

    private val radioNotificationManager = RadioNotificationManager(service, mediaSession)

    private val stopCastingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val stopIntent = Intent(context, RadioPlaybackService::class.java).apply {
                action = PlaybackManager.ACTION_STOP_CASTING
            }

            service.startService(stopIntent)
        }
    }

    private val mediaRouter = MediaRouter.getInstance(service)

    init {
        updatePlaybackState(STATE_NONE, null)

        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(service) == ConnectionResult.SUCCESS) {
            castSessionManager = CastContext.getSharedInstance(service)
                .sessionManager
                .apply {
                    addSessionManagerListener(this@PlaybackManager, CastSession::class.java)
                }
        }
    }

    suspend fun initialize(mediaItems: List<MediaBrowserCompat.MediaItem>) {
        mutex.withLock {
            if (isInitialized) {
                return
            }

            val queue = mediaItems.mapIndexed { index, mediaItem ->
                MediaSessionCompat.QueueItem(mediaItem.description, index.toLong())
            }

            queueManager.queue = queue

            mediaSession.setQueueTitle(service.getString(R.string.app_name))
            mediaSession.setQueue(queue)

            isInitialized = true
        }
    }

    override fun onSessionEnded(session: CastSession, error: Int) {
        service.unregisterReceiver(stopCastingReceiver)

        mediaSession.setExtras(null)

        mediaRouter.setMediaSessionCompat(null)

        switchToPlayback(LocalPlayback(service, this))
    }

    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {}

    override fun onSessionStarted(session: CastSession, sessionId: String) {
        service.registerReceiver(
            stopCastingReceiver,
            IntentFilter(PlaybackManager.ACTION_STOP_CASTING)
        )

        val sessionExtras = bundleOf(EXTRA_CONNECTED_CAST to session.castDevice.friendlyName)
        mediaSession.setExtras(sessionExtras)

        mediaRouter.setMediaSessionCompat(mediaSession)

        switchToPlayback(CastPlayback(service, castSessionManager!!, this))
    }

    override fun onSessionStarting(session: CastSession) {}

    override fun onSessionStartFailed(session: CastSession, error: Int) {}

    override fun onSessionEnding(session: CastSession) {}

    override fun onSessionResuming(session: CastSession, sessionId: String) {}

    override fun onSessionResumeFailed(session: CastSession, error: Int) {}

    override fun onSessionSuspended(session: CastSession, reason: Int) {}

    override fun onPlay() {
        if (!isInitialized || playback.isPlaying) {
            return
        }

        queueManager.currentItem?.let {
            startPlayback(it)
        }
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        if (!isInitialized ||
            mediaId == null ||
            (mediaId == queueManager.currentItem?.description?.mediaId && playback.isPlaying)
        ) {
            return
        }

        queueManager.skipToItem(mediaId)
    }

    override fun onPause() {
        if (!isInitialized || !playback.isPlaying) {
            return
        }

        playback.pause()
    }

    override fun onStop() {
        if (!isInitialized || !playback.isPlaying) {
            return
        }

        playback.stop()
    }

    override fun onSkipToQueueItem(id: Long) {
        if (!isInitialized) {
            return
        }

        queueManager.skipToPosition(id)
    }

    override fun onSkipToPrevious() {
        if (!isInitialized) {
            return
        }

        queueManager.skipToPrevious()
    }

    override fun onSkipToNext() {
        if (!isInitialized) {
            return
        }

        queueManager.skipToNext()
    }

    override fun onPlaybackStateChanged(state: Int) {
        updatePlaybackState(state, null)
    }

    override fun onError(error: String) {
        updatePlaybackState(STATE_ERROR, error)
    }

    override fun onQueueItemChanged(item: MediaSessionCompat.QueueItem) {
        startPlayback(item)

        with(item.description) {
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title.toString())
                .putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                    description.toString()
                )
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, iconUri.toString())
                .build()

            mediaSession.setMetadata(metadata)
        }
    }

    private fun startPlayback(item: MediaSessionCompat.QueueItem) {
        playback.play(item)

        service.startService(Intent(service, RadioPlaybackService::class.java))
    }

    private fun updatePlaybackState(state: Int, error: String?) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(getAvailableActions())
            .setState(
                state,
                PLAYBACK_POSITION_UNKNOWN,
                1.0f,
                SystemClock.elapsedRealtime()
            )
            .apply {
                error?.let {
                    setErrorMessage(ERROR_CODE_UNKNOWN_ERROR, it)
                }

                queueManager.currentItem?.let {
                    setActiveQueueItemId(it.queueId)
                }
            }
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun switchToPlayback(newPlayback: Playback) {
        val wasPlaying = playback.isPlaying

        playback.release()

        playback = newPlayback

        if (wasPlaying) {
            playback.play(queueManager.currentItem!!)
        }
    }

    private fun getAvailableActions(): Long {
        val actions = ACTION_PLAY_PAUSE or
                ACTION_PLAY_FROM_MEDIA_ID or
                ACTION_PLAY_FROM_SEARCH or
                ACTION_SKIP_TO_PREVIOUS or
                ACTION_SKIP_TO_NEXT

        return if (playback.isPlaying) {
            actions or ACTION_PAUSE
        } else {
            actions or ACTION_PLAY
        }
    }

    fun stopCasting() {
        castSessionManager?.endCurrentSession(true)
    }

    fun release() {
        mediaSession.release()

        playback.release()

        radioNotificationManager.cancel()

        try {
            service.unregisterReceiver(stopCastingReceiver)
        } catch (e: IllegalArgumentException) {
        }

        castSessionManager?.let {
            it.removeSessionManagerListener(this, CastSession::class.java)
            it.endCurrentSession(true)
        }
    }

    companion object {
        const val ACTION_STOP_CASTING = "${BuildConfig.APPLICATION_ID}.ACTION_STOP_CASTING"

        const val EXTRA_CONNECTED_CAST = "${BuildConfig.APPLICATION_ID}.EXTRAS_CAST_NAME"
    }
}
