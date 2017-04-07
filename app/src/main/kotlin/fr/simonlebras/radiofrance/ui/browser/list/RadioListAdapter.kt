package fr.simonlebras.radiofrance.ui.browser.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.di.scopes.FragmentScope
import fr.simonlebras.radiofrance.models.Radio
import kotlinx.android.synthetic.main.list_item_radio.view.*
import javax.inject.Inject

@FragmentScope
class RadioListAdapter @Inject constructor(private val fragment: RadioListFragment) : RecyclerView.Adapter<RadioListAdapter.ViewHolder>() {
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

        viewHolder.itemView.button_radio_links.setOnClickListener {
            val position = viewHolder.adapterPosition
            if (position == NO_POSITION) {
                return@setOnClickListener
            }

            val popup = PopupMenu(fragment.context, it)

            popup.inflate(R.menu.list_item_radio)

            popup.setOnMenuItemClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                when (it.itemId) {
                    R.id.radio_website -> {
                        intent.data = Uri.parse(radios[position].website)
                    }
                    R.id.radio_twitter -> {
                        intent.data = Uri.parse(radios[position].twitter)
                    }
                    R.id.radio_facebook -> {
                        intent.data = Uri.parse(radios[position].facebook)
                    }
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }

                with(fragment.context) {
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }

                return@setOnMenuItemClickListener false
            }

            popup.show()
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
            itemView.text_radio_title.text = title
        }

        fun bindRadioDescription(description: String) {
            itemView.text_radio_description.text = description
        }

        fun bindRadioLogo(logoUrl: String) {
            Glide.with(fragment)
                    .from(String::class.java)
                    .asBitmap()
                    .placeholder(ContextCompat.getDrawable(fragment.context, R.drawable.ic_radio_blue_40dp))
                    .error(ContextCompat.getDrawable(fragment.context, R.drawable.ic_radio_blue_40dp))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .load(logoUrl)
                    .into(itemView.image_radio_logo)
        }
    }
}
