package fr.simonlebras.radiofrance.ui.browser.list

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

class SpaceItemDecoration(private val columnSpace: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.top = columnSpace
        outRect.bottom = columnSpace
        outRect.left = columnSpace
        outRect.right = columnSpace
    }
}
