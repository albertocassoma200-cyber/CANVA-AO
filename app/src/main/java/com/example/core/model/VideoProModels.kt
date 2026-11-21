package com.example.core.model

import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin

enum class ProEffectKind(val label: String) {
    NONE("Nenhum"),
    CINEMA_BARS("Cinemático"),
    GLITCH("Glitch"),
    BLUR("Blur"),
    MOTION_BLUR("Desfoque Mov."),
    EFFECT_3D("3D Anaglifo"),
    LIGHT_FLARE("Luz & Flare"),
    SHAKE_MOTION("Movimento"),
    VINTAGE_FILM("Vintage")
}

enum class VideoMaskShape(val label: String) {
    NONE("Sem Máscara"),
    CIRCLE("Círculo"),
    ROUNDED("Arredondado"),
    SPLIT_H("Divisão Horiz."),
    HEART("Coração"),
    STAR("Estrela"),
    DIAMOND("Losango")
}

enum class VideoBlendMode(val label: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiplicar"),
    SCREEN("Tela (Screen)"),
    OVERLAY("Sobrepor"),
    DARKEN("Escurecer"),
    LIGHTEN("Clarear"),
    ADD("Adicionar")
}

enum class SpeedCurvePreset(val key: String, val label: String) {
    LINEAR("linear", "Normal"),
    MONTAGE("montage", "Montagem"),
    HERO("hero", "Herói"),
    BULLET("bullet", "Bala"),
    JUMP_CUT("jump_cut", "Salto"),
    FLASH_IN("flash_in", "Flash In"),
    FLASH_OUT("flash_out", "Flash Out");

    fun speedMultiplierAt(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return when (this) {
            LINEAR -> 1f
            MONTAGE -> if (p < 0.2f || p > 0.8f) 1.8f else 0.7f
            HERO -> if (p in 0.3f..0.65f) 0.45f else 1.55f
            BULLET -> if (p in 0.25f..0.75f) 0.35f else 2.2f
            JUMP_CUT -> if (p < 0.5f) 1.75f else 0.75f
            FLASH_IN -> 2.0f - p * 1.1f
            FLASH_OUT -> 0.8f + p * 1.3f
        }
    }
}

enum class VideoBackgroundAnimatedStyle(val key: String, val label: String) {
    NONE("none", "Estático"),
    AURORA("aurora", "Aurora"),
    PULSE_GRID("pulse_grid", "Grelha Neon"),
    PARTICLES("particles", "Partículas"),
    SUNSET_WAVE("sunset_wave", "Ondas"),
    NEON_SCAN("neon_scan", "Scanline")
}

enum class VideoExportQuality(val label: String, val bitrateMultiplier: Float) {
    LOW("Compacta (Rápida)", 0.6f),
    MEDIUM("Equilibrada", 1.0f),
    HIGH("Alta Qualidade", 1.5f),
    ULTRA("Master Pro", 2.2f)
}

data class VideoKeyframe(
    val id: String = "kf_${UUID.randomUUID().toString().take(6)}",
    val timeFraction: Float = 0f, // 0f..1f within item duration
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "timeFraction" to timeFraction.toDouble(),
        "xFraction" to xFraction.toDouble(),
        "yFraction" to yFraction.toDouble(),
        "scale" to scale.toDouble(),
        "rotation" to rotation.toDouble(),
        "opacity" to opacity.toDouble()
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): VideoKeyframe = VideoKeyframe(
            id = map["id"] as? String ?: "kf_${UUID.randomUUID().toString().take(6)}",
            timeFraction = (map["timeFraction"] as? Number)?.toFloat() ?: 0f,
            xFraction = (map["xFraction"] as? Number)?.toFloat() ?: 0.5f,
            yFraction = (map["yFraction"] as? Number)?.toFloat() ?: 0.5f,
            scale = (map["scale"] as? Number)?.toFloat() ?: 1f,
            rotation = (map["rotation"] as? Number)?.toFloat() ?: 0f,
            opacity = (map["opacity"] as? Number)?.toFloat() ?: 1f
        )
    }
}

data class InterpolatedTransform(
    val xFraction: Float,
    val yFraction: Float,
    val scale: Float,
    val rotation: Float,
    val opacity: Float
)

