package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.core.model.DesignLayer
import com.example.core.model.DesignProject
import com.example.core.model.LayerType
import com.example.core.model.ShapeKind
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
    val socialCaption: String
) {
    fun toEditableProject(userId: String): DesignProject {
        val layers = listOf(
            DesignLayer(
                layerId = "ai_glow_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.SHAPE,
                name = "Destaque Visual",
                xFraction = 0.8f,
                yFraction = 0.2f,
                widthFraction = 0.48f,
                heightFraction = 0.38f,
                colorHex = accentColorHex,
                opacity = 0.28f,
                shapeKind = ShapeKind.CIRCLE
            ).toMap(),
            DesignLayer(
                layerId = "ai_badge_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.SHAPE,
                name = "Etiqueta",
                xFraction = 0.5f,
                yFraction = 0.24f,
                widthFraction = 0.56f,
                heightFraction = 0.09f,
                colorHex = accentColorHex,
                shapeKind = ShapeKind.BADGE
            ).toMap(),
            DesignLayer(
                layerId = "ai_badge_txt_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.TEXT,
                name = "Texto Selo",
                xFraction = 0.5f,
                yFraction = 0.24f,
                widthFraction = 0.52f,
                heightFraction = 0.07f,
                text = badgeText.uppercase(),
                fontFamilyKey = "plus_jakarta_sans",
                fontSizeSp = 14f,
                colorHex = "#FFFFFF",
                bold = true,
                letterSpacing = 1.6f
            ).toMap(),
            DesignLayer(
                layerId = "ai_headline_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.TEXT,
                name = "Título Principal",
                xFraction = 0.5f,
                yFraction = 0.48f,
                widthFraction = 0.86f,
                heightFraction = 0.26f,
                text = headline,
                fontFamilyKey = fontFamilyKey,
                fontSizeSp = 38f,
                colorHex = textColorHex,
                bold = true,
                shadowEnabled = true
            ).toMap(),
            DesignLayer(
                layerId = "ai_sub_${UUID.randomUUID().toString().take(6)}",
                type = LayerType.TEXT,
                name = "Subtítulo",
                xFraction = 0.5f,
                yFraction = 0.74f,
                widthFraction = 0.82f,
                heightFraction = 0.14f,
                text = subheadline,
                fontFamilyKey = "inter",
                fontSizeSp = 16f,
                colorHex = textColorHex,
                opacity = 0.9f
            ).toMap()
        )
        return DesignProject(
            userId = userId,
            title = title,
            category = category,
            formatName = formatName,
            width = width.toLong(),
            height = height.toLong(),
            backgroundColorHex = backgroundColorHex,
            backgroundGradientHex = backgroundGradientHex,
            layers = layers,
            videoClips = emptyList(),
            projectType = "design",
            thumbnailText = headline
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
                "title" to Schema.string("Short project title in Portuguese"),
                "category" to Schema.string("Category from: Instagram, Negócios, Moda, Promoções, Eventos, Restaurantes, Música, Tecnologia"),
                "formatName" to Schema.string("Design format name e.g. Instagram Quadrado or Cartaz"),
                "width" to Schema.integer("Canvas width in pixels, e.g. 1080"),
                "height" to Schema.integer("Canvas height in pixels, e.g. 1080 or 1350"),
                "backgroundColorHex" to Schema.string("Hex color like #0A1628"),
                "backgroundGradientHex" to Schema.string("Gradient end hex color like #FF7A00"),
                "headline" to Schema.string("Impactful headline in Portuguese (max 40 chars)"),
                "subheadline" to Schema.string("Supporting copy in Portuguese (max 90 chars)"),
                "badgeText" to Schema.string("Short badge label like NOVIDADE or 30% OFF"),
                "accentColorHex" to Schema.string("Accent hex color like #FF7A00"),
                "textColorHex" to Schema.string("High contrast text hex like #FFFFFF"),
                "fontFamilyKey" to Schema.enumeration(
                    listOf("plus_jakarta_sans", "oswald", "playfair_display", "montserrat", "pacifico", "inter"),
                    "Font family key"
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
                    socialCaption = json.optString("socialCaption", "#CANVAAO #Design")
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

    private fun sanitizeHex(value: String, fallback: String): String {
        val trimmed = value.trim()
        return if (trimmed.matches(Regex("^#[0-9A-Fa-f]{6}$"))) trimmed else fallback
    }
}
