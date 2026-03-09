package com.example.myapp

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * AdminActionsManager.kt
 * ─────────────────────────────────────────────────────────
 * Écoute en temps réel les signaux envoyés par l'admin
 * depuis le dashboard et efface les données locales.
 *
 * Signaux écoutés dans Firestore → users/{uid}/admin_actions :
 *   clear_historique   → efface SharedPreferences historique
 *   clear_discussions  → efface SharedPreferences discussions
 *   clear_contacts     → efface SharedPreferences contacts
 * ─────────────────────────────────────────────────────────
 */
object AdminActionsManager {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    // ─────────────────────────────────────────────
    // Démarrer l'écoute (appeler dans MainActivity.onCreate)
    // ─────────────────────────────────────────────
    fun startListening(context: Context) {
        val uid = auth.currentUser?.uid ?: return

        listenerRegistration = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminActions", "Erreur écoute : ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val actions = snapshot.get("admin_actions") as? Map<*, *> ?: return@addSnapshotListener

                // 🗑️ Effacer historique
                if (actions["clear_historique"] == true) {
                    Log.d("AdminActions", "🗑️ Admin a effacé l'historique")
                    HistoriqueManager.effacer(context)
                    // Réinitialiser le signal
                    db.collection("users").document(uid)
                        .update("admin_actions.clear_historique", false)
                }

                // 🗑️ Effacer discussions
                if (actions["clear_discussions"] == true) {
                    Log.d("AdminActions", "🗑️ Admin a effacé les discussions")
                    // Effacer SharedPreferences discussions si vous en avez
                    context.getSharedPreferences(
                        "alexandra_discussions_$uid", Context.MODE_PRIVATE
                    ).edit().clear().apply()
                    db.collection("users").document(uid)
                        .update("admin_actions.clear_discussions", false)
                }

                // 🗑️ Effacer contacts
                if (actions["clear_contacts"] == true) {
                    Log.d("AdminActions", "🗑️ Admin a effacé les contacts")
                    context.getSharedPreferences(
                        "alexandra_contacts_$uid", Context.MODE_PRIVATE
                    ).edit().clear().apply()
                    db.collection("users").document(uid)
                        .update("admin_actions.clear_contacts", false)
                }
            }

        Log.d("AdminActions", "✅ Écoute des actions admin démarrée")
    }

    // ─────────────────────────────────────────────
    // Arrêter l'écoute (appeler dans onDestroy)
    // ─────────────────────────────────────────────
    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d("AdminActions", "🛑 Écoute des actions admin arrêtée")
    }
}