package com.example.myapp

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

data class Contact(
    val nom: String,
    val telephone: String
)

class ContactsManager(private val context: Context) {

    fun chercherContact(nomRecherche: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val nomLower = nomRecherche.lowercase().trim()

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val nom = it.getString(0) ?: continue
                    val tel = it.getString(1) ?: continue
                    val nomContactLower = nom.lowercase()
                    val partiesContact = nomContactLower.split(" ")

                    val correspond =
                        // ✅ Le nom recherché est contenu dans le nom du contact
                        partiesContact.any { partie -> partie.startsWith(nomLower) } ||
                                // ✅ Le nom du contact est contenu dans le nom recherché
                                nomLower.startsWith(partiesContact[0]) ||
                                // ✅ Similarité élevée (seuil augmenté à 0.85)
                                partiesContact.any { partie ->
                                    calculerSimilarite(nomLower, partie) >= 0.85
                                }

                    if (correspond) {
                        if (contacts.none { c -> c.nom == nom }) {
                            contacts.add(Contact(nom, tel.replace(" ", "")))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CONTACTS", "Erreur recherche : ${e.message}")
        }

        Log.d("CONTACTS", "🔍 '$nomRecherche' → ${contacts.size} contact(s)")
        return contacts
    }

    fun chercherContactsSimilaires(nomRecherche: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val nomLower = nomRecherche.lowercase().trim()

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val nom = it.getString(0) ?: continue
                    val tel = it.getString(1) ?: continue
                    val nomContactLower = nom.lowercase()
                    val partiesContact = nomContactLower.split(" ")

                    // ✅ Seuil de similarité à 0.7 pour les similaires
                    val estSimilaire = partiesContact.any { partie ->
                        calculerSimilarite(nomLower, partie) >= 0.7
                    }

                    if (estSimilaire && contacts.none { c -> c.nom == nom }) {
                        contacts.add(Contact(nom, tel.replace(" ", "")))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CONTACTS", "Erreur similaires : ${e.message}")
        }

        return contacts.take(5)
    }

    private fun calculerSimilarite(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val longueurMax = maxOf(s1.length, s2.length)
        val fenetre = maxOf(longueurMax / 2 - 1, 0)

        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)

        var correspondances = 0
        var transpositions = 0

        for (i in s1.indices) {
            val debut = maxOf(0, i - fenetre)
            val fin = minOf(i + fenetre + 1, s2.length)
            for (j in debut until fin) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                correspondances++
                break
            }
        }

        if (correspondances == 0) return 0.0

        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val jaro = (correspondances.toDouble() / s1.length +
                correspondances.toDouble() / s2.length +
                (correspondances - transpositions / 2.0) / correspondances) / 3.0

        var prefixe = 0
        for (i in 0 until minOf(4, minOf(s1.length, s2.length))) {
            if (s1[i] == s2[i]) prefixe++ else break
        }

        return jaro + prefixe * 0.1 * (1 - jaro)
    }

    fun extraireNom(commande: String): String {
        val t = commande.lowercase().trim()

        val motsCles = listOf(
            "whatsapp", "message", "sms", "email", "mail",
            "appelle", "appeler", "appel", "envoie", "envoyer",
            "dire", "dis", "un", "une", "le", "la", "les"
        )

        val patterns = listOf(
            Regex("(?:message|msg|whatsapp|sms)\\s+(?:[àa]|pour|de|vers)\\s+([\\w\\s]+)$"),
            Regex("(?:[àa]|pour)\\s+([a-zàâäéèêëîïôùûüç\\-]+(?:\\s+[a-zàâäéèêëîïôùûüç\\-]+)?)\\s*$"),
            Regex("(?:appelle?r?|téléphone\\s+[àa])\\s+([a-zàâäéèêëîïôùûüç\\-]+(?:\\s+[a-zàâäéèêëîïôùûüç\\-]+)?)"),
            Regex("^([a-zàâäéèêëîïôùûüç\\-]+(?:\\s+[a-zàâäéèêëîïôùûüç\\-]+)?)$")
        )

        for (pattern in patterns) {
            val match = pattern.find(t)
            if (match != null) {
                val nom = match.groupValues[1].trim()
                if (nom.isNotEmpty() && !motsCles.contains(nom)) {
                    return nom.split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                }
            }
        }

        return ""
    }
}