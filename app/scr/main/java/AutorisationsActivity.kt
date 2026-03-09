package com.example.myapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class AutorisationsActivity : AppCompatActivity() {

    companion object {
        const val REQ_AUDIO    = 101
        const val REQ_CONTACTS = 102
        const val REQ_NOTIF    = 103
        const val REQ_SMS      = 104
        const val REQ_PHONE    = 105
    }

    private val requestingMap = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()
        setContentView(R.layout.activity_autorisations)

        setupSwitch(R.id.switchMicro,    Manifest.permission.RECORD_AUDIO,       REQ_AUDIO)
        setupSwitch(R.id.switchContacts, Manifest.permission.READ_CONTACTS,       REQ_CONTACTS)
        setupSwitch(R.id.switchSms,      Manifest.permission.SEND_SMS,            REQ_SMS)
        setupSwitch(R.id.switchAppel,    Manifest.permission.CALL_PHONE,          REQ_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            setupSwitch(R.id.switchNotif, Manifest.permission.POST_NOTIFICATIONS, REQ_NOTIF)

        // ✅ Bouton Applications connectées
        findViewById<CardView>(R.id.cardAppsConnectees).setOnClickListener {
            startActivity(Intent(this, AppsConnecteesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        BottomNavHelper.setup(this, R.id.nav_autorisations)
        refreshAllSwitches()
    }

    // ✅ Ouvre directement la page Autorisations
    private fun ouvrirAutorisations() {
        try {
            val intent = Intent("android.settings.APP_PERMISSION_SETTINGS")
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }
    }

    private fun setupSwitch(switchId: Int, permission: String, requestCode: Int) {
        val switch = findViewById<SwitchMaterial>(switchId)
        requestingMap[requestCode] = false

        switch.setOnCheckedChangeListener(null)
        switch.isChecked = ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED

        switch.setOnCheckedChangeListener { _, isChecked ->
            val isRequesting = requestingMap[requestCode] ?: false

            if (isChecked && !isRequesting) {
                switch.isChecked = false

                val estAccordee = ContextCompat.checkSelfPermission(this, permission) ==
                        PackageManager.PERMISSION_GRANTED

                if (estAccordee) {
                    switch.isChecked = true
                    return@setOnCheckedChangeListener
                }

                // ✅ SharedPreferences pour savoir si déjà demandée
                val prefs = getSharedPreferences("permissions_prefs", MODE_PRIVATE)
                val dejaDemandee = prefs.getBoolean(permission, false)
                val peutDemander = ActivityCompat.shouldShowRequestPermissionRationale(
                    this, permission
                )

                if (!dejaDemandee || peutDemander) {
                    // ✅ Première fois → dialog Android normal
                    prefs.edit().putBoolean(permission, true).apply()
                    requestingMap[requestCode] = true
                    ActivityCompat.requestPermissions(
                        this, arrayOf(permission), requestCode
                    )
                } else {
                    // ✅ Bloquée définitivement → ouvrir paramètres
                    AlertDialog.Builder(this)
                        .setTitle("Activer la permission")
                        .setMessage("Cette permission a été refusée. Veuillez l'activer manuellement dans les Autorisations de l'application.")
                        .setPositiveButton("Ouvrir Autorisations") { _, _ ->
                            ouvrirAutorisations()
                        }
                        .setNegativeButton("Annuler", null)
                        .show()
                }

            } else if (!isChecked && !isRequesting) {
                // ✅ DÉSACTIVER → ouvrir paramètres
                switch.isChecked = true
                AlertDialog.Builder(this)
                    .setTitle("Désactiver la permission")
                    .setMessage("Pour désactiver cette permission, veuillez aller dans les Autorisations de l'application.")
                    .setPositiveButton("Ouvrir Autorisations") { _, _ ->
                        ouvrirAutorisations()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }
    }

    // ✅ Rafraîchir tous les switches après retour des Paramètres
    private fun refreshAllSwitches() {
        val permissions = mutableMapOf(
            R.id.switchMicro    to Pair(Manifest.permission.RECORD_AUDIO,    REQ_AUDIO),
            R.id.switchContacts to Pair(Manifest.permission.READ_CONTACTS,   REQ_CONTACTS),
            R.id.switchSms      to Pair(Manifest.permission.SEND_SMS,        REQ_SMS),
            R.id.switchAppel    to Pair(Manifest.permission.CALL_PHONE,      REQ_PHONE)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[R.id.switchNotif] =
                Pair(Manifest.permission.POST_NOTIFICATIONS, REQ_NOTIF)
        }
        for ((switchId, pair) in permissions) {
            setupSwitch(switchId, pair.first, pair.second)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        requestingMap[requestCode] = false

        val switchId = when (requestCode) {
            REQ_AUDIO    -> R.id.switchMicro
            REQ_CONTACTS -> R.id.switchContacts
            REQ_SMS      -> R.id.switchSms
            REQ_PHONE    -> R.id.switchAppel
            REQ_NOTIF    -> R.id.switchNotif
            else         -> return
        }

        val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

        val switch = findViewById<SwitchMaterial>(switchId)
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = granted
        setupSwitch(switchId, permissions[0], requestCode)

        if (granted) {
            Toast.makeText(this, "Permission accordée ✓", Toast.LENGTH_SHORT).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Permission refusée")
                .setMessage("Veuillez activer cette permission manuellement dans les Autorisations de l'application.")
                .setPositiveButton("Ouvrir Autorisations") { _, _ ->
                    ouvrirAutorisations()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }
}