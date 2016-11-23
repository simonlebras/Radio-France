package fr.simonlebras.radiofrance.playback.mappers

import android.support.v4.media.MediaMetadataCompat
import com.google.firebase.database.DataSnapshot
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.models.Radio
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils
import javax.inject.Inject

@ServiceScope
class MediaMetadataMapper @Inject constructor() {
    fun transform(snapshot: DataSnapshot): MediaMetadataCompat {
        val radio = snapshot.getValue(Radio::class.java)

        return MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, radio.id)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, radio.name)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, radio.description)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, radio.stream)
                .putString(MediaMetadataUtils.METADATA_WEBSITE, radio.website)
                .putString(MediaMetadataUtils.METADATA_TWITTER, radio.twitter)
                .putString(MediaMetadataUtils.METADATA_FACEBOOK, radio.facebook)
                .putString(MediaMetadataUtils.METADATA_SMALL_LOGO, radio.smallLogo)
                .putString(MediaMetadataUtils.METADATA_MEDIUM_LOGO, radio.mediumLogo)
                .putString(MediaMetadataUtils.METADATA_LARGE_LOGO, radio.largeLogo)
                .build()
    }

    fun transform(snapshots: Iterable<DataSnapshot>): List<MediaMetadataCompat> {
        return snapshots.map {
            transform(it)
        }
    }
}
