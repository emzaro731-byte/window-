# WindowBridge

WindowBridge is an Android project for managing Windows installer files on Android, with a longer-term goal of running compatible Windows programs through a separate compatibility runtime.

## Current features

- Dark navy WindowBridge interface.
- Import `.exe` and `.msi` files from Android's document picker.
- Copy imported installers into the app's private storage, so the library persists after closing the app.
- View installer details and file size.
- Delete saved installers.
- Receive selected files from compatible Android file-opening intents where the sender supplies a content URI.
- GitHub Actions builds a debug APK and uploads it as the `windowbridge-debug-apk` artifact.

## Important limitation

**This build does not execute or install Windows software yet.** An Android APK cannot run Windows executables natively. Real execution requires a compatible runtime such as Wine plus appropriate CPU and graphics translation components. Runtime integration, licensing, device ABI support, and graphics compatibility still need engineering and testing. Not every Windows application will work.

A future home-screen shortcut would launch a compatible program through that runtime; it would not convert the Windows program into a native Android app.

## Build and install

1. Open the repository's **Actions** tab.
2. Open the latest successful Android APK workflow run.
3. Download the `windowbridge-debug-apk` artifact and extract the ZIP.
4. Install the APK on your Android phone. Android may ask you to allow installation from the app you used to open the APK.

This is a debug build for testing, not a Play Store release.
