package com.simonlebras.radiofrance.playback

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.playback.PlaybackManager.Companion.EXTRA_CONNECTED_CAST
import com.simonlebras.radiofrance.ui.MainActivity
import com.simonlebras.radiofrance.utils.GlideApp
import androidx.media.app.NotificationCompat as MediaNotificationCompat

private const val CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.MUSIC_CHANNEL_ID"
private const val NOTIFICATION_ID = 1
private const val REQUEST_CODE_CONTROL = 100

class RadioNotificationManager(
    private val service: Service,
    private val mediaSession: MediaSessionCompat
) {
    private val notificationManager = NotificationManagerCompat.from(service)
        .apply {
            cancelAll()
        }

    private val playIntent = NotificationCompat.Action(
        R.drawable.ic_play_arrow,
        service.getString(R.string.action_play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY)
    )
    private val pauseIntent = NotificationCompat.Action(
        R.drawable.ic_pause,
        service.getString(R.string.action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PAUSE)
    )
    private val stopIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, ACTION_STOP)
    private val skipToPreviousIntent = NotificationCompat.Action(
        R.drawable.ic_skip_previous,
        service.getString(R.string.action_previous),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, ACTION_SKIP_TO_PREVIOUS)
    )
    private val skipToNextIntent = NotificationCompat.Action(
        R.drawable.ic_skip_next,
        service.getString(R.string.action_next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(service, ACTION_SKIP_TO_NEXT)
    )
    private val stopCastingIntent = PendingIntent.getBroadcast(
        service,
        REQUEST_CODE_CONTROL,
        Intent(PlaybackManager.ACTION_STOP_CASTING).setPackage(service.packageName),
        PendingIntent.FLAG_CANCEL_CURRENT
    )

    private var isForegroundService = false

    private val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            updateNotification()
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            updateNotification()
        }
    }

    private var controller = mediaSession.controller.apply {
        registerCallback(callback)
    }
    private val notificationSize =
        service.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    private val placeholder by lazy(LazyThreadSafetyMode.NONE) {
        ContextCompat.getDrawable(service, R.drawable.ic_radio)!!
            .apply {
                setColorFilter(Color.WHITE, PorterDuff.Mode.DST_ATOP)
            }
            .toBitmap(notificationSize, notificationSize)
    }
    private var target: SimpleTarget<Bitmap>? = null

    fun updateNotification() {
        val state = controller.playbackState.state

        val notification = if (state != PlaybackStateCompat.STATE_NONE) {
            createNotification()
        } else {
            null
        }

        when (state) {
            PlaybackStateCompat.STATE_BUFFERING,
            PlaybackStateCompat.STATE_PLAYING -> {
                service.startForeground(NOTIFICATION_ID, notification)

                isForegroundService = true
            }
            else -> {
                if (isForegroundService) {
                    if (notification != null) {
                        service.stopForeground(false)
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    } else {
                        service.stopForeground(true)
                    }

                    isForegroundService = false
                }
            }
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        val description = controller.metadata.description
        val playbackState = controller.playbackState.state

        return NotificationCompat.Builder(service, CHANNEL_ID)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setCancelButtonIntent(stopIntent)
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowCancelButton(true)
                    .setShowActionsInCompactView(1)
            )
            .setSmallIcon(R.drawable.ic_radio)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(MainActivity.createNotificationIntent(service))
            .setContentTitle(description.title)
            .setDeleteIntent(stopIntent)
            .setContentText(description.subtitle)
            .setOnlyAlertOnce(true)
            .apply {
                addAction(skipToPreviousIntent)

                if (playbackState == STATE_PLAYING) {
                    addAction(pauseIntent)
                } else {
                    addAction(playIntent)
                }

                addAction(skipToNextIntent)

                controller.extras?.getString(EXTRA_CONNECTED_CAST)?.also {
                    setSubText(
                        service.resources.getString(
                            R.string.casting_to_device,
                            it
                        )
                    )

                    addAction(
                        R.drawable.ic_close,
                        service.getString(R.string.action_stop_casting),
                        stopCastingIntent
                    )
                }

                val logoUrl = description.iconUri!!

                getLogoFromCache(logoUrl)?.also {
                    setLargeIcon(it)

                    return@apply
                }


                setLargeIcon(placeholder)

                GlideApp.with(service).clear(target)

                target = GlideApp.with(service)
                    .asBitmap()
                    .load(logoUrl)
                    .override(notificationSize, notificationSize)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            if (logoUrl != controller.metadata.description.iconUri ||
                                playbackState != controller.playbackState.state
                            ) {
                                return
                            }

                            setLargeIcon(resource)

                            notificationManager.notify(NOTIFICATION_ID, build())
                        }
                    })
            }
            .build()
    }

    private fun getLogoFromCache(logoUrl: Uri): Bitmap? {
        val future = GlideApp.with(service)
            .asBitmap()
            .onlyRetrieveFromCache(true)
            .load(logoUrl)
            .override(notificationSize, notificationSize)
            .submit()

        return if (future.isDone) {
            future.get()
        } else {
            future.cancel(true)

            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = service.getSystemService<NotificationManager>()!!

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    service.getString(R.string.playback_notification_channel),
                    NotificationManager.IMPORTANCE_LOW
                )

                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun cancel() {
        controller.unregisterCallback(callback)

        GlideApp.with(service).clear(target)

        if (isForegroundService) {
            service.stopForeground(true)
        }

        notificationManager.cancelAll()
    }
}
