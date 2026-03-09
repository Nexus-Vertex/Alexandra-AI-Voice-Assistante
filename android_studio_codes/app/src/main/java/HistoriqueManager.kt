package com.example.myapp

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

object HistoriqueManager {

    private const val KEY_LISTE = "commandes"
    private const val MAX_ITEMS = 50

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ─────────────────────────────────────────────
    // Nom des prefs unique PAR utilisateur (UID Firebase)
    // ─────────────────────────────────────────────
    private fun prefsName(): String {
        val uid = auth.currentUser?.uid ?: "guest"
        return "alexandra_historique_$uid"
    }

    private fun now(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

    // ─────────────────────────────────────────────
    // 🔥 MODIFIÉ — Sauvegarde LOCAL + FIRESTORE
    // ─────────────────────────────────────────────
    fun sauvegarder(context: Context, commande: String) {

        // ✅ 1. Sauvegarde locale (SharedPreferences) — comme avant
        val prefs = context.getSharedPreferences(prefsName(), Context.MODE_PRIVATE)
        val liste = getListe(context).toMutableList()
        liste.add(0, commande)
        val listeLimitee = liste.take(MAX_ITEMS)
        prefs.edit()
            .putString(KEY_LISTE, listeLimitee.joinToString("|||"))
            .apply()

        // ✅ 2. Sauvegarde Firestore — nouvelle ligne vers le dashboard
        val uid = auth.currentUser?.uid ?: return  // pas connecté → on skip Firestore

        val type = detecterType(commande)

        val data = hashMapOf(
            "commande"  to commande,
            "type"      to type,
            "resultat"  to "Succès",
            "date"      to now(),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .collection("historique")
            .add(data)
            .addOnSuccessListener {
                android.util.Log.d("HistoriqueManager", "✅ Firestore : $commande")
            }
            .addOnFailureListener {
                android.util.Log.e("HistoriqueManager", "❌ Firestore : ${it.message}")
            }
    }

    // ─────────────────────────────────────────────
    // 🔥 NOUVEAU — Détecte le type depuis le texte
    // ─────────────────────────────────────────────
    private fun detecterType(commande: String): String {
        val c = commande.lowercase()
        return when {
            c.contains("whatsapp") || c.contains("instagram") ||
                    c.contains("telegram") || c.contains("message") ||
                    c.contains("→")                                    -> "Message"
            c.contains("appel")                                -> "Appel"
            c.contains("alarme")                               -> "Alarme"
            c.contains("rappel")                               -> "Rappel"
            c.contains("timer") || c.contains("minuteur")      -> "Minuteur"
            c.contains("météo") || c.contains("meteo")         -> "Météo"
            c.contains("navigation") || c.contains("maps")     -> "Navigation"
            c.contains("musique") || c.contains("spotify")     -> "Musique"
            c.contains("recherche") || c.contains("google")    -> "Recherche"
            c.contains("wifi")                                  -> "Wi-Fi"
            c.contains("bluetooth")                             -> "Bluetooth"
            c.contains("lampe")                                 -> "Lampe torche"
            c.contains("volume")                                -> "Volume"
            c.contains("batterie")                              -> "Batterie"
            c.contains("notification")                          -> "Notifications"
            c.contains("discussion")                            -> "Discussion"
            c.contains("ouverture") || c.contains("ouvre")     -> "Ouvrir app"
            else                                                -> "Commande vocale"
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Récupérer toutes les commandes (local)
    // ─────────────────────────────────────────────
    fun getListe(context: Context): List<String> {
        val prefs = context.getSharedPreferences(prefsName(), Context.MODE_PRIVATE)
        val data  = prefs.getString(KEY_LISTE, "") ?: ""
        return if (data.isEmpty()) emptyList()
        else data.split("|||").filter { it.isNotBlank() }
    }

    // ─────────────────────────────────────────────
    // ✅ Remplacer toute la liste (local)
    // ─────────────────────────────────────────────
    fun remplacerListe(context: Context, nouvelleListe: List<String>) {
        val prefs = context.getSharedPreferences(prefsName(), Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LISTE,
                if (nouvelleListe.isEmpty()) ""
                else nouvelleListe.joinToString("|||"))
            .apply()
    }

    // ─────────────────────────────────────────────
    // ✅ Effacer l'historique local uniquement
    // ─────────────────────────────────────────────
    fun effacer(context: Context) {
        context.getSharedPreferences(prefsName(), Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    // ─────────────────────────────────────────────
    // 🔥 NOUVEAU — Migration : envoie TOUT l'ancien
    //    historique local vers Firestore en 1 fois
    // ─────────────────────────────────────────────
    fun migrerVersFirestore(context: Context) {
        val uid = auth.currentUser?.uid ?: return
        val liste = getListe(context)
        if (liste.isEmpty()) return

        android.util.Log.d("HistoriqueManager", "🔄 Migration de ${liste.size} entrées...")

        // On envoie chaque entrée locale vers Firestore
        // Les plus récentes sont en tête de liste (index 0)
        liste.forEachIndexed { index, commande ->
            val type = detecterType(commande)
            val data = hashMapOf(
                "commande"  to commande,
                "type"      to type,
                "resultat"  to "Succès",
                "date"      to "Historique local",
                // timestamp décroissant pour garder l'ordre
                "timestamp" to (System.currentTimeMillis() - (index * 1000L))
            )

            db.collection("users")
                .document(uid)
                .collection("historique")
                .add(data)
                .addOnSuccessListener {
                    android.util.Log.d("HistoriqueManager", "✅ Migré : $commande")
                }
                .addOnFailureListener {
                    android.util.Log.e("HistoriqueManager", "❌ Migration échouée : ${it.message}")
                }
        }

        android.util.Log.d("HistoriqueManager", "✅ Migration terminée !")
    }
}