package com.simonlebras.radiofrance.playback

import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v7.media.MediaRouter
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.simonlebras.radiofrance.di.RadioPlaybackModule.Companion.CAST_KEY
import com.simonlebras.radiofrance.di.RadioPlaybackModule.Companion.LOCAL_KEY
import com.simonlebras.radiofrance.di.ServiceScope
import javax.inject.Inject
import javax.inject.Named

@ServiceScope
class CastSessionManagerListener @Inject constructor(
        private val context: Context,
        private val mediaSession: MediaSessionCompat,
        private val playbackManager: PlaybackManager,
        private @Named(LOCAL_KEY) val localPlaybackFactory: (Context) -> Playback,
        private @Named(CAST_KEY) val castPlaybackFactory: (Context) -> Playback
) : SessionManagerListener<CastSession> {
    private val sessionExtras = Bundle()

    private val mediaRouter = MediaRouter.getInstance(context.applicationContext)

    override fun onSessionEnded(session: CastSession, error: Int) {
        sessionExtras.remove(RadioPlaybackService.EXTRA_CONNECTED_CAST)
        mediaSession.setExtras(sessionExtras)

        val playback = localPlaybackFactory(context)
        mediaRouter.setMediaSessionCompat(null)
        playbackManager.switchToPlayback(playback, false)
    }

    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {}

    override fun onSessionStarted(session: CastSession, sessionId: String) {
        sessionExtras.putString(RadioPlaybackService.EXTRA_CONNECTED_CAST, session.castDevice.friendlyName)
        mediaSession.setExtras(sessionExtras)

        val playback = castPlaybackFactory(context)
        mediaRouter.setMediaSessionCompat(mediaSession)
        playbackManager.switchToPlayback(playback, true)
    }

    override fun onSessionStarting(session: CastSession) {}

    override fun onSessionStartFailed(session: CastSession, error: Int) {}

    override fun onSessionEnding(session: CastSession) {}

    override fun onSessionResuming(session: CastSession, sessionId: String) {}

    override fun onSessionResumeFailed(session: CastSession, error: Int) {}

    override fun onSessionSuspended(session: CastSession, reason: Int) {}
}
