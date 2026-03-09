package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnTogglePassword: ImageView
    private lateinit var btnForgotPassword: TextView
    private lateinit var btnGoRegister: TextView
    private lateinit var auth: FirebaseAuth

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF050A18.toInt()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        auth = Firebase.auth

        // ✅ Déjà connecté → aller directement à Home
        if (auth.currentUser != null) {
            goToHome()
            return
        }

        setContentView(R.layout.activity_login)

        etEmail           = findViewById(R.id.etEmail)
        etPassword        = findViewById(R.id.etPassword)
        btnLogin          = findViewById(R.id.btnLogin)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnForgotPassword = findViewById(R.id.btnForgotPassword)
        btnGoRegister     = findViewById(R.id.btnGoRegister)

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            etPassword.inputType =
                if (passwordVisible)
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            etPassword.setSelection(etPassword.text.length)
            btnTogglePassword.alpha = if (passwordVisible) 1f else 0.7f
        }

        btnLogin.setOnClickListener { loginUser() }

        btnForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun loginUser() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        when {
            email.isEmpty() -> { etEmail.error = "Entrez votre email"; etEmail.requestFocus(); return }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { etEmail.error = "Email invalide"; etEmail.requestFocus(); return }
            password.isEmpty() -> { etPassword.error = "Entrez votre mot de passe"; etPassword.requestFocus(); return }
            password.length < 6 -> { etPassword.error = "Minimum 6 caractères"; etPassword.requestFocus(); return }
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Connexion..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Toast.makeText(this, "Bienvenue !", Toast.LENGTH_SHORT).show()
                goToHome()
            }
            .addOnFailureListener { e ->
                btnLogin.isEnabled = true
                btnLogin.text = "Se connecter"
                val msg = if (e is FirebaseAuthException) {
                    when (e.errorCode) {
                        "ERROR_USER_NOT_FOUND" -> "Aucun compte avec cet email"
                        "ERROR_WRONG_PASSWORD" -> "Mot de passe incorrect"
                        "ERROR_INVALID_EMAIL"  -> "Email invalide"
                        else -> e.localizedMessage ?: "Erreur inconnue"
                    }
                } else "Erreur : ${e.localizedMessage}"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
    }

    // ✅ Directement MainActivity — les permissions sont demandées là-bas
    private fun goToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}