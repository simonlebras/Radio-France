package fr.simonlebras.radiofrance.ui.browser.fragment

import android.os.Bundle
import android.support.v7.util.DiffUtil
import fr.simonlebras.radiofrance.models.Radio

class RadioListDiffCallback(private val oldList: List<Radio>,  val newList: List<Radio>) : DiffUtil.Callback() {
    companion object {
        const val BUNDLE_DIFF_NAME = "BUNDLE_DIFF_NAME"
        const val BUNDLE_DIFF_DESCRIPTION = "BUNDLE_DIFF_DESCRIPTION"
        const val BUNDLE_DIFF_LOGO = "BUNDLE_DIFF_LOGO"
    }

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].id == newList[newItemPosition].id

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldRadio = oldList[oldItemPosition]
        val newRadio = newList[newItemPosition]

        if (oldRadio.name != newRadio.name) {
            return false
        }

        if (oldRadio.description != newRadio.description) {
            return false
        }

        if ((oldRadio.smallLogo != newRadio.smallLogo) ||
                (oldRadio.mediumLogo != newRadio.mediumLogo) ||
                (oldRadio.largeLogo != newRadio.largeLogo)) {
            return false
        }

        return true
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Bundle {
        val diff = Bundle()

        val oldRadio = oldList[oldItemPosition]
        val newRadio = newList[newItemPosition]

        if (oldRadio.name != newRadio.name) {
            diff.putBoolean(BUNDLE_DIFF_NAME, true)
        }

        if (oldRadio.description != newRadio.description) {
            diff.putBoolean(BUNDLE_DIFF_DESCRIPTION, true)
        }

        if ((oldRadio.smallLogo != newRadio.smallLogo) ||
                (oldRadio.mediumLogo != newRadio.mediumLogo) ||
                (oldRadio.largeLogo != newRadio.largeLogo)) {
            diff.putBoolean(BUNDLE_DIFF_LOGO, true)
        }

        return diff
    }
}
