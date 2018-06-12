package com.simonlebras.radiofrance.playback.mappers

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat

object QueueItemMapper {
    fun transform(metadata: MediaMetadataCompat, index: Long): MediaSessionCompat.QueueItem {
        val mediaDescription = metadata.description

        val builder = MediaDescriptionCompat.Builder()
                .setMediaId(mediaDescription.mediaId)
                .setTitle(mediaDescription.title ?: "")
                .setDescription(mediaDescription.description ?: "")
                .setMediaUri(mediaDescription.mediaUri ?: Uri.EMPTY)
                .setExtras(metadata.bundle)

        return MediaSessionCompat.QueueItem(builder.build(), index)
    }
}
