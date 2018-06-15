package com.simonlebras.radiofrance.playback

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.di.ServiceScope
import com.simonlebras.radiofrance.data.repository.RadioRepository
import javax.inject.Inject

@ServiceScope
class QueueManager @Inject constructor(val context: Context, private val radioRepository: RadioRepository) {
    private companion object {
        private const val INVALID_INDEX = -1
    }

    var listener: Listener? = null
    val currentRadio: MediaSessionCompat.QueueItem?
        get() {
            if (isIndexPlayable(currentIndex)) {
                return queue[currentIndex]
            }

            return null
        }

    private var queue: List<MediaSessionCompat.QueueItem> = emptyList()
    private var currentIndex = INVALID_INDEX

    fun setCurrentQueueItem(queueId: Long): Boolean {
        val index = queue.indexOfFirst {
            it.queueId == queueId
        }

        return setCurrentIndex(index)
    }

    fun setCurrentQueueItem(radioId: String?): Boolean {
        if (queue.isEmpty() && radioRepository.queue.isNotEmpty()) {
            queue = radioRepository.queue
            listener?.onQueueUpdated(context.getString(R.string.app_name), queue)
        }

        val index = queue.indexOfFirst {
            it.description.mediaId == radioId
        }

        return setCurrentIndex(index)
    }

    fun skipToPrevious(): Boolean {
        var index = 0
        if (currentIndex != INVALID_INDEX) {
            index = currentIndex - 1
            if (index < 0) {
                index = queue.size - 1
            }
        }

        return setCurrentIndex(index)
    }

    fun skipToNext(): Boolean {
        var index = currentIndex + 1
        if (queue.isNotEmpty()) {
            index %= queue.size
        }

        return setCurrentIndex(index)
    }

    fun updateMetadata() {
        if (queue.isEmpty() || radioRepository.metadata.isEmpty()) {
            listener?.onMetadataRetrieveError()
            return
        }

        val radioId = currentRadio?.description?.mediaId
        val metadata = radioRepository.metadata[radioId]
        if (metadata == null) {
            listener?.onMetadataRetrieveError()
            return
        }

        listener?.onMetadataChanged(metadata)
    }

    private fun isIndexPlayable(index: Int) = index >= 0 && index < queue.size

    private fun setCurrentIndex(index: Int): Boolean {
        if (isIndexPlayable(index)) {
            currentIndex = index
            return true
        }

        return false
    }

    interface Listener {
        fun onQueueUpdated(title: String, queue: List<MediaSessionCompat.QueueItem>)

        fun onMetadataChanged(metadata: MediaMetadataCompat)

        fun onMetadataRetrieveError()
    }
}
