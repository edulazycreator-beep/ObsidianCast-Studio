package com.obsidiancast.studio.domain

enum class CameraSourceType {
    INTERNAL_BACK,
    INTERNAL_FRONT,
    USB_UVC,
    RTSP_IP,
    NETWORK_NDI_SRT,
    SCREEN_CAPTURE
}

enum class ConnectionState {
    CONNECTED,
    CONNECTING,
    RECONNECTING,
    DISCONNECTED,
    BANDWIDTH_LIMITED
}

enum class TransitionType(val label: String, val defaultDurationMs: Long) {
    CUT("Cut", 0L),
    FADE("Fade", 450L),
    AUTO("Auto", 800L),
    WIPE("Wipe", 500L)
}

enum class CompositorLayoutMode(val label: String) {
    FULLSCREEN("Single Fullscreen"),
    PICTURE_IN_PICTURE("Picture in Picture"),
    SPLIT_SCREEN_2UP("Split Screen 2-Up"),
    QUAD_MULTIVIEW("Quad 4-Input View")
}

data class CameraExposureConfig(
    val exposureCompensationIndex: Int = 0,
    val isoSensitivity: Int = 400,
    val whiteBalanceKelvin: Int = 5600,
    val continuousAutoFocus: Boolean = true,
    val manualFocusDistance: Float = 0.0f
)

data class CameraInputSource(
    val id: String,
    val slotIndex: Int,
    val name: String,
    val type: CameraSourceType,
    val resolutionWidth: Int = 1920,
    val resolutionHeight: Int = 1080,
    val targetFps: Int = 30,
    val measuredFps: Int = 30,
    val bitrateKbps: Int = 4500,
    val connectionState: ConnectionState = ConnectionState.CONNECTED,
    val rtspUrl: String? = null,
    val usbVendorProductId: String? = null,
    val supportsManualSensorControl: Boolean = true,
    val hasEmbeddedAudio: Boolean = true,
    val exposureConfig: CameraExposureConfig = CameraExposureConfig(),
    val reconnectAttempts: Int = 0
)

data class SceneLayerItem(
    val id: String,
    val sourceId: String,
    val zOrder: Int,
    val normalizedX: Float = 0f,
    val normalizedY: Float = 0f,
    val normalizedWidth: Float = 1f,
    val normalizedHeight: Float = 1f,
    val visible: Boolean = true
)

data class BroadcastScene(
    val id: String,
    val name: String,
    val layoutMode: CompositorLayoutMode = CompositorLayoutMode.FULLSCREEN,
    val primarySourceId: String,
    val secondarySourceId: String? = null,
    val layers: List<SceneLayerItem> = emptyList(),
    val isCustom: Boolean = false
)

enum class AudioBusType {
    INTERNAL_MIC,
    CAMERA_EMBEDDED,
    USB_AUDIO_INTERFACE,
    RTSP_STREAM_AUDIO,
    MASTER
}

data class AudioMixerChannel(
    val id: String,
    val name: String,
    val busType: AudioBusType,
    val faderGainDb: Float = 0.0f, // -60.0 dB to +6.0 dB
    val volumeLinear: Float = 0.85f, // 0.0f to 1.0f
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val isMonitoringEnabled: Boolean = false,
    val currentPeakDb: Float = -18.0f,
    val currentRmsDb: Float = -24.0f,
    val clippingDetected: Boolean = false
)

enum class StreamProtocol {
    RTMP,
    RTMPS
}

enum class StreamPresetTarget(val label: String, val defaultUrl: String) {
    YOUTUBE_LIVE("YouTube Live", "rtmps://a.rtmp.youtube.com/live2"),
    TWITCH("Twitch Ingest", "rtmp://live.twitch.tv/app"),
    CUSTOM_RTMP("Custom RTMP/RTMPS Server", "rtmp://stream.example.com:1935/live")
}

data class StreamConfiguration(
    val protocol: StreamProtocol = StreamProtocol.RTMPS,
    val presetTarget: StreamPresetTarget = StreamPresetTarget.YOUTUBE_LIVE,
    val serverUrl: String = "rtmps://a.rtmp.youtube.com/live2",
    val streamKey: String = "",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val videoBitrateKbps: Int = 4500,
    val audioBitrateKbps: Int = 128,
    val adaptiveBitrateEnabled: Boolean = true,
    val autoReconnectEnabled: Boolean = true
)

data class LiveStreamRuntimeStats(
    val isStreaming: Boolean = false,
    val connectionStatus: String = "IDLE",
    val elapsedDurationSec: Long = 0L,
    val currentVideoBitrateKbps: Int = 0,
    val currentFps: Float = 0f,
    val droppedFrames: Int = 0,
    val reconnectCount: Int = 0,
    val lastErrorMessage: String? = null
)

enum class RecordingQualityPreset(
    val label: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrateKbps: Int
) {
    HD_720P_30("720p 30 FPS (6 Mbps)", 1280, 720, 30, 6_000),
    FHD_1080P_30("1080p 30 FPS (12 Mbps)", 1920, 1080, 30, 12_000),
    FHD_1080P_60("1080p 60 FPS (18 Mbps)", 1920, 1080, 60, 18_000)
}

data class RecordingRuntimeState(
    val isRecording: Boolean = false,
    val qualityPreset: RecordingQualityPreset = RecordingQualityPreset.FHD_1080P_30,
    val outputFilePath: String = "/storage/emulated/0/Movies/ObsidianCast/cast_capture.mp4",
    val durationSec: Long = 0L,
    val bytesWritten: Long = 0L,
    val statusText: String = "READY"
)

data class HardwareCapabilityReport(
    val maxConcurrentInternalCameras: Int = 2,
    val supportsConcurrentBackAndFront: Boolean = true,
    val maxUsbIsochronousBandwidthMbps: Int = 380,
    val estimatedActiveUsbBandwidthMbps: Int = 145,
    val maxHardwareAvcEncoderInstances: Int = 4,
    val supports1080p60AvcHardwareEncode: Boolean = true,
    val recommendedMaxUsbCamerasSimultaneous: Int = 2,
    val deviceSoCTier: String = "HIGH_TIER_HARDWARE_CODEC",
    val warnings: List<String> = emptyList()
)
