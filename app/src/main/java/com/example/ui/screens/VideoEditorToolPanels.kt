package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ProAudioTrack
import com.example.core.model.ProEffectKind
import com.example.core.model.ProOverlayItem
import com.example.core.model.SpeedCurvePreset
import com.example.core.model.VideoBackgroundAnimatedStyle
import com.example.core.model.VideoBlendMode
import com.example.core.model.VideoClipItem
import com.example.core.model.VideoFilterKind
import com.example.core.model.VideoKeyframe
import com.example.core.model.VideoMaskShape
import com.example.core.model.VideoOverlayKind
import com.example.core.model.VideoTransitionKind
import com.example.engine.audio.BuiltInAudioPreset
import com.example.engine.audio.VideoAudioLibraryCatalog
import com.example.engine.video.ImportedFontInfo
import com.example.ui.components.parseComposeColor
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.SuccessGreen

enum class ProVideoToolTab(val label: String, val icon: ImageVector) {
    EDIT("Editar", Icons.Filled.ContentCut),
    AUDIO("Áudio", Icons.Filled.MusicNote),
    TEXT("Texto", Icons.Filled.Subtitles),
    LAYERS("Camadas", Icons.Filled.Layers),
    EFFECTS("Efeitos", Icons.Filled.Filter),
    ANIMATE("Animar", Icons.Filled.Animation),
    TRANSITIONS("Transição", Icons.Filled.SwapHoriz),
    AI("IA Studio", Icons.Filled.AutoAwesome),
    BACKGROUNDS("Fundos", Icons.Filled.Palette)
}

