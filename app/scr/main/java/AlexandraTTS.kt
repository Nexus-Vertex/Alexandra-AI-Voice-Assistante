package com.example.myapp

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class AlexandraTTS(private val context: Context) {

    private var tts              : TextToSpeech? = null
    private var isReady           = false
    private var onReadyCallback   : (() -> Unit)? = null
    private var onStartCallback   : (() -> Unit)? = null   // ✅ Alexandra commence → ondes ON
    private var onFinCallback     : (() -> Unit)? = null   // ✅ Alexandra finit → ondes OFF

    fun init() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.FRENCH)

                // Chercher voix féminine française
                val voixFeminine = tts?.voices?.filter { voice ->
                    voice.locale.language == "fr" &&
                            !voice.isNetworkConnectionRequired &&
                            (voice.name.contains("female", ignoreCase = true) ||
                                    voice.name.contains("f-",     ignoreCase = true) ||
                                    voice.name.contains("-f-",    ignoreCase = true))
                }?.maxByOrNull { it.quality }

                if (voixFeminine != null) {
                    tts?.voice = voixFeminine
                    Log.d("TTS", "Voix feminine : ${voixFeminine.name}")
                } else {
                    val meilleureVoix = tts?.voices
                        ?.filter { it.locale.language == "fr" && !it.isNetworkConnectionRequired }
                        ?.maxByOrNull { it.quality }
                    if (meilleureVoix != null) tts?.voice = meilleureVoix
                }

                tts?.setPitch(1.2f)
                tts?.setSpeechRate(0.95f)
                isReady = true
                onReadyCallback?.invoke()
            }
        }
    }

    fun parler(texte: String, onFin: (() -> Unit)? = null) {
        if (!isReady || texte.isEmpty()) {
            onFin?.invoke()
            return
        }
        tts?.stop()

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "alexandra")

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                // ✅ Alexandra commence à parler → signal ondes ON
                onStartCallback?.invoke()
                Log.d("TTS", "🔊 Alexandra parle...")
            }
            override fun onDone(id: String?) {
                // ✅ Alexandra finit → signal ondes OFF
                onFinCallback?.invoke()
                onFin?.invoke()
                Log.d("TTS", "✅ Alexandra a fini")
            }
            override fun onError(id: String?) {
                onFinCallback?.invoke()
                onFin?.invoke()
                Log.e("TTS", "❌ Erreur TTS")
            }
        })

        tts?.speak(texte, TextToSpeech.QUEUE_FLUSH, params, "alexandra")
    }

    fun arreter() {
        tts?.stop()
        // ✅ Arrêt forcé → signal ondes OFF
        onFinCallback?.invoke()
    }

    // ─── CALLBACKS ────────────────────────────────────────────────────────────

    fun onReady(callback: () -> Unit) {
        onReadyCallback = callback
        if (isReady) callback()
    }

    fun onStart(callback: () -> Unit)  { onStartCallback = callback }  // ✅ NOUVEAU
    fun onFin(callback: () -> Unit)    { onFinCallback   = callback }  // ✅ NOUVEAU

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}