package fr.simonlebras.radiofrance.ui.preferences

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife
import fr.simonlebras.radiofrance.R

class PreferencesActivity : AppCompatActivity() {
    @BindView(R.id.toolbar) lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_preferences)

        ButterKnife.bind(this)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }
}
