package com.example.core.model

import androidx.annotation.DrawableRes
import com.example.R

object TemplateSeedCatalog {

    const val TOKEN_USER_AVATAR = "user://profile_avatar"
    const val TOKEN_USER_NAME = "{USER_NAME}"
    const val TOKEN_USER_HANDLE = "{USER_HANDLE}"

    val allCategories: List<String> = listOf(
        "Instagram",
        "Facebook",
        "TikTok",
        "YouTube",
        "Publicidade",
        "Negócios",
        "Notícias",
        "Futebol",
        "Música",
        "Eventos",
        "Aniversários",
        "Convites",
        "Restaurantes",
        "Moda",
        "Beleza",
        "Tecnologia",
        "Educação",
        "Promoções"
    )

    @DrawableRes
    fun resolveDrawableResId(uri: String?): Int? {
        return when (uri) {
            "res://tpl_art_luanda" -> R.drawable.tpl_art_luanda
            "res://tpl_art_sonhe" -> R.drawable.tpl_art_sonhe
            "res://tpl_art_blackfriday" -> R.drawable.tpl_art_blackfriday
            "res://tpl_art_suamarca" -> R.drawable.tpl_art_suamarca
            "res://tpl_art_aniversario" -> R.drawable.tpl_art_aniversario
            "res://tpl_art_youtube" -> R.drawable.tpl_art_youtube
            "res://tpl_art_futebol" -> R.drawable.tpl_art_futebol
            "res://tpl_art_musica" -> R.drawable.tpl_art_musica
            "res://tpl_art_restaurante" -> R.drawable.tpl_art_restaurante
            "res://tpl_art_moda" -> R.drawable.tpl_art_moda
            "res://tpl_art_tech" -> R.drawable.tpl_art_tech
            "res://tpl_art_noticias" -> R.drawable.tpl_art_noticias
            "res://tpl_user_cartoon" -> R.drawable.tpl_user_cartoon
            "res://tpl_user_bluecar" -> R.drawable.tpl_user_bluecar
            "res://tpl_user_portrait" -> R.drawable.tpl_user_portrait
            "res://tpl_user_blackfriday_bg" -> R.drawable.tpl_user_blackfriday_bg
            "res://ic_default_avatar" -> R.drawable.ic_default_avatar
            else -> null
        }
    }

    fun resolveLayersWithUserProfile(
        layers: List<Map<String, Any>>,
        avatarUrl: String?,
        displayName: String?,
        email: String?
    ): List<Map<String, Any>> {
        val safeAvatar = avatarUrl?.takeIf { it.isNotBlank() } ?: "res://ic_default_avatar"
        val safeName = displayName?.takeIf { it.isNotBlank() } ?: "Criador CANVA AO"
        val prefix = email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: safeName.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "criador" }
        val safeHandle = "@$prefix"

