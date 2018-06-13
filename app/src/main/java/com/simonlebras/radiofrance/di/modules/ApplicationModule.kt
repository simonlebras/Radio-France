package com.simonlebras.radiofrance.di.modules

import com.simonlebras.radiofrance.utils.AppSchedulers
import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
abstract class ApplicationModule {
    @Module
    companion object {
        @Provides
        @JvmStatic
        @Singleton
        fun provideAppSchedulers() = AppSchedulers(
                Schedulers.computation(),
                Schedulers.from(Executors.newFixedThreadPool(3)),
                AndroidSchedulers.mainThread()
        )
    }
}
