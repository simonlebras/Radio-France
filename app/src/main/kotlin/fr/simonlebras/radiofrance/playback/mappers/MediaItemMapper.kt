package fr.simonlebras.radiofrance.playback.mappers

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat

object MediaItemMapper {
    private fun transform(metadata: MediaMetadataCompat): MediaBrowserCompat.MediaItem {
        val mediaDescription = metadata.description
        val builder = MediaDescriptionCompat.Builder()
                .setMediaId(mediaDescription.mediaId)
                .setTitle(mediaDescription.title ?: "")
                .setDescription(mediaDescription.description ?: "")
                .setMediaUri(mediaDescription.mediaUri ?: Uri.EMPTY)
                .setExtras(metadata.bundle)

        return MediaBrowserCompat.MediaItem(builder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    fun transform(metadata: Iterable<MediaMetadataCompat>): List<MediaBrowserCompat.MediaItem> {
        return metadata.map {
            transform(it)
        }
    }
}
