package com.example.myapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class AlexandraVoiceService(private val context: Context) {

    private var speechRecognizer  : SpeechRecognizer? = null
    private var isListening        = false
    private var estDetruit         = false

    private var onResultCallback   : ((String) -> Unit)? = null
    private var onErrorCallback    : ((String) -> Unit)? = null
    private var onReadyCallback    : (() -> Unit)?       = null
    private var onSoundCallback    : (() -> Unit)?       = null   // son détecté → ondes ON
    private var onSilenceCallback  : (() -> Unit)?       = null   // silence → ondes OFF

    val contactsManager = ContactsManager(context)
    private val handler = Handler(Looper.getMainLooper())

    // ─────────────────────────────────────────────────────────────────────────

    fun init() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onErrorCallback?.invoke("Reconnaissance vocale non disponible")
            return
        }
        creerRecognizer()
    }

    private fun creerRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(creerListener())
        Log.d("VOICE", "✅ Recognizer créé")
    }

    private fun creerListener() = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VOICE", "✅ Prêt à écouter")
            isListening = true
            onReadyCallback?.invoke()
        }

        override fun onBeginningOfSpeech() {
            // ✅ L'utilisateur commence à parler → démarrer les ondes
            Log.d("VOICE", "🎙️ Parole détectée")
            onSoundCallback?.invoke()
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Son détecté (volume) — ondes actives seulement si son présent
            // rmsdB > 2 = son réel (pas silence)
            if (rmsdB > 2f) {
                onSoundCallback?.invoke()
            } else {
                onSilenceCallback?.invoke()
            }
        }

        override fun onEndOfSpeech() {
            // ✅ Utilisateur a arrêté de parler → arrêter les ondes
            Log.d("VOICE", "🔇 Fin de parole")
            isListening = false
            onSilenceCallback?.invoke()
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            onSilenceCallback?.invoke()

            val tousResultats = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?: arrayListOf()

            val texte = tousResultats.firstOrNull()?.trim() ?: ""
            Log.d("VOICE", "✅ Texte reconnu : '$texte'")

            if (texte.isNotEmpty()) {
                onResultCallback?.invoke(texte)
            } else {
                onErrorCallback?.invoke("Je n'ai rien entendu, réessayez.")
            }
        }

        override fun onError(error: Int) {
            isListening = false
            onSilenceCallback?.invoke()

            when (error) {
                SpeechRecognizer.ERROR_CLIENT -> {
                    // Erreur au démarrage — réessayer silencieusement
                    Log.w("VOICE", "⚠️ Erreur client → réessai dans 800ms")
                    handler.postDelayed({
                        if (!estDetruit && isListening.not()) {
                            creerRecognizer()
                        }
                    }, 800)
                    return
                }
                8 /* ERROR_RECOGNIZER_BUSY */ -> {
                    Log.w("VOICE", "⚠️ Recognizer occupé → recréation")
                    handler.postDelayed({
                        if (!estDetruit) creerRecognizer()
                    }, 500)
                    return
                }
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    // Pas de correspondance — ne rien afficher, juste logger
                    Log.w("VOICE", "⚠️ Pas de correspondance")
                    onErrorCallback?.invoke("Je n'ai pas compris, réessayez.")
                }
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    Log.w("VOICE", "⚠️ Timeout")
                    onErrorCallback?.invoke("Vous n'avez rien dit.")
                }
                SpeechRecognizer.ERROR_AUDIO -> {
                    Log.e("VOICE", "❌ Erreur audio")
                    onErrorCallback?.invoke("Erreur audio.")
                }
                SpeechRecognizer.ERROR_NETWORK -> {
                    Log.e("VOICE", "❌ Pas de réseau")
                    onErrorCallback?.invoke("Pas de connexion internet.")
                }
                else -> {
                    Log.e("VOICE", "❌ Erreur ($error)")
                    onErrorCallback?.invoke("Erreur ($error)")
                }
            }
        }

        override fun onPartialResults(p: Bundle?)    {}
        override fun onEvent(e: Int, p: Bundle?)     {}
        override fun onBufferReceived(b: ByteArray?) {}
    }

    // ─────────────────────────────────────────────────────────────────────────

    fun startListening() {
        if (isListening) {
            Log.w("VOICE", "⚠️ Déjà en écoute — ignoré")
            return
        }
        if (estDetruit) {
            Log.w("VOICE", "⚠️ Service détruit — ignoré")
            return
        }
        handler.postDelayed({
            if (!isListening && !estDetruit) demarrerEcoute()
        }, 200)
    }

    private fun demarrerEcoute() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }
        try {
            speechRecognizer?.startListening(intent)
            Log.d("VOICE", "🎤 Écoute démarrée")
        } catch (e: Exception) {
            Log.e("VOICE", "❌ Impossible de démarrer : ${e.message}")
            creerRecognizer()
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            onSilenceCallback?.invoke()
            Log.d("VOICE", "🛑 Écoute arrêtée")
        }
    }

    // ─── CALLBACKS ────────────────────────────────────────────────────────────

    fun onResult(callback: (String) -> Unit)  { onResultCallback  = callback }
    fun onError(callback: (String) -> Unit)   { onErrorCallback   = callback }
    fun onReady(callback: () -> Unit)         { onReadyCallback   = callback }
    fun onSound(callback: () -> Unit)         { onSoundCallback   = callback }   // ✅ NOUVEAU
    fun onSilence(callback: () -> Unit)       { onSilenceCallback = callback }   // ✅ NOUVEAU

    // ─── CONTACTS ─────────────────────────────────────────────────────────────

    fun rechercherContact(nom: String, type: String = "WHATSAPP", callback: (String) -> Unit) {
        val contacts = contactsManager.chercherContact(nom)
        Log.d("VOICE", "🔍 Recherche '$nom' → ${contacts.size} résultat(s)")
        when {
            contacts.isEmpty() ->
                callback("CONTACT_INTROUVABLE:$nom:$type")
            contacts.size == 1 ->
                callback("${type}_CONTACT:${contacts[0].nom}|${contacts[0].telephone}|$nom")
            else -> {
                val liste = contacts.take(3).joinToString(",") { "${it.nom}|${it.telephone}" }
                callback("${type}_PLUSIEURS:$liste|$nom")
            }
        }
    }

    fun destroy() {
        estDetruit = true
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        Log.d("VOICE", "🗑️ Service détruit")
    }
}