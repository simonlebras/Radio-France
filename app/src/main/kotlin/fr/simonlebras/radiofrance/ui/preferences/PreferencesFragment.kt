package fr.simonlebras.radiofrance.ui.preferences

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import fr.simonlebras.radiofrance.R

class PreferencesFragment : PreferenceFragmentCompat() {
    companion object {
        const val PREFERENCE_KEY_LIST_TYPE = "preference_key_list_type"

        const val PREFERENCE_VALUE_LIST_TYPE_GRID = "0"
        const val PREFERENCE_VALUE_LIST_TYPE_LIST = "1"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}
