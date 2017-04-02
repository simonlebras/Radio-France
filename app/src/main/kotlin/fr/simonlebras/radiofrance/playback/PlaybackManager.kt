package fr.simonlebras.radiofrance.playback

import android.content.Context
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.di.modules.RadioPlaybackModule.Companion.LOCAL_KEY
import fr.simonlebras.radiofrance.utils.DebugUtils
import fr.simonlebras.radiofrance.utils.LogUtils
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@ServiceScope
class PlaybackManager @Inject constructor(val context: Context,
                                          val queueManager: QueueManager,
                                          @Named(LOCAL_KEY) localPlaybackFactory: PlaybackFactory) : Playback.Callback {
    private companion object {
        val TAG = LogUtils.makeLogTag(PlaybackManager::class.java.simpleName)
    }

    var callback: Callback? = null
    val isPlaying: Boolean
        get() = playback.isPlaying

    private var playback = localPlaybackFactory.create(context)

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
            playbackState = PlaybackStateCompat.STATE_ERROR
        }

        builder.setState(playbackState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f, SystemClock.elapsedRealtime())

        queueManager.currentRadio?.let {
            builder.setActiveQueueItemId(it.queueId)
        }

        callback?.onPlaybackStateChanged(builder.build())

        if (playbackState == PlaybackStateCompat.STATE_PLAYING || playbackState == PlaybackStateCompat.STATE_PAUSED) {
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
            PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING, PlaybackStateCompat.STATE_PAUSED -> this.playback.pause()
            PlaybackStateCompat.STATE_PLAYING -> {
                val currentMusic = queueManager.currentRadio
                if (resumePlaying && currentMusic != null) {
                    this.playback.play(currentMusic)
                } else if (!resumePlaying) {
                    this.playback.pause()
                } else {
                    this.playback.stop(true)
                }
            }
            PlaybackStateCompat.STATE_NONE -> {
            }
            else -> {
                DebugUtils.executeInDebugMode {
                    Timber.d(TAG, "Default called. Old state is ", oldState)
                }
            }
        }
    }

    private fun getAvailableActions(): Long {
        var actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT

        if (playback.isPlaying) {
            actions = actions or PlaybackStateCompat.ACTION_PAUSE
        }

        return actions
    }

    interface Callback {
        fun onPlaybackStart()

        fun onPlaybackStop()

        fun onNotificationRequired()

        fun onPlaybackStateChanged(playbackState: PlaybackStateCompat)
    }
}
