package com.obsidiancast.studio.audio

import android.annotation.SuppressLint
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Low-latency 48kHz 16-bit PCM AudioRecord capture loop with USB Audio Class (UAC2) preferred device routing.
 */
class MultiSourceAudioCaptureEngine(
    private val scope: CoroutineScope,
    private val audioManager: AudioManager? = null
) {
    private var captureJob: Job? = null
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun startPcmCapture(
        sampleRateHz: Int = 48_000,
        onPcmFrame: (ShortArray, Float, Float) -> Unit
    ) {
        if (captureJob?.isActive == true) return
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufSize = AudioRecord.getMinBufferSize(sampleRateHz, channelConfig, encoding)
            .coerceAtLeast(4096)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateHz,
            channelConfig,
            encoding,
            minBufSize * 2
        )

        // Route to external USB Audio Interface if connected
        audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)?.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }?.let { usbAudioDevice ->
            recorder.preferredDevice = usbAudioDevice
        }

        audioRecord = recorder
        recorder.startRecording()

        captureJob = scope.launch(Dispatchers.Default) {
            val buffer = ShortArray(1024)
            while (isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val (peakDb, rmsDb) = PcmAudioMixer.calculateMetersDbFs(buffer, read)
                    onPcmFrame(buffer, peakDb, rmsDb)
                }
            }
        }
    }

    fun stopPcmCapture() {
        captureJob?.cancel()
        captureJob = null
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
    }
}
