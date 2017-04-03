package fr.simonlebras.radiofrance.utils

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicReference

class OnErrorRetryCache<T>(source: Observable<T>) {
    val result: Observable<T> = Observable.defer {
        val observable: Observable<T>
        while (true) {
            cache.get()?.let {
                return@defer it
            }

            val next = source
                    .doOnError {
                        cache.set(null)
                    }
                    .replay(1)
                    .autoConnect(1) {
                        disposable = it
                    }

            if (cache.compareAndSet(null, next)) {
                observable = next
                break
            }
        }

        observable
    }

    @Volatile private var disposable: Disposable? = null
    private val cache = AtomicReference<Observable<T>?>()

    fun dispose() {
        disposable?.dispose()
    }
}