fun interpolateKeyframes(
    keyframes: List<VideoKeyframe>,
    progress: Float,
    defaultX: Float,
    defaultY: Float,
    defaultScale: Float,
    defaultRotation: Float,
    defaultOpacity: Float
): InterpolatedTransform {
    if (keyframes.isEmpty()) {
        return InterpolatedTransform(defaultX, defaultY, defaultScale, defaultRotation, defaultOpacity)
    }
    val sorted = keyframes.sortedBy { it.timeFraction }
    val t = progress.coerceIn(0f, 1f)
    if (t <= sorted.first().timeFraction) {
        val k = sorted.first()
        return InterpolatedTransform(k.xFraction, k.yFraction, k.scale, k.rotation, k.opacity)
    }
    if (t >= sorted.last().timeFraction) {
        val k = sorted.last()
        return InterpolatedTransform(k.xFraction, k.yFraction, k.scale, k.rotation, k.opacity)
    }
    for (i in 0 until sorted.lastIndex) {
        val a = sorted[i]
        val b = sorted[i + 1]
        if (t in a.timeFraction..b.timeFraction) {
            val span = (b.timeFraction - a.timeFraction).coerceAtLeast(0.0001f)
            val rawF = ((t - a.timeFraction) / span).coerceIn(0f, 1f)
            // Smooth ease-in-out cubic curve
            val f = (0.5f - 0.5f * kotlin.math.cos(rawF * PI)).toFloat()
            return InterpolatedTransform(
                xFraction = a.xFraction + (b.xFraction - a.xFraction) * f,
                yFraction = a.yFraction + (b.yFraction - a.yFraction) * f,
                scale = a.scale + (b.scale - a.scale) * f,
                rotation = a.rotation + (b.rotation - a.rotation) * f,
                opacity = a.opacity + (b.opacity - a.opacity) * f
            )
        }
    }
    return InterpolatedTransform(defaultX, defaultY, defaultScale, defaultRotation, defaultOpacity)
}

data class ProAudioTrack(
    val id: String = "aud_${UUID.randomUUID().toString().take(8)}",
    val title: String,
    val uriString: String,
    val sourceType: String = "music", // "music", "device", "voiceover", "extracted", "sfx", "tts"
    val startMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 8000L,
    val durationMs: Long = 8000L,
    val volume: Float = 1.0f,
    val muted: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val noiseReduction: Boolean = false,
    val vocalIsolation: Boolean = false,
    val waveformPeaks: List<Float> = emptyList()
) {
    val activeDurationMs: Long get() = (trimEndMs - trimStartMs).coerceAtLeast(300L)
    val endMsOnTimeline: Long get() = startMs + activeDurationMs

    fun effectiveGainAt(timelinePosMs: Long): Float {
        if (muted) return 0f
        val rel = timelinePosMs - startMs
        if (rel < 0L || rel > activeDurationMs) return 0f
        var gain = volume
        if (fadeInMs > 0L && rel < fadeInMs) {
            gain *= (rel.toFloat() / fadeInMs.toFloat()).coerceIn(0f, 1f)
        }
        val remaining = activeDurationMs - rel
        if (fadeOutMs > 0L && remaining < fadeOutMs) {
            gain *= (remaining.toFloat() / fadeOutMs.toFloat()).coerceIn(0f, 1f)
        }
        return gain.coerceIn(0f, 2f)
    }

    fun toLayerMap(): Map<String, Any> = buildMap {
        put("layerId", id)
        put("type", "AUDIO_TRACK")
        put("name", title)
        put("uriString", uriString)
        put("sourceType", sourceType)
        put("startMs", startMs)
        put("trimStartMs", trimStartMs)
        put("trimEndMs", trimEndMs)
        put("durationMs", durationMs)
        put("volume", volume.toDouble())
        put("muted", muted)
        put("fadeInMs", fadeInMs)
        put("fadeOutMs", fadeOutMs)
        put("noiseReduction", noiseReduction)
        put("vocalIsolation", vocalIsolation)
        if (waveformPeaks.isNotEmpty()) {
            put("waveformPeaks", waveformPeaks.take(40).map { it.toDouble() })
        }
    }

    companion object {
        fun fromLayerMap(map: Map<String, Any?>): ProAudioTrack {
            val peaks = (map["waveformPeaks"] as? List<*>)
                ?.mapNotNull { (it as? Number)?.toFloat() }
                ?: emptyList()
            val dur = (map["durationMs"] as? Number)?.toLong() ?: 8000L
            return ProAudioTrack(
                id = map["layerId"] as? String ?: "aud_${UUID.randomUUID().toString().take(8)}",
                title = map["name"] as? String ?: "Faixa de Áudio",
                uriString = map["uriString"] as? String ?: "",
                sourceType = map["sourceType"] as? String ?: "music",
                startMs = (map["startMs"] as? Number)?.toLong() ?: 0L,
                trimStartMs = (map["trimStartMs"] as? Number)?.toLong() ?: 0L,
                trimEndMs = (map["trimEndMs"] as? Number)?.toLong() ?: dur,
                durationMs = dur,
                volume = (map["volume"] as? Number)?.toFloat() ?: 1.0f,
                muted = map["muted"] as? Boolean ?: false,
                fadeInMs = (map["fadeInMs"] as? Number)?.toLong() ?: 0L,
                fadeOutMs = (map["fadeOutMs"] as? Number)?.toLong() ?: 0L,
                noiseReduction = map["noiseReduction"] as? Boolean ?: false,
                vocalIsolation = map["vocalIsolation"] as? Boolean ?: false,
                waveformPeaks = peaks
            )
        }
    }
}

