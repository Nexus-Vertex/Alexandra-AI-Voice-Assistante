# Alexandra – AI Voice Assistant

![Alexandra Banner](https://capsule-render.vercel.app/api?type=waving&color=0:0D1117,100:2E4057&height=200&section=header&text=Alexandra%20AI%20Voice%20Assistant&fontSize=40&fontColor=FFFFFF&animation=fadeIn)

> An intelligent voice assistant built with Kotlin, Java, and the OpenAI API.
> Final Year Project (PFE) – 2026

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://www.java.com)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat&logo=firebase&logoColor=black)](https://firebase.google.com)
[![OpenAI](https://img.shields.io/badge/OpenAI_API-412991?style=flat&logo=openai&logoColor=white)](https://openai.com)

---

## Table of Contents

- [Overview](#overview)
- [Demo](#demo)
- [Features](#features)
- [Architecture](#architecture)
- [Technical Challenges and Learnings](#technical-challenges-and-learnings)
- [Technologies](#technologies)
- [Project Structure](#project-structure)
- [Author](#author)
- [License](#license)

---

## Overview

**Alexandra** is an intelligent voice assistant designed to simplify everyday tasks: voice recognition, conversation with an AI, reminders and notifications, all within a mobile application paired with a web dashboard for user management.

This project was designed as a complete end-to-end application: mobile app (Kotlin/Java), backend/authentication (Firebase), conversational intelligence (OpenAI API), and an admin interface (HTML/CSS/JS).

---

## Demo

### Screenshots — Mobile Application

| Home | History | Profile |
|---------|------------|--------|
| <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/home.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/histories.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/profile.jpeg" width="250"> |

| Login | Sign up | Permissions |
|-----------|-------------|-------------|
| <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/login.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/register.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/App/permissions.jpeg" width="250"> |

### Screenshots — Web Dashboard

| Admin dashboard | User dashboard | Login |
|-----------------|------------------------|-----------|
| <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/%20dashboard/admin.jpeg" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/%20dashboard/user.png" width="250"> | <img src="https://github.com/Nexus-Vertex/Alexandra-AI-Voice-Assistante/blob/main/screenshots/%20dashboard/login.jpeg" width="250"> |

---

## Features

### Mobile Application
- **Voice recognition** — natural interaction through speech
- **AI Chat** — intelligent responses generated via the OpenAI API
- **Reminders and tasks** — never forget anything
- **Smart notifications** — alerts at the right moment
- **User profile** — personalized experience

### Web Dashboard
- **User management** — add, edit, delete
- **Statistics and analytics** — app usage tracking
- **Secure login** — protected admin access
- **Activity log** — traceability of user actions

---

## Architecture

```
   🎙️ User voice
        │
        ▼
 SpeechRecognizer (Android)
        │
        ▼
   Transcribed text
        │
        ▼
   OpenAI API call  ──────► Firebase (auth + history)
        │
        ▼
   Generated response
        │
        ▼
   Text-to-speech (TTS) + display in the app
```

---

## Technical Challenges and Learnings

- **Real-time synchronization**: making continuous voice recognition work alongside network calls to the OpenAI API without blocking the user interface.
- **Handling API latency**: displaying visual feedback while waiting for the AI's response so the user doesn't think the app has frozen.
- **Kotlin/Java consistency**: managing the coexistence of both languages within the project, between legacy modules and newer development.
- **API key security**: the OpenAI key and the `google-services.json` file are excluded from the repository and managed locally to prevent any leaks.

---

## Technologies

| Technology | Usage |
|-------------|-------|
| Kotlin | Main application development |
| Java | Backend logic / legacy modules |
| OpenAI API | Generating conversational responses |
| Firebase | Database and authentication |
| HTML/CSS | Dashboard pages |
| JavaScript | Dashboard interactions |

---

## Project Structure

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

## Author

- GitHub: [@Nexus-Vertex](https://github.com/Nexus-Vertex)
- Contact: via [LinkedIn](#)

---

## License

This project is distributed under the MIT License – see the [LICENSE](LICENSE) file for more details.

---

⭐ If you like this project, feel free to leave it a star!
