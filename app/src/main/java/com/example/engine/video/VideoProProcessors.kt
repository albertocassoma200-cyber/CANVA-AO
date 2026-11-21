package com.example.engine.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.example.core.model.ProAudioTrack
import com.example.core.model.ProOverlayItem
import com.example.core.model.TemplateSeedCatalog
import com.example.core.model.VideoBackgroundAnimatedStyle
import com.example.core.model.VideoBlendMode
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoEditorSnapshot
import com.example.core.model.VideoFilterKind
import com.example.core.model.VideoKeyframe
import com.example.core.model.VideoMaskShape
import com.example.core.model.VideoOverlayKind
import com.example.core.model.VideoTransitionKind
import com.example.core.model.ProEffectKind
import com.example.engine.audio.VideoAudioStudioEngine
import com.example.ui.theme.editorFontFamily
import com.example.ui.theme.editorFontResId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "VideoProProcessors"

data class ImportedFontInfo(
    val name: String,
    val fileUri: String
)

object VideoProProcessors {

    fun loadTypeface(context: Context, fontFamilyKey: String, customFontUri: String?): Typeface {
        if (!customFontUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(customFontUri)
                if (uri.scheme == "file" && uri.path != null) {
                    val f = File(uri.path!!)
                    if (f.exists()) return Typeface.createFromFile(f)
                }
            } catch (_: Exception) {
            }
        }
        return try {
            ResourcesCompat.getFont(context, editorFontResId(fontFamilyKey)) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    fun loadComposeFontFamily(context: Context, fontFamilyKey: String, customFontUri: String?): FontFamily {
        if (!customFontUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(customFontUri)
                if (uri.scheme == "file" && uri.path != null) {
                    val f = File(uri.path!!)
                    if (f.exists()) {
                        return FontFamily(Typeface.createFromFile(f))
                    }
                }
            } catch (_: Exception) {
            }
        }
        return editorFontFamily(fontFamilyKey)
    }

