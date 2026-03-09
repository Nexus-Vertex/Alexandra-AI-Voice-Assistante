package com.example.myapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class RappelsActivity : AppCompatActivity() {

    private lateinit var btnBack          : ImageButton
    private lateinit var btnAdd           : ImageButton
    private lateinit var tvAVenir         : TextView
    private lateinit var tvAujourdhui     : TextView
    private lateinit var tvUrgent         : TextView
    private lateinit var tvTotal          : TextView
    private lateinit var btnFiltreAll     : TextView
    private lateinit var btnFiltreUrgent  : TextView
    private lateinit var btnFiltreToday   : TextView
    private lateinit var btnFiltreUpcoming: TextView
    private lateinit var btnFiltreDone    : TextView
    private lateinit var recyclerView     : RecyclerView
    private lateinit var tvEmptyState     : LinearLayout
    private lateinit var fabAjouter       : ImageButton

    private var rappels = mutableListOf<RappelItem>()
    private var filtreActif = "tous"
    private lateinit var adapter: RappelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#07041a")
        window.navigationBarColor = android.graphics.Color.parseColor("#100d2e")
        setContentView(R.layout.activity_rappels)

        bindViews()
        chargerRappels()
        setupRecyclerView()
        setupFiltres()
        setupBoutons()
        mettreAJourStats()
    }

    private fun bindViews() {
        btnBack           = findViewById(R.id.btnBackRappels)
        btnAdd            = findViewById(R.id.btnAddRappel)
        tvAVenir          = findViewById(R.id.tvStatAVenir)
        tvAujourdhui      = findViewById(R.id.tvStatAujourdhui)
        tvUrgent          = findViewById(R.id.tvStatUrgent)
        tvTotal           = findViewById(R.id.tvStatTotal)
        btnFiltreAll      = findViewById(R.id.btnFiltreAll)
        btnFiltreUrgent   = findViewById(R.id.btnFiltreUrgent)
        btnFiltreToday    = findViewById(R.id.btnFiltreToday)
        btnFiltreUpcoming = findViewById(R.id.btnFiltreUpcoming)
        btnFiltreDone     = findViewById(R.id.btnFiltreDone)
        recyclerView      = findViewById(R.id.recyclerRappels)
        tvEmptyState      = findViewById(R.id.layoutEmptyState)
        fabAjouter        = findViewById(R.id.fabAjouter)
    }

    private fun setupRecyclerView() {
        adapter = RappelsAdapter(
            getRappelsFiltres(),
            onDelete = { rappel -> supprimerRappel(rappel) },
            onToggleDone = { rappel -> toggleDone(rappel) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFiltres() {
        val filtres = listOf(
            btnFiltreAll      to "tous",
            btnFiltreUrgent   to "urgent",
            btnFiltreToday    to "today",
            btnFiltreUpcoming to "upcoming",
            btnFiltreDone     to "done"
        )
        filtres.forEach { (btn, type) ->
            btn.setOnClickListener {
                filtreActif = type
                filtres.forEach { (b, _) -> b.isSelected = false }
                btn.isSelected = true
                rafraichirListe()
            }
        }
        btnFiltreAll.isSelected = true
    }

    private fun setupBoutons() {
        btnBack.setOnClickListener { finish() }
        fabAjouter.setOnClickListener { afficherDialogueAjout() }
        btnAdd.setOnClickListener { afficherDialogueAjout() }
    }

    private fun afficherDialogueAjout() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_ajouter_rappel, null)
        val etMessage  = view.findViewById<EditText>(R.id.etMessageRappel)
        val datePicker = view.findViewById<DatePicker>(R.id.datePickerRappel)
        val timePicker = view.findViewById<TimePicker>(R.id.timePickerRappel)
        timePicker.setIs24HourView(true)

        val dialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setView(view)
            .setPositiveButton("Ajouter") { _, _ ->
                val message = etMessage.text.toString().trim()
                if (message.isEmpty()) return@setPositiveButton

                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR,         datePicker.year)
                    set(Calendar.MONTH,        datePicker.month)
                    set(Calendar.DAY_OF_MONTH, datePicker.dayOfMonth)
                    set(Calendar.HOUR_OF_DAY,  timePicker.hour)
                    set(Calendar.MINUTE,       timePicker.minute)
                    set(Calendar.SECOND,       0)
                }

                val sdfHeure = SimpleDateFormat("HH:mm",       Locale.getDefault())
                val sdfDate  = SimpleDateFormat("dd/MM/yyyy",  Locale.getDefault())

                val rappel = RappelItem(
                    message     = message,
                    heure       = sdfHeure.format(cal.time),
                    date        = sdfDate.format(cal.time),
                    heureMillis = cal.timeInMillis
                )

                RappelsActivity.sauvegarderRappel(this, rappel)
                chargerRappels()
                mettreAJourStats()
                rafraichirListe()
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    // ─── DONNÉES ───────────────────────────────────────────────────────────────

    private fun chargerRappels() {
        val prefs = getSharedPreferences("rappels_alexandra", Context.MODE_PRIVATE)
        val json  = prefs.getString("liste_rappels", "[]") ?: "[]"
        rappels.clear()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                rappels.add(
                    RappelItem(
                        id          = obj.optString("id", UUID.randomUUID().toString()),
                        message     = obj.optString("message", ""),
                        heure       = obj.optString("heure", ""),
                        date        = obj.optString("date", ""),
                        heureMillis = obj.optLong("heureMillis", 0L),
                        isDone      = obj.optBoolean("isDone", false)
                    )
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
        rappels.sortBy { it.heureMillis }
    }

    private fun sauvegarderRappels() {
        val arr = JSONArray()
        rappels.forEach { r ->
            arr.put(JSONObject().apply {
                put("id",          r.id)
                put("message",     r.message)
                put("heure",       r.heure)
                put("date",        r.date)
                put("heureMillis", r.heureMillis)
                put("isDone",      r.isDone)
            })
        }
        getSharedPreferences("rappels_alexandra", Context.MODE_PRIVATE)
            .edit().putString("liste_rappels", arr.toString()).apply()
    }

    private fun supprimerRappel(rappel: RappelItem) {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le rappel ?")
            .setMessage(rappel.message)
            .setPositiveButton("Supprimer") { _, _ ->
                rappels.removeAll { it.id == rappel.id }
                sauvegarderRappels()
                mettreAJourStats()
                rafraichirListe()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun toggleDone(rappel: RappelItem) {
        val index = rappels.indexOfFirst { it.id == rappel.id }
        if (index >= 0) {
            rappels[index] = rappels[index].copy(isDone = !rappels[index].isDone)
            sauvegarderRappels()
            mettreAJourStats()
            rafraichirListe()
        }
    }

    private fun getRappelsFiltres(): List<RappelItem> {
        val now        = System.currentTimeMillis()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 86_400_000L

        return when (filtreActif) {
            "urgent"   -> rappels.filter { !it.isDone && it.heureMillis in now..now + 3_600_000L }
            "today"    -> rappels.filter { !it.isDone && it.heureMillis in todayStart..todayEnd }
            "upcoming" -> rappels.filter { !it.isDone && it.heureMillis > todayEnd }
            "done"     -> rappels.filter { it.isDone }
            else       -> rappels
        }
    }

    private fun mettreAJourStats() {
        val now        = System.currentTimeMillis()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 86_400_000L

        tvUrgent.text     = rappels.count { !it.isDone && it.heureMillis in now..now + 3_600_000L }.toString()
        tvAujourdhui.text = rappels.count { !it.isDone && it.heureMillis in todayStart..todayEnd }.toString()
        tvAVenir.text     = rappels.count { !it.isDone && it.heureMillis > todayEnd }.toString()
        tvTotal.text      = rappels.size.toString()
    }

    private fun rafraichirListe() {
        val liste = getRappelsFiltres()
        tvEmptyState.visibility = if (liste.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (liste.isEmpty()) View.GONE else View.VISIBLE
        adapter.mettreAJour(liste)
    }

    override fun onResume() {
        super.onResume()
        chargerRappels()
        mettreAJourStats()
        rafraichirListe()
    }

    // ─── COMPANION : sauvegarder depuis MainActivity ───────────────────────────

    companion object {
        fun sauvegarderRappel(context: Context, rappel: RappelItem) {
            val prefs = context.getSharedPreferences("rappels_alexandra", Context.MODE_PRIVATE)
            val json  = prefs.getString("liste_rappels", "[]") ?: "[]"
            val arr   = try { JSONArray(json) } catch (e: Exception) { JSONArray() }
            arr.put(JSONObject().apply {
                put("id",          rappel.id)
                put("message",     rappel.message)
                put("heure",       rappel.heure)
                put("date",        rappel.date)
                put("heureMillis", rappel.heureMillis)
                put("isDone",      false)
            })
            prefs.edit().putString("liste_rappels", arr.toString()).apply()
        }
    }
}

// ─── DATA CLASS ────────────────────────────────────────────────────────────────

data class RappelItem(
    val id          : String = UUID.randomUUID().toString(),
    val message     : String,
    val heure       : String,
    val date        : String,
    val heureMillis : Long,
    val isDone      : Boolean = false
)

// ─── ADAPTER ───────────────────────────────────────────────────────────────────

class RappelsAdapter(
    private var liste       : List<RappelItem>,
    private val onDelete    : (RappelItem) -> Unit,
    private val onToggleDone: (RappelItem) -> Unit
) : RecyclerView.Adapter<RappelsAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card       : androidx.cardview.widget.CardView = v.findViewById(R.id.cardRappel)
        val tvMessage  : TextView    = v.findViewById(R.id.tvRappelMessage)
        val tvDateTime : TextView    = v.findViewById(R.id.tvRappelDateTime)
        val tvBadge    : TextView    = v.findViewById(R.id.tvRappelBadge)
        val tvIcon     : TextView    = v.findViewById(R.id.tvRappelIcon)
        val btnDelete  : ImageButton = v.findViewById(R.id.btnDeleteRappel)
        val viewStripe : View        = v.findViewById(R.id.viewStripe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_rappel, parent, false))

    override fun getItemCount() = liste.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r   = liste[pos]
        val now = System.currentTimeMillis()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 86_400_000L

        // Message
        h.tvMessage.text = r.message
        if (r.isDone) {
            h.tvMessage.paintFlags = h.tvMessage.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            h.tvMessage.alpha = 0.5f
        } else {
            h.tvMessage.paintFlags = h.tvMessage.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            h.tvMessage.alpha = 1f
        }

        // Date/Heure affichée
        val dateAffichee = when {
            r.isDone -> "Terminé"
            r.heureMillis in now..now + 3_600_000L -> "Aujourd'hui · ${r.heure} 🔥"
            r.heureMillis in todayStart..todayEnd  -> "Aujourd'hui · ${r.heure}"
            else -> "${r.date} · ${r.heure}"
        }
        h.tvDateTime.text = dateAffichee

        // Badge + Stripe couleur
        when {
            r.isDone -> {
                h.tvBadge.text = "TERMINÉ"
                h.tvBadge.setBackgroundResource(R.drawable.badge_done)
                h.viewStripe.setBackgroundResource(R.drawable.stripe_grey)
                h.card.alpha = 0.6f
            }
            r.heureMillis in now..now + 3_600_000L -> {
                h.tvBadge.text = "URGENT"
                h.tvBadge.setBackgroundResource(R.drawable.badge_urgent)
                h.viewStripe.setBackgroundResource(R.drawable.stripe_orange)
                h.card.alpha = 1f
            }
            r.heureMillis in todayStart..todayEnd -> {
                h.tvBadge.text = "CE SOIR"
                h.tvBadge.setBackgroundResource(R.drawable.badge_today)
                h.viewStripe.setBackgroundResource(R.drawable.stripe_green)
                h.card.alpha = 1f
            }
            else -> {
                h.tvBadge.text = "BIENTÔT"
                h.tvBadge.setBackgroundResource(R.drawable.badge_soon)
                h.viewStripe.setBackgroundResource(R.drawable.stripe_purple)
                h.card.alpha = 1f
            }
        }

        // Icône selon le message
        h.tvIcon.text = when {
            r.message.contains("médecin",      ignoreCase = true) ||
                    r.message.contains("dentiste",     ignoreCase = true) ||
                    r.message.contains("médical",      ignoreCase = true) -> "🏥"
            r.message.contains("appel",        ignoreCase = true) ||
                    r.message.contains("appeler",      ignoreCase = true) -> "📞"
            r.message.contains("réunion",      ignoreCase = true) ||
                    r.message.contains("meeting",      ignoreCase = true) -> "💼"
            r.message.contains("médicament",   ignoreCase = true) -> "💊"
            r.message.contains("anniversaire", ignoreCase = true) -> "🎂"
            r.message.contains("voyage",       ignoreCase = true) ||
                    r.message.contains("avion",        ignoreCase = true) -> "✈️"
            r.message.contains("courses",      ignoreCase = true) ||
                    r.message.contains("marché",       ignoreCase = true) -> "🛒"
            else -> "🔔"
        }

        // Actions
        h.btnDelete.setOnClickListener { onDelete(r) }
        h.card.setOnClickListener { onToggleDone(r) }
    }

    fun mettreAJour(nouvelleListe: List<RappelItem>) {
        liste = nouvelleListe
        notifyDataSetChanged()
    }
}