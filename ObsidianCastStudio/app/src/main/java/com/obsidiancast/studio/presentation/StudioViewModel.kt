package com.obsidiancast.studio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.obsidiancast.studio.audio.PcmAudioMixer
import com.obsidiancast.studio.camera.HardwareCapabilityDetector
import com.obsidiancast.studio.data.BroadcastRepository
import com.obsidiancast.studio.domain.*
import com.obsidiancast.studio.recording.Mp4RecorderEngine
import com.obsidiancast.studio.scene.SceneManager
import com.obsidiancast.studio.streaming.RtmpStreamEngine
import com.obsidiancast.studio.video.SwitcherState
import com.obsidiancast.studio.video.VideoSwitcherEngine
import kotlinx.coroutines.flow.*

class StudioViewModel(
    private val repository: BroadcastRepository = BroadcastRepository()
) : ViewModel() {

    private val switcherEngine = VideoSwitcherEngine(viewModelScope)
    private val streamEngine = RtmpStreamEngine(viewModelScope)
    private val recorderEngine = Mp4RecorderEngine(viewModelScope)

    val cameraSources: StateFlow<List<CameraInputSource>> = repository.cameraSources
    val scenes: StateFlow<List<BroadcastScene>> = repository.scenes
    val audioChannels: StateFlow<List<AudioMixerChannel>> = repository.audioChannels
    val streamConfig: StateFlow<StreamConfiguration> = repository.streamConfig

    val switcherState: StateFlow<SwitcherState> = switcherEngine.state
    val streamStats: StateFlow<LiveStreamRuntimeStats> = streamEngine.stats
    val recordingState: StateFlow<RecordingRuntimeState> = recorderEngine.state

    private val _activeSceneId = MutableStateFlow("scene_1")
    val activeSceneId: StateFlow<String> = _activeSceneId.asStateFlow()

    val hardwareCapabilities: StateFlow<HardwareCapabilityReport> = cameraSources
        .map { sources ->
            HardwareCapabilityDetector.evaluateBandwidthAndConcurrency(sources)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HardwareCapabilityDetector.evaluateBandwidthAndConcurrency(
                BroadcastRepository.defaultCameraSources()
            )
        )

    fun selectPreviewCamera(cameraId: String) {
        switcherEngine.selectPreviewSource(cameraId)
    }

    fun cutTransition() {
        switcherEngine.executeCut()
    }

    fun fadeTransition() {
        switcherEngine.executeAnimatedTransition(TransitionType.FADE)
    }

    fun autoTransition() {
        switcherEngine.executeAnimatedTransition(TransitionType.AUTO)
    }

    fun setCompositorLayout(mode: CompositorLayoutMode) {
        switcherEngine.setLayoutMode(mode)
    }

    fun activateScene(scene: BroadcastScene) {
        _activeSceneId.value = scene.id
        switcherEngine.selectPreviewSource(scene.primarySourceId)
        switcherEngine.setLayoutMode(scene.layoutMode)
        switcherEngine.executeAnimatedTransition(TransitionType.FADE, 300L)
    }

    fun duplicateActiveScene() {
        val updated = SceneManager.duplicateScene(scenes.value, _activeSceneId.value)
        repository.updateScenes(updated)
    }

    fun createNewCustomScene(name: String, layoutMode: CompositorLayoutMode, primarySourceId: String) {
        val updated = SceneManager.createCustomScene(
            scenes = scenes.value,
            name = name,
            layoutMode = layoutMode,
            primarySourceId = primarySourceId,
            secondarySourceId = switcherState.value.previewSourceId
        )
        repository.updateScenes(updated)
    }

    fun setAudioFaderGain(channelId: String, gainDb: Float) {
        val existing = audioChannels.value.find { it.id == channelId } ?: return
        val clampedDb = gainDb.coerceIn(-60f, 6f)
        val linear = PcmAudioMixer.dbToLinearGain(clampedDb).coerceIn(0f, 1.25f)
        repository.updateAudioChannel(
            existing.copy(
                faderGainDb = clampedDb,
                volumeLinear = linear
            )
        )
    }

    fun toggleAudioMute(channelId: String) {
        val existing = audioChannels.value.find { it.id == channelId } ?: return
        repository.updateAudioChannel(existing.copy(isMuted = !existing.isMuted))
    }

    fun toggleAudioMonitoring(channelId: String) {
        val existing = audioChannels.value.find { it.id == channelId } ?: return
        repository.updateAudioChannel(existing.copy(isMonitoringEnabled = !existing.isMonitoringEnabled))
    }

    fun updateStreamConfig(config: StreamConfiguration) {
        repository.updateStreamConfiguration(config)
    }

    fun toggleLiveStreaming(): Result<Unit> {
        return if (streamStats.value.isStreaming) {
            streamEngine.stopStreaming()
            Result.success(Unit)
        } else {
            streamEngine.startStreaming(streamConfig.value)
        }
    }

    fun toggleRecording() {
        if (recordingState.value.isRecording) {
            recorderEngine.stopRecording()
        } else {
            recorderEngine.startRecording()
        }
    }
}
