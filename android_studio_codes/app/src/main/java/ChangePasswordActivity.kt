package com.example.myapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()

        setContentView(R.layout.activity_change_password)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnChanger).setOnClickListener {
            val ancienMdp    = findViewById<EditText>(R.id.etAncienMdp).text.toString()
            val nouveauMdp   = findViewById<EditText>(R.id.etNouveauMdp).text.toString()
            val confirmMdp   = findViewById<EditText>(R.id.etConfirmMdp).text.toString()

            if (ancienMdp.isEmpty() || nouveauMdp.isEmpty() || confirmMdp.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nouveauMdp != confirmMdp) {
                Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nouveauMdp.length < 6) {
                Toast.makeText(this, "Mot de passe trop court (min 6 caractères)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            val email = user?.email ?: return@setOnClickListener

            // Re-authentifier avant de changer le mot de passe
            val credential = EmailAuthProvider.getCredential(email, ancienMdp)
            user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                if (reAuthTask.isSuccessful) {
                    user.updatePassword(nouveauMdp).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(this, "Mot de passe modifié ✓", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Erreur : ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Ancien mot de passe incorrect", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}