# 🏎️ Auto Analytics
**The Ultimate Android Companion for Your Vehicle Management with AI Integration**

---

## 🌟 Key Features

### 🤖 AI-Powered Intelligence
- **Smart Diagnosis (Virtual Mechanic)**: Persistent chat with an expert AI car mechanic.
  - **History Persistence**: Conversations are saved per car and synced to the cloud.
  - **Context-Aware**: The AI knows your car's technical specs and history to provide precise advice.
  - **Function Calling**: The AI can perform actions like updating car specs or odometer readings directly through the conversation.
- **AI Document Scanning**: Extract technical data instantly from vehicle documents.
  - **Dual Input**: Support for both **Photo** (Gallery/Camera) and **PDF** files.
  - **Auto-Population**: Automatically fills exhaustive fields like VIN, Make, Model, Year, Fuel Type, and more using Google Gemini.

### 🛠️ Comprehensive Vehicle Management
- **Exhaustive Technical Profiles**: Track everything from engine layout and cylinder configuration to tire dimensions and brake types.
- **Smart Mileage History**: 
  - **Unified Log**: A central place to track your vehicle's odometer progress.
  - **Intelligent Import**: One-tap import of mileage records from **Fuel**, **Service**, and **Technical Inspection (ITP)** logs.
- **Maintenance & Service**: Keep a detailed technical log of every repair, oil change, or part replacement.
- **Tire Management**: Track active and stored tire sets with DOT and size specifications.
- **Fuel Consumption**: Log fillings, track efficiency, and visualize trends with interactive **Vico Charts**.
- **Legal Document Tracking**: Stay ahead of deadlines with history and expiration alerts for **ITP**, **Insurance (RCA)**, and **Vignettes**.

### 🔐 Secure Access & Sync
- **Modern Authentication**: Sign in with Email/Password, **Google One Tap**, or explore as a **Guest**.
- **Seamless Sync**: Powered by Firebase for real-time data synchronization across all your devices.
- **Offline-First**: Built with a robust local-first strategy; data stays on your device and syncs whenever you're online.
- **Recycle Bin**: Safety first—deleted vehicles can be recovered from the bin to prevent accidental data loss.

### 🌗 Premium UI/UX
- **Material Design 3 (M3)**: A beautiful, modern interface using the latest adaptive components.
- **Adaptive Theming**: Native support for Light Mode and a deep **OLED Black Dark Mode**.
- **Multi-Language Support**: Full localization for **English** and **Romanian**.
- **Edge-to-Edge**: Immersive experience with content flowing elegantly behind system bars.

---

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | **Kotlin 2.0+** |
| **UI Framework** | **Jetpack Compose** with **Material 3** |
| **AI SDK** | **Google Generative AI SDK** (Gemini 3.5 Flash-lite) |
| **Networking** | **Ktor Client 3.0** (with Content Negotiation & Logging) |
| **Backend** | **Firebase** (Firestore, Storage, Authentication) |
| **Architecture** | **MVVM** + Clean Architecture + Hilt DI |
| **Charts** | **Vico Charts** |
| **Identity** | **Android Credential Manager** |
| **Serialization** | **Kotlinx Serialization** |
| **Image Loading** | **Coil** |

---

## ⚖️ License

Copyright © 2026 **Darius Epure (Darius DevWorks)**

This project is licensed under the **GNU General Public License v3**.  
You are free to use, modify, and distribute this software under the terms of the GPL v3, ensuring that all derivative works remain open source under the same license.

---

## ⚙️ Configuration

### API Keys
To enable AI features, add your Gemini API Key to `local.properties`:
```properties
gemini.api.key=YOUR_API_KEY_HERE
```

---
*Developed by Darius DevWorks - Empowering drivers with data-driven vehicle maintenance.*
