# Wanbo WPC024 Remote — Android starter project

**Status: prototype, NOT a functional remote.**

Based on the user's remote photograph and projector About screen (WPC024, firmware wanbo-WPC024-XWM E-V1.0.3-20250824). Contains a dark button interface and a diagnostic probe for selected TCP ports on a manually entered projector IP. No remote commands are transmitted.

## Build
Open this directory in Android Studio with Android SDK 35 and Gradle-compatible JDK 17. Let Android Studio sync the project, then Build → Build APK(s). This environment did not have the Android SDK/Gradle, so an APK could not be built or tested here.

## What is needed to make it functional
Identify whether WPC024 exposes Android TV Remote Service (typically TLS ports 6466/6467), another vendor Wi-Fi service, or only Bluetooth/IR. Implement authenticated pairing and the correct command protocol. Do not assume a port scan proves compatibility. Power-on may require IR or network wake support.
