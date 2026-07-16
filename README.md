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
- [Auteur](#auteur)
- [Licence](#licence)

---

## Aperçu

**Alexandra** est un assistant vocal intelligent conçu pour simplifier les tâches du quotidien : reconnaissance vocale, conversation avec une IA, rappels et notifications, le tout dans une application mobile accompagnée d'un tableau de bord web pour la gestion des utilisateurs.

Ce projet a été pensé comme une application complète de bout en bout : app mobile (Kotlin/Java), backend/authentification (Firebase), intelligence conversationnelle (API OpenAI) et interface d'administration (HTML/CSS/JS).

---

## Démo

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

---

## Défis techniques et apprentissages

- **Synchronisation temps réel** : faire cohabiter la reconnaissance vocale continue avec les appels réseau vers l'API OpenAI sans bloquer l'interface utilisateur.
- **Gestion de la latence de l'API** : affichage d'un retour visuel pendant l'attente de la réponse IA pour éviter que l'utilisateur pense l'application figée.
- **Cohérence Kotlin/Java** : cohabitation des deux langages au sein du projet, entre modules historiques et nouveaux développements.
- **Sécurité des clés API** : la clé OpenAI et le fichier `google-services.json` sont exclus du dépôt et gérés localement pour éviter toute fuite.

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

## Auteur

- GitHub : [@Nexus-Vertex](https://github.com/Nexus-Vertex)
- Contact : via [LinkedIn](#)

---

## Licence

Ce projet est distribué sous licence MIT – voir le fichier [LICENSE](LICENSE) pour plus de détails.

---

⭐ Si ce projet vous plaît, n'hésitez pas à lui laisser une étoile !
