package com.simonlebras.radiofrance.playback

import android.support.v4.media.session.MediaSessionCompat
import kotlin.properties.Delegates

private const val INVALID_INDEX = -1

class QueueManager(
        var queue: List<MediaSessionCompat.QueueItem>,
        private val callback: Callback
) {
    var currentItem by Delegates.observable<MediaSessionCompat.QueueItem?>(null) { _, _, item ->
        callback.onQueueItemChanged(item!!)
    }

    private var currentIndex by Delegates.observable(INVALID_INDEX) { _, _, index ->
        if (queue.isEmpty() || index < 0 || index > queue.size) {
            return@observable
        }

        currentItem = queue[index]
    }

    fun skipToPrevious() {
        if (currentIndex == INVALID_INDEX) {
            currentIndex = 0
        } else {
            var index = currentIndex - 1
            if (index < 0) {
                index = queue.size - 1
            }

            currentIndex = index
        }
    }

    fun skipToNext() {
        currentIndex = (currentIndex + 1) % queue.size
    }

    fun skipToPosition(position: Long) {
        currentIndex = queue.indexOfFirst {
            it.queueId == position
        }
    }

    fun skipToItem(itemId: String) {
        currentIndex = queue.indexOfFirst {
            it.description.mediaId == itemId
        }
    }

    interface Callback {
        fun onQueueItemChanged(item: MediaSessionCompat.QueueItem)
    }
}
