package com.simonlebras.radiofrance.ui.browser.manager

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.data.models.Resource
import com.simonlebras.radiofrance.playback.RadioPlaybackService
import com.simonlebras.radiofrance.utils.AppContexts
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.ConflatedChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import javax.inject.Inject

class RadioManagerImpl @Inject constructor(
    private val context: Context,
    private val appContexts: AppContexts
) : MediaControllerCompat.Callback(),
    RadioManager {
    private lateinit var mediaBrowser: MediaBrowserCompat

    private lateinit var mediaController: MediaControllerCompat

    private val playbackStateChannel = ConflatedChannel<PlaybackStateCompat>()
    private val metadataChannel = ConflatedChannel<MediaMetadataCompat>()

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        state?.let {
            playbackStateChannel.offer(it)
        }
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        metadata?.let {
            metadataChannel.offer(it)
        }
    }

    override fun connect(): Deferred<Boolean> {
        return async(context = appContexts.main) {
            suspendCancellableCoroutine<Boolean> {
                mediaBrowser = MediaBrowserCompat(
                    context,
                    ComponentName(context, RadioPlaybackService::class.java),
                    object : MediaBrowserCompat.ConnectionCallback() {
                        override fun onConnected() {
                            mediaController =
                                    MediaControllerCompat(
                                        context,
                                        mediaBrowser.sessionToken
                                    ).apply {
                                        registerCallback(this@RadioManagerImpl)

                                        playbackState?.let {
                                            playbackStateChannel.offer(it)
                                        }

                                        metadata?.let {
                                            metadataChannel.offer(it)
                                        }
                                    }

                            it.resume(true)
                        }
                    },
                    null
                )
                mediaBrowser.connect()
            }
        }
    }

    override fun playbackStateUpdates() = playbackStateChannel

    override fun metadataUpdates() = metadataChannel

    override fun loadRadios(): Deferred<Resource<List<Radio>>> {
        return async {
            suspendCancellableCoroutine<Resource<List<Radio>>> {
                val root = mediaBrowser.root

                mediaBrowser.unsubscribe(root)

                mediaBrowser.subscribe(
                    root,
                    object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(
                            parentId: String,
                            children: List<MediaBrowserCompat.MediaItem>
                        ) {
                            mediaBrowser.unsubscribe(root)

                            launch(context = appContexts.computation) {
                                val radios = children.map {
                                    with(it.description) {
                                        Radio(
                                            mediaId!!,
                                            title.toString(),
                                            description.toString(),
                                            mediaUri.toString(),
                                            iconUri.toString()
                                        )
                                    }
                                }

                                it.resume(Resource.success(radios))
                            }
                        }

                        override fun onError(parentId: String) {
                            it.resumeWithException(SubscriptionException(parentId))
                        }
                    }
                )
            }
        }
    }

    override fun playFromId(id: String) {
        mediaController.transportControls.playFromMediaId(id, null)
    }

    override fun togglePlayPause() {
        when (mediaController.playbackState.state) {
            PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_NONE -> mediaController.transportControls.play()
            PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.STATE_BUFFERING, PlaybackStateCompat.STATE_CONNECTING -> mediaController.transportControls.pause()
        }
    }

    override fun skipToPrevious() {
        mediaController.transportControls.skipToPrevious()
    }

    override fun skipToNext() {
        mediaController.transportControls.skipToNext()
    }

    override fun clear() {
        if (this::mediaBrowser.isInitialized && mediaBrowser.isConnected) {
            mediaBrowser.unsubscribe(mediaBrowser.root)
            mediaBrowser.disconnect()
        }

        if (this::mediaController.isInitialized) {
            mediaController.unregisterCallback(this)
        }

        playbackStateChannel.close()
        metadataChannel.close()
    }
}
