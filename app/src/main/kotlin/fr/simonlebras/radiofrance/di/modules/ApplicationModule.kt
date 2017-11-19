package fr.simonlebras.radiofrance.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.utils.DebugUtils
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class ApplicationModule {
    private companion object {
        private const val CACHE_DIRECTORY = "HttpCache"
        private const val CACHE_SIZE: Long = 20 * 1024 * 1024 // 20 MiB

        private const val TIMEOUT = 10L // in seconds
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .cache(Cache(File(context.cacheDir, CACHE_DIRECTORY), CACHE_SIZE))

        DebugUtils.executeInDebugMode {
            builder.addNetworkInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }

        return builder.build()
    }
}
