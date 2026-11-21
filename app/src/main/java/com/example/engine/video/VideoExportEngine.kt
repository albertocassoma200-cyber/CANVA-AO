package com.example.engine.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.example.core.model.ProAudioTrack
import com.example.core.model.ProOverlayItem
import com.example.core.model.TemplateSeedCatalog
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoExportQuality
import com.example.core.model.VideoFilterKind
import com.example.core.model.VideoMaskShape
import com.example.core.model.VideoOverlayKind
import com.example.core.model.VideoTextOverlay
import com.example.engine.audio.VideoAudioStudioEngine
import com.example.engine.graphics.BitmapRenderer
import com.example.engine.graphics.ExportedFileResult
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class VideoResolutionPreset(val label: String, val targetHeight: Int, val baseBitrate: Int) {
    HD_720("720p HD", 720, 4_500_000),
    FHD_1080("1080p Full HD", 1080, 8_500_000),
    UHD_4K("4K Ultra HD", 2160, 20_000_000)
}

@OptIn(UnstableApi::class)
class VideoExportEngine(private val context: Context) {

    private var activeTransformer: Transformer? = null
    private var prepareJob: Job? = null
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    @OptIn(UnstableApi::class)
    fun exportMp4(
        projectTitle: String,
        clips: List<VideoClipItem>,
        audioUri: String? = null,
        audioMuted: Boolean = false,
        textOverlays: List<VideoTextOverlay> = emptyList(),
        resolution: VideoResolutionPreset = VideoResolutionPreset.FHD_1080,
        aspectRatio: String = "9:16",
        fps: Int = 30,
        quality: VideoExportQuality = VideoExportQuality.HIGH,
        audioTracks: List<ProAudioTrack> = emptyList(),
        proOverlays: List<ProOverlayItem> = emptyList(),
        onProgress: (Int) -> Unit,
        onSuccess: (ExportedFileResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (clips.isEmpty()) {
            onError("Adicione pelo menos um vídeo ou foto à linha do tempo antes de exportar.")
            return
        }

        val cleanName = projectTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(36).ifBlank { "canva_ao_video" }
        val fileName = "${cleanName}_${System.currentTimeMillis()}.mp4"
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val outFile = File(exportDir, fileName)

        prepareJob?.cancel()
        prepareJob = engineScope.launch {
            var mixedWavFile: File? = null
            var overlayBitmap: Bitmap? = null
            try {
                onProgress(3)
                val totalDurMs = clips.sumOf {
                    ((it.trimEndMs - it.trimStartMs).coerceAtLeast(1000L) / it.speed.coerceIn(0.25f, 4f)).toLong()
                }.coerceAtLeast(1500L)

                if (!audioMuted && audioTracks.any { !it.muted && it.volume > 0.01f }) {
                    mixedWavFile = VideoAudioStudioEngine.renderMixedAudioToWav(
                        context = context,
                        tracks = audioTracks,
                        totalDurationMs = totalDurMs
                    )
                }

                val (rawW, rawH) = when (aspectRatio) {
                    "9:16" -> (resolution.targetHeight * 9 / 16) to resolution.targetHeight
                    "3:4" -> (resolution.targetHeight * 3 / 4) to resolution.targetHeight
                    "1:1" -> resolution.targetHeight to resolution.targetHeight
                    "4:5" -> (resolution.targetHeight * 4 / 5) to resolution.targetHeight
                    else -> (resolution.targetHeight * 16 / 9) to resolution.targetHeight
                }
                val outW = (rawW - (rawW % 2)).coerceAtLeast(360)
                val outH = (rawH - (rawH % 2)).coerceAtLeast(360)

                val visibleOverlays = proOverlays.filter { it.visible }
                if (visibleOverlays.isNotEmpty() || textOverlays.isNotEmpty()) {
                    overlayBitmap = renderCompositeOverlayBitmap(
                        width = outW,
                        height = outH,
                        proOverlays = visibleOverlays,
                        legacyTexts = if (visibleOverlays.isEmpty()) textOverlays else emptyList()
                    )
                }

                val hasMixedAudio = mixedWavFile != null
                val hasLegacyAudio = !hasMixedAudio && !audioUri.isNullOrBlank() && !audioMuted

                val editedItems = clips.map { clip ->
                    val uri = TemplateSeedCatalog.resolvePlayableUri(context, clip.uriString)
                        ?: Uri.parse(clip.uriString)
                    val itemBuilder = MediaItem.Builder().setUri(uri)
                    if (clip.mediaType == "image") {
                        itemBuilder.setImageDurationMs(
                            ((clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1500L) / clip.speed.coerceIn(0.25f, 4f)).toLong()
                        )
                    } else if (clip.trimEndMs > clip.trimStartMs) {
                        itemBuilder.setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                                .setEndPositionMs(clip.trimEndMs)
                                .build()
                        )
                    }
                    val mediaItem = itemBuilder.build()

                    val videoEffects = mutableListOf<Effect>()
                    if (clip.zoomScale != 1f || clip.rotationDeg != 0f || clip.flipH || clip.flipV) {
                        val sx = (if (clip.flipH) -clip.zoomScale else clip.zoomScale).coerceIn(-4f, 4f)
                        val sy = (if (clip.flipV) -clip.zoomScale else clip.zoomScale).coerceIn(-4f, 4f)
                        videoEffects.add(
                            ScaleAndRotateTransformation.Builder()
                                .setScale(sx, sy)
                                .setRotationDegrees(-clip.rotationDeg)
                                .build()
                        )
                    }

                    // Filter presets + Pro Color Grading
                    when (clip.filter) {
                        VideoFilterKind.BW, VideoFilterKind.NOIR -> {
                            videoEffects.add(RgbFilter.createGrayscaleFilter())
                            if (clip.filter == VideoFilterKind.NOIR) {
                                videoEffects.add(Contrast(0.35f))
                            }
                        }
                        VideoFilterKind.CINEMA -> videoEffects.add(Contrast(0.28f))
                        VideoFilterKind.WARM -> {
                            videoEffects.add(
                                RgbAdjustment.Builder()
                                    .setRedScale(1.12f)
                                    .setBlueScale(0.88f)
                                    .build()
                            )
                        }
                        VideoFilterKind.COOL -> {
                            videoEffects.add(
                                RgbAdjustment.Builder()
                                    .setRedScale(0.90f)
                                    .setBlueScale(1.14f)
                                    .build()
                            )
                        }
                        VideoFilterKind.VINTAGE -> {
                            videoEffects.add(
                                RgbAdjustment.Builder()
                                    .setRedScale(1.08f)
                                    .setGreenScale(0.96f)
                                    .setBlueScale(0.80f)
                                    .build()
                            )
                        }
                        VideoFilterKind.VIVID -> {
                            videoEffects.add(HslAdjustment.Builder().adjustSaturation(38f).build())
                            videoEffects.add(Contrast(0.18f))
                        }
                        VideoFilterKind.TEAL_ORANGE -> {
                            videoEffects.add(
                                RgbAdjustment.Builder()
                                    .setRedScale(1.10f)
                                    .setGreenScale(1.02f)
                                    .setBlueScale(1.06f)
                                    .build()
                            )
                        }
                        VideoFilterKind.NORMAL -> Unit
                    }

                    val combinedBrightness = (clip.brightness + clip.exposure).coerceIn(-0.8f, 0.8f)
                    if (kotlin.math.abs(combinedBrightness) > 0.02f) {
                        videoEffects.add(Brightness(combinedBrightness))
                    }
                    val contrastDelta = (clip.contrast - 1f).coerceIn(-0.85f, 1.0f)
                    if (kotlin.math.abs(contrastDelta) > 0.02f) {
                        videoEffects.add(Contrast(contrastDelta))
                    }
                    if (kotlin.math.abs(clip.saturation - 1f) > 0.02f || kotlin.math.abs(clip.hueShift) > 1f) {
                        videoEffects.add(
                            HslAdjustment.Builder()
                                .adjustHue(clip.hueShift.coerceIn(-180f, 180f))
                                .adjustSaturation(((clip.saturation - 1f) * 100f).coerceIn(-100f, 100f))
                                .build()
                        )
                    }
                    if (kotlin.math.abs(clip.temperature) > 0.03f || kotlin.math.abs(clip.rgbGamma - 1f) > 0.03f) {
                        val t = clip.temperature.coerceIn(-1f, 1f)
                        val g = clip.rgbGamma.coerceIn(0.6f, 1.5f)
                        videoEffects.add(
                            RgbAdjustment.Builder()
                                .setRedScale(((1f + t * 0.16f) * g).coerceIn(0.4f, 1.8f))
                                .setGreenScale(g.coerceIn(0.4f, 1.8f))
                                .setBlueScale(((1f - t * 0.16f) * g).coerceIn(0.4f, 1.8f))
                                .build()
                        )
                    }

                    val audioProcessors = mutableListOf<androidx.media3.common.audio.AudioProcessor>()
                    if (kotlin.math.abs(clip.speed - 1.0f) > 0.02f) {
                        audioProcessors.add(
                            SonicAudioProcessor().apply {
                                setSpeed(clip.speed.coerceIn(0.25f, 4.0f))
                            }
                        )
                    }

                    val itemBuilderEdited = EditedMediaItem.Builder(mediaItem)
                        .setEffects(Effects(audioProcessors, videoEffects))
                        .setRemoveAudio(clip.muted || audioMuted || hasMixedAudio || hasLegacyAudio)

                    if (clip.mediaType == "image") {
                        itemBuilderEdited.setFrameRate(fps.coerceIn(24, 60))
                    }
                    itemBuilderEdited.build()
                }

                val videoSequence = if (clips.any { it.mediaType == "video" } && !hasMixedAudio && !hasLegacyAudio) {
                    EditedMediaItemSequence.withAudioAndVideoFrom(editedItems)
                } else {
                    EditedMediaItemSequence.withVideoFrom(editedItems)
                }

                val sequences = mutableListOf(videoSequence)
                if (mixedWavFile != null) {
                    val mixItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(mixedWavFile))).build()
                    val mixSeq = EditedMediaItemSequence.withAudioFrom(listOf(mixItem))
                    sequences.add(mixSeq)
                } else if (hasLegacyAudio && audioUri != null) {
                    val resolvedAudioUri = TemplateSeedCatalog.resolvePlayableUri(context, audioUri) ?: Uri.parse(audioUri)
                    val bgAudioItem = EditedMediaItem.Builder(MediaItem.fromUri(resolvedAudioUri)).build()
                    val bgAudioSeq = EditedMediaItemSequence.withAudioFrom(listOf(bgAudioItem))
                        .buildUpon()
                        .setIsLooping(true)
                        .build()
                    sequences.add(bgAudioSeq)
                }

                val globalVideoEffects = mutableListOf<Effect>()
                globalVideoEffects.add(
                    Presentation.createForWidthAndHeight(
                        outW,
                        outH,
                        Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                    )
                )

                overlayBitmap?.let { ovBmp ->
                    val overlays = ImmutableList.builder<TextureOverlay>()
                    overlays.add(
                        BitmapOverlay.createStaticBitmapOverlay(
                            ovBmp,
                            StaticOverlaySettings.Builder().build()
                        )
                    )
                    globalVideoEffects.add(OverlayEffect(overlays.build()))
                }

                val composition = Composition.Builder(sequences)
                    .setEffects(Effects(emptyList(), globalVideoEffects))
                    .build()

                val targetBitrate = (resolution.baseBitrate * quality.bitrateMultiplier * (fps / 30f))
                    .toInt()
                    .coerceIn(1_500_000, 35_000_000)

                withContext(Dispatchers.Main) {
                    val encoderFactory = DefaultEncoderFactory.Builder(context)
                        .setRequestedVideoEncoderSettings(
                            VideoEncoderSettings.Builder()
                                .setBitrate(targetBitrate)
                                .build()
                        )
                        .build()

                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setEncoderFactory(encoderFactory)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                stopProgressPolling()
                                activeTransformer = null
                                overlayBitmap?.recycle()
                                mixedWavFile?.delete()
                                val galleryUri = insertVideoIntoGallery(context, outFile, fileName)
                                val shareUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    outFile
                                )
                                onProgress(100)
                                onSuccess(
                                    ExportedFileResult(
                                        fileName = fileName,
                                        fileSizeBytes = outFile.length(),
                                        shareUri = shareUri,
                                        galleryUri = galleryUri
                                    )
                                )
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                stopProgressPolling()
                                activeTransformer = null
                                overlayBitmap?.recycle()
                                mixedWavFile?.delete()
                                if (outFile.exists()) outFile.delete()
                                onError(exportException.localizedMessage ?: "Erro ao processar vídeo MP4.")
                            }
                        })
                        .build()

                    activeTransformer = transformer
                    transformer.start(composition, outFile.absolutePath)
                    startProgressPolling(transformer, onProgress)
                }
            } catch (e: Exception) {
                overlayBitmap?.recycle()
                mixedWavFile?.delete()
                if (outFile.exists()) outFile.delete()
                withContext(Dispatchers.Main) {
                    stopProgressPolling()
                    activeTransformer = null
                    onError(e.localizedMessage ?: "Não foi possível iniciar a exportação de vídeo.")
                }
            }
        }
    }

    private fun renderCompositeOverlayBitmap(
        width: Int,
        height: Int,
        proOverlays: List<ProOverlayItem>,
        legacyTexts: List<VideoTextOverlay>
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val scale = min(width, height) / 400f

        for (ov in proOverlays) {
            if (!ov.visible || ov.opacity <= 0.01f) continue
            canvas.save()
            val cx = ov.xFraction * width
            val cy = ov.yFraction * height
            canvas.translate(cx, cy)
            canvas.rotate(ov.rotation)
            canvas.scale(ov.scale, ov.scale)

            val alphaInt = (ov.opacity.coerceIn(0f, 1f) * 255).toInt()
            when (ov.kind) {
                VideoOverlayKind.TEXT, VideoOverlayKind.SUBTITLE, VideoOverlayKind.WATERMARK -> {
                    drawProTextOnCanvas(canvas, ov, width, scale, alphaInt)
                }
                VideoOverlayKind.IMAGE, VideoOverlayKind.VIDEO_PIP, VideoOverlayKind.LOGO -> {
                    var imgBmp = VideoProProcessors.loadBitmapSync(context, ov.uriString, 640)
                    if (imgBmp != null) {
                        if (ov.chromaKeyEnabled) {
                            val keyed = VideoProProcessors.applyChromaKeyToBitmap(
                                imgBmp, ov.chromaKeyColorHex, ov.chromaKeyTolerance
                            )
                            if (keyed !== imgBmp) imgBmp.recycle()
                            imgBmp = keyed
                        }
                        if (ov.maskShape != VideoMaskShape.NONE) {
                            val masked = VideoProProcessors.applyMaskToBitmap(imgBmp, ov.maskShape)
                            if (masked !== imgBmp) imgBmp.recycle()
                            imgBmp = masked
                        }
                        val boxW = width * 0.34f
                        val ratio = imgBmp.height.toFloat() / imgBmp.width.coerceAtLeast(1).toFloat()
                        val boxH = boxW * ratio
                        val rect = RectF(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f)
                        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = alphaInt }
                        canvas.drawBitmap(imgBmp, null, rect, p)
                        imgBmp.recycle()
                    }
                }
                VideoOverlayKind.VERIFIED_BADGE -> {
                    val r = 24f * scale
                    val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = BitmapRenderer.parseColorSafe(ov.colorHex, Color.parseColor("#1D9BF0"))
                        alpha = alphaInt
                    }
                    canvas.drawCircle(0f, 0f, r * 0.8f, sealPaint)
                    for (i in 0 until 8) {
                        val a = (i * 45.0) * PI / 180.0
                        canvas.drawCircle((r * 0.72f * cos(a)).toFloat(), (r * 0.72f * sin(a)).toFloat(), r * 0.27f, sealPaint)
                    }
                    val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        alpha = alphaInt
                        style = Paint.Style.STROKE
                        strokeWidth = (r * 0.22f).coerceAtLeast(3f)
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    val path = Path().apply {
                        moveTo(-r * 0.34f, r * 0.02f)
                        lineTo(-r * 0.08f, r * 0.28f)
                        lineTo(r * 0.36f, -r * 0.24f)
                    }
                    canvas.drawPath(path, checkPaint)
                }
                VideoOverlayKind.EMOJI, VideoOverlayKind.STICKER -> {
                    val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = (ov.fontSizeSp * 1.4f * scale).coerceAtLeast(24f)
                        textAlign = Paint.Align.CENTER
                        alpha = alphaInt
                    }
                    canvas.drawText(ov.text.ifBlank { "★" }, 0f, p.textSize * 0.35f, p)
                }
                VideoOverlayKind.SHAPE, VideoOverlayKind.FRAME -> {
                    val sz = 96f * scale
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = BitmapRenderer.parseColorSafe(ov.colorHex, Color.parseColor("#FF7A00"))
                        alpha = alphaInt
                        style = if (ov.kind == VideoOverlayKind.FRAME) Paint.Style.STROKE else Paint.Style.FILL
                        strokeWidth = 6f * scale
                    }
                    val r = RectF(-sz / 2f, -sz / 2f, sz / 2f, sz / 2f)
                    canvas.drawRoundRect(r, 14f * scale, 14f * scale, p)
                }
                VideoOverlayKind.EFFECT_CLIP -> Unit
            }
            canvas.restore()
        }

        for (txt in legacyTexts) {
            val dummy = ProOverlayItem(
                kind = if (txt.isSubtitle) VideoOverlayKind.SUBTITLE else VideoOverlayKind.TEXT,
                text = txt.text,
                yFraction = txt.yPositionFraction,
                colorHex = txt.colorHex,
                bgColorHex = if (txt.isSubtitle) "#99000000" else null
            )
            canvas.save()
            canvas.translate(width * 0.5f, txt.yPositionFraction * height)
            drawProTextOnCanvas(canvas, dummy, width, scale, 255)
            canvas.restore()
        }

        return bmp
    }

    private fun drawProTextOnCanvas(
        canvas: Canvas,
        ov: ProOverlayItem,
        videoWidth: Int,
        scale: Float,
        alphaInt: Int
    ) {
        if (ov.text.isBlank()) return
        val tf = Typeface.create(
            VideoProProcessors.loadTypeface(context, ov.fontFamilyKey, ov.customFontUri),
            Typeface.BOLD
        )
        val layoutW = (videoWidth * 0.84f).toInt().coerceAtLeast(80)
        val mainColor = BitmapRenderer.parseColorSafe(ov.colorHex, Color.WHITE)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = (ov.fontSizeSp * scale).coerceAtLeast(16f)
            color = mainColor
            alpha = alphaInt
            if (ov.shadowEnabled) {
                setShadowLayer(6f * scale, 2f * scale, 3f * scale, Color.argb(180, 0, 0, 0))
            }
        }

        val alignment = when (ov.textAlign) {
            "left" -> Layout.Alignment.ALIGN_NORMAL
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }
        val layout = StaticLayout.Builder.obtain(ov.text, 0, ov.text.length, paint, layoutW)
            .setAlignment(alignment)
            .build()

        // Background box if set or subtitle
        val bgHex = ov.bgColorHex ?: if (ov.kind == VideoOverlayKind.SUBTITLE) "#A6000000" else null
        if (!bgHex.isNullOrBlank()) {
            val textW = (0 until layout.lineCount).maxOfOrNull { layout.getLineWidth(it) } ?: layoutW.toFloat()
            val padX = 18f * scale
            val padY = 10f * scale
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BitmapRenderer.parseColorSafe(bgHex, Color.argb(170, 0, 0, 0))
                alpha = (alphaInt * 0.88f).toInt()
            }
            val bgRect = RectF(
                -textW / 2f - padX,
                -layout.height / 2f - padY,
                textW / 2f + padX,
                layout.height / 2f + padY
            )
            canvas.drawRoundRect(bgRect, 10f * scale, 10f * scale, bgPaint)
        }

        // 3D extrusion layers
        if (ov.is3DText) {
            val depthPaint = TextPaint(paint).apply {
                color = Color.argb((alphaInt * 0.75f).toInt(), 15, 25, 45)
                clearShadowLayer()
            }
            for (d in 4 downTo 1) {
                val dl = StaticLayout.Builder.obtain(ov.text, 0, ov.text.length, depthPaint, layoutW)
                    .setAlignment(alignment).build()
                canvas.save()
                canvas.translate(-layoutW / 2f + d * 1.4f * scale, -dl.height / 2f + d * 1.4f * scale)
                dl.draw(canvas)
                canvas.restore()
            }
        }

        // Outline stroke
        if (ov.outlineEnabled) {
            val strokePaint = TextPaint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f * scale
                color = BitmapRenderer.parseColorSafe(ov.outlineColorHex, Color.BLACK)
                alpha = alphaInt
            }
            val sl = StaticLayout.Builder.obtain(ov.text, 0, ov.text.length, strokePaint, layoutW)
                .setAlignment(alignment).build()
            canvas.save()
            canvas.translate(-layoutW / 2f, -sl.height / 2f)
            sl.draw(canvas)
            canvas.restore()
        }

        // Gradient fill if configured
        if (!ov.gradientStartHex.isNullOrBlank() && !ov.gradientEndHex.isNullOrBlank()) {
            val g1 = BitmapRenderer.parseColorSafe(ov.gradientStartHex, mainColor)
            val g2 = BitmapRenderer.parseColorSafe(ov.gradientEndHex, mainColor)
            paint.shader = LinearGradient(
                0f, 0f, layoutW.toFloat(), layout.height.toFloat(),
                g1, g2, Shader.TileMode.CLAMP
            )
        }

        canvas.save()
        canvas.translate(-layoutW / 2f, -layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    @OptIn(UnstableApi::class)
    fun cancelExport() {
        prepareJob?.cancel()
        prepareJob = null
        stopProgressPolling()
        try {
            activeTransformer?.cancel()
        } catch (_: Exception) {
        }
        activeTransformer = null
    }

    @OptIn(UnstableApi::class)
    private fun startProgressPolling(transformer: Transformer, onProgress: (Int) -> Unit) {
        stopProgressPolling()
        val holder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                val state = transformer.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress.coerceIn(5, 99))
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED && activeTransformer != null) {
                    mainHandler.postDelayed(this, 350L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun insertVideoIntoGallery(context: Context, file: File, fileName: String): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/CANVA AO")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { os ->
                file.inputStream().use { input -> input.copyTo(os) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            null
        }
    }
}
