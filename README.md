# TV Remote — Bluetooth HID prototype

Bluetooth only: uses Android BluetoothHidDevice to register as a keyboard/consumer-control HID accessory. No Wi-Fi, IR, or network discovery.

## Pairing
1. Install the GitHub Actions debug APK and grant Nearby Devices permission.
2. Tap PAIR and Make Discoverable.
3. On Wanbo WPC024: Bluetooth → Search for accessories; select the phone and approve pairing.
4. If necessary tap PAIR → Reconnect paired. Try arrows/OK.

**Compatibility warning:** Android may not support HID Device on this phone, and the projector may reject generic keyboards. The HID app registration and connection are not guaranteed. Power, Home, volume, and streaming shortcuts may not be recognized by the projector. App shortcuts are intentionally not sent as arbitrary keycodes. Android may unregister HID when the app is backgrounded.

## GitHub build
Upload project contents to repository root, run Actions → Build TV Remote APK, download the artifact. Android SDK 35, Gradle 8.9, Java 17.

No actual-device test or APK compilation was performed in this environment.
