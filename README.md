# 🏎️ Auto Analytics
**The Ultimate Android Companion for Your Vehicle Management with AI Integration**

---

## 🌟 Key Features

### 🤖 AI-Powered Intelligence
- **Smart Diagnosis**: Persistent chat with an expert AI car mechanic.
  - **History Persistence**: Conversations are saved per car and synced to the cloud.
  - **Context-Aware**: The AI knows your car's specs to provide precise advice.
  - **Function Calling**: The AI can perform actions like updating car specs or mileage directly through chat.
- **AI Document Scanning**: Extract technical data instantly from registration certificates.
  - **Dual Input**: Support for both **Photo** (Gallery) and **PDF** files.
  - **Direct Population**: Automatically fills fields like VIN, Make, Model, Year, Fuel Type, and more.

### 🛠️ Comprehensive Vehicle Management
- **Detailed Profiles**: Exhaustive technical specs for every vehicle, including Body, Chassis, Dimensions, and Engine details.
- **Visual Identity**: Modernized logo and support for car profile photos with smart compression.
- **Full History Tracking**:
  - **Service & Maintenance**: Log repairs and oil changes.
  - **Tire Management**: Track multiple sets (Summer/Winter/All-Season).
  - **Fuel Consumption**: Track fillings and view average consumption stats with charts.
  - **Legal Documents**: History and alerts for **ITP (Inspection)**, **Insurance**, and **Vignettes**.
- **Data Safety**: **Recycle Bin** for deleted vehicles to prevent accidental data loss.

### 🔐 Secure Access & Sync
- **Flexible Login**: Sign in with Email/Password, **Google One Tap**, or continue as **Guest**.
- **Cross-Device Sync**: Powered by Firebase for real-time data synchronization.
- **Credential Manager**: Seamless and secure sign-in experience using the latest Android APIs.

### 🌗 Premium UI/UX & Localization
- **Material Design 3 (M3)**: Modern, clean interface with updated typography and components.
- **Adaptive Theming**: Support for Light Mode and **OLED Black Dark Mode**.
- **Multi-Language Support**: In-app language selector (English, Romanian, and more).
- **Edge-to-Edge**: Immersive experience with content flowing behind system bars.
- **Developed by Darius DevWorks**: Consistent branding and footer across the authentication flow.

---

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | **Kotlin** (2.0+) |
| **Compatibility** | **Android 7.0 (API 24) and up** |
| **UI Framework** | **Jetpack Compose** with **Material 3** |
| **AI SDK** | **Google Generative AI SDK** (Gemini 3.5 Flash-lite) |
| **Architecture** | **MVVM** + Clean Architecture |
| **DI** | **Hilt** (Dagger) |
| **Networking** | **Ktor Client 3.0** |
| **Database** | **Cloud Firestore** |
| **Storage** | **Firebase Storage** |
| **Charts** | **Vico Charts** |
| **Identity** | **Android Credential Manager** |
| **Serialization** | **Kotlinx Serialization** |
| **Image Loading** | **Coil** |

---

## ⚙️ Configuration & Setup

### API Keys
To use the AI features, you must provide a Gemini API Key in your `local.properties` file:
```properties
gemini.api.key=YOUR_API_KEY_HERE
```

### Build & Run
The project uses Gradle Version Catalog for dependency management. Ensure you have the latest Android Studio installed.

---

> [!IMPORTANT]
> This app is designed to be **Offline-First**. All changes are saved locally and synced automatically when a connection is available.

---
*Developed by Darius DevWorks to provide total control over your vehicle's health and documentation.*
