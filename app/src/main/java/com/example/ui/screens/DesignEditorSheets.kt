package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.model.DesignFormats
import com.example.core.model.DesignLayer
import com.example.core.model.ImageFilterKind
import com.example.core.model.ImageMaskKind
import com.example.core.model.LayerType
import com.example.core.model.ShapeKind
import com.example.ui.components.parseComposeColor
import com.example.ui.theme.OrangeAccent

val EditorColorSwatches = listOf(
    "#FFFFFF", "#000000", "#0A1628", "#FF7A00", "#FF9500", "#1D9BF0",
    "#EF4444", "#10B981", "#06B6D4", "#4F46E5", "#7C3AED", "#EC4899",
    "#FACC15", "#1E293B", "#F8FAFC", "#B45309"
)

val EditorBackgroundPresets = listOf(
    Pair("#0A1628", "#1A325C"),
    Pair("#07101F", "#FF7A00"),
    Pair("#4F46E5", "#7C3AED"),
    Pair("#0F172A", "#991B1B"),
    Pair("#064E3B", "#022C22"),
    Pair("#3B1D60", "#EA580C"),
    Pair("#18181B", "#3F3F46"),
    Pair("#F8FAFC", "#E2E8F0"),
    Pair("#FFF8F1", "#FDE68A"),
    Pair("#030712", "#1D4ED8")
)

val EditorFontOptions = listOf(
    "plus_jakarta_sans" to "Jakarta",
    "inter" to "Inter",
    "montserrat" to "Montserrat",
    "playfair_display" to "Playfair",
    "oswald" to "Oswald",
    "pacifico" to "Pacifico"
)

