package fr.simonlebras.radiofrance.playback.mappers

import android.support.v4.media.MediaMetadataCompat
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils

object MediaMetadataMapper {
    fun transform(radio: Radio): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, radio.id)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, radio.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, radio.description)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, radio.stream)
                .putString(MediaMetadataUtils.METADATA_KEY_WEBSITE, radio.website)
                .putString(MediaMetadataUtils.METADATA_KEY_TWITTER, radio.twitter)
                .putString(MediaMetadataUtils.METADATA_KEY_FACEBOOK, radio.facebook)
                .putString(MediaMetadataUtils.METADATA_KEY_SMALL_LOGO, radio.smallLogo)
                .putString(MediaMetadataUtils.METADATA_KEY_MEDIUM_LOGO, radio.mediumLogo)
                .putString(MediaMetadataUtils.METADATA_KEY_LARGE_LOGO, radio.largeLogo)
                .build()
    }

    fun transform(radios: Iterable<Radio>): List<MediaMetadataCompat> {
        return radios.map {
            transform(it)
        }
    }
}
