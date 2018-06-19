package com.simonlebras.radiofrance.ui.utils

import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.v4.graphics.drawable.DrawableCompat

fun Drawable.mutableTint(@ColorInt tint: Int): Drawable =
    DrawableCompat.wrap(this).apply {
        DrawableCompat.setTint(mutate(), tint)
    }
