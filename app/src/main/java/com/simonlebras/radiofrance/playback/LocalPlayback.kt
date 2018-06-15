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
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.simonlebras.radiofrance.BuildConfig
import com.simonlebras.radiofrance.R
import timber.log.Timber
import javax.inject.Inject
import com.google.android.exoplayer2.audio.AudioAttributes as ExoPlayerAudioAttributes

@TargetApi(Build.VERSION_CODES.O)
class LocalPlayback @Inject constructor(
        private val context: Context
) : Playback,
        AudioManager.OnAudioFocusChangeListener,
        Player.EventListener {
    private companion object {
        const val TIMEOUT = 5000 // in milliseconds

        const val AUDIO_NO_FOCUS_NO_DUCK = 0
        const val AUDIO_NO_FOCUS_CAN_DUCK = 1
        const val AUDIO_FOCUSED = 2

        const val VOLUME_DUCK = .2f
        const val VOLUME_NORMAL = 1f

        const val WIFI_LOCK_NAME = "radiofrance:playback-wifi-lock"
        const val WAKE_LOCK_NAME = "radiofrance:playback-wake-lock"
    }

    override var currentRadioId: String? = null

    override var playbackState = STATE_NONE

    override var callback: Playback.Callback? = null

    override val isPlaying: Boolean
        get() = playOnFocusGain || (exoPlayer?.playWhenReady == true)

    private var exoPlayer: SimpleExoPlayer? = null
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
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action && isPlaying) {
                val pauseIntent = Intent(context, RadioPlaybackService::class.java).apply {
                    action = RadioPlaybackService.ACTION_CMD
                    putExtra(RadioPlaybackService.EXTRAS_CMD_NAME, RadioPlaybackService.CMD_PAUSE)
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
        playOnFocusGain = true

        requestAudioFocus()
        registerAudioNoisyReceiver()

        val radioId = item.description.mediaId
        if (currentRadioId != radioId || exoPlayer == null) {
            currentRadioId = radioId

            acquireWakeAndWifiLocks()

            prepareExoPlayer(item.description.mediaUri)

            playbackState = STATE_BUFFERING
        }

        configureExoPlayerState()
    }

    override fun pause() {
        exoPlayer!!.playWhenReady = false

        releaseWakeAndWifiLocks()
        unregisterAudioNoisyReceiver()

        playbackState = STATE_PAUSED
        callback?.onPlaybackStateChanged(playbackState)
    }

    override fun stop(notify: Boolean) {
        abandonAudioFocus()
        unregisterAudioNoisyReceiver()
        releaseWakeAndWifiLocks()

        releaseExoPlayer()

        playbackState = STATE_STOPPED
        if (notify) {
            callback?.onPlaybackStateChanged(playbackState)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> audioFocus = AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> audioFocus = AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocus = AUDIO_NO_FOCUS_NO_DUCK
                playOnFocusGain = exoPlayer!!.playWhenReady
            }
            AudioManager.AUDIOFOCUS_LOSS -> audioFocus = AUDIO_NO_FOCUS_NO_DUCK
        }

        configureExoPlayerState()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playWhenReady && playbackState == Player.STATE_READY &&
                (this.playbackState == STATE_BUFFERING || this.playbackState == STATE_PAUSED)) {
            this.playbackState = STATE_PLAYING

            configureExoPlayerState()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        if (BuildConfig.DEBUG) {
            Timber.e(error, "ExoPlayer error")
        }

        stop(false)

        playbackState = STATE_ERROR
        callback?.onError(context.getString(R.string.error_occurred))
    }

    override fun onLoadingChanged(isLoading: Boolean) {
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
    }

    override fun onSeekProcessed() {
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    }

    private fun prepareExoPlayer(radioUri: Uri?) {
        initializeExoPlayer()

        val dataSourceFactory = DefaultHttpDataSourceFactory(userAgent, null, TIMEOUT, TIMEOUT, true)
        val source = ExtractorMediaSource.Factory(dataSourceFactory)
                .setExtractorsFactory { arrayOf(Mp3Extractor()) }
                .createMediaSource(radioUri)

        exoPlayer!!.prepare(source)
    }

    private fun initializeExoPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
                    .apply {
                        audioAttributes = ExoPlayerAudioAttributes.Builder()
                                .setContentType(CONTENT_TYPE_MUSIC)
                                .setUsage(USAGE_MEDIA)
                                .build()
                        addListener(this@LocalPlayback)
                    }

            return
        }

        exoPlayer!!.stop()
    }

    private fun configureExoPlayerState() {
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            pause()
        } else {
            registerAudioNoisyReceiver()

            exoPlayer!!.volume = if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) VOLUME_DUCK else VOLUME_NORMAL

            if (playOnFocusGain) {
                playOnFocusGain = false

                exoPlayer!!.playWhenReady = true
            }
        }

        callback?.onPlaybackStateChanged(playbackState)
    }

    private fun requestAudioFocus() {
        if (audioFocus != AUDIO_FOCUSED) {
            val result: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.requestAudioFocus(audioFocusRequest)
            } else {
                audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED
            }
        }
    }

    private fun abandonAudioFocus() {
        if (audioFocus == AUDIO_FOCUSED) {
            val result: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    private fun releaseExoPlayer() {
        exoPlayer?.let {
            it.release()
            it.removeListener(this)
            exoPlayer = null
            playOnFocusGain = false
        }
    }
}
