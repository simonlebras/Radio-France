package com.simonlebras.radiofrance.data.repository

import android.support.v4.media.MediaBrowserCompat
import kotlinx.coroutines.experimental.Deferred

interface MediaRepository {
    fun loadMediaItemsAsync(): Deferred<List<MediaBrowserCompat.MediaItem>>
}
