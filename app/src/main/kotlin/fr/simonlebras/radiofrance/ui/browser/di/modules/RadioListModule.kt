package fr.simonlebras.radiofrance.ui.browser.di.modules

import android.graphics.Bitmap
import android.support.v4.content.ContextCompat
import com.bumptech.glide.BitmapRequestBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import dagger.Module
import dagger.Provides
import fr.simonlebras.radiofrance.R
import fr.simonlebras.radiofrance.ui.browser.list.RadioListFragment

@Module
class RadioListModule {
    @Provides
    fun provideGlideRequest(fragment: RadioListFragment): BitmapRequestBuilder<String, Bitmap> {
        return Glide.with(fragment)
                .from(String::class.java)
                .asBitmap()
                .placeholder(ContextCompat.getDrawable(fragment.context, R.drawable.ic_radio_blue_40dp))
                .error(ContextCompat.getDrawable(fragment.context, R.drawable.ic_radio_blue_40dp))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
    }
}
