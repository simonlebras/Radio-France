package fr.simonlebras.radiofrance.playback

import android.content.Context

interface PlaybackFactory {
    fun create(context: Context): Playback
}
