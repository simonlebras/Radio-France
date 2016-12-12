package fr.simonlebras.radiofrance.playback.di.modules

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.PowerManager
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.modules.ServiceModule
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.LocalPlayback
import fr.simonlebras.radiofrance.playback.MediaSessionCallback
import fr.simonlebras.radiofrance.playback.Playback
import fr.simonlebras.radiofrance.playback.RadioPlaybackService
import fr.simonlebras.radiofrance.playback.data.FirebaseService
import fr.simonlebras.radiofrance.playback.data.RadioProvider
import fr.simonlebras.radiofrance.playback.data.RadioProviderImpl
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
class RadioPlaybackModule(service: RadioPlaybackService) : ServiceModule<RadioPlaybackService>(service) {
    private companion object {
        private const val LOCK_NAME = "Radio France lock"

        private const val BASE_URL = "https://radio-france-77818.firebaseapp.com"
    }

    @Provides
    @ServiceScope
    fun provideMediaSessionCompat(context: Context, mediaSessionCallback: MediaSessionCallback): MediaSessionCompat {
        val mediaSession = MediaSessionCompat(context, RadioPlaybackService.TAG)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mediaSession.setCallback(mediaSessionCallback)

        // PendingIntent for the media button
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(context, MediaButtonReceiver::class.java)
        var pendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0)
        mediaSession.setMediaButtonReceiver(pendingIntent)

        val intent = Intent(context, RadioBrowserActivity::class.java)
        pendingIntent = PendingIntent.getActivity(context, RadioBrowserActivity.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        mediaSession.setSessionActivity(pendingIntent)

        return mediaSession
    }

    @Provides
    @ServiceScope
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
    }

    @Provides
    @ServiceScope
    fun provideRadioService(retrofit: Retrofit): FirebaseService = retrofit.create(FirebaseService::class.java)

    @Provides
    @ServiceScope
    fun provideRadioProvider(radioProvider: RadioProviderImpl): RadioProvider = radioProvider

    @Provides
    @ServiceScope
    fun provideNotificationManagerCompat(context: Context): NotificationManagerCompat = NotificationManagerCompat.from(context)

    @Provides
    fun provideAudioManager(context: Context) = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    fun provideWifiLock(context: Context): WifiManager.WifiLock {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.createWifiLock(LOCK_NAME)
    }

    @Provides
    fun provideWakeLock(context: Context): PowerManager.WakeLock {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME)
    }

    @Provides
    fun provideExoPlayer(context: Context): (() -> SimpleExoPlayer) {
        val trackSelector = DefaultTrackSelector(Handler())

        val loadControl = DefaultLoadControl()

        return { ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl) }
    }

    @Provides
    fun provideLocalPlayback(playback: LocalPlayback): Playback = playback
}
