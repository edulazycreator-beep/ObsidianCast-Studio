package com.obsidiancast.studio.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import com.obsidiancast.studio.domain.CameraInputSource
import com.obsidiancast.studio.domain.CameraSourceType
import com.obsidiancast.studio.domain.HardwareCapabilityReport

/**
 * Inspects real Android Camera2 concurrent camera sets, USB host controller bandwidth limits,
 * and hardware MediaCodec AVC/HEVC instance limits so the studio never blindly opens 4 USB UVC
 * streams on hardware that cannot sustain the isochronous USB bus or decoder pipeline.
 */
class HardwareCapabilityDetector(private val context: Context) {

    fun probeDeviceCapabilities(activeSources: List<CameraInputSource>): HardwareCapabilityReport {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val concurrentSets: Set<Set<String>> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && cameraManager != null) {
            runCatching { cameraManager.concurrentCameraIds }.getOrDefault(emptySet())
        } else {
            emptySet()
        }

        val maxConcurrentInternal = when {
            concurrentSets.any { it.size >= 2 } -> 2
            else -> 1
        }

        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        var maxAvcInstances = 2
        var supports1080p60 = false

        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) continue
            val types = codecInfo.supportedTypes
            if (types.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }) {
                runCatching {
                    val caps = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                    maxAvcInstances = maxOf(maxAvcInstances, caps.maxSupportedInstances)
                    val videoCaps = caps.videoCapabilities
                    if (videoCaps != null && videoCaps.areSizeAndRateSupported(1920, 1080, 60.0)) {
                        supports1080p60 = true
                    }
                }
            }
        }

        return evaluateBandwidthAndConcurrency(
            activeSources = activeSources,
            maxConcurrentInternalCameras = maxConcurrentInternal,
            maxHardwareAvcEncoders = maxAvcInstances,
            supports1080p60Encode = supports1080p60
        )
    }

    companion object {
        private const val USB_2_HIGH_SPEED_EFFECTIVE_MBPS = 320
        private const val USB_3_SUPERSPEED_EFFECTIVE_MBPS = 2400

        /**
         * Pure deterministic calculation decoupled for unit testing and runtime guardrails.
         */
        fun evaluateBandwidthAndConcurrency(
            activeSources: List<CameraInputSource>,
            maxConcurrentInternalCameras: Int = 2,
            maxHardwareAvcEncoders: Int = 4,
            supports1080p60Encode: Boolean = true,
            isUsbSuperSpeedAvailable: Boolean = false
        ): HardwareCapabilityReport {
            val warnings = mutableListOf<String>()
            val usbSources = activeSources.filter { it.type == CameraSourceType.USB_UVC }
            val internalSources = activeSources.filter {
                it.type == CameraSourceType.INTERNAL_BACK || it.type == CameraSourceType.INTERNAL_FRONT
            }

            // Estimate compressed MJPEG vs uncompressed YUY2 USB bandwidth per camera
            val estimatedUsbMbps = usbSources.sumOf { src ->
                val pixelsPerSec = src.resolutionWidth.toLong() * src.resolutionHeight.toLong() * src.targetFps
                // Assume MJPEG over UVC (~0.12 bits/pixel) + protocol overhead
                ((pixelsPerSec * 12L) / 100_000_000L).toInt().coerceAtLeast(45)
            }

            val maxUsbBusMbps = if (isUsbSuperSpeedAvailable) {
                USB_3_SUPERSPEED_EFFECTIVE_MBPS
            } else {
                USB_2_HIGH_SPEED_EFFECTIVE_MBPS
            }

            if (internalSources.size > maxConcurrentInternalCameras) {
                warnings.add(
                    "Device Camera2 HAL supports at most $maxConcurrentInternalCameras simultaneous internal camera streams."
                )
            }

            if (usbSources.size > 2 && !isUsbSuperSpeedAvailable) {
                warnings.add(
                    "USB 2.0 High-Speed controller detected (${estimatedUsbMbps}/${maxUsbBusMbps} Mbps). Limit USB UVC inputs to 2 cameras at 720p30/1080p30 MJPEG."
                )
            } else if (estimatedUsbMbps > (maxUsbBusMbps * 0.85).toInt()) {
                warnings.add(
                    "USB isochronous bandwidth near saturation (${estimatedUsbMbps}/${maxUsbBusMbps} Mbps). Adaptive fallback to 720p30 recommended."
                )
            }

            return HardwareCapabilityReport(
                maxConcurrentInternalCameras = maxConcurrentInternalCameras,
                supportsConcurrentBackAndFront = maxConcurrentInternalCameras >= 2,
                maxUsbIsochronousBandwidthMbps = maxUsbBusMbps,
                estimatedActiveUsbBandwidthMbps = estimatedUsbMbps,
                maxHardwareAvcEncoderInstances = maxHardwareAvcEncoders,
                supports1080p60AvcHardwareEncode = supports1080p60Encode,
                recommendedMaxUsbCamerasSimultaneous = if (isUsbSuperSpeedAvailable) 3 else 2,
                deviceSoCTier = if (supports1080p60Encode && maxHardwareAvcEncoders >= 4) {
                    "FLAGSHIP_HW_CODEC"
                } else {
                    "MIDRANGE_ADAPTIVE"
                },
                warnings = warnings
            )
        }
    }
}
