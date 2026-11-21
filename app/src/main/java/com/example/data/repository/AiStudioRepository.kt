package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.core.model.DesignLayer
import com.example.core.model.DesignProject
import com.example.core.model.DesignTemplate
import com.example.core.model.ImageMaskKind
import com.example.core.model.LayerType
import com.example.core.model.ShapeKind
import com.example.core.model.TemplateSeedCatalog
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "AiStudioRepository"

data class AiDesignProposal(
    val title: String,
    val category: String,
    val formatName: String,
    val width: Int,
    val height: Int,
    val backgroundColorHex: String,
    val backgroundGradientHex: String,
    val headline: String,
    val subheadline: String,
    val badgeText: String,
    val accentColorHex: String,
    val textColorHex: String,
    val fontFamilyKey: String,
    val socialCaption: String,
    val photoTopic: String = "advertising"
) {
    private fun resolvePhotoUri(): String? = when (photoTopic.lowercase()) {
        "social" -> "res://img_tpl_social_creator"
        "news" -> "res://img_tpl_news_studio"
        "advertising" -> "res://img_tpl_ad_product"
        "business" -> "res://img_tpl_business_city"
        "tech" -> "res://img_hero_ideas"
        "city" -> "res://tpl_art_luanda"
        "fashion" -> "res://tpl_art_moda"
        "food" -> "res://tpl_art_restaurante"
        "none" -> null
        else -> "res://img_tpl_ad_product"
    }

    private fun buildLayers(): List<Map<String, Any>> {
        val photoUri = resolvePhotoUri()
        val hasPhoto = photoUri != null
        val layers = mutableListOf<Map<String, Any>>()

        if (photoUri != null) {
            layers += DesignLayer(
                layerId = "ai_photo_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.IMAGE,
                name = "Fotografia de Fundo",
                xFraction = 0.5f,
                yFraction = 0.5f,
                widthFraction = 1.0f,
                heightFraction = 1.0f,
                imageUri = photoUri,
                maskKind = ImageMaskKind.NONE
            ).toMap()
            layers += DesignLayer(
                layerId = "ai_top_scrim_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.SHAPE,
                name = "Contraste Superior",
                xFraction = 0.5f,
                yFraction = 0.12f,
                widthFraction = 1.0f,
                heightFraction = 0.24f,
                colorHex = "#060D1A",
                opacity = 0.45f,
                shapeKind = ShapeKind.RECTANGLE
            ).toMap()
            layers += DesignLayer(
                layerId = "ai_scrim_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.SHAPE,
                name = "Gradiente Editorial",
                xFraction = 0.5f,
                yFraction = 0.64f,
                widthFraction = 1.0f,
                heightFraction = 0.72f,
                colorHex = backgroundColorHex,
                opacity = 0.94f,
                shapeKind = ShapeKind.SCRIM_BOTTOM
            ).toMap()
        } else {
            layers += DesignLayer(
                layerId = "ai_glow_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.SHAPE,
                name = "Destaque Visual",
                xFraction = 0.8f,
                yFraction = 0.22f,
                widthFraction = 0.48f,
                heightFraction = 0.38f,
                colorHex = accentColorHex,
                opacity = 0.28f,
                shapeKind = ShapeKind.CIRCLE
            ).toMap()
        }

        // Verified creator profile corner
        layers += DesignLayer(
            layerId = "prof_avatar",
            type = LayerType.IMAGE,
            name = "Foto da Conta",
            xFraction = 0.12f,
            yFraction = 0.085f,
            widthFraction = 0.105f,
            heightFraction = 0.085f,
            imageUri = TemplateSeedCatalog.TOKEN_USER_AVATAR,
            maskKind = ImageMaskKind.CIRCLE
        ).toMap()
        layers += DesignLayer(
            layerId = "prof_handle",
            type = LayerType.TEXT,
            name = "Perfil Criador",
            xFraction = 0.35f,
            yFraction = 0.085f,
            widthFraction = 0.32f,
            heightFraction = 0.045f,
            text = TemplateSeedCatalog.TOKEN_USER_HANDLE,
            fontFamilyKey = "plus_jakarta_sans",
            fontSizeSp = 12f,
            colorHex = "#FFFFFF",
            bold = true,
            textAlign = "left"
        ).toMap()
        layers += DesignLayer(
            layerId = "prof_verified",
            type = LayerType.SHAPE,
            name = "Selo Verificado",
            xFraction = 0.54f,
            yFraction = 0.085f,
            widthFraction = 0.048f,
            heightFraction = 0.042f,
            colorHex = "#1D9BF0",
            shapeKind = ShapeKind.VERIFIED_BADGE
        ).toMap()

        val badgeY = if (hasPhoto) 0.48f else 0.28f
        val headlineY = if (hasPhoto) 0.66f else 0.50f
        val subY = if (hasPhoto) 0.85f else 0.74f

        layers += DesignLayer(
            layerId = "ai_badge_${UUID.randomUUID().toString().take(6)}",
            type = LayerType.SHAPE,
            name = "Etiqueta",
            xFraction = 0.5f,
            yFraction = badgeY,
            widthFraction = 0.54f,
            heightFraction = 0.085f,
            colorHex = accentColorHex,
            shapeKind = ShapeKind.BADGE
        ).toMap()
        layers += DesignLayer(
            layerId = "ai_badge_txt_${UUID.randomUUID().toString().take(6)}",
            type = LayerType.TEXT,
            name = "Texto Selo",
            xFraction = 0.5f,
            yFraction = badgeY,
            widthFraction = 0.50f,
            heightFraction = 0.065f,
            text = badgeText.uppercase(),
            fontFamilyKey = "plus_jakarta_sans",
            fontSizeSp = 13f,
            colorHex = "#FFFFFF",
            bold = true,
            letterSpacing = 1.6f
        ).toMap()
        layers += DesignLayer(
            layerId = "ai_headline_${UUID.randomUUID().toString().take(6)}",
            type = LayerType.TEXT,
            name = "Título Principal",
            xFraction = 0.5f,
            yFraction = headlineY,
            widthFraction = 0.86f,
            heightFraction = 0.24f,
            text = headline,
            fontFamilyKey = fontFamilyKey,
            fontSizeSp = 34f,
            colorHex = if (hasPhoto) "#FFFFFF" else textColorHex,
            bold = true,
            shadowEnabled = true
        ).toMap()
        layers += DesignLayer(
            layerId = "ai_sub_${UUID.randomUUID().toString().take(6)}",
            type = LayerType.TEXT,
            name = "Subtítulo",
            xFraction = 0.5f,
            yFraction = subY,
            widthFraction = 0.82f,
            heightFraction = 0.13f,
            text = subheadline,
            fontFamilyKey = "inter",
            fontSizeSp = 15f,
            colorHex = if (hasPhoto) "#F1F5F9" else textColorHex,
            opacity = 0.94f
        ).toMap()

        return layers
    }

    fun toEditableProject(userId: String): DesignProject {
        return DesignProject(
            userId = userId,
            title = title,
            category = category,
            formatName = formatName,
            width = width.toLong(),
            height = height.toLong(),
            backgroundColorHex = backgroundColorHex,
            backgroundGradientHex = backgroundGradientHex,
            layers = buildLayers(),
            videoClips = emptyList(),
            projectType = "design",
            thumbnailText = headline
        )
    }

    fun toDesignTemplate(isPremium: Boolean = false): DesignTemplate {
        return DesignTemplate(
            id = "ai_tpl_${UUID.randomUUID().toString().take(8)}",
            name = title,
            category = category,
            previewStyle = "ai_studio",
            width = width.toLong(),
            height = height.toLong(),
            backgroundColorHex = backgroundColorHex,
            backgroundGradientHex = backgroundGradientHex,
            layers = buildLayers(),
            fontsUsed = listOf(fontFamilyKey, "inter").distinct(),
            colorsUsed = listOf(backgroundColorHex, backgroundGradientHex, accentColorHex, textColorHex).distinct(),
            premium = isPremium,
            published = true
        )
    }
}

class AiStudioRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val structuredDesignModel by lazy {
        val designSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string("Short template or project title in Portuguese"),
                "category" to Schema.enumeration(
                    listOf(
                        "Instagram", "Facebook", "TikTok", "YouTube",
                        "Notícias", "Publicidade", "Promoções", "Negócios",
                        "Moda", "Eventos", "Restaurantes", "Tecnologia"
                    ),
                    "Best matching category"
                ),
                "formatName" to Schema.string("Design format name e.g. Instagram Quadrado or Cartaz"),
                "width" to Schema.integer("Canvas width in pixels, e.g. 1080 or 1280"),
                "height" to Schema.integer("Canvas height in pixels, e.g. 1080, 1350, 1920 or 720"),
                "backgroundColorHex" to Schema.string("Hex color like #0A1628"),
                "backgroundGradientHex" to Schema.string("Gradient end hex color like #FF7A00"),
                "headline" to Schema.string("Impactful headline in Portuguese (max 40 chars)"),
                "subheadline" to Schema.string("Supporting copy in Portuguese (max 90 chars)"),
                "badgeText" to Schema.string("Short badge label like ÚLTIMA HORA, NOVIDADE or 30% OFF"),
                "accentColorHex" to Schema.string("Accent hex color like #FF7A00"),
                "textColorHex" to Schema.string("High contrast text hex like #FFFFFF"),
                "fontFamilyKey" to Schema.enumeration(
                    listOf("plus_jakarta_sans", "oswald", "playfair_display", "montserrat", "pacifico", "inter"),
                    "Font family key"
                ),
                "photoTopic" to Schema.enumeration(
                    listOf("social", "news", "advertising", "business", "tech", "city", "fashion", "food", "none"),
                    "Real photography subject for the template background"
                ),
                "socialCaption" to Schema.string("Engaging social media caption with hashtags in Portuguese")
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = designSchema
            }
        )
    }

    private val imageGenModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun streamAssistantReply(prompt: String, conversationContext: String): Flow<String> = flow {
        val fullPrompt = """
            És o Assistente Criativo do CANVA AO, especialista em design gráfico, marketing digital,
            copywriting publicitário, paletas de cores, tipografia e edição de vídeos.
            Responde sempre em Português de forma clara, prática e criativa.
            Contexto anterior: $conversationContext
            Pedido do utilizador: $prompt
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatModel.generateContentStream(fullPrompt).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("O modelo não devolveu resposta.")
        }
    }

    suspend fun generateDesignProposal(prompt: String): Result<AiDesignProposal> {
        return try {
            val response = withTimeout(60_000L) {
                structuredDesignModel.generateContent(
                    "Gera um projeto gráfico completo e moderno em Português para o seguinte pedido: $prompt"
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Resposta vazia do modelo.")
            val json = JSONObject(rawJson)
            Result.success(
                AiDesignProposal(
                    title = json.optString("title", "Design IA"),
                    category = json.optString("category", "Publicidade"),
                    formatName = json.optString("formatName", "Instagram Vertical"),
                    width = json.optInt("width", 1080).coerceIn(400, 4000),
                    height = json.optInt("height", 1350).coerceIn(400, 4000),
                    backgroundColorHex = sanitizeHex(json.optString("backgroundColorHex", "#0A1628"), "#0A1628"),
                    backgroundGradientHex = sanitizeHex(json.optString("backgroundGradientHex", "#1E3A8A"), "#1E3A8A"),
                    headline = json.optString("headline", "SUA MARCA EM DESTAQUE"),
                    subheadline = json.optString("subheadline", "Qualidade profissional criada com CANVA AO."),
                    badgeText = json.optString("badgeText", "NOVIDADE"),
                    accentColorHex = sanitizeHex(json.optString("accentColorHex", "#FF7A00"), "#FF7A00"),
                    textColorHex = sanitizeHex(json.optString("textColorHex", "#FFFFFF"), "#FFFFFF"),
                    fontFamilyKey = json.optString("fontFamilyKey", "plus_jakarta_sans"),
                    socialCaption = json.optString("socialCaption", "#CANVAAO #Design"),
                    photoTopic = json.optString("photoTopic", "advertising")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Design proposal timed out", e)
            Result.failure(Exception("Tempo limite excedido (60s). Tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Design proposal failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateImageFromPrompt(prompt: String): Result<Bitmap> {
        return try {
            val response = withTimeout(60_000L) {
                imageGenModel.generateContent(prompt)
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
                ?: throw IllegalStateException("Nenhuma imagem foi devolvida pelo modelo.")
            Result.success(bitmap)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(Exception("A geração da imagem demorou mais de 60s. Tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(e)
        }
    }

    suspend fun editOrRemoveBackground(inputBitmap: Bitmap, instruction: String): Result<Bitmap> {
        return try {
            val response = withTimeout(60_000L) {
                imageGenModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(instruction)
                    }
                )
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
                ?: throw IllegalStateException("Não foi possível processar a imagem.")
            Result.success(bitmap)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image edit timed out", e)
            Result.failure(Exception("O processamento da imagem excedeu 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image edit failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateAutoCaptions(
        topicOrScript: String,
        totalDurationMs: Long,
        frameBitmap: Bitmap? = null
    ): Result<List<AiCaptionSegment>> {
        return try {
            val captionSchema = Schema.obj(
                properties = mapOf(
                    "segments" to Schema.array(
                        items = Schema.obj(
                            properties = mapOf(
                                "startMs" to Schema.integer("Segment start time in milliseconds >= 0"),
                                "endMs" to Schema.integer("Segment end time in milliseconds <= totalDurationMs"),
                                "text" to Schema.string("Short crisp subtitle line in Portuguese (max 42 chars)")
                            )
                        ),
                        description = "Chronological timed video subtitle segments"
                    )
                )
            )
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = captionSchema
                }
            )
            val safeDur = totalDurationMs.coerceAtLeast(3000L)
            val prompt = """
                Gera legendas cronológicas sincronizadas em Português para um vídeo de ${safeDur}ms.
                Tema / Guião / Contexto: ${topicOrScript.ifBlank { "Vídeo criativo profissional CANVA AO" }}
                Divide em 3 a 6 legendas curtas e impactantes cobrindo de 0ms até ${safeDur}ms sem sobreposições.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                if (frameBitmap != null) {
                    model.generateContent(content {
                        image(frameBitmap)
                        text(prompt)
                    })
                } else {
                    model.generateContent(prompt)
                }
            }
            val rawJson = response.text ?: throw IllegalStateException("Sem resposta da IA.")
            val root = JSONObject(rawJson)
            val arr = root.optJSONArray("segments") ?: org.json.JSONArray()
            val list = mutableListOf<AiCaptionSegment>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val s = item.optLong("startMs", i * (safeDur / arr.length().coerceAtLeast(1)))
                    .coerceIn(0L, safeDur - 400L)
                val e = item.optLong("endMs", s + 1800L).coerceIn(s + 400L, safeDur)
                val t = item.optString("text", "").trim()
                if (t.isNotBlank()) list.add(AiCaptionSegment(s, e, t))
            }
            if (list.isEmpty()) throw IllegalStateException("Não foram geradas legendas.")
            Result.success(list)
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Tempo limite excedido ao gerar legendas IA."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Auto captions failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeVideoFrameForProAi(
        frameBitmap: Bitmap,
        goal: String // "enhance", "track_object", "smart_slowmo"
    ): Result<AiVideoAnalysisResult> {
        return try {
            val schema = Schema.obj(
                properties = mapOf(
                    "brightness" to Schema.float("Brightness adjustment -0.25 to 0.25"),
                    "contrast" to Schema.float("Contrast multiplier 0.85 to 1.45"),
                    "saturation" to Schema.float("Saturation multiplier 0.7 to 1.5"),
                    "temperature" to Schema.float("Color temperature -0.5 to 0.5"),
                    "targetX" to Schema.float("Normalized horizontal focal subject center 0.1 to 0.9"),
                    "targetY" to Schema.float("Normalized vertical focal subject center 0.1 to 0.9"),
                    "recommendedSpeed" to Schema.float("Recommended playback speed 0.35 to 1.5"),
                    "speedCurve" to Schema.enumeration(
                        listOf("linear", "montage", "hero", "bullet", "jump_cut", "flash_in", "flash_out"),
                        "Best speed curve preset"
                    ),
                    "summary" to Schema.string("Short Portuguese description of detected scene & adjustment")
                )
            )
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                }
            )
            val instruction = when (goal) {
                "track_object" -> "Detect the main moving subject or focal person/object in this video frame. Return normalized targetX (0..1) and targetY (0..1) centered on the subject plus a short Portuguese summary."
                "smart_slowmo" -> "Analyze motion/action intensity in this video frame and recommend optimal slow-motion speed (0.35..0.85) and speedCurve ('hero' or 'bullet' or 'montage') plus summary in Portuguese."
                else -> "Analyze lighting, exposure, contrast and colors of this video frame to apply cinematic professional color enhancement (brightness, contrast, saturation, temperature) and explain briefly in Portuguese."
            }
            val response = withTimeout(45_000L) {
                model.generateContent(content {
                    image(frameBitmap)
                    text(instruction)
                })
            }
            val raw = response.text ?: throw IllegalStateException("Sem análise devolvida.")
            val json = JSONObject(raw)
            Result.success(
                AiVideoAnalysisResult(
                    brightness = json.optDouble("brightness", 0.05).toFloat().coerceIn(-0.35f, 0.35f),
                    contrast = json.optDouble("contrast", 1.15).toFloat().coerceIn(0.7f, 1.6f),
                    saturation = json.optDouble("saturation", 1.18).toFloat().coerceIn(0.5f, 1.7f),
                    temperature = json.optDouble("temperature", 0.08).toFloat().coerceIn(-0.6f, 0.6f),
                    targetX = json.optDouble("targetX", 0.5).toFloat().coerceIn(0.1f, 0.9f),
                    targetY = json.optDouble("targetY", 0.5).toFloat().coerceIn(0.1f, 0.9f),
                    recommendedSpeed = json.optDouble("recommendedSpeed", 0.5).toFloat().coerceIn(0.25f, 2.0f),
                    speedCurve = json.optString("speedCurve", "hero"),
                    summary = json.optString("summary", "Otimização aplicada com sucesso.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("A análise IA demorou demasiado. Tente novamente."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video AI analysis failed", e)
            Result.failure(e)
        }
    }

    private fun sanitizeHex(value: String, fallback: String): String {
        val trimmed = value.trim()
        return if (trimmed.matches(Regex("^#[0-9A-Fa-f]{6}$"))) trimmed else fallback
    }
}

data class AiCaptionSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class AiVideoAnalysisResult(
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val targetX: Float,
    val targetY: Float,
    val recommendedSpeed: Float,
    val speedCurve: String,
    val summary: String
)

