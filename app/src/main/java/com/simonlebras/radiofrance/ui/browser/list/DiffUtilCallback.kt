package com.simonlebras.radiofrance.ui.browser.list

import androidx.recyclerview.widget.DiffUtil
import com.simonlebras.radiofrance.data.models.Radio

class DiffUtilCallback(
    private val oldList: List<Radio>,
    private val newList: List<Radio>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
        oldList[oldItemPosition] == newList[newItemPosition]
}