@Composable
fun EditClipToolPanel(
    selectedClip: VideoClipItem?,
    selectedClipIndex: Int,
    clipCount: Int,
    selectedMultiCount: Int,
    onSplitAtPlayhead: () -> Unit,
    onDuplicateClip: () -> Unit,
    onReplaceClip: () -> Unit,
    onFreezeFrame: () -> Unit,
    onReverseClip: () -> Unit,
    onDeleteClips: () -> Unit,
    onUpdateClip: (VideoClipItem) -> Unit,
    onMoveClip: (Int) -> Unit
) {
    if (selectedClip == null) {
        Text("Adicione ou selecione um vídeo/foto na timeline para editar.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Primary Action Strip
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProActionButton("Dividir", Icons.Filled.ContentCut, onClick = onSplitAtPlayhead)
            ProActionButton("Duplicar", Icons.Filled.ContentCopy, onClick = onDuplicateClip)
            ProActionButton("Substituir", Icons.Filled.SwapHoriz, onClick = onReplaceClip)
            ProActionButton("Congelar", Icons.Filled.CameraAlt, onClick = onFreezeFrame)
            if (selectedClip.mediaType == "video") {
                ProActionButton(
                    if (selectedClip.reversed) "Normal" else "Reverso",
                    Icons.Filled.FastRewind,
                    highlighted = selectedClip.reversed,
                    onClick = onReverseClip
                )
            }
            ProActionButton(
                if (selectedMultiCount > 1) "Eliminar ($selectedMultiCount)" else "Eliminar",
                Icons.Filled.Delete,
                tint = MaterialTheme.colorScheme.error,
                onClick = onDeleteClips
            )
        }

        // Order & Transform Strip
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { onMoveClip(-1) },
                enabled = selectedClipIndex > 0
            ) { Text("← Mover") }
            OutlinedButton(
                onClick = { onMoveClip(1) },
                enabled = selectedClipIndex < clipCount - 1
            ) { Text("Mover →") }
            ProActionButton("Girar 90°", Icons.Filled.RotateRight) {
                onUpdateClip(selectedClip.copy(rotationDeg = (selectedClip.rotationDeg + 90f) % 360f))
            }
            ProActionButton("Espelhar H", Icons.Filled.Flip, highlighted = selectedClip.flipH) {
                onUpdateClip(selectedClip.copy(flipH = !selectedClip.flipH))
            }
            ProActionButton("Espelhar V", Icons.Filled.Flip, highlighted = selectedClip.flipV) {
                onUpdateClip(selectedClip.copy(flipV = !selectedClip.flipV))
            }
        }

        // Crop Mode
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recorte:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            listOf("fill" to "Preencher", "fit" to "Ajustar", "1:1" to "1:1", "4:5" to "4:5", "16:9" to "16:9").forEach { (k, lbl) ->
                FilterChip(
                    selected = selectedClip.cropMode == k,
                    onClick = { onUpdateClip(selectedClip.copy(cropMode = k)) },
                    label = { Text(lbl, fontSize = 11.sp) }
                )
            }
        }

        // Trim / Duration Control
        if (selectedClip.mediaType == "image") {
            val durSec = (selectedClip.trimEndMs - selectedClip.trimStartMs).coerceIn(1000L, 20000L) / 1000f
            LabeledSlider("Duração da Foto: %.1fs".format(durSec), durSec, 1f..20f) { v ->
                val ms = (v * 1000L).toLong()
                onUpdateClip(selectedClip.copy(durationMs = ms, trimStartMs = 0L, trimEndMs = ms))
            }
        } else {
            val dur = selectedClip.durationMs.coerceAtLeast(2000L).toFloat()
            LabeledSlider(
                "Início: %.1fs".format(selectedClip.trimStartMs / 1000f),
                selectedClip.trimStartMs.toFloat().coerceIn(0f, dur - 800f),
                0f..(dur - 800f)
            ) { v ->
                onUpdateClip(selectedClip.copy(trimStartMs = v.toLong().coerceAtMost(selectedClip.trimEndMs - 500L)))
            }
            LabeledSlider(
                "Fim: %.1fs".format(selectedClip.trimEndMs / 1000f),
                selectedClip.trimEndMs.toFloat().coerceIn(800f, dur),
                800f..dur
            ) { v ->
                onUpdateClip(selectedClip.copy(trimEndMs = v.toLong().coerceAtLeast(selectedClip.trimStartMs + 500L)))
            }
        }

        // Speed & Speed Curve
        Text("Velocidade (${selectedClip.speed}x) & Curva:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f).forEach { spd ->
                FilterChip(
                    selected = kotlin.math.abs(selectedClip.speed - spd) < 0.05f,
                    onClick = { onUpdateClip(selectedClip.copy(speed = spd)) },
                    label = { Text("${spd}x") }
                )
            }
        }
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpeedCurvePreset.entries.forEach { preset ->
                FilterChip(
                    selected = selectedClip.speedCurve == preset.key,
                    onClick = { onUpdateClip(selectedClip.copy(speedCurve = preset.key)) },
                    label = { Text(preset.label, fontSize = 11.sp) }
                )
            }
        }

        // Zoom & Pan Framing + Clip Volume
        LabeledSlider("Zoom do Clipe: %.2fx".format(selectedClip.zoomScale), selectedClip.zoomScale, 0.5f..3.0f) {
            onUpdateClip(selectedClip.copy(zoomScale = it))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                LabeledSlider("Posição X: %.2f".format(selectedClip.offsetX), selectedClip.offsetX, -0.8f..0.8f) {
                    onUpdateClip(selectedClip.copy(offsetX = it))
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                LabeledSlider("Posição Y: %.2f".format(selectedClip.offsetY), selectedClip.offsetY, -0.8f..0.8f) {
                    onUpdateClip(selectedClip.copy(offsetY = it))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Áudio Original do Clipe (${(selectedClip.volume * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
            Switch(checked = !selectedClip.muted, onCheckedChange = { onUpdateClip(selectedClip.copy(muted = !it)) })
        }
        if (!selectedClip.muted) {
            LabeledSlider("Volume Original", selectedClip.volume, 0f..1.5f) {
                onUpdateClip(selectedClip.copy(volume = it))
            }
        }
    }
}

@Composable
fun AudioStudioToolPanel(
    audioTracks: List<ProAudioTrack>,
    selectedAudioId: String?,
    previewingUri: String?,
    isRecordingVoice: Boolean,
    micLevel: Float,
    canExtractFromClip: Boolean,
    currentPosMs: Long,
    onImportDeviceAudio: () -> Unit,
    onToggleRecordVoice: () -> Unit,
    onExtractVideoAudio: () -> Unit,
    onPreviewPreset: (BuiltInAudioPreset) -> Unit,
    onStopPreview: () -> Unit,
    onAddPresetTrack: (BuiltInAudioPreset) -> Unit,
    onSelectTrack: (String) -> Unit,
    onUpdateTrack: (ProAudioTrack) -> Unit,
    onSplitTrackAtPlayhead: (String) -> Unit,
    onDeleteTrack: (String) -> Unit
) {
    val selectedTrack = audioTracks.firstOrNull { it.id == selectedAudioId } ?: audioTracks.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Primary Studio Controls
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProActionButton("Importar Áudio", Icons.Filled.MusicNote, onClick = onImportDeviceAudio)
            ProActionButton(
                if (isRecordingVoice) "Parar Voz" else "Gravar Voz",
                if (isRecordingVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                highlighted = isRecordingVoice,
                tint = if (isRecordingVoice) Color.Red else OrangeAccent,
                onClick = onToggleRecordVoice
            )
            if (canExtractFromClip) {
                ProActionButton("Extrair Áudio", Icons.Filled.GraphicEq, onClick = onExtractVideoAudio)
            }
        }

        if (isRecordingVoice) {
            Surface(color = Color.Red.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("A gravar voz do microfone... Fale agora!", color = Color.Red, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(progress = { micLevel }, color = Color.Red, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Built-in Music Library
        Text("Biblioteca de Músicas (Pré-visualizar ou Adicionar):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VideoAudioLibraryCatalog.musicPresets.forEach { preset ->
                val isPrev = previewingUri?.contains(preset.id) == true
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.border(1.dp, if (isPrev) CyanGlow else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (isPrev) onStopPreview() else onPreviewPreset(preset) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(if (isPrev) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Ouvir", tint = CyanGlow, modifier = Modifier.size(16.dp))
                        }
                        Column(modifier = Modifier.padding(end = 4.dp)) {
                            Text(preset.title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text(preset.durationLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onAddPresetTrack(preset) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "Adicionar", tint = OrangeAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // SFX Library
        Text("Efeitos Sonoros (SFX):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VideoAudioLibraryCatalog.sfxPresets.forEach { preset ->
                val isPrev = previewingUri?.contains(preset.id) == true
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (isPrev) onStopPreview() else onPreviewPreset(preset) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(if (isPrev) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(15.dp))
                        }
                        Text(preset.title, style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = { onAddPresetTrack(preset) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        // Active Tracks List & Individual Mixer
        if (audioTracks.isNotEmpty()) {
            Text("Faixas no Projeto (${audioTracks.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                audioTracks.forEach { trk ->
                    FilterChip(
                        selected = trk.id == selectedTrack?.id,
                        onClick = { onSelectTrack(trk.id) },
                        label = { Text(trk.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        if (selectedTrack != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedTrack.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { onUpdateTrack(selectedTrack.copy(muted = !selectedTrack.muted)) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (selectedTrack.muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Silenciar",
                                    tint = if (selectedTrack.muted) MaterialTheme.colorScheme.error else CyanGlow
                                )
                            }
                            IconButton(onClick = { onSplitTrackAtPlayhead(selectedTrack.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.ContentCut, contentDescription = "Dividir faixa", tint = OrangeAccent)
                            }
                            IconButton(onClick = { onDeleteTrack(selectedTrack.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remover faixa", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    LabeledSlider("Volume: ${(selectedTrack.volume * 100).toInt()}%", selectedTrack.volume, 0f..1.5f) {
                        onUpdateTrack(selectedTrack.copy(volume = it))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            LabeledSlider("Fade In: %.1fs".format(selectedTrack.fadeInMs / 1000f), selectedTrack.fadeInMs.toFloat(), 0f..3000f) {
                                onUpdateTrack(selectedTrack.copy(fadeInMs = it.toLong()))
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            LabeledSlider("Fade Out: %.1fs".format(selectedTrack.fadeOutMs / 1000f), selectedTrack.fadeOutMs.toFloat(), 0f..3000f) {
                                onUpdateTrack(selectedTrack.copy(fadeOutMs = it.toLong()))
                            }
                        }
                    }
                    LabeledSlider(
                        "Início Timeline: %.1fs".format(selectedTrack.startMs / 1000f),
                        selectedTrack.startMs.toFloat().coerceIn(0f, 60_000f),
                        0f..60_000f
                    ) {
                        onUpdateTrack(selectedTrack.copy(startMs = it.toLong()))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Redução de Ruído", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = selectedTrack.noiseReduction, onCheckedChange = { onUpdateTrack(selectedTrack.copy(noiseReduction = it)) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Isolamento Vocal", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = selectedTrack.vocalIsolation, onCheckedChange = { onUpdateTrack(selectedTrack.copy(vocalIsolation = it)) })
                    }
                    TextButton(onClick = { onUpdateTrack(selectedTrack.copy(startMs = currentPosMs)) }) {
                        Text("Sincronizar início com agulha (${currentPosMs / 1000f}s)")
                    }
                }
            }
        }
    }
}

@Composable
fun TextAndSubtitlesToolPanel(
    selectedOverlay: ProOverlayItem?,
    importedFonts: List<ImportedFontInfo>,
    onAddTextOverlay: (title: String, isSubtitle: Boolean, is3D: Boolean, bgHex: String?, gradStart: String?, gradEnd: String?) -> Unit,
    onImportCustomFont: () -> Unit,
    onGenerateTtsFromText: (String) -> Unit,
    onGenerateAiCaptions: () -> Unit,
    onUpdateOverlay: (ProOverlayItem) -> Unit,
    onDeleteOverlay: (String) -> Unit
) {
    var quickText by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = quickText,
                onValueChange = { quickText = it },
                placeholder = { Text("Texto ou legenda...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val txt = quickText.trim().ifBlank { "CANVA AO Studio" }
                    onAddTextOverlay(txt, false, false, null, null, null)
                    quickText = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) { Text("+ Texto") }
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProActionButton("+ Legenda Manual", Icons.Filled.Subtitles) {
                val txt = quickText.trim().ifBlank { "Legenda do vídeo" }
                onAddTextOverlay(txt, true, false, "#B3000000", null, null)
                quickText = ""
            }
            ProActionButton("Legendas IA", Icons.Filled.AutoAwesome, highlighted = true, onClick = onGenerateAiCaptions)
            ProActionButton("Texto -> Voz (TTS)", Icons.Filled.RecordVoiceOver) {
                val txt = quickText.trim().ifBlank { selectedOverlay?.text.orEmpty() }
                onGenerateTtsFromText(txt.ifBlank { "Bem-vindo ao CANVA AO!" })
            }
            ProActionButton("Importar Fonte", Icons.Filled.FontDownload, onClick = onImportCustomFont)
        }

        // Title Presets
        Text("Modelos de Títulos:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = false, onClick = {
                onAddTextOverlay("DESTAQUE 3D", false, true, null, "#FF7A00", "#FFD166")
            }, label = { Text("Título 3D Gradiente") })
            FilterChip(selected = false, onClick = {
                onAddTextOverlay("ÚLTIMA HORA", true, false, "#E11D48", null, null)
            }, label = { Text("Notícia Urgente") })
            FilterChip(selected = false, onClick = {
                onAddTextOverlay("ASSISTA ATÉ AO FIM", false, false, "#111827", null, null)
            }, label = { Text("Gancho Viral") })
            FilterChip(selected = false, onClick = {
                onAddTextOverlay("@suamarca", false, false, null, null, null)
            }, label = { Text("Marca d'Água") })
        }

        if (selectedOverlay != null && (selectedOverlay.kind == VideoOverlayKind.TEXT || selectedOverlay.kind == VideoOverlayKind.SUBTITLE || selectedOverlay.kind == VideoOverlayKind.WATERMARK)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Editar Texto Selecionado", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { onDeleteOverlay(selectedOverlay.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    OutlinedTextField(
                        value = selectedOverlay.text,
                        onValueChange = { onUpdateOverlay(selectedOverlay.copy(text = it)) },
                        label = { Text("Conteúdo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LabeledSlider("Tamanho: ${selectedOverlay.fontSizeSp.toInt()}sp", selectedOverlay.fontSizeSp, 12f..64f) {
                        onUpdateOverlay(selectedOverlay.copy(fontSizeSp = it))
                    }

                    // Font selector
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "plus_jakarta_sans" to "Jakarta",
                            "montserrat" to "Montserrat",
                            "oswald" to "Oswald",
                            "playfair_display" to "Playfair",
                            "pacifico" to "Pacifico",
                            "inter" to "Inter"
                        ).forEach { (k, lbl) ->
                            FilterChip(
                                selected = selectedOverlay.fontFamilyKey == k && selectedOverlay.customFontUri == null,
                                onClick = { onUpdateOverlay(selectedOverlay.copy(fontFamilyKey = k, customFontUri = null)) },
                                label = { Text(lbl, fontSize = 11.sp) }
                            )
                        }
                        importedFonts.forEach { f ->
                            FilterChip(
                                selected = selectedOverlay.customFontUri == f.fileUri,
                                onClick = { onUpdateOverlay(selectedOverlay.copy(customFontUri = f.fileUri)) },
                                label = { Text(f.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Colors + Gradient
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("#FFFFFF", "#000000", "#FF7A00", "#00E5FF", "#FACC15", "#EF4444", "#22C55E", "#A855F7").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseComposeColor(hex, Color.White))
                                    .border(if (selectedOverlay.colorHex == hex) 2.dp else 1.dp, Color.White, CircleShape)
                                    .clickable { onUpdateOverlay(selectedOverlay.copy(colorHex = hex, gradientStartHex = null, gradientEndHex = null)) }
                            )
                        }
                    }

                    // Styles toggles
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = selectedOverlay.is3DText, onClick = { onUpdateOverlay(selectedOverlay.copy(is3DText = !selectedOverlay.is3DText)) }, label = { Text("Texto 3D") })
                        FilterChip(selected = selectedOverlay.outlineEnabled, onClick = { onUpdateOverlay(selectedOverlay.copy(outlineEnabled = !selectedOverlay.outlineEnabled)) }, label = { Text("Contorno") })
                        FilterChip(selected = selectedOverlay.shadowEnabled, onClick = { onUpdateOverlay(selectedOverlay.copy(shadowEnabled = !selectedOverlay.shadowEnabled)) }, label = { Text("Sombra") })
                        FilterChip(selected = selectedOverlay.typewriterEffect, onClick = { onUpdateOverlay(selectedOverlay.copy(typewriterEffect = !selectedOverlay.typewriterEffect)) }, label = { Text("Escrita") })
                        FilterChip(selected = selectedOverlay.bgColorHex != null, onClick = {
                            onUpdateOverlay(selectedOverlay.copy(bgColorHex = if (selectedOverlay.bgColorHex == null) "#B3000000" else null))
                        }, label = { Text("Fundo") })
                        FilterChip(selected = selectedOverlay.gradientStartHex != null, onClick = {
                            onUpdateOverlay(
                                if (selectedOverlay.gradientStartHex == null) selectedOverlay.copy(gradientStartHex = "#FF7A00", gradientEndHex = "#00E5FF")
                                else selectedOverlay.copy(gradientStartHex = null, gradientEndHex = null)
                            )
                        }, label = { Text("Gradiente") })
                    }

                    // Alignment
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("left" to "Esq", "center" to "Centro", "right" to "Dir").forEach { (k, lbl) ->
                            FilterChip(selected = selectedOverlay.textAlign == k, onClick = { onUpdateOverlay(selectedOverlay.copy(textAlign = k)) }, label = { Text(lbl) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayersAndOverlaysToolPanel(
    overlays: List<ProOverlayItem>,
    selectedOverlayId: String?,
    onAddImageOverlay: () -> Unit,
    onAddVideoPip: () -> Unit,
    onAddBuiltInOverlay: (VideoOverlayKind, String, String, String) -> Unit,
    onSelectOverlay: (String) -> Unit,
    onMoveOverlayZ: (String, Int) -> Unit,
    onDuplicateOverlay: (String) -> Unit,
    onUpdateOverlay: (ProOverlayItem) -> Unit,
    onDeleteOverlay: (String) -> Unit
) {
    val selected = overlays.firstOrNull { it.id == selectedOverlayId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Add Layer Buttons
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProActionButton("+ Imagem", Icons.Filled.AddPhotoAlternate, onClick = onAddImageOverlay)
            ProActionButton("+ Vídeo PiP", Icons.Filled.MovieFilter, onClick = onAddVideoPip)
            ProActionButton("Selo Azul", Icons.Filled.Verified, tint = Color(0xFF1D9BF0)) {
                onAddBuiltInOverlay(VideoOverlayKind.VERIFIED_BADGE, "Selo Verificado", "", "#1D9BF0")
            }
            ProActionButton("Logótipo", Icons.Filled.AutoFixHigh) {
                onAddBuiltInOverlay(VideoOverlayKind.LOGO, "Logótipo Marca", "CANVA AO", "#00E5FF")
            }
            ProActionButton("Marca d'Água", Icons.Filled.Layers) {
                onAddBuiltInOverlay(VideoOverlayKind.WATERMARK, "Marca d'Água", "@canva.ao", "#FFFFFF")
            }
        }

        // Emojis, Stickers, Shapes & Frames Quick Strip
        Text("Stickers, Emojis, Molduras & Formas:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("🔥", "⭐", "🚀", "👑", "❤️", "⚡", "🎬", "🎉", "💎", "🎵").forEach { emj ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        onAddBuiltInOverlay(VideoOverlayKind.EMOJI, "Emoji $emj", emj, "#FFFFFF")
                    }
                ) {
                    Text(emj, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            FilterChip(selected = false, onClick = {
                onAddBuiltInOverlay(VideoOverlayKind.STICKER, "Sticker NOVO", "NOVO!", "#FF7A00")
            }, label = { Text("Sticker NOVO") })
            FilterChip(selected = false, onClick = {
                onAddBuiltInOverlay(VideoOverlayKind.FRAME, "Moldura Neon", "", "#00E5FF")
            }, label = { Text("Moldura") })
            FilterChip(selected = false, onClick = {
                onAddBuiltInOverlay(VideoOverlayKind.SHAPE, "Estrela", "star", "#FACC15")
            }, label = { Text("Estrela") })
            FilterChip(selected = false, onClick = {
                onAddBuiltInOverlay(VideoOverlayKind.SHAPE, "Círculo", "circle", "#FF7A00")
            }, label = { Text("Círculo") })
        }

        // Layer Stack Manager
        if (overlays.isNotEmpty()) {
            Text("Gestor de Camadas (${overlays.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            overlays.forEachIndexed { idx, ov ->
                val isSel = ov.id == selectedOverlayId
                Surface(
                    color = if (isSel) OrangeAccent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(if (isSel) 1.dp else 0.dp, OrangeAccent, RoundedCornerShape(10.dp))
                        .clickable { onSelectOverlay(ov.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${ov.kind.label}: ${ov.text.ifBlank { ov.title }}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onMoveOverlayZ(ov.id, -1) }, enabled = idx > 0, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Subir camada", modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { onMoveOverlayZ(ov.id, 1) }, enabled = idx < overlays.lastIndex, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Descer camada", modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { onUpdateOverlay(ov.copy(locked = !ov.locked)) }, modifier = Modifier.size(28.dp)) {
                            Icon(if (ov.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = "Bloquear", modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { onUpdateOverlay(ov.copy(visible = !ov.visible)) }, modifier = Modifier.size(28.dp)) {
                            Icon(if (ov.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Ocultar", modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { onDuplicateOverlay(ov.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicar", modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { onDeleteOverlay(ov.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        // Selected Layer Blending / Mask / Chroma Key / Opacity
        if (selected != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Propriedades: ${selected.title}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    LabeledSlider("Opacidade: ${(selected.opacity * 100).toInt()}%", selected.opacity, 0f..1f) {
                        onUpdateOverlay(selected.copy(opacity = it))
                    }
                    LabeledSlider("Escala: %.2fx".format(selected.scale), selected.scale, 0.3f..3.0f) {
                        onUpdateOverlay(selected.copy(scale = it))
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VideoBlendMode.entries.forEach { bm ->
                            FilterChip(selected = selected.blendMode == bm, onClick = { onUpdateOverlay(selected.copy(blendMode = bm)) }, label = { Text(bm.label, fontSize = 11.sp) })
                        }
                    }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VideoMaskShape.entries.forEach { ms ->
                            FilterChip(selected = selected.maskShape == ms, onClick = { onUpdateOverlay(selected.copy(maskShape = ms)) }, label = { Text(ms.label, fontSize = 11.sp) })
                        }
                    }
                    if (selected.kind == VideoOverlayKind.IMAGE || selected.kind == VideoOverlayKind.VIDEO_PIP || selected.kind == VideoOverlayKind.LOGO) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Chroma Key (Remover Cor de Fundo)", style = MaterialTheme.typography.bodySmall)
                            Switch(checked = selected.chromaKeyEnabled, onCheckedChange = { onUpdateOverlay(selected.copy(chromaKeyEnabled = it)) })
                        }
                        if (selected.chromaKeyEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("#00FF00" to "Verde", "#0000FF" to "Azul", "#FFFFFF" to "Branco", "#000000" to "Preto").forEach { (hex, lbl) ->
                                    FilterChip(
                                        selected = selected.chromaKeyColorHex == hex,
                                        onClick = { onUpdateOverlay(selected.copy(chromaKeyColorHex = hex)) },
                                        label = { Text(lbl, fontSize = 11.sp) }
                                    )
                                }
                            }
                            LabeledSlider("Tolerância Chroma", selected.chromaKeyTolerance, 0.1f..0.75f) {
                                onUpdateOverlay(selected.copy(chromaKeyTolerance = it))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EffectsAndColorToolPanel(
    selectedClip: VideoClipItem?,
    onUpdateClip: (VideoClipItem) -> Unit
) {
    if (selectedClip == null) {
        Text("Selecione um clipe para aplicar efeitos e correção de cor.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Efeitos Visuais Pro:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProEffectKind.entries.forEach { fx ->
                FilterChip(
                    selected = selectedClip.effectName == fx.name,
                    onClick = { onUpdateClip(selectedClip.copy(effectName = fx.name)) },
                    label = { Text(fx.label, fontSize = 11.sp) }
                )
            }
        }
        if (selectedClip.effectName != ProEffectKind.NONE.name) {
            LabeledSlider("Intensidade do Efeito", selectedClip.effectIntensity, 0.1f..1f) {
                onUpdateClip(selectedClip.copy(effectIntensity = it))
            }
        }

        Text("Filtros de Cor:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VideoFilterKind.entries.forEach { flt ->
                FilterChip(
                    selected = selectedClip.filter == flt,
                    onClick = { onUpdateClip(selectedClip.copy(filter = flt)) },
                    label = { Text(flt.label, fontSize = 11.sp) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ajuste Profissional & Curvas / HSL:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                onUpdateClip(
                    selectedClip.copy(
                        filter = VideoFilterKind.NORMAL,
                        brightness = 0f, contrast = 1f, saturation = 1f, exposure = 0f,
                        temperature = 0f, sharpness = 0f, vignette = 0f, grain = 0f,
                        hueShift = 0f, rgbGamma = 1f, effectName = ProEffectKind.NONE.name
                    )
                )
            }) { Text("Repor Cor") }
        }

        LabeledSlider("Brilho: %.2f".format(selectedClip.brightness), selectedClip.brightness, -0.45f..0.45f) {
            onUpdateClip(selectedClip.copy(brightness = it))
        }
        LabeledSlider("Contraste: %.2f".format(selectedClip.contrast), selectedClip.contrast, 0.5f..1.8f) {
            onUpdateClip(selectedClip.copy(contrast = it))
        }
        LabeledSlider("Saturação: %.2f".format(selectedClip.saturation), selectedClip.saturation, 0f..2f) {
            onUpdateClip(selectedClip.copy(saturation = it))
        }
        LabeledSlider("Exposição: %.2f".format(selectedClip.exposure), selectedClip.exposure, -0.45f..0.45f) {
            onUpdateClip(selectedClip.copy(exposure = it))
        }
        LabeledSlider("Temperatura: %.2f".format(selectedClip.temperature), selectedClip.temperature, -0.8f..0.8f) {
            onUpdateClip(selectedClip.copy(temperature = it))
        }
        LabeledSlider("Nitidez: %.0f%%".format(selectedClip.sharpness * 100), selectedClip.sharpness, 0f..1f) {
            onUpdateClip(selectedClip.copy(sharpness = it))
        }
        LabeledSlider("Vinheta: %.0f%%".format(selectedClip.vignette * 100), selectedClip.vignette, 0f..1f) {
            onUpdateClip(selectedClip.copy(vignette = it))
        }
        LabeledSlider("Granulação: %.0f%%".format(selectedClip.grain * 100), selectedClip.grain, 0f..1f) {
            onUpdateClip(selectedClip.copy(grain = it))
        }
        LabeledSlider("Matiz HSL: %.0f°".format(selectedClip.hueShift), selectedClip.hueShift, -180f..180f) {
            onUpdateClip(selectedClip.copy(hueShift = it))
        }
        LabeledSlider("Curva Gama RGB: %.2f".format(selectedClip.rgbGamma), selectedClip.rgbGamma, 0.6f..1.5f) {
            onUpdateClip(selectedClip.copy(rgbGamma = it))
        }
    }
}

@Composable
fun AnimationsAndKeyframesToolPanel(
    selectedClip: VideoClipItem?,
    selectedOverlay: ProOverlayItem?,
    clipProgress: Float,
    onUpdateClip: (VideoClipItem) -> Unit,
    onUpdateOverlay: (ProOverlayItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedClip != null) {
            Text("Animações de Entrada (Clipe):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("none" to "Nenhuma", "fade" to "Fade", "zoom_in" to "Zoom In", "zoom_out" to "Zoom Out", "slide_up" to "Deslizar", "pop" to "Pop", "spin" to "Girar").forEach { (k, lbl) ->
                    FilterChip(selected = selectedClip.animation == k, onClick = { onUpdateClip(selectedClip.copy(animation = k)) }, label = { Text(lbl, fontSize = 11.sp) })
                }
            }
            Text("Animações de Saída & Combo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("none" to "Saída: Nenhuma", "fade" to "Saída Fade", "zoom_out" to "Saída Zoom", "slide_down" to "Saída Descer").forEach { (k, lbl) ->
                    FilterChip(selected = selectedClip.animOut == k, onClick = { onUpdateClip(selectedClip.copy(animOut = k)) }, label = { Text(lbl, fontSize = 11.sp) })
                }
                listOf("none" to "Combo: Nenhum", "pulse" to "Pulso", "pendulum" to "Pêndulo", "ken_burns" to "Ken Burns").forEach { (k, lbl) ->
                    FilterChip(selected = selectedClip.animCombo == k, onClick = { onUpdateClip(selectedClip.copy(animCombo = k)) }, label = { Text(lbl, fontSize = 11.sp) })
                }
            }
            LabeledSlider("Duração da Animação: %.1fs".format(selectedClip.animDurationMs / 1000f), selectedClip.animDurationMs.toFloat(), 200f..2000f) {
                onUpdateClip(selectedClip.copy(animDurationMs = it.toLong()))
            }

            // Keyframes for Clip
            val clipKfs = remember(selectedClip.keyframesData) { selectedClip.keyframesData.map { VideoKeyframe.fromMap(it) } }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Keyframes do Clipe (${clipKfs.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                val kf = VideoKeyframe(
                                    timeFraction = clipProgress.coerceIn(0f, 1f),
                                    xFraction = 0.5f + selectedClip.offsetX * 0.5f,
                                    yFraction = 0.5f + selectedClip.offsetY * 0.5f,
                                    scale = selectedClip.zoomScale,
                                    rotation = selectedClip.rotationDeg,
                                    opacity = 1f
                                )
                                val updated = (clipKfs.filterNot { kotlin.math.abs(it.timeFraction - kf.timeFraction) < 0.04f } + kf)
                                    .sortedBy { it.timeFraction }
                                onUpdateClip(selectedClip.copy(keyframesData = updated.map { it.toMap() }))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
                        ) { Text("+ Keyframe") }
                    }
                    if (clipKfs.isNotEmpty()) {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            clipKfs.forEach { kf ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val remaining = clipKfs.filterNot { it.id == kf.id }
                                        onUpdateClip(selectedClip.copy(keyframesData = remaining.map { it.toMap() }))
                                    },
                                    label = { Text("${(kf.timeFraction * 100).toInt()}% • ${"%.1f".format(kf.scale)}x ✕", fontSize = 11.sp) }
                                )
                            }
                        }
                    } else {
                        Text("Ajuste zoom/posição/rotação em instantes diferentes e toque + Keyframe para animar suavemente.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (selectedOverlay != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Animação & Keyframes da Camada: ${selectedOverlay.title}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("none" to "Normal", "fade" to "Fade", "pop" to "Pop", "slide_up" to "Subir").forEach { (k, lbl) ->
                            FilterChip(selected = selectedOverlay.animIn == k, onClick = { onUpdateOverlay(selectedOverlay.copy(animIn = k)) }, label = { Text(lbl) })
                        }
                        Button(onClick = {
                            val kf = VideoKeyframe(
                                timeFraction = clipProgress.coerceIn(0f, 1f),
                                xFraction = selectedOverlay.xFraction,
                                yFraction = selectedOverlay.yFraction,
                                scale = selectedOverlay.scale,
                                rotation = selectedOverlay.rotation,
                                opacity = selectedOverlay.opacity
                            )
                            val nextKfs = (selectedOverlay.keyframes + kf).sortedBy { it.timeFraction }
                            onUpdateOverlay(selectedOverlay.copy(keyframes = nextKfs))
                        }) { Text("+ KF Camada (${selectedOverlay.keyframes.size})") }
                    }
                }
            }
        }
    }
}

@Composable
fun TransitionsToolPanel(
    selectedClip: VideoClipItem?,
    onUpdateClip: (VideoClipItem) -> Unit,
    onApplyToAll: (VideoTransitionKind, Long) -> Unit
) {
    if (selectedClip == null) {
        Text("Selecione um clipe para configurar a transição.", style = MaterialTheme.typography.bodySmall)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Biblioteca de Transições:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VideoTransitionKind.entries.forEach { tr ->
                FilterChip(
                    selected = selectedClip.transition == tr,
                    onClick = { onUpdateClip(selectedClip.copy(transition = tr)) },
                    label = { Text(tr.label) }
                )
            }
        }
        LabeledSlider(
            "Duração da Transição: %.2fs".format(selectedClip.transitionDurationMs / 1000f),
            selectedClip.transitionDurationMs.toFloat(),
            200f..2000f
        ) {
            onUpdateClip(selectedClip.copy(transitionDurationMs = it.toLong()))
        }
        OutlinedButton(onClick = { onApplyToAll(selectedClip.transition, selectedClip.transitionDurationMs) }) {
            Text("Aplicar a Todos os Clipes")
        }
    }
}

@Composable
fun AiStudioVideoToolPanel(
    aiBusy: Boolean,
    onRemoveBackground: () -> Unit,
    onSmartSubjectCutout: () -> Unit,
    onAutoCaptions: (String) -> Unit,
    onTextToSpeech: (String) -> Unit,
    onAiEnhanceVideo: () -> Unit,
    onAiSmartSlowMo: () -> Unit,
    onAiObjectTracking: () -> Unit,
    onAiGenerateSticker: (String) -> Unit,
    onToggleVoiceEnhance: () -> Unit
) {
    var captionTopic by remember { mutableStateOf("") }
    var stickerPrompt by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (aiBusy) {
            Surface(color = OrangeAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OrangeAccent, strokeWidth = 2.5.dp)
                    Text("Firebase IA a processar em tempo real...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProActionButton("Remover Fundo IA", Icons.Filled.AutoFixHigh, highlighted = true, onClick = onRemoveBackground)
            ProActionButton("Recorte Pessoa/Obj", Icons.Filled.CenterFocusStrong, onClick = onSmartSubjectCutout)
            ProActionButton("Melhorar Vídeo IA", Icons.Filled.Tune, onClick = onAiEnhanceVideo)
            ProActionButton("Slow-Mo IA", Icons.Filled.SlowMotionVideo, onClick = onAiSmartSlowMo)
            ProActionButton("Rastrear Objeto IA", Icons.Filled.CenterFocusStrong, onClick = onAiObjectTracking)
            ProActionButton("Melhorar Voz", Icons.Filled.GraphicEq, onClick = onToggleVoiceEnhance)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = captionTopic,
                onValueChange = { captionTopic = it },
                placeholder = { Text("Tema ou texto p/ Legendas IA ou Voz TTS...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onAutoCaptions(captionTopic) },
                enabled = !aiBusy,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) { Text("Legendas IA") }
            OutlinedButton(
                onClick = { onTextToSpeech(captionTopic.ifBlank { "Apresentação criada no CANVA AO!" }) },
                enabled = !aiBusy
            ) { Text("Voz TTS") }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = stickerPrompt,
                onValueChange = { stickerPrompt = it },
                placeholder = { Text("Descreva um sticker para gerar com IA...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onAiGenerateSticker(stickerPrompt.ifBlank { "Troféu dourado brilhante 3D" }) },
                enabled = !aiBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
            ) { Text("Gerar Sticker") }
        }
    }
}

@Composable
fun BackgroundsAndFormatToolPanel(
    aspectRatio: String,
    bgColorStart: String,
    bgColorEnd: String?,
    animatedBgStyle: VideoBackgroundAnimatedStyle,
    onSelectAspectRatio: (String) -> Unit,
    onSelectBgColors: (String, String?) -> Unit,
    onSelectAnimatedStyle: (VideoBackgroundAnimatedStyle) -> Unit,
    onSelectScenePhoto: (String) -> Unit,
    onPickGalleryBgPhoto: () -> Unit,
    onClearBgPhoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Proporção do Vídeo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("9:16" to "9:16 Reels", "16:9" to "16:9 YouTube", "1:1" to "1:1 Quadrado", "3:4" to "3:4 Retrato", "4:5" to "4:5 Feed").forEach { (r, lbl) ->
                FilterChip(selected = aspectRatio == r, onClick = { onSelectAspectRatio(r) }, label = { Text(lbl, fontSize = 11.sp) })
            }
        }

        Text("Fundos Animados:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            VideoBackgroundAnimatedStyle.entries.forEach { st ->
                FilterChip(selected = animatedBgStyle == st, onClick = { onSelectAnimatedStyle(st) }, label = { Text(st.label, fontSize = 11.sp) })
            }
        }

        Text("Cores Sólidas & Gradientes:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = bgColorStart.equals("transparent", true),
                onClick = { onSelectBgColors("transparent", null) },
                label = { Text("Transparente") }
            )
            listOf(
                "#0A1628" to null,
                "#000000" to null,
                "#FFFFFF" to null,
                "#0A1628" to "#1E40AF",
                "#FF7A00" to "#EC4899",
                "#0F172A" to "#00E5FF",
                "#4C1D95" to "#F59E0B"
            ).forEach { (c1, c2) ->
                val brush = if (c2 != null) Brush.linearGradient(listOf(parseComposeColor(c1), parseComposeColor(c2)))
                else Brush.linearGradient(listOf(parseComposeColor(c1), parseComposeColor(c1)))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(brush)
                        .border(if (bgColorStart == c1 && bgColorEnd == c2) 2.5.dp else 1.dp, Color.White, CircleShape)
                        .clickable { onSelectBgColors(c1, c2) }
                )
            }
        }

        Text("Fotografias Reais & Galeria:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onPickGalleryBgPhoto) { Text("+ Foto Galeria") }
            OutlinedButton(onClick = onClearBgPhoto) { Text("Sem Foto") }
            listOf(
                "Estúdio TV" to "res://img_tpl_news_studio",
                "Cidade" to "res://img_tpl_business_city",
                "Luanda" to "res://tpl_art_luanda",
                "Moda" to "res://tpl_art_moda",
                "Produto" to "res://img_tpl_ad_product",
                "Ideias" to "res://img_hero_ideas"
            ).forEach { (lbl, uri) ->
                FilterChip(selected = false, onClick = { onSelectScenePhoto(uri) }, label = { Text(lbl) })
            }
        }
    }
}

@Composable
private fun ProActionButton(
    label: String,
    icon: ImageVector,
    highlighted: Boolean = false,
    tint: Color = CyanGlow,
    onClick: () -> Unit
) {
    Surface(
        color = if (highlighted) OrangeAccent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .border(1.dp, if (highlighted) OrangeAccent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = label, tint = if (highlighted) OrangeAccent else tint, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = OrangeAccent, activeTrackColor = OrangeAccent),
            modifier = Modifier.height(26.dp)
        )
    }
}
