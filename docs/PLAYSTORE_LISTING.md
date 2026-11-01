# edith AI — Google Play Store Listing & Submission Guide

## 1. App Information & Artifact
- **App Name:** edith AI
- **Tagline:** Your Intelligent Voice-First AI Companion
- **Package Name:** `com.edith.assistant`
- **Application ID:** `com.aistudio.edithai.kxvmpq`
- **Upload Artifact:** Android App Bundle (`.aab`) generated via Release build type with resource shrinking and minification.

---

## 2. Store Listing Copy

### Short Description (up to 80 chars)
Voice-first personal AI companion with real-time conversations & 3D Website Forge.

### Full Description
**Meet edith AI** — your real-time, voice-first intelligent companion designed for Android. 

Whether you speak in Hindi, Hinglish, or English, edith understands you naturally with ultra-low latency full-duplex conversations powered by Google Gemini Live. Interrupt, change topics, or ask deep questions seamlessly just like talking to a friend.

### Key Features:
- 🎙️ **Real-Time Voice Conversations:** Fluid, full-duplex speech with conversational barge-in. Powered by Google Gemini Live.
- 🌐 **Website Forge:** Simply say *"ek cafe ki website banao"* and watch a complete, animated modern 3D website materialize in real time.
- 🎭 **10 Selectable Personalities:** Choose between Friendly, Professional, Funny, Companion, Sweet, Teacher, Developer, Foodie, Nautanki, or Default.
- 🔮 **Stunning 3D Orb Visualizer:** GPU-shaded AGSL 3D orb that responds dynamically to your voice amplitude and device tilt.
- 📱 **Smart Phone Assistance:** Call contacts, open apps, prepare WhatsApp and SMS messages with voice commands.
- 💬 **Typed Chat Mode:** Chat with the same rich personality, word-by-word streaming, and text-to-speech replay.
- 🔒 **Privacy-First:** Your Gemini API key is stored locally on your device in private preferences. Never logged, never sent to third-party tracking servers.

---

## 3. Permission Justifications

| Permission | Justification |
| :--- | :--- |
| `RECORD_AUDIO` | Required exclusively when the user engages in a live voice conversation with the assistant. Audio is streamed directly over a secure WebSocket connection and is never saved or recorded locally. |
| `FOREGROUND_SERVICE_MICROPHONE` | Required to maintain continuous, hands-free voice conversations when the user navigates between apps or minimizes the screen. Displays an ongoing notification with a one-tap Stop action. |
| `POST_NOTIFICATIONS` | Required to display the ongoing voice session controller and alert the user when their Website Forge generation is ready. |
| `READ_CONTACTS` | Used solely when the user requests voice commands such as *"Call Mummy"* or *"WhatsApp Rahul"*. Never uploaded to any external server. |
| `CALL_PHONE` | Allows the user to directly place a telephone call to a requested contact or number without manual dialing. |

---

## 4. Google Play Data Safety Answers
- **Data Collected:**
  - **Voice / Audio recordings:** Transferred ephemerally over encrypted TLS for live conversational processing. Not stored. Not shared with third parties for advertising.
  - **Contacts:** Accessed on-device only for matching spoken names to phone numbers. Not collected, stored, or transmitted off-device.
- **Security Practices:**
  - Data encrypted in transit using industry-standard TLS / HTTPS / WSS.
  - No personal information sold or shared.

---

## 5. Submission Checklist
- [x] App icons configured across all mipmap densities (adaptive + round + non-adaptive).
- [x] Custom typography bundled locally (`res/font/instrument_serif.ttf` and `res/font/manrope.ttf`).
- [x] ProGuard rules configured for reflection and serialization safety.
- [x] MinifyEnabled and isShrinkResources enabled for release build.
- [x] 48dp minimum touch target compliance for all interactive elements.
- [x] Light and Dark theme contrast validated with Material 3 tokens.
- [x] Unit tests pass across core business logic, state machines, and forge parsers.
