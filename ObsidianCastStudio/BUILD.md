# Panduan Build & Konfigurasi Dependency — ObsidianCast Studio

## 1. Persyaratan Build

- **JDK**: Java 17 (Eclipse Temurin 17 direkomendasikan)
- **Android Gradle Plugin (AGP)**: `8.5.2`
- **Kotlin**: `1.9.24`
- **Gradle**: `8.7`
- **Compile SDK / Target SDK**: `34` (Android 14)
- **Min SDK**: `26` (Android 8.0 Oreo)

---

## 2. Daftar Dependency Stabil (`app/build.gradle.kts`)

| Kategori | Dependency Artifact | Versi | Fungsi |
| :--- | :--- | :--- | :--- |
| **UI & Material 3** | `androidx.compose:compose-bom` | `2024.06.00` | Dark Glassmorphism Broadcast UI |
| **Camera Internal** | `androidx.camera:camera-camera2` | `1.3.4` | CameraX + Camera2Interop manual sensor |
| **IP Camera (RTSP)** | `androidx.media3:media3-exoplayer-rtsp` | `1.4.0` | Hardware RTSP stream decoding |
| **RTMP/RTMPS Engine** | `com.github.pedroSG94.RootEncoder:library` | `2.4.5` | Hardware MediaCodec RTMP/RTMPS streaming |
| **Async Processing** | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.8.1` | Non-blocking transitions & audio mixing |

---

## 3. Perintah Build Lokal (CLI)

```bash
# Masuk ke direktori project Android
cd android-studio-project

# 1. Jalankan Unit Tests
./gradlew testDebugUnitTest

# 2. Build Debug APK
./gradlew assembleDebug
# Output APK: app/build/outputs/apk/debug/app-debug.apk

# 3. Build Release APK
./gradlew assembleRelease
# Output APK: app/build/outputs/apk/release/app-release.apk
```

---

## 4. Otomatisasi Build dengan GitHub Actions

File workflow tersedia di `.github/workflows/android-build.yml`. Setiap kali ada `push`, `pull_request`, atau pemicu manual `workflow_dispatch`:
1. GitHub Actions menyiapkan Ubuntu runner, JDK 17, Android SDK 34, dan Gradle 8.7.
2. Menjalankan `./gradlew testDebugUnitTest`.
3. Mengompilasi Debug APK (`assembleDebug`) dan Release APK (`assembleRelease`).
4. Mengunggah file APK siap pasang ke bagian **Artifacts** (`ObsidianCast-Debug-APK` & `ObsidianCast-Release-APK`).
