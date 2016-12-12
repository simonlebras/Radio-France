package fr.simonlebras.radiofrance.playback

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import javax.inject.Inject

@ServiceScope
class MediaSessionCallback @Inject constructor(val playbackManager: PlaybackManager) : MediaSessionCompat.Callback() {
    override fun onPlay() {
        playbackManager.play()
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        playbackManager.playFromRadioId(mediaId)
    }

    override fun onPause() {
        playbackManager.pause()
    }

    override fun onStop() {
        playbackManager.stop(null)
    }

    override fun onSkipToQueueItem(id: Long) {
        playbackManager.skipToQueueItem(id)
    }

    override fun onSkipToPrevious() {
        playbackManager.skipToPrevious()
    }

    override fun onSkipToNext() {
        playbackManager.skipToNext()
    }
}
