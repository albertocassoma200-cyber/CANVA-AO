package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DesignFormat
import com.example.core.model.DesignFormats
import com.example.core.model.DesignLayer
import com.example.core.model.ImageMaskKind
import com.example.core.model.LayerType
import com.example.core.model.ShapeKind
import com.example.core.model.TemplateSeedCatalog
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.OrangeBright
import com.example.ui.theme.editorFontFamily
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun parseComposeColor(hex: String, fallback: Color = Color.White): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (_: Exception) {
        fallback
    }
}

fun DrawScope.drawDesignShape(layer: DesignLayer, col: Color, baseStroke: Float = 4f) {
    when (layer.shapeKind) {
        ShapeKind.CIRCLE -> drawCircle(
            col,
            radius = min(size.width, size.height) / 2f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
        ShapeKind.BADGE -> drawRoundRect(
            col,
            size = size,
            cornerRadius = CornerRadius(min(size.width, size.height) / 2f)
        )
        ShapeKind.FRAME -> drawRect(
            col,
            size = size,
            style = Stroke(width = baseStroke)
        )
        ShapeKind.RECTANGLE -> drawRect(col, size = size)
        ShapeKind.STAR -> {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = min(size.width, size.height) / 2f
            val innerR = outerR * 0.45f
            val p = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerR else innerR
                val a = (i * 36.0 - 90.0) * PI / 180.0
                val px = cx + (r * cos(a)).toFloat()
                val py = cy + (r * sin(a)).toFloat()
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            drawPath(p, col)
        }
        ShapeKind.TRIANGLE -> {
            val p = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(p, col)
        }
        ShapeKind.ARROW -> {
            val p = Path().apply {
                moveTo(0f, size.height * 0.32f)
                lineTo(size.width * 0.65f, size.height * 0.32f)
                lineTo(size.width * 0.65f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(size.width * 0.65f, size.height)
                lineTo(size.width * 0.65f, size.height * 0.68f)
                lineTo(0f, size.height * 0.68f)
                close()
            }
            drawPath(p, col)
        }
        ShapeKind.VERIFIED_BADGE -> {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(size.width, size.height) * 0.48f
            val sealColor = if (layer.colorHex.equals("#FF7A00", true)) Color(0xFF1D9BF0) else col
            drawCircle(color = sealColor, radius = r * 0.80f, center = Offset(cx, cy))
            for (i in 0 until 8) {
                val a = (i * 45.0) * PI / 180.0
                val lx = cx + (r * 0.72f * cos(a)).toFloat()
                val ly = cy + (r * 0.72f * sin(a)).toFloat()
                drawCircle(color = sealColor, radius = r * 0.27f, center = Offset(lx, ly))
            }
            val checkPath = Path().apply {
                moveTo(cx - r * 0.34f, cy + r * 0.02f)
                lineTo(cx - r * 0.08f, cy + r * 0.28f)
                lineTo(cx + r * 0.36f, cy - r * 0.24f)
            }
            drawPath(
                path = checkPath,
                color = Color.White.copy(alpha = layer.opacity.coerceIn(0f, 1f)),
                style = Stroke(width = (r * 0.22f).coerceAtLeast(1.8f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        ShapeKind.QUOTE_MARKS -> {
            val w = size.width
            val h = size.height
            listOf(0.06f, 0.54f).forEach { xOff ->
                val left = w * xOff
                val markW = w * 0.38f
                drawRoundRect(
                    color = col,
                    topLeft = Offset(left, h * 0.42f),
                    size = Size(markW, h * 0.54f),
                    cornerRadius = CornerRadius(markW * 0.25f, markW * 0.25f)
                )
                val curl = Path().apply {
                    moveTo(left, h * 0.52f)
                    cubicTo(left, h * 0.14f, left + markW * 0.42f, h * 0.04f, left + markW * 0.92f, h * 0.04f)
                    lineTo(left + markW * 0.92f, h * 0.28f)
                    cubicTo(left + markW * 0.55f, h * 0.28f, left + markW * 0.44f, h * 0.34f, left + markW * 0.44f, h * 0.52f)
                    close()
                }
                drawPath(curl, col)
            }
        }
        ShapeKind.BOTTOM_ARCH -> {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(0f, h * 0.38f)
                quadraticTo(w * 0.5f, -h * 0.38f, w, h * 0.38f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(p, col)
        }
        ShapeKind.DIAMOND_LINE -> {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val dr = (min(w * 0.08f, h * 0.36f)).coerceAtLeast(3f)
            val strokeW = (h * 0.11f).coerceIn(1.5f, 6f)
            drawLine(
                color = col,
                start = Offset(dr, cy),
                end = Offset(w - dr, cy),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            listOf(dr, w - dr).forEach { dx ->
                val d = Path().apply {
                    moveTo(dx, cy - dr)
                    lineTo(dx + dr, cy)
                    lineTo(dx, cy + dr)
                    lineTo(dx - dr, cy)
                    close()
                }
                drawPath(d, col)
            }
        }
        ShapeKind.DOT_GRID -> {
            val cols = 5
            val rows = 4
            val dotR = (min(size.width, size.height) * 0.048f).coerceAtLeast(1.5f)
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (r == rows - 1 && c > 0) continue
                    val px = size.width * (0.10f + c * 0.20f)
                    val py = size.height * (0.12f + r * 0.25f)
                    drawCircle(col, radius = dotR, center = Offset(px, py))
                }
            }
        }
        ShapeKind.BRACKET_FRAME -> {
            val w = size.width
            val h = size.height
            val sw = (min(w, h) * 0.055f).coerceIn(2.5f, 10f)
            val p = Path().apply {
                moveTo(w * 0.44f, sw * 0.5f)
                lineTo(sw * 0.5f, sw * 0.5f)
                lineTo(sw * 0.5f, h - sw * 0.5f)
                lineTo(w - sw * 0.5f, h - sw * 0.5f)
            }
            drawPath(p, col, style = Stroke(width = sw, cap = StrokeCap.Square, join = StrokeJoin.Miter))
        }
        ShapeKind.PILL_OUTLINE -> {
            val r = min(size.width, size.height) / 2f
            val sw = (min(size.width, size.height) * 0.065f).coerceIn(2f, 6f)
            drawRoundRect(
                color = col,
                size = size,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = sw)
            )
        }
        ShapeKind.SCRIM_BOTTOM -> {
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.55f to col.copy(alpha = col.alpha * 0.72f),
                    1.0f to col
                ),
                size = size
            )
        }
        ShapeKind.ROUNDED_RECT -> {
            val cr = (layer.cornerRadiusDp * (min(size.width, size.height) / 140f)).coerceIn(2f, min(size.width, size.height) / 2f)
            drawRoundRect(col, size = size, cornerRadius = CornerRadius(cr))
        }
    }
}

@Composable
fun CanvaAoLogoMark(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val outerPath = Path().apply {
            moveTo(w * 0.18f, h * 0.14f)
            lineTo(w * 0.88f, h * 0.46f)
            quadraticTo(w * 0.96f, h * 0.50f, w * 0.88f, h * 0.54f)
            lineTo(w * 0.18f, h * 0.86f)
            quadraticTo(w * 0.10f, h * 0.89f, w * 0.10f, h * 0.80f)
            lineTo(w * 0.10f, h * 0.20f)
            quadraticTo(w * 0.10f, h * 0.11f, w * 0.18f, h * 0.14f)
            close()
        }
        drawPath(
            path = outerPath,
            brush = Brush.linearGradient(listOf(OrangeBright, OrangeAccent))
        )
        val innerPath = Path().apply {
            moveTo(w * 0.30f, h * 0.31f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.30f, h * 0.69f)
            close()
        }
        drawPath(path = innerPath, color = Color.White)
        // Creative nib cut
        val nibPath = Path().apply {
            moveTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.94f, h * 0.43f)
            lineTo(w * 0.94f, h * 0.57f)
            close()
        }
        drawPath(path = nibPath, color = NavyPrimary)
    }
}

@Composable
fun CanvaAoWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CanvaAoLogoMark(size = 28.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "CANVA ",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "AO",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = OrangeAccent
            )
        }
    }
}