    suspend fun importCustomFontFile(context: Context, sourceUri: Uri): Result<ImportedFontInfo> =
        withContext(Dispatchers.IO) {
            try {
                val rawName = queryDisplayName(context, sourceUri)
                    ?: sourceUri.lastPathSegment?.substringAfterLast('/')
                    ?: "Fonte_${System.currentTimeMillis()}.ttf"
                val ext = rawName.substringAfterLast('.', "ttf").lowercase()
                    .takeIf { it in setOf("ttf", "otf") } ?: "ttf"
                val dir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
                val dest = File(dir, "font_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Não foi possível ler o ficheiro de fonte.")
                if (dest.length() < 64L) {
                    dest.delete()
                    throw IllegalStateException("Ficheiro de fonte inválido.")
                }
                // Verify Typeface parses properly
                Typeface.createFromFile(dest)
                Result.success(
                    ImportedFontInfo(
                        name = rawName.substringBeforeLast('.').ifBlank { "Fonte Personalizada" },
                        fileUri = Uri.fromFile(dest).toString()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Font import failed", e)
                Result.failure(e)
            }
        }

    fun loadBitmapSync(context: Context, uriString: String?, maxDim: Int = 720): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val resId = TemplateSeedCatalog.resolveDrawableResId(uriString)
            val raw = if (resId != null) {
                BitmapFactory.decodeResource(context.resources, resId)
            } else {
                val uri = TemplateSeedCatalog.resolvePlayableUri(context, uriString) ?: Uri.parse(uriString)
                if (uri.scheme == "file" && uri.path != null) {
                    BitmapFactory.decodeFile(uri.path!!)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { dec, _, _ -> dec.isMutableRequired = true }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
            } ?: return null

            val w = raw.width
            val h = raw.height
            if (max(w, h) <= maxDim) {
                if (raw.config == Bitmap.Config.ARGB_8888) raw
                else raw.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                val scale = maxDim.toFloat() / max(w, h).toFloat()
                val nw = (w * scale).toInt().coerceAtLeast(16)
                val nh = (h * scale).toInt().coerceAtLeast(16)
                Bitmap.createScaledBitmap(raw, nw, nh, true).also {
                    if (it !== raw) raw.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadBitmapSync failed for $uriString: ${e.message}")
            null
        }
    }

    suspend fun extractFrameAtMs(
        context: Context,
        clip: VideoClipItem,
        clipRelativeMs: Long = 500L,
        maxDim: Int = 640
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (clip.mediaType == "image") {
            return@withContext loadBitmapSync(context, clip.uriString, maxDim)
        }
        val retriever = MediaMetadataRetriever()
        try {
            val uri = TemplateSeedCatalog.resolvePlayableUri(context, clip.uriString) ?: Uri.parse(clip.uriString)
            retriever.setDataSource(context, uri)
            val targetMs = (clip.trimStartMs + clipRelativeMs).coerceIn(0L, clip.durationMs.coerceAtLeast(1000L))
            val frame = retriever.getFrameAtTime(targetMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: return@withContext null
            val w = frame.width
            val h = frame.height
            if (max(w, h) <= maxDim) frame
            else {
                val scale = maxDim.toFloat() / max(w, h).toFloat()
                Bitmap.createScaledBitmap(
                    frame,
                    (w * scale).toInt().coerceAtLeast(16),
                    (h * scale).toInt().coerceAtLeast(16),
                    true
                ).also { if (it !== frame) frame.recycle() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "extractFrameAtMs failed: ${e.message}")
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    suspend fun extractClipThumbnails(
        context: Context,
        clip: VideoClipItem,
        count: Int = 5
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val safeCount = count.coerceIn(1, 8)
        if (clip.mediaType == "image") {
            val bmp = loadBitmapSync(context, clip.uriString, 160) ?: return@withContext emptyList()
            return@withContext List(safeCount) { bmp }
        }
        val retriever = MediaMetadataRetriever()
        val list = mutableListOf<Bitmap>()
        try {
            val uri = TemplateSeedCatalog.resolvePlayableUri(context, clip.uriString) ?: Uri.parse(clip.uriString)
            retriever.setDataSource(context, uri)
            val start = clip.trimStartMs.coerceAtLeast(0L)
            val end = clip.trimEndMs.coerceAtLeast(start + 500L)
            val span = (end - start).coerceAtLeast(500L)
            for (i in 0 until safeCount) {
                val tMs = start + (span * i) / safeCount.coerceAtLeast(1)
                val raw = retriever.getFrameAtTime(tMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (raw != null) {
                    val thumb = Bitmap.createScaledBitmap(raw, 96, 72, true)
                    if (thumb !== raw) raw.recycle()
                    list.add(thumb)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Thumb extraction skipped for ${clip.title}: ${e.message}")
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        list
    }

    suspend fun captureFreezeFrame(
        context: Context,
        clip: VideoClipItem,
        clipRelativeMs: Long = 500L
    ): Result<VideoClipItem> = withContext(Dispatchers.IO) {
        try {
            val frame = extractFrameAtMs(context, clip, clipRelativeMs, maxDim = 1080)
                ?: throw IllegalStateException("Não foi possível capturar o fotograma deste clipe.")
            val dir = File(context.filesDir, "freeze_frames").apply { mkdirs() }
            val outFile = File(dir, "freeze_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                frame.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            frame.recycle()
            Result.success(
                VideoClipItem(
                    clipId = "frz_${UUID.randomUUID().toString().take(8)}",
                    title = "${clip.title} (Congelado)",
                    uriString = Uri.fromFile(outFile).toString(),
                    mediaType = "image",
                    durationMs = 2500L,
                    trimStartMs = 0L,
                    trimEndMs = 2500L,
                    filter = clip.filter,
                    frozenFromClipId = clip.clipId,
                    brightness = clip.brightness,
                    contrast = clip.contrast,
                    saturation = clip.saturation,
                    exposure = clip.exposure,
                    temperature = clip.temperature,
                    vignette = clip.vignette,
                    grain = clip.grain,
                    hueShift = clip.hueShift,
                    rgbGamma = clip.rgbGamma
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Freeze frame failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a real reversed MP4 file by sampling frames in reverse order
     * and encoding an H.264 stream with MediaCodec + MediaMuxer.
     */
    suspend fun generateReversedVideoClip(
        context: Context,
        clip: VideoClipItem,
        onProgress: (Float) -> Unit = {}
    ): Result<VideoClipItem> = withContext(Dispatchers.IO) {
        if (clip.mediaType != "video") {
            return@withContext Result.failure(IllegalStateException("A reprodução reversa aplica-se apenas a clipes de vídeo."))
        }
        val retriever = MediaMetadataRetriever()
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        val dir = File(context.filesDir, "reversed_videos").apply { mkdirs() }
        val outFile = File(dir, "rev_${System.currentTimeMillis()}.mp4")
        try {
            val uri = TemplateSeedCatalog.resolvePlayableUri(context, clip.uriString) ?: Uri.parse(clip.uriString)
            retriever.setDataSource(context, uri)
            val startMs = clip.trimStartMs.coerceAtLeast(0L)
            val endMs = clip.trimEndMs.coerceAtLeast(startMs + 800L)
            val activeDur = (endMs - startMs).coerceAtMost(20_000L)

            val firstFrame = retriever.getFrameAtTime(startMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw IllegalStateException("Não foi possível ler os fotogramas do vídeo.")
            val rawW = firstFrame.width.coerceAtLeast(240)
            val rawH = firstFrame.height.coerceAtLeast(240)
            firstFrame.recycle()

            val scale = min(720f / max(rawW, rawH), 1f)
            val encW = (((rawW * scale).toInt() / 16) * 16).coerceAtLeast(256)
            val encH = (((rawH * scale).toInt() / 16) * 16).coerceAtLeast(256)

            val fps = 15
            val frameCount = ((activeDur / 1000f) * fps).toInt().coerceIn(8, 220)
            val frameDurationUs = 1_000_000L / fps

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, encW, encH).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_200_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) encoder.signalEndOfInputStream()
                while (true) {
                    val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10_000L)
                    if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break
                    } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (outIdx >= 0) {
                        val encodedData = encoder.getOutputBuffer(outIdx)
                        if (encodedData != null && bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    }
                }
            }

            for (i in 0 until frameCount) {
                // Reverse timestamp progression from endMs down to startMs
                val frac = i.toFloat() / (frameCount - 1).coerceAtLeast(1)
                val sampleMs = (endMs - (frac * activeDur)).toLong().coerceIn(startMs, endMs)
                val bmp = retriever.getFrameAtTime(sampleMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: retriever.getFrameAtTime(sampleMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bmp != null) {
                    val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        inputSurface.lockHardwareCanvas()
                    } else {
                        inputSurface.lockCanvas(null)
                    }
                    try {
                        canvas.drawColor(Color.BLACK)
                        val dstRect = Rect(0, 0, encW, encH)
                        canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    bmp.recycle()
                    drainEncoder(false)
                }
                onProgress((i + 1).toFloat() / frameCount.toFloat())
            }
            drainEncoder(true)
            inputSurface.release()
            try { muxer.stop() } catch (_: Exception) {}

            if (!outFile.exists() || outFile.length() < 1024L) {
                throw IllegalStateException("Não foi possível concluir a codificação reversa.")
            }
            val newDur = VideoAudioStudioEngine.extractAudioDurationMs(context, Uri.fromFile(outFile))
                .coerceAtLeast(1000L)
            Result.success(
                clip.copy(
                    title = if (clip.reversed) clip.title.removeSuffix(" (Reverso)") else "${clip.title} (Reverso)",
                    uriString = Uri.fromFile(outFile).toString(),
                    durationMs = newDur,
                    trimStartMs = 0L,
                    trimEndMs = newDur,
                    reversed = !clip.reversed
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Reverse video generation failed", e)
            if (outFile.exists()) outFile.delete()
            Result.failure(e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
            try { encoder?.stop() } catch (_: Exception) {}
            try { encoder?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    fun applyChromaKeyToBitmap(src: Bitmap, keyColorHex: String, tolerance: Float): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val keyColor = try { Color.parseColor(keyColorHex) } catch (_: Exception) { Color.GREEN }
        val kr = Color.red(keyColor)
        val kg = Color.green(keyColor)
        val kb = Color.blue(keyColor)
        val maxDist = (tolerance.coerceIn(0.08f, 0.9f) * 441.67f)
        val feather = maxDist * 0.25f

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val a = Color.alpha(c)
            val dr = (r - kr).toFloat()
            val dg = (g - kg).toFloat()
            val db = (b - kb).toFloat()
            val dist = sqrt(dr * dr + dg * dg + db * db)
            if (dist < maxDist) {
                val factor = ((dist - (maxDist - feather)) / feather.coerceAtLeast(1f)).coerceIn(0f, 1f)
                val newAlpha = (a * factor).toInt()
                pixels[i] = Color.argb(newAlpha, r, g, b)
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    fun applyMaskToBitmap(src: Bitmap, maskShape: VideoMaskShape): Bitmap {
        if (maskShape == VideoMaskShape.NONE) return src
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val path = buildMaskPath(w.toFloat(), h.toFloat(), maskShape)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(path, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    fun buildMaskPath(w: Float, h: Float, maskShape: VideoMaskShape): Path {
        val path = Path()
        val minD = min(w, h)
        when (maskShape) {
            VideoMaskShape.NONE -> path.addRect(0f, 0f, w, h, Path.Direction.CW)
            VideoMaskShape.CIRCLE -> path.addCircle(w / 2f, h / 2f, minD / 2f, Path.Direction.CW)
            VideoMaskShape.ROUNDED -> path.addRoundRect(RectF(0f, 0f, w, h), minD * 0.14f, minD * 0.14f, Path.Direction.CW)
            VideoMaskShape.SPLIT_H -> path.addRect(0f, h * 0.18f, w, h * 0.82f, Path.Direction.CW)
            VideoMaskShape.DIAMOND -> {
                path.moveTo(w / 2f, 0f)
                path.lineTo(w, h / 2f)
                path.lineTo(w / 2f, h)
                path.lineTo(0f, h / 2f)
                path.close()
            }
            VideoMaskShape.STAR -> {
                val cx = w / 2f
                val cy = h / 2f
                val outerR = minD * 0.48f
                val innerR = outerR * 0.44f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = (i * 36.0 - 90.0) * PI / 180.0
                    val px = cx + (r * cos(angle)).toFloat()
                    val py = cy + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            VideoMaskShape.HEART -> {
                val cx = w / 2f
                val top = h * 0.18f
                path.moveTo(cx, h * 0.88f)
                path.cubicTo(w * 0.05f, h * 0.62f, w * 0.06f, top, cx, h * 0.36f)
                path.cubicTo(w * 0.94f, top, w * 0.95f, h * 0.62f, cx, h * 0.88f)
                path.close()
            }
        }
        return path
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

object VideoAutosaveStore {

    private fun draftFile(context: Context, key: String): File {
        val safe = key.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "default_video" }
        val dir = File(context.filesDir, "video_drafts").apply { mkdirs() }
        return File(dir, "draft_$safe.json")
    }

    fun saveDraft(context: Context, key: String, title: String, snapshot: VideoEditorSnapshot) {
        try {
            val root = JSONObject().apply {
                put("savedAtMs", System.currentTimeMillis())
                put("title", title)
                put("aspectRatio", snapshot.aspectRatio)
                put("bgColorStart", snapshot.bgColorStart)
                put("bgColorEnd", snapshot.bgColorEnd ?: JSONObject.NULL)
                put("bgImageUri", snapshot.bgImageUri ?: JSONObject.NULL)
                put("animatedBgStyle", snapshot.animatedBgStyle.name)
                put("clips", JSONArray().apply {
                    snapshot.clips.forEach { clip -> put(mapToJsonObject(clip.toMap())) }
                })
                put("audioTracks", JSONArray().apply {
                    snapshot.audioTracks.forEach { trk -> put(mapToJsonObject(trk.toLayerMap())) }
                })
                put("overlays", JSONArray().apply {
                    snapshot.overlays.forEach { ov -> put(mapToJsonObject(ov.toLayerMap())) }
                })
            }
            draftFile(context, key).writeText(root.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Autosave write failed: ${e.message}")
        }
    }

    data class RecoveredDraft(
        val savedAtMs: Long,
        val title: String,
        val snapshot: VideoEditorSnapshot
    )

    fun loadDraft(context: Context, key: String): RecoveredDraft? {
        val f = draftFile(context, key)
        if (!f.exists() || f.length() < 16L) return null
        return try {
            val root = JSONObject(f.readText())
            val clips = mutableListOf<VideoClipItem>()
            val cArr = root.optJSONArray("clips") ?: JSONArray()
            for (i in 0 until cArr.length()) {
                clips.add(VideoClipItem.fromMap(jsonObjectToMap(cArr.getJSONObject(i))))
            }
            val audios = mutableListOf<ProAudioTrack>()
            val aArr = root.optJSONArray("audioTracks") ?: JSONArray()
            for (i in 0 until aArr.length()) {
                audios.add(ProAudioTrack.fromLayerMap(jsonObjectToMap(aArr.getJSONObject(i))))
            }
            val overlays = mutableListOf<ProOverlayItem>()
            val oArr = root.optJSONArray("overlays") ?: JSONArray()
            for (i in 0 until oArr.length()) {
                overlays.add(ProOverlayItem.fromLayerMap(jsonObjectToMap(oArr.getJSONObject(i))))
            }
            val bgStyle = enumValues<VideoBackgroundAnimatedStyle>()
                .firstOrNull { it.name == root.optString("animatedBgStyle") }
                ?: VideoBackgroundAnimatedStyle.NONE
            RecoveredDraft(
                savedAtMs = root.optLong("savedAtMs", 0L),
                title = root.optString("title", "Projeto Recuperado"),
                snapshot = VideoEditorSnapshot(
                    clips = clips,
                    audioTracks = audios,
                    overlays = overlays,
                    aspectRatio = root.optString("aspectRatio", "9:16"),
                    bgColorStart = root.optString("bgColorStart", "#0A1628"),
                    bgColorEnd = root.optString("bgColorEnd").takeIf { it.isNotBlank() && it != "null" },
                    bgImageUri = root.optString("bgImageUri").takeIf { it.isNotBlank() && it != "null" },
                    animatedBgStyle = bgStyle
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Draft parse failed: ${e.message}")
            null
        }
    }

    fun clearDraft(context: Context, key: String) {
        try {
            draftFile(context, key).delete()
        } catch (_: Exception) {
        }
    }

    private fun mapToJsonObject(map: Map<String, Any?>): JSONObject {
        val obj = JSONObject()
        for ((k, v) in map) {
            when (v) {
                null -> obj.put(k, JSONObject.NULL)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    obj.put(k, mapToJsonObject(v as Map<String, Any?>))
                }
                is List<*> -> obj.put(k, listToJsonArray(v))
                else -> obj.put(k, v)
            }
        }
        return obj
    }

    private fun listToJsonArray(list: List<*>): JSONArray {
        val arr = JSONArray()
        for (item in list) {
            when (item) {
                null -> arr.put(JSONObject.NULL)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    arr.put(mapToJsonObject(item as Map<String, Any?>))
                }
                is List<*> -> arr.put(listToJsonArray(item))
                else -> arr.put(item)
            }
        }
        return arr
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = obj.get(k)
            map[k] = when (v) {
                JSONObject.NULL -> null
                is JSONObject -> jsonObjectToMap(v)
                is JSONArray -> jsonArrayToList(v)
                else -> v
            }
        }
        return map
    }

    private fun jsonArrayToList(arr: JSONArray): List<Any?> {
        val list = mutableListOf<Any?>()
        for (i in 0 until arr.length()) {
            val v = arr.get(i)
            list.add(
                when (v) {
                    JSONObject.NULL -> null
                    is JSONObject -> jsonObjectToMap(v)
                    is JSONArray -> jsonArrayToList(v)
                    else -> v
                }
            )
        }
        return list
    }
}
