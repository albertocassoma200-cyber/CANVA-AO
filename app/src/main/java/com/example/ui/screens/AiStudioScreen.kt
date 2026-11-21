package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.DesignLayer
import com.example.core.model.DesignProject
import com.example.core.model.LayerType
import com.example.core.model.UiState
import com.example.engine.graphics.BitmapRenderer
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.OrangeAccent
import com.example.ui.viewmodel.AiStudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun AiStudioScreen(
    aiViewModel: AiStudioViewModel,
    currentUserId: String,
    onBack: () -> Unit,
    onOpenAiProjectInEditor: (DesignProject) -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val chatBusy by aiViewModel.chatBusy.collectAsStateWithLifecycle()
    val imageGenState by aiViewModel.imageGenState.collectAsStateWithLifecycle()
    val lastError by aiViewModel.lastError.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var promptInput by remember { mutableStateOf("") }
    var imagePrompt by remember { mutableStateOf("") }
    var editInstruction by remember { mutableStateOf("Remover o fundo e manter o objeto em destaque limpo") }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val photoEditPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                if (bmp != null) {
                    aiViewModel.editImageWithAi(bmp, editInstruction)
                } else {
                    onShowMessage("Não foi possível carregar a imagem selecionada.")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("AI Studio", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Surface(color = ElectricViolet, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Beta",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Gradient Hero Banner matching mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(ElectricIndigo, ElectricViolet, CyanGlow)))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Crie com Inteligência Artificial",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            text = "Transforme ideias em cartazes editáveis, imagens e legendas profissionais.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OrangeAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Assistente & Design") },
                    icon = { Icon(Icons.Filled.Brush, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Gerar Imagem") },
                    icon = { Icon(Icons.Filled.Image, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Textos & IA") },
                    icon = { Icon(Icons.Filled.EditNote, null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Quick suggestions strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickPrompts = listOf(
                            "Cria um cartaz profissional para uma loja de roupas.",
                            "Cria um flyer de promoção 50% OFF para restaurante.",
                            "Cria uma capa moderna para evento de música.",
                            "Sugira uma paleta de cores para marca de tecnologia."
                        )
                        quickPrompts.forEach { suggestion ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    aiViewModel.sendChatPrompt(
                                        suggestion,
                                        buildDesignAlso = suggestion.startsWith("Cria")
                                    )
                                },
                                label = { Text(suggestion.take(36) + if (suggestion.length > 36) "…" else "") }
                            )
                        }
                    }

                    if (!lastError.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = lastError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (msg.fromUser) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    color = if (msg.fromUser) OrangeAccent else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(18.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.widthIn(max = 320.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = msg.text,
                                            color = if (msg.fromUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        msg.designProposal?.let { proposal ->
                                            Button(
                                                onClick = {
                                                    onOpenAiProjectInEditor(proposal.toEditableProject(currentUserId))
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = Color.White),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("ai_open_design_button")
                                            ) {
                                                Icon(Icons.Filled.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Abrir Design no Editor")
                                            }
                                        }
                                        msg.generatedBitmap?.let { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Imagem gerada por IA",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promptInput,
                                onValueChange = { promptInput = it },
                                placeholder = { Text("Descreva o design ou peça ajuda...") },
                                maxLines = 3,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_chat_input")
                            )
                            IconButton(
                                onClick = {
                                    val p = promptInput
                                    promptInput = ""
                                    aiViewModel.sendChatPrompt(p)
                                },
                                enabled = !chatBusy && promptInput.isNotBlank(),
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(OrangeAccent)
                                    .testTag("ai_send_button")
                            ) {
                                if (chatBusy) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Generate Image & Remove Background with AI
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Gerar Imagem com IA", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(
                                        value = imagePrompt,
                                        onValueChange = { imagePrompt = it },
                                        label = { Text("Descreva a imagem desejada") },
                                        placeholder = { Text("Ex: Fotografia de moda moderna com iluminação laranja e azul escuro") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_image_prompt_input")
                                    )
                                    Button(
                                        onClick = { aiViewModel.generateAiImage(imagePrompt) },
                                        enabled = imageGenState !is UiState.Loading && imagePrompt.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("ai_generate_image_button")
                                    ) {
                                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Gerar Imagem")
                                    }
                                }
                            }
                        }

                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Remover Fundo / Editar Foto IA", style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(
                                        value = editInstruction,
                                        onValueChange = { editInstruction = it },
                                        label = { Text("Instrução de edição") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            photoEditPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        enabled = imageGenState !is UiState.Loading,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.LayersClear, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Selecionar Foto da Galeria")
                                    }
                                }
                            }
                        }

                        item {
                            when (val st = imageGenState) {
                                is UiState.Loading -> {
                                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CircularProgressIndicator(color = OrangeAccent)
                                            Text("A processar imagem com Gemini...")
                                        }
                                    }
                                }
                                is UiState.Error -> {
                                    Surface(color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f), shape = RoundedCornerShape(14.dp)) {
                                        Text(st.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(14.dp))
                                    }
                                }
                                is UiState.Success -> {
                                    st.data?.let { bmp ->
                                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Resultado IA",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(260.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            val uri = BitmapRenderer.saveBitmapToTempUri(context, bmp, "ai_studio")
                                                            val proj = DesignProject(
                                                                userId = currentUserId,
                                                                title = "Arte AI Studio",
                                                                category = "AI Studio",
                                                                formatName = "Instagram Quadrado",
                                                                width = 1080L,
                                                                height = 1080L,
                                                                backgroundColorHex = "#0A1628",
                                                                layers = listOf(
                                                                    DesignLayer(
                                                                        layerId = "ai_${UUID.randomUUID().toString().take(6)}",
                                                                        type = LayerType.IMAGE,
                                                                        name = "Imagem IA",
                                                                        xFraction = 0.5f,
                                                                        yFraction = 0.5f,
                                                                        widthFraction = 0.9f,
                                                                        heightFraction = 0.9f,
                                                                        imageUri = uri.toString()
                                                                    ).toMap()
                                                                )
                                                            )
                                                            onOpenAiProjectInEditor(proj)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Usar no Editor Gráfico")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Recursos de IA (Copywriting, Paletas, Roteiros)
                    val tools = listOf(
                        "Gerar legenda para Instagram" to "Escreve 3 legendas envolventes para Instagram com emojis e hashtags para um negócio em Angola.",
                        "Criar slogan e título de cartaz" to "Sugira 5 títulos curtos e impactantes para um cartaz promocional.",
                        "Paleta de cores e fontes" to "Recomenda 3 paletas de cores em hex e combinações de fontes para uma marca moderna.",
                        "Roteiro para vídeo curto (Reels/TikTok)" to "Escreva um roteiro de 15 segundos para um vídeo promocional dinâmico."
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tools) { (title, prompt) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedTab = 0
                                        aiViewModel.sendChatPrompt(prompt)
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = OrangeAccent)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.titleMedium)
                                        Text(prompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
