package com.example.myapp

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ProfilActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private lateinit var imgAvatar : ImageView
    private lateinit var tvNom     : TextView
    private lateinit var tvEmail   : TextView
    private val handler = Handler(Looper.getMainLooper())

    private val avatarFile: File
        get() {
            val uid = auth.currentUser?.uid ?: "guest"
            return File(filesDir, "avatar_profil_$uid.jpg")
        }

    // ─── PICK IMAGE ────────────────────────────────────────────────────────────
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            if (uri == null) return@registerForActivityResult

            try {
                val bytes = contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                } ?: return@registerForActivityResult

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@registerForActivityResult

                // ── Sauvegarder localement ──────────────────────────────────
                avatarFile.delete()
                FileOutputStream(avatarFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }

                imgAvatar.setImageBitmap(bitmap)
                imgAvatar.clearColorFilter()
                handler.postDelayed({ afficherDansBottomNav(bitmap) }, 100)

                // ── ✅ Sauvegarder dans Firestore (pour le dashboard admin) ──
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    // Réduire à max 200x200 pour ne pas dépasser la limite Firestore
                    val maxSize = 200
                    val scale   = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                    val newW    = (bitmap.width  * scale).toInt().coerceAtLeast(1)
                    val newH    = (bitmap.height * scale).toInt().coerceAtLeast(1)
                    val small   = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

                    val bos = ByteArrayOutputStream()
                    small.compress(Bitmap.CompressFormat.JPEG, 75, bos)
                    val base64Str  = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
                    val dataUrl    = "data:image/jpeg;base64,$base64Str"

                    db.collection("users").document(uid)
                        .set(mapOf("photoURL" to dataUrl), SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Photo mise à jour ✓", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // Photo locale OK même si Firestore échoue
                            Toast.makeText(this, "Photo locale OK, sync échouée: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Photo mise à jour ✓", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    // ─── ON CREATE ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()

        setContentView(R.layout.activity_profil)

        imgAvatar = findViewById(R.id.imgAvatar)
        tvNom     = findViewById(R.id.tvNom)
        tvEmail   = findViewById(R.id.tvEmail)

        findViewById<ImageButton>(R.id.btnEditAvatar).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<FrameLayout>(R.id.frameAvatar).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<CardView>(R.id.cardNomEmail).setOnClickListener {
            startActivity(Intent(this, EditNomEmailActivity::class.java))
        }

        findViewById<CardView>(R.id.cardMotDePasse).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        findViewById<CardView>(R.id.cardNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardDeconnexion).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui") { _, _ ->
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Non", null)
                .show()
        }
    }

    // ─── ON RESUME ─────────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()

        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        val user = auth.currentUser
        if (user != null) {
            tvNom.text   = ""
            tvEmail.text = user.email ?: "Email non disponible"

            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    tvNom.text = doc.getString("name")
                        ?: user.displayName
                                ?: "Utilisateur"
                }
                .addOnFailureListener {
                    tvNom.text = user.displayName ?: "Utilisateur"
                }
        }

        BottomNavHelper.setup(this, R.id.nav_profile)

        chargerPhotoLocale()

        handler.postDelayed({
            val file = avatarFile
            if (file.exists() && file.length() >= 100) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) afficherDansBottomNav(bitmap)
            }
        }, 300)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    // ─── CHARGER PHOTO LOCALE ──────────────────────────────────────────────────
    private fun chargerPhotoLocale() {
        if (!avatarFile.exists() || avatarFile.length() < 100) {
            imgAvatar.setImageResource(R.drawable.ic_person)
            return
        }
        val bitmap = BitmapFactory.decodeFile(avatarFile.absolutePath) ?: return
        imgAvatar.setImageBitmap(bitmap)
        imgAvatar.clearColorFilter()
    }

    // ─── AFFICHER PHOTO DANS BOTTOM NAV ───────────────────────────────────────
    private fun afficherDansBottomNav(bitmap: Bitmap) {
        try {
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
            val item = bottomNav.menu.findItem(R.id.nav_profile) ?: return

            val size   = 96
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint  = Paint().apply { isAntiAlias = true }

            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            paint.xfermode = null

            item.icon = BitmapDrawable(resources, output)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}