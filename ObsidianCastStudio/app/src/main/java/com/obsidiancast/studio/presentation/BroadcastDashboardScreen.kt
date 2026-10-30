package com.obsidiancast.studio.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidiancast.studio.domain.*
import com.obsidiancast.studio.utils.TimecodeFormatter

private val ObsidianBg = Color(0xFF080B12)
private val ObsidianSurface = Color(0xFF111827)
private val GlassBorder = Color(0x1FFFFFFF)
private val AccentOrange = Color(0xFFFF7A35)
private val SecondaryIndigo = Color(0xFF7C8CFF)
private val LiveGreen = Color(0xFF22C55E)
private val DangerRed = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)

@Composable
fun BroadcastDashboardScreen(viewModel: StudioViewModel) {
    val cameras by viewModel.cameraSources.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val activeSceneId by viewModel.activeSceneId.collectAsState()
    val audioChannels by viewModel.audioChannels.collectAsState()
    val switcher by viewModel.switcherState.collectAsState()
    val streamStats by viewModel.streamStats.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val hwReport by viewModel.hardwareCapabilities.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Top Navigation & Live Status Bar
            TopBroadcastBar(
                streamStats = streamStats,
                recordingState = recordingState,
                hwReport = hwReport,
                onToggleStream = { viewModel.toggleLiveStreaming() },
                onToggleRecord = { viewModel.toggleRecording() }
            )

            // 2. Dual Monitors (Preview & Program) + Transition Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonitorViewportCard(
                    title = "PREVIEW",
                    badgeColor = SecondaryIndigo,
                    activeCamera = cameras.find { it.id == switcher.previewSourceId } ?: cameras.first(),
                    layoutMode = switcher.layoutMode,
                    modifier = Modifier.weight(1f)
                )

                TransitionControlStrip(
                    activeLayout = switcher.layoutMode,
                    onCut = { viewModel.cutTransition() },
                    onFade = { viewModel.fadeTransition() },
                    onAuto = { viewModel.autoTransition() },
                    onLayoutChange = { viewModel.setCompositorLayout(it) }
                )

                MonitorViewportCard(
                    title = "PROGRAM · ON AIR",
                    badgeColor = DangerRed,
                    activeCamera = cameras.find { it.id == switcher.programSourceId } ?: cameras.first(),
                    layoutMode = switcher.layoutMode,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Lower Deck: 4-Camera Input Grid + Scene Manager + PCM Audio Mixer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CameraInputGridPanel(
                    cameras = cameras,
                    programId = switcher.programSourceId,
                    previewId = switcher.previewSourceId,
                    onSelectPreview = { viewModel.selectPreviewCamera(it) },
                    modifier = Modifier.weight(0.42f)
                )

                SceneSelectionPanel(
                    scenes = scenes,
                    activeSceneId = activeSceneId,
                    onSelectScene = { viewModel.activateScene(it) },
                    onDuplicateScene = { viewModel.duplicateActiveScene() },
                    modifier = Modifier.weight(0.26f)
                )

                AudioMixerPanel(
                    channels = audioChannels,
                    onGainChange = { id, db -> viewModel.setAudioFaderGain(id, db) },
                    onToggleMute = { id -> viewModel.toggleAudioMute(id) },
                    modifier = Modifier.weight(0.32f)
                )
            }
        }
    }
}

