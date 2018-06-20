package com.simonlebras.radiofrance.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.simonlebras.radiofrance.data.repository.RadioRepository
import com.simonlebras.radiofrance.data.repository.RadioRepositoryImpl
import com.simonlebras.radiofrance.playback.*
import com.simonlebras.radiofrance.ui.MainActivity
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

@Module
class RadioPlaybackModule {
    companion object {
        const val LOCAL_KEY = "LOCAL_KEY"
        const val CAST_KEY = "CAST_KEY"
    }

    @Provides
    @ServiceScope
    fun provideMediaSessionCompat(
        context: Context,
        mediaSessionCallback: MediaSessionCallback
    ): MediaSessionCompat {
        val mediaSession = MediaSessionCompat(context, RadioPlaybackService.TAG).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
            setCallback(mediaSessionCallback)
        }

        // PendingIntent for the media button
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            setClass(context, MediaButtonReceiver::class.java)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0)
        mediaSession.setMediaButtonReceiver(pendingIntent)

        mediaSession.setSessionActivity(MainActivity.createSessionIntent(context))

        return mediaSession
    }

    @Provides
    fun provideRadioProvider(radioProvider: RadioRepositoryImpl): RadioRepository = radioProvider

    @Provides
    @ServiceScope
    fun provideCastSessionManager(
        context: Context,
        castSessionManagerListener: CastSessionManagerListener
    ): SessionManager {
        val castSessionManager = CastContext.getSharedInstance(context)
            .sessionManager
        castSessionManager.addSessionManagerListener(
            castSessionManagerListener,
            CastSession::class.java
        )

        return castSessionManager
    }

    @Provides
    @ServiceScope
    @Named(LOCAL_KEY)
    fun provideLocalPlaybackFactory(provider: Provider<LocalPlayback>): Function1<@JvmWildcard Context, @JvmWildcard Playback> =
        { provider.get() }

    @Provides
    @ServiceScope
    @Named(CAST_KEY)
    fun provideCastPlaybackFactory(provider: Provider<CastPlayback>): Function1<@JvmWildcard Context, @JvmWildcard Playback> =
        { provider.get() }
}