package com.simonlebras.radiofrance.playback.mappers

import android.support.v4.media.MediaMetadataCompat
import com.simonlebras.radiofrance.data.model.Radio

object MediaMetadataMapper {
    private fun transform(radio: Radio): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, radio.id)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, radio.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, radio.description)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, radio.stream)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, radio.logo)
                .build()
    }

    fun transform(radios: Iterable<Radio>) = radios.map { transform(it) }
}
