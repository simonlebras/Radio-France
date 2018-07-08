package com.simonlebras.radiofrance.ui.browser.player

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.widget.ImageButton
import com.simonlebras.radiofrance.R
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
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

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @TargetApi(Build.VERSION_CODES.M)
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

                    suspendCancellableCoroutine<Unit> {
                        if (drawable is AnimatedVectorDrawable) {
                            with(drawable as AnimatedVectorDrawable) {
                                registerAnimationCallback(object :
                                                              Animatable2.AnimationCallback() {
                                    override fun onAnimationEnd(drawable: Drawable?) {
                                        unregisterAnimationCallback(this)

                                        it.resume(Unit)
                                    }
                                })

                                start()
                            }
                        } else {
                            with(drawable as AnimatedVectorDrawableCompat) {
                                registerAnimationCallback(object :
                                                              Animatable2Compat.AnimationCallback() {
                                    override fun onAnimationEnd(drawable: Drawable?) {
                                        unregisterAnimationCallback(this)

                                        it.resume(Unit)
                                    }
                                })

                                start()
                            }
                        }
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
