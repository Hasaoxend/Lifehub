# 🌟 LifeHub Ecosystem

LifeHub is a comprehensive life management system designed to sync your digital life across all platforms. Securely manage passwords, tasks, notes, and schedules with a unified Firebase backend.

## 📱 Multi-Platform Support
- **Android App:** Native experience with biometric security and advanced productivity tools.
- **Web Platform:** Modern dashboard built with React/Vite for desktop management.
- **Browser Extension:** Quick access to passwords, TOTP codes, and calendar events directly in your browser.

---

## 🚀 Key Features

### 🔐 Security & Identity
- **AES-256 Encryption:** All sensitive data is encrypted locally before syncing.
- **Biometric Auth:** Secure login via Fingerprint/Face ID on mobile.
- **Autofill Service:** Experience seamless logins on Android and Web via browser extensions.
- **2FA / Authenticator:** Built-in TOTP support for multi-factor authentication.

### 📅 Productivity & Organization
- **Smart Calendar:** Multi-platform event syncing with interactive day views.
- **Task Management:** Categorize tasks into projects and sub-projects.
- **Secure Notes:** Encrypted notes with reminder support.
- **Real-time Sync:** Powered by Firebase Firestore for instant updates across devices.

---

## 🏗️ Technical Stack

| Component | Stack | Core Libraries |
|-----------|-------|----------------|
| **Android** | Java / Android SDK | Hilt, Retrofit, Firebase, Material 3 |
| **Web** | React / Vite | TypeScript, Firebase SDK, Tailwind CSS |
| **Extension**| Vanilla JS / Manifest v3 | Firebase SDK, Web Components |

---

## 🛠️ Setup Instructions

### 1. General Security (Git Readiness)
To prevent leaking sensitive data, this project uses a template-based config approach:
- **Android:** Copy `google-services.json.template` to `google-services.json`.
- **Web:** Copy `.env.example` to `.env`.
- **Extension:** Copy `extension/popup/libs/firebase-config.example.js` to `firebase-config.js`.

### 2. Android App
1. Place your `google-services.json` in the `app/` directory.
2. Configure `local.properties` with your OpenWeatherMap API key:
   ```properties
   OPENWEATHER_API_KEY=YOUR_KEY_HERE
   ```

### 3. Web Platform
1. Navigate to `web/`.
2. Install dependencies: `npm install`.
3. Fill your Firebase config in `.env`.
4. Run locally: `npm run dev`.

### 4. Browser Extension
1. Update `extension/popup/libs/firebase-config.js` with your Firebase credentials.
2. Open Chrome/Brave `Settings > Extensions`.
3. Enable "Developer mode".
4. Click "Load unpacked" and select the `extension/` folder.

---

## 🛡️ Security Audit Note
This repository has been audited for security. All API keys and private configurations are excluded via `.gitignore`. 
**If you are contributing**, please ensure you never commit `local.properties`, `.env`, or any file ending in `.jks` or `.json`.
