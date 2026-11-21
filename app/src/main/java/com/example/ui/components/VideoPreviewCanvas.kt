package com.example.ui.components

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.model.ProEffectKind
import com.example.core.model.ProOverlayItem
import com.example.core.model.TemplateSeedCatalog
import com.example.core.model.VideoBackgroundAnimatedStyle
import com.example.core.model.VideoBlendMode
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoFilterKind
import com.example.core.model.VideoKeyframe
import com.example.core.model.VideoMaskShape
import com.example.core.model.VideoOverlayKind
import com.example.core.model.VideoTransitionKind
import com.example.core.model.interpolateKeyframes
import com.example.engine.video.VideoProProcessors
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.OrangeBright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewCanvas(
    exoPlayer: ExoPlayer,
    clips: List<VideoClipItem>,
    selectedClipIndex: Int,
    overlays: List<ProOverlayItem>,
    selectedOverlayId: String?,
    aspectRatio: String,
    bgColorStart: String,
    bgColorEnd: String?,
    bgImageUri: String?,
    animatedBgStyle: VideoBackgroundAnimatedStyle,
    currentPosMs: Long,
    clipRelativeProgress: Float,
    isPlaying: Boolean,
    fullscreenPreview: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onAddVideo: () -> Unit,
    onAddPhoto: () -> Unit,
    onSelectOverlay: (String) -> Unit,
    onDragOverlay: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewportZoom by remember { mutableFloatStateOf(1f) }
    var viewportPanX by remember { mutableFloatStateOf(0f) }
    var viewportPanY by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val selectedClip = clips.getOrNull(selectedClipIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextZoom = (viewportZoom * zoom).coerceIn(0.65f, 3.5f)
                    viewportZoom = nextZoom
                    if (nextZoom > 1.02f) {
                        viewportPanX = (viewportPanX + pan.x).coerceIn(-600f, 600f)
                        viewportPanY = (viewportPanY + pan.y).coerceIn(-600f, 600f)
                    } else {
                        viewportPanX = 0f
                        viewportPanY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val ratio = when (aspectRatio) {
            "9:16" -> 9f / 16f
            "3:4" -> 3f / 4f
            "4:5" -> 4f / 5f
            "1:1" -> 1f
            else -> 16f / 9f
        }
        val isTransparentBg = bgColorStart.equals("transparent", ignoreCase = true)
        val startCol = if (isTransparentBg) Color.Transparent else parseComposeColor(bgColorStart, Color(0xFF0A1628))
        val endCol = if (isTransparentBg) Color.Transparent else (bgColorEnd?.let { parseComposeColor(it, startCol) } ?: startCol)

        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(ratio, matchHeightConstraintsFirst = true)
                .graphicsLayer {
                    scaleX = viewportZoom
                    scaleY = viewportZoom
                    translationX = viewportPanX
                    translationY = viewportPanY
                }
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (!isTransparentBg) Modifier.background(Brush.linearGradient(listOf(startCol, endCol)))
                    else Modifier
                )
                .border(1.2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            val canvasWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val canvasHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

            // Transparent checkerboard
            if (isTransparentBg) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cell = 38f
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

            // Animated Background Canvas
            if (animatedBgStyle != VideoBackgroundAnimatedStyle.NONE) {
                AnimatedBackgroundLayer(
                    style = animatedBgStyle,
                    phase = animPhase,
                    baseStart = startCol,
                    baseEnd = endCol
                )
            }

            // Optional Background Image
            val resolvedBgModel = TemplateSeedCatalog.resolvePlayableUri(context, bgImageUri)
            if (resolvedBgModel != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(resolvedBgModel).crossfade(true).build(),
                    contentDescription = "Fundo de imagem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Active Clip Video / Photo with live Color Grading, Keyframes, Effects, Transitions, Masks
            if (selectedClip != null) {
                val clipKfs = remember(selectedClip.keyframesData) {
                    selectedClip.keyframesData.map { VideoKeyframe.fromMap(it) }
                }
                val interp = interpolateKeyframes(
                    keyframes = clipKfs,
                    progress = clipRelativeProgress,
                    defaultX = 0.5f + selectedClip.offsetX * 0.5f,
                    defaultY = 0.5f + selectedClip.offsetY * 0.5f,
                    defaultScale = selectedClip.zoomScale,
                    defaultRotation = selectedClip.rotationDeg,
                    defaultOpacity = 1f
                )

                val animTransform = computeAnimationTransform(
                    animIn = selectedClip.animation,
                    animOut = selectedClip.animOut,
                    animCombo = selectedClip.animCombo,
                    progress = clipRelativeProgress,
                    clipDurationMs = (selectedClip.trimEndMs - selectedClip.trimStartMs).coerceAtLeast(1000L),
                    animDurationMs = selectedClip.animDurationMs
                )

                val transTransform = computeTransitionTransform(
                    transition = selectedClip.transition,
                    progress = clipRelativeProgress,
                    clipDurationMs = (selectedClip.trimEndMs - selectedClip.trimStartMs).coerceAtLeast(1000L),
                    transDurationMs = selectedClip.transitionDurationMs
                )

                val effectKind = enumValues<ProEffectKind>()
                    .firstOrNull { it.name == selectedClip.effectName } ?: ProEffectKind.NONE
                val maskShape = enumValues<VideoMaskShape>()
                    .firstOrNull { it.name == selectedClip.maskShapeName } ?: VideoMaskShape.NONE

                val shakeX = if (effectKind == ProEffectKind.SHAKE_MOTION && isPlaying) {
                    sin(animPhase * 9f) * 14f * selectedClip.effectIntensity
                } else 0f
                val shakeY = if (effectKind == ProEffectKind.SHAKE_MOTION && isPlaying) {
                    cos(animPhase * 11f) * 10f * selectedClip.effectIntensity
                } else 0f

                val blurRadius = when {
                    effectKind == ProEffectKind.BLUR -> (10f * selectedClip.effectIntensity).dp
                    effectKind == ProEffectKind.MOTION_BLUR -> (5f * selectedClip.effectIntensity).dp
                    transTransform.blurDp > 0f -> transTransform.blurDp.dp
                    else -> 0.dp
                }

                val clipModifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (maskShape != VideoMaskShape.NONE) {
                            Modifier.clip(GenericShape { size, _ ->
                                val androidPath = VideoProProcessors.buildMaskPath(size.width, size.height, maskShape)
                                addPath(androidPath.asComposePath())
                            })
                        } else Modifier
                    )
                    .graphicsLayer {
                        val baseScaleX = interp.scale * animTransform.scale * transTransform.scale * (if (selectedClip.flipH) -1f else 1f)
                        val baseScaleY = interp.scale * animTransform.scale * transTransform.scale * (if (selectedClip.flipV) -1f else 1f)
                        scaleX = baseScaleX
                        scaleY = baseScaleY
                        rotationZ = interp.rotation + animTransform.rotation + transTransform.rotationZ
                        rotationY = transTransform.rotationY
                        translationX = ((interp.xFraction - 0.5f) * canvasWidthPx) + animTransform.tx + transTransform.tx + shakeX
                        translationY = ((interp.yFraction - 0.5f) * canvasHeightPx) + animTransform.ty + transTransform.ty + shakeY
                        alpha = (interp.opacity * animTransform.alpha * transTransform.alpha).coerceIn(0f, 1f)
                    }
                    .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)

                val combinedColorMatrix = remember(
                    selectedClip.filter,
                    selectedClip.brightness,
                    selectedClip.contrast,
                    selectedClip.saturation,
                    selectedClip.exposure,
                    selectedClip.temperature,
                    selectedClip.hueShift,
                    selectedClip.rgbGamma
                ) {
                    buildProColorMatrix(selectedClip)
                }

                val showPhoto = selectedClip.mediaType == "image" || clips.none { it.mediaType == "video" }
                Box(modifier = clipModifier) {
                    if (showPhoto) {
                        val processedChromaBmp by produceState<Bitmap?>(
                            initialValue = null,
                            selectedClip.uriString,
                            selectedClip.chromaKeyEnabled,
                            selectedClip.chromaKeyColorHex,
                            selectedClip.chromaKeyTolerance
                        ) {
                            value = if (selectedClip.chromaKeyEnabled) {
                                withContext(Dispatchers.IO) {
                                    val src = VideoProProcessors.loadBitmapSync(context, selectedClip.uriString, 720)
                                    src?.let {
                                        VideoProProcessors.applyChromaKeyToBitmap(
                                            it,
                                            selectedClip.chromaKeyColorHex,
                                            selectedClip.chromaKeyTolerance
                                        ).also { keyed -> if (keyed !== it) it.recycle() }
                                    }
                                }
                            } else null
                        }

                        if (processedChromaBmp != null) {
                            Image(
                                bitmap = processedChromaBmp!!.asImageBitmap(),
                                contentDescription = selectedClip.title,
                                contentScale = if (selectedClip.cropMode == "fit") ContentScale.Fit else ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(combinedColorMatrix),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val resolvedClipModel = TemplateSeedCatalog.resolvePlayableUri(context, selectedClip.uriString)
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(resolvedClipModel).crossfade(false).build(),
                                contentDescription = selectedClip.title,
                                contentScale = if (selectedClip.cropMode == "fit") ContentScale.Fit else ContentScale.Crop,
                                colorFilter = ColorFilter.colorMatrix(combinedColorMatrix),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                (LayoutInflater.from(ctx).inflate(R.layout.video_player_texture_view, null) as PlayerView).apply {
                                    useController = false
                                    resizeMode = if (selectedClip.cropMode == "fit") {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    player = exoPlayer
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view ->
                                if (view.player !== exoPlayer) view.player = exoPlayer
                                view.resizeMode = if (selectedClip.cropMode == "fit") {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                } else {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Live Color Filter & Grading Tint overlay for Video playback + Vignette / Grain / Pro FX
                    ProEffectsAndGradingOverlay(
                        clip = selectedClip,
                        effectKind = effectKind,
                        phase = animPhase,
                        flashAlpha = transTransform.flashAlpha
                    )
                }

                // AI Object Motion Tracking Reticle if active
                if (selectedClip.motionTrackX != null && selectedClip.motionTrackY != null) {
                    val tx = (selectedClip.motionTrackX * canvasWidthPx).roundToInt()
                    val ty = (selectedClip.motionTrackY * canvasHeightPx).roundToInt()
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(tx - 24, ty - 24) }
                            .size(48.dp)
                            .border(2.dp, CyanGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OrangeAccent)
                        )
                    }
                }
            } else {
                // Empty State Import Prompt
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Icon(Icons.Filled.VideoCall, contentDescription = null, tint = OrangeBright, modifier = Modifier.size(44.dp))
                    Text(
                        text = "Importe vídeos ou fotos para editar",
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddVideo,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                            modifier = Modifier.testTag("video_add_clip_button")
                        ) {
                            Text("+ Vídeo")
                        }
                        OutlinedButton(onClick = onAddPhoto) {
                            Text("+ Foto", color = Color.White)
                        }
                    }
                }
            }

            // Multi-layer Pro Overlays (Text, Subtitles, Stickers, Emojis, Verified Badge, Images, Logos, Watermarks, Shapes, Frames)
            overlays.filter { it.isActiveAt(currentPosMs) }.forEach { ov ->
                val isSelected = ov.id == selectedOverlayId
                val ovDur = (ov.endMs - ov.startMs).coerceAtLeast(300L)
                val ovProgress = ((currentPosMs - ov.startMs).toFloat() / ovDur.toFloat()).coerceIn(0f, 1f)
                val ovInterp = interpolateKeyframes(
                    keyframes = ov.keyframes,
                    progress = ovProgress,
                    defaultX = ov.xFraction,
                    defaultY = ov.yFraction,
                    defaultScale = ov.scale,
                    defaultRotation = ov.rotation,
                    defaultOpacity = ov.opacity
                )
                val animTf = computeAnimationTransform(
                    animIn = ov.animIn,
                    animOut = ov.animOut,
                    animCombo = "none",
                    progress = ovProgress,
                    clipDurationMs = ovDur,
                    animDurationMs = 450L
                )

                val cxPx = ovInterp.xFraction * canvasWidthPx
                val cyPx = ovInterp.yFraction * canvasHeightPx

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset((cxPx - 120f).roundToInt(), (cyPx - 48f).roundToInt()) }
                        .size(width = 240.dp, height = 96.dp)
                        .graphicsLayer {
                            scaleX = ovInterp.scale * animTf.scale
                            scaleY = ovInterp.scale * animTf.scale
                            rotationZ = ovInterp.rotation + animTf.rotation
                            alpha = (ovInterp.opacity * animTf.alpha).coerceIn(0f, 1f)
                        }
                        .then(
                            if (!ov.locked) {
                                Modifier.pointerInput(ov.id, canvasWidthPx, canvasHeightPx) {
                                    detectDragGestures(
                                        onDragStart = { onSelectOverlay(ov.id) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val dx = dragAmount.x / canvasWidthPx.coerceAtLeast(1f)
                                            val dy = dragAmount.y / canvasHeightPx.coerceAtLeast(1f)
                                            onDragOverlay(ov.id, dx, dy)
                                        }
                                    )
                                }
                            } else Modifier
                        )
                        .clickable { onSelectOverlay(ov.id) }
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, CyanGlow, RoundedCornerShape(8.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    RenderProOverlayNode(ov = ov, ovProgress = ovProgress)
                }
            }

            // Bottom-Right Play / Pause floating control inside viewport
            if (clips.isNotEmpty()) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .testTag("video_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Minimized Top-Left Aspect Ratio Chip
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clickable { onCycleAspectRatio() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.AspectRatio, contentDescription = "Proporção", tint = CyanGlow, modifier = Modifier.size(14.dp))
                Text(aspectRatio, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Top-Right Preview Zoom Controls & Fullscreen Button
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (abs(viewportZoom - 1f) > 0.05f) {
                Surface(
                    color = OrangeAccent.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.clickable {
                        viewportZoom = 1f
                        viewportPanX = 0f
                        viewportPanY = 0f
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Repor Zoom", tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("%.1fx".format(viewportZoom), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            IconButton(
                onClick = { viewportZoom = (viewportZoom - 0.25f).coerceAtLeast(0.65f) },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
            ) {
                Icon(Icons.Filled.ZoomOut, contentDescription = "Reduzir preview", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = { viewportZoom = (viewportZoom + 0.25f).coerceAtMost(3.5f) },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
            ) {
                Icon(Icons.Filled.ZoomIn, contentDescription = "Ampliar preview", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
            ) {
                Icon(
                    imageVector = if (fullscreenPreview) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (fullscreenPreview) "Sair do ecrã inteiro" else "Ecrã inteiro",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RenderProOverlayNode(ov: ProOverlayItem, ovProgress: Float) {
    val context = LocalContext.current
    val fontFam = remember(ov.fontFamilyKey, ov.customFontUri) {
        VideoProProcessors.loadComposeFontFamily(context, ov.fontFamilyKey, ov.customFontUri)
    }

    when (ov.kind) {
        VideoOverlayKind.TEXT, VideoOverlayKind.SUBTITLE, VideoOverlayKind.WATERMARK -> {
            val shownText = if (ov.typewriterEffect) {
                val len = ((ovProgress * 1.35f).coerceIn(0.05f, 1f) * ov.text.length).toInt().coerceIn(1, ov.text.length)
                ov.text.take(len)
            } else ov.text

            val mainColor = parseComposeColor(ov.colorHex, Color.White)
            val bgColor = ov.bgColorHex?.let { parseComposeColor(it, Color.Transparent) }
                ?: if (ov.kind == VideoOverlayKind.SUBTITLE) Color.Black.copy(alpha = 0.68f) else Color.Transparent
            val align = when (ov.textAlign) {
                "left" -> TextAlign.Start
                "right" -> TextAlign.End
                else -> TextAlign.Center
            }
            val textBrush = if (!ov.gradientStartHex.isNullOrBlank() && !ov.gradientEndHex.isNullOrBlank()) {
                Brush.linearGradient(
                    listOf(
                        parseComposeColor(ov.gradientStartHex, mainColor),
                        parseComposeColor(ov.gradientEndHex, mainColor)
                    )
                )
            } else null

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (ov.is3DText) {
                    Text(
                        text = shownText,
                        fontFamily = fontFam,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = ov.fontSizeSp.sp,
                        color = Color(0xCC0D1B2A),
                        textAlign = align,
                        modifier = Modifier.offset(x = 3.dp, y = 3.dp)
                    )
                }
                if (ov.outlineEnabled) {
                    val strokeCol = parseComposeColor(ov.outlineColorHex, Color.Black)
                    listOf(Offset(-1.5f, 0f), Offset(1.5f, 0f), Offset(0f, -1.5f), Offset(0f, 1.5f)).forEach { d ->
                        Text(
                            text = shownText,
                            fontFamily = fontFam,
                            fontWeight = FontWeight.Bold,
                            fontSize = ov.fontSizeSp.sp,
                            color = strokeCol,
                            textAlign = align,
                            modifier = Modifier.offset(x = d.x.dp, y = d.y.dp)
                        )
                    }
                }
                Text(
                    text = shownText,
                    style = if (textBrush != null) {
                        TextStyle(
                            brush = textBrush,
                            fontFamily = fontFam,
                            fontWeight = FontWeight.Bold,
                            fontSize = ov.fontSizeSp.sp,
                            textAlign = align
                        )
                    } else {
                        TextStyle(
                            color = mainColor,
                            fontFamily = fontFam,
                            fontWeight = FontWeight.Bold,
                            fontSize = ov.fontSizeSp.sp,
                            textAlign = align
                        )
                    }
                )
            }
        }
        VideoOverlayKind.IMAGE, VideoOverlayKind.VIDEO_PIP, VideoOverlayKind.LOGO -> {
            val keyedBmp by produceState<Bitmap?>(
                initialValue = null,
                ov.uriString,
                ov.chromaKeyEnabled,
                ov.chromaKeyColorHex,
                ov.chromaKeyTolerance,
                ov.maskShape
            ) {
                value = if (ov.chromaKeyEnabled || ov.maskShape != VideoMaskShape.NONE) {
                    withContext(Dispatchers.IO) {
                        var b = VideoProProcessors.loadBitmapSync(context, ov.uriString, 360)
                        if (b != null && ov.chromaKeyEnabled) {
                            val k = VideoProProcessors.applyChromaKeyToBitmap(b, ov.chromaKeyColorHex, ov.chromaKeyTolerance)
                            if (k !== b) b.recycle()
                            b = k
                        }
                        if (b != null && ov.maskShape != VideoMaskShape.NONE) {
                            val m = VideoProProcessors.applyMaskToBitmap(b, ov.maskShape)
                            if (m !== b) b.recycle()
                            b = m
                        }
                        b
                    }
                } else null
            }

            val blend = when (ov.blendMode) {
                VideoBlendMode.MULTIPLY -> BlendMode.Multiply
                VideoBlendMode.SCREEN -> BlendMode.Screen
                VideoBlendMode.OVERLAY -> BlendMode.Overlay
                VideoBlendMode.DARKEN -> BlendMode.Darken
                VideoBlendMode.LIGHTEN -> BlendMode.Lighten
                VideoBlendMode.ADD -> BlendMode.Plus
                VideoBlendMode.NORMAL -> BlendMode.SrcOver
            }

            if (keyedBmp != null) {
                Image(
                    bitmap = keyedBmp!!.asImageBitmap(),
                    contentDescription = ov.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(92.dp)
                )
            } else {
                val model = TemplateSeedCatalog.resolvePlayableUri(context, ov.uriString)
                AsyncImage(
                    model = model,
                    contentDescription = ov.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                )
                if (blend != BlendMode.SrcOver) {
                    Box(modifier = Modifier.size(90.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }
        }
        VideoOverlayKind.VERIFIED_BADGE -> {
            val badgeCol = parseComposeColor(ov.colorHex, Color(0xFF1D9BF0))
            Canvas(modifier = Modifier.size(44.dp)) {
                val r = size.minDimension * 0.44f
                val c = center
                drawCircle(color = badgeCol, radius = r * 0.78f, center = c)
                for (i in 0 until 8) {
                    val a = i * 45.0 * PI / 180.0
                    drawCircle(
                        color = badgeCol,
                        radius = r * 0.28f,
                        center = Offset(c.x + (r * 0.72f * cos(a)).toFloat(), c.y + (r * 0.72f * sin(a)).toFloat())
                    )
                }
                val check = Path().apply {
                    moveTo(c.x - r * 0.34f, c.y + r * 0.02f)
                    lineTo(c.x - r * 0.08f, c.y + r * 0.28f)
                    lineTo(c.x + r * 0.36f, c.y - r * 0.24f)
                }
                drawPath(check, color = Color.White, style = Stroke(width = (r * 0.22f).coerceAtLeast(3f)))
            }
        }
        VideoOverlayKind.EMOJI, VideoOverlayKind.STICKER -> {
            Text(
                text = ov.text.ifBlank { "★" },
                fontSize = (ov.fontSizeSp * 1.45f).sp,
                color = parseComposeColor(ov.colorHex, Color.White)
            )
        }
        VideoOverlayKind.SHAPE, VideoOverlayKind.FRAME -> {
            val col = parseComposeColor(ov.colorHex, OrangeAccent)
            Canvas(modifier = Modifier.size(72.dp)) {
                if (ov.kind == VideoOverlayKind.FRAME) {
                    drawRect(color = col, style = Stroke(width = 6f))
                } else {
                    when (ov.symbolKey) {
                        "circle" -> drawCircle(color = col)
                        "star" -> {
                            val p = Path()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val outR = size.minDimension * 0.46f
                            val inR = outR * 0.44f
                            for (i in 0 until 10) {
                                val r = if (i % 2 == 0) outR else inR
                                val a = (i * 36.0 - 90.0) * PI / 180.0
                                val px = cx + (r * cos(a)).toFloat()
                                val py = cy + (r * sin(a)).toFloat()
                                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                            }
                            p.close()
                            drawPath(p, color = col)
                        }
                        else -> drawRoundRect(color = col, cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
                    }
                }
            }
        }
        VideoOverlayKind.EFFECT_CLIP -> Unit
    }
}

@Composable
private fun AnimatedBackgroundLayer(
    style: VideoBackgroundAnimatedStyle,
    phase: Float,
    baseStart: Color,
    baseEnd: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (style) {
            VideoBackgroundAnimatedStyle.NONE -> Unit
            VideoBackgroundAnimatedStyle.AURORA -> {
                val c1 = Offset(size.width * (0.35f + 0.25f * sin(phase)), size.height * (0.3f + 0.2f * cos(phase)))
                val c2 = Offset(size.width * (0.65f + 0.25f * cos(phase)), size.height * (0.7f + 0.2f * sin(phase)))
                drawCircle(
                    brush = Brush.radialGradient(listOf(CyanGlow.copy(alpha = 0.55f), Color.Transparent), center = c1, radius = size.minDimension * 0.65f),
                    radius = size.minDimension * 0.65f,
                    center = c1
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(OrangeAccent.copy(alpha = 0.45f), Color.Transparent), center = c2, radius = size.minDimension * 0.6f),
                    radius = size.minDimension * 0.6f,
                    center = c2
                )
            }
            VideoBackgroundAnimatedStyle.PULSE_GRID -> {
                val step = 48f
                val alphaPulse = 0.16f + 0.14f * (0.5f + 0.5f * sin(phase * 2f))
                val col = CyanGlow.copy(alpha = alphaPulse)
                var x = (phase * 15f) % step
                while (x < size.width) {
                    drawLine(col, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                    x += step
                }
                var y = (phase * 15f) % step
                while (y < size.height) {
                    drawLine(col, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                    y += step
                }
            }
            VideoBackgroundAnimatedStyle.PARTICLES -> {
                val rng = Random(42)
                for (i in 0 until 24) {
                    val bx = rng.nextFloat() * size.width
                    val by = ((rng.nextFloat() + phase / (2f * PI.toFloat())) % 1f) * size.height
                    val r = 4f + rng.nextFloat() * 9f
                    drawCircle(
                        color = (if (i % 2 == 0) OrangeBright else CyanGlow).copy(alpha = 0.45f),
                        radius = r,
                        center = Offset(bx, by)
                    )
                }
            }
            VideoBackgroundAnimatedStyle.SUNSET_WAVE -> {
                val p = Path().apply {
                    moveTo(0f, size.height * 0.6f)
                    var x = 0f
                    while (x <= size.width) {
                        val y = size.height * 0.55f + sin(x * 0.015f + phase * 2f) * 32f
                        lineTo(x, y)
                        x += 20f
                    }
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(p, brush = Brush.verticalGradient(listOf(OrangeAccent.copy(alpha = 0.55f), baseEnd.copy(alpha = 0.85f))))
            }
            VideoBackgroundAnimatedStyle.NEON_SCAN -> {
                val scanY = ((phase / (2f * PI.toFloat())) % 1f) * size.height
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, CyanGlow.copy(alpha = 0.45f), Color.Transparent),
                        startY = scanY - 40f,
                        endY = scanY + 40f
                    ),
                    topLeft = Offset(0f, scanY - 40f),
                    size = Size(size.width, 80f)
                )
            }
        }
    }
}

@Composable
private fun ProEffectsAndGradingOverlay(
    clip: VideoClipItem,
    effectKind: ProEffectKind,
    phase: Float,
    flashAlpha: Float
) {
    // Video surface tint (since AndroidView PlayerView doesn't use Compose ColorFilter directly)
    val filterOverlay = when (clip.filter) {
        VideoFilterKind.WARM -> Color(0x28FF8C00)
        VideoFilterKind.COOL -> Color(0x2800A8FF)
        VideoFilterKind.CINEMA -> Color(0x26000000)
        VideoFilterKind.BW, VideoFilterKind.NOIR -> Color(0x40757575)
        VideoFilterKind.VINTAGE -> Color(0x30A67C52)
        VideoFilterKind.VIVID -> Color(0x18FF2E93)
        VideoFilterKind.TEAL_ORANGE -> Color(0x2200B4D8)
        VideoFilterKind.NORMAL -> Color.Transparent
    }
    if (filterOverlay != Color.Transparent) {
        Box(modifier = Modifier.fillMaxSize().background(filterOverlay))
    }

    // Temperature + Brightness visual overlay for video preview
    if (abs(clip.brightness + clip.exposure) > 0.03f) {
        val b = (clip.brightness + clip.exposure).coerceIn(-0.6f, 0.6f)
        val col = if (b > 0f) Color.White.copy(alpha = b * 0.4f) else Color.Black.copy(alpha = abs(b) * 0.55f)
        Box(modifier = Modifier.fillMaxSize().background(col))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val inten = clip.effectIntensity.coerceIn(0.1f, 1f)
        when (effectKind) {
            ProEffectKind.NONE -> Unit
            ProEffectKind.CINEMA_BARS -> {
                val barH = size.height * 0.11f * inten
                drawRect(Color.Black, Offset.Zero, Size(size.width, barH))
                drawRect(Color.Black, Offset(0f, size.height - barH), Size(size.width, barH))
            }
            ProEffectKind.GLITCH -> {
                val y1 = ((sin(phase * 5f) * 0.5f + 0.5f) * size.height * 0.85f)
                val y2 = ((cos(phase * 7f) * 0.5f + 0.5f) * size.height * 0.85f)
                drawRect(Color(0x55FF0055), Offset(0f, y1), Size(size.width, 14f * inten))
                drawRect(Color(0x5500FFFF), Offset(0f, y2), Size(size.width, 10f * inten))
            }
            ProEffectKind.EFFECT_3D -> {
                drawRect(Color(0x28FF003C), Offset(-8f * inten, 0f), size)
                drawRect(Color(0x2800E5FF), Offset(8f * inten, 0f), size)
            }
            ProEffectKind.LIGHT_FLARE -> {
                val c = Offset(size.width * (0.25f + 0.5f * (0.5f + 0.5f * sin(phase))), size.height * 0.22f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.55f * inten), OrangeBright.copy(alpha = 0.28f * inten), Color.Transparent),
                        center = c,
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = c
                )
            }
            ProEffectKind.VINTAGE_FILM -> {
                drawRect(Color(0x33704214), Offset.Zero, size)
            }
            ProEffectKind.BLUR, ProEffectKind.MOTION_BLUR, ProEffectKind.SHAKE_MOTION -> Unit
        }

        if (clip.vignette > 0.02f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = (clip.vignette * 0.8f).coerceIn(0f, 0.85f))),
                    center = center,
                    radius = size.maxDimension * 0.72f
                )
            )
        }
        if (clip.grain > 0.02f) {
            val rng = Random((phase * 100).toInt())
            val dots = (clip.grain * 90).toInt().coerceIn(8, 90)
            for (i in 0 until dots) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f * clip.grain),
                    radius = 1.8f,
                    center = Offset(rng.nextFloat() * size.width, rng.nextFloat() * size.height)
                )
            }
        }
        if (flashAlpha > 0.01f) {
            drawRect(Color.White.copy(alpha = flashAlpha.coerceIn(0f, 0.9f)))
        }
    }
}

private data class AnimTransformState(
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val tx: Float = 0f,
    val ty: Float = 0f,
    val alpha: Float = 1f
)

private fun computeAnimationTransform(
    animIn: String,
    animOut: String,
    animCombo: String,
    progress: Float,
    clipDurationMs: Long,
    animDurationMs: Long
): AnimTransformState {
    var scale = 1f
    var rot = 0f
    var tx = 0f
    var ty = 0f
    var alpha = 1f

    val durRatio = (animDurationMs.toFloat() / clipDurationMs.coerceAtLeast(500L).toFloat()).coerceIn(0.15f, 0.45f)

    if (animIn != "none" && progress < durRatio) {
        val t = (progress / durRatio).coerceIn(0f, 1f)
        when (animIn) {
            "fade" -> alpha *= t
            "zoom_in" -> scale *= (0.5f + 0.5f * t)
            "zoom_out" -> scale *= (1.4f - 0.4f * t)
            "slide_up" -> { ty += (1f - t) * 140f; alpha *= t }
            "slide_left" -> { tx += (1f - t) * 180f; alpha *= t }
            "pop" -> scale *= (0.4f + 0.72f * sin(t * PI * 0.65).toFloat())
            "spin" -> { rot += (1f - t) * -180f; alpha *= t }
        }
    }
    if (animOut != "none" && progress > (1f - durRatio)) {
        val t = ((1f - progress) / durRatio).coerceIn(0f, 1f)
        when (animOut) {
            "fade" -> alpha *= t
            "zoom_out" -> { scale *= (0.5f + 0.5f * t); alpha *= t }
            "slide_down" -> { ty += (1f - t) * 140f; alpha *= t }
        }
    }
    when (animCombo) {
        "pulse" -> scale *= (1f + 0.08f * sin(progress * 4 * PI).toFloat())
        "pendulum" -> rot += 8f * sin(progress * 4 * PI).toFloat()
        "ken_burns" -> scale *= (1f + 0.22f * progress)
    }
    return AnimTransformState(scale, rot, tx, ty, alpha)
}

private data class TransitionTransformState(
    val scale: Float = 1f,
    val rotationZ: Float = 0f,
    val rotationY: Float = 0f,
    val tx: Float = 0f,
    val ty: Float = 0f,
    val alpha: Float = 1f,
    val blurDp: Float = 0f,
    val flashAlpha: Float = 0f
)

private fun computeTransitionTransform(
    transition: VideoTransitionKind,
    progress: Float,
    clipDurationMs: Long,
    transDurationMs: Long
): TransitionTransformState {
    if (transition == VideoTransitionKind.NONE) return TransitionTransformState()
    val transRatio = (transDurationMs.toFloat() / clipDurationMs.coerceAtLeast(600L).toFloat()).coerceIn(0.1f, 0.4f)
    if (progress > transRatio) return TransitionTransformState()
    val t = (progress / transRatio).coerceIn(0f, 1f)
    return when (transition) {
        VideoTransitionKind.NONE -> TransitionTransformState()
        VideoTransitionKind.FADE -> TransitionTransformState(alpha = t)
        VideoTransitionKind.SLIDE -> TransitionTransformState(tx = (1f - t) * 260f, alpha = 0.4f + 0.6f * t)
        VideoTransitionKind.ZOOM -> TransitionTransformState(scale = 0.65f + 0.35f * t, alpha = t)
        VideoTransitionKind.FLASH -> TransitionTransformState(flashAlpha = (1f - t) * 0.85f)
        VideoTransitionKind.BLUR -> TransitionTransformState(blurDp = (1f - t) * 14f, alpha = 0.5f + 0.5f * t)
        VideoTransitionKind.ROTATE -> TransitionTransformState(rotationZ = (1f - t) * -90f, scale = 0.7f + 0.3f * t, alpha = t)
        VideoTransitionKind.FLIP_3D -> TransitionTransformState(rotationY = (1f - t) * 90f, alpha = 0.3f + 0.7f * t)
        VideoTransitionKind.CINEMATIC -> TransitionTransformState(scale = 1.18f - 0.18f * t, alpha = t)
        VideoTransitionKind.CUSTOM -> TransitionTransformState(scale = 0.85f + 0.15f * t, ty = (1f - t) * -80f, alpha = t)
    }
}

private fun buildProColorMatrix(clip: VideoClipItem): ColorMatrix {
    val baseSat = when (clip.filter) {
        VideoFilterKind.BW, VideoFilterKind.NOIR -> 0f
        VideoFilterKind.VIVID -> clip.saturation * 1.4f
        VideoFilterKind.VINTAGE -> clip.saturation * 0.75f
        else -> clip.saturation
    }
    val cm = android.graphics.ColorMatrix().apply { setSaturation(baseSat.coerceIn(0f, 2.5f)) }

    val contrast = (clip.contrast * when (clip.filter) {
        VideoFilterKind.CINEMA, VideoFilterKind.NOIR -> 1.22f
        VideoFilterKind.VIVID -> 1.14f
        else -> 1f
    }).coerceIn(0.4f, 2.2f)
    val brightOffset = (clip.brightness + clip.exposure) * 140f + ((-0.5f * contrast + 0.5f) * 255f)
    val tempR = 1f + clip.temperature * 0.18f
    val tempB = 1f - clip.temperature * 0.18f
    val gammaScale = clip.rgbGamma.coerceIn(0.6f, 1.6f)

    cm.postConcat(
        android.graphics.ColorMatrix(
            floatArrayOf(
                contrast * tempR * gammaScale, 0f, 0f, 0f, brightOffset,
                0f, contrast * gammaScale, 0f, 0f, brightOffset,
                0f, 0f, contrast * tempB * gammaScale, 0f, brightOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    if (abs(clip.hueShift) > 1f) {
        val cosV = cos(clip.hueShift * PI / 180.0).toFloat()
        val sinV = sin(clip.hueShift * PI / 180.0).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        cm.postConcat(
            android.graphics.ColorMatrix(
                floatArrayOf(
                    lumR + cosV * (1 - lumR) + sinV * (-lumR), lumG + cosV * (-lumG) + sinV * (-lumG), lumB + cosV * (-lumB) + sinV * (1 - lumB), 0f, 0f,
                    lumR + cosV * (-lumR) + sinV * 0.143f, lumG + cosV * (1 - lumG) + sinV * 0.140f, lumB + cosV * (-lumB) + sinV * (-0.283f), 0f, 0f,
                    lumR + cosV * (-lumR) + sinV * (-(1 - lumR)), lumG + cosV * (-lumG) + sinV * lumG, lumB + cosV * (1 - lumB) + sinV * lumB, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    return ColorMatrix(cm.array)
}
