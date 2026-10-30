package com.obsidiancast.studio.scene

import com.obsidiancast.studio.domain.BroadcastScene
import com.obsidiancast.studio.domain.CompositorLayoutMode
import com.obsidiancast.studio.domain.SceneLayerItem
import java.util.UUID

/**
 * Pure domain Scene Manager supporting OBS-style operations:
 * Add/Duplicate/Delete scene, Add/Remove/Rearrange layers, and Save/Load preset configurations.
 */
object SceneManager {

    fun duplicateScene(scenes: List<BroadcastScene>, sceneId: String): List<BroadcastScene> {
        val original = scenes.find { it.id == sceneId } ?: return scenes
        val copyId = "scene_${UUID.randomUUID().toString().take(6)}"
        val duplicated = original.copy(
            id = copyId,
            name = "${original.name} (Copy)",
            isCustom = true,
            layers = original.layers.map {
                it.copy(id = "layer_${UUID.randomUUID().toString().take(6)}")
            }
        )
        return scenes + duplicated
    }

    fun createCustomScene(
        scenes: List<BroadcastScene>,
        name: String,
        layoutMode: CompositorLayoutMode,
        primarySourceId: String,
        secondarySourceId: String? = null
    ): List<BroadcastScene> {
        val newId = "scene_${UUID.randomUUID().toString().take(6)}"
        val layers = buildList {
            add(SceneLayerItem("layer_${newId}_1", primarySourceId, zOrder = 0))
            if (secondarySourceId != null) {
                add(
                    SceneLayerItem(
                        id = "layer_${newId}_2",
                        sourceId = secondarySourceId,
                        zOrder = 1,
                        normalizedX = 0.65f,
                        normalizedY = 0.62f,
                        normalizedWidth = 0.30f,
                        normalizedHeight = 0.30f
                    )
                )
            }
        }
        val newScene = BroadcastScene(
            id = newId,
            name = name.ifBlank { "Custom Scene ${scenes.size + 1}" },
            layoutMode = layoutMode,
            primarySourceId = primarySourceId,
            secondarySourceId = secondarySourceId,
            layers = layers,
            isCustom = true
        )
        return scenes + newScene
    }

    fun addSourceLayerToScene(
        scene: BroadcastScene,
        sourceId: String
    ): BroadcastScene {
        val nextZ = (scene.layers.maxOfOrNull { it.zOrder } ?: -1) + 1
        val newLayer = SceneLayerItem(
            id = "layer_${UUID.randomUUID().toString().take(6)}",
            sourceId = sourceId,
            zOrder = nextZ,
            normalizedX = 0.65f,
            normalizedY = 0.62f,
            normalizedWidth = 0.30f,
            normalizedHeight = 0.30f
        )
        return scene.copy(
            secondarySourceId = sourceId,
            layers = scene.layers + newLayer
        )
    }

    fun removeSourceLayerFromScene(
        scene: BroadcastScene,
        layerId: String
    ): BroadcastScene {
        val remaining = scene.layers
            .filterNot { it.id == layerId }
            .mapIndexed { idx, item -> item.copy(zOrder = idx) }
        return scene.copy(layers = remaining)
    }

    fun moveLayerOrder(
        scene: BroadcastScene,
        layerId: String,
        directionDelta: Int
    ): BroadcastScene {
        val sorted = scene.layers.sortedBy { it.zOrder }.toMutableList()
        val index = sorted.indexOfFirst { it.id == layerId }
        if (index == -1) return scene
        val targetIndex = (index + directionDelta).coerceIn(0, sorted.lastIndex)
        if (index == targetIndex) return scene

        val item = sorted.removeAt(index)
        sorted.add(targetIndex, item)
        return scene.copy(
            layers = sorted.mapIndexed { idx, layer -> layer.copy(zOrder = idx) }
        )
    }
}
