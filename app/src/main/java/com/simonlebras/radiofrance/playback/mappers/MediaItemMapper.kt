package com.simonlebras.radiofrance.playback.mappers

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat

class MediaItemMapper {
    fun transform(metadata: Iterable<MediaMetadataCompat>) = metadata.map {
        with(it.description) {
            val description = MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(title)
                    .setDescription(description)
                    .setMediaUri(mediaUri)
                    .setIconUri(iconUri)
                    .build()

            MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        }
    }
}
