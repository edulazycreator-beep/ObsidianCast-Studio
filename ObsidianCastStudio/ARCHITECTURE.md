# Arsitektur Sistem — ObsidianCast Android Live Multi-Camera Studio

## 1. Pola Arsitektur (Clean Modular MVVM + Reactive StateFlow)

Aplikasi ini dirancang dengan pemisahan tegas antara:
- **Presentation Layer (`presentation/`)**: Jetpack Compose Material 3 dengan konsep *Dark Glassmorphism Broadcast Interface* serta `StudioViewModel` yang mengekspos `StateFlow` imutabel.
- **Domain & Engine Layer (`camera/`, `video/`, `audio/`, `streaming/`, `recording/`, `scene/`, `network/`)**: Komponen pemrosesan media non-blocking berbasis Kotlin Coroutines.
- **Data Layer (`data/`)**: `BroadcastRepository` yang menyimpan konfigurasi kamera, scene, saluran audio, serta preset streaming.

---

## 2. Manajemen Keterbatasan Hardware & Bandwidth Multi-Kamera

Tidak semua perangkat Android mendukung 4 kamera USB atau 4 kamera internal secara bersamaan. `HardwareCapabilityDetector` mengimplementasikan pemeriksaan bertingkat:
1. **Camera2 Concurrent Camera Sets**: Memanggil `CameraManager.getConcurrentCameraIds()` pada Android 11+ (API 30+) untuk memverifikasi apakah sensor belakang dan depan dapat berjalan serentak.
2. **USB Isochronous/Bulk Bandwidth Guard**: Menghitung estimasi throughput MJPEG/YUY2 dari seluruh kamera USB UVC aktif terhadap kapasitas efektif controller USB 2.0 High-Speed (~320 Mbps efektif) atau USB 3.0 SuperSpeed.
3. **MediaCodec Instance Probe**: Memeriksa `MediaCodecInfo.CodecCapabilities.getMaxSupportedInstances()` untuk encoder/decoder hardware `video/avc` agar tidak terjadi `MediaCodec.CodecException` saat melakukan preview + streaming + recording bersamaan.

---

## 3. Pipeline Audio & Video

- **Video Pipeline**: Input Kamera (CameraX Surface / USB UVC SurfaceTexture / Media3 RTSP Surface) -> Compositor Switcher (Preview & Program state + Cut/Fade/PiP/Split-Screen) -> Hardware `MediaCodec` H.264 Encoder -> `RtmpStreamEngine` & `Mp4RecorderEngine`.
- **Audio Pipeline**: `AudioRecord` PCM 16-bit 48kHz Stereo -> `PcmAudioMixer.mixPcm16Streams()` (gain per kanal + mute + master limiter + kalkulasi Peak/RMS dBFS) -> Hardware `MediaCodec` AAC-LC Encoder.
