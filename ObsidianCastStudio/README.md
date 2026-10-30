# ObsidianCast — Android Live Multi-Camera Studio

ObsidianCast Studio adalah aplikasi produksi siaran langsung (*live multi-camera broadcast switcher, PCM audio mixer, RTMP/RTMPS streamer, dan MP4 recorder*) native untuk Android yang terinspirasi oleh **OBS Studio** dan **vMix**.

---

## Fitur Utama

1. **Multi-Camera Engine (Hingga 4 Input Kamera)**
   - **Internal CameraX + Camera2Interop**: Kontrol sensor lanjutan (Exposure Compensation EV, ISO, White Balance Kelvin, Continuous/Manual Focus, FPS Range).
   - **External USB UVC Camera**: Mendukung HDMI-to-USB capture card & webcam UVC (Class `0x0E`) dengan filter perangkat USB otomatis (`res/xml/usb_device_filter.xml`).
   - **IP Camera (RTSP)**: Pemutaran latensi rendah menggunakan AndroidX Media3 ExoPlayer RTSP (`RtspMediaSource`) melalui TCP interleaved atau UDP.
   - **Hardware Capability & Bandwidth Detector**: Mendeteksi batas `CameraManager.getConcurrentCameraIds()` (Android 11+), estimasi bandwidth isochronous USB 2.0/3.0, serta instance `MediaCodec` H.264 hardware agar aplikasi tetap stabil.
   - **Auto-Recovery Watchdog**: Reconnect otomatis dengan *exponential backoff* ketika kamera USB atau stream RTSP terputus.

2. **OBS-Style Video Switcher & Compositor**
   - Monitor ganda **PREVIEW (PVW)** dan **PROGRAM (PGM)**.
   - Transisi instan **CUT (0ms)**, **FADE (450ms)**, dan **AUTO (800ms)** berbasis Coroutines tanpa memblokir UI thread.
   - Mode tata letak kompositor: **Fullscreen**, **Picture-in-Picture (PiP)**, **Split Screen 2-Up**, dan **Quad 4-Input Multiview**.

3. **Real-Time 16-Bit PCM Audio Mixer**
   - Pencampuran multi-bus: Internal Microphone, Camera Embedded Audio, USB Audio Interface (UAC2), dan Master Bus.
   - Fader gain (`-60 dB` hingga `+6 dB`), Mute, Solo, Audio Monitoring, dan meter level Peak/RMS dBFS real-time.

4. **Live Streaming RTMP / RTMPS**
   - Kompatibel dengan **YouTube Live**, **Twitch**, dan **Custom RTMP/RTMPS Server**.
   - Kontrol resolusi (`720p30`, `1080p30`, `1080p60`), bitrate video & audio, penghitung *dropped frames*, durasi siaran, dan mekanisme auto-reconnect.

5. **MP4 Hardware Recording**
   - Perekaman langsung ke penyimpanan perangkat (`/Movies/ObsidianCast/*.mp4`) dengan preset kualitas `720p30`, `1080p30`, dan `1080p60`.

---

## Struktur Modul Project

```text
app/src/main/java/com/obsidiancast/studio/
├── audio/          # PcmAudioMixer.kt (16-bit PCM mixing & dBFS Peak/RMS metering)
├── camera/         # HardwareCapabilityDetector.kt, MultiCameraController.kt (CameraX/Camera2/UVC)
├── data/           # BroadcastRepository.kt (Single source of truth state flows)
├── domain/         # BroadcastModels.kt (Camera, Scene, Audio, Stream, Recording models)
├── network/        # RtspNetworkClient.kt (Media3 ExoPlayer RTSP low-latency client)
├── presentation/   # MainActivity.kt, StudioViewModel.kt, BroadcastDashboardScreen.kt
├── recording/      # Mp4RecorderEngine.kt (MediaCodec + MediaMuxer MP4 recording)
├── scene/          # SceneManager.kt (Add, Remove, Rearrange, Duplicate, Save/Load scenes)
├── streaming/      # RtmpStreamEngine.kt (RTMP/RTMPS validation, adaptive bitrate, telemetry)
├── utils/          # TimecodeFormatter.kt
└── video/          # VideoSwitcherEngine.kt (PVW/PGM switching, Cut/Fade/Auto transitions)
```

---

## Cara Membuka dan Menjalankan di Android Studio

1. Ekstrak file **`ObsidianCast-AndroidStudio-Project.zip`** (atau buka folder `android-studio-project/`).
2. Buka **Android Studio Ladybug / Koala (2024.1+)** -> Pilih **Open** -> Arahkan ke folder project yang memiliki `settings.gradle.kts`.
3. Pastikan **JDK 17** terpilih pada *Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK*.
4. Tunggu proses **Gradle Sync** selesai, lalu klik **Run (`Shift + F10`)** pada perangkat Android fisik (minimal Android 8.0 / API 26, disarankan Android 11+ / API 30+ untuk concurrent multi-camera).
