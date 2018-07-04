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
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import timber.log.Timber
import com.google.android.exoplayer2.audio.AudioAttributes as ExoPlayerAudioAttributes

@TargetApi(Build.VERSION_CODES.O)
class LocalPlayback(
    private val context: Context,
    private val callback: Playback.Callback
) : Player.DefaultEventListener(),
    Playback,
    AudioManager.OnAudioFocusChangeListener {
    private var currentItem: MediaSessionCompat.QueueItem? = null

    override var playbackState = STATE_NONE

    override val isPlaying: Boolean get() = playOnFocusGain || (this::exoPlayer.isInitialized && exoPlayer.playWhenReady)

    private lateinit var exoPlayer: SimpleExoPlayer
    private val userAgent = context.getString(R.string.app_name)

    private var playOnFocusGain = false
    private var audioFocus = AUDIO_NO_FOCUS_NO_DUCK
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

    private val audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)!!

    private val wifiLock = ContextCompat
        .getSystemService(context.applicationContext, WifiManager::class.java)!!
        .createWifiLock(WIFI_LOCK_NAME)

    private val wakeLock = ContextCompat
        .getSystemService(context, PowerManager::class.java)!!
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_NAME)

    override fun play(item: MediaSessionCompat.QueueItem) {
        currentItem = item

        playOnFocusGain = true

        requestAudioFocus()
        registerAudioNoisyReceiver()
        acquireWakeAndWifiLocks()

        prepareExoPlayer()

        playbackState = PlaybackStateCompat.STATE_BUFFERING

        configureExoPlayerState()
    }

    override fun pause() {
        if (this::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = false
        }

        releaseWakeAndWifiLocks()
        unregisterAudioNoisyReceiver()

        playbackState = STATE_PAUSED

        callback.onPlaybackStateChanged(playbackState)
    }

    override fun stop() {
        abandonAudioFocus()
        unregisterAudioNoisyReceiver()
        releaseWakeAndWifiLocks()

        if (this::exoPlayer.isInitialized) {
            exoPlayer.stop(true)
        }

        playOnFocusGain = false
    }

    override fun release() {
        stop()

        releaseExoPlayer()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> audioFocus = AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> audioFocus = AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK
                playOnFocusGain = exoPlayer.playWhenReady
            }
            AudioManager.AUDIOFOCUS_LOSS -> audioFocus = AUDIO_NO_FOCUS_NO_DUCK
        }

        configureExoPlayerState()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playWhenReady && playbackState == Player.STATE_READY &&
            (this.playbackState == STATE_BUFFERING || this.playbackState == STATE_PAUSED)
        ) {
            this.playbackState = STATE_PLAYING

            configureExoPlayerState()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (BuildConfig.DEBUG) {
            Timber.e(error, "ExoPlayer error")
        }

        stop()

        playbackState = STATE_ERROR

        callback.onError(context.getString(R.string.error_occurred))
    }

    private fun prepareExoPlayer() {
        initializeExoPlayer()

        val dataSourceFactory =
            DefaultHttpDataSourceFactory(userAgent, null, TIMEOUT, TIMEOUT, true)
        val source = ExtractorMediaSource.Factory(dataSourceFactory)
            .setExtractorsFactory { arrayOf(Mp3Extractor()) }
            .createMediaSource(currentItem!!.description.mediaUri)

        exoPlayer.prepare(source)
    }

    private fun initializeExoPlayer() {
        if (!this::exoPlayer.isInitialized) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector()).apply {
                audioAttributes = ExoPlayerAudioAttributes.Builder()
                    .setContentType(CONTENT_TYPE_MUSIC)
                    .setUsage(USAGE_MEDIA)
                    .build()

                addListener(this@LocalPlayback)
            }
        }
    }

    private fun releaseExoPlayer() {
        if (this::exoPlayer.isInitialized) {
            exoPlayer.release()

            exoPlayer.removeListener(this)
        }
    }

    private fun configureExoPlayerState() {
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            pause()
        } else {
            registerAudioNoisyReceiver()

            exoPlayer.volume =
                    if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) VOLUME_DUCK else VOLUME_NORMAL

            if (playOnFocusGain) {
                playOnFocusGain = false

                exoPlayer.playWhenReady = true
            }
        }

        callback.onPlaybackStateChanged(playbackState)
    }

    private fun requestAudioFocus() {
        if (audioFocus != AUDIO_FOCUSED) {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.requestAudioFocus(audioFocusRequest)
            } else {
                audioManager.requestAudioFocus(
                    this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED
            }
        }
    }

    private fun abandonAudioFocus() {
        if (audioFocus == AUDIO_FOCUSED) {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            } else {
                audioManager.abandonAudioFocus(this)
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK
            }
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
        private const val TIMEOUT = 5000 // in milliseconds

        private const val AUDIO_NO_FOCUS_NO_DUCK = 0
        private const val AUDIO_NO_FOCUS_CAN_DUCK = 1
        private const val AUDIO_FOCUSED = 2

        private const val VOLUME_DUCK = .2f
        private const val VOLUME_NORMAL = 1f

        private const val WIFI_LOCK_NAME = "radiofrance:playback-wifi-lock"
        private const val WAKE_LOCK_NAME = "radiofrance:playback-wake-lock"

        const val ACTION_PAUSE = "${BuildConfig.APPLICATION_ID}.ACTION_PAUSE"
    }
}
