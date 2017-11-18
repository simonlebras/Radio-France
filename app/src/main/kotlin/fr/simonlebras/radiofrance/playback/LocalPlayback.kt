package fr.simonlebras.radiofrance.playback

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.text.TextUtils
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.extractor.mp4.Mp4Extractor
import com.google.android.exoplayer2.extractor.ogg.OggExtractor
import com.google.android.exoplayer2.extractor.wav.WavExtractor
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.utils.DebugUtils
import timber.log.Timber
import javax.inject.Inject

class LocalPlayback @Inject constructor(
        private val context: Context,
        private val audioManager: AudioManager,
        private val wifiLock: WifiManager.WifiLock,
        private val wakeLock: PowerManager.WakeLock
) : Playback, AudioManager.OnAudioFocusChangeListener, ExoPlayer.EventListener {
    private companion object {
        const val TIMEOUT = 5000 // in milliseconds
    }

    override var currentRadioId: String? = null

    override var playbackState = STATE_NONE

    override var callback: Playback.Callback? = null

    override val isPlaying: Boolean
        get() = playOnFocusGain || (exoPlayer?.playWhenReady == true)

    private var exoPlayer: SimpleExoPlayer? = null
    private val userAgent = context.getString(R.string.app_name)
    private var playOnFocusGain = false
    private var audioFocus = AudioFocus.NO_FOCUS_NO_DUCK
    private var audioNoisyReceiverRegistered = false
    private val audioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val mAudioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent?.action) {
                if (exoPlayer?.playWhenReady == true) {
                    val pauseIntent = Intent(context, RadioPlaybackService::class.java)
                    pauseIntent.action = RadioPlaybackService.ACTION_CMD
                    pauseIntent.putExtra(RadioPlaybackService.EXTRAS_CMD_NAME, RadioPlaybackService.CMD_PAUSE)
                    this@LocalPlayback.context.startService(pauseIntent)
                }
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    override fun play(item: MediaSessionCompat.QueueItem) {
        playOnFocusGain = true

        requestAudioFocus()

        registerAudioNoisyReceiver()

        val radioId = item.description.mediaId
        val radioHasChanged = !TextUtils.equals(radioId, currentRadioId)
        if (radioHasChanged) {
            currentRadioId = radioId
        }

        if ((playbackState == STATE_PAUSED) && !radioHasChanged && (exoPlayer != null)) {
            configureExoPlayerState()
        } else {
            playbackState = STATE_STOPPED

            release(false)

            initializeExoPlayer()

            playbackState = STATE_BUFFERING

            prepareExoPlayer(item.description.mediaUri)

            wifiLock.acquire()
            wakeLock.acquire()

            callback?.onPlaybackStateChanged(playbackState)
        }
    }

    override fun pause() {
        if (playbackState == STATE_PLAYING) {
            if (exoPlayer?.playWhenReady == true) {
                exoPlayer!!.playWhenReady = false
            }

            release(false)
        }

        playbackState = STATE_PAUSED

        callback?.onPlaybackStateChanged(playbackState)

        unregisterAudioNoisyReceiver()
    }

    override fun stop(notify: Boolean) {
        playbackState = STATE_STOPPED

        if (notify) {
            callback?.onPlaybackStateChanged(playbackState)
        }

        abandonAudioFocus()

        unregisterAudioNoisyReceiver()

        release(true)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            audioFocus = AudioFocus.FOCUSED
        } else if ((focusChange == AudioManager.AUDIOFOCUS_LOSS) ||
                (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) ||
                (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)) {
            val canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            audioFocus = if (canDuck) AudioFocus.FOCUS_CAN_DUCK else AudioFocus.NO_FOCUS_NO_DUCK

            if ((playbackState == STATE_PLAYING) && !canDuck) {
                playOnFocusGain = true
            }
        }

        configureExoPlayerState()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playWhenReady && (playbackState == ExoPlayer.STATE_READY) && (this.playbackState == STATE_BUFFERING)) {
            this.playbackState = STATE_PLAYING

            configureExoPlayerState()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        DebugUtils.executeInDebugMode {
            Timber.e(error, "ExoPlayer error")
        }

        stop(false)

        playbackState = STATE_ERROR

        callback?.onError(context.getString(R.string.error_occurred))
    }

    override fun onLoadingChanged(isLoading: Boolean) {
    }

    override fun onPositionDiscontinuity() {
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
    }

    private fun initializeExoPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector(), DefaultLoadControl())

            exoPlayer!!.addListener(this)

            return
        }

        exoPlayer!!.stop()
    }

    private fun prepareExoPlayer(radioUri: Uri?) {
        val dataSourceFactory = DefaultHttpDataSourceFactory(userAgent, null, TIMEOUT, TIMEOUT, true)

        val extractorsFactory = ExtractorsFactory {
            arrayOf(Mp3Extractor(),
                    WavExtractor(),
                    Mp4Extractor(),
                    OggExtractor())
        }

        val source = ExtractorMediaSource(radioUri, dataSourceFactory, extractorsFactory, Handler(), null)

        exoPlayer!!.prepare(source)

        exoPlayer!!.playWhenReady = true
    }

    private fun configureExoPlayerState() {
        if (audioFocus == AudioFocus.NO_FOCUS_NO_DUCK) {
            if (playbackState == STATE_PLAYING) {
                pause()
            }
        } else {
            registerAudioNoisyReceiver()

            val volume = if (audioFocus == AudioFocus.FOCUS_CAN_DUCK) AudioVolume.DUCK.volume else AudioVolume.NORMAL.volume
            exoPlayer?.volume = volume

            if (playOnFocusGain) {
                if (exoPlayer?.playWhenReady == false) {
                    exoPlayer!!.playWhenReady = true
                    playbackState = STATE_PLAYING
                }

                playOnFocusGain = false
            }
        }

        callback?.onPlaybackStateChanged(playbackState)
    }

    private fun requestAudioFocus() {
        if (audioFocus != AudioFocus.FOCUSED) {
            val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AudioFocus.FOCUSED
            }
        }
    }

    private fun abandonAudioFocus() {
        if (audioFocus == AudioFocus.FOCUSED) {
            if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AudioFocus.NO_FOCUS_NO_DUCK
            }
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            context.registerReceiver(mAudioNoisyReceiver, audioNoisyIntentFilter)
            audioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            context.unregisterReceiver(mAudioNoisyReceiver)
            audioNoisyReceiverRegistered = false
        }
    }

    private fun release(releaseExoPlayer: Boolean) {
        if (releaseExoPlayer) {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        }

        if (wifiLock.isHeld) {
            wifiLock.release()
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private enum class AudioFocus {
        NO_FOCUS_NO_DUCK, FOCUS_CAN_DUCK, FOCUSED
    }

    private enum class AudioVolume(val volume: Float) {
        DUCK(0.2f), NORMAL(1.0f)
    }
}
