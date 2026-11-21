package com.example.engine.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.core.model.ProAudioTrack
import com.example.core.model.TemplateSeedCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val TAG = "VideoAudioStudio"

data class BuiltInAudioPreset(
    val id: String,
    val title: String,
    val category: String, // "music" or "sfx"
    val durationLabel: String
)

object VideoAudioLibraryCatalog {
    val musicPresets = listOf(
        BuiltInAudioPreset("kizomba_groove", "Kizomba Groove", "music", "0:08"),
        BuiltInAudioPreset("afrobeats_luanda", "Afrobeats Luanda", "music", "0:08"),
        BuiltInAudioPreset("lofi_studio", "Lo-Fi Estúdio", "music", "0:08"),
        BuiltInAudioPreset("epic_cinema", "Épico Cinema", "music", "0:08"),
        BuiltInAudioPreset("pop_comercial", "Pop Comercial", "music", "0:08"),
        BuiltInAudioPreset("news_flash", "Notícias Flash", "music", "0:06")
    )

    val sfxPresets = listOf(
        BuiltInAudioPreset("sfx_whoosh", "Whoosh Transição", "sfx", "0:01"),
        BuiltInAudioPreset("sfx_pop", "Pop Bolha", "sfx", "0:01"),
        BuiltInAudioPreset("sfx_glitch", "Glitch Digital", "sfx", "0:01"),
        BuiltInAudioPreset("sfx_flash", "Flash Câmara", "sfx", "0:01"),
        BuiltInAudioPreset("sfx_bell", "Sino Sucesso", "sfx", "0:02"),
        BuiltInAudioPreset("sfx_impact", "Impacto Grave", "sfx", "0:02")
    )
}

object VideoAudioStudioEngine {

    private const val SAMPLE_RATE = 44100

