package com.simonlebras.radiofrance.data.repository

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.simonlebras.radiofrance.data.model.Radio
import com.simonlebras.radiofrance.di.scopes.ServiceScope
import com.simonlebras.radiofrance.playback.mappers.MediaMetadataMapper
import com.simonlebras.radiofrance.playback.mappers.QueueItemMapper
import com.simonlebras.radiofrance.utils.AppSchedulers
import com.simonlebras.radiofrance.utils.OnErrorRetryCache
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

@ServiceScope
class RadioRepositoryImpl @Inject constructor(
        private val appSchedulers: AppSchedulers
) : RadioRepository {
    @Volatile
    override var queue: List<MediaSessionCompat.QueueItem> = emptyList()

    @Volatile
    override var metadata: Map<String, MediaMetadataCompat> = linkedMapOf()

    private var retryCache: OnErrorRetryCache<List<MediaMetadataCompat>>? = null

    override val radios: Observable<List<MediaMetadataCompat>> by lazy(LazyThreadSafetyMode.NONE) {
        val source = Single
                .create<List<Radio>> { emitter ->
                    FirebaseFirestore.getInstance()
                            .collection(RADIO_COLLECTION)
                            .get()
                            .addOnSuccessListener {
                                val size = it.isEmpty
                                val d = it.documentChanges
                                val dd = it.documents
                                emitter.onSuccess(it.toObjects(Radio::class.java))
                            }
                            .addOnFailureListener { emitter.onError(it) }
                }
                .toObservable()
                .subscribeOn(appSchedulers.network)
                .map { MediaMetadataMapper.transform(it) }
                .doOnNext {
                    val queue: MutableList<MediaSessionCompat.QueueItem> = mutableListOf()
                    val metadata: MutableMap<String, MediaMetadataCompat> = mutableMapOf()

                    for ((index, value) in it.withIndex()) {
                        queue.add(QueueItemMapper.transform(value, index.toLong()))
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

    private companion object {
        const val RADIO_COLLECTION = "radios"
    }
}
