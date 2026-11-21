package com.example.engine.graphics

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.core.model.DesignLayer
import com.example.core.model.DesignProject
import com.example.core.model.ImageFilterKind
import com.example.core.model.ImageMaskKind
import com.example.core.model.LayerType
import com.example.core.model.ShapeKind
import com.example.ui.theme.editorFontResId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class ExportedFileResult(
    val fileName: String,
    val fileSizeBytes: Long,
    val shareUri: Uri,
    val galleryUri: Uri?
)

object BitmapRenderer {

    fun parseColorSafe(hex: String, fallback: Int = Color.WHITE): Int {
        return try {
            Color.parseColor(hex.trim())
        } catch (_: Exception) {
            fallback
        }
    }

    suspend fun saveBitmapToTempUri(context: Context, bitmap: Bitmap, prefix: String = "ai_img"): Uri =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "${prefix}_${UUID.randomUUID().toString().take(8)}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }

    suspend fun exportProjectToImage(
        context: Context,
        project: DesignProject,
        targetWidth: Int,
        targetHeight: Int,
        isPng: Boolean
    ): Result<ExportedFileResult> = withContext(Dispatchers.IO) {
        try {
            val safeW = targetWidth.coerceIn(200, 4096)
            val safeH = targetHeight.coerceIn(200, 4096)
            val bitmap = renderDesignBitmap(context, project, safeW, safeH)

            val ext = if (isPng) "png" else "jpg"
            val mime = if (isPng) "image/png" else "image/jpeg"
            val cleanName = project.title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40).ifBlank { "canva_ao" }
            val fileName = "${cleanName}_${System.currentTimeMillis()}.$ext"

            val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
            val outFile = File(exportDir, fileName)
            FileOutputStream(outFile).use { out ->
                val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val quality = if (isPng) 100 else 95
                if (!bitmap.compress(format, quality, out)) {
                    throw IllegalStateException("Falha ao comprimir imagem.")
                }
            }

            // Save to Android MediaStore Gallery
            val galleryUri = insertIntoGallery(context, outFile, fileName, mime)
            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            bitmap.recycle()

            Result.success(
                ExportedFileResult(
                    fileName = fileName,
                    fileSizeBytes = outFile.length(),
                    shareUri = shareUri,
                    galleryUri = galleryUri
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun insertIntoGallery(context: Context, file: File, fileName: String, mimeType: String): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CANVA AO")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { os ->
                file.inputStream().use { input -> input.copyTo(os) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Exception) {
            null
        }
    }

    fun renderDesignBitmap(
        context: Context,
        project: DesignProject,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background
        if (!project.backgroundColorHex.equals("transparent", ignoreCase = true)) {
            val bgStart = parseColorSafe(project.backgroundColorHex, Color.parseColor("#0A1628"))
            val bgEnd = project.backgroundGradientHex?.let { parseColorSafe(it, bgStart) } ?: bgStart
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    bgStart, bgEnd, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }

        // 2. Layers
        val layers = project.layers.map { DesignLayer.fromMap(it) }
        val scaleFactor = min(width, height) / 400f

        for (layer in layers) {
            if (!layer.visible) continue
            canvas.save()
            val cx = layer.xFraction * width
            val cy = layer.yFraction * height
            val lw = (layer.widthFraction * width).coerceAtLeast(8f)
            val lh = (layer.heightFraction * height).coerceAtLeast(8f)
            canvas.translate(cx, cy)
            canvas.rotate(layer.rotation)

            when (layer.type) {
                LayerType.SHAPE, LayerType.STICKER -> drawShapeLayer(canvas, layer, lw, lh, scaleFactor)
                LayerType.LINE -> drawLineLayer(canvas, layer, lw, scaleFactor)
                LayerType.IMAGE -> drawImageLayer(context, canvas, layer, lw, lh, scaleFactor)
                LayerType.TEXT -> drawTextLayer(context, canvas, layer, lw, lh, scaleFactor)
            }
            canvas.restore()
        }
        return bitmap
    }

    private fun drawShapeLayer(
        canvas: Canvas,
        layer: DesignLayer,
        lw: Float,
        lh: Float,
        scale: Float
    ) {
        val alphaInt = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
        val color = parseColorSafe(layer.colorHex, Color.parseColor("#FF7A00"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.alpha = alphaInt
            style = if (layer.shapeKind == ShapeKind.FRAME) Paint.Style.STROKE else Paint.Style.FILL
            if (layer.shapeKind == ShapeKind.FRAME) {
                strokeWidth = 6f * scale
            }
        }
        val rect = RectF(-lw / 2f, -lh / 2f, lw / 2f, lh / 2f)
        when (layer.shapeKind) {
            ShapeKind.RECTANGLE, ShapeKind.FRAME -> canvas.drawRect(rect, paint)
            ShapeKind.ROUNDED_RECT -> {
                val r = (layer.cornerRadiusDp * (min(lw, lh) / 140f)).coerceIn(2f, min(lw, lh) / 2f)
                canvas.drawRoundRect(rect, r, r, paint)
            }
            ShapeKind.BADGE -> {
                val r = min(lw, lh) / 2f
                canvas.drawRoundRect(rect, r, r, paint)
            }
            ShapeKind.CIRCLE -> {
                val radius = min(lw, lh) / 2f
                canvas.drawCircle(0f, 0f, radius, paint)
            }
            ShapeKind.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(0f, -lh / 2f)
                    lineTo(lw / 2f, lh / 2f)
                    lineTo(-lw / 2f, lh / 2f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            ShapeKind.STAR -> {
                val outerR = min(lw, lh) / 2f
                val innerR = outerR * 0.45f
                val path = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = (i * 36.0 - 90.0) * PI / 180.0
                    val px = (r * cos(angle)).toFloat()
                    val py = (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            ShapeKind.ARROW -> {
                val path = Path().apply {
                    moveTo(-lw / 2f, -lh * 0.18f)
                    lineTo(lw * 0.15f, -lh * 0.18f)
                    lineTo(lw * 0.15f, -lh / 2f)
                    lineTo(lw / 2f, 0f)
                    lineTo(lw * 0.15f, lh / 2f)
                    lineTo(lw * 0.15f, lh * 0.18f)
                    lineTo(-lw / 2f, lh * 0.18f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            ShapeKind.VERIFIED_BADGE -> {
                val r = min(lw, lh) * 0.48f
                val sealColor = if (layer.colorHex.equals("#FF7A00", true)) Color.parseColor("#1D9BF0") else color
                val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = sealColor
                    this.alpha = alphaInt
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(0f, 0f, r * 0.80f, sealPaint)
                for (i in 0 until 8) {
                    val a = (i * 45.0) * PI / 180.0
                    val lx = (r * 0.72f * cos(a)).toFloat()
                    val ly = (r * 0.72f * sin(a)).toFloat()
                    canvas.drawCircle(lx, ly, r * 0.27f, sealPaint)
                }
                val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = Color.WHITE
                    this.alpha = alphaInt
                    style = Paint.Style.STROKE
                    strokeWidth = (r * 0.22f).coerceAtLeast(3f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val checkPath = Path().apply {
                    moveTo(-r * 0.34f, r * 0.02f)
                    lineTo(-r * 0.08f, r * 0.28f)
                    lineTo(r * 0.36f, -r * 0.24f)
                }
                canvas.drawPath(checkPath, checkPaint)
            }
            ShapeKind.QUOTE_MARKS -> {
                listOf(-lw * 0.44f, lw * 0.04f).forEach { left ->
                    val markW = lw * 0.38f
                    val boxRect = RectF(left, -lh * 0.08f, left + markW, lh * 0.46f)
                    canvas.drawRoundRect(boxRect, markW * 0.25f, markW * 0.25f, paint)
                    val curl = Path().apply {
                        moveTo(left, lh * 0.02f)
                        cubicTo(left, -lh * 0.36f, left + markW * 0.42f, -lh * 0.46f, left + markW * 0.92f, -lh * 0.46f)
                        lineTo(left + markW * 0.92f, -lh * 0.22f)
                        cubicTo(left + markW * 0.55f, -lh * 0.22f, left + markW * 0.44f, -lh * 0.16f, left + markW * 0.44f, lh * 0.02f)
                        close()
                    }
                    canvas.drawPath(curl, paint)
                }
            }
            ShapeKind.BOTTOM_ARCH -> {
                val p = Path().apply {
                    moveTo(-lw / 2f, -lh * 0.12f)
                    quadTo(0f, -lh * 0.88f, lw / 2f, -lh * 0.12f)
                    lineTo(lw / 2f, lh / 2f)
                    lineTo(-lw / 2f, lh / 2f)
                    close()
                }
                canvas.drawPath(p, paint)
            }
            ShapeKind.DIAMOND_LINE -> {
                val dr = min(lw * 0.08f, lh * 0.36f).coerceAtLeast(5f)
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    this.alpha = alphaInt
                    style = Paint.Style.STROKE
                    strokeWidth = (lh * 0.11f).coerceIn(3f, 10f)
                    strokeCap = Paint.Cap.ROUND
                }
                canvas.drawLine(-lw / 2f + dr, 0f, lw / 2f - dr, 0f, linePaint)
                listOf(-lw / 2f + dr, lw / 2f - dr).forEach { dx ->
                    val d = Path().apply {
                        moveTo(dx, -dr)
                        lineTo(dx + dr, 0f)
                        lineTo(dx, dr)
                        lineTo(dx - dr, 0f)
                        close()
                    }
                    canvas.drawPath(d, paint)
                }
            }
            ShapeKind.DOT_GRID -> {
                val dotR = min(lw, lh) * 0.048f
                for (r in 0 until 4) {
                    for (c in 0 until 5) {
                        if (r == 3 && c > 0) continue
                        val px = -lw / 2f + lw * (0.10f + c * 0.20f)
                        val py = -lh / 2f + lh * (0.12f + r * 0.25f)
                        canvas.drawCircle(px, py, dotR, paint)
                    }
                }
            }
            ShapeKind.BRACKET_FRAME -> {
                val sw = (min(lw, lh) * 0.055f).coerceIn(4f, 18f)
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    this.alpha = alphaInt
                    style = Paint.Style.STROKE
                    strokeWidth = sw
                    strokeCap = Paint.Cap.SQUARE
                    strokeJoin = Paint.Join.MITER
                }
                val p = Path().apply {
                    moveTo(-lw * 0.06f, -lh / 2f + sw * 0.5f)
                    lineTo(-lw / 2f + sw * 0.5f, -lh / 2f + sw * 0.5f)
                    lineTo(-lw / 2f + sw * 0.5f, lh / 2f - sw * 0.5f)
                    lineTo(lw / 2f - sw * 0.5f, lh / 2f - sw * 0.5f)
                }
                canvas.drawPath(p, strokePaint)
            }
            ShapeKind.PILL_OUTLINE -> {
                val r = min(lw, lh) / 2f
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    this.alpha = alphaInt
                    style = Paint.Style.STROKE
                    strokeWidth = (min(lw, lh) * 0.065f).coerceIn(3f, 12f)
                }
                canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            ShapeKind.SCRIM_BOTTOM -> {
                val midColor = Color.argb((alphaInt * 0.72f).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                val fullColor = Color.argb(alphaInt, Color.red(color), Color.green(color), Color.blue(color))
                val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, -lh / 2f, 0f, lh / 2f,
                        intArrayOf(Color.TRANSPARENT, midColor, fullColor),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(rect, scrimPaint)
            }
        }
    }

    private fun drawLineLayer(canvas: Canvas, layer: DesignLayer, lw: Float, scale: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorSafe(layer.colorHex, Color.WHITE)
            alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
            strokeWidth = 4f * scale
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(-lw / 2f, 0f, lw / 2f, 0f, paint)
    }

    private fun drawImageLayer(
        context: Context,
        canvas: Canvas,
        layer: DesignLayer,
        lw: Float,
        lh: Float,
        scale: Float
    ) {
        val uriStr = layer.imageUri ?: return
        val resId = com.example.core.model.TemplateSeedCatalog.resolveDrawableResId(uriStr)
        val srcBitmap = try {
            if (resId != null) {
                BitmapFactory.decodeResource(context.resources, resId)
            } else if (uriStr.startsWith("http://", true) || uriStr.startsWith("https://", true)) {
                java.net.URL(uriStr).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(uriStr))
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            null
        } ?: return

        val rect = RectF(-lw / 2f, -lh / 2f, lw / 2f, lh / 2f)
        canvas.save()
        when (layer.maskKind) {
            ImageMaskKind.CIRCLE -> {
                val path = Path().apply { addCircle(0f, 0f, min(lw, lh) / 2f, Path.Direction.CW) }
                canvas.clipPath(path)
            }
            ImageMaskKind.ROUNDED -> {
                val r = 20f * scale
                val path = Path().apply { addRoundRect(rect, r, r, Path.Direction.CW) }
                canvas.clipPath(path)
            }
            ImageMaskKind.NONE -> Unit
        }

        // Compute CENTER_CROP source rect to avoid stretching photos
        val bw = srcBitmap.width.toFloat().coerceAtLeast(1f)
        val bh = srcBitmap.height.toFloat().coerceAtLeast(1f)
        val targetAspect = lw / lh.coerceAtLeast(1f)
        val bmpAspect = bw / bh
        val srcRect = if (bmpAspect > targetAspect) {
            val cropW = bh * targetAspect
            val left = ((bw - cropW) / 2f).toInt()
            android.graphics.Rect(left, 0, (left + cropW).toInt(), srcBitmap.height)
        } else {
            val cropH = bw / targetAspect
            val top = ((bh - cropH) / 2f).toInt()
            android.graphics.Rect(0, top, srcBitmap.width, (top + cropH).toInt())
        }

        val cm = buildColorMatrix(layer)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(srcBitmap, srcRect, rect, paint)
        canvas.restore()
        srcBitmap.recycle()
    }

    private fun buildColorMatrix(layer: DesignLayer): ColorMatrix {
        val sat = when (layer.filterKind) {
            ImageFilterKind.BW -> 0f
            ImageFilterKind.VIVID -> (layer.saturation * 1.45f).coerceIn(0f, 2.5f)
            else -> layer.saturation
        }
        val cm = ColorMatrix().apply { setSaturation(sat) }

        val contrast = when (layer.filterKind) {
            ImageFilterKind.VIVID -> layer.contrast * 1.15f
            ImageFilterKind.VINTAGE -> layer.contrast * 0.9f
            else -> layer.contrast
        }
        val translate = (-0.5f * contrast + 0.5f) * 255f + (layer.brightness * 120f)
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        when (layer.filterKind) {
            ImageFilterKind.WARM -> cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.1f, 0f, 0f, 0f, 12f,
                        0f, 1.02f, 0f, 0f, 4f,
                        0f, 0f, 0.88f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            ImageFilterKind.COOL -> cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, 0f,
                        0f, 1.0f, 0f, 0f, 4f,
                        0f, 0f, 1.15f, 0f, 14f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            ImageFilterKind.VINTAGE -> cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        0.95f, 0.15f, 0.05f, 0f, 10f,
                        0.05f, 0.9f, 0.05f, 0f, 6f,
                        0f, 0.1f, 0.72f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            else -> Unit
        }
        return cm
    }

    private fun drawTextLayer(
        context: Context,
        canvas: Canvas,
        layer: DesignLayer,
        lw: Float,
        lh: Float,
        scale: Float
    ) {
        val baseTf = try {
            ResourcesCompat.getFont(context, editorFontResId(layer.fontFamilyKey)) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
        val style = when {
            layer.bold && layer.italic -> Typeface.BOLD_ITALIC
            layer.bold -> Typeface.BOLD
            layer.italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val tf = Typeface.create(baseTf, style)
        val alphaInt = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = tf
            textSize = (layer.fontSizeSp * scale).coerceAtLeast(12f)
            color = parseColorSafe(layer.colorHex, Color.WHITE)
            alpha = alphaInt
            letterSpacing = layer.letterSpacing * 0.05f
            if (layer.shadowEnabled) {
                setShadowLayer(6f * scale, 2f * scale, 3f * scale, Color.argb(160, 0, 0, 0))
            }
        }

        if (layer.curved) {
            val radius = (lw * 0.55f).coerceAtLeast(40f)
            val arcPath = Path().apply {
                val oval = RectF(-radius, -radius * 0.35f, radius, radius * 1.65f)
                addArc(oval, 195f, 150f)
            }
            textPaint.textAlign = Paint.Align.CENTER
            if (layer.outlineEnabled) {
                val strokePaint = TextPaint(textPaint).apply {
                    this.style = Paint.Style.STROKE
                    strokeWidth = 3f * scale
                    color = parseColorSafe(layer.outlineColorHex, Color.BLACK)
                }
                canvas.drawTextOnPath(layer.text.replace("\n", " "), arcPath, 0f, 0f, strokePaint)
            }
            canvas.drawTextOnPath(layer.text.replace("\n", " "), arcPath, 0f, 0f, textPaint)
            return
        }

        val alignment = when (layer.textAlign) {
            "left" -> Layout.Alignment.ALIGN_NORMAL
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }

        val layoutWidth = lw.toInt().coerceAtLeast(32)
        if (layer.outlineEnabled) {
            val strokePaint = TextPaint(textPaint).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = 4f * scale
                color = parseColorSafe(layer.outlineColorHex, Color.BLACK)
            }
            val strokeLayout = StaticLayout.Builder.obtain(
                layer.text, 0, layer.text.length, strokePaint, layoutWidth
            ).setAlignment(alignment).build()
            canvas.save()
            canvas.translate(-layoutWidth / 2f, -strokeLayout.height / 2f)
            strokeLayout.draw(canvas)
            canvas.restore()
        }

        val layout = StaticLayout.Builder.obtain(
            layer.text, 0, layer.text.length, textPaint, layoutWidth
        ).setAlignment(alignment).build()
        canvas.save()
        canvas.translate(-layoutWidth / 2f, -layout.height / 2f)
        layout.draw(canvas)
        canvas.restore()
    }
}
