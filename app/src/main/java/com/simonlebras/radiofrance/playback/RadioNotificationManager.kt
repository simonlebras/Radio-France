package com.simonlebras.radiofrance.playback

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.RemoteException
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.ui.MainActivity
import com.simonlebras.radiofrance.utils.GlideApp
import timber.log.Timber
import javax.inject.Inject

class RadioNotificationManager @Inject constructor(
    private val context: Context,
    private val service: RadioPlaybackService
) : BroadcastReceiver() {
    private companion object {
        const val CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.MUSIC_CHANNEL_ID"

        const val NOTIFICATION_ID = 1

        const val REQUEST_CODE_CONTROL = 100

        const val ACTION_PLAY = "${BuildConfig.APPLICATION_ID}.playFromId"
        const val ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.pause"
        const val ACTION_PREV = "${BuildConfig.APPLICATION_ID}.prev"
        const val ACTION_NEXT = "${BuildConfig.APPLICATION_ID}.next"
        const val ACTION_STOP_CASTING = "${BuildConfig.APPLICATION_ID}.stopCasting"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    private val pauseIntent = PendingIntent.getBroadcast(
        service,
        REQUEST_CODE_CONTROL,
        Intent(ACTION_PAUSE).setPackage(service.packageName),
        PendingIntent.FLAG_CANCEL_CURRENT
    )
    private val playIntent = PendingIntent.getBroadcast(
        service,
        REQUEST_CODE_CONTROL,
        Intent(ACTION_PLAY).setPackage(service.packageName),
        PendingIntent.FLAG_CANCEL_CURRENT
    )
    private val previousIntent = PendingIntent.getBroadcast(
        service,
        REQUEST_CODE_CONTROL,
        Intent(ACTION_PREV).setPackage(service.packageName),
        PendingIntent.FLAG_CANCEL_CURRENT
    )
    private val nextIntent = PendingIntent.getBroadcast(
        service,
        REQUEST_CODE_CONTROL,
        Intent(ACTION_NEXT).setPackage(service.packageName),
        PendingIntent.FLAG_CANCEL_CURRENT
    )
    private val stopCastingIntent = PendingIntent.getBroadcast(
        service,
        REQUEST_CODE_CONTROL,
        Intent(ACTION_STOP_CASTING).setPackage(service.packageName),
        PendingIntent.FLAG_CANCEL_CURRENT
    )

    private var sessionToken: MediaSessionCompat.Token? = null
    private var controller: MediaControllerCompat? = null
    private lateinit var transportControls: MediaControllerCompat.TransportControls
    private var playbackState: PlaybackStateCompat? = null
    private var metadata: MediaMetadataCompat? = null
    private var started = false
    private val notificationSize =
        service.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    private lateinit var target: SimpleTarget<Bitmap>
    private val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            playbackState = state

            if ((state.state == STATE_STOPPED) || (state.state == STATE_NONE) || (state.state == STATE_ERROR)) {
                stopNotification()
            } else {
                createNotification()?.let {
                    notificationManager.notify(NOTIFICATION_ID, it)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            this@RadioNotificationManager.metadata = metadata

            createNotification()?.let {
                notificationManager.notify(NOTIFICATION_ID, it)
            }
        }

        override fun onSessionDestroyed() {
            try {
                updateSessionToken()
            } catch (e: RemoteException) {
            }

            super.onSessionDestroyed()
        }
    }

    init {
        notificationManager.cancelAll()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> transportControls.play()
            ACTION_PAUSE -> transportControls.pause()
            ACTION_PREV -> transportControls.skipToPrevious()
            ACTION_NEXT -> transportControls.skipToNext()
            ACTION_STOP_CASTING -> {
                val stopIntent = Intent(context, RadioPlaybackService::class.java).apply {
                    action = RadioPlaybackService.ACTION_CMD
                    putExtra(
                        RadioPlaybackService.EXTRAS_CMD_NAME,
                        RadioPlaybackService.CMD_STOP_CASTING
                    )
                }

                service.startService(stopIntent)
            }
            else -> {
                if (BuildConfig.DEBUG) {
                    Timber.e("Unknown action: %s", intent.action)
                }
            }
        }
    }

    fun startNotification() {
        if (!started) {
            metadata = controller!!.metadata

            playbackState = controller!!.playbackState

            createNotification()?.let {
                controller!!.registerCallback(callback)

                val filter = IntentFilter().apply {
                    addAction(ACTION_PLAY)
                    addAction(ACTION_PAUSE)
                    addAction(ACTION_PREV)
                    addAction(ACTION_NEXT)
                    addAction(ACTION_STOP_CASTING)
                }

                service.registerReceiver(this, filter)

                service.startForeground(NOTIFICATION_ID, it)

                started = true
            }
        }
    }

    fun stopNotification() {
        if (started) {
            started = false

            controller!!.unregisterCallback(callback)

            try {
                notificationManager.cancel(NOTIFICATION_ID)

                service.unregisterReceiver(this)
            } catch (ex: IllegalArgumentException) {
            }

            service.stopForeground(true)
        }
    }

    fun updateSessionToken() {
        val token = service.sessionToken
        if (((sessionToken == null) && (token != null)) ||
            ((sessionToken != null) && (sessionToken != token))
        ) {
            if (controller != null) {
                controller!!.unregisterCallback(callback)
            }

            sessionToken = token

            sessionToken?.let {
                controller = MediaControllerCompat(service, it)

                transportControls = controller!!.transportControls

                if (started) {
                    controller!!.registerCallback(callback)
                }
            }
        }
    }

    fun reset() {
        GlideApp.with(context)
            .clear(target)

        stopNotification()
    }

    private fun createContentIntent(): PendingIntent {
        val contentIntent = Intent(service, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            service,
            MainActivity.REQUEST_CODE_NOTIFICATION,
            contentIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification? {
        if ((metadata == null) || (playbackState == null)) {
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val builder = NotificationCompat.Builder(service, CHANNEL_ID)

        var playPauseButtonPosition = 0
        if (playbackState!!.actions and ACTION_SKIP_TO_PREVIOUS != 0L) {
            builder.addAction(
                R.drawable.ic_skip_previous_white_24dp,
                service.getString(R.string.action_previous),
                previousIntent
            )

            playPauseButtonPosition = 1
        }

        addPlayPauseAction(builder)

        if (playbackState!!.actions and ACTION_SKIP_TO_NEXT != 0L) {
            builder.addAction(
                R.drawable.ic_skip_next_white_24dp,
                service.getString(R.string.action_next),
                nextIntent
            )
        }

        val description = metadata!!.description

        builder
            .setStyle(
                android.support.v4.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(playPauseButtonPosition)
                    .setMediaSession(sessionToken)
            )
            .setSmallIcon(R.drawable.ic_radio_white_24dp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(createContentIntent())
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(false)

        val extras = controller?.extras
        if (extras != null) {
            val castName = extras.getString(RadioPlaybackService.EXTRA_CONNECTED_CAST)
            if (castName != null) {
                val castInfo = service.resources.getString(R.string.casting_to_device, castName)
                builder.setSubText(castInfo)
                    .addAction(
                        R.drawable.ic_close_black_24dp,
                        service.getString(R.string.action_stop_casting),
                        stopCastingIntent
                    )
            }
        }

        setNotificationPlaybackState(builder)

        val logoUrl = metadata?.description?.iconUri
        target = object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (metadata?.description?.iconUri == logoUrl) {
                    builder.setLargeIcon(resource)

                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                }
            }
        }

        GlideApp.with(context)
            .asBitmap()
            .load(logoUrl)
            .placeholder(R.drawable.ic_radio_blue_64dp)
            .override(notificationSize, notificationSize)
            .into(target)

        return builder.build()
    }

    private fun addPlayPauseAction(builder: NotificationCompat.Builder) {
        if (playbackState!!.state == STATE_PLAYING) {
            builder.addAction(
                R.drawable.ic_pause_white_24dp,
                service.getString(R.string.action_pause),
                pauseIntent
            )
        } else {
            builder.addAction(
                R.drawable.ic_play_arrow_white_24dp,
                service.getString(R.string.action_play),
                playIntent
            )
        }
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        if (playbackState == null || !started) {
            service.stopForeground(true)
            return
        }

        builder.setOngoing(playbackState!!.state == STATE_PLAYING)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