@Composable
fun TextToolsPanel(
    selectedLayer: DesignLayer?,
    onAddText: () -> Unit,
    onUpdateLayer: (DesignLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ferramentas de Texto", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onAddText,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("editor_add_text_button")
            ) {
                Text("+ Novo Texto")
            }
        }

        if (selectedLayer?.type != LayerType.TEXT) {
            Text(
                "Selecione uma camada de texto no canvas ou toque em \"+ Novo Texto\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            OutlinedTextField(
                value = selectedLayer.text,
                onValueChange = { onUpdateLayer(selectedLayer.copy(text = it)) },
                label = { Text("Conteúdo do texto") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EditorFontOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedLayer.fontFamilyKey == key,
                        onClick = { onUpdateLayer(selectedLayer.copy(fontFamilyKey = key)) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onUpdateLayer(selectedLayer.copy(bold = !selectedLayer.bold)) }) {
                    Icon(
                        Icons.Filled.FormatBold,
                        contentDescription = "Negrito",
                        tint = if (selectedLayer.bold) OrangeAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { onUpdateLayer(selectedLayer.copy(italic = !selectedLayer.italic)) }) {
                    Icon(
                        Icons.Filled.FormatItalic,
                        contentDescription = "Itálico",
                        tint = if (selectedLayer.italic) OrangeAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { onUpdateLayer(selectedLayer.copy(textAlign = "left")) }) {
                    Icon(
                        Icons.Filled.FormatAlignLeft,
                        contentDescription = "Esquerda",
                        tint = if (selectedLayer.textAlign == "left") OrangeAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { onUpdateLayer(selectedLayer.copy(textAlign = "center")) }) {
                    Icon(
                        Icons.Filled.FormatAlignCenter,
                        contentDescription = "Centro",
                        tint = if (selectedLayer.textAlign == "center") OrangeAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { onUpdateLayer(selectedLayer.copy(textAlign = "right")) }) {
                    Icon(
                        Icons.Filled.FormatAlignRight,
                        contentDescription = "Direita",
                        tint = if (selectedLayer.textAlign == "right") OrangeAccent else MaterialTheme.colorScheme.onSurface
                    )
                }
                FilterChip(
                    selected = selectedLayer.outlineEnabled,
                    onClick = { onUpdateLayer(selectedLayer.copy(outlineEnabled = !selectedLayer.outlineEnabled)) },
                    label = { Text("Contorno") }
                )
                FilterChip(
                    selected = selectedLayer.shadowEnabled,
                    onClick = { onUpdateLayer(selectedLayer.copy(shadowEnabled = !selectedLayer.shadowEnabled)) },
                    label = { Text("Sombra") }
                )
                FilterChip(
                    selected = selectedLayer.curved,
                    onClick = { onUpdateLayer(selectedLayer.copy(curved = !selectedLayer.curved)) },
                    label = { Text("Curvo") }
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EditorColorSwatches) { hex ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(parseComposeColor(hex, Color.White))
                            .border(
                                width = if (selectedLayer.colorHex.equals(hex, true)) 3.dp else 1.dp,
                                color = if (selectedLayer.colorHex.equals(hex, true)) OrangeAccent else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onUpdateLayer(selectedLayer.copy(colorHex = hex)) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tamanho", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                Slider(
                    value = selectedLayer.fontSizeSp,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(fontSizeSp = it)) },
                    valueRange = 10f..72f,
                    colors = SliderDefaults.colors(thumbColor = OrangeAccent, activeTrackColor = OrangeAccent),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opacidade", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                Slider(
                    value = selectedLayer.opacity,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(opacity = it)) },
                    valueRange = 0.1f..1f,
                    colors = SliderDefaults.colors(thumbColor = OrangeAccent, activeTrackColor = OrangeAccent),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rotação", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                Slider(
                    value = selectedLayer.rotation,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(rotation = it)) },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(thumbColor = OrangeAccent, activeTrackColor = OrangeAccent),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ImageToolsPanel(
    selectedLayer: DesignLayer?,
    aiBusy: Boolean,
    onImportPhoto: () -> Unit,
    onReplacePhoto: (DesignLayer) -> Unit,
    onUseAsBackground: (DesignLayer) -> Unit,
    onRemoveBackgroundAi: (DesignLayer) -> Unit,
    onUpdateLayer: (DesignLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ferramentas de Imagem", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onImportPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("editor_import_photo_button")
            ) {
                Text("+ Importar Foto")
            }
        }

        if (selectedLayer?.type != LayerType.IMAGE) {
            Text(
                "Toque numa imagem do template ou importe uma foto para ajustar tamanho, filtros, máscaras ou remover o fundo com IA.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Button(
                        onClick = { onRemoveBackgroundAi(selectedLayer) },
                        enabled = !aiBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (aiBusy) "A processar..." else "Remover Fundo IA")
                    }
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { onReplacePhoto(selectedLayer) },
                        label = { Text("Substituir Foto") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedLayer.widthFraction >= 0.99f && selectedLayer.heightFraction >= 0.99f,
                        onClick = { onUseAsBackground(selectedLayer) },
                        label = { Text("Usar como Fundo") }
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ImageFilterKind.entries) { filter ->
                    val label = when (filter) {
                        ImageFilterKind.NORMAL -> "Original"
                        ImageFilterKind.VIVID -> "Vívido"
                        ImageFilterKind.WARM -> "Quente"
                        ImageFilterKind.COOL -> "Frio"
                        ImageFilterKind.BW -> "P&B"
                        ImageFilterKind.VINTAGE -> "Vintage"
                    }
                    FilterChip(
                        selected = selectedLayer.filterKind == filter,
                        onClick = { onUpdateLayer(selectedLayer.copy(filterKind = filter)) },
                        label = { Text(label) }
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ImageMaskKind.entries) { mask ->
                    FilterChip(
                        selected = selectedLayer.maskKind == mask,
                        onClick = { onUpdateLayer(selectedLayer.copy(maskKind = mask)) },
                        label = {
                            Text(
                                when (mask) {
                                    ImageMaskKind.NONE -> "Sem Máscara"
                                    ImageMaskKind.ROUNDED -> "Arredondado"
                                    ImageMaskKind.CIRCLE -> "Círculo"
                                }
                            )
                        }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tamanho", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                Slider(
                    value = selectedLayer.widthFraction.coerceIn(0.15f, 1.0f),
                    onValueChange = { newSize ->
                        val ratio = (selectedLayer.heightFraction / selectedLayer.widthFraction.coerceAtLeast(0.1f)).coerceIn(0.35f, 1.6f)
                        onUpdateLayer(
                            selectedLayer.copy(
                                widthFraction = newSize,
                                heightFraction = (newSize * ratio).coerceIn(0.15f, 1.0f)
                            )
                        )
                    },
                    valueRange = 0.15f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = OrangeAccent, activeTrackColor = OrangeAccent),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opacidade", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                Slider(
                    value = selectedLayer.opacity,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(opacity = it)) },
                    valueRange = 0.1f..1f,
                    colors = SliderDefaults.colors(thumbColor = OrangeAccent, activeTrackColor = OrangeAccent),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Brilho", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                Slider(
                    value = selectedLayer.brightness,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(brightness = it)) },
                    valueRange = -0.5f..0.5f,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Contraste", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                Slider(
                    value = selectedLayer.contrast,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(contrast = it)) },
                    valueRange = 0.5f..1.8f,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Saturação", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(72.dp))
                Slider(
                    value = selectedLayer.saturation,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(saturation = it)) },
                    valueRange = 0f..2f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ElementsToolsPanel(
    selectedLayer: DesignLayer?,
    onAddShape: (ShapeKind) -> Unit,
    onAddLine: () -> Unit,
    onUpdateLayer: (DesignLayer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Formas, Adesivos & Molduras", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ShapeKind.entries) { kind ->
                FilterChip(
                    selected = selectedLayer?.shapeKind == kind && selectedLayer.type == LayerType.SHAPE,
                    onClick = { onAddShape(kind) },
                    label = {
                        Text(
                            when (kind) {
                                ShapeKind.RECTANGLE -> "Retângulo"
                                ShapeKind.ROUNDED_RECT -> "Cartão"
                                ShapeKind.CIRCLE -> "Círculo"
                                ShapeKind.TRIANGLE -> "Triângulo"
                                ShapeKind.STAR -> "Estrela"
                                ShapeKind.BADGE -> "Selo"
                                ShapeKind.ARROW -> "Seta"
                                ShapeKind.FRAME -> "Moldura"
                                ShapeKind.VERIFIED_BADGE -> "Verificado"
                                ShapeKind.QUOTE_MARKS -> "Aspas"
                                ShapeKind.BOTTOM_ARCH -> "Arco"
                                ShapeKind.DIAMOND_LINE -> "Linha Losango"
                                ShapeKind.DOT_GRID -> "Pontos"
                                ShapeKind.BRACKET_FRAME -> "Chaveta"
                                ShapeKind.PILL_OUTLINE -> "Pílula"
                                ShapeKind.SCRIM_BOTTOM -> "Degradê Escuro"
                            }
                        )
                    }
                )
            }
            item {
                FilterChip(selected = false, onClick = onAddLine, label = { Text("Linha") })
            }
        }

        if (selectedLayer != null && (selectedLayer.type == LayerType.SHAPE || selectedLayer.type == LayerType.LINE || selectedLayer.type == LayerType.STICKER)) {
            Text("Cor & Opacidade do Elemento", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EditorColorSwatches) { hex ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(parseComposeColor(hex, OrangeAccent))
                            .border(
                                width = if (selectedLayer.colorHex.equals(hex, true)) 3.dp else 1.dp,
                                color = if (selectedLayer.colorHex.equals(hex, true)) Color.White else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onUpdateLayer(selectedLayer.copy(colorHex = hex)) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tamanho", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                Slider(
                    value = selectedLayer.widthFraction,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(widthFraction = it, heightFraction = it.coerceAtMost(0.95f))) },
                    valueRange = 0.12f..0.95f,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Opacidade", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(68.dp))
                Slider(
                    value = selectedLayer.opacity,
                    onValueChange = { onUpdateLayer(selectedLayer.copy(opacity = it)) },
                    valueRange = 0.1f..1f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LayersPanel(
    layers: List<DesignLayer>,
    selectedLayerIds: Set<String>,
    onSelectLayer: (String, Boolean) -> Unit,
    onMoveLayer: (Int, Int) -> Unit,
    onDuplicateLayer: (DesignLayer) -> Unit,
    onToggleLock: (DesignLayer) -> Unit,
    onToggleVisible: (DesignLayer) -> Unit,
    onGroupSelected: () -> Unit,
    onDeleteLayer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Camadas (${layers.size})", style = MaterialTheme.typography.titleMedium)
            if (selectedLayerIds.size >= 2) {
                Button(onClick = onGroupSelected, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(Icons.Filled.GroupWork, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agrupar (${selectedLayerIds.size})")
                }
            }
        }
        LazyColumn(
            modifier = Modifier.height(155.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(layers.reversed(), key = { _, l -> l.layerId }) { revIdx, layer ->
                val actualIdx = layers.lastIndex - revIdx
                val selected = layer.layerId in selectedLayerIds
                Surface(
                    color = if (selected) OrangeAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLayer(layer.layerId, true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layer.name.ifBlank { layer.type.name },
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                            if (layer.groupId != null) {
                                Text("Grupo: ${layer.groupId.take(6)}", style = MaterialTheme.typography.labelSmall, color = OrangeAccent)
                            }
                        }
                        IconButton(onClick = { if (actualIdx < layers.lastIndex) onMoveLayer(actualIdx, actualIdx + 1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { if (actualIdx > 0) onMoveLayer(actualIdx, actualIdx - 1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Descer", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDuplicateLayer(layer) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicar", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onToggleLock(layer) }, modifier = Modifier.size(32.dp)) {
                            Icon(if (layer.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = "Bloquear", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onToggleVisible(layer) }, modifier = Modifier.size(32.dp)) {
                            Icon(if (layer.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Visibilidade", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDeleteLayer(layer.layerId) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundAndFormatPanel(
    currentBgStart: String,
    currentBgEnd: String?,
    currentWidth: Long,
    currentHeight: Long,
    hasBackgroundImage: Boolean,
    onChangeBackground: (String, String?) -> Unit,
    onPickBackgroundImage: () -> Unit,
    onClearBackgroundImage: () -> Unit,
    onResizeCanvas: (String, Int, Int) -> Unit
) {
    var customW by remember(currentWidth) { mutableStateOf(currentWidth.toString()) }
    var customH by remember(currentHeight) { mutableStateOf(currentHeight.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fundo do Canvas", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = hasBackgroundImage,
                    onClick = onPickBackgroundImage,
                    label = { Text(if (hasBackgroundImage) "Trocar Imagem" else "+ Imagem Fundo") }
                )
                if (hasBackgroundImage) {
                    FilterChip(
                        selected = false,
                        onClick = onClearBackgroundImage,
                        label = { Text("Remover Fundo") }
                    )
                }
            }
        }

        Text("Cores Sólidas & Transparente", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            item {
                FilterChip(
                    selected = currentBgStart.equals("transparent", ignoreCase = true),
                    onClick = { onChangeBackground("transparent", null) },
                    label = { Text("Transparente") }
                )
            }
            items(EditorColorSwatches) { hex ->
                val active = currentBgStart.equals(hex, true) && currentBgEnd == null
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(parseComposeColor(hex, Color.Black))
                        .border(if (active) 3.dp else 1.dp, if (active) OrangeAccent else Color.Gray, CircleShape)
                        .clickable { onChangeBackground(hex, null) }
                )
            }
        }

        Text("Gradientes", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(EditorBackgroundPresets) { (start, end) ->
                val active = currentBgStart.equals(start, true) && (currentBgEnd ?: start).equals(end, true)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(parseComposeColor(start), parseComposeColor(end))))
                        .border(if (active) 3.dp else 1.dp, if (active) OrangeAccent else Color.Gray, RoundedCornerShape(10.dp))
                        .clickable { onChangeBackground(start, end) }
                )
            }
        }

        Text("Redimensionar Formato", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DesignFormats.presets.filter { it.id != "custom" }) { fmt ->
                FilterChip(
                    selected = currentWidth.toInt() == fmt.width && currentHeight.toInt() == fmt.height,
                    onClick = { onResizeCanvas(fmt.name, fmt.width, fmt.height) },
                    label = { Text("${fmt.name} (${fmt.width}×${fmt.height})") }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = customW,
                onValueChange = { if (it.all(Char::isDigit)) customW = it.take(4) },
                label = { Text("L") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = customH,
                onValueChange = { if (it.all(Char::isDigit)) customH = it.take(4) },
                label = { Text("A") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                val w = (customW.toIntOrNull() ?: 1080).coerceIn(200, 4096)
                val h = (customH.toIntOrNull() ?: 1080).coerceIn(200, 4096)
                onResizeCanvas("Personalizado", w, h)
            }) {
                Text("Aplicar")
            }
        }
    }
}

@Composable
fun ExportImageDialog(
    projectTitle: String,
    defaultWidth: Int,
    defaultHeight: Int,
    creditsBalance: Long,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onGoToCredits: () -> Unit,
    onConfirmExport: (isPng: Boolean, width: Int, height: Int) -> Unit
) {
    var isPng by remember { mutableStateOf(true) }
    var scaleMultiplier by remember { mutableStateOf(1) }
    val targetW = (defaultWidth * scaleMultiplier).coerceIn(200, 4096)
    val targetH = (defaultHeight * scaleMultiplier).coerceIn(200, 4096)
    val hasEnough = creditsBalance >= 2L

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text("Exportar Imagem") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Projeto: $projectTitle", style = MaterialTheme.typography.bodyMedium)
                Text("Formato do ficheiro:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isPng,
                        onClick = { isPng = true },
                        label = { Text("PNG (Sem perdas)") }
                    )
                    FilterChip(
                        selected = !isPng,
                        onClick = { isPng = false },
                        label = { Text("JPG (Compacto)") }
                    )
                }
                Text("Resolução: $targetW × $targetH px", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = scaleMultiplier == 1,
                        onClick = { scaleMultiplier = 1 },
                        label = { Text("1x Padrão") }
                    )
                    FilterChip(
                        selected = scaleMultiplier == 2,
                        onClick = { scaleMultiplier = 2 },
                        label = { Text("2x Alta Def.") }
                    )
                }
                Surface(
                    color = if (hasEnough) OrangeAccent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (hasEnough) {
                            "Custo: 2 créditos (Saldo atual: $creditsBalance). Os créditos só são descontados após a conclusão."
                        } else {
                            "Saldo insuficiente ($creditsBalance créditos). Precisa de 2 créditos para exportar imagem."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasEnough) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (hasEnough) {
                Button(
                    onClick = { onConfirmExport(isPng, targetW, targetH) },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_export_image_button")
                ) {
                    Text(if (isExporting) "A exportar..." else "Exportar (-2 créditos)")
                }
            } else {
                Button(onClick = onGoToCredits) {
                    Text("Ganhar Créditos (+2)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExporting) { Text("Cancelar") }
        }
    )
}
