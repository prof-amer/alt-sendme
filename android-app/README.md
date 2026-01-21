# AltSendme Android App

A native Android app for peer-to-peer file transfer using encrypted connections. This app mirrors the functionality and design of the AltSendme desktop application.

## Features

- **Send Files**: Share files via encrypted P2P connections with ticket-based sharing
- **Receive Files**: Download files using tickets from senders
- **Progress Tracking**: Real-time transfer progress with speed and ETA
- **Dark Theme**: Consistent design matching the desktop app
- **Background Transfers**: Continue transfers when the app is in the background

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34 (Android 14)
- Minimum SDK 26 (Android 8.0)
- JDK 17

## Building the App

### Using Android Studio

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the `android-app` directory
4. Wait for Gradle sync to complete
5. Click "Run" or press Shift+F10

### Using Command Line

```bash
# Navigate to the android-app directory
cd android-app

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

The built APK will be located at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
android-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/altsendme/app/
│   │   │   ├── ui/
│   │   │   │   ├── components/     # Reusable UI components
│   │   │   │   ├── screens/        # Send and Receive screens
│   │   │   │   └── theme/          # Color and typography definitions
│   │   │   ├── viewmodel/          # ViewModels for state management
│   │   │   ├── sendme/             # P2P transfer library interface
│   │   │   ├── service/            # Background transfer service
│   │   │   ├── MainActivity.kt
│   │   │   └── AltSendmeApplication.kt
│   │   └── res/
│   │       ├── values/             # Strings, colors, themes
│   │       ├── drawable/           # Icons and graphics
│   │       └── xml/                # Configuration files
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml          # Version catalog
├── build.gradle.kts
└── settings.gradle.kts
```

## Architecture

The app follows modern Android development practices:

- **Jetpack Compose**: Declarative UI framework
- **MVVM Pattern**: ViewModels for state management
- **Kotlin Coroutines**: Asynchronous operations
- **Material Design 3**: Modern UI components

## Integrating Iroh P2P Library

The current implementation includes a mock `SendmeLibrary` that simulates P2P transfers. To integrate the actual Iroh library:

1. Build the `sendme` Rust library with UniFFI bindings for Android:
   ```bash
   cargo build --target aarch64-linux-android --release
   cargo build --target armv7-linux-androideabi --release
   cargo build --target x86_64-linux-android --release
   ```

2. Generate Kotlin bindings using uniffi-bindgen:
   ```bash
   uniffi-bindgen-kotlin sendme.udl --out-dir android-app/app/src/main/java
   ```

3. Add the native libraries to `app/src/main/jniLibs/`:
   ```
   jniLibs/
   ├── arm64-v8a/libsendme.so
   ├── armeabi-v7a/libsendme.so
   └── x86_64/libsendme.so
   ```

4. Update `SendmeLibrary.kt` to use the generated bindings

## Design System

The app uses a dark theme matching the desktop application:

| Color | Hex | Usage |
|-------|-----|-------|
| Background | `#191919` | App background |
| Primary | `#25D365` | Action buttons, active states |
| Accent | `#2D78DC` | Secondary actions, completed states |
| Destructive | `#903C3C` | Stop/cancel buttons |
| Text Primary | `#FFFFFF` | Main text |
| Text Muted | `#99FFFFFF` | Secondary text |

## Permissions

The app requires the following permissions:

- `INTERNET`: For P2P network connections
- `ACCESS_NETWORK_STATE`: To check network availability
- `READ_EXTERNAL_STORAGE`: To read files for sharing (Android 12 and below)
- `READ_MEDIA_*`: To read media files (Android 13+)
- `FOREGROUND_SERVICE`: For background transfers
- `POST_NOTIFICATIONS`: For transfer progress notifications

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the AGPL-3.0 License - see the LICENSE file in the root directory for details.
