package com.simonlebras.radiofrance.utils

import android.content.Context
import android.support.annotation.Keep
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import okhttp3.OkHttpClient
import java.io.InputStream

@Keep
@GlideModule
class CustomGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val okHttpClient = OkHttpClient.Builder()
                .build()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }

    override fun isManifestParsingEnabled() = false
}
