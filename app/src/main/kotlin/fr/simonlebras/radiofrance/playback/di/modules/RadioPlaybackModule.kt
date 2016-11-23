package fr.simonlebras.radiofrance.playback.di.modules

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.modules.ServiceModule
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.RadioPlaybackService
import fr.simonlebras.radiofrance.playback.data.RadioProvider
import fr.simonlebras.radiofrance.playback.data.RadioProviderImpl

@Module
class RadioPlaybackModule(service: RadioPlaybackService) : ServiceModule<RadioPlaybackService>(service) {
    @Provides
    @ServiceScope
    fun provideMediaSession(service: RadioPlaybackService): MediaSessionCompat {
        val mediaSession = MediaSessionCompat(service, RadioPlaybackService.TAG)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)

        // PendingIntent for the media button
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(service, MediaButtonReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(service, 0, mediaButtonIntent, 0)
        mediaSession.setMediaButtonReceiver(pendingIntent)

        return mediaSession
    }

    @Provides
    @ServiceScope
    fun provideRadioProvider(radioProvider: RadioProviderImpl): RadioProvider = radioProvider
}
