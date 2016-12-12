package fr.simonlebras.radiofrance.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.RemoteException
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import fr.simonlebras.radiofrance.BuildConfig
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.ui.browser.RadioBrowserActivity
import fr.simonlebras.radiofrance.utils.DebugUtils
import fr.simonlebras.radiofrance.utils.LogUtils
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils
import timber.log.Timber
import javax.inject.Inject

@ServiceScope
class RadioNotificationManager @Inject constructor(val context: Context,
                                                   val service: RadioPlaybackService,
                                                   val notificationManager: NotificationManagerCompat) : BroadcastReceiver() {
    private companion object {
        private val TAG = LogUtils.makeLogTag(RadioNotificationManager::class.java.simpleName)

        private const val NOTIFICATION_ID = 1

        private const val REQUEST_CODE = 1

        private const val ACTION_PLAY = "${BuildConfig.APPLICATION_ID}.play"
        private const val ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.pause"
        private const val ACTION_PREV = "${BuildConfig.APPLICATION_ID}.prev"
        private const val ACTION_NEXT = "${BuildConfig.APPLICATION_ID}.next"
    }

    private val packageName = service.packageName
    private val pauseIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
            Intent(ACTION_PAUSE).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT)
    private val playIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
            Intent(ACTION_PLAY).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT)
    private val previousIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
            Intent(ACTION_PREV).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT)
    private val nextIntent = PendingIntent.getBroadcast(service, REQUEST_CODE,
            Intent(ACTION_NEXT).setPackage(packageName), PendingIntent.FLAG_CANCEL_CURRENT)
    private var sessionToken: MediaSessionCompat.Token? = null
    private var controller: MediaControllerCompat? = null
    private lateinit var transportControls: MediaControllerCompat.TransportControls
    private var playbackState: PlaybackStateCompat? = null
    private var metadata: MediaMetadataCompat? = null
    private var started = false
    private var request: Request? = null
    private val notificationSize = service.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
    private val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            playbackState = state

            if ((state.state == PlaybackStateCompat.STATE_STOPPED) ||
                    (state.state == PlaybackStateCompat.STATE_NONE)) {
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
        val action = intent.action
        when (action) {
            ACTION_PLAY -> transportControls.play()
            ACTION_PAUSE -> transportControls.pause()
            ACTION_PREV -> transportControls.skipToPrevious()
            ACTION_NEXT -> transportControls.skipToNext()
            else -> {
                DebugUtils.executeInDebugMode {
                    Timber.e(TAG, "Unknown action: ", action)
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
                ((sessionToken != null) && (sessionToken != token))) {
            if (controller != null) {
                controller!!.unregisterCallback(callback)
            }

            sessionToken = token

            if (sessionToken != null) {
                controller = MediaControllerCompat(service, sessionToken)

                transportControls = controller!!.transportControls

                if (started) {
                    controller!!.registerCallback(callback)
                }
            }
        }
    }

    fun reset() {
        request?.clear()

        stopNotification()
    }

    private fun createContentIntent(): PendingIntent {
        val contentIntent = Intent(service, RadioBrowserActivity::class.java)
        contentIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(service, RadioBrowserActivity.REQUEST_CODE, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    private fun createNotification(): Notification? {
        if ((metadata == null) || (playbackState == null)) {
            return null
        }

        val builder = NotificationCompat.Builder(service)

        var playPauseButtonPosition = 0
        if (playbackState!!.actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L) {
            builder.addAction(R.drawable.ic_skip_previous_white_24dp, service.getString(R.string.action_previous), previousIntent)

            playPauseButtonPosition = 1
        }

        addPlayPauseAction(builder)

        if (playbackState!!.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L) {
            builder.addAction(R.drawable.ic_skip_next_white_24dp, service.getString(R.string.action_next), nextIntent)
        }

        val description = metadata!!.description

        builder
                .setStyle(NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(playPauseButtonPosition)
                        .setMediaSession(sessionToken))
                .setSmallIcon(R.drawable.ic_radio_white_24dp)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(createContentIntent())
                .setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setWhen(0)
                .setShowWhen(false)
                .setUsesChronometer(false)

        setNotificationPlaybackState(builder)

        val logoUrl = metadata?.getString(MediaMetadataUtils.METADATA_KEY_SMALL_LOGO)
        request = Glide.with(context)
                .load(logoUrl)
                .asBitmap()
                .override(notificationSize, notificationSize)
                .placeholder(R.drawable.ic_radio_blue_64dp)
                .error(R.drawable.ic_radio_blue_64dp)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, glideAnimation: GlideAnimation<in Bitmap>) {
                        if (metadata?.getString(MediaMetadataUtils.METADATA_KEY_SMALL_LOGO) == logoUrl) {
                            builder.setLargeIcon(resource)

                            notificationManager.notify(NOTIFICATION_ID, builder.build())
                        }
                    }
                })
                .request

        return builder.build()
    }

    private fun addPlayPauseAction(builder: NotificationCompat.Builder) {
        if (playbackState!!.state == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(R.drawable.ic_pause_white_24dp, service.getString(R.string.action_pause), pauseIntent)
        } else {
            builder.addAction(R.drawable.ic_play_arrow_white_24dp, service.getString(R.string.action_play), playIntent)
        }
    }

    private fun setNotificationPlaybackState(builder: NotificationCompat.Builder) {
        if (playbackState == null || !started) {
            service.stopForeground(true)
            return
        }

        builder.setOngoing(playbackState!!.state == PlaybackStateCompat.STATE_PLAYING)
    }
}
