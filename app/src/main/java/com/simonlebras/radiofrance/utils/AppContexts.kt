package com.simonlebras.radiofrance.utils

import kotlin.coroutines.experimental.CoroutineContext

class AppContexts(
    val computation: CoroutineContext,
    val network: CoroutineContext,
    val main: CoroutineContext
)
