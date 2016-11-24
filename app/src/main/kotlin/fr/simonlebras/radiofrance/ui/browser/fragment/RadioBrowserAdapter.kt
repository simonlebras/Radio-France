package fr.simonlebras.radiofrance.ui.browser.fragment

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.models.Radio
import kotlinx.android.synthetic.main.item_radio.view.*
import javax.inject.Inject

@FragmentScope
class RadioBrowserAdapter @Inject constructor(val fragment: RadioBrowserFragment) : RecyclerView.Adapter<RadioBrowserAdapter.ViewHolder>() {
    var radios: List<Radio> = emptyList()

    private val inflater = LayoutInflater.from(fragment.context)
    private val glide = Glide.with(fragment).from(String::class.java)
            .placeholder(ContextCompat.getDrawable(fragment.context, R.drawable.ic_radio_blue_40dp))
            .error(ContextCompat.getDrawable(fragment.context, R.drawable.ic_radio_blue_40dp))
            .diskCacheStrategy(DiskCacheStrategy.ALL)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.item_radio, parent, false)
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
        }
    }

    override fun getItemCount() = radios.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindRadio(radio: Radio) {
            bindRadioTitle(radio.name)
            bindRadioDescription(radio.description)
            bindRadioLogo(radio.smallLogo)
        }

        fun bindRadioTitle(title: String) {
            itemView.radio_title.text = title
        }

        fun bindRadioDescription(description: String) {
            itemView.radio_description.text = description
        }

        fun bindRadioLogo(logoUrl: String) {
            glide.load(logoUrl).into(itemView.radio_thumbnail)
        }
    }
}
