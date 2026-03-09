package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnTogglePassword: ImageView
    private lateinit var btnGoLogin: TextView
    private lateinit var auth: FirebaseAuth
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF050A18.toInt()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        etName            = findViewById(R.id.etName)
        etEmail           = findViewById(R.id.etEmail)
        etPassword        = findViewById(R.id.etPassword)
        btnRegister       = findViewById(R.id.btnRegister)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnGoLogin        = findViewById(R.id.btnGoLogin)

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.inputType = if (passwordVisible)
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etPassword.setSelection(etPassword.text.length)
            btnTogglePassword.alpha = if (passwordVisible) 1f else 0.7f
        }

        btnRegister.setOnClickListener { registerUser() }

        btnGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }
    }

    private fun registerUser() {
        val name     = etName.text.toString().trim()
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        when {
            name.isEmpty()      -> { etName.error = "Entrez votre nom"; etName.requestFocus(); return }
            name.length < 2     -> { etName.error = "Nom trop court"; etName.requestFocus(); return }
            email.isEmpty()     -> { etEmail.error = "Entrez votre email"; etEmail.requestFocus(); return }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { etEmail.error = "Email invalide"; etEmail.requestFocus(); return }
            password.isEmpty()  -> { etPassword.error = "Entrez un mot de passe"; etPassword.requestFocus(); return }
            password.length < 6 -> { etPassword.error = "Minimum 6 caractères"; etPassword.requestFocus(); return }
        }

        btnRegister.isEnabled = false
        btnRegister.text = "Création..."

        Log.d("REGISTER", ">>> Tentative : $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                Log.d("REGISTER", "✅ Auth OK - UID: ${result.user?.uid}")
                val uid  = result.user?.uid ?: ""
                val user = result.user

                // Mettre à jour displayName dans Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user?.updateProfile(profileUpdates)

                // ✅ CORRIGÉ : document complet avec photoURL null
                val userData = hashMapOf(
                    "name"      to name,
                    "email"     to email,
                    "createdAt" to System.currentTimeMillis(),
                    "photoURL"  to null   // sera rempli quand l'user choisit une photo
                )

                try {
                    Firebase.firestore.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d("REGISTER", "✅ Firestore OK")
                            onRegisterSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e("REGISTER", "⚠️ Firestore erreur: ${e.message}")
                            // ✅ On laisse quand même l'user accéder à l'app
                            onRegisterSuccess()
                        }
                } catch (e: Exception) {
                    Log.e("REGISTER", "⚠️ Exception: ${e.message}")
                    onRegisterSuccess()
                }
            }
            .addOnFailureListener { e ->
                Log.e("REGISTER", "❌ Auth erreur: ${e.message}")

                // ✅ CORRIGÉ : remettre le bouton dans son état initial
                btnRegister.isEnabled = true
                btnRegister.text = "Créer un compte"

                val msg = if (e is FirebaseAuthException) {
                    when (e.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE"   -> "Cet email est déjà utilisé"
                        "ERROR_WEAK_PASSWORD"          -> "Mot de passe trop faible"
                        "ERROR_INVALID_EMAIL"          -> "Format email invalide"
                        "ERROR_NETWORK_REQUEST_FAILED" -> "Pas de connexion internet"
                        "ERROR_TOO_MANY_REQUESTS"      -> "Trop de tentatives. Réessaie plus tard"
                        else -> "Erreur: ${e.errorCode}"
                    }
                } else "Erreur: ${e.localizedMessage}"

                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
    }

    private fun onRegisterSuccess() {
        Toast.makeText(this, "Compte créé avec succès ! 🎉", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}