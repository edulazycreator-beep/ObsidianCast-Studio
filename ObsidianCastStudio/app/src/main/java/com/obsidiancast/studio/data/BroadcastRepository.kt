package com.obsidiancast.studio.data

import com.obsidiancast.studio.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BroadcastRepository {

    private val _cameraSources = MutableStateFlow(defaultCameraSources())
    val cameraSources: StateFlow<List<CameraInputSource>> = _cameraSources.asStateFlow()

    private val _scenes = MutableStateFlow(defaultScenes())
    val scenes: StateFlow<List<BroadcastScene>> = _scenes.asStateFlow()

    private val _audioChannels = MutableStateFlow(defaultAudioChannels())
    val audioChannels: StateFlow<List<AudioMixerChannel>> = _audioChannels.asStateFlow()

    private val _streamConfig = MutableStateFlow(StreamConfiguration())
    val streamConfig: StateFlow<StreamConfiguration> = _streamConfig.asStateFlow()

    fun updateCameraSource(updated: CameraInputSource) {
        _cameraSources.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    fun updateScenes(newScenes: List<BroadcastScene>) {
        _scenes.value = newScenes
    }

    fun updateAudioChannel(updated: AudioMixerChannel) {
        _audioChannels.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    fun updateStreamConfiguration(config: StreamConfiguration) {
        _streamConfig.value = config
    }

    companion object {
        fun defaultCameraSources(): List<CameraInputSource> = listOf(
            CameraInputSource(
                id = "cam_1",
                slotIndex = 1,
                name = "CAM 1 · Rear Wide (CameraX)",
                type = CameraSourceType.INTERNAL_BACK,
                resolutionWidth = 1920,
                resolutionHeight = 1080,
                targetFps = 60,
                measuredFps = 60,
                bitrateKbps = 6000,
                connectionState = ConnectionState.CONNECTED,
                supportsManualSensorControl = true,
                hasEmbeddedAudio = true
            ),
            CameraInputSource(
                id = "cam_2",
                slotIndex = 2,
                name = "CAM 2 · Front Host (Camera2)",
                type = CameraSourceType.INTERNAL_FRONT,
                resolutionWidth = 1920,
                resolutionHeight = 1080,
                targetFps = 30,
                measuredFps = 30,
                bitrateKbps = 4500,
                connectionState = ConnectionState.CONNECTED,
                supportsManualSensorControl = true,
                hasEmbeddedAudio = true
            ),
            CameraInputSource(
                id = "cam_3",
                slotIndex = 3,
                name = "CAM 3 · USB HDMI Capture (UVC)",
                type = CameraSourceType.USB_UVC,
                resolutionWidth = 1920,
                resolutionHeight = 1080,
                targetFps = 30,
                measuredFps = 30,
                bitrateKbps = 5000,
                connectionState = ConnectionState.CONNECTED,
                usbVendorProductId = "0BDA:5830",
                supportsManualSensorControl = false,
                hasEmbeddedAudio = true
            ),
            CameraInputSource(
                id = "cam_4",
                slotIndex = 4,
                name = "CAM 4 · PTZ Stage Overhead (RTSP)",
                type = CameraSourceType.RTSP_IP,
                resolutionWidth = 1280,
                resolutionHeight = 720,
                targetFps = 30,
                measuredFps = 30,
                bitrateKbps = 3500,
                connectionState = ConnectionState.CONNECTED,
                rtspUrl = "rtsp://192.168.1.140:554/stream1",
                supportsManualSensorControl = false,
                hasEmbeddedAudio = true
            )
        )

        fun defaultScenes(): List<BroadcastScene> = listOf(
            BroadcastScene(
                id = "scene_1",
                name = "Scene 1 · Main Anchor",
                layoutMode = CompositorLayoutMode.FULLSCREEN,
                primarySourceId = "cam_1",
                layers = listOf(
                    SceneLayerItem("layer_1", "cam_1", zOrder = 0)
                )
            ),
            BroadcastScene(
                id = "scene_2",
                name = "Scene 2 · Host + Demo PiP",
                layoutMode = CompositorLayoutMode.PICTURE_IN_PICTURE,
                primarySourceId = "cam_3",
                secondarySourceId = "cam_2",
                layers = listOf(
                    SceneLayerItem("layer_2a", "cam_3", zOrder = 0),
                    SceneLayerItem("layer_2b", "cam_2", zOrder = 1, 0.68f, 0.64f, 0.28f, 0.28f)
                )
            ),
            BroadcastScene(
                id = "scene_3",
                name = "Scene 3 · Dual Interview Split",
                layoutMode = CompositorLayoutMode.SPLIT_SCREEN_2UP,
                primarySourceId = "cam_1",
                secondarySourceId = "cam_4",
                layers = listOf(
                    SceneLayerItem("layer_3a", "cam_1", zOrder = 0, 0f, 0f, 0.5f, 1f),
                    SceneLayerItem("layer_3b", "cam_4", zOrder = 1, 0.5f, 0f, 0.5f, 1f)
                )
            )
        )

        fun defaultAudioChannels(): List<AudioMixerChannel> = listOf(
            AudioMixerChannel(
                id = "aud_mic",
                name = "Mic 1 (Internal / Lavalier)",
                busType = AudioBusType.INTERNAL_MIC,
                faderGainDb = -1.5f,
                volumeLinear = 0.88f,
                currentPeakDb = -9.2f,
                currentRmsDb = -14.8f
            ),
            AudioMixerChannel(
                id = "aud_cam",
                name = "Camera Embedded Audio",
                busType = AudioBusType.CAMERA_EMBEDDED,
                faderGainDb = -6.0f,
                volumeLinear = 0.72f,
                currentPeakDb = -16.4f,
                currentRmsDb = -22.1f
            ),
            AudioMixerChannel(
                id = "aud_usb",
                name = "USB Audio Interface (UAC2)",
                busType = AudioBusType.USB_AUDIO_INTERFACE,
                faderGainDb = 0.0f,
                volumeLinear = 0.92f,
                currentPeakDb = -6.8f,
                currentRmsDb = -11.5f
            ),
            AudioMixerChannel(
                id = "aud_master",
                name = "Master Program Bus",
                busType = AudioBusType.MASTER,
                faderGainDb = 0.0f,
                volumeLinear = 0.90f,
                currentPeakDb = -5.4f,
                currentRmsDb = -10.2f
            )
        )
    }
}
