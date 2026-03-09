package com.example.myapp

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoriqueActivity : AppCompatActivity() {

    private lateinit var recyclerView      : RecyclerView
    private lateinit var tvVide            : TextView
    private lateinit var barreSelection    : LinearLayout
    private lateinit var tvNbSelectionnes  : TextView
    private lateinit var btnSelectionnerTout : LinearLayout
    private lateinit var btnSupprimerSelection : ImageButton
    private lateinit var btnAnnulerSelection   : ImageButton

    private lateinit var adapter : HistoriqueAdapter
    private var commandes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()
        setContentView(R.layout.activity_historique)

        bindViews()
        chargerHistorique()
        setupAdapter()
        setupBarreSelection()
    }

    private fun bindViews() {
        recyclerView           = findViewById(R.id.rvHistorique)
        tvVide                 = findViewById(R.id.tvHistoriqueVide)
        barreSelection         = findViewById(R.id.barreSelection)
        tvNbSelectionnes       = findViewById(R.id.tvNbSelectionnes)
        btnSelectionnerTout    = findViewById(R.id.btnSelectionnerTout)
        btnSupprimerSelection  = findViewById(R.id.btnSupprimerSelection)
        btnAnnulerSelection    = findViewById(R.id.btnAnnulerSelection)
    }

    private fun chargerHistorique() {
        commandes = HistoriqueManager.getListe(this).toMutableList()
    }

    private fun setupAdapter() {
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = HistoriqueAdapter(
            commandes = commandes,
            onSelectionChanged = { nbSelectionnes ->
                mettreAJourBarreSelection(nbSelectionnes)
            }
        )
        recyclerView.adapter = adapter
        mettreAJourAffichageVide()
    }

    private fun setupBarreSelection() {

        // Bouton ANNULER
        btnAnnulerSelection.setOnClickListener {
            adapter.desactiverModeSelection()
            barreSelection.visibility = View.GONE
        }

        // Bouton SÉLECTIONNER TOUT
        btnSelectionnerTout.setOnClickListener {
            val toutSelectionne = adapter.toutEstSelectionne()
            if (toutSelectionne) {
                adapter.deselectionnerTout()
            } else {
                adapter.selectionnerTout()
            }
        }

        // Bouton SUPPRIMER
        btnSupprimerSelection.setOnClickListener {
            val nb = adapter.getNbSelectionnes()
            if (nb == 0) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Supprimer $nb commande${if (nb > 1) "s" else ""} ?")
                .setMessage("Cette action est irréversible.")
                .setPositiveButton("Supprimer") { _, _ ->
                    supprimerSelectionnes()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    private fun mettreAJourBarreSelection(nbSelectionnes: Int) {
        if (nbSelectionnes == 0 && !adapter.estEnModeSelection()) {
            barreSelection.visibility = View.GONE
            return
        }

        barreSelection.visibility = View.VISIBLE
        tvNbSelectionnes.text = "$nbSelectionnes sélectionné${if (nbSelectionnes > 1) "s" else ""}"

        // Changer le texte du bouton selon l'état
        val tvSelTout = btnSelectionnerTout.findViewById<TextView>(R.id.tvSelectionnerTout)
        tvSelTout?.text = if (adapter.toutEstSelectionne()) "Tout désélectionner" else "Tout sélectionner"

        // Activer/désactiver le bouton supprimer
        btnSupprimerSelection.alpha = if (nbSelectionnes > 0) 1f else 0.4f
        btnSupprimerSelection.isEnabled = nbSelectionnes > 0
    }

    private fun supprimerSelectionnes() {
        val indices = adapter.getIndicesSelectionnes()

        // Supprimer de la liste (en partant de la fin pour ne pas décaler les indices)
        indices.sortedDescending().forEach { index ->
            if (index < commandes.size) {
                commandes.removeAt(index)
            }
        }

        // Sauvegarder la nouvelle liste
        HistoriqueManager.remplacerListe(this, commandes)

        // Mettre à jour l'affichage
        adapter.desactiverModeSelection()
        adapter.notifyDataSetChanged()
        barreSelection.visibility = View.GONE
        mettreAJourAffichageVide()

        val nb = indices.size
        Toast.makeText(this, "$nb commande${if (nb > 1) "s" else ""} supprimée${if (nb > 1) "s" else ""}", Toast.LENGTH_SHORT).show()
    }

    private fun mettreAJourAffichageVide() {
        tvVide.visibility = if (commandes.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        BottomNavHelper.setup(this, R.id.nav_historique)
    }

    // Fermer la barre de sélection si on appuie sur retour
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (adapter.estEnModeSelection()) {
            adapter.desactiverModeSelection()
            barreSelection.visibility = View.GONE
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}

// ─── ADAPTER ───────────────────────────────────────────────────────────────────

class HistoriqueAdapter(
    private val commandes: MutableList<String>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<HistoriqueAdapter.ViewHolder>() {

    private val selectionnes = mutableSetOf<Int>()
    private var modeSelection = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCommande  : TextView  = view.findViewById(R.id.tvCommande)
        val tvNumero    : TextView  = view.findViewById(R.id.tvNumero)
        val checkBox    : CheckBox  = view.findViewById(R.id.checkBoxItem)
        val conteneur   : View      = view.findViewById(R.id.conteneurItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historique, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvNumero.text   = "#${position + 1}"
        holder.tvCommande.text = commandes[position]

        // Afficher/masquer checkbox selon le mode
        holder.checkBox.visibility = if (modeSelection) View.VISIBLE else View.GONE
        holder.checkBox.isChecked  = selectionnes.contains(position)

        // Couleur de fond si sélectionné
        holder.conteneur.setBackgroundResource(
            if (selectionnes.contains(position)) R.drawable.bg_item_selectionne
            else R.drawable.bg_item_historique
        )

        // Clic normal → si mode sélection, sélectionner/désélectionner
        holder.itemView.setOnClickListener {
            if (modeSelection) {
                toggleSelection(position, holder)
            }
        }

        // Long clic → activer mode sélection
        holder.itemView.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            if (!modeSelection) {
                modeSelection = true
            }
            toggleSelection(position, holder)
            true
        }

        // Clic sur checkbox
        holder.checkBox.setOnClickListener {
            toggleSelection(position, holder)
        }
    }

    private fun toggleSelection(position: Int, holder: ViewHolder) {
        if (selectionnes.contains(position)) {
            selectionnes.remove(position)
            holder.checkBox.isChecked = false
            holder.conteneur.setBackgroundResource(R.drawable.bg_item_historique)
        } else {
            selectionnes.add(position)
            holder.checkBox.isChecked = true
            holder.conteneur.setBackgroundResource(R.drawable.bg_item_selectionne)
        }

        // Animation légère
        holder.itemView.animate()
            .scaleX(0.97f).scaleY(0.97f).setDuration(80)
            .withEndAction {
                holder.itemView.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()

        onSelectionChanged(selectionnes.size)
    }

    fun selectionnerTout() {
        selectionnes.clear()
        for (i in commandes.indices) selectionnes.add(i)
        notifyDataSetChanged()
        onSelectionChanged(selectionnes.size)
    }

    fun deselectionnerTout() {
        selectionnes.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun toutEstSelectionne() = selectionnes.size == commandes.size

    fun estEnModeSelection() = modeSelection

    fun getNbSelectionnes() = selectionnes.size

    fun getIndicesSelectionnes(): List<Int> = selectionnes.toList()

    fun desactiverModeSelection() {
        modeSelection = false
        selectionnes.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    override fun getItemCount() = commandes.size
}