package com.example.myapp

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AlexandraNotificationService : NotificationListenerService() {

    companion object {
        var instance: AlexandraNotificationService? = null
        val notificationsParApp = mutableMapOf<String, MutableList<NotifInfo>>()

        fun isPermissionGranted(context: Context): Boolean {
            val cn = ComponentName(context, AlexandraNotificationService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(cn.flattenToString())
        }

        fun ouvrirParametres(context: Context) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

        fun compterNonLus(packageName: String): Int = notificationsParApp[packageName]?.size ?: 0

        fun compterTousNonLus(): Map<String, Int> =
            notificationsParApp.filter { it.value.isNotEmpty() }.mapValues { it.value.size }

        fun getResume(): String {
            val nonLus = compterTousNonLus()
            if (nonLus.isEmpty()) return "Vous n'avez aucune notification non lue."
            val sb = StringBuilder("Voici vos notifications : ")
            nonLus.forEach { (pkg, count) ->
                val nom = getNomApp(pkg)
                sb.append("$nom : $count message${if (count > 1) "s" else ""} non lu${if (count > 1) "s" else ""}. ")
            }
            return sb.toString()
        }

        fun getNotificationsApp(packageName: String): List<NotifInfo> =
            notificationsParApp[packageName] ?: emptyList()

        fun getNomApp(packageName: String): String = when {
            packageName.contains("whatsapp")  -> "WhatsApp"
            packageName.contains("telegram")  -> "Telegram"
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("facebook") && !packageName.contains("orca") -> "Facebook"
            packageName.contains("snapchat")  -> "Snapchat"
            packageName.contains("twitter") || packageName.contains(".x.") -> "Twitter"
            packageName.contains("gmail")     -> "Gmail"
            packageName.contains("youtube")   -> "YouTube"
            packageName.contains("spotify")   -> "Spotify"
            packageName.contains("tiktok") || packageName.contains("musically") -> "TikTok"
            packageName.contains("linkedin")  -> "LinkedIn"
            packageName.contains("discord")   -> "Discord"
            packageName.contains("slack")     -> "Slack"
            packageName.contains("teams")     -> "Teams"
            packageName.contains("zoom")      -> "Zoom"
            packageName.contains("viber")     -> "Viber"
            packageName.contains("skype")     -> "Skype"
            packageName.contains("orca")      -> "Messenger"
            packageName.contains("messaging") || packageName.contains("mms") -> "SMS"
            else -> packageName.substringAfterLast(".")
        }

        fun trouverPackage(nomApp: String): String? {
            val nom = nomApp.lowercase()
            return when {
                nom.contains("whatsapp")  -> "com.whatsapp"
                nom.contains("telegram")  -> "org.telegram.messenger"
                nom.contains("instagram") -> "com.instagram.android"
                nom.contains("facebook")  -> "com.facebook.katana"
                nom.contains("snap")      -> "com.snapchat.android"
                nom.contains("twitter") || nom.contains(" x ") -> "com.twitter.android"
                nom.contains("gmail")     -> "com.google.android.gm"
                nom.contains("youtube")   -> "com.google.android.youtube"
                nom.contains("spotify")   -> "com.spotify.music"
                nom.contains("tiktok")    -> "com.zhiliaoapp.musically"
                nom.contains("linkedin")  -> "com.linkedin.android"
                nom.contains("discord")   -> "com.discord"
                nom.contains("slack")     -> "com.slack"
                nom.contains("teams")     -> "com.microsoft.teams"
                nom.contains("zoom")      -> "us.zoom.videomeetings"
                nom.contains("viber")     -> "com.viber.voip"
                nom.contains("skype")     -> "com.skype.raider"
                nom.contains("messenger") -> "com.facebook.orca"
                else -> null
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d("NOTIF", "Service connecte")
        chargerNotificationsExistantes()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == "android" || pkg == "com.android.systemui" || pkg == "com.example.myapp") return

        val extras = sbn.notification?.extras
        val titre  = extras?.getString(Notification.EXTRA_TITLE) ?: ""
        val texte  = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (titre.isEmpty() && texte.isEmpty()) return

        val notif = NotifInfo(
            packageName = pkg,
            nomApp      = getNomApp(pkg),
            titre       = titre,
            texte       = texte,
            timestamp   = sbn.postTime,
            id          = sbn.id
        )

        notificationsParApp.getOrPut(pkg) { mutableListOf() }.also { list ->
            if (list.none { it.titre == titre && it.texte == texte }) list.add(notif)
        }
        Log.d("NOTIF", "Recu [${getNomApp(pkg)}] $titre: $texte")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        notificationsParApp[pkg]?.removeAll { it.id == sbn.id }
        if (notificationsParApp[pkg]?.isEmpty() == true) notificationsParApp.remove(pkg)
    }

    private fun chargerNotificationsExistantes() {
        try {
            activeNotifications?.forEach { onNotificationPosted(it) }
        } catch (e: Exception) {
            Log.e("NOTIF", "Erreur: ${e.message}")
        }
    }
}

data class NotifInfo(
    val packageName : String,
    val nomApp      : String,
    val titre       : String,
    val texte       : String,
    val timestamp   : Long,
    val id          : Int
)