package com.simonlebras.radiofrance.ui.browser.player

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.databinding.FragmentMiniPlayerBinding
import com.simonlebras.radiofrance.ui.MainViewModel
import com.simonlebras.radiofrance.ui.utils.mutableTint
import com.simonlebras.radiofrance.ui.utils.observeK
import com.simonlebras.radiofrance.ui.utils.withViewModel
import com.simonlebras.radiofrance.utils.GlideApp
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class MiniPlayerFragment : DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentMiniPlayerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMiniPlayerBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tint = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val drawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_radio)!!.mutableTint(tint)

        binding.glideRequest = GlideApp.with(this@MiniPlayerFragment)
            .asBitmap()
            .placeholder(drawable)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireActivity().withViewModel<MainViewModel>(viewModelFactory) {
            connect()

            binding.viewModel = this

            playbackState.observeK(viewLifecycleOwner) {
                binding.playbackState = it
            }

            metadata.observeK(viewLifecycleOwner) {
                binding.metadata = it
            }
        }
    }
}
