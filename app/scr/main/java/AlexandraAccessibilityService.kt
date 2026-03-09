package com.example.myapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AlexandraAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AlexandraAccessibilityService? = null
        private val handler = Handler(Looper.getMainLooper())

        var actionEnAttente: ActionApp? = null
        var onActionTerminee: ((String) -> Unit)? = null

        fun executerAction(action: ActionApp, callback: (String) -> Unit) {
            actionEnAttente  = action
            onActionTerminee = callback
            Log.d("ACCESS", "Action programmee: ${action.type} sur ${action.packageName}")
        }
    }

    private var packageActuel = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo?.also { info ->
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            info.notificationTimeout = 100
            info.packageNames = null
        }
        Log.d("ACCESS", "Service connecte - toutes apps activees")
    }

    override fun onInterrupt() { Log.d("ACCESS", "Service interrompu") }
    override fun onDestroy() { super.onDestroy(); instance = null }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.example.myapp") return
        packageActuel = pkg
        val action = actionEnAttente ?: return

        if (pkg == action.packageName &&
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d("ACCESS", "App detectee: $pkg - Execution: ${action.type}")
            handler.postDelayed({ executerActionSurApp(action) }, 1000)
        }
    }

    private fun executerActionSurApp(action: ActionApp) {
        val rootNode = rootInActiveWindow ?: run {
            Log.e("ACCESS", "Pas de noeud racine")
            onActionTerminee?.invoke("Impossible d'acceder a l'application.")
            actionEnAttente = null
            return
        }

        when (action.type) {

            TypeAction.LIRE_NON_LUS -> {
                val nonLus = AlexandraNotificationService.compterNonLus(action.packageName)
                val nomApp = AlexandraNotificationService.getNomApp(action.packageName)
                actionEnAttente = null
                onActionTerminee?.invoke("Vous avez $nonLus message${if (nonLus > 1) "s" else ""} non lu${if (nonLus > 1) "s" else ""} sur $nomApp.")
            }

            // 🔥 MODIFIÉ — Envoyer message + sauvegarde Firestore contacts
            TypeAction.ENVOYER_MESSAGE -> {
                envoyerMessageAvecDelai(action)
            }

            TypeAction.OUVRIR_DISCUSSION -> {
                val succes = rechercherContactDansApp(rootNode, action.contact)
                if (!succes) {
                    actionEnAttente = null
                    onActionTerminee?.invoke("Je n'ai pas trouve ${action.contact} dans l'application.")
                }
            }

            TypeAction.LIRE_CONTENU -> {
                val contenu = lireContenuEcran(rootNode)
                actionEnAttente = null
                onActionTerminee?.invoke(contenu)
            }

            TypeAction.CLIQUER_ELEMENT -> {
                val succes = cliqueSurTexte(rootNode, action.texteElement)
                actionEnAttente = null
                onActionTerminee?.invoke(
                    if (succes) "J'ai clique sur ${action.texteElement}."
                    else "Je n'ai pas trouve cet element."
                )
            }

            TypeAction.ECRIRE_TEXTE -> {
                val succes = ecrireDansChamp(rootNode, action.message)
                actionEnAttente = null
                onActionTerminee?.invoke(
                    if (succes) "J'ai ecrit le texte."
                    else "Je n'ai pas trouve de champ texte."
                )
            }

            else -> {
                actionEnAttente = null
                onActionTerminee?.invoke("Action non reconnue.")
            }
        }

        rootNode.recycle()
    }

    // 🔥 MODIFIÉ — Envoyer message avec sauvegarde Firestore à la fin
    private fun envoyerMessageAvecDelai(action: ActionApp) {

        // Étape 1 : trouver et remplir le champ texte
        handler.postDelayed({
            val root = rootInActiveWindow ?: run {
                actionEnAttente = null
                onActionTerminee?.invoke("Impossible d'acceder au champ de message.")
                return@postDelayed
            }

            val champ = root.findChamps(listOf(
                "android.widget.EditText",
                "message", "input", "compose", "text_input", "msg_input", "entry"
            )).firstOrNull()

            if (champ == null) {
                Log.e("ACCESS", "❌ Champ texte non trouvé")
                actionEnAttente = null
                onActionTerminee?.invoke("Je n'ai pas trouve le champ de message.")
                root.recycle()
                return@postDelayed
            }

            champ.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            // Étape 2 : écrire le message après 400ms
            handler.postDelayed({
                val root2 = rootInActiveWindow ?: return@postDelayed
                val champ2 = root2.findChamps(listOf(
                    "android.widget.EditText",
                    "message", "input", "compose", "text_input", "msg_input", "entry"
                )).firstOrNull()

                if (champ2 != null) {
                    val args = Bundle()
                    args.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        action.message
                    )
                    champ2.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    Log.d("ACCESS", "✅ Message écrit : ${action.message}")
                }
                root2.recycle()

                // Étape 3 : cliquer sur Envoyer après 600ms
                handler.postDelayed({
                    val root3 = rootInActiveWindow ?: return@postDelayed
                    val bouton = root3.trouverBoutonEnvoyer()

                    if (bouton != null) {
                        bouton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d("ACCESS", "✅ Bouton Envoyer cliqué")

                        // 🔥 SAUVEGARDE FIRESTORE — Contact + message envoyé
                        val nomApp = getNomAppDepuisPackage(action.packageName)
                        FirestoreManager.sauvegarderContact(
                            contact = action.contact,
                            message = action.message,
                            app     = nomApp
                        )

                        actionEnAttente = null
                        onActionTerminee?.invoke("Message envoyé à ${action.contact}.")
                    } else {
                        Log.w("ACCESS", "⚠️ Bouton Envoyer non trouvé → essai touche Entrée")
                        val root4 = rootInActiveWindow
                        val champ3 = root4?.findChamps(listOf("android.widget.EditText"))?.firstOrNull()
                        champ3?.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)

                        // 🔥 SAUVEGARDE FIRESTORE — même en fallback
                        val nomApp = getNomAppDepuisPackage(action.packageName)
                        FirestoreManager.sauvegarderContact(
                            contact = action.contact,
                            message = action.message,
                            app     = nomApp
                        )

                        actionEnAttente = null
                        onActionTerminee?.invoke("Message envoyé à ${action.contact}.")
                        root4?.recycle()
                    }
                    root3.recycle()
                }, 800)

            }, 600)

        }, 800)
    }

    // ─────────────────────────────────────────────
    // 🔥 NOUVEAU — Convertit packageName → nom lisible
    // ─────────────────────────────────────────────
    private fun getNomAppDepuisPackage(packageName: String): String {
        return when {
            packageName.contains("whatsapp")   -> "WhatsApp"
            packageName.contains("instagram")  -> "Instagram"
            packageName.contains("telegram")   -> "Telegram"
            packageName.contains("messenger")  -> "Messenger"
            packageName.contains("snapchat")   -> "Snapchat"
            packageName.contains("twitter") ||
                    packageName.contains("x.android")  -> "X (Twitter)"
            packageName.contains("tiktok")     -> "TikTok"
            packageName.contains("facebook")   -> "Facebook"
            packageName.contains("gmail")      -> "Gmail"
            packageName.contains("sms") ||
                    packageName.contains("mms") ||
                    packageName.contains("messaging")  -> "SMS"
            else -> packageName.substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
        }
    }

    private fun rechercherContactDansApp(root: AccessibilityNodeInfo, contact: String): Boolean {
        val recherche = root.findChamps(listOf(
            "search", "recherche", "find", "query", "nom"
        )).firstOrNull() ?: return false

        recherche.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        handler.postDelayed({
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contact)
            recherche.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }, 300)
        return true
    }

    private fun lireContenuEcran(root: AccessibilityNodeInfo): String {
        val textes = mutableListOf<String>()
        lireNoeudsRecursivement(root, textes)
        if (textes.isEmpty()) return "Je ne vois pas de contenu sur cet ecran."
        return "Voici ce que je vois : " + textes.take(5).joinToString(". ")
    }

    private fun lireNoeudsRecursivement(node: AccessibilityNodeInfo?, textes: MutableList<String>) {
        node ?: return
        val texte = node.text?.toString()?.trim()
        if (!texte.isNullOrEmpty() && texte.length > 2) textes.add(texte)
        for (i in 0 until node.childCount) lireNoeudsRecursivement(node.getChild(i), textes)
    }

    private fun cliqueSurTexte(root: AccessibilityNodeInfo, texte: String): Boolean {
        val noeuds = root.findAccessibilityNodeInfosByText(texte)
        if (noeuds.isNullOrEmpty()) return false
        noeuds.first().performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return true
    }

    private fun ecrireDansChamp(root: AccessibilityNodeInfo, texte: String): Boolean {
        val champ = root.findChamps(listOf("android.widget.EditText")).firstOrNull() ?: return false
        champ.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, texte)
        champ.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return true
    }

    private fun AccessibilityNodeInfo.findChamps(mots: List<String>): List<AccessibilityNodeInfo> {
        val resultats = mutableListOf<AccessibilityNodeInfo>()
        findChampsRecursif(this, mots, resultats)
        return resultats
    }

    private fun findChampsRecursif(
        node: AccessibilityNodeInfo?,
        mots: List<String>,
        resultats: MutableList<AccessibilityNodeInfo>
    ) {
        node ?: return
        val className = node.className?.toString()?.lowercase() ?: ""
        val viewId    = node.viewIdResourceName?.lowercase() ?: ""
        val hint      = node.hintText?.toString()?.lowercase() ?: ""

        if (mots.any { mot -> className.contains(mot) || viewId.contains(mot) || hint.contains(mot) }) {
            resultats.add(node)
        }
        for (i in 0 until node.childCount) findChampsRecursif(node.getChild(i), mots, resultats)
    }

    private fun AccessibilityNodeInfo.trouverBoutonEnvoyer(): AccessibilityNodeInfo? {
        val motsCles = listOf("send", "envoyer", "envoie", "submit", "go", "done")
        val noeuds = mutableListOf<AccessibilityNodeInfo>()
        findChampsRecursif(this, motsCles + listOf("imagebutton"), noeuds)
        return noeuds.firstOrNull { it.isClickable }
    }
}

enum class TypeAction {
    LIRE_NON_LUS,
    ENVOYER_MESSAGE,
    OUVRIR_DISCUSSION,
    LIRE_CONTENU,
    CLIQUER_ELEMENT,
    ECRIRE_TEXTE
}

data class ActionApp(
    val type         : TypeAction,
    val packageName  : String,
    val contact      : String  = "",
    val message      : String  = "",
    val texteElement : String  = ""
)