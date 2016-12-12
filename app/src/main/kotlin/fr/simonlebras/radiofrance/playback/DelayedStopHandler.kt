package fr.simonlebras.radiofrance.playback

import android.os.Handler
import android.os.Message
import fr.simonlebras.radiofrance.di.scopes.ServiceScope
import java.lang.ref.WeakReference
import javax.inject.Inject

@ServiceScope
class DelayedStopHandler @Inject constructor(service: RadioPlaybackService) : Handler() {
    private val serviceReference = WeakReference(service)

    override fun handleMessage(msg: Message) {
        serviceReference.get()?.let {
            if (!it.playbackManager.isPlaying) {
                it.stopSelf()
            }
        }
    }
}
