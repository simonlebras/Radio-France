package fr.simonlebras.radiofrance.playback.mappers

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils
import javax.inject.Inject

@ServiceScope
class MediaItemMapper @Inject constructor() {
    fun transform(mediaMetadata: MediaMetadataCompat): MediaBrowserCompat.MediaItem {
        val mediaDescription = mediaMetadata.description
        val builder = MediaDescriptionCompat.Builder()
                .setMediaId(mediaDescription.mediaId)
                .setTitle(mediaDescription.title ?: "")
                .setDescription(mediaDescription.description ?: "")
                .setMediaUri(mediaDescription.mediaUri ?: Uri.EMPTY)

        val extras = Bundle()
        MediaMetadataUtils.EXTRA_METADATA_KEYS
                .forEach {
                    extras.putString(it, mediaMetadata.getString(it))
                }
        builder.setExtras(extras)

        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    fun transform(mediaMetadata: Iterable<MediaMetadataCompat>): List<MediaBrowserCompat.MediaItem> {
        return mediaMetadata.map {
            transform(it)
        }
    }
}
