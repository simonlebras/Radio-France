package com.simonlebras.radiofrance.ui.browser.mappers

import android.support.v4.media.MediaBrowserCompat
import com.simonlebras.radiofrance.data.models.Radio

class RadioMapper {
    fun transform(mediaItems: Iterable<MediaBrowserCompat.MediaItem>) = mediaItems.map {
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
}
