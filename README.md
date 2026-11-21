# TV Remote — redesigned Android prototype

A dark, touch-friendly remote interface based on the supplied Wanbo remote, with a circular D-pad, device selector, scan dialog, and clear controls. The previous thin-bar UI has been replaced.

## Status

This is **not a functioning universal remote yet**. Wi-Fi NSD discovery looks for `_androidtvremote2._tcp.`, `_androidtvremote._tcp.`, and `_googlecast._tcp.` advertisements. It does not authenticate, pair, or transmit remote commands. Cast discovery alone does not imply remote compatibility. Selecting a device only saves its name. The Wanbo WPC024's support is unverified.

## GitHub build

Upload the **contents** of this project to the root of your GitHub repository, including `.github/workflows/build-apk.yml`. Run Actions → Build TV Remote APK → Run workflow. Download the APK artifact from a successful run.

The project has not been compiled locally in this environment; use the GitHub Actions run to verify the build.
