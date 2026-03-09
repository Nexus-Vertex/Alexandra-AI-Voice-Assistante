package com.example.myapp

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.io.File

object BottomNavHelper {

    fun setup(activity: Activity, currentItemId: Int) {

        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener(null)
        bottomNav.menu.findItem(currentItemId)?.isChecked = true

        bottomNav.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bottomNav.viewTreeObserver.removeOnGlobalLayoutListener(this)
                chargerPhotoAvatar(activity)
            }
        })

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == currentItemId) return@setOnItemSelectedListener true

            val intent = when (item.itemId) {
                R.id.nav_home          -> Intent(activity, MainActivity::class.java)
                R.id.nav_historique    -> Intent(activity, HistoriqueActivity::class.java)
                R.id.nav_autorisations -> Intent(activity, AutorisationsActivity::class.java)
                R.id.nav_profile       -> Intent(activity, ProfilActivity::class.java)
                else -> null
            } ?: return@setOnItemSelectedListener false

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            activity.startActivity(intent)
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            true
        }
    }

    fun chargerPhotoAvatar(activity: Activity) {
        try {
            val imgNavAvatar = activity.findViewById<ImageView>(R.id.imgNavAvatar)
                ?: return

            val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNav)

            val uid        = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
            val avatarFile = File(activity.filesDir, "avatar_profil_$uid.jpg")

            if (!avatarFile.exists() || avatarFile.length() < 100) {
                // ✅ Pas de photo → icône par défaut visible
                imgNavAvatar.visibility = android.view.View.GONE
                bottomNav?.menu?.findItem(R.id.nav_profile)?.icon =
                    androidx.core.content.ContextCompat.getDrawable(activity, R.drawable.ic_person)
                return
            }

            // ✅ Charger le bitmap en taille correcte
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            val bitmap  = BitmapFactory.decodeFile(avatarFile.absolutePath, options)
                ?: return

            // ✅ Rogner en cercle parfait
            val size   = 96
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

            // 1. Dessiner le cercle
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

            // 2. Appliquer la photo dans le cercle
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            paint.xfermode = null

            // ✅ Cacher l'icône menu et afficher la photo
            bottomNav?.menu?.findItem(R.id.nav_profile)?.icon = null
            imgNavAvatar.setImageBitmap(output)
            imgNavAvatar.background = null
            imgNavAvatar.visibility = android.view.View.VISIBLE

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}