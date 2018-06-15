package com.simonlebras.radiofrance.data.repository

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.di.ServiceScope
import com.simonlebras.radiofrance.playback.mappers.MediaMetadataMapper
import com.simonlebras.radiofrance.playback.mappers.QueueItemMapper
import com.simonlebras.radiofrance.utils.AppSchedulers
import com.simonlebras.radiofrance.utils.OnErrorRetryCache
import io.reactivex.Observable
import javax.inject.Inject

@ServiceScope
class RadioRepositoryImpl @Inject constructor(
        private val appSchedulers: AppSchedulers
) : RadioRepository {
    override var queue: List<MediaSessionCompat.QueueItem> = emptyList()

    override var metadata: Map<String, MediaMetadataCompat> = linkedMapOf()

    private var retryCache: OnErrorRetryCache<List<MediaMetadataCompat>>? = null

    override val radios: Observable<List<MediaMetadataCompat>> by lazy(LazyThreadSafetyMode.NONE) {
        val source = Observable
                .create<List<Radio>> { emitter ->
                    FirebaseFirestore.getInstance()
                            .collection("radios")
                            .get()
                            .addOnSuccessListener {
                                if (!emitter.isDisposed) {
                                    emitter.onNext(it.toObjects(Radio::class.java))
                                }
                            }
                            .addOnFailureListener {
                                if (!emitter.isDisposed) {
                                    emitter.onError(it)
                                }
                            }
                }
                .observeOn(appSchedulers.computation)
                .map { MediaMetadataMapper().transform(it) }
                .doOnNext {
                    val queue: MutableList<MediaSessionCompat.QueueItem> = mutableListOf()
                    val metadata: MutableMap<String, MediaMetadataCompat> = mutableMapOf()

                    for ((index, value) in it.withIndex()) {
                        queue.add(QueueItemMapper().transform(value, index.toLong()))
                        metadata[value.description.mediaId!!] = value
                    }

                    this.queue = queue
                    this.metadata = metadata
                }

        retryCache = OnErrorRetryCache(source)
        retryCache!!.result
    }

    override fun reset() {
        retryCache?.dispose()
    }
}
