package com.simonlebras.radiofrance.ui.browser.list

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

class DividerItemDecoration(color: Int, width: Float) : RecyclerView.ItemDecoration() {
    private val paint = Paint().apply {
        this.color = color
        strokeWidth = width
    }
    private val alpha = paint.alpha

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition
        if (position < state.itemCount) {
            outRect.set(0, 0, 0, paint.strokeWidth.toInt())
        } else {
            outRect.setEmpty()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val view = parent.getChildAt(i)

            val position = (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition
            if (position < state.itemCount) {
                val positionY = view.bottom + paint.strokeWidth / 2 + view.translationY
                paint.alpha = (view.alpha * alpha).toInt()

                c.drawLine(view.left + view.translationX, positionY, view.right + view.translationX, positionY, paint)
            }
        }
    }
}
