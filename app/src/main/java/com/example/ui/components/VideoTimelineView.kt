package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProAudioTrack
import com.example.core.model.ProOverlayItem
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoOverlayKind
import com.example.core.model.VideoTransitionKind
import com.example.engine.video.VideoProProcessors
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.SuccessGreen
import kotlin.math.roundToInt

@Composable
fun VideoTimelineView(
    clips: List<VideoClipItem>,
    audioTracks: List<ProAudioTrack>,
    overlays: List<ProOverlayItem>,
    selectedClipIndex: Int,
    selectedClipIndices: Set<Int>,
    multiSelectMode: Boolean,
    selectedAudioId: String?,
    selectedOverlayId: String?,
    currentPosMs: Long,
    totalDurationMs: Long,
    timelineZoom: Float,
    snapEnabled: Boolean,
    toolsExpanded: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onToggleSnap: () -> Unit,
    onTimelineZoomChange: (Float) -> Unit,
    onToggleToolsExpanded: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onSelectClip: (Int) -> Unit,
    onSwapClips: (Int, Int) -> Unit,
    onSelectAudio: (String) -> Unit,
    onNudgeAudioStart: (String, Long) -> Unit,
    onSelectOverlay: (String) -> Unit,
    onAddVideo: () -> Unit,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pxPerSec = 34f * timelineZoom.coerceIn(0.5f, 3.0f)
    val dpPerSec: Dp = pxPerSec.dp
    val density = LocalDensity.current
    val pxPerMs = with(density) { (dpPerSec / 1000f).toPx() }
    val scrollState = rememberScrollState()

    val safeTotalMs = totalDurationMs.coerceAtLeast(6000L)
    val trackWidthDp = ((safeTotalMs / 1000f) * dpPerSec.value).coerceAtLeast(320f).dp

    Surface(
        color = Color(0xFF0E1726),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(bottom = 4.dp)) {
            // Compact Timeline Toolbar: Timecode + Undo/Redo + Multi-Select + Snap + Zoom + Import + Collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${formatTimeShort(currentPosMs)} / ${formatTimeShort(totalDurationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Filled.Undo,
                            contentDescription = "Desfazer",
                            tint = if (canUndo) Color.White else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Filled.Redo,
                            contentDescription = "Refazer",
                            tint = if (canRedo) Color.White else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleMultiSelect,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Checklist,
                            contentDescription = "Seleção múltipla",
                            tint = if (multiSelectMode) OrangeAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleSnap,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.GridOn,
                            contentDescription = "Alinhar elementos",
                            tint = if (snapEnabled) CyanGlow else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = { onTimelineZoomChange((timelineZoom - 0.35f).coerceAtLeast(0.5f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Reduzir timeline", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                        onClick = { onTimelineZoomChange((timelineZoom + 0.35f).coerceAtMost(3.0f)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Ampliar timeline", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                    }
                    TextButton(
                        onClick = onAddVideo,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.VideoCall, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Vídeo", color = OrangeAccent, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = onAddPhoto,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Foto", color = CyanGlow, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = onToggleToolsExpanded,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (toolsExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = if (toolsExpanded) "Minimizar painel" else "Expandir painel",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Scrollable Multi-Track Timeline Canvas + Scrubber Playhead
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp)
            ) {
                Box(modifier = Modifier.width(trackWidthDp + 48.dp)) {
                    val textItems = overlays.filter {
                        it.kind == VideoOverlayKind.TEXT || it.kind == VideoOverlayKind.SUBTITLE || it.kind == VideoOverlayKind.WATERMARK
                    }
                    val graphicItems = overlays.filter {
                        it.kind != VideoOverlayKind.TEXT && it.kind != VideoOverlayKind.SUBTITLE && it.kind != VideoOverlayKind.WATERMARK
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Ruler Bar
                        val totalSecs = (safeTotalMs / 1000L).toInt().coerceAtLeast(6)
                        Box(
                            modifier = Modifier
                                .width(trackWidthDp)
                                .height(16.dp)
                                .pointerInput(pxPerMs, safeTotalMs) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaMs = (dragAmount / pxPerMs.coerceAtLeast(0.01f)).toLong()
                                        val target = (currentPosMs + deltaMs).coerceIn(0L, safeTotalMs)
                                        onSeekToMs(if (snapEnabled) ((target + 250L) / 500L) * 500L else target)
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                for (s in 0..totalSecs) {
                                    val x = s * 1000f * pxPerMs
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.35f),
                                        start = Offset(x, 8f),
                                        end = Offset(x, 16f),
                                        strokeWidth = 2f
                                    )
                                }
                            }
                        }

                        // Track 1: Video & Photo Filmstrips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            clips.forEachIndexed { idx, clip ->
                                val activeDur = ((clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1000L) / clip.speed.coerceIn(0.25f, 4f)).toLong()
                                val itemW = ((activeDur / 1000f) * dpPerSec.value).coerceIn(68f, 340f).dp
                                val isSel = idx == selectedClipIndex
                                val isMultiSel = idx in selectedClipIndices

                                VideoFilmstripCell(
                                    clip = clip,
                                    width = itemW,
                                    isSelected = isSel,
                                    isMultiSelected = isMultiSel,
                                    canMoveRight = idx < clips.lastIndex,
                                    onClick = { onSelectClip(idx) },
                                    onMoveRight = { if (idx < clips.lastIndex) onSwapClips(idx, idx + 1) }
                                )

                                if (clip.transition != VideoTransitionKind.NONE && idx < clips.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1F2D45))
                                            .border(1.dp, CyanGlow, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        // Track 2: Text, Subtitles, Watermarks
                        if (textItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .width(trackWidthDp)
                                    .height(24.dp)
                            ) {
                                textItems.forEach { ov ->
                                    val offsetDp = ((ov.startMs / 1000f) * dpPerSec.value).dp
                                    val lenDp = (((ov.endMs - ov.startMs).coerceAtLeast(600L) / 1000f) * dpPerSec.value).coerceAtLeast(64f).dp
                                    val isSel = ov.id == selectedOverlayId
                                    Surface(
                                        color = if (ov.kind == VideoOverlayKind.SUBTITLE) Color(0xFF1D4ED8) else Color(0xFF7C3AED),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .offset(x = offsetDp)
                                            .width(lenDp)
                                            .height(22.dp)
                                            .border(if (isSel) 1.5.dp else 0.dp, Color.White, RoundedCornerShape(6.dp))
                                            .clickable { onSelectOverlay(ov.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Subtitles, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                ov.text.ifBlank { ov.title },
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Track 3: Stickers, Emojis, Images, PiP, Verified Badge, Logos, Shapes
                        if (graphicItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .width(trackWidthDp)
                                    .height(24.dp)
                            ) {
                                graphicItems.forEach { ov ->
                                    val offsetDp = ((ov.startMs / 1000f) * dpPerSec.value).dp
                                    val lenDp = (((ov.endMs - ov.startMs).coerceAtLeast(600L) / 1000f) * dpPerSec.value).coerceAtLeast(64f).dp
                                    val isSel = ov.id == selectedOverlayId
                                    Surface(
                                        color = Color(0xFF0F766E),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .offset(x = offsetDp)
                                            .width(lenDp)
                                            .height(22.dp)
                                            .border(if (isSel) 1.5.dp else 0.dp, CyanGlow, RoundedCornerShape(6.dp))
                                            .clickable { onSelectOverlay(ov.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${ov.kind.label}: ${ov.title}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Track 4+: Multi-track Audio with Real Waveforms (drag horizontally to sync!)
                        audioTracks.forEach { trk ->
                            Box(
                                modifier = Modifier
                                    .width(trackWidthDp)
                                    .height(30.dp)
                            ) {
                                val offsetDp = ((trk.startMs / 1000f) * dpPerSec.value).dp
                                val lenDp = (((trk.activeDurationMs) / 1000f) * dpPerSec.value).coerceAtLeast(72f).dp
                                val isSel = trk.id == selectedAudioId
                                val laneColor = when (trk.sourceType) {
                                    "voiceover", "tts" -> Color(0xFF9333EA)
                                    "sfx" -> OrangeAccent
                                    else -> Color(0xFF0284C7)
                                }
                                Surface(
                                    color = laneColor.copy(alpha = if (trk.muted) 0.35f else 0.85f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .offset(x = offsetDp)
                                        .width(lenDp)
                                        .height(28.dp)
                                        .border(if (isSel) 1.5.dp else 0.dp, Color.White, RoundedCornerShape(6.dp))
                                        .pointerInput(trk.id, pxPerMs, snapEnabled) {
                                            detectHorizontalDragGestures { change, dragAmount ->
                                                change.consume()
                                                val deltaMs = (dragAmount / pxPerMs.coerceAtLeast(0.01f)).toLong()
                                                onNudgeAudioStart(trk.id, deltaMs)
                                            }
                                        }
                                        .clickable { onSelectAudio(trk.id) }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                                        // Real PCM Waveform bars
                                        if (trk.waveformPeaks.isNotEmpty()) {
                                            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 4.dp, vertical = 3.dp)) {
                                                val count = trk.waveformPeaks.size
                                                val barW = (size.width / count.coerceAtLeast(1)) * 0.65f
                                                val stepW = size.width / count.coerceAtLeast(1)
                                                trk.waveformPeaks.forEachIndexed { i, p ->
                                                    val bh = (p.coerceIn(0.1f, 1f) * size.height)
                                                    val top = (size.height - bh) / 2f
                                                    drawRect(
                                                        color = Color.White.copy(alpha = 0.35f),
                                                        topLeft = Offset(i * stepW, top),
                                                        size = Size(barW.coerceAtLeast(1.5f), bh)
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (trk.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.Filled.GraphicEq,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                trk.title,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Draggable Vertical Playhead Scrubber Cursor
                    val playheadPx = (currentPosMs * pxPerMs).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(playheadPx - 6, 0) }
                            .width(12.dp)
                            .height(48.dp + (if (textItems.isNotEmpty()) 28.dp else 0.dp) + (if (graphicItems.isNotEmpty()) 28.dp else 0.dp) + (audioTracks.size * 34).dp)
                            .pointerInput(pxPerMs, safeTotalMs, snapEnabled) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaMs = (dragAmount / pxPerMs.coerceAtLeast(0.01f)).toLong()
                                    val raw = (currentPosMs + deltaMs).coerceIn(0L, safeTotalMs)
                                    onSeekToMs(if (snapEnabled) ((raw + 250L) / 500L) * 500L else raw)
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, OrangeAccent, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoFilmstripCell(
    clip: VideoClipItem,
    width: Dp,
    isSelected: Boolean,
    isMultiSelected: Boolean,
    canMoveRight: Boolean,
    onClick: () -> Unit,
    onMoveRight: () -> Unit
) {
    val context = LocalContext.current
    val thumbs by produceState<List<Bitmap>>(initialValue = emptyList(), clip.clipId, clip.uriString) {
        value = VideoProProcessors.extractClipThumbnails(context, clip, count = 4)
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(
                width = if (isSelected || isMultiSelected) 2.dp else 1.dp,
                color = when {
                    isMultiSelected -> SuccessGreen
                    isSelected -> OrangeAccent
                    else -> Color.White.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
    ) {
        if (thumbs.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                thumbs.forEach { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = clip.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Movie, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(18.dp))
            }
        }

        // Dark gradient readability strip
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.56f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "${clip.title} • %.1fx".format(clip.speed),
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isMultiSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selecionado",
                tint = SuccessGreen,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .size(14.dp)
            )
        }
        if (isSelected && canMoveRight) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .clickable { onMoveRight() }
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text("→", color = CyanGlow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatTimeShort(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    val tenth = (ms % 1000L) / 100L
    return "%02d:%02d.%d".format(min, sec, tenth)
}
