package com.simonlebras.radiofrance.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.v4.graphics.drawable.DrawableCompat

fun Drawable.mutableTint(@ColorInt tint: Int): Drawable {
    return DrawableCompat.wrap(this).apply {
        DrawableCompat.setTint(mutate(), tint)
    }
}

fun Drawable.toBitmap(context: Context): Bitmap {
    val width = intrinsicWidth
    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap).apply {
        drawColor(Color.WHITE)
    }
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)

    return bitmap
}