    suspend fun ensurePresetWavTrack(
        context: Context,
        preset: BuiltInAudioPreset,
        startMs: Long = 0L
    ): Result<ProAudioTrack> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "audio_studio").apply { mkdirs() }
            val wavFile = File(dir, "preset_${preset.id}.wav")
            if (!wavFile.exists() || wavFile.length() < 512L) {
                val samples = synthesizePresetSamples(preset.id)
                writeMono16Wav(wavFile, samples, SAMPLE_RATE)
            }
            val uriString = Uri.fromFile(wavFile).toString()
            val dur = extractAudioDurationMs(context, Uri.fromFile(wavFile)).coerceAtLeast(800L)
            val peaks = extractWaveformPeaks(context, uriString, 48)
            Result.success(
                ProAudioTrack(
                    id = "aud_${UUID.randomUUID().toString().take(8)}",
                    title = preset.title,
                    uriString = uriString,
                    sourceType = if (preset.category == "sfx") "sfx" else "library",
                    startMs = startMs.coerceAtLeast(0L),
                    trimStartMs = 0L,
                    trimEndMs = dur,
                    durationMs = dur,
                    volume = if (preset.category == "sfx") 0.9f else 0.75f,
                    waveformPeaks = peaks
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating built-in audio", e)
            Result.failure(e)
        }
    }

    suspend fun importAudioFromDevice(
        context: Context,
        sourceUri: Uri,
        startMs: Long = 0L
    ): Result<ProAudioTrack> = withContext(Dispatchers.IO) {
        try {
            val displayName = queryDisplayName(context, sourceUri)
                ?: sourceUri.lastPathSegment?.substringAfterLast('/')
                ?: "Música Importada"
            val ext = displayName.substringAfterLast('.', "m4a").lowercase()
                .takeIf { it in setOf("mp3", "wav", "aac", "m4a", "ogg", "flac") } ?: "m4a"
            val dir = File(context.filesDir, "audio_imports").apply { mkdirs() }
            val destFile = File(dir, "imp_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}.$ext")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Não foi possível ler o ficheiro de áudio selecionado.")

            if (destFile.length() == 0L) {
                destFile.delete()
                throw IllegalStateException("O ficheiro de áudio selecionado está vazio.")
            }

            val localUri = Uri.fromFile(destFile)
            val dur = extractAudioDurationMs(context, localUri).coerceAtLeast(1000L)
            val peaks = extractWaveformPeaks(context, localUri.toString(), 48)
            Result.success(
                ProAudioTrack(
                    id = "aud_${UUID.randomUUID().toString().take(8)}",
                    title = displayName.substringBeforeLast('.').ifBlank { "Áudio" },
                    uriString = localUri.toString(),
                    sourceType = "device",
                    startMs = startMs.coerceAtLeast(0L),
                    trimStartMs = 0L,
                    trimEndMs = dur,
                    durationMs = dur,
                    volume = 0.85f,
                    waveformPeaks = peaks
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Audio import failed", e)
            Result.failure(e)
        }
    }

    suspend fun extractAudioFromVideo(
        context: Context,
        videoUriString: String,
        clipTitle: String,
        startMs: Long = 0L
    ): Result<ProAudioTrack> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        val dir = File(context.filesDir, "audio_extracted").apply { mkdirs() }
        val outFile = File(dir, "ext_${System.currentTimeMillis()}.m4a")
        try {
            val uri = TemplateSeedCatalog.resolvePlayableUri(context, videoUriString)
                ?: Uri.parse(videoUriString)
            extractor.setDataSource(context, uri, null)
            var audioTrackIdx = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    audioFormat = fmt
                    break
                }
            }
            if (audioTrackIdx == -1 || audioFormat == null) {
                return@withContext Result.failure(IllegalStateException("Este clipe não contém faixa de áudio."))
            }

            extractor.selectTrack(audioTrackIdx)
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxTrackIdx = muxer.addTrack(audioFormat)
            muxer.start()

            val bufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime.coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxTrackIdx, buffer, bufferInfo)
                extractor.advance()
            }
            muxer.stop()
            muxer.release()
            muxer = null

            val localUri = Uri.fromFile(outFile)
            val dur = extractAudioDurationMs(context, localUri).coerceAtLeast(1000L)
            val peaks = extractWaveformPeaks(context, localUri.toString(), 48)
            Result.success(
                ProAudioTrack(
                    id = "aud_${UUID.randomUUID().toString().take(8)}",
                    title = "Áudio: ${clipTitle.take(20)}",
                    uriString = localUri.toString(),
                    sourceType = "extracted",
                    startMs = startMs.coerceAtLeast(0L),
                    trimStartMs = 0L,
                    trimEndMs = dur,
                    durationMs = dur,
                    volume = 1.0f,
                    waveformPeaks = peaks
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Audio extraction failed", e)
            if (outFile.exists()) outFile.delete()
            Result.failure(e)
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    suspend fun synthesizeTextToAudioTrack(
        context: Context,
        text: String,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        startMs: Long = 0L
    ): Result<ProAudioTrack> = withContext(Dispatchers.Main) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Introduza o texto para converter em voz."))
        }
        val dir = File(context.filesDir, "audio_tts").apply { mkdirs() }
        val outFile = File(dir, "tts_${System.currentTimeMillis()}.wav")
        var tts: TextToSpeech? = null
        try {
            val initDeferred = CompletableDeferred<Int>()
            tts = TextToSpeech(context.applicationContext) { status ->
                if (!initDeferred.isCompleted) initDeferred.complete(status)
            }
            val status = withTimeout(10_000L) { initDeferred.await() }
            if (status != TextToSpeech.SUCCESS) {
                return@withContext Result.failure(IllegalStateException("Motor de voz (TTS) indisponível neste dispositivo."))
            }

            val ptLocale = Locale("pt", "PT")
            val brLocale = Locale("pt", "BR")
            val langResult = tts.setLanguage(ptLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                val brRes = tts.setLanguage(brLocale)
                if (brRes == TextToSpeech.LANG_MISSING_DATA || brRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault())
                }
            }
            tts.setSpeechRate(speechRate.coerceIn(0.6f, 1.8f))
            tts.setPitch(pitch.coerceIn(0.6f, 1.6f))

            val synthDeferred = CompletableDeferred<Boolean>()
            val utteranceId = "canva_ao_tts_${UUID.randomUUID().toString().take(8)}"
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && !synthDeferred.isCompleted) synthDeferred.complete(true)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId && !synthDeferred.isCompleted) synthDeferred.complete(false)
                }

                override fun onError(id: String?, errorCode: Int) {
                    if (id == utteranceId && !synthDeferred.isCompleted) synthDeferred.complete(false)
                }
            })

            val params = Bundle()
            val queued = tts.synthesizeToFile(cleanText, params, outFile, utteranceId)
            if (queued != TextToSpeech.SUCCESS) {
                return@withContext Result.failure(IllegalStateException("Falha ao gerar voz a partir do texto."))
            }
            val ok = withTimeout(30_000L) { synthDeferred.await() }
            if (!ok || !outFile.exists() || outFile.length() == 0L) {
                return@withContext Result.failure(IllegalStateException("Não foi possível gravar o ficheiro de voz TTS."))
            }

            withContext(Dispatchers.IO) {
                val uri = Uri.fromFile(outFile)
                val dur = extractAudioDurationMs(context, uri).coerceAtLeast(1000L)
                val peaks = extractWaveformPeaks(context, uri.toString(), 48)
                Result.success(
                    ProAudioTrack(
                        id = "aud_${UUID.randomUUID().toString().take(8)}",
                        title = "Voz TTS: ${cleanText.take(18)}",
                        uriString = uri.toString(),
                        sourceType = "tts",
                        startMs = startMs.coerceAtLeast(0L),
                        trimStartMs = 0L,
                        trimEndMs = dur,
                        durationMs = dur,
                        volume = 1.0f,
                        waveformPeaks = peaks
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "TTS generation failed", e)
            if (outFile.exists()) outFile.delete()
            Result.failure(e)
        } finally {
            try { tts?.shutdown() } catch (_: Exception) {}
        }
    }

    suspend fun extractWaveformPeaks(
        context: Context,
        uriString: String,
        sampleCount: Int = 48
    ): List<Float> = withContext(Dispatchers.IO) {
        try {
            val uri = TemplateSeedCatalog.resolvePlayableUri(context, uriString) ?: Uri.parse(uriString)
            if (uri.scheme == "file" && uri.path?.endsWith(".wav", ignoreCase = true) == true) {
                val file = File(uri.path!!)
                if (file.exists() && file.length() > 44L) {
                    return@withContext readWavPeaks(file, sampleCount)
                }
            }
            val pcm = decodeToMonoPcm16(context, uri, maxDurationMs = 20_000L, targetRate = 11025)
            if (pcm.isNotEmpty()) {
                return@withContext downsampleToPeaks(pcm, sampleCount)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Waveform decode fallback: ${e.message}")
        }
        List(sampleCount) { i -> 0.2f + 0.55f * abs(sin(i * 0.45f)) }
    }

    /**
     * Mixes multiple active ProAudioTracks (with trim, startMs offset, volume, fadeIn, fadeOut,
     * noise reduction, and vocal isolation DSP) into a single PCM WAV file for reliable Media3 export.
     */
    suspend fun renderMixedAudioToWav(
        context: Context,
        tracks: List<ProAudioTrack>,
        totalDurationMs: Long
    ): File? = withContext(Dispatchers.IO) {
        val activeTracks = tracks.filter { !it.muted && it.volume > 0.01f && it.uriString.isNotBlank() }
        if (activeTracks.isEmpty() || totalDurationMs <= 0L) return@withContext null

        val outRate = 44100
        val totalSamples = ((totalDurationMs.coerceAtMost(300_000L) / 1000.0) * outRate).toInt().coerceAtLeast(outRate)
        val mixBuffer = FloatArray(totalSamples)

        for (track in activeTracks) {
            try {
                val uri = TemplateSeedCatalog.resolvePlayableUri(context, track.uriString) ?: Uri.parse(track.uriString)
                val decoded = decodeToMonoPcm16(context, uri, maxDurationMs = 240_000L, targetRate = outRate)
                if (decoded.isEmpty()) continue

                val trimStartSamples = ((track.trimStartMs.coerceAtLeast(0L) / 1000.0) * outRate).toInt()
                    .coerceIn(0, (decoded.size - 1).coerceAtLeast(0))
                val trimEndSamples = ((track.trimEndMs.coerceAtLeast(track.trimStartMs + 200L) / 1000.0) * outRate).toInt()
                    .coerceIn(trimStartSamples + 1, decoded.size)
                val startOffsetSamples = ((track.startMs.coerceAtLeast(0L) / 1000.0) * outRate).toInt()
                val activeLen = (trimEndSamples - trimStartSamples).coerceAtLeast(1)
                val fadeInSamples = ((track.fadeInMs / 1000.0) * outRate).toInt().coerceIn(0, activeLen)
                val fadeOutSamples = ((track.fadeOutMs / 1000.0) * outRate).toInt().coerceIn(0, activeLen)

                var prevSample = 0f
                var prevFiltered = 0f
                val hpAlpha = 0.94f // High-pass ~250Hz for vocal isolation / rumble removal

                for (i in 0 until activeLen) {
                    val outIdx = startOffsetSamples + i
                    if (outIdx >= totalSamples) break
                    var s = decoded[trimStartSamples + i] / 32768f

                    if (track.vocalIsolation) {
                        // Simple 1-pole high-pass + mid presence emphasis
                        val hp = hpAlpha * (prevFiltered + s - prevSample)
                        prevSample = s
                        prevFiltered = hp
                        s = (hp * 1.25f).coerceIn(-1f, 1f)
                    }
                    if (track.noiseReduction) {
                        // Adaptive soft noise gate (-34dB threshold)
                        val mag = abs(s)
                        if (mag < 0.022f) {
                            s *= (mag / 0.022f) * 0.25f
                        }
                    }

                    var env = track.volume
                    if (fadeInSamples > 0 && i < fadeInSamples) {
                        env *= (i.toFloat() / fadeInSamples.toFloat())
                    }
                    val rem = activeLen - i
                    if (fadeOutSamples > 0 && rem < fadeOutSamples) {
                        env *= (rem.toFloat() / fadeOutSamples.toFloat())
                    }

                    mixBuffer[outIdx] += s * env
                }
            } catch (e: Exception) {
                Log.w(TAG, "Skipping track '${track.title}' in mix: ${e.message}")
            }
        }

        val outPcm = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val v = mixBuffer[i]
            // Soft tanh-like limiter to avoid harsh digital clipping
            val limited = if (abs(v) > 0.95f) kotlin.math.tanh(v.toDouble()).toFloat() else v
            outPcm[i] = (limited.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }

        val dir = File(context.cacheDir, "audio_mix").apply { mkdirs() }
        val outFile = File(dir, "mix_${System.currentTimeMillis()}.wav")
        writeMono16Wav(outFile, outPcm, outRate)
        outFile
    }

    fun extractAudioDurationMs(context: Context, uri: Uri): Long {
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

    private fun decodeToMonoPcm16(
        context: Context,
        uri: Uri,
        maxDurationMs: Long,
        targetRate: Int
    ): ShortArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/")) {
                    trackIndex = i
                    format = fmt
                    break
                }
            }
            if (trackIndex == -1 || format == null) return ShortArray(0)
            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val srcRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else targetRate
            var srcChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val outStream = ByteArrayOutputStream()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            val maxUs = maxDurationMs * 1000L

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5000L)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        val pts = extractor.sampleTime
                        if (sampleSize < 0 || pts > maxUs) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 5000L)
                if (outIdx >= 0) {
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                    if (info.size > 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)!!
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        outBuf.get(chunk)
                        outStream.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outFmt = codec.outputFormat
                    if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        srcChannels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }

            val bytes = outStream.toByteArray()
            val totalShorts = bytes.size / 2
            val sb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val monoCount = totalShorts / srcChannels.coerceAtLeast(1)
            val mono = ShortArray(monoCount)
            for (i in 0 until monoCount) {
                var sum = 0
                for (c in 0 until srcChannels.coerceAtLeast(1)) {
                    sum += sb.get(i * srcChannels + c).toInt()
                }
                mono[i] = (sum / srcChannels.coerceAtLeast(1)).toShort()
            }

            if (srcRate == targetRate || mono.isEmpty()) return mono
            val resampledLen = ((mono.size.toLong() * targetRate) / srcRate.coerceAtLeast(8000)).toInt().coerceAtLeast(1)
            return ShortArray(resampledLen) { idx ->
                val srcIdx = ((idx.toLong() * srcRate) / targetRate).toInt().coerceIn(0, mono.lastIndex)
                mono[srcIdx]
            }
        } catch (e: Exception) {
            Log.w(TAG, "decodeToMonoPcm16 failed: ${e.message}")
            return ShortArray(0)
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    private fun readWavPeaks(file: File, sampleCount: Int): List<Float> {
        val totalDataBytes = (file.length() - 44L).coerceAtLeast(2L)
        val totalSamples = (totalDataBytes / 2L).toInt().coerceAtLeast(1)
        val step = (totalSamples / sampleCount).coerceAtLeast(1)
        val peaks = MutableList(sampleCount) { 0.15f }
        RandomAccessFile(file, "r").use { raf ->
            val buf = ByteArray(2)
            for (i in 0 until sampleCount) {
                var maxAbs = 0
                val baseSample = i * step
                val probes = min(16, step)
                for (p in 0 until probes) {
                    val samplePos = baseSample + (p * step / probes.coerceAtLeast(1))
                    val offset = 44L + samplePos.toLong() * 2L
                    if (offset + 2L <= file.length()) {
                        raf.seek(offset)
                        raf.readFully(buf)
                        val s = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        maxAbs = max(maxAbs, abs(s))
                    }
                }
                peaks[i] = (maxAbs / 32767f).coerceIn(0.1f, 1.0f)
            }
        }
        return peaks
    }

    private fun downsampleToPeaks(pcm: ShortArray, sampleCount: Int): List<Float> {
        if (pcm.isEmpty()) return List(sampleCount) { 0.2f }
        val chunkSize = (pcm.size / sampleCount).coerceAtLeast(1)
        return List(sampleCount) { i ->
            val start = (i * chunkSize).coerceAtMost(pcm.lastIndex)
            val end = ((i + 1) * chunkSize).coerceAtMost(pcm.size)
            var peak = 0
            for (j in start until end) {
                peak = max(peak, abs(pcm[j].toInt()))
            }
            (peak / 32767f).coerceIn(0.1f, 1.0f)
        }
    }

    private fun writeMono16Wav(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataSize = pcm.size * 2
        val byteRate = sampleRate * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(sampleRate)
            putInt(byteRate)
            putShort(2) // Block align
            putShort(16) // Bits per sample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }
        FileOutputStream(file).use { fos ->
            fos.write(header.array())
            val dataBuf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            dataBuf.asShortBuffer().put(pcm)
            fos.write(dataBuf.array())
        }
    }

    private fun synthesizePresetSamples(id: String): ShortArray {
        val rng = Random(id.hashCode())
        return when (id) {
            "sfx_whoosh" -> {
                val n = (SAMPLE_RATE * 1.0).toInt()
                ShortArray(n) { i ->
                    val t = i.toFloat() / n
                    val env = sin(t * PI).toFloat()
                    val sweep = sin(2.0 * PI * (180 + 900 * t) * i / SAMPLE_RATE).toFloat()
                    val noise = (rng.nextFloat() * 2f - 1f) * 0.55f
                    ((sweep * 0.3f + noise * 0.7f) * env * 22000f).toInt().toShort()
                }
            }
            "sfx_pop" -> {
                val n = (SAMPLE_RATE * 0.35).toInt()
                ShortArray(n) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val env = exp(-t * 14f)
                    val f = 720f * exp(-t * 8f) + 140f
                    (sin(2.0 * PI * f * t) * env * 26000.0).toInt().toShort()
                }
            }
            "sfx_glitch" -> {
                val n = (SAMPLE_RATE * 0.55).toInt()
                ShortArray(n) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val gate = if ((i / 600) % 2 == 0) 1f else 0.25f
                    val f = if ((i / 1100) % 2 == 0) 880f else 310f
                    val s = sin(2.0 * PI * f * t + sin(2.0 * PI * 90f * t) * 4.0)
                    (s * gate * (1f - t) * 21000.0).toInt().toShort()
                }
            }
            "sfx_flash" -> {
                val n = (SAMPLE_RATE * 0.65).toInt()
                ShortArray(n) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val click = if (t < 0.04f) (rng.nextFloat() * 2f - 1f) * 0.8f else 0f
                    val chime = (sin(2.0 * PI * 1480.0 * t) + 0.5 * sin(2.0 * PI * 2220.0 * t)) * exp(-t * 7.0)
                    ((click + chime * 0.6).coerceIn(-1.0, 1.0) * 22000.0).toInt().toShort()
                }
            }
            "sfx_bell" -> {
                val n = (SAMPLE_RATE * 1.4).toInt()
                ShortArray(n) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val env = exp(-t * 2.8)
                    val s = sin(2.0 * PI * 880.0 * t) * 0.6 + sin(2.0 * PI * 1320.0 * t) * 0.35 + sin(2.0 * PI * 1760.0 * t) * 0.2
                    (s * env * 20000.0).toInt().toShort()
                }
            }
            "sfx_impact" -> {
                val n = (SAMPLE_RATE * 1.2).toInt()
                ShortArray(n) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val env = exp(-t * 3.5)
                    val freq = 140.0 * exp(-t * 4.0) + 48.0
                    val sub = sin(2.0 * PI * freq * t)
                    val punch = if (t < 0.08f) (rng.nextFloat() * 2f - 1f) * 0.4 else 0.0
                    ((sub * 0.85 + punch) * env * 27000.0).toInt().toShort()
                }
            }
            else -> synthesizeMusicalLoop(id, rng)
        }
    }

    private fun synthesizeMusicalLoop(id: String, rng: Random): ShortArray {
        val durSec = if (id == "news_flash") 6 else 8
        val n = SAMPLE_RATE * durSec
        val bpm = when (id) {
            "kizomba_groove" -> 96.0
            "afrobeats_luanda" -> 112.0
            "lofi_studio" -> 78.0
            "epic_cinema" -> 90.0
            "pop_comercial" -> 120.0
            else -> 124.0
        }
        val beatDur = 60.0 / bpm
        val chords = when (id) {
            "kizomba_groove" -> listOf(
                listOf(220.0, 261.63, 329.63), // Am
                listOf(174.61, 220.0, 261.63), // F
                listOf(261.63, 329.63, 392.0), // C
                listOf(196.0, 246.94, 293.66)  // G
            )
            "afrobeats_luanda" -> listOf(
                listOf(293.66, 349.23, 440.0), // Dm
                listOf(233.08, 293.66, 349.23), // Bb
                listOf(261.63, 329.63, 392.0), // C
                listOf(220.0, 261.63, 329.63)  // Am
            )
            "epic_cinema" -> listOf(
                listOf(130.81, 196.0, 261.63),
                listOf(110.0, 164.81, 220.0),
                listOf(146.83, 220.0, 293.66),
                listOf(123.47, 196.0, 246.94)
            )
            else -> listOf(
                listOf(261.63, 329.63, 392.0),
                listOf(220.0, 261.63, 329.63),
                listOf(174.61, 220.0, 261.63),
                listOf(196.0, 246.94, 293.66)
            )
        }
        return ShortArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val beatIdx = (t / beatDur).toInt()
            val beatPos = (t % beatDur) / beatDur
            val chord = chords[(beatIdx / 2) % chords.size]

            // Warm chord pad / keys
            var pad = 0.0
            val keyEnv = exp(-beatPos * (if (id == "epic_cinema") 1.2 else 2.6))
            for (f in chord) {
                pad += sin(2.0 * PI * f * t) * 0.22
                if (id != "lofi_studio") pad += sin(2.0 * PI * f * 2.0 * t) * 0.06
            }
            pad *= keyEnv

            // Bassline on sub octave
            val bassF = chord.first() / 2.0
            val bass = sin(2.0 * PI * bassF * t) * exp(-beatPos * 3.0) * 0.42

            // Kick drum on beat start
            val kick = if (beatPos < 0.14) {
                val kt = beatPos * beatDur
                sin(2.0 * PI * (120.0 * exp(-kt * 24.0) + 46.0) * kt) * exp(-kt * 18.0) * 0.55
            } else 0.0

            // Subtle hi-hat / shaker on offbeat
            val hat = if (beatPos in 0.48..0.54) {
                (rng.nextDouble() * 2.0 - 1.0) * 0.14
            } else 0.0

            val mixed = (pad + bass + kick + hat).coerceIn(-0.95, 0.95)
            (mixed * 21000.0).toInt().toShort()
        }
    }
}

