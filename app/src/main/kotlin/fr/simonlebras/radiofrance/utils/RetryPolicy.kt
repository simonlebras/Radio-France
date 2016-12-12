package fr.simonlebras.radiofrance.utils

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

class RetryPolicy(private val initialDelay: Long,
                  private val delayUnit: TimeUnit,
                  private val retryCount: Int,
                  private vararg val exceptions: KClass<out Throwable>) : Function<Observable<out Throwable>, Observable<*>> {
    private companion object {
        private const val UNCHECKED_ERROR_TYPE_CODE = -100
    }

    override fun apply(errors: Observable<out Throwable>): Observable<*> {
        return errors
                .zipWith(Observable.range(1, retryCount + 1), BiFunction { error: Throwable, retryAttempt: Int ->
                    if (retryAttempt == retryCount + 1) {
                        return@BiFunction Pair(error, UNCHECKED_ERROR_TYPE_CODE)
                    }

                    exceptions
                            .filter {
                                it.java.isInstance(error)
                            }
                            .forEach {
                                return@BiFunction Pair(error, retryAttempt)
                            }

                    Pair(error, UNCHECKED_ERROR_TYPE_CODE)
                })
                .flatMap {
                    val retryAttempt = it.second

                    if (retryAttempt == UNCHECKED_ERROR_TYPE_CODE) {
                        return@flatMap Observable.error<Throwable>(it.first)
                    }

                    val delay = Math.pow(initialDelay.toDouble(), retryAttempt.toDouble()).toLong()
                    Observable.timer(delay, delayUnit)
                }
    }
}
