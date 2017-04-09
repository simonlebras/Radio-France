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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.Lazy
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.ui.base.BaseActivity
import fr.simonlebras.radiofrance.ui.base.BaseFragment
import fr.simonlebras.radiofrance.utils.MediaMetadataUtils
import javax.inject.Inject

class MiniPlayerFragment : BaseFragment<MiniPlayerPresenter>(), MiniPlayerPresenter.View {
    @Inject lateinit var presenterProvider: Lazy<MiniPlayerPresenter>

    @BindView(R.id.image_radio_logo) lateinit var imageRadioLogo: ImageView
    @BindView(R.id.text_radio_title) lateinit var textRadioTitle: TextView
    @BindView(R.id.text_radio_description) lateinit var textRadioDescription: TextView
    @BindView(R.id.button_radio_play_pause) lateinit var buttonRadioPlayPause: ImageButton
    @BindView(R.id.button_radio_skip_previous) lateinit var buttonRadioSkipPrevious: ImageButton
    @BindView(R.id.button_radio_skip_next) lateinit var buttonRadioSkipNext: ImageButton

    private lateinit var unbinder: Unbinder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_mini_player, container, false)

        unbinder = ButterKnife.bind(this, view)

        buttonRadioPlayPause.setOnClickListener {
            val state = MediaControllerCompat.getMediaController(activity).playbackState.state
            when (state) {
                STATE_PAUSED, STATE_STOPPED, STATE_NONE -> presenter.play()
                STATE_PLAYING, STATE_BUFFERING, STATE_CONNECTING -> presenter.pause()
            }
        }

        buttonRadioSkipPrevious.setOnClickListener {
            presenter.skipToPrevious()
        }

        buttonRadioSkipNext.setOnClickListener {
            presenter.skipToNext()
        }

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.onAttachView(this)
    }

    override fun onDestroyView() {
        unbinder.unbind()

        super.onDestroyView()
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
        textRadioTitle.text = metadata.description.title
        textRadioDescription.text = metadata.description.description

        Glide.with(this)
                .from(String::class.java)
                .asBitmap()
                .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_radio_blue_64dp))
                .error(ContextCompat.getDrawable(context, R.drawable.ic_radio_blue_64dp))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .load(metadata.getString(MediaMetadataUtils.METADATA_KEY_SMALL_LOGO))
                .into(imageRadioLogo)
    }

    override fun onPlaybackStateChanged(playbackState: PlaybackStateCompat) {
        if (playbackState.state != STATE_PLAYING) {
            buttonRadioPlayPause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play_arrow_pink_36dp))
        } else {
            buttonRadioPlayPause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pause_pink_36dp))
        }
    }
}
