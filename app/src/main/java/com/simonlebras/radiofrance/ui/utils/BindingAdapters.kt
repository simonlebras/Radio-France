package com.simonlebras.radiofrance.ui.utils

import android.databinding.BindingAdapter
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder

@BindingAdapter("glideRequest", "imageUrl")
fun ImageView.bindImage(glideRequest: RequestBuilder<Bitmap>, imageUrl: String?) {
    imageUrl?.let {
        glideRequest.load(it).into(this)
    }
}

