package com.example.myapp

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppAutoSender {

    fun envoyerViaAccessibility(
        context: Context,
        contact: String,
        message: String,
        onResultat: (String) -> Unit
    ) {
        val service = AlexandraAccessibilityService.instance
        if (service != null) {
            // Ouvrir WhatsApp sur le contact
            val pkg = "com.whatsapp"
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                AlexandraAccessibilityService.executerAction(
                    ActionApp(TypeAction.ENVOYER_MESSAGE, pkg, contact, message)
                ) { resultat ->
                    onResultat(resultat)
                }
            } else {
                onResultat("WhatsApp n'est pas installé.")
            }
        } else {
            // Fallback : intent WhatsApp direct avec numéro
            try {
                val numero = contact.replace(" ", "").replace("-", "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$numero&text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                onResultat("Message préparé dans WhatsApp.")
            } catch (e: Exception) {
                onResultat("Impossible d'ouvrir WhatsApp.")
            }
        }
    }
}