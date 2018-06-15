package com.simonlebras.radiofrance.playback.mappers

import android.support.v4.media.MediaMetadataCompat
import com.simonlebras.radiofrance.data.models.Radio

class MediaMetadataMapper {
    fun transform(radios: Iterable<Radio>) = radios.map {
        MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, it.id)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, it.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, it.description)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, it.stream)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, it.logo)
                .build()
    }
}
