package com.simonlebras.radiofrance.ui.browser.player

import android.content.Context
import android.graphics.drawable.Animatable
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.widget.ImageButton
import com.simonlebras.radiofrance.R
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.withContext
import kotlin.properties.Delegates

class PlayPauseButton : ImageButton {
    var playbackState by Delegates.observable<PlaybackStateCompat?>(null) { _, _, new ->
        if (this::actor.isInitialized && !actor.isClosedForSend) {
            new?.let {
                actor.offer(it)
            }
        }
    }

    private var previousPlaybackState: Int? = null
    private val pausedStates = setOf(
        PlaybackStateCompat.STATE_NONE,
        PlaybackStateCompat.STATE_BUFFERING,
        PlaybackStateCompat.STATE_PAUSED,
        PlaybackStateCompat.STATE_STOPPED,
        PlaybackStateCompat.STATE_ERROR
    )

    private lateinit var actor: SendChannel<PlaybackStateCompat>

    private val animationDuration by lazy(LazyThreadSafetyMode.NONE) {
        resources.getInteger(R.integer.play_pause_animation_duration)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        actor = actor(context = UI, capacity = Channel.CONFLATED) {
            channel.consumeEach {
                if (previousPlaybackState == it.state) {
                    return@consumeEach
                }

                if (previousPlaybackState == null ||
                    previousPlaybackState == PlaybackStateCompat.STATE_NONE ||
                    previousPlaybackState == PlaybackStateCompat.STATE_ERROR
                ) {
                    val drawable = if (it.state == PlaybackStateCompat.STATE_PLAYING) {
                        R.drawable.ic_pause
                    } else {
                        R.drawable.ic_play_arrow
                    }

                    setImageResource(drawable)
                } else if (it.state == PlaybackStateCompat.STATE_PLAYING ||
                    !pausedStates.contains(previousPlaybackState!!)
                ) {
                    val animatedDrawable = if (it.state == PlaybackStateCompat.STATE_PLAYING) {
                        R.drawable.avd_play_to_pause
                    } else {
                        R.drawable.avd_pause_to_play
                    }

                    setImageResource(animatedDrawable)

                    (drawable as Animatable).start()

                    withContext(CommonPool) {
                        delay(animationDuration)
                    }
                }

                previousPlaybackState = it.state
            }
        }

        playbackState?.let {
            actor.offer(it)
        }
    }

    override fun onDetachedFromWindow() {
        actor.close()

        super.onDetachedFromWindow()
    }
}
