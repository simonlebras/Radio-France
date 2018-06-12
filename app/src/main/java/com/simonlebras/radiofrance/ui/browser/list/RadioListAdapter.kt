package com.simonlebras.radiofrance.ui.browser.list

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.models.Radio
import com.simonlebras.radiofrance.utils.GlideApp
import kotlinx.android.synthetic.main.list_item_radio.view.*

class RadioListAdapter(private val fragment: RadioListFragment) : RecyclerView.Adapter<RadioListAdapter.ViewHolder>() {
    var radios: List<Radio> = emptyList()

    private val inflater = LayoutInflater.from(fragment.context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewHolder = ViewHolder(inflater.inflate(R.layout.list_item_radio, parent, false))

        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position != NO_POSITION) {
                fragment.onRadioSelected(radios[position].id)
            }
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindRadio(radios[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        with(radios[position]) {
            val diff = payloads[0] as Bundle
            for (key in diff.keySet()) {
                when (key) {
                    RadioListDiffCallback.BUNDLE_DIFF_NAME -> holder.bindRadioTitle(name)
                    RadioListDiffCallback.BUNDLE_DIFF_DESCRIPTION -> holder.bindRadioDescription(description)
                    RadioListDiffCallback.BUNDLE_DIFF_LOGO -> holder.bindRadioLogo(logo)
                }
            }
        }
    }

    override fun getItemCount() = radios.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindRadio(radio: Radio) {
            bindRadioTitle(radio.name)
            bindRadioDescription(radio.description)
            bindRadioLogo(radio.logo)
        }

        fun bindRadioTitle(title: String) {
            itemView.text_radio_title.text = title
        }

        fun bindRadioDescription(description: String) {
            itemView.text_radio_description.text = description
        }

        fun bindRadioLogo(logoUrl: String) {
            GlideApp.with(fragment)
                    .asBitmap()
                    .placeholder(ContextCompat.getDrawable(fragment.context!!, R.drawable.ic_radio_blue_40dp))
                    .error(ContextCompat.getDrawable(fragment.context!!, R.drawable.ic_radio_blue_40dp))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .load(logoUrl)
                    .into(itemView.image_radio_logo)
        }
    }
}
