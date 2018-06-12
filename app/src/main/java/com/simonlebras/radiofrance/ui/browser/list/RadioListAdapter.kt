package com.simonlebras.radiofrance.ui.browser.list

import android.support.v4.content.ContextCompat
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.models.Radio
import com.simonlebras.radiofrance.utils.GlideApp
import kotlinx.android.synthetic.main.list_item_radio.view.*

class RadioListAdapter(
        private val fragment: RadioListFragment
) : ListAdapter<Radio, RadioListAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(fragment.context)

        return ViewHolder(inflater.inflate(R.layout.list_item_radio, parent, false))
                .apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION) {
                            fragment.onRadioSelected(getItem(adapterPosition).id)
                        }
                    }
                }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindRadio(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindRadio(radio: Radio) {
            bindRadioTitle(radio.name)
            bindRadioDescription(radio.description)
            bindRadioLogo(radio.logo)
        }

        private fun bindRadioTitle(title: String) {
            itemView.text_radio_title.text = title
        }

        private fun bindRadioDescription(description: String) {
            itemView.text_radio_description.text = description
        }

        private fun bindRadioLogo(logoUrl: String) {
            GlideApp.with(fragment)
                    .asBitmap()
                    .placeholder(ContextCompat.getDrawable(fragment.context!!, R.drawable.ic_radio_blue_40dp))
                    .load(logoUrl)
                    .into(itemView.image_radio_logo)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Radio>() {
        override fun areItemsTheSame(oldItem: Radio, newItem: Radio) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Radio, newItem: Radio) = oldItem == newItem
    }
}
