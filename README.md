# WindowBridge

WindowBridge is an Android project for managing Windows installer files and developing a standalone Windows compatibility layer.

## Current version: v0.6.0 (development)

- Dark navy Android interface.
- Import and privately store Windows `.exe` and `.msi` files.
- View, share, delete, and create home-screen shortcuts for saved installers.
- Choose a compatibility profile: Auto, Lightweight, or PC & games.
- GitHub Actions builds a debug APK artifact named `windowbridge-debug-apk`.

## Important runtime status

**This version does not yet install or run Windows programs directly.** The compatibility profiles save a preference only. They do not install a runtime. WindowBridge does not currently bundle Wine, Box64/other CPU translation, graphics drivers/components, or a working installer/launcher adapter. The “Install / Run Windows App” button reports this limitation instead of falsely claiming to execute an installer.

Android cannot execute Windows `.exe` files natively. To operate without a separate Winlator installation, this project needs a tested, legally distributable runtime integration, device-architecture handling, runtime storage management, graphics/audio components, and a real install-and-launch adapter. These components are substantial native software and must be built and tested together. Compatibility will vary by app and phone; demanding PC games may not run.

The optional Winlator sharing action remains available as a separate fallback, but WindowBridge does not require it for the future standalone-engine design.

## Development roadmap

1. Integrate a standalone runtime with compatible licenses and ARM64 Android support.
2. Add runtime download/package verification and safe extraction, or bundle required components where licensing permits.
3. Implement per-app prefixes/containers and installation/launch tracking.
4. Connect the Auto / Lightweight / PC & games profiles to real runtime settings.
5. Test on target devices and report unsupported programs clearly.

Upstream runtime research:
- Winlator source: https://github.com/brunodev85/winlator
- Wine project: https://www.winehq.org/
- Box64 project: https://github.com/ptitSeb/box64

## Install the debug APK

1. Open this repository's **Actions** tab.
2. Open the latest successful **Android APK** workflow run.
3. Download the `windowbridge-debug-apk` artifact and extract the ZIP.
4. Install the APK on your Android phone. If prompted, allow installation from the app used to open the APK.

This is a debug build for testing, not a Play Store release.
