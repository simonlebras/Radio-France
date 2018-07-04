package com.simonlebras.radiofrance.di

import com.simonlebras.radiofrance.data.repository.MediaRepository
import com.simonlebras.radiofrance.data.repository.MediaRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
abstract class RadioPlaybackModule {
    @Binds
    abstract fun providemediaRepository(mediaRepository: MediaRepositoryImpl): MediaRepository
}
