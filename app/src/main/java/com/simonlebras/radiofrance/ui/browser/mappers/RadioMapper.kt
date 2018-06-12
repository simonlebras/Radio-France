package com.simonlebras.radiofrance.ui.browser.mappers

import android.support.v4.media.MediaBrowserCompat
import com.simonlebras.radiofrance.models.Radio

object RadioMapper {
    private fun transform(mediaItem: MediaBrowserCompat.MediaItem): Radio {
        with(mediaItem.description) {
            return Radio(
                    mediaId!!,
                    title.toString(),
                    description.toString(),
                    mediaUri.toString(),
                    iconUri.toString()
            )
        }
    }

    fun transform(mediaItems: Iterable<MediaBrowserCompat.MediaItem>) = mediaItems.map { transform(it) }
}
