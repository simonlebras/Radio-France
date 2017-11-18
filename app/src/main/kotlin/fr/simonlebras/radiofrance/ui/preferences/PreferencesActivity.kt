package fr.simonlebras.radiofrance.ui.preferences

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import fr.simonlebras.radiofrance.R
import kotlinx.android.synthetic.main.partial_toolbar.*

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_preferences)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }
}
