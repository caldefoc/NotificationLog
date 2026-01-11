package com.notificationlog

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This line links your Kotlin code to your XML layout file.
        setContentView(R.layout.activity_main)

        // Find the button in the layout by its ID.
        val enableButton: Button = findViewById(R.id.enablePermissionButton)

        // Set a listener that executes code when the button is clicked.
        enableButton.setOnClickListener {
            // This special Intent action tells Android to open the notification listener settings screen.
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}

