package com.obsidiancast.studio.video

import com.obsidiancast.studio.domain.CompositorLayoutMode

data class ViewportQuad(
    val sourceId: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val alpha: Float = 1.0f
)

/**
 * Computes zero-allocation OpenGL ES normalized viewport coordinates for multi-camera scenes
 * (Fullscreen, Picture-in-Picture, Split-Screen 2-Up, Quad 4-Input Multiview) and smooth cross-fade
 * blending between Preview and Program surfaces.
 */
object GlSurfaceCompositor {

    fun computeSceneQuads(
        layoutMode: CompositorLayoutMode,
        programSourceId: String,
        previewSourceId: String,
        secondaryPipSourceId: String,
        allCameraIds: List<String>,
        transitionProgress: Float,
        isTransitioning: Boolean
    ): List<ViewportQuad> {
        val baseQuads = when (layoutMode) {
            CompositorLayoutMode.FULLSCREEN -> listOf(
                ViewportQuad(programSourceId, 0f, 0f, 1f, 1f, 1f)
            )
            CompositorLayoutMode.PICTURE_IN_PICTURE -> listOf(
                ViewportQuad(programSourceId, 0f, 0f, 1f, 1f, 1f),
                ViewportQuad(secondaryPipSourceId, 0.66f, 0.62f, 0.30f, 0.32f, 1f)
            )
            CompositorLayoutMode.SPLIT_SCREEN_2UP -> listOf(
                ViewportQuad(programSourceId, 0f, 0f, 0.5f, 1f, 1f),
                ViewportQuad(secondaryPipSourceId, 0.5f, 0f, 0.5f, 1f, 1f)
            )
            CompositorLayoutMode.QUAD_MULTIVIEW -> {
                val c0 = allCameraIds.getOrElse(0) { programSourceId }
                val c1 = allCameraIds.getOrElse(1) { previewSourceId }
                val c2 = allCameraIds.getOrElse(2) { secondaryPipSourceId }
                val c3 = allCameraIds.getOrElse(3) { programSourceId }
                listOf(
                    ViewportQuad(c0, 0f, 0f, 0.5f, 0.5f, 1f),
                    ViewportQuad(c1, 0.5f, 0f, 0.5f, 0.5f, 1f),
                    ViewportQuad(c2, 0f, 0.5f, 0.5f, 0.5f, 1f),
                    ViewportQuad(c3, 0.5f, 0.5f, 0.5f, 0.5f, 1f)
                )
            }
        }

        if (!isTransitioning || transitionProgress <= 0f) {
            return baseQuads
        }

        return baseQuads + ViewportQuad(
            sourceId = previewSourceId,
            x = 0f,
            y = 0f,
            width = 1f,
            height = 1f,
            alpha = transitionProgress.coerceIn(0f, 1f)
        )
    }
}
