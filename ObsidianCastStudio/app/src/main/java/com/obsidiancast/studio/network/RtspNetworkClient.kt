package com.obsidiancast.studio.network

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource

/**
 * Hardware-accelerated RTSP network camera client powered by AndroidX Media3 ExoPlayer RTSP.
 * Supports low-latency TCP interleaved and UDP RTP transport for PTZ and IP cameras.
 */
class RtspNetworkClient(private val context: Context) {

    fun createLowLatencyPlayer(rtspUrl: String, forceTcpTransport: Boolean = true): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        val mediaItem = MediaItem.fromUri(rtspUrl)
        val mediaSource = RtspMediaSource.Factory()
            .setForceUseRtpTcp(forceTcpTransport)
            .setTimeoutMs(4_000L)
            .createMediaSource(mediaItem)

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
        return player
    }
}
