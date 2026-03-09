package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var btnPrev: android.widget.Button
    private lateinit var btnNext: android.widget.Button
    private lateinit var btnSkip: TextView
    private lateinit var dots: Array<ImageView>
    private var currentPage = 0
    private var totalPages = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )

        setContentView(R.layout.activity_intro)

        viewPager   = findViewById(R.id.viewPager)
        dotsLayout  = findViewById(R.id.dotsLayout)
        btnPrev     = findViewById(R.id.btnPrev)
        btnNext     = findViewById(R.id.btnNext)
        btnSkip     = findViewById(R.id.btnSkip)

        val slides = listOf(
            Slide(
                R.drawable.alex1,
                "Bonjour, je suis\nAlexandra",
                "Ton assistante vocale intelligente."
            ),
            Slide(
                R.drawable.alex2,
                "Je peux contrôler\nton téléphone",
                "Ouvrir des applications, envoyer des messages,\net plus encore avec des commandes vocales simples."
            ),
            Slide(
                R.drawable.alex3,
                "Ta sécurité\nest prioritaire",
                "Je demande toujours confirmation\navant d'effectuer une action sensible."
            )
        )

        totalPages = slides.size
        viewPager.adapter = IntroAdapter(slides)
        setupDots(totalPages)
        updateButtons(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateDots(position)
                updateButtons(position)
            }
        })

        btnPrev.setOnClickListener {
            if (currentPage > 0) {
                viewPager.currentItem = currentPage - 1
            }
        }

        btnNext.setOnClickListener {
            if (currentPage < totalPages - 1) {
                viewPager.currentItem = currentPage + 1
            } else {
                goToLogin()
            }
        }

        btnSkip.setOnClickListener {
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupDots(count: Int) {
        dots = Array(count) { ImageView(this) }
        dotsLayout.removeAllViews()
        for (i in 0 until count) {
            dots[i].setImageResource(R.drawable.dot_inactive)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 0, 8, 0)
            dotsLayout.addView(dots[i], params)
        }
        dots[0].setImageResource(R.drawable.dot_active)
    }

    private fun updateDots(position: Int) {
        for (i in dots.indices) {
            dots[i].setImageResource(
                if (i == position) R.drawable.dot_active
                else R.drawable.dot_inactive
            )
        }
    }

    private fun updateButtons(position: Int) {
        btnPrev.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        btnNext.text = if (position == totalPages - 1) "Commencer" else "Suivant"

        btnSkip.visibility = View.VISIBLE
    }
}