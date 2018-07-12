package com.simonlebras.radiofrance.playback

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import androidx.core.content.getSystemService
import com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import timber.log.Timber
import com.google.android.exoplayer2.audio.AudioAttributes as ExoPlayerAudioAttributes

private const val TIMEOUT = 5000 // in milliseconds

private const val VOLUME_DUCK = .2f
private const val VOLUME_NORMAL = 1f

private const val WIFI_LOCK_NAME = "radiofrance:playback-wifi-lock"
private const val WAKE_LOCK_NAME = "radiofrance:playback-wake-lock"

@TargetApi(Build.VERSION_CODES.O)
class LocalPlayback(
    private val context: Context,
    private val callback: Playback.Callback
) : Player.DefaultEventListener(),
    Playback,
    AudioManager.OnAudioFocusChangeListener {
    private var currentItem: MediaSessionCompat.QueueItem? = null

    override val isPlaying: Boolean get() = playOnFocusGain || exoPlayer.playWhenReady

    private var exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
        .apply {
            audioAttributes = ExoPlayerAudioAttributes.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .setUsage(USAGE_MEDIA)
                .build()

            addListener(this@LocalPlayback)
        }
    private val userAgent = context.getString(R.string.app_name)

    private val mediaSource by lazy(LazyThreadSafetyMode.NONE) {
        val dataSourceFactory =
            DefaultHttpDataSourceFactory(userAgent, null, TIMEOUT, TIMEOUT, true)

        ExtractorMediaSource.Factory(dataSourceFactory)
            .setExtractorsFactory {
                arrayOf(Mp3Extractor())
            }
    }

    private var playOnFocusGain = false
    private val audioFocusRequest by lazy(LazyThreadSafetyMode.NONE) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(this)
            .build()
    }

    private var audioNoisyReceiverRegistered = false
    private val audioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && isPlaying) {
                val pauseIntent = Intent(context, RadioPlaybackService::class.java).apply {
                    action = ACTION_PAUSE
                }

                context.startService(pauseIntent)
            }
        }
    }

    private val audioManager = context.getSystemService<AudioManager>()!!

    private val wifiLock = context.getSystemService<WifiManager>()!!.createWifiLock(WIFI_LOCK_NAME)
    private val wakeLock = context.getSystemService<PowerManager>()!!.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        WAKE_LOCK_NAME
    )

    override fun play(item: MediaSessionCompat.QueueItem) {
        currentItem = item

        playOnFocusGain = true

        if (!requestAudioFocus()) {
            return
        }

        registerAudioNoisyReceiver()
        acquireWakeAndWifiLocks()

        exoPlayer.apply {
            stop(true)
            prepare(mediaSource.createMediaSource(currentItem!!.description.mediaUri))
            playWhenReady = true
        }
    }

    override fun pause() {
        stopPlayback(false)
    }

    override fun stop() {
        stopPlayback(true)
    }

    override fun release() {
        stop()

        exoPlayer.removeListener(this)

        exoPlayer.release()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> callback.onPlaybackStateChanged(STATE_BUFFERING)
            Player.STATE_READY -> {
                val state = if (playWhenReady) STATE_PLAYING else STATE_PAUSED
                callback.onPlaybackStateChanged(state)
            }
            Player.STATE_IDLE -> callback.onPlaybackStateChanged(STATE_STOPPED)
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (BuildConfig.DEBUG) {
            Timber.e(error, "ExoPlayer error")
        }

        stop()

        callback.onError(context.getString(R.string.error_occurred))
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                exoPlayer.volume = VOLUME_NORMAL

                if (!exoPlayer.playWhenReady && playOnFocusGain) {
                    play(currentItem!!)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (exoPlayer.playWhenReady) {
                    exoPlayer.volume = VOLUME_DUCK
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val wasPlaying = exoPlayer.playWhenReady

                pause()

                playOnFocusGain = wasPlaying
            }
            AudioManager.AUDIOFOCUS_LOSS -> stop()
        }
    }

    private fun stopPlayback(abandonAudioFocus: Boolean) {
        if (abandonAudioFocus) {
            abandonAudioFocus()
        }

        unregisterAudioNoisyReceiver()
        releaseWakeAndWifiLocks()

        exoPlayer.stop(true)
        exoPlayer.playWhenReady = false

        playOnFocusGain = false
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(this)
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, audioNoisyIntentFilter)
            audioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver)
            audioNoisyReceiverRegistered = false
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeAndWifiLocks() {
        if (!wifiLock.isHeld) {
            wifiLock.acquire()
        }

        if (!wakeLock.isHeld) {
            wakeLock.acquire()
        }
    }

    private fun releaseWakeAndWifiLocks() {
        if (wifiLock.isHeld) {
            wifiLock.release()
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    companion object {
        const val ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.ACTION_PAUSE"
    }
}
