# WindowBridge

WindowBridge is an Android app for managing Windows installer files, with a longer-term goal of running compatible Windows programs through a Windows compatibility runtime.

## Current features (v0.2.0)

- Dark navy interface and private installer library.
- Import `.exe` and `.msi` files using Android's document picker.
- Keep a private app-storage copy of each imported installer.
- View installer file sizes and details; delete saved files.
- Securely share saved installer files with other apps using Android's FileProvider and temporary read permissions.
- Open the official Winlator releases page from the runtime setup button.
- GitHub Actions builds a debug APK and uploads the `windowbridge-debug-apk` artifact.

## Runtime limitation

**WindowBridge does not yet run Windows applications itself.** This version can share an installer with another app, but the other app must explicitly support receiving that file. The runtime setup button opens the official Winlator releases page; it does not silently install Winlator or embed its runtime into WindowBridge.

Running Windows software inside WindowBridge requires integrating and testing a compatible runtime such as Wine plus CPU and graphics translation components. This is a substantial native integration, and compatibility varies by Android device and Windows program.

Official Winlator source and releases:
- https://github.com/brunodev85/winlator
- https://github.com/brunodev85/winlator/releases

A future home-screen shortcut would launch a compatible program through the runtime; it would not convert the Windows program into a native Android app.

## Install the debug APK

1. Open the repository's **Actions** tab.
2. Open the latest successful **Android APK** workflow run.
3. Download the `windowbridge-debug-apk` artifact and extract the ZIP.
4. Install the APK on your Android phone. If prompted, allow installation from the app used to open the APK.

This is a debug build for testing, not a Play Store release.
