package com.simonlebras.radiofrance.ui.browser.list

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecoration(color: Int, width: Float) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply {
        this.color = color
        strokeWidth = width
    }

    private val originalAlpha = paint.alpha

    override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
    ) {
        val position = (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition
        if (position < state.itemCount) {
            outRect.set(0, 0, 0, paint.strokeWidth.toInt())
        } else {
            outRect.setEmpty()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        parent.forEach {
            val position = (it.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition
            if (position < state.itemCount) {
                paint.alpha = (it.alpha * originalAlpha).toInt()

                val y = it.bottom + paint.strokeWidth / 2 + it.translationY
                c.drawLine(it.left + it.translationX, y, it.right + it.translationX, y, paint)
            }
        }
    }
}
