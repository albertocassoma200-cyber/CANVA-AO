package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.core.audio.LocalSoundEffects
import com.example.core.model.DesignProject
import com.example.core.model.ProAudioTrack
import com.example.core.model.ProOverlayItem
import com.example.core.model.SpeedCurvePreset
import com.example.core.model.TemplateSeedCatalog
import com.example.core.model.VideoBackgroundAnimatedStyle
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoEditorSnapshot
import com.example.core.model.VideoExportQuality
import com.example.core.model.VideoOverlayKind
import com.example.core.model.VideoTransitionKind
import com.example.data.repository.AiStudioRepository
import com.example.engine.audio.VideoAudioStudioEngine
import com.example.engine.audio.VideoAudioTrackMixer
import com.example.engine.audio.VoiceRecorderController
import com.example.engine.graphics.BitmapRenderer
import com.example.engine.graphics.ExportedFileResult
import com.example.engine.video.ImportedFontInfo
import com.example.engine.video.VideoAutosaveStore
import com.example.engine.video.VideoExportEngine
import com.example.engine.video.VideoProProcessors
import com.example.engine.video.VideoResolutionPreset
import com.example.ui.components.VideoPreviewCanvas
import com.example.ui.components.VideoTimelineView
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(UnstableApi::class)
@Composable
fun VideoEditorScreen(
    initialProject: DesignProject,
    creditsBalance: Long,
    onBack: () -> Unit,
    onSaveToCloud: (DesignProject) -> Unit,
    onReserveExport: suspend (projectId: String, title: String, exportType: String, resolution: String, cost: Long, fileName: String) -> Result<String>,
    onCompleteExport: suspend (exportId: String, cost: Long, exportType: String, fileSizeBytes: Long) -> Result<Long>,
    onFailExport: suspend (exportId: String, cancelled: Boolean) -> Unit,
    onGoToCredits: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sound = LocalSoundEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exportEngine = remember(context) { VideoExportEngine(context) }
    val aiRepository = remember { AiStudioRepository() }
    val audioMixer = remember(context) { VideoAudioTrackMixer(context) }
    val voiceRecorder = remember(context) { VoiceRecorderController(context) }

    val draftKey = remember(initialProject.id) { initialProject.id.ifBlank { "new_video_draft" } }

    var projectTitle by remember(initialProject) { mutableStateOf(initialProject.title) }
    var aspectRatio by remember(initialProject) {
        mutableStateOf(inferVideoAspectRatio(initialProject.width, initialProject.height))
    }
    var bgColorStart by remember(initialProject) {
        mutableStateOf(initialProject.backgroundColorHex.ifBlank { "#0A1628" })
    }
    var bgColorEnd by remember(initialProject) { mutableStateOf(initialProject.backgroundGradientHex) }
    var bgImageUri by remember(initialProject) {
        mutableStateOf(
            initialProject.layers.firstOrNull { it["layerId"] == "video_bg_image" }?.get("imageUri") as? String
        )
    }
    var animatedBgStyle by remember(initialProject) {
        val rawStyle = initialProject.layers.firstOrNull { it["layerId"] == "video_bg_settings" }
            ?.get("animatedBgStyle") as? String
        mutableStateOf(
            enumValues<VideoBackgroundAnimatedStyle>().firstOrNull { it.name == rawStyle }
                ?: VideoBackgroundAnimatedStyle.NONE
        )
    }

    val clips = remember(initialProject) {
        mutableStateListOf<VideoClipItem>().apply {
            addAll(initialProject.videoClips.map { VideoClipItem.fromMap(it) })
        }
    }
    val audioTracks = remember(initialProject) {
        mutableStateListOf<ProAudioTrack>().apply {
            initialProject.layers.filter { it["type"] == "AUDIO_TRACK" }.forEach { map ->
                add(ProAudioTrack.fromLayerMap(map))
            }
        }
    }
    val overlays = remember(initialProject) {
        mutableStateListOf<ProOverlayItem>().apply {
            initialProject.layers
                .filter {
                    val id = it["layerId"] as? String
                    val type = it["type"] as? String
                    id != "video_bg_image" && id != "video_bg_settings" && type != "AUDIO_TRACK"
                }
                .forEach { map ->
                    add(ProOverlayItem.fromLayerMap(map))
                }
        }
    }
    val importedFonts = remember { mutableStateListOf<ImportedFontInfo>() }

    // Selection & UI state
    var selectedClipIndex by remember(initialProject) { mutableIntStateOf(0) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedClipIndices by remember { mutableStateOf(emptySet<Int>()) }
    var selectedAudioId by remember { mutableStateOf<String?>(null) }
    var selectedOverlayId by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf(ProVideoToolTab.EDIT) }
    var toolsExpanded by remember { mutableStateOf(true) }
    var fullscreenPreview by remember { mutableStateOf(false) }
    var timelineZoom by remember { mutableFloatStateOf(1.0f) }
    var snapEnabled by remember { mutableStateOf(true) }
    var isReplacingClip by remember { mutableStateOf(false) }

    // Undo / Redo Stacks
    val undoStack = remember(initialProject) { mutableStateListOf<VideoEditorSnapshot>() }
    val redoStack = remember(initialProject) { mutableStateListOf<VideoEditorSnapshot>() }

    fun captureSnapshot() = VideoEditorSnapshot(
        clips = clips.toList(),
        audioTracks = audioTracks.toList(),
        overlays = overlays.toList(),
        aspectRatio = aspectRatio,
        bgColorStart = bgColorStart,
        bgColorEnd = bgColorEnd,
        bgImageUri = bgImageUri,
        animatedBgStyle = animatedBgStyle
    )

    fun pushUndo() {
        undoStack.add(captureSnapshot())
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun restoreSnapshot(snap: VideoEditorSnapshot) {
        clips.clear(); clips.addAll(snap.clips)
        audioTracks.clear(); audioTracks.addAll(snap.audioTracks)
        overlays.clear(); overlays.addAll(snap.overlays)
        aspectRatio = snap.aspectRatio
        bgColorStart = snap.bgColorStart
        bgColorEnd = snap.bgColorEnd
        bgImageUri = snap.bgImageUri
        animatedBgStyle = snap.animatedBgStyle
        selectedClipIndex = selectedClipIndex.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
    }

    // Recovery draft check
    var pendingRecovery by remember(initialProject) { mutableStateOf<VideoAutosaveStore.RecoveredDraft?>(null) }
    LaunchedEffect(draftKey) {
        val draft = withContext(Dispatchers.IO) { VideoAutosaveStore.loadDraft(context, draftKey) }
        if (draft != null && (draft.snapshot.clips.isNotEmpty() || draft.snapshot.audioTracks.isNotEmpty() || draft.snapshot.overlays.isNotEmpty())) {
            val savedUpdatedMs = initialProject.updatedAt?.toDate()?.time ?: 0L
            if (draft.savedAtMs > savedUpdatedMs + 5_000L) {
                pendingRecovery = draft
            }
        }
    }

    // Playback, Audio Mixer, Voice Recording, AI & Export states
    var isPlaying by remember(initialProject) { mutableStateOf(false) }
    var currentPosMs by remember(initialProject) { mutableLongStateOf(0L) }
    var totalDurationMs by remember(initialProject) { mutableLongStateOf(0L) }
    var previewingAudioUri by remember { mutableStateOf<String?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var micLevel by remember { mutableFloatStateOf(0.1f) }
    var aiBusy by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // Export state
    var showExportModal by remember { mutableStateOf(false) }
    var exportRes by remember { mutableStateOf(VideoResolutionPreset.FHD_1080) }
    var exportFps by remember { mutableIntStateOf(30) }
    var exportQuality by remember { mutableStateOf(VideoExportQuality.HIGH) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableIntStateOf(0) }
    var activeExportId by remember { mutableStateOf<String?>(null) }
    var exportedResult by remember { mutableStateOf<ExportedFileResult?>(null) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    // Lifecycle cleanup
    DisposableEffect(lifecycleOwner, exoPlayer, audioMixer, voiceRecorder) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
                isPlaying = false
                audioMixer.updatePlayback(audioTracks.toList(), currentPosMs, false)
                audioMixer.stopSinglePreview()
                previewingAudioUri = null
                if (isRecordingVoice) {
                    voiceRecorder.stopAndDiscard()
                    isRecordingVoice = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exportEngine.cancelExport()
            exoPlayer.release()
            audioMixer.releaseAll()
            voiceRecorder.stopAndDiscard()
        }
    }

    // Local Autosave (debounced 1.5s)
    LaunchedEffect(
        clips.toList(),
        audioTracks.toList(),
        overlays.toList(),
        aspectRatio,
        bgColorStart,
        bgColorEnd,
        bgImageUri,
        animatedBgStyle,
        projectTitle
    ) {
        delay(1500L)
        if (clips.isNotEmpty() || audioTracks.isNotEmpty() || overlays.isNotEmpty()) {
            val snap = captureSnapshot()
            withContext(Dispatchers.IO) {
                VideoAutosaveStore.saveDraft(context, draftKey, projectTitle, snap)
            }
        }
    }

    // Sync Audio Mixer whenever tracks change
    LaunchedEffect(audioTracks.toList()) {
        audioMixer.syncTracks(audioTracks.toList())
        if (selectedAudioId == null && audioTracks.isNotEmpty()) {
            selectedAudioId = audioTracks.first().id
        }
    }

    // Helper to compute active clip index and relative progress from currentPosMs
    fun resolveClipTimingAt(posMs: Long): Triple<Int, Long, Float> {
        if (clips.isEmpty()) return Triple(0, 0L, 0f)
        var acc = 0L
        for (i in clips.indices) {
            val c = clips[i]
            val cDur = ((c.trimEndMs - c.trimStartMs).coerceAtLeast(1000L) / c.speed.coerceIn(0.25f, 4f)).toLong().coerceAtLeast(400L)
            if (posMs < acc + cDur || i == clips.lastIndex) {
                val rel = (posMs - acc).coerceIn(0L, cDur)
                val prog = (rel.toFloat() / cDur.toFloat()).coerceIn(0f, 1f)
                return Triple(i, rel, prog)
            }
            acc += cDur
        }
        return Triple(0, 0L, 0f)
    }

    // Sync ExoPlayer playlist with video clips
    LaunchedEffect(clips.toList()) {
        val videoClipsOnly = clips.filter { it.mediaType == "video" }
        if (videoClipsOnly.isNotEmpty()) {
            val items = videoClipsOnly.map { clip ->
                val uri = TemplateSeedCatalog.resolvePlayableUri(context, clip.uriString) ?: Uri.parse(clip.uriString)
                val b = MediaItem.Builder().setUri(uri)
                if (clip.trimEndMs > clip.trimStartMs) {
                    b.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                            .setEndPositionMs(clip.trimEndMs)
                            .build()
                    )
                }
                b.build()
            }
            exoPlayer.setMediaItems(items)
            exoPlayer.prepare()
        } else {
            exoPlayer.clearMediaItems()
        }
        totalDurationMs = clips.sumOf {
            ((it.trimEndMs - it.trimStartMs).coerceAtLeast(1000L) / it.speed.coerceIn(0.25f, 4f)).toLong()
        }
    }

    // Dynamic playback loop: updates timeline playhead, Speed Curves, ExoPlayer volume/speed, and Multi-Track Audio Mixer!
    LaunchedEffect(isPlaying, clips.toList(), audioTracks.toList()) {
        while (isPlaying) {
            val total = totalDurationMs.coerceAtLeast(1000L)
            val (clipIdx, relMs, clipProg) = resolveClipTimingAt(currentPosMs)
            val clip = clips.getOrNull(clipIdx)

            if (clip != null) {
                val curve = SpeedCurvePreset.entries.firstOrNull { it.key == clip.speedCurve } ?: SpeedCurvePreset.LINEAR
                val dynamicSpeed = (clip.speed * curve.speedMultiplierAt(clipProg)).coerceIn(0.25f, 4f)
                if (clip.mediaType == "video") {
                    exoPlayer.volume = if (clip.muted) 0f else clip.volume.coerceIn(0f, 1f)
                    exoPlayer.setPlaybackSpeed(dynamicSpeed)
                    if (!exoPlayer.isPlaying && exoPlayer.mediaItemCount > 0) {
                        val videoIdx = clips.take(clipIdx + 1).count { it.mediaType == "video" } - 1
                        if (videoIdx >= 0 && videoIdx < exoPlayer.mediaItemCount) {
                            exoPlayer.seekTo(videoIdx, (relMs * dynamicSpeed).toLong())
                            exoPlayer.play()
                        }
                    }
                } else if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                }
                selectedClipIndex = clipIdx
            }

            audioMixer.updatePlayback(audioTracks.toList(), currentPosMs, true)
            delay(80L)
            val next = currentPosMs + 80L
            currentPosMs = if (next >= total) {
                audioMixer.seekAllTo(audioTracks.toList(), 0L)
                if (clips.any { it.mediaType == "video" }) exoPlayer.seekTo(0, 0L)
                0L
            } else next
        }
        exoPlayer.pause()
        audioMixer.updatePlayback(audioTracks.toList(), currentPosMs, false)
    }

    // Mic amplitude polling while recording voiceover
    LaunchedEffect(isRecordingVoice) {
        while (isRecordingVoice) {
            micLevel = voiceRecorder.readAmplitudeNormalized()
            delay(70L)
        }
    }

    fun seekTimelineTo(ms: Long) {
        val safeMs = ms.coerceIn(0L, totalDurationMs.coerceAtLeast(1000L))
        currentPosMs = safeMs
        val (idx, relMs, _) = resolveClipTimingAt(safeMs)
        selectedClipIndex = idx
        val clip = clips.getOrNull(idx)
        if (clip?.mediaType == "video") {
            val videoIdx = clips.take(idx + 1).count { it.mediaType == "video" } - 1
            if (videoIdx >= 0 && videoIdx < exoPlayer.mediaItemCount) {
                exoPlayer.seekTo(videoIdx, (relMs * clip.speed).toLong())
            }
        }
        audioMixer.seekAllTo(audioTracks.toList(), safeMs)
    }

    // Activity Launchers
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val dur = withContext(Dispatchers.IO) { extractVideoDurationMs(context, uri).coerceAtLeast(1500L) }
                pushUndo()
                if (isReplacingClip && clips.getOrNull(selectedClipIndex) != null) {
                    val old = clips[selectedClipIndex]
                    clips[selectedClipIndex] = old.copy(
                        uriString = uri.toString(),
                        mediaType = "video",
                        durationMs = dur,
                        trimStartMs = 0L,
                        trimEndMs = dur,
                        reversed = false
                    )
                    isReplacingClip = false
                    onShowMessage("Clipe substituído!")
                } else {
                    clips.add(
                        VideoClipItem(
                            clipId = "vc_${UUID.randomUUID().toString().take(8)}",
                            title = "Vídeo ${clips.size + 1}",
                            uriString = uri.toString(),
                            mediaType = "video",
                            durationMs = dur,
                            trimStartMs = 0L,
                            trimEndMs = dur
                        )
                    )
                    selectedClipIndex = clips.lastIndex
                }
            }
        } else {
            isReplacingClip = false
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pushUndo()
            if (isReplacingClip && clips.getOrNull(selectedClipIndex) != null) {
                val old = clips[selectedClipIndex]
                clips[selectedClipIndex] = old.copy(
                    uriString = uri.toString(),
                    mediaType = "image",
                    durationMs = 4000L,
                    trimStartMs = 0L,
                    trimEndMs = 4000L,
                    reversed = false
                )
                isReplacingClip = false
                onShowMessage("Clipe substituído por foto!")
            } else {
                clips.add(
                    VideoClipItem(
                        clipId = "img_${UUID.randomUUID().toString().take(8)}",
                        title = "Foto ${clips.size + 1}",
                        uriString = uri.toString(),
                        mediaType = "image",
                        durationMs = 4000L,
                        trimStartMs = 0L,
                        trimEndMs = 4000L
                    )
                )
                selectedClipIndex = clips.lastIndex
            }
        } else {
            isReplacingClip = false
        }
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                VideoAudioStudioEngine.importAudioFromDevice(context, uri, currentPosMs).fold(
                    onSuccess = { trk ->
                        pushUndo()
                        audioTracks.add(trk)
                        selectedAudioId = trk.id
                        sound?.playSuccess()
                        onShowMessage("Áudio '${trk.title}' importado para a timeline!")
                    },
                    onFailure = { err ->
                        onShowMessage(err.localizedMessage ?: "Não foi possível importar este áudio.")
                    }
                )
            }
        }
    }

    val pickOverlayImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pushUndo()
            val ov = ProOverlayItem(
                kind = VideoOverlayKind.IMAGE,
                title = "Imagem Camada",
                uriString = uri.toString(),
                startMs = currentPosMs,
                endMs = (currentPosMs + 5000L).coerceAtLeast(currentPosMs + 1500L)
            )
            overlays.add(ov)
            selectedOverlayId = ov.id
            onShowMessage("Imagem adicionada como camada!")
        }
    }

    val pickOverlayVideoPipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val frame = withContext(Dispatchers.IO) {
                    val ret = MediaMetadataRetriever()
                    try {
                        ret.setDataSource(context, uri)
                        ret.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (_: Exception) {
                        null
                    } finally {
                        try { ret.release() } catch (_: Exception) {}
                    }
                }
                val thumbUri = if (frame != null) {
                    BitmapRenderer.saveBitmapToTempUri(context, frame, "pip_thumb").toString()
                } else uri.toString()
                pushUndo()
                val ov = ProOverlayItem(
                    kind = VideoOverlayKind.VIDEO_PIP,
                    title = "Vídeo PiP",
                    uriString = thumbUri,
                    startMs = currentPosMs,
                    endMs = (currentPosMs + 6000L).coerceAtLeast(currentPosMs + 1500L)
                )
                overlays.add(ov)
                selectedOverlayId = ov.id
                onShowMessage("Sobreposição PiP adicionada!")
            }
        }
    }

    val pickBgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pushUndo()
            bgImageUri = uri.toString()
            onShowMessage("Imagem de fundo aplicada!")
        }
    }

    val pickCustomFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                VideoProProcessors.importCustomFontFile(context, uri).fold(
                    onSuccess = { info ->
                        importedFonts.add(info)
                        val selOv = overlays.firstOrNull { it.id == selectedOverlayId }
                        if (selOv != null) {
                            val idx = overlays.indexOf(selOv)
                            if (idx >= 0) overlays[idx] = selOv.copy(customFontUri = info.fileUri)
                        }
                        onShowMessage("Fonte '${info.name}' importada!")
                    },
                    onFailure = { onShowMessage(it.localizedMessage ?: "Selecione um ficheiro .ttf ou .otf válido.") }
                )
            }
        }
    }

    fun startOrStopVoiceRecording() {
        if (isRecordingVoice) {
            scope.launch {
                voiceRecorder.stopRecordingAndCreateTrack(currentPosMs).fold(
                    onSuccess = { trk ->
                        pushUndo()
                        audioTracks.add(trk)
                        selectedAudioId = trk.id
                        sound?.playSuccess()
                        onShowMessage("Locução adicionada à timeline!")
                    },
                    onFailure = { onShowMessage(it.localizedMessage ?: "Falha na gravação de voz.") }
                )
                isRecordingVoice = false
            }
        } else {
            val res = voiceRecorder.startRecording()
            if (res.isSuccess) {
                isRecordingVoice = true
            } else {
                onShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Não foi possível abrir o microfone.")
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startOrStopVoiceRecording()
        else onShowMessage("Permissão de microfone necessária para gravar voz.")
    }

    fun toggleVoiceRecordingWithPermission() {
        if (isRecordingVoice) {
            startOrStopVoiceRecording()
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) startOrStopVoiceRecording()
        else recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Real AI Studio Runners
    fun runAiBgRemovalOrCutout(isSmartCutout: Boolean) {
        val clip = clips.getOrNull(selectedClipIndex)
        if (clip == null) {
            onShowMessage("Selecione primeiro um clipe ou foto na timeline.")
            return
        }
        aiBusy = true
        scope.launch {
            try {
                val (_, relMs, _) = resolveClipTimingAt(currentPosMs)
                val frame = VideoProProcessors.extractFrameAtMs(context, clip, relMs, 720)
                    ?: throw IllegalStateException("Não foi possível obter fotograma deste clipe.")
                val instruction = if (isSmartCutout) {
                    "Cut out the primary person or focal object cleanly with crisp edges and make the background pure solid #00FF00 green screen."
                } else {
                    "Remove the background cleanly and isolate the main subject on a pure solid #00FF00 backdrop."
                }
                aiRepository.editOrRemoveBackground(frame, instruction).fold(
                    onSuccess = { outBmp ->
                        val keyed = withContext(Dispatchers.IO) {
                            VideoProProcessors.applyChromaKeyToBitmap(outBmp, "#00FF00", 0.36f)
                        }
                        val outUri = BitmapRenderer.saveBitmapToTempUri(context, keyed, "ai_cutout")
                        outBmp.recycle()
                        keyed.recycle()
                        pushUndo()
                        if (isSmartCutout) {
                            val ov = ProOverlayItem(
                                kind = VideoOverlayKind.IMAGE,
                                title = "Recorte Inteligente IA",
                                uriString = outUri.toString(),
                                startMs = currentPosMs,
                                endMs = (currentPosMs + 5000L).coerceAtLeast(currentPosMs + 1500L)
                            )
                            overlays.add(ov)
                            selectedOverlayId = ov.id
                            activeTab = ProVideoToolTab.LAYERS
                            onShowMessage("Recorte inteligente adicionado como camada!")
                        } else if (clip.mediaType == "image") {
                            clips[selectedClipIndex] = clip.copy(uriString = outUri.toString())
                            onShowMessage("Fundo removido com IA!")
                        } else {
                            clips.add(
                                selectedClipIndex + 1,
                                clip.copy(
                                    clipId = "cut_${UUID.randomUUID().toString().take(6)}",
                                    title = "${clip.title} (Sem Fundo IA)",
                                    uriString = outUri.toString(),
                                    mediaType = "image",
                                    durationMs = 4000L,
                                    trimStartMs = 0L,
                                    trimEndMs = 4000L
                                )
                            )
                            selectedClipIndex += 1
                            onShowMessage("Fotograma sem fundo adicionado!")
                        }
                    },
                    onFailure = { onShowMessage(it.localizedMessage ?: "Erro ao processar com IA.") }
                )
            } catch (e: Exception) {
                onShowMessage(e.localizedMessage ?: "Erro ao processar fotograma.")
            } finally {
                aiBusy = false
            }
        }
    }

    fun runAiAutoCaptions(topic: String) {
        aiBusy = true
        scope.launch {
            try {
                val clip = clips.getOrNull(selectedClipIndex)
                val frame = clip?.let { VideoProProcessors.extractFrameAtMs(context, it, 500L, 512) }
                val dur = totalDurationMs.coerceAtLeast(5000L)
                aiRepository.generateAutoCaptions(topic, dur, frame).fold(
                    onSuccess = { segments ->
                        pushUndo()
                        segments.forEachIndexed { idx, seg ->
                            overlays.add(
                                ProOverlayItem(
                                    kind = VideoOverlayKind.SUBTITLE,
                                    title = "Legenda IA ${idx + 1}",
                                    text = seg.text,
                                    startMs = seg.startMs,
                                    endMs = seg.endMs,
                                    yFraction = 0.80f,
                                    fontSizeSp = 19f,
                                    bgColorHex = "#B3000000"
                                )
                            )
                        }
                        sound?.playSuccess()
                        onShowMessage("${segments.size} legendas automáticas criadas com IA!")
                    },
                    onFailure = { onShowMessage(it.localizedMessage ?: "Erro ao gerar legendas IA.") }
                )
            } finally {
                aiBusy = false
            }
        }
    }

    fun runAiFrameAnalysis(goal: String) {
        val clip = clips.getOrNull(selectedClipIndex)
        if (clip == null) {
            onShowMessage("Selecione primeiro um clipe na timeline.")
            return
        }
        aiBusy = true
        scope.launch {
            try {
                val (_, relMs, _) = resolveClipTimingAt(currentPosMs)
                val frame = VideoProProcessors.extractFrameAtMs(context, clip, relMs, 512)
                    ?: throw IllegalStateException("Não foi possível analisar este clipe.")
                aiRepository.analyzeVideoFrameForProAi(frame, goal).fold(
                    onSuccess = { res ->
                        pushUndo()
                        val updated = when (goal) {
                            "track_object" -> clip.copy(
                                motionTrackX = res.targetX,
                                motionTrackY = res.targetY,
                                offsetX = ((0.5f - res.targetX) * 0.4f).coerceIn(-0.4f, 0.4f),
                                offsetY = ((0.5f - res.targetY) * 0.4f).coerceIn(-0.4f, 0.4f),
                                zoomScale = clip.zoomScale.coerceAtLeast(1.18f)
                            )
                            "smart_slowmo" -> clip.copy(
                                speed = res.recommendedSpeed,
                                speedCurve = res.speedCurve
                            )
                            else -> clip.copy(
                                brightness = res.brightness,
                                contrast = res.contrast,
                                saturation = res.saturation,
                                temperature = res.temperature,
                                sharpness = 0.25f
                            )
                        }
                        clips[selectedClipIndex] = updated
                        sound?.playSuccess()
                        onShowMessage(res.summary)
                    },
                    onFailure = { onShowMessage(it.localizedMessage ?: "Erro na análise IA.") }
                )
            } catch (e: Exception) {
                onShowMessage(e.localizedMessage ?: "Erro IA.")
            } finally {
                aiBusy = false
            }
        }
    }

    fun saveProjectNow() {
        val currentBgUri = bgImageUri
        val savedLayers = buildList {
            if (currentBgUri != null) {
                add(
                    mapOf(
                        "layerId" to "video_bg_image",
                        "type" to "IMAGE",
                        "name" to "Fundo Vídeo",
                        "imageUri" to currentBgUri
                    )
                )
            }
            add(
                mapOf(
                    "layerId" to "video_bg_settings",
                    "type" to "IMAGE",
                    "name" to "Config Fundo",
                    "animatedBgStyle" to animatedBgStyle.name
                )
            )
            audioTracks.forEach { add(it.toLayerMap()) }
            overlays.forEach { add(it.toLayerMap()) }
        }.take(60) // Enforced strictly <= 60 per firestore.rules

        val (saveW, saveH) = videoDimensionsForAspect(aspectRatio)
        sound?.playSuccess()
        onSaveToCloud(
            initialProject.copy(
                title = projectTitle,
                formatName = "Vídeo $aspectRatio",
                width = saveW,
                height = saveH,
                backgroundColorHex = bgColorStart,
                backgroundGradientHex = bgColorEnd,
                layers = savedLayers,
                videoClips = clips.take(20).map { it.toMap() },
                projectType = "video"
            )
        )
    }

    val (_, _, currentClipProgress) = resolveClipTimingAt(currentPosMs)
    val selectedClip = clips.getOrNull(selectedClipIndex)
    val selectedOverlay = overlays.firstOrNull { it.id == selectedOverlayId }

    @Composable
    fun RenderActiveToolPanel() {
        when (activeTab) {
            ProVideoToolTab.EDIT -> EditClipToolPanel(
                selectedClip = selectedClip,
                selectedClipIndex = selectedClipIndex,
                clipCount = clips.size,
                selectedMultiCount = selectedClipIndices.size,
                onSplitAtPlayhead = {
                    if (selectedClip != null) {
                        val activeSpan = (selectedClip.trimEndMs - selectedClip.trimStartMs).coerceAtLeast(1000L)
                        if (activeSpan < 1400L) {
                            onShowMessage("O clipe é demasiado curto para dividir.")
                        } else {
                            pushUndo()
                            val (_, relMs, _) = resolveClipTimingAt(currentPosMs)
                            val cutRel = (relMs * selectedClip.speed).toLong().coerceIn(500L, activeSpan - 500L)
                            val midMs = selectedClip.trimStartMs + cutRel
                            val firstHalf = selectedClip.copy(trimEndMs = midMs)
                            val secondHalf = selectedClip.copy(
                                clipId = "split_${UUID.randomUUID().toString().take(8)}",
                                title = "${selectedClip.title} (2)",
                                trimStartMs = midMs
                            )
                            clips[selectedClipIndex] = firstHalf
                            clips.add(selectedClipIndex + 1, secondHalf)
                            selectedClipIndex += 1
                            onShowMessage("Clipe dividido na posição atual!")
                        }
                    }
                },
                onDuplicateClip = {
                    if (selectedClip != null) {
                        pushUndo()
                        clips.add(
                            selectedClipIndex + 1,
                            selectedClip.copy(
                                clipId = "dup_${UUID.randomUUID().toString().take(8)}",
                                title = "${selectedClip.title} (Cópia)"
                            )
                        )
                        selectedClipIndex += 1
                        onShowMessage("Clipe duplicado!")
                    }
                },
                onReplaceClip = {
                    if (selectedClip != null) {
                        isReplacingClip = true
                        if (selectedClip.mediaType == "image") {
                            pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        }
                    }
                },
                onFreezeFrame = {
                    if (selectedClip != null) {
                        aiBusy = true
                        scope.launch {
                            val (_, relMs, _) = resolveClipTimingAt(currentPosMs)
                            VideoProProcessors.captureFreezeFrame(context, selectedClip, relMs).fold(
                                onSuccess = { frz ->
                                    pushUndo()
                                    clips.add(selectedClipIndex + 1, frz)
                                    selectedClipIndex += 1
                                    sound?.playSuccess()
                                    onShowMessage("Fotograma congelado inserido!")
                                },
                                onFailure = { onShowMessage(it.localizedMessage ?: "Erro ao congelar fotograma.") }
                            )
                            aiBusy = false
                        }
                    }
                },
                onReverseClip = {
                    if (selectedClip != null) {
                        aiBusy = true
                        onShowMessage("A gerar vídeo reverso real...")
                        scope.launch {
                            VideoProProcessors.generateReversedVideoClip(context, selectedClip).fold(
                                onSuccess = { revClip ->
                                    pushUndo()
                                    clips[selectedClipIndex] = revClip
                                    sound?.playSuccess()
                                    onShowMessage("Reprodução reversa aplicada!")
                                },
                                onFailure = { onShowMessage(it.localizedMessage ?: "Falha ao inverter vídeo.") }
                            )
                            aiBusy = false
                        }
                    }
                },
                onDeleteClips = {
                    pushUndo()
                    if (selectedClipIndices.size > 1) {
                        val toKeep = clips.filterIndexed { i, _ -> i !in selectedClipIndices }
                        clips.clear()
                        clips.addAll(toKeep)
                        selectedClipIndices = emptySet()
                    } else if (selectedClipIndex in clips.indices) {
                        clips.removeAt(selectedClipIndex)
                    }
                    selectedClipIndex = selectedClipIndex.coerceIn(0, (clips.size - 1).coerceAtLeast(0))
                },
                onUpdateClip = { updated ->
                    if (selectedClipIndex in clips.indices) clips[selectedClipIndex] = updated
                },
                onMoveClip = { delta ->
                    val target = selectedClipIndex + delta
                    if (selectedClipIndex in clips.indices && target in clips.indices) {
                        pushUndo()
                        val item = clips.removeAt(selectedClipIndex)
                        clips.add(target, item)
                        selectedClipIndex = target
                    }
                }
            )

            ProVideoToolTab.AUDIO -> AudioStudioToolPanel(
                audioTracks = audioTracks.toList(),
                selectedAudioId = selectedAudioId,
                previewingUri = previewingAudioUri,
                isRecordingVoice = isRecordingVoice,
                micLevel = micLevel,
                canExtractFromClip = selectedClip?.mediaType == "video",
                currentPosMs = currentPosMs,
                onImportDeviceAudio = {
                    pickAudioLauncher.launch(arrayOf("audio/*"))
                },
                onToggleRecordVoice = { toggleVoiceRecordingWithPermission() },
                onExtractVideoAudio = {
                    val clip = selectedClip ?: return@AudioStudioToolPanel
                    scope.launch {
                        VideoAudioStudioEngine.extractAudioFromVideo(context, clip.uriString, clip.title, currentPosMs).fold(
                            onSuccess = { trk ->
                                pushUndo()
                                audioTracks.add(trk)
                                selectedAudioId = trk.id
                                sound?.playSuccess()
                                onShowMessage("Áudio extraído do vídeo!")
                            },
                            onFailure = { onShowMessage(it.localizedMessage ?: "Não foi possível extrair áudio.") }
                        )
                    }
                },
                onPreviewPreset = { preset ->
                    scope.launch {
                        VideoAudioStudioEngine.ensurePresetWavTrack(context, preset).onSuccess { trk ->
                            previewingAudioUri = trk.uriString
                            audioMixer.previewSingle(trk.uriString) {
                                previewingAudioUri = null
                            }
                        }
                    }
                },
                onStopPreview = {
                    audioMixer.stopSinglePreview()
                    previewingAudioUri = null
                },
                onAddPresetTrack = { preset ->
                    scope.launch {
                        VideoAudioStudioEngine.ensurePresetWavTrack(context, preset, currentPosMs).fold(
                            onSuccess = { trk ->
                                pushUndo()
                                audioTracks.add(trk)
                                selectedAudioId = trk.id
                                sound?.playSuccess()
                                onShowMessage("'${preset.title}' adicionada à timeline!")
                            },
                            onFailure = { onShowMessage("Erro ao carregar faixa.") }
                        )
                    }
                },
                onSelectTrack = { selectedAudioId = it },
                onUpdateTrack = { trk ->
                    val idx = audioTracks.indexOfFirst { it.id == trk.id }
                    if (idx >= 0) audioTracks[idx] = trk
                },
                onSplitTrackAtPlayhead = { trkId ->
                    val idx = audioTracks.indexOfFirst { it.id == trkId }
                    if (idx >= 0) {
                        val trk = audioTracks[idx]
                        val rel = currentPosMs - trk.startMs
                        if (rel > 400L && rel < trk.activeDurationMs - 400L) {
                            pushUndo()
                            val cutPoint = trk.trimStartMs + rel
                            audioTracks[idx] = trk.copy(trimEndMs = cutPoint)
                            val second = trk.copy(
                                id = "aud_${UUID.randomUUID().toString().take(8)}",
                                title = "${trk.title} (2)",
                                startMs = currentPosMs,
                                trimStartMs = cutPoint
                            )
                            audioTracks.add(idx + 1, second)
                            selectedAudioId = second.id
                            onShowMessage("Faixa de áudio dividida!")
                        } else {
                            onShowMessage("Posicione a agulha dentro da faixa para dividir.")
                        }
                    }
                },
                onDeleteTrack = { trkId ->
                    pushUndo()
                    audioTracks.removeAll { it.id == trkId }
                    selectedAudioId = audioTracks.firstOrNull()?.id
                }
            )

            ProVideoToolTab.TEXT -> TextAndSubtitlesToolPanel(
                selectedOverlay = selectedOverlay,
                importedFonts = importedFonts.toList(),
                onAddTextOverlay = { text, isSub, is3D, bgHex, g1, g2 ->
                    pushUndo()
                    val ov = ProOverlayItem(
                        kind = if (isSub) VideoOverlayKind.SUBTITLE else VideoOverlayKind.TEXT,
                        title = text.take(18),
                        text = text,
                        startMs = currentPosMs,
                        endMs = (currentPosMs + 4500L).coerceAtLeast(currentPosMs + 1500L),
                        yFraction = if (isSub) 0.80f else 0.42f,
                        fontSizeSp = if (isSub) 19f else 28f,
                        is3DText = is3D,
                        bgColorHex = bgHex,
                        gradientStartHex = g1,
                        gradientEndHex = g2
                    )
                    overlays.add(ov)
                    selectedOverlayId = ov.id
                },
                onImportCustomFont = {
                    pickCustomFontLauncher.launch(arrayOf("*/*"))
                },
                onGenerateTtsFromText = { text ->
                    aiBusy = true
                    scope.launch {
                        VideoAudioStudioEngine.synthesizeTextToAudioTrack(context, text, startMs = currentPosMs).fold(
                            onSuccess = { trk ->
                                pushUndo()
                                audioTracks.add(trk)
                                selectedAudioId = trk.id
                                sound?.playSuccess()
                                onShowMessage("Voz gerada e adicionada na timeline!")
                            },
                            onFailure = { onShowMessage(it.localizedMessage ?: "Erro no motor TTS.") }
                        )
                        aiBusy = false
                    }
                },
                onGenerateAiCaptions = { runAiAutoCaptions(projectTitle) },
                onUpdateOverlay = { ov ->
                    val idx = overlays.indexOfFirst { it.id == ov.id }
                    if (idx >= 0) overlays[idx] = ov
                },
                onDeleteOverlay = { id ->
                    pushUndo()
                    overlays.removeAll { it.id == id }
                    if (selectedOverlayId == id) selectedOverlayId = null
                }
            )

            ProVideoToolTab.LAYERS -> LayersAndOverlaysToolPanel(
                overlays = overlays.toList(),
                selectedOverlayId = selectedOverlayId,
                onAddImageOverlay = {
                    pickOverlayImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onAddVideoPip = {
                    pickOverlayVideoPipLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                onAddBuiltInOverlay = { kind, title, textOrSymbol, colorHex ->
                    pushUndo()
                    val ov = ProOverlayItem(
                        kind = kind,
                        title = title,
                        text = if (kind == VideoOverlayKind.SHAPE) "" else textOrSymbol,
                        symbolKey = if (kind == VideoOverlayKind.SHAPE) textOrSymbol else "star",
                        colorHex = colorHex,
                        startMs = currentPosMs,
                        endMs = (currentPosMs + 5000L).coerceAtLeast(currentPosMs + 1500L),
                        xFraction = if (kind == VideoOverlayKind.VERIFIED_BADGE) 0.85f else 0.5f,
                        yFraction = if (kind == VideoOverlayKind.WATERMARK) 0.14f else 0.5f,
                        fontSizeSp = if (kind == VideoOverlayKind.WATERMARK) 14f else 24f
                    )
                    overlays.add(ov)
                    selectedOverlayId = ov.id
                },
                onSelectOverlay = { selectedOverlayId = it },
                onMoveOverlayZ = { id, delta ->
                    val idx = overlays.indexOfFirst { it.id == id }
                    val target = idx + delta
                    if (idx >= 0 && target in overlays.indices) {
                        pushUndo()
                        val item = overlays.removeAt(idx)
                        overlays.add(target, item)
                    }
                },
                onDuplicateOverlay = { id ->
                    val src = overlays.firstOrNull { it.id == id } ?: return@LayersAndOverlaysToolPanel
                    pushUndo()
                    val dup = src.copy(
                        id = "ov_${UUID.randomUUID().toString().take(8)}",
                        title = "${src.title} (2)",
                        xFraction = (src.xFraction + 0.06f).coerceIn(0.1f, 0.9f),
                        yFraction = (src.yFraction + 0.06f).coerceIn(0.1f, 0.9f)
                    )
                    overlays.add(dup)
                    selectedOverlayId = dup.id
                },
                onUpdateOverlay = { ov ->
                    val idx = overlays.indexOfFirst { it.id == ov.id }
                    if (idx >= 0) overlays[idx] = ov
                },
                onDeleteOverlay = { id ->
                    pushUndo()
                    overlays.removeAll { it.id == id }
                    if (selectedOverlayId == id) selectedOverlayId = null
                }
            )

            ProVideoToolTab.EFFECTS -> EffectsAndColorToolPanel(
                selectedClip = selectedClip,
                onUpdateClip = { updated ->
                    if (selectedClipIndex in clips.indices) clips[selectedClipIndex] = updated
                }
            )

            ProVideoToolTab.ANIMATE -> AnimationsAndKeyframesToolPanel(
                selectedClip = selectedClip,
                selectedOverlay = selectedOverlay,
                clipProgress = currentClipProgress,
                onUpdateClip = { updated ->
                    if (selectedClipIndex in clips.indices) clips[selectedClipIndex] = updated
                },
                onUpdateOverlay = { ov ->
                    val idx = overlays.indexOfFirst { it.id == ov.id }
                    if (idx >= 0) overlays[idx] = ov
                }
            )

            ProVideoToolTab.TRANSITIONS -> TransitionsToolPanel(
                selectedClip = selectedClip,
                onUpdateClip = { updated ->
                    if (selectedClipIndex in clips.indices) clips[selectedClipIndex] = updated
                },
                onApplyToAll = { trans, dur ->
                    pushUndo()
                    for (i in clips.indices) {
                        clips[i] = clips[i].copy(transition = trans, transitionDurationMs = dur)
                    }
                    onShowMessage("Transição '${trans.label}' aplicada a todos os clipes!")
                }
            )

            ProVideoToolTab.AI -> AiStudioVideoToolPanel(
                aiBusy = aiBusy,
                onRemoveBackground = { runAiBgRemovalOrCutout(isSmartCutout = false) },
                onSmartSubjectCutout = { runAiBgRemovalOrCutout(isSmartCutout = true) },
                onAutoCaptions = { topic -> runAiAutoCaptions(topic) },
                onTextToSpeech = { text ->
                    aiBusy = true
                    scope.launch {
                        VideoAudioStudioEngine.synthesizeTextToAudioTrack(context, text, startMs = currentPosMs).fold(
                            onSuccess = { trk ->
                                pushUndo()
                                audioTracks.add(trk)
                                selectedAudioId = trk.id
                                sound?.playSuccess()
                                onShowMessage("Áudio TTS adicionado à timeline!")
                            },
                            onFailure = { onShowMessage(it.localizedMessage ?: "Erro no TTS.") }
                        )
                        aiBusy = false
                    }
                },
                onAiEnhanceVideo = { runAiFrameAnalysis("enhance") },
                onAiSmartSlowMo = { runAiFrameAnalysis("smart_slowmo") },
                onAiObjectTracking = { runAiFrameAnalysis("track_object") },
                onAiGenerateSticker = { prompt ->
                    aiBusy = true
                    scope.launch {
                        aiRepository.generateImageFromPrompt(
                            "Clean isolated flat vector sticker icon of: $prompt. Bold crisp outline on pure solid #00FF00 background."
                        ).fold(
                            onSuccess = { bmp ->
                                val keyed = withContext(Dispatchers.IO) {
                                    VideoProProcessors.applyChromaKeyToBitmap(bmp, "#00FF00", 0.36f)
                                }
                                val uri = BitmapRenderer.saveBitmapToTempUri(context, keyed, "ai_sticker")
                                bmp.recycle(); keyed.recycle()
                                pushUndo()
                                val ov = ProOverlayItem(
                                    kind = VideoOverlayKind.IMAGE,
                                    title = "Sticker IA",
                                    uriString = uri.toString(),
                                    startMs = currentPosMs,
                                    endMs = (currentPosMs + 5000L).coerceAtLeast(currentPosMs + 1500L)
                                )
                                overlays.add(ov)
                                selectedOverlayId = ov.id
                                sound?.playSuccess()
                                onShowMessage("Sticker IA criado e adicionado!")
                            },
                            onFailure = { onShowMessage(it.localizedMessage ?: "Erro ao gerar sticker IA.") }
                        )
                        aiBusy = false
                    }
                },
                onToggleVoiceEnhance = {
                    val trk = audioTracks.firstOrNull { it.id == selectedAudioId } ?: audioTracks.firstOrNull()
                    if (trk != null) {
                        val idx = audioTracks.indexOfFirst { it.id == trk.id }
                        if (idx >= 0) {
                            audioTracks[idx] = trk.copy(noiseReduction = !trk.noiseReduction, vocalIsolation = !trk.vocalIsolation)
                            onShowMessage(if (!trk.noiseReduction) "Melhoramento de voz ativado!" else "Melhoramento de voz desativado.")
                        }
                    } else {
                        onShowMessage("Adicione ou grave uma faixa de voz primeiro.")
                    }
                }
            )

            ProVideoToolTab.BACKGROUNDS -> BackgroundsAndFormatToolPanel(
                aspectRatio = aspectRatio,
                bgColorStart = bgColorStart,
                bgColorEnd = bgColorEnd,
                animatedBgStyle = animatedBgStyle,
                onSelectAspectRatio = { aspectRatio = it },
                onSelectBgColors = { s, e -> bgColorStart = s; bgColorEnd = e },
                onSelectAnimatedStyle = { animatedBgStyle = it },
                onSelectScenePhoto = { bgImageUri = it },
                onPickGalleryBgPhoto = {
                    pickBgImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClearBgPhoto = { bgImageUri = null }
            )
        }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            sound?.playTap()
                            onBack()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRenameDialog = true }
                    ) {
                        Text(
                            text = projectTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$aspectRatio • ${clips.size} clipes • ${audioTracks.size} áudios",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { saveProjectNow() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Guardar Vídeo", tint = CyanGlow)
                    }
                    Button(
                        onClick = {
                            sound?.playTap()
                            showExportModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("video_export_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NavyDeep)
        ) {
            val isWideScreen = maxWidth >= 840.dp && !fullscreenPreview

            if (isWideScreen) {
                // Adaptive Tablet / Desktop / PC Layout: Preview + Timeline on Left, Tools in Lateral Side Panel
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        VideoPreviewCanvas(
                            exoPlayer = exoPlayer,
                            clips = clips.toList(),
                            selectedClipIndex = selectedClipIndex,
                            overlays = overlays.toList(),
                            selectedOverlayId = selectedOverlayId,
                            aspectRatio = aspectRatio,
                            bgColorStart = bgColorStart,
                            bgColorEnd = bgColorEnd,
                            bgImageUri = bgImageUri,
                            animatedBgStyle = animatedBgStyle,
                            currentPosMs = currentPosMs,
                            clipRelativeProgress = currentClipProgress,
                            isPlaying = isPlaying,
                            fullscreenPreview = false,
                            onTogglePlayPause = { isPlaying = !isPlaying },
                            onToggleFullscreen = { fullscreenPreview = !fullscreenPreview },
                            onCycleAspectRatio = {
                                val ratios = listOf("9:16", "16:9", "1:1", "3:4", "4:5")
                                aspectRatio = ratios[(ratios.indexOf(aspectRatio) + 1) % ratios.size]
                            },
                            onAddVideo = { pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            onAddPhoto = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onSelectOverlay = { selectedOverlayId = it },
                            onDragOverlay = { id, dx, dy ->
                                val idx = overlays.indexOfFirst { it.id == id }
                                if (idx >= 0) {
                                    val o = overlays[idx]
                                    overlays[idx] = o.copy(
                                        xFraction = (o.xFraction + dx).coerceIn(0.05f, 0.95f),
                                        yFraction = (o.yFraction + dy).coerceIn(0.05f, 0.95f)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1.45f).padding(10.dp)
                        )

                        VideoTimelineView(
                            clips = clips.toList(),
                            audioTracks = audioTracks.toList(),
                            overlays = overlays.toList(),
                            selectedClipIndex = selectedClipIndex,
                            selectedClipIndices = selectedClipIndices,
                            multiSelectMode = multiSelectMode,
                            selectedAudioId = selectedAudioId,
                            selectedOverlayId = selectedOverlayId,
                            currentPosMs = currentPosMs,
                            totalDurationMs = totalDurationMs,
                            timelineZoom = timelineZoom,
                            snapEnabled = snapEnabled,
                            toolsExpanded = toolsExpanded,
                            canUndo = undoStack.isNotEmpty(),
                            canRedo = redoStack.isNotEmpty(),
                            onUndo = { undoStack.removeLastOrNull()?.let { redoStack.add(captureSnapshot()); restoreSnapshot(it) } },
                            onRedo = { redoStack.removeLastOrNull()?.let { undoStack.add(captureSnapshot()); restoreSnapshot(it) } },
                            onToggleMultiSelect = {
                                multiSelectMode = !multiSelectMode
                                if (!multiSelectMode) selectedClipIndices = emptySet()
                            },
                            onToggleSnap = { snapEnabled = !snapEnabled },
                            onTimelineZoomChange = { timelineZoom = it },
                            onToggleToolsExpanded = { toolsExpanded = !toolsExpanded },
                            onSeekToMs = { seekTimelineTo(it) },
                            onSelectClip = { idx ->
                                if (multiSelectMode) {
                                    selectedClipIndices = if (idx in selectedClipIndices) selectedClipIndices - idx else selectedClipIndices + idx
                                }
                                selectedClipIndex = idx
                            },
                            onSwapClips = { a, b ->
                                if (a in clips.indices && b in clips.indices) {
                                    pushUndo()
                                    val item = clips.removeAt(a)
                                    clips.add(b, item)
                                    selectedClipIndex = b
                                }
                            },
                            onSelectAudio = { selectedAudioId = it; activeTab = ProVideoToolTab.AUDIO },
                            onNudgeAudioStart = { id, delta ->
                                val idx = audioTracks.indexOfFirst { it.id == id }
                                if (idx >= 0) {
                                    val t = audioTracks[idx]
                                    val raw = (t.startMs + delta).coerceIn(0L, 120_000L)
                                    audioTracks[idx] = t.copy(startMs = if (snapEnabled) ((raw + 125L) / 250L) * 250L else raw)
                                }
                            },
                            onSelectOverlay = { selectedOverlayId = it },
                            onAddVideo = { pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            onAddPhoto = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        )
                    }

                    VerticalDivider()

                    // Lateral Pro Tool Panel
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(350.dp).fillMaxHeight()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProVideoToolTab.entries.forEach { tab ->
                                    val isActive = tab == activeTab
                                    Surface(
                                        color = if (isActive) OrangeAccent.copy(alpha = 0.16f) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.clickable { activeTab = tab }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(tab.icon, contentDescription = null, tint = if (isActive) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                            Text(tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                RenderActiveToolPanel()
                            }
                        }
                    }
                }
            } else {
                // Phone / Portrait Adaptive Workspace: Maximized Preview + Timeline + Collapsible Drawer
                Column(modifier = Modifier.fillMaxSize()) {
                    VideoPreviewCanvas(
                        exoPlayer = exoPlayer,
                        clips = clips.toList(),
                        selectedClipIndex = selectedClipIndex,
                        overlays = overlays.toList(),
                        selectedOverlayId = selectedOverlayId,
                        aspectRatio = aspectRatio,
                        bgColorStart = bgColorStart,
                        bgColorEnd = bgColorEnd,
                        bgImageUri = bgImageUri,
                        animatedBgStyle = animatedBgStyle,
                        currentPosMs = currentPosMs,
                        clipRelativeProgress = currentClipProgress,
                        isPlaying = isPlaying,
                        fullscreenPreview = fullscreenPreview,
                        onTogglePlayPause = { isPlaying = !isPlaying },
                        onToggleFullscreen = { fullscreenPreview = !fullscreenPreview },
                        onCycleAspectRatio = {
                            val ratios = listOf("9:16", "16:9", "1:1", "3:4", "4:5")
                            aspectRatio = ratios[(ratios.indexOf(aspectRatio) + 1) % ratios.size]
                        },
                        onAddVideo = { pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                        onAddPhoto = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onSelectOverlay = { selectedOverlayId = it },
                        onDragOverlay = { id, dx, dy ->
                            val idx = overlays.indexOfFirst { it.id == id }
                            if (idx >= 0) {
                                val o = overlays[idx]
                                overlays[idx] = o.copy(
                                    xFraction = (o.xFraction + dx).coerceIn(0.05f, 0.95f),
                                    yFraction = (o.yFraction + dy).coerceIn(0.05f, 0.95f)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (fullscreenPreview || !toolsExpanded) 1f else 1.55f)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    if (!fullscreenPreview) {
                        VideoTimelineView(
                            clips = clips.toList(),
                            audioTracks = audioTracks.toList(),
                            overlays = overlays.toList(),
                            selectedClipIndex = selectedClipIndex,
                            selectedClipIndices = selectedClipIndices,
                            multiSelectMode = multiSelectMode,
                            selectedAudioId = selectedAudioId,
                            selectedOverlayId = selectedOverlayId,
                            currentPosMs = currentPosMs,
                            totalDurationMs = totalDurationMs,
                            timelineZoom = timelineZoom,
                            snapEnabled = snapEnabled,
                            toolsExpanded = toolsExpanded,
                            canUndo = undoStack.isNotEmpty(),
                            canRedo = redoStack.isNotEmpty(),
                            onUndo = { undoStack.removeLastOrNull()?.let { redoStack.add(captureSnapshot()); restoreSnapshot(it) } },
                            onRedo = { redoStack.removeLastOrNull()?.let { undoStack.add(captureSnapshot()); restoreSnapshot(it) } },
                            onToggleMultiSelect = {
                                multiSelectMode = !multiSelectMode
                                if (!multiSelectMode) selectedClipIndices = emptySet()
                            },
                            onToggleSnap = { snapEnabled = !snapEnabled },
                            onTimelineZoomChange = { timelineZoom = it },
                            onToggleToolsExpanded = { toolsExpanded = !toolsExpanded },
                            onSeekToMs = { seekTimelineTo(it) },
                            onSelectClip = { idx ->
                                if (multiSelectMode) {
                                    selectedClipIndices = if (idx in selectedClipIndices) selectedClipIndices - idx else selectedClipIndices + idx
                                }
                                selectedClipIndex = idx
                            },
                            onSwapClips = { a, b ->
                                if (a in clips.indices && b in clips.indices) {
                                    pushUndo()
                                    val item = clips.removeAt(a)
                                    clips.add(b, item)
                                    selectedClipIndex = b
                                }
                            },
                            onSelectAudio = { selectedAudioId = it; activeTab = ProVideoToolTab.AUDIO },
                            onNudgeAudioStart = { id, delta ->
                                val idx = audioTracks.indexOfFirst { it.id == id }
                                if (idx >= 0) {
                                    val t = audioTracks[idx]
                                    val raw = (t.startMs + delta).coerceIn(0L, 120_000L)
                                    audioTracks[idx] = t.copy(startMs = if (snapEnabled) ((raw + 125L) / 250L) * 250L else raw)
                                }
                            },
                            onSelectOverlay = { selectedOverlayId = it },
                            onAddVideo = { pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            onAddPhoto = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        )

                        if (toolsExpanded) {
                            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 120.dp, max = 185.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        RenderActiveToolPanel()
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ProVideoToolTab.entries.forEach { tab ->
                                            val isSelected = tab == activeTab
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) OrangeAccent.copy(alpha = 0.16f) else Color.Transparent)
                                                    .clickable {
                                                        sound?.playTap()
                                                        activeTab = tab
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    tab.icon,
                                                    contentDescription = tab.label,
                                                    tint = if (isSelected) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    tab.label,
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Autosave Project Recovery Dialog
    pendingRecovery?.let { draft ->
        AlertDialog(
            onDismissRequest = { pendingRecovery = null },
            title = { Text("Recuperar Projeto Guardado?") },
            text = {
                Text("Encontramos alterações locais não guardadas na nuvem (${draft.snapshot.clips.size} clipes, ${draft.snapshot.audioTracks.size} áudios). Deseja recuperar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectTitle = draft.title
                        restoreSnapshot(draft.snapshot)
                        pendingRecovery = null
                        onShowMessage("Projeto recuperado com sucesso!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) { Text("Recuperar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        VideoAutosaveStore.clearDraft(context, draftKey)
                        pendingRecovery = null
                    }
                ) { Text("Descartar") }
            }
        )
    }

    // Rename Project Dialog
    if (showRenameDialog) {
        var tempTitle by remember(projectTitle) { mutableStateOf(projectTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Nome do Projeto") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tempTitle.isNotBlank()) projectTitle = tempTitle.trim()
                    showRenameDialog = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") } }
        )
    }

    // Pro Export MP4 Dialog (720p / 1080p / 4K + 24/30/60 FPS + Bitrate Quality + Preview)
    if (showExportModal) {
        val estMb = ((totalDurationMs / 1000f).coerceAtLeast(3f) *
            (exportRes.baseBitrate * exportQuality.bitrateMultiplier * (exportFps / 30f)) / 8_000_000f)
        AlertDialog(
            onDismissRequest = { if (!isExporting) showExportModal = false },
            title = { Text("Exportar Vídeo MP4 Pro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Proporção $aspectRatio • ${clips.size} clipes • ${audioTracks.size} áudios • ~%.1f MB".format(estMb),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Custo: 4 créditos (Saldo atual: $creditsBalance)", style = MaterialTheme.typography.labelMedium)

                    if (isExporting) {
                        LinearProgressIndicator(
                            progress = { exportProgress / 100f },
                            color = OrangeAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("A renderizar MP4 (${exportRes.label} @ ${exportFps}fps): $exportProgress%", style = MaterialTheme.typography.labelMedium)
                    } else if (creditsBalance < 4L) {
                        Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                            Text(
                                "Créditos insuficientes. São necessários 4 créditos para exportar vídeo.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text("Resolução:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VideoResolutionPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = exportRes == preset,
                                    onClick = { exportRes = preset },
                                    label = { Text(preset.label.substringBefore(' '), fontSize = 12.sp) }
                                )
                            }
                        }

                        Text("Fotogramas (FPS):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24, 30, 60).forEach { f ->
                                FilterChip(
                                    selected = exportFps == f,
                                    onClick = { exportFps = f },
                                    label = { Text("$f FPS", fontSize = 12.sp) }
                                )
                            }
                        }

                        Text("Qualidade & Bitrate:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            VideoExportQuality.entries.forEach { q ->
                                FilterChip(
                                    selected = exportQuality == q,
                                    onClick = { exportQuality = q },
                                    label = { Text(q.label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (clips.isEmpty()) {
                                    onShowMessage("Adicione pelo menos 1 vídeo ou foto antes de exportar.")
                                    return@Button
                                }
                                scope.launch {
                                    isExporting = true
                                    exportProgress = 0
                                    val res = onReserveExport(
                                        initialProject.id,
                                        projectTitle,
                                        "video_mp4",
                                        "${exportRes.label} ${exportFps}fps",
                                        4L,
                                        "${projectTitle.take(24)}.mp4"
                                    )
                                    val exportId = res.getOrElse { err ->
                                        isExporting = false
                                        onShowMessage(err.localizedMessage ?: "Erro ao reservar exportação.")
                                        return@launch
                                    }
                                    activeExportId = exportId
                                    exportEngine.exportMp4(
                                        projectTitle = projectTitle,
                                        clips = clips.toList(),
                                        resolution = exportRes,
                                        aspectRatio = aspectRatio,
                                        fps = exportFps,
                                        quality = exportQuality,
                                        audioTracks = audioTracks.toList(),
                                        proOverlays = overlays.toList(),
                                        onProgress = { pct -> exportProgress = pct },
                                        onSuccess = { exported ->
                                            scope.launch {
                                                onCompleteExport(exportId, 4L, "video_mp4", exported.fileSizeBytes)
                                                sound?.playSuccess()
                                                isExporting = false
                                                activeExportId = null
                                                showExportModal = false
                                                exportedResult = exported
                                                onShowMessage("Vídeo exportado para Movies/CANVA AO! (-4 créditos)")
                                            }
                                        },
                                        onError = { errMsg ->
                                            scope.launch {
                                                onFailExport(exportId, false)
                                                isExporting = false
                                                activeExportId = null
                                                onShowMessage("Erro na exportação (nenhum crédito descontado): $errMsg")
                                            }
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Iniciar Exportação (-4 Créditos)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                if (!isExporting && creditsBalance < 4L) {
                    Button(onClick = { showExportModal = false; onGoToCredits() }) {
                        Text("Obter Créditos")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isExporting) {
                            exportEngine.cancelExport()
                            val eid = activeExportId
                            if (eid != null) scope.launch { onFailExport(eid, true) }
                            isExporting = false
                            activeExportId = null
                            onShowMessage("Exportação cancelada. Nenhum crédito foi descontado.")
                        }
                        showExportModal = false
                    }
                ) { Text(if (isExporting) "Cancelar Render" else "Fechar") }
            }
        )
    }

    // Post-Export Ready & Share Dialog
    exportedResult?.let { result ->
        AlertDialog(
            onDismissRequest = { exportedResult = null },
            title = { Text("Vídeo Pronto!", color = SuccessGreen) },
            text = { Text("Guardado na Galeria em Movies/CANVA AO (${result.fileName}).") },
            confirmButton = {
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/mp4"
                            putExtra(Intent.EXTRA_STREAM, result.shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partilhar Vídeo CANVA AO"))
                        exportedResult = null
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partilhar")
                }
            },
            dismissButton = { TextButton(onClick = { exportedResult = null }) { Text("Concluir") } }
        )
    }
}

private fun extractVideoDurationMs(context: android.content.Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 5000L
    } catch (_: Exception) {
        5000L
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}

private fun inferVideoAspectRatio(width: Long, height: Long): String {
    val w = width.coerceAtLeast(1L)
    val h = height.coerceAtLeast(1L)
    return when {
        w == h -> "1:1"
        w * 4L == h * 3L -> "3:4"
        w * 5L == h * 4L -> "4:5"
        w > h -> "16:9"
        else -> "9:16"
    }
}

private fun videoDimensionsForAspect(aspectRatio: String): Pair<Long, Long> = when (aspectRatio) {
    "1:1" -> 1080L to 1080L
    "3:4" -> 1080L to 1440L
    "4:5" -> 1080L to 1350L
    "16:9" -> 1920L to 1080L
    else -> 1080L to 1920L
}
