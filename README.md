# Alexandra – AI Voice Assistant

![Alexandra Banner](https://capsule-render.vercel.app/api?type=waving&color=0:0D1117,100:2E4057&height=200&section=header&text=Alexandra%20AI%20Voice%20Assistant&fontSize=40&fontColor=FFFFFF&animation=fadeIn)

> Un assistant vocal intelligent développé avec Kotlin, Java et l'API OpenAI.
> Projet de Fin d'Études (PFE) – 2026

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://www.java.com)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat&logo=firebase&logoColor=black)](https://firebase.google.com)
[![OpenAI](https://img.shields.io/badge/OpenAI_API-412991?style=flat&logo=openai&logoColor=white)](https://openai.com)

---

## Table des matières

- [Aperçu](#aperçu)
- [Démo](#démo)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Défis techniques et apprentissages](#défis-techniques-et-apprentissages)
- [Technologies](#technologies)
- [Structure du projet](#structure-du-projet)
- [Installation](#installation)
- [Roadmap](#roadmap)
- [Auteur](#auteur)
- [Licence](#licence)

---

## Aperçu

**Alexandra** est un assistant vocal intelligent conçu pour simplifier les tâches du quotidien : reconnaissance vocale, conversation avec une IA, rappels et notifications, le tout dans une application mobile accompagnée d'un tableau de bord web pour la gestion des utilisateurs.

Ce projet a été pensé comme une application complète de bout en bout : app mobile (Kotlin/Java), backend/authentification (Firebase), intelligence conversationnelle (API OpenAI) et interface d'administration (HTML/CSS/JS).

---

## Démo

<!-- Remplacez ce placeholder par un GIF ou une courte vidéo montrant une interaction vocale réelle -->
<!-- Un gif vaut mille captures d'écran statiques pour une app de reconnaissance vocale -->

**[À ajouter : GIF de 10-15 secondes montrant "je parle → Alexandra répond"]**

### Captures d'écran — Application mobile

| Accueil | Historique | Profil |
|---------|------------|--------|
| <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/home.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/histories.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/profile.jpeg" width="250"> |

| Connexion | Inscription | Permissions |
|-----------|-------------|-------------|
| <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/login.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/register.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/permissions.jpeg" width="250"> |

### Captures d'écran — Tableau de bord web

| Dashboard admin | Dashboard utilisateur | Connexion |
|-----------------|------------------------|-----------|
| <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/dashboard/admin.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/dashboard/user.png" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/dashboard/login.jpeg" width="250"> |

> Note : les chemins d'images ont été uniformisés (`dashboard/` sans espace ni majuscule) — vérifiez que les dossiers réels du dépôt correspondent.

---

## Fonctionnalités

### Application mobile
- **Reconnaissance vocale** — interaction naturelle par la voix
- **Chat IA** — réponses intelligentes générées via l'API OpenAI
- **Rappels et tâches** — pour ne rien oublier
- **Notifications intelligentes** — alertes au bon moment
- **Profil utilisateur** — expérience personnalisée

### Tableau de bord web
- **Gestion des utilisateurs** — ajout, modification, suppression
- **Statistiques et analyses** — suivi de l'utilisation de l'app
- **Connexion sécurisée** — accès administrateur protégé
- **Journal d'activité** — traçabilité des actions utilisateurs

---

## Architecture

```
   🎙️ Voix utilisateur
        │
        ▼
 SpeechRecognizer (Android)
        │
        ▼
   Texte transcrit
        │
        ▼
   Appel API OpenAI  ──────► Firebase (auth + historique)
        │
        ▼
   Réponse générée
        │
        ▼
   Synthèse vocale (TTS) + affichage dans l'app
```

<!-- Remplacez ce schéma par le vôtre s'il diffère de l'implémentation réelle -->
<!-- Précisez ici : bibliothèque de reconnaissance vocale utilisée, gestion de l'asynchronisme (coroutines ?), format d'échange avec l'API -->

---

## Défis techniques et apprentissages

<!--
Cette section est la plus importante pour un recruteur ou un jury de PFE.
Remplacez les exemples ci-dessous par vos propres défis réels. Soyez précis et honnête :
le but n'est pas de paraître parfait, mais de montrer une réflexion d'ingénieur.
-->

- **Synchronisation temps réel** : faire cohabiter la reconnaissance vocale continue avec les appels réseau vers l'API OpenAI sans bloquer l'interface utilisateur. *(Précisez votre solution : coroutines Kotlin, threads, callbacks ?)*
- **Gestion de la latence de l'API** : afficher un retour visuel pendant l'attente de la réponse IA pour éviter que l'utilisateur pense l'app figée.
- **Cohérence Kotlin/Java** : le projet mélange les deux langages — *expliquez pourquoi (ex. modules hérités en Java, nouveau code en Kotlin) ou indiquez un plan de migration complète vers Kotlin.*
- **Sécurité des clés API** : la clé OpenAI et le fichier `google-services.json` ne doivent jamais être commit dans le dépôt — *précisez comment vous les gérez (variables d'environnement, `local.properties`, etc.)*
- **Ce que j'ai appris** : *(1-2 phrases sur une compétence acquise — architecture Android, gestion d'API tierces, Firebase, etc.)*

---

## Technologies

| Technologie | Usage |
|-------------|-------|
| Kotlin | Développement principal de l'application |
| Java | Logique backend / modules existants |
| API OpenAI | Génération des réponses conversationnelles |
| Firebase | Base de données et authentification |
| HTML/CSS | Pages du tableau de bord |
| JavaScript | Interactions du tableau de bord |

---

## Structure du projet

```
Alexandra-AI-Voice-Assistante/
│
├── android_studio_codes/
│   ├── app/
│   │   └── src/main/
│   │       ├── java/
│   │       │   └── views/
│   │       ├── res/
│   │       │   ├── color/
│   │       │   ├── drawable/
│   │       │   ├── layout/
│   │       │   ├── menu/
│   │       │   └── values/
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
│
├── dashboard_pages/
│   ├── dashboard-admin.html
│   ├── dashboard-user.html
│   └── login.html
│
├── screenshots/
│   ├── app/
│   └── dashboard/
│
├── README.md
└── LICENSE
```

---

## Installation

### Prérequis
- Android Studio (dernière version)
- Java 11 ou supérieur
- Compte Firebase
- Clé API OpenAI

### Étapes

1. Cloner le dépôt
   ```bash
   git clone https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante.git
   ```

2. Ouvrir `android_studio_codes/` dans Android Studio

3. Configurer Firebase :
   - Aller sur [Firebase Console](https://console.firebase.google.com)
   - Créer un nouveau projet (ou ouvrir un projet existant)
   - Cliquer sur **"Ajouter une application"** → Android
   - Renseigner le nom du package
   - Télécharger `google-services.json`
   - Placer le fichier dans `/android_studio_codes/app`

   > ⚠️ Ce fichier est personnel et sensible — ne jamais le committer ni le partager. Ajoutez-le à votre `.gitignore`.

4. Configurer la clé API OpenAI :
   - Ne pas l'écrire en dur dans le code source
   - La placer dans un fichier `local.properties` (non versionné) ou une variable d'environnement, puis la lire depuis `BuildConfig`

5. Compiler et lancer l'application

---

## Roadmap

- [ ] Ajouter des tests unitaires (JUnit) sur les fonctions clés de reconnaissance vocale
- [ ] Ajouter un pipeline CI (GitHub Actions) pour la compilation automatique
- [ ] Support multilingue de la reconnaissance vocale
- [ ] Mode hors-ligne partiel pour les rappels

---

## Auteur

- GitHub : [@Nexus-Vertex](https://github.com/Nexus-Vertex)
- Contact : via [LinkedIn](#) <!-- Remplacez par votre lien LinkedIn plutôt que l'email en clair -->

---

## Licence

Ce projet est distribué sous licence MIT — voir le fichier [LICENSE](LICENSE) pour plus de détails.

---

⭐ Si ce projet vous plaît, n'hésitez pas à lui laisser une étoile !
