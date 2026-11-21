package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.core.model.DesignLayer
import com.example.core.model.DesignProject
import com.example.core.model.ImageFilterKind
import com.example.core.model.ImageMaskKind
import com.example.core.model.LayerType
import com.example.core.model.ShapeKind
import com.example.core.model.TemplateSeedCatalog
import com.example.data.repository.AiStudioRepository
import com.example.engine.graphics.BitmapRenderer
import com.example.engine.graphics.ExportedFileResult
import com.example.ui.components.drawDesignShape
import com.example.ui.components.parseComposeColor
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.editorFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class EditorTab(val label: String, val icon: ImageVector) {
    TEXT("Texto", Icons.Filled.TextFields),
    IMAGE("Imagem", Icons.Filled.Image),
    ELEMENTS("Elementos", Icons.Filled.Category),
    LAYERS("Camadas", Icons.Filled.Layers),
    BACKGROUND("Fundos", Icons.Filled.Palette)
}

@Composable
fun DesignEditorScreen(
    initialProject: DesignProject,
    creditsBalance: Long,
    onBack: () -> Unit,
    onSaveToCloud: (DesignProject) -> Unit,
    onReserveExport: suspend (projectId: String, title: String, exportType: String, resolution: String, cost: Long, fileName: String) -> Result<String>,
    onCompleteExport: suspend (exportId: String, cost: Long, exportType: String, fileSizeBytes: Long) -> Result<Long>,
    onFailExport: suspend (exportId: String, cancelled: Boolean) -> Unit,
    onGoToCredits: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiRepository = remember { AiStudioRepository() }

    var project by remember(initialProject.id, initialProject.title) { mutableStateOf(initialProject) }
    var layers by remember(initialProject) {
        mutableStateOf(initialProject.layers.map { DesignLayer.fromMap(it) })
    }

    // Undo / Redo Stacks
    val undoStack = remember { mutableStateListOf<List<DesignLayer>>() }
    val redoStack = remember { mutableStateListOf<List<DesignLayer>>() }

    fun commitLayers(newLayers: List<DesignLayer>, recordUndo: Boolean = true) {
        if (recordUndo && newLayers != layers) {
            undoStack.add(layers)
            if (undoStack.size > 30) undoStack.removeAt(0)
            redoStack.clear()
        }
        layers = newLayers
        project = project.copy(layers = newLayers.map { it.toMap() })
    }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val primarySelected = remember(layers, selectedIds) {
        layers.firstOrNull { it.layerId in selectedIds }
    }

    var activeTab by remember { mutableStateOf(EditorTab.TEXT) }
    var toolsExpanded by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(false) }
    var snapCenterX by remember { mutableStateOf(false) }
    var snapCenterY by remember { mutableStateOf(false) }
    var canvasZoom by remember { mutableFloatStateOf(1f) }
    var canvasPan by remember { mutableStateOf(Offset.Zero) }
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportedResult by remember { mutableStateOf<ExportedFileResult?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var aiBusy by remember { mutableStateOf(false) }
    var replacingLayerId by remember { mutableStateOf<String?>(null) }

    // Select first editable text/top layer initially for convenience
    LaunchedEffect(initialProject) {
        selectedIds = layers.lastOrNull()?.let { setOf(it.layerId) } ?: emptySet()
    }

    // Photo Picker (Zero-permission)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val newLayer = DesignLayer(
                layerId = "img_${UUID.randomUUID().toString().take(8)}",
                type = LayerType.IMAGE,
                name = "Imagem ${layers.size + 1}",
                xFraction = 0.5f,
                yFraction = 0.5f,
                widthFraction = 0.6f,
                heightFraction = 0.48f,
                imageUri = uri.toString()
            )
            commitLayers(layers + newLayer)
            selectedIds = setOf(newLayer.layerId)
            activeTab = EditorTab.IMAGE
            toolsExpanded = true
        }
    }

    val replacePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val targetId = replacingLayerId
        replacingLayerId = null
        if (uri != null && targetId != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            commitLayers(layers.map {
                if (it.layerId == targetId) it.copy(imageUri = uri.toString()) else it
            })
            onShowMessage("Imagem substituída!")
        }
    }

    val bgImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val bgLayer = DesignLayer(
                layerId = "bg_image_layer",
                type = LayerType.IMAGE,
                name = "Imagem de Fundo",
                xFraction = 0.5f,
                yFraction = 0.5f,
                widthFraction = 1.0f,
                heightFraction = 1.0f,
                imageUri = uri.toString()
            )
            val withoutOldBg = layers.filterNot { it.layerId == "bg_image_layer" }
            commitLayers(listOf(bgLayer) + withoutOldBg)
            onShowMessage("Imagem de fundo aplicada!")
        }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showRenameDialog = true }
                    ) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${project.width} × ${project.height} px",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val prev = undoStack.removeAt(undoStack.lastIndex)
                                redoStack.add(layers)
                                layers = prev
                                project = project.copy(layers = prev.map { it.toMap() })
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Desfazer")
                    }
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeAt(redoStack.lastIndex)
                                undoStack.add(layers)
                                layers = next
                                project = project.copy(layers = next.map { it.toMap() })
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Refazer")
                    }
                    IconButton(onClick = { showGrid = !showGrid }) {
                        Icon(
                            Icons.Filled.GridOn,
                            contentDescription = "Grelha",
                            tint = if (showGrid) OrangeAccent else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { onSaveToCloud(project) },
                        modifier = Modifier.testTag("editor_save_cloud_button")
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Guardar na nuvem")
                    }
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("editor_export_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Column {
                    // Collapsible header bar so canvas always gets 65-80% of the screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toolsExpanded = !toolsExpanded }
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (primarySelected != null) "${activeTab.label} • ${primarySelected.name}" else activeTab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = OrangeAccent
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (toolsExpanded) "Ocultar painel" else "Mostrar painel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = if (toolsExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                contentDescription = if (toolsExpanded) "Ocultar ferramentas" else "Mostrar ferramentas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (toolsExpanded) {
                        val toolBoxModifier = if (activeTab == EditorTab.LAYERS) {
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 185.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 175.dp)
                                .verticalScroll(rememberScrollState())
                        }
                        Box(modifier = toolBoxModifier) {
                            when (activeTab) {
                                EditorTab.TEXT -> TextToolsPanel(
                                    selectedLayer = primarySelected,
                                    onAddText = {
                                        val newTxt = DesignLayer(
                                            layerId = "txt_${UUID.randomUUID().toString().take(8)}",
                                            type = LayerType.TEXT,
                                            name = "Texto ${layers.size + 1}",
                                            xFraction = 0.5f,
                                            yFraction = 0.5f,
                                            widthFraction = 0.72f,
                                            heightFraction = 0.16f,
                                            text = "NOVO TEXTO",
                                            fontFamilyKey = "plus_jakarta_sans",
                                            fontSizeSp = 28f,
                                            colorHex = "#FFFFFF",
                                            bold = true
                                        )
                                        commitLayers(layers + newTxt)
                                        selectedIds = setOf(newTxt.layerId)
                                    },
                                    onUpdateLayer = { updated ->
                                        commitLayers(layers.map { if (it.layerId == updated.layerId) updated else it }, recordUndo = false)
                                    }
                                )
                                EditorTab.IMAGE -> ImageToolsPanel(
                                    selectedLayer = primarySelected,
                                    aiBusy = aiBusy,
                                    onImportPhoto = {
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    onReplacePhoto = { imgLayer ->
                                        replacingLayerId = imgLayer.layerId
                                        replacePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    onUseAsBackground = { imgLayer ->
                                        val asBg = imgLayer.copy(
                                            xFraction = 0.5f,
                                            yFraction = 0.5f,
                                            widthFraction = 1.0f,
                                            heightFraction = 1.0f,
                                            rotation = 0f
                                        )
                                        val reordered = listOf(asBg) + layers.filterNot { it.layerId == imgLayer.layerId }
                                        commitLayers(reordered)
                                        onShowMessage("Imagem definida como fundo!")
                                    },
                                    onRemoveBackgroundAi = { imgLayer ->
                                        val uriStr = imgLayer.imageUri ?: return@ImageToolsPanel
                                        aiBusy = true
                                        scope.launch {
                                            try {
                                                val bmp = withContext(Dispatchers.IO) {
                                                    val resId = TemplateSeedCatalog.resolveDrawableResId(uriStr)
                                                    if (resId != null) {
                                                        BitmapFactory.decodeResource(context.resources, resId)
                                                    } else if (uriStr.startsWith("http://", true) || uriStr.startsWith("https://", true)) {
                                                        java.net.URL(uriStr).openStream().use { stream ->
                                                            BitmapFactory.decodeStream(stream)
                                                        }
                                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                        val src = ImageDecoder.createSource(context.contentResolver, Uri.parse(uriStr))
                                                        ImageDecoder.decodeBitmap(src) { dec, _, _ -> dec.isMutableRequired = true }
                                                    } else {
                                                        context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                                                            BitmapFactory.decodeStream(it)
                                                        }
                                                    }
                                                } ?: throw IllegalStateException("Não foi possível ler a imagem.")
                                                aiRepository.editOrRemoveBackground(
                                                    bmp,
                                                    "Remove the background cleanly and isolate the main subject on a clean transparent or solid studio backdrop."
                                                ).fold(
                                                    onSuccess = { outBmp ->
                                                        val newUri = BitmapRenderer.saveBitmapToTempUri(context, outBmp, "nobg")
                                                        commitLayers(layers.map {
                                                            if (it.layerId == imgLayer.layerId) it.copy(imageUri = newUri.toString()) else it
                                                        })
                                                        onShowMessage("Fundo removido com IA!")
                                                    },
                                                    onFailure = { err ->
                                                        onShowMessage(err.localizedMessage ?: "Erro ao remover fundo com IA.")
                                                    }
                                                )
                                            } catch (e: Exception) {
                                                onShowMessage(e.localizedMessage ?: "Erro ao carregar imagem.")
                                            } finally {
                                                aiBusy = false
                                            }
                                        }
                                    },
                                    onUpdateLayer = { updated ->
                                        commitLayers(layers.map { if (it.layerId == updated.layerId) updated else it }, recordUndo = false)
                                    }
                                )
                                EditorTab.ELEMENTS -> ElementsToolsPanel(
                                    selectedLayer = primarySelected,
                                    onAddShape = { kind ->
                                        val shape = DesignLayer(
                                            layerId = "shp_${UUID.randomUUID().toString().take(8)}",
                                            type = LayerType.SHAPE,
                                            name = "Forma ${kind.name}",
                                            xFraction = 0.5f,
                                            yFraction = 0.5f,
                                            widthFraction = 0.36f,
                                            heightFraction = 0.24f,
                                            colorHex = "#FF7A00",
                                            shapeKind = kind
                                        )
                                        commitLayers(layers + shape)
                                        selectedIds = setOf(shape.layerId)
                                    },
                                    onAddLine = {
                                        val line = DesignLayer(
                                            layerId = "ln_${UUID.randomUUID().toString().take(8)}",
                                            type = LayerType.LINE,
                                            name = "Linha",
                                            xFraction = 0.5f,
                                            yFraction = 0.5f,
                                            widthFraction = 0.65f,
                                            heightFraction = 0.05f,
                                            colorHex = "#FFFFFF"
                                        )
                                        commitLayers(layers + line)
                                        selectedIds = setOf(line.layerId)
                                    },
                                    onUpdateLayer = { updated ->
                                        commitLayers(layers.map { if (it.layerId == updated.layerId) updated else it }, recordUndo = false)
                                    }
                                )
                                EditorTab.LAYERS -> LayersPanel(
                                    layers = layers,
                                    selectedLayerIds = selectedIds,
                                    onSelectLayer = { id, multi ->
                                        selectedIds = if (multi && selectedIds.isNotEmpty() && id !in selectedIds) {
                                            selectedIds + id
                                        } else {
                                            setOf(id)
                                        }
                                    },
                                    onMoveLayer = { from, to ->
                                        val mutable = layers.toMutableList()
                                        val item = mutable.removeAt(from)
                                        mutable.add(to, item)
                                        commitLayers(mutable)
                                    },
                                    onDuplicateLayer = { layer ->
                                        val copy = layer.copy(
                                            layerId = "dup_${UUID.randomUUID().toString().take(8)}",
                                            name = "${layer.name} (Cópia)",
                                            xFraction = (layer.xFraction + 0.05f).coerceAtMost(0.9f),
                                            yFraction = (layer.yFraction + 0.05f).coerceAtMost(0.9f)
                                        )
                                        commitLayers(layers + copy)
                                        selectedIds = setOf(copy.layerId)
                                    },
                                    onToggleLock = { layer ->
                                        commitLayers(layers.map { if (it.layerId == layer.layerId) it.copy(locked = !it.locked) else it })
                                    },
                                    onToggleVisible = { layer ->
                                        commitLayers(layers.map { if (it.layerId == layer.layerId) it.copy(visible = !it.visible) else it })
                                    },
                                    onGroupSelected = {
                                        val gid = UUID.randomUUID().toString().take(8)
                                        commitLayers(layers.map { if (it.layerId in selectedIds) it.copy(groupId = gid) else it })
                                        onShowMessage("Camadas agrupadas!")
                                    },
                                    onDeleteLayer = { id ->
                                        commitLayers(layers.filterNot { it.layerId == id })
                                        selectedIds = selectedIds - id
                                    }
                                )
                                EditorTab.BACKGROUND -> BackgroundAndFormatPanel(
                                    currentBgStart = project.backgroundColorHex,
                                    currentBgEnd = project.backgroundGradientHex,
                                    currentWidth = project.width,
                                    currentHeight = project.height,
                                    hasBackgroundImage = layers.any {
                                        it.layerId == "bg_image_layer" ||
                                            (it.type == LayerType.IMAGE && it.widthFraction >= 0.99f && it.heightFraction >= 0.99f)
                                    },
                                    onChangeBackground = { start, end ->
                                        project = project.copy(backgroundColorHex = start, backgroundGradientHex = end)
                                    },
                                    onPickBackgroundImage = {
                                        bgImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                    onClearBackgroundImage = {
                                        commitLayers(layers.filterNot {
                                            it.layerId == "bg_image_layer" ||
                                                (it.type == LayerType.IMAGE && it.widthFraction >= 0.99f && it.heightFraction >= 0.99f)
                                        })
                                        onShowMessage("Imagem de fundo removida.")
                                    },
                                    onResizeCanvas = { name, w, h ->
                                        project = project.copy(formatName = name, width = w.toLong(), height = h.toLong())
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Tab Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EditorTab.entries.forEach { tab ->
                            val isSelected = activeTab == tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (activeTab == tab) toolsExpanded = !toolsExpanded
                                        else {
                                            activeTab = tab
                                            toolsExpanded = true
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("editor_tab_${tab.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) OrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Workspace Canvas Area with Zoom & Pan + Alignment Guides
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NavyDeep)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        canvasZoom = (canvasZoom * zoom).coerceIn(0.6f, 2.5f)
                        canvasPan += pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val aspect = (project.width.toFloat() / project.height.coerceAtLeast(1L).toFloat()).coerceIn(0.4f, 2.2f)
            val isTransparentBg = project.backgroundColorHex.equals("transparent", ignoreCase = true)
            val bgStart = if (isTransparentBg) Color.Transparent else parseComposeColor(project.backgroundColorHex, Color(0xFF0A1628))
            val bgEnd = if (isTransparentBg) Color.Transparent else (project.backgroundGradientHex?.let { parseComposeColor(it, bgStart) } ?: bgStart)

            BoxWithConstraints(
                modifier = Modifier
                    .padding(14.dp)
                    .aspectRatio(aspect)
                    .graphicsLayer(
                        scaleX = canvasZoom,
                        scaleY = canvasZoom,
                        translationX = canvasPan.x,
                        translationY = canvasPan.y
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (!isTransparentBg) Modifier.background(Brush.verticalGradient(listOf(bgStart, bgEnd)))
                        else Modifier
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            ) {
                val canvasW = maxWidth
                val canvasH = maxHeight
                val scale = min(canvasW.value, canvasH.value) / 360f

                if (isTransparentBg) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cell = 24f
                        val cols = (size.width / cell).toInt() + 1
                        val rows = (size.height / cell).toInt() + 1
                        for (r in 0..rows) {
                            for (c in 0..cols) {
                                val light = (r + c) % 2 == 0
                                drawRect(
                                    color = if (light) Color(0xFF2B3548) else Color(0xFF1B2333),
                                    topLeft = Offset(c * cell, r * cell),
                                    size = Size(cell, cell)
                                )
                            }
                        }
                    }
                }

                // Render ALL layers in EXACT Z-order (shapes, images, stickers, lines, texts)
                for (layer in layers) {
                    if (!layer.visible) continue
                    val isSelected = layer.layerId in selectedIds
                    val boxW = canvasW * layer.widthFraction
                    val boxH = canvasH * layer.heightFraction

                    Box(
                        modifier = Modifier
                            .offset(
                                x = canvasW * (layer.xFraction - layer.widthFraction / 2f),
                                y = canvasH * (layer.yFraction - layer.heightFraction / 2f)
                            )
                            .size(width = boxW, height = boxH)
                            .rotate(layer.rotation)
                            .then(
                                if (isSelected) Modifier.border(2.dp, OrangeAccent, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable {
                                selectedIds = setOf(layer.layerId)
                                when (layer.type) {
                                    LayerType.TEXT -> activeTab = EditorTab.TEXT
                                    LayerType.IMAGE -> activeTab = EditorTab.IMAGE
                                    LayerType.SHAPE, LayerType.STICKER, LayerType.LINE -> activeTab = EditorTab.ELEMENTS
                                }
                            }
                            .pointerInput(layer.layerId, layer.locked) {
                                if (!layer.locked) {
                                    var dragInitialLayers: List<DesignLayer>? = null
                                    detectDragGestures(
                                        onDragStart = {
                                            selectedIds = setOf(layer.layerId)
                                            dragInitialLayers = layers
                                        },
                                        onDragEnd = {
                                            snapCenterX = false
                                            snapCenterY = false
                                            val startSnapshot = dragInitialLayers
                                            if (startSnapshot != null && startSnapshot != layers) {
                                                undoStack.add(startSnapshot)
                                                if (undoStack.size > 30) undoStack.removeAt(0)
                                                redoStack.clear()
                                            }
                                            dragInitialLayers = null
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val current = layers.firstOrNull { it.layerId == layer.layerId } ?: return@detectDragGestures
                                            val dx = dragAmount.x / (canvasW.toPx().coerceAtLeast(1f))
                                            val dy = dragAmount.y / (canvasH.toPx().coerceAtLeast(1f))
                                            var nx = (current.xFraction + dx).coerceIn(0.05f, 0.95f)
                                            var ny = (current.yFraction + dy).coerceIn(0.05f, 0.95f)
                                            snapCenterX = abs(nx - 0.5f) < 0.025f
                                            snapCenterY = abs(ny - 0.5f) < 0.025f
                                            if (snapCenterX) nx = 0.5f
                                            if (snapCenterY) ny = 0.5f
                                            val deltaX = nx - current.xFraction
                                            val deltaY = ny - current.yFraction
                                            layers = layers.map {
                                                if (it.layerId == current.layerId) {
                                                    it.copy(xFraction = nx, yFraction = ny)
                                                } else if (current.groupId != null && it.groupId == current.groupId) {
                                                    it.copy(
                                                        xFraction = (it.xFraction + deltaX).coerceIn(0.05f, 0.95f),
                                                        yFraction = (it.yFraction + deltaY).coerceIn(0.05f, 0.95f)
                                                    )
                                                } else it
                                            }
                                            project = project.copy(layers = layers.map { it.toMap() })
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (layer.type) {
                            LayerType.SHAPE, LayerType.STICKER, LayerType.LINE -> {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val col = parseComposeColor(layer.colorHex, OrangeAccent)
                                        .copy(alpha = layer.opacity.coerceIn(0f, 1f))
                                    if (layer.type == LayerType.LINE) {
                                        drawLine(
                                            col,
                                            Offset(0f, size.height / 2f),
                                            Offset(size.width, size.height / 2f),
                                            strokeWidth = 6f,
                                            cap = StrokeCap.Round
                                        )
                                    } else {
                                        drawDesignShape(layer, col, baseStroke = 6f)
                                    }
                                }
                            }
                            LayerType.TEXT -> {
                                val fontSp = (layer.fontSizeSp * scale * 0.72f).coerceIn(9f, 52f).sp
                                Text(
                                    text = layer.text,
                                    color = parseComposeColor(layer.colorHex, Color.White).copy(alpha = layer.opacity.coerceIn(0f, 1f)),
                                    fontFamily = editorFontFamily(layer.fontFamilyKey),
                                    fontSize = fontSp,
                                    lineHeight = fontSp * 1.1f,
                                    fontWeight = if (layer.bold) FontWeight.ExtraBold else FontWeight.Normal,
                                    fontStyle = if (layer.italic) FontStyle.Italic else FontStyle.Normal,
                                    letterSpacing = layer.letterSpacing.sp,
                                    style = if (layer.shadowEnabled) {
                                        TextStyle(shadow = Shadow(color = Color.Black.copy(alpha = 0.65f), offset = Offset(2f, 3f), blurRadius = 6f))
                                    } else {
                                        TextStyle.Default
                                    },
                                    textAlign = when (layer.textAlign) {
                                        "left" -> TextAlign.Start
                                        "right" -> TextAlign.End
                                        else -> TextAlign.Center
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            LayerType.IMAGE -> {
                                val maskShape = when (layer.maskKind) {
                                    ImageMaskKind.CIRCLE -> CircleShape
                                    ImageMaskKind.ROUNDED -> RoundedCornerShape(16.dp)
                                    ImageMaskKind.NONE -> RoundedCornerShape(0.dp)
                                }
                                val cm = buildComposeImageColorMatrix(layer)
                                val resId = TemplateSeedCatalog.resolveDrawableResId(layer.imageUri)
                                if (resId != null) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = layer.name,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = ColorFilter.colorMatrix(cm),
                                        alpha = layer.opacity.coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(maskShape)
                                    )
                                } else if (!layer.imageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(Uri.parse(layer.imageUri))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = layer.name,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = ColorFilter.colorMatrix(cm),
                                        alpha = layer.opacity.coerceIn(0f, 1f),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(maskShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(maskShape)
                                            .background(Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Overlay alignment grid & dynamic snap guides
                if (showGrid || snapCenterX || snapCenterY) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (showGrid) {
                            val gridColor = Color.White.copy(alpha = 0.18f)
                            for (i in 1..3) {
                                drawLine(gridColor, Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), strokeWidth = 1.5f)
                                drawLine(gridColor, Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), strokeWidth = 1.5f)
                            }
                        }
                        if (snapCenterX) {
                            drawLine(CyanGlow, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = 2.5f)
                        }
                        if (snapCenterY) {
                            drawLine(CyanGlow, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 2.5f)
                        }
                    }
                }
            }

            // Reset zoom pill if zoomed
            if (canvasZoom != 1f || canvasPan != Offset.Zero) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable {
                            canvasZoom = 1f
                            canvasPan = Offset.Zero
                        }
                ) {
                    Text(
                        "Zoom ${(canvasZoom * 100).toInt()}% • Repor",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        var tempTitle by remember(project.title) { mutableStateOf(project.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Nome do Projeto") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    singleLine = true,
                    label = { Text("Título") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    project = project.copy(title = tempTitle.ifBlank { "Novo Design" })
                    showRenameDialog = false
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showExportDialog) {
        ExportImageDialog(
            projectTitle = project.title,
            defaultWidth = project.width.toInt(),
            defaultHeight = project.height.toInt(),
            creditsBalance = creditsBalance,
            isExporting = isExporting,
            onDismiss = { showExportDialog = false },
            onGoToCredits = {
                showExportDialog = false
                onGoToCredits()
            },
            onConfirmExport = { isPng, targetW, targetH ->
                isExporting = true
                scope.launch {
                    val typeKey = if (isPng) "image_png" else "image_jpg"
                    val resLabel = "${targetW}x${targetH}"
                    val reserveResult = onReserveExport(
                        project.id,
                        project.title,
                        typeKey,
                        resLabel,
                        2L,
                        "${project.title}.${if (isPng) "png" else "jpg"}"
                    )
                    reserveResult.fold(
                        onSuccess = { exportId ->
                            val renderResult = BitmapRenderer.exportProjectToImage(
                                context = context,
                                project = project,
                                targetWidth = targetW,
                                targetHeight = targetH,
                                isPng = isPng
                            )
                            renderResult.fold(
                                onSuccess = { fileRes ->
                                    onCompleteExport(exportId, 2L, typeKey, fileRes.fileSizeBytes)
                                    isExporting = false
                                    showExportDialog = false
                                    exportedResult = fileRes
                                    onShowMessage("Imagem exportada para a Galeria! (-2 créditos)")
                                },
                                onFailure = { err ->
                                    onFailExport(exportId, false)
                                    isExporting = false
                                    onShowMessage("Erro na exportação (créditos preservados): ${err.localizedMessage}")
                                }
                            )
                        },
                        onFailure = { err ->
                            isExporting = false
                            onShowMessage(err.localizedMessage ?: "Créditos insuficientes.")
                        }
                    )
                }
            }
        )
    }

    exportedResult?.let { res ->
        AlertDialog(
            onDismissRequest = { exportedResult = null },
            icon = { Icon(Icons.Filled.Check, contentDescription = null, tint = OrangeAccent) },
            title = { Text("Exportação Concluída!") },
            text = {
                Text("Ficheiro \"${res.fileName}\" (${res.fileSizeBytes / 1024} KB) guardado em Pictures/CANVA AO.")
            },
            confirmButton = {
                Button(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = if (res.fileName.endsWith(".png")) "image/png" else "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, res.shareUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Partilhar Design"))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partilhar")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedResult = null }) { Text("Fechar") }
            }
        )
    }
}

private fun buildComposeImageColorMatrix(layer: DesignLayer): ColorMatrix {
    val sat = when (layer.filterKind) {
        ImageFilterKind.BW -> 0f
        ImageFilterKind.VIVID -> (layer.saturation * 1.45f).coerceIn(0f, 2.5f)
        else -> layer.saturation
    }
    val cm = ColorMatrix().apply { setToSaturation(sat) }
    val contrast = when (layer.filterKind) {
        ImageFilterKind.VIVID -> layer.contrast * 1.15f
        ImageFilterKind.VINTAGE -> layer.contrast * 0.9f
        else -> layer.contrast
    }
    val translate = (-0.5f * contrast + 0.5f) * 255f + (layer.brightness * 120f)
    cm.timesAssign(
        ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    )
    when (layer.filterKind) {
        ImageFilterKind.WARM -> cm.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    1.1f, 0f, 0f, 0f, 12f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.88f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        ImageFilterKind.COOL -> cm.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    0.9f, 0f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f, 4f,
                    0f, 0f, 1.15f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        ImageFilterKind.VINTAGE -> cm.timesAssign(
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
