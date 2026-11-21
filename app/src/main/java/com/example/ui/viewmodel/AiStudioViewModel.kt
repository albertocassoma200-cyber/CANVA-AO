package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.UiState
import com.example.data.repository.AiDesignProposal
import com.example.data.repository.AiStudioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AiChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val designProposal: AiDesignProposal? = null,
    val generatedBitmap: Bitmap? = null
)

class AiStudioViewModel(
    private val aiRepository: AiStudioRepository = AiStudioRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(
            AiChatMessage(
                id = "welcome",
                fromUser = false,
                text = "Olá! Sou a IA oficial do CANVA AO. Posso criar templates completos com fotografia real prontos para guardar na sua biblioteca, abrir cartazes editáveis no editor, gerar imagens exclusivas e escrever legendas."
            )
        )
    )
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _chatBusy = MutableStateFlow(false)
    val chatBusy: StateFlow<Boolean> = _chatBusy.asStateFlow()

    private val _imageGenState = MutableStateFlow<UiState<Bitmap?>>(UiState.Success(null))
    val imageGenState: StateFlow<UiState<Bitmap?>> = _imageGenState.asStateFlow()

    private val _designGenState = MutableStateFlow<UiState<AiDesignProposal?>>(UiState.Success(null))
    val designGenState: StateFlow<UiState<AiDesignProposal?>> = _designGenState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private fun isDesignOrTemplatePrompt(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf(
            "template", "modelo", "cria", "criar", "faz", "gera",
            "cartaz", "flyer", "post", "banner", "capa", "story",
            "anúncio", "anuncio", "publicidade", "notícia", "noticia", "design"
        )
        return keywords.any { lower.contains(it) }
    }

    fun sendChatPrompt(prompt: String, buildDesignAlso: Boolean = false) {
        val clean = prompt.trim()
        if (clean.isBlank() || _chatBusy.value) return
        _lastError.value = null

        val userMsg = AiChatMessage(id = "u_${System.currentTimeMillis()}", fromUser = true, text = clean)
        val aiMsgId = "a_${System.currentTimeMillis()}"
        _messages.value = _messages.value + userMsg + AiChatMessage(id = aiMsgId, fromUser = false, text = "A criar com Gemini...")
        _chatBusy.value = true

        viewModelScope.launch {
            try {
                if (buildDesignAlso || isDesignOrTemplatePrompt(clean)) {
                    val proposalResult = aiRepository.generateDesignProposal(clean)
                    proposalResult.fold(
                        onSuccess = { proposal ->
                            val reply = "Template/Design criado para \"${proposal.title}\":\n• Categoria: ${proposal.category}\n• Formato: ${proposal.formatName} (${proposal.width}×${proposal.height})\n• Título: ${proposal.headline}\n• Legenda sugerida: ${proposal.socialCaption}"
                            updateAiMessage(aiMsgId, reply, proposal = proposal)
                        },
                        onFailure = { err ->
                            // Try streaming a text fallback so the user still gets a helpful response if structured output fails
                            val msg = err.localizedMessage ?: "Falha na geração estruturada."
                            _lastError.value = msg
                            streamTextIntoMessage(clean, aiMsgId)
                        }
                    )
                } else {
                    streamTextIntoMessage(clean, aiMsgId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "Falha na comunicação com Firebase AI Logic."
                _lastError.value = errMsg
                updateAiMessage(aiMsgId, "Não foi possível completar o pedido: $errMsg")
            } finally {
                _chatBusy.value = false
            }
        }
    }

    private suspend fun streamTextIntoMessage(prompt: String, aiMsgId: String) {
        val contextSummary = _messages.value.takeLast(6).joinToString("\n") {
            "${if (it.fromUser) "Utilizador" else "IA"}: ${it.text}"
        }
        try {
            aiRepository.streamAssistantReply(prompt, contextSummary).collect { partial ->
                updateAiMessage(aiMsgId, partial)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Erro ao gerar resposta com Gemini."
            _lastError.value = msg
            updateAiMessage(aiMsgId, "Não foi possível responder: $msg")
        }
    }

    private fun updateAiMessage(
        id: String,
        newText: String,
        proposal: AiDesignProposal? = null,
        bitmap: Bitmap? = null
    ) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) {
                msg.copy(
                    text = newText,
                    designProposal = proposal ?: msg.designProposal,
                    generatedBitmap = bitmap ?: msg.generatedBitmap
                )
            } else {
                msg
            }
        }
    }

    fun generateAiDesign(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _designGenState.value = UiState.Loading
            aiRepository.generateDesignProposal(prompt).fold(
                onSuccess = { _designGenState.value = UiState.Success(it) },
                onFailure = { _designGenState.value = UiState.Error(it.localizedMessage ?: "Erro ao gerar design.") }
            )
        }
    }

    fun generateAiImage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _imageGenState.value = UiState.Loading
            aiRepository.generateImageFromPrompt(prompt).fold(
                onSuccess = { bmp ->
                    _imageGenState.value = UiState.Success(bmp)
                    _messages.value = _messages.value + AiChatMessage(
                        id = "img_${System.currentTimeMillis()}",
                        fromUser = false,
                        text = "Imagem gerada para: \"$prompt\"",
                        generatedBitmap = bmp
                    )
                },
                onFailure = { err ->
                    _imageGenState.value = UiState.Error(err.localizedMessage ?: "Erro ao gerar imagem.")
                }
            )
        }
    }

    fun editImageWithAi(input: Bitmap, instruction: String) {
        viewModelScope.launch {
            _imageGenState.value = UiState.Loading
            aiRepository.editOrRemoveBackground(input, instruction).fold(
                onSuccess = { bmp -> _imageGenState.value = UiState.Success(bmp) },
                onFailure = { err -> _imageGenState.value = UiState.Error(err.localizedMessage ?: "Erro ao processar imagem.") }
            )
        }
    }

    fun resetImageGenState() {
        _imageGenState.value = UiState.Success(null)
    }

    fun resetDesignGenState() {
        _designGenState.value = UiState.Success(null)
    }
}
