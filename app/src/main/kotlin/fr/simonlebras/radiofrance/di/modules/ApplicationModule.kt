package fr.simonlebras.radiofrance.di.modules

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
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
class ApplicationModule(private val context: Context) {
    private companion object {
        private const val CACHE_DIRECTORY = "HttpCache"
        private const val CACHE_SIZE: Long = 20 * 1024 * 1024 // 20 MiB

        private const val TIMEOUT = 10L // in seconds
    }

    @Provides
    @Singleton
    fun provideContext() = context

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun provideCache(context: Context): Cache {
        val cacheDirectory = File(context.cacheDir, CACHE_DIRECTORY)
        return Cache(cacheDirectory, CACHE_SIZE)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(cache: Cache): OkHttpClient {
        val builder = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .cache(cache)

        DebugUtils.executeInDebugMode {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }
}