@Composable
private fun TopBroadcastBar(
    streamStats: LiveStreamRuntimeStats,
    recordingState: RecordingRuntimeState,
    hwReport: HardwareCapabilityReport,
    onToggleStream: () -> Unit,
    onToggleRecord: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "ObsidianCast Studio",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${hwReport.deviceSoCTier} · USB ${hwReport.estimatedActiveUsbBandwidthMbps}/${hwReport.maxUsbIsochronousBandwidthMbps} Mbps",
                color = TextMuted,
                fontSize = 12.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (streamStats.isStreaming) {
                    "LIVE ${TimecodeFormatter.formatDurationHhMmSs(streamStats.elapsedDurationSec)} · ${streamStats.currentVideoBitrateKbps} kbps"
                } else {
                    "STREAM STANDBY"
                },
                color = if (streamStats.isStreaming) LiveGreen else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = onToggleRecord,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recordingState.isRecording) DangerRed else ObsidianBg
                )
            ) {
                Text(
                    text = if (recordingState.isRecording) {
                        "REC ${TimecodeFormatter.formatDurationHhMmSs(recordingState.durationSec)}"
                    } else {
                        "Start Record"
                    },
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onToggleStream,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (streamStats.isStreaming) DangerRed else AccentOrange
                )
            ) {
                Text(
                    text = if (streamStats.isStreaming) "End Stream" else "Go Live (RTMP)",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MonitorViewportCard(
    title: String,
    badgeColor: Color,
    activeCamera: CameraInputSource,
    layoutMode: CompositorLayoutMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                text = "${activeCamera.resolutionWidth}x${activeCamera.resolutionHeight} · ${activeCamera.measuredFps} FPS · ${layoutMode.label}",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activeCamera.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TransitionControlStrip(
    activeLayout: CompositorLayoutMode,
    onCut: () -> Unit,
    onFade: () -> Unit,
    onAuto: () -> Unit,
    onLayoutChange: (CompositorLayoutMode) -> Unit
) {
    Column(
        modifier = Modifier
            .width(124.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SWITCHER", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Button(onClick = onCut, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)) {
            Text("CUT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = onFade, modifier = Modifier.fillMaxWidth()) {
            Text("FADE 450ms", fontSize = 11.sp, color = TextPrimary)
        }
        OutlinedButton(onClick = onAuto, modifier = Modifier.fillMaxWidth()) {
            Text("AUTO 800ms", fontSize = 11.sp, color = TextPrimary)
        }
        Divider(color = GlassBorder)
        CompositorLayoutMode.values().forEach { mode ->
            val selected = mode == activeLayout
            Text(
                text = mode.label,
                color = if (selected) AccentOrange else TextMuted,
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLayoutChange(mode) }
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CameraInputGridPanel(
    cameras: List<CameraInputSource>,
    programId: String,
    previewId: String,
    onSelectPreview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("MULTI-CAMERA INPUTS (4 SLOTS)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cameras, key = { it.id }) { cam ->
                val isPgm = cam.id == programId
                val isPvw = cam.id == previewId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(
                            width = 1.dp,
                            color = when {
                                isPgm -> DangerRed
                                isPvw -> SecondaryIndigo
                                else -> GlassBorder
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectPreview(cam.id) }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(cam.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${cam.resolutionWidth}x${cam.resolutionHeight} · ${cam.targetFps}fps · ${cam.connectionState.name}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = when {
                            isPgm -> "PGM"
                            isPvw -> "PVW"
                            else -> "STBY"
                        },
                        color = when {
                            isPgm -> DangerRed
                            isPvw -> SecondaryIndigo
                            else -> TextMuted
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneSelectionPanel(
    scenes: List<BroadcastScene>,
    activeSceneId: String,
    onSelectScene: (BroadcastScene) -> Unit,
    onDuplicateScene: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SCENES", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "Duplicate",
                color = AccentOrange,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onDuplicateScene() }
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(scenes, key = { it.id }) { scene ->
                val active = scene.id == activeSceneId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color(0x26FF7A35) else ObsidianBg)
                        .border(1.dp, if (active) AccentOrange else GlassBorder, RoundedCornerShape(8.dp))
                        .clickable { onSelectScene(scene) }
                        .padding(10.dp)
                ) {
                    Text(scene.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(scene.layoutMode.label, color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AudioMixerPanel(
    channels: List<AudioMixerChannel>,
    onGainChange: (String, Float) -> Unit,
    onToggleMute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("PCM AUDIO MIXER", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(channels, key = { it.id }) { ch ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ch.name, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (ch.isMuted) "MUTED" else String.format("%.1f dB", ch.faderGainDb),
                            color = if (ch.isMuted) DangerRed else LiveGreen,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onToggleMute(ch.id) }
                        )
                    }
                    Slider(
                        value = ch.faderGainDb,
                        onValueChange = { onGainChange(ch.id, it) },
                        valueRange = -60f..6f
                    )
                }
            }
        }
    }
}
