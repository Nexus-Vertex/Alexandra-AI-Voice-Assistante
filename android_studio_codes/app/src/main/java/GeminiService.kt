package com.example.myapp

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {

    private const val API_KEY = "Your Groq link hier "
    private const val MODEL   = "llama-3.3-70b-versatile"
    private const val URL     = "https://api.groq.com/openai/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val PROMPT_COMMANDE = """
        Tu es Alexandra, assistante vocale Android comme Siri sur iPhone.
        Tu reponds TOUJOURS en francais.
        Tu reponds UNIQUEMENT avec un JSON valide, rien d'autre.

        FORMAT: {"action":"TYPE","app":"nom app","contact":"contact","message":"msg","reponse_vocale":"ce que tu dis","donnees":"info"}

        REGLES TRES IMPORTANTES POUR EXTRAIRE LE CONTACT ET LE MESSAGE:
        - Le champ "contact" doit contenir UNIQUEMENT le prenom ou nom de la personne. 
          JAMAIS de mots comme "a", "à", "le", "la", "envoyer", "envoie", "appelle", "appeler" etc.
        - Le champ "message" doit contenir UNIQUEMENT le contenu du message sans mots parasites.

        ACTIONS:
        LIRE_NOTIFS -> lire toutes les notifications non lues
        LIRE_NOTIFS_APP -> messages non lus d'une app (app=nom)
        ENVOYER_MESSAGE -> envoyer message dans n'importe quelle app (app+contact+message)
        OUVRIR_DISCUSSION -> ouvrir discussion (app+contact)
        APPEL -> appel telephone (contact)
        APPEL_VIDEO -> appel video (app+contact)
        ALARME -> alarme (donnees=HH:MM)
        RAPPEL -> rappel (message+donnees=HH:MM)
        TIMER -> minuteur (donnees=secondes)
        METEO -> meteo (donnees=ville)
        MUSIQUE -> musique (app+donnees=titre)
        NAVIGATION -> GPS (donnees=destination)
        RECHERCHE -> Google (donnees=requete)
        OUVRIR_APP -> ouvrir app (app=nom)
        LIRE_ECRAN -> lire contenu ecran
        LAMPE_ON -> allumer lampe
        LAMPE_OFF -> eteindre lampe
        BATTERIE -> niveau batterie
        WIFI_ON -> activer wifi
        WIFI_OFF -> desactiver wifi
        BLUETOOTH_ON -> activer bluetooth
        BLUETOOTH_OFF -> desactiver bluetooth
        VOLUME_HAUT -> augmenter volume
        VOLUME_BAS -> baisser volume
        INCONNU -> commande non reconnue

        EXEMPLES COMPLETS:
        "ouvre instagram" -> {"action":"OUVRIR_APP","app":"Instagram","contact":"","message":"","reponse_vocale":"J'ouvre Instagram.","donnees":""}
        "appelle maman" -> {"action":"APPEL","app":"","contact":"maman","message":"","reponse_vocale":"J'appelle maman.","donnees":""}
        "alarme 7h30" -> {"action":"ALARME","app":"","contact":"","message":"","reponse_vocale":"Alarme programmee a 7h30.","donnees":"07:30"}
        "meteo casablanca" -> {"action":"METEO","app":"","contact":"","message":"","reponse_vocale":"Je cherche la meteo pour Casablanca.","donnees":"Casablanca"}
        "wifi off" -> {"action":"WIFI_OFF","app":"","contact":"","message":"","reponse_vocale":"Je desactive le wifi.","donnees":""}
    """.trimIndent()

    private val PROMPT_DISCUSSION = """
        Tu es Alexandra, une assistante personnelle intelligente, chaleureuse et sympathique.
        Tu reponds TOUJOURS en francais, de maniere naturelle et conversationnelle.
        Tu es comme une amie qui repond a toutes les questions.
        Tes reponses sont courtes (2-3 phrases max) pour etre lues a voix haute facilement.
        Tu peux repondre a : questions generales, culture, conseils, blagues, histoires, calculs, definitions, etc.
        Ne commence JAMAIS par "En tant qu'IA" ou "Je suis une IA".
        Commence directement par la reponse.
        
        Exemples:
        "c'est quoi la capitale du Japon?" -> "La capitale du Japon est Tokyo, une ville fascinante avec plus de 13 millions d'habitants !"
        "raconte moi une blague" -> "Pourquoi les plongeurs plongent-ils toujours en arrière ? Parce que sinon ils tomberaient dans le bateau !"
        "comment faire une omelette?" -> "Cassez 3 oeufs, battez-les avec du sel et du poivre, puis faites cuire dans une poele avec du beurre chaud pendant 2 minutes. Simple et delicieux !"
    """.trimIndent()

    private val PROMPT_CONTACT = """
        Tu es un extracteur de nom de contact.
        Extrais UNIQUEMENT le prenom ou nom de la personne.
        Reponds UNIQUEMENT avec le nom propre, rien d'autre. Premiere lettre en majuscule.
        Exemples:
        "a leila" -> Leila
        "appelle ahmed" -> Ahmed
        "a maman" -> maman
    """.trimIndent()

    private val PROMPT_MESSAGE = """
        Tu es un extracteur de message.
        Extrais UNIQUEMENT le contenu du message a envoyer.
        Supprime tous les mots parasites comme: dis lui, ecris, que, dire que etc.
        Reponds UNIQUEMENT avec le message, rien d'autre.
        Exemples:
        "dis lui bonjour" -> bonjour
        "que je suis en route" -> je suis en route
    """.trimIndent()

    private val PROMPT_RAPPEL = """
        Tu es un extracteur de rappel intelligent.
        Tu dois extraire ces informations et retourner UNIQUEMENT un JSON valide, rien d'autre.
        La date actuelle t'est fournie dans la requete.
        FORMAT STRICT: {"message":"texte du rappel","heure":"HH:mm","date":"yyyy-MM-dd"}
        Exemples:
        "demain a 14h rendez-vous" -> {"message":"rendez-vous","heure":"14:00","date":"2026-03-06"}
        "ce soir a 20h appeler le medecin" -> {"message":"appeler le medecin","heure":"20:00","date":"2026-03-05"}
    """.trimIndent()

    private val PROMPT_GENERER_MESSAGE = """
        Tu es Alexandra, assistante personnelle.
        L'utilisateur veut envoyer un message a quelqu'un.
        Tu dois reformuler le message de facon naturelle et conversationnelle en francais.
        Le message doit etre court, naturel, comme si c'etait l'utilisateur qui ecrivait.
        Reponds UNIQUEMENT avec le message reformule, rien d'autre.
        
        Exemples:
        "dis lui que je serai en retard" -> "Je serai en retard, desolé !"
        "bonjour comment tu vas" -> "Bonjour ! Comment tu vas ?"
        "je suis arrive" -> "Je suis arrivé !"
        "on se voit demain" -> "On se voit demain alors ?"
    """.trimIndent()

    // ─────────────────────────────────────────────────────────────────────────
    // 🔥 MODIFIÉ — analyserCommande avec sauvegarde Firestore
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun analyserCommande(texteUtilisateur: String): GeminiReponse {
        return withContext(Dispatchers.IO) {
            try {
                val reponseTexte = appelerGroq(texteUtilisateur, PROMPT_COMMANDE)
                Log.d("GEMINI_CMD", "OK: $reponseTexte")

                val json = JSONObject(
                    reponseTexte.removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                )

                val reponse = GeminiReponse(
                    action        = json.optString("action", "INCONNU"),
                    app           = json.optString("app", ""),
                    contact       = json.optString("contact", ""),
                    telephone     = "",
                    message       = json.optString("message", ""),
                    reponseVocale = json.optString("reponse_vocale", "Commande non reconnue."),
                    donnees       = json.optString("donnees", "")
                )

                // 🔥 SAUVEGARDE FIRESTORE — Historique de la commande
                if (reponse.action != "INCONNU") {
                    val typeLabel = when (reponse.action) {
                        "ALARME"          -> "Alarme"
                        "RAPPEL"          -> "Rappel"
                        "TIMER"           -> "Minuteur"
                        "METEO"           -> "Météo"
                        "NAVIGATION"      -> "Navigation"
                        "MUSIQUE"         -> "Musique"
                        "APPEL"           -> "Appel"
                        "APPEL_VIDEO"     -> "Appel vidéo"
                        "ENVOYER_MESSAGE" -> "Message"
                        "OUVRIR_APP"      -> "Ouvrir app"
                        "LIRE_NOTIFS",
                        "LIRE_NOTIFS_APP" -> "Notifications"
                        "WIFI_ON",
                        "WIFI_OFF"        -> "Wi-Fi"
                        "BLUETOOTH_ON",
                        "BLUETOOTH_OFF"   -> "Bluetooth"
                        "LAMPE_ON",
                        "LAMPE_OFF"       -> "Lampe torche"
                        "VOLUME_HAUT",
                        "VOLUME_BAS"      -> "Volume"
                        "BATTERIE"        -> "Batterie"
                        "RECHERCHE"       -> "Recherche"
                        else              -> "Commande vocale"
                    }
                    FirestoreManager.sauvegarderHistorique(
                        commande = texteUtilisateur,
                        type     = typeLabel,
                        resultat = "Succès"
                    )
                }

                reponse

            } catch (e: Exception) {
                Log.e("GEMINI_CMD", "Erreur: ${e.message}")

                // 🔥 SAUVEGARDE FIRESTORE — Commande échouée
                FirestoreManager.sauvegarderHistorique(
                    commande = texteUtilisateur,
                    type     = "Commande vocale",
                    resultat = "Échec"
                )

                GeminiReponse(action = "INCONNU", reponseVocale = "Desole, commande non reconnue.")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 🔥 MODIFIÉ — discuterLibrement avec sauvegarde Firestore
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun discuterLibrement(question: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val reponse = appelerGroq(question, PROMPT_DISCUSSION).trim()
                Log.d("GEMINI_DISCUSSION", "Réponse: $reponse")

                // 🔥 SAUVEGARDE FIRESTORE — Discussion libre
                FirestoreManager.sauvegarderDiscussion(
                    question = question,
                    reponse  = reponse
                )

                reponse
            } catch (e: Exception) {
                Log.e("GEMINI_DISCUSSION", "Erreur: ${e.message}")
                "Désolée, je n'arrive pas à répondre pour le moment."
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pas de modification — genererMessage (inchangé)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun genererMessage(texte: String): String {
        return withContext(Dispatchers.IO) {
            try {
                appelerGroq(texte, PROMPT_GENERER_MESSAGE).trim()
            } catch (e: Exception) {
                texte
            }
        }
    }

    suspend fun extraireContact(texte: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val resultat = appelerGroq(texte, PROMPT_CONTACT).trim()
                resultat.replaceFirstChar { it.uppercase() }
            } catch (e: Exception) {
                extraireContactLocal(texte)
            }
        }
    }

    suspend fun extraireMessage(texte: String): String {
        return withContext(Dispatchers.IO) {
            try {
                appelerGroq(texte, PROMPT_MESSAGE).trim()
            } catch (e: Exception) {
                extraireMessageLocal(texte)
            }
        }
    }

    suspend fun extraireRappel(texte: String): RappelInfo {
        return withContext(Dispatchers.IO) {
            try {
                val maintenant = java.util.Calendar.getInstance()
                val dateAujourdhui = String.format(
                    "%04d-%02d-%02d",
                    maintenant.get(java.util.Calendar.YEAR),
                    maintenant.get(java.util.Calendar.MONTH) + 1,
                    maintenant.get(java.util.Calendar.DAY_OF_MONTH)
                )
                val texteAvecDate = "Date actuelle: $dateAujourdhui\nTexte: $texte"
                val reponseTexte = appelerGroq(texteAvecDate, PROMPT_RAPPEL).trim()

                val json = JSONObject(
                    reponseTexte.removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                )

                val message  = json.optString("message", texte)
                val heureStr = json.optString("heure", "09:00")
                val dateStr  = json.optString("date", dateAujourdhui)

                val cal = java.util.Calendar.getInstance()
                try {
                    val dateParts  = dateStr.split("-")
                    val heureParts = heureStr.split(":")
                    cal.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt(),
                        heureParts[0].toInt(), heureParts[1].toInt(), 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                } catch (e: Exception) {
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                    cal.set(java.util.Calendar.MINUTE, 0)
                }

                RappelInfo(message = message, heure = heureStr, date = dateStr, heureMillis = cal.timeInMillis)

            } catch (e: Exception) {
                Log.e("GEMINI_RAPPEL", "Erreur: ${e.message}")
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                RappelInfo(message = texte, heure = "09:00", date = "demain", heureMillis = cal.timeInMillis)
            }
        }
    }

    private fun extraireContactLocal(texte: String): String {
        var r = texte.trim().lowercase()
        listOf("à ", "a ", "appelle ", "appeler ").forEach { if (r.startsWith(it)) { r = r.removePrefix(it); return@forEach } }
        return r.replaceFirstChar { it.uppercase() }
    }

    private fun extraireMessageLocal(texte: String): String {
        var r = texte.trim().lowercase()
        listOf("dis lui que ", "dis lui ", "dire que ", "que ").forEach { if (r.startsWith(it)) { r = r.removePrefix(it); return@forEach } }
        return r.replaceFirstChar { it.uppercase() }
    }

    private fun appelerGroq(texte: String, systemPrompt: String): String {
        val body = JSONObject().apply {
            put("model", MODEL)
            put("temperature", 0.1)
            put("max_tokens", 300)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user");   put("content", texte) })
            })
        }
        val request = Request.Builder().url(URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $responseBody")
        return JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}

data class GeminiReponse(
    val action        : String = "INCONNU",
    val app           : String = "",
    val contact       : String = "",
    val telephone     : String = "",
    val message       : String = "",
    val reponseVocale : String = "",
    val donnees       : String = ""
)

data class RappelInfo(
    val message     : String,
    val heure       : String,
    val date        : String,
    val heureMillis : Long

)
