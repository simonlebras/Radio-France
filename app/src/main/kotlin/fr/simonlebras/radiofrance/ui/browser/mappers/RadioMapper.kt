package fr.simonlebras.radiofrance.ui.browser.mappers

import android.support.v4.media.MediaBrowserCompat
import fr.simonlebras.radiofrance.di.scopes.ActivityScope
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils
import javax.inject.Inject

@ActivityScope
class RadioMapper @Inject constructor() {
    fun transform(mediaItem: MediaBrowserCompat.MediaItem): Radio {
        val description = mediaItem.description
        val extras = description.extras!!

        return Radio(
                description.mediaId!!,
                description.title.toString(),
                description.description.toString(),
                description.mediaUri.toString(),
                extras.getString(MediaMetadataUtils.METADATA_WEBSITE),
                extras.getString(MediaMetadataUtils.METADATA_TWITTER),
                extras.getString(MediaMetadataUtils.METADATA_FACEBOOK),
                extras.getString(MediaMetadataUtils.METADATA_SMALL_LOGO),
                extras.getString(MediaMetadataUtils.METADATA_MEDIUM_LOGO),
                extras.getString(MediaMetadataUtils.METADATA_LARGE_LOGO)
        )
    }

    fun transform(mediaItems: Iterable<MediaBrowserCompat.MediaItem>): List<Radio> {
        return mediaItems.map {
            transform(it)
        }
    }
}
