# WingGo 🪽

**The mobile companion to [SWINGO](https://swingo-eta.vercel.app/).**

WingGo brings the virtual companion experience to Android with an animated flying mascot designed to make your phone feel a little more alive.

## ✨ Features

* 🪽 **Animated Mascot** — A cute flying companion with expressive animations.
* 🪟 **Floating Mode** — Keep WingGo visible as an overlay while using other apps.
* 🔔 **Notification Reactions** — Let WingGo react to incoming notifications.
* 👆 **Interactive Movement** — Tap, drag, and throw the companion around the screen.
* 🎨 **Custom Animations** — Smooth movement, blinking, wing flapping, and idle behavior.
* 🔐 **Authentication** — Google sign-in and guest access.
* ⚙️ **Native Android Integration** — Android overlay and notification functionality powered by Kotlin.

## 🛠️ Tech Stack

| Technology                   | Purpose                             |
| ---------------------------- | ----------------------------------- |
| React Native                 | Mobile application UI               |
| Expo                         | Development and build tooling       |
| TypeScript                   | Application logic and type safety   |
| Expo Router                  | Navigation                          |
| React Native Reanimated      | Animations                          |
| React Native Gesture Handler | Touch and drag interactions         |
| Clerk                        | Authentication                      |
| Kotlin                       | Native Android services and overlay |
| Android WindowManager        | Floating companion overlay          |

## 🪽 About the Project

WingGo is the Android-focused extension of the SWINGO companion concept. While SWINGO brings interactive desktop charms to Windows, WingGo explores a similar companion experience on mobile devices.

The goal is simple: **one little creature, living on your phone.**

## 🚀 Getting Started

### Prerequisites

* Node.js
* npm
* Android Studio and Android SDK
* Java Development Kit (JDK)
* An Android device or emulator

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/YOUR_USERNAME/winggo.git
   ```

2. Enter the project directory:

   ```bash
   cd winggo
   ```

3. Install dependencies:

   ```bash
   npm install
   ```

4. Configure your environment variables using a local `.env` file. Refer to `.env.example` if provided.

5. Start the Expo development server:

   ```bash
   npx expo start
   ```

### Android Development Build

To run the native Android project:

```bash
npx expo run:android
```

To generate a local release APK:

```bash
cd android
gradlew assembleRelease
```

The APK is typically generated at:

```text
android/app/build/outputs/apk/release/app-release.apk
```

**Note:** Floating mode and notification features require native Android integration and the relevant Android permissions. A standard Expo Go session may not support these features.

## 🔐 Security

* Never commit `.env` files containing secrets.
* Never commit `credentials.json`, signing keystores, or passwords.
* Keep authentication credentials and signing keys private.
* Configure your own Clerk environment variables before running the project.

## 📁 Project Structure

```text
winggo/
├── app/                 # Expo Router screens
├── assets/              # Images, mascot parts, and fonts
├── components/          # Reusable UI components
├── types/               # TypeScript types and image mappings
├── android/             # Native Android project and Kotlin integration
├── app.json             # Expo app configuration
├── eas.json             # EAS build profiles
├── package.json         # Dependencies and scripts
└── README.md
```

*The structure may evolve as development continues.*

## 🗺️ Roadmap

* [x] Core animated mascot concept
* [x] Native Android overlay integration
* [x] Notification listener integration
* [ ] Refine floating-mode reliability
* [ ] Improve animation polish and performance
* [ ] Prepare a stable public release

## 🤝 Contributing

Contributions, suggestions, and bug reports are welcome. Feel free to open an issue or submit a pull request.

## 👨‍💻 Created By

**Jones J.** and contributors

Built with curiosity, creativity, and a little bit of code. 💙

---

<p align="center">
  <i>One little creature. A whole new mobile experience.</i>
</p>
