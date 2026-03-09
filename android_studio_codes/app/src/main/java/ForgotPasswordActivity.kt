package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var btnSendCode: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF050A18.toInt()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        auth = Firebase.auth

        setContentView(R.layout.activity_forgot_password)

        emailInput      = findViewById(R.id.emailInput)
        btnSendCode     = findViewById(R.id.btnSendCode)
        btnBackToLogin  = findViewById(R.id.btnBackToLogin)

        btnSendCode.setOnClickListener {

            val email = emailInput.text.toString().trim()

            when {
                email.isEmpty() -> {
                    emailInput.error = "Entrez votre email"
                    emailInput.requestFocus()
                    return@setOnClickListener
                }

                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailInput.error = "Email invalide"
                    emailInput.requestFocus()
                    return@setOnClickListener
                }
            }

            btnSendCode.isEnabled = false
            btnSendCode.text = "Envoi en cours..."

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {

                    btnSendCode.isEnabled = true
                    btnSendCode.text = "Send Code"

                    Toast.makeText(
                        this,
                        "Email envoyé à $email\nVérifie ta boîte mail !",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }

                .addOnFailureListener { e ->

                    btnSendCode.isEnabled = true
                    btnSendCode.text = "Send Code"

                    Toast.makeText(
                        this,
                        "Erreur : ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}