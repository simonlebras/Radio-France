package com.simonlebras.radiofrance.data.repository

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.utils.AppContexts
import kotlinx.coroutines.experimental.*
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    private val appContexts: AppContexts
) : MediaRepository {
    private val deferredReference =
        AtomicReference<Deferred<List<MediaBrowserCompat.MediaItem>>?>(null)

    override fun loadMediaItemsAsync(): Deferred<List<MediaBrowserCompat.MediaItem>> {
        while (true) {
            deferredReference.get()?.let {
                return it
            }

            val deferred = async(context = appContexts.network, start = CoroutineStart.LAZY) {
                suspendCancellableCoroutine<List<MediaBrowserCompat.MediaItem>> { continuation ->
                    FirebaseFirestore.getInstance()
                        .collection("radios")
                        .get()
                        .addOnSuccessListener {
                            launch(context = appContexts.computation) {
                                val mediaItems = it.toObjects(Radio::class.java).map {
                                    with(it) {
                                        val description = MediaDescriptionCompat.Builder()
                                            .setMediaId(id)
                                            .setTitle(name)
                                            .setDescription(description)
                                            .setMediaUri(stream.toUri())
                                            .setIconUri(logo.toUri())
                                            .build()

                                        MediaBrowserCompat.MediaItem(
                                            description,
                                            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                                        )
                                    }
                                }

                                continuation.resume(mediaItems)
                            }
                        }
                        .addOnFailureListener {
                            deferredReference.set(null)

                            continuation.resumeWithException(it)
                        }
                }
            }

            if (deferredReference.compareAndSet(null, deferred)) {
                return deferred
            }
        }
    }
}
