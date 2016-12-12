package fr.simonlebras.radiofrance.utils

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicReference

class OnErrorRetryCache<T>(source: Observable<T>) {
    val result: Observable<T> = Observable.defer {
        val observable: Observable<T>
        while (true) {
            val connection = cacheReference.get()
            if (connection != null) {
                observable = connection
                break
            }

            val next = source
                    .doOnError {
                        cacheReference.set(null)
                        disposableReference.set(null)
                    }
                    .replay(1)
                    .autoConnect(1) {
                        disposableReference.set(it)
                    }

            if (cacheReference.compareAndSet(null, next)) {
                observable = next
                break
            }
        }

        observable
    }

    private val cacheReference = AtomicReference<Observable<T>?>()
    private val disposableReference = AtomicReference<Disposable?>()

    fun reset() {
        disposableReference.get()?.dispose()
    }
}
