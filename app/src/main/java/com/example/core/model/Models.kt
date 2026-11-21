package com.example.core.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val role: String = "user",
    val blocked: Boolean = false,
    val themeMode: String = "dark",
    val language: String = "pt",
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserCredits(
    @DocumentId val id: String = "",
    val userId: String = "",
    val balance: Long = 50L,
    val totalEarned: Long = 50L,
    val totalSpent: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreditTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "welcome",
    val amount: Long = 0L,
    val description: String = "",
    val referenceId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RewardAdLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val adUnitId: String = "",
    val rewardAmount: Long = 2L,
    val status: String = "completed",
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DesignTemplate(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",
    val previewStyle: String = "modern",
    val templateType: String = "image", // "image" or "video"
    val aspectRatio: String = "1:1",    // "1:1", "3:4", "4:5", "16:9", "9:16"
    val width: Long = 1080L,
    val height: Long = 1080L,
    val backgroundColorHex: String = "#0A1628",
    val backgroundGradientHex: String? = null,
    val layers: List<Map<String, Any>> = emptyList(),
    val videoClips: List<Map<String, Any>> = emptyList(),
    val fontsUsed: List<String> = emptyList(),
    val colorsUsed: List<String> = emptyList(),
    val premium: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveAspectRatio(): String {
        if (aspectRatio.isNotBlank() && aspectRatio != "1:1") return aspectRatio
        val w = width.coerceAtLeast(1L)
        val h = height.coerceAtLeast(1L)
        return when {
            w == h -> "1:1"
            w * 4L == h * 3L -> "3:4"
            w * 5L == h * 4L -> "4:5"
            w > h -> "16:9"
            w * 16L == h * 9L -> "9:16"
            else -> aspectRatio.ifBlank { "1:1" }
        }
    }
}

@Keep
@IgnoreExtraProperties
data class CategoryEntry(
    @DocumentId val id: String = "",
    val name: String = "",
    val iconKey: String = "sparkles",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DesignProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Novo Design",
    val category: String = "Instagram",
    val formatName: String = "Instagram Quadrado",
    val width: Long = 1080L,
    val height: Long = 1080L,
    val backgroundColorHex: String = "#0A1628",
    val backgroundGradientHex: String? = null,
    val layers: List<Map<String, Any>> = emptyList(),
    val videoClips: List<Map<String, Any>> = emptyList(),
    val projectType: String = "design",
    val favorite: Boolean = false,
    val thumbnailText: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExportRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectTitle: String = "",
    val exportType: String = "image_png",
    val resolution: String = "1080x1080",
    val creditsCost: Long = 2L,
    val status: String = "pending",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val itemType: String = "template",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GraphicAsset(
    @DocumentId val id: String = "",
    val name: String = "",
    val assetType: String = "sticker",
    val category: String = "Geral",
    val colorHex: String = "#FF7A00",
    val symbolKey: String = "star",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FontEntry(
    @DocumentId val id: String = "",
    val name: String = "",
    val familyKey: String = "plus_jakarta_sans",
    val category: String = "Sans",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "all",
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReportItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// --- Domain Editor Models (Serialized into Firestore Native Maps) ---

enum class LayerType { TEXT, SHAPE, IMAGE, LINE, STICKER }

enum class ShapeKind {
    RECTANGLE,
    ROUNDED_RECT,
    CIRCLE,
    TRIANGLE,
    STAR,
    BADGE,
    ARROW,
    FRAME,
    VERIFIED_BADGE,
    QUOTE_MARKS,
    BOTTOM_ARCH,
    DIAMOND_LINE,
    DOT_GRID,
    BRACKET_FRAME,
    PILL_OUTLINE,
    SCRIM_BOTTOM
}

enum class ImageFilterKind { NORMAL, VIVID, WARM, COOL, BW, VINTAGE }

enum class ImageMaskKind { NONE, ROUNDED, CIRCLE }

data class DesignLayer(
    val layerId: String,
    val type: LayerType,
    val name: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val widthFraction: Float = 0.6f,
    val heightFraction: Float = 0.2f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val groupId: String? = null,
    // Text properties
    val text: String = "",
    val fontFamilyKey: String = "plus_jakarta_sans",
    val fontSizeSp: Float = 32f,
    val colorHex: String = "#FFFFFF",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val textAlign: String = "center", // left, center, right
    val letterSpacing: Float = 0f,
    val outlineEnabled: Boolean = false,
    val outlineColorHex: String = "#000000",
    val shadowEnabled: Boolean = false,
    val curved: Boolean = false,
    // Shape / Sticker properties
    val shapeKind: ShapeKind = ShapeKind.ROUNDED_RECT,
    val cornerRadiusDp: Float = 16f,
    val symbolKey: String = "star",
    // Image properties
    val imageUri: String? = null,
    val brightness: Float = 0f, // -1f..1f
    val contrast: Float = 1f,   // 0.5f..2f
    val saturation: Float = 1f, // 0f..2f
    val filterKind: ImageFilterKind = ImageFilterKind.NORMAL,
    val maskKind: ImageMaskKind = ImageMaskKind.NONE
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("layerId", layerId)
        put("type", type.name)
        put("name", name)
        put("xFraction", xFraction.toDouble())
        put("yFraction", yFraction.toDouble())
        put("widthFraction", widthFraction.toDouble())
        put("heightFraction", heightFraction.toDouble())
        put("rotation", rotation.toDouble())
        put("opacity", opacity.toDouble())
        put("visible", visible)
        put("locked", locked)
        groupId?.let { put("groupId", it) }
        put("text", text)
        put("fontFamilyKey", fontFamilyKey)
        put("fontSizeSp", fontSizeSp.toDouble())
        put("colorHex", colorHex)
        put("bold", bold)
        put("italic", italic)
        put("textAlign", textAlign)
        put("letterSpacing", letterSpacing.toDouble())
        put("outlineEnabled", outlineEnabled)
        put("outlineColorHex", outlineColorHex)
        put("shadowEnabled", shadowEnabled)
        put("curved", curved)
        put("shapeKind", shapeKind.name)
        put("cornerRadiusDp", cornerRadiusDp.toDouble())
        put("symbolKey", symbolKey)
        imageUri?.let { put("imageUri", it) }
        put("brightness", brightness.toDouble())
        put("contrast", contrast.toDouble())
        put("saturation", saturation.toDouble())
        put("filterKind", filterKind.name)
        put("maskKind", maskKind.name)
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): DesignLayer {
            val typeStr = map["type"] as? String ?: LayerType.TEXT.name
            val shapeStr = map["shapeKind"] as? String ?: ShapeKind.ROUNDED_RECT.name
            val filterStr = map["filterKind"] as? String ?: ImageFilterKind.NORMAL.name
            val maskStr = map["maskKind"] as? String ?: ImageMaskKind.NONE.name
            return DesignLayer(
                layerId = map["layerId"] as? String ?: java.util.UUID.randomUUID().toString(),
                type = enumValues<LayerType>().firstOrNull { it.name == typeStr } ?: LayerType.TEXT,
                name = map["name"] as? String ?: "Camada",
                xFraction = (map["xFraction"] as? Number)?.toFloat() ?: 0.5f,
                yFraction = (map["yFraction"] as? Number)?.toFloat() ?: 0.5f,
                widthFraction = (map["widthFraction"] as? Number)?.toFloat() ?: 0.6f,
                heightFraction = (map["heightFraction"] as? Number)?.toFloat() ?: 0.2f,
                rotation = (map["rotation"] as? Number)?.toFloat() ?: 0f,
                opacity = (map["opacity"] as? Number)?.toFloat() ?: 1f,
                visible = map["visible"] as? Boolean ?: true,
                locked = map["locked"] as? Boolean ?: false,
                groupId = map["groupId"] as? String,
                text = map["text"] as? String ?: "",
                fontFamilyKey = map["fontFamilyKey"] as? String ?: "plus_jakarta_sans",
                fontSizeSp = (map["fontSizeSp"] as? Number)?.toFloat() ?: 32f,
                colorHex = map["colorHex"] as? String ?: "#FFFFFF",
                bold = map["bold"] as? Boolean ?: false,
                italic = map["italic"] as? Boolean ?: false,
                textAlign = map["textAlign"] as? String ?: "center",
                letterSpacing = (map["letterSpacing"] as? Number)?.toFloat() ?: 0f,
                outlineEnabled = map["outlineEnabled"] as? Boolean ?: false,
                outlineColorHex = map["outlineColorHex"] as? String ?: "#000000",
                shadowEnabled = map["shadowEnabled"] as? Boolean ?: false,
                curved = map["curved"] as? Boolean ?: false,
                shapeKind = enumValues<ShapeKind>().firstOrNull { it.name == shapeStr } ?: ShapeKind.ROUNDED_RECT,
                cornerRadiusDp = (map["cornerRadiusDp"] as? Number)?.toFloat() ?: 16f,
                symbolKey = map["symbolKey"] as? String ?: "star",
                imageUri = map["imageUri"] as? String,
                brightness = (map["brightness"] as? Number)?.toFloat() ?: 0f,
                contrast = (map["contrast"] as? Number)?.toFloat() ?: 1f,
                saturation = (map["saturation"] as? Number)?.toFloat() ?: 1f,
                filterKind = enumValues<ImageFilterKind>().firstOrNull { it.name == filterStr } ?: ImageFilterKind.NORMAL,
                maskKind = enumValues<ImageMaskKind>().firstOrNull { it.name == maskStr } ?: ImageMaskKind.NONE
            )
        }
    }
}

enum class VideoTransitionKind(val label: String) {
    NONE("Nenhuma"),
    FADE("Dissolver"),
    SLIDE("Deslizar"),
    ZOOM("Zoom"),
    FLASH("Flash"),
    BLUR("Blur"),
    ROTATE("Rotação"),
    FLIP_3D("3D Flip"),
    CINEMATIC("Cinematográfica"),
    CUSTOM("Personalizada")
}

enum class VideoFilterKind(val label: String) {
    NORMAL("Original"),
    CINEMA("Cinema"),
    WARM("Quente"),
    COOL("Frio"),
    BW("P&B"),
    VINTAGE("Vintage"),
    VIVID("Vívido"),
    NOIR("Noir"),
    TEAL_ORANGE("Teal & Orange")
}

data class VideoClipItem(
    val clipId: String,
    val title: String,
    val uriString: String,
    val mediaType: String = "video", // "video" or "image"
    val durationMs: Long = 5000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 5000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val muted: Boolean = false,
    val filter: VideoFilterKind = VideoFilterKind.NORMAL,
    val transition: VideoTransitionKind = VideoTransitionKind.NONE,
    val animation: String = "none",
    // Transform & Crop
    val zoomScale: Float = 1.0f,
    val offsetX: Float = 0.0f,
    val offsetY: Float = 0.0f,
    val rotationDeg: Float = 0.0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val cropMode: String = "fill", // "fill", "fit", "1:1", "4:5", "16:9", "9:16"
    // Playback & Transition & Speed Curve
    val reversed: Boolean = false,
    val frozenFromClipId: String? = null,
    val transitionDurationMs: Long = 600L,
    val speedCurve: String = "linear",
    val animOut: String = "none",
    val animCombo: String = "none",
    val animDurationMs: Long = 500L,
    // Pro Color Grading & HSL & Curves
    val brightness: Float = 0.0f,    // -0.5..0.5
    val contrast: Float = 1.0f,      // 0.5..2.0
    val saturation: Float = 1.0f,    // 0.0..2.0
    val exposure: Float = 0.0f,      // -0.5..0.5
    val temperature: Float = 0.0f,   // -1.0..1.0
    val sharpness: Float = 0.0f,     // 0.0..1.0
    val vignette: Float = 0.0f,      // 0.0..1.0
    val grain: Float = 0.0f,         // 0.0..1.0
    val hueShift: Float = 0.0f,      // -180..180
    val rgbGamma: Float = 1.0f,      // 0.5..1.8 (color curves)
    // Pro FX & Mask & Chroma Key
    val effectName: String = "NONE",
    val effectIntensity: Float = 0.6f,
    val maskShapeName: String = "NONE",
    val chromaKeyEnabled: Boolean = false,
    val chromaKeyColorHex: String = "#00FF00",
    val chromaKeyTolerance: Float = 0.35f,
    // Keyframes & Motion Tracking (serialized compact maps)
    val keyframesData: List<Map<String, Any>> = emptyList(),
    val motionTrackX: Float? = null,
    val motionTrackY: Float? = null
) {
    fun toMap(): Map<String, Any> = buildMap {
        put("clipId", clipId)
        put("title", title)
        put("uriString", uriString)
        put("mediaType", mediaType)
        put("durationMs", durationMs)
        put("trimStartMs", trimStartMs)
        put("trimEndMs", trimEndMs)
        put("speed", speed.toDouble())
        put("volume", volume.toDouble())
        put("muted", muted)
        put("filter", filter.name)
        put("transition", transition.name)
        put("animation", animation)
        put("zoomScale", zoomScale.toDouble())
        put("offsetX", offsetX.toDouble())
        put("offsetY", offsetY.toDouble())
        put("rotationDeg", rotationDeg.toDouble())
        put("flipH", flipH)
        put("flipV", flipV)
        put("cropMode", cropMode)
        put("reversed", reversed)
        frozenFromClipId?.let { put("frozenFromClipId", it) }
        put("transitionDurationMs", transitionDurationMs)
        put("speedCurve", speedCurve)
        put("animOut", animOut)
        put("animCombo", animCombo)
        put("animDurationMs", animDurationMs)
        put("brightness", brightness.toDouble())
        put("contrast", contrast.toDouble())
        put("saturation", saturation.toDouble())
        put("exposure", exposure.toDouble())
        put("temperature", temperature.toDouble())
        put("sharpness", sharpness.toDouble())
        put("vignette", vignette.toDouble())
        put("grain", grain.toDouble())
        put("hueShift", hueShift.toDouble())
        put("rgbGamma", rgbGamma.toDouble())
        put("effectName", effectName)
        put("effectIntensity", effectIntensity.toDouble())
        put("maskShapeName", maskShapeName)
        put("chromaKeyEnabled", chromaKeyEnabled)
        put("chromaKeyColorHex", chromaKeyColorHex)
        put("chromaKeyTolerance", chromaKeyTolerance.toDouble())
        if (keyframesData.isNotEmpty()) put("keyframesData", keyframesData)
        motionTrackX?.let { put("motionTrackX", it.toDouble()) }
        motionTrackY?.let { put("motionTrackY", it.toDouble()) }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): VideoClipItem {
            val filterStr = map["filter"] as? String ?: VideoFilterKind.NORMAL.name
            val transStr = map["transition"] as? String ?: VideoTransitionKind.NONE.name
            val kfRaw = (map["keyframesData"] as? List<*>)
                ?.mapNotNull { it as? Map<String, Any> }
                ?: emptyList()
            return VideoClipItem(
                clipId = map["clipId"] as? String ?: java.util.UUID.randomUUID().toString(),
                title = map["title"] as? String ?: "Clipe",
                uriString = map["uriString"] as? String ?: "",
                mediaType = map["mediaType"] as? String ?: "video",
                durationMs = (map["durationMs"] as? Number)?.toLong() ?: 5000L,
                trimStartMs = (map["trimStartMs"] as? Number)?.toLong() ?: 0L,
                trimEndMs = (map["trimEndMs"] as? Number)?.toLong() ?: 5000L,
                speed = (map["speed"] as? Number)?.toFloat() ?: 1.0f,
                volume = (map["volume"] as? Number)?.toFloat() ?: 1.0f,
                muted = map["muted"] as? Boolean ?: false,
                filter = enumValues<VideoFilterKind>().firstOrNull { it.name == filterStr } ?: VideoFilterKind.NORMAL,
                transition = enumValues<VideoTransitionKind>().firstOrNull { it.name == transStr } ?: VideoTransitionKind.NONE,
                animation = map["animation"] as? String ?: "none",
                zoomScale = (map["zoomScale"] as? Number)?.toFloat() ?: 1.0f,
                offsetX = (map["offsetX"] as? Number)?.toFloat() ?: 0.0f,
                offsetY = (map["offsetY"] as? Number)?.toFloat() ?: 0.0f,
                rotationDeg = (map["rotationDeg"] as? Number)?.toFloat() ?: 0.0f,
                flipH = map["flipH"] as? Boolean ?: false,
                flipV = map["flipV"] as? Boolean ?: false,
                cropMode = map["cropMode"] as? String ?: "fill",
                reversed = map["reversed"] as? Boolean ?: false,
                frozenFromClipId = map["frozenFromClipId"] as? String,
                transitionDurationMs = (map["transitionDurationMs"] as? Number)?.toLong() ?: 600L,
                speedCurve = map["speedCurve"] as? String ?: "linear",
                animOut = map["animOut"] as? String ?: "none",
                animCombo = map["animCombo"] as? String ?: "none",
                animDurationMs = (map["animDurationMs"] as? Number)?.toLong() ?: 500L,
                brightness = (map["brightness"] as? Number)?.toFloat() ?: 0.0f,
                contrast = (map["contrast"] as? Number)?.toFloat() ?: 1.0f,
                saturation = (map["saturation"] as? Number)?.toFloat() ?: 1.0f,
                exposure = (map["exposure"] as? Number)?.toFloat() ?: 0.0f,
                temperature = (map["temperature"] as? Number)?.toFloat() ?: 0.0f,
                sharpness = (map["sharpness"] as? Number)?.toFloat() ?: 0.0f,
                vignette = (map["vignette"] as? Number)?.toFloat() ?: 0.0f,
                grain = (map["grain"] as? Number)?.toFloat() ?: 0.0f,
                hueShift = (map["hueShift"] as? Number)?.toFloat() ?: 0.0f,
                rgbGamma = (map["rgbGamma"] as? Number)?.toFloat() ?: 1.0f,
                effectName = map["effectName"] as? String ?: "NONE",
                effectIntensity = (map["effectIntensity"] as? Number)?.toFloat() ?: 0.6f,
                maskShapeName = map["maskShapeName"] as? String ?: "NONE",
                chromaKeyEnabled = map["chromaKeyEnabled"] as? Boolean ?: false,
                chromaKeyColorHex = map["chromaKeyColorHex"] as? String ?: "#00FF00",
                chromaKeyTolerance = (map["chromaKeyTolerance"] as? Number)?.toFloat() ?: 0.35f,
                keyframesData = kfRaw,
                motionTrackX = (map["motionTrackX"] as? Number)?.toFloat(),
                motionTrackY = (map["motionTrackY"] as? Number)?.toFloat()
            )
        }
    }
}

data class VideoTextOverlay(
    val id: String,
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val colorHex: String = "#FFFFFF",
    val yPositionFraction: Float = 0.78f,
    val isSubtitle: Boolean = false
)

data class DesignFormat(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val category: String
) {
    val dimensionLabel: String get() = "${width} × ${height} px"
}

object DesignFormats {
    val presets = listOf(
        DesignFormat("ig_square", "Quadrado 1:1", 1080, 1080, "Instagram"),
        DesignFormat("portrait_3_4", "Retrato 3:4", 1080, 1440, "Instagram"),
        DesignFormat("ig_vertical", "Feed Vertical 4:5", 1080, 1350, "Instagram"),
        DesignFormat("widescreen_16_9", "Panorâmico 16:9", 1920, 1080, "YouTube"),
        DesignFormat("stories_reels", "Vertical 9:16", 1080, 1920, "Instagram"),
        DesignFormat("fb_horizontal", "Facebook Horizontal", 1200, 630, "Facebook"),
        DesignFormat("yt_thumb", "Miniatura YouTube", 1280, 720, "YouTube"),
        DesignFormat("tiktok_vert", "Vertical do TikTok", 1080, 1920, "TikTok"),
        DesignFormat("flyer_a4", "Flyer / Cartaz", 1080, 1528, "Publicidade"),
        DesignFormat("custom", "Formato Personalizado", 1080, 1080, "Personalizado")
    )
}
