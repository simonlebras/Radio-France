package fr.simonlebras.radiofrance.ui.browser.player

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.Lazy
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.base.BaseFragment
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils
import kotlinx.android.synthetic.main.fragment_mini_player.*
import kotlinx.android.synthetic.main.fragment_mini_player.view.*
import javax.inject.Inject

class MiniPlayerFragment : BaseFragment<MiniPlayerPresenter>(), MiniPlayerPresenter.View {
    @Inject lateinit var presenterProvider: Lazy<MiniPlayerPresenter>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mini_player, container, false)

        view.button_radio_play_pause.setOnClickListener {
            val state = MediaControllerCompat.getMediaController(activity).playbackState.state
            when (state) {
                STATE_PAUSED, STATE_STOPPED, STATE_NONE -> presenter.play()
                STATE_PLAYING, STATE_BUFFERING, STATE_CONNECTING -> presenter.pause()
            }
        }

        view.button_radio_skip_previous.setOnClickListener {
            presenter.skipToPrevious()
        }

        view.button_radio_skip_next.setOnClickListener {
            presenter.skipToNext()
        }

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.onAttachView(this)
    }

    override fun restorePresenter() {
        val presenterManager = (activity as BaseActivity<*>).presenterManager
        presenter = presenterManager[uuid] as? MiniPlayerPresenter ?: presenterProvider.get()
        presenterManager[uuid] = presenter
    }

    fun onConnected() {
        val controller = MediaControllerCompat.getMediaController(activity)

        controller.metadata?.let {
            onMetadataChanged(it)
        }

        controller.playbackState?.let {
            onPlaybackStateChanged(it)
        }

        presenter.subscribeToPlaybackUpdates()
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat) {
        text_radio_title.text = metadata.description.title
        text_radio_description.text = metadata.description.description

        Glide.with(this)
                .from(String::class.java)
                .asBitmap()
                .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_radio_blue_64dp))
                .error(ContextCompat.getDrawable(context, R.drawable.ic_radio_blue_64dp))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .load(metadata.getString(MediaMetadataUtils.METADATA_KEY_SMALL_LOGO))
                .into(image_radio_logo)
    }

    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        if (playbackState.state != STATE_PLAYING) {
            button_radio_play_pause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play_arrow_pink_36dp))
        } else {
            button_radio_play_pause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pause_pink_36dp))
        }
    }
}
