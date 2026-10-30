# Panduan Pengujian (Testing) — ObsidianCast Studio

## 1. Unit Testing (JUnit 4 & Coroutines Test)

Lokasi file: `app/src/test/java/com/obsidiancast/studio/StudioEngineUnitTest.kt`

Cakupan pengujian otomatis:
- **PCM 16-bit Audio Mixing & Mute Logic**: Memastikan pencampuran sampel PCM multi-kanal akurat, menghormati status mute per kanal, dan mencegah overflow 16-bit (`Short.MIN_VALUE..Short.MAX_VALUE`).
- **Deteksi Bandwidth USB & Concurrency Kamera**: Memastikan `HardwareCapabilityDetector` menghitung beban bitrate/bandwidth kamera USB UVC dan batas kamera internal secara akurat.
- **Scene Management (Duplicate, Create, Layer Reorder)**: Memverifikasi pembuatan scene baru, duplikasi scene, serta pengurutan layer Z-order.
- **Validasi Endpoint RTMP/RTMPS**: Memastikan URL `rtmp://` dan `rtmps://` serta Stream Key divalidasi sebelum koneksi jaringan dibuka.
- **Timecode & Storage Formatting**: Memastikan format `HH:MM:SS` dan ukuran file MP4 akurat.

Jalankan dengan:
```bash
./gradlew testDebugUnitTest
```

---

## 2. Daftar Periksa Pengujian Perangkat Fisik (Field Verification)

1. **Kamera Internal & Sensor**: Uji perpindahan instan antara kamera belakang dan depan serta perubahan Exposure Compensation (EV).
2. **USB UVC Hot-Plug**: Hubungkan USB HDMI capture card melalui adaptor OTG; pastikan dialog izin USB muncul dan status input berubah menjadi `CONNECTED`, lalu cabut kabel untuk memverifikasi status `RECONNECTING` tanpa *force close*.
3. **RTSP Stream**: Masukkan URL kamera IP lokal (`rtsp://<ip>:554/stream1`) dan pastikan latensi pemutaran tetap rendah.
4. **Live Streaming RTMP/RTMPS**: Masukkan URL YouTube Live (`rtmps://a.rtmp.youtube.com/live2`) dan Stream Key uji, mulai streaming pada `1080p30`, dan pantau penghitung *dropped frames*.
5. **Perekaman MP4**: Rekam durasi 30 detik pada `1080p30`, hentikan rekaman, dan putar kembali file MP4 hasil rekaman untuk memastikan sinkronisasi audio-video.
