package com.example.myapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class EditNomEmailActivity : AppCompatActivity() {

    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()

        setContentView(R.layout.activity_edit_nom_email)

        val etNom          = findViewById<EditText>(R.id.etNom)
        val etEmail        = findViewById<EditText>(R.id.etEmail)
        val btnSauvegarder = findViewById<Button>(R.id.btnSauvegarder)

        // ✅ Pré-remplir depuis Firestore
        val user = auth.currentUser
        etEmail.setText(user?.email ?: "")

        if (user != null) {
            firestore.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    etNom.setText(doc.getString("name") ?: user.displayName ?: "")
                }
        }

        // Bouton retour
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Bouton sauvegarder
        btnSauvegarder.setOnClickListener {
            val nouveauNom  = etNom.text.toString().trim()
            val nouvelEmail = etEmail.text.toString().trim()

            if (nouveauNom.isEmpty()) {
                etNom.error = "Entrez votre nom"; etNom.requestFocus(); return@setOnClickListener
            }
            if (nouvelEmail.isEmpty()) {
                etEmail.error = "Entrez votre email"; etEmail.requestFocus(); return@setOnClickListener
            }

            btnSauvegarder.isEnabled = false
            btnSauvegarder.text = "Sauvegarde..."

            if (user == null) {
                Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ ÉTAPE 1 — Mettre à jour le nom dans Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(nouveauNom)
                .build()

            user.updateProfile(profileUpdates).addOnSuccessListener {

                // ✅ ÉTAPE 2 — Sauvegarder nom dans Firestore
                firestore.collection("users").document(user.uid)
                    .update(mapOf("name" to nouveauNom, "email" to nouvelEmail))
                    .addOnSuccessListener {

                        // ✅ ÉTAPE 3 — Email changé ? → Réauthentifier d'abord
                        if (nouvelEmail != user.email) {
                            demanderMotDePassePourEmail(nouvelEmail, btnSauvegarder)
                        } else {
                            Toast.makeText(this, "Profil mis à jour ✓", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .addOnFailureListener { e ->
                        btnSauvegarder.isEnabled = true
                        btnSauvegarder.text = "Sauvegarder"
                        Toast.makeText(this, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }.addOnFailureListener { e ->
                btnSauvegarder.isEnabled = true
                btnSauvegarder.text = "Sauvegarder"
                Toast.makeText(this, "Erreur nom : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ─── DEMANDER MOT DE PASSE POUR CHANGER EMAIL ─────────────────────────────
    private fun demanderMotDePassePourEmail(nouvelEmail: String, btnSauvegarder: Button) {
        val input = EditText(this).apply {
            hint = "Mot de passe actuel"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(40, 20, 40, 20)
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmer votre identité")
            .setMessage("Pour changer l'email, entrez votre mot de passe actuel :")
            .setView(input)
            .setPositiveButton("Confirmer") { _, _ ->
                val motDePasse = input.text.toString().trim()
                if (motDePasse.isEmpty()) {
                    Toast.makeText(this, "Mot de passe requis", Toast.LENGTH_SHORT).show()
                    btnSauvegarder.isEnabled = true
                    btnSauvegarder.text = "Sauvegarder"
                    return@setPositiveButton
                }

                val user       = auth.currentUser ?: return@setPositiveButton
                val credential = EmailAuthProvider.getCredential(user.email!!, motDePasse)

                // ✅ Réauthentifier puis changer l'email
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.verifyBeforeUpdateEmail(nouvelEmail)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "✓ Vérifiez votre nouvelle adresse email pour confirmer.",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                btnSauvegarder.isEnabled = true
                                btnSauvegarder.text = "Sauvegarder"
                                Toast.makeText(this, "Erreur email : ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        btnSauvegarder.isEnabled = true
                        btnSauvegarder.text = "Sauvegarder"
                        Toast.makeText(this, "Mot de passe incorrect ❌", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Annuler") { _, _ ->
                btnSauvegarder.isEnabled = true
                btnSauvegarder.text = "Sauvegarder"
            }
            .show()
    }
}