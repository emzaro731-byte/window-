# WindowBridge

WindowBridge is an Android project intended to provide a Winlator-style Windows application manager with two launch modes:

1. Run compatible Windows applications inside the WindowBridge app.
2. Offer Android home-screen shortcuts for apps managed by the compatibility environment, where Android launcher and runtime integration permit it.

## Important technical reality

An APK cannot run a Windows `.exe` natively. The app needs a compatible runtime such as Wine plus suitable CPU and graphics translation components. Compatibility is not universal. A home-screen shortcut can launch a managed program through the runtime; it does not turn the Windows program into a native Android app.

## Planned milestones

- [x] Android project foundation and APK CI
- [ ] Handle shared/opened `.exe` and `.msi` files through Android's file-opening flow
- [ ] Build a Windows installer wizard and program library
- [ ] Integrate a legally distributable, Android-compatible Wine/runtime stack
- [ ] Launch compatible programs inside the app
- [ ] Create home-screen shortcuts for managed programs
- [ ] Test CPU ABI, graphics, storage permissions, and runtime compatibility

## Build

Open this repository in Android Studio and build the `app` module. GitHub Actions is configured to build a debug APK when changes are pushed to `main`.

The initial app is a project foundation, not yet a functioning Windows runtime. Runtime integration is a separate, substantial engineering step.
