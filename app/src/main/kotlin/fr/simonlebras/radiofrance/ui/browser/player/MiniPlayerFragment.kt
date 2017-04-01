package fr.simonlebras.radiofrance.ui.browser.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.BitmapRequestBuilder
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
    @Inject lateinit var glideRequest: BitmapRequestBuilder<String, Bitmap>

    private var callback: MiniPlayerFragment.Callback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        callback = context as MiniPlayerFragment.Callback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mini_player, container, false)

        view.button_radio_play_pause.setOnClickListener {
            val controller = MediaControllerCompat.getMediaController(activity)
            val playbackState = controller.playbackState
            val state = playbackState.state
            if (state == PlaybackStateCompat.STATE_PAUSED ||
                    state == PlaybackStateCompat.STATE_STOPPED ||
                    state == PlaybackStateCompat.STATE_NONE) {
                presenter.play()
            } else if (state == PlaybackStateCompat.STATE_PLAYING ||
                    state == PlaybackStateCompat.STATE_BUFFERING ||
                    state == PlaybackStateCompat.STATE_CONNECTING) {
                presenter.pause()
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

    override fun onStart() {
        super.onStart()

        presenter.onAttachView(this)
    }

    override fun onDetach() {
        callback = null

        super.onDetach()
    }

    override fun restorePresenter() {
        val presenterManager = (activity as BaseActivity<*>).presenterManager
        presenter = presenterManager[uuid] as? MiniPlayerPresenter ?: presenterProvider.get()
        presenterManager[uuid] = presenter
    }

    fun onConnected() {
        val controller = MediaControllerCompat.getMediaController(activity)
        onMetadataChanged(controller.metadata)
        onPlaybackStateChanged(controller.playbackState)

        presenter.subscribeToPlaybackUpdates()
    }

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        if (metadata == null) {
            return
        }

        text_radio_title.text = metadata.description.title
        text_radio_description.text = metadata.description.description

        glideRequest
                .load(metadata.getString(MediaMetadataUtils.METADATA_KEY_SMALL_LOGO))
                .into(image_radio_logo)
    }

    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat?) {
        if (playbackState == null) {
            return
        }

        var enablePlay = false
        when (playbackState.state) {
            PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.STATE_STOPPED -> enablePlay = true
            PlaybackStateCompat.STATE_ERROR -> {
                callback!!.showPlaybackError(playbackState.errorMessage.toString())
            }
        }

        if (enablePlay) {
            button_radio_play_pause.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_play_arrow_pink_36dp))
        } else {
            button_radio_play_pause.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_pause_pink_36dp))
        }
    }

    interface Callback {
        fun showPlaybackError(error: String)
    }
}
