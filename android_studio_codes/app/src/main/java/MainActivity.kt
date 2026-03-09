package com.example.myapp

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.TimePickerDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapp.views.WaveformView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE            = 100
    private val PERMISSION_RECORD_AUDIO = 101

    private lateinit var btnMic              : ImageButton
    private lateinit var micContainer        : FrameLayout
    private lateinit var micRingOuter        : View
    private lateinit var waveformUnique      : WaveformView
    private lateinit var tvTexteUnique       : TextView
    private lateinit var layoutSaisieTexte   : LinearLayout
    private lateinit var etCommandeTexte     : EditText
    private lateinit var btnEnvoyerTexte     : ImageButton
    private lateinit var btnRaccourciQuestionLibre : LinearLayout
    private lateinit var btnRaccourciAppel    : LinearLayout
    private lateinit var btnRaccourciMessage  : LinearLayout
    private lateinit var btnRaccourciMeteo    : LinearLayout
    private lateinit var btnRaccourciRappel : LinearLayout
    private lateinit var btnRaccourciAlarme   : LinearLayout

    private lateinit var voiceService : AlexandraVoiceService
    private lateinit var alexandraTTS : AlexandraTTS

    private var isListening           = false
    private var pulseAnimator         : AnimatorSet? = null
    private var torchOn               = false
    private var premiereFois          = true
    private var quiParle              = ""
    private var raccourciActif        : LinearLayout? = null
    private var contactsSimilairesEnAttente: List<Contact> = emptyList()

    private var etatConversation      = EtatConversation.IDLE
    private var contactEnAttente      = ""
    private var telephoneEnAttente    = ""
    private var messageEnAttente      = ""
    private var appEnAttente          = ""
    private var typeCommandeEnAttente = ""

    private val mainHandler = Handler(Looper.getMainLooper())

    // ✅ AJOUT 1 : Déclarer OnlineManager
    private val onlineManager = OnlineManager()

    enum class EtatConversation {
        IDLE, ATTENTE_CONFIRMATION, ATTENTE_NOM_CONTACT,
        ATTENTE_MESSAGE, ATTENTE_CHOIX_CONTACT
    }

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFF0D0630.toInt()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        setContentView(R.layout.activity_main)

        requestPermissionsIfNeeded()
        bindViews()

        waveformUnique.visibility = View.VISIBLE
        waveformUnique.stopAnimating()

        setupMicButton()
        setupRaccourcis()
        setupSaisieTexte()
        startMicPulse()

        voiceService = AlexandraVoiceService(this)
        voiceService.init()
        alexandraTTS = AlexandraTTS(this)
        alexandraTTS.init()

        setupCallbacks()

        if (!AlexandraNotificationService.isPermissionGranted(this)) {
            mainHandler.postDelayed({ demanderPermissionNotifications() }, 3000)
        }

        // 🔥 Écoute des actions admin en temps réel
        AdminActionsManager.startListening(this)

        // 🔥 Migration historique local → Firestore
        HistoriqueManager.migrerVersFirestore(this)
    }

    // ✅ AJOUT 2 : onResume → démarrer le tracking en ligne
    override fun onResume() {
        super.onResume()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        BottomNavHelper.setup(this, R.id.nav_home)
        waveformUnique.visibility = View.VISIBLE
        startMicPulse()

        // ✅ Lance le tracking : écrit lastSeen dans Firestore immédiatement + toutes les 60s
        onlineManager.startTracking()
    }

    // ✅ AJOUT 3 : onPause → arrêter le tracking (marque hors ligne)
    override fun onPause() {
        super.onPause()
        pulseAnimator?.cancel()

        // ✅ Arrête le tracking et marque l'utilisateur hors ligne
        onlineManager.stopTracking()
    }

    // ✅ AJOUT 4 : onDestroy → nettoyage
    override fun onDestroy() {
        super.onDestroy()
        AdminActionsManager.stopListening()
        voiceService.destroy()
        alexandraTTS.destroy()

        // ✅ Force hors ligne au destroy
        onlineManager.setOffline()
    }

    // ─── CALLBACKS ─────────────────────────────────────────────────────────────

    private fun setupCallbacks() {

        alexandraTTS.onStart {
            runOnUiThread {
                quiParle = "alexandra"
                waveformUnique.startAnimating()
            }
        }

        alexandraTTS.onFin {
            runOnUiThread {
                if (quiParle == "alexandra") {
                    waveformUnique.stopAnimating()
                    tvTexteUnique.text = "Appuyez pour parler..."
                    quiParle = ""
                }
            }
        }

        voiceService.onSound {
            runOnUiThread {
                if (quiParle == "user") waveformUnique.startAnimating()
            }
        }

        voiceService.onSilence {
            runOnUiThread {
                if (quiParle == "user") waveformUnique.stopAnimating()
            }
        }

        voiceService.onResult { texte ->
            runOnUiThread {
                quiParle = ""
                waveformUnique.stopAnimating()
                traiterCommande(texte)
            }
        }

        voiceService.onError { erreur ->
            runOnUiThread {
                if (erreur.contains("client", ignoreCase = true)) {
                    mainHandler.postDelayed({
                        if (isListening) voiceService.startListening()
                    }, 800)
                } else {
                    quiParle = ""
                    waveformUnique.stopAnimating()
                    tvTexteUnique.text = erreur
                    stopListening()
                }
            }
        }

        alexandraTTS.onReady {
            runOnUiThread {
                val msg = "Bonjour, je suis Alexandra. Comment puis-je vous aider ?"
                tvTexteUnique.text = msg
                alexandraTTS.parler(msg)
            }
        }
    }

    // ─── BIND VIEWS ────────────────────────────────────────────────────────────

    private fun bindViews() {
        btnMic               = findViewById(R.id.btnMicrophone)
        micContainer         = findViewById(R.id.micContainer)
        micRingOuter         = findViewById(R.id.micRingOuter)
        waveformUnique       = findViewById(R.id.waveformUnique)
        tvTexteUnique        = findViewById(R.id.tvTexteUnique)
        layoutSaisieTexte    = findViewById(R.id.layoutSaisieTexte)
        etCommandeTexte      = findViewById(R.id.etCommandeTexte)
        btnEnvoyerTexte      = findViewById(R.id.btnEnvoyerTexte)
        btnRaccourciQuestionLibre = findViewById(R.id.btnRaccourciQuestionLibre)
        btnRaccourciAppel    = findViewById(R.id.btnRaccourciAppel)
        btnRaccourciMessage  = findViewById(R.id.btnRaccourciMessage)
        btnRaccourciMeteo    = findViewById(R.id.btnRaccourciMeteo)
        btnRaccourciRappel = findViewById(R.id.btnRaccourciRappel)
        btnRaccourciAlarme   = findViewById(R.id.btnRaccourciAlarme)
    }

    // ─── ONDES ─────────────────────────────────────────────────────────────────

    private fun afficherReponseAlexandra(texte: String) {
        quiParle = "alexandra"
        tvTexteUnique.text = texte
    }

    private fun arreterOnde() {
        waveformUnique.stopAnimating()
        tvTexteUnique.text = "Appuyez pour parler..."
        quiParle = ""
    }

    // ─── METEO API ─────────────────────────────────────────────────────────────

    private fun obtenirMeteoEtParler(ville: String) {
        afficherReponseAlexandra("Je cherche la météo pour $ville...")
        alexandraTTS.parler("Je cherche la météo pour $ville.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val villeEncode = Uri.encode(ville)
                val url = java.net.URL("https://wttr.in/$villeEncode?format=j1")
                val connexion = url.openConnection() as java.net.HttpURLConnection
                connexion.requestMethod = "GET"
                connexion.connectTimeout = 6000
                connexion.readTimeout = 6000
                connexion.setRequestProperty("User-Agent", "Alexandra-App")

                val texteReponse = connexion.inputStream.bufferedReader().readText()
                val json = org.json.JSONObject(texteReponse)

                val current   = json.getJSONArray("current_condition").getJSONObject(0)
                val tempC     = current.getString("temp_C")
                val feelsLike = current.getString("FeelsLikeC")
                val humidite  = current.getString("humidity")
                val vent      = current.getString("windspeedKmph")
                val desc      = current.getJSONArray("weatherDesc")
                    .getJSONObject(0).getString("value")
                val descFr    = traduireMeteo(desc)

                val meteoTexte = "À $ville, il fait $tempC degrés. " +
                        "Ressenti $feelsLike degrés. " +
                        "Temps : $descFr. " +
                        "Humidité $humidite pourcent. " +
                        "Vent à $vent kilomètres par heure."

                runOnUiThread {
                    afficherReponseAlexandra(
                        "🌤 $ville : $tempC°C — $descFr\n" +
                                "💧 Humidité : $humidite% | 💨 Vent : ${vent}km/h"
                    )
                    alexandraTTS.parler(meteoTexte)
                    desactiverTousRaccourcis()
                    HistoriqueManager.sauvegarder(this@MainActivity, "Météo $ville : $tempC°C")
                }

            } catch (e: Exception) {
                runOnUiThread {
                    val erreur = "Je n'arrive pas à obtenir la météo pour $ville. " +
                            "Vérifiez votre connexion internet."
                    afficherReponseAlexandra(erreur)
                    alexandraTTS.parler(erreur)
                }
            }
        }
    }

    private fun traduireMeteo(desc: String): String {
        return when {
            desc.contains("Sunny",        ignoreCase = true) -> "ensoleillé"
            desc.contains("Clear",        ignoreCase = true) -> "ciel dégagé"
            desc.contains("Partly cloudy",ignoreCase = true) -> "partiellement nuageux"
            desc.contains("Cloudy",       ignoreCase = true) -> "nuageux"
            desc.contains("Overcast",     ignoreCase = true) -> "couvert"
            desc.contains("Rain",         ignoreCase = true) -> "pluvieux"
            desc.contains("Drizzle",      ignoreCase = true) -> "bruine"
            desc.contains("Thunder",      ignoreCase = true) -> "orageux"
            desc.contains("Snow",         ignoreCase = true) -> "neigeux"
            desc.contains("Fog",          ignoreCase = true) -> "brouillard"
            desc.contains("Mist",         ignoreCase = true) -> "brumeux"
            desc.contains("Wind",         ignoreCase = true) -> "venteux"
            desc.contains("Blizzard",     ignoreCase = true) -> "blizzard"
            else -> desc
        }
    }

    // ─── WHATSAPP AUTO ─────────────────────────────────────────────────────────

    private fun verifierEtDemanderAccessibility(onAccepte: () -> Unit) {
        if (AlexandraAccessibilityService.instance != null) {
            onAccepte()
            return
        }
        val msg = "Pour envoyer des messages automatiquement, " +
                "j'ai besoin du service d'accessibilité. " +
                "Voulez-vous l'activer maintenant ?"
        afficherReponseAlexandra(msg)
        alexandraTTS.parler(msg)

        AlertDialog.Builder(this)
            .setTitle("🔐 Service d'accessibilité requis")
            .setMessage(
                "Pour envoyer des messages WhatsApp automatiquement, " +
                        "activez le service Alexandra :\n\n" +
                        "Paramètres → Accessibilité → Alexandra → Activer"
            )
            .setPositiveButton("Activer maintenant") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .setNegativeButton("Envoyer quand même") { _, _ ->
                onAccepte()
            }
            .setCancelable(false)
            .show()
    }

    private fun envoyerWhatsAppAuto(contact: String, telephone: String, message: String) {
        verifierEtDemanderAccessibility {

            if (AlexandraAccessibilityService.instance != null) {
                afficherReponseAlexandra("Envoi du message à $contact sur WhatsApp...")
                alexandraTTS.parler("J'envoie le message à $contact sur WhatsApp.")
                val pkg = "com.whatsapp"
                ouvrirApp(pkg)
                mainHandler.postDelayed({
                    AlexandraAccessibilityService.executerAction(
                        ActionApp(TypeAction.ENVOYER_MESSAGE, pkg, contact, message)
                    ) { resultat ->
                        runOnUiThread {
                            val msg = "Message envoyé à $contact sur WhatsApp."
                            afficherReponseAlexandra(msg)
                            alexandraTTS.parler(msg)
                            desactiverTousRaccourcis()
                            HistoriqueManager.sauvegarder(
                                this, "WhatsApp → $contact : $message"
                            )
                        }
                    }
                }, 3000)

            } else {
                afficherReponseAlexandra("Ouverture de WhatsApp pour $contact...")
                alexandraTTS.parler("J'ouvre WhatsApp avec le message prêt à envoyer.")
                try {
                    val numeroNettoye = telephone
                        .replace(" ", "")
                        .replace("-", "")
                        .replace(".", "")
                        .let {
                            if (!it.startsWith("+")) "+212${it.removePrefix("0")}" else it
                        }
                    val uri = Uri.parse(
                        "https://api.whatsapp.com/send?phone=$numeroNettoye" +
                                "&text=${Uri.encode(message)}"
                    )
                    startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    mainHandler.postDelayed({
                        AlexandraAccessibilityService.instance?.let {
                            AlexandraAccessibilityService.executerAction(
                                ActionApp(
                                    TypeAction.ENVOYER_MESSAGE,
                                    "com.whatsapp", contact, message
                                )
                            ) { r ->
                                runOnUiThread {
                                    afficherReponseAlexandra("Message envoyé à $contact.")
                                    alexandraTTS.parler("Message envoyé à $contact.")
                                    HistoriqueManager.sauvegarder(
                                        this, "WhatsApp → $contact : $message"
                                    )
                                }
                            }
                        }
                    }, 2000)
                } catch (e: Exception) {
                    val erreur = "Impossible d'ouvrir WhatsApp. Vérifiez qu'il est installé."
                    afficherReponseAlexandra(erreur)
                    alexandraTTS.parler(erreur)
                }
            }
        }
    }

    // ─── SAISIE TEXTE ──────────────────────────────────────────────────────────

    private fun setupSaisieTexte() {
        btnEnvoyerTexte.setOnClickListener { envoyerCommandeTexte() }
        etCommandeTexte.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { envoyerCommandeTexte(); true } else false
        }
    }

    private fun fermerSaisieTexte() {
        if (layoutSaisieTexte.visibility == View.VISIBLE) {
            layoutSaisieTexte.visibility = View.GONE
            etCommandeTexte.text.clear()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etCommandeTexte.windowToken, 0)
        }
    }

    private fun envoyerCommandeTexte() {
        val texte = etCommandeTexte.text.toString().trim()
        if (texte.isEmpty()) return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etCommandeTexte.windowToken, 0)
        layoutSaisieTexte.visibility = View.GONE
        etCommandeTexte.text.clear()
        traiterCommande(texte)
    }

    // ─── MICRO ─────────────────────────────────────────────────────────────────

    private fun setupMicButton() {
        btnMic.setOnClickListener {
            desactiverTousRaccourcis()
            fermerSaisieTexte()
            alexandraTTS.arreter()
            if (!isGranted(Manifest.permission.RECORD_AUDIO))
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_RECORD_AUDIO
                )
            else toggleListening()
        }
    }

    private fun toggleListening() {
        if (!isListening) startListening() else stopListening()
    }

    private fun startListening() {
        isListening = true
        quiParle = "user"
        btnMic.setImageResource(R.drawable.mon_micro)
        startListeningAnimation()
        waveformUnique.stopAnimating()
        tvTexteUnique.text = "Je vous écoute..."

        if (premiereFois) {
            premiereFois = false
            val intro = "Bonjour ! Ici Alexandra, votre assistante vocale. " +
                    "Je peux contrôler votre téléphone. " +
                    "Dites-moi n'importe quelle commande et je la ferai tout de suite."
            tvTexteUnique.text = intro
            alexandraTTS.parler(intro) {
                runOnUiThread {
                    if (isListening) {
                        quiParle = "user"
                        tvTexteUnique.text = "Je vous écoute..."
                        mainHandler.postDelayed({
                            if (isListening) voiceService.startListening()
                        }, 600)
                    }
                }
            }
        } else {
            mainHandler.postDelayed({
                if (isListening) voiceService.startListening()
            }, 300)
        }
    }

    private fun stopListening() {
        isListening = false
        quiParle = ""
        btnMic.setImageResource(R.drawable.mon_micro)
        waveformUnique.stopAnimating()
        tvTexteUnique.text = "Appuyez pour parler..."
        startMicPulse()
        voiceService.stopListening()
    }

    private fun startListeningPourReponse() {
        isListening = true
        quiParle = "user"
        btnMic.setImageResource(R.drawable.mon_micro)
        waveformUnique.stopAnimating()
        tvTexteUnique.text = "Je vous écoute..."
        startListeningAnimation()
        mainHandler.postDelayed({
            if (isListening) voiceService.startListening()
        }, 500)
    }

    // ─── RACCOURCIS ────────────────────────────────────────────────────────────

    private fun setupRaccourcis() {

        btnRaccourciQuestionLibre.setOnClickListener {
            fermerSaisieTexte()
            alexandraTTS.arreter()
            activerRaccourci(btnRaccourciQuestionLibre)

            val q = "Posez une question !"
            afficherReponseAlexandra(q)
            alexandraTTS.parler(q) {
                runOnUiThread {
                    typeCommandeEnAttente = "DISCUSSION_LIBRE"
                    etatConversation = EtatConversation.ATTENTE_MESSAGE
                    startListeningPourReponse()
                }
            }
        }

        btnRaccourciAppel.setOnClickListener {
            fermerSaisieTexte()
            alexandraTTS.arreter()
            activerRaccourci(btnRaccourciAppel)
            typeCommandeEnAttente = "APPEL"
            etatConversation = EtatConversation.ATTENTE_NOM_CONTACT
            val q = "Qui voulez-vous appeler ?"
            afficherReponseAlexandra(q)
            alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
        }

        btnRaccourciMessage.setOnClickListener {
            alexandraTTS.arreter()
            activerRaccourci(btnRaccourciMessage)
            layoutSaisieTexte.visibility = View.VISIBLE
            etCommandeTexte.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etCommandeTexte, InputMethodManager.SHOW_IMPLICIT)
            tvTexteUnique.text = "Écrivez votre commande..."
        }

        btnRaccourciMeteo.setOnClickListener {
            fermerSaisieTexte()
            alexandraTTS.arreter()
            activerRaccourci(btnRaccourciMeteo)
            typeCommandeEnAttente = "METEO"
            etatConversation = EtatConversation.ATTENTE_MESSAGE
            val q = "Pour quelle ville voulez-vous la météo ?"
            afficherReponseAlexandra(q)
            alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
        }

        btnRaccourciRappel.setOnClickListener {
            fermerSaisieTexte()
            alexandraTTS.arreter()
            activerRaccourci(btnRaccourciRappel)

            val q = "Quel rappel voulez-vous programmer ?"
            afficherReponseAlexandra(q)
            alexandraTTS.parler(q) {
                runOnUiThread {
                    typeCommandeEnAttente = "RAPPEL_NOTE"
                    etatConversation = EtatConversation.ATTENTE_MESSAGE
                    startListeningPourReponse()
                }
            }
        }

        btnRaccourciRappel.setOnLongClickListener {
            desactiverTousRaccourcis()
            alexandraTTS.arreter()
            startActivity(Intent(this, RappelsActivity::class.java))
            true
        }

        btnRaccourciAlarme.setOnClickListener {
            fermerSaisieTexte()
            alexandraTTS.arreter()
            activerRaccourci(btnRaccourciAlarme)
            val q = "À quelle heure voulez-vous l'alarme ?"
            afficherReponseAlexandra(q)
            alexandraTTS.parler(q) {
                runOnUiThread {
                    val cal = java.util.Calendar.getInstance()
                    TimePickerDialog(this@MainActivity, { _, heure, minute ->
                        val heureStr  = String.format("%02d:%02d", heure, minute)
                        val minuteTxt = if (minute > 0) " $minute" else ""
                        val label     = "Alarme Alexandra $heureStr"
                        afficherReponseAlexandra("Alarme programmée à $heureStr ✅")
                        alexandraTTS.parler("Alarme programmée à $heure heure$minuteTxt.")
                        lancerAlarme(heureStr, label)
                        desactiverTousRaccourcis()
                        HistoriqueManager.sauvegarder(this@MainActivity, "Alarme $heureStr")
                    }, cal.get(java.util.Calendar.HOUR_OF_DAY),
                        cal.get(java.util.Calendar.MINUTE), true).show()
                }
            }
        }
    }

    // ─── RACCOURCI ACTIF ───────────────────────────────────────────────────────

    private fun activerRaccourci(vue: LinearLayout) {
        raccourciActif?.setBackgroundResource(0)
        raccourciActif = vue
        vue.setBackgroundResource(R.drawable.bg_raccourci_actif)
    }

    private fun desactiverTousRaccourcis() {
        raccourciActif?.setBackgroundResource(0)
        raccourciActif = null
    }

    private fun animerRaccourci(view: View) {
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
        }.start()
    }

    // ─── TRAITEMENT COMMANDES ──────────────────────────────────────────────────

    private fun traiterCommande(texte: String) {
        when (etatConversation) {
            EtatConversation.ATTENTE_CONFIRMATION    -> { traiterConfirmation(texte); return }
            EtatConversation.ATTENTE_NOM_CONTACT     -> { traiterNomContact(texte);   return }
            EtatConversation.ATTENTE_MESSAGE         -> { traiterMessage(texte);      return }
            EtatConversation.ATTENTE_CHOIX_CONTACT   -> { traiterChoixContact(texte); return }
            else -> {}
        }
        tvTexteUnique.text = texte

        if (raccourciActif == null) {
            stopListening()
        } else {
            isListening = false
            quiParle = ""
            voiceService.stopListening()
            waveformUnique.stopAnimating()
        }

        CoroutineScope(Dispatchers.Main).launch {
            val reponse = GeminiService.analyserCommande(texte)
            runOnUiThread { executerReponse(reponse) }
        }
    }

    private fun executerReponse(reponse: GeminiReponse) {
        afficherReponseAlexandra(reponse.reponseVocale)

        if (reponse.action == "ENVOYER_MESSAGE" &&
            reponse.app.isNotEmpty() && !appEstAutoriseeParNom(reponse.app)) {
            alexandraTTS.parler(
                "Je n'ai pas accès à ${reponse.app}. Activez-la dans les applications connectées."
            )
            AlertDialog.Builder(this)
                .setTitle("${reponse.app} non autorisée")
                .setMessage("Allez dans Autorisations pour activer ${reponse.app}")
                .setPositiveButton("Ouvrir") { _, _ ->
                    startActivity(Intent(this, AutorisationsActivity::class.java))
                }
                .setNegativeButton("Annuler", null).show()
            return
        }

        when (reponse.action) {

            "LIRE_NOTIFS" -> {
                val r = AlexandraNotificationService.getResume()
                afficherReponseAlexandra(r)
                alexandraTTS.parler(r)
                HistoriqueManager.sauvegarder(this, "Notifications lues")
            }

            "LIRE_NOTIFS_APP" -> {
                val pkg = AlexandraNotificationService.trouverPackage(reponse.app) ?: ""
                val n   = if (pkg.isNotEmpty()) AlexandraNotificationService.compterNonLus(pkg) else 0
                val msg = if (n == 0) "Aucun message non lu sur ${reponse.app}."
                else "$n message${if(n>1)"s" else ""} non lu${if(n>1)"s" else ""} sur ${reponse.app}."
                afficherReponseAlexandra(msg)
                alexandraTTS.parler(msg)
                HistoriqueManager.sauvegarder(this, "Notifications ${reponse.app}")
            }

            "ENVOYER_MESSAGE" -> {
                appEnAttente          = reponse.app
                typeCommandeEnAttente = "ENVOYER_MESSAGE"
                contactEnAttente      = reponse.contact
                messageEnAttente      = reponse.message
                when {
                    reponse.contact.isEmpty() -> {
                        etatConversation = EtatConversation.ATTENTE_NOM_CONTACT
                        val q = "À qui voulez-vous envoyer le message sur ${reponse.app} ?"
                        afficherReponseAlexandra(q)
                        alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
                    }
                    reponse.message.isEmpty() -> {
                        etatConversation = EtatConversation.ATTENTE_MESSAGE
                        val q = "Que voulez-vous dire à ${reponse.contact} ?"
                        afficherReponseAlexandra(q)
                        alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
                    }
                    else -> {
                        etatConversation = EtatConversation.ATTENTE_CONFIRMATION
                        val q = "J'envoie : ${reponse.message}, à ${reponse.contact} " +
                                "sur ${reponse.app}. Confirmez ?"
                        afficherReponseAlexandra(q)
                        alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
                    }
                }
            }

            "OUVRIR_DISCUSSION" -> {
                val pkg = AlexandraNotificationService.trouverPackage(reponse.app)
                val msg = if (pkg != null) { ouvrirApp(pkg); "J'ouvre ${reponse.app}." }
                else "Je ne trouve pas ${reponse.app}."
                afficherReponseAlexandra(msg)
                alexandraTTS.parler(msg)
                HistoriqueManager.sauvegarder(this, "Ouverture ${reponse.app}")
            }

            "APPEL" -> {
                contactEnAttente = reponse.contact
                typeCommandeEnAttente = "APPEL"
                if (reponse.contact.isEmpty()) {
                    etatConversation = EtatConversation.ATTENTE_NOM_CONTACT
                    val q = "Qui voulez-vous appeler ?"
                    afficherReponseAlexandra(q)
                    alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
                } else {
                    etatConversation = EtatConversation.ATTENTE_CONFIRMATION
                    voiceService.rechercherContact(reponse.contact, "APPEL") { r ->
                        runOnUiThread {
                            if (r.startsWith("APPEL_CONTACT:"))
                                telephoneEnAttente = r.removePrefix("APPEL_CONTACT:")
                                    .split("|").getOrElse(1) { "" }
                        }
                    }
                    val q = "Appeler ${reponse.contact} ? Dites oui ou non."
                    afficherReponseAlexandra(q)
                    alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
                }
            }

            "APPEL_VIDEO" -> {
                val pkg = AlexandraNotificationService.trouverPackage(reponse.app) ?: "com.whatsapp"
                ouvrirApp(pkg)
                val m = "J'ouvre ${reponse.app} pour l'appel vidéo."
                afficherReponseAlexandra(m)
                alexandraTTS.parler(m)
                HistoriqueManager.sauvegarder(this, "Appel vidéo ${reponse.app}")
            }

            "ALARME" -> {
                afficherReponseAlexandra(reponse.reponseVocale)
                alexandraTTS.parler(reponse.reponseVocale) {
                    runOnUiThread { lancerAlarme(reponse.donnees) }
                }
                HistoriqueManager.sauvegarder(this, "Alarme ${reponse.donnees}")
            }

            "RAPPEL" -> {
                afficherReponseAlexandra(reponse.reponseVocale)
                alexandraTTS.parler(reponse.reponseVocale) {
                    runOnUiThread { lancerRappel(reponse.message, reponse.donnees) }
                }
                HistoriqueManager.sauvegarder(this, "Rappel : ${reponse.message}")
            }

            "TIMER" -> {
                afficherReponseAlexandra(reponse.reponseVocale)
                alexandraTTS.parler(reponse.reponseVocale) {
                    runOnUiThread { lancerTimer(reponse.donnees) }
                }
                HistoriqueManager.sauvegarder(this, "Timer ${reponse.donnees}s")
            }

            "METEO" -> { obtenirMeteoEtParler(reponse.donnees) }

            "RECHERCHE" -> {
                afficherReponseAlexandra(reponse.reponseVocale)
                alexandraTTS.parler(reponse.reponseVocale) {
                    runOnUiThread { ouvrirRecherche(reponse.donnees) }
                }
                HistoriqueManager.sauvegarder(this, "Recherche : ${reponse.donnees}")
            }

            "NAVIGATION" -> {
                afficherReponseAlexandra(reponse.reponseVocale)
                alexandraTTS.parler(reponse.reponseVocale) {
                    runOnUiThread { ouvrirNavigation(reponse.donnees) }
                }
                HistoriqueManager.sauvegarder(this, "Navigation : ${reponse.donnees}")
            }

            "MUSIQUE" -> {
                afficherReponseAlexandra(reponse.reponseVocale)
                alexandraTTS.parler(reponse.reponseVocale) {
                    runOnUiThread { lancerMusique(reponse.app, reponse.donnees) }
                }
                HistoriqueManager.sauvegarder(this, "Musique : ${reponse.donnees}")
            }

            "OUVRIR_APP" -> {
                val pkg = AlexandraNotificationService.trouverPackage(reponse.app)
                val m = if (pkg != null && ouvrirApp(pkg)) "J'ouvre ${reponse.app}."
                else "Je ne trouve pas ${reponse.app}."
                afficherReponseAlexandra(m)
                alexandraTTS.parler(m)
                HistoriqueManager.sauvegarder(this, "Ouverture ${reponse.app}")
            }

            "LIRE_ECRAN" -> {
                val node = AlexandraAccessibilityService.instance?.rootInActiveWindow
                if (node != null) {
                    val t = mutableListOf<String>()
                    lireNoeuds(node, t)
                    val c = if (t.isEmpty()) "Je ne vois pas de contenu."
                    else "Je vois : " + t.take(5).joinToString(". ")
                    afficherReponseAlexandra(c)
                    alexandraTTS.parler(c)
                    HistoriqueManager.sauvegarder(this, "Lecture écran")
                } else {
                    val m = "Le service d'accessibilité n'est pas actif."
                    afficherReponseAlexandra(m)
                    alexandraTTS.parler(m)
                }
            }

            "LAMPE_ON"      -> { allumerLampe(true);  afficherReponseAlexandra("Lampe allumée.");    alexandraTTS.parler("Lampe allumée.");    HistoriqueManager.sauvegarder(this, "Lampe allumée") }
            "LAMPE_OFF"     -> { allumerLampe(false); afficherReponseAlexandra("Lampe éteinte.");   alexandraTTS.parler("Lampe éteinte.");   HistoriqueManager.sauvegarder(this, "Lampe éteinte") }
            "BATTERIE"      -> { val n = getBatterie(); afficherReponseAlexandra("Batterie : $n%"); alexandraTTS.parler("Votre batterie est à $n pourcent."); HistoriqueManager.sauvegarder(this, "Batterie : $n%") }
            "WIFI_ON"       -> { changerWifi(true);   afficherReponseAlexandra("Wifi activé.");     alexandraTTS.parler("J'active le wifi.");     HistoriqueManager.sauvegarder(this, "Wifi activé") }
            "WIFI_OFF"      -> { changerWifi(false);  afficherReponseAlexandra("Wifi désactivé.");  alexandraTTS.parler("Je désactive le wifi.");  HistoriqueManager.sauvegarder(this, "Wifi désactivé") }
            "BLUETOOTH_ON"  -> { changerBluetooth(true);  afficherReponseAlexandra("Bluetooth activé.");    alexandraTTS.parler("Bluetooth activé.");    HistoriqueManager.sauvegarder(this, "Bluetooth activé") }
            "BLUETOOTH_OFF" -> { changerBluetooth(false); afficherReponseAlexandra("Bluetooth désactivé."); alexandraTTS.parler("Bluetooth désactivé."); HistoriqueManager.sauvegarder(this, "Bluetooth désactivé") }
            "VOLUME_HAUT"   -> { changerVolume(true);  afficherReponseAlexandra("Volume augmenté."); alexandraTTS.parler("Volume augmenté."); HistoriqueManager.sauvegarder(this, "Volume augmenté") }
            "VOLUME_BAS"    -> { changerVolume(false); afficherReponseAlexandra("Volume baissé.");  alexandraTTS.parler("Volume baissé.");  HistoriqueManager.sauvegarder(this, "Volume baissé") }

            else -> {
                afficherReponseAlexandra(reponse.reponseVocale)
                alexandraTTS.parler(reponse.reponseVocale)
                if (reponse.reponseVocale.isNotBlank())
                    HistoriqueManager.sauvegarder(this, reponse.reponseVocale.take(80))
            }
        }
    }

    // ─── ETATS CONVERSATION ────────────────────────────────────────────────────

    private fun traiterConfirmation(texte: String) {
        val t = texte.lowercase()
        when {
            t.contains("oui") || t.contains("ok") ||
                    t.contains("ouais") || t.contains("vas-y") -> {
                etatConversation = EtatConversation.IDLE
                executerActionFinale()
            }
            t.contains("non") || t.contains("annule") || t.contains("stop") -> {
                etatConversation = EtatConversation.IDLE
                desactiverTousRaccourcis()
                afficherReponseAlexandra("D'accord, annulé.")
                alexandraTTS.parler("D'accord, annulé.")
                HistoriqueManager.sauvegarder(this, "Commande annulée")
            }
            else -> {
                val q = "Dites oui ou non."
                afficherReponseAlexandra(q)
                alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
            }
        }
    }

    private fun traiterNomContact(texte: String) {
        etatConversation = EtatConversation.IDLE

        CoroutineScope(Dispatchers.Main).launch {
            val contactNettoye = GeminiService.extraireContact(texte)
            contactEnAttente = contactNettoye

            runOnUiThread {
                val contactsManager = ContactsManager(this@MainActivity)
                val resultats = contactsManager.chercherContact(contactNettoye)

                when {
                    resultats.size == 1 -> {
                        telephoneEnAttente = resultats[0].telephone
                        contactEnAttente = resultats[0].nom
                        if (typeCommandeEnAttente == "APPEL") {
                            etatConversation = EtatConversation.ATTENTE_CONFIRMATION
                            val q = "Appeler ${resultats[0].nom} ? Dites oui ou non."
                            afficherReponseAlexandra(q)
                            alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
                        } else {
                            etatConversation = EtatConversation.ATTENTE_MESSAGE
                            val q = "Que voulez-vous dire à ${resultats[0].nom} ?"
                            afficherReponseAlexandra(q)
                            alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
                        }
                    }

                    resultats.size > 1 -> {
                        etatConversation = EtatConversation.IDLE
                        proposerChoixContacts(resultats)
                    }

                    else -> {
                        val similaires = contactsManager.chercherContactsSimilaires(contactNettoye)
                        if (similaires.isNotEmpty()) {
                            val nomsProches = similaires.take(3)
                                .mapIndexed { i, c -> "${i + 1}. ${c.nom}" }
                                .joinToString(", ")
                            etatConversation = EtatConversation.IDLE
                            val msg = "Je n'ai pas trouvé $contactNettoye. " +
                                    "Voulez-vous dire : $nomsProches ? " +
                                    "Dites le numéro ou le nom."
                            afficherReponseAlexandra(msg)
                            alexandraTTS.parler(msg) {
                                runOnUiThread {
                                    contactsSimilairesEnAttente = similaires.take(3)
                                    etatConversation = EtatConversation.ATTENTE_CHOIX_CONTACT
                                    startListeningPourReponse()
                                }
                            }
                        } else {
                            etatConversation = EtatConversation.IDLE
                            val msg = "Je n'ai trouvé aucun contact nommé $contactNettoye dans vos contacts."
                            afficherReponseAlexandra(msg)
                            alexandraTTS.parler(msg)
                        }
                    }
                }
            }
        }
    }

    private fun proposerChoixContacts(contacts: List<Contact>) {
        contactsSimilairesEnAttente = contacts
        val noms = contacts.mapIndexed { i, c -> "${i + 1}. ${c.nom}" }.joinToString(", ")
        val msg = "J'ai trouvé plusieurs contacts : $noms. Lequel voulez-vous ?"
        afficherReponseAlexandra(msg)
        alexandraTTS.parler(msg) {
            runOnUiThread {
                etatConversation = EtatConversation.ATTENTE_CHOIX_CONTACT
                startListeningPourReponse()
            }
        }
    }

    private fun traiterChoixContact(texte: String) {
        val t = texte.lowercase().trim()
        etatConversation = EtatConversation.IDLE

        val numero = t.filter { it.isDigit() }.firstOrNull()?.digitToInt()
        if (numero != null && numero >= 1 && numero <= contactsSimilairesEnAttente.size) {
            val contact = contactsSimilairesEnAttente[numero - 1]
            choisirContact(contact)
            return
        }

        val contactTrouve = contactsSimilairesEnAttente.firstOrNull { c ->
            t.contains(c.nom.lowercase()) || c.nom.lowercase().contains(t)
        }

        if (contactTrouve != null) {
            choisirContact(contactTrouve)
        } else {
            val msg = "Je n'ai pas compris. Dites le numéro ou le nom du contact."
            afficherReponseAlexandra(msg)
            alexandraTTS.parler(msg) {
                runOnUiThread {
                    etatConversation = EtatConversation.ATTENTE_CHOIX_CONTACT
                    startListeningPourReponse()
                }
            }
        }
    }

    private fun choisirContact(contact: Contact) {
        contactEnAttente = contact.nom
        telephoneEnAttente = contact.telephone
        if (typeCommandeEnAttente == "APPEL") {
            etatConversation = EtatConversation.ATTENTE_CONFIRMATION
            val q = "Appeler ${contact.nom} ? Dites oui ou non."
            afficherReponseAlexandra(q)
            alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
        } else {
            etatConversation = EtatConversation.ATTENTE_MESSAGE
            val q = "Que voulez-vous dire à ${contact.nom} ?"
            afficherReponseAlexandra(q)
            alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
        }
    }

    private fun traiterMessage(texte: String) {
        if (typeCommandeEnAttente == "DISCUSSION_LIBRE") {
            etatConversation = EtatConversation.IDLE

            afficherReponseAlexandra("Je réfléchis...")
            CoroutineScope(Dispatchers.Main).launch {
                val reponse = GeminiService.discuterLibrement(texte)
                runOnUiThread {
                    afficherReponseAlexandra(reponse)

                    val stop = texte.lowercase().contains("stop") ||
                            texte.lowercase().contains("merci") ||
                            texte.lowercase().contains("arrête") ||
                            texte.lowercase().contains("terminer")

                    if (stop) {
                        typeCommandeEnAttente = ""
                        desactiverTousRaccourcis()
                        alexandraTTS.parler(reponse)
                    } else {
                        alexandraTTS.parler(reponse) {
                            runOnUiThread {
                                typeCommandeEnAttente = "DISCUSSION_LIBRE"
                                etatConversation = EtatConversation.ATTENTE_MESSAGE
                                startListeningPourReponse()
                            }
                        }
                    }

                    HistoriqueManager.sauvegarder(this@MainActivity, "Discussion: $texte")
                }
            }
            return
        }

        if (typeCommandeEnAttente == "RAPPEL_NOTE") {
            etatConversation = EtatConversation.IDLE
            typeCommandeEnAttente = ""

            CoroutineScope(Dispatchers.Main).launch {
                val rappelInfo = GeminiService.extraireRappel(texte)
                runOnUiThread {
                    afficherReponseAlexandra("✅ Rappel enregistré : ${rappelInfo.message} à ${rappelInfo.heure}")
                    alexandraTTS.parler("D'accord, je vous rappellerai : ${rappelInfo.message}")
                    programmNotificationRappel(
                        rappelInfo.message,
                        rappelInfo.heureMillis,
                        rappelInfo.heure,
                        rappelInfo.date
                    )
                    HistoriqueManager.sauvegarder(this@MainActivity, "Rappel: ${rappelInfo.message}")
                    desactiverTousRaccourcis()
                }
            }
            return
        }

        if (typeCommandeEnAttente == "METEO") {
            etatConversation = EtatConversation.IDLE
            typeCommandeEnAttente = ""
            obtenirMeteoEtParler(texte)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val messageNaturel = GeminiService.genererMessage(texte)
            messageEnAttente = messageNaturel
            runOnUiThread {
                etatConversation = EtatConversation.ATTENTE_CONFIRMATION
                val appNom = if (appEnAttente.isNotEmpty()) " sur $appEnAttente" else ""
                val q = "J'envoie : $messageNaturel, à $contactEnAttente$appNom. Confirmez ?"
                afficherReponseAlexandra(q)
                alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
            }
        }
    }

    private fun traiterResultatContact(resultat: String) {
        when {
            resultat.startsWith("APPEL_CONTACT:") -> {
                telephoneEnAttente = resultat.removePrefix("APPEL_CONTACT:")
                    .split("|").getOrElse(1) { "" }
                etatConversation = EtatConversation.ATTENTE_CONFIRMATION
                val q = "Appeler $contactEnAttente ? Dites oui ou non."
                afficherReponseAlexandra(q)
                alexandraTTS.parler(q) { runOnUiThread { startListeningPourReponse() } }
            }
            resultat.startsWith("CONTACT_INTROUVABLE:") -> {
                etatConversation = EtatConversation.IDLE
                val msg = "Je n'ai pas trouvé " +
                        "${resultat.removePrefix("CONTACT_INTROUVABLE:").split(":")[0]} " +
                        "dans vos contacts."
                afficherReponseAlexandra(msg)
                alexandraTTS.parler(msg)
            }
        }
    }

    private fun executerActionFinale() {
        when (typeCommandeEnAttente) {
            "ENVOYER_MESSAGE" -> {
                if (appEnAttente.lowercase().contains("whatsapp") ||
                    appEnAttente.lowercase().contains("what")) {
                    envoyerWhatsAppAuto(contactEnAttente, telephoneEnAttente, messageEnAttente)
                } else {
                    val pkg = AlexandraNotificationService.trouverPackage(appEnAttente)
                    if (pkg != null && AlexandraAccessibilityService.instance != null) {
                        afficherReponseAlexandra("Envoi sur $appEnAttente à $contactEnAttente...")
                        alexandraTTS.parler("J'envoie le message à $contactEnAttente sur $appEnAttente.")
                        ouvrirApp(pkg)
                        AlexandraAccessibilityService.executerAction(
                            ActionApp(TypeAction.ENVOYER_MESSAGE, pkg, contactEnAttente, messageEnAttente)
                        ) { r -> runOnUiThread { afficherReponseAlexandra(r); alexandraTTS.parler(r) } }
                        HistoriqueManager.sauvegarder(this, "$appEnAttente → $contactEnAttente : $messageEnAttente")
                    } else {
                        ouvrirApp(pkg ?: "")
                        val msg = "J'ai ouvert $appEnAttente."
                        afficherReponseAlexandra(msg)
                        alexandraTTS.parler(msg)
                    }
                }
            }
            "APPEL" -> {
                afficherReponseAlexandra("Appel de $contactEnAttente...")
                alexandraTTS.parler("J'appelle $contactEnAttente.") {
                    runOnUiThread {
                        lancerAppel(telephoneEnAttente)
                        desactiverTousRaccourcis()
                        HistoriqueManager.sauvegarder(this@MainActivity, "Appel $contactEnAttente")
                    }
                }
            }
        }
    }

    // ─── SYSTEME ───────────────────────────────────────────────────────────────

    private fun ouvrirApp(packageName: String): Boolean {
        return try { val i = packageManager.getLaunchIntentForPackage(packageName); if (i != null) { startActivity(i); true } else false } catch (e: Exception) { false }
    }
    private fun allumerLampe(on: Boolean) { try { val cm = getSystemService(CAMERA_SERVICE) as CameraManager; cm.setTorchMode(cm.cameraIdList.first(), on); torchOn = on } catch (e: Exception) {} }
    private fun getBatterie(): Int { val i = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)); val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1; val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1; return if (l >= 0 && s > 0) (l * 100 / s) else 0 }
    private fun changerWifi(on: Boolean) { try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) else @Suppress("DEPRECATION") (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).isWifiEnabled = on } catch (e: Exception) {} }
    private fun changerBluetooth(on: Boolean) { try { val bt = BluetoothAdapter.getDefaultAdapter(); if (on) bt?.enable() else bt?.disable() } catch (e: Exception) { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } }
    private fun changerVolume(augmenter: Boolean) { (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC, if (augmenter) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI) }
    private fun lireNoeuds(node: android.view.accessibility.AccessibilityNodeInfo?, textes: MutableList<String>) { node ?: return; val t = node.text?.toString()?.trim(); if (!t.isNullOrEmpty() && t.length > 2) textes.add(t); for (i in 0 until node.childCount) lireNoeuds(node.getChild(i), textes) }
    private fun appEstAutoriseeParNom(nomApp: String): Boolean { val pkg = AlexandraNotificationService.trouverPackage(nomApp) ?: return true; return getSharedPreferences("apps_connectees", Context.MODE_PRIVATE).getBoolean(pkg, false) }
    private fun demanderPermissionNotifications() { AlertDialog.Builder(this).setTitle("Notifications requises").setMessage("Paramètres → Accès aux notifications → Alexandra").setPositiveButton("Activer") { _, _ -> AlexandraNotificationService.ouvrirParametres(this) }.setNegativeButton("Plus tard", null).show() }
    private fun lancerAppel(telephone: String) { try { val i = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:${telephone.replace(" ","").replace("-","")}"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }; if (isGranted(Manifest.permission.CALL_PHONE)) startActivity(i) else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 102) } catch (e: Exception) {} }
    private fun lancerAlarme(heureData: String, label: String = "Alexandra") {
        Log.d("ALARME", "=== lancerAlarme appelée avec: $heureData, label: $label ===")
        try {
            val p = heureData.split(":")
            val heure = p.getOrElse(0) { "7" }.toIntOrNull() ?: 7
            val minute = p.getOrElse(1) { "0" }.toIntOrNull() ?: 0
            Log.d("ALARME", "Heure parsée: $heure:$minute")
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, heure)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                putExtra(AlarmClock.EXTRA_VIBRATE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val resolu = packageManager.resolveActivity(intent, 0)
            if (resolu != null) startActivity(intent)
            else {
                val fallback = packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
                    ?: packageManager.getLaunchIntentForPackage("com.android.deskclock")
                    ?: packageManager.getLaunchIntentForPackage("com.samsung.android.app.clockpackage")
                fallback?.let { startActivity(it.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
            }
        } catch (e: Exception) { Log.e("ALARME", "❌ EXCEPTION: ${e.message}", e) }
    }
    private fun lancerRappel(message: String, heureData: String) { try { val p = heureData.split(":"); startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply { putExtra(AlarmClock.EXTRA_HOUR, p.getOrElse(0){"8"}.toIntOrNull() ?: 8); putExtra(AlarmClock.EXTRA_MINUTES, p.getOrElse(1){"0"}.toIntOrNull() ?: 0); putExtra(AlarmClock.EXTRA_MESSAGE, message); putExtra(AlarmClock.EXTRA_SKIP_UI, true); flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } catch (e: Exception) {} }
    private fun lancerTimer(donnees: String) { try { startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply { putExtra(AlarmClock.EXTRA_LENGTH, donnees.toIntOrNull() ?: 300); putExtra(AlarmClock.EXTRA_SKIP_UI, true); flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } catch (e: Exception) {} }
    private fun lancerMusique(app: String, requete: String) { try { val pkg = AlexandraNotificationService.trouverPackage(app); if (pkg != null && ouvrirApp(pkg)) return; startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$requete")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } catch (e: Exception) {} }
    private fun ouvrirRecherche(requete: String) { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$requete")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } catch (e: Exception) {} }
    private fun ouvrirNavigation(destination: String) { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$destination")).apply { setPackage("com.google.android.apps.maps"); flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } catch (e: Exception) { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/maps?q=$destination")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) } catch (e2: Exception) {} } }
    private fun programmNotificationRappel(message: String, heureMillis: Long, heure: String = "", date: String = "") {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel("rappels_alexandra", "Rappels Alexandra", android.app.NotificationManager.IMPORTANCE_HIGH).apply { description = "Rappels programmés par Alexandra"; enableVibration(true) }
                getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
            }
            val intent = Intent(this, RappelReceiver::class.java).apply { putExtra("message", message) }
            val pendingIntent = android.app.PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { if (alarmManager.canScheduleExactAlarms()) alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, heureMillis, pendingIntent) }
            else alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, heureMillis, pendingIntent)
            Log.d("RAPPEL", "✅ Notification programmée pour: $heureMillis")
            RappelsActivity.sauvegarderRappel(this, RappelItem(message = message, heure = heure, date = date, heureMillis = heureMillis))
        } catch (e: Exception) { Log.e("RAPPEL", "❌ Erreur: ${e.message}") }
    }

    // ─── ANIMATIONS MICRO ──────────────────────────────────────────────────────

    private fun makeAnimator(target: Any, property: String, vararg values: Float, duration: Long): ObjectAnimator {
        return ObjectAnimator.ofFloat(target, property, *values).apply {
            this.duration = duration; this.repeatCount = ValueAnimator.INFINITE
            this.repeatMode = ValueAnimator.RESTART; interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun startMicPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = AnimatorSet().apply {
            playTogether(
                makeAnimator(micContainer, "scaleX", 1f, 1.08f, 1f, duration = 1800L),
                makeAnimator(micContainer, "scaleY", 1f, 1.08f, 1f, duration = 1800L),
                makeAnimator(micRingOuter, "scaleX", 1f, 1.25f, 1f, duration = 1800L),
                makeAnimator(micRingOuter, "scaleY", 1f, 1.25f, 1f, duration = 1800L),
                makeAnimator(micRingOuter, "alpha",  0.8f, 0f, 0.8f, duration = 1800L)
            ); start()
        }
    }

    private fun startListeningAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = AnimatorSet().apply {
            playTogether(
                makeAnimator(micContainer, "scaleX", 1f, 1.15f, 1f, duration = 900L),
                makeAnimator(micContainer, "scaleY", 1f, 1.15f, 1f, duration = 900L),
                makeAnimator(micRingOuter, "scaleX", 1f, 1.4f,  1f, duration = 900L),
                makeAnimator(micRingOuter, "scaleY", 1f, 1.4f,  1f, duration = 900L),
                makeAnimator(micRingOuter, "alpha",  1f, 0f,    1f, duration = 900L)
            ); start()
        }
    }

    // ─── PERMISSIONS ───────────────────────────────────────────────────────────

    private fun requestPermissionsIfNeeded() {
        val toRequest = mutableListOf<String>()
        if (!isGranted(Manifest.permission.RECORD_AUDIO))  toRequest.add(Manifest.permission.RECORD_AUDIO)
        if (!isGranted(Manifest.permission.READ_CONTACTS)) toRequest.add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            if (!isGranted(Manifest.permission.POST_NOTIFICATIONS))
                toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        if (toRequest.isNotEmpty())
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), REQUEST_CODE)
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) toggleListening()
    }
}