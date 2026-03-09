package com.example.myapp

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class NotificationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()

        setContentView(R.layout.activity_notifications)

        val prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE)

        val switchGenerales  = findViewById<SwitchMaterial>(R.id.switchGenerales)
        val switchEmail      = findViewById<SwitchMaterial>(R.id.switchEmail)
        val switchSecurity   = findViewById<SwitchMaterial>(R.id.switchSecurity)

        // Charger les préférences sauvegardées
        switchGenerales.isChecked = prefs.getBoolean("notif_generales", true)
        switchEmail.isChecked     = prefs.getBoolean("notif_email", true)
        switchSecurity.isChecked  = prefs.getBoolean("notif_securite", true)

        // Sauvegarder les changements
        switchGenerales.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("notif_generales", checked).apply()
        }
        switchEmail.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("notif_email", checked).apply()
        }
        switchSecurity.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("notif_securite", checked).apply()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}