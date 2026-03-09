package com.example.myapp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class AppsConnecteesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()
        setContentView(R.layout.activity_apps_connectees)

        val layoutApps = findViewById<LinearLayout>(R.id.layoutApps)
        val prefs = getSharedPreferences("apps_connectees", Context.MODE_PRIVATE)

        // ✅ Récupérer toutes les apps installées
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null } // ✅ Apps avec icône de lancement seulement
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() } // ✅ Trier par nom

        layoutApps.removeAllViews()

        for (app in apps) {
            val appName    = pm.getApplicationLabel(app).toString()
            val appPackage = app.packageName
            val appIcon    = pm.getApplicationIcon(app.packageName)

            // ✅ Inflater le layout item_app
            val itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_app, layoutApps, false)

            val imgIcon    = itemView.findViewById<ImageView>(R.id.imgAppIcon)
            val tvName     = itemView.findViewById<TextView>(R.id.tvAppName)
            val tvPackage  = itemView.findViewById<TextView>(R.id.tvAppPackage)
            val switchApp  = itemView.findViewById<SwitchMaterial>(R.id.switchApp)

            imgIcon.setImageDrawable(appIcon)
            tvName.text    = appName
            tvPackage.text = appPackage

            // ✅ Charger l'état sauvegardé
            switchApp.isChecked = prefs.getBoolean(appPackage, false)

            // ✅ Sauvegarder quand l'utilisateur change
            switchApp.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(appPackage, isChecked).apply()
            }

            layoutApps.addView(itemView)
        }
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        BottomNavHelper.setup(this, R.id.nav_autorisations)
    }
}