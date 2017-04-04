package fr.simonlebras.radiofrance.ui.base

import android.support.v4.util.ArrayMap
import java.util.*

class PresenterManager {
    private val cache = ArrayMap<UUID, BasePresenter<out BaseView>>()

    operator fun get(uuid: UUID) = cache[uuid]

    operator fun set(uuid: UUID, presenter: BasePresenter<out BaseView>) {
        cache[uuid] = presenter
    }

    fun remove(uuid: UUID) = cache.remove(uuid)
}
