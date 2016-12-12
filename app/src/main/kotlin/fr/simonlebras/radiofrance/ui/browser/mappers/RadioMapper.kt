package fr.simonlebras.radiofrance.ui.browser.mappers

import android.support.v4.media.MediaBrowserCompat
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils

object RadioMapper {
    fun transform(mediaItem: MediaBrowserCompat.MediaItem): Radio {
        val description = mediaItem.description
        val extras = description.extras!!

        return Radio(
                description.mediaId!!,
                description.title.toString(),
                description.description.toString(),
                description.mediaUri.toString(),
                extras.getString(MediaMetadataUtils.METADATA_KEY_WEBSITE),
                extras.getString(MediaMetadataUtils.METADATA_KEY_TWITTER),
                extras.getString(MediaMetadataUtils.METADATA_KEY_FACEBOOK),
                extras.getString(MediaMetadataUtils.METADATA_KEY_SMALL_LOGO),
                extras.getString(MediaMetadataUtils.METADATA_KEY_MEDIUM_LOGO),
                extras.getString(MediaMetadataUtils.METADATA_KEY_LARGE_LOGO)
        )
    }

    fun transform(mediaItems: Iterable<MediaBrowserCompat.MediaItem>): List<Radio> {
        return mediaItems.map {
            transform(it)
        }
    }
}
