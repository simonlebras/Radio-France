package com.simonlebras.radiofrance.ui.utils

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.RequestBuilder

@BindingAdapter("glideRequest", "imageUrl")
fun ImageView.bindImage(glideRequest: RequestBuilder<Bitmap>, imageUrl: String?) {
    imageUrl?.let {
        glideRequest.load(it)
                .into(this)
    }
}

@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}
