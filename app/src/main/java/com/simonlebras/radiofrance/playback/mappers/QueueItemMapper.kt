package com.simonlebras.radiofrance.playback.mappers

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat

class QueueItemMapper {
    fun transform(metadata: MediaMetadataCompat, index: Long): MediaSessionCompat.QueueItem {
        with(metadata.description) {
            val description = MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(title)
                    .setDescription(description)
                    .setMediaUri(mediaUri)
                    .setIconUri(iconUri)
                    .build()

            return MediaSessionCompat.QueueItem(description, index)
        }
    }
}
