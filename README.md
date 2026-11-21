# TV Remote (Android)

## Status
This is an installable **discovery and UI prototype**, not a functioning universal remote. It can discover advertised Android TV Remote and Google Cast mDNS services on the same Wi-Fi network and remember a selected service. It **cannot yet pair or send control commands**. Do not confuse service discovery with working pairing.

The Wanbo WPC024's protocol is unverified. Functional pairing requires implementation of the authenticated Android TV Remote v2 protocol or the actual projector's supported protocol. Netflix/YouTube buttons are placeholders.

## GitHub mobile build
Upload the *contents* of this folder to the repository root (including `.github/workflows/build-apk.yml`). In GitHub Actions, run **Build TV Remote APK**. Download artifact **TV-Remote-debug-APK**, extract it, and install `app-debug.apk`.

Uses Android SDK 35, Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17. No Gradle wrapper is included; GitHub Actions installs Gradle 8.9.
