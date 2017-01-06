package fr.simonlebras.radiofrance.playback

import android.content.Context
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import javax.inject.Inject

@ServiceScope
class PlaybackManager @Inject constructor(val context: Context,
                                          val queueManager: QueueManager,
                                          var playback: Playback) : Playback.Callback {
    var callback: Callback? = null
    val isPlaying: Boolean
        get() = playback.isPlaying

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
            builder.setErrorMessage(error)
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
