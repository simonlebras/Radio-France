package fr.simonlebras.radiofrance.utils

import android.content.Context
import android.support.annotation.Keep
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import fr.simonlebras.radiofrance.R

@Keep
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.cast_application_id))
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context?) = null
}
