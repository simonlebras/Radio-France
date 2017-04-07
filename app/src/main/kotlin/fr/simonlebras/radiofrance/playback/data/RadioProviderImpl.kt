package fr.simonlebras.radiofrance.playback.data

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import fr.simonlebras.radiofrance.playback.mappers.MediaMetadataMapper
import fr.simonlebras.radiofrance.playback.mappers.QueueItemMapper
import fr.simonlebras.radiofrance.utils.OnErrorRetryCache
import fr.simonlebras.radiofrance.utils.RetryPolicy
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ServiceScope
class RadioProviderImpl @Inject constructor(private val firebaseService: FirebaseService) : RadioProvider {
    @Volatile override var queue: List<MediaSessionCompat.QueueItem> = emptyList()

    @Volatile override var metadata: Map<String, MediaMetadataCompat> = linkedMapOf()

    private var retryCache: OnErrorRetryCache<List<MediaMetadataCompat>>? = null

    override val radios: Observable<List<MediaMetadataCompat>> by lazy(LazyThreadSafetyMode.NONE) {
        val source = firebaseService.getRadios()
                .subscribeOn(Schedulers.io())
                .retryWhen(RetryPolicy(2, TimeUnit.SECONDS, 3, IOException::class))
                .map {
                    MediaMetadataMapper.transform(it)
                }
                .doOnNext {
                    val queue: MutableList<MediaSessionCompat.QueueItem> = mutableListOf()
                    val metadata: MutableMap<String, MediaMetadataCompat> = mutableMapOf()

                    for ((index, value) in it.withIndex()) {
                        queue.add(QueueItemMapper.transform(value, index.toLong()))
                        metadata.put(value.description.mediaId!!, value)
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
