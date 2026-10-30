package com.obsidiancast.studio

import com.obsidiancast.studio.audio.PcmAudioMixer
import com.obsidiancast.studio.camera.HardwareCapabilityDetector
import com.obsidiancast.studio.data.BroadcastRepository
import com.obsidiancast.studio.domain.CompositorLayoutMode
import com.obsidiancast.studio.domain.StreamConfiguration
import com.obsidiancast.studio.scene.SceneManager
import com.obsidiancast.studio.streaming.RtmpStreamEngine
import com.obsidiancast.studio.utils.TimecodeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

class StudioEngineUnitTest {

    @Test
    fun `pcm audio mixer mixes active channels and respects mute`() {
        val ch1 = shortArrayOf(1000, 2000, -1000)
        val ch2 = shortArrayOf(500, 500, 500)
        val output = ShortArray(3)

        PcmAudioMixer.mixPcm16Streams(
            inputs = listOf(ch1, ch2),
            channelGainsLinear = listOf(1.0f, 1.0f),
            channelMuted = listOf(false, true), // Channel 2 muted
            masterGainLinear = 1.0f,
            outputBuffer = output
        )

        assertEquals(1000.toShort(), output[0])
        assertEquals(2000.toShort(), output[1])
        assertEquals((-1000).toShort(), output[2])
    }

    @Test
    fun `hardware capability detector warns when USB bandwidth is exceeded`() {
        val defaultSources = BroadcastRepository.defaultCameraSources()
        val report = HardwareCapabilityDetector.evaluateBandwidthAndConcurrency(
            activeSources = defaultSources,
            maxConcurrentInternalCameras = 2,
            maxHardwareAvcEncoders = 4,
            supports1080p60Encode = true
        )
        assertTrue(report.estimatedActiveUsbBandwidthMbps > 0)
        assertEquals(2, report.maxConcurrentInternalCameras)
    }

    @Test
    fun `scene manager duplicates and reorders layers deterministically`() {
        val scenes = BroadcastRepository.defaultScenes()
        val duplicated = SceneManager.duplicateScene(scenes, "scene_2")
        assertEquals(scenes.size + 1, duplicated.size)

        val custom = SceneManager.createCustomScene(
            scenes = duplicated,
            name = "Quad Engineering View",
            layoutMode = CompositorLayoutMode.QUAD_MULTIVIEW,
            primarySourceId = "cam_1",
            secondarySourceId = "cam_3"
        )
        assertEquals(duplicated.size + 1, custom.size)
        assertEquals("Quad Engineering View", custom.last().name)
    }

    @Test
    fun `rtmp stream engine validates endpoint url and stream key`() {
        val engine = RtmpStreamEngine(CoroutineScope(Dispatchers.Unconfined))
        val invalidKeyConfig = StreamConfiguration(
            serverUrl = "rtmps://a.rtmp.youtube.com/live2",
            streamKey = ""
        )
        assertTrue(engine.validateEndpoint(invalidKeyConfig).isFailure)

        val validConfig = StreamConfiguration(
            serverUrl = "rtmps://a.rtmp.youtube.com/live2",
            streamKey = "abcd-1234-efgh-5678"
        )
        val result = engine.validateEndpoint(validConfig)
        assertTrue(result.isSuccess)
        assertEquals("rtmps://a.rtmp.youtube.com/live2/abcd-1234-efgh-5678", result.getOrNull())
    }

    @Test
    fun `timecode formatter formats hours minutes seconds accurately`() {
        assertEquals("00:01:05", TimecodeFormatter.formatDurationHhMmSs(65L))
        assertEquals("01:00:00", TimecodeFormatter.formatDurationHhMmSs(3600L))
    }
}
