package fr.simonlebras.radiofrance.playback.di.modules

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v7.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.di.modules.ServiceModule
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.*
import fr.simonlebras.radiofrance.playback.data.FirebaseService
import fr.simonlebras.radiofrance.playback.data.RadioProvider
import fr.simonlebras.radiofrance.playback.data.RadioProviderImpl
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named

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
    @ServiceScope
    fun provideMediaRouter(context: Context): MediaRouter {
        return MediaRouter.getInstance(context.applicationContext)
    }

    @Provides
    @ServiceScope
    fun provideCastSessionManager(context: Context, castSessionManagerListener: CastSessionManagerListener): SessionManager {
        val castSessionManager = CastContext.getSharedInstance(context).sessionManager
        castSessionManager.addSessionManagerListener(castSessionManagerListener, CastSession::class.java)

        return castSessionManager
    }

    @Provides
    fun provideRemoteMediaClient(context: Context): RemoteMediaClient {
        val castSession = CastContext.getSharedInstance(context.applicationContext).sessionManager.currentCastSession
        return castSession.remoteMediaClient
    }

    @Provides
    @ServiceScope
    @Named("Local")
    fun provideLocalPlaybackFactory(): PlaybackFactory {
        return object : PlaybackFactory {
            override fun create(context: Context): Playback {
                return LocalPlayback(context, provideAudioManager(context), provideWifiLock(context), provideWakeLock(context))
            }
        }
    }

    @Provides
    @ServiceScope
    @Named("Cast")
    fun provideCastPlaybackFactory(radioProvider: RadioProviderImpl): PlaybackFactory {
        return object : PlaybackFactory {
            override fun create(context: Context): Playback {
                return CastPlayback(context, provideRemoteMediaClient(context), radioProvider)
            }
        }
    }
}
