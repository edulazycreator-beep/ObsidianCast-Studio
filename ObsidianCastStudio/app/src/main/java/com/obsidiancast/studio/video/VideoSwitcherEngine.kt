package com.obsidiancast.studio.video

import com.obsidiancast.studio.domain.CompositorLayoutMode
import com.obsidiancast.studio.domain.TransitionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SwitcherState(
    val programSourceId: String = "cam_1",
    val previewSourceId: String = "cam_2",
    val secondaryPipSourceId: String = "cam_3",
    val layoutMode: CompositorLayoutMode = CompositorLayoutMode.FULLSCREEN,
    val activeTransition: TransitionType = TransitionType.FADE,
    val transitionProgress: Float = 0f,
    val isTransitioning: Boolean = false,
    val isFullscreenMonitor: Boolean = false
)

/**
 * Low-latency, non-blocking Video Switcher & Compositor controller.
 * All transitions run on a background coroutine timer updating an atomic StateFlow
 * consumed by the OpenGL ES surface compositor without allocating Bitmaps or blocking the UI thread.
 */
class VideoSwitcherEngine(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(SwitcherState())
    val state: StateFlow<SwitcherState> = _state.asStateFlow()

    private var transitionJob: Job? = null

    fun selectPreviewSource(sourceId: String) {
        if (sourceId == _state.value.previewSourceId) return
        _state.value = _state.value.copy(previewSourceId = sourceId)
    }

    fun setLayoutMode(mode: CompositorLayoutMode) {
        _state.value = _state.value.copy(layoutMode = mode)
    }

    fun executeCut() {
        transitionJob?.cancel()
        val current = _state.value
        _state.value = current.copy(
            programSourceId = current.previewSourceId,
            previewSourceId = current.programSourceId,
            transitionProgress = 0f,
            isTransitioning = false
        )
    }

    fun executeAnimatedTransition(
        type: TransitionType = _state.value.activeTransition,
        durationMs: Long = type.defaultDurationMs
    ) {
        if (type == TransitionType.CUT || durationMs <= 0L) {
            executeCut()
            return
        }

        transitionJob?.cancel()
        transitionJob = scope.launch {
            val steps = 24
            val stepDelay = (durationMs / steps).coerceAtLeast(8L)
            _state.value = _state.value.copy(
                activeTransition = type,
                isTransitioning = true,
                transitionProgress = 0f
            )

            for (i in 1..steps) {
                delay(stepDelay)
                val progress = i.toFloat() / steps.toFloat()
                _state.value = _state.value.copy(transitionProgress = progress)
            }

            val finalState = _state.value
            _state.value = finalState.copy(
                programSourceId = finalState.previewSourceId,
                previewSourceId = finalState.programSourceId,
                transitionProgress = 0f,
                isTransitioning = false
            )
        }
    }

    fun toggleFullscreenMonitor() {
        _state.value = _state.value.copy(
            isFullscreenMonitor = !_state.value.isFullscreenMonitor
        )
    }
}