enum class VideoOverlayKind(val label: String) {
    TEXT("Texto"),
    SUBTITLE("Legenda"),
    IMAGE("Imagem"),
    VIDEO_PIP("Vídeo PiP"),
    STICKER("Sticker"),
    EMOJI("Emoji"),
    SHAPE("Forma"),
    FRAME("Moldura"),
    VERIFIED_BADGE("Selo Azul"),
    LOGO("Logótipo"),
    WATERMARK("Marca d'Água"),
    EFFECT_CLIP("Efeito")
}

data class ProOverlayItem(
    val id: String = "ov_${UUID.randomUUID().toString().take(8)}",
    val kind: VideoOverlayKind = VideoOverlayKind.TEXT,
    val title: String = "Camada",
    val text: String = "",
    val uriString: String? = null,
    val symbolKey: String = "star",
    val fontFamilyKey: String = "plus_jakarta_sans",
    val customFontUri: String? = null,
    val startMs: Long = 0L,
    val endMs: Long = 6000L,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val scale: Float = 1.0f,
    val rotation: Float = 0.0f,
    val opacity: Float = 1.0f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val blendMode: VideoBlendMode = VideoBlendMode.NORMAL,
    val maskShape: VideoMaskShape = VideoMaskShape.NONE,
    val chromaKeyEnabled: Boolean = false,
    val chromaKeyColorHex: String = "#00FF00",
    val chromaKeyTolerance: Float = 0.35f,
    // Text & Graphics Styling
    val fontSizeSp: Float = 24f,
    val colorHex: String = "#FFFFFF",
    val bgColorHex: String? = null,
    val gradientStartHex: String? = null,
    val gradientEndHex: String? = null,
    val outlineEnabled: Boolean = false,
    val outlineColorHex: String = "#000000",
    val shadowEnabled: Boolean = true,
    val is3DText: Boolean = false,
    val textAlign: String = "center",
    val animIn: String = "none", // none, fade, pop, slide_up, zoom, typewriter
    val animOut: String = "none",
    val typewriterEffect: Boolean = false,
    val effectKind: ProEffectKind = ProEffectKind.NONE,
    val keyframes: List<VideoKeyframe> = emptyList()
) {
    fun isActiveAt(posMs: Long): Boolean = visible && posMs in startMs..endMs

    fun toLayerMap(): Map<String, Any> = buildMap {
        put("layerId", id)
        put("type", "PRO_OVERLAY")
        put("overlayKind", kind.name)
        put("name", title)
        put("text", text)
        uriString?.let { put("imageUri", it) }
        put("symbolKey", symbolKey)
        put("fontFamilyKey", fontFamilyKey)
        customFontUri?.let { put("customFontUri", it) }
        put("startMs", startMs)
        put("endMs", endMs)
        put("xFraction", xFraction.toDouble())
        put("yFraction", yFraction.toDouble())
        put("scale", scale.toDouble())
        put("rotation", rotation.toDouble())
        put("opacity", opacity.toDouble())
        put("visible", visible)
        put("locked", locked)
        put("blendMode", blendMode.name)
        put("maskShape", maskShape.name)
        put("chromaKeyEnabled", chromaKeyEnabled)
        put("chromaKeyColorHex", chromaKeyColorHex)
        put("chromaKeyTolerance", chromaKeyTolerance.toDouble())
        put("fontSizeSp", fontSizeSp.toDouble())
        put("colorHex", colorHex)
        bgColorHex?.let { put("bgColorHex", it) }
        gradientStartHex?.let { put("gradientStartHex", it) }
        gradientEndHex?.let { put("gradientEndHex", it) }
        put("outlineEnabled", outlineEnabled)
        put("outlineColorHex", outlineColorHex)
        put("shadowEnabled", shadowEnabled)
        put("is3DText", is3DText)
        put("textAlign", textAlign)
        put("animIn", animIn)
        put("animOut", animOut)
        put("typewriterEffect", typewriterEffect)
        put("effectKind", effectKind.name)
        if (keyframes.isNotEmpty()) {
            put("keyframes", keyframes.map { it.toMap() })
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromLayerMap(map: Map<String, Any?>): ProOverlayItem {
            val kindName = map["overlayKind"] as? String
            val legacyType = map["type"] as? String
            val resolvedKind = when {
                kindName != null -> enumValues<VideoOverlayKind>().firstOrNull { it.name == kindName } ?: VideoOverlayKind.TEXT
                legacyType == "TEXT" -> {
                    val y = (map["yFraction"] as? Number)?.toFloat() ?: 0.5f
                    if (y > 0.65f) VideoOverlayKind.SUBTITLE else VideoOverlayKind.TEXT
                }
                legacyType == "SHAPE" -> VideoOverlayKind.SHAPE
                legacyType == "STICKER" -> VideoOverlayKind.STICKER
                legacyType == "IMAGE" -> VideoOverlayKind.IMAGE
                else -> VideoOverlayKind.TEXT
            }
            val kfList = (map["keyframes"] as? List<*>)
                ?.mapNotNull { it as? Map<String, Any?> }
                ?.map { VideoKeyframe.fromMap(it) }
                ?: emptyList()
            return ProOverlayItem(
                id = map["layerId"] as? String ?: "ov_${UUID.randomUUID().toString().take(8)}",
                kind = resolvedKind,
                title = map["name"] as? String ?: resolvedKind.label,
                text = map["text"] as? String ?: "",
                uriString = map["imageUri"] as? String,
                symbolKey = map["symbolKey"] as? String ?: "star",
                fontFamilyKey = map["fontFamilyKey"] as? String ?: "plus_jakarta_sans",
                customFontUri = map["customFontUri"] as? String,
                startMs = (map["startMs"] as? Number)?.toLong() ?: 0L,
                endMs = (map["endMs"] as? Number)?.toLong() ?: 6000L,
                xFraction = (map["xFraction"] as? Number)?.toFloat() ?: 0.5f,
                yFraction = (map["yFraction"] as? Number)?.toFloat() ?: 0.5f,
                scale = (map["scale"] as? Number)?.toFloat() ?: 1.0f,
                rotation = (map["rotation"] as? Number)?.toFloat() ?: 0.0f,
                opacity = (map["opacity"] as? Number)?.toFloat() ?: 1.0f,
                visible = map["visible"] as? Boolean ?: true,
                locked = map["locked"] as? Boolean ?: false,
                blendMode = enumValues<VideoBlendMode>().firstOrNull { it.name == (map["blendMode"] as? String) } ?: VideoBlendMode.NORMAL,
                maskShape = enumValues<VideoMaskShape>().firstOrNull { it.name == (map["maskShape"] as? String) } ?: VideoMaskShape.NONE,
                chromaKeyEnabled = map["chromaKeyEnabled"] as? Boolean ?: false,
                chromaKeyColorHex = map["chromaKeyColorHex"] as? String ?: "#00FF00",
                chromaKeyTolerance = (map["chromaKeyTolerance"] as? Number)?.toFloat() ?: 0.35f,
                fontSizeSp = (map["fontSizeSp"] as? Number)?.toFloat() ?: ((map["fontSize"] as? Number)?.toFloat() ?: 24f),
                colorHex = map["colorHex"] as? String ?: "#FFFFFF",
                bgColorHex = map["bgColorHex"] as? String,
                gradientStartHex = map["gradientStartHex"] as? String,
                gradientEndHex = map["gradientEndHex"] as? String,
                outlineEnabled = map["outlineEnabled"] as? Boolean ?: false,
                outlineColorHex = map["outlineColorHex"] as? String ?: "#000000",
                shadowEnabled = map["shadowEnabled"] as? Boolean ?: true,
                is3DText = map["is3DText"] as? Boolean ?: false,
                textAlign = map["textAlign"] as? String ?: "center",
                animIn = map["animIn"] as? String ?: "none",
                animOut = map["animOut"] as? String ?: "none",
                typewriterEffect = map["typewriterEffect"] as? Boolean ?: false,
                effectKind = enumValues<ProEffectKind>().firstOrNull { it.name == (map["effectKind"] as? String) } ?: ProEffectKind.NONE,
                keyframes = kfList
            )
        }
    }
}

data class VideoEditorSnapshot(
    val clips: List<VideoClipItem>,
    val audioTracks: List<ProAudioTrack>,
    val overlays: List<ProOverlayItem>,
    val aspectRatio: String,
    val bgColorStart: String,
    val bgColorEnd: String?,
    val bgImageUri: String?,
    val animatedBgStyle: VideoBackgroundAnimatedStyle
)
