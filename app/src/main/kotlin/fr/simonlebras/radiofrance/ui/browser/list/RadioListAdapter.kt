package fr.simonlebras.radiofrance.ui.browser.list

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.BitmapRequestBuilder
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.models.Radio
import kotlinx.android.synthetic.main.list_item_radio.view.*
import javax.inject.Inject

@FragmentScope
class RadioListAdapter @Inject constructor(val fragment: RadioListFragment,
                                           val glideRequest: BitmapRequestBuilder<String, Bitmap>) : RecyclerView.Adapter<RadioListAdapter.ViewHolder>() {
    var radios: List<Radio> = emptyList()

    private val inflater = LayoutInflater.from(fragment.context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.list_item_radio, parent, false)
        return ViewHolder(view)
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
                if (key == RadioListDiffCallback.BUNDLE_DIFF_NAME) {
                    holder.bindRadioTitle(name)
                } else if (key == RadioListDiffCallback.BUNDLE_DIFF_DESCRIPTION) {
                    holder.bindRadioDescription(description)
                } else if (key == RadioListDiffCallback.BUNDLE_DIFF_LOGO) {
                    holder.bindRadioLogo(smallLogo)
                }
            }

            holder.radioId = id
        }
    }

    override fun getItemCount() = radios.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        lateinit var radioId: String

        init {
            itemView.setOnClickListener {
                fragment.onRadioSelected(radioId)
            }
        }

        fun bindRadio(radio: Radio) {
            bindRadioTitle(radio.name)
            bindRadioDescription(radio.description)
            bindRadioLogo(radio.smallLogo)

            radioId = radio.id
        }

        fun bindRadioTitle(title: String) {
            itemView.text_radio_title.text = title
        }

        fun bindRadioDescription(description: String) {
            itemView.text_radio_description.text = description
        }

        fun bindRadioLogo(logoUrl: String) {
            glideRequest.load(logoUrl).into(itemView.image_radio_logo)
        }
    }
}
