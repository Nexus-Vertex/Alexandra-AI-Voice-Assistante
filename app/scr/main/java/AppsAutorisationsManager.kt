package com.example.myapp

import android.content.Context

/**
 * ✅ Gestionnaire des autorisations d'applications
 * Alexandra vérifie ici avant d'exécuter une commande
 */
object AppsAutorisationsManager {

    private const val PREFS_NAME = "apps_connectees"

    // ✅ Packages connus par action Gemini
    val PACKAGES_PAR_ACTION = mapOf(
        "WHATSAPP"   to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "TELEGRAM"   to listOf("org.telegram.messenger", "org.telegram.messenger.web"),
        "INSTAGRAM"  to listOf("com.instagram.android"),
        "FACEBOOK"   to listOf("com.facebook.katana"),
        "TWITTER"    to listOf("com.twitter.android", "com.x.android"),
        "SNAPCHAT"   to listOf("com.snapchat.android"),
        "TIKTOK"     to listOf("com.zhiliaoapp.musically"),
        "GMAIL"      to listOf("com.google.android.gm"),
        "YOUTUBE"    to listOf("com.google.android.youtube"),
        "SPOTIFY"    to listOf("com.spotify.music"),
        "MAPS"       to listOf("com.google.android.apps.maps"),
        "APPEL"      to listOf("com.android.phone", "com.android.dialer"),
        "SMS"        to listOf("com.android.mms", "com.google.android.apps.messaging")
    )

    // ✅ Noms affichés
    val NOMS_APPS = mapOf(
        "WHATSAPP"   to "WhatsApp",
        "TELEGRAM"   to "Telegram",
        "INSTAGRAM"  to "Instagram",
        "FACEBOOK"   to "Facebook",
        "TWITTER"    to "Twitter / X",
        "SNAPCHAT"   to "Snapchat",
        "TIKTOK"     to "TikTok",
        "GMAIL"      to "Gmail",
        "YOUTUBE"    to "YouTube",
        "SPOTIFY"    to "Spotify",
        "MAPS"       to "Google Maps",
        "APPEL"      to "Téléphone",
        "SMS"        to "SMS"
    )

    // ✅ Vérifier si une ACTION est autorisée
    fun actionEstAutorisee(context: Context, action: String): Boolean {
        val prefs    = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val packages = PACKAGES_PAR_ACTION[action] ?: return true
        return packages.any { prefs.getBoolean(it, false) }
    }

    // ✅ Obtenir le nom de l'app pour affichage
    fun getNomApp(action: String): String = NOMS_APPS[action] ?: action

    // ✅ Vérifier si un package spécifique est autorisé
    fun packageEstAutorise(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(packageName, false)
    }
}