@Composable
fun CreditPillBadge(
    balance: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("credits_pill_badge"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = "Créditos",
                tint = OrangeBright,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$balance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(OrangeAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Adicionar créditos",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun DesignCanvasThumbnail(
    width: Long,
    height: Long,
    backgroundColorHex: String,
    backgroundGradientHex: String?,
    layers: List<Map<String, Any>>,
    modifier: Modifier = Modifier,
    userAvatarUrl: String? = null,
    userDisplayName: String? = null,
    userHandle: String? = null
) {
    val needsAuthFallback = userAvatarUrl.isNullOrBlank() || userDisplayName.isNullOrBlank() || userHandle.isNullOrBlank()
    val authUser = remember(needsAuthFallback) {
        if (needsAuthFallback) Firebase.auth.currentUser else null
    }
    val resolvedAvatar = userAvatarUrl?.takeIf { it.isNotBlank() }
        ?: authUser?.photoUrl?.toString()?.takeIf { it.isNotBlank() }
        ?: "res://ic_default_avatar"
    val resolvedName = userDisplayName?.takeIf { it.isNotBlank() }
        ?: authUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "Criador CANVA AO"
    val resolvedHandle = userHandle?.takeIf { it.isNotBlank() }
        ?: run {
            val emailPrefix = authUser?.email?.substringBefore("@").orEmpty()
            val clean = emailPrefix.ifBlank { resolvedName.lowercase().replace(Regex("[^a-z0-9]"), "") }
            "@" + clean.ifBlank { "criador" }
        }

    val context = LocalContext.current
    val isTransparent = backgroundColorHex.equals("transparent", ignoreCase = true)
    val bgStart = if (isTransparent) Color(0xFF1E293B) else parseComposeColor(backgroundColorHex, NavyPrimary)
    val bgEnd = if (isTransparent) Color(0xFF334155) else (backgroundGradientHex?.let { parseComposeColor(it, bgStart) } ?: bgStart)
    val parsedLayers = remember(layers, resolvedAvatar, resolvedName, resolvedHandle) {
        layers.map { raw ->
            val l = DesignLayer.fromMap(raw)
            val resolvedUri = if (l.imageUri == TemplateSeedCatalog.TOKEN_USER_AVATAR) resolvedAvatar else l.imageUri
            val resolvedText = l.text
                .replace(TemplateSeedCatalog.TOKEN_USER_NAME, resolvedName)
                .replace(TemplateSeedCatalog.TOKEN_USER_HANDLE, resolvedHandle)
            l.copy(imageUri = resolvedUri, text = resolvedText)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(bgStart, bgEnd)))
    ) {
        val boxW = maxWidth
        val boxH = maxHeight
        val scale = min(boxW.value, boxH.value) / 260f

        for (layer in parsedLayers) {
            if (!layer.visible) continue
            val layerW = boxW * layer.widthFraction
            val layerH = boxH * layer.heightFraction
            val offsetX = boxW * (layer.xFraction - layer.widthFraction / 2f)
            val offsetY = boxH * (layer.yFraction - layer.heightFraction / 2f)

            Box(
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .size(width = layerW, height = layerH)
                    .rotate(layer.rotation),
                contentAlignment = Alignment.Center
            ) {
                when (layer.type) {
                    LayerType.IMAGE -> {
                        val maskShape = when (layer.maskKind) {
                            ImageMaskKind.CIRCLE -> CircleShape
                            ImageMaskKind.ROUNDED -> RoundedCornerShape(10.dp)
                            ImageMaskKind.NONE -> RectangleShape
                        }
                        val resId = TemplateSeedCatalog.resolveDrawableResId(layer.imageUri)
                        val imageData: Any? = when {
                            resId != null -> resId
                            !layer.imageUri.isNullOrBlank() -> android.net.Uri.parse(layer.imageUri)
                            else -> null
                        }
                        if (imageData != null) {
                            val request = remember(imageData, context) {
                                coil.request.ImageRequest.Builder(context)
                                    .data(imageData)
                                    .crossfade(false)
                                    .build()
                            }
                            coil.compose.AsyncImage(
                                model = request,
                                contentDescription = layer.name,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alpha = layer.opacity.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(maskShape)
                            )
                        }
                    }
                    LayerType.SHAPE, LayerType.STICKER, LayerType.LINE -> {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val col = parseComposeColor(layer.colorHex, OrangeAccent)
                                .copy(alpha = layer.opacity.coerceIn(0f, 1f))
                            if (layer.type == LayerType.LINE) {
                                drawLine(
                                    col,
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f),
                                    strokeWidth = 3f,
                                    cap = StrokeCap.Round
                                )
                            } else {
                                drawDesignShape(layer, col, baseStroke = 3f)
                            }
                        }
                    }
                    LayerType.TEXT -> {
                        val textColor = parseComposeColor(layer.colorHex, Color.White)
                            .copy(alpha = layer.opacity.coerceIn(0f, 1f))
                        val fontSize = (layer.fontSizeSp * scale * 0.68f).coerceIn(5.5f, 28f).sp
                        Text(
                            text = layer.text,
                            color = textColor,
                            fontFamily = editorFontFamily(layer.fontFamilyKey),
                            fontSize = fontSize,
                            lineHeight = fontSize * 1.08f,
                            fontWeight = if (layer.bold) FontWeight.ExtraBold else FontWeight.Medium,
                            fontStyle = if (layer.italic) FontStyle.Italic else FontStyle.Normal,
                            textAlign = when (layer.textAlign) {
                                "left" -> TextAlign.Start
                                "right" -> TextAlign.End
                                else -> TextAlign.Center
                            },
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

data class CreateMenuOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val format: DesignFormat?,
    val isVideo: Boolean = false,
    val isCustom: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOptionsBottomSheet(
    onDismiss: () -> Unit,
    onSelectDesignFormat: (DesignFormat, String) -> Unit,
    onSelectCustomDimensions: (String, Int, Int) -> Unit,
    onSelectVideoEditor: () -> Unit
) {
    val options = remember {
        listOf(
            CreateMenuOption("post", "Criar Publicação", "1080 × 1080 px", Icons.Filled.PostAdd, DesignFormats.presets[0]),
            CreateMenuOption("flyer", "Criar Flyer", "1080 × 1528 px", Icons.Filled.Campaign, DesignFormats.presets[6]),
            CreateMenuOption("poster", "Criar Cartaz", "1080 × 1350 px", Icons.Filled.PhotoSizeSelectLarge, DesignFormats.presets[1]),
            CreateMenuOption("story", "Criar Story", "1080 × 1920 px", Icons.Filled.AutoAwesome, DesignFormats.presets[2]),
            CreateMenuOption("cover", "Criar Capa", "1200 × 630 px", Icons.Filled.Image, DesignFormats.presets[3]),
            CreateMenuOption("yt", "Miniatura YouTube", "1280 × 720 px", Icons.Filled.PlayCircle, DesignFormats.presets[4]),
            CreateMenuOption("video", "Criar Vídeo", "Linha do tempo MP4", Icons.Filled.VideoLibrary, null, isVideo = true),
            CreateMenuOption("custom", "Design Personalizado", "Largura × Altura", Icons.Filled.AspectRatio, null, isCustom = true)
        )
    }

    var showCustomForm by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("Meu Design") }
    var customWidth by remember { mutableStateOf("1080") }
    var customHeight by remember { mutableStateOf("1080") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = if (showCustomForm) "Dimensões Personalizadas" else "O que deseja criar hoje?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (showCustomForm) "Defina a resolução em píxeis para o seu canvas." else "Escolha um formato profissional ou inicie um vídeo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (showCustomForm) {
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Nome do projeto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customWidth,
                        onValueChange = { if (it.all(Char::isDigit)) customWidth = it.take(4) },
                        label = { Text("Largura (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customHeight,
                        onValueChange = { if (it.all(Char::isDigit)) customHeight = it.take(4) },
                        label = { Text("Altura (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showCustomForm = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text("Voltar")
                    }
                    Button(
                        onClick = {
                            val w = (customWidth.toIntOrNull() ?: 1080).coerceIn(200, 4096)
                            val h = (customHeight.toIntOrNull() ?: 1080).coerceIn(200, 4096)
                            onSelectCustomDimensions(customTitle, w, h)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("create_custom_confirm_button")
                    ) {
                        Text("Criar Canvas")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    modifier = Modifier.height(380.dp)
                ) {
                    items(options, key = { it.id }) { opt ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    when {
                                        opt.isVideo -> {
                                            onDismiss()
                                            onSelectVideoEditor()
                                        }
                                        opt.isCustom -> showCustomForm = true
                                        opt.format != null -> {
                                            onDismiss()
                                            onSelectDesignFormat(opt.format, opt.title.removePrefix("Criar "))
                                        }
                                    }
                                }
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .testTag("create_option_${opt.id}"),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(OrangeAccent.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = opt.icon,
                                        contentDescription = opt.title,
                                        tint = OrangeAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = opt.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = opt.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
