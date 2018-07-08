package com.simonlebras.radiofrance.ui

import android.app.PendingIntent
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import com.simonlebras.radiofrance.R
import com.simonlebras.radiofrance.ui.browser.list.RadioListFragment
import dagger.android.support.DaggerAppCompatActivity

class MainActivity : DaggerAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, RadioListFragment.newInstance())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()

        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 100
        private const val REQUEST_CODE_SESSION = 101

        fun createNotificationIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            return PendingIntent.getActivity(
                context,
                REQUEST_CODE_NOTIFICATION,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        }

        fun createSessionIntent(context: Context): PendingIntent {
            return PendingIntent.getActivity(
                context,
                REQUEST_CODE_SESSION,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