/**
 * Real-time microphone voice recorder writing AAC M4A audio into app filesDir
 * with live normalized amplitude polling for waveform bars.
 */
class VoiceRecorderController(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startedAtMs: Long = 0L

    fun startRecording(): Result<File> {
        return try {
            stopAndDiscard()
            val dir = File(context.filesDir, "voiceovers").apply { mkdirs() }
            val outFile = File(dir, "voz_${System.currentTimeMillis()}.m4a")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            currentFile = outFile
            startedAtMs = System.currentTimeMillis()
            Result.success(outFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start mic recording", e)
            stopAndDiscard()
            Result.failure(e)
        }
    }

    fun readAmplitudeNormalized(): Float {
        return try {
            val amp = recorder?.maxAmplitude ?: 0
            (sqrt(amp / 32767f)).coerceIn(0.05f, 1.0f)
        } catch (_: Exception) {
            0.05f
        }
    }

    suspend fun stopRecordingAndCreateTrack(startMsOnTimeline: Long): Result<ProAudioTrack> =
        withContext(Dispatchers.IO) {
            val file = currentFile ?: return@withContext Result.failure(IllegalStateException("Nenhuma gravação ativa."))
            try {
                try {
                    recorder?.stop()
                } catch (_: Exception) {
                }
                recorder?.release()
                recorder = null
                currentFile = null
                if (!file.exists() || file.length() < 128L) {
                    return@withContext Result.failure(IllegalStateException("Gravação muito curta ou vazia."))
                }
                val uri = Uri.fromFile(file)
                val dur = VideoAudioStudioEngine.extractAudioDurationMs(context, uri)
                    .coerceAtLeast(System.currentTimeMillis() - startedAtMs)
                    .coerceAtLeast(1000L)
                val peaks = VideoAudioStudioEngine.extractWaveformPeaks(context, uri.toString(), 48)
                Result.success(
                    ProAudioTrack(
                        id = "aud_${UUID.randomUUID().toString().take(8)}",
                        title = "Locução Voz",
                        uriString = uri.toString(),
                        sourceType = "voiceover",
                        startMs = startMsOnTimeline.coerceAtLeast(0L),
                        trimStartMs = 0L,
                        trimEndMs = dur,
                        durationMs = dur,
                        volume = 1.0f,
                        waveformPeaks = peaks
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun stopAndDiscard() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        currentFile?.delete()
        currentFile = null
    }
}

/**
 * Multi-track audio preview player synchronized to video timeline position,
 * plus single-track previewer for the audio library.
 */
class VideoAudioTrackMixer(private val context: Context) {

    private data class PlayerSlot(
        val trackId: String,
        val uriString: String,
        val player: MediaPlayer,
        var prepared: Boolean = false
    )

    private val trackPlayers = mutableMapOf<String, PlayerSlot>()
    private var singlePreviewPlayer: MediaPlayer? = null
    var previewingUri: String? = null
        private set

    fun syncTracks(tracks: List<ProAudioTrack>) {
        val desiredIds = tracks.map { it.id }.toSet()
        val toRemove = trackPlayers.keys.filter { it !in desiredIds }
        toRemove.forEach { id ->
            releaseSlot(trackPlayers.remove(id))
        }
        for (track in tracks) {
            val existing = trackPlayers[track.id]
            if (existing != null && existing.uriString == track.uriString) continue
            if (existing != null) releaseSlot(trackPlayers.remove(track.id))
            if (track.uriString.isBlank()) continue
            try {
                val uri = TemplateSeedCatalog.resolvePlayableUri(context, track.uriString)
                    ?: Uri.parse(track.uriString)
                val mp = MediaPlayer()
                val slot = PlayerSlot(track.id, track.uriString, mp, prepared = false)
                mp.setOnPreparedListener { slot.prepared = true }
                mp.setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Audio track '${track.title}' playback error: $what/$extra")
                    true
                }
                mp.setDataSource(context, uri)
                mp.prepareAsync()
                trackPlayers[track.id] = slot
            } catch (e: Exception) {
                Log.w(TAG, "Unable to load audio '${track.title}': ${e.message}")
            }
        }
    }

    fun updatePlayback(tracks: List<ProAudioTrack>, timelinePosMs: Long, isPlaying: Boolean) {
        for (track in tracks) {
            val slot = trackPlayers[track.id] ?: continue
            if (!slot.prepared) continue
            val mp = slot.player
            val gain = track.effectiveGainAt(timelinePosMs)
            val vol = if (track.noiseReduction) (gain * 0.92f).coerceIn(0f, 1f) else gain.coerceIn(0f, 1f)
            try {
                mp.setVolume(vol, vol)
                val inWindow = timelinePosMs in track.startMs until track.endMsOnTimeline
                if (isPlaying && inWindow && !track.muted) {
                    val targetAudioPos = (track.trimStartMs + (timelinePosMs - track.startMs)).toInt()
                    val drift = abs(mp.currentPosition - targetAudioPos)
                    if (drift > 450) {
                        mp.seekTo(targetAudioPos)
                    }
                    if (!mp.isPlaying) {
                        mp.seekTo(targetAudioPos)
                        mp.start()
                    }
                } else {
                    if (mp.isPlaying) {
                        mp.pause()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun seekAllTo(tracks: List<ProAudioTrack>, timelinePosMs: Long) {
        for (track in tracks) {
            val slot = trackPlayers[track.id] ?: continue
            if (!slot.prepared) continue
            val targetAudioPos = (track.trimStartMs + (timelinePosMs - track.startMs).coerceAtLeast(0L)).toInt()
            try {
                slot.player.seekTo(targetAudioPos)
            } catch (_: Exception) {
            }
        }
    }

    fun previewSingle(uriString: String, onFinished: () -> Unit = {}) {
        stopSinglePreview()
        try {
            val uri = TemplateSeedCatalog.resolvePlayableUri(context, uriString) ?: Uri.parse(uriString)
            val mp = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    previewingUri = null
                    onFinished()
                }
                setOnErrorListener { _, _, _ ->
                    previewingUri = null
                    onFinished()
                    true
                }
                prepareAsync()
            }
            singlePreviewPlayer = mp
            previewingUri = uriString
        } catch (e: Exception) {
            Log.w(TAG, "Single preview failed: ${e.message}")
            previewingUri = null
            onFinished()
        }
    }

    fun stopSinglePreview() {
        try {
            singlePreviewPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            singlePreviewPlayer?.release()
        } catch (_: Exception) {
        }
        singlePreviewPlayer = null
        previewingUri = null
    }

    fun releaseAll() {
        stopSinglePreview()
        trackPlayers.values.forEach { releaseSlot(it) }
        trackPlayers.clear()
    }

    private fun releaseSlot(slot: PlayerSlot?) {
        if (slot == null) return
        try {
            if (slot.player.isPlaying) slot.player.stop()
        } catch (_: Exception) {
        }
        try {
            slot.player.release()
        } catch (_: Exception) {
        }
    }
}
