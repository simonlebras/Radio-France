package fr.simonlebras.radiofrance.utils

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import fr.simonlebras.radiofrance.R

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.cast_application_id))
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context?) = null
}
