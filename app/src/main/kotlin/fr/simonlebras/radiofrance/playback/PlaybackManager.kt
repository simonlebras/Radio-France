package fr.simonlebras.radiofrance.playback

import android.content.Context
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule.Companion.LOCAL_KEY
import fr.simonlebras.radiofrance.utils.DebugUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@ServiceScope
class PlaybackManager @Inject constructor(
        private val context: Context,
        val queueManager: QueueManager,
        @Named(LOCAL_KEY) localPlaybackFactory: (Context) -> Playback
) : Playback.Callback {
    var callback: Callback? = null
    val isPlaying: Boolean
        get() = playback.isPlaying

    private var playback = localPlaybackFactory(context)

    init {
        playback.callback = this
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updatePlaybackState(null)
    }

    override fun onError(error: String) {
        updatePlaybackState(error)
    }

    override fun setCurrentRadioId(id: String) {
        queueManager.setCurrentQueueItem(id)

        queueManager.updateMetadata()
    }

    fun play() {
        queueManager.currentRadio?.let {
            callback?.onPlaybackStart()
            playback.play(it)
        }
    }

    fun playFromRadioId(radioId: String?) {
        if (radioId == queueManager.currentRadio?.description?.mediaId && playback.isPlaying) {
            return
        }

        if (queueManager.setCurrentQueueItem(radioId)) {
            play()
        }

        queueManager.updateMetadata()
    }

    fun pause() {
        if (playback.isPlaying) {
            playback.pause()
            callback?.onPlaybackStop()
        }
    }

    fun stop(error: String?) {
        playback.stop(true)
        callback?.onPlaybackStop()
        updatePlaybackState(error)
    }

    fun skipToQueueItem(id: Long) {
        if (queueManager.setCurrentQueueItem(id)) {
            play()
        }

        queueManager.updateMetadata()
    }

    fun skipToPrevious() {
        if (queueManager.skipToPrevious()) {
            play()
        } else {
            stop(context.getString(R.string.error_action_unavailable))
        }

        queueManager.updateMetadata()
    }

    fun skipToNext() {
        if (queueManager.skipToNext()) {
            play()
        } else {
            stop(context.getString(R.string.error_action_unavailable))
        }

        queueManager.updateMetadata()
    }

    fun updatePlaybackState(error: String?) {
        val builder = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions())

        var playbackState = playback.playbackState

        if (error != null) {
            builder.setErrorMessage(ERROR_CODE_UNKNOWN_ERROR, error)
            playbackState = STATE_ERROR
        }

        builder.setState(playbackState, PLAYBACK_POSITION_UNKNOWN, 1.0f, SystemClock.elapsedRealtime())

        queueManager.currentRadio?.let {
            builder.setActiveQueueItemId(it.queueId)
        }

        callback?.onPlaybackStateChanged(builder.build())

        if (playbackState == STATE_PLAYING || playbackState == STATE_PAUSED) {
            callback?.onNotificationRequired()
        }
    }

    fun switchToPlayback(playback: Playback, resumePlaying: Boolean) {
        val oldState = this.playback.playbackState
        val currentRadioId = this.playback.currentRadioId

        this.playback.stop(false)
        playback.callback = this
        playback.currentRadioId = currentRadioId
        playback.start()

        this.playback = playback

        when (oldState) {
            STATE_BUFFERING, STATE_CONNECTING, STATE_PAUSED -> this.playback.pause()
            STATE_PLAYING -> {
                val currentMusic = queueManager.currentRadio
                if (resumePlaying && currentMusic != null) {
                    this.playback.play(currentMusic)
                } else if (!resumePlaying) {
                    this.playback.pause()
                } else {
                    this.playback.stop(true)
                }
            }
            STATE_NONE -> {
            }
            else -> {
                DebugUtils.executeInDebugMode {
                    Timber.d("Default called. Old state is %d", oldState)
                }
            }
        }
    }

    private fun getAvailableActions(): Long {
        val actions = ACTION_PLAY_PAUSE or
                ACTION_PLAY_FROM_MEDIA_ID or
                ACTION_PLAY_FROM_SEARCH or
                ACTION_SKIP_TO_PREVIOUS or
                ACTION_SKIP_TO_NEXT

        return if (playback.isPlaying) {
            actions or ACTION_PAUSE
        } else {
            actions or ACTION_PLAY
        }
    }

    interface Callback {
        fun onPlaybackStart()

        fun onPlaybackStop()

        fun onNotificationRequired()

        fun onPlaybackStateChanged(playbackState: PlaybackStateCompat)
    }
}
