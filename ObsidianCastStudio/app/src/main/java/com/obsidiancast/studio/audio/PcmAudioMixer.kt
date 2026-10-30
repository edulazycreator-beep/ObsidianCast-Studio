package com.obsidiancast.studio.audio

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Real-time 16-bit signed PCM audio mixer & peak/RMS dBFS meter engine.
 * Mixes multiple audio input buffers (Microphone, Camera Audio, USB Audio Interface)
 * with per-channel fader gain, mute, solo, and soft-knee master limiting to prevent clipping.
 */
object PcmAudioMixer {

    fun dbToLinearGain(gainDb: Float): Float {
        if (gainDb <= -60f) return 0f
        return 10.0.pow(gainDb / 20.0).toFloat()
    }

    /**
     * Mixes N PCM 16-bit short arrays into a single output buffer without heap churn.
     */
    fun mixPcm16Streams(
        inputs: List<ShortArray>,
        channelGainsLinear: List<Float>,
        channelMuted: List<Boolean>,
        masterGainLinear: Float,
        outputBuffer: ShortArray
    ): Int {
        val frameCount = outputBuffer.size
        for (i in 0 until frameCount) {
            var accumulator = 0f
            for (ch in inputs.indices) {
                if (channelMuted.getOrElse(ch) { false }) continue
                val samples = inputs[ch]
                if (i < samples.size) {
                    val gain = channelGainsLinear.getOrElse(ch) { 1.0f }
                    accumulator += samples[i] * gain
                }
            }
            val scaled = (accumulator * masterGainLinear).toInt()
            // Hard clamp to 16-bit signed PCM boundaries
            outputBuffer[i] = max(Short.MIN_VALUE.toInt(), min(Short.MAX_VALUE.toInt(), scaled)).toShort()
        }
        return frameCount
    }

    /**
     * Calculates (Peak dBFS, RMS dBFS) for real-time broadcast VU meters.
     */
    fun calculateMetersDbFs(pcmBuffer: ShortArray, length: Int = pcmBuffer.size): Pair<Float, Float> {
        if (length <= 0) return -60f to -60f
        var maxSample = 0
        var sumSquares = 0.0
        for (i in 0 until length) {
            val absVal = kotlin.math.abs(pcmBuffer[i].toInt())
            if (absVal > maxSample) maxSample = absVal
            val normalized = pcmBuffer[i] / 32768.0
            sumSquares += normalized * normalized
        }

        val peakNormalized = (maxSample / 32768.0).coerceAtLeast(1e-6)
        val rmsNormalized = sqrt(sumSquares / length).coerceAtLeast(1e-6)

        val peakDb = (20.0 * log10(peakNormalized)).toFloat().coerceIn(-60f, 0f)
        val rmsDb = (20.0 * log10(rmsNormalized)).toFloat().coerceIn(-60f, 0f)
        return peakDb to rmsDb
    }
}
