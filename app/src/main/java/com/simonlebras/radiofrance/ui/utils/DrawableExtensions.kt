package com.simonlebras.radiofrance.ui.utils

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat

fun Drawable.mutableTint(@ColorInt tint: Int): Drawable =
    DrawableCompat.wrap(this).apply {
        DrawableCompat.setTint(mutate(), tint)
    }
