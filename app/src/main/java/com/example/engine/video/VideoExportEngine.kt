package com.example.engine.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.OptIn
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoFilterKind
import com.example.core.model.VideoTextOverlay
import com.example.engine.graphics.BitmapRenderer
import com.example.engine.graphics.ExportedFileResult
import com.google.common.collect.ImmutableList
import java.io.File

enum class VideoResolutionPreset(val label: String, val targetHeight: Int) {
    HD_720("720p HD", 720),
    FHD_1080("1080p Full HD", 1080),
    UHD_4K("4K Ultra HD", 2160)
}

class VideoExportEngine(private val context: Context) {

    private var activeTransformer: Transformer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    @OptIn(UnstableApi::class)
    fun exportMp4(
        projectTitle: String,
        clips: List<VideoClipItem>,
        audioUri: String?,
        audioMuted: Boolean,
        textOverlays: List<VideoTextOverlay>,
        resolution: VideoResolutionPreset,
        aspectRatio: String,
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

        try {
            val editedItems = clips.map { clip ->
                val uri = Uri.parse(clip.uriString)
                val itemBuilder = MediaItem.Builder().setUri(uri)
                if (clip.mediaType == "image") {
                    itemBuilder.setImageDurationMs(
                        (clip.trimEndMs - clip.trimStartMs).coerceAtLeast(1500L)
                    )
                } else {
                    if (clip.trimEndMs > clip.trimStartMs) {
                        itemBuilder.setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(clip.trimStartMs.coerceAtLeast(0L))
                                .setEndPositionMs(clip.trimEndMs)
                                .build()
                        )
                    }
                }
                val mediaItem = itemBuilder.build()

                val videoEffects = mutableListOf<Effect>()
                when (clip.filter) {
                    VideoFilterKind.BW -> videoEffects.add(RgbFilter.createGrayscaleFilter())
                    VideoFilterKind.CINEMA -> videoEffects.add(Contrast(0.28f))
                    VideoFilterKind.WARM -> videoEffects.add(Contrast(0.12f))
                    VideoFilterKind.COOL -> videoEffects.add(Contrast(0.15f))
                    VideoFilterKind.NORMAL -> Unit
                }

                val audioProcessors = mutableListOf<androidx.media3.common.audio.AudioProcessor>()
                if (clip.speed != 1.0f) {
                    val sonic = SonicAudioProcessor().apply {
                        setSpeed(clip.speed.coerceIn(0.5f, 2.0f))
                    }
                    audioProcessors.add(sonic)
                }

                val itemBuilderEdited = EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(audioProcessors, videoEffects))
                    .setRemoveAudio(clip.muted || audioMuted || !audioUri.isNullOrBlank())

                if (clip.mediaType == "image") {
                    itemBuilderEdited.setFrameRate(30)
                }
                itemBuilderEdited.build()
            }

            val videoSequence = if (clips.any { it.mediaType == "video" }) {
                EditedMediaItemSequence.withAudioAndVideoFrom(editedItems)
            } else {
                EditedMediaItemSequence.withVideoFrom(editedItems)
            }

            val sequences = mutableListOf(videoSequence)
            if (!audioUri.isNullOrBlank() && !audioMuted) {
                val bgAudioItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(audioUri))).build()
                val bgAudioSeq = EditedMediaItemSequence.withAudioFrom(listOf(bgAudioItem))
                    .buildUpon()
                    .setIsLooping(true)
                    .build()
                sequences.add(bgAudioSeq)
            }

            val globalVideoEffects = mutableListOf<Effect>()
            val (w, h) = when (aspectRatio) {
                "9:16" -> (resolution.targetHeight * 9 / 16) to resolution.targetHeight
                "1:1" -> resolution.targetHeight to resolution.targetHeight
                "4:5" -> (resolution.targetHeight * 4 / 5) to resolution.targetHeight
                else -> (resolution.targetHeight * 16 / 9) to resolution.targetHeight
            }
            globalVideoEffects.add(
                Presentation.createForWidthAndHeight(
                    w - (w % 2),
                    h - (h % 2),
                    Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                )
            )

            if (textOverlays.isNotEmpty()) {
                val overlays = ImmutableList.builder<TextureOverlay>()
                textOverlays.take(6).forEach { txt ->
                    val spannable = SpannableString(txt.text)
                    val color = BitmapRenderer.parseColorSafe(txt.colorHex, Color.WHITE)
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        0,
                        spannable.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    val yOffset = (0.5f - txt.yPositionFraction) * 1.6f
                    val settings = StaticOverlaySettings.Builder()
                        .setOverlayFrameAnchor(0f, yOffset.coerceIn(-0.85f, 0.85f))
                        .build()
                    overlays.add(TextOverlay.createStaticTextOverlay(spannable, settings))
                }
                globalVideoEffects.add(OverlayEffect(overlays.build()))
            }

            val composition = Composition.Builder(sequences)
                .setEffects(Effects(emptyList(), globalVideoEffects))
                .build()

            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        stopProgressPolling()
                        activeTransformer = null
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
                        if (outFile.exists()) outFile.delete()
                        onError(exportException.localizedMessage ?: "Erro ao processar vídeo MP4.")
                    }
                })
                .build()

            activeTransformer = transformer
            transformer.start(composition, outFile.absolutePath)
            startProgressPolling(transformer, onProgress)
        } catch (e: Exception) {
            stopProgressPolling()
            activeTransformer = null
            if (outFile.exists()) outFile.delete()
            onError(e.localizedMessage ?: "Não foi possível iniciar a exportação de vídeo.")
        }
    }

    @OptIn(UnstableApi::class)
    fun cancelExport() {
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
                    onProgress(holder.progress.coerceIn(0, 99))
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
