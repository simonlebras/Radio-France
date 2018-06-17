package com.simonlebras.radiofrance.ui

import android.databinding.BindingAdapter
import android.graphics.Bitmap
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.simonlebras.radiofrance.R

@BindingAdapter("glideRequest", "imageUrl")
fun ImageView.bindImage(glideRequest: RequestBuilder<Bitmap>, imageUrl: String?) {
    imageUrl?.let {
        glideRequest.load(it).into(this)
    }
}

@BindingAdapter("playbackState")
fun ImageButton.bindPlaybackState(playbackState: PlaybackStateCompat?) {
    val drawable =
        if (playbackState == null || playbackState.state != PlaybackStateCompat.STATE_PLAYING) {
            R.drawable.ic_play_arrow_pink_36dp
        } else {
            R.drawable.ic_pause_pink_36dp
        }

    setImageDrawable(ContextCompat.getDrawable(context, drawable))
}

