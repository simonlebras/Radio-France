package com.simonlebras.radiofrance.ui.utils

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.filter
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.selects.whileSelect
import kotlinx.coroutines.experimental.yield
import java.util.concurrent.TimeUnit

fun <T> ReceiveChannel<T>.debounce(
    time: Long,
    unit: TimeUnit = TimeUnit.MILLISECONDS
): ReceiveChannel<T> =
    produce(capacity = CONFLATED) {
        var value = this@debounce.receive()

        whileSelect {
            onTimeout(time, unit) {
                send(value)
                value = this@debounce.receive()
                true
            }

            this@debounce.onReceive {
                value = it
                true
            }
        }
    }

fun <T> ReceiveChannel<T>.distinctUntilChanged(): ReceiveChannel<T> =
    produce(capacity = CONFLATED) {
        var previous: T? = null

        this@distinctUntilChanged
            .filter {
                it != previous
            }
            .consumeEach {
                previous = it

                send(it)
            }
    }

fun <T, R> ReceiveChannel<T>.switchMap(block: suspend (T) -> R): ReceiveChannel<R> =
    produce(capacity = CONFLATED) {
        var job: Job? = null

        this@switchMap.consumeEach {
            job?.cancel()

            job = launch {
                val result = block(it)

                yield()

                send(result)
            }
        }

        job?.join()
    }
