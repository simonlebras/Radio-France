package com.simonlebras.radiofrance.ui.browser.player

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.databinding.FragmentMiniPlayerBinding
import com.simonlebras.radiofrance.ui.MainViewModel
import com.simonlebras.radiofrance.ui.utils.mutableTint
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
            .get(MainViewModel::class.java)

        viewModel.connect()

        binding.viewModel = viewModel

        val tint = ContextCompat.getColor(requireContext(), R.color.colorPrimary)

        val drawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_radio)!!.mutableTint(tint)

        binding.glideRequest = GlideApp.with(this)
            .asBitmap()
            .placeholder(drawable)

        viewModel.playbackState.observe(
            viewLifecycleOwner,
            Observer { binding.playbackState = it }
        )
        viewModel.metadata.observe(viewLifecycleOwner, Observer { binding.metadata = it })
    }
}
