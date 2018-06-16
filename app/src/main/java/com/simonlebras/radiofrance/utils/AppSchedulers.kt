package com.simonlebras.radiofrance.utils

import io.reactivex.Scheduler

class AppSchedulers(
    val computation: Scheduler,
    val network: Scheduler,
    val main: Scheduler
)