        return layers.map { raw ->
            val layer = DesignLayer.fromMap(raw)
            val updatedUri = if (layer.imageUri == TOKEN_USER_AVATAR) safeAvatar else layer.imageUri
            val updatedText = layer.text
                .replace(TOKEN_USER_NAME, safeName)
                .replace(TOKEN_USER_HANDLE, safeHandle)
            layer.copy(imageUri = updatedUri, text = updatedText).toMap()
        }
    }

    private fun compactVerifiedCreatorCornerLayers(
        isDarkText: Boolean = false
    ): List<Map<String, Any>> {
        val textHex = if (isDarkText) "#0A1628" else "#FFFFFF"
        return listOf(
            DesignLayer(
                layerId = "prof_avatar",
                type = LayerType.IMAGE,
                name = "Foto da Conta",
                xFraction = 0.12f,
                yFraction = 0.085f,
                widthFraction = 0.105f,
                heightFraction = 0.085f,
                imageUri = TOKEN_USER_AVATAR,
                maskKind = ImageMaskKind.CIRCLE
            ).toMap(),
            DesignLayer(
                layerId = "prof_handle",
                type = LayerType.TEXT,
                name = "Perfil Criador",
                xFraction = 0.35f,
                yFraction = 0.085f,
                widthFraction = 0.32f,
                heightFraction = 0.045f,
                text = TOKEN_USER_HANDLE,
                fontFamilyKey = "plus_jakarta_sans",
                fontSizeSp = 12f,
                colorHex = textHex,
                bold = true,
                textAlign = "left"
            ).toMap(),
            DesignLayer(
                layerId = "prof_verified",
                type = LayerType.SHAPE,
                name = "Selo Verificado",
                xFraction = 0.54f,
                yFraction = 0.085f,
                widthFraction = 0.042f,
                heightFraction = 0.036f,
                colorHex = "#1D9BF0",
                shapeKind = ShapeKind.VERIFIED_BADGE
            ).toMap()
        )
    }

    val builtInTemplates: List<DesignTemplate> = listOf(
        // 1. Dark Quote Card (Orange Line + Quotes + Dot Matrix + Verified Profile)
        DesignTemplate(
            id = "tpl_user_quote_dark",
            name = "Citação Dark Laranja",
            category = "Instagram",
            previewStyle = "quote_dark_orange",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#1A1A1A",
            backgroundGradientHex = "#1A1A1A",
            fontsUsed = listOf("Plus Jakarta Sans"),
            colorsUsed = listOf("#1A1A1A", "#FF9900", "#FFFFFF", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "q1_bar",
                    type = LayerType.SHAPE,
                    name = "Linha Vertical Laranja",
                    xFraction = 0.075f,
                    yFraction = 0.50f,
                    widthFraction = 0.011f,
                    heightFraction = 0.80f,
                    colorHex = "#FF9900",
                    cornerRadiusDp = 6f,
                    shapeKind = ShapeKind.ROUNDED_RECT
                ).toMap(),
                DesignLayer(
                    layerId = "q1_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.175f,
                    yFraction = 0.145f,
                    widthFraction = 0.095f,
                    heightFraction = 0.076f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "q1_name",
                    type = LayerType.TEXT,
                    name = "Nome da Conta",
                    xFraction = 0.42f,
                    yFraction = 0.130f,
                    widthFraction = 0.36f,
                    heightFraction = 0.045f,
                    text = TOKEN_USER_NAME,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 15f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "q1_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.63f,
                    yFraction = 0.130f,
                    widthFraction = 0.042f,
                    heightFraction = 0.034f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "q1_handle",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.42f,
                    yFraction = 0.163f,
                    widthFraction = 0.36f,
                    heightFraction = 0.038f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 12f,
                    colorHex = "#9CA3AF",
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "q1_quotes",
                    type = LayerType.SHAPE,
                    name = "Aspas Laranja",
                    xFraction = 0.165f,
                    yFraction = 0.405f,
                    widthFraction = 0.070f,
                    heightFraction = 0.056f,
                    colorHex = "#FF9900",
                    shapeKind = ShapeKind.QUOTE_MARKS
                ).toMap(),
                DesignLayer(
                    layerId = "q1_greeting",
                    type = LayerType.TEXT,
                    name = "Saudação",
                    xFraction = 0.45f,
                    yFraction = 0.505f,
                    widthFraction = 0.64f,
                    heightFraction = 0.08f,
                    text = "Olá",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 28f,
                    colorHex = "#FFFFFF",
                    bold = false,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "q1_body",
                    type = LayerType.TEXT,
                    name = "Texto Principal",
                    xFraction = 0.49f,
                    yFraction = 0.595f,
                    widthFraction = 0.72f,
                    heightFraction = 0.075f,
                    text = "Clica aqui para digitar o texto",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 20f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "q1_dots",
                    type = LayerType.SHAPE,
                    name = "Grelha de Pontos",
                    xFraction = 0.88f,
                    yFraction = 0.90f,
                    widthFraction = 0.13f,
                    heightFraction = 0.10f,
                    colorHex = "#FF9900",
                    opacity = 0.38f,
                    shapeKind = ShapeKind.DOT_GRID
                ).toMap()
            )
        ),

        // 2. Cartoon Meme Card (Illustration + Diamond Line + Avatar + Verified Badge + Bold Caption)
        DesignTemplate(
            id = "tpl_user_meme_cartoon",
            name = "Meme Cartoon Viral",
            category = "Instagram",
            previewStyle = "meme_cartoon",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#000000",
            backgroundGradientHex = "#000000",
            fontsUsed = listOf("Oswald", "Plus Jakarta Sans"),
            colorsUsed = listOf("#000000", "#FFFFFF", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "mc_art",
                    type = LayerType.IMAGE,
                    name = "Ilustração Cartoon",
                    xFraction = 0.50f,
                    yFraction = 0.35f,
                    widthFraction = 1.0f,
                    heightFraction = 0.70f,
                    imageUri = "res://tpl_user_cartoon"
                ).toMap(),
                DesignLayer(
                    layerId = "mc_scrim",
                    type = LayerType.SHAPE,
                    name = "Fusão Escura",
                    xFraction = 0.50f,
                    yFraction = 0.62f,
                    widthFraction = 1.0f,
                    heightFraction = 0.22f,
                    colorHex = "#000000",
                    shapeKind = ShapeKind.SCRIM_BOTTOM
                ).toMap(),
                DesignLayer(
                    layerId = "mc_line",
                    type = LayerType.SHAPE,
                    name = "Linha Divisória",
                    xFraction = 0.50f,
                    yFraction = 0.76f,
                    widthFraction = 0.88f,
                    heightFraction = 0.042f,
                    colorHex = "#FFFFFF",
                    shapeKind = ShapeKind.DIAMOND_LINE
                ).toMap(),
                DesignLayer(
                    layerId = "mc_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.50f,
                    yFraction = 0.752f,
                    widthFraction = 0.105f,
                    heightFraction = 0.084f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "mc_handle",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.70f,
                    yFraction = 0.728f,
                    widthFraction = 0.28f,
                    heightFraction = 0.04f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 12f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "mc_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.865f,
                    yFraction = 0.728f,
                    widthFraction = 0.038f,
                    heightFraction = 0.030f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "mc_caption",
                    type = LayerType.TEXT,
                    name = "Legenda Meme",
                    xFraction = 0.50f,
                    yFraction = 0.875f,
                    widthFraction = 0.90f,
                    heightFraction = 0.15f,
                    text = "HOJE NÃO VOU PODER IR CONTIGO\nVÁS EMBORA E NÃO",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 28f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap()
            )
        ),

        // 3. Purple-to-Blue Gradient + Smooth White Arch + Centered Avatar + Verified Badge
        DesignTemplate(
            id = "tpl_user_arco_gradiente",
            name = "Arco Roxo & Azul",
            category = "Facebook",
            previewStyle = "arch_violet_blue",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#2E0E66",
            backgroundGradientHex = "#2B84B8",
            fontsUsed = listOf("Oswald", "Plus Jakarta Sans"),
            colorsUsed = listOf("#2E0E66", "#2B84B8", "#FFFFFF", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "ar_arch",
                    type = LayerType.SHAPE,
                    name = "Arco Branco",
                    xFraction = 0.50f,
                    yFraction = 0.845f,
                    widthFraction = 1.0f,
                    heightFraction = 0.31f,
                    colorHex = "#FFFFFF",
                    shapeKind = ShapeKind.BOTTOM_ARCH
                ).toMap(),
                DesignLayer(
                    layerId = "ar_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.50f,
                    yFraction = 0.735f,
                    widthFraction = 0.18f,
                    heightFraction = 0.144f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "ar_handle",
                    type = LayerType.TEXT,
                    name = "Perfil Verificado",
                    xFraction = 0.47f,
                    yFraction = 0.825f,
                    widthFraction = 0.40f,
                    heightFraction = 0.04f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 13f,
                    colorHex = "#2E0E66",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "ar_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.70f,
                    yFraction = 0.825f,
                    widthFraction = 0.042f,
                    heightFraction = 0.034f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "ar_title",
                    type = LayerType.TEXT,
                    name = "Mensagem",
                    xFraction = 0.50f,
                    yFraction = 0.902f,
                    widthFraction = 0.86f,
                    heightFraction = 0.13f,
                    text = "OLHA MINHA HF QUE EU TE\nMANDEI",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 28f,
                    colorHex = "#2E2E38",
                    bold = true
                ).toMap()
            )
        ),

        // 4. Sky-Blue to Dark News Banner (Top-left Avatar + Verified + NEWS Pill + Yellow Accent)
        DesignTemplate(
            id = "tpl_user_news_azul",
            name = "Notícias Azul & Amarelo",
            category = "Notícias",
            previewStyle = "news_sky_dark",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#D8ECFF",
            backgroundGradientHex = "#99C8FA",
            fontsUsed = listOf("Oswald", "Montserrat"),
            colorsUsed = listOf("#D8ECFF", "#05070B", "#FACC15", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "nw4_scrim",
                    type = LayerType.SHAPE,
                    name = "Degradê Escuro",
                    xFraction = 0.50f,
                    yFraction = 0.74f,
                    widthFraction = 1.0f,
                    heightFraction = 0.54f,
                    colorHex = "#05070B",
                    shapeKind = ShapeKind.SCRIM_BOTTOM
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.145f,
                    yFraction = 0.115f,
                    widthFraction = 0.14f,
                    heightFraction = 0.112f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_name",
                    type = LayerType.TEXT,
                    name = "Nome da Conta",
                    xFraction = 0.405f,
                    yFraction = 0.102f,
                    widthFraction = 0.34f,
                    heightFraction = 0.04f,
                    text = TOKEN_USER_NAME,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 14f,
                    colorHex = "#0A1628",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.605f,
                    yFraction = 0.102f,
                    widthFraction = 0.042f,
                    heightFraction = 0.034f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_handle",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.405f,
                    yFraction = 0.132f,
                    widthFraction = 0.34f,
                    heightFraction = 0.035f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 12f,
                    colorHex = "#1E293B",
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_pill_bg",
                    type = LayerType.SHAPE,
                    name = "Etiqueta NEWS",
                    xFraction = 0.50f,
                    yFraction = 0.715f,
                    widthFraction = 0.145f,
                    heightFraction = 0.046f,
                    colorHex = "#FFFFFF",
                    cornerRadiusDp = 12f,
                    shapeKind = ShapeKind.ROUNDED_RECT
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_pill_txt",
                    type = LayerType.TEXT,
                    name = "NEWS",
                    xFraction = 0.50f,
                    yFraction = 0.715f,
                    widthFraction = 0.14f,
                    heightFraction = 0.04f,
                    text = "NEWS",
                    fontFamilyKey = "montserrat",
                    fontSizeSp = 12f,
                    colorHex = "#05070B",
                    bold = true,
                    letterSpacing = 1.4f
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_headline",
                    type = LayerType.TEXT,
                    name = "Legenda",
                    xFraction = 0.50f,
                    yFraction = 0.790f,
                    widthFraction = 0.88f,
                    heightFraction = 0.08f,
                    text = "Clica aqui para digitar a tua legenda",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 26f,
                    colorHex = "#FACC15",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_bar",
                    type = LayerType.SHAPE,
                    name = "Barra Amarela",
                    xFraction = 0.50f,
                    yFraction = 0.845f,
                    widthFraction = 0.135f,
                    heightFraction = 0.008f,
                    colorHex = "#FACC15",
                    cornerRadiusDp = 6f,
                    shapeKind = ShapeKind.ROUNDED_RECT
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_cta_ring",
                    type = LayerType.SHAPE,
                    name = "Botão Contorno",
                    xFraction = 0.50f,
                    yFraction = 0.903f,
                    widthFraction = 0.58f,
                    heightFraction = 0.062f,
                    colorHex = "#FACC15",
                    shapeKind = ShapeKind.PILL_OUTLINE
                ).toMap(),
                DesignLayer(
                    layerId = "nw4_cta_txt",
                    type = LayerType.TEXT,
                    name = "Texto Botão",
                    xFraction = 0.50f,
                    yFraction = 0.903f,
                    widthFraction = 0.54f,
                    heightFraction = 0.045f,
                    text = "CLICA AQUI PARA DIGITAR O TEXTO",
                    fontFamilyKey = "montserrat",
                    fontSizeSp = 11f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap()
            )
        ),

        // 5. Crisp White Social Card (Verified Header + Caption + Rounded Car Photo)
        DesignTemplate(
            id = "tpl_user_social_branco",
            name = "Post Social Verificado",
            category = "Instagram",
            previewStyle = "social_white_verified",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#FFFFFF",
            backgroundGradientHex = "#FFFFFF",
            fontsUsed = listOf("Plus Jakarta Sans", "Inter"),
            colorsUsed = listOf("#FFFFFF", "#111827", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "sc5_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.095f,
                    yFraction = 0.155f,
                    widthFraction = 0.115f,
                    heightFraction = 0.092f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "sc5_name",
                    type = LayerType.TEXT,
                    name = "Nome da Conta",
                    xFraction = 0.355f,
                    yFraction = 0.138f,
                    widthFraction = 0.38f,
                    heightFraction = 0.042f,
                    text = TOKEN_USER_NAME,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 17f,
                    colorHex = "#111827",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "sc5_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.565f,
                    yFraction = 0.138f,
                    widthFraction = 0.042f,
                    heightFraction = 0.034f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "sc5_handle",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.355f,
                    yFraction = 0.174f,
                    widthFraction = 0.38f,
                    heightFraction = 0.036f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 13f,
                    colorHex = "#6B7280",
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "sc5_dots",
                    type = LayerType.TEXT,
                    name = "Menu",
                    xFraction = 0.92f,
                    yFraction = 0.125f,
                    widthFraction = 0.10f,
                    heightFraction = 0.04f,
                    text = "•••",
                    fontFamilyKey = "inter",
                    fontSizeSp = 16f,
                    colorHex = "#4B5563",
                    bold = true,
                    textAlign = "right"
                ).toMap(),
                DesignLayer(
                    layerId = "sc5_caption",
                    type = LayerType.TEXT,
                    name = "Legenda",
                    xFraction = 0.30f,
                    yFraction = 0.252f,
                    widthFraction = 0.52f,
                    heightFraction = 0.055f,
                    text = "Digita aqui",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 22f,
                    colorHex = "#111827",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "sc5_photo",
                    type = LayerType.IMAGE,
                    name = "Foto do Post",
                    xFraction = 0.50f,
                    yFraction = 0.592f,
                    widthFraction = 0.925f,
                    heightFraction = 0.595f,
                    imageUri = "res://tpl_user_bluecar",
                    maskKind = ImageMaskKind.ROUNDED
                ).toMap()
            )
        ),

        // 6. Deep Purple Dark Frame + Orange Open Bracket + Ring Avatar + Verified Badge
        DesignTemplate(
            id = "tpl_user_moldura_laranja",
            name = "Moldura Laranja Dark",
            category = "TikTok",
            previewStyle = "dark_orange_bracket",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#26054D",
            backgroundGradientHex = "#040208",
            fontsUsed = listOf("Plus Jakarta Sans"),
            colorsUsed = listOf("#26054D", "#FF9900", "#FFFFFF", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "ml_bracket",
                    type = LayerType.SHAPE,
                    name = "Chaveta Laranja",
                    xFraction = 0.50f,
                    yFraction = 0.892f,
                    widthFraction = 0.905f,
                    heightFraction = 0.138f,
                    colorHex = "#FF9900",
                    shapeKind = ShapeKind.BRACKET_FRAME
                ).toMap(),
                DesignLayer(
                    layerId = "ml_ring",
                    type = LayerType.SHAPE,
                    name = "Anel Laranja",
                    xFraction = 0.50f,
                    yFraction = 0.822f,
                    widthFraction = 0.118f,
                    heightFraction = 0.094f,
                    colorHex = "#FF9900",
                    shapeKind = ShapeKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "ml_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.50f,
                    yFraction = 0.822f,
                    widthFraction = 0.102f,
                    heightFraction = 0.082f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "ml_ola",
                    type = LayerType.TEXT,
                    name = "Saudação",
                    xFraction = 0.47f,
                    yFraction = 0.896f,
                    widthFraction = 0.30f,
                    heightFraction = 0.05f,
                    text = "Ola",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 21f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "ml_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.58f,
                    yFraction = 0.896f,
                    widthFraction = 0.042f,
                    heightFraction = 0.034f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "ml_handle",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.50f,
                    yFraction = 0.935f,
                    widthFraction = 0.56f,
                    heightFraction = 0.036f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 12f,
                    colorHex = "#E2E8F0"
                ).toMap()
            )
        ),

        // 7. Editorial Split 45/55 (Left Portrait Photo + Right Black Quote + Verified Profile)
        DesignTemplate(
            id = "tpl_user_editorial_split",
            name = "Editorial Split Retrato",
            category = "Moda",
            previewStyle = "editorial_split",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#000000",
            backgroundGradientHex = "#000000",
            fontsUsed = listOf("Playfair Display", "Plus Jakarta Sans"),
            colorsUsed = listOf("#000000", "#FFFFFF", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "es_photo",
                    type = LayerType.IMAGE,
                    name = "Foto Editorial",
                    xFraction = 0.225f,
                    yFraction = 0.50f,
                    widthFraction = 0.45f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_user_portrait"
                ).toMap(),
                DesignLayer(
                    layerId = "es_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.545f,
                    yFraction = 0.12f,
                    widthFraction = 0.095f,
                    heightFraction = 0.076f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "es_name",
                    type = LayerType.TEXT,
                    name = "Nome da Conta",
                    xFraction = 0.74f,
                    yFraction = 0.108f,
                    widthFraction = 0.26f,
                    heightFraction = 0.04f,
                    text = TOKEN_USER_NAME,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 12f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "es_verified_top",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.91f,
                    yFraction = 0.108f,
                    widthFraction = 0.040f,
                    heightFraction = 0.032f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "es_handle_top",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.74f,
                    yFraction = 0.138f,
                    widthFraction = 0.26f,
                    heightFraction = 0.034f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 10f,
                    colorHex = "#9CA3AF",
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "es_quotes",
                    type = LayerType.SHAPE,
                    name = "Aspas Brancas",
                    xFraction = 0.535f,
                    yFraction = 0.352f,
                    widthFraction = 0.072f,
                    heightFraction = 0.056f,
                    colorHex = "#FFFFFF",
                    shapeKind = ShapeKind.QUOTE_MARKS
                ).toMap(),
                DesignLayer(
                    layerId = "es_headline",
                    type = LayerType.TEXT,
                    name = "Título Editorial",
                    xFraction = 0.725f,
                    yFraction = 0.495f,
                    widthFraction = 0.44f,
                    heightFraction = 0.18f,
                    text = "Clica aqui para\ndigitar a tua\nlegenda",
                    fontFamilyKey = "playfair_display",
                    fontSizeSp = 24f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "es_sub",
                    type = LayerType.TEXT,
                    name = "Subtítulo",
                    xFraction = 0.725f,
                    yFraction = 0.640f,
                    widthFraction = 0.44f,
                    heightFraction = 0.08f,
                    text = "Clica aqui para digitar o\ntexto",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 15f,
                    colorHex = "#E5E7EB",
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "es_handle_bot",
                    type = LayerType.TEXT,
                    name = "Assinatura",
                    xFraction = 0.78f,
                    yFraction = 0.955f,
                    widthFraction = 0.28f,
                    heightFraction = 0.04f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 12f,
                    colorHex = "#9CA3AF",
                    textAlign = "right"
                ).toMap(),
                DesignLayer(
                    layerId = "es_verified_bot",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado Rodapé",
                    xFraction = 0.948f,
                    yFraction = 0.955f,
                    widthFraction = 0.036f,
                    heightFraction = 0.029f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap()
            )
        ),

        // 8. 3D Neon Ring Black Friday + Orange Pill + Verified Creator Header
        DesignTemplate(
            id = "tpl_user_black_friday_neon",
            name = "Black Friday Neon 3D",
            category = "Promoções",
            previewStyle = "bf_neon_3d",
            width = 1080L,
            height = 1080L,
            backgroundColorHex = "#090C12",
            backgroundGradientHex = "#090C12",
            fontsUsed = listOf("Oswald", "Plus Jakarta Sans"),
            colorsUsed = listOf("#090C12", "#FF7A00", "#FFFFFF", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "bf8_bg",
                    type = LayerType.IMAGE,
                    name = "Pódio Neon 3D",
                    xFraction = 0.50f,
                    yFraction = 0.50f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_user_blackfriday_bg"
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.10f,
                    yFraction = 0.095f,
                    widthFraction = 0.095f,
                    heightFraction = 0.095f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_name",
                    type = LayerType.TEXT,
                    name = "Nome da Conta",
                    xFraction = 0.305f,
                    yFraction = 0.080f,
                    widthFraction = 0.29f,
                    heightFraction = 0.042f,
                    text = TOKEN_USER_NAME,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 13f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.485f,
                    yFraction = 0.080f,
                    widthFraction = 0.042f,
                    heightFraction = 0.042f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_handle",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.305f,
                    yFraction = 0.114f,
                    widthFraction = 0.29f,
                    heightFraction = 0.036f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 11f,
                    colorHex = "#CBD5E1",
                    textAlign = "left"
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_pill",
                    type = LayerType.SHAPE,
                    name = "Pílula Oferta",
                    xFraction = 0.50f,
                    yFraction = 0.220f,
                    widthFraction = 0.44f,
                    heightFraction = 0.082f,
                    colorHex = "#FF7A00",
                    shapeKind = ShapeKind.BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_pill_txt",
                    type = LayerType.TEXT,
                    name = "Oferta Para Ti",
                    xFraction = 0.50f,
                    yFraction = 0.220f,
                    widthFraction = 0.40f,
                    heightFraction = 0.055f,
                    text = "OFERTA Para ti",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 15f,
                    colorHex = "#FFFFFF"
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_title",
                    type = LayerType.TEXT,
                    name = "BLACK FRIDAY",
                    xFraction = 0.50f,
                    yFraction = 0.455f,
                    widthFraction = 0.46f,
                    heightFraction = 0.24f,
                    text = "BLACK\nFRIDAY",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 44f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_off",
                    type = LayerType.TEXT,
                    name = "50% OFF",
                    xFraction = 0.50f,
                    yFraction = 0.665f,
                    widthFraction = 0.46f,
                    heightFraction = 0.09f,
                    text = "50% OFF",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 30f,
                    colorHex = "#FF7A00",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "bf8_footer",
                    type = LayerType.TEXT,
                    name = "Seu estilo",
                    xFraction = 0.50f,
                    yFraction = 0.868f,
                    widthFraction = 0.48f,
                    heightFraction = 0.08f,
                    text = "Seu estilo",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 24f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap()
            )
        ),

        // 9. Violet-Teal-Dark Gradient + Top-Right Verified Profile + OK + Orange Accent
        DesignTemplate(
            id = "tpl_user_roxo_azul_ok",
            name = "Destaque Roxo & Laranja",
            category = "Eventos",
            previewStyle = "violet_teal_ok",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#2E0E66",
            backgroundGradientHex = "#24799C",
            fontsUsed = listOf("Oswald", "Montserrat"),
            colorsUsed = listOf("#2E0E66", "#24799C", "#FF9900", "#1D9BF0"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "ok9_scrim",
                    type = LayerType.SHAPE,
                    name = "Degradê Escuro",
                    xFraction = 0.50f,
                    yFraction = 0.84f,
                    widthFraction = 1.0f,
                    heightFraction = 0.36f,
                    colorHex = "#070A0F",
                    shapeKind = ShapeKind.SCRIM_BOTTOM
                ).toMap(),
                DesignLayer(
                    layerId = "ok9_name",
                    type = LayerType.TEXT,
                    name = "Nome da Conta",
                    xFraction = 0.59f,
                    yFraction = 0.092f,
                    widthFraction = 0.34f,
                    heightFraction = 0.04f,
                    text = TOKEN_USER_NAME,
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 13f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    textAlign = "right"
                ).toMap(),
                DesignLayer(
                    layerId = "ok9_handle",
                    type = LayerType.TEXT,
                    name = "Utilizador",
                    xFraction = 0.59f,
                    yFraction = 0.122f,
                    widthFraction = 0.34f,
                    heightFraction = 0.035f,
                    text = TOKEN_USER_HANDLE,
                    fontFamilyKey = "inter",
                    fontSizeSp = 11f,
                    colorHex = "#E2E8F0",
                    textAlign = "right"
                ).toMap(),
                DesignLayer(
                    layerId = "ok9_verified",
                    type = LayerType.SHAPE,
                    name = "Selo Verificado",
                    xFraction = 0.785f,
                    yFraction = 0.092f,
                    widthFraction = 0.040f,
                    heightFraction = 0.032f,
                    colorHex = "#1D9BF0",
                    shapeKind = ShapeKind.VERIFIED_BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "ok9_avatar",
                    type = LayerType.IMAGE,
                    name = "Foto da Conta",
                    xFraction = 0.880f,
                    yFraction = 0.105f,
                    widthFraction = 0.145f,
                    heightFraction = 0.116f,
                    imageUri = TOKEN_USER_AVATAR,
                    maskKind = ImageMaskKind.CIRCLE
                ).toMap(),
                DesignLayer(
                    layerId = "ok9_tag",
                    type = LayerType.TEXT,
                    name = "OK",
                    xFraction = 0.50f,
                    yFraction = 0.815f,
                    widthFraction = 0.22f,
                    heightFraction = 0.042f,
                    text = "OK",
                    fontFamilyKey = "montserrat",
                    fontSizeSp = 14f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "ok9_bar",
                    type = LayerType.SHAPE,
                    name = "Barra Laranja",
                    xFraction = 0.50f,
                    yFraction = 0.846f,
                    widthFraction = 0.115f,
                    heightFraction = 0.008f,
                    colorHex = "#FF9900",
                    cornerRadiusDp = 6f,
                    shapeKind = ShapeKind.ROUNDED_RECT
                ).toMap(),
                DesignLayer(
                    layerId = "ok9_headline",
                    type = LayerType.TEXT,
                    name = "Legenda",
                    xFraction = 0.50f,
                    yFraction = 0.912f,
                    widthFraction = 0.90f,
                    heightFraction = 0.115f,
                    text = "CLICA AQUI PARA DIGITAR A TUA\nLEGENDA",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 24f,
                    colorHex = "#FF9900",
                    bold = true
                ).toMap()
            )
        ),

        DesignTemplate(
            id = "tpl_sonhe_realize",
            name = "Sonhe & Realize",
            category = "Instagram",
            previewStyle = "sunset_inspire",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#3B1D60",
            backgroundGradientHex = "#FF7A00",
            fontsUsed = listOf("Oswald", "Plus Jakarta Sans"),
            colorsUsed = listOf("#3B1D60", "#FF7A00", "#FFFFFF"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "l_photo",
                    type = LayerType.IMAGE,
                    name = "Imagem Horizonte",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_sonhe"
                ).toMap(),
                DesignLayer(
                    layerId = "l_badge",
                    type = LayerType.SHAPE,
                    name = "Faixa Superior",
                    xFraction = 0.5f,
                    yFraction = 0.17f,
                    widthFraction = 0.54f,
                    heightFraction = 0.065f,
                    colorHex = "#FFFFFF",
                    opacity = 0.24f,
                    shapeKind = ShapeKind.BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "l_kicker",
                    type = LayerType.TEXT,
                    name = "Etiqueta",
                    xFraction = 0.5f,
                    yFraction = 0.17f,
                    widthFraction = 0.52f,
                    heightFraction = 0.06f,
                    text = "MENTALIDADE DIÁRIA",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 13f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    letterSpacing = 2.2f
                ).toMap(),
                DesignLayer(
                    layerId = "l_title",
                    type = LayerType.TEXT,
                    name = "Título Principal",
                    xFraction = 0.5f,
                    yFraction = 0.38f,
                    widthFraction = 0.85f,
                    heightFraction = 0.26f,
                    text = "SONHE\nREALIZE",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 46f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    italic = true,
                    shadowEnabled = true
                ).toMap(),
                DesignLayer(
                    layerId = "l_sub",
                    type = LayerType.TEXT,
                    name = "Subtítulo",
                    xFraction = 0.5f,
                    yFraction = 0.88f,
                    widthFraction = 0.82f,
                    heightFraction = 0.08f,
                    text = "Transforme ideias em resultados reais.",
                    fontFamilyKey = "inter",
                    fontSizeSp = 15f,
                    colorHex = "#FFE8CC",
                    bold = true
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_black_friday",
            name = "Black Friday 50% OFF",
            category = "Promoções",
            previewStyle = "promo_dark",
            width = 1080L,
            height = 1080L,
            backgroundColorHex = "#0A101D",
            backgroundGradientHex = "#1A2942",
            fontsUsed = listOf("Oswald", "Montserrat"),
            colorsUsed = listOf("#0A101D", "#FF7A00", "#FFFFFF"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "bf_photo",
                    type = LayerType.IMAGE,
                    name = "Cenário Oferta",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_blackfriday"
                ).toMap(),
                DesignLayer(
                    layerId = "bf_title",
                    type = LayerType.TEXT,
                    name = "Título Black Friday",
                    xFraction = 0.5f,
                    yFraction = 0.32f,
                    widthFraction = 0.88f,
                    heightFraction = 0.28f,
                    text = "BLACK\nFRIDAY",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 48f,
                    colorHex = "#FF9500",
                    bold = true,
                    italic = true,
                    shadowEnabled = true
                ).toMap(),
                DesignLayer(
                    layerId = "bf_pill",
                    type = LayerType.SHAPE,
                    name = "Etiqueta Desconto",
                    xFraction = 0.5f,
                    yFraction = 0.64f,
                    widthFraction = 0.58f,
                    heightFraction = 0.13f,
                    colorHex = "#FF7A00",
                    shapeKind = ShapeKind.BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "bf_off",
                    type = LayerType.TEXT,
                    name = "Desconto",
                    xFraction = 0.5f,
                    yFraction = 0.64f,
                    widthFraction = 0.56f,
                    heightFraction = 0.1f,
                    text = "50% OFF",
                    fontFamilyKey = "montserrat",
                    fontSizeSp = 28f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "bf_cta",
                    type = LayerType.TEXT,
                    name = "Chamada",
                    xFraction = 0.5f,
                    yFraction = 0.85f,
                    widthFraction = 0.82f,
                    heightFraction = 0.08f,
                    text = "SÓ ESTA SEMANA • APROVEITE AGORA",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 13f,
                    colorHex = "#E2E8F0",
                    bold = true,
                    letterSpacing = 1.5f
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_sua_marca",
            name = "Sua Marca Aqui",
            category = "Negócios",
            previewStyle = "brand_clean",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#F8FAFC",
            backgroundGradientHex = "#E2E8F0",
            fontsUsed = listOf("Oswald", "Plus Jakarta Sans"),
            colorsUsed = listOf("#0A1628", "#FF7A00", "#F8FAFC"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "sm_photo",
                    type = LayerType.IMAGE,
                    name = "Foto Corporativa",
                    xFraction = 0.5f,
                    yFraction = 0.72f,
                    widthFraction = 1.0f,
                    heightFraction = 0.58f,
                    imageUri = "res://tpl_art_suamarca"
                ).toMap(),
                DesignLayer(
                    layerId = "sm_stripe",
                    type = LayerType.SHAPE,
                    name = "Destaque Laranja",
                    xFraction = 0.5f,
                    yFraction = 0.44f,
                    widthFraction = 0.62f,
                    heightFraction = 0.045f,
                    colorHex = "#FF7A00",
                    rotation = -3f,
                    shapeKind = ShapeKind.ROUNDED_RECT
                ).toMap(),
                DesignLayer(
                    layerId = "sm_title",
                    type = LayerType.TEXT,
                    name = "Sua Marca",
                    xFraction = 0.5f,
                    yFraction = 0.25f,
                    widthFraction = 0.86f,
                    heightFraction = 0.34f,
                    text = "SUA\nMARCA\nAQUI",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 44f,
                    colorHex = "#0A1628",
                    bold = true,
                    italic = true
                ).toMap(),
                DesignLayer(
                    layerId = "sm_slogan",
                    type = LayerType.TEXT,
                    name = "Slogan",
                    xFraction = 0.5f,
                    yFraction = 0.52f,
                    widthFraction = 0.85f,
                    heightFraction = 0.08f,
                    text = "Inove. Conquiste. Cresça.",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 18f,
                    colorHex = "#0A1628",
                    bold = true,
                    italic = true
                ).toMap(),
                DesignLayer(
                    layerId = "sm_footer",
                    type = LayerType.TEXT,
                    name = "Contacto",
                    xFraction = 0.5f,
                    yFraction = 0.91f,
                    widthFraction = 0.88f,
                    heightFraction = 0.08f,
                    text = "WWW.SUAMARCA.AO • @SUAMARCA",
                    fontFamilyKey = "inter",
                    fontSizeSp = 13f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    letterSpacing = 1.8f
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers(isDarkText = true)
        ),
        DesignTemplate(
            id = "tpl_luanda_city",
            name = "Luanda Minha Cidade",
            category = "Eventos",
            previewStyle = "luanda_sunset",
            width = 1080L,
            height = 1080L,
            backgroundColorHex = "#0F2242",
            backgroundGradientHex = "#EA580C",
            fontsUsed = listOf("Oswald", "Pacifico"),
            colorsUsed = listOf("#0F2242", "#EA580C", "#FFFFFF"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "ld_photo",
                    type = LayerType.IMAGE,
                    name = "Baía de Luanda",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_luanda"
                ).toMap(),
                DesignLayer(
                    layerId = "ld_scrim",
                    type = LayerType.SHAPE,
                    name = "Contraste Central",
                    xFraction = 0.5f,
                    yFraction = 0.65f,
                    widthFraction = 0.92f,
                    heightFraction = 0.34f,
                    colorHex = "#07101F",
                    opacity = 0.45f,
                    shapeKind = ShapeKind.ROUNDED_RECT
                ).toMap(),
                DesignLayer(
                    layerId = "ld_title",
                    type = LayerType.TEXT,
                    name = "LUANDA",
                    xFraction = 0.5f,
                    yFraction = 0.58f,
                    widthFraction = 0.88f,
                    heightFraction = 0.18f,
                    text = "LUANDA",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 52f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    italic = true,
                    shadowEnabled = true
                ).toMap(),
                DesignLayer(
                    layerId = "ld_sub",
                    type = LayerType.TEXT,
                    name = "Minha Cidade",
                    xFraction = 0.5f,
                    yFraction = 0.74f,
                    widthFraction = 0.78f,
                    heightFraction = 0.12f,
                    text = "Minha Cidade",
                    fontFamilyKey = "pacifico",
                    fontSizeSp = 28f,
                    colorHex = "#FF9500",
                    shadowEnabled = true
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_aniversario",
            name = "Feliz Aniversário Luxo",
            category = "Aniversários",
            previewStyle = "birthday_gold",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#FFF8F1",
            backgroundGradientHex = "#FDE68A",
            fontsUsed = listOf("Pacifico", "Playfair Display"),
            colorsUsed = listOf("#7C2D12", "#D97706", "#FFF8F1"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "an_photo",
                    type = LayerType.IMAGE,
                    name = "Balões Festivos",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_aniversario"
                ).toMap(),
                DesignLayer(
                    layerId = "an_frame",
                    type = LayerType.SHAPE,
                    name = "Moldura Dourada",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 0.84f,
                    heightFraction = 0.84f,
                    colorHex = "#D97706",
                    opacity = 0.3f,
                    shapeKind = ShapeKind.FRAME
                ).toMap(),
                DesignLayer(
                    layerId = "an_title",
                    type = LayerType.TEXT,
                    name = "Parabéns",
                    xFraction = 0.5f,
                    yFraction = 0.44f,
                    widthFraction = 0.84f,
                    heightFraction = 0.25f,
                    text = "Feliz\nAniversário",
                    fontFamilyKey = "pacifico",
                    fontSizeSp = 40f,
                    colorHex = "#9A3412"
                ).toMap(),
                DesignLayer(
                    layerId = "an_sub",
                    type = LayerType.TEXT,
                    name = "Mensagem",
                    xFraction = 0.5f,
                    yFraction = 0.7f,
                    widthFraction = 0.78f,
                    heightFraction = 0.12f,
                    text = "Que este novo ano traga saúde, alegria e grandes conquistas!",
                    fontFamilyKey = "playfair_display",
                    fontSizeSp = 17f,
                    colorHex = "#451A03",
                    bold = true
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers(isDarkText = true)
        ),
        DesignTemplate(
            id = "tpl_youtube_gamer",
            name = "Miniatura YouTube Viral",
            category = "YouTube",
            previewStyle = "yt_viral",
            width = 1280L,
            height = 720L,
            backgroundColorHex = "#1E1B4B",
            backgroundGradientHex = "#E11D48",
            fontsUsed = listOf("Oswald", "Montserrat"),
            colorsUsed = listOf("#1E1B4B", "#FACC15", "#FFFFFF"),
            premium = true,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "yt_photo",
                    type = LayerType.IMAGE,
                    name = "Estúdio Criador",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_youtube"
                ).toMap(),
                DesignLayer(
                    layerId = "yt_badge",
                    type = LayerType.SHAPE,
                    name = "Faixa Amarela",
                    xFraction = 0.34f,
                    yFraction = 0.54f,
                    widthFraction = 0.56f,
                    heightFraction = 0.36f,
                    colorHex = "#FACC15",
                    rotation = -4f,
                    shapeKind = ShapeKind.ROUNDED_RECT
                ).toMap(),
                DesignLayer(
                    layerId = "yt_title",
                    type = LayerType.TEXT,
                    name = "Texto Viral",
                    xFraction = 0.34f,
                    yFraction = 0.54f,
                    widthFraction = 0.54f,
                    heightFraction = 0.32f,
                    text = "COMO CRIAR\nNO TELEMÓVEL",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 36f,
                    colorHex = "#0A1628",
                    bold = true,
                    italic = true
                ).toMap(),
                DesignLayer(
                    layerId = "yt_tag",
                    type = LayerType.TEXT,
                    name = "Tag",
                    xFraction = 0.34f,
                    yFraction = 0.22f,
                    widthFraction = 0.45f,
                    heightFraction = 0.14f,
                    text = "PASSO A PASSO",
                    fontFamilyKey = "montserrat",
                    fontSizeSp = 17f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    outlineEnabled = true,
                    outlineColorHex = "#000000"
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_futebol_matchday",
            name = "Dia de Jogo Futebol",
            category = "Futebol",
            previewStyle = "matchday",
            width = 1080L,
            height = 1080L,
            backgroundColorHex = "#064E3B",
            backgroundGradientHex = "#022C22",
            fontsUsed = listOf("Oswald", "Plus Jakarta Sans"),
            colorsUsed = listOf("#064E3B", "#34D399", "#FFFFFF"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "fb_photo",
                    type = LayerType.IMAGE,
                    name = "Estádio Noite",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_futebol"
                ).toMap(),
                DesignLayer(
                    layerId = "fb_kicker",
                    type = LayerType.TEXT,
                    name = "Campeonato",
                    xFraction = 0.5f,
                    yFraction = 0.22f,
                    widthFraction = 0.8f,
                    heightFraction = 0.08f,
                    text = "GIRABOLA • JORNADA 14",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 15f,
                    colorHex = "#6EE7B7",
                    bold = true,
                    letterSpacing = 2f
                ).toMap(),
                DesignLayer(
                    layerId = "fb_title",
                    type = LayerType.TEXT,
                    name = "Confronto",
                    xFraction = 0.5f,
                    yFraction = 0.48f,
                    widthFraction = 0.85f,
                    heightFraction = 0.26f,
                    text = "DIA DE JOGO\n20:00",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 48f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    shadowEnabled = true
                ).toMap(),
                DesignLayer(
                    layerId = "fb_stadium",
                    type = LayerType.TEXT,
                    name = "Estádio",
                    xFraction = 0.5f,
                    yFraction = 0.8f,
                    widthFraction = 0.82f,
                    heightFraction = 0.08f,
                    text = "ESTÁDIO 11 DE NOVEMBRO",
                    fontFamilyKey = "montserrat",
                    fontSizeSp = 14f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_musica_show",
            name = "Show & Festival Ao Vivo",
            category = "Música",
            previewStyle = "music_neon",
            width = 1080L,
            height = 1350L,
            backgroundColorHex = "#090D16",
            backgroundGradientHex = "#7C3AED",
            fontsUsed = listOf("Montserrat", "Oswald"),
            colorsUsed = listOf("#7C3AED", "#EC4899", "#FFFFFF"),
            premium = true,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "mu_photo",
                    type = LayerType.IMAGE,
                    name = "Palco Festival",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_musica"
                ).toMap(),
                DesignLayer(
                    layerId = "mu_title",
                    type = LayerType.TEXT,
                    name = "Festival",
                    xFraction = 0.5f,
                    yFraction = 0.36f,
                    widthFraction = 0.86f,
                    heightFraction = 0.24f,
                    text = "NOITE\nAFROBEAT",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 46f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    shadowEnabled = true
                ).toMap(),
                DesignLayer(
                    layerId = "mu_date",
                    type = LayerType.TEXT,
                    name = "Data",
                    xFraction = 0.5f,
                    yFraction = 0.58f,
                    widthFraction = 0.82f,
                    heightFraction = 0.1f,
                    text = "SÁBADO • 22H • BAÍA DE LUANDA",
                    fontFamilyKey = "montserrat",
                    fontSizeSp = 14f,
                    colorHex = "#FDE047",
                    bold = true
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_restaurante_menu",
            name = "Menu & Sabores Especiais",
            category = "Restaurantes",
            previewStyle = "food_warm",
            width = 1080L,
            height = 1080L,
            backgroundColorHex = "#451A03",
            backgroundGradientHex = "#B45309",
            fontsUsed = listOf("Playfair Display", "Plus Jakarta Sans"),
            colorsUsed = listOf("#451A03", "#FBBF24", "#FFFBEB"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "rs_photo",
                    type = LayerType.IMAGE,
                    name = "Prato Gourmet",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_restaurante"
                ).toMap(),
                DesignLayer(
                    layerId = "rs_tag",
                    type = LayerType.SHAPE,
                    name = "Selo Preço",
                    xFraction = 0.5f,
                    yFraction = 0.84f,
                    widthFraction = 0.48f,
                    heightFraction = 0.11f,
                    colorHex = "#FBBF24",
                    shapeKind = ShapeKind.BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "rs_title",
                    type = LayerType.TEXT,
                    name = "Prato do Dia",
                    xFraction = 0.5f,
                    yFraction = 0.25f,
                    widthFraction = 0.85f,
                    heightFraction = 0.24f,
                    text = "SABOR\nAUTÊNTICO",
                    fontFamilyKey = "playfair_display",
                    fontSizeSp = 40f,
                    colorHex = "#FFFBEB",
                    bold = true,
                    shadowEnabled = true
                ).toMap(),
                DesignLayer(
                    layerId = "rs_desc",
                    type = LayerType.TEXT,
                    name = "Descrição",
                    xFraction = 0.5f,
                    yFraction = 0.43f,
                    widthFraction = 0.82f,
                    heightFraction = 0.09f,
                    text = "Grelhados na brasa com molho da casa",
                    fontFamilyKey = "inter",
                    fontSizeSp = 15f,
                    colorHex = "#FDE68A"
                ).toMap(),
                DesignLayer(
                    layerId = "rs_price",
                    type = LayerType.TEXT,
                    name = "Preço",
                    xFraction = 0.5f,
                    yFraction = 0.84f,
                    widthFraction = 0.46f,
                    heightFraction = 0.08f,
                    text = "4.500 Kz",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 22f,
                    colorHex = "#451A03",
                    bold = true
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_moda_editorial",
            name = "Editorial Moda Urbana",
            category = "Moda",
            previewStyle = "fashion_minimal",
            width = 1080L,
            height = 1920L,
            backgroundColorHex = "#18181B",
            backgroundGradientHex = "#3F3F46",
            fontsUsed = listOf("Playfair Display", "Inter"),
            colorsUsed = listOf("#18181B", "#F43F5E", "#FFFFFF"),
            premium = true,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "md_photo",
                    type = LayerType.IMAGE,
                    name = "Estúdio Moda",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_moda"
                ).toMap(),
                DesignLayer(
                    layerId = "md_box",
                    type = LayerType.SHAPE,
                    name = "Moldura Editorial",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 0.84f,
                    heightFraction = 0.86f,
                    colorHex = "#F43F5E",
                    opacity = 0.35f,
                    shapeKind = ShapeKind.FRAME
                ).toMap(),
                DesignLayer(
                    layerId = "md_title",
                    type = LayerType.TEXT,
                    name = "Coleção",
                    xFraction = 0.5f,
                    yFraction = 0.42f,
                    widthFraction = 0.82f,
                    heightFraction = 0.24f,
                    text = "NOVA\nCOLEÇÃO",
                    fontFamilyKey = "playfair_display",
                    fontSizeSp = 44f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "md_sub",
                    type = LayerType.TEXT,
                    name = "Temporada",
                    xFraction = 0.5f,
                    yFraction = 0.65f,
                    widthFraction = 0.78f,
                    heightFraction = 0.08f,
                    text = "ESTILO & ELEGÂNCIA • 2026",
                    fontFamilyKey = "inter",
                    fontSizeSp = 14f,
                    colorHex = "#FDA4AF",
                    bold = true,
                    letterSpacing = 2.4f
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_tech_launch",
            name = "Lançamento Tech IA",
            category = "Tecnologia",
            previewStyle = "tech_cyan",
            width = 1200L,
            height = 630L,
            backgroundColorHex = "#030712",
            backgroundGradientHex = "#1D4ED8",
            fontsUsed = listOf("Plus Jakarta Sans", "Inter"),
            colorsUsed = listOf("#030712", "#06B6D4", "#FFFFFF"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "tc_photo",
                    type = LayerType.IMAGE,
                    name = "Esfera Cyber",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_tech"
                ).toMap(),
                DesignLayer(
                    layerId = "tc_title",
                    type = LayerType.TEXT,
                    name = "Headline Tech",
                    xFraction = 0.38f,
                    yFraction = 0.4f,
                    widthFraction = 0.64f,
                    heightFraction = 0.34f,
                    text = "O FUTURO É\nDIGITAL",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 36f,
                    colorHex = "#FFFFFF",
                    bold = true
                ).toMap(),
                DesignLayer(
                    layerId = "tc_sub",
                    type = LayerType.TEXT,
                    name = "Subtítulo",
                    xFraction = 0.38f,
                    yFraction = 0.72f,
                    widthFraction = 0.64f,
                    heightFraction = 0.14f,
                    text = "Soluções inteligentes para o seu negócio",
                    fontFamilyKey = "inter",
                    fontSizeSp = 15f,
                    colorHex = "#67E8F9"
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        ),
        DesignTemplate(
            id = "tpl_noticias_flash",
            name = "Última Hora Notícias",
            category = "Notícias",
            previewStyle = "news_breaking",
            width = 1080L,
            height = 1080L,
            backgroundColorHex = "#0F172A",
            backgroundGradientHex = "#991B1B",
            fontsUsed = listOf("Oswald", "Inter"),
            colorsUsed = listOf("#DC2626", "#0F172A", "#FFFFFF"),
            premium = false,
            published = true,
            layers = listOf(
                DesignLayer(
                    layerId = "nw_photo",
                    type = LayerType.IMAGE,
                    name = "Estúdio Jornal",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    widthFraction = 1.0f,
                    heightFraction = 1.0f,
                    imageUri = "res://tpl_art_noticias"
                ).toMap(),
                DesignLayer(
                    layerId = "nw_badge",
                    type = LayerType.SHAPE,
                    name = "Selo Urgente",
                    xFraction = 0.5f,
                    yFraction = 0.24f,
                    widthFraction = 0.52f,
                    heightFraction = 0.09f,
                    colorHex = "#DC2626",
                    shapeKind = ShapeKind.BADGE
                ).toMap(),
                DesignLayer(
                    layerId = "nw_tag",
                    type = LayerType.TEXT,
                    name = "Última Hora",
                    xFraction = 0.5f,
                    yFraction = 0.24f,
                    widthFraction = 0.5f,
                    heightFraction = 0.07f,
                    text = "ÚLTIMA HORA",
                    fontFamilyKey = "oswald",
                    fontSizeSp = 18f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    letterSpacing = 2f
                ).toMap(),
                DesignLayer(
                    layerId = "nw_headline",
                    type = LayerType.TEXT,
                    name = "Manchete",
                    xFraction = 0.5f,
                    yFraction = 0.54f,
                    widthFraction = 0.86f,
                    heightFraction = 0.34f,
                    text = "INOVAÇÃO DIGITAL CRESCE 45% NO PAÍS",
                    fontFamilyKey = "plus_jakarta_sans",
                    fontSizeSp = 30f,
                    colorHex = "#FFFFFF",
                    bold = true,
                    shadowEnabled = true
                ).toMap()
            ) + compactVerifiedCreatorCornerLayers()
        )
    )
}
