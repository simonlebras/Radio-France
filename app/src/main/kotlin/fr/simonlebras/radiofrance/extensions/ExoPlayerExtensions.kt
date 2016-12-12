package fr.simonlebras.radiofrance.extensions

import com.google.android.exoplayer2.ExoPlayer

val ExoPlayer.isPlaying: Boolean
    get() = playWhenReady && (playbackState == ExoPlayer.STATE_READY)
