package com.simonlebras.radiofrance.ui.browser.player

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.ui.MainViewModel
import com.simonlebras.radiofrance.utils.GlideApp
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_mini_player.*
import javax.inject.Inject

class MiniPlayerFragment : DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mini_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_radio_play_pause.setOnClickListener { viewModel.togglePlayPause() }

        button_radio_skip_previous.setOnClickListener { viewModel.skipToPrevious() }

        button_radio_skip_next.setOnClickListener { viewModel.skipToNext() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(requireActivity(), viewModelFactory)
                .get(MainViewModel::class.java)

        viewModel.connect()

        viewModel.playbackState.observe(viewLifecycleOwner, Observer { onPlaybackStateChanged(it!!) })
        viewModel.metadata.observe(viewLifecycleOwner, Observer { onMetadataChanged(it!!) })
    }

    private fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        val drawable = if (playbackState.state != STATE_PLAYING) {
            R.drawable.ic_play_arrow_pink_36dp
        } else {
            R.drawable.ic_pause_pink_36dp
        }

        button_radio_play_pause.setImageDrawable(ContextCompat.getDrawable(requireContext(), drawable))
    }

    private fun onMetadataChanged(metadata: MediaMetadataCompat) {
        with(metadata.description) {
            text_radio_title.text = title
            text_radio_description.text = description

            GlideApp.with(this@MiniPlayerFragment)
                    .load(iconUri)
                    .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.ic_radio_blue_64dp))
                    .into(image_radio_logo)
        }
    }
}
