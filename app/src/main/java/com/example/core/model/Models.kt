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
    val width: Long = 1080L,
    val height: Long = 1080L,
    val backgroundColorHex: String = "#0A1628",
    val backgroundGradientHex: String? = null,
    val layers: List<Map<String, Any>> = emptyList(),
    val fontsUsed: List<String> = emptyList(),
    val colorsUsed: List<String> = emptyList(),
    val premium: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

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
    FADE("Fade"),
    SLIDE("Deslizar"),
    ZOOM("Zoom")
}

enum class VideoFilterKind(val label: String) {
    NORMAL("Original"),
    CINEMA("Cinema"),
    WARM("Quente"),
    COOL("Frio"),
    BW("P&B")
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
    val animation: String = "none"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "clipId" to clipId,
        "title" to title,
        "uriString" to uriString,
        "mediaType" to mediaType,
        "durationMs" to durationMs,
        "trimStartMs" to trimStartMs,
        "trimEndMs" to trimEndMs,
        "speed" to speed.toDouble(),
        "volume" to volume.toDouble(),
        "muted" to muted,
        "filter" to filter.name,
        "transition" to transition.name,
        "animation" to animation
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): VideoClipItem {
            val filterStr = map["filter"] as? String ?: VideoFilterKind.NORMAL.name
            val transStr = map["transition"] as? String ?: VideoTransitionKind.NONE.name
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
                animation = map["animation"] as? String ?: "none"
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
        DesignFormat("ig_square", "Instagram Quadrado", 1080, 1080, "Instagram"),
        DesignFormat("ig_vertical", "Instagram Vertical", 1080, 1350, "Instagram"),
        DesignFormat("stories_reels", "Stories e Reels", 1080, 1920, "Instagram"),
        DesignFormat("fb_horizontal", "Facebook Horizontal", 1200, 630, "Facebook"),
        DesignFormat("yt_thumb", "Miniatura YouTube", 1280, 720, "YouTube"),
        DesignFormat("tiktok_vert", "Vertical do TikTok", 1080, 1920, "TikTok"),
        DesignFormat("flyer_a4", "Flyer / Cartaz", 1080, 1528, "Publicidade"),
        DesignFormat("custom", "Formato Personalizado", 1080, 1080, "Personalizado")
    )
}
