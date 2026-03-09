package com.example.myapp

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * ─────────────────────────────────────────────────────────
 *  FirestoreManager.kt
 *  Synchronise automatiquement vers Firebase Firestore :
 *    • users/{uid}/historique   → commandes vocales
 *    • users/{uid}/discussions  → discussions libres
 *    • users/{uid}/contacts     → messages envoyés
 * ─────────────────────────────────────────────────────────
 */
object FirestoreManager {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun uid(): String? = auth.currentUser?.uid

    private fun now(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

    // ─────────────────────────────────────────────
    // 📋 HISTORIQUE — Commandes vocales exécutées
    // ─────────────────────────────────────────────
    fun sauvegarderHistorique(
        commande     : String,
        type         : String,    // ex: "Commande vocale", "Alarme", "Navigation" …
        resultat     : String     // ex: "Succès", "Échec"
    ) {
        val uid = uid() ?: run { Log.w("Firestore", "Utilisateur non connecté"); return }

        val data = hashMapOf(
            "commande"   to commande,
            "type"       to type,
            "resultat"   to resultat,
            "date"       to now(),
            "timestamp"  to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .collection("historique")
            .add(data)
            .addOnSuccessListener { Log.d("Firestore", "✅ Historique : $commande") }
            .addOnFailureListener { Log.e("Firestore", "❌ Historique : ${it.message}") }
    }

    // ─────────────────────────────────────────────
    // 💬 DISCUSSIONS — Échanges libres Alexandra
    // ─────────────────────────────────────────────
    fun sauvegarderDiscussion(
        question : String,
        reponse  : String
    ) {
        val uid = uid() ?: run { Log.w("Firestore", "Utilisateur non connecté"); return }

        val data = hashMapOf(
            "question"   to question,
            "reponse"    to reponse,
            "date"       to now(),
            "timestamp"  to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .collection("discussions")
            .add(data)
            .addOnSuccessListener { Log.d("Firestore", "✅ Discussion sauvegardée") }
            .addOnFailureListener { Log.e("Firestore", "❌ Discussion : ${it.message}") }
    }

    // ─────────────────────────────────────────────
    // 👤 CONTACTS — Messages envoyés via Alexandra
    // ─────────────────────────────────────────────
    fun sauvegarderContact(
        contact : String,
        message : String,
        app     : String   // ex: "WhatsApp", "Instagram" …
    ) {
        val uid = uid() ?: run { Log.w("Firestore", "Utilisateur non connecté"); return }

        val data = hashMapOf(
            "contact"    to contact,
            "message"    to message,
            "app"        to app,
            "date"       to now(),
            "timestamp"  to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .collection("contacts")
            .add(data)
            .addOnSuccessListener { Log.d("Firestore", "✅ Contact : $contact → $message") }
            .addOnFailureListener { Log.e("Firestore", "❌ Contact : ${it.message}") }
    }
}