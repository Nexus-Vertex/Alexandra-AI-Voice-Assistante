package com.example.myapp

import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * OnlineManager — Gère le statut en ligne/hors ligne dans Firestore
 *
 * UTILISATION dans chaque Activity :
 *
 *   private val onlineManager = OnlineManager()
 *
 *   override fun onResume()  { super.onResume();  onlineManager.startTracking() }
 *   override fun onPause()   { super.onPause();   onlineManager.stopTracking()  }
 *   override fun onDestroy() { super.onDestroy(); onlineManager.setOffline()    }
 */
class OnlineManager {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // Met à jour lastSeen toutes les 60 secondes
    private val INTERVAL_MS = 60_000L

    private val handler = Handler(Looper.getMainLooper())

    private val pingRunnable = object : Runnable {
        override fun run() {
            updateLastSeen()
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    // ── Démarre le tracking (appeler dans onResume) ────────────────────────────
    fun startTracking() {
        updateLastSeen()                          // Ping immédiat
        handler.postDelayed(pingRunnable, INTERVAL_MS)
    }

    // ── Arrête le tracking (appeler dans onPause) ──────────────────────────────
    fun stopTracking() {
        handler.removeCallbacks(pingRunnable)
        setOffline()                              // Marque hors ligne immédiatement
    }

    // ── Met à jour lastSeen = maintenant ──────────────────────────────────────
    fun updateLastSeen() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .set(
                mapOf("lastSeen" to System.currentTimeMillis()),
                SetOptions.merge()
            )
            .addOnFailureListener { e ->
                android.util.Log.e("OnlineManager", "updateLastSeen failed: ${e.message}")
            }
    }

    // ── Marque l'utilisateur hors ligne ──────────────────────────────────────
    fun setOffline() {
        val uid = auth.currentUser?.uid ?: return
        // On met lastSeen = il y a 10 minutes pour qu'il soit considéré hors ligne
        val offlineTs = System.currentTimeMillis() - (10 * 60 * 1000L)
        db.collection("users").document(uid)
            .set(
                mapOf("lastSeen" to offlineTs),
                SetOptions.merge()
            )
            .addOnFailureListener { e ->
                android.util.Log.e("OnlineManager", "setOffline failed: ${e.message}")
            }
    }
}