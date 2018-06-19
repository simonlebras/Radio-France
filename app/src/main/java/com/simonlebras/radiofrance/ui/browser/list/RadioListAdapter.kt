package com.simonlebras.radiofrance.ui.browser.list

import android.graphics.Bitmap
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.ListPreloader
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.data.models.Radio
import com.simonlebras.radiofrance.databinding.ListItemRadioBinding
import com.simonlebras.radiofrance.ui.utils.mutableTint
import com.simonlebras.radiofrance.utils.GlideApp
import com.simonlebras.radiofrance.utils.GlideRequest

class RadioListAdapter(
    private val fragment: RadioListFragment
) : RecyclerView.Adapter<RadioListAdapter.ViewHolder>(),
    ListPreloader.PreloadModelProvider<Radio> {
    var radios = emptyList<Radio>()

    private val glideRequest: GlideRequest<Bitmap>

    init {
        val context = fragment.requireContext()

        val tint = ContextCompat.getColor(context, R.color.colorPrimary)

        val drawable =
            ContextCompat.getDrawable(context, R.drawable.ic_radio)!!.mutableTint(tint)

        glideRequest = GlideApp.with(fragment)
            .asBitmap()
            .placeholder(drawable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = ListItemRadioBinding.inflate(inflater, parent, false)
            .also {
                it.glideRequest = glideRequest
                it.viewModel = fragment.viewModel
            }

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(radios[position])
    }

    override fun getItemCount() = radios.size

    override fun getPreloadItems(position: Int) = listOf(radios[position])

    override fun getPreloadRequestBuilder(item: Radio) = glideRequest.load(item.logo)

    class ViewHolder(
        private val binding: ListItemRadioBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(radio: Radio) {
            binding.radio = radio
            binding.executePendingBindings()
        }
    }
}
