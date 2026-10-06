# 🏎️ Auto Analytics
**The Ultimate Android Companion for Your Vehicle Management with AI Integration**

---

## 🌟 Key Features

### 🤖 AI-Powered Intelligence
- **Smart Diagnosis (Virtual Mechanic)**: Persistent chat with an expert AI car mechanic powered by **Gemini AI**.
  - **History Persistence**: Conversations are saved per car and synced to the cloud.
  - **Context-Aware**: The AI knows your car's technical specs and history to provide precise advice.
- **AI Document Scanning**: Extract technical data from vehicle documents (registration certificates, etc.).
  - **Flexible Input**: Import documents via **Gallery** or **PDF** files.
  - **Manual Data Confirmation**: Review and select which scanned details (VIN, Make, Model, Year, etc.) to apply to your car profile.

### 🛠️ Comprehensive Vehicle Management
- **Exhaustive Technical Profiles**: Track everything from **Vehicle Generation**, engine layout, and power output to tire dimensions, safety equipment, and **VIN** (with a clean monospace surface container).
- **Advanced Search & Management**: Instantly find vehicles by **Make**, **Model**, **License Plate**, or **VIN**.
- **Bento-Style History Screens**: Redesigned history logs for **Service**, **Tires**, **Inspections**, **Insurance**, and **Vignettes** featuring real-time statistics cards and a modern list layout.
- **Smart Mileage History**: Central place to track vehicle odometer progress with interactive **Vico Charts** and intelligent log imports.
- **Maintenance, Fuel & Legal Tracking**: Keep detailed technical logs, track fuel efficiency (L/100km or MPG), and monitor deadlines for **Technical Inspections**, **Insurance**, and **Vignettes**.

### 🔐 Secure Access, Local Storage & Cloud Sync
- **Offline-First Local Caching & Firebase Firestore**: High-performance local file persistence with automated offline-first synchronization to **Firebase Firestore**.
- **Modern Authentication**: Sign in with Email/Password or **Google One Tap** (Credential Manager).
- **Cloud Backup**: Securely store your vehicle data, history, and AI logs in the cloud.

### ⚡ Performance & Automated Testing
- **Optimized Build & Sync**: Configured with Gradle Build Cache, Configuration Cache (`org.gradle.configuration-cache=true`), parallel execution (`org.gradle.parallel=true`), and Kotlin Daemon tuning for lightning-fast syncs and builds.
- **Branded Startup Experience**: Custom Jetpack SplashScreen API with a smooth branded loading state eliminating cold-start flickering.
- **Automated Test Suite**: Comprehensive unit tests (`CarFormattersTest`) and instrumented Compose UI tests (`AppFlowTest`).

### 🌗 Premium UI/UX
- **Material Design 3 (M3)**: Beautiful, adaptive interface with refined card layouts (top-right action buttons, sleek license plate badges, and styled VIN containers).
- **Global Unit System**: Full support for **Metric** (km, L) and **Imperial** (mi, gal) units.
- **Multi-Language Support**: Full localization for **English** and **Romanian**.

---

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | **Kotlin 2.0+** |
| **Compatibility** | **Android 8.0 (API 26) and up** |
| **UI Framework** | **Jetpack Compose** with **Material 3** |
| **AI SDK** | **Google Generative AI SDK** (Gemini) |
| **Local Storage** | **Offline-First Local CSV Caching** (`LocalStorageHelper`) |
| **Cloud Backend** | **Firebase** (Firestore, Auth, Storage, Cloud Messaging, Remote Config, Crashlytics) |
| **Networking** | **Ktor Client** |
| **Architecture** | **MVVM** + Clean Architecture + Hilt DI |
| **Charts** | **Vico Charts** |
| **Identity** | **Android Credential Manager** |
| **Testing** | **JUnit** & **Jetpack Compose UI Tests** |

---

## ⚖️ License

Copyright © 2026 **Darius Epure (Darius DevWorks)**

This project is licensed under the **GNU General Public License v3**.  
You are free to use, modify, and distribute this software under the terms of the GPL v3, ensuring that all derivative works remain open source under the same license.

---
*Developed by Darius DevWorks - Empowering drivers with data-driven vehicle maintenance.*
