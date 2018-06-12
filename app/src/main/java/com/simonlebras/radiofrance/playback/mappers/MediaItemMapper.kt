package com.simonlebras.radiofrance.playback.mappers

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat

object MediaItemMapper {
    private fun transform(metadata: MediaMetadataCompat): MediaBrowserCompat.MediaItem {
        with(metadata.description) {
            val description = MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(title)
                    .setDescription(description)
                    .setMediaUri(mediaUri)
                    .setIconUri(iconUri)
                    .build()

            return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }
    }

    fun transform(metadata: Iterable<MediaMetadataCompat>) = metadata.map { transform(it) }
}
