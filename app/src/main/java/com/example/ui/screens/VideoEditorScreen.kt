package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoCall
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.model.DesignProject
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoFilterKind
import com.example.core.model.VideoTextOverlay
import com.example.core.model.VideoTransitionKind
import com.example.data.repository.AiStudioRepository
import com.example.engine.graphics.BitmapRenderer
import com.example.engine.graphics.ExportedFileResult
import com.example.engine.video.VideoExportEngine
import com.example.engine.video.VideoResolutionPreset
import com.example.ui.components.parseComposeColor
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.OrangeBright
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private enum class VideoToolTab(val label: String, val icon: ImageVector) {
    CLIP("Cortar", Icons.Filled.ContentCut),
    BACKGROUND("Fundo", Icons.Filled.Palette),
    AUDIO("Música", Icons.Filled.MusicNote),
    TEXT("Texto", Icons.Filled.Subtitles),
    FILTER("Filtros", Icons.Filled.Filter),
    TRANSITION("Transição", Icons.Filled.SwapHoriz),
    FORMAT("Formato", Icons.Filled.AspectRatio)
}

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val exportEngine = remember(context) { VideoExportEngine(context) }
    val aiRepository = remember { AiStudioRepository() }

    var projectTitle by remember(initialProject.id) { mutableStateOf(initialProject.title) }
    val clips = remember(initialProject.id) {
        mutableStateListOf<VideoClipItem>().apply {
            addAll(initialProject.videoClips.map { VideoClipItem.fromMap(it) })
        }
    }
    val textOverlays = remember { mutableStateListOf<VideoTextOverlay>() }
    var selectedClipIndex by remember { mutableIntStateOf(0) }
    var audioUri by remember { mutableStateOf<String?>(null) }
    var audioTitle by remember { mutableStateOf("Sem faixa de áudio") }
    var audioMuted by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var activeTab by remember { mutableStateOf(VideoToolTab.CLIP) }
    var toolsExpanded by remember { mutableStateOf(true) }
    var fullscreenPreview by remember { mutableStateOf(false) }

    var bgColorStart by remember(initialProject.id) {
        mutableStateOf(initialProject.backgroundColorHex.ifBlank { "#0A1628" })
    }
    var bgColorEnd by remember(initialProject.id) {
        mutableStateOf(initialProject.backgroundGradientHex)
    }
    var bgImageUri by remember(initialProject.id) {
        mutableStateOf(
            initialProject.layers.firstOrNull { it["layerId"] == "video_bg_image" }?.get("imageUri") as? String
        )
    }
    var aiBusy by remember { mutableStateOf(false) }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(0L) }

    var showExportModal by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableIntStateOf(0) }
    var activeExportId by remember { mutableStateOf<String?>(null) }
    var exportedResult by remember { mutableStateOf<ExportedFileResult?>(null) }

    var showTextDialog by remember { mutableStateOf(false) }
    var newOverlayText by remember { mutableStateOf("") }
    var newOverlaySubtitle by remember { mutableStateOf(false) }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    // Lifecycle & cleanup
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
                isPlaying = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exportEngine.cancelExport()
            exoPlayer.release()
        }
    }

    // Sync playlist with ExoPlayer whenever clips change
    LaunchedEffect(clips.toList(), audioMuted) {
        val videoClipsOnly = clips.filter { it.mediaType == "video" }
        if (videoClipsOnly.isNotEmpty()) {
            val mediaItems = videoClipsOnly.map { clip ->
                val builder = MediaItem.Builder().setUri(Uri.parse(clip.uriString))
                if (clip.trimEndMs > clip.trimStartMs) {
                    builder.setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                            .setEndPositionMs(clip.trimEndMs)
                            .build()
                    )
                }
                builder.build()
            }
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
            val selected = clips.getOrNull(selectedClipIndex)
            exoPlayer.volume = if (audioMuted || selected?.muted == true) 0f else (selected?.volume ?: 1f)
            exoPlayer.setPlaybackSpeed(selected?.speed ?: 1f)
        } else {
            exoPlayer.clearMediaItems()
        }
        totalDurationMs = clips.sumOf { (it.trimEndMs - it.trimStartMs).coerceAtLeast(1000L) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            delay(250L)
        }
    }

    // Pickers
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val dur = extractVideoDurationMs(context, uri).coerceAtLeast(2000L)
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

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
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
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            audioUri = uri.toString()
            audioTitle = uri.lastPathSegment?.substringAfterLast('/') ?: "Áudio selecionado"
            onShowMessage("Música adicionada ao projeto!")
        }
    }

    val pickBgImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            bgImageUri = uri.toString()
            onShowMessage("Imagem de fundo do vídeo aplicada!")
        }
    }

    val selectedClip = clips.getOrNull(selectedClipIndex)

    fun runAiBackgroundRemovalOnClip() {
        val clip = clips.getOrNull(selectedClipIndex)
        if (clip == null) {
            onShowMessage("Selecione primeiro uma foto ou clipe na linha do tempo.")
            return
        }
        aiBusy = true
        scope.launch {
            try {
                val srcBitmap = withContext(Dispatchers.IO) {
                    if (clip.mediaType == "image") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val src = ImageDecoder.createSource(context.contentResolver, Uri.parse(clip.uriString))
                            ImageDecoder.decodeBitmap(src) { dec, _, _ -> dec.isMutableRequired = true }
                        } else {
                            context.contentResolver.openInputStream(Uri.parse(clip.uriString))?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        }
                    } else {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, Uri.parse(clip.uriString))
                            val frameTimeUs = (clip.trimStartMs.coerceAtLeast(0L) + 500L) * 1000L
                            retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } finally {
                            try { retriever.release() } catch (_: Exception) {}
                        }
                    }
                } ?: throw IllegalStateException("Não foi possível obter a imagem do clipe selecionado.")

                aiRepository.editOrRemoveBackground(
                    srcBitmap,
                    "Remove the background cleanly and isolate the main subject on a clean transparent backdrop."
                ).fold(
                    onSuccess = { outBmp ->
                        val outUri = BitmapRenderer.saveBitmapToTempUri(context, outBmp, "vid_nobg")
                        if (clip.mediaType == "image") {
                            clips[selectedClipIndex] = clip.copy(uriString = outUri.toString())
                        } else {
                            clips.add(
                                selectedClipIndex + 1,
                                VideoClipItem(
                                    clipId = "cut_${UUID.randomUUID().toString().take(6)}",
                                    title = "${clip.title} (Recorte IA)",
                                    uriString = outUri.toString(),
                                    mediaType = "image",
                                    durationMs = (clip.trimEndMs - clip.trimStartMs).coerceIn(2000L, 6000L),
                                    trimStartMs = 0L,
                                    trimEndMs = (clip.trimEndMs - clip.trimStartMs).coerceIn(2000L, 6000L),
                                    filter = clip.filter
                                )
                            )
                            selectedClipIndex += 1
                        }
                        onShowMessage("Fundo removido com IA!")
                    },
                    onFailure = { err ->
                        onShowMessage(err.localizedMessage ?: "Erro ao remover fundo com IA.")
                    }
                )
            } catch (e: Exception) {
                onShowMessage(e.localizedMessage ?: "Erro ao processar fundo.")
            } finally {
                aiBusy = false
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Editar Vídeo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "$aspectRatio • ${clips.size} clipes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            val bgMetaLayers = if (bgImageUri != null) {
                                listOf(
                                    mapOf(
                                        "layerId" to "video_bg_image",
                                        "type" to "IMAGE",
                                        "name" to "Fundo Vídeo",
                                        "imageUri" to bgImageUri!!
                                    )
                                )
                            } else emptyList()
                            onSaveToCloud(
                                initialProject.copy(
                                    title = projectTitle,
                                    formatName = "Vídeo $aspectRatio",
                                    backgroundColorHex = bgColorStart,
                                    backgroundGradientHex = bgColorEnd,
                                    layers = bgMetaLayers,
                                    videoClips = clips.map { it.toMap() },
                                    projectType = "video"
                                )
                            )
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Guardar Vídeo", tint = CyanGlow)
                    }
                    Button(
                        onClick = { showExportModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("video_export_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(NavyDeep)
        ) {
            // 1. Spacious Video Preview Viewport (maximizes visibility!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (fullscreenPreview) 1f else 1.45f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val ratio = when (aspectRatio) {
                    "9:16" -> 9f / 16f
                    "1:1" -> 1f
                    "4:5" -> 4f / 5f
                    else -> 16f / 9f
                }
                val isTransparentBg = bgColorStart.equals("transparent", ignoreCase = true)
                val startCol = if (isTransparentBg) Color.Transparent else parseComposeColor(bgColorStart, Color.Black)
                val endCol = if (isTransparentBg) Color.Transparent else (bgColorEnd?.let { parseComposeColor(it, startCol) } ?: startCol)

                Box(
                    modifier = Modifier
                        .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (!isTransparentBg) Modifier.background(Brush.linearGradient(listOf(startCol, endCol)))
                            else Modifier
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTransparentBg) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cell = 22f
                            val cols = (size.width / cell).toInt() + 1
                            val rows = (size.height / cell).toInt() + 1
                            for (r in 0..rows) {
                                for (c in 0..cols) {
                                    drawRect(
                                        color = if ((r + c) % 2 == 0) Color(0xFF2B3548) else Color(0xFF1B2333),
                                        topLeft = Offset(c * cell, r * cell),
                                        size = Size(cell, cell)
                                    )
                                }
                            }
                        }
                    }

                    // Optional Video Canvas Background Image
                    if (!bgImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(Uri.parse(bgImageUri)).crossfade(true).build(),
                            contentDescription = "Imagem de fundo do vídeo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    val showSelectedPhoto = selectedClip?.mediaType == "image" || (clips.isNotEmpty() && clips.none { it.mediaType == "video" })
                    if (showSelectedPhoto && selectedClip != null) {
                        val photoFilter = videoFilterColorMatrix(selectedClip.filter)
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(Uri.parse(selectedClip.uriString)).crossfade(true).build(),
                            contentDescription = selectedClip.title,
                            contentScale = ContentScale.Fit,
                            colorFilter = photoFilter?.let { ColorFilter.colorMatrix(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (clips.any { it.mediaType == "video" }) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    player = exoPlayer
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view ->
                                view.player = exoPlayer
                                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Visual filter tint preview over video playback
                        val filterTint = when (selectedClip?.filter) {
                            VideoFilterKind.WARM -> Color(0x33FF8C00)
                            VideoFilterKind.COOL -> Color(0x3300A8FF)
                            VideoFilterKind.CINEMA -> Color(0x28000000)
                            VideoFilterKind.BW -> Color(0x44808080)
                            else -> Color.Transparent
                        }
                        if (filterTint != Color.Transparent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(filterTint)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Icon(Icons.Filled.VideoCall, contentDescription = null, tint = OrangeBright, modifier = Modifier.size(46.dp))
                            Text(
                                text = "Importe vídeos ou fotos para montar o seu projeto",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                    modifier = Modifier.testTag("video_add_clip_button")
                                ) {
                                    Text("+ Vídeo")
                                }
                                OutlinedButton(
                                    onClick = {
                                        pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                ) {
                                    Text("+ Foto", color = Color.White)
                                }
                            }
                        }
                    }

                    // Overlay texts preview
                    textOverlays.forEach { ov ->
                        Box(
                            modifier = Modifier
                                .align(if (ov.yPositionFraction > 0.6f) Alignment.BottomCenter else Alignment.Center)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (ov.isSubtitle) Color.Black.copy(alpha = 0.65f) else Color.Transparent)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ov.text,
                                color = parseComposeColor(ov.colorHex, Color.White),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Play/Pause floating control when video clips exist
                    if (clips.any { it.mediaType == "video" } && !showSelectedPhoto) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .testTag("video_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Expand / Fullscreen preview toggle
                IconButton(
                    onClick = { fullscreenPreview = !fullscreenPreview },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = if (fullscreenPreview) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (fullscreenPreview) "Sair do ecrã inteiro" else "Ampliar pré-visualização",
                        tint = Color.White
                    )
                }
            }

            if (!fullscreenPreview) {
                // 2. Timecode & Quick Import Bar + Collapse Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatDuration(currentPosMs)} / ${formatDuration(totalDurationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                            }
                        ) {
                            Icon(Icons.Filled.VideoCall, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vídeo", color = OrangeAccent)
                        }
                        TextButton(
                            onClick = {
                                pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Foto", color = CyanGlow)
                        }
                        IconButton(onClick = { toolsExpanded = !toolsExpanded }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                imageVector = if (toolsExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                contentDescription = if (toolsExpanded) "Minimizar ferramentas" else "Expandir ferramentas",
                                tint = Color.White
                            )
                        }
                    }
                }

                // 3. Multi-track Visual Timeline + Collapsible Controls
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(clips) { idx, clip ->
                                val isSel = idx == selectedClipIndex
                                Surface(
                                    modifier = Modifier
                                        .width(136.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedClipIndex = idx
                                            if (clip.mediaType == "video") {
                                                val videoIdx = clips.take(idx + 1).count { it.mediaType == "video" } - 1
                                                if (videoIdx >= 0 && videoIdx < exoPlayer.mediaItemCount) {
                                                    exoPlayer.seekTo(videoIdx, 0L)
                                                }
                                            }
                                        }
                                        .border(
                                            if (isSel) 2.dp else 1.dp,
                                            if (isSel) OrangeAccent else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(12.dp)
                                        ),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (clip.mediaType == "image") {
                                            AsyncImage(
                                                model = Uri.parse(clip.uriString),
                                                contentDescription = clip.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(OrangeAccent.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Movie, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = clip.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${(clip.trimEndMs - clip.trimStartMs) / 1000f}s • ${clip.speed}x",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (toolsExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            // Audio track strip
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { pickAudioLauncher.launch(arrayOf("audio/*")) },
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = if (audioUri == null) "Adicionar áudio de fundo (+)" else audioTitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { audioMuted = !audioMuted },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (audioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Silenciar áudio",
                                            tint = if (audioMuted) MaterialTheme.colorScheme.error else OrangeAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // 4. Contextual Tool Controls (bounded height so preview stays clear)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 128.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                when (activeTab) {
                                    VideoToolTab.CLIP -> {
                                        if (selectedClip == null) {
                                            Text(
                                                "Selecione um clipe na linha do tempo para cortar, ajustar velocidade ou reordenar.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            if (selectedClipIndex > 0) {
                                                                val item = clips.removeAt(selectedClipIndex)
                                                                clips.add(selectedClipIndex - 1, item)
                                                                selectedClipIndex -= 1
                                                            }
                                                        },
                                                        label = { Text("Mover Esq.") },
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp)) }
                                                    )
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            if (selectedClipIndex < clips.lastIndex) {
                                                                val item = clips.removeAt(selectedClipIndex)
                                                                clips.add(selectedClipIndex + 1, item)
                                                                selectedClipIndex += 1
                                                            }
                                                        },
                                                        label = { Text("Mover Dir.") },
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp)) }
                                                    )
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            val mid = (selectedClip.trimStartMs + selectedClip.trimEndMs) / 2L
                                                            if (mid - selectedClip.trimStartMs >= 800L) {
                                                                clips[selectedClipIndex] = selectedClip.copy(trimEndMs = mid)
                                                                clips.add(
                                                                    selectedClipIndex + 1,
                                                                    selectedClip.copy(
                                                                        clipId = "split_${UUID.randomUUID().toString().take(6)}",
                                                                        title = "${selectedClip.title} (2)",
                                                                        trimStartMs = mid
                                                                    )
                                                                )
                                                            }
                                                        },
                                                        label = { Text("Dividir") },
                                                        leadingIcon = { Icon(Icons.Filled.ContentCut, null, Modifier.size(16.dp)) }
                                                    )
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = {
                                                            clips.removeAt(selectedClipIndex)
                                                            selectedClipIndex = (selectedClipIndex - 1).coerceAtLeast(0)
                                                        },
                                                        label = { Text("Remover") },
                                                        leadingIcon = { Icon(Icons.Filled.Delete, null, Modifier.size(16.dp)) }
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Duração: ${(selectedClip.trimEndMs - selectedClip.trimStartMs) / 1000f}s", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(96.dp))
                                                    Slider(
                                                        value = selectedClip.trimEndMs.toFloat(),
                                                        onValueChange = { newEnd ->
                                                            clips[selectedClipIndex] = selectedClip.copy(
                                                                trimEndMs = newEnd.toLong().coerceAtLeast(selectedClip.trimStartMs + 1000L)
                                                            )
                                                        },
                                                        valueRange = (selectedClip.trimStartMs + 1000L).toFloat()..selectedClip.durationMs.coerceAtLeast(2000L).toFloat(),
                                                        colors = SliderDefaults.colors(thumbColor = OrangeAccent, activeTrackColor = OrangeAccent),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                                        FilterChip(
                                                            selected = selectedClip.speed == spd,
                                                            onClick = {
                                                                clips[selectedClipIndex] = selectedClip.copy(speed = spd)
                                                                exoPlayer.setPlaybackSpeed(spd)
                                                            },
                                                            label = { Text("${spd}x") }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    VideoToolTab.BACKGROUND -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = { runAiBackgroundRemovalOnClip() },
                                                    enabled = !aiBusy && selectedClip != null,
                                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (aiBusy) "A remover..." else "Remover Fundo IA")
                                                }
                                                FilterChip(
                                                    selected = !bgImageUri.isNullOrBlank(),
                                                    onClick = {
                                                        pickBgImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                    },
                                                    label = { Text(if (bgImageUri == null) "+ Imagem de Fundo" else "Trocar Fundo") }
                                                )
                                                if (!bgImageUri.isNullOrBlank()) {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = { bgImageUri = null },
                                                        label = { Text("Limpar Imagem") }
                                                    )
                                                }
                                            }
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                item {
                                                    FilterChip(
                                                        selected = bgColorStart.equals("transparent", ignoreCase = true),
                                                        onClick = { bgColorStart = "transparent"; bgColorEnd = null },
                                                        label = { Text("Transparente") }
                                                    )
                                                }
                                                itemsIndexed(EditorColorSwatches) { _, hex ->
                                                    val active = bgColorStart.equals(hex, true) && bgColorEnd == null
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(parseComposeColor(hex, Color.Black))
                                                            .border(if (active) 2.5.dp else 1.dp, if (active) OrangeAccent else Color.Gray, CircleShape)
                                                            .clickable { bgColorStart = hex; bgColorEnd = null }
                                                    )
                                                }
                                                itemsIndexed(EditorBackgroundPresets) { _, (s, e) ->
                                                    val active = bgColorStart.equals(s, true) && bgColorEnd.equals(e, true)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Brush.linearGradient(listOf(parseComposeColor(s), parseComposeColor(e))))
                                                            .border(if (active) 2.5.dp else 1.dp, if (active) OrangeAccent else Color.Gray, RoundedCornerShape(8.dp))
                                                            .clickable { bgColorStart = s; bgColorEnd = e }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    VideoToolTab.AUDIO -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) },
                                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                                            ) {
                                                Text("Escolher Música")
                                            }
                                            if (selectedClip != null) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Volume do clipe: ${(selectedClip.volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                                                    Slider(
                                                        value = selectedClip.volume,
                                                        onValueChange = { v ->
                                                            clips[selectedClipIndex] = selectedClip.copy(volume = v, muted = v == 0f)
                                                            exoPlayer.volume = v
                                                        },
                                                        valueRange = 0f..1f
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    VideoToolTab.TEXT -> {
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    newOverlayText = ""
                                                    newOverlaySubtitle = false
                                                    showTextDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                                            ) {
                                                Text("+ Texto / Legenda")
                                            }
                                            textOverlays.forEachIndexed { idx, ov ->
                                                FilterChip(
                                                    selected = true,
                                                    onClick = { textOverlays.removeAt(idx) },
                                                    label = { Text("${ov.text.take(14)} ×") }
                                                )
                                            }
                                        }
                                    }
                                    VideoToolTab.FILTER -> {
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            VideoFilterKind.entries.forEach { fk ->
                                                FilterChip(
                                                    selected = selectedClip?.filter == fk,
                                                    onClick = {
                                                        if (selectedClip != null) {
                                                            clips[selectedClipIndex] = selectedClip.copy(filter = fk)
                                                        }
                                                    },
                                                    label = { Text(fk.label) }
                                                )
                                            }
                                        }
                                    }
                                    VideoToolTab.TRANSITION -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                VideoTransitionKind.entries.forEach { tk ->
                                                    FilterChip(
                                                        selected = selectedClip?.transition == tk,
                                                        onClick = {
                                                            if (selectedClip != null) {
                                                                clips[selectedClipIndex] = selectedClip.copy(transition = tk)
                                                            }
                                                        },
                                                        label = { Text(tk.label) }
                                                    )
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.Animation, contentDescription = null, modifier = Modifier.size(16.dp))
                                                listOf("none" to "Sem anim.", "fade" to "Fade In", "zoom" to "Zoom In").forEach { (k, lbl) ->
                                                    FilterChip(
                                                        selected = selectedClip?.animation == k,
                                                        onClick = {
                                                            if (selectedClip != null) {
                                                                clips[selectedClipIndex] = selectedClip.copy(animation = k)
                                                            }
                                                        },
                                                        label = { Text(lbl) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    VideoToolTab.FORMAT -> {
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("9:16", "16:9", "1:1", "4:5").forEach { ar ->
                                                FilterChip(
                                                    selected = aspectRatio == ar,
                                                    onClick = { aspectRatio = ar },
                                                    label = { Text(ar) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Bottom Toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VideoToolTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (activeTab == tab) toolsExpanded = !toolsExpanded
                                            else {
                                                activeTab = tab
                                                toolsExpanded = true
                                            }
                                        }
                                        .minimumInteractiveComponentSize(),
                                    color = if (selected) OrangeAccent.copy(alpha = 0.16f) else Color.Transparent
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.label,
                                            tint = if (selected) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
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

    // Add Text Overlay Dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Adicionar Texto / Legenda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newOverlayText,
                        onValueChange = { newOverlayText = it },
                        label = { Text("Texto a exibir no vídeo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !newOverlaySubtitle,
                            onClick = { newOverlaySubtitle = false },
                            label = { Text("Título Central") }
                        )
                        FilterChip(
                            selected = newOverlaySubtitle,
                            onClick = { newOverlaySubtitle = true },
                            label = { Text("Legenda Inferior") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newOverlayText.isNotBlank()) {
                            textOverlays.add(
                                VideoTextOverlay(
                                    id = "vtxt_${UUID.randomUUID().toString().take(6)}",
                                    text = newOverlayText.trim(),
                                    yPositionFraction = if (newOverlaySubtitle) 0.85f else 0.5f,
                                    isSubtitle = newOverlaySubtitle
                                )
                            )
                        }
                        showTextDialog = false
                    }
                ) { Text("Adicionar") }
            },
            dismissButton = { TextButton(onClick = { showTextDialog = false }) { Text("Cancelar") } }
        )
    }

    // Export MP4 Dialog (-4 credits ONLY upon successful completion)
    if (showExportModal) {
        AlertDialog(
            onDismissRequest = { if (!isExporting) showExportModal = false },
            title = { Text("Exportar Vídeo MP4") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Custo: 4 créditos (Saldo atual: $creditsBalance)")
                    Text("Formato: MP4 H.264 • Proporção $aspectRatio", style = MaterialTheme.typography.bodySmall)
                    if (isExporting) {
                        LinearProgressIndicator(
                            progress = { exportProgress / 100f },
                            color = OrangeAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("A renderizar vídeo: $exportProgress%", style = MaterialTheme.typography.labelMedium)
                    } else if (creditsBalance < 4L) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Créditos insuficientes. São necessários 4 créditos para exportar vídeo.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        VideoResolutionPreset.entries.forEach { preset ->
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
                                            preset.label,
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
                                            audioUri = audioUri,
                                            audioMuted = audioMuted,
                                            textOverlays = textOverlays.toList(),
                                            resolution = preset,
                                            aspectRatio = aspectRatio,
                                            onProgress = { pct -> exportProgress = pct },
                                            onSuccess = { exported ->
                                                scope.launch {
                                                    onCompleteExport(exportId, 4L, "video_mp4", exported.fileSizeBytes)
                                                    isExporting = false
                                                    activeExportId = null
                                                    showExportModal = false
                                                    exportedResult = exported
                                                    onShowMessage("Vídeo exportado para Filmes/CANVA AO! (-4 créditos)")
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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Exportar ${preset.label} (-4 Créditos)")
                            }
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
                            if (eid != null) {
                                scope.launch { onFailExport(eid, true) }
                            }
                            isExporting = false
                            activeExportId = null
                            onShowMessage("Exportação cancelada. Nenhum crédito foi descontado.")
                        }
                        showExportModal = false
                    }
                ) {
                    Text(if (isExporting) "Cancelar Render" else "Fechar")
                }
            }
        )
    }

    // Share after Export
    exportedResult?.let { result ->
        AlertDialog(
            onDismissRequest = { exportedResult = null },
            title = { Text("Vídeo Pronto!", color = SuccessGreen) },
            text = {
                Text("Guardado na Galeria em Movies/CANVA AO (${result.fileName}).")
            },
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
            dismissButton = {
                TextButton(onClick = { exportedResult = null }) { Text("Concluir") }
            }
        )
    }
}

private fun videoFilterColorMatrix(filter: VideoFilterKind): ColorMatrix? {
    return when (filter) {
        VideoFilterKind.NORMAL -> null
        VideoFilterKind.BW -> ColorMatrix().apply { setToSaturation(0f) }
        VideoFilterKind.WARM -> ColorMatrix(
            floatArrayOf(
                1.12f, 0f, 0f, 0f, 14f,
                0f, 1.03f, 0f, 0f, 4f,
                0f, 0f, 0.88f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        VideoFilterKind.COOL -> ColorMatrix(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 1.0f, 0f, 0f, 4f,
                0f, 0f, 1.15f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        VideoFilterKind.CINEMA -> ColorMatrix(
            floatArrayOf(
                1.18f, 0f, 0f, 0f, -12f,
                0f, 1.15f, 0f, 0f, -10f,
                0f, 0f, 1.12f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f
            )
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
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%02d:%02d".format(min, sec)
}
