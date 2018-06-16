package com.simonlebras.radiofrance.ui.browser.list

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.ListPreloader
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.utils.GlideApp
import kotlinx.android.synthetic.main.list_item_radio.view.*

class RadioListAdapter(
        private val fragment: RadioListFragment
) : RecyclerView.Adapter<RadioListAdapter.ViewHolder>(),
        ListPreloader.PreloadModelProvider<Radio> {
    var radios = emptyList<Radio>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(fragment.context)

        return ViewHolder(inflater.inflate(R.layout.list_item_radio, parent, false))
                .apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION) {
                            fragment.onRadioSelected(radios[adapterPosition].id)
                        }
                    }

                    fragment.preloadSizeProvider.setView(itemView.image_radio_logo)
                }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindRadio(radios[position])
    }

    override fun getItemCount() = radios.size

    override fun getPreloadItems(position: Int) = listOf(radios[position])

    override fun getPreloadRequestBuilder(item: Radio) = GlideApp.with(fragment).load(item.logo)

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
                    .load(logoUrl)
                    .placeholder(ContextCompat.getDrawable(fragment.context!!, R.drawable.ic_radio_blue_40dp))
                    .into(itemView.image_radio_logo)
        }
    }
}
