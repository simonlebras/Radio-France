package fr.simonlebras.radiofrance.utils

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.GlideModule
import fr.simonlebras.radiofrance.RadioFranceApplication
import java.io.InputStream

class CustomGlideModule : GlideModule {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888)
    }

    override fun registerComponents(context: Context, glide: Glide) {
        val okHttpClient = (context.applicationContext as RadioFranceApplication).component.okHttpClient()
        glide.register(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(okHttpClient))
    }
}
