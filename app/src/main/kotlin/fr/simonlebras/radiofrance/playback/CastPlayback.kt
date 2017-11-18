package fr.simonlebras.radiofrance.playback

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.text.TextUtils
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.playback.data.RadioProvider
import fr.simonlebras.radiofrance.utils.DebugUtils
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class CastPlayback @Inject constructor(
        private val context: Context,
        private val remoteMediaClient: RemoteMediaClient,
        private val radioProvider: RadioProvider
) : Playback, RemoteMediaClient.Listener {
    private companion object {
        const val MIME_TYPE_AUDIO_MPEG = "audio/mpeg"
        const val ITEM_ID = "ITEM_ID"
    }

    override var currentRadioId: String? = null

    override var playbackState = STATE_NONE

    override var callback: Playback.Callback? = null

    override val isConnected: Boolean
        get() {
            val castSession = CastContext.getSharedInstance(context.applicationContext).sessionManager.currentCastSession
            return castSession?.isConnected == true
        }

    override val isPlaying: Boolean
        get() = isConnected && remoteMediaClient.isPlaying

    override fun play(item: MediaSessionCompat.QueueItem) {
        try {
            loadRadio(item.description.mediaId, true)

            playbackState = STATE_BUFFERING

            callback?.onPlaybackStateChanged(playbackState)
        } catch (e: JSONException) {
            callback?.onError(context.getString(R.string.error_occurred))
        }
    }

    override fun pause() {
        try {
            if (remoteMediaClient.hasMediaSession()) {
                remoteMediaClient.pause()
            } else {
                loadRadio(currentRadioId, false)
            }
        } catch (e: JSONException) {
            callback?.onError(context.getString(R.string.error_occurred))
        }
    }

    override fun start() {
        remoteMediaClient.addListener(this)
    }

    override fun stop(notify: Boolean) {
        remoteMediaClient.removeListener(this)

        playbackState = STATE_STOPPED

        if (notify) {
            callback?.onPlaybackStateChanged(playbackState)
        }
    }

    override fun onStatusUpdated() {
        updatePlaybackState()
    }

    override fun onQueueStatusUpdated() {}

    override fun onPreloadStatusUpdated() {}

    override fun onSendingRemoteMediaRequest() {}

    override fun onMetadataUpdated() {
        setMetadataFromRemote()
    }

    override fun onAdBreakStatusUpdated() {}

    @Throws(JSONException::class)
    private fun loadRadio(radioId: String?, autoPlay: Boolean) {
        val radio = radioProvider.metadata[radioId] ?: throw IllegalArgumentException("Invalid radioId " + radioId)

        currentRadioId = radioId

        val customData = JSONObject()
        customData.put(ITEM_ID, radioId)

        val mediaInfo = createMediaInfo(radio, customData)
        remoteMediaClient.load(mediaInfo, autoPlay, 0, customData)
    }

    private fun createMediaInfo(radio: MediaMetadataCompat, customData: JSONObject): MediaInfo {
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, radio.description.title?.toString() ?: "")
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, radio.description.description?.toString() ?: "")

        val image = WebImage(Uri.Builder()
                .encodedPath(radio.getString(MediaMetadataUtils.METADATA_KEY_LARGE_LOGO) ?: "")
                .build())

        mediaMetadata.addImage(image)
        mediaMetadata.addImage(image)

        return MediaInfo.Builder(radio.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
                .setContentType(MIME_TYPE_AUDIO_MPEG)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .setCustomData(customData)
                .build()
    }

    private fun setMetadataFromRemote() {
        try {
            val mediaInfo = remoteMediaClient.mediaInfo ?: return

            val customData = mediaInfo.customData

            if (customData != null && customData.has(ITEM_ID)) {
                val remoteRadioId = customData.getString(ITEM_ID)
                if (!TextUtils.equals(currentRadioId, remoteRadioId)) {
                    currentRadioId = remoteRadioId

                    callback?.setCurrentRadioId(remoteRadioId)
                }
            }
        } catch (e: JSONException) {
            DebugUtils.executeInDebugMode {
                Timber.e(e, "Exception processing update metadata")
            }
        }
    }

    private fun updatePlaybackState() {
        val status = remoteMediaClient.playerState

        when (status) {
            MediaStatus.PLAYER_STATE_BUFFERING -> {
                playbackState = STATE_BUFFERING

                callback?.onPlaybackStateChanged(playbackState)
            }
            MediaStatus.PLAYER_STATE_PLAYING -> {
                playbackState = STATE_PLAYING

                setMetadataFromRemote()

                callback?.onPlaybackStateChanged(playbackState)
            }
            MediaStatus.PLAYER_STATE_PAUSED -> {
                playbackState = STATE_PAUSED

                setMetadataFromRemote()

                callback?.onPlaybackStateChanged(playbackState)
            }
            else -> {
                DebugUtils.executeInDebugMode {
                    Timber.d("State default : %d", status)
                }
            }
        }
    }
}